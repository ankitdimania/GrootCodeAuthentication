package com.grootcode.android.auth.util;

import static com.grootcode.android.util.LogUtils.LOGD;
import static com.grootcode.android.util.LogUtils.LOGE;
import static com.grootcode.android.util.LogUtils.LOGI;
import static com.grootcode.android.util.LogUtils.LOGW;
import static com.grootcode.android.util.LogUtils.makeLogTag;

import java.io.IOException;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.UserRecoverableNotifiedException;
import com.google.android.gms.common.Scopes;
import com.grootcode.android.auth.ui.AccountActivity;
import com.grootcode.base.util.ContractUtils;

/**
 * declare AccountActivity in Manifest.xml
 * declare permission GET_ACCOUNTS, ACCESS_NETWORK_STATE, WRITE_SYNC_SETTINGS
 * 
 * <!-- Google Play Service Version -->
 * <meta-data
 * android:name="com.google.android.gms.version"
 * android:value="@integer/google_play_services_version" />
 * update res/xml/syncadapter.xml
 * init ContractUtils from App
 * extend database
 * 
 * describe
 * description_sign_in_main string
 * description_choose_account string
 * actionbar_logo drawable
 * 
 * declare style
 * 
 * <style name="Theme.GrootStockMaths.Account" parent="Theme.GrootStockMaths.Base">
 * <item name="android:actionBarStyle">@style/ActionBar.Account</item>
 * <item name="actionBarStyle">@style/ActionBar.Account</item>
 * </style>
 * 
 * <style name="ActionBar.Account" parent="ActionBar">
 * <item name="android:displayOptions">showHome|showTitle</item>
 * <item name="displayOptions">showHome|showTitle</item>
 * </style>
 * 
 * @author AnkitD
 * 
 */
public class AccountUtils {
    private static final String TAG = makeLogTag(AccountUtils.class);

    private static final String PREF_CHOSEN_ACCOUNT = "chosen_account";
    private static final String PREF_AUTH_TOKEN = "auth_token";
    private static final String PREF_PLUS_PROFILE_ID = "plus_profile_id";

    public static final String AUTH_SCOPES[] = {
            Scopes.PLUS_LOGIN,
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/developerssite",
    };

    static final String AUTH_TOKEN_TYPE;

    static {
        StringBuilder sb = new StringBuilder();
        sb.append("oauth2:");
        for (String scope : AUTH_SCOPES) {
            sb.append(scope);
            sb.append(" ");
        }
        AUTH_TOKEN_TYPE = sb.toString();
    }

    public static boolean isAuthenticated(final Context context) {
        return !TextUtils.isEmpty(getChosenAccountName(context));
    }

    public static String getChosenAccountName(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getString(PREF_CHOSEN_ACCOUNT, null);
    }

    public static Account getChosenAccount(final Context context) {
        String account = getChosenAccountName(context);
        if (account != null) {
            return new Account(account, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
        } else {
            return null;
        }
    }

    static void setChosenAccountName(final Context context, final String accountName) {
        LOGD(TAG, "Chose account " + accountName);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putString(PREF_CHOSEN_ACCOUNT, accountName).commit();
    }

    public static String getAuthToken(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getString(PREF_AUTH_TOKEN, null);
    }

    private static void setAuthToken(final Context context, final String authToken) {
        LOGI(TAG, "Auth token of length " + (TextUtils.isEmpty(authToken) ? 0 : authToken.length()));
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putString(PREF_AUTH_TOKEN, authToken).commit();
        LOGD(TAG, "Auth Token: " + authToken);
    }

    static void invalidateAuthToken(final Context context) {
        GoogleAuthUtil.invalidateToken(context, getAuthToken(context));
        setAuthToken(context, null);
    }

    public static void setPlusProfileId(final Context context, final String profileId) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putString(PREF_PLUS_PROFILE_ID, profileId).commit();
    }

