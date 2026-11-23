package com.example.keyboard

import android.view.KeyEvent
import androidx.compose.ui.graphics.Color

enum class KeyboardLayoutMode(val label: String, val shortLabel: String, val subtitle: String) {
    PC_WITH_NUMPAD("PC + Numpad", "PC+Num", "10-col QWERTY + dedicated 4x5 numeric pad"),
    PC_STANDARD("PC Full", "PC", "Full 6-row PC QWERTY + F1–F12"),
    SIMPLE("Simple Mobile", "Simple", "Gboard-style 5-row QWERTY with hold-to-write symbols & accents"),
    NUMPAD_ONLY("Numeric Pad", "Num", "Large 4x5 dedicated numeric keypad"),
    GAMING("Gaming + Mouse", "Game", "Left WASD/QREFGC keys + right mouse touchpad"),
}

enum class KeyboardDockAlignment(val label: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right"),
}

enum class RgbBorderMode(val label: String) {
    GAMING_ONLY("Gaming Only"),
    ALL("All"),
    OFF("Off"),
}

enum class AppThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

enum class KeyboardSizePreset(
    val label: String,
    val keyHeightDp: Int,
    val widthFraction: Float,
    val hGapDp: Int,
    val vGapDp: Int,
    val deckPaddingDp: Int,
    val fontScale: Float,
    val cornerRadiusDp: Int,
    val bubbleSizeDp: Int,
) {
    MICRO("Compact", 30, 0.90f, 2, 2, 4, 0.86f, 8, 42),
    TIGHT("Tight", 34, 0.96f, 2, 3, 5, 0.92f, 10, 46),
    BALANCED("Balanced", 38, 1.00f, 3, 4, 6, 1.00f, 11, 50),
    COMFORT("Comfort", 46, 1.00f, 4, 5, 8, 1.12f, 13, 58),
}

