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
    static final String ACTION_ONE_SHOT_SLEEP = "com.norahc.sleeptimer.ACTION_ONE_SHOT_SLEEP";

    private static final String LEGACY_ACTION_SLEEP = "com.norahc.sleeptimer.ACTION_SLEEP";
    private static final int REQUEST_DAILY = 401;
    private static final int REQUEST_ONE_SHOT = 402;
    private static final int SCHEDULE_FAILED = 0;
    private static final int SCHEDULE_APPROXIMATE = 1;
    private static final int SCHEDULE_EXACT = 2;
    private static final long DAILY_STALE_GRACE_MS = 30L * 60L * 1000L;
    private static final long ONE_SHOT_STALE_GRACE_MS = 30L * 60L * 1000L;

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

        if (!AppPrefs.isEnabled(appContext)) {
            AppPrefs.clearNextTrigger(appContext);
            DailyCountdownNotifier.cancel(appContext);
            return false;
        }

        long nextTrigger = calculateNextDailyTrigger(
                System.currentTimeMillis(),
                AppPrefs.getHour(appContext),
                AppPrefs.getMinute(appContext)
        );
        int result = scheduleAt(appContext, alarmManager, nextTrigger, operation);
        if (result == SCHEDULE_FAILED) {
            AppPrefs.clearNextTrigger(appContext);
            DailyCountdownNotifier.cancel(appContext);
            return false;
        }

        AppPrefs.setNextTrigger(appContext, nextTrigger, result == SCHEDULE_EXACT);
        DailyCountdownNotifier.show(appContext, nextTrigger);
        DailyCountdownNotifier.requestPermissionIfNeeded(context);
        return true;
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
            return false;
        }

        PendingIntent operation = getOneShotPendingIntent(appContext);
        alarmManager.cancel(operation);

        int result = scheduleAt(appContext, alarmManager, triggerAtMillis, operation);
        if (result == SCHEDULE_FAILED) {
            AppPrefs.clearOneShotTrigger(appContext);
            return false;
        }

        AppPrefs.setOneShotTrigger(appContext, triggerAtMillis, result == SCHEDULE_EXACT);
        return true;
    }

    static void cancelDaily(Context context) {
        Context appContext = context.getApplicationContext();
        AlarmManager alarmManager = getAlarmManager(appContext);
        if (alarmManager != null) {
            alarmManager.cancel(getDailyPendingIntent(appContext));
            cancelLegacyDailyAlarm(appContext, alarmManager);
        }
        AppPrefs.clearNextTrigger(appContext);
        DailyCountdownNotifier.cancel(appContext);
    }

    static void cancelOneShot(Context context) {
        Context appContext = context.getApplicationContext();
        cancelOneShotAlarmOnly(appContext);
        AppPrefs.clearOneShotTrigger(appContext);
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

    private static Intent oneShotIntent(Context context) {
        return new Intent(context, SleepActionReceiver.class)
                .setAction(ACTION_ONE_SHOT_SLEEP)
                .setData(Uri.parse("chzzk-sleep-timer://sleep/once"));
    }
}
