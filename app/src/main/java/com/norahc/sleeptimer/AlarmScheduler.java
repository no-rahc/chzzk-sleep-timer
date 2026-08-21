package com.norahc.sleeptimer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import java.util.Calendar;

final class AlarmScheduler {
    static final String ACTION_DAILY_SLEEP = "com.norahc.sleeptimer.ACTION_DAILY_SLEEP";
    static final String ACTION_DAILY_WARNING = "com.norahc.sleeptimer.ACTION_DAILY_WARNING";
    static final String ACTION_ONE_SHOT_SLEEP = "com.norahc.sleeptimer.ACTION_ONE_SHOT_SLEEP";
    static final String EXTRA_EXPECTED_DAILY_TRIGGER = "expected_daily_trigger";

    static final String TIMER_SOURCE_NONE = "";
    static final String TIMER_SOURCE_DAILY = "daily";
    static final String TIMER_SOURCE_ONE_SHOT = "one_shot";

    private static final String LEGACY_ACTION_SLEEP = "com.norahc.sleeptimer.ACTION_SLEEP";
    private static final int REQUEST_DAILY = 401;
    private static final int REQUEST_ONE_SHOT = 402;
    private static final int REQUEST_DAILY_WARNING = 403;
    private static final int SCHEDULE_FAILED = 0;
    private static final int SCHEDULE_APPROXIMATE = 1;
    private static final int SCHEDULE_EXACT = 2;
    private static final long DAILY_STALE_GRACE_MS = 30L * 60L * 1000L;
    private static final long ONE_SHOT_STALE_GRACE_MS = 30L * 60L * 1000L;
    private static final long DAILY_WARNING_LEAD_MS = 10L * 60L * 1000L;

    private AlarmScheduler() {
    }

    static boolean canScheduleExactAlarms(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        try {
            return alarmManager.canScheduleExactAlarms();
        } catch (SecurityException ignored) {
            return false;
        }
    }

    static void ensureScheduled(Context context) {
        Context appContext = context.getApplicationContext();
        ensureDailyScheduled(appContext);
        ensureOneShotScheduled(appContext);
        DailyCountdownNotifier.sync(appContext);
        if (AppPrefs.isEnabled(appContext)) {
            DailyCountdownNotifier.requestPermissionIfNeeded(context);
        }
    }

    static void rescheduleAll(Context context) {
        Context appContext = context.getApplicationContext();
        if (AppPrefs.isEnabled(appContext)) {
            scheduleDaily(appContext);
        } else {
            cancelDaily(appContext);
        }

        long oneShot = AppPrefs.getOneShotTrigger(appContext);
        if (oneShot <= 0L) {
            cancelOneShotAlarmOnly(appContext);
            DailyCountdownNotifier.sync(appContext);
            return;
        }

        long now = System.currentTimeMillis();
        if (oneShot < now - ONE_SHOT_STALE_GRACE_MS) {
            cancelOneShot(appContext);
        } else {
            scheduleOneShotAt(appContext, Math.max(oneShot, now + 1_000L));
        }
    }

    static boolean scheduleDaily(Context context) {
        Context appContext = context.getApplicationContext();
        AlarmManager alarmManager = getAlarmManager(appContext);
        if (alarmManager == null) {
            AppPrefs.clearNextTrigger(appContext);
            DailyCountdownNotifier.cancel(appContext);
            return false;
        }

        cancelLegacyDailyAlarm(appContext, alarmManager);
        PendingIntent operation = getDailyPendingIntent(appContext);
        alarmManager.cancel(operation);
        cancelDailyWarningAlarm(appContext);

        if (!AppPrefs.isEnabled(appContext)) {
            AppPrefs.clearNextTrigger(appContext);
            AppPrefs.clearDailyOverride(appContext);
            DailyCountdownNotifier.cancel(appContext);
            DailyWarningNotifier.cancel(appContext);
            return false;
        }

        long now = System.currentTimeMillis();
        long nextTrigger = resolveNextDailyTrigger(appContext, now);
        int result = scheduleAt(appContext, alarmManager, nextTrigger, operation);
        if (result == SCHEDULE_FAILED) {
            AppPrefs.clearNextTrigger(appContext);
            DailyCountdownNotifier.cancel(appContext);
            return false;
        }

        AppPrefs.setNextTrigger(appContext, nextTrigger, result == SCHEDULE_EXACT);
        scheduleDailyWarning(appContext, nextTrigger);
        DailyCountdownNotifier.sync(appContext);
        DailyCountdownNotifier.requestPermissionIfNeeded(context);
        return true;
    }

