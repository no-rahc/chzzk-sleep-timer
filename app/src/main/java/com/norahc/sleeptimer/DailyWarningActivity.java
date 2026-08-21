package com.norahc.sleeptimer;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
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
        setFinishOnTouchOutside(true);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(16), dp(18), dp(16));
        panel.setBackground(UiKit.roundedStroke(
                this,
                UiKit.SURFACE,
                22,
                UiKit.DIVIDER
        ));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView badge = UiKit.text(this, "10분 남음", 11, UiKit.WARNING);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(9), dp(5), dp(9), dp(5));
        badge.setBackground(UiKit.rounded(this, UiKit.SURFACE_RAISED, 12));
        top.addView(badge);
        panel.addView(top, margins(0, 0, 0, 10));

        TextView title = UiKit.text(this, "곧 재생이 종료됩니다", 19, UiKit.TEXT_PRIMARY);
        title.setTypeface(null, Typeface.BOLD);
        panel.addView(title);

        TextView detail = UiKit.text(
                this,
                "종료 예정 " + AppPrefs.formatClockTime(trigger) + " · 필요하면 오늘만 연장하세요",
                13,
                UiKit.TEXT_SECONDARY
        );
        panel.addView(detail, margins(0, 5, 0, 14));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.addView(extensionButton("+5분", 5, false), actionParams(0));
        actions.addView(extensionButton("+20분", 20, true), actionParams(dp(7)));
        actions.addView(extensionButton("+40분", 40, false), actionParams(dp(7)));
        panel.addView(actions);

        setContentView(panel);
        UiKit.applyFloatingWindow(
                this,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                460,
                16,
                72,
                0.05f
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        long trigger = AppPrefs.getNextTrigger(this);
        if (!AppPrefs.isEnabled(this) || trigger <= System.currentTimeMillis()) {
            finish();
        }
    }

    private Button extensionButton(String label, int minutes, boolean emphasized) {
        Button button = UiKit.chip(this, label, emphasized);
        button.setTextSize(13);
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
                dp(46),
                1f
        );
        params.setMarginStart(startMargin);
        return params;
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
