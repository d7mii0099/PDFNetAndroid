package com.pdftron.pdf.widget.toolbar.component.view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;

import com.pdftron.pdf.widget.toolbar.builder.AnnotationToolbarBuilder;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarItem;

/**
 * Class that contains the views related to the new annotation toolbar,
 * and exposes necessary Annotation Toolbar related API.
 */
public class AnnotationToolbarView {

    protected ViewGroup mParent;
    protected ActionToolbar mActionToolbar;
    @Nullable
    private AnnotationToolbarBuilder mPreviousToolbarBuilder;

    public AnnotationToolbarView(@NonNull ViewGroup parent) {
        mParent = parent;

        mActionToolbar = getActionToolbar(parent.getContext());
        mActionToolbar.setVisibility(View.GONE);
        mActionToolbar.setLayoutParams(parent.getLayoutParams());
        parent.addView(mActionToolbar);
    }

    protected ActionToolbar getActionToolbar(@NonNull Context context) {
        return new ActionToolbar(context);
    }

    public void addOnMenuItemClickListener(Toolbar.OnMenuItemClickListener onMenuItemClickListener) {
        mActionToolbar.addOnMenuItemClickListener(onMenuItemClickListener);
    }

    public void addOnButtonLongClickListener(View.OnLongClickListener onLongClickListener) {
        mActionToolbar.addOnButtonLongClickListener(onLongClickListener);
    }

    public void setCompactMode(boolean compactMode) {
        mActionToolbar.setCompactMode(compactMode);
    }

    public void setToolbarHeight(int height) {
        mActionToolbar.setMinimumHeight(height);
    }

    public void setNavigationIcon(@DrawableRes int icon) {
        mActionToolbar.setNavigationIcon(icon);
    }

    public void setNavigationIconVisible(boolean visible) {
        mActionToolbar.setNavigationIconVisible(visible);
    }

    public void setToolbarSwitcherVisible(boolean visible) {
        mActionToolbar.setToolbarSwitcherVisible(visible);
    }

    public void setNavigationIconProperty(int paddingLeft, int minWidth) {
        mActionToolbar.setNavigationIconProperty(paddingLeft, minWidth);
    }

    public void selectToolbarButtonIfAvailable(int buttonId) {
        mActionToolbar.selectToolbarButtonIfAvailable(buttonId);
    }

    public void toggleToolbarButtons(@NonNull ToolbarItem toolbarItem) {
        mActionToolbar.toggleToolbarButtons(toolbarItem.buttonId);
    }

    public void deselectAllToolbarButtons() {
        mActionToolbar.deselectAllTools();
    }

    public void updateTheme() {
        mActionToolbar.updateTheme();
    }

    public void inflateWithBuilder(AnnotationToolbarBuilder builder) {
        if (mPreviousToolbarBuilder != null && mPreviousToolbarBuilder.equals(builder)) {
            return;
        }
        mActionToolbar.inflateWithBuilder(builder);
        mPreviousToolbarBuilder = builder;
    }

    public void clearPreviousToolbarBuilder() {
        mPreviousToolbarBuilder = null;
    }

    public boolean isShown() {
        return mActionToolbar.isShown();
    }

    public int getHeight() {
        return mActionToolbar.getHeight();
    }

    public void hide(boolean animate) {
        if (mActionToolbar.getVisibility() != View.VISIBLE) { // already hidden
            return;
        }
        if (animate) {
            mActionToolbar.animate().translationY(-mActionToolbar.getHeight())
                    .setDuration(100)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            mActionToolbar.setVisibility(View.GONE);
                        }
                    })
                    .start();
        } else {
            mActionToolbar.setVisibility(View.GONE);
        }
    }

    public void show(boolean animate) {
        if (mActionToolbar.getVisibility() != View.GONE) { // already showing
            return;
        }
        if (animate) {
            mActionToolbar.animate().translationY(0)
                    .setDuration(100)
                    .setInterpolator(new DecelerateInterpolator())
                    .withStartAction(new Runnable() {
                        @Override
                        public void run() {
                            mActionToolbar.setVisibility(View.VISIBLE);
                        }
                    })
                    .start();
        } else {
            mActionToolbar.setVisibility(View.VISIBLE);
        }
    }

    public void addToolbarOverlay(@NonNull View view) {
        mActionToolbar.addToolbarOverlay(view);
    }

    public void addToolbarLeftOptionalContainer(@NonNull View view) {
        mActionToolbar.addToolbarLeftOptionalContainer(view);
    }

    public void addToolbarActionsRightOptionalContainer(@NonNull View view) {
        mActionToolbar.addToolbarActionsRightOptionalContainer(view);
    }

    public void setToolRegionVisible(boolean visible) {
        mActionToolbar.setToolRegionVisible(visible);
    }

    public void setEmptyToolText(@StringRes int emptyText) {
        mActionToolbar.setEmptyToolText(emptyText);
    }

    public void setEmptyToolTextVisible(boolean visible) {
        mActionToolbar.setEmptyToolTextVisible(visible);
    }

    public void setEmptyToolTextOnClickListener(@Nullable View.OnClickListener listener) {
        mActionToolbar.setEmptyToolTextOnClickListener(listener);
    }

    public FrameLayout getPresetContainer() {
        return mActionToolbar.getPresetContainer();
    }

    public void clearToolbarOverlayView() {
        mActionToolbar.clearToolbarOverlayView();
    }

    public void clearOptionContainers() {
        mActionToolbar.clearToolbarOverlayView();
        mActionToolbar.clearOptionalContainers();
    }

    public void updateAccentButton(int buttonId, @ColorInt int color, @IntRange(from = 0, to = 255) int alpha) {
        mActionToolbar.updateAccentButton(buttonId, color, alpha);
    }

    public void setItemEnabled(int buttonId, boolean isEnabled) {
        mActionToolbar.setItemEnabled(buttonId, isEnabled);
    }

    public void setItemAppearanceEnabled(int buttonId, boolean isAppearanceEnabled) {
        mActionToolbar.setItemAppearanceEnabled(buttonId, isAppearanceEnabled);
    }

    public void setItemVisibility(int buttonId, boolean isVisible) {
        mActionToolbar.setItemVisibility(buttonId, isVisible);
    }

    public Context getContext() {
        return mParent.getContext();
    }

    public void setToolbarItemGravity(int layoutGravity) {
        mActionToolbar.setToolbarItemGravity(layoutGravity);
    }

    public void setSelectedIconColor(int buttonId, @ColorInt int iconColor) {
        mActionToolbar.setSelectedIconColor(buttonId, iconColor);
    }

    public void setIconColor(int buttonId, @ColorInt int iconColor) {
        mActionToolbar.setIconColor(buttonId, iconColor);
    }

    public void disableAllItems() {
        mActionToolbar.disableAllItems();
    }
}
