package com.pdftron.pdf.widget.toolbar.builder;

import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class QuickMenuToolbarItem extends ToolbarItem {

    public static String QUICK_MENU_TOOL_STYLE_ID = "";

    public QuickMenuToolbarItem(@NonNull String toolbarId, @NonNull ToolbarButtonType toolbarButtonType, int buttonId, boolean isCheckable, int title, int icon, int showAsAction, boolean isVisible, int order) {
        super(toolbarId, toolbarButtonType, buttonId, isCheckable, false, title, null, icon, showAsAction, isVisible, order);
    }

    public QuickMenuToolbarItem(@NonNull String toolbarId, @NonNull ToolbarButtonType toolbarButtonType, int buttonId, boolean isCheckable, int title, int icon, int showAsAction, int order) {
        super(toolbarId, toolbarButtonType, buttonId, isCheckable, title, icon, showAsAction, order);
    }

    protected QuickMenuToolbarItem(Parcel in) {
        super(in);
    }

    @Override
    public String getStyleId() {
        // Do not use a unique style id, instead use the default empty one.
        return QUICK_MENU_TOOL_STYLE_ID;
    }
}
