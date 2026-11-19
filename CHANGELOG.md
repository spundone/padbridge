# Changelog

All notable changes to PadBridge are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Versions before 0.8.0 were private test builds. Their history is reconstructed, grouped by minor version, and
undated.

## [1.3.2] – 2026-11-19

### Added
- Palm rejection for the keyboard's trackpad, like a laptop's. A touch that lands on the trackpad within a moment of
  typing is ignored until it lifts, a hand resting still while you type no longer taps or long-presses, and
  palm-sized contacts are ignored on the trackpad and the keys. On by default; switch it off in Settings ›
  Trackpad or in the laptop section of the keyboard layout sheet.
- Settings › About: **What's new**, the changelog for every version in the app, and an About the developer card
  with a UPI donation (GitHub build only).

## [1.3.1] – 2026-11-19

### Added
- Keyboard layout sheet: a **Windows / macOS / Linux** switch at the top, so you can change the keys, labels and
  shortcuts without leaving the keyboard (it sets the computer type app-wide).
- Laptop deck: **resize the keyboard on screen**. Grips under the keyboard's bottom corners change its width (it
  stays centered), the middle of the bar changes its height; double-tap the bar to reset both. The sheet adds a
  **Keyboard height** slider next to the width slider (width now goes down to 40%).

## [1.3.0] – 2026-11-19

### Added
- **Adjustable laptop deck** (tablets): drag the bar under the keys to resize the keyboard; under the trackpad,
  drag the middle grip to move it and the end grips to make it wider or narrower (double-tap to reset). The layout
  sheet adds keyboard width, a **full-width trackpad**, **Lock layout** to hide the handles, and **Reset deck**.
  Everything is remembered per orientation. The deck is centered on the screen itself rather than on the space beside the navigation
  rail, so the keyboard sits in the middle of the tablet.
- **S Pen and stylus support** on every trackpad (on by default; Settings › Trackpad): hover moves the pointer,
  touching clicks or drags, the pen button (or the eraser end) right-clicks, and fingers are ignored while the pen
  is near the screen, so you can rest your palm.

- **Dim when idle:** on the trackpad, remote and air mouse, PadBridge switches to the dim outline (AOD) mode
  after a minute without a touch while connected; a tap brings the normal screen back. Settings › General ›
  Dim to outline mode when idle (never, 15 s – 5 min). AOD turned on by hand stays on.
- **Phone** keyboard layout: ten columns with big keys like a phone keyboard; Fn gives F1–F12, the symbols, and
  Home/End/PgUp/PgDn.
- README screenshots (phone and tablet), captured with a debug-only demo mode that shows no real device names.

### Changed
- **Pure black is the default** dark theme (Settings › Appearance to turn it off).
- On phones in portrait the keyboard no longer stretches to fill the screen with tall, skinny keys: it stays short
  with the trackpad above it (drag the bar to resize as before).

### Fixed
- The "Nearby devices" sheet (setup wizard and Connect tab) now closes by itself once a device connects.
- Car head units get a car icon, and when a car refuses the keyboard and mouse connection PadBridge explains why
  (Android Auto takes input only from the car's own controls) instead of suggesting a re-pair.
- Updates: cancelling Android's install prompt keeps the update banner so you can retry; an old "couldn't reach
  GitHub" message clears when you come back to the app; the banner's buttons are easier to read.

## [1.2.1] – 2026-11-19

### Fixed
- GitHub build: **Check for updates** said "Couldn't reach GitHub" when the phone had turned network access off
  for PadBridge. Some ROMs do this for apps installed without the internet permission (1.1.0 and older) and keep
  it off after the update. PadBridge now detects it and the row opens App info, where **Allow network access**
  turns it back on. Other failures name the actual cause (offline, timeout, GitHub error).

## [1.2.0] – 2026-11-19

### Added
- **In-app updates.**
  - GitHub build: checks GitHub releases once a day (or from Settings › About › Check for updates), shows a
    banner on the Connect tab with the release notes, downloads the APK, verifies its SHA-256 checksum and signing
    key, and installs it. A notification brings you back after the update. The daily check can be turned off. This
    adds the `INTERNET` and install permissions to the GitHub build only.
  - Play build: Google Play in-app updates (flexible): Play downloads in the background, then **Restart** applies
    it. The Play build still has no internet permission.

### Fixed
- **Pairing with TVs, phones and tablets:**
  - Paired TVs, set-top boxes, phones and tablets now show in the Connect list (it used to show computers only,
    so a TV vanished after pairing). Anything PadBridge has paired with or connected to is always listed, with a
    matching icon.
  - Pairing from the phone waits for the scan to stop before bonding; bonding during a scan failed or stalled on
    many phones.
  - A pairing request the other device never answers now gives up after 45 s with advice, instead of hanging.
  - When a TV or phone pairs from its own Bluetooth settings while PadBridge's pairing screen is open (or the phone
    was just made visible), PadBridge connects the trackpad and keyboard itself if the other side doesn't.
  - "Find nearby devices" lists TVs and phones next to computers, with TV pairing tips.
  - When a TV pairs but hangs up on the keyboard and mouse connection (common: many TVs accept keyboards only from
    their own accessory menu), PadBridge now says so and explains the TV-side pairing route instead of the
    computer advice. TVs that misreport their Bluetooth class are recognized by name.

