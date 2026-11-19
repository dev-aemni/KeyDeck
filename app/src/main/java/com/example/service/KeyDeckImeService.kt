package com.example.service

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
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
import com.example.keyboard.KeyDispatcher
import com.example.keyboard.PcKeyboardDeck
import com.example.keyboard.PcKeyboardLayoutData
import com.example.ui.theme.KeyDeckTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class KeyDeckImeService : InputMethodService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: KeyDeckRepository
    private val lifecycleOwner = OverlayLifecycleOwner()

    companion object {
        @Volatile
        var activeInstance: KeyDeckImeService? = null
            private set

        fun hasActiveInputConnection(): Boolean {
            return activeInstance?.currentInputConnection != null
        }
    }

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        repository = KeyDeckRepository(applicationContext)
        lifecycleOwner.onCreate()
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
                    initialValue = com.example.keyboard.KeyDeckSettings(),
                )
                val hotkeys by repository.hotkeysFlow.collectAsStateWithLifecycle(
                    initialValue = PcKeyboardLayoutData.defaultHotkeys,
                )
                KeyDeckTheme {
                    val palette = settings.resolvePalette()
                    val dockAlign = when (settings.dockAlignment) {
                        com.example.keyboard.KeyboardDockAlignment.LEFT -> Alignment.CenterStart
                        com.example.keyboard.KeyboardDockAlignment.CENTER -> Alignment.Center
                        com.example.keyboard.KeyboardDockAlignment.RIGHT -> Alignment.CenterEnd
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.deckBg)
                            .navigationBarsPadding(),
                        contentAlignment = dockAlign,
                    ) {
                        PcKeyboardDeck(
                            settings = settings,
                            hotkeys = hotkeys,
                            routeLabel = "IME Input",
                            isOverlay = false,
                            onKeyDispatch = { keyCode, metaState, charToCommit, comboLabel ->
                                KeyDispatcher.dispatchKey(
                                    context = this@KeyDeckImeService,
                                    keyCode = keyCode,
                                    metaState = metaState,
                                    charToCommit = charToCommit,
                                    comboLabel = comboLabel,
                                    hapticEnabled = settings.hapticFeedback,
                                )
                            },
                            onHotkeyDispatch = { hotkey ->
                                KeyDispatcher.dispatchHotkey(
                                    context = this@KeyDeckImeService,
                                    hotkey = hotkey,
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
                            onMinimize = {
                                requestHideSelf(0)
                            },
                            modifier = Modifier.fillMaxWidth(settings.keyboardWidthFraction.coerceIn(0.60f, 1.0f)),
                        )
                    }
                }
            }
        }
        window?.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(lifecycleOwner)
            decor.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            decor.setViewTreeViewModelStoreOwner(lifecycleOwner)
        }
        return composeView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        KeyDeckRuntimeState.setImeSessionActive(true)
        lifecycleOwner.onResume()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        KeyDeckRuntimeState.setImeSessionActive(false)
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
        val isRawNullInput = editorInfo == null || editorInfo.inputType == android.text.InputType.TYPE_NULL
        val hasCtrlAltMeta = (metaState and (KeyEvent.META_CTRL_ON or KeyEvent.META_ALT_ON or KeyEvent.META_META_ON)) != 0

        if (!isRawNullInput && !hasCtrlAltMeta && charToCommit != null && keyCode != KeyEvent.KEYCODE_ENTER && keyCode != KeyEvent.KEYCODE_TAB) {
            ic.commitText(charToCommit.toString(), 1)
            return true
        }

        val downEvent = KeyEvent(uptimeMs, uptimeMs, KeyEvent.ACTION_DOWN, keyCode, 0, metaState)
        val upEvent = KeyEvent(uptimeMs, uptimeMs + 10, KeyEvent.ACTION_UP, keyCode, 0, metaState)
        ic.sendKeyEvent(downEvent)
        ic.sendKeyEvent(upEvent)
        return true
    }
}
