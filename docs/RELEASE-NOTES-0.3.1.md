# RidePlay 0.3.1 — public preview

RidePlay is a DiPlay-based CarPlay receiver with opt-in adaptations for motorcycle
media controls. This preview has been tested on a 2019 KTM 390 Duke with a Redmi
Note 9 Pro running a ported HyperOS build and an iPhone 14. Other vehicles and ROMs
need their own testing.

## Changes

- Optional audio-free media controls forward Android media keys to the iPhone.
  The default-off **Media-key routing bootstrap (experimental)** helps the tested
  ROM select RidePlay for handlebar media keys after iPhone media information is
  available or the driver returns to CarPlay.
- RidePlay registers its own media-button receiver when this mode is enabled,
  including recovery after process restart and an in-place app update.
- Update checks now use `Chiccccken/RidePlay` releases and reject downloads from
  other repositories. Settings backups include RidePlay media-control, pinyin,
  orientation and artist-name choices, with type validation.
- Diagnostic reports include bounded iAP2 command summaries and unacknowledged
  packet attribution without exporting protocol payloads. Temporary subscription
  and packet-pacing experiments are not included in this release.
- Native diagnostic filenames use project-relative paths, so the distributed
  libraries do not expose the build machine's username or source directory.

## Tested and known limitations

The owner confirmed installation, reconnecting within the app, connecting after a
Redmi reboot, repeated vehicle use, handlebar play/pause/track changes and music
from the iPhone to the headset. The app's runtime diagnostics reported iOS 27.2.1.
These results do not establish Siri/call audio, every background-recovery scenario,
other vehicles or other ROMs.

- On this tested HyperOS combination, set **both the system hotspot and RidePlay's
  saved hotspot description to WPA2**, with matching name and password. WPA3 and
  transition-mode attempts failed; the exact failing layer is unresolved. This is
  not a claim that all devices require WPA2. RidePlay does not change the system
  hotspot automatically.
- The instrument's play/pause icon may stay on the play triangle while the buttons
  work normally. This display issue is deferred.
- Routing bootstrap is off by default and takes effect after reconnecting. When
  enabled, returning to CarPlay may take media-key ownership from another Android
  player. The tested control-only setup keeps music output on the iPhone/headset.

## Installation

Install `RidePlay-0.3.1.apk` on the Android receiver. The release package is
`com.chiccccken.rideplay`, versionCode 45. It can update the same officially signed
RidePlay candidate/diagnostic package in place. Installation ends an active
CarPlay session, so reconnect afterward. Do not uninstall or clear app data to
perform this update.

The earlier `.debug` package is a separate installation. Back up settings before
moving from it, configure the new app's permissions and hotspot details, and run
one receiver app at a time. Do not delete a shared iPhone vehicle record merely
to change the Android app package.

Verify downloads against `SHA256SUMS.txt`. The release includes corresponding
source with the original license notices. The standalone APK includes the
upstream experimental accessory authentication identity; it is not an
Apple-certified product. Source archives exclude authentication keys and the
Android release-signing key. See [third-party notices](THIRD_PARTY_NOTICES.md) and
[build instructions](BUILD.md).

## 中文摘要

0.3.1 是面向有限实测范围的公开测试版。已确认安装连接、App 内重连、红米重启后
连接、多次实车使用、车把播放／暂停／切歌，以及 iPhone 到头盔的音乐输出正常。

新增默认关闭的媒体按键选路引导，改善实测 ROM 的车把媒体键选路；更新检查改为
RidePlay 自有仓库，设置备份补齐个性化项目，诊断增加安全的协议命令摘要。
本版保留原有完整协议订阅及发送行为，不包含临时跳过订阅或限制发送节奏的实验。
原生库诊断文件名已映射为项目相对路径，公开包不再包含构建机器的用户名或源码目录。

实测红米组合请让系统热点和 App 已保存的热点描述**同时使用 WPA2**，名称、密码
一致。WPA3 的具体失败原因尚未定位，不推广为所有设备的限制。仪表播放／暂停图标
固定显示播放三角形仍为已知问题；Siri／电话等未确认的路径不扩大验收。

同正式签名的候选／诊断版可覆盖安装并保留数据；旧 `.debug` 包与正式包互相独立。
安装会结束当前连接，之后重新连接即可。源码、许可证和校验文件随发布提供。
