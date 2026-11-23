package com.example

import android.view.KeyEvent
import androidx.compose.ui.graphics.Color
import com.example.keyboard.AppThemeMode
import com.example.keyboard.BuiltInWallpapers
import com.example.keyboard.CustomHotkey
import com.example.keyboard.HotkeyActionType
import com.example.keyboard.HotkeyCategory
import com.example.keyboard.KeyDeckProfileCodec
import com.example.keyboard.KeyDeckSettings
import com.example.keyboard.KeyboardLayoutMode
import com.example.keyboard.KeyboardSizePreset
import com.example.keyboard.KeyboardThemeId
import com.example.keyboard.KeyboardWallpaper
import com.example.keyboard.PcKeyboardLayoutData
import com.example.keyboard.RgbBorderMode
import com.example.keyboard.SimpleSuggestionEngine
import com.example.keyboard.SimpleTypingController
import com.example.keyboard.WallpaperSourceType
import com.example.keyboard.buildWallpaperColorMatrix
import java.util.Base64
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

        val defaultWp = KeyboardWallpaper(
            id = "wp_default_check",
            name = "Default Crop",
            sourceType = WallpaperSourceType.CUSTOM_IMAGE,
            imagePath = "/tmp/default.png",
        )
        assertEquals(1.0f, defaultWp.cropZoom, 0.001f)
        assertEquals(0.0f, defaultWp.cropPanX, 0.001f)
        assertEquals(0.0f, defaultWp.cropPanY, 0.001f)

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
            cropZoom = 1.65f,
            cropPanX = 0.25f,
            cropPanY = -0.18f,
        )
        assertEquals(custom, BuiltInWallpapers.resolve("custom_1", listOf(custom)))
        assertEquals(BuiltInWallpapers.presets.first(), BuiltInWallpapers.resolve("wp_carbon", listOf(custom)))
        assertNull(BuiltInWallpapers.resolve("", listOf(custom)))

        val matrix = buildWallpaperColorMatrix(saturation = 1.2f, contrast = 1.1f, brightness = -0.1f)
        assertEquals(20, matrix.values.size)
        assertTrue(matrix.values.all { it.isFinite() })
    }

    @Test
    fun `keydeck profile codec exports and imports kdb layouts, shortcuts, and animated gif wallpapers`() {
        assertEquals("My_Gaming_Deck.kdb", KeyDeckProfileCodec.buildFileName("My Gaming Deck"))
        assertEquals("Test_Deck.kdb", KeyDeckProfileCodec.buildFileName("Test/Deck.kdb"))
        assertEquals("Legacy_Deck.kdb", KeyDeckProfileCodec.buildFileName("Legacy Deck.keydeck"))

        val fakeGifBytes = "GIF89a\u0001\u0000\u0001\u0000".toByteArray(Charsets.ISO_8859_1)
        val gifWallpaper = KeyboardWallpaper(
            id = "wp_gif_1",
            name = "Pixel Rain",
            sourceType = WallpaperSourceType.CUSTOM_IMAGE,
            imagePath = "/data/local/wp_gif_1.gif",
            lightness = 0.24f,
            saturation = 1.15f,
            contrast = 1.32f,
            brightness = -0.05f,
            keyOpacity = 0.58f,
            isAnimatedGif = true,
        )
        assertTrue(gifWallpaper.isGifWallpaper())

        val settings = KeyDeckSettings(
            layoutMode = KeyboardLayoutMode.GAMING,
            themeId = KeyboardThemeId.PURE_BLACK,
            appThemeMode = AppThemeMode.DARK,
            keyHeightDp = 42,
            keyboardWidthFraction = 0.90f,
            overlayOpacity = 0.75f,
            showKeyBorder = true,
            simpleSuggestionsEnabled = true,
            simpleAutoCorrectEnabled = false,
            holdToRepeat = false,
        )
        assertTrue(KeyDeckSettings().holdToRepeat)
        val hotkeys = listOf(
            CustomHotkey(
                id = "hk_copy",
                label = "Copy",
                keyCode = KeyEvent.KEYCODE_C,
                keyDisplay = "C",
                ctrl = true,
            ),
            CustomHotkey(
                id = "hk_float",
                label = "Float Deck",
                keyCode = KeyEvent.KEYCODE_F,
                keyDisplay = "F",
                ctrl = true,
                alt = true,
                actionType = HotkeyActionType.TOGGLE_FLOATING,
            ),
        )

        val serialized = KeyDeckProfileCodec.serialize(
            profileName = "Pro Setup",
            settings = settings,
            hotkeys = hotkeys,
            activeWallpaper = gifWallpaper,
            embeddedImageBytes = fakeGifBytes,
            base64Encoder = { Base64.getEncoder().encodeToString(it) },
        )

        assertTrue(serialized.startsWith("@keydeck.aemni"))
        assertTrue(serialized.contains("@version:113"))
        assertTrue(serialized.contains("@kdb.name:Pro Setup"))
        assertTrue(serialized.contains("@kdb.hight:42"))
        assertTrue(serialized.contains("@kdb.width:0.90"))
        assertTrue(serialized.contains("@animated.gif:true"))
        assertTrue(serialized.contains("@kdb.shortcut(Copy): ctrl+c"))
        assertTrue(serialized.contains("@kdb.tranceparency:25%"))
        assertTrue(serialized.contains("@kdb.appearane:dark"))
        assertTrue(serialized.contains("@kdb.contrast:1.32"))
        assertTrue(serialized.contains("@kdb.suggestions:true"))
        assertTrue(serialized.contains("@kdb.autocorrect:false"))
        assertTrue(serialized.contains("@kdb.hold_repeat:false"))

        val parsed = KeyDeckProfileCodec.parse(
            content = serialized,
            base64Decoder = { Base64.getDecoder().decode(it) },
        )!!
        assertEquals("Pro Setup", parsed.profileName)
        assertEquals(113, parsed.formatVersion)
        assertTrue(parsed.isAnimatedGif)
        assertEquals(42, parsed.settings.keyHeightDp)
        assertEquals(0.90f, parsed.settings.keyboardWidthFraction, 0.01f)
        assertEquals(0.75f, parsed.settings.overlayOpacity, 0.01f)
        assertEquals(AppThemeMode.DARK, parsed.settings.appThemeMode)
        assertEquals(KeyboardThemeId.PURE_BLACK, parsed.settings.themeId)
        assertEquals(KeyboardLayoutMode.GAMING, parsed.settings.layoutMode)
        assertTrue(parsed.settings.showKeyBorder)
        assertTrue(parsed.settings.simpleSuggestionsEnabled)
        assertFalse(parsed.settings.simpleAutoCorrectEnabled)
        assertFalse(parsed.settings.holdToRepeat)
        assertEquals(2, parsed.hotkeys?.size)
        assertEquals("Ctrl+C", parsed.hotkeys?.first()?.comboText())
        assertEquals(HotkeyActionType.TOGGLE_FLOATING, parsed.hotkeys?.get(1)?.actionType)
        assertTrue(parsed.embeddedImageBytes?.contentEquals(fakeGifBytes) == true)
        assertEquals(1.32f, parsed.wallpaperConfig?.contrast ?: 0f, 0.01f)
    }

    @Test
    fun `simple mode returns 3 suggestions and applies strict one-time autocorrect without disturbing edits`() {
        val initialEval = SimpleSuggestionEngine.evaluate(activeWord = "")
        assertEquals(3, initialEval.slots.size)
        assertNull(initialEval.autoCorrectReplacement)

        val prefixEval = SimpleSuggestionEngine.evaluate(activeWord = "keyb")
        assertEquals(3, prefixEval.slots.size)
        assertTrue(prefixEval.slots.any { it.text == "keyboard" })

        val controller = SimpleTypingController()

        // 1. Freshly typed clear typo -> autocorrected ONCE on Space
        "teh".forEach { ch ->
            assertNull(controller.onCharTyped(ch, autoCorrectEnabled = true, isCursorTouchingExistingWord = false))
        }
        val evalBeforeSpace = controller.currentEval(autoCorrectEnabled = true)
        assertEquals(3, evalBeforeSpace.slots.size)
        assertEquals("teh", evalBeforeSpace.slots[0].text)
        assertEquals("the", evalBeforeSpace.autoCorrectReplacement)

        val autoResult = controller.onCharTyped(' ', autoCorrectEnabled = true, isCursorTouchingExistingWord = false)
        assertEquals("teh", autoResult?.originalWord)
        assertEquals("the ", autoResult?.replacementWithSeparator)

        // 2. Immediate Backspace reverts to user word "teh" and remembers it so Space NEVER re-corrects it
        val undo = controller.onBackspace()
        assertEquals("teh", undo?.originalWord)
        assertEquals("the ", undo?.replacedTextWithSeparator)
        assertTrue("teh" in controller.learnedCustomWords)

        val secondSpace = controller.onCharTyped(' ', autoCorrectEnabled = true, isCursorTouchingExistingWord = false)
        assertNull(secondSpace)

        // 3. Any manual Backspace while typing disarms autocorrect for the edited word
        "htea".forEach { ch -> controller.onCharTyped(ch, autoCorrectEnabled = true) }
        assertNull(controller.onBackspace()) // User edited back to "hte"
        assertFalse(controller.wordEligibleForAutoCorrect)
        assertNull(controller.onCharTyped(' ', autoCorrectEnabled = true))

        // 4. Editing an existing/previous word (cursor touching letters or cursor moved) NEVER autocorrects
        controller.resetOnCursorOrBoundary()
        assertNull(controller.onCharTyped('x', autoCorrectEnabled = true, isCursorTouchingExistingWord = true))
        assertFalse(controller.wordEligibleForAutoCorrect)
        assertNull(controller.onCharTyped(' ', autoCorrectEnabled = true))

        // 5. Known dictionary word is never rewritten
        "hello".forEach { ch -> controller.onCharTyped(ch, autoCorrectEnabled = true, isCursorTouchingExistingWord = false) }
        assertNull(controller.onCharTyped(' ', autoCorrectEnabled = true))
    }

    @Test
    fun `simple mode predicts contextual next words accurately for phrases like I love`() {
        val controller = SimpleTypingController()
        "I love ".forEach { ch ->
            controller.onCharTyped(ch, autoCorrectEnabled = true, isCursorTouchingExistingWord = false)
        }
        val afterLove = controller.currentEval(autoCorrectEnabled = true)
        assertEquals(listOf("you", "it", "that"), afterLove.slots.map { it.text })

        // Typing a prefix boosts contextual match first
        controller.onCharTyped('y', autoCorrectEnabled = true, isCursorTouchingExistingWord = false)
        val afterY = controller.currentEval(autoCorrectEnabled = true)
        assertEquals("y", afterY.slots[0].text)
        assertEquals("you", afterY.slots[1].text)

        // Syncing from an existing cursor context also yields accurate next-word predictions
        val synced = SimpleTypingController()
        synced.syncContextFromSurroundingText("Thank you ")
        assertEquals(listOf("so", "very", "much"), synced.currentEval(true).slots.map { it.text })

        // Sentence boundary resets back to sentence starters
        synced.syncContextFromSurroundingText("I love you. ")
        assertEquals(listOf("I", "The", "Hello"), synced.currentEval(true).slots.map { it.text })
    }
}
