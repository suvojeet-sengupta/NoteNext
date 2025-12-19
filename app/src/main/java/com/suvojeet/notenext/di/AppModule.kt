package com.suvojeet.notenext.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.suvojeet.notenext.data.ChecklistItemDao
import com.suvojeet.notenext.data.LabelDao
import com.suvojeet.notenext.data.LinkPreviewRepository
import com.suvojeet.notenext.data.NoteDao
import com.suvojeet.notenext.data.NoteDatabase
import com.suvojeet.notenext.data.ProjectDao
import com.suvojeet.notenext.ui.settings.SettingsRepository
import com.suvojeet.notenext.util.AlarmScheduler
import com.suvojeet.notenext.util.AlarmSchedulerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNoteDatabase(app: Application): NoteDatabase {
        // NoteDatabase.getDatabase(app) handles the singleton logic internally,
        // but for Hilt we can just call it or use Room.databaseBuilder directly.
        // Since NoteDatabase.getDatabase includes migrations, it's safer to use that for now to ensure consistency.
        return NoteDatabase.getDatabase(app)
    }

    @Provides
    @Singleton
    fun provideNoteDao(db: NoteDatabase): NoteDao {
        return db.noteDao()
    }

    @Provides
    @Singleton
    fun provideLabelDao(db: NoteDatabase): LabelDao {
        return db.labelDao()
    }

    @Provides
    @Singleton
    fun provideProjectDao(db: NoteDatabase): ProjectDao {
        return db.projectDao()
    }

    @Provides
    @Singleton
    fun provideChecklistItemDao(db: NoteDatabase): ChecklistItemDao {
        return db.checklistItemDao()
    }

    @Provides
    @Singleton
    fun provideNoteRepository(
        noteDao: NoteDao,
        labelDao: LabelDao,
        projectDao: ProjectDao,
        checklistItemDao: ChecklistItemDao
    ): com.suvojeet.notenext.data.NoteRepository {
        return com.suvojeet.notenext.data.NoteRepositoryImpl(noteDao, labelDao, projectDao, checklistItemDao)
    }

    @Provides
    @Singleton
    fun provideAlarmScheduler(@ApplicationContext context: Context): AlarmScheduler {
        return AlarmSchedulerImpl(context)
    }

    @Provides
    @Singleton
    fun provideLinkPreviewRepository(): LinkPreviewRepository {
        return LinkPreviewRepository()
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepository(context)
    }
}
