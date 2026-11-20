package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.KeyDeckViewModel
import com.example.ui.screens.DeckHomeScreen
import com.example.ui.screens.HotkeysScreen
import com.example.ui.screens.LayoutPickerSheet
import com.example.ui.screens.SettingsSheet
import com.example.ui.screens.ThemePickerSheet
import com.example.ui.screens.WebSandboxScreen
import com.example.ui.theme.DarkNeuBg
import com.example.ui.theme.KeyDeckColors
import com.example.ui.theme.KeyDeckTheme
import com.example.ui.theme.NeuBg
import com.example.ui.theme.resolveIsAppDark

enum class KeyDeckTab(val label: String) {
    DECK("Deck"),
    HOTKEYS("Hotkeys"),
    WEB_LAB("Web Lab"),
}

class MainActivity : ComponentActivity() {

    private val viewModel: KeyDeckViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(NeuBg.toArgb(), NeuBg.toArgb()),
            navigationBarStyle = SystemBarStyle.light(NeuBg.toArgb(), NeuBg.toArgb()),
        )
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val isDark = resolveIsAppDark(settings.appThemeMode)
            LaunchedEffect(isDark) {
                val barColor = (if (isDark) DarkNeuBg else NeuBg).toArgb()
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) SystemBarStyle.dark(barColor) else SystemBarStyle.light(barColor, barColor),
                    navigationBarStyle = if (isDark) SystemBarStyle.dark(barColor) else SystemBarStyle.light(barColor, barColor),
                )
            }
            KeyDeckTheme(darkTheme = isDark) {
                KeyDeckApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun KeyDeckApp(viewModel: KeyDeckViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = KeyDeckColors.current

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hotkeys by viewModel.hotkeys.collectAsStateWithLifecycle()
    val readiness by viewModel.readiness.collectAsStateWithLifecycle()
    val isOverlayRunning by viewModel.isOverlayRunning.collectAsStateWithLifecycle()
    val isKeyboardExpanded by viewModel.isKeyboardExpanded.collectAsStateWithLifecycle()
    val lastDispatchedKey by viewModel.lastDispatchedKey.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(KeyDeckTab.DECK) }
    var showThemesSheet by remember { mutableStateOf(false) }
    var showLayoutSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    var askedNotificationOnce by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { _ ->
        viewModel.setFloatingKeyboardEnabled(enabled = true)
    }

    fun startFloatingWithOptionalNotificationPermission(expandImmediately: Boolean = false) {
        if (!android.provider.Settings.canDrawOverlays(context)) {
            viewModel.openOverlayPermissionSettings(context)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !askedNotificationOnce) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted && !isOverlayRunning) {
                askedNotificationOnce = true
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        viewModel.setFloatingKeyboardEnabled(enabled = true, expandImmediately = expandImmediately)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    com.example.data.KeyDeckRuntimeState.setAppInForeground(true)
                    viewModel.refreshSystemReadiness()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    com.example.data.KeyDeckRuntimeState.setAppInForeground(false)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            com.example.data.KeyDeckRuntimeState.setAppInForeground(false)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (currentTab != KeyDeckTab.DECK) {
        BackHandler { currentTab = KeyDeckTab.DECK }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.bg,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.shadowDark.copy(alpha = 0.55f)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.shadowLight.copy(alpha = 0.70f)),
                )
                NavigationBar(
                    containerColor = colors.bg,
                    contentColor = colors.textPrimary,
                    tonalElevation = 0.dp,
                ) {
                    KeyDeckTab.entries.forEach { tab ->
                        val selected = currentTab == tab
                        val icon = when (tab) {
                            KeyDeckTab.DECK -> if (selected) Icons.Filled.Keyboard else Icons.Outlined.Keyboard
                            KeyDeckTab.HOTKEYS -> if (selected) Icons.Filled.Bolt else Icons.Outlined.Bolt
                            KeyDeckTab.WEB_LAB -> if (selected) Icons.Filled.Public else Icons.Outlined.Public
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentTab = tab },
                            icon = { Icon(icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.indigo,
                                selectedTextColor = colors.indigo,
                                indicatorColor = colors.activeFill,
                                unselectedIconColor = colors.textSecondary,
                                unselectedTextColor = colors.textSecondary,
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        when (currentTab) {
            KeyDeckTab.DECK -> DeckHomeScreen(
                settings = settings,
                hotkeys = hotkeys,
                readiness = readiness,
                isOverlayRunning = isOverlayRunning,
                isKeyboardExpanded = isKeyboardExpanded,
                lastDispatchedKey = lastDispatchedKey,
                onToggleOverlay = { enabled ->
                    if (enabled) {
                        startFloatingWithOptionalNotificationPermission(expandImmediately = false)
                    } else {
                        viewModel.setFloatingKeyboardEnabled(false)
                    }
                },
                onOpenFloatingDeck = { viewModel.toggleFloatingExpansion() },
                onRequestOverlayPermission = { viewModel.openOverlayPermissionSettings(context) },
                onOpenAccessibilitySettings = { viewModel.openAccessibilitySettings(context) },
                onOpenImeSettingsOrPicker = {
                    if (readiness.isImeEnabled) {
                        viewModel.showInputMethodPicker(context)
                    } else {
                        viewModel.openImeEnableSettings(context)
                    }
                },
                onOpenHotkeysTab = { currentTab = KeyDeckTab.HOTKEYS },
                onOpenThemesSheet = { showThemesSheet = true },
                onOpenLayoutSheet = { showLayoutSheet = true },
                onOpenSettingsSheet = { showSettingsSheet = true },
                onChangeLayoutMode = { viewModel.setLayoutMode(it) },
                onToggleNumLock = { viewModel.updateSettings { it.copy(numLockOn = !it.numLockOn) } },
                onToggleFunctionRow = { viewModel.updateSettings { it.copy(showFunctionRow = !it.showFunctionRow) } },
                onUpdateSettings = { viewModel.updateSettings(it) },
                modifier = Modifier.padding(innerPadding),
            )

            KeyDeckTab.HOTKEYS -> HotkeysScreen(
                hotkeys = hotkeys,
                hapticEnabled = settings.hapticFeedback,
                onSaveHotkey = { viewModel.saveHotkey(it) },
                onDeleteHotkey = { viewModel.deleteHotkey(it) },
                onMoveHotkeyUp = { viewModel.moveHotkeyUp(it) },
                onAddPresetPack = { viewModel.addPresetPack(it) },
                modifier = Modifier.padding(innerPadding),
            )

            KeyDeckTab.WEB_LAB -> WebSandboxScreen(
                isOverlayRunning = isOverlayRunning,
                suppressSystemKeyboard = isOverlayRunning || settings.suppressAutoImeOnInput,
                onLaunchFloatingOverWeb = {
                    if (isOverlayRunning) {
                        viewModel.toggleFloatingExpansion()
                    } else {
                        startFloatingWithOptionalNotificationPermission(expandImmediately = true)
                    }
                },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    if (showThemesSheet) {
        ThemePickerSheet(
            settings = settings,
            onSelectTheme = { viewModel.setTheme(it) },
            onUpdateSettings = { viewModel.updateSettings(it) },
            onDismiss = { showThemesSheet = false },
        )
    }

    if (showLayoutSheet) {
        LayoutPickerSheet(
            settings = settings,
            onSelectLayout = { viewModel.setLayoutMode(it) },
            onUpdateSettings = { viewModel.updateSettings(it) },
            onDismiss = { showLayoutSheet = false },
        )
    }

    if (showSettingsSheet) {
        SettingsSheet(
            settings = settings,
            onUpdateSettings = { viewModel.updateSettings(it) },
            onResetHotkeys = { viewModel.resetHotkeys() },
            onDismiss = { showSettingsSheet = false },
        )
    }
}
