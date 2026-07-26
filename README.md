# Diarium

[![CI](https://github.com/Shraggen/Diarium/actions/workflows/ci.yml/badge.svg)](https://github.com/Shraggen/Diarium/actions/workflows/ci.yml)
[![Documentation](https://github.com/Shraggen/Diarium/actions/workflows/docs.yml/badge.svg)](https://shraggen.github.io/Diarium/)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

Diarium is an offline-first, multilingual field journal for beekeepers. It
turns a typed or spoken inspection note into a structured, reviewable action
and saves it locally only after explicit confirmation.

The Android app is the first complete runtime. Shared domain logic and UI are
built with Kotlin Multiplatform; an iOS host is present and built in CI, but
does not yet have feature parity with Android.

## Why Diarium?

- **Private by default** — speech recognition, command planning, and journal
  persistence run on the device.
- **Safe writes** — planning is side-effect free; cancellation, ambiguity, or
  contradiction writes nothing.
- **Multilingual input** — English, German, Serbian Latin, and Serbian
  Cyrillic are supported by the inspection workflow.
- **Editable proposals** — users can inspect the transcript and structured
  fields before committing a record.
- **Offline persistence** — confirmed inspections are stored with Room and
  restored across app launches.

## How it works

```text
microphone ──> Silero VAD ──> Whisper ──> editable transcript
                                                   │
typed note ────────────────────────────────────────┘
                                                   ▼
                                  deterministic tool proposal
                                                   ▼
                                      review and confirmation
                                                   ▼
                                          Room / SQLite
```

Diarium deliberately separates probabilistic transcription from deterministic
planning and execution. A proposed write cannot access persistence directly,
and missing or conflicting facts keep confirmation disabled.

## Getting started

### Prerequisites

- Android Studio with Android SDK 37 installed
- JDK 21
- An Android device or emulator running Android 8.0 (API 26) or newer
- For voice input, a compatible **multilingual** Whisper `.bin` model

Clone the repository and build the debug APK:

```bash
git clone https://github.com/Shraggen/Diarium.git
cd Diarium
./gradlew :app:androidApp:assembleDebug
```

On Windows PowerShell, use `.\gradlew.bat` instead of `./gradlew`.

Open the repository root in Android Studio, select the `androidApp` run
configuration, and run it on a device or emulator. Typed inspection commands
work immediately. For voice input, choose **Select Whisper model** in the app
and import a multilingual `.bin` model; the app copies it into private storage.
The English-only `.en` model variants do not cover German or Serbian.

Try one of these commands:

```text
I inspected hive 4 and saw the queen.
Ich habe Bienenstock 4 kontrolliert und die Königin gesehen.
Pregledao sam košnicu 4 i video maticu.
Прегледао сам кошницу 4 и видео матицу.
```

Review the proposed hive and queen observation, then confirm or cancel the
journal write.

## Verification

Run the local non-device gate:

```bash
./gradlew detekt :core:jvmTest :app:sharedLogic:allTests \
  :app:sharedUI:allTests :app:androidApp:assembleDebug \
  :app:androidApp:assembleDebugAndroidTest :app:androidApp:lintDebug
```

With an emulator or device connected, run the Android instrumentation suite:

```bash
./gradlew :app:androidApp:connectedDebugAndroidTest
```

See the [testing strategy](docs/testing.md) for the test layers, native-model
behavior, and multilingual release checklist.

## Project structure

| Module | Responsibility |
| --- | --- |
| `core` | Domain-neutral schemas, tools, planning contracts, and execution kernel |
| `app/sharedLogic` | Beekeeping rules, speech contracts, and application composition |
| `app/sharedUI` | Shared Compose UI and localized copy |
| `app/androidApp` | Android host, audio and model runtimes, coordinators, and Room |
| `app/iosApp` | iOS host consuming the shared KMP framework |

For source-set and package details, see the
[project structure guide](docs/project-structure.md).

## Documentation

The full documentation is available at
[shraggen.github.io/Diarium](https://shraggen.github.io/Diarium/).

- [Architecture overview](docs/architecture.md)
- [Architecture decisions](docs/decisions/0001-microkernel-tool-contracts.md)
- [Development model](docs/development-model.md)
- [Testing strategy](docs/testing.md)
- [Release process](docs/releasing.md)
- [Project journal](docs/journal.md)

## Status

Diarium is under active development. The current milestone focuses on one
bounded workflow—recording a hive inspection with a hive identifier and an
explicit queen observation—so its safety properties can remain clear and
executable. Hotword detection, text-to-speech, production model downloads,
additional inspection tools, synchronization, and full iOS parity are not
implemented yet.

## Contributing

Issues and pull requests are welcome. Keep changes focused, update the readable
Gherkin scenario and its executable counterpart together when behavior changes,
and use a [Conventional Commit](https://www.conventionalcommits.org/) title for
pull requests.

## License

Licensed under the [Apache License 2.0](LICENSE).
