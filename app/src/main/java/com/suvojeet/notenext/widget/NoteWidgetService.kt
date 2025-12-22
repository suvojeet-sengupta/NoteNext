package com.suvojeet.notenext.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.data.NoteRepository
import com.suvojeet.notenext.util.HtmlConverter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class NoteWidgetService : RemoteViewsService() {
    @Inject
    lateinit var repository: NoteRepository

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return NoteWidgetRemoteViewsFactory(this.applicationContext, repository)
    }
}

class NoteWidgetRemoteViewsFactory(
    private val context: Context,
    private val repository: NoteRepository
) : RemoteViewsService.RemoteViewsFactory {

    private var notes: List<Note> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        runBlocking {
            try {
                // Fetch Pinned notes first, if empty then recent notes?
                // For now, strict "Pinned Notes" as per XML title.
                val allNotesWithAttachments = repository.getNotes("", com.suvojeet.notenext.data.SortType.DATE_MODIFIED).first()
                notes = allNotesWithAttachments
                        .map { it.note }
                        .filter { it.isPinned && !it.isArchived && !it.isBinned }
            } catch (e: Exception) {
                e.printStackTrace()
                notes = emptyList()
            }
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = notes.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position == -1 || position >= notes.size) return RemoteViews(context.packageName, R.layout.widget_note_item)

        val note = notes[position]
        val views = RemoteViews(context.packageName, R.layout.widget_note_item)

        views.setTextViewText(R.id.widget_item_title, note.title.ifEmpty { "Untitled" })
        
        // HtmlConverter needs Android context, usually safe here.
        val plainContent = try {
            if (note.noteType == "CHECKLIST") "Checklist..." else runBlocking { HtmlConverter.htmlToPlainText(note.content) }
        } catch (e: Exception) {
            ""
        }
        views.setTextViewText(R.id.widget_item_content, plainContent)

        val fillInIntent = Intent().apply {
            putExtra("NOTE_ID", note.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = notes[position].id.toLong()
    override fun hasStableIds(): Boolean = true
}