package com.suvojeet.notenext.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.withLock
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.suvojeet.notenext.data.backup.BackupWorker
import com.suvojeet.notenext.util.CryptoUtils
import com.suvojeet.notenext.core.util.SortType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val db: NoteDatabase,
    private val noteDao: NoteDao,
    private val labelDao: LabelDao,
    private val projectDao: ProjectDao,
    private val checklistItemDao: ChecklistItemDao,
    @ApplicationContext private val context: Context
) : NoteRepository {

    private val editCounterMutex = kotlinx.coroutines.sync.Mutex()

    override suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return db.withTransaction {
            block()
        }
    }

    override fun getNotes(searchQuery: String, sortType: SortType, projectId: Int?, isDecoy: Boolean): Flow<List<NoteWithAttachments>> {
        val flow = if (searchQuery.isBlank()) {
            when (sortType) {
                SortType.DATE_MODIFIED -> noteDao.getNotesOrderedByDateModified(projectId, isDecoy)
                SortType.DATE_CREATED -> noteDao.getNotesOrderedByDateCreated(projectId, isDecoy)
                SortType.TITLE -> noteDao.getNotesOrderedByTitle(projectId, isDecoy)
                SortType.CUSTOM -> noteDao.getNotesOrderedByPosition(projectId, isDecoy)
            }
        } else {
            val formattedQuery = "$searchQuery*"
            when (sortType) {
                SortType.DATE_MODIFIED -> noteDao.searchNotesOrderedByDateModified(formattedQuery, projectId, isDecoy)
                SortType.DATE_CREATED -> noteDao.searchNotesOrderedByDateCreated(formattedQuery, projectId, isDecoy)
                SortType.TITLE -> noteDao.searchNotesOrderedByTitle(formattedQuery, projectId, isDecoy)
                SortType.CUSTOM -> noteDao.searchNotesOrderedByPosition(formattedQuery, projectId, isDecoy)
            }
        }
        return flow
    }

    override fun getPinnedNotes(searchQuery: String, projectId: Int?, isDecoy: Boolean): Flow<List<NoteWithAttachments>> {
        return if (searchQuery.isBlank()) {
            noteDao.getPinnedNotes(projectId, isDecoy)
        } else {
            val formattedQuery = "$searchQuery*"
            noteDao.searchPinnedNotes(formattedQuery, projectId, isDecoy)
        }
    }

    override fun getPinnedNoteSummaries(searchQuery: String, projectId: Int?, isDecoy: Boolean): Flow<List<NoteSummaryWithAttachments>> {
        return if (searchQuery.isBlank()) {
            noteDao.getPinnedNoteSummaries(projectId, isDecoy)
        } else {
            val formattedQuery = "$searchQuery*"
            noteDao.searchPinnedNoteSummaries(formattedQuery, projectId, isDecoy)
        }
    }

    override fun getOtherNotesPaged(searchQuery: String, sortType: SortType, projectId: Int?, isDecoy: Boolean): Flow<PagingData<NoteWithAttachments>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = true),
            pagingSourceFactory = {
                if (searchQuery.isBlank()) {
                    when (sortType) {
                        SortType.DATE_MODIFIED -> noteDao.getOtherNotesPagedOrderedByDateModified(projectId, isDecoy)
                        SortType.DATE_CREATED -> noteDao.getOtherNotesPagedOrderedByDateCreated(projectId, isDecoy)
                        SortType.TITLE -> noteDao.getOtherNotesPagedOrderedByTitle(projectId, isDecoy)
                        SortType.CUSTOM -> noteDao.getOtherNotesPagedOrderedByPosition(projectId, isDecoy)
                    }
                } else {
                    val formattedQuery = "$searchQuery*"
                    when (sortType) {
                        SortType.DATE_MODIFIED -> noteDao.searchOtherNotesPagedOrderedByDateModified(formattedQuery, projectId, isDecoy)
                        SortType.DATE_CREATED -> noteDao.searchOtherNotesPagedOrderedByDateCreated(formattedQuery, projectId, isDecoy)
                        SortType.TITLE -> noteDao.searchOtherNotesPagedOrderedByTitle(formattedQuery, projectId, isDecoy)
                        SortType.CUSTOM -> noteDao.searchOtherNotesPagedOrderedByPosition(formattedQuery, projectId, isDecoy)
                    }
                }
            }
        ).flow
    }

    override fun getOtherNoteSummariesPaged(searchQuery: String, sortType: SortType, projectId: Int?, isDecoy: Boolean): Flow<PagingData<NoteSummaryWithAttachments>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = true),
            pagingSourceFactory = {
                if (searchQuery.isBlank()) {
                    when (sortType) {
                        SortType.DATE_MODIFIED -> noteDao.getOtherNoteSummariesPagedOrderedByDateModified(projectId, isDecoy)
                        SortType.DATE_CREATED -> noteDao.getOtherNoteSummariesPagedOrderedByDateCreated(projectId, isDecoy)
                        SortType.TITLE -> noteDao.getOtherNoteSummariesPagedOrderedByTitle(projectId, isDecoy)
                        SortType.CUSTOM -> noteDao.getOtherNoteSummariesPagedOrderedByPosition(projectId, isDecoy)
                    }
                } else {
                    val formattedQuery = "$searchQuery*"
                    when (sortType) {
                        SortType.DATE_MODIFIED -> noteDao.searchOtherNoteSummariesPagedOrderedByDateModified(formattedQuery, projectId, isDecoy)
                        SortType.DATE_CREATED -> noteDao.searchOtherNoteSummariesPagedOrderedByDateCreated(formattedQuery, projectId, isDecoy)
                        SortType.TITLE -> noteDao.searchOtherNoteSummariesPagedOrderedByTitle(formattedQuery, projectId, isDecoy)
                        SortType.CUSTOM -> noteDao.searchOtherNoteSummariesPagedOrderedByPosition(formattedQuery, projectId, isDecoy)
                    }
                }
            }
        ).flow
    }

    override fun getArchivedNoteSummaries(isDecoy: Boolean): Flow<List<NoteSummaryWithAttachments>> = 
        noteDao.getArchivedNoteSummaries(isDecoy)

    override fun getBinnedNoteSummaries(isDecoy: Boolean): Flow<List<NoteSummaryWithAttachments>> = 
        noteDao.getBinnedNoteSummaries(isDecoy)

    override fun getNoteSummariesByProjectId(projectId: Int, isDecoy: Boolean): Flow<List<NoteSummaryWithAttachments>> = 
        noteDao.getNoteSummariesByProjectId(projectId, isDecoy)

    override suspend fun getAllNoteIds(searchQuery: String, projectId: Int?, isDecoy: Boolean): List<Int> {
        return if (searchQuery.isBlank()) {
            noteDao.getAllNoteIds(projectId, isDecoy)
        } else {
            noteDao.searchAllNoteIds(searchQuery, projectId, isDecoy)
        }
    }

    override fun getArchivedNotes(isDecoy: Boolean): Flow<List<NoteWithAttachments>> = 
        noteDao.getArchivedNotes(isDecoy)

    override fun getBinnedNotes(isDecoy: Boolean): Flow<List<NoteWithAttachments>> = 
        noteDao.getBinnedNotes(isDecoy)

    override fun getNotesByProjectId(projectId: Int, isDecoy: Boolean): Flow<List<NoteWithAttachments>> = 
        noteDao.getNotesByProjectId(projectId, isDecoy)

    override fun getNotesModifiedSince(timestamp: Long): Flow<List<NoteWithAttachments>> =
        noteDao.getNotesModifiedSince(timestamp)

    override suspend fun getNoteById(id: Int): NoteWithAttachments? = 
        noteDao.getNoteById(id)?.let { 
            // We return it AS IS if locked, so the caller can trigger biometric auth
            if (it.note.isLocked) {
                it
            } else {
                val decryptedNote = CryptoUtils.decryptNote(it.note)
                val decryptedChecklist = it.checklistItems.map { item -> 
                    CryptoUtils.decryptChecklistItem(item, isLocked = false) 
                }
                it.copy(note = decryptedNote, checklistItems = decryptedChecklist)
            }
        }

    private suspend fun incrementEditCounter() {
        editCounterMutex.withLock {
            val sharedPrefs = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
            val smartBackupEnabled = sharedPrefs.getBoolean("smart_backup_enabled", false)
            if (!smartBackupEnabled) return@withLock

            val currentCount = sharedPrefs.getInt("edit_counter", 0) + 1
            val threshold = sharedPrefs.getInt("edits_before_backup", 10)

            if (currentCount >= threshold) {
                // Trigger backup
                val email = sharedPrefs.getString("google_account_email", null)
                val inputData = androidx.work.Data.Builder()
                if (email != null) inputData.putString("email", email)
                
                val workRequest = OneTimeWorkRequestBuilder<BackupWorker>()
                    .setInputData(inputData.build())
                    .build()
                
                WorkManager.getInstance(context).enqueue(workRequest)
                sharedPrefs.edit().putInt("edit_counter", 0).apply()
            } else {
                sharedPrefs.edit().putInt("edit_counter", currentCount).apply()
            }
        }
    }

    override suspend fun insertNote(note: Note): Long {
        val noteToInsert = if (note.isLocked) CryptoUtils.encryptNote(note) else note
        val id = noteDao.insertNote(noteToInsert)
        if (id > 0) incrementEditCounter()
        return id
    }

    override suspend fun updateNote(note: Note) {
        val noteToSave = if (note.isLocked && !note.isEncrypted) {
            CryptoUtils.encryptNote(note)
        } else {
            note
        }
        noteDao.updateNote(noteToSave)
        incrementEditCounter()
    }

    override suspend fun updateNotePosition(id: Int, position: Int) = noteDao.updateNotePosition(id, position)

    override suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    override suspend fun emptyBin() = noteDao.emptyBin()

    override suspend fun insertAttachment(attachment: Attachment) = noteDao.insertAttachment(attachment)

    override suspend fun deleteAttachment(attachment: Attachment) = noteDao.deleteAttachment(attachment)

    override suspend fun deleteAttachmentById(attachmentId: Int) = noteDao.deleteAttachmentById(attachmentId)

    override fun getLabels(): Flow<List<Label>> = labelDao.getLabels()

    override fun getLabelsWithParent(parentName: String): Flow<List<Label>> = labelDao.getLabelsWithParent(parentName)

    override fun getRootLabels(): Flow<List<Label>> = labelDao.getRootLabels()

    override suspend fun insertLabel(label: Label) = labelDao.insertLabel(label)

    override suspend fun updateLabel(label: Label) = labelDao.updateLabel(label)

    override suspend fun deleteLabel(label: Label) = labelDao.deleteLabel(label)

    override suspend fun updateLabelName(oldName: String, newName: String) = 
        noteDao.updateLabelName(oldName, newName)

    override suspend fun removeLabelFromNotes(labelName: String) = 
        noteDao.removeLabelFromNotes(labelName)

    override fun getProjects(): Flow<List<Project>> = projectDao.getAllProjects()

    override fun getRootProjects(): Flow<List<Project>> = projectDao.getRootProjects()

    override fun getSubProjects(parentId: Int): Flow<List<Project>> = projectDao.getSubProjects(parentId)

    override fun getProjectHierarchy(): Flow<List<Project>> = projectDao.getProjectHierarchy()

    override suspend fun insertProject(project: Project): Long = projectDao.insertProject(project)

    override suspend fun updateProject(project: Project) = projectDao.updateProject(project)

    override suspend fun deleteProject(projectId: Int) = projectDao.deleteProject(projectId)

    override suspend fun getProjectById(projectId: Int): Project? = projectDao.getProjectById(projectId)

    override suspend fun moveProject(projectId: Int, newParentId: Int?) {
        projectDao.moveProjectToParent(projectId, newParentId)
    }

    override suspend fun reorderProject(projectId: Int, newOrder: Int) {
        projectDao.updateProjectOrder(projectId, newOrder)
    }

    override fun getNotesWithReminders(currentTime: Long): Flow<List<Note>> = 
        noteDao.getNotesWithReminders(currentTime)

    override fun getAllReminders(): Flow<List<Note>> = noteDao.getAllReminders()

    override suspend fun insertChecklistItems(items: List<ChecklistItem>) {
        if (items.isEmpty()) return
        val noteId = items.first().noteId
        val isLocked = noteDao.isNoteLocked(noteId)
        val encryptedItems = if (isLocked) {
            items.map { CryptoUtils.encryptChecklistItem(it, isLocked) }
        } else {
            items
        }
        checklistItemDao.insertChecklistItems(encryptedItems)
    }

    override suspend fun updateChecklistItem(item: ChecklistItem) {
        val isLocked = noteDao.isNoteLocked(item.noteId)
        val itemToUpdate = if (isLocked) CryptoUtils.encryptChecklistItem(item, isLocked) else item
        checklistItemDao.updateChecklistItem(itemToUpdate)
    }

    override suspend fun updateChecklistItemStatus(id: String, isChecked: Boolean) {
        checklistItemDao.updateChecklistItemStatus(id, isChecked)
    }

    override suspend fun updateChecklistItems(items: List<ChecklistItem>) {
        if (items.isEmpty()) return
        val noteId = items.first().noteId
        val isLocked = noteDao.isNoteLocked(noteId)
        val itemsToUpdate = if (isLocked) {
            items.map { CryptoUtils.encryptChecklistItem(it, isLocked) }
        } else {
            items
        }
        checklistItemDao.updateChecklistItems(itemsToUpdate)
    }

    override suspend fun deleteChecklistItem(item: ChecklistItem) = checklistItemDao.deleteChecklistItem(item)

    override suspend fun deleteChecklistForNote(noteId: Int) = checklistItemDao.deleteChecklistForNote(noteId)

    override suspend fun insertNoteVersion(version: NoteVersion) {
        val isLocked = noteDao.isNoteLocked(version.noteId)
        val versionToInsert = if (isLocked) CryptoUtils.encryptNoteVersion(version, isLocked) else version
        noteDao.insertNoteVersion(versionToInsert)
    }

    override fun getNoteVersions(noteId: Int): Flow<List<NoteVersion>> {
        return noteDao.getNoteVersions(noteId)
    }

    override suspend fun limitNoteVersions(noteId: Int, limit: Int) = noteDao.limitNoteVersions(noteId, limit)

    override suspend fun getNoteIdByTitle(title: String): Int? = noteDao.getNoteIdByTitle(title)

    override suspend fun getNoteByTitleAndCreatedAt(title: String, createdAt: Long): Note? =
        noteDao.getNoteByTitleAndCreatedAt(title, createdAt)

    override fun getAllNotes(): Flow<List<NoteWithAttachments>> = noteDao.getAllNotes()

    override fun getAllNotesModifiedSince(timestamp: Long): Flow<List<NoteWithAttachments>> =
        noteDao.getAllNotesModifiedSince(timestamp)
}
