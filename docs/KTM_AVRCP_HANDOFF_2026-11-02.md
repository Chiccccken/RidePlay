# KTM 歌曲信息与车把控制故障交接

记录日期：2026-11-02（Asia/Shanghai）。面向接手排查的模型或开发者。

## 1. 当前结论与任务

**红米本机播放器的歌曲信息能正常显示在 KTM 仪表上；RidePlay 的 Android 媒体卡片与 CarPlay 正常，但 KTM 显示 `Not Provided` / `Unavailable`，车把切歌、暂停均无效。根因尚未确认，未实施修复。**

优先排查 Android 系统的媒体会话选路，以及 AVRCP 播放器选择/元数据同步。不要把“蓝牙没有选中 RidePlay”写成已经证实的结论，也不要只修改 Title / Artist 字段就视为解决。

用户明确说明：保持 KTM 蓝牙连接及 CarPlay 播放时，手机无法同时连接电脑。上一轮建议现场接 USB 读取日志不可行。具体是端口、连接拓扑还是现场环境限制，尚未确认；不要自行认定原因，也不要重复把同时接电脑当成前置条件。

用户本轮要求先编写交接文档，由其他模型继续分析。

## 2. 用户反馈：事实与未验证项

| 项目 | 已知情况 | 证据边界 |
| --- | --- | --- |
| 红米本机播放 | 用户确认播放正常、仪表显示正常 | 证明该组合能传递本机播放器的歌曲元数据；本机播放器的车把控制结果未明确反馈 |
| 仪表默认状态 | `Title` 和 `Artist` | 这是用户明确区分的默认占位文字 |
| RidePlay 场景 | 仪表变成 `Not Provided` 和 `Unavailable` | 与默认占位不同；不能直接断言是仪表一直未接收到任何更新 |
| 当前手机媒体卡片 | 用户确认正常 | 未取得本次现场的 MediaSession 原始快照 |
| CarPlay | 用户确认正常 | 未取得本次现场原始 Now Playing 数据 |
| KTM 车把 | 用户确认不能切歌、不能暂停 | 尚不知按键是否到达红米系统、到达其他会话，或到达 RidePlay 但未执行 |
| 电脑 ADB | 本轮 `adb devices -l` 无设备 | 未读取现场的 media_session、audio、bluetooth_manager 或按键日志 |

前一日项目记录 `RIDEPLAY_P1.md` 另记有：红米媒体卡片的上一首、下一首、暂停、播放可控制 iPhone；保持 iPhone 播放时，重连后卡片会同步。这是既往用户反馈，不是本轮重新验证，也不能替代 KTM 实车测试。

既往设备资料：2019 KTM 390 Duke、红米 Note 9 Pro（用户描述为移植版澎湃 OS 4）、iPhone 14、恒疆 C50。实际 Android API/ROM 构建、iOS 版本、仪表固件、现场配对角色和连接配置均待确认，不能从 ROM 营销名称推断。

## 3. 需求与必须保留的行为

目标结构：

```text
声音：iPhone ──蓝牙音乐──> C50

信息：iPhone ──CarPlay Now Playing──> 红米 RidePlay
      ──Android MediaSession──> Android 系统蓝牙 AVRCP ──> KTM

控制：KTM 车把 ──蓝牙 AVRCP──> Android 系统媒体按键选路
      ──RidePlay MediaSession.Callback──> CarPlay HID ──> iPhone
```

这是目标及源码架构，不表示所有箭头都已现场验证。RidePlay 没有实现一个直接连接 KTM 的私有协议，也没有自己创建 AVRCP 服务；应用把信息交给 Android MediaSession，蓝牙转发与播放器选择由系统负责。

- 不要求驾驶者每次手工切换 iPhone 音频输出；保持 iPhone → C50 的既有体验。
- 当前控制会话不播放本地声音、不创建静音 AudioTrack、不申请音频焦点。
- 不擅自加入静音播放、抢焦点、强制蓝牙连接/路由切换作为修复。这些会改变既定约束，必须先说明依据、影响和回退方式。
- KTM 功能应默认关闭、可关闭并重连回退。
- Android 媒体卡片、单测、安装成功分别记录；KTM 显示与车把控制通过前，不宣称适配完成。
- 不根据现象直接断言必须 Root，也不承诺普通应用一定能强制成为系统蓝牙播放器。

## 4. 当前代码逻辑（2026-11-02 工作区核查）

主要源目录：`common/src/main/java/com/shilapi/xcertplay/`；下述路径均相对仓库根目录。

### 4.1 会话创建和配置读取

