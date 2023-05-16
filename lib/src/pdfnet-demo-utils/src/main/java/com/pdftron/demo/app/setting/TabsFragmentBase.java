package com.pdftron.demo.app.setting;

import android.content.Context;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.pdftron.demo.R;
import com.pdftron.demo.utils.SettingsManager;
import com.pdftron.pdf.utils.Utils;

public class TabsFragmentBase extends SettingFragmentBase {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.setting_tabs_preferences, rootKey);
        checkTabBarVisibility(getContext());
    }

    protected void checkTabBarVisibility(Context context) {
        PreferenceCategory tabs = findPreference("pref_category_tabs");
        Preference showTabBar = findPreference(SettingsManager.KEY_PREF_NEW_UI_SHOW_TAB_BAR);
        if (tabs != null && showTabBar != null) {
            if (!Utils.isTablet(context)) {
                tabs.removePreference(showTabBar);
            }
        }

        Preference showTabBarPhone = findPreference(SettingsManager.KEY_PREF_NEW_UI_SHOW_TAB_BAR_PHONE);
        if (tabs != null && showTabBarPhone != null) {
            if (Utils.isTablet(context)) {
                tabs.removePreference(showTabBarPhone);
            }
        }
    }
}
