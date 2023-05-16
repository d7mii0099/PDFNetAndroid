package com.pdftron.pdf.widget.preset.component;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Pair;
import android.view.ViewGroup;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.controls.AnnotStyleDialogFragment;
import com.pdftron.pdf.controls.AnnotStyleView;
import com.pdftron.pdf.dialog.measurecount.CountToolCreatePresetDialog;
import com.pdftron.pdf.dialog.measurecount.CountToolDialogFragment;
import com.pdftron.pdf.dialog.signature.SignatureDialogFragment;
import com.pdftron.pdf.interfaces.OnCreateSignatureListener;
import com.pdftron.pdf.interfaces.OnDialogDismissListener;
import com.pdftron.pdf.interfaces.OnRubberStampSelectedListener;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.model.LineEndingStyle;
import com.pdftron.pdf.model.LineStyle;
import com.pdftron.pdf.model.RulerItem;
import com.pdftron.pdf.model.ShapeBorderStyle;
import com.pdftron.pdf.tools.AnnotEditRectGroup;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.RubberStampCreate;
import com.pdftron.pdf.tools.Signature;
import com.pdftron.pdf.tools.SmartPenInk;
import com.pdftron.pdf.tools.SmartPenMarkup;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.StampManager;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.preset.component.model.PresetBarState;
import com.pdftron.pdf.widget.preset.component.model.PresetButtonState;
import com.pdftron.pdf.widget.preset.component.model.SinglePresetState;
import com.pdftron.pdf.widget.preset.component.view.PresetBarView;
import com.pdftron.pdf.widget.preset.component.view.TabletPresetBarView;
import com.pdftron.pdf.widget.preset.signature.SignaturePresetComponent;
import com.pdftron.pdf.widget.preset.signature.SignatureSelectionDialog;
import com.pdftron.pdf.widget.preset.signature.SignatureViewModel;
import com.pdftron.pdf.widget.preset.signature.model.SignatureData;
import com.pdftron.pdf.widget.toolbar.ToolManagerViewModel;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarButtonType;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarItem;
import com.pdftron.pdf.widget.toolbar.component.AnnotationToolbarComponent;
import com.pdftron.sdf.Obj;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

import io.reactivex.functions.Consumer;

/**
 * Preset bar UI Component that is in charge of handling the preset bar bar logic and UI events. Used
 * to show the current tool's available style presets.
 */
public class PresetBarComponent {

    private final PresetBarView mPresetBarView;
    private final PresetBarViewModel mPresetBarViewModel;
    private final ToolManagerViewModel mToolManagerViewModel;
    private final SignatureViewModel mSignatureViewModel;
    private final boolean mButtonStayDown;
    @Nullable
    private final HashSet<ToolbarButtonType> mToolsToHidePresetBar;

    private final SignaturePresetComponent mSignaturePresetComponent;
    private SignatureSelectionDialog mSignatureSelectionDialog;

    /**
     * Creates a the default {@link PresetBarComponent}
     *
     * @param lifecycleOwner       the lifecycle owner that will handle this UI component's state
     * @param fragmentManager      the fragment manager, which will be used to show the annot style dialog
     * @param presetBarViewModel   the {@link PresetBarViewModel} that manages the preset bar state
     * @param toolManagerViewModel the {@link ToolManagerViewModel} that manages the current ToolManager
     * @param signatureViewModel   the {@link SignatureViewModel} that manages the list of available signatures
     * @param container            the container to add this {@link PresetBarComponent}
     */
    public PresetBarComponent(@NonNull final LifecycleOwner lifecycleOwner,
            @NonNull final FragmentManager fragmentManager,
            @NonNull PresetBarViewModel presetBarViewModel,
            @NonNull ToolManagerViewModel toolManagerViewModel,
            @NonNull SignatureViewModel signatureViewModel,
            @NonNull ViewGroup container) {
        this(lifecycleOwner,
                fragmentManager,
                presetBarViewModel,
                toolManagerViewModel,
                signatureViewModel,
                new PresetBarView(container),
                null);
    }

