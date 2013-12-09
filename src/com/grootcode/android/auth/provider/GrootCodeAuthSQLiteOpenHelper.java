package com.grootcode.android.auth.provider;

import static com.grootcode.android.util.LogUtils.LOGD;
import static com.grootcode.android.util.LogUtils.LOGI;
import static com.grootcode.android.util.LogUtils.makeLogTag;
import android.accounts.Account;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.os.Build;

import com.grootcode.android.auth.util.AccountUtils;
import com.grootcode.android.provider.GrootCodeContractBase;
import com.grootcode.android.provider.GrootCodeSQLiteOpenHelper;
import com.grootcode.base.sync.SyncHelper;

public abstract class GrootCodeAuthSQLiteOpenHelper extends GrootCodeSQLiteOpenHelper {
    private static final String TAG = makeLogTag(GrootCodeAuthSQLiteOpenHelper.class);

    private final Context mContext;

    public GrootCodeAuthSQLiteOpenHelper(Context context, String name, CursorFactory factory, int version) {
        super(context, name, factory, version);
        mContext = context;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public GrootCodeAuthSQLiteOpenHelper(Context context, String name, CursorFactory factory, int version,
            DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
        mContext = context;
    }

    @Override
    public final void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        LOGD(TAG, "onUpgrade() from " + oldVersion + " to " + newVersion);

        // Cancel any sync currently in progress
        Account account = AccountUtils.getChosenAccount(mContext);
        if (account != null) {
            LOGI(TAG, "Cancelling any pending syncs for for account");
            ContentResolver.cancelSync(account, GrootCodeContractBase.CONTENT_AUTHORITY);
        }

        onUpgradeInternal(db, oldVersion, newVersion);

        if (account != null) {
            LOGI(TAG, "DB upgrade complete. Requesting resync.");
            SyncHelper.requestManualSync(account);
        }
    }

    protected abstract void onUpgradeInternal(SQLiteDatabase db, int oldVersion, int newVersion);
}
