package com.pdftron.pdf.dialog.tabswitcher.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class TabSwitcherItem {

    @NonNull
    private String mTabTag;
    @NonNull
    private String mTitle;
    @Nullable
    private String mPreviewPath;

    public TabSwitcherItem(@NonNull String tabTag, @NonNull String title, @Nullable String previewPath) {
        mTabTag = tabTag;
        mTitle = title;
        mPreviewPath = previewPath;
    }

    @Nullable
    public String getPreviewPath() {
        return mPreviewPath;
    }

    @NonNull
    public String getTabTag() {
        return mTabTag;
    }

    @NonNull
    public String getTitle() {
        return mTitle;
    }
}
