package com.example.keyboard

import android.view.KeyEvent

object PcKeyboardLayoutData {

    // Top Function Row: esc, f1..f12 + numpad toggle
    val functionRow: List<KeySpec> = listOf(
        KeySpec("esc", "Esc", keyCode = KeyEvent.KEYCODE_ESCAPE, weight = 1.15f, isModifier = true),
        KeySpec("f1", "F1", keyCode = KeyEvent.KEYCODE_F1),
        KeySpec("f2", "F2", keyCode = KeyEvent.KEYCODE_F2),
        KeySpec("f3", "F3", keyCode = KeyEvent.KEYCODE_F3),
        KeySpec("f4", "F4", keyCode = KeyEvent.KEYCODE_F4),
        KeySpec("f5", "F5", keyCode = KeyEvent.KEYCODE_F5),
        KeySpec("f6", "F6", keyCode = KeyEvent.KEYCODE_F6),
        KeySpec("f7", "F7", keyCode = KeyEvent.KEYCODE_F7),
        KeySpec("f8", "F8", keyCode = KeyEvent.KEYCODE_F8),
        KeySpec("f9", "F9", keyCode = KeyEvent.KEYCODE_F9),
        KeySpec("f10", "F10", keyCode = KeyEvent.KEYCODE_F10),
        KeySpec("f11", "F11", keyCode = KeyEvent.KEYCODE_F11),
        KeySpec("f12", "F12", keyCode = KeyEvent.KEYCODE_F12),
        KeySpec("num_toggle_top", "123", keyCode = KeyEvent.KEYCODE_NUM_LOCK, weight = 1.1f, isModifier = true, iconType = KeyIconType.MIC_OR_NUM),
    )

    // Full 5-row main PC QWERTY layout (14 columns)
    val mainRows: List<List<KeySpec>> = listOf(
        listOf(
            KeySpec("grave", "`", "~", keyCode = KeyEvent.KEYCODE_GRAVE, charPrimary = '`', charShift = '~'),
            KeySpec("1", "1", "!", keyCode = KeyEvent.KEYCODE_1, charPrimary = '1', charShift = '!'),
            KeySpec("2", "2", "@", keyCode = KeyEvent.KEYCODE_2, charPrimary = '2', charShift = '@'),
            KeySpec("3", "3", "#", keyCode = KeyEvent.KEYCODE_3, charPrimary = '3', charShift = '#'),
            KeySpec("4", "4", "$", keyCode = KeyEvent.KEYCODE_4, charPrimary = '4', charShift = '$'),
            KeySpec("5", "5", "%", keyCode = KeyEvent.KEYCODE_5, charPrimary = '5', charShift = '%'),
            KeySpec("6", "6", "^", keyCode = KeyEvent.KEYCODE_6, charPrimary = '6', charShift = '^'),
            KeySpec("7", "7", "&", keyCode = KeyEvent.KEYCODE_7, charPrimary = '7', charShift = '&'),
            KeySpec("8", "8", "*", keyCode = KeyEvent.KEYCODE_8, charPrimary = '8', charShift = '*'),
            KeySpec("9", "9", "(", keyCode = KeyEvent.KEYCODE_9, charPrimary = '9', charShift = '('),
            KeySpec("0", "0", ")", keyCode = KeyEvent.KEYCODE_0, charPrimary = '0', charShift = ')'),
            KeySpec("minus", "-", "_", keyCode = KeyEvent.KEYCODE_MINUS, charPrimary = '-', charShift = '_'),
            KeySpec("equals", "=", "+", keyCode = KeyEvent.KEYCODE_EQUALS, charPrimary = '=', charShift = '+'),
            KeySpec("bksp", "⌫", fnLabel = "Del", keyCode = KeyEvent.KEYCODE_DEL, fnKeyCode = KeyEvent.KEYCODE_FORWARD_DEL, weight = 1.65f, isModifier = true, iconType = KeyIconType.BACKSPACE),
        ),
        listOf(
            KeySpec("tab", "Tab", keyCode = KeyEvent.KEYCODE_TAB, weight = 1.35f, isModifier = true),
            KeySpec("q", "q", "Q", keyCode = KeyEvent.KEYCODE_Q, charPrimary = 'q', charShift = 'Q'),
            KeySpec("w", "w", "W", keyCode = KeyEvent.KEYCODE_W, charPrimary = 'w', charShift = 'W'),
            KeySpec("e", "e", "E", keyCode = KeyEvent.KEYCODE_E, charPrimary = 'e', charShift = 'E'),
            KeySpec("r", "r", "R", keyCode = KeyEvent.KEYCODE_R, charPrimary = 'r', charShift = 'R'),
            KeySpec("t", "t", "T", keyCode = KeyEvent.KEYCODE_T, charPrimary = 't', charShift = 'T'),
            KeySpec("y", "y", "Y", keyCode = KeyEvent.KEYCODE_Y, charPrimary = 'y', charShift = 'Y'),
            KeySpec("u", "u", "U", keyCode = KeyEvent.KEYCODE_U, charPrimary = 'u', charShift = 'U'),
            KeySpec("i", "i", "I", keyCode = KeyEvent.KEYCODE_I, charPrimary = 'i', charShift = 'I'),
            KeySpec("o", "o", "O", keyCode = KeyEvent.KEYCODE_O, charPrimary = 'o', charShift = 'O'),
            KeySpec("p", "p", "P", keyCode = KeyEvent.KEYCODE_P, charPrimary = 'p', charShift = 'P'),
            KeySpec("lbracket", "[", "{", keyCode = KeyEvent.KEYCODE_LEFT_BRACKET, charPrimary = '[', charShift = '{'),
            KeySpec("rbracket", "]", "}", keyCode = KeyEvent.KEYCODE_RIGHT_BRACKET, charPrimary = ']', charShift = '}'),
            KeySpec("bslash", "\\", "|", keyCode = KeyEvent.KEYCODE_BACKSLASH, charPrimary = '\\', charShift = '|', weight = 1.25f),
        ),
        listOf(
            KeySpec("caps", "Caps", keyCode = KeyEvent.KEYCODE_CAPS_LOCK, weight = 1.55f, isModifier = true, modifierType = ModifierKey.CAPS),
            KeySpec("a", "a", "A", keyCode = KeyEvent.KEYCODE_A, charPrimary = 'a', charShift = 'A'),
            KeySpec("s", "s", "S", keyCode = KeyEvent.KEYCODE_S, charPrimary = 's', charShift = 'S'),
            KeySpec("d", "d", "D", keyCode = KeyEvent.KEYCODE_D, charPrimary = 'd', charShift = 'D'),
            KeySpec("f", "f", "F", keyCode = KeyEvent.KEYCODE_F, charPrimary = 'f', charShift = 'F'),
            KeySpec("g", "g", "G", keyCode = KeyEvent.KEYCODE_G, charPrimary = 'g', charShift = 'G'),
            KeySpec("h", "h", "H", keyCode = KeyEvent.KEYCODE_H, charPrimary = 'h', charShift = 'H'),
            KeySpec("j", "j", "J", keyCode = KeyEvent.KEYCODE_J, charPrimary = 'j', charShift = 'J'),
            KeySpec("k", "k", "K", keyCode = KeyEvent.KEYCODE_K, charPrimary = 'k', charShift = 'K'),
            KeySpec("l", "l", "L", keyCode = KeyEvent.KEYCODE_L, charPrimary = 'l', charShift = 'L'),
            KeySpec("semi", ";", ":", keyCode = KeyEvent.KEYCODE_SEMICOLON, charPrimary = ';', charShift = ':'),
            KeySpec("quote", "'", "\"", keyCode = KeyEvent.KEYCODE_APOSTROPHE, charPrimary = '\'', charShift = '"'),
            KeySpec("enter", "Enter", keyCode = KeyEvent.KEYCODE_ENTER, weight = 1.85f, isModifier = true),
        ),
        listOf(
            KeySpec("shift_l", "Shift", keyCode = KeyEvent.KEYCODE_SHIFT_LEFT, weight = 1.85f, isModifier = true, modifierType = ModifierKey.SHIFT),
            KeySpec("z", "z", "Z", keyCode = KeyEvent.KEYCODE_Z, charPrimary = 'z', charShift = 'Z'),
            KeySpec("x", "x", "X", keyCode = KeyEvent.KEYCODE_X, charPrimary = 'x', charShift = 'X'),
            KeySpec("c", "c", "C", keyCode = KeyEvent.KEYCODE_C, charPrimary = 'c', charShift = 'C'),
            KeySpec("v", "v", "V", keyCode = KeyEvent.KEYCODE_V, charPrimary = 'v', charShift = 'V'),
            KeySpec("b", "b", "B", keyCode = KeyEvent.KEYCODE_B, charPrimary = 'b', charShift = 'B'),
            KeySpec("n", "n", "N", keyCode = KeyEvent.KEYCODE_N, charPrimary = 'n', charShift = 'N'),
            KeySpec("m", "m", "M", keyCode = KeyEvent.KEYCODE_M, charPrimary = 'm', charShift = 'M'),
            KeySpec("comma", ",", "<", keyCode = KeyEvent.KEYCODE_COMMA, charPrimary = ',', charShift = '<'),
            KeySpec("period", ".", ">", keyCode = KeyEvent.KEYCODE_PERIOD, charPrimary = '.', charShift = '>'),
            KeySpec("slash", "/", "?", keyCode = KeyEvent.KEYCODE_SLASH, charPrimary = '/', charShift = '?'),
            KeySpec("up", "↑", fnLabel = "PgUp", keyCode = KeyEvent.KEYCODE_DPAD_UP, fnKeyCode = KeyEvent.KEYCODE_PAGE_UP, isModifier = true, iconType = KeyIconType.ARROW_UP),
            KeySpec("shift_r", "Shift", keyCode = KeyEvent.KEYCODE_SHIFT_RIGHT, weight = 1.25f, isModifier = true, modifierType = ModifierKey.SHIFT),
        ),
        listOf(
            KeySpec("ctrl", "Ctrl", keyCode = KeyEvent.KEYCODE_CTRL_LEFT, weight = 1.25f, isModifier = true, modifierType = ModifierKey.CTRL),
            KeySpec("meta", "Win", keyCode = KeyEvent.KEYCODE_META_LEFT, weight = 1.05f, isModifier = true, modifierType = ModifierKey.META),
            KeySpec("alt", "Alt", keyCode = KeyEvent.KEYCODE_ALT_LEFT, weight = 1.05f, isModifier = true, modifierType = ModifierKey.ALT),
            KeySpec("space", "Space", keyCode = KeyEvent.KEYCODE_SPACE, charPrimary = ' ', charShift = ' ', weight = 4.2f),
            KeySpec("alt_r", "Alt", keyCode = KeyEvent.KEYCODE_ALT_RIGHT, weight = 1.0f, isModifier = true, modifierType = ModifierKey.ALT),
            KeySpec("fn", "Fn", keyCode = KeyEvent.KEYCODE_FUNCTION, weight = 0.95f, isModifier = true, modifierType = ModifierKey.FN),
            KeySpec("left", "←", fnLabel = "Home", keyCode = KeyEvent.KEYCODE_DPAD_LEFT, fnKeyCode = KeyEvent.KEYCODE_MOVE_HOME, isModifier = true, iconType = KeyIconType.ARROW_LEFT),
            KeySpec("down", "↓", fnLabel = "PgDn", keyCode = KeyEvent.KEYCODE_DPAD_DOWN, fnKeyCode = KeyEvent.KEYCODE_PAGE_DOWN, isModifier = true, iconType = KeyIconType.ARROW_DOWN),
            KeySpec("right", "→", fnLabel = "End", keyCode = KeyEvent.KEYCODE_DPAD_RIGHT, fnKeyCode = KeyEvent.KEYCODE_MOVE_END, isModifier = true, iconType = KeyIconType.ARROW_RIGHT),
        ),
    )

