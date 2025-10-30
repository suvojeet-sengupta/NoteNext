package com.suvojeet.notenext.ui.setup

sealed class SetupEvent {
    object ExactAlarmPermissionResult : SetupEvent()
    object CompleteSetup : SetupEvent()
}
