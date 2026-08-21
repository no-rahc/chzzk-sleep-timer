package com.norahc.sleeptimer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;

import java.util.List;

public class SleepActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, android.content.Intent intent) {
        if (intent == null || !AlarmScheduler.ACTION_SLEEP.equals(intent.getAction())) {
            return;
        }

        Context appContext = context.getApplicationContext();
        if (!AppPrefs.isEnabled(appContext)) {
            return;
        }

        runNow(appContext);
        AlarmScheduler.schedule(appContext);
    }

    static void runNow(Context context) {
        Context appContext = context.getApplicationContext();
        int paused = pauseActiveMedia(appContext);
        boolean muted = muteMediaVolume(appContext);
        boolean locked = ScreenLockAccessibilityService.lockScreen();

        String mediaResult = MediaControlNotificationListenerService.isEnabled(appContext)
                ? paused + "개 미디어 일시정지"
                : "미디어 권한 없음";
        String volumeResult = muted ? "음량 0" : "음량 변경 실패";
        String lockResult = locked ? "화면 잠금" : "화면 잠금 권한 없음";
        AppPrefs.recordLastRun(
                appContext,
                mediaResult + " · " + volumeResult + " · " + lockResult
        );
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
            // The user may have revoked notification access between the status check and call.
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
        } catch (SecurityException ignored) {
            return false;
        }
    }
}
