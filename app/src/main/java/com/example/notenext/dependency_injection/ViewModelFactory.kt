package com.example.notenext.dependency_injection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.notenext.data.LabelDao
import com.example.notenext.data.NoteDao
import com.example.notenext.data.LinkPreviewRepository
import com.example.notenext.ui.archive.ArchiveViewModel
import com.example.notenext.ui.bin.BinViewModel
import com.example.notenext.ui.labels.EditLabelsViewModel
import com.example.notenext.ui.notes.NotesViewModel

class ViewModelFactory(
    val noteDao: NoteDao,
    val labelDao: LabelDao,
    private val linkPreviewRepository: LinkPreviewRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val savedStateHandle = extras.createSavedStateHandle()
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotesViewModel(noteDao, labelDao, linkPreviewRepository) as T
        }
        if (modelClass.isAssignableFrom(ArchiveViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ArchiveViewModel(noteDao) as T
        }
        if (modelClass.isAssignableFrom(EditLabelsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditLabelsViewModel(labelDao, noteDao) as T
        }
        if (modelClass.isAssignableFrom(BinViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BinViewModel(noteDao, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}