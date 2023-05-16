package com.pdftron.pdf.widget.bottombar.component;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.pdftron.pdf.tools.R;

/**
 * Helper to get {@link BottomBarComponent} related theme attributes.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BottomBarTheme {

    @ColorInt
    public final int backgroundColor;
    @ColorInt
    public final int iconColor;
    @ColorInt
    public final int selectedIconColor;
    @ColorInt
    public final int disabledIconColor;
    @ColorInt
    public final int selectedBackgroundColor;

    public BottomBarTheme(int backgroundColor, int iconColor, int selectedBackgroundColor, int disabledIconColor, int selectedIconColor) {
        this.backgroundColor = backgroundColor;
        this.iconColor = iconColor;
        this.selectedBackgroundColor = selectedBackgroundColor;
        this.disabledIconColor = disabledIconColor;
        this.selectedIconColor = selectedIconColor;
    }

    public static BottomBarTheme fromContext(@NonNull Context context) {
        final TypedArray a = context.obtainStyledAttributes(
                null, R.styleable.BottomBarTheme, R.attr.pt_bottom_bar_style, R.style.PTBottomBarTheme);
        int backgroundColor = a.getColor(R.styleable.BottomBarTheme_colorBackground, context.getResources().getColor(R.color.bottombar_background));
        int iconColor = a.getColor(R.styleable.BottomBarTheme_iconColor, context.getResources().getColor(R.color.bottombar_icon));
        int selectedBackgroundColor = a.getColor(R.styleable.BottomBarTheme_selectedBackgroundColor, context.getResources().getColor(R.color.bottombar_selected_background));
        int disabledIconColor = a.getColor(R.styleable.BottomBarTheme_disabledIconColor, context.getResources().getColor(R.color.bottombar_disabled_icon));
        int selectedIconColor = a.getColor(R.styleable.BottomBarTheme_selectedIconColor, context.getResources().getColor(R.color.bottombar_selected_icon));
        a.recycle();

        return new BottomBarTheme(backgroundColor, iconColor, selectedBackgroundColor, disabledIconColor, selectedIconColor);
    }
}
