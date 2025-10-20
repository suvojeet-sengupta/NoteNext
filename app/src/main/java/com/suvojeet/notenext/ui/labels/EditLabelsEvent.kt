package com.suvojeet.notenext.ui.labels

import com.suvojeet.notenext.data.Label

sealed class EditLabelsEvent {
    data class AddLabel(val name: String) : EditLabelsEvent()
    data class UpdateLabel(val oldLabel: Label, val newName: String) : EditLabelsEvent()
    data class DeleteLabel(val label: Label) : EditLabelsEvent()
    object ShowAddLabelDialog : EditLabelsEvent()
    data class ShowEditLabelDialog(val label: Label) : EditLabelsEvent()
    object HideDialog : EditLabelsEvent()
}
