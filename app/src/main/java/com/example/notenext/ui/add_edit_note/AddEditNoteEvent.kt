
package com.example.notenext.ui.add_edit_note

sealed interface AddEditNoteEvent {
    data class OnTitleChange(val title: String) : AddEditNoteEvent
    data class OnContentChange(val content: String) : AddEditNoteEvent
    data class OnColorChange(val color: Int) : AddEditNoteEvent
    object OnSaveNoteClick : AddEditNoteEvent
    object OnDeleteNoteClick : AddEditNoteEvent
}
