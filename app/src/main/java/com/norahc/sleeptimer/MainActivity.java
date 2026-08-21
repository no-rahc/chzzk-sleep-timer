package com.norahc.sleeptimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(11, 16, 32);
    private static final int SURFACE = Color.rgb(21, 29, 50);
    private static final int SURFACE_RAISED = Color.rgb(32, 43, 71);
    private static final int PRIMARY = Color.rgb(157, 174, 255);
    private static final int TEXT_PRIMARY = Color.rgb(246, 247, 251);
    private static final int TEXT_SECONDARY = Color.rgb(174, 184, 206);
    private static final int POSITIVE = Color.rgb(112, 224, 178);
    private static final int WARNING = Color.rgb(247, 198, 107);

    private TextView timeButton;
    private TextView scheduleSummary;
    private TextView lastRun;
    private TextView exactStatus;
    private TextView mediaStatus;
    private TextView screenStatus;
    private Switch scheduleSwitch;
    private boolean updatingSwitch;
    private int hour;
    private int minute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        hour = AppPrefs.getHour(this);
        minute = AppPrefs.getMinute(this);
        setContentView(buildContent());
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (scheduleSummary != null) {
            refresh();
        }
    }

    private View buildContent() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(20), dp(22), dp(28));
        scrollView.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView eyebrow = text("BEDTIME ROUTINE", 12, PRIMARY);
        eyebrow.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(eyebrow, margins(0, 0, 0, 7));

        TextView title = text("수면 타이머", 32, TEXT_PRIMARY);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title, margins(0, 0, 0, 6));

        TextView subtitle = text(
                "정해진 시각에 재생을 멈추고\n조용히 화면을 잠급니다.",
                16,
                TEXT_SECONDARY
        );
        subtitle.setLineSpacing(2f, 1.0f);
        root.addView(subtitle, margins(0, 0, 0, 22));

        LinearLayout scheduleCard = card();
        scheduleCard.addView(text("매일 실행 시각", 13, TEXT_SECONDARY), margins(0, 0, 0, 4));
        timeButton = button(AppPrefs.formatTime(hour, minute), true);
        timeButton.setTextSize(36);
        timeButton.setTypeface(null, android.graphics.Typeface.BOLD);
        timeButton.setGravity(Gravity.CENTER);
        timeButton.setContentDescription("매일 실행 시각 변경");
        timeButton.setOnClickListener(v -> showTimePicker());
        scheduleCard.addView(timeButton, margins(0, 0, 0, 16));

        LinearLayout switchRow = new LinearLayout(this);
        switchRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView switchCopy = text("매일 자동 실행", 17, TEXT_PRIMARY);
        switchRow.addView(switchCopy, new LinearLayout.LayoutParams(0, dp(52), 1f));
        scheduleSwitch = new Switch(this);
        scheduleSwitch.setText("");
        scheduleSwitch.setContentDescription("매일 자동 실행 켜기 또는 끄기");
        scheduleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updatingSwitch) {
                return;
            }
            AppPrefs.setEnabled(this, isChecked);
            if (isChecked) {
                AlarmScheduler.schedule(this);
                if (!AlarmScheduler.canScheduleExactAlarms(this)) {
                    Toast.makeText(
                            this,
                            "정확한 시각에 실행하려면 알람 및 리마인더 권한을 허용하세요.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            } else {
                AlarmScheduler.cancel(this);
            }
            refresh();
        });
        switchRow.addView(scheduleSwitch, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(52)
        ));
        scheduleCard.addView(switchRow);

        scheduleSummary = text("", 14, TEXT_SECONDARY);
        scheduleSummary.setLineSpacing(1f, 1.1f);
        scheduleCard.addView(scheduleSummary, margins(0, 10, 0, 0));
        root.addView(scheduleCard, margins(0, 0, 0, 12));

        LinearLayout testCard = card();
        TextView testTitle = text("동작 확인", 16, TEXT_PRIMARY);
        testTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        testCard.addView(testTitle, margins(0, 0, 0, 5));
        testCard.addView(text(
                "지금 재생 중인 미디어를 일시정지하고 음량을 0으로 만든 뒤 화면을 잠급니다.",
                14,
                TEXT_SECONDARY
        ), margins(0, 0, 0, 13));
        TextView testButton = button("지금 테스트", false);
        testButton.setOnClickListener(v -> confirmTest());
        testCard.addView(testButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
        ));
        root.addView(testCard, margins(0, 0, 0, 22));

        TextView permissionsTitle = text("필수 권한", 20, TEXT_PRIMARY);
        permissionsTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(permissionsTitle, margins(0, 0, 0, 10));

        LinearLayout permissionsCard = card();
        exactStatus = addPermissionRow(
                permissionsCard,
                "정확한 알람",
                "매일 설정한 시각에 실행",
                "설정",
                v -> openExactAlarmSettings()
        );
        addDivider(permissionsCard);
        mediaStatus = addPermissionRow(
                permissionsCard,
                "미디어 제어",
                "YouTube·CHZZK·브라우저에 일시정지 요청",
                "허용",
                v -> openNotificationSettings()
        );
        addDivider(permissionsCard);
        screenStatus = addPermissionRow(
                permissionsCard,
                "화면 잠금",
                "예약 시각에 화면 끄기",
                "허용",
                v -> openAccessibilitySettings()
        );
        root.addView(permissionsCard, margins(0, 0, 0, 22));

        LinearLayout noteCard = card();
        TextView noteTitle = text("작동 방식", 16, TEXT_PRIMARY);
        noteTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        noteCard.addView(noteTitle, margins(0, 0, 0, 7));
        noteCard.addView(text(
                "미디어 권한을 허용하면 Android가 노출한 활성 미디어 세션에 일시정지를 요청합니다. 앱이 미디어 세션을 제공하지 않는 경우 일시정지는 건너뛸 수 있지만, 음량 0과 화면 잠금은 별도로 시도합니다.\n\n화면 잠금 권한은 화면 내용을 읽거나 입력을 자동화하지 않고, 잠금 동작 하나만 호출하는 데 사용합니다.",
                13,
                TEXT_SECONDARY
        ));
        root.addView(noteCard, margins(0, 0, 0, 18));

        lastRun = text("", 12, TEXT_SECONDARY);
        lastRun.setGravity(Gravity.CENTER);
        root.addView(lastRun);

        return scrollView;
    }

    private TextView addPermissionRow(
            LinearLayout parent,
            String title,
            String detail,
            String buttonText,
            View.OnClickListener listener
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = text(title, 15, TEXT_PRIMARY);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        copy.addView(titleView);
        copy.addView(text(detail, 12, TEXT_SECONDARY), margins(0, 3, 0, 0));
        TextView status = text("확인 중", 12, WARNING);
        copy.addView(status, margins(0, 5, 0, 0));

        row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView action = button(buttonText, false);
        action.setTextSize(13);
        action.setOnClickListener(listener);
        row.addView(action, new LinearLayout.LayoutParams(dp(76), dp(44)));
        parent.addView(row, margins(0, 1, 0, 1));
        return status;
    }

    private void addDivider(LinearLayout parent) {
        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(47, 58, 87));
        parent.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                1
        ));
    }

    private void showTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    hour = selectedHour;
                    minute = selectedMinute;
                    AppPrefs.setTime(this, hour, minute);
                    if (AppPrefs.isEnabled(this)) {
                        AlarmScheduler.schedule(this);
                    }
                    refresh();
                },
                hour,
                minute,
                true
        );
        dialog.setTitle("매일 실행 시각");
        dialog.show();
    }

    private void confirmTest() {
        new AlertDialog.Builder(this)
                .setTitle("지금 실행할까요?")
                .setMessage("재생 중인 미디어를 일시정지하고 음량을 0으로 만든 뒤 화면을 잠급니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("실행", (dialog, which) -> {
                    SleepActionReceiver.runNow(this);
                    refresh();
                })
                .show();
    }

    private void openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Toast.makeText(this, "이 Android 버전에서는 별도 권한이 필요하지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            startActivity(new Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + getPackageName())
            ));
        } catch (Exception ignored) {
            Toast.makeText(this, "정확한 알람 설정을 열 수 없습니다. 시스템 설정에서 앱의 알람 권한을 찾아 허용하세요.", Toast.LENGTH_LONG).show();
        }
    }

    private void openNotificationSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        } catch (Exception ignored) {
            Toast.makeText(this, "알림 접근 설정을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception ignored) {
            Toast.makeText(this, "접근성 설정을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void refresh() {
        if (timeButton != null) {
            timeButton.setText(AppPrefs.formatTime(hour, minute));
        }

        boolean enabled = AppPrefs.isEnabled(this);
        boolean exact = AlarmScheduler.canScheduleExactAlarms(this);
        boolean media = MediaControlNotificationListenerService.isEnabled(this);
        boolean screen = ScreenLockAccessibilityService.isEnabled(this);

        updatingSwitch = true;
        scheduleSwitch.setChecked(enabled);
        updatingSwitch = false;

        if (enabled) {
            scheduleSummary.setText(exact
                    ? getString(R.string.schedule_exact, AppPrefs.formatTime(hour, minute))
                    : getString(R.string.schedule_approximate, AppPrefs.formatTime(hour, minute)));
        } else {
            scheduleSummary.setText(R.string.schedule_disabled);
        }

        setStatus(exactStatus, exact, exact ? "허용됨" : "허용 필요");
        setStatus(mediaStatus, media, media ? "허용됨" : "허용 필요");
        setStatus(screenStatus, screen, screen ? "허용됨" : "허용 필요");
        lastRun.setText(getString(R.string.last_run, AppPrefs.getLastRun(this)));
    }

    private void setStatus(TextView view, boolean good, String label) {
        view.setText(getString(good ? R.string.permission_allowed : R.string.permission_required, label));
        view.setTextColor(good ? POSITIVE : WARNING);
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(17), dp(16), dp(17), dp(16));
        card.setBackground(round(SURFACE, 20));
        return card;
    }

    private TextView button(String label, boolean prominent) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextColor(prominent ? BG : TEXT_PRIMARY);
        view.setTextSize(15);
        view.setGravity(Gravity.CENTER);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        view.setClickable(true);
        view.setFocusable(true);
        view.setPadding(dp(12), 0, dp(12), 0);
        view.setBackground(round(prominent ? PRIMARY : SURFACE_RAISED, 14));
        return view;
    }

    private TextView text(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
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
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
