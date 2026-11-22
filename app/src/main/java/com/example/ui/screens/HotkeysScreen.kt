package com.example.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keyboard.CustomHotkey
import com.example.keyboard.HotkeyActionType
import com.example.keyboard.HotkeyCategory
import com.example.keyboard.KeyDispatcher
import com.example.keyboard.PcKeyboardLayoutData
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.KeyDeckColors
import com.example.ui.theme.neumorphicInset
import com.example.ui.theme.neumorphicRaised
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class LaunchableAppItem(
    val label: String,
    val packageName: String,
)

@Composable
fun HotkeysScreen(
    hotkeys: List<CustomHotkey>,
    hapticEnabled: Boolean,
    onSaveHotkey: (CustomHotkey) -> Unit,
    onDeleteHotkey: (String) -> Unit,
    onMoveHotkeyUp: (String) -> Unit,
    onAddPresetPack: (HotkeyCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = KeyDeckColors.current
    var selectedCategory by remember { mutableStateOf(HotkeyCategory.ALL) }
    var editingHotkey by remember { mutableStateOf<CustomHotkey?>(null) }
    var showEditorDialog by remember { mutableStateOf(false) }

    val filtered = remember(hotkeys, selectedCategory) {
        if (selectedCategory == HotkeyCategory.ALL) hotkeys
        else hotkeys.filter { it.category == selectedCategory }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingHotkey = null
                    showEditorDialog = true
                },
                containerColor = colors.accentFill,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_hotkey"),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create custom hotkey")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            Column {
                Text(
                    text = "Customizable Hotkeys",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary,
                )
                Text(
                    text = "Browse, customize, and test keyboard shortcut macros",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }

            // Neumorphic Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HotkeyCategory.entries.forEach { cat ->
                    val selected = selectedCategory == cat
                    val mod = if (selected) {
                        Modifier.neumorphicInset(
                            surfaceColor = colors.activeFill,
                            shadowLight = colors.shadowLight,
                            shadowDark = colors.shadowDark,
                            cornerRadius = 12.dp,
                            depth = 3.dp,
                            borderColor = colors.keyBorder.copy(alpha = 0.45f),
                        )
                    } else {
                        Modifier.neumorphicRaised(
                            surfaceColor = colors.bg,
                            shadowLight = colors.shadowLight,
                            shadowDark = colors.shadowDark,
                            cornerRadius = 12.dp,
                            shadowOffset = 3.dp,
                            blurRadius = 6.dp,
                            borderColor = colors.keyBorder.copy(alpha = 0.45f),
                        )
                    }
                    Box(
                        modifier = mod
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("hotkey_filter_${cat.name.lowercase()}"),
                    ) {
                        Text(
                            text = cat.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) colors.indigo else colors.textSecondary,
                        )
                    }
                }
            }

            // Quick Preset Packs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PresetPackButton(
                    label = "+ Web & Browser Pack",
                    testTag = "preset_browser_pack",
                    onClick = { onAddPresetPack(HotkeyCategory.BROWSER) },
                )
                PresetPackButton(
                    label = "+ Editor Pack",
                    testTag = "preset_editor_pack",
                    onClick = { onAddPresetPack(HotkeyCategory.EDITING) },
                )
                PresetPackButton(
                    label = "+ Page Nav Pack",
                    testTag = "preset_nav_pack",
                    onClick = { onAddPresetPack(HotkeyCategory.NAVIGATION) },
                )
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = colors.indigo,
                            modifier = Modifier.size(44.dp),
                        )
                        Text(
                            text = "No hotkeys in ${selectedCategory.label}",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = "Tap + to create a custom keyboard shortcut macro",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filtered, key = { it.id }) { hk ->
                        val accent = Color(hk.accentHex)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neumorphicRaised(
                                    surfaceColor = colors.bg,
                                    shadowLight = colors.shadowLight,
                                    shadowDark = colors.shadowDark,
                                    cornerRadius = 18.dp,
                                    shadowOffset = 4.dp,
                                    blurRadius = 8.dp,
                                    borderColor = colors.keyBorder.copy(alpha = 0.45f),
                                )
                                .testTag("hotkey_card_${hk.id}"),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(accent),
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = hk.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colors.textPrimary,
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .neumorphicInset(
                                                        surfaceColor = colors.activeFill,
                                                        shadowLight = colors.shadowLight,
                                                        shadowDark = colors.shadowDark,
                                                        cornerRadius = 8.dp,
                                                        depth = 2.dp,
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                            ) {
                                                Text(
                                                    text = hk.actionSummary(),
                                                    fontFamily = BodyFontFamily,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = accent,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                            Text(
                                                text = when (hk.actionType) {
                                                    HotkeyActionType.LAUNCH_APP -> hk.targetPackageName.ifBlank { hk.category.label }
                                                    HotkeyActionType.TOGGLE_FLOATING -> "Opens Floating Keyboard"
                                                    HotkeyActionType.KEY_COMBO -> hk.category.label
                                                },
                                                fontSize = 11.sp,
                                                color = colors.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            KeyDispatcher.dispatchHotkey(context, hk, hapticEnabled)
                                        },
                                        modifier = Modifier.testTag("hotkey_test_${hk.id}"),
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Test fire ${hk.label}",
                                            tint = colors.emerald,
                                        )
                                    }
                                    IconButton(
                                        onClick = { onMoveHotkeyUp(hk.id) },
                                        modifier = Modifier.testTag("hotkey_move_up_${hk.id}"),
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = "Move ${hk.label} up",
                                            tint = colors.textSecondary,
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            editingHotkey = hk
                                            showEditorDialog = true
                                        },
                                        modifier = Modifier.testTag("hotkey_edit_${hk.id}"),
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit ${hk.label}",
                                            tint = colors.indigo,
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteHotkey(hk.id) },
                                        modifier = Modifier.testTag("hotkey_delete_${hk.id}"),
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete ${hk.label}",
                                            tint = colors.coral,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditorDialog) {
        HotkeyEditorDialog(
            initial = editingHotkey,
            onDismiss = { showEditorDialog = false },
            onConfirm = { saved ->
                onSaveHotkey(saved)
                showEditorDialog = false
            },
        )
    }
}