enum class KeyboardThemeId(
    val displayName: String,
    val deckBg: Color,
    val keyBg: Color,
    val modifierBg: Color,
    val activeFill: Color,
    val accentBg: Color,
    val shadowLight: Color,
    val shadowDark: Color,
    val keyBorder: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val activeIndicator: Color,
    val isDark: Boolean = false,
    val themeBorderColor: Color = accentBg,
) {
    SOFT_NEUMORPH(
        displayName = "Soft UI Slate",
        deckBg = Color(0xFFD5DCE5),
        keyBg = Color(0xFFDCE1EA),
        modifierBg = Color(0xFFCFD6E2),
        activeFill = Color(0xFFCDE0F8),
        accentBg = Color(0xFF4F46E5),
        shadowLight = Color(0xFFF1F4F9),
        shadowDark = Color(0xFFA5AEBC),
        keyBorder = Color.Black,
        primaryText = Color(0xFF1F2937),
        secondaryText = Color(0xFF4B5563),
        activeIndicator = Color(0xFF4F46E5),
        isDark = false,
        themeBorderColor = Color(0xFF64748B),
    ),
    LAVENDER_NEU(
        displayName = "Soft Lavender",
        deckBg = Color(0xFFDCD9EC),
        keyBg = Color(0xFFE3E0F1),
        modifierBg = Color(0xFFD3CFE6),
        activeFill = Color(0xFFD2CBFA),
        accentBg = Color(0xFF6D28D9),
        shadowLight = Color(0xFFF3F1FA),
        shadowDark = Color(0xFFAEB0C9),
        keyBorder = Color.Black,
        primaryText = Color(0xFF2E244D),
        secondaryText = Color(0xFF5A5178),
        activeIndicator = Color(0xFF6D28D9),
        isDark = false,
        themeBorderColor = Color(0xFF7C3AED),
    ),
    WARM_SAND_NEU(
        displayName = "Warm Stone",
        deckBg = Color(0xFFE2DCD4),
        keyBg = Color(0xFFE9E4DC),
        modifierBg = Color(0xFFD8D0C6),
        activeFill = Color(0xFFF2DBC2),
        accentBg = Color(0xFFD97706),
        shadowLight = Color(0xFFF6F2EC),
        shadowDark = Color(0xFFB8AEA2),
        keyBorder = Color.Black,
        primaryText = Color(0xFF332A22),
        secondaryText = Color(0xFF63574B),
        activeIndicator = Color(0xFFD97706),
        isDark = false,
        themeBorderColor = Color(0xFFD97706),
    ),
    MINT_MIST_NEU(
        displayName = "Sage Mist",
        deckBg = Color(0xFFD1DFDA),
        keyBg = Color(0xFFD9E6E1),
        modifierBg = Color(0xFFC6D6D0),
        activeFill = Color(0xFFBFE7DA),
        accentBg = Color(0xFF0D9488),
        shadowLight = Color(0xFFECF4F1),
        shadowDark = Color(0xFFA2B7AF),
        keyBorder = Color.Black,
        primaryText = Color(0xFF1E352E),
        secondaryText = Color(0xFF46655C),
        activeIndicator = Color(0xFF0D9488),
        isDark = false,
        themeBorderColor = Color(0xFF0D9488),
    ),
    DARK_SLATE_NEU(
        displayName = "Dark Neumorph",
        deckBg = Color(0xFF222630),
        keyBg = Color(0xFF282D39),
        modifierBg = Color(0xFF1E222B),
        activeFill = Color(0xFF1E2A4A),
        accentBg = Color(0xFF6366F1),
        shadowLight = Color(0xFF353B49),
        shadowDark = Color(0xFF14171E),
        keyBorder = Color.Black,
        primaryText = Color(0xFFE2E8F0),
        secondaryText = Color(0xFF94A3B8),
        activeIndicator = Color(0xFF818CF8),
        isDark = true,
        themeBorderColor = Color(0xFF64748B),
    ),
    MIDNIGHT_NEU(
        displayName = "Midnight Gaming",
        deckBg = Color(0xFF171D2B),
        keyBg = Color(0xFF1D2436),
        modifierBg = Color(0xFF151A27),
        activeFill = Color(0xFF1F355C),
        accentBg = Color(0xFF38BDF8),
        shadowLight = Color(0xFF2A344D),
        shadowDark = Color(0xFF0D111A),
        keyBorder = Color.Black,
        primaryText = Color(0xFFF1F5F9),
        secondaryText = Color(0xFF93C5FD),
        activeIndicator = Color(0xFF38BDF8),
        isDark = true,
        themeBorderColor = Color(0xFF38BDF8),
    ),
    ROSE_QUARTZ(
        displayName = "Dusty Rose",
        deckBg = Color(0xFFE7D9DD),
        keyBg = Color(0xFFEEE1E5),
        modifierBg = Color(0xFFDDCBD0),
        activeFill = Color(0xFFF7CBD7),
        accentBg = Color(0xFFE11D48),
        shadowLight = Color(0xFFF7EEF1),
        shadowDark = Color(0xFFBDA8AE),
        keyBorder = Color.Black,
        primaryText = Color(0xFF3E242C),
        secondaryText = Color(0xFF6D4C56),
        activeIndicator = Color(0xFFE11D48),
        isDark = false,
        themeBorderColor = Color(0xFFE11D48),
    ),
    CYBER_MINT(
        displayName = "Carbon Stealth",
        deckBg = Color(0xFF16191D),
        keyBg = Color(0xFF1C2026),
        modifierBg = Color(0xFF13161A),
        activeFill = Color(0xFF13332C),
        accentBg = Color(0xFF10B981),
        shadowLight = Color(0xFF272C34),
        shadowDark = Color(0xFF0B0D10),
        keyBorder = Color.Black,
        primaryText = Color(0xFFE2FDF8),
        secondaryText = Color(0xFF6EE7B7),
        activeIndicator = Color(0xFF10B981),
        isDark = true,
        themeBorderColor = Color(0xFF10B981),
    ),
    PURE_BLACK(
        displayName = "Pure Black #000",
        deckBg = Color(0xFF000000),
        keyBg = Color(0xFF0A0A0A),
        modifierBg = Color(0xFF141414),
        activeFill = Color(0xFF1E293B),
        accentBg = Color(0xFF3B82F6),
        shadowLight = Color(0xFF161616),
        shadowDark = Color(0xFF000000),
        keyBorder = Color(0xFF262626),
        primaryText = Color(0xFFFFFFFF),
        secondaryText = Color(0xFFA3A3A3),
        activeIndicator = Color(0xFF60A5FA),
        isDark = true,
        themeBorderColor = Color(0xFF52525B),
    ),
    CLASSIC_DARK(
        displayName = "Classic Dark",
        deckBg = Color(0xFF1B1C1E),
        keyBg = Color(0xFF2E3033),
        modifierBg = Color(0xFF232528),
        activeFill = Color(0xFF2A4165),
        accentBg = Color(0xFF60A5FA),
        shadowLight = Color(0xFF3B3E44),
        shadowDark = Color(0xFF101113),
        keyBorder = Color.Black,
        primaryText = Color(0xFFF1F3F4),
        secondaryText = Color(0xFF9AA0A6),
        activeIndicator = Color(0xFF60A5FA),
        isDark = true,
        themeBorderColor = Color(0xFF6B7280),
    ),
    DEEP_CHARCOAL(
        displayName = "Deep Charcoal",
        deckBg = Color(0xFF121212),
        keyBg = Color(0xFF212121),
        modifierBg = Color(0xFF181818),
        activeFill = Color(0xFF2C3E50),
        accentBg = Color(0xFF38BDF8),
        shadowLight = Color(0xFF2E2E2E),
        shadowDark = Color(0xFF080808),
        keyBorder = Color(0xFF303030),
        primaryText = Color(0xFFFAFAFA),
        secondaryText = Color(0xFFA1A1AA),
        activeIndicator = Color(0xFF38BDF8),
        isDark = true,
        themeBorderColor = Color(0xFF71717A),
    ),
    NAVY_BLUE(
        displayName = "Classic Navy",
        deckBg = Color(0xFF0F172A),
        keyBg = Color(0xFF1E293B),
        modifierBg = Color(0xFF172033),
        activeFill = Color(0xFF1E3A8A),
        accentBg = Color(0xFF38BDF8),
        shadowLight = Color(0xFF2C3B52),
        shadowDark = Color(0xFF090D16),
        keyBorder = Color.Black,
        primaryText = Color(0xFFF8FAFC),
        secondaryText = Color(0xFF94A3B8),
        activeIndicator = Color(0xFF38BDF8),
        isDark = true,
        themeBorderColor = Color(0xFF38BDF8),
    ),
    PURE_WHITE(
        displayName = "Clean White",
        deckBg = Color(0xFFF3F4F6),
        keyBg = Color(0xFFFFFFFF),
        modifierBg = Color(0xFFE5E7EB),
        activeFill = Color(0xFFDBEAFE),
        accentBg = Color(0xFF2563EB),
        shadowLight = Color(0xFFFFFFFF),
        shadowDark = Color(0xFFD1D5DB),
        keyBorder = Color.Black,
        primaryText = Color(0xFF111827),
        secondaryText = Color(0xFF4B5563),
        activeIndicator = Color(0xFF2563EB),
        isDark = false,
        themeBorderColor = Color(0xFF9CA3AF),
    ),
    NEUTRAL_GRAY(
        displayName = "Neutral Gray",
        deckBg = Color(0xFFE5E7EB),
        keyBg = Color(0xFFF9FAFB),
        modifierBg = Color(0xFFD1D5DB),
        activeFill = Color(0xFFBFDBFE),
        accentBg = Color(0xFF1D4ED8),
        shadowLight = Color(0xFFFFFFFF),
        shadowDark = Color(0xFF9CA3AF),
        keyBorder = Color.Black,
        primaryText = Color(0xFF1F2937),
        secondaryText = Color(0xFF4B5563),
        activeIndicator = Color(0xFF1D4ED8),
        isDark = false,
        themeBorderColor = Color(0xFF6B7280),
    ),
    CLASSIC_SLATE(
        displayName = "Cool Blue-Gray",
        deckBg = Color(0xFFCFD8DC),
        keyBg = Color(0xFFECEFF1),
        modifierBg = Color(0xFFB0BEC5),
        activeFill = Color(0xFFBBDEFB),
        accentBg = Color(0xFF1976D2),
        shadowLight = Color(0xFFFFFFFF),
        shadowDark = Color(0xFF90A4AE),
        keyBorder = Color.Black,
        primaryText = Color(0xFF263238),
        secondaryText = Color(0xFF546E7A),
        activeIndicator = Color(0xFF1976D2),
        isDark = false,
        themeBorderColor = Color(0xFF64748B),
    ),
    WARM_CREAM(
        displayName = "Minimal Cream",
        deckBg = Color(0xFFF5F2EB),
        keyBg = Color(0xFFFFFFFF),
        modifierBg = Color(0xFFE7E2D8),
        activeFill = Color(0xFFFDE68A),
        accentBg = Color(0xFFB45309),
        shadowLight = Color(0xFFFFFFFF),
        shadowDark = Color(0xFFD6CFC2),
        keyBorder = Color.Black,
        primaryText = Color(0xFF292524),
        secondaryText = Color(0xFF57534E),
        activeIndicator = Color(0xFFB45309),
        isDark = false,
        themeBorderColor = Color(0xFFB45309),
    ),
    CUSTOM_STUDIO(
        displayName = "Custom Studio",
        deckBg = Color(0xFFD5DCE5),
        keyBg = Color(0xFFDCE1EA),
        modifierBg = Color(0xFFCFD6E2),
        activeFill = Color(0xFFCDE0F8),
        accentBg = Color(0xFF4F46E5),
        shadowLight = Color(0xFFF1F4F9),
        shadowDark = Color(0xFFA5AEBC),
        keyBorder = Color.Black,
        primaryText = Color(0xFF1F2937),
        secondaryText = Color(0xFF4B5563),
        activeIndicator = Color(0xFF4F46E5),
        isDark = false,
        themeBorderColor = Color(0xFF4F46E5),
    ),
}

