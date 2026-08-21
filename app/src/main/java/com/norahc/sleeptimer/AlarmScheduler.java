package com.norahc.sleeptimer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import java.util.Calendar;

final class AlarmScheduler {
    static final String ACTION_SLEEP = "com.norahc.sleeptimer.ACTION_SLEEP";
    private static final int REQUEST_CODE = 401;

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

    static void schedule(Context context) {
        Context appContext = context.getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent operation = getPendingIntent(appContext);
        alarmManager.cancel(operation);

        if (!AppPrefs.isEnabled(appContext)) {
            return;
        }

        Calendar now = Calendar.getInstance();
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, AppPrefs.getHour(appContext));
        next.set(Calendar.MINUTE, AppPrefs.getMinute(appContext));
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(now)) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (canScheduleExactAlarms(appContext)) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        next.getTimeInMillis(),
                        operation
                );
                return;
            } catch (SecurityException ignored) {
                // The user may revoke special access between the check and this call.
            }
        }

        // The fallback keeps the timer alive, but Android may deliver it late.
        alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                next.getTimeInMillis(),
                operation
        );
    }

    static void cancel(Context context) {
        Context appContext = context.getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(getPendingIntent(appContext));
        }
    }

    private static PendingIntent getPendingIntent(Context context) {
        Intent intent = new Intent(context, SleepActionReceiver.class)
                .setAction(ACTION_SLEEP)
                .setData(Uri.parse("chzzk-sleep-timer://sleep"));
        return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
