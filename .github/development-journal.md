# Development Journal

## Software Stack

- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Build system**: Gradle (Groovy DSL)
- **UI**: XML layouts + Material Components (`com.google.android.material:material:1.11.0`)
- **Key dependency**: [Shizuku](https://shizuku.rikka.app) — required at runtime to send keycodes to the foreground app (not a compile-time dependency; interacted with via ADB/shell)

## Core Features

- **Floating overlay**: `OverlayService` draws a `TYPE_APPLICATION_OVERLAY` window with a DMD Remote 2 control layout, running as a foreground service.
- **Draggable overlay**: `DraggableOverlayLayout` allows repositioning; position is persisted in `SharedPreferences`.
- **Size & opacity control**: Configurable via sliders in `MainActivity`; changes are applied live via a `SharedPreferences` listener in the service.
- **ADB control**: `OverlayControlReceiver` (exported) accepts `OVERLAY_SHOW` / `OVERLAY_HIDE` broadcasts to start/stop the overlay headlessly.
- **Self-update**: `UpdateChecker` polls GitHub Releases and offers in-app APK download + install.

## Key Decisions

- **Foreground service for overlay**: Android requires a foreground service for persistent `TYPE_APPLICATION_OVERLAY` windows. The notification provides the mandatory user-visible indicator and a quick-stop action.
- **`OverlayService.isRunning` flag**: A `@Volatile` companion object flag is used to avoid querying the ActivityManager just to check service state. Reliable as long as the process stays alive.
- **No migration code below v1.0.0**: `SharedPreferences` schema changes during development will be handled by wiping prefs rather than writing migration paths.
- **ADB intent receiver**: `OverlayControlReceiver` is exported without a custom permission — it is intended for developer/ADB use only. The `OVERLAY_SHOW` path guards against missing `SYSTEM_ALERT_WINDOW` permission before starting the service.
