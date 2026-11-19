package com.example.service

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.KeyDeckRuntimeState

class KeyDeckAccessibilityService : AccessibilityService() {

    private var accessibilityButtonCallback: AccessibilityButtonController.AccessibilityButtonCallback? = null

    companion object {
        @Volatile
        var activeInstance: KeyDeckAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeInstance = this
        KeyDeckRuntimeState.setAccessibilityRunning(true)

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

    override fun onDestroy() {
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

    fun dispatchPcShortcut(keyCode: Int, metaState: Int, charToCommit: Char?): String {
        val isCtrl = (metaState and KeyEvent.META_CTRL_ON) != 0
        val isAlt = (metaState and KeyEvent.META_ALT_ON) != 0
        val isShift = (metaState and KeyEvent.META_SHIFT_ON) != 0
        val isMeta = (metaState and KeyEvent.META_META_ON) != 0

        val root = rootInActiveWindow
        val focusedNode = root?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: root?.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)

        // 1. System-wide PC navigation shortcuts
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

        // 2. Clipboard & Selection shortcuts on focused node
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

        // 3. If an editable text field is focused, inject character / backspace directly
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

        // 4. Non-text-input Web / App Navigation & Media Shortcuts
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
