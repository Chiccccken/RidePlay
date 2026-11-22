# KTM AVRCP 故障交接给 Astra（2026-11-04）

## 从哪里开始

用户希望仔细排查，不清楚根因。先读本文，再读 `KTM_AVRCP_HANDOFF_2026-11-03.md` 和 `KTM_FOCUS_EXPERIMENT.md`。11 月 2 日文档仅作历史背景，其中“未取得系统现场证据”已经过时。

**已证实：RidePlay 会话存在、歌曲与播放状态正常发布；KTM 按键到达 Android 蓝牙，但系统媒体按键目标与蓝牙活动播放器没有选中 RidePlay。11 月 4 日实车证明：即使 RidePlay 已持有音频焦点，也没有解决选路。更底层的根因仍未确定。**

先分析技术约束、列出有证据可区分的分支，再制定最小实验。不要直接把静音 AudioTrack、伪造 PLAYING、Root 或重写元数据当作已确定的修复。用户此前已多次去车旁测试，下一次应先把实验与采集在电脑旁准备好。

## 用户事实与约束

- 设备：2019 KTM 390 Duke；红米 Note 9 Pro / M2007J17C / gauguinpro；iPhone 14；恒疆 C50。
- 红米是移植 ROM，自报 API 37 / Android 17 / OS4.0.5.0.XPCCNXM；不能据营销名推断官方支持或框架实现。
- 声音必须保持 iPhone → C50；不接受每次手动切 iPhone 音频输出充当验收。
- 信息目标：iPhone Now Playing → RidePlay MediaSession → Android 蓝牙 AVRCP → KTM；按键反向经 MediaSession callback、CarPlay HID 到 iPhone。
- 红米本机播放器可由 KTM 车把暂停、播放、切歌；仪表中文乱码是用户已知的中文显示限制，**不是红米本机乱码，不是本次新增故障**。RidePlay 已有可选拼音转换。
- 本机和 CarPlay 场景，用户都已在红米媒体输出面板选择 KTM；不要反复建议选择输出设备。输出设备选择与目标媒体会话选择不同。
- Android 媒体卡片与 CarPlay 本身正常；此前媒体卡片可控制 iPhone（历史反馈）。本轮 KTM 歌曲和车把控制仍不正常。
- 保持实车连接时不能同时用电脑 USB 取日志，但预先启动 shell 采集、拔线、回来取文件的方式已实际验证。
- 用户 11 月 4 日补充：最初忘记清掉红米后台音乐，仪表能显示该音乐；清后台后失效。仪表按键调音量正常。音量正常不能证明 PLAY/NEXT 已路由到 RidePlay。

## 11 月 4 日新证据：焦点实验无效

电脑原始快照：

`/tmp/rideplay-ktm-field-review/RidePlay-KTM-focus-20261104-01/`

手机同名目录：`/sdcard/Download/RidePlay-KTM-focus-20261104-01/`。

电脑补充摘要：`/tmp/rideplay-ktm-field-review/focus-result-20261104.md`。
应用当前自身日志副本：`/tmp/rideplay-ktm-field-review/rideplay-own-20261104.log`。
临时目录可能被清理，先核查存在。原始系统报告包含设备身份、MAC 与歌曲信息，只在本地分析，不提交公开仓库。

采集 10:28:32 启动，已回收 43 组（至 10:42:21）。回来后已写 STOP，未修改设置或 APK。

### 10:31:40：后台 Apple Music 还在

读取 `20261104-103140-media_session.txt`、`20261104-103140-bluetooth.txt`、`20261104-103140-audio.txt`：

- 系统媒体按键目标为 `com.apple.android.music`。
- Bluetooth 的 `<Active>` 同样是 Android Apple Music，缓存歌曲为 `The Falling Summer`。
- RidePlay 另有 active 会话，PLAYING，歌曲描述是 `Shi, Lei Guang Xia, 音乐`。
- RidePlay 位于 Audio Focus stack，gain=GAIN、loss=none。

所以仪表最初显示的是红米后台 Apple Music 的信息，不能算 RidePlay 部分成功。

### 10:31:56 至 10:32:43：清掉后台后

读取 `20261104-103156-media_session.txt`、`20261104-103156-bluetooth.txt`、对应 audio/keys 文件，再看 103227、103243：

