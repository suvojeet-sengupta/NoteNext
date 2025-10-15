
package com.example.notenext.dependency_injection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.notenext.data.NoteDao
import com.example.notenext.ui.add_edit_note.AddEditNoteViewModel
import com.example.notenext.ui.notes.NotesViewModel

class ViewModelFactory(private val noteDao: NoteDao) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val savedStateHandle = extras.createSavedStateHandle()
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotesViewModel(noteDao) as T
        }
        if (modelClass.isAssignableFrom(AddEditNoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddEditNoteViewModel(noteDao, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
