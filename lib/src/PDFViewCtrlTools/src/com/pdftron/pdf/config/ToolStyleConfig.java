//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.SparseIntArray;
import android.view.Gravity;
import androidx.annotation.ArrayRes;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.model.LineEndingStyle;
import com.pdftron.pdf.model.LineStyle;
import com.pdftron.pdf.model.ShapeBorderStyle;
import com.pdftron.pdf.tools.Eraser;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.RubberStampCreate;
import com.pdftron.pdf.tools.SoundCreate;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.pdf.utils.MeasureUtils;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.StampManager;
import com.pdftron.pdf.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

import static com.pdftron.pdf.utils.PdfViewCtrlSettingsManager.getDefaultSharedPreferences;

/**
 * A helper class for configuring style of annotation creator tools.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ToolStyleConfig {
    private static ToolStyleConfig _INSTANCE;

    /**
     * Custom annotation properties for SharedPreference
     */
    private static final String PREF_ANNOTATION_PROPERTY_LINE = "annotation_property_shape"; // line
    private static final String PREF_ANNOTATION_PROPERTY_ARROW = "annotation_property_arrow";
    private static final String PREF_ANNOTATION_PROPERTY_RULER = "annotation_property_ruler";
    private static final String PREF_ANNOTATION_PROPERTY_POLYLINE = "annotation_property_polyline";
    private static final String PREF_ANNOTATION_PROPERTY_RECTANGLE = "annotation_property_rectangle";
    private static final String PREF_ANNOTATION_PROPERTY_OVAL = "annotation_property_oval";
    private static final String PREF_ANNOTATION_PROPERTY_POLYGON = "annotation_property_polygon";
    private static final String PREF_ANNOTATION_PROPERTY_CLOUD = "annotation_property_cloud";
    private static final String PREF_ANNOTATION_PROPERTY_HIGHLIGHT = "annotation_property_highlight";
    private static final String PREF_ANNOTATION_PROPERTY_UNDERLINE = "annotation_property_text_markup"; // underline
    private static final String PREF_ANNOTATION_PROPERTY_LINK = "annotation_property_link"; // link
    private static final String PREF_ANNOTATION_PROPERTY_STRIKEOUT = "annotation_property_strikeout";
    private static final String PREF_ANNOTATION_PROPERTY_SQUIGGLY = "annotation_property_squiggly";
    private static final String PREF_ANNOTATION_PROPERTY_FREETEXT = "annotation_property_freetext";
    private static final String PREF_ANNOTATION_PROPERTY_CALLOUT = "annotation_property_callout";
    private static final String PREF_ANNOTATION_PROPERTY_FREETEXT_DATE = "annotation_property_freetext_date";
    private static final String PREF_ANNOTATION_PROPERTY_FREETEXT_SPACING = "annotation_property_freetext_spacing";
    private static final String PREF_ANNOTATION_PROPERTY_FREEHAND = "annotation_property_freehand";
    private static final String PREF_ANNOTATION_PROPERTY_FREE_HIGHLIGHTER = "annotation_property_free_highlighter";
    private static final String PREF_ANNOTATION_PROPERTY_NOTE = "annotation_property_note";
    private static final String PREF_ANNOTATION_PROPERTY_COUNT = "annotation_property_count";
    private static final String PREF_ANNOTATION_PROPERTY_ERASER = "annotation_property_eraser";
    private static final String PREF_ANNOTATION_PROPERTY_SIGNATURE = "annotation_property_signature";
    private static final String PREF_ANNOTATION_PROPERTY_SOUND = "annotation_property_sound";
    private static final String PREF_ANNOTATION_PROPERTY_REDACTION = "annotation_property_redaction";
    private static final String PREF_ANNOTATION_PROPERTY_PERIMETER_MEASURE = "annotation_property_perimeter_measure";
    private static final String PREF_ANNOTATION_PROPERTY_AREA_MEASURE = "annotation_property_area_measure";
    private static final String PREF_ANNOTATION_PROPERTY_RECT_AREA_MEASURE = "annotation_property_rect_area_measure";
    private static final String PREF_ANNOTATION_PROPERTY_WIDGET = "annotation_property_widget";
    private static final String PREF_ANNOTATION_PROPERTY_SMART_PEN = "annotation_property_smart_pen";

    private static final String PREF_ANNOTATION_PROPERTY_FILL_COLORS = "_fill_colors";
    private static final String PREF_ANNOTATION_PROPERTY_COLOR = "_color";
    private static final String PREF_ANNOTATION_PROPERTY_TEXT_COLOR = "_text_color";
    private static final String PREF_ANNOTATION_PROPERTY_TEXT_SIZE = "_text_size";
    private static final String PREF_ANNOTATION_PROPERTY_FILL_COLOR = "_fill_color";
    private static final String PREF_ANNOTATION_PROPERTY_OPACITY = "_opacity";
    private static final String PREF_ANNOTATION_PROPERTY_THICKNESS = "_thickness";
    private static final String PREF_ANNOTATION_PROPERTY_ICON = "_icon";
    private static final String PREF_ANNOTATION_PROPERTY_FONT = "_font";
    private static final String PREF_ANNOTATION_PROPERTY_ERASER_TYPE = "_eraser_type";
    private static final String PREF_ANNOTATION_PROPERTY_INK_ERASER_MODE_TYPE = "_ink_eraser_mode";
    private static final String PREF_ANNOTATION_PROPERTY_PRESSURE_SENSITIVITY = "_pressure_sensitive";
    private static final String PREF_ANNOTATION_PROPERTY_DATE = "_date";
    private static final String PREF_ANNOTATION_PROPERTY_TEXT_MARKUP_TYPE = "_text_markup_type";
    private static final String PREF_ANNOTATION_PROPERTY_CUSTOM = "_custom";

    private static final String PREF_ANNOTATION_PROPERTY_RULER_BASE_UNIT = "_ruler_base_unit";
    private static final String PREF_ANNOTATION_PROPERTY_RULER_BASE_VALUE = "_ruler_base_value";
    private static final String PREF_ANNOTATION_PROPERTY_RULER_TRANSLATE_UNIT = "_ruler_translate_unit";
    private static final String PREF_ANNOTATION_PROPERTY_RULER_TRANSLATE_VALUE = "_ruler_translate_value";
    private static final String PREF_ANNOTATION_PROPERTY_RULER_PRECISION = "_ruler_precision";

    private static final String PREF_ANNOTATION_PROPERTY_BORDER_STYLE = "_annot_border_style";
    private static final String PREF_ANNOTATION_PROPERTY_LINE_STYLE = "_annot_line_style";
    private static final String PREF_ANNOTATION_PROPERTY_LINE_START_STYLE = "_annot_line_start_style";
    private static final String PREF_ANNOTATION_PROPERTY_LINE_END_STYLE = "_annot_line_end_style";
    private static final String PREF_ANNOTATION_PROPERTY_HORIZONTAL_ALIGNMENT = "_annot_horizontal_alignment";
    private static final String PREF_ANNOTATION_PROPERTY_VERTICAL_ALIGNMENT = "_annot_vertical_alignment";

    /**
     * @return instance of ToolStyleConfig
     */
    public static ToolStyleConfig getInstance() {
        if (null == _INSTANCE) {
            _INSTANCE = new ToolStyleConfig();
        }
        return _INSTANCE;
    }

    /**
     * Annotation type and annotation style map
     */
    private SparseIntArray mAnnotStyleMap;

    /**
     * Annotation type and annotation presets map
     */
    private SparseIntArray mAnnotPresetMap;

    @NonNull
    private Eraser.EraserType mDefaultEraserType = Eraser.EraserType.INK_ERASER;

    // fonts
    private Set<String> mFreeTextFonts;
    private Set<String> mFreeTextFontsFromAssets;
    private Set<String> mFreeTextFontsFromStorage;

    /**
     * Class constructor
     */
    public ToolStyleConfig() {
        mAnnotStyleMap = new SparseIntArray();
    }

    public void setDefaultEraserType(@NonNull Eraser.EraserType type) {
        mDefaultEraserType = type;
    }

    @NonNull
    public Eraser.EraserType getDefaultEraserType() {
        return mDefaultEraserType;
    }

    /**
     * Sets list of free text fonts to have as
     * options in the properties popup.
     * (Sets whiteList fonts among the PDFNet system fonts)
     */
    public void setFreeTextFonts(Set<String> freeTextFonts) {
        mFreeTextFonts = freeTextFonts;
    }

    /**
     * Sets custom font list from Assets for free text tool
     * if sets font list from Assets, then it is not possible to set font list from storage
     * The system fonts list won't load anymore
     *
     * @param fontNameList array of custom font's absolute path from Assets
     */
    public void setFreeTextFontsFromAssets(Set<String> fontNameList) {
        mFreeTextFontsFromAssets = fontNameList;
    }

    /**
     * Sets custom font list from device storage for free text tool
     * if font list from Assets is not set, then it's possible to set font list from storage
     * The system fonts list won't load anymore
     *
     * @param fontPathList array of custom font's absolute path from device storage
     */
    public void setFreeTextFontsFromStorage(Set<String> fontPathList) {
        mFreeTextFontsFromStorage = fontPathList;
    }

    /**
     * Gets the list of free text fonts to have as
     * options in the properties popup
     * (Gets whiteList fonts among the PDFNet system fonts)
     */
    public Set<String> getFreeTextFonts() {
        return mFreeTextFonts;
    }

    /**
     * Gets custom font list from Assets for free text tool
     */
    public Set<String> getFreeTextFontsFromAssets() {
        return mFreeTextFontsFromAssets;
    }

    /**
     * Gets custom font list from Storage for free text tool
     */
    public Set<String> getFreeTextFontsFromStorage() {
        return mFreeTextFontsFromStorage;
    }

    /**
     * Add Customized default style for annotation
     *
     * @param annotType annotation mode
     * @param styleRes  annotation style resource
     */
    public void addDefaultStyleMap(int annotType, @StyleRes int styleRes) {
        mAnnotStyleMap.put(annotType, styleRes);
    }

    /**
     * Add Customized default style for annotation
     *
     * @param annotType annotation mode
     * @param attrRes   preset attribute resource
     */
    public void addAnnotPresetMap(int annotType, @AttrRes int attrRes) {
        if (mAnnotPresetMap == null) {
            mAnnotPresetMap = new SparseIntArray();
        }
        mAnnotPresetMap.put(annotType, attrRes);
    }

    /**
     * Gets default tool style
     *
     * @param annotType annotation tool mode
     * @return style resource
     */
    @StyleRes
    public int getDefaultStyle(int annotType) {
        if (mAnnotStyleMap.indexOfKey(annotType) >= 0) {
            return mAnnotStyleMap.get(annotType);
        }
        switch (annotType) {
            case Annot.e_Widget:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_TEXT_FIELD:
                return R.style.WidgetPreset1;
            case Annot.e_Highlight:
                return R.style.HighlightPresetStyle1;
            case Annot.e_Underline:
            case Annot.e_Squiggly:
            case Annot.e_StrikeOut:
                return R.style.TextMarkupStyle1;
            case Annot.e_FreeText:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_SPACING:
                return R.style.FreeTextPresetStyle1;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT:
                return R.style.CalloutPresetStyle1;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_DATE:
                return R.style.FreeTextDatePresetStyle1;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT:
                return R.style.AnnotPresetStyle3;
            case Annot.e_Text:
                return R.style.AnnotPresetStyle1;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE:
                return R.style.SignaturePresetStyle1;
            case Annot.e_Ink:
                return R.style.AnnotPresetStyle4;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_ERASER:
                return R.style.EraserStyle1;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_HIGHLIGHTER:
                return R.style.FreeHighlighterStyle4;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RULER:
                return R.style.RulerStyle1;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD:
                return R.style.CloudStyle1;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW:
                return R.style.ArrowStyle1;
            default:
                return R.style.AnnotPresetStyle4;
        }
    }

    /**
     * Gets default tool style
     *
     * @param annotType annotation tool mode
     * @return style resource
     */
    @AttrRes
    public int getDefaultAttr(int annotType) {

        switch (annotType) {
            case Annot.e_Widget:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_TEXT_FIELD:
                return R.attr.widget_default_style;
            case Annot.e_Highlight:
                return R.attr.highlight_default_style;
            case Annot.e_Underline:
                return R.attr.underline_default_style;
            case Annot.e_Squiggly:
                return R.attr.squiggly_default_style;
            case Annot.e_StrikeOut:
                return R.attr.strikeout_default_style;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_HIGHLIGHTER:
                return R.attr.free_highlighter_default_style;
            case Annot.e_FreeText:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_SPACING:
                return R.attr.free_text_default_style;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_DATE:
                return R.attr.free_date_text_default_style;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT:
                return R.attr.count_measurement_default_style;
            case Annot.e_Text:
                return R.attr.sticky_note_default_style;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE:
                return R.attr.signature_default_style;
            case Annot.e_Link:
                return R.attr.link_default_style;
            case Annot.e_Ink:
                return R.attr.freehand_default_style;
            case Annot.e_Line:
                return R.attr.line_default_style;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW:
                return R.attr.arrow_default_style;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RULER:
                return R.attr.ruler_default_style;
            case Annot.e_Polyline:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_PERIMETER_MEASURE:
                return R.attr.polyline_default_style;
            case Annot.e_Square:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RECT_AREA_MEASURE:
                return R.attr.rect_default_style;
            case Annot.e_Circle:
                return R.attr.oval_default_style;
            case Annot.e_Polygon:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_AREA_MEASURE:
                return R.attr.polygon_default_style;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD:
                return R.attr.cloud_default_style;
            default:
                return R.attr.other_default_style;
        }
    }

    /**
     * Gets presets attr
     *
     * @param annotType annotation tool mode
     * @return attribute resource
     */
    public @AttrRes
    int getPresetsAttr(int annotType) {
        if (mAnnotPresetMap != null && mAnnotPresetMap.indexOfKey(annotType) >= 0) {
            return mAnnotPresetMap.get(annotType);
        }
        switch (annotType) {
            case Annot.e_Widget:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_TEXT_FIELD:
                return R.attr.widget_presets;
            case Annot.e_Highlight:
                return R.attr.highlight_presets;
            case Annot.e_Underline:
                return R.attr.underline_presets;
            case Annot.e_Squiggly:
                return R.attr.squiggly_presets;
            case Annot.e_StrikeOut:
                return R.attr.strikeout_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_HIGHLIGHTER:
                return R.attr.free_highlighter_presets;
            case Annot.e_FreeText:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_SPACING:
                return R.attr.free_text_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT:
                return R.attr.callout_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_DATE:
                return R.attr.free_date_text_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT:
                return R.attr.count_measurement_presets;
            case Annot.e_Text:
                return R.attr.sticky_note_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE:
                return R.attr.signature_presets;
            case Annot.e_Link:
                return R.attr.link_presets;
            case Annot.e_Ink:
                return R.attr.freehand_presets;
            case Annot.e_Line:
                return R.attr.line_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW:
                return R.attr.arrow_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RULER:
                return R.attr.ruler_presets;
            case Annot.e_Polyline:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_PERIMETER_MEASURE:
                return R.attr.polyline_presets;
            case Annot.e_Square:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RECT_AREA_MEASURE:
                return R.attr.rect_presets;
            case Annot.e_Circle:
                return R.attr.oval_presets;
            case Annot.e_Polygon:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_AREA_MEASURE:
                return R.attr.polygon_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD:
                return R.attr.cloud_presets;
            default:
                return R.attr.other_presets;
        }
    }

    /**
     * Gets default tool style
     *
     * @param annotType annotation tool mode
     * @return style resource
     */
    public @ArrayRes
    int getDefaultPresetsArrayRes(int annotType) {

        switch (annotType) {
            case Annot.e_Widget:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_TEXT_FIELD:
                return R.array.widget_presets;
            case Annot.e_Highlight:
                return R.array.highlight_presets;
            case Annot.e_Underline:
            case Annot.e_Squiggly:
            case Annot.e_StrikeOut:
                return R.array.text_markup_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT:
            case Annot.e_Text:
                return R.array.color_only_presets;
            case Annot.e_FreeText:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_SPACING:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_DATE:
                return R.array.free_text_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT:
                return R.array.callout_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE:
                return R.array.signature_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_ERASER:
                return R.array.eraser_presets;
            case Annot.e_Square:
            case Annot.e_Circle:
            case Annot.e_Polygon:
                return R.array.fill_only_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD:
                return R.array.cloud_presets;
            case Annot.e_Ink:
                return R.array.freehand_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_HIGHLIGHTER:
                return R.array.freehand_highlighter_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RULER:
                return R.array.ruler_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW:
                return R.array.arrow_presets;
            default:
                return R.array.stroke_only_presets;
        }
    }

    /**
     * Gets default tool color
     *
     * @param context   The context
     * @param annotType annotation tool mode
     * @param extraTag  extra tag
     * @return color
     */
    public @ColorInt
    int getDefaultColor(Context context, int annotType, String extraTag) {
        int index = 0;
        if (extraTag.endsWith("1") || Utils.isNullOrEmpty(extraTag)) {
            return getDefaultColor(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
        } else if (extraTag.endsWith("2")) {
            index = 1;
        } else if (extraTag.endsWith("3")) {
            index = 2;
        } else if (extraTag.endsWith("4")) {
            index = 3;
        } else if (extraTag.endsWith("5")) {
            index = 4;
        }
        return getPresetColor(context, index, getPresetsAttr(annotType), getDefaultPresetsArrayRes(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default tool color
     *
     * @param context The context
     * @return color
     */
    public @ColorInt
    int getDefaultColor(@NonNull Context context, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defStyleAttr, defStyleRes);
        int color = a.getColor(R.styleable.ToolStyle_annot_color, Color.BLACK);
        a.recycle();
        return color;
    }

    /**
     * Gets default icon list
     *
     * @param context The context
     * @return list of icon names
     */
    public ArrayList<String> getIconsList(Context context) {
        return getIconsList(context, R.attr.sticky_note_icons, R.array.stickynote_icons);
    }

    /**
     * Gets icon list based on given attribute resource and default array resource
     *
     * @param context     The context
     * @param defAttrRes  attribute resource
     * @param defArrayRes array resource
     * @return list of icon names
     */
    public ArrayList<String> getIconsList(@NonNull Context context, @AttrRes int defAttrRes, @ArrayRes int defArrayRes) {

        TypedArray typedArray = context.obtainStyledAttributes(new int[]{defAttrRes});
        int iconsRes = typedArray.getResourceId(0, defArrayRes);
        typedArray.recycle();

        TypedArray iconsArray = context.getResources().obtainTypedArray(iconsRes);
        ArrayList<String> icons = new ArrayList<>();
        for (int i = 0; i < iconsArray.length(); i++) {
            String icon = iconsArray.getString(i);
            if (!Utils.isNullOrEmpty(icon)) {
                icons.add(icon);
            }
        }
        iconsArray.recycle();
        return icons;
    }

    /**
     * Gets default color
     *
     * @param context   The context
     * @param annotType annotation tool mode
     * @return color
     */
    public @ColorInt
    int getDefaultColor(Context context, int annotType) {
        return getDefaultColor(context, annotType, "");
    }

    /**
     * Gets default text color
     *
     * @param context the context
     * @return color
     * @deprecated use {@link #getDefaultTextColor(Context, int)} instead
     */
    @Deprecated
    public @ColorInt
    int getDefaultTextColor(Context context) {
        return getDefaultTextColor(context, getDefaultAttr(Annot.e_FreeText), getDefaultStyle(Annot.e_FreeText));
    }

    /**
     * Gets default text color for the given annotation type.
     *
     * @param context   the context
     * @param annotType the annotation type
     * @return color
     */
    public @ColorInt
    int getDefaultTextColor(@NonNull Context context, int annotType) {
        return getDefaultTextColor(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default text color
     *
     * @param context     the context
     * @param attrRes     attribute resource
     * @param defStyleRes default style resource
     * @return color
     */
    @ColorInt
    public int getDefaultTextColor(@NonNull Context context, @AttrRes int attrRes, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, attrRes, defStyleRes);
        int color = a.getColor(R.styleable.ToolStyle_annot_text_color, Color.BLACK);
        a.recycle();
        return color;
    }

    /**
     * Gets default date format
     *
     * @param context   The context
     * @param annotType annotation tool mode
     * @return default date format
     */
    public String getDefaultDateFormat(Context context, int annotType) {
        return getDefaultDateFormat(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default date format
     *
     * @param context The context
     * @return default date format
     */
    public String getDefaultDateFormat(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        String dateFormat = a.getString(R.styleable.ToolStyle_annot_date_format);
        a.recycle();
        if (null == dateFormat) {
            dateFormat = context.getResources().getString(R.string.style_picker_date_format1);
        }
        return dateFormat;
    }

    /**
     * Gets default thickness
     *
     * @param context   The context
     * @param annotType annotation tool mode
     * @return default thickness
     */
    public float getDefaultThickness(Context context, int annotType) {
        return getDefaultThickness(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default thickness
     *
     * @param context The context
     * @return default thickness
     */
    public float getDefaultThickness(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        float thickness = a.getFloat(R.styleable.ToolStyle_annot_thickness, 1.0f);
        a.recycle();
        return thickness;
    }

    /**
     * Gets default ruler scale base value
     *
     * @param context   The context
     * @param annotType annotation tool mode
     * @return default value
     */
    public float getDefaultRulerBaseValue(Context context, int annotType) {
        return getDefaultRulerBaseValue(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default ruler scale base value
     *
     * @param context The context
     * @return default value
     */
    public float getDefaultRulerBaseValue(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        float value = a.getFloat(R.styleable.ToolStyle_ruler_base_value, 1.0f);
        a.recycle();
        return value;
    }

    /**
     * Gets default ruler scale base unit
     *
     * @param context   The context
     * @param annotType annotation tool mode
     * @return default unit
     */
    public String getDefaultRulerBaseUnit(Context context, int annotType) {
        return getDefaultRulerBaseUnit(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default ruler scale base unit
     *
     * @param context The context
     * @return default unit
     */
    public String getDefaultRulerBaseUnit(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        String unit = a.getString(R.styleable.ToolStyle_ruler_base_unit);
        if (unit == null) {
            unit = MeasureUtils.U_IN;
        }
        a.recycle();
        return unit;
    }

    /**
     * Gets default ruler scale translate value
     *
     * @param context   The context
     * @param annotType annotation tool mode
     * @return default value
     */
    public float getDefaultRulerTranslateValue(Context context, int annotType) {
        return getDefaultRulerTranslateValue(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default ruler scale translate value
     *
     * @param context The context
     * @return default value
     */
    public float getDefaultRulerTranslateValue(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        float value = a.getFloat(R.styleable.ToolStyle_ruler_translate_value, 1.0f);
        a.recycle();
        return value;
    }

    /**
     * Gets default ruler scale base unit
     *
     * @param context   The context
     * @param annotType annotation tool mode
     * @return default unit
     */
    public String getDefaultRulerTranslateUnit(Context context, int annotType) {
        return getDefaultRulerTranslateUnit(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default ruler scale base unit
     *
     * @param context The context
     * @return default unit
     */
    public String getDefaultRulerTranslateUnit(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        String unit = a.getString(R.styleable.ToolStyle_ruler_translate_unit);
        if (unit == null) {
            unit = MeasureUtils.U_IN;
        }
        a.recycle();
        return unit;
    }

    /**
     * Gets default ruler precision
     *
     * @param context   The context
     * @param annotType annotation tool mode
     * @return default value
     */
    public int getDefaultRulerPrecision(Context context, int annotType) {
        return getDefaultRulerPrecision(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default ruler precision
     *
     * @param context The context
     * @return default value
     */
    public int getDefaultRulerPrecision(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        int value = a.getInt(R.styleable.ToolStyle_ruler_precision, MeasureUtils.PRECISION_DEFAULT);
        a.recycle();
        return value;
    }

    /**
     * Gets default line style for the given annotation type.
     *
     * @param context   The context
     * @param annotType The annotation type
     * @return default line style
     */
    public LineStyle getDefaultLineStyle(@NonNull Context context, int annotType) {
        return getDefaultLineStyle(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default line style
     *
     * @param context The context
     * @return default line style
     */
    public LineStyle getDefaultLineStyle(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        String lineStyle;
        try {
            lineStyle = a.getString(R.styleable.ToolStyle_annot_line_style);
        } finally {
            a.recycle();
        }
        return lineStyle == null ? LineStyle.DEFAULT : LineStyle.valueOf(lineStyle);
    }

    /**
     * Gets default border style for the given annotation type.
     *
     * @param context   The context
     * @param annotType The annotation type
     * @return default border style
     */
    public ShapeBorderStyle getDefaultBorderStyle(@NonNull Context context, int annotType) {
        return getDefaultBorderStyle(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default border style
     *
     * @param context The context
     * @return default border style
     */
    public ShapeBorderStyle getDefaultBorderStyle(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        String borderStyle;
        try {
            borderStyle = a.getString(R.styleable.ToolStyle_annot_border_style);
        } finally {
            a.recycle();
        }
        return borderStyle == null ? ShapeBorderStyle.DEFAULT : ShapeBorderStyle.valueOf(borderStyle);
    }

    /**
     * Gets default line start style for the given annotation type.
     *
     * @param context   The context
     * @param annotType The annotation type
     * @return default line start style
     */
    public LineEndingStyle getDefaultLineStartStyle(@NonNull Context context, int annotType) {
        return getDefaultLineStartStyle(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default line start style
     *
     * @param context The context
     * @return default line start style
     */
    public LineEndingStyle getDefaultLineStartStyle(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        String lineStartStyle;
        try {
            lineStartStyle = a.getString(R.styleable.ToolStyle_annot_line_start_style);
        } finally {
            a.recycle();
        }
        return lineStartStyle == null ? LineEndingStyle.NONE : LineEndingStyle.valueOf(lineStartStyle);
    }

    /**
     * Gets default line end style for the given annotation type.
     *
     * @param context   The context
     * @param annotType The annotation type
     * @return default line end style
     */
    public LineEndingStyle getDefaultLineEndStyle(@NonNull Context context, int annotType) {
        return getDefaultLineEndStyle(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default line end style
     *
     * @param context The context
     * @return default line end style
     */
    public LineEndingStyle getDefaultLineEndStyle(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        String lineEndStyle;
        try {
            lineEndStyle = a.getString(R.styleable.ToolStyle_annot_line_end_style);
        } finally {
            a.recycle();
        }
        return lineEndStyle == null ? LineEndingStyle.NONE : LineEndingStyle.valueOf(lineEndStyle);
    }

    /**
     * Gets default font size for the given annotation type.
     *
     * @param context   The context
     * @param annotType The annotation type
     * @return default font size
     */
    public float getDefaultTextSize(@NonNull Context context, int annotType) {
        return getDefaultTextSize(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default font size
     *
     * @param context The context
     * @return default font size
     */
    public float getDefaultTextSize(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {

        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        float fontSize;
        try {
            fontSize = a.getFloat(R.styleable.ToolStyle_annot_font_size, 16);
        } finally {
            a.recycle();
        }
        return fontSize;
    }

    /**
     * Gets default fill color
     *
     * @param context   The context
     * @param annotType The annotation type
     * @return default fill color
     */
    public @ColorInt
    int getDefaultFillColor(Context context, int annotType) {
        return getDefaultFillColor(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default fill color
     *
     * @param context context
     * @return default fill color
     */
    @ColorInt
    public int getDefaultFillColor(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        int color;
        try {
            color = a.getColor(R.styleable.ToolStyle_annot_fill_color, Color.TRANSPARENT);
        } finally {
            a.recycle();
        }
        return color;
    }

    /**
     * Gets default maximum thickness
     *
     * @param context   context
     * @param annotType The annotation type
     * @return maximum thickness
     */
    public float getDefaultMaxThickness(Context context, int annotType) {
        return getDefaultMaxThickness(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default maximum thickness
     *
     * @param context context
     * @return maximum thickness
     */
    public float getDefaultMaxThickness(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        float thickness;
        try {
            thickness = a.getFloat(R.styleable.ToolStyle_annot_thickness_max, 1.0f);
        } finally {
            a.recycle();
        }
        return thickness;
    }

    /**
     * Gets default minimum thickness
     *
     * @param context   context
     * @param annotType annotation tool mode
     * @return minimum thickness
     */
    public float getDefaultMinThickness(Context context, int annotType) {
        return getDefaultMinThickness(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default minimum thickness
     *
     * @param context context
     * @return minimum thickness
     */
    public float getDefaultMinThickness(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        float thickness;
        try {
            thickness = a.getFloat(R.styleable.ToolStyle_annot_thickness_min, 0);
        } finally {
            a.recycle();
        }
        return thickness;
    }

    /**
     * Gets default minimum text size
     *
     * @param context context
     * @return minimum text size
     */
    public float getDefaultMinTextSize(Context context) {
        return getDefaultMinTextSize(context, getDefaultAttr(Annot.e_FreeText), getDefaultStyle(Annot.e_FreeText));
    }

    /**
     * Gets default minimum text size
     *
     * @param context context
     * @return minimum text size
     */
    public float getDefaultMinTextSize(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        float thickness;
        try {
            thickness = a.getFloat(R.styleable.ToolStyle_annot_text_size_min, 1.0f);
        } finally {
            a.recycle();
        }
        return thickness;
    }

    /**
     * Gets default maximum text size
     *
     * @param context context
     * @return maximum text size
     */
    public float getDefaultMaxTextSize(Context context) {
        return getDefaultMaxTextSize(context, getDefaultAttr(Annot.e_FreeText), getDefaultStyle(Annot.e_FreeText));
    }

    /**
     * Gets default maximum text size
     *
     * @param context context
     * @return maximum text size
     */
    public float getDefaultMaxTextSize(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        float thickness;
        try {
            thickness = a.getFloat(R.styleable.ToolStyle_annot_text_size_max, 72.0f);
        } finally {
            a.recycle();
        }
        return thickness;
    }

    /**
     * Gets default font
     *
     * @param context   context
     * @param annotType annotation tool mode
     * @return default font
     */
    public String getDefaultFont(Context context, int annotType) {
        return getDefaultFont(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default font
     *
     * @param context context
     * @return default font
     */
    public String getDefaultFont(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        String font;
        try {
            font = a.getString(R.styleable.ToolStyle_annot_font);
        } finally {
            a.recycle();
        }
        if (null == font) {
            return "";
        }
        return font;
    }

    /**
     * Gets default icon
     *
     * @param context   context
     * @param annotType annotation tool mode
     * @return default icon
     */
    public String getDefaultIcon(Context context, int annotType) {
        return getDefaultIcon(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default icon
     *
     * @param context context
     * @return default icon
     */
    public String getDefaultIcon(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        String result;
        try {
            result = a.getString(R.styleable.ToolStyle_annot_icon);
        } finally {
            a.recycle();
        }
        if (null == result) {
            return "";
        }
        return result;
    }

    /**
     * Gets default opacity
     *
     * @param context   context
     * @param annotType annotation tool mode
     * @return default opacity
     */
    public float getDefaultOpacity(Context context, int annotType) {
        return getDefaultOpacity(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default opacity
     *
     * @param context context
     * @return default opacity
     */
    public float getDefaultOpacity(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        float result;
        try {
            result = a.getFloat(R.styleable.ToolStyle_annot_opacity, 1.0f);
        } finally {
            a.recycle();
        }
        return result;
    }

    /**
     * Gets whether freehand will use pressure sensitive calculation
     *
     * @param context context
     * @return default pressure sensitive option
     */
    public boolean getDefaultPressureSensitivity(@NonNull Context context, int annotType) {
        return getDefaultPressureSensitivity(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets whether freehand will use pressure sensitive calculation
     *
     * @param context context
     * @return default pressure sensitive option
     */
    public boolean getDefaultPressureSensitivity(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        boolean result;
        try {
            result = a.getBoolean(R.styleable.ToolStyle_ink_pressure, false);
        } finally {
            a.recycle();
        }
        return result;
    }

    /**
     * Gets thickness range
     *
     * @param context   context
     * @param annotType annotation tool mode
     * @return thickness range
     */
    public float getDefaultThicknessRange(Context context, int annotType) {
        return getDefaultThicknessRange(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets thickness range
     *
     * @param context context
     * @return thickness range
     */
    public float getDefaultThicknessRange(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        float thicknessRange;
        try {
            float thicknessMin = a.getFloat(R.styleable.ToolStyle_annot_thickness_min, 0);
            float thicknessMax = a.getFloat(R.styleable.ToolStyle_annot_thickness_max, 1);
            thicknessRange = thicknessMax - thicknessMin;
        } finally {
            a.recycle();
        }
        return thicknessRange;
    }

    /**
     * Gets the horizontal text alignment
     *
     * @param context   the context
     * @param annotType annotation tool mode
     * @return the horizontal text alignment
     */
    public int getDefaultHorizontalAlignment(@NonNull Context context, int annotType) {
        return getDefaultHorizontalAlignment(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets the horizontal text alignment
     *
     * @param context the context
     * @return the horizontal text alignment
     */
    public int getDefaultHorizontalAlignment(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        int alignment;
        try {
            alignment = a.getInt(R.styleable.ToolStyle_annot_horizontal_alignment, Gravity.NO_GRAVITY);
        } finally {
            a.recycle();
        }
        return alignment;
    }

    /**
     * Gets the vertical text alignment
     *
     * @param context   the context
     * @param annotType annotation tool mode
     * @return the vertical text alignment
     */
    public int getDefaultVerticalAlignment(@NonNull Context context, int annotType) {
        return getDefaultVerticalAlignment(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets the vertical text alignment
     *
     * @param context the context
     * @return the vertical text alignment
     */
    public int getDefaultVerticalAlignment(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        int alignment;
        try {
            alignment = a.getInt(R.styleable.ToolStyle_annot_vertical_alignment, Gravity.NO_GRAVITY);
        } finally {
            a.recycle();
        }
        return alignment;
    }

    /**
     * Gets default annotation style defined in attrs
     *
     * @param context   the context
     * @param annotType tool mode
     * @return annotation style
     */
    public AnnotStyle getDefaultAnnotStyle(Context context, int annotType) {
        AnnotStyle annotStyle = new AnnotStyle();
        annotStyle.setAnnotType(annotType);
        annotStyle.setStrokeColor(getDefaultColor(context, annotType));
        annotStyle.setFillColor(getDefaultFillColor(context, annotType));
        annotStyle.setOpacity(getDefaultOpacity(context, annotType));
        if (annotStyle.hasTextStyle()) {
            annotStyle.setTextSize(getDefaultTextSize(context, annotType));
            annotStyle.setTextColor(getDefaultTextColor(context, annotType));
        }

        if (annotStyle.hasBorderStyle()) {
            annotStyle.setBorderStyle(getDefaultBorderStyle(context, annotType));
        } else if (annotStyle.hasLineStyle()) {
            annotStyle.setLineStyle(getDefaultLineStyle(context, annotType));
        }
        if (annotStyle.hasLineStartStyle()) {
            annotStyle.setLineStartStyle(getDefaultLineStartStyle(context, annotType));
        }
        if (annotStyle.hasLineEndStyle()) {
            annotStyle.setLineEndStyle(getDefaultLineEndStyle(context, annotType));
        }

        if (annotStyle.hasFont()) {
            annotStyle.setFont(new FontResource(getDefaultFont(context, annotType)));
        }

        if (annotStyle.hasTextAlignment()) {
            annotStyle.setHorizontalAlignment(getDefaultHorizontalAlignment(context, annotType));
            annotStyle.setVerticalAlignment(getDefaultVerticalAlignment(context, annotType));
        }

        annotStyle.setThickness(getDefaultThickness(context, annotType));

        if (annotStyle.isStickyNote() || annotStyle.isCountMeasurement()) {
            annotStyle.setIcon(getDefaultIcon(context, annotType));
        } else if (annotStyle.isSound()) {
            annotStyle.setIcon(SoundCreate.SOUND_ICON);
        } else if (annotStyle.isMeasurement()) {
            annotStyle.setRulerBaseValue(getDefaultRulerBaseValue(context, annotType));
            annotStyle.setRulerBaseUnit(getDefaultRulerBaseUnit(context, annotType));
            annotStyle.setRulerTranslateValue(getDefaultRulerTranslateValue(context, annotType));
            annotStyle.setRulerTranslateUnit(getDefaultRulerTranslateUnit(context, annotType));
            annotStyle.setRulerPrecision(getDefaultRulerPrecision(context, annotType));
        } else if (annotStyle.isDateFreeText()) {
            annotStyle.setDateFormat(getDefaultDateFormat(context, annotType));
        }
        return annotStyle;
    }

    /**
     * Gets custom annotation style from settings
     *
     * @param context   the context
     * @param annotType tool mode
     * @return annotation style
     */
    public AnnotStyle getCustomAnnotStyle(@NonNull Context context, int annotType, String extraTag) {
        AnnotStyle annotStyle = new AnnotStyle();
        annotStyle.setAnnotType(annotType);

        annotStyle.setThickness(getCustomThickness(context, annotType, extraTag));
        annotStyle.setOpacity(getCustomOpacity(context, annotType, extraTag));
        annotStyle.setStrokeColor(getCustomColor(context, annotType, extraTag));
        annotStyle.setFillColor(getCustomFillColor(context, annotType, extraTag));
        annotStyle.setTextColor(getCustomTextColor(context, annotType, extraTag));
        annotStyle.setTextSize(getCustomTextSize(context, annotType, extraTag));
        annotStyle.setIcon(getCustomIconName(context, annotType, extraTag));
        annotStyle.setRulerBaseValue(getCustomRulerBaseValue(context, annotType, extraTag));
        annotStyle.setRulerBaseUnit(getCustomRulerBaseUnit(context, annotType, extraTag));
        annotStyle.setRulerTranslateValue(getCustomRulerTranslateValue(context, annotType, extraTag));
        annotStyle.setRulerTranslateUnit(getCustomRulerTranslateUnit(context, annotType, extraTag));
        annotStyle.setRulerPrecision(getCustomRulerPrecision(context, annotType, extraTag));
        annotStyle.setEraserType(getCustomEraserType(context, annotType, extraTag));
        annotStyle.setInkEraserMode(getCustomInkEraserMode(context, annotType, extraTag));
        annotStyle.setDateFormat(getCustomDateFormat(context, annotType, extraTag));
        annotStyle.setPressureSensitivity(getCustomPressureSensitive(context, annotType, extraTag));
        if (annotStyle.hasBorderStyle()) {
            annotStyle.setBorderStyle(getCustomBorderStyleMode(context, annotType, extraTag));
        } else if (annotStyle.hasLineStyle()) {
            annotStyle.setLineStyle(getCustomLineStyleMode(context, annotType, extraTag));
        }
        if (annotStyle.hasLineStartStyle()) {
            annotStyle.setLineStartStyle(getCustomLineStartStyleMode(context, annotType, extraTag));
        }
        if (annotStyle.hasLineEndStyle()) {
            annotStyle.setLineEndStyle(getCustomLineEndStyleMode(context, annotType, extraTag));
        }
        if (annotStyle.hasTextAlignment()) {
            annotStyle.setHorizontalAlignment(getCustomHorizontalAlignment(context, annotType, extraTag));
            annotStyle.setVerticalAlignment(getCustomVerticalAlignment(context, annotType, extraTag));
        }
        String fontPDFTronName = getCustomFontName(context, annotType, extraTag);
        FontResource mFont = new FontResource(fontPDFTronName);
        annotStyle.setFont(mFont);

        return annotStyle;
    }

    // TODO make it private
    public String getThicknessKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_THICKNESS);
    }

    // TODO make it private
    public String getBorderStyleKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_BORDER_STYLE);
    }

    // TODO make it private
    public String getLineStyleKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_LINE_STYLE);
    }

    // TODO make it private
    public String getLineStartStyleKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_LINE_START_STYLE);
    }

    // TODO make it private
    public String getLineEndStyleKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_LINE_END_STYLE);
    }

    // TODO make it private
    public String getOpacityKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_OPACITY);
    }

    // TODO make it private
    public String getColorKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_COLOR);
    }

    // TODO make it private
    public String getTextColorKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_TEXT_COLOR);
    }

    // TODO make it private
    public String getTextSizeKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_TEXT_SIZE);
    }

    public String getDateFormatKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_DATE);
    }

    // TODO make it private
    public String getFillColorKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_FILL_COLOR);
    }

    // TODO make it private
    public String getIconKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_ICON);
    }

    // TODO make it private
    public String getFontKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_FONT);
    }

    public String getRulerBaseUnitKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_RULER_BASE_UNIT);
    }

    public String getRulerTranslateUnitKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_RULER_TRANSLATE_UNIT);
    }

    public String getRulerBaseValueKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_RULER_BASE_VALUE);
    }

    public String getRulerTranslateValueKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_RULER_TRANSLATE_VALUE);
    }

    public String getRulerPrecisionKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_RULER_PRECISION);
    }

    public String getEraserTypeKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_ERASER_TYPE);
    }

    public String getInkEraserModeKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_INK_ERASER_MODE_TYPE);
    }

    public String getPressureSensitiveKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_PRESSURE_SENSITIVITY);
    }

    public String getTextMarkupTypeKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_TEXT_MARKUP_TYPE);
    }

    public String getHorizontalAlignmentKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_HORIZONTAL_ALIGNMENT);
    }

    public String getVerticalAlignmentKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_VERTICAL_ALIGNMENT);
    }

    /**
     * Save annotation style to settings. The saved annotation style can be retrieved by {@link #getCustomAnnotStyle(Context, int, String)}
     *
     * @param context    The context
     * @param annotStyle annotation style
     * @param extraTag   extra tag for settings
     */
    public void saveAnnotStyle(@NonNull Context context, AnnotStyle annotStyle, String extraTag) {
        SharedPreferences settings = Tool.getToolPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        int annotType = annotStyle.getAnnotType();
        editor.putFloat(getThicknessKey(annotType, extraTag), annotStyle.getThickness());
        editor.putFloat(getOpacityKey(annotType, extraTag), annotStyle.getOpacity());
        editor.putInt(getColorKey(annotType, extraTag), annotStyle.getColor());
        editor.putInt(getTextColorKey(annotType, extraTag), annotStyle.getTextColor());
        editor.putFloat(getTextSizeKey(annotType, extraTag), annotStyle.getTextSize());
        editor.putInt(getFillColorKey(annotType, extraTag), annotStyle.getFillColor());
        editor.putString(getIconKey(annotType, extraTag), annotStyle.getIcon());
        editor.putString(getRulerBaseUnitKey(annotType, extraTag), annotStyle.getRulerBaseUnit());
        editor.putFloat(getRulerBaseValueKey(annotType, extraTag), annotStyle.getRulerBaseValue());
        editor.putString(getRulerTranslateUnitKey(annotType, extraTag), annotStyle.getRulerTranslateUnit());
        editor.putFloat(getRulerTranslateValueKey(annotType, extraTag), annotStyle.getRulerTranslateValue());
        editor.putInt(getRulerPrecisionKey(annotType, extraTag), annotStyle.getPrecision());
        editor.putBoolean(getPressureSensitiveKey(annotType, extraTag), annotStyle.getPressureSensitive());
        String font = annotStyle.getFont() != null ? annotStyle.getFont().getPDFTronName() : "";
        editor.putString(getFontKey(annotType, extraTag), font);
        editor.putInt(getHorizontalAlignmentKey(annotType, extraTag), annotStyle.getHorizontalAlignment());
        editor.putInt(getVerticalAlignmentKey(annotType, extraTag), annotStyle.getVerticalAlignment());
        editor.apply();
    }

    /**
     * Gets color saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return color in settings
     */
    public int getCustomColor(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getInt(
                getColorKey(annotType, extraTag),
                getDefaultColor(context, annotType, extraTag));
    }

    /**
     * Gets text color saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return text color in settings
     */
    public int getCustomTextColor(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getInt(
                getTextColorKey(annotType, extraTag),
                getDefaultTextColor(context, annotType));
    }

    /**
     * Gets text size saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return text size in settings
     */
    public float getCustomTextSize(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getFloat(
                getTextSizeKey(annotType, extraTag),
                getDefaultTextSize(context, annotType));
    }

    /**
     * Gets date format saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return date format in settings
     */
    public String getCustomDateFormat(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getString(
                getDateFormatKey(annotType, extraTag),
                getDefaultDateFormat(context, annotType));
    }

    /**
     * Gets fill color saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return fill color in settings
     */
    public int getCustomFillColor(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getInt(
                getFillColorKey(annotType, extraTag),
                getDefaultFillColor(context, annotType));
    }

    /**
     * Gets thickness saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return thickness in settings
     */
    public float getCustomThickness(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getFloat(
                getThicknessKey(annotType, extraTag),
                getDefaultThickness(context, annotType));
    }

    /**
     * Gets opacity saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return opacity in settings
     */
    public float getCustomOpacity(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getFloat(
                getOpacityKey(annotType, extraTag),
                getDefaultOpacity(context, annotType));
    }

    /**
     * Gets font name saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return font name in settings
     */
    public String getCustomFontName(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getString(
                getFontKey(annotType, extraTag),
                getDefaultFont(context, annotType));
    }

    /**
     * Gets icon name saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return icon name in settings
     */
    public String getCustomIconName(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getString(
                getIconKey(annotType, extraTag),
                getDefaultIcon(context, annotType));
    }

    /**
     * Gets ruler base value saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return ruler base value in settings
     */
    public float getCustomRulerBaseValue(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getFloat(
                getRulerBaseValueKey(annotType, extraTag),
                getDefaultRulerBaseValue(context, annotType));
    }

    /**
     * Gets ruler base unit saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return ruler base unit in settings
     */
    public String getCustomRulerBaseUnit(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getString(
                getRulerBaseUnitKey(annotType, extraTag),
                getDefaultRulerBaseUnit(context, annotType));
    }

    /**
     * Gets borderStyle mode saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return BorderStyle saved in settings
     */
    public ShapeBorderStyle getCustomBorderStyleMode(@NonNull Context context, int annotType, String extraTag) {
        String borderStyleMode = Tool.getToolPreferences(context).getString(
                getBorderStyleKey(annotType, extraTag),
                ShapeBorderStyle.DEFAULT.name());
        return ShapeBorderStyle.valueOf(borderStyleMode);
    }

    /**
     * Gets lineStyle mode saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return LineStyle saved in settings
     */
    public LineStyle getCustomLineStyleMode(@NonNull Context context, int annotType, String extraTag) {
        String lineStyleMode = Tool.getToolPreferences(context).getString(
                getLineStyleKey(annotType, extraTag),
                LineStyle.DEFAULT.name());
        return LineStyle.valueOf(lineStyleMode);
    }

    /**
     * Gets lineStartStyle mode saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return LineEndingStyle saved in settings
     */
    public LineEndingStyle getCustomLineStartStyleMode(@NonNull Context context, int annotType, String extraTag) {
        String lineStartStyleMode = Tool.getToolPreferences(context).getString(
                getLineStartStyleKey(annotType, extraTag),
                LineEndingStyle.NONE.name());
        return LineEndingStyle.valueOf(lineStartStyleMode);
    }

    /**
     * Gets lineEndStyle mode saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return LineEndingStyle saved in settings
     */
    public LineEndingStyle getCustomLineEndStyleMode(@NonNull Context context, int annotType, String extraTag) {
        String lineEndStyleMode = Tool.getToolPreferences(context).getString(
                getLineEndStyleKey(annotType, extraTag),
                LineEndingStyle.NONE.name());
        return LineEndingStyle.valueOf(lineEndStyleMode);
    }

    /**
     * Gets ruler translate value saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return ruler translate value in settings
     */
    public float getCustomRulerTranslateValue(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getFloat(
                getRulerTranslateValueKey(annotType, extraTag),
                getDefaultRulerTranslateValue(context, annotType));
    }

    /**
     * Gets ruler translate unit saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return ruler translate unit in settings
     */
    public String getCustomRulerTranslateUnit(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getString(
                getRulerTranslateUnitKey(annotType, extraTag),
                getDefaultRulerTranslateUnit(context, annotType));
    }

    /**
     * Gets ruler translate unit saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return ruler translate unit in settings
     */
    public int getCustomRulerPrecision(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getInt(
                getRulerPrecisionKey(annotType, extraTag),
                getDefaultRulerPrecision(context, annotType));
    }

    /**
     * Gets the eraser type saved in settings
     *
     * @param context   the context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return the eraser type saved in settings
     */
    public Eraser.EraserType getCustomEraserType(@NonNull Context context, int annotType, String extraTag) {
        String eraserTypeStr = Tool.getToolPreferences(context).getString(
                getEraserTypeKey(annotType, extraTag),
                mDefaultEraserType.name());
        return Eraser.EraserType.valueOf(eraserTypeStr);
    }

    /**
     * Gets the ink eraser mode saved in settings
     *
     * @param context   the context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return the eraser type saved in settings
     */
    public Eraser.InkEraserMode getCustomInkEraserMode(@NonNull Context context, int annotType, String extraTag) {
        String inkEraserMode = Tool.getToolPreferences(context).getString(
                getInkEraserModeKey(annotType, extraTag),
                Eraser.InkEraserMode.PIXEL.name());
        return Eraser.InkEraserMode.valueOf(inkEraserMode);
    }

    /**
     * Gets the eraser type saved in settings
     *
     * @param context   the context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return the eraser type saved in settings
     */
    public boolean getCustomPressureSensitive(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getBoolean(
                getPressureSensitiveKey(annotType, extraTag),
                false);
    }

    public int getCustomHorizontalAlignment(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getInt(
                getHorizontalAlignmentKey(annotType, extraTag),
                getDefaultHorizontalAlignment(context, annotType));
    }

    public int getCustomVerticalAlignment(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getInt(
                getVerticalAlignmentKey(annotType, extraTag),
                getDefaultVerticalAlignment(context, annotType));
    }

    /**
     * Gets annotation preset style saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param index     index of the preset
     * @return annotation style
     */
    public AnnotStyle getAnnotPresetStyle(Context context, int annotType, int index) {
        // load preset from settings first
        String presetJSON = PdfViewCtrlSettingsManager.getAnnotStylePreset(context, annotType, index);
        if (!Utils.isNullOrEmpty(presetJSON)) {
            return AnnotStyle.loadJSONString(context, presetJSON, annotType);
        }

        return getDefaultAnnotPresetStyle(context, annotType, index, getPresetsAttr(annotType), getDefaultPresetsArrayRes(annotType));
    }

    /**
     * Gets annotation preset style saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param index     index of the preset
     * @param suffix    suffix string to uniquely identify this annot style
     * @return annotation style
     */
    public AnnotStyle getAnnotPresetStyle(Context context, int annotType, int index, String suffix) {
        // load preset from settings first
        String presetJSON = PdfViewCtrlSettingsManager.getAnnotStylePreset(context, annotType, index, suffix);
        if (!Utils.isNullOrEmpty(presetJSON)) {
            return AnnotStyle.loadJSONString(context, presetJSON, annotType);
        }

        return getDefaultAnnotPresetStyle(context, annotType, index, getPresetsAttr(annotType), getDefaultPresetsArrayRes(annotType));
    }

    /**
     * Returns the number of presets defined in styles for given annot type
     *
     * @param context   the context to obtain preset from style attributes
     * @param annotType the annot type of preset
     * @return Returns the number of presets for given annot type
     */
    public int numberOfAnnotPresetStyles(@NonNull Context context, int annotType) {
        int attrRes = getPresetsAttr(annotType);
        int arrayRes = getDefaultPresetsArrayRes(annotType);
        // load the preset from style attributes

        TypedArray typedArray = context.obtainStyledAttributes(new int[]{attrRes});
        int presetArrayRes = typedArray.getResourceId(0, arrayRes);
        typedArray.recycle();

        TypedArray presetsArray = context.getResources().obtainTypedArray(presetArrayRes);
        int numStyles = presetsArray.length();
        presetsArray.recycle();
        return numStyles;
    }

    /**
     * Gets default annotation preset style defined in attribute, style, and array resource
     *
     * @param context   The context
     * @param annotType annotation type
     * @param index     index of annotation type
     * @param attrRes   attribute resource for annotation preset
     * @param arrayRes  array resource for defining presets of annotation style
     * @return annotation style of preset
     */
    public AnnotStyle getDefaultAnnotPresetStyle(@NonNull Context context, int annotType, int index, @AttrRes int attrRes, @ArrayRes int arrayRes) {
        // load the preset from style attributes

        TypedArray typedArray = context.obtainStyledAttributes(new int[]{attrRes});
        int presetArrayRes = typedArray.getResourceId(0, arrayRes);
        typedArray.recycle();

        TypedArray presetsArray = context.getResources().obtainTypedArray(presetArrayRes);
        int styleResId = presetsArray.getResourceId(index, getDefaultStyle(annotType));

        presetsArray.recycle();

        AnnotStyle annotStyle = new AnnotStyle();
        annotStyle.setAnnotType(annotType);
        annotStyle.setStrokeColor(getDefaultColor(context, 0, styleResId));
        annotStyle.setFillColor(getDefaultFillColor(context, 0, styleResId));
        annotStyle.setOpacity(getDefaultOpacity(context, 0, styleResId));
        if (annotStyle.hasTextStyle()) {
            annotStyle.setTextSize(getDefaultTextSize(context, 0, styleResId));
            annotStyle.setTextColor(getDefaultTextColor(context, 0, styleResId));
        }
        if (annotStyle.hasFont()) {
            annotStyle.setFont(new FontResource(getDefaultFont(context, 0, styleResId)));
        }
        if (annotStyle.hasBorderStyle()) {
            annotStyle.setBorderStyle(getDefaultBorderStyle(context, 0, styleResId));
        } else if (annotStyle.hasLineStyle()) {
            annotStyle.setLineStyle(getDefaultLineStyle(context, 0, styleResId));
        }
        if (annotStyle.hasLineStartStyle()) {
            annotStyle.setLineStartStyle(getDefaultLineStartStyle(context, 0, styleResId));
        }
        if (annotStyle.hasLineEndStyle()) {
            annotStyle.setLineEndStyle(getDefaultLineEndStyle(context, 0, styleResId));
        }
        if (annotStyle.hasTextAlignment()) {
            annotStyle.setHorizontalAlignment(getDefaultHorizontalAlignment(context, 0, styleResId));
            annotStyle.setVerticalAlignment(getDefaultVerticalAlignment(context, 0, styleResId));
        }
        annotStyle.setThickness(getDefaultThickness(context, 0, styleResId));
        if (annotStyle.isStickyNote()) {
            annotStyle.setIcon(getDefaultIcon(context, 0, styleResId));
        } else if (annotStyle.isSound()) {
            annotStyle.setIcon(SoundCreate.SOUND_ICON);
        } else if (annotStyle.isMeasurement()) {
            annotStyle.setRulerBaseValue(getDefaultRulerBaseValue(context, 0, styleResId));
            annotStyle.setRulerBaseUnit(getDefaultRulerBaseUnit(context, 0, styleResId));
            annotStyle.setRulerTranslateValue(getDefaultRulerTranslateValue(context, 0, styleResId));
            annotStyle.setRulerTranslateUnit(getDefaultRulerTranslateUnit(context, 0, styleResId));
            annotStyle.setRulerPrecision(getDefaultRulerPrecision(context, 0, styleResId));
        } else if (annotStyle.isDateFreeText()) {
            annotStyle.setDateFormat(getDefaultDateFormat(context, 0, styleResId));
        } else if (annotStyle.isRubberStamp()) {
            // first check if it is one of the special stamp
            if (annotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_CHECKMARK_STAMP) {
                annotStyle.setStampId(RubberStampCreate.sCHECK_MARK_LABEL);
            } else if (annotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_CROSS_STAMP) {
                annotStyle.setStampId(RubberStampCreate.sCROSS_LABEL);
            } else if (annotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_DOT_STAMP) {
                annotStyle.setStampId(RubberStampCreate.sDOT_LABEL);
            } else {
                annotStyle.setStampId(RubberStampCreate.sStandardStampPreviewAppearance[0].stampLabel);
            }
        } else if (annotStyle.isSignature()) {
            File[] files = StampManager.getInstance().getSavedSignatures(context);
            if (files != null && files.length > 0) {
                annotStyle.setStampId(files[0].getAbsolutePath());
            }
        }
        annotStyle.setPressureSensitivity(getDefaultPressureSensitivity(context, 0, styleResId));
        return annotStyle;
    }

    /**
     * Gets preset color
     *
     * @param context         the context
     * @param index           index of presets
     * @param attrRes         preset attribute resource, get default attr:{@link #getPresetsAttr(int)}
     * @param arrayRes        array resource, get default array resource: {@link #getDefaultPresetsArrayRes(int)}
     * @param defaultStyleRes default style resource, get default style resource: {@link #getDefaultStyle(int)}
     * @return preset color
     */
    public int getPresetColor(@NonNull Context context, int index, @AttrRes int attrRes, @ArrayRes int arrayRes, @StyleRes int defaultStyleRes) {
        TypedArray typedArray = context.obtainStyledAttributes(new int[]{attrRes});
        int presetArrayRes = typedArray.getResourceId(0, arrayRes);
        typedArray.recycle();

        TypedArray presetsArray = context.getResources().obtainTypedArray(presetArrayRes);
        int styleResId = presetsArray.getResourceId(index, defaultStyleRes);

        presetsArray.recycle();

        return getDefaultColor(context, 0, styleResId);
    }

    /**
     * Gets color key to put in settings
     *
     * @param annotType The annotation mode
     * @param extraTag  extra tag
     * @param mode      mode
     * @return key
     */
    public String getAnnotationPropertySettingsKey(int annotType, String extraTag, String mode) {
        String annotProperty;
        switch (annotType) {
            case Annot.e_Highlight:
                annotProperty = PREF_ANNOTATION_PROPERTY_HIGHLIGHT;
                break;
            case Annot.e_Underline:
                annotProperty = PREF_ANNOTATION_PROPERTY_UNDERLINE;
                break;
            case Annot.e_Link:
                annotProperty = PREF_ANNOTATION_PROPERTY_LINK;
                break;
            case Annot.e_StrikeOut:
                annotProperty = PREF_ANNOTATION_PROPERTY_STRIKEOUT;
                break;
            case Annot.e_Squiggly:
                annotProperty = PREF_ANNOTATION_PROPERTY_SQUIGGLY;
                break;
            case Annot.e_FreeText:
                annotProperty = PREF_ANNOTATION_PROPERTY_FREETEXT;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT:
                annotProperty = PREF_ANNOTATION_PROPERTY_CALLOUT;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_DATE:
                annotProperty = PREF_ANNOTATION_PROPERTY_FREETEXT_DATE;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_SPACING:
                annotProperty = PREF_ANNOTATION_PROPERTY_FREETEXT_SPACING;
                break;
            case Annot.e_Ink:
                annotProperty = PREF_ANNOTATION_PROPERTY_FREEHAND;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW:
                annotProperty = PREF_ANNOTATION_PROPERTY_ARROW;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RULER:
                annotProperty = PREF_ANNOTATION_PROPERTY_RULER;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_PERIMETER_MEASURE:
                annotProperty = PREF_ANNOTATION_PROPERTY_PERIMETER_MEASURE;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_AREA_MEASURE:
                annotProperty = PREF_ANNOTATION_PROPERTY_AREA_MEASURE;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RECT_AREA_MEASURE:
                annotProperty = PREF_ANNOTATION_PROPERTY_RECT_AREA_MEASURE;
                break;
            case Annot.e_Polyline:
                annotProperty = PREF_ANNOTATION_PROPERTY_POLYLINE;
                break;
            case Annot.e_Square:
                annotProperty = PREF_ANNOTATION_PROPERTY_RECTANGLE;
                break;
            case Annot.e_Circle:
                annotProperty = PREF_ANNOTATION_PROPERTY_OVAL;
                break;
            case Annot.e_Polygon:
                annotProperty = PREF_ANNOTATION_PROPERTY_POLYGON;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD:
                annotProperty = PREF_ANNOTATION_PROPERTY_CLOUD;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE:
                annotProperty = PREF_ANNOTATION_PROPERTY_SIGNATURE;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT:
                annotProperty = PREF_ANNOTATION_PROPERTY_COUNT;
                break;
            case Annot.e_Text:
                annotProperty = PREF_ANNOTATION_PROPERTY_NOTE;
                break;
            case Annot.e_Sound:
                annotProperty = PREF_ANNOTATION_PROPERTY_SOUND;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_ERASER:
                annotProperty = PREF_ANNOTATION_PROPERTY_ERASER;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_HIGHLIGHTER:
                annotProperty = PREF_ANNOTATION_PROPERTY_FREE_HIGHLIGHTER;
                break;
            case Annot.e_Redact:
                annotProperty = PREF_ANNOTATION_PROPERTY_REDACTION;
                break;
            case Annot.e_Widget:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_TEXT_FIELD:
                annotProperty = PREF_ANNOTATION_PROPERTY_WIDGET;
                break;
            case AnnotStyle.CUSTOM_SMART_PEN:
                annotProperty = PREF_ANNOTATION_PROPERTY_SMART_PEN;
                break;
            default:
                annotProperty = PREF_ANNOTATION_PROPERTY_LINE;
        }

        return annotProperty + extraTag + mode;
    }

    public static String KEY_PREF_PRESET_INDEX = "preset_index";

    public int getLastSelectedPresetIndex(Context context, int toolbarItemType, String toolbarStyleId) {
        return getDefaultSharedPreferences(context).getInt(lastSelectedPresetIndexKey(toolbarItemType, toolbarStyleId), 0);
    }

    public void setLastSelectedPresetIndex(Context context, int toolbarItemType, String toolbarStyleId, int index) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putInt(lastSelectedPresetIndexKey(toolbarItemType, toolbarStyleId), index);
        editor.apply();
    }

    private String lastSelectedPresetIndexKey(int toolbarItemType, String toolbarStyleId) {
        return KEY_PREF_PRESET_INDEX + "_" + toolbarItemType + "_" + toolbarStyleId;
    }
}