@Composable
private fun PresetPackButton(
    label: String,
    testTag: String,
    onClick: () -> Unit,
) {
    val colors = KeyDeckColors.current
    Box(
        modifier = Modifier
            .neumorphicRaised(
                surfaceColor = colors.bg,
                shadowLight = colors.shadowLight,
                shadowDark = colors.shadowDark,
                cornerRadius = 10.dp,
                shadowOffset = 3.dp,
                blurRadius = 6.dp,
                borderColor = colors.keyBorder.copy(alpha = 0.45f),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(testTag),
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.indigo)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HotkeyEditorDialog(
    initial: CustomHotkey?,
    onDismiss: () -> Unit,
    onConfirm: (CustomHotkey) -> Unit,
) {
    val context = LocalContext.current
    val colors = KeyDeckColors.current
    var actionType by remember { mutableStateOf(initial?.actionType ?: HotkeyActionType.KEY_COMBO) }
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var targetPackageName by remember { mutableStateOf(initial?.targetPackageName ?: "") }
    var targetAppLabel by remember { mutableStateOf(initial?.targetAppLabel ?: "") }
    var appSearchQuery by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<LaunchableAppItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        installedApps = withContext(Dispatchers.IO) {
            runCatching {
                val pm = context.packageManager
                val mainIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
                } else {
                    @Suppress("DEPRECATION")
                    pm.queryIntentActivities(mainIntent, 0)
                }
                resolved
                    .mapNotNull { info ->
                        val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                        val appName = info.loadLabel(pm)?.toString()?.trim().orEmpty().ifEmpty { pkg }
                        LaunchableAppItem(label = appName, packageName = pkg)
                    }
                    .distinctBy { it.packageName }
                    .sortedBy { it.label.lowercase() }
            }.getOrDefault(emptyList())
        }
    }

    var ctrl by remember { mutableStateOf(initial?.ctrl ?: true) }
    var alt by remember { mutableStateOf(initial?.alt ?: false) }
    var shift by remember { mutableStateOf(initial?.shift ?: false) }
    var meta by remember { mutableStateOf(initial?.meta ?: false) }
    var category by remember { mutableStateOf(initial?.category ?: HotkeyCategory.CUSTOM) }
    var accentHex by remember { mutableLongStateOf(initial?.accentHex ?: 0xFF4F46E5) }

    val initialKey = remember(initial) {
        PcKeyboardLayoutData.selectableHotkeyKeys.find { it.keyCode == initial?.keyCode }
            ?: PcKeyboardLayoutData.selectableHotkeyKeys.first()
    }
    var selectedKey by remember { mutableStateOf(initialKey) }
    var selectedGroup by remember { mutableStateOf(initialKey.group) }

    val filteredApps = remember(installedApps, appSearchQuery) {
        val q = appSearchQuery.trim().lowercase()
        if (q.isEmpty()) installedApps
        else installedApps.filter {
            it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
    }

    val accentSwatches = listOf(
        0xFF4F46E5L,
        0xFF0D9488L,
        0xFFE11D48L,
        0xFFD97706L,
        0xFF7C3AEDL,
        0xFF2563EBL,
    )

    val groups = listOf("Letters", "Numbers", "Numpad", "Function", "Special")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.bg,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textPrimary,
        title = {
            Text(
                text = if (initial == null) "New Custom Shortcut" else "Edit Shortcut",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Action Type", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HotkeyActionType.entries.forEach { type ->
                        val active = actionType == type
                        Box(
                            modifier = (if (active) {
                                Modifier.neumorphicInset(colors.activeFill, colors.shadowLight, colors.shadowDark, 10.dp, 2.dp, colors.keyBorder.copy(alpha = 0.45f))
                            } else {
                                Modifier.neumorphicRaised(colors.bg, colors.shadowLight, colors.shadowDark, 10.dp, 3.dp, 5.dp, colors.keyBorder.copy(alpha = 0.45f))
                            })
                                .weight(1f)
                                .height(38.dp)
                                .clickable {
                                    actionType = type
                                    if (type != HotkeyActionType.KEY_COMBO && initial == null && !alt) {
                                        ctrl = true
                                        alt = true
                                    }
                                    if (type == HotkeyActionType.TOGGLE_FLOATING && label.isBlank()) {
                                        label = "Float Keyboard"
                                    }
                                }
                                .padding(horizontal = 4.dp)
                                .testTag("hotkey_action_${type.name.lowercase()}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(
                                    imageVector = when (type) {
                                        HotkeyActionType.LAUNCH_APP -> Icons.Default.Apps
                                        HotkeyActionType.TOGGLE_FLOATING -> Icons.Default.Layers
                                        HotkeyActionType.KEY_COMBO -> Icons.Default.Bolt
                                    },
                                    contentDescription = null,
                                    tint = if (active) colors.indigo else colors.textSecondary,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = type.label,
                                    fontFamily = BodyFontFamily,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) colors.indigo else colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Shortcut Name") },
                    placeholder = {
                        Text(
                            when (actionType) {
                                HotkeyActionType.LAUNCH_APP -> "e.g. Open Chrome, Open WhatsApp"
                                HotkeyActionType.TOGGLE_FLOATING -> "e.g. Float Keyboard"
                                HotkeyActionType.KEY_COMBO -> "e.g. Hard Reload, Play/Pause"
                            },
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.indigo,
                        unfocusedBorderColor = colors.keyBorder,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedLabelColor = colors.indigo,
                        unfocusedLabelColor = colors.textSecondary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hotkey_name_input"),
                )

                if (actionType == HotkeyActionType.TOGGLE_FLOATING) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neumorphicInset(
                                surfaceColor = colors.activeFill.copy(alpha = 0.7f),
                                shadowLight = colors.shadowLight,
                                shadowDark = colors.shadowDark,
                                cornerRadius = 10.dp,
                                depth = 2.dp,
                                borderColor = colors.keyBorder.copy(alpha = 0.35f),
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = "Pressing this combo on KeyDeck IME (or tapping it in Shortcuts) turns on and pops open the Floating Keyboard.",
                            fontSize = 11.5.sp,
                            color = colors.textPrimary,
                        )
                    }
                }

                if (actionType == HotkeyActionType.LAUNCH_APP) {
                    Text(
                        text = "Select Installed App or Enter Package Name",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary,
                    )
                    OutlinedTextField(
                        value = targetPackageName,
                        onValueChange = {
                            targetPackageName = it
                            if (targetAppLabel.isBlank()) {
                                targetAppLabel = label
                            }
                        },
                        label = { Text("App Package Name") },
                        placeholder = { Text("e.g. com.android.chrome") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.indigo,
                            unfocusedBorderColor = colors.keyBorder,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedLabelColor = colors.indigo,
                            unfocusedLabelColor = colors.textSecondary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hotkey_package_input"),
                    )

                    OutlinedTextField(
                        value = appSearchQuery,
                        onValueChange = { appSearchQuery = it },
                        label = { Text("Search Apps (${installedApps.size})") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.indigo,
                            unfocusedBorderColor = colors.keyBorder,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedLabelColor = colors.indigo,
                            unfocusedLabelColor = colors.textSecondary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hotkey_app_search"),
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                            .neumorphicInset(colors.bg, colors.shadowLight, colors.shadowDark, 10.dp, 2.dp, colors.keyBorder.copy(alpha = 0.35f))
                            .padding(6.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (filteredApps.isEmpty()) {
                            Text(
                                text = "No matching apps found. Type any package name above.",
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(6.dp),
                            )
                        } else {
                            filteredApps.take(40).forEach { app ->
                                val selected = targetPackageName == app.packageName
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                        .background(if (selected) colors.activeFill else Color.Transparent)
                                        .clickable {
                                            targetPackageName = app.packageName
                                            targetAppLabel = app.label
                                            if (label.isBlank()) {
                                                label = app.label
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (selected) colors.indigo else colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = app.packageName,
                                            fontSize = 10.sp,
                                            color = colors.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = colors.indigo,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = if (actionType == HotkeyActionType.LAUNCH_APP) "Trigger Modifiers" else "Modifiers",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ModifierTogglePill("Ctrl", ctrl, Modifier.weight(1f).testTag("mod_ctrl")) { ctrl = !ctrl }
                    ModifierTogglePill("Alt", alt, Modifier.weight(1f).testTag("mod_alt")) { alt = !alt }
                    ModifierTogglePill("Shift", shift, Modifier.weight(1f).testTag("mod_shift")) { shift = !shift }
                    ModifierTogglePill("Win", meta, Modifier.weight(1f).testTag("mod_meta")) { meta = !meta }
                }

                Text(
                    text = if (actionType == HotkeyActionType.LAUNCH_APP) {
                        "Trigger Key on Keyboard (${selectedKey.label})"
                    } else {
                        "Primary Key (${selectedKey.label})"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    groups.forEach { group ->
                        val active = selectedGroup == group
                        Box(
                            modifier = (if (active) {
                                Modifier.neumorphicInset(colors.activeFill, colors.shadowLight, colors.shadowDark, 8.dp, 2.dp, colors.keyBorder.copy(alpha = 0.45f))
                            } else {
                                Modifier.neumorphicRaised(colors.bg, colors.shadowLight, colors.shadowDark, 8.dp, 2.dp, 4.dp, colors.keyBorder.copy(alpha = 0.45f))
                            })
                                .clickable { selectedGroup = group }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = group,
                                color = if (active) colors.indigo else colors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PcKeyboardLayoutData.selectableHotkeyKeys
                        .filter { it.group == selectedGroup }
                        .forEach { option ->
                            val active = option.keyCode == selectedKey.keyCode
                            Box(
                                modifier = (if (active) {
                                    Modifier.neumorphicInset(colors.activeFill, colors.shadowLight, colors.shadowDark, 8.dp, 2.dp, colors.keyBorder.copy(alpha = 0.45f))
                                } else {
                                    Modifier.neumorphicRaised(colors.bg, colors.shadowLight, colors.shadowDark, 8.dp, 2.dp, 4.dp, colors.keyBorder.copy(alpha = 0.45f))
                                })
                                    .clickable { selectedKey = option }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = option.label,
                                    fontFamily = BodyFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) colors.indigo else colors.textPrimary,
                                )
                            }
                        }
                }

                Text("Accent Color", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    accentSwatches.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .clickable { accentHex = hex },
                        )
                    }
                }

                Text("Category", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    HotkeyCategory.entries.filter { it != HotkeyCategory.ALL }.forEach { cat ->
                        val active = category == cat
                        Box(
                            modifier = (if (active) {
                                Modifier.neumorphicInset(colors.activeFill, colors.shadowLight, colors.shadowDark, 8.dp, 2.dp, colors.keyBorder.copy(alpha = 0.45f))
                            } else {
                                Modifier.neumorphicRaised(colors.bg, colors.shadowLight, colors.shadowDark, 8.dp, 2.dp, 4.dp, colors.keyBorder.copy(alpha = 0.45f))
                            })
                                .clickable { category = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = cat.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) colors.indigo else colors.textSecondary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val canSave = actionType != HotkeyActionType.LAUNCH_APP || targetPackageName.isNotBlank()
            Button(
                enabled = canSave,
                onClick = {
                    val cleanPkg = targetPackageName.trim()
                    val effectiveLabel = label.trim().ifEmpty {
                        when (actionType) {
                            HotkeyActionType.LAUNCH_APP -> targetAppLabel.ifBlank { cleanPkg }
                            HotkeyActionType.TOGGLE_FLOATING -> "Float Keyboard"
                            HotkeyActionType.KEY_COMBO -> selectedKey.label
                        }
                    }
                    onConfirm(
                        CustomHotkey(
                            id = initial?.id ?: "",
                            label = effectiveLabel,
                            keyCode = selectedKey.keyCode,
                            keyDisplay = selectedKey.label,
                            ctrl = ctrl,
                            alt = alt,
                            shift = shift,
                            meta = meta,
                            category = category,
                            accentHex = accentHex,
                            actionType = actionType,
                            targetPackageName = if (actionType == HotkeyActionType.LAUNCH_APP) cleanPkg else "",
                            targetAppLabel = if (actionType == HotkeyActionType.LAUNCH_APP) targetAppLabel.trim().ifBlank { effectiveLabel } else "",
                        ),
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accentFill),
                modifier = Modifier.testTag("hotkey_save_button"),
            ) {
                Text("Save Shortcut", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        },
    )
}

@Composable
private fun ModifierTogglePill(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = KeyDeckColors.current
    val mod = if (active) {
        Modifier.neumorphicInset(colors.activeFill, colors.shadowLight, colors.shadowDark, 10.dp, 2.dp, colors.keyBorder.copy(alpha = 0.45f))
    } else {
        Modifier.neumorphicRaised(colors.bg, colors.shadowLight, colors.shadowDark, 10.dp, 3.dp, 5.dp, colors.keyBorder.copy(alpha = 0.45f))
    }
    Box(
        modifier = modifier
            .height(38.dp)
            .then(mod)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontFamily = BodyFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) colors.indigo else colors.textSecondary,
        )
    }
}
