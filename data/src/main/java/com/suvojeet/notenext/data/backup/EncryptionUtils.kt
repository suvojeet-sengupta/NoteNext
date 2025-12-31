package com.suvojeet.notenext.data.backup

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtils {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "AES"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12
    private const val SALT_LENGTH_BYTE = 16
    private const val PBKDF2_ITERATIONS = 10000
    private const val KEY_LENGTH_BIT = 256

    // Header to identify encrypted files
    private const val ENCRYPTED_FILE_HEADER = "NOTENEXT_ENC_V1"

    fun encryptFile(inputFile: File, outputStream: OutputStream, password: String) {
        val salt = ByteArray(SALT_LENGTH_BYTE)
        SecureRandom().nextBytes(salt)

        val iv = ByteArray(IV_LENGTH_BYTE)
        SecureRandom().nextBytes(iv)

        val secretKey = deriveKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

        outputStream.use { out ->
            // Write Header
            out.write(ENCRYPTED_FILE_HEADER.toByteArray(Charsets.UTF_8))
            // Write Salt and IV
            out.write(salt)
            out.write(iv)

            FileInputStream(inputFile).use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    val encrypted = cipher.update(buffer, 0, bytesRead)
                    if (encrypted != null) {
                        out.write(encrypted)
                    }
                }
                val finalBytes = cipher.doFinal()
                if (finalBytes != null) {
                    out.write(finalBytes)
                }
            }
        }
    }

    /**
     * Decrypts an encrypted stream to a target file.
     * @return true if decryption was successful, false if it failed (e.g. wrong password).
     * Throws exception for IO errors.
     */
    fun decryptFile(inputStream: InputStream, outputFile: File, password: String) {
        inputStream.use { objIn ->
            // Read Header
            val headerBytes = ByteArray(ENCRYPTED_FILE_HEADER.length)
            val headerRead = objIn.read(headerBytes)
            val header = String(headerBytes, Charsets.UTF_8)
            
            if (headerRead != ENCRYPTED_FILE_HEADER.length || header != ENCRYPTED_FILE_HEADER) {
                throw IllegalArgumentException("Invalid file format or not an encrypted backup.")
            }

            // Read Salt and IV
            val salt = ByteArray(SALT_LENGTH_BYTE)
            if (objIn.read(salt) != SALT_LENGTH_BYTE) throw IllegalArgumentException("Corrupted file (missing salt).")

            val iv = ByteArray(IV_LENGTH_BYTE)
            if (objIn.read(iv) != IV_LENGTH_BYTE) throw IllegalArgumentException("Corrupted file (missing IV).")

            val secretKey = deriveKey(password, salt)
            val cipher = Cipher.getInstance(ALGORITHM)
            val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)

            FileOutputStream(outputFile).use { out ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (objIn.read(buffer).also { bytesRead = it } != -1) {
                    val decrypted = cipher.update(buffer, 0, bytesRead)
                    if (decrypted != null) {
                        out.write(decrypted)
                    }
                }
                val finalBytes = cipher.doFinal()
                if (finalBytes != null) {
                    out.write(finalBytes)
                }
            }
        }
    }
    
    fun isEncrypted(inputStream: InputStream): Boolean {
        if (!inputStream.markSupported()) {
           // If we can't mark, we can't peek easily. 
           // In this app context, we are opening fresh streams from ContentResolver, so we can just read.
           // But acts as a check.
        }
        
        try {
            val headerBytes = ByteArray(ENCRYPTED_FILE_HEADER.length)
            // We need to read without consuming if we want to reuse stream, OR we assume this is just a check 
            // and the caller will open a NEW stream for actual processing.
            // Since we use ContentResolver, we can just open a new stream.
            // So we just read.
            val bytesRead = inputStream.read(headerBytes)
            if (bytesRead != ENCRYPTED_FILE_HEADER.length) return false
            
            val header = String(headerBytes, Charsets.UTF_8)
            return header == ENCRYPTED_FILE_HEADER
        } catch (e: Exception) {
            return false
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BIT)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, KEY_ALGORITHM)
    }
}
