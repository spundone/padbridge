# Remote (D-pad)

The **Remote** tab is a TV-remote style controller for TVs, media centers, computers and presentations.

## Layout

From top to bottom:

- **Profile chips:** Android TV, Apple TV, Mac, PC, Slides. The choice is remembered.
- **Back** and **Home** buttons (labels depend on the profile).
- **D-pad:**
  - The arrow wedges send arrow keys and are held while your finger is down, so the host repeats them. Slide
    between wedges without lifting.
  - Tap the center for **OK** (Enter).
  - Swipe across the center like a Siri Remote touch surface: every ~48 dp of travel sends one arrow.
- **Previous / Play-pause / Next.** In Slides, Previous and Next change slides (← / →).
- **Volume down / Mute / Volume up** (media keys).
- **Profile extras** (see below) and the **Volume keys** chip.

The keyboard button at the top opens your phone keyboard for search boxes. The moon turns on
[AOD mode](user-guide.md#aod-mode), which is handy when presenting.

## Profiles

| Profile | Back | Home | Extras |
|---|---|---|---|
| **Android TV** | AC Back (consumer 0x224) | AC Home (consumer 0x223) | Menu (keyboard Menu key), Search (AC Search 0x221), Rewind, Fast forward |
| **Apple TV** | Menu = Esc | AC Home (unverified on tvOS) | Space, Rewind, Fast forward |
| **Mac** | Esc | Mission Control (⌃↑) | Space, Full screen (⌃⌘F), ⌘Tab, Spotlight (⌘Space) |
| **PC** | Esc | Start (Win) | Space, F, Alt+Tab, Full screen (F11) |
| **Slides** | End show (Esc) | Start show: F5, or ⌘⇧↩ when Computer type is macOS | From here (⇧F5, or ⌘↩ on macOS), Black (B), White (W), Laser (Ctrl+L) |

In every profile the arrows are arrow keys and OK is Enter.

Notes:
- **Android TV** maps the HID consumer usages AC Back and AC Home to its Back and Home keys, and the keyboard Menu
  key to its menu key.
- **Apple TV** navigates with keyboard arrows, Return and Esc. Its handling of AC Home is unverified, and tvOS may
  not list a phone for pairing at all. See [pairing.md](pairing.md#apple-tv).
- **Slides** shortcuts match PowerPoint (F5 / ⇧F5, B, W, Ctrl+L laser pointer) and also work in Keynote and Google
  Slides where those use the same keys. ⌘⇧↩ starts from the beginning in PowerPoint for Mac and Keynote.
- **Mac** and **PC** profiles suit media playback and couch use: Space plays/pauses most players, F toggles full
  screen in YouTube and many web players.

## Phone volume buttons

With the **Volume keys** chip on (default), the phone's volume buttons go to the host instead of the phone:

| Profile | Volume up | Volume down |
|---|---|---|
| Slides | Next slide (→) | Previous slide (←) |
| Others | Host volume up | Host volume down |

In Slides, holding a volume button doesn't auto-repeat, so you can't skip slides by accident. This only applies
while the Remote tab is open.
