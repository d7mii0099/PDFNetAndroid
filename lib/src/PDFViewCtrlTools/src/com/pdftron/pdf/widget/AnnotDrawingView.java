package com.pdftron.pdf.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.CurvePainter;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Ink;
import com.pdftron.pdf.config.ToolConfig;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.LineEndingStyle;
import com.pdftron.pdf.model.LineStyle;
import com.pdftron.pdf.model.RulerItem;
import com.pdftron.pdf.model.ShapeBorderStyle;
import com.pdftron.pdf.model.ink.InkItem;
import com.pdftron.pdf.model.ink.PressureInkItem;
import com.pdftron.pdf.tools.CloudCreate;
import com.pdftron.pdf.tools.FreeHighlighterCreate;
import com.pdftron.pdf.tools.RulerCreate;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.DrawingUtils;
import com.pdftron.pdf.utils.PressureInkUtils;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.UUID;

import static com.pdftron.pdf.tools.AnnotEdit.e_lr;
import static com.pdftron.pdf.tools.AnnotEdit.e_ul;
import static com.pdftron.pdf.tools.FreehandCreate.createStrokeListFromArrayObj;

public class AnnotDrawingView extends AppCompatImageView {

    private AnnotViewImpl mAnnotViewImpl;
    private RotationImpl mRotationImpl;

    private RectF mOval = new RectF();
    private int mPageNum;

    // Re-usable points for drawing
    private final PointF mPt3 = new PointF(0, 0);
    private final PointF mPt4 = new PointF(0, 0);
    private final PointF mPt5 = new PointF(0, 0);
    private final PointF mPt6 = new PointF(0, 0);
    private final PointF mPt7 = new PointF(0, 0);
    private final PointF mPt8 = new PointF(0, 0);
    private final PointF mPt9 = new PointF(0, 0);
    private final PointF mPt10 = new PointF(0, 0);
    private final PointF mPt11 = new PointF(0, 0);
    private final PointF mPt12 = new PointF(0, 0);
    private final PointF mPt13 = new PointF(0, 0);
    private final PointF mPt14 = new PointF(0, 0);

    private int mXOffset;
    private int mYOffset;

    private Path mOnDrawPath = new Path();

    private RectF mOffsetRect = new RectF();

    // used for onDraw so we don't need to allocate new object in onDraw
    private RectF mTempRect = new RectF();

    private String mIcon;
    @Nullable
    private Drawable mIconDrawable;

    @NonNull
    private ArrayList<InkItem> mInks = new ArrayList<>();
    private PointF mInkOffset = new PointF();
    private float mInitialWidthScreen;
    private float mInitialHeightScreen;
    private float mScaleWidthScreen;
    private float mScaleHeightScreen;

    private boolean mInitRectSet;

    @NonNull
    private final RectF mDownRect = new RectF();
    private RectF mInkDownRect;
    private Matrix mInkTransform = new Matrix();

    private DashPathEffect mDashPathEffect;

    private boolean mCanDraw;
    private Bitmap mAnnotBitmap;

    public AnnotDrawingView(Context context) {
        this(context, null);
    }

    public AnnotDrawingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnnotDrawingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    private void init(Context context) {
        mAnnotViewImpl = new AnnotViewImpl(context);
        mDashPathEffect = DrawingUtils.getDashPathEffect(context);
    }

    public void setCanDraw(boolean canDraw) {
        mCanDraw = canDraw;
    }

    public boolean getCanDraw() {
        return mCanDraw;
    }

    public void setAnnotStyle(AnnotViewImpl annotViewImpl) {
        mAnnotViewImpl = annotViewImpl;
        updateIcon(mAnnotViewImpl.mAnnotStyle.getIcon());
    }

    public void setAnnotBitmap(Bitmap bitmap) {
        mAnnotBitmap = bitmap;
        // when bitmap is updated, we will always have the latest bounding box
        mDownRect.set(mAnnotViewImpl.mAnnotRectF);
        invalidate();
    }

