
package com.example.notesapp.di

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.example.notesapp.data.NoteDao
import com.example.notesapp.ui.add_edit_note.AddEditNoteViewModel
import com.example.notesapp.ui.notes.NotesViewModel

class ViewModelFactory(
    owner: SavedStateRegistryOwner,
    private val noteDao: NoteDao,
    defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotesViewModel(noteDao) as T
        }
        if (modelClass.isAssignableFrom(AddEditNoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddEditNoteViewModel(noteDao, handle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
