package com.pdftron.demo.app.setting;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.XmlRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.pdftron.demo.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    private Theme mTheme;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(getXmlRes(), rootKey);
        setupMenuIcons();
        setupSearchSetting();
    }

    protected void setupSearchSetting() {
        Preference prefSearchSetting = findPreference("search_setting");
        if (prefSearchSetting != null) {
            prefSearchSetting.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    FragmentActivity activity = getActivity();
                    if (activity != null) {
                        activity.getSupportFragmentManager().beginTransaction()
                                .replace(getContainerId(), getSettingsFragment())
                                .addToBackStack(null)
                                .commit();
                    }
                    return true;
                }
            });
        }
    }

    protected void setupMenuIcons() {
        mTheme = SettingsFragment.Theme.fromContext(getActivity());

        Preference generalPref = findPreference("pref_setting_general");
        if (generalPref != null) {
            generalPref.getIcon().mutate().setColorFilter(new PorterDuffColorFilter(mTheme.iconColor, PorterDuff.Mode.SRC_ATOP));
        }
        Preference viewingPref = findPreference("pref_setting_viewing");
        if (viewingPref != null) {
            viewingPref.getIcon().mutate().setColorFilter(new PorterDuffColorFilter(mTheme.iconColor, PorterDuff.Mode.SRC_ATOP));
        }
        Preference tabsPref = findPreference("pref_setting_tabs");
        if (tabsPref != null) {
            tabsPref.getIcon().mutate().setColorFilter(new PorterDuffColorFilter(mTheme.iconColor, PorterDuff.Mode.SRC_ATOP));
        }
        Preference annotationPref = findPreference("pref_setting_annotation");
        if (annotationPref != null) {
            annotationPref.getIcon().mutate().setColorFilter(new PorterDuffColorFilter(mTheme.iconColor, PorterDuff.Mode.SRC_ATOP));
        }
        Preference stylusPref = findPreference("pref_setting_stylus");
        if (stylusPref != null) {
            stylusPref.getIcon().mutate().setColorFilter(new PorterDuffColorFilter(mTheme.iconColor, PorterDuff.Mode.SRC_ATOP));
        }
        Preference aboutPref = findPreference("pref_setting_about");
        if (aboutPref != null) {
            aboutPref.getIcon().mutate().setColorFilter(new PorterDuffColorFilter(mTheme.iconColor, PorterDuff.Mode.SRC_ATOP));
        }
    }

    @XmlRes
    protected int getXmlRes() {
        return R.xml.setting_preferences;
    }

    protected Fragment getSettingsFragment() {
        return new SearchSettingFragmentBase();
    }

    @IdRes
    protected int getContainerId() {
        return R.id.settings_container;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class Theme {
        @ColorInt
        public final int iconColor;

        public Theme(int iconColor) {
            this.iconColor = iconColor;
        }

        public static SettingsFragment.Theme fromContext(@NonNull Context context) {

            final TypedArray a = context.obtainStyledAttributes(
                    null, R.styleable.SettingsTheme, R.attr.pt_settings_style, R.style.PTSettingsTheme);
            int iconColor = a.getColor(R.styleable.SettingsTheme_iconColor,
                    context.getResources().getColor(R.color.controls_settings_icon));
            a.recycle();

            return new SettingsFragment.Theme(iconColor);
        }
    }
}
