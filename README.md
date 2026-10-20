# Lucid Logger

A private, offline Android dream journal for the moment between sleep and waking.

Lucid Logger is built around one rule: **the alarm does not stop until the dream is safe.** At wake time the app opens over the lock screen and keeps ringing until you record the minimum seconds of voice, or write the minimum number of characters. A hidden emergency exit appears after a configurable delay.

## Features

- Full-screen, exact wake alarms with weekly schedules, volume ramp, vibration and four built-in sounds.
- Strict wake gate with configurable voice seconds and written-character thresholds.
- Optional snooze (off by default), re-ring on early exit, and a hold-to-dismiss emergency exit.
- Voice capture with live waveform and AAC/M4A storage.
- Obsidian-compatible Markdown notes with YAML frontmatter, tags, `[[wikilinks]]`, audio embeds and waveform metadata.
- Multiple dreams per night and a morning-edit placeholder for voice-only captures.
- Dream journal with search, filters and streak/lucidity metrics.
- Entity graph for recurring people, places, emotions and dream signs.
- Calendar heatmap of dream frequency.
- Obsidian folder export via Android's Storage Access Framework, plus full ZIP backup.
- App-private storage. No account, analytics, network sync or cloud dependency.

## Project layout

```
app/src/main/java/com/lemomation/lucidlogger/
  alarm/        Alarm scheduling, foreground ringing service and ring activity
  audio/        Voice recorder, playback controls, sound preview and catalog
  data/         Markdown codec, dream store, alarms, settings and export
  ui/           Compose screens, theme and shared components
```

## Build

The repository is built by GitHub Actions on every push to `main` and every pull request.

- **CI workflow:** `.github/workflows/android.yml`
- **Requirements:** JDK 17, Android SDK 35, Gradle 8.9
- Open the project in Android Studio, or run with a local Gradle 8.9 installation:

```bash
./gradlew assembleDebug
```

The CI job runs unit tests, lint and `assembleRelease`, then uploads the APK as the `lucid-logger-release` artifact. Pushing a `v*` tag publishes a GitHub Release with that APK.

## Release signing

The workflow signs release builds when these repository secrets exist:

| Secret | Meaning |
| --- | --- |
| `KEYSTORE_BASE64` | Base64-encoded PKCS12 keystore |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_PASSWORD` | Key password |
| `KEY_ALIAS` | Key alias inside the keystore |

Without those secrets, CI still tests and assembles an unsigned release build.

## Device permissions

For reliable wake-ups, allow notifications, exact alarms, full-screen intents and microphone access. The Alarm screen includes a readiness card with direct links to each setting. Some manufacturers also require disabling battery optimization for background alarms.

## Data format

Notes live in the app's private `files/dreams/` directory and are written as Markdown. A typical note starts like this:

```markdown
---
id: "2026-10-20_0345_the-train"
title: "The train that became a river"
date: 2026-10-20
time: "03:45"
created: "2026-10-20T03:45:00+02:00"
tags: ["lucid"]
lucid: true
audio: "audio/2026-10-20_0345_the-train.m4a"
audio_duration: 57
waveform: [12, 20, 55, 91]
source: mixed
schema: 1
---

# The train that became a river

I boarded a [[night train]] beside the [[ocean]].

![[audio/2026-10-20_0345_the-train.m4a]]
```

Exports are one-way. Editing an exported file in Obsidian does not modify the private copy in Lucid Logger.

## Tests

```bash
./gradlew testDebugUnitTest
```

Unit coverage currently includes the Markdown round trip, legacy voice-note parsing, metadata extraction, wikilink protection and text-gate counting.
