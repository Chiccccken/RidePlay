# RidePlay

*面向摩托车的个性化 CarPlay 体验。*

**简体中文** · [English](README.en.md) · [繁體中文](README.zh-TW.md)

<img src="site/assets/icon.png" alt="RidePlay 图标" width="160">

RidePlay 是基于 [DiPlay](https://github.com/shihabal3amri/DiPlay) 的个人定制项目，安装在 Android 接收端，与 iPhone 配合使用。首个适配目标是 **KTM MY RIDE**，项目名称不限定摩托车品牌。

[下载安装包](https://github.com/Chiccccken/RidePlay/releases) · [安装与首次连接](docs/RIDEPLAY_QUICKSTART.zh-CN.md) · [0.3.1 发布说明](docs/RELEASE-NOTES-0.3.1.md) · [从源码构建](docs/BUILD.md)

## 为什么 Fork DiPlay

我的使用场景是骑摩托车：在 Android 接收端上查看 CarPlay 导航和界面，让 iPhone 直接通过蓝牙向头盔输出音乐，同时继续使用 KTM 原厂车把按键播放、暂停和切歌。

DiPlay 已经提供了有线／无线 CarPlay 接收能力，是这个项目的基础。但在我的使用组合中，音乐不经过 Android 播放时，系统媒体会话和蓝牙媒体按键的选路还需要额外适配；仪表上的中文歌曲信息也需要更适合阅读的显示方式。因此，我 Fork 了 DiPlay，在原有接收能力上补充媒体控制、歌曲信息转换和日常使用设置，形成 RidePlay。

## 相比 DiPlay 做了哪些变动

- **无音频媒体控制（实验性）**：关闭 CarPlay 音频接收时，仍向 Android 发布歌曲信息和真实播放状态，并将播放／暂停／上一首／下一首操作转发给 iPhone，配合 iPhone 直接向头盔输出音乐的用法。
- **媒体按键选路引导（实验性）**：为部分系统增加可选的选路引导，在收到 iPhone 媒体信息或返回 CarPlay 时，帮助系统选择 RidePlay 接收媒体按键。默认关闭，重连后生效；启用后可能接管其他 Android 播放器的媒体按键控制权。
- **歌曲信息拼音与歌手名称替换**：可离线转换歌名、歌手和专辑名，并设置完整歌手名称的精确替换。转换只作用于 Android 媒体会话，CarPlay 画面保留原文。拼音功能需要 Android 10 及以上，默认关闭。
- **界面方向偏好**：首页、设置和 CarPlay 支持保存方向设置，并增加启动方向切换的缓解处理。
- **独立应用与更新渠道**：使用 RidePlay 名称、图标、包名及正式签名；应用内更新检查指向本仓库，并校验下载来源和校验文件。可与原版 DiPlay 共存。
- **设置备份与诊断改进**：备份覆盖媒体控制、拼音、歌手名称和方向偏好；诊断增加有界的协议命令摘要与未确认数据包归属信息，过滤协议载荷和敏感字段；原生库诊断路径使用项目相对路径。

有线／无线 CarPlay 接收及现有车机集成来自上游。比亚迪专属功能仍沿用其原有适用范围。

## 当前实测范围

当前公开版本为 **0.3.1 公开测试版**。我已在 **2019 KTM 390 Duke** 上连续多日使用，确认以下操作正常：

- 安装后连接、App 内重连，以及 Android 接收设备重启后打开 App 再连接。
- 实车车把播放／暂停／上一首／下一首。
- iPhone 到头盔的正常音乐输出。

以上是实际使用反馈。其他车型和 Android 系统仍需各自测试；Siri、电话音频、完整后台／锁屏恢复及启动方向尚未完成专项验收。当前版本的范围以[发布说明](docs/RELEASE-NOTES-0.3.1.md)为准，早期设计和试验文档保留当时的记录。

## 安装与构建

安装在 **Android 接收端**，最低支持 Android 7.0（API 24）。正式包名为 `com.chiccccken.rideplay`，debug 包名为 `com.chiccccken.rideplay.debug`。两者数据独立，测试时只运行一个 CarPlay 接收端。

从 [RidePlay Releases](https://github.com/Chiccccken/RidePlay/releases) 下载 APK，并使用同页的 `SHA256SUMS.txt` 核对。更新使用相同正式签名的版本可保留数据；从 debug 版迁移请先备份设置。

普通源码／CI 构建不含独立连接所需的运行时配件认证材料；公开安装包包含上游实验性配件认证身份。准备自己的真机测试包前请阅读[构建说明](docs/BUILD.md)和[第三方声明](docs/THIRD_PARTY_NOTICES.md)。

```sh
./gradlew :mobile:assembleDebug
```

完整单元测试、Lint 和构建命令见 [AGENTS.md](AGENTS.md)。

## 仓库与文档

[`custom`](https://github.com/Chiccccken/RidePlay/tree/custom) 是 RidePlay 开发及默认展示分支，`main` 用于上游镜像。上游更新的同步方案见[维护设计](docs/KTM_UPSTREAM_SYNC.md)。

[安装与连接](docs/INSTALL.md) · [兼容性](docs/COMPATIBILITY.md) · [隐私](docs/PRIVACY.md) · [发布检查记录](docs/RIDEPLAY_RELEASE_CHECKLIST.md)

部分安装、兼容性和隐私文档保留上游背景。诊断报告目录继续使用 `Downloads/DiPlay`；公开报告前请检查并移除个人信息。

## 来源与致谢

感谢 [DiPlay](https://github.com/shihabal3amri/DiPlay) 和 [xcertplay](https://github.com/shilapi/xcertplay) 提供的基础，项目遵循 GPL-3.0。上游界面和网站参考 [DiAuto](https://github.com/shihabal3amri/DiAuto)（AGPL-3.0），保留原有[许可证及第三方声明](docs/THIRD_PARTY_NOTICES.md)。本项目独立开发，与 Apple、KTM、比亚迪无隶属或背书关系。
