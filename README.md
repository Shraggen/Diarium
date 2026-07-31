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

## Start here

Choose the page that matches what you need:

| If you want to… | Read… |
| --- | --- |
| Learn the build on one reliable path | [Build your first Diarium APK](docs/getting-started.md) |
| Validate work before opening a pull request | [How to verify a change](docs/verifying-changes.md) |
| Find the module that owns some behaviour | [Module and source-set reference](docs/module-reference.md) |
| Understand the safety and platform boundaries | [Architecture overview](docs/architecture.md) |
| Understand the test layers and their purpose | [Testing strategy](docs/testing.md) |
| Prepare a GitHub release | [Release process](docs/releasing.md) |

The full documentation is available at
[shraggen.github.io/Diarium](https://shraggen.github.io/Diarium/).

## Status

Diarium is under active development. The current milestone focuses on one
bounded workflow—recording a hive inspection with a hive identifier and an
explicit queen observation—so its safety properties can remain clear and
executable. Hotword detection, text-to-speech, production model downloads,
additional inspection tools, synchronization, and full iOS parity are not
implemented yet.

## Contributing

Issues and pull requests are welcome. Keep changes focused, update the readable
Gherkin scenario and its executable counterpart together when behaviour changes,
run the [verification guide](docs/verifying-changes.md), and use a
[Conventional Commit](https://www.conventionalcommits.org/) title for pull
requests.

## License

Licensed under the [Apache License 2.0](LICENSE).
