package com.norahc.sleeptimer;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

public final class NotificationPermissionActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            DailyCountdownNotifier.permissionRequestFinished();
            DailyCountdownNotifier.sync(this);
            finish();
            return;
        }

        requestPermissions(
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                DailyCountdownNotifier.NOTIFICATION_PERMISSION_REQUEST
        );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == DailyCountdownNotifier.NOTIFICATION_PERMISSION_REQUEST) {
            DailyCountdownNotifier.permissionRequestFinished();
            DailyCountdownNotifier.sync(this);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        DailyCountdownNotifier.permissionRequestFinished();
        super.onDestroy();
    }
}
