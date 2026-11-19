# AutoSwipe AI — Part 2

Native Android application built with Kotlin and Jetpack Compose.

## Part 2 features
- Android AccessibilityService with gesture capability
- Floating panel Start, Pause and Stop controls
- Upward swipe every 5 seconds while explicitly running
- App allowlist: TikTok, TikTok Lite, Instagram, YouTube and Facebook
- Touch-enabled floating control panel

## First-time setup
1. Install the updated APK.
2. Open AutoSwipe AI.
3. Grant **Display over other apps**.
4. Tap **Open Accessibility settings**.
5. Find **AutoSwipe AI swipe control** and enable it.
6. Return to the app and tap **Show floating button**.
7. Open a supported short-video feed.
8. Tap the floating **AS** bubble, then **Start auto-swipe**.
9. Use **Pause** or **Stop** whenever needed.

## Limitations
- This is a timed swipe (5 seconds), not video-end detection. It may advance before or after a clip finishes.
- The service checks the active window's app package and only swipes for the listed apps.
- It does not inspect, copy, or store screen text or personal content.
- App versions, feed layout, phone manufacturer settings, and Android accessibility behavior can affect reliability.
- This Part 2 service is not yet a persistent foreground service; some phones may stop it in the background. Background/battery reliability is a later improvement.

## Build
Open this project in Android Studio with JDK 17 and Android SDK 36, sync Gradle, then build the debug APK.
