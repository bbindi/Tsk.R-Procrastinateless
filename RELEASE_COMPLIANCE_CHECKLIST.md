# Tsk.R 2.1.0 — Release Compliance & Verification Guide

**Developer:** Brixavier (`com.brixavier.tskr`)  
**Target Release:** Tsk.R 2.1.0 (Version Code 6)  
**Effective Date:** October 14, 2026  

This guide provides the complete release compliance verification state for Tsk.R 2.1.0, categorizing requirements into **[Code-Verified]** implementation facts, **[Console Action]** developer tasks in AdMob and Google Play Console, and **[Developer Decision]** policy choices.

---

## 1. Verified Source Code Architecture [Code-Verified]

- [x] **1.1 AdMob & UMP SDK Dependencies [Code-Verified]:**
  * `com.google.android.gms:play-services-ads:25.5.0`
  * `com.google.android.ump:user-messaging-platform:4.0.0`
  * Official Docs: [AdMob Mobile Ads SDK for Android](https://developers.google.com/admob/android/quick-start) | [User Messaging Platform (UMP) SDK](https://developers.google.com/admob/android/privacy)

- [x] **1.2 Manifest Permissions & AdMob App ID [Code-Verified]:**
  * Permissions added: `INTERNET`, `ACCESS_NETWORK_STATE`.
  * Manifest placeholder `${adMobAppId}` injected via `app/build.gradle.kts`.
  * Defaults safely to Google Sample Test App ID `ca-app-pub-3940256099942544~3347511713` in development.

- [x] **1.3 Test & Production ID Resolution [Code-Verified]:**
  * Native Ad Unit ID injected via `BuildConfig.ADMOB_NATIVE_AD_UNIT_ID`.
  * Production IDs can be passed via `release-signing.properties` (`adMobAppId`, `adMobNativeAdUnitId`) or environment variables (`TSKR_ADMOBAPPID`, `TSKR_ADMOBNATIVEADUNITID`).
  * If building release binaries with test IDs, Gradle outputs an explicit build warning. Custom IDs are format-validated.

- [x] **1.4 Data Isolation Boundary [Code-Verified]:**
  * Personal task titles, due dates, notes, plan steps, and checklist items are stored in private local SQLite (`ReminderDatabase.kt`) and **never** passed to AdMob or UMP SDK calls.

- [x] **1.5 Approved Ad Placements & Ad-Free Screen Protection [Code-Verified]:**
  * **Dashboard:** Max 1 native ad rendered after main content (after Plans section).
  * **Task Library:** Max 1 native ad rendered after 5th item when task list size $\ge 6$. Empty/short lists (< 6 items) render 0 ads.
  * **Ad-Free Screens:** Plans, Plan Detail, Calendar 2.0, Rich Notes Editor, Task Details, Creation/Editing Sheets, Mission Mode (`AlarmActivity.kt`), Notifications, Widgets, Welcome, and What's New screens are 100% ad-free.

- [x] **1.6 In-App Review & Update Lifecycle [Code-Verified]:**
  * Review prompt eligibility engine (`InAppReviewEligibilityEngine.kt`) enforces $\ge 5$ sessions, $\ge 3$ days elapsed, $\ge 5$ completed tasks, 30-day cooldown, and permanent dismissal tracking.
  * Permanent "Rate Tsk.R" option in `SettingsPage.kt` opens `market://details?id=com.brixavier.tskr` with browser fallback.
  * Flexible In-App Updates (`PlayInAppUpdateManager.kt`) checks updates asynchronously in `onResume()`, restores downloaded state, and presents non-intrusive restart banners.

---

## 2. Google AdMob Console Tasks [Console Action]

- [ ] **2.1 Register Tsk.R App in AdMob Console [Console Action]:**
  * Log into [AdMob Console](https://admob.google.com/).
  * Register `com.brixavier.tskr` for Android.
  * Retrieve unique **AdMob App ID** (`ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY`).

- [ ] **2.2 Create Native Advanced Ad Unit [Console Action]:**
  * Create a **Native Advanced** ad unit under Tsk.R.
  * Retrieve unique **Native Ad Unit ID** (`ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ`).

- [ ] **2.3 Supply Production IDs to Release Build [Console Action]:**
  * Add production IDs to `release-signing.properties` or environment variables:
    ```properties
    adMobAppId=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
    adMobNativeAdUnitId=ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ
    ```

---

## 3. Google Play Console Tasks [Console Action] & Developer Decisions

- [ ] **3.1 App Content -> "Contains Ads" Declaration [Console Action]:**
  * In [Google Play Console](https://play.google.com/console/), go to **App content** -> **Ads**.
  * Select **"Yes, my app contains ads"**.
  * Guidance: [Google Play Help: Declare whether your app contains ads](https://support.google.com/googleplay/android-developer/answer/9859455)

- [ ] **3.2 Data Safety Form Declarations [Console Action]:**
  * Go to **App content** -> **Data safety**.
  * Declare data processing by Google Mobile Ads (AdMob) SDK:
    * **Device or other IDs:** Advertising ID (Collected, Transmitted, Ephemeral/Processed, Purpose: Advertising & Analytics).
    * **App info & performance:** Diagnostics & Crash logs (Collected, Processed, Purpose: Analytics & Fraud Prevention).
    * **IP Address:** Processed by Google for ad serving and fraud detection.
  * Confirm that **User Personal Data** (Task titles, notes, plan content, checklist items) is marked as **NOT collected or shared**.
  * Guidance: [Google Play Data Safety Guidance](https://support.google.com/googleplay/android-developer/answer/10787469)

- [ ] **3.3 Advertising ID Declaration [Console Action]:**
  * In **App content** -> **Advertising ID**, select **"Yes"** (my app uses Advertising ID for Advertising & Analytics).

- [ ] **3.4 Target Audience & Child-Directed Content [Developer Decision]:**
  * Current policy specifies target audience **13 and older**.
  * Confirm target age selection in Play Console. If serving ads to users in EEA/UK, UMP SDK handles GDPR/TCF v2.2 consent.

- [ ] **3.5 Publish Standalone Privacy Policy URL [Developer Decision / Console Action]:**
  * Host [PRIVACY_POLICY.md](file:///C:/Users/Brix/Desktop/R.Suite/Tsk.R-Procrastinateless/PRIVACY_POLICY.md) on a public HTTPS site (e.g. GitHub Pages).
  * Enter the public URL under **App content** -> **Privacy policy** in Play Console.

---

## 4. Real-Device Physical Testing Track [Manual Verification Required]

- [ ] **4.1 Upload AAB to Play Internal Testing Track [Manual Verification Required]:**
  * Upload `app-release.aab` (`versionCode 6`) to Google Play Console Internal Testing track.
  * Verify UMP consent form rendering on an EEA/UK test device or using UMP debug test device hashed ID (`ConsentDebugSettings.Builder().addTestDeviceHashedId(...)`).
  * Verify Native Ad rendering on Dashboard and Task Library.
  * Verify In-App Review dialog and In-App Update flow.
  * Guidance: [Google Play In-App Review Testing](https://developer.android.com/guide/playcore/in-app-review/test)