    // Ergonomic 10-col x 5-row QWERTY block paired with 4-col x 5-row Numpad (14 total columns -> large tappable keys!)
    val splitPcRows: List<List<KeySpec>> = listOf(
        // Row 1: utility + essential symbols (numbers live on the dedicated right Numpad!)
        listOf(
            KeySpec("sp_esc", "Esc", keyCode = KeyEvent.KEYCODE_ESCAPE, weight = 1.05f, isModifier = true),
            KeySpec("sp_tab", "Tab", keyCode = KeyEvent.KEYCODE_TAB, weight = 1.05f, isModifier = true),
            KeySpec("sp_grave", "`", "~", keyCode = KeyEvent.KEYCODE_GRAVE, charPrimary = '`', charShift = '~'),
            KeySpec("sp_lbr", "[", "{", keyCode = KeyEvent.KEYCODE_LEFT_BRACKET, charPrimary = '[', charShift = '{'),
            KeySpec("sp_rbr", "]", "}", keyCode = KeyEvent.KEYCODE_RIGHT_BRACKET, charPrimary = ']', charShift = '}'),
            KeySpec("sp_semi", ";", ":", keyCode = KeyEvent.KEYCODE_SEMICOLON, charPrimary = ';', charShift = ':'),
            KeySpec("sp_quote", "'", "\"", keyCode = KeyEvent.KEYCODE_APOSTROPHE, charPrimary = '\'', charShift = '"'),
            KeySpec("sp_slash", "/", "?", keyCode = KeyEvent.KEYCODE_SLASH, charPrimary = '/', charShift = '?'),
            KeySpec("sp_bsl", "\\", "|", keyCode = KeyEvent.KEYCODE_BACKSLASH, charPrimary = '\\', charShift = '|'),
            KeySpec("sp_fn", "Fn", keyCode = KeyEvent.KEYCODE_FUNCTION, weight = 1.0f, isModifier = true, modifierType = ModifierKey.FN),
        ),
        // Row 2: Q..P
        listOf(
            KeySpec("q", "q", "Q", keyCode = KeyEvent.KEYCODE_Q, charPrimary = 'q', charShift = 'Q'),
            KeySpec("w", "w", "W", keyCode = KeyEvent.KEYCODE_W, charPrimary = 'w', charShift = 'W'),
            KeySpec("e", "e", "E", keyCode = KeyEvent.KEYCODE_E, charPrimary = 'e', charShift = 'E'),
            KeySpec("r", "r", "R", keyCode = KeyEvent.KEYCODE_R, charPrimary = 'r', charShift = 'R'),
            KeySpec("t", "t", "T", keyCode = KeyEvent.KEYCODE_T, charPrimary = 't', charShift = 'T'),
            KeySpec("y", "y", "Y", keyCode = KeyEvent.KEYCODE_Y, charPrimary = 'y', charShift = 'Y'),
            KeySpec("u", "u", "U", keyCode = KeyEvent.KEYCODE_U, charPrimary = 'u', charShift = 'U'),
            KeySpec("i", "i", "I", keyCode = KeyEvent.KEYCODE_I, charPrimary = 'i', charShift = 'I'),
            KeySpec("o", "o", "O", keyCode = KeyEvent.KEYCODE_O, charPrimary = 'o', charShift = 'O'),
            KeySpec("p", "p", "P", keyCode = KeyEvent.KEYCODE_P, charPrimary = 'p', charShift = 'P'),
        ),
        // Row 3: Caps + A..L
        listOf(
            KeySpec("sp_caps", "Caps", keyCode = KeyEvent.KEYCODE_CAPS_LOCK, weight = 1.05f, isModifier = true, modifierType = ModifierKey.CAPS),
            KeySpec("a", "a", "A", keyCode = KeyEvent.KEYCODE_A, charPrimary = 'a', charShift = 'A'),
            KeySpec("s", "s", "S", keyCode = KeyEvent.KEYCODE_S, charPrimary = 's', charShift = 'S'),
            KeySpec("d", "d", "D", keyCode = KeyEvent.KEYCODE_D, charPrimary = 'd', charShift = 'D'),
            KeySpec("f", "f", "F", keyCode = KeyEvent.KEYCODE_F, charPrimary = 'f', charShift = 'F'),
            KeySpec("g", "g", "G", keyCode = KeyEvent.KEYCODE_G, charPrimary = 'g', charShift = 'G'),
            KeySpec("h", "h", "H", keyCode = KeyEvent.KEYCODE_H, charPrimary = 'h', charShift = 'H'),
            KeySpec("j", "j", "J", keyCode = KeyEvent.KEYCODE_J, charPrimary = 'j', charShift = 'J'),
            KeySpec("k", "k", "K", keyCode = KeyEvent.KEYCODE_K, charPrimary = 'k', charShift = 'K'),
            KeySpec("l", "l", "L", keyCode = KeyEvent.KEYCODE_L, charPrimary = 'l', charShift = 'L'),
        ),
        // Row 4: Shift + Z..M + , + Bksp
        listOf(
            KeySpec("sp_shift", "⇧", keyCode = KeyEvent.KEYCODE_SHIFT_LEFT, weight = 1.25f, isModifier = true, modifierType = ModifierKey.SHIFT, iconType = KeyIconType.SHIFT),
            KeySpec("z", "z", "Z", keyCode = KeyEvent.KEYCODE_Z, charPrimary = 'z', charShift = 'Z'),
            KeySpec("x", "x", "X", keyCode = KeyEvent.KEYCODE_X, charPrimary = 'x', charShift = 'X'),
            KeySpec("c", "c", "C", keyCode = KeyEvent.KEYCODE_C, charPrimary = 'c', charShift = 'C'),
            KeySpec("v", "v", "V", keyCode = KeyEvent.KEYCODE_V, charPrimary = 'v', charShift = 'V'),
            KeySpec("b", "b", "B", keyCode = KeyEvent.KEYCODE_B, charPrimary = 'b', charShift = 'B'),
            KeySpec("n", "n", "N", keyCode = KeyEvent.KEYCODE_N, charPrimary = 'n', charShift = 'N'),
            KeySpec("m", "m", "M", keyCode = KeyEvent.KEYCODE_M, charPrimary = 'm', charShift = 'M'),
            KeySpec("sp_comma", ",", "<", keyCode = KeyEvent.KEYCODE_COMMA, charPrimary = ',', charShift = '<'),
            KeySpec("sp_bksp", "⌫", fnLabel = "Del", keyCode = KeyEvent.KEYCODE_DEL, fnKeyCode = KeyEvent.KEYCODE_FORWARD_DEL, weight = 1.25f, isModifier = true, iconType = KeyIconType.BACKSPACE),
        ),
        // Row 5: Ctrl + Alt + Space + . + Arrows + Enter
        listOf(
            KeySpec("sp_ctrl", "Ctrl", keyCode = KeyEvent.KEYCODE_CTRL_LEFT, weight = 1.2f, isModifier = true, modifierType = ModifierKey.CTRL),
            KeySpec("sp_alt", "Alt", keyCode = KeyEvent.KEYCODE_ALT_LEFT, weight = 1.0f, isModifier = true, modifierType = ModifierKey.ALT),
            KeySpec("sp_space", "Space", keyCode = KeyEvent.KEYCODE_SPACE, charPrimary = ' ', charShift = ' ', weight = 2.6f),
            KeySpec("sp_dot", ".", ">", keyCode = KeyEvent.KEYCODE_PERIOD, charPrimary = '.', charShift = '>', weight = 0.85f),
            KeySpec("sp_left", "←", fnLabel = "Hm", keyCode = KeyEvent.KEYCODE_DPAD_LEFT, fnKeyCode = KeyEvent.KEYCODE_MOVE_HOME, weight = 0.9f, isModifier = true, iconType = KeyIconType.ARROW_LEFT),
            KeySpec("sp_up", "↑", fnLabel = "PU", keyCode = KeyEvent.KEYCODE_DPAD_UP, fnKeyCode = KeyEvent.KEYCODE_PAGE_UP, weight = 0.9f, isModifier = true, iconType = KeyIconType.ARROW_UP),
            KeySpec("sp_down", "↓", fnLabel = "PD", keyCode = KeyEvent.KEYCODE_DPAD_DOWN, fnKeyCode = KeyEvent.KEYCODE_PAGE_DOWN, weight = 0.9f, isModifier = true, iconType = KeyIconType.ARROW_DOWN),
            KeySpec("sp_right", "→", fnLabel = "En", keyCode = KeyEvent.KEYCODE_DPAD_RIGHT, fnKeyCode = KeyEvent.KEYCODE_MOVE_END, weight = 0.9f, isModifier = true, iconType = KeyIconType.ARROW_RIGHT),
            KeySpec("sp_enter", "↵", keyCode = KeyEvent.KEYCODE_ENTER, weight = 1.25f, isModifier = true, iconType = KeyIconType.ENTER),
        ),
    )

