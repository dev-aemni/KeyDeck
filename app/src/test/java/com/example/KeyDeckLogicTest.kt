package com.example

import android.view.KeyEvent
import androidx.compose.ui.graphics.Color
import com.example.keyboard.AppThemeMode
import com.example.keyboard.CustomHotkey
import com.example.keyboard.HotkeyActionType
import com.example.keyboard.HotkeyCategory
import com.example.keyboard.KeyDeckSettings
import com.example.keyboard.KeyboardLayoutMode
import com.example.keyboard.KeyboardSizePreset
import com.example.keyboard.KeyboardThemeId
import com.example.keyboard.PcKeyboardLayoutData
import com.example.keyboard.RgbBorderMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    }
}
