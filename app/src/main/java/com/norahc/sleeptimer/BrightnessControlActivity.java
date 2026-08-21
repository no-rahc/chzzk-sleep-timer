package com.norahc.sleeptimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
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
        content.setPadding(dp(24), dp(8), dp(24), dp(4));

        TextView value = new TextView(this);
        value.setTextSize(22);
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
        sliderParams.setMargins(0, dp(14), 0, dp(8));
        content.addView(slider, sliderParams);

        TextView hint = new TextView(this);
        hint.setText("시스템 밝기는 그대로 두고 검은 화면 필터를 추가해 최저 밝기보다 더 어둡게 만듭니다.");
        hint.setTextSize(13);
        content.addView(hint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView status = new TextView(this);
        status.setTextSize(12);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        statusParams.setMargins(0, dp(12), 0, 0);
        content.addView(status, statusParams);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("화면 밝기")
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
        });
        dialog.setOnDismissListener(ignored -> finish());
        dialog.show();
    }

    private void applyDim(int percent, TextView value, TextView status) {
        int safePercent = AppPrefs.clampExtraDimPercent(percent);
        value.setText(safePercent == 0
                ? "추가 어둡게 꺼짐"
                : "추가 어둡게 " + safePercent + "%");

        boolean applied = ScreenLockAccessibilityService.setExtraDimPercent(this, safePercent);
        if (applied) {
            status.setText(safePercent == 0
                    ? "화면 필터가 해제되었습니다."
                    : "움직이는 즉시 화면에 적용됩니다.");
        } else {
            status.setText("화면 제어 서비스가 연결되면 저장된 값이 자동 적용됩니다.");
        }
    }

    private void showPermissionDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("화면 제어 권한 필요")
                .setMessage("추가 어둡게 기능은 화면 잠금과 같은 접근성 서비스를 사용합니다. 화면 내용을 읽거나 터치를 가로채지 않습니다.")
                .setPositiveButton("접근성 설정", (ignored, which) -> {
                    try {
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    } catch (RuntimeException ignoredException) {
                        // The settings page may not exist on heavily customized Android builds.
                    }
                })
                .setNegativeButton("닫기", null)
                .create();
        dialog.setOnDismissListener(ignored -> finish());
        dialog.show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
