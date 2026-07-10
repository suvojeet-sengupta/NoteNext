package com.suvojeet.notenext.ui.notes

import com.suvojeet.notenext.data.ChecklistItem

object ChecklistManager {

    fun addChecklistItem(currentList: List<ChecklistItem>): Pair<List<ChecklistItem>, String> {
        val newItem = ChecklistItem(text = "", isChecked = false, position = currentList.size)
        return (currentList + newItem) to newItem.id
    }

    fun addChecklistItemAfter(currentList: List<ChecklistItem>, afterId: String): Pair<List<ChecklistItem>, String> {
        val list = currentList.toMutableList()
        val index = list.indexOfFirst { it.id == afterId }
        val newItem = ChecklistItem(
            text = "",
            isChecked = false,
            position = if (index != -1) index + 1 else list.size,
            level = if (index != -1) list[index].level else 0
        )
        
        if (index != -1) {
            list.add(index + 1, newItem)
        } else {
            list.add(newItem)
        }
        
        // Update positions for all items
        val updatedList = list.mapIndexed { i, item -> item.copy(position = i) }
        return updatedList to newItem.id
    }

    fun swapItems(currentList: List<ChecklistItem>, fromId: String, toId: String): List<ChecklistItem> {
        val list = currentList.toMutableList()
        val fromIndex = list.indexOfFirst { it.id == fromId }
        val toIndex = list.indexOfFirst { it.id == toId }

        if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
            java.util.Collections.swap(list, fromIndex, toIndex)
            return list.mapIndexed { index, item -> item.copy(position = index) }
        }
        return currentList
    }

    fun deleteItem(currentList: List<ChecklistItem>, itemId: String): List<ChecklistItem> {
        return currentList.filterNot { it.id == itemId }
    }

    fun indentItem(currentList: List<ChecklistItem>, itemId: String): List<ChecklistItem> {
        return currentList.map {
            if (it.id == itemId) it.copy(level = kotlin.math.min(it.level + 1, 5)) else it
        }
    }

    fun outdentItem(currentList: List<ChecklistItem>, itemId: String): List<ChecklistItem> {
        return currentList.map {
            if (it.id == itemId) it.copy(level = kotlin.math.max(it.level - 1, 0)) else it
        }
    }

    fun changeItemCheckedState(currentList: List<ChecklistItem>, itemId: String, isChecked: Boolean): List<ChecklistItem> {
        val updatedChecklist = currentList.toMutableList()
        val index = updatedChecklist.indexOfFirst { it.id == itemId }
        if (index != -1) {
            val item = updatedChecklist.removeAt(index).copy(isChecked = isChecked)
            if (isChecked) {
                updatedChecklist.add(item) // Add to the end if checked
            } else {
                // Find the first checked item and insert before it, or at the end if no checked items
                val firstCheckedIndex = updatedChecklist.indexOfFirst { it.isChecked }
                if (firstCheckedIndex != -1) {
                    updatedChecklist.add(firstCheckedIndex, item)
                } else {
                    updatedChecklist.add(0, item) // Add to the beginning if no checked items
                }
            }
        }
        return updatedChecklist
    }

    fun changeItemText(currentList: List<ChecklistItem>, itemId: String, text: String): List<ChecklistItem> {
        return currentList.map {
            if (it.id == itemId) it.copy(text = text) else it
        }
    }

    fun deleteAllCheckedItems(currentList: List<ChecklistItem>): List<ChecklistItem> {
        return currentList.filterNot { it.isChecked }
    }
}
