//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.MotionEvent;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.QuadPoint;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.annots.Popup;
import com.pdftron.pdf.annots.Redaction;
import com.pdftron.pdf.annots.TextMarkup;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.Utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * This class is the base class for all text markup creation tools.
 */
@Keep
abstract public class TextMarkupCreate extends BaseTool {

    Rect mInvalidateBBox;

    float mOpacity;
    int mColor;
    float mThickness;
    int mFillColor; // for redaction

    Paint mPaint;

    Path mSelPath;
    RectF mTempRect;
    RectF mSelBBox;
    PointF mStationPt;

    boolean mOnUpCalled;

    protected boolean mIsPointOutsidePage;
    boolean mDrawWithFinger = false;

    /**
     * Class constructor
     */
    public TextMarkupCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);

        mSelPath = new Path();
        mTempRect = new RectF();
        mSelBBox = new RectF();
        mStationPt = new PointF();

        mOnUpCalled = false;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        mInvalidateBBox = new Rect();

        mNextToolMode = getToolMode();
        mDrawWithFinger = PdfViewCtrlSettingsManager.getDrawWithFinger(ctrl.getContext());
    }

    /**
     * @return whether stylus as pen behavior should be applied on current ink stroke.
     */
    private boolean shouldApplyStylusAsPenBehavior() {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        boolean stylusAsPen = toolManager != null && toolManager.isStylusAsPen();
        if (mDrawWithFinger) {
            return mIsStylus && stylusAsPen;
        } else {
            return stylusAsPen;
        }
    }

    /**
     * Returns annotation index for adding markup.
     *
     * @param page The page
     * @return The annotation index
     */
    protected static int getAnnotIndexForAddingMarkup(Page page) {
        int index = 0;
        boolean foundMarkupAnnot = false;

        try {
            for (index = page.getNumAnnots() - 1; index > 0; --index) {
                int type = page.getAnnot(index).getType();
                if (type == com.pdftron.pdf.Annot.e_Highlight ||
                        type == com.pdftron.pdf.Annot.e_Underline ||
                        type == com.pdftron.pdf.Annot.e_Squiggly ||
                        type == com.pdftron.pdf.Annot.e_StrikeOut ||
                        type == com.pdftron.pdf.Annot.e_Link ||
                        type == com.pdftron.pdf.Annot.e_Widget) {
                    foundMarkupAnnot = true;
                    break;
                }
            }
        } catch (PDFNetException ex) {

        }

        if (foundMarkupAnnot) {
            ++index;
        }
        return index;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Unknown;
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    abstract public ToolManager.ToolModeBase getToolMode();

    /**
     * The overload implementation of {@link Tool#isCreatingAnnotation()}.
     */
    @Override
    public boolean isCreatingAnnotation() {
        return true;
    }

    /**
     * The overload implementation of {@link Tool#setupAnnotProperty(int, float, float, int, String, String)}.
     */
    @Override
    public void setupAnnotProperty(int color, float opacity, float thickness, int fillColor, String icon, String pdfTronFontName) {
        mColor = color;
        mOpacity = opacity;
        mThickness = thickness;
        mFillColor = fillColor;

        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt(getColorKey(getCreateAnnotType()), mColor);
        editor.putFloat(getOpacityKey(getCreateAnnotType()), mOpacity);
        editor.putFloat(getThicknessKey(getCreateAnnotType()), mThickness);
        editor.putInt(getColorFillKey(getCreateAnnotType()), mFillColor);

        editor.apply();
    }

    /**
     * The overload implementation of {@link Tool#onSingleTapConfirmed(MotionEvent)}.
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        if (mForceSameNextToolMode) {
            // If annotation was already pushed back, avoid re-entry due to single tap
            if (mAnnotPushedBack) {
                return true;
            }

            int x = (int) (e.getX() + 0.5);
            int y = (int) (e.getY() + 0.5);

            Annot tempAnnot = ((ToolManager) mPdfViewCtrl.getToolManager()).getAnnotationAt(x, y);
            int page = mPdfViewCtrl.getPageNumberFromScreenPt(x, y);

            setCurrentDefaultToolModeHelper(getToolMode());

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

    /**
     * The overload implementation of {@link Tool#onDown(MotionEvent)}.
     */
    @Override
    public boolean onDown(MotionEvent e) {
        super.onDown(e);
        if ((mStylusUsed || shouldApplyStylusAsPenBehavior()) && !mIsStylus) {
            return false;
        }

        mLoupeEnabled = true;
        mOnUpCalled = true;

        mColor = 0xFFFF00;
        mOpacity = 1.0f;
        mThickness = 1.0f;
        mFillColor = Color.TRANSPARENT;

        mAnnotPushedBack = false;

        float x = e.getX() + mPdfViewCtrl.getScrollX();
        float y = e.getY() + mPdfViewCtrl.getScrollY();

        mStationPt.set(x, y);

        setLoupeInfo(e.getX(), e.getY());
        mPdfViewCtrl.invalidate();
        animateLoupe(true);

        int page = mPdfViewCtrl.getPageNumberFromScreenPt(e.getX(), e.getY());
        if (page < 1) {
            mIsPointOutsidePage = true;
        } else {
            mIsPointOutsidePage = false;
        }

        return false;
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
     * The overload implementation of {@link Tool#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        // ignore if from two finger scrolling
        if (mAllowTwoFingerScroll) {
            doneTwoFingerScrolling();
            animateLoupe(false);
            mLoupeEnabled = false;
            return false;
        }

        if (priorEventMode == PDFViewCtrl.PriorEventMode.PAGE_SLIDING) {
            return false;
        }

        // If annotation was already pushed back, avoid re-entry due to fling motion.
        if (mAnnotPushedBack && mForceSameNextToolMode) {
            return true;
        }

        // In stylus mode, ignore finger input
        mAllowOneFingerScrollWithStylus = mStylusUsed && e.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS;
        if (mAllowOneFingerScrollWithStylus) {
            animateLoupe(false);
            mLoupeEnabled = false;
            return false;
        }

        if (mOnUpCalled) {

            mOnUpCalled = false;

            if (!mPdfViewCtrl.hasSelection()) {
                try {
                    if (!((ToolManager) mPdfViewCtrl.getToolManager()).isQuickMenuJustClosed()) {
                        // if no selection yet, let's see if it is OK tap to markup
                        int x = (int) (e.getX() + 0.5);
                        int y = (int) (e.getY() + 0.5);
                        if (!hasAnnot(x, y)) {
                            float sx = mPdfViewCtrl.getScrollX();
                            float sy = mPdfViewCtrl.getScrollY();
                            mPressedPoint.x = e.getX() + sx;
                            mPressedPoint.y = e.getY() + sy;

                            selectText(mStationPt.x - sx, mStationPt.y - sy, e.getX(), e.getY(), true);
                            mPdfViewCtrl.invalidate(mInvalidateBBox);
                        }
                    }
                } catch (Exception ex) {
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                }
            }

            mPdfViewCtrl.invalidate(); //always needed to draw away the previous loupe even if there is not any selection.
            createTextMarkup();
        }

        animateLoupe(false);

        return skipOnUpPriorEvent(priorEventMode);
    }

    protected void createTextMarkup() {
        if (!mPdfViewCtrl.hasSelection()) {
            return;
        }
        int sel_pg_begin = mPdfViewCtrl.getSelectionBeginPage();
        int sel_pg_end = mPdfViewCtrl.getSelectionEndPage();

        class AnnotUpdateInfo {
            Annot mAnnot;
            int mPageNum;
            com.pdftron.pdf.Rect mRect;

            public AnnotUpdateInfo(Annot annot, int pageNum, com.pdftron.pdf.Rect rect) {
                mAnnot = annot;
                mPageNum = pageNum;
                mRect = rect;
            }
        }

        LinkedList<AnnotUpdateInfo> updateInfoList = new LinkedList<>();

        boolean shouldUnlock = false;
        try {
            //setNextToolModeHelper(ToolMode.ANNOT_EDIT_TEXT_MARKUP);
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            if (toolManager.isAutoSelectAnnotation()) {
                mNextToolMode = ToolMode.ANNOT_EDIT_TEXT_MARKUP;
            } else {
                mNextToolMode = mForceSameNextToolMode ? getToolMode() : ToolManager.ToolMode.PAN;
            }

            setCurrentDefaultToolModeHelper(getToolMode());
            // add UI to drawing list
            addOldTools();

            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            PDFDoc doc = mPdfViewCtrl.getDoc();
            for (int pg = sel_pg_begin; pg <= sel_pg_end; ++pg) {
                PDFViewCtrl.Selection sel = mPdfViewCtrl.getSelection(pg);
                double[] quads = sel.getQuads();
                int sz = quads.length / 8;
                if (sz == 0) {
                    continue;
                }

                Point p1 = new Point();
                Point p2 = new Point();
                Point p3 = new Point();
                Point p4 = new Point();
                QuadPoint qp = new QuadPoint(p1, p2, p3, p4);

                com.pdftron.pdf.Rect bbox = new com.pdftron.pdf.Rect(quads[0], quads[1], quads[4], quads[5]); //just use the first quad to temporarily populate the bbox
                Annot tm = createMarkup(doc, bbox);

                Context context = mPdfViewCtrl.getContext();
                SharedPreferences settings = Tool.getToolPreferences(context);
                mColor = settings.getInt(getColorKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultColor(context, getCreateAnnotType()));
                mOpacity = settings.getFloat(getOpacityKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultOpacity(context, getCreateAnnotType()));
                mThickness = settings.getFloat(getThicknessKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultThickness(context, getCreateAnnotType()));
                mFillColor = settings.getInt(getColorFillKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultFillColor(context, getCreateAnnotType()));

                boolean useAdobeHack = toolManager.isTextMarkupAdobeHack();

                int k = 0;
                // left, right, top, bottom
                double left = 0;
                double right = 0;
                double top = 0;
                double bottom = 0;
                for (int i = 0; i < sz; ++i, k += 8) {
                    p1.x = quads[k];
                    p1.y = quads[k + 1];

                    p2.x = quads[k + 2];
                    p2.y = quads[k + 3];

                    p3.x = quads[k + 4];
                    p3.y = quads[k + 5];

                    p4.x = quads[k + 6];
                    p4.y = quads[k + 7];

                    if (useAdobeHack) {
                        qp.p1 = p4;
                        qp.p2 = p3;
                        qp.p3 = p1;
                        qp.p4 = p2;
                    } else {
                        qp.p1 = p1;
                        qp.p2 = p2;
                        qp.p3 = p3;
                        qp.p4 = p4;
                    }
                    if (tm != null && tm instanceof TextMarkup) {
                        ((TextMarkup) tm).setQuadPoint(i, qp);
                    } else if (tm != null && tm instanceof Redaction) {
                        ((Redaction) tm).setQuadPoint(i, qp);
                    } else {
                        // find rect for non TextMarkup annotation
                        if (0 == left) {
                            left = p1.x;
                        } else {
                            left = Math.min(left, p1.x);
                        }
                        right = Math.max(right, p2.x);
                        if (0 == top) {
                            top = p1.y;
                        } else {
                            top = Math.min(top, p1.y);
                        }
                        bottom = Math.max(bottom, p3.y);
                        setAnnotRect(tm, new com.pdftron.pdf.Rect(left, top, right, bottom), pg);
                    }
                }

                if (tm != null) {
                    ColorPt colorPt = Utils.color2ColorPt(mColor);
                    tm.setColor(colorPt, 3);
                    // current tm is Markup
                    if (tm instanceof Markup) {
                        ((Markup) tm).setOpacity(mOpacity);
                        setAuthor((Markup) tm);
                        if (((ToolManager) mPdfViewCtrl.getToolManager()).isCopyAnnotatedTextToNoteEnabled()) {
                            // create note from selected text
                            try {
                                Popup p = Popup.create(mPdfViewCtrl.getDoc(), tm.getRect());
                                p.setParent(tm);
                                ((Markup) tm).setPopup(p);
                                p.setContents(sel.getAsUnicode());
                                Utils.setTextCopy(tm);
                            } catch (PDFNetException ex) {
                                AnalyticsHandlerAdapter.getInstance().sendException(ex);
                            }
                        }
                    }

                    if (tm instanceof Redaction) {
                        ColorPt fillColorPt = Utils.color2ColorPt(mFillColor);
                        ((Redaction) tm).setInteriorColor(fillColorPt, 3);
                    }

                    if (tm.getType() != com.pdftron.pdf.Annot.e_Highlight) {
                        com.pdftron.pdf.Annot.BorderStyle bs = tm.getBorderStyle();
                        bs.setWidth(mThickness);
                        tm.setBorderStyle(bs);
                    }

                    Page page = mPdfViewCtrl.getDoc().getPage(pg);
                    int index = getAnnotIndexForAddingMarkup(page);
                    page.annotInsert(index, tm);
                    tm.refreshAppearance();

                    mAnnotPushedBack = true;
                    setAnnot(tm, pg);
                    buildAnnotBBox();

                    //compute the bbox of the annotation in screen space
                    com.pdftron.pdf.Rect ur = AnnotUtils.computeAnnotInbox(mPdfViewCtrl, tm, pg);
                    updateInfoList.add(new AnnotUpdateInfo(tm, pg, ur));
                }
            }

            //clear existing selections
            if (!mSelPath.isEmpty()) {
                mSelPath.reset();
            }
            mPdfViewCtrl.clearSelection();

            // make sure to raise add event after mPdfViewCtrl.update
            HashMap<Annot, Integer> annots = new HashMap<>();
            Iterator<AnnotUpdateInfo> itr = updateInfoList.iterator();
            while (itr.hasNext()) {
                AnnotUpdateInfo updateInfo = itr.next();
                Annot annot = updateInfo.mAnnot;
                int pageNum = updateInfo.mPageNum;
                if (annot != null) {
                    annots.put(annot, pageNum);
                    if (mPdfViewCtrl.isAnnotationLayerEnabled()) {
                        mPdfViewCtrl.update(annot, pageNum);
                    } else {
                        com.pdftron.pdf.Rect rect = updateInfo.mRect;
                        mPdfViewCtrl.update(rect);
                    }
                }
            }
            raiseAnnotationAddedEvent(annots);

            mPdfViewCtrl.invalidate(); //always needed to draw away the previous loupe even if there is not any selection.

            //after highlighting, register a custom callback, in which will
            //switch to pan tool.
            //mPdfViewCtrl.postToolOnCustomEvent(null);
        } catch (Exception ex) {
            ((ToolManager) mPdfViewCtrl.getToolManager()).annotationCouldNotBeAdded(ex.getMessage());
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    /**
     * Sets rectangle to annotation
     *
     * @param annot   The annotation
     * @param rect    The rectangle area to set to annotation
     * @param pageNum The page number of annotation
     */
    protected void setAnnotRect(@Nullable Annot annot, com.pdftron.pdf.Rect rect, int pageNum) throws PDFNetException {
        if (annot == null) {
            return;
        }
        annot.setRect(rect);
    }

    /**
     * The overload implementation of {@link Tool#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        super.onMove(e1, e2, x_dist, y_dist);

        if ((mStylusUsed || shouldApplyStylusAsPenBehavior()) && !mIsStylus) {
            animateLoupe(false);
            mLoupeEnabled = false;
            return false;
        }

        if (mAllowTwoFingerScroll) {
            animateLoupe(false);
            mLoupeEnabled = false;
            return false;
        }

        if (mAllowOneFingerScrollWithStylus) {
            animateLoupe(false);
            mLoupeEnabled = false;
            return false;
        }

        float sx = mPdfViewCtrl.getScrollX();
        float sy = mPdfViewCtrl.getScrollY();

        selectText(mStationPt.x - sx, mStationPt.y - sy, e2.getX(), e2.getY(), false);
        mPdfViewCtrl.invalidate(mInvalidateBBox);
        return true;
    }

    /**
     * The overload implementation of {@link Tool#onDraw(Canvas, Matrix)}.
     */
    // It is OK to suppress lint warning (isHardwareAccelerated) since
    // PDFViewCtrl deals with it internally.
    @SuppressLint("NewApi")
    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        if (mAllowTwoFingerScroll) {
            return;
        }

        if (mIsPointOutsidePage) {
            return;
        }

        if (mAllowOneFingerScrollWithStylus) {
            return;
        }
        boolean loupeEnabled = mLoupeEnabled;
        mLoupeEnabled = false;
        if (!mDrawingLoupe) {
            super.onDraw(canvas, tfm);
        }
        mLoupeEnabled = loupeEnabled;

        if (mOnUpCalled) {

            drawLoupe();

            //draw the selection
            if (!mSelPath.isEmpty()) {
                mPaint.setStyle(Paint.Style.FILL);
                mPaint.setColor(Color.rgb(0, 100, 175));
                mPaint.setAlpha(127);
                canvas.drawPath(mSelPath, mPaint);
            }
        }
    }

    /**
     * Selects text in the specified rectangle.
     *
     * @param x1     The x coordinate at one of the end point of the rectangle
     * @param y1     The y coordinate at one of the end point of the rectangle
     * @param x2     The x coordinate at another point
     * @param y2     The y coordinate at another point
     * @param byRect True if should select by rectangle;
     *               false if should select by struct with smart snapping
     * @return True if some text was selected
     */
    protected boolean selectText(float x1, float y1, float x2, float y2, boolean byRect) {
        if (byRect) {
            float delta = 0.01f;
            x2 += delta;
            y2 += delta;
            delta *= 2;
            x1 = x2 - delta >= 0 ? x2 - delta : 0;
            y1 = y2 - delta >= 0 ? y2 - delta : 0;
        }
        boolean result = false;
        //clear pre-selected content

        boolean had_sel = !mSelPath.isEmpty();
        mSelPath.reset();

        //select text
        boolean shouldUnlockRead = false;
        try {
            //locks the document first as accessing annotation/doc information isn't thread safe.
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            if (byRect) {
                result = mPdfViewCtrl.selectByRect(x1, y1, x2, y2);
            } else {
                result = mPdfViewCtrl.selectByStructWithSmartSnapping(x1, y1, x2, y2);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }

        //update the bounding box that should include:
        //(1)previous selection bbox and loupe bbox
        //(2)current selection bbox and loupe bbox
        if (had_sel) {
            mTempRect.set(mSelBBox);
        }
        populateSelectionResult();
        if (!had_sel) {
            mTempRect.set(mSelBBox);
        } else {
            mTempRect.union(mSelBBox);
        }

        mTempRect.union(mLoupeBBox);
        setLoupeInfo(x2, y2);
        mTempRect.union(mLoupeBBox);

        return result;
    }

    private void populateSelectionResult() {
        float sx = mPdfViewCtrl.getScrollX();
        float sy = mPdfViewCtrl.getScrollY();
        int sel_pg_begin = mPdfViewCtrl.getSelectionBeginPage();
        int sel_pg_end = mPdfViewCtrl.getSelectionEndPage();
        float min_x = 1E10f, min_y = 1E10f, max_x = 0, max_y = 0;
        boolean has_sel = false;

        // loop through the pages that have text selection, and construct 'mSelPath' for highlighting.
        // NOTE: android has a bug that if hardware acceleration is turned on and the path is too big,
        // it may not get rendered. See http://code.google.com/p/android/issues/detail?id=24023
        for (int pg = sel_pg_begin; pg <= sel_pg_end; ++pg) {
            PDFViewCtrl.Selection sel = mPdfViewCtrl.getSelection(pg); // each Selection may have multiple quads
            double[] quads = sel.getQuads();
            double[] pts;
            int sz = quads.length / 8; // each quad has eight numbers (x0, y0), ... (x3, y3)

            if (sz == 0) {
                continue;
            }
            int k = 0;
            float x, y;
            for (int i = 0; i < sz; ++i, k += 8) {
                has_sel = true;

                pts = mPdfViewCtrl.convPagePtToScreenPt(quads[k], quads[k + 1], pg);
                x = (float) pts[0] + sx;
                y = (float) pts[1] + sy;
                mSelPath.moveTo(x, y);
                min_x = min_x > x ? x : min_x;
                max_x = max_x < x ? x : max_x;
                min_y = min_y > y ? y : min_y;
                max_y = max_y < y ? y : max_y;

                if (pg == sel_pg_begin && i == 0) {
                    // set the start point of the first selection widget that is based
                    // on the first quad point.
                    // mSelWidgets[0].mStrPt.set(x-mTSWidgetThickness/2, y);
                    // x -= mTSWidgetThickness + mTSWidgetRadius;
                    min_x = min_x > x ? x : min_x;
                    max_x = max_x < x ? x : max_x;
                }

                pts = mPdfViewCtrl.convPagePtToScreenPt(quads[k + 2], quads[k + 3], pg);
                x = (float) pts[0] + sx;
                y = (float) pts[1] + sy;
                mSelPath.lineTo(x, y);
                min_x = min_x > x ? x : min_x;
                max_x = max_x < x ? x : max_x;
                min_y = min_y > y ? y : min_y;
                max_y = max_y < y ? y : max_y;

                if (pg == sel_pg_end && i == sz - 1) {
                    // set the end point of the second selection widget that is based
                    // on the last quad point.
                    // mSelWidgets[1].mEndPt.set(x+mTSWidgetThickness/2, y);
                    // x += mTSWidgetThickness + mTSWidgetRadius;
                    // y += mTSWidgetRadius * 2;
                    min_x = min_x > x ? x : min_x;
                    max_x = max_x < x ? x : max_x;
                    min_y = min_y > y ? y : min_y;
                    max_y = max_y < y ? y : max_y;
                }

                pts = mPdfViewCtrl.convPagePtToScreenPt(quads[k + 4], quads[k + 5], pg);
                x = (float) pts[0] + sx;
                y = (float) pts[1] + sy;
                mSelPath.lineTo(x, y);
                min_x = min_x > x ? x : min_x;
                max_x = max_x < x ? x : max_x;
                min_y = min_y > y ? y : min_y;
                max_y = max_y < y ? y : max_y;

                if (pg == sel_pg_end && i == sz - 1) {
                    // set the start point of the second selection widget that is based
                    // on the last quad point.
                    // mSelWidgets[1].mStrPt.set(x+mTSWidgetThickness/2, y);
                    // x += mTSWidgetThickness + mTSWidgetRadius;
                    min_x = min_x > x ? x : min_x;
                    max_x = max_x < x ? x : max_x;
                }

                pts = mPdfViewCtrl.convPagePtToScreenPt(quads[k + 6], quads[k + 7], pg);
                x = (float) pts[0] + sx;
                y = (float) pts[1] + sy;
                mSelPath.lineTo(x, y);
                min_x = min_x > x ? x : min_x;
                max_x = max_x < x ? x : max_x;
                min_y = min_y > y ? y : min_y;
                max_y = max_y < y ? y : max_y;

                if (pg == sel_pg_begin && i == 0) {
                    // set the end point of the first selection widget that is based
                    // on the first quad point.
                    // mSelWidgets[0].mEndPt.set(x-mTSWidgetThickness/2, y);
                    // x -= mTSWidgetThickness + mTSWidgetRadius;
                    // y -= mTSWidgetRadius * 2;
                    min_x = min_x > x ? x : min_x;
                    max_x = max_x < x ? x : max_x;
                    min_y = min_y > y ? y : min_y;
                    max_y = max_y < y ? y : max_y;
                }

                mSelPath.close();
            }
        }

        if (has_sel) {
            mSelBBox.set(min_x, min_y, max_x, max_y);
            mSelBBox.round(mInvalidateBBox);
        }
    }

    /**
     * The overload implementation of {@link Tool#doneTwoFingerScrolling()}.
     */
    @Override
    protected void doneTwoFingerScrolling() {
        super.doneTwoFingerScrolling();

        // We are up from scrolling
        if (mPdfViewCtrl.hasSelection()) {
            //clear existing selections
            if (!mSelPath.isEmpty()) {
                mSelPath.reset();
            }
            mPdfViewCtrl.clearSelection();
        }
        mPdfViewCtrl.invalidate(); //always needed to draw away the previous loupe even if there is not any selection.
    }

    private boolean hasAnnot(int x, int y) {
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            Annot annot = ((ToolManager) mPdfViewCtrl.getToolManager()).getAnnotationAt(x, y);
            if (isValidAnnot(annot)) {
                return true;
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
        return false;
    }

    /**
     * Creates a text markup.
     *
     * @param doc  The PDF doc
     * @param bbox The bounding box to create a markup
     * @return The created text markup
     * @throws PDFNetException
     */
    protected abstract Annot createMarkup(PDFDoc doc, com.pdftron.pdf.Rect bbox) throws PDFNetException;

    /**
     * When quick menu clicked, creates text markup annotation
     *
     * @param menuItem The clicked menu item.
     * @return true if handled, else otherwise
     */
    @Override
    public boolean onQuickMenuClicked(QuickMenuItem menuItem) {
        safeSetNextToolMode(getCurrentDefaultToolMode());
        Bundle bundle = new Bundle();
        bundle.putStringArray(KEYS, new String[]{"menuItemId"});
        bundle.putInt("menuItemId", menuItem.getItemId());
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return true;
        }
        // create text markup annotation
        createTextMarkup();

        return true;
    }

    @Override
    protected boolean canDrawLoupe() {
        return !mDrawingLoupe;
    }

    @Override
    protected int getLoupeType() {
        return LOUPE_TYPE_TEXT;
    }
}
