package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.keyboard.AppThemeMode
import com.example.keyboard.CustomHotkey
import com.example.keyboard.DispatchedKeyRecord
import com.example.keyboard.HotkeyActionType
import com.example.keyboard.HotkeyCategory
import com.example.keyboard.KeyDeckSettings
import com.example.keyboard.KeyboardDockAlignment
import com.example.keyboard.KeyboardLayoutMode
import com.example.keyboard.KeyboardThemeId
import com.example.keyboard.KeyboardWallpaper
import com.example.keyboard.PcKeyboardLayoutData
import com.example.keyboard.RgbBorderMode
import com.example.keyboard.WallpaperScaleMode
import com.example.keyboard.WallpaperSourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.keyDeckDataStore: DataStore<Preferences> by preferencesDataStore(name = "keydeck_prefs")

object KeyDeckRuntimeState {
    private val _isAppInForeground = MutableStateFlow(false)
    val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()

    private val _isOverlayServiceRunning = MutableStateFlow(false)
    val isOverlayServiceRunning: StateFlow<Boolean> = _isOverlayServiceRunning.asStateFlow()

    private val _isKeyboardOverlayExpanded = MutableStateFlow(false)
    val isKeyboardOverlayExpanded: StateFlow<Boolean> = _isKeyboardOverlayExpanded.asStateFlow()

    private val _isAccessibilityRunning = MutableStateFlow(false)
    val isAccessibilityRunning: StateFlow<Boolean> = _isAccessibilityRunning.asStateFlow()

    private val _isImeSessionActive = MutableStateFlow(false)
    val isImeSessionActive: StateFlow<Boolean> = _isImeSessionActive.asStateFlow()

    private val _lastDispatchedKey = MutableStateFlow<DispatchedKeyRecord?>(null)
    val lastDispatchedKey: StateFlow<DispatchedKeyRecord?> = _lastDispatchedKey.asStateFlow()

    private val _recentKeyLog = MutableStateFlow<List<DispatchedKeyRecord>>(emptyList())
    val recentKeyLog: StateFlow<List<DispatchedKeyRecord>> = _recentKeyLog.asStateFlow()

    private val _inputSessionGeneration = MutableStateFlow(0L)
    val inputSessionGeneration: StateFlow<Long> = _inputSessionGeneration.asStateFlow()

    private val _mouseCursorState = MutableStateFlow(com.example.keyboard.MouseCursorState())
    val mouseCursorState: StateFlow<com.example.keyboard.MouseCursorState> = _mouseCursorState.asStateFlow()

    fun updateMouseCursor(xPx: Float, yPx: Float, visible: Boolean) {
        _mouseCursorState.value = com.example.keyboard.MouseCursorState(
            xPx = xPx,
            yPx = yPx,
            visible = visible,
        )
    }

    fun moveMouseCursorBy(
        dx: Float,
        dy: Float,
        screenWidthPx: Float,
        screenHeightPx: Float,
        visible: Boolean = true,
    ): Pair<Float, Float> {
        val cur = _mouseCursorState.value
        val (nx, ny) = com.example.keyboard.MouseJoystickMath.clampCursorPosition(
            currentX = cur.xPx,
            currentY = cur.yPx,
            dx = dx,
            dy = dy,
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx,
        )
        _mouseCursorState.value = com.example.keyboard.MouseCursorState(
            xPx = nx,
            yPx = ny,
            visible = visible,
        )
        return nx to ny
    }

    fun parkMouseCursorAtCenter(
        screenWidthPx: Float,
        screenHeightPx: Float,
        visible: Boolean,
    ) {
        val w = screenWidthPx.coerceAtLeast(320f)
        val h = screenHeightPx.coerceAtLeast(480f)
        _mouseCursorState.value = com.example.keyboard.MouseCursorState(
            xPx = w * 0.5f,
            yPx = h * 0.42f,
            visible = visible,
        )
    }

    fun setAppInForeground(foreground: Boolean) {
        _isAppInForeground.value = foreground
    }

    fun setOverlayServiceRunning(running: Boolean) {
        _isOverlayServiceRunning.value = running
        if (!running) {
            _isKeyboardOverlayExpanded.value = false
        }
    }

