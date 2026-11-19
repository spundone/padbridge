# PadBridge

[![License: Apache-2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Build](https://github.com/spundone/padbridge/actions/workflows/build.yml/badge.svg)](https://github.com/spundone/padbridge/actions/workflows/build.yml)

PadBridge turns an Android phone into a Bluetooth **trackpad, keyboard, TV-remote D-pad, air mouse and gamepad**
for Windows, macOS, Linux and TVs.

It uses Android's built-in Bluetooth HID Device profile (`BluetoothHidDevice`, Android 9+). The computer sees an
ordinary Bluetooth keyboard and mouse, so it needs no app, driver or server. PadBridge has no internet permission
and collects no data.

> **Status:** 1.0. Pairing and input are verified on a Mac and a Windows 11 PC from a Xiaomi 14 (Android 17).
> Other combinations should work but are untested; see [Known limits](#known-limits).

## Features

| Mode | What it does |
|---|---|
| **Trackpad** | Move, tap to click, tap-and-drag, double tap. Two-finger scroll with momentum, pinch to zoom, rotate. Three- and four-finger swipes and taps, edge swipes, all remappable per OS. DeX-style hold-to-drag and resting-finger rejection. Optional click buttons, scroll strips, three-finger drag. |
| **Keyboard** | Your phone keyboard (Gboard, SwiftKey, …) in Live or Predictive mode, or a full on-screen keyboard with real key-down/up. Six layouts (Full, Full + numpad, 65%, Split, Gaming, Numpad), resizable, with Windows, Mac or Linux key labels, a shortcut strip and media keys. |
| **Remote** | A TV-remote style D-pad with profiles for Android TV, Apple TV, Mac, PC and presentation slides. Phone volume buttons can drive the host's volume or change slides. |
| **Air mouse** | Point and turn the phone; the gyroscope moves the pointer. Tap to click, hold Left to drag, scroll strip. |
| **Gamepad** | Presets for Standard, Racing (tilt to steer, analog pedals), First person (drag-to-look plus gyro aim), Third person and Retro games. Xbox or PlayStation labels, haptics. |

Also:
- **Full display** and **AOD mode** (dim outlines on black at minimum brightness) for the trackpad.
- A Quick Settings tile and notification buttons that jump straight to the trackpad or keyboard.
- **Find nearby computers**: pair from the phone instead of from the computer.
- Material 3 Expressive UI with wallpaper colors (or a vivid built-in palette), an adaptive navigation rail on tablets and a themed icon.
- **Native touchpad (beta)**: Windows Precision Touchpad / Linux multitouch, so the computer does gestures itself.
- Experimental **root mode** (GitHub build only) for Bluetooth speaker, device class and controller identity.

## Requirements

- Android 9 (API 28) or newer, with the Bluetooth HID Device profile enabled. Most phones have it; some custom
  ROMs turn it off.
- A computer or TV with classic Bluetooth (BR/EDR). Bluetooth LE alone is not enough.
- A gyroscope for the air mouse and gamepad gyro (optional otherwise).

## Install

- **GitHub:** download the APK from [Releases](https://github.com/spundone/padbridge/releases) and install it
  (allow installs from your browser or file manager).
- **Google Play:** planned.

The two builds differ only in that root mode is in the GitHub build. See [Building](docs/building.md#flavors).

## Quick start

1. Open PadBridge. The setup flow asks for **Nearby devices** and **Notifications** and which kind of computer you
   use.
2. Pair:
   - **Windows:** keep *Settings › Bluetooth & devices* open on the PC and tap **Find nearby computers** on the
     phone. Then enable HID for the phone in Control Panel (see [Pairing › Windows](docs/pairing.md#windows)).
   - **macOS / Linux:** tap **Make this phone visible**, then add the phone from the computer's Bluetooth settings.
3. The computer appears under **Computers**. Tap **Connect**. Use the tabs at the bottom to switch modes.

Already paired the phone with this computer before installing PadBridge? Remove the pairing on both sides and pair
again. Computers cache a device's services at pairing time.

## Documentation

| Guide | Contents |
|---|---|
| [User guide](docs/user-guide.md) | Tour of every screen and setting |
| [Pairing](docs/pairing.md) | Per OS: Windows, macOS, Linux, Android TV, Apple TV, plus troubleshooting |
| [Gestures](docs/gestures.md) | Every trackpad gesture and its default per OS |
| [Native touchpad (beta)](docs/native-touchpad.md) | Windows Precision Touchpad / Linux multitouch mode, and why macOS can't have it |
| [Keyboard](docs/keyboard.md) | Phone keyboard modes, on-screen layouts, modifiers, Fn layer |
| [Remote](docs/remote.md) | D-pad profiles and what each button sends |
| [Root mode](docs/root-mode.md) | Experimental root features and their system properties |
| [FAQ](docs/faq.md) | Common questions |
| [Architecture](docs/architecture.md) | For contributors: modules, HID descriptor, threading, gesture engine, theming |
| [Building](docs/building.md) | Toolchain, flavors, signing |
| [Changelog](CHANGELOG.md) · [Contributing](CONTRIBUTING.md) · [Privacy](PRIVACY.md) | |

## Known limits

- **Gestures are recognized on the phone.** macOS accepts multitouch only from Apple trackpads, and no one has
  gotten a Windows Precision Touchpad working over Android's HID API. PadBridge sends gestures as wheel, pan and
  keyboard-shortcut events instead.
- **The gamepad is a generic HID gamepad**, not XInput or DualSense. Those drivers bind by vendor/product ID, which
  apps can't set. Use Steam Input on Windows for XInput-only games. Root mode can change the Device ID record, but
  the report format stays generic.
- **Typing assumes a US keyboard layout on the host.** Characters a US layout can't type are skipped, and the app
  tells you.
- **Some ROMs disable the HID Device profile** (`bluetooth.profile.hid.device.enabled=false`). PadBridge then shows
  *Not supported*.
- **Android's Bluetooth stack can get stuck** after PadBridge is reinstalled while connected. PadBridge detects
  this and asks you to turn Bluetooth off and on.
- **Windows refuses the connection** until HID is enabled for the phone in Control Panel. PadBridge detects the
  refusal and shows the steps.
- **Apple TV** may not list the phone, and its handling of the Home button is unverified.
- **Using the phone as a Bluetooth speaker** (A2DP sink) needs root.
- **No rumble.** Haptics are generated on the phone only.

## Privacy

PadBridge has no internet permission, no analytics and no accounts. What you type and touch is sent only over
Bluetooth to the computer you connect. Details in [PRIVACY.md](PRIVACY.md).

## License

[Apache License 2.0](LICENSE).
