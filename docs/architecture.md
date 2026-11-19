# Architecture

A guide for contributors. PadBridge is a single-module Android app (`:app`) written in Kotlin. The UI is Jetpack
Compose with Material 3 Expressive; the input surfaces (trackpad, keyboard, gamepad, D-pad, air mouse) are custom
`View`s drawn on a `Canvas` and embedded with `AndroidView`.

```
 touch / sensors / IME          Compose UI (ui/*)              Android Bluetooth
 ───────────────────            ────────────────               ─────────────────
 TrackpadView ─┐                App, ConnectScreen,            BluetoothHidDevice
 KeyboardView  │                SettingsScreen, ...            (HID Device profile)
 ImeCaptureView├──► HidManager ──── hid-io thread ───────────►  sendReport()
 GamepadView   │     (object)    reports built + sent here      ▲
 AirMouseView  │        │                                       │ callbacks on hid-io
 DpadView ─────┘        └── Listener ──► main thread ──► HidUi snapshot ──► Compose
```

## Source layout

`app/src/main/java/dev/padbridge/`

| File | Contents |
|---|---|
| `MainActivity.kt` | Single activity. Permissions, starts the service, window flags (keep screen on, AOD brightness, immersive bars, cutout mode, orientation), volume-key routing for the remote |
| `HidService.kt` | Foreground service (`connectedDevice`) that keeps the HID registration alive in the background; notification with Trackpad / Keyboard / Stop. Also `Perms` (runtime permission lists) |
| `HidManager.kt` | Owns the HID Device profile: registration, watchdog, connect/switch/disconnect, phone-initiated discovery and bonding, refusal detection, and all report building and sending |
| `HidDescriptor.kt` | The composite HID report descriptor and report IDs/lengths |
| `Keys.kt` | USB HID keyboard usages, consumer usages, US-layout character → keystroke mapping, Android key code mapping |
| `HostOs.kt` | `HostOs` (Windows/macOS/Linux), `Combo`, per-OS shortcut lists |
| `Gestures.kt` | `GestureSlot` (every remappable gesture), `GAction` (what a gesture does, per-OS shortcut), defaults and the DeX preset |
| `TrackpadView.kt` | The gesture engine and the trackpad surface; `ClickBar` (left/right buttons) |
| `KeyboardView.kt` | On-screen keyboard: `KbLayout`, unit-grid layouts, modifier state machine, Fn layer |
| `ImeCaptureView.kt` | Bridges the phone's IME (Gboard etc.) to HID keystrokes, Live and Predictive modes |
| `GamepadView.kt` | Touch gamepad, gyro aim / steer |
| `AirMouseView.kt` | Gyro air mouse |
| `Motion.kt` | Gyroscope + gravity expressed as yaw/pitch rates independent of grip and screen rotation |
| `Remote.kt` | `RemoteProfile`, `RemoteOut`, `RemoteSender` (press/hold/release) |
| `DpadView.kt` | Round D-pad with touch-surface swipes |
| `PadTile.kt` | Quick Settings tile |
| `Root.kt` (`src/github`) | Experimental root mode: `su`, `resetprop`/`setprop`, Bluetooth restart, presets (`github` flavor only from 0.8.0) |
| `Settings.kt` | `SharedPreferences` wrapper for every setting |
| `Ui.kt` | `C` (colors for the Canvas views), `fillOrOutline` (AOD drawing), `Haptics`, dp helpers |
| `ui/App.kt` | Top-level navigation (`Dest`), bottom bar / navigation rail, full screen, full display and AOD routing, `HidUi` snapshot |
| `ui/ConnectScreen.kt` | Status card, paired computers, nearby-computer scan, computer type |
| `ui/InputScreens.kt` | Trackpad, Keyboard (+ layout sheet, full display keyboard), Air mouse, Remote, Gamepad screens |
| `ui/SettingsScreen.kt` | Settings, gesture picker, shortcut formatting, About |
| `ui/SetupScreen.kt` | First-run setup flow |
| `ui/RootScreen.kt` (`src/github`; stub in `src/play`) | Root mode screen |
| `ui/Theme.kt` | `PadBridgeTheme`, `AodTheme`, syncing Compose colors into `C` |

## HID descriptor

One composite descriptor is registered with `BluetoothHidDevice.registerApp()` (SDP subclass
`SUBCLASS1_COMBO`). It has four top-level collections, each with a report ID:

| ID | Collection | Input report | Other |
|---|---|---|---|
| 1 | Keyboard | 8 bytes: modifier bits, reserved, 6-key array (usages 0–255) | Output, 1 byte: LEDs (Num, Caps, Scroll, Compose, Kana) |
| 2 | Mouse | 7 bytes: 5 buttons (+3 pad bits), X and Y as signed 16-bit relative, wheel (int8), AC Pan (int8) | Feature, 1 byte: Resolution Multiplier for wheel (bits 0–1) and pan (bits 2–3) |
| 3 | Consumer control | 2 bytes: one 16-bit consumer usage (0–0x3FF) | |
| 4 | Gamepad | 9 bytes: 16 buttons, 4-bit hat (null state) + 4 pad bits, X, Y, Z, Rz (0–255), Brake, Accelerator (0–255) | |

