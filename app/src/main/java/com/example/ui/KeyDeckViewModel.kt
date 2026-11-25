package com.example.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
import com.example.keyboard.KeyDeckProfileCodec
import com.example.keyboard.KeyDeckSettings
import com.example.keyboard.KeyboardLayoutMode
import com.example.keyboard.KeyboardThemeId
import com.example.keyboard.KeyboardWallpaper
import com.example.keyboard.PcKeyboardLayoutData
import com.example.keyboard.WallpaperSourceType
import com.example.keyboard.deleteWallpaperFileIfUnused
import com.example.keyboard.importWallpaperFromBytes
import com.example.keyboard.importWallpaperFromUri
import com.example.service.FloatingKeyboardService
import com.example.service.KeyDeckAccessibilityService
import com.example.service.KeyDeckImeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class SystemReadinessState(
    val canDrawOverlays: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val isImeEnabled: Boolean = false,
    val isImeSelected: Boolean = false,
) {
    val isFloatingMinReady: Boolean
        get() = canDrawOverlays && isImeEnabled && isImeSelected
}

fun inspectSystemReadiness(context: Context): SystemReadinessState {
    val canOverlay = Settings.canDrawOverlays(context)

    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    val a11yEnabled = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        ?.any {
            it.resolveInfo?.serviceInfo?.packageName == context.packageName &&
                it.resolveInfo?.serviceInfo?.name == KeyDeckAccessibilityService::class.java.name
        } == true || KeyDeckAccessibilityService.activeInstance != null

    val imeComponent = ComponentName(context, KeyDeckImeService::class.java)
    val shortImeId = imeComponent.flattenToShortString()
    val fullImeId = imeComponent.flattenToString()

    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    val imeEnabled = imm?.enabledInputMethodList?.any {
        (it.packageName == context.packageName &&
            (it.serviceName == KeyDeckImeService::class.java.name || it.id.contains("KeyDeckImeService"))) ||
            it.id == shortImeId ||
            it.id == fullImeId
    } == true

    val defaultIme = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
    val imeSelected = defaultIme == shortImeId ||
        defaultIme == fullImeId ||
        (defaultIme.contains(context.packageName) && defaultIme.contains("KeyDeckImeService"))

    return SystemReadinessState(
        canDrawOverlays = canOverlay,
        isAccessibilityEnabled = a11yEnabled,
        isImeEnabled = imeEnabled,
        isImeSelected = imeSelected,
    )
}

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
        _readiness.value = inspectSystemReadiness(getApplication())
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
            val imported = importWallpaperFromUri(ctx, uri)
            if (imported == null) {
                onReady(null)
                return@launch
            }
            val nextNum = savedWallpapers.value.size + 1
            onReady(
                KeyboardWallpaper(
                    id = "draft_${UUID.randomUUID().toString().take(6)}",
                    name = if (imported.isAnimatedGif) "GIF Theme #$nextNum" else "Photo Theme #$nextNum",
                    sourceType = WallpaperSourceType.CUSTOM_IMAGE,
                    imagePath = imported.path,
                    lightness = if (imported.isAnimatedGif) 0.22f else 0.16f,
                    saturation = 0.90f,
                    contrast = 1.08f,
                    brightness = 0.0f,
                    tintHex = 0L,
                    tintIntensity = 0.22f,
                    keyOpacity = 0.62f,
                    darkBackdrop = true,
                    fitCenter = false,
                    isAnimatedGif = imported.isAnimatedGif,
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

    private suspend fun buildSerializedProfile(profileName: String): String = withContext(Dispatchers.IO) {
        val currentSettings = settings.value
        val currentHotkeys = hotkeys.value
        val currentWp = activeWallpaper.value
        val imageBytes = if (
            currentWp != null &&
            currentWp.sourceType == WallpaperSourceType.CUSTOM_IMAGE &&
            currentWp.imagePath.isNotBlank()
        ) {
            runCatching {
                val f = File(currentWp.imagePath)
                if (f.exists() && f.length() <= 12L * 1024L * 1024L) f.readBytes() else null
            }.getOrNull()
        } else {
            null
        }
        KeyDeckProfileCodec.serialize(
            profileName = profileName,
            settings = currentSettings,
            hotkeys = currentHotkeys,
            activeWallpaper = currentWp,
            embeddedImageBytes = imageBytes,
        )
    }

    fun exportProfileToDownloads(
        profileName: String,
        onResult: (success: Boolean, message: String) -> Unit,
    ) {
        viewModelScope.launch {
            val fileName = KeyDeckProfileCodec.buildFileName(profileName)
            val content = buildSerializedProfile(profileName)
            val ctx = getApplication<Application>()
            val savedPathMessage = withContext(Dispatchers.IO) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    runCatching {
                        val resolver = ctx.contentResolver
                        val values = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                            put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                            put(MediaStore.Downloads.IS_PENDING, 1)
                        }
                        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                            ?: return@runCatching null
                        resolver.openOutputStream(uri)?.use { out ->
                            out.write(content.toByteArray(Charsets.UTF_8))
                        } ?: return@runCatching null
                        values.clear()
                        values.put(MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                        "Saved to Download/$fileName"
                    }.getOrNull()
                } else {
                    null
                }
            }
            if (savedPathMessage != null) {
                onResult(true, savedPathMessage)
            } else {
                onResult(false, "Unable to write Download/$fileName directly. Use Save As.")
            }
        }
    }

    fun exportProfileToUri(
        uri: Uri,
        profileName: String,
        onResult: (success: Boolean, message: String) -> Unit,
    ) {
        viewModelScope.launch {
            val fileName = KeyDeckProfileCodec.buildFileName(profileName)
            val content = buildSerializedProfile(profileName)
            val ctx = getApplication<Application>()
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    ctx.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(content.toByteArray(Charsets.UTF_8))
                    } != null
                }.getOrDefault(false)
            }
            if (ok) {
                onResult(true, "Saved profile $fileName")
            } else {
                onResult(false, "Could not save .kdb file to selected location.")
            }
        }
    }

    fun importProfileFromUri(
        uri: Uri,
        onResult: (success: Boolean, message: String) -> Unit,
    ) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val rawText = withContext(Dispatchers.IO) {
                runCatching {
                    ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                        reader.readText()
                    }
                }.getOrNull()
            }
            if (rawText.isNullOrBlank()) {
                onResult(false, "Could not read selected .kdb file.")
                return@launch
            }
            val parsed = KeyDeckProfileCodec.parse(rawText)
            if (parsed == null) {
                onResult(false, "Invalid profile file: missing @keydeck.aemni header.")
                return@launch
            }

            var finalWallpaperId = parsed.activeBuiltInWallpaperId.orEmpty()
            val wpConfig = parsed.wallpaperConfig
            if (wpConfig != null) {
                val persistedWp = if (parsed.embeddedImageBytes != null) {
                    val importedImg = importWallpaperFromBytes(
                        context = ctx,
                        rawBytes = parsed.embeddedImageBytes,
                        preferGif = parsed.isAnimatedGif,
                    )
                    if (importedImg != null) {
                        wpConfig.copy(
                            id = "wp_${UUID.randomUUID().toString().take(8)}",
                            imagePath = importedImg.path,
                            isAnimatedGif = importedImg.isAnimatedGif || parsed.isAnimatedGif,
                            isBuiltIn = false,
                        )
                    } else {
                        null
                    }
                } else {
                    wpConfig.copy(
                        id = "wp_${UUID.randomUUID().toString().take(8)}",
                        isBuiltIn = false,
                    )
                }

                if (persistedWp != null) {
                    val currentWps = savedWallpapers.value.toMutableList()
                    currentWps.add(0, persistedWp)
                    repository.saveWallpapers(currentWps)
                    finalWallpaperId = persistedWp.id
                }
            }

            repository.updateSettings {
                parsed.settings.copy(activeWallpaperId = finalWallpaperId)
            }
            parsed.hotkeys?.let { importedHotkeys ->
                repository.saveHotkeys(importedHotkeys)
            }

            val shortcutInfo = parsed.hotkeys?.let { " • ${it.size} shortcuts" }.orEmpty()
            val gifInfo = if (parsed.isAnimatedGif) " • Animated GIF" else ""
            onResult(true, "Imported \"${parsed.profileName}\" (v${parsed.formatVersion}$shortcutInfo$gifInfo)")
        }
    }
}
