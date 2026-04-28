package com.suvojeet.notenext.ui

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.ui.theme.ThemeMode
import com.suvojeet.notenext.util.UpdateChecker
import com.suvojeet.notenext.util.ReviewManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MainUiEvent {
    object StartUpdateFlow : MainUiEvent()
    object RequestReviewFlow : MainUiEvent()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    val settingsRepository: SettingsRepository,
    private val updateChecker: UpdateChecker,
    private val reviewManager: ReviewManager
) : ViewModel() {

    val themeMode = settingsRepository.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.SYSTEM
    )

    val isSetupComplete = settingsRepository.isSetupComplete.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val enableAppLock = settingsRepository.enableAppLock.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val disallowScreenshots = settingsRepository.disallowScreenshots.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    val language = settingsRepository.language.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "en"
    )

    val clipboardClearTimeout = settingsRepository.clipboardClearTimeout.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    private val _startNoteId = MutableStateFlow(-1)
    val startNoteId = _startNoteId.asStateFlow()

    private val _startProjectId = MutableStateFlow(-1)
    val startProjectId = _startProjectId.asStateFlow()

    private val _startAddNote = MutableStateFlow(false)
    val startAddNote = _startAddNote.asStateFlow()

    private val _sharedText = MutableStateFlow<String?>(null)
    val sharedText = _sharedText.asStateFlow()

    private val _initialTitle = MutableStateFlow<String?>(null)
    val initialTitle = _initialTitle.asStateFlow()

    private val _searchQuery = MutableStateFlow<String?>(null)
    val searchQuery = _searchQuery.asStateFlow()

    private val _externalUri = MutableStateFlow<Uri?>(null)
    val externalUri = _externalUri.asStateFlow()

    private val _unlockedByAuth = MutableStateFlow(false)
    val unlockedByAuth = _unlockedByAuth.asStateFlow()

    private val _isDecoySession = MutableStateFlow(false)
    val isDecoySession = _isDecoySession.asStateFlow()

    private val _lockTrigger = MutableStateFlow(0L)
    val lockTrigger = _lockTrigger.asStateFlow()

    private var lastPauseTime: Long = 0
    private var clipboardJob: kotlinx.coroutines.Job? = null

    val updateStatus = updateChecker.updateStatus

    private val _uiEvent = MutableSharedFlow<MainUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        checkReview()
    }

    fun handleIntent(intent: Intent) {
        val noteId = intent.getIntExtra("NOTE_ID", -1)
        _startNoteId.value = noteId

        val projectId = intent.getIntExtra("PROJECT_ID", -1)
        _startProjectId.value = projectId

        val isCreateNoteAction = intent.action == "android.intent.action.CREATE_NOTE"
        _startAddNote.value = intent.getBooleanExtra("START_ADD_NOTE", false) || isCreateNoteAction
        
        _initialTitle.value = intent.getStringExtra("TITLE") ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
        _searchQuery.value = intent.getStringExtra("QUERY")
        
        if (intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_EDIT) {
            _externalUri.value = intent.data
        } else {
            _externalUri.value = null
        }

        val sharedText = when {
            intent.action == Intent.ACTION_SEND && "text/plain" == intent.type -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)
            }
            intent.hasExtra(Intent.EXTRA_TEXT) -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)
            }
            else -> null
        }
        _sharedText.value = sharedText
    }

    fun onUnlock(isDecoy: Boolean = false) {
        _isDecoySession.value = isDecoy
        _unlockedByAuth.value = true
    }

    fun onAppStart() {
        clipboardJob?.cancel()
        if (enableAppLock.value == true && lastPauseTime > 0) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastPauseTime > 120_000) { // 2 minutes
                _unlockedByAuth.value = false
                _isDecoySession.value = false
                _lockTrigger.value = currentTime
            }
        }
    }

    fun onAppStop(clearClipboard: () -> Unit) {
        lastPauseTime = System.currentTimeMillis()
        val timeout = clipboardClearTimeout.value
        if (timeout > 0L) {
            clipboardJob?.cancel()
            clipboardJob = viewModelScope.launch {
                if (timeout > 1L) {
                    kotlinx.coroutines.delay(timeout)
                }
                clearClipboard()
            }
        }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000L)
            updateChecker.checkForUpdate()
        }
    }

    fun startUpdate() {
        viewModelScope.launch {
            _uiEvent.emit(MainUiEvent.StartUpdateFlow)
        }
    }

    fun completeUpdate() {
        updateChecker.completeUpdate()
    }

    private fun checkReview() {
        if (reviewManager.shouldRequestReview()) {
            viewModelScope.launch {
                _uiEvent.emit(MainUiEvent.RequestReviewFlow)
            }
        }
    }
}
