package com.pdftron.pdf.model;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.pdftron.pdf.tools.R;

public enum LineEndingStyle {
    NONE(R.drawable.annotation_line_ending_style_none),
    OPEN_ARROW(R.drawable.annotation_line_ending_style_open_arrow),
    CLOSED_ARROW(R.drawable.annotation_line_ending_style_closed_arrow),
    R_OPEN_ARROW(R.drawable.annotation_line_ending_style_r_open_arrow),
    R_CLOSED_ARROW(R.drawable.annotation_line_ending_style_r_closed_arrow),
    BUTT(R.drawable.annotation_line_ending_style_butt),
    CIRCLE(R.drawable.annotation_line_ending_style_circle),
    DIAMOND(R.drawable.annotation_line_ending_style_diamond),
    SQUARE(R.drawable.annotation_line_ending_style_square),
    SLASH(R.drawable.annotation_line_ending_style_slash);

    @DrawableRes
    private final int mDrawableResource;

    LineEndingStyle(@DrawableRes int drawableRes) {
        this.mDrawableResource = drawableRes;
    }

    public static LineEndingStyle fromInteger(@NonNull Integer number) {
        if (number >= 0 && number < values().length) {
            return values()[number];
        } else {
            return LineEndingStyle.NONE;
        }
    }

    @DrawableRes
    public int getResource() {
        return mDrawableResource;
    }
}
