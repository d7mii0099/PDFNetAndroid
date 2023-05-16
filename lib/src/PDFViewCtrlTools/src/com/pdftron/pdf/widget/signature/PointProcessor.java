package com.pdftron.pdf.widget.signature;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.pdf.StrokeOutlineBuilder;
import com.pdftron.pdf.utils.PathPool;
import com.pdftron.pdf.utils.PointFPool;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

/**
 * Responsible for processing touch events on a screen and converts strokes to stroke outlines for
 * a given thickness. If pressure sensitivity is enabled and available, will use pressure information
 * to vary the stroke thickness.
 */
class PointProcessor {

    @Nullable
    protected StrokeOutlineBuilder mCurrentOutlineBuilder;
    protected ArrayList<PointF> mCurrentCanvasStroke = new ArrayList<>();
    private final List<StrokeOutlineResult> mAllStrokeOutlines = new ArrayList<>();

    private final Observable<InkDrawInfo> mTouchEventToOutlinePathObservable;
    private final PublishSubject<InkEvent> mTouchEventSubject = PublishSubject.create();
    private final CompositeDisposable mDisposable = new CompositeDisposable();
    private final DrawCallback mDrawCallback;

    private final Bitmap mBitmap;
    private final Canvas mCanvas;
    private final Paint mPaint;
    private double mStrokeWidth;

    private final boolean mIsPressureSensitive = true; // for debugging

    private PointProcessor(
            int width,
            int height,
            @ColorInt int strokeColor,
            double strokeWidth,
            @NonNull final DrawCallback drawCallback
    ) {
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mStrokeWidth = strokeWidth;

        // init paint
        mPaint = new Paint();
        mPaint.setStrokeCap(Paint.Cap.ROUND); // this one is important
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setStrokeWidth(0);
        mPaint.setAntiAlias(true);
        mPaint.setColor(strokeColor);

        mDrawCallback = drawCallback;
        // this observable is cached so we can quickly redraw later by re-subscribing
        mTouchEventToOutlinePathObservable = outlineArrayToOutlineSegmentPath(touchEventToOutlineSegmentArray()).cache();
        mDisposable.add(subscribeToCanvasDrawer());
    }

    public PointProcessor(
            int width,
            int height,
            @ColorInt int strokeColor,
            double strokeWidth,
            double opacity,
            @NonNull final DrawCallback drawCallback
    ) {
        this(width, height, strokeColor, strokeWidth, drawCallback);
        mPaint.setAlpha((int) (255 * opacity));
    }

