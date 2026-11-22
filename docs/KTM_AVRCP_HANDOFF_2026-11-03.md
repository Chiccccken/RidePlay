# KTM AVRCP 故障交接：实车系统证据与下一步实验

日期：2026-11-03（Asia/Shanghai）。接续 `KTM_AVRCP_HANDOFF_2026-11-02.md`，以本文的新现场证据为准。

## 1. 当前结论

2026-11-03 做了红米本机播放器与 RidePlay CarPlay 的停车对照测试，并通过预先启动的 adb shell 后台任务采集了拔掉 USB 期间的系统快照。

**本次已证实的断点：CarPlay 播放期间，RidePlay 有 active、PLAYING 且进度持续更新的 MediaSession，但系统媒体按键目标为 null，蓝牙内部 active player 也为 null。KTM 车把指令到达红米蓝牙服务，却没有选路到 RidePlay。**

这比昨天的“可能没有选中 RidePlay”更明确。但尚未证实其底层原因必然是没有申请焦点或没有本地音频。焦点申请能否改善选路尚未实验。

用户确认：

- 红米本机播放器经 KTM 车把控制正常；CarPlay 场景控制不正常。
- 中文乱码发生在 KTM 仪表，属于用户已知的仪表中文显示限制，不是红米本机乱码，不作为本次新增故障。
- 两种场景都在红米输出选择界面选择 KTM。不要再次建议“先选择 KTM”。输出设备与媒体按键目标播放器是不同概念。
- 昨天两份报告导出前都按过切歌，且可能关过一次仪表。昨天 15:06 的连接列表变空可能由此解释，不应作为独立已确认故障。

## 2. 实车采集及证据位置

电脑本地原始记录（仅本地，未纳入仓库，可能包含蓝牙身份与歌曲信息）：

`/tmp/rideplay-ktm-field-review/RidePlay-KTM-field-20261103-01/`

手机原始记录：

`/sdcard/Download/RidePlay-KTM-field-20261103-01/`

临时目录可能被清理，接手时先确认存在。不要将整份 Bluetooth dump 提交公开仓库。

采集从 10:45:58 开始，已取回 27 组（最后索引 snapshot 26，10:52:50）的快照；回来后已创建手机目录中的 `STOP` 文件，请勿直接复用该输出目录重启采集。命令没有改蓝牙路由、配对、音频设置或 APK。没有在此次新增实验开关，也没有安装新 APK。

### 本机播放器正常场景

- `20261103-104803-media_session.txt`：`Media button session is com.netease.cloudmusic/MediaSession/74`，active=true，PAUSED；metadata 中中文正常。
- `20261103-104906-media_session.txt`：同一个网易云会话为 PLAYING，歌曲已变化。
- `20261103-104906-keys.txt`：10:49:06.747 蓝牙 `KEYCODE_MEDIA_NEXT` 的目标与 internal active player 均为 `com.netease.cloudmusic`；随后 MediaSessionService 把 DOWN/UP 送到对应会话。
- `20261103-104922-bluetooth.txt`：网易云条目有 `<Active>`，蓝牙缓存中有真实歌曲名、歌手、专辑及 PLAYING 状态。

### CarPlay 失败场景

优先读取 `20261103-105010-media_session.txt`、`20261103-105010-bluetooth.txt`、`20261103-105010-keys.txt`，再对照 10:50:26、10:50:42 的快照。

系统媒体会话：

```text
Media button session is null
Sessions Stack - have 1 sessions:
  package=com.chiccccken.rideplay.debug
  active=true
  state=PlaybackState {state=PLAYING(3), position=11839, ...}
  metadata: size=9, description=Zuo Ci: Zhang Le Raku, ...
```

10:50:26 时进度更新为 28066，仍 PLAYING；10:50:42 为 PAUSED。不能把快照里的“作词／混音”直接判断成字段解析错误，可能是 iPhone 音乐应用的动态歌曲展示，尚未专门核查。

蓝牙服务 10:50:10 快照（`AvrcpTargetService` 部分约 843 行开始）：

```text
List of MediaControllers: size=1
  Media Player 2: com.chiccccken.rideplay.debug
  MediaController (...) Zuo Ci: Zhang Le Raku, ...
  Current Data:
    Song: { ... title="Not Provided" artist="" album="" ... duration=0 ... }
    PlayState: PlaybackState {state=PAUSED(2), position=-1, ... updated=0 ...}
```