    static long extendCurrentDaily(Context context, int minutes) {
        if (minutes <= 0 || minutes > 240) {
            return 0L;
        }

        Context appContext = context.getApplicationContext();
        if (!AppPrefs.isEnabled(appContext)) {
            return 0L;
        }

        AlarmManager alarmManager = getAlarmManager(appContext);
        if (alarmManager == null) {
            return 0L;
        }

        long now = System.currentTimeMillis();
        long currentTrigger = AppPrefs.getNextTrigger(appContext);
        if (currentTrigger <= now + 1_000L) {
            return 0L;
        }

        long extendedTrigger = calculateExtendedTrigger(currentTrigger, minutes);
        int result = scheduleAt(
                appContext,
                alarmManager,
                extendedTrigger,
                getDailyPendingIntent(appContext)
        );
        if (result == SCHEDULE_FAILED) {
            return 0L;
        }

        AppPrefs.setDailyOverrideTrigger(appContext, extendedTrigger);
        AppPrefs.setNextTrigger(appContext, extendedTrigger, result == SCHEDULE_EXACT);
        scheduleDailyWarning(appContext, extendedTrigger);
        DailyWarningNotifier.cancel(appContext);
        DailyCountdownNotifier.sync(appContext);
        return extendedTrigger;
    }

    static long extendCurrentOneShot(Context context, int minutes) {
        if (minutes <= 0 || minutes > 240) {
            return 0L;
        }

        Context appContext = context.getApplicationContext();
        long now = System.currentTimeMillis();
        long currentTrigger = AppPrefs.getOneShotTrigger(appContext);
        if (currentTrigger <= now + 1_000L) {
            return 0L;
        }

        long extendedTrigger = calculateExtendedTrigger(currentTrigger, minutes);
        return scheduleOneShotAt(appContext, extendedTrigger) ? extendedTrigger : 0L;
    }

    static boolean isDailyOverrideActive(Context context) {
        return AppPrefs.isDailyOverrideForCurrentSchedule(context)
                && AppPrefs.getDailyOverrideTrigger(context) > System.currentTimeMillis();
    }

    static void consumeDailyOverride(Context context) {
        AppPrefs.clearDailyOverride(context.getApplicationContext());
    }

    static void cancelDailyWarning(Context context) {
        cancelDailyWarningAlarm(context.getApplicationContext());
        DailyWarningNotifier.cancel(context.getApplicationContext());
    }

    static boolean scheduleOneShotAfter(Context context, int minutes) {
        if (minutes < 1 || minutes > 1_440) {
            return false;
        }
        long trigger = System.currentTimeMillis() + minutes * 60_000L;
        return scheduleOneShotAt(context, trigger);
    }

    static boolean scheduleOneShotAt(Context context, long triggerAtMillis) {
        Context appContext = context.getApplicationContext();
        AlarmManager alarmManager = getAlarmManager(appContext);
        if (alarmManager == null || triggerAtMillis <= 0L) {
            AppPrefs.clearOneShotTrigger(appContext);
            DailyCountdownNotifier.sync(appContext);
            return false;
        }

        PendingIntent operation = getOneShotPendingIntent(appContext);
        alarmManager.cancel(operation);

        int result = scheduleAt(appContext, alarmManager, triggerAtMillis, operation);
        if (result == SCHEDULE_FAILED) {
            AppPrefs.clearOneShotTrigger(appContext);
            DailyCountdownNotifier.sync(appContext);
            return false;
        }

        AppPrefs.setOneShotTrigger(appContext, triggerAtMillis, result == SCHEDULE_EXACT);
        DailyCountdownNotifier.sync(appContext);
        return true;
    }

    static void cancelDaily(Context context) {
        Context appContext = context.getApplicationContext();
        AlarmManager alarmManager = getAlarmManager(appContext);
        if (alarmManager != null) {
            alarmManager.cancel(getDailyPendingIntent(appContext));
            cancelLegacyDailyAlarm(appContext, alarmManager);
        }
        cancelDailyWarningAlarm(appContext);
        AppPrefs.clearNextTrigger(appContext);
        AppPrefs.clearDailyOverride(appContext);
        DailyCountdownNotifier.cancel(appContext);
        DailyWarningNotifier.cancel(appContext);
    }

    static void cancelOneShot(Context context) {
        Context appContext = context.getApplicationContext();
        cancelOneShotAlarmOnly(appContext);
        AppPrefs.clearOneShotTrigger(appContext);
        DailyCountdownNotifier.sync(appContext);
    }

    static long getNextDailyTrigger(Context context) {
        long stored = AppPrefs.getNextTrigger(context);
        return stored > 0L
                ? stored
                : calculateNextDailyTrigger(
                        System.currentTimeMillis(),
                        AppPrefs.getHour(context),
                        AppPrefs.getMinute(context)
                );
    }

