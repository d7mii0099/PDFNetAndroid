package com.pdftron.pdf.widget.toolbar.component.view;

import android.graphics.drawable.Drawable;
import android.view.MenuItem;
import androidx.annotation.NonNull;

class OverflowButton implements ToolbarButton {

    private MenuItem mMenuItem;
    private boolean mIsCheckable;
    private boolean mIsChecked;
    private boolean mIsEnabled;

    OverflowButton(MenuItem menuItem) {
        mMenuItem = menuItem;
        mIsCheckable = menuItem.isCheckable();
    }

    @Override
    public void deselect() {
        if (mIsCheckable && mIsChecked) {
            mMenuItem.setChecked(false);
        }
        mIsChecked = false;
    }

    @Override
    public void select() {
        if (mIsCheckable && !mIsChecked) {
            mMenuItem.setChecked(true);
        }
        mIsChecked = true;
    }

    @Override
    public void enable() {
        mIsEnabled = true;
        mMenuItem.setEnabled(true);
    }

    @Override
    public void disable() {
        mIsEnabled = false;
        mMenuItem.setEnabled(false);
    }

    @Override
    public int getId() {
        return mMenuItem.getItemId();
    }

    @Override
    public boolean isCheckable() {
        return mIsCheckable;
    }

    @Override
    public void setCheckable(boolean isCheckable) {
        mIsCheckable = isCheckable;
    }

    @Override
    public boolean hasOption() {
        return false;
    }

    @Override
    public void setHasOption(boolean hasOption) {

    }

    @Override
    public boolean isSelected() {
        return mIsChecked;
    }

    @Override
    public void show() {
        mMenuItem.setVisible(true);
    }

    @Override
    public void hide() {
        mMenuItem.setVisible(false);
    }

    @Override
    public void setIcon(@NonNull Drawable drawable) {
        mMenuItem.setIcon(drawable);
    }
}
