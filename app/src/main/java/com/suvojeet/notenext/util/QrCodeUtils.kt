package com.suvojeet.notenext.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Utility class for QR code generation and decoding for note sharing.
 * Uses JSON + GZIP compression to maximize data capacity within QR code limits.
 */
object QrCodeUtils {

    private val gson = Gson()

    // Maximum characters before compression warning (QR code limit ~2KB after encoding)
    private const val MAX_CONTENT_LENGTH = 1500
    private const val QR_SIZE = 512

    /**
     * Data class representing note data for QR encoding.
     */
    data class NoteQrData(
        val t: String, // title
        val c: String  // content
    )

    /**
     * Generates a QR code bitmap from note title and content.
     *
     * @param title Note title
     * @param content Note content
     * @param size QR code size in pixels (default 512)
     * @return Bitmap of the QR code, or null if generation fails
     */
    fun generateQrCode(
        title: String,
        content: String,
        size: Int = QR_SIZE
    ): Bitmap? {
        return try {
            val noteData = NoteQrData(t = title, c = content)
            val jsonData = gson.toJson(noteData)
            val compressedData = compressData(jsonData)

            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 2,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(compressedData, BarcodeFormat.QR_CODE, size, size, hints)

            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    pixels[y * width + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                }
            }

            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decodes QR code data string back to NoteQrData.
     *
     * @param data The compressed/encoded data from QR scan
     * @return NoteQrData if successful, null otherwise
     */
    fun decodeQrData(data: String): NoteQrData? {
        return try {
            val decompressedJson = decompressData(data)
            gson.fromJson(decompressedJson, NoteQrData::class.java)
        } catch (e: Exception) {
            // Try parsing as plain JSON (for older/simple QR codes)
            try {
                gson.fromJson(data, NoteQrData::class.java)
            } catch (e2: Exception) {
                e2.printStackTrace()
                null
            }
        }
    }

    /**
     * Checks if the note content is within QR code size limits.
     *
     * @param title Note title
     * @param content Note content
     * @return true if the note can be encoded, false if it's too large
     */
    fun isWithinSizeLimit(title: String, content: String): Boolean {
        return (title.length + content.length) <= MAX_CONTENT_LENGTH
    }

    /**
     * Gets the estimated size percentage used (0-100+).
     * Values over 100 indicate the note is too large.
     */
    fun getSizePercentage(title: String, content: String): Int {
        val totalLength = title.length + content.length
        return (totalLength * 100) / MAX_CONTENT_LENGTH
    }

    /**
     * Compresses data using GZIP and encodes to Base64.
     */
    private fun compressData(data: String): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        GZIPOutputStream(byteArrayOutputStream).use { gzip ->
            gzip.write(data.toByteArray(Charsets.UTF_8))
        }
        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())
    }

    /**
     * Decompresses Base64-encoded GZIP data.
     */
    private fun decompressData(compressedData: String): String {
        val bytes = Base64.getDecoder().decode(compressedData)
        return GZIPInputStream(bytes.inputStream()).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}
