package com.pdftron.demo.app.setting;

import android.content.Context;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.pdftron.demo.R;
import com.pdftron.demo.utils.SettingsManager;
import com.pdftron.pdf.utils.Utils;

public class ViewingFragmentBase extends SettingFragmentBase {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.setting_viewing_preferences, rootKey);
        checkDesktopUIVisibility(getContext());
        checkScrollbarGuidelineVisibility();
    }

    protected void checkDesktopUIVisibility(Context context) {
        PreferenceCategory viewing = findPreference("pref_category_viewing");
        Preference prefDesktopUI = findPreference(SettingsManager.KEY_PREF_DESKTOP_UI_MODE);
        if (viewing != null && prefDesktopUI != null && context != null) {
            if (!Utils.isChromebook(context)) {
                viewing.removePreference(prefDesktopUI);
            }
        }
    }

    protected void checkScrollbarGuidelineVisibility() {
        PreferenceCategory viewing = findPreference("pref_category_viewing");
        Preference showScrollbarGuideline = findPreference(SettingsManager.KEY_PREF_SCROLLBAR_GUIDELINE);
        if (viewing != null && showScrollbarGuideline != null) {
            if (!Utils.isMarshmallow()) {
                viewing.removePreference(showScrollbarGuideline);
            }
        }
    }
}
