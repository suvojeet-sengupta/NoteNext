package com.suvojeet.notenext.ui.labels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.notenext.data.Label
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditLabelsViewModel @Inject constructor(private val repository: com.suvojeet.notenext.data.NoteRepository) : ViewModel() {

    private val _state = MutableStateFlow(EditLabelsState())
    val state = _state.asStateFlow()

    init {
        repository.getLabels()
            .onEach { labels ->
                _state.update { it.copy(labels = labels) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: EditLabelsEvent) {
        when (event) {
            is EditLabelsEvent.AddLabel -> {
                viewModelScope.launch {
                    repository.insertLabel(Label(event.name))
                    _state.update { it.copy(showAddLabelDialog = false) }
                }
            }
            is EditLabelsEvent.UpdateLabel -> {
                viewModelScope.launch {
                    repository.insertLabel(Label(event.newName))
                    repository.updateLabelName(event.oldLabel.name, event.newName)
                    repository.deleteLabel(event.oldLabel)
                    _state.update { it.copy(showEditLabelDialog = false, selectedLabel = null) }
                }
            }
            is EditLabelsEvent.DeleteLabel -> {
                viewModelScope.launch {
                    repository.removeLabelFromNotes(event.label.name)
                    repository.deleteLabel(event.label)
                    _state.update { it.copy(showEditLabelDialog = false, selectedLabel = null) }
                }
            }
            is EditLabelsEvent.ShowAddLabelDialog -> {
                _state.update { it.copy(showAddLabelDialog = true) }
            }
            is EditLabelsEvent.ShowEditLabelDialog -> {
                _state.update { it.copy(showEditLabelDialog = true, selectedLabel = event.label) }
            }
            is EditLabelsEvent.HideDialog -> {
                _state.update { it.copy(showAddLabelDialog = false, showEditLabelDialog = false, selectedLabel = null) }
            }
            is EditLabelsEvent.OnSearchQueryChange -> {
                _state.update { it.copy(searchQuery = event.query) }
            }
            is EditLabelsEvent.OnSearchVisibilityChange -> {
                _state.update { it.copy(isSearchVisible = event.isVisible, searchQuery = if (!event.isVisible) "" else it.searchQuery) }
            }
        }
    }
}
