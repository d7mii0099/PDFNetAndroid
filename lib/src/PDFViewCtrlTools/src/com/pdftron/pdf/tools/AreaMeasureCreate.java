package com.pdftron.pdf.tools;

import android.graphics.Canvas;
import android.graphics.Matrix;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import android.view.MotionEvent;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.annots.Polygon;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.MeasureInfo;
import com.pdftron.pdf.model.RulerItem;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.MeasureImpl;

import java.util.ArrayList;

@Keep
public class AreaMeasureCreate extends PolygonCreate {

    private MeasureImpl mMeasureImpl;

    /**
     * Class constructor
     */
    public AreaMeasureCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);

        mMeasureImpl = new MeasureImpl(getCreateAnnotType());

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        setSnappingEnabled(toolManager.isSnappingEnabledForMeasurementTools());
    }

    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolManager.ToolMode.AREA_MEASURE_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return AnnotStyle.CUSTOM_ANNOT_TYPE_AREA_MEASURE;
    }

    @Override
    public void setupAnnotProperty(AnnotStyle annotStyle) {
        super.setupAnnotProperty(annotStyle);

        mMeasureImpl.setupAnnotProperty(mPdfViewCtrl.getContext(), annotStyle);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        mMeasureImpl.handleDown(mPdfViewCtrl.getContext());

        boolean result = super.onDown(e);

        mLoupeEnabled = true;
        setLoupeInfo(e.getX(), e.getY());
        animateLoupe(true);

        return result;
    }

    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        boolean result = super.onMove(e1, e2, x_dist, y_dist);

        setLoupeInfo(e2.getX(), e2.getY());

        return result;
    }

    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        animateLoupe(false);
        return super.onUp(e, priorEventMode);
    }

    @Override
    protected Annot createMarkup(@NonNull PDFDoc doc, ArrayList<Point> pagePoints) throws PDFNetException {
        Polygon polygon = new Polygon(super.createMarkup(doc, pagePoints));
        polygon.setContents(adjustContents());
        mMeasureImpl.commit(polygon);
        return polygon;
    }

    private String adjustContents() {
        return adjustContents(mMeasureImpl, mPagePoints);
    }

    static String adjustContents(MeasureImpl measureImpl, ArrayList<Point> points) {
        double area = getArea(points);
        MeasureInfo axis = measureImpl.getAxis();
        MeasureInfo areaMeasure = measureImpl.getMeasure();
        if (axis == null || areaMeasure == null) {
            return "";
        }

        double convertedArea = area * axis.getFactor() * axis.getFactor() * areaMeasure.getFactor();
        return measureImpl.getMeasurementText(convertedArea, areaMeasure);
    }

    static double getArea(ArrayList<Point> points) {
        int numPoints = points.size();
        double area = 0;

        for (int i = 0; i < numPoints; i++) {
            Point point = points.get(i);
            double addX = point.x;
            double addY = points.get(i == numPoints - 1 ? 0 : i + 1).y;
            double subX = points.get(i == numPoints - 1 ? 0 : i + 1).x;
            double subY = point.y;
            area += addX * addY - subX * subY;
        }
        return Math.abs(area) / 2;
    }

    public static void adjustContents(Annot annot, RulerItem rulerItem, ArrayList<Point> points) {
        try {
            MeasureImpl measure = new MeasureImpl(AnnotUtils.getAnnotType(annot));
            measure.updateRulerItem(rulerItem);
            String result = adjustContents(measure, points);
            annot.setContents(result);
            measure.commit(annot);
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
    }

    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        super.onDraw(canvas, tfm);

        drawLoupe();
    }

    @Override
    protected boolean canDrawLoupe() {
        return !mDrawingLoupe;
    }

    @Override
    protected int getLoupeType() {
        return LOUPE_TYPE_MEASURE;
    }
}
