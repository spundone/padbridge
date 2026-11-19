# User guide

A tour of every screen in PadBridge. For pairing see [pairing.md](pairing.md); for details on individual modes see
[gestures.md](gestures.md), [keyboard.md](keyboard.md) and [remote.md](remote.md).

## Setup

The first launch opens a short setup flow:

1. **Welcome:** what PadBridge does.
2. **Permissions:**
   - **Nearby devices** lets PadBridge act as a Bluetooth trackpad and keyboard and find computers.
   - **Notifications** shows the connection while PadBridge runs in the background.
3. **Your computer:** Windows, macOS or Linux. This sets keyboard labels, shortcuts and what gestures send. For a TV,
   pick any; the Remote tab has TV profiles.
4. **Pair:** shows OS-specific instructions with **Find nearby computers** and **Make this phone visible**.
5. **Done:** a few tips.

You can run it again from **Settings › Run setup again**.

## Navigation

On phones, a bottom bar holds five tabs: **Connect**, **Trackpad**, **Keyboard**, **Remote** and **Gamepad**.
**Air mouse** opens from the trackpad's ⋮ menu. On wider screens (600 dp and up, e.g. tablets and unfolded foldables) they move to a navigation rail on
the left.

The status pill at the top right shows the connection (computer name, *Connecting*, *Not connected*, or *Needs
attention*). Tap it to go to Connect. The gear opens Settings.

Back from an input tab returns to Connect rather than closing the app; Back from Air mouse returns to the trackpad.
When a computer connects while you're on the Connect tab, PadBridge opens the trackpad.

Settings is a list of categories: **Trackpad**, **Gestures**, **Air mouse & gyro**, **Appearance**, **General** and
**About**. Each one opens its own page.

## Connect

- **Status card:** the current state and what to do next (for example *Bluetooth is off*, *Permission needed*,
  *Restart Bluetooth*, or the Windows HID steps after a refused connection).
- **Computers:** phones and computers this phone is paired with. By default only devices whose Bluetooth class looks
  like a computer are listed; **Settings › Show all paired devices** lists everything. Tap **Connect**, or
  **Switch** to move from the current computer. Only one computer is connected at a time. Switching releases held
  keys and buttons first.
- **Find nearby computers:** scans for about 12 seconds and pairs from the phone. Only classic Bluetooth entries
  are listed.
- **Make this phone visible:** makes the phone discoverable for 5 minutes so you can pair from the computer.
- **Computer type:** Windows, macOS or Linux.

The last computer reconnects automatically when PadBridge starts (**Settings › Reconnect to last computer**).
A foreground service keeps the connection alive when you switch apps or lock the screen. Its notification has
**Trackpad**, **Keyboard** and **Stop** buttons.

## Trackpad

A multi-touch pad. Gestures are covered in [gestures.md](gestures.md).

Toolbar:

| Control | Action |
|---|---|
| Moon | AOD mode |
| Keyboard | Open your phone keyboard above the pad, with a row of quick keys (Esc, Tab, arrows, ⌫, Del, ⏎, Home, End, select all / copy / paste / undo) |
| ⋮ menu | **Use the whole screen** (full display), **Full screen** (hide app and system bars), **Gesture guide**, and rotation: **Auto-rotate**, **Lock portrait**, **Lock landscape** |

While nothing is connected, the Trackpad, Keyboard and Remote screens show a banner. It offers a one-tap
**Connect to <last computer>**, or **Choose computer** to jump to the Connect tab.

### Full display

The pad uses every pixel of the screen: edge to edge, under the camera cutout, with no bars or margins. Its corners
follow the screen's rounded corners. A floating toolbar (Exit, Keyboard, AOD) folds into a small handle at the top
after a few seconds; tap the handle to bring it back. Back also exits.

Android still owns the outermost edge for its own gestures. PadBridge reserves the middle of the side edges, but a
swipe up from the very bottom edge can reveal the system bars.

### AOD mode

Available on Trackpad, Air mouse and Remote. The same screen is drawn as thin dim outlines on true black, so most
OLED pixels are off, and brightness drops to the minimum. The title chip shows the time and the connection, and the
content shifts a few dp every minute to avoid burn-in. Input keeps working. Tap the moon again or press Back to exit.

### Quick Settings tile

Add the **Trackpad** tile from your Quick Settings editor. It shows whether a computer is connected and opens the
trackpad.

## Keyboard

Your phone keyboard at the top, an optional shortcut row and media row, and the on-screen keyboard below. See
[keyboard.md](keyboard.md).

## Air mouse

Open it from the trackpad's ⋮ menu. Point the phone and turn it; the gyroscope moves the pointer.

- **Aim:** hold the big pad, or switch on **Always aim** (then a tap on the pad clicks).
- **Click:** tap the pad, or use **Left** / **Right**.
- **Drag:** hold **Left** while turning the phone.
- **Scroll:** roll a finger along the strip between the buttons.

It works with the phone held flat like a remote or upright like a camera, in any screen rotation. Speed is in
**Settings › Air mouse & gyro**. Needs a gyroscope.

### Gamepad presets

Pick a preset from the gamepad toolbar's preset button (🎮):

| Preset | Layout |
|---|---|
| Standard | Two sticks, D-pad, face buttons, bumpers and triggers |
| Racing | Gyro steering (tilt like a wheel); tall analog **Brake** (LT) and **Gas** (RT) pedals, where pressing lower on a pedal presses harder; Gear − / Gear + (LB / RB); small D-pad, stick and face buttons for menus |
| First person | Move stick that appears under your left thumb. The right half is a **look surface**: drag to aim, with gyro aim on top. Big **Fire** (RT) and **Aim** (LT), plus Jump (A), Crouch (B), Reload (X), Switch (Y), Grenade (RB), Tactical (LB), Sprint (L3), Melee (R3) |
| Third person | Move stick, camera surface, face buttons labelled Jump / Dodge / Attack / Heavy, shoulders, Sprint (L3), Lock on (R3) |
| Retro | Big D-pad and face buttons, bumpers, View / Menu |

Choosing a preset also sets the gyro mode: Racing steers, First person aims, the rest start with gyro off. You
can still change gyro yourself. Button names are labels only; the computer sees the standard gamepad buttons
shown in brackets.

## Remote

A TV-remote D-pad with per-device profiles. See [remote.md](remote.md).

## Gamepad

Opens in landscape with the system bars hidden.

- Two analog sticks. Double-tap a stick for L3 / R3. Touches in the left third of the screen also drive the left
  stick, so your thumb doesn't have to land exactly.
- D-pad (8 directions), ABXY, LB/RB, LT/RT (digital: fully pressed or released), View, Menu, Home, Share.
- Slide a thumb across the face buttons to roll from one to the next.
- Haptics on press, at the stick edge and on D-pad steps.

The floating toolbar has Exit, an **Xbox / PlayStation** label switch, a haptics toggle and the **Gyro** mode:

| Gyro mode | Effect |
|---|---|
| Off | Touch only |
| Aim | Turning the phone moves the right stick (gyro aiming), added to the touch stick |
| Steer | Tilting the phone like a steering wheel moves the left stick; full lock at 40° (at sensitivity 1) |

Sensitivity is in **Settings › Air mouse & gyro**.

PadBridge reports a generic HID gamepad. See [FAQ](faq.md#which-games-see-the-gamepad) for what that means per OS.

## Settings

| Section | Settings |
|---|---|
| Computer type | Windows, macOS, Linux |
| Pointer & scrolling | Pointer speed, scroll speed, natural scrolling (remembered per OS; off by default on macOS, which applies its own), scroll momentum, scroll rails, edge scroll strips, two-finger swipe for back/forward |
| Tapping | Tap to click, double-tap window (ms), ignore resting fingers, show click buttons, three-finger drag |
| Gestures on *OS* | Remap every gesture; reset to OS defaults or apply the Samsung DeX preset. See [gestures.md](gestures.md) |
| Air mouse & gyro | Air mouse speed, always aim, gamepad gyro sensitivity |
| Full screen input | Trackpad, keyboard and air mouse hide the bars |
| Appearance (0.8.0) | Dynamic color or the PadBridge palette; Light, Dark or System; pure black dark theme |
| General | Haptic feedback, reconnect to last computer, keep screen on while connected, show all paired devices |
| More | Run setup again; Root mode (GitHub build only, see [root-mode.md](root-mode.md)) |
| About | Version, HID descriptor version, links (an About screen in 0.8.0) |
| Stop PadBridge | Disconnects, unregisters the HID profile and stops the background service |

Settings are stored on the phone only. App data is excluded from Android backups.
