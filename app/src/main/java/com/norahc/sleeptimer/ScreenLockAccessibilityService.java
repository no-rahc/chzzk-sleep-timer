package com.norahc.sleeptimer;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

public class ScreenLockAccessibilityService extends AccessibilityService {
    private static volatile ScreenLockAccessibilityService instance;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // This service only needs the global lock-screen action.
    }

    @Override
    public void onInterrupt() {
        // No ongoing gesture or spoken feedback to interrupt.
    }

    @Override
    public boolean onUnbind(Intent intent) {
        clearInstance();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        clearInstance();
        super.onDestroy();
    }

    static boolean isEnabled(Context context) {
        AccessibilityManager manager = (AccessibilityManager) context.getSystemService(
                Context.ACCESSIBILITY_SERVICE
        );
        if (manager == null) {
            return false;
        }

        List<AccessibilityServiceInfo> enabledServices = manager
                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        ComponentName target = new ComponentName(context, ScreenLockAccessibilityService.class);
        for (AccessibilityServiceInfo serviceInfo : enabledServices) {
            if (serviceInfo.getResolveInfo() != null
                    && serviceInfo.getResolveInfo().serviceInfo != null) {
                ComponentName enabled = new ComponentName(
                        serviceInfo.getResolveInfo().serviceInfo.packageName,
                        serviceInfo.getResolveInfo().serviceInfo.name
                );
                if (target.equals(enabled)) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean lockScreen() {
        ScreenLockAccessibilityService service = instance;
        if (service == null) {
            return false;
        }
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN);
    }

    private void clearInstance() {
        if (instance == this) {
            instance = null;
        }
    }
}
