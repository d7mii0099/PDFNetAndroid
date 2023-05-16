package com.pdftron.pdf.tools;

import android.animation.Animator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import androidx.annotation.NonNull;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.SelectionLoupe;

public abstract class BaseTool extends Tool {

    public static final int LOUPE_TYPE_TEXT = 1;
    public static final int LOUPE_TYPE_MEASURE = 2;

    public static final int LOUPE_RADIUS = 60;
    public static final int LOUPE_SIZE = LOUPE_RADIUS * 2;

    protected final float mTSWidgetRadius;

    // for text
    private int mLoupeWidth;
    private int mLoupeHeight;

    // for measurement
    private int mLoupeRadius;
    private int mLoupeSize;

    protected boolean mLoupeEnabled;
    protected SelectionLoupe mSelectionLoupe;
    protected RectF mLoupeBBox;
    protected Canvas mCanvas;
    protected Bitmap mBitmap;
    protected boolean mDrawingLoupe;

    protected RectF mSrcRectF;
    protected RectF mDesRectF;
    protected Matrix mMatrix;
    protected PointF mPressedPoint;

    /**
     * Class constructor
     */
    public BaseTool(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);

        mTSWidgetRadius = this.convDp2Pix(12);

        mLoupeWidth = mPdfViewCtrl.getContext().getResources().getDimensionPixelSize(R.dimen.pdftron_magnifier_width);
        mLoupeHeight = mPdfViewCtrl.getContext().getResources().getDimensionPixelSize(R.dimen.pdftron_magnifier_height);

        mLoupeBBox = new RectF();

        mSrcRectF = new RectF();
        mDesRectF = new RectF();
        mMatrix = new Matrix();
        mPressedPoint = new PointF();

        mSelectionLoupe = ((ToolManager) mPdfViewCtrl.getToolManager()).getSelectionLoupe(getLoupeType());
        mBitmap = ((ToolManager) mPdfViewCtrl.getToolManager()).getSelectionLoupeBitmap(getLoupeType());
        mCanvas = ((ToolManager) mPdfViewCtrl.getToolManager()).getSelectionLoupeCanvas(getLoupeType());

        mLoupeSize = (int) this.convDp2Pix(LOUPE_SIZE);
        mLoupeRadius = (int) this.convDp2Pix(LOUPE_RADIUS);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        boolean result = super.onDown(e);

        float x = e.getX() + mPdfViewCtrl.getScrollX();
        float y = e.getY() + mPdfViewCtrl.getScrollY();
        mPressedPoint.x = x;
        mPressedPoint.y = y;

        return result;
    }

    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        boolean result = super.onMove(e1, e2, x_dist, y_dist);

        float sx = mPdfViewCtrl.getScrollX();
        float sy = mPdfViewCtrl.getScrollY();
        mPressedPoint.x = e2.getX() + sx;
        mPressedPoint.y = e2.getY() + sy;

        return result;
    }

    @Override
    public void onClose() {
        super.onClose();
        mSelectionLoupe.dismiss();
    }

    @Override
    protected void unsetAnnot() {
        super.unsetAnnot();

        mSelectionLoupe.dismiss();
    }

    public boolean isDrawingLoupe() {
        return mDrawingLoupe;
    }

    protected void setLoupeInfo(float touch_x, float touch_y) {
        if (!((ToolManager) mPdfViewCtrl.getToolManager()).isLoupeEnabled()) {
            return;
        }
        if (!mLoupeEnabled) {
            return;
        }

        if (Utils.isPie() && LOUPE_TYPE_TEXT == getLoupeType()) {
            try {
                mSelectionLoupe.show(touch_x, touch_y - mTSWidgetRadius);
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            }
            return;
        }

        float sx = mPdfViewCtrl.getScrollX();
        float sy = mPdfViewCtrl.getScrollY();

        float left = touch_x + sx - mLoupeWidth / 2.0f;
        float right = left + mLoupeWidth;
        float top = touch_y + sy - mLoupeHeight * 1.6f;
        float bottom = top + mLoupeHeight;

        if (LOUPE_TYPE_MEASURE == getLoupeType()) {
            left = touch_x + sx - mLoupeSize / 2.0f;
            right = left + mLoupeSize;
            top = touch_y + sy - mLoupeSize * 1.2f;
            bottom = top + mLoupeSize;
        }

        mLoupeBBox.set(left, top, right, bottom);

        float centerX = mLoupeBBox.centerX();
        float centerY = mLoupeBBox.centerY();

        if (mSelectionLoupe != null) {
            if (LOUPE_TYPE_MEASURE == getLoupeType()) {
                mSelectionLoupe.layout(
                        (int) (centerX - mLoupeRadius),
                        (int) (centerY - mLoupeRadius),
                        (int) (centerX + mLoupeRadius),
                        (int) (centerY + mLoupeRadius));
            } else {
                mSelectionLoupe.layout(
                        (int) (centerX - mLoupeWidth / 2),
                        (int) (centerY - mLoupeHeight / 2),
                        (int) (centerX + mLoupeWidth / 2),
                        (int) (centerY + mLoupeHeight / 2));
            }
        }
    }

    // Starts the show/hide loupe animation
    protected void animateLoupe(boolean show) {
        if (!((ToolManager) mPdfViewCtrl.getToolManager()).isLoupeEnabled()) {
            return;
        }
        if (!mLoupeEnabled) {
            return;
        }
        if (null == mSelectionLoupe) {
            return;
        }
        if (Utils.isPie() && LOUPE_TYPE_TEXT == getLoupeType()) {
            if (!show) {
                mSelectionLoupe.dismiss();
            }
            return;
        }
        if (LOUPE_TYPE_MEASURE == getLoupeType()) {
            mSelectionLoupe.setPivotX(mLoupeRadius);
            mSelectionLoupe.setPivotY(mLoupeRadius * 2);
        } else {
            mSelectionLoupe.setPivotX(mLoupeWidth / 2f);
            mSelectionLoupe.setPivotY(mLoupeHeight);
        }
        if (show) {
            mSelectionLoupe.show();
            mSelectionLoupe.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(100)
                    .setInterpolator(new DecelerateInterpolator())
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            mSelectionLoupe.setScaleX(0);
                            mSelectionLoupe.setScaleY(0);
                            mSelectionLoupe.setAlpha(0);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {

                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            mSelectionLoupe.dismiss();
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    });
        } else {
            mSelectionLoupe.animate()
                    .scaleX(0)
                    .scaleY(0)
                    .alpha(0)
                    .setDuration(150)
                    .setInterpolator(new AccelerateInterpolator())
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mSelectionLoupe.dismiss();
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            mSelectionLoupe.dismiss();
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    });
        }
    }

    protected abstract boolean canDrawLoupe();

    protected abstract int getLoupeType();

    protected Matrix getMatrix() {
        float left = mPressedPoint.x - mLoupeBBox.width() / 2 / 1.25f;
        float top = mPressedPoint.y - mLoupeBBox.height() / 2 / 1.25f;
        float right = mPressedPoint.x + mLoupeBBox.width() / 2 / 1.25f;
        float bottom = mPressedPoint.y + mLoupeBBox.height() / 2 / 1.25f;

        if (LOUPE_TYPE_MEASURE == getLoupeType()) {
            left = mPressedPoint.x - mLoupeBBox.width() / 4;
            top = mPressedPoint.y - mLoupeBBox.height() / 4;
            right = left + mLoupeBBox.width() / 2;
            bottom = top + mLoupeBBox.height() / 2;
        }

        mSrcRectF.set(left, top, right, bottom);
        mDesRectF.set(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        mMatrix.setRectToRect(mSrcRectF, mDesRectF, Matrix.ScaleToFit.CENTER);

        return mMatrix;
    }

    protected void drawLoupe() {
        if (!((ToolManager) mPdfViewCtrl.getToolManager()).isLoupeEnabled()) {
            return;
        }
        if (!mLoupeEnabled) {
            return;
        }
        if (mDrawingLoupe) {
            return;
        }

        if (null == mSelectionLoupe) {
            return;
        }

        if (Utils.isPie() && LOUPE_TYPE_TEXT == getLoupeType()) {
            return;
        }

        if (canDrawLoupe()) {
            mDrawingLoupe = true;

            mSelectionLoupe.setAlpha(0);
            if (mAnnotView != null) {
                mAnnotView.setSelectionHandleVisible(false);
            }
            mCanvas.save();
            mCanvas.setMatrix(getMatrix());
            mPdfViewCtrl.draw(mCanvas);
            mCanvas.restore();
            mSelectionLoupe.setAlpha(1);
            if (mAnnotView != null) {
                mAnnotView.setSelectionHandleVisible(true);
            }

            mDrawingLoupe = false;
        }
    }

    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        if (!mDrawingLoupe) {
            super.onDraw(canvas, tfm);
        }
        drawSnapToShapeIfEnabled(canvas);
        drawLoupe();
    }
}
