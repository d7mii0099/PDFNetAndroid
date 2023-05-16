package com.pdftron.pdf.widget;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;

public class TopCropImageView extends AppCompatImageView {

    public TopCropImageView(Context context) {
        super(context);
    }

    public TopCropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TopCropImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
        if (getScaleType() == ScaleType.MATRIX) { // Crop top - fits width
            Matrix matrix = getImageMatrix();
            Drawable drawable = getDrawable();
            if (drawable != null && drawable.getIntrinsicWidth() != -1) {
                float scaleFactor = (r-l) / (float) drawable.getIntrinsicWidth();
                matrix.setScale(scaleFactor, scaleFactor, 0, 0);
                setImageMatrix(matrix);
                float finalHeight = drawable.getIntrinsicHeight() * scaleFactor;
                if(finalHeight < (b-t)){
                    int centDist = (int) (b-t-finalHeight)/2;
                    t = t + centDist;
                }

            }
        }

        return super.setFrame(l, t, r, b);
    }
}
