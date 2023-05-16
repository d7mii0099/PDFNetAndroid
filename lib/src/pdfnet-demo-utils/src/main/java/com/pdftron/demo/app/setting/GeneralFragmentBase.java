package com.pdftron.demo.app.setting;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.pdftron.demo.R;
import com.pdftron.demo.utils.SettingsManager;
import com.pdftron.pdf.utils.Utils;

public class GeneralFragmentBase extends SettingFragmentBase {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.setting_general_preferences, rootKey);
        checkFullScreenModeVisibility();
        checkFollowSystemDarkModeVisibility();
    }

    protected void checkFullScreenModeVisibility() {
        PreferenceCategory general = findPreference("pref_category_general");
        Preference prefFullScreen = findPreference(SettingsManager.KEY_PREF_FULL_SCREEN_MODE);
        if (general != null && prefFullScreen != null) {
            if (!Utils.isKitKat()) {
                general.removePreference(prefFullScreen);
            }
        }
    }

    private void checkFollowSystemDarkModeVisibility() {
        PreferenceCategory general = findPreference("pref_category_general");
        Preference prefFollowSystemDark = findPreference(SettingsManager.KEY_PREF_FOLLOW_SYSTEM_DARK_MODE);
        if (general != null && prefFollowSystemDark != null) {
            if (!Utils.isPie()) {
                general.removePreference(prefFollowSystemDark);
            }
        }
    }
}
