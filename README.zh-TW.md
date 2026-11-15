# RidePlay

*面向摩托車的個人化 CarPlay 體驗。*

[简体中文](README.md) · [English](README.en.md) · **繁體中文**

<img src="site/assets/icon.png" alt="RidePlay 圖示" width="160">

RidePlay 是基於 [DiPlay](https://github.com/shihabal3amri/DiPlay) 的個人客製專案，安裝在 Android 接收端，搭配 iPhone 使用。首個適配目標是 **KTM MY RIDE**，專案名稱不限定摩托車品牌。

[下載安裝包](https://github.com/Chiccccken/RidePlay/releases) · [0.3.1 發行說明](docs/RELEASE-NOTES-0.3.1.md) · [從原始碼建置](docs/BUILD.md)

## 為什麼 Fork DiPlay

我的使用情境是騎摩托車：在 Android 接收端查看 CarPlay 導航與介面，讓 iPhone 直接透過藍牙向安全帽輸出音樂，同時繼續使用 KTM 原廠把手按鍵播放、暫停和切歌。

DiPlay 已經提供有線／無線 CarPlay 接收能力，是這個專案的基礎。但在我的使用組合中，音樂不經過 Android 播放時，系統媒體工作階段和藍牙媒體按鍵的路由還需要額外適配；儀表上的中文歌曲資訊也需要更適合閱讀的顯示方式。因此，我 Fork 了 DiPlay，在原有接收能力上補充媒體控制、歌曲資訊轉換和日常使用設定，形成 RidePlay。

## 相較 DiPlay 做了哪些變更

- **無音訊媒體控制（實驗性）**：關閉 CarPlay 音訊接收時，仍向 Android 發布歌曲資訊和實際播放狀態，並將播放／暫停／上一首／下一首操作轉發給 iPhone，配合 iPhone 直接向安全帽輸出音樂的用法。
- **媒體按鍵路由引導（實驗性）**：為部分系統增加可選的路由引導，在收到 iPhone 媒體資訊或返回 CarPlay 時，協助系統選擇 RidePlay 接收媒體按鍵。預設關閉，重新連線後生效；啟用後可能接管其他 Android 播放器的媒體按鍵控制權。
- **歌曲資訊拼音與歌手名稱替換**：可離線轉換歌名、歌手和專輯名稱，並設定完整歌手名稱的精確替換。轉換只作用於 Android 媒體工作階段，CarPlay 畫面保留原文。拼音功能需要 Android 10 以上，預設關閉。
- **介面方向偏好**：首頁、設定和 CarPlay 支援儲存方向設定，並增加啟動方向切換的緩解處理。
- **獨立應用程式與更新管道**：使用 RidePlay 名稱、圖示、套件名稱及正式簽章；應用程式內更新檢查指向本儲存庫，並驗證下載來源和校驗檔案。可與原版 DiPlay 共存。
- **設定備份與診斷改進**：備份涵蓋媒體控制、拼音、歌手名稱和方向偏好；診斷增加有界的協定命令摘要與未確認封包歸屬資訊，過濾協定內容和敏感欄位；原生函式庫診斷路徑使用專案相對路徑。

有線／無線 CarPlay 接收及現有車機整合來自上游。比亞迪專屬功能仍沿用其原有適用範圍。

## 目前實測範圍

目前公開版本為 **0.3.1 公開測試版**。我已在 **2019 KTM 390 Duke** 上連續多日使用，確認以下操作正常：

- 安裝後連線、應用程式內重新連線，以及 Android 接收裝置重新啟動後開啟應用程式再連線。
- 實車把手播放／暫停／上一首／下一首。
- iPhone 到安全帽的正常音樂輸出。

以上是實際使用回饋。其他車型和 Android 系統仍需各自測試；Siri、電話音訊、完整背景／鎖定畫面恢復及啟動方向尚未完成專項驗收。目前版本的範圍以[發行說明](docs/RELEASE-NOTES-0.3.1.md)為準，早期設計和試驗文件保留當時的紀錄。

## 安裝與建置

安裝在 **Android 接收端**，最低支援 Android 7.0（API 24）。正式套件名稱為 `com.chiccccken.rideplay`，debug 套件名稱為 `com.chiccccken.rideplay.debug`。兩者資料獨立，測試時只執行一個 CarPlay 接收端。

從 [RidePlay Releases](https://github.com/Chiccccken/RidePlay/releases) 下載 APK，並使用同頁的 `SHA256SUMS.txt` 核對。更新使用相同正式簽章的版本可保留資料；從 debug 版遷移請先備份設定。

一般原始碼／CI 建置不含獨立連線所需的執行階段配件認證資料；公開安裝包包含上游實驗性配件認證身分。準備自己的真機測試包前請閱讀[建置說明](docs/BUILD.md)和[第三方聲明](docs/THIRD_PARTY_NOTICES.md)。

```sh
./gradlew :mobile:assembleDebug
```

完整單元測試、Lint 和建置命令見 [AGENTS.md](AGENTS.md)。

## 儲存庫與文件

[`custom`](https://github.com/Chiccccken/RidePlay/tree/custom) 是 RidePlay 開發及預設展示分支，`main` 用於上游鏡像。上游更新的同步方案見[維護設計](docs/KTM_UPSTREAM_SYNC.md)。

[安裝與連線](docs/INSTALL.md) · [相容性](docs/COMPATIBILITY.md) · [隱私](docs/PRIVACY.md) · [發行檢查紀錄](docs/RIDEPLAY_RELEASE_CHECKLIST.md)

部分安裝、相容性和隱私文件保留上游背景。診斷報告目錄繼續使用 `Downloads/DiPlay`；公開報告前請檢查並移除個人資訊。

## 來源與致謝

感謝 [DiPlay](https://github.com/shihabal3amri/DiPlay) 和 [xcertplay](https://github.com/shilapi/xcertplay) 提供的基礎，專案遵循 GPL-3.0。上游介面和網站參考 [DiAuto](https://github.com/shihabal3amri/DiAuto)（AGPL-3.0），保留原有[授權及第三方聲明](docs/THIRD_PARTY_NOTICES.md)。本專案獨立開發，與 Apple、KTM、比亞迪無隸屬或背書關係。
