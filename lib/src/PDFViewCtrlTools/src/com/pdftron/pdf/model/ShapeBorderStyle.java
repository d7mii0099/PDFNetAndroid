package com.pdftron.pdf.model;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.pdftron.pdf.tools.R;

public enum ShapeBorderStyle {
    DEFAULT(R.drawable.annotation_stroke_style_solid),
    DASHED(R.drawable.annotation_stroke_style_dash),
    CLOUDY(R.drawable.annotation_stroke_style_cloud);

    @DrawableRes
    private final int mDrawableResource;

    ShapeBorderStyle(@DrawableRes int drawableRes) {
        this.mDrawableResource = drawableRes;
    }

    public static ShapeBorderStyle fromInteger(@NonNull Integer number) {
        if (number >= 0 && number < values().length) {
            return values()[number];
        } else {
            return ShapeBorderStyle.DEFAULT;
        }
    }

    @DrawableRes
    public int getResource() {
        return mDrawableResource;
    }
}
