package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.InputMethod
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.os.SystemClock
import android.provider.Settings
import android.text.InputType
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.EditorInfo
import androidx.annotation.RequiresApi
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

    private var cursorX = -1f
    private var cursorY = -1f
    private var lastGestureAtMs = 0L
    private var pendingDx = 0f
    private var pendingDy = 0f

    private var lastExternalEditableNode: AccessibilityNodeInfo? = null
    private var lastEditedNodeKey: String = ""
    private var lastContentRefreshMs: Long = 0L
    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()
    private var pendingClipRestoreRunnable: Runnable? = null
    private var savedOriginalClip: ClipData? = null

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

        runCatching {
            val info = serviceInfo
            if (info != null) {
                var updatedFlags = info.flags
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    updatedFlags = updatedFlags and AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON.inv()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    updatedFlags = updatedFlags or
                        AccessibilityServiceInfo.FLAG_INPUT_METHOD_EDITOR or
                        AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                        AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                }
                if (updatedFlags != info.flags) {
                    info.flags = updatedFlags
                    serviceInfo = info
                }
            }
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val type = event.eventType
        val isDirectInputEvent = type == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
            type == AccessibilityEvent.TYPE_VIEW_CLICKED ||
            type == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED ||
            type == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        val isWindowRefreshEvent = type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            type == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
            type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        if (!isDirectInputEvent && !isWindowRefreshEvent) {
            return
        }
        runCatching {
            val src = event.source
            if (src != null) {
                val srcPkg = src.packageName?.toString()
                if (srcPkg == packageName && !KeyDeckRuntimeState.isAppInForeground.value) {
                    return
                }
                if (isDirectInputEvent) {
                    var cur: AccessibilityNodeInfo? = src
                    var hops = 0
                    while (cur != null && hops < 4) {
                        if (isNodeEditableInput(cur) && cur.isVisibleToUser) {
                            lastExternalEditableNode = cur
                            return
                        }
                        cur = cur.parent
                        hops++
                    }
                    (findFocusedEditableInTree(src, maxNodes = 180, requireFocused = true)
                        ?: findFocusedEditableInTree(src, maxNodes = 120, requireFocused = false))?.let {
                        lastExternalEditableNode = it
                        return
                    }
                }
            }
            if (isWindowRefreshEvent) {
                val existingValid = lastExternalEditableNode?.let { cached ->
                    runCatching { cached.refresh() }.getOrDefault(false) &&
                        cached.isVisibleToUser &&
                        cached.isFocused &&
                        isNodeEditableInput(cached)
                } == true
                if (!existingValid) {
                    val now = SystemClock.uptimeMillis()
                    if (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && now - lastContentRefreshMs < 220L) {
                        return
                    }
                    lastContentRefreshMs = now
                    val (root, focused) = findTargetWindowAndFocus()
                    val resolved = focused?.takeIf { isNodeEditableInput(it) }
                        ?: findEditableAncestorOrCached(focused, root)
                    if (resolved != null && isNodeEditableInput(resolved)) {
                        lastExternalEditableNode = resolved
                    }
                }
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

        // Android 13+ (API 33+) AccessibilityInputConnection: works in Chrome, WhatsApp, WebViews, Compose, & rich editors
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            A11yInputBridge.dispatchShortcutOrText(
                service = this,
                keyCode = keyCode,
                metaState = metaState,
                charToCommit = charToCommit,
            )?.let { result ->
                return result
            }
        }

        val (activeRoot, focusedNode) = findTargetWindowAndFocus()
        val editableNode = focusedNode?.takeIf { isNodeEditableInput(it) && it.isVisibleToUser }
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

    private fun isNodeEditableInput(node: AccessibilityNodeInfo): Boolean {
        if (!node.isEnabled) return false
        if (node.isEditable) return true
        return node.actionList.any {
            it.id == AccessibilityNodeInfo.ACTION_SET_TEXT ||
                it.id == AccessibilityNodeInfo.ACTION_PASTE
        }
    }

    private fun findTargetWindowAndFocus(): Pair<AccessibilityNodeInfo?, AccessibilityNodeInfo?> {
        val candidateRoots = mutableListOf<AccessibilityNodeInfo>()
        val windowList = runCatching { windows }.getOrNull().orEmpty()
        windowList
            .filter { win ->
                runCatching {
                    win.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY &&
                        win.type != AccessibilityWindowInfo.TYPE_INPUT_METHOD
                }.getOrDefault(false)
            }
            .sortedWith(
                compareByDescending<AccessibilityWindowInfo> { runCatching { it.isFocused }.getOrDefault(false) }
                    .thenByDescending { runCatching { it.isActive }.getOrDefault(false) }
                    .thenByDescending { runCatching { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }.getOrDefault(false) }
                    .thenByDescending { runCatching { it.layer }.getOrDefault(0) },
            )
            .forEach { win ->
                val root = runCatching { win.root }.getOrNull() ?: return@forEach
                val pkg = runCatching { root.packageName?.toString() }.getOrNull()
                if (pkg == packageName && !KeyDeckRuntimeState.isAppInForeground.value) {
                    return@forEach
                }
                candidateRoots += root
            }

        val activeFallback = runCatching { rootInActiveWindow }.getOrNull()?.takeIf { root ->
            runCatching { root.packageName?.toString() }.getOrNull() != packageName || KeyDeckRuntimeState.isAppInForeground.value
        }
        if (activeFallback != null && candidateRoots.none { it.windowId == activeFallback.windowId }) {
            candidateRoots += activeFallback
        }

        val bestRoot = candidateRoots.firstOrNull()

        // Global input focus lookup first (handles split-screen, PiP, WebViews, & Compose focus scopes cleanly)
        val globalInputFocus = runCatching { findFocus(AccessibilityNodeInfo.FOCUS_INPUT) }.getOrNull()
        if (globalInputFocus != null) {
            val focusPkg = runCatching { globalInputFocus.packageName?.toString() }.getOrNull()
            val allowedPkg = focusPkg != packageName || KeyDeckRuntimeState.isAppInForeground.value
            if (allowedPkg && runCatching { globalInputFocus.isVisibleToUser }.getOrDefault(false)) {
                val matchingRoot = candidateRoots.firstOrNull { it.windowId == globalInputFocus.windowId } ?: bestRoot
                return matchingRoot to globalInputFocus
            }
        }

        for (root in candidateRoots) {
            val inputFocus = runCatching { root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) }.getOrNull()
            if (inputFocus != null && runCatching { inputFocus.isVisibleToUser }.getOrDefault(false)) {
                return root to inputFocus
            }
        }

        for (root in candidateRoots) {
            val focusedEditable = findFocusedEditableInTree(root, maxNodes = 360, requireFocused = true)
            if (focusedEditable != null) {
                return root to focusedEditable
            }
        }

        lastExternalEditableNode?.let { cached ->
            if (runCatching { cached.refresh() }.getOrDefault(false) &&
                cached.isVisibleToUser &&
                isNodeEditableInput(cached)
            ) {
                val cachedPkg = runCatching { cached.packageName?.toString() }.getOrNull()
                if (cachedPkg != packageName || KeyDeckRuntimeState.isAppInForeground.value) {
                    val matchingRoot = candidateRoots.firstOrNull { it.windowId == cached.windowId } ?: bestRoot
                    return matchingRoot to cached
                }
            }
        }

        for (root in candidateRoots) {
            val anyEditable = findFocusedEditableInTree(root, maxNodes = 300, requireFocused = false)
            if (anyEditable != null) {
                return root to anyEditable
            }
        }

        for (root in candidateRoots) {
            val a11yFocus = runCatching { root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY) }.getOrNull()
            if (a11yFocus != null && runCatching { a11yFocus.isVisibleToUser }.getOrDefault(false)) {
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
            if (isNodeEditableInput(cur) && cur.isVisibleToUser) return cur
            cur = runCatching { cur.parent }.getOrNull()
            hops++
        }
        if (focusedNode != null) {
            // Check inside focused WebView/Compose wrapper for a focused editable first,
            // then any visible enabled editable descendant (WebView/Compose often marks wrapper focused instead).
            (findFocusedEditableInTree(focusedNode, maxNodes = 260, requireFocused = true)
                ?: findFocusedEditableInTree(focusedNode, maxNodes = 260, requireFocused = false))?.let {
                return it
            }
        }
        lastExternalEditableNode?.let { cached ->
            if (runCatching { cached.refresh() }.getOrDefault(false) &&
                cached.isVisibleToUser &&
                isNodeEditableInput(cached) &&
                (activeRoot == null || cached.windowId == activeRoot.windowId)
            ) {
                return cached
            }
        }
        if (activeRoot != null) {
            (findFocusedEditableInTree(activeRoot, maxNodes = 360, requireFocused = true)
                ?: findFocusedEditableInTree(activeRoot, maxNodes = 300, requireFocused = false))?.let {
                return it
            }
        }
        return null
    }

    private fun findFocusedEditableInTree(
        rootNode: AccessibilityNodeInfo,
        maxNodes: Int = 320,
        requireFocused: Boolean = true,
    ): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.addLast(rootNode)
        var visited = 0
        while (queue.isNotEmpty() && visited < maxNodes) {
            val node = queue.removeFirst()
            visited++
            val visible = runCatching { node.isVisibleToUser }.getOrDefault(false)
            val editable = runCatching { isNodeEditableInput(node) }.getOrDefault(false)
            val focused = runCatching { node.isFocused }.getOrDefault(false)
            if (visible && editable && (!requireFocused || focused)) {
                return node
            }
            val count = runCatching { node.childCount }.getOrDefault(0).coerceAtMost(120)
            for (i in 0 until count) {
                val child = runCatching { node.getChild(i) }.getOrNull() ?: continue
                queue.addLast(child)
            }
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
        runCatching { node.refresh() }
        if (!node.isFocused) {
            if (!node.performAction(AccessibilityNodeInfo.ACTION_FOCUS) && node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            runCatching { node.refresh() }
        }
        if (node.isPassword) {
            return pasteViaClipboard(node, textToInsert)
        }
        val supportsSetText = node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT }
        val existing = realNodeText(node)
        val (start, end) = selectionRange(node, existing.length)
        val newText = existing.substring(0, start) + textToInsert + existing.substring(end)
        val newCursor = start + textToInsert.length
        pushUndoSnapshot(node, existing)
        if (supportsSetText && setNodeTextAndCursor(node, newText, newCursor)) {
            lastExternalEditableNode = node
            return true
        }
        return pasteViaClipboard(node, textToInsert)
    }

    private fun deleteBackwardAtCursor(node: AccessibilityNodeInfo): Boolean {
        runCatching { node.refresh() }
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
            lastExternalEditableNode = node
            return true
        }
        return pasteViaClipboardBackspaceFallback(node, existing)
    }

    private fun deleteForwardAtCursor(node: AccessibilityNodeInfo): Boolean {
        runCatching { node.refresh() }
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
        pendingClipRestoreRunnable?.let { mainHandler.removeCallbacks(it) }
        val currentClip = runCatching { cm.primaryClip }.getOrNull()
        val isOwnTempClip = currentClip?.description?.label?.toString() == "KeyDeckInput"
        if (!isOwnTempClip && currentClip != null) {
            savedOriginalClip = currentClip
        }
        val clip = ClipData.newPlainText("KeyDeckInput", text).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                description.extras = PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
            }
        }
        val setOk = runCatching { cm.setPrimaryClip(clip) }.isSuccess
        if (!setOk) return false
        val pasted = runCatching { node.performAction(AccessibilityNodeInfo.ACTION_PASTE) }.getOrDefault(false)
        val restoreClip = savedOriginalClip
        val runnable = Runnable {
            if (restoreClip != null) {
                runCatching { cm.setPrimaryClip(restoreClip) }
            }
            savedOriginalClip = null
            pendingClipRestoreRunnable = null
        }
        pendingClipRestoreRunnable = runnable
        mainHandler.postDelayed(runnable, 320L)
        if (pasted) {
            lastExternalEditableNode = node
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

    fun dispatchKeyHoldViaA11y(keyCode: Int, metaState: Int, isDown: Boolean): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return A11yInputBridge.dispatchHoldKeyEvent(this, keyCode, metaState, isDown)
        }
        return false
    }

    fun isSuggestionsSuppressedInput(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            A11yInputBridge.isSuggestionsSuppressed(this)?.let { return it }
        }
        val (activeRoot, focusedNode) = findTargetWindowAndFocus()
        val editableNode = focusedNode?.takeIf { isNodeEditableInput(it) && it.isVisibleToUser }
            ?: findEditableAncestorOrCached(focusedNode, activeRoot)
        return editableNode?.isPassword == true
    }

    fun getSurroundingChars(beforeLen: Int, afterLen: Int): Pair<String, String>? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            A11yInputBridge.getSurroundingChars(this, beforeLen, afterLen)?.let { return it }
        }
        val (activeRoot, focusedNode) = findTargetWindowAndFocus()
        val editableNode = focusedNode?.takeIf { isNodeEditableInput(it) && it.isVisibleToUser }
            ?: findEditableAncestorOrCached(focusedNode, activeRoot)
            ?: return null
        if (editableNode.isPassword) return null
        runCatching { editableNode.refresh() }
        val existing = realNodeText(editableNode)
        val (start, end) = selectionRange(editableNode, existing.length)
        if (start != end) return "" to ""
        val before = existing.substring(0, start).takeLast(beforeLen)
        val after = existing.substring(end).take(afterLen)
        return before to after
    }

    fun replaceTailText(expectedTail: String, replacement: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            A11yInputBridge.replaceTailText(this, expectedTail, replacement)?.let { ok ->
                if (ok) return true
            }
        }
        val (activeRoot, focusedNode) = findTargetWindowAndFocus()
        val editableNode = focusedNode?.takeIf { isNodeEditableInput(it) && it.isVisibleToUser }
            ?: findEditableAncestorOrCached(focusedNode, activeRoot)
            ?: return false
        if (editableNode.isPassword) return false
        runCatching { editableNode.refresh() }
        val supportsSetText = editableNode.actionList.any { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT }
        if (!supportsSetText) return false
        val existing = realNodeText(editableNode)
        val (start, end) = selectionRange(editableNode, existing.length)
        if (start != end) return false
        if (expectedTail.isNotEmpty() && !existing.substring(0, start).endsWith(expectedTail)) {
            return false
        }
        val replaceStart = start - expectedTail.length
        val newText = existing.substring(0, replaceStart) + replacement + existing.substring(end)
        val newCursor = replaceStart + replacement.length
        pushUndoSnapshot(editableNode, existing)
        return if (setNodeTextAndCursor(editableNode, newText, newCursor)) {
            lastExternalEditableNode = editableNode
            true
        } else {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private object A11yInputBridge {
        fun isSuggestionsSuppressed(service: AccessibilityService): Boolean? {
            val im: InputMethod = runCatching { service.inputMethod }.getOrNull() ?: return null
            if (!im.currentInputStarted) return null
            val info = im.currentInputEditorInfo ?: return null
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

        fun getSurroundingChars(
            service: AccessibilityService,
            beforeLen: Int,
            afterLen: Int,
        ): Pair<String, String>? {
            val im: InputMethod = runCatching { service.inputMethod }.getOrNull() ?: return null
            if (!im.currentInputStarted) return null
            val ic = im.currentInputConnection ?: return null
            val info = im.currentInputEditorInfo
            if (info == null || info.inputType == InputType.TYPE_NULL) return null
            val surrounding = runCatching { ic.getSurroundingText(beforeLen, afterLen, 0) }.getOrNull() ?: return null
            val fullText = surrounding.text?.toString() ?: return null
            val selStart = surrounding.selectionStart.coerceIn(0, fullText.length)
            val selEnd = surrounding.selectionEnd.coerceIn(0, fullText.length)
            val before = fullText.substring(0, selStart).takeLast(beforeLen)
            val after = fullText.substring(selEnd).take(afterLen)
            return before to after
        }

        fun replaceTailText(
            service: AccessibilityService,
            expectedTail: String,
            replacement: String,
        ): Boolean? {
            val im: InputMethod = runCatching { service.inputMethod }.getOrNull() ?: return null
            if (!im.currentInputStarted) return null
            val ic = im.currentInputConnection ?: return null
            val info = im.currentInputEditorInfo
            if (!KeyDeckRuntimeState.isAppInForeground.value && info?.packageName == service.packageName) {
                return null
            }
            if (isSuggestionsSuppressed(service) == true) return false
            if (expectedTail.isNotEmpty()) {
                val surrounding = runCatching { ic.getSurroundingText(expectedTail.length, 0, 0) }.getOrNull()
                    ?: return false
                val text = surrounding.text?.toString() ?: return false
                val selStart = surrounding.selectionStart.coerceIn(0, text.length)
                val selEnd = surrounding.selectionEnd.coerceIn(0, text.length)
                if (selStart != selEnd) return false
                val before = text.substring(0, selStart)
                if (!before.endsWith(expectedTail)) return false
                ic.deleteSurroundingText(expectedTail.length, 0)
            }
            ic.commitText(replacement, 1, null)
            return true
        }

        fun dispatchHoldKeyEvent(
            service: AccessibilityService,
            keyCode: Int,
            metaState: Int,
            isDown: Boolean,
        ): Boolean {
            val im: InputMethod = runCatching { service.inputMethod }.getOrNull() ?: return false
            val ic = im.currentInputConnection ?: return false
            val now = SystemClock.uptimeMillis()
            val action = if (isDown) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP
            ic.sendKeyEvent(
                KeyEvent(
                    now,
                    now,
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
            return true
        }

        fun dispatchShortcutOrText(
            service: AccessibilityService,
            keyCode: Int,
            metaState: Int,
            charToCommit: Char?,
        ): String? {
            val im: InputMethod = runCatching { service.inputMethod }.getOrNull() ?: return null
            if (!im.currentInputStarted) return null
            val ic = im.currentInputConnection ?: return null
            val editorInfo = im.currentInputEditorInfo
            if (!KeyDeckRuntimeState.isAppInForeground.value && editorInfo?.packageName == service.packageName) {
                return null
            }
            val isRawNull = editorInfo == null || editorInfo.inputType == InputType.TYPE_NULL

            val isCtrl = (metaState and KeyEvent.META_CTRL_ON) != 0
            val isAlt = (metaState and KeyEvent.META_ALT_ON) != 0
            val isShift = (metaState and KeyEvent.META_SHIFT_ON) != 0
            val isMeta = (metaState and KeyEvent.META_META_ON) != 0

            if (!isRawNull && isCtrl && !isAlt && !isMeta) {
                when (keyCode) {
                    KeyEvent.KEYCODE_C -> {
                        ic.performContextMenuAction(android.R.id.copy)
                        return "Ctrl+C → Copy (A11y IME)"
                    }
                    KeyEvent.KEYCODE_V -> {
                        ic.performContextMenuAction(android.R.id.paste)
                        return "Ctrl+V → Paste (A11y IME)"
                    }
                    KeyEvent.KEYCODE_X -> {
                        ic.performContextMenuAction(android.R.id.cut)
                        return "Ctrl+X → Cut (A11y IME)"
                    }
                    KeyEvent.KEYCODE_A -> {
                        ic.performContextMenuAction(android.R.id.selectAll)
                        return "Ctrl+A → Select All (A11y IME)"
                    }
                    KeyEvent.KEYCODE_Z -> {
                        if (isShift) {
                            ic.performContextMenuAction(android.R.id.redo)
                            return "Ctrl+Shift+Z → Redo (A11y IME)"
                        } else {
                            ic.performContextMenuAction(android.R.id.undo)
                            return "Ctrl+Z → Undo (A11y IME)"
                        }
                    }
                    KeyEvent.KEYCODE_Y -> {
                        ic.performContextMenuAction(android.R.id.redo)
                        return "Ctrl+Y → Redo (A11y IME)"
                    }
                    else -> {
                        sendKeyPair(ic, keyCode, metaState)
                        return "Shortcut sent (A11y IME)"
                    }
                }
            }

            if (isCtrl || isAlt || isMeta) {
                sendKeyPair(ic, keyCode, metaState)
                return "Combo sent (A11y IME)"
            }

            if (keyCode == KeyEvent.KEYCODE_DEL) {
                if (!isRawNull) {
                    val surrounding = runCatching { ic.getSurroundingText(1, 0, 0) }.getOrNull()
                    if (surrounding != null) {
                        if (surrounding.selectionStart != surrounding.selectionEnd) {
                            ic.commitText("", 1, null)
                        } else {
                            ic.deleteSurroundingText(1, 0)
                        }
                        return "Bksp → Deleted (A11y IME)"
                    }
                }
                sendKeyPair(ic, KeyEvent.KEYCODE_DEL, 0)
                return "Bksp → KeyEvent (A11y IME)"
            }

            if (keyCode == KeyEvent.KEYCODE_FORWARD_DEL) {
                if (!isRawNull) {
                    ic.deleteSurroundingText(0, 1)
                } else {
                    sendKeyPair(ic, KeyEvent.KEYCODE_FORWARD_DEL, 0)
                }
                return "Del → Forward delete (A11y IME)"
            }

            if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                val inputType = editorInfo?.inputType ?: 0
                val isMultiLine = (inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0 ||
                    (inputType and InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE) != 0
                val imeAction = (editorInfo?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
                val noEnterAction = ((editorInfo?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
                if (!isRawNull && isMultiLine) {
                    ic.commitText("\n", 1, null)
                    return "Enter → Newline (A11y IME)"
                } else if (!isRawNull && !noEnterAction &&
                    imeAction != EditorInfo.IME_ACTION_NONE &&
                    imeAction != EditorInfo.IME_ACTION_UNSPECIFIED
                ) {
                    ic.performEditorAction(imeAction)
                    return "Enter → Action (A11y IME)"
                } else {
                    sendKeyPair(ic, KeyEvent.KEYCODE_ENTER, 0)
                    return "Enter → Key (A11y IME)"
                }
            }

            if (charToCommit != null && keyCode != KeyEvent.KEYCODE_TAB) {
                if (!isRawNull) {
                    ic.commitText(charToCommit.toString(), 1, null)
                    return "Typed '$charToCommit'"
                }
                val kcm = runCatching { KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD) }.getOrNull()
                val mappedEvents = runCatching { kcm?.getEvents(charArrayOf(charToCommit)) }.getOrNull()
                if (mappedEvents != null && mappedEvents.isNotEmpty()) {
                    mappedEvents.forEach { ev -> ic.sendKeyEvent(ev) }
                } else {
                    ic.commitText(charToCommit.toString(), 1, null)
                }
                return "Typed '$charToCommit'"
            }

            sendKeyPair(ic, keyCode, metaState)
            return "Key $keyCode sent (A11y IME)"
        }

        private fun sendKeyPair(
            ic: InputMethod.AccessibilityInputConnection,
            keyCode: Int,
            metaState: Int,
        ) {
            val now = SystemClock.uptimeMillis()
            ic.sendKeyEvent(
                KeyEvent(
                    now,
                    now,
                    KeyEvent.ACTION_DOWN,
                    keyCode,
                    0,
                    metaState,
                    KeyCharacterMap.VIRTUAL_KEYBOARD,
                    0,
                    KeyEvent.FLAG_KEEP_TOUCH_MODE,
                    InputDevice.SOURCE_KEYBOARD,
                ),
            )
            ic.sendKeyEvent(
                KeyEvent(
                    now,
                    now + 10,
                    KeyEvent.ACTION_UP,
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
    }
}
