# RidePlay P1：无音频媒体控制试验

2026-11-01。状态：实验实现已通过完整 Linux Android CI；用户反馈红米上的无音频连接及媒体卡片正常。媒体按键、头盔音频与 KTM AVRCP 尚待真机验证，不是完整功能验收。

## 行为

在 Settings → Advanced 的音频实验卡片中，新增“无音频媒体控制（实验性）”，默认关闭。需同时开启现有“通过车辆蓝牙播放 CarPlay 音频”。新开关保存后只标记需要重连，不主动断开当前会话。

实际连接的 `disableAudioOutput=true` 且新开关开启时，复用原有 Android MediaSession，在 controller 绑定后的主线程元数据回调中创建会话。使用 iPhone 的真实元数据和播放状态，不等待 Android 音频活动，不申请音频焦点，不创建静音 AudioTrack，不更改 iPhone 的蓝牙音频路由。暂停时保留，断开/替换 controller 时释放。PLAY/PAUSE/NEXT/PREVIOUS 使用现有 CarPlay HID 回调。

新开关只在新 controller 创建时读取。关闭并重连即可恢复上游策略；普通接收音频的连接不受此开关影响。排队的旧 controller 音频回调不能创建新 controller 的会话。

测试覆盖默认关闭、实际音频模式门控、元数据/播放状态、无音频焦点、媒体控制、断连/替换、旧回调、下次连接生效；提供大字体窄屏/宽屏和阿拉伯语 RTL 设置截图。完整检查沿用 AGENTS.md，custom 推送也触发 Android CI。

## 验证顺序

1. 完整 Android CI 与上述专项测试成功；只证明源码行为。
2. 测试包独立安装为 `com.chiccccken.rideplay.debug`；原版 `com.shihab.diplay` 保留。配置和配对相互独立，测试时只运行一个接收端。
3. source-only APK 不含独立连接 iPhone 所需认证材料。实际 CarPlay 测试按 BUILD.md 本地配置运行时材料，任何材料、密钥和隐私日志不得提交仓库。
4. iPhone 仍自动直连 C50；核查红米会话的曲名、播放/暂停、元数据变化与控制是否到达 iPhone，并确认红米无音乐流/音频焦点。再测后台、锁屏、重连和开机后连接。
5. 停车连接 KTM，先用红米本机播放器确认仪表/蓝牙基线，再测 RidePlay 的显示和车把操作。卡片出现或 Android MediaController 成功不能代替 KTM AVRCP 验证。

Android 媒体会话选路可能依赖真实的本地音频播放；本实现没有绕过 ROM 的系统选路限制。若系统蓝牙栈不能选中无音频会话，记录不支持的设备组合，不自动启用静音播放或申请焦点。

## 本轮检查记录

此前 macOS 基线被缓存锁与备份服务压力测试耗时影响，未全套通过。改名之前的 Linux 基线运行：<https://github.com/Chiccccken/DiPlay/actions/runs/41278259634>，结果需以 GitHub 状态为准。包名改动已生成 APK 并读取到 `com.chiccccken.rideplay.debug`；完整构建命令退出曾受缓存锁影响，不能将输出 APK 等同全套检查通过。

拼音与来电桥接不属于本试验实现。

## 首次设备反馈（2026-11-01）

- 用户在家中让两台手机连接同一 Wi-Fi，独立测试包已成功连接 iPhone。
- 用户反馈：RidePlay 不播放声音，iPhone 外放，红米控制中心媒体卡片正常显示。
- 这是用户反馈，尚未逐项核对歌曲标题/歌手/进度、系统媒体控制指令、暂停/重连/后台与锁屏行为。
- 当前代码提交 `979d08af` 已通过[完整 Linux Android CI](https://github.com/Chiccccken/DiPlay/actions/runs/41278834173)，本地 73 项专项测试通过；本机完整命令另行运行，不能据此推断本机全套已完成。
- 用户进一步反馈：红米控制中心的上一首、下一首、暂停和播放均正常；iPhone 保持播放时，手动断开重连后媒体卡片也会自动同步。后台、锁屏与开机后恢复尚未验证；C50 直连音频和 KTM 信息显示/车把按键等待实际设备测试。
- Android 媒体卡片正常不等于 KTM 会选择该 AVRCP 会话，实车兼容性继续保留为未验证。