    // Left-hand Gaming Cluster (Minecraft PE / PC Games): Hotbar 1-5, Esc, Tab, Q W E R, A S D F G, Shift C Space Ctrl
    val gamingLeftRows: List<List<KeySpec>> = listOf(
        listOf(
            KeySpec("gm_esc", "Esc", keyCode = KeyEvent.KEYCODE_ESCAPE, weight = 1.05f, isModifier = true),
            KeySpec("gm_1", "1", keyCode = KeyEvent.KEYCODE_1, charPrimary = '1'),
            KeySpec("gm_2", "2", keyCode = KeyEvent.KEYCODE_2, charPrimary = '2'),
            KeySpec("gm_3", "3", keyCode = KeyEvent.KEYCODE_3, charPrimary = '3'),
            KeySpec("gm_4", "4", keyCode = KeyEvent.KEYCODE_4, charPrimary = '4'),
            KeySpec("gm_5", "5", keyCode = KeyEvent.KEYCODE_5, charPrimary = '5'),
        ),
        listOf(
            KeySpec("gm_q", "Q", keyCode = KeyEvent.KEYCODE_Q, charPrimary = 'q', charShift = 'Q'),
            KeySpec("gm_w", "W", keyCode = KeyEvent.KEYCODE_W, charPrimary = 'w', charShift = 'W', isAccent = true),
            KeySpec("gm_e", "E", keyCode = KeyEvent.KEYCODE_E, charPrimary = 'e', charShift = 'E'),
            KeySpec("gm_r", "R", keyCode = KeyEvent.KEYCODE_R, charPrimary = 'r', charShift = 'R'),
            KeySpec("gm_tab", "Tab", keyCode = KeyEvent.KEYCODE_TAB, isModifier = true),
        ),
        listOf(
            KeySpec("gm_a", "A", keyCode = KeyEvent.KEYCODE_A, charPrimary = 'a', charShift = 'A', isAccent = true),
            KeySpec("gm_s", "S", keyCode = KeyEvent.KEYCODE_S, charPrimary = 's', charShift = 'S', isAccent = true),
            KeySpec("gm_d", "D", keyCode = KeyEvent.KEYCODE_D, charPrimary = 'd', charShift = 'D', isAccent = true),
            KeySpec("gm_f", "F", keyCode = KeyEvent.KEYCODE_F, charPrimary = 'f', charShift = 'F'),
            KeySpec("gm_g", "G", keyCode = KeyEvent.KEYCODE_G, charPrimary = 'g', charShift = 'G'),
        ),
        listOf(
            KeySpec("gm_shift", "Shift", keyCode = KeyEvent.KEYCODE_SHIFT_LEFT, weight = 1.3f, isModifier = true),
            KeySpec("gm_c", "C", keyCode = KeyEvent.KEYCODE_C, charPrimary = 'c', charShift = 'C'),
            KeySpec("gm_space", "Space", keyCode = KeyEvent.KEYCODE_SPACE, charPrimary = ' ', weight = 2.0f, isModifier = true),
            KeySpec("gm_ctrl", "Ctrl", keyCode = KeyEvent.KEYCODE_CTRL_LEFT, weight = 1.1f, isModifier = true),
        ),
    )

