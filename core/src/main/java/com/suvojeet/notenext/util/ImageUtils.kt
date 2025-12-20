package com.suvojeet.notenext.util

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
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap == null) return@withContext null

                // Resize if too big (max 1920px width/height)
                val maxDimension = 1920
                val ratio = Math.min(
                    maxDimension.toDouble() / originalBitmap.width,
                    maxDimension.toDouble() / originalBitmap.height
                )
                
                val finalBitmap = if (ratio < 1.0) {
                    val width = (originalBitmap.width * ratio).toInt()
                    val height = (originalBitmap.height * ratio).toInt()
                    Bitmap.createScaledBitmap(originalBitmap, width, height, true)
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
                val outputStream = FileOutputStream(file)

                // Compress to JPEG with 80% quality
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                outputStream.flush()
                outputStream.close()
                
                if (finalBitmap != originalBitmap) {
                    finalBitmap.recycle()
                }
                originalBitmap.recycle()

                // Return the Uri for the new file
                FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
