package com.norahc.sleeptimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public final class BrightnessControlActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!ScreenLockAccessibilityService.isEnabled(this)) {
            showPermissionDialog();
            return;
        }
        showBrightnessDialog();
    }

    private void showBrightnessDialog() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(2), dp(16), 0);

        TextView value = new TextView(this);
        value.setTextSize(18);
        value.setGravity(Gravity.CENTER);
        content.addView(value, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        SeekBar slider = new SeekBar(this);
        slider.setMax(AppPrefs.MAX_EXTRA_DIM_PERCENT);
        slider.setProgress(AppPrefs.getExtraDimPercent(this));
        slider.setContentDescription("추가 화면 어둡기 조절");
        LinearLayout.LayoutParams sliderParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        sliderParams.setMargins(0, dp(7), 0, dp(2));
        content.addView(slider, sliderParams);

        TextView status = new TextView(this);
        status.setTextSize(11);
        status.setGravity(Gravity.CENTER);
        content.addView(status, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("추가 어둡게")
                .setView(content)
                .setNeutralButton("해제", null)
                .setNegativeButton("닫기", (ignored, which) -> finish())
                .create();

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
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
        };
        slider.setOnSeekBarChangeListener(listener);

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> slider.setProgress(0));
            applyDim(slider.getProgress(), value, status);
            applyCompactDialog(dialog, 0.86f, 360);
        });
        dialog.setOnDismissListener(ignored -> finish());
        dialog.show();
    }

    private void applyDim(int percent, TextView value, TextView status) {
        int safePercent = AppPrefs.clampExtraDimPercent(percent);
        value.setText(safePercent == 0
                ? "꺼짐"
                : safePercent + "% 더 어둡게");

        boolean applied = ScreenLockAccessibilityService.setExtraDimPercent(this, safePercent);
        if (applied) {
            status.setText(safePercent == 0 ? "필터 해제" : "즉시 적용");
        } else {
            status.setText("화면 제어 서비스 연결 대기");
        }
    }

    private void showPermissionDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("화면 제어 권한 필요")
                .setMessage("추가 어둡게 기능을 사용하려면 화면 제어 접근성을 켜주세요.")
                .setPositiveButton("접근성 설정", (ignored, which) -> {
                    try {
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    } catch (RuntimeException ignoredException) {
                        // The settings page may not exist on heavily customized Android builds.
                    }
                })
                .setNegativeButton("닫기", null)
                .create();
        dialog.setOnShowListener(ignored -> applyCompactDialog(dialog, 0.86f, 360));
        dialog.setOnDismissListener(ignored -> finish());
        dialog.show();
    }

    private void applyCompactDialog(AlertDialog dialog, float fraction, int maxDp) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.dimAmount = 0.12f;
        window.setAttributes(attributes);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setLayout(compactWidth(fraction, maxDp), WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private int compactWidth(float fraction, int maxDp) {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        return Math.min(Math.round(screenWidth * fraction), dp(maxDp));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
