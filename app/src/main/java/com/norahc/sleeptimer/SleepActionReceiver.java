package com.norahc.sleeptimer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;

import java.util.ArrayList;
import java.util.List;

public class SleepActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        Context appContext = context.getApplicationContext();
        String action = intent.getAction();

        if (AlarmScheduler.ACTION_DAILY_SLEEP.equals(action)) {
            if (!AppPrefs.isEnabled(appContext)) {
                AppPrefs.clearNextTrigger(appContext);
                return;
            }

            // Re-arm tomorrow before executing actions so one unexpected failure
            // cannot break the daily schedule chain.
            AlarmScheduler.scheduleDaily(appContext);
            runNow(appContext, "매일 예약");
            return;
        }

        if (AlarmScheduler.ACTION_ONE_SHOT_SLEEP.equals(action)) {
            if (!AlarmScheduler.isOneShotActive(appContext)) {
                return;
            }

            // Consume the one-shot first to guarantee at-most-once execution.
            AppPrefs.clearOneShotTrigger(appContext);
            runNow(appContext, "일회성 타이머");
        }
    }

    static void runNow(Context context) {
        runNow(context, "수동 테스트");
    }

    static void runNow(Context context, String source) {
        Context appContext = context.getApplicationContext();
        List<String> results = new ArrayList<>();

        if (AppPrefs.shouldPauseMedia(appContext)) {
            int paused = pauseActiveMedia(appContext);
            if (MediaControlNotificationListenerService.isEnabled(appContext)) {
                results.add(paused + "개 미디어 일시정지");
            } else {
                results.add("미디어 권한 없음");
            }
        }

        if (AppPrefs.shouldMuteVolume(appContext)) {
            results.add(muteMediaVolume(appContext) ? "음량 0" : "음량 변경 실패");
        }

        if (AppPrefs.shouldLockScreen(appContext)) {
            results.add(lockScreenSafely() ? "화면 잠금" : "화면 잠금 권한 없음");
        }

        if (results.isEmpty()) {
            results.add("실행 동작 없음");
        }

        AppPrefs.recordLastRun(appContext, source, joinResults(results));
    }

    private static int pauseActiveMedia(Context context) {
        if (!MediaControlNotificationListenerService.isEnabled(context)) {
            return 0;
        }

        MediaSessionManager sessionManager = (MediaSessionManager) context.getSystemService(
                Context.MEDIA_SESSION_SERVICE
        );
        if (sessionManager == null) {
            return 0;
        }

        ComponentName listener = new ComponentName(
                context,
                MediaControlNotificationListenerService.class
        );
        int paused = 0;
        try {
            List<MediaController> controllers = sessionManager.getActiveSessions(listener);
            for (MediaController controller : controllers) {
                try {
                    PlaybackState state = controller.getPlaybackState();
                    if (state == null
                            || state.getState() == PlaybackState.STATE_PLAYING
                            || state.getState() == PlaybackState.STATE_BUFFERING) {
                        controller.getTransportControls().pause();
                        paused++;
                    }
                } catch (RuntimeException ignored) {
                    // One uncooperative media session must not block the others.
                }
            }
        } catch (SecurityException ignored) {
            // Notification access can be revoked between the status check and this call.
        } catch (RuntimeException ignored) {
            // Vendor media-session implementations can fail independently of our app.
        }
        return paused;
    }

    private static boolean muteMediaVolume(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null || audioManager.isVolumeFixed()) {
            return false;
        }
        try {
            audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    0,
                    AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
            );
            return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean lockScreenSafely() {
        try {
            return ScreenLockAccessibilityService.lockScreen();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static String joinResults(List<String> results) {
        StringBuilder value = new StringBuilder();
        for (String result : results) {
            if (value.length() > 0) {
                value.append(" · ");
            }
            value.append(result);
        }
        return value.toString();
    }
}
