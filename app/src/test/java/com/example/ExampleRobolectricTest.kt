package com.example

import android.content.Context
import android.view.KeyEvent
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.keyboard.CustomHotkey
import com.example.keyboard.HotkeyCategory
import com.example.keyboard.KeyDeckSettings
import com.example.keyboard.KeyboardSizePreset
import com.example.keyboard.KeyboardThemeId
import com.example.keyboard.PcKeyboardLayoutData
import org.junit.Assert.assertEquals
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
    fun `pc layout has 5 main rows, function row, and dedicated numpad`() {
        assertEquals(5, PcKeyboardLayoutData.mainRows.size)
        assertEquals(5, PcKeyboardLayoutData.numpadRows.size)
        assertEquals(14, PcKeyboardLayoutData.functionRow.size)
        assertTrue(PcKeyboardLayoutData.defaultHotkeys.isNotEmpty())
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
    fun `soft ui default theme and palette overrides resolve accurately`() {
        val softDefault = KeyDeckSettings().resolvePalette()
        assertEquals(Color(0xFFE0E5ECL), softDefault.deckBg)
        assertEquals(Color(0xFFE0E5ECL), softDefault.keyBg)
        assertEquals(Color(0xFFD7E3F4L), softDefault.activeFill)
        assertEquals(Color(0xFF4F46E5L), softDefault.accentBg)
        assertEquals(Color(0xFFFFFFFFL), softDefault.shadowLight)
        assertEquals(Color(0xFFB9BCC6L), softDefault.shadowDark)

        val overridden = KeyDeckSettings(
            themeId = KeyboardThemeId.MINT_MIST_NEU,
            overrideAccentHex = 0xFF7C83FDL,
        ).resolvePalette()
        assertEquals(Color(0xFF7C83FDL), overridden.accentBg)
        assertEquals(KeyboardThemeId.MINT_MIST_NEU.keyBg, overridden.keyBg)

        val custom = KeyDeckSettings(
            themeId = KeyboardThemeId.CUSTOM_STUDIO,
            customDeckHex = 0xFFE6E4F2L,
            customKeyHex = 0xFFEAEFF6L,
            customAccentHex = 0xFF0D9488L,
        ).resolvePalette()
        assertEquals(Color(0xFFE6E4F2L), custom.deckBg)
        assertEquals(Color(0xFFEAEFF6L), custom.keyBg)
        assertEquals(Color(0xFF0D9488L), custom.accentBg)
    }

    @Test
    fun `micro size preset stays compact within supported bounds`() {
        val micro = KeyboardSizePreset.MICRO
        assertTrue(micro.keyHeightDp in 24..30)
        assertTrue(micro.widthFraction < 1.0f)
        assertTrue(micro.bubbleSizeDp in 36..44)
    }
}
