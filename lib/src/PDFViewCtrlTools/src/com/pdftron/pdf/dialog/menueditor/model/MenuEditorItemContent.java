package com.pdftron.pdf.dialog.menueditor.model;

import android.graphics.drawable.Drawable;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.pdf.widget.toolbar.builder.ToolbarButtonType;

public class MenuEditorItemContent implements MenuEditorItem {

    private @IdRes int mId;
    private ToolbarButtonType mToolbarButtonType;
    private String mTitle;
    private @DrawableRes int mIconRes;
    private Drawable mDrawable;

    public MenuEditorItemContent(@IdRes int id, @NonNull String title, @DrawableRes int iconRes) {
        mId = id;
        mTitle = title;
        mIconRes = iconRes;
    }

    public MenuEditorItemContent(@IdRes int id, @NonNull String title, @NonNull Drawable drawable) {
        mId = id;
        mTitle = title;
        mDrawable = drawable;
    }

    public MenuEditorItemContent(@IdRes int id, @NonNull ToolbarButtonType toolbarButtonType, @NonNull String title, @NonNull Drawable drawable) {
        mId = id;
        mToolbarButtonType = toolbarButtonType;
        mTitle = title;
        mDrawable = drawable;
    }

    @Override
    public boolean isHeader() {
        return false;
    }

    public @IdRes int getId() {
        return mId;
    }

    @Nullable
    public ToolbarButtonType getToolbarButtonType() {
        return mToolbarButtonType;
    }

    public String getTitle() {
        return mTitle;
    }

    public @DrawableRes int getIconRes() {
        return mIconRes;
    }

    @Nullable
    public Drawable getDrawable() {
        return mDrawable;
    }
}
