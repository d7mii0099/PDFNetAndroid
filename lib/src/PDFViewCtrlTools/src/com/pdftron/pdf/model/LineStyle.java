package com.pdftron.pdf.model;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.pdftron.pdf.tools.R;

public enum LineStyle {
    DEFAULT(R.drawable.annotation_stroke_style_solid),
    DASHED(R.drawable.annotation_stroke_style_dash);

    @DrawableRes
    private final int mDrawableResource;

    LineStyle(@DrawableRes int drawableRes) {
        this.mDrawableResource = drawableRes;
    }

    public static LineStyle fromInteger(@NonNull Integer number) {
        if (number >= 0 && number < values().length) {
            return values()[number];
        } else {
            return LineStyle.DEFAULT;
        }
    }

    @DrawableRes
    public int getResource() {
        return mDrawableResource;
    }
}
