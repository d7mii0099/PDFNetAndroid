//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.preference.PreferenceManager;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Ink;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.controls.OnToolbarStateUpdateListener;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.ink.InkItem;
import com.pdftron.pdf.model.ink.PressureInkItem;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.PressureInkUtils;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.sdf.Obj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.subjects.PublishSubject;

/**
 * This class is for creating a free hand annotation.
 * <p>
 * If mMultiStrokeMode is true, then each subsequent stroke will be added to the same annot.
 * <p>
 * If mTimedModeEnabled is true, then the strokes will be committed to the annot on a timer basis. This flag
 * is only under effect if mMultiStrokeMode is true.
 * <p>
 * If mIsFromEditToolbar is true, then mMultiStrokeMode must be false and mTimedModeEnabled must be false.
 * <p>
 * # Test cases:
 * <p>
 * ## Stylus as Pen:
 * ### Invariants
 * mTimedModeEnabled can be true or false
 * mMultiStrokeMode can be true or false
 * mIsEditingAnnot must be false
 * ### Tests
 * 1. mTimedModeEnabled = true and mMultiStrokeMode = true. A new annot should not be created each stroke,
 * and strokes should be saved every 3 seconds.
 * 2. mMultiStrokeMode = false. A new annot will be created after every stroke.
 * <p>
 * ## Ink tool from toolbar:
 * ### Invariants
 * mTimedModeEnabled can be true or false
 * mMultiStrokeMode can be true or false
 * mIsEditingAnnot must be false
 * ### Tests
 * 1. mTimedModeEnabled = true and mMultiStrokeMode = true. A new annot should not be created each stroke,
 * and strokes should be saved every 30 seconds.
 * 2. mMultiStrokeMode = false. A new annot will be created after every stroke.
 * <p>
 * ## Ink tool from quick menu:
 * ### Invariants
 * mTimedModeEnabled can be true or false
 * mMultiStrokeMode must be true
 * mIsEditingAnnot must be false
 * 1. mTimedModeEnabled = true and mMultiStrokeMode = true. A new annot should not be created each stroke,
 * and strokes should be saved every 30 seconds.
 * <p>
 * <p>
 * ## Draw with finger setting
 * <p>
 * ### 1. Draw with finger setting enabled:
 * i. Enable stylus as pen setting and **enable** draw with finger setting.
 * ii. Open a document and select the ink tool.
 * iii. When you draw with the finger, ink should be created. However once you start drawing with the stylus, the finger should only pan.
 * <p>
 * ### 2. Draw with finger setting disabled:
 * i. Enable stylus as pen setting and **disable** draw with finger setting.
 * ii. Open a document and select the ink tool.
 * iii. When you draw with the finger, it should pan.
 * <p>
 * ### 3. Stylus as pen setting disabled:
 * i. Disable stylus as pen setting.
 * ii. Open a document and select the ink tool.
 * iii. When you draw with the finger, ink should be created. However once you start drawing with the stylus, the finger should only pan. Same as expected behavior 1.
 */
@Keep
public class FreehandCreate extends SimpleShapeCreate {

    private static final String TAG = FreehandCreate.class.getName();
    private static boolean sDebug = false;
    // When in stylus ink mode, we will also save and start a new annotation if the user draws far enough from the current ink.
    // set to 0 or <0 in order to disable this. If you disable it, every stroke will count as the same annotation.
    private static final double SAVE_INK_MARGIN = 200;

    // Custom ink flags
    private boolean mIsPressureSensitive = false;

    // Eraser related
    private EraserState mEraserState = new EraserState();
    private boolean mEraserFromSpen = false;
    private boolean mEraserFromToolbar = false;
    private float mEraserThickness = 5.0f; // default value without zoom
    private Eraser.InkEraserMode mInkEraserMode = Eraser.InkEraserMode.PIXEL;

    // Use for drawing
    private CanvasStateManager mCanvasStateManager = new CanvasStateManager();
    private float mPrevX = Float.MAX_VALUE; // previous X and Y are used to filter onMove events based on distance
    private float mPrevY = Float.MAX_VALUE;

    // We set this in onDown and only push to mCanvasStateManager in onUp in case where the onDown touch is a drag/swipe.
    // These gestures are only known after onMove and we do not want to save the state for these gestures, so we have
    // to store this and do it in onDown
    @Nullable
    private InkState mStateToPush = null;

    // Internal flags
    private boolean mIsFromEditToolbar = false;
    private boolean mIsFirstPointNotOnPage;
    private boolean mFlinging;
    private boolean mIsScaleBegun;
    private boolean mIsStartPointOutsidePage;
    private boolean mIsEditingAnnot;
    private boolean mNeedNewAnnot = false; // used to determine whether a new annot should be made on next stroke

    // Editing annotation and page
    private Ink mEditInkAnnot;
    private int mEditInkPageNum;

    private boolean mScrollEventOccurred = true;
    private boolean mRegisteredDownEvent;

    private OnToolbarStateUpdateListener mOnToolbarStateUpdateListener;

    @Nullable
    private InkCommitter mInkCommitter; // lazy initialized in onDown
    public static float sSampleDelta = -1;

    boolean mDrawWithFinger = false;

    /**
     * Class constructor
     */
    public FreehandCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        mNextToolMode = getToolMode();

        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        mPdfViewCtrl.setStylusScaleEnabled(false);
        addOldTools(); // fixes blinking issue when annotations are hidden and shown at the end of inking

        if (sSampleDelta == -1) {
            sSampleDelta = computeThresholdValue(mPdfViewCtrl);
        }

        mDrawWithFinger = PdfViewCtrlSettingsManager.getDrawWithFinger(ctrl.getContext());