RidePlay 条目没有 `<Active>`。**蓝牙包装对象能读到当前 controller 的描述，但其 Current Data 缓存仍是初始空数据**；系统会话已经更新并不等于蓝牙缓存已更新。缓存未更新与未选为活动播放器的因果关系仍需进一步核查，不能认定只改 metadata 就可解决。

该 Bluetooth dump 保存了连续车把事件，例如：

```text
10:50:05 KEYCODE_MEDIA_NEXT ... to null ... internal active player is null
10:50:09 KEYCODE_MEDIA_PREVIOUS ... to null ... internal active player is null
```

`20261103-105010-keys.txt` 还记录：

```text
10:50:10.595 AvrcpTargetService: ... KEYCODE_MEDIA_PREVIOUS ... to null ... internal active player is null
10:50:10.599 MediaSessionService: ... targetPackage:com.netease.cloudmusic reason:PendingIntentHolder
```

因此车把事件已到红米蓝牙，系统尝试回退到历史网易云接收器。**不要把 null 误写成选中了网易云的活动会话，也不要继续说“不知道车把是否到手机”。**

## 3. 设备与当前安装

2026-11-03 USB 实查：

- adb serial：`bf7b20d`；红米 M2007J17C / gauguinpro。
- 系统自报 Android API 37。前日报告 firmware：`Redmi/gauguinpro/gauguinpro:17/CP2A.260605.016/OS4.0.5.0.XPCCNXM:user/release-keys`。这是 ROM 自报，不代表官方机型升级支持状态。
- 安装包：`com.chiccccken.rideplay.debug`，versionCode=41，versionName=`0.2.22-hud-test`。版本名不能唯一标识源码或 APK 哈希。
- 对照记录来自本机网易云 Android 播放与 RidePlay 转发 iPhone 音乐。
- 昨日报告中实际 media keys 会话 `focusDisabled=true focusGranted=false`。报告头 `Media-key audio focus: enabled` 只是保存设置，不代表当前会话申请／拿到焦点。

## 4. 现有设置为什么不能完成下一步实验

核查源文件：

- `common/src/main/java/com/shilapi/xcertplay/CarPlayMediaKeys.kt`
- `common/src/main/java/com/shilapi/xcertplay/AirPlayPersistence.kt`
- `common/src/main/java/com/shilapi/xcertplay/DiPlayActivity.kt`
- `shared/src/main/java/com/shilapi/xcertplay/media/AndroidMediaSink.kt`

控制专用模式：

```kotlin
controlOnly = audioOutputDisabled && AirPlayPersistence.loadControlOnlyMediaSession(context)
```

`start()` 中：

```kotlin
val focusDisabled = controlOnly || AirPlayPersistence.loadDisableMediaKeyAudioFocus(context)
```

因此“禁用媒体按键音频焦点”即便关闭，controlOnly 也强制禁用焦点。

- “高级音频通道映射”：本地音频输出映射，不是 AVRCP 播放器选择。
- “音频焦点”：AndroidMediaSink 本地音频侧设置，不会覆盖上述 controlOnly 条件。
- 不建议关闭“无音频媒体控制”来实验：本地音频禁用时，会话可能根本不创建。

不要把申请焦点、真实本地音频活动、MediaSession active/PLAYING、蓝牙 active player 混为一谈。申请焦点也不保证系统选择此会话。

## 5. 下一步方案：尚未实施

上一轮向用户提出的方案：新增默认关闭的“无音频控制申请焦点（实验性）”，下次连接生效，使控制专用会话可选择尝试媒体按键焦点。用户随后要求交接，打算到新对话写代码。

接手先检查技术约束和最小实施计划，再实施：

