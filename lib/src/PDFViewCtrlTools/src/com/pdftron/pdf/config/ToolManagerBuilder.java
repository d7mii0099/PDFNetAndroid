//------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------
package com.pdftron.pdf.config;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;
import androidx.annotation.ArrayRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.StyleRes;
import androidx.fragment.app.FragmentActivity;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.controls.PdfViewCtrlTabBaseFragment;
import com.pdftron.pdf.model.AnnotStyleProperty;
import com.pdftron.pdf.tools.AnnotEditRectGroup;
import com.pdftron.pdf.tools.Eraser;
import com.pdftron.pdf.tools.FreeHighlighterCreate;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.SimpleShapeCreate;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.DrawingUtils;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.SnapUtils;
import com.pdftron.pdf.widget.toolbar.builder.AnnotationToolbarBuilder;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * ToolManagerBuilder is a helper for constructing {@link ToolManager} with xml configuration and
 * set {@link ToolManager} to {@link PDFViewCtrl}
 * <p>
 * For example, you can initialize ToolManager as following:
 * <pre>
 *  ToolManager toolManager = ToolManagerBuilder
 *      .from()
 *      .build(getActivity(), mPDFViewCtrl);
 * </pre>
 * where {@code mPDFViewCtrl} is an instance of {@link PDFViewCtrl}
 */
public class ToolManagerBuilder implements Parcelable {
    private boolean editInk = true;
    private boolean addImage = true;
    private boolean openToolbar = true;
    private boolean buildInPageIndicator;
    private boolean annotPermission;
    private boolean showAuthor;
    private boolean textMarkupAdobeHack = true;
    private boolean copyAnnot = true;
    private boolean stylusAsPen;
    private boolean inkSmoothing = true;
    private boolean autoSelect = true;
    private boolean disableQuickMenu;
    private boolean doubleTapToZoom = true;
    private boolean autoResizeFreeText;
    private boolean realtimeAnnotEdit = true;
    private boolean editFreeTextOnTap;
    private int disableToolModesId = -1;
    private int modeSize = 0;
    private String[] modes;
    private SparseArray<Object> customToolClassMap;
    private SparseArray<Object> customToolParamMap;
    private int disableEditingAnnotTypesId = -1;
    private int annotTypeSize = 0;
    private int[] annotTypes;
    private boolean showSavedSignatures = true;
    private boolean defaultStoreNewSignature = true;
    private boolean persistStoreSignatureSetting = true;
    private boolean showAnnotIndicator;
    private boolean showSignatureFromImage = true;
    private boolean annotationLayerEnabled;
    private boolean useDigitalSignature;
    private String digitalSignatureKeystorePath;
    private String digitalSignatureKeystorePassword;
    private int annotToolbarPrecedenceSize = 0;
    private String[] annotToolbarPrecedence;
    private boolean usePressureSensitiveSignatures = true;
    private String eraserType = Eraser.EraserType.INK_ERASER.name();
    private boolean showUndoRedo = true;
    private int selectionBoxMargin = DrawingUtils.sSelectionBoxMargin;
    private boolean freeTextInlineToggleEnabled = true;
    private boolean showRichContentOption = true;
    private HashMap<Integer, AnnotStyleProperty> mAnnotStyleProperties = new HashMap<>();
    private String multiSelectMode = AnnotEditRectGroup.SelectionMode.RECTANGULAR.name();
    private boolean pdfContentEditingEnabled;
    private boolean showSignaturePresets = true;
    private boolean restrictedTapAnnotCreation;
    private boolean showRotateHandle = true;
    private int tapToCreateHalfWidth = SimpleShapeCreate.sTapToCreateHalfWidth;
    private boolean inkMultiStrokeEnabled = true;
    private int freeTextFontsFromAssetSize = 0;
    private String[] freeTextFontsFromAssets;
    private int freeTextFontsFromStorageSize = 0;
    private String[] freeTextFontsFromStorage;
    private boolean moveAnnotBetweenPages = false;
    private boolean freehandTimerEnabled = true;
    private float freeHighlighterAutoSmoothingRange = FreeHighlighterCreate.AUTO_SMOOTH_RANGE_DEFAULT;
    private boolean showTypedSignature = true;
    private int signatureColorsSize = 0;
    private int[] signatureColors;
    private int snappingModes;
    private boolean loupeEnabled = true;

    private ToolManagerBuilder() {
    }

    protected ToolManagerBuilder(Parcel in) {
        editInk = in.readByte() != 0;
        addImage = in.readByte() != 0;
        openToolbar = in.readByte() != 0;
        copyAnnot = in.readByte() != 0;
        stylusAsPen = in.readByte() != 0;
        inkSmoothing = in.readByte() != 0;
        autoSelect = in.readByte() != 0;
        buildInPageIndicator = in.readByte() != 0;
        annotPermission = in.readByte() != 0;
        showAuthor = in.readByte() != 0;
        textMarkupAdobeHack = in.readByte() != 0;
        disableQuickMenu = in.readByte() != 0;
        doubleTapToZoom = in.readByte() != 0;
        autoResizeFreeText = in.readByte() != 0;
        realtimeAnnotEdit = in.readByte() != 0;
        editFreeTextOnTap = in.readByte() != 0;
        disableToolModesId = in.readInt();
        modeSize = in.readInt();
        modes = new String[modeSize];
        in.readStringArray(modes);
        customToolClassMap = in.readSparseArray(Tool.class.getClassLoader());
        customToolParamMap = in.readSparseArray(Object[].class.getClassLoader());
        disableEditingAnnotTypesId = in.readInt();
        annotTypeSize = in.readInt();
        annotTypes = new int[annotTypeSize];
        in.readIntArray(annotTypes);
        showSavedSignatures = in.readByte() != 0;
        defaultStoreNewSignature = in.readByte() != 0;
        persistStoreSignatureSetting = in.readByte() != 0;
        showAnnotIndicator = in.readByte() != 0;
        showSignatureFromImage = in.readByte() != 0;
        annotationLayerEnabled = in.readByte() != 0;
        useDigitalSignature = in.readByte() != 0;
        digitalSignatureKeystorePath = in.readString();
        digitalSignatureKeystorePassword = in.readString();
        annotToolbarPrecedenceSize = in.readInt();
        annotToolbarPrecedence = new String[annotToolbarPrecedenceSize];
        in.readStringArray(annotToolbarPrecedence);
        usePressureSensitiveSignatures = in.readByte() != 0;
        eraserType = in.readString();
        showUndoRedo = in.readByte() != 0;
        selectionBoxMargin = in.readInt();
        freeTextInlineToggleEnabled = in.readByte() != 0;
        showRichContentOption = in.readByte() != 0;
        in.readMap(mAnnotStyleProperties, AnnotStyleProperty.class.getClassLoader());
        multiSelectMode = in.readString();
        pdfContentEditingEnabled = in.readByte() != 0;
        showSignaturePresets = in.readByte() != 0;
        restrictedTapAnnotCreation = in.readByte() != 0;
        showRotateHandle = in.readByte() != 0;
        tapToCreateHalfWidth = in.readInt();
        inkMultiStrokeEnabled = in.readByte() != 0;
        freeTextFontsFromAssetSize = in.readInt();
        freeTextFontsFromAssets = new String[freeTextFontsFromAssetSize];
        in.readStringArray(freeTextFontsFromAssets);
        freeTextFontsFromStorageSize = in.readInt();
        freeTextFontsFromStorage = new String[freeTextFontsFromStorageSize];
        in.readStringArray(freeTextFontsFromStorage);
        moveAnnotBetweenPages = in.readByte() != 0;
        freehandTimerEnabled = in.readByte() != 0;
        freeHighlighterAutoSmoothingRange = in.readFloat();
        showTypedSignature = in.readByte() != 0;
        signatureColorsSize = in.readInt();
        signatureColors = new int[signatureColorsSize];
        in.readIntArray(signatureColors);
        snappingModes = in.readInt();
        loupeEnabled = in.readByte() != 0;
    }

