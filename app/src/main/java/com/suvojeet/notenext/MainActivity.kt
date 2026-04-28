package com.suvojeet.notenext

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.Modifier
import com.suvojeet.notenext.navigation.NavGraph
import com.suvojeet.notenext.ui.theme.NoteNextTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.ui.theme.ThemeMode
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.suvojeet.notenext.ui.lock.LockScreen
import androidx.compose.runtime.LaunchedEffect
import com.suvojeet.notenext.ui.setup.SetupScreen
import java.util.Locale
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import javax.inject.Inject
import com.suvojeet.notenext.util.UpdateChecker
import com.suvojeet.notenext.util.ReviewManager
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.*
import androidx.compose.animation.core.*

import androidx.activity.viewModels
import com.suvojeet.notenext.ui.MainViewModel
import com.suvojeet.notenext.ui.MainUiEvent

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var updateChecker: UpdateChecker
    @Inject
    lateinit var reviewManager: ReviewManager

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        
        splashScreen.setKeepOnScreenCondition {
            viewModel.isSetupComplete.value == null || viewModel.enableAppLock.value == null
        }
        
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val splashView = splashScreenView.view
            splashView.animate()
                .alpha(0f)
                .scaleX(1.3f)
                .scaleY(1.3f)
                .setDuration(400L)
                .setInterpolator(android.view.animation.AnticipateInterpolator())
                .withEndAction {
                    if (!isFinishing && !isDestroyed) {
                        splashScreenView.remove()
                    }
                }
                .start()
        }

        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            )
        )
        super.onCreate(savedInstanceState)

        viewModel.handleIntent(intent)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.disallowScreenshots.collect { disallow ->
                    if (disallow) {
                        window.setFlags(
                            android.view.WindowManager.LayoutParams.FLAG_SECURE,
                            android.view.WindowManager.LayoutParams.FLAG_SECURE
                        )
                    } else {
                        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.language.collect { languageCode ->
                    val appLocales = LocaleListCompat.forLanguageTags(languageCode)
                    if (AppCompatDelegate.getApplicationLocales() != appLocales) {
                        AppCompatDelegate.setApplicationLocales(appLocales)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvent.collect { event ->
                    when (event) {
                        is MainUiEvent.StartUpdateFlow -> updateChecker.startUpdate(this@MainActivity)
                        is MainUiEvent.RequestReviewFlow -> reviewManager.requestReviewFlow(this@MainActivity)
                    }
                }
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.AMOLED -> true
            }

            androidx.compose.runtime.LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (!darkTheme) {
                        androidx.activity.SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    } else {
                        androidx.activity.SystemBarStyle.dark(
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (!darkTheme) {
                        androidx.activity.SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    } else {
                        androidx.activity.SystemBarStyle.dark(
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
            }

            val enableAppLockLoaded by viewModel.enableAppLock.collectAsStateWithLifecycle()
            val isSetupCompleteLoaded by viewModel.isSetupComplete.collectAsStateWithLifecycle()
            val lockTrigger by viewModel.lockTrigger.collectAsStateWithLifecycle()
            val unlockedByAuth by viewModel.unlockedByAuth.collectAsStateWithLifecycle()

            // In-App Update Handling
            val updateStatus by viewModel.updateStatus.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            var showUpdateDialog by remember { mutableStateOf(false) }
            
            val updateAvailableText = stringResource(R.string.update_downloaded_ready)
            val restartText = stringResource(R.string.restart_to_update)

            LaunchedEffect(Unit) {
                viewModel.checkForUpdate()
            }

            LaunchedEffect(updateStatus) {
                when (updateStatus) {
                    is UpdateChecker.UpdateStatus.UpdateAvailable -> {
                        showUpdateDialog = true
                    }
                    is UpdateChecker.UpdateStatus.Downloaded -> {
                        if (!showUpdateDialog) {
                            val result = snackbarHostState.showSnackbar(
                                message = updateAvailableText,
                                actionLabel = restartText,
                                duration = androidx.compose.material3.SnackbarDuration.Indefinite
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.completeUpdate()
                            }
                        }
                    }
                    else -> {}
                }
            }

            if (showUpdateDialog) {
                com.suvojeet.notenext.ui.components.UpdateAvailableDialog(
                    updateStatus = updateStatus,
                    onUpdateClick = {
                        viewModel.startUpdate()
                    },
                    onCompleteUpdate = {
                        viewModel.completeUpdate()
                    },
                    onDismiss = { showUpdateDialog = false }
                )
            }

            NoteNextTheme(themeMode = themeMode) {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { paddingValues ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val unusedPadding = paddingValues
                        
                        androidx.compose.animation.AnimatedContent(
                            targetState = enableAppLockLoaded == null || isSetupCompleteLoaded == null,
                            transitionSpec = {
                                (androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.92f))
                                    .togetherWith(androidx.compose.animation.fadeOut())
                            },
                            label = "AppStartupTransition"
                        ) { isInitialLoading ->
                            if (isInitialLoading) {
                                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                            } else if (isSetupCompleteLoaded == false) {
                                SetupScreen { }
                            } else if (enableAppLockLoaded == true && !unlockedByAuth) {
                                LockScreen(onUnlock = { isDecoy -> viewModel.onUnlock(isDecoy) })
                            } else {
                                val startNoteId by viewModel.startNoteId.collectAsStateWithLifecycle()
                                val startProjectId by viewModel.startProjectId.collectAsStateWithLifecycle()
                                val startAddNote by viewModel.startAddNote.collectAsStateWithLifecycle()
                                val sharedText by viewModel.sharedText.collectAsStateWithLifecycle()
                                val initialTitle by viewModel.initialTitle.collectAsStateWithLifecycle()
                                val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                                val externalUri by viewModel.externalUri.collectAsStateWithLifecycle()

                                NavGraph(
                                    themeMode = themeMode,
                                    windowSizeClass = windowSizeClass,
                                    settingsRepository = viewModel.settingsRepository,
                                    mainViewModel = viewModel,
                                    startNoteId = startNoteId,
                                    startProjectId = startProjectId,
                                    startAddNote = startAddNote,
                                    sharedText = sharedText,
                                    initialTitle = initialTitle,
                                    searchQuery = searchQuery,
                                    externalUri = externalUri
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppStart()
    }

    override fun onStop() {
        super.onStop()
        viewModel.onAppStop {
            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                clipboard.clearPrimaryClip()
            } else {
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("", ""))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateChecker.resumeUpdateCheck(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        updateChecker.unregisterListener()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handleIntent(intent)
    }

    override fun onActionModeStarted(mode: android.view.ActionMode?) {
        val menu = mode?.menu
        if (menu != null) {
            menu.add("Bold").setOnMenuItemClickListener {
                lifecycleScope.launch {
                    com.suvojeet.notenext.ui.notes.NoteSelectionManager.onAction(
                        androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    )
                }
                mode.finish() 
                true
            }
            menu.add("Italic").setOnMenuItemClickListener {
                lifecycleScope.launch {
                    com.suvojeet.notenext.ui.notes.NoteSelectionManager.onAction(
                        androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    )
                }
                mode.finish()
                true
            }
            menu.add("Underline").setOnMenuItemClickListener {
                lifecycleScope.launch {
                    com.suvojeet.notenext.ui.notes.NoteSelectionManager.onAction(
                        androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                    )
                }
                mode.finish()
                true
            }
            menu.add("Strike").setOnMenuItemClickListener {
                lifecycleScope.launch {
                    com.suvojeet.notenext.ui.notes.NoteSelectionManager.onAction(
                        androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                    )
                }
                mode.finish()
                true
            }
        }
        super.onActionModeStarted(mode)
    }
}
