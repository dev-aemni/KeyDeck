package com.example.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.KeyDeckRepository
import com.example.data.KeyDeckRuntimeState
import com.example.keyboard.BuiltInWallpapers
import com.example.keyboard.CustomHotkey
import com.example.keyboard.HotkeyCategory
import com.example.keyboard.KeyDeckSettings
import com.example.keyboard.KeyboardLayoutMode
import com.example.keyboard.KeyboardThemeId
import com.example.keyboard.KeyboardWallpaper
import com.example.keyboard.PcKeyboardLayoutData
import com.example.keyboard.WallpaperSourceType
import com.example.keyboard.deleteWallpaperFileIfUnused
import com.example.keyboard.importWallpaperFromUri
import com.example.service.FloatingKeyboardService
import com.example.service.KeyDeckAccessibilityService
import com.example.service.KeyDeckImeService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class SystemReadinessState(
    val canDrawOverlays: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val isImeEnabled: Boolean = false,
    val isImeSelected: Boolean = false,
)

class KeyDeckViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = KeyDeckRepository(application.applicationContext)

    val settings: StateFlow<KeyDeckSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = KeyDeckSettings(),
    )

    val hotkeys: StateFlow<List<CustomHotkey>> = repository.hotkeysFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PcKeyboardLayoutData.defaultHotkeys,
    )

    val savedWallpapers: StateFlow<List<KeyboardWallpaper>> = repository.wallpapersFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    val activeWallpaper: StateFlow<KeyboardWallpaper?> = combine(
        settings,
        savedWallpapers,
    ) { currentSettings, saved ->
        BuiltInWallpapers.resolve(currentSettings.activeWallpaperId, saved)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null,
    )

    val isOverlayRunning: StateFlow<Boolean> = KeyDeckRuntimeState.isOverlayServiceRunning
    val isKeyboardExpanded: StateFlow<Boolean> = KeyDeckRuntimeState.isKeyboardOverlayExpanded
    val lastDispatchedKey = KeyDeckRuntimeState.lastDispatchedKey
    val recentKeyLog = KeyDeckRuntimeState.recentKeyLog

    private val _readiness = MutableStateFlow(SystemReadinessState())
    val readiness: StateFlow<SystemReadinessState> = _readiness.asStateFlow()

    init {
        viewModelScope.launch {
            repository.migrateSchemaIfNeeded()
        }
        refreshSystemReadiness()
    }

    fun refreshSystemReadiness() {
        val ctx = getApplication<Application>()
        val canOverlay = Settings.canDrawOverlays(ctx)

        val am = ctx.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val a11yEnabled = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            ?.any {
                it.resolveInfo?.serviceInfo?.packageName == ctx.packageName &&
                    it.resolveInfo?.serviceInfo?.name == KeyDeckAccessibilityService::class.java.name
            } == true || KeyDeckAccessibilityService.activeInstance != null

        val imeComponent = ComponentName(ctx, KeyDeckImeService::class.java)
        val shortImeId = imeComponent.flattenToShortString()
        val fullImeId = imeComponent.flattenToString()

        val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val imeEnabled = imm?.enabledInputMethodList?.any {
            (it.packageName == ctx.packageName &&
                (it.serviceName == KeyDeckImeService::class.java.name || it.id.contains("KeyDeckImeService"))) ||
                it.id == shortImeId ||
                it.id == fullImeId
        } == true

        val defaultIme = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
        val imeSelected = defaultIme == shortImeId ||
            defaultIme == fullImeId ||
            (defaultIme.contains(ctx.packageName) && defaultIme.contains("KeyDeckImeService"))

        _readiness.value = SystemReadinessState(
            canDrawOverlays = canOverlay,
            isAccessibilityEnabled = a11yEnabled,
            isImeEnabled = imeEnabled,
            isImeSelected = imeSelected,
        )
    }

    fun setFloatingKeyboardEnabled(enabled: Boolean, expandImmediately: Boolean = false) {
        val ctx = getApplication<Application>()
        if (enabled) {
            if (!Settings.canDrawOverlays(ctx)) {
                openOverlayPermissionSettings(ctx)
                return
            }
            val intent = Intent(ctx, FloatingKeyboardService::class.java).apply {
                action = if (expandImmediately) {
                    FloatingKeyboardService.ACTION_START_AND_EXPAND
                } else {
                    FloatingKeyboardService.ACTION_START
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        } else {
            val stopIntent = Intent(ctx, FloatingKeyboardService::class.java).apply {
                action = FloatingKeyboardService.ACTION_STOP
            }
            ctx.startService(stopIntent)
        }
    }

    fun toggleFloatingExpansion() {
        val ctx = getApplication<Application>()
        if (!isOverlayRunning.value) {
            setFloatingKeyboardEnabled(true, expandImmediately = true)
        } else {
            val intent = Intent(ctx, FloatingKeyboardService::class.java).apply {
                action = FloatingKeyboardService.ACTION_TOGGLE_KEYBOARD
            }
            ctx.startService(intent)
        }
    }

    fun openOverlayPermissionSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openImeEnableSettings(context: Context) {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun showInputMethodPicker(context: Context) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showInputMethodPicker()
    }

    fun setLayoutMode(mode: KeyboardLayoutMode) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(layoutMode = mode) }
        }
    }

    fun setTheme(themeId: KeyboardThemeId) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(themeId = themeId) }
        }
    }

    fun updateSettings(transform: (KeyDeckSettings) -> KeyDeckSettings) {
        viewModelScope.launch {
            repository.updateSettings(transform)
        }
    }

    fun selectWallpaper(wallpaperId: String) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(activeWallpaperId = wallpaperId) }
        }
    }

    fun saveCustomWallpaper(wallpaper: KeyboardWallpaper, activateImmediately: Boolean = true) {
        viewModelScope.launch {
            val current = savedWallpapers.value.toMutableList()
            val cleanName = wallpaper.name.trim().ifBlank { "Custom Wallpaper" }
            val existingIdx = current.indexOfFirst { it.id == wallpaper.id && !wallpaper.isBuiltIn }
            val persisted = if (existingIdx >= 0) {
                val updated = wallpaper.copy(name = cleanName, isBuiltIn = false)
                current[existingIdx] = updated
                updated
            } else {
                val created = wallpaper.copy(
                    id = "wp_${UUID.randomUUID().toString().take(8)}",
                    name = cleanName,
                    isBuiltIn = false,
                    createdAtMs = System.currentTimeMillis(),
                )
                current.add(0, created)
                created
            }
            repository.saveWallpapers(current)
            if (activateImmediately) {
                repository.updateSettings { it.copy(activeWallpaperId = persisted.id) }
            }
        }
    }

    fun deleteCustomWallpaper(wallpaperId: String) {
        viewModelScope.launch {
            val current = savedWallpapers.value
            val removed = current.firstOrNull { it.id == wallpaperId } ?: return@launch
            val remaining = current.filterNot { it.id == wallpaperId }
            repository.saveWallpapers(remaining)
            if (settings.value.activeWallpaperId == wallpaperId) {
                repository.updateSettings { it.copy(activeWallpaperId = "") }
            }
            deleteWallpaperFileIfUnused(removed, remaining)
        }
    }

    fun importWallpaperFromGallery(uri: Uri, onReady: (KeyboardWallpaper?) -> Unit) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val path = importWallpaperFromUri(ctx, uri)
            if (path == null) {
                onReady(null)
                return@launch
            }
            val nextNum = savedWallpapers.value.size + 1
            onReady(
                KeyboardWallpaper(
                    id = "draft_${UUID.randomUUID().toString().take(6)}",
                    name = "Photo Theme #$nextNum",
                    sourceType = WallpaperSourceType.CUSTOM_IMAGE,
                    imagePath = path,
                    lightness = 0.16f,
                    saturation = 0.90f,
                    contrast = 1.08f,
                    brightness = 0.0f,
                    tintHex = 0L,
                    tintIntensity = 0.22f,
                    keyOpacity = 0.62f,
                    darkBackdrop = true,
                    fitCenter = false,
                    isBuiltIn = false,
                    createdAtMs = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun saveHotkey(hotkey: CustomHotkey) {
        viewModelScope.launch {
            val current = hotkeys.value.toMutableList()
            val idx = current.indexOfFirst { it.id == hotkey.id }
            if (idx >= 0) {
                current[idx] = hotkey
            } else {
                current.add(0, hotkey.copy(id = "hk_${UUID.randomUUID().toString().take(8)}"))
            }
            repository.saveHotkeys(current)
        }
    }

    fun deleteHotkey(id: String) {
        viewModelScope.launch {
            repository.saveHotkeys(hotkeys.value.filterNot { it.id == id })
        }
    }

    fun moveHotkeyUp(id: String) {
        viewModelScope.launch {
            val list = hotkeys.value.toMutableList()
            val idx = list.indexOfFirst { it.id == id }
            if (idx > 0) {
                val item = list.removeAt(idx)
                list.add(idx - 1, item)
                repository.saveHotkeys(list)
            }
        }
    }

    fun resetHotkeys() {
        viewModelScope.launch {
            repository.resetHotkeysToDefault()
        }
    }

    fun addPresetPack(category: HotkeyCategory) {
        viewModelScope.launch {
            val presets = when (category) {
                HotkeyCategory.BROWSER -> listOf(
                    CustomHotkey("hk_dev_${UUID.randomUUID().toString().take(4)}", "DevTools", android.view.KeyEvent.KEYCODE_I, "I", ctrl = true, shift = true, category = HotkeyCategory.BROWSER, accentHex = 0xFF38BDF8),
                    CustomHotkey("hk_close_${UUID.randomUUID().toString().take(4)}", "Close Tab", android.view.KeyEvent.KEYCODE_W, "W", ctrl = true, category = HotkeyCategory.BROWSER, accentHex = 0xFFE95663),
                    CustomHotkey("hk_reload_${UUID.randomUUID().toString().take(4)}", "Hard Reload", android.view.KeyEvent.KEYCODE_R, "R", ctrl = true, shift = true, category = HotkeyCategory.BROWSER, accentHex = 0xFF34D399),
                )
                HotkeyCategory.EDITING -> listOf(
                    CustomHotkey("hk_save_${UUID.randomUUID().toString().take(4)}", "Save", android.view.KeyEvent.KEYCODE_S, "S", ctrl = true, category = HotkeyCategory.EDITING, accentHex = 0xFF34D399),
                    CustomHotkey("hk_bold_${UUID.randomUUID().toString().take(4)}", "Bold", android.view.KeyEvent.KEYCODE_B, "B", ctrl = true, category = HotkeyCategory.EDITING, accentHex = 0xFFFBBF24),
                )
                HotkeyCategory.NAVIGATION -> listOf(
                    CustomHotkey("hk_home_${UUID.randomUUID().toString().take(4)}", "Top", android.view.KeyEvent.KEYCODE_MOVE_HOME, "Home", ctrl = true, category = HotkeyCategory.NAVIGATION, accentHex = 0xFFA78BFA),
                    CustomHotkey("hk_end_${UUID.randomUUID().toString().take(4)}", "Bottom", android.view.KeyEvent.KEYCODE_MOVE_END, "End", ctrl = true, category = HotkeyCategory.NAVIGATION, accentHex = 0xFF56CCF2),
                )
                else -> emptyList()
            }
            if (presets.isNotEmpty()) {
                val existingCombos = hotkeys.value.map { it.comboText() }.toSet()
                val newItems = presets.filterNot { it.comboText() in existingCombos }
                repository.saveHotkeys(newItems + hotkeys.value)
            }
        }
    }
}