1. `CarPlayHostActivity.kt` 创建连接配置时设置 `disableAudioOutput = carBluetoothAudio`，随后调用：

   ```kotlin
   CarPlayMediaKeys.attach(this, next, audioOutputDisabled = airPlayConfig.disableAudioOutput)
   ```

2. `CarPlayMediaKeys.attach()` 在 controller 改变时释放旧会话，读取拼音、专辑拼音、歌手别名设置，并计算：

   ```kotlin
   controlOnly = audioOutputDisabled && AirPlayPersistence.loadControlOnlyMediaSession(context)
   ```

   必须是本次实际连接禁用音频输出，且“无音频媒体控制（实验性）”开启，才进入控制专用模式。不能仅凭设置页面当前值认定活动连接的模式。

3. attach 注册 `playbackListener`、`nowPlayingListener`、`artworkListener`，并主动处理 controller 缓存的 Now Playing。
4. `onNowPlayingChanged()` 投递主线程，校验 controller 身份，从 `nowPlayingSnapshot()` 取最新数据。在 `controlOnly && session == null` 时调用 `start()`，不等待本地音频回调。
5. 非 controlOnly 模式仍主要由 `onMediaAudioChanged(true)` → `updateLocked()` 触发创建。

`AirPlayPersistence.kt` 中 `rideplay_control_only_media_session`、`rideplay_pinyin_metadata`、`rideplay_pinyin_album` 默认均为 false。设置保存调用 `markReconnectNeeded()`，不立即断开会话；这些值在新 controller attach 时读取。相关 UI 在 `DiPlayActivity.kt` 的 `controlOnlyMediaControls()` / `pinyinMetadataControls()`。

### 4.2 焦点与播放状态

`start()` 的焦点开关：

```kotlin
val focusDisabled = controlOnly || AirPlayPersistence.loadDisableMediaKeyAudioFocus(context)
```

控制专用模式不会创建/请求音频焦点，`focusRequest` 保持 null，因此后续 regainFocus 也不会请求焦点。会话创建逻辑为：

```kotlin
MediaSession(context, "DiPlay CarPlay").apply {
    setCallback(callback, mainHandler)
    setMetadata(publishedMetadata(nowPlaying))
    isActive = true
}
```

`publishPlaybackStateLocked()` 设置 PLAY、PAUSE、PLAY_PAUSE、NEXT、PREVIOUS actions。controlOnly 使用 iPhone 的 `nowPlaying.playing` 决定 PLAYING/PAUSED；普通模式在没有标题及进度时才回退到本地 `mediaAudioActive`。未知进度用 `PLAYBACK_POSITION_UNKNOWN`。没有为了提高选路优先级而强行伪造 PLAYING。

暂停保留会话；detach 或 controller 替换释放会话、监听和缓存。旧 controller 的排队回调有身份检查。

### 4.3 元数据：标准字段已经填写

`publishedMetadata()` → `androidMetadata()`：

| iPhone 数据 | Android 字段 |
| --- | --- |
| title | `METADATA_KEY_TITLE` 及 `METADATA_KEY_DISPLAY_TITLE` |
| artist | `METADATA_KEY_ARTIST` 及 `METADATA_KEY_DISPLAY_SUBTITLE` |
| album | `METADATA_KEY_ALBUM` |
| durationMillis | `METADATA_KEY_DURATION` |
| sourceApp | `METADATA_KEY_DISPLAY_DESCRIPTION` |
| artwork | `METADATA_KEY_ALBUM_ART` 及 `METADATA_KEY_DISPLAY_ICON` |

字段为 null 时不写入；本轮没有拿到现场值。没有把 `Not Provided` / `Unavailable` 写入这些字段作为应用占位的逻辑。

拼音开关开启时，复制 Now Playing 后转换 title/artist，歌手精确别名优先；album 还要求专辑拼音开关。原始 CarPlay 数据不改。`KtmMetadataTransformer.kt` 使用本地 ICU，API < 29、无汉字或含假名等情况保留原文，异常回退原值。

元数据变化或封面更新时发布 metadata；仅进度变化不重复发送整张封面，避免既有内存问题。每次 Now Playing 回调更新 playback state。

### 4.4 按键转发

`CarPlayMediaCallback` 支持：

- `onPlay()` / `onPause()` / `onSkipToNext()` / `onSkipToPrevious()`；
- `onMediaButtonEvent()` 解析 KeyEvent，只发送第一次 ACTION_DOWN，过滤重复按下和抬起。

