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
import com.suvojeet.notenext.data.TodoDao
import com.suvojeet.notenext.data.TodoRepository
import com.suvojeet.notenext.data.TodoRepositoryImpl
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.data.ReminderScheduler
import com.suvojeet.notenext.data.AlarmManagerScheduler
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
        return Room.databaseBuilder(
            app,
            NoteDatabase::class.java,
            "note_database"
        ).addMigrations(
            NoteDatabase.MIGRATION_1_2,
            NoteDatabase.MIGRATION_2_3,
            NoteDatabase.MIGRATION_3_4,
            NoteDatabase.MIGRATION_4_5,
            NoteDatabase.MIGRATION_5_6,
            NoteDatabase.MIGRATION_6_7,
            NoteDatabase.MIGRATION_7_8,
            NoteDatabase.MIGRATION_8_9,
            NoteDatabase.MIGRATION_9_10,
            NoteDatabase.MIGRATION_10_11,
            NoteDatabase.MIGRATION_11_12,
            NoteDatabase.MIGRATION_12_13,
            NoteDatabase.MIGRATION_13_14,
            NoteDatabase.MIGRATION_14_15,
            NoteDatabase.MIGRATION_15_16,
            NoteDatabase.MIGRATION_16_17,
            NoteDatabase.MIGRATION_17_18,
            NoteDatabase.MIGRATION_18_19,
            NoteDatabase.MIGRATION_19_20,
            NoteDatabase.MIGRATION_20_21,
            NoteDatabase.MIGRATION_21_22,
            NoteDatabase.MIGRATION_22_23,
            NoteDatabase.MIGRATION_23_24,
            NoteDatabase.MIGRATION_24_25,
            NoteDatabase.MIGRATION_25_26,
            NoteDatabase.MIGRATION_26_27,
            NoteDatabase.MIGRATION_27_28,
            NoteDatabase.MIGRATION_28_29,
            NoteDatabase.MIGRATION_29_30,
            NoteDatabase.MIGRATION_30_31,
            NoteDatabase.MIGRATION_31_32
        )
            // Installing an older APK over a newer one (DB downgrade) would otherwise throw
            // IllegalStateException on launch and crash-loop the app. Rebuild the DB instead
            // of crashing. Upgrades remain non-destructive via the migrations above.
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()
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
    fun provideTodoDao(db: NoteDatabase): TodoDao {
        return db.todoDao()
    }

    @Provides
    @Singleton
    fun provideAIUsageDao(db: NoteDatabase): com.suvojeet.notenext.data.ai.AIUsageDao {
        return db.aiUsageDao()
    }

    @Provides
    @Singleton
    fun provideNoteRepository(
        db: NoteDatabase,
        noteDao: NoteDao,
        labelDao: LabelDao,
        projectDao: ProjectDao,
        checklistItemDao: ChecklistItemDao,
        backupSettingsRepository: com.suvojeet.notenext.data.repository.BackupSettingsRepository,
        @ApplicationContext context: Context
    ): com.suvojeet.notenext.data.NoteRepository {
        return com.suvojeet.notenext.data.NoteRepositoryImpl(db, noteDao, labelDao, projectDao, checklistItemDao, context, backupSettingsRepository)
    }

    @Provides
    @Singleton
    fun provideTodoRepository(todoDao: TodoDao): TodoRepository {
        return TodoRepositoryImpl(todoDao)
    }

    @Provides
    @Singleton
    fun provideReminderScheduler(@ApplicationContext context: Context): ReminderScheduler {
        return AlarmManagerScheduler(context)
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

    @Provides
    @Singleton
    fun provideUpdateChecker(@ApplicationContext context: Context): com.suvojeet.notenext.util.UpdateChecker {
        return com.suvojeet.notenext.util.UpdateChecker(context)
    }

    @Provides
    @Singleton
    fun provideReviewManager(repository: com.suvojeet.notenext.data.repository.ReviewSettingsRepository): com.suvojeet.notenext.util.ReviewManager {
        return com.suvojeet.notenext.util.ReviewManager(repository)
    }
}

