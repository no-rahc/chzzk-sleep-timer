package com.norahc.sleeptimer;

import android.service.notification.NotificationListenerService;
import android.provider.Settings;
import android.content.ComponentName;
import android.content.Context;

public class MediaControlNotificationListenerService extends NotificationListenerService {
    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
    }

    static boolean isEnabled(Context context) {
        String enabledListeners = Settings.Secure.getString(
                context.getContentResolver(),
                "enabled_notification_listeners"
        );
        if (enabledListeners == null || enabledListeners.isEmpty()) {
            return false;
        }

        ComponentName target = new ComponentName(
                context,
                MediaControlNotificationListenerService.class
        );
        for (String flattened : enabledListeners.split(":")) {
            ComponentName enabled = ComponentName.unflattenFromString(flattened);
            if (target.equals(enabled)) {
                return true;
            }
        }
        return false;
    }
}
