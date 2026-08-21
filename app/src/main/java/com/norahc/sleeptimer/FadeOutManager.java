package com.norahc.sleeptimer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;

final class FadeOutManager {
    static final String SOURCE_DAILY = "daily";
    static final String SOURCE_ONE_SHOT = "one_shot";
    static final long FADE_DURATION_MS = 30_000L;

    static final String EXTRA_SOURCE = "fade_source";
    static final String EXTRA_TARGET = "fade_target";

    private static final String ACTION_START_FADE = "com.norahc.sleeptimer.ACTION_START_FADE";
    private static final String PREFS = "fade_out_state";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_TARGET = "target";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_ORIGINAL_VOLUME = "original_volume";
    private static final int REQUEST_START_FADE = 6101;

    private FadeOutManager() {
    }

    static void sync(Context context) {
        Context appContext = context.getApplicationContext();
        Target target = resolveTarget(appContext);
        if (!AppPrefs.shouldMuteVolume(appContext) || target == null) {
            cancelAndRestore(appContext);
            return;
        }

        SharedPreferences prefs = prefs(appContext);
        String storedSource = prefs.getString(KEY_SOURCE, "");
        long storedTarget = prefs.getLong(KEY_TARGET, 0L);
        boolean active = prefs.getBoolean(KEY_ACTIVE, false);

        if (target.source.equals(storedSource) && target.atMillis == storedTarget) {
            if (!active) {
                scheduleOrStart(appContext, target);
            }
            return;
        }

        cancelAndRestore(appContext);
        prefs(appContext).edit()
                .putString(KEY_SOURCE, target.source)
                .putLong(KEY_TARGET, target.atMillis)
                .putBoolean(KEY_ACTIVE, false)
                .putInt(KEY_ORIGINAL_VOLUME, -1)
                .commit();
        scheduleOrStart(appContext, target);
    }

    static void startExpectedFade(Context context, String source, long targetAtMillis) {
        Context appContext = context.getApplicationContext();
        Target current = resolveTarget(appContext);
        if (!AppPrefs.shouldMuteVolume(appContext)
                || current == null
                || !current.source.equals(source)
                || current.atMillis != targetAtMillis
                || targetAtMillis <= System.currentTimeMillis()) {
            sync(appContext);
            return;
        }

        AudioManager audioManager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null || audioManager.isVolumeFixed()) {
            return;
        }

        SharedPreferences prefs = prefs(appContext);
        int originalVolume = prefs.getInt(KEY_ORIGINAL_VOLUME, -1);
        if (originalVolume < 0) {
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
        prefs.edit()
                .putString(KEY_SOURCE, source)
                .putLong(KEY_TARGET, targetAtMillis)
                .putBoolean(KEY_ACTIVE, true)
                .putInt(KEY_ORIGINAL_VOLUME, originalVolume)
                .commit();

        Intent serviceIntent = new Intent(appContext, FadeOutService.class)
                .putExtra(EXTRA_SOURCE, source)
                .putExtra(EXTRA_TARGET, targetAtMillis);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(serviceIntent);
            } else {
                appContext.startService(serviceIntent);
            }
        } catch (RuntimeException ignored) {
            cancelAndRestore(appContext);
        }
    }

    static boolean isExpectedActiveSession(Context context, String source, long targetAtMillis) {
        SharedPreferences prefs = prefs(context);
        return prefs.getBoolean(KEY_ACTIVE, false)
                && targetAtMillis == prefs.getLong(KEY_TARGET, 0L)
                && source.equals(prefs.getString(KEY_SOURCE, ""));
    }

    static int getOriginalVolume(Context context) {
        return prefs(context).getInt(KEY_ORIGINAL_VOLUME, -1);
    }

    static void consumeCurrentWithoutRestore(Context context) {
        Context appContext = context.getApplicationContext();
        cancelStartAlarm(appContext);
        clearState(appContext);
        stopFadeService(appContext);
    }

    static void cancelAndRestore(Context context) {
        Context appContext = context.getApplicationContext();
        cancelStartAlarm(appContext);

        int originalVolume = prefs(appContext).getInt(KEY_ORIGINAL_VOLUME, -1);
        if (originalVolume >= 0) {
            restoreVolume(appContext, originalVolume);
        }

        clearState(appContext);
        stopFadeService(appContext);
    }

    static int calculateVolume(int originalVolume, long remainingMillis) {
        if (originalVolume <= 0 || remainingMillis <= 0L) {
            return 0;
        }
        if (remainingMillis >= FADE_DURATION_MS) {
            return originalVolume;
        }
        float ratio = remainingMillis / (float) FADE_DURATION_MS;
        return Math.max(0, Math.min(originalVolume, Math.round(originalVolume * ratio)));
    }

    private static void scheduleOrStart(Context context, Target target) {
        long now = System.currentTimeMillis();
        long startAt = target.atMillis - FADE_DURATION_MS;
        if (startAt <= now + 1_000L) {
            startExpectedFade(context, target.source, target.atMillis);
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent operation = startPendingIntent(context, target.source, target.atMillis);
        alarmManager.cancel(operation);
        try {
            if (AlarmScheduler.canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        startAt,
                        operation
                );
            } else {
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        startAt,
                        operation
                );
            }
        } catch (RuntimeException ignored) {
            // The normal sleep alarm still runs even when the optional fade cannot be scheduled.
        }
    }

    private static Target resolveTarget(Context context) {
        long now = System.currentTimeMillis();
        long daily = AppPrefs.isEnabled(context) ? AppPrefs.getNextTrigger(context) : 0L;
        long oneShot = AppPrefs.getOneShotTrigger(context);
        if (daily <= now) {
            daily = 0L;
        }
        if (oneShot <= now) {
            oneShot = 0L;
        }

        if (daily == 0L && oneShot == 0L) {
            return null;
        }
        if (daily > 0L && (oneShot == 0L || daily <= oneShot)) {
            return new Target(SOURCE_DAILY, daily);
        }
        return new Target(SOURCE_ONE_SHOT, oneShot);
    }

    private static PendingIntent startPendingIntent(Context context, String source, long targetAtMillis) {
        Intent intent = new Intent(context, FadeOutStartReceiver.class)
                .setAction(ACTION_START_FADE)
                .setData(Uri.parse("chzzk-sleep-timer://fade/start"))
                .putExtra(EXTRA_SOURCE, source)
                .putExtra(EXTRA_TARGET, targetAtMillis);
        return PendingIntent.getBroadcast(
                context,
                REQUEST_START_FADE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static void cancelStartAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent operation = PendingIntent.getBroadcast(
                context,
                REQUEST_START_FADE,
                new Intent(context, FadeOutStartReceiver.class)
                        .setAction(ACTION_START_FADE)
                        .setData(Uri.parse("chzzk-sleep-timer://fade/start")),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (alarmManager != null && operation != null) {
            alarmManager.cancel(operation);
            operation.cancel();
        }
    }

    private static void restoreVolume(Context context, int volume) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null || audioManager.isVolumeFixed()) {
            return;
        }
        try {
            int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    Math.max(0, Math.min(max, volume)),
                    0
            );
        } catch (RuntimeException ignored) {
            // Volume restoration is best-effort.
        }
    }

    private static void stopFadeService(Context context) {
        try {
            context.stopService(new Intent(context, FadeOutService.class));
        } catch (RuntimeException ignored) {
            // The service may already be stopped.
        }
    }

    private static void clearState(Context context) {
        prefs(context).edit().clear().commit();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static final class Target {
        final String source;
        final long atMillis;

        Target(String source, long atMillis) {
            this.source = source;
            this.atMillis = atMillis;
        }
    }
}
