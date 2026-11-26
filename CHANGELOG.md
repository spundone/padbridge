# Changelog

All notable changes to PadBridge are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Versions before 0.8.0 were private test builds. Their history is reconstructed, grouped by minor version, and
undated.

## [Unreleased]

## [1.10.0] – 2026-11-26

### Changed
- Two GitHub APKs. `PadBridge-vX.apk` no longer includes the accessibility service, so Android's fraud protection
  no longer blocks installing it from a browser or file manager ("App blocked to protect your device").
  `PadBridge-vX-control.apk` keeps it, for "Control this phone" without root; install it with ADB or an app store
  such as Obtainium. Each updates to its own APK, and either installs over the other. Updating from 1.9.0 in the app
  moves to the standard APK.
- Setup's pairing step follows the computer type: Macs, Linux and TVs pair from their own Bluetooth settings after
  "Make this phone visible" (what works best for them), Windows from "Find nearby devices", with the step to turn
  on HID for the phone shown up front.

### Fixed
- Setup now says when pairing didn't complete or the computer refused or didn't answer, instead of showing nothing.
- Pairing from the phone: tapping a computer that's already mid-pairing waits for it instead of failing, there's
  more time to accept the code (Windows asks in a notification), PadBridge no longer starts a second connection
  while the computer is already connecting in, and a computer that refuses the first connection right after pairing
  gets one retry.

## [1.9.0] – 2026-11-24

### Changed
- Setup is shorter and smarter: one Allow button asks for the permission and moves on by itself, picking a
  computer type moves on, pairing is three numbered steps with troubleshooting folded away, a connection moves on
  by itself, and the last step lets you pick where to start (trackpad, keyboard, remote, gamepad, air mouse or
  drawing tablet). On tablets it sits in a readable column.
- Problems say what's wrong and offer the fix: "Permission needed" with Allow (or the app's settings page once
  Android stops asking), "Bluetooth off" with Turn on, "Stopped" with Start, instead of "Needs attention" and
  "Fix".
- Settings are regrouped: a new Keyboard page (word suggestions, TrackPoint, key rollover) and Screen page (keep
  awake, dim when idle, full screen, Do Not Disturb); General comes first.
- Search settings by name or by what they do ("dark", "vibration", "nkro").
- Each mode's ⋮ menu links straight to its settings, and Back returns to the mode. The trackpad menu is grouped
  into view, other modes, help and rotation. The air mouse has a back button.
- The last-used computer is listed first.

### Fixed
- Skipping setup no longer leaves PadBridge stopped with no explanation.
- Revoking Nearby devices in the phone's settings no longer crashes the app when Android restarts its service.

## [1.8.0] – 2026-11-20

### Added
- Drawing tablet:
  - **Palm rejection:** while the S Pen is in use (and for a moment after), and for palm-sized touches, fingers
    don't draw, so your hand can rest on the glass.
  - **Express keys** beside the area (left when sideways, below upright), like a Wacom's: undo, redo, brush
    smaller / bigger, eraser, pan (holds Space) and zoom out / in, with the right shortcuts for Mac or
    Windows / Linux.
  - **Finger gestures:** two fingers pan, pinch zooms, a two-finger tap undoes, a three-finger tap redoes. A stroke
    starts once the finger moves, so a second finger becomes a gesture instead of a dot.
  - **Pressure curve:** Soft, Linear or Firm.
  - The area starts centered and as large as fits.

### Changed
- Tablets: the remote uses the screen. Sideways, the clickpad and the buttons sit side by side instead of a
  phone-width column in the middle; the clickpad, swipe pad and buttons are bigger in both orientations.
- Tablets: the gamepad keeps phone-sized controls near the bottom corners, where your thumbs are when you hold a
  tablet, instead of scaling the sticks up with the screen.

## [1.7.0] – 2026-11-20

### Added
- About › Other apps: PadKeys, PadBridge's keyboard as your phone's own keyboard.
- Adjustable transparency for the floating trackpad: the half-circle button on its bar cycles 100, 80, 60 and 40%,
  or set any level with the slider under "Floating trackpad" on the Connect screen. Apps underneath show through.
- Haptics on tablets and phones with simple vibration motors (like a Galaxy Tab): Settings › General › Haptic
  feedback now has a style, Auto, Rich or Basic. Basic plays plain pulses that simple motors can feel (the crisp
  preset ticks did nothing on them), with light textures spaced out so they patter instead of buzzing; Auto picks by
  what the motor supports. Devices with no motor at all can play a quiet click sound instead.

## [1.6.0] – 2026-11-20

