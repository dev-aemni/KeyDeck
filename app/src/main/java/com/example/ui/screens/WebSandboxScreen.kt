package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.KeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.keyboard.CustomHotkey
import com.example.keyboard.KeyDeckProfileCodec
import com.example.keyboard.KeyDeckSettings
import com.example.keyboard.KeyDispatcher
import com.example.keyboard.KeyboardWallpaper
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.KeyDeckColors
import com.example.ui.theme.neumorphicInset
import com.example.ui.theme.neumorphicRaised
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val PLAYGROUND_URL = "playground://shortcuts"

@Composable
fun WebSandboxScreen(
    settings: KeyDeckSettings,
    hotkeys: List<CustomHotkey>,
    activeWallpaper: KeyboardWallpaper?,
    isOverlayRunning: Boolean,
    @Suppress("UNUSED_PARAMETER") suppressSystemKeyboard: Boolean,
    onLaunchFloatingOverWeb: () -> Unit,
    onExportProfileToDownloads: (profileName: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onExportProfileToUri: (Uri, profileName: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onImportProfileFromUri: (Uri, onResult: (Boolean, String) -> Unit) -> Unit,
    openDocsSignal: Int = 0,
    modifier: Modifier = Modifier,
) {
    val colors = KeyDeckColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val outerScrollState = rememberScrollState()
    var docsSectionOffsetY by remember { mutableIntStateOf(0) }

    LaunchedEffect(openDocsSignal, docsSectionOffsetY) {
        if (openDocsSignal > 0 && docsSectionOffsetY > 0) {
            outerScrollState.animateScrollTo(docsSectionOffsetY)
        }
    }

    var urlInput by remember { mutableStateOf(PLAYGROUND_URL) }
    var lastUrlNotice by remember { mutableStateOf<String?>(null) }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportNameInput by remember { mutableStateOf("My KeyDeck") }
    var profileStatusMessage by remember { mutableStateOf<String?>(null) }
    var profileStatusSuccess by remember { mutableStateOf(true) }

    // Native Compose Playground state (replaces crash-prone Chromium WebView process)
    var cursorX by remember { mutableFloatStateOf(0.50f) }
    var cursorY by remember { mutableFloatStateOf(0.50f) }
    var lmbCount by remember { mutableIntStateOf(0) }
    var rmbCount by remember { mutableIntStateOf(0) }
    var numpadDigit by remember { mutableStateOf("5") }
    var numpadProgress by remember { mutableFloatStateOf(0.50f) }
    var heldPills by remember { mutableStateOf(emptySet<String>()) }
    val eventLog = remember { mutableStateListOf("Deck Lab ready • Open Float Deck to test keys & mouse.") }

    fun pushLog(line: String) {
        eventLog.add(0, line)
        while (eventLog.size > 16) {
            eventLog.removeAt(eventLog.lastIndex)
        }
    }

    fun resetPlayground() {
        urlInput = PLAYGROUND_URL
        lastUrlNotice = null
        cursorX = 0.50f
        cursorY = 0.50f
        lmbCount = 0
        rmbCount = 0
        numpadDigit = "5"
        numpadProgress = 0.50f
        heldPills = emptySet()
        eventLog.clear()
        eventLog.add("Deck Lab reset • Ready for key & mouse input.")
    }

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

    DisposableEffect(Unit) {
        val target = object : KeyDispatcher.DirectKeyTarget {
            override fun onDirectKeyEvent(keyCode: Int, metaState: Int, charToCommit: Char?): Boolean {
                val pillKey = resolvePlaygroundPill(keyCode, charToCommit)
                if (pillKey != null) {
                    heldPills = heldPills + pillKey
                    scope.launch {
                        delay(160L)
                        heldPills = heldPills - pillKey
                    }
                }
                val digit = resolveDigitChar(keyCode, charToCommit)
                if (digit != null) {
                    numpadDigit = digit.toString()
                    numpadProgress = (digit.digitToIntOrNull() ?: 5) / 10f
                }
                val label = formatPlaygroundKeyLabel(keyCode, metaState, charToCommit)
                pushLog("KEY  $label (code=$keyCode)")
                return true
            }

            override fun onDirectKeyHold(keyCode: Int, metaState: Int, isDown: Boolean): Boolean {
                val pillKey = resolvePlaygroundPill(keyCode, null)
                if (pillKey != null) {
                    heldPills = if (isDown) heldPills + pillKey else heldPills - pillKey
                }
                if (isDown) {
                    val label = formatPlaygroundKeyLabel(keyCode, metaState, null)
                    pushLog("HOLD $label")
                }
                return true
            }
        }
        val mouseTarget = object : KeyDispatcher.DirectMouseTarget {
            override fun onMouseDelta(dx: Float, dy: Float) {
                cursorX = (cursorX + dx * 0.0025f).coerceIn(0.05f, 0.95f)
                cursorY = (cursorY + dy * 0.0035f).coerceIn(0.08f, 0.92f)
            }

            override fun onMouseClick(isRightClick: Boolean) {
                if (isRightClick) rmbCount++ else lmbCount++
                val btn = if (isRightClick) "RMB (Use)" else "LMB (Click)"
                pushLog("$btn at (${(cursorX * 100).roundToInt()}%, ${(cursorY * 100).roundToInt()}%)")
            }
        }
        KeyDispatcher.setDirectTarget(target)
        KeyDispatcher.setDirectMouseTarget(mouseTarget)
        onDispose {
            KeyDispatcher.setDirectTarget(null)
            KeyDispatcher.setDirectMouseTarget(null)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(outerScrollState)
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
                    text = "Export/import .kdb backups & test keys/mouse",
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

        // Export / Import .kdb Profile Backup Card
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
                        text = "Keyboard Profile (.kdb)",
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
                    text = "v${KeyDeckProfileCodec.CURRENT_FORMAT_VERSION}",
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
                            text = "Export .kdb",
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
                            text = "Import .kdb",
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

        // URL / Playground Action Row (opens external browser for http/https without embedded WebView crash risk)
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
                        val trimmed = urlInput.trim()
                        if (trimmed.isEmpty() || trimmed.startsWith("playground://")) {
                            resetPlayground()
                        } else {
                            val normalized = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                                trimmed
                            } else {
                                "https://$trimmed"
                            }
                            val launched = runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    },
                                )
                                true
                            }.getOrDefault(false)
                            lastUrlNotice = if (launched) {
                                "Opened $normalized in browser"
                            } else {
                                "No browser app found for $normalized"
                            }
                        }
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
                    .clickable { resetPlayground() }
                    .testTag("sandbox_reset_button"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset shortcut playground", tint = colors.textPrimary)
            }
        }

        if (lastUrlNotice != null) {
            Text(
                text = lastUrlNotice.orEmpty(),
                color = colors.indigo,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        // Native Compose Interactive Key & Mouse Playground (zero Chromium child processes)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphicRaised(colors.bg, colors.shadowLight, colors.shadowDark, 20.dp, 5.dp, 9.dp, colors.keyBorder.copy(alpha = 0.45f))
                .padding(14.dp)
                .testTag("sandbox_native_playground"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.activeFill)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "Live Key & Mouse Playground",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.indigo,
                    )
                }
                Text(
                    text = "LMB: $lmbCount • RMB: $rmbCount",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                )
            }

            // Mouse Touchpad Arena
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .neumorphicInset(
                        surfaceColor = colors.surface,
                        shadowLight = colors.shadowLight,
                        shadowDark = colors.shadowDark,
                        cornerRadius = 14.dp,
                        depth = 3.dp,
                        borderColor = colors.keyBorder.copy(alpha = 0.35f),
                    ),
            ) {
                val accent = colors.indigo
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val px = size.width * cursorX
                    val py = size.height * cursorY
                    drawCircle(
                        color = accent.copy(alpha = 0.22f),
                        radius = 12.dp.toPx(),
                        center = Offset(px, py),
                    )
                    drawCircle(
                        color = accent,
                        radius = 7.dp.toPx(),
                        center = Offset(px, py),
                        style = Stroke(width = 2.2.dp.toPx()),
                    )
                }
                Text(
                    text = "Pointer (${(cursorX * 100).roundToInt()}%, ${(cursorY * 100).roundToInt()}%)",
                    fontSize = 10.5.sp,
                    color = colors.textSecondary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                )
            }

            // Key & Numpad State
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("W", "A", "S", "D", "Space", "Shift").forEach { keyName ->
                    val isActive = keyName in heldPills
                    Box(
                        modifier = Modifier
                            .weight(if (keyName.length > 1) 1.3f else 1f)
                            .height(34.dp)
                            .then(
                                if (isActive) {
                                    Modifier.neumorphicInset(
                                        surfaceColor = colors.activeFill,
                                        shadowLight = colors.shadowLight,
                                        shadowDark = colors.shadowDark,
                                        cornerRadius = 9.dp,
                                        depth = 2.dp,
                                        borderColor = colors.indigo.copy(alpha = 0.6f),
                                    )
                                } else {
                                    Modifier.neumorphicRaised(
                                        surfaceColor = colors.surface,
                                        shadowLight = colors.shadowLight,
                                        shadowDark = colors.shadowDark,
                                        cornerRadius = 9.dp,
                                        shadowOffset = 2.dp,
                                        blurRadius = 3.dp,
                                        borderColor = colors.keyBorder.copy(alpha = 0.3f),
                                    )
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = keyName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) colors.indigo else colors.textPrimary,
                        )
                    }
                }
            }

            // Numpad Digit Meter
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Numpad / Digit Gauge", fontSize = 11.sp, color = colors.textSecondary)
                    Text(numpadDigit, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colors.indigo)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.shadowDark.copy(alpha = 0.25f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(numpadProgress.coerceIn(0.05f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.indigo),
                    )
                }
            }

            // Event Log Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphicInset(
                        surfaceColor = colors.surface,
                        shadowLight = colors.shadowLight,
                        shadowDark = colors.shadowDark,
                        cornerRadius = 12.dp,
                        depth = 2.dp,
                        borderColor = colors.keyBorder.copy(alpha = 0.30f),
                    )
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                eventLog.take(6).forEach { entry ->
                    Text(
                        text = entry,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        KeyDeckDocsSection(
            forceExpandSignal = openDocsSignal,
            modifier = Modifier.onGloballyPositioned { coords ->
                docsSectionOffsetY = coords.positionInParent().y.roundToInt()
            },
        )
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
                            text = "@version:${KeyDeckProfileCodec.CURRENT_FORMAT_VERSION} • Height ${settings.keyHeightDp}dp • Width ${(settings.keyboardWidthFraction * 100).roundToInt()}%",
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

private fun resolvePlaygroundPill(keyCode: Int, ch: Char?): String? {
    return when {
        keyCode == KeyEvent.KEYCODE_W || ch?.lowercaseChar() == 'w' -> "W"
        keyCode == KeyEvent.KEYCODE_A || ch?.lowercaseChar() == 'a' -> "A"
        keyCode == KeyEvent.KEYCODE_S || ch?.lowercaseChar() == 's' -> "S"
        keyCode == KeyEvent.KEYCODE_D || ch?.lowercaseChar() == 'd' -> "D"
        keyCode == KeyEvent.KEYCODE_SPACE || ch == ' ' -> "Space"
        keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT -> "Shift"
        else -> null
    }
}

private fun resolveDigitChar(keyCode: Int, ch: Char?): Char? {
    if (ch != null && ch in '0'..'9') return ch
    return when (keyCode) {
        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> ('0'.code + (keyCode - KeyEvent.KEYCODE_0)).toChar()
        in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> ('0'.code + (keyCode - KeyEvent.KEYCODE_NUMPAD_0)).toChar()
        else -> null
    }
}

private fun formatPlaygroundKeyLabel(keyCode: Int, metaState: Int, ch: Char?): String {
    val mods = buildList {
        if ((metaState and KeyEvent.META_CTRL_ON) != 0) add("Ctrl")
        if ((metaState and KeyEvent.META_ALT_ON) != 0) add("Alt")
        if ((metaState and KeyEvent.META_SHIFT_ON) != 0) add("Shift")
        if ((metaState and KeyEvent.META_META_ON) != 0) add("Win")
    }
    val keyLabel = ch?.let { if (it == ' ') "Space" else it.toString() }
        ?: KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
    return (mods + keyLabel).joinToString("+")
}
