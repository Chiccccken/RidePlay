# RidePlay · DiPlay for KTM：实施计划与验收边界

RidePlay · A personalized CarPlay experience for motorcycles.

RidePlay 是基于 [DiPlay](https://github.com/shihabal3amri/DiPlay) 的个人定制项目，首个适配目标为 KTM MY RIDE；项目名称不限定摩托车品牌，后续可扩展到其他车型。完整标题中的 KTM 表示当前适配重点，不代表已完成兼容验证。

名称约定：Codex 项目名称为 `RidePlay`；当前完整标题为 `RidePlay · DiPlay for KTM`；GitHub 仓库保持 `Chiccccken/DiPlay`，开发分支保持 `custom`。Android 应用显示名称暂不修改，后续再决定。命名调整不改变 P1 优先级。

核查日期：2026-11-01（Asia/Shanghai）。状态：P1 实验实现已通过完整 Linux CI，用户反馈无音频连接及 Android 媒体卡片正常；P1–P3 均未在 KTM 实测。最新验证见 [RIDEPLAY_P1.md](RIDEPLAY_P1.md)。

## 1. 仓库与本轮范围

- 用户 Fork：`Chiccccken/DiPlay`；官方上游：`shihabal3amri/DiPlay`。
- 核查时两者 `main` 都是 `615b32ce0b40cd0b99ce94ba23a512001553684f`，领先/落后均为 0。
- 已从该提交建立远端和本地 `custom`，用于长期个人定制；默认分支暂保留 `main`。
- `main`、`custom` 在核查时均没有分支保护；账号拥有 admin/push 权限。
- 已阅读根目录 `AGENTS.md`；仓库内未发现更深层的同名说明文件。
- 本轮只增加实施与同步设计文档，不修改 CarPlay、蓝牙、电话或构建配置。
- 用户上传的交接文档用作需求及待核实线索；其中的代码位置和既往测试结论不直接当作当前验证结果。

用户报告的设备组合：2019 KTM 390 Duke、红米 Note 9 Pro（移植版澎湃 OS 4）、iPhone 14、恒疆 C50。实际 Android API、ROM 构建号、iOS 版本、仪表固件及蓝牙协商结果仍需设备确认，不能由 ROM 名称推断。

本轮 ADB 列表未发现目标红米；没有向其他已连接设备安装应用或进行媒体/电话测试。

## 2. 必须保持的行为

```text
iPhone ──蓝牙音乐──> C50
   ⇅ CarPlay 画面、Now Playing、HID 控制
红米上的 DiPlay ──Android MediaSession / 系统蓝牙 AVRCP──> KTM
```

- 保持当前“通过车辆蓝牙播放 CarPlay 音频”打开后的音频直连行为；不要求每次手工选输出。
- KTM 功能默认关闭；关闭后沿用上游行为。不开静音 AudioTrack，不向红米转发音乐，不默认申请音频焦点。
- 不改变 CarPlay 音频能力声明，不引入自动蓝牙连接/断开或强制路由切换。
- 代码合入、单元测试成功、系统媒体卡片出现、KTM 实测通过分别记录；只有最后一项可作为功能验收。
- 停车后测试；日志不公开 MAC、配对材料、姓名或电话。

## 3. 当前源码核查

| 位置 | 已确认行为 | 对设计的影响 |
| --- | --- | --- |
| `common/.../CarPlayMediaKeys.kt` 的 `attach`、`onNowPlayingChanged` | 绑定 controller 并接收、缓存 Now Playing；会给已存在的 MediaSession 更新元数据和播放状态 | 可复用元数据链路，但 attach 本身不会创建会话 |
| 同文件 `updateLocked(active)` | 仅在 `active && session == null` 时调用 `start` | 无 Android 音频回调时没有 MediaSession，与用户现象一致；尚非设备实测根因证明 |
| 同文件 `start` | 默认申请媒体音频焦点；现有“禁用媒体按键音频焦点”设置可以跳过 | 不能直接在 attach 无条件调用旧 start；KTM 控制会话必须独立禁止焦点申请 |
| 同文件 `CarPlayMediaCallback` | 已有 PLAY、PAUSE、NEXT、PREVIOUS 和媒体 KeyEvent 映射 | 可复用并保留按下/抬起、重复事件过滤 |
| `shared/.../orchestration/CarPlayController.kt` 的 `sendMediaButton` | 向活动 CarPlay session 发送 HID；返回值表示接受排队 | 返回 true 不能证明 iPhone 已执行，更不能证明仪表按键可达 |
| `shared/.../airplay/AirPlayInfoPlist.kt` | `disableAudioOutput` 去掉音频能力与 formats/latencies；仍声明 media HID | 无音频模式保留控制路径；不需要为按键重新打开音频能力 |
| `common/.../CarPlayHostActivity.kt` | 配置时传入 `disableAudioOutput`；生命周期调用 MediaKeys attach/detach | 应将实际连接使用的音频模式传入媒体会话策略，避免只读已改变但尚未重连的设置 |
| `shared/.../hud/BydCarPlayCall.kt` | `CarPlayCallState` 解析 0x4155；现有 BYD 输出另有厂商桥接 | 可复用纯状态解析，不能将 BYD Binder/ADB 当成 KTM 接口 |
| `shared/.../telecom/TelecomPhoneAudioBackend.kt` | 自管理 SIP PhoneAccount，`placeCall` 创建活动 VoIP connection | 是音频用途的呼叫，不是只读来电显示桥；`PhoneAudioConnectionService` 类也在此文件内 |

`...` 的完整源目录为 `src/main/java/com/shilapi/xcertplay/`。

### 主要可行性边界

Android 的 active MediaSession 可以发布元数据并接收控制，但媒体按键的系统选路不只看 `isActive`。Android 官方文档说明，Android 8+ 会寻找最后实际在本地播放音频的媒体会话。因此“无音频、无焦点会话”能否成为红米蓝牙栈选中的 AVRCP 会话，是 P1 的首个真机验证门槛。只设置 `STATE_PLAYING` 不能视为已解决系统选路。

参考：[MediaSession API](https://developer.android.com/reference/android/media/session/MediaSession)、[媒体按键选路](https://developer.android.com/media/legacy/media-buttons)。

即使 Android MediaController 能控制 DiPlay，也不保证 KTM 在当前 A2DP/AVRCP 配对状态下会订阅该会话。若 ROM 必须有真实本地音频才能选中会话，应记录“当前组合不支持既定约束”，再单独评估系统级能力；不自动加入静音播放、抢焦点或改音频路由的变通方案。

## 4. 分阶段工作与门槛

### P0：构建与维护基线

1. 补齐与 CI 相同的 JDK 25、SDK `platforms;android-37.0`、Build Tools `36.0.0`、NDK `28.2.13676358`；使用仓库 Gradle wrapper 9.5.0，不通过降级源码来迁就本机环境。
2. 在未改功能的基线上执行下方完整命令，保存测试/Lint 报告，生成 source-only Debug APK。
3. 自动同步按 [KTM_UPSTREAM_SYNC.md](KTM_UPSTREAM_SYNC.md) 实施，先手动演练无更新、有更新、冲突与权限失败，再开启定时。
4. 建立真机测试包的本地认证材料与固定签名方案；公开 CI 不携带认证材料或发布可用身份。

构建要求：

```sh
./gradlew :shared:testDebugUnitTest :common:testDebugUnitTest :home:testDebugUnitTest \
  :mobile:lintDebug :home:lintDebug :maphost:lintDebug \
  :mobile:assembleDebug :home:assembleDebug :maphost:assembleDebug
```

初始本机只有已注册的 JDK 17/26，SDK 平台最高 34，Build Tools 最高 34.0.0。第一次受限环境运行在 Gradle 下载阶段因 DNS 失败；允许联网重试后，仓库 `gradle/gradle-daemon-jvm.properties` 的 Java 25 要求触发自动下载，并由 Gradle 补齐 SDK 37、Build Tools 36 和 NDK 28.2.13676358。Gradle 缓存使用本机临时目录，不提交工具链产物。`python3 scripts/check_public_tree.py` 已通过。没有完成基线构建前，不开始 P1 功能修改。

本轮基线执行结果：

| 检查 | 结果 |
| --- | --- |
| `shared` 单元测试 | 1,798 项，零失败、零错误，跳过 1 项 |
| `home` 单元测试 | 11 项，零失败、零错误 |
| `mobile` / `home` / `maphost` Lint | 全部通过 |
| 三个应用 `assembleDebug` | 全部生成 APK；ZIP 检查不含 `assets/offline-mfi/` |
| `common` 单元测试 | 未完成；耗时集中在 `SettingsBackupServerTest.stopDoesNotFailWhileConnectionsAreEnding` 的 1,000 次连接关闭循环 |
| 完整 Gradle 命令 | 未通过：运行 13 分 37 秒后主动停止耗时的 common 测试 worker，退出 143，Gradle 返回失败；不是功能断言失败的结论 |

两次 Java 25 线程快照显示该压力测试仍创建新线程，不能将其描述为已证实死锁。未修改/跳过测试，也未宣称完整基线通过。下一步先在与官方相同的 Linux CI 环境完成全套测试，或继续诊断 macOS 下 socket close 的耗时；不为绕过此问题改应用行为。报告与 APK 留在本地 build 目录，未提交公开仓库。

### P1：独立媒体控制会话（首个实现目标）

拟新增默认关闭的“无音频媒体控制（实验性）”开关，位于 Advanced 的现有音频实验卡片；仅在实际 CarPlay 连接使用 `disableAudioOutput=true` 时生效。保存后提示下次连接应用并调用 `markReconnectNeeded()`，不自动断开当前连接。新增文案覆盖全部 locale。

优先复用现有单个 MediaSession，不再创建竞争会话。引入小型会话策略，分开决定“会话是否存在”与“是否申请音频焦点”：

| 连接情况 | 会话策略 | 焦点策略 |
| --- | --- | --- |
| KTM 开关关闭 | 完整保留上游音频回调触发策略 | 保留上游设置 |
| KTM 开关开启，但实际连接仍接收音频 | 保留上游策略 | 保留上游设置 |
| KTM 开关开启，实际连接不接收音频 | 活动 CarPlay controller attach 后建立，暂停时保留，detach 时释放 | 始终不申请，也不因恢复播放/音频回调重获焦点 |
| 无活动 controller | 无会话，不发送 HID | 无焦点 |

实现细节：

- 创建时立即发布缓存的元数据与真实播放状态，不将未知状态伪装为播放中。
- 延续现有 artwork 更新节流与 position timestamp，切歌、暂停、元数据晚到时均更新。
- 为排队的音频/元数据回调加 controller 或 generation 校验，防止旧连接回调污染重连后的会话。
- 销毁/替换 controller 时释放 session、封面和关联监听；后台/锁屏沿用现有 CarPlay 会话持有机制，额外服务或保活只有出现证据时再评估。
- 键事件继续使用原 `CarPlayMediaCallback → sendMediaButton`，不绕过现有视频播放按键策略。
- 不请求联系人权限，不修改 Telecom，不为“看起来在播放”添加任何音源。

自动验证重点：无音频回调也创建会话；默认关闭行为不变；两种模式切换；反复暂停/播放；NEXT/PREV/PLAY/PAUSE 与重复事件；旧回调晚到；detach/重连；始终无焦点请求；元数据、播放状态、封面生命周期；既有焦点和视频按键测试回归。沿用 Robolectric/Mockito 测试基础设施。

真机门槛按顺序执行，每次只增加一个变量：

| 次序 | 停车测试 | 必须记录 |
| --- | --- | --- |
| 1 | 红米本机播放器 ↔ KTM | 仪表 Phone/Headset 配对状态、媒体/电话开关、歌曲/歌手显示、按键可用性 |
| 2 | 原版 DiPlay 音频模式开/关 | 相同曲目下的 MediaSession、KTM 行为、iPhone 实际输出 |
| 3 | 定制包关闭新开关 | 与原版相同的控制、音频和 CarPlay 画面行为 |
| 4 | 定制包开启新开关、无音频连接 | 会话、系统 MediaController 控制、KTM 信息与按键，逐层定位 |
| 5 | 暂停、切歌、后台、锁屏、断连重连、开机后重新连接 | iPhone→C50 音频持续正确、红米不发声/不接收音乐、无重复按键或旧元数据 |

设备诊断可用 `adb shell dumpsys media_session`、`adb shell dumpsys audio`、`adb shell dumpsys bluetooth_manager`；原始输出可能含隐私，只本地保存并脱敏。进程被杀后或重启后不承诺在没有 CarPlay 连接的情况下继续控制。

验收：物理 KTM 显示曲名/歌手并完成可用的原厂切歌/播放暂停操作；iPhone 始终自动直连 C50；默认关闭恢复原行为。任何一项未验证，P1 保持实验状态。

### P2：歌曲与歌手拼音

P1 真机成立后，新增纯本地 `KtmMetadataTransformer`，在发布 Android MediaSession 的边界转换 title/artist，不改 CarPlay 原始模型与画面。默认关闭，不带声调，保留英数和符号；先比较 Android ICU 与离线词典方案的可用 API、词组/多音字、人名纠正规则、依赖体积与许可证，再确定实现。

标准 MediaSession 是共享元数据，开启后红米媒体卡片可能也显示拼音；不承诺普通 App 能按蓝牙设备分别提供中文/拼音。测试中英混合、空值、多音字、人名、长文本、Unicode 与 KTM 截断。总时长与播放进度分别验收，不承诺仪表进度条。

### P3：来电状态与联系人拼音（独立可行性试验）

先记录和测试 iAP2 状态到只读内部模型，包含呼叫等待、结束、断连清理；不调用 `placeCall` 来模拟“只读通知”。姓名来自 iAP2 显示名，缺失回退号码；不额外读取或上传通讯录。联系人拼音复用 P2 转换器，原始姓名供 CarPlay 保留。

Android `ConnectionService` 官方说明支持自管理通话的蓝牙集成，但这同时涉及系统通话与控制能力，**不等于提供只显示、不影响音频的 HFP 通知接口**。参考：[ConnectionService](https://developer.android.com/reference/android/telecom/ConnectionService)。须在目标 ROM/API 与 KTM 上验证来电可见性、允许的电话账户能力、路由与权限；目前尚不能断言需要 Root，也不能承诺无 Root 可行。

先确定有无满足“不抢 iPhone→C50 通话音频、不误接听/挂断”的实现路径，再做默认关闭的实验开关。如果平台无法分离显示与通话副作用，停留在诊断模型，报告限制；不以 Android 本地通知冒充仪表来电支持。

## 5. 安装、签名与回退

RidePlay 的 `mobile` 正式包名为 `com.chiccccken.rideplay`，Debug 包名为 `com.chiccccken.rideplay.debug`，可以与原版正式包 `com.shihab.diplay` 共存；配置和配对不会自动共享。测试时只运行一个接收端，避免两个服务竞争。不要卸载原版来绕过签名问题。

`assembleDebug` 是不含认证材料的源码构建，能安装不代表能独立连接 iPhone。真正的连接测试遵守 [BUILD.md](BUILD.md) 的 `DIPLAY_AUTH_ASSETS_DIR` 与 `:mobile:assembleStandaloneDebug` 流程，认证目录和签名密钥留在 Git 外，不自动抓取或公开打包。现有正式 APK 的签名不能假设由本项目掌握。

每个功能单独提交；开关关闭并重连为首选运行时回退，Git 使用 revert 撤销自定义提交，保留官方 APK 与已验证测试包。上游同步不能覆盖 `custom`，测试构建不发布成官方更新。
