# Release signing workflow

Release publishing is gated on an annotated signed Git tag. GitLab CI verifies the tag before publishing Maven artifacts.

## Trusted release keys

Trusted release signing public keys are committed in:

```sh
ci/trusted-release-gpg-keys.asc
```

Add a trusted public key with:

```sh
gpg --export-options export-minimal --armor --export <fingerprint> >> ci/trusted-release-gpg-keys.asc
```

Use the full key fingerprint instead of a short key ID. Review changes to this file like code: adding a key grants that key release-signing authority.

Check the committed keyring with:

```sh
gpg --show-keys ci/trusted-release-gpg-keys.asc
```

## Creating a release tag

Create an annotated signed tag from a commit that has already landed on the default branch:

```sh
git checkout main
git pull --ff-only
git tag -s <version> -m "Release <version>"
git push origin <version>
```

If you have multiple signing keys, select the release key explicitly:

```sh
git tag -s <version> -u <fingerprint> -m "Release <version>"
```

Local verification:

```sh
git verify-tag -v <version>
```

## CI release gate

For tag pipelines, the `release:verify_tag` job:

- imports public keys from `ci/trusted-release-gpg-keys.asc` into a clean GNUPGHOME;
- fails if no valid public keys are imported;
- fetches the tag and the default branch;
- requires the tagged commit to be contained in the default branch;
- rejects lightweight tags;
- runs `git verify-tag -v <tag>`.

The `publish:maven` job depends on `release:verify_tag` for tag pipelines. If
tag verification fails, Maven publishing does not run.

## Trust model

Keeping trusted keys in git is intentional. It makes release authority
reviewable and avoids relying on mutable GitLab CI variables for the release
trust root.

This does not make GitLab a fully trusted release authority. Consumers and
external release tooling should verify signed tags independently when stronger
provenance is required.
