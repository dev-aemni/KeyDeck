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
import com.example.keyboard.CustomHotkey
import com.example.keyboard.DispatchedKeyRecord
import com.example.keyboard.HotkeyCategory
import com.example.keyboard.KeyDeckSettings
import com.example.keyboard.KeyboardDockAlignment
import com.example.keyboard.KeyboardLayoutMode
import com.example.keyboard.KeyboardThemeId
import com.example.keyboard.PcKeyboardLayoutData
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
        val LAYOUT_MODE = stringPreferencesKey("layout_mode")
        val THEME_ID = stringPreferencesKey("theme_id")
        val SHOW_HOTKEY_BAR = booleanPreferencesKey("show_hotkey_bar")
        val SHOW_MODE_STRIP = booleanPreferencesKey("show_mode_strip")
        val SHOW_FUNCTION_ROW = booleanPreferencesKey("show_function_row")
        val NUM_LOCK_ON = booleanPreferencesKey("num_lock_on")
        val STICKY_MODIFIERS = booleanPreferencesKey("sticky_modifiers")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
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

    suspend fun updateSettings(transform: (KeyDeckSettings) -> KeyDeckSettings) {
        context.keyDeckDataStore.edit { prefs ->
            val current = decodeSettings(prefs)
            val updated = transform(current)
            prefs[Keys.LAYOUT_MODE] = updated.layoutMode.name
            prefs[Keys.THEME_ID] = updated.themeId.name
            prefs[Keys.SHOW_HOTKEY_BAR] = updated.showHotkeyBar
            prefs[Keys.SHOW_MODE_STRIP] = updated.showModeStrip
            prefs[Keys.SHOW_FUNCTION_ROW] = updated.showFunctionRow
            prefs[Keys.NUM_LOCK_ON] = updated.numLockOn
            prefs[Keys.STICKY_MODIFIERS] = updated.stickyModifiers
            prefs[Keys.HAPTIC_FEEDBACK] = updated.hapticFeedback
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
            prefs[Keys.OVERRIDE_ACCENT_HEX] = updated.overrideAccentHex
            prefs[Keys.CUSTOM_DECK_HEX] = updated.customDeckHex
            prefs[Keys.CUSTOM_KEY_HEX] = updated.customKeyHex
            prefs[Keys.CUSTOM_MOD_HEX] = updated.customModifierHex
            prefs[Keys.CUSTOM_TEXT_HEX] = updated.customTextHex
            prefs[Keys.CUSTOM_ACCENT_HEX] = updated.customAccentHex
        }
    }

    private fun decodeSettings(prefs: Preferences): KeyDeckSettings {
        val layoutName = prefs[Keys.LAYOUT_MODE] ?: KeyboardLayoutMode.PC_STANDARD.name
        val themeName = prefs[Keys.THEME_ID] ?: KeyboardThemeId.SOFT_NEUMORPH.name
        val dockName = prefs[Keys.DOCK_ALIGNMENT] ?: KeyboardDockAlignment.CENTER.name
        return KeyDeckSettings(
            layoutMode = runCatching { KeyboardLayoutMode.valueOf(layoutName) }
                .getOrDefault(KeyboardLayoutMode.PC_STANDARD),
            themeId = runCatching { KeyboardThemeId.valueOf(themeName) }
                .getOrDefault(KeyboardThemeId.SOFT_NEUMORPH),
            showHotkeyBar = prefs[Keys.SHOW_HOTKEY_BAR] ?: true,
            showModeStrip = prefs[Keys.SHOW_MODE_STRIP] ?: true,
            showFunctionRow = prefs[Keys.SHOW_FUNCTION_ROW] ?: true,
            numLockOn = prefs[Keys.NUM_LOCK_ON] ?: true,
            stickyModifiers = prefs[Keys.STICKY_MODIFIERS] ?: true,
            hapticFeedback = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
            keyHeightDp = (prefs[Keys.KEY_HEIGHT_DP] ?: 34).coerceIn(24, 54),
            keyboardWidthFraction = (prefs[Keys.WIDTH_FRACTION] ?: 1.0f).coerceIn(0.60f, 1.0f),
            dockAlignment = runCatching { KeyboardDockAlignment.valueOf(dockName) }
                .getOrDefault(KeyboardDockAlignment.CENTER),
            keyHorizontalGapDp = (prefs[Keys.KEY_H_GAP_DP] ?: 3).coerceIn(1, 6),
            keyVerticalGapDp = (prefs[Keys.KEY_V_GAP_DP] ?: 4).coerceIn(1, 7),
            deckPaddingDp = (prefs[Keys.DECK_PADDING_DP] ?: 6).coerceIn(1, 10),
            keyCornerRadiusDp = (prefs[Keys.KEY_CORNER_DP] ?: 11).coerceIn(4, 16),
            fontScale = (prefs[Keys.FONT_SCALE] ?: 0.92f).coerceIn(0.70f, 1.25f),
            overlayOpacity = (prefs[Keys.OVERLAY_OPACITY] ?: 0.97f).coerceIn(0.50f, 1.0f),
            bubbleSizeDp = (prefs[Keys.BUBBLE_SIZE_DP] ?: 48).coerceIn(36, 68),
            overrideAccentHex = prefs[Keys.OVERRIDE_ACCENT_HEX] ?: 0L,
            customDeckHex = prefs[Keys.CUSTOM_DECK_HEX] ?: 0xFFE0E5EC,
            customKeyHex = prefs[Keys.CUSTOM_KEY_HEX] ?: 0xFFE0E5EC,
            customModifierHex = prefs[Keys.CUSTOM_MOD_HEX] ?: 0xFFE0E5EC,
            customTextHex = prefs[Keys.CUSTOM_TEXT_HEX] ?: 0xFF4B5563,
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
                            accentHex = obj.optLong("accentHex", 0xFFE95663),
                        ),
                    )
                }
            }
        }.getOrElse { PcKeyboardLayoutData.defaultHotkeys }
    }
}