1. 新设置只影响 controlOnly，默认关闭保持既有逻辑，放在 Advanced 的现有相关卡片中；全部 locale 翻译，保存后 `markReconnectNeeded()`，不要擅自断开会话。
2. 明确定义与“禁用媒体按键音频焦点”的优先级，避免两开关互相矛盾；建议全局禁用仍优先。说明开启实验所需组合。
3. 控制专用模式申请焦点时，审查 `forwardGrantedFocusLocked()`、`regainFocusLocked()`、焦点丢失和 detach/release 行为，不能直接放开布尔条件就认为安全。核查目标 API 37 的焦点请求资格及当前前台/服务状态；请求失败须可见且不崩溃。
4. 日志/诊断至少包含实际 controlOnly、实验是否启用、焦点请求及返回值、媒体会话 active/state、歌曲字段存在性、回调计数或转发记录。普通应用无法靠 Runtime.exec 获取 adb shell 的 dumpsys 权限。
5. 不加入静音 AudioTrack、假 PLAYING、重复刷 metadata、强制播放器或蓝牙路由。若焦点实验无效，再基于证据讨论其他方案。
6. 测试默认关闭、实验开启、全局禁用优先、请求失败、重连生效及关闭回退、释放焦点、普通模式不回归。布局变更按 AGENTS.md 提供紧凑/完整大字体截图，触及行对齐再加阿拉伯语。

影响与边界已经向用户说明：申请焦点可能暂停或压低红米其他应用音乐；不主动切换 iPhone 输出，但仍必须验证 iPhone → C50 实际连续播放。用户不接受每次手工切 iPhone 输出作为验收。

**需要再去车旁测试。** 电脑旁先验证构建、设置持久化、会话与焦点行为，再安装可实际连接的实验 APK，最后停车对照开关关闭/开启/回退：仪表显示、车把暂停/播放/切歌、C50 声音、后台/锁屏及重连。APK 安装、单测或焦点获准不等于 KTM 适配通过。

## 6. 已验证的无 USB 采集方式

adb 路径：`/Users/taopengcong/Library/Android/sdk/platform-tools/adb`。沙箱内首次启动 adb 服务失败（不能监听本机端口），随后使用获准的提权 adb 命令成功。不要将沙箱错误视为手机问题。

脚本现存：电脑 `/tmp/rideplay-ktm-capture.sh`，手机 `/data/local/tmp/rideplay-ktm-capture.sh`。以下是实际脚本，临时文件丢失时可重建：

```sh
#!/system/bin/sh
OUT="$1"
COUNT="${2:-90}"
INTERVAL="${3:-10}"
mkdir -p "$OUT" || exit 1
exec >> "$OUT/collector.log" 2>&1
trap '' HUP
printf '%s\n' "started $(date) pid=$$ count=$COUNT interval=$INTERVAL"
i=0
while [ "$i" -lt "$COUNT" ]; do
  [ -f "$OUT/STOP" ] && break
  stamp=$(date +%Y%m%d-%H%M%S)
  dumpsys -t 5 media_session > "$OUT/$stamp-media_session.txt" 2>&1
  dumpsys -t 5 audio > "$OUT/$stamp-audio.txt" 2>&1
  dumpsys -t 5 bluetooth_manager > "$OUT/$stamp-bluetooth.txt" 2>&1
  logcat -d -v threadtime -t 400 -s DiPlay-MediaKeys MediaSessionService MediaSessionStack MediaPlayerList AvrcpTargetService > "$OUT/$stamp-keys.txt" 2>&1
  printf '%s\n' "snapshot $i $stamp"
  i=$((i + 1))
  [ "$i" -lt "$COUNT" ] && sleep "$INTERVAL"
done
printf '%s\n' "finished $(date)"
```

已真实完成拔 USB 一分钟测试：7 组，10:42:01 至 10:43:06，正常 `finished`。其后实车 27 组也成功回收，说明本机此配置下已可使用。不能保证重启、系统杀进程等情况仍存活。

启动示例，**每次用新目录，现场准备好后才启动**：

```sh
adb -s bf7b20d shell 'setsid sh /data/local/tmp/rideplay-ktm-capture.sh /sdcard/Download/RidePlay-KTM-UNIQUE 120 15 </dev/null >/dev/null 2>&1 & echo started'
```

120 次、间隔 15 秒加命令耗时，本次按约 30–35 分钟估算；dumpsys 有超时，极端情况下总时长会更长，不是严格墙钟截止。此次无需重新申请 shell 权限或打开无线 ADB。

回来后先停止后续采样，再 pull；需要确认 `finished` 后再作最终归档，避免末尾文件仍在写：

