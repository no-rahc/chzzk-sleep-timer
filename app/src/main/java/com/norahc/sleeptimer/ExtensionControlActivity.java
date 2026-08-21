package com.norahc.sleeptimer;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class ExtensionControlActivity extends Activity {
    private static final int[] EXTENSION_MINUTES = {5, 20, 40};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFinishOnTouchOutside(true);

        String requestedTarget = getIntent() == null
                ? null
                : getIntent().getStringExtra(DailyWarningReceiver.EXTRA_EXTENSION_TARGET);
        String currentTarget = AlarmScheduler.getNextActiveTimerSource(this);
        String target = requestedTarget != null && requestedTarget.equals(currentTarget)
                ? requestedTarget
                : currentTarget;
        long currentTrigger = AlarmScheduler.getTriggerForSource(this, target);

        if (AlarmScheduler.TIMER_SOURCE_NONE.equals(target)
                || currentTrigger <= System.currentTimeMillis()) {
            Toast.makeText(this, "활성화된 타이머가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(10), dp(20), dp(20));
        panel.setBackground(UiKit.roundedStroke(
                this,
                UiKit.SURFACE,
                24,
                UiKit.DIVIDER
        ));

        LinearLayout handleRow = new LinearLayout(this);
        handleRow.setGravity(Gravity.CENTER);
        handleRow.addView(UiKit.dragHandle(this), new LinearLayout.LayoutParams(dp(38), dp(4)));
        panel.addView(handleRow, margins(0, 0, 0, 15));

        String titleText = AlarmScheduler.TIMER_SOURCE_ONE_SHOT.equals(target)
                ? "일회성 타이머 연장"
                : "오늘만 연장";
        TextView title = UiKit.text(this, titleText, 20, UiKit.TEXT_PRIMARY);
        title.setTypeface(null, Typeface.BOLD);
        panel.addView(title);

        TextView current = UiKit.text(
                this,
                "현재 종료 " + AppPrefs.formatClockTime(currentTrigger),
                13,
                UiKit.TEXT_SECONDARY
        );
        panel.addView(current, margins(0, 5, 0, 16));

        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < EXTENSION_MINUTES.length; i++) {
            int minutes = EXTENSION_MINUTES[i];
            String label = "+" + minutes + "분\n" + AppPrefs.formatClockTime(currentTrigger + minutes * 60_000L);
            Button choice = UiKit.chip(this, label, minutes == 20);
            choice.setTextSize(13);
            choice.setLines(2);
            choice.setContentDescription(minutes + "분 연장");
            choice.setOnClickListener(v -> {
                DailyWarningReceiver.extendTarget(this, target, minutes);
                finish();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(58), 1f);
            if (i > 0) {
                params.setMarginStart(dp(7));
            }
            choices.addView(choice, params);
        }
        panel.addView(choices);

        Button cancel = UiKit.button(this, "취소", false);
        cancel.setOnClickListener(v -> finish());
        panel.addView(cancel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
        ));
        ((LinearLayout.LayoutParams) cancel.getLayoutParams()).setMargins(0, dp(12), 0, 0);

        setContentView(panel);
        UiKit.applyFloatingWindow(
                this,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL,
                520,
                16,
                16,
                0.08f
        );
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
