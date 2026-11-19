package com.example.service

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.KeyDeckRepository
import com.example.data.KeyDeckRuntimeState
import com.example.keyboard.KeyDeckSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.hypot

class KeyDeckAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var accessibilityButtonCallback: AccessibilityButtonController.AccessibilityButtonCallback? = null

    private var cursorX = -1f
    private var cursorY = -1f
    private var lastGestureAtMs = 0L
    private var pendingDx = 0f
    private var pendingDy = 0f

    companion object {
        @Volatile
        var activeInstance: KeyDeckAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeInstance = this
        KeyDeckRuntimeState.setAccessibilityRunning(true)

        val dm = resources.displayMetrics
        cursorX = dm.widthPixels * 0.5f
        cursorY = dm.heightPixels * 0.42f

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val controller = accessibilityButtonController
            val callback = object : AccessibilityButtonController.AccessibilityButtonCallback() {
                override fun onClicked(controller: AccessibilityButtonController) {
                    handleAccessibilityButtonClick()
                }
            }
            accessibilityButtonCallback = callback
            controller.registerAccessibilityButtonCallback(callback)
        }

        val repo = KeyDeckRepository(applicationContext)
        serviceScope.launch {
            combine(
                KeyDeckRuntimeState.isOverlayServiceRunning,
                KeyDeckRuntimeState.isKeyboardOverlayExpanded,
                repo.settingsFlow,
            ) { overlayRunning, overlayExpanded, settings: KeyDeckSettings ->
                (overlayRunning || overlayExpanded) && settings.suppressAutoImeOnInput
            }.collect { shouldHide ->
                syncSoftKeyboardPolicy(shouldHide)
            }
        }
    }

    fun syncSoftKeyboardPolicy(shouldHideSystemKeyboard: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching {
                val desired = if (shouldHideSystemKeyboard) {
                    SHOW_MODE_HIDDEN
                } else {
                    SHOW_MODE_AUTO
                }
                if (softKeyboardController.showMode != desired) {
                    softKeyboardController.showMode = desired
                }
            }
        }
    }

    private fun handleAccessibilityButtonClick() {
        if (KeyDeckRuntimeState.isOverlayServiceRunning.value) {
            val toggleIntent = Intent(this, FloatingKeyboardService::class.java).apply {
                action = FloatingKeyboardService.ACTION_TOGGLE_KEYBOARD
            }
            startService(toggleIntent)
        } else if (Settings.canDrawOverlays(this)) {
            val startIntent = Intent(this, FloatingKeyboardService::class.java).apply {
                action = FloatingKeyboardService.ACTION_START_AND_EXPAND
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(startIntent)
            } else {
                startService(startIntent)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Window state is queried on demand via rootInActiveWindow during key dispatch
    }

    override fun onInterrupt() {
        // Required override
    }

    override fun onUnbind(intent: Intent?): Boolean {
        syncSoftKeyboardPolicy(false)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        syncSoftKeyboardPolicy(false)
        serviceScope.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            accessibilityButtonCallback?.let {
                accessibilityButtonController.unregisterAccessibilityButtonCallback(it)
            }
        }
        KeyDeckRuntimeState.setAccessibilityRunning(false)
        if (activeInstance === this) {
            activeInstance = null
        }
        super.onDestroy()
    }

    fun dispatchMouseLookOrMove(dx: Float, dy: Float, mcpeLookMode: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val dm = resources.displayMetrics
        val w = dm.widthPixels.toFloat().coerceAtLeast(320f)
        val h = dm.heightPixels.toFloat().coerceAtLeast(480f)

        if (cursorX < 0f || cursorY < 0f) {
            cursorX = w * 0.5f
            cursorY = h * 0.42f
        }

        pendingDx += dx
        pendingDy += dy

        val now = SystemClock.uptimeMillis()
        if (now - lastGestureAtMs < 65L) return
        val dist = hypot(pendingDx.toDouble(), pendingDy.toDouble())
        if (dist < 4.0) return

        lastGestureAtMs = now
        val moveX = pendingDx.coerceIn(-w * 0.35f, w * 0.35f)
        val moveY = pendingDy.coerceIn(-h * 0.30f, h * 0.30f)
        pendingDx = 0f
        pendingDy = 0f

        val startX: Float
        val startY: Float
        val endX: Float
        val endY: Float

        if (mcpeLookMode) {
            // MCPE camera look expects swipes in the upper-right screen viewport
            startX = w * 0.65f
            startY = h * 0.36f
            endX = (startX + moveX).coerceIn(20f, w - 20f)
            endY = (startY + moveY).coerceIn(40f, h * 0.65f)
        } else {
            startX = cursorX.coerceIn(16f, w - 16f)
            startY = cursorY.coerceIn(32f, h - 32f)
            cursorX = (startX + moveX).coerceIn(16f, w - 16f)
            cursorY = (startY + moveY).coerceIn(32f, h - 32f)
            endX = cursorX
            endY = cursorY
        }

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 45L)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    fun dispatchMouseTap(isRightClick: Boolean, mcpeLookMode: Boolean): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return "Tap requires Android 7+"
        val dm = resources.displayMetrics
        val w = dm.widthPixels.toFloat().coerceAtLeast(320f)
        val h = dm.heightPixels.toFloat().coerceAtLeast(480f)

        val tapX = if (mcpeLookMode) w * 0.5f else cursorX.coerceIn(16f, w - 16f)
        val tapY = if (mcpeLookMode) h * 0.42f else cursorY.coerceIn(32f, h - 32f)
        val duration = if (isRightClick) 420L else 40L

        val path = Path().apply { moveTo(tapX, tapY) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, duration)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
        return if (isRightClick) "RMB → Hold/Use Tap" else "LMB → Attack/Click Tap"
    }

    fun dispatchPcShortcut(keyCode: Int, metaState: Int, charToCommit: Char?): String {
        val isCtrl = (metaState and KeyEvent.META_CTRL_ON) != 0
        val isAlt = (metaState and KeyEvent.META_ALT_ON) != 0
        val isShift = (metaState and KeyEvent.META_SHIFT_ON) != 0
        val isMeta = (metaState and KeyEvent.META_META_ON) != 0

        val root = rootInActiveWindow
        val focusedNode = root?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: root?.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)

        if (isAlt && keyCode == KeyEvent.KEYCODE_TAB) {
            performGlobalAction(GLOBAL_ACTION_RECENTS)
            return "Alt+Tab → Recents"
        }
        if (isMeta || keyCode == KeyEvent.KEYCODE_META_LEFT) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            return "Win → Home"
        }
        if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            return "Esc → Back"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && keyCode == KeyEvent.KEYCODE_SYSRQ) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            return "PrtSc → Screenshot"
        }

        if (isCtrl && focusedNode != null) {
            when (keyCode) {
                KeyEvent.KEYCODE_C -> {
                    val copied = focusedNode.performAction(AccessibilityNodeInfo.ACTION_COPY)
                    if (!copied && !focusedNode.text.isNullOrEmpty()) {
                        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("KeyDeck", focusedNode.text))
                        return "Ctrl+C → Copied node text"
                    }
                    return "Ctrl+C → Copy"
                }
                KeyEvent.KEYCODE_V -> {
                    focusedNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                    return "Ctrl+V → Paste"
                }
                KeyEvent.KEYCODE_X -> {
                    focusedNode.performAction(AccessibilityNodeInfo.ACTION_CUT)
                    return "Ctrl+X → Cut"
                }
                KeyEvent.KEYCODE_A -> {
                    val textLen = focusedNode.text?.length ?: 0
                    val args = Bundle().apply {
                        putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                        putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, textLen)
                    }
                    focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)
                    return "Ctrl+A → Select All"
                }
            }
        }

        if (focusedNode != null && focusedNode.isEditable) {
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                val existing = focusedNode.text?.toString() ?: ""
                if (existing.isNotEmpty()) {
                    val args = Bundle().apply {
                        putCharSequence(
                            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                            existing.dropLast(1),
                        )
                    }
                    focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                }
                return "Bksp → Editable Node"
            }
            if (!isCtrl && !isAlt && !isMeta && charToCommit != null) {
                val existing = focusedNode.text?.toString() ?: ""
                val args = Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        existing + charToCommit,
                    )
                }
                focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                return "Text '$charToCommit' → Editable Node"
            }
        }

        when (keyCode) {
            KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (scrollActiveWindow(root, forward = true)) {
                    return "Scroll Down"
                }
            }
            KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_DPAD_UP -> {
                if (scrollActiveWindow(root, forward = false)) {
                    return "Scroll Up"
                }
            }
            KeyEvent.KEYCODE_TAB -> {
                val direction = if (isShift) View.FOCUS_BACKWARD else View.FOCUS_FORWARD
                val next = focusedNode?.focusSearch(direction) ?: root?.focusSearch(direction)
                if (next != null) {
                    next.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                    return if (isShift) "Shift+Tab → Focus Prev" else "Tab → Focus Next"
                }
            }
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (focusedNode != null && focusedNode.isClickable) {
                    focusedNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return "Enter → Click Focused"
                }
            }
            KeyEvent.KEYCODE_SPACE -> {
                dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                return "Space → Media Play/Pause"
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
                return "Right → Media Forward"
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_REWIND)
                return "Left → Media Rewind"
            }
        }

        return "Global Shortcut ($keyCode)"
    }

    private fun scrollActiveWindow(root: AccessibilityNodeInfo?, forward: Boolean): Boolean {
        if (root == null) return false
        val scrollable = findScrollableNode(root) ?: return false
        val action = if (forward) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }
        return scrollable.performAction(action)
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val match = findScrollableNode(child)
            if (match != null) return match
        }
        return null
    }

    private fun dispatchMediaKey(mediaKeyCode: Int) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val now = SystemClock.uptimeMillis()
        audioManager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, mediaKeyCode, 0))
        audioManager.dispatchMediaKeyEvent(KeyEvent(now, now + 10, KeyEvent.ACTION_UP, mediaKeyCode, 0))
    }
}
