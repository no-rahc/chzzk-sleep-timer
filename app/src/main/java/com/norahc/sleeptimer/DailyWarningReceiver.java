package com.norahc.sleeptimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public final class DailyWarningReceiver extends BroadcastReceiver {
    static final String ACTION_EXTEND_DAILY = "com.norahc.sleeptimer.ACTION_EXTEND_DAILY";
    static final String ACTION_EXTEND_ACTIVE_TIMER = "com.norahc.sleeptimer.ACTION_EXTEND_ACTIVE_TIMER";
    static final String EXTRA_EXTENSION_MINUTES = "extension_minutes";
    static final String EXTRA_EXTENSION_TARGET = "extension_target";

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
            String effectiveSource = AlarmScheduler.getNextActiveTimerSource(appContext);
            long effectiveTrigger = AlarmScheduler.getNextActiveTimerTrigger(appContext);
            if (!AppPrefs.isEnabled(appContext)
                    || currentTrigger <= System.currentTimeMillis()
                    || expectedTrigger != currentTrigger
                    || !AlarmScheduler.TIMER_SOURCE_DAILY.equals(effectiveSource)
                    || effectiveTrigger != currentTrigger) {
                return;
            }
            DailyWarningNotifier.show(appContext, currentTrigger);
            return;
        }

        if (ACTION_EXTEND_DAILY.equals(action)) {
            int minutes = intent.getIntExtra(EXTRA_EXTENSION_MINUTES, 0);
            extend(appContext, minutes);
            return;
        }

        if (ACTION_EXTEND_ACTIVE_TIMER.equals(action)) {
            int minutes = intent.getIntExtra(EXTRA_EXTENSION_MINUTES, 0);
            String target = intent.getStringExtra(EXTRA_EXTENSION_TARGET);
            extendTarget(appContext, target, minutes);
        }
    }

    static long extend(Context context, int minutes) {
        long extendedTrigger = AlarmScheduler.extendCurrentDaily(context, minutes);
        showResult(context, AlarmScheduler.TIMER_SOURCE_DAILY, minutes, extendedTrigger);
        return extendedTrigger;
    }

    static long extendTarget(Context context, String requestedTarget, int minutes) {
        String currentTarget = AlarmScheduler.getNextActiveTimerSource(context);
        String target = requestedTarget;
        if (target == null
                || AlarmScheduler.TIMER_SOURCE_NONE.equals(target)
                || !target.equals(currentTarget)) {
            target = currentTarget;
        }

        long extendedTrigger;
        if (AlarmScheduler.TIMER_SOURCE_ONE_SHOT.equals(target)) {
            extendedTrigger = AlarmScheduler.extendCurrentOneShot(context, minutes);
        } else if (AlarmScheduler.TIMER_SOURCE_DAILY.equals(target)) {
            extendedTrigger = AlarmScheduler.extendCurrentDaily(context, minutes);
        } else {
            extendedTrigger = 0L;
        }

        showResult(context, target, minutes, extendedTrigger);
        return extendedTrigger;
    }

    private static void showResult(Context context, String target, int minutes, long extendedTrigger) {
        if (extendedTrigger > 0L) {
            String prefix = AlarmScheduler.TIMER_SOURCE_ONE_SHOT.equals(target)
                    ? "일회성 타이머를 "
                    : "오늘 종료 시간을 ";
            Toast.makeText(
                    context,
                    prefix + minutes + "분 연장했습니다. 새 종료 시각: "
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
    }
}
