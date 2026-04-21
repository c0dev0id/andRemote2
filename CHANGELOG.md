# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **TopIcon key injection**: for non-DMD2, non-nav apps, button and lever presses also dispatch a `hk.topicon.injectinput.INPUT_EVENT` broadcast with the corresponding `KeyEvent`, matching the secondary injection path in wireddevices. Navigation keys (arrows, Escape, F6, F7) repeat at 250ms while held. Silently no-ops on devices without TopIcon installed.
- **Reduce Sensitivity toggle**: when enabled, all joystick output is clamped to magnitude 1 (`U1`, `D1`, `L1`, `R1`, diagonal combinations). Matches the `ReduceJoySensitivity` whitelist enforced by the wireddevices app on serial input. Toggle available in both the overlay context menu and the main settings screen.
- **Automatic joy mode** (`isDMD2InView`): the joystick now automatically switches between analog `joy` token mode and D-pad keycode mode based on the foreground app. When `com.thorkracing.dmd2launcher` or `com.thorkracing.dmdplayground` is in view, the joystick sends `joy` tokens; otherwise it sends standard keycodes. Requires Usage Access permission (grant via the new button in Overlay Settings). Button and lever presses also switch between `down-r2`/`up-r2` (DMD2) and `key_press`/`key_release` (other apps) accordingly.

### Removed
- **Manual 360° toggle**: replaced by automatic foreground-app detection.

### Changed
- **Collapse handle**: reduced from 48dp to 32dp and repositioned half outside the overlay background (top-left corner), giving a cleaner visual separation between the handle and the control panel.

### Added
- **Overlay collapse/expand handle**: a small draggable circle in the top-left corner of the overlay lets the user collapse it to just the handle dot, freeing screen space without stopping the service. Tap the handle to open a context menu with "Collapse/Expand" and "360°: On/Off" actions. Drag the handle to reposition the overlay. Collapsed state persists across service restarts.
- **Enable 360° setting**: new toggle in Overlay Settings switches the joystick from 4-direction keycode mode to the analog `joy` intent protocol (`com.thorkracing.wireddevices.keypress`, `deviceName=Remote2`). Supports diagonal input and 4 deflection magnitudes (2–5) with a dead zone. The running overlay reacts to the setting change without restart.
- `OverlayControlReceiver`: exported `BroadcastReceiver` to enable/disable the overlay via ADB intents.
  - `adb shell am broadcast -a de.codevoid.andremote2.OVERLAY_SHOW -p de.codevoid.andremote2` — starts the overlay (no-op if already running or overlay permission not granted)
  - `adb shell am broadcast -a de.codevoid.andremote2.OVERLAY_HIDE -p de.codevoid.andremote2` — stops the overlay (no-op if not running)
  - `adb shell am broadcast -a de.codevoid.andremote2.OVERLAY_TOGGLE -p de.codevoid.andremote2` — toggles the overlay on/off
  - Note: `-p de.codevoid.andremote2` is required — Android 8+ does not deliver implicit broadcasts to manifest receivers
