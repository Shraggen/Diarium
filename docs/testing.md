# Testing strategy

Diarium uses a layered test strategy so most failures are found without an
emulator, while Android-specific persistence and native speech dependencies
are still exercised on a real Android runtime.

| Layer | Scope | Current coverage |
| --- | --- | --- |
| Unit | Pure Kotlin contracts and transformations | JSON Schema, tool parsing, strict prompts, German/Serbian locale mapping, Unicode transcript parsing, PCM16 WAV encoding |
| Integration | Multiple production components with controlled adapters | Serbian Cyrillic transcript → kernel plan → explicit execution → inspection repository |
| Android instrumentation | Android framework and native dependencies | Room write/read ordering, Activity launch, bundled Silero ONNX inference, optional provisioned Whisper initialization through Llamatik |
| Manual acceptance | Physical microphone, model quality, latency, thermal behavior | English, German, Serbian Latin, and Serbian Cyrillic voice commands on a representative phone |

The integration test deliberately checks that planning leaves the repository
unchanged. Persistence occurs only when the planned call is executed, matching
the confirmation boundary in the app.

## CI gates

The main CI workflow has three jobs:

1. Linux runs Detekt, Konsist/core tests, all shared-logic and shared-UI tests,
   Android lint, and debug app/test APK builds.
2. An API 35 x86_64 emulator runs the Room, launch, and native speech
   instrumentation suite under Android Test Orchestrator.
3. macOS builds the iOS simulator app and its embedded KMP framework.

CodeQL independently scans Actions, Java/Kotlin, and Swift. Its manual build
now compiles the real core, shared, Android, and iOS targets. JavaScript was
removed from the matrix because this repository has no JavaScript product
source.

Test reports, lint reports, and Android APKs are retained as workflow
artifacts. Workflow concurrency cancels stale runs for the same branch.

## Local verification

Run the non-device gate with:

```powershell
.\gradlew.bat detekt :core:jvmTest :app:sharedLogic:allTests `
  :app:sharedUI:allTests :app:androidApp:assembleDebug `
  :app:androidApp:assembleDebugAndroidTest :app:androidApp:lintDebug
```

Run Android instrumentation with:

```powershell
.\gradlew.bat :app:androidApp:connectedDebugAndroidTest
```

`connectedDebugAndroidTest` installs and later removes the target package.
Use an emulator or a test device when its app data matters.

The Whisper initialization test skips when no `.bin` file exists in the
target app's private `files/whisper-models` directory. CI still exercises
Silero and all other instrumentation tests without downloading a large speech
model. A provisioned release/device gate must run the Whisper test without a
skip.

## Multilingual release acceptance

Use a multilingual Whisper model; `.en` variants are not acceptable. Record
and confirm all of these on a physical device:

- English: `I inspected hive 4 and saw the queen.`
- German: `Ich habe Bienenstock 4 kontrolliert und die Königin gesehen.`
- Serbian Latin: `Pregledao sam košnicu 4 i video maticu.`
- Serbian Cyrillic: `Прегледао сам кошницу 4 и видео матицу.`

For each phrase, verify the transcript, proposed `hive_id`, and
`queen_seen` value before confirming. Then force-stop and relaunch the app and
verify that Room restores the journal entry. Test cancellation once per
language and confirm that it writes nothing.

Native ASR quality is data- and device-dependent, especially for lower-resource
languages. German and Serbian golden-audio fixtures should be added once the
team owns representative, consented recordings that can legally live in the
test repository.
