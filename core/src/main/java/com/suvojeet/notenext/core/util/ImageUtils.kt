package com.suvojeet.notenext.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ImageUtils {

    suspend fun compressImage(context: Context, uri: Uri): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                // First decode with inJustDecodeBounds=true to check dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(uri)?.use { 
                    BitmapFactory.decodeStream(it, null, options)
                }

                val maxDimension = 1920
                var inSampleSize = 1
                if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                    val halfHeight: Int = options.outHeight / 2
                    val halfWidth: Int = options.outWidth / 2
                    while (halfHeight / inSampleSize >= maxDimension && halfWidth / inSampleSize >= maxDimension) {
                        inSampleSize *= 2
                    }
                }

                // Decode with inSampleSize
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = inSampleSize
                }
                val originalBitmap = context.contentResolver.openInputStream(uri)?.use { 
                    BitmapFactory.decodeStream(it, null, decodeOptions)
                }

                if (originalBitmap == null) return@withContext null

                // Final resize if still slightly above maxDimension
                val ratio = Math.min(
                    maxDimension.toDouble() / originalBitmap.width,
                    maxDimension.toDouble() / originalBitmap.height
                )
                
                val finalBitmap = if (ratio < 1.0) {
                    val width = (originalBitmap.width * ratio).toInt().coerceAtLeast(1)
                    val height = (originalBitmap.height * ratio).toInt().coerceAtLeast(1)
                    // createScaledBitmap can return null on allocation failure; fall back
                    // to the original rather than crashing on a null dereference.
                    Bitmap.createScaledBitmap(originalBitmap, width, height, true) ?: originalBitmap
                } else {
                    originalBitmap
                }

                // Create a file in the cache directory
                val filesDir = context.getExternalFilesDir(null) ?: context.filesDir
                val imagesDir = File(filesDir, "images")
                if (!imagesDir.exists()) {
                    imagesDir.mkdirs()
                }
                
                val fileName = "IMG_${UUID.randomUUID()}.jpg"
                val file = File(imagesDir, fileName)

                // Compress to JPEG with 80% quality. Use .use{} so the stream is closed
                // even if compress() throws (prevents file-descriptor leaks on repeated saves).
                FileOutputStream(file).use { outputStream ->
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    outputStream.flush()
                }

                if (finalBitmap != originalBitmap) {
                    finalBitmap.recycle()
                }
                originalBitmap.recycle()

                // Return the Uri for the new file
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: OutOfMemoryError) {
                // Large camera images on low-RAM devices can exhaust the heap. OOM is an
                // Error (not an Exception) so it would otherwise crash the app here.
                e.printStackTrace()
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