    // Dedicated 4x5 PC Numeric Pad
    val numpadRows: List<List<KeySpec>> = listOf(
        listOf(
            KeySpec("num_lock", "Num", keyCode = KeyEvent.KEYCODE_NUM_LOCK, isModifier = true),
            KeySpec("num_div", "/", keyCode = KeyEvent.KEYCODE_NUMPAD_DIVIDE, charPrimary = '/', isModifier = true),
            KeySpec("num_mul", "*", keyCode = KeyEvent.KEYCODE_NUMPAD_MULTIPLY, charPrimary = '*', isModifier = true),
            KeySpec("num_sub", "-", keyCode = KeyEvent.KEYCODE_NUMPAD_SUBTRACT, charPrimary = '-', isModifier = true),
        ),
        listOf(
            KeySpec("num_7", "7", fnLabel = "Hm", keyCode = KeyEvent.KEYCODE_NUMPAD_7, fnKeyCode = KeyEvent.KEYCODE_MOVE_HOME, charPrimary = '7'),
            KeySpec("num_8", "8", fnLabel = "↑", keyCode = KeyEvent.KEYCODE_NUMPAD_8, fnKeyCode = KeyEvent.KEYCODE_DPAD_UP, charPrimary = '8'),
            KeySpec("num_9", "9", fnLabel = "PU", keyCode = KeyEvent.KEYCODE_NUMPAD_9, fnKeyCode = KeyEvent.KEYCODE_PAGE_UP, charPrimary = '9'),
            KeySpec("num_add", "+", keyCode = KeyEvent.KEYCODE_NUMPAD_ADD, charPrimary = '+', isModifier = true),
        ),
        listOf(
            KeySpec("num_4", "4", fnLabel = "←", keyCode = KeyEvent.KEYCODE_NUMPAD_4, fnKeyCode = KeyEvent.KEYCODE_DPAD_LEFT, charPrimary = '4'),
            KeySpec("num_5", "5", fnLabel = "5", keyCode = KeyEvent.KEYCODE_NUMPAD_5, fnKeyCode = KeyEvent.KEYCODE_NUMPAD_5, charPrimary = '5'),
            KeySpec("num_6", "6", fnLabel = "→", keyCode = KeyEvent.KEYCODE_NUMPAD_6, fnKeyCode = KeyEvent.KEYCODE_DPAD_RIGHT, charPrimary = '6'),
            KeySpec("num_bksp", "⌫", keyCode = KeyEvent.KEYCODE_DEL, isModifier = true, iconType = KeyIconType.BACKSPACE),
        ),
        listOf(
            KeySpec("num_1", "1", fnLabel = "En", keyCode = KeyEvent.KEYCODE_NUMPAD_1, fnKeyCode = KeyEvent.KEYCODE_MOVE_END, charPrimary = '1'),
            KeySpec("num_2", "2", fnLabel = "↓", keyCode = KeyEvent.KEYCODE_NUMPAD_2, fnKeyCode = KeyEvent.KEYCODE_DPAD_DOWN, charPrimary = '2'),
            KeySpec("num_3", "3", fnLabel = "PD", keyCode = KeyEvent.KEYCODE_NUMPAD_3, fnKeyCode = KeyEvent.KEYCODE_PAGE_DOWN, charPrimary = '3'),
            KeySpec("num_enter", "Ent", keyCode = KeyEvent.KEYCODE_NUMPAD_ENTER, isAccent = true),
        ),
        listOf(
            KeySpec("num_0", "0", fnLabel = "Ins", keyCode = KeyEvent.KEYCODE_NUMPAD_0, fnKeyCode = KeyEvent.KEYCODE_INSERT, charPrimary = '0', weight = 2f),
            KeySpec("num_dot", ".", fnLabel = "Del", keyCode = KeyEvent.KEYCODE_NUMPAD_DOT, fnKeyCode = KeyEvent.KEYCODE_FORWARD_DEL, charPrimary = '.'),
            KeySpec("num_eq", "=", keyCode = KeyEvent.KEYCODE_NUMPAD_EQUALS, charPrimary = '=', isModifier = true),
        ),
    )

