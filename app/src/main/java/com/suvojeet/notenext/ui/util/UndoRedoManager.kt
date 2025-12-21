package com.suvojeet.notenext.ui.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class UndoRedoManager<T>(initialState: T, private val maxHistorySize: Int = 50) {

    private val history = ArrayList<T>()
    private var currentIndex = 0

    private val _canUndo = MutableStateFlow(false)
    val canUndo = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo = _canRedo.asStateFlow()

    init {
        history.add(initialState)
    }

    fun addState(state: T) {
        // If we are not at the end, remove all future states (redo history)
        if (currentIndex < history.lastIndex) {
            history.subList(currentIndex + 1, history.size).clear()
        }

        history.add(state)
        
        // Limit history size
        if (history.size > maxHistorySize) {
            history.removeAt(0)
        } else {
            currentIndex++
        }
        
        // Ensure currentIndex is correct if we removed items
        currentIndex = history.lastIndex

        updateFlags()
    }

    fun undo(): T? {
        if (currentIndex > 0) {
            currentIndex--
            updateFlags()
            return history[currentIndex]
        }
        return null
    }

    fun redo(): T? {
        if (currentIndex < history.lastIndex) {
            currentIndex++
            updateFlags()
            return history[currentIndex]
        }
        return null
    }

    fun getCurrentState(): T {
        return history[currentIndex]
    }
    
    fun reset(state: T) {
        history.clear()
        history.add(state)
        currentIndex = 0
        updateFlags()
    }

    private fun updateFlags() {
        _canUndo.value = currentIndex > 0
        _canRedo.value = currentIndex < history.lastIndex
    }
}
