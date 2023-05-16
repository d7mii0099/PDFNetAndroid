package com.pdftron.pdf.model.ink;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.Log;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.annots.Ink;
import com.pdftron.pdf.utils.Logger;
import com.pdftron.pdf.utils.PathPool;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Class used to describe all ink stroke related data.
 */
public class InkItem {

    private final static String TAG = InkItem.class.getName();
    private static boolean sDebug = false;

    // Copied from PDFViewCtrl. S-Pen motion events
    private static final int ACTION_PEN_DOWN = 211;
    private static final int ACTION_PEN_MOVE = 213;
    private static final int ACTION_PEN_UP = 212;

    public final String id;
    @Nullable
    public List<PointF> currentActiveStroke = new ArrayList<>(); // This list is modifiable and must be null if the user is not drawing (i.e. after onUp)
    public final List<List<PointF>> finishedStrokes; // Point list in this List are not modifiable
    private final Map<List<PointF>, Path> drawPaths = new HashMap<>(); // is lazy populated the first time a finished stroke is drawn
    public final int pageNumber;
    public final int color;
    public final float opacity;
    public final float baseThickness;
    public float paintThickness;
    public final boolean isStylus;

    public Paint paint = null; // invariant: paint must not be changed after setting it in the constructor. it is lazy initialized in getPaint
    @Nullable
    public List<PointF> previousStroke;
    public boolean shouldAnimateUndoRedo = true;

    /**
     * Create a copy of an InkItem and return it.
     * Note that Paint is not copied as it's always lazy initialized
     *
     * @return a new instance of InkItem.
     */
    public InkItem copy() {
        InkItem newInkItem = new InkItem(
                this.id,
                null,
                new ArrayList<>(this.finishedStrokes),
                this.pageNumber,
                this.color,
                this.opacity,
                this.baseThickness,
                this.paintThickness,
                this.isStylus
        );
        newInkItem.shouldAnimateUndoRedo = this.shouldAnimateUndoRedo;
        newInkItem.previousStroke = this.previousStroke == null ? null : new ArrayList<>(this.previousStroke);
        return newInkItem;
    }

    public InkItem(String id,
            @Nullable List<PointF> currentActiveStroke,
            List<List<PointF>> finishedStrokes,
            int pageNumber,
            int color,
            float opacity,
            float baseThickness,
            float paintThickness,
            boolean isStylus
    ) {
        this.id = id;
        this.currentActiveStroke = currentActiveStroke;
        this.finishedStrokes = finishedStrokes;
        this.pageNumber = pageNumber;
        this.color = color;
        this.opacity = opacity;
        this.baseThickness = baseThickness;
        this.paintThickness = paintThickness;
        this.isStylus = isStylus;
    }

    public InkItem(int pageNumber,
            int color,
            float opacity,
            float baseThickness,
            boolean isStylus,
            PDFViewCtrl pdfViewCtrl
    ) {
        this.id = UUID.randomUUID().toString();

        this.finishedStrokes = new ArrayList<>();

        float zoom = (float) pdfViewCtrl.getZoom();
        paintThickness = baseThickness * zoom;

        this.color = color;
        this.opacity = opacity;
        this.baseThickness = baseThickness;
        this.isStylus = isStylus;
        this.pageNumber = pageNumber;
    }

    public void addPoint(float x, float y, float pressure, int action) {
        switch (action) {
            case ACTION_PEN_DOWN:
            case MotionEvent.ACTION_DOWN:
                onDown(x, y, pressure);
                break;
            case ACTION_PEN_MOVE:
            case MotionEvent.ACTION_MOVE:
                onMove(x, y, pressure);
                break;
            case ACTION_PEN_UP:
            case MotionEvent.ACTION_UP:
                // We ignore data for on up events, and do not store it otherwise it might be
                // too close to the previous on move event
                onUp();
                break;
            default:
                if (sDebug) {
                    Log.d(InkItem.class.getName(), "Unhandled state " + action);
                }
        }
    }

    private boolean isPageInPages(PDFViewCtrl pdfViewCtrl, int page) {
        int[] pages = pdfViewCtrl.getVisiblePagesInTransition();
        for (int p : pages) {
            if (p == page) {
                return true;
            }
        }
        return false;
    }

    /**
     * When draw, we will lazy populate the paths for the finished strokes (this is done by
     * converting page points via convPagePtToHorizontalScrollingPt). When the viewer zooms, we will
     * need to delete the old paths and re-calculate/re-populate new paths.
     */
    public void draw(@NonNull Canvas canvas, @NonNull PDFViewCtrl pdfViewCtrl, @Nullable Matrix transform, @Nullable PointF offset) {
        // Should only draw the visible portions of the screen
        if (!pdfViewCtrl.isContinuousPagePresentationMode(pdfViewCtrl.getPagePresentationMode()) &&
                !isPageInPages(pdfViewCtrl, pageNumber)) {
            return;
        }
        // Create the path for current active stroke and draw it. We do not store this as it can change in the future
        if (currentActiveStroke != null) {
            Path currentPath = createPathFromCurrentActiveStroke(currentActiveStroke, pdfViewCtrl, offset);
            drawPathOnCanvas(canvas, currentPath, pdfViewCtrl, transform);
        }

        // If zoom changes
        float newPaintThickness = (float) (baseThickness * pdfViewCtrl.getZoom());
        boolean hasZoomedSinceLastDraw = newPaintThickness != this.paintThickness;
        if (hasZoomedSinceLastDraw) {
            this.paintThickness = newPaintThickness;
            drawPaths.clear();
        }

        // Create the path for finished strokes and store this, since this will not change unless we zoom.
        for (int i = 0; i < finishedStrokes.size(); i++) {
            List<PointF> screenStroke = finishedStrokes.get(i);
            Path previousPath;
            if (drawPaths.containsKey(screenStroke)) {
                previousPath = drawPaths.get(screenStroke);
            } else {
                previousPath = createPathFromFinishedStroke(i, pdfViewCtrl, offset);
                drawPaths.put(screenStroke, previousPath);
            }
            //noinspection ConstantConditions
            drawPathOnCanvas(canvas, previousPath, pdfViewCtrl, transform);
        }
    }

