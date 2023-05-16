package com.pdftron.pdf.widget.toolbar.component.view;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.pdftron.pdf.tools.R;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TabActionButtonTheme {
    @ColorInt
    public final int textColor;
    @ColorInt
    public final int iconColor;

    public TabActionButtonTheme(int textColor, int iconColor) {
        this.textColor = textColor;
        this.iconColor = iconColor;
    }

    public static TabActionButtonTheme fromContext(@NonNull Context context) {

        final TypedArray a = context.obtainStyledAttributes(
                null, R.styleable.TabActionButtonTheme, R.attr.pt_toolbar_tab_action_button_style, R.style.PTToolbarTabActionButtonTheme);
        int textColor = a.getColor(R.styleable.TabActionButtonTheme_textColor, context.getResources().getColor(R.color.toolbar_tab_action_button_text));
        int iconColor = a.getColor(R.styleable.TabActionButtonTheme_iconColor, context.getResources().getColor(R.color.toolbar_tab_action_button_icon));
        a.recycle();

        return new TabActionButtonTheme(textColor, iconColor);
    }
}
