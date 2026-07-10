package com.suvojeet.notenext.data

/**
 * Interface for scheduling time-based reminders across the app.
 * Abstracts the underlying implementation (AlarmManager, WorkManager, etc.)
 */
interface ReminderScheduler {
    fun scheduleNoteReminder(note: Note)
    fun cancelNoteReminder(note: Note)
    
    fun scheduleTodoReminder(todo: TodoItem)
    fun cancelTodoReminder(todo: TodoItem)

    /**
     * Exact-alarm self-destruct for notes.
     */
    fun scheduleNoteExpiry(note: Note)
    fun cancelNoteExpiry(note: Note)
}
