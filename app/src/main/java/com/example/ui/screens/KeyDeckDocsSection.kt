package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.keyboard.KeyDeckProfileCodec
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.KeyDeckColors
import com.example.ui.theme.neumorphicInset
import com.example.ui.theme.neumorphicRaised

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyDeckDocsSection(
    forceExpandSignal: Int = 0,
    modifier: Modifier = Modifier,
) {
    val colors = KeyDeckColors.current
    val context = LocalContext.current
    var issuesLaunchNotice by remember { mutableStateOf<String?>(null) }

    val pulseTransition = rememberInfiniteTransition(label = "docs_banner_pulse")
    val ledAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "led_alpha",
    )
    val cursorAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cursor_alpha",
    )

    var expandModes by remember(forceExpandSignal) { mutableStateOf(true) }
    var expandShortcuts by remember(forceExpandSignal) { mutableStateOf(forceExpandSignal > 0) }
    var expandMouseA11y by remember { mutableStateOf(false) }
    var expandDualDelivery by remember { mutableStateOf(false) }
    var expandThemesProfiles by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("deck_lab_docs_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Animated Hero Docs Header matching README banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphicRaised(
                    surfaceColor = colors.bg,
                    shadowLight = colors.shadowLight,
                    shadowDark = colors.shadowDark,
                    cornerRadius = 18.dp,
                    shadowOffset = 4.dp,
                    blurRadius = 8.dp,
                    borderColor = colors.indigo.copy(alpha = 0.45f),
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .neumorphicInset(
                                surfaceColor = colors.activeFill,
                                shadowLight = colors.shadowLight,
                                shadowDark = colors.shadowDark,
                                cornerRadius = 10.dp,
                                depth = 2.dp,
                                borderColor = colors.indigo.copy(alpha = 0.4f),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "KeyDeck Docs",
                            tint = colors.indigo,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        text = "KeyDeck Docs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.activeFill)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .alpha(ledAlpha)
                            .clip(CircleShape)
                            .background(colors.emerald),
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.indigo,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = ">_ System IME • Floating Deck • Mouse Joystick ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.5.sp,
                    color = colors.textSecondary,
                )
                Box(
                    modifier = Modifier
                        .size(width = 7.dp, height = 13.dp)
                        .alpha(cursorAlpha)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(colors.indigo),
                )
            }

            // OTG Mechanical Keyboard Comparison (conversational voice from README)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphicInset(
                        surfaceColor = colors.activeFill.copy(alpha = 0.65f),
                        shadowLight = colors.shadowLight,
                        shadowDark = colors.shadowDark,
                        cornerRadius = 12.dp,
                        depth = 2.dp,
                        borderColor = colors.keyBorder.copy(alpha = 0.35f),
                    )
                    .padding(11.dp),
            ) {
                Text(
                    text = "Our KBD board works the same way as when you connect a mechanical keyboard and mouse to your phone with an OTG. It works and inputs the exact same like that—just still a few things are limited, mostly the real mechanical feel that you won't ever feel on touch glass. Android won't even know whether you connected a physical mechanical keyboard or not; our app sends hardware signals straight to the inputs the same way a mechanical keyboard does, not like other basic mobile keyboard apps that only input plain text.",
                    fontFamily = BodyFontFamily,
                    fontSize = 11.5.sp,
                    lineHeight = 16.5.sp,
                    color = colors.textPrimary,
                )
            }
        }

        // Important Note Quick Checklist inside Docs
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphicRaised(
                    surfaceColor = colors.bg,
                    shadowLight = colors.shadowLight,
                    shadowDark = colors.shadowDark,
                    cornerRadius = 16.dp,
                    shadowOffset = 4.dp,
                    blurRadius = 8.dp,
                    borderColor = colors.keyBorder.copy(alpha = 0.45f),
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Important Note",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
            DocsBullet(colors.emerald, "Enable and select KeyDeck as IME to use the IME (Normal keyboard) version.")
            DocsBullet(colors.indigo, "Enable and select KeyDeck IME and open Floating Keyboard to use it in any area's input.")
            DocsBullet(colors.amber, "Enable Accessibility to use Game mode's Mouse Pad / Joystick on screen.")
            Text(
                text = stringResource(R.string.important_note_audience),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
                color = colors.textSecondary,
            )
        }

        // Expandable Docs Accordion Cards (matching README <details> blocks)
        DocsAccordionCard(
            title = "5 Instant-Switch Modes",
            subtitle = "PC+Num • PC Full • Simple • Num • Game",
            expanded = expandModes,
            onToggle = { expandModes = !expandModes },
        ) {
            DocsBullet(colors.indigo, "PC+Num: 10-column QWERTY + live 4x5 numeric keypad with NumLock toggle.")
            DocsBullet(colors.indigo, "PC (Full): 6-row QWERTY with Esc, F1-F12 row, Tab, Caps, Win, Ctrl, Alt, Fn, Arrows & Hold-to-Repeat.")
            DocsBullet(colors.emerald, "Simple: 5-row Gboard-style typing, accent popups, 3-word context prediction, one-time non-intrusive Auto-Correct & Simple-only haptics.")
            DocsBullet(colors.amber, "Num: Oversized numeric & navigation keypad.")
            DocsBullet(colors.coral, "Game: Hold-down WASD/Q/R/E/F/G/C/Shift/Space + right-hand Mouse Joystick.")
        }

        DocsAccordionCard(
            title = "Hardware OS Shortcuts & Macros",
            subtitle = "Win+Enter • Alt+Tab • Ctrl+C/V/Z • Custom App Launch",
            expanded = expandShortcuts,
            onToggle = { expandShortcuts = !expandShortcuts },
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                DocsKeycapBadge("Win + Enter -> Home")
                DocsKeycapBadge("Win + Bksp -> Back")
                DocsKeycapBadge("Alt + Tab -> Recents")
                DocsKeycapBadge("Ctrl + C / V / X / A / Z")
                DocsKeycapBadge("Ctrl + Alt + F -> Float")
                DocsKeycapBadge("PrtSc -> Screenshot")
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Create custom multi-modifier shortcuts or bind combos directly to launch any installed app.",
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = colors.textSecondary,
            )
        }

        DocsAccordionCard(
            title = "Mouse Joystick & Clean Accessibility",
            subtitle = "MCPE 3D Look • Desktop Pointer • Zero Android 11 Icon",
            expanded = expandMouseA11y,
            onToggle = { expandMouseA11y = !expandMouseA11y },
        ) {
            DocsBullet(colors.indigo, "Hybrid Joystick: Drag for 1:1 precision; hold near the circular rim for continuous 60Hz camera/cursor movement; tap inside deadzone for LMB.")
            DocsBullet(colors.indigo, "MCPE Camera Look Mode: Chained Android 8+ continuous gestures with edge re-centering + LMB click / RMB hold-use.")
            DocsBullet(colors.emerald, "Desktop Cursor Pointer: Renders a live non-touchable mouse pointer aligned with LMB/RMB clicks.")
            DocsBullet(colors.amber, "Clean Android 11 Setup: Accessibility works without adding an intrusive accessibility human icon to the nav bar.")
        }

        DocsAccordionCard(
            title = "Dual Delivery & Smart IME Suppression",
            subtitle = "System IME + Floating Overlay (0%-75% Transparency)",
            expanded = expandDualDelivery,
            onToggle = { expandDualDelivery = !expandDualDelivery },
        ) {
            DocsBullet(colors.indigo, "Works both as your System IME keyboard and as a draggable, corner-resizable Floating Keyboard.")
            DocsBullet(colors.emerald, "Automatically suppresses the bottom system keyboard when the floating deck is open so two keyboards never stack.")
        }

        DocsAccordionCard(
            title = "Themes, GIF Wallpapers & .kdb Profiles",
            subtitle = "12+ Themes • Animated GIF Crop • @version:${KeyDeckProfileCodec.CURRENT_FORMAT_VERSION} Backups",
            expanded = expandThemesProfiles,
            onToggle = { expandThemesProfiles = !expandThemesProfiles },
        ) {
            DocsBullet(colors.indigo, "12+ Neumorphic themes, Custom Color Studio, and RGB Border Modes (Gaming Only, All, Off).")
            DocsBullet(colors.emerald, "7 built-in pattern wallpapers plus custom Image/Animated GIF picker with pan/zoom crop and glass opacity.")
            DocsBullet(colors.amber, "Export full setups—including embedded GIFs and macros—into Download/*.kdb (@version:${KeyDeckProfileCodec.CURRENT_FORMAT_VERSION}).")
        }

        // GitHub Issues Feature Request & Bug Report Card
        val issuesUrl = stringResource(R.string.deck_lab_issues_url)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphicRaised(
                    surfaceColor = colors.bg,
                    shadowLight = colors.shadowLight,
                    shadowDark = colors.shadowDark,
                    cornerRadius = 16.dp,
                    shadowOffset = 4.dp,
                    blurRadius = 8.dp,
                    borderColor = colors.emerald.copy(alpha = 0.45f),
                )
                .padding(14.dp)
                .testTag("deck_lab_github_issues_card"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = stringResource(R.string.deck_lab_issues_title),
                    tint = colors.emerald,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.deck_lab_issues_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .neumorphicRaised(
                        surfaceColor = colors.activeFill,
                        shadowLight = colors.shadowLight,
                        shadowDark = colors.shadowDark,
                        cornerRadius = 12.dp,
                        shadowOffset = 3.dp,
                        blurRadius = 6.dp,
                        borderColor = colors.indigo.copy(alpha = 0.45f),
                    )
                    .clickable {
                        val launched = runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(issuesUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                },
                            )
                            true
                        }.getOrDefault(false)
                        issuesLaunchNotice = if (launched) {
                            "Opened GitHub Issues in browser"
                        } else {
                            "No browser app found to open $issuesUrl"
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("deck_lab_github_issues_button"),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = stringResource(R.string.deck_lab_issues_cta),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.indigo,
                        )
                        Text(
                            text = issuesUrl,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            color = colors.textSecondary,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = stringResource(R.string.deck_lab_issues_cta),
                        tint = colors.indigo,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            if (issuesLaunchNotice != null) {
                Text(
                    text = issuesLaunchNotice.orEmpty(),
                    fontSize = 11.sp,
                    color = colors.indigo,
                )
            }
        }
    }
}

@Composable
private fun DocsAccordionCard(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = KeyDeckColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphicRaised(
                surfaceColor = colors.bg,
                shadowLight = colors.shadowLight,
                shadowDark = colors.shadowDark,
                cornerRadius = 16.dp,
                shadowOffset = 4.dp,
                blurRadius = 7.dp,
                borderColor = colors.keyBorder.copy(alpha = 0.42f),
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = colors.textSecondary,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                tint = colors.indigo,
                modifier = Modifier.size(20.dp),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun DocsBullet(dotColor: Color, text: String) {
    val colors = KeyDeckColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Text(
            text = text,
            fontFamily = BodyFontFamily,
            fontSize = 11.sp,
            lineHeight = 15.5.sp,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DocsKeycapBadge(label: String) {
    val colors = KeyDeckColors.current
    Box(
        modifier = Modifier
            .neumorphicRaised(
                surfaceColor = colors.surface,
                shadowLight = colors.shadowLight,
                shadowDark = colors.shadowDark,
                cornerRadius = 8.dp,
                shadowOffset = 2.dp,
                blurRadius = 3.dp,
                borderColor = colors.keyBorder.copy(alpha = 0.45f),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.indigo,
        )
    }
}
