
package com.example.notesapp.ui.add_edit_note

data class AddEditNoteState(
    val title: String = "",
    val content: String = "",
    val isNoteSaved: Boolean = false
)
