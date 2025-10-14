
package com.example.notesapp.ui.add_edit_note

sealed class AddEditNoteEvent {
    data class OnTitleChange(val title: String) : AddEditNoteEvent()
    data class OnContentChange(val content: String) : AddEditNoteEvent()
    data class OnColorChange(val color: Int) : AddEditNoteEvent()
    object OnSaveNoteClick : AddEditNoteEvent()
}