    public static final Creator<ToolManagerBuilder> CREATOR = new Creator<ToolManagerBuilder>() {
        @Override
        public ToolManagerBuilder createFromParcel(Parcel in) {
            return new ToolManagerBuilder(in);
        }

        @Override
        public ToolManagerBuilder[] newArray(int size) {
            return new ToolManagerBuilder[size];
        }
    };

    /**
     * Creates a new ToolManagerBuilder for constructing {@link ToolManager}.
     *
     * @return a new ToolManagerBuilder instance
     */
    public static ToolManagerBuilder from() {
        return new ToolManagerBuilder();
    }

    /**
     * Creates a new ToolManagerBuilder for constructing {@link ToolManager}.
     *
     * @param context  The context
     * @param styleRes style resource that contains tool manager configuration
     * @return a new ToolManagerBuilder instance
     */
    public static ToolManagerBuilder from(Context context, @StyleRes int styleRes) {
        return from().setStyle(context, styleRes);
    }

    /**
     * Sets configuration to tool manager
     *
     * @param styleRes style resource that contains tool manager configuration
     * @return ToolManagerBuilder instance
     */
    public ToolManagerBuilder setStyle(Context context, @StyleRes int styleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolManager, 0, styleRes);
        try {

            String keystoreFilepath = a.getString(R.styleable.ToolManager_digital_signature_keystore_path);
            String keystorePassword = a.getString(R.styleable.ToolManager_digital_signature_keystore_password);
            String customEraserType = a.getString(R.styleable.ToolManager_eraser_type);
            if (null == customEraserType) {
                customEraserType = eraserType;
            }

            setEditInk(a.getBoolean(R.styleable.ToolManager_edit_ink_annots, editInk))
                    .setAddImage(a.getBoolean(R.styleable.ToolManager_add_image_stamper_tool, addImage))
                    .setMoveAnnotationBetweenPages(a.getBoolean(R.styleable.ToolManager_move_annot_between_pages, moveAnnotBetweenPages))
                    .setFreeHandTimerEnabled(a.getBoolean(R.styleable.ToolManager_free_hand_timer_enabled, freehandTimerEnabled))
                    .setFreeHighlighterAutoSmoothingRange(a.getFloat(R.styleable.ToolManager_free_highlighter_auto_smooth_range, freeHighlighterAutoSmoothingRange))
                    .setOpenToolbar(a.getBoolean(R.styleable.ToolManager_open_toolbar_on_pan_ink_selected, openToolbar))
                    .setBuildInPageIndicator(a.getBoolean(R.styleable.ToolManager_build_in_page_number_indicator, buildInPageIndicator))
                    .setAnnotPermission(a.getBoolean(R.styleable.ToolManager_annot_permission_check, annotPermission))
                    .setShowAuthor(a.getBoolean(R.styleable.ToolManager_show_author_dialog, showAuthor))
                    .setTextMarkupAdobeHack(a.getBoolean(R.styleable.ToolManager_text_markup_adobe_hack, textMarkupAdobeHack))
                    .setCopyAnnot(a.getBoolean(R.styleable.ToolManager_copy_annotated_text_to_note, copyAnnot))
                    .setStylusAsPen(a.getBoolean(R.styleable.ToolManager_stylus_as_pen, stylusAsPen))
                    .setInkSmoothing(a.getBoolean(R.styleable.ToolManager_ink_smoothing_enabled, inkSmoothing))
                    .setDisableQuickMenu(a.getBoolean(R.styleable.ToolManager_quick_menu_disable, disableQuickMenu))
                    .setDoubleTapToZoom(a.getBoolean(R.styleable.ToolManager_double_tap_to_zoom, doubleTapToZoom))
                    .setAutoResizeFreeText(a.getBoolean(R.styleable.ToolManager_auto_resize_freetext, autoResizeFreeText))
                    .setRealTimeAnnotEdit(a.getBoolean(R.styleable.ToolManager_realtime_annot_edit, realtimeAnnotEdit))
                    .setEditFreeTextOnTap(a.getBoolean(R.styleable.ToolManager_edit_freetext_on_tap, editFreeTextOnTap))
                    .freeTextInlineToggleEnabled(a.getBoolean(R.styleable.ToolManager_freeText_inline_toggle_enabled, freeTextInlineToggleEnabled))
                    .setShowRichContentOption(a.getBoolean(R.styleable.ToolManager_freeText_show_rich_content_switch, showRichContentOption))
                    .setShowSavedSignatures(a.getBoolean(R.styleable.ToolManager_show_saved_signatures, showSavedSignatures))
                    .setDefaultStoreNewSignature(a.getBoolean(R.styleable.ToolManager_default_store_new_signature, defaultStoreNewSignature))
                    .setPersistStoreSignatureSetting(a.getBoolean(R.styleable.ToolManager_persist_store_signature_setting, persistStoreSignatureSetting))
                    .setShowSignaturePresets(a.getBoolean(R.styleable.ToolManager_show_signature_presets, showSignaturePresets))
                    .setShowAnnotIndicator(a.getBoolean(R.styleable.ToolManager_show_annot_indicator, showAnnotIndicator))
                    .setShowSignatureFromImage(a.getBoolean(R.styleable.ToolManager_show_signature_from_image, showSignatureFromImage))
                    .setShowTypedSignature(a.getBoolean(R.styleable.ToolManager_show_typed_signature, showTypedSignature))
                    .setAutoSelect(a.getBoolean(R.styleable.ToolManager_auto_select_annotation, autoSelect))
                    .setAnnotationLayerEnabled(a.getBoolean(R.styleable.ToolManager_annotation_layer_enabled, annotationLayerEnabled))
                    .setUseDigitalSignature(a.getBoolean(R.styleable.ToolManager_use_digital_signature, useDigitalSignature))
                    .setDigitalSignatureKeystorePath(keystoreFilepath == null ? digitalSignatureKeystorePath : keystoreFilepath)
                    .setDigitalSignatureKeystorePassword(keystorePassword == null ? digitalSignatureKeystorePassword : keystorePassword)
                    .setDisableToolModesId(a.getResourceId(R.styleable.ToolManager_disable_tool_modes, disableToolModesId))
                    .setDisableEditingAnnotTypesId(a.getResourceId(R.styleable.ToolManager_disable_annot_editing_by_types, disableEditingAnnotTypesId))
                    .setUsePressureSensitiveSignatures(a.getBoolean(R.styleable.ToolManager_use_pressure_sensitive_signatures, usePressureSensitiveSignatures))
                    .setEraserType(customEraserType == null ? null : Eraser.EraserType.valueOf(customEraserType))
                    .setShowUndoRedo(a.getBoolean(R.styleable.ToolManager_show_undo_redo, showUndoRedo))
                    .setShowRotateHandle(a.getBoolean(R.styleable.ToolManager_show_rotate_handle, showRotateHandle))
                    .setInkMultiStrokeEnabled(a.getBoolean(R.styleable.ToolManager_ink_multi_stroke_enabled, inkMultiStrokeEnabled))
                    .setSelectionBoxMargin(a.getInteger(R.styleable.ToolManager_selection_box_margin, selectionBoxMargin))
                    .setTapToCreateShapeHalfWidth(a.getInteger(R.styleable.ToolManager_tap_to_create_half_width, tapToCreateHalfWidth))
                    .setPdfContentEditingEnabled(a.getBoolean(R.styleable.ToolManager_pdf_content_editing_enabled, pdfContentEditingEnabled))
                    .setLoupeEnabled(a.getBoolean(R.styleable.ToolManager_loupe_enabled, loupeEnabled))
                    .setRestrictedTapAnnotCreation(a.getBoolean(R.styleable.ToolManager_restricted_tap_annot_creation, restrictedTapAnnotCreation));
            if (disableToolModesId != -1) {
                modes = context.getResources().getStringArray(disableToolModesId);
            }
            if (disableEditingAnnotTypesId != -1) {
                annotTypes = context.getResources().getIntArray(disableEditingAnnotTypesId);
            }
        } finally {
            a.recycle();
        }
        return this;
    }

    /**
     * Sets whether user can edit ink annotation
     *
     * @param editInk true then able to edit ink, false other wise. Default to true.
     * @return ToolManagerBuilder
     */
    public ToolManagerBuilder setEditInk(boolean editInk) {
        this.editInk = editInk;
        return this;
    }

    /**
     * Sets whether user can add stamper image
     *
     * @param addImage true then able to add image, false other wise. Default to true.
     * @return ToolManagerBuilder
     */
    public ToolManagerBuilder setAddImage(boolean addImage) {
        this.addImage = addImage;
        return this;
    }

    /**
     * Sets whether ink tool will open annotation toolbar
     *
     * @param openToolbar if true, click ink from quick menu will
     *                    open the annotation toolbar in ink mode,
     *                    false otherwise. Default to true.
     * @return ToolManagerBuilder
     */
    public ToolManagerBuilder setOpenToolbar(boolean openToolbar) {
        this.openToolbar = openToolbar;
        return this;
    }

    /**
     * Indicates whether to use/show the built-in page number indicator.
     *
     * @param buildInPageIndicator true to show the built-in page number indicator, false
     *                             otherwise. Default to false.
     */
    public ToolManagerBuilder setBuildInPageIndicator(boolean buildInPageIndicator) {
        this.buildInPageIndicator = buildInPageIndicator;
        return this;
    }

    /**
     * Sets whether to check annotation author permission
     *
     * @param annotPermission if true, annotation created by user A cannot be modified by user B,
     *                        else anyone can modify any annotation. Default to false.
     */
    public ToolManagerBuilder setAnnotPermission(boolean annotPermission) {
        this.annotPermission = annotPermission;
        return this;
    }

    /**
     * ets whether to show author dialog the first time when user annotates.
     *
     * @param showAuthor if true, show author dialog the first time when user annotates. Default to false.
     */
    public ToolManagerBuilder setShowAuthor(boolean showAuthor) {
        this.showAuthor = showAuthor;
        return this;
    }

    /**
     * Sets whether the TextMarkup annotations are compatible with Adobe
     * (Adobe's quads don't follow the specification, but they don't handle quads that do).
     * Default to true.
     */
    public ToolManagerBuilder setTextMarkupAdobeHack(boolean textMarkupAdobeHack) {
        this.textMarkupAdobeHack = textMarkupAdobeHack;
        return this;
    }

    /**
     * Sets whether to copy annotated text to note. Default to true.
     *
     * @param copyAnnot enable copy annotated text to note
     */
    public ToolManagerBuilder setCopyAnnot(boolean copyAnnot) {
        this.copyAnnot = copyAnnot;
        return this;
    }

    /**
     * Sets whether to use stylus to draw without entering ink tool. Default to false.
     *
     * @param stylusAsPen enable inking with stylus in pan mode
     */
    public ToolManagerBuilder setStylusAsPen(boolean stylusAsPen) {
        this.stylusAsPen = stylusAsPen;
        return this;
    }

    /**
     * Sets whether to smooth ink annotation. Default to true.
     *
     * @param inkSmoothing enable ink smoothing
     */
    public ToolManagerBuilder setInkSmoothing(boolean inkSmoothing) {
        this.inkSmoothing = inkSmoothing;
        return this;
    }

    /**
     * Sets whether auto select annotation after annotation is created
     *
     * @param autoSelect if true, after creating annotation, it will auto select it and show quick menu. Default to true.
     */
    public ToolManagerBuilder setAutoSelect(boolean autoSelect) {
        this.autoSelect = autoSelect;
        return this;
    }

    /**
     * Sets whether disable showing the long press quick menu
     *
     * @param disableQuickMenu if true, disable showing the long press quick menu. Default to false.
     */
    public ToolManagerBuilder setDisableQuickMenu(boolean disableQuickMenu) {
        this.disableQuickMenu = disableQuickMenu;
        return this;
    }

    /**
     * Sets whether can double tap to zoom. Default to true.
     *
     * @param doubleTapToZoom if true, can double tap to zoom, false otherwise
     */
    public ToolManagerBuilder setDoubleTapToZoom(boolean doubleTapToZoom) {
        this.doubleTapToZoom = doubleTapToZoom;
        return this;
    }

    /**
     * Sets whether can auto resize free text when editing
     *
     * @param autoResizeFreeText if true can auto resize, false otherwise. Default to false.
     */
    public ToolManagerBuilder setAutoResizeFreeText(boolean autoResizeFreeText) {
        this.autoResizeFreeText = autoResizeFreeText;
        return this;
    }

    /**
     * Sets whether annotation editing is real time
     *
     * @param realTimeAnnotEdit true if real time, false otherwise. Default to true.
     */
    public ToolManagerBuilder setRealTimeAnnotEdit(boolean realTimeAnnotEdit) {
        this.realtimeAnnotEdit = realTimeAnnotEdit;
        return this;
    }

    /**
     * Sets whether can edit freetext annotation on tap
     *
     * @param editFreeTextOnTap true if tap will start edit FreeText, false otherwise. Default to false.
     */
    public ToolManagerBuilder setEditFreeTextOnTap(boolean editFreeTextOnTap) {
        this.editFreeTextOnTap = editFreeTextOnTap;
        return this;
    }

    /**
     * Sets whether to show the FreeText inline toggle button
     *
     * @param freeTextInlineToggleEnabled true showing the FreeText inline toggle button. Default to true.
     */
    public ToolManagerBuilder freeTextInlineToggleEnabled(boolean freeTextInlineToggleEnabled) {
        this.freeTextInlineToggleEnabled = freeTextInlineToggleEnabled;
        return this;
    }

    /**
     * Sets whether to show rich content switch for FreeText.
     * This API only takes effect for Lollipop+.
     *
     * @param showRichContentOption true show rich content switch for FreeText. Default to true.
     */
    public ToolManagerBuilder setShowRichContentOption(boolean showRichContentOption) {
        this.showRichContentOption = showRichContentOption;
        return this;
    }

    /**
     * Sets whether to show option to edit original PDF content.
     *
     * @param pdfContentEditingEnabled true if show option to edit original PDF content. Default to false.
     */
    public ToolManagerBuilder setPdfContentEditingEnabled(boolean pdfContentEditingEnabled) {
        this.pdfContentEditingEnabled = pdfContentEditingEnabled;
        return this;
    }

    /**
     * Sets whether can show saved signature in signature dialog
     *
     * @param showSavedSignatures true if saved signature will show in signature dialog, false otherwise. Default to true.
     */
    public ToolManagerBuilder setShowSavedSignatures(boolean showSavedSignatures) {
        this.showSavedSignatures = showSavedSignatures;
        return this;
    }

    /**
     * Sets whether "Store signature" setting in the CreateSignatureFragment dialog should
     * be enabled or disabled by default.
     * <p>
     * If persistStoreSignatureSetting is true, then this default value is used the
     * very first time the user creates a new signature. When the user creates the next
     * signatures, this value is ignored and instead will read their last used "Store signature" setting.
     * <p>
     * If persistStoreSignatureSetting is false, then this default
     * value is used every time the user creates a new signature
     * <p>
     * Default true.
     *
     * @param defaultStoreNewSignature true if show saved signatures, false otherwise
     */
    public ToolManagerBuilder setDefaultStoreNewSignature(boolean defaultStoreNewSignature) {
        this.defaultStoreNewSignature = defaultStoreNewSignature;
        return this;
    }

    /**
     * Sets whether to use the user's last set "Store signature" setting in the
     * CreateSignatureFragment dialog.
     * <p>
     * Default true.
     *
     * @param persistStoreSignatureSetting true if "Store signature" setting should persist the next
     *                                     time a user creates signature, false otherwise
     */
    public ToolManagerBuilder setPersistStoreSignatureSetting(boolean persistStoreSignatureSetting) {
        this.persistStoreSignatureSetting = persistStoreSignatureSetting;
        return this;
    }

    /**
     * Sets whether can show indicator for annotations with comments
     *
     * @param showAnnotIndicator true if show indicator, false otherwise. Default to false.
     */
    public ToolManagerBuilder setShowAnnotIndicator(boolean showAnnotIndicator) {
        this.showAnnotIndicator = showAnnotIndicator;
        return this;
    }

    /**
     * Sets whether can show pick from image in signature dialog
     *
     * @param showSignatureFromImage true if pick from image icon will show in signature dialog, false otherwise. Default to true.
     */
    public ToolManagerBuilder setShowSignatureFromImage(boolean showSignatureFromImage) {
        this.showSignatureFromImage = showSignatureFromImage;
        return this;
    }

    /**
     * Sets whether can show typed signature button in signature dialog
     *
     * @param showTypedSignature true if typed signature will show in signature dialog, false otherwise. Default to true.
     */
    public ToolManagerBuilder setShowTypedSignature(boolean showTypedSignature) {
        this.showTypedSignature = showTypedSignature;
        return this;
    }

    /**
     * Sets whether to show color presets in the signature dialog
     *
     * @param showSignaturePresets true if color presets should be shown in the signature dialog,
     *                             false otherwise. Default to true.
     */
    public ToolManagerBuilder setShowSignaturePresets(boolean showSignaturePresets) {
        this.showSignaturePresets = showSignaturePresets;
        return this;
    }

    /**
     * Sets whether to allow tap to create annotations on another type of annotation, for example for free text and sticky note.
     *
     * @param restrictedTapAnnotCreation true if creating tap type annotations is not allowed on other type of annotations. Default to false.
     */
    public ToolManagerBuilder setRestrictedTapAnnotCreation(boolean restrictedTapAnnotCreation) {
        this.restrictedTapAnnotCreation = restrictedTapAnnotCreation;
        return this;
    }

    /**
     * Sets whether to enable annotation layer
     *
     * @param annotationLayerEnabled true if annotation layer is enabled, false otherwise. Default to false.
     */
    public ToolManagerBuilder setAnnotationLayerEnabled(boolean annotationLayerEnabled) {
        this.annotationLayerEnabled = annotationLayerEnabled;
        return this;
    }

    /**
     * Sets whether to use digital signature instead of signature
     *
     * @param useDigitalSignature true if using digital signature instead of signature,
     *                            false otherwise. Default to false.
     */
    public ToolManagerBuilder setUseDigitalSignature(boolean useDigitalSignature) {
        this.useDigitalSignature = useDigitalSignature;
        return this;
    }

    /**
     * Sets the digital signature keystore file path
     *
     * @param digitalSignatureKeystorePath the keystore file path
     */
    public ToolManagerBuilder setDigitalSignatureKeystorePath(String digitalSignatureKeystorePath) {
        this.digitalSignatureKeystorePath = digitalSignatureKeystorePath;
        return this;
    }

    /**
     * Sets the digital signature keystore password
     *
     * @param digitalSignatureKeystorePassword the keystore password
     */
    public ToolManagerBuilder setDigitalSignatureKeystorePassword(String digitalSignatureKeystorePassword) {
        this.digitalSignatureKeystorePassword = digitalSignatureKeystorePassword;
        return this;
    }

    /**
     * Sets disabled tool modes reference array id
     *
     * @param disableToolModesId disabled tool modes string array id
     * @return ToolManagerBuilder
     */
    public ToolManagerBuilder setDisableToolModesId(@ArrayRes int disableToolModesId) {
        this.disableToolModesId = disableToolModesId;
        return this;
    }

    /**
     * Disable tool modes in tool manager
     *
     * @param toolModes disabled tool modes
     * @return ToolManagerBuilder
     */
    public ToolManagerBuilder disableToolModes(ToolManager.ToolMode[] toolModes) {
        this.modes = new String[toolModes.length];
        for (int i = 0; i < toolModes.length; i++) {
            this.modes[i] = toolModes[i].name();
        }
        return this;
    }

    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public Set<ToolManager.ToolMode> getDisabledToolModes(@NonNull Context context) {
        Set<ToolManager.ToolMode> disabledModes = new HashSet<>();
        String[] tmpModes;
        if (disableToolModesId != -1) {
            tmpModes = context.getResources().getStringArray(disableToolModesId);
        } else if (modes != null) {
            tmpModes = modes;
        } else {
            return disabledModes;
        }
        for (String mode : tmpModes) {
            disabledModes.add(ToolManager.ToolMode.valueOf(mode));
        }
        return disabledModes;
    }

    /**
     * @param toolModes the tool modes in the order of importance
     * @return ToolManagerBuilder
     * @deprecated this property is no longer in use for the new UI
     * use {@link ViewerConfig.Builder#addToolbarBuilder(AnnotationToolbarBuilder)} instead
     * An array that determines which tool icon should show when space is limited
     * (portrait mode on small devices when collapsed)
     */
    @Deprecated
    public ToolManagerBuilder setAnnotToolbarPrecedence(ToolManager.ToolMode[] toolModes) {
        this.annotToolbarPrecedence = new String[toolModes.length];
        for (int i = 0; i < toolModes.length; i++) {
            this.annotToolbarPrecedence[i] = toolModes[i].name();
        }
        return this;
    }

    /**
     * Sets disabled editing annot type reference int id
     *
     * @param disableEditingAnnotId disabled editing of annotation type int array id
     * @return ToolManagerBuilder
     */
    public ToolManagerBuilder setDisableEditingAnnotTypesId(@ArrayRes int disableEditingAnnotId) {
        this.disableEditingAnnotTypesId = disableEditingAnnotId;
        return this;
    }

    /**
     * Disable editing by annot type in tool manager
     *
     * @param annotTypes disabled editing of annot types
     * @return ToolManagerBuilder
     */
    public ToolManagerBuilder disableAnnotEditing(int[] annotTypes) {
        this.annotTypes = new int[annotTypes.length];
        System.arraycopy(annotTypes, 0, this.annotTypes, 0, annotTypes.length);
        return this;
    }

    /**
     * Add customized tool
     *
     * @param tool The customized tool
     * @return ToolManagerBuilder
     */
    public ToolManagerBuilder addCustomizedTool(Tool tool) {
        if (null == customToolClassMap) {
            customToolClassMap = new SparseArray<>();
        }
        customToolClassMap.put(tool.getToolMode().getValue(), tool.getClass());
        return this;
    }

    /**
     * Add customized tool
     *
     * @param toolMode  The customized tool mode
     * @param toolClass The customized tool mode class
     * @return ToolManagerBuilder
     */
    public ToolManagerBuilder addCustomizedTool(ToolManager.ToolModeBase toolMode, Class<? extends Tool> toolClass) {
        if (null == customToolClassMap) {
            customToolClassMap = new SparseArray<>();
        }
        customToolClassMap.put(toolMode.getValue(), toolClass);
        return this;
    }

    /**
     * Add customized tool
     *
     * @param tool   customized tool.
     * @param params parameter for instantiate tool
     * @return ToolManagerBuilder
     */
    public ToolManagerBuilder addCustomizedTool(Tool tool, Object... params) {
        addCustomizedTool(tool);
        if (null == customToolParamMap) {
            customToolParamMap = new SparseArray<>();
        }
        customToolParamMap.put(tool.getToolMode().getValue(), params);
        return this;
    }

    /**
     * Add customized tool
     *
     * @param toolMode  customized tool mode.
     * @param toolClass The customized tool mode class
     * @param params    parameter for instantiate tool
     * @return ToolManagerBuilder
     */
    public ToolManagerBuilder addCustomizedTool(ToolManager.ToolModeBase toolMode, Class<? extends Tool> toolClass, Object... params) {
        addCustomizedTool(toolMode, toolClass);
        if (null == customToolParamMap) {
            customToolParamMap = new SparseArray<>();
        }
        customToolParamMap.put(toolMode.getValue(), params);
        return this;
    }

    /**
     * Sets whether to use pressure sensitivity on styluses for signatures.
     *
     * @param usePressureSensitiveSignatures true if using pressure sensitivity for signatures. Default to true.
     */
    public ToolManagerBuilder setUsePressureSensitiveSignatures(boolean usePressureSensitiveSignatures) {
        this.usePressureSensitiveSignatures = usePressureSensitiveSignatures;
        return this;
    }

    /**
     * Sets the eraser type.
     *
     * @param type the {@link com.pdftron.pdf.tools.Eraser.EraserType} for eraser tool
     *             Default to {@link com.pdftron.pdf.tools.Eraser.EraserType#INK_ERASER}
     */
    public ToolManagerBuilder setEraserType(Eraser.EraserType type) {
        if (type != null) {
            this.eraserType = type.name();
        }
        return this;
    }

    /**
     * Sets the multi-select mode.
     *
     * @param mode the {@link com.pdftron.pdf.tools.AnnotEditRectGroup.SelectionMode} for multi-select tool
     *             Default to {@link com.pdftron.pdf.tools.AnnotEditRectGroup.SelectionMode#RECTANGULAR}
     */
    public ToolManagerBuilder setMultiSelectMode(AnnotEditRectGroup.SelectionMode mode) {
        if (mode != null) {
            this.multiSelectMode = mode.name();
        }
        return this;
    }

    /**
     * Sets whether to show undo/redo buttons in the toolbar.
     *
     * @param showUndoRedo true if showing undo/redo buttons in the toolbar. Default to true.
     */
    public ToolManagerBuilder setShowUndoRedo(boolean showUndoRedo) {
        this.showUndoRedo = showUndoRedo;
        return this;
    }

    /**
     * @return whether to show undo/redo buttons in the toolbar.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public boolean getShowUndoRedo() {
        return this.showUndoRedo;
    }

    /**
     * Sets the margin between annotation bounding box and selection box.
     *
     * @param selectionBoxMargin margin value in DP, default to 16dp.
     */
    public ToolManagerBuilder setSelectionBoxMargin(int selectionBoxMargin) {
        this.selectionBoxMargin = selectionBoxMargin;
        return this;
    }

    /**
     * Sets whether to show rotate handle when annotation selected
     *
     * @param showRotateHandle true if rotation handle should be shown. Default to true.
     */
    public ToolManagerBuilder setShowRotateHandle(boolean showRotateHandle) {
        this.showRotateHandle = showRotateHandle;
        return this;
    }

    /**
     * Sets the half width for tap to create shapes
     *
     * @param tapToCreateHalfWidth the half width in dp, default to 50dp.
     */
    public ToolManagerBuilder setTapToCreateShapeHalfWidth(int tapToCreateHalfWidth) {
        this.tapToCreateHalfWidth = tapToCreateHalfWidth;
        return this;
    }

    /**
     * Sets whether multi-stroke is enabled for ink tool
     *
     * @param inkMultiStrokeEnabled true if enabled, default to true.
     */
    public ToolManagerBuilder setInkMultiStrokeEnabled(boolean inkMultiStrokeEnabled) {
        this.inkMultiStrokeEnabled = inkMultiStrokeEnabled;
        return this;
    }

    /**
     * Sets whether free hand annotations are saved on a timer basis. This flag is ignored if
     * isInkMultiStrokeEnabled() returns false.
     *
     * @param freehandTimerEnabled true if enabled. Default to true.
     */
    public ToolManagerBuilder setFreeHandTimerEnabled(boolean freehandTimerEnabled) {
        this.freehandTimerEnabled = freehandTimerEnabled;
        return this;
    }

    /**
     * Sets how wide the range would be for auto smoothing vertical and horizontal strokes, in dp independent of zoom.
     *
     * @param autoSmoothingRange indicating how wide is the range to auto smooth vertical and horizontal strokes, in dp
     */
    public ToolManagerBuilder setFreeHighlighterAutoSmoothingRange(float autoSmoothingRange) {
        this.freeHighlighterAutoSmoothingRange = autoSmoothingRange;
        return this;
    }

    /**
     * Sets the magnifying loupe when using text markup.
     *
     * @param loupeEnabled indicates if the user wants to enable magnifying loupe on text markup
     */
    public ToolManagerBuilder setLoupeEnabled(boolean loupeEnabled) {
        this.loupeEnabled = loupeEnabled;
        return this;
    }

    /**
     * Sets custom font list from Assets for free text tool
     * if sets font list from Assets, then it is not possible to set font list from storage
     * The system fonts list won't load anymore
     *
     * @param fontsName array of custom font's absolute path from Assets
     */
    public ToolManagerBuilder setFreeTextFontsFromAssets(String[] fontsName) {
        this.freeTextFontsFromAssets = fontsName;
        return this;
    }

    /**
     * Sets custom font list from device storage for free text tool
     * if font list from Assets is not set, then it's possible to set font list from storage
     * The system fonts list won't load anymore
     *
     * @param fontsPath array of custom font's absolute path from device storage
     */
    public ToolManagerBuilder setFreeTextFontsFromStorage(String[] fontsPath) {
        this.freeTextFontsFromStorage = fontsPath;
        return this;
    }

    /**
     * Sets whether to allow moving annotations between pages
     *
     * @param moveAnnotBetweenPages true if moving annotations between pages is enabled, default to false.
     */
    public ToolManagerBuilder setMoveAnnotationBetweenPages(boolean moveAnnotBetweenPages) {
        this.moveAnnotBetweenPages = moveAnnotBetweenPages;
        return this;
    }

    /**
     * Sets the colors used for signing. Minimum one color and maximum three colors, any additional colors are ignored.
     * <p>
     * When set, users will not be able to change these colors through the annotation style dialog.
     *
     * @param signatureColors color resources used for creating signatures.
     */
    public ToolManagerBuilder setSignatureColors(@ColorInt int... signatureColors) {
        int numColors = signatureColors.length;
        this.signatureColors = new int[numColors];
        System.arraycopy(signatureColors, 0, this.signatureColors, 0, numColors);
        return this;
    }

    /**
     * Set the point snapping mode for the SnapToNearestInDoc method.
     * By default SnapToNearestInDoc will snap to line endpoints, midpoints, and intersections.
     *
     * @param modeFlags an array of SnapUtils.SnappingMode
     */
    public ToolManagerBuilder setSnappingModes(SnapUtils.SnappingMode[] modeFlags) {
        this.snappingModes = 0;
        if (modeFlags != null) {
            for (SnapUtils.SnappingMode modeFlag : modeFlags) {
                this.snappingModes = this.snappingModes | modeFlag.value;
            }
        }
        return this;
    }

    /**
     * Building tool manager by given {@link PDFViewCtrl}
     *
     * @param pdfViewCtrl The pdfviewCtrl
     * @return tool manager
     */
    public ToolManager build(@Nullable FragmentActivity activity, @NonNull PDFViewCtrl pdfViewCtrl) {
        ToolManager toolManager = new ToolManager(pdfViewCtrl);
        Context context = pdfViewCtrl.getContext();
        toolManager.setEditInkAnnots(editInk);
        toolManager.setAddImageStamperTool(addImage);
        toolManager.setCanOpenEditToolbarFromPan(openToolbar);
        toolManager.setCopyAnnotatedTextToNoteEnabled(copyAnnot);
        toolManager.setStylusAsPen(stylusAsPen);
        toolManager.setInkSmoothingEnabled(inkSmoothing);
        toolManager.setFreeTextFonts(PdfViewCtrlSettingsManager.getFreeTextFonts(context));
        if (freeTextFontsFromAssets != null) {
            toolManager.setFreeTextFontsFromAssets(new HashSet<>(Arrays.asList(freeTextFontsFromAssets)));
        }
        if (freeTextFontsFromStorage != null) {
            toolManager.setFreeTextFontsFromStorage(new HashSet<>(Arrays.asList(freeTextFontsFromStorage)));
        }
        toolManager.setAutoSelectAnnotation(autoSelect);
        toolManager.setBuiltInPageNumberIndicatorVisible(buildInPageIndicator);
        toolManager.setAnnotPermissionCheckEnabled(annotPermission);
        toolManager.setShowAuthorDialog(showAuthor);
        toolManager.setTextMarkupAdobeHack(textMarkupAdobeHack);
        toolManager.setDisableQuickMenu(disableQuickMenu);
        toolManager.setDoubleTapToZoom(doubleTapToZoom);
        toolManager.setAutoResizeFreeText(autoResizeFreeText);
        toolManager.setRealTimeAnnotEdit(realtimeAnnotEdit);
        toolManager.setEditFreeTextOnTap(editFreeTextOnTap);
        toolManager.freeTextInlineToggleEnabled(freeTextInlineToggleEnabled);
        toolManager.setShowRichContentOption(showRichContentOption);
        toolManager.setPdfContentEditingEnabled(pdfContentEditingEnabled);
        toolManager.setShowSavedSignatures(showSavedSignatures);
        toolManager.setDefaultStoreNewSignature(defaultStoreNewSignature);
        toolManager.setPersistStoreSignatureSetting(persistStoreSignatureSetting);
        toolManager.setShowSignaturePresets(showSignaturePresets);
        toolManager.setShowRotateHandle(showRotateHandle);
        toolManager.setFreeHandTimerEnabled(freehandTimerEnabled);
        toolManager.setFreeHighlighterAutoSmoothingRange(freeHighlighterAutoSmoothingRange);
        toolManager.setMoveAnnotationBetweenPages(moveAnnotBetweenPages);
        toolManager.setInkMultiStrokeEnabled(inkMultiStrokeEnabled);
        toolManager.setShowAnnotIndicators(showAnnotIndicator);
        toolManager.setShowSignatureFromImage(showSignatureFromImage);
        toolManager.setShowTypedSignature(showTypedSignature);
        toolManager.setUsePressureSensitiveSignatures(usePressureSensitiveSignatures);
        toolManager.setSnappingMode(snappingModes);
        toolManager.setLoupeEnabled(loupeEnabled);

        if (eraserType != null) {
            toolManager.setEraserType(Eraser.EraserType.valueOf(eraserType));
        }
        if (multiSelectMode != null) {
            toolManager.setMultiSelectMode(AnnotEditRectGroup.SelectionMode.valueOf(multiSelectMode));
        }
        toolManager.setShowUndoRedo(showUndoRedo);
        toolManager.setSelectionBoxMargin(selectionBoxMargin);
        toolManager.setTapToCreateShapeHalfWidth(tapToCreateHalfWidth);
        if (annotationLayerEnabled) {
            toolManager.enableAnnotationLayer();
        }
        toolManager.setUsingDigitalSignature(useDigitalSignature);
        toolManager.setDigitalSignatureKeystorePath(digitalSignatureKeystorePath);
        toolManager.setDigitalSignatureKeystorePassword(digitalSignatureKeystorePassword);
        toolManager.setRestrictedTapAnnotCreation(restrictedTapAnnotCreation);
        toolManager.setCurrentActivity(activity);

        if (modes == null && disableToolModesId != -1) {
            modes = context.getResources().getStringArray(disableToolModesId);
        }

        if (annotTypes == null && disableEditingAnnotTypesId != -1) {
            annotTypes = context.getResources().getIntArray(disableEditingAnnotTypesId);
        }

        if (modes != null) {
            ArrayList<ToolManager.ToolMode> disabledModes = new ArrayList<>(modes.length);
            for (String mode : modes) {
                disabledModes.add(ToolManager.ToolMode.valueOf(mode));
            }
            toolManager.disableToolMode(disabledModes.toArray(new ToolManager.ToolMode[disabledModes.size()]));
        }
        if (annotToolbarPrecedence != null) {
            ArrayList<ToolManager.ToolMode> modes = new ArrayList<>(annotToolbarPrecedence.length);
            for (String mode : annotToolbarPrecedence) {
                modes.add(ToolManager.ToolMode.valueOf(mode));
            }
            toolManager.setAnnotToolbarPrecedence(modes.toArray(new ToolManager.ToolMode[modes.size()]));
        }
        if (annotTypes != null) {
            toolManager.disableAnnotEditing(ArrayUtils.toObject(annotTypes));
        }
        if (customToolClassMap != null) {
            HashMap<ToolManager.ToolModeBase, Class<? extends Tool>> map = new HashMap<>();
            for (int i = 0; i < customToolClassMap.size(); i++) {
                int toolModeVal = customToolClassMap.keyAt(i);
                ToolManager.ToolModeBase toolMode = ToolManager.ToolMode.toolModeFor(toolModeVal);
                Class<? extends Tool> value = (Class<? extends Tool>) customToolClassMap.valueAt(i);
                map.put(toolMode, value);
            }
            toolManager.addCustomizedTool(map);
        }
        if (customToolParamMap != null) {
            HashMap<ToolManager.ToolModeBase, Object[]> map = new HashMap<>();
            for (int i = 0; i < customToolParamMap.size(); i++) {
                int toolModeVal = customToolParamMap.keyAt(i);
                ToolManager.ToolModeBase toolMode = ToolManager.ToolMode.toolModeFor(toolModeVal);
                Object[] value = (Object[]) customToolParamMap.valueAt(i);
                map.put(toolMode, value);
            }
            toolManager.addCustomizedToolParams(map);
        }
        pdfViewCtrl.setToolManager(toolManager);
        for (AnnotStyleProperty annotStyleProperty : mAnnotStyleProperties.values()) {
            toolManager.addAnnotStyleProperty(annotStyleProperty);
        }

        if (signatureColors != null) {
            toolManager.setSignatureColors(signatureColors);
        }
        return toolManager;
    }

    /**
     * Building tool manager by given {@link com.pdftron.pdf.controls.PdfViewCtrlTabFragment2}
     * or {@link com.pdftron.pdf.controls.PdfViewCtrlTabFragment2} and sets fragment listener
     *
     * @param fragment The PdfViewCtrlTabFragment2
     * @return tool manager
     */
    public ToolManager build(@NonNull PdfViewCtrlTabBaseFragment fragment) {
        ToolManager toolManager = build(fragment.getActivity(), fragment.getPDFViewCtrl());
        toolManager.addToolChangedListener(fragment);
        toolManager.addAnnotationModificationListener(fragment);
        toolManager.addPdfDocModificationListener(fragment);
        toolManager.addPdfTextModificationListener(fragment);
        toolManager.setPreToolManagerListener(fragment);
        toolManager.setQuickMenuListener(fragment);
        toolManager.setBasicAnnotationListener(fragment);
        toolManager.setAdvancedAnnotationListener(fragment);
        toolManager.setFileAttachmentAnnotationListener(fragment);
        toolManager.setOnGenericMotionEventListener(fragment);
        toolManager.addActionGoBackListener(fragment);
        toolManager.addFullSaveListener(fragment);
        return toolManager;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (editInk ? 1 : 0));
        dest.writeByte((byte) (addImage ? 1 : 0));
        dest.writeByte((byte) (openToolbar ? 1 : 0));
        dest.writeByte((byte) (copyAnnot ? 1 : 0));
        dest.writeByte((byte) (stylusAsPen ? 1 : 0));
        dest.writeByte((byte) (inkSmoothing ? 1 : 0));
        dest.writeByte((byte) (autoSelect ? 1 : 0));
        dest.writeByte((byte) (buildInPageIndicator ? 1 : 0));
        dest.writeByte((byte) (annotPermission ? 1 : 0));
        dest.writeByte((byte) (showAuthor ? 1 : 0));
        dest.writeByte((byte) (textMarkupAdobeHack ? 1 : 0));
        dest.writeByte((byte) (disableQuickMenu ? 1 : 0));
        dest.writeByte((byte) (doubleTapToZoom ? 1 : 0));
        dest.writeByte((byte) (autoResizeFreeText ? 1 : 0));
        dest.writeByte((byte) (realtimeAnnotEdit ? 1 : 0));
        dest.writeByte((byte) (editFreeTextOnTap ? 1 : 0));
        dest.writeInt(disableToolModesId);
        if (modes == null) {
            modes = new String[0];
        }
        modeSize = modes.length;
        dest.writeInt(modeSize);
        dest.writeStringArray(modes);
        dest.writeSparseArray(customToolClassMap);
        dest.writeSparseArray(customToolParamMap);
        dest.writeInt(disableEditingAnnotTypesId);
        if (annotTypes == null) {
            annotTypes = new int[0];
        }
        annotTypeSize = annotTypes.length;
        dest.writeInt(annotTypeSize);
        dest.writeIntArray(annotTypes);
        dest.writeByte((byte) (showSavedSignatures ? 1 : 0));
        dest.writeByte((byte) (defaultStoreNewSignature ? 1 : 0));
        dest.writeByte((byte) (persistStoreSignatureSetting ? 1 : 0));
        dest.writeByte((byte) (showAnnotIndicator ? 1 : 0));
        dest.writeByte((byte) (showSignatureFromImage ? 1 : 0));
        dest.writeByte((byte) (annotationLayerEnabled ? 1 : 0));
        dest.writeByte((byte) (useDigitalSignature ? 1 : 0));
        dest.writeString(digitalSignatureKeystorePath);
        dest.writeString(digitalSignatureKeystorePassword);
        if (annotToolbarPrecedence == null) {
            annotToolbarPrecedence = new String[0];
        }
        annotToolbarPrecedenceSize = annotToolbarPrecedence.length;
        dest.writeInt(annotToolbarPrecedenceSize);
        dest.writeStringArray(annotToolbarPrecedence);
        dest.writeByte((byte) (usePressureSensitiveSignatures ? 1 : 0));
        dest.writeString(eraserType);
        dest.writeByte((byte) (showUndoRedo ? 1 : 0));
        dest.writeInt(selectionBoxMargin);
        dest.writeByte((byte) (freeTextInlineToggleEnabled ? 1 : 0));
        dest.writeByte((byte) (showRichContentOption ? 1 : 0));
        dest.writeMap(mAnnotStyleProperties);
        dest.writeString(multiSelectMode);
        dest.writeByte((byte) (pdfContentEditingEnabled ? 1 : 0));
        dest.writeByte((byte) (showSignaturePresets ? 1 : 0));
        dest.writeByte((byte) (restrictedTapAnnotCreation ? 1 : 0));
        dest.writeByte((byte) (showRotateHandle ? 1 : 0));
        dest.writeInt(tapToCreateHalfWidth);
        dest.writeByte((byte) (inkMultiStrokeEnabled ? 1 : 0));
        if (freeTextFontsFromAssets == null) {
            freeTextFontsFromAssets = new String[0];
        }
        freeTextFontsFromAssetSize = freeTextFontsFromAssets.length;
        dest.writeInt(freeTextFontsFromAssetSize);
        dest.writeStringArray(freeTextFontsFromAssets);
        if (freeTextFontsFromStorage == null) {
            freeTextFontsFromStorage = new String[0];
        }
        freeTextFontsFromStorageSize = freeTextFontsFromStorage.length;
        dest.writeInt(freeTextFontsFromStorageSize);
        dest.writeStringArray(freeTextFontsFromStorage);
        dest.writeByte((byte) (moveAnnotBetweenPages ? 1 : 0));
        dest.writeByte((byte) (freehandTimerEnabled ? 1 : 0));
        dest.writeFloat(freeHighlighterAutoSmoothingRange);
        dest.writeByte((byte) (showTypedSignature ? 1 : 0));
        if (signatureColors == null) {
            signatureColors = new int[0];
        }
        signatureColorsSize = signatureColors.length;
        dest.writeInt(signatureColorsSize);
        dest.writeIntArray(signatureColors);
        dest.writeInt(snappingModes);
        dest.writeByte((byte) (loupeEnabled ? 1 : 0));
    }

    /**
     * Add an AnnotStyleProperty used to hide annotation style properties in the annotation style dialog.
     *
     * @param annotStyleProperty the AnnotStyleProperty used to hide annotation style properties
     */
    @SuppressWarnings("ConstantConditions")
    public ToolManagerBuilder addAnnotStyleProperty(@NonNull AnnotStyleProperty annotStyleProperty) {
        mAnnotStyleProperties.put(annotStyleProperty.getAnnotType(), annotStyleProperty);
        return this;
    }
}


