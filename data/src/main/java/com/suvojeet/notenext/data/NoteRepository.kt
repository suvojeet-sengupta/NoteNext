package com.suvojeet.notenext.data

import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    // Note operations
    fun getNotes(searchQuery: String = "", sortType: SortType = SortType.DATE_MODIFIED): Flow<List<NoteWithAttachments>>
    fun getArchivedNotes(): Flow<List<NoteWithAttachments>>
    fun getBinnedNotes(): Flow<List<NoteWithAttachments>>
    fun getNotesByProjectId(projectId: Int): Flow<List<NoteWithAttachments>>
    suspend fun getNoteById(id: Int): NoteWithAttachments?
    suspend fun insertNote(note: Note): Long
    suspend fun updateNote(note: Note)
    suspend fun deleteNote(note: Note)
    suspend fun emptyBin()
    
    // Attachment operations
    suspend fun insertAttachment(attachment: Attachment)
    suspend fun deleteAttachment(attachment: Attachment)
    suspend fun deleteAttachmentById(attachmentId: Int)
    
    // Label operations
    fun getLabels(): Flow<List<Label>>
    suspend fun insertLabel(label: Label)
    suspend fun updateLabel(label: Label)
    suspend fun deleteLabel(label: Label)
    suspend fun updateLabelName(oldName: String, newName: String)
    suspend fun removeLabelFromNotes(labelName: String)

    // Project operations
    fun getProjects(): Flow<List<Project>>
    suspend fun insertProject(project: Project): Long
    suspend fun deleteProject(projectId: Int)
    suspend fun getProjectById(projectId: Int): Project?

    // Reminder operations
    fun getNotesWithReminders(currentTime: Long): Flow<List<Note>>

    // Checklist operations
    suspend fun insertChecklistItems(items: List<ChecklistItem>)
    suspend fun updateChecklistItem(item: ChecklistItem)
    suspend fun updateChecklistItems(items: List<ChecklistItem>)
    suspend fun deleteChecklistItem(item: ChecklistItem)
    suspend fun deleteChecklistForNote(noteId: Int)
}