```sh
adb -s bf7b20d shell 'touch /sdcard/Download/RidePlay-KTM-UNIQUE/STOP'
adb -s bf7b20d pull /sdcard/Download/RidePlay-KTM-UNIQUE /tmp/rideplay-ktm-field-review/
```

logcat 每次只看最近 400 条再筛选 tag，不是连续无损全量采集；部分 key 文件为空，不能因此证明没按键。Bluetooth dump 的事件环形历史在本次补足了关键证据。若下次需更强时序证据，改进有界连续采集，不要无界记录。

## 7. 工作区与构建交接

仓库有大量既有未提交修改，见 git status，不要 reset、clean 或覆盖。此次仅写交接文档，不改运行逻辑、设置或 APK。

先读根目录 AGENTS.md；代码修改后必须执行：

```sh
./gradlew :shared:testDebugUnitTest :common:testDebugUnitTest :home:testDebugUnitTest \
  :mobile:lintDebug :home:lintDebug :maphost:lintDebug \
  :mobile:assembleDebug :home:assembleDebug :maphost:assembleDebug
```

SDK：`/Users/taopengcong/Library/Android/sdk`。现存隔离缓存/输出候选 `/private/tmp/diplay-gradle`、`/private/tmp/diplay-build-20261102`、`/private/tmp/diplay-project-cache-20261102`，接手核查后使用，详见 `LOCAL_BUILD_TROUBLESHOOTING.md`，不要清理原目录来碰运气。

真机包按 `BUILD.md` 的 standalone 流程构建，普通 source-only Debug 不包含实际 iPhone 认证资产。不要公开密钥、认证资产或完整蓝牙身份。安装前检查签名，不能为解决签名冲突直接卸载丢设置。

前日交接文档关于“没有现场 media_session/audio/bluetooth 记录”的状态已被本次更新；其中历史构建失败不能代替当前检查结果。本文的完整检查尝试结果见本轮交付说明。

本轮文档交付已尝试上述完整命令：Gradle wrapper 创建 `/Users/taopengcong/.gradle/wrapper/dists/gradle-9.5.0-bin/bvnork1r7n8i6kp5cnkibsc9q/gradle-9.5.0-bin.zip.lck` 被沙箱拒绝（`Operation not permitted`），任务未开始，不能报告检查通过。本轮没有源码变更，未为文档任务升级权限重跑构建。


## 后续实施更新（2026-11-03）

焦点实验现已实现，详见 `KTM_FOCUS_EXPERIMENT.md`。新增 Advanced 设置
`settings_control_only_audio_focus`，持久化 key 为 `rideplay_control_only_audio_focus`，
默认 false，下次 controller attach 读取；全局禁用媒体按键焦点优先。控制专用模式
不转发焦点事件给本地音频 sink。申请失败仍保留媒体会话；诊断增加实际模式、
焦点请求结果、歌曲字段存在性及 CarPlay 按键转发尝试次数。没有静音播放或强制路由。

完整 AGENTS.md 检查使用现有临时 Gradle 缓存、SDK 及隔离输出，首次因沙箱套接字
权限失败，获准升级后 `BUILD SUCCESSFUL in 11m 33s`，日志 `/tmp/rideplay-focus-ci.log`。
common 全量 1779 项零失败；控制专用会话 24 项、焦点转发 33 项和翻译测试通过。
追加的高密度中文截图测试和 standalone 构建成功（22 秒），日志
`/tmp/rideplay-focus-standalone.log`。紧凑日文、宽屏英文、紧凑阿拉伯语和高密度中文
大字体截图位于 `common/build/reports/control-only-media-ui/`，已目视检查。

实验包 `/private/tmp/diplay-build-20261102/mobile/outputs/apk/debug/mobile-debug.apk`，
SHA256 `d67f6e6964387c0a45712e11fb3cb3b4ca613be8d9b4cc79f055f88974331d3f`。
与原安装包签名一致，两个认证资产与原包逐字节相同。原安装包备份
`/tmp/rideplay-before-focus.apk`；认证资产仅在本地临时目录，没有加入仓库。
实车仍未验证本次焦点实验是否有效，不能将上述本地检查当作 KTM 修复成功。

安装结果：已通过 `adb install -r` 覆盖更新成功（Success），没有卸载；启动命令将已有任务带到前台。尚未开启实验，也未进行本次实验的 KTM 实车验证。
