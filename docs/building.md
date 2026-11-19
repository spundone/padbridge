# Building

## Toolchain

| Component | Version |
|---|---|
| Android Gradle Plugin | 9.5 (built-in Kotlin support) |
| Kotlin | 2.4 |
| Gradle | 9.9 (wrapper included) |
| JDK | 17 |
| compileSdk | 37 |
| targetSdk | 36 (from 0.8.0) |
| minSdk | 28 (Android 9) |

You need the Android SDK with platform 37 installed and `ANDROID_HOME` set (or a `local.properties` with `sdk.dir`).

**Build on x86_64.** Google ships no arm64 Linux build of `aapt2`, so builds fail on arm64 Linux hosts (for
example an ARM workstation or a Raspberry Pi). x86_64 Linux, macOS and Windows work. On an arm64 Linux machine,
build on an x86_64 host or in CI.

## Flavors

From 0.8.0 the app has two product flavors:

| Flavor | Distribution | Differences |
|---|---|---|
| `github` | GitHub Releases, sideloading | Includes experimental [root mode](root-mode.md) |
| `play` | Google Play | No root mode |

Everything else is the same.

## Commands

```sh
# Debug build of the GitHub flavor
./gradlew assembleGithubDebug

# Release APK for GitHub Releases (R8-shrunk)
./gradlew assembleGithubRelease
# -> app/build/outputs/apk/github/release/

# Release bundle for Google Play
./gradlew bundlePlayRelease
# -> app/build/outputs/bundle/playRelease/

# Install a debug build on a connected phone
./gradlew installGithubDebug
adb logcat -s PadBridge
```

Release builds have R8 minification and resource shrinking on.

## Signing

### Dev key (sideload builds)

`keystore/padbridge-dev.keystore` is a **shared, non-secret** debug-style key (alias `androiddebugkey`, passwords
`android`). It is committed on purpose: builds from any machine and from CI install over each other, because
Android refuses an update signed with a different key. Debug builds always use it, and release builds fall back to
it when no release key is configured.

Do not use the dev key for anything published to Google Play.

### Release key

Release signing reads `keystore.properties` in the project root. This file is **not committed** (add it to
`.gitignore` if it isn't already). Example:

```properties
storeFile=/absolute/path/to/padbridge-release.jks
storePassword=...
keyAlias=padbridge
keyPassword=...
```

Check `app/build.gradle.kts` for the exact property names it reads.

For Google Play, use Play App Signing: Play holds the app signing key and you sign uploads with an upload key
configured as above.

Keep the release keystore and its passwords backed up somewhere safe. If you lose the key used for GitHub release
APKs, users must uninstall to install a build signed with a new key.

## Versioning

`versionName` follows [semantic versioning](https://semver.org/) and `versionCode` increases by one for every
build that is distributed. Record changes in [CHANGELOG.md](../CHANGELOG.md).

If a change alters the bytes of the HID descriptor, bump `HidDescriptor.VERSION` and call it out in the changelog:
users must re-pair.

## CI

`.github/workflows/build.yml` builds the project on every push and pull request.