    val defaultHotkeys: List<CustomHotkey> = listOf(
        CustomHotkey("hk_copy", "Copy", KeyEvent.KEYCODE_C, "C", ctrl = true, category = HotkeyCategory.EDITING, accentHex = 0xFF4F46E5),
        CustomHotkey("hk_paste", "Paste", KeyEvent.KEYCODE_V, "V", ctrl = true, category = HotkeyCategory.EDITING, accentHex = 0xFF0D9488),
        CustomHotkey("hk_cut", "Cut", KeyEvent.KEYCODE_X, "X", ctrl = true, category = HotkeyCategory.EDITING, accentHex = 0xFFD97706),
        CustomHotkey("hk_undo", "Undo", KeyEvent.KEYCODE_Z, "Z", ctrl = true, category = HotkeyCategory.EDITING, accentHex = 0xFFE11D48),
        CustomHotkey("hk_redo", "Redo", KeyEvent.KEYCODE_Z, "Z", ctrl = true, shift = true, category = HotkeyCategory.EDITING, accentHex = 0xFF7C3AED),
        CustomHotkey("hk_selall", "Select All", KeyEvent.KEYCODE_A, "A", ctrl = true, category = HotkeyCategory.EDITING, accentHex = 0xFF2563EB),
        CustomHotkey("hk_find", "Find", KeyEvent.KEYCODE_F, "F", ctrl = true, category = HotkeyCategory.BROWSER, accentHex = 0xFFDB2777),
        CustomHotkey("hk_newtab", "New Tab", KeyEvent.KEYCODE_T, "T", ctrl = true, category = HotkeyCategory.BROWSER, accentHex = 0xFF4F46E5),
        CustomHotkey("hk_reopen", "Reopen Tab", KeyEvent.KEYCODE_T, "T", ctrl = true, shift = true, category = HotkeyCategory.BROWSER, accentHex = 0xFFEA580C),
        CustomHotkey("hk_alttab", "Switch App", KeyEvent.KEYCODE_TAB, "Tab", alt = true, category = HotkeyCategory.NAVIGATION, accentHex = 0xFFE11D48),
        CustomHotkey("hk_f11", "Fullscreen", KeyEvent.KEYCODE_F11, "F11", category = HotkeyCategory.BROWSER, accentHex = 0xFF059669),
        CustomHotkey("hk_play", "Play/Pause", KeyEvent.KEYCODE_SPACE, "Space", category = HotkeyCategory.NAVIGATION, accentHex = 0xFFD97706),
    )

