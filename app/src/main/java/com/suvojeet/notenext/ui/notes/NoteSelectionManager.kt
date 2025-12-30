package com.suvojeet.notenext.ui.notes

import androidx.compose.ui.text.SpanStyle
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object NoteSelectionManager {
    private val _actions = MutableSharedFlow<SpanStyle>()
    val actions = _actions.asSharedFlow()
    
    // We can also support heading styles if needed, but for now just span styles
    private val _headingActions = MutableSharedFlow<Int>()
    val headingActions = _headingActions.asSharedFlow()

    suspend fun onAction(style: SpanStyle) {
        _actions.emit(style)
    }
    
    suspend fun onHeadingAction(level: Int) {
        _headingActions.emit(level)
    }
}