    static long getOneShotTrigger(Context context) {
        return AppPrefs.getOneShotTrigger(context);
    }

    static boolean isOneShotActive(Context context) {
        return AppPrefs.getOneShotTrigger(context) > 0L;
    }

    static String getNextActiveTimerSource(Context context) {
        long daily = AppPrefs.isEnabled(context) ? AppPrefs.getNextTrigger(context) : 0L;
        long oneShot = AppPrefs.getOneShotTrigger(context);
        return selectNextActiveTimerSource(System.currentTimeMillis(), daily, oneShot);
    }

    static long getNextActiveTimerTrigger(Context context) {
        long daily = AppPrefs.isEnabled(context) ? AppPrefs.getNextTrigger(context) : 0L;
        long oneShot = AppPrefs.getOneShotTrigger(context);
        return selectNextActiveTimerTrigger(System.currentTimeMillis(), daily, oneShot);
    }

    static long getTriggerForSource(Context context, String source) {
        if (TIMER_SOURCE_DAILY.equals(source)) {
            return AppPrefs.isEnabled(context) ? AppPrefs.getNextTrigger(context) : 0L;
        }
        if (TIMER_SOURCE_ONE_SHOT.equals(source)) {
            return AppPrefs.getOneShotTrigger(context);
        }
        return 0L;
    }

    static String selectNextActiveTimerSource(long nowMillis, long dailyTrigger, long oneShotTrigger) {
        long daily = dailyTrigger > nowMillis ? dailyTrigger : 0L;
        long oneShot = oneShotTrigger > nowMillis ? oneShotTrigger : 0L;
        if (daily > 0L && (oneShot == 0L || daily <= oneShot)) {
            return TIMER_SOURCE_DAILY;
        }
        if (oneShot > 0L) {
            return TIMER_SOURCE_ONE_SHOT;
        }
        return TIMER_SOURCE_NONE;
    }

    static long selectNextActiveTimerTrigger(long nowMillis, long dailyTrigger, long oneShotTrigger) {
        String source = selectNextActiveTimerSource(nowMillis, dailyTrigger, oneShotTrigger);
        if (TIMER_SOURCE_DAILY.equals(source)) {
            return dailyTrigger;
        }
        if (TIMER_SOURCE_ONE_SHOT.equals(source)) {
            return oneShotTrigger;
        }
        return 0L;
    }

