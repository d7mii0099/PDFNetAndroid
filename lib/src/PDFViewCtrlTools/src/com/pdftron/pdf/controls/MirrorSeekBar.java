//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.appcompat.widget.AppCompatSeekBar;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.utils.Utils;

/**
 * A seek bar that can reverse in RTL mode
 */
public class MirrorSeekBar extends AppCompatSeekBar {

    private OnSeekBarChangeListener mOnSeekBarChangeListener;

    private boolean mIsReversed;
    private boolean mIsVertical;
    private boolean mInteractThumbOnly;
    Drawable mDrawable = null;

    private boolean mTouchDisabled;

    private PDFViewCtrl mPdfViewCtrl;

    /**
     * Class constructor
     */
    public MirrorSeekBar(Context context) {
        super(context);
        mIsReversed = false;
    }

    /**
     * Class constructor
     */
    public MirrorSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mIsReversed = false;
    }

    /**
     * Class constructor
     */
    public MirrorSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mIsReversed = false;
    }

    /**
     * @return True if reversed
     */
    public boolean isReversed() {
        return mIsReversed;
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        super.setOnSeekBarChangeListener(l);
        mOnSeekBarChangeListener = l;
    }

    /**
     * Sets whether the seek bar should be reversed or not.
     *
     * @param isReversed True if should be reversed
     */
    public void setReversed(boolean isReversed) {
        mIsReversed = isReversed;
        if (isReversed) {
            mDrawable = getBackground();
            setBackground(null);
        } else if (mDrawable != null) {
            setBackground(mDrawable);
        }
        invalidate();
        refreshDrawableState();
    }

    public boolean isVertical() {
        return mIsVertical;
    }

    public void setVertical(boolean isVertical) {
        mIsVertical = isVertical;
        requestLayout();
        invalidate();
    }

    public void setInteractThumbOnly(boolean thumbOnly) {
        mInteractThumbOnly = thumbOnly;
    }

    public void setPdfViewCtrl(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mIsVertical) {
            if (Utils.isRtlLayout(getContext())) {
                float px = this.getWidth() / 2.0f;
                float py = this.getHeight() / 2.0f;
                canvas.scale(-1, -1, px, py);
                canvas.rotate(90);
            } else {
                canvas.rotate(90);
                canvas.translate(0, -getWidth());
            }
        } else {
            if (mIsReversed) {
                float px = this.getWidth() / 2.0f;
                float py = this.getHeight() / 2.0f;
                canvas.scale(-1, 1, px, py);
            }
        }
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int[] posInWindow = new int[2];
        getLocationInWindow(posInWindow);

        // in horizontal RTL mode, let's reverse the x-axis
        if (mIsReversed && !mIsVertical) {
            event.setLocation(this.getWidth() - event.getX(), event.getY());
        }

        if (mInteractThumbOnly && event.getAction() == MotionEvent.ACTION_DOWN) {
            boolean validTouch = false;
            android.graphics.Rect thumbBounds = getThumb().getBounds();
            float padding = Utils.convDp2Pix(getContext(), 12);
            float minBound = Math.min(thumbBounds.left, thumbBounds.right) - padding;
            float maxBound = Math.max(thumbBounds.left, thumbBounds.right) + padding;
            if (mIsVertical) {
                if (Utils.isRtlLayout(getContext())) {
                    float actualPos = event.getY();
                    if (Utils.isLollipop()) {
                        actualPos = this.getHeight() - event.getY();
                    }
                    validTouch = actualPos > minBound &&
                            actualPos < maxBound; // in vertical mode, the thumb is transformed
                } else {
                    validTouch = event.getY() > minBound &&
                            event.getY() < maxBound; // in vertical mode, the thumb is transformed
                }
            } else {
                validTouch = event.getX() > minBound &&
                        event.getX() < maxBound;
            }
            if (!validTouch) {
                mTouchDisabled = true;
                if (mPdfViewCtrl != null) {
                    event.setLocation(posInWindow[0] + event.getX(), event.getY());
                    return mPdfViewCtrl.dispatchTouchEvent(event);
                }
                return true;
            }
        }
        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (mTouchDisabled) {
                mTouchDisabled = false;
                if (mPdfViewCtrl != null) {
                    event.setLocation(posInWindow[0] + event.getX(), event.getY());
                    return mPdfViewCtrl.dispatchTouchEvent(event);
                }
                return true;
            }
        }
        if (mTouchDisabled) {
            if (mPdfViewCtrl != null) {
                event.setLocation(posInWindow[0] + event.getX(), event.getY());
                return mPdfViewCtrl.dispatchTouchEvent(event);
            }
            return true;
        }

        if (mIsVertical) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_UP:
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        if (mOnSeekBarChangeListener != null) {
                            mOnSeekBarChangeListener.onStartTrackingTouch(this);
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (mOnSeekBarChangeListener != null) {
                            mOnSeekBarChangeListener.onStopTrackingTouch(this);
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        updateProgress(event);
                    }
                    if (!mInteractThumbOnly && event.getAction() != MotionEvent.ACTION_MOVE) {
                        updateProgress(event);
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    break;
            }
            return true;
        }

        return super.onTouchEvent(event);
    }

    private void updateProgress(MotionEvent event) {
        int progress = (int) (getMax() * event.getY() / getHeight());
        if (progress >= 0 && progress <= getMax()) {
            setProgress(progress);
            if (mOnSeekBarChangeListener != null) {
                mOnSeekBarChangeListener.onProgressChanged(this, progress, true);
            }
        }
    }

    @Override
    public synchronized void setProgress(int progress) {
        super.setProgress(progress);

        if (mIsVertical) {
            onSizeChanged(getWidth(), getHeight(), 0, 0);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (mIsVertical) {
            super.onSizeChanged(h, w, oldh, oldw);
        } else {
            super.onSizeChanged(w, h, oldw, oldh);
        }
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mIsVertical) {
            super.onMeasure(heightMeasureSpec, widthMeasureSpec);
            setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}
