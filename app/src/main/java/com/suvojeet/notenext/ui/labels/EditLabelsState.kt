package com.suvojeet.notenext.ui.labels

import androidx.compose.runtime.Immutable
import com.suvojeet.notenext.data.Label

@Immutable
data class EditLabelsState(
    val labels: List<Label> = emptyList(),
    val showAddLabelDialog: Boolean = false,
    val showEditLabelDialog: Boolean = false,
    val selectedLabel: Label? = null,
    val searchQuery: String = "",
    val isSearchVisible: Boolean = false
)
