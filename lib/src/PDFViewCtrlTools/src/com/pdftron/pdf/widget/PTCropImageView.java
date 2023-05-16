package com.pdftron.pdf.widget;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.annotation.NonNull;

import com.edmodo.cropper.cropwindow.edge.Edge;

public class PTCropImageView extends PTCropImageViewBase {

    public interface CropImageViewListener {
        void onDown(MotionEvent event);

        void onUp(MotionEvent event);
    }

    private CropImageViewListener mCropImageViewListener;
    private double mZoom = 1.0;

    public void setCropImageViewListener(CropImageViewListener listener) {
        this.mCropImageViewListener = listener;
    }

    // the rectangle representing the current crop as a percentage across the page from each edge
    // of the bounding box.
    private RectF mCropPercentageRect;

    public PTCropImageView(Context context) {
        super(context);
    }

    public PTCropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PTCropImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected RectF getBitmapRect() {
        Drawable drawable = this.getDrawable();
        if (drawable == null) {
            return new RectF();
        } else {
            final float[] matrixValues = new float[9];
            getImageMatrix().getValues(matrixValues);
            final float scaleX = matrixValues[Matrix.MSCALE_X];
            final float scaleY = matrixValues[Matrix.MSCALE_Y];
            final float transX = matrixValues[Matrix.MTRANS_X];
            final float transY = matrixValues[Matrix.MTRANS_Y];
            final int drawableIntrinsicWidth = drawable.getIntrinsicWidth();
            final int drawableIntrinsicHeight = drawable.getIntrinsicHeight();
            final int drawableDisplayWidth = Math.round(drawableIntrinsicWidth * scaleX * (float) mZoom);
            final int drawableDisplayHeight = Math.round(drawableIntrinsicHeight * scaleY * (float) mZoom);
            final float left = Math.max(transX, 0);
            final float top = Math.max(transY, 0);
            final float right = Math.min(left + drawableDisplayWidth, getWidth());
            final float bottom = Math.min(top + drawableDisplayHeight, getHeight());
            return new RectF(left, top, right, bottom);
        }
    }

    public boolean hasBitmap() {
        Drawable drawable = getDrawable();
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap() != null;
        }
        return false;
    }

    public RectF getCropRectPercentageMargins() {
        RectF bitmapRect = this.getBitmapRect();

        float actualWidth = bitmapRect.width() - getPaddingLeft() - getPaddingRight();
        float actualHeight = bitmapRect.height() - getPaddingTop() - getPaddingBottom();
        float left = bitmapRect.left + getPaddingLeft();
        float right = bitmapRect.right - getPaddingRight();
        float top = bitmapRect.top + getPaddingTop();
        float bottom = bitmapRect.bottom - getPaddingBottom();

        final float leftPadding = (Edge.LEFT.getCoordinate() - left) / actualWidth;
        final float rightPadding = (right - Edge.RIGHT.getCoordinate()) / actualWidth;
        final float bottomPadding = (bottom - Edge.BOTTOM.getCoordinate()) / actualHeight;
        final float topPadding = (Edge.TOP.getCoordinate() - top) / actualHeight;

        mCropPercentageRect = new RectF(Math.max(0f, leftPadding),
                Math.max(0f, topPadding),
                Math.max(0f, rightPadding),
                Math.max(0f, bottomPadding));

        return mCropPercentageRect;
    }

    public void setZoom(double zoom) {
        mZoom = zoom;
    }

    public void setCropRectPercentageMargins(RectF percentageMargins) {
        mCropPercentageRect = percentageMargins;
        initCropWindow(this.getBitmapRect());
        invalidate();
    }

    @Override
    protected void initCropWindow(@NonNull RectF bitmapRect) {
        super.initCropWindow(bitmapRect);
        if (mCropPercentageRect != null) {
            float actualWidth = bitmapRect.width() - getPaddingLeft() - getPaddingRight();
            float actualHeight = bitmapRect.height() - getPaddingTop() - getPaddingBottom();
            float left = bitmapRect.left + getPaddingLeft();
            float right = bitmapRect.right - getPaddingRight();
            float top = bitmapRect.top + getPaddingTop();
            float bottom = bitmapRect.bottom - getPaddingBottom();

            final float leftPadding = mCropPercentageRect.left * actualWidth;
            final float rightPadding = mCropPercentageRect.right * actualWidth;
            final float bottomPadding = mCropPercentageRect.bottom * actualHeight;
            final float topPadding = mCropPercentageRect.top * actualHeight;

            Edge.LEFT.setCoordinate(left + leftPadding);
            Edge.TOP.setCoordinate(top + topPadding);
            Edge.RIGHT.setCoordinate(right - rightPadding);
            Edge.BOTTOM.setCoordinate(bottom - bottomPadding);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isEnabled()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (mCropImageViewListener != null) {
                        mCropImageViewListener.onDown(event);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (mCropImageViewListener != null) {
                        mCropImageViewListener.onUp(event);
                    }
                    break;
                default:
                    break;
            }
        }
        return super.onTouchEvent(event);
    }
}
