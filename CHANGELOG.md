# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- `OverlayControlReceiver`: exported `BroadcastReceiver` to enable/disable the overlay via ADB intents.
  - `adb shell am broadcast -a de.codevoid.andremote2.OVERLAY_SHOW` — starts the overlay (no-op if already running or overlay permission not granted)
  - `adb shell am broadcast -a de.codevoid.andremote2.OVERLAY_HIDE` — stops the overlay (no-op if not running)
  - `adb shell am broadcast -a de.codevoid.andremote2.OVERLAY_TOGGLE` — toggles the overlay on/off
