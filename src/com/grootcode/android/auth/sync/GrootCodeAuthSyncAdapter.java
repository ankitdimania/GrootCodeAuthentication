package com.grootcode.android.auth.sync;

import static com.grootcode.android.util.LogUtils.LOGE;
import static com.grootcode.android.util.LogUtils.LOGI;
import static com.grootcode.android.util.LogUtils.LOGW;
import static com.grootcode.android.util.LogUtils.makeLogTag;

import java.io.IOException;
import java.util.regex.Pattern;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.text.TextUtils;

import com.grootcode.android.auth.util.AccountUtils;
import com.grootcode.android.util.BuildConfigUtils;

/**
 * Sync adapter Base
 */
public abstract class GrootCodeAuthSyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = makeLogTag(GrootCodeAuthSyncAdapter.class);

    private static final Pattern sSanitizeAccountNamePattern = Pattern.compile("(.).*?(.?)@");

    private final Context mContext;

    public GrootCodeAuthSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContext = context;

        // noinspection ConstantConditions,PointlessBooleanExpression
        if (!BuildConfigUtils.BUILD_CONFIG_DEBUG) {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable throwable) {
                    LOGE(TAG, "Uncaught sync exception, suppressing UI in release build.", throwable);
                }
            });
        }
    }

    @Override
    public void onPerformSync(final Account account, Bundle extras, String authority,
            final ContentProviderClient provider, final SyncResult syncResult) {
        final boolean uploadOnly = extras.getBoolean(ContentResolver.SYNC_EXTRAS_UPLOAD, false);
        final boolean manualSync = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
        final boolean initialize = extras.getBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE, false);

        final String logSanitizedAccountName = sSanitizeAccountNamePattern.matcher(account.name).replaceAll("$1...$2@");

        if (uploadOnly) {
            return;
        }

        LOGI(TAG, "Beginning sync for account " + logSanitizedAccountName + "," + " uploadOnly=" + uploadOnly
                + " manualSync=" + manualSync + " initialize=" + initialize);

        String chosenAccountName = AccountUtils.getChosenAccountName(mContext);
        boolean isAccountSet = !TextUtils.isEmpty(chosenAccountName);
        boolean isChosenAccount = isAccountSet && chosenAccountName.equals(account.name);
        if (isAccountSet) {
            ContentResolver.setIsSyncable(account, authority, isChosenAccount ? 1 : 0);
        }
        if (!isChosenAccount) {
            LOGW(TAG, "Tried to sync account " + logSanitizedAccountName + " but the chosen " + "account is actually "
                    + chosenAccountName);
            ++syncResult.stats.numAuthExceptions;
            return;
        }

        try {
            performSync(syncResult);

        } catch (IOException e) {
            ++syncResult.stats.numIoExceptions;
            LOGE(TAG, "Error syncing data for I/O 2013.", e);
        }
    }

    protected abstract void performSync(SyncResult syncResult) throws IOException;
}
