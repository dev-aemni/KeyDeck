package com.example

import android.view.KeyEvent
import androidx.compose.ui.graphics.Color
import com.example.keyboard.AppThemeMode
import com.example.keyboard.BuiltInWallpapers
import com.example.keyboard.CustomHotkey
import com.example.keyboard.HotkeyActionType
import com.example.keyboard.HotkeyCategory
import com.example.keyboard.KeyDeckSettings
import com.example.keyboard.KeyboardLayoutMode
import com.example.keyboard.KeyboardSizePreset
import com.example.keyboard.KeyboardThemeId
import com.example.keyboard.KeyboardWallpaper
import com.example.keyboard.PcKeyboardLayoutData
import com.example.keyboard.RgbBorderMode
import com.example.keyboard.WallpaperSourceType
import com.example.keyboard.buildWallpaperColorMatrix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyDeckLogicTest {

    @Test
    fun `simple mobile layout has 5-row gboard qwerty, symbol layers, and hold-to-write accents`() {
        assertEquals(5, PcKeyboardLayoutData.simpleAlphaRows.size)
        assertEquals(4, PcKeyboardLayoutData.simpleSymbolRows.size)
        assertEquals(4, PcKeyboardLayoutData.simpleSymbolAltRows.size)

        val numberRow = PcKeyboardLayoutData.simpleAlphaRows.first()
        assertEquals(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"), numberRow.map { it.label })

        val iKey = PcKeyboardLayoutData.simpleAlphaRows[1].first { it.id == "sm_i" }
        assertEquals(">", iKey.cornerHint)
        assertTrue(iKey.longPressChars.containsAll(listOf("Į", "ī", "ì", "î", ">", "í", "ï")))

        val alphaBottom = PcKeyboardLayoutData.simpleAlphaRows.last()
        assertTrue(alphaBottom.any { it.id == "simple_sym_toggle" && it.label == "?123" })
        assertTrue(alphaBottom.any { it.keyCode == KeyEvent.KEYCODE_SPACE })
        assertTrue(alphaBottom.any { it.keyCode == KeyEvent.KEYCODE_ENTER })

        val symBottom = PcKeyboardLayoutData.simpleSymbolRows.last()
        assertTrue(symBottom.any { it.id == "simple_sym_toggle" && it.label == "ABC" })
        assertTrue(PcKeyboardLayoutData.simpleSymbolRows[2].any { it.id == "simple_sym_alt_toggle" && it.label == "=\\<" })
        assertTrue(PcKeyboardLayoutData.simpleSymbolAltRows[2].any { it.id == "simple_sym_alt_toggle" && it.label == "?123" })
    }

    @Test
    fun `rgb border modes resolve correctly across all keyboard modes`() {
        val defaultSettings = KeyDeckSettings()
        assertEquals(RgbBorderMode.GAMING_ONLY, defaultSettings.rgbBorderMode)
        assertFalse(defaultSettings.isRgbActiveForCurrentMode())
        assertFalse(defaultSettings.copy(layoutMode = KeyboardLayoutMode.SIMPLE).isRgbActiveForCurrentMode())
        assertFalse(defaultSettings.copy(layoutMode = KeyboardLayoutMode.PC_WITH_NUMPAD).isRgbActiveForCurrentMode())
        assertFalse(defaultSettings.copy(layoutMode = KeyboardLayoutMode.NUMPAD_ONLY).isRgbActiveForCurrentMode())
        assertTrue(defaultSettings.copy(layoutMode = KeyboardLayoutMode.GAMING).isRgbActiveForCurrentMode())

        val allRgb = defaultSettings.copy(rgbBorderMode = RgbBorderMode.ALL)
        KeyboardLayoutMode.entries.forEach { mode ->
            assertTrue(allRgb.copy(layoutMode = mode).isRgbActiveForCurrentMode())
        }

        val offRgb = defaultSettings.copy(rgbBorderMode = RgbBorderMode.OFF)
        KeyboardLayoutMode.entries.forEach { mode ->
            assertFalse(offRgb.copy(layoutMode = mode).isRgbActiveForCurrentMode())
        }
    }

    @Test
    fun `dark mode palette and custom app-launch hotkeys work properly`() {
        val autoDark = KeyDeckSettings(appThemeMode = AppThemeMode.DARK).resolvePalette(isDarkUi = true)
        assertTrue(autoDark.isDark)
        assertEquals(KeyboardThemeId.DARK_SLATE_NEU.deckBg, autoDark.deckBg)
        assertEquals(Color.Black, autoDark.keyBorder)

        val hk = CustomHotkey(
            id = "hk_1",
            label = "Copy",
            keyCode = KeyEvent.KEYCODE_C,
            keyDisplay = "C",
            ctrl = true,
            category = HotkeyCategory.EDITING,
        )
        assertEquals("Ctrl+C", hk.comboText())
        assertTrue((hk.metaState() and KeyEvent.META_CTRL_ON) != 0)
        assertTrue(KeyboardSizePreset.MICRO.keyHeightDp in 24..34)

        val launchHk = CustomHotkey(
            id = "hk_app",
            label = "Open WhatsApp",
            keyCode = KeyEvent.KEYCODE_W,
            keyDisplay = "W",
            ctrl = true,
            alt = true,
            actionType = HotkeyActionType.LAUNCH_APP,
            targetPackageName = "com.whatsapp",
            targetAppLabel = "WhatsApp",
        )
        assertEquals("Ctrl+Alt+W → WhatsApp", launchHk.actionSummary())
        assertTrue(launchHk.matchesKeyEvent(KeyEvent.KEYCODE_W, KeyEvent.META_CTRL_ON or KeyEvent.META_ALT_ON))
        assertFalse(launchHk.matchesKeyEvent(KeyEvent.KEYCODE_W, KeyEvent.META_CTRL_ON))
        assertFalse(launchHk.matchesKeyEvent(KeyEvent.KEYCODE_Q, KeyEvent.META_CTRL_ON or KeyEvent.META_ALT_ON))

        val floatHk = PcKeyboardLayoutData.defaultHotkeys.first { it.actionType == HotkeyActionType.TOGGLE_FLOATING }
        assertEquals("Ctrl+Alt+F", floatHk.comboText())
        assertEquals("Ctrl+Alt+F → Float Deck", floatHk.actionSummary())
        assertTrue(floatHk.matchesKeyEvent(KeyEvent.KEYCODE_F, KeyEvent.META_CTRL_ON or KeyEvent.META_ALT_ON))
        assertFalse(floatHk.matchesKeyEvent(KeyEvent.KEYCODE_F, KeyEvent.META_CTRL_ON))

        val pureBlack = KeyDeckSettings(themeId = KeyboardThemeId.PURE_BLACK).resolvePalette()
        assertEquals(Color(0xFF000000), pureBlack.deckBg)
        assertTrue(pureBlack.isDark)

        val pureWhite = KeyDeckSettings(themeId = KeyboardThemeId.PURE_WHITE).resolvePalette()
        assertFalse(pureWhite.isDark)

        val metaKey = PcKeyboardLayoutData.mainRows.last().first { it.id == "meta" }
        assertEquals(com.example.keyboard.KeyIconType.META_WIN, metaKey.iconType)
    }

    @Test
    fun `key borders default off and theme-adaptive borders track theme colors`() {
        val defaults = KeyDeckSettings()
        assertFalse(defaults.showKeyBorder)
        assertFalse(defaults.themeAdaptiveBorder)
        assertFalse(defaults.isKeyBorderVisible())

        assertTrue(defaults.copy(showKeyBorder = true).isKeyBorderVisible())
        assertTrue(defaults.copy(themeAdaptiveBorder = true).isKeyBorderVisible())

        val greenThemeBorder = KeyDeckSettings(
            themeId = KeyboardThemeId.CYBER_MINT,
            themeAdaptiveBorder = true,
        ).resolvePalette().keyBorder
        assertEquals(KeyboardThemeId.CYBER_MINT.themeBorderColor, greenThemeBorder)

        val grayThemeBorder = KeyDeckSettings(
            themeId = KeyboardThemeId.NEUTRAL_GRAY,
            themeAdaptiveBorder = true,
        ).resolvePalette().keyBorder
        assertEquals(KeyboardThemeId.NEUTRAL_GRAY.themeBorderColor, grayThemeBorder)

        val pureBlackAdaptive = KeyDeckSettings(
            themeId = KeyboardThemeId.PURE_BLACK,
            themeAdaptiveBorder = true,
        ).resolvePalette().keyBorder
        assertEquals(KeyboardThemeId.PURE_BLACK.themeBorderColor, pureBlackAdaptive)

        val accentOverrideBorder = KeyDeckSettings(
            themeId = KeyboardThemeId.CYBER_MINT,
            themeAdaptiveBorder = true,
            overrideAccentHex = 0xFFF43F5E,
        ).resolvePalette().keyBorder
        assertEquals(Color(0xFFF43F5E), accentOverrideBorder)
    }

    @Test
    fun `built-in pattern wallpapers and color filter matrix work properly`() {
        assertEquals(7, BuiltInWallpapers.presets.size)
        BuiltInWallpapers.presets.forEach { preset ->
            assertTrue(preset.isBuiltIn)
            assertTrue(preset.darkBackdrop)
            assertTrue(preset.lightness in 0.04f..0.40f)
            assertFalse(preset.sourceType == WallpaperSourceType.CUSTOM_IMAGE)
            assertFalse(preset.sourceType == WallpaperSourceType.NONE)
        }

        val custom = KeyboardWallpaper(
            id = "custom_1",
            name = "My Dark Logo",
            sourceType = WallpaperSourceType.CUSTOM_IMAGE,
            imagePath = "/tmp/logo.png",
            lightness = 0.14f,
            saturation = 1.15f,
            contrast = 1.10f,
            brightness = -0.05f,
            darkBackdrop = true,
            fitCenter = true,
        )
        assertEquals(custom, BuiltInWallpapers.resolve("custom_1", listOf(custom)))
        assertEquals(BuiltInWallpapers.presets.first(), BuiltInWallpapers.resolve("wp_carbon", listOf(custom)))
        assertNull(BuiltInWallpapers.resolve("", listOf(custom)))

        val matrix = buildWallpaperColorMatrix(saturation = 1.2f, contrast = 1.1f, brightness = -0.1f)
        assertEquals(20, matrix.values.size)
        assertTrue(matrix.values.all { it.isFinite() })
    }
}
