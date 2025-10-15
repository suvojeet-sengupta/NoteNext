package com.example.notesapp.ui.add_edit_note

sealed interface AddEditNoteUiEvent {
    object OnNoteSaved : AddEditNoteUiEvent
}