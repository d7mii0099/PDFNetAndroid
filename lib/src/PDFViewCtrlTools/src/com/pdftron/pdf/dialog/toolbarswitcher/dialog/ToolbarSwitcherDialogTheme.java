package com.pdftron.pdf.dialog.toolbarswitcher.dialog;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.pdftron.pdf.tools.R;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ToolbarSwitcherDialogTheme {
    @ColorInt
    public final int textColor;
    @ColorInt
    public final int iconColor;
    @ColorInt
    public final int backgroundColor;
    @ColorInt
    public final int selectedItemBackgroundColor;
    @ColorInt
    public final int selectedIconColor;

    public ToolbarSwitcherDialogTheme(int textColor, int iconColor, int backgroundColor, int selectedItemBackgroundColor, int selectedIconColor) {
        this.textColor = textColor;
        this.iconColor = iconColor;
        this.backgroundColor = backgroundColor;
        this.selectedItemBackgroundColor = selectedItemBackgroundColor;
        this.selectedIconColor = selectedIconColor;
    }

    public static ToolbarSwitcherDialogTheme fromContext(@NonNull Context context) {

        final TypedArray a = context.obtainStyledAttributes(
                null, R.styleable.ToolbarSwitcherDialogTheme, R.attr.pt_toolbar_switcher_dialog_style, R.style.PTToolbarSwitcherDialogTheme);
        int textColor = a.getColor(R.styleable.ToolbarSwitcherDialogTheme_textColor, context.getResources().getColor(R.color.toolbar_switcher_item_dialog_text));
        int iconColor = a.getColor(R.styleable.ToolbarSwitcherDialogTheme_iconColor, context.getResources().getColor(R.color.toolbar_switcher_item_dialog_icon));
        int backgroundColor = a.getColor(R.styleable.ToolbarSwitcherDialogTheme_backgroundColor, context.getResources().getColor(R.color.toolbar_switcher_dialog_background));
        int selectedItemBackgroundColor = a.getColor(R.styleable.ToolbarSwitcherDialogTheme_selectedItemBackgroundColor, context.getResources().getColor(R.color.toolbar_switcher_item_dialog_selected_background));
        int selectedIconColor = a.getColor(R.styleable.ToolbarSwitcherDialogTheme_selectedIconColor, context.getResources().getColor(R.color.toolbar_switcher_item_selected_icon));
        a.recycle();

        return new ToolbarSwitcherDialogTheme(textColor, iconColor, backgroundColor, selectedItemBackgroundColor, selectedIconColor);
    }
}