随后 `CarPlayMediaKeys.send()` → `CarPlayController.sendMediaButton()` → executor → 活动 CarPlay session 的 `sendMedia()`。如果 CarPlayVideo 消费了按键，会走视频分支；目前没有日志证据表明现场走了该分支。

`sendMediaButton()` 返回 true 只表示接受排队，不证明 iPhone 执行。日志 tag 为 `DiPlay-MediaKeys`，会记录：

```text
media keys active focusDisabled=... focusGranted=...
media key <source> -> CarPlay <index> sent=...
media key <source> -> car video player <index>
media key <source> not used by CarPlay
```

这些事件也进入 `WheelKeyLog`。

## 5. 当前推断与证据缺口

**优先假设：RidePlay 会话存在，但没有成为系统媒体按键/蓝牙的目标播放器。** 理由是卡片正常而仪表元数据、车把控制同时失败，且这个会话没有本地音频。仍需现场证据，不能写成 ROM 已被证实不支持无音频会话。

其他分支也未排除：

- 蓝牙选中旧播放器或其他应用，或媒体按键目标与 AVRCP 信息来源不同；
- AVRCP 已关联 RidePlay，但元数据/播放状态同步条件、初始化时序或 ROM 行为造成空数据；
- 车把指令没有到红米，或到达后未进入 RidePlay callback；
- 实际安装包与当前工作区不同，或活动连接未应用新设置。

关于占位文字：AOSP 蓝牙代码确实使用 `Not Provided` 作为空元数据的默认标题。这与用户现象吻合，但只是上游实现证据；**未证明目标 ROM 具体使用同一代码路径**。`Unavailable` 的来源未确认，不要将两者都断言为 RidePlay 发出的值，也不要断言两个字段发生了倒置。

参考原始源码（后续需与目标 ROM 版本核对）：

- [AOSP 蓝牙空元数据处理与默认值](https://android.googlesource.com/platform/packages/apps/Bluetooth/+/4fc9b64c375ad825f0da10311ce0910ab985cec0%5E2..4fc9b64c375ad825f0da10311ce0910ab985cec0/)
- [AOSP MediaPlayerList](https://android.googlesource.com/platform/packages/modules/Bluetooth/+/619fafa41b7fe03ce7905422d966a59c68769f3d/android/app/src/com/android/bluetooth/audio_util/MediaPlayerList.java)：active session 列表变化与 active player 选择是不同动作；源码监听 media-key event session 变化来选播放器。
- [AOSP AvrcpTargetService](https://android.googlesource.com/platform/packages/modules/Bluetooth/+/619fafa41b7fe03ce7905422d966a59c68769f3d/android/app/src/com/android/bluetooth/avrcp/AvrcpTargetService.java)：从 MediaPlayerList 获取歌曲和播放状态。
- [Android 媒体按键选路文档](https://developer.android.com/media/legacy/media-buttons)。不要混同媒体卡片的直接 MediaController 操作与系统硬件按键选路。

## 6. 后续诊断：现场不能同时接电脑

先利用现有能力，避免立即增加有行为副作用的代码。

### A. 已存在的应用内诊断报告（首选入口，尚未现场尝试）

`DiPlayActivity.diagnosticReportText()` 已收集版本、Android API/构建、会话状态、蓝牙基础快照、WheelKeyLog 和会话日志。`DiagnosticExportStore.kt` 支持先保存到 Downloads/DiPlay，失败再回退应用存储及报告查看/分享。可在现场操作后保存，再离开现场传给电脑。

`WheelKeyLog` 是进程内最多 40 条事件的队列；相同连续事件会合并。应在按完车把键后及时导出，尽量不重启或杀应用，否则记录会丢。若回调完全没到应用，缺少按键事件只能证明未观察到，不能定位蓝牙/系统内部的具体断点。

现有 `BluetoothDiagnosticSnapshot` **只覆盖公开 API 可见的适配器、权限、开关及 API 级别，不包含系统 AVRCP active player**。报告头的媒体焦点设置也不能代替实际 controlOnly 焦点状态。不要承诺现有报告足以确诊。

### B. 若报告证据不足，再设计不依赖现场 USB 的采集

候选方案需先核查设备能力及对现有连接的影响：

1. 在电脑旁预先配置 shell 权限的延迟采集任务，现场定时将 `dumpsys media_session`、`dumpsys audio`、`dumpsys bluetooth_manager` 和必要的有界 logcat 写到手机本地，之后再回收。先验证 USB 断开后任务存活、权限、存储与自动停止；目前没有现成已验证脚本。
2. 设备自带的错误报告或开发者诊断，在现场保存后带回分析；内容和可用权限取决于 ROM。
3. 无线 ADB 仅是候选；CarPlay 可能占用 Wi-Fi/热点资源，用户现场网络拓扑未知。不能未经核查就要求改 Wi-Fi 或保证能并行工作。
4. 必要时给 RidePlay 增加默认关闭、受限容量的诊断快照，记录实际 controlOnly、会话 active/state、metadata 字段是否存在、callback 计数及发送结果。普通 App 无权读取完整系统服务状态；`Runtime.exec("dumpsys ...")` 不会自动获得 adb shell 权限。

采集目标是对照本机播放器正常场景与 RidePlay 失败场景，定位：当前安装版本、真实连接模式、媒体按键目标、蓝牙 active/addressed player、元数据、真实本地音频活动，以及车把按键是否进入 callback。原始记录仅本地保存，分享前脱敏 MAC、设备名和身份/配对材料。

若无法获得系统证据，应明确写出仍不确定的环节，再评估最小实验。不要用伪造歌曲、重复 setMetadata、伪造 PLAYING 或加静音播放掩盖尚未确认的选路问题。

## 7. 工作区、构建与既往验证

- 工作目录：`/Users/taopengcong/Documents/ChatGPT/DiPlay`。
- 当前分支：`custom`；HEAD：`ab87c676e7d46af771686bdc6641aaf0d23a83ba`。
- **工作区有大量未提交修改**，包括应用名称/图标、构建、连接/方向及多语言等。HEAD 不能代表全部当前源码，更不能证明手机安装包版本。接手先看 `git status --short` 和相关 diff，不要 reset/覆盖。
- 当前 Debug application ID 为 `com.chiccccken.rideplay.debug`，正式为 `com.chiccccken.rideplay`；历史安装/签名信息需现场核对。
- 相关测试：`ControlOnlyMediaSessionTest`、`CarPlayMediaCallbackTest`、`CarPlayMediaFocusDisabledTest`、`CarPlayMediaFocusForwardingTest`、`KtmMetadataTransformerTest`、`ArtistNameOverridesTest`；UI 截图测试为 `ControlOnlyMediaSettingsScreenshotsTest`、`PinyinMetadataSettingsScreenshotsTest`。
- `ControlOnlyMediaSessionTest` 用 Robolectric/Shadow 记录 metadata/state，验证应用逻辑、无焦点和 callback 转发；它没有验证实际 Android 蓝牙栈选路。
- `RIDEPLAY_P1.md` 记录过历史 Linux CI 成功和手机媒体卡片控制成功；本轮未查询最新云端 CI，也未重跑成功，不能视作当前工作区或 KTM 已验证。
- 本轮仅新增本文档，未修改运行逻辑、未安装 APK、未更改配对或路由。

已按根目录 AGENTS.md 尝试完整检查命令：

```sh
./gradlew :shared:testDebugUnitTest :common:testDebugUnitTest :home:testDebugUnitTest \
  :mobile:lintDebug :home:lintDebug :maphost:lintDebug \
  :mobile:assembleDebug :home:assembleDebug :maphost:assembleDebug
```

本轮结果：Gradle wrapper 在创建 `/Users/taopengcong/.gradle/wrapper/dists/gradle-9.5.0-bin/.../gradle-9.5.0-bin.zip.lck` 时被文件系统沙箱拒绝（`Operation not permitted`），构建任务未开始；不是应用编译或测试断言失败，亦不能报告完整检查通过。

后续代码修改必须运行上述完整检查。SDK 本机路径可检查 `/Users/taopengcong/Library/Android/sdk`，根目录当前无 `local.properties`。已有临时 Gradle 缓存及构建隔离说明见 `LOCAL_BUILD_TROUBLESHOOTING.md`；不要为本轮文档任务擅自清理已有输出。

实际连接测试包按 `BUILD.md` 的 standalone 流程处理；source-only Debug 不含连接 iPhone 所需认证资产。不要把认证材料或私钥写入交接文档/公开仓库。

## 8. 接手建议

先阅读本文与 `CarPlayMediaKeys.kt`、`ControlOnlyMediaSessionTest.kt`，解释普通应用 MediaSession 与目标 ROM 的 AVRCP/媒体按键选路之间的边界，再制定可关闭、可回退的最小诊断或修复计划。优先拿到本地保存的现场证据。无需重复要求用户确认“媒体卡片正常”“本机显示正常”“车把无效”，这些已明确反馈。

背景文档：`KTM_CUSTOM_PLAN.md`、`RIDEPLAY_P1.md`、`BUILD.md`。背景文档包含早期计划和历史验证状态，以本文的最新用户反馈及当前源码为本问题的起点。
