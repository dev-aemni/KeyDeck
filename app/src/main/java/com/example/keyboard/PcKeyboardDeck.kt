package com.example.keyboard

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.KeyboardTab
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.KeyDeckRuntimeState
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.KeyDeckColors
import com.example.ui.theme.neumorphicInset
import com.example.ui.theme.neumorphicKeyCap
import com.example.ui.theme.neumorphicRaised
import com.example.ui.theme.rgbAnimatedBorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.ceil
import kotlin.math.roundToInt

private data class ActiveHoldPopup(
    val keyRectInRoot: Rect,
    val options: List<String>,
    val selectedIndex: Int,
)

private data class ActiveKeyPreview(
    val keyRectInRoot: Rect,
    val label: String,
)

@Composable
fun PcKeyboardDeck(
    settings: KeyDeckSettings,
    routeLabel: String,
    isOverlay: Boolean,
    onKeyDispatch: (keyCode: Int, metaState: Int, charToCommit: Char?, comboLabel: String) -> Unit,
    onRepeatKeyDispatch: ((keyCode: Int, metaState: Int, charToCommit: Char?, comboLabel: String) -> Unit)? = null,
    onChangeLayoutMode: (KeyboardLayoutMode) -> Unit,
    onToggleNumLock: () -> Unit,
    onToggleFunctionRow: () -> Unit,
    hotkeys: List<CustomHotkey> = emptyList(),
    onHotkeyDispatch: ((CustomHotkey) -> Unit)? = null,
    onUpdateSettings: ((KeyDeckSettings) -> KeyDeckSettings) -> Unit = {},
    onKeyHoldDispatch: ((keyCode: Int, metaState: Int, isDown: Boolean, comboLabel: String) -> Unit)? = null,
    onMouseMove: ((dx: Float, dy: Float) -> Unit)? = null,
    onMouseClick: ((isRightClick: Boolean) -> Unit)? = null,
    onMouseEnd: (() -> Unit)? = null,
    onOverlayDrag: ((dx: Float, dy: Float) -> Unit)? = null,
    onResizeDragDelta: ((dx: Float, dy: Float) -> Unit)? = null,
    onMinimize: (() -> Unit)? = null,
    onCloseOverlay: (() -> Unit)? = null,
    onSetupAccessibility: (() -> Unit)? = null,
    onSetupIme: (() -> Unit)? = null,
    activeWallpaper: KeyboardWallpaper? = null,
    enableLiveSuggestions: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isAppDark = KeyDeckColors.current.isDark
    val basePalette = remember(settings, isAppDark) { settings.resolvePalette(isAppDark) }
    val hasWallpaper = activeWallpaper != null && activeWallpaper.sourceType != WallpaperSourceType.NONE
    val keyOpacity = if (hasWallpaper) activeWallpaper!!.keyOpacity.coerceIn(0.15f, 1.0f) else 1f
    val palette = remember(basePalette, activeWallpaper) {
        if (activeWallpaper != null && activeWallpaper.sourceType != WallpaperSourceType.NONE && activeWallpaper.darkBackdrop) {
            basePalette.copy(
                deckBg = Color.Black,
                keyBg = if (basePalette.isDark) {
                    androidx.compose.ui.graphics.lerp(basePalette.keyBg, Color(0xFF0A0A0D), 0.55f)
                } else {
                    Color(0xFF0E1015)
                },
                modifierBg = if (basePalette.isDark) {
                    androidx.compose.ui.graphics.lerp(basePalette.modifierBg, Color(0xFF050608), 0.55f)
                } else {
                    Color(0xFF151820)
                },
                activeFill = androidx.compose.ui.graphics.lerp(Color(0xFF121620), basePalette.accentBg, 0.28f),
                shadowLight = Color(0xFF222630),
                shadowDark = Color(0xFF000000),
                primaryText = Color(0xFFF8FAFC),
                secondaryText = Color(0xFFCBD5E1),
                isDark = true,
            )
        } else {
            basePalette
        }
    }
    var ctrlActive by remember { mutableStateOf(false) }
    var altActive by remember { mutableStateOf(false) }
    var shiftActive by remember { mutableStateOf(false) }
    var capsActive by remember { mutableStateOf(false) }
    var metaActive by remember { mutableStateOf(false) }
    var fnActive by remember { mutableStateOf(false) }

    var showShortcutsStrip by remember { mutableStateOf(false) }
    var showClipboardStrip by remember { mutableStateOf(false) }
    var showResizeStrip by remember { mutableStateOf(false) }
    var showQuickSettingsPopup by remember { mutableStateOf(false) }
    var showCloseConfirm by remember { mutableStateOf(false) }
    var simpleSymbolLayer by remember { mutableStateOf(false) }
    var simpleSymbolAltLayer by remember { mutableStateOf(false) }
    val clipboardClips by ClipboardHistoryStore.clips.collectAsStateWithLifecycle()

    var rootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var activeHoldPopup by remember { mutableStateOf<ActiveHoldPopup?>(null) }
    var activeKeyPreview by remember { mutableStateOf<ActiveKeyPreview?>(null) }
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val typingController = remember { SimpleTypingController() }
    var suggestionEval by remember {
        mutableStateOf(typingController.currentEval(settings.simpleAutoCorrectEnabled))
    }
    var autoShiftEligible by remember { mutableStateOf(false) }
    var autoShiftDisarmedForCursor by remember { mutableStateOf(false) }
    val inputSessionGen by KeyDeckRuntimeState.inputSessionGeneration.collectAsStateWithLifecycle()

    fun refreshSuggestionEval() {
        suggestionEval = typingController.currentEval(settings.simpleAutoCorrectEnabled)
    }

    fun refreshAutoCapitalize(beforeCursorOverride: String? = null) {
        if (!enableLiveSuggestions ||
            settings.layoutMode != KeyboardLayoutMode.SIMPLE ||
            !settings.simpleAutoCapitalize ||
            KeyDispatcher.shouldDisableSuggestions()
        ) {
            autoShiftEligible = false
            return
        }
        val before = beforeCursorOverride ?: KeyDispatcher.getSurroundingChars(32, 0)?.first
        autoShiftEligible = SimpleSmartTyping.shouldAutoCapitalize(before)
    }

    LaunchedEffect(
        inputSessionGen,
        settings.layoutMode,
        settings.simpleSuggestionsEnabled,
        settings.simpleAutoCorrectEnabled,
        settings.simpleAutoCapitalize,
        enableLiveSuggestions,
    ) {
        if (settings.layoutMode != KeyboardLayoutMode.SIMPLE || !enableLiveSuggestions) {
            typingController.resetOnCursorOrBoundary(clearLastWord = true)
            refreshSuggestionEval()
            autoShiftEligible = false
            autoShiftDisarmedForCursor = false
        } else {
            val before = KeyDispatcher.getSurroundingChars(48, 0)?.first
            if (settings.simpleSuggestionsEnabled) {
                if (before != null && typingController.activeWord.isEmpty()) {
                    typingController.syncContextFromSurroundingText(before)
                }
            } else {
                typingController.resetOnCursorOrBoundary(clearLastWord = true)
            }
            refreshSuggestionEval()
            refreshAutoCapitalize(before)
        }
    }

    LaunchedEffect(inputSessionGen) {
        typingController.resetOnCursorOrBoundary(clearLastWord = true)
        autoShiftDisarmedForCursor = false
        val before = if (enableLiveSuggestions && settings.layoutMode == KeyboardLayoutMode.SIMPLE) {
            KeyDispatcher.getSurroundingChars(48, 0)?.first
        } else {
            null
        }
        if (before != null && settings.simpleSuggestionsEnabled) {
            typingController.syncContextFromSurroundingText(before)
        }
        refreshSuggestionEval()
        refreshAutoCapitalize(before)
    }

    LaunchedEffect(settings.layoutMode) {
        if (settings.layoutMode != KeyboardLayoutMode.SIMPLE) {
            simpleSymbolLayer = false
            simpleSymbolAltLayer = false
            activeKeyPreview = null
            autoShiftEligible = false
            autoShiftDisarmedForCursor = false
        }
    }

    LaunchedEffect(settings.simpleClipboardStripEnabled) {
        if (!settings.simpleClipboardStripEnabled) {
            showClipboardStrip = false
        }
    }

    val lastDispatched by KeyDeckRuntimeState.lastDispatchedKey.collectAsStateWithLifecycle()
    LaunchedEffect(
        lastDispatched?.timestampMs,
        settings.layoutMode,
        settings.simpleSuggestionsEnabled,
        settings.simpleAutoCapitalize,
        enableLiveSuggestions,
    ) {
        if (!enableLiveSuggestions || settings.layoutMode != KeyboardLayoutMode.SIMPLE) {
            return@LaunchedEffect
        }
        delay(18L)
        if (!KeyDispatcher.shouldDisableSuggestions()) {
            val before = KeyDispatcher.getSurroundingChars(48, 0)?.first
            if (before != null && settings.simpleSuggestionsEnabled && typingController.activeWord.isEmpty()) {
                typingController.syncContextFromSurroundingText(before)
                refreshSuggestionEval()
            }
            refreshAutoCapitalize(before)
        } else {
            autoShiftEligible = false
        }
    }
    var overlayFeedbackText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(isOverlay, lastDispatched?.timestampMs) {
        if (!isOverlay) {
            overlayFeedbackText = null
            return@LaunchedEffect
        }
        val rec = lastDispatched ?: return@LaunchedEffect
        val shouldShow = rec.route == "Setup" ||
            rec.detail.startsWith("Tap an input") ||
            rec.detail.startsWith("Enable ") ||
            rec.detail.startsWith("Select KeyDeck")
        if (shouldShow) {
            overlayFeedbackText = rec.detail
            delay(2600L)
            overlayFeedbackText = null
        } else if (overlayFeedbackText != null) {
            overlayFeedbackText = null
        }
    }

    val rgbActive = settings.isRgbActiveForCurrentMode()
    val rgbPhaseDeg by if (rgbActive) {
        val transition = rememberInfiniteTransition(label = "rgb_border_transition")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "rgb_phase_deg",
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val keyHeight = settings.keyHeightDp.coerceIn(26, 54).dp
    val hGap = settings.keyHorizontalGapDp.coerceIn(1, 6).dp
    val vGap = settings.keyVerticalGapDp.coerceIn(1, 7).dp
    val deckPadding = settings.deckPaddingDp.coerceIn(1, 10).dp
    val keyCorner = settings.keyCornerRadiusDp.coerceIn(4, 16).dp
    val fontScale = settings.fontScale.coerceIn(0.75f, 1.25f)

    val autoShiftActive = enableLiveSuggestions &&
        settings.layoutMode == KeyboardLayoutMode.SIMPLE &&
        settings.simpleAutoCapitalize &&
        !simpleSymbolLayer &&
        !capsActive &&
        !autoShiftDisarmedForCursor &&
        autoShiftEligible &&
        !KeyDispatcher.shouldDisableSuggestions()
    val effectiveUpperCase = (shiftActive || autoShiftActive) xor capsActive

    fun currentMetaState(): Int {
        var flags = 0
        if (ctrlActive) flags = flags or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (altActive) flags = flags or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        if (shiftActive) flags = flags or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        if (capsActive) flags = flags or KeyEvent.META_CAPS_LOCK_ON
        if (metaActive) flags = flags or KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON
        if (fnActive) flags = flags or KeyEvent.META_FUNCTION_ON
        return flags
    }

    fun formatComboLabel(baseLabel: String): String {
        val parts = mutableListOf<String>()
        if (ctrlActive) parts += "Ctrl"
        if (altActive) parts += "Alt"
        if (shiftActive) parts += "Shift"
        if (metaActive) parts += "Win"
        if (fnActive) parts += "Fn"
        parts += baseLabel
        return parts.joinToString("+")
    }

    fun clearStickyModifiersIfNeeded() {
        if (settings.stickyModifiers) {
            ctrlActive = false
            altActive = false
            shiftActive = false
            metaActive = false
        }
    }

    fun handleKeyTap(spec: KeySpec, isFromNumpad: Boolean = false, isRepeat: Boolean = false) {
        if (isRepeat && (
                spec.modifierType != null ||
                    spec.id == "simple_sym_toggle" ||
                    spec.id == "simple_sym_alt_toggle" ||
                    spec.id == "num_toggle_top" ||
                    spec.id == "num_toggle" ||
                    spec.id == "num_lock"
                )
        ) {
            return
        }
        when (spec.modifierType) {
            ModifierKey.CTRL -> {
                ctrlActive = !ctrlActive
                return
            }
            ModifierKey.ALT -> {
                altActive = !altActive
                return
            }
            ModifierKey.SHIFT -> {
                if (settings.layoutMode == KeyboardLayoutMode.SIMPLE && autoShiftActive && !shiftActive) {
                    autoShiftDisarmedForCursor = true
                    shiftActive = false
                } else {
                    autoShiftDisarmedForCursor = true
                    shiftActive = !shiftActive
                }
                return
            }
            ModifierKey.CAPS -> {
                autoShiftDisarmedForCursor = true
                capsActive = !capsActive
                return
            }
            ModifierKey.META -> {
                metaActive = !metaActive
                return
            }
            ModifierKey.FN -> {
                fnActive = !fnActive
                return
            }
            null -> Unit
        }

        if (spec.id == "simple_sym_toggle") {
            simpleSymbolLayer = !simpleSymbolLayer
            simpleSymbolAltLayer = false
            return
        }

        if (spec.id == "simple_sym_alt_toggle") {
            simpleSymbolAltLayer = !simpleSymbolAltLayer
            return
        }

        if (spec.id == "num_toggle_top" || spec.id == "num_toggle") {
            val nextMode = when (settings.layoutMode) {
                KeyboardLayoutMode.PC_WITH_NUMPAD -> KeyboardLayoutMode.NUMPAD_ONLY
                KeyboardLayoutMode.NUMPAD_ONLY -> KeyboardLayoutMode.PC_STANDARD
                else -> KeyboardLayoutMode.PC_WITH_NUMPAD
            }
            onChangeLayoutMode(nextMode)
            return
        }

        if (spec.id == "num_lock") {
            onToggleNumLock()
            return
        }

        val effectiveKeyCode = when {
            isFromNumpad && !settings.numLockOn && spec.fnKeyCode != null -> spec.fnKeyCode
            fnActive && spec.fnKeyCode != null -> spec.fnKeyCode
            else -> spec.keyCode
        }

        val isLetter = spec.charPrimary?.isLetter() == true
        val useShiftedChar = if (isLetter) effectiveUpperCase else shiftActive
        val effectiveChar: Char? = when {
            isFromNumpad && !settings.numLockOn -> null
            fnActive && spec.fnKeyCode != null -> null
            useShiftedChar && spec.charShift != null -> spec.charShift
            else -> spec.charPrimary
        }

        val displayLabel = when {
            isFromNumpad && !settings.numLockOn && spec.fnLabel != null -> spec.fnLabel
            fnActive && spec.fnLabel != null -> spec.fnLabel
            useShiftedChar && spec.shiftLabel != null -> spec.shiftLabel
            else -> spec.label
        }

        val isSimpleSmartMode =
            enableLiveSuggestions &&
                settings.layoutMode == KeyboardLayoutMode.SIMPLE
        val isSimpleSuggestionTracking =
            isSimpleSmartMode && settings.simpleSuggestionsEnabled

        val effectiveMeta = currentMetaState()
        val hasCtrlAltMeta = (effectiveMeta and (KeyEvent.META_CTRL_ON or KeyEvent.META_ALT_ON or KeyEvent.META_META_ON)) != 0

        if (isSimpleSmartMode) {
            if (hasCtrlAltMeta) {
                typingController.resetOnCursorOrBoundary(clearLastWord = true)
                refreshSuggestionEval()
                autoShiftDisarmedForCursor = false
            } else if (effectiveKeyCode == KeyEvent.KEYCODE_DEL) {
                val undo = typingController.onBackspace()
                refreshSuggestionEval()
                autoShiftDisarmedForCursor = false
                if (undo != null) {
                    val isPeriodUndo = undo.originalWord == " " && undo.replacedTextWithSeparator == ". "
                    val reverted = KeyDispatcher.dispatchSuggestionReplace(
                        context = context,
                        expectedTail = undo.replacedTextWithSeparator,
                        replacement = undo.originalWord,
                        comboLabel = if (isPeriodUndo) "Undo Period" else "Undo Auto → ${undo.originalWord}",
                        hapticEnabled = settings.isHapticActiveForCurrentMode(),
                    )
                    if (reverted) {
                        autoShiftEligible = false
                        clearStickyModifiersIfNeeded()
                        return
                    }
                    // Target did not match; clear state and fall through to normal Backspace
                    typingController.resetOnCursorOrBoundary(clearLastWord = true)
                    refreshSuggestionEval()
                }
            } else if (
                effectiveKeyCode == KeyEvent.KEYCODE_ENTER ||
                effectiveKeyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
            ) {
                typingController.resetOnCursorOrBoundary(clearLastWord = true)
                refreshSuggestionEval()
                autoShiftDisarmedForCursor = false
                if (settings.simpleAutoCapitalize && !KeyDispatcher.shouldDisableSuggestions()) {
                    autoShiftEligible = true
                }
            } else if (
                effectiveKeyCode == KeyEvent.KEYCODE_TAB ||
                effectiveKeyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                effectiveKeyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                effectiveKeyCode == KeyEvent.KEYCODE_DPAD_UP ||
                effectiveKeyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                effectiveKeyCode == KeyEvent.KEYCODE_MOVE_HOME ||
                effectiveKeyCode == KeyEvent.KEYCODE_MOVE_END ||
                effectiveKeyCode == KeyEvent.KEYCODE_FORWARD_DEL ||
                effectiveKeyCode == KeyEvent.KEYCODE_ESCAPE
            ) {
                typingController.resetOnCursorOrBoundary(clearLastWord = true)
                refreshSuggestionEval()
                autoShiftDisarmedForCursor = false
            } else if (effectiveChar == ' ' && !isRepeat && !KeyDispatcher.shouldDisableSuggestions()) {
                val nowMs = System.currentTimeMillis()
                val beforeCursor = KeyDispatcher.getSurroundingChars(32, 0)?.first
                if (settings.simpleDoubleSpacePeriod &&
                    SimpleSmartTyping.canConvertDoubleSpaceToPeriod(
                        beforeCursor = beforeCursor,
                        lastSpaceTapAtMs = typingController.lastSpaceTapAtMs,
                        nowMs = nowMs,
                    )
                ) {
                    val replacedPeriod = KeyDispatcher.dispatchSuggestionReplace(
                        context = context,
                        expectedTail = " ",
                        replacement = ". ",
                        comboLabel = "Double-Space → .",
                        hapticEnabled = settings.isHapticActiveForCurrentMode(),
                    )
                    if (replacedPeriod) {
                        typingController.registerDoubleSpacePeriod()
                        refreshSuggestionEval()
                        autoShiftDisarmedForCursor = false
                        if (settings.simpleAutoCapitalize) {
                            autoShiftEligible = true
                        }
                        clearStickyModifiersIfNeeded()
                        return
                    }
                }

                if (isSimpleSuggestionTracking) {
                    val cursorTouchingExistingWord = if (typingController.activeWord.isEmpty()) {
                        KeyDispatcher.isCursorTouchingWord()
                    } else {
                        false
                    }
                    val autoAction = typingController.onCharTyped(
                        ch = ' ',
                        autoCorrectEnabled = settings.simpleAutoCorrectEnabled,
                        isCursorTouchingExistingWord = cursorTouchingExistingWord,
                        smartCapitalizeSoloI = settings.simpleAutoCorrectEnabled || settings.simpleSmartPunctuation,
                    )
                    typingController.recordSpaceTapped(nowMs)
                    refreshSuggestionEval()
                    if (autoAction != null) {
                        val replaced = KeyDispatcher.dispatchSuggestionReplace(
                            context = context,
                            expectedTail = autoAction.originalWord,
                            replacement = autoAction.replacementWithSeparator,
                            comboLabel = "Auto: ${autoAction.replacementWithSeparator.trimEnd()}",
                            hapticEnabled = settings.isHapticActiveForCurrentMode(),
                        )
                        if (replaced) {
                            autoShiftDisarmedForCursor = false
                            if (settings.simpleAutoCapitalize) {
                                autoShiftEligible = SimpleSmartTyping.shouldAutoCapitalize(
                                    (beforeCursor?.dropLast(autoAction.originalWord.length) ?: "") + autoAction.replacementWithSeparator,
                                )
                            }
                            clearStickyModifiersIfNeeded()
                            return
                        }
                    }
                } else {
                    typingController.recordSpaceTapped(nowMs)
                }

                autoShiftDisarmedForCursor = false
                if (settings.simpleAutoCapitalize) {
                    autoShiftEligible = SimpleSmartTyping.shouldAutoCapitalize((beforeCursor ?: "") + " ")
                }
            } else if (
                effectiveChar != null &&
                SimpleSmartTyping.isSmartPunctuationMark(effectiveChar) &&
                settings.simpleSmartPunctuation &&
                typingController.trailingAutoSpaceAfterCommit &&
                typingController.activeWord.isEmpty() &&
                !isRepeat &&
                !KeyDispatcher.shouldDisableSuggestions()
            ) {
                val replacedPunct = KeyDispatcher.dispatchSuggestionReplace(
                    context = context,
                    expectedTail = " ",
                    replacement = "$effectiveChar ",
                    comboLabel = "Smart Punct → $effectiveChar",
                    hapticEnabled = settings.isHapticActiveForCurrentMode(),
                )
                if (replacedPunct) {
                    typingController.markSmartPunctuationAfterSpace(effectiveChar)
                    refreshSuggestionEval()
                    autoShiftDisarmedForCursor = false
                    if (settings.simpleAutoCapitalize) {
                        autoShiftEligible = SimpleSmartTyping.isSentenceEndingPunctuation(effectiveChar)
                    }
                    clearStickyModifiersIfNeeded()
                    return
                }
            } else if (effectiveChar != null && !KeyDispatcher.shouldDisableSuggestions()) {
                autoShiftEligible = false
                autoShiftDisarmedForCursor = false
                if (isSimpleSuggestionTracking) {
                    val cursorTouchingExistingWord = if (typingController.activeWord.isEmpty()) {
                        KeyDispatcher.isCursorTouchingWord()
                    } else {
                        false
                    }
                    val autoAction = typingController.onCharTyped(
                        ch = effectiveChar,
                        autoCorrectEnabled = settings.simpleAutoCorrectEnabled,
                        isCursorTouchingExistingWord = cursorTouchingExistingWord,
                        smartCapitalizeSoloI = settings.simpleAutoCorrectEnabled || settings.simpleSmartPunctuation,
                    )
                    refreshSuggestionEval()
                    if (autoAction != null) {
                        val replaced = KeyDispatcher.dispatchSuggestionReplace(
                            context = context,
                            expectedTail = autoAction.originalWord,
                            replacement = autoAction.replacementWithSeparator,
                            comboLabel = "Auto: ${autoAction.replacementWithSeparator.trimEnd()}",
                            hapticEnabled = settings.isHapticActiveForCurrentMode(),
                        )
                        if (replaced) {
                            if (settings.simpleAutoCapitalize) {
                                autoShiftEligible = false
                            }
                            clearStickyModifiersIfNeeded()
                            return
                        }
                    }
                }
            } else if (effectiveChar == null) {
                typingController.resetOnCursorOrBoundary(clearLastWord = true)
                refreshSuggestionEval()
                autoShiftDisarmedForCursor = false
            } else {
                autoShiftEligible = false
                autoShiftDisarmedForCursor = false
            }
        }

        val dispatcher = if (isRepeat) (onRepeatKeyDispatch ?: onKeyDispatch) else onKeyDispatch
        dispatcher(
            effectiveKeyCode,
            effectiveMeta,
            effectiveChar,
            formatComboLabel(displayLabel),
        )
        clearStickyModifiersIfNeeded()
    }

    fun handleHoldCharCommit(chosen: String) {
        val ch = chosen.firstOrNull()
        if (settings.layoutMode == KeyboardLayoutMode.SIMPLE) {
            autoShiftEligible = false
            autoShiftDisarmedForCursor = false
        }
        if (enableLiveSuggestions &&
            settings.layoutMode == KeyboardLayoutMode.SIMPLE &&
            settings.simpleSuggestionsEnabled &&
            ch != null &&
            !KeyDispatcher.shouldDisableSuggestions()
        ) {
            val touching = if (typingController.activeWord.isEmpty()) {
                KeyDispatcher.isCursorTouchingWord()
            } else {
                false
            }
            val autoAction = typingController.onCharTyped(
                ch = ch,
                autoCorrectEnabled = settings.simpleAutoCorrectEnabled,
                isCursorTouchingExistingWord = touching,
                smartCapitalizeSoloI = settings.simpleAutoCorrectEnabled || settings.simpleSmartPunctuation,
            )
            refreshSuggestionEval()
            if (autoAction != null) {
                val replaced = KeyDispatcher.dispatchSuggestionReplace(
                    context = context,
                    expectedTail = autoAction.originalWord,
                    replacement = autoAction.replacementWithSeparator,
                    comboLabel = "Auto: ${autoAction.replacementWithSeparator.trimEnd()}",
                    hapticEnabled = settings.isHapticActiveForCurrentMode(),
                )
                if (replaced) {
                    clearStickyModifiersIfNeeded()
                    return
                }
            }
        }
        onKeyDispatch(
            KeyEvent.KEYCODE_UNKNOWN,
            0,
            ch,
            chosen,
        )
        clearStickyModifiersIfNeeded()
    }

    fun handleSpaceCursorSlide(direction: Int) {
        if (settings.layoutMode == KeyboardLayoutMode.SIMPLE) {
            typingController.resetOnCursorOrBoundary(clearLastWord = true)
            typingController.clearDoubleSpaceHistory()
            refreshSuggestionEval()
            autoShiftDisarmedForCursor = false
        }
        val code = if (direction < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        val label = if (direction < 0) "◀ Cursor" else "Cursor ▶"
        onKeyDispatch(
            code,
            currentMetaState(),
            null,
            formatComboLabel(label),
        )
    }

    fun handleSuggestionSlotTap(slot: SuggestionSlot) {
        if (!enableLiveSuggestions || KeyDispatcher.shouldDisableSuggestions()) return
        val (prior, baseCommit) = typingController.acceptSuggestion(slot)
        val surroundingBefore = if (prior.isEmpty()) KeyDispatcher.getSurroundingChars(1, 0)?.first.orEmpty() else ""
        val finalCommit = if (prior.isEmpty() && surroundingBefore.lastOrNull()?.isLetterOrDigit() == true) {
            " $baseCommit"
        } else {
            baseCommit
        }
        val replaced = KeyDispatcher.dispatchSuggestionReplace(
            context = context,
            expectedTail = prior,
            replacement = finalCommit,
            comboLabel = "Suggestion: ${slot.text}",
            hapticEnabled = settings.isHapticActiveForCurrentMode(),
        )
        if (!replaced && prior.isEmpty()) {
            finalCommit.forEach { ch ->
                val kc = if (ch == ' ') KeyEvent.KEYCODE_SPACE else KeyEvent.KEYCODE_UNKNOWN
                onKeyDispatch(kc, 0, ch, slot.text)
            }
        }
        autoShiftEligible = false
        autoShiftDisarmedForCursor = false
        refreshSuggestionEval()
        clearStickyModifiersIfNeeded()
    }

    val isTightDeck = settings.keyHeightDp <= 32
    val borderVisible = settings.isKeyBorderVisible()
    val borderCol = if (borderVisible) palette.keyBorder.copy(alpha = 0.85f) else null
    val isPcRepeatMode = settings.layoutMode == KeyboardLayoutMode.PC_STANDARD ||
        settings.layoutMode == KeyboardLayoutMode.PC_WITH_NUMPAD ||
        settings.layoutMode == KeyboardLayoutMode.NUMPAD_ONLY
    val holdToRepeatForDeck = settings.holdToRepeat && isPcRepeatMode

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { rootCoords = it },
    ) {
        KeyboardWallpaperBackdrop(
            wallpaper = activeWallpaper,
            palette = palette,
            overlayAlpha = if (isOverlay) settings.overlayOpacity.coerceIn(0.25f, 1.0f) else 1f,
            modifier = Modifier.matchParentSize(),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = deckPadding, vertical = deckPadding),
            verticalArrangement = Arrangement.spacedBy(vGap),
        ) {
        // Uncluttered Top Bar: Drag Handle + Essential Mode Pills + Resize + Quick Settings Gear (+ Minimize/Close in overlay)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isOverlay && onOverlayDrag != null) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onOverlayDrag(dragAmount.x, dragAmount.y)
                            }
                        }
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 2.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (isOverlay) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Drag floating keyboard",
                        tint = palette.secondaryText,
                        modifier = Modifier.size(16.dp),
                    )
                }
                if (settings.showModeStrip) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        KeyboardLayoutMode.entries.forEach { mode ->
                            DeckModeChip(
                                label = mode.shortLabel,
                                selected = settings.layoutMode == mode,
                                palette = palette,
                                borderColor = borderCol,
                                compact = isTightDeck,
                                testTag = "deck_mode_${mode.name.lowercase()}",
                                onClick = { onChangeLayoutMode(mode) },
                            )
                        }
                    }
                } else {
                    Text(
                        text = routeLabel,
                        fontFamily = BodyFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.secondaryText,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (settings.simpleClipboardStripEnabled) {
                    TopActionIconChip(
                        icon = Icons.Default.ContentPaste,
                        contentDescription = "Quick clipboard history strip",
                        active = showClipboardStrip,
                        palette = palette,
                        borderColor = borderCol,
                        testTag = "deck_top_clipboard_toggle",
                        onClick = {
                            showClipboardStrip = !showClipboardStrip
                            if (showClipboardStrip) {
                                ClipboardHistoryStore.syncFromSystemClipboard(context)
                                showShortcutsStrip = false
                                showResizeStrip = false
                                showQuickSettingsPopup = false
                            }
                        },
                    )
                }
                if (hotkeys.isNotEmpty()) {
                    // Custom App-Launch & Macro Shortcuts Drawer Toggle
                    TopActionIconChip(
                        icon = Icons.Default.Bolt,
                        contentDescription = "Custom shortcuts & app launchers",
                        active = showShortcutsStrip,
                        palette = palette,
                        borderColor = borderCol,
                        testTag = "deck_top_shortcuts_toggle",
                        onClick = {
                            showShortcutsStrip = !showShortcutsStrip
                            if (showShortcutsStrip) {
                                showClipboardStrip = false
                                showResizeStrip = false
                                showQuickSettingsPopup = false
                            }
                        },
                    )
                }
                // Resize Toggle in Top Area
                TopActionIconChip(
                    icon = Icons.Default.OpenInFull,
                    contentDescription = "Resize keyboard",
                    active = showResizeStrip,
                    palette = palette,
                    borderColor = borderCol,
                    testTag = "deck_top_resize_toggle",
                    onClick = {
                        showResizeStrip = !showResizeStrip
                        if (showResizeStrip) {
                            showClipboardStrip = false
                            showQuickSettingsPopup = false
                            showShortcutsStrip = false
                        }
                    },
                )
                // Quick Settings Popup Toggle
                TopActionIconChip(
                    icon = Icons.Default.Settings,
                    contentDescription = "Quick keyboard settings",
                    active = showQuickSettingsPopup,
                    palette = palette,
                    borderColor = borderCol,
                    testTag = "deck_top_settings_toggle",
                    onClick = {
                        showQuickSettingsPopup = !showQuickSettingsPopup
                        if (showQuickSettingsPopup) {
                            showClipboardStrip = false
                            showResizeStrip = false
                            showShortcutsStrip = false
                        }
                    },
                )
                if (onMinimize != null) {
                    TopActionIconChip(
                        icon = Icons.Default.KeyboardHide,
                        contentDescription = "Minimize keyboard",
                        active = false,
                        palette = palette,
                        borderColor = borderCol,
                        testTag = "ime_minimize_button",
                        onClick = onMinimize,
                    )
                }
                if (isOverlay && onCloseOverlay != null) {
                    TopActionIconChip(
                        icon = Icons.Default.Close,
                        contentDescription = "Close floating keyboard",
                        active = true,
                        palette = palette,
                        borderColor = borderCol,
                        testTag = "overlay_close_button",
                        onClick = { showCloseConfirm = true },
                    )
                }
            }
        }

        // Expandable Quick Clipboard History Strip
        AnimatedVisibility(visible = showClipboardStrip && settings.simpleClipboardStripEnabled) {
            KeyboardClipboardHistoryStrip(
                clips = clipboardClips,
                palette = palette,
                borderColor = borderCol,
                onPasteClip = { entry ->
                    if (KeyDispatcher.dispatchTextCommit(
                            context = context,
                            text = entry.text,
                            comboLabel = "Clip: ${entry.preview.take(18)}",
                            hapticEnabled = settings.isHapticActiveForCurrentMode(),
                        )
                    ) {
                        if (settings.layoutMode == KeyboardLayoutMode.SIMPLE) {
                            typingController.resetOnCursorOrBoundary(clearLastWord = true)
                            refreshSuggestionEval()
                            autoShiftDisarmedForCursor = false
                            if (settings.simpleAutoCapitalize) {
                                autoShiftEligible = SimpleSmartTyping.shouldAutoCapitalize(entry.text)
                            }
                        }
                        clearStickyModifiersIfNeeded()
                    }
                },
                onClearAll = { ClipboardHistoryStore.clearAll() },
            )
        }

        // Expandable Custom App & Hotkey Strip
        AnimatedVisibility(visible = showShortcutsStrip && hotkeys.isNotEmpty()) {
            val orderedHotkeys = remember(hotkeys) {
                hotkeys.sortedByDescending {
                    when (it.actionType) {
                        HotkeyActionType.TOGGLE_FLOATING -> 2
                        HotkeyActionType.LAUNCH_APP -> 1
                        HotkeyActionType.KEY_COMBO -> 0
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 2.dp)
                    .testTag("deck_shortcuts_strip"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                orderedHotkeys.forEach { hk ->
                    val isAppLaunch = hk.actionType == HotkeyActionType.LAUNCH_APP && hk.targetPackageName.isNotBlank()
                    val isFloatDeck = hk.actionType == HotkeyActionType.TOGGLE_FLOATING
                    val isSpecial = isAppLaunch || isFloatDeck
                    val accent = Color(hk.accentHex)
                    Row(
                        modifier = Modifier
                            .neumorphicKeyCap(
                                surfaceColor = if (isSpecial) palette.activeFill else palette.keyBg,
                                shadowLight = palette.shadowLight,
                                shadowDark = palette.shadowDark,
                                cornerRadius = 8.dp,
                                borderColor = borderCol,
                            )
                            .clickable {
                                onHotkeyDispatch?.invoke(hk) ?: onKeyDispatch(
                                    hk.keyCode,
                                    hk.metaState(),
                                    null,
                                    hk.comboText(),
                                )
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("deck_shortcut_chip_${hk.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (isAppLaunch || isFloatDeck) {
                            Icon(
                                imageVector = if (isFloatDeck) Icons.Default.Layers else Icons.Default.Apps,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                        Text(
                            text = hk.label,
                            fontFamily = BodyFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSpecial) accent else palette.primaryText,
                            maxLines = 1,
                        )
                        Text(
                            text = hk.comboText(),
                            fontFamily = BodyFontFamily,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.secondaryText,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        // Expandable Top Resize Controls
        AnimatedVisibility(visible = showResizeStrip) {
            DeckQuickResizeStrip(
                settings = settings,
                palette = palette,
                isOverlay = isOverlay,
                onUpdateSettings = onUpdateSettings,
                onResizeDragDelta = onResizeDragDelta,
            )
        }

        // Expandable Quick Feature Settings Popup
        AnimatedVisibility(visible = showQuickSettingsPopup) {
            DeckQuickSettingsPopup(
                settings = settings,
                palette = palette,
                isOverlay = isOverlay,
                onUpdateSettings = onUpdateSettings,
                onDismiss = { showQuickSettingsPopup = false },
            )
        }

        // Readiness notice shown ONLY inside the floating overlay when neither KeyDeck IME nor Accessibility is active
        if (isOverlay && routeLabel == "Setup") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphicInset(
                        surfaceColor = palette.activeFill.copy(alpha = 0.85f),
                        shadowLight = palette.shadowLight,
                        shadowDark = palette.shadowDark,
                        cornerRadius = 8.dp,
                        depth = 2.dp,
                        borderColor = borderCol,
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("overlay_setup_banner"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Select KeyDeck IME to type anywhere (A11y optional)",
                    fontFamily = BodyFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (onSetupIme != null) {
                        DeckModeChip(
                            label = "Select IME",
                            selected = true,
                            palette = palette,
                            borderColor = borderCol,
                            compact = true,
                            testTag = "overlay_setup_ime",
                            onClick = onSetupIme,
                        )
                    }
                    if (onSetupAccessibility != null) {
                        DeckModeChip(
                            label = "A11y (Opt)",
                            selected = false,
                            palette = palette,
                            borderColor = borderCol,
                            compact = true,
                            testTag = "overlay_setup_a11y",
                            onClick = onSetupAccessibility,
                        )
                    }
                }
            }
        }

        // Transient focus/setup hint when tapping an uneditable external target or unconfigured route in floating overlay
        AnimatedVisibility(visible = isOverlay && overlayFeedbackText != null) {
            overlayFeedbackText?.let { feedback ->
                Text(
                    text = feedback,
                    fontFamily = BodyFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.accentBg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .testTag("overlay_feedback_hint"),
                )
            }
        }

        // Optional Top Function Row (Esc, F1..F12) for PC modes
        if ((settings.showFunctionRow || fnActive) &&
            settings.layoutMode != KeyboardLayoutMode.GAMING &&
            settings.layoutMode != KeyboardLayoutMode.NUMPAD_ONLY &&
            settings.layoutMode != KeyboardLayoutMode.SIMPLE
        ) {
            val fnHeight = (keyHeight * 0.78f).coerceAtLeast(24.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(hGap),
            ) {
                PcKeyboardLayoutData.functionRow.forEach { spec ->
                    PcKeyCap(
                        spec = spec,
                        palette = palette,
                        keyHeight = fnHeight,
                        keyCorner = (keyCorner - 2.dp).coerceAtLeast(4.dp),
                        fontScale = fontScale * 0.85f,
                        upperCaseLetters = false,
                        shiftActive = false,
                        fnActive = false,
                        isModifierLit = spec.id == "num_toggle_top" && settings.layoutMode != KeyboardLayoutMode.PC_STANDARD,
                        compact = true,
                        hGap = hGap,
                        vGap = vGap,
                        showKeyBorder = borderVisible,
                        keyOpacity = keyOpacity,
                        rgbActive = rgbActive,
                        rgbPhaseDeg = rgbPhaseDeg,
                        holdToRepeatEnabled = holdToRepeatForDeck,
                        modifier = Modifier.weight(spec.weight),
                        onRepeat = { handleKeyTap(spec, isRepeat = true) },
                        onClick = { handleKeyTap(spec) },
                    )
                }
            }
        }

        // Main Deck Body
        when (settings.layoutMode) {
            KeyboardLayoutMode.PC_WITH_NUMPAD -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(hGap + 3.dp),
                ) {
                    KeyGridBlock(
                        rows = PcKeyboardLayoutData.splitPcRows,
                        palette = palette,
                        keyHeight = keyHeight,
                        hGap = hGap,
                        vGap = vGap,
                        keyCorner = (keyCorner - 1.dp).coerceAtLeast(5.dp),
                        fontScale = fontScale * 0.92f,
                        upperCaseLetters = effectiveUpperCase,
                        ctrlActive = ctrlActive,
                        altActive = altActive,
                        shiftActive = shiftActive,
                        capsActive = capsActive,
                        metaActive = metaActive,
                        fnActive = fnActive,
                        compact = true,
                        showKeyBorder = borderVisible,
                        keyOpacity = keyOpacity,
                        rgbActive = rgbActive,
                        rgbPhaseDeg = rgbPhaseDeg,
                        holdToRepeatEnabled = holdToRepeatForDeck,
                        onSpaceCursorSlide = { dir -> handleSpaceCursorSlide(dir) },
                        modifier = Modifier.weight(0.68f),
                        onKeyRepeat = { handleKeyTap(it, isFromNumpad = false, isRepeat = true) },
                        onKeyTap = { handleKeyTap(it, isFromNumpad = false) },
                    )
                    NumpadBlock(
                        palette = palette,
                        keyHeight = keyHeight,
                        hGap = hGap,
                        vGap = vGap,
                        keyCorner = (keyCorner - 1.dp).coerceAtLeast(5.dp),
                        fontScale = fontScale * 0.94f,
                        numLockOn = settings.numLockOn,
                        compact = true,
                        showKeyBorder = borderVisible,
                        keyOpacity = keyOpacity,
                        rgbActive = rgbActive,
                        rgbPhaseDeg = rgbPhaseDeg,
                        holdToRepeatEnabled = holdToRepeatForDeck,
                        modifier = Modifier.weight(0.32f),
                        onKeyRepeat = { handleKeyTap(it, isFromNumpad = true, isRepeat = true) },
                        onKeyTap = { handleKeyTap(it, isFromNumpad = true) },
                    )
                }
            }

            KeyboardLayoutMode.PC_STANDARD -> {
                KeyGridBlock(
                    rows = PcKeyboardLayoutData.mainRows,
                    palette = palette,
                    keyHeight = keyHeight,
                    hGap = hGap,
                    vGap = vGap,
                    keyCorner = keyCorner,
                    fontScale = fontScale,
                    upperCaseLetters = effectiveUpperCase,
                    ctrlActive = ctrlActive,
                    altActive = altActive,
                    shiftActive = shiftActive,
                    capsActive = capsActive,
                    metaActive = metaActive,
                    fnActive = fnActive,
                    compact = isTightDeck,
                    showKeyBorder = borderVisible,
                    keyOpacity = keyOpacity,
                    rgbActive = rgbActive,
                    rgbPhaseDeg = rgbPhaseDeg,
                    holdToRepeatEnabled = holdToRepeatForDeck,
                    onSpaceCursorSlide = { dir -> handleSpaceCursorSlide(dir) },
                    onKeyRepeat = { handleKeyTap(it, isFromNumpad = false, isRepeat = true) },
                    onKeyTap = { handleKeyTap(it, isFromNumpad = false) },
                )
            }

            KeyboardLayoutMode.SIMPLE -> {
                val simpleKeyHeight = (keyHeight + 5.dp).coerceIn(36.dp, 52.dp)
                val simpleRows = when {
                    simpleSymbolLayer && simpleSymbolAltLayer -> PcKeyboardLayoutData.simpleSymbolAltRows
                    simpleSymbolLayer -> PcKeyboardLayoutData.simpleSymbolRows
                    else -> PcKeyboardLayoutData.simpleAlphaRows
                }
                val simpleHGap = hGap.coerceIn(3.dp, 5.dp)
                val simpleVGap = (vGap + 2.dp).coerceIn(5.dp, 8.dp)
                val inputSuppressed = remember(inputSessionGen, lastDispatched?.timestampMs, enableLiveSuggestions) {
                    enableLiveSuggestions && KeyDispatcher.shouldDisableSuggestions()
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(simpleVGap),
                ) {
                    if (settings.simpleSuggestionsEnabled) {
                        SimpleSuggestionBar(
                            slots = suggestionEval.slots,
                            suppressed = inputSuppressed,
                            palette = palette,
                            barHeight = (keyHeight * 0.78f).coerceIn(28.dp, 36.dp),
                            hGap = simpleHGap,
                            cornerRadius = keyCorner.coerceIn(7.dp, 11.dp),
                            fontScale = fontScale,
                            showKeyBorder = borderVisible,
                            keyOpacity = keyOpacity,
                            rgbActive = rgbActive,
                            rgbPhaseDeg = rgbPhaseDeg,
                            onSelectSlot = { slot -> handleSuggestionSlotTap(slot) },
                        )
                    }
                    KeyGridBlock(
                        rows = simpleRows,
                        palette = palette,
                        keyHeight = simpleKeyHeight,
                        hGap = simpleHGap,
                        vGap = simpleVGap,
                        keyCorner = keyCorner.coerceIn(8.dp, 12.dp),
                        fontScale = (fontScale * 1.08f).coerceAtMost(1.24f),
                        upperCaseLetters = effectiveUpperCase,
                        ctrlActive = ctrlActive,
                        altActive = altActive,
                        shiftActive = if (simpleSymbolLayer) shiftActive else (shiftActive || autoShiftActive),
                        capsActive = capsActive,
                        metaActive = metaActive,
                        fnActive = fnActive,
                        compact = false,
                        centerShortRows = !simpleSymbolLayer,
                        showKeyBorder = borderVisible,
                        keyOpacity = keyOpacity,
                        rgbActive = rgbActive,
                        rgbPhaseDeg = rgbPhaseDeg,
                        holdToRepeatEnabled = false,
                        rootCoordsProvider = { rootCoords },
                        onKeyPreviewChanged = { preview -> activeKeyPreview = preview },
                        onPopupChanged = { popup ->
                            if (popup != null && activeHoldPopup == null && settings.isHapticActiveForCurrentMode()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            if (popup != null) {
                                activeKeyPreview = null
                            }
                            activeHoldPopup = popup
                        },
                        onPopupCommit = { chosen ->
                            activeHoldPopup = null
                            activeKeyPreview = null
                            handleHoldCharCommit(chosen)
                        },
                        onSpaceCursorSlide = { dir -> handleSpaceCursorSlide(dir) },
                        modifier = Modifier.testTag("simple_mode_deck"),
                        onKeyRepeat = { handleKeyTap(it, isFromNumpad = false, isRepeat = true) },
                        onKeyTap = { handleKeyTap(it, isFromNumpad = false) },
                    )
                }
            }

            KeyboardLayoutMode.NUMPAD_ONLY -> {
                NumpadBlock(
                    palette = palette,
                    keyHeight = (keyHeight + 5.dp),
                    hGap = hGap + 1.dp,
                    vGap = vGap + 1.dp,
                    keyCorner = keyCorner,
                    fontScale = fontScale * 1.08f,
                    numLockOn = settings.numLockOn,
                    compact = false,
                    showKeyBorder = borderVisible,
                    keyOpacity = keyOpacity,
                    rgbActive = rgbActive,
                    rgbPhaseDeg = rgbPhaseDeg,
                    holdToRepeatEnabled = holdToRepeatForDeck,
                    modifier = Modifier.fillMaxWidth(),
                    onKeyRepeat = { handleKeyTap(it, isFromNumpad = true, isRepeat = true) },
                    onKeyTap = { handleKeyTap(it, isFromNumpad = true) },
                )
            }

            KeyboardLayoutMode.GAMING -> {
                val gameKeyHeight = (keyHeight + 2.dp).coerceAtLeast(36.dp)
                val totalPadHeight = gameKeyHeight * 4 + vGap * 3
                val padWeight = settings.touchpadWidthFraction.coerceIn(0.30f, 0.55f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gaming_mode_deck"),
                    horizontalArrangement = Arrangement.spacedBy(hGap + 4.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    GamingKeysBlock(
                        rows = PcKeyboardLayoutData.gamingLeftRows,
                        palette = palette,
                        keyHeight = gameKeyHeight,
                        hGap = hGap,
                        vGap = vGap,
                        keyCorner = keyCorner,
                        fontScale = fontScale,
                        showKeyBorder = borderVisible,
                        keyOpacity = keyOpacity,
                        rgbActive = rgbActive,
                        rgbPhaseDeg = rgbPhaseDeg,
                        modifier = Modifier.weight(1f - padWeight),
                        onKeyHold = { spec, isDown ->
                            if (onKeyHoldDispatch != null) {
                                onKeyHoldDispatch(spec.keyCode, 0, isDown, spec.label)
                            } else if (isDown) {
                                onKeyDispatch(spec.keyCode, 0, spec.charPrimary, spec.label)
                            }
                        },
                    )
                    GamingMouseTouchpad(
                        palette = palette,
                        settings = settings,
                        rgbPhaseDeg = rgbPhaseDeg,
                        rgbEnabled = rgbActive,
                        keyOpacity = keyOpacity,
                        totalHeight = totalPadHeight,
                        cornerRadius = keyCorner,
                        modifier = Modifier.weight(padWeight),
                        onMouseMove = { dx, dy -> onMouseMove?.invoke(dx, dy) },
                        onMouseClick = { isRight -> onMouseClick?.invoke(isRight) },
                        onMouseEnd = { onMouseEnd?.invoke() },
                        onToggleLookMode = {
                            onUpdateSettings { cur ->
                                cur.copy(gamingMcpeLookMode = !cur.gamingMcpeLookMode)
                            }
                        },
                    )
                }
            }
        }
        }

        // Instant Key-Press Preview Bubble for Simple Mode (Gboard-style tactile feedback)
        val keyPreview = activeKeyPreview
        if (keyPreview != null && activeHoldPopup == null) {
            val bubbleW = 42.dp
            val bubbleH = 48.dp
            val bubbleWPx = with(density) { bubbleW.toPx() }
            val bubbleHPx = with(density) { bubbleH.toPx() }
            val rootWidthPx = (rootCoords?.size?.width ?: 1080).toFloat()

            val targetX = (keyPreview.keyRectInRoot.center.x - bubbleWPx / 2f)
                .coerceIn(6f, (rootWidthPx - bubbleWPx - 6f).coerceAtLeast(6f))
            val targetY = (keyPreview.keyRectInRoot.top - bubbleHPx - 6f).coerceAtLeast(4f)

            Box(
                modifier = Modifier
                    .zIndex(45f)
                    .offset { IntOffset(targetX.roundToInt(), targetY.roundToInt()) }
                    .size(width = bubbleW, height = bubbleH)
                    .neumorphicRaised(
                        surfaceColor = palette.keyBg,
                        shadowLight = palette.shadowLight,
                        shadowDark = palette.shadowDark,
                        cornerRadius = 12.dp,
                        shadowOffset = 4.dp,
                        blurRadius = 8.dp,
                        borderColor = borderCol ?: palette.keyBorder.copy(alpha = 0.65f),
                    )
                    .testTag("simple_key_preview_bubble"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = keyPreview.label,
                    color = palette.primaryText,
                    fontFamily = BodyFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }

        // In-Compose Floating Hold-to-Write Accent & Symbol Picker Overlay
        val holdPopup = activeHoldPopup
        if (holdPopup != null) {
            val cellWidthDp = 38.dp
            val cellHeightDp = 40.dp
            val cols = if (holdPopup.options.size <= 5) holdPopup.options.size else 4
            val rowsCount = ceil(holdPopup.options.size / cols.toFloat()).roundToInt().coerceAtLeast(1)
            val popupW = cellWidthDp * cols + 10.dp
            val popupH = cellHeightDp * rowsCount + 10.dp

            val popupWPx = with(density) { popupW.toPx() }
            val popupHPx = with(density) { popupH.toPx() }
            val rootWidthPx = (rootCoords?.size?.width ?: 1080).toFloat()

            val targetX = (holdPopup.keyRectInRoot.center.x - popupWPx / 2f)
                .coerceIn(6f, (rootWidthPx - popupWPx - 6f).coerceAtLeast(6f))
            val targetY = (holdPopup.keyRectInRoot.top - popupHPx - 10f).coerceAtLeast(4f)

            Box(
                modifier = Modifier
                    .zIndex(50f)
                    .offset { IntOffset(targetX.roundToInt(), targetY.roundToInt()) }
                    .width(popupW)
                    .neumorphicRaised(
                        surfaceColor = palette.deckBg,
                        shadowLight = palette.shadowLight,
                        shadowDark = palette.shadowDark,
                        cornerRadius = 12.dp,
                        shadowOffset = 5.dp,
                        blurRadius = 10.dp,
                        borderColor = borderCol ?: palette.keyBorder.copy(alpha = 0.75f),
                    )
                    .padding(5.dp)
                    .testTag("hold_char_popup"),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (r in 0 until rowsCount) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            for (c in 0 until cols) {
                                val idx = r * cols + c
                                if (idx < holdPopup.options.size) {
                                    val ch = holdPopup.options[idx]
                                    val isSel = idx == holdPopup.selectedIndex
                                    Box(
                                        modifier = Modifier
                                            .size(width = cellWidthDp - 2.dp, height = cellHeightDp - 2.dp)
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                            .background(if (isSel) palette.accentBg else Color.Transparent),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = ch,
                                            color = if (isSel) Color.White else palette.primaryText,
                                            fontFamily = BodyFontFamily,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(width = cellWidthDp - 2.dp, height = cellHeightDp - 2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // In-Compose Close Confirmation Warning Overlay (prevents accidental service shutdown)
        if (showCloseConfirm && isOverlay && onCloseOverlay != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(60f)
                    .background(Color.Black.copy(alpha = 0.56f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { showCloseConfirm = false }
                    .padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .neumorphicRaised(
                            surfaceColor = palette.deckBg,
                            shadowLight = palette.shadowLight,
                            shadowDark = palette.shadowDark,
                            cornerRadius = 16.dp,
                            shadowOffset = 6.dp,
                            blurRadius = 12.dp,
                            borderColor = borderCol ?: palette.keyBorder.copy(alpha = 0.75f),
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("overlay_close_confirm_dialog"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Close Floating Keyboard?",
                            fontFamily = BodyFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText,
                        )
                    }
                    val floatComboText = remember(hotkeys) {
                        hotkeys.firstOrNull { it.actionType == HotkeyActionType.TOGGLE_FLOATING }?.comboText()
                            ?: "Ctrl+Alt+F"
                    }
                    Text(
                        text = "This turns off the floating icon completely until you open KeyDeck or press $floatComboText on KeyDeck IME.",
                        fontFamily = BodyFontFamily,
                        fontSize = 11.5.sp,
                        color = palette.secondaryText,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .neumorphicRaised(
                                    surfaceColor = palette.keyBg,
                                    shadowLight = palette.shadowLight,
                                    shadowDark = palette.shadowDark,
                                    cornerRadius = 10.dp,
                                    shadowOffset = 2.dp,
                                    blurRadius = 4.dp,
                                    borderColor = borderCol,
                                )
                                .clickable { showCloseConfirm = false }
                                .testTag("overlay_close_cancel"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Keep Open",
                                fontFamily = BodyFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.primaryText,
                                maxLines = 1,
                            )
                        }
                        if (onMinimize != null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .neumorphicInset(
                                        surfaceColor = palette.activeFill,
                                        shadowLight = palette.shadowLight,
                                        shadowDark = palette.shadowDark,
                                        cornerRadius = 10.dp,
                                        depth = 2.dp,
                                        borderColor = borderCol,
                                    )
                                    .clickable {
                                        showCloseConfirm = false
                                        onMinimize()
                                    }
                                    .testTag("overlay_close_minimize"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Minimize",
                                    fontFamily = BodyFontFamily,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.accentBg,
                                    maxLines = 1,
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                                .background(Color(0xFFE11D48))
                                .clickable {
                                    showCloseConfirm = false
                                    onCloseOverlay()
                                }
                                .testTag("overlay_close_confirm"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Close",
                                fontFamily = BodyFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyGridBlock(
    rows: List<List<KeySpec>>,
    palette: ResolvedKeyboardPalette,
    keyHeight: Dp,
    hGap: Dp,
    vGap: Dp,
    keyCorner: Dp,
    fontScale: Float,
    upperCaseLetters: Boolean,
    ctrlActive: Boolean,
    altActive: Boolean,
    shiftActive: Boolean,
    capsActive: Boolean,
    metaActive: Boolean,
    fnActive: Boolean,
    compact: Boolean,
    centerShortRows: Boolean = false,
    showKeyBorder: Boolean,
    keyOpacity: Float = 1f,
    rgbActive: Boolean,
    rgbPhaseDeg: Float,
    holdToRepeatEnabled: Boolean = false,
    rootCoordsProvider: () -> LayoutCoordinates? = { null },
    onKeyPreviewChanged: ((ActiveKeyPreview?) -> Unit)? = null,
    onPopupChanged: ((ActiveHoldPopup?) -> Unit)? = null,
    onPopupCommit: ((String) -> Unit)? = null,
    onSpaceCursorSlide: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    onKeyRepeat: ((KeySpec) -> Unit)? = null,
    onKeyTap: (KeySpec) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(vGap),
    ) {
        rows.forEach { row ->
            val totalWeight = row.sumOf { it.weight.toDouble() }.toFloat()
            val needsSidePad = centerShortRows && totalWeight in 8.5f..9.5f
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(hGap),
            ) {
                if (needsSidePad) {
                    Spacer(modifier = Modifier.weight(0.5f))
                }
                row.forEach { spec ->
                    val isLit = when (spec.modifierType) {
                        ModifierKey.CTRL -> ctrlActive
                        ModifierKey.ALT -> altActive
                        ModifierKey.SHIFT -> shiftActive
                        ModifierKey.CAPS -> capsActive
                        ModifierKey.META -> metaActive
                        ModifierKey.FN -> fnActive
                        null -> false
                    }
                    PcKeyCap(
                        spec = spec,
                        palette = palette,
                        keyHeight = keyHeight,
                        keyCorner = keyCorner,
                        fontScale = fontScale,
                        upperCaseLetters = upperCaseLetters,
                        shiftActive = shiftActive,
                        fnActive = fnActive,
                        isModifierLit = isLit,
                        compact = compact,
                        hGap = hGap,
                        vGap = vGap,
                        showKeyBorder = showKeyBorder,
                        keyOpacity = keyOpacity,
                        rgbActive = rgbActive,
                        rgbPhaseDeg = rgbPhaseDeg,
                        holdToRepeatEnabled = holdToRepeatEnabled,
                        rootCoordsProvider = rootCoordsProvider,
                        onKeyPreviewChanged = onKeyPreviewChanged,
                        onPopupChanged = onPopupChanged,
                        onPopupCommit = onPopupCommit,
                        onSpaceCursorSlide = onSpaceCursorSlide,
                        modifier = Modifier.weight(spec.weight),
                        onRepeat = onKeyRepeat?.let { repeatCb -> { repeatCb(spec) } },
                        onClick = { onKeyTap(spec) },
                    )
                }
                if (needsSidePad) {
                    Spacer(modifier = Modifier.weight(0.5f))
                }
            }
        }
    }
}

@Composable
private fun GamingKeysBlock(
    rows: List<List<KeySpec>>,
    palette: ResolvedKeyboardPalette,
    keyHeight: Dp,
    hGap: Dp,
    vGap: Dp,
    keyCorner: Dp,
    fontScale: Float,
    showKeyBorder: Boolean,
    keyOpacity: Float = 1f,
    rgbActive: Boolean,
    rgbPhaseDeg: Float,
    modifier: Modifier = Modifier,
    onKeyHold: (KeySpec, Boolean) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(vGap),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(hGap),
            ) {
                row.forEach { spec ->
                    GamingHoldKeyCap(
                        spec = spec,
                        palette = palette,
                        keyHeight = keyHeight,
                        keyCorner = keyCorner,
                        fontScale = fontScale,
                        hGap = hGap,
                        vGap = vGap,
                        showKeyBorder = showKeyBorder,
                        keyOpacity = keyOpacity,
                        rgbActive = rgbActive,
                        rgbPhaseDeg = rgbPhaseDeg,
                        modifier = Modifier.weight(spec.weight),
                        onHoldChanged = { isDown -> onKeyHold(spec, isDown) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GamingHoldKeyCap(
    spec: KeySpec,
    palette: ResolvedKeyboardPalette,
    keyHeight: Dp,
    keyCorner: Dp,
    fontScale: Float,
    hGap: Dp = 3.dp,
    vGap: Dp = 4.dp,
    showKeyBorder: Boolean,
    keyOpacity: Float = 1f,
    rgbActive: Boolean,
    rgbPhaseDeg: Float,
    modifier: Modifier = Modifier,
    onHoldChanged: (Boolean) -> Unit,
) {
    var held by remember { mutableStateOf(false) }
    val pressOffsetY by animateDpAsState(
        targetValue = if (held) 1.5.dp else 0.dp,
        animationSpec = spring(stiffness = 950f),
        label = "gaming_key_offset",
    )

    val borderCol = if (showKeyBorder && !rgbActive) palette.keyBorder.copy(alpha = 0.90f) else null
    val baseKeyBg = when {
        held -> palette.activeFill
        spec.isAccent -> palette.activeFill.copy(alpha = 0.75f)
        spec.isModifier -> palette.modifierBg
        else -> palette.keyBg
    }
    val keyBg = if (keyOpacity < 0.99f) {
        baseKeyBg.copy(alpha = (baseKeyBg.alpha * if (held) (keyOpacity + 0.18f).coerceAtMost(0.95f) else keyOpacity).coerceIn(0.14f, 1f))
    } else {
        baseKeyBg
    }
    val textColor = when {
        held || spec.isAccent -> palette.accentBg
        else -> palette.primaryText
    }
    val spreadX = (hGap * 0.45f).coerceIn(0.5.dp, 2.2.dp)
    val spreadY = (vGap * 0.45f).coerceIn(0.5.dp, 2.6.dp)

    Box(
        modifier = modifier
            .height(keyHeight)
            .offset(y = pressOffsetY)
            .then(
                if (held) {
                    Modifier.neumorphicInset(
                        surfaceColor = keyBg,
                        shadowLight = palette.shadowLight,
                        shadowDark = palette.shadowDark,
                        cornerRadius = keyCorner,
                        depth = 3.dp,
                        borderColor = borderCol,
                    )
                } else {
                    Modifier.neumorphicKeyCap(
                        surfaceColor = keyBg,
                        shadowLight = palette.shadowLight,
                        shadowDark = palette.shadowDark,
                        cornerRadius = keyCorner,
                        shadowOffset = 1.6.dp,
                        blurRadius = 2.5.dp,
                        maxSpreadX = spreadX,
                        maxSpreadY = spreadY,
                        borderColor = borderCol,
                    )
                },
            )
            .then(
                if (rgbActive) {
                    Modifier.rgbAnimatedBorder(
                        phaseDegrees = rgbPhaseDeg,
                        cornerRadius = keyCorner,
                        strokeWidth = if (held || spec.isAccent) 1.8.dp else 1.3.dp,
                        alpha = if (held) 0.98f else 0.80f,
                    )
                } else {
                    Modifier
                },
            )
            .pointerInput(spec.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    held = true
                    onHoldChanged(true)
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.all { !it.pressed }) {
                                break
                            }
                        }
                    } finally {
                        held = false
                        onHoldChanged(false)
                    }
                }
            }
            .testTag("key_${spec.id}"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = spec.label,
            color = textColor,
            fontFamily = BodyFontFamily,
            fontSize = (if (spec.label.length > 2) 10f else 13f * fontScale).coerceIn(8f, 16f).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun NumpadBlock(
    palette: ResolvedKeyboardPalette,
    keyHeight: Dp,
    hGap: Dp,
    vGap: Dp,
    keyCorner: Dp,
    fontScale: Float,
    numLockOn: Boolean,
    compact: Boolean,
    showKeyBorder: Boolean,
    keyOpacity: Float = 1f,
    rgbActive: Boolean,
    rgbPhaseDeg: Float,
    holdToRepeatEnabled: Boolean = false,
    modifier: Modifier = Modifier,
    onKeyRepeat: ((KeySpec) -> Unit)? = null,
    onKeyTap: (KeySpec) -> Unit,
) {
    val borderCol = if (showKeyBorder && !rgbActive) palette.keyBorder.copy(alpha = 0.78f) else null
    Column(
        modifier = modifier
            .neumorphicInset(
                surfaceColor = if (keyOpacity < 0.99f) palette.deckBg.copy(alpha = (keyOpacity * 0.45f).coerceIn(0.12f, 0.7f)) else palette.deckBg,
                shadowLight = palette.shadowLight,
                shadowDark = palette.shadowDark,
                cornerRadius = keyCorner + 2.dp,
                depth = 2.5.dp,
                borderColor = borderCol,
            )
            .padding(2.5.dp),
        verticalArrangement = Arrangement.spacedBy(vGap),
    ) {
        PcKeyboardLayoutData.numpadRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(hGap),
            ) {
                row.forEach { spec ->
                    val isNumLockKey = spec.id == "num_lock"
                    val showNav = !numLockOn && spec.fnLabel != null
                    val displaySpec = if (showNav) {
                        spec.copy(label = spec.fnLabel.orEmpty(), shiftLabel = spec.label)
                    } else {
                        spec
                    }
                    PcKeyCap(
                        spec = displaySpec,
                        palette = palette,
                        keyHeight = keyHeight,
                        keyCorner = keyCorner,
                        fontScale = fontScale,
                        upperCaseLetters = false,
                        shiftActive = false,
                        fnActive = false,
                        isModifierLit = isNumLockKey && numLockOn,
                        compact = compact,
                        hGap = hGap,
                        vGap = vGap,
                        showKeyBorder = showKeyBorder,
                        keyOpacity = keyOpacity,
                        rgbActive = rgbActive,
                        rgbPhaseDeg = rgbPhaseDeg,
                        holdToRepeatEnabled = holdToRepeatEnabled,
                        modifier = Modifier.weight(spec.weight),
                        onRepeat = onKeyRepeat?.let { repeatCb -> { repeatCb(spec) } },
                        onClick = { onKeyTap(spec) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PcKeyCap(
    spec: KeySpec,
    palette: ResolvedKeyboardPalette,
    keyHeight: Dp,
    keyCorner: Dp,
    fontScale: Float,
    upperCaseLetters: Boolean,
    shiftActive: Boolean,
    fnActive: Boolean,
    isModifierLit: Boolean,
    compact: Boolean,
    hGap: Dp = 3.dp,
    vGap: Dp = 4.dp,
    showKeyBorder: Boolean,
    keyOpacity: Float = 1f,
    rgbActive: Boolean,
    rgbPhaseDeg: Float,
    holdToRepeatEnabled: Boolean = false,
    rootCoordsProvider: () -> LayoutCoordinates? = { null },
    onKeyPreviewChanged: ((ActiveKeyPreview?) -> Unit)? = null,
    onPopupChanged: ((ActiveHoldPopup?) -> Unit)? = null,
    onPopupCommit: ((String) -> Unit)? = null,
    onSpaceCursorSlide: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    onRepeat: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    var gesturePressed by remember { mutableStateOf(false) }
    var isSlidingCursor by remember { mutableStateOf(false) }
    val isPressed = gesturePressed
    val isEngaged = isPressed || isModifierLit

    val pressOffsetY by animateDpAsState(
        targetValue = if (isPressed) 1.5.dp else 0.dp,
        animationSpec = spring(stiffness = 900f),
        label = "key_y_offset",
    )

    val isLetter = spec.charPrimary?.isLetter() == true
    val primaryText = when {
        spec.keyCode == KeyEvent.KEYCODE_SPACE && isSlidingCursor -> "◀  Cursor  ▶"
        fnActive && spec.fnLabel != null -> spec.fnLabel
        isLetter && upperCaseLetters && spec.charShift != null -> spec.charShift.toString()
        !isLetter && shiftActive && spec.charShift != null -> spec.charShift.toString()
        else -> spec.label
    }

    val cornerText = when {
        keyHeight < 29.dp -> null
        fnActive && spec.fnLabel != null -> spec.label
        spec.cornerHint != null -> spec.cornerHint
        !shiftActive && spec.shiftLabel != null && !isLetter -> spec.shiftLabel
        else -> null
    }

    val holdOptions = remember(spec, upperCaseLetters, fnActive) {
        when {
            fnActive || spec.isModifier -> emptyList()
            upperCaseLetters && spec.longPressShiftChars.isNotEmpty() -> spec.longPressShiftChars
            else -> spec.longPressChars
        }
    }
    var keyRectInRoot by remember { mutableStateOf<Rect?>(null) }
    val latestOnClick by rememberUpdatedState(onClick)
    val latestOnRepeat by rememberUpdatedState(onRepeat)
    val latestHoldToRepeat by rememberUpdatedState(holdToRepeatEnabled)
    val latestHoldOptions by rememberUpdatedState(holdOptions)
    val latestCornerHint by rememberUpdatedState(spec.cornerHint)
    val latestPrimaryText by rememberUpdatedState(primaryText)
    val latestOnKeyPreviewChanged by rememberUpdatedState(onKeyPreviewChanged)
    val latestOnPopupChanged by rememberUpdatedState(onPopupChanged)
    val latestOnPopupCommit by rememberUpdatedState(onPopupCommit)
    val latestOnSpaceCursorSlide by rememberUpdatedState(onSpaceCursorSlide)
    val density = LocalDensity.current

    val isRepeatableSpec = remember(spec.id, spec.keyCode, spec.isModifier) {
        spec.keyCode == KeyEvent.KEYCODE_DEL || (
            !spec.isModifier &&
                spec.id != "num_lock" &&
                spec.id != "num_toggle" &&
                spec.id != "num_toggle_top" &&
                spec.id != "simple_sym_toggle" &&
                spec.id != "simple_sym_alt_toggle"
            )
    }
    val canShowKeyPreview = onKeyPreviewChanged != null &&
        !spec.isModifier &&
        spec.iconType == null &&
        spec.keyCode != KeyEvent.KEYCODE_SPACE &&
        spec.keyCode != KeyEvent.KEYCODE_DEL &&
        spec.keyCode != KeyEvent.KEYCODE_ENTER &&
        spec.id != "simple_sym_toggle" &&
        spec.id != "simple_sym_alt_toggle"

    val baseKeyBg = when {
        isEngaged -> palette.activeFill
        spec.isAccent -> palette.accentBg
        spec.isModifier -> palette.modifierBg
        else -> palette.keyBg
    }
    val keyBg = if (keyOpacity < 0.99f && !spec.isAccent) {
        val targetAlpha = if (isEngaged) (keyOpacity + 0.20f).coerceAtMost(0.95f) else keyOpacity
        baseKeyBg.copy(alpha = (baseKeyBg.alpha * targetAlpha).coerceIn(0.14f, 1f))
    } else {
        baseKeyBg
    }

    val textColor = when {
        spec.isAccent && !isEngaged -> Color.White
        isEngaged -> palette.activeIndicator
        else -> palette.primaryText
    }

    val borderCol = if (showKeyBorder && !rgbActive) palette.keyBorder.copy(alpha = 0.90f) else null
    val shadowDepth = if (compact) 1.3.dp else 1.7.dp
    val blurRadius = if (compact) 2.0.dp else 2.6.dp
    val spreadX = (hGap * 0.45f).coerceIn(0.5.dp, 2.2.dp)
    val spreadY = (vGap * 0.45f).coerceIn(0.5.dp, 2.6.dp)
    val iconSize = (keyHeight * 0.38f).coerceIn(11.dp, 17.dp)

    val neumorphicModifier = if (isEngaged) {
        Modifier.neumorphicInset(
            surfaceColor = keyBg,
            shadowLight = palette.shadowLight,
            shadowDark = palette.shadowDark,
            cornerRadius = keyCorner,
            depth = 2.5.dp,
            borderColor = borderCol,
            borderWidth = 1.15.dp,
        )
    } else {
        Modifier.neumorphicKeyCap(
            surfaceColor = keyBg,
            shadowLight = palette.shadowLight,
            shadowDark = palette.shadowDark,
            cornerRadius = keyCorner,
            shadowOffset = shadowDepth,
            blurRadius = blurRadius,
            maxSpreadX = spreadX,
            maxSpreadY = spreadY,
            borderColor = borderCol,
            borderWidth = 1.15.dp,
        )
    }

    val inputModifier = if (holdOptions.isNotEmpty() && onPopupChanged != null && onPopupCommit != null) {
        Modifier.pointerInput(spec.id) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                gesturePressed = true
                if (canShowKeyPreview) {
                    keyRectInRoot?.let { rect ->
                        latestOnKeyPreviewChanged?.invoke(ActiveKeyPreview(rect, latestPrimaryText))
                    }
                }
                var releasedEarly = false
                var abortedByScroll = false
                try {
                    val waitResult = withTimeoutOrNull(300L) {
                        while (true) {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.firstOrNull { it.id == down.id }
                            if (ch == null || !ch.pressed) {
                                releasedEarly = true
                                abortedByScroll = ch?.isConsumed == true
                                break
                            }
                            if (ch.isConsumed) {
                                abortedByScroll = true
                                break
                            }
                        }
                    }
                    if (waitResult != null && releasedEarly) {
                        latestOnKeyPreviewChanged?.invoke(null)
                        if (!abortedByScroll) {
                            latestOnClick()
                        }
                    } else if (!abortedByScroll) {
                        latestOnKeyPreviewChanged?.invoke(null)
                        val opts = latestHoldOptions
                        val rect = keyRectInRoot
                        if (opts.isNotEmpty() && rect != null) {
                            val defaultIdx = opts.indexOfFirst { it == latestCornerHint }.takeIf { it >= 0 } ?: 0
                            var selIdx = defaultIdx
                            latestOnPopupChanged?.invoke(ActiveHoldPopup(rect, opts, selIdx))

                            val cols = if (opts.size <= 5) opts.size else 4
                            val rowsCount = ceil(opts.size / cols.toFloat()).roundToInt().coerceAtLeast(1)
                            val cellWPx = with(density) { 38.dp.toPx() }
                            val cellHPx = with(density) { 40.dp.toPx() }
                            val padPx = with(density) { 5.dp.toPx() }
                            val popupWPx = cellWPx * cols + padPx * 2
                            val popupHPx = cellHPx * rowsCount + padPx * 2
                            val rootW = (rootCoordsProvider()?.size?.width ?: 1080).toFloat()
                            val popupLeft = (rect.center.x - popupWPx / 2f)
                                .coerceIn(6f, (rootW - popupWPx - 6f).coerceAtLeast(6f))
                            val popupTop = (rect.top - popupHPx - 10f).coerceAtLeast(4f)

                            while (true) {
                                val ev = awaitPointerEvent()
                                val ch = ev.changes.firstOrNull { it.id == down.id }
                                if (ch == null || !ch.pressed) {
                                    break
                                }
                                ch.consume()
                                val ptrX = rect.left + ch.position.x
                                val ptrY = rect.top + ch.position.y
                                val relX = ptrX - (popupLeft + padPx)
                                val relY = ptrY - (popupTop + padPx)
                                // Only switch hovered slot once finger moves near popup area or sideways
                                if (ptrY < rect.top + 8f) {
                                    val c = (relX / cellWPx).toInt().coerceIn(0, cols - 1)
                                    val r = (relY / cellHPx).toInt().coerceIn(0, rowsCount - 1)
                                    val candidate = r * cols + c
                                    if (candidate in opts.indices && candidate != selIdx) {
                                        selIdx = candidate
                                        latestOnPopupChanged?.invoke(ActiveHoldPopup(rect, opts, selIdx))
                                    }
                                }
                            }
                            latestOnPopupCommit?.invoke(opts[selIdx])
                        } else {
                            // Released without popup
                            while (true) {
                                val ev = awaitPointerEvent()
                                val ch = ev.changes.firstOrNull { it.id == down.id }
                                if (ch == null || !ch.pressed) break
                            }
                            latestOnClick()
                        }
                    }
                } finally {
                    gesturePressed = false
                    latestOnKeyPreviewChanged?.invoke(null)
                    latestOnPopupChanged?.invoke(null)
                }
            }
        }
    } else if (spec.keyCode == KeyEvent.KEYCODE_SPACE && onSpaceCursorSlide != null) {
        Modifier.pointerInput(spec.id) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                gesturePressed = true
                var totalDx = 0f
                var accumX = 0f
                var slid = false
                var releasedEarly = false
                var abortedByScroll = false
                val activationPx = with(density) { 10.dp.toPx() }
                val stepPx = with(density) { 11.dp.toPx() }
                try {
                    val waitResult = withTimeoutOrNull(340L) {
                        while (true) {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.firstOrNull { it.id == down.id }
                            if (ch == null || !ch.pressed) {
                                releasedEarly = true
                                abortedByScroll = ch?.isConsumed == true
                                break
                            }
                            if (ch.isConsumed) {
                                abortedByScroll = true
                                break
                            }
                            val dx = ch.positionChange().x
                            if (!slid) {
                                totalDx += dx
                                if (kotlin.math.abs(totalDx) >= activationPx) {
                                    slid = true
                                    isSlidingCursor = true
                                    accumX = totalDx
                                    ch.consume()
                                }
                            } else {
                                accumX += dx
                                ch.consume()
                            }
                            if (slid) {
                                while (accumX <= -stepPx) {
                                    accumX += stepPx
                                    latestOnSpaceCursorSlide?.invoke(-1)
                                }
                                while (accumX >= stepPx) {
                                    accumX -= stepPx
                                    latestOnSpaceCursorSlide?.invoke(1)
                                }
                            }
                        }
                    }
                    if (waitResult != null) {
                        if (releasedEarly && !slid && !abortedByScroll) {
                            latestOnClick()
                        }
                    } else if (!abortedByScroll) {
                        if (!slid && latestHoldToRepeat) {
                            latestOnClick()
                            while (true) {
                                val nextWait = withTimeoutOrNull(55L) {
                                    while (true) {
                                        val ev = awaitPointerEvent()
                                        val ch = ev.changes.firstOrNull { it.id == down.id }
                                        if (ch == null || !ch.pressed) break
                                    }
                                }
                                if (nextWait != null) break
                                (latestOnRepeat ?: latestOnClick).invoke()
                            }
                        } else {
                            while (true) {
                                val ev = awaitPointerEvent()
                                val ch = ev.changes.firstOrNull { it.id == down.id }
                                if (ch == null || !ch.pressed) {
                                    if (!slid && ch?.isConsumed != true) {
                                        latestOnClick()
                                    }
                                    break
                                }
                                if (ch.isConsumed && !slid) break
                                val dx = ch.positionChange().x
                                if (!slid) {
                                    totalDx += dx
                                    if (kotlin.math.abs(totalDx) >= activationPx) {
                                        slid = true
                                        isSlidingCursor = true
                                        accumX = totalDx
                                        ch.consume()
                                    }
                                } else {
                                    accumX += dx
                                    ch.consume()
                                }
                                if (slid) {
                                    while (accumX <= -stepPx) {
                                        accumX += stepPx
                                        latestOnSpaceCursorSlide?.invoke(-1)
                                    }
                                    while (accumX >= stepPx) {
                                        accumX -= stepPx
                                        latestOnSpaceCursorSlide?.invoke(1)
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    gesturePressed = false
                    isSlidingCursor = false
                }
            }
        }
    } else if (isRepeatableSpec && (spec.keyCode == KeyEvent.KEYCODE_DEL || holdToRepeatEnabled)) {
        Modifier.pointerInput(spec.id) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                gesturePressed = true
                var releasedEarly = false
                var abortedByScroll = false
                try {
                    val waitResult = withTimeoutOrNull(340L) {
                        while (true) {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.firstOrNull { it.id == down.id }
                            if (ch == null || !ch.pressed) {
                                releasedEarly = true
                                abortedByScroll = ch?.isConsumed == true
                                break
                            }
                            if (ch.isConsumed) {
                                abortedByScroll = true
                                break
                            }
                        }
                    }
                    if (!abortedByScroll) {
                        latestOnClick()
                        if (waitResult == null && !releasedEarly) {
                            val repeatIntervalMs = if (spec.keyCode == KeyEvent.KEYCODE_DEL) 62L else 55L
                            while (true) {
                                val nextWait = withTimeoutOrNull(repeatIntervalMs) {
                                    while (true) {
                                        val ev = awaitPointerEvent()
                                        val ch = ev.changes.firstOrNull { it.id == down.id }
                                        if (ch == null || !ch.pressed) break
                                    }
                                }
                                if (nextWait != null) break
                                (latestOnRepeat ?: latestOnClick).invoke()
                            }
                        }
                    }
                } finally {
                    gesturePressed = false
                }
            }
        }
    } else {
        Modifier.pointerInput(spec.id) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                gesturePressed = true
                if (canShowKeyPreview) {
                    keyRectInRoot?.let { rect ->
                        latestOnKeyPreviewChanged?.invoke(ActiveKeyPreview(rect, latestPrimaryText))
                    }
                }
                val slopPx = with(density) { 18.dp.toPx() }
                try {
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id }
                        if (ch == null) break
                        if (!ch.pressed) {
                            if (!ch.isConsumed) {
                                val inBounds = ch.position.x >= -slopPx &&
                                    ch.position.x <= size.width + slopPx &&
                                    ch.position.y >= -slopPx &&
                                    ch.position.y <= size.height + slopPx
                                if (inBounds) {
                                    latestOnKeyPreviewChanged?.invoke(null)
                                    latestOnClick()
                                }
                            }
                            break
                        }
                        if (ch.isConsumed) break
                    }
                } finally {
                    gesturePressed = false
                    latestOnKeyPreviewChanged?.invoke(null)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .height(keyHeight)
            .offset(y = pressOffsetY)
            .onGloballyPositioned { coords ->
                val rc = rootCoordsProvider()
                if (rc != null && rc.isAttached && coords.isAttached) {
                    keyRectInRoot = rc.localBoundingBoxOf(coords, clipBounds = false)
                }
            }
            .then(neumorphicModifier)
            .then(
                if (rgbActive) {
                    Modifier.rgbAnimatedBorder(
                        phaseDegrees = rgbPhaseDeg,
                        cornerRadius = keyCorner,
                        strokeWidth = if (isEngaged || spec.isAccent) 1.7.dp else 1.25.dp,
                        alpha = if (isEngaged) 0.96f else 0.78f,
                    )
                } else {
                    Modifier
                },
            )
            .then(inputModifier)
            .padding(horizontal = if (compact) 1.dp else 2.dp, vertical = 1.dp)
            .testTag("key_${spec.id}"),
    ) {
        if (cornerText != null) {
            Text(
                text = cornerText,
                color = palette.secondaryText.copy(alpha = 0.80f),
                fontFamily = BodyFontFamily,
                fontSize = (if (compact) 7f else 8f * fontScale.coerceAtMost(1.05f)).sp,
                lineHeight = 9.sp,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 3.dp, top = 1.dp),
            )
        }

        if (isModifierLit) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(3.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(palette.activeIndicator),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = if (cornerText != null && spec.iconType == null) 3.dp else 0.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (spec.iconType) {
                KeyIconType.BACKSPACE -> Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = textColor,
                    modifier = Modifier.size(iconSize),
                )
                KeyIconType.TAB -> Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardTab,
                    contentDescription = "Tab",
                    tint = textColor,
                    modifier = Modifier.size(iconSize),
                )
                KeyIconType.ENTER -> Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                    contentDescription = "Enter",
                    tint = textColor,
                    modifier = Modifier.size(iconSize),
                )
                KeyIconType.SHIFT -> ShiftArrowGlyph(
                    color = textColor,
                    filled = isModifierLit,
                    size = iconSize,
                )
                KeyIconType.ARROW_UP -> Icon(
                    imageVector = Icons.Default.ArrowDropUp,
                    contentDescription = "Arrow Up",
                    tint = textColor,
                    modifier = Modifier.size((iconSize + 3.dp).coerceAtMost(20.dp)),
                )
                KeyIconType.ARROW_DOWN -> Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Arrow Down",
                    tint = textColor,
                    modifier = Modifier.size((iconSize + 3.dp).coerceAtMost(20.dp)),
                )
                KeyIconType.ARROW_LEFT -> Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowLeft,
                    contentDescription = "Arrow Left",
                    tint = textColor,
                    modifier = Modifier.size((iconSize + 3.dp).coerceAtMost(20.dp)),
                )
                KeyIconType.ARROW_RIGHT -> Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                    contentDescription = "Arrow Right",
                    tint = textColor,
                    modifier = Modifier.size((iconSize + 3.dp).coerceAtMost(20.dp)),
                )
                KeyIconType.MIC_OR_NUM -> Icon(
                    imageVector = Icons.Default.Dialpad,
                    contentDescription = "Toggle Numpad",
                    tint = palette.activeIndicator,
                    modifier = Modifier.size(iconSize),
                )
                KeyIconType.META_WIN -> WindowsLogoGlyph(
                    color = textColor,
                    size = (iconSize - 1.dp).coerceAtLeast(10.dp),
                )
                KeyIconType.SPACE -> SpaceBarGlyph(
                    color = textColor,
                    modifier = Modifier
                        .width(if (compact) 16.dp else 24.dp)
                        .height(6.dp),
                )
                KeyIconType.LAYER_CIRCLE -> Canvas(modifier = Modifier.size((iconSize - 3.dp).coerceAtLeast(8.dp))) {
                    drawCircle(
                        color = textColor,
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }
                null -> {
                    val baseSp = when {
                        compact && primaryText.length > 3 -> 8.2f
                        compact && primaryText.length > 1 -> 9.4f
                        compact -> 11.4f
                        primaryText.length > 3 -> 9.2f
                        primaryText.length > 1 -> 10.6f
                        else -> 13.2f
                    }
                    Text(
                        text = primaryText,
                        color = textColor,
                        fontFamily = BodyFontFamily,
                        fontSize = (baseSp * fontScale).coerceIn(8f, 17f).sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}

@Composable
private fun WindowsLogoGlyph(color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val pad = w * 0.08f
        val gap = (w * 0.10f).coerceAtLeast(1.2.dp.toPx())
        val paneW = (w - pad * 2f - gap) / 2f
        val paneH = (h - pad * 2f - gap) / 2f
        val corner = CornerRadius(0.8.dp.toPx(), 0.8.dp.toPx())

        drawRoundRect(color = color, topLeft = Offset(pad, pad), size = Size(paneW, paneH), cornerRadius = corner)
        drawRoundRect(color = color, topLeft = Offset(pad + paneW + gap, pad), size = Size(paneW, paneH), cornerRadius = corner)
        drawRoundRect(color = color, topLeft = Offset(pad, pad + paneH + gap), size = Size(paneW, paneH), cornerRadius = corner)
        drawRoundRect(color = color, topLeft = Offset(pad + paneW + gap, pad + paneH + gap), size = Size(paneW, paneH), cornerRadius = corner)
    }
}

@Composable
private fun ShiftArrowGlyph(color: Color, filled: Boolean, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.50f, h * 0.14f)
            lineTo(w * 0.86f, h * 0.54f)
            lineTo(w * 0.66f, h * 0.54f)
            lineTo(w * 0.66f, h * 0.86f)
            lineTo(w * 0.34f, h * 0.86f)
            lineTo(w * 0.34f, h * 0.54f)
            lineTo(w * 0.14f, h * 0.54f)
            close()
        }
        if (filled) {
            drawPath(path = path, color = color)
        } else {
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 1.65.dp.toPx(), join = StrokeJoin.Round),
            )
        }
    }
}

@Composable
private fun SpaceBarGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(0f, h)
            lineTo(w, h)
            lineTo(w, 0f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun DeckModeChip(
    label: String,
    selected: Boolean,
    palette: ResolvedKeyboardPalette,
    borderColor: Color?,
    compact: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    val modifier = if (selected) {
        Modifier.neumorphicInset(
            surfaceColor = palette.activeFill,
            shadowLight = palette.shadowLight,
            shadowDark = palette.shadowDark,
            cornerRadius = 9.dp,
            depth = 2.dp,
            borderColor = borderColor,
        )
    } else {
        Modifier.neumorphicKeyCap(
            surfaceColor = palette.keyBg,
            shadowLight = palette.shadowLight,
            shadowDark = palette.shadowDark,
            cornerRadius = 9.dp,
            borderColor = borderColor,
        )
    }
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (compact) 7.dp else 9.dp,
                vertical = if (compact) 3.5.dp else 4.5.dp,
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) palette.activeIndicator else palette.secondaryText,
            fontFamily = BodyFontFamily,
            fontSize = if (compact) 9.5.sp else 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TopActionIconChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    active: Boolean,
    palette: ResolvedKeyboardPalette,
    borderColor: Color?,
    testTag: String,
    onClick: () -> Unit,
) {
    val mod = if (active) {
        Modifier.neumorphicInset(
            surfaceColor = palette.activeFill,
            shadowLight = palette.shadowLight,
            shadowDark = palette.shadowDark,
            cornerRadius = 9.dp,
            depth = 2.dp,
            borderColor = borderColor,
        )
    } else {
        Modifier.neumorphicKeyCap(
            surfaceColor = palette.keyBg,
            shadowLight = palette.shadowLight,
            shadowDark = palette.shadowDark,
            cornerRadius = 9.dp,
            borderColor = borderColor,
        )
    }
    Box(
        modifier = mod
            .size(26.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) palette.accentBg else palette.primaryText,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun SimpleSuggestionBar(
    slots: List<SuggestionSlot>,
    suppressed: Boolean,
    palette: ResolvedKeyboardPalette,
    barHeight: Dp,
    hGap: Dp,
    cornerRadius: Dp,
    fontScale: Float,
    showKeyBorder: Boolean,
    keyOpacity: Float,
    rgbActive: Boolean,
    rgbPhaseDeg: Float,
    onSelectSlot: (SuggestionSlot) -> Unit,
) {
    val borderCol = if (showKeyBorder && !rgbActive) palette.keyBorder.copy(alpha = 0.85f) else null
    if (suppressed) {
        val modBg = if (keyOpacity < 0.99f) {
            palette.modifierBg.copy(alpha = (palette.modifierBg.alpha * keyOpacity).coerceIn(0.14f, 1f))
        } else {
            palette.modifierBg
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .neumorphicInset(
                    surfaceColor = modBg,
                    shadowLight = palette.shadowLight,
                    shadowDark = palette.shadowDark,
                    cornerRadius = cornerRadius,
                    depth = 2.dp,
                    borderColor = borderCol,
                )
                .padding(horizontal = 10.dp)
                .testTag("simple_suggestion_paused"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Suggestions paused for protected input",
                fontFamily = BodyFontFamily,
                fontSize = (10.5f * fontScale).coerceIn(9f, 12f).sp,
                fontWeight = FontWeight.Medium,
                color = palette.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        return
    }

    val paddedSlots = remember(slots) {
        if (slots.size >= 3) {
            slots.take(3)
        } else {
            val out = slots.toMutableList()
            val defaults = listOf("I", "The", "Hello")
            for (d in defaults) {
                if (out.size >= 3) break
                if (out.none { it.text.equals(d, ignoreCase = true) }) {
                    out.add(SuggestionSlot(text = d, commitTextValue = d))
                }
            }
            out
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("simple_suggestion_bar"),
        horizontalArrangement = Arrangement.spacedBy(hGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        paddedSlots.forEachIndexed { idx, slot ->
            val rawBg = when {
                slot.isAutoCorrectHighlight -> palette.activeFill
                else -> palette.keyBg
            }
            val bg = if (keyOpacity < 0.99f) {
                val targetAlpha = if (slot.isAutoCorrectHighlight) (keyOpacity + 0.16f).coerceAtMost(0.95f) else keyOpacity
                rawBg.copy(alpha = (rawBg.alpha * targetAlpha).coerceIn(0.14f, 1f))
            } else {
                rawBg
            }
            val displayText = if (slot.isLiteralTyped && !SimpleSuggestionEngine.isKnownWord(slot.text)) {
                "\"${slot.text}\""
            } else {
                slot.text
            }
            val textColor = when {
                slot.isAutoCorrectHighlight -> palette.accentBg
                slot.isLiteralTyped -> palette.secondaryText
                else -> palette.primaryText
            }
            val pillModifier = if (slot.isAutoCorrectHighlight) {
                Modifier.neumorphicInset(
                    surfaceColor = bg,
                    shadowLight = palette.shadowLight,
                    shadowDark = palette.shadowDark,
                    cornerRadius = cornerRadius,
                    depth = 2.dp,
                    borderColor = borderCol,
                )
            } else {
                Modifier.neumorphicKeyCap(
                    surfaceColor = bg,
                    shadowLight = palette.shadowLight,
                    shadowDark = palette.shadowDark,
                    cornerRadius = cornerRadius,
                    shadowOffset = 1.4.dp,
                    blurRadius = 2.2.dp,
                    maxSpreadX = (hGap * 0.45f).coerceIn(0.5.dp, 2.0.dp),
                    maxSpreadY = 1.8.dp,
                    borderColor = borderCol,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(barHeight)
                    .then(pillModifier)
                    .then(
                        if (rgbActive && slot.isAutoCorrectHighlight) {
                            Modifier.rgbAnimatedBorder(
                                phaseDegrees = rgbPhaseDeg,
                                cornerRadius = cornerRadius,
                                strokeWidth = 1.3.dp,
                                alpha = 0.85f,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelectSlot(slot) }
                    .padding(horizontal = 8.dp)
                    .testTag("simple_suggestion_slot_$idx"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = displayText,
                    fontFamily = BodyFontFamily,
                    fontSize = (12.sp.value * fontScale).coerceIn(10f, 15f).sp,
                    fontWeight = if (slot.isAutoCorrectHighlight) FontWeight.Bold else FontWeight.Medium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
