package com.norahc.sleeptimer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public final class BrightnessControlActivity extends Activity {
    private static final int[] PRESETS = {0, 25, 50, 75};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFinishOnTouchOutside(true);
        if (!ScreenLockAccessibilityService.isEnabled(this)) {
            showPermissionPanel();
            return;
        }
        showBrightnessPanel();
    }

    private void showBrightnessPanel() {
        LinearLayout panel = panel();
        addHandle(panel);

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        TextView title = UiKit.text(this, "추가 어둡게", 20, UiKit.TEXT_PRIMARY);
        title.setTypeface(null, Typeface.BOLD);
        titleBlock.addView(title);
        titleBlock.addView(
                UiKit.text(this, "기본 밝기 아래로 화면을 더 어둡게 합니다", 12, UiKit.TEXT_SECONDARY),
                margins(0, 3, 0, 0)
        );
        heading.addView(titleBlock, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView value = UiKit.text(this, "0%", 26, UiKit.PRIMARY);
        value.setTypeface(null, Typeface.BOLD);
        value.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        heading.addView(value);
        panel.addView(heading, margins(0, 0, 0, 16));

        SeekBar slider = new SeekBar(this);
        slider.setMax(AppPrefs.MAX_EXTRA_DIM_PERCENT);
        slider.setProgress(AppPrefs.getExtraDimPercent(this));
        slider.setContentDescription("추가 화면 어둡기 조절");
        slider.setPadding(0, 0, 0, 0);
        UiKit.tintSeekBar(slider);
        panel.addView(slider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44)
        ));

        LinearLayout presets = new LinearLayout(this);
        presets.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < PRESETS.length; i++) {
            int presetValue = PRESETS[i];
            Button preset = UiKit.chip(this, presetValue == 0 ? "해제" : presetValue + "%", false);
            preset.setContentDescription("추가 어둡기 " + presetValue + "% 적용");
            preset.setOnClickListener(v -> slider.setProgress(presetValue));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1f);
            if (i > 0) {
                params.setMarginStart(dp(6));
            }
            presets.addView(preset, params);
        }
        panel.addView(presets, margins(0, 10, 0, 0));

        TextView status = UiKit.text(this, "", 12, UiKit.TEXT_SECONDARY);
        status.setGravity(Gravity.CENTER_HORIZONTAL);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        panel.addView(status, margins(0, 12, 0, 0));

        Button done = UiKit.button(this, "완료", true);
        done.setOnClickListener(v -> finish());
        panel.addView(done, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
        ));
        ((LinearLayout.LayoutParams) done.getLayoutParams()).setMargins(0, dp(14), 0, 0);

        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                applyDim(progress, value, status);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        setContentView(panel);
        UiKit.applyFloatingWindow(
                this,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL,
                520,
                16,
                16,
                0.08f
        );
        applyDim(slider.getProgress(), value, status);
    }

    private void applyDim(int percent, TextView value, TextView status) {
        int safePercent = AppPrefs.clampExtraDimPercent(percent);
        value.setText(safePercent + "%");

        boolean applied = ScreenLockAccessibilityService.setExtraDimPercent(this, safePercent);
        if (applied) {
            status.setText(safePercent == 0 ? "추가 어둡기 해제됨" : "실시간 적용 중");
            status.setTextColor(UiKit.TEXT_SECONDARY);
        } else {
            status.setText("화면 제어 서비스 연결을 기다리는 중입니다");
            status.setTextColor(UiKit.WARNING);
        }
    }

    private void showPermissionPanel() {
        LinearLayout panel = panel();
        addHandle(panel);

        TextView title = UiKit.text(this, "화면 제어 권한 필요", 20, UiKit.TEXT_PRIMARY);
        title.setTypeface(null, Typeface.BOLD);
        panel.addView(title);
        panel.addView(
                UiKit.text(
                        this,
                        "추가 어둡게 기능은 화면 제어 접근성 서비스를 사용합니다.",
                        13,
                        UiKit.TEXT_SECONDARY
                ),
                margins(0, 7, 0, 16)
        );

        Button settings = UiKit.button(this, "접근성 설정 열기", true);
        settings.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } catch (RuntimeException ignored) {
                // Customized Android builds may not expose this settings page.
            }
            finish();
        });
        panel.addView(settings, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
        ));

        Button close = UiKit.button(this, "닫기", false);
        close.setOnClickListener(v -> finish());
        panel.addView(close, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
        ));
        ((LinearLayout.LayoutParams) close.getLayoutParams()).setMargins(0, dp(8), 0, 0);

        setContentView(panel);
        UiKit.applyFloatingWindow(
                this,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL,
                520,
                16,
                16,
                0.10f
        );
    }

    private LinearLayout panel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(10), dp(20), dp(20));
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
        panel.addView(handleRow, margins(0, 0, 0, 15));
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
}
