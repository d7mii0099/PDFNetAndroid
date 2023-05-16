package com.pdftron.demo.app.setting;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import com.pdftron.demo.R;
import com.pdftron.demo.app.AboutDialogFragment;
import com.pdftron.demo.app.AboutDialogPreference;

public class AboutFragmentBase extends SettingFragmentBase {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.setting_about_preferences, rootKey);
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        FragmentManager fragmentManager = getFragmentManager();
        if (preference instanceof AboutDialogPreference && fragmentManager != null) {
            DialogFragment fragment = AboutDialogFragment.newInstance(preference.getKey());
            fragment.setTargetFragment(this, 0);
            fragment.show(fragmentManager, "about_dialog");
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }
}
