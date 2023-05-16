package com.pdftron.pdf.dialog.toolbarswitcher.button;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.pdftron.pdf.tools.R;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ToolbarSwitcherButtonTheme {
    @ColorInt
    public final int textColor;
    @ColorInt
    public final int iconColor;
    @ColorInt
    public final int backgroundColorCompact;

    public ToolbarSwitcherButtonTheme(int textColor, int iconColor, int backgroundColorCompact) {
        this.textColor = textColor;
        this.iconColor = iconColor;
        this.backgroundColorCompact = backgroundColorCompact;
    }

    public static ToolbarSwitcherButtonTheme fromContext(@NonNull Context context) {

        final TypedArray a = context.obtainStyledAttributes(
                null, R.styleable.ToolbarSwitcherButtonTheme, R.attr.pt_toolbar_switcher_button_style, R.style.PTToolbarSwitcherButtonTheme);
        int textColor = a.getColor(R.styleable.ToolbarSwitcherButtonTheme_textColor, context.getResources().getColor(R.color.toolbar_switcher_button_text));
        int iconColor = a.getColor(R.styleable.ToolbarSwitcherButtonTheme_iconColor, context.getResources().getColor(R.color.toolbar_switcher_button_icon));
        int backgroundColorCompact = a.getColor(R.styleable.ToolbarSwitcherButtonTheme_backgroundColorCompact, context.getResources().getColor(R.color.annot_toolbar_background_compact));
        a.recycle();

        return new ToolbarSwitcherButtonTheme(textColor, iconColor, backgroundColorCompact);
    }
}
