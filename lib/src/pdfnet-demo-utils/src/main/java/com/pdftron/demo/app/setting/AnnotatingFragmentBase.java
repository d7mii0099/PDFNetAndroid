package com.pdftron.demo.app.setting;

import android.content.Context;
import android.os.Bundle;

import androidx.preference.Preference;

import com.pdftron.demo.R;
import com.pdftron.demo.utils.SettingsManager;

public class AnnotatingFragmentBase extends SettingFragmentBase {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.setting_annotating_preferences, rootKey);
        setupContinuousAnnotationEdit(getContext());
    }

    protected void setupContinuousAnnotationEdit(Context context) {
        Preference contAnnotationEditPref = findPreference(SettingsManager.KEY_PREF_CONT_ANNOT_EDIT);
        final Preference annotationSelectionPref = findPreference(SettingsManager.KEY_PREF_SHOW_QUICK_MENU);

        if (context != null && annotationSelectionPref != null) {
            if (!SettingsManager.getContinuousAnnotationEdit(context)) {
                annotationSelectionPref.setEnabled(false);
            }
        }
        if (contAnnotationEditPref != null) {
            contAnnotationEditPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (annotationSelectionPref != null) {
                        annotationSelectionPref.setEnabled(newValue.toString().equals("true"));
                    }
                    return true;
                }
            });
        }
    }
}
