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
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class ScreenLockAccessibilityService extends AccessibilityService {
    private static final long EXTRA_DIM_CLEAR_AFTER_LOCK_DELAY_MS = 750L;
    private static volatile ScreenLockAccessibilityService instance;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private View dimOverlay;
    private View controlOverlay;

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

    static boolean showBrightnessControlOverlay() {
        ScreenLockAccessibilityService service = instance;
        if (service == null) {
            return false;
        }
        service.mainHandler.post(service::showBrightnessOverlay);
        return true;
    }

    static boolean showExtensionControlOverlay(String requestedTarget, boolean warning) {
        ScreenLockAccessibilityService service = instance;
        if (service == null) {
            return false;
        }
        service.mainHandler.post(() -> service.showExtensionOverlay(requestedTarget, warning));
        return true;
    }

    static void dismissControlOverlay() {
        ScreenLockAccessibilityService service = instance;
        if (service != null) {
            service.mainHandler.post(service::removeControlOverlay);
        }
    }

    static void clearExtraDimNow(Context context) {
        AppPrefs.setExtraDimPercent(context.getApplicationContext(), 0);
        ScreenLockAccessibilityService service = instance;
        if (service != null) {
            service.applyExtraDim(0);
        }
    }

    static void clearExtraDimAfterSuccessfulLock(Context context) {
        AppPrefs.setExtraDimPercent(context.getApplicationContext(), 0);

        ScreenLockAccessibilityService service = instance;
        if (service == null) {
            return;
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (instance == service && AppPrefs.getExtraDimPercent(service) == 0) {
                service.applyExtraDim(0);
            }
        }, EXTRA_DIM_CLEAR_AFTER_LOCK_DELAY_MS);
    }

    private void showBrightnessOverlay() {
        if (!ensureWindowManager()) {
            return;
        }
        removeControlOverlay();
        ensureDimOverlayLayer();

        LinearLayout panel = controlPanel();
        addHandle(panel);

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        TextView title = UiKit.text(this, "추가 어둡게", 20, UiKit.TEXT_PRIMARY);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        titleBlock.addView(title);
        titleBlock.addView(
                UiKit.text(this, "보던 화면을 유지한 채 바로 조절합니다", 12, UiKit.TEXT_SECONDARY),
                margins(0, 3, 0, 0)
        );
        heading.addView(titleBlock, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        int currentPercent = AppPrefs.getExtraDimPercent(this);
        TextView value = UiKit.text(this, currentPercent + "%", 27, UiKit.PRIMARY);
        value.setTypeface(null, android.graphics.Typeface.BOLD);
        heading.addView(value);
        panel.addView(heading, margins(0, 0, 0, 14));

        SeekBar slider = new SeekBar(this);
        slider.setMax(AppPrefs.MAX_EXTRA_DIM_PERCENT);
        slider.setProgress(currentPercent);
        slider.setContentDescription("추가 화면 어둡기 조절");
        slider.setPadding(0, 0, 0, 0);
        UiKit.tintSeekBar(slider);
        panel.addView(slider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44)
        ));

        int[] presets = {0, 25, 50, 75};
        LinearLayout presetRow = new LinearLayout(this);
        presetRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < presets.length; i++) {
            int presetValue = presets[i];
            Button preset = UiKit.chip(
                    this,
                    presetValue == 0 ? "해제" : presetValue + "%",
                    false
            );
            preset.setContentDescription("추가 어둡기 " + presetValue + "% 적용");
            preset.setOnClickListener(v -> slider.setProgress(presetValue));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1f);
            if (i > 0) {
                params.setMarginStart(dp(6));
            }
            presetRow.addView(preset, params);
        }
        panel.addView(presetRow, margins(0, 9, 0, 0));

        TextView status = UiKit.text(
                this,
                currentPercent == 0 ? "추가 어둡기 해제됨" : "실시간 적용 중",
                12,
                UiKit.TEXT_SECONDARY
        );
        status.setGravity(Gravity.CENTER_HORIZONTAL);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        panel.addView(status, margins(0, 10, 0, 0));

        Button close = UiKit.button(this, "완료", true);
        close.setOnClickListener(v -> removeControlOverlay());
        panel.addView(close, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
        ));
        ((LinearLayout.LayoutParams) close.getLayoutParams()).setMargins(0, dp(13), 0, 0);

        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int safePercent = AppPrefs.clampExtraDimPercent(progress);
                AppPrefs.setExtraDimPercent(ScreenLockAccessibilityService.this, safePercent);
                applyExtraDim(safePercent);
                value.setText(safePercent + "%");
                status.setText(safePercent == 0 ? "추가 어둡기 해제됨" : "실시간 적용 중");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        attachControlOverlay(panel, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 520, 16);
    }

    private void showExtensionOverlay(String requestedTarget, boolean warning) {
        if (!ensureWindowManager()) {
            return;
        }

        String currentTarget = AlarmScheduler.getNextActiveTimerSource(this);
        String target = requestedTarget != null && requestedTarget.equals(currentTarget)
                ? requestedTarget
                : currentTarget;
        long currentTrigger = AlarmScheduler.getTriggerForSource(this, target);
        if (AlarmScheduler.TIMER_SOURCE_NONE.equals(target)
                || currentTrigger <= System.currentTimeMillis()) {
            Toast.makeText(this, "활성화된 타이머가 없습니다.", Toast.LENGTH_SHORT).show();
            removeControlOverlay();
            return;
        }

        removeControlOverlay();
        LinearLayout panel = controlPanel();
        addHandle(panel);

        String titleText;
        if (warning) {
            titleText = "곧 재생이 종료됩니다";
        } else if (AlarmScheduler.TIMER_SOURCE_ONE_SHOT.equals(target)) {
            titleText = "일회성 타이머 연장";
        } else {
            titleText = "오늘만 종료 연장";
        }

        TextView title = UiKit.text(this, titleText, 20, UiKit.TEXT_PRIMARY);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        panel.addView(title);

        String detail = warning
                ? AppPrefs.formatClockTime(currentTrigger) + " 종료 · 10분 남음"
                : "현재 종료 " + AppPrefs.formatClockTime(currentTrigger) + " · 이번 예약만 변경";
        panel.addView(
                UiKit.text(this, detail, 13, UiKit.TEXT_SECONDARY),
                margins(0, 5, 0, 13)
        );

        int[] extensionMinutes = {5, 20, 40};
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < extensionMinutes.length; i++) {
            int minutes = extensionMinutes[i];
            Button button = UiKit.chip(
                    this,
                    "+" + minutes + "분\n"
                            + AppPrefs.formatClockTime(currentTrigger + minutes * 60_000L),
                    false
            );
            button.setTextSize(13);
            button.setLines(2);
            button.setContentDescription(minutes + "분 연장");
            button.setOnClickListener(v -> {
                long result = DailyWarningReceiver.extendTarget(
                        ScreenLockAccessibilityService.this,
                        target,
                        minutes
                );
                if (result > 0L) {
                    DailyWarningNotifier.cancel(ScreenLockAccessibilityService.this);
                    removeControlOverlay();
                }
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(58), 1f);
            if (i > 0) {
                params.setMarginStart(dp(7));
            }
            actions.addView(button, params);
        }
        panel.addView(actions);

        Button close = UiKit.button(this, warning ? "그대로 둘게요" : "닫기", false);
        close.setOnClickListener(v -> removeControlOverlay());
        panel.addView(close, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44)
        ));
        ((LinearLayout.LayoutParams) close.getLayoutParams()).setMargins(0, dp(10), 0, 0);

        int gravity = warning
                ? Gravity.TOP | Gravity.CENTER_HORIZONTAL
                : Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        int yDp = warning ? 72 : 16;
        int maxWidthDp = warning ? 440 : 520;
        attachControlOverlay(panel, gravity, maxWidthDp, yDp);

        if (warning) {
            long delay = Math.max(1_000L, currentTrigger - System.currentTimeMillis());
            mainHandler.postDelayed(() -> {
                if (controlOverlay == panel) {
                    removeControlOverlay();
                }
            }, delay);
        }
    }

    private LinearLayout controlPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(10), dp(20), dp(18));
        panel.setBackground(UiKit.roundedStroke(
                this,
                UiKit.SURFACE,
                24,
                UiKit.DIVIDER
        ));
        return panel;
    }

    private void addHandle(LinearLayout panel) {
        LinearLayout handleRow = new LinearLayout(this);
        handleRow.setGravity(Gravity.CENTER);
        View handle = UiKit.dragHandle(this);
        handleRow.addView(handle, new LinearLayout.LayoutParams(dp(38), dp(4)));
        panel.addView(handleRow, margins(0, 0, 0, 14));
    }

    private void attachControlOverlay(View panel, int gravity, int maxWidthDp, int yDp) {
        if (windowManager == null) {
            return;
        }
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int width = Math.min(screenWidth - dp(32), dp(maxWidthDp));
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                Math.max(dp(280), width),
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = gravity;
        params.y = dp(yDp);
        try {
            windowManager.addView(panel, params);
            controlOverlay = panel;
        } catch (RuntimeException ignored) {
            controlOverlay = null;
        }
    }

    private boolean ensureWindowManager() {
        if (windowManager == null) {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }
        return windowManager != null;
    }

    private void ensureDimOverlayLayer() {
        if (dimOverlay != null || !ensureWindowManager()) {
            return;
        }
        createDimOverlay(AppPrefs.getExtraDimPercent(this) / 100f);
    }

    private void createDimOverlay(float alpha) {
        if (windowManager == null || dimOverlay != null) {
            return;
        }
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
        params.alpha = alpha;
        try {
            windowManager.addView(overlay, params);
            dimOverlay = overlay;
        } catch (RuntimeException ignored) {
            // The service can be disconnected while the overlay is being attached.
        }
    }

    private void applyExtraDim(int percent) {
        int safePercent = AppPrefs.clampExtraDimPercent(percent);
        if (safePercent <= 0) {
            if (controlOverlay != null && dimOverlay != null) {
                updateDimOverlayAlpha(0f);
            } else {
                removeDimOverlay();
            }
            return;
        }

        if (!ensureWindowManager()) {
            return;
        }

        if (dimOverlay == null) {
            createDimOverlay(safePercent / 100f);
            return;
        }
        updateDimOverlayAlpha(safePercent / 100f);
    }

    private void updateDimOverlayAlpha(float alpha) {
        if (dimOverlay == null || windowManager == null) {
            return;
        }
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) dimOverlay.getLayoutParams();
        params.alpha = alpha;
        try {
            windowManager.updateViewLayout(dimOverlay, params);
        } catch (RuntimeException ignored) {
            removeDimOverlay();
        }
    }

    private void removeControlOverlay() {
        if (controlOverlay == null || windowManager == null) {
            controlOverlay = null;
            if (AppPrefs.getExtraDimPercent(this) == 0) {
                removeDimOverlay();
            }
            return;
        }
        try {
            windowManager.removeView(controlOverlay);
        } catch (RuntimeException ignored) {
            // It may already have been detached by the system.
        } finally {
            controlOverlay = null;
            if (AppPrefs.getExtraDimPercent(this) == 0) {
                removeDimOverlay();
            }
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

    private LinearLayout.LayoutParams margins(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private int dp(int value) {
        return UiKit.dp(this, value);
    }

    private void clearInstance() {
        removeControlOverlay();
        removeDimOverlay();
        if (instance == this) {
            instance = null;
        }
    }
}
