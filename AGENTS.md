# AGENTS.md

Guide for AI agents working on this repository. Read it before making changes.

## What this is

A floating teleprompter strip for Android. It does **not** include a camera: it
draws a window on top of the phone's camera app so you can record with the app
you already use. Scripts are plain `.txt` files stored inside the app.

## Stack

- Kotlin, Android Gradle Plugin 9 with **built-in Kotlin support**. Do not add
  `org.jetbrains.kotlin.android` or any other Kotlin plugin.
- `compileSdk` / `targetSdk` 36, `minSdk` 28.
- **No AndroidX, no Jetpack Compose, no third-party libraries.** The UI is
  framework views and the overlay is a custom `FrameLayout`. Keep it that way:
  adding a dependency needs explicit approval first.

## Build and test

| Action | Command |
| --- | --- |
| Debug APK | `./gradlew :app:assembleDebug` |
| Release APK | `./gradlew :app:assembleRelease` |
| Unit tests | `./gradlew :app:testDebugUnitTest` |
| Lint | `./gradlew :app:lintDebug` |
| Install | `adb install -r app/build/outputs/apk/release/app-release.apk` |

Requires JDK 17 or newer and the Android SDK. Provide `sdk.dir` in
`local.properties` or set `ANDROID_HOME`.

The release build is signed with the local debug key unless `keystore.properties`
is present, in which case that key is used. Never commit the keystore or its
properties file.

## Layout

```
app/src/main/java/dev/jorgex/teleprompter/
  ScriptListActivity.kt   script list, import TXT, rename, delete, Stop button
  EditorActivity.kt       script editor, saves on exit
  SettingsActivity.kt     strip settings and gesture reference
  ScriptStore.kt          .txt library in files/scripts
  PromptSettings.kt       settings model and constants
  SettingsStore.kt        SharedPreferences persistence
  PromptLauncher.kt       permission check and overlay startup
  OverlayPermission.kt    "Appear on top" permission
  overlay/
    PromptService.kt      foreground service holding the window
    PromptOverlay.kt      WindowManager params, position, visibility
    PromptView.kt         text drawing, scroll engine, touch gestures
app/src/test/java/dev/jorgex/teleprompter/ScriptStoreTest.kt
```

## Invariants

- **Package `dev.jorgex.teleprompter` is fixed.** Changing it breaks updates on
  devices that already have the app installed.
- **UI strings are hardcoded** in layouts and Kotlin on purpose. The app is
  single-language (English). Do not migrate them to `strings.xml` unless asked.
- **The demo script stays in Spanish.** `demo_script_title` and `demo_script` in
  `strings.xml` are the only non-English strings; that is intentional.
- **Do not remove the manual measurement in `PromptView.onMeasure`.** A
  `FrameLayout` measures a `WRAP_CONTENT` child with the window height as a
  maximum, which clamps the text and breaks scrolling.
- **The strip is a separate overlay window on purpose**: it only receives
  touches inside its own bounds, so the camera keeps the rest of the screen.
- Gestures are part of the product: tap starts or pauses, drag moves the script
  1:1 with the finger, double tap closes the teleprompter.
- Settings are global and read once when the overlay is created. Changing them
  while the strip is running has no effect until it is restarted.
- There is no public API to know when another app starts recording, so playback
  is manual by design. Do not try to "fix" that with hacks.

## Verification

- There is no usable emulator in this environment. Test overlay changes on a
  physical device, and check them against a real camera app: that is the whole
  point of the app.
- Run `testDebugUnitTest` and `lintDebug` before committing.
- Overlay changes that affect layout must also be checked with the `center`
  position and with text longer than the visible lines.

## License

MIT. Contributions are released under the same license.
