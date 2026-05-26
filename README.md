# FloatKill

Floating button that kills (and optionally relaunches) the foreground app with a single tap.

Designed for the recurring annoyance where an app gets stuck / laggy / misbehaves and you want a one-tap "kill and restart" without diving into Settings > App info every time.

## What it does

A small draggable bubble overlays whatever app you're using. Tap it:

- **Short tap** → kill the foreground app **and** relaunch it (fresh state)
- **Long press** → menu with *Kill only* / *Stop service* / *Open settings*
- **Drag** → reposition anywhere on screen; release snaps to the nearest edge

Position persists across reboots. A Quick Settings tile is also exposed to toggle the bubble from the notification shade.

## How it kills

FloatKill auto-picks the best strategy available on the device. In order of preference:

| Strategy | Speed | Visibility | Requirements |
|---|---|---|---|
| **Direct** (`forceStopPackage`) | Instant | Invisible | `FORCE_STOP_PACKAGES` pre-granted (rare — only some ROMs) |
| **Recents dismiss** | ~1 s | Brief Recents flash | Accessibility service enabled |
| **Settings force-stop** | ~2 s | Brief Settings flash | Accessibility service enabled |

Recents and Settings are always available once you grant the accessibility service. Direct is a silent fast-path when the permission happens to be pre-granted on the ROM.

### Kill mode toggle

In the main screen you can choose:

- **Recents (soft kill)** — default. Faster, less invasive, but apps with foreground services (WhatsApp, Spotify, etc.) may survive.
- **Settings (hard kill)** — slower, but guaranteed `forceStopPackage` via the system dialog.

If Recents fails for any reason (OEM not exposing dismiss action, card not found, app with sticky foreground service), FloatKill automatically retries the kill via Settings — you never get a silent fail.

## Permissions

| Permission | Why |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Draw the floating bubble |
| Accessibility service | Detect the foreground package + drive the Settings/Recents UI |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` | Host the bubble as a foreground service |
| `POST_NOTIFICATIONS` (Android 13+) | Persistent service notification |

No internet permission. No analytics. No background calls.

## Compatibility

- **Min Android**: 8.0 (API 26)
- **Target Android**: 15 (API 35)
- **Tested on**: Samsung Galaxy S24 Ultra — Android 16. OEM-specific Recents card structure handled (Samsung exposes a custom `Close` action instead of the AOSP `ACTION_DISMISS`).
- **Other OEMs**: should work on stock Android (AOSP resource IDs covered); Xiaomi / OnePlus / Vivo not yet validated — PRs welcome

## Install

Grab the latest APK from [Releases](../../releases) and:

```
adb install app-debug.apk
```

or sideload it via your file manager (enable "Install from unknown sources" for the source app).

After install:

1. Open FloatKill
2. Tap **Open Overlay settings** and grant the permission
3. Tap **Open Accessibility settings** and enable the FloatKill service
4. Tap **Start floating button**

## Why not Play Store?

Play Store rejects apps that use `AccessibilityService` for non-disability purposes — even legitimate ones. So this stays on GitHub / sideload only. Same fate as Brevent, SuperFreezZ, and similar.

## Building from source

```
git clone https://github.com/Ares9323/FloatKill.git
cd FloatKill
./gradlew :app:assembleDebug
```

Requires JDK 21 and Android SDK with API 35 platform + build-tools 35.x.

## Stack

- Kotlin
- AndroidX + Material 1.x (no Compose — plain Views to keep APK small)
- No third-party runtime dependencies beyond AndroidX

## License

MIT. See [LICENSE](LICENSE).
