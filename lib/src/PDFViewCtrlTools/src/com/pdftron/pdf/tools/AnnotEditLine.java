//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.annots.Line;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.RulerItem;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.MeasureUtils;
import com.pdftron.pdf.utils.Utils;

/**
 * This class is responsible for editing a selected line or arrow, e.g., moving and resizing.
 */
@SuppressWarnings("WeakerAccess")
@Keep
public class AnnotEditLine extends AnnotEdit {

    private Line mLine = null;
    private RectF mTempRect;
    private Path mPath;

    private final int e_start_point = 0;
    private final int e_end_point = 1;

    /**
     * Class constructor
     */
    public AnnotEditLine(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        setOriginalCtrlPtsDisabled(true);

        CTRL_PTS_CNT = 2;
        mCtrlPts = new PointF[CTRL_PTS_CNT];
        mCtrlPtsOnDown = new PointF[CTRL_PTS_CNT];
        for (int i = 0; i < 2; ++i) {
            mCtrlPts[i] = new PointF();
            mCtrlPtsOnDown[i] = new PointF();
        }

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mBBox = new RectF();
        mTempRect = new RectF();
        mPath = new Path();
        mModifiedAnnot = false;
        mCtrlPtsSet = false;
        mScaled = false;
    }

    /**
     * The overload implementation of {@link Tool#onCreate()}.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        if (mAnnot != null) {
            mHasSelectionPermission = hasPermission(mAnnot, ANNOT_PERMISSION_SELECTION);
            mHasMenuPermission = hasPermission(mAnnot, ANNOT_PERMISSION_MENU);

            try {
                mLine = new Line(mAnnot);
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }


            // Remember the page bounding box in client space; this is used to ensure while
            // moving/resizing, the widget doesn't go beyond the page boundary.
            mPageCropOnClientF = Utils.buildPageBoundBoxOnClient(mPdfViewCtrl, mAnnotPageNum);

        }
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.ANNOT_EDIT_LINE;
    }

    @Override
    protected void setCtrlPts() {
        setCtrlPts(true);
    }

    @Override
    protected void setCtrlPts(boolean resetAnnotView) {
        if (mPdfViewCtrl == null ||
            mLine == null ||
            onInterceptAnnotationHandling(mAnnot)) {
            return;
        }

        mCtrlPtsSet = true;

        try {
            float x1 = (float) mLine.getStartPoint().x;
            float y1 = (float) mLine.getStartPoint().y;
            float x2 = (float) mLine.getEndPoint().x;
            float y2 = (float) mLine.getEndPoint().y;

            float sx = mPdfViewCtrl.getScrollX();
            float sy = mPdfViewCtrl.getScrollY();

            // Start point
            double[] pts = mPdfViewCtrl.convPagePtToScreenPt(x1, y1, mAnnotPageNum);
            mCtrlPts[e_start_point].x = (float) pts[0] + sx;
            mCtrlPts[e_start_point].y = (float) pts[1] + sy;

            // End point
            pts = mPdfViewCtrl.convPagePtToScreenPt(x2, y2, mAnnotPageNum);
            mCtrlPts[e_end_point].x = (float) pts[0] + sx;
            mCtrlPts[e_end_point].y = (float) pts[1] + sy;

            // Compute the bounding box
            mBBox.left = Math.min(mCtrlPts[e_start_point].x, mCtrlPts[e_end_point].x) - mCtrlRadius;
            mBBox.top = Math.min(mCtrlPts[e_start_point].y, mCtrlPts[e_end_point].y) - mCtrlRadius;
            mBBox.right = Math.max(mCtrlPts[e_start_point].x, mCtrlPts[e_end_point].x) + mCtrlRadius;
            mBBox.bottom = Math.max(mCtrlPts[e_start_point].y, mCtrlPts[e_end_point].y) + mCtrlRadius;

            addAnnotView();
            updateAnnotView();

            for (int i = 0; i < 2; ++i) {
                mCtrlPtsOnDown[i].set(mCtrlPts[i]);
            }

        } catch (PDFNetException e) {
            mCtrlPtsSet = false;
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    @Override
    protected boolean canAddAnnotView(Annot annot, AnnotStyle annotStyle) {
        if (!((ToolManager) mPdfViewCtrl.getToolManager()).isRealTimeAnnotEdit()) {
            return false;
        }
        return mPdfViewCtrl.isAnnotationLayerEnabled() || !annotStyle.hasAppearance();
    }

    protected void updateAnnotView() {
        if (mAnnotView != null) {
            mAnnotView.setAnnotRect(getAnnotRect());
            int xOffset = mPdfViewCtrl.getScrollX();
            int yOffset = mPdfViewCtrl.getScrollY();
            mAnnotView.layout(xOffset,
                yOffset,
                xOffset + mPdfViewCtrl.getWidth(),
                yOffset + mPdfViewCtrl.getHeight());
            PointF start = new PointF(mCtrlPts[e_start_point].x - xOffset,
                    mCtrlPts[e_start_point].y - yOffset);
            PointF end = new PointF(mCtrlPts[e_end_point].x - xOffset,
                    mCtrlPts[e_end_point].y - yOffset);
            mAnnotView.setVertices(start, end);
            mAnnotView.invalidate();
        }
    }

    /**
     * The overload implementation of {@link Tool#onDown(MotionEvent)}.
     */
    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        super.onDraw(canvas, tfm);

