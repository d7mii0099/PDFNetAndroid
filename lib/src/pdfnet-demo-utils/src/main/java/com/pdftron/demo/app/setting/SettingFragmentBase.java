package com.pdftron.demo.app.setting;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import static com.pdftron.demo.app.setting.SearchSettingFragmentBase.START_PREFERENCE;

public class SettingFragmentBase extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle bundle = this.getArguments();
        if (bundle != null && !bundle.isEmpty()) {
            String selectedPrefKey = bundle.getString(START_PREFERENCE);
            scrollToPreference(selectedPrefKey);
        }
    }
}