    public static String getPlusProfileId(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getString(PREF_PLUS_PROFILE_ID, null);
    }

    public static void refreshAuthToken(Context mContext) {
        invalidateAuthToken(mContext);
        tryAuthenticateWithErrorNotification(mContext, ContractUtils.CONTENT_AUTHORITY, getChosenAccountName(mContext));
    }

    public static interface AuthenticateCallback {
        public boolean shouldCancelAuthentication();

        public void onAuthTokenAvailable();

        public void onRecoverableException(final int code);

        public void onUnRecoverableException(final String errorMessage);
    }

    static void tryAuthenticateWithErrorNotification(Context context, String syncAuthority, String accountName) {
        try {
            LOGI(TAG, "Requesting new auth token (with notification)");
            final String token = GoogleAuthUtil.getTokenWithNotification(context, accountName, AUTH_TOKEN_TYPE, null,
                    syncAuthority, null);
            setAuthToken(context, token);
            setChosenAccountName(context, accountName);

        } catch (UserRecoverableNotifiedException e) {
            // Notification has already been pushed.
            LOGW(TAG, "User recoverable exception. Check notification.", e);
        } catch (GoogleAuthException e) {
            // This is likely unrecoverable.
            LOGE(TAG, "Unrecoverable authentication exception: " + e.getMessage(), e);
        } catch (IOException e) {
            LOGE(TAG, "transient error encountered: " + e.getMessage());
        }
    }

    public static void tryAuthenticate(final Activity activity, final AuthenticateCallback callback,
            final String accountName, final int requestCode) {
        (new GetTokenTask(activity, callback, accountName, requestCode)).execute();
    }

    private static class GetTokenTask extends AsyncTask<Void, Void, String> {
        private final String mAccountName;
        private final Activity mActivity;
        private final AuthenticateCallback mCallback;
        private final int mRequestCode;

        public GetTokenTask(Activity activity, AuthenticateCallback callback, String name, int requestCode) {
            mAccountName = name;
            mActivity = activity;
            mCallback = callback;
            mRequestCode = requestCode;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                if (mCallback.shouldCancelAuthentication())
                    return null;

                final String token = GoogleAuthUtil.getToken(mActivity, mAccountName, AUTH_TOKEN_TYPE);
                // Persists auth token.
                setAuthToken(mActivity, token);
                setChosenAccountName(mActivity, mAccountName);
                return token;
            } catch (GooglePlayServicesAvailabilityException e) {
                mCallback.onRecoverableException(e.getConnectionStatusCode());
            } catch (UserRecoverableAuthException e) {
                mActivity.startActivityForResult(e.getIntent(), mRequestCode);
            } catch (IOException e) {
                LOGE(TAG, "transient error encountered: " + e.getMessage());
                mCallback.onUnRecoverableException(e.getMessage());
            } catch (GoogleAuthException e) {
                LOGE(TAG, "transient error encountered: " + e.getMessage());
                mCallback.onUnRecoverableException(e.getMessage());
            } catch (RuntimeException e) {
                LOGE(TAG, "Error encountered: " + e.getMessage());
                mCallback.onUnRecoverableException(e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String token) {
            super.onPostExecute(token);
            mCallback.onAuthTokenAvailable();
        }
    }

    public static void signOut(final Context context) {
        // Sign out of GCM message router
        // ServerUtilities.onSignOut(context);

        // Destroy auth tokens
        invalidateAuthToken(context);

        // Remove remaining application state
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().clear().commit();
        context.getContentResolver().delete(ContractUtils.BASE_CONTENT_URI, null, null);
    }

    public static void startAuthenticationFlow(final Context context, final Intent finishIntent) {
        Intent loginFlowIntent = new Intent(context, AccountActivity.class);
        loginFlowIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        loginFlowIntent.putExtra(AccountActivity.EXTRA_FINISH_INTENT, finishIntent);
        context.startActivity(loginFlowIntent);
    }
}
