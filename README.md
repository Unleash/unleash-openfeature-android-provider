# Unleash OpenFeature Android Provider

Android provider for Unleash and OpenFeature.

This is currently a scaffold with a runnable Android hello-world app. Provider implementation comes next.

## Requirements

- JDK 17+
- Android SDK installed locally or through CI

## Local SDK Setup

Gradle needs the Android SDK location before CLI tasks can run. Either export `ANDROID_HOME`:

```bash
export ANDROID_HOME=/home/some-user/Android/Sdk
```

Or create an untracked `local.properties` file in the repo root:

```properties
sdk.dir=/home/some-user/Android/Sdk
```

Android Studio usually creates `local.properties` automatically when opening the project. The file is ignored because SDK paths are machine-specific.

## Build

```bash
./gradlew assembleDebug
```

## Test

```bash
./gradlew test
```

## Run Example

Open this project in Android Studio and run the `app` configuration, or install the debug APK on a connected device:

```bash
./gradlew :app:installDebug
```
