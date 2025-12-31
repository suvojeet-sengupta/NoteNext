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

// Helper to map AnnotatedString to Spannable for PDF
private fun annotatedStringToSpannable(annotatedString: AnnotatedString): android.text.SpannableString {
    val spannable = android.text.SpannableString(annotatedString.text)
    annotatedString.spanStyles.forEach { range ->
        val style = range.item
        val start = range.start
        val end = range.end
        
        if (style.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold) {
            spannable.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (style.fontStyle == androidx.compose.ui.text.font.FontStyle.Italic) {
            spannable.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.ITALIC), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (style.textDecoration == androidx.compose.ui.text.style.TextDecoration.Underline) {
            spannable.setSpan(android.text.style.UnderlineSpan(), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        // Add more style mappings as needed
    }
    return spannable
}

fun saveAsPdf(
    context: Context, 
    title: String, 
    content: AnnotatedString,
    attachments: List<Attachment> = emptyList(),
    checklist: List<ChecklistItem> = emptyList()
) {
    val pdfDocument = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val margin = 40f
    val contentWidth = (pageWidth - 2 * margin).toInt()
    
    var currentPageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas
    var yPos = margin

    val titlePaint = TextPaint().apply {
        textSize = 24f
        isFakeBoldText = true
        color = android.graphics.Color.BLACK
    }
    val textPaint = TextPaint().apply {
        textSize = 12f
        color = android.graphics.Color.BLACK
    }
    val checkboxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = android.graphics.Color.BLACK
    }

    fun startNewPage() {
        pdfDocument.finishPage(page)
        currentPageNumber++
        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
        page = pdfDocument.startPage(pageInfo)
        canvas = page.canvas
        yPos = margin
    }

    fun checkSpace(height: Float) {
        if (yPos + height > pageHeight - margin) {
            startNewPage()
        }
    }

    // Draw Title
    val titleLayout = StaticLayout.Builder.obtain(title, 0, title.length, titlePaint, contentWidth)
        .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(0f, 1f)
        .setIncludePad(true)
        .build()

    checkSpace(titleLayout.height.toFloat())
    canvas.save()
    canvas.translate(margin, yPos)
    titleLayout.draw(canvas)
    canvas.restore()
    yPos += titleLayout.height + 20f

    // Draw Attachments (Images)
    attachments.filter { it.type == "IMAGE" }.forEach { attachment ->
        try {
            val imageUri = android.net.Uri.parse(attachment.uri)
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
                var targetWidth = contentWidth.toFloat()
                var targetHeight = targetWidth * aspectRatio
                
                // If image is too tall for a single page, scale it down or just let it be (handling multi-page images is complex)
                // For now, ensure it doesn't exceed page height limit roughly
                if (targetHeight > (pageHeight - 2 * margin)) {
                     targetHeight = (pageHeight - 2 * margin).toFloat()
                     targetWidth = targetHeight / aspectRatio
                }

                checkSpace(targetHeight + 20f)
                
                val destRect = android.graphics.RectF(margin, yPos, margin + targetWidth, yPos + targetHeight)
                canvas.drawBitmap(bitmap, null, destRect, null)
                yPos += targetHeight + 20f
                bitmap.recycle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Draw Checklist or Content
    if (checklist.isNotEmpty()) {
        checklist.forEach { item ->
            val checkboxSize = 16f
            val textIndent = 24f
            
            val itemText = item.text.ifBlank { " " }
            val textLayout = StaticLayout.Builder.obtain(itemText, 0, itemText.length, textPaint, (contentWidth - textIndent).toInt())
                .build()
            
            checkSpace(textLayout.height.toFloat() + 10f)

            // Draw Checkbox
            canvas.drawRect(margin, yPos, margin + checkboxSize, yPos + checkboxSize, checkboxPaint)
            if (item.isChecked) {
                canvas.drawLine(margin + 2f, yPos + 8f, margin + 6f, yPos + 14f, checkboxPaint)
                canvas.drawLine(margin + 6f, yPos + 14f, margin + 14f, yPos + 4f, checkboxPaint)
            }

            // Draw Text
            canvas.save()
            canvas.translate(margin + textIndent, yPos)
            textLayout.draw(canvas)
            canvas.restore()

            yPos += textLayout.height + 10f
        }
    } else {
        // Draw Rich Text Content
        val spannable = annotatedStringToSpannable(content)
        val contentLayout = StaticLayout.Builder.obtain(spannable, 0, spannable.length, textPaint, contentWidth)
            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.2f)
            .setIncludePad(true)
            .build()
            
        // StaticLayout doesn't support partial drawing easily across pages for flow.
        // We will simple-mindedly start a new page if the WHOLE text doesn't fit? 
        // No, that's bad for long text. 
        // We need to render line by line?
        // Or create a Layout and draw it clipped?
        
        // Better approach for flowing text:
        // Iterate lines of the layout
        
        var currentLine = 0
        while (currentLine < contentLayout.lineCount) {
             val lineTop = contentLayout.getLineTop(currentLine)
             val lineBottom = contentLayout.getLineBottom(currentLine)
             val lineHeight = lineBottom - lineTop
             
             checkSpace(lineHeight.toFloat())
             
             // Draw the specific line
             // We can translate the canvas to -lineTop + yPos and clip?
             // Yes
             
             // How many lines fit on this page?
             val remainingHeight = pageHeight - margin - yPos
             
             // Calculate how many lines we can draw
             var linesToDraw = 0
             var heightToDraw = 0
             
             for (i in currentLine until contentLayout.lineCount) {
                 val h = contentLayout.getLineBottom(i) - contentLayout.getLineTop(i)
                 if (heightToDraw + h > remainingHeight) break
                 heightToDraw += h
                 linesToDraw++
             }
             
             if (linesToDraw == 0) {
                 startNewPage()
                 continue
             }
             
             canvas.save()
             canvas.translate(margin, yPos)
             canvas.clipRect(0, 0, contentWidth, heightToDraw)
             canvas.translate(0f, -contentLayout.getLineTop(currentLine).toFloat())
             contentLayout.draw(canvas)
             canvas.restore()
             
             yPos += heightToDraw
             currentLine += linesToDraw
        }
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