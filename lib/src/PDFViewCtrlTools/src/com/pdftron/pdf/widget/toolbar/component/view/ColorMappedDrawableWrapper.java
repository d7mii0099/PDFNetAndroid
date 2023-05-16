package com.pdftron.pdf.widget.toolbar.component.view;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;

import java.util.HashSet;
import java.util.Set;

class ColorMappedDrawableWrapper {

    @NonNull
    private Drawable mDrawable;

    public ColorMappedDrawableWrapper(@NonNull Drawable drawable) {
        mDrawable = drawable;
    }

    @NonNull
    public Drawable getDrawable() {
        return mDrawable;
    }

    private boolean isColorMappedDrawable(@NonNull Drawable drawable) {
        if (drawable instanceof LayerDrawable) {
            Set<Integer> ids = new HashSet<>();
            LayerDrawable layerDrawable = (LayerDrawable) drawable;
            int numLayers = layerDrawable.getNumberOfLayers();
            for (int i = 0; i < numLayers; i++) {
                ids.add(layerDrawable.getId(i));
            }
            return ids.contains(R.id.colored_section) || ids.contains(R.id.uncolored_section);
        }
        return false;
    }

    /**
     * Sets the color for the entire icon
     *
     * @param color to set
     */
    public void setIconColor(@ColorInt int color) {
        if (mDrawable instanceof LayerDrawable) {
            setColorFilter(mDrawable, color);
            ((LayerDrawable) mDrawable).setAlpha(Color.alpha(color));
        } else if (mDrawable instanceof Drawable) {
            setColorFilter(mDrawable, color);
            mDrawable.setAlpha(Color.alpha(color));
        }
    }

    /**
     * Sets the color for only the accented areas of the icon
     *
     * @param color to set
     */
    public void updateIconHighlightColor(@ColorInt int color, @IntRange(from=0,to=255) int alpha) {
        if (mDrawable instanceof LayerDrawable) {
            Drawable drawable = ((LayerDrawable) mDrawable).findDrawableByLayerId(R.id.colored_section);
            if (drawable != null) {
                setColorFilter(drawable, color);
                drawable.setAlpha(alpha);
            }
        } else if (mDrawable instanceof Drawable) {
            setColorFilter(mDrawable, color);
            mDrawable.setAlpha(alpha);
        }
    }

    private void setColorFilter(Drawable drawable, @ColorInt int color) {
        if (Utils.isLollipop()) {
            DrawableCompat.setTint(drawable, color);
        } else {
            drawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
        }
    }

}