- 系统 `Media button session is null`。
- RidePlay 仍 active、PLAYING，进度及歌曲更新，如 `Tian Hei Hei, Sun Yan Zi`、`Shao Nv, Lin You Jia`。
- Bluetooth 的 RidePlay MediaController 描述可读到新歌曲，但 Current Data 缓存仍是 `Not Provided`、空 artist/album、duration=0、初始 PAUSED、position=-1。没有 `<Active>` RidePlay。
- 车把 PREVIOUS/PLAY 等事件到 AvrcpTargetService，日志为 `to null ... internal active player is null`。
- MediaSessionService 尝试回退到历史 NetEase `PendingIntentHolder`。这不是存在一个已选中的活动网易云会话。
- **同时 Audio Focus stack 中 RidePlay 仍是 GAIN、loss=none。焦点已拿到，但选路未改善。**

本地音频播放历史只见 System UI 和 Android Apple Music（uid 10323），没有 RidePlay（uid 10372）。这支持调查真实本地音频活动与选择条件，但不是“静音播放必然有效”的证据。

### 第二次连接与开关边界

10:32:48 旧焦点释放，10:33:26 新媒体按键焦点申请；10:33:29、10:34:16 仍持有 RidePlay 焦点，按键目标仍 null。

返回后只读核查的保存设置：

```text
rideplay_control_only_audio_focus = true
rideplay_control_only_media_session = true
disable_media_key_audio_focus = absent / default false
```

**不能把第二次连接写成已验证“关闭实验”的对照**：实际证据显示两次都持有焦点。无需为了证明焦点开启无效再重复实车，开启状态本身已经捕获到失败。

## 已实施的代码及验证

11 月 3 日新增默认关闭的 Advanced 设置“无音频控制申请焦点（实验性）”：

- `AirPlayPersistence.kt`：key `rideplay_control_only_audio_focus`，默认 false。
- `CarPlayMediaKeys.kt`：controller attach 时读取；条件为 `(controlOnly && !controlOnlyFocus) || loadDisableMediaKeyAudioFocus(context)`，全局禁用优先。
- 原 controlOnly 仍要求本次 audioOutputDisabled 与无音频控制设置同时开启。
- 控制专用模式获焦点后只更新自身状态，不将焦点事件转发到本地 AndroidMediaSink，避免改变本地音频处理。
- 焦点请求失败保留媒体会话；实际模式、焦点结果和按键转发尝试次数进入应用内诊断。
- 设置保存 `markReconnectNeeded()`，不主动断开，重连后生效。
- 11 种语言、紧凑/宽屏/高密度中文/阿拉伯语大字体截图已补并目视检查。
- 没有静音 AudioTrack，没有强制蓝牙路由，没有伪造 PLAYING，没有加入新权限。

相关测试：`ControlOnlyMediaSessionTest`、`CarPlayMediaFocusForwardingTest`、`ControlOnlyMediaSettingsScreenshotsTest` 等。
11 月 3 日完整 AGENTS.md CI 检查成功（11m33s）；common 1779 项零失败，控制专用会话 24 项、焦点转发 33 项通过；standalone 与追加截图测试成功（22s）。日志：`/tmp/rideplay-focus-ci.log`、`/tmp/rideplay-focus-standalone.log`。

安装包：`com.chiccccken.rideplay.debug`，versionCode 41，versionName `0.2.22-hud-test`。
已 `adb install -r` 安装，签名与原包一致，认证资产逐字节匹配，未卸载、原设置保留。
实验包路径：`/private/tmp/diplay-build-20261102/mobile/outputs/apk/debug/mobile-debug.apk`。
SHA256：`d67f6e6964387c0a45712e11fb3cb3b4ca613be8d9b4cc79f055f88974331d3f`。
旧包备份：`/tmp/rideplay-before-focus.apk`。认证材料不得公开。

**本地检查和安装通过不代表 KTM 修复通过：本轮实车已证实焦点方案没有解决。**

## Astra 应重点调查什么

