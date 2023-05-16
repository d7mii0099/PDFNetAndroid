package com.pdftron.pdf.widget.toolbar.component;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.pdftron.pdf.tools.R;

/**
 * Helper to get {@link AnnotationToolbarComponent} related theme attributes.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AnnotationToolbarTheme {

    @ColorInt
    public final int backgroundColor;
    @ColorInt
    public final int backgroundColorSecondary;
    @ColorInt
    public final int iconColor;
    @ColorInt
    public final int selectedBackgroundColor;
    @ColorInt
    public final int disabledIconColor;
    @ColorInt
    public final int selectedIconColor;
    @ColorInt
    public final int highlightIconColor;
    @ColorInt
    public final int presetTextColor;
    @ColorInt
    public final int textColor;
    @ColorInt
    public int dividerColor;
    @ColorInt
    public int backgroundColorCompact;

    public AnnotationToolbarTheme(int backgroundColor,
            int backgroundColorSecondary,
            int iconColor,
            int selectedBackgroundColor,
            int disabledIconColor,
            int selectedIconColor,
            int highlightIconColor,
            int presetTextColor,
            int textColor,
            int dividerColor,
            int backgroundColorCompact) {
        this.backgroundColor = backgroundColor;
        this.backgroundColorSecondary = backgroundColorSecondary;
        this.iconColor = iconColor;
        this.selectedBackgroundColor = selectedBackgroundColor;
        this.disabledIconColor = disabledIconColor;
        this.selectedIconColor = selectedIconColor;
        this.highlightIconColor = highlightIconColor;
        this.presetTextColor = presetTextColor;
        this.textColor = textColor;
        this.dividerColor = dividerColor;
        this.backgroundColorCompact = backgroundColorCompact;
    }

    public static AnnotationToolbarTheme fromContext(@NonNull Context context) {

        final TypedArray a = context.obtainStyledAttributes(
                null, R.styleable.AnnotationToolbarTheme, R.attr.pt_annotation_toolbar_style, R.style.PTAnnotationToolbarTheme);
        int backgroundColor = a.getColor(R.styleable.AnnotationToolbarTheme_colorBackground, context.getResources().getColor(R.color.annot_toolbar_background_primary));
        int backgroundColorSecondary = a.getColor(R.styleable.AnnotationToolbarTheme_colorBackgroundSecondary, context.getResources().getColor(R.color.annot_toolbar_background_secondary));
        int iconColor = a.getColor(R.styleable.AnnotationToolbarTheme_iconColor, context.getResources().getColor(R.color.annot_toolbar_icon));
        int selectedBackgroundColor = a.getColor(R.styleable.AnnotationToolbarTheme_selectedBackgroundColor, context.getResources().getColor(R.color.annot_toolbar_selected_background));
        int disabledIconColor = a.getColor(R.styleable.AnnotationToolbarTheme_disabledIconColor, context.getResources().getColor(R.color.annot_toolbar_disabled_icon));
        int selectedIconColor = a.getColor(R.styleable.AnnotationToolbarTheme_selectedIconColor, context.getResources().getColor(R.color.annot_toolbar_selected_icon));
        int highlightIconColor = a.getColor(R.styleable.AnnotationToolbarTheme_highlightedIconColor, context.getResources().getColor(R.color.annot_toolbar_accent_icon));
        int presetTextColor = a.getColor(R.styleable.AnnotationToolbarTheme_presetTextColor, context.getResources().getColor(R.color.annot_toolbar_preset_text));
        int textColor = a.getColor(R.styleable.AnnotationToolbarTheme_textColor, context.getResources().getColor(R.color.annot_toolbar_text));
        int dividerColor = a.getColor(R.styleable.AnnotationToolbarTheme_dividerColor, context.getResources().getColor(R.color.annot_toolbar_divider));
        int backgroundColorCompact = a.getColor(R.styleable.AnnotationToolbarTheme_backgroundColorCompact, context.getResources().getColor(R.color.annot_toolbar_background_compact));
        a.recycle();

        return new AnnotationToolbarTheme(backgroundColor,
                backgroundColorSecondary,
                iconColor,
                selectedBackgroundColor,
                disabledIconColor,
                selectedIconColor,
                highlightIconColor,
                presetTextColor,
                textColor,
                dividerColor,
                backgroundColorCompact);
    }
}
