package com.pdftron.pdf.dialog.menueditor.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class MenuEditorItemHeader implements MenuEditorItem {

    private String mTitle;
    private @StringRes
    int mTitleId;
    private String mDescription;
    private @StringRes
    int mDescriptionId;
    private int mGroup;

    private String mDraggingTitle;
    private @StringRes
    int mDraggingTitleId;

    public MenuEditorItemHeader(int group, @NonNull String title, @Nullable String description) {
        mGroup = group;
        mTitle = title;
        mDescription = description;
    }

    public MenuEditorItemHeader(int group, @StringRes int titleId, @StringRes int descriptionId) {
        mGroup = group;
        mTitleId = titleId;
        mDescriptionId = descriptionId;
    }

    public void setDraggingTitle(@NonNull String title) {
        mDraggingTitle = title;
    }

    public void setDraggingTitleId(@StringRes int titleId) {
        mDraggingTitleId = titleId;
    }

    @Override
    public boolean isHeader() {
        return true;
    }

    @Nullable
    public String getTitle() {
        return mTitle;
    }

    public @StringRes
    int getTitleId() {
        return mTitleId;
    }

    @Nullable
    public String getDescription() {
        return mDescription;
    }

    public @StringRes
    int getDescriptionId() {
        return mDescriptionId;
    }

    @Nullable
    public String getDraggingTitle() {
        return mDraggingTitle;
    }

    public @StringRes
    int getDraggingTitleId() {
        return mDraggingTitleId;
    }

    public int getGroup() {
        return mGroup;
    }
}
