package com.pdftron.demo.app.setting;

import android.content.Context;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.pdftron.demo.R;
import com.pdftron.demo.utils.SettingsManager;

public class StylusFragmentBase extends SettingFragmentBase {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.setting_stylus_preferences, rootKey);
        setupStylus(getContext());
    }

    protected void setupStylus(Context context) {
        Preference stylusAsPen = findPreference(SettingsManager.KEY_PREF_STYLUS_AS_PEN);
        final ListPreference stylusToolListPref = findPreference("default_stylus_tool_mode");
        final Preference drawWithFinger = findPreference("pref_draw_with_finger");

        if (context != null && stylusToolListPref != null && drawWithFinger != null) {
            if (!SettingsManager.getStylusAsPen(context)) {
                stylusToolListPref.setEnabled(false);
                drawWithFinger.setEnabled(false);
            }
        }

        if (stylusAsPen != null) {
            stylusAsPen.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (stylusToolListPref != null && drawWithFinger != null) {
                        stylusToolListPref.setEnabled(newValue.toString().equals("true"));
                        drawWithFinger.setEnabled(newValue.toString().equals("true"));
                    }
                    return true;
                }
            });
        }
    }
}
