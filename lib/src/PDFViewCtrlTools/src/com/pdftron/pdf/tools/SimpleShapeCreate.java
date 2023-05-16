//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Line;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.LineEndingStyle;
import com.pdftron.pdf.model.LineStyle;
import com.pdftron.pdf.model.ShapeBorderStyle;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.DrawingUtils;
import com.pdftron.pdf.utils.Utils;

/**
 * This class is the base class for several shape creation classes,
 * e.g., LineCreate, OvalCreate, etc.
 */
@Keep
public abstract class SimpleShapeCreate extends BaseTool {

    public static final int sTapToCreateHalfWidth = 50;

    private static final String TAG = SimpleShapeCreate.class.getName();
    protected PointF mPt1, mPt2;    // Touch-down point and moving point
    protected Paint mPaint;
    protected Paint mFillPaint;
    protected int mDownPageNum;
    protected RectF mPageCropOnClientF;
    protected float mThickness;
    protected float mThicknessDraw;
    protected int mStrokeColor;
    protected int mFillColor;
    protected float mOpacity;
    protected boolean mIsAllPointsOutsidePage;
    protected boolean mHasFill;
    protected boolean mHasBorderStyle;
    protected boolean mHasLineStyle;
    protected boolean mHasLineStartStyle;
    protected boolean mHasLineEndStyle;
    protected double mZoom;
    protected ShapeBorderStyle mBorderStyle;
    protected LineStyle mLineStyle;
    protected LineEndingStyle mLineStartStyle;
    protected LineEndingStyle mLineEndStyle;
    protected boolean mPageBoundaryRestricted = true;
    protected DashPathEffect mDashPathEffect = null;

    protected boolean mSkipAfterQuickMenuClose;
    protected int mTapToCreateShapeHalfSize;

    protected boolean mOnUpCalled; // work around issue where onUp is called twice