data class ResolvedKeyboardPalette(
    val deckBg: Color,
    val keyBg: Color,
    val modifierBg: Color,
    val activeFill: Color,
    val accentBg: Color,
    val shadowLight: Color,
    val shadowDark: Color,
    val keyBorder: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val activeIndicator: Color,
    val isDark: Boolean,
)

enum class ModifierKey {
    CTRL, ALT, SHIFT, META, FN, CAPS
}

enum class HotkeyCategory(val label: String) {
    ALL("All"),
    EDITING("Editing"),
    BROWSER("Browser"),
    NAVIGATION("Navigation"),
    CUSTOM("Custom"),
}

enum class HotkeyActionType(val label: String) {
    KEY_COMBO("Key Combo"),
    LAUNCH_APP("Launch App"),
    TOGGLE_FLOATING("Float Deck"),
}

data class CustomHotkey(
    val id: String,
    val label: String,
    val keyCode: Int,
    val keyDisplay: String,
    val ctrl: Boolean = false,
    val alt: Boolean = false,
    val shift: Boolean = false,
    val meta: Boolean = false,
    val category: HotkeyCategory = HotkeyCategory.CUSTOM,
    val accentHex: Long = 0xFF4F46E5,
    val actionType: HotkeyActionType = HotkeyActionType.KEY_COMBO,
    val targetPackageName: String = "",
    val targetAppLabel: String = "",
) {
    fun comboText(): String {
        val parts = mutableListOf<String>()
        if (ctrl) parts += "Ctrl"
        if (alt) parts += "Alt"
        if (shift) parts += "Shift"
        if (meta) parts += "Win"
        parts += keyDisplay
        return parts.joinToString("+")
    }

    fun actionSummary(): String {
        return when (actionType) {
            HotkeyActionType.LAUNCH_APP -> {
                if (targetPackageName.isNotBlank()) {
                    val appName = targetAppLabel.ifBlank { targetPackageName }
                    "${comboText()} → $appName"
                } else {
                    comboText()
                }
            }
            HotkeyActionType.TOGGLE_FLOATING -> "${comboText()} → Float Deck"
            HotkeyActionType.KEY_COMBO -> comboText()
        }
    }

    fun matchesKeyEvent(eventKeyCode: Int, eventMetaState: Int): Boolean {
        if (eventKeyCode != keyCode) return false
        val hasCtrl = (eventMetaState and KeyEvent.META_CTRL_ON) != 0
        val hasAlt = (eventMetaState and KeyEvent.META_ALT_ON) != 0
        val hasShift = (eventMetaState and KeyEvent.META_SHIFT_ON) != 0
        val hasMeta = (eventMetaState and KeyEvent.META_META_ON) != 0
        return hasCtrl == ctrl && hasAlt == alt && hasShift == shift && hasMeta == meta
    }

    fun metaState(): Int {
        var metaFlags = 0
        if (ctrl) metaFlags = metaFlags or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (alt) metaFlags = metaFlags or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        if (shift) metaFlags = metaFlags or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        if (meta) metaFlags = metaFlags or KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON
        return metaFlags
    }
}