    fun setKeyboardOverlayExpanded(expanded: Boolean) {
        _isKeyboardOverlayExpanded.value = expanded
    }

    fun toggleKeyboardOverlayExpanded() {
        _isKeyboardOverlayExpanded.value = !_isKeyboardOverlayExpanded.value
    }

    fun setAccessibilityRunning(running: Boolean) {
        _isAccessibilityRunning.value = running
    }

    fun setImeSessionActive(active: Boolean) {
        _isImeSessionActive.value = active
    }

    fun notifyInputSessionReset() {
        _inputSessionGeneration.value = _inputSessionGeneration.value + 1L
    }

    fun recordDispatchedKey(record: DispatchedKeyRecord) {
        _lastDispatchedKey.value = record
        _recentKeyLog.value = (listOf(record) + _recentKeyLog.value).take(25)
    }
}

class KeyDeckRepository(private val context: Context) {

    private object Keys {
        val SETTINGS_SCHEMA_VERSION = intPreferencesKey("settings_schema_version")
        val LAYOUT_MODE = stringPreferencesKey("layout_mode")
        val THEME_ID = stringPreferencesKey("theme_id")
        val APP_THEME_MODE = stringPreferencesKey("app_theme_mode")
        val SYNC_KEYBOARD_DARK_MODE = booleanPreferencesKey("sync_keyboard_dark_mode")
        val SHOW_MODE_STRIP = booleanPreferencesKey("show_mode_strip")
        val SHOW_FUNCTION_ROW = booleanPreferencesKey("show_function_row")
        val NUM_LOCK_ON = booleanPreferencesKey("num_lock_on")
        val STICKY_MODIFIERS = booleanPreferencesKey("sticky_modifiers")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val SUPPRESS_AUTO_IME_ON_INPUT = booleanPreferencesKey("suppress_auto_ime_on_input")
        val SIMPLE_SUGGESTIONS_ENABLED = booleanPreferencesKey("simple_suggestions_enabled")
        val SIMPLE_AUTO_CORRECT_ENABLED = booleanPreferencesKey("simple_auto_correct_enabled")
        val SIMPLE_AUTO_CAPITALIZE = booleanPreferencesKey("simple_auto_capitalize")
        val SIMPLE_SMART_PUNCTUATION = booleanPreferencesKey("simple_smart_punctuation")
        val SIMPLE_DOUBLE_SPACE_PERIOD = booleanPreferencesKey("simple_double_space_period")
        val SIMPLE_CLIPBOARD_STRIP = booleanPreferencesKey("simple_clipboard_strip")
        val HOLD_TO_REPEAT = booleanPreferencesKey("hold_to_repeat")
        val KEY_HEIGHT_DP = intPreferencesKey("key_height_dp")
        val WIDTH_FRACTION = floatPreferencesKey("width_fraction")
        val DOCK_ALIGNMENT = stringPreferencesKey("dock_alignment")
        val KEY_H_GAP_DP = intPreferencesKey("key_h_gap_dp")
        val KEY_V_GAP_DP = intPreferencesKey("key_v_gap_dp")
        val DECK_PADDING_DP = intPreferencesKey("deck_padding_dp")
        val KEY_CORNER_DP = intPreferencesKey("key_corner_dp")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val OVERLAY_OPACITY = floatPreferencesKey("overlay_opacity")
        val BUBBLE_SIZE_DP = intPreferencesKey("bubble_size_dp")
        val RGB_BORDER_MODE = stringPreferencesKey("rgb_border_mode")
        val SHOW_KEY_BORDER = booleanPreferencesKey("show_key_border")
        val THEME_ADAPTIVE_BORDER = booleanPreferencesKey("theme_adaptive_border")
        val ACTIVE_WALLPAPER_ID = stringPreferencesKey("active_wallpaper_id")
        val SAVED_WALLPAPERS_JSON = stringPreferencesKey("saved_wallpapers_json")
        val TOUCHPAD_SENSITIVITY = floatPreferencesKey("touchpad_sensitivity")
        val TOUCHPAD_WIDTH_FRACTION = floatPreferencesKey("touchpad_width_fraction")
        val GAMING_MCPE_LOOK_MODE = booleanPreferencesKey("gaming_mcpe_look_mode")
        val SHOW_GAME_MOUSE_POINTER = booleanPreferencesKey("show_game_mouse_pointer")
        val GAME_MOUSE_POINTER_SCALE = floatPreferencesKey("game_mouse_pointer_scale")
        val OVERRIDE_ACCENT_HEX = longPreferencesKey("override_accent_hex")
        val CUSTOM_DECK_HEX = longPreferencesKey("custom_deck_hex")
        val CUSTOM_KEY_HEX = longPreferencesKey("custom_key_hex")
        val CUSTOM_MOD_HEX = longPreferencesKey("custom_mod_hex")
        val CUSTOM_TEXT_HEX = longPreferencesKey("custom_text_hex")
        val CUSTOM_ACCENT_HEX = longPreferencesKey("custom_accent_hex")
        val CUSTOM_HOTKEYS_JSON = stringPreferencesKey("custom_hotkeys_json")
    }

