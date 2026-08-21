package com.norahc.sleeptimer;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int ACTION_PAUSE_MEDIA = 1;
    private static final int ACTION_MUTE_VOLUME = 2;
    private static final int ACTION_LOCK_SCREEN = 3;

    private Button timeButton;
    private Button oneShotCancelButton;
    private TextView appStatus;
    private TextView nextTimerTime;
    private TextView nextTimerDetail;
    private TextView dailySummary;
    private TextView oneShotSummary;
    private TextView exactStatus;
    private TextView mediaStatus;
    private TextView screenStatus;
    private LinearLayout mediaPermissionGroup;
    private LinearLayout screenPermissionGroup;
    private Switch dailySwitch;
    private Switch pauseMediaSwitch;
    private Switch muteVolumeSwitch;
    private Switch lockScreenSwitch;
    private boolean updatingSwitches;
    private boolean tabletLayout;
    private int hour;
    private int minute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(UiKit.BG);
        getWindow().setNavigationBarColor(UiKit.BG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().setNavigationBarDividerColor(UiKit.BG);
        }

        tabletLayout = UiKit.isTablet(this);
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
        scrollView.setBackgroundColor(UiKit.BG);
        applySystemBarInsets(scrollView);

        FrameLayout host = new FrameLayout(this);
        scrollView.addView(host, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        int sideMargin = tabletLayout ? 28 : 16;
        int maxWidth = tabletLayout ? 1120 : 640;
        int viewportWidth = getResources().getDisplayMetrics().widthPixels;
        int contentWidth = Math.min(
                viewportWidth - dp(sideMargin * 2),
                dp(maxWidth)
        );

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, dp(tabletLayout ? 28 : 20), 0, dp(30));
        FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams(
                Math.max(dp(280), contentWidth),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL
        );
        host.addView(root, rootParams);

        root.addView(buildHeader(), margins(0, 0, 0, 16));
        root.addView(buildHeroCard(), margins(0, 0, 0, 14));

        if (tabletLayout) {
            LinearLayout columns = new LinearLayout(this);
            columns.setOrientation(LinearLayout.HORIZONTAL);
            columns.setGravity(Gravity.TOP);

            LinearLayout left = new LinearLayout(this);
            left.setOrientation(LinearLayout.VERTICAL);
            left.addView(buildDailyCard(), margins(0, 0, 0, 14));
            left.addView(buildQuickTimerCard());

            LinearLayout right = new LinearLayout(this);
            right.setOrientation(LinearLayout.VERTICAL);
            right.addView(buildActionsCard(), margins(0, 0, 0, 14));
            right.addView(buildPermissionsCard(), margins(0, 0, 0, 14));
            right.addView(buildToolsCard());

            LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
            );
            leftParams.setMarginEnd(dp(7));
            columns.addView(left, leftParams);

            LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
            );
            rightParams.setMarginStart(dp(7));
            columns.addView(right, rightParams);
            root.addView(columns);
        } else {
            root.addView(buildDailyCard(), margins(0, 0, 0, 12));
            root.addView(buildQuickTimerCard(), margins(0, 0, 0, 12));
            root.addView(buildActionsCard(), margins(0, 0, 0, 12));
            root.addView(buildPermissionsCard(), margins(0, 0, 0, 12));
            root.addView(buildToolsCard());
        }

        return scrollView;
    }

    private View buildHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("수면 타이머", tabletLayout ? 32 : 29, UiKit.TEXT_PRIMARY);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        titles.addView(title);
        TextView subtitle = text("재생 종료를 자동으로 정리합니다", 13, UiKit.TEXT_SECONDARY);
        titles.addView(subtitle, margins(0, 3, 0, 0));
        row.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        appStatus = text("대기", 12, UiKit.NEUTRAL);
        appStatus.setGravity(Gravity.CENTER);
        appStatus.setTypeface(null, android.graphics.Typeface.BOLD);
        appStatus.setPadding(dp(12), dp(7), dp(12), dp(7));
        appStatus.setBackground(UiKit.rounded(this, UiKit.SURFACE_RAISED, 16));
        appStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        row.addView(appStatus);
        return row;
    }

    private View buildHeroCard() {
        LinearLayout card = card();
        card.setPadding(dp(tabletLayout ? 20 : 17), dp(17), dp(tabletLayout ? 20 : 17), dp(17));

        TextView label = text("다음 종료", 12, UiKit.TEXT_SECONDARY);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(label);

        nextTimerTime = text("예약 없음", tabletLayout ? 34 : 31, UiKit.TEXT_PRIMARY);
        nextTimerTime.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(nextTimerTime, margins(0, 5, 0, 0));

        nextTimerDetail = text("", 13, UiKit.TEXT_SECONDARY);
        nextTimerDetail.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        card.addView(nextTimerDetail, margins(0, 5, 0, 0));
        return card;
    }

    private View buildDailyCard() {
        LinearLayout card = card();
        LinearLayout top = sectionHeader("매일 종료");

        dailySwitch = new Switch(this);
        dailySwitch.setId(View.generateViewId());
        dailySwitch.setContentDescription("매일 자동 종료 켜기 또는 끄기");
        UiKit.tintSwitch(dailySwitch);
        TextView title = (TextView) ((LinearLayout) top.getChildAt(0)).getChildAt(0);
        title.setLabelFor(dailySwitch.getId());
        top.addView(dailySwitch, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(48)
        ));
        card.addView(top, margins(0, 0, 0, 10));

        timeButton = button(AppPrefs.formatTime(hour, minute), false);
        timeButton.setTextSize(tabletLayout ? 36 : 34);
        timeButton.setTextColor(UiKit.PRIMARY);
        timeButton.setBackground(UiKit.rippleStroke(
                this,
                UiKit.SURFACE_RAISED,
                16,
                UiKit.PRIMARY_DARK
        ));
        timeButton.setContentDescription("매일 종료 시각 변경");
        timeButton.setOnClickListener(v -> showTimePicker());
        card.addView(timeButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(66)
        ));

        dailySummary = text("", 13, UiKit.TEXT_SECONDARY);
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
        card.addView(sectionTitle("일회성 타이머"), margins(0, 0, 0, 12));

        int[] minutes = {15, 30, 60, 90};
        if (tabletLayout) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int i = 0; i < minutes.length; i++) {
                addPresetButton(row, minutes[i], i == 0 ? 0 : 4);
            }
            card.addView(row);
        } else {
            LinearLayout row1 = new LinearLayout(this);
            row1.setOrientation(LinearLayout.HORIZONTAL);
            addPresetButton(row1, 15, 0);
            addPresetButton(row1, 30, 6);
            card.addView(row1);

            LinearLayout row2 = new LinearLayout(this);
            row2.setOrientation(LinearLayout.HORIZONTAL);
            addPresetButton(row2, 60, 0);
            addPresetButton(row2, 90, 6);
            card.addView(row2, margins(0, 7, 0, 0));
        }

        Button custom = button("직접 입력", false);
        custom.setContentDescription("일회성 타이머 시간을 직접 입력");
        custom.setOnClickListener(v -> showCustomTimerDialog());
        card.addView(custom, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
        ));
        ((LinearLayout.LayoutParams) custom.getLayoutParams()).setMargins(0, dp(9), 0, 0);

        oneShotSummary = text("", 13, UiKit.TEXT_SECONDARY);
        oneShotSummary.setGravity(Gravity.CENTER_HORIZONTAL);
        oneShotSummary.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        card.addView(oneShotSummary, margins(0, 12, 0, 7));

        oneShotCancelButton = button("타이머 취소", false);
        oneShotCancelButton.setTextColor(UiKit.WARNING);
        oneShotCancelButton.setOnClickListener(v -> {
            AlarmScheduler.cancelOneShot(this);
            Toast.makeText(this, "일회성 타이머를 취소했습니다.", Toast.LENGTH_SHORT).show();
            refresh();
        });
        card.addView(oneShotCancelButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44)
        ));
        return card;
    }

    private void addPresetButton(LinearLayout row, int minutes, int startMarginDp) {
        Button preset = UiKit.chip(this, minutes + "분", false);
        preset.setTextSize(14);
        preset.setContentDescription(minutes + "분 후 일회성 타이머 설정");
        preset.setOnClickListener(v -> startQuickTimer(minutes));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1f);
        if (startMarginDp > 0) {
            params.setMarginStart(dp(startMarginDp));
        }
        row.addView(preset, params);
    }

    private View buildActionsCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("종료할 때"), margins(0, 0, 0, 5));

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
        card.addView(sectionTitle("권한"), margins(0, 0, 0, 5));

        exactStatus = permissionStatus();
        card.addView(permissionRow("정확한 알람", exactStatus, v -> openExactAlarmSettings()));

        mediaStatus = permissionStatus();
        mediaPermissionGroup = permissionGroup(
                "미디어 제어",
                mediaStatus,
                v -> openNotificationSettings()
        );
        card.addView(mediaPermissionGroup);

        screenStatus = permissionStatus();
        screenPermissionGroup = permissionGroup(
                "화면 제어",
                screenStatus,
                v -> openAccessibilitySettings()
        );
        card.addView(screenPermissionGroup);
        return card;
    }

    private View buildToolsCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("도구"), margins(0, 0, 0, 10));
        Button test = button("동작 테스트", false);
        test.setContentDescription("현재 선택한 종료 동작을 즉시 테스트");
        test.setOnClickListener(v -> confirmTest());
        card.addView(test, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
        ));
        return card;
    }

    private LinearLayout sectionHeader(String titleText) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(sectionTitle(titleText));
        row.addView(titles, new LinearLayout.LayoutParams(0, dp(48), 1f));
        return row;
    }

    private TextView sectionTitle(String title) {
        TextView view = text(title, 17, UiKit.TEXT_PRIMARY);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        return view;
    }

    private Switch addActionRow(LinearLayout parent, String title) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleView = text(title, 15, UiKit.TEXT_PRIMARY);
        Switch toggle = new Switch(this);
        toggle.setId(View.generateViewId());
        toggle.setContentDescription(title + " 켜기 또는 끄기");
        titleView.setLabelFor(toggle.getId());
        UiKit.tintSwitch(toggle);

        row.addView(titleView, new LinearLayout.LayoutParams(0, dp(52), 1f));
        row.addView(toggle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(52)
        ));
        parent.addView(row);
        return toggle;
    }

    private LinearLayout permissionGroup(
            String title,
            TextView status,
            View.OnClickListener listener
    ) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        addDivider(group);
        group.addView(permissionRow(title, status, listener));
        return group;
    }

    private LinearLayout permissionRow(String title, TextView status, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView titleView = text(title, 14, UiKit.TEXT_PRIMARY);
        row.addView(titleView, new LinearLayout.LayoutParams(0, dp(48), 1f));

        status.setGravity(Gravity.CENTER);
        row.addView(status, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(34)
        ));

        Button action = UiKit.chip(this, "설정", false);
        action.setTextSize(12);
        action.setContentDescription(title + " 설정 열기");
        action.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(62), dp(38));
        params.setMarginStart(dp(8));
        row.addView(action, params);
        return row;
    }

    private TextView permissionStatus() {
        TextView status = text("확인 중", 11, UiKit.NEUTRAL);
        status.setTypeface(null, android.graphics.Typeface.BOLD);
        status.setPadding(dp(9), 0, dp(9), 0);
        status.setBackground(UiKit.rounded(this, UiKit.SURFACE_SOFT, 12));
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
        divider.setBackgroundColor(UiKit.DIVIDER);
        parent.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
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
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout panel = modalPanel();
        TextView title = text("일회성 타이머", 20, UiKit.TEXT_PRIMARY);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        panel.addView(title);
        panel.addView(
                text("몇 분 후 종료할지 입력하세요. 최대 24시간입니다.", 13, UiKit.TEXT_SECONDARY),
                margins(0, 6, 0, 14)
        );

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("1~1440분");
        input.setTextColor(UiKit.TEXT_PRIMARY);
        input.setHintTextColor(UiKit.NEUTRAL);
        input.setSingleLine(true);
        input.setSelectAllOnFocus(true);
        input.setPadding(dp(14), 0, dp(14), 0);
        input.setBackground(UiKit.roundedStroke(
                this,
                UiKit.SURFACE_RAISED,
                13,
                UiKit.DIVIDER
        ));
        panel.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
        ));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button cancel = button("취소", false);
        cancel.setOnClickListener(v -> dialog.dismiss());
        actions.addView(cancel, new LinearLayout.LayoutParams(0, dp(48), 1f));
        Button apply = button("설정", true);
        LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        applyParams.setMarginStart(dp(8));
        actions.addView(apply, applyParams);
        panel.addView(actions, margins(0, 16, 0, 0));

        apply.setOnClickListener(v -> {
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
        });

        dialog.setContentView(panel);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        applyModalWindow(dialog, 430);
        input.requestFocus();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
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

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = modalPanel();

        TextView title = text("동작 테스트", 20, UiKit.TEXT_PRIMARY);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        panel.addView(title);
        panel.addView(text(configuredActionsLabel(), 13, UiKit.TEXT_SECONDARY), margins(0, 7, 0, 16));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button cancel = button("취소", false);
        cancel.setOnClickListener(v -> dialog.dismiss());
        actions.addView(cancel, new LinearLayout.LayoutParams(0, dp(48), 1f));
        Button run = button("실행", true);
        run.setOnClickListener(v -> {
            dialog.dismiss();
            SleepActionReceiver.runNow(this);
        });
        LinearLayout.LayoutParams runParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        runParams.setMarginStart(dp(8));
        actions.addView(run, runParams);
        panel.addView(actions);

        dialog.setContentView(panel);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        applyModalWindow(dialog, 420);
    }

    private LinearLayout modalPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(19), dp(20), dp(18));
        panel.setBackground(UiKit.rounded(this, UiKit.SURFACE, 22));
        return panel;
    }

    private void applyModalWindow(Dialog dialog, int maxWidthDp) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.dimAmount = 0.16f;
        window.setAttributes(attributes);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int width = Math.min(screenWidth - dp(32), dp(maxWidthDp));
        window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
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

        refreshNextTimerHero();

        if (dailyEnabled) {
            long next = AlarmScheduler.getNextDailyTrigger(this);
            if (AlarmScheduler.isDailyOverrideActive(this)) {
                dailySummary.setText(
                        "오늘만 " + AppPrefs.formatClockTime(next)
                                + " · 기본 " + AppPrefs.formatTime(hour, minute)
                );
            } else {
                dailySummary.setText(
                        "다음 " + AppPrefs.formatDateTime(next)
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
            setAppStatus("권한 확인", UiKit.WARNING);
        } else if (anyTimerActive) {
            setAppStatus("작동 중", UiKit.POSITIVE);
        } else {
            setAppStatus("대기", UiKit.NEUTRAL);
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

        mediaPermissionGroup.setVisibility(pauseMedia ? View.VISIBLE : View.GONE);
        if (pauseMedia) {
            setStatus(mediaStatus, mediaAccess, mediaAccess ? "허용됨" : "설정 필요");
        }

        screenPermissionGroup.setVisibility(lockScreen ? View.VISIBLE : View.GONE);
        if (lockScreen) {
            setStatus(screenStatus, screenAccess, screenAccess ? "허용됨" : "설정 필요");
        }
    }

    private void refreshNextTimerHero() {
        String source = AlarmScheduler.getNextActiveTimerSource(this);
        long trigger = AlarmScheduler.getNextActiveTimerTrigger(this);
        if (AlarmScheduler.TIMER_SOURCE_NONE.equals(source) || trigger <= System.currentTimeMillis()) {
            nextTimerTime.setText("예약 없음");
            nextTimerTime.setTextColor(UiKit.TEXT_PRIMARY);
            nextTimerDetail.setText("타이머를 설정하면 다음 종료 시각이 표시됩니다.");
            return;
        }

        nextTimerTime.setText(AppPrefs.formatClockTime(trigger));
        nextTimerTime.setTextColor(UiKit.PRIMARY);
        if (AlarmScheduler.TIMER_SOURCE_ONE_SHOT.equals(source)) {
            nextTimerDetail.setText(remainingLabel(trigger) + " · 일회성 타이머");
        } else if (AlarmScheduler.isDailyOverrideActive(this)) {
            nextTimerDetail.setText(remainingLabel(trigger) + " · 오늘만 연장");
        } else {
            nextTimerDetail.setText(remainingLabel(trigger) + " · 매일 종료");
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
        view.setTextColor(good ? UiKit.POSITIVE : UiKit.WARNING);
    }

    private void setNeutralStatus(TextView view, String label) {
        view.setText(label);
        view.setTextColor(UiKit.NEUTRAL);
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
        card.setPadding(dp(17), dp(15), dp(17), dp(15));
        card.setBackground(UiKit.roundedStroke(
                this,
                UiKit.SURFACE,
                20,
                Color.rgb(36, 47, 72)
        ));
        return card;
    }

    private Button button(String label, boolean prominent) {
        return UiKit.button(this, label, prominent);
    }

    private TextView text(String value, float size, int color) {
        return UiKit.text(this, value, size, color);
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