        float left = mCtrlPts[e_start_point].x;
        float top = mCtrlPts[e_end_point].y;
        float right = mCtrlPts[e_end_point].x;
        float bottom = mCtrlPts[e_start_point].y;

        if (mAnnot != null) {
            if (mModifiedAnnot) {
                mPaint.setColor(mPdfViewCtrl.getResources().getColor(R.color.tools_annot_edit_line_shadow));
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setPathEffect(mDashPathEffect);
                // Bug in drawLine: https://code.google.com/p/android/issues/detail?id=29944
                // Need to use drawPath instead.
                // canvas.drawLine(right, top, left, bottom, mPaint);
                mPath.reset();
                mPath.moveTo(right, top);
                mPath.lineTo(left, bottom);
                canvas.drawPath(mPath, mPaint);
            }

            if (!mHasSelectionPermission) {
                drawSelectionBox(canvas,
                        left,
                        top,
                        right,
                        bottom
                );
            }
        }
    }

    /**
     * The overload implementation of {@link Tool#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        super.onUp(e, priorEventMode);

        if (mAnnotView != null) {
            mAnnotView.setActiveHandle(e_unknown);
        }

        mNextToolMode = ToolMode.ANNOT_EDIT_LINE;
        mScaled = false;

        if (!mHasMenuPermission) {
            if (mAnnot != null) {
                showMenu(getAnnotRect());
            }
        }

        if (mAnnot != null
            && (mModifiedAnnot
            || !mCtrlPtsSet
            || priorEventMode == PDFViewCtrl.PriorEventMode.SCROLLING
            || priorEventMode == PDFViewCtrl.PriorEventMode.PINCH
            || priorEventMode == PDFViewCtrl.PriorEventMode.DOUBLE_TAP)) {

            if (!mCtrlPtsSet) {
                setCtrlPts();
            }

            resizeLine(priorEventMode);

            // TODO: work around core issue where real time
            // annot editing is not supported yet
            setCtrlPts();

            showMenu(getAnnotRect());

            // Don't let the main view scroll
            return priorEventMode == PDFViewCtrl.PriorEventMode.SCROLLING || priorEventMode == PDFViewCtrl.PriorEventMode.FLING;

        } else {
            return false;
        }
    }

    private void resizeLine(PDFViewCtrl.PriorEventMode priorEventMode) {
        if (mAnnot == null || onInterceptAnnotationHandling(mAnnot)) {
            return;
        }
        boolean shouldCopyAnnot = shouldCopyAnnot(mAnnotPageNum);
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            if (mModifiedAnnot) {
                mModifiedAnnot = false;
                float x1 = mCtrlPts[e_start_point].x - mPdfViewCtrl.getScrollX();
                float y1 = mCtrlPts[e_start_point].y - mPdfViewCtrl.getScrollY();
                float x2 = mCtrlPts[e_end_point].x - mPdfViewCtrl.getScrollX();
                float y2 = mCtrlPts[e_end_point].y - mPdfViewCtrl.getScrollY();
                if (shouldCopyAnnot) {
                    float left, top;
                    if (x1 < x2) {
                        left = x1;
                    } else {
                        left = x2;
                    }
                    if (y1 < y2) {
                        top = y1;
                    } else {
                        top = y2;
                    }
                    float midX = left + mBBox.width() / 2.0f - mCtrlRadius;
                    float midY = top + mBBox.height() / 2.0f - mCtrlRadius;
                    int newPageNumber = mPdfViewCtrl.getPageNumberFromScreenPt(midX, midY);
                    // This occurs when the screen point is outside of a page
                    if (newPageNumber < 1) {
                        newPageNumber = mAnnotPageNum;
                    }
                    mAnnotPageNum = copyAnnotToNewPage(
                            mAnnot,
                            mAnnotPageNum,
                            newPageNumber
                    );
                }
                if (!shouldCopyAnnot) {
                    raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);
                }

                // Compute the new annotation position
                float width = mCtrlPts[e_start_point].x - mCtrlPts[e_end_point].x;
                float height = mCtrlPts[e_start_point].y - mCtrlPts[e_end_point].y;

                PointF snappedStartPoint = new PointF(mCtrlPts[e_start_point].x, mCtrlPts[e_start_point].y);
                PointF snappedEndPoint = new PointF(mCtrlPts[e_end_point].x, mCtrlPts[e_end_point].y);
                boolean hasSnappedStartPoint = snapPtToPage(snappedStartPoint, mAnnotPageNum);
                boolean hasSnappedEndPoint = snapPtToPage(snappedEndPoint, mAnnotPageNum);
                if (hasSnappedStartPoint && hasSnappedEndPoint) {
                    float newX1 = snappedStartPoint.x - mPdfViewCtrl.getScrollX();
                    float newY1 = snappedStartPoint.y - mPdfViewCtrl.getScrollY();
                    float newX2 = snappedEndPoint.x - mPdfViewCtrl.getScrollX();
                    float newY2 = snappedEndPoint.y - mPdfViewCtrl.getScrollY();
                    float shiftX1 = newX1 - x1;
                    float shiftY1 = newY1 - y1;
                    float shiftX2 = newX2 - x2;
                    float shiftY2 = newY2 - y2;
                    float totalShiftX = shiftX1 + shiftX2;
                    float totalShiftY = shiftY1 + shiftY2;

                    x1 += totalShiftX;
                    y1 += totalShiftY;
                    x2 += totalShiftX;
                    y2 += totalShiftY;

                } else if (hasSnappedStartPoint) {
                    x1 = snappedStartPoint.x - mPdfViewCtrl.getScrollX();
                    y1 = snappedStartPoint.y - mPdfViewCtrl.getScrollY();
                    x2 = x1 - width;
                    y2 = y1 - height;
                } else if (hasSnappedEndPoint) {
                    x2 = snappedEndPoint.x - mPdfViewCtrl.getScrollX();
                    y2 = snappedEndPoint.y - mPdfViewCtrl.getScrollY();
                    x1 = x2 + width;
                    y1 = y2 + height;
                }

                double[] pts1, pts2;
                pts1 = mPdfViewCtrl.convScreenPtToPagePt(x1, y1, mAnnotPageNum);
                pts2 = mPdfViewCtrl.convScreenPtToPagePt(x2, y2, mAnnotPageNum);
                com.pdftron.pdf.Rect new_annot_rect = new com.pdftron.pdf.Rect(pts1[0], pts1[1], pts2[0], pts2[1]);
                new_annot_rect.normalize();

                com.pdftron.pdf.Rect old_update_rect = null;
                if (!mPdfViewCtrl.isAnnotationLayerEnabled()) {
                    // Compute the old annotation position in screen space for update
                    double[] pts1_old, pts2_old;
                    com.pdftron.pdf.Rect r = mAnnot.getRect();
                    pts1_old = mPdfViewCtrl.convPagePtToScreenPt(r.getX1(), r.getY1(), mAnnotPageNum);
                    pts2_old = mPdfViewCtrl.convPagePtToScreenPt(r.getX2(), r.getY2(), mAnnotPageNum);
                    old_update_rect = new com.pdftron.pdf.Rect(pts1_old[0], pts1_old[1], pts2_old[0], pts2_old[1]);
                    old_update_rect.normalize();
                }

                mAnnot.resize(new_annot_rect);

                Line line = new Line(mAnnot);

                // first check if legacy key present
                RulerItem rulerItem = MeasureUtils.getRulerItemFromAnnot(mAnnot);
                if (rulerItem == null) {
                    rulerItem = RulerItem.getRulerItem(mAnnot); // legacy
                    RulerItem.removeRulerItem(mAnnot); // remove legacy key
                }
                if (null != rulerItem) {
                    RulerCreate.adjustContents(line, rulerItem, pts1[0], pts1[1], pts2[0], pts2[1]);
                }

                line.setStartPoint(new Point(pts1[0], pts1[1]));
                line.setEndPoint(new Point(pts2[0], pts2[1]));

                mAnnot.refreshAppearance();
                buildAnnotBBox();
                if (null != old_update_rect) {
                    mPdfViewCtrl.update(old_update_rect);   // Update the old position
                }
                mPdfViewCtrl.update(mAnnot, mAnnotPageNum);

                if (!shouldCopyAnnot) {
                    raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum);
                }
            } else if (priorEventMode == PDFViewCtrl.PriorEventMode.PINCH || priorEventMode == PDFViewCtrl.PriorEventMode.DOUBLE_TAP) {
                setCtrlPts();
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    /**
     * The overload implementation of {@link Tool#onDown(MotionEvent)}.
     */
    @Override
    public boolean onDown(MotionEvent e) {
        super.onDown(e);
        mBBoxOnDown.set(mBBox);

        float x = e.getX() + mPdfViewCtrl.getScrollX();
        float y = e.getY() + mPdfViewCtrl.getScrollY();

        // Check if any control point is hit
        mEffCtrlPtId = e_unknown;
        float thresh = mCtrlRadius * 2.25f;
        float shortest_dist = -1;
        for (int i = 0; i < 2; ++i) {
            float s = mCtrlPts[i].x;
            float t = mCtrlPts[i].y;

            float dist = (x - s) * (x - s) + (y - t) * (y - t);
            dist = (float) Math.sqrt(dist);
            if (dist <= thresh && (dist < shortest_dist || shortest_dist < 0)) {
                mEffCtrlPtId = i;
                shortest_dist = dist;
            }

            mCtrlPtsOnDown[i].set(mCtrlPts[i]);
        }

        // Check if hit within the line without hitting any control point.
        if (mEffCtrlPtId < 0) {
            if (pointToLineDistance(x, y)) {
                mEffCtrlPtId = e_moving; // Indicating moving mode;
            }
        }

        // Re-compute the page bounding box on screen, since the zoom
        // factor may have been changed.
        if (mAnnot != null) {
            mPageCropOnClientF = Utils.buildPageBoundBoxOnClient(mPdfViewCtrl, mAnnotPageNum);
        }

        if (mAnnotView != null) {
            mAnnotView.setActiveHandle(mEffCtrlPtId);
        }

        if (mAnnot != null) {
            if (!isInsideAnnot(e) && mEffCtrlPtId < 0) {
                removeAnnotView(true);
                unsetAnnot();
                mNextToolMode = mCurrentDefaultToolMode;
                setCtrlPts();
                // Draw away the edit widget
                mPdfViewCtrl.invalidate((int) Math.floor(mBBox.left), (int) Math.floor(mBBox.top), (int) Math.ceil(mBBox.right), (int) Math.ceil(mBBox.bottom));
            }
        }

        return false;
    }

    @Override
    protected boolean isInsideAnnot(MotionEvent e) {
        int x = (int) (e.getX() + 0.5);
        int y = (int) (e.getY() + 0.5);
        // line is a special case where you can have a large area and not touching the line at all
        // so we do a more precise hit test here
        Annot tempAnnot = ((ToolManager) mPdfViewCtrl.getToolManager()).getAnnotationAt(x, y);
        return (mAnnot != null && mAnnot.equals(tempAnnot));
    }

    private boolean pointToLineDistance(double x, double y) {
        double lineXDist = mCtrlPts[e_end_point].x - mCtrlPts[e_start_point].x;
        double lineYDist = mCtrlPts[e_end_point].y - mCtrlPts[e_start_point].y;

        double squaredDist = (lineXDist * lineXDist) + (lineYDist * lineYDist);

        double distRatio = ((x - mCtrlPts[e_start_point].x) * lineXDist + (y - mCtrlPts[e_start_point].y) * lineYDist) / squaredDist;

        if (distRatio < 0) {
            distRatio = 0;  // This way, we will compare against e_start_point
        }
        if (distRatio > 1) {
            distRatio = 0;  // This way, we will compare against e_end_point
        }

        double dx = mCtrlPts[e_start_point].x - x + distRatio * lineXDist;
        double dy = mCtrlPts[e_start_point].y - y + distRatio * lineYDist;

        double dist = (dx * dx) + (dy * dy);

        return dist < (mCtrlRadius * mCtrlRadius * 4f);
    }

    private void boundCornerCtrlPts(float ox, float oy, boolean translate) {
        if (mPageCropOnClientF != null) {
            if (translate) {

                float left = mBBoxOnDown.left + ox;
                float right = mBBoxOnDown.right + ox;
                float top = mBBoxOnDown.top + oy;
                float bottom = mBBoxOnDown.bottom + oy;
                updateCtrlPts(true, left, right, top, bottom, mBBox);

                float bboxLeft = mBBox.left + mCtrlRadius;
                float bboxTop = mBBox.top + mCtrlRadius;
                float bboxRight = mBBox.right - mCtrlRadius;
                float bboxBottom = mBBox.bottom - mCtrlRadius;

                float max_x = Math.max(mCtrlPts[e_start_point].x, mCtrlPts[e_end_point].x);
                float min_x = Math.min(mCtrlPts[e_start_point].x, mCtrlPts[e_end_point].x);
                float max_y = Math.max(mCtrlPts[e_start_point].y, mCtrlPts[e_end_point].y);
                float min_y = Math.min(mCtrlPts[e_start_point].y, mCtrlPts[e_end_point].y);

                float shift_x = 0, shift_y = 0;
                if (min_x < bboxLeft) {
                    shift_x = bboxLeft - min_x;
                }
                if (min_y < bboxTop) {
                    shift_y = bboxTop - min_y;
                }
                if (max_x > bboxRight) {
                    shift_x = bboxRight - max_x;
                }
                if (max_y > bboxBottom) {
                    shift_y = bboxBottom - max_y;
                }

                mCtrlPts[e_start_point].x += shift_x;
                mCtrlPts[e_start_point].y += shift_y;
                mCtrlPts[e_end_point].x += shift_x;
                mCtrlPts[e_end_point].y += shift_y;
            } else {

                // Bounding along x-axis
                if (mCtrlPts[e_start_point].x > mPageCropOnClientF.right && ox > 0) {
                    mCtrlPts[e_start_point].x = mPageCropOnClientF.right;
                } else if (mCtrlPts[e_start_point].x < mPageCropOnClientF.left && ox < 0) {
                    mCtrlPts[e_start_point].x = mPageCropOnClientF.left;
                } else if (mCtrlPts[e_end_point].x > mPageCropOnClientF.right && ox > 0) {
                    mCtrlPts[e_end_point].x = mPageCropOnClientF.right;
                } else if (mCtrlPts[e_end_point].x < mPageCropOnClientF.left && ox < 0) {
                    mCtrlPts[e_end_point].x = mPageCropOnClientF.left;
                }

                // Bounding along y-axis
                if (mCtrlPts[e_start_point].y < mPageCropOnClientF.top && oy < 0) {
                    mCtrlPts[e_start_point].y = mPageCropOnClientF.top;
                } else if (mCtrlPts[e_start_point].y > mPageCropOnClientF.bottom && oy > 0) {
                    mCtrlPts[e_start_point].y = mPageCropOnClientF.bottom;
                } else if (mCtrlPts[e_end_point].y < mPageCropOnClientF.top && oy < 0) {
                    mCtrlPts[e_end_point].y = mPageCropOnClientF.top;
                } else if (mCtrlPts[e_end_point].y > mPageCropOnClientF.bottom && oy > 0) {
                    mCtrlPts[e_end_point].y = mPageCropOnClientF.bottom;
                }
            }
        }
    }

    /**
     * The overload implementation of {@link Tool#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        if (mScaled) {
            // Scaled and if while moving, disable moving to avoid complications.
            return false;
        }
        if (!mHasSelectionPermission) {
            // does not have permission to modify annotation
            return false;
        }

        if (mEffCtrlPtId != e_unknown) {
            PointF snapPoint = snapToNearestIfEnabled(new PointF(e2.getX(), e2.getY()));

            float sx = mPdfViewCtrl.getScrollX();
            float sy = mPdfViewCtrl.getScrollY();
            mPressedPoint.x = snapPoint.x + sx;
            mPressedPoint.y = snapPoint.y + sy;
            setLoupeInfo(snapPoint.x, snapPoint.y);

            float totalMoveX = snapPoint.x - e1.getX();
            float totalMoveY = snapPoint.y - e1.getY();

            mTempRect.set(mBBox);

            if (mEffCtrlPtId == e_moving) {
                for (int i = 0; i < 2; ++i) {
                    mCtrlPts[i].x = mCtrlPtsOnDown[i].x + totalMoveX;
                    mCtrlPts[i].y = mCtrlPtsOnDown[i].y + totalMoveY;
                }
                boundCornerCtrlPts(totalMoveX, totalMoveY, true);
                if (mAnnotView != null && mAnnotView.getDrawingView() != null) {
                    mAnnotView.getDrawingView().setOffset((int) totalMoveX, (int) totalMoveY);
                }

                applyTranslationSnapping();

                // Compute the bounding box
                mBBox.left = Math.min(mCtrlPts[e_start_point].x, mCtrlPts[e_end_point].x) - mCtrlRadius;
                mBBox.top = Math.min(mCtrlPts[e_start_point].y, mCtrlPts[e_end_point].y) - mCtrlRadius;
                mBBox.right = Math.max(mCtrlPts[e_start_point].x, mCtrlPts[e_end_point].x) + mCtrlRadius;
                mBBox.bottom = Math.max(mCtrlPts[e_start_point].y, mCtrlPts[e_end_point].y) + mCtrlRadius;

                mModifiedAnnot = true;
            } else {
                boolean valid = false;
                switch (mEffCtrlPtId) {
                    case e_start_point: {
                        mCtrlPts[e_start_point].x = mCtrlPtsOnDown[e_start_point].x + totalMoveX;
                        mCtrlPts[e_start_point].y = mCtrlPtsOnDown[e_start_point].y + totalMoveY;
                        valid = true;

                        PointF snappedPoint = applyResizeSnapping(
                                mCtrlPts[e_start_point].x,
                                mCtrlPts[e_start_point].y
                        );
                        if (snappedPoint != null) {
                            mCtrlPts[e_start_point].x += snappedPoint.x - mCtrlPts[e_start_point].x;
                            mCtrlPts[e_start_point].y += snappedPoint.y - mCtrlPts[e_start_point].y;
                        }
                        break;
                    }
                    case e_end_point: {
                        mCtrlPts[e_end_point].x = mCtrlPtsOnDown[e_end_point].x + totalMoveX;
                        mCtrlPts[e_end_point].y = mCtrlPtsOnDown[e_end_point].y + totalMoveY;
                        valid = true;

                        PointF snappedPoint = applyResizeSnapping(
                                mCtrlPts[e_end_point].x,
                                mCtrlPts[e_end_point].y
                        );
                        if (snappedPoint != null) {
                            mCtrlPts[e_end_point].x += snappedPoint.x - mCtrlPts[e_end_point].x;
                            mCtrlPts[e_end_point].y += snappedPoint.y - mCtrlPts[e_end_point].y;
                        }
                        break;
                    }
                }

                mModifiedAnnot = true;

                if (valid) {
                    boundCornerCtrlPts(totalMoveX, totalMoveY, false);

                    // Compute the bounding box
                    mBBox.left = Math.min(mCtrlPts[e_start_point].x, mCtrlPts[e_end_point].x) - mCtrlRadius;
                    mBBox.top = Math.min(mCtrlPts[e_start_point].y, mCtrlPts[e_end_point].y) - mCtrlRadius;
                    mBBox.right = Math.max(mCtrlPts[e_start_point].x, mCtrlPts[e_end_point].x) + mCtrlRadius;
                    mBBox.bottom = Math.max(mCtrlPts[e_start_point].y, mCtrlPts[e_end_point].y) + mCtrlRadius;

                    mModifiedAnnot = true;
                }
            }

            float min_x = Math.min(mTempRect.left, mBBox.left);
            float max_x = Math.max(mTempRect.right, mBBox.right);
            float min_y = Math.min(mTempRect.top, mBBox.top);
            float max_y = Math.max(mTempRect.bottom, mBBox.bottom);
            mPdfViewCtrl.invalidate((int) min_x - 1, (int) min_y - 1, (int) Math.ceil(max_x) + 1, (int) Math.ceil(max_y) + 1);

            updateAnnotView();

            return true;
        }
        return false;
    }

    private void applyTranslationSnapping() {
        float minX = Math.min(mCtrlPts[e_start_point].x, mCtrlPts[e_end_point].x);
        float minY = Math.min(mCtrlPts[e_start_point].y, mCtrlPts[e_end_point].y);
        float maxX = Math.max(mCtrlPts[e_start_point].x, mCtrlPts[e_end_point].x);
        float maxY = Math.max(mCtrlPts[e_start_point].y, mCtrlPts[e_end_point].y);
        RectF snappedRect = applyTranslationSnapping(
                minX,
                minY,
                maxX,
                maxY
        );

        if (snappedRect != null) {
            mCtrlPts[e_start_point].x += snappedRect.left - minX;
            mCtrlPts[e_end_point].y += snappedRect.top - minY;
            mCtrlPts[e_end_point].x += snappedRect.right - maxX;
            mCtrlPts[e_start_point].y += snappedRect.bottom - maxY;
        }
    }

    @Nullable
    protected PointF applyResizeSnapping(float x, float y) {
        RectF rectF = applyResizeSnapping(
                x,
                y,
                x,
                y,
                true,
                true,
                true,
                true
        );
        if (rectF != null) {
            return new PointF(rectF.right, rectF.top);
        } else {
            return null;
        }
    }

    /**
     * Returns which effective control point is closest to the specified coordinate.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @return The effective control point which can be one of this
     * -1 : unspecified
     * 0 : the first point,
     * 1 : the second point,
     * 2 : moving mode
     */
    public int getEffectCtrlPointId(float x, float y) {
        int effCtrlPtId = -1;
        float thresh = mCtrlRadius * 2.25f;
        float shortest_dist = -1;
        for (int i = 0; i < 2; ++i) {
            float s = mCtrlPts[i].x;
            float t = mCtrlPts[i].y;

            float dist = (x - s) * (x - s) + (y - t) * (y - t);
            dist = (float) Math.sqrt(dist);
            if (dist <= thresh && (dist < shortest_dist || shortest_dist < 0)) {
                effCtrlPtId = i;
                shortest_dist = dist;
            }
        }

        // Check if hit within the line without hitting any control point.
        if (effCtrlPtId < 0 && pointToLineDistance(x, y)) {
            effCtrlPtId = 2; // Indicating moving mode;
        }
        return effCtrlPtId;
    }
}