### Changed
- Region screenshots with the trackpad: after Cmd+Shift+4 (or 5) on a Mac, Win+Shift+S on Windows or Print Screen
  on Linux, from the keyboard, a shortcut chip or the Screenshot gesture, the next one-finger touch on the trackpad
  holds the click: drag to select, lift to take it.
- Keyboard shortcuts (anything with Cmd, Ctrl, Alt or Windows held) no longer count as typing, so palm rejection
  doesn't ignore the trackpad right after one.

### Added
- **Word suggestions on PadBridge's own keyboard** (keyboard options): a strip above the keys offers the three most
  likely words as you type; tap one to replace the word on the computer. Completions come from a built-in list of
  the 40,000 most common English words, corrections from your phone's spell checker. Optionally they also learn the
  words you type, kept only on this phone (off by default, with "Forget learned words").
- **Drawing tablet** (trackpad ⋮ › Drawing tablet): an area of the phone maps onto the computer's whole screen,
  like a Wacom. Pick the display's ratio (16:9, 16:10, 3:2, 4:3, 21:9, 32:9, portrait, custom, or stretch over the
  whole pad), then move and resize the area. An S Pen hovers (the pointer follows above the glass), presses with
  pressure, right-clicks with its button and erases with the eraser end. A finger either draws, or hovers (move to
  point, tap to click, double-tap and hold to drag, two-finger tap for right click). Sent as a pen with pressure
  (Windows Ink, Linux) or an absolute mouse (macOS); Auto picks by computer type. Turning it on needs a one-time
  re-pair; in Control this phone it works right away.
- Subtle haptics on the keyboard and TrackPoint: a light detent for each space bar cursor step, a tick as a flick
  switches to the shifted character, a firmer click when a modifier locks; on the TrackPoint a soft touch, faint
  detents as you push harder, a ratchet tick per scroll notch and a tick for tap-to-click. They use the phone's
  lightest vibration (low tick) where the motor supports it, and follow the Haptic feedback setting.
- The same feel on the other screens: a ratchet tick per wheel notch while scrolling on the trackpad (fainter while
  it coasts) and on the air mouse wheel, a soft tick per zoom step and when tap-and-drag picks something up, light
  detents for each step on the remote's clickpad and swipe pad, and on the gamepad a faint tick as a stick leaves
  center and a halfway detent on the triggers.

## [1.5.0] – 2026-11-20

### Added
- **Control this phone** (GitHub build, Connect › Without Bluetooth): no Bluetooth needed. The trackpad, keyboard
  and remote drive this phone, or the external display it's showing (desktop mode, Samsung DeX, an extended
  display), with a pointer drawn on that display. With root it sends real mouse and keyboard events (hover, right
  click, every shortcut); without root the "PadBridge: control this phone" accessibility service turns clicks,
  drags and scrolling into touches and types into the focused field (Esc = Back, Windows / Command = Home,
  Alt / Cmd + Tab = Recents, Ctrl / Cmd + A, C, X, V). Media and volume keys control this phone. You can pick the
  display.
- **Floating trackpad** (GitHub build): a small trackpad over other apps; drag the bar to move it, the corner to
  resize, the dash to fold it into a bubble. It drives the Bluetooth computer or, in Control this phone, this phone.

### Fixed
- Auto AOD no longer dims the screen while you type with the phone keyboard (Gboard).

## [1.4.0] – 2026-11-20

### Added
- **Realistic** appearance (Settings › Appearance › Theme), replacing Mixed. The pads are drawn as hardware: in
  light, a silver aluminum keyboard deck with white key caps that press down, a frosted-glass trackpad and a silver
  remote with raised white buttons; in dark, the same in space black. It follows the system's light or dark mode,
  with plain neutral greys behind. Caps Lock and latched modifiers get a small green light.
- Incognito button (eye icon) next to the remote's text field (Android TV and the other remotes). Off, the phone keyboard
  learns and suggests as usual; Direct typing moves to Suggest for that, still typed live.
- AOD mode on the keyboard too: black screen, outlined keys and trackpad, a clock with the connection, dimmed
  brightness. The moon button or Back leaves it, and the idle timer can turn it on as on the trackpad.

### Changed
- The remote stays in portrait on phones (tablets still rotate).
- Phone landscape trackpad: the toolbar is a slim column at the side and the status bar hides (swipe from the
  edge to see it), so the pad gets nearly the whole height instead of about two thirds.
- "Keyboard only, edge to edge" keeps the Keyboard tab's layout: your keyboard height, the trackpad in the space
  above it (phones in portrait), and the drag bar to resize, instead of stretching the keys over the whole screen.
