# Keyboard

The Keyboard tab has two ways to type: your **phone keyboard** (top field) and the **on-screen keyboard** (below).
The trackpad and remote can also open the phone keyboard.

## Phone keyboard

Tap the field at the top to open Gboard, SwiftKey, Samsung Keyboard or whatever you use. Text isn't kept on the
phone; each character is sent to the computer as a key press, and the field only echoes the last 200 characters.

| Mode | How it works | Use it for |
|---|---|---|
| **Live** (default) | Suggestions off. Each key goes out immediately. Backspace, Enter and arrows work as on a real keyboard. | Terminals, games, shortcuts, passwords |
| **Predictive** | The keyboard composes words (glide typing, autocorrect, suggestions). When it changes a word, PadBridge sends backspaces and the corrected text. | Writing prose |

Switch modes in the layout sheet (▦): **Phone keyboard: predictive (glide, autocorrect)**.

In both modes PadBridge asks the keyboard not to learn from what you type (`IME_FLAG_NO_PERSONALIZED_LEARNING`).
In Live mode it also asks for a "visible password" field, so most keyboards show no suggestions. In Predictive mode
the keyboard offers suggestions, glide typing and autocorrect as usual.

A hardware keyboard attached to the phone also types through this field.

## On-screen keyboard

A full keyboard with real key-down and key-up events. Held keys repeat on the computer, and games see them as held.
Labels follow the **Computer type**: Windows (Ctrl, Win, Alt, AltGr), Mac (fn ⌃ ⌥ ⌘, return, delete) or Linux
(Super).

### Modifiers

| Action | Result |
|---|---|
| Tap a modifier | One-shot: applies to the next key, then turns off |
| Tap it twice | Locked until you tap it again |
| Hold it while pressing another key | Chord, like a physical keyboard |

The Caps Lock key has an LED that follows the computer's Caps Lock state.

### Fn layer

Hold or tap **Fn** to reach:

| Key | With Fn |
|---|---|
| 1 … 0, −, = | F1 … F12 |
| ← ↑ ↓ → | Home, PgUp, PgDn, End |
| Backspace | Delete |
| Del (Full layouts) | Insert |
| Esc (65%, Split, Gaming) | ` (grave) |

### Layouts

Tap the layout button (▦) to choose. Portrait and landscape each remember their own layout and size.

| Layout | Contents |
|---|---|
| **Full** | Esc + F-row + Del, number row, letters, modifier row with arrow cluster |
| **Full + numpad** | Full, plus PrtSc, Ins, PgUp, PgDn and a full number pad. Best in landscape or on tablets |
| **65%** | No F-row; Del, PgUp, PgDn, End column on the right; Fn + number = F-key; Fn + Esc = ` |
| **Split** | The 65% halves pushed to the screen edges for two thumbs. The gap is adjustable |
| **Gaming** | Large Esc–6 / Tab–T / Caps–G / Shift–B block with Ctrl, Alt and a wide Space; on the right F5, F9, Backspace, I, M, Enter, J, K, L and arrows |
| **Numpad** | Number pad only, with Esc, Tab, Backspace, Del and = on the side |

### Size and options

In the layout sheet:

- **Width** 40–100 % and **alignment** left / center / right.
- **Split gap** (Split layout), in key widths.
- **Height:** drag the bar above the keys. Double-tap the bar to fill the space again. **Reset size** restores both.
- **Shortcut and media rows:** a per-OS shortcut strip and media keys above the keyboard. On by default in
  portrait, off in landscape.
- **Key preview bubbles:** a pressed character key shows its label above your finger.

### Full display keyboard

The expand button (⤢) puts the keyboard on the whole screen. A handle at the top opens Exit and Layout.

## Shortcut strip

Per-OS shortcuts, sent as one press:

| | Windows | macOS | Linux |
|---|---|---|---|
| OS-specific | Start, Alt+Tab, Task view, Desktop, Snip, Explorer, Search, Emoji, Task Mgr, Close app, Lock | Spotlight, ⌘Tab, Mission Ctrl, Quit, Hide, Shot area, Shot menu, Emoji, Force quit, Lock | Super, Alt+Tab, Terminal, Run, Close win, Screenshot, Lock |
| Common | Copy, Paste, Cut, Undo, Redo, Select all, Find, Save, New tab, Close tab, Reopen tab | same, with ⌘ | same |

The media row has previous, play/pause, next, volume down/up, mute, and brightness down/up (brightness keys work
where the host maps the HID consumer brightness usages, typically laptops).

## Host keyboard layout

PadBridge sends USB HID key codes, which the computer interprets with its own keyboard layout. Typing from the phone
keyboard assumes the computer uses a **US (ANSI) layout**:

- Characters a US layout can't type (accented letters, €, emoji, …) are skipped, and a message says how many.
- Typographic quotes, dashes and non-breaking spaces from the phone keyboard are folded to ASCII.
- If the computer uses another layout (e.g. German or French), letters and symbols may come out wrong. Add a US
  layout on the computer and switch to it while using PadBridge.

The on-screen keyboard sends physical key positions, so it follows whatever layout the computer uses, but its
labels show US positions.

## Trackpad in the free space

When the keyboard doesn't use the whole area, the free space becomes a compact trackpad with the same gestures.
- **Shorter keyboard:** drag the bar above the keys down, and the area above it turns into a trackpad.
- **Narrower keyboard aligned left or right:** set the width (for example 45%) and alignment in the layout sheet,
  and a trackpad fills the rest of the row. Numpad on the right with a trackpad on the left works well in
  landscape for spreadsheets.

Turn it off with **Trackpad in free space** in the layout sheet (▦).
