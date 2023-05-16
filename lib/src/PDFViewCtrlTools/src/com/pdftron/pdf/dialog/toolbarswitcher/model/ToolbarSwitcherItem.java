package com.pdftron.pdf.dialog.toolbarswitcher.model;

import android.content.Context;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.pdf.widget.toolbar.builder.AnnotationToolbarBuilder;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarItem;

import java.util.List;

public class ToolbarSwitcherItem {

    @DrawableRes
    private int icon = -1;
    @Nullable
    public boolean isSelected;
    public final AnnotationToolbarBuilder builder;
    private boolean isVisible = true;

    @NonNull
    public String getTag() {
        return builder.getToolbarTag();
    }

    @DrawableRes
    public int getIcon() {
        if (icon == -1) {
            List<ToolbarItem> toolbarButtons = builder.getToolbarItems();
            if (!toolbarButtons.isEmpty()) {
                return toolbarButtons.get(0).icon;
            } else {
                return icon;
            }
        } else {
            return icon;
        }
    }

    public String getToolbarName(@NonNull Context context) {
        return builder.getToolbarName(context);
    }

    public List<ToolbarItem> getToolbarItems() {
        return builder.getToolbarItems();
    }

    public ToolbarSwitcherItem(AnnotationToolbarBuilder builder) {
        this.icon = builder.getToolbarIcon();
        this.builder = builder;
    }

    /**
     * Sets whether this toolbar switcher item is selected. Default false.
     * @param isSelected whether the toolbar switcher item is selected
     *
     * @return this ToolbarSwitcherItem instance
     */
    public ToolbarSwitcherItem setSelected(boolean isSelected) {
        this.isSelected = isSelected;
        return this;
    }

    /**
     * Sets whether this toolbar switcher item is visible. Default true.
     * @param isVisible whether the toolbar switcher item is visible
     *
     * @return this ToolbarSwitcherItem instance
     */
    public ToolbarSwitcherItem setVisibility(boolean isVisible) {
        this.isVisible = isVisible;
        return this;
    }

    /**
     * @return Whether this toolbar switcher item is visible/
     */
    public boolean isVisible() {
        return isVisible;
    }

}
