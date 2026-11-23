package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.keyboard.CustomHotkey
import com.example.keyboard.KeyDeckProfileCodec
import com.example.keyboard.KeyDeckSettings
import com.example.keyboard.KeyDispatcher
import com.example.keyboard.KeyboardWallpaper
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.KeyDeckColors
import com.example.ui.theme.neumorphicInset
import com.example.ui.theme.neumorphicRaised
import kotlin.math.roundToInt

private const val PLAYGROUND_URL = "playground://shortcuts"
private const val PLAYGROUND_BASE_URL = "https://keydeck.local/"
private const val MAX_AUTO_RENDERER_RECOVERIES = 3

private class SandboxWebView(context: Context) : WebView(context) {
    var suppressSystemKeyboard: Boolean = true
    @Volatile
    var isDisposed: Boolean = false

    override fun onCheckIsTextEditor(): Boolean {
        return if (suppressSystemKeyboard) false else super.onCheckIsTextEditor()
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        return if (suppressSystemKeyboard) null else super.onCreateInputConnection(outAttrs)
    }

    fun disposeSafely() {
        if (isDisposed) return
        isDisposed = true
        runCatching {
            stopLoading()
            onPause()
            (parent as? ViewGroup)?.removeView(this)
            webViewClient = WebViewClient()
            removeAllViews()
            destroy()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebSandboxScreen(
    settings: KeyDeckSettings,
    hotkeys: List<CustomHotkey>,
    activeWallpaper: KeyboardWallpaper?,
    isOverlayRunning: Boolean,
    suppressSystemKeyboard: Boolean,
    onLaunchFloatingOverWeb: () -> Unit,
    onExportProfileToDownloads: (profileName: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onExportProfileToUri: (Uri, profileName: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onImportProfileFromUri: (Uri, onResult: (Boolean, String) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KeyDeckColors.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var urlInput by remember { mutableStateOf(PLAYGROUND_URL) }
    var activeTargetUrl by remember { mutableStateOf(PLAYGROUND_URL) }
    var webViewRef by remember { mutableStateOf<SandboxWebView?>(null) }
    var webViewEpoch by remember { mutableIntStateOf(0) }
    var consecutiveCrashes by remember { mutableIntStateOf(0) }
    var useSoftwareLayerFallback by remember { mutableStateOf(false) }
    var isPendingRecovery by remember { mutableStateOf(false) }
    var isPageLoaded by remember { mutableStateOf(false) }
    var lastLoadError by remember { mutableStateOf<String?>(null) }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportNameInput by remember { mutableStateOf("My KeyDeck") }
    var profileStatusMessage by remember { mutableStateOf<String?>(null) }
    var profileStatusSuccess by remember { mutableStateOf(true) }

    val saveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) {
            onExportProfileToUri(uri, exportNameInput) { ok, msg ->
                profileStatusSuccess = ok
                profileStatusMessage = msg
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            onImportProfileFromUri(uri) { ok, msg ->
                profileStatusSuccess = ok
                profileStatusMessage = msg
            }
        }
    }

    fun loadTargetIntoWebView(wv: SandboxWebView, rawTarget: String, dark: Boolean) {
        if (wv.isDisposed) return
        lastLoadError = null
        isPageLoaded = false
        val target = rawTarget.trim()
        if (target.startsWith("playground://") || target.isEmpty()) {
            activeTargetUrl = PLAYGROUND_URL
            wv.loadDataWithBaseURL(
                PLAYGROUND_BASE_URL,
                buildPlaygroundHtml(dark),
                "text/html",
                "UTF-8",
                null,
            )
        } else {
            val normalized = if (target.startsWith("http://") || target.startsWith("https://")) {
                target
            } else {
                "https://$target"
            }
            val scheme = runCatching { Uri.parse(normalized).scheme?.lowercase() }.getOrNull()
            if (scheme == "http" || scheme == "https") {
                activeTargetUrl = normalized
                wv.loadUrl(normalized)
            } else {
                lastLoadError = "Unsupported URL scheme"
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (isPendingRecovery) {
                        isPendingRecovery = false
                        if (consecutiveCrashes < MAX_AUTO_RENDERER_RECOVERIES) {
                            webViewEpoch++
                        }
                    } else {
                        webViewRef?.takeIf { !it.isDisposed }?.let { wv ->
                            runCatching { wv.onResume() }
                        }
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    webViewRef?.takeIf { !it.isDisposed }?.let { wv ->
                        runCatching { wv.onPause() }
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(webViewRef) {
        val wv = webViewRef
        if (wv != null && !wv.isDisposed) {
            val target = object : KeyDispatcher.DirectKeyTarget {
                override fun onDirectKeyEvent(keyCode: Int, metaState: Int, charToCommit: Char?): Boolean {
                    if (wv.isDisposed || !wv.isAttachedToWindow) return false
                    wv.requestFocus()
                    val now = SystemClock.uptimeMillis()
                    val down = KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, metaState)
                    val up = KeyEvent(now, now + 10, KeyEvent.ACTION_UP, keyCode, 0, metaState)
                    wv.dispatchKeyEvent(down)
                    wv.dispatchKeyEvent(up)
                    return true
                }

                override fun onDirectKeyHold(keyCode: Int, metaState: Int, isDown: Boolean): Boolean {
                    if (wv.isDisposed || !wv.isAttachedToWindow) return false
                    wv.requestFocus()
                    val now = SystemClock.uptimeMillis()
                    val action = if (isDown) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP
                    wv.dispatchKeyEvent(KeyEvent(now, now, action, keyCode, 0, metaState))
                    return true
                }
            }
            val mouseTarget = object : KeyDispatcher.DirectMouseTarget {
                override fun onMouseDelta(dx: Float, dy: Float) {
                    if (wv.isDisposed || !wv.isAttachedToWindow) return
                    wv.post {
                        if (!wv.isDisposed && wv.isAttachedToWindow) {
                            wv.evaluateJavascript(
                                "window.onKeyDeckMouseDelta && window.onKeyDeckMouseDelta($dx, $dy);",
                                null,
                            )
                        }
                    }
                }

                override fun onMouseClick(isRightClick: Boolean) {
                    if (wv.isDisposed || !wv.isAttachedToWindow) return
                    wv.post {
                        if (!wv.isDisposed && wv.isAttachedToWindow) {
                            wv.evaluateJavascript(
                                "window.onKeyDeckMouseClick && window.onKeyDeckMouseClick($isRightClick);",
                                null,
                            )
                        }
                    }
                }
            }
            KeyDispatcher.setDirectTarget(target)
            KeyDispatcher.setDirectMouseTarget(mouseTarget)
        } else {
            KeyDispatcher.setDirectTarget(null)
            KeyDispatcher.setDirectMouseTarget(null)
        }
        onDispose {
            KeyDispatcher.setDirectTarget(null)
            KeyDispatcher.setDirectMouseTarget(null)
        }
    }

    val isDark = colors.isDark
    LaunchedEffect(isDark, isPageLoaded, webViewRef) {
        val wv = webViewRef
        if (wv != null && isPageLoaded && !wv.isDisposed && wv.isAttachedToWindow) {
            wv.evaluateJavascript("window.setKeyDeckDarkMode && window.setKeyDeckDarkMode($isDark);", null)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "Deck Lab & Profiles",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary,
                )
                Text(
                    text = "Export/import .keydeck backups & test keys/mouse",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .then(
                        if (isOverlayRunning) {
                            Modifier.neumorphicInset(colors.activeFill, colors.shadowLight, colors.shadowDark, 14.dp, 3.dp, colors.keyBorder.copy(alpha = 0.45f))
                        } else {
                            Modifier.neumorphicRaised(colors.bg, colors.shadowLight, colors.shadowDark, 14.dp, 4.dp, 7.dp, colors.keyBorder.copy(alpha = 0.45f))
                        },
                    )
                    .clickable(onClick = onLaunchFloatingOverWeb)
                    .padding(horizontal = 14.dp)
                    .testTag("sandbox_float_button"),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = if (isOverlayRunning) colors.emerald else colors.indigo,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOverlayRunning) "Toggle Deck" else "Float Deck",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isOverlayRunning) colors.emerald else colors.indigo,
                    )
                }
            }
        }

        // Export / Import .keydeck Profile Backup Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphicRaised(colors.bg, colors.shadowLight, colors.shadowDark, 16.dp, 4.dp, 8.dp, colors.keyBorder.copy(alpha = 0.45f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Keyboard Profile (.keydeck)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "${settings.layoutMode.shortLabel} • ${settings.themeId.displayName} • ${hotkeys.size} shortcuts" +
                            if (activeWallpaper?.isGifWallpaper() == true) " • GIF" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "v112",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.indigo,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .neumorphicRaised(
                            surfaceColor = colors.accentFill,
                            shadowLight = colors.shadowLight,
                            shadowDark = colors.shadowDark,
                            cornerRadius = 12.dp,
                            shadowOffset = 3.dp,
                            blurRadius = 6.dp,
                            borderColor = null,
                        )
                        .clickable { showExportDialog = true }
                        .testTag("profile_export_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Export .keydeck",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .neumorphicRaised(
                            surfaceColor = colors.bg,
                            shadowLight = colors.shadowLight,
                            shadowDark = colors.shadowDark,
                            cornerRadius = 12.dp,
                            shadowOffset = 3.dp,
                            blurRadius = 6.dp,
                            borderColor = colors.keyBorder.copy(alpha = 0.45f),
                        )
                        .clickable {
                            importLauncher.launch(arrayOf("*/*"))
                        }
                        .testTag("profile_import_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = colors.indigo,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Import .keydeck",
                            color = colors.indigo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                        )
                    }
                }
            }

            if (profileStatusMessage != null) {
                Text(
                    text = profileStatusMessage.orEmpty(),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (profileStatusSuccess) colors.emerald else colors.coral,
                    modifier = Modifier.testTag("profile_status_message"),
                )
            }
        }

        // Neumorphic Inset URL / Playground Loader Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .neumorphicInset(colors.bg, colors.shadowLight, colors.shadowDark, 14.dp, 3.dp, colors.keyBorder.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.Public,
                        contentDescription = null,
                        tint = colors.indigo,
                        modifier = Modifier.size(18.dp),
                    )
                    BasicTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = BodyFontFamily,
                            fontSize = 13.sp,
                            color = colors.textPrimary,
                        ),
                        cursorBrush = SolidColor(colors.indigo),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sandbox_url_input"),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .height(48.dp)
                    .neumorphicRaised(colors.bg, colors.shadowLight, colors.shadowDark, 14.dp, 4.dp, 7.dp, colors.keyBorder.copy(alpha = 0.45f))
                    .clickable {
                        if (consecutiveCrashes >= MAX_AUTO_RENDERER_RECOVERIES) {
                            consecutiveCrashes = 0
                            activeTargetUrl = urlInput.trim().ifEmpty { PLAYGROUND_URL }
                            webViewEpoch++
                            return@clickable
                        }
                        val wv = webViewRef ?: return@clickable
                        loadTargetIntoWebView(wv, urlInput, isDark)
                    }
                    .padding(horizontal = 16.dp)
                    .testTag("sandbox_go_button"),
                contentAlignment = Alignment.Center,
            ) {
                Text("Go", color = colors.indigo, fontWeight = FontWeight.Bold)
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .neumorphicRaised(colors.bg, colors.shadowLight, colors.shadowDark, 14.dp, 4.dp, 7.dp, colors.keyBorder.copy(alpha = 0.45f))
                    .clickable {
                        urlInput = PLAYGROUND_URL
                        activeTargetUrl = PLAYGROUND_URL
                        lastLoadError = null
                        if (consecutiveCrashes >= MAX_AUTO_RENDERER_RECOVERIES || webViewRef == null) {
                            consecutiveCrashes = 0
                            webViewEpoch++
                        } else {
                            webViewRef?.let { loadTargetIntoWebView(it, PLAYGROUND_URL, isDark) }
                        }
                    }
                    .testTag("sandbox_reset_button"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset shortcut playground", tint = colors.textPrimary)
            }
        }

        if (lastLoadError != null) {
            Text(
                text = lastLoadError ?: "",
                color = colors.coral,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        // Embedded WebView inside a Soft UI Neumorphic frame
        val suppressIme = suppressSystemKeyboard
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .neumorphicRaised(colors.bg, colors.shadowLight, colors.shadowDark, 20.dp, 5.dp, 9.dp, colors.keyBorder.copy(alpha = 0.45f))
                .padding(6.dp),
        ) {
            if (consecutiveCrashes >= MAX_AUTO_RENDERER_RECOVERIES) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = colors.coral,
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Web renderer stopped unexpectedly",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "KeyDeck caught the WebView renderer exit safely. Tap below to reload the playground.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .neumorphicRaised(colors.bg, colors.shadowLight, colors.shadowDark, 14.dp, 4.dp, 7.dp, colors.keyBorder.copy(alpha = 0.45f))
                            .clickable {
                                urlInput = PLAYGROUND_URL
                                activeTargetUrl = PLAYGROUND_URL
                                consecutiveCrashes = 0
                                useSoftwareLayerFallback = true
                                lastLoadError = null
                                webViewEpoch++
                            }
                            .padding(horizontal = 20.dp)
                            .testTag("sandbox_recover_button"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Reload Deck Lab",
                            color = colors.indigo,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            } else {
                key(webViewEpoch) {
                    AndroidView(
                        factory = { ctx ->
                            SandboxWebView(ctx).apply {
                                this.suppressSystemKeyboard = suppressIme
                                isFocusable = true
                                isFocusableInTouchMode = true
                                if (useSoftwareLayerFallback) {
                                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
                                }
                                getSettings().javaScriptEnabled = true
                                getSettings().domStorageEnabled = true
                                getSettings().allowFileAccess = false
                                getSettings().allowContentAccess = false
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(
                                        view: WebView?,
                                        url: String?,
                                        favicon: android.graphics.Bitmap?,
                                    ) {
                                        isPageLoaded = false
                                        lastLoadError = null
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        val wv = view as? SandboxWebView ?: return
                                        if (wv.isDisposed) return
                                        isPageLoaded = true
                                        consecutiveCrashes = 0
                                        wv.evaluateJavascript(
                                            "window.setKeyDeckDarkMode && window.setKeyDeckDarkMode($isDark);",
                                            null,
                                        )
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                    ): Boolean {
                                        val scheme = request?.url?.scheme?.lowercase()
                                        return scheme != "http" && scheme != "https"
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?,
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            lastLoadError = "Could not load page (${error?.description ?: "network error"})"
                                        }
                                    }

                                    override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: RenderProcessGoneDetail?,
                                    ): Boolean {
                                        val crashed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            detail?.didCrash() == true
                                        } else {
                                            true
                                        }
                                        val deadView = view as? SandboxWebView
                                        if (deadView != null) {
                                            deadView.disposeSafely()
                                        } else if (view != null) {
                                            runCatching {
                                                (view.parent as? ViewGroup)?.removeView(view)
                                                view.stopLoading()
                                                view.destroy()
                                            }
                                        }
                                        if (webViewRef === view) {
                                            webViewRef = null
                                        }
                                        isPageLoaded = false
                                        consecutiveCrashes++
                                        if (crashed || consecutiveCrashes >= 1) {
                                            useSoftwareLayerFallback = true
                                        }
                                        if (consecutiveCrashes >= 2 && activeTargetUrl != PLAYGROUND_URL) {
                                            activeTargetUrl = PLAYGROUND_URL
                                            urlInput = PLAYGROUND_URL
                                        }
                                        if (consecutiveCrashes < MAX_AUTO_RENDERER_RECOVERIES) {
                                            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                                webViewEpoch++
                                            } else {
                                                isPendingRecovery = true
                                            }
                                        }
                                        return true
                                    }
                                }
                                loadTargetIntoWebView(this, activeTargetUrl, isDark)
                                webViewRef = this
                            }
                        },
                        update = { wv ->
                            if (wv.isDisposed) return@AndroidView
                            wv.suppressSystemKeyboard = suppressIme
                            if (suppressIme && wv.isAttachedToWindow) {
                                val imm = wv.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                imm?.hideSoftInputFromWindow(wv.windowToken, 0)
                            }
                        },
                        onRelease = { wv ->
                            wv.disposeSafely()
                            if (webViewRef === wv) {
                                webViewRef = null
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    if (showExportDialog) {
        val targetFileName = KeyDeckProfileCodec.buildFileName(exportNameInput)
        val transpPct = ((1f - settings.overlayOpacity.coerceIn(0.25f, 1.0f)) * 100f).roundToInt().coerceIn(0, 75)
        Dialog(onDismissRequest = { showExportDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphicRaised(
                        surfaceColor = colors.bg,
                        shadowLight = colors.shadowLight,
                        shadowDark = colors.shadowDark,
                        cornerRadius = 20.dp,
                        shadowOffset = 5.dp,
                        blurRadius = 10.dp,
                        borderColor = colors.keyBorder.copy(alpha = 0.55f),
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Export Keyboard Layout",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showExportDialog = false },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close export dialog",
                            tint = colors.textSecondary,
                        )
                    }
                }

                OutlinedTextField(
                    value = exportNameInput,
                    onValueChange = { exportNameInput = it },
                    label = { Text("Keyboard Settings Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_profile_name_input"),
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphicInset(
                            surfaceColor = colors.bg,
                            shadowLight = colors.shadowLight,
                            shadowDark = colors.shadowDark,
                            cornerRadius = 12.dp,
                            depth = 2.dp,
                            borderColor = colors.keyBorder.copy(alpha = 0.35f),
                        )
                        .padding(10.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "File: sdcard/Download/$targetFileName",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.indigo,
                        )
                        Text(
                            text = "@version:112 • Height ${settings.keyHeightDp}dp • Width ${(settings.keyboardWidthFraction * 100).roundToInt()}%",
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                        )
                        Text(
                            text = "Shortcuts: ${hotkeys.size} • Transparency: $transpPct% • GIF: ${activeWallpaper?.isGifWallpaper() == true}",
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .neumorphicRaised(
                                surfaceColor = colors.bg,
                                shadowLight = colors.shadowLight,
                                shadowDark = colors.shadowDark,
                                cornerRadius = 12.dp,
                                shadowOffset = 3.dp,
                                blurRadius = 6.dp,
                                borderColor = colors.keyBorder.copy(alpha = 0.45f),
                            )
                            .clickable {
                                showExportDialog = false
                                saveAsLauncher.launch(targetFileName)
                            }
                            .testTag("export_profile_save_as_button"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Save As...", color = colors.indigo, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.4f)
                            .height(46.dp)
                            .neumorphicRaised(
                                surfaceColor = colors.accentFill,
                                shadowLight = colors.shadowLight,
                                shadowDark = colors.shadowDark,
                                cornerRadius = 12.dp,
                                shadowOffset = 4.dp,
                                blurRadius = 8.dp,
                                borderColor = null,
                            )
                            .clickable {
                                val cleanName = exportNameInput.trim().ifBlank { "My KeyDeck" }
                                showExportDialog = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    onExportProfileToDownloads(cleanName) { ok, msg ->
                                        profileStatusSuccess = ok
                                        profileStatusMessage = msg
                                    }
                                } else {
                                    saveAsLauncher.launch(targetFileName)
                                }
                            }
                            .testTag("export_profile_confirm_button"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Save to Download",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun buildPlaygroundHtml(isDark: Boolean): String = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
<style>
  :root {
    --bg: #d6dce5;
    --surface: #dce1ea;
    --arena: #cfd6e2;
    --shadow-light: #f1f4f9;
    --shadow-dark: #a5aebc;
    --text: #1f2937;
    --muted: #4b5563;
    --accent: #4f46e5;
    --active-bg: #cde0f8;
    --border: rgba(139,149,165,0.65);
  }
  body.dark {
    --bg: #1a1e28;
    --surface: #212633;
    --arena: #151922;
    --shadow-light: #2e3547;
    --shadow-dark: #0d1017;
    --text: #f1f5f9;
    --muted: #94a3b8;
    --accent: #818cf8;
    --active-bg: #233050;
    --border: rgba(62,71,91,0.8);
  }
  body {
    margin: 0; padding: 12px; font-family: system-ui, -apple-system, sans-serif;
    background: var(--bg); color: var(--text);
  }
  .badge {
    display: inline-block; padding: 5px 11px; border-radius: 999px; font-size: 11px; font-weight: 700;
    background: var(--active-bg); color: var(--accent); border: 1px solid var(--border);
  }
  .card {
    background: var(--bg); border-radius: 16px; padding: 12px; margin-top: 10px;
    border: 1px solid var(--border);
    box-shadow: 5px 5px 10px var(--shadow-dark), -5px -5px 10px var(--shadow-light);
  }
  .arena {
    position: relative; height: 110px; border-radius: 12px; margin-top: 8px; overflow: hidden;
    background: var(--arena); border: 1px solid var(--border);
    box-shadow: inset 3px 3px 6px var(--shadow-dark), inset -3px -3px 6px var(--shadow-light);
  }
  .cursor {
    position: absolute; width: 16px; height: 16px; border-radius: 50%;
    border: 2.5px solid var(--accent); background: rgba(129,140,248,0.22);
    transform: translate(-50%, -50%); left: 50%; top: 50%; pointer-events: none;
  }
  .row { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 8px; }
  .pill {
    padding: 6px 10px; border-radius: 9px; background: var(--surface); font-size: 11px; font-weight: 600;
    border: 1px solid var(--border);
  }
  .pill.active { background: var(--active-bg); color: var(--accent); border-color: var(--accent); }
  .stage { display: flex; align-items: center; justify-content: space-between; margin-top: 4px; font-size: 13px; }
  .meter {
    height: 8px; background: var(--arena); border-radius: 99px; overflow: hidden; margin-top: 8px;
  }
  .meter-fill { height: 100%; width: 35%; background: var(--accent); border-radius: 99px; transition: width 0.12s; }
  #log { font-size: 11px; color: var(--muted); max-height: 110px; overflow-y: auto; margin-top: 6px; }
  .evt { padding: 4px 0; border-bottom: 1px solid var(--border); }
  .k { color: var(--accent); font-weight: 700; }
</style>
</head>
<body class="${if (isDark) "dark" else ""}">
  <div class="badge">WEB + GAMING MODE TEST ARENA</div>
  <div class="card">
    <div class="stage">
      <strong id="playerState">WASD &amp; Mouse Aim Arena</strong>
      <span id="clickCounts" style="color:var(--accent);font-weight:700">LMB: 0 • RMB: 0</span>
    </div>
    <div class="arena" id="arena">
      <div class="cursor" id="cursor"></div>
    </div>
    <div class="row">
      <div class="pill" id="k-w">W</div>
      <div class="pill" id="k-a">A</div>
      <div class="pill" id="k-s">S</div>
      <div class="pill" id="k-d">D</div>
      <div class="pill" id="k-space">Space</div>
      <div class="pill" id="k-shift">Shift</div>
      <div class="pill" id="tool-num">Num: <span id="numVal">-</span></div>
    </div>
    <div class="meter"><div id="fill" class="meter-fill"></div></div>
  </div>

  <div class="card">
    <strong>Live Key &amp; Mouse Events</strong>
    <div id="log"><div class="evt">Open Floating Deck → Game mode to test WASD hold &amp; Touchpad...</div></div>
  </div>

<script>
  let cx = 50, cy = 50, lmb = 0, rmb = 0, pos = 35;
  const cursor = document.getElementById('cursor');
  const clicksEl = document.getElementById('clickCounts');
  const log = document.getElementById('log');
  const fillEl = document.getElementById('fill');
  const numVal = document.getElementById('numVal');

  function appendLogEvent(label, detail) {
    const row = document.createElement('div');
    row.className = 'evt';
    const span = document.createElement('span');
    span.className = 'k';
    span.textContent = label;
    row.appendChild(span);
    row.appendChild(document.createTextNode(detail));
    log.prepend(row);
    if (log.children.length > 14) log.removeChild(log.lastChild);
  }

  window.setKeyDeckDarkMode = (dark) => {
    document.body.classList.toggle('dark', !!dark);
  };

  window.onKeyDeckMouseDelta = (dx, dy) => {
    cx = Math.max(5, Math.min(95, cx + dx * 0.25));
    cy = Math.max(8, Math.min(92, cy + dy * 0.35));
    cursor.style.left = cx + '%';
    cursor.style.top = cy + '%';
  };

  window.onKeyDeckMouseClick = (isRight) => {
    if (isRight) rmb++; else lmb++;
    clicksEl.textContent = 'LMB: ' + lmb + ' • RMB: ' + rmb;
    appendLogEvent(
      isRight ? 'RMB (Use/Place)' : 'LMB (Attack/Click)',
      ' at (' + Math.round(cx) + '%, ' + Math.round(cy) + '%)'
    );
  };

  function toggleKeyPill(key, active) {
    const k = (key || '').toLowerCase();
    const map = { w: 'k-w', a: 'k-a', s: 'k-s', d: 'k-d', ' ': 'k-space', shift: 'k-shift' };
    const id = map[k];
    if (id) document.getElementById(id).classList.toggle('active', active);
  }

  window.addEventListener('keydown', (e) => {
    toggleKeyPill(e.key, true);
    if (/^[0-9]$/.test(e.key)) {
      numVal.textContent = e.key;
      pos = parseInt(e.key, 10) * 10;
      fillEl.style.width = pos + '%';
    }
    appendLogEvent('DOWN ' + (e.key || e.code), ' • keyCode=' + e.keyCode);
  });

  window.addEventListener('keyup', (e) => {
    toggleKeyPill(e.key, false);
  });
</script>
</body>
</html>
""".trimIndent()