    static long calculateNextDailyTrigger(long nowMillis, int hour, int minute) {
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(nowMillis);

        Calendar next = Calendar.getInstance();
        next.setTimeInMillis(nowMillis);
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(now)) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }
        return next.getTimeInMillis();
    }

    static long calculateWarningTrigger(long nowMillis, long dailyTriggerMillis) {
        return Math.max(nowMillis + 1_000L, dailyTriggerMillis - DAILY_WARNING_LEAD_MS);
    }

    static long calculateExtendedTrigger(long currentTriggerMillis, int minutes) {
        return currentTriggerMillis + minutes * 60_000L;
    }

    private static long resolveNextDailyTrigger(Context context, long now) {
        long override = AppPrefs.getDailyOverrideTrigger(context);
        if (AppPrefs.isDailyOverrideForCurrentSchedule(context) && override > now + 1_000L) {
            return override;
        }

        if (override > 0L) {
            AppPrefs.clearDailyOverride(context);
        }
        return calculateNextDailyTrigger(
                now,
                AppPrefs.getHour(context),
                AppPrefs.getMinute(context)
        );
    }

    private static void ensureDailyScheduled(Context context) {
        if (!AppPrefs.isEnabled(context)) {
            cancelDaily(context);
            return;
        }

        long now = System.currentTimeMillis();
        long storedTrigger = AppPrefs.getNextTrigger(context);
        boolean exactChanged = AppPrefs.isNextTriggerExact(context) != canScheduleExactAlarms(context);
        boolean tooStale = storedTrigger > 0L && storedTrigger < now - DAILY_STALE_GRACE_MS;
        boolean alarmMissing = findDailyPendingIntent(context) == null;

        if (storedTrigger <= 0L || tooStale || exactChanged || alarmMissing) {
            scheduleDaily(context);
        }
    }

    private static void ensureOneShotScheduled(Context context) {
        long oneShot = AppPrefs.getOneShotTrigger(context);
        if (oneShot <= 0L) {
            cancelOneShotAlarmOnly(context);
            return;
        }

        long now = System.currentTimeMillis();
        if (oneShot < now - ONE_SHOT_STALE_GRACE_MS) {
            cancelOneShot(context);
            return;
        }

        boolean exactChanged = AppPrefs.isOneShotExact(context) != canScheduleExactAlarms(context);
        boolean alarmMissing = findOneShotPendingIntent(context) == null;
        if (exactChanged || alarmMissing) {
            scheduleOneShotAt(context, Math.max(oneShot, now + 1_000L));
        }
    }

    private static void scheduleDailyWarning(Context context, long dailyTriggerMillis) {
        AlarmManager alarmManager = getAlarmManager(context);
        if (alarmManager == null || dailyTriggerMillis <= System.currentTimeMillis()) {
            return;
        }

        long warningTrigger = calculateWarningTrigger(
                System.currentTimeMillis(),
                dailyTriggerMillis
        );
        scheduleAt(
                context,
                alarmManager,
                warningTrigger,
                getDailyWarningPendingIntent(context, dailyTriggerMillis)
        );
    }

    private static int scheduleAt(
            Context context,
            AlarmManager alarmManager,
            long triggerAtMillis,
            PendingIntent operation
    ) {
        if (canScheduleExactAlarms(context)) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        operation
                );
                return SCHEDULE_EXACT;
            } catch (SecurityException ignored) {
                // Special access can be revoked between capability check and scheduling.
            } catch (RuntimeException ignored) {
                return SCHEDULE_FAILED;
            }
        }

        try {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    operation
            );
            return SCHEDULE_APPROXIMATE;
        } catch (RuntimeException ignored) {
            return SCHEDULE_FAILED;
        }
    }

    private static AlarmManager getAlarmManager(Context context) {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    private static void cancelDailyWarningAlarm(Context context) {
        AlarmManager alarmManager = getAlarmManager(context);
        PendingIntent pendingIntent = findDailyWarningPendingIntent(context);
        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private static void cancelOneShotAlarmOnly(Context context) {
        AlarmManager alarmManager = getAlarmManager(context);
        if (alarmManager != null) {
            alarmManager.cancel(getOneShotPendingIntent(context));
        }
    }

    private static void cancelLegacyDailyAlarm(Context context, AlarmManager alarmManager) {
        PendingIntent legacy = findLegacyDailyPendingIntent(context);
        if (legacy != null) {
            alarmManager.cancel(legacy);
            legacy.cancel();
        }
    }

    private static PendingIntent findDailyPendingIntent(Context context) {
        return PendingIntent.getBroadcast(
                context,
                REQUEST_DAILY,
                dailyIntent(context),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent findDailyWarningPendingIntent(Context context) {
        return PendingIntent.getBroadcast(
                context,
                REQUEST_DAILY_WARNING,
                dailyWarningIntent(context, 0L),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent findOneShotPendingIntent(Context context) {
        return PendingIntent.getBroadcast(
                context,
                REQUEST_ONE_SHOT,
                oneShotIntent(context),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent findLegacyDailyPendingIntent(Context context) {
        Intent intent = new Intent(context, SleepActionReceiver.class)
                .setAction(LEGACY_ACTION_SLEEP)
                .setData(Uri.parse("chzzk-sleep-timer://sleep"));
        return PendingIntent.getBroadcast(
                context,
                REQUEST_DAILY,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent getDailyPendingIntent(Context context) {
        return PendingIntent.getBroadcast(
                context,
                REQUEST_DAILY,
                dailyIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent getDailyWarningPendingIntent(Context context, long expectedTrigger) {
        return PendingIntent.getBroadcast(
                context,
                REQUEST_DAILY_WARNING,
                dailyWarningIntent(context, expectedTrigger),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent getOneShotPendingIntent(Context context) {
        return PendingIntent.getBroadcast(
                context,
                REQUEST_ONE_SHOT,
                oneShotIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static Intent dailyIntent(Context context) {
        return new Intent(context, SleepActionReceiver.class)
                .setAction(ACTION_DAILY_SLEEP)
                .setData(Uri.parse("chzzk-sleep-timer://sleep/daily"));
    }

    private static Intent dailyWarningIntent(Context context, long expectedTrigger) {
        return new Intent(context, DailyWarningReceiver.class)
                .setAction(ACTION_DAILY_WARNING)
                .setData(Uri.parse("chzzk-sleep-timer://sleep/daily-warning"))
                .putExtra(EXTRA_EXPECTED_DAILY_TRIGGER, expectedTrigger);
    }

    private static Intent oneShotIntent(Context context) {
        return new Intent(context, SleepActionReceiver.class)
                .setAction(ACTION_ONE_SHOT_SLEEP)
                .setData(Uri.parse("chzzk-sleep-timer://sleep/once"));
    }
}
