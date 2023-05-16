//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PointF;
import android.view.MotionEvent;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Line;
import com.pdftron.pdf.model.LineEndingStyle;
import com.pdftron.pdf.model.LineStyle;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.DrawingUtils;

/**
 * This class is for creating a line annotation.
 */
@Keep
public class LineCreate extends SimpleShapeCreate {

    protected PointF mStartPt, mEndPt;
    protected PointF mSPt1, mSPt2, mSPt3, mSPt4, mEPt1, mEPt2, mEPt3, mEPt4;
    protected Path mOnDrawPath = new Path();

    /**
     * Class constructor
     */
    public LineCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        mNextToolMode = getToolMode();
        mStartPt = new PointF(0, 0);
        mEndPt = new PointF(0, 0);
        mSPt1 = new PointF(0, 0);
        mSPt2 = new PointF(0, 0);
        mSPt3 = new PointF(0, 0);
        mSPt4 = new PointF(0, 0);
        mEPt1 = new PointF(0, 0);
        mEPt2 = new PointF(0, 0);
        mEPt3 = new PointF(0, 0);
        mEPt4 = new PointF(0, 0);
        mHasLineStyle = true;
        mHasLineStartStyle = true;
        mHasLineEndStyle = true;
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.LINE_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Line;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onDown(MotionEvent)}.
     */
    @Override
    public boolean onDown(MotionEvent e) {
        if (super.onDown(e)) {
            return true;
        }
        mZoom = mPdfViewCtrl.getZoom();
        return false;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        super.onMove(e1, e2, x_dist, y_dist);

        // We are scrolling
        if (mAllowTwoFingerScroll) {
            return false;
        }
        if (mAllowOneFingerScrollWithStylus) {
            return false;
        }

        return true;
    }

    public static void calculateLineEndingStyle(LineEndingStyle lineEndingStyle, PointF startPoint, PointF endPoint,
            PointF endPoint1, PointF endPoint2, PointF endPoint3, PointF endPoint4, float thickness, double zoom) {
        switch (lineEndingStyle) {
            case BUTT:
                DrawingUtils.calButt(startPoint, endPoint, endPoint1, endPoint2, thickness, zoom);
                break;
            case DIAMOND:
                DrawingUtils.calcDiamond(startPoint, endPoint, endPoint1, endPoint2, endPoint3, thickness, zoom);
                break;
            case CIRCLE:
                DrawingUtils.calcCircle(startPoint, endPoint, endPoint1, thickness, zoom);
                break;
            case OPEN_ARROW:
                DrawingUtils.calcOpenArrow(startPoint, endPoint, endPoint1, endPoint2, thickness, zoom);
                break;
            case CLOSED_ARROW:
                DrawingUtils.calcClosedArrow(startPoint, endPoint, endPoint1, endPoint2, thickness, zoom);
                break;
            case R_OPEN_ARROW:
                DrawingUtils.calcROpenArrow(startPoint, endPoint, endPoint1, endPoint2, thickness, zoom);
                break;
            case R_CLOSED_ARROW:
                DrawingUtils.calcRClosedArrow(startPoint, endPoint, endPoint1, endPoint2, thickness, zoom);
                break;
            case SLASH:
                DrawingUtils.calcSlash(startPoint, endPoint, endPoint1, endPoint2, thickness, zoom);
                break;
            case SQUARE:
                DrawingUtils.calcSquare(startPoint, endPoint, endPoint1, endPoint2, endPoint3, endPoint4, thickness, zoom);
                break;
        }
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#doneTwoFingerScrolling()}.
     */
    @Override
    protected void doneTwoFingerScrolling() {
        super.doneTwoFingerScrolling();
        mPt2.set(mPt1);
        mStartPt.set(mPt1);
        mEndPt.set(mPt1);
        mSPt1.set(mPt1);
        mSPt2.set(mPt1);
        mSPt3.set(mPt1);
        mSPt4.set(mPt1);
        mEPt1.set(mPt1);
        mEPt2.set(mPt1);
        mEPt3.set(mPt1);
        mEPt4.set(mPt1);
        mPdfViewCtrl.invalidate();
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#createMarkup(PDFDoc, Rect)}.
     */
    @Override
    protected Annot createMarkup(@NonNull PDFDoc doc, Rect bbox) throws PDFNetException {
        return Line.create(doc, bbox);
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#resetPts()}.
     */
    @Override
    protected void resetPts() {
        mPt1.set(0, 0);
        mPt2.set(0, 0);
        mStartPt.set(0, 0);
        mEndPt.set(0, 0);
        mSPt1.set(0, 0);
        mSPt2.set(0, 0);
        mSPt3.set(0, 0);
        mSPt4.set(0, 0);
        mEPt1.set(0, 0);
        mEPt2.set(0, 0);
        mEPt3.set(0, 0);
        mEPt4.set(0, 0);
    }

    @Override
    protected boolean canTapToCreate() {
        return true;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onDraw(Canvas, Matrix)}.
     */
    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        if (mAllowTwoFingerScroll) {
            return;
        }
        if (mIsAllPointsOutsidePage) {
            return;
        }
        if (mSkipAfterQuickMenuClose) {
            return;
        }

        DrawingUtils.drawLine(canvas, mPt1, mPt2, mStartPt, mEndPt, mSPt1, mSPt2, mSPt3, mSPt4, mEPt1, mEPt2, mEPt3, mEPt4,
                mLineStartStyle, mLineEndStyle,
                mOnDrawPath, mPaint,
                (mLineStyle == LineStyle.DASHED ? mDashPathEffect : null),
                mThickness, mZoom);
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#getDefaultNextTool()}.
     */
    @Override
    protected ToolMode getDefaultNextTool() {
        return ToolMode.ANNOT_EDIT_LINE;
    }
}
