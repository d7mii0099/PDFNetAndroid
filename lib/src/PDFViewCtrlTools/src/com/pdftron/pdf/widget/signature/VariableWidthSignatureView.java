package com.pdftron.pdf.widget.signature;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.pdf.tools.FreehandCreate;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.List;

// Modified from https://github.com/simplifycom/ink-android/blob/master/ink/src/main/java/com/simplify/ink/InkView.java
@SuppressWarnings("RedundantThrows")
public class VariableWidthSignatureView extends View {

    private static final int DEFAULT_STROKE_COLOR = 0xFF000000;

    private Bitmap mBitmap;
    @ColorInt
    private int mStrokeColor = DEFAULT_STROKE_COLOR;

    // Bounding box in PDF page coordinates, not Android coordinates
    private float mLeft = 0.0f;
    private float mTop = 0.0f;
    private float mRight = 0.0f;
    private float mBottom = 0.0f;
    private boolean mIsFirstTouch = true; // reset this when the signature is cleared. used to initialize bounding box

    private boolean mIsPressureSensitive = true;
    private float mStrokeWidth;
    private ArrayList<InkListener> mListeners = new ArrayList<>();

    private float mPrevX = Float.MAX_VALUE; // previous X and Y are used to filter onMove events based on distance
    private float mPrevY = Float.MAX_VALUE;

    @Nullable
    private PointProcessor mPointProcessorObj;

    private static float mSampleDelta = FreehandCreate.sSampleDelta;

