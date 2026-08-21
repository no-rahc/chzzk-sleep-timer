package com.norahc.sleeptimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.widget.Toast;

public final class DailyWarningReceiver extends BroadcastReceiver {
    static final String ACTION_EXTEND_DAILY = "com.norahc.sleeptimer.ACTION_EXTEND_DAILY";
    static final String ACTION_EXTEND_ACTIVE_TIMER = "com.norahc.sleeptimer.ACTION_EXTEND_ACTIVE_TIMER";
    static final String ACTION_SHOW_BRIGHTNESS_CONTROL = "com.norahc.sleeptimer.ACTION_SHOW_BRIGHTNESS_CONTROL";
    static final String ACTION_SHOW_EXTENSION_CONTROL = "com.norahc.sleeptimer.ACTION_SHOW_EXTENSION_CONTROL";
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

            if (isScreenInteractive(appContext)) {
                ScreenLockAccessibilityService.showExtensionControlOverlay(
                        AlarmScheduler.TIMER_SOURCE_DAILY,
                        true
                );
            }
            DailyWarningNotifier.show(appContext, currentTrigger);
            return;
        }

        if (ACTION_SHOW_BRIGHTNESS_CONTROL.equals(action)) {
            if (!ScreenLockAccessibilityService.showBrightnessControlOverlay()) {
                openBrightnessFallback(appContext);
            }
            return;
        }

        if (ACTION_SHOW_EXTENSION_CONTROL.equals(action)) {
            String target = intent.getStringExtra(EXTRA_EXTENSION_TARGET);
            if (!ScreenLockAccessibilityService.showExtensionControlOverlay(target, false)) {
                openExtensionFallback(appContext, target);
            }
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
        if (extendedTrigger > 0L) {
            ScreenLockAccessibilityService.dismissControlOverlay();
        }
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
        if (extendedTrigger > 0L) {
            ScreenLockAccessibilityService.dismissControlOverlay();
        }
        return extendedTrigger;
    }

    private static void openBrightnessFallback(Context context) {
        Intent fallback = new Intent(context, BrightnessControlActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try {
            context.startActivity(fallback);
        } catch (RuntimeException ignored) {
            Toast.makeText(
                    context,
                    "화면 제어 접근성 권한을 확인하세요.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private static void openExtensionFallback(Context context, String target) {
        Intent fallback = new Intent(context, ExtensionControlActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_EXTENSION_TARGET, target);
        try {
            context.startActivity(fallback);
        } catch (RuntimeException ignored) {
            Toast.makeText(
                    context,
                    "연장 화면을 열 수 없습니다.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private static boolean isScreenInteractive(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager == null) {
            return true;
        }
        try {
            return powerManager.isInteractive();
        } catch (RuntimeException ignored) {
            return true;
        }
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
