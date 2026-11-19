# Privacy policy

*Effective date: 2026-11-19. Applies to PadBridge for Android (package `dev.padbridge`), both the Google Play and
GitHub builds.*

## Summary

PadBridge does not collect, store on a server, share or sell any personal data. It has no analytics, no advertising,
no crash-reporting service and no accounts. The Google Play build has no internet access at all. The GitHub build
connects to GitHub only to check for and download app updates (see [Updates](#updates)).

## What PadBridge does with your input

PadBridge turns your phone into a Bluetooth keyboard, mouse, remote and gamepad. Touches, key presses, text you
type, and gyroscope movement are converted into Bluetooth HID input reports and sent **only** to the computer or TV
you chose to connect to, over a direct Bluetooth link. They are not recorded, logged to a file, or sent anywhere
else.

Text you type is shown briefly in an on-screen echo (the last 200 characters, kept in memory only) and discarded
when you leave the screen.

## Data stored on your phone

PadBridge keeps its settings in the app's private storage on your phone:

- Your preferences (computer type, speeds, gesture mappings, keyboard layout and size, theme, etc.).
- The Bluetooth address of the last computer you connected to, so it can reconnect automatically.
- In the GitHub build, if you use root mode: the phone's original Bluetooth device class, so it can be restored.

This data never leaves the phone. It is excluded from Android cloud backup (`allowBackup="false"`). Uninstalling
PadBridge or clearing its storage deletes it.

## Permissions

| Permission | Why PadBridge needs it |
|---|---|
| **Nearby devices** (`BLUETOOTH_CONNECT`) | Register as a Bluetooth HID device, list paired computers, connect and send input. |
| **Nearby devices** (`BLUETOOTH_ADVERTISE`) | Make the phone discoverable so a computer can pair with it. |
| **Nearby devices** (`BLUETOOTH_SCAN`, declared `neverForLocation`) | **Find nearby computers**: scan for computers to pair with from the phone. Asked for only when you use this feature. PadBridge does not derive location from scan results. |
| **Location** (`ACCESS_FINE_LOCATION`, Android 9–11 only) | On Android 11 and older, the system requires location permission to scan for Bluetooth devices. Used only for **Find nearby computers**. PadBridge never reads your location. |
| `BLUETOOTH`, `BLUETOOTH_ADMIN` (Android 9–11 only) | The older equivalents of the Nearby devices permissions. |
| **Notifications** (`POST_NOTIFICATIONS`) | Show the ongoing connection notification with Trackpad, Keyboard and Stop buttons. |
| **Foreground service** (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE`) | Keep the Bluetooth connection to your computer alive while PadBridge is in the background or the screen is locked. |
| `VIBRATE` | Haptic feedback on touches and button presses. |

PadBridge reads the gyroscope, gravity and accelerometer sensors for the air mouse and gamepad gyro modes, only
while those screens are open. Sensor data is turned into pointer or stick movement and is not stored.

The Google Play build does **not** request the `INTERNET` permission, so it cannot send data over the network.

The GitHub build additionally requests:

| Permission | Why |
|---|---|
| `INTERNET` | Check GitHub for a newer release and download it. Nothing else is sent or received. |
| `REQUEST_INSTALL_PACKAGES`, `UPDATE_PACKAGES_WITHOUT_USER_ACTION` | Hand the downloaded update to Android's installer. Android asks you to confirm unless PadBridge installed the current version itself. |

## Updates

- **Google Play build:** updates come from Google Play through Play's in-app update API. PadBridge asks the Play
  Store app on your phone whether a newer version exists; PadBridge itself makes no network connection.
- **GitHub build:** when you open the app (at most once a day, and only if **Check for updates automatically** is
  on) or when you tap **Check for updates**, PadBridge requests
  `https://api.github.com/repos/spundone/padbridge/releases/latest`. If you tap **Update**, it downloads the release
  APK from GitHub, checks its SHA-256 checksum against the one GitHub publishes and checks it is signed with the same
  key as the installed app, then passes it to Android's installer. These requests carry no personal data or
  identifiers beyond what any HTTPS request reveals (your IP address and a generic `PadBridge-Android` user agent),
  and are subject to [GitHub's privacy statement](https://docs.github.com/site-policy/privacy-policies/github-general-privacy-statement).

## Your phone keyboard

When you type with your phone's keyboard app (Gboard, SwiftKey, Samsung Keyboard, …), that keyboard app is
governed by its own privacy policy. In both typing modes PadBridge asks the keyboard not to learn from what you type
(`IME_FLAG_NO_PERSONALIZED_LEARNING`, the same request incognito browser tabs make). In **Live** mode the field is
also marked as a visible-password field, so most keyboards show no suggestions at all. Whether a keyboard honours
these requests is up to the keyboard app.

## Root mode (GitHub build only)

The optional, experimental root mode runs shell commands as root to change Bluetooth system properties on your
phone. It doesn't collect or transmit any data. It isn't included in the Google Play build.

## Children

PadBridge is not directed at children and collects no data from anyone.

## Changes

If this policy changes, the new version will be published in the app's repository at
<https://github.com/spundone/padbridge/blob/main/PRIVACY.md> with a new effective date.

## Contact

Questions: open an issue at <https://github.com/spundone/padbridge/issues>.
