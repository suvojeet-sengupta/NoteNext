
package com.suvojeet.notenext.ui.notes.delegate

import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.suvojeet.notenext.data.NoteRepository
import com.suvojeet.notenext.core.util.SortType
import com.suvojeet.notenext.core.util.NoteSearchState
import com.suvojeet.notenext.ui.notes.NotesListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.persistentListOf
import javax.inject.Inject

class NoteListDelegate @Inject constructor(
    private val repository: NoteRepository
) {
    private val _listState = MutableStateFlow(NotesListState())
    val listState = _listState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _sortType = MutableStateFlow(SortType.DATE_MODIFIED)
    private val _filteredProjectId = MutableStateFlow<Int?>(null)
    private val _isDecoy = MutableStateFlow(false)

    fun observeNotes(scope: CoroutineScope) {
        // Removed search-query Log.d — it leaked the user's query to logcat in release.
        val combinedFlow = combine(
            _searchQuery,
            _sortType,
            _filteredProjectId,
            _isDecoy
        ) { query: String, sort: SortType, project: Int?, decoy: Boolean ->
            NoteSearchState(query, sort, project, decoy)
        }.distinctUntilChanged()

        combinedFlow.flatMapLatest { state: NoteSearchState ->
            repository.getPinnedNoteSummaries(state.query, state.projectId, state.isDecoy)
        }.onEach { pinned ->
            _listState.update { it.copy(pinnedNotes = pinned.toImmutableList()) }
        }.launchIn(scope)

        // The pager itself is built ONCE: a stable cachedIn flow that swaps inner sources
        // via flatMapLatest as search/sort/project/decoy change. Previously we built a
        // brand-new Pager + cachedIn() on every keystroke and stored the new flow inside
        // listState — old collectors leaked for the lifetime of the ViewModel and the
        // UI saw a fresh PagingData on each character typed.
        val pagedFlow = combinedFlow.flatMapLatest { state: NoteSearchState ->
            repository.getOtherNoteSummariesPaged(state.query, state.sortType, state.projectId, state.isDecoy)
        }.cachedIn(scope)

        _listState.update { it.copy(pagedNotes = pagedFlow) }

        combinedFlow.onEach { state: NoteSearchState ->
            _listState.update {
                it.copy(
                    searchQuery = state.query,
                    sortType = state.sortType,
                    filteredProjectId = state.projectId
                )
            }
        }.launchIn(scope)

        repository.getLabels().onEach { labels ->
            val labelNames = labels.map { it.name }.toImmutableList()
            _listState.update { it.copy(labels = labelNames) }
        }.launchIn(scope)

        repository.getProjects().onEach { projects ->
            _listState.update { it.copy(projects = projects.toImmutableList(), isLoading = false) }
        }.launchIn(scope)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortType(sortType: SortType) {
        _sortType.value = sortType
    }

    fun setProjectId(projectId: Int?) {
        _filteredProjectId.value = projectId
    }

    fun setDecoyMode(isDecoy: Boolean) {
        _isDecoy.value = isDecoy
    }

    fun toggleSelection(noteId: Int) {
        _listState.update { state ->
            val selectedIds = state.selectedNoteIds.toMutableList()
            if (selectedIds.contains(noteId)) {
                selectedIds.remove(noteId)
            } else {
                selectedIds.add(noteId)
            }
            state.copy(selectedNoteIds = selectedIds.toImmutableList())
        }
    }

    fun clearSelection() {
        _listState.update { it.copy(selectedNoteIds = persistentListOf()) }
    }

    fun updateState(transform: (NotesListState) -> NotesListState) {
        _listState.update(transform)
    }
}
