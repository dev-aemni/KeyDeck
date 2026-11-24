# KeyDeck (v1.1.5)

**KeyDeck** is a full PC-grade mechanical-style keyboard, persistent floating deck, gaming controller/touchpad, and macro launcher for Android (`com.aemni.keydeck`). Unlike ordinary mobile keyboards that only commit plain text—or physical keyboards tied to wires and desks—KeyDeck combines **hardware-level Android `KeyEvent` synthesis**, **multi-mode desktop/mobile/gaming layouts**, and **deep visual customization** in one keyboard.

---

## What KeyDeck Supports That Standard PC & Mobile Keyboards Don't

### 1. Native Hardware-Style `KeyEvent` Synthesis + Android OS Shortcuts
- Dispatches paired `ACTION_DOWN` and `ACTION_UP` Android `KeyEvent` objects tagged with `InputDevice.SOURCE_KEYBOARD` and `KeyCharacterMap.VIRTUAL_KEYBOARD` plus real modifier bitmasks (`META_META_ON`, `META_CTRL_ON`, `META_ALT_ON`, `META_SHIFT_ON`).
- Unlocks Android's built-in physical keyboard navigation directly from touch glass:
  - **Win + Enter** → Home
  - **Win + Backspace** → Back
  - **Win + Tab** / **Alt + Tab** → Recents / App Switcher
  - **Ctrl + C / V / X / A / Z / Y**, arrow selection, and terminal/IDE control codes
  - **PrtSc** → System Screenshot (via optional Accessibility Service)

### 2. Five Instant-Switch Keyboard Modes
Switch layouts on the fly right from the top mode strip:
- **PC+Num**: Compact 10-column QWERTY paired with a live 4×5 numeric keypad and `NumLock` toggle (switching between digits and `Home`/`End`/`PgUp`/`PgDn`/Arrows).
- **PC (Full)**: 6-row desktop QWERTY with `Esc`, `F1–F12` Function Row toggle, `Tab`, `Caps`, `Win`, `Ctrl`, `Alt`, `Fn`, and Arrow keys.
- **Simple**: Smooth Gboard-style 5-row mobile typing with:
  - Instant key-press preview bubble & zero-latency tap dispatch
  - Long-press accent & symbol popups (`?123` and `=\<` layers)
  - Offline **context-aware 3-suggestion bar** with trigram/bigram next-word prediction
  - **Non-intrusive one-time Auto-Correct** (fixes clear typos once on Space; Backspace reverts and remembers your custom word permanently without fighting edits)
  - Dedicated **Haptic Feedback (Simple Mode only)** toggle so PC, Numpad, and Gaming modes remain silent while mobile typing stays tactile.
- **Num**: Dedicated oversized 4×5 numeric & navigation keypad.
- **Game**: Left-hand `WASD` + `Q/R/E/F/G/C/Shift/Space` gaming cluster (true hold-down key states) paired with a live right-hand **Mouse Joystick**.

### 3. Built-In Hybrid Mouse Joystick & MCPE Camera Look Mode
- Integrated circular **Mouse Joystick + Trackpad** in **Game** mode with adjustable **Touchpad Speed** (`0.6x–3.5x`) and **Split Width** (`30%–55%`), combining 1:1 drag precision with continuous 60Hz analog panning when deflected near the rim.
- **MCPE Camera Look Mode** uses continuous Android 8+ chained accessibility gestures for smooth 3D camera rotation + `LMB` tap / `RMB` hold-use, plus a live floating **Mouse Cursor Pointer** overlay in desktop Mouse Joystick mode.
- **Clean Accessibility Setup**: Accessibility permission powers Mouse Mode, cross-app shortcuts, and non-IME typing **without** spawning an intrusive floating Accessibility human icon on Android 11 navigation bars.

### 4. Dual Delivery: System IME + Persistent Floating Overlay
- Works as a **native Android System IME** (`KeyDeck PC Keyboard`) across all apps.
- Also runs as a **draggable, corner-resizable Floating Keyboard** (`stopWithTask=false`) with:
  - **Floating Transparency** (`0%–75%`) so games, terminals, and videos stay visible behind the keys (while keeping the System IME crisp and solid)
  - Quick **Floating Bubble** launcher (`36dp–68dp`)
  - **Smart IME Suppression**: automatically hides the bottom system keyboard while the floating deck is expanded so two keyboards never stack
  - Instant summon hotkey (**Ctrl + Alt + F** by default)

### 5. Hold-to-Repeat (PC Mode) & Sticky Modifiers
- **Hold to Repeat (PC Mode)**: Holding letters, numbers, or navigation keys in `PC`, `PC+Num`, or `Num` repeats continuously just like a physical mechanical keyboard (enabled by default, toggleable anytime).
- **Sticky Modifiers**: Tap `Ctrl`, `Alt`, `Shift`, or `Win` once on touchscreens to arm the modifier for the next key press, clearing automatically after dispatch.

### 6. Custom Macro Hotkeys & Direct App Launchers
- Create custom multi-modifier shortcuts (`Ctrl`, `Alt`, `Shift`, `Win` + any key) with preset packs for Editing, Browser, Terminal, and Window/System actions.
- Bind keyboard combos directly to **Launch Any Installed App** or **Toggle Floating Deck** from inside any text field.

### 7. Neumorphic Theme Engine & Live Wallpaper Studio (With GIF Support)
- Soft extruded Neumorphic keycaps across **12+ built-in themes** (including `Pure Black #000`, `Midnight Gaming`, `Carbon Stealth`, `Soft UI Slate`, and auto Dark Mode sync) plus a **Custom Color Studio** and **RGB Border Modes** (`Gaming Only`, `All`, `Off`).
- **Live Wallpaper Studio**:
  - 7 built-in geometric/cyber pattern wallpapers
  - Custom **Image & Animated GIF** gallery picker with interactive **drag/pan/zoom crop** preview
  - Real-time **Lightness**, **Saturation**, **Contrast**, **Brightness**, **Color Tint**, and **Keycap Glass Opacity** sliders

### 8. Portable `.kdb` Keyboard Profiles (`@keydeck.aemni`)
- Export your entire setup—key height, deck width, transparency, dark mode, borders, RGB mode, custom hotkeys, and even base64-embedded static or animated **GIF wallpapers**—into a single `.kdb` file in `Download/` and restore or share it in one tap.

### 9. Native Deck Lab
- Built-in low-overhead **Deck Lab** playground to test shortcut combos, hold states, cursor/touchpad coordinates, LMB/RMB clicks, and numpad responses live without leaving the app.
