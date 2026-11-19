# Pairing

PadBridge registers the phone as a Bluetooth HID device (keyboard + mouse + consumer control + gamepad). The
computer pairs with it like any Bluetooth keyboard. After pairing, you connect from the phone.

**Before you start**
- Open PadBridge first, and keep it open while pairing. The HID profile is only advertised while PadBridge is
  registered, and computers read a device's services once, at pairing time.
- If the phone was paired with this computer before (for example for Phone Link or file transfer), remove the
  pairing on **both** the computer and the phone, then pair again.
- One computer is connected at a time. You can pair several and switch between them on the Connect tab.

There are two ways to pair:

| Method | How | Best for |
|---|---|---|
| **Find nearby computers** | Phone scans, you pick the computer, both sides confirm a code, PadBridge connects | Windows |
| **Make this phone visible** | Phone is discoverable for 5 minutes; you add it from the computer's Bluetooth settings | macOS, Linux, TVs |

**Find nearby computers** lists classic Bluetooth devices only. Windows also advertises over Bluetooth LE (Swift
Pair), and an LE pairing cannot carry HID, so those entries are hidden. If PadBridge says a device was found over
LE only, keep the computer's Bluetooth settings open and scan again.

## Windows

Verified on Windows 11.

1. On the PC, open **Settings › Bluetooth & devices** and leave it open so the PC is discoverable.
2. In PadBridge tap **Find nearby computers**, pick the PC, and confirm the same code on both screens.
   (Alternatively: **Make this phone visible**, then **Add device › Bluetooth** on the PC.)
3. **Enable HID for the phone.** Windows reads the phone's Bluetooth class ("phone"), turns on its audio profiles
   and leaves HID off. The connection is then refused (L2CAP result `0x0004`, "no resources"). To fix it:
   1. Open **Control Panel › Hardware and Sound › Devices and Printers**. On Windows 11 you can also press Win+R and
      run `explorer shell:::{A8A91A66-3A7D-4424-8D24-04E180695C7A}`.
   2. Right-click the phone › **Properties** › **Services**.
   3. Tick **Drivers for keyboard, mice, etc. (HID)** and click **OK**.
4. In PadBridge tap **Connect** next to the PC.

PadBridge notices when a connection is dropped within about 1.5 seconds and shows these steps on the status card.

**HID isn't listed under Services?** Windows cached the phone's services from before PadBridge was running. Remove
the phone on the PC, forget the PC on the phone, and pair again with PadBridge open. Then repeat step 3.

With root, you can also change the phone's Bluetooth class to keyboard + mouse so Windows loads HID by itself
(untested; see [root-mode.md](root-mode.md#features)).

## macOS

Verified on a Mac (pointer and keyboard).

1. Open **System Settings › Bluetooth**.
2. In PadBridge tap **Make this phone visible**.
3. Click **Connect** next to the phone on the Mac and accept the code.
4. If PadBridge didn't connect by itself, tap **Connect** next to the Mac.

If the **Keyboard Setup Assistant** appears and asks you to press the key to the right of the left Shift key, press
`z` on the Keyboard tab and choose **ANSI**.

Set **Computer type** to macOS so shortcuts use ⌘ and gestures send Mac shortcuts. Natural scrolling is off in
PadBridge by default on macOS because macOS applies its own setting to wheel input.

## Linux

Not yet verified on hardware; standard BlueZ HID host support should work.

**GNOME / KDE:** open Bluetooth settings, tap **Make this phone visible**, and pair the phone from the list.

**bluetoothctl:**

```sh
bluetoothctl
power on
scan on            # wait for the phone's name
pair XX:XX:XX:XX:XX:XX
trust XX:XX:XX:XX:XX:XX
connect XX:XX:XX:XX:XX:XX
```

Linux negotiates high-resolution scrolling with PadBridge (the HID Resolution Multiplier), so scrolling is smoother
than on hosts that don't.

## Android TV / Google TV

Not yet verified on hardware.

1. On the TV open **Settings › Remotes & Accessories › Add accessory** (wording varies by model).
2. In PadBridge tap **Make this phone visible**.
3. Select the phone on the TV.
4. Use the **Remote** tab with the **Android TV** profile. Back and Home use the HID consumer usages *AC Back* and
   *AC Home*, which Android maps to its Back and Home keys.

## Apple TV

Unverified. tvOS lists only some device types under **Settings › Remotes and Devices › Bluetooth**, so it may not
show a phone at all. If it does pair, use the **Apple TV** profile on the Remote tab: arrows, Return and Esc
(= Menu / Back) are standard keyboard input that tvOS accepts. The Home button sends *AC Home*, which may do
nothing on tvOS.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| Status says **Not supported**, "HID Device profile did not respond" | The ROM disabled the HID Device profile (`bluetooth.profile.hid.device.enabled=false`; seen on some LineageOS and /e/OS builds). There is no fix without changing the ROM. |
| Status says **Restart Bluetooth**, "Android's Bluetooth service is stuck" | The Bluetooth stack accepted the registration but never confirmed it, usually because a stale registration survived a reinstall. Turn Bluetooth off and on (or reboot). PadBridge reconnects by itself. |
| "Another app is using Bluetooth HID" | Only one app can hold the HID Device role. Close the other app (another remote/keyboard app); PadBridge retries with back-off. |
| Windows pairs but the connection is refused straight away | HID is off for the phone. See [Windows](#windows), step 3. |
| "*Computer* didn't respond" after 12 seconds | The computer may have cached the phone from an earlier pairing without HID. Remove the pairing on both sides and pair again with PadBridge open. |
| Paired, but the computer isn't under **Computers** | Its Bluetooth class isn't "computer". Turn on **Settings › Show all paired devices**. |
| Nothing found in **Find nearby computers** | Keep the computer's Bluetooth settings open so it's discoverable, then **Scan again**. On Android 12+ allow Nearby devices; on Android 9–11 allow Location (Android requires it for scanning there). |
| Pairing "didn't complete" | Accept the pairing prompt on the computer as well as the phone, with the Bluetooth settings window still open. |
| Connected but nothing happens after reinstalling PadBridge | The HID descriptor may have changed. Remove the pairing on both sides and pair again. |
| Keys type the wrong characters | The host keyboard layout isn't US. See [keyboard.md](keyboard.md#host-keyboard-layout). |
| Connection drops when the screen locks | Check that the PadBridge notification is present; some ROMs kill background services. Exempt PadBridge from battery optimization. |

For a log, connect the phone over USB and run:

```sh
adb logcat -s PadBridge
```

It shows registration, connection state changes and the reason for refusals. Attach it to bug reports.
