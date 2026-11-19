package com.example

import android.content.Context
import android.view.KeyEvent
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.keyboard.CustomHotkey
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("KeyDeck", appName)
    }

    @Test
    fun `pc layout has 5 main rows, split rows, gaming keys, and dedicated numpad`() {
        assertEquals(5, PcKeyboardLayoutData.mainRows.size)
        assertEquals(5, PcKeyboardLayoutData.splitPcRows.size)
        assertEquals(5, PcKeyboardLayoutData.numpadRows.size)
        assertEquals(14, PcKeyboardLayoutData.functionRow.size)
        assertEquals(4, PcKeyboardLayoutData.gamingLeftRows.size)

        val gamingLabels = PcKeyboardLayoutData.gamingLeftRows.flatten().map { it.label }
        assertTrue(listOf("W", "A", "S", "D", "Q", "R", "E", "F", "G", "C", "Space", "Shift").all { it in gamingLabels })
        assertTrue(PcKeyboardLayoutData.defaultHotkeys.isNotEmpty())
        assertEquals(KeyboardLayoutMode.PC_STANDARD, KeyDeckSettings().layoutMode)
    }

    @Test
    fun `custom hotkey formats combo and meta flags`() {
        val hk = CustomHotkey(
            id = "test_hk",
            label = "Reopen Tab",
            keyCode = KeyEvent.KEYCODE_T,
            keyDisplay = "T",
            ctrl = true,
            shift = true,
            category = HotkeyCategory.BROWSER,
        )
        assertEquals("Ctrl+Shift+T", hk.comboText())
        val meta = hk.metaState()
        assertTrue((meta and KeyEvent.META_CTRL_ON) != 0)
        assertTrue((meta and KeyEvent.META_SHIFT_ON) != 0)
        assertEquals(0, meta and KeyEvent.META_ALT_ON)
    }

    @Test
    fun `soft ui default theme, black border, gaming-only rgb, and manual trigger resolve accurately`() {
        val defaultSettings = KeyDeckSettings()
        assertTrue(defaultSettings.showKeyBorder)
        assertTrue(defaultSettings.suppressAutoImeOnInput)
        assertEquals(RgbBorderMode.GAMING_ONLY, defaultSettings.rgbBorderMode)
        assertFalse(defaultSettings.isRgbActiveForCurrentMode())

        val softDefault = defaultSettings.resolvePalette()
        assertEquals(Color(0xFFD5DCE5L), softDefault.deckBg)
        assertEquals(Color(0xFFDCE1EAL), softDefault.keyBg)
        assertEquals(Color(0xFFCDE0F8L), softDefault.activeFill)
        assertEquals(Color(0xFF4F46E5L), softDefault.accentBg)
        assertEquals(Color(0xFFF1F4F9L), softDefault.shadowLight)
        assertEquals(Color(0xFFA5AEBCL), softDefault.shadowDark)
        assertEquals(Color(0xFF12151BL), softDefault.keyBorder)

        val gamingActive = defaultSettings.copy(layoutMode = KeyboardLayoutMode.GAMING)
        assertTrue(gamingActive.isRgbActiveForCurrentMode())
        assertTrue(defaultSettings.copy(rgbBorderMode = RgbBorderMode.ALL_MODES).isRgbActiveForCurrentMode())
        assertFalse(gamingActive.copy(rgbBorderMode = RgbBorderMode.OFF).isRgbActiveForCurrentMode())

        val overridden = KeyDeckSettings(
            themeId = KeyboardThemeId.MINT_MIST_NEU,
            overrideAccentHex = 0xFF7C83FDL,
        ).resolvePalette()
        assertEquals(Color(0xFF7C83FDL), overridden.accentBg)
        assertEquals(KeyboardThemeId.MINT_MIST_NEU.keyBg, overridden.keyBg)

        val custom = KeyDeckSettings(
            themeId = KeyboardThemeId.CUSTOM_STUDIO,
            customDeckHex = 0xFFDCD9ECL,
            customKeyHex = 0xFFE3E0F1L,
            customAccentHex = 0xFF0D9488L,
        ).resolvePalette()
        assertEquals(Color(0xFFDCD9ECL), custom.deckBg)
        assertEquals(Color(0xFFE3E0F1L), custom.keyBg)
        assertEquals(Color(0xFF0D9488L), custom.accentBg)
    }

    @Test
    fun `micro size preset stays compact within supported bounds`() {
        val micro = KeyboardSizePreset.MICRO
        assertTrue(micro.keyHeightDp in 24..34)
        assertTrue(micro.widthFraction < 1.0f)
        assertTrue(micro.bubbleSizeDp in 36..44)
    }
}
