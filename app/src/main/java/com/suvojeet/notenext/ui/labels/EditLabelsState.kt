package com.suvojeet.notenext.ui.labels

import com.suvojeet.notenext.data.Label

data class EditLabelsState(
    val labels: List<Label> = emptyList(),
    val showAddLabelDialog: Boolean = false,
    val showEditLabelDialog: Boolean = false,
    val selectedLabel: Label? = null
)
