//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.app;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.pdftron.demo.R;
import com.pdftron.demo.app.setting.SettingsFragment;
import com.pdftron.pdf.utils.Utils;

/**
 * Settings dialog used in the CompleteReader demo app.
 */
public class SettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String TITLE_TAG = "settingsActivityTitle";
    @Nullable
    protected ActionBar mActionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Utils.applyDayNight(this)) {
            return;
        }

        setContentView(getLayoutResId());

        getSupportFragmentManager()
                .beginTransaction()
                .replace(getContainerId(), getFragment())
                .commit();
        if (savedInstanceState != null) {
            setTitle(savedInstanceState.getCharSequence(TITLE_TAG));
        }

        Toolbar toolbar = findViewById(R.id.tool_bar);
        if (toolbar != null) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
            setSupportActionBar(toolbar);
        }

        mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setTitle(getResources().getString(getTitleRes()));
        }

        initBackStackChangedListener();
    }

    protected void initBackStackChangedListener() {
        getSupportFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    @Override
                    public void onBackStackChanged() {
                        if (mActionBar != null && getSupportFragmentManager().getBackStackEntryCount() == 0) {
                            mActionBar.setTitle(getResources().getString(getTitleRes()));
                        }
                    }
                });
    }

    protected int getTitleRes() {
        return R.string.action_settings;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, getTitle());
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate())
            return true;
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        getSupportFragmentManager().beginTransaction()
                .replace(getContainerId(), fragment)
                .addToBackStack(null)
                .commit();
        if (mActionBar != null) {
            mActionBar.setTitle(pref.getTitle());
        }
        return true;
    }

    protected Fragment getFragment() {
        return new SettingsFragment();
    }

    @LayoutRes
    protected int getLayoutResId() {
        return R.layout.activity_complete_reader_settings;
    }

    @IdRes
    protected int getContainerId() {
        return R.id.settings_container;
    }

    public static void setPreferencesDefaultValues(Context context) {
        PreferenceManager.setDefaultValues(context, R.xml.setting_general_preferences, false);
        PreferenceManager.setDefaultValues(context, R.xml.setting_viewing_preferences, false);
        PreferenceManager.setDefaultValues(context, R.xml.setting_tabs_preferences, false);
        PreferenceManager.setDefaultValues(context, R.xml.setting_annotating_preferences, false);
        PreferenceManager.setDefaultValues(context, R.xml.setting_stylus_preferences, false);
        PreferenceManager.setDefaultValues(context, R.xml.setting_about_preferences, false);
    }
}