Notes:
- **Resolution Multiplier:** physical max 8. When the host sets the feature (Linux does), `HidManager.scroll()`
  multiplies wheel and pan values by 8, so each unit is 1/8 notch. `onGetReport` answers feature reads with the
  current state.
- **Gamepad buttons** use bits 0 A, 1 B, 3 X, 4 Y, 6 LB, 7 RB, 8 LT, 9 RT, 10 View, 11 Menu, 12 Home, 13 L3,
  14 R3, 15 Share (bits 2 and 5 unused). Triggers are reported both as buttons and as Brake/Accelerator axes (0 or
  255, as the touch triggers are digital). Left stick = X/Y, right stick = Z/Rz.
- **Changing the descriptor means every user must re-pair.** Hosts cache it at pairing time. Bump
  `HidDescriptor.VERSION` whenever the bytes change, and keep it frozen otherwise. It is shown in Settings › About.
- Report lengths in `HidDescriptor` must match what `HidManager` sends and what `onGetReport` replies.

QoS matches Google's WearMouse sample: best effort, token rate 800, bucket 9, peak 0, latency 11250 µs.

## HidManager

`HidManager` is a process-wide `object`. States:
`IDLE → REGISTERING → READY ⇄ CONNECTING → CONNECTED`, plus `BLUETOOTH_OFF`, `NO_PERMISSION`, `UNSUPPORTED`.

### Registration

1. `start()` gets the `HID_DEVICE` profile proxy. If it doesn't connect within 6 s, the state becomes
   `UNSUPPORTED` with the "ROM may disable the profile" message.
2. `onServiceConnected` → `registerApp(sdp, null, qos, ioExecutor, callback)`.
3. **Registration watchdog:** if `onAppStatusChanged(registered = true)` doesn't arrive within 5 s, it calls
   `unregisterApp()` and retries once. If that also gets no answer, it sets `needsBluetoothRestart` and asks the
   user to toggle Bluetooth. This is the stuck-stack case after reinstalling.
4. If `registerApp` returns false (another app holds the exclusive HID Device role), it retries with exponential
   back-off (2, 4, 8, 16 s).
5. Once registered, it connects to a pending target, the device the stack reports as plugged, or the last host if
   auto-reconnect is on.

`HidService` listens for Bluetooth on/off and calls `restart()` / `onBluetoothOff()`.

### Connections

- `connect()` disconnects the current host first (after releasing all keys/buttons), then calls `hid.connect()`.
  A 12 s timeout falls back to `READY` with a re-pair hint.
- **Refusal detection:** a disconnect within 1.5 s of `connect()` without ever connecting sets `hostRefused` and
  shows an OS-specific hint (the Windows HID service steps for Windows).
- **Phone-initiated pairing:** `startDiscovery()` (classic inquiry, about 12 s), then `pairAndConnect()` →
  `createBond()` → on `BOND_BONDED` wait 2 s for the host to read SDP, then `connect()`. LE-only devices are
  rejected because HID Device needs BR/EDR.

### Threading

- **One background thread, `hid-io`** (`HandlerThread`). All report state (mouse buttons, accumulated motion and
  scroll, keyboard modifiers and keys, last gamepad report) is owned by this thread. Public methods (`move`,
  `scroll`, `tap`, `setKeyboardState`, `gamepad`, …) just `post` to it, so touch handling never blocks on Binder.
- **Bluetooth callbacks** are delivered on `hid-io` too (the executor passed to `registerApp`).
- **Listeners** (`MainActivity`, `HidService`) are notified on the main thread. `MainActivity` turns each change
  into an immutable `HidUi` snapshot held in Compose state.
- **Motion coalescing:** pointer deltas and wheel/pan are accumulated as floats and flushed in one report per
  `hid-io` turn; fractional remainders carry over so slow movements aren't lost. Button changes flush pending motion
  first.
- **Gamepad dedup:** identical consecutive gamepad reports aren't sent. Gyro-driven sends are throttled to one per
  9 ms (the link manages roughly 90 reports/s), while sensors run at 200 Hz.
- Chords press modifiers in a separate report before the key, which macOS needs to recognize the chord.

## Gesture engine (`TrackpadView`)

A single `onTouchEvent` state machine with modes `NONE, MOVE, SCROLL, NAV, PINCH, ROTATE, SWIPE, THREE_DRAG,
EDGE_SCROLL_V, EDGE_SCROLL_H, EDGE_SWIPE, DONE`.

