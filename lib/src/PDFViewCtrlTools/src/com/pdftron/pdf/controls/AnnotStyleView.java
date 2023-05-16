//------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------
package com.pdftron.pdf.controls;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.text.Editable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.TooltipCompat;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.asynctask.LoadFontAsyncTask;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.interfaces.OnDialogDismissListener;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.AnnotStyleProperty;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.tools.CountMeasurementCreateTool;
import com.pdftron.pdf.tools.Eraser;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsAnnotStylePicker;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.AnnotationPropertyPreviewView;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.ExpandableGridView;
import com.pdftron.pdf.utils.FontAdapter;
import com.pdftron.pdf.utils.IconPickerGridViewAdapter;
import com.pdftron.pdf.utils.MeasureUtils;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.InertSwitch;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarButtonType;
import com.pdftron.pdf.widget.toolbar.component.ToolModeMapper;
import com.pdftron.pdf.widget.toolbar.component.view.ActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A LinearLayout that can adjust annotation appearance
 */
public class AnnotStyleView extends LinearLayout implements
        TextView.OnEditorActionListener,
        SeekBar.OnSeekBarChangeListener,
        View.OnFocusChangeListener,
        View.OnClickListener,
        GridView.OnItemClickListener,
        AdapterView.OnItemSelectedListener {

    public static final String SOUND_ICON_OUTLINE = "annotation_icon_sound_outline";
    public static final String SOUND_ICON_FILL = "annotation_icon_sound_fill";

    private static final int MAX_PROGRESS = 100;
    private static final int PRESET_SIZE = 4;
    private int mAnnotType = Annot.e_Unknown;
    private Set<String> mWhiteListFonts;
    private Set<String> mFontListFromAsset;
    private Set<String> mFontListFromStorage;

    // View elements
    // more tool types
    private LinearLayout mMoreToolLayout;

    // toolbar
    private OnDialogDismissListener mOnDismissListener;

    // stroke
    private LinearLayout mStrokeLayout;
    private TextView mStrokeColorTextView;
    private AnnotationPropertyPreviewView mStrokePreview;

    // fill
    private LinearLayout mFillLayout;
    private TextView mFillColorTextView;
    private AnnotationPropertyPreviewView mFillPreview;

    // thickness
    private LinearLayout mThicknessLayout;
    private SeekBar mThicknessSeekbar;
    private EditText mThicknessEditText;
    private LinearLayout mThicknessValueGroup;

    // opacity
    private LinearLayout mOpacityLayout;
    private TextView mOpacityTextView;
    private SeekBar mOpacitySeekbar;
    private EditText mOpacityEditText;
    private LinearLayout mOpacityValueGroup;

    // fonts
    private LinearLayout mFontLayout;
    private Spinner mFontSpinner;
    private FontAdapter mFontAdapter;

    // horizontal alignment
    private boolean mCanShowTextAlignment = true;
    private ConstraintLayout mHorizontalTextAlignment;
    private LinearLayout mHorizontalTextAlignmentGroup;
    private ActionButton mHorizontalTextLeft;
    private ActionButton mHorizontalTextCenter;
    private ActionButton mHorizontalTextRight;

    // vertical alignment
    private ConstraintLayout mVerticalTextAlignment;
    private LinearLayout mVerticalTextAlignmentGroup;
    private ActionButton mVerticalTextTop;
    private ActionButton mVerticalTextCenter;
    private ActionButton mVerticalTextBottom;

    // date format
    private LinearLayout mDateFormatLayout;
    private Spinner mDateFormatSpinner;
    private ArrayAdapter<CharSequence> mDateFormatSpinnerAdapter;

    // text color
    private LinearLayout mTextColorLayout;
    private AnnotationPropertyPreviewView mTextColorPreview;

    // text size
    private LinearLayout mTextSizeLayout;
    private SeekBar mTextSizeSeekbar;
    private EditText mTextSizeEditText;

    // icons
    private LinearLayout mIconLayout;
    private ImageView mIconExpandableBtn;
    private ExpandableGridView mIconExpandableGridView;
    private IconPickerGridViewAdapter mIconAdapter;
    private AnnotationPropertyPreviewView mIconPreview;
    private boolean mIconExpanded;

    // stroke style
    private LinearLayout mStrokeStyleLayout;
    private TextView mStrokeStyleTextView;
    private AnnotationPropertyPreviewView mStrokeStylePreview;

    // line start style
    private LinearLayout mLineStartLayout;
    private TextView mLineStartTextView;
    private AnnotationPropertyPreviewView mLineStartPreview;

    // line end style
    private LinearLayout mLineEndLayout;
    private TextView mLineEndTextView;
    private AnnotationPropertyPreviewView mLineEndPreview;

    // ruler unit
    private LinearLayout mRulerUnitLayout;
    private EditText mRulerBaseEditText;
    private Spinner mRulerBaseSpinner;
    private ArrayAdapter<CharSequence> mRulerBaseSpinnerAdapter;
    private EditText mRulerTranslateEditText;
    private Spinner mRulerTranslateSpinner;
    private ArrayAdapter<CharSequence> mRulerTranslateSpinnerAdapter;

    // ruler precision
    private LinearLayout mRulerPrecisionLayout;
    private Spinner mRulerPrecisionSpinner;
    private ArrayAdapter<CharSequence> mRulerPrecisionSpinnerAdapter;

    // snap
    private LinearLayout mSnapLayout;
    private InertSwitch mSnapSwitch;

    // rich text enabled
    private boolean mCanShowRCOption;
    private LinearLayout mRCEnableLayout;
    private InertSwitch mRCEnableSwitch;

    // overlay text layout
    private LinearLayout mTextOverlayLayout;
    private EditText mOverlayEditText;

    // eraser
    private LinearLayout mEraserTypeLayout;
    private InertSwitch mEraserTypeSwitch;

    // ink eraser mode
    private LinearLayout mInkEraserModeLayout;
    private Spinner mInkEraserModeSpinner;
    private ArrayAdapter<CharSequence> mInkEraserModeAdapter;

    // pressure
    private LinearLayout mPressureSensitiveLayout;
    private InertSwitch mPressureSensitiveSwitch;
    private boolean mCanShowPressureOption;

    // presets
    private LinearLayout mPresetContainer;
    private ActionButton[] mPresetViews = new ActionButton[PRESET_SIZE];
    private AnnotStyle[] mPresetStyles = new AnnotStyle[PRESET_SIZE];
    private OnPresetSelectedListener mPresetSelectedListener;

    // attributes

    private float mMaxThickness;
    private float mMinThickness;
    private float mMaxTextSize;
    private float mMinTextSize;
    private boolean mPrevThicknessFocus = false;
    private boolean mPrevOpacityFocus = false;
    private boolean mInitSpinner = true;

    private AnnotStyle.AnnotStyleHolder mAnnotStyleHolder;

    private ArrayList<Integer> mMoreAnnotTypes;
    private OnMoreAnnotTypeClickedListener mMoreAnnotTypeListener;

    // listener
    private OnColorLayoutClickedListener mColorLayoutClickedListener;
    private OnStyleLayoutClickedListener mStyleLayoutClickedListener;
    private boolean mTextJustChanged;
    private boolean mShowPresets = true;

    @Nullable
    private HashMap<Integer, AnnotStyleProperty> mAnnotStyleProperties;

    @Nullable
    private ArrayList<AnnotStyle> mGroupAnnotStyles;

    private AnnotStyleDialogFragment.Theme mTheme;

    /**
     * Class constructor
     */
    public AnnotStyleView(Context context) {
        this(context, null);
    }

    /**
     * Class constructor
     */
    public AnnotStyleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Class constructor
     */
    public AnnotStyleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * set annotation type
     */
    public void setAnnotType(int annotType) {
        setAnnotType(0, annotType);
    }

    /**
     * set annotation type
     */
    public void setAnnotType(int tabIndex, int annotType) {
        mAnnotType = annotType;
        mMaxThickness = ToolStyleConfig.getInstance().getDefaultMaxThickness(getContext(), annotType);
        mMinThickness = ToolStyleConfig.getInstance().getDefaultMinThickness(getContext(), annotType);
        mMinTextSize = ToolStyleConfig.getInstance().getDefaultMinTextSize(getContext());
        mMaxTextSize = ToolStyleConfig.getInstance().getDefaultMaxTextSize(getContext());

        SparseArray<AnnotationPropertyPreviewView> previewViews = mAnnotStyleHolder.getAnnotPreviews();
        AnnotationPropertyPreviewView previewView = previewViews.get(tabIndex);
        previewView.setAnnotType(mAnnotType);
        loadPresets();

        if (getAnnotStyle().hasFont()) {
            mStrokePreview.setAnnotType(mAnnotType);
            // set up font spinner if it is TEXT_CREATE tool
            setupFontSpinner();
        }
        if (mAnnotType == Annot.e_Text || mAnnotType == AnnotStyle.CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT) {
            // get list of note icons
            List<String> source = null;
            source = ToolStyleConfig.getInstance().getIconsList(getContext());
            if (mAnnotType == AnnotStyle.CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT) {
                source.add(CountMeasurementCreateTool.COUNT_MEASURE_CHECKMARK_ICON);
            } else {
                source = ToolStyleConfig.getInstance().getIconsList(getContext());
            }
            mIconAdapter = new IconPickerGridViewAdapter(getContext(), source);
            mIconExpandableGridView.setAdapter(mIconAdapter);
            mIconExpandableGridView.setOnItemClickListener(this);
        }
    }

    /**
     * Sets whether can show rich content option
     */
    public void setCanShowRichContentSwitch(boolean canShow) {
        mCanShowRCOption = canShow;
    }

    public void setCanShowPressureSwitch(boolean canShow) {
        mCanShowPressureOption = canShow;
    }

    /**
     * Load presets from settings
     */
    private void loadPresets() {
        for (int i = 0; i < PRESET_SIZE; i++) {
            ActionButton presetView = mPresetViews[i];
            AnnotStyle preset = ToolStyleConfig.getInstance().getAnnotPresetStyle(getContext(), mAnnotType, i);
            ToolbarButtonType buttonType = ToolModeMapper.getButtonType(mAnnotType);
            int icon = buttonType != null ? buttonType.icon : R.drawable.ic_annotation_freehand_black_24dp;
            Drawable drawable = getResources().getDrawable(icon);
            presetView.setIconColor(mTheme.presetIconColor);
            presetView.setSelectedIconColor(mTheme.selectedPresetIconColor);
            presetView.setSelectedBackgroundColor(mTheme.selectedPresetBackgroundColor);
            presetView.setCheckable(true);
            presetView.setIcon(drawable);
            presetView.setShowIconHighlightColor(true);
            presetView.setAlwaysShowIconHighlightColor(true);
            presetView.setIconHighlightColor(ActionButton.getPreviewColor(preset));
            ArrayList<AnnotStyle> annotStyles = new ArrayList<>(1);
            annotStyles.add(preset);
            presetView.updateAppearance(annotStyles);
            preset.bindPreview(presetView);
            if (!preset.getFont().hasFontName() && mFontAdapter != null && mFontAdapter.getData() != null && mFontAdapter.getData().size() > 1) {
                preset.setFont(mFontAdapter.getData().get(1));
            }
            // snapping mode and rich text mode is global setting
            preset.setSnap(getAnnotStyle().getSnap());
            preset.setTextHTMLContent(getAnnotStyle().getTextHTMLContent());
            mPresetStyles[i] = preset;
        }
    }

    /**
     * Sets annotation style holder
     *
     * @param annotStyleHolder annotation style holder
     */
    public void setAnnotStyleHolder(AnnotStyle.AnnotStyleHolder annotStyleHolder) {
        mAnnotStyleHolder = annotStyleHolder;
    }

    private AnnotStyle getAnnotStyle() {
        return mAnnotStyleHolder.getAnnotStyle();
    }

    private void setIcon(String icon) {
        getAnnotStyle().setIcon(icon);
        int iconPosition = mIconAdapter.getItemIndex(icon);
        mIconAdapter.setSelected(iconPosition);
        mAnnotStyleHolder.getAnnotPreview().setImageDrawable(getAnnotStyle().getIconDrawable(getContext()));
        mIconPreview.setImageDrawable(AnnotStyle.getIconDrawable(getContext(), getAnnotStyle().getIcon(), getAnnotStyle().getColor(), 1));
    }

    /**
     * Sets white font list for Free text fonts spinner
     *
     * @param whiteFontList white font list
     */
    public void setWhiteFontList(Set<String> whiteFontList) {
        mWhiteListFonts = whiteFontList;
        if (!checkPresets()) {
            setFontSpinner();
        }
    }

    /**
     * Sets font list from asset for Free text fonts spinner
     *
     * @param fontList font list
     */
    public void setFontListFromAsset(Set<String> fontList) {
        mFontListFromAsset = fontList;
        if (!checkPresets()) {
            setFontSpinner();
        }
    }

    /**
     * Sets font list from storage for Free text fonts spinner
     *
     * @param fontList font list
     */
    public void setFontListFromStorage(Set<String> fontList) {
        mFontListFromStorage = fontList;
        if (!checkPresets()) {
            setFontSpinner();
        }
    }

    /**
     * Sets more tools to display
     *
     * @param annotTypes The tools to display
     */
    public void setMoreAnnotTypes(ArrayList<Integer> annotTypes) {
        mMoreAnnotTypes = annotTypes;
        // clear more tool layout except the first one
        View firstView = mMoreToolLayout.getChildAt(0);
        mMoreToolLayout.removeAllViews();
        mMoreToolLayout.addView(firstView);
        for (final Integer type : mMoreAnnotTypes) {
            ActionButton imageButton = getAnnotTypeButtonForTool(type);
            imageButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!v.isSelected() && mMoreAnnotTypeListener != null) {
                        mMoreAnnotTypeListener.onAnnotTypeClicked(type);
                    }
                }
            });
            mMoreToolLayout.addView(imageButton);
        }
        mMoreToolLayout.setVisibility(annotTypes.isEmpty() ? GONE : VISIBLE);
    }

    public void updateAnnotTypes() {
        if (mMoreAnnotTypes == null || mMoreAnnotTypes.isEmpty()) {
            return;
        }
        int annotTypeIndex = mMoreAnnotTypes.indexOf(mAnnotType);

        int childCount = mMoreToolLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mMoreToolLayout.getChildAt(i);
            if (child instanceof ActionButton) {
                child.setSelected(i == (annotTypeIndex + 1));
            }
        }
    }

    private void setFont(FontResource font) {
        getAnnotStyle().setFont(font);
        setFontSpinner();
    }

    private void setHorizontalAlignment(int alignment) {
        getAnnotStyle().setHorizontalAlignment(alignment);
    }

    private void setVerticalAlignment(int alignment) {
        getAnnotStyle().setVerticalAlignment(alignment);
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.controls_annotation_styles, this);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setOrientation(VERTICAL);

        // toolbar
        AppCompatImageButton backButton = findViewById(R.id.back_btn);
        backButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnDismissListener != null) {
                    mOnDismissListener.onDialogDismiss();
                }
            }
        });

        // stroke layout
        mStrokeLayout = findViewById(R.id.stroke_color_layout);
        mStrokeColorTextView = findViewById(R.id.stroke_color_textivew);
        mStrokePreview = findViewById(R.id.stroke_preview);

        // more tools
        mMoreToolLayout = findViewById(R.id.more_tools_layout);

        // fill layout
        mFillLayout = findViewById(R.id.fill_color_layout);
        mFillColorTextView = findViewById(R.id.fill_color_textview);
        mFillPreview = findViewById(R.id.fill_preview);

        // thickness layout
        mThicknessLayout = findViewById(R.id.thickness_layout);
        mThicknessSeekbar = findViewById(R.id.thickness_seekbar);
        mThicknessEditText = findViewById(R.id.thickness_edit_text);
        mThicknessValueGroup = findViewById(R.id.thickness_value_group);

        // opacity layout
        mOpacityLayout = findViewById(R.id.opacity_layout);
        mOpacityTextView = findViewById(R.id.opacity_textivew);
        mOpacitySeekbar = findViewById(R.id.opacity_seekbar);
        mOpacityEditText = findViewById(R.id.opacity_edit_text);
        mOpacityValueGroup = findViewById(R.id.opacity_value_group);

        // icons
        mIconLayout = findViewById(R.id.icon_layout);
        mIconExpandableBtn = findViewById(R.id.icon_expandable_btn);
        mIconExpandableGridView = findViewById(R.id.icon_grid);
        mIconPreview = findViewById(R.id.icon_preview);
        mIconExpandableGridView.setExpanded(true);
        mIconLayout.setOnClickListener(this);

        // Stroke style
        mStrokeStyleLayout = findViewById(R.id.stroke_style_layout);
        mStrokeStyleTextView = findViewById(R.id.stroke_style_textview);
        mStrokeStylePreview = findViewById(R.id.stroke_style_fill_preview);
        mStrokeStyleLayout.setOnClickListener(this);

        // line start style
        mLineStartLayout = findViewById(R.id.line_start_layout);
        mLineStartTextView = findViewById(R.id.line_start_textview);
        mLineStartPreview = findViewById(R.id.line_start_fill_preview);
        mLineStartLayout.setOnClickListener(this);

        // line end style
        mLineEndLayout = findViewById(R.id.line_end_layout);
        mLineEndTextView = findViewById(R.id.line_start_textview);
        mLineEndPreview = findViewById(R.id.line_end_fill_preview);
        mLineEndLayout.setOnClickListener(this);

        // font layout
        mFontLayout = findViewById(R.id.font_layout);
        mFontSpinner = findViewById(R.id.font_dropdown);

        // text horizontal alignment layout
        mHorizontalTextAlignment = findViewById(R.id.horizontal_text_alignment);
        mHorizontalTextAlignmentGroup = findViewById(R.id.horizontal_text_alignment_group);
        mHorizontalTextLeft = findViewById(R.id.horizontal_left_align);
        mHorizontalTextCenter = findViewById(R.id.horizontal_center_align);
        mHorizontalTextRight = findViewById(R.id.horizontal_right_align);

        // text vertical alignment layout
        mVerticalTextAlignment = findViewById(R.id.vertical_text_alignment);
        mVerticalTextAlignmentGroup = findViewById(R.id.vertical_text_alignment_group);
        mVerticalTextTop = findViewById(R.id.vertical_top_align);
        mVerticalTextCenter = findViewById(R.id.vertical_center_align);
        mVerticalTextBottom = findViewById(R.id.vertical_bottom_align);

        // date format layout
        mDateFormatLayout = findViewById(R.id.date_format_layout);
        mDateFormatSpinner = findViewById(R.id.date_format_spinner);

        // Apply colors
        mTheme = AnnotStyleDialogFragment.Theme.fromContext(getContext());

        View background = findViewById(R.id.background);
        background.getBackground().mutate().setColorFilter(mTheme.annotPreviewBackgroundColor, PorterDuff.Mode.SRC_IN);
        View attributesBackground = findViewById(R.id.attribute_background);
        attributesBackground.setBackgroundColor(mTheme.backgroundColor);

        ImageView textColorExpandBtn = findViewById(R.id.text_color_expand_button);
        ImageView strokeColorExpandBtn = findViewById(R.id.stroke_color_expand_button);
        ImageView fillColorExpandBtn = findViewById(R.id.fill_color_expand_button);

        mIconExpandableBtn.setColorFilter(mTheme.iconColor);
        textColorExpandBtn.setColorFilter(mTheme.iconColor);
        strokeColorExpandBtn.setColorFilter(mTheme.iconColor);
        fillColorExpandBtn.setColorFilter(mTheme.iconColor);
        backButton.setColorFilter(mTheme.iconColor);
        mStrokeStylePreview.setColorFilter(mTheme.iconColor);
        mLineStartPreview.setColorFilter(mTheme.iconColor);
        mLineEndPreview.setColorFilter(mTheme.iconColor);

        TextView title = findViewById(R.id.toolbar_title);
        title.setTextColor(mTheme.textColor);
        TextView moreTools = findViewById(R.id.more_tools_textview);
        moreTools.setTextColor(mTheme.textColor);
        TextView textColor = findViewById(R.id.text_color_textivew);
        textColor.setTextColor(mTheme.textColor);
        TextView thicknessLabel = findViewById(R.id.thickness_textview);
        thicknessLabel.setTextColor(mTheme.textColor);
        TextView unitThicknessLabel = findViewById(R.id.unit_thickness);
        unitThicknessLabel.setTextColor(mTheme.textColor);
        TextView textSizeLabel = findViewById(R.id.text_size_textview);
        textSizeLabel.setTextColor(mTheme.textColor);
        TextView unitTextSizeLabel = findViewById(R.id.unit_text_size);
        unitTextSizeLabel.setTextColor(mTheme.textColor);
        TextView opacityPercentlabel = findViewById(R.id.opacity_percent);
        opacityPercentlabel.setTextColor(mTheme.textColor);
        TextView pressureLabel = findViewById(R.id.pressure_sensitive_enabled_textview);
        pressureLabel.setTextColor(mTheme.textColor);
        TextView rulerUnitLabel = findViewById(R.id.ruler_unit_textivew);
        rulerUnitLabel.setTextColor(mTheme.textColor);
        TextView rulerEqualLabel = findViewById(R.id.ruler_equals);
        rulerEqualLabel.setTextColor(mTheme.textColor);
        TextView rulerPrecisionLabel = findViewById(R.id.ruler_precision_textivew);
        rulerPrecisionLabel.setTextColor(mTheme.textColor);
        TextView snapLabel = findViewById(R.id.snap_textview);
        snapLabel.setTextColor(mTheme.textColor);
        TextView overlayLabel = findViewById(R.id.overlay_textview);
        overlayLabel.setTextColor(mTheme.textColor);
        TextView fontLabel = findViewById(R.id.font_textview);
        fontLabel.setTextColor(mTheme.textColor);
        TextView richTextLabel = findViewById(R.id.rich_text_enabled_textview);
        richTextLabel.setTextColor(mTheme.textColor);
        TextView dateFormatLabel = findViewById(R.id.date_format_textview);
        dateFormatLabel.setTextColor(mTheme.textColor);
        TextView iconLabel = findViewById(R.id.icon_textview);
        iconLabel.setTextColor(mTheme.textColor);
        TextView eraserModeLabel = findViewById(R.id.eraser_mode);
        eraserModeLabel.setTextColor(mTheme.textColor);
        TextView inkEraserMode = findViewById(R.id.ink_eraser_mode);
        inkEraserMode.setTextColor(mTheme.textColor);
        TextView presetLabel = findViewById(R.id.preset_label);
        presetLabel.setTextColor(mTheme.textColor);

        mStrokeColorTextView.setTextColor(mTheme.textColor);
        mFillColorTextView.setTextColor(mTheme.textColor);
        mOpacityTextView.setTextColor(mTheme.textColor);
        mStrokeStyleTextView.setTextColor(mTheme.textColor);
        mLineStartTextView.setTextColor(mTheme.textColor);
        mLineEndTextView.setTextColor(mTheme.textColor);

        final CharSequence[] strings = getContext().getResources().getTextArray(R.array.style_picker_date_formats);
        final CharSequence[] nowStrings = new CharSequence[strings.length];
        for (int i = 0; i < strings.length; i++) {
            CharSequence c = strings[i];
            SimpleDateFormat dateFormat = new SimpleDateFormat(c.toString(), Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            String dateStr = dateFormat.format(cal.getTime());
            nowStrings[i] = dateStr;
        }
        mDateFormatSpinnerAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, 0, Arrays.asList(nowStrings));
        mDateFormatSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDateFormatSpinner.setAdapter(mDateFormatSpinnerAdapter);
        mDateFormatSpinner.setOnItemSelectedListener(this);

        // text color layout
        mTextColorLayout = findViewById(R.id.text_color_layout);
        mTextColorPreview = findViewById(R.id.text_color_preview);
        mTextColorPreview.setAnnotType(Annot.e_FreeText);
        mTextColorLayout.setOnClickListener(this);

        // text size layout
        mTextSizeLayout = findViewById(R.id.text_size_layout);
        mTextSizeSeekbar = findViewById(R.id.text_size_seekbar);
        mTextSizeEditText = findViewById(R.id.text_size_edit_text);
        mTextSizeSeekbar.setOnSeekBarChangeListener(this);
        mTextSizeEditText.setOnFocusChangeListener(this);
        mTextSizeEditText.setOnEditorActionListener(this);

        // ruler layout
        // unit conversion
        // document
        mRulerUnitLayout = findViewById(R.id.ruler_unit_layout);
        mRulerBaseEditText = findViewById(R.id.ruler_base_edit_text);
        mRulerBaseSpinner = findViewById(R.id.ruler_base_unit_spinner);
        mRulerBaseSpinnerAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.ruler_base_unit, android.R.layout.simple_spinner_item);
        mRulerBaseSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mRulerBaseSpinner.setAdapter(mRulerBaseSpinnerAdapter);
        mRulerBaseSpinner.setOnItemSelectedListener(this);
        // world
        mRulerTranslateEditText = findViewById(R.id.ruler_translate_edit_text);
        mRulerTranslateSpinner = findViewById(R.id.ruler_translate_unit_spinner);
        mRulerTranslateSpinnerAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.ruler_translate_unit, android.R.layout.simple_spinner_item);
        mRulerTranslateSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mRulerTranslateSpinner.setAdapter(mRulerTranslateSpinnerAdapter);
        mRulerTranslateSpinner.setOnItemSelectedListener(this);

        // precision
        mRulerPrecisionLayout = findViewById(R.id.ruler_precision_layout);
        mRulerPrecisionSpinner = findViewById(R.id.ruler_precision_spinner);
        List<CharSequence> localizedPrecisionArray = RulerPrecisionAdapter.toLocalized(getContext());
        mRulerPrecisionSpinnerAdapter = new RulerPrecisionAdapter(getContext(), localizedPrecisionArray);
        mRulerPrecisionSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mRulerPrecisionSpinner.setAdapter(mRulerPrecisionSpinnerAdapter);
        mRulerPrecisionSpinner.setOnItemSelectedListener(this);

        // snap
        mSnapLayout = findViewById(R.id.snap_layout);
        mSnapSwitch = findViewById(R.id.snap_switch);
        mSnapLayout.setOnClickListener(this);

        // rich text enabled
        mRCEnableLayout = findViewById(R.id.rich_text_enabled_layout);
        mRCEnableSwitch = findViewById(R.id.rich_text_enabled_switch);
        mRCEnableLayout.setOnClickListener(this);

        // redaction and watermark
        mTextOverlayLayout = findViewById(R.id.overlay_text_layout);
        mOverlayEditText = findViewById(R.id.overlay_edittext);

        // eraser
        mEraserTypeLayout = findViewById(R.id.eraser_type);
        mEraserTypeSwitch = findViewById(R.id.eraser_type_switch);
        mEraserTypeLayout.setOnClickListener(this);

        // ink eraser mode
        mInkEraserModeLayout = findViewById(R.id.eraser_ink_mode);
        mInkEraserModeSpinner = findViewById(R.id.eraser_ink_mode_spinner);
        mInkEraserModeAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.style_ink_eraser_mode, android.R.layout.simple_spinner_item);
        mInkEraserModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mInkEraserModeSpinner.setAdapter(mInkEraserModeAdapter);
        mInkEraserModeSpinner.setOnItemSelectedListener(this);

        // pressure sensitivity
        mPressureSensitiveLayout = findViewById(R.id.pressure_sensitive_layout);
        mPressureSensitiveSwitch = findViewById(R.id.pressure_sensitive_enabled_switch);
        mPressureSensitiveLayout.setOnClickListener(this);

        // presets
        mPresetContainer = findViewById(R.id.presets_layout);
        mPresetViews[0] = findViewById(R.id.preset0);
        mPresetViews[1] = findViewById(R.id.preset1);
        mPresetViews[2] = findViewById(R.id.preset2);
        mPresetViews[3] = findViewById(R.id.preset3);

        for (ActionButton presetView : mPresetViews) {
            presetView.setOnClickListener(this);
        }

        // listeners
        mStrokeLayout.setOnClickListener(this);
        mFillLayout.setOnClickListener(this);

        mThicknessSeekbar.setOnSeekBarChangeListener(this);
        mOpacitySeekbar.setOnSeekBarChangeListener(this);

        mThicknessEditText.setOnEditorActionListener(this);
        mOpacityEditText.setOnEditorActionListener(this);
        mRulerBaseEditText.setOnEditorActionListener(this);
        mRulerTranslateEditText.setOnEditorActionListener(this);
        mOverlayEditText.setOnEditorActionListener(this);

        mThicknessEditText.setOnFocusChangeListener(this);
        mOpacityEditText.setOnFocusChangeListener(this);
        mRulerBaseEditText.setOnFocusChangeListener(this);
        mRulerTranslateEditText.setOnFocusChangeListener(this);
        mOverlayEditText.setOnFocusChangeListener(this);

        mThicknessValueGroup.setOnClickListener(this);
        mOpacityValueGroup.setOnClickListener(this);

        setupHorizontalAlignmentButton(mHorizontalTextLeft, R.drawable.ic_format_align_left_24px);
        setupHorizontalAlignmentButton(mHorizontalTextCenter, R.drawable.ic_format_align_center_24px);
        setupHorizontalAlignmentButton(mHorizontalTextRight, R.drawable.ic_format_align_right_24px);

        setupVerticalAlignmentButton(mVerticalTextTop, R.drawable.ic_vertical_top_align);
        setupVerticalAlignmentButton(mVerticalTextCenter, R.drawable.ic_vertical_center_align);
        setupVerticalAlignmentButton(mVerticalTextBottom, R.drawable.ic_vertical_bottom_align);
    }

    private void setupHorizontalAlignmentButton(@NonNull ActionButton actionButton, @DrawableRes int icon) {
        actionButton.setCheckable(true);
        actionButton.setShowIconHighlightColor(false);
        actionButton.setIconColor(mTheme.iconColor);
        actionButton.setSelectedIconColor(mTheme.selectedPresetIconColor);
        actionButton.setSelectedBackgroundColor(mTheme.selectedBackgroundColor);
        actionButton.setIcon(getContext().getResources().getDrawable(icon));
        actionButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onTextAlignmentClick(actionButton, Arrays.asList(mHorizontalTextLeft, mHorizontalTextCenter, mHorizontalTextRight), true);
            }
        });
    }

    private void setupVerticalAlignmentButton(@NonNull ActionButton actionButton, @DrawableRes int icon) {
        actionButton.setCheckable(true);
        actionButton.setShowIconHighlightColor(false);
        actionButton.setIconColor(mTheme.iconColor);
        actionButton.setSelectedIconColor(mTheme.selectedPresetIconColor);
        actionButton.setSelectedBackgroundColor(mTheme.selectedBackgroundColor);
        actionButton.setIcon(getContext().getResources().getDrawable(icon));
        actionButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onTextAlignmentClick(actionButton, Arrays.asList(mVerticalTextTop, mVerticalTextCenter, mVerticalTextBottom), false);
            }
        });
    }

    private void onTextAlignmentClick(@NonNull ActionButton actionButton, @NonNull List<ActionButton> otherButtons, boolean horizontalAlignment) {
        deselectOtherButtons(actionButton, otherButtons);
        if (actionButton.isSelected()) {
            actionButton.deselect();
            clearTextAlignment(horizontalAlignment);
        } else {
            actionButton.select();
            setTextAlignmentFromButtonPress(actionButton);
        }
    }

    private void clearTextAlignment(boolean horizontalAlignment) {
        if (horizontalAlignment) {
            getAnnotStyle().setHorizontalAlignment(Gravity.NO_GRAVITY);
        } else {
            getAnnotStyle().setVerticalAlignment(Gravity.NO_GRAVITY);
        }
    }

    private void setTextAlignmentFromButtonPress(@NonNull ActionButton actionButton) {
        if (actionButton.getId() == mHorizontalTextLeft.getId()) {
            getAnnotStyle().setHorizontalAlignment(Gravity.LEFT);
        } else if (actionButton.getId() == mHorizontalTextCenter.getId()) {
            getAnnotStyle().setHorizontalAlignment(Gravity.CENTER_HORIZONTAL);
        } else if (actionButton.getId() == mHorizontalTextRight.getId()) {
            getAnnotStyle().setHorizontalAlignment(Gravity.RIGHT);
        } else if (actionButton.getId() == mVerticalTextTop.getId()) {
            getAnnotStyle().setVerticalAlignment(Gravity.TOP);
        } else if (actionButton.getId() == mVerticalTextCenter.getId()) {
            getAnnotStyle().setVerticalAlignment(Gravity.CENTER_VERTICAL);
        } else if (actionButton.getId() == mVerticalTextBottom.getId()) {
            getAnnotStyle().setVerticalAlignment(Gravity.BOTTOM);
        }
    }

    private void deselectOtherButtons(@NonNull ActionButton actionButton, @NonNull List<ActionButton> otherButtons) {
        for (ActionButton button : otherButtons) {
            if (!actionButton.equals(button)) {
                button.setSelected(false);
            }
        }
    }

    /**
     * hide this annotation style view
     */
    public void dismiss() {
        setVisibility(GONE);
    }

    /**
     * Show this view
     */
    public void show() {
        setVisibility(VISIBLE);
        initLayoutStyle();
    }

    /**
     * Check if there is any annotation preset matches current annotation style.
     * If true, then call {@link OnPresetSelectedListener#onPresetSelected(AnnotStyle)}
     *
     * @return true then there is matched preset, false otherwise
     */
    public boolean checkPresets() {
        int i = 0;
        for (AnnotStyle preset : mPresetStyles) {
            if (preset == null) {
                break;
            }
            if (preset != getAnnotStyle() && preset.equals(getAnnotStyle())) {
                if (mPresetSelectedListener != null) {
                    // end initialize layout style if listener is not null
                    mPresetSelectedListener.onPresetSelected(preset);
                    AnalyticsAnnotStylePicker.getInstance().setPresetIndex(i);
                    return true;
                }
            }
            i++;
        }
        return false;
    }

    private void initLayoutStyle() {
        mAnnotStyleHolder.getAnnotPreview().updateFillPreview(getAnnotStyle());

        int backgroundColor = Utils.getBackgroundColor(getContext());

        // set stroke preview
        Drawable drawable;
        if (getAnnotStyle().getColor() == Color.TRANSPARENT) {
            drawable = getContext().getResources().getDrawable(R.drawable.oval_fill_transparent);
        } else if (getAnnotStyle().getColor() == backgroundColor) {
            if (getAnnotStyle().hasFillColor()) {
                drawable = getContext().getResources().getDrawable(R.drawable.ring_stroke_preview);
            } else {
                drawable = getContext().getResources().getDrawable(R.drawable.oval_stroke_preview);
            }

            drawable.mutate();
            ((GradientDrawable) drawable).setStroke((int) Utils.convDp2Pix(getContext(), 1), Color.GRAY);
        } else {
            if (getAnnotStyle().hasFillColor()) {
                drawable = getContext().getResources().getDrawable(R.drawable.oval_stroke_preview);
            } else {
                drawable = getContext().getResources().getDrawable(R.drawable.oval_fill_preview);
            }
            drawable.mutate();
            drawable.setColorFilter(getAnnotStyle().getColor(), PorterDuff.Mode.SRC_IN);
        }
        mStrokePreview.setImageDrawable(drawable);

        // set fill preview drawable
        if (getAnnotStyle().getFillColor() != backgroundColor) {
            int fillDrawableRes = getAnnotStyle().getFillColor() == Color.TRANSPARENT ? R.drawable.oval_fill_transparent : R.drawable.oval_fill_preview;
            Drawable fillDrawable = getContext().getResources().getDrawable(fillDrawableRes);
            if (fillDrawableRes != R.drawable.oval_fill_transparent) {
                fillDrawable.mutate();
                fillDrawable.setColorFilter(getAnnotStyle().getFillColor(), PorterDuff.Mode.SRC_IN);
            }
            mFillPreview.setImageDrawable(fillDrawable);
        } else {
            GradientDrawable fillDrawable = (GradientDrawable) getContext().getResources().getDrawable(R.drawable.oval_stroke_preview);
            fillDrawable.mutate();
            fillDrawable.setStroke((int) Utils.convDp2Pix(getContext(), 1), Color.GRAY);
            mFillPreview.setImageDrawable(fillDrawable);
        }

        // set thickness
        if (getAnnotStyle().hasThickness()) {
            String annotThickness = String.format(getContext().getString(R.string.tools_misc_thickness), getAnnotStyle().getThickness());
            if (!mThicknessEditText.getText().toString().equals(annotThickness)) {
                mThicknessEditText.setText(annotThickness);
            }
            mTextJustChanged = true;
            mThicknessSeekbar.setProgress(Math.round((getAnnotStyle().getThickness() - mMinThickness) / (mMaxThickness - mMinThickness) * MAX_PROGRESS));
        }

        // set text size, text color,
        if (getAnnotStyle().hasTextStyle()) {
            String textSizeStr = getContext().getString(R.string.tools_misc_textsize, (int) getAnnotStyle().getTextSize());
            if (!mTextSizeEditText.getText().toString().equals(textSizeStr)) {
                mTextSizeEditText.setText(textSizeStr);
            }
            mTextJustChanged = true;
            mTextSizeSeekbar.setProgress(Math.round((getAnnotStyle().getTextSize() - mMinTextSize) / (mMaxTextSize - mMinTextSize) * MAX_PROGRESS));
            mTextColorPreview.updateFillPreview(Color.TRANSPARENT, Color.TRANSPARENT, 0, 1);
            mTextColorPreview.updateFreeTextStyle(getAnnotStyle().getTextColor(), 1);
        }

        // set font
        if (getAnnotStyle().hasFont()) {
            setFont(getAnnotStyle().getFont());
        }

        // set date format
        if (getAnnotStyle().isDateFreeText()) {
            String format = getAnnotStyle().getDateFormat();
            final CharSequence[] strings = getContext().getResources().getTextArray(R.array.style_picker_date_formats);
            for (int i = 0; i < strings.length; i++) {
                CharSequence s = strings[i];
                if (s.equals(format)) {
                    mDateFormatSpinner.setSelection(i);
                    break;
                }
            }
        }

        if (getAnnotStyle().hasBorderStyle()) {
            if (getAnnotStyle().getBorderStyle() != null) {
                mStrokeStylePreview.setImageResource(getAnnotStyle().getBorderStyle().getResource());
            }
        } else if (getAnnotStyle().hasLineStyle()) {
            if (getAnnotStyle().getLineStyle() != null) {
                mStrokeStylePreview.setImageResource(getAnnotStyle().getLineStyle().getResource());
            }
        }
        if (getAnnotStyle().hasLineStartStyle()) {
            if (getAnnotStyle().getLineStartStyle() != null) {
                mLineStartPreview.setImageResource(getAnnotStyle().getLineStartStyle().getResource());
            }
        }
        if (getAnnotStyle().hasLineEndStyle()) {
            if (getAnnotStyle().getLineEndStyle() != null) {
                mLineEndPreview.setImageResource(getAnnotStyle().getLineEndStyle().getResource());
            }
        }

        // set rich text enabled
        if (getAnnotStyle().isFreeText() && !getAnnotStyle().isCallout()) {
            mRCEnableSwitch.setChecked(getAnnotStyle().isRCFreeText());
        }

        // set opacity
        if (getAnnotStyle().hasOpacity()) {
            int progress = (int) (getAnnotStyle().getOpacity() * MAX_PROGRESS);
            mOpacityEditText.setText(String.valueOf(progress));
            mTextJustChanged = true;
            mOpacitySeekbar.setProgress(progress);
        }

        // set sticky note icon
        if (getAnnotStyle().hasIcon()) {
            if (!Utils.isNullOrEmpty(getAnnotStyle().getIcon())) {
                mAnnotStyleHolder.getAnnotPreview().setImageDrawable(getAnnotStyle().getIconDrawable(getContext()));
                if (mIconAdapter != null) {
                    mIconAdapter.setSelected(mIconAdapter.getItemIndex(getAnnotStyle().getIcon()));
                }
                mIconPreview.setImageDrawable(AnnotStyle.getIconDrawable(getContext(), getAnnotStyle().getIcon(), getAnnotStyle().getColor(), 1));
            }
            if (mIconAdapter != null) {
                mIconAdapter.updateIconColor(getAnnotStyle().getColor());
                mIconAdapter.updateIconOpacity(getAnnotStyle().getOpacity());
            }
        }

        // set ruler measures
        if (getAnnotStyle().isMeasurement()) {
            // snap
            mSnapSwitch.setChecked(getAnnotStyle().getSnap());
            // document value
            mRulerBaseEditText.setText(String.format(Locale.getDefault(), "%.1f", getAnnotStyle().getRulerBaseValue()));
            // document unit
            String documentUnit = getAnnotStyle().getRulerBaseUnit();
            int dIndex = mRulerBaseSpinnerAdapter.getPosition(documentUnit);
            if (dIndex >= 0) {
                mRulerBaseSpinner.setSelection(dIndex);
            }
            // world value
            mRulerTranslateEditText.setText(String.format(Locale.getDefault(), "%.1f", getAnnotStyle().getRulerTranslateValue()));
            // world unit
            String worldUnit = getAnnotStyle().getRulerTranslateUnit();
            int wIndex = mRulerTranslateSpinnerAdapter.getPosition(worldUnit);
            if (wIndex >= 0) {
                mRulerTranslateSpinner.setSelection(wIndex);
            }
            // precision
            boolean found = false;
            int precision = getAnnotStyle().getPrecision();
            HashMap<String, Integer> precisions = MeasureUtils.getPrecisions();
            for (Integer precisionValue : precisions.values()) {
                if (precisionValue == precision) {
                    found = true;
                    int pos = MeasureUtils.getPrecisionPosition(precisionValue);
                    if (mRulerPrecisionSpinnerAdapter.getCount() > pos) {
                        mRulerPrecisionSpinner.setSelection(pos);
                    }
                }
            }
            if (!found) {
                if (mRulerPrecisionSpinnerAdapter.getCount() > 2) {
                    mRulerPrecisionSpinner.setSelection(2); // default to 100 if possible
                }
            }
        }

        // set overlay text
        if (getAnnotStyle().isRedaction() || getAnnotStyle().isWatermark()) {
            mOverlayEditText.setText(getAnnotStyle().getOverlayText());
        }

        if (getAnnotStyle().isEraser()) {
            // Initialize eraser type state
            mEraserTypeSwitch.setChecked(getAnnotStyle().getEraserType().equals(Eraser.EraserType.INK_ERASER));

            // Initialize ink eraser mode state
            Eraser.InkEraserMode mode = getAnnotStyle().getInkEraserMode();
            final CharSequence[] strings = getContext().getResources().getTextArray(R.array.style_ink_eraser_mode);
            for (int i = 0; i < strings.length; i++) {
                CharSequence s = strings[i];
                if (s.equals(getContext().getResources().getString(mode.mLabelRes))) {
                    mInkEraserModeSpinner.setSelection(i);
                    break;
                }
            }
        }

        // set whether pressure sensitive
        if (getAnnotStyle().hasPressureSensitivity()) {
            mPressureSensitiveSwitch.setChecked(getAnnotStyle().getPressureSensitive());
        }

        // set text alignment
        if (getAnnotStyle().hasTextAlignment()) {
            int horizontalAlignment = getAnnotStyle().getHorizontalAlignment();
            int verticalAlignment = getAnnotStyle().getVerticalAlignment();

            mHorizontalTextLeft.setSelected(false);
            mHorizontalTextCenter.setSelected(false);
            mHorizontalTextRight.setSelected(false);
            switch (horizontalAlignment) {
                case Gravity.LEFT:
                    mHorizontalTextLeft.setSelected(true);
                    break;
                case Gravity.CENTER_HORIZONTAL:
                    mHorizontalTextCenter.setSelected(true);
                    break;
                case Gravity.RIGHT:
                    mHorizontalTextRight.setSelected(true);
                    break;
            }

            mVerticalTextTop.setSelected(false);
            mVerticalTextCenter.setSelected(false);
            mVerticalTextBottom.setSelected(false);
            switch (verticalAlignment) {
                case Gravity.TOP:
                    mVerticalTextTop.setSelected(true);
                    break;
                case Gravity.CENTER_VERTICAL:
                    mVerticalTextCenter.setSelected(true);
                    break;
                case Gravity.BOTTOM:
                    mVerticalTextBottom.setSelected(true);
                    break;
            }
        }
    }

    /**
     * Deselect all annotation presets preview button
     */
    public void deselectAllPresetsPreview() {
        // Check presets
        for (AnnotStyle preset : mPresetStyles) {
            if (preset != null) {
                ActionButton preview = preset.getBindedPreview();
                if (preview != null) {
                    preview.setSelected(false);
                }
            }
        }
    }

    private void setupFontSpinner() {
        // get supported languages
        ArrayList<FontResource> fonts = new ArrayList<>();
        FontResource loading = new FontResource(getContext().getString(R.string.free_text_fonts_loading), "", "", "");
        fonts.add(loading);
        // set font spinner
        mFontAdapter = new FontAdapter(getContext(), android.R.layout.simple_spinner_item, fonts);
        mFontAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mFontSpinner.setAdapter(mFontAdapter);
        mFontSpinner.setOnItemSelectedListener(this);

        Set<String> whiteListFonts = mWhiteListFonts;
        boolean isCustomFont = false;
        if (mFontListFromAsset != null && !mFontListFromAsset.isEmpty()) {
            whiteListFonts = mFontListFromAsset;
            isCustomFont = true;
        } else if (mFontListFromStorage != null && !mFontListFromStorage.isEmpty()) {
            whiteListFonts = mFontListFromStorage;
            isCustomFont = true;
        }

        LoadFontAsyncTask fontAsyncTask = new LoadFontAsyncTask(getContext(), whiteListFonts);
        fontAsyncTask.setIsCustomFont(isCustomFont);
        fontAsyncTask.setCallback(new LoadFontAsyncTask.Callback() {
            @Override
            public void onFinish(ArrayList<FontResource> fonts) {
                FontResource fontHint = new FontResource(getContext().getString(R.string.free_text_fonts_prompt), "", "", "");
                // add hint to fonts - "Pick Font" will display as the default
                // value in the spinner
                // the previous first font is "Loading fonts...", remove it
                fonts.add(0, fontHint);
                mFontAdapter.setData(fonts);
                if (getAnnotStyle() != null && getAnnotStyle().getFont() != null) {
                    setFontSpinner();
                }
                setPresetFonts(fonts);
            }
        });
        fontAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void setPresetFonts(ArrayList<FontResource> fonts) {
        for (AnnotStyle preset : mPresetStyles) {
            boolean fontFound = false;
            for (FontResource font : fonts) {
                if (preset.getFont().equals(font)) {
                    preset.setFont(font);
                    fontFound = true;
                    break;
                }
            }
            if (!fontFound && fonts.size() > 1) {
                // if no font found for presets, then set the first font to preset.(0th font is "Pick font")
                preset.setFont(fonts.get(1));
            }
        }
        checkPresets();
    }

    private void setFontSpinner() {
        if (mFontAdapter != null && mFontAdapter.getData() != null && mFontSpinner != null) {
            // check if font name matches a font
            boolean matchFound = false;
            if (getAnnotStyle().getFont().hasPDFTronName()) {
                // first try pdftron font name
                for (int i = 0; i < mFontAdapter.getData().size(); i++) {
                    if (mFontAdapter.getData().get(i).getPDFTronName().equals(getAnnotStyle().getFont().getPDFTronName())) {
                        mFontSpinner.setSelection(i);
                        matchFound = true;
                        break;
                    }
                }
            } else if (getAnnotStyle().getFont().hasFontName()) {
                // then try font name
                for (int i = 0; i < mFontAdapter.getData().size(); i++) {
                    if (mFontAdapter.getData().get(i).getFontName().equals(getAnnotStyle().getFont().getFontName())) {
                        mFontSpinner.setSelection(i);
                        matchFound = true;
                        break;
                    }
                }
            }

            if (!matchFound) {
                mFontSpinner.setSelection(0);
            } else {
                int index = mFontSpinner.getSelectedItemPosition();
                FontResource fontResource = mFontAdapter.getItem(index);
                if (fontResource != null && !Utils.isNullOrEmpty(fontResource.getFilePath())) {
                    mAnnotStyleHolder.getAnnotPreview().setFontPath(fontResource.getFilePath());
                }
            }
        }
    }

    private void setPreviewThickness() {
        setPreviewThickness(getAnnotStyle().getThickness());
    }

    private void setPreviewThickness(float thickness) {
        mAnnotStyleHolder.getAnnotPreview().updateFillPreview(getAnnotStyle().getColor(), getAnnotStyle().getFillColor(), thickness, getAnnotStyle().getOpacity());
    }

    private void setPreviewOpacity() {
        setPreviewOpacity(getAnnotStyle().getOpacity());
    }

    private void setPreviewOpacity(float opacity) {
        mAnnotStyleHolder.getAnnotPreview().updateFillPreview(getAnnotStyle().getColor(), getAnnotStyle().getFillColor(),
                getAnnotStyle().getThickness(), opacity);

        if (getAnnotStyle().isStickyNote() || getAnnotStyle().isCountMeasurement()) {
            mIconAdapter.updateIconOpacity(opacity);
        }
    }

    private void setPreviewTextSize() {
        setPreviewTextSize(getAnnotStyle().getTextSize());
    }

    private void setPreviewTextSize(float textSize) {
        mAnnotStyleHolder.getAnnotPreview().updateFreeTextStyle(getAnnotStyle().getTextColor(), textSize / mMaxTextSize);
    }

    private void updateUIVisibility() {
        boolean hasColor = true;
        boolean hasFillColor = true;
        boolean hasThickness = true;
        boolean hasBorderStyle = true;
        boolean hasLineStyle = true;
        boolean hasLineStartStyle = true;
        boolean hasLineEndStyle = true;
        boolean hasOpacity = true;
        boolean hasFont = true;
        boolean hasTextAlignment = true;
        boolean hasIcon = true;
        boolean hasTextSize = true;
        boolean hasTextColor = true;
        boolean hasRulerUnit = true;
        boolean hasRulerPrecision = true;
        boolean hasSnap = true;
        boolean hasRCContent = true;
        boolean hasTextOverlay = true;
        boolean hasPreset = true;
        boolean hasEraserType = true;
        boolean hasEraserMode = true;
        boolean hasDateFormat = true;
        boolean hasPressure = true;

        if (mGroupAnnotStyles != null) {
            mMoreToolLayout.setVisibility(GONE);

            for (AnnotStyle annotStyle : mGroupAnnotStyles) {
                Integer type = annotStyle.getAnnotType();
                AnnotStyleProperty annotStyleProperty = mAnnotStyleProperties != null ? mAnnotStyleProperties.get(type) : null;

                if (hasColor) {
                    hasColor = hasColor(annotStyle, annotStyleProperty);
                }
                if (hasFillColor) {
                    hasFillColor = hasFillColor(annotStyle, annotStyleProperty);
                }
                if (hasThickness) {
                    hasThickness = hasThickness(annotStyle, annotStyleProperty);
                }
                if (hasLineStyle) {
                    hasLineStyle = hasLineStyle(annotStyle, annotStyleProperty);
                }
                if (hasBorderStyle) {
                    hasBorderStyle = hasBorderStyle(annotStyle, annotStyleProperty);
                }
                if (hasLineStartStyle) {
                    hasLineStartStyle = hasLineStartStyle(annotStyle, annotStyleProperty);
                }
                if (hasLineEndStyle) {
                    hasLineEndStyle = hasLineEndStyle(annotStyle, annotStyleProperty);
                }
                if (hasOpacity) {
                    hasOpacity = hasOpacity(annotStyle, annotStyleProperty);
                }
                if (hasIcon) {
                    hasIcon = hasIcon(annotStyle, annotStyleProperty);
                }
                if (hasRulerUnit) {
                    hasRulerUnit = hasRulerUnit(annotStyle, annotStyleProperty);
                }
                if (hasRulerPrecision) {
                    hasRulerPrecision = hasRulerPrecision(annotStyle, annotStyleProperty);
                }
                if (hasSnap) {
                    hasSnap = hasSnap(annotStyle, annotStyleProperty);
                }
                if (hasTextOverlay) {
                    hasTextOverlay = hasTextOverlay(annotStyle, annotStyleProperty);
                }
                if (hasPreset) {
                    hasPreset = hasPreset(annotStyle, annotStyleProperty);
                }
                if (hasEraserType) {
                    hasEraserType = hasEraserType(annotStyle, annotStyleProperty);
                }
                if (hasEraserMode) {
                    hasEraserMode = hasEraserMode(annotStyle, annotStyleProperty);
                }
                if (hasPressure) {
                    hasPressure = hasPressure(annotStyle, annotStyleProperty);
                }
            }

            // text related properties are disabled in group select as they require custom rendering of appearance
            hasFont = false;
            hasTextSize = false;
            hasTextColor = false;
            hasRCContent = false;
            hasDateFormat = false;
        } else {
            if (mRCEnableSwitch.isChecked()) {
                mMoreToolLayout.setVisibility(GONE);
            } else {
                mMoreToolLayout.setVisibility(mMoreAnnotTypes == null || mMoreAnnotTypes.isEmpty() ? GONE : VISIBLE);
            }
            AnnotStyle annotStyle = getAnnotStyle();
            Integer type = annotStyle.getAnnotType();
            AnnotStyleProperty annotStyleProperty = mAnnotStyleProperties != null ? mAnnotStyleProperties.get(type) : null;

            hasColor = hasColor(annotStyle, annotStyleProperty);
            hasFillColor = hasFillColor(annotStyle, annotStyleProperty);
            hasThickness = hasThickness(annotStyle, annotStyleProperty);
            hasBorderStyle = hasBorderStyle(annotStyle, annotStyleProperty);
            hasLineStyle = hasLineStyle(annotStyle, annotStyleProperty);
            hasLineStartStyle = hasLineStartStyle(annotStyle, annotStyleProperty);
            hasLineEndStyle = hasLineEndStyle(annotStyle, annotStyleProperty);
            hasOpacity = hasOpacity(annotStyle, annotStyleProperty);
            hasFont = hasFont(annotStyle, annotStyleProperty);
            hasTextAlignment = hasTextAlignment(annotStyle, annotStyleProperty);
            hasIcon = hasIcon(annotStyle, annotStyleProperty);
            hasTextSize = hasTextSize(annotStyle, annotStyleProperty);
            hasTextColor = hasTextColor(annotStyle, annotStyleProperty);
            hasRulerUnit = hasRulerUnit(annotStyle, annotStyleProperty);
            hasRulerPrecision = hasRulerPrecision(annotStyle, annotStyleProperty);
            hasSnap = hasSnap(annotStyle, annotStyleProperty);
            hasRCContent = hasRCContent(annotStyle, annotStyleProperty);
            hasTextOverlay = hasTextOverlay(annotStyle, annotStyleProperty);
            hasPreset = hasPreset(annotStyle, annotStyleProperty);
            hasEraserType = hasEraserType(annotStyle, annotStyleProperty);
            hasEraserMode = hasEraserMode(annotStyle, annotStyleProperty);
            hasDateFormat = hasDateFormat(annotStyle, annotStyleProperty);
            hasPressure = hasPressure(annotStyle, annotStyleProperty);
        }

        mStrokeLayout.setVisibility(hasColor ? VISIBLE : GONE);
        mFillLayout.setVisibility(hasFillColor ? VISIBLE : GONE);
        mThicknessLayout.setVisibility(hasThickness ? VISIBLE : GONE);
        mStrokeStyleLayout.setVisibility(hasLineStyle || hasBorderStyle ? VISIBLE : GONE);
        mLineStartLayout.setVisibility(hasLineStartStyle ? VISIBLE : GONE);
        mLineEndLayout.setVisibility(hasLineEndStyle ? VISIBLE : GONE);
        mOpacityLayout.setVisibility(hasOpacity ? VISIBLE : GONE);
        mFontLayout.setVisibility(hasFont ? VISIBLE : GONE);
        if (hasTextAlignment && mCanShowTextAlignment) {
            mHorizontalTextAlignment.setVisibility(VISIBLE);
            mVerticalTextAlignment.setVisibility(VISIBLE);
        } else {
            mHorizontalTextAlignment.setVisibility(GONE);
            mVerticalTextAlignment.setVisibility(GONE);
        }
        mIconLayout.setVisibility(hasIcon ? VISIBLE : GONE);
        if (mIconExpanded) {
            mIconExpandableGridView.setVisibility(hasIcon ? VISIBLE : GONE);
        }
        mTextSizeLayout.setVisibility(hasTextSize ? VISIBLE : GONE);
        mTextColorLayout.setVisibility(hasTextColor ? VISIBLE : GONE);
        mRulerUnitLayout.setVisibility(hasRulerUnit ? VISIBLE : GONE);
        mRulerPrecisionLayout.setVisibility(hasRulerPrecision ? VISIBLE : GONE);
        mSnapLayout.setVisibility(hasSnap ? VISIBLE : GONE);
        if (mCanShowRCOption) {
            mRCEnableLayout.setVisibility(hasRCContent ? VISIBLE : GONE);
        } else {
            mRCEnableLayout.setVisibility(GONE);
        }
        mTextOverlayLayout.setVisibility(hasTextOverlay ? VISIBLE : GONE);
        mPresetContainer.setVisibility(hasPreset && mShowPresets ? VISIBLE : GONE);
        mEraserTypeLayout.setVisibility(hasEraserType ? VISIBLE : GONE);
        mInkEraserModeLayout.setVisibility(hasEraserMode ? VISIBLE : GONE);
        mDateFormatLayout.setVisibility(hasDateFormat ? VISIBLE : GONE);
        if (mCanShowPressureOption) {
            mPressureSensitiveLayout.setVisibility(hasPressure ? VISIBLE : GONE);
        }
    }

    private static boolean hasColor(@NonNull AnnotStyle annotStyle, @Nullable AnnotStyleProperty annotStyleProperty) {
        return annotStyle.hasColor() && (annotStyleProperty == null || annotStyleProperty.canShowStrokeColor());
    }

    private static boolean hasFillColor(@NonNull AnnotStyle annotStyle, @Nullable AnnotStyleProperty annotStyleProperty) {
        return annotStyle.hasFillColor() && (annotStyleProperty == null || annotStyleProperty.canShowFillColor());
    }

    private static boolean hasThickness(@NonNull AnnotStyle annotStyle, @Nullable AnnotStyleProperty annotStyleProperty) {
        return annotStyle.hasThickness() && (annotStyleProperty == null || annotStyleProperty.canShowThickness());
    }

    private static boolean hasBorderStyle(@NonNull AnnotStyle annotStyle, @Nullable AnnotStyleProperty annotStyleProperty) {
        return annotStyle.hasBorderStyle() && (annotStyleProperty == null || annotStyleProperty.canShowBorderStyle());
    }

    private static boolean hasLineStyle(@NonNull AnnotStyle annotStyle, @Nullable AnnotStyleProperty annotStyleProperty) {
        return annotStyle.hasLineStyle() && (annotStyleProperty == null || annotStyleProperty.canShowLineStyle());
    }

    private static boolean hasLineStartStyle(@NonNull AnnotStyle annotStyle, @Nullable AnnotStyleProperty annotStyleProperty) {
        return annotStyle.hasLineStartStyle() && (annotStyleProperty == null || annotStyleProperty.canShowLineStartStyle());
    }

    private static boolean hasLineEndStyle(@NonNull AnnotStyle annotStyle, @Nullable AnnotStyleProperty annotStyleProperty) {
        return annotStyle.hasLineEndStyle() && (annotStyleProperty == null || annotStyleProperty.canShowLineEndStyle());
    }

    private static boolean hasOpacity(@NonNull AnnotStyle annotStyle, @Nullable AnnotStyleProperty annotStyleProperty) {
        return annotStyle.hasOpacity() && (annotStyleProperty == null || annotStyleProperty.canShowOpacity());
    }

    private static boolean hasFont(@NonNull AnnotStyle annotStyle, @Nullable AnnotStyleProperty annotStyleProperty) {
        return annotStyle.hasFont() && (annotStyleProperty == null || annotStyleProperty.canShowFont());
    }

    private static boolean hasTextAlignment(@NonNull AnnotStyle annotStyle, @Nullable AnnotStyleProperty annotStyleProperty) {
        return annotStyle.hasTextAlignment() && (annotStyleProperty == null || annotStyleProperty.canShowTextAlignment());
    }

    private static boolean hasIcon(@NonNull AnnotStyle annotStyle, @Nullable AnnotStyleProperty annotStyleProperty) {
        return annotStyle.isStickyNote() && (annotStyleProperty == null || annotStyleProperty.canShowIcons());
    }

    private static boolean hasTextSize(@NonNull AnnotStyle annotStyle, @Nullable AnnotStyleProperty annotStyleProperty) {
        return annotStyle.hasTextStyle() && (annotStyleProperty == null || annotStyleProperty.canShowTextSize());
    }

    private static boolean hasTextColor(@NonNull AnnotStyle annotStyle, @Nullable AnnotStyleProperty annotStyleProperty) {
        return annotStyle.hasTextStyle() && (annotStyleProperty == null || annotStyleProperty.canShowTextColor());
    }

    private static boolean hasRulerUnit(@NonNull AnnotStyle annotStyle, @Nullable AnnotStyleProperty annotStyleProperty) {
        return (annotStyle.isMeasurement() && !annotStyle.isCountMeasurement()) && (annotStyleProperty == null || annotStyleProperty.canShowRulerUnit());
    }

    private static boolean hasRulerPrecision(@NonNull AnnotStyle annotStyle, @Nullable AnnotStyleProperty annotStyleProperty) {
        return (annotStyle.isMeasurement() && !annotStyle.isCountMeasurement()) && (annotStyleProperty == null || annotStyleProperty.canShowRulerPrecision());
    }

    private static boolean hasSnap(@NonNull AnnotStyle annotStyle, @Nullable AnnotStyleProperty annotStyleProperty) {
        return (annotStyle.isMeasurement() && !annotStyle.isCountMeasurement()) && (annotStyleProperty == null || annotStyleProperty.canShowSnap());
    }

    private static boolean hasRCContent(@NonNull AnnotStyle annotStyle, @Nullable AnnotStyleProperty annotStyleProperty) {
        return annotStyle.isFreeText() && !annotStyle.isCallout() && (annotStyleProperty == null || annotStyleProperty.canShowRichContent());
    }

    private static boolean hasTextOverlay(@NonNull AnnotStyle annotStyle, @Nullable AnnotStyleProperty annotStyleProperty) {
        return annotStyle.isRedaction() || annotStyle.isWatermark() && (annotStyleProperty == null || annotStyleProperty.canShowTextOverlay());
    }

    private static boolean hasPreset(@NonNull AnnotStyle annotStyle, @Nullable AnnotStyleProperty annotStyleProperty) {
        return !annotStyle.isWatermark() && (annotStyleProperty == null || annotStyleProperty.canShowPreset());
    }

    private static boolean hasEraserType(@NonNull AnnotStyle annotStyle, @Nullable AnnotStyleProperty annotStyleProperty) {
        return annotStyle.isEraser() && (annotStyleProperty == null || annotStyleProperty.canShowEraserType());
    }

    private static boolean hasEraserMode(@NonNull AnnotStyle annotStyle, @Nullable AnnotStyleProperty annotStyleProperty) {
        return annotStyle.isEraser() && (annotStyleProperty == null || annotStyleProperty.canShowEraserMode());
    }

    private static boolean hasDateFormat(@NonNull AnnotStyle annotStyle, @Nullable AnnotStyleProperty annotStyleProperty) {
        return annotStyle.isDateFreeText() && (annotStyleProperty == null || annotStyleProperty.canShowDateFormat());
    }

    private static boolean hasPressure(@NonNull AnnotStyle annotStyle, @Nullable AnnotStyleProperty annotStyleProperty) {
        return annotStyle.hasPressureSensitivity() && (annotStyleProperty == null || annotStyleProperty.canShowPressure());
    }

    /**
     * Sets color layout clicked listener, color layout includes {stroke color layout, fill color layout, and text color layout}
     *
     * @param listener The color layout clicked listener
     */
    public void setOnColorLayoutClickedListener(OnColorLayoutClickedListener listener) {
        mColorLayoutClickedListener = listener;
    }

    /**
     * Sets style layout clicked listener, style layout includes {stroke style layout, line style layout, line staring style layout and line ending style layout}
     *
     * @param listener The style layout clicked listener
     */
    public void setOnStyleLayoutClickedListener(OnStyleLayoutClickedListener listener) {
        mStyleLayoutClickedListener = listener;
    }

    /**
     * hide keyboard and remove edit text focus when EditorInfo.IME_ACTION_DONE is clicked
     *
     * @param v        text view
     * @param actionId action id
     * @param event    key event
     * @return Return true if you have consumed the action, else false.
     */
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            Utils.hideSoftKeyboard(getContext(), v);
            v.clearFocus();
            if (v.getId() == mOverlayEditText.getId()) {
                Editable s = mOverlayEditText.getText();
                getAnnotStyle().setOverlayText(s.toString());
            } else {
                mAnnotStyleHolder.getAnnotPreview().requestFocus();
            }
            return true;
        }
        return false;
    }

    /**
     * When slider progress changes, set corresponding value to annotation style
     *
     * @param seekBar  seek bar
     * @param progress progress value
     * @param fromUser True if the progress change was initiated by the user.
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (mTextJustChanged) {
            mTextJustChanged = false;
            return;
        }
        if (seekBar.getId() == mThicknessSeekbar.getId()) {
            float thickness = (mMaxThickness - mMinThickness) * progress / MAX_PROGRESS + mMinThickness;
            getAnnotStyle().setThickness(thickness, false);
            mThicknessEditText.setText(String.format(getContext().getString(R.string.tools_misc_thickness), thickness));
            // change preview based on thickness
            setPreviewThickness(thickness);
        } else if (seekBar.getId() == mOpacitySeekbar.getId()) {
            float opacity = (float) progress / MAX_PROGRESS;
            getAnnotStyle().setOpacity(opacity, false);
            mOpacityEditText.setText(String.valueOf(progress));
            // change preview based on thickness
            setPreviewOpacity(opacity);
        } else if (seekBar.getId() == mTextSizeSeekbar.getId()) {
            int textSize = Math.round((mMaxTextSize - mMinTextSize) * progress / MAX_PROGRESS + mMinTextSize);
            getAnnotStyle().setTextSize(textSize, false);
            mTextSizeEditText.setText(getContext().getString(R.string.tools_misc_textsize, textSize));
            setPreviewTextSize(textSize);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
//        getAnnotStyle().disableUpdateListener(true);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int progress = seekBar.getProgress();
        if (seekBar.getId() == mThicknessSeekbar.getId()) {
            float thickness = (mMaxThickness - mMinThickness) * progress / MAX_PROGRESS + mMinThickness;
            getAnnotStyle().setThickness(thickness);
            mThicknessEditText.setText(String.format(getContext().getString(R.string.tools_misc_thickness), thickness));
            // change preview based on thickness
            setPreviewThickness();

            AnalyticsAnnotStylePicker.getInstance().selectThickness(thickness);
        } else if (seekBar.getId() == mOpacitySeekbar.getId()) {
            getAnnotStyle().setOpacity((float) progress / MAX_PROGRESS);
            mOpacityEditText.setText(String.valueOf(progress));
            // change preview based on thickness
            setPreviewOpacity();

            AnalyticsAnnotStylePicker.getInstance().selectOpacity(getAnnotStyle().getOpacity());
        } else if (seekBar.getId() == mTextSizeSeekbar.getId()) {
            int textSize = Math.round((mMaxTextSize - mMinTextSize) * progress / MAX_PROGRESS + mMinTextSize);
            getAnnotStyle().setTextSize(textSize);
            mTextSizeEditText.setText(getContext().getString(R.string.tools_misc_textsize, textSize));
            setPreviewTextSize();

            AnalyticsAnnotStylePicker.getInstance().selectTextSize(textSize);
        }
    }

    /**
     * Callback method when there is a focus change
     * in mThicknessEditText and #mOpacityEditText,
     * When focus removed, set corresponding thickness or opacity
     *
     * @param v        focus changed view
     * @param hasFocus whether the view has focus
     */
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        mTextJustChanged = true;
        if (v.getId() == mThicknessEditText.getId()) {
            if (!hasFocus && mPrevThicknessFocus) {
                Editable s = mThicknessEditText.getText();
                String number = s.toString();
                try {
                    float value = Utils.parseFloat(number);
                    if (value > getAnnotStyle().getMaxInternalThickness()) {
                        value = getAnnotStyle().getMaxInternalThickness();
                        mThicknessEditText.setText(getContext().getString(R.string.tools_misc_thickness, value));
                    }
                    getAnnotStyle().setThickness(value);
                    int progress = Math.round(getAnnotStyle().getThickness() / (mMaxThickness - mMinThickness) * MAX_PROGRESS);
                    mThicknessSeekbar.setProgress(progress);
                    setPreviewThickness();
                    AnalyticsAnnotStylePicker.getInstance().selectThickness(value);
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e, "annot style invalid number");
                    CommonToast.showText(getContext(), R.string.invalid_number);
                }
            }
            mPrevThicknessFocus = hasFocus;
        } else if (v.getId() == mOpacityEditText.getId()) {
            if (!hasFocus && mPrevOpacityFocus) {
                Editable s = mOpacityEditText.getText();
                String number = s.toString();
                try {
                    float value = Utils.parseFloat(number);
                    if (value > MAX_PROGRESS) {
                        value = MAX_PROGRESS;
                        mOpacityEditText.setText(String.valueOf(value));
                    }
                    getAnnotStyle().setOpacity(value / MAX_PROGRESS);
                    mOpacitySeekbar.setProgress((int) value);
                    setPreviewOpacity();
                    AnalyticsAnnotStylePicker.getInstance().selectThickness(getAnnotStyle().getOpacity());
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e, "annot style invalid number");
                    CommonToast.showText(getContext(), R.string.invalid_number);
                }
            }
            mPrevOpacityFocus = hasFocus;
        } else if (v.getId() == mTextSizeEditText.getId() && !hasFocus) {
            Editable s = mTextSizeEditText.getText();
            String number = s.toString();
            try {
                float value = Utils.parseFloat(number);
                value = Math.round(value);
                getAnnotStyle().setTextSize(value);
                int progress = Math.round(getAnnotStyle().getTextSize() / (mMaxTextSize - mMinTextSize) * MAX_PROGRESS);
                mTextSizeSeekbar.setProgress(progress);
                setPreviewTextSize();
                AnalyticsAnnotStylePicker.getInstance().selectThickness(value);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e, "annot style invalid number");
                CommonToast.showText(getContext(), R.string.invalid_number);
            }
        } else if (v.getId() == mRulerBaseEditText.getId() && !hasFocus) {
            Editable s = mRulerBaseEditText.getText();
            String number = s.toString();
            try {
                float value = Utils.parseFloat(number);
                if (value < 0.1) {
                    value = 0.1f;
                    mRulerBaseEditText.setText(String.format(Locale.getDefault(), "%.1f", 0.1));
                }
                getAnnotStyle().setRulerBaseValue(value);
                AnalyticsAnnotStylePicker.getInstance().selectRulerBaseValue(value);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e, "annot style invalid number");
                CommonToast.showText(getContext(), R.string.invalid_number);
            }
        } else if (v.getId() == mRulerTranslateEditText.getId() && !hasFocus) {
            Editable s = mRulerTranslateEditText.getText();
            String number = s.toString();
            try {
                float value = Utils.parseFloat(number);
                if (value < 0.1) {
                    value = 0.1f;
                    mRulerTranslateEditText.setText(String.format(Locale.getDefault(), "%.1f", 0.1));
                }
                getAnnotStyle().setRulerTranslateValue(value);
                AnalyticsAnnotStylePicker.getInstance().selectRulerTranslateValue(value);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e, "annot style invalid number");
                CommonToast.showText(getContext(), R.string.invalid_number);
            }
        } else if (v.getId() == mOverlayEditText.getId() && !hasFocus) {
            Editable s = mOverlayEditText.getText();
            getAnnotStyle().setOverlayText(s.toString());
        }
        if (!hasFocus) {
            Utils.hideSoftKeyboard(getContext(), v);
        }
    }

    /**
     * Callback method invoked when child view clicked
     *
     * @param v clicked view
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == mThicknessValueGroup.getId()) {
            Utils.showSoftKeyboard(getContext(), mThicknessEditText);
            mThicknessEditText.requestFocus();
        } else if (v.getId() == mOpacityValueGroup.getId()) {
            Utils.showSoftKeyboard(getContext(), mOpacityTextView);
            mOpacityEditText.requestFocus();
        } else if (v.getId() == mIconLayout.getId()) {
            boolean isGridViewVisible = mIconExpandableGridView.getVisibility() == VISIBLE;
            mIconExpandableGridView.setVisibility(isGridViewVisible ? GONE : VISIBLE);
            mIconExpandableBtn.setImageResource(isGridViewVisible ?
                    R.drawable.ic_chevron_right_black_24dp : R.drawable.ic_arrow_down_white_24dp);
            mIconExpanded = mIconExpandableGridView.getVisibility() == VISIBLE;
        } else if (v.getId() == mStrokeStyleLayout.getId()) {
            if (getAnnotStyle().hasBorderStyle()) {
                mStyleLayoutClickedListener.onStyleLayoutClicked(AnnotStyleDialogFragment.BORDER_STYLE);
            } else {
                mStyleLayoutClickedListener.onStyleLayoutClicked(AnnotStyleDialogFragment.LINE_STYLE);
            }
        } else if (v.getId() == mLineStartLayout.getId()) {
            mStyleLayoutClickedListener.onStyleLayoutClicked(AnnotStyleDialogFragment.LINE_START_STYLE);
        } else if (v.getId() == mLineEndLayout.getId()) {
            mStyleLayoutClickedListener.onStyleLayoutClicked(AnnotStyleDialogFragment.LINE_END_STYLE);
        } else if (v.getId() == mStrokeLayout.getId() && mColorLayoutClickedListener != null) {
            int colorMode = getAnnotStyle().hasFillColor() ? AnnotStyleDialogFragment.STROKE_COLOR : AnnotStyleDialogFragment.COLOR;
            mColorLayoutClickedListener.onColorLayoutClicked(colorMode);
        } else if (v.getId() == mTextColorLayout.getId() && mColorLayoutClickedListener != null) {
            mColorLayoutClickedListener.onColorLayoutClicked(AnnotStyleDialogFragment.TEXT_COLOR);
        } else if (v.getId() == mFillLayout.getId() && mColorLayoutClickedListener != null) {
            mColorLayoutClickedListener.onColorLayoutClicked(AnnotStyleDialogFragment.FILL_COLOR);
        } else if (v.getId() == mSnapLayout.getId()) {
            mSnapSwitch.toggle();
            getAnnotStyle().setSnap(mSnapSwitch.isChecked());
        } else if (v.getId() == mRCEnableLayout.getId()) {
            mRCEnableSwitch.toggle();
            if (mRCEnableSwitch.isChecked()) {
                getAnnotStyle().setTextHTMLContent("rc");
            } else {
                getAnnotStyle().setTextHTMLContent("");
            }
            updateLayout();
            AnalyticsAnnotStylePicker.getInstance().setRichTextEnabled(mRCEnableSwitch.isChecked());
        } else if (v.getId() == mEraserTypeLayout.getId()) {
            mEraserTypeSwitch.toggle();
            getAnnotStyle().setEraserType(mEraserTypeSwitch.isChecked() ? Eraser.EraserType.INK_ERASER : Eraser.EraserType.HYBRID_ERASER);
            AnalyticsAnnotStylePicker.getInstance().setEraseInkOnlyEnabled(mEraserTypeSwitch.isChecked());
        } else if (v.getId() == mPressureSensitiveLayout.getId()) {
            mPressureSensitiveSwitch.toggle();
            getAnnotStyle().setPressureSensitivity(mPressureSensitiveSwitch.isChecked());
            AnalyticsAnnotStylePicker.getInstance().setPressureSensitiveEnabled(mPressureSensitiveSwitch.isChecked());
        } else {
            for (int i = 0; i < PRESET_SIZE; i++) {
                ActionButton presetView = mPresetViews[i];
                AnnotStyle presetStyle = mPresetStyles[i];
                if (v.getId() == presetView.getId() && mPresetSelectedListener != null) {
                    if (v.isSelected()) {
                        mPresetSelectedListener.onPresetDeselected(presetStyle);
                        AnalyticsAnnotStylePicker.getInstance().deselectPreset(i);
                    } else {
                        mPresetSelectedListener.onPresetSelected(presetStyle);
                        AnalyticsAnnotStylePicker.getInstance().selectPreset(i, isAnnotStyleInDefaults(presetStyle));
                        break;
                    }
                }
            }
        }
    }

    private boolean isAnnotStyleInDefaults(AnnotStyle annotStyle) {
        for (int i = 0; i < PRESET_SIZE; i++) {
            AnnotStyle defaultStyle = ToolStyleConfig.getInstance().getDefaultAnnotPresetStyle(
                    getContext(), mAnnotType, i, ToolStyleConfig.getInstance().getPresetsAttr(mAnnotType),
                    ToolStyleConfig.getInstance().getDefaultPresetsArrayRes(mAnnotType));
            if (defaultStyle.equals(annotStyle)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Update layout
     */
    public void updateLayout() {
        if (getAnnotStyle().isFreeText()) {
            mFillColorTextView.setText(R.string.pref_colormode_custom_bg_color);
        } else if (!getAnnotStyle().hasFillColor()) {
            mStrokeColorTextView.setText(R.string.tools_qm_color);
        } else {
            mStrokeColorTextView.setText(R.string.tools_qm_stroke_color);
        }
        updateUIVisibility();
        initLayoutStyle();
        mAnnotStyleHolder.onAnnotStyleLayoutUpdated();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == View.VISIBLE) {
            updateLayout();
        }
    }

    /**
     * Save annotation presets to settings
     */
    public void savePresets() {
        for (int i = 0; i < PRESET_SIZE; i++) {
            AnnotStyle preset = mPresetStyles[i];
            PdfViewCtrlSettingsManager.setAnnotStylePreset(getContext(), mAnnotType, i, preset.toJSONString());
        }
    }

    /**
     * Gets color based on color mode
     *
     * @param colorMode color mode, must be one of {@link AnnotStyleDialogFragment#STROKE_COLOR}
     *                  or {@link AnnotStyleDialogFragment#FILL_COLOR}
     * @return color
     */
    @SuppressLint("SwitchIntDef")
    public @ColorInt
    int getColor(@AnnotStyleDialogFragment.SelectColorMode int colorMode) {
        switch (colorMode) {
            case AnnotStyleDialogFragment.FILL_COLOR:
                return getAnnotStyle().getFillColor();
            default:
                return getAnnotStyle().getColor();
        }
    }

    /**
     * Called when icon grid item clicked
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this
     *                 will be a view provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String item = mIconAdapter.getItem(position);
        mIconAdapter.setSelected(position);
        setIcon(item);
    }

    /**
     * Called when selected one item from font spinner
     *
     * @param parent   Font spinner
     * @param view     selected view
     * @param position the position of the view in the adapter
     * @param id       the row id of the item that was selected
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == mFontSpinner.getId()) {
            if (position >= 0 && mFontAdapter != null) {
                FontResource font = mFontAdapter.getItem(position);
                if (font != null && !mInitSpinner) {
                    setFont(font);
                } else if (mInitSpinner) {
                    mInitSpinner = false;
                }
            }
        } else if (parent.getId() == mRulerBaseSpinner.getId()) {
            if (position >= 0 && mRulerBaseSpinnerAdapter != null) {
                CharSequence unit = mRulerBaseSpinnerAdapter.getItem(position);
                if (unit != null) {
                    getAnnotStyle().setRulerBaseUnit(unit.toString());
                }
            }
        } else if (parent.getId() == mRulerTranslateSpinner.getId()) {
            if (position >= 0 && mRulerTranslateSpinnerAdapter != null) {
                CharSequence unit = mRulerTranslateSpinnerAdapter.getItem(position);
                if (unit != null) {
                    getAnnotStyle().setRulerTranslateUnit(unit.toString());
                }
            }
        } else if (parent.getId() == mRulerPrecisionSpinner.getId()) {
            if (position >= 0) {
                getAnnotStyle().setRulerPrecision(MeasureUtils.getPrecision(position));
            }
        } else if (parent.getId() == mDateFormatSpinner.getId()) {
            if (position >= 0 && mDateFormatSpinnerAdapter != null) {
                final CharSequence[] strings = getContext().getResources().getTextArray(R.array.style_picker_date_formats);
                CharSequence format = strings[position];
                if (format != null) {
                    getAnnotStyle().setDateFormat(format.toString());
                }
            }
        } else if (parent.getId() == mInkEraserModeSpinner.getId()) {
            if (position >= 0 && mInkEraserModeAdapter != null) {
                final CharSequence[] strings = getContext().getResources().getTextArray(R.array.style_ink_eraser_mode);
                Eraser.InkEraserMode mode = Eraser.InkEraserMode.fromLabel(getContext(), strings[position].toString());
                if (mode != null) {
                    getAnnotStyle().setInkEraserMode(mode);
                }
            }
        }
    }

    /**
     * Callback method to be invoked when the selection disappears from font spinner
     *
     * @param parent The AdapterView that now contains no selected item.
     */
    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    /**
     * Sets on annotation preset selected listener
     *
     * @param listener annotation preset selected listener
     */
    public void setOnPresetSelectedListener(OnPresetSelectedListener listener) {
        mPresetSelectedListener = listener;
    }

    /**
     * Sets a listener to listen more annot type click event
     *
     * @param listener The listener
     */
    public void setOnMoreAnnotTypesClickListener(OnMoreAnnotTypeClickedListener listener) {
        mMoreAnnotTypeListener = listener;
    }

    public void setOnDismissListener(OnDialogDismissListener listener) {
        mOnDismissListener = listener;
    }

    private ActionButton getAnnotTypeButtonForTool(int annotType) {
        Context context = getContext();
        ActionButton actionButton = new ActionButton(context);
        actionButton.setCheckable(true);
        actionButton.setIcon(context.getResources().getDrawable(AnnotUtils.getAnnotImageResId(annotType)));
        actionButton.setIconColor(mTheme.iconColor);
        actionButton.setSelectedIconColor(mTheme.iconColor);
        actionButton.setSelectedBackgroundColor(mTheme.selectedBackgroundColor);
        actionButton.setAlpha(0.54f);
        actionButton.setShowIconHighlightColor(false);
        actionButton.setAlwaysShowIconHighlightColor(false);
        String text = AnnotUtils.getAnnotTypeAsString(getContext(), annotType);
        TooltipCompat.setTooltipText(actionButton, text);
        actionButton.setContentDescription(text);
        actionButton.setLayoutParams(
                new RelativeLayout.LayoutParams(
                        getContext().getResources().getDimensionPixelSize(R.dimen.quick_menu_button_size),
                        getContext().getResources().getDimensionPixelSize(R.dimen.quick_menu_button_size)));
        if (annotType == getAnnotStyle().getAnnotType()) {
            actionButton.setSelected(true);
        }
        return actionButton;
    }

    /**
     * Sets whether to show preset list. Default true
     *
     * @param showPresets True if preset list is shown
     */
    public void setShowPreset(boolean showPresets) {
        this.mShowPresets = showPresets;
    }

    /**
     * Sets the AnnotStyleProperties that will be used to hide elements of the AnnotStyleDialog.
     *
     * @param annotStyleProperties hash map of annot types and the AnnotStyleProperties
     */
    public void setAnnotStyleProperties(@Nullable HashMap<Integer, AnnotStyleProperty> annotStyleProperties) {
        mAnnotStyleProperties = annotStyleProperties;
    }

    public void setGroupAnnotStyles(@Nullable ArrayList<AnnotStyle> annotStyles) {
        mGroupAnnotStyles = annotStyles;
    }

    public void setCanShowTextAlignment(boolean canShowTextAlignment) {
        mCanShowTextAlignment = canShowTextAlignment;
    }

    /**
     * This interface is for switching between color picker and style picker
     */
    public interface OnColorLayoutClickedListener {
        /**
         * This method is invoked when clicked on stroke color layout or fill color layout
         *
         * @param colorMode clicked color layout, has to be one of
         *                  {@link AnnotStyleDialogFragment#STROKE_COLOR}
         *                  or {@link AnnotStyleDialogFragment#FILL_COLOR}
         */
        void onColorLayoutClicked(@AnnotStyleDialogFragment.SelectColorMode int colorMode);
    }

    /**
     * This interface is for switching between icon picker and style picker
     */
    public interface OnStyleLayoutClickedListener {
        /**
         * This method is invoked when clicked on lineStyle, borderStyle, LineStartStyle, LineEndStyle layout
         *
         * @param styleMode clicked style layout, has to be one of
         *                  {@link AnnotStyleDialogFragment#BORDER_STYLE}
         *                  or {@link AnnotStyleDialogFragment#LINE_STYLE}
         *                  or {@link AnnotStyleDialogFragment#LINE_START_STYLE}
         *                  or {@link AnnotStyleDialogFragment#LINE_END_STYLE}
         */
        void onStyleLayoutClicked(@AnnotStyleDialogFragment.SelectStyleMode int styleMode);
    }

    /**
     * This interface is for listening preset style buttons pressed event
     */
    public interface OnPresetSelectedListener {
        /**
         * This method is invoked when preset button is selected
         *
         * @param presetStyle presetStyle
         */
        void onPresetSelected(AnnotStyle presetStyle);

        /**
         * This method is invoked when preset button is de-selected
         *
         * @param presetStyle presetStyle
         */
        void onPresetDeselected(AnnotStyle presetStyle);
    }

    public interface OnMoreAnnotTypeClickedListener {
        void onAnnotTypeClicked(int annotType);
    }

    private static class RulerPrecisionAdapter extends ArrayAdapter<CharSequence> {
        public RulerPrecisionAdapter(@NonNull Context context, List<CharSequence> items) {
            super(context, android.R.layout.simple_spinner_item, 0, items);
        }

        @NonNull
        public static List<CharSequence> toLocalized(@NonNull Context context) {
            final CharSequence[] rulerPrecisionStrings = context.getResources().getTextArray(R.array.ruler_precision);
            ArrayList<CharSequence> localizedArray = new ArrayList<>(rulerPrecisionStrings.length);
            for (int i = 0; i < rulerPrecisionStrings.length; i++) {
                try {
                    float usFloat = Utils.parseFloat(rulerPrecisionStrings[i].toString(), Locale.US);
                    String format = "%." + i + "f";
                    String localized = String.format(Locale.getDefault(), format, usFloat);
                    localizedArray.add(localized);
                } catch (Exception ignored) {
                }
            }
            return localizedArray;
        }
    }
}
