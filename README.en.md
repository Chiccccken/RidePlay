# RidePlay

*A CarPlay receiver tailored for motorcycle riding.*

[简体中文](README.md) · **English** · [繁體中文](README.zh-TW.md)

<img src="site/assets/icon.png" alt="RidePlay icon" width="160">

RidePlay is an open-source CarPlay receiver tailored from [DiPlay](https://github.com/shihabal3amri/DiPlay), designed to run on Android receiver devices paired with an iPhone. The primary integration target is motorcycle systems supporting Bluetooth clusters and handlebar controls (e.g., **KTM MY RIDE**).

[Download Releases](https://github.com/Chiccccken/RidePlay/releases) · [0.3.1 Release Notes](docs/RELEASE-NOTES-0.3.1.md) · [Build from Source](docs/BUILD.md)

## Background & Use Case

In motorcycle riding setups, many riders prefer having their iPhone output audio directly to a Bluetooth helmet headset (or intercom) to enjoy uncompressed, zero-latency audio and clear communications, while mounting an Android device on the handlebar to serve as a CarPlay navigation display.

While DiPlay provides robust wired and wireless CarPlay core reception, using this audio-direct-to-headset arrangement introduces two challenges:
1. **Handlebar keys stop working**: When CarPlay audio is not routed through the Android receiver, the Android system `MediaSession` remains inactive, preventing the motorcycle's handlebar buttons from sending play/pause/track-skip commands to the iPhone.
2. **Cluster text corruption**: Some motorcycle instrument clusters (such as KTM LCD displays) do not support Chinese character sets, resulting in illegible blocks or garbled text when displaying Chinese song titles.

RidePlay builds upon DiPlay's core CarPlay engine by adding audio-free media session bridging, cluster pinyin metadata conversion, and orientation locking to deliver a seamless riding experience.

## Key Features

- **Audio-Free Media Controls (Experimental)**: When CarPlay audio is output directly from the phone (e.g., to a helmet headset or vehicle Bluetooth) without playing sound on the Android receiver, RidePlay continues to publish playback metadata to the system and routes handlebar controls (play/pause/skip) to the iPhone.
- **Media-Key Routing Bootstrap (Experimental)**: For third-party or ported Android ROMs that only dispatch media keys to active audio streams, briefly primes the audio channel upon connection or return to ensure physical handlebar keys route to RidePlay.
- **Cluster Pinyin & Artist Aliases**: Converts Chinese song titles, artists, and album names into pinyin offline to avoid garbled text on clusters without Chinese font support. Supports custom exact artist replacements (e.g., abbreviations or English names). Only transformed metadata is published to the cluster; the CarPlay UI maintains original text.
- **App Orientation Preferences**: Supports locking orientation to landscape or portrait, with smooth transition mitigations to prevent orientation flicker during launch.
- **Independent Coexistence & Secure Updates**: Uses a distinct application identity (`com.chiccccken.rideplay`), allowing side-by-side installation with upstream DiPlay. In-app update checks strictly verify release sources and checksums.
- **Enhanced Backup & Diagnostics**: Configuration backup covers media control, pinyin conversion, and orientation preferences. Diagnostic reports redact protocol payloads and sensitive fields.

## Verified Configurations

The following hardware combination has completed extensive multi-day physical testing, with all core features confirmed working stably:

- **Motorcycle**: 2019 KTM 390 Duke (KTM MY RIDE)
- **Receiver**: Redmi Note 9 Pro (Snapdragon 750G platform, HyperOS 4 / Android 17)
- **Transmitter**: iPhone 14 (iOS 27)
- **Audio Routing**: iPhone Bluetooth directly connected to Bluetooth headset (iPhone loudspeaker also works)
- **Observed Results**:
  - Stable wired and wireless CarPlay visual streams;
  - Lossless audio direct to headset or speaker with zero lag;
  - KTM cluster accurately displays song pinyin, artist, and playback progress;
  - OEM KTM handlebar controls (play, pause, next, previous) reliably control iPhone playback;
  - Reconnection after device reboot or app restart functions smoothly.

*Note: Instrument cluster protocols and Android firmware implementations vary across models. Testing and feedback on other motorcycle brands are welcome.*

## Installation & Building

Install on the **Android receiver**, supporting Android 7.0+ (API 24). Release builds use `com.chiccccken.rideplay`; debug builds use `com.chiccccken.rideplay.debug`.

Download the APK from [RidePlay Releases](https://github.com/Chiccccken/RidePlay/releases) and verify with `SHA256SUMS.txt`.

Standard source/CI builds exclude runtime accessory authentication assets. Standalone pre-built APKs include upstream experimental accessory identities. Refer to [Build Guide](docs/BUILD.md) and [Third-Party Notices](docs/THIRD_PARTY_NOTICES.md) before building your own testing APK.

```sh
./gradlew :mobile:assembleDebug
```

See [AGENTS.md](AGENTS.md) for full testing and verification commands.

## Repository & Maintenance

The [`custom`](https://github.com/Chiccccken/RidePlay/tree/custom) branch is the active development and release branch; `main` mirrors upstream DiPlay. For details on upstream synchronization, see [Maintenance Design](docs/KTM_UPSTREAM_SYNC.md).

[Installation Guide](docs/INSTALL.md) · [Compatibility](docs/COMPATIBILITY.md) · [Privacy](docs/PRIVACY.md) · [Release Checklist](docs/RIDEPLAY_RELEASE_CHECKLIST.md)

## Credits & License

Special thanks to [DiPlay](https://github.com/shihabal3amri/DiPlay) and [xcertplay](https://github.com/shilapi/xcertplay) for providing a solid foundation. This project is licensed under GPL-3.0. Interface and website design adapt [DiAuto](https://github.com/shihabal3amri/DiAuto) (AGPL-3.0). Original [licenses and third-party notices](docs/THIRD_PARTY_NOTICES.md) are fully preserved. RidePlay is an independent project and is not affiliated with Apple, KTM, or BYD.
