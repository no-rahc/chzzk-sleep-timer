package com.norahc.sleeptimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public final class ExtensionControlActivity extends Activity {
    private static final int[] EXTENSION_MINUTES = {5, 20, 40};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long currentTrigger = AppPrefs.getNextTrigger(this);
        if (!AppPrefs.isEnabled(this) || currentTrigger <= System.currentTimeMillis()) {
            Toast.makeText(this, "활성화된 매일 타이머가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String[] choices = {
                "+5분  ·  " + AppPrefs.formatClockTime(currentTrigger + 5L * 60_000L),
                "+20분 ·  " + AppPrefs.formatClockTime(currentTrigger + 20L * 60_000L),
                "+40분 ·  " + AppPrefs.formatClockTime(currentTrigger + 40L * 60_000L)
        };

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("오늘만 연장")
                .setItems(choices, (ignored, which) -> {
                    if (which >= 0 && which < EXTENSION_MINUTES.length) {
                        DailyWarningReceiver.extend(this, EXTENSION_MINUTES[which]);
                    }
                    finish();
                })
                .setNegativeButton("취소", (ignored, which) -> finish())
                .create();
        dialog.setOnDismissListener(ignored -> finish());
        dialog.show();
        applyCompactDialog(dialog);
    }

    private void applyCompactDialog(AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.dimAmount = 0.12f;
        window.setAttributes(attributes);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setLayout(compactWidth(0.82f, 340), WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private int compactWidth(float fraction, int maxDp) {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        return Math.min(Math.round(screenWidth * fraction), dp(maxDp));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
