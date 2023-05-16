//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Toast;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ContentReplacer;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * This class selects text on pages.
 */
@Keep
public class TextSelect extends BaseTool {

    protected final float mTSWidgetThickness;
    protected boolean mIsRightToLeft;
    protected boolean mIsNightMode = false;
    LinkedList<RectF> mSelRects;
    Path mSelPath;
    boolean mBeingLongPressed;
    boolean mBeingPressed;
    RectF mSelBBox;
    RectF mTempRect;
    com.pdftron.pdf.Rect mTempRotationRect;
    PointF[] mQuadPoints;
    Rect mInvalidateBBox;
    boolean mScaled;
    PDFViewCtrl.PagePresentationMode mPagePresModeWhileSelected;
    int mEffSelWidgetId;
    boolean mSelWidgetEnabled;
    SelWidget[] mSelWidgets;
    PointF mStationPt;
    Paint mPaint;
    int mSelColor;
    PorterDuffXfermode mBlendmode;
    // Text To Speech
    private TextToSpeech mTTS;

    // text edit
    private int mAnnotButtonPressed;
    private DialogAnnotNote mDialogAnnotNote;

    /**
     * Class constructor
     */
    @SuppressWarnings("WeakerAccess")
    public TextSelect(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);

        mTSWidgetThickness = this.convDp2Pix(2);

        mSelRects = new LinkedList<>();
        mSelPath = new Path();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mBeingLongPressed = false;
        mBeingPressed = false;
        mSelBBox = new RectF();
        mTempRect = new RectF();
        try {
            mTempRotationRect = new com.pdftron.pdf.Rect();
        } catch (PDFNetException e) {
            mTempRotationRect = null;
        }
        mQuadPoints = new PointF[4];
        mQuadPoints[0] = new PointF();
        mQuadPoints[1] = new PointF();
        mQuadPoints[2] = new PointF();
        mQuadPoints[3] = new PointF();
        mInvalidateBBox = new Rect();
        mEffSelWidgetId = -1;
        mSelWidgetEnabled = false;
        mSelWidgets = new SelWidget[2];
        mSelWidgets[0] = new SelWidget();
        mSelWidgets[1] = new SelWidget();
        mSelWidgets[0].mStrPt = new PointF();
        mSelWidgets[0].mEndPt = new PointF();
        mSelWidgets[1].mStrPt = new PointF();
        mSelWidgets[1].mEndPt = new PointF();
        mStationPt = new PointF();
        mDrawingLoupe = false;
        mScaled = false;

