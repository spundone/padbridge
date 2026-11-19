# FAQ

### Do I need to install anything on the computer?

No. The computer sees a standard Bluetooth keyboard, mouse, media-key device and gamepad, which every desktop OS
supports. The only setup is pairing, plus one Control Panel tick on Windows (see [pairing.md](pairing.md#windows)).

### Which Android versions work?

Android 9 (API 28) and newer, as long as the phone's Bluetooth stack has the HID Device profile enabled. Most stock
ROMs do. Some custom ROMs turn it off; PadBridge then shows *Not supported*.

### Does it work over USB or Wi-Fi?

No. PadBridge uses Bluetooth only, and has no internet permission.

### Does it work with Bluetooth LE-only computers?

No. Android's HID Device profile uses classic Bluetooth (BR/EDR). Almost all computers and TVs have classic
Bluetooth.

### Can I connect to two computers at once?

No. One computer is connected at a time. Pair several and switch on the Connect tab; switching releases held keys
and buttons first.

### Why don't macOS / Windows see real multitouch gestures?

macOS accepts multitouch only from Apple trackpads. Windows Precision Touchpad support needs a certified device,
and no one has gotten it working over Android's HID API. PadBridge recognizes gestures on the phone and sends the
OS's own shortcut for each one. See [gestures.md](gestures.md).

### Why does Windows pair but refuse to connect?

Windows sees a phone and enables audio profiles, not HID. Enable **Drivers for keyboard, mice, etc. (HID)** for the
phone under Control Panel › Devices and Printers › phone › Properties › Services. PadBridge detects the refusal
and shows the steps.

### Which games see the gamepad?

PadBridge reports a generic HID gamepad, not an Xbox (XInput) or PlayStation controller. Drivers for those bind by
USB vendor/product ID, which Android apps can't set.

| Host | Support |
|---|---|
| Windows | DirectInput games see it. For XInput-only games, add it in Steam (Settings › Controller) and let Steam Input present it as an Xbox controller. |
| Linux | Works as an evdev gamepad; SDL games map it. |
| macOS | Visible to SDL and Steam, not to Apple's GameController framework. |
| Android TV | Should appear as a gamepad (untested). |

Rumble from games doesn't reach the phone. Root mode can change the reported vendor/product ID, but not the report
format; see [root-mode.md](root-mode.md).

### Why do some characters come out wrong or get skipped?

Typing assumes the computer uses a US keyboard layout. See [keyboard.md](keyboard.md#host-keyboard-layout).

### Why does scrolling feel smoother on Linux?

Linux negotiates high-resolution scrolling (the HID Resolution Multiplier) with PadBridge. Windows and macOS get
whole wheel notches, smoothed by momentum.

### Scrolling goes the wrong way.

Toggle **Settings › Natural scrolling**. It is remembered per computer type. On macOS, also check the Mac's own
*Natural scrolling* setting, which applies on top.

### The Bluetooth connection stopped working after I updated or reinstalled PadBridge.

Android's Bluetooth stack can hold on to a stale registration. PadBridge detects this and shows *Restart
Bluetooth*: turn Bluetooth off and on (or reboot). If the HID descriptor changed in the update, the changelog says
so, and you need to remove the pairing on both sides and pair again.

### Can the phone be a Bluetooth speaker for the computer?

Only with root, using root mode's experimental **Bluetooth speaker** (A2DP sink) in the GitHub build. Android
doesn't let normal apps enable the A2DP sink profile.

### Does it work with Apple TV?

Unverified. tvOS may not list a phone when pairing. If it pairs, the Apple TV remote profile sends arrows, Return
and Esc, which tvOS accepts from keyboards.

### Does PadBridge drain the battery?

A foreground service keeps the Bluetooth link alive, which costs little. The screen is the main cost; AOD mode keeps
it on at minimum brightness with most pixels black. **Settings › Keep screen on while connected** can be turned
off. Use **Stop PadBridge** in Settings or the notification to stop it entirely.

### Does PadBridge collect any data?

No. It has no internet permission, no analytics, no ads and no accounts. See [PRIVACY.md](../PRIVACY.md).

### What's the difference between the GitHub and Play builds?

The GitHub build includes experimental root mode. Otherwise they are the same.

### How do I report a bug?

Open an issue at <https://github.com/spundone/padbridge/issues> with your phone model, Android version, the host OS
and version, what you did, and if possible the output of `adb logcat -s PadBridge`.
