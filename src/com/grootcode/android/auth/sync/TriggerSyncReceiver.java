package com.grootcode.android.auth.sync;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.grootcode.android.auth.util.AccountUtils;
import com.grootcode.android.provider.GrootCodeContractBase;

/**
 * A simple {@link BroadcastReceiver} that triggers a sync. This is used by the GCM code to trigger
 * jittered syncs using {@link android.app.AlarmManager}.
 */
public class TriggerSyncReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String accountName = AccountUtils.getChosenAccountName(context);
        if (TextUtils.isEmpty(accountName)) {
            return;
        }

        ContentResolver.requestSync(AccountUtils.getChosenAccount(context), GrootCodeContractBase.CONTENT_AUTHORITY,
                new Bundle());
    }
}
