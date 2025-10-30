package com.suvojeet.notenext.ui.setup

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.notenext.ui.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SetupViewModel(private val application: Application, private val settingsRepository: SettingsRepository) : ViewModel() {

    private val _state = MutableStateFlow(SetupState())
    val state: StateFlow<SetupState> = _state.asStateFlow()

    init {
        checkExactAlarmPermission()
    }

    fun onEvent(event: SetupEvent) {
        when (event) {
            SetupEvent.ExactAlarmPermissionResult -> {
                checkExactAlarmPermission()
            }
            SetupEvent.CompleteSetup -> {
                viewModelScope.launch {
                    settingsRepository.setSetupComplete(true)
                }
            }
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            _state.update { it.copy(exactAlarmGranted = alarmManager.canScheduleExactAlarms()) }
        } else {
            _state.update { it.copy(exactAlarmGranted = true) }
        }
    }
}