- **First down:** records the start point and edge zone (scroll strips, left/right/top edges), and either starts a
  tap-and-drag (second touch within the double-tap window) or schedules the press-and-hold runnable.
- **Extra fingers:** reset the decision (`mode = NONE`) so a resting finger followed by a two-finger scroll works,
  and re-baseline two-finger span and angle.
- **Resting-finger rejection** runs before a mode is chosen; ignored pointer IDs are excluded from averaging.
- **Move:** averages the active fingers' deltas. With one finger, the mode is chosen once the tap slop is exceeded
  (edge scroll, edge swipe, or move). Two fingers choose between rotate (only if a rotate slot is mapped), pinch,
  and scroll/nav based on span change, angle change and travel. Three or more fingers swipe (or drag with
  three-finger drag).
- **Last up:** releases drags, recognizes 1–4 finger taps if nothing moved, and starts momentum for scroll modes.
- **Output:** `movePointer` applies a speed-dependent acceleration curve; `scrollBy` converts px to notches
  (divisor 22, or 30 with hi-res) and applies natural scrolling; `pinch` sends a zoom step per 12 % span change.
- **Mappable actions** go through `fire(slot)` → `Settings.gesture(slot)` → `GAction.perform(os)`, which sends a
  click, consumer usage, or `combo(os)`. A label is shown on the pad, with "not on *OS*" when an action has no
  equivalent.

To add a gesture: add a `GestureSlot`, give it a default in `GAction.default()`, and fire it from `TrackpadView`.
To add an action: add a `GAction`, implement `combo()` for each OS (or a special case in `perform()`), and
`availableOn()` if it doesn't exist everywhere. Update [gestures.md](gestures.md).

## Motion (`Motion.kt`)

Combines the gyroscope with gravity (`TYPE_GRAVITY`, or a low-passed accelerometer) to express rotation as:
- **yaw:** turn rate around the world's vertical axis, whatever the phone's tilt;
- **pitch:** tilt rate around the screen's horizontal axis, flattened so rolling doesn't leak in.

Device axes are rotated into screen axes for the current display rotation. This makes the air mouse and gyro aim
work with the phone held flat like a remote or upright like a camera. Gamepad steering uses the gravity vector's
x component (roll angle).

## UI and theming

- `PadBridgeTheme` wraps `MaterialExpressiveTheme` with `MotionScheme.expressive()`. Color comes from the
  wallpaper (dynamic color, Android 12+) or a built-in scheme. From 0.8.0 the user can choose dynamic color or the
  PadBridge palette, Light/Dark/System, and a pure-black dark theme. Wallpaper colors are the default; the
  PadBridge palette (Android 9-11, or with wallpaper colors off) is generated with Material Color Utilities (see the comment above `BrandLight` in `ui/Theme.kt`).
- The Canvas views read colors from the `C` object. `syncViewColors()` copies the Compose `ColorScheme` into `C` on
  every theme composition, so the views follow theme changes.
- **AOD mode:** `AodTheme` swaps in a black/grey scheme and sets `C.AOD = true`. Views then draw with
  `Paint.fillOrOutline()`, which strokes thin outlines instead of filling. `MainActivity.setAod()` drops window
  brightness to 0.01 and keeps the screen on. `aodPixelShift` moves content a few dp every minute.
- **Full display:** `MainActivity.setFullDisplay()` sets `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`; the trackpad
  reads the display's rounded-corner radius (API 31+).
- Material 3 Expressive components in use: `ShortNavigationBar`, `NavigationRail` (≥ 600 dp), connected
  `ToggleButton` groups, segmented list items, `LoadingIndicator` / `ContainedLoadingIndicator`, `MaterialShapes`,
  `HorizontalFloatingToolbar` on the gamepad, `LargeFlexibleTopAppBar`.
- The launcher icon is adaptive with a monochrome layer for themed icons.

## Settings and state

All settings live in one `SharedPreferences` file (`padbridge`) via `Settings`. Per-OS settings are keyed with the
OS name (`g_<OS>_<SLOT>`, `naturalScroll_<OS>`, `twoNav_<OS>`); keyboard layout and size are keyed per orientation
(`_P` / `_L`). `allowBackup="false"`, so nothing is backed up off the device.

## Build flavors

From 0.8.0 there are two product flavors, `github` and `play`. Root mode lives in the `github` source set
(`app/src/github/java/…/Root.kt` and `ui/RootScreen.kt`); the `play` source set has a no-op `RootScreen` stub, and
`BuildConfig.ROOT_FEATURES` hides the Settings entry. The Play build therefore contains no `su` or system-property
code at all. See [building.md](building.md).

## Debugging

```sh
adb logcat -s PadBridge
```

Logs state changes, registration, `onAppStatusChanged`, resolution-multiplier negotiation and send failures.
