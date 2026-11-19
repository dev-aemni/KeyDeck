package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.data.KeyDeckRepository
import com.example.data.KeyDeckRuntimeState
import com.example.keyboard.KeyDeckSettings
import com.example.keyboard.KeyDispatcher
import com.example.keyboard.KeyboardDockAlignment
import com.example.keyboard.PcKeyboardDeck
import com.example.keyboard.PcKeyboardLayoutData
import com.example.keyboard.ResolvedKeyboardPalette
import com.example.ui.theme.KeyDeckTheme
import com.example.ui.theme.NeuEmerald
import com.example.ui.theme.neumorphicInset
import com.example.ui.theme.neumorphicRaised
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

class FloatingKeyboardService : Service() {

    companion object {
        const val ACTION_START = "com.example.action.START_FLOATING"
        const val ACTION_START_AND_EXPAND = "com.example.action.START_AND_EXPAND"
        const val ACTION_TOGGLE_KEYBOARD = "com.example.action.TOGGLE_KEYBOARD"
        const val ACTION_STOP = "com.example.action.STOP_FLOATING"

        private const val CHANNEL_ID = "keydeck_floating_channel"
        private const val NOTIFICATION_ID = 4102
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private lateinit var repository: KeyDeckRepository
    private val lifecycleOwner = OverlayLifecycleOwner()

    private var bubbleView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var keyboardView: View? = null
    private var keyboardParams: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        repository = KeyDeckRepository(applicationContext)
        lifecycleOwner.onCreate()
        lifecycleOwner.onResume()

        createNotificationChannel()
        val startedFg = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
        }.isSuccess

        if (!startedFg || !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        KeyDeckRuntimeState.setOverlayServiceRunning(true)
        attachBubbleOverlay()
        observeExpansionState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopAllAndQuit()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_KEYBOARD -> {
                KeyDeckRuntimeState.toggleKeyboardOverlayExpanded()
            }
            ACTION_START_AND_EXPAND -> {
                KeyDeckRuntimeState.setKeyboardOverlayExpanded(true)
            }
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopAllAndQuit()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        removeKeyboardOverlay()
        removeBubbleOverlay()
        KeyDeckRuntimeState.setOverlayServiceRunning(false)
        lifecycleOwner.onPause()
        lifecycleOwner.onDestroy()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun stopAllAndQuit() {
        KeyDeckRuntimeState.setKeyboardOverlayExpanded(false)
        KeyDeckRuntimeState.setOverlayServiceRunning(false)
        removeKeyboardOverlay()
        removeBubbleOverlay()
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    private fun observeExpansionState() {
        serviceScope.launch {
            KeyDeckRuntimeState.isKeyboardOverlayExpanded.collectLatest { expanded ->
                if (expanded) {
                    attachKeyboardOverlay()
                } else {
                    removeKeyboardOverlay()
                }
            }
        }
    }

    private fun overlayType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun attachBubbleOverlay() {
        if (bubbleView != null) return

        val metrics = resources.displayMetrics
        val maxW = metrics.widthPixels
        val maxH = metrics.heightPixels

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = (maxH * 0.35f).roundToInt()
        }
        bubbleParams = params

        val view = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setContent {
                val settings by repository.settingsFlow.collectAsStateWithLifecycle(initialValue = KeyDeckSettings())
                val expanded by KeyDeckRuntimeState.isKeyboardOverlayExpanded.collectAsStateWithLifecycle()
                val palette = settings.resolvePalette()
                KeyDeckTheme(darkTheme = false) {
                    FloatingTriggerBubble(
                        sizeDp = settings.bubbleSizeDp,
                        palette = palette,
                        expanded = expanded,
                        onDragDelta = { dx, dy ->
                            val p = bubbleParams ?: return@FloatingTriggerBubble
                            p.x = (p.x + dx.roundToInt()).coerceIn(0, (maxW - 120).coerceAtLeast(0))
                            p.y = (p.y + dy.roundToInt()).coerceIn(48, (maxH - 180).coerceAtLeast(48))
                            runCatching { windowManager.updateViewLayout(this, p) }
                        },
                        onClick = {
                            KeyDeckRuntimeState.toggleKeyboardOverlayExpanded()
                        },
                    )
                }
            }
        }
        bubbleView = view
        runCatching { windowManager.addView(view, params) }
    }

    private fun removeBubbleOverlay() {
        bubbleView?.let { v ->
            runCatching { windowManager.removeView(v) }
        }
        bubbleView = null
    }

    private fun attachKeyboardOverlay() {
        if (keyboardView != null || !Settings.canDrawOverlays(this)) return

        val metrics = resources.displayMetrics
        val maxH = metrics.heightPixels

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 28
        }
        keyboardParams = params

