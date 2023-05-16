package com.pdftron.pdf.tools;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.view.MotionEvent;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Polygon;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.utils.MeasureImpl;
import com.pdftron.pdf.utils.MeasureUtils;

import java.util.ArrayList;

@Keep
public class RectAreaMeasureCreate extends RectCreate {

    private MeasureImpl mMeasureImpl;

    /**
     * Class constructor
     *
     * @param ctrl
     */
    public RectAreaMeasureCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);

        mMeasureImpl = new MeasureImpl(getCreateAnnotType());

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        setSnappingEnabled(toolManager.isSnappingEnabledForMeasurementTools());
    }

    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolManager.ToolMode.RECT_AREA_MEASURE_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return AnnotStyle.CUSTOM_ANNOT_TYPE_RECT_AREA_MEASURE;
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
    protected Annot createMarkup(@NonNull PDFDoc doc, Rect bbox) throws PDFNetException {
        Polygon poly = new Polygon(Polygon.create(doc, Annot.e_Polygon, bbox));
        ArrayList<Point> pagePoints = convRectToPoints(bbox);

        int pointIdx = 0;
        for (Point point : pagePoints) {
            poly.setVertex(pointIdx++, point);
        }
        bbox.inflate(mThickness);
        poly.setRect(bbox);

        poly.setContents(adjustContents(pagePoints));
        mMeasureImpl.commit(poly);
        // indicating this is a rectangular area measure
        // so we can resize it like a rectangle
        poly.setCustomData(MeasureUtils.K_RECT_AREA, PDFTRON_ID);
        return poly;
    }

    private String adjustContents(ArrayList<Point> pagePoints) {
        return AreaMeasureCreate.adjustContents(mMeasureImpl, pagePoints);
    }

    @NonNull
    private static ArrayList<Point> convRectToPoints(@NonNull Rect bbox) throws PDFNetException {
        ArrayList<Point> pts = new ArrayList<>();
        bbox.normalize();
        pts.add(new Point(bbox.getX1(), bbox.getY1()));
        pts.add(new Point(bbox.getX1(), bbox.getY2()));
        pts.add(new Point(bbox.getX2(), bbox.getY2()));
        pts.add(new Point(bbox.getX2(), bbox.getY1()));
        return pts;
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
