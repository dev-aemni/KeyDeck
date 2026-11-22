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
import com.example.keyboard.PcKeyboardLayoutData
import com.example.keyboard.RgbBorderMode
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
        val TOUCHPAD_SENSITIVITY = floatPreferencesKey("touchpad_sensitivity")
        val TOUCHPAD_WIDTH_FRACTION = floatPreferencesKey("touchpad_width_fraction")
        val GAMING_MCPE_LOOK_MODE = booleanPreferencesKey("gaming_mcpe_look_mode")
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

    suspend fun migrateSchemaIfNeeded() {
        context.keyDeckDataStore.edit { prefs ->
            val currentVersion = prefs[Keys.SETTINGS_SCHEMA_VERSION] ?: 0
            if (currentVersion < CURRENT_SCHEMA_VERSION) {
                prefs[Keys.SETTINGS_SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION
                prefs[Keys.LAYOUT_MODE] = KeyboardLayoutMode.PC_STANDARD.name
                prefs[Keys.RGB_BORDER_MODE] = RgbBorderMode.GAMING_ONLY.name
                prefs[Keys.SHOW_KEY_BORDER] = true
                prefs[Keys.SUPPRESS_AUTO_IME_ON_INPUT] = true
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
            prefs[Keys.TOUCHPAD_SENSITIVITY] = updated.touchpadSensitivity
            prefs[Keys.TOUCHPAD_WIDTH_FRACTION] = updated.touchpadWidthFraction
            prefs[Keys.GAMING_MCPE_LOOK_MODE] = updated.gamingMcpeLookMode
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
        val isLegacyV1 = schemaVersion < CURRENT_SCHEMA_VERSION
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
            keyHeightDp = (prefs[Keys.KEY_HEIGHT_DP] ?: 38).coerceIn(26, 54),
            keyboardWidthFraction = (prefs[Keys.WIDTH_FRACTION] ?: 1.0f).coerceIn(0.60f, 1.0f),
            dockAlignment = runCatching { KeyboardDockAlignment.valueOf(dockName) }
                .getOrDefault(KeyboardDockAlignment.CENTER),
            keyHorizontalGapDp = (prefs[Keys.KEY_H_GAP_DP] ?: 3).coerceIn(1, 6),
            keyVerticalGapDp = (prefs[Keys.KEY_V_GAP_DP] ?: 4).coerceIn(1, 7),
            deckPaddingDp = (prefs[Keys.DECK_PADDING_DP] ?: 6).coerceIn(1, 10),
            keyCornerRadiusDp = (prefs[Keys.KEY_CORNER_DP] ?: 10).coerceIn(4, 16),
            fontScale = (prefs[Keys.FONT_SCALE] ?: 0.96f).coerceIn(0.75f, 1.25f),
            overlayOpacity = (prefs[Keys.OVERLAY_OPACITY] ?: 0.96f).coerceIn(0.50f, 1.0f),
            bubbleSizeDp = (prefs[Keys.BUBBLE_SIZE_DP] ?: 48).coerceIn(36, 68),
            rgbBorderMode = runCatching { RgbBorderMode.valueOf(rgbName) }
                .getOrDefault(RgbBorderMode.GAMING_ONLY),
            showKeyBorder = if (isLegacyV1) true else (prefs[Keys.SHOW_KEY_BORDER] ?: true),
            touchpadSensitivity = (prefs[Keys.TOUCHPAD_SENSITIVITY] ?: 1.6f).coerceIn(0.6f, 3.5f),
            touchpadWidthFraction = (prefs[Keys.TOUCHPAD_WIDTH_FRACTION] ?: 0.42f).coerceIn(0.30f, 0.55f),
            gamingMcpeLookMode = prefs[Keys.GAMING_MCPE_LOOK_MODE] ?: true,
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

    companion object {
        private const val CURRENT_SCHEMA_VERSION = 2
    }
}