data class KeySpec(
    val id: String,
    val label: String,
    val shiftLabel: String? = null,
    val fnLabel: String? = null,
    val keyCode: Int,
    val fnKeyCode: Int? = null,
    val charPrimary: Char? = null,
    val charShift: Char? = null,
    val weight: Float = 1f,
    val isModifier: Boolean = false,
    val modifierType: ModifierKey? = null,
    val isAccent: Boolean = false,
    val iconType: KeyIconType? = null,
    val cornerHint: String? = null,
    val longPressChars: List<String> = emptyList(),
    val longPressShiftChars: List<String> = emptyList(),
)

enum class KeyIconType {
    BACKSPACE,
    TAB,
    ENTER,
    SHIFT,
    ARROW_UP,
    ARROW_DOWN,
    ARROW_LEFT,
    ARROW_RIGHT,
    META_WIN,
    SPACE,
    MIC_OR_NUM,
    LAYER_CIRCLE,
}

data class KeyDeckSettings(
    val layoutMode: KeyboardLayoutMode = KeyboardLayoutMode.PC_STANDARD,
    val themeId: KeyboardThemeId = KeyboardThemeId.SOFT_NEUMORPH,
    val appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val syncKeyboardWithDarkMode: Boolean = true,
    val showModeStrip: Boolean = true,
    val showFunctionRow: Boolean = false,
    val numLockOn: Boolean = true,
    val stickyModifiers: Boolean = true,
    val hapticFeedback: Boolean = true,
    val suppressAutoImeOnInput: Boolean = true,
    val simpleSuggestionsEnabled: Boolean = true,
    val simpleAutoCorrectEnabled: Boolean = true,
    val holdToRepeat: Boolean = true,
    // Granular size & tightness customization
    val keyHeightDp: Int = 38,
    val keyboardWidthFraction: Float = 1.0f,
    val dockAlignment: KeyboardDockAlignment = KeyboardDockAlignment.CENTER,
    val keyHorizontalGapDp: Int = 3,
    val keyVerticalGapDp: Int = 4,
    val deckPaddingDp: Int = 6,
    val keyCornerRadiusDp: Int = 10,
    val fontScale: Float = 0.96f,
    val overlayOpacity: Float = 0.96f,
    val bubbleSizeDp: Int = 48,
    // Visual borders, RGB & Wallpapers
    val rgbBorderMode: RgbBorderMode = RgbBorderMode.GAMING_ONLY,
    val showKeyBorder: Boolean = false,
    val themeAdaptiveBorder: Boolean = false,
    val activeWallpaperId: String = "",
    // Gaming & Touchpad tuning
    val touchpadSensitivity: Float = 1.6f,
    val touchpadWidthFraction: Float = 0.42f,
    val gamingMcpeLookMode: Boolean = true,
    // Custom color overrides
    val overrideAccentHex: Long = 0L,
    val customDeckHex: Long = 0xFFD5DCE5,
    val customKeyHex: Long = 0xFFDCE1EA,
    val customModifierHex: Long = 0xFFCFD6E2,
    val customTextHex: Long = 0xFF1F2937,
    val customAccentHex: Long = 0xFF4F46E5,
) {
    fun isKeyBorderVisible(): Boolean = showKeyBorder || themeAdaptiveBorder

    fun isHapticActiveForCurrentMode(): Boolean =
        hapticFeedback && layoutMode == KeyboardLayoutMode.SIMPLE

    fun isRgbActiveForCurrentMode(): Boolean = when (rgbBorderMode) {
        RgbBorderMode.GAMING_ONLY -> layoutMode == KeyboardLayoutMode.GAMING
        RgbBorderMode.ALL -> true
        RgbBorderMode.OFF -> false
    }

    fun resolvePalette(isDarkUi: Boolean = false): ResolvedKeyboardPalette {
        val effectiveTheme = if (isDarkUi && syncKeyboardWithDarkMode && themeId == KeyboardThemeId.SOFT_NEUMORPH) {
            KeyboardThemeId.DARK_SLATE_NEU
        } else {
            themeId
        }
        val themeBorder = when {
            overrideAccentHex != 0L -> Color(overrideAccentHex)
            effectiveTheme == KeyboardThemeId.CUSTOM_STUDIO -> Color(customAccentHex)
            else -> effectiveTheme.themeBorderColor
        }
        return if (effectiveTheme == KeyboardThemeId.CUSTOM_STUDIO) {
            val deck = Color(customDeckHex)
            val key = Color(customKeyHex)
            val mod = Color(customModifierHex)
            val text = Color(customTextHex)
            val accent = Color(if (overrideAccentHex != 0L) overrideAccentHex else customAccentHex)
            val isLightDeck = (deck.red * 0.299f + deck.green * 0.587f + deck.blue * 0.114f) > 0.5f
            val sLight = if (isLightDeck) Color(0xFFF3F6FA) else androidx.compose.ui.graphics.lerp(deck, Color.White, 0.12f)
            val sDark = if (isLightDeck) androidx.compose.ui.graphics.lerp(deck, Color(0xFF7B8596), 0.45f) else androidx.compose.ui.graphics.lerp(deck, Color.Black, 0.48f)
            val active = if (isLightDeck) Color(0xFFCDE0F8) else androidx.compose.ui.graphics.lerp(deck, accent, 0.26f)
            val border = if (themeAdaptiveBorder) themeBorder else Color.Black
            ResolvedKeyboardPalette(
                deckBg = deck,
                keyBg = key,
                modifierBg = mod,
                activeFill = active,
                accentBg = accent,
                shadowLight = sLight,
                shadowDark = sDark,
                keyBorder = border,
                primaryText = text,
                secondaryText = text.copy(alpha = 0.74f),
                activeIndicator = accent,
                isDark = !isLightDeck,
            )
        } else {
            val accent = if (overrideAccentHex != 0L) Color(overrideAccentHex) else effectiveTheme.accentBg
            val indicator = if (overrideAccentHex != 0L) Color(overrideAccentHex) else effectiveTheme.activeIndicator
            val border = if (themeAdaptiveBorder) themeBorder else effectiveTheme.keyBorder
            ResolvedKeyboardPalette(
                deckBg = effectiveTheme.deckBg,
                keyBg = effectiveTheme.keyBg,
                modifierBg = effectiveTheme.modifierBg,
                activeFill = effectiveTheme.activeFill,
                accentBg = accent,
                shadowLight = effectiveTheme.shadowLight,
                shadowDark = effectiveTheme.shadowDark,
                keyBorder = border,
                primaryText = effectiveTheme.primaryText,
                secondaryText = effectiveTheme.secondaryText,
                activeIndicator = indicator,
                isDark = effectiveTheme.isDark,
            )
        }
    }
}

data class DispatchedKeyRecord(
    val timestampMs: Long,
    val comboLabel: String,
    val keyCode: Int,
    val route: String,
    val detail: String,
)