## [1.1.0] – 2026-11-19

### Added
- **Laptop deck** keyboard mode: the keyboard across the top with a trackpad centered below it, like a laptop's
  palm rest (Esc and F-row included, full gesture engine on the pad). Turn it on in the keyboard layout sheet; it is
  the default on tablets in landscape.
- **Tablet layouts:**
  - Tablets keep the navigation rail and top bar in landscape (phones still go edge to edge).
  - Connect splits into two panes on wide screens: status and computer type | computers and pairing.
  - Settings is list-detail on wide screens: categories on the left, the open page on the right.
  - The remote stays remote-sized and centered instead of spreading across the screen; the side-by-side
    landscape layout is only used on short (phone) screens.
  - On tablets the keyboard stops at about half the screen width in height so keys stay key-shaped, and the
    space above becomes trackpad.
- **Material 3 Expressive motion:**
  - Switching tabs slides a little toward the tab you picked, on the expressive spring.
  - Settings pages slide in and back.
  - The status pill animates its color, size and text when the connection changes.
  - The Connect status card animates its color and size; its badge turns slowly and pops in with a spring when a
    computer connects.
  - Computers in the list animate in and out.
  - Trackpad taps leave a ring ripple (larger for multi-finger taps).
  - The Find nearby computers button morphs shape when pressed.

### Changed
- The remote's computer profile follows the computer type: macOS → Mac, Windows / Linux → PC (TV and Slides
  profiles are untouched). A Mac/PC profile saved before this is fixed once on update.

## [1.0.2] – 2026-11-19

### Changed
- **Rebuilt PadBridge palette** (used on Android 9–11, or with wallpaper colors off): generated with Material Color
  Utilities, with a saturated blue primary, blue-tinted surfaces instead of flat grey, a matching blue secondary
  and a coral accent.
- **Connection status shows once.** The top bar's status pill is the only connection indicator on the input tabs;
  the in-screen computer chip now appears only where the top bar is hidden (full screen, landscape, AOD), and the
  "Not connected" banner only when it offers a one-tap reconnect or an error fix. The pill is hidden on the Connect
  tab, whose status card says the same thing.
- **Minimal remote, Siri Remote style.** The clickpad is one flat disc with four small direction dots and no "OK"
  label; pressing lights the wedge softly and swiping shows a glow under your thumb. Below it, identical round
  icon-only keys in two columns: Back | Home, Play/Pause | a tall volume rocker, Mute. Keys shrink and brighten
  when pressed instead of flashing the accent color. The panel behind the controls is gone. Previous / Next moved
  to the row of extra buttons (on Slides, the clickpad's left / right already step slides).
- **Text dedupe pass** (checked on a phone connected to a Mac):
  - Connect: the computer's name appears once (status card reads "Connected" above it, without a second
    "Connected to …" line); the connected computer is no longer repeated in the list, which becomes "Other
    computers"; the mode tiles that repeated the navigation bar and the settings shortcut that repeated the gear
    are gone.
  - Setup: once connected, the pairing step just says "Connected" instead of a broken-up name followed by pairing
    steps; macOS is no longer subtitled "macOS"; the tips point at the trackpad's ⋮ menu for the gesture guide.
  - Settings pages no longer repeat their title as the first section header ("General" section → "Behavior").
  - "Full screen" vs "Use the whole screen" were easy to confuse: now "Hide app bars" and "Trackpad / Keyboard
    only, edge to edge".
  - Gamepad: the Xbox Guide button shows a home glyph instead of an "X" next to the X face button.

### Fixed
- Remote in landscape: the clickpad collapsed to a dot and Mute to a sliver. Landscape now puts the profile chips
  in the toolbar row and the keys beside the clickpad, with the volume rocker lying sideways.

### Developer
- Debug builds install as `dev.padbridge.dev` ("PadBridge Dev") next to the release app.

## [1.0.1] – 2026-11-19

### Fixed
- When a computer doesn't answer, PadBridge no longer shows "didn't respond… remove the pairing and pair again"
  partway through its retries. A timeout left over from the first attempt fired on top of them. The message is
  now consistently "isn't answering… it can also connect to this phone by itself", which doesn't wrongly suggest
  re-pairing.

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
