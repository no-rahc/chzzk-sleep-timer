package com.norahc.sleeptimer;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class DailyWarningActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long trigger = AppPrefs.getNextTrigger(this);
        if (!AppPrefs.isEnabled(this) || trigger <= System.currentTimeMillis()) {
            finish();
            return;
        }

        setShowWhenLocked(true);
        setTurnScreenOn(true);
        setFinishOnTouchOutside(true);

        Window window = getWindow();
        window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DIM_BEHIND
        );

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(12));

        TextView title = new TextView(this);
        title.setText("곧 재생이 종료됩니다.");
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title, matchWrap());

        TextView detail = new TextView(this);
        detail.setText("종료 예정 " + AppPrefs.formatClockTime(trigger) + " · 오늘만 연장");
        detail.setTextSize(13);
        LinearLayout.LayoutParams detailParams = matchWrap();
        detailParams.setMargins(0, dp(5), 0, dp(10));
        root.addView(detail, detailParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.addView(extensionButton("+5분", 5), actionParams(0));
        actions.addView(extensionButton("+20분", 20), actionParams(dp(6)));
        actions.addView(extensionButton("+40분", 40), actionParams(dp(6)));
        root.addView(actions, matchWrap());

        setContentView(root);
        applyCompactWindow(window);
    }

    @Override
    protected void onResume() {
        super.onResume();
        long trigger = AppPrefs.getNextTrigger(this);
        if (!AppPrefs.isEnabled(this) || trigger <= System.currentTimeMillis()) {
            finish();
        }
    }

    private Button extensionButton(String label, int minutes) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setPadding(dp(4), 0, dp(4), 0);
        button.setContentDescription("오늘 종료 시간을 " + minutes + "분 연장");
        button.setOnClickListener(v -> {
            long result = DailyWarningReceiver.extend(this, minutes);
            if (result > 0L) {
                finish();
            }
        });
        return button;
    }

    private LinearLayout.LayoutParams actionParams(int startMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                dp(44),
                1f
        );
        params.setMarginStart(startMargin);
        return params;
    }

    private void applyCompactWindow(Window window) {
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        attributes.y = dp(72);
        attributes.dimAmount = 0.12f;
        window.setAttributes(attributes);
        window.setLayout(compactWidth(0.88f, 380), WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private int compactWidth(float fraction, int maxDp) {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        return Math.min(Math.round(screenWidth * fraction), dp(maxDp));
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
