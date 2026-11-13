# Aerial TV N1

> **AI-built project:** Code, UI, tests and documentation were produced with AI assistance. The build has been tested on a real Phicomm N1, but you should still audit the source and keep an independent recovery path.

A clean Android TV home screen for the Phicomm N1, based on the open-source [Aerial](https://github.com/pengchujin/aerial-tv) project and maintained as an independent N1 adaptation. It targets the `p230` running Android 7.1.2 / API 25 and a 1920×1080 display.

The current companion mobile controller is [N1 Pocket Remote 0.1.1 APK](https://github.com/jimmieyangcsjs/N1-Pocket-Remote/blob/main/dist/N1Remote-v0.1.1.apk). Both projects update independently, but the QR hand-off requires both APKs to use the same signing key.

## N1 setup

- Aerial TV N1 is the recommended Home screen.
- [N1 Pocket Remote](https://github.com/jimmieyangcsjs/N1-Pocket-Remote) provides the phone/PWA remote, text input, app management and touch control.
- [CleanYourTV-N1](https://github.com/jimmieyangcsjs/CleanYourTV-N1) remains available as a rollback desktop.

The Aerial and Remote APKs must be signed with the same key for the protected remote-status provider to work.

## Features

- tvOS-inspired Home screen with a glass Dock and scrollable app grid.
- Persistent 16:9 TV artwork for apps that only ship phone or legacy launcher icons.
- Home and screensaver QR codes for N1 Pocket Remote.
- A compact `56dp` Home QR code with two lightweight labels; it leaves when the app grid reaches the shelf area.
- A compact `132dp` screensaver QR code with a floating info capsule instead of a large dark card.
- QR codes hide automatically when the service is offline, the PIN changes or the LAN address no longer matches.
- Recovery after offline boot, later network connection, network loss and IP changes.
- 1080p-first rendering with vector/high-resolution artwork and a 4K-capable UI path.

## Build

JDK 17 and Android SDK 36 are required.

```powershell
$env:JAVA_HOME="C:\path\to\jdk17"
$env:ANDROID_HOME="C:\path\to\android-sdk"
.\gradlew.bat testStandardDebugUnitTest testGuardDebugUnitTest assembleStandardRelease
```

The APK is written to:

```text
app\build\outputs\apk\standard\release\app-standard-release.apk
```

## Install

```powershell
adb connect <N1-IP>:5555
adb install -r app\build\outputs\apk\standard\release\app-standard-release.apk
adb shell am start -n app.orchard.tv/.MainActivity
adb shell cmd package compile -m speed -f app.orchard.tv
adb shell cmd package set-home-activity app.orchard.tv/.MainActivity
```

Press Home once and verify that the foreground window is `app.orchard.tv/app.orchard.tv.MainActivity`.

The current verified combination is Aerial TV N1 `1.99 (142)` with N1 Pocket Remote `0.1.1 (2)`.

## Documentation

- [N1 design record](docs/superpowers/specs/2026-11-13-aerial-tv-n1-design.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Handoff notes](docs/HANDOFF.md)
- [AI notice](AI-NOTICE.md)
- [Changelog](CHANGELOG.md)

## License

The upstream license and notices are retained in [LICENSE](LICENSE). This N1 adaptation is distributed under the MIT License. Aerial TV N1 is not affiliated with Apple, Phicomm, Xiaomi or TCL.
