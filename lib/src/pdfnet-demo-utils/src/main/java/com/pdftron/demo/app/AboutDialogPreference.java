//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.app;

import android.content.Context;
import android.util.AttributeSet;

import com.pdftron.demo.R;
import com.pdftron.demo.utils.SettingsManager;

import androidx.preference.DialogPreference;

/**
 * About dialog used in the CompleteReader demo app.
 */
public class AboutDialogPreference extends DialogPreference {

    public AboutDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public AboutDialogPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AboutDialogPreference(Context context) {
        this(context, null);
    }

    protected void init() {
        setDialogLayoutResource(R.layout.dialog_about);
        setDialogTitle(R.string.about);
        setSummary(getAboutSummary());
        setPositiveButtonText(R.string.ok);
        setNegativeButtonText(null);
    }

    protected String getAboutSummary() {
        Context context = getContext();

        StringBuilder summary = new StringBuilder();

        if (context != null) {
            summary.append(context.getString(R.string.app_name));
            summary.append(context.getString(R.string.registered_trademark));
            String versionName = SettingsManager.getAppVersionName(context);
            if (!versionName.isEmpty()) {
                summary.append(" " + versionName);
            }
        }

        return summary.toString();
    }
}