    /**
     * Creates a {@link PresetBarComponent} with a specific {@link PresetBarView}
     *
     * @param lifecycleOwner       the lifecycle owner that will handle this UI component's state
     * @param fragmentManager      the fragment manager, which will be used to show the annot style dialog
     * @param presetBarViewModel   the {@link PresetBarViewModel} that manages the preset bar state
     * @param toolManagerViewModel the {@link ToolManagerViewModel} that manages the current ToolManager
     * @param signatureViewModel   the {@link SignatureViewModel} that manages the list of available signatures
     * @param presetBarView        the view that contains the preset bar
     */
    public PresetBarComponent(@NonNull final LifecycleOwner lifecycleOwner,
            @NonNull final FragmentManager fragmentManager,
            @NonNull PresetBarViewModel presetBarViewModel,
            @NonNull ToolManagerViewModel toolManagerViewModel,
            @NonNull SignatureViewModel signatureViewModel,
            @NonNull PresetBarView presetBarView) {
        this(lifecycleOwner,
                fragmentManager,
                presetBarViewModel,
                toolManagerViewModel,
                signatureViewModel,
                presetBarView,
                null);
    }

    public PresetBarComponent(@NonNull final LifecycleOwner lifecycleOwner,
            @NonNull final FragmentManager fragmentManager,
            @NonNull PresetBarViewModel presetBarViewModel,
            @NonNull ToolManagerViewModel toolManagerViewModel,
            @NonNull SignatureViewModel signatureViewModel,
            @NonNull PresetBarView presetBarView,
            @Nullable HashSet<ToolbarButtonType> toolsToHidePresetBar) {
        final Context context = presetBarView.getContext();
        mSignatureSelectionDialog = new SignatureSelectionDialog(fragmentManager);
        mSignaturePresetComponent = new SignaturePresetComponent(
                mSignatureSelectionDialog,
                signatureViewModel,
                mSignatureSelectionDialog,
                context
        );
        mSignaturePresetComponent.setButtonEventListener(
                new SignatureSelectionDialog.ButtonClickListener() {
                    @Override
                    public void onManageClicked() {
                        showSignaturePicker(false);
                    }

                    @Override
                    public void onCreateClicked() {
                        showSignaturePicker(true);
                    }

                    @Override
                    public void onFirstSignatureClicked() {
                        if (mPresetBarViewModel.getPresetBarState() != null && mSignatureViewModel.getSignatures() != null) {
                            String toolbarStyleId = mPresetBarViewModel.getPresetBarState().getToolbarStyleId();
                            List<SignatureData> signatures = mSignatureViewModel.getSignatures();
                            if (signatures.size() > 0) {
                                saveStampPreset(context, AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE, signatures.get(0).getFilePath(), toolbarStyleId, 0);
                                mSignaturePresetComponent.close();
                            }
                        }
                    }

                    @Override
                    public void onSecondSignatureClicked() {
                        if (mPresetBarViewModel.getPresetBarState() != null && mSignatureViewModel.getSignatures() != null) {
                            String toolbarStyleId = mPresetBarViewModel.getPresetBarState().getToolbarStyleId();
                            List<SignatureData> signatures = mSignatureViewModel.getSignatures();
                            if (signatures.size() > 1) {
                                saveStampPreset(context, AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE, signatures.get(1).getFilePath(), toolbarStyleId, 0);
                                mSignaturePresetComponent.close();
                            } else {
                                showSignaturePicker(false);
                            }
                        }
                    }

                    private void showSignaturePicker(boolean forceCreate) {
                        if (mPresetBarViewModel.getPresetBarState() != null) {
                            String toolbarStyleId = mPresetBarViewModel.getPresetBarState().getToolbarStyleId();
                            showSignatureDialog(toolbarStyleId, 0, forceCreate);
                        }
                    }
                });
        mButtonStayDown = PdfViewCtrlSettingsManager.getContinuousAnnotationEdit(context);
        mPresetBarView = presetBarView;
        mPresetBarViewModel = presetBarViewModel;
        mToolManagerViewModel = toolManagerViewModel;
        mToolsToHidePresetBar = toolsToHidePresetBar;
        mSignatureViewModel = signatureViewModel;
        observeToolbarState(context, lifecycleOwner, fragmentManager, mToolManagerViewModel);
        mPresetBarViewModel.observePresetState(lifecycleOwner, new Observer<PresetBarState>() {

            @Override
            public void onChanged(PresetBarState presetState) {
                if (presetState.styleDialogState == PresetBarState.PRESET_STYLE_DIALOG_SHOWN) {
                    mPresetBarView.updatePresetStyle(presetState);
                    return;
                }
                mPresetBarView.updatePresetState(presetState);
                String toolbarStyleId = presetState.getToolbarStyleId();
                if (presetState.isVisible) {
                    if (presetState.isSinglePreset) {
                        mPresetBarView.setSinglePreset(true);
                        if (presetState.getToolbarButtonTypeId() == ToolbarButtonType.STAMP.getValue()) {
                            mPresetBarView.singlePresetWithBackground(false);
                        } else if (presetState.getToolbarButtonTypeId() == ToolbarButtonType.SIGNATURE.getValue()) {
                            mPresetBarView.singlePresetWithBackground(true);
                        }
                    } else {
                        mPresetBarView.setSinglePreset(false);
                    }
                } else {
                    mSignaturePresetComponent.close();
                }
                if (presetState.isSinglePreset) {
                    ToolStyleConfig.getInstance().setLastSelectedPresetIndex(context, presetState.getToolbarButtonTypeId(), presetState.getToolbarStyleId(), 0);
                    PresetButtonState state = presetState.getPresetState(0);
                    ArrayList<AnnotStyle> annotStyles = state.getAnnotStyles();
                    if (annotStyles != null && !annotStyles.isEmpty()) {
                        mPresetBarViewModel.generatePreview(presetState.getToolbarButtonTypeId(), annotStyles.get(0).getStampId());
                    }
                } else {
                    for (int i = 0; i < presetState.getNumberOfPresetStates(); i++) {
                        if (presetState.isPresetSelected(i)) {
                            // Save selected preset position
                            ToolStyleConfig.getInstance().setLastSelectedPresetIndex(context, presetState.getToolbarButtonTypeId(), presetState.getToolbarStyleId(), i);
                            PresetButtonState state = presetState.getPresetState(i);
                            ArrayList<AnnotStyle> annotStyles = state.getAnnotStyles();
                            if (annotStyles != null) {
                                if (presetState.styleDialogState == PresetBarState.PRESET_SELECTED) { // double selected (i.e. double tapped) so show annot style dialog
                                    showPresetDialog(annotStyles, fragmentManager, toolbarStyleId, i);
                                } else { // new state selected, so switch annot styles in the tool
                                    // Set style for tool
                                    ToolManager toolManager = mToolManagerViewModel.getToolManager();
                                    if (toolManager != null) {
                                        Tool tool = (Tool) mToolManagerViewModel.getTool();
                                        if (tool != null) {
                                            if (tool.isEditAnnotTool() && !(tool instanceof AnnotEditRectGroup)) { // if annot edit, then close quickmenu and switch back to previous tool
                                                if (!tool.hasAnnotSelected()) {
                                                    // only set the tool if no annotation selected
                                                    tool = (Tool) toolManager.createTool(tool.getCurrentDefaultToolMode(), null);
                                                    toolManager.setTool(tool);
                                                    tool.setForceSameNextToolMode(mButtonStayDown);
                                                } else {
                                                    // create a fake tool to save preset values
                                                    tool = toolManager.safeCreateTool(tool.getCurrentDefaultToolMode());
                                                }
                                            }
                                            tool.setupAnnotStyles(annotStyles);
                                        }
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }
        });

        mPresetBarViewModel.observeSinglePresetImageFile(lifecycleOwner, new Observer<SinglePresetState>() {
            @Override
            public void onChanged(SinglePresetState singlePresetState) {
                if (singlePresetState != null) {
                    mPresetBarView.updateSinglePreset(singlePresetState);
                }
            }
        });

        // Listen to UI events
        mPresetBarView.addOnPresetButtonClickListener(new PresetBarView.OnPresetViewButtonClickListener() {
            @Override
            public void onPresetButtonClicked(@IntRange(from = 0, to = 3) int index) {
                mPresetBarViewModel.selectPreset(index);
            }
        });
    }

    public void setCompactMode(boolean compactMode) {
        mPresetBarView.setCompactMode(compactMode);
    }

    public void handleAnnotStyleDialogDismiss(AnnotStyleDialogFragment popupWindow) {
        PresetBarState presetBarState = mPresetBarViewModel.getPresetBarState();
        if (presetBarState != null) {
            Pair<PresetButtonState, Integer> presetPair = presetBarState.getActivePresetState();
            if (presetPair != null) {
                int index = presetPair.second;
                handleAnnotStyleDialogDismiss(popupWindow, index, presetBarState.getToolbarStyleId());
            }
        }
    }

    public void handleAnnotStyleDialogDismiss(
            AnnotStyleDialogFragment popupWindow, int index, String toolbarStyleId
    ) {
        Context context = popupWindow.getContext();
        Tool tool = (Tool) mToolManagerViewModel.getTool();
        if (context == null || tool == null) {
            return;
        }
        ArrayList<AnnotStyle> annotStyles = popupWindow.getAnnotStyles();
        for (AnnotStyle style : annotStyles) {
            PdfViewCtrlSettingsManager.setAnnotStylePreset(context,
                    style.getAnnotType(),
                    index,
                    toolbarStyleId,
                    style.toJSONString()
            );
        }
        tool.setupAnnotStyles(annotStyles);

        if (tool instanceof SmartPenInk || tool instanceof SmartPenMarkup) {
            // save tab index
            PdfViewCtrlSettingsManager.setAnnotStylesTabIndex(context,
                    AnnotStyle.CUSTOM_SMART_PEN,
                    index,
                    toolbarStyleId,
                    popupWindow.getCurrentTabIndex()
            );
            // save last used more annot type
            if (popupWindow.getAnnotStyles().size() == 2) {
                PdfViewCtrlSettingsManager.setAnnotStylesMoreAnnotType(context,
                        AnnotStyle.CUSTOM_SMART_PEN,
                        index,
                        toolbarStyleId,
                        popupWindow.getAnnotStyles().get(1).getAnnotType());
            }
        }

        mPresetBarViewModel.dismissStyleDialog();
        mPresetBarViewModel.updateAnnotStyles(annotStyles, index);
    }

    private void showSignatureDialog(@NonNull final String toolbarStyleId, final int index, boolean forceCreate) {
        final ToolManager toolManager = mToolManagerViewModel.getToolManager();
        if (toolManager != null) {
            final ToolManager.Tool tool = mToolManagerViewModel.getTool();
            if (tool instanceof Signature) {
                ((Signature) tool).showSignaturePickerDialog(
                        new OnCreateSignatureListener() {
                            @Override
                            public void onSignatureCreated(@Nullable String filepath, boolean saveSignature) {
                                if (filepath != null) {
                                    saveStampPreset(toolManager.getPDFViewCtrl().getContext(), AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE, filepath, toolbarStyleId, index);
                                    if (!saveSignature) {
                                        StampManager.getInstance().setDelayRemoveSignature(filepath);
                                    }
                                    // When a signature is selected, we will update the last modified date.
                                    // This date will be used to show the last used signatures.
                                    File file = new File(filepath);
                                    long setLastUsedTime = Calendar.getInstance().getTime().getTime();
                                    if (file.exists()) {
                                        file.setLastModified(setLastUsedTime);
                                    }
                                }
                                mSignaturePresetComponent.close();
                            }

                            @Override
                            public void onSignatureFromImage(@Nullable PointF targetPoint, int targetPage, @Nullable Long widget) {
                                toolManager.onImageSignatureSelected(
                                        targetPoint,
                                        targetPage,
                                        widget);
                                mSignaturePresetComponent.close();
                            }

                            @Override
                            public void onAnnotStyleDialogFragmentDismissed(AnnotStyleDialogFragment styleDialog) {
                                ((Signature) tool).handleAnnotStyleDialogFragmentDismissed(styleDialog);
                            }
                        }, new OnDialogDismissListener() {
                            @Override
                            public void onDialogDismiss() {
                                // we want to reload the preset here in case selected preset was deleted
                                mPresetBarViewModel.reloadSignaturePreset(toolManager.getPDFViewCtrl().getContext(), AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE, toolbarStyleId, index);
                            }
                        },
                        forceCreate ? SignatureDialogFragment.DialogMode.MODE_CREATE : SignatureDialogFragment.DialogMode.MODE_SAVED);
            }
        }
    }

    private void showRubberStampDialog(@NonNull final String toolbarStyleId,
            final int index) {
        final ToolManager toolManager = mToolManagerViewModel.getToolManager();
        if (toolManager != null) {
            ToolManager.Tool tool = mToolManagerViewModel.getTool();
            if (tool instanceof RubberStampCreate) {
                ((RubberStampCreate) tool).showRubberStampDialogFragment(new OnRubberStampSelectedListener() {
                    @Override
                    public void onRubberStampSelected(@NonNull String stampLabel) {
                        saveStampPreset(toolManager.getPDFViewCtrl().getContext(), Annot.e_Stamp, stampLabel, toolbarStyleId, index);
                    }

                    @Override
                    public void onRubberStampSelected(@Nullable String stampId, @Nullable Obj stampObj) {
                        if (stampId != null) {
                            saveStampPreset(toolManager.getPDFViewCtrl().getContext(), Annot.e_Stamp, stampId, toolbarStyleId, index);
                        }
                    }
                }, new OnDialogDismissListener() {
                    @Override
                    public void onDialogDismiss() {
                        mPresetBarViewModel.reloadStampPreset(toolManager.getPDFViewCtrl().getContext(), Annot.e_Stamp, toolbarStyleId, index);
                    }
                });
            }
        }
    }

    private void saveStampPreset(Context context, int annotType,
            @NonNull String stampId,
            @NonNull final String toolbarStyleId,
            final int index) {
        mPresetBarViewModel.saveStampPreset(context, annotType, stampId, toolbarStyleId, index);
    }

    private void showCountMeasurementPresetCreateDialog(@NonNull final ArrayList<AnnotStyle> annotStyles,
            @NonNull FragmentManager fragmentManager,
            @NonNull final String toolbarStyleId) {
        ToolManager toolManager = mToolManagerViewModel.getToolManager();

        if (toolManager == null) {
            return;
        }
        if (annotStyles.isEmpty()) {
            return;
        }

        CountToolCreatePresetDialog.Builder builder = new CountToolCreatePresetDialog.Builder();
        builder.setAnnotStyle(annotStyles.get(0));
        CountToolCreatePresetDialog dialog = builder.build();
        dialog.setAnnotStyleProperties(toolManager.getAnnotStyleProperties());
        dialog.setPresetBarViewModel(mPresetBarViewModel);
        dialog.setToolbarStyleId(toolbarStyleId);
        dialog.show(fragmentManager, CountToolCreatePresetDialog.TAG);
    }

    private void showPresetDialog(@NonNull final ArrayList<AnnotStyle> annotStyles,
            @NonNull FragmentManager fragmentManager,
            @NonNull final String toolbarStyleId,
            final int index) {
        ToolManager toolManager = mToolManagerViewModel.getToolManager();
        if (toolManager == null) {
            return;
        }
        if (annotStyles.isEmpty()) {
            return;
        }
        Context context = toolManager.getPDFViewCtrl().getContext();
        if (context == null) {
            return;
        }

        int initialTabIndex = 0;
        String[] tabTitles = null;
        Tool tool = (Tool) mToolManagerViewModel.getTool();
        if (tool instanceof SmartPenInk || tool instanceof SmartPenMarkup) {
            initialTabIndex = PdfViewCtrlSettingsManager.getAnnotStylesTabIndex(toolManager.getPDFViewCtrl().getContext(),
                    AnnotStyle.CUSTOM_SMART_PEN,
                    index,
                    toolbarStyleId
            );
            tabTitles = new String[]{
                    context.getResources().getString(R.string.annot_ink),
                    context.getResources().getString(R.string.annot_text_markup)
            };
        }

        AnnotStyleDialogFragment.Builder builder = new AnnotStyleDialogFragment.Builder(annotStyles.get(0))
                .setShowPreset(false)
                .setWhiteListFont(toolManager.getFreeTextFonts())
                .setFontListFromAsset(toolManager.getFreeTextFontsFromAssets())
                .setFontListFromStorage(toolManager.getFreeTextFontsFromStorage())
                .setInitialTabIndex(initialTabIndex)
                .setTabTitles(tabTitles);
        // If preset bar is tablet preset bar, then anchor style dialog to the preset bar
        // parent layout instead of bottom sheet
        if (mPresetBarView instanceof TabletPresetBarView) {
            builder.setAnchorView(mPresetBarView.getParent());
        }
        if (annotStyles.size() > 1) {
            ArrayList<AnnotStyle> extraAnnotStyles = new ArrayList<>(annotStyles);
            extraAnnotStyles.remove(0); // remove first as that is passed in as annot style for backwards compatibility
            builder.setExtraAnnotStyles(extraAnnotStyles);
            if (tool instanceof SmartPenInk || tool instanceof SmartPenMarkup) {
                ArrayList<Integer> moreTypes = new ArrayList<>();
                moreTypes.add(Annot.e_Highlight);
                moreTypes.add(Annot.e_Underline);
                moreTypes.add(Annot.e_Squiggly);
                moreTypes.add(Annot.e_StrikeOut);
                builder.setMoreAnnotTypes(1, moreTypes);
            }
        }
        final AnnotStyleDialogFragment popupWindow = builder
                .build();

        popupWindow.setCanShowRichContentSwitch(toolManager.isShowRichContentOption());
        popupWindow.setCanShowTextAlignment(!toolManager.isAutoResizeFreeText());
        popupWindow.setCanShowPressureSwitch(true); // pressure switch should always be available when customizing the tool properties
        popupWindow.setAnnotStyleProperties(toolManager.getAnnotStyleProperties());
        popupWindow.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                handleAnnotStyleDialogDismiss(popupWindow, index, toolbarStyleId);
            }
        });

        popupWindow.setOnAnnotStyleChangeListener(new AnnotStyle.OnAnnotStyleChangeListener() {
            @Override
            public void onChangeAnnotThickness(float thickness, boolean done) {
                mPresetBarViewModel.updateAnnotStyles(popupWindow.getAnnotStyles(), index);
            }

            @Override
            public void onChangeAnnotTextSize(float textSize, boolean done) {

            }

            @Override
            public void onChangeAnnotTextColor(int textColor) {
                mPresetBarViewModel.updateAnnotStyles(popupWindow.getAnnotStyles(), index);
            }

            @Override
            public void onChangeAnnotOpacity(float opacity, boolean done) {

            }

            @Override
            public void onChangeAnnotStrokeColor(int color) {
                mPresetBarViewModel.updateAnnotStyles(popupWindow.getAnnotStyles(), index);
            }

            @Override
            public void onChangeAnnotFillColor(int color) {
                mPresetBarViewModel.updateAnnotStyles(popupWindow.getAnnotStyles(), index);
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
        popupWindow.setOnMoreAnnotTypesClickListener(new AnnotStyleView.OnMoreAnnotTypeClickedListener() {
            @Override
            public void onAnnotTypeClicked(int annotType) {
                popupWindow.saveAnnotStyles();
                Context context = popupWindow.getContext();
                Tool tool = (Tool) mToolManagerViewModel.getTool();
                if (context == null || tool == null) {
                    return;
                }

                AnnotStyle annotStyle = popupWindow.getAnnotStyle();
                PdfViewCtrlSettingsManager.setAnnotStylePreset(context,
                        annotStyle.getAnnotType(),
                        index,
                        toolbarStyleId,
                        annotStyle.toJSONString()
                );

                AnnotStyle newStyle = ToolStyleConfig.getInstance().getAnnotPresetStyle(context, annotType, index, toolbarStyleId);
                popupWindow.setAnnotStyle(newStyle);
            }
        });

        popupWindow.show(fragmentManager);
        mPresetBarViewModel.openStyleDialog();
    }

    private void observeToolbarState(
            @NonNull final Context context,
            @NonNull final LifecycleOwner lifecycleOwner,
            @NonNull final FragmentManager fragmentManager,
            @NonNull final ToolManagerViewModel toolManagerViewModel) {
        toolManagerViewModel.observeToolChanges(lifecycleOwner, new Observer<ToolManagerViewModel.ToolChange>() {
            @Override
            public void onChanged(ToolManagerViewModel.ToolChange toolChange) {
                if (context == null || toolChange == null || toolChange.newTool == null) {
                    return;
                }
                Tool tool = toolChange.newTool;
                Bundle bundle = tool.getBundle();
                ToolbarItem toolbarItem = bundle == null ? null : (ToolbarItem) bundle.getParcelable(AnnotationToolbarComponent.TOOLBAR_ITEM_BUNDLE);
                boolean createdFromDisabledTool = bundle != null && bundle.getBoolean(AnnotationToolbarComponent.TOOLMODE_DISABLED);
                boolean presetBarVisible;
                boolean singlePreset;
                if (toolbarItem == null || createdFromDisabledTool) {
                    if (tool.getToolMode() == ToolManager.ToolMode.PAN) {
                        mPresetBarViewModel.hidePresetBar();
                    }
                } else {
                    switch (toolbarItem.toolbarButtonType) {
                        case TEXT_UNDERLINE:
                        case TEXT_STRIKEOUT:
                        case TEXT_HIGHLIGHT:
                        case FREE_HIGHLIGHT:
                        case TEXT_SQUIGGLY:
                        case STICKY_NOTE:
                        case FREE_TEXT:
                        case CALLOUT:
                        case SQUARE:
                        case ERASER:
                        case CIRCLE:
                        case SOUND:
                        case ATTACHMENT:
                        case RULER:
                        case RECT_AREA:
                        case ARROW:
                        case LINE:
                        case STAMP:
                        case SIGNATURE:
                        case DATE:
                        case TEXT_FIELD:
                        case COMBO_BOX:
                        case LIST_BOX:
                        case INK:
                        case SMART_PEN:
                        case POLYGON:
                        case POLYLINE:
                        case AREA:
                        case PERIMETER:
                        case POLY_CLOUD:
                        case LINK:
                        case TEXT_REDACTION:
                        case RECT_REDACTION:
                        case FREE_TEXT_SPACING:
                        case COUNT_MEASUREMENT: {
                            presetBarVisible = true;
                            break;
                        }
                        default: {
                            presetBarVisible = false;
                            break;
                        }
                    }

                    switch (toolbarItem.toolbarButtonType) {
                        case STAMP:
                        case SIGNATURE:
                        case COUNT_MEASUREMENT: {
                            singlePreset = true;
                            break;
                        }
                        default: {
                            singlePreset = false;
                            break;
                        }
                    }

                    if (mToolsToHidePresetBar != null && mToolsToHidePresetBar.contains(toolbarItem.toolbarButtonType)) {
                        presetBarVisible = false;
                    }

                    // Initialize state then show
                    PresetBarState presetStateList = PresetBarState.fromSavedState(
                            context,
                            toolbarItem);
                    presetStateList.isVisible = presetBarVisible;
                    presetStateList.isSinglePreset = singlePreset;
                    mPresetBarViewModel.setPresetBarState(presetStateList);
                }
            }
        });

        mPresetBarView.addOnCloseButtonClickListener(new PresetBarView.OnCloseButtonClickListener() {
            @Override
            public void onCloseButtonClicked() {
                ToolManager toolManager = toolManagerViewModel.getToolManager();
                if (toolManager != null) {
                    Tool tool = (Tool) toolManager.createTool(ToolManager.ToolMode.PAN, null);
                    toolManager.setTool(tool);
                }
                if (!Utils.isTablet(context)) {
                    AnalyticsHandlerAdapter.getInstance().sendEvent(
                            AnalyticsHandlerAdapter.EVENT_NEW_ANNOTATION_TOOLBAR_TOOL_CLOSE,
                            AnalyticsParam.toolToggleClose(false)
                    );
                }
            }
        });

        mPresetBarView.addOnStyleButtonClickListener(new PresetBarView.OnStyleButtonClickListener() {
            @Override
            public void onStyleButtonClicked() {
                PresetBarState presetBarState = mPresetBarViewModel.getPresetBarState();
                Pair<PresetButtonState, Integer> presetPair = presetBarState != null ? presetBarState.getActivePresetState() : null;
                if (presetPair != null) {
                    PresetButtonState presetButtonState = presetPair.first;
                    int index = presetPair.second;
                    ToolManager toolManager = mToolManagerViewModel.getToolManager();
                    if (toolManager != null && presetButtonState != null && fragmentManager != null) {
                        ArrayList<AnnotStyle> annotStyles = presetButtonState.getAnnotStyles();
                        if (annotStyles != null && !annotStyles.isEmpty()) {
                            String toolbarStyleId = presetBarState.getToolbarStyleId();
                            if (presetBarState.getToolbarButtonTypeId() == ToolbarButtonType.STAMP.getValue()) {
                                showRubberStampDialog(toolbarStyleId, index);
                            } else if (presetBarState.getToolbarButtonTypeId() == ToolbarButtonType.SIGNATURE.getValue()) {
                                mSignatureViewModel.populateSignaturesAsync(
                                        mPresetBarView.getContext(),
                                        new Consumer<List<SignatureData>>() {
                                            @Override
                                            public void accept(List<SignatureData> signatureData) throws Exception {
                                                if (mSignatureViewModel.hasSignatures() && toolManager.isShowSavedSignature()) {
                                                    if (mPresetBarView instanceof TabletPresetBarView) {
                                                        mSignatureSelectionDialog.setAnchorView(mPresetBarView.getParent());
                                                    } else {
                                                        mSignatureSelectionDialog.setAnchorView(null);
                                                    }
                                                    mSignaturePresetComponent.show();
                                                } else {
                                                    showSignatureDialog(toolbarStyleId, index, true);
                                                }
                                            }
                                        });
                            } else if (presetBarState.getToolbarButtonTypeId() == ToolbarButtonType.COUNT_MEASUREMENT.getValue()) {
                                if (!mPresetBarViewModel.hasCountMeasurementPresets()) {
                                    showCountMeasurementPresetCreateDialog(annotStyles, fragmentManager, toolbarStyleId);
                                } else {
                                    CountToolDialogFragment dialog = new CountToolDialogFragment();
                                    dialog.setToolbarStyleId(toolbarStyleId);
                                    dialog.setPresetStyle(annotStyles.get(0));
                                    dialog.setToolManagerViewModel(mToolManagerViewModel);
                                    dialog.setPresetViewModel(mPresetBarViewModel);
                                    dialog.setStyle(DialogFragment.STYLE_NO_TITLE, toolManager.getTheme());
                                    dialog.show(fragmentManager, CountToolDialogFragment.TAG);
                                }
                            } else {
                                showPresetDialog(annotStyles, fragmentManager, toolbarStyleId, index);
                            }
                        }
                    }
                }
            }
        });
    }
}