    /**
     * Class constructor
     */
    public SimpleShapeCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        mZoom = ctrl.getZoom();
        mPt1 = new PointF(0, 0);
        mPt2 = new PointF(0, 0);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.BLUE);
        mPaint.setStyle(Paint.Style.STROKE);
        mFillPaint = new Paint(mPaint);
        mFillPaint.setStyle(Paint.Style.FILL);
        mFillPaint.setColor(Color.TRANSPARENT);
        mThickness = 1.0f;
        mThicknessDraw = 1.0f;
        mHasFill = false;

        mTapToCreateShapeHalfSize = (int) Utils.convDp2Pix(mPdfViewCtrl.getContext(), ((ToolManager) mPdfViewCtrl.getToolManager()).getTapToCreateShapeHalfWidth());
        mDashPathEffect = DrawingUtils.getDashPathEffect(mPdfViewCtrl.getContext());
    }

    /**
     * The overload interface of {@link Tool#getToolMode()} ()}.
     */
    @Override
    abstract public ToolManager.ToolModeBase getToolMode();

    @Override
    abstract public int getCreateAnnotType();

    @Override
    public void setupAnnotProperty(AnnotStyle annotStyle) {
        super.setupAnnotProperty(annotStyle);

        mStrokeColor = annotStyle.getColor();
        mFillColor = annotStyle.getFillColor();
        mOpacity = annotStyle.getOpacity();
        mThickness = annotStyle.getThickness();
        mHasBorderStyle = annotStyle.hasBorderStyle();
        mHasLineStyle = annotStyle.hasLineStyle();
        mHasLineStartStyle = annotStyle.hasLineStartStyle();
        mHasLineEndStyle = annotStyle.hasLineEndStyle();
        mBorderStyle = annotStyle.getBorderStyle();
        mLineStyle = annotStyle.getLineStyle();
        mLineStartStyle = annotStyle.getLineStartStyle();
        mLineEndStyle = annotStyle.getLineEndStyle();

        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(getColorKey(getCreateAnnotType()), mStrokeColor);
        editor.putInt(getColorFillKey(getCreateAnnotType()), mFillColor);
        editor.putFloat(getOpacityKey(getCreateAnnotType()), mOpacity);
        editor.putFloat(getThicknessKey(getCreateAnnotType()), mThickness);
        if (mBorderStyle != null) {
            editor.putString(getBorderStyleKey(getCreateAnnotType()), mBorderStyle.name());
        }
        if (mLineStyle != null) {
            editor.putString(getLineStyleKey(getCreateAnnotType()), mLineStyle.name());
        }
        if (mLineStartStyle != null) {
            editor.putString(getLineStartStyleKey(getCreateAnnotType()), mLineStartStyle.name());
        }
        if (mLineEndStyle != null) {
            editor.putString(getLineEndStyleKey(getCreateAnnotType()), mLineEndStyle.name());
        }
        editor.apply();
    }

    /**
     * The overload implementation of {@link Tool#isCreatingAnnotation()}.
     */
    @Override
    public boolean isCreatingAnnotation() {
        return true;
    }

    /**
     * The overload implementation of {@link Tool#onConfigurationChanged(Configuration)}.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // reset points as screen location is no longer valid after rotation
        resetPts();
        mPdfViewCtrl.invalidate();
    }

    /**
     * The overload implementation of {@link Tool#onSingleTapConfirmed(MotionEvent)}.
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        ToolManager.ToolModeBase toolMode = ToolManager.getDefaultToolModeBase(getToolMode());
        if (tapToSelectAllowed()) {
            int x = (int) (e.getX() + 0.5);
            int y = (int) (e.getY() + 0.5);

            Annot tempAnnot = ((ToolManager) mPdfViewCtrl.getToolManager()).getAnnotationAt(x, y);
            int page = mPdfViewCtrl.getPageNumberFromScreenPt(x, y);

            setCurrentDefaultToolModeHelper(toolMode);
            try {
                if (isAnnotSupportEdit(tempAnnot)) {
                    ((ToolManager) mPdfViewCtrl.getToolManager()).selectAnnot(tempAnnot, page);
                } else {
                    mNextToolMode = getToolMode();
                }
            } catch (PDFNetException ignored) {
            }

            return false;
        }
        return super.onSingleTapConfirmed(e);
    }

    protected boolean tapToSelectAllowed() {
        ToolMode toolMode = ToolManager.getDefaultToolMode(getToolMode());
        return mForceSameNextToolMode &&
                toolMode != ToolMode.INK_CREATE &&
                toolMode != ToolMode.SMART_PEN_INK &&
                toolMode != ToolMode.INK_ERASER &&
                toolMode != ToolMode.TEXT_ANNOT_CREATE &&
                toolMode != ToolMode.COUNT_MEASUREMENT;
    }

    /**
     * The overload implementation of {@link Tool#onDown(MotionEvent)}.
     */
    @Override
    public boolean onDown(MotionEvent e) {
        setSnapCanDrawShapeIfEnabled(true);
        super.onDown(e);

        mOnUpCalled = true;

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        mSkipAfterQuickMenuClose = toolManager.isQuickMenuJustClosed();

        // The first touch-down point
        PointF snapPoint = snapToNearestIfEnabled(new PointF(e.getX(), e.getY()));
        mPt1.x = snapPoint.x + mPdfViewCtrl.getScrollX();
        mPt1.y = snapPoint.y + mPdfViewCtrl.getScrollY();

        // Remembers which page is touched initially and that is the page where
        // the annotation is going to reside on.
        mDownPageNum = mPdfViewCtrl.getPageNumberFromScreenPt(e.getX(), e.getY());
        if (mDownPageNum < 1) {
            mIsAllPointsOutsidePage = true;
            mDownPageNum = mPdfViewCtrl.getCurrentPage();
        } else {
            mIsAllPointsOutsidePage = false;
        }
        if (mDownPageNum >= 1) {
            mPageCropOnClientF = Utils.buildPageBoundBoxOnClient(mPdfViewCtrl, mDownPageNum);
            if (mPageBoundaryRestricted) {
                Utils.snapPointToRect(mPt1, mPageCropOnClientF);
            }
        }

        // The moving point that is the same with the touch-down point initially
        mPt2.set(mPt1);

        // Query for the default thickness and color, which are to be used when the
        // annotation is created in the derived classes.
        Context context = mPdfViewCtrl.getContext();
        SharedPreferences settings = Tool.getToolPreferences(context);
        mThickness = settings.getFloat(getThicknessKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultThickness(context, getCreateAnnotType()));
        mStrokeColor = settings.getInt(getColorKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultColor(context, getCreateAnnotType()));
        mFillColor = settings.getInt(getColorFillKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultFillColor(context, getCreateAnnotType()));
        mOpacity = settings.getFloat(getOpacityKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultOpacity(context, getCreateAnnotType()));

        if (mHasBorderStyle) {
            String borderStyle = settings.getString(getBorderStyleKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultBorderStyle(context, getCreateAnnotType()).name());
            mBorderStyle = ShapeBorderStyle.valueOf(borderStyle);
        } else if (mHasLineStyle) {
            String lineStyle = settings.getString(getLineStyleKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultLineStyle(context, getCreateAnnotType()).name());
            mLineStyle = LineStyle.valueOf(lineStyle);
        }

        if (mHasLineStartStyle) {
            String lineStartStyle = settings.getString(getLineStartStyleKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultLineStartStyle(context, getCreateAnnotType()).name());
            mLineStartStyle = LineEndingStyle.valueOf(lineStartStyle);
        }

        if (mHasLineEndStyle) {
            String lineEndStyle = settings.getString(getLineEndStyleKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultLineEndStyle(context, getCreateAnnotType()).name());
            mLineEndStyle = LineEndingStyle.valueOf(lineEndStyle);
        }

        float zoom = (float) mPdfViewCtrl.getZoom();
        mThicknessDraw = mThickness * zoom;
        mPaint.setStrokeWidth(mThicknessDraw);
        int color = Utils.getPostProcessedColor(mPdfViewCtrl, mStrokeColor);
        mPaint.setColor(color);
        mPaint.setAlpha((int) (255 * mOpacity));
        if (mHasFill) {
            mFillPaint.setColor(Utils.getPostProcessedColor(mPdfViewCtrl, mFillColor));
            mFillPaint.setAlpha((int) (255 * mOpacity));
        }

        mAnnotPushedBack = false;

        return false;
    }

    /**
     * The overload implementation of {@link Tool#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        super.onMove(e1, e2, x_dist, y_dist);

        if (mAllowTwoFingerScroll) {
            return false;
        }

        if (mAllowOneFingerScrollWithStylus) {
            return false;
        }

        if (mIsAllPointsOutsidePage) {
            // if any points was inside the page, it is OK to create the annot
            if (mPdfViewCtrl.getPageNumberFromScreenPt(e2.getX(), e2.getY()) >= 1) {
                mIsAllPointsOutsidePage = false;
            }
        }

        mSkipAfterQuickMenuClose = false; // reset as we are creating another one now

        // While moving, update the moving point so that a rubber band can be shown to
        // indicate the bounding box of the resulting annotation.
        float x = mPt2.x;
        float y = mPt2.y;
        PointF snapPoint = snapToNearestIfEnabled(new PointF(e2.getX(), e2.getY()));
        mPt2.x = snapPoint.x + mPdfViewCtrl.getScrollX();
        mPt2.y = snapPoint.y + mPdfViewCtrl.getScrollY();

        if (mPageBoundaryRestricted) {
            Utils.snapPointToRect(mPt2, mPageCropOnClientF);
        }

        float min_x = Math.min(Math.min(x, mPt2.x), mPt1.x) - mThicknessDraw;
        float max_x = Math.max(Math.max(x, mPt2.x), mPt1.x) + mThicknessDraw;
        float min_y = Math.min(Math.min(y, mPt2.y), mPt1.y) - mThicknessDraw;
        float max_y = Math.max(Math.max(y, mPt2.y), mPt1.y) + mThicknessDraw;

        mPdfViewCtrl.invalidate((int) min_x, (int) min_y, (int) Math.ceil(max_x), (int) Math.ceil(max_y));
        return true;
    }

    /**
     * The overload implementation of {@link Tool#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        super.onUp(e, priorEventMode);
        setSnapCanDrawShapeIfEnabled(false);
        if (!mOnUpCalled) {
            return false;
        }
        mOnUpCalled = false;

        // We are scrolling
        if (mAllowTwoFingerScroll) {
            doneTwoFingerScrolling();
            return false;
        }

        // consume quick menu
        if (mSkipAfterQuickMenuClose) {
            resetPts();
            return true;
        }

        if (priorEventMode == PDFViewCtrl.PriorEventMode.PAGE_SLIDING) {
            return false;
        }

        // If annotation was already pushed back, avoid re-entry due to fling motion
        // but allow when creating multiple strokes.
        if (mAnnotPushedBack && mForceSameNextToolMode) {
            return true;
        }

        // If all points are outside of the page, we don't push back the annotation
        if (mIsAllPointsOutsidePage) {
            return true;
        }

        // In stylus mode, ignore finger input
        mAllowOneFingerScrollWithStylus = mStylusUsed && e.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS;
        if (mAllowOneFingerScrollWithStylus) {
            return true;
        }

        // If both start point and end point are the same, we will try to tap to create
        if (mPt1.x == mPt2.x && mPt1.y == mPt2.y) {
            boolean canCreate = false;
            if (canTapToCreate()) {
                int x = (int) (e.getX() + 0.5);
                int y = (int) (e.getY() + 0.5);

                boolean shouldUnlockRead = false;
                try {
                    mPdfViewCtrl.docLockRead();
                    shouldUnlockRead = true;
                    Annot tempAnnot = ((ToolManager) mPdfViewCtrl.getToolManager()).getAnnotationAt(x, y);
                    canCreate = tempAnnot == null || !tempAnnot.isValid();
                } catch (Exception ignored) {
                } finally {
                    if (shouldUnlockRead) {
                        mPdfViewCtrl.docUnlockRead();
                    }
                }
                if (canCreate) {
                    // support tap creation
                    mPt1.x -= mTapToCreateShapeHalfSize;
                    mPt1.y -= mTapToCreateShapeHalfSize;

                    mPt2.x += mTapToCreateShapeHalfSize;
                    mPt2.y += mTapToCreateShapeHalfSize;

                    if (mPageBoundaryRestricted) {
                        Utils.snapPointToRect(mPt1, mPageCropOnClientF);
                        Utils.snapPointToRect(mPt2, mPageCropOnClientF);
                    }
                }
            }
            if (!canCreate) {
                resetPts();
                return true;
            }
        }

        setNextToolModeHelper();
        setCurrentDefaultToolModeHelper(getToolMode());
        // add UI to drawing list
        addOldTools();

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            com.pdftron.pdf.Rect rect = getShapeBBox();
            if (rect != null) {
                Annot markup = createMarkup(mPdfViewCtrl.getDoc(), rect);
                setStyle(markup);

                markup.refreshAppearance();

                Page page = mPdfViewCtrl.getDoc().getPage(mDownPageNum);
                if (page != null) {
                    page.annotPushBack(markup);
                    mAnnotPushedBack = true;
                    setAnnot(markup, mDownPageNum);
                    buildAnnotBBox();
                    mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
                    raiseAnnotationAddedEvent(mAnnot, mAnnotPageNum);
                }
            }
        } catch (Exception ex) {
            mNextToolMode = ToolMode.PAN;
            ((ToolManager) mPdfViewCtrl.getToolManager()).annotationCouldNotBeAdded(ex.getMessage());
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
            onCreateMarkupFailed(ex);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }

        return skipOnUpPriorEvent(priorEventMode);
    }

    /**
     * The overload implementation of {@link Tool#doneTwoFingerScrolling()}.
     */
    @Override
    protected void doneTwoFingerScrolling() {
        super.doneTwoFingerScrolling();

        mPt2.set(mPt1);
        mPdfViewCtrl.invalidate();
    }

    /**
     * The overload implementation of {@link Tool#onFlingStop()}.
     */
    @Override
    public boolean onFlingStop() {
        if (mAllowTwoFingerScroll) {
            doneTwoFingerScrolling();
        }
        return false;
    }

    /**
     * The overload implementation of {@link Tool#onScaleBegin(float, float)}.
     */
    @Override
    public boolean onScaleBegin(float x, float y) {
        // In the new version we allow scaling during annotation creation
        return super.onScaleBegin(x, y);
    }

    /**
     * @return The shape bounding box of the rubber band in page space
     */
    protected com.pdftron.pdf.Rect getShapeBBox() {
        // Computes the bounding box of the rubber band in page space.
        double[] pts1;
        double[] pts2;
        pts1 = mPdfViewCtrl.convScreenPtToPagePt(mPt1.x - mPdfViewCtrl.getScrollX(), mPt1.y - mPdfViewCtrl.getScrollY(), mDownPageNum);
        pts2 = mPdfViewCtrl.convScreenPtToPagePt(mPt2.x - mPdfViewCtrl.getScrollX(), mPt2.y - mPdfViewCtrl.getScrollY(), mDownPageNum);
        com.pdftron.pdf.Rect rect;
        try {
            rect = new com.pdftron.pdf.Rect(pts1[0], pts1[1], pts2[0], pts2[1]);
            return rect;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Sets the style of the specified annotations as the current style
     *
     * @param annot The annotation
     */
    protected void setStyle(Annot annot) {
        setStyle(annot, mHasFill);
    }

    /**
     * Sets the style of the specified annotations as the current style
     *
     * @param annot   The annotation
     * @param hasFill True if has fill property
     */
    protected void setStyle(Annot annot, boolean hasFill) {
        try {
            if (annot.isMarkup()) {
                Markup markup = new Markup(annot);
                setAuthor(markup);
                double[] dash = DrawingUtils.getShapesDashIntervals();
                if (mHasBorderStyle) {
                    switch (mBorderStyle) {
                        case CLOUDY:
                            AnnotUtils.setBorderStyle(annot, Markup.e_Cloudy, Annot.BorderStyle.e_solid, null);
                            break;
                        case DASHED:
                            AnnotUtils.setBorderStyle(annot, Markup.e_None, Annot.BorderStyle.e_dashed, dash);
                            break;
                        case DEFAULT:
                            AnnotUtils.setBorderStyle(annot, Markup.e_None, Annot.BorderStyle.e_solid, null);
                            break;
                    }
                } else if (mHasLineStyle) {
                    switch (mLineStyle) {
                        case DASHED:
                            AnnotUtils.setBorderStyle(annot, Markup.e_None, Annot.BorderStyle.e_dashed, dash);
                            break;
                        case DEFAULT:
                            AnnotUtils.setBorderStyle(annot, Markup.e_None, Annot.BorderStyle.e_solid, null);
                            break;
                    }
                }
                if (annot.getType() == Annot.e_Line || annot.getType() == Annot.e_Polyline) {
                    Line lineAnnot = new Line(annot);
                    if (mHasLineStartStyle) {
                        AnnotUtils.setLineEndingStyle(lineAnnot, mLineStartStyle, true);
                    }
                    if (mHasLineEndStyle) {
                        AnnotUtils.setLineEndingStyle(lineAnnot, mLineEndStyle, false);
                    }
                }
            }

            // set style at the end as border style will alter the width of stroke
            AnnotUtils.setStyle(annot, hasFill,
                    mStrokeColor, mFillColor,
                    mThickness, mOpacity);
        } catch (PDFNetException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * create markup annotation, called in {@link #onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}
     *
     * @param doc  PDF Document
     * @param bbox bounding box
     * @return Markup annotation
     * @throws PDFNetException PDFNet exception
     */
    protected abstract Annot createMarkup(@NonNull PDFDoc doc, Rect bbox) throws PDFNetException;

    protected boolean canTapToCreate() {
        return false;
    }

    /**
     * reset drawing pts
     */
    protected void resetPts() {
        mPt1.set(0, 0);
        mPt2.set(0, 0);
    }

    /**
     * set next tool mode helper
     */
    @SuppressWarnings("WeakerAccess")
    protected void setNextToolModeHelper() {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        if (toolManager.isAutoSelectAnnotation()) {
            mNextToolMode = getDefaultNextTool();
        } else {
            mNextToolMode = mForceSameNextToolMode ? getToolMode() : ToolManager.ToolMode.PAN;
        }
    }

    /**
     * Gets alternative next tool mode if next tool is not current tool
     * By default, it is ToolMode.ANNOT_EDIT, if the alternative tool of subclass
     * is not ToolMode.ANNOT_EDIT, then the subclass can override this method.
     *
     * @return alternative next tool mode
     */
    protected ToolMode getDefaultNextTool() {
        return ToolMode.ANNOT_EDIT;
    }

    /**
     * Called when creating markup annotaiton failed
     *
     * @param e
     */
    protected void onCreateMarkupFailed(Exception e) {
        // do nothing by default
        Log.e(TAG, "onCreateMarkupFailed", e);
    }

    @Override
    protected boolean canDrawLoupe() {
        return false;
    }

    @Override
    protected int getLoupeType() {
        return LOUPE_TYPE_TEXT;
    }
}
