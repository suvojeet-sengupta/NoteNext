package com.suvojeet.notenext.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.text.AnnotatedString
import com.suvojeet.notenext.data.Attachment
import com.suvojeet.notenext.core.model.AttachmentType
import com.suvojeet.notenext.util.HtmlConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object NoteHtmlGenerator {

    suspend fun generateNoteHtml(
        context: Context,
        title: String,
        content: AnnotatedString,
        attachments: List<Attachment>
    ): String = withContext(Dispatchers.IO) {
        val contentHtml = HtmlConverter.annotatedStringToHtml(content)
        val attachmentsHtml = attachments
            .filter { it.type == AttachmentType.IMAGE }
            .joinToString("<br>") { attachment ->
                try {
                    val uri = Uri.parse(attachment.uri)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    
                    if (bitmap != null) {
                        val outputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                        val byteArray = outputStream.toByteArray()
                        val base64 = Base64.encodeToString(byteArray, Base64.DEFAULT)
                        bitmap.recycle()
                        "<img src=\"data:image/jpeg;base64,$base64\" style=\"max-width: 100%; height: auto; border-radius: 8px; margin-bottom: 8px;\">"
                    } else {
                        ""
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    ""
                }
            }
        
        """
        <html>
            <head>
                <style>
                    body { font-family: sans-serif; padding: 20px; line-height: 1.6; }
                    h1 { color: #333; margin-bottom: 20px; }
                    img { display: block; margin: 10px auto; width: 100%; max-width: 100%; }
                </style>
            </head>
            <body>
                <h1>$title</h1>
                <div>$contentHtml</div>
                <div style="margin-top: 20px;">$attachmentsHtml</div>
            </body>
        </html>
        """.trimIndent()
    }
}
