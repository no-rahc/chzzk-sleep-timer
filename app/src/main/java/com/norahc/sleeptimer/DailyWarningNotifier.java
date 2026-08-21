package com.norahc.sleeptimer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

final class DailyWarningNotifier {
    private static final String CHANNEL_ID = "daily_timer_warning_v1";
    private static final String CHANNEL_NAME = "종료 10분 전 알림";
    private static final int NOTIFICATION_ID = 5301;
    private static final int REQUEST_WARNING_SCREEN = 5302;
    private static final int REQUEST_EXTEND_5 = 5305;
    private static final int REQUEST_EXTEND_20 = 5320;
    private static final int REQUEST_EXTEND_40 = 5340;

    private DailyWarningNotifier() {
    }

    static void show(Context context, long triggerAtMillis) {
        Context appContext = context.getApplicationContext();
        if (!DailyCountdownNotifier.hasPermission(appContext)
                || triggerAtMillis <= System.currentTimeMillis()) {
            return;
        }

        NotificationManager manager = (NotificationManager) appContext.getSystemService(
                Context.NOTIFICATION_SERVICE
        );
        if (manager == null) {
            return;
        }

        createChannel(manager);

        PendingIntent warningScreen = PendingIntent.getActivity(
                appContext,
                REQUEST_WARNING_SCREEN,
                new Intent(appContext, DailyWarningActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long remaining = Math.max(1_000L, triggerAtMillis - System.currentTimeMillis());
        Notification.Builder builder = new Notification.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer_notification)
                .setContentTitle("곧 재생이 종료됩니다.")
                .setContentText("종료 예정 " + AppPrefs.formatClockTime(triggerAtMillis)
                        + " · 필요한 만큼 오늘만 연장할 수 있습니다.")
                .setSubText("수면 타이머")
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_MAX)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setTimeoutAfter(remaining)
                .setContentIntent(warningScreen)
                .addAction(0, "+5분 추가", extensionIntent(appContext, 5, REQUEST_EXTEND_5))
                .addAction(0, "+20분 추가", extensionIntent(appContext, 20, REQUEST_EXTEND_20))
                .addAction(0, "+40분 추가", extensionIntent(appContext, 40, REQUEST_EXTEND_40));

        // Never wake a display that is already off just to show the 10-minute warning.
        // When the phone is already in use, Android may surface this as a heads-up popup.
        if (isScreenInteractive(appContext) && canUseFullScreenIntent(manager)) {
            builder.setFullScreenIntent(warningScreen, true);
        }

        try {
            manager.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException ignored) {
            // Notification or full-screen-intent permission can change at runtime.
        }
    }

    static void cancel(Context context) {
        NotificationManager manager = (NotificationManager) context.getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(NOTIFICATION_ID);
        }
    }

    private static PendingIntent extensionIntent(Context context, int minutes, int requestCode) {
        Intent intent = new Intent(context, DailyWarningReceiver.class)
                .setAction(DailyWarningReceiver.ACTION_EXTEND_DAILY)
                .setData(android.net.Uri.parse("chzzk-sleep-timer://sleep/extend/" + minutes))
                .putExtra(DailyWarningReceiver.EXTRA_EXTENSION_MINUTES, minutes);
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static boolean canUseFullScreenIntent(NotificationManager manager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return true;
        }
        try {
            return manager.canUseFullScreenIntent();
        } catch (RuntimeException ignored) {
            return false;
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

    private static void createChannel(NotificationManager manager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("매일 종료 예정 시각 10분 전에 연장 버튼이 있는 팝업을 표시합니다.");
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(channel);
    }
}
