# 🔔 Tsk.R: Procrastinateless

> **"Congratulations! You're procrastinating inside an anti-procrastination app."**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](#)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](#)

**Tsk.R** (*Task Reminder*) is a high-impact productivity companion for Android engineered to actively dismantle procrastination. Moving away from passive notifications, Tsk.R enforces momentum using a **3-Tiered Warning Cascade**, sarcastic accountability quotes, dynamic Material 3 design, and an interactive Home Screen Widget.

---

## ⚡ Core Features

### ⏰ 3-Tiered Warning Cascade
Never let a deadline sneak up on you again. Tsk.R automatically calculates and schedules three distinct escalation stages for every task:
* ⚠️ **7-Day Early Warning:** Heads-up alert prompting initial preparation before panic sets in.
* 🚨 **24-Hour Countdown:** Urgency-boosting warning to build active momentum.
* 🎯 **Due Now "Mission Mode":** A full-screen priority overlay featuring heartbeat haptics and sarcastic Text-to-Speech (TTS) demanding immediate execution.

### 🎨 Native Material 3 Expressive UI
* **Dynamic Material You:** Automatically pulls dynamic color tokens from your system wallpaper across dark and light modes.
* **14-Day Adaptive Calendar Ribbon:** Interactive strip centered around today's date for fast schedule visibility.
* **Smooth Micro-animations:** Built entirely with modern Jetpack Compose state animations.

### 🧩 Jetpack Glance Home Widget
* View top pending tasks and target deadlines directly from your home screen.
* High-contrast dark container design matching your app aesthetic.

---

## 🛠️ Tech Stack & Architecture

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material 3)
* **Architecture:** MVVM + Clean Architecture / Repository Pattern
* **Database:** Room Database (Offline-First)
* **Background Scheduling:** `AlarmManager` with exact execution guards & broadcast receivers
* **Widget Engine:** Jetpack Glance
* **Package Identity:** `com.brixavier.tskr`

---

## 🚀 Getting Started

### Prerequisites
* Android 8.0 (API Level 26) or higher.
* Recommended: Android 13+ (API 33) for full dynamic Material You themed icons.

### Building from Source
1. Clone the repository:
   ```bash
   git clone [https://github.com/YOUR_GITHUB_USERNAME/Tsk.R-Procrastinateless.git](https://github.com/YOUR_GITHUB_USERNAME/Tsk.R-Procrastinateless.git)
