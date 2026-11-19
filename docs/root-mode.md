# Root mode (experimental)

> **Untested on a rooted phone.** Root mode is only in the **GitHub** build (the `github` flavor), not the Google
> Play build. Use it to experiment; it can break Bluetooth until you reboot.

Root mode changes Bluetooth system properties that apps can't normally touch. Open **Settings › More › Root mode
(experimental)**.

## Requirements

- A rooted phone with **Magisk** or **KernelSU**.
- PadBridge allowed in the root manager. The first check (`su -c id -u`) may trigger the manager's prompt.

## How it works

Each change:

1. Sets one or more `bluetooth.*` system properties with `resetprop` (Magisk / KernelSU), falling back to
   `setprop`. An empty value deletes the property.
2. Reads the property back with `getprop` to check it took effect.
3. Restarts Bluetooth (`cmd bluetooth_manager disable`, then `enable`), because the stack reads these properties
   only at start. Connections drop for a few seconds.

The screen shows a log of what was set and whether it worked.

**Nothing persists.** A reboot restores the phone's defaults. Computers cache a device's class and ID when they
pair, so **remove and re-pair** on the computer after changing them.

## Features

| Feature | Properties | What it does |
|---|---|---|
| **Bluetooth speaker** | `bluetooth.profile.a2dp.sink.enabled`, `bluetooth.profile.avrcp.controller.enabled`, `bluetooth.a2dp.src_sink_coexist.enabled` | Turns on Android's built-in A2DP sink and AVRCP controller, so a computer can pick the phone as an audio output. Source/sink coexistence keeps headphones working. After enabling, connect from the computer's sound settings. Turning it off restores the three properties to the values the ROM had before. |
| **Device class** | `bluetooth.device.class_of_device` (3 bytes, most significant first, e.g. `0,5,192`) | Reports the phone as a keyboard + mouse (0x0005C0) or gamepad (0x000508) instead of a phone. On Windows this should load HID directly instead of phone audio profiles. **Phone (default)** restores the value saved before the first change. |
| **Controller identity** | `bluetooth.device_id.vendor_id`, `bluetooth.device_id.vendor_id_source` (2 = USB-IF), `bluetooth.device_id.product_id` | Puts a game controller's vendor and product ID in the Bluetooth Device ID (DI) record. |

### Controller identity presets

| Preset | VID:PID |
|---|---|
| Off (phone's own ID) | — |
| Xbox Wireless Controller (Xbox One S, Bluetooth) | 045E:02FD |
| DualSense | 054C:0CE6 |
| DualShock 4 (v2) | 054C:09CC |
| Switch Pro Controller | 057E:2009 |

The gamepad's **report format stays PadBridge's generic layout**. A driver that matches the ID and expects the
real controller's reports (for example `hid-playstation` or `hid-nintendo` on Linux, or Steam's controller drivers)
may misread buttons or ignore the device. This is for experimenting, not a working XInput or DualSense emulation.

## Where the property names come from

AOSP: `BluetoothProperties.sysprop` (profile switches, class of device) and, in `packages/modules/Bluetooth`,
`sysprop/device_id.sysprop` and `sysprop/a2dp.sysprop`. Vendors can override or ignore them, so a property may set
correctly and still have no effect.

## Why it isn't in the Play build

The features are unverified, they run shell commands as root, and they can leave Bluetooth in a broken state until
a reboot. The store build leaves them out to keep its behavior simple to review and support. The code is in the
same repository and is compiled only into the `github` flavor; see [building.md](building.md#flavors).
