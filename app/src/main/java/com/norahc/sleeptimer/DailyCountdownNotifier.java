package com.norahc.sleeptimer;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

final class DailyCountdownNotifier {
    static final int NOTIFICATION_PERMISSION_REQUEST = 5203;

    private static final String CHANNEL_ID = "daily_timer_countdown";
    private static final String CHANNEL_NAME = "매일 수면 타이머";
    private static final int NOTIFICATION_ID = 5201;
    private static final int REQUEST_OPEN_APP = 5202;
    private static final int REQUEST_BRIGHTNESS = 5204;

    private static boolean permissionRequestInFlight;

    private DailyCountdownNotifier() {
    }

    static boolean hasPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    static void requestPermissionIfNeeded(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || hasPermission(context)
                || !(context instanceof Activity)
                || permissionRequestInFlight) {
            return;
        }

        permissionRequestInFlight = true;
        try {
            Intent intent = new Intent(context, NotificationPermissionActivity.class);
            context.startActivity(intent);
        } catch (RuntimeException ignored) {
            permissionRequestInFlight = false;
        }
    }

    static void permissionRequestFinished() {
        permissionRequestInFlight = false;
    }

    static void sync(Context context) {
        Context appContext = context.getApplicationContext();
        if (!AppPrefs.isEnabled(appContext)) {
            cancel(appContext);
            return;
        }

        long triggerAtMillis = AppPrefs.getNextTrigger(appContext);
        if (triggerAtMillis > 0L) {
            show(appContext, triggerAtMillis);
        }
    }

    static void show(Context context, long triggerAtMillis) {
        Context appContext = context.getApplicationContext();
        if (triggerAtMillis <= System.currentTimeMillis() || !hasPermission(appContext)) {
            return;
        }

        NotificationManager manager = (NotificationManager) appContext.getSystemService(
                Context.NOTIFICATION_SERVICE
        );
        if (manager == null) {
            return;
        }

        createChannel(manager);

        Intent openIntent = new Intent(appContext, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openApp = PendingIntent.getActivity(
                appContext,
                REQUEST_OPEN_APP,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent brightnessIntent = new Intent(appContext, BrightnessControlActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent brightnessControl = PendingIntent.getActivity(
                appContext,
                REQUEST_BRIGHTNESS,
                brightnessIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String baseTime = AppPrefs.formatTime(
                AppPrefs.getHour(appContext),
                AppPrefs.getMinute(appContext)
        );
        String subText = AlarmScheduler.isDailyOverrideActive(appContext)
                ? "오늘만 " + AppPrefs.formatClockTime(triggerAtMillis) + " · 기본 " + baseTime
                : "매일 " + baseTime;

        Notification notification = new Notification.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer_notification)
                .setContentTitle("타이머가 작동 중입니다.")
                .setContentText("꺼지기까지 남은 시간")
                .setSubText(subText)
                .setWhen(triggerAtMillis)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(openApp)
                .addAction(R.drawable.ic_brightness_notification, "밝기", brightnessControl)
                .build();

        try {
            manager.notify(NOTIFICATION_ID, notification);
        } catch (SecurityException ignored) {
            // Notification permission can be revoked between the permission check and notify().
        }
    }

    static void cancel(Context context) {
        NotificationManager manager = (NotificationManager) context.getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(NOTIFICATION_ID);
        }
    }

    private static void createChannel(NotificationManager manager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("매일 예약된 수면 타이머의 남은 시간을 표시합니다.");
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(channel);
    }
}
