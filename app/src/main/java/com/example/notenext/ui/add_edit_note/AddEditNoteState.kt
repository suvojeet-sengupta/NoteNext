
package com.example.notenext.ui.add_edit_note

data class AddEditNoteState(
    val title: String = "",
    val content: String = "",
    val createdAt: Long = 0,
    val lastEdited: Long = 0,
    val color: Int = 0,
    val isNewNote: Boolean = true
)
