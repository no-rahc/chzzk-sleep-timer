package com.norahc.sleeptimer;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

final class UiKit {
    static final int BG = Color.rgb(10, 15, 29);
    static final int SURFACE = Color.rgb(20, 28, 48);
    static final int SURFACE_RAISED = Color.rgb(31, 42, 68);
    static final int SURFACE_SOFT = Color.rgb(25, 34, 57);
    static final int DIVIDER = Color.rgb(46, 57, 83);
    static final int PRIMARY = Color.rgb(159, 177, 255);
    static final int PRIMARY_DARK = Color.rgb(58, 72, 119);
    static final int TEXT_PRIMARY = Color.rgb(247, 248, 252);
    static final int TEXT_SECONDARY = Color.rgb(173, 184, 207);
    static final int POSITIVE = Color.rgb(112, 224, 178);
    static final int WARNING = Color.rgb(247, 198, 107);
    static final int NEUTRAL = Color.rgb(149, 161, 184);

    private UiKit() {
    }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static boolean isTablet(Context context) {
        return context.getResources().getConfiguration().screenWidthDp >= 700;
    }

    static GradientDrawable rounded(Context context, int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    static GradientDrawable roundedStroke(Context context, int color, int radiusDp, int strokeColor) {
        GradientDrawable drawable = rounded(context, color, radiusDp);
        drawable.setStroke(dp(context, 1), strokeColor);
        return drawable;
    }

    static Drawable ripple(Context context, int color, int radiusDp) {
        GradientDrawable content = rounded(context, color, radiusDp);
        return new RippleDrawable(
                ColorStateList.valueOf(Color.argb(42, 255, 255, 255)),
                content,
                null
        );
    }

    static Drawable rippleStroke(Context context, int color, int radiusDp, int strokeColor) {
        GradientDrawable content = roundedStroke(context, color, radiusDp, strokeColor);
        return new RippleDrawable(
                ColorStateList.valueOf(Color.argb(42, 255, 255, 255)),
                content,
                null
        );
    }

    static TextView text(Context context, String value, float sizeSp, int color) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        return view;
    }

    static Button button(Context context, String label, boolean primary) {
        Button view = new Button(context);
        view.setText(label);
        view.setTextColor(primary ? BG : TEXT_PRIMARY);
        view.setTextSize(14);
        view.setGravity(Gravity.CENTER);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        view.setAllCaps(false);
        view.setMinHeight(0);
        view.setMinWidth(0);
        view.setPadding(dp(context, 12), 0, dp(context, 12), 0);
        view.setBackground(ripple(context, primary ? PRIMARY : SURFACE_RAISED, 14));
        return view;
    }

    static Button chip(Context context, String label, boolean selected) {
        Button view = button(context, label, false);
        view.setTextSize(13);
        view.setTextColor(selected ? TEXT_PRIMARY : TEXT_SECONDARY);
        view.setBackground(rippleStroke(
                context,
                selected ? PRIMARY_DARK : SURFACE_SOFT,
                16,
                selected ? PRIMARY : DIVIDER
        ));
        return view;
    }

    static void tintSwitch(Switch toggle) {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        toggle.setThumbTintList(new ColorStateList(
                states,
                new int[]{PRIMARY, Color.rgb(190, 198, 214)}
        ));
        toggle.setTrackTintList(new ColorStateList(
                states,
                new int[]{PRIMARY_DARK, Color.rgb(62, 72, 96)}
        ));
    }

    static void tintSeekBar(SeekBar seekBar) {
        seekBar.setProgressTintList(ColorStateList.valueOf(PRIMARY));
        seekBar.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(55, 65, 91)));
        seekBar.setThumbTintList(ColorStateList.valueOf(PRIMARY));
    }

    static int panelWidth(Activity activity, int maxDp, int sideMarginDp) {
        int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        return Math.min(
                screenWidth - dp(activity, sideMarginDp * 2),
                dp(activity, maxDp)
        );
    }

    static void applyFloatingWindow(
            Activity activity,
            int gravity,
            int maxWidthDp,
            int sideMarginDp,
            int yDp,
            float dimAmount
    ) {
        Window window = activity.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.gravity = gravity;
        attributes.y = dp(activity, yDp);
        attributes.dimAmount = dimAmount;
        window.setAttributes(attributes);
        window.setLayout(
                panelWidth(activity, maxWidthDp, sideMarginDp),
                WindowManager.LayoutParams.WRAP_CONTENT
        );
    }

    static View dragHandle(Context context) {
        View handle = new View(context);
        handle.setBackground(rounded(context, Color.rgb(85, 96, 120), 4));
        return handle;
    }
}
