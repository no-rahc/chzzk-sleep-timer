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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(22), dp(24), dp(20));

        TextView title = new TextView(this);
        title.setText("곧 재생이 종료됩니다.");
        title.setTextSize(21);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title, matchWrap());

        TextView detail = new TextView(this);
        detail.setText("현재 종료 예정 시각은 " + AppPrefs.formatClockTime(trigger)
                + "입니다.\n오늘만 종료 시간을 연장할 수 있습니다.");
        detail.setTextSize(15);
        detail.setLineSpacing(dp(2), 1.05f);
        LinearLayout.LayoutParams detailParams = matchWrap();
        detailParams.setMargins(0, dp(10), 0, dp(18));
        root.addView(detail, detailParams);

        root.addView(extensionButton("+5분 추가", 5), buttonParams());
        root.addView(extensionButton("+20분 추가", 20), buttonParams());
        root.addView(extensionButton("+40분 추가", 40), buttonParams());

        TextView hint = new TextView(this);
        hint.setText("연장해도 내일의 기본 종료 시각은 변경되지 않습니다.");
        hint.setTextSize(12);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintParams = matchWrap();
        hintParams.setMargins(0, dp(10), 0, 0);
        root.addView(hint, hintParams);

        setContentView(root);
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
        button.setTextSize(16);
        button.setOnClickListener(v -> {
            long result = DailyWarningReceiver.extend(this, minutes);
            if (result > 0L) {
                finish();
            }
        });
        return button;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
        );
        params.setMargins(0, 0, 0, dp(8));
        return params;
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
