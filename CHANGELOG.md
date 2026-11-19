# Changelog

All notable changes to PadBridge are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Versions before 0.8.0 were private test builds. Their history is reconstructed, grouped by minor version, and
undated.

## [1.0.0] – 2026-11-19

First public release.

### Changed
- **Calmer layout:**
  - Settings is a short list of categories (Trackpad, Gestures, Air mouse & gyro, Appearance, General, About), each
    on its own page.
  - The bottom bar has five tabs; Air mouse moved into the trackpad's ⋮ menu.
  - Pairing help on the Connect screen is folded into "How pairing works".
- After a computer connects from the Connect tab, PadBridge opens the trackpad.
- The trackpad's top row shows the connected computer instead of the OS name.

## 0.9.1

### Changed
- When a computer doesn't answer a connection attempt (Bluetooth page timeout, e.g. a sleeping PC), PadBridge
  retries twice. After that it explains that the computer can also connect in by itself, instead of reporting a
  failure.
- The Computers list shows one row per computer. A PC bonded twice (classic plus an LE bond resolving to the same
  machine) no longer appears as two "STARLABS" rows, one Connected and one offering Switch.

## 0.9.0

### Added
- **Gamepad presets**, chosen from the gamepad toolbar:
  - **Racing:** tilt-to-steer, analog gas and brake pedals, gear buttons.
  - **First person:** floating move stick, drag-to-look surface plus gyro aim, large Fire and Aim, and named
    action buttons.
  - **Third person:** move stick, camera surface, full face buttons and shoulders.
  - **Retro:** big D-pad and face buttons.
  - **Standard:** the previous layout.

  Each preset sets a sensible gyro mode. The Xbox / PlayStation labels moved into the same menu.
- **Native touchpad (beta):** optional Windows Precision Touchpad / Linux multitouch descriptor (v2), so the computer
  does gestures itself. Off by default; turning it on needs a re-pair. See
  [docs/native-touchpad.md](docs/native-touchpad.md).
- The HID descriptor is logged at registration (`adb logcat -s PadBridge`) for debugging.

## 0.8.1

### Added
- Keyboard: the space the keyboard leaves free becomes a trackpad, either above a shortened keyboard or beside a
  narrower keyboard aligned left or right (e.g. numpad + trackpad). Toggle: **Trackpad in free space**.

### Fixed
- The navigation rail scrolls when all six tabs don't fit (phones in landscape).
- While Bluetooth is starting, the "not connected" banner shows progress instead of a Fix button.

## 0.8.0

Launch preparation for the public GitHub release and Google Play.

### Added
- `github` and `play` build flavors. Root mode is included only in the `github` flavor.
- Theme settings: dynamic color or the PadBridge palette; Light, Dark or follow System; optional pure-black dark
  theme.
- Overflow menus on the input screens, so toolbars stay uncluttered.
- About screen.
- Public documentation, privacy policy and contributor guide.
- Apache-2.0 license.

- "Not connected" banner on the Trackpad, Keyboard and Remote screens, with one-tap reconnect to the last
  computer.
- GitHub Actions: build + lint on every push, and signed releases (APK + Play bundle) on `v*` tags. Release builds
  refuse to use the public dev key.
- Issue templates.

### Changed
- targetSdk 36, predictive back.
- Remote: controls sit on a rounded "remote body" panel. OK uses the accent color, and the connection chip shows
  only while connected.
- Rotate gestures now default to Nothing (Ctrl/⌘+R reloads browser pages).
- The phone keyboard is asked not to learn from what you type, in both typing modes.
- Root mode: turning the speaker off restores the ROM's original property values instead of forcing them off.
- Clearer Connect status text, solid-colour status icon, and the trackpad's hint text no longer repeats the
  connection banner.

### Fixed
- A connection attempt that fails to start no longer gets a delayed "didn't respond" message on top.
- A virtual-cable unplug from another device no longer resets the current connection status.

## [0.7.0]

### Added
- First-run setup flow: welcome, permissions with explanations, computer type, pairing, tips.
- Adaptive navigation rail on wide screens (600 dp and up).
- New adaptive launcher icon with a monochrome layer for themed icons.
- Experimental root mode: Bluetooth speaker (A2DP sink), device class and controller identity, set through
  Bluetooth system properties.

## [0.6.0]

### Added
- Remote tab: a TV-remote D-pad with profiles for Android TV, Apple TV, Mac, PC and Slides. Arrow keys repeat
  while held, swipe across the center for touch-remote navigation, transport and volume buttons, per-profile extra
  buttons, phone keyboard for search boxes.
- Phone volume buttons can control the host's volume, or change slides in the Slides profile.
- AOD mode on the Remote screen.

## [0.5.2]

Covers 0.5.0 – 0.5.2.

### Added
- On-screen keyboard layouts: Full, Full + numpad, 65%, Split, Gaming and Numpad.
- Resizable keyboard: drag to set height, width 40–100 %, left/center/right alignment, split gap; remembered per
  orientation.
- Key preview bubbles; option to hide the shortcut and media rows.
- Full display keyboard.
- **Find nearby computers**: phone-initiated pairing (scan, bond, connect). Lists classic Bluetooth devices only,
  since an LE bond can't carry HID.
- Windows refusal detection: a connection dropped within 1.5 s shows how to enable the HID service for the phone in
  Control Panel.

## [0.4.2]

Covers 0.4.0 – 0.4.2.

### Added
- AOD mode for the trackpad: thin outlines on true black at minimum brightness, clock in the title chip, pixel
  shift against burn-in.
- Full display trackpad: edge to edge, under the camera cutout, following the screen's rounded corners, with a
  folding floating toolbar.
- Air mouse: gyro pointing with tap to click, hold-to-drag and a scroll strip; works held flat or upright.
- Gamepad gyro: aim (right stick) and steer (left stick).

## [0.3.2]

Covers 0.3.0 – 0.3.2.

### Added
- Samsung DeX-style trackpad behavior: press and hold, then move to drag; resting-finger rejection; a DeX gesture
  preset; adjustable double-tap timing; trackpad rotation lock; gesture guide.
- Edge scroll strips, scroll rails, edge swipes, optional three-finger drag.
- Quick Settings tile for the trackpad; Trackpad and Keyboard buttons on the notification.
- Registration watchdog: detects when Android's Bluetooth stack accepts the HID registration but never confirms
  it, retries once, then asks the user to toggle Bluetooth.

## [0.2.0]

### Changed
- Rewritten UI in Jetpack Compose with Material 3 Expressive: dynamic color, expressive motion, short navigation
  bar, connected button groups, segmented lists, loading indicators.

### Added
- Mappable gestures: every multi-finger tap, swipe, rotate and edge gesture can be assigned an action, per host OS.

## [0.1.0]

### Added
- First build: the phone registers as a Bluetooth HID device using Android's HID Device profile.
- Trackpad, keyboard and gamepad modes.
- UI built with Android Views.
