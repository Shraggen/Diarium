# Build your first Diarium APK

This tutorial takes you from a fresh checkout to a debug APK. By the end, you
will have built the Android app without changing any source files.

## Before you start

You need:

- Git;
- JDK 21;
- Android Studio with Android SDK 37 installed.

Run:

```bash
java -version
```

The output should identify Java 21. If it reports another version, configure
`JAVA_HOME` for JDK 21 before continuing.

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
