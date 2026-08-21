package com.norahc.sleeptimer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public final class FadeOutService extends Service {
    private static final String CHANNEL_ID = "sleep_fade_out";
    private static final String CHANNEL_NAME = "종료 전 볼륨 페이드";
    private static final int NOTIFICATION_ID = 5401;
    private static final long TICK_MS = 1_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private String source;
    private long targetAtMillis;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (source == null
                    || !AppPrefs.shouldMuteVolume(FadeOutService.this)
                    || !FadeOutManager.isExpectedActiveSession(
                    FadeOutService.this,
                    source,
                    targetAtMillis
            )) {
                abortFadeAndRestore();
                return;
            }

            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int originalVolume = FadeOutManager.getOriginalVolume(FadeOutService.this);
            if (audioManager == null || audioManager.isVolumeFixed() || originalVolume < 0) {
                abortFadeAndRestore();
                return;
            }

            long remaining = targetAtMillis - System.currentTimeMillis();
            int targetVolume = FadeOutManager.calculateVolume(originalVolume, remaining);
            try {
                int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                if (targetVolume < current) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0);
                }
            } catch (RuntimeException ignored) {
                abortFadeAndRestore();
                return;
            }

            if (remaining <= 0L) {
                // Keep the now-muted volume in place for the sleep action, but clear
                // the active fade state so a delayed/approximate sleep alarm cannot
                // leave this session marked active forever.
                FadeOutManager.consumeCurrentWithoutRestore(FadeOutService.this);
                stopSelf();
                return;
            }
            handler.postDelayed(this, TICK_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            abortFadeAndRestore();
            return START_NOT_STICKY;
        }

        source = intent.getStringExtra(FadeOutManager.EXTRA_SOURCE);
        targetAtMillis = intent.getLongExtra(FadeOutManager.EXTRA_TARGET, 0L);
        if (source == null) {
            abortFadeAndRestore();
            return START_NOT_STICKY;
        }
        if (targetAtMillis <= System.currentTimeMillis()) {
            FadeOutManager.consumeCurrentWithoutRestore(this);
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, buildNotification());
        handler.removeCallbacks(tick);
        handler.post(tick);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tick);
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void abortFadeAndRestore() {
        FadeOutManager.cancelAndRestore(this);
        stopSelf();
    }

    private Notification buildNotification() {
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer_notification)
                .setContentTitle("곧 재생이 종료됩니다.")
                .setContentText("볼륨을 천천히 낮추는 중")
                .setWhen(targetAtMillis)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    private void createChannel() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("타이머 종료 직전 30초 동안 미디어 볼륨을 부드럽게 낮춥니다.");
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.setShowBadge(false);
        manager.createNotificationChannel(channel);
    }
}
