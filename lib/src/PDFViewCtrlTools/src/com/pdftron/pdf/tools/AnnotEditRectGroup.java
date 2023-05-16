//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Bundle;
import android.util.Pair;
import android.view.MotionEvent;
import android.widget.Toast;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.StrokeOutlineBuilder;
import com.pdftron.pdf.annots.Ink;
import com.pdftron.pdf.controls.AnnotStyleDialogFragment;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.LineEndingStyle;
import com.pdftron.pdf.model.LineStyle;
import com.pdftron.pdf.model.RulerItem;
import com.pdftron.pdf.model.ShapeBorderStyle;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.AnnotationClipboardHelper;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.DrawingUtils;
import com.pdftron.pdf.utils.PathPool;
import com.pdftron.pdf.utils.PressureInkUtils;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.sdf.Obj;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is for selecting a group of annotations
 */
@Keep
public class AnnotEditRectGroup extends AnnotEdit {

    private static final String TAG = AnnotEditRectGroup.class.getName();

    public enum SelectionMode {
        RECTANGULAR,
        LASSO
    }

    private SelectionMode mSelectionMode = SelectionMode.RECTANGULAR;

    private final HashMap<Annot, Integer> mSelectedAnnotsMap = new HashMap<>();
    private final HashMap<Annot, Integer> mPostRemoveAnnotsMap = new HashMap<>();
    // Touch-down point and moving point:
    protected PointF mPt1 = new PointF(0, 0);
    protected PointF mPt2 = new PointF(0, 0);
    private final Paint mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mLassoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF mSelectedArea = new RectF();
    private final RectF mSelectedAreaPageSpace = new RectF();
    private int mDownPageNum;
    private boolean mResizeAnnots;
    private boolean mGroupSelected; // whether selected annotations are from one group

    private boolean mCanCopy = true;
    private boolean mCanResize = true;
    private boolean mCanGroup = true;

    // lasso
    private final Path mPath = new Path();
    private final Path mScreenPath = new Path();
    private float mMinX;
    private float mMinY;
    private float mMaxX;
    private float mMaxY;

    private final StrokeLassoHelper strokeLassoHelper = new StrokeLassoHelper();

    /**
     * Class constructor
     */
    public AnnotEditRectGroup(@NonNull PDFViewCtrl ctrl) {
        this(ctrl, SelectionMode.RECTANGULAR);
    }

    /**
     * Class constructor
     */
    public AnnotEditRectGroup(@NonNull PDFViewCtrl ctrl, @NonNull SelectionMode selectionMode) {
        super(ctrl);
        mNextToolMode = getToolMode();
        mSelectionMode = selectionMode;
        mFillPaint.setStyle(Paint.Style.FILL);
        mLassoPaint.setStyle(Paint.Style.STROKE);

        TypedArray a = ctrl.getContext().obtainStyledAttributes(null, R.styleable.RectGroupAnnotEdit, R.attr.rect_group_annot_edit_style, R.style.RectGroupAnnotEdit);
        try {
            int color = a.getColor(R.styleable.RectGroupAnnotEdit_fillColor, Color.BLUE);
            float opacity = a.getFloat(R.styleable.RectGroupAnnotEdit_fillOpacity, 0.38f);
            mFillPaint.setColor(color);
            mFillPaint.setAlpha((int) (opacity * 255));

            int lassoColor = a.getColor(R.styleable.RectGroupAnnotEdit_lassoColor, Color.BLUE);
            float lassoOpacity = a.getFloat(R.styleable.RectGroupAnnotEdit_lassoOpacity, 1f);
            float lassoThickness = a.getFloat(R.styleable.RectGroupAnnotEdit_lassoThickness, 2f);
            mLassoPaint.setColor(lassoColor);
            mLassoPaint.setAlpha((int) (lassoOpacity * 255));
            mLassoPaint.setStrokeWidth(this.convDp2Pix(lassoThickness));
            mLassoPaint.setPathEffect(new DashPathEffect(new float[]{this.convDp2Pix(4.5f), this.convDp2Pix(2.5f)}, 0));
        } finally {
            a.recycle();
        }
        mSelectionBoxMargin = 0;
        mCtrlRadius = this.convDp2Pix(7.5f);
    }

    public void setSelectionMode(@NonNull SelectionMode mode) {
        mSelectionMode = mode;
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.ANNOT_EDIT_RECT_GROUP;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        super.onDown(e);
        if (mEffCtrlPtId == e_unknown) {
            // The first touch-down point
            mPt1.x = e.getX() + mPdfViewCtrl.getScrollX();
            mPt1.y = e.getY() + mPdfViewCtrl.getScrollY();

            // Remembers which page is touched initially and that is the page where
            // the annotation is going to reside on.
            mDownPageNum = mPdfViewCtrl.getPageNumberFromScreenPt(e.getX(), e.getY());
            if (mDownPageNum < 1) {
                mDownPageNum = mPdfViewCtrl.getCurrentPage();
            }

            // The moving point that is the same with the touch-down point initiallyAnnotationTool
            mPt2.set(mPt1);
            mResizeAnnots = false;
            mSelectedArea.setEmpty();
            mSelectedAreaPageSpace.setEmpty();

            // for lasso
            if (SelectionMode.LASSO == mSelectionMode) {
                mPath.reset();
                mPath.moveTo(mPt1.x, mPt1.y);
                mScreenPath.reset();
                mScreenPath.moveTo(e.getX(), e.getY());
                mMinX = mMaxX = mPt1.x;
                mMinY = mMaxY = mPt1.y;
                double[] pgPt = mPdfViewCtrl.convScreenPtToPagePt(e.getX(), e.getY());
                strokeLassoHelper.resetPath();
                strokeLassoHelper.pathMoveTo((float) pgPt[0], (float) pgPt[1]);
                strokeLassoHelper.initRect((float) pgPt[0], (float) pgPt[1]);
            }
        }
        if (mDownPageNum >= 1) {
            mPageCropOnClientF = Utils.buildPageBoundBoxOnClient(mPdfViewCtrl, mDownPageNum);
            Utils.snapPointToRect(mPt1, mPageCropOnClientF);
        }
        return false;
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

        super.onMove(e1, e2, x_dist, y_dist);
        if (mEffCtrlPtId != e_unknown) {
            mResizeAnnots = true;
            return true;
        }

        mAllowTwoFingerScroll = e1.getPointerCount() == 2 || e2.getPointerCount() == 2;

        // check to see whether use finger to scroll or not
        mAllowOneFingerScrollWithStylus = mStylusUsed && e2.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS;

        if (mAllowTwoFingerScroll || mAllowOneFingerScrollWithStylus) {
            mPdfViewCtrl.setBuiltInPageSlidingState(true);
        } else {
            mPdfViewCtrl.setBuiltInPageSlidingState(false);
        }

        if (mAllowTwoFingerScroll) {
            return false;
        }

        if (mAllowOneFingerScrollWithStylus) {
            return false;
        }

        // While moving, update the moving point so that a rubber band can be shown to
        // indicate the bounding box of the resulting annotation.
        float x = mPt2.x;
        float y = mPt2.y;
        mPt2.x = e2.getX() + mPdfViewCtrl.getScrollX();
        mPt2.y = e2.getY() + mPdfViewCtrl.getScrollY();

        // for lasso
        if (SelectionMode.LASSO == mSelectionMode) {
            mPath.lineTo(mPt2.x, mPt2.y);
            mScreenPath.lineTo(e2.getX(), e2.getY());
            double[] pgPt = mPdfViewCtrl.convScreenPtToPagePt(e2.getX(), e2.getY());
            strokeLassoHelper.pathLineTo((float) pgPt[0], (float) pgPt[1]);
            mMinX = Math.min(mMinX, mPt2.x);
            mMaxX = Math.max(mMaxX, mPt2.x);
            mMinY = Math.min(mMinY, mPt2.y);
            mMaxY = Math.max(mMaxY, mPt2.y);
            strokeLassoHelper.updateRect((float) pgPt[0], (float) pgPt[1]);
        }

        Utils.snapPointToRect(mPt2, mPageCropOnClientF);

        float min_x = Math.min(Math.min(x, mPt2.x), mPt1.x);
        float max_x = Math.max(Math.max(x, mPt2.x), mPt1.x);
        float min_y = Math.min(Math.min(y, mPt2.y), mPt1.y);
        float max_y = Math.max(Math.max(y, mPt2.y), mPt1.y);

        mPdfViewCtrl.invalidate((int) min_x, (int) min_y, (int) Math.ceil(max_x), (int) Math.ceil(max_y));
        return true;
    }

    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        // We are scrolling
        if (mAllowTwoFingerScroll) {
            doneTwoFingerScrolling();
            return false;
        }