1. 系统按键目标选择是否仅从实际本地音频播放 UID 历史中寻找，以及 ROM 是否有额外策略。AOSP 参考 `MediaSessionStack.updateMediaButtonSessionIfNeeded()` 遍历 AudioPlayerStateMonitor 的播放 UID；onPlaybackStateChanged 并不等价于真实音频活动。与当前现场吻合，但目标 ROM 框架尚未提取比对。
2. 普通应用有无适用于此 ROM、无需本地音频的目标选择入口；明确公开 API、系统权限与 shell/Root 的边界。不要把媒体卡片直接控制与系统硬件按键分发混同。
3. Bluetooth 发现了 RidePlay controller，但没有选中且缓存停在初始数据：调查未选中时是否不注册更新监听、初始化时序、旧包装对象或 ROM 缓存条件。需区分选路主因与缓存次生表现，不盲目反复 setMetadata。
4. 如需最小本地音频实验，先解释对 C50 声音、Android 音频、功耗、后台、暂停、焦点竞争的影响，给出默认关闭与回退；不能当作普通修复直接加入。
5. 不要仅凭现象断言需要 Root，也不要保证普通应用一定能强制成为蓝牙播放器。先研究再选择有明确验收点的实验。

已核查的上游参考：

- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/services/core/java/com/android/server/media/MediaSessionStack.java
- https://developer.android.com/media/legacy/media-buttons
- https://developer.android.com/media/optimize/audio-focus （API 35+ 前台/前台服务限制；本次已实际持有焦点，不能再将该限制当作已证实故障原因）
- 11 月 2 日文档中的 MediaPlayerList / AvrcpTargetService 参考。

以上上游代码只能帮助解释，不等于目标移植 ROM 的确切实现。

## 后续现场采集

adb：`/Users/taopengcong/Library/Android/sdk/platform-tools/adb`，serial `bf7b20d`。
手机脚本：`/data/local/tmp/rideplay-ktm-capture.sh`；电脑 `/tmp/rideplay-ktm-capture.sh`。
完整脚本见 11 月 3 日文档，真实拔 USB 和实车回收均通过。启动前确认用户准备出发，使用新目录：

```sh
adb -s bf7b20d shell 'setsid sh /data/local/tmp/rideplay-ktm-capture.sh /sdcard/Download/RidePlay-KTM-UNIQUE 120 15 </dev/null >/dev/null 2>&1 & echo started'
```

每 15 秒间隔、120 次，通常约半小时，dumpsys 变慢时更久；不是严格墙钟截止。本轮有几组间隔增大。每组保存 media_session/audio/bluetooth_manager 与筛选 logcat。
回来创建输出目录 STOP、确认 finished，再 pull。旧目录已有 STOP，不能复用。
logcat 取最近 400 条后筛选，并非无损全量；本轮 Bluetooth dump 自带历史事件补足了按键证据。不要因 keys 文件为空就认定无按键。

## 工作区和操作边界

工作区有大量此前未提交的 rebrand、方向和连接等修改；先看 git status/diff，不 reset、不 clean、不覆盖。此次只新增本文，不改功能、不改手机设置、不安装 APK。
源码检查按根 AGENTS.md 的完整命令；隔离构建说明见 `LOCAL_BUILD_TROUBLESHOOTING.md`。
已可用的本机配置：

```sh
JAVA_HOME=/private/tmp/diplay-gradle/jdks/eclipse_adoptium-25-aarch64-os_x.2/jdk-25.0.3+9/Contents/Home
GRADLE_USER_HOME=/private/tmp/diplay-gradle
ANDROID_HOME=/Users/taopengcong/Library/Android/sdk
```

Gradle 参数：`-Pdiplay.buildRoot=/private/tmp/diplay-build-20261102 --project-cache-dir /private/tmp/diplay-project-cache-20261102`。
沙箱内 Gradle 可能因锁套接字 Operation not permitted 失败，获准升级后完整检查可运行。lint 在源目录读取源码曾很慢，确认仍有进展再处理，不要清理输出碰运气。
真机包依 `BUILD.md` standalone 流程；source-only Debug 不能代替带认证资产的现场包。认证材料只能留本地，安装前比较签名，禁止通过卸载解决签名不符而丢设置。

本文档不代表故障已修复。用户希望 Astra 深入分析后给出有依据的下一步，不需要他先自行猜根因。

本次纯文档交付按 AGENTS.md 尝试了完整检查命令；wrapper 的默认用户缓存 `.zip.lck` 被沙箱拒绝（Operation not permitted），任务未开始，不能报告本轮检查通过。没有为纯文档重跑耗时的升级构建；11 月 3 日成功记录与本轮尝试须分开看。
