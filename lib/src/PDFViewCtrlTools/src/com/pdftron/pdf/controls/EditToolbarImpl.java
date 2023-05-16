//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.model.LineEndingStyle;
import com.pdftron.pdf.model.LineStyle;
import com.pdftron.pdf.model.RulerItem;
import com.pdftron.pdf.model.ShapeBorderStyle;
import com.pdftron.pdf.tools.AdvancedShapeCreate;
import com.pdftron.pdf.tools.Eraser;
import com.pdftron.pdf.tools.FreehandCreate;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.PressureInkUtils;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.toolbar.component.view.SingleButtonToolbar;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static com.pdftron.pdf.tools.ToolManager.ToolMode.AREA_MEASURE_CREATE;
import static com.pdftron.pdf.tools.ToolManager.ToolMode.CLOUD_CREATE;
import static com.pdftron.pdf.tools.ToolManager.ToolMode.INK_CREATE;
import static com.pdftron.pdf.tools.ToolManager.ToolMode.PERIMETER_MEASURE_CREATE;
import static com.pdftron.pdf.tools.ToolManager.ToolMode.POLYGON_CREATE;
import static com.pdftron.pdf.tools.ToolManager.ToolMode.POLYLINE_CREATE;
import static com.pdftron.pdf.tools.ToolManager.ToolMode.SMART_PEN_INK;

/**
 * This class is implementing the logic for {@link EditToolbar} to create/edit annotations.
 */
