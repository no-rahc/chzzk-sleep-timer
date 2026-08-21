# Changelog

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
The repository does not contain a persistent distribution keystore. CI artifacts and the v1.2.0 APK are installable debug-signed builds; update installation across independently generated CI APKs is not guaranteed until a persistent signing key is configured securely.