    public VariableWidthSignatureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VariableWidthSignatureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        if (mSampleDelta == -1) {
            mSampleDelta = FreehandCreate.computeThresholdValue(this);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        boolean sizeModified = mBitmap != null && (mBitmap.getWidth() != getWidth() || mBitmap.getHeight() != getHeight());
        if (sizeModified) {
            // Does not make sense to keep content of bitmap canvas if its size has changed. So
            // here we just clear the state. This will also re-initialize the bitmap
            clear();
        } else { // otherwise initialize
            initializeState();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getAction();
        // on down, initialize stroke point
        float x = e.getX();
        float y = e.getY();
        float pressure = mIsPressureSensitive ? e.getPressure() : 1.0f; // if not pressure sensitive, then just set pressure to 1

        if (action == MotionEvent.ACTION_MOVE) {
            processOnMoveHistoricalMotionPoints(e);
        } else if (action == MotionEvent.ACTION_DOWN) {
            onTouchDown(x, y, pressure);
        } else if (action == MotionEvent.ACTION_UP) {
            onTouchUp(x, y, pressure);
        }

        mLeft = Math.min(x, mLeft);
        mTop = Math.max(y, mTop);
        mRight = Math.max(x, mRight);
        mBottom = Math.min(y, mBottom);
        return true;
    }

    private void processOnMoveHistoricalMotionPoints(MotionEvent ev) {
        final float eventX = ev.getX();
        final float eventY = ev.getY();
        final float eventPressure = ev.getPressure();
        final int historySize = ev.getHistorySize();
        final int pointerCount = ev.getPointerCount();

        // Loop through all intermediate points
        // During moving, update the free hand path and the bounding box. Note that for the
        // bounding box, we need to include the previous bounding box in the new bounding box
        // so that the previously drawn free hand will go away.
        for (int h = 0; h < historySize; h++) {
            if (pointerCount >= 1) {

                float historicalX = ev.getHistoricalX(0, h);
                float historicalY = ev.getHistoricalY(0, h);
                float historicalPressure = ev.getHistoricalPressure(0, h);

                if (distance(historicalX, historicalY, mPrevX, mPrevY) > mSampleDelta
                        && distance(historicalX, historicalY, eventX, eventY) > mSampleDelta) {
                    onTouchMove(historicalX, historicalY, historicalPressure);
                }
            }
        }
        if (!(eventX == mPrevX && eventY == mPrevY)) {
            onTouchMove(eventX, eventY, eventPressure);
        }
    }

    private float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    // Handle on touch down events, i.e. start of a stroke
    private void onTouchDown(float x, float y, float pressure) {
        // notify listeners
        for (InkListener listener : mListeners) {
            listener.onInkStarted();
        }
        mPrevX = x;
        mPrevY = y;
        mPointProcessorObj.onDown(x, y, pressure);

        // Initialize bounding values if it is the first touch
        if (mIsFirstTouch) {
            mLeft = x;
            mTop = y;
            mRight = x;
            mBottom = y;
            mIsFirstTouch = false;
        }
    }

    // Handle on touch move events, i.e. during a stroke
    private void onTouchMove(float x, float y, float pressure) {
        mPointProcessorObj.onMove(x, y, pressure);
        // note flipped ordering due pdf page coordinates for mLeft, mTop, mRight, and mBottom
        if (!mIsFirstTouch) {
            mLeft = Math.min((float) x, mLeft);
            mTop = Math.max((float) y, mTop);
            mRight = Math.max((float) x, mRight);
            mBottom = Math.min((float) y, mBottom);
        }
        mPrevX = x;
        mPrevY = y;
    }

    // Handle on touch up events, i.e. end of a stroke
    private void onTouchUp(float x, float y, float pressure) {
        mPointProcessorObj.onUp(x, y, pressure);
        mPrevX = x;
        mPrevY = y;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // simply paint the bitmap on the canvas
        if (mBitmap != null && !mBitmap.isRecycled()) {
            canvas.drawBitmap(mBitmap, 0, 0, null);
        }
        super.onDraw(canvas);
    }

    /**
     * Adds a listener on the view
     *
     * @param listener The listener
     */
    public void addListener(InkListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    /**
     * Removes the listener from the view
     *
     * @param listener The listener
     */
    public void removeListener(InkListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Enables and disables pressure sensitive inking. By default this is enabled.
     *
     * @param isEnabled true if pressure sensitivity is enabled, false otherwise
     */
    public void setPressureSensitivity(boolean isEnabled) {
        mIsPressureSensitive = isEnabled;
    }

    /**
     * Sets the stroke color
     *
     * @param color The color value
     */
    public void setColor(@ColorInt int color) {
        mStrokeColor = color;
        if (mPointProcessorObj != null) {
            mPointProcessorObj.setColorWithRedraw(color);
        }
    }

    /**
     * Clear the signature, stop all processing, and re-initialize the view.
     */
    public void clear() {
        resetState();
        invalidate();
    }

    private void resetState() {
        // First clear the point processor otherwise it will try to draw on a recycled bitmap
        if (mPointProcessorObj != null) {
            mPointProcessorObj.destroy();
            mPointProcessorObj = null;
        }

        // Clear and create a new bitmap
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }

        initializeState();
    }

    private void initializeState() {
        // In certain cases, if either width and height are zero, we will use default width/height.
        // Otherwise the createBitmap call later will crash
        int width = getWidth() == 0 ? 100 : getWidth();
        int height = getHeight() == 0 ? 100 : getHeight();

        if (mBitmap == null && mPointProcessorObj == null) {
            mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            PointProcessor.DrawCallback drawCallback = new PointProcessor.DrawCallback() {
                @Override
                public void onDrawInfoReceived(@NonNull InkDrawInfo drawInfo, @NonNull Bitmap bitmap) {
                    Utils.throwIfOnMainThread();
                    mBitmap = bitmap;
                    postInvalidate(drawInfo.left, drawInfo.top, drawInfo.right, drawInfo.bottom);
                }

                @Override
                public void onComplete(List<StrokeOutlineResult> strokeOutlineResults) {
                    for (InkListener listener : mListeners) {
                        listener.onInkCompleted(getStrokes(strokeOutlineResults));
                    }
                    resetState();
                }
            };
            mPointProcessorObj = new PointProcessor(
                    width,
                    height,
                    mStrokeColor,
                    mStrokeWidth,
                    1.0f,
                    drawCallback);

            mIsFirstTouch = true;
        }
    }

    public void setStrokeWidth(float strokeWidth) {
        this.mStrokeWidth = strokeWidth;
        if (mPointProcessorObj != null) {
            this.mPointProcessorObj.setStrokeWidth(strokeWidth);
        }
    }

    public RectF getBoundingBox() {
        float strokeWidthBuffer = mStrokeWidth * 1.5f;
        return new RectF(mLeft - strokeWidthBuffer,
                mTop + strokeWidthBuffer,
                mRight + strokeWidthBuffer,
                mBottom - strokeWidthBuffer);
    }

    private List<double[]> getStrokes(@NonNull List<StrokeOutlineResult> allStrokeOutlines) {
        List<double[]> outlines = new ArrayList<>();
        for (StrokeOutlineResult outline : allStrokeOutlines) {
            outlines.add(outline.strokeOutline);
        }
        return outlines;
    }

    /**
     * Finish processing points and emit on finish event.
     */
    public void finish() {
        if (mPointProcessorObj != null) {
            mPointProcessorObj.finish();
        }
    }

    /**
     * Listener for the ink view to notify clear events
     */
    public interface InkListener {
        /**
         * Callback method when the stroke has started.
         */
        void onInkStarted();

        void onInkCompleted(List<double[]> mStrokeOutline);
    }
}
