# RidePlay 0.3.2 — public preview

RidePlay is a DiPlay-based CarPlay receiver tailored for motorcycle riding and
media control integration. This update refines media key reception across Android
versions, fixes orientation reconnect handling, and polishes documentation and
interface localization across all supported languages.

## Changes

- **Media button reception on Android 11 and earlier**: `CarPlayMediaButtonReceiver`
  is now correctly exported so legacy `ACTION_MEDIA_BUTTON` system broadcasts can
  reach RidePlay when routing bootstrap is active.
- **Orientation reconnect indication**: Modifying the orientation lock setting now
  properly presents the "Reconnect now" bar for active CarPlay sessions in accordance
  with the description and architecture guidelines.
- **UI string polishing & localization**: Cleaned up experimental setting descriptions
  across all 10 supported languages, replacing developer-oriented debug phrasing
  with clear, user-facing explanations.
- **Documentation refresh**: Restructured the project READMEs to clearly outline the
  motorcycle use case (direct iPhone-to-headset audio + handlebar controls + pinyin
  cluster text) and documented verified hardware configurations.

## Tested and Verified Environment

- **Motorcycle**: 2019 KTM 390 Duke (KTM MY RIDE)
- **Receiver**: Redmi Note 9 Pro (Snapdragon 750G platform, HyperOS 4 / Android 17)
- **Transmitter**: iPhone 14 (iOS 27)
- **Audio Routing**: iPhone Bluetooth directly connected to Bluetooth headset (iPhone loudspeaker also works)
- **Status**: Visual CarPlay, lossless headset/speaker audio, cluster pinyin display, and
  OEM handlebar control keys (play/pause/next/previous) confirmed working reliably.

## Installation

Install `RidePlay-0.3.2.apk` on the Android receiver. The release package is
`com.chiccccken.rideplay`, versionCode 46.

Verify downloads against `SHA256SUMS.txt`.
