# Release

Releases are published from the manual `Release` GitHub Actions workflow on `main`.

The workflow reads `VERSION_NAME` from `gradle.properties`, validates that the `v<version>` tag does not already exist, creates and pushes the tag, publishes the `:provider` Android AAR to Maven Central through Central Portal, and creates a GitHub release.

## Version

`VERSION_NAME` in `gradle.properties` is the release version source of truth.

The same value is used for:

- the Maven publication version
- the generated `BuildConfig.SDK_FLAVOUR_VERSION`
- the Git tag and GitHub release name, prefixed with `v`

## Required Secrets

Configure these GitHub Actions secrets:

- `JRELEASER_GPG_PUBLIC_KEY`
- `JRELEASER_GPG_SECRET_KEY`
- `JRELEASER_GPG_PASSPHRASE`
- `JRELEASER_MAVENCENTRAL_USERNAME`
- `JRELEASER_MAVENCENTRAL_TOKEN`

The workflow uses the built-in `github.token` for pushing the release tag and creating the GitHub release. No Unleash bot app token is required.

## GPG Key Setup

Create a release signing key:

```bash
gpg --full-generate-key
gpg --list-secret-keys --keyid-format LONG
```

Export the key material for GitHub Actions:

```bash
gpg --armor --export <KEY_ID> > public.asc
gpg --armor --export-secret-keys <KEY_ID> > secret.asc
```

Use the contents of `public.asc` for `JRELEASER_GPG_PUBLIC_KEY`, the contents of `secret.asc` for `JRELEASER_GPG_SECRET_KEY`, and the key passphrase for `JRELEASER_GPG_PASSPHRASE`.

Publish the public key to a public keyserver so Maven Central users and tooling can verify artifact signatures:

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
```

Sonatype Central currently documents these supported keyservers:

- `keyserver.ubuntu.com`
- `keys.openpgp.org`
- `pgp.mit.edu`

## Maven Central Setup

Before the first release:

1. Make sure the `io.getunleash` namespace is available in Sonatype Central Portal.
2. Generate a Central Portal user token.
3. Store the token name as `JRELEASER_MAVENCENTRAL_USERNAME`.
4. Store the token value as `JRELEASER_MAVENCENTRAL_TOKEN`.

This project publishes an Android AAR. JReleaser is configured to sign artifacts and verify sources/Javadocs are staged, but its PomChecker-based POM verification is disabled because PomChecker does not understand Maven POMs with `<packaging>aar</packaging>`.

## Local Dry Run

Stage the Maven publication locally:

```bash
./gradlew :provider:publishReleasePublicationToStagingRepository
```

Validate the JReleaser configuration with local or dummy environment values:

```bash
JRELEASER_GITHUB_TOKEN=dummy \
JRELEASER_GPG_PUBLIC_KEY=dummy \
JRELEASER_GPG_SECRET_KEY=dummy \
JRELEASER_GPG_PASSPHRASE=dummy \
JRELEASER_MAVENCENTRAL_USERNAME=dummy \
JRELEASER_MAVENCENTRAL_TOKEN=dummy \
./gradlew jreleaserConfig
```

## Publishing

1. Update `VERSION_NAME` in `gradle.properties`.
2. Merge the version change to `main`.
3. Run the manual `Release` workflow from `main`.
4. Confirm the workflow creates `v<version>`, publishes the Maven Central deployment, and creates the GitHub release.
