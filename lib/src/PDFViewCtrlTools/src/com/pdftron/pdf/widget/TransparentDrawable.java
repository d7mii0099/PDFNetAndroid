//------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------

package com.pdftron.pdf.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.pdf.utils.Utils;

/**
 * This class is used for drawing a grey rectangle border and a red line inside,
 * It is used to represents the transparent color
 */
public class TransparentDrawable extends Drawable {
    private boolean mDrawCircle;
    private float mRoundedCorner = 0;
    private Paint mBorderPaint;
    private Paint mRedLinePaint;

    public TransparentDrawable(Context context) {
        mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(Utils.convDp2Pix(context, 0.5f));
        mBorderPaint.setColor(Color.GRAY);

        mRedLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRedLinePaint.setStyle(Paint.Style.STROKE);
        mRedLinePaint.setStrokeWidth(Utils.convDp2Pix(context, 1));
        mRedLinePaint.setColor(Color.RED);
    }

    /**
     * Draw a light gray transparent rectangle border and a red line
     */

    @Override
    public void draw(@NonNull Canvas canvas) {
        int canvasWidth = canvas.getClipBounds().width();
        int canvasHeight = canvas.getClipBounds().height();
        float strokePadding = mBorderPaint.getStrokeWidth() / 2;
        if (mDrawCircle) {
            mRoundedCorner = canvasWidth / 2;
        }
        double corner = mRoundedCorner + strokePadding;
        double d = Math.sqrt(corner * corner * 2) - corner;
        float x = (float) Math.sqrt(d * d / 2);

        canvas.drawLine(x, canvasHeight - x, canvasWidth - x, x, mRedLinePaint);
        RectF rectF = new RectF(strokePadding,
                strokePadding,
                canvasWidth - strokePadding,
                canvasHeight - strokePadding);

        canvas.drawRoundRect(rectF, mRoundedCorner, mRoundedCorner, mBorderPaint);
    }

    /**
     * Specify an alpha value for the drawable. 0 means fully transparent, and
     * 255 means fully opaque.
     */
    @Override
    public void setAlpha(int alpha) {
        mBorderPaint.setAlpha(alpha);
        mRedLinePaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mBorderPaint.setColorFilter(colorFilter);
        mRedLinePaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    /**
     * Sets rounded corner
     *
     * @param roundedConer
     */
    public void setRoundedConer(float roundedConer) {
        mRoundedCorner = roundedConer;
    }

    /**
     * Sets rectangle border color
     *
     * @param borderColor border color
     */
    public void setBorderColor(@ColorInt int borderColor) {
        mBorderPaint.setColor(borderColor);
    }

    /**
     * Sets draw shape as circle
     */
    public void setDrawCircle(boolean circle) {
        mDrawCircle = circle;
    }
}