    private Disposable subscribeToCanvasDrawer() {
        return mTouchEventToOutlinePathObservable
                .observeOn(AndroidSchedulers.mainThread()) // run onComplete on the main thread
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        Utils.throwIfNotOnMainThread();
                        mDrawCallback.onComplete(mAllStrokeOutlines);
                    }
                })
                .observeOn(Schedulers.computation()) // we need to explicitly call observeOn the computation thread again.
                .subscribe(                          // Since the observable is cached, it will run on the main thread
                        new Consumer<InkDrawInfo>() {
                            @Override
                            public void accept(InkDrawInfo drawInfo) throws Exception {
                                Utils.throwIfOnMainThread();
                                // Draw the outline
                                mCanvas.drawPath(drawInfo.path, mPaint);
                                mDrawCallback.onDrawInfoReceived(drawInfo, mBitmap);
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                throw new RuntimeException(throwable);
                            }
                        }
                );
    }

    public void onDown(float x, float y, float pressure) {
        mTouchEventSubject.onNext(
                new InkEvent(
                        InkEventType.ON_TOUCH_DOWN,
                        x,
                        y,
                        mIsPressureSensitive ? pressure : 1.0f
                )
        );
    }

    public void onMove(float x, float y, float pressure) {
        mTouchEventSubject.onNext(
                new InkEvent(
                        InkEventType.ON_TOUCH_MOVE,
                        x,
                        y,
                        mIsPressureSensitive ? pressure : 1.0f
                )
        );
    }

    public void onUp(float x, float y, float pressure) {
        mTouchEventSubject.onNext(
                new InkEvent(
                        InkEventType.ON_TOUCH_UP,
                        x,
                        y,
                        mIsPressureSensitive ? pressure : 1.0f
                )
        );
    }

    public void destroy() {
        mDisposable.clear();
    }

    /**
     * Sets the color of the path drawn by the {@link PointProcessor}. Also refreshes the associated
     * canvas and redraws the strokes in the new color.
     *
     * @param color that will be used by the {@link PointProcessor} for drawing strokes
     */
    void setColorWithRedraw(@ColorInt int color) {
        mPaint.setColor(color);
        onRedrawIfInitialized();
    }

    /**
     * Sets the stroke width of the path drawn by the {@link PointProcessor}. If pressure sensitivity
     * is enabled and available, then the width will vary around the specified stroke width.
     * <p>
     * Does not refresh the canvas and will not redraw the strokes due to performance.
     *
     * @param strokeWidth that will be used by the {@link PointProcessor} for drawing strokes
     */
    void setStrokeWidth(float strokeWidth) {
        this.mStrokeWidth = strokeWidth;
    }

    /**
     * Finish processing points and emit on finish event. This object should not be used anymore.
     */
    public void finish() {
        mTouchEventSubject.onComplete();
    }

    private void onRedrawIfInitialized() {
        if (mBitmap != null) {
            // Dispose of all observers that are drawing to canvas and also clear the canvas
            mDisposable.clear();
            mBitmap.eraseColor(Color.TRANSPARENT);

            // re-draw stroke outlines, note previous points are cached

            // draw stroke outlines with new color, note previous points are cached
            mDisposable.add(subscribeToCanvasDrawer());
        }
    }

    @NonNull
    private Observable<double[]> touchEventToOutlineSegmentArray() {
        return mTouchEventSubject.serialize()
                .observeOn(Schedulers.computation())
                .map(new Function<InkEvent, double[]>() {
                    @Override
                    public double[] apply(InkEvent inkEvent) throws Exception {
                        Utils.throwIfOnMainThread();

                        // Add the point to StrokeOutlineBuilder
                        InkEventType eventType = inkEvent.eventType;
                        float x = inkEvent.x;
                        float y = inkEvent.y;
                        float pressure = inkEvent.pressure;

                        return handleTouches(eventType, x, y, pressure);
                    }
                });
    }

    private double[] handleTouches(InkEventType event, float x, float y, float pressure) {
        // On touch down create a new stroke, on touch move add to the
        // existing stroke, and on touch up
        switch (event) {
            case ON_TOUCH_DOWN:
                return handleOnDown(x, y, pressure);
            case ON_TOUCH_MOVE:
                return handleOnMove(x, y, pressure);
            case ON_TOUCH_UP:
                // We ignore data for on up events, and do not store it otherwise it might be
                // too close to the previous on move event
                return handleOnUp(pressure);
        }
        throw new RuntimeException("Missing check for event type");
    }

    protected double[] handleOnDown(float x, float y, float pressure) {
        mCurrentOutlineBuilder = new StrokeOutlineBuilder(mStrokeWidth);
        mCurrentCanvasStroke = new ArrayList<>();
        mCurrentOutlineBuilder.addPoint(x, y, pressure);
        mCurrentCanvasStroke.add(PointFPool.getInstance().obtain(x, y));
        return mCurrentOutlineBuilder.getOutline();
    }

    protected double[] handleOnMove(float x, float y, float pressure) {
        if (mCurrentOutlineBuilder != null) {
            mCurrentOutlineBuilder.addPoint(x, y, pressure);
            mCurrentCanvasStroke.add(PointFPool.getInstance().obtain(x, y));
            return mCurrentOutlineBuilder.getOutline();
        } else {
            return new double[]{};
        }
    }

    // We ignore data for on up events, and do not store it otherwise it might be
    // too close to the previous on move event
    protected double[] handleOnUp(float pressure) {
        if (mCurrentOutlineBuilder != null) {
            StrokeOutlineResult strokeOutlineResult =
                    new StrokeOutlineResult(mCurrentCanvasStroke,
                            mCurrentOutlineBuilder.getOutline());
            mAllStrokeOutlines.add(strokeOutlineResult);

            return mCurrentOutlineBuilder.getOutline();
        } else {
            return new double[]{};
        }
    }

    // observable must run in background thread otherwise an exception will br thrown
    @NonNull
    private Observable<InkDrawInfo> outlineArrayToOutlineSegmentPath(Observable<double[]> observable) {
        return observable
                .filter(new Predicate<double[]>() {
                    @Override
                    public boolean test(@NonNull double[] outline) throws Exception {
                        return outline.length > 8; // required as we may pass empty in cases where the threshold is not met
                    }
                })
                .map(new Function<double[], InkDrawInfo>() {
                    @Override
                    public InkDrawInfo apply(@NonNull double[] outline) throws Exception {
                        Utils.throwIfOnMainThread();
                        Path pathf = PathPool.getInstance().obtain();
                        pathf.setFillType(Path.FillType.WINDING);

                        double left = outline[0];
                        double top = outline[1];
                        double right = outline[0];
                        double bottom = outline[1];

                        pathf.moveTo((float) outline[0], (float) outline[1]);
                        for (int i = 2, cnt = outline.length; i < cnt; i += 6) {

                            // Curve will reside in convex hull of control points so
                            // determine boundary to draw
                            for (int k = 0; k <= 5; k += 2) {
                                double x = outline[i + k];
                                double y = outline[i + k + 1];
                                left = Math.min(x, left);
                                top = Math.min(y, top);
                                right = Math.max(x, right);
                                bottom = Math.max(y, bottom);
                            }

                            pathf.cubicTo((float) outline[i], (float) outline[i + 1], (float) outline[i + 2],
                                    (float) outline[i + 3], (float) outline[i + 4], (float) outline[i + 5]);
                        }
                        int fudge = 2;
                        return new InkDrawInfo( // fudge the drawing box size by expanding a couple of pixels, just in case
                                (int) left - fudge,
                                (int) right + fudge,
                                (int) top - fudge,
                                (int) bottom + fudge,
                                pathf,
                                mPaint
                        );
                    }
                });
    }

    public interface DrawCallback {
        void onDrawInfoReceived(@NonNull InkDrawInfo drawInfo, @NonNull Bitmap bitmap);

        void onComplete(List<StrokeOutlineResult> mStrokeOutlines);
    }
}
