package com.pdftron.pdf.model.ink;

import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.utils.PathPool;
import com.pdftron.pdf.utils.PressureInkUtils;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PressureInkItem extends InkItem {

    public List<Float> currentActivePressure = new ArrayList<>(); // This list is modifiable and must be null if the user is not drawing (i.e. after onUp)
    public final List<List<Float>> finishedPressures; // Pressure list in this List are not modifiable

    // Create a copy of an ink item
    // Paint is not copied as it's always lazy initialized
    @Override
    public InkItem copy() {
        PressureInkItem newInkItem = new PressureInkItem(
                this.id,
                null,
                null,
                new ArrayList<>(this.finishedStrokes),
                new ArrayList<>(this.finishedPressures),
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

    public PressureInkItem(String id,
            @Nullable List<PointF> currentActiveStroke,
            @Nullable List<Float> currentActivePressure,
            List<List<PointF>> finishedStrokes,
            List<List<Float>> finishedPressures,
            int pageNumber,
            int color,
            float opacity,
            float baseThickness,
            float paintThickness,
            boolean isStylus) {
        super(id, currentActiveStroke, finishedStrokes, pageNumber, color, opacity, baseThickness, paintThickness, isStylus);
        this.finishedPressures = finishedPressures;
        this.currentActivePressure = currentActivePressure;
    }

    public PressureInkItem(int pageNumber, int color, float opacity, float baseThickness, boolean isStylus, PDFViewCtrl pdfViewCtrl) {
        super(pageNumber, color, opacity, baseThickness, isStylus, pdfViewCtrl);
        finishedPressures = new ArrayList<>();
    }

    @Override
    protected void onDown(float x, float y, float pressure) {
        super.onDown(x, y, pressure);
        currentActivePressure = new ArrayList<>();
        currentActivePressure.add(pressure);
    }

    @Override
    protected void onMove(float x, float y, float pressure) {
        super.onMove(x, y, pressure);
        if (currentActivePressure == null) {
            throw new RuntimeException("This should not happen. Missing onDown call");
        }
        currentActivePressure.add(pressure);
    }

    @Override
    protected void onUp() {
        super.onUp();
        if (currentActivePressure == null) {
            throw new RuntimeException("This should not happen. Missing onDown call");
        }
        List<Float> newPressure = Collections.unmodifiableList(currentActivePressure);
        finishedPressures.add(newPressure);
        currentActivePressure = null;
    }

    @Override
    protected Path createPathFromCurrentActiveStroke(@NonNull List<PointF> points, @NonNull PDFViewCtrl pdfViewCtrl, @Nullable PointF offset) {
        return createPathFromPressurePagePoint(points, currentActivePressure, pdfViewCtrl, offset);
    }

    @Override
    protected Path createPathFromFinishedStroke(int index, @NonNull PDFViewCtrl pdfViewCtrl, @Nullable PointF offset) {
        return createPathFromPressurePagePoint(finishedStrokes.get(index), finishedPressures.get(index), pdfViewCtrl, offset);
    }

    private Path createPathFromPressurePagePoint(List<PointF> points, List<Float> pressure, PDFViewCtrl pdfViewCtrl, PointF offset) {
        float xOffset = 0;
        float yOffset = 0;
        if (offset != null) {
            xOffset = offset.x;
            yOffset = offset.y;
        }

        List<List<PointF>> pathList = new ArrayList<>();
        List<List<Float>> thicknessesList = new ArrayList<>();
        thicknessesList.add(pressure);

        // First convert page space to horizontal scrolling pt
        List<PointF> convertedPoints = new ArrayList<>();
        for (PointF pt : points) {
            float[] newPt =  convPagePtToHorizontalScrollingPt(pt.x, pt.y, pdfViewCtrl);
            convertedPoints.add(new PointF(newPt[0] - xOffset, newPt[1] - yOffset));
        }
        pathList.add(convertedPoints);

        // Convert page points and thickness to outline points
        List<double[]> outlines =
                PressureInkUtils.generateOutlines(
                        pathList,
                        thicknessesList,
                        paintThickness
                );

        // Finally convert outline points to path object for canvas
        Path path = PathPool.getInstance().obtain();
        path.setFillType(Path.FillType.WINDING);

        double[] outline = outlines == null ? null : outlines.get(0);
        if (outline != null && outline.length > 2) {

            path.moveTo((float) outline[0], (float) outline[1]);
            for (int i = 2, cnt = outline.length; i < cnt; i += 6) {
                path.cubicTo((float) outline[i], (float) outline[i + 1], (float) outline[i + 2],
                        (float) outline[i + 3], (float) outline[i + 4], (float) outline[i + 5]);
            }
        }

        return path;
    }

    @Override
    public Paint getPaint(@NonNull PDFViewCtrl pdfViewCtrl) {
        if (paint == null) {
            paint = new Paint();
            paint.setStrokeCap(Paint.Cap.ROUND); // this one is important
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setStrokeWidth(0);
            paint.setAntiAlias(true);
            paint.setColor(Utils.getPostProcessedColor(pdfViewCtrl, color));
            paint.setAlpha((int) (255 * opacity));
        }

        return paint;
    }
}
