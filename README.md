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

## Initialization

`initialize()` starts the underlying Unleash client and waits for the first toggle
state to land, so `OpenFeatureAPI.setProviderAndWait(...)` returns with flags in hand.
That state may come from the network or from the local backup on disk, so readiness
means usable flags rather than fresh ones. The client is started by the provider
regardless of `delayedInitialization`, which Unleash leaves enabled by default.

The wait is bounded, because Unleash signals readiness only once a non-empty toggle set
arrives, and three configurations never produce one: a failed fetch, a project with no
toggles, and `pollingStrategy.enabled = false`. On timeout the provider logs a warning
and evaluations fall back to defaults until the background poll catches up. Three
seconds is the default:

```kotlin
UnleashOpenFeatureProvider(
    androidContext = androidContext,
    unleashConfig = config,
    initializationTimeout = 2.seconds,
)
```

Do not gate your first frame on this call: on a cold start with no connectivity it
suspends for the full timeout.

## Build

```bash
./gradlew assembleDebug
```

## Test

```bash
./gradlew test
```

## Release

Release setup and publishing steps are documented in [RELEASE.md](./RELEASE.md).

The published coordinates are:

```kotlin
implementation("io.getunleash:unleash-openfeature-android-provider:<version>")
```

## Run Example

Open this project in Android Studio and run the `app` configuration, or install the debug APK on a connected device:

```bash
./gradlew :app:installDebug
```
