package com.suvojeet.notenext.util

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
import java.io.IOException

fun saveAsTxt(context: Context, title: String, content: String) {
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
                outputStream.write((title + "\n\n" + content).toByteArray())

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

fun saveAsPdf(context: Context, title: String, content: String) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
    val page = pdfDocument.startPage(pageInfo)
    val canvas: Canvas = page.canvas
    val titlePaint = Paint()
    titlePaint.textSize = 18f
    titlePaint.isFakeBoldText = true
    canvas.drawText(title, 40f, 60f, titlePaint)

    val textPaint = TextPaint()
    textPaint.textSize = 12f

    val contentToSave = content.ifBlank { " " } // StaticLayout crashes on empty string

    val staticLayout = StaticLayout.Builder.obtain(
        contentToSave, 0, contentToSave.length, textPaint, canvas.width - 80
    ).build()

    canvas.save()
    canvas.translate(40f, 90f)
    staticLayout.draw(canvas)
    canvas.restore()

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