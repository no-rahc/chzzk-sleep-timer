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
    private static final String KEY_LAST_RUN = "last_run";
    private static final String KEY_LAST_RESULT = "last_result";
    private static final String KEY_NEXT_TRIGGER = "next_trigger";
    private static final String KEY_NEXT_TRIGGER_EXACT = "next_trigger_exact";

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
                .apply();
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
                .apply();
    }

    static void recordLastRun(Context context, String result) {
        get(context).edit()
                .putLong(KEY_LAST_RUN, System.currentTimeMillis())
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
        String result = get(context).getString(KEY_LAST_RESULT, "");
        return result.isEmpty() ? when : when + " · " + result;
    }

    static String formatTime(int hour, int minute) {
        return String.format(Locale.KOREA, "%02d:%02d", hour, minute);
    }

    static String formatDateTime(long timestamp) {
        return new SimpleDateFormat("M월 d일 (E) HH:mm", Locale.KOREA)
                .format(new Date(timestamp));
    }
}
