package com.norahc.sleeptimer;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class AppPrefs {
    private static final String PREFS = "sleep_timer_preferences";

    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_HOUR = "hour";
    private static final String KEY_MINUTE = "minute";

    private static final String KEY_NEXT_TRIGGER = "next_trigger";
    private static final String KEY_NEXT_TRIGGER_EXACT = "next_trigger_exact";
    private static final String KEY_ONE_SHOT_TRIGGER = "one_shot_trigger";
    private static final String KEY_ONE_SHOT_EXACT = "one_shot_exact";

    private static final String KEY_PAUSE_MEDIA = "pause_media";
    private static final String KEY_MUTE_VOLUME = "mute_volume";
    private static final String KEY_LOCK_SCREEN = "lock_screen";

    private static final String KEY_LAST_RUN = "last_run";
    private static final String KEY_LAST_SOURCE = "last_source";
    private static final String KEY_LAST_RESULT = "last_result";

    private AppPrefs() {
    }

    private static SharedPreferences get(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static boolean isEnabled(Context context) {
        return get(context).getBoolean(KEY_ENABLED, false);
    }

    static void setEnabled(Context context, boolean enabled) {
        get(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    static int getHour(Context context) {
        return get(context).getInt(KEY_HOUR, 23);
    }

    static int getMinute(Context context) {
        return get(context).getInt(KEY_MINUTE, 30);
    }

    static void setTime(Context context, int hour, int minute) {
        get(context).edit()
                .putInt(KEY_HOUR, hour)
                .putInt(KEY_MINUTE, minute)
                .apply();
    }

    static void setNextTrigger(Context context, long timestamp, boolean exact) {
        get(context).edit()
                .putLong(KEY_NEXT_TRIGGER, timestamp)
                .putBoolean(KEY_NEXT_TRIGGER_EXACT, exact)
                .commit();
    }

    static long getNextTrigger(Context context) {
        return get(context).getLong(KEY_NEXT_TRIGGER, 0L);
    }

    static boolean isNextTriggerExact(Context context) {
        return get(context).getBoolean(KEY_NEXT_TRIGGER_EXACT, false);
    }

    static void clearNextTrigger(Context context) {
        get(context).edit()
                .remove(KEY_NEXT_TRIGGER)
                .remove(KEY_NEXT_TRIGGER_EXACT)
                .commit();
    }

    static void setOneShotTrigger(Context context, long timestamp, boolean exact) {
        get(context).edit()
                .putLong(KEY_ONE_SHOT_TRIGGER, timestamp)
                .putBoolean(KEY_ONE_SHOT_EXACT, exact)
                .commit();
    }

    static long getOneShotTrigger(Context context) {
        return get(context).getLong(KEY_ONE_SHOT_TRIGGER, 0L);
    }

    static boolean isOneShotExact(Context context) {
        return get(context).getBoolean(KEY_ONE_SHOT_EXACT, false);
    }

    static void clearOneShotTrigger(Context context) {
        get(context).edit()
                .remove(KEY_ONE_SHOT_TRIGGER)
                .remove(KEY_ONE_SHOT_EXACT)
                .commit();
    }

    static boolean shouldPauseMedia(Context context) {
        return get(context).getBoolean(KEY_PAUSE_MEDIA, true);
    }

    static void setPauseMedia(Context context, boolean enabled) {
        get(context).edit().putBoolean(KEY_PAUSE_MEDIA, enabled).apply();
    }

    static boolean shouldMuteVolume(Context context) {
        return get(context).getBoolean(KEY_MUTE_VOLUME, true);
    }

    static void setMuteVolume(Context context, boolean enabled) {
        get(context).edit().putBoolean(KEY_MUTE_VOLUME, enabled).apply();
    }

    static boolean shouldLockScreen(Context context) {
        return get(context).getBoolean(KEY_LOCK_SCREEN, true);
    }

    static void setLockScreen(Context context, boolean enabled) {
        get(context).edit().putBoolean(KEY_LOCK_SCREEN, enabled).apply();
    }

    static boolean hasAnyActionEnabled(Context context) {
        return shouldPauseMedia(context)
                || shouldMuteVolume(context)
                || shouldLockScreen(context);
    }

    static void recordLastRun(Context context, String source, String result) {
        get(context).edit()
                .putLong(KEY_LAST_RUN, System.currentTimeMillis())
                .putString(KEY_LAST_SOURCE, source)
                .putString(KEY_LAST_RESULT, result)
                .apply();
    }

    static String getLastRun(Context context) {
        long timestamp = get(context).getLong(KEY_LAST_RUN, 0L);
        if (timestamp == 0L) {
            return "아직 실행 기록이 없습니다.";
        }

        String when = new SimpleDateFormat("M월 d일 HH:mm", Locale.KOREA)
                .format(new Date(timestamp));
        String source = get(context).getString(KEY_LAST_SOURCE, "");
        String result = get(context).getString(KEY_LAST_RESULT, "");

        StringBuilder value = new StringBuilder(when);
        if (!source.isEmpty()) {
            value.append(" · ").append(source);
        }
        if (!result.isEmpty()) {
            value.append(" · ").append(result);
        }
        return value.toString();
    }

    static String formatTime(int hour, int minute) {
        return String.format(Locale.KOREA, "%02d:%02d", hour, minute);
    }

    static String formatDateTime(long timestamp) {
        return new SimpleDateFormat("M월 d일 (E) HH:mm", Locale.KOREA)
                .format(new Date(timestamp));
    }
}