public class EditToolbarImpl implements
        OnToolbarStateUpdateListener,
        OnToolSelectedListener,
        EditToolbar.OnEditToolbarChangedListener {

    private static final String INK_TAG_1 = "ink_tag_1";
    private static final String INK_TAG_2 = "ink_tag_2";
    private static final String INK_TAG_3 = "ink_tag_3";
    private static final String INK_TAG_4 = "ink_tag_4";
    private static final String INK_TAG_5 = "ink_tag_5";

    private WeakReference<FragmentActivity> mActivityRef;
    private BaseEditToolbar mEditToolbar;
    protected ToolManager mToolManager;
    private PDFViewCtrl mPdfViewCtrl;
    private ToolManager.ToolMode mStartToolMode;
    private ArrayList<AnnotStyle> mDrawStyles = new ArrayList<>();
    private AnnotStyle mEraserStyle;
    private boolean mIsStyleFixed;

    private OnEditToolbarListener mOnEditToolbarListener;

    /**
     * Callback interface invoked when the edit toolbar is dismissed.
     */
    public interface OnEditToolbarListener {

        /**
         * Called when the edit toolbar has been dismissed.
         */
        void onEditToolbarDismissed();
    }

    // Bundle that contains any meta data. Mainly used for annotation toolbar button meta data, in order
    // to keep track of which annotation tool is selected
    @NonNull
    final Bundle mBundle;

    public EditToolbarImpl(@NonNull FragmentActivity activity,
            @NonNull EditToolbar editToolbar,
            @NonNull ToolManager toolManager,
            @NonNull ToolManager.ToolMode toolMode,
            @Nullable Annot editAnnot,
            int pageNumber,
            boolean shouldExpand) {
        this(activity, editToolbar, toolManager, toolMode, editAnnot, pageNumber, shouldExpand, new Bundle());
    }

    /**
     * Class constructor
     *
     * @param activity     The activity which is used for showing a popup window dialog
     * @param editToolbar  The edit toolbar view
     * @param toolManager  The tool manager
     * @param toolMode     The tool mode which the toolbar should start with
     * @param editAnnot    The annotation to be edited
     * @param shouldExpand Specify whether the toolbar should be expanded
     *                     when phone is in portrait mode
     */
    @SuppressWarnings("WeakerAccess")
    public EditToolbarImpl(@NonNull FragmentActivity activity,
            @NonNull BaseEditToolbar editToolbar,
            @NonNull ToolManager toolManager,
            @NonNull ToolManager.ToolMode toolMode,
            @Nullable Annot editAnnot,
            int pageNumber,
            boolean shouldExpand,
            @NonNull Bundle bundle) {
        mBundle = bundle;
        mActivityRef = new WeakReference<>(activity);
        mEditToolbar = editToolbar;
        mToolManager = toolManager;
        mPdfViewCtrl = mToolManager.getPDFViewCtrl();
        mStartToolMode = toolMode;

        mEditToolbar.setVisibility(View.GONE);

        boolean hasEraseBtn = false;
        boolean isInkEditing = false;
        initTool(toolMode);
        ToolManager.Tool currentTool = mToolManager.getTool();
        if (currentTool instanceof FreehandCreate) {
            FreehandCreate freehandTool = (FreehandCreate) currentTool;
            freehandTool.commitAnnotation(); // in the case where we switch to ink tool from stylus ink tool
            if (editAnnot != null) {
                mIsStyleFixed = true;
                mDrawStyles.add(getAnnotStyleFromAnnot(editAnnot));
                freehandTool.setTimedModeEnabled(false); // order matters, must set time mode false before initializing with an ink annot
                isInkEditing = true;
            } else {
                if (mToolManager.isInkMultiStrokeEnabled()) {
                    freehandTool.setTimedModeEnabled(mToolManager.isFreehandTimerEnabled());
                } else {
                    freehandTool.setTimedModeEnabled(false);
                }
                for (int i = 0; i < 5; ++i) {
                    AnnotStyle annotStyle = ToolStyleConfig.getInstance().getCustomAnnotStyle(activity, Annot.e_Ink, getInkTag(i));
                    mDrawStyles.add(annotStyle);
                }
            }
            mEraserStyle = ToolStyleConfig.getInstance().getCustomAnnotStyle(activity, AnnotStyle.CUSTOM_ANNOT_TYPE_ERASER, "");
            hasEraseBtn = true;

            freehandTool.setOnToolbarStateUpdateListener(this);
        } else if ((toolMode == POLYLINE_CREATE || toolMode == POLYGON_CREATE || toolMode == CLOUD_CREATE ||
                toolMode == PERIMETER_MEASURE_CREATE || toolMode == AREA_MEASURE_CREATE) &&
                currentTool instanceof AdvancedShapeCreate) {
            AnnotStyle annotStyle;
            switch (toolMode) {
                case PERIMETER_MEASURE_CREATE:
                    annotStyle = ToolStyleConfig.getInstance().getCustomAnnotStyle(activity, AnnotStyle.CUSTOM_ANNOT_TYPE_PERIMETER_MEASURE, "");
                    break;
                case AREA_MEASURE_CREATE:
                    annotStyle = ToolStyleConfig.getInstance().getCustomAnnotStyle(activity, AnnotStyle.CUSTOM_ANNOT_TYPE_AREA_MEASURE, "");
                    break;
                case POLYLINE_CREATE:
                    annotStyle = ToolStyleConfig.getInstance().getCustomAnnotStyle(activity, Annot.e_Polyline, "");
                    break;
                case POLYGON_CREATE:
                    annotStyle = ToolStyleConfig.getInstance().getCustomAnnotStyle(activity, Annot.e_Polygon, "");
                    break;
                case CLOUD_CREATE:
                default:
                    annotStyle = ToolStyleConfig.getInstance().getCustomAnnotStyle(activity, AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD, "");
                    break;
            }
            annotStyle.setSnap(mToolManager.isSnappingEnabledForMeasurementTools());
            mDrawStyles.add(annotStyle);
            ((AdvancedShapeCreate) currentTool).setOnToolbarStateUpdateListener(this);
        }

        mEditToolbar.setup(mPdfViewCtrl, this, mDrawStyles,
                true, hasEraseBtn, true, shouldExpand, mIsStyleFixed);
        mEditToolbar.setOnEditToolbarChangeListener(this);
        updateToolbarControlButtons();
        if (!mDrawStyles.isEmpty()) {
            initAnnotProperties(mDrawStyles.get(0));
        }
        if (isInkEditing) { // we need this at the end because we need pressure info first
            FreehandCreate tool = (FreehandCreate) currentTool;
            tool.setInitInkItem(editAnnot, pageNumber);
        }
    }

    /**
     * make the edit toolbar visible
     */
    @SuppressWarnings("WeakerAccess")
    public void showToolbar() {
        mEditToolbar.show();
    }

    /**
     * @return True if the edit toolbar is shown
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isToolbarShown() {
        return mEditToolbar.isShown();
    }

    private boolean startWith(ToolManager.ToolMode toolMode) {
        if (mToolManager == null) {
            return false;
        }
        if (mStartToolMode == toolMode) {
            if (mStartToolMode != mToolManager.getTool().getToolMode()) {
                initTool(mStartToolMode);
            }
            return true;
        }

        return false;
    }

    private boolean startWithClickBasedAnnot() {
        if (mToolManager == null) {
            return false;
        }
        if (mStartToolMode == POLYLINE_CREATE || mStartToolMode == POLYGON_CREATE || mStartToolMode == CLOUD_CREATE ||
                mStartToolMode == PERIMETER_MEASURE_CREATE || mStartToolMode == AREA_MEASURE_CREATE) {
            if (mStartToolMode != mToolManager.getTool().getToolMode()) {
                initTool(mStartToolMode);
            }
            return true;
        }

        return false;
    }

    private AnnotStyle getAnnotStyleFromAnnot(Annot annot) {
        if (mToolManager == null || mPdfViewCtrl == null) {
            return null;
        }

        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            // color
            ColorPt colorPt = annot.getColorAsRGB();
            int color = Utils.colorPt2color(colorPt);

            boolean isPressure = PressureInkUtils.isPressureSensitive(annot);

            // opacity
            Markup m = new Markup(annot);
            float opacity = (float) m.getOpacity();

            // thickness
            float thickness = (float) annot.getBorderStyle().getWidth();

            final AnnotStyle annotStyle = new AnnotStyle();
            annotStyle.setAnnotType(annot.getType());
            // AnnotStyle should store the real color of the annotation and not the post processed color
            annotStyle.setStyle(color, Color.TRANSPARENT, thickness, opacity);
            annotStyle.setPressureSensitivity(isPressure);
            return annotStyle;
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
        return null;
    }

    /**
     * The overloaded implementation of {@link OnToolSelectedListener#onDrawSelected(int, boolean, View)} }
     */
    @Override
    public void onDrawSelected(int drawIndex,
            boolean wasSelectedBefore,
            View anchor) {
        if (mToolManager == null) {
            return;
        }

        AnnotStyle annotStyle = mDrawStyles.get(drawIndex);
        if (annotStyle != null) {
            if (!mIsStyleFixed && wasSelectedBefore) {
                annotStyle.setSnap(mToolManager.isSnappingEnabledForMeasurementTools());
                AnnotStyleDialogFragment popupWindow = new AnnotStyleDialogFragment.Builder(annotStyle).setAnchorView(anchor).build();
                if (startWith(INK_CREATE) || startWith(SMART_PEN_INK)) {
                    showAnnotPropertyPopup(popupWindow, drawIndex, getInkTag(drawIndex), AnalyticsHandlerAdapter.ANNOTATION_TOOL_FREEHAND);
                } else if (startWith(POLYLINE_CREATE)) {
                    showAnnotPropertyPopup(popupWindow, drawIndex, "", AnalyticsHandlerAdapter.ANNOTATION_TOOL_POLYLINE);
                } else if (startWith(POLYGON_CREATE)) {
                    showAnnotPropertyPopup(popupWindow, drawIndex, "", AnalyticsHandlerAdapter.ANNOTATION_TOOL_POLYGON);
                } else if (startWith(CLOUD_CREATE)) {
                    showAnnotPropertyPopup(popupWindow, drawIndex, "", AnalyticsHandlerAdapter.ANNOTATION_TOOL_CLOUD);
                } else if (startWith(PERIMETER_MEASURE_CREATE)) {
                    showAnnotPropertyPopup(popupWindow, drawIndex, "", AnalyticsHandlerAdapter.ANNOTATION_TOOL_PERIMETER_MEASURE);
                } else if (startWith(AREA_MEASURE_CREATE)) {
                    showAnnotPropertyPopup(popupWindow, drawIndex, "", AnalyticsHandlerAdapter.ANNOTATION_TOOL_AREA_MEASURE);
                }
            }
            updateAnnotProperties(annotStyle);
        }

        if (mToolManager.isSkipNextTapEvent()) {
            // if we previously closed a popup without clicking viewer
            // let's clear the flag
            mToolManager.resetSkipNextTapEvent();
        }
    }

    /**
     * The overloaded implementation of {@link OnToolSelectedListener#onClearSelected()} }
     */
    @Override
    public void onClearSelected() {
        if (mToolManager == null) {
            return;
        }

        final ToolManager.Tool tool = mToolManager.getTool();
        if (tool instanceof Eraser || startWith(INK_CREATE) || startWith(SMART_PEN_INK)) {
            if (tool instanceof FreehandCreate) {
                ((FreehandCreate) tool).clearStrokes();
            }
            updateToolbarControlButtons();
        } else if (startWithClickBasedAnnot() && tool instanceof AdvancedShapeCreate) {
            ((AdvancedShapeCreate) tool).clear();
            updateToolbarControlButtons();
        }
    }

    /**
     * The overloaded implementation of {@link OnToolSelectedListener#onEraserSelected(boolean, View)} }
     */
    @Override
    public void onEraserSelected(boolean wasSelectedBefore,
            View anchor) {
        if (mToolManager == null) {
            return;
        }
        ToolManager.Tool tool = mToolManager.getTool();
        if (tool instanceof FreehandCreate || tool instanceof Eraser) {
            if (wasSelectedBefore && mEraserStyle != null) {
                AnnotStyleDialogFragment popupWindow = new AnnotStyleDialogFragment.Builder(mEraserStyle).setAnchorView(anchor).build();
                showInkEraserAnnotPropertyPopup(popupWindow);
            }
        }

        if (mToolManager.isSkipNextTapEvent()) {
            // if we previously closed a popup without clicking viewer
            // let's clear the flag
            mToolManager.resetSkipNextTapEvent();
        }

        updateInkEraserAnnotProperties();
    }

    public boolean canUndo() {
        ToolManager.Tool tool = mToolManager.getTool();
        if (tool instanceof FreehandCreate) {
            return ((FreehandCreate) tool).canUndoStroke();
        } else if (startWithClickBasedAnnot() && tool instanceof AdvancedShapeCreate) {
            ((AdvancedShapeCreate) tool).canUndo();
        }
        return false;
    }

    public boolean canRedo() {
        ToolManager.Tool tool = mToolManager.getTool();
        if (tool instanceof FreehandCreate) {
            return ((FreehandCreate) tool).canRedoStroke();
        } else if (startWithClickBasedAnnot() && tool instanceof AdvancedShapeCreate) {
            ((AdvancedShapeCreate) tool).canRedo();
        }
        return false;
    }

    /**
     * The overloaded implementation of {@link OnToolSelectedListener#onUndoSelected()} }
     */
    @Override
    public void onUndoSelected() {
        if (mToolManager == null) {
            return;
        }

        ToolManager.Tool tool = mToolManager.getTool();
        if (tool instanceof Eraser || startWith(INK_CREATE) || startWith(SMART_PEN_INK)) {
            if (tool instanceof FreehandCreate) {
                ((FreehandCreate) tool).undoStroke();
            }
        } else if (startWithClickBasedAnnot() && tool instanceof AdvancedShapeCreate) {
            ((AdvancedShapeCreate) tool).undo();
        }

        updateToolbarControlButtons();
    }

    /**
     * The overloaded implementation of {@link OnToolSelectedListener#onRedoSelected()} }
     */
    @Override
    public void onRedoSelected() {
        if (mToolManager == null) {
            return;
        }

        ToolManager.Tool tool = mToolManager.getTool();
        if (tool instanceof Eraser || startWith(INK_CREATE) || startWith(SMART_PEN_INK)) {
            if (tool instanceof FreehandCreate) {
                ((FreehandCreate) tool).redoStroke();
            }
        } else if (startWithClickBasedAnnot() && tool instanceof AdvancedShapeCreate) {
            ((AdvancedShapeCreate) tool).redo();
        }

        updateToolbarControlButtons();
    }

    /**
     * The overloaded implementation of {@link OnToolSelectedListener#onCloseSelected()} }
     */
    @Override
    public void onCloseSelected() {
        if (mToolManager == null || mEditToolbar == null) {
            return;
        }

        if (startWithClickBasedAnnot() && mToolManager.getTool() instanceof AdvancedShapeCreate) {
            ((AdvancedShapeCreate) mToolManager.getTool()).commit();
        }

        if (mToolManager.getTool() instanceof FreehandCreate) {
            ((FreehandCreate) mToolManager.getTool()).commitAnnotation();
        }

        mEditToolbar.setVisibility(View.GONE);
        if (mOnEditToolbarListener != null) {
            mOnEditToolbarListener.onEditToolbarDismissed();
        }
    }

    /**
     * Called when the state of edit toolbar should be updated.
     */
    @Override
    public void onToolbarStateUpdated() {
        updateToolbarControlButtons();
    }

    /**
     * Commits the changes and closes the Edit toolbar
     */
    public void close() {
        onCloseSelected();

        if (mToolManager != null) {
            mToolManager.getUndoRedoManger().setEditToolbarImpl(null);
        }
    }

    private void updateToolbarControlButtons() {
        if (mToolManager == null) {
            return;
        }
        boolean canClear = false, canErase = false, canUndo = false, canRedo = false;

        ToolManager.Tool tool = mToolManager.getTool();
        if (tool instanceof Eraser || startWith(INK_CREATE) || startWith(SMART_PEN_INK)) {
            if (tool instanceof FreehandCreate) {
                canClear = ((FreehandCreate) tool).canEraseStroke();
                canUndo = ((FreehandCreate) tool).canUndoStroke();
                canRedo = ((FreehandCreate) tool).canRedoStroke();
                canErase = ((FreehandCreate) tool).canEraseStroke();
            }
        } else if (startWithClickBasedAnnot() && tool instanceof AdvancedShapeCreate) {
            canClear = ((AdvancedShapeCreate) tool).canClear();
            canUndo = ((AdvancedShapeCreate) tool).canUndo();
            canRedo = ((AdvancedShapeCreate) tool).canRedo();
        }

        mEditToolbar.updateControlButtons(canClear, canErase, canUndo, canRedo);
    }

    protected void initTool(ToolManager.ToolMode toolMode) {
        // Called when changing to ink tool from quick menu
        if (mToolManager.getTool().getToolMode() != toolMode) {
            Tool tool = (Tool) mToolManager.createTool(toolMode, mToolManager.getTool(), mBundle);
            mToolManager.setTool(tool);
        }
        if ((mToolManager.getTool() instanceof FreehandCreate)) {
            ((FreehandCreate) mToolManager.getTool()).setForceSameNextToolMode(true);
            ((FreehandCreate) mToolManager.getTool()).setMultiStrokeMode(mToolManager.isInkMultiStrokeEnabled());
            ((FreehandCreate) mToolManager.getTool()).setFromEditToolbar(true);
            ((FreehandCreate) mToolManager.getTool()).setOnToolbarStateUpdateListener(this);
        }
    }

    private void showAnnotPropertyPopup(@NonNull final AnnotStyleDialogFragment popupWindow,
            final int drawIndex,
            final String extraTag,
            int analyticsScreenId) {
        FragmentActivity activity = mActivityRef.get();
        if (activity == null || mToolManager == null) {
            return;
        }

        if (mToolManager.isSkipNextTapEvent()) {
            mToolManager.resetSkipNextTapEvent();
            return;
        }
        popupWindow.setCanShowPressureSwitch(true);
        popupWindow.setAnnotStyleProperties(mToolManager.getAnnotStyleProperties());

        popupWindow.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (mToolManager == null || mPdfViewCtrl == null) {
                    return;
                }
                Context context = mPdfViewCtrl.getContext();
                if (context == null) {
                    return;
                }

                AnnotStyle annotStyle = popupWindow.getAnnotStyle();
                updateAnnotProperties(annotStyle);
                ToolStyleConfig.getInstance().saveAnnotStyle(context, annotStyle, extraTag);

                mDrawStyles.set(drawIndex, annotStyle);
                mEditToolbar.updateDrawStyles(mDrawStyles);
            }
        });
        popupWindow.setOnAnnotStyleChangeListener(new AnnotStyle.OnAnnotStyleChangeListener() {
            @Override
            public void onChangeAnnotThickness(float thickness, boolean done) {

            }

            @Override
            public void onChangeAnnotTextSize(float textSize, boolean done) {

            }

            @Override
            public void onChangeAnnotTextColor(int textColor) {

            }

            @Override
            public void onChangeAnnotOpacity(float opacity, boolean done) {

            }

            @Override
            public void onChangeAnnotStrokeColor(int color) {
                mEditToolbar.updateDrawColor(drawIndex, color);
            }

            @Override
            public void onChangeAnnotFillColor(int color) {

            }

            @Override
            public void onChangeAnnotIcon(String icon) {

            }

            @Override
            public void onChangeAnnotFont(FontResource font) {

            }

            @Override
            public void onChangeRulerProperty(RulerItem rulerItem) {

            }

            @Override
            public void onChangeOverlayText(String overlayText) {

            }

            @Override
            public void onChangeSnapping(boolean snap) {
                mToolManager.setSnappingEnabledForMeasurementTools(snap);
            }

            @Override
            public void onChangeRichContentEnabled(boolean enabled) {

            }

            @Override
            public void onChangeDateFormat(String dateFormat) {

            }

            @Override
            public void onChangeAnnotBorderStyle(ShapeBorderStyle borderStyle) {

            }

            @Override
            public void onChangeAnnotLineStyle(LineStyle lineStyle) {

            }

            @Override
            public void onChangeAnnotLineStartStyle(LineEndingStyle lineStartStyle) {

            }

            @Override
            public void onChangeAnnotLineEndStyle(LineEndingStyle lineEndStyle) {

            }

            @Override
            public void onChangeTextAlignment(int horizontalAlignment, int verticalAlignment) {

            }
        });
        popupWindow.show(activity.getSupportFragmentManager(),
                AnalyticsHandlerAdapter.STYLE_PICKER_LOC_ANNOT_TOOLBAR,
                AnalyticsHandlerAdapter.getInstance().getAnnotationTool(analyticsScreenId));
    }

    private void showInkEraserAnnotPropertyPopup(@NonNull final AnnotStyleDialogFragment popupWindow) {
        FragmentActivity activity = mActivityRef.get();
        if (activity == null || mToolManager == null) {
            return;
        }

        if (mToolManager.isSkipNextTapEvent()) {
            mToolManager.resetSkipNextTapEvent();
            return;
        }

        popupWindow.setAnnotStyleProperties(mToolManager.getAnnotStyleProperties());
        popupWindow.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (mToolManager == null || mPdfViewCtrl == null) {
                    return;
                }
                Context context = mPdfViewCtrl.getContext();
                if (context == null) {
                    return;
                }

                AnnotStyle annotStyle = popupWindow.getAnnotStyle();
                ToolStyleConfig.getInstance().saveAnnotStyle(context, annotStyle, "");
                Tool tool = (Tool) mToolManager.getTool();
                if (tool instanceof Eraser) {
                    ((Eraser) tool).setupAnnotProperty(annotStyle);
                } else if (tool instanceof FreehandCreate) {
                    ((FreehandCreate) tool).setupEraserProperty(annotStyle);
                }

                mEraserStyle = annotStyle;
            }
        });
        popupWindow.show(activity.getSupportFragmentManager(),
                AnalyticsHandlerAdapter.STYLE_PICKER_LOC_ANNOT_TOOLBAR,
                AnalyticsHandlerAdapter.getInstance().getAnnotationTool(AnalyticsHandlerAdapter.ANNOTATION_TOOL_ERASER));
    }

    private void updateAnnotProperties(AnnotStyle annotStyle) {
        if (mToolManager == null || annotStyle == null) {
            return;
        }

        ToolManager.Tool tool = mToolManager.getTool();
        ((Tool) tool).setupAnnotProperty(annotStyle);
    }

    private void initAnnotProperties(AnnotStyle annotStyle) {
        if (mToolManager == null || annotStyle == null) {
            return;
        }

        ToolManager.Tool tool = mToolManager.getTool();
        // If using SingleButtonToolbar and creating a new annotation, the initial annot style
        // is set via the preset component (i.e. last used preset) so no need to initialize in this case.
        // However if we are editing an annotation with this toolbar (i.e. edit ink), then we
        // will need to initialize annot style properties here.
        boolean isSingleButtonToolbarAndEditing = mEditToolbar instanceof SingleButtonToolbar
                && ((SingleButtonToolbar) mEditToolbar).isEditingAnnotation();
        if (mEditToolbar instanceof EditToolbar || isSingleButtonToolbarAndEditing) {
            ((Tool) tool).setupAnnotProperty(annotStyle);
        }
    }

    private void updateInkEraserAnnotProperties() {
        if (mToolManager == null || mEraserStyle == null) {
            return;
        }

        if (mToolManager.getTool() instanceof FreehandCreate) {
            ((FreehandCreate) mToolManager.getTool()).setupEraserProperty(mEraserStyle);
        }
    }

    private String getInkTag(int inkIndex) {
        switch (inkIndex) {
            case 0:
                return INK_TAG_1;
            case 1:
                return INK_TAG_2;
            case 2:
                return INK_TAG_3;
            case 3:
                return INK_TAG_4;
            case 4:
                return INK_TAG_5;
        }
        return "";
    }

    /**
     * Handles the shortcuts key in the edit toolbar.
     *
     * @param keyCode the key code
     * @param event   the key event
     * @return true if it is handled; false otherwise
     */
    @SuppressWarnings("WeakerAccess")
    public boolean handleKeyUp(int keyCode,
            KeyEvent event) {
        return mEditToolbar.handleKeyUp(keyCode, event);
    }

    /**
     * Sets listener to {@link OnEditToolbarListener}.
     *
     * @param listener The {@link OnEditToolbarListener} listener
     */
    @SuppressWarnings("WeakerAccess")
    public void setOnEditToolbarListener(OnEditToolbarListener listener) {
        mOnEditToolbarListener = listener;
    }

    @Override
    public void onOrientationChanged() {
        if (mEditToolbar.isShown()) {
            updateToolbarControlButtons();
        }
    }

    public ToolManager.ToolMode getToolMode() {
        return mStartToolMode;
    }
}
