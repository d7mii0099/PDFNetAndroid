package com.pdftron.pdf.widget;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;

/**
 * @hide
 *
 * Image with rounded corners
 * @author Gabriele Mariotti (gabri.mariotti@gmail.com)
 * modified by PDFTron
 *
 * You can find the original source here:
 * http://www.curious-creature.org/2012/12/11/android-recipe-1-image-with-rounded-corners/
 */
class RoundCornersDrawable extends Drawable {

    private final float mCornerRadius;
    private final RectF mRect = new RectF();
    private final Paint mPaint;
    private final Paint mBorderPaint;
    private final int mMargin;
    private boolean mDrawBorder;

    RoundCornersDrawable(Bitmap bitmap, float cornerRadius, int margin) {
        mCornerRadius = cornerRadius;

        BitmapShader bitmapShader = new BitmapShader(bitmap,
                Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setShader(bitmapShader);

        mBorderPaint = new Paint();
        mBorderPaint.setAntiAlias(true);
        mBorderPaint.setStyle(Paint.Style.STROKE);

        mMargin = margin;
    }

    void disableBorder() {
        mDrawBorder = false;
    }

    void enableBorder(int color, float thickness) {
        mDrawBorder = true;
        mBorderPaint.setColor(color);
        mBorderPaint.setStrokeWidth(thickness);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mRect.set(mMargin, mMargin, bounds.width() - mMargin, bounds.height() - mMargin);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.drawRoundRect(mRect, mCornerRadius, mCornerRadius, mPaint);

        if (mDrawBorder) {
            canvas.drawRoundRect(mRect, mCornerRadius, mCornerRadius, mBorderPaint);
        }
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
        mBorderPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }
}
