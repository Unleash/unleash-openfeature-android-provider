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

## Release

Releases are published from the manual `Release` GitHub Actions workflow on `main`.
The workflow reads `VERSION_NAME` from `gradle.properties`, validates that the `v<version>` tag does not already exist, creates and pushes the tag, publishes the `:provider` Android AAR to Maven Central through Central Portal, and creates a GitHub release.

The published coordinates are:

```kotlin
implementation("io.getunleash:unleash-openfeature-android-provider:<version>")
```

Required GitHub Actions secrets:

- `JRELEASER_GPG_PUBLIC_KEY`
- `JRELEASER_GPG_SECRET_KEY`
- `JRELEASER_GPG_PASSPHRASE`
- `JRELEASER_MAVENCENTRAL_USERNAME`
- `JRELEASER_MAVENCENTRAL_TOKEN`

To create a release signing key:

```bash
gpg --full-generate-key
gpg --list-secret-keys --keyid-format LONG
gpg --armor --export <KEY_ID> > public.asc
gpg --armor --export-secret-keys <KEY_ID> > secret.asc
```

Use the contents of `public.asc` for `JRELEASER_GPG_PUBLIC_KEY`, the contents of `secret.asc` for `JRELEASER_GPG_SECRET_KEY`, and the key passphrase for `JRELEASER_GPG_PASSPHRASE`.

Before the first release, make sure the `io.getunleash` namespace is available in Sonatype Central Portal and generate a Central Portal user token. Store the token name as `JRELEASER_MAVENCENTRAL_USERNAME` and the token value as `JRELEASER_MAVENCENTRAL_TOKEN`.

To dry-run the staging step locally:

```bash
./gradlew :provider:publishReleasePublicationToStagingRepository
```

## Run Example

Open this project in Android Studio and run the `app` configuration, or install the debug APK on a connected device:

```bash
./gradlew :app:installDebug
```
