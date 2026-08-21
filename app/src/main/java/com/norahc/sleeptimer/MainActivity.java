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
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(11, 16, 32);
    private static final int SURFACE = Color.rgb(21, 29, 50);
    private static final int SURFACE_RAISED = Color.rgb(32, 43, 71);
    private static final int PRIMARY = Color.rgb(157, 174, 255);
    private static final int TEXT_PRIMARY = Color.rgb(246, 247, 251);
    private static final int TEXT_SECONDARY = Color.rgb(174, 184, 206);
    private static final int POSITIVE = Color.rgb(112, 224, 178);
    private static final int WARNING = Color.rgb(247, 198, 107);
    private static final int NEUTRAL = Color.rgb(150, 161, 184);

    private static final int ACTION_PAUSE_MEDIA = 1;
    private static final int ACTION_MUTE_VOLUME = 2;
    private static final int ACTION_LOCK_SCREEN = 3;

    private Button timeButton;
    private Button oneShotCancelButton;
    private TextView appStatus;
    private TextView dailySummary;
    private TextView oneShotSummary;
    private TextView exactStatus;
    private TextView mediaStatus;
    private TextView screenStatus;
    private LinearLayout mediaPermissionRow;
    private LinearLayout screenPermissionRow;
    private Switch dailySwitch;
    private Switch pauseMediaSwitch;
    private Switch muteVolumeSwitch;
    private Switch lockScreenSwitch;
    private boolean updatingSwitches;
    private int hour;
    private int minute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().setNavigationBarDividerColor(BG);
        }

        hour = AppPrefs.getHour(this);
        minute = AppPrefs.getMinute(this);
        setContentView(buildContent());
        AlarmScheduler.ensureScheduled(this);
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AlarmScheduler.ensureScheduled(this);
        if (dailySummary != null) {
            refresh();
        }
    }

    private View buildContent() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BG);
        applySystemBarInsets(scrollView);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(28));
        scrollView.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        root.addView(buildHeader(), margins(0, 0, 0, 18));
        root.addView(buildDailyCard(), margins(0, 0, 0, 12));
        root.addView(buildQuickTimerCard(), margins(0, 0, 0, 12));
        root.addView(buildActionsCard(), margins(0, 0, 0, 12));
        root.addView(buildPermissionsCard(), margins(0, 0, 0, 12));
        root.addView(buildTestButton());

        return scrollView;
    }

    private View buildHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = text("수면 타이머", 29, TEXT_PRIMARY);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        appStatus = text("대기", 12, NEUTRAL);
        appStatus.setGravity(Gravity.CENTER);
        appStatus.setTypeface(null, android.graphics.Typeface.BOLD);
        appStatus.setPadding(dp(12), dp(7), dp(12), dp(7));
        appStatus.setBackground(round(SURFACE_RAISED, 16));
        appStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        row.addView(appStatus);
        return row;
    }

    private View buildDailyCard() {
        LinearLayout card = card();

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("매일 종료", 17, TEXT_PRIMARY);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        dailySwitch = new Switch(this);
        dailySwitch.setId(View.generateViewId());
        dailySwitch.setContentDescription("매일 자동 종료 켜기 또는 끄기");
        title.setLabelFor(dailySwitch.getId());
        top.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1f));
        top.addView(dailySwitch, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(48)
        ));
        card.addView(top, margins(0, 0, 0, 7));

        timeButton = button(AppPrefs.formatTime(hour, minute), true);
        timeButton.setTextSize(35);
        timeButton.setContentDescription("매일 종료 시각 변경");
        timeButton.setOnClickListener(v -> showTimePicker());
        card.addView(timeButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(64)
        ));

        dailySummary = text("", 13, TEXT_SECONDARY);
        dailySummary.setGravity(Gravity.CENTER_HORIZONTAL);
        dailySummary.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        card.addView(dailySummary, margins(0, 11, 0, 0));

        dailySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updatingSwitches) {
                return;
            }
            AppPrefs.setEnabled(this, isChecked);
            if (isChecked) {
                boolean scheduled = AlarmScheduler.scheduleDaily(this);
                if (!scheduled) {
                    AppPrefs.setEnabled(this, false);
                    Toast.makeText(this, "예약을 만들 수 없습니다.", Toast.LENGTH_LONG).show();
                } else if (!AlarmScheduler.canScheduleExactAlarms(this)) {
                    Toast.makeText(this, "정확한 알람 권한이 없어 근사 시각으로 예약됩니다.", Toast.LENGTH_LONG).show();
                }
            } else {
                AlarmScheduler.cancelDaily(this);
            }
            refresh();
        });
        return card;
    }

    private View buildQuickTimerCard() {
        LinearLayout card = card();
        TextView title = text("일회성 타이머", 17, TEXT_PRIMARY);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(title, margins(0, 0, 0, 12));

        LinearLayout presets = new LinearLayout(this);
        presets.setOrientation(LinearLayout.HORIZONTAL);
        int[] minutes = {15, 30, 60, 90};
        for (int i = 0; i < minutes.length; i++) {
            int value = minutes[i];
            Button preset = button(value + "분", false);
            preset.setTextSize(14);
            preset.setContentDescription(value + "분 후 일회성 타이머 설정");
            preset.setOnClickListener(v -> startQuickTimer(value));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(46), 1f);
            if (i > 0) {
                params.setMarginStart(dp(4));
            }
            if (i < minutes.length - 1) {
                params.setMarginEnd(dp(4));
            }
            presets.addView(preset, params);
        }
        card.addView(presets);

        Button custom = button("직접 입력", false);
        custom.setContentDescription("일회성 타이머 시간을 직접 입력");
        custom.setOnClickListener(v -> showCustomTimerDialog());
        card.addView(custom, margins(0, 8, 0, 0));
        custom.getLayoutParams().height = dp(44);

        oneShotSummary = text("", 13, TEXT_SECONDARY);
        oneShotSummary.setGravity(Gravity.CENTER_HORIZONTAL);
        oneShotSummary.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        card.addView(oneShotSummary, margins(0, 12, 0, 7));

        oneShotCancelButton = button("타이머 취소", false);
        oneShotCancelButton.setOnClickListener(v -> {
            AlarmScheduler.cancelOneShot(this);
            Toast.makeText(this, "일회성 타이머를 취소했습니다.", Toast.LENGTH_SHORT).show();
            refresh();
        });
        card.addView(oneShotCancelButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(42)
        ));
        return card;
    }

    private View buildActionsCard() {
        LinearLayout card = card();
        TextView title = text("종료할 때", 17, TEXT_PRIMARY);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(title, margins(0, 0, 0, 5));

        pauseMediaSwitch = addActionRow(card, "재생 일시정지");
        pauseMediaSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                handleActionToggle(ACTION_PAUSE_MEDIA, pauseMediaSwitch, isChecked));

        addDivider(card);
        muteVolumeSwitch = addActionRow(card, "미디어 음량 0");
        muteVolumeSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                handleActionToggle(ACTION_MUTE_VOLUME, muteVolumeSwitch, isChecked));

        addDivider(card);
        lockScreenSwitch = addActionRow(card, "화면 잠금");
        lockScreenSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                handleActionToggle(ACTION_LOCK_SCREEN, lockScreenSwitch, isChecked));
        return card;
    }

    private View buildPermissionsCard() {
        LinearLayout card = card();
        TextView title = text("권한", 17, TEXT_PRIMARY);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(title, margins(0, 0, 0, 5));

        exactStatus = permissionStatus();
        card.addView(permissionRow("정확한 알람", exactStatus, v -> openExactAlarmSettings()));
        addDivider(card);

        mediaStatus = permissionStatus();
        mediaPermissionRow = permissionRow("미디어 제어", mediaStatus, v -> openNotificationSettings());
        card.addView(mediaPermissionRow);
        addDivider(card);

        screenStatus = permissionStatus();
        screenPermissionRow = permissionRow("화면 잠금", screenStatus, v -> openAccessibilitySettings());
        card.addView(screenPermissionRow);
        return card;
    }

    private View buildTestButton() {
        Button test = button("동작 테스트", false);
        test.setContentDescription("현재 선택한 종료 동작을 즉시 테스트");
        test.setOnClickListener(v -> confirmTest());
        return test;
    }

    private Switch addActionRow(LinearLayout parent, String title) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleView = text(title, 15, TEXT_PRIMARY);
        Switch toggle = new Switch(this);
        toggle.setId(View.generateViewId());
        toggle.setContentDescription(title + " 켜기 또는 끄기");
        titleView.setLabelFor(toggle.getId());

        row.addView(titleView, new LinearLayout.LayoutParams(0, dp(50), 1f));
        row.addView(toggle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(50)
        ));
        parent.addView(row);
        return toggle;
    }

    private LinearLayout permissionRow(String title, TextView status, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(3), 0, dp(3));

        TextView titleView = text(title, 14, TEXT_PRIMARY);
        row.addView(titleView, new LinearLayout.LayoutParams(0, dp(46), 1f));

        status.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        row.addView(status, new LinearLayout.LayoutParams(dp(88), dp(46)));

        Button action = button("설정", false);
        action.setTextSize(12);
        action.setContentDescription(title + " 설정 열기");
        action.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(64), dp(40));
        params.setMarginStart(dp(8));
        row.addView(action, params);
        return row;
    }

    private TextView permissionStatus() {
        TextView status = text("확인 중", 12, NEUTRAL);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        return status;
    }

    private void handleActionToggle(int action, Switch changedSwitch, boolean enabled) {
        if (updatingSwitches) {
            return;
        }

        setActionPreference(action, enabled);
        if (!AppPrefs.hasAnyActionEnabled(this)) {
            setActionPreference(action, true);
            updatingSwitches = true;
            changedSwitch.setChecked(true);
            updatingSwitches = false;
            Toast.makeText(this, "종료 동작을 하나 이상 켜야 합니다.", Toast.LENGTH_SHORT).show();
        }
        refresh();
    }

    private void setActionPreference(int action, boolean enabled) {
        if (action == ACTION_PAUSE_MEDIA) {
            AppPrefs.setPauseMedia(this, enabled);
        } else if (action == ACTION_MUTE_VOLUME) {
            AppPrefs.setMuteVolume(this, enabled);
        } else if (action == ACTION_LOCK_SCREEN) {
            AppPrefs.setLockScreen(this, enabled);
        }
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
                        AlarmScheduler.scheduleDaily(this);
                    }
                    refresh();
                },
                hour,
                minute,
                true
        );
        dialog.setTitle("매일 종료 시각");
        dialog.show();
    }

    private void showCustomTimerDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("분 단위 (1~1440)");
        input.setSelectAllOnFocus(true);
        input.setPadding(dp(18), dp(12), dp(18), dp(12));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("일회성 타이머")
                .setMessage("몇 분 후 종료할지 입력하세요. 최대 24시간까지 설정할 수 있습니다.")
                .setView(input)
                .setNegativeButton("취소", null)
                .setPositiveButton("설정", null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String value = input.getText() == null ? "" : input.getText().toString().trim();
                    int minutes;
                    try {
                        minutes = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        input.setError("1~1440 사이의 숫자를 입력하세요.");
                        return;
                    }
                    if (minutes < 1 || minutes > 1_440) {
                        input.setError("1~1440 사이의 숫자를 입력하세요.");
                        return;
                    }
                    if (startQuickTimer(minutes)) {
                        dialog.dismiss();
                    }
                }));
        dialog.show();
    }

    private boolean startQuickTimer(int minutes) {
        if (!AppPrefs.hasAnyActionEnabled(this)) {
            Toast.makeText(this, "종료 동작을 하나 이상 켜세요.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!AlarmScheduler.scheduleOneShotAfter(this, minutes)) {
            Toast.makeText(this, "일회성 타이머를 설정할 수 없습니다.", Toast.LENGTH_LONG).show();
            refresh();
            return false;
        }

        String message = minutes + "분 후 종료합니다.";
        if (!AlarmScheduler.canScheduleExactAlarms(this)) {
            message += " 정확한 알람 권한이 없어 실행이 늦어질 수 있습니다.";
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        refresh();
        return true;
    }

    private void confirmTest() {
        if (!AppPrefs.hasAnyActionEnabled(this)) {
            Toast.makeText(this, "종료 동작을 하나 이상 켜세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("동작을 테스트할까요?")
                .setMessage(configuredActionsLabel())
                .setNegativeButton("취소", null)
                .setPositiveButton("실행", (dialog, which) -> SleepActionReceiver.runNow(this))
                .show();
    }

    private String configuredActionsLabel() {
        List<String> actions = new ArrayList<>();
        if (AppPrefs.shouldPauseMedia(this)) {
            actions.add("재생 일시정지");
        }
        if (AppPrefs.shouldMuteVolume(this)) {
            actions.add("음량 0");
        }
        if (AppPrefs.shouldLockScreen(this)) {
            actions.add("화면 잠금");
        }

        StringBuilder value = new StringBuilder("실행: ");
        for (String action : actions) {
            if (value.length() > 4) {
                value.append(", ");
            }
            value.append(action);
        }
        return value.toString();
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
            openAppDetails("앱 설정에서 알람 권한을 확인하세요.");
        }
    }

    private void openNotificationSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        } catch (Exception ignored) {
            openAppDetails("앱 설정에서 미디어 제어 권한을 확인하세요.");
        }
    }

    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception ignored) {
            openAppDetails("앱 설정에서 접근성 권한을 확인하세요.");
        }
    }

    private void openAppDetails(String fallbackMessage) {
        Toast.makeText(this, fallbackMessage, Toast.LENGTH_LONG).show();
        try {
            Intent intent = new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())
            );
            startActivity(intent);
        } catch (Exception ignored) {
            // No further settings page is available.
        }
    }

    private void refresh() {
        hour = AppPrefs.getHour(this);
        minute = AppPrefs.getMinute(this);

        boolean dailyEnabled = AppPrefs.isEnabled(this);
        boolean oneShotActive = AlarmScheduler.isOneShotActive(this);
        boolean exact = AlarmScheduler.canScheduleExactAlarms(this);
        boolean mediaAccess = MediaControlNotificationListenerService.isEnabled(this);
        boolean screenAccess = ScreenLockAccessibilityService.isEnabled(this);
        boolean pauseMedia = AppPrefs.shouldPauseMedia(this);
        boolean muteVolume = AppPrefs.shouldMuteVolume(this);
        boolean lockScreen = AppPrefs.shouldLockScreen(this);

        updatingSwitches = true;
        timeButton.setText(AppPrefs.formatTime(hour, minute));
        dailySwitch.setChecked(dailyEnabled);
        pauseMediaSwitch.setChecked(pauseMedia);
        muteVolumeSwitch.setChecked(muteVolume);
        lockScreenSwitch.setChecked(lockScreen);
        updatingSwitches = false;

        if (dailyEnabled) {
            long next = AlarmScheduler.getNextDailyTrigger(this);
            if (AlarmScheduler.isDailyOverrideActive(this)) {
                dailySummary.setText(
                        "오늘만 " + AppPrefs.formatClockTime(next)
                                + " · 기본 " + AppPrefs.formatTime(hour, minute)
                );
            } else {
                dailySummary.setText(
                        "다음 종료 " + AppPrefs.formatDateTime(next)
                                + (AppPrefs.isNextTriggerExact(this) ? "" : " · 근사")
                );
            }
        } else {
            dailySummary.setText("사용 안 함");
        }

        if (oneShotActive) {
            long trigger = AlarmScheduler.getOneShotTrigger(this);
            oneShotSummary.setText(remainingLabel(trigger) + " · " + AppPrefs.formatClockTime(trigger));
            oneShotSummary.setVisibility(View.VISIBLE);
            oneShotCancelButton.setVisibility(View.VISIBLE);
        } else {
            oneShotSummary.setVisibility(View.GONE);
            oneShotCancelButton.setVisibility(View.GONE);
        }

        boolean anyTimerActive = dailyEnabled || oneShotActive;
        boolean missingRequiredAccess = (pauseMedia && !mediaAccess) || (lockScreen && !screenAccess);
        if (missingRequiredAccess) {
            setAppStatus("권한 확인", WARNING);
        } else if (anyTimerActive) {
            setAppStatus("작동 중", POSITIVE);
        } else {
            setAppStatus("대기", NEUTRAL);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            setNeutralStatus(exactStatus, "필요 없음");
        } else if (exact) {
            setStatus(exactStatus, true, "허용됨");
        } else if (anyTimerActive) {
            setStatus(exactStatus, false, "근사 사용");
        } else {
            setNeutralStatus(exactStatus, "선택 사항");
        }

        mediaPermissionRow.setVisibility(pauseMedia ? View.VISIBLE : View.GONE);
        if (pauseMedia) {
            setStatus(mediaStatus, mediaAccess, mediaAccess ? "허용됨" : "설정 필요");
        }

        screenPermissionRow.setVisibility(lockScreen ? View.VISIBLE : View.GONE);
        if (lockScreen) {
            setStatus(screenStatus, screenAccess, screenAccess ? "허용됨" : "설정 필요");
        }
    }

    private void setAppStatus(String label, int color) {
        appStatus.setText(label);
        appStatus.setTextColor(color);
    }

    private String remainingLabel(long trigger) {
        long remaining = trigger - System.currentTimeMillis();
        if (remaining <= 0L) {
            return "곧 종료";
        }
        long totalMinutes = Math.max(1L, (remaining + 59_999L) / 60_000L);
        if (totalMinutes < 60L) {
            return totalMinutes + "분 후";
        }
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        if (minutes == 0L) {
            return hours + "시간 후";
        }
        return String.format(Locale.KOREA, "%d시간 %d분 후", hours, minutes);
    }

    private void setStatus(TextView view, boolean good, String label) {
        view.setText(label);
        view.setTextColor(good ? POSITIVE : WARNING);
    }

    private void setNeutralStatus(TextView view, String label) {
        view.setText(label);
        view.setTextColor(NEUTRAL);
    }

    private void applySystemBarInsets(ScrollView scrollView) {
        if (Build.VERSION.SDK_INT < 35) {
            return;
        }
        scrollView.setClipToPadding(false);
        scrollView.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets systemBars = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()
            );
            view.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
            );
            return insets;
        });
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(round(SURFACE, 18));
        return card;
    }

    private Button button(String label, boolean prominent) {
        Button view = new Button(this);
        view.setText(label);
        view.setTextColor(prominent ? BG : TEXT_PRIMARY);
        view.setTextSize(14);
        view.setGravity(Gravity.CENTER);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        view.setAllCaps(false);
        view.setMinHeight(0);
        view.setMinWidth(0);
        view.setPadding(dp(10), 0, dp(10), 0);
        view.setBackground(round(prominent ? PRIMARY : SURFACE_RAISED, 13));
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
