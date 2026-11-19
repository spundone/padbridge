# Native touchpad (beta)

PadBridge normally recognises gestures on the phone and sends the computer mouse movement, wheel scrolling and
keyboard shortcuts. That works everywhere, but it isn't the computer's own touchpad experience: you don't get
Windows or Linux touchpad settings, or precise smooth scrolling and pinch.

**Native touchpad** mode makes the phone also describe itself as a **Windows Precision Touchpad (PTP)**: a
multi-touch digitizer that reports up to five fingers. Computers that support PTP then take the raw touches and
do every gesture themselves.

| Computer | What happens |
|---|---|
| Windows 10 / 11 | Treated as a Precision Touchpad. Taps, two-finger scroll, pinch, and three- and four-finger gestures come from Windows, configured in Settings › Bluetooth & devices › Touchpad. |
| Linux (GNOME, KDE, …) | `hid-multitouch` binds it as a Win8-class multitouch touchpad and libinput provides native gestures. |
| macOS | Ignores the touchpad collection, so PadBridge keeps recognising gestures on the phone. |

## Turning it on

1. Settings › **Native touchpad for Windows and Linux**.
2. PadBridge re-registers its HID descriptor (v2). Computers remember the old one, so **remove the phone in the
   computer's Bluetooth settings and pair again.**
3. Once the computer switches the phone into touchpad mode, Settings shows **Active**. The trackpad's hint text
   then says the computer is handling gestures.

Turning it off re-registers descriptor v1 and needs another re-pair.

## How it works

PadBridge follows Microsoft's PTP specification:
[collection](https://learn.microsoft.com/windows-hardware/design/component-guidelines/touchpad-windows-precision-touchpad-collection),
[configuration](https://learn.microsoft.com/windows-hardware/design/component-guidelines/touchpad-configuration-collection).

- **Touch Pad collection** (report 5): five finger slots, each with Confidence, Tip, Contact ID and 12-bit X/Y
  carrying a physical size so gestures scale correctly. The report also carries Scan Time (100 µs), Contact Count
  and Button 1. The phone uses parallel reporting mode.
- **Feature reports:**
  - Contact Count Maximum / Pad Type (6): 5 contacts, click-pad.
  - Latency (10).
  - Microsoft's published default certification blob (7): Windows requires the report to exist; the signature
    is optional on Windows 10 and later.
- **Configuration collection:** Input Mode (8) and Surface / Button switch (9).
- **Mode switching:** the device starts in **mouse mode** and keeps using the normal mouse report. It switches to
  raw contacts only when the host writes Input Mode = 3. The mode resets on every connection. A computer that
  never switches (macOS, older systems) keeps the phone-side gestures.
- **Resting fingers:** while fingers are down the phone re-sends the frame every 50 ms, because Linux treats a
  silent contact as lifted after about 100 ms.

The v2 descriptor (821 bytes) registers on Android's stack. `hid-decode` (hid-tools) parses it and classifies it
as "win 8 certified", the class Linux's `hid-multitouch` uses for Precision Touchpads.

## Not verified yet

- **Windows on real hardware:** that it sends the Input Mode write over Bluetooth and accepts this device. Shipping
  Bluetooth PTPs (Brydge keyboards, Logitech Casa) suggest it does.
- **Android's 256-byte reply:** whether Android's HID stack delivers the certification blob in one reply on every
  phone.
- **Pad size:** taken from the phone's screen (portrait width × about 65% of its height), so gesture distances can
  be slightly off in landscape.

If it misbehaves, turn it off and re-pair. Please open an issue with `adb logcat -s PadBridge` output: it logs
every feature request from the computer (`GET_FEATURE …`) and Input Mode changes.

## What about macOS (Magic Trackpad emulation)?

Researched, not built. macOS gives native multitouch gestures only to Apple trackpads.

- **Identity:** a fake would need Apple's Bluetooth vendor/product ID, which needs root (the Device ID record).
- **Report format:** it would need the Magic Trackpad 2 report format: report 0x31 with 9-byte touch records, as
  documented in Linux's `hid-magicmouse.c`.
- **Private queries:** Apple's driver queries several undocumented feature reports at attach.
  [VoodooInput](https://github.com/acidanthera/VoodooInput) answers these from inside the Mac's kernel, but nobody
  has published a working Bluetooth emulation from another device.
- **Conflict:** it would also be mutually exclusive with the Windows and Linux touchpad mode.

On macOS, PadBridge keeps recognising gestures on the phone and maps them to the Mac's own shortcuts: Mission
Control, Spaces, App Exposé, ⌘+/− zoom.
