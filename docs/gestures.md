# Trackpad gestures

macOS accepts multitouch only from Apple trackpads, and Windows needs a certified Precision Touchpad, which no one
has gotten working over Android's HID API. So PadBridge recognizes gestures **on the phone** and sends what the
gesture means: a mouse click, a wheel or pan event, a media key, or the keyboard shortcut the host OS binds to that
action.

Set **Computer type** (Connect tab or Settings) correctly: it decides which shortcuts are sent.

## Built-in gestures

These aren't remappable, though most can be switched off in Settings.

| Gesture | Result |
|---|---|
| Move one finger | Move the pointer, with acceleration for fast swipes |
| Tap | Left click (**Tap to click**) |
| Double tap | Double click |
| Tap, then touch again and move | Drag (button held until you lift). Lifting without moving gives a double click |
| Two fingers move | Scroll, vertical and horizontal |
| Flick with two fingers | Momentum scrolling (**Scroll momentum**) |
| Pinch | Zoom: Ctrl+wheel on Windows and Linux, ⌘= / ⌘− on macOS |
| Slide one finger along the right edge | Vertical scroll (**Edge scroll strips**) |
| Slide one finger along the bottom edge | Horizontal scroll (**Edge scroll strips**) |
| Three fingers move | Drag, only with **Three-finger drag** on (this turns off three-finger swipes) |

**Scroll rails:** when a scroll starts clearly vertical or horizontal (1.8× more one way than the other), it stays
on that axis. Turn off in Settings for free scrolling.

**Natural scrolling:** content follows your fingers. Remembered per computer type. Off by default for macOS because
macOS applies its own natural-scrolling setting to wheel input.

**High-resolution scrolling:** if the host enables the HID Resolution Multiplier (Linux does), each wheel unit is
1/8 of a notch, so scrolling is smooth. Other hosts get whole notches.

## Remappable gestures

