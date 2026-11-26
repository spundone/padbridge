# Screenshots

The README screenshots, captured on emulators from a debug build in **demo mode**. Demo mode fakes a connection to
"MacBook Pro" and lists made-up computers with placeholder addresses, so no real device names or Bluetooth
addresses appear. It only exists in debug builds
(`adb shell am start -n dev.padbridge.dev/dev.padbridge.MainActivity --ez demo true`).

| File | Device | Screen |
|---|---|---|
| `phone-connect.png` | Phone, portrait | Connect: connected, other computers, pairing |
| `phone-trackpad.png` | Phone, portrait | Trackpad |
| `phone-keyboard.png` | Phone, portrait | Keyboard: shortcut and media rows, trackpad, Full layout |
| `phone-remote.png` | Phone, portrait | Remote, Android TV profile |
| `phone-settings.png` | Phone, portrait | Settings |
| `phone-gamepad.png` | Phone, landscape | Gamepad, Xbox labels |
| `tablet-laptop-deck.png` | Tablet, landscape | Keyboard as a laptop deck (keys with a trackpad below) |
| `tablet-connect.png` | Tablet, landscape | Two-pane Connect |
| `tablet-trackpad.png` | Tablet, landscape | Trackpad with the navigation rail |
| `tablet-remote.png` | Tablet, landscape | Remote |

## Regenerating

```sh
./gradlew assembleGithubStandardDebug
scripts/screenshots.sh <phone-avd> phone
scripts/screenshots.sh <tablet-avd> tablet
```

The script boots the AVD headless (it only talks to `emulator-5554`, never a connected phone), sets dark mode and
a clean status bar (9:41, full battery), installs the debug build, skips setup and captures each screen into
`~/shots/<avd>/`. The images in this folder were then scaled to 1280 px on the long side and reduced to 256 colors
to keep the repository small.

The Play Store images (with captions) live in `fastlane/metadata/android/en-US/images/`.
