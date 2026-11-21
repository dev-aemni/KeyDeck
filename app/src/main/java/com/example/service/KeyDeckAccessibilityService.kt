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
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
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
    private val mainHandler = Handler(Looper.getMainLooper())
    private var accessibilityButtonCallback: AccessibilityButtonController.AccessibilityButtonCallback? = null

    private var cursorX = -1f
    private var cursorY = -1f
    private var lastGestureAtMs = 0L
    private var pendingDx = 0f
    private var pendingDy = 0f

    private var lastExternalEditableNode: AccessibilityNodeInfo? = null
    private var lastEditedNodeKey: String = ""
    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()

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
                KeyDeckRuntimeState.isKeyboardOverlayExpanded,
                repo.settingsFlow,
            ) { overlayExpanded, settings: KeyDeckSettings ->
                val defaultIme = Settings.Secure.getString(
                    contentResolver,
                    Settings.Secure.DEFAULT_INPUT_METHOD,
                ) ?: ""
                val keyDeckImeSelected = defaultIme.contains(packageName) &&
                    defaultIme.contains("KeyDeckImeService")
                overlayExpanded && settings.suppressAutoImeOnInput && !keyDeckImeSelected
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
        if (event == null) return
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_VIEW_FOCUSED &&
            type != AccessibilityEvent.TYPE_VIEW_CLICKED &&
            type != AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
        ) {
            return
        }
        val src = event.source ?: return
        val srcPkg = src.packageName?.toString()
        if (srcPkg == packageName && !KeyDeckRuntimeState.isAppInForeground.value) {
            return
        }
        if (src.isEditable) {
            lastExternalEditableNode = src
        } else {
            findFocusedEditableInTree(src, maxDepth = 4)?.let {
                lastExternalEditableNode = it
            }
        }
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

    fun isGlobalSystemShortcut(keyCode: Int, metaState: Int): Boolean {
        val isAlt = (metaState and KeyEvent.META_ALT_ON) != 0
        val isMeta = (metaState and KeyEvent.META_META_ON) != 0
        return (isAlt && keyCode == KeyEvent.KEYCODE_TAB) ||
            isMeta ||
            keyCode == KeyEvent.KEYCODE_META_LEFT ||
            keyCode == KeyEvent.KEYCODE_META_RIGHT ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && keyCode == KeyEvent.KEYCODE_SYSRQ)
    }

    fun dispatchPcShortcut(keyCode: Int, metaState: Int, charToCommit: Char?): String {
        val isCtrl = (metaState and KeyEvent.META_CTRL_ON) != 0
        val isAlt = (metaState and KeyEvent.META_ALT_ON) != 0
        val isShift = (metaState and KeyEvent.META_SHIFT_ON) != 0
        val isMeta = (metaState and KeyEvent.META_META_ON) != 0

        if (isAlt && keyCode == KeyEvent.KEYCODE_TAB) {
            performGlobalAction(GLOBAL_ACTION_RECENTS)
            return "Alt+Tab → Recents"
        }
        if (isMeta || keyCode == KeyEvent.KEYCODE_META_LEFT || keyCode == KeyEvent.KEYCODE_META_RIGHT) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            return "Win → Home"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && keyCode == KeyEvent.KEYCODE_SYSRQ) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            return "PrtSc → Screenshot"
        }
        if (keyCode == KeyEvent.KEYCODE_ESCAPE || (isAlt && keyCode == KeyEvent.KEYCODE_DPAD_LEFT)) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            return if (keyCode == KeyEvent.KEYCODE_ESCAPE) "Esc → Back" else "Alt+Left → Back"
        }

        val (activeRoot, focusedNode) = findTargetWindowAndFocus()
        val editableNode = focusedNode?.takeIf { it.isEditable }
            ?: findEditableAncestorOrCached(focusedNode, activeRoot)

        // Ctrl editing & clipboard shortcuts
        if (isCtrl && !isAlt && !isMeta) {
            val targetNode = editableNode ?: focusedNode
            when (keyCode) {
                KeyEvent.KEYCODE_C -> {
                    if (targetNode != null) {
                        val copied = targetNode.performAction(AccessibilityNodeInfo.ACTION_COPY)
                        if (copied) return "Ctrl+C → Copy"
                        val text = realNodeText(targetNode)
                        if (text.isNotEmpty()) {
                            val start = targetNode.textSelectionStart
                            val end = targetNode.textSelectionEnd
                            val slice = if (start >= 0 && end > start && end <= text.length) {
                                text.substring(start, end)
                            } else {
                                text
                            }
                            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("KeyDeck", slice))
                            return "Ctrl+C → Copied text"
                        }
                    }
                    return "Ctrl+C → Select text or use KeyDeck IME"
                }

                KeyEvent.KEYCODE_V -> {
                    if (targetNode != null) {
                        if (targetNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)) {
                            return "Ctrl+V → Paste"
                        }
                        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipText = cm.primaryClip?.takeIf { it.itemCount > 0 }
                            ?.getItemAt(0)?.coerceToText(this)?.toString()
                        if (!clipText.isNullOrEmpty() && editableNode != null) {
                            if (insertTextAtCursor(editableNode, clipText)) {
                                return "Ctrl+V → Pasted text"
                            }
                        }
                    }
                    return "Ctrl+V → Focus an input field first"
                }

                KeyEvent.KEYCODE_X -> {
                    if (targetNode != null && targetNode.performAction(AccessibilityNodeInfo.ACTION_CUT)) {
                        return "Ctrl+X → Cut"
                    }
                }

                KeyEvent.KEYCODE_A -> {
                    if (targetNode != null) {
                        val textLen = realNodeText(targetNode).length
                        val args = Bundle().apply {
                            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, textLen)
                        }
                        if (targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)) {
                            return "Ctrl+A → Select All"
                        }
                    }
                }

                KeyEvent.KEYCODE_Z -> {
                    if (editableNode != null) {
                        val nodeKey = nodeIdentityKey(editableNode)
                        if (nodeKey == lastEditedNodeKey) {
                            if (isShift && redoStack.isNotEmpty()) {
                                val next = redoStack.removeLast()
                                undoStack.addLast(realNodeText(editableNode))
                                if (setNodeTextAndCursor(editableNode, next, next.length)) {
                                    return "Ctrl+Shift+Z → Redo"
                                }
                            } else if (!isShift && undoStack.isNotEmpty()) {
                                val prev = undoStack.removeLast()
                                redoStack.addLast(realNodeText(editableNode))
                                if (setNodeTextAndCursor(editableNode, prev, prev.length)) {
                                    return "Ctrl+Z → Undo"
                                }
                            }
                        }
                    }
                }

                KeyEvent.KEYCODE_Y -> {
                    if (editableNode != null && nodeIdentityKey(editableNode) == lastEditedNodeKey && redoStack.isNotEmpty()) {
                        val next = redoStack.removeLast()
                        undoStack.addLast(realNodeText(editableNode))
                        if (setNodeTextAndCursor(editableNode, next, next.length)) {
                            return "Ctrl+Y → Redo"
                        }
                    }
                }
            }
        }

        // Editable text insertion & deletion (letters, digits, numpad, symbols, space, backspace, del)
        if (editableNode != null) {
            if (!isCtrl && !isAlt && !isMeta && keyCode == KeyEvent.KEYCODE_DEL) {
                if (deleteBackwardAtCursor(editableNode)) {
                    return "Bksp → Deleted character"
                }
            }
            if (!isCtrl && !isAlt && !isMeta && keyCode == KeyEvent.KEYCODE_FORWARD_DEL) {
                if (deleteForwardAtCursor(editableNode)) {
                    return "Del → Deleted forward"
                }
            }
            if (!isCtrl && !isAlt && !isMeta &&
                (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)
            ) {
                if (submitOrNewlineInNode(editableNode)) {
                    return "Enter → Submitted / Newline"
                }
            }
            if (!isCtrl && !isAlt && !isMeta && charToCommit != null) {
                if (insertTextAtCursor(editableNode, charToCommit.toString())) {
                    return "Typed '$charToCommit'"
                }
            }
            // Cursor & line movement inside editable text
            if (!isCtrl && !isAlt && !isMeta) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT,
                    KeyEvent.KEYCODE_DPAD_RIGHT,
                    KeyEvent.KEYCODE_MOVE_HOME,
                    KeyEvent.KEYCODE_MOVE_END,
                    -> {
                        if (moveCursorInNode(editableNode, keyCode, extendSelection = isShift)) {
                            return "Cursor moved"
                        }
                    }
                }
            }
        }

        // Non-editable / global navigation & media actions
        when (keyCode) {
            KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (scrollActiveWindow(activeRoot, forward = true) || dispatchScrollGesture(forward = true)) {
                    return "Scroll Down"
                }
            }
            KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_DPAD_UP -> {
                if (scrollActiveWindow(activeRoot, forward = false) || dispatchScrollGesture(forward = false)) {
                    return "Scroll Up"
                }
            }
            KeyEvent.KEYCODE_TAB -> {
                if (moveFocusNextPrev(activeRoot, focusedNode, backward = isShift)) {
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
                if (editableNode == null) {
                    dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    return "Space → Media Play/Pause"
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (editableNode == null) {
                    dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
                    return "Right → Media Forward"
                }
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (editableNode == null) {
                    dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_REWIND)
                    return "Left → Media Rewind"
                }
            }
        }

        return if (isCtrl || isAlt || (keyCode in KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12)) {
            "Select KeyDeck IME to send PC shortcut ($keyCode)"
        } else {
            "Tap an input field or select KeyDeck IME"
        }
    }

    private fun findTargetWindowAndFocus(): Pair<AccessibilityNodeInfo?, AccessibilityNodeInfo?> {
        val candidateRoots = mutableListOf<AccessibilityNodeInfo>()
        val windowList = runCatching { windows }.getOrNull().orEmpty()
        windowList
            .filter { win ->
                win.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY &&
                    win.type != AccessibilityWindowInfo.TYPE_INPUT_METHOD
            }
            .sortedWith(
                compareByDescending<AccessibilityWindowInfo> { it.isFocused }
                    .thenByDescending { it.isActive }
                    .thenByDescending { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
                    .thenByDescending { it.layer },
            )
            .forEach { win ->
                val root = win.root ?: return@forEach
                val pkg = root.packageName?.toString()
                if (pkg == packageName && !KeyDeckRuntimeState.isAppInForeground.value) {
                    return@forEach
                }
                candidateRoots += root
            }

        val activeFallback = runCatching { rootInActiveWindow }.getOrNull()?.takeIf { root ->
            root.packageName?.toString() != packageName || KeyDeckRuntimeState.isAppInForeground.value
        }
        if (activeFallback != null && candidateRoots.none { it.windowId == activeFallback.windowId }) {
            candidateRoots += activeFallback
        }

        val bestRoot = candidateRoots.firstOrNull()

        for (root in candidateRoots) {
            val inputFocus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            if (inputFocus != null && inputFocus.isVisibleToUser) {
                return root to inputFocus
            }
        }

        for (root in candidateRoots) {
            val focusedEditable = findFocusedEditableInTree(root, maxDepth = 18)
            if (focusedEditable != null) {
                return root to focusedEditable
            }
        }

        lastExternalEditableNode?.let { cached ->
            if (runCatching { cached.refresh() }.getOrDefault(false) &&
                cached.isVisibleToUser &&
                cached.isEditable
            ) {
                val matchingRoot = candidateRoots.firstOrNull { it.windowId == cached.windowId } ?: bestRoot
                return matchingRoot to cached
            }
        }

        for (root in candidateRoots) {
            val a11yFocus = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
            if (a11yFocus != null && a11yFocus.isVisibleToUser) {
                return root to a11yFocus
            }
        }

        return bestRoot to null
    }

    private fun findEditableAncestorOrCached(
        focusedNode: AccessibilityNodeInfo?,
        activeRoot: AccessibilityNodeInfo?,
    ): AccessibilityNodeInfo? {
        var cur = focusedNode
        var hops = 0
        while (cur != null && hops < 6) {
            if (cur.isEditable && cur.isVisibleToUser) return cur
            cur = cur.parent
            hops++
        }
        if (focusedNode != null) {
            findFocusedEditableInTree(focusedNode, maxDepth = 8)?.let { return it }
        }
        lastExternalEditableNode?.let { cached ->
            if (runCatching { cached.refresh() }.getOrDefault(false) &&
                cached.isVisibleToUser &&
                cached.isEditable &&
                (activeRoot == null || cached.windowId == activeRoot.windowId)
            ) {
                return cached
            }
        }
        return null
    }

    private fun findFocusedEditableInTree(node: AccessibilityNodeInfo, maxDepth: Int): AccessibilityNodeInfo? {
        if (maxDepth <= 0) return null
        if (node.isEditable && node.isFocused && node.isVisibleToUser) {
            return node
        }
        val count = node.childCount
        for (i in 0 until count) {
            val child = node.getChild(i) ?: continue
            val match = findFocusedEditableInTree(child, maxDepth - 1)
            if (match != null) return match
        }
        return null
    }

    private fun realNodeText(node: AccessibilityNodeInfo): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && node.isShowingHintText) {
            return ""
        }
        return node.text?.toString() ?: ""
    }

    private fun nodeIdentityKey(node: AccessibilityNodeInfo): String {
        return "${node.windowId}:${node.viewIdResourceName ?: node.className}"
    }

    private fun pushUndoSnapshot(node: AccessibilityNodeInfo, currentText: String) {
        val key = nodeIdentityKey(node)
        if (key != lastEditedNodeKey) {
            lastEditedNodeKey = key
            undoStack.clear()
            redoStack.clear()
        }
        if (undoStack.lastOrNull() != currentText) {
            undoStack.addLast(currentText)
            if (undoStack.size > 30) undoStack.removeFirst()
        }
        redoStack.clear()
    }

    private fun selectionRange(node: AccessibilityNodeInfo, textLength: Int): Pair<Int, Int> {
        val rawStart = node.textSelectionStart
        val rawEnd = node.textSelectionEnd
        return if (rawStart in 0..textLength && rawEnd in 0..textLength) {
            minOf(rawStart, rawEnd) to maxOf(rawStart, rawEnd)
        } else {
            textLength to textLength
        }
    }

    private fun setNodeTextAndCursor(node: AccessibilityNodeInfo, newText: String, cursorPos: Int): Boolean {
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
        }
        val ok = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        if (ok) {
            val clamped = cursorPos.coerceIn(0, newText.length)
            val selArgs = Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, clamped)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, clamped)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selArgs)
        }
        return ok
    }

    private fun insertTextAtCursor(node: AccessibilityNodeInfo, textToInsert: String): Boolean {
        val existing = realNodeText(node)
        val (start, end) = selectionRange(node, existing.length)
        val newText = existing.substring(0, start) + textToInsert + existing.substring(end)
        val newCursor = start + textToInsert.length
        pushUndoSnapshot(node, existing)
        if (setNodeTextAndCursor(node, newText, newCursor)) {
            return true
        }
        return pasteViaClipboard(node, textToInsert)
    }

    private fun deleteBackwardAtCursor(node: AccessibilityNodeInfo): Boolean {
        val existing = realNodeText(node)
        if (existing.isEmpty()) {
            return pasteViaClipboardBackspaceFallback(node, existing)
        }
        val (start, end) = selectionRange(node, existing.length)
        val (newText, newCursor) = if (start != end) {
            (existing.substring(0, start) + existing.substring(end)) to start
        } else if (start > 0) {
            (existing.substring(0, start - 1) + existing.substring(start)) to (start - 1)
        } else {
            return true
        }
        pushUndoSnapshot(node, existing)
        if (setNodeTextAndCursor(node, newText, newCursor)) {
            return true
        }
        return pasteViaClipboardBackspaceFallback(node, existing)
    }

    private fun deleteForwardAtCursor(node: AccessibilityNodeInfo): Boolean {
        val existing = realNodeText(node)
        if (existing.isEmpty()) return true
        val (start, end) = selectionRange(node, existing.length)
        val (newText, newCursor) = if (start != end) {
            (existing.substring(0, start) + existing.substring(end)) to start
        } else if (start < existing.length) {
            (existing.substring(0, start) + existing.substring(start + 1)) to start
        } else {
            return true
        }
        pushUndoSnapshot(node, existing)
        return setNodeTextAndCursor(node, newText, newCursor)
    }

    private fun pasteViaClipboardBackspaceFallback(node: AccessibilityNodeInfo, existing: String): Boolean {
        val (start, end) = selectionRange(node, existing.length)
        if (start > 0 && start == end) {
            val selArgs = Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, start - 1)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, start)
            }
            if (node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selArgs)) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CUT)
            }
        }
        return false
    }

    private fun pasteViaClipboard(node: AccessibilityNodeInfo, text: String): Boolean {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
        val previousClip = runCatching { cm.primaryClip }.getOrNull()
        cm.setPrimaryClip(ClipData.newPlainText("KeyDeckInput", text))
        val pasted = node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        if (previousClip != null) {
            mainHandler.postDelayed({
                runCatching { cm.setPrimaryClip(previousClip) }
            }, 260L)
        }
        return pasted
    }

    private fun submitOrNewlineInNode(node: AccessibilityNodeInfo): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val hasImeEnter = node.actionList.any {
                it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id
            }
            if (hasImeEnter || !node.isMultiLine) {
                if (node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)) {
                    return true
                }
            }
        }
        if (node.isMultiLine) {
            return insertTextAtCursor(node, "\n")
        }
        if (node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        return false
    }

    private fun moveCursorInNode(
        node: AccessibilityNodeInfo,
        keyCode: Int,
        extendSelection: Boolean,
    ): Boolean {
        val len = realNodeText(node).length
        val anchor = node.textSelectionStart.coerceIn(0, len)
        val cur = node.textSelectionEnd.coerceIn(0, len)
        val next = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> (cur - 1).coerceAtLeast(0)
            KeyEvent.KEYCODE_DPAD_RIGHT -> (cur + 1).coerceAtMost(len)
            KeyEvent.KEYCODE_MOVE_HOME -> 0
            KeyEvent.KEYCODE_MOVE_END -> len
            else -> cur
        }
        val newStart = if (extendSelection) anchor else next
        val args = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, newStart)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, next)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)
    }

    private fun moveFocusNextPrev(
        root: AccessibilityNodeInfo?,
        focusedNode: AccessibilityNodeInfo?,
        backward: Boolean,
    ): Boolean {
        val direction = if (backward) View.FOCUS_BACKWARD else View.FOCUS_FORWARD
        val next = focusedNode?.focusSearch(direction) ?: root?.focusSearch(direction)
        if (next != null) {
            if (next.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) return true
            if (next.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)) return true
        }
        if (root == null) return false
        val focusables = mutableListOf<AccessibilityNodeInfo>()
        collectFocusableNodes(root, focusables, maxNodes = 80)
        if (focusables.isEmpty()) return false
        val currentIdx = focusables.indexOfFirst { it.isFocused }
        val nextIdx = when {
            currentIdx < 0 -> if (backward) focusables.lastIndex else 0
            backward -> (currentIdx - 1 + focusables.size) % focusables.size
            else -> (currentIdx + 1) % focusables.size
        }
        return focusables[nextIdx].performAction(AccessibilityNodeInfo.ACTION_FOCUS)
    }

    private fun collectFocusableNodes(
        node: AccessibilityNodeInfo,
        out: MutableList<AccessibilityNodeInfo>,
        maxNodes: Int,
    ) {
        if (out.size >= maxNodes) return
        if (node.isFocusable && node.isVisibleToUser && node.isEnabled) {
            out += node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectFocusableNodes(child, out, maxNodes)
        }
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
        if (node.isScrollable && node.isVisibleToUser) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val match = findScrollableNode(child)
            if (match != null) return match
        }
        return null
    }

    private fun dispatchScrollGesture(forward: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val dm = resources.displayMetrics
        val x = dm.widthPixels * 0.5f
        val startY = dm.heightPixels * (if (forward) 0.52f else 0.28f)
        val endY = dm.heightPixels * (if (forward) 0.26f else 0.54f)
        val path = Path().apply {
            moveTo(x, startY)
            lineTo(x, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 180L)
        return dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    private fun dispatchMediaKey(mediaKeyCode: Int) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val now = SystemClock.uptimeMillis()
        audioManager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, mediaKeyCode, 0))
        audioManager.dispatchMediaKeyEvent(KeyEvent(now, now + 10, KeyEvent.ACTION_UP, mediaKeyCode, 0))
    }
}
