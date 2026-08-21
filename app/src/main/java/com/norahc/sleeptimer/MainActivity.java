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
    private TextView dailySummary;
    private TextView oneShotSummary;
    private TextView readinessStatus;
    private TextView lastRun;
    private TextView exactStatus;
    private TextView mediaStatus;
    private TextView screenStatus;
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
        root.setPadding(dp(22), dp(20), dp(22), dp(30));
        scrollView.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView eyebrow = text("SLEEP ROUTINE", 12, PRIMARY);
        eyebrow.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(eyebrow, margins(0, 0, 0, 7));

        TextView title = text("수면 타이머", 32, TEXT_PRIMARY);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title, margins(0, 0, 0, 6));

        TextView subtitle = text(
                "원하는 시각이나 잠들기 전 카운트다운에 맞춰\n재생을 멈추고 조용히 마무리합니다.",
                16,
                TEXT_SECONDARY
        );
        subtitle.setLineSpacing(2f, 1.0f);
        root.addView(subtitle, margins(0, 0, 0, 22));

        root.addView(buildDailyCard(), margins(0, 0, 0, 12));
        root.addView(buildQuickTimerCard(), margins(0, 0, 0, 12));
        root.addView(buildActionsCard(), margins(0, 0, 0, 12));
        root.addView(buildTestCard(), margins(0, 0, 0, 22));

        TextView permissionsTitle = text("권한 및 접근", 20, TEXT_PRIMARY);
        permissionsTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(permissionsTitle, margins(0, 0, 0, 10));

        LinearLayout permissionsCard = card();
        exactStatus = addPermissionRow(
                permissionsCard,
                "정확한 알람",
                "정확한 시각에 실행 · 미허용 시 근사 예약",
                "설정",
                v -> openExactAlarmSettings()
        );
        addDivider(permissionsCard);
        mediaStatus = addPermissionRow(
                permissionsCard,
                "미디어 제어",
                "YouTube·CHZZK·브라우저에 일시정지 요청",
                "설정",
                v -> openNotificationSettings()
        );
        addDivider(permissionsCard);
        screenStatus = addPermissionRow(
                permissionsCard,
                "화면 잠금",
                "접근성의 잠금 동작만 사용",
                "설정",
                v -> openAccessibilitySettings()
        );
        root.addView(permissionsCard, margins(0, 0, 0, 12));

        readinessStatus = text("", 13, TEXT_SECONDARY);
        readinessStatus.setGravity(Gravity.CENTER);
        readinessStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        root.addView(readinessStatus, margins(0, 2, 0, 20));

        LinearLayout noteCard = card();
        TextView noteTitle = text("작동 방식", 16, TEXT_PRIMARY);
        noteTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        noteCard.addView(noteTitle, margins(0, 0, 0, 7));
        noteCard.addView(text(
                "매일 예약과 일회성 타이머는 서로 독립적으로 동작합니다. 재부팅·시간 변경·앱 업데이트 뒤에도 남아 있는 예약을 다시 구성합니다.\n\n미디어 제어는 Android가 노출한 활성 미디어 세션에만 일시정지를 요청합니다. 화면 잠금 접근성 서비스는 화면 내용을 읽지 않으며 잠금 동작 하나만 호출합니다.",
                13,
                TEXT_SECONDARY
        ));
        root.addView(noteCard, margins(0, 0, 0, 18));

        lastRun = text("", 12, TEXT_SECONDARY);
        lastRun.setGravity(Gravity.CENTER);
        lastRun.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        root.addView(lastRun);

        return scrollView;
    }

    private View buildDailyCard() {
        LinearLayout card = card();
        card.addView(text("매일 자동 실행", 13, TEXT_SECONDARY), margins(0, 0, 0, 4));

        timeButton = button(AppPrefs.formatTime(hour, minute), true);
        timeButton.setTextSize(36);
        timeButton.setContentDescription("매일 실행 시각 변경");
        timeButton.setOnClickListener(v -> showTimePicker());
        card.addView(timeButton, margins(0, 0, 0, 14));

        LinearLayout switchRow = new LinearLayout(this);
        switchRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView switchCopy = text("지정한 시각에 매일 실행", 16, TEXT_PRIMARY);
        dailySwitch = new Switch(this);
        dailySwitch.setId(View.generateViewId());
        dailySwitch.setContentDescription("매일 자동 실행 켜기 또는 끄기");
        switchCopy.setLabelFor(dailySwitch.getId());
        switchRow.addView(switchCopy, new LinearLayout.LayoutParams(0, dp(52), 1f));
        switchRow.addView(dailySwitch, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(52)
        ));
        card.addView(switchRow);

        dailySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updatingSwitches) {
                return;
            }
            AppPrefs.setEnabled(this, isChecked);
            if (isChecked) {
                boolean scheduled = AlarmScheduler.scheduleDaily(this);
                if (!scheduled) {
                    AppPrefs.setEnabled(this, false);
                    Toast.makeText(this, "예약을 만들 수 없습니다. 잠시 후 다시 시도하세요.", Toast.LENGTH_LONG).show();
                } else if (!AlarmScheduler.canScheduleExactAlarms(this)) {
                    Toast.makeText(
                            this,
                            "현재는 근사 시각으로 예약됩니다. 정확한 실행을 원하면 알람 및 리마인더 권한을 허용하세요.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            } else {
                AlarmScheduler.cancelDaily(this);
            }
            refresh();
        });

        dailySummary = text("", 14, TEXT_SECONDARY);
        dailySummary.setLineSpacing(1f, 1.1f);
        dailySummary.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        card.addView(dailySummary, margins(0, 8, 0, 0));
        return card;
    }

    private View buildQuickTimerCard() {
        LinearLayout card = card();
        TextView title = text("빠른 타이머", 17, TEXT_PRIMARY);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(title, margins(0, 0, 0, 4));
        card.addView(text(
                "한 번만 실행할 카운트다운을 설정합니다. 매일 예약과 함께 사용할 수 있습니다.",
                13,
                TEXT_SECONDARY
        ), margins(0, 0, 0, 13));

        card.addView(presetRow(15, 30), margins(0, 0, 0, 8));
        card.addView(presetRow(60, 90), margins(0, 0, 0, 8));

        Button custom = button("직접 입력", false);
        custom.setContentDescription("일회성 타이머 시간을 직접 입력");
        custom.setOnClickListener(v -> showCustomTimerDialog());
        card.addView(custom, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
        ));

        oneShotSummary = text("", 14, TEXT_SECONDARY);
        oneShotSummary.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        card.addView(oneShotSummary, margins(0, 13, 0, 9));

        oneShotCancelButton = button("일회성 타이머 취소", false);
        oneShotCancelButton.setOnClickListener(v -> {
            AlarmScheduler.cancelOneShot(this);
            Toast.makeText(this, "일회성 타이머를 취소했습니다.", Toast.LENGTH_SHORT).show();
            refresh();
        });
        card.addView(oneShotCancelButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
        ));
        return card;
    }

    private View buildActionsCard() {
        LinearLayout card = card();
        TextView title = text("실행 동작", 17, TEXT_PRIMARY);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(title, margins(0, 0, 0, 4));
        card.addView(text(
                "타이머가 끝났을 때 수행할 동작을 고릅니다. 최소 1개는 켜져 있어야 합니다.",
                13,
                TEXT_SECONDARY
        ), margins(0, 0, 0, 8));

        pauseMediaSwitch = addActionRow(
                card,
                "미디어 일시정지",
                "활성 미디어 세션에 일시정지 요청"
        );
        pauseMediaSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                handleActionToggle(ACTION_PAUSE_MEDIA, pauseMediaSwitch, isChecked));

        addDivider(card);
        muteVolumeSwitch = addActionRow(
                card,
                "미디어 음량 0",
                "음악/영상 스트림의 음량을 0으로 변경"
        );
        muteVolumeSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                handleActionToggle(ACTION_MUTE_VOLUME, muteVolumeSwitch, isChecked));

        addDivider(card);
        lockScreenSwitch = addActionRow(
                card,
                "화면 잠금",
                "접근성 전역 잠금 동작 실행"
        );
        lockScreenSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                handleActionToggle(ACTION_LOCK_SCREEN, lockScreenSwitch, isChecked));
        return card;
    }

    private View buildTestCard() {
        LinearLayout card = card();
        TextView title = text("동작 확인", 16, TEXT_PRIMARY);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(title, margins(0, 0, 0, 5));
        card.addView(text(
                "현재 선택한 실행 동작을 즉시 테스트합니다. 화면 잠금을 켰다면 테스트 직후 화면이 잠깁니다.",
                14,
                TEXT_SECONDARY
        ), margins(0, 0, 0, 13));
        Button testButton = button("지금 테스트", false);
        testButton.setOnClickListener(v -> confirmTest());
        card.addView(testButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
        ));
        return card;
    }

    private LinearLayout presetRow(int leftMinutes, int rightMinutes) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        Button left = button("+" + leftMinutes + "분", false);
        left.setContentDescription(leftMinutes + "분 후 일회성 타이머 설정");
        left.setOnClickListener(v -> startQuickTimer(leftMinutes));
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        leftParams.setMarginEnd(dp(4));
        row.addView(left, leftParams);

        Button right = button("+" + rightMinutes + "분", false);
        right.setContentDescription(rightMinutes + "분 후 일회성 타이머 설정");
        right.setOnClickListener(v -> startQuickTimer(rightMinutes));
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        rightParams.setMarginStart(dp(4));
        row.addView(right, rightParams);
        return row;
    }

    private Switch addActionRow(LinearLayout parent, String title, String detail) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(5), 0, dp(5));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = text(title, 15, TEXT_PRIMARY);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        copy.addView(titleView);
        copy.addView(text(detail, 12, TEXT_SECONDARY), margins(0, 3, 0, 0));

        Switch toggle = new Switch(this);
        toggle.setId(View.generateViewId());
        toggle.setContentDescription(title + " 켜기 또는 끄기");
        titleView.setLabelFor(toggle.getId());

        row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(toggle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(52)
        ));
        parent.addView(row);
        return toggle;
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
            Toast.makeText(this, "최소 1개의 실행 동작은 켜져 있어야 합니다.", Toast.LENGTH_SHORT).show();
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

    private TextView addPermissionRow(
            LinearLayout parent,
            String title,
            String detail,
            String buttonText,
            View.OnClickListener listener
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = text(title, 15, TEXT_PRIMARY);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        copy.addView(titleView);
        copy.addView(text(detail, 12, TEXT_SECONDARY), margins(0, 3, 0, 0));
        TextView status = text("확인 중", 12, WARNING);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        copy.addView(status, margins(0, 5, 0, 0));

        row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button action = button(buttonText, false);
        action.setTextSize(13);
        action.setContentDescription(title + " 설정 열기");
        action.setOnClickListener(listener);
        row.addView(action, new LinearLayout.LayoutParams(dp(76), dp(44)));
        parent.addView(row);
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
                        AlarmScheduler.scheduleDaily(this);
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

    private void showCustomTimerDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("분 단위 (1~1440)");
        input.setSelectAllOnFocus(true);
        input.setPadding(dp(18), dp(12), dp(18), dp(12));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("일회성 타이머")
                .setMessage("몇 분 후 실행할지 입력하세요. 최대 24시간까지 설정할 수 있습니다.")
                .setView(input)
                .setNegativeButton("취소", null)
                .setPositiveButton("예약", null)
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
            Toast.makeText(this, "실행 동작을 하나 이상 켜세요.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!AlarmScheduler.scheduleOneShotAfter(this, minutes)) {
            Toast.makeText(this, "일회성 타이머를 예약할 수 없습니다.", Toast.LENGTH_LONG).show();
            refresh();
            return false;
        }

        String message = minutes + "분 후 실행하도록 예약했습니다.";
        if (!AlarmScheduler.canScheduleExactAlarms(this)) {
            message += " 정확한 알람 권한이 없어 실제 실행은 늦어질 수 있습니다.";
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        refresh();
        return true;
    }

    private void confirmTest() {
        if (!AppPrefs.hasAnyActionEnabled(this)) {
            Toast.makeText(this, "실행 동작을 하나 이상 켜세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("지금 실행할까요?")
                .setMessage("선택한 동작: " + configuredActionsLabel())
                .setNegativeButton("취소", null)
                .setPositiveButton("실행", (dialog, which) -> {
                    SleepActionReceiver.runNow(this);
                    refresh();
                })
                .show();
    }

    private String configuredActionsLabel() {
        List<String> actions = new ArrayList<>();
        if (AppPrefs.shouldPauseMedia(this)) {
            actions.add("미디어 일시정지");
        }
        if (AppPrefs.shouldMuteVolume(this)) {
            actions.add("음량 0");
        }
        if (AppPrefs.shouldLockScreen(this)) {
            actions.add("화면 잠금");
        }

        StringBuilder value = new StringBuilder();
        for (String action : actions) {
            if (value.length() > 0) {
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
            openAppDetails("정확한 알람 설정을 직접 열 수 없습니다. 앱 설정에서 알람 권한을 확인하세요.");
        }
    }

    private void openNotificationSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        } catch (Exception ignored) {
            openAppDetails("알림 접근 설정을 직접 열 수 없습니다. 앱 설정을 확인하세요.");
        }
    }

    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception ignored) {
            openAppDetails("접근성 설정을 직접 열 수 없습니다. 앱 설정을 확인하세요.");
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
            // There is no further system settings page we can safely open.
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
            dailySummary.setText(
                    "다음 실행 " + AppPrefs.formatDateTime(next)
                            + (AppPrefs.isNextTriggerExact(this) ? " · 정확한 알람" : " · 근사 알람")
            );
        } else {
            dailySummary.setText("꺼짐 · 원하는 시각을 선택한 뒤 스위치를 켜세요.");
        }

        if (oneShotActive) {
            long trigger = AlarmScheduler.getOneShotTrigger(this);
            oneShotSummary.setText(
                    remainingLabel(trigger) + " · " + AppPrefs.formatDateTime(trigger)
                            + (AppPrefs.isOneShotExact(this) ? " · 정확한 알람" : " · 근사 알람")
            );
            oneShotCancelButton.setEnabled(true);
            oneShotCancelButton.setAlpha(1f);
        } else {
            oneShotSummary.setText("설정된 일회성 타이머가 없습니다.");
            oneShotCancelButton.setEnabled(false);
            oneShotCancelButton.setAlpha(0.45f);
        }

        boolean anyTimerActive = dailyEnabled || oneShotActive;
        int missing = 0;
        if (anyTimerActive && !exact) {
            missing++;
        }
        if (pauseMedia && !mediaAccess) {
            missing++;
        }
        if (lockScreen && !screenAccess) {
            missing++;
        }

        if (missing == 0) {
            readinessStatus.setText(anyTimerActive
                    ? "● 예약과 실행 동작이 준비되었습니다."
                    : "● 실행 동작이 준비되었습니다.");
            readinessStatus.setTextColor(POSITIVE);
        } else {
            readinessStatus.setText("○ 설정 또는 권한 " + missing + "개를 확인해 주세요.");
            readinessStatus.setTextColor(WARNING);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            setNeutralStatus(exactStatus, "● 별도 권한 필요 없음");
        } else if (exact) {
            setStatus(exactStatus, true, "허용됨");
        } else if (anyTimerActive) {
            setStatus(exactStatus, false, "근사 알람 사용 중");
        } else {
            setNeutralStatus(exactStatus, "○ 선택 사항");
        }

        if (!pauseMedia) {
            setNeutralStatus(mediaStatus, "○ 사용 안 함");
        } else {
            setStatus(mediaStatus, mediaAccess, mediaAccess ? "허용됨" : "허용 필요");
        }

        if (!lockScreen) {
            setNeutralStatus(screenStatus, "○ 사용 안 함");
        } else {
            setStatus(screenStatus, screenAccess, screenAccess ? "허용됨" : "허용 필요");
        }

        lastRun.setText(getString(R.string.last_run, AppPrefs.getLastRun(this)));
    }

    private String remainingLabel(long trigger) {
        long remaining = trigger - System.currentTimeMillis();
        if (remaining <= 0L) {
            return "곧 실행";
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
        view.setText(getString(good ? R.string.permission_allowed : R.string.permission_required, label));
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
        card.setPadding(dp(17), dp(16), dp(17), dp(16));
        card.setBackground(round(SURFACE, 20));
        return card;
    }

    private Button button(String label, boolean prominent) {
        Button view = new Button(this);
        view.setText(label);
        view.setTextColor(prominent ? BG : TEXT_PRIMARY);
        view.setTextSize(15);
        view.setGravity(Gravity.CENTER);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        view.setAllCaps(false);
        view.setMinHeight(0);
        view.setMinWidth(0);
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
