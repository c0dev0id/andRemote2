# Development Journal

## Software Stack

- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Build system**: Gradle (Groovy DSL)
- **UI**: XML layouts + Material Components (`com.google.android.material:material:1.11.0`)

## Core Features

- **Floating overlay**: `OverlayService` draws a `TYPE_APPLICATION_OVERLAY` window with a DMD Remote 2 control layout, running as a foreground service.
- **Draggable overlay**: `DraggableOverlayLayout` allows repositioning; position is persisted in `SharedPreferences`.
- **Size & opacity control**: Configurable via sliders in `MainActivity`; changes are applied live via a `SharedPreferences` listener in the service.
- **ADB control**: `OverlayControlReceiver` (exported) accepts `OVERLAY_SHOW` / `OVERLAY_HIDE` broadcasts to start/stop the overlay headlessly.
- **Self-update**: `UpdateChecker` polls GitHub Releases and offers in-app APK download + install.

## Key Decisions

- **Overlay collapse handle + in-overlay menu**: `CollapseHandleView` (32dp circle) floats at the top-left of the overlay window, half-outside the `bg_overlay` background. This is achieved by giving `overlayContent` `marginStart=16dp` / `marginTop=16dp` (half handle size) while keeping the handle at position (0,0) of the window. Tap → opens a mini context menu; drag → repositions the window. When collapsed, the WindowManager window is shrunk to 32dp×32dp so only the handle is visible. A `FrameLayout` root wraps the original `DraggableOverlayLayout` (`overlayContent`) to allow the handle and menu to float on top.
- **In-overlay menu without focus**: `FLAG_NOT_FOCUSABLE` prevents `PopupMenu`/`AlertDialog`. The menu is a `LinearLayout` child of the root `FrameLayout`, toggled `GONE`/`VISIBLE`. Window is temporarily expanded to full size when opening the menu while collapsed.
- **`applyScaleAndAlpha()` guard**: if collapsed and menu is hidden, scale/alpha changes from the `SharedPreferences` listener are suppressed so the window stays at handle size.
- **Foreground service for overlay**: Android requires a foreground service for persistent `TYPE_APPLICATION_OVERLAY` windows. The notification provides the mandatory user-visible indicator and a quick-stop action.
- **`OverlayService.isRunning` flag**: A `@Volatile` companion object flag is used to avoid querying the ActivityManager just to check service state. Reliable as long as the process stays alive.
- **No migration code below v1.0.0**: `SharedPreferences` schema changes during development will be handled by wiping prefs rather than writing migration paths.
- **ADB intent receiver**: `OverlayControlReceiver` is exported without a custom permission — it is intended for developer/ADB use only. The `OVERLAY_SHOW` path guards against missing `SYSTEM_ALERT_WINDOW` permission before starting the service.
