
//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementReader;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.FreeText;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.FreeTextCacheStruct;
import com.pdftron.pdf.model.FreeTextInfo;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.FreeTextAlignmentUtils;
import com.pdftron.pdf.utils.InlineEditText;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.viewmodel.RichTextViewModel;
import com.pdftron.pdf.widget.AutoScrollEditText;
import com.pdftron.sdf.Obj;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

/**
 * A tool for creating free text annotation
 */
@Keep
public class FreeTextCreate extends Tool implements DialogAnnotNote.DialogAnnotNoteListener,
        InlineEditText.InlineEditTextListener, TextWatcher {
    private static final String TAG = FreeTextCreate.class.getName();

    final private static int THRESHOLD_FROM_PAGE_EDGE = 3; // in page space
    final private static int MINIMUM_BBOX_WIDTH = 50; // in page space

    public static final boolean sUseEditTextAppearance = true;

    protected PointF mTargetPointCanvasSpace;
    protected com.pdftron.pdf.Point mTargetPointPageSpace;
    protected int mPageNum;
    private int mTextColor;
    private float mTextSize;
    private int mHorizontalAlignment = Gravity.NO_GRAVITY;
    private int mVerticalAlignment = Gravity.NO_GRAVITY;

    private AnnotStyle mAnnotStyle;
    private int mStrokeColor;
    private float mThickness;
    private int mFillColor;
    private float mOpacity;
    private String mPDFTronFontName;
    private int mCurrentEditMode;
    private boolean mUpdateEditMode;

    protected InlineEditText mInlineEditText;
    private int mAnnotButtonPressed;
    private long mStoredTimeStamp;
    protected boolean mOnUpOccurred;
    private float mStoredPointX = 0;
    private float mStoredPointY = 0;
    private String mCacheFileName;
    private DialogFreeTextNote mDialogFreeTextNote;
    protected boolean mFreeTextInlineToggleEnabled;

    protected boolean mOnCloseOccurred;

    // Used for custom data, default Rect coordinates.
    private static final String DEFAULT_RECT_X1_KEY = "pdftron_defaultX1";
    private static final String DEFAULT_RECT_Y1_KEY = "pdftron_defaultY1";
    private static final String DEFAULT_RECT_X2_KEY = "pdftron_defaultX2";
    private static final String DEFAULT_RECT_Y2_KEY = "pdftron_defaultY2";

    // rich text
    private boolean mRichContentEnabled;
    @Nullable
    private RichTextViewModel mRichTextViewModel;

    // appearance
    protected boolean mUseEditTextAppearance = sUseEditTextAppearance;

    /**
     * Class constructor
     */
    public FreeTextCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        mNextToolMode = getToolMode();
        mTargetPointCanvasSpace = new PointF(0, 0);
        mTargetPointPageSpace = new com.pdftron.pdf.Point(0, 0);
        mStoredTimeStamp = System.currentTimeMillis();

        ToolManager toolManager = (ToolManager) ctrl.getToolManager();
        mCacheFileName = toolManager.getFreeTextCacheFileName();
        mFreeTextInlineToggleEnabled = toolManager.isfreeTextInlineToggleEnabled();
        mRichContentEnabled = toolManager.isRichContentEnabledForFreeText();

        mAllowScrollWithTapTool = true;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        setRichContentEnabled(mRichContentEnabled);
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.TEXT_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_FreeText;
    }

    /**
     * The overload implementation of {@link Tool#isCreatingAnnotation()}.
     */
    @Override
    public boolean isCreatingAnnotation() {
        return true;
    }

    /**
     * Gets whether rich content is enabled for the FreeText annotation
     */
    public boolean isRichContentEnabled() {
        return mRichContentEnabled;
    }

    /**
     * Sets whether rich content is enabled for the FreeText annotation
     */
    public void setRichContentEnabled(boolean richContentEnabled) {
        mRichContentEnabled = richContentEnabled;

        if (richContentEnabled) {
            FragmentActivity activity = ((ToolManager) mPdfViewCtrl.getToolManager()).getCurrentActivity();
            if (activity != null) {
                mRichTextViewModel = ViewModelProviders.of(activity).get(RichTextViewModel.class);
            }
        }
    }

    @Override
    public void setupAnnotProperty(AnnotStyle annotStyle) {
        super.setupAnnotProperty(annotStyle);

        mAnnotStyle = annotStyle;
        int color = annotStyle.getColor();
        int fill = annotStyle.getFillColor();
        float thickness = annotStyle.getThickness();
        float opacity = annotStyle.getOpacity();
        String pdftronFontName = annotStyle.getPDFTronFontName();
        float textSize = annotStyle.getTextSize();
        int textColor = annotStyle.getTextColor();

        boolean updateColor = color != mStrokeColor;
        boolean updateFill = fill != mFillColor;
        boolean updateThickness = thickness != mThickness;
        boolean updateOpacity = opacity != mOpacity;
        boolean updateFont = !Objects.equals(pdftronFontName, mPDFTronFontName);
        boolean updateTextSize = textSize != mTextSize;
        boolean updateTextColor = textColor != mTextColor;

        mStrokeColor = color;
        mThickness = thickness;
        mTextColor = textColor;
        mTextSize = textSize;
        mHorizontalAlignment = annotStyle.getHorizontalAlignment();
        mVerticalAlignment = annotStyle.getVerticalAlignment();
        mOpacity = opacity;
        mFillColor = fill;
        mPDFTronFontName = pdftronFontName;

        mRichContentEnabled = !Utils.isNullOrEmpty(annotStyle.getTextHTMLContent());
        setRichContentEnabled(mRichContentEnabled);
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        toolManager.setRichContentEnabledForFreeText(mRichContentEnabled);

        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt(getTextColorKey(getCreateAnnotType()), mTextColor);
        editor.putFloat(getOpacityKey(getCreateAnnotType()), mOpacity);
        editor.putFloat(getTextSizeKey(getCreateAnnotType()), mTextSize);
        editor.putInt(getColorFillKey(getCreateAnnotType()), mFillColor);
        editor.putInt(getColorKey(getCreateAnnotType()), mStrokeColor);
        editor.putFloat(getThicknessKey(getCreateAnnotType()), mThickness);
        editor.putString(getFontKey(getCreateAnnotType()), mPDFTronFontName);
        editor.putInt(getHorizontalAlignmentKey(getCreateAnnotType()), mHorizontalAlignment);
        editor.putInt(getVerticalAlignmentKey(getCreateAnnotType()), mVerticalAlignment);

        editor.apply();

        // also update any active edit text
        if (mInlineEditText != null && mInlineEditText.isEditing()) {
            if (updateColor) {
                mInlineEditText.getEditText().updateColor(mStrokeColor);
            }
            if (updateFill) {
                mInlineEditText.getEditText().updateFillColor(mFillColor);
            }
            if (updateThickness) {
                mInlineEditText.getEditText().updateThickness(mThickness);
            }
            if (updateOpacity) {
                mInlineEditText.getEditText().updateOpacity(mOpacity);
            }
            if (updateFont) {
                mInlineEditText.getEditText().updateFont(mAnnotStyle.getFont());
            }
            if (updateTextSize) {
                mInlineEditText.getEditText().updateTextSize(mTextSize);
            }
            if (updateTextColor) {
                mInlineEditText.getEditText().updateTextColor(mTextColor);
            }
        }
    }

    /**
     * The overload implementation of {@link Tool#onDown(MotionEvent)}.
     */
    @Override
    public boolean onDown(MotionEvent e) {
        super.onDown(e);
        if (mInlineEditText == null || !mInlineEditText.isEditing()) {
            initTextStyle();
            mAnnotPushedBack = false;
            setTargetPoints(new PointF(e.getX(), e.getY()));
        }
        mOnUpOccurred = false;

        return super.onDown(e);
    }

    /**
     * The overload implementation of {@link Tool#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        super.onMove(e1, e2, x_dist, y_dist);

        // allow scrolling
        return false;
    }

    /**
     * The overload implementation of {@link Tool#onScaleBegin(float, float)}.
     */
    @Override
    public boolean onScaleBegin(float x, float y) {
        // hide edit text during scaling
        if (mInlineEditText != null && mInlineEditText.isEditing()) {
            saveAndQuitInlineEditText(true);
        }
        return super.onScaleBegin(x, y);
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
        // With a fling motion, onUp is called twice. We want
        // to ignore the second call
        if (mOnUpOccurred) {
            return false;
        }
        mOnUpOccurred = true;

        if (mInlineEditText != null && mInlineEditText.isEditing()) {
            saveAndQuitInlineEditText(false);
            return false;
        }

        // We are scrolling
        if (mAllowTwoFingerScroll) {
            doneTwoFingerScrolling();
            return false;
        }

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();

        // consume quick menu
        if (toolManager.isQuickMenuJustClosed()) {
            return true;
        }

        if (priorEventMode == PDFViewCtrl.PriorEventMode.PAGE_SLIDING) {
            return false;
        }

        // If we are just up from fling or pinch, do not add new note
        if (priorEventMode == PDFViewCtrl.PriorEventMode.FLING ||
                priorEventMode == PDFViewCtrl.PriorEventMode.PINCH) {
            // allow scrolling
            return false;
        }

        // If annotation was already pushed back, avoid re-entry due to fling motion
        // but allow when creating multiple strokes.
        if (mAnnotPushedBack && mForceSameNextToolMode) {
            return true;
        }

        setTargetPoints(new PointF(e.getX(), e.getY()));
        mStoredPointX = e.getX();
        mStoredPointY = e.getY();
        if (mPageNum >= 1) {
            // prevents creating annotation outside of page bounds

            // if tap on the same kind, select the annotation instead of create a new one
            Annot tappedAnnot = didTapOnSameTypeAnnot(e);
            int x = (int) e.getX();
            int y = (int) e.getY();
            int page = mPdfViewCtrl.getPageNumberFromScreenPt(x, y);
            if (tappedAnnot != null) {
                // force ToolManager to select the annotation
                setCurrentDefaultToolModeHelper(getToolMode());
                toolManager.selectAnnot(tappedAnnot, page);
            } else {
                createFreeText();
                return true;
            }
        }

        return false;
    }

    public static com.pdftron.pdf.Rect getRectUnion(com.pdftron.pdf.Rect rect1, com.pdftron.pdf.Rect rect2) {
        com.pdftron.pdf.Rect rectUnion = null;
        try {
            rectUnion = new com.pdftron.pdf.Rect();
            rectUnion.setX1(Math.min(rect1.getX1(), rect2.getX1()));
            rectUnion.setY1(Math.min(rect1.getY1(), rect2.getY1()));
            rectUnion.setX2(Math.max(rect1.getX2(), rect2.getX2()));
            rectUnion.setY2(Math.max(rect1.getY2(), rect2.getY2()));
        } catch (PDFNetException e) {
            e.printStackTrace();
        }

        return rectUnion;
    }

    public static com.pdftron.pdf.Rect getTextBBoxOnPage(PDFViewCtrl pdfViewCtrl, int pageNum, com.pdftron.pdf.Point targetPoint) {
        try {
            Page page = pdfViewCtrl.getDoc().getPage(pageNum);
            com.pdftron.pdf.Rect pageCropOnClientF = page.getBox(pdfViewCtrl.getPageBox());
            pageCropOnClientF.normalize();

            // point is somehow outside of the page bounding box
            if (targetPoint.x < pageCropOnClientF.getX1()) {
                targetPoint.x = (float) pageCropOnClientF.getX1();
            }
            if (targetPoint.y < pageCropOnClientF.getY1()) {
                targetPoint.y = (float) pageCropOnClientF.getY1();
            }
            if (targetPoint.x > pageCropOnClientF.getX2()) {
                targetPoint.x = (float) pageCropOnClientF.getX2();
            }
            if (targetPoint.y > pageCropOnClientF.getY2()) {
                targetPoint.y = (float) pageCropOnClientF.getY2();
            }

            // determine what the bounds are based on the rotation
            int pageRotation = pdfViewCtrl.getDoc().getPage(pageNum).getRotation();
            int viewRotation = pdfViewCtrl.getPageRotation();
            int annotRotation = ((pageRotation + viewRotation) % 4) * 90;

            double left, top, right, bottom;
            left = targetPoint.x;
            top = targetPoint.y;
            if (annotRotation == 0) {
                right = pageCropOnClientF.getX2();
                bottom = pageCropOnClientF.getY1();
            } else if (annotRotation == 90) {
                right = pageCropOnClientF.getX2();
                bottom = pageCropOnClientF.getY2();
            } else if (annotRotation == 180) {
                right = pageCropOnClientF.getX1();
                bottom = pageCropOnClientF.getY2();
            } else {
                right = pageCropOnClientF.getX1();
                bottom = pageCropOnClientF.getY1();
            }

            if (Math.abs(right - left) < THRESHOLD_FROM_PAGE_EDGE) {
                left = right - THRESHOLD_FROM_PAGE_EDGE;
            }
            if (Math.abs(top - bottom) < THRESHOLD_FROM_PAGE_EDGE) {
                top = bottom + THRESHOLD_FROM_PAGE_EDGE;
            }

            com.pdftron.pdf.Rect rect = new com.pdftron.pdf.Rect(left, top, right, bottom);
            rect.normalize();
            return rect;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Initializes the free text.
     *
     * @param point The new target point
     */
    @SuppressWarnings("WeakerAccess")
    public void initFreeText(PointF point) {
        mAnnotPushedBack = false;
        initTextStyle();
        setTargetPoints(point);
        createFreeText();
    }

    /**
     * The overload implementation of {@link Tool#onClose()}.
     */
    @Override
    public void onClose() {
        super.onClose();
        mOnCloseOccurred = true;

        // save dialog version first as we use inline version for appearance
        if (mDialogFreeTextNote != null && mDialogFreeTextNote.isShowing()) {
            // force to save the content
            mAnnotButtonPressed = DialogInterface.BUTTON_POSITIVE;
            prepareDialogFreeTextNoteDismiss();
            mDialogFreeTextNote.dismiss();
        }

        if (mInlineEditText != null && mInlineEditText.isEditing()) {
            saveAndQuitInlineEditText(false);
        }

        if (mNextToolMode != ToolMode.ANNOT_EDIT) {
            unsetAnnot();
        }

        // hide soft keyboard
        Utils.hideSoftKeyboard(mPdfViewCtrl.getContext(), mPdfViewCtrl);
    }

    /**
     * The overload implementation of {@link DialogAnnotNote.DialogAnnotNoteListener#onAnnotButtonPressed(int)}.
     */
    @Override
    public void onAnnotButtonPressed(int button) {
        mAnnotButtonPressed = button;
    }

    protected void createFreeText() {
        try {
            mAnnotPushedBack = true;

            if (mPageNum < 1) {
                mPageNum = mPdfViewCtrl.getCurrentPage();
            }

            // get last used free text editing preference and use that for creating the annot
            mCurrentEditMode = getEditMode();
            String cacheStr = null;
            if (Utils.cacheFileExists(mPdfViewCtrl.getContext(), mCacheFileName)) {
                JSONObject cacheJson = Utils.retrieveToolCache(mPdfViewCtrl.getContext(), mCacheFileName);
                if (cacheJson != null) {
                    cacheStr = cacheJson.getString("contents");
                }
            }
            // if phone in landscape mode, the default Android full screen keyboard will appear, so use the popup method
            if (!Utils.isTablet(mPdfViewCtrl.getContext()) &&
                    mPdfViewCtrl.getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                fallbackDialog(cacheStr, true);
            } else if (mCurrentEditMode == ANNOTATION_FREE_TEXT_PREFERENCE_DIALOG) {
                fallbackDialog(cacheStr, false);
            } else {
                inlineTextEditing(cacheStr);
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex, "createFreeText");
        }
    }

    protected int getEditMode() {
        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        return settings.getInt(ANNOTATION_FREE_TEXT_PREFERENCE_EDITING, ANNOTATION_FREE_TEXT_PREFERENCE_EDITING_DEFAULT);
    }

    /**
     * The overload implementation of {@link InlineEditText.InlineEditTextListener#getInlineEditTextPosition()}.
     */
    @Override
    public RectF getInlineEditTextPosition() {
        // position edit text such that the upper left corner (upper right
        // corner if RTL) aligns with where the user tapped and the lower right
        // corner (lower left corner if RTL) is the bottom right corner of the
        // page or PDFView, whichever is smaller.
        int left, top, right, bottom;
        left = right = Math.round(mTargetPointCanvasSpace.x); // points contain PDFView Scroll
        top = Math.round(mTargetPointCanvasSpace.y);

        // get PDFView's height and width
        int viewBottom = mPdfViewCtrl.getHeight() + mPdfViewCtrl.getScrollY();
        int viewRight = mPdfViewCtrl.getWidth() + mPdfViewCtrl.getScrollX();
        int viewLeft = mPdfViewCtrl.getScrollX();

        // get page right and bottom edge
        RectF pageCropOnClientF = Utils.buildPageBoundBoxOnClient(mPdfViewCtrl, mPageNum);
        if (pageCropOnClientF != null) {
            int pageRight = Math.round(pageCropOnClientF.right);
            int pageLeft = Math.round(pageCropOnClientF.left);
            int pageBottom = Math.round(pageCropOnClientF.bottom);

            if (mPdfViewCtrl.getRightToLeftLanguage()) {
                left = viewLeft > pageLeft ? viewLeft : pageLeft;
            } else {
                right = viewRight < pageRight ? viewRight : pageRight;
            }
            bottom = viewBottom < pageBottom ? viewBottom : pageBottom;
        } else {
            // if we can't get the page bottom and right/left edge, we default
            // to using the pdfView's bottom and right/left edge
            if (mPdfViewCtrl.getRightToLeftLanguage()) {
                left = viewLeft;
            } else {
                right = viewRight;
            }
            bottom = viewBottom;
        }

        return new RectF(left, top, right, bottom);
    }

    /**
     * The overload implementation of {@link Tool#onConfigurationChanged(Configuration)}.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mInlineEditText != null && mInlineEditText.isEditing()) {
            saveAndQuitInlineEditText(false);
        }
    }

    /**
     * The overload implementation of {@link InlineEditText.InlineEditTextListener#toggleToFreeTextDialog(String)}.
     */
    @Override
    public void toggleToFreeTextDialog(String interimText) {
        mCurrentEditMode = ANNOTATION_FREE_TEXT_PREFERENCE_DIALOG;
        mUpdateEditMode = true;
        fallbackDialog(interimText, false);
        if (mInlineEditText != null && mInlineEditText.isEditing()) {
            mInlineEditText.getEditText().setCursorVisible(false);
        }
    }

    protected void setNextToolMode() {
        if (mAnnot != null && ((ToolManager) mPdfViewCtrl.getToolManager()).isAutoSelectAnnotation()) {
            mNextToolMode = ToolMode.ANNOT_EDIT;
            setCurrentDefaultToolModeHelper(getToolMode());
        } else if (mForceSameNextToolMode) {
            mNextToolMode = getToolMode();
        } else {
            mNextToolMode = ToolMode.PAN;
            if (!mOnCloseOccurred) {
                ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                ToolManager.Tool tool = toolManager.createTool(mNextToolMode, null);
                toolManager.setTool(tool);
            }
        }
    }

    private void setTargetPoints(PointF point) {
        mPageNum = mPdfViewCtrl.getPageNumberFromScreenPt(point.x, point.y);

        // set the target point in both canvas and page space
        mTargetPointCanvasSpace.x = point.x + mPdfViewCtrl.getScrollX();
        mTargetPointCanvasSpace.y = point.y + mPdfViewCtrl.getScrollY();

        double[] pagePt = mPdfViewCtrl.convScreenPtToPagePt(point.x, point.y, mPageNum);
        mTargetPointPageSpace = new com.pdftron.pdf.Point((int) pagePt[0], (int) pagePt[1]);

        mStoredPointX = point.x;
        mStoredPointY = point.y;
    }

    /**
     * The overload implementation of {@link Tool#onPageTurning(int, int)}.
     */
    @Override
    public void onPageTurning(int old_page, int cur_page) {
        super.onPageTurning(old_page, cur_page);
        saveAndQuitInlineEditText(false);
    }

    protected void saveAndQuitInlineEditText(boolean immediateEditTextRemoval) {
        if (mRichTextViewModel != null) {
            mRichTextViewModel.onCloseToolbar();
        }
        if (mPdfViewCtrl.isAnnotationLayerEnabled()) {
            // if we are using separate annotation layer, always remove immediately
            immediateEditTextRemoval = true;
        }
        if (mInlineEditText != null) {
            final String text = mInlineEditText.getContents();
            if (!TextUtils.isEmpty(text)) {
                commitFreeTextImpl(text, immediateEditTextRemoval);
            } else {
                Utils.deleteCacheFile(mPdfViewCtrl.getContext(), mCacheFileName);
                quitInlineEditText();
            }
        }

        // save new edit mode in settings
        if (mUpdateEditMode) {
            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(ANNOTATION_FREE_TEXT_PREFERENCE_EDITING, mCurrentEditMode);
            editor.apply();
        }
    }

    protected void commitFreeTextImpl(String text, boolean immediateEditTextRemoval) {
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            createAnnot(text);

            if (mInlineEditText != null) {
                if (mInlineEditText.getRichEditor().getVisibility() == View.VISIBLE) {
                    AnnotUtils.createRCFreeTextAppearance(
                            mInlineEditText.getRichEditor(),
                            mPdfViewCtrl,
                            mAnnot,
                            mAnnotPageNum,
                            mAnnotStyle
                    );
                    mPdfViewCtrl.update(mAnnot, mPageNum);
                } else if (mUseEditTextAppearance &&
                        mInlineEditText.getEditText().getVisibility() == View.VISIBLE) {
                    // calculate the bbox on screen to be used directly as annotation bbox
                    int[] screenPos = new int[2];
                    mInlineEditText.getEditText().getLocationOnScreen(screenPos);
                    int offsetX = screenPos[0];
                    int offsetY = screenPos[1];
                    mPdfViewCtrl.getLocationOnScreen(screenPos);
                    offsetX -= screenPos[0];
                    offsetY -= screenPos[1];
                    Rect bbox = mInlineEditText.getEditText().getBoundingRect();
                    try {
                        if (bbox != null) {
                            bbox = new Rect(offsetX + bbox.getX1(), offsetY + bbox.getY1(), offsetX + bbox.getX2(), offsetY + bbox.getY2());
                        }
                    } catch (Exception ignored) {
                    }

                    AnnotUtils.createCustomFreeTextAppearance(
                            mInlineEditText.getEditText(),
                            mPdfViewCtrl,
                            mAnnot,
                            mAnnotPageNum,
                            bbox,
                            true
                    );
                    mPdfViewCtrl.update(mAnnot, mPageNum);
                }
            }

            raiseAnnotationAddedEvent(mAnnot, mAnnotPageNum);

            if (mInlineEditText != null) {
                mInlineEditText.close(immediateEditTextRemoval);
            }
            Utils.deleteCacheFile(mPdfViewCtrl.getContext(), mCacheFileName);

            if (!immediateEditTextRemoval) {
                addOldTools();
            }
        } catch (Exception e) {
            ((ToolManager) mPdfViewCtrl.getToolManager()).annotationCouldNotBeAdded(e.getMessage());
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            if (mInlineEditText != null) {
                mInlineEditText.removeView();
            }
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }

            if (immediateEditTextRemoval) {
                mInlineEditText = null;
            }
            // set the tool mode
            setNextToolMode();
        }
    }

    private void quitInlineEditText() {
        mInlineEditText.close(true);
        mInlineEditText = null;

        // reset the tool mode
        setNextToolMode();
    }

    /**
     * @return True if it is inline edit text and
     */
    protected boolean isFreeTextEditing() {
        if (mInlineEditText != null) {
            return mInlineEditText.isEditing();
        }
        return false;
    }

    protected void inlineTextEditing(String interimText) {
        // override next tool mode
        setNextToolModeHelper(ToolMode.PAN);
        mNextToolMode = getToolMode();
        // reset onUp here because we will call onUp in inline box later
        mOnUpOccurred = false;

        if (mRichTextViewModel != null && mRichContentEnabled) {
            mRichTextViewModel.onOpenToolbar();
        }

        // create inline edit text
        mInlineEditText = new InlineEditText(mPdfViewCtrl, null, mPageNum, mTargetPointPageSpace, mFreeTextInlineToggleEnabled, mRichContentEnabled, this);
        mInlineEditText.setRichTextViewModel(mRichTextViewModel);
        mInlineEditText.addTextWatcher(this);
        // Here we only set horizontal alignment because auto resize is true.
        if (!mRichContentEnabled && mPdfViewCtrl.getToolManager() != null) {
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            if (!toolManager.isAutoResizeFreeText()) {
                mInlineEditText.setCalculateAlignment(true);
                mInlineEditText.getEditText().setHorizontalTextAlignment(mHorizontalAlignment);
            }
        }
        // change style of text to match style of free text annot
        // font size
        mInlineEditText.setTextSize((int) mTextSize);
        // keyboard shortcut
        mInlineEditText.getEditText().setAutoScrollEditTextListener(new AutoScrollEditText.AutoScrollEditTextListener() {
            @Override
            public boolean onKeyUp(int keyCode, KeyEvent event) {
                if (ShortcutHelper.isCommitText(keyCode, event)) {
                    // if DialogFreeTextNote is open then it swallows keys event, hence we only handle
                    // inline edit
                    saveAndQuitInlineEditText(false);
                    // hide soft keyboard
                    InputMethodManager imm = (InputMethodManager) mPdfViewCtrl.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(mPdfViewCtrl.getRootView().getWindowToken(), 0);
                    }
                }
                return true;
            }

            @Override
            public boolean onKeyPreIme(int keyCode, KeyEvent event) {
                return false;
            }
        });
        mInlineEditText.getEditText().setUseAutoResize(true);

        // font color
        int r = Color.red(mTextColor);
        int g = Color.green(mTextColor);
        int b = Color.blue(mTextColor);
        int opacity = (int) (mOpacity * 0xFF);
        int fontColor = Color.argb(opacity, r, g, b);
        mInlineEditText.setTextColor(fontColor);

        // background color
        mInlineEditText.setBackgroundColor(Color.TRANSPARENT);

        // set contents if necessary
        if (interimText != null) {
            mInlineEditText.setContents(interimText);
        }
    }

    private void fallbackDialog(String interimText, boolean disableToggleButton) {
        boolean enableSave = true;
        if (interimText == null) {
            interimText = "";
            enableSave = false;
        }

        mDialogFreeTextNote = new DialogFreeTextNote(mPdfViewCtrl, interimText, enableSave);
        mDialogFreeTextNote.setHorizontalTextAlignment(mHorizontalAlignment);
        mDialogFreeTextNote.setVerticalTextAlignment(mVerticalAlignment);
        mDialogFreeTextNote.addTextWatcher(this);
        mDialogFreeTextNote.setAnnotNoteListener(this);
        mDialogFreeTextNote.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                prepareDialogFreeTextNoteDismiss();
            }
        });

        mDialogFreeTextNote.show();
        if (disableToggleButton) {
            mDialogFreeTextNote.disableToggleButton();
        }

        if (mInlineEditText == null) {
            // always add the inline text
            inlineTextEditing(interimText);
        }
    }

    private void prepareDialogFreeTextNoteDismiss() {
        if (mPdfViewCtrl == null || mDialogFreeTextNote == null) {
            return;
        }

        if (mAnnotButtonPressed == DialogInterface.BUTTON_POSITIVE) {
            boolean shouldUnlock = false;
            try {
                mPdfViewCtrl.docLock(true);
                shouldUnlock = true;
                if (!TextUtils.isEmpty(mDialogFreeTextNote.getNote())) {
                    if (mInlineEditText != null) {
                        mInlineEditText.setContents(mDialogFreeTextNote.getNote());
                    }
                    saveAndQuitInlineEditText(true);
                }
                Utils.deleteCacheFile(mPdfViewCtrl.getContext(), mCacheFileName);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    mPdfViewCtrl.docUnlock();
                }
            }

            if (mInlineEditText != null && mInlineEditText.isEditing()) {
                quitInlineEditText();
            } else {
                // set the next tool mode
                setNextToolMode();
            }

            // save new edit mode in settings
            if (mUpdateEditMode) {
                SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt(ANNOTATION_FREE_TEXT_PREFERENCE_EDITING, mCurrentEditMode);
                editor.apply();
            }
            Utils.hideSoftKeyboard(mPdfViewCtrl.getContext(), mPdfViewCtrl);
        } else if (mAnnotButtonPressed == DialogInterface.BUTTON_NEUTRAL) {
            mCurrentEditMode = ANNOTATION_FREE_TEXT_PREFERENCE_INLINE;
            mUpdateEditMode = true;
            if (mInlineEditText != null) {
                mInlineEditText.setContents(mDialogFreeTextNote.getNote());

                // force show keyboard
                Utils.showSoftKeyboard(mPdfViewCtrl.getContext(), null);
            } else {
                inlineTextEditing(mDialogFreeTextNote.getNote());
            }
        } else {
            if (mInlineEditText != null && mInlineEditText.isEditing()) {
                quitInlineEditText();
            } else {
                // set the next tool mode
                setNextToolMode();
            }

            // save new edit mode in settings
            if (mUpdateEditMode) {
                SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt(ANNOTATION_FREE_TEXT_PREFERENCE_EDITING, mCurrentEditMode);
                editor.apply();
            }

            Utils.deleteCacheFile(mPdfViewCtrl.getContext(), mCacheFileName);
        }
        mAnnotButtonPressed = 0;
    }

    protected void createAnnot(String contents) throws PDFNetException, JSONException {
        // set edit text bounding box
        Rect bbox = getTextBBoxOnPage(mPdfViewCtrl, mPageNum, mTargetPointPageSpace);
        if (bbox == null) {
            return;
        }

        FreeText freeText = FreeText.create(mPdfViewCtrl.getDoc(), bbox);

        freeText.setContents(contents);
        freeText.setFontSize(mTextSize);
        boolean isRightToLeft = false;
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        if (toolManager.isAutoResizeFreeText()) {
            isRightToLeft = Utils.isRightToLeftString(contents);
            if (isRightToLeft) {
                freeText.setQuaddingFormat(2); // right justification
            }
        } else {
            FreeTextAlignmentUtils.setHorizontalAlignment(freeText, mHorizontalAlignment);
            FreeTextAlignmentUtils.setVerticalAlignment(freeText, mVerticalAlignment);
        }
        if (mFillColor == Color.TRANSPARENT) {
            freeText.setColor(new ColorPt(0, 0, 0, 0), 0);
        } else {
            ColorPt colorPt = Utils.color2ColorPt(mFillColor);
            freeText.setColor(colorPt, 3);
        }
        freeText.setOpacity(mOpacity);

        // Set border style and color
        float thickness = mThickness;

        if (mStrokeColor == Color.TRANSPARENT) {
            freeText.setLineColor(new ColorPt(0, 0, 0, 0), 0);
            thickness = 0;
            freeText.getSDFObj().putNumber(PDFTRON_THICKNESS, mThickness);
        } else {
            freeText.setLineColor(Utils.color2ColorPt(mStrokeColor), 3);
        }

        Annot.BorderStyle border = freeText.getBorderStyle();
        border.setWidth(thickness);
        freeText.setBorderStyle(border);
        ColorPt color = Utils.color2ColorPt(mTextColor);
        freeText.setTextColor(color, 3);
        freeText.refreshAppearance();
        FreeTextInfo.setFont(mPdfViewCtrl, freeText, mPDFTronFontName);

        setAuthor(freeText);

        bbox = getFreeTextBBox(freeText, isRightToLeft);
        bbox.normalize();

        // take border thickness into account
        // FreeText.cpp in core seems to add 1.5*bs.width as padding
        double padding = thickness * 1.5;

        // multiply thickness by .5 because in core "half of the border is outside the rect, half inside"
        bbox = new Rect(bbox.getX1(), bbox.getY1() - padding * 2 - thickness * .5, bbox.getX2() + padding * 2 + thickness * .5, bbox.getY2());

        freeText.resize(bbox);

        Page page = mPdfViewCtrl.getDoc().getPage(mPageNum);
        page.annotPushBack(freeText);

        // rotate the annotation based on the users perspective, so
        // that it always faces down towards the user
        int pageRotation = mPdfViewCtrl.getDoc().getPage(mPageNum).getRotation();
        int viewRotation = mPdfViewCtrl.getPageRotation();
        int annotRotation = ((pageRotation + viewRotation) % 4) * 90;
        freeText.setRotation(annotRotation);

        setExtraFreeTextProps(freeText, bbox);

        freeText.refreshAppearance();

        setAnnot(freeText, mPageNum);
        buildAnnotBBox();

        mPdfViewCtrl.update(mAnnot, mPageNum);
    }

    protected Rect getFreeTextBBox(FreeText freeText, boolean isRightToLeft) throws PDFNetException {
        return calcFreeTextBBox(mPdfViewCtrl, freeText, mPageNum,
                isRightToLeft, mTargetPointPageSpace);
    }

    protected void setExtraFreeTextProps(FreeText freetext, Rect bbox) throws PDFNetException {
        // used for advanced freetext type such as callout annotation
    }

    public static Rect calcFreeTextBBox(PDFViewCtrl pdfViewCtrl, FreeText freeText, int pageNum,
            boolean isRightToLeft, com.pdftron.pdf.Point targetPoint) throws PDFNetException {
        // Get the annotation's content stream and iterate through elements to union
        // their bounding boxes
        Obj contentStream = freeText.getSDFObj().findObj("AP").findObj("N");
        ElementReader er = new ElementReader();
        Rect unionRect = null;
        Element element;

        er.begin(contentStream);
        for (element = er.next(); element != null; element = er.next()) {
            Rect rect = element.getBBox();
            if (rect != null && element.getType() == Element.e_text) {
                if (unionRect == null) {
                    unionRect = rect;
                }
                unionRect = getRectUnion(rect, unionRect);
            }
        }
        er.end();
        er.destroy();

        // get the page rotation from the users perspective
        int pageRotation = pdfViewCtrl.getDoc().getPage(pageNum).getRotation();
        int viewRotation = pdfViewCtrl.getPageRotation();
        int annotRotation = ((pageRotation + viewRotation) % 4) * 90;

        double xDist = 0;
        double yDist = 0;
        double left, bottom, right, top;
        if (unionRect != null) {
            unionRect.normalize();
            // position the unionRect in the correct location on the
            // page based on the pages rotation from the users perspective

            // get the height/width of the unionRect and swap as necessary
            // for rotation
            if (annotRotation == 90 || annotRotation == 270) {
                xDist = unionRect.getHeight();
                yDist = unionRect.getWidth();
            } else {
                xDist = unionRect.getWidth();
                yDist = unionRect.getHeight();
            }
        }

        // grow the edit text to ensure all text is visible
        if (xDist == 0 || yDist == 0) {
            xDist = 60;
            yDist = 60;
        } else {
            xDist += 25;
            yDist += 5;
        }

        // add or subtract the height and width from the target point
        // as necessary based on rotation. This way the edit text will grow
        // in the correct direct in relation to the target point (where the user
        // taps down on the screen)
        if (annotRotation == 90) {
            if (pdfViewCtrl.getRightToLeftLanguage()) {
                xDist *= -1;
            } else {
                yDist *= -1;
            }
        } else if (annotRotation == 270) {
            if (pdfViewCtrl.getRightToLeftLanguage()) {
                yDist *= -1;
            } else {
                xDist *= -1;
            }
        } else if (annotRotation == 180) {
            xDist *= -1; // multiply by -1, so that the bbox grows downwards
            yDist *= -1;
        }

        // set the bounding box using the target point and
        // size of the text
        left = targetPoint.x - (isRightToLeft ? xDist : 0);
        top = targetPoint.y;
        right = targetPoint.x + (isRightToLeft ? 0 : xDist);
        bottom = targetPoint.y - yDist;

        // normalize the bounding box
        Rect rect = new Rect(left, top, right, bottom);
        rect.normalize();
        left = rect.getX1();
        top = rect.getY1();
        right = rect.getX2();
        bottom = rect.getY2();

        // Let's make sure we do not go beyond page borders
        Page page = pdfViewCtrl.getDoc().getPage(pageNum);
        com.pdftron.pdf.Rect pageCropOnClientF = page.getBox(pdfViewCtrl.getPageBox());
        pageCropOnClientF.normalize();

        if (left < pageCropOnClientF.getX1()) {
            left = pageCropOnClientF.getX1();
        }
        if (top < pageCropOnClientF.getY1()) {
            top = pageCropOnClientF.getY1();
        }
        if (right > pageCropOnClientF.getX2()) {
            right = pageCropOnClientF.getX2();
        }
        if (bottom > pageCropOnClientF.getY2()) {
            bottom = pageCropOnClientF.getY2();
        }

        // and that we have a visible bounding box when
        // inserting the free text close to the border
        if (Math.abs(pageCropOnClientF.getY1() - top) < THRESHOLD_FROM_PAGE_EDGE) {
            top = pageCropOnClientF.getY1() + THRESHOLD_FROM_PAGE_EDGE;
        }
        if (Math.abs(pageCropOnClientF.getX2() - right) < THRESHOLD_FROM_PAGE_EDGE) {
            right = pageCropOnClientF.getX2() - THRESHOLD_FROM_PAGE_EDGE;
        }

        if (right - left < MINIMUM_BBOX_WIDTH) {
            right = left + MINIMUM_BBOX_WIDTH;
        }
        if (right > pageCropOnClientF.getX2()) {
            right = pageCropOnClientF.getX2();
            left = right - MINIMUM_BBOX_WIDTH;
        }
        return new Rect(left, top, right, bottom);
    }

    /**
     * The overload implementation of {@link Tool#onRenderingFinished()}.
     */
    @Override
    public void onRenderingFinished() {
        super.onRenderingFinished();

        if (mInlineEditText != null && mInlineEditText.delayViewRemoval()) {
            mInlineEditText.removeView();
            mInlineEditText = null;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        long currentTimeStamp = System.currentTimeMillis();
        if (currentTimeStamp - mStoredTimeStamp > 3000) {
            mStoredTimeStamp = currentTimeStamp;
            if (s != null && s.length() > 0) {
                FreeTextCacheStruct freeTextCacheStruct = new FreeTextCacheStruct();
                freeTextCacheStruct.contents = s.toString();
                freeTextCacheStruct.pageNum = mPageNum;
                freeTextCacheStruct.x = mStoredPointX;
                freeTextCacheStruct.y = mStoredPointY;
                AnnotUtils.saveFreeTextCache(freeTextCacheStruct, mPdfViewCtrl);
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    protected void initTextStyle() {
        Context context = mPdfViewCtrl.getContext();
        SharedPreferences settings = Tool.getToolPreferences(context);
        mTextColor = settings.getInt(getTextColorKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultTextColor(context, getCreateAnnotType()));
        mTextSize = settings.getFloat(getTextSizeKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultTextSize(context, getCreateAnnotType()));
        mStrokeColor = settings.getInt(getColorKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultColor(context, getCreateAnnotType()));
        mThickness = settings.getFloat(getThicknessKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultThickness(context, getCreateAnnotType()));
        mFillColor = settings.getInt(getColorFillKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultFillColor(context, getCreateAnnotType()));
        mOpacity = settings.getFloat(getOpacityKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultOpacity(context, getCreateAnnotType()));
        mPDFTronFontName = settings.getString(getFontKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultFont(context, getCreateAnnotType()));
        mHorizontalAlignment = settings.getInt(getHorizontalAlignmentKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultHorizontalAlignment(context, getCreateAnnotType()));
        mVerticalAlignment = settings.getInt(getVerticalAlignmentKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultVerticalAlignment(context, getCreateAnnotType()));
    }

    /**
     * Used to set the coordinates for the default position of the FreeText as custom data in the annotation.
     * This custom data is used if {@link ToolManager#isDeleteEmptyFreeText()} is false and
     * {@link ToolManager#isAutoResizeFreeText()} is true. When the FreeText has no text content (empty text),
     * the FreeText resizes to return to the specified default size.
     * <p>
     * This method must be called in a write lock.
     *
     * @param freeText    the FreeText annotation to add the default Rect
     * @param defaultRect the rect containing coordinates for the default position of the FreeText
     * @throws PDFNetException
     */
    public static void putDefaultRect(@NonNull FreeText freeText, @NonNull Rect defaultRect) throws PDFNetException {
        freeText.setCustomData(DEFAULT_RECT_X1_KEY, String.valueOf(defaultRect.getX1()));
        freeText.setCustomData(DEFAULT_RECT_Y1_KEY, String.valueOf(defaultRect.getY1()));
        freeText.setCustomData(DEFAULT_RECT_X2_KEY, String.valueOf(defaultRect.getX2()));
        freeText.setCustomData(DEFAULT_RECT_Y2_KEY, String.valueOf(defaultRect.getY2()));
    }

    /**
     * Returns the Rect containing coordinates for the default position of the FreeText, null
     * if not available. See {@link #putDefaultRect(FreeText, Rect)} for more info.
     * <p>
     * This method must be called in a read lock.
     *
     * @param freeText the FreeText annotation to get the default Rect
     * @return Rect containing the coordiantes for the default position of the FreeText, null if not available.
     * @throws PDFNetException
     */
    @Nullable
    public static Rect getDefaultRect(@NonNull FreeText freeText) throws PDFNetException {
        String origX1Str = freeText.getCustomData(DEFAULT_RECT_X1_KEY);
        String origY1Str = freeText.getCustomData(DEFAULT_RECT_Y1_KEY);
        String origX2Str = freeText.getCustomData(DEFAULT_RECT_X2_KEY);
        String origY2Str = freeText.getCustomData(DEFAULT_RECT_Y2_KEY);
        try {
            double origX1 = Double.parseDouble(origX1Str);
            double origY1 = Double.parseDouble(origY1Str);
            double origX2 = Double.parseDouble(origX2Str);
            double origY2 = Double.parseDouble(origY2Str);
            return new Rect(origX1, origY1, origX2, origY2);
        } catch (NullPointerException | NumberFormatException e) {
            return null;
        }
    }
}