Every gesture below can be set to any [action](#actions) in **Settings › Gestures on *OS***. Mappings are saved per
computer type. The settings screen also has **OS defaults** (reset) and **Samsung DeX style** buttons.

### Defaults

| Gesture | Windows | macOS | Linux |
|---|---|---|---|
| Two-finger tap | Right click | Right click | Right click |
| Three-finger tap | Middle click | Middle click | Middle click |
| Four-finger tap | Notifications (Win+N) | Spotlight (⌘Space) | Launcher (Super+A) |
| Press and hold, then move | Drag / select text | same | same |
| Two-finger swipe left ¹ | Forward (Alt+→) | Forward (⌘]) | Forward (Alt+→) |
| Two-finger swipe right ¹ | Back (Alt+←) | Back (⌘[) | Back (Alt+←) |
| Rotate clockwise | Nothing | Nothing | Nothing |
| Rotate counter-clockwise | Nothing | Nothing | Nothing |
| Three-finger swipe up | Task view (Win+Tab) | Mission Control (⌃↑) | Overview (Super) |
| Three-finger swipe down | Show desktop (Win+D) | App Exposé (⌃↓) | Show desktop (Super+D) |
| Three-finger swipe left | Switch app (Alt+Tab) | Next Space (⌃→) | Next desktop (Ctrl+Alt+→) |
| Three-finger swipe right | Switch app back (Alt+Shift+Tab) | Previous Space (⌃←) | Previous desktop (Ctrl+Alt+←) |
| Four-finger swipe up | Task view (Win+Tab) | Mission Control (⌃↑) | Overview (Super) |
| Four-finger swipe down | Show desktop (Win+D) | App Exposé (⌃↓) | Show desktop (Super+D) |
| Four-finger swipe left | Next desktop (Ctrl+Win+→) | Next Space (⌃→) | Next desktop (Ctrl+Alt+→) |
| Four-finger swipe right | Previous desktop (Ctrl+Win+←) | Previous Space (⌃←) | Previous desktop (Ctrl+Alt+←) |
| Swipe in from left edge | Widgets (Win+W) | Show desktop (F11) | Launcher (Super+A) |
| Swipe in from right edge | Notifications (Win+N) | App Exposé (⌃↓) | Notifications (Super+V) |
| Swipe down from top edge | Nothing | Nothing | Nothing |

¹ Two-finger horizontal swipes navigate only when **Two-finger swipe for back / forward** is on. It is on by default
for macOS (as on a Mac trackpad) and off for Windows and Linux, where the same swipe scrolls sideways. The direction
follows the content: swiping right reveals the previous page.

Linux shortcuts follow GNOME defaults. KDE and other desktops may bind them differently; remap as needed.

### Samsung DeX preset

Mirrors Samsung DeX's touchpad mode, translated to the host's shortcuts. Gestures not listed keep the OS defaults.

| Gesture | Action |
|---|---|
| Two-finger tap | Right click |
| Three-finger tap | Back |
| Four-finger tap | Launcher |
| Press and hold | Drag |
| Three-finger swipe up | Overview |
| Three-finger swipe down | Show desktop |
| Three-finger swipe left / right | Switch app / switch app back |

## Actions

| Action | Windows | macOS | Linux |
|---|---|---|---|
| Nothing | | | |
| Left / right / middle click, double click | mouse button | mouse button | mouse button |
| Drag (hold left button) | held while you move (press-and-hold only; elsewhere a click) | same | same |
| Back | Alt+← | ⌘[ | Alt+← |
| Forward | Alt+→ | ⌘] | Alt+→ |
| Switch app | Alt+Tab | ⌘Tab | Alt+Tab |
| Switch app (reverse) | Alt+Shift+Tab | ⌘⇧Tab | Alt+Shift+Tab |
| Overview / Mission Control | Win+Tab | ⌃↑ | Super |
| App windows / Exposé | Win+Tab | ⌃↓ | Super |
| Show desktop | Win+D | F11 | Super+D |
| Next desktop / Space | Ctrl+Win+→ | ⌃→ | Ctrl+Alt+→ |
| Previous desktop / Space | Ctrl+Win+← | ⌃← | Ctrl+Alt+← |
| Start / Launcher | Win | ⌘Space | Super+A |
| Search / Spotlight | Win+S | ⌘Space | Super |
| Notifications | Win+N | not available | Super+V |
| Widgets | Win+W | not available | not available |
| Next tab / previous tab | Ctrl+Tab / Ctrl+Shift+Tab | ⌃Tab / ⌃⇧Tab | Ctrl+Tab / Ctrl+Shift+Tab |
| New tab / close tab / reopen tab | Ctrl+T / Ctrl+W / Ctrl+Shift+T | ⌘T / ⌘W / ⌘⇧T | Ctrl+T / Ctrl+W / Ctrl+Shift+T |
| Refresh | F5 | ⌘R | F5 |
| Zoom in / out / reset | Ctrl+= / Ctrl+− / Ctrl+0 | ⌘= / ⌘− / ⌘0 | Ctrl+= / Ctrl+− / Ctrl+0 |
| Rotate right / left | Ctrl+R / Ctrl+Shift+R | ⌘R / ⌘L | Ctrl+R / Ctrl+Shift+R |
| Toggle full screen | F11 | ⌃⌘F | F11 |
| Minimize window | Win+↓ | ⌘M | Super+H |
| Close window | Alt+F4 | ⌘W | Alt+F4 |
| Screenshot | Win+Shift+S | ⌘⇧4 | PrtSc |
| Play/pause, next/previous track, volume up/down, mute | media key | media key | media key |
| Copy / paste / undo | Ctrl+C / V / Z | ⌘C / V / Z | Ctrl+C / V / Z |
| Escape, Enter | key | key | key |

The picker greys out actions that have no shortcut on the selected OS. Rotate is off by default: its shortcuts are
the photo-viewer ones (Windows Photos, macOS Preview), and in a browser Ctrl+R / ⌘R reloads the page. Map it in
Settings › Gestures if you want it.

## Recognition details

For contributors and the curious. Values are in dp unless noted; see `TrackpadView.kt`.

| Parameter | Value |
|---|---|
| Tap slop (movement that turns a touch into a move) | 10 |
| Tap max duration | 260 ms (×1.4 for multi-finger taps) |
| Double-tap window | 300 ms default (Settings), within 60 dp of the first tap |
| Press-and-hold delay | 650 ms |
| Three- / four-finger swipe distance | 70 |
| Two-finger back/forward distance | 90 |
| Edge swipe: start zone / distance | 16 / 60 |
| Scroll strips width | 30 |
| Rotate: start threshold / step per action | 22° / 40° |
| Pinch: start slop / step per zoom event | 28 dp / 12 % span change |
| Momentum decay time constant | 325 ms |

**Resting-finger rejection** (**Ignore resting fingers**, on by default): before a gesture is chosen, a finger that
stays still while another clearly moves, and that wasn't put down at the same time (more than 120 ms apart), is
ignored for the rest of the gesture. Resting fingers are drawn faded.

Android reserves the screen edges for its own back gesture. PadBridge excludes a 200 dp band in the middle of each
side edge (Android caps how much an app can exclude), so edge swipes are most reliable there.
