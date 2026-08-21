package com.norahc.sleeptimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class FadeOutStartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        String source = intent.getStringExtra(FadeOutManager.EXTRA_SOURCE);
        long target = intent.getLongExtra(FadeOutManager.EXTRA_TARGET, 0L);
        if (source == null || target <= 0L) {
            return;
        }
        FadeOutManager.startExpectedFade(context.getApplicationContext(), source, target);
    }
}