        mMultiStrokeMode = ((ToolManager) mPdfViewCtrl.getToolManager()).isInkMultiStrokeEnabled();
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.INK_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Ink;
    }

    /**
     * Sets multiple stroke mode.
     *
     * @param mode True if multiple stroke mode is enabled
     */
    @SuppressWarnings("SameParameterValue")
    public void setMultiStrokeMode(boolean mode) {
        mMultiStrokeMode = mode;
    }

    /**
     * Sets whether allow tap to select another annotation.
     * Only allowed in single stroke mode.
     *
     * @param allowTapToSelect true if allow tap to select another annotation, dot will not be drawn.
     *                         Default to false.
     */
    public void setAllowTapToSelect(boolean allowTapToSelect) {
        mAllowTapToSelect = allowTapToSelect;
    }

    /**
     * Sets whether this tool is used from the editor toolbar.
     *
     * @param fromEditToolbar True if the tool is being used in the edit toolbar
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setFromEditToolbar(boolean fromEditToolbar) {
        mIsFromEditToolbar = fromEditToolbar;
    }

    /**
     * Sets time mode.
     *
     * @param enabled True if time mode is enabled
     */
    @SuppressWarnings("SameParameterValue")
    public void setTimedModeEnabled(boolean enabled) {
        mTimedModeEnabled = enabled;
    }

    /**
     * Sets whether the Ink annotation should be use pressure sensitive data. This is only available for
     * devices that support capacitive touch screen that provide pressure data to MotionEvents.
     * <p>
     * By default Pressure Sensitivity is disabled.
     *
     * @param isPressureSensitive True if pressure sensitivity is enabled.
     */
    public void setPressureSensitive(boolean isPressureSensitive) {
        // If pressure changed, we push a new ink item (one ink item per annot)
        if (this.mIsPressureSensitive != isPressureSensitive) {
            if (mCanvasStateManager.getCurrentState().currentInk != null) { // each ink item corresponds to an annot, if the current ink item is null then we push
                mNeedNewAnnot = true;
            }
        }
        this.mIsPressureSensitive = isPressureSensitive;

        // Save default pressure sensitive setting for stylus as pen
        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(getPressureSensitiveKey(), mIsPressureSensitive);
        editor.apply();
    }

    /**
     * Sets the {@link OnToolbarStateUpdateListener} listener.
     *
     * @param listener the {@link OnToolbarStateUpdateListener} listener
     */
    public void setOnToolbarStateUpdateListener(OnToolbarStateUpdateListener listener) {
        mOnToolbarStateUpdateListener = listener;
    }

    @Override
    public void setupAnnotProperty(AnnotStyle annotStyle) {
        super.setupAnnotProperty(annotStyle);

        boolean isPressure = annotStyle.getPressureSensitive();
        setPressureSensitive(isPressure);
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#setupAnnotProperty(int, float, float, int, String, String)}.
     */
    @Override
    public void setupAnnotProperty(int color, float opacity, float thickness, int fillColor, String icon, String pdfTronFontName) {
        // if the stroke has a different style than the previous stroke, create a
        // new ink and update the paint style
        if (mStrokeColor != color || mOpacity != opacity || mThickness != thickness) {
            super.setupAnnotProperty(color, opacity, thickness, fillColor, icon, pdfTronFontName);

            float zoom = (float) mPdfViewCtrl.getZoom();
            mThicknessDraw = mThickness * zoom;
            mPaint.setStrokeWidth(mThicknessDraw);
            color = Utils.getPostProcessedColor(mPdfViewCtrl, mStrokeColor);
            mPaint.setColor(color);
            mPaint.setAlpha((int) (255 * mOpacity));

            // If style changed, we push a new ink item (one ink item per annot)
            if (mCanvasStateManager.getCurrentState().currentInk != null) {
                mNeedNewAnnot = true;
            }
        }
        mEraserFromToolbar = false;
        mEraserFromSpen = false;
    }

    /**
     * Setups eraser property.
     *
     * @param annotStyle The annot style
     */
    public void setupEraserProperty(AnnotStyle annotStyle) {
        float thickness = annotStyle.getThickness();
        Eraser.InkEraserMode inkEraserMode = annotStyle.getInkEraserMode();
        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat(getThicknessKey(AnnotStyle.CUSTOM_ANNOT_TYPE_ERASER), thickness);
        editor.putString(getEraserTypeKey(AnnotStyle.CUSTOM_ANNOT_TYPE_ERASER), annotStyle.getEraserType().name());
        editor.putString(getInkEraserModeKey(AnnotStyle.CUSTOM_ANNOT_TYPE_ERASER), inkEraserMode.name());
        editor.apply();

        mEraserThickness = thickness;
        mEraserFromSpen = false;
        mEraserFromToolbar = true;
        mInkEraserMode = inkEraserMode;
    }

    /**
     * Initializes the ink item based on the specified annotation.
     *
     * @param inkAnnot The ink annotation
     * @param pageNum  The page number
     */
    public void setInitInkItem(Annot inkAnnot, int pageNum) {
        try {
            if (inkAnnot == null || inkAnnot.getType() != Annot.e_Ink) {
                return;
            }
        } catch (PDFNetException e) {
            return;
        }

        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;

            Ink castedInkAnnot = new Ink(inkAnnot);
            mIsEditingAnnot = true;

            mEditInkAnnot = castedInkAnnot;
            mEditInkPageNum = pageNum;

            // get ink annot's appearance
            // color
            ColorPt colorPt = mEditInkAnnot.getColorAsRGB();
            int color = Utils.colorPt2color(colorPt);

            // opacity
            Markup m = new Markup(mEditInkAnnot);
            float opacity = (float) m.getOpacity();

            // thickness
            float thickness = (float) mEditInkAnnot.getBorderStyle().getWidth();
            setupAnnotProperty(color, opacity, thickness, color, null, null);

            // draw ink annot in UI and hide actual annot
            setupInitInkItem(mEditInkAnnot, pageNum);

            mPdfViewCtrl.hideAnnotation(mEditInkAnnot);
            mPdfViewCtrl.update(mEditInkAnnot, pageNum);

            mPdfViewCtrl.invalidate();

            // update undo/redo and eraser buttons
            if (mOnToolbarStateUpdateListener != null) {
                mOnToolbarStateUpdateListener.onToolbarStateUpdated();
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
    }

    // Must be called within a read lock
    private void setupInitInkItem(Ink ink, int pageNum) throws PDFNetException {
        // Then push our ink annot state
        mCanvasStateManager.initializeStateForEditing(
                mPdfViewCtrl,
                pageNum,
                mStrokeColor,
                mOpacity,
                mThickness,
                mIsStylus,
                ink);

        // Also initialize ink committer
        mInkCommitter = new InkCommitter(mIsEditingAnnot);
        mInkCommitter.initializeWithInkAnnot(mEditInkAnnot, mCanvasStateManager.getCurrentState());

        // Update UI for undo/redo/clear/erase
        updateEditToolbar();
    }

    @Override
    @SuppressWarnings("WeakerAccess")
    protected void setNextToolModeHelper() {
        super.setNextToolModeHelper();
        if (shouldApplyStylusAsPenBehavior()) {
            mNextToolMode = getToolMode();
        }
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onDown(MotionEvent)}.
     */
    @Override
    public boolean onDown(MotionEvent e) {
        super.onDown(e);

        mPdfViewCtrl.setStylusScaleEnabled(false); // needed due to tool-lopping

        Context context = mPdfViewCtrl.getContext();
        SharedPreferences settings = Tool.getToolPreferences(context);
        mIsPressureSensitive = settings.getBoolean(getPressureSensitiveKey(), ToolStyleConfig.getInstance().getDefaultPressureSensitivity(context, getCreateAnnotType()));
        mScrollEventOccurred = false;

        if ((mStylusUsed || shouldApplyStylusAsPenBehavior()) && !mIsStylus) {
            if (mIsFromEditToolbar) {
                return false;
            } else {
                mNextToolMode = ToolMode.PAN;
                return false;
            }
        }

        mDownPageNum = mPdfViewCtrl.getPageNumberFromScreenPt(e.getX(), e.getY());
        mIsStartPointOutsidePage = mDownPageNum < 1;
        if (mIsStartPointOutsidePage) {
            return false;
        }

        // This check is to ensure that we return to ink mode after spen erasing is finished
        mEraserFromSpen = false;

        // Enable ink mode
        if (Utils.isMarshmallow() &&
                e.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
            int state = e.getButtonState();
            if (state == MotionEvent.BUTTON_STYLUS_PRIMARY) {

                // If we're in edit toolbar, then we do not switch the Eraser tool but use the canvas eraser
                if (mIsFromEditToolbar && mCanvasStateManager.canClear()) {
                    // side button pressed, switch to canvas eraser
                    mEraserFromSpen = true;
                    mEraserThickness = settings.getFloat(getThicknessKey(AnnotStyle.CUSTOM_ANNOT_TYPE_ERASER), mEraserThickness);
                    mInkEraserMode = Eraser.InkEraserMode.valueOf(
                            settings.getString(
                                    getInkEraserModeKey(AnnotStyle.CUSTOM_ANNOT_TYPE_ERASER),
                                    Eraser.InkEraserMode.PIXEL.name()
                            )
                    );
                } else {
                    // side button pressed, switch to eraser tool
                    mNextToolMode = ToolMode.INK_ERASER;
                    setCurrentDefaultToolModeHelper(getToolMode());
                    return false;
                }
            }
        }

        if (mAllowTwoFingerScroll) {
            mRegisteredDownEvent = false;
            return false;
        } else {
            mRegisteredDownEvent = true;
        }

        if (mIsEditingAnnot && mDownPageNum != mEditInkPageNum) {
            return false;
        }

        if (mTimedModeEnabled) {
            if (mIsStylus && e.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) {
                // once in stylus mode, ignore finger touch
                return false;
            }
        }

        // Skip if first point is outside page limits
        if (mPageCropOnClientF != null) {
            if (mPt1.x < mPageCropOnClientF.left ||
                    mPt1.x > mPageCropOnClientF.right ||
                    mPt1.y < mPageCropOnClientF.top ||
                    mPt1.y > mPageCropOnClientF.bottom) {
                if (!mMultiStrokeMode) {
                    setNextToolModeHelper(ToolMode.ANNOT_EDIT);
                } else {
                    mIsFirstPointNotOnPage = true;
                }
                return false;
            } else {
                mIsFirstPointNotOnPage = false;
            }
        }

        if (!mIsFromEditToolbar && mCanvasStateManager.getCurrentState().currentInk != null) { // i.e. stylus as pen
            // check distance of pointer point from current bounding box.
            try {
                Rect rect = mCanvasStateManager.getCurrentState().getBoundingBox();
                if (rect != null) {
                    double[] pt1 = mPdfViewCtrl.convScreenPtToPagePt(e.getX(), e.getY(), mCanvasStateManager.getCurrentState().currentInk.pageNumber);
                    rect.normalize();
                    rect.inflate(SAVE_INK_MARGIN);
                    if (!rect.contains(pt1[0], pt1[1])) {
                        mNeedNewAnnot = true;
                    }
                }
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            }
        }

        if (isEraserEnabled()) {
            mEraserState = new EraserState();
            mEraserState.pushInk(
                    mPdfViewCtrl,
                    mDownPageNum,
                    Color.LTGRAY,
                    0.7f,
                    mEraserThickness,
                    mIsStylus
            );
            if (mCanvasStateManager.getCurrentState().currentInk != null) {
                mStateToPush = new InkState(mCanvasStateManager.getCurrentState());
            }
            mEraserState.addPoint(e.getX(), e.getY(), e.getPressure(), e.getAction());
        } else {
            InkState currentState = mCanvasStateManager.getCurrentState();
            if (currentState.currentInk == null || currentState.currentInk.pageNumber != mDownPageNum || mNeedNewAnnot) {// either annot style changed or page changed
                currentState.pushInk(
                        mPdfViewCtrl,
                        mDownPageNum,
                        mStrokeColor,
                        mOpacity,
                        mThickness,
                        mIsStylus,
                        mIsPressureSensitive
                );
                mNeedNewAnnot = false;
            }
            mStateToPush = new InkState(mCanvasStateManager.getCurrentState());
            currentState.addPoint(e.getX(), e.getY(), e.getPressure(), e.getAction());
            mPrevX = e.getX();
            mPrevY = e.getY();
        }

        // Note this must come after initializing currentState in CanvasStateManager by calling pushInk
        if (mInkCommitter == null) {
            mInkCommitter = new InkCommitter(mIsEditingAnnot);
        } else {
            mInkCommitter.restartIfStopped();
        }

        return false;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        super.onMove(e1, e2, x_dist, y_dist);

        if (mIsStartPointOutsidePage) {
            return false;
        }

        // if we were still scrolling and flinging when the onDown event was called
        if (!mRegisteredDownEvent) {
            return false;
        }

        // We are scrolling
        if (mAllowTwoFingerScroll) {
            return false;
        }

        if (mAllowOneFingerScrollWithStylus) {
            return false;
        }

        if (mIsEditingAnnot && mDownPageNum != mEditInkPageNum) {
            return false;
        }

        if (isEraserEnabled()) {
            processOnMoveHistoricalMotionPoints(e2, true);
        } else {

            // If free hand started out of the page boundaries or the page we are adding the path is
            // different from the page where we started, we just skip
            InkState currentState = mCanvasStateManager.getCurrentState();
            if (currentState.currentInk == null) {
                AnalyticsHandlerAdapter.getInstance().sendException(new Exception("Current ink item is null"));
                return false;
            }
            if (mMultiStrokeMode && (mIsFirstPointNotOnPage || currentState.currentInk.pageNumber != mDownPageNum)) {
                return false;
            }

            processOnMoveHistoricalMotionPoints(e2, false);
        }

        return true;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {

        if (mIsStartPointOutsidePage) {
            return false;
        }

        // if we were still scrolling and flinging when the onDown event was called
        if (!mRegisteredDownEvent) {
            return false;
        }

        // we are flinging
        if (priorEventMode == PDFViewCtrl.PriorEventMode.FLING) {
            mFlinging = true;
        }

        // We are scrolling
        if (mAllowTwoFingerScroll) {
            doneTwoFingerScrolling();
            mScrollEventOccurred = true;
            return false;
        }

        if (priorEventMode == PDFViewCtrl.PriorEventMode.PAGE_SLIDING) {
            return false;
        }

        // If both start point and end point are the same, we don't push back the annotation
        if (mAllowTapToSelect && mPt1.x == mPt2.x && mPt1.y == mPt2.y) {
            if (mCanvasStateManager.getCurrentState().currentInk != null) {
                mCanvasStateManager.getCurrentState().currentInk.reset();
            }
            resetPts();
            return true;
        }

        if (mAllowOneFingerScrollWithStylus) {
            doneOneFingerScrollingWithStylus();
            mScrollEventOccurred = true;
            return false;
        }

        // if the user is scrolling and fires a fling event,
        // onUp will be called twice - the first call will be the fling event
        // on the second call pass the event to the PDFViewCtrl to prevent
        // saving an ink dot.
        if (mScrollEventOccurred) {
            mScrollEventOccurred = false;
            return false;
        }

        if (mIsStylus && e.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) {
            return false;
        }

        if (mStylusUsed && e.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) {
            return false;
        }

        // If annotation was already pushed back, avoid re-entry due to fling motion.
        if (mAnnotPushedBack) {
            mAnnotPushedBack = false;
            return false;
        }

        if (mIsEditingAnnot && (mDownPageNum != mEditInkPageNum)) {
            return false;
        }
        InkState inkState = mCanvasStateManager.getCurrentState();
        if (isEraserEnabled()) {
            float x = e.getX();
            float y = e.getY();

            processEraserMotionPoint(x, y, e.getAction());
            if (mEraserState != null) {
                // Only process the eraser if we have initialized a current state, i.e. we have something to erase
                InkItem currentInk = inkState.currentInk;
                if (currentInk != null) {
                    processEraser(currentInk);
                }
                mEraserState = null;
            }
        } else {
            // If free hand started out of the page boundaries or the page we are adding the path is
            // different from the page where we started, we just skip
            if (inkState.currentInk == null) {
                AnalyticsHandlerAdapter.getInstance().sendException(new Exception("Current ink item is null"));
                return false;
            }
            if (mMultiStrokeMode &&
                    mIsFirstPointNotOnPage || (inkState.currentInk.pageNumber != mDownPageNum)) {
                return false;
            }

            float x = e.getX();
            float y = e.getY();

            processMotionPoint(x, y, e.getPressure(), e.getAction());
        }

        if (mStateToPush != null) { // non null if it is set in onDown
            mCanvasStateManager.saveState(mStateToPush);
            mStateToPush = null;
        }
        mAnnotPushedBack = true;
        updateEditToolbar();

        mPdfViewCtrl.invalidate(); // to handle when there is no move event between down and up

        if (mIsStylus) {
            raiseStylusUsedFirstTimeEvent();
        }

        // We will commit if not multi stroke
        if (!mMultiStrokeMode) {
            if (mInkCommitter != null) {
                commitAnnotation(false);
                addOldTools();
                mNeedNewAnnot = true;
            }
            setNextToolModeHelper();
            setCurrentDefaultToolModeHelper(getToolMode());
        }

        return skipOnUpPriorEvent(priorEventMode);
    }

    @Override
    protected boolean tapToSelectAllowed() {
        return mForceSameNextToolMode && mAllowTapToSelect;
    }

    // Must only be called with ACTION_MOVE event
    private void processOnMoveHistoricalMotionPoints(MotionEvent ev, boolean isEraser) {

        if (isEraser) {
            // Do not process historical points for eraser as this is too many points.
            processEraserMotionPoint(ev.getX(), ev.getY(), ev.getAction());
        } else {
            final float eventX = ev.getX();
            final float eventY = ev.getY();
            final float eventPressure = ev.getPressure();
            final int historySize = ev.getHistorySize();
            final int pointerCount = ev.getPointerCount();

            // Loop through all intermediate points
            // During moving, update the free hand path and the bounding box. Note that for the
            // bounding box, we need to include the previous bounding box in the new bounding box
            // so that the previously drawn free hand will go away.
            for (int h = 0; h < historySize; h++) {
                if (pointerCount >= 1) {

                    float historicalX = ev.getHistoricalX(0, h);
                    float historicalY = ev.getHistoricalY(0, h);
                    float historicalPressure = ev.getHistoricalPressure(0, h);

                    if (distance(historicalX, historicalY, mPrevX, mPrevY) > sSampleDelta
                            && distance(historicalX, historicalY, eventX, eventY) > sSampleDelta) {
                        processMotionPoint(historicalX, historicalY, historicalPressure, ev.getAction());
                    }
                }
            }
            processMotionPoint(eventX, eventY, eventPressure, ev.getAction());
        }
    }

    private float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void processEraserMotionPoint(float x, float y, int action) {
        if (mEraserState != null) {
            mEraserState.addPoint(x, y, -1, action);
        } else {
            AnalyticsHandlerAdapter.getInstance().sendException(new RuntimeException("Eraser state is not initialized"));
        }
    }

    protected void processMotionPoint(float x, float y, float pressure, int action) {
        int sx = mPdfViewCtrl.getScrollX();
        int sy = mPdfViewCtrl.getScrollY();
        float canvasX = x + sx;
        float canvasY = y + sy;

        // Don't allow the annotation to go beyond the page
        if (mPageCropOnClientF != null) {
            if (canvasX < mPageCropOnClientF.left) {
                canvasX = mPageCropOnClientF.left;
                x = canvasX - sx;
            } else if (canvasX > mPageCropOnClientF.right) {
                canvasX = mPageCropOnClientF.right;
                x = canvasX - sx;
            }
            if (canvasY < mPageCropOnClientF.top) {
                canvasY = mPageCropOnClientF.top;
                y = canvasY - sy;
            } else if (canvasY > mPageCropOnClientF.bottom) {
                canvasY = mPageCropOnClientF.bottom;
                y = canvasY - sy;
            }
        }

        mCanvasStateManager.getCurrentState().addPoint(x, y, pressure, action);

        mPt1.x = Math.min(canvasX, mPt1.x);
        mPt1.y = Math.min(canvasY, mPt1.y);
        mPt2.x = Math.max(canvasX, mPt2.x);
        mPt2.y = Math.max(canvasY, mPt2.y);

        if (Utils.isLollipop()) {
            mPdfViewCtrl.invalidate();
        } else {
            // Get the draw region
            float minX = Math.min(mPrevX, x) - 2.0f * mThicknessDraw + sx; // add some padding to account for thickness
            float maxX = Math.max(mPrevX, x) + 2.0f * mThicknessDraw + sx;
            float minY = Math.min(mPrevY, y) - 2.0f * mThicknessDraw + sy;
            float maxY = Math.max(mPrevY, y) + 2.0f * mThicknessDraw + sy;

            // Recalculate the invalidate box by bounding it by the screen rect
            android.graphics.Rect boundingRect = new android.graphics.Rect((int) minX, (int) minY, (int) maxX, (int) maxY);
            android.graphics.Rect drawingRect = new android.graphics.Rect();
            mPdfViewCtrl.getDrawingRect(drawingRect);
            boolean hasIntersect = boundingRect.intersect(drawingRect);

            if (hasIntersect) {
                mPdfViewCtrl.invalidate(boundingRect);
            }
        }

        mPrevX = x;
        mPrevY = y;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onFlingStop()}.
     */
    @Override
    public boolean onFlingStop() {
        super.onFlingStop();
        if (mAllowOneFingerScrollWithStylus) {
            doneOneFingerScrollingWithStylus();
        }
        mFlinging = false;
        mIsScaleBegun = false;
        mPdfViewCtrl.invalidate();
        return false;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onScaleBegin(float, float)}.
     */
    @Override
    public boolean onScaleBegin(float x, float y) {
        mIsScaleBegun = true;
        return super.onScaleBegin(x, y);
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#createMarkup(PDFDoc, Rect)}.
     */
    @Override
    protected Annot createMarkup(@NonNull PDFDoc doc, Rect bbox) throws PDFNetException {
        return null;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        onDoubleTapEvent(e);
        return true;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onDoubleTapEvent(MotionEvent)}.
     */
    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        // fast writing is sometimes treated as double tap and onMove is never triggered
        // let's handle the movement properly here
        if (e.getAction() == MotionEvent.ACTION_MOVE) {
            onMove(e, e, 0, 0);
        }

        // onDown and onUp would be called in normal way, so no worries about these events
        // It seems the order of events when a double tap happens is like this:
        //   Down
        //   Move
        //   Up
        //   Double Tap (Down)
        //   Down
        //   Double Tap (Move)
        //   Double Tap (Up)
        //   Up

        return true;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onDraw(Canvas, Matrix)}.
     */
    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        // during sliding drawing strokes cannot be reliably calculated
        if ((mFlinging && mIsScaleBegun) || mPdfViewCtrl.isSlidingWhileZoomed()) {
            return;
        }

        // Draw existing strokes
        InkState inkState = mCanvasStateManager.getCurrentState();
        inkState.drawInk(canvas, mPdfViewCtrl);

        // Draw eraser if enabled
        if (isEraserEnabled() && mEraserState != null) {
            mEraserState.drawInk(canvas, mPdfViewCtrl);
        }
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#isCreatingAnnotation()}.
     */
    @Override
    public boolean isCreatingAnnotation() {
        return true;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onClose()}.
     */
    @Override
    public void onClose() {
        super.onClose();
        unsetAnnot();
        if (mInkCommitter != null) {
            mInkCommitter.stop();
        }
    }

    @Override
    public void onRenderingFinished() {
        super.onRenderingFinished();

        if (mInkCommitter == null) {
            mCanvasStateManager.reset();
            mPdfViewCtrl.invalidate();
        }
    }

    /**
     * Commits all changes to the annotation and resets this FreehandCreate object to
     * it's initial state.
     *
     * <p>
     * <div class="warning">
     * After this is called, the tool should not continue to be used.
     * Before undo/redo, you should ensure there is no commit left to annotations.
     * </div>
     */
    public void commitAnnotation() {
        commitAnnotation(true);
    }

    /**
     * Save the current ink strokes back to the annotation. The tool can continue to be used to edit
     * the current annotation.
     */
    public void saveAnnotation() {
        if (mInkCommitter != null) {
            mInkCommitter.stop();
        }
    }

    private void commitAnnotation(boolean clearPath) {
        if (mInkCommitter != null) {
            mInkCommitter.finish();
            mInkCommitter = null;
            if (clearPath) {
                mCanvasStateManager.reset();
            }
        }
    }

    // Must check mEraserState != null before calling
    private void processEraser(@NonNull InkItem currentInkItem) {

        // for each ink on the same page as the eraser stroke,
        // erase the points touched by the eraser stroke
        boolean eraserTouchedAnyStroke = false;
        InkState currentState = mCanvasStateManager.getCurrentState();
        InkState newState = new InkState();

        // Update the current ink item
        InkItem erasedCurrentInk = processEraserOnInkItem(currentInkItem);
        InkItem newCurrentInk;
        if (erasedCurrentInk != null) {
            eraserTouchedAnyStroke = true;
            newCurrentInk = erasedCurrentInk;
            newCurrentInk.shouldAnimateUndoRedo = false;
        } else {
            newCurrentInk = currentState.currentInk.copy();
        }

        // Update the previous ink items
        List<InkItem> newPreviousInks = new ArrayList<>();
        for (int index = 0, count = currentState.previousInks.size(); index < count; ++index) {
            InkItem previousInk = currentState.previousInks.get(index);
            InkItem erasedPreviousInk = processEraserOnInkItem(previousInk);
            if (erasedPreviousInk != null) {
                eraserTouchedAnyStroke = true;
                erasedPreviousInk.shouldAnimateUndoRedo = false;
                newPreviousInks.add(erasedPreviousInk);
            } else {
                InkItem inkItem = previousInk.copy();
                newPreviousInks.add(inkItem);
            }
        }

        // If any strokes were erased, push a new state
        if (eraserTouchedAnyStroke) {
            newState.currentInk = newCurrentInk;
            newState.previousInks.addAll(newPreviousInks);
            if (mStateToPush != null) {
                mCanvasStateManager.saveState(mStateToPush);
                mStateToPush = null;
            }
            mCanvasStateManager.mCurrentState = newState;
        }

        if (sDebug) {
            if (eraserTouchedAnyStroke) {
                Log.d(TAG, "Eraser has erased");
            } else {
                Log.d(TAG, "Eraser did nothing");
            }
        }

        // invalidate area touched by eraser
        mPdfViewCtrl.invalidate();
    }

    /**
     * Computes erased ink point and returns a brand new {@link InkItem}.
     *
     * @param inkItem the ink item to check against our eraser points
     * @return the modified {@link InkItem} otherwise returns null if not modified
     */
    @Nullable
    private InkItem processEraserOnInkItem(@NonNull InkItem inkItem) {

        // check if ink is on the same page as the eraser stroke,
        // if not, continue
        if (mEraserState.getPageNumber() != inkItem.pageNumber) {
            return null;
        }

        // if there are no points in the current ink, then there is nothing
        // to erase
        if (inkItem.finishedStrokes.isEmpty()) {
            return null;
        }

        try {
            // calculate the ink rect
            Rect annotRect = getInkItemBBox(inkItem, mPdfViewCtrl);

            List<List<Float>> thicknessList = inkItem instanceof PressureInkItem ? ((PressureInkItem) inkItem).finishedPressures : null;
            PressureInkUtils.EraserData eraserData;
            switch (mInkEraserMode) {
                case PIXEL:
                    eraserData = PressureInkUtils.erasePointsAndThickness(
                            inkItem.finishedStrokes,
                            thicknessList,
                            mPdfViewCtrl,
                            mEraserState.getEraserStrokes(),
                            mEraserState.getEraserWidth() / 2.0f,
                            annotRect
                    );
                    break;
                case STROKE:
                default:
                    eraserData = PressureInkUtils.erasePressureStrokesAndThickness(
                            inkItem.finishedStrokes,
                            thicknessList,
                            mPdfViewCtrl,
                            mEraserState.getEraserStrokes(),
                            mEraserState.getEraserWidth() / 2.0f,
                            annotRect
                    );
                    break;
            }

            if (eraserData.hasErased) {
                if (inkItem instanceof PressureInkItem) {
                    return new PressureInkItem(
                            inkItem.id,
                            null,
                            null,
                            eraserData.newStrokeList,
                            eraserData.newThicknessList,
                            inkItem.pageNumber,
                            inkItem.color,
                            inkItem.opacity,
                            inkItem.baseThickness,
                            inkItem.paintThickness,
                            inkItem.isStylus
                    );
                } else {
                    return new InkItem(
                            inkItem.id,
                            null,
                            eraserData.newStrokeList,
                            inkItem.pageNumber,
                            inkItem.color,
                            inkItem.opacity,
                            inkItem.baseThickness,
                            inkItem.paintThickness,
                            inkItem.isStylus
                    );
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            return null;
        }
    }

    @NonNull
    public static ArrayList<ArrayList<PointF>> createPageStrokesFromArrayObj(Obj strokesArray) throws PDFNetException {
        ArrayList<ArrayList<PointF>> pageStrokes = new ArrayList<>();
        if (!strokesArray.isArray()) {
            return pageStrokes;
        }

        for (long i = 0, cnt = strokesArray.size(); i < cnt; i++) {
            Obj strokeArray = strokesArray.getAt((int) i);
            if (strokeArray.isArray()) {
                ArrayList<PointF> pageStroke = new ArrayList<>();
                for (long j = 0, count = strokeArray.size(); j < count; j += 2) {
                    float x = (float) strokeArray.getAt((int) j).getNumber();
                    float y = (float) strokeArray.getAt((int) j + 1).getNumber();
                    PointF p = new PointF(x, y);
                    pageStroke.add(p);
                }
                pageStrokes.add(pageStroke);
            }
        }
        return pageStrokes;
    }

    @NonNull
    public static List<List<PointF>> createStrokeListFromArrayObj(Obj strokesArray) throws PDFNetException {
        ArrayList<ArrayList<PointF>> pageStrokes = createPageStrokesFromArrayObj(strokesArray);
        List<List<PointF>> output = new ArrayList<>();
        for (ArrayList<PointF> pageStroke : pageStrokes) {
            //noinspection UseBulkOperation
            output.add(pageStroke);
        }
        return output;
    }

    private void raiseStylusUsedFirstTimeEvent() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mPdfViewCtrl.getContext());
        boolean setStylusHasBeenAsked = pref.getBoolean("pref_set_stylus_as_default_has_been_asked", false);
        if (!setStylusHasBeenAsked) {
            ((ToolManager) (mPdfViewCtrl.getToolManager())).onFreehandStylusUsedFirstTime();
            final SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("pref_set_stylus_as_default_has_been_asked", true);
            editor.apply();
        }
    }

    void setStyle(Markup annot, InkItem inkItem) {
        try {
            ColorPt color = Utils.color2ColorPt(inkItem.color);
            annot.setColor(color, 3);

            annot.setOpacity(inkItem.opacity);

            Annot.BorderStyle bs = annot.getBorderStyle();
            bs.setWidth(inkItem.baseThickness);
            annot.setBorderStyle(bs);

            setAuthor(annot);
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * Clears all strokes.
     */
    public void clearStrokes() {
        mCanvasStateManager.clear();
        updateEditToolbar();
        mPdfViewCtrl.invalidate();
        mNeedNewAnnot = true;
    }

    /**
     * Undoes the last stroke.
     */
    public void undoStroke() {
        if (mCanvasStateManager.canUndo()) {
            InkState oldState = mCanvasStateManager.getCurrentState();
            if (oldState.currentInk == null) {
                AnalyticsHandlerAdapter.getInstance().sendException(new IllegalStateException("Current undo state should not be null"));
                return;
            }
            List<PointF> previousStroke = oldState.currentInk.previousStroke;
            int previousPage = oldState.currentInk.pageNumber;
            if (!oldState.currentInk.shouldAnimateUndoRedo) {
                previousStroke = null;
            } else if (previousStroke == null) { // If null, we might have another ink annotation from previously so we need to check
                if (oldState.previousInks.size() > 0) {
                    previousStroke = oldState.previousInks.get(oldState.previousInks.size() - 1).previousStroke;
                    previousPage = oldState.previousInks.get(oldState.previousInks.size() - 1).pageNumber;
                }
            }

            // Apply undo to UI
            mCanvasStateManager.undo();
            updateEditToolbar();

            // Jump to annot
            InkState newState = mCanvasStateManager.getCurrentState();
            InkItem currentInk = newState.currentInk;
            if (currentInk == null) {
                AnalyticsHandlerAdapter.getInstance().sendException(new IllegalStateException("New undo state should not be null"));
                return;
            }
            if (currentInk.shouldAnimateUndoRedo && previousStroke != null) {
                List<List<PointF>> stroke = new ArrayList<>();
                stroke.add(previousStroke);
                float thickness = currentInk.baseThickness;
                Rect bbox = PressureInkUtils.getInkItemBBox(stroke, thickness, previousPage, mPdfViewCtrl, false);
                ViewerUtils.animateUndoRedo(mPdfViewCtrl, bbox, previousPage);
            }
            mPdfViewCtrl.invalidate();
        }
    }

    /**
     * Redoes the last undo.
     */
    public void redoStroke() {
        // Apply redo to UI
        if (mCanvasStateManager.canRedo()) {
            mCanvasStateManager.redo();
            updateEditToolbar();

            InkState newState = mCanvasStateManager.getCurrentState();
            // Jump to annot
            InkItem newCurrentInk = newState.currentInk;
            if (newCurrentInk == null) {
                AnalyticsHandlerAdapter.getInstance().sendException(new IllegalStateException("New redo state should not be null"));
                return;
            }
            List<PointF> nextStroke = newCurrentInk.previousStroke;
            int nextPage = newCurrentInk.pageNumber;
            if (!newCurrentInk.shouldAnimateUndoRedo) {
                nextStroke = null;
            } else if (nextStroke == null) { // If null, we might have another ink annotation from previously so we need to check
                if (newState.previousInks.size() > 0) {
                    nextStroke = newState.previousInks.get(newState.previousInks.size() - 1).previousStroke;
                    nextPage = newState.previousInks.get(newState.previousInks.size() - 1).pageNumber;
                }
            }

            if (nextStroke != null) {
                List<List<PointF>> stroke = new ArrayList<>();
                stroke.add(nextStroke);
                float thickness = newCurrentInk.baseThickness;
                Rect bbox = PressureInkUtils.getInkItemBBox(stroke, thickness, nextPage, mPdfViewCtrl, false);
                ViewerUtils.animateUndoRedo(mPdfViewCtrl, bbox, nextPage);
            }
            mPdfViewCtrl.invalidate();
        }
    }

    /**
     * @return True if can undo the last stroke
     */
    public boolean canUndoStroke() {
        return mCanvasStateManager.canUndo();
    }

    /**
     * @return True if can redo the last undo
     */
    public boolean canRedoStroke() {
        return mCanvasStateManager.canRedo();
    }

    /**
     * @return True if can erase any strokes
     */
    public boolean canEraseStroke() {
        return mCanvasStateManager.canClear();
    }

    private void updateEditToolbar() {
        if (mOnToolbarStateUpdateListener != null) {
            mOnToolbarStateUpdateListener.onToolbarStateUpdated();
        }
    }

    public static void setDebug(boolean debug) {
        sDebug = debug;
    }

    private static Rect getInkItemBBox(InkItem inkData, PDFViewCtrl pdfViewCtrl) {
        return PressureInkUtils.getInkItemBBox(inkData.finishedStrokes, inkData.baseThickness, inkData.pageNumber, pdfViewCtrl, true);
    }

    public String getPressureSensitiveKey() {
        return ToolStyleConfig.getInstance().getPressureSensitiveKey(getCreateAnnotType(), "");
    }

    private boolean isEraserEnabled() {
        return mEraserFromSpen || mEraserFromToolbar;
    }

    private class EraserState extends InkState {

        boolean pushInksCalled = false;

        @Override
        void pushInk(PDFViewCtrl pdfViewCtrl, int pageNumber, int strokeColor, float opacity, float baseThickness, boolean isStylus) {
            if (!pushInksCalled) {
                super.pushInk(pdfViewCtrl, pageNumber, strokeColor, opacity, baseThickness, isStylus);
            } else {
                throw new RuntimeException("PushInk should not be called multiple times for EraserState");
            }
        }

        @NonNull
        private List<List<PointF>> getEraserStrokes() {
            if (currentInk == null) {
                return new ArrayList<>();
            } else {
                return currentInk.finishedStrokes;
            }
        }

        private float getEraserWidth() {
            if (currentInk != null) {
                return currentInk.baseThickness;
            } else {
                AnalyticsHandlerAdapter.getInstance().sendException(new IllegalStateException("Could not get eraser width from current ink"));
                return 2.0f;
            }
        }

        private int getPageNumber() {
            if (currentInk != null) {
                return currentInk.pageNumber;
            } else {
                AnalyticsHandlerAdapter.getInstance().sendException(new IllegalStateException("Could not get eraser page number from current ink"));
                return 0;
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static float computeThresholdValue(View pdfViewCtrl) {
        // Compute the delta that will be used to filter out points that are too
        // close together. Use the device DPI to determine pixel delta of 2 mm (2 mm is good enough
        // visually).
        //
        // Note we convert DPI (dots per inch) to dots per mm by multiplying by 1 / 25.4,
        // since there are approximately 25.4 mm in 1 inch.
        DisplayMetrics metrics = pdfViewCtrl.getResources().getDisplayMetrics();
        float xDel = 2.0f * (metrics.xdpi * 1 / 25.4f); // number of x pixels in a span of 2 mm
        float yDel = 2.0f * (metrics.ydpi * 1 / 25.4f); // number of y pixels in a span of 2 mm
        float del = Math.max(xDel, yDel);
        return Math.max(del, 10.0f);
    }

    /**
     * @return whether stylus as pen behavior should be applied on current ink stroke. Will be true if
     * stylus as pen is enable and current motion event is stylus.
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
     * Class in charge of committing annots to the PDF and keeping track of which annots
     * have been committed. Note that we do not save the current stroke i.e. we do not save the stroke
     * the use is currently drawing.
     * <p>
     * For modifying ink annotations, currently timer mode is not supported.
     */
    private class InkCommitter {

        // PublishSubject emits a boolean. If true, then commit calls should raise annotation modified or added events
        @NonNull
        private final PublishSubject<Boolean> mObjectPublishSubject = PublishSubject.create();
        @NonNull
        private HashMap<InkItem, Ink> mPreviouslyPushedAnnotations = new HashMap<>(); // keep track of which annots we pushed so next time we commit we'll update the annotation
        private Disposable mSaveDisposable;
        private final boolean mIsModifyingInk;

        /**
         * Must only be created after mCanvasStateManager.push(...) or mCanvasStateManager.initializeStateForEditing(...) has been called.
         */
        InkCommitter(boolean isModified) {
            this.mIsModifyingInk = isModified;

            if (mTimedModeEnabled && this.mIsModifyingInk) {
                throw new RuntimeException("Timer mode while modifying ink is not currently supported");
            }
            initalizeObservables();
        }

        @SuppressWarnings("RedundantThrows")
        private void initalizeObservables() {
            if (mMultiStrokeMode && mTimedModeEnabled) { // only use timer flag if mMultiStrokeMode is true
                // Observable emits a boolean. If true, then commit calls should raise annotation modified or added events
                mSaveDisposable =
                        getTimerObservable(shouldApplyStylusAsPenBehavior(), mCanvasStateManager)
                                .subscribeOn(AndroidSchedulers.mainThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .doOnDispose(new Action() {
                                    @Override
                                    public void run() throws Exception {
                                        commitAndShowAnnotation(true);
                                    }
                                })
                                .subscribe(new Consumer<Boolean>() {
                                    @Override
                                    public void accept(Boolean shouldRaiseEvent) throws Exception {
                                        InkState currentState = mCanvasStateManager.getCurrentState();
                                        if (sDebug) {
                                            Log.d(TAG, "There are " + currentState.previousInks.size() + "previous inks");
                                        }
                                        commitInkState(currentState, mPdfViewCtrl, FreehandCreate.this, shouldRaiseEvent);
                                    }
                                });
            } else { // no timer mode
                mSaveDisposable = mObjectPublishSubject.serialize()
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnDispose(new Action() {
                            @Override
                            public void run() throws Exception {
                                commitAndShowAnnotation(true);
                            }
                        })
                        .subscribe(new Consumer<Boolean>() {
                            @Override
                            public void accept(Boolean shouldRaiseEvent) throws Exception {
                                commitAndShowAnnotation(shouldRaiseEvent);
                            }
                        });
            }
        }

        private void commitAndShowAnnotation(boolean shouldRaiseEvent) {
            // Do one last commit before it's disposed
            commitInkState(mCanvasStateManager.getCurrentState(), mPdfViewCtrl, FreehandCreate.this, shouldRaiseEvent);
            // Then show all annotations that were hidden. At this point mPreviouslyPushedAnnotations
            // should contain all annots added to the doc
            for (Map.Entry<InkItem, Ink> inkItemEntry : mPreviouslyPushedAnnotations.entrySet()) {
                InkItem inkItem = inkItemEntry.getKey();
                Ink annot = inkItemEntry.getValue();
                boolean shouldUnlockRead = false;
                try {
                    mPdfViewCtrl.showAnnotation(annot);
                    mPdfViewCtrl.docLockRead();
                    shouldUnlockRead = true;
                    mPdfViewCtrl.update(annot, inkItem.pageNumber);
                } catch (PDFNetException e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } finally {
                    if (shouldUnlockRead) {
                        mPdfViewCtrl.docUnlockRead();
                    }
                }
            }
        }

        // By default, timer is 3 seconds if stylus as pen mode and 30 seconds if in edit toolbar.
        // When multistroke is disabled, auto commits will occur every 3 seconds and the new annot flag is set (results in next emission creating a new annot)
        // When multistroke is enabled, auto commit will occur every 30 seconds.

        @SuppressWarnings("RedundantThrows")
        private Observable<Boolean> getTimerObservable(boolean isStylusAsPen, final CanvasStateManager canvasStateManager) {
            int timePeriod = isStylusAsPen ? 3 : 30;

            Observable<Boolean> observable = Observable.interval(timePeriod, TimeUnit.SECONDS)
                    .map(new Function<Long, Boolean>() {
                        @Override
                        public Boolean apply(Long aLong) throws Exception {
                            return Boolean.FALSE;
                        }
                    })
                    .mergeWith(mObjectPublishSubject.serialize())
                    .filter(new Predicate<Boolean>() {
                        @Override
                        public boolean test(Boolean aLong) throws Exception {
                            //noinspection ConstantConditions
                            return canvasStateManager.getCurrentState() != null && canvasStateManager.getCurrentState().currentInk != null;
                        }
                    });
            return observable;
        }

        void initializeWithInkAnnot(@Nullable Ink inkAnnot, @NonNull InkState inkState) {
            if (inkAnnot != null) {
                mPreviouslyPushedAnnotations.put(inkState.currentInk, inkAnnot);
            } else {
                AnalyticsHandlerAdapter.getInstance().sendException(new IllegalStateException("Edit ink annot is null and can not be initialized."));
            }
        }

        // after this method is called, the InkCommitter should not be reused
        void finish() {
            mSaveDisposable.dispose();
            mPreviouslyPushedAnnotations.clear();
        }

        void stop() {
            mSaveDisposable.dispose();
        }

        void restartIfStopped() {
            if (mSaveDisposable.isDisposed()) {
                initalizeObservables();
            }
        }

        /**
         * Commit InkStates to the document.
         *
         * @param inkState    Ink annotation data used to update, create, or delete ink annotations.
         * @param pdfViewCtrl used to get a reference to the doc
         * @param tool        used to set style in FreehandCreate
         * @param lastCommit  if true, will raise annotation events appropriately (i.e. last commit of the session)
         */
        private void commitInkState(InkState inkState, PDFViewCtrl pdfViewCtrl, FreehandCreate tool, boolean lastCommit) {
            if (inkState.currentInk != null) {
                InkState inkStateCopy = new InkState(inkState);

                List<InkItem> inksToSave = new ArrayList<>();
                // Add current ink item then the previous ink items
                // Note: Order matters. To support correct undo redo behavior, must add previous inks first
                //noinspection CollectionAddAllCanBeReplacedWithConstructor
                inksToSave.addAll(inkState.previousInks);
                inksToSave.add(inkStateCopy.currentInk);

                // Now save the ink back to the doc
                commitInks(inksToSave, pdfViewCtrl, tool, lastCommit);
            }
        }

        /**
         * Commit InkStates to the document.
         *
         * @param allInkAnnotData Ink annotation data used to update, create, or delete ink annotations.
         * @param pdfViewCtrl     used to get a reference to the doc
         * @param tool            used to set style in FreehandCreate
         * @param lastCommit      if true, will raise annotation events appropriately (i.e. last commit of the session)
         */
        private void commitInks(List<InkItem> allInkAnnotData, PDFViewCtrl pdfViewCtrl, FreehandCreate tool, boolean lastCommit) {
            if (sDebug) {
                Log.d(TAG, "Committing annotations, is last commit = " + lastCommit);
            }
            boolean shouldUnlock = false;
            try {
                pdfViewCtrl.docLock(true);
                shouldUnlock = true;

                // Commit the ink annots to the doc
                if (mIsModifyingInk) {
                    if (!lastCommit) {
                        throw new RuntimeException("When editing annot, commit can only happen once so lastCommit must be true");
                    }
                    commitEditAnnotToDoc(allInkAnnotData, pdfViewCtrl, tool);
                } else {
                    commitToDoc(allInkAnnotData, pdfViewCtrl, tool, lastCommit);
                }
            } catch (Exception e) {
                mNextToolMode = ToolMode.PAN;
                ((ToolManager) pdfViewCtrl.getToolManager()).annotationCouldNotBeAdded(e.getMessage());
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    pdfViewCtrl.docUnlock();
                }
            }
        }

        private void updatePressureInkItem(@NonNull Ink ink, @NonNull PressureInkItem pressureInkItem) throws PDFNetException {
            List<List<Float>> thicknessList = pressureInkItem.finishedPressures;
            if (thicknessList != null && thicknessList.size() == pressureInkItem.finishedStrokes.size()) {
                PressureInkUtils.clearThicknessList(ink);
                PressureInkUtils.setThicknessList(ink, thicknessList);
                PressureInkUtils.refreshCustomAppearanceForNewAnnot(mPdfViewCtrl, ink);
            } else {// fallback to normal refresh appearance
                ink.refreshAppearance();
            }
        }

        /**
         * Commit InkState to the document for editing a single annot. Must only be called when modifying inks.
         *
         * @param allInkAnnotData Ink annotation data used to update, crteate, or delete ink annotations.
         * @param pdfViewCtrl     used to get a reference to the doc
         * @param tool            used to set style in FreehandCreate
         */
        private void commitEditAnnotToDoc(List<InkItem> allInkAnnotData,
                PDFViewCtrl pdfViewCtrl,
                FreehandCreate tool
        ) throws PDFNetException {

            if (!mIsModifyingInk) {
                throw new RuntimeException("This should not be called for modifying inks");
            }

            PDFDoc pdfDoc = mPdfViewCtrl.getDoc();

            for (InkItem inkItem : allInkAnnotData) {
                Rect annotRect = getInkItemBBox(inkItem, pdfViewCtrl);
                if (annotRect == null) {
                    continue;
                }

                // If old ink exists, then we grab it. Otherwise create a new Ink annot.
                Ink ink;
                if (mPreviouslyPushedAnnotations.containsKey(inkItem)) {
                    ink = mPreviouslyPushedAnnotations.get(inkItem);
                } else {
                    AnalyticsHandlerAdapter.getInstance().sendException(new RuntimeException("The edit annot must exist"));
                    return;
                }

                // First determine whether we should update the ink list. If so, we replace the old ink list
                @SuppressWarnings("ConstantConditions")
                boolean shouldUpdateInkList = shouldUpdateInkList(ink, inkItem);
                boolean isErased = inkItem.finishedStrokes.isEmpty();

                if (isErased) {
                    if (sDebug) {
                        Log.d(TAG, "Edit Annotation deleted");
                    }
                    raiseAnnotationPreRemoveEvent(ink, inkItem.pageNumber);
                    mPdfViewCtrl.showAnnotation(ink); // need to show in case the user does undo
                    Page page = pdfDoc.getPage(inkItem.pageNumber);
                    Annot inkAnnot = AnnotUtils.safeDeleteAnnotAndUpdate(mPdfViewCtrl, page, ink, inkItem.pageNumber);
                    raiseAnnotationRemovedEvent(inkAnnot, inkItem.pageNumber);
                } else if (shouldUpdateInkList) {
                    if (sDebug) {
                        Log.d(TAG, "Edit Annotation updated");
                    }
                    raiseAnnotationPreModifyEvent(ink, inkItem.pageNumber);

                    // Set smoothing flag if enabled
                    boolean inkSmoothing = ((ToolManager) pdfViewCtrl.getToolManager()).isInkSmoothingEnabled();
                    ink.setSmoothing(inkSmoothing);

                    // Insert the ink list into the ink annot, rebuild bbox, and update style
                    PressureInkUtils.setInkList(ink, inkItem.finishedStrokes);

                    buildAnnotBBox(ink, annotRect);
                    ink.setRect(annotRect);
                    tool.setStyle(ink, inkItem);
                    if (inkItem instanceof PressureInkItem) {
                        updatePressureInkItem(ink, (PressureInkItem) inkItem);
                    } else {
                        ink.refreshAppearance();
                    }

                    setAnnot(ink, inkItem.pageNumber);

                    // Now update viewer and raise event
                    pdfViewCtrl.update(ink, inkItem.pageNumber);
                    raiseAnnotationModifiedEvent(ink, inkItem.pageNumber);
                }
            }
        }

        /**
         * Commit InkStates to the document.
         *
         * @param allInkAnnotData Ink annotation data used to update, crteate, or delete ink annotations.
         * @param pdfViewCtrl     used to get a reference to the doc
         * @param tool            used to set style in FreehandCreate
         * @param lastCommit      if true, will raise annotation events appropriately (i.e. last commit of the session)
         */
        private void commitToDoc(List<InkItem> allInkAnnotData,
                PDFViewCtrl pdfViewCtrl,
                FreehandCreate tool,
                boolean lastCommit
        ) throws PDFNetException {
            PDFDoc pdfDoc = pdfViewCtrl.getDoc();
            HashMap<InkItem, Ink> annotsToDelete = new HashMap<>(mPreviouslyPushedAnnotations); // set of annotations that we need to delete later
            for (InkItem inkItem : allInkAnnotData) {
                Rect annotRect = getInkItemBBox(inkItem, pdfViewCtrl);
                if (annotRect == null) {
                    continue;
                }

                // If old ink exists, then we grab it. Otherwise create a new Ink annot.
                Ink ink;
                if (mPreviouslyPushedAnnotations.containsKey(inkItem)) {
                    if (lastCommit) {
                        Page page = pdfDoc.getPage(inkItem.pageNumber);
                        //noinspection ConstantConditions
                        page.annotRemove(mPreviouslyPushedAnnotations.get(inkItem));
                        mPreviouslyPushedAnnotations.remove(inkItem);
                        annotsToDelete.remove(inkItem);
                        ink = Ink.create(pdfDoc, annotRect);
                    } else {
                        ink = mPreviouslyPushedAnnotations.get(inkItem);
                    }

                    // In the case where a previously committed annotation has no ink strokes left, we'll simply delete the annot
                    if (inkItem.finishedStrokes.isEmpty()) {
                        continue;
                    }
                } else {
                    if (inkItem.finishedStrokes.isEmpty()) {
                        continue;
                    }
                    ink = Ink.create(pdfDoc, annotRect);
                    pdfViewCtrl.hideAnnotation(ink);
                }

                // First determine whether we should update the ink list. If so, we replace the old ink list
                //noinspection ConstantConditions
                boolean shouldUpdateInkList = shouldUpdateInkList(ink, inkItem);

                if (shouldUpdateInkList) {
                    if (sDebug) {
                        Obj annotObj = ink.getSDFObj();
                        if (annotObj.findObj(AnnotUtils.KEY_INK_LIST) == null) {
                            Log.d(TAG, "Annotation pushed");
                        } else {
                            Log.d(TAG, "Annotation updated");
                        }
                    }

                    // Set smoothing flag if enabled
                    boolean inkSmoothing = ((ToolManager) pdfViewCtrl.getToolManager()).isInkSmoothingEnabled();
                    ink.setSmoothing(inkSmoothing);

                    // Insert the ink list into the ink annot, rebuild bbox, and update style
                    PressureInkUtils.setInkList(ink, inkItem.finishedStrokes);

                    buildAnnotBBox(ink, annotRect);
                    ink.setRect(annotRect);
                    tool.setStyle(ink, inkItem);
                    if (inkItem instanceof PressureInkItem) {
                        updatePressureInkItem(ink, (PressureInkItem) inkItem);
                    } else {
                        ink.refreshAppearance();
                    }
                }

                // Push annot back to the document
                if (!mPreviouslyPushedAnnotations.keySet().contains(inkItem)) { // ink annot, so just push
                    Page page = pdfDoc.getPage(inkItem.pageNumber);
                    page.annotPushBack(ink);
                    mPreviouslyPushedAnnotations.put(inkItem, ink);
                } else {
                    annotsToDelete.remove(inkItem); // this is required otherwise our annot will be deleted
                }

                if (lastCommit) {
                    // Now update viewer and raise event
                    pdfViewCtrl.update(ink, inkItem.pageNumber);
                    raiseAnnotationAddedEvent(ink, inkItem.pageNumber);
                    setAnnot(ink, inkItem.pageNumber);
                }
            }

            // Now delete the annots that were not touched from both the page and reference hash map
            for (Map.Entry<InkItem, Ink> inkItemEntry : annotsToDelete.entrySet()) {
                Ink annot = inkItemEntry.getValue();
                InkItem inkItem = inkItemEntry.getKey();
                int pageNumber = inkItem.pageNumber;

                // Remove annot and raise remove eventwe will quietly delete inks in the background
                Page page = pdfDoc.getPage(pageNumber);
                page.annotRemove(annot);
                mPreviouslyPushedAnnotations.remove(inkItem);
            }
        }

        private boolean shouldUpdateInkList(Ink inkAnnot, InkItem inkItem) throws PDFNetException {

            Obj inkList = inkAnnot.getSDFObj().findObj(AnnotUtils.KEY_INK_LIST);
            List<List<PointF>> pageStrokesFromArrayObj;
            if (inkList != null) {
                pageStrokesFromArrayObj = createStrokeListFromArrayObj(inkList);
            } else {
                pageStrokesFromArrayObj = new ArrayList<>();
            }
            boolean shouldUpdateInkList = false;
            if (pageStrokesFromArrayObj.isEmpty()) { // i.e. new annot
                shouldUpdateInkList = true;
            } else if (pageStrokesFromArrayObj.size() != inkItem.finishedStrokes.size()) { // i.e. number of stroke changed
                shouldUpdateInkList = true;
            } else {
                int size = pageStrokesFromArrayObj.size();
                for (int i = 0; i < size; i++) {
                    if (pageStrokesFromArrayObj.get(i).size() != inkItem.finishedStrokes.get(i).size()) {
                        shouldUpdateInkList = true;
                        break;
                    }
                }
            }

            return shouldUpdateInkList;
        }

        protected void buildAnnotBBox(Annot ink, Rect bbox) throws PDFNetException {
            if (ink != null && ink.isValid()) {
                bbox.set(0, 0, 0, 0);
                try {
                    com.pdftron.pdf.Rect r = ink.getVisibleContentBox();
                    bbox.set((float) r.getX1(), (float) r.getY1(), (float) r.getX2(), (float) r.getY2());
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
            }
        }
    }

    /**
     * Used to store current, previous, and next canvas ink states
     */
    private class CanvasStateManager {

        @NonNull
        private InkState mCurrentState = new InkState();
        private Stack<InkState> mUndoStateStack = new Stack<>();
        private Stack<InkState> mRedoStateStack = new Stack<>();

        /**
         * Initializes the canvas state for editing existing ink annotations, given an annotation
         * and related annotation attributes.
         * <p>
         * This will first push back an empty state to the undo stack in order to support
         * the correct undo/redo/clear states. Then a new state for the current state is updated based on
         * the given information.
         */
        // Must be called within a read lock
        void initializeStateForEditing(PDFViewCtrl pdfViewCtrl,
                int pageNum,
                int mStrokeColor,
                float mOpacity,
                float mThicknessDraw,
                boolean mIsStylus,
                Ink inkAnnot) throws PDFNetException {

            boolean isPressure = PressureInkUtils.isPressureSensitive(inkAnnot);

            // Push an empty state first, so that will enable you to undo/redo/clear at the very start
            mCurrentState.pushInk(
                    pdfViewCtrl,
                    pageNum,
                    mStrokeColor,
                    mOpacity,
                    mThicknessDraw,
                    mIsStylus,
                    isPressure
            );

            mUndoStateStack.push(new InkState(mCurrentState));
            mRedoStateStack.clear();

            // Then populate the current state using the annot's ink list
            Obj annotObj;
            try {
                annotObj = inkAnnot.getSDFObj();
                Obj inkList = annotObj.findObj(AnnotUtils.KEY_INK_LIST);
                List<List<PointF>> pageStrokesFromArrayObj = createStrokeListFromArrayObj(inkList);
                List<double[]> thickneses = PressureInkUtils.getThicknessArrays(inkAnnot);
                boolean isPressureInk = thickneses != null && pageStrokesFromArrayObj.size() == thickneses.size();

                for (int k = 0; k < pageStrokesFromArrayObj.size(); k++) {
                    List<PointF> stroke = pageStrokesFromArrayObj.get(k);
                    double[] thickness = null;
                    if (isPressureInk) {
                        thickness = thickneses.get(k);
                    }
                    for (int i = 0; i < stroke.size(); i++) {
                        PointF pt = stroke.get(i);
                        double[] pagePt = pdfViewCtrl.convPagePtToScreenPt(pt.x, pt.y, pageNum);
                        int motionEvent;
                        if (i == 0) { //first
                            motionEvent = MotionEvent.ACTION_DOWN;
                        } else {
                            motionEvent = MotionEvent.ACTION_MOVE;
                        }
                        double pressure = thickness == null ? 1.0 : thickness[i];
                        mCurrentState.addPoint((float) pagePt[0], (float) pagePt[1], (float) pressure, motionEvent);
                    }
                    mCurrentState.addPoint(-1, -1, -1, MotionEvent.ACTION_UP); // data for on up is ignored so we input anything
                }
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }

        @NonNull
        InkState getCurrentState() {
            return mCurrentState;
        }

        // Called when a change has been made which should be undoable
        // pass in the state you want to save
        void saveState(@NonNull InkState state) {
            mUndoStateStack.push(state);
            mRedoStateStack.clear();
        }

        void undo() {
            mRedoStateStack.push(new InkState(mCurrentState));
            mCurrentState = mUndoStateStack.pop();
        }

        void redo() {
            mUndoStateStack.push(new InkState(mCurrentState));
            mCurrentState = mRedoStateStack.pop();
        }

        boolean canUndo() {
            return !mUndoStateStack.isEmpty();
        }

        boolean canRedo() {
            return !mRedoStateStack.isEmpty();
        }

        boolean canClear() {
            return mCurrentState.canClear();
        }

        public void clear() {
            saveState(new InkState(mCurrentState));
            getCurrentState().clear();
        }

        public void reset() {
            mCurrentState = new InkState();
            mUndoStateStack = new Stack<>();
            mRedoStateStack = new Stack<>();
        }
    }

    /**
     * Class that represents the ink stroke state of the canvas.
     */
    private class InkState {
        @Nullable
        InkItem currentInk; // one for current annot
        final Stack<InkItem> previousInks = new Stack<>(); // one for each previous annot

        InkState() {

        }

        // in page space
        @Nullable
        Rect getBoundingBox() {
            if (currentInk != null) {
                return PressureInkUtils.getInkItemBBox(currentInk.finishedStrokes, currentInk.baseThickness, currentInk.pageNumber, mPdfViewCtrl, false);
            } else {
                return null;
            }
        }

        InkState(@NonNull InkState thatState) {
            this.currentInk = thatState.currentInk == null ? null : thatState.currentInk.copy();
            for (InkItem item : thatState.previousInks) {
                this.previousInks.push(item);
            }
        }

        // We ignore data for on up events, and do not store it otherwise it might be
        // too close to the previous on move event
        void addPoint(float x, float y, float pressure, int action) {
            if (currentInk != null) {
                double[] pt = mPdfViewCtrl.convScreenPtToPagePt(x, y, currentInk.pageNumber);
                currentInk.addPoint((float) pt[0], (float) pt[1], pressure, action);
            }
        }

        // This should be called when the next strokes should be in a separate annotation
        // By default pressure is not used
        void pushInk(PDFViewCtrl pdfViewCtrl,
                int pageNumber,
                int strokeColor,
                float opacity,
                float baseThickness,
                boolean isStylus) {
            pushInk(pdfViewCtrl, pageNumber, strokeColor, opacity, baseThickness, isStylus, false);
        }

        void pushInk(PDFViewCtrl pdfViewCtrl,
                int pageNumber,
                int strokeColor,
                float opacity,
                float baseThickness,
                boolean isStylus,
                boolean isPressure) {
            if (currentInk != null) {
                previousInks.push(currentInk);
            }
            if (isPressure) {
                currentInk = new PressureInkItem(
                        pageNumber,
                        strokeColor,
                        opacity,
                        baseThickness,
                        isStylus,
                        pdfViewCtrl);
            } else {
                currentInk = new InkItem(
                        pageNumber,
                        strokeColor,
                        opacity,
                        baseThickness,
                        isStylus,
                        pdfViewCtrl);
            }
        }

        void drawInk(Canvas canvas, PDFViewCtrl pdfViewCtrl) {
            if (currentInk != null) {
                //
                // Drawing order matters. The current ink should be on top, so we draw it afterwards
                //
                // Draw lines from other stroke items from other annots
                for (InkItem inkItem : previousInks) {
                    inkItem.draw(canvas, pdfViewCtrl, null, null);
                }
                // Draw lines from current stroke item for current annot
                currentInk.draw(canvas, pdfViewCtrl, null, null);
            }
        }

        void clear() {
            if (currentInk != null) {
                currentInk.finishedStrokes.clear();
                previousInks.clear();
            }
        }

        boolean canClear() {
            return (currentInk != null && !currentInk.finishedStrokes.isEmpty()) || previousInks.size() > 0;
        }
    }
}
