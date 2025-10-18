package com.example.notenext.ui.bin

import com.example.notenext.data.Note

sealed class BinEvent {
    data class RestoreNote(val note: Note) : BinEvent()
    data class DeleteNotePermanently(val note: Note) : BinEvent()
    object EmptyBin : BinEvent()
}