- Root mode: Controller identity now warns that it applies to the whole phone, so a computer that pairs
  while it's set sees only a game controller (no trackpad or keyboard; on a Mac, taps open the Games app).
- About: the developer card shows my photo and links to X (@spandu500) next to GitHub.

## [1.3.5] – 2026-11-20

### Added
- TrackPoint sensitivity slider (keyboard options).
- Voice typing on the remote: open the keyboard and tap the mic; what you say is typed on the TV or computer.
- Do Not Disturb in full screen (Settings → Air mouse & gyro, next to "Hide app bars"): silences notifications while the app bars are hidden, and only
  while PadBridge is in front. Your own Do Not Disturb is left alone.
- A notice when the phone's own three-finger screenshot swipe takes a trackpad gesture, saying how to turn it off.

### Changed
- Android TV remote is calmer: one remote picker instead of a row of chips, D-pad / swipe switch in the toolbar,
  power joins the key rows, and Previous / Next / Rewind / Fast forward / Menu move to the ⋮ menu.

### Fixed
- Crash with three- and four-finger gestures when the phone's own gesture (such as a three-finger screenshot swipe)
  took over the touch.

## [1.3.4] – 2026-11-19

### Added
- **Custom colors** (Settings › Appearance › Colors): pick one color from swatches or hue, saturation and
  brightness sliders, and the whole light and dark palette grows from it, like wallpaper colors.
- **Two-tone**: the whole app in just two colors you pick, one for backgrounds and one for text, keys and accents,
  with a contrast check, a swap button and presets.
- **Mixed** theme: a light app with the keyboard, trackpad and gamepad in dark, like a laptop's dark keys.
- **Blur and transparency**: see-through sheets, dialogs and menus, with the app blurred behind them on Android 12+.
- **Indirect typing** for the phone keyboard: write with everything Gboard (or your keyboard) can do, including
  suggestions, glide typing and voice typing, edit it, then Enter types it all on the computer and presses Enter.
  Direct (no suggestions) and Direct with suggestions are still there, in the keyboard sheet.
- The phone keyboard can now learn from what you type (off by default, which keeps Gboard in incognito).
- **Google TV-style remote** for Android TV: power, a D-pad or a swipe pad (swipe to move, tap to select, hold to
  long-press), Back, Home and Assistant, mute, volume and play / pause.
- TrackPoint on the Laptop and Full keyboard layouts too (tap it to click, hold Fn and push to scroll); it can be
  turned off in the keyboard sheet.

### Changed
- Pure black (on by default in dark mode) keeps the theme's color in cards, keys and pads instead of flat greys.

### Fixed
- On-screen keyboard keys could get lost on a slow Bluetooth link, such as a TV's: a quick tap was already released
  by the time its key report was built, so nothing was sent.

## [1.3.3] – 2026-11-19

### Added
- **Laptop** keyboard layout, now the default: five rows like Windows' traditional touch keyboard, with the F-keys on
  Fn + the number row (and ` on Fn + Esc), so every key is about 20% taller. The six-row Full layout is still there.
- **ThinkPad** keyboard layout: low F-row with Home, End, Ins and Del, Fn bottom-left, PrtSc beside the right Ctrl,
  half-height arrows with PgUp and PgDn, and a red **TrackPoint** between G, H and B with its three buttons under the
  space bar. Push the TrackPoint to move the pointer (further = faster), tap it to click, and hold the middle
  button while pushing it to scroll.
- Flick up on a key to type its shifted character: `!` from 1, `:` from ;, capitals from letters. Keys that can be
  flicked type when you lift your finger, or turn into a normal held key (with key repeat) if you hold them.
- Drag the space bar to move the cursor: left and right (and up and down) send arrow keys. A tap still types a
  space.
- Both can be turned off in the keyboard sheet, and are off on the Gaming and Numpad layouts.
- **N-key rollover** (Settings › Trackpad, opt-in): the keyboard reports every held key instead of at most six, for
  big chords and games. It changes the Bluetooth HID descriptor (v3, or v4 with the native touchpad), so remove
  and re-pair the phone on the computer once after turning it on.

### Fixed
- Laptop keyboard: you can now type with your hands resting on the trackpad. Keys pressed while anything touched
  the trackpad were lost (and could reach the trackpad as a second finger, scrolling or right-clicking), because
  every finger went to whichever of the two was touched first. Fingers now go to the keyboard, trackpad or click
  button they land on, which also fixes Shift-click and clicking with the Left / Right buttons while moving.
- Palm rejection: typing now ends anything resting on the trackpad, including a palm that had turned into press
  and hold (which held the mouse button down), and press and hold is off on the keyboard's trackpad while palm
  rejection is on.

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
