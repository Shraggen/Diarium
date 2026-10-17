# Build your first Diarium APK

This tutorial takes you from a fresh checkout to a debug APK. By the end, you
will have built the Android app without changing any source files.

## Before you start

You need:

- Git;
- any Java 17 or newer on `PATH` or in `JAVA_HOME`, used only to start the
  Gradle wrapper;
- Android Studio, or an Android SDK location with accepted licenses.

You do not need to install JDK 21 yourself. On first run, Gradle downloads it
into `~/.gradle/jdks` for the build daemon (`gradle/gradle-daemon-jvm.properties`)
and for compilation and tests (`jdk` in `gradle/libs.versions.toml`).

## Get the source

Clone the repository and enter its root directory:

```bash
git clone https://github.com/Shraggen/Diarium.git
cd Diarium
```

The root contains `gradlew`, `settings.gradle.kts`, `core`, and `app`.

## Configure the Android SDK

Open the repository root in Android Studio and wait for the initial Gradle sync
to finish. Select Android SDK 37 if Android Studio asks which SDK to use.

Android Studio creates a local `local.properties` file that tells Gradle where
the SDK is installed. The file is machine-specific and is not committed.
Without Android Studio, set `ANDROID_HOME` to the SDK directory instead.

The build installs missing SDK platforms and build tools on demand
(`android.builder.sdkDownload=true`). This only works for licenses already
accepted in that SDK; Android Studio accepts them during setup, or run
`sdkmanager --licenses` once.

## Build the debug APK

Run:

```bash
./gradlew :app:androidApp:assembleDebug
```

On Windows PowerShell, run
`.\gradlew.bat :app:androidApp:assembleDebug` instead.

The first build downloads the Gradle distribution and project dependencies, so
it takes longer than later builds. A successful build ends with:

```text
BUILD SUCCESSFUL
```

## Find the result

The generated APKs are in:

```text
app/androidApp/build/outputs/apk/debug/
```

Diarium creates one APK for each supported Android ABI. The filename includes
the ABI, such as `arm64-v8a`, `x86_64`, or `x86`.

You have now built Diarium from source. To run it, open the repository root in
Android Studio, select the `androidApp` run configuration and a device or
emulator, then choose **Run**. Android Studio selects the APK that matches the
target device.

Next, read [how to verify a change](verifying-changes.md) before modifying the
project, or use the [module and source-set reference](module-reference.md) to
find the module that owns the behaviour you want to change.