    public void setRotateImpl(RotationImpl rotateImpl) {
        mRotationImpl = rotateImpl;
    }

    public void setCurvePainter(CurvePainter curvePainter) {
        if (curvePainter == null) {
            return;
        }
        if (mAnnotViewImpl.mCurvePainter != null && mRotationImpl != null && mRotationImpl.mRotated) {
            // don't update the appearance if rotated
            return;
        }
        mAnnotViewImpl.mCurvePainter = curvePainter;
        if (curvePainter.getRect() != null) {
            mInitialWidthScreen = mScaleWidthScreen = curvePainter.getRect().width();
            mInitialHeightScreen = mScaleHeightScreen = curvePainter.getRect().height();
        }
        invalidate();
    }

    private boolean isSizeOfAnnot() {
        // whether the annot drawing view is size of the annot bbox
        return ToolConfig.getInstance().getAnnotationHandlerToolMode(mAnnotViewImpl.mAnnotStyle.getAnnotType()) == ToolManager.ToolMode.ANNOT_EDIT ||
                mAnnotViewImpl.mAnnotStyle.getAnnotType() == Annot.e_Link ||
                mAnnotViewImpl.mAnnotStyle.getAnnotType() == Annot.e_Widget;
    }

    /**
     * A read lock is expected around this method
     */
    public void initInkItem(Annot inkAnnot, int pageNum, PointF offset) {
        if (mAnnotViewImpl.mAnnotStyle.getAnnotType() == Annot.e_Ink ||
                mAnnotViewImpl.isFreeHighlighter()) {
            try {
                if (mInks.isEmpty()) {
                    Ink ink = new Ink(inkAnnot);
                    com.pdftron.pdf.Rect rect = inkAnnot.getRect();
                    rect.normalize();

                    InkItem item;
                    if (PressureInkUtils.isPressureSensitive(ink)) {
                        item = new PressureInkItem(
                                UUID.randomUUID().toString(),
                                null,
                                null,
                                createStrokeListFromArrayObj(ink.getSDFObj().findObj(AnnotUtils.KEY_INK_LIST)),
                                PressureInkUtils.getThicknessList(ink),
                                pageNum,
                                mAnnotViewImpl.mStrokeColor,
                                mAnnotViewImpl.mOpacity,
                                mAnnotViewImpl.mThickness,
                                (float) mAnnotViewImpl.mPdfViewCtrl.getZoom() * mAnnotViewImpl.mThickness,
                                false
                        );
                    } else {
                        item = new InkItem(
                                UUID.randomUUID().toString(),
                                null,
                                createStrokeListFromArrayObj(ink.getSDFObj().findObj(AnnotUtils.KEY_INK_LIST)),
                                pageNum,
                                mAnnotViewImpl.mStrokeColor,
                                mAnnotViewImpl.mOpacity,
                                mAnnotViewImpl.mThickness,
                                (float) mAnnotViewImpl.mPdfViewCtrl.getZoom() * mAnnotViewImpl.mThickness,
                                false
                        );
                    }
                    item.getPaint(mAnnotViewImpl.mPdfViewCtrl).setColor(Utils.getPostProcessedColor(mAnnotViewImpl.mPdfViewCtrl, mAnnotViewImpl.mStrokeColor));
                    if (mAnnotViewImpl.isFreeHighlighter()) {
                        item.getPaint(mAnnotViewImpl.mPdfViewCtrl).setAlpha((int) (255 * mAnnotViewImpl.mOpacity * FreeHighlighterCreate.BLEND_OPACITY));
                    }
                    mInks.add(item);
                    mInkOffset.set(offset);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void updateColor(int color) {
        mAnnotViewImpl.updateColor(color);
        if (!Utils.isNullOrEmpty(mIcon)) {
            updateIcon(mIcon);
        }
        if (!mInks.isEmpty()) {
            ArrayList<InkItem> newInkItems = new ArrayList<>();
            for (InkItem inkItem : mInks) {
                InkItem newInkItem;
                if (inkItem instanceof PressureInkItem) {
                    newInkItem = new PressureInkItem(
                            inkItem.id,
                            inkItem.currentActiveStroke,
                            ((PressureInkItem) inkItem).currentActivePressure,
                            inkItem.finishedStrokes,
                            ((PressureInkItem) inkItem).finishedPressures,
                            inkItem.pageNumber,
                            color,
                            ((float) mAnnotViewImpl.mPaint.getAlpha()) / 255.0f,
                            inkItem.baseThickness,
                            mAnnotViewImpl.mPaint.getStrokeWidth(),
                            inkItem.isStylus
                    );
                } else {
                    newInkItem = new InkItem(
                            inkItem.id,
                            inkItem.currentActiveStroke,
                            inkItem.finishedStrokes,
                            inkItem.pageNumber,
                            color,
                            ((float) mAnnotViewImpl.mPaint.getAlpha()) / 255.0f,
                            inkItem.baseThickness,
                            mAnnotViewImpl.mPaint.getStrokeWidth(),
                            inkItem.isStylus
                    );
                }
                newInkItems.add(newInkItem);
            }
            mInks = newInkItems;
        }
        invalidate();
    }

    public void updateFillColor(int color) {
        mAnnotViewImpl.updateFillColor(color);
        invalidate();
    }

    public void updateThickness(float thickness) {
        mAnnotViewImpl.updateThickness(thickness);
        if (!mInks.isEmpty()) {
            ArrayList<InkItem> newInkItems = new ArrayList<>();
            for (InkItem inkItem : mInks) {
                InkItem newInkItem;
                if (inkItem instanceof PressureInkItem) {
                    newInkItem = new PressureInkItem(
                            inkItem.id,
                            inkItem.currentActiveStroke,
                            ((PressureInkItem) inkItem).currentActivePressure,
                            inkItem.finishedStrokes,
                            ((PressureInkItem) inkItem).finishedPressures,
                            inkItem.pageNumber,
                            mAnnotViewImpl.mPaint.getColor(),
                            ((float) mAnnotViewImpl.mPaint.getAlpha()) / 255.0f,
                            thickness,
                            (float) (thickness * mAnnotViewImpl.mPdfViewCtrl.getZoom()),
                            inkItem.isStylus
                    );
                } else {
                    newInkItem = new InkItem(
                            inkItem.id,
                            inkItem.currentActiveStroke,
                            inkItem.finishedStrokes,
                            inkItem.pageNumber,
                            mAnnotViewImpl.mPaint.getColor(),
                            ((float) mAnnotViewImpl.mPaint.getAlpha()) / 255.0f,
                            thickness,
                            (float) (thickness * mAnnotViewImpl.mPdfViewCtrl.getZoom()),
                            inkItem.isStylus
                    );
                }
                newInkItems.add(newInkItem);
            }
            mInks = newInkItems;
        }
        invalidate();
    }

    public void updateOpacity(float opacity) {
        mAnnotViewImpl.updateOpacity(opacity);
        if (!Utils.isNullOrEmpty(mIcon)) {
            updateIcon(mIcon);
        }
        if (!mInks.isEmpty()) {
            ArrayList<InkItem> newInkItems = new ArrayList<>();
            for (InkItem inkItem : mInks) {

                InkItem newInkItem;
                if (inkItem instanceof PressureInkItem) {
                    newInkItem = new PressureInkItem(
                            inkItem.id,
                            inkItem.currentActiveStroke,
                            ((PressureInkItem) inkItem).currentActivePressure,
                            inkItem.finishedStrokes,
                            ((PressureInkItem) inkItem).finishedPressures,
                            inkItem.pageNumber,
                            mAnnotViewImpl.mPaint.getColor(),
                            opacity,
                            inkItem.baseThickness,
                            mAnnotViewImpl.mPaint.getStrokeWidth(),
                            inkItem.isStylus
                    );
                } else {
                    newInkItem = new InkItem(
                            inkItem.id,
                            inkItem.currentActiveStroke,
                            inkItem.finishedStrokes,
                            inkItem.pageNumber,
                            mAnnotViewImpl.mPaint.getColor(),
                            opacity,
                            inkItem.baseThickness,
                            mAnnotViewImpl.mPaint.getStrokeWidth(),
                            inkItem.isStylus
                    );
                }
                newInkItems.add(newInkItem);
            }
            mInks = newInkItems;
        }
        invalidate();
    }

    public void updateRulerItem(RulerItem rulerItem) {
        mAnnotViewImpl.updateRulerItem(rulerItem);
        invalidate();
    }

    public void updateBorderStyle(ShapeBorderStyle borderStyle) {
        mAnnotViewImpl.updateBorderStyle(borderStyle);
        invalidate();
    }

    public void updateLineStyle(LineStyle lineStyle) {
        mAnnotViewImpl.updateLineStyle(lineStyle);
        invalidate();
    }

    public void updateLineStartStyle(LineEndingStyle lineStartStyle) {
        mAnnotViewImpl.updateLineStartStyle(lineStartStyle);
        invalidate();
    }

    public void updateLineEndStyle(LineEndingStyle lineEndStyle) {
        mAnnotViewImpl.updateLineEndStyle(lineEndStyle);
        invalidate();
    }

    public void setZoom(double zoom) {
        mAnnotViewImpl.setZoom(zoom);
    }

    public void setAnnotRect(@Nullable RectF rect) {
        if (null == rect) {
            return;
        }
        RectF inflatedRect = null;
        try {
            // use core's logic to inflate the bbox to match core drawing
            double borderWidth = mAnnotViewImpl.mAnnotStyle.getThickness() * mAnnotViewImpl.mZoom;
            Rect pRect = new Rect(rect.left, rect.top, rect.right, rect.bottom);
            pRect.normalize();
            if (pRect.getWidth() > borderWidth && pRect.getHeight() > borderWidth) {
                pRect.inflate(-borderWidth / 2);
            }
            inflatedRect = new RectF((float) pRect.getX1(), (float) pRect.getY1(), (float) pRect.getX2(), (float) pRect.getY2());
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }

        if (!mInitRectSet) {
            mInitialWidthScreen = rect.width();
            mInitialHeightScreen = rect.height();
            mScaleWidthScreen = mInitialWidthScreen;
            mScaleHeightScreen = mInitialHeightScreen;
            mDownRect.set(rect);
            if (inflatedRect != null) {
                mInkDownRect = new RectF(inflatedRect);
            }

            mInitRectSet = true;
        }
        mAnnotViewImpl.mPt1.set(rect.left, rect.top);
        mAnnotViewImpl.mPt2.set(rect.right, rect.bottom);

        mScaleWidthScreen = rect.width();
        mScaleHeightScreen = rect.height();

        mAnnotViewImpl.mAnnotRectF.set(rect);
        rect.round(mAnnotViewImpl.mAnnotRect);

        if (mInkDownRect != null && inflatedRect != null) {
            mInkTransform.setRectToRect(mInkDownRect, inflatedRect, Matrix.ScaleToFit.FILL);
        }
    }

    public void setOffset(int x, int y) {
        mXOffset = x;
        mYOffset = y;
        invalidate();
    }

    public void setPageNum(int pageNum) {
        mPageNum = pageNum;
    }

    public void updateIcon(String icon) {
        mIcon = icon;

        mIconDrawable = AnnotStyle.getIconDrawable(getContext(), mIcon, mAnnotViewImpl.mStrokeColor, mAnnotViewImpl.mOpacity);
    }

    public boolean hasIcon() {
        if (mAnnotViewImpl.mAnnotStyle.getAnnotType() == Annot.e_Text) {
            return mIconDrawable != null;
        }
        return true;
    }

    private void drawSelectionBox(Canvas canvas) {
        if (!mAnnotViewImpl.mCanDrawCtrlPts) {
            return;
        }
        if (mAnnotViewImpl.isAnnotEditLine() || mAnnotViewImpl.isAnnotEditAdvancedShape()) {
            return;
        }
        if (!mAnnotViewImpl.mHasSelectionPermission) {
            return;
        }
        float left = mAnnotViewImpl.mCtrlPts[e_ul].x;
        float top = mAnnotViewImpl.mCtrlPts[e_ul].y;
        float right = mAnnotViewImpl.mCtrlPts[e_lr].x;
        float bottom = mAnnotViewImpl.mCtrlPts[e_lr].y;
        DrawingUtils.drawSelectionBox(mAnnotViewImpl.mCtrlPtsPaint, getContext(),
                canvas, left, top, right, bottom, mAnnotViewImpl.mHasSelectionPermission);
    }

    private boolean canUseCoreRender() {
        // for stamp, we can generate both, prefer core version
        return mAnnotViewImpl.mAnnotStyle.hasAppearance() || mAnnotViewImpl.isStamp();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        try {
            canvas.save();
            PointF centerPt = mRotationImpl != null ? mRotationImpl.center() : null;
            if (centerPt != null) {
                float degree = mRotationImpl.mRotating ? mRotationImpl.mRotDegreeSave + mRotationImpl.mRotDegree : mRotationImpl.mRotDegreeSave;
                canvas.rotate(degree, centerPt.x, centerPt.y);
            }
            if (mAnnotViewImpl.mCurvePainter != null && canUseCoreRender() && mCanDraw) {
                if (isSizeOfAnnot()) {
                    if (mAnnotViewImpl.mCurvePainter.getBitmap() != null) {
                        Paint paint = mAnnotViewImpl.mBmpPaint;
                        if (mAnnotViewImpl.isFreeHighlighter() && !mAnnotViewImpl.isNightMode()) {
                            paint = mAnnotViewImpl.mBmpMultBlendPaint;
                        }
                        mOffsetRect.left = mAnnotViewImpl.mCurvePainter.getRect().left + mAnnotViewImpl.mAnnotRectF.left;
                        mOffsetRect.right = mOffsetRect.left + mAnnotViewImpl.mCurvePainter.getRect().width();
                        mOffsetRect.top = mAnnotViewImpl.mCurvePainter.getRect().top + mAnnotViewImpl.mAnnotRectF.top;
                        mOffsetRect.bottom = mOffsetRect.top + mAnnotViewImpl.mCurvePainter.getRect().height();
                        canvas.drawBitmap(mAnnotViewImpl.mCurvePainter.getBitmap(), null, mOffsetRect, paint);
                    } else {
                        mAnnotViewImpl.mCurvePainter.draw(canvas, mAnnotViewImpl.mAnnotRectF.left, mAnnotViewImpl.mAnnotRectF.top,
                                mScaleWidthScreen / mInitialWidthScreen * mAnnotViewImpl.mZoom,
                                mScaleHeightScreen / mInitialHeightScreen * mAnnotViewImpl.mZoom,
                                mAnnotViewImpl.mZoom, mAnnotViewImpl.mZoom);
                    }
                } else {
                    RectF rect = mAnnotViewImpl.mAnnotRectF;
                    if (mAnnotViewImpl.mCurvePainter.getBitmap() != null) {
                        // draw bitmap into annot rect
                        canvas.drawBitmap(mAnnotViewImpl.mCurvePainter.getBitmap(), rect.left + mXOffset, rect.top + mYOffset, mAnnotViewImpl.mBmpPaint);
                    } else {
                        mAnnotViewImpl.mCurvePainter.draw(canvas, rect.left + mXOffset, rect.top + mYOffset,
                                mAnnotViewImpl.mZoom, mAnnotViewImpl.mZoom,
                                mAnnotViewImpl.mZoom, mAnnotViewImpl.mZoom);
                    }
                }
            } else if (mCanDraw) {
                if (mAnnotViewImpl.mAnnotStyle.getAnnotType() == Annot.e_Square
                        && (mAnnotViewImpl.mAnnotStyle.getBorderStyle() == ShapeBorderStyle.CLOUDY)) {
                    DrawingUtils.drawCloudyRectangle(mAnnotViewImpl.mPdfViewCtrl,
                            mPageNum, canvas, mOnDrawPath,
                            mAnnotViewImpl.mPt1, mAnnotViewImpl.mPt2,
                            mAnnotViewImpl.mFillColor, mAnnotViewImpl.mStrokeColor,
                            mAnnotViewImpl.mFillPaint, mAnnotViewImpl.mPaint,
                            mAnnotViewImpl.mAnnotStyle.getBorderEffectIntensity());
                } else if (mAnnotViewImpl.mAnnotStyle.getAnnotType() == Annot.e_Square) {
                    DrawingUtils.drawRectangle(canvas,
                            mAnnotViewImpl.mPt1, mAnnotViewImpl.mPt2,
                            mAnnotViewImpl.mThicknessDraw,
                            mAnnotViewImpl.mFillColor, mAnnotViewImpl.mStrokeColor,
                            mAnnotViewImpl.mFillPaint, mAnnotViewImpl.mPaint,
                            (mAnnotViewImpl.mAnnotStyle.getBorderStyle() == ShapeBorderStyle.DASHED ? mDashPathEffect : null));
                } else if (mAnnotViewImpl.mAnnotStyle.getAnnotType() == Annot.e_Circle) {
                    DrawingUtils.drawOval(canvas,
                            mAnnotViewImpl.mPt1, mAnnotViewImpl.mPt2,
                            mAnnotViewImpl.mThicknessDraw,
                            mOval,
                            mAnnotViewImpl.mFillColor, mAnnotViewImpl.mStrokeColor,
                            mAnnotViewImpl.mFillPaint, mAnnotViewImpl.mPaint,
                            (mAnnotViewImpl.mAnnotStyle.getBorderStyle() == ShapeBorderStyle.DASHED ? mDashPathEffect : null));
                } else if (mAnnotViewImpl.mAnnotStyle.getAnnotType() == Annot.e_Line ||
                        mAnnotViewImpl.mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW) {
                    DrawingUtils.drawLine(canvas, mAnnotViewImpl.mVertices.get(0),
                            mAnnotViewImpl.mVertices.get(1), mPt3, mPt4, mPt5, mPt6, mPt7, mPt8, mPt9, mPt10, mPt11, mPt12,
                            mAnnotViewImpl.mAnnotStyle.getLineStartStyle(), mAnnotViewImpl.mAnnotStyle.getLineEndStyle(),
                            mOnDrawPath, mAnnotViewImpl.mPaint,
                            (mAnnotViewImpl.mAnnotStyle.getLineStyle() == LineStyle.DASHED ? mDashPathEffect : null),
                            mAnnotViewImpl.mThickness, mAnnotViewImpl.mZoom);
                } else if (mAnnotViewImpl.mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_RULER) {
                    // calc distance
                    double[] pts1, pts2;
                    pts1 = mAnnotViewImpl.mPdfViewCtrl.convScreenPtToPagePt(mAnnotViewImpl.mVertices.get(0).x, mAnnotViewImpl.mVertices.get(0).y, mPageNum);
                    pts2 = mAnnotViewImpl.mPdfViewCtrl.convScreenPtToPagePt(mAnnotViewImpl.mVertices.get(1).x, mAnnotViewImpl.mVertices.get(1).y, mPageNum);
                    String text = RulerCreate.getLabel(mAnnotViewImpl.mAnnotStyle.getRulerItem(), pts1[0], pts1[1], pts2[0], pts2[1]);
                    DrawingUtils.drawRuler(canvas, mAnnotViewImpl.mVertices.get(0),
                            mAnnotViewImpl.mVertices.get(1), mPt3, mPt4, mPt5, mPt6, mPt7, mPt8, mPt9, mPt10, mPt11, mPt12,
                            mAnnotViewImpl.mAnnotStyle.getLineStartStyle(), mAnnotViewImpl.mAnnotStyle.getLineEndStyle(),
                            text,
                            mOnDrawPath, mAnnotViewImpl.mPaint,
                            (mAnnotViewImpl.mAnnotStyle.getLineStyle() == LineStyle.DASHED ? mDashPathEffect : null),
                            mAnnotViewImpl.mThickness, mAnnotViewImpl.mZoom);
                } else if (mAnnotViewImpl.mAnnotStyle.getAnnotType() == Annot.e_Polyline ||
                        mAnnotViewImpl.mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_PERIMETER_MEASURE) {
                    DrawingUtils.drawPolyline(mAnnotViewImpl.mPdfViewCtrl, mPageNum,
                            canvas, mAnnotViewImpl.mVertices, mPt3, mPt4, mPt5, mPt6, mPt7, mPt8, mPt9, mPt10, mPt11, mPt12, mPt13, mPt14,
                            mAnnotViewImpl.mAnnotStyle.getLineStartStyle(), mAnnotViewImpl.mAnnotStyle.getLineEndStyle(),
                            mOnDrawPath, mAnnotViewImpl.mPaint, mAnnotViewImpl.mStrokeColor,
                            (mAnnotViewImpl.mAnnotStyle.getLineStyle() == LineStyle.DASHED ? mDashPathEffect : null),
                            mAnnotViewImpl.mThickness, mAnnotViewImpl.mZoom);
                } else if ((mAnnotViewImpl.mAnnotStyle.getAnnotType() == Annot.e_Polygon ||
                        mAnnotViewImpl.mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_AREA_MEASURE)
                        && (mAnnotViewImpl.mAnnotStyle.getBorderStyle() == ShapeBorderStyle.CLOUDY)) {
                    DrawingUtils.drawCloudyPolygon(mAnnotViewImpl.mPdfViewCtrl, mPageNum,
                            canvas, mAnnotViewImpl.mVertices, mOnDrawPath, mAnnotViewImpl.mPaint, mAnnotViewImpl.mStrokeColor,
                            mAnnotViewImpl.mFillPaint, mAnnotViewImpl.mFillColor, CloudCreate.BORDER_INTENSITY);
                } else if ((mAnnotViewImpl.mAnnotStyle.getAnnotType() == Annot.e_Polygon ||
                        mAnnotViewImpl.mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_AREA_MEASURE)) {
                    DrawingUtils.drawPolygon(mAnnotViewImpl.mPdfViewCtrl, mPageNum,
                            canvas, mAnnotViewImpl.mVertices, mOnDrawPath, mAnnotViewImpl.mPaint, mAnnotViewImpl.mStrokeColor,
                            mAnnotViewImpl.mFillPaint, mAnnotViewImpl.mFillColor,
                            (mAnnotViewImpl.mAnnotStyle.getBorderStyle() == ShapeBorderStyle.DASHED ? mDashPathEffect : null));
                } else if (mAnnotViewImpl.mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_RECT_AREA_MEASURE) {
                    DrawingUtils.drawPolygon(mAnnotViewImpl.mPdfViewCtrl, mPageNum,
                            canvas, mAnnotViewImpl.mVertices, mOnDrawPath, mAnnotViewImpl.mPaint, mAnnotViewImpl.mStrokeColor,
                            mAnnotViewImpl.mFillPaint, mAnnotViewImpl.mFillColor, mInkTransform,
                            (mAnnotViewImpl.mAnnotStyle.getBorderStyle() == ShapeBorderStyle.DASHED ? mDashPathEffect : null));
                } else if (mAnnotViewImpl.mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD) {
                    DrawingUtils.drawCloud(mAnnotViewImpl.mPdfViewCtrl, mPageNum, canvas,
                            mAnnotViewImpl.mVertices, mOnDrawPath, mAnnotViewImpl.mPaint, mAnnotViewImpl.mStrokeColor,
                            mAnnotViewImpl.mFillPaint, mAnnotViewImpl.mFillColor, mAnnotViewImpl.mAnnotStyle.getBorderEffectIntensity());
                } else if (mAnnotViewImpl.mAnnotStyle.getAnnotType() == Annot.e_Ink ||
                        mAnnotViewImpl.mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_HIGHLIGHTER) {
                    DrawingUtils.drawInk(mAnnotViewImpl.mPdfViewCtrl, canvas, mInks,
                            mInkTransform, mInkOffset);
                } else if (mAnnotViewImpl.mAnnotStyle.getAnnotType() == Annot.e_Text || mAnnotViewImpl.mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT && mIconDrawable != null && mAnnotBitmap == null) {
                    mIconDrawable.setBounds(mAnnotViewImpl.mAnnotRect);
                    mIconDrawable.draw(canvas);
                } else if (mAnnotBitmap != null) {
                    // for free text let's not stretch the bitmap
                    if (mAnnotViewImpl.mAnnotStyle.getAnnotType() == Annot.e_FreeText ||
                            mAnnotViewImpl.mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_DATE) {
                        mTempRect.set(mAnnotViewImpl.mAnnotRectF.left,
                                mAnnotViewImpl.mAnnotRectF.top,
                                mAnnotViewImpl.mAnnotRectF.left + mDownRect.width(),
                                mAnnotViewImpl.mAnnotRectF.top + mDownRect.height());
                        canvas.drawBitmap(mAnnotBitmap, null,
                                mTempRect,
                                mAnnotViewImpl.mBmpPaint);
                    } else {
                        canvas.drawBitmap(mAnnotBitmap, null, mAnnotViewImpl.mAnnotRectF, mAnnotViewImpl.mBmpPaint);
                    }
                }
            }

            // draw selection box
            if (mRotationImpl == null || !mRotationImpl.mRotated) {
                drawSelectionBox(canvas);
            }

            canvas.restore();
            // draw rotation guideline
            if (mRotationImpl != null && mRotationImpl.mSnapDegree != null) {
                DrawingUtils.drawGuideline(mRotationImpl.mSnapDegree, mAnnotViewImpl.mRotateCenterRadius,
                        canvas, mAnnotViewImpl.mBBox, mAnnotViewImpl.mGuidelinePath, mAnnotViewImpl.mGuidelinePaint);
            }
            // draw snap guideline
            if (mAnnotViewImpl.mSnapMode != null) {
                DrawingUtils.drawGuideline(mAnnotViewImpl.mSnapMode,
                        mAnnotViewImpl.mGuidelinExtend, canvas, mAnnotViewImpl.mBBox, mAnnotViewImpl.mGuidelinePath, mAnnotViewImpl.mGuidelinePaint);
            }
            // draw annotation position snapping guideline
            if (mAnnotViewImpl.mAnnotPositionSnappingGuidelines != null && !mAnnotViewImpl.mAnnotPositionSnappingGuidelines.isEmpty()) {
                for (Pair<Point, Point> annotPositionSnappingGuideline : mAnnotViewImpl.mAnnotPositionSnappingGuidelines) {
                    DrawingUtils.drawGuideline(
                            canvas,
                            mAnnotViewImpl.mGuidelinePath,
                            annotPositionSnappingGuideline.first.x - mAnnotViewImpl.mPdfViewCtrl.getScrollX(),
                            annotPositionSnappingGuideline.first.y - mAnnotViewImpl.mPdfViewCtrl.getScrollY(),
                            annotPositionSnappingGuideline.second.x - mAnnotViewImpl.mPdfViewCtrl.getScrollX(),
                            annotPositionSnappingGuideline.second.y - mAnnotViewImpl.mPdfViewCtrl.getScrollY(),
                            mAnnotViewImpl.mGuidelinePaint
                    );
                }
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
    }
}
