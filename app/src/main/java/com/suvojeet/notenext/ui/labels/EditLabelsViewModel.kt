package com.suvojeet.notenext.ui.labels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.notenext.data.Label
import com.suvojeet.notenext.data.LabelDao
import com.suvojeet.notenext.data.NoteDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditLabelsViewModel @Inject constructor(private val repository: com.suvojeet.notenext.data.NoteRepository) : ViewModel() {

    private val _state = MutableStateFlow(EditLabelsState())
    val state = _state.asStateFlow()

    init {
        repository.getLabels()
            .onEach { labels ->
                _state.value = _state.value.copy(labels = labels)
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: EditLabelsEvent) {
        when (event) {
            is EditLabelsEvent.AddLabel -> {
                viewModelScope.launch {
                    repository.insertLabel(Label(event.name))
                    _state.value = _state.value.copy(showAddLabelDialog = false)
                }
            }
            is EditLabelsEvent.UpdateLabel -> {
                viewModelScope.launch {
                    // 1. Create new label
                    repository.insertLabel(Label(event.newName))
                    // 2. Update notes
                    repository.updateLabelName(event.oldLabel.name, event.newName)
                    // 3. Delete old label
                    repository.deleteLabel(event.oldLabel)

                    _state.value = _state.value.copy(showEditLabelDialog = false, selectedLabel = null)
                }
            }
            is EditLabelsEvent.DeleteLabel -> {
                viewModelScope.launch {
                    repository.removeLabelFromNotes(event.label.name)
                    repository.deleteLabel(event.label)
                    _state.value = _state.value.copy(showEditLabelDialog = false, selectedLabel = null)
                }
            }
            is EditLabelsEvent.ShowAddLabelDialog -> {
                _state.value = _state.value.copy(showAddLabelDialog = true)
            }
            is EditLabelsEvent.ShowEditLabelDialog -> {
                _state.value = _state.value.copy(showEditLabelDialog = true, selectedLabel = event.label)
            }
            is EditLabelsEvent.HideDialog -> {
                _state.value = _state.value.copy(showAddLabelDialog = false, showEditLabelDialog = false, selectedLabel = null)
            }
        }
    }
}
