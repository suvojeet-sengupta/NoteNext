package com.suvojeet.notenext.ui.project

import androidx.compose.runtime.Immutable
import com.suvojeet.notenext.data.Project

@Immutable
data class ProjectState(
    val projects: List<Project> = emptyList()
)
