package com.pdftron.pdf.dialog.reflow;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

public class ReflowAnnotEditItem {

    private final @DrawableRes int mIcon;
    private final @StringRes int mTitle;

    public ReflowAnnotEditItem(@DrawableRes int icon, @StringRes int title) {
        mIcon = icon;
        mTitle = title;
    }

    @DrawableRes
    public int getIcon() {
        return mIcon;
    }

    @StringRes
    public int getTitle() {
        return mTitle;
    }
}
