package com.pdftron.pdf.widget.preset.component;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.pdftron.pdf.tools.R;

/**
 * Helper to get {@link PresetBarComponent} related theme attributes.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PresetBarTheme {

    @ColorInt
    public final int backgroundColor;
    @ColorInt
    public final int iconColor;
    @ColorInt
    public final int disabledIconColor;
    @ColorInt
    public final int selectedIconColor;
    @ColorInt
    public final int selectedBackgroundColor;
    @ColorInt
    public final int selectedBackgroundColorSecondary;
    @ColorInt
    public final int expandIconColor;
    @ColorInt
    public final int accentColor;

    public PresetBarTheme(int backgroundColor, int iconColor, int disabledIconColor, int selectedIconColor, int selectedBackgroundColor, int selectedBackgroundColorSecondary, int expandIconColor, int accentColor) {
        this.backgroundColor = backgroundColor;
        this.iconColor = iconColor;
        this.disabledIconColor = disabledIconColor;
        this.selectedIconColor = selectedIconColor;
        this.selectedBackgroundColor = selectedBackgroundColor;
        this.selectedBackgroundColorSecondary = selectedBackgroundColorSecondary;
        this.expandIconColor = expandIconColor;
        this.accentColor = accentColor;
    }

    public static PresetBarTheme fromContext(@NonNull Context context) {
        final TypedArray a = context.obtainStyledAttributes(
                null, R.styleable.PresetBarTheme, R.attr.pt_preset_bar_style, R.style.PTPresetBarTheme);
        int backgroundColor = a.getColor(R.styleable.PresetBarTheme_colorBackground, context.getResources().getColor(R.color.presetbar_background));
        int iconColor = a.getColor(R.styleable.PresetBarTheme_iconColor, context.getResources().getColor(R.color.presetbar_icon));
        int disabledIconColor = a.getColor(R.styleable.PresetBarTheme_disabledIconColor, context.getResources().getColor(R.color.presetbar_disabled_icon));
        int selectedBackgroundColor = a.getColor(R.styleable.PresetBarTheme_selectedBackgroundColor, context.getResources().getColor(R.color.presetbar_selected_background));
        int selectedBackgroundColorSecondary = a.getColor(R.styleable.PresetBarTheme_selectedBackgroundColorSecondary, context.getResources().getColor(R.color.presetbar_selected_background_secondary));
        int selectedIconColor = a.getColor(R.styleable.PresetBarTheme_selectedIconColor, context.getResources().getColor(R.color.presetbar_selected_icon));
        int expandIconColor = a.getColor(R.styleable.PresetBarTheme_expandIconColor, context.getResources().getColor(R.color.presetbar_selected_icon));
        int accentColor = a.getColor(R.styleable.PresetBarTheme_accentColor, context.getResources().getColor(R.color.presetbar_accent_color));
        a.recycle();

        return new PresetBarTheme(backgroundColor, iconColor, disabledIconColor, selectedIconColor, selectedBackgroundColor, selectedBackgroundColorSecondary, expandIconColor, accentColor);
    }
}
