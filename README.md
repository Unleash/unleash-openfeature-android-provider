# Unleash OpenFeature Android Provider

Android provider for Unleash and OpenFeature.

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

## Verifier Submodule

The provider contract tests read the shared OpenFeature/Unleash verifier spec from `verifier/`.
After cloning the repo, initialize the submodule:

```bash
git submodule update --init --recursive
```

To refresh the verifier to the pinned commit after pulling:

```bash
git submodule update --recursive
```

To intentionally move the verifier forward:

```bash
git -C verifier fetch
git -C verifier checkout main
git -C verifier pull --ff-only
git add verifier
```

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
