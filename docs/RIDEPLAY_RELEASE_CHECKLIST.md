# RidePlay 0.3.1 release validation

Package: `com.chiccccken.rideplay`, version `0.3.1`, versionCode `45`.

APK SHA256: `20cafb862534ca93494bc09a432f3abcb6c40c49c99fb2192e0a18772176891b`.

## Build checks

- Full Android check command completed: shared 1801, common 1800 and Home 11 tests, with no failures (3612 total).
- Mobile, Home and MapHost Debug builds and lint passed; standalone release build and release lint passed.
- Release signature, package and non-debuggable status verified. All DEX, resources, manifest and authentication assets match the field-tested candidate; native libraries were rebuilt solely to map machine-specific diagnostic paths to relative paths. All four ABIs were rescanned.
- Temporary subscription and packet-pacing experiments removed. Original subscriptions, window and retransmission behavior retained.

## Device acceptance

The owner reported successful in-place installation, connection, app reconnect,
connection after restarting the Android receiver and repeated real-vehicle tests.
Handlebar play/pause/track changes and iPhone-to-headset music were explicitly confirmed.

This feedback is separate from automated checks. It does not establish Siri/call
audio, every background-recovery scenario or other vehicles/ROMs.

## Release artifacts

Publish one main APK, `SHA256SUMS.txt` and corresponding source under `v0.3.1` as a
prerelease. Preserve the original license and third-party notices. Source archives
exclude private signing/authentication files and internal device reports.

After publication, verify the actual release JSON, APK download/checksum and the
RidePlay update endpoint. Public hosting is not established by local build checks.

See [release notes](RELEASE-NOTES-0.3.1.md) for installation and compatibility scope.

## Publication privacy review

A prepublication audit found machine-local paths in the native echo library. The
release rebuild normalizes compiler file paths; no Kotlin/Java, protocol, DSP
algorithm or feature setting changed. The field-tested candidate and release
APK therefore differ in native rebuild outputs and signing data only. The full
Android check command and standalone release/lint passed after this change.