    val settingsFlow: Flow<KeyDeckSettings> = context.keyDeckDataStore.data.map { prefs ->
        decodeSettings(prefs)
    }

    val hotkeysFlow: Flow<List<CustomHotkey>> = context.keyDeckDataStore.data.map { prefs ->
        val raw = prefs[Keys.CUSTOM_HOTKEYS_JSON]
        if (raw.isNullOrBlank()) {
            PcKeyboardLayoutData.defaultHotkeys
        } else {
            decodeHotkeys(raw)
        }
    }

    val wallpapersFlow: Flow<List<KeyboardWallpaper>> = context.keyDeckDataStore.data.map { prefs ->
        val raw = prefs[Keys.SAVED_WALLPAPERS_JSON]
        if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            decodeWallpapers(raw)
        }
    }

    suspend fun migrateSchemaIfNeeded() {
        context.keyDeckDataStore.edit { prefs ->
            val currentVersion = prefs[Keys.SETTINGS_SCHEMA_VERSION] ?: 0
            if (currentVersion < 2) {
                prefs[Keys.LAYOUT_MODE] = KeyboardLayoutMode.PC_STANDARD.name
                prefs[Keys.RGB_BORDER_MODE] = RgbBorderMode.GAMING_ONLY.name
                prefs[Keys.SUPPRESS_AUTO_IME_ON_INPUT] = true
            }
            if (currentVersion < 3) {
                val rawHotkeys = prefs[Keys.CUSTOM_HOTKEYS_JSON]
                if (!rawHotkeys.isNullOrBlank()) {
                    val decoded = decodeHotkeys(rawHotkeys)
                    if (decoded.none { it.actionType == HotkeyActionType.TOGGLE_FLOATING }) {
                        val defaultFloat = PcKeyboardLayoutData.defaultHotkeys
                            .firstOrNull { it.actionType == HotkeyActionType.TOGGLE_FLOATING }
                        if (defaultFloat != null) {
                            prefs[Keys.CUSTOM_HOTKEYS_JSON] = encodeHotkeys(listOf(defaultFloat) + decoded)
                        }
                    }
                }
            }
            if (currentVersion < 4) {
                prefs[Keys.SHOW_KEY_BORDER] = false
                prefs[Keys.THEME_ADAPTIVE_BORDER] = false
            }
            if (currentVersion < CURRENT_SCHEMA_VERSION) {
                prefs[Keys.SETTINGS_SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION
            }
        }
    }

    suspend fun updateSettings(transform: (KeyDeckSettings) -> KeyDeckSettings) {
        context.keyDeckDataStore.edit { prefs ->
            val current = decodeSettings(prefs)
            val updated = transform(current)
            prefs[Keys.SETTINGS_SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION
            prefs[Keys.LAYOUT_MODE] = updated.layoutMode.name
            prefs[Keys.THEME_ID] = updated.themeId.name
            prefs[Keys.APP_THEME_MODE] = updated.appThemeMode.name
            prefs[Keys.SYNC_KEYBOARD_DARK_MODE] = updated.syncKeyboardWithDarkMode
            prefs[Keys.SHOW_MODE_STRIP] = updated.showModeStrip
            prefs[Keys.SHOW_FUNCTION_ROW] = updated.showFunctionRow
            prefs[Keys.NUM_LOCK_ON] = updated.numLockOn
            prefs[Keys.STICKY_MODIFIERS] = updated.stickyModifiers
            prefs[Keys.HAPTIC_FEEDBACK] = updated.hapticFeedback
            prefs[Keys.SUPPRESS_AUTO_IME_ON_INPUT] = updated.suppressAutoImeOnInput
            prefs[Keys.SIMPLE_SUGGESTIONS_ENABLED] = updated.simpleSuggestionsEnabled
            prefs[Keys.SIMPLE_AUTO_CORRECT_ENABLED] = updated.simpleAutoCorrectEnabled
            prefs[Keys.SIMPLE_AUTO_CAPITALIZE] = updated.simpleAutoCapitalize
            prefs[Keys.SIMPLE_SMART_PUNCTUATION] = updated.simpleSmartPunctuation
            prefs[Keys.SIMPLE_DOUBLE_SPACE_PERIOD] = updated.simpleDoubleSpacePeriod
            prefs[Keys.SIMPLE_CLIPBOARD_STRIP] = updated.simpleClipboardStripEnabled
            prefs[Keys.HOLD_TO_REPEAT] = updated.holdToRepeat
            prefs[Keys.KEY_HEIGHT_DP] = updated.keyHeightDp
            prefs[Keys.WIDTH_FRACTION] = updated.keyboardWidthFraction
            prefs[Keys.DOCK_ALIGNMENT] = updated.dockAlignment.name
            prefs[Keys.KEY_H_GAP_DP] = updated.keyHorizontalGapDp
            prefs[Keys.KEY_V_GAP_DP] = updated.keyVerticalGapDp
            prefs[Keys.DECK_PADDING_DP] = updated.deckPaddingDp
            prefs[Keys.KEY_CORNER_DP] = updated.keyCornerRadiusDp
            prefs[Keys.FONT_SCALE] = updated.fontScale
            prefs[Keys.OVERLAY_OPACITY] = updated.overlayOpacity
            prefs[Keys.BUBBLE_SIZE_DP] = updated.bubbleSizeDp
            prefs[Keys.RGB_BORDER_MODE] = updated.rgbBorderMode.name
            prefs[Keys.SHOW_KEY_BORDER] = updated.showKeyBorder
            prefs[Keys.THEME_ADAPTIVE_BORDER] = updated.themeAdaptiveBorder
            prefs[Keys.ACTIVE_WALLPAPER_ID] = updated.activeWallpaperId
            prefs[Keys.TOUCHPAD_SENSITIVITY] = updated.touchpadSensitivity
            prefs[Keys.TOUCHPAD_WIDTH_FRACTION] = updated.touchpadWidthFraction
            prefs[Keys.GAMING_MCPE_LOOK_MODE] = updated.gamingMcpeLookMode
            prefs[Keys.SHOW_GAME_MOUSE_POINTER] = updated.showGameMousePointer
            prefs[Keys.GAME_MOUSE_POINTER_SCALE] = updated.gameMousePointerScale.coerceIn(0.55f, 1.60f)
            prefs[Keys.OVERRIDE_ACCENT_HEX] = updated.overrideAccentHex
            prefs[Keys.CUSTOM_DECK_HEX] = updated.customDeckHex
            prefs[Keys.CUSTOM_KEY_HEX] = updated.customKeyHex
            prefs[Keys.CUSTOM_MOD_HEX] = updated.customModifierHex
            prefs[Keys.CUSTOM_TEXT_HEX] = updated.customTextHex
            prefs[Keys.CUSTOM_ACCENT_HEX] = updated.customAccentHex
        }
    }

    private fun decodeSettings(prefs: Preferences): KeyDeckSettings {
        val schemaVersion = prefs[Keys.SETTINGS_SCHEMA_VERSION] ?: 0
        val isLegacyV1 = schemaVersion < 2
        val isLegacyBeforeV4 = schemaVersion < 4
        val layoutName = if (isLegacyV1) {
            KeyboardLayoutMode.PC_STANDARD.name
        } else {
            prefs[Keys.LAYOUT_MODE] ?: KeyboardLayoutMode.PC_STANDARD.name
        }
        val themeName = prefs[Keys.THEME_ID] ?: KeyboardThemeId.SOFT_NEUMORPH.name
        val appThemeName = prefs[Keys.APP_THEME_MODE] ?: AppThemeMode.SYSTEM.name
        val dockName = prefs[Keys.DOCK_ALIGNMENT] ?: KeyboardDockAlignment.CENTER.name
        val rgbName = if (isLegacyV1) {
            RgbBorderMode.GAMING_ONLY.name
        } else {
            prefs[Keys.RGB_BORDER_MODE] ?: RgbBorderMode.GAMING_ONLY.name
        }
        return KeyDeckSettings(
            layoutMode = runCatching { KeyboardLayoutMode.valueOf(layoutName) }
                .getOrDefault(KeyboardLayoutMode.PC_STANDARD),
            themeId = runCatching { KeyboardThemeId.valueOf(themeName) }
                .getOrDefault(KeyboardThemeId.SOFT_NEUMORPH),
            appThemeMode = runCatching { AppThemeMode.valueOf(appThemeName) }
                .getOrDefault(AppThemeMode.SYSTEM),
            syncKeyboardWithDarkMode = prefs[Keys.SYNC_KEYBOARD_DARK_MODE] ?: true,
            showModeStrip = prefs[Keys.SHOW_MODE_STRIP] ?: true,
            showFunctionRow = prefs[Keys.SHOW_FUNCTION_ROW] ?: false,
            numLockOn = prefs[Keys.NUM_LOCK_ON] ?: true,
            stickyModifiers = prefs[Keys.STICKY_MODIFIERS] ?: true,
            hapticFeedback = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
            suppressAutoImeOnInput = prefs[Keys.SUPPRESS_AUTO_IME_ON_INPUT] ?: true,
            simpleSuggestionsEnabled = prefs[Keys.SIMPLE_SUGGESTIONS_ENABLED] ?: true,
            simpleAutoCorrectEnabled = prefs[Keys.SIMPLE_AUTO_CORRECT_ENABLED] ?: true,
            simpleAutoCapitalize = prefs[Keys.SIMPLE_AUTO_CAPITALIZE] ?: true,
            simpleSmartPunctuation = prefs[Keys.SIMPLE_SMART_PUNCTUATION] ?: true,
            simpleDoubleSpacePeriod = prefs[Keys.SIMPLE_DOUBLE_SPACE_PERIOD] ?: true,
            simpleClipboardStripEnabled = prefs[Keys.SIMPLE_CLIPBOARD_STRIP] ?: true,
            holdToRepeat = prefs[Keys.HOLD_TO_REPEAT] ?: true,
            keyHeightDp = (prefs[Keys.KEY_HEIGHT_DP] ?: 38).coerceIn(26, 54),
            keyboardWidthFraction = (prefs[Keys.WIDTH_FRACTION] ?: 1.0f).coerceIn(0.60f, 1.0f),
            dockAlignment = runCatching { KeyboardDockAlignment.valueOf(dockName) }
                .getOrDefault(KeyboardDockAlignment.CENTER),
            keyHorizontalGapDp = (prefs[Keys.KEY_H_GAP_DP] ?: 3).coerceIn(1, 6),
            keyVerticalGapDp = (prefs[Keys.KEY_V_GAP_DP] ?: 4).coerceIn(1, 7),
            deckPaddingDp = (prefs[Keys.DECK_PADDING_DP] ?: 6).coerceIn(1, 10),
            keyCornerRadiusDp = (prefs[Keys.KEY_CORNER_DP] ?: 10).coerceIn(4, 16),
            fontScale = (prefs[Keys.FONT_SCALE] ?: 0.96f).coerceIn(0.75f, 1.25f),
            overlayOpacity = (prefs[Keys.OVERLAY_OPACITY] ?: 0.96f).coerceIn(0.15f, 1.0f),
            bubbleSizeDp = (prefs[Keys.BUBBLE_SIZE_DP] ?: 48).coerceIn(36, 68),
            rgbBorderMode = runCatching { RgbBorderMode.valueOf(rgbName) }
                .getOrDefault(RgbBorderMode.GAMING_ONLY),
            showKeyBorder = if (isLegacyBeforeV4) false else (prefs[Keys.SHOW_KEY_BORDER] ?: false),
            themeAdaptiveBorder = if (isLegacyBeforeV4) false else (prefs[Keys.THEME_ADAPTIVE_BORDER] ?: false),
            activeWallpaperId = prefs[Keys.ACTIVE_WALLPAPER_ID] ?: "",
            touchpadSensitivity = (prefs[Keys.TOUCHPAD_SENSITIVITY] ?: 1.6f).coerceIn(0.6f, 3.5f),
            touchpadWidthFraction = (prefs[Keys.TOUCHPAD_WIDTH_FRACTION] ?: 0.42f).coerceIn(0.30f, 0.55f),
            gamingMcpeLookMode = prefs[Keys.GAMING_MCPE_LOOK_MODE] ?: false,
            showGameMousePointer = prefs[Keys.SHOW_GAME_MOUSE_POINTER] ?: true,
            gameMousePointerScale = (prefs[Keys.GAME_MOUSE_POINTER_SCALE] ?: 0.80f).coerceIn(0.55f, 1.60f),
            overrideAccentHex = prefs[Keys.OVERRIDE_ACCENT_HEX] ?: 0L,
            customDeckHex = prefs[Keys.CUSTOM_DECK_HEX] ?: 0xFFD5DCE5,
            customKeyHex = prefs[Keys.CUSTOM_KEY_HEX] ?: 0xFFDCE1EA,
            customModifierHex = prefs[Keys.CUSTOM_MOD_HEX] ?: 0xFFCFD6E2,
            customTextHex = prefs[Keys.CUSTOM_TEXT_HEX] ?: 0xFF1F2937,
            customAccentHex = prefs[Keys.CUSTOM_ACCENT_HEX] ?: 0xFF4F46E5,
        )
    }

    suspend fun saveHotkeys(hotkeys: List<CustomHotkey>) {
        context.keyDeckDataStore.edit { prefs ->
            prefs[Keys.CUSTOM_HOTKEYS_JSON] = encodeHotkeys(hotkeys)
        }
    }

    suspend fun resetHotkeysToDefault() {
        saveHotkeys(PcKeyboardLayoutData.defaultHotkeys)
    }

    private fun encodeHotkeys(list: List<CustomHotkey>): String {
        val arr = JSONArray()
        list.forEach { hk ->
            arr.put(
                JSONObject().apply {
                    put("id", hk.id)
                    put("label", hk.label)
                    put("keyCode", hk.keyCode)
                    put("keyDisplay", hk.keyDisplay)
                    put("ctrl", hk.ctrl)
                    put("alt", hk.alt)
                    put("shift", hk.shift)
                    put("meta", hk.meta)
                    put("category", hk.category.name)
                    put("accentHex", hk.accentHex)
                    put("actionType", hk.actionType.name)
                    put("targetPackageName", hk.targetPackageName)
                    put("targetAppLabel", hk.targetAppLabel)
                },
            )
        }
        return arr.toString()
    }

    private fun decodeHotkeys(raw: String): List<CustomHotkey> {
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        CustomHotkey(
                            id = obj.getString("id"),
                            label = obj.getString("label"),
                            keyCode = obj.getInt("keyCode"),
                            keyDisplay = obj.getString("keyDisplay"),
                            ctrl = obj.optBoolean("ctrl", false),
                            alt = obj.optBoolean("alt", false),
                            shift = obj.optBoolean("shift", false),
                            meta = obj.optBoolean("meta", false),
                            category = runCatching {
                                HotkeyCategory.valueOf(obj.optString("category", HotkeyCategory.CUSTOM.name))
                            }.getOrDefault(HotkeyCategory.CUSTOM),
                            accentHex = obj.optLong("accentHex", 0xFF4F46E5),
                            actionType = runCatching {
                                HotkeyActionType.valueOf(obj.optString("actionType", HotkeyActionType.KEY_COMBO.name))
                            }.getOrDefault(HotkeyActionType.KEY_COMBO),
                            targetPackageName = obj.optString("targetPackageName", ""),
                            targetAppLabel = obj.optString("targetAppLabel", ""),
                        ),
                    )
                }
            }
        }.getOrElse { PcKeyboardLayoutData.defaultHotkeys }
    }

    suspend fun saveWallpapers(wallpapers: List<KeyboardWallpaper>) {
        context.keyDeckDataStore.edit { prefs ->
            prefs[Keys.SAVED_WALLPAPERS_JSON] = encodeWallpapers(wallpapers)
        }
    }

    private fun encodeWallpapers(list: List<KeyboardWallpaper>): String {
        val arr = JSONArray()
        list.forEach { wp ->
            arr.put(
                JSONObject().apply {
                    put("id", wp.id)
                    put("name", wp.name)
                    put("sourceType", wp.sourceType.name)
                    put("imagePath", wp.imagePath)
                    put("lightness", wp.lightness.toDouble())
                    put("saturation", wp.saturation.toDouble())
                    put("contrast", wp.contrast.toDouble())
                    put("brightness", wp.brightness.toDouble())
                    put("tintHex", wp.tintHex)
                    put("tintIntensity", wp.tintIntensity.toDouble())
                    put("keyOpacity", wp.keyOpacity.toDouble())
                    put("darkBackdrop", wp.darkBackdrop)
                    put("fitCenter", wp.resolvedScaleMode() == WallpaperScaleMode.FIT)
                    put("scaleMode", wp.resolvedScaleMode().name)
                    put("cropZoom", wp.cropZoom.toDouble())
                    put("cropPanX", wp.cropPanX.toDouble())
                    put("cropPanY", wp.cropPanY.toDouble())
                    put("isAnimatedGif", wp.isAnimatedGif)
                    put("createdAtMs", wp.createdAtMs)
                },
            )
        }
        return arr.toString()
    }

    private fun decodeWallpapers(raw: String): List<KeyboardWallpaper> {
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val legacyFit = obj.optBoolean("fitCenter", false)
                    val parsedScaleMode = if (obj.has("scaleMode")) {
                        runCatching {
                            WallpaperScaleMode.valueOf(obj.optString("scaleMode", WallpaperScaleMode.STRETCH.name))
                        }.getOrDefault(if (legacyFit) WallpaperScaleMode.FIT else WallpaperScaleMode.STRETCH)
                    } else {
                        if (legacyFit) WallpaperScaleMode.FIT else WallpaperScaleMode.STRETCH
                    }
                    add(
                        KeyboardWallpaper(
                            id = obj.getString("id"),
                            name = obj.optString("name", "Custom Wallpaper"),
                            sourceType = runCatching {
                                WallpaperSourceType.valueOf(obj.optString("sourceType", WallpaperSourceType.NONE.name))
                            }.getOrDefault(WallpaperSourceType.NONE),
                            imagePath = obj.optString("imagePath", ""),
                            lightness = obj.optDouble("lightness", 1.0).toFloat().coerceIn(0.03f, 1.0f),
                            saturation = obj.optDouble("saturation", 1.0).toFloat().coerceIn(0.0f, 2.0f),
                            contrast = obj.optDouble("contrast", 1.0).toFloat().coerceIn(0.4f, 2.0f),
                            brightness = obj.optDouble("brightness", 0.0).toFloat().coerceIn(-0.5f, 0.5f),
                            tintHex = obj.optLong("tintHex", 0L),
                            tintIntensity = obj.optDouble("tintIntensity", 0.0).toFloat().coerceIn(0f, 0.85f),
                            keyOpacity = obj.optDouble("keyOpacity", 0.48).toFloat().coerceIn(0.15f, 1.0f),
                            darkBackdrop = obj.optBoolean("darkBackdrop", false),
                            fitCenter = parsedScaleMode == WallpaperScaleMode.FIT,
                            scaleMode = parsedScaleMode,
                            cropZoom = obj.optDouble("cropZoom", 1.0).toFloat().coerceIn(0.4f, 4.0f),
                            cropPanX = obj.optDouble("cropPanX", 0.0).toFloat().coerceIn(-1.5f, 1.5f),
                            cropPanY = obj.optDouble("cropPanY", 0.0).toFloat().coerceIn(-1.5f, 1.5f),
                            isAnimatedGif = obj.optBoolean("isAnimatedGif", false),
                            isBuiltIn = false,
                            createdAtMs = obj.optLong("createdAtMs", 0L),
                        ),
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    companion object {
        private const val CURRENT_SCHEMA_VERSION = 4
    }
}
