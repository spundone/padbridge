# Google Play listing

Copy for the Play Console. Character limits are Play's; counts were checked when this file was written. Re-check
after editing.

## Store listing

**App name** (30 max, 30 used)

```
PadBridge: Trackpad & Keyboard
```

**Short description** (80 max, 72 used)

```
Use your phone as a Bluetooth trackpad, keyboard, TV remote and gamepad.
```

**Full description** (4000 max, about 3378 used)

```
PadBridge turns your Android phone into a Bluetooth trackpad, keyboard, TV remote, air mouse and gamepad for Windows, macOS, Linux and TVs.

The computer sees a normal Bluetooth keyboard and mouse, so there is nothing to install on it: no app, driver or server. Pair once, then tap Connect.

TRACKPAD
• Move, tap to click, double tap, tap-and-drag
• Two-finger scroll with momentum, pinch to zoom, rotate
• Three- and four-finger swipes and taps for Task view, Mission Control, desktops, app switching and more
• Every gesture can be remapped, separately for Windows, macOS and Linux
• Samsung DeX-style press-and-hold drag, resting-finger rejection and a DeX gesture preset
• Edge scroll strips, optional click buttons and three-finger drag
• Full display mode uses every pixel of the screen
• Native touchpad (beta): on Windows and Linux the computer can read real finger positions for its own gestures and smooth scrolling
• AOD mode: dim outlines on black at minimum brightness, for long sessions

KEYBOARD
• Type with your own phone keyboard: Live mode sends every key instantly, Predictive mode supports glide typing and autocorrect
• Full on-screen keyboard with real key presses, so held keys repeat and work in games
• Layouts: Full, Full + numpad, 65%, Split, Gaming and Numpad
• Resize, narrow and align the keyboard; portrait and landscape remember their own setup
• Windows, Mac or Linux key labels, a Fn layer, one-tap shortcuts and media keys

TV REMOTE
• D-pad with profiles for Android TV, Apple TV, Mac, PC and presentation slides
• Hold arrows to repeat, or swipe across the center like a touch remote
• Phone volume buttons control the TV or computer volume, or change slides

AIR MOUSE
• Point and turn the phone to move the pointer, using the gyroscope
• Tap to click, hold to drag, scroll strip

GAMEPAD
• Two analog sticks, D-pad, ABXY with Xbox or PlayStation labels, bumpers, triggers and system buttons
• Presets for racing (pedals and tilt steering), first-person (look pad and gyro aim), third-person and retro games
• Gyro aiming and tilt steering
• Haptic feedback

ALSO
• Find nearby computers and pair from the phone
• Switch between paired computers with one tap
• Quick Settings tile and notification shortcuts
• Material 3 Expressive design with your wallpaper colors or a vivid blue palette, light and dark themes and a pure black option
• Tablet layout with a navigation rail

PRIVACY
PadBridge has no internet permission. It collects no data, shows no ads and needs no account. Your input goes only over Bluetooth to the computer you choose.

GOOD TO KNOW
• Requires Android 9 or newer with Bluetooth HID Device support. A few custom ROMs disable it.
• On Windows, enable "Drivers for keyboard, mice, etc. (HID)" for the phone under Control Panel › Devices and Printers › Properties › Services. PadBridge shows these steps when needed.
• Multi-finger gestures are sent as each system's keyboard shortcuts, because macOS doesn't accept multitouch from non-Apple touchpads. On Windows and Linux you can try the native touchpad instead.
• The gamepad is a standard HID gamepad, not an Xbox or PlayStation controller. On Windows, use Steam Input for XInput-only games.
• Typing from the phone keyboard assumes a US keyboard layout on the computer.
• Apple TV support is unverified.

PadBridge is open source under the Apache 2.0 license: https://github.com/spundone/padbridge
```

## Feature bullets

For promo text, the feature graphic and screenshot captions:

- Trackpad with multi-finger gestures for Windows, macOS and Linux
- Your phone keyboard or a full on-screen keyboard, six layouts
- TV remote for Android TV, Apple TV, computers and slides
- Air mouse and gamepad with gyro
- No computer software, no internet, no data collection
- Free and open source

## Categorization

| Field | Value |
|---|---|
| App or game | App |
| Category | Tools |
| Tags (suggested) | Remote control, Keyboard, Productivity |
| Contact email | (owner to fill in) |
| Website | https://github.com/spundone/padbridge |
| Privacy policy URL | https://github.com/spundone/padbridge/blob/main/PRIVACY.md |

