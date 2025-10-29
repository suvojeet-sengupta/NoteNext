package com.suvojeet.notenext.dependency_injection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.suvojeet.notenext.data.LabelDao
import com.suvojeet.notenext.data.NoteDao
import com.suvojeet.notenext.data.LinkPreviewRepository
import com.suvojeet.notenext.ui.archive.ArchiveViewModel
import com.suvojeet.notenext.ui.bin.BinViewModel
import com.suvojeet.notenext.ui.labels.EditLabelsViewModel
import com.suvojeet.notenext.ui.notes.NotesViewModel

import android.content.Context
import com.suvojeet.notenext.util.AlarmSchedulerImpl

class ViewModelFactory(
    val noteDao: NoteDao,
    val labelDao: LabelDao,
    private val linkPreviewRepository: LinkPreviewRepository,
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val savedStateHandle = extras.createSavedStateHandle()
        val alarmScheduler = AlarmSchedulerImpl(context)
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotesViewModel(noteDao, labelDao, linkPreviewRepository, alarmScheduler) as T
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