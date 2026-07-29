# How to verify a change

This guide shows contributors how to select and run the narrowest useful
verification for a change. Run commands from the repository root.

## Choose the gate for your change

Start with the row that matches the files you changed. If a change spans
several rows, use the most comprehensive applicable command.

| Changed area | Command |
| --- | --- |
| Documentation and `mkdocs.yml` | `mkdocs build --strict` |
| `core` | `./gradlew detekt :core:jvmTest --stacktrace --no-daemon` |
| `app/sharedLogic` | `./gradlew detekt :core:jvmTest :app:sharedLogic:allTests --stacktrace --no-daemon` |
| `app/sharedUI` | `./gradlew detekt :core:jvmTest :app:sharedLogic:allTests :app:sharedUI:allTests --stacktrace --no-daemon` |
| Android host or build configuration | Use the full non-device gate below |

On Windows PowerShell, replace `./gradlew` with `.\gradlew.bat`.

## Verify documentation

Create an isolated Python environment and install the same documentation
package used by CI:

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install mkdocs-material
mkdocs build --strict
```

On Windows PowerShell, activate the environment with:

```powershell
.\.venv\Scripts\Activate.ps1
```

If the strict build reports a missing page, update both the link and the
`nav` section of `mkdocs.yml` as appropriate.

## Run the full non-device gate

Before opening a pull request that changes code or build configuration, run:

```bash
./gradlew detekt :core:jvmTest :app:sharedLogic:allTests \
  :app:sharedUI:allTests :app:androidApp:assembleDebug \
  :app:androidApp:assembleDebugAndroidTest :app:androidApp:lintDebug \
  --stacktrace --no-daemon
```

This matches the Linux static-analysis, shared-test, Android-build, and lint
coverage in CI without starting an emulator.

## Verify Android runtime behaviour

If the change affects Room, application launch, audio capture, Silero, Whisper
initialisation, or another Android runtime boundary, connect an emulator or
test device and run:

```bash
./gradlew :app:androidApp:connectedDebugAndroidTest \
  --stacktrace --no-daemon
```

The task installs and later removes the target package. Use a dedicated
emulator or test device if existing app data matters.

The Whisper initialisation test skips when the target app has no `.bin` file in
its private `files/whisper-models` directory. A release or device check that
claims Whisper coverage must provision a compatible multilingual model and
confirm that the test did not skip.

For the purpose and coverage of each test layer, see the
[testing strategy](testing.md).
