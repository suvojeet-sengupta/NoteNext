package com.example.notenext.ui.labels

import com.example.notenext.data.Label

data class EditLabelsState(
    val labels: List<Label> = emptyList(),
    val showAddLabelDialog: Boolean = false,
    val showEditLabelDialog: Boolean = false,
    val selectedLabel: Label? = null
)
