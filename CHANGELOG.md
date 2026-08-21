# Changelog

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
