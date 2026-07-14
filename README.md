# Diarium

Diarium is a mobile-first, offline field journal built with Kotlin
Multiplatform. The current Android application combines on-device speech
recognition, local LLM inference, confirmed structured tool execution, and a
persistent beekeeping inspection journal.

The implemented voice path supports English, German, Serbian Latin, and
Serbian Cyrillic:

```text
microphone -> Silero VAD -> Whisper -> editable transcript
           -> tool proposal -> explicit confirmation -> Room
```

Start with the [architecture](docs/architecture.md), then use the
[journal](docs/journal.md) for chronological progress and the next-session
handoff. Testing commands and acceptance coverage are in
[testing](docs/testing.md).
