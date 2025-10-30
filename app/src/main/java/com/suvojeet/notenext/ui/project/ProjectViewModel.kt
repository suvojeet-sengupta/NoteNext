package com.suvojeet.notenext.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.notenext.data.ProjectDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ProjectViewModel(
    private val projectDao: ProjectDao
) : ViewModel() {

    private val _state = MutableStateFlow(ProjectState())
    val state = _state.asStateFlow()

    init {
        projectDao.getProjects().onEach { projects ->
            _state.value = _state.value.copy(projects = projects)
        }.launchIn(viewModelScope)
    }
}