    data class KeyPickerOption(val label: String, val keyCode: Int, val group: String)

    val selectableHotkeyKeys: List<KeyPickerOption> = buildList {
        for (c in 'A'..'Z') {
            add(KeyPickerOption(c.toString(), KeyEvent.KEYCODE_A + (c - 'A'), "Letters"))
        }
        for (d in 0..9) {
            add(KeyPickerOption(d.toString(), KeyEvent.KEYCODE_0 + d, "Numbers"))
        }
        for (d in 0..9) {
            add(KeyPickerOption("Num$d", KeyEvent.KEYCODE_NUMPAD_0 + d, "Numpad"))
        }
        for (f in 1..12) {
            add(KeyPickerOption("F$f", KeyEvent.KEYCODE_F1 + (f - 1), "Function"))
        }
        add(KeyPickerOption("Space", KeyEvent.KEYCODE_SPACE, "Special"))
        add(KeyPickerOption("Tab", KeyEvent.KEYCODE_TAB, "Special"))
        add(KeyPickerOption("Enter", KeyEvent.KEYCODE_ENTER, "Special"))
        add(KeyPickerOption("Esc", KeyEvent.KEYCODE_ESCAPE, "Special"))
        add(KeyPickerOption("Bksp", KeyEvent.KEYCODE_DEL, "Special"))
        add(KeyPickerOption("Del", KeyEvent.KEYCODE_FORWARD_DEL, "Special"))
        add(KeyPickerOption("Home", KeyEvent.KEYCODE_MOVE_HOME, "Special"))
        add(KeyPickerOption("End", KeyEvent.KEYCODE_MOVE_END, "Special"))
        add(KeyPickerOption("PgUp", KeyEvent.KEYCODE_PAGE_UP, "Special"))
        add(KeyPickerOption("PgDn", KeyEvent.KEYCODE_PAGE_DOWN, "Special"))
        add(KeyPickerOption("Left", KeyEvent.KEYCODE_DPAD_LEFT, "Special"))
        add(KeyPickerOption("Up", KeyEvent.KEYCODE_DPAD_UP, "Special"))
        add(KeyPickerOption("Down", KeyEvent.KEYCODE_DPAD_DOWN, "Special"))
        add(KeyPickerOption("Right", KeyEvent.KEYCODE_DPAD_RIGHT, "Special"))
        add(KeyPickerOption("/", KeyEvent.KEYCODE_SLASH, "Special"))
        add(KeyPickerOption(".", KeyEvent.KEYCODE_PERIOD, "Special"))
        add(KeyPickerOption("[", KeyEvent.KEYCODE_LEFT_BRACKET, "Special"))
        add(KeyPickerOption("]", KeyEvent.KEYCODE_RIGHT_BRACKET, "Special"))
    }
}
