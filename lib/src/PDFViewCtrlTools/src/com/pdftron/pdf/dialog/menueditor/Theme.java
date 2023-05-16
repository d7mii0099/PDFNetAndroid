package com.pdftron.pdf.dialog.menueditor;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.pdftron.pdf.tools.R;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class Theme {

    @ColorInt
    final int backgroundColor;
    @ColorInt
    final int headerColor;
    @ColorInt
    final int iconColor;
    @ColorInt
    final int textColor;
    @ColorInt
    final int pinnedBackgroundColor;
    @ColorInt
    final int dottedLineColor;
    @ColorInt
    final int dottedLineActiveColor;

    public Theme(int backgroundColor, int headerColor, int iconColor, int textColor, int pinnedBackgroundColor, int dottedLineColor, int dottedLineActiveColor) {
        this.backgroundColor = backgroundColor;
        this.headerColor = headerColor;
        this.iconColor = iconColor;
        this.textColor = textColor;
        this.pinnedBackgroundColor = pinnedBackgroundColor;
        this.dottedLineColor = dottedLineColor;
        this.dottedLineActiveColor = dottedLineActiveColor;
    }

    public static Theme fromContext(@NonNull Context context) {

        final TypedArray a = context.obtainStyledAttributes(
                null, R.styleable.MenuEditorDialogTheme, R.attr.pt_menu_editor_dialog_style, R.style.PTMenuEditorDialogTheme);
        int backgroundColor = a.getColor(R.styleable.MenuEditorDialogTheme_backgroundColor, context.getResources().getColor(R.color.menu_editor_background_color));
        int headerColor = a.getColor(R.styleable.MenuEditorDialogTheme_headerColor, context.getResources().getColor(R.color.menu_editor_icon_tint_dark));
        int iconColor = a.getColor(R.styleable.MenuEditorDialogTheme_iconColor, context.getResources().getColor(R.color.menu_editor_icon_tint_dark));
        int textColor = a.getColor(R.styleable.MenuEditorDialogTheme_textColor, context.getResources().getColor(R.color.menu_editor_icon_tint_dark));
        int pinnedBackgroundColor = a.getColor(R.styleable.MenuEditorDialogTheme_pinnedBackgroundColor, context.getResources().getColor(R.color.menu_editor_pinned_background_color));
        int dottedLineColor = a.getColor(R.styleable.MenuEditorDialogTheme_dottedLineColor, context.getResources().getColor(R.color.menu_editor_icon_tint_light));
        int dottedLineActiveColor = a.getColor(R.styleable.MenuEditorDialogTheme_dottedLineActiveColor, context.getResources().getColor(R.color.menu_editor_icon_tint));
        a.recycle();

        return new Theme(backgroundColor, headerColor, iconColor, textColor, pinnedBackgroundColor, dottedLineColor, dottedLineActiveColor);
    }
}