        val view = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setContent {
                val settings by repository.settingsFlow.collectAsStateWithLifecycle(initialValue = KeyDeckSettings())
                val hotkeys by repository.hotkeysFlow.collectAsStateWithLifecycle(initialValue = PcKeyboardLayoutData.defaultHotkeys)
                val lastKey by KeyDeckRuntimeState.lastDispatchedKey.collectAsStateWithLifecycle()
                val palette = settings.resolvePalette()
                val dockAlign = when (settings.dockAlignment) {
                    KeyboardDockAlignment.LEFT -> Alignment.CenterStart
                    KeyboardDockAlignment.CENTER -> Alignment.Center
                    KeyboardDockAlignment.RIGHT -> Alignment.CenterEnd
                }
                val outerCorner = (settings.keyCornerRadiusDp + 10).coerceIn(14, 28).dp

                KeyDeckTheme(darkTheme = false) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        contentAlignment = dockAlign,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(settings.keyboardWidthFraction.coerceIn(0.60f, 1.0f))
                                .neumorphicRaised(
                                    surfaceColor = palette.deckBg.copy(alpha = settings.overlayOpacity),
                                    shadowLight = palette.shadowLight,
                                    shadowDark = palette.shadowDark,
                                    cornerRadius = outerCorner,
                                    shadowOffset = 6.dp,
                                    blurRadius = 12.dp,
                                ),
                        ) {
                            // Soft UI drag handle + live key badge + quick hide/stop pills
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val p = keyboardParams ?: return@detectDragGestures
                                            p.y = (p.y - dragAmount.y.roundToInt()).coerceIn(0, (maxH * 0.65f).roundToInt())
                                            runCatching { windowManager.updateViewLayout(this@apply, p) }
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "Drag keyboard vertically",
                                        tint = palette.secondaryText,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = lastKey?.let { "${it.comboLabel} • ${it.route}" } ?: "Soft UI KeyDeck",
                                        color = palette.secondaryText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .neumorphicRaised(
                                                surfaceColor = palette.keyBg,
                                                shadowLight = palette.shadowLight,
                                                shadowDark = palette.shadowDark,
                                                cornerRadius = 12.dp,
                                                shadowOffset = 2.dp,
                                                blurRadius = 4.dp,
                                            )
                                            .clickable { KeyDeckRuntimeState.setKeyboardOverlayExpanded(false) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardHide,
                                            contentDescription = "Minimize to floating icon",
                                            tint = palette.primaryText,
                                            modifier = Modifier.size(13.dp),
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .neumorphicInset(
                                                surfaceColor = palette.activeFill,
                                                shadowLight = palette.shadowLight,
                                                shadowDark = palette.shadowDark,
                                                cornerRadius = 12.dp,
                                                depth = 2.dp,
                                            )
                                            .clickable { stopAllAndQuit() },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Turn off floating keyboard",
                                            tint = palette.accentBg,
                                            modifier = Modifier.size(13.dp),
                                        )
                                    }
                                }
                            }

                            PcKeyboardDeck(
                                settings = settings,
                                hotkeys = hotkeys,
                                routeLabel = KeyDispatcher.activeRouteLabel(),
                                isOverlay = true,
                                onKeyDispatch = { keyCode, metaState, charToCommit, comboLabel ->
                                    KeyDispatcher.dispatchKey(
                                        context = this@FloatingKeyboardService,
                                        keyCode = keyCode,
                                        metaState = metaState,
                                        charToCommit = charToCommit,
                                        comboLabel = comboLabel,
                                        hapticEnabled = settings.hapticFeedback,
                                    )
                                },
                                onHotkeyDispatch = { hotkey ->
                                    KeyDispatcher.dispatchHotkey(
                                        context = this@FloatingKeyboardService,
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
                                    KeyDeckRuntimeState.setKeyboardOverlayExpanded(false)
                                },
                            )
                        }
                    }
                }
            }
        }
        keyboardView = view
        runCatching { windowManager.addView(view, params) }
    }

    private fun removeKeyboardOverlay() {
        keyboardView?.let { v ->
            runCatching { windowManager.removeView(v) }
        }
        keyboardView = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "KeyDeck Floating Keyboard",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Keeps the floating PC keyboard trigger active while KeyDeck is open"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, FloatingKeyboardService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.fgs_notification_title))
            .setContentText(getString(R.string.fgs_notification_text))
            .setContentIntent(openAppIntent)
            .addAction(0, getString(R.string.fgs_action_stop), stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}

@Composable
private fun FloatingTriggerBubble(
    sizeDp: Int,
    palette: ResolvedKeyboardPalette,
    expanded: Boolean,
    onDragDelta: (Float, Float) -> Unit,
    onClick: () -> Unit,
) {
    val clampedSize = sizeDp.coerceIn(36, 68).dp
    val touchSlop = LocalViewConfiguration.current.touchSlop

    Box(
        modifier = Modifier
            .padding(6.dp)
            .size(clampedSize)
            .then(
                if (expanded) {
                    Modifier.neumorphicInset(
                        surfaceColor = palette.activeFill,
                        shadowLight = palette.shadowLight,
                        shadowDark = palette.shadowDark,
                        cornerRadius = clampedSize / 2,
                        depth = 4.dp,
                    )
                } else {
                    Modifier.neumorphicRaised(
                        surfaceColor = palette.deckBg,
                        shadowLight = palette.shadowLight,
                        shadowDark = palette.shadowDark,
                        cornerRadius = clampedSize / 2,
                        shadowOffset = 4.dp,
                        blurRadius = 8.dp,
                    )
                },
            )
            .pointerInput(touchSlop) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var dragged = false
                    var totalDx = 0f
                    var totalDy = 0f
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (!dragged) {
                                onClick()
                            }
                            break
                        }
                        val delta = change.positionChange()
                        totalDx += delta.x
                        totalDy += delta.y
                        if (!dragged && hypot(totalDx.toDouble(), totalDy.toDouble()) > touchSlop) {
                            dragged = true
                        }
                        if (dragged && (delta.x != 0f || delta.y != 0f)) {
                            change.consume()
                            onDragDelta(delta.x, delta.y)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(targetState = expanded, label = "bubble_icon") { isOpen ->
            Icon(
                imageVector = if (isOpen) Icons.Default.KeyboardHide else Icons.Default.Keyboard,
                contentDescription = if (isOpen) "Close floating PC keyboard" else "Open floating PC keyboard",
                tint = if (isOpen) palette.accentBg else palette.primaryText,
                modifier = Modifier.size(clampedSize * 0.48f),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(7.dp)
                .clip(CircleShape)
                .background(if (expanded) palette.accentBg else NeuEmerald),
        )
    }
}
