# RidePlay

*面向摩托車騎乘場景最佳化的 CarPlay 接收端。*

[English](README.en.md) · [简体中文](README.zh-CN.md) · **繁體中文**

<img src="site/assets/icon.png" alt="RidePlay 圖示" width="160">

RidePlay 是基於 [DiPlay](https://github.com/shihabal3amri/DiPlay) 客製的 CarPlay 接收端應用程式，安裝在 Android 接收設備上，配合 iPhone 使用。首個適配目標為 **KTM MY RIDE** 等支援藍牙儀表與車把按鍵控制的車型。

[下載安裝包](https://github.com/Chiccccken/RidePlay/releases) · [安裝與首次連線](docs/RIDEPLAY_QUICKSTART.zh-CN.md) · [0.3.1 發行說明](docs/RELEASE-NOTES-0.3.1.md) · [從原始碼建置](docs/BUILD.md)

## 專案背景與適用場景

在摩托車騎乘場景中，許多騎士習慣讓 iPhone 直接透過藍牙連接安全帽耳機（如通訊對講耳機），以獲得無壓縮、低延遲的音訊與通話體驗，同時將車把上的 Android 設備用作 CarPlay 導航螢幕。

DiPlay 提供了可靠的有線／無線 CarPlay 接收核心能力。但在上述音訊直連安全帽的組合下：
1. **車把按鍵失效**：CarPlay 音訊不經由 Android 接收端輸出時，Android 系統的 MediaSession 預設不啟用，導致摩托車車把上的藍牙多功能按鍵無法向 iPhone 發送切歌、暫停等控制指令。
2. **儀表中文亂碼**：部分摩托車儀表（如 KTM 液晶螢幕）不支援中文字元編碼，顯示中文歌名時會出現方塊或亂碼。

RidePlay 在保留 DiPlay 原生 CarPlay 接收能力的基礎上，針對性地補充了無音訊媒體工作階段橋接、儀表拼音詮釋資料轉換與螢幕方向鎖定等功能，打造更符合騎乘習慣的完整體驗。

## 核心功能

- **無音訊媒體控制（實驗性）**：當 CarPlay 音訊由手機直接輸出（如藍牙耳機或車輛藍牙）、Android 本機不發聲時，仍向系統發布媒體資訊與播放狀態，並將車把按鍵的播放／暫停／上一首／下一首操作準確轉發至 iPhone。
- **媒體按鍵選路輔助（實驗性）**：針對部分第三方或移植 ROM 藍牙按鍵分發機制，在建立連線或返回應用程式時輔助啟用按鍵通道，確保車把實體按鍵精準路由給 RidePlay。
- **歌曲資訊拼音與歌手別名**：可離線將中文歌名、歌手和專輯名轉換為拼音，徹底解決摩托車儀表不支援中文導致的亂碼問題；支援自訂歌手名稱精確替換（如縮寫或英文名）。僅轉換發往儀表的系統詮釋資料，CarPlay 畫面始終保留原始文字。
- **介面方向偏好**：支援應用程式級的橫向或直向鎖定，並針對啟動時旋轉閃爍進行了平滑過渡處理。
- **獨立共存與安全更新**：使用獨立的 RidePlay 應用程式識別項與套件名稱（`com.chiccccken.rideplay`），可與原版 DiPlay 並存安裝，互不干擾；內建更新機制嚴格校驗發布來源與雜湊值。
- **設定備份與診斷增強**：設定備份完整覆蓋媒體控制、拼音轉換與方向偏好；增強診斷資訊輸出，嚴格過濾通訊協定酬載與敏感隱私。

## 實測驗證環境

目前已在以下實車與設備組合中完成連續多日測試驗證，各項核心體驗均正常穩定：

- **車輛**：2019 KTM 390 Duke (KTM MY RIDE)
- **接收端**：Redmi Note 9 Pro（高通驍龍 750G 平台，HyperOS 4 / Android 17）
- **發送端**：iPhone 14 (iOS 27)
- **音訊鏈路**：iPhone 藍牙直連耳機（支援安全帽/車載藍牙耳機，亦可直接使用手機揚聲器外放）
- **實測表現**：
  - CarPlay 有線／無線畫面流暢穩定；
  - 音樂由 iPhone 直接輸出至耳機或揚聲器，播放流暢穩定；
  - KTM 液晶儀表正常顯示歌曲拼音、歌手及播放進度；
  - KTM 原廠車把按鍵（播放／暫停／切歌）正常控制 iPhone 播放；
  - 設備重啟、應用程式重新連線及中斷重新連線流程均順暢恢復。

*註：不同車型儀表通訊協定與 Android 韌體機制可能存在差異，歡迎在其他摩托車車型上測試並分享回饋。*

## 安裝與建置

安裝在 **Android 接收端**，最低支援 Android 7.0（API 24）。正式套件名稱為 `com.chiccccken.rideplay`，debug 套件名稱為 `com.chiccccken.rideplay.debug`。兩者資料獨立。

從 [RidePlay Releases](https://github.com/Chiccccken/RidePlay/releases) 下載 APK，並建議使用同頁的 `SHA256SUMS.txt` 檢查完整性。

一般原始碼／CI 建置不含執行時期配件認證素材；公開預先編譯 APK 包含上游實驗性配件身分。自行編譯實機測試包請參閱[建置說明](docs/BUILD.md)和[第三方聲明](docs/THIRD_PARTY_NOTICES.md)。

```sh
./gradlew :mobile:assembleDebug
```

完整單元測試、Lint 和建置命令見 [AGENTS.md](AGENTS.md)。

## 儲存庫與維護

[`custom`](https://github.com/Chiccccken/RidePlay/tree/custom) 是 RidePlay 開發及預設展示分支，`main` 用於上游鏡像。上游更新的同步機制詳見[維護設計](docs/KTM_UPSTREAM_SYNC.md)。

[安裝與連線](docs/INSTALL.md) · [相容性](docs/COMPATIBILITY.md) · [隱私說明](docs/PRIVACY.md) · [發行檢查紀錄](docs/RIDEPLAY_RELEASE_CHECKLIST.md)

## 來源與致謝

感謝 [DiPlay](https://github.com/shihabal3amri/DiPlay) 和 [xcertplay](https://github.com/shilapi/xcertplay) 提供的紮實基礎，本專案遵循 GPL-3.0 開源協議。介面與站點設計參考 [DiAuto](https://github.com/shihabal3amri/DiAuto)（AGPL-3.0），完整保留原有[許可證及第三方聲明](docs/THIRD_PARTY_NOTICES.md)。本專案為獨立開源適配，與 Apple、KTM 或比亞迪無隸屬或商業背書關係。
