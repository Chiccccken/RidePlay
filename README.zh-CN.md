# RidePlay

*面向摩托车骑行场景优化的 CarPlay 接收端。*

**简体中文** · [English](README.en.md) · [繁體中文](README.zh-TW.md)

<img src="site/assets/icon.png" alt="RidePlay 图标" width="160">

RidePlay 是基于 [DiPlay](https://github.com/shihabal3amri/DiPlay) 定制的 CarPlay 接收端应用，安装在 Android 接收设备上，配合 iPhone 使用。首个适配目标为 **KTM MY RIDE** 等支持蓝牙仪表与车把按键控制的车型。

[下载安装包](https://github.com/Chiccccken/RidePlay/releases) · [安装与首次连接](docs/RIDEPLAY_QUICKSTART.zh-CN.md) · [0.3.1 发布说明](docs/RELEASE-NOTES-0.3.1.md) · [从源码构建](docs/BUILD.md)

## 项目背景与适用场景

在摩托车骑行场景中，许多骑手习惯让 iPhone 直接通过蓝牙连接头盔耳机（如对讲耳机），以获得无压缩、低延迟的音频与通话体验，同时将车把上的 Android 设备用作 CarPlay 导航显示屏。

DiPlay 提供了可靠的有线／无线 CarPlay 接收核心能力。但在上述音频直连头盔的组合下：
1. **车把按键失效**：CarPlay 音频不经由 Android 接收端输出时，Android 系统的 MediaSession 默认不激活，导致摩托车车把上的蓝牙多功能按键无法向 iPhone 发送切歌、暂停等控制指令。
2. **仪表中文乱码**：部分摩托车仪表（如 KTM 液晶屏）不支持中文字符编码，显示中文歌名时会出现方块或乱码。

RidePlay 在保留 DiPlay 原生 CarPlay 接收能力的基础上，针对性地补充了无音频媒体会话桥接、仪表拼音元数据转换与屏幕方向锁定等功能，打造更符合骑行习惯的完整体验。

## 核心特性

- **无音频媒体控制（实验性）**：当 CarPlay 音频由手机直接输出（如蓝牙耳机或车辆蓝牙）、Android 本机不发声时，仍向系统发布媒体信息与播放状态，并将车把按键的播放／暂停／上一首／下一首操作准确转发至 iPhone。
- **媒体按键选路辅助（实验性）**：针对部分第三方或移植 ROM 蓝牙按键分发机制，在建立连接或返回应用时辅助激活按键通道，确保车把物理按键精准路由给 RidePlay。
- **歌曲信息拼音与歌手别名**：可离线将中文歌名、歌手和专辑名转换为拼音，彻底解决摩托车仪表不支持中文导致的乱码问题；支持自定义歌手名称精确替换（如缩写或英文名）。仅转换发往仪表的系统元数据，CarPlay 界面始终保留原始文本。
- **界面方向偏好**：支持应用级的横屏或竖屏锁定，并针对启动时旋转闪烁进行了平滑过渡处理。
- **独立共存与安全更新**：使用独立的 RidePlay 应用标识与包名（`com.chiccccken.rideplay`），可与原版 DiPlay 并存安装，互不干扰；内置更新机制严格校验发布源与散列值。
- **设置备份与诊断增强**：设置备份完整覆盖媒体控制、拼音转换与方向偏好；增强诊断信息输出，严格过滤协议载荷与敏感隐私。

## 实测验证环境

目前已在以下实车与设备组合中完成连续多日测试验证，各项核心体验均正常稳定：

- **车辆**：2019 KTM 390 Duke (KTM MY RIDE)
- **接收端**：Redmi Note 9 Pro（高通骁龙 750G 平台）
- **发送端**：iPhone 14 (iOS 17)
- **音频链路**：iPhone 蓝牙直接连接恒疆 C50 头盔耳机（音频直出）
- **实测表现**：
  - CarPlay 有线／无线画面流畅稳定；
  - 音乐由 iPhone 直出头盔耳机，音质无损零延迟；
  - KTM 液晶仪表正常显示歌曲拼音、歌手及播放进度；
  - KTM 原厂车把按键（播放／暂停／切歌）正常控制 iPhone 播放；
  - 设备重启、应用重连及断开重连流程均顺畅恢复。

*注：不同车型仪表协议与 Android 固件机制可能存在差异，欢迎在其他摩托车车型上测试并分享反馈。*

## 安装与构建

安装在 **Android 接收端**，最低支持 Android 7.0（API 24）。正式包名为 `com.chiccccken.rideplay`，debug 包名为 `com.chiccccken.rideplay.debug`。两者数据独立。

从 [RidePlay Releases](https://github.com/Chiccccken/RidePlay/releases) 下载 APK，并建议使用同页的 `SHA256SUMS.txt` 校验完整性。

普通源码／CI 构建不含运行时配件认证材料；公开预编译 APK 包含上游实验性配件身份。自行编译真机测试包请参阅[构建说明](docs/BUILD.md)和[第三方声明](docs/THIRD_PARTY_NOTICES.md)。

```sh
./gradlew :mobile:assembleDebug
```

完整单元测试、Lint 和构建命令见 [AGENTS.md](AGENTS.md)。

## 仓库与维护

[`custom`](https://github.com/Chiccccken/RidePlay/tree/custom) 是 RidePlay 开发及默认展示分支，`main` 用于上游镜像。上游更新的同步机制详见[维护设计](docs/KTM_UPSTREAM_SYNC.md)。

[安装与连接](docs/INSTALL.md) · [兼容性](docs/COMPATIBILITY.md) · [隐私说明](docs/PRIVACY.md) · [发布检查记录](docs/RIDEPLAY_RELEASE_CHECKLIST.md)

## 来源与致谢

感谢 [DiPlay](https://github.com/shihabal3amri/DiPlay) 和 [xcertplay](https://github.com/shilapi/xcertplay) 提供的扎实基础，本项目遵循 GPL-3.0 开源协议。界面与站点设计参考 [DiAuto](https://github.com/shihabal3amri/DiAuto)（AGPL-3.0），完整保留原有[许可证及第三方声明](docs/THIRD_PARTY_NOTICES.md)。本项目为独立开源适配，与 Apple、KTM 或比亚迪无隶属或商业背书关系。
