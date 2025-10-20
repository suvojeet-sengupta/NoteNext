package com.suvojeet.notenext.ui.bin

import com.suvojeet.notenext.data.Note

sealed class BinEvent {
    data class RestoreNote(val note: Note) : BinEvent()
    data class DeleteNotePermanently(val note: Note) : BinEvent()
    object EmptyBin : BinEvent()
    data class ToggleNoteSelection(val noteId: Int) : BinEvent()
    object ClearSelection : BinEvent()
    object RestoreSelectedNotes : BinEvent()
    object DeleteSelectedNotesPermanently : BinEvent()
    data class ExpandNote(val noteId: Int) : BinEvent()
    object CollapseNote : BinEvent()
}
