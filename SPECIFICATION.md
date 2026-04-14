# Joystick Control Specification

## Overview

The joystick control (`JoystickView`) operates in one of two exclusive modes, selectable via the **Enable 360°** toggle in the app settings:

| Mode | Signal sent | Signal suppressed |
|------|-------------|-------------------|
| D-pad (default) | `key_press` / `key_release` | `joy` string |
| 360° | `joy` string | `key_press` / `key_release` |

Both modes use the `com.thorkracing.wireddevices.keypress` broadcast intent with `deviceName = "Remote2"`. When the mode is changed while the overlay is running, any active state (held key or non-neutral joy string) is released/reset before switching.

---

## Coordinate System

- Origin is the **center of the joystick base circle**.
- `dx` is positive to the right, negative to the left.
- `dy` is positive downward, negative upward (Android screen coordinates).
- The knob is clamped to the edge of the base circle. The usable travel range is `maxDist = baseRadius - knobRadius`.

---

## Signal 1: Analog Joy String

### Format

```
[U|D<mag>][L|R<mag>]   — when displaced
Y0X0                   — neutral (center)
```

Each axis contributes independently. Both, one, or neither component may be present.

- `U` = up (dy < 0), `D` = down (dy > 0)
- `L` = left (dx < 0), `R` = right (dx > 0)
- `<mag>` is an integer from 2–5

Examples: `U3R4`, `D2`, `L5`, `U5L5`, `Y0X0`

### Magnitude Mapping

Each axis is normalized independently: `norm = |displacement| / maxDist`, clamped to [0, 1].

| norm range    | magnitude |
|---------------|-----------|
| < 0.25        | (no output — dead zone) |
| 0.25 – <0.50  | 2 |
| 0.50 – <0.70  | 3 |
| 0.70 – <0.85  | 4 |
| ≥ 0.85        | 5 |

If both axes are in the dead zone, the string is `Y0X0`.

### Deduplication

The joy string is only broadcast when it **changes**. Holding the joystick still produces no repeated events.

### Release

On `ACTION_UP` or `ACTION_CANCEL`, `Y0X0` is sent (once, if not already the last sent value), and the internal last-string state is cleared.

---

## Signal 2: D-pad Key Events

### Dead Zone

D-pad events are only emitted when the displacement from center exceeds **30% of `baseRadius`**. Within this radius, any active key is released and no new key is pressed.

### Axis Selection

The D-pad tracks a **single key at a time** — the dominant axis wins:

- If `|dx| > |dy|`: horizontal axis — `keycodeRight` or `keycodeLeft`
- Otherwise: vertical axis — `keycodeDown` or `keycodeUp`

### Default Key Codes

| Direction | Default keycode |
|-----------|----------------|
| Up        | 19 (KEYCODE_DPAD_UP) |
| Down      | 20 (KEYCODE_DPAD_DOWN) |
| Left      | 21 (KEYCODE_DPAD_LEFT) |
| Right     | 22 (KEYCODE_DPAD_RIGHT) |

Key codes are configurable via `setKeyCodes(up, down, left, right)`.

### State Transitions

- When the active direction changes, the old key is **released before the new key is pressed**.
- On `ACTION_UP` or `ACTION_CANCEL`, the active key (if any) is released.
- No key repeat is ever sent — only press and release.

---

## Broadcast Intent Summary

| Extra key    | Type    | Present when              | Value                        |
|--------------|---------|---------------------------|------------------------------|
| `joy`        | String  | Analog signal              | Joy string (e.g. `U3R4`, `Y0X0`) |
| `key_press`  | Integer | D-pad direction activated  | Key code                     |
| `key_release`| Integer | D-pad direction deactivated| Key code                     |
| `deviceName` | String  | Always                    | `"Remote2"`                  |

Each broadcast carries exactly one of `joy`, `key_press`, or `key_release` — never combined.
