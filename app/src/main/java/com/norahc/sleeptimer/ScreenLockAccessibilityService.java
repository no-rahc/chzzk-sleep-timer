package com.norahc.sleeptimer;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

public class ScreenLockAccessibilityService extends AccessibilityService {
    private static final long EXTRA_DIM_CLEAR_AFTER_LOCK_DELAY_MS = 750L;
    private static volatile ScreenLockAccessibilityService instance;

    private WindowManager windowManager;
    private View dimOverlay;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        applyExtraDim(AppPrefs.getExtraDimPercent(this));
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // This service does not inspect accessibility events or window content.
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

    static boolean isConnected() {
        return instance != null;
    }

    static boolean lockScreen() {
        ScreenLockAccessibilityService service = instance;
        if (service == null) {
            return false;
        }
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN);
    }

    static boolean setExtraDimPercent(Context context, int percent) {
        int safePercent = AppPrefs.clampExtraDimPercent(percent);
        AppPrefs.setExtraDimPercent(context, safePercent);

        ScreenLockAccessibilityService service = instance;
        if (service == null) {
            return false;
        }
        service.applyExtraDim(safePercent);
        return true;
    }

    static void clearExtraDimAfterSuccessfulLock(Context context) {
        AppPrefs.setExtraDimPercent(context.getApplicationContext(), 0);

        ScreenLockAccessibilityService service = instance;
        if (service == null) {
            return;
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (instance == service) {
                service.applyExtraDim(0);
            }
        }, EXTRA_DIM_CLEAR_AFTER_LOCK_DELAY_MS);
    }

    private void applyExtraDim(int percent) {
        int safePercent = AppPrefs.clampExtraDimPercent(percent);
        if (safePercent <= 0) {
            removeDimOverlay();
            return;
        }

        if (windowManager == null) {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }
        if (windowManager == null) {
            return;
        }

        if (dimOverlay == null) {
            View overlay = new View(this);
            overlay.setBackgroundColor(Color.BLACK);
            overlay.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.TOP | Gravity.START;
            params.alpha = safePercent / 100f;

            try {
                windowManager.addView(overlay, params);
                dimOverlay = overlay;
            } catch (RuntimeException ignored) {
                // The service can be disconnected while the overlay is being attached.
            }
            return;
        }

        WindowManager.LayoutParams params = (WindowManager.LayoutParams) dimOverlay.getLayoutParams();
        params.alpha = safePercent / 100f;
        try {
            windowManager.updateViewLayout(dimOverlay, params);
        } catch (RuntimeException ignored) {
            removeDimOverlay();
        }
    }

    private void removeDimOverlay() {
        if (dimOverlay == null || windowManager == null) {
            dimOverlay = null;
            return;
        }
        try {
            windowManager.removeView(dimOverlay);
        } catch (RuntimeException ignored) {
            // It may already have been detached by the system.
        } finally {
            dimOverlay = null;
        }
    }

    private void clearInstance() {
        removeDimOverlay();
        if (instance == this) {
            instance = null;
        }
    }
}
