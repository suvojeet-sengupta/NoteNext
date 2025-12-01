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
class EditLabelsViewModel @Inject constructor(private val labelDao: LabelDao, private val noteDao: NoteDao) : ViewModel() {

    private val _state = MutableStateFlow(EditLabelsState())
    val state = _state.asStateFlow()

    init {
        labelDao.getLabels()
            .onEach { labels ->
                _state.value = _state.value.copy(labels = labels)
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: EditLabelsEvent) {
        when (event) {
            is EditLabelsEvent.AddLabel -> {
                viewModelScope.launch {
                    labelDao.insertLabel(Label(event.name))
                    _state.value = _state.value.copy(showAddLabelDialog = false)
                }
            }
            is EditLabelsEvent.UpdateLabel -> {
                viewModelScope.launch {
                    // 1. Create new label
                    labelDao.insertLabel(Label(event.newName))
                    // 2. Update notes
                    noteDao.updateLabelName(event.oldLabel.name, event.newName)
                    // 3. Delete old label
                    labelDao.deleteLabel(event.oldLabel)

                    _state.value = _state.value.copy(showEditLabelDialog = false, selectedLabel = null)
                }
            }
            is EditLabelsEvent.DeleteLabel -> {
                viewModelScope.launch {
                    noteDao.removeLabelFromNotes(event.label.name)
                    labelDao.deleteLabel(event.label)
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