    protected Path createPathFromCurrentActiveStroke(@NonNull List<PointF> points, @NonNull PDFViewCtrl pdfViewCtrl, @Nullable PointF offset) {
        return createPathFromPagePoint(points, pdfViewCtrl, offset);
    }

    protected Path createPathFromFinishedStroke(int index, @NonNull PDFViewCtrl pdfViewCtrl, @Nullable PointF offset) {
        return createPathFromPagePoint(finishedStrokes.get(index), pdfViewCtrl, offset);
    }

    private Path createPathFromPagePoint(@NonNull List<PointF> points, @NonNull PDFViewCtrl pdfViewCtrl, @Nullable PointF offset) {
        float xOffset = 0;
        float yOffset = 0;
        if (offset != null) {
            xOffset = offset.x;
            yOffset = offset.y;
        }

        Path path = PathPool.getInstance().obtain();

        if (points.size() < 1) {
            return path;
        } else if (points.size() == 1) {
            PointF pt1 = points.get(0);
            float[] scrPt = convPagePtToHorizontalScrollingPt(pt1.x, pt1.y, pdfViewCtrl);
            path.moveTo(scrPt[0], scrPt[1]);
            // Workaround to draw a circle using Path. Just create a very small path with offset from the first point.
            path.lineTo(scrPt[0] + 0.01f, scrPt[1]);
        } else {
            // calculate points
            double[] inputLine = new double[(points.size() * 2)];
            for (int i = 0, cnt = points.size(); i < cnt; i++) {
                float[] pts = convPagePtToHorizontalScrollingPt(points.get(i).x, points.get(i).y, pdfViewCtrl);
                inputLine[i * 2] = pts[0]  - xOffset;
                inputLine[i * 2 + 1] = pts[1] - yOffset;
            }

            double[] currentBeizerPts;
            try {
                currentBeizerPts = Ink.getBezierControlPoints(inputLine);
            } catch (Exception e) {
                return path;
            }

            path.moveTo((float) currentBeizerPts[0], (float) currentBeizerPts[1]);
            for (int i = 2, cnt = currentBeizerPts.length; i < cnt; i += 6) {
                path.cubicTo((float) currentBeizerPts[i], (float) currentBeizerPts[i + 1], (float) currentBeizerPts[i + 2],
                        (float) currentBeizerPts[i + 3], (float) currentBeizerPts[i + 4], (float) currentBeizerPts[i + 5]);
            }
        }
        return path;
    }

    protected float[] convPagePtToHorizontalScrollingPt(float x, float y, @NonNull PDFViewCtrl pdfViewCtrl) {
        double[] pt = pdfViewCtrl.convPagePtToHorizontalScrollingPt(x, y, this.pageNumber);
        return new float[]{(float) pt[0], (float) pt[1]};
    }

    private void drawPathOnCanvas(@NonNull Canvas canvas, @NonNull Path path, @NonNull PDFViewCtrl pdfViewCtrl, @Nullable Matrix transform) {

        Path drawPath = path;
        if (transform != null) {
            drawPath = new Path();
            drawPath.addPath(path, transform);
        }

        if (pdfViewCtrl.isMaintainZoomEnabled()) {
            canvas.save();
            try {
                canvas.translate(0,  - pdfViewCtrl.getScrollYOffsetInTools(this.pageNumber));
                canvas.drawPath(drawPath, getPaint(pdfViewCtrl));
            } finally {
                canvas.restore();
            }
        } else {
            canvas.drawPath(drawPath, getPaint(pdfViewCtrl));
        }
    }

    public Paint getPaint(@NonNull PDFViewCtrl pdfViewCtrl) {
        if (paint == null) {
            paint = new Paint();
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND); // this one is important
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(paintThickness);
            paint.setAntiAlias(true);
            paint.setColor(Utils.getPostProcessedColor(pdfViewCtrl, color));
            paint.setAlpha((int) (255 * opacity));
        }

        if (paintThickness != paint.getStrokeWidth()) {
            paint.setStrokeWidth(paintThickness);
        }

        return paint;
    }

    public void reset() {
        if (currentActiveStroke != null) {
            currentActiveStroke.clear();
        }
        if (finishedStrokes != null) {
            finishedStrokes.clear();
        }
        drawPaths.clear();
    }

    protected void onDown(float x, float y, float pressure) {
        currentActiveStroke = new ArrayList<>();
        currentActiveStroke.add(new PointF(x, y));
    }

    protected void onMove(float x, float y, float pressure) {
        if (currentActiveStroke == null) {
            Logger.INSTANCE.LogE(TAG, "currentActiveStroke is null in onMove. This should not happen. Missing onDown call");
            return;
        }
        currentActiveStroke.add(new PointF(x, y));
    }

    // We ignore data for on up events, and do not store it otherwise it might be
    // too close to the previous on move event
    protected void onUp() {
        if (currentActiveStroke == null) {
            Logger.INSTANCE.LogE(TAG, "currentActiveStroke is null in onUp. This should not happen. Missing onDown call");
            return;
        }
        List<PointF> newStroke = Collections.unmodifiableList(currentActiveStroke);
        finishedStrokes.add(newStroke);
        previousStroke = newStroke;
        currentActiveStroke = null;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final InkItem other = (InkItem) obj;
        return this.id.equals(other.id);
    }
}
