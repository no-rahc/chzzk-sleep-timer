package com.norahc.sleeptimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public final class DailyWarningReceiver extends BroadcastReceiver {
    static final String ACTION_EXTEND_DAILY = "com.norahc.sleeptimer.ACTION_EXTEND_DAILY";
    static final String EXTRA_EXTENSION_MINUTES = "extension_minutes";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        Context appContext = context.getApplicationContext();
        String action = intent.getAction();

        if (AlarmScheduler.ACTION_DAILY_WARNING.equals(action)) {
            long expectedTrigger = intent.getLongExtra(
                    AlarmScheduler.EXTRA_EXPECTED_DAILY_TRIGGER,
                    0L
            );
            long currentTrigger = AppPrefs.getNextTrigger(appContext);
            if (!AppPrefs.isEnabled(appContext)
                    || currentTrigger <= System.currentTimeMillis()
                    || expectedTrigger != currentTrigger) {
                return;
            }
            DailyWarningNotifier.show(appContext, currentTrigger);
            return;
        }

        if (ACTION_EXTEND_DAILY.equals(action)) {
            int minutes = intent.getIntExtra(EXTRA_EXTENSION_MINUTES, 0);
            extend(appContext, minutes);
        }
    }

    static long extend(Context context, int minutes) {
        long extendedTrigger = AlarmScheduler.extendCurrentDaily(context, minutes);
        if (extendedTrigger > 0L) {
            Toast.makeText(
                    context,
                    "오늘 종료 시간을 " + minutes + "분 연장했습니다. 새 종료 시각: "
                            + AppPrefs.formatClockTime(extendedTrigger),
                    Toast.LENGTH_LONG
            ).show();
        } else {
            Toast.makeText(
                    context,
                    "타이머를 연장할 수 없습니다. 예약 상태를 확인하세요.",
                    Toast.LENGTH_LONG
            ).show();
        }
        return extendedTrigger;
    }
}