        if (priorEventMode == PDFViewCtrl.PriorEventMode.PAGE_SLIDING) {
            return false;
        }

        if (hasAnnotSelected() && mResizeAnnots) {
            mResizeAnnots = false;
            return super.onUp(e, priorEventMode);
        }

        if (mPt1.equals(mPt2)) {
            return true;
        }

        // In stylus mode, ignore finger input
        mAllowOneFingerScrollWithStylus = mStylusUsed && e.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS;
        if (mAllowOneFingerScrollWithStylus) {
            return true;
        }

        if (!mSelectedArea.isEmpty()) {
            return skipOnUpPriorEvent(priorEventMode);
        }

        strokeLassoHelper.updateRegion();
        boolean shouldUnlockWrite = false;
        try {
            // for lasso
            Region lassoRegionScreen = null;
            if (SelectionMode.LASSO == mSelectionMode) {
                float x = e.getX() + mPdfViewCtrl.getScrollX();
                float y = e.getY() + mPdfViewCtrl.getScrollY();
                mPath.lineTo(x, y);
                mScreenPath.lineTo(e.getX(), e.getY());
                mMinX = Math.min(mMinX, x);
                mMaxX = Math.max(mMaxX, x);
                mMinY = Math.min(mMinY, y);
                mMaxY = Math.max(mMaxY, y);
                double[] pgPt = mPdfViewCtrl.convScreenPtToPagePt(e.getX(), e.getY());
                strokeLassoHelper.pathLineTo((float) pgPt[0], (float) pgPt[1]);
                strokeLassoHelper.updateRect((float) pgPt[0], (float) pgPt[1]);

                lassoRegionScreen = new Region();
                lassoRegionScreen.setPath(mScreenPath, new Region(
                        Math.round(mMinX - mPdfViewCtrl.getScrollX()),
                        Math.round(mMinY - mPdfViewCtrl.getScrollY()),
                        Math.round(mMaxX - mPdfViewCtrl.getScrollX()),
                        Math.round(mMaxY - mPdfViewCtrl.getScrollY())
                ));
            }

            float min_x = Math.min(mPt1.x, mPt2.x);
            float max_x = Math.max(mPt1.x, mPt2.x);
            float min_y = Math.min(mPt1.y, mPt2.y);
            float max_y = Math.max(mPt1.y, mPt2.y);
            mPt1.x = min_x;
            mPt1.y = min_y;
            mPt2.x = max_x;
            mPt2.y = max_y;

            com.pdftron.pdf.Rect pageRect = getShapeBBox();

            mPdfViewCtrl.docLock(true);
            shouldUnlockWrite = true;
            ArrayList<Annot> annotsInPage = mPdfViewCtrl.getAnnotationsOnPage(mDownPageNum);

            RectF selectedRectInPage = new RectF();
            mSelectedAnnotsMap.clear();
            mAnnotIsTextMarkup = false;
            // search for selected annotations in page
            if (pageRect != null) {
                boolean reCalculateBBox = false;
                Set<Annot> annotsGroupSet = new HashSet<>();
                for (Annot annot : annotsInPage) {
                    com.pdftron.pdf.Rect annotRect = mPdfViewCtrl.getPageRectForAnnot(annot, mDownPageNum);
                    annotRect.normalize();

                    Region regionScreenAnnot = null;
                    if (SelectionMode.LASSO == mSelectionMode) {
                        // region for the annot
                        com.pdftron.pdf.Rect annotScreenRect = mPdfViewCtrl.getScreenRectForAnnot(annot, mDownPageNum);
                        annotScreenRect.normalize();

                        float x1 = Math.round(annotScreenRect.getX1());
                        float x2 = Math.round(annotScreenRect.getX2());
                        float y1 = Math.round(annotScreenRect.getY1());
                        float y2 = Math.round(annotScreenRect.getY2());
                        regionScreenAnnot = new Region();
                        Path annotPath = new Path();
                        annotPath.moveTo(x1, y1);
                        annotPath.lineTo(x1, y2);
                        annotPath.lineTo(x2, y2);
                        annotPath.lineTo(x2, y1);
                        regionScreenAnnot.setPath(annotPath,
                                new Region(Math.round(x1), Math.round(y1),
                                        Math.round(x2), Math.round(y2)));
                    }
                    boolean intersectCondition = annotRect.intersectRect(pageRect, annotRect);
                    HashMap<Annot, Integer> newlyCreateInks = new HashMap<>();
                    if (regionScreenAnnot != null && lassoRegionScreen != null) { // non null if lasso
                        intersectCondition = regionScreenAnnot.op(lassoRegionScreen, Region.Op.INTERSECT);
                        if (intersectCondition && annot.getType() == Annot.e_Ink) { // if intersect, then we should try to select ink strokes
                            Ink ink = new Ink(annot);
                            Pair<Ink, Boolean> splitInfo = strokeLassoHelper.splitAndCreateNewInks(ink);
                            Ink newInk = splitInfo.first;
                            intersectCondition = splitInfo.second;
                            if (newInk != null) {
                                setAuthor(newInk);
                                newlyCreateInks.put(newInk, mDownPageNum);
                            }
                        }
                    }
                    if (hasPermission(annot, ANNOT_PERMISSION_SELECTION) && isAnnotSupportEdit(annot) && intersectCondition) {
                        if (!isAnnotSupportResize(annot)) {
                            mCanResize = false;
                        }
                        if (!isAnnotSupportMove(annot)) {
                            mAnnotIsTextMarkup = true;
                        }
                        if (!isAnnotSupportCopy(annot)) {
                            mCanCopy = false;
                            mCanGroup = false; // adobe does not allow grouping with text markup
                        }
                        if (newlyCreateInks.size() != 0) { // if we have newly created inks, then only select those
                            mSelectedAnnotsMap.putAll(newlyCreateInks);
                        } else {
                            mSelectedAnnotsMap.put(annot, mDownPageNum);
                        }

                        // let's add all selected annotation's groups to the list
                        annotsGroupSet.add(annot);
                        ArrayList<Annot> groupAnnots = AnnotUtils.getAnnotationsInGroup(mPdfViewCtrl, annot, mDownPageNum);
                        if (groupAnnots != null && groupAnnots.size() > 1) {
                            annotsGroupSet.addAll(groupAnnots);
                            reCalculateBBox = true;
                        }
                    }
                }
                // We will raise the events here as we want to group all annot additions and modifications together.
                strokeLassoHelper.raiseNewInkAddedEvent();
                strokeLassoHelper.updateExistingInks();

                selectedRectInPage = calculateSelectedRect();

                if (reCalculateBBox) {
                    mSelectedAnnotsMap.clear();
                    selectedRectInPage = new RectF();
                    for (Annot annot : annotsGroupSet) {
                        if (hasPermission(annot, ANNOT_PERMISSION_SELECTION) && isAnnotSupportEdit(annot)) {
                            if (!isAnnotSupportResize(annot)) {
                                mCanResize = false;
                            }
                            if (!isAnnotSupportMove(annot)) {
                                mAnnotIsTextMarkup = true;
                            }
                            mSelectedAnnotsMap.put(annot, mDownPageNum);
                            selectedRectInPage.union(getAnnotPageRect(annot));
                        }
                    }
                }
            }

            // If there is only one annotation, let the other tool handles it
            if (hasAnnotSelected() && mSelectedAnnotsMap.size() == 1) {
                ArrayList<Annot> entry = new ArrayList<>(mSelectedAnnotsMap.keySet());
                mAnnot = entry.get(0);
                mAnnotPageNum = mDownPageNum;
                buildAnnotBBox();
                ((ToolManager) mPdfViewCtrl.getToolManager()).selectAnnot(mAnnot, mDownPageNum);
                return false;
            }

            setupCtrlPts(selectedRectInPage);
        } catch (PDFNetException ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlockWrite) {
                mPdfViewCtrl.docUnlock();
            }
        }

        // If there is no selected annotations, go back to Pan tool
        Rect dirtyRect = new Rect((int) mPt1.x, (int) mPt1.y, (int) mPt2.x, (int) mPt2.y);
        if (!hasAnnotSelected()) {
            resetPts();
            setNextToolModeHelper(ToolMode.PAN);
        } else {
            setNextToolModeHelper((ToolMode) getToolMode());
        }

        mPdfViewCtrl.invalidate(dirtyRect);
        return skipOnUpPriorEvent(priorEventMode);
    }

    private RectF calculateSelectedRect() throws PDFNetException {
        RectF selectedRectInPage = new RectF();
        for (Annot selectedAnnot : mSelectedAnnotsMap.keySet()) {
            RectF annotPageRect = getAnnotPageRect(selectedAnnot);
            selectedRectInPage.union(annotPageRect);
        }
        return selectedRectInPage;
    }

    private RectF getAnnotPageRect(Annot annot) throws PDFNetException {
        com.pdftron.pdf.Rect annotRect = mPdfViewCtrl.getPageRectForAnnot(annot, mDownPageNum);
        annotRect.normalize();

        return new RectF(
                (float) (Math.min(annotRect.getX1(), annotRect.getX2())),
                (float) (Math.min(annotRect.getY1(), annotRect.getY2())),
                (float) (Math.max(annotRect.getX1(), annotRect.getX2())),
                (float) (Math.max(annotRect.getY1(), annotRect.getY2())));
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        PointF pt = new PointF(e.getX() + mPdfViewCtrl.getScrollX(), e.getY() + mPdfViewCtrl.getScrollY());
        if (mSelectedArea.contains(pt.x, pt.y)) {
            showMenu(getAnnotRect());
        } else if (mGroupAnnots != null) {
            prepareGroupAnnots();
        } else {
            backToPan();
        }
        return false;
    }

    /**
     * The overload implementation of {@link AnnotEdit#onPageTurning(int, int)}.
     */
    @Override
    public void onPageTurning(
            int old_page,
            int cur_page) {

        super.onPageTurning(old_page, cur_page);
        backToPan();
    }

    @Override
    public boolean onQuickMenuClicked(QuickMenuItem menuItem) {
        if (menuItem.getItemId() == R.id.qm_group) {
            createAnnotGroup();
            return true;
        } else if (menuItem.getItemId() == R.id.qm_ungroup) {
            ungroupAnnots();
            return true;
        } else if (menuItem.getItemId() == R.id.qm_note) {
            ArrayList<Annot> annots = new ArrayList<>(mSelectedAnnotsMap.keySet());
            try {
                Annot primary = AnnotUtils.getPrimaryAnnotInGroup(mPdfViewCtrl, annots);
                if (primary != null) {
                    mAnnot = primary;
                    super.onQuickMenuClicked(menuItem);
                }
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            }
            return true;
        } else if (menuItem.getItemId() == R.id.qm_copy) {
            ArrayList<Annot> annots = new ArrayList<>(mSelectedAnnotsMap.keySet());
            if (!annots.isEmpty()) {
                AnnotationClipboardHelper.copyAnnot(mPdfViewCtrl.getContext(), annots, mPdfViewCtrl,
                        new AnnotationClipboardHelper.OnClipboardTaskListener() {
                            @Override
                            public void onClipboardTaskDone(String error, ArrayList<Annot> pastedAnnotList) {
                                if (error == null && mPdfViewCtrl.getContext() != null) {
                                    CommonToast.showText(mPdfViewCtrl.getContext(), R.string.tools_copy_annot_confirmation, Toast.LENGTH_SHORT);
                                }
                            }
                        });
            }
            return true;
        }
        return super.onQuickMenuClicked(menuItem);
    }

    @Override
    protected void customizeQuickMenuItems(QuickMenu quickMenu) {
        super.customizeQuickMenuItems(quickMenu);

        QuickMenuItem noteItem = quickMenu.findMenuItem(R.id.qm_note);
        QuickMenuItem groupItem = quickMenu.findMenuItem(R.id.qm_group);
        QuickMenuItem ungroupItem = quickMenu.findMenuItem(R.id.qm_ungroup);
        QuickMenuItem copyItem = quickMenu.findMenuItem(R.id.qm_copy);
        QuickMenuItem styleItem = quickMenu.findMenuItem(R.id.qm_appearance);
        if (noteItem == null || ungroupItem == null || groupItem == null) {
            return;
        }

        ArrayList<Annot> annots = new ArrayList<>(mSelectedAnnotsMap.keySet());
        boolean sameGroup = false;
        try {
            sameGroup = AnnotUtils.isGroupSelected(mPdfViewCtrl, annots, mDownPageNum);
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
        if (sameGroup) {
            groupItem.setVisible(false);
        } else {
            groupItem.setVisible(mCanGroup);
        }

        if (copyItem != null) {
            copyItem.setVisible(mCanCopy);
        }
        if (styleItem != null) {
            styleItem.setVisible(canShowStyleMenu());
        }

        if (mGroupSelected) {
            noteItem.setVisible(true);
            ungroupItem.setVisible(true);
        } else {
            noteItem.setVisible(false);
            if (sameGroup) {
                ungroupItem.setVisible(true);
            } else {
                ungroupItem.setVisible(false);
            }
        }
    }

    @Override
    public void selectAnnot(Annot annot, int pageNum) {
        super.selectAnnot(annot, pageNum);
        prepareGroupAnnots();
    }

    @Nullable
    private AnnotStyle findFirstAnnotStyle() {
        boolean shouldUnlockRead = false;
        AnnotStyle firstStyle = null;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            for (Annot annot : mSelectedAnnotsMap.keySet()) {
                if (annot.getType() != Annot.e_FreeText) {
                    AnnotStyle annotStyle = AnnotUtils.getAnnotStyle(annot);
                    if (firstStyle == null) {
                        firstStyle = annotStyle;
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
        return firstStyle;
    }

    @Override
    protected void changeAnnotAppearance() {
        if (hasAnnotSelected()) {
            mAnnotStyle = findFirstAnnotStyle();
            super.changeAnnotAppearance();
            mAnnotStyleDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mAnnotStyleDialog = null;
                    showMenu(getAnnotRect());
                }
            });
        }
    }

    @Override
    protected AnnotStyleDialogFragment.Builder getAnnotStyleBuilder() {
        AnnotStyleDialogFragment.Builder builder = super.getAnnotStyleBuilder();

        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            ArrayList<AnnotStyle> styles = new ArrayList<>();
            for (Annot annot : mSelectedAnnotsMap.keySet()) {
                styles.add(AnnotUtils.getAnnotStyle(annot));
            }
            builder.setGroupAnnotTypes(styles);
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }

        return builder;
    }

    private void backToPan() {
        resetPts();
        mSelectedArea.setEmpty();
        mSelectedAnnotsMap.clear();
        mEffCtrlPtId = e_unknown;
        mAnnot = null;
        mGroupAnnots = null;
        mGroupSelected = false;
        closeQuickMenu();
        setNextToolModeHelper(ToolMode.PAN);
        mPdfViewCtrl.invalidate();

        ((ToolManager) mPdfViewCtrl.getToolManager()).raiseAnnotationsSelectionChangedEvent(mSelectedAnnotsMap);
    }

    private void prepareGroupAnnots() {
        if (null == mGroupAnnots) {
            return;
        }
        try {
            RectF selectedRectInPage = new RectF();
            mAnnotIsTextMarkup = false;
            mDownPageNum = mAnnotPageNum;
            for (Annot annot : mGroupAnnots) {
                com.pdftron.pdf.Rect annotRect = mPdfViewCtrl.getPageRectForAnnot(annot, mDownPageNum);
                annotRect.normalize();

                RectF annotPageRect = new RectF(
                        (float) (Math.min(annotRect.getX1(), annotRect.getX2())),
                        (float) (Math.min(annotRect.getY1(), annotRect.getY2())),
                        (float) (Math.max(annotRect.getX1(), annotRect.getX2())),
                        (float) (Math.max(annotRect.getY1(), annotRect.getY2())));

                if (hasPermission(annot, ANNOT_PERMISSION_SELECTION)) {
                    if (!isAnnotSupportResize(annot)) {
                        mCanResize = false;
                    }
                    if (!isAnnotSupportMove(annot)) {
                        mAnnotIsTextMarkup = true;
                    }
                    mSelectedAnnotsMap.put(annot, mDownPageNum);
                    selectedRectInPage.union(annotPageRect);
                }
            }
            mGroupSelected = true;
            mGroupAnnots = null;
            mAnnot = null;
            mForceSameNextToolMode = false;
            setupCtrlPts(selectedRectInPage);
        } catch (PDFNetException ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
    }

    private void createAnnotGroup() {
        // first find primary annot
        // here we assume the first one created is the primary
        ArrayList<Annot> annots = new ArrayList<>(mSelectedAnnotsMap.keySet());
        if (annots.isEmpty()) {
            return;
        }
        sort(annots);
        Annot primary = annots.get(0);
        try {
            raiseAnnotationPreModifyEvent(mSelectedAnnotsMap);
            AnnotUtils.createAnnotationGroup(mPdfViewCtrl, primary, annots);
            raiseAnnotationModifiedEvent(mSelectedAnnotsMap);
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
        mForceSameNextToolMode = false;
        backToPan();
    }

    private void ungroupAnnots() {
        ArrayList<Annot> annots = new ArrayList<>(mSelectedAnnotsMap.keySet());
        if (annots.isEmpty()) {
            return;
        }
        try {
            raiseAnnotationPreModifyEvent(mSelectedAnnotsMap);
            AnnotUtils.ungroupAnnotations(mPdfViewCtrl, annots);
            raiseAnnotationModifiedEvent(mSelectedAnnotsMap);
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
        mForceSameNextToolMode = false;
        backToPan();
    }

    private void setupCtrlPts(@NonNull RectF selectedRectInPage) {
        // Sets annotations area and control points
        if (!selectedRectInPage.isEmpty()) {
            mSelectedAreaPageSpace.set(selectedRectInPage);
            double[] pts1 = mPdfViewCtrl.convPagePtToScreenPt((double) selectedRectInPage.left, (double) selectedRectInPage.top, mDownPageNum);
            double[] pts2 = mPdfViewCtrl.convPagePtToScreenPt((double) selectedRectInPage.right, (double) selectedRectInPage.bottom, mDownPageNum);
            int scrollX = mPdfViewCtrl.getScrollX();
            int scrollY = mPdfViewCtrl.getScrollY();
            double minX = Math.min(pts1[0] + scrollX, pts2[0] + scrollX);
            double minY = Math.min(pts1[1] + scrollY, pts2[1] + scrollY);
            double maxX = Math.max(pts1[0] + scrollX, pts2[0] + scrollX);
            double maxY = Math.max(pts1[1] + scrollY, pts2[1] + scrollY);
            mSelectedArea = new RectF((float) minX, (float) minY, (float) maxX, (float) maxY);
            mAnnotPageNum = mDownPageNum;
            setCtrlPts();
            showMenu(getAnnotRect());

            ((ToolManager) mPdfViewCtrl.getToolManager()).raiseAnnotationsSelectionChangedEvent(mSelectedAnnotsMap);
        }
    }

    @Override
    protected int getMenuResByAnnot(Annot annot) {
        return R.menu.annot_group;
    }

    /**
     * The overload implementation of {@link Tool#deleteAnnot()}.
     * Deletes a set of annotations.
     */
    @Override
    protected void deleteAnnot() {

        if (mPdfViewCtrl == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "deleteAnnot");
        for (Map.Entry<Annot, Integer> entry : mSelectedAnnotsMap.entrySet()) {
            Annot annot = entry.getKey();
            if (onInterceptAnnotationHandling(annot, bundle)) {
                return;
            }
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            PDFDoc pdfDoc = mPdfViewCtrl.getDoc();
            raiseAnnotationPreRemoveEvent(mSelectedAnnotsMap);

            mPostRemoveAnnotsMap.clear();
            for (Map.Entry<Annot, Integer> entry : mSelectedAnnotsMap.entrySet()) {
                Annot annot = entry.getKey();
                if (annot == null) {
                    continue;
                }
                int annotPageNum = entry.getValue();
                Page page = pdfDoc.getPage(annotPageNum);
                annot = AnnotUtils.safeDeleteAnnotAndUpdate(mPdfViewCtrl, page, annot, annotPageNum);
                mPostRemoveAnnotsMap.put(annot, annotPageNum);
            }

            raiseAnnotationRemovedEvent(mPostRemoveAnnotsMap);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }

        backToPan();
    }

    /**
     * The overload implementation of {@link Tool#flattenAnnot()}.
     * Flatten a set of annotations.
     */
    @Override
    protected void flattenAnnot() {
        if (mPdfViewCtrl == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "flattenAnnot");
        bundle.putStringArray(KEYS, new String[]{FLATTENED});
        bundle.putBoolean(FLATTENED, true);

        for (Map.Entry<Annot, Integer> entry : mSelectedAnnotsMap.entrySet()) {
            Annot annot = entry.getKey();
            if (onInterceptAnnotationHandling(annot, bundle)) {
                return;
            }
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            raiseAnnotationPreRemoveEvent(mSelectedAnnotsMap);

            mPostRemoveAnnotsMap.clear();
            for (Map.Entry<Annot, Integer> entry : mSelectedAnnotsMap.entrySet()) {
                Annot annot = entry.getKey();
                if (annot == null) {
                    continue;
                }
                int annotPageNum = entry.getValue();
                annot = AnnotUtils.flattenAnnot(mPdfViewCtrl, annot, annotPageNum);
                mPostRemoveAnnotsMap.put(annot, annotPageNum);
            }

            raiseAnnotationRemovedEvent(mPostRemoveAnnotsMap, bundle);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }

        backToPan();
    }

    @Override
    protected void duplicateAnnot() {
        if (mSelectedAnnotsMap.isEmpty() || mSelectedAreaPageSpace.isEmpty() || mPdfViewCtrl == null) {
            return;
        }

        // first obtain the center of union rect
        double centerX = mSelectedAreaPageSpace.centerX() + 30f;
        double centerY = mSelectedAreaPageSpace.centerY() - 30f;
        double[] pts = mPdfViewCtrl.convPagePtToScreenPt(centerX, centerY, mDownPageNum);
        PointF targetPoint = new PointF((float) pts[0], (float) pts[1]);
        AnnotationClipboardHelper.copyAnnot(mPdfViewCtrl.getContext(), new ArrayList<>(mSelectedAnnotsMap.keySet()), mPdfViewCtrl, new AnnotationClipboardHelper.OnClipboardTaskListener() {
            @Override
            public void onClipboardTaskDone(String error, ArrayList<Annot> pastedAnnotList) {
                if (error == null) {
                    AnnotationClipboardHelper.pasteAnnot(mPdfViewCtrl.getContext(), mPdfViewCtrl, mDownPageNum, targetPoint, new AnnotationClipboardHelper.OnClipboardTaskListener() {
                        @Override
                        public void onClipboardTaskDone(String error, ArrayList<Annot> pastedAnnotList) {
                            // TODO multi select via API
                        }
                    });
                }
                backToPan();
            }
        });
    }

    @Override
    protected RectF getAnnotRect() {
        RectF annotsRect = new RectF(mSelectedArea);
        annotsRect.offset(-mPdfViewCtrl.getScrollX(), -mPdfViewCtrl.getScrollY());
        return annotsRect;
    }

    /**
     * Edits selected annotations size
     *
     * @param priorEventMode prior event mode
     * @return true if successfully modified annotations size, false otherwise
     */
    @Override
    protected boolean editAnnotSize(PDFViewCtrl.PriorEventMode priorEventMode) {
        float x1 = mCtrlPts[e_ul].x;
        float y1 = mCtrlPts[e_ul].y;
        float x2 = mCtrlPts[e_lr].x;
        float y2 = mCtrlPts[e_lr].y;

        RectF ctrlRect = new RectF(x1, y1, x2, y2);
        // skip calculate annots size if there is no movement or resize
        if (ctrlRect.equals(mSelectedArea)) {
            return true;
        }
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            updateSelectedAnnotSize();
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
        return true;
    }

    private void updateSelectedAnnotSize() throws PDFNetException {
        int scrollX = mPdfViewCtrl.getScrollX();
        int scrollY = mPdfViewCtrl.getScrollY();
        // current control point location
        float x1 = mCtrlPts[e_ul].x - scrollX;
        float y1 = mCtrlPts[e_ul].y - scrollY;
        float x2 = mCtrlPts[e_lr].x - scrollX;
        float y2 = mCtrlPts[e_lr].y - scrollY;
        RectF selectedArea = new RectF(mSelectedArea);
        selectedArea.offset(-scrollX, -scrollY);

        double[] ctrlPagePt1 = mPdfViewCtrl.convScreenPtToPagePt(x1, y1, mDownPageNum);
        double[] ctrlPagePt2 = mPdfViewCtrl.convScreenPtToPagePt(x2, y2, mDownPageNum);
        double[] selectedPagePt1 = mPdfViewCtrl.convScreenPtToPagePt(selectedArea.left, selectedArea.top, mDownPageNum);
        double[] selectedPagePt2 = mPdfViewCtrl.convScreenPtToPagePt(selectedArea.right, selectedArea.bottom, mDownPageNum);

        float offsetX = (float) ctrlPagePt1[0];
        float offsetY = (float) ctrlPagePt2[1];
        float scaleX = (float) ((ctrlPagePt2[0] - ctrlPagePt1[0]) / (selectedPagePt2[0] - selectedPagePt1[0]));
        float scaleY = (float) ((ctrlPagePt2[1] - ctrlPagePt1[1]) / (selectedPagePt2[1] - selectedPagePt1[1]));
        // Compute the selected area
        RectF nextSelectArea = new RectF();
        for (Map.Entry<Annot, Integer> entry : mSelectedAnnotsMap.entrySet()) {
            Annot annot = entry.getKey();
            int annotPageNum = entry.getValue();

            com.pdftron.pdf.Rect rect = mPdfViewCtrl.getPageRectForAnnot(annot, annotPageNum);
            rect.normalize();
            RectF annotRect = new RectF((float) rect.getX1(), (float) rect.getY1(), (float) rect.getX2(), (float) rect.getY2());
            annotRect.offset((float) -selectedPagePt1[0], (float) -selectedPagePt2[1]);

            com.pdftron.pdf.Rect newAnnotRect = new com.pdftron.pdf.Rect(
                    (double) annotRect.left * scaleX + offsetX,
                    (double) annotRect.top * scaleY + offsetY,
                    (double) annotRect.right * scaleX + offsetX,
                    (double) annotRect.bottom * scaleY + offsetY);
            newAnnotRect.normalize();

            double[] pt1 = mPdfViewCtrl.convPagePtToScreenPt(newAnnotRect.getX1(), newAnnotRect.getY1(), annotPageNum);
            double[] pt2 = mPdfViewCtrl.convPagePtToScreenPt(newAnnotRect.getX2(), newAnnotRect.getY2(), annotPageNum);
            nextSelectArea.union(new RectF((float) pt1[0], (float) pt2[1], (float) pt2[0], (float) pt1[1]));
        }

        boolean shouldCopyAnnot = shouldCopyAnnot(mAnnotPageNum);
        float midX = nextSelectArea.left + nextSelectArea.width() / 2.0f - mCtrlRadius;
        float midY = nextSelectArea.top + nextSelectArea.height() / 2.0f - mCtrlRadius;
        int newPageNumber = mPdfViewCtrl.getPageNumberFromScreenPt(midX, midY);

        // This occurs when the screen point is outside of a page
        if (newPageNumber < 1) {
            newPageNumber = mAnnotPageNum;
        }

        RectF tempRect = new RectF(nextSelectArea);
        tempRect.offset(mPdfViewCtrl.getScrollX(), mPdfViewCtrl.getScrollY());
        if (shouldCopyAnnot) {
            hasSnapped = snapRectToPage(tempRect, newPageNumber);
        } else {
            hasSnapped = snapRectToPage(tempRect, mAnnotPageNum);
        }
        tempRect.offset(-mPdfViewCtrl.getScrollX(), -mPdfViewCtrl.getScrollY());
        float xSnapOffset = nextSelectArea.left - tempRect.left;
        float ySnapOffset = nextSelectArea.top - tempRect.top;

        nextSelectArea.set(tempRect);

        if (!shouldCopyAnnot) {
            raiseAnnotationPreModifyEvent(mSelectedAnnotsMap);
        }
        for (Map.Entry<Annot, Integer> entry : mSelectedAnnotsMap.entrySet()) {
            Annot annot = entry.getKey();
            int annotPageNum = entry.getValue();

            // First compute page rect after scaling and translating
            com.pdftron.pdf.Rect rect = mPdfViewCtrl.getPageRectForAnnot(annot, annotPageNum);
            rect.normalize();

            rect.setX1(rect.getX1() - selectedPagePt1[0]);
            rect.setX2(rect.getX2() - selectedPagePt1[0]);
            rect.setY1(rect.getY1() - selectedPagePt2[1]);
            rect.setY2(rect.getY2() - selectedPagePt2[1]);

            com.pdftron.pdf.Rect temp = new com.pdftron.pdf.Rect(
                    rect.getX1() * scaleX + offsetX,
                    rect.getY1() * scaleY + offsetY,
                    rect.getX2() * scaleX + offsetX,
                    rect.getY2() * scaleY + offsetY
            );

            // Copy the annot to the new page if needed. Also we compute the screen rect here so that
            // we can use this to convert to the final page rect.
            com.pdftron.pdf.Rect screenRect = Utils.convertFromPageRectToScreenRect(mPdfViewCtrl, temp, annotPageNum);
            if (hasSnapped) {
                screenRect.setX1(screenRect.getX1() - xSnapOffset);
                screenRect.setX2(screenRect.getX2() - xSnapOffset);
                screenRect.setY1(screenRect.getY1() - ySnapOffset);
                screenRect.setY2(screenRect.getY2() - ySnapOffset);
            }

            if (shouldCopyAnnot) {
                annotPageNum = copyAnnotToNewPage(annot, annotPageNum, newPageNumber);
                mAnnotPageNum = annotPageNum;
                mDownPageNum = annotPageNum;
                entry.setValue(annotPageNum);
            }

            // Then we compute the new page rect, note the page may change
            com.pdftron.pdf.Rect newAnnotRect = AnnotUtils.getPageRectFromScreenRect(mPdfViewCtrl, screenRect, annotPageNum); // note annotPageNum may have changed
            newAnnotRect.normalize();

            // It is possible during viewing that GetRect does not return the most accurate bounding box
            // of what is actually rendered, to obtain the correct behavior when resizing/moving, we
            // need to call refreshAppearance before resize
            if (annot.getType() != Annot.e_Stamp && annot.getType() != Annot.e_Text) {
                if (annot.getType() == Annot.e_Ink && PressureInkUtils.isPressureSensitive(annot)) {
                    PressureInkUtils.refreshCustomInkAppearanceForExistingAnnot(annot);
                } else {
                    annot.refreshAppearance();
                }
            }
            annot.resize(newAnnotRect);
            // We do not want to call refreshAppearance for stamps
            // to not alter their original appearance.
            if (annot.getType() != Annot.e_Stamp) {
                AnnotUtils.refreshAnnotAppearance(mPdfViewCtrl.getContext(), annot);
            }

            mPdfViewCtrl.update(annot, annotPageNum);
        }
        if (!shouldCopyAnnot) {
            raiseAnnotationModifiedEvent(mSelectedAnnotsMap);
        }

        if (!mPdfViewCtrl.isAnnotationLayerEnabled()) {
            mPdfViewCtrl.update(new com.pdftron.pdf.Rect(nextSelectArea.left, nextSelectArea.top, nextSelectArea.right, nextSelectArea.bottom));
        }

        nextSelectArea.offset(scrollX, scrollY);
        mSelectedArea.set(nextSelectArea);
        mPt1.set(mSelectedArea.left, mSelectedArea.top);
        mPt2.set(mSelectedArea.right, mSelectedArea.bottom);
        setCtrlPts();
    }

    private boolean isAnnotSupportResize(@NonNull Annot annot) throws PDFNetException {
        return annot.getType() != Annot.e_Highlight
                && annot.getType() != Annot.e_StrikeOut
                && annot.getType() != Annot.e_Underline
                && annot.getType() != Annot.e_Squiggly
                && annot.getType() != Annot.e_Text
                && annot.getType() != Annot.e_Stamp;
    }

    private boolean isAnnotSupportMove(@NonNull Annot annot) throws PDFNetException {
        return annot.getType() != Annot.e_Highlight
                && annot.getType() != Annot.e_StrikeOut
                && annot.getType() != Annot.e_Underline
                && annot.getType() != Annot.e_Squiggly;
    }

    @Override
    protected boolean isAnnotResizable() {
        return mCanResize;
    }

    private boolean isAnnotSupportCopy(@NonNull Annot annot) throws PDFNetException {
        return annot.getType() != Annot.e_Highlight
                && annot.getType() != Annot.e_StrikeOut
                && annot.getType() != Annot.e_Underline
                && annot.getType() != Annot.e_Squiggly;
    }

    private void resetPts() {
        mPt1.set(0, 0);
        mPt2.set(0, 0);
        mBBox.setEmpty();

        mScreenPath.reset();
        mPath.reset();
        strokeLassoHelper.resetPath();
        mMinX = mMinY = mMaxX = mMaxY = 0;
        strokeLassoHelper.initRect(0, 0);
    }

    private com.pdftron.pdf.Rect getShapeBBox() {
        // Computes the bounding box of the rubber band in page space.
        double[] pts1;
        double[] pts2;
        pts1 = mPdfViewCtrl.convScreenPtToPagePt(mPt1.x - mPdfViewCtrl.getScrollX(), mPt1.y - mPdfViewCtrl.getScrollY(), mDownPageNum);
        pts2 = mPdfViewCtrl.convScreenPtToPagePt(mPt2.x - mPdfViewCtrl.getScrollX(), mPt2.y - mPdfViewCtrl.getScrollY(), mDownPageNum);
        com.pdftron.pdf.Rect rect;
        try {
            rect = new com.pdftron.pdf.Rect(pts1[0], pts1[1], pts2[0], pts2[1]);
            rect.normalize();
            return rect;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected RectF getScreenRect(com.pdftron.pdf.Rect screen_rect) {
        if (hasAnnotSelected() && mSelectedAnnotsMap.size() == 1) {
            return super.getScreenRect(screen_rect);
        }
        return new RectF(mSelectedArea);
    }

    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        // We are scrolling
        if (mAllowTwoFingerScroll) {
            return;
        }

        if (hasAnnotSelected()) {
            super.onDraw(canvas, tfm);

            if (isAnnotResizable() && !mHandleEffCtrlPtsDisabled) {
                DrawingUtils.drawCtrlPts(mPdfViewCtrl.getResources(), canvas, mPaint,
                        mCtrlPts[e_ul], mCtrlPts[e_lr], mCtrlPts[e_lm], mCtrlPts[e_ml],
                        mCtrlRadius, mHasSelectionPermission, mMaintainAspectRatio);
            }
        } else {
            if (SelectionMode.LASSO == mSelectionMode) {
                canvas.drawPath(mPath, mLassoPaint);
            } else {
                int min_x = (int) Math.min(mPt1.x, mPt2.x);
                int max_x = (int) Math.max(mPt1.x, mPt2.x);
                int min_y = (int) Math.min(mPt1.y, mPt2.y);
                int max_y = (int) Math.max(mPt1.y, mPt2.y);

                Rect fillRect = new Rect(min_x, min_y, max_x, max_y);
                if (!fillRect.isEmpty()) {
                    canvas.drawRect(fillRect, mFillPaint);
                }
            }
        }
    }

    // styles start

    @Override
    protected void refreshAppearanceImpl(Annot annot, int pageNum) throws PDFNetException {
        if (null == annot) {
            return;
        }
        AnnotUtils.refreshAnnotAppearance(mPdfViewCtrl.getContext(), annot);
    }

    @Override
    protected void editThickness(float thickness) {
        raiseAnnotationPreModifyEvent(mSelectedAnnotsMap);
        for (Map.Entry<Annot, Integer> entry : mSelectedAnnotsMap.entrySet()) {
            Annot annot = entry.getKey();
            int pageNum = entry.getValue();
            if (isFreeText(annot)) {
                continue;
            }
            super.editThickness(annot, pageNum, thickness, false);
        }
        raiseAnnotationModifiedEvent(mSelectedAnnotsMap);
    }

    @Override
    protected void editOpacity(float opacity) {
        raiseAnnotationPreModifyEvent(mSelectedAnnotsMap);
        for (Map.Entry<Annot, Integer> entry : mSelectedAnnotsMap.entrySet()) {
            Annot annot = entry.getKey();
            int pageNum = entry.getValue();
            if (isFreeText(annot)) {
                continue;
            }
            super.editOpacity(annot, pageNum, opacity, false);
        }
        raiseAnnotationModifiedEvent(mSelectedAnnotsMap);
    }

    @Override
    protected void editColor(int color) {
        raiseAnnotationPreModifyEvent(mSelectedAnnotsMap);
        for (Map.Entry<Annot, Integer> entry : mSelectedAnnotsMap.entrySet()) {
            Annot annot = entry.getKey();
            int pageNum = entry.getValue();
            if (isFreeText(annot)) {
                continue;
            }
            super.editColor(annot, pageNum, color, false);
        }
        raiseAnnotationModifiedEvent(mSelectedAnnotsMap);
    }

    @Override
    protected void editFillColor(int color) {
        raiseAnnotationPreModifyEvent(mSelectedAnnotsMap);
        for (Map.Entry<Annot, Integer> entry : mSelectedAnnotsMap.entrySet()) {
            Annot annot = entry.getKey();
            int pageNum = entry.getValue();
            if (isFreeText(annot)) {
                continue;
            }
            super.editFillColor(annot, pageNum, color, false);
        }
        raiseAnnotationModifiedEvent(mSelectedAnnotsMap);
    }

    @Override
    protected void editIcon(String icon) {
        raiseAnnotationPreModifyEvent(mSelectedAnnotsMap);
        for (Map.Entry<Annot, Integer> entry : mSelectedAnnotsMap.entrySet()) {
            Annot annot = entry.getKey();
            int pageNum = entry.getValue();
            if (isFreeText(annot)) {
                continue;
            }
            super.editIcon(annot, pageNum, icon, false);
        }
        raiseAnnotationModifiedEvent(mSelectedAnnotsMap);
    }

    @Override
    protected void editRuler(RulerItem rulerItem) {
        raiseAnnotationPreModifyEvent(mSelectedAnnotsMap);
        for (Map.Entry<Annot, Integer> entry : mSelectedAnnotsMap.entrySet()) {
            Annot annot = entry.getKey();
            int pageNum = entry.getValue();
            if (isFreeText(annot)) {
                continue;
            }
            super.editRuler(annot, pageNum, rulerItem, false);
        }
        raiseAnnotationModifiedEvent(mSelectedAnnotsMap);
    }

    @Override
    protected void editRedactionOverlayText(String overlayText) {
        raiseAnnotationPreModifyEvent(mSelectedAnnotsMap);
        for (Map.Entry<Annot, Integer> entry : mSelectedAnnotsMap.entrySet()) {
            Annot annot = entry.getKey();
            int pageNum = entry.getValue();
            if (isFreeText(annot)) {
                continue;
            }
            super.editRedactionOverlayText(annot, pageNum, overlayText, false);
        }
        raiseAnnotationModifiedEvent(mSelectedAnnotsMap);
    }

    @Override
    protected void editBorderStyle(ShapeBorderStyle borderStyle) {
        raiseAnnotationPreModifyEvent(mSelectedAnnotsMap);
        for (Map.Entry<Annot, Integer> entry : mSelectedAnnotsMap.entrySet()) {
            Annot annot = entry.getKey();
            int pageNum = entry.getValue();
            super.editBorderStyle(annot, pageNum, borderStyle, false);
        }
        raiseAnnotationModifiedEvent(mSelectedAnnotsMap);
    }

    @Override
    protected void editLineStyle(LineStyle lineStyle) {
        raiseAnnotationPreModifyEvent(mSelectedAnnotsMap);
        for (Map.Entry<Annot, Integer> entry : mSelectedAnnotsMap.entrySet()) {
            Annot annot = entry.getKey();
            int pageNum = entry.getValue();
            super.editLineStyle(annot, pageNum, lineStyle, false);
        }
        raiseAnnotationModifiedEvent(mSelectedAnnotsMap);
    }

    @Override
    protected void editLineStartStyle(LineEndingStyle lineStartStyle) {
        raiseAnnotationPreModifyEvent(mSelectedAnnotsMap);
        for (Map.Entry<Annot, Integer> entry : mSelectedAnnotsMap.entrySet()) {
            Annot annot = entry.getKey();
            int pageNum = entry.getValue();
            super.editLineStartStyle(annot, pageNum, lineStartStyle, false);
        }
        raiseAnnotationModifiedEvent(mSelectedAnnotsMap);
    }

    @Override
    protected void editLineEndStyle(LineEndingStyle lineEndStyle) {
        raiseAnnotationPreModifyEvent(mSelectedAnnotsMap);
        for (Map.Entry<Annot, Integer> entry : mSelectedAnnotsMap.entrySet()) {
            Annot annot = entry.getKey();
            int pageNum = entry.getValue();
            super.editLineEndStyle(annot, pageNum, lineEndStyle, false);
        }
        raiseAnnotationModifiedEvent(mSelectedAnnotsMap);
    }

    // free text uses android ui to refresh appearance
    // so we won't do anything for it in multi-select
    private boolean isFreeText(@NonNull Annot annot) {
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            return annot.getType() == Annot.e_FreeText;
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
        return false;
    }

    // styles end

    /**
     * Used to manage ink stroke related lasso tool functionality.
     */
    private class StrokeLassoHelper {

        private float mMinXPage;
        private float mMinYPage;
        private float mMaxXPage;
        private float mMaxYPage;
        private final Path mPagePath = new Path();
        private Region mLassoRegionPage;

        private final List<Annot> mAnnotsAdded = new ArrayList<>();

        private final List<Pair<Ink, List<Integer>>> mInksToUpdate = new ArrayList<>(); // annots to update and their path indices to remove

        /**
         * Gets the indices of paths in the ink list that are selected by the lasso region.
         *
         * @param lassoRegionPage region of the lasso page
         * @param ink             the ink annotation to select strokes from
         * @return a list of indices that were selected.
         * @throws PDFNetException
         */
        @NonNull
        List<Integer> getSelectedPathIndices(Region lassoRegionPage, Ink ink) throws PDFNetException {
            Obj inkList = ink.getSDFObj().findObj(AnnotUtils.KEY_INK_LIST);
            List<ArrayList<PointF>> strokeList =
                    FreehandCreate.createPageStrokesFromArrayObj(
                            inkList
                    );

            List<Integer> selectedPathIndices = new ArrayList<>();
            for (int i = 0; i < strokeList.size(); i++) {
                ArrayList<PointF> stroke = strokeList.get(i);
                float minX = Float.MAX_VALUE;
                float minY = Float.MAX_VALUE;
                float maxX = Float.MIN_VALUE;
                float maxY = Float.MIN_VALUE;
                float strokeWidth = (float) ink.getBorderStyle().getWidth();
                StrokeOutlineBuilder builder = new StrokeOutlineBuilder(strokeWidth);
                for (PointF point : stroke) {
                    float x = point.x;
                    float y = point.y;
                    builder.addPoint(x, y, strokeWidth);
                    minX = Math.min(x, minX);
                    minY = Math.min(y, minY);
                    maxX = Math.max(x, maxX);
                    maxY = Math.max(y, maxY);
                }
                double[] outline = builder.getOutline();

                Path outlinePath = PathPool.getInstance().obtain();
                outlinePath.setFillType(Path.FillType.WINDING);

                if (outline.length == 0) {
                    break;
                }
                outlinePath.moveTo((float) outline[0], (float) outline[1]);
                for (int j = 2, cnt = outline.length; j < cnt; j += 6) {
                    outlinePath.cubicTo((float) outline[j], (float) outline[j + 1], (float) outline[j + 2],
                            (float) outline[j + 3], (float) outline[j + 4], (float) outline[j + 5]);
                }

                Region strokeRegionPage = new Region();
                strokeRegionPage.setPath(outlinePath, new Region(
                        (int) (minX - strokeWidth),
                        (int) (minY - strokeWidth),
                        (int) (maxX + strokeWidth),
                        (int) (maxY + strokeWidth)
                ));
                boolean intersectStroke = strokeRegionPage.op(lassoRegionPage, Region.Op.INTERSECT);
                if (intersectStroke) {
                    selectedPathIndices.add(i);
                }
            }
            return selectedPathIndices;
        }

        /**
         * Splits the specified path indices in the ink annotation in to a new ink annotation.
         *
         * @param ink                the ink annotation to split the paths from
         * @param pathIndicesToSplit the indices of the paths in path list that we want to split in to a new ink annotation.
         * @return The ink annotation that was added to the doc. If null, then no annot was added.
         * @throws PDFNetException
         */
        @Nullable
        Ink splitInkAnnot(@NonNull Ink ink, List<Integer> pathIndicesToSplit) throws PDFNetException {
            PDFDoc pdfDoc = mPdfViewCtrl.getDoc();
            Obj inkList = ink.getSDFObj().findObj(AnnotUtils.KEY_INK_LIST);

            if (!inkList.isArray() || pathIndicesToSplit.size() == 0) {
                return null;
            }

            // We only want to create a new ink annot if we are splitting a subset of the ink list
            if (inkList.size() > pathIndicesToSplit.size()) {
                // ************************************************************************************
                // First find all paths and thicknesses we want to add from pathIndicesToSplit
                // ************************************************************************************
                List<List<PointF>> pathsToAdd = new ArrayList<>();  // do not reorder, order matters
                for (int i = 0, cnt = pathIndicesToSplit.size(); i < cnt; i++) {
                    Obj pathArray = inkList.getAt(pathIndicesToSplit.get(i));
                    if (pathArray != null && pathArray.isArray()) {
                        List<PointF> pagePoints = new ArrayList<>();
                        for (long j = 0, count = pathArray.size(); j < count; j += 2) {
                            float x = (float) pathArray.getAt((int) j).getNumber();
                            float y = (float) pathArray.getAt((int) j + 1).getNumber();
                            PointF p = new PointF(x, y);
                            pagePoints.add(p);
                        }
                        pathsToAdd.add(pagePoints);
                    }
                }

                boolean pressureSensitive = PressureInkUtils.isPressureSensitive(ink);
                List<List<Float>> thicknessesToAdd = new ArrayList<>(); // do not reorder, order matters
                if (pressureSensitive) {
                    List<List<Float>> thicknessListToSplit = PressureInkUtils.getThicknessList(ink);
                    if (thicknessListToSplit != null) {
                        for (int i = 0, cnt = pathIndicesToSplit.size(); i < cnt; i++) {
                            Integer idx = pathIndicesToSplit.get(i);
                            if (thicknessListToSplit.size() > idx) {
                                List<Float> thickness = thicknessListToSplit.get(idx);
                                thicknessesToAdd.add(thickness);
                            }
                        }
                    }
                }
                // ************************************************************************************
                // Create new annot and add paths and thicknesses specified by path indices.
                // Then add the annot to the PDF.
                // ************************************************************************************
                float strokeWidth = (float) ink.getBorderStyle().getWidth();
                com.pdftron.pdf.Rect annotRect = PressureInkUtils.getInkItemBBox(
                        pathsToAdd,
                        strokeWidth,
                        mDownPageNum,
                        null,
                        false
                );
                if (annotRect == null) {
                    return null;
                }
                Ink newInk = Ink.create(pdfDoc, annotRect);
                PressureInkUtils.setInkList(newInk, pathsToAdd);
                if (pressureSensitive && thicknessesToAdd.size() > 0) {
                    PressureInkUtils.setThicknessList(newInk, thicknessesToAdd);
                }

                // Copy style from original ink annot
                newInk.setSmoothing(ink.getSmoothing());
                newInk.setColor(ink.getColorAsRGB(), 3);
                newInk.setOpacity(ink.getOpacity());
                Annot.BorderStyle bs = ink.getBorderStyle();
                bs.setWidth(strokeWidth);
                newInk.setBorderStyle(bs);
                if (pressureSensitive && thicknessesToAdd.size() > 0) {
                    PressureInkUtils.refreshCustomAppearanceForNewAnnot(mPdfViewCtrl, newInk);
                } else {
                    newInk.refreshAppearance();
                }

                Page page = pdfDoc.getPage(mDownPageNum);
                page.annotPushBack(newInk);
                mPdfViewCtrl.update(newInk, mDownPageNum);
                mAnnotsAdded.add(newInk);

                // ************************************************************************************
                // Finally,
                // ************************************************************************************
                mInksToUpdate.add(new Pair<Ink, List<Integer>>(ink, pathIndicesToSplit));
                return newInk;
            } else {
                return null;
            }
        }

        /**
         * Creates a new ink annotation by splitting ink list paths from the given ink annotation. The
         * paths to split will be determined by the lasso tool selection region.
         *
         * @param ink annotation used to find selected ink list paths
         * @return a pair containing the annotation that was split and created, and a boolean that is true if strokes were selected.
         * Note: It is possible that no new ink was created but strokes were selected. For example, if all strokes are selected then
         * we do not want to create a new ink annotation.
         * @throws PDFNetException
         */
        @Nullable
        Pair<Ink, Boolean> splitAndCreateNewInks(Ink ink) throws PDFNetException {
            List<Integer> indicesToSelect = getSelectedPathIndices(mLassoRegionPage, ink);
            // Create new annot only if not all strokes are selected
            if (indicesToSelect.size() == 0) {
                return new Pair<>(null, false);
            } else {
                return new Pair<>(splitInkAnnot(ink, indicesToSelect), true);
            }
        }

        void raiseNewInkAddedEvent() {
            Map<Annot, Integer> annotsAdded = new HashMap<>();
            for (Annot annot : this.mAnnotsAdded) {
                annotsAdded.put(annot, mDownPageNum);
            }
            raiseAnnotationAddedEvent(annotsAdded);
            this.mAnnotsAdded.clear();
        }

        /**
         * Remove all added paths and thicknesses from the original ink annotation that were split
         * previously. Also raises annotation modified event.
         *
         * @throws PDFNetException
         */
        void updateExistingInks() throws PDFNetException {
            if (mInksToUpdate.isEmpty()) {
                return;
            }

            Map<Annot, Integer> annotsToModify = new HashMap<>();
            for (Pair<Ink, List<Integer>> inkListPair : mInksToUpdate) {
                annotsToModify.put(inkListPair.first, mDownPageNum);
            }
            raiseAnnotationPreModifyEvent(annotsToModify);
            for (Pair<Ink, List<Integer>> annotListPair : mInksToUpdate) {
                Ink ink = annotListPair.first;
                List<Integer> pathIndicesToSplit = annotListPair.second;
                boolean pressureSensitive = PressureInkUtils.isPressureSensitive(ink);
                float strokeWidth = (float) ink.getBorderStyle().getWidth();
                Collections.sort(pathIndicesToSplit, Collections.reverseOrder()); // eraser from largest index to smallest
                for (Integer index : pathIndicesToSplit) {
                    PressureInkUtils.eraseSubPath(ink, index);
                }
                if (pressureSensitive) {
                    com.pdftron.pdf.Rect rect = PressureInkUtils.getInkItemBBox(
                            FreehandCreate.createStrokeListFromArrayObj(ink.getSDFObj().findObj(AnnotUtils.KEY_INK_LIST)),
                            strokeWidth,
                            0,
                            null,
                            false
                    );
                    if (rect != null) {
                        ink.setRect(rect);
                    }
                    PressureInkUtils.refreshCustomInkAppearanceForExistingAnnot(ink);
                } else {
                    ink.refreshAppearance();
                }
            }
            raiseAnnotationModifiedEvent(annotsToModify);
            mInksToUpdate.clear();
        }

        /**
         * Update the region of selection.
         */
        public void updateRect(float x, float y) {
            mMinXPage = Math.min(mMinXPage, (float) x);
            mMaxXPage = Math.max(mMaxXPage, (float) x);
            mMinYPage = Math.min(mMinYPage, (float) y);
            mMaxYPage = Math.max(mMaxYPage, (float) y);
        }

        /**
         * Initialize the region of selection.
         */
        public void initRect(float x, float y) {
            strokeLassoHelper.mMinXPage = strokeLassoHelper.mMaxXPage = x;
            strokeLassoHelper.mMinYPage = strokeLassoHelper.mMaxYPage = y;
        }

        public void resetPath() {
            mPagePath.reset();
        }

        public void pathMoveTo(float x, float y) {
            mPagePath.moveTo(x, y);
        }

        public void pathLineTo(float x, float y) {
            mPagePath.lineTo(x, y);
        }

        /**
         * Called to update the region associated with the lasso tool. Should be called before
         * calling splitAndCreateNewInks.
         */
        void updateRegion() {
            mLassoRegionPage = new Region();
            mLassoRegionPage.setPath(mPagePath, new Region(
                    Math.round(mMinXPage),
                    Math.round(mMinYPage),
                    Math.round(mMaxXPage),
                    Math.round(mMaxYPage)
            ));
        }
    }

    private boolean canShowStyleMenu() {
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;

            for (Annot annot : mSelectedAnnotsMap.keySet()) {
                if (annot.getType() == Annot.e_Stamp ||
                        annot.getType() == Annot.e_FreeText ||
                        annot.getType() == Annot.e_FileAttachment ||
                        annot.getType() == Annot.e_Widget) {
                    return false;
                }
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
        return true;
    }

    /**
     * The overload implementation of {@link AnnotEdit#hasAnnotSelected()}.
     */
    @Override
    public boolean hasAnnotSelected() {
        return !mSelectedAnnotsMap.isEmpty();
    }

    private final Comparator<Annot> mDateComparator =
            new Comparator<Annot>() {
                @Override
                public int compare(Annot o1, Annot o2) {
                    return AnnotUtils.compareCreationDate(o2, o1); // reverse order
                }
            };

    private void sort(@NonNull List<Annot> annots) {
        Collections.sort(annots, mDateComparator);
    }
}