## App content

| Section | Answer |
|---|---|
| Privacy policy | URL above |
| Ads | No, the app contains no ads |
| App access | All functionality is available without special access (no login). Reviewers need a Bluetooth computer to test input. |
| Content rating | Questionnaire: Utility/productivity; no violence, sexual content, profanity, drugs, gambling, user-generated content, user interaction, location sharing or purchases. Expected rating: Everyone / PEGI 3. |
| Target audience | 13 and over (not designed for children) |
| News app | No |
| COVID-19 contact tracing | No |
| Government app | No |
| Financial features | None |
| Health | No |

## Data safety form

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | Not applicable (no data collected). Bluetooth links use the standard Bluetooth pairing encryption. |
| Do you provide a way for users to request that their data is deleted? | Not applicable (no data collected; settings stay on the device and are removed on uninstall). |
| Data types collected | None |
| Data types shared | None |

Why "No" is accurate:
- No `INTERNET` permission: nothing can leave the device over the network.
- Input (touches, keystrokes, sensor motion) is sent over Bluetooth to a device the user picked and controls. Play's
  definition of "collection" is data transmitted off the device to the developer or third parties; user-directed
  transfer to the user's own computer is not that.
- Settings and the last-connected computer's Bluetooth address are stored locally only and excluded from backup.
- Location permission (Android 11 and lower) is used only because the OS requires it for Bluetooth scanning. No
  location is read or stored.

## Permission declarations and justifications

### Foreground service: `connectedDevice`

Play Console › App content › Foreground service permissions.

| Field | Answer |
|---|---|
| Foreground service type | Connected device (`FOREGROUND_SERVICE_CONNECTED_DEVICE`) |
| Task | Maintains the Bluetooth HID connection to the user's computer, TV or other host |
| Description | PadBridge acts as a Bluetooth keyboard, mouse and gamepad for a computer the user has paired. The foreground service keeps the app registered with Android's Bluetooth HID Device profile and keeps the connection to that computer open while the user switches apps or locks the screen, so input continues to work and the computer doesn't drop the device. It starts when the user opens PadBridge with Bluetooth permission granted and stops when the user taps Stop in the notification or in Settings. An ongoing notification shows the connection state. |
| User impact if deferred or interrupted | The computer loses its keyboard and mouse immediately and must reconnect; held keys or buttons could be left in an undefined state. |
| Video | Short screen recording: open PadBridge, connect to a computer, show the notification, switch to another app and lock the screen while the pointer still moves on the computer, then tap Stop. (Owner to record and upload.) |

### `BLUETOOTH_SCAN` with `neverForLocation`

Declared with `android:usesPermissionFlags="neverForLocation"`. Requested only when the user taps **Find nearby
computers**, to discover computers and pair with them from the phone (needed on Windows, where pairing from the PC
often skips HID). Scan results are shown in a list and are not used to derive location. With this flag Android 12+
does not require location permission for the scan.

### `ACCESS_FINE_LOCATION` (`maxSdkVersion="30"`)

Requested only on Android 9–11, only for **Find nearby computers**, because those Android versions require location
permission for Bluetooth discovery. Not used to access location. No background location.

### `BLUETOOTH_CONNECT`

Core function: register as a Bluetooth HID device, read paired devices, connect to the chosen computer and send
input reports.

### `BLUETOOTH_ADVERTISE`

Make the phone discoverable (**Make this phone visible**) so a computer or TV can find and pair with it.

### `POST_NOTIFICATIONS`

Ongoing notification required for the foreground service, with Trackpad, Keyboard and Stop actions.

### `VIBRATE`

Haptic feedback for clicks, key presses and gamepad buttons. Can be turned off in Settings.

## Pre-launch notes

- Build the `play` flavor (`./gradlew bundlePlayStandardRelease`); it excludes root mode.
- Upload with an upload key; enroll in Play App Signing. Never upload a build signed with the shared dev key.
- Screenshots: see [screenshots/README.md](screenshots/README.md).
- Store-ready assets and text live in `fastlane/metadata/android/en-US/` (fastlane supply layout): icon 512², feature
  graphic 1024×500, seven phone screenshots, three 7-inch and three 10-inch tablet screenshots, title, descriptions
  and per-versionCode changelogs. `scripts/play_images.py` renders the captioned images from raw demo-mode captures
  (`scripts/screenshots.sh`).
