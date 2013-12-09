package com.grootcode.android.auth.ui;

import android.os.Bundle;

import com.grootcode.android.base.util.PlayServicesUtils;
import com.grootcode.android.ui.SimpleSectionedListAdapter.Section;

public class MainActivity extends com.grootcode.base.ui.MainActivity {

    public MainActivity(SlidingMenuItem[] menuItems, Section[] menuSections) {
        super(menuItems, menuSections);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BaseActivity.isAuthenticated(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Verifies the proper version of Google Play Services exists on the device.
        PlayServicesUtils.checkGooglePlaySevices(this);
    }
}
