package com.example.ui.screens

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
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keyboard.CustomHotkey
import com.example.keyboard.HotkeyCategory
import com.example.keyboard.KeyDispatcher
import com.example.keyboard.PcKeyboardLayoutData
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.NeuActiveFill
import com.example.ui.theme.NeuBg
import com.example.ui.theme.NeuCoral
import com.example.ui.theme.NeuEmerald
import com.example.ui.theme.NeuIndigo
import com.example.ui.theme.NeuShadowDark
import com.example.ui.theme.NeuShadowLight
import com.example.ui.theme.NeuTextPrimary
import com.example.ui.theme.NeuTextSecondary
import com.example.ui.theme.neumorphicInset
import com.example.ui.theme.neumorphicRaised

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
    var selectedCategory by remember { mutableStateOf(HotkeyCategory.ALL) }
    var editingHotkey by remember { mutableStateOf<CustomHotkey?>(null) }
    var showEditorDialog by remember { mutableStateOf(false) }

    val filtered = remember(hotkeys, selectedCategory) {
        if (selectedCategory == HotkeyCategory.ALL) hotkeys
        else hotkeys.filter { it.category == selectedCategory }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NeuBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingHotkey = null
                    showEditorDialog = true
                },
                containerColor = NeuIndigo,
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
                    color = NeuTextPrimary,
                )
                Text(
                    text = "Pinned to the top strip of your Soft UI PC keyboard",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeuTextSecondary,
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
                            surfaceColor = NeuActiveFill,
                            shadowLight = NeuShadowLight,
                            shadowDark = NeuShadowDark,
                            cornerRadius = 12.dp,
                            depth = 3.dp,
                        )
                    } else {
                        Modifier.neumorphicRaised(
                            surfaceColor = NeuBg,
                            shadowLight = NeuShadowLight,
                            shadowDark = NeuShadowDark,
                            cornerRadius = 12.dp,
                            shadowOffset = 3.dp,
                            blurRadius = 6.dp,
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
                            color = if (selected) NeuIndigo else NeuTextSecondary,
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
                            tint = NeuIndigo,
                            modifier = Modifier.size(44.dp),
                        )
                        Text(
                            text = "No hotkeys in ${selectedCategory.label}",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeuTextPrimary,
                        )
                        Text(
                            text = "Tap + to create a custom keyboard shortcut macro",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeuTextSecondary,
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
                                    surfaceColor = NeuBg,
                                    shadowLight = NeuShadowLight,
                                    shadowDark = NeuShadowDark,
                                    cornerRadius = 18.dp,
                                    shadowOffset = 4.dp,
                                    blurRadius = 8.dp,
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
                                            color = NeuTextPrimary,
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .neumorphicInset(
                                                        surfaceColor = NeuActiveFill,
                                                        shadowLight = NeuShadowLight,
                                                        shadowDark = NeuShadowDark,
                                                        cornerRadius = 8.dp,
                                                        depth = 2.dp,
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                            ) {
                                                Text(
                                                    text = hk.comboText(),
                                                    fontFamily = BodyFontFamily,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = accent,
                                                )
                                            }
                                            Text(
                                                text = hk.category.label,
                                                fontSize = 11.sp,
                                                color = NeuTextSecondary,
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
                                            tint = NeuEmerald,
                                        )
                                    }
                                    IconButton(
                                        onClick = { onMoveHotkeyUp(hk.id) },
                                        modifier = Modifier.testTag("hotkey_move_up_${hk.id}"),
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = "Move ${hk.label} up",
                                            tint = NeuTextSecondary,
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
                                            tint = NeuIndigo,
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteHotkey(hk.id) },
                                        modifier = Modifier.testTag("hotkey_delete_${hk.id}"),
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete ${hk.label}",
                                            tint = NeuCoral,
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
    Box(
        modifier = Modifier
            .neumorphicRaised(
                surfaceColor = NeuBg,
                shadowLight = NeuShadowLight,
                shadowDark = NeuShadowDark,
                cornerRadius = 10.dp,
                shadowOffset = 3.dp,
                blurRadius = 6.dp,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(testTag),
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NeuIndigo)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HotkeyEditorDialog(
    initial: CustomHotkey?,
    onDismiss: () -> Unit,
    onConfirm: (CustomHotkey) -> Unit,
) {
    var label by remember { mutableStateOf(initial?.label ?: "") }
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
        containerColor = NeuBg,
        titleContentColor = NeuTextPrimary,
        textContentColor = NeuTextPrimary,
        title = {
            Text(
                text = if (initial == null) "New Hotkey Shortcut" else "Edit Hotkey",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Shortcut Name") },
                    placeholder = { Text("e.g. Hard Reload, Play/Pause") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeuIndigo,
                        unfocusedBorderColor = NeuShadowDark,
                        focusedTextColor = NeuTextPrimary,
                        unfocusedTextColor = NeuTextPrimary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hotkey_name_input"),
                )

                Text("Modifiers", style = MaterialTheme.typography.labelLarge, color = NeuTextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ModifierTogglePill("Ctrl", ctrl, Modifier.weight(1f).testTag("mod_ctrl")) { ctrl = !ctrl }
                    ModifierTogglePill("Alt", alt, Modifier.weight(1f).testTag("mod_alt")) { alt = !alt }
                    ModifierTogglePill("Shift", shift, Modifier.weight(1f).testTag("mod_shift")) { shift = !shift }
                    ModifierTogglePill("Win", meta, Modifier.weight(1f).testTag("mod_meta")) { meta = !meta }
                }

                Text("Primary Key (${selectedKey.label})", style = MaterialTheme.typography.labelLarge, color = NeuTextSecondary)
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
                                Modifier.neumorphicInset(NeuActiveFill, NeuShadowLight, NeuShadowDark, 8.dp, 2.dp)
                            } else {
                                Modifier.neumorphicRaised(NeuBg, NeuShadowLight, NeuShadowDark, 8.dp, 2.dp, 4.dp)
                            })
                                .clickable { selectedGroup = group }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = group,
                                color = if (active) NeuIndigo else NeuTextSecondary,
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
                                    Modifier.neumorphicInset(NeuActiveFill, NeuShadowLight, NeuShadowDark, 8.dp, 2.dp)
                                } else {
                                    Modifier.neumorphicRaised(NeuBg, NeuShadowLight, NeuShadowDark, 8.dp, 2.dp, 4.dp)
                                })
                                    .clickable { selectedKey = option }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = option.label,
                                    fontFamily = BodyFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) NeuIndigo else NeuTextPrimary,
                                )
                            }
                        }
                }

                Text("Accent Color", style = MaterialTheme.typography.labelLarge, color = NeuTextSecondary)
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

                Text("Category", style = MaterialTheme.typography.labelLarge, color = NeuTextSecondary)
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
                                Modifier.neumorphicInset(NeuActiveFill, NeuShadowLight, NeuShadowDark, 8.dp, 2.dp)
                            } else {
                                Modifier.neumorphicRaised(NeuBg, NeuShadowLight, NeuShadowDark, 8.dp, 2.dp, 4.dp)
                            })
                                .clickable { category = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = cat.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) NeuIndigo else NeuTextSecondary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val effectiveLabel = label.trim().ifEmpty { selectedKey.label }
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
                        ),
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeuIndigo),
                modifier = Modifier.testTag("hotkey_save_button"),
            ) {
                Text("Save Hotkey", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NeuTextSecondary)
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
    val mod = if (active) {
        Modifier.neumorphicInset(NeuActiveFill, NeuShadowLight, NeuShadowDark, 10.dp, 2.dp)
    } else {
        Modifier.neumorphicRaised(NeuBg, NeuShadowLight, NeuShadowDark, 10.dp, 3.dp, 5.dp)
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
            color = if (active) NeuIndigo else NeuTextSecondary,
        )
    }
}
