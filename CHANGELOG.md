# Changelog

## 1.2.7 - 2026-08-21

### UX
- Reduced the 10-minute warning to a compact floating card near the top of the screen instead of a tall modal covering the playback area.
- Replaced the three full-width extension buttons with a single horizontal +5 / +20 / +40 minute row.
- Limited warning, extension-picker, brightness, and accessibility prompt widths on phones and tablets.
- Reduced background dimming behind timer popups so the currently playing screen stays visible.
- Simplified brightness popup copy and spacing while keeping the live slider, reset, and close actions.

## 1.2.6 - 2026-08-21

### Added
- Smooth 30-second media-volume fade before the next active timer reaches its shutdown time.
- One-tap `+20분` extension action on the persistent daily timer notification.
- `연장` notification action that opens a compact +5 / +20 / +40 minute picker while preserving the existing brightness action.

### Changed
- Fade-out follows whichever active timer (daily or one-shot) is due first.
- If the daily timer is extended during the fade, the original media volume is restored immediately and the fade is re-armed for the new shutdown time.
- Fade-out only runs when the existing `미디어 음량 0` shutdown action is enabled, so disabling that action also disables and restores an active fade.
- The scheduled sleep action consumes the fade without restoring volume first, preventing a brief loud-volume flash at the exact shutdown handoff.

### Reliability
- The 30-second fade runs in a short foreground service instead of keeping a broadcast receiver alive for an extended period.
- The normal sleep alarm remains authoritative even if the optional fade service cannot be started.

## 1.2.5 - 2026-08-21

### Changed
- Extra dim now behaves as a temporary bedtime setting instead of carrying into the next morning.
- When a scheduled daily or one-shot timer successfully locks the screen, the stored extra-dim level is reset to 0% and the overlay is removed shortly after the lock transition.
- Manual action testing does not automatically clear extra dim.

### UX
- The extra-dim preference is cleared immediately after a successful scheduled lock, while the visible overlay stays in place for about 750 ms to avoid a bright flash just before the screen turns off.

## 1.2.4 - 2026-08-21

### Added
- Brightness action on the persistent daily timer notification.
- Popup slider for an extra-dim screen filter from 0% to 85%.
- Immediate live dimming while the slider moves, plus a one-tap reset action.
- Persistent extra-dim preference restored when the screen-control accessibility service reconnects.

### Changed
- The existing screen-lock accessibility service now also owns a touch-through accessibility overlay used only for extra dimming.
- Accessibility copy now describes both screen locking and the extra-dim filter while continuing to declare that window content is not read and input is not automated.

### Safety
- Extra dimming uses a transparent black overlay instead of attempting to drive the hardware backlight below vendor limits.
- The overlay is capped at 85% opacity and is non-focusable/non-touchable so normal app interaction remains available.

## 1.2.3 - 2026-08-21

### Changed
- Simplified the main screen to focus on daily shutdown, one-shot timers, shutdown actions, permissions, and testing.
- Removed the decorative eyebrow, long intro copy, duplicated readiness text, implementation notes, and last-run footer from the main UI.
- Condensed one-shot presets into a single row and hide cancel/status controls when no one-shot timer is active.
- Condensed shutdown-action and permission rows to remove repeated explanatory copy.
- Permission rows for media control and screen lock are only shown when their matching shutdown actions are enabled.
- Added a compact top-level status badge for active, idle, and missing-permission states.
- Daily temporary extensions are surfaced directly in the main screen as the current one-time time alongside the normal daily time.

## 1.2.2 - 2026-08-21

### Added
- Warning popup 10 minutes before the recurring daily sleep action.
- Quick extension actions for +5, +20, and +40 minutes directly from the warning.
- Dedicated warning screen for lock-screen/full-screen alarm presentation when Android allows it.

### Changed
- Daily extensions apply only to the current scheduled occurrence and never modify the configured recurring time.
- Extending the current occurrence immediately updates the actual AlarmManager trigger, countdown notification, and next 10-minute warning.
- Reboot/time-change recovery preserves a still-active one-day extension.
- The persistent countdown notification labels a temporary override as "오늘만" and keeps the normal daily time visible.

### Fixed
- After an extended occurrence runs, the next daily alarm is explicitly rebuilt from the normal configured time.

## 1.2.1 - 2026-08-21

### Added
- Persistent daily timer notification while the recurring schedule is enabled.
- System-managed countdown showing the remaining time until the next daily sleep action.
- Android 13+ notification permission request flow when the daily schedule is active.

### Changed
- Changing the daily execution time immediately retargets the countdown notification.
- Running a daily sleep action rolls the notification forward to the next day's schedule.
- Disabling the daily schedule also removes its countdown notification.

## 1.2.0 - 2026-08-21

### Added
- 15, 30, 60, 90 minute quick one-shot timers.
- Custom one-shot timers from 1 to 1440 minutes.
- Independent daily and one-shot schedules that can coexist.
- Per-action controls for media pause, media volume mute, and screen lock.
- Remaining-time and schedule-accuracy status for one-shot timers.
- Unit tests for next-daily-trigger calculation.
- GitHub Release publishing workflow with APK and SHA-256 checksum assets.

### Changed
- Reorganized the main screen around daily schedule, quick timer, actions, permissions, and testing.
- Improved permission/readiness messaging so unused actions do not look like missing required permissions.
- Daily alarms are re-armed before executing sleep actions.
- Both schedule types are restored after reboot, clock/timezone changes, package replacement, exact-alarm permission changes, and app resume.
- Accessibility service lifecycle cleanup is more defensive.
- CI now runs unit tests, Android lint, and APK assembly.

### Fixed
- Daily and one-shot alarms no longer overwrite each other.
- Stale one-shot timers are discarded instead of firing unexpectedly long after their intended time.
- One-shot state is consumed before actions to avoid accidental repeated execution.
- Android 15 system-bar inset handling is preserved for targetSdk 35.

### Signing note
The repository does not contain a persistent distribution keystore. CI artifacts are installable debug-signed builds; update installation across independently generated CI APKs is not guaranteed until a persistent signing key is configured securely.