        mIsRightToLeft = mPdfViewCtrl.getRightToLeftLanguage();
        try {
            mIsNightMode = ((ToolManager) mPdfViewCtrl.getToolManager()).isNightMode();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        mSelColor = mPdfViewCtrl.getContext().getResources().getColor(
                mIsNightMode ? R.color.tools_text_select_color_dark :
                        R.color.tools_text_select_color
        );

        mBlendmode = mIsNightMode ? new PorterDuffXfermode(PorterDuff.Mode.SCREEN) :
                new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY);
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.TEXT_SELECT;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Unknown;
    }

    /**
     * The overload implementation of {@link Tool#onCreate()}.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        // Add mru items and the remainder to the overflow menu
    }

    @Override
    protected QuickMenu createQuickMenu() {
        QuickMenu quickMenu = super.createQuickMenu();
        quickMenu.inflate(R.menu.text_select);
        customizeQuickMenuItems(quickMenu);
        quickMenu.addMenuEntries(QM_MAX_ROW_SIZE);
        return quickMenu;
    }

    @Override
    protected void customizeQuickMenuItems(QuickMenu quickMenu) {
        super.customizeQuickMenuItems(quickMenu);

        QuickMenuItem editItem = quickMenu.findMenuItem(R.id.qm_edit);
        if (editItem != null) {
            if (((ToolManager) mPdfViewCtrl.getToolManager()).isPdfContentEditingEnabled()) {
                editItem.setVisible(true);
            } else {
                editItem.setVisible(false);
            }
        }
    }

    /**
     * The overload implementation of {@link Tool#onCustomEvent(Object)}.
     */
    @Override
    public void onCustomEvent(Object o) {
        mNextToolMode = ToolMode.PAN;
    }

    /**
     * The overload implementation of {@link Tool#onDown(MotionEvent)}.
     */
    @Override
    public boolean onDown(MotionEvent e) {
        super.onDown(e);
        mLoupeEnabled = true;
        float x = e.getX() + mPdfViewCtrl.getScrollX();
        float y = e.getY() + mPdfViewCtrl.getScrollY();
        mBeingPressed = true;

        // Test if one of the two select widgets are hit
        mEffSelWidgetId = hitTest(x, y);

        if (getToolMode() == ToolMode.TEXT_SELECT && ShortcutHelper.isTextSelect(e)) {
            mNextToolMode = ToolMode.TEXT_SELECT;
            mStationPt.set(mPressedPoint);
        }

        if (mEffSelWidgetId >= 0) {
            // Update station point that is the starting selection point.
            x = (mSelWidgets[1 - mEffSelWidgetId].mStrPt.x + mSelWidgets[1 - mEffSelWidgetId].mEndPt.x) / 2;
            y = (mSelWidgets[1 - mEffSelWidgetId].mStrPt.y + mSelWidgets[1 - mEffSelWidgetId].mEndPt.y) / 2;
            mStationPt.set(x, y);

            // Show loupe
            setLoupeInfo(e.getX(), e.getY());
            mPdfViewCtrl.invalidate();
            animateLoupe(true);
        }
        return false;
    }

    /**
     * The overload implementation of {@link Tool#onScaleEnd(float, float)}.
     */
    @Override
    public boolean onScaleEnd(float x, float y) {
        super.onScaleEnd(x, y);
        mScaled = true;
        return false;
    }

    /**
     * The overload implementation of {@link Tool#onKeyUp(int, KeyEvent)}.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mPdfViewCtrl == null) {
            return false;
        }

        if (isQuickMenuShown() && hasMenuEntry(R.id.qm_copy) && ShortcutHelper.isCopy(keyCode, event)) {
            closeQuickMenu();
            copyAnnot(ViewerUtils.getSelectedString(mPdfViewCtrl), null);
            return true;
        }

        if (mPdfViewCtrl.hasSelection() && isQuickMenuShown()) {
            if (hasMenuEntry(R.id.qm_highlight) && ShortcutHelper.isHighlightAnnot(keyCode, event)) {
                onQuickMenuClicked(new QuickMenuItem(mPdfViewCtrl.getContext(), R.id.qm_highlight));
                return true;
            }

            if (hasMenuEntry(R.id.qm_underline) && ShortcutHelper.isUnderlineAnnot(keyCode, event)) {
                onQuickMenuClicked(new QuickMenuItem(mPdfViewCtrl.getContext(), R.id.qm_underline));
                return true;
            }

            if (hasMenuEntry(R.id.qm_strikeout) && ShortcutHelper.isStrikethroughAnnot(keyCode, event)) {
                onQuickMenuClicked(new QuickMenuItem(mPdfViewCtrl.getContext(), R.id.qm_strikeout));
                return true;
            }

            if (hasMenuEntry(R.id.qm_squiggly) && ShortcutHelper.isSquigglyAnnot(keyCode, event)) {
                onQuickMenuClicked(new QuickMenuItem(mPdfViewCtrl.getContext(), R.id.qm_squiggly));
                return true;
            }

            if (hasMenuEntry(R.id.qm_link) && ShortcutHelper.isHyperlinkAnnot(keyCode, event)) {
                onQuickMenuClicked(new QuickMenuItem(mPdfViewCtrl.getContext(), R.id.qm_link));
                return true;
            }
        }

        if (isQuickMenuShown() && ShortcutHelper.isCloseMenu(keyCode, event)) {
            onClose();
            exitCurrentMode();
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    /**
     * The overload implementation of {@link Tool#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        if (hasSelection()) {
            mPagePresModeWhileSelected = mPdfViewCtrl.getPagePresentationMode();
            mSelWidgetEnabled = true;

            if (mScaled
                    || priorEventMode == PDFViewCtrl.PriorEventMode.SCROLLING
                    || priorEventMode == PDFViewCtrl.PriorEventMode.PINCH
                    || priorEventMode == PDFViewCtrl.PriorEventMode.DOUBLE_TAP
                    || (mBeingLongPressed && priorEventMode != PDFViewCtrl.PriorEventMode.FLING)) {
                // After zooming, re-populate the selection result
                mSelPath.reset();
                populateSelectionResult();
            }
            showMenu(getQMAnchorRect());
        } else {
            // no selection
            exitCurrentMode();
        }

        mScaled = false;
        mBeingLongPressed = false;
        mBeingPressed = false;
        mEffSelWidgetId = -1;
        mPdfViewCtrl.invalidate();  // Always needed to draw away the previous loupe even if there is not any selection.
        animateLoupe(false);

        return skipOnUpPriorEvent(priorEventMode);
    }

    /**
     * The overload implementation of {@link Tool#onConfigurationChanged(Configuration)}.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (mPdfViewCtrl.hasSelection()) {
            mNextToolMode = getToolMode();
        } else {
            exitCurrentMode();
        }
    }

    /**
     * The overload implementation of {@link Tool#onClose()}.
     */
    @Override
    public void onClose() {
        closeQuickMenu();
        if (mTTS != null) {
            mTTS.stop();
        }
    }

    @Override
    protected boolean canDrawLoupe() {
        // show loupe either when being long pressed or a selection widget is effective
        return !mDrawingLoupe && // prevent recursive calling
                (mEffSelWidgetId >= 0 || mBeingLongPressed);
    }

    @Override
    protected int getLoupeType() {
        return LOUPE_TYPE_TEXT;
    }

    private boolean hasSelection() {
        boolean hasSelectionForReal = false;
        if (mPdfViewCtrl.hasSelection()) {
            // CORE BUG workaround
            // hasSelection can be true even if no selection
            try {
                int sel_pg_begin = mPdfViewCtrl.getSelectionBeginPage();
                int sel_pg_end = mPdfViewCtrl.getSelectionEndPage();
                for (int pg = sel_pg_begin; pg <= sel_pg_end; ++pg) {
                    PDFViewCtrl.Selection sel = mPdfViewCtrl.getSelection(pg);
                    double[] quads = sel.getQuads();
                    if (quads.length != 0) {
                        hasSelectionForReal = true;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return hasSelectionForReal;
    }

    public void exitCurrentMode() {
        // deselect text
        mNextToolMode = mCurrentDefaultToolMode;
        mPdfViewCtrl.clearSelection();
        mEffSelWidgetId = -1;
        mSelWidgetEnabled = false;
        if (!mSelPath.isEmpty()) {
            // Clear the path data for highlighting
            mSelPath.reset();
            mPdfViewCtrl.invalidate();
        }
        animateLoupe(false);
    }

    /**
     * Selects text in the specified rectangle.
     *
     * @param x1     The x coordinate at one of the end point of the rectangle
     * @param y1     The y coordinate at one of the end point of the rectangle
     * @param x2     The x coordinate at another point
     * @param y2     The y coordinate at another point
     * @param byRect True if should select by rectangle
     * @param bySnap True if should select by struct with smart snapping
     *               Note: if both byRect and bySnap are false then select by structure
     * @return True if some text was selected
     */
    protected void selectText(float x1, float y1, float x2, float y2, boolean byRect, boolean bySnap) {
        if (byRect) {
            RectF textSelectRect = getTextSelectRect(x2, y2);
            x1 = textSelectRect.left;
            y1 = textSelectRect.top;
            x2 = textSelectRect.right;
            y2 = textSelectRect.bottom;
        }

        // Clear pre-selected content

        boolean had_sel = !mSelPath.isEmpty();
        mSelPath.reset();

        // Select text
        boolean shouldUnlockRead = false;
        try {
            // Locks the document first as accessing annotation/doc information isn't thread safe.
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            if (byRect) {
                mPdfViewCtrl.selectByRect(x1, y1, x2, y2);
            } else if (bySnap) {
                mPdfViewCtrl.selectByStructWithSmartSnapping(x1, y1, x2, y2);
            } else {
                mPdfViewCtrl.setTextSelectionMode(PDFViewCtrl.TextSelectionMode.STRUCTURAL);
                mPdfViewCtrl.select(x1, y1, x2, y2);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }

        // Update the bounding box that should include:
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
    }

    /**
     * The overload implementation of {@link Tool#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        super.onMove(e1, e2, x_dist, y_dist);

        // Detect trackpad scrolling
        if (ShortcutHelper.isTextSelect(e2) && 0 == Float.compare(0.0f, e2.getPressure(0))) {
            mNextToolMode = ToolMode.PAN;
            return false;
        }

        float sx = mPdfViewCtrl.getScrollX();
        float sy = mPdfViewCtrl.getScrollY();

        if ((ShortcutHelper.isTextSelect(e2) && getToolMode() == ToolMode.TEXT_SELECT) || mEffSelWidgetId >= 0) {
            // Structural selection
            selectText(mStationPt.x - sx, mStationPt.y - sy, e2.getX(), e2.getY(), false, true);
            mPdfViewCtrl.invalidate(mInvalidateBBox);
            return true;
        } else {
            if (mBeingLongPressed) {
                // Select single word using rectangular selection
                selectText(0, 0, e2.getX(), e2.getY(), true, false);
                mPdfViewCtrl.invalidate(mInvalidateBBox);
                return true;
            } else {
                showTransientPageNumber();
                return false;   // Just scroll
            }
        }
    }

    /**
     * The overload implementation of {@link Tool#onLayout(boolean, int, int, int, int)}.
     */
    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (mPdfViewCtrl.hasSelection()) {
            // After relayout, need to adjust the positions of the selection and menu;
            // but, if page presentation has changed from continuous to non-continuous, just
            // in case the selection crosses pages, remove all the text selection.
            if (mPdfViewCtrl.isContinuousPagePresentationMode(mPagePresModeWhileSelected) &&
                    !mPdfViewCtrl.isContinuousPagePresentationMode(mPdfViewCtrl.getPagePresentationMode())) {
                mPdfViewCtrl.clearSelection();
                closeQuickMenu();
                mSelPath.reset();
                mNextToolMode = ToolMode.PAN;

                return;
            }

            mSelPath.reset();
            populateSelectionResult();
            mPdfViewCtrl.invalidate();

            if (isQuickMenuShown()) {
                closeQuickMenu();
                showMenu(getQMAnchorRect());
            }
        }
    }

    /**
     * The overload implementation of {@link Tool#onLongPress(MotionEvent)}.
     */
    @Override
    public boolean onLongPress(MotionEvent e) {
        mNextToolMode = ToolMode.TEXT_SELECT;
        mBeingLongPressed = true;
        mBeingPressed = true;
        mLoupeEnabled = true;

        float sx = mPdfViewCtrl.getScrollX();
        float sy = mPdfViewCtrl.getScrollY();
        mPressedPoint.x = e.getX() + sx;
        mPressedPoint.y = e.getY() + sy;

        if (mEffSelWidgetId < 0) {
            // If there is no effective selection widget, select a single word.
            // Need to clear the existing selection info first.
            mEffSelWidgetId = -1;
            mSelWidgetEnabled = false;
            selectText(0, 0, e.getX(), e.getY(), true, false);
            mPdfViewCtrl.invalidate(mInvalidateBBox);
            animateLoupe(true);

            if (!mPdfViewCtrl.hasSelection()) {
                // Nothing selected, go back to pan mode.
                animateLoupe(false);
                mNextToolMode = ToolMode.PAN;
            }
        } // else Do nothing and wait for onMove event

        return false;
    }

    /**
     * The overload implementation of {@link Tool#onSingleTapConfirmed(MotionEvent)}.
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        super.onSingleTapConfirmed(e);

        exitCurrentMode();

        return false;
    }

    /**
     * The overload implementation of {@link Tool#onPageTurning(int, int)}.
     */
    @Override
    public void onPageTurning(int old_page, int cur_page) {
        super.onPageTurning(old_page, cur_page);

        // in non-continuous mode, if page changed, deselect
        if (!mPdfViewCtrl.isContinuousPagePresentationMode(mPdfViewCtrl.getPagePresentationMode())) {
            if (mPdfViewCtrl.hasSelection()) {
                exitCurrentMode();
            }
        }
    }

    /**
     * The overload implementation of {@link Tool#onNightModeUpdated(boolean)}.
     */
    @Override
    public void onNightModeUpdated(boolean isNightMode) {
        if (mIsNightMode != isNightMode) {
            // Night has changed - update blend mode
            if (isNightMode) {
                mBlendmode = new PorterDuffXfermode(PorterDuff.Mode.SCREEN);
            } else {
                mBlendmode = new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY);
            }
        }
        mIsNightMode = isNightMode;
    }

    /**
     * Test if one of the two selection widgets is hit; if
     * so, return the selection widget id. during the onMove()
     * gesture afterwards, we can select properly.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @return The selected widget ID
     */
    public int hitTest(float x, float y) {
        float dist = -1;
        int id = -1;
        for (int i = 0; i < 2; ++i) {
            PointF mPt = i == 0 ? mSelWidgets[i].mStrPt : mSelWidgets[i].mEndPt;
            float s = x - mPt.x;
            float t = y - mPt.y;
            float d = (float) Math.sqrt(s * s + t * t);
            if (d < mTSWidgetRadius * 4) {
                if (dist < 0 || dist > d) {
                    dist = d;
                    id = i;
                }
            }
        }
        return id;
    }

    /**
     * Populates selection result.
     */
    protected void populateSelectionResult() {
        float sx = mPdfViewCtrl.getScrollX();
        float sy = mPdfViewCtrl.getScrollY();
        int sel_pg_begin = mPdfViewCtrl.getSelectionBeginPage();
        int sel_pg_end = mPdfViewCtrl.getSelectionEndPage();
        float min_x = 1E10f, min_y = 1E10f, max_x = 0, max_y = 0;
        boolean has_sel = false;

        try {
            com.pdftron.pdf.Rect firstQuad = getFirstQuad();
            com.pdftron.pdf.Rect lastQuad = getLastQuad();
            firstQuad = convertFromPageRectToScreenRect(firstQuad, sel_pg_begin);
            lastQuad = convertFromPageRectToScreenRect(lastQuad, sel_pg_end);

            if (firstQuad == null || lastQuad == null) {
                return;
            }

            firstQuad.normalize();
            lastQuad.normalize();

            mSelWidgets[0].mStrPt.set((float) firstQuad.getX1() - mTSWidgetThickness / 2, (float) firstQuad.getY2()); // bottom point
            mSelWidgets[1].mEndPt.set((float) lastQuad.getX2() + mTSWidgetThickness / 2, (float) lastQuad.getY2()); // bottom point

            if (mIsRightToLeft) {
                mSelWidgets[0].mStrPt.x = (float) firstQuad.getX2() - mTSWidgetThickness / 2; // bottom point
                mSelWidgets[1].mEndPt.x = (float) lastQuad.getX1() + mTSWidgetThickness / 2; // bottom point
            }

            mSelWidgets[0].mEndPt.set(mSelWidgets[0].mStrPt.x, (float) (mSelWidgets[0].mStrPt.y - firstQuad.getHeight())); // top point
            mSelWidgets[1].mStrPt.set(mSelWidgets[1].mEndPt.x, (float) (mSelWidgets[1].mEndPt.y - lastQuad.getHeight())); // top point
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        // Loop through the pages that have text selection, and construct 'mSelPath' for
        // highlighting.
        // NOTE: Android has a bug that if hardware acceleration is turned on and the path is too
        // big, it may not get rendered. See http://code.google.com/p/android/issues/detail?id=24023
        for (int pg = sel_pg_begin; pg <= sel_pg_end; ++pg) {
            PDFViewCtrl.Selection sel = mPdfViewCtrl.getSelection(pg);  // Each Selection may have multiple quads
            double[] quads = sel.getQuads();
            double[] pts;
            int sz = quads.length / 8;  // Each quad has eight numbers (x0, y0), ... (x3, y3)

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
                mTempRect.left = x;
                mTempRect.bottom = y;
                mQuadPoints[0].x = x;
                mQuadPoints[0].y = y;
                min_x = min_x > x ? x : min_x;
                max_x = max_x < x ? x : max_x;
                min_y = min_y > y ? y : min_y;
                max_y = max_y < y ? y : max_y;

                if (pg == sel_pg_begin && i == 0) {
                    // Set the start point of the first selection widget that is based
                    // on the first quad point.
                    //mSelWidgets[0].mStrPt.set(x-mTSWidgetThickness/2, y);
                    x -= mTSWidgetThickness + mTSWidgetRadius;
                    min_x = min_x > x ? x : min_x;
                    max_x = max_x < x ? x : max_x;
                }

                pts = mPdfViewCtrl.convPagePtToScreenPt(quads[k + 2], quads[k + 3], pg);
                x = (float) pts[0] + sx;
                y = (float) pts[1] + sy;
                mTempRect.right = x;
                mQuadPoints[1].x = x;
                mQuadPoints[1].y = y;
                min_x = min_x > x ? x : min_x;
                max_x = max_x < x ? x : max_x;
                min_y = min_y > y ? y : min_y;
                max_y = max_y < y ? y : max_y;

                if (pg == sel_pg_end && i == sz - 1) {
                    // Set the end point of the second selection widget that is based
                    // on the last quad point.
                    //mSelWidgets[1].mEndPt.set(x+mTSWidgetThickness/2, y);
                    x += mTSWidgetThickness + mTSWidgetRadius;
                    y += mTSWidgetRadius * 2;
                    min_x = min_x > x ? x : min_x;
                    max_x = max_x < x ? x : max_x;
                    min_y = min_y > y ? y : min_y;
                    max_y = max_y < y ? y : max_y;
                }

                pts = mPdfViewCtrl.convPagePtToScreenPt(quads[k + 4], quads[k + 5], pg);
                x = (float) pts[0] + sx;
                y = (float) pts[1] + sy;
                mTempRect.top = y;
                mQuadPoints[2].x = x;
                mQuadPoints[2].y = y;
                min_x = min_x > x ? x : min_x;
                max_x = max_x < x ? x : max_x;
                min_y = min_y > y ? y : min_y;
                max_y = max_y < y ? y : max_y;

                if (pg == sel_pg_end && i == sz - 1) {
                    // Set the start point of the second selection widget that is based
                    // on the last quad point.
                    //mSelWidgets[1].mStrPt.set(x+mTSWidgetThickness/2, y);
                    x += mTSWidgetThickness + mTSWidgetRadius;
                    min_x = min_x > x ? x : min_x;
                    max_x = max_x < x ? x : max_x;
                }

                pts = mPdfViewCtrl.convPagePtToScreenPt(quads[k + 6], quads[k + 7], pg);
                x = (float) pts[0] + sx;
                y = (float) pts[1] + sy;
                mQuadPoints[3].x = x;
                mQuadPoints[3].y = y;
                min_x = min_x > x ? x : min_x;
                max_x = max_x < x ? x : max_x;
                min_y = min_y > y ? y : min_y;
                max_y = max_y < y ? y : max_y;

                if (pg == sel_pg_begin && i == 0) {
                    // Set the end point of the first selection widget that is based
                    // on the first quad point.
                    //mSelWidgets[0].mEndPt.set(x-mTSWidgetThickness/2, y);
                    x -= mTSWidgetThickness + mTSWidgetRadius;
                    y -= mTSWidgetRadius * 2;
                    min_x = min_x > x ? x : min_x;
                    max_x = max_x < x ? x : max_x;
                    min_y = min_y > y ? y : min_y;
                    max_y = max_y < y ? y : max_y;
                }

                try {
                    Page page1 = mPdfViewCtrl.getDoc().getPage(pg);
                    if (page1 != null && (page1.getRotation() == Page.e_90 ||
                            page1.getRotation() == Page.e_270 ||
                            mPdfViewCtrl.getPageRotation() == Page.e_90 ||
                            mPdfViewCtrl.getPageRotation() == Page.e_270)) {
                        // flip the X and Y for rotated Page
                        mTempRotationRect.setX1(mQuadPoints[0].x);
                        mTempRotationRect.setY1(mQuadPoints[0].y);
                        mTempRotationRect.setX2(mQuadPoints[2].x);
                        mTempRotationRect.setY2(mQuadPoints[2].y);
                    } else {
                        mTempRotationRect.setX1(mQuadPoints[0].x);
                        mTempRotationRect.setY1(mQuadPoints[0].y);
                        mTempRotationRect.setX2(mQuadPoints[2].x);
                        mTempRotationRect.setY2(mQuadPoints[3].y);
                    }
                    mTempRotationRect.normalize();

                    mTempRect.left = (float) mTempRotationRect.getX1();
                    mTempRect.top = (float) mTempRotationRect.getY1();
                    mTempRect.right = (float) mTempRotationRect.getX2();
                    mTempRect.bottom = (float) mTempRotationRect.getY2();
                } catch (PDFNetException e) {
                    mTempRect.left = mQuadPoints[0].x;
                    mTempRect.top = mQuadPoints[2].y;
                    mTempRect.right = mQuadPoints[1].x;
                    mTempRect.bottom = mQuadPoints[0].y;
                }
                mSelPath.addRect(mTempRect, Path.Direction.CW); // TODO: one quad per path
            }
        }

        if (has_sel) {
            mSelBBox.set(min_x, min_y, max_x, max_y);
            mSelBBox.round(mInvalidateBBox);
        }
    }

    /**
     * @return The first quad rectangle
     */
    protected com.pdftron.pdf.Rect getFirstQuad() {
        int sel_pg_begin = mPdfViewCtrl.getSelectionBeginPage();
        int sel_pg_end = mPdfViewCtrl.getSelectionEndPage();
        for (int pg = sel_pg_begin; pg <= sel_pg_end; ++pg) {
            PDFViewCtrl.Selection sel = mPdfViewCtrl.getSelection(pg);  // Each Selection may have multiple quads
            double[] quads = sel.getQuads();
            int sz = quads.length / 8;  // Each quad has eight numbers (x0, y0), ... (x3, y3)

            if (sz > 0) {
                try {
                    return new com.pdftron.pdf.Rect(quads[0], quads[1], quads[4], quads[5]);
                } catch (PDFNetException e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * @return The last quad rectangle
     */
    protected com.pdftron.pdf.Rect getLastQuad() {
        int sel_pg_begin = mPdfViewCtrl.getSelectionBeginPage();
        int sel_pg_end = mPdfViewCtrl.getSelectionEndPage();
        for (int pg = sel_pg_end; pg >= sel_pg_begin; --pg) {
            PDFViewCtrl.Selection sel = mPdfViewCtrl.getSelection(pg);  // Each Selection may have multiple quads
            double[] quads = sel.getQuads();
            int sz = quads.length / 8;  // Each quad has eight numbers (x0, y0), ... (x3, y3)
            if (sz > 0) {
                try {
                    return new com.pdftron.pdf.Rect(quads[quads.length - 8], quads[quads.length - 7], quads[quads.length - 4], quads[quads.length - 3]);
                } catch (PDFNetException e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * The overload implementation of {@link Tool#onQuickMenuClicked(QuickMenuItem)}.
     */
    @SuppressWarnings("deprecation")
    @Override
    public boolean onQuickMenuClicked(QuickMenuItem menuItem) {
        if (super.onQuickMenuClicked(menuItem)) {
            return true;
        }

        if (mPdfViewCtrl.hasSelection()) {
            String text = ViewerUtils.getSelectedString(mPdfViewCtrl);
            if (menuItem.getItemId() == R.id.qm_define || menuItem.getItemId() == R.id.qm_translate) {
                // get anchor point
                RectF anchor;
                if (this instanceof AnnotEditTextMarkup) {
                    anchor = getAnnotRect();
                } else {
                    int sx = mPdfViewCtrl.getScrollX();
                    int sy = mPdfViewCtrl.getScrollY();
                    RectF selectBox = new RectF(mSelBBox.left - sx, mSelBBox.top - sy, mSelBBox.right - sx, mSelBBox.bottom - sy);
                    anchor = calculateQMAnchor(selectBox);
                }

                // is define or translate
                boolean isDefine = menuItem.getItemId() == R.id.qm_define;

                ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                toolManager.defineTranslateSelected(text, anchor, isDefine);

                exitCurrentMode();
                return true;
            } else if (menuItem.getItemId() == R.id.qm_share) {
                String subject = mPdfViewCtrl.getContext().getResources().getString(R.string.empty_title);
                try {
                    File file = new File(mPdfViewCtrl.getDoc().getFileName());
                    subject = mPdfViewCtrl.getContext().getResources().getString(R.string.tools_share_subject) + " " + file.getName();
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }

                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                sharingIntent.putExtra(Intent.EXTRA_TEXT, text);
                mPdfViewCtrl.getContext().startActivity(Intent.createChooser(sharingIntent, mPdfViewCtrl.getContext().getResources().getString(R.string.tools_share_title)));

                exitCurrentMode();

                return true;
            } else if (menuItem.getItemId() == R.id.qm_copy) {
                copyAnnot(text, mPdfViewCtrl.getContext().getResources().getString(R.string.tools_copy_confirmation));
                exitCurrentMode();
                return true;
            } else if (menuItem.getItemId() == R.id.qm_search) {
                Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                intent.putExtra(SearchManager.QUERY, text); // query contains search string

                try {
                    mPdfViewCtrl.getContext().startActivity(intent);
                } catch (ActivityNotFoundException ignored) {
                    // This exception gets thrown if Google Search has been deactivated
                }
                exitCurrentMode();
                return true;
            } else if (menuItem.getItemId() == R.id.qm_highlight) {
                mNextToolMode = ToolMode.TEXT_HIGHLIGHT;
            } else if (menuItem.getItemId() == R.id.qm_underline) {
                mNextToolMode = ToolMode.TEXT_UNDERLINE;
            } else if (menuItem.getItemId() == R.id.qm_squiggly) {
                mNextToolMode = ToolMode.TEXT_SQUIGGLY;
            } else if (menuItem.getItemId() == R.id.qm_strikeout) {
                mNextToolMode = ToolMode.TEXT_STRIKEOUT;
            } else if (menuItem.getItemId() == R.id.qm_link) {
                mNextToolMode = ToolMode.TEXT_LINK_CREATE;
            } else if (menuItem.getItemId() == R.id.qm_redaction) {
                mNextToolMode = ToolMode.TEXT_REDACTION;
            } else if (menuItem.getItemId() == R.id.qm_tts) {
                // check if volume is on mute, if so display toast telling user
                AudioManager audio = (AudioManager) mPdfViewCtrl.getContext().getSystemService(Context.AUDIO_SERVICE);
                int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
                if (currentVolume == 0) {
                    CommonToast.showText(mPdfViewCtrl.getContext(), mPdfViewCtrl.getContext().getString(R.string.text_to_speech_mute_volume), Toast.LENGTH_SHORT);
                }
                try {
                    mTTS = ((ToolManager) mPdfViewCtrl.getToolManager()).getTTS();
                    if (mTTS != null) {
                        mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);
                    } else {
                        final String finalText = text;
                        mTTS = new TextToSpeech(mPdfViewCtrl.getContext(), new TextToSpeech.OnInitListener() {
                            @Override
                            public void onInit(int status) {
                                try {
                                    if (status == TextToSpeech.SUCCESS) {
                                        mTTS.speak(finalText, TextToSpeech.QUEUE_FLUSH, null);
                                    } else {
                                        CommonToast.showText(mPdfViewCtrl.getContext(), mPdfViewCtrl.getContext().getString(R.string.error_text_to_speech), Toast.LENGTH_SHORT);
                                    }
                                } catch (Exception e) {
                                    Utils.showAlertDialogWithLink(mPdfViewCtrl.getContext(), mPdfViewCtrl.getContext().getResources().getString(R.string.error_thrown_text_to_speech), "");
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    CommonToast.showText(mPdfViewCtrl.getContext(), mPdfViewCtrl.getContext().getString(R.string.error_text_to_speech), Toast.LENGTH_SHORT);
                }
                return true;
            } else if (menuItem.getItemId() == R.id.qm_edit) {
                final String textWithNewLine = StringUtils.chomp(ViewerUtils.getSelectedString(mPdfViewCtrl, true));
                mDialogAnnotNote = new DialogAnnotNote(mPdfViewCtrl, textWithNewLine.trim(), true);
                mDialogAnnotNote.setNegativeButtonRes(R.string.cancel);
                mDialogAnnotNote.setAnnotNoteListener(new DialogAnnotNote.DialogAnnotNoteListener() {
                    @Override
                    public void onAnnotButtonPressed(int button) {
                        mAnnotButtonPressed = button;
                    }
                });
                // set buttons
                mDialogAnnotNote.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        if (mAnnotButtonPressed == DialogInterface.BUTTON_POSITIVE) {
                            String newContent = mDialogAnnotNote.getNote();
                            if (!textWithNewLine.equals(newContent)) {
                                editText(newContent);
                            }
                        }
                        exitCurrentMode();
                    }
                });
                mDialogAnnotNote.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {

                    }
                });

                mDialogAnnotNote.show();
            }
        }
        return false;
    }

    @Override
    public boolean showMenu(RectF anchor_rect) {
        if (onInterceptAnnotationHandling(mAnnot)) {
            return true;
        }

        if (mPdfViewCtrl == null) {
            return false;
        }

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        return toolManager != null && !toolManager.isQuickMenuDisabled() && super.showMenu(anchor_rect);
    }

    private void copyAnnot(String text, String toastMsg) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) mPdfViewCtrl.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("text", text);
            clipboard.setPrimaryClip(clip);
        }
        if (!Utils.isNullOrEmpty(toastMsg)) {
            CommonToast.showText(mPdfViewCtrl.getContext(), toastMsg, Toast.LENGTH_SHORT);
        }
    }

    /**
     * The overload implementation of {@link Tool#onDraw(Canvas, Matrix)}.
     */
    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        if (!mDrawingLoupe) {
            super.onDraw(canvas, tfm);
        }

        if (mPdfViewCtrl.isSlidingWhileZoomed()) {
            return;
        }

        drawLoupe();

        // Draw the highlight quads
        if (!mSelPath.isEmpty()) {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setXfermode(mBlendmode);
            mPaint.setColor(mSelColor);
            canvas.drawPath(mSelPath, mPaint);
            mPaint.setXfermode(null); // Reset mode

            // Draw the two selection widgets
            if (mSelWidgetEnabled) {
                mPaint.setColor(mPdfViewCtrl.getResources().getColor(R.color.fab_light_blue));
                //mPaint.setAnnotStyle(Paint.Style.STROKE);
                //mPaint.setStrokeWidth(mTSWidgetThickness);

                float x1 = mSelWidgets[0].mStrPt.x;
                float y1 = mSelWidgets[0].mStrPt.y;
                if (mHasSelectionPermission) {
                    //canvas.drawLine(x1, y1, x2, y2, mPaint);
                    mPaint.setStyle(Paint.Style.FILL);

                    BitmapDrawable drawable = (BitmapDrawable) mPdfViewCtrl.getContext()
                            .getResources().getDrawable(R.drawable.text_select_handle_left);
                    Bitmap handle = drawable.getBitmap();

                    Rect locRect = new Rect((int) (x1 - 2 * mTSWidgetRadius), (int) y1, (int) x1, (int) (y1 + 2 * mTSWidgetRadius));

                    canvas.drawBitmap(handle, null, locRect, mPaint);
                }

                float x2 = mSelWidgets[1].mEndPt.x;
                float y2 = mSelWidgets[1].mEndPt.y;
                if (mHasSelectionPermission) {

                    mPaint.setStyle(Paint.Style.FILL);

                    BitmapDrawable drawable = (BitmapDrawable) mPdfViewCtrl.getContext()
                            .getResources().getDrawable(R.drawable.text_select_handle_right);
                    Bitmap handle = drawable.getBitmap();

                    Rect locRect = new Rect((int) x2, (int) y2, (int) (x2 + 2 * mTSWidgetRadius), (int) (y2 + 2 * mTSWidgetRadius));

                    canvas.drawBitmap(handle, null, locRect, mPaint);
                }
            }
        }
    }

    // The selection widget at the two ends of the selected text.
    class SelWidget {
        PointF mStrPt;
        PointF mEndPt;

        SelWidget() {
        }
    }

    /**
     * The overload implementation of {@link Tool#getModeAHLabel()}.
     */
    @Override
    protected int getModeAHLabel() {
        return AnalyticsHandlerAdapter.LABEL_QM_TEXTSELECT;
    }

    private RectF getQMAnchorRect() {
        RectF annotRect = mSelBBox;
        if (annotRect == null) {
            return null;
        }
        int sx = mPdfViewCtrl.getScrollX();
        int sy = mPdfViewCtrl.getScrollY();
        return new RectF(annotRect.left - sx, annotRect.top + 2 * mTSWidgetRadius - sy, annotRect.right - sx, annotRect.bottom - sy);
    }

    /**
     * Resets selection.
     */
    public void resetSelection() {
        if (hasSelection()) {
            closeQuickMenu();
            mSelPath.reset();
            populateSelectionResult();
            showMenu(getQMAnchorRect());
        }
        mPdfViewCtrl.invalidate();  // Always needed to draw away the previous loupe even if there is not any selection.
    }

    /**
     * Clears selection.
     */
    public void clearSelection() {
        mSelPath.reset();
        mPdfViewCtrl.invalidate();
    }

    /**
     * @return {@link AnalyticsHandlerAdapter#QUICK_MENU_TYPE_TEXT_SELECT}
     */
    @Override
    protected int getQuickMenuAnalyticType() {
        return AnalyticsHandlerAdapter.QUICK_MENU_TYPE_TEXT_SELECT;
    }

    static class ReplacerInfo {
        int page;
        com.pdftron.pdf.Rect rect;
        String text;

        public ReplacerInfo(int page, com.pdftron.pdf.Rect rect, String text) {
            this.page = page;
            this.rect = rect;
            this.text = text;
        }
    }

    /**
     * Replace selected PDF text with new content
     *
     * @param newContent the new content
     */
    public void editText(String newContent) {
        String[] lines = newContent.split("\\r?\\n");

        if (mPdfViewCtrl.hasSelection() && lines.length > 0) {
            int sel_pg_begin = mPdfViewCtrl.getSelectionBeginPage();
            int sel_pg_end = mPdfViewCtrl.getSelectionEndPage();
            boolean shouldUnlock = false;
            ContentReplacer replacer = null;
            try {
                mPdfViewCtrl.docLock(true);
                shouldUnlock = true;

                replacer = new ContentReplacer();

                SparseArray<ArrayList<ReplacerInfo>> replacerInfo = new SparseArray<>();

                for (int pg = sel_pg_begin; pg <= sel_pg_end; ++pg) {
                    PDFViewCtrl.Selection sel = mPdfViewCtrl.getSelection(pg);

                    ArrayList<ReplacerInfo> infos = new ArrayList<>();

                    double[] quads = sel.getQuads();
                    int sz = quads.length / 8;  // Each quad has eight numbers (x0, y0), ... (x3, y3)

                    if (sz == 0) {
                        continue;
                    }
                    int lineIndex = 0;
                    int k = 0;
                    for (int i = 0; i < sz; ++i, k += 8) {
                        double[] xVals = new double[]{
                                quads[k],
                                quads[k + 2],
                                quads[k + 4],
                                quads[k + 6]
                        };
                        Arrays.sort(xVals);
                        double[] yVals = new double[]{
                                quads[k + 1],
                                quads[k + 3],
                                quads[k + 5],
                                quads[k + 7]
                        };
                        Arrays.sort(yVals);

                        com.pdftron.pdf.Rect quadRect = new com.pdftron.pdf.Rect(xVals[0], yVals[0], xVals[xVals.length - 1], yVals[yVals.length - 1]);
                        quadRect.normalize();

                        if (lineIndex < lines.length) {
                            infos.add(new ReplacerInfo(pg, quadRect, lines[lineIndex++]));
                        } else {
                            infos.add(new ReplacerInfo(pg, quadRect, ""));
                        }
                    }
                    replacerInfo.put(pg, infos);
                }
                // clear selection cache
                mPdfViewCtrl.clearSelection();

                for (int i = 0; i < replacerInfo.size(); i++) {
                    int pg = replacerInfo.keyAt(i);
                    ArrayList<ReplacerInfo> infos = replacerInfo.get(pg);
                    for (ReplacerInfo info : infos) {
                        replacer.addText(info.rect, info.text);
                    }
                    Page page = mPdfViewCtrl.getDoc().getPage(pg);
                    replacer.process(page);
                }

                mPdfViewCtrl.update(true);

                ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                toolManager.raisePdfTextModified();
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    mPdfViewCtrl.docUnlock();
                }
                if (replacer != null) {
                    replacer.destroy();
                }
            }
        }
    }
}
