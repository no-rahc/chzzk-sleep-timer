package com.norahc.sleeptimer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

final class DailyWarningNotifier {
    private static final String CHANNEL_ID = "daily_timer_warning_v1";
    private static final String CHANNEL_NAME = "종료 10분 전 알림";
    private static final int NOTIFICATION_ID = 5301;
    private static final int REQUEST_SHOW_EXTENSION = 5302;
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

        Intent showExtensionIntent = new Intent(appContext, DailyWarningReceiver.class)
                .setAction(DailyWarningReceiver.ACTION_SHOW_EXTENSION_CONTROL)
                .setData(android.net.Uri.parse("chzzk-sleep-timer://warning/extend"))
                .putExtra(
                        DailyWarningReceiver.EXTRA_EXTENSION_TARGET,
                        AlarmScheduler.TIMER_SOURCE_DAILY
                );
        PendingIntent showExtension = PendingIntent.getBroadcast(
                appContext,
                REQUEST_SHOW_EXTENSION,
                showExtensionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long remaining = Math.max(1_000L, triggerAtMillis - System.currentTimeMillis());
        Notification notification = new Notification.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer_notification)
                .setContentTitle("10분 후 " + AppPrefs.formatClockTime(triggerAtMillis) + " 종료")
                .setContentText("+5 / +20 / +40분 연장할 수 있습니다.")
                .setSubText("수면 타이머")
                .setWhen(triggerAtMillis)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_MAX)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setTimeoutAfter(remaining)
                .setContentIntent(showExtension)
                .addAction(0, "+5분 추가", extensionIntent(appContext, 5, REQUEST_EXTEND_5))
                .addAction(0, "+20분 추가", extensionIntent(appContext, 20, REQUEST_EXTEND_20))
                .addAction(0, "+40분 추가", extensionIntent(appContext, 40, REQUEST_EXTEND_40))
                .build();

        try {
            manager.notify(NOTIFICATION_ID, notification);
        } catch (SecurityException ignored) {
            // Notification permission can change at runtime.
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

    private static void createChannel(NotificationManager manager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("매일 종료 예정 시각 10분 전에 현재 화면 위 연장 패널과 알림을 표시합니다.");
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(channel);
    }
}
