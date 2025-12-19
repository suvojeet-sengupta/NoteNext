package com.suvojeet.notenext.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val labelDao: LabelDao,
    private val projectDao: ProjectDao
) : NoteRepository {

    override fun getNotes(searchQuery: String, sortType: SortType): Flow<List<NoteWithAttachments>> {
        return if (searchQuery.isBlank()) {
            when (sortType) {
                SortType.DATE_MODIFIED -> noteDao.getNotesOrderedByDateModified()
                SortType.DATE_CREATED -> noteDao.getNotesOrderedByDateCreated()
                SortType.TITLE -> noteDao.getNotesOrderedByTitle()
            }
        } else {
            val formattedQuery = "$searchQuery*"
            when (sortType) {
                SortType.DATE_MODIFIED -> noteDao.searchNotesOrderedByDateModified(formattedQuery)
                SortType.DATE_CREATED -> noteDao.searchNotesOrderedByDateCreated(formattedQuery)
                SortType.TITLE -> noteDao.searchNotesOrderedByTitle(formattedQuery)
            }
        }
    }

    override fun getArchivedNotes(): Flow<List<NoteWithAttachments>> = noteDao.getArchivedNotes()

    override fun getBinnedNotes(): Flow<List<NoteWithAttachments>> = noteDao.getBinnedNotes()

    override fun getNotesByProjectId(projectId: Int): Flow<List<NoteWithAttachments>> = 
        noteDao.getNotesByProjectId(projectId)

    override suspend fun getNoteById(id: Int): NoteWithAttachments? = noteDao.getNoteById(id)

    override suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)

    override suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    override suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    override suspend fun emptyBin() = noteDao.emptyBin()

    override suspend fun insertAttachment(attachment: Attachment) = noteDao.insertAttachment(attachment)

    override suspend fun deleteAttachment(attachment: Attachment) = noteDao.deleteAttachment(attachment)

    override suspend fun deleteAttachmentById(attachmentId: Int) = noteDao.deleteAttachmentById(attachmentId)

    override fun getLabels(): Flow<List<Label>> = labelDao.getLabels()

    override suspend fun insertLabel(label: Label) = labelDao.insertLabel(label)

    override suspend fun updateLabel(label: Label) = labelDao.updateLabel(label)

    override suspend fun deleteLabel(label: Label) = labelDao.deleteLabel(label)

    override suspend fun updateLabelName(oldName: String, newName: String) = 
        noteDao.updateLabelName(oldName, newName)

    override suspend fun removeLabelFromNotes(labelName: String) = 
        noteDao.removeLabelFromNotes(labelName)

    override fun getProjects(): Flow<List<Project>> = projectDao.getProjects()

    override suspend fun insertProject(project: Project): Long = projectDao.insertProject(project)

    override suspend fun deleteProject(projectId: Int) = projectDao.deleteProject(projectId)

    override suspend fun getProjectById(projectId: Int): Project? = projectDao.getProjectById(projectId)

    override fun getNotesWithReminders(currentTime: Long): Flow<List<Note>> = 
        noteDao.getNotesWithReminders(currentTime)
}
