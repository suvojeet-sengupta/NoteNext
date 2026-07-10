package com.suvojeet.notenext.todo

sealed class TodoFilter {
    object All : TodoFilter()
    object Active : TodoFilter()
    object Completed : TodoFilter()
}
