package com.suvojeet.notenext.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.app.NotificationCompat
import com.suvojeet.notenext.data.ChecklistItem
import java.io.IOException

import com.suvojeet.notenext.util.HtmlConverter
import com.suvojeet.notenext.data.MarkdownExporter
import androidx.compose.ui.text.AnnotatedString

suspend fun saveAsMd(context: Context, title: String, content: AnnotatedString, checklist: List<ChecklistItem> = emptyList()) {
    val contentResolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "${title.ifBlank { "Untitled" }}.md")
        put(MediaStore.MediaColumns.MIME_TYPE, "text/markdown")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
    }

    val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

    uri?.let {
        try {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                val fullContent = StringBuilder()
                fullContent.append("# $title\n\n")
                if (checklist.isNotEmpty()) {
                    checklist.forEach { item ->
                        fullContent.append("- [${if (item.isChecked) "x" else " "}] ${item.text}\n")
                    }
                } else {
                    val html = HtmlConverter.annotatedStringToHtml(content)
                    val markdown = MarkdownExporter.convertHtmlToMarkdown(html)
                    fullContent.append(markdown)
                }
                outputStream.write(fullContent.toString().toByteArray())
            }
            showSaveSuccessNotification(context, title, "Documents", "MD")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

fun saveAsTxt(context: Context, title: String, content: String, checklist: List<ChecklistItem> = emptyList()) {
    val contentResolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "${title.ifBlank { "Untitled" }}.txt")
        put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
    }

    val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

    uri?.let {
        try {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                val fullContent = StringBuilder()
                fullContent.append(title).append("\n\n")
                if (checklist.isNotEmpty()) {
                    checklist.forEach { item ->
                        fullContent.append("[${if (item.isChecked) "x" else " "}] ${item.text}\n")
                    }
                } else {
                    fullContent.append(content)
                }
                outputStream.write(fullContent.toString().toByteArray())
            }
            showSaveSuccessNotification(context, title, "Documents", "TXT")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

fun showSaveSuccessNotification(context: Context, title: String, location: String, fileType: String) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "note_save_channel"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Note Save Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(context, channelId)
        .setContentTitle("Note Saved")
        .setContentText("'$title' saved as $fileType in $location")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setAutoCancel(true)
        .build()

    notificationManager.notify(1, notification)
}

fun saveAsPdf(context: Context, title: String, content: String, checklist: List<ChecklistItem> = emptyList()) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
    val page = pdfDocument.startPage(pageInfo)
    val canvas: Canvas = page.canvas
    val titlePaint = TextPaint().apply {
        textSize = 18f
        isFakeBoldText = true
    }
    val textPaint = TextPaint().apply {
        textSize = 12f
    }
    val checkboxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    var yPosition = 60f

    // Draw title
    val titleLayout = StaticLayout.Builder.obtain(
        title, 0, title.length, titlePaint, canvas.width - 80
    ).build()
    canvas.save()
    canvas.translate(40f, yPosition)
    titleLayout.draw(canvas)
    canvas.restore()
    yPosition += titleLayout.height + 20f

    // Draw content or checklist
    if (checklist.isNotEmpty()) {
        checklist.forEach { item ->
            val checkboxSize = 20f
            val textIndent = 40f + checkboxSize + 10f

            // Draw checkbox
            canvas.drawRect(40f, yPosition, 40f + checkboxSize, yPosition + checkboxSize, checkboxPaint)
            if (item.isChecked) {
                canvas.drawLine(45f, yPosition + 10f, 50f, yPosition + 15f, checkboxPaint)
                canvas.drawLine(50f, yPosition + 15f, 55f, yPosition + 5f, checkboxPaint)
            }

            // Draw text
            val itemText = item.text.ifBlank { " " }
            val textLayout = StaticLayout.Builder.obtain(
                itemText, 0, itemText.length, textPaint, canvas.width - (textIndent.toInt() + 40)
            ).build()

            canvas.save()
            canvas.translate(textIndent, yPosition)
            textLayout.draw(canvas)
            canvas.restore()

            yPosition += textLayout.height + 10f // Add some space between items
        }
    } else {
        val contentToSave = content.ifBlank { " " }
        val contentLayout = StaticLayout.Builder.obtain(
            contentToSave, 0, contentToSave.length, textPaint, canvas.width - 80
        ).build()
        canvas.save()
        canvas.translate(40f, yPosition)
        contentLayout.draw(canvas)
        canvas.restore()
    }

    pdfDocument.finishPage(page)

    val contentResolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "${title.ifBlank { "Untitled" }}.pdf")
        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
    }

    val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

    uri?.let {
        try {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            showSaveSuccessNotification(context, title, "Documents", "PDF")
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }
    }
}