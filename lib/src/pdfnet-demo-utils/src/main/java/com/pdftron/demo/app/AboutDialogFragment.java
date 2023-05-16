package com.pdftron.demo.app;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.pdftron.common.PDFNetException;
import com.pdftron.demo.R;
import com.pdftron.demo.utils.SettingsManager;
import com.pdftron.pdf.PDFNet;

import androidx.preference.PreferenceDialogFragmentCompat;

public class AboutDialogFragment extends PreferenceDialogFragmentCompat {

    public static AboutDialogFragment newInstance(String key) {
        AboutDialogFragment fragment = new AboutDialogFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        TextView textView = (TextView) view.findViewById(R.id.dialog_about_textview);
        textView.setText(getAboutBody());
        textView.setMovementMethod(new LinkMovementMethod());
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {

    }

    protected SpannableStringBuilder getAboutBody() {
        Context context = getContext();

        StringBuilder aboutBody = new StringBuilder();

        if (context != null) {
            // App name
            aboutBody.append("<b>" + context.getResources().getString(R.string.app_name) + context.getString(R.string.registered_trademark) + "</b><br>");

            // App version
            try {
                aboutBody.append(context.getResources().getString(R.string.version) + " "
                        + SettingsManager.getAppVersionName(context) + " (" + PDFNet.getVersion() + ")<br>");
            } catch (PDFNetException e) {
                e.printStackTrace();
            }

            aboutBody.append("<br><br>");

            // About text
            aboutBody.append(context.getResources().getString(R.string.dialog_about_body));
        }

        SpannableStringBuilder aboutBodyFinal = new SpannableStringBuilder();
        aboutBodyFinal.append(Html.fromHtml(aboutBody.toString()));

        return aboutBodyFinal;
    }
}
