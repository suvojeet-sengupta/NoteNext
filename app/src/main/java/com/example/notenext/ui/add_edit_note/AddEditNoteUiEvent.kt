package com.example.notenext.ui.add_edit_note

sealed interface AddEditNoteUiEvent {
    object OnNoteSaved : AddEditNoteUiEvent
}