# Contributing to PadBridge

Thanks for helping. Bug reports, hardware test results and pull requests are all welcome.

## Reporting bugs

Open an issue at <https://github.com/spundone/padbridge/issues> and include:

- Phone model, Android version and ROM (stock, LineageOS, …).
- PadBridge version and build (GitHub or Play), from Settings › About.
- Host: OS and version (e.g. Windows 11 24H2, macOS 15, Ubuntu 24.04 / GNOME), or TV model.
- What you did, what you expected, what happened.
- If it's a connection problem, the output of `adb logcat -s PadBridge` from launch until the failure.

## Hardware test reports

Much of PadBridge is only verified on a few devices (see the README). Reports that something **works** are useful
too, especially for Linux desktops, Android TV, Apple TV, ChromeOS and the gamepad in specific games. Open an issue
with the label `compatibility` (or just say "compatibility report").

## Development setup

See [docs/building.md](docs/building.md). In short: JDK 17, Android SDK with platform 37, an x86_64 build host,
then `./gradlew assembleGithubDebug`.

Read [docs/architecture.md](docs/architecture.md) before larger changes. It explains the HID descriptor, the
`hid-io` thread and the gesture engine.

## Pull requests

- Keep each PR focused on one change. Open an issue first for large features or UI changes.
- Match the existing style: Kotlin official code style, small files by feature, comments that explain *why*.
- Test on a real phone with at least one host. Say in the PR what you tested (phone, Android version, host OS).
- Update the docs in `docs/` and add an entry under **Unreleased** in [CHANGELOG.md](CHANGELOG.md).
- CI must pass.

### Things that need extra care

- **The HID descriptor** (`HidDescriptor.kt`). Any byte change forces every user to re-pair. Avoid it unless
  necessary; if you must, bump `HidDescriptor.VERSION`, keep report lengths in sync with `HidManager`, and say so
  in the changelog.
- **Report state** in `HidManager` belongs to the `hid-io` thread. Don't read or write it from the UI thread; post
  to the thread instead.
- **Stuck keys.** Anything that presses a key or button must release it on every exit path (pointer cancel, view
  detach, switching computers). Use `releaseAll()` / `releaseEverything()`.
- **Permissions and privacy.** PadBridge has no internet permission and collects nothing. PRs that add network
  access, analytics, ads or crash reporting services won't be accepted. New permissions need a clear reason and an
  update to [PRIVACY.md](PRIVACY.md).
- **Root features** belong in the `github` flavor only.
- **Gesture and shortcut defaults:** if you change one, update [docs/gestures.md](docs/gestures.md).

## License

By contributing, you agree that your contributions are licensed under the [Apache License 2.0](LICENSE), the
project's license.
