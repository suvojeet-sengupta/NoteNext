package com.suvojeet.notenext.ui.add_edit_note.components

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.notes.NotesEvent
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

class CustomTextToolbar(private val view: View, private val onMoreOptionsClick: () -> Unit, private val onEvent: (NotesEvent) -> Unit, private val textFieldValue: TextFieldValue, private val clipboardManager: androidx.compose.ui.platform.ClipboardManager) : TextToolbar {
    private var actionMode: ActionMode? = null
    private val systemClipboardManager = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        val activity = view.context as? Activity ?: return

        actionMode = activity.startActionMode(object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                menu.add(0, android.R.id.copy, 0, "Copy").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                menu.add(0, android.R.id.paste, 0, "Paste").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                menu.add(0, android.R.id.cut, 0, "Cut").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                menu.add(0, R.id.more_options, 0, "More").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                when (item.itemId) {
                    android.R.id.copy -> {
                        val selectedText = textFieldValue.text.substring(textFieldValue.selection.min, textFieldValue.selection.max)
                        clipboardManager.setText(AnnotatedString(selectedText))
                        val clip = ClipData.newPlainText("note", selectedText)
                        systemClipboardManager.setPrimaryClip(clip)
                        onEvent(NotesEvent.OnCopyText(selectedText))
                    }
                    android.R.id.paste -> {
                        val clipboardText = systemClipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
                        clipboardText?.let { onEvent(NotesEvent.OnPasteText(it)) }
                    }
                    android.R.id.cut -> {
                        val selectedText = textFieldValue.text.substring(textFieldValue.selection.min, textFieldValue.selection.max)
                        clipboardManager.setText(AnnotatedString(selectedText))
                        val clip = ClipData.newPlainText("note", selectedText)
                        systemClipboardManager.setPrimaryClip(clip)
                        onEvent(NotesEvent.OnCutText)
                    }
                    R.id.more_options -> onMoreOptionsClick()
                }
                mode.finish()
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                val hasText = systemClipboardManager.primaryClip?.getItemAt(0)?.text != null
                menu.findItem(android.R.id.paste)?.isEnabled = hasText
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                actionMode = null
            }
        })
    }

    override fun hide() {
        actionMode?.finish()
        actionMode = null
    }

    override val status: TextToolbarStatus
        get() = if (actionMode != null) TextToolbarStatus.Shown else TextToolbarStatus.Hidden
}
