package com.suvojeet.notenext.domain.use_case.todo

import javax.inject.Inject

data class TodoUseCases @Inject constructor(
    val completeTodo: CompleteTodoUseCase,
    val saveTodo: SaveTodoUseCase
)
