package com.example.notenext.ui.notes

sealed class NotesUiEvent {
    data class SendNotes(val title: String, val content: String) : NotesUiEvent()
}