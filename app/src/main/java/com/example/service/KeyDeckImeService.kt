package com.example.service

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.KeyDeckRepository
import com.example.data.KeyDeckRuntimeState
import com.example.keyboard.BuiltInWallpapers
import com.example.keyboard.KeyDeckSettings
import com.example.keyboard.KeyDispatcher
import com.example.keyboard.KeyboardDockAlignment
import com.example.keyboard.PcKeyboardDeck
import com.example.keyboard.WallpaperSourceType
import com.example.ui.theme.KeyDeckTheme
import com.example.ui.theme.resolveIsAppDark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class KeyDeckImeService : InputMethodService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: KeyDeckRepository
    private val lifecycleOwner = OverlayLifecycleOwner()

    @Volatile
    private var latestSettings = KeyDeckSettings()

    companion object {
        @Volatile
        var activeInstance: KeyDeckImeService? = null
            private set

        fun hasActiveInputConnection(): Boolean {
            return activeInstance?.currentInputConnection != null
        }
    }

    private fun shouldSuppressViewForFloatingDeck(): Boolean {
        return KeyDeckRuntimeState.isKeyboardOverlayExpanded.value && latestSettings.suppressAutoImeOnInput
    }

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        repository = KeyDeckRepository(applicationContext)
        lifecycleOwner.onCreate()
        serviceScope.launch {
            repository.migrateSchemaIfNeeded()
            combine(
                repository.settingsFlow,
                KeyDeckRuntimeState.isKeyboardOverlayExpanded,
            ) { settings, overlayExpanded ->
                settings to overlayExpanded
            }.collect { (settings, overlayExpanded) ->
                latestSettings = settings
                if (overlayExpanded && settings.suppressAutoImeOnInput) {
                    requestHideSelf(0)
                }
            }
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onEvaluateInputViewShown(): Boolean {
        if (shouldSuppressViewForFloatingDeck()) {
            return false
        }
        return super.onEvaluateInputViewShown()
    }

    override fun onShowInputRequested(flags: Int, configChange: Boolean): Boolean {
        if (shouldSuppressViewForFloatingDeck()) {
            return false
        }
        return super.onShowInputRequested(flags, configChange)
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        KeyDeckRuntimeState.setImeSessionActive(currentInputConnection != null)
        if (!restarting) {
            KeyDeckRuntimeState.notifyInputSessionReset()
        }
    }

    override fun onFinishInput() {
        super.onFinishInput()
        KeyDeckRuntimeState.setImeSessionActive(false)
        KeyDeckRuntimeState.notifyInputSessionReset()
    }

    override fun onCreateInputView(): View {
        lifecycleOwner.onResume()
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setContent {
                val settings by repository.settingsFlow.collectAsStateWithLifecycle(
                    initialValue = KeyDeckSettings(),
                )
                val hotkeys by repository.hotkeysFlow.collectAsStateWithLifecycle(
                    initialValue = com.example.keyboard.PcKeyboardLayoutData.defaultHotkeys,
                )
                val savedWallpapers by repository.wallpapersFlow.collectAsStateWithLifecycle(
                    initialValue = emptyList(),
                )
                val activeWallpaper = BuiltInWallpapers.resolve(settings.activeWallpaperId, savedWallpapers)
                val isDark = resolveIsAppDark(settings.appThemeMode)
                KeyDeckTheme(darkTheme = isDark) {
                    val palette = settings.resolvePalette(isDark)
                    val dockAlign = when (settings.dockAlignment) {
                        KeyboardDockAlignment.LEFT -> Alignment.CenterStart
                        KeyboardDockAlignment.CENTER -> Alignment.Center
                        KeyboardDockAlignment.RIGHT -> Alignment.CenterEnd
                    }
                    val imeNavBg = if (activeWallpaper != null &&
                        activeWallpaper.sourceType != WallpaperSourceType.NONE &&
                        activeWallpaper.darkBackdrop
                    ) {
                        androidx.compose.ui.graphics.Color.Black
                    } else {
                        palette.deckBg
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(imeNavBg)
                            .navigationBarsPadding(),
                        contentAlignment = dockAlign,
                    ) {
                        PcKeyboardDeck(
                            settings = settings,
                            routeLabel = "IME",
                            isOverlay = false,
                            hotkeys = hotkeys,
                            activeWallpaper = activeWallpaper,
                            onHotkeyDispatch = { hk ->
                                KeyDispatcher.dispatchHotkey(
                                    context = this@KeyDeckImeService,
                                    hotkey = hk,
                                    hapticEnabled = settings.hapticFeedback,
                                )
                            },
                            onKeyDispatch = { keyCode, metaState, charToCommit, comboLabel ->
                                KeyDispatcher.dispatchKey(
                                    context = this@KeyDeckImeService,
                                    keyCode = keyCode,
                                    metaState = metaState,
                                    charToCommit = charToCommit,
                                    comboLabel = comboLabel,
                                    hapticEnabled = settings.hapticFeedback,
                                    hotkeys = hotkeys,
                                )
                            },
                            onKeyHoldDispatch = { keyCode, metaState, isDown, comboLabel ->
                                KeyDispatcher.dispatchKeyHold(
                                    context = this@KeyDeckImeService,
                                    keyCode = keyCode,
                                    metaState = metaState,
                                    isDown = isDown,
                                    comboLabel = comboLabel,
                                    hapticEnabled = settings.hapticFeedback,
                                )
                            },
                            onMouseMove = { dx, dy ->
                                KeyDispatcher.dispatchMouseMove(
                                    dx = dx,
                                    dy = dy,
                                    sensitivity = settings.touchpadSensitivity,
                                    mcpeLookMode = settings.gamingMcpeLookMode,
                                )
                            },
                            onMouseClick = { isRight ->
                                KeyDispatcher.dispatchMouseClick(
                                    context = this@KeyDeckImeService,
                                    isRightClick = isRight,
                                    mcpeLookMode = settings.gamingMcpeLookMode,
                                    hapticEnabled = settings.hapticFeedback,
                                )
                            },
                            onChangeLayoutMode = { newMode ->
                                serviceScope.launch {
                                    repository.updateSettings { it.copy(layoutMode = newMode) }
                                }
                            },
                            onToggleNumLock = {
                                serviceScope.launch {
                                    repository.updateSettings { it.copy(numLockOn = !it.numLockOn) }
                                }
                            },
                            onToggleFunctionRow = {
                                serviceScope.launch {
                                    repository.updateSettings { it.copy(showFunctionRow = !it.showFunctionRow) }
                                }
                            },
                            onUpdateSettings = { transform ->
                                serviceScope.launch {
                                    repository.updateSettings(transform)
                                }
                            },
                            onMinimize = {
                                requestHideSelf(0)
                            },
                            modifier = Modifier.fillMaxWidth(settings.keyboardWidthFraction.coerceIn(0.60f, 1.0f)),
                        )
                    }
                }
            }
        }
        window?.window?.let { win ->
            win.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
            win.decorView.let { decor ->
                decor.setViewTreeLifecycleOwner(lifecycleOwner)
                decor.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
                decor.setViewTreeViewModelStoreOwner(lifecycleOwner)
            }
        }
        return composeView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (shouldSuppressViewForFloatingDeck()) {
            requestHideSelf(0)
            return
        }
        KeyDeckRuntimeState.setImeSessionActive(true)
        lifecycleOwner.onResume()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        KeyDeckRuntimeState.setImeSessionActive(currentInputConnection != null)
    }

    override fun onDestroy() {
        KeyDeckRuntimeState.setImeSessionActive(false)
        if (activeInstance === this) {
            activeInstance = null
        }
        lifecycleOwner.onDestroy()
        serviceScope.cancel()
        super.onDestroy()
    }

    fun dispatchPcKey(
        keyCode: Int,
        metaState: Int,
        charToCommit: Char?,
        uptimeMs: Long,
    ): Boolean {
        val ic = currentInputConnection ?: return false
        val editorInfo = currentInputEditorInfo
        val isRawNullInput = editorInfo == null || editorInfo.inputType == InputType.TYPE_NULL
        val hasCtrlAltMeta = (metaState and (KeyEvent.META_CTRL_ON or KeyEvent.META_ALT_ON or KeyEvent.META_META_ON)) != 0

        if (!isRawNullInput && !hasCtrlAltMeta && charToCommit != null &&
            keyCode != KeyEvent.KEYCODE_ENTER &&
            keyCode != KeyEvent.KEYCODE_NUMPAD_ENTER &&
            keyCode != KeyEvent.KEYCODE_TAB
        ) {
            return ic.commitText(charToCommit.toString(), 1)
        }

        val effectiveKeyCode = if (!isRawNullInput && keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            KeyEvent.KEYCODE_ENTER
        } else {
            keyCode
        }

        val downEvent = KeyEvent(
            uptimeMs,
            uptimeMs,
            KeyEvent.ACTION_DOWN,
            effectiveKeyCode,
            0,
            metaState,
            KeyCharacterMap.VIRTUAL_KEYBOARD,
            0,
            KeyEvent.FLAG_KEEP_TOUCH_MODE,
            InputDevice.SOURCE_KEYBOARD,
        )
        val upEvent = KeyEvent(
            uptimeMs,
            uptimeMs + 10,
            KeyEvent.ACTION_UP,
            effectiveKeyCode,
            0,
            metaState,
            KeyCharacterMap.VIRTUAL_KEYBOARD,
            0,
            KeyEvent.FLAG_KEEP_TOUCH_MODE,
            InputDevice.SOURCE_KEYBOARD,
        )
        val sentDown = ic.sendKeyEvent(downEvent)
        val sentUp = ic.sendKeyEvent(upEvent)
        return sentDown && sentUp
    }

    fun dispatchKeyHoldEvent(
        keyCode: Int,
        metaState: Int,
        isDown: Boolean,
        uptimeMs: Long,
    ): Boolean {
        val ic = currentInputConnection ?: return false
        val action = if (isDown) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP
        return ic.sendKeyEvent(
            KeyEvent(
                uptimeMs,
                uptimeMs,
                action,
                keyCode,
                0,
                metaState,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                KeyEvent.FLAG_KEEP_TOUCH_MODE,
                InputDevice.SOURCE_KEYBOARD,
            ),
        )
    }

    fun isSuggestionsSuppressedInput(): Boolean {
        val info = currentInputEditorInfo ?: return false
        val inputType = info.inputType
        if (inputType == InputType.TYPE_NULL) return true
        val cls = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        val isPassword = (cls == InputType.TYPE_CLASS_TEXT && (
            variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            )) || (cls == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        val noSuggestionsFlag = (inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0
        return isPassword || noSuggestionsFlag
    }

    fun getSurroundingChars(beforeLen: Int, afterLen: Int): Pair<String, String>? {
        val ic = currentInputConnection ?: return null
        val before = ic.getTextBeforeCursor(beforeLen, 0)?.toString() ?: ""
        val after = ic.getTextAfterCursor(afterLen, 0)?.toString() ?: ""
        return before to after
    }

    fun replaceTailText(expectedTail: String, replacement: String): Boolean {
        val ic = currentInputConnection ?: return false
        if (isSuggestionsSuppressedInput()) return false
        if (expectedTail.isNotEmpty()) {
            val before = ic.getTextBeforeCursor(expectedTail.length, 0)?.toString() ?: return false
            if (before != expectedTail) return false
        }
        ic.beginBatchEdit()
        try {
            if (expectedTail.isNotEmpty()) {
                ic.deleteSurroundingText(expectedTail.length, 0)
            }
            ic.commitText(replacement, 1)
        } finally {
            ic.endBatchEdit()
        }
        return true
    }
}
