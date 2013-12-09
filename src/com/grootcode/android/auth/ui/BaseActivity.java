package com.grootcode.android.auth.ui;

import static com.grootcode.android.util.LogUtils.makeLogTag;
import android.app.Activity;
import android.os.Bundle;

import com.grootcode.android.auth.util.AccountUtils;
import com.grootcode.android.base.util.PlayServicesUtils;
import com.grootcode.android.util.LogUtils;

public class BaseActivity extends com.grootcode.base.ui.BaseActivity {
    private static final String TAG = makeLogTag(BaseActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isAuthenticated(this);
    }

    static void isAuthenticated(Activity activity) {

        if (!AccountUtils.isAuthenticated(activity)) {
            LogUtils.LOGD(TAG, "exiting:" + " isAuthenticated=" + AccountUtils.isAuthenticated(activity));
            AccountUtils.startAuthenticationFlow(activity, activity.getIntent());
            activity.finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Verifies the proper version of Google Play Services exists on the device.
        PlayServicesUtils.checkGooglePlaySevices(this);
    }
}
