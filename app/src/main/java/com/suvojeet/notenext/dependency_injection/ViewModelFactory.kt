package com.suvojeet.notenext.dependency_injection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.suvojeet.notenext.data.LabelDao
import com.suvojeet.notenext.data.NoteDao
import com.suvojeet.notenext.data.LinkPreviewRepository
import com.suvojeet.notenext.data.ProjectDao
import com.suvojeet.notenext.ui.archive.ArchiveViewModel
import com.suvojeet.notenext.ui.bin.BinViewModel
import com.suvojeet.notenext.ui.labels.EditLabelsViewModel
import com.suvojeet.notenext.ui.notes.NotesViewModel
import com.suvojeet.notenext.ui.project.ProjectViewModel
import com.suvojeet.notenext.ui.project.ProjectNotesViewModel
import com.suvojeet.notenext.ui.reminder.ReminderViewModel

import android.app.Application
import com.suvojeet.notenext.util.AlarmSchedulerImpl
import com.suvojeet.notenext.ui.setup.SetupViewModel
import com.suvojeet.notenext.ui.settings.BackupRestoreViewModel
import com.suvojeet.notenext.ui.settings.SettingsRepository

class ViewModelFactory(
    val noteDao: NoteDao,
    val labelDao: LabelDao,
    val projectDao: ProjectDao,
    private val linkPreviewRepository: LinkPreviewRepository,
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val savedStateHandle = extras.createSavedStateHandle()
        val alarmScheduler = AlarmSchedulerImpl(application)
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotesViewModel(noteDao, labelDao, projectDao, linkPreviewRepository, alarmScheduler) as T
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
        if (modelClass.isAssignableFrom(ReminderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReminderViewModel(noteDao) as T
        }
        if (modelClass.isAssignableFrom(ProjectViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProjectViewModel(projectDao) as T
        }
        if (modelClass.isAssignableFrom(ProjectNotesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProjectNotesViewModel(noteDao, projectDao, labelDao, linkPreviewRepository, alarmScheduler, savedStateHandle) as T
        }
        if (modelClass.isAssignableFrom(SetupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SetupViewModel(application, SettingsRepository(application)) as T
        }
        if (modelClass.isAssignableFrom(BackupRestoreViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BackupRestoreViewModel(noteDao, labelDao, projectDao, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}