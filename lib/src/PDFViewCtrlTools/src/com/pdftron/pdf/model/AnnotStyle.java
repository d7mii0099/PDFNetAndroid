//------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------

package com.pdftron.pdf.model;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.SparseArray;
import android.view.Gravity;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.content.ContextCompat;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.annots.FreeText;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.annots.Text;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.controls.AnnotStyleView;
import com.pdftron.pdf.tools.CloudCreate;
import com.pdftron.pdf.tools.Eraser;
import com.pdftron.pdf.tools.SoundCreate;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotationPropertyPreviewView;
import com.pdftron.pdf.utils.MeasureUtils;
import com.pdftron.pdf.utils.UnitConverter;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.toolbar.component.view.ActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * This class contains annotation style information
 */
public class AnnotStyle {

    private static final String KEY_ANNOT_TYPE = "annotType";
    private static final String KEY_THICKNESS = "thickness";
    private static final String KEY_STROKE_COLOR = "strokeColor";
    private static final String KEY_FILL_COLOR = "fillColor";
    private static final String KEY_OPACITY = "opacity";
    private static final String KEY_ICON = "icon";
    private static final String KEY_TEXT_SIZE = "textSize";
    private static final String KEY_TEXT_COLOR = "textColor";
    private static final String KEY_FONT_PATH = "fontPath";
    private static final String KEY_FONT_NAME = "fontName";
    private static final String KEY_PDFTRON_NAME = "pdftronName";
    private static final String KEY_OVERLAY_TEXT = "overlayText";
    public static final String KEY_PDFTRON_RULER = "pdftronRuler";
    public static final String KEY_RULER_BASE = "rulerBase";
    public static final String KEY_RULER_BASE_UNIT = "rulerBaseUnit";
    public static final String KEY_RULER_TRANSLATE = "rulerTranslate";
    public static final String KEY_RULER_TRANSLATE_UNIT = "rulerTranslateUnit";
    private static final String KEY_RULER_PRECISION = "rulerPrecision";
    private static final String KEY_SNAP = "snap";
    private static final String KEY_RICH_CONTENT = "freeTextRC";
    private static final String KEY_ERASER_TYPE = "eraserType";
    private static final String KEY_INK_ERASER_MODE = "inkEraserMode";
    private static final String KEY_DATE_FORMAT = "dateFormat";
    private static final String KEY_PRESSURE_SENSITIVE = "pressureSensitive";
    private static final String KEY_BORDER_STYLE = "borderStyle";
    private static final String KEY_LINE_STYLE = "lineStyle";
    private static final String KEY_LINE_START_STYLE = "lineStartStyle";
    private static final String KEY_LINE_END_STYLE = "lineEndStyle";
    private static final String KEY_STAMP_ID = "stampId";
    private static final String KEY_HORIZONTAL_ALIGNMENT = "horizontal_alignment";
    private static final String KEY_VERTICAL_ALIGNMENT = "vertical_alignment";

    /**
     * The constant represents arrow annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_ARROW = 1001;

    /**
     * The constant represents signature annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_SIGNATURE = 1002;

    /**
     * The constant represents eraser annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_ERASER = 1003;

    /**
     * The constant represents free highlighter annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_FREE_HIGHLIGHTER = 1004;

    /**
     * The constant represents cloud annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_CLOUD = 1005;

    /**
     * The constant represents ruler annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_RULER = 1006;

    /**
     * The constant represents callout annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_CALLOUT = 1007;

    /**
     * The constant represents perimeter annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_PERIMETER_MEASURE = 1008;

    /**
     * The constant represents area annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_AREA_MEASURE = 1009;

    /**
     * The constant represents stretchable free text type
     */
    public static final int CUSTOM_ANNOT_TYPE_FREE_TEXT_SPACING = 1010;

    /**
     * The constant represents a date free text type
     */
    public static final int CUSTOM_ANNOT_TYPE_FREE_TEXT_DATE = 1011;

    /**
     * The constant represents rectangular area annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_RECT_AREA_MEASURE = 1012;

    /**
     * The constant represents image stamp annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_IMAGE_STAMP = 1013;

    /**
     * The constant represents check mark stamp annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_CHECKMARK_STAMP = 1014;

    /**
     * The constant represents cross stamp annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_CROSS_STAMP = 1015;

    /**
     * The constant represents dot stamp annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_DOT_STAMP = 1016;

    /**
     * The constant represents the checkbox field annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_CHECKBOX_FIELD = 1017;

    /**
     * The constant represents the combo box field annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_COMBO_BOX = 1018;

    /**
     * The constant represents the radio button field annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_RADIO_BUTTON = 1019;

    /**
     * The constant represents the text field annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_TEXT_FIELD = 1020;

    /**
     * The constant represents the list box field annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_LIST_BOX = 1021;

    /**
     * The constant represents the signature field annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_SIGNATURE_FIELD = 1022;

    /**
     * The constant represents the pan tool
     */
    public static final int CUSTOM_ANNOT_TYPE_PAN = 1023;

    /**
     * The constant represents the rectangle multi-select tool
     */
    public static final int CUSTOM_ANNOT_TYPE_RECT_MULTI_SELECT = 1024;

    /**
     * The constant represents the lasso multi-select tool
     */
    public static final int CUSTOM_ANNOT_TYPE_LASSO_MULTI_SELECT = 1025;

    /**
     * The constant represents the undo tool
     */
    public static final int CUSTOM_TOOL_UNDO = 1026;

    /**
     * The constant represents the redo button
     */
    public static final int CUSTOM_TOOL_REDO = 1027;

    /**
     * The constant represents the edit toolbar button
     */
    public static final int CUSTOM_EDIT_TOOLBAR = 1028;

    /**
     * The constant represents the area redaction tool
     */
    public static final int CUSTOM_RECT_REDACTION = 1029;

    /**
     * The constant represents the smart pen tool
     */
    public static final int CUSTOM_SMART_PEN = 1030;

    /**
     * The constant represents the add page button
     */
    public static final int CUSTOM_ADD_PAGE = 1031;

    /**
     * The constant represents the page redaction button
     */
    public static final int CUSTOM_PAGE_REDACTION = 1032;

    /**
     * The constant represents the search redaction button
     */
    public static final int CUSTOM_SEARCH_REDACTION = 1033;

    /**
     * The constant represents the rectangle multi-select tool
     */
    public static final int CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT = 1034;

    private float mThickness;
    private float mTextSize;
    private int mTextColor;
    private String mTextContent = "";
    private String mTextHTMLContent = "";
    private @ColorInt
    int mStrokeColor;
    private @ColorInt
    int mFillColor;
    private float mOpacity;
    private String mOverlayText;
    private double mBorderEffectIntensity = CloudCreate.BORDER_INTENSITY;
    private String mIcon = "";
    private String mEraserType = Eraser.EraserType.INK_ERASER.name();
    private String mInkEraserMode = Eraser.InkEraserMode.PIXEL.name();
    private String mDateFormat;
    private float mLetterSpacing;
    private String mBorderStyle = ShapeBorderStyle.DEFAULT.name();
    private String mLineStyle = LineStyle.DEFAULT.name();
    private String mLineStartStyle = LineEndingStyle.NONE.name();
    private String mLineEndStyle = LineEndingStyle.NONE.name();

    private OnAnnotStyleChangeListener mAnnotChangeListener;
    private boolean mUpdateListener = true;
    private ActionButton mPreview;

    private FontResource mFont = new FontResource("");

    private int mHorizontalAlignment = Gravity.NO_GRAVITY;
    private int mVerticalAlignment = Gravity.NO_GRAVITY;

    private int mAnnotType = Annot.e_Unknown;

    private boolean mHasAppearance = true; // assume all annotations has appearance

    private RulerItem mRuler = new RulerItem();
    private RulerItem mRulerCopy;

    private boolean mSnap;
    private boolean mPressureSensitive = false;

    private String mStampId = "";
    private boolean mEnabled = true;

    /**
     * Class constructor
     */
    public AnnotStyle() {
        mEraserType = ToolStyleConfig.getInstance().getDefaultEraserType().name();
    }

    /**
     * Class constructor
     *
     * @param other other annotation style to copy from
     */
    public AnnotStyle(AnnotStyle other) {
        mThickness = other.getThickness();
        mTextSize = other.mTextSize;
        mStrokeColor = other.getColor();
        mFillColor = other.getFillColor();
        mIcon = other.getIcon();
        mOpacity = other.getOpacity();
        mAnnotChangeListener = other.mAnnotChangeListener;
        mUpdateListener = other.mUpdateListener;
        mPreview = other.mPreview;
        mFont = other.getFont();
        mAnnotType = other.getAnnotType();
        mTextColor = other.mTextColor;
        mRuler = other.mRuler;
        mOverlayText = other.mOverlayText;
        mSnap = other.mSnap;
        mStampId = other.mStampId;
        mBorderStyle = other.mBorderStyle;
        mLineStyle = other.mLineStyle;
        mLineStartStyle = other.mLineStartStyle;
        mLineEndStyle = other.mLineEndStyle;
        mHorizontalAlignment = other.mHorizontalAlignment;
        mVerticalAlignment = other.mVerticalAlignment;
    }

    /**
     * Convert annotation style to string in json format
     *
     * @return json format string
     */
    public String toJSONString() {
        try {
            JSONObject object = new JSONObject();
            object.put(KEY_ANNOT_TYPE, String.valueOf(mAnnotType));
            object.put(KEY_THICKNESS, String.valueOf(mThickness));
            object.put(KEY_STROKE_COLOR, mStrokeColor);
            object.put(KEY_FILL_COLOR, mFillColor);
            object.put(KEY_OPACITY, String.valueOf(mOpacity));
            if (hasBorderStyle()) {
                object.put(KEY_BORDER_STYLE, mBorderStyle);
            } else if (hasLineStyle()) {
                object.put(KEY_LINE_STYLE, mLineStyle);
            }
            if (hasLineStartStyle()) {
                object.put(KEY_LINE_START_STYLE, mLineStartStyle);
            }
            if (hasLineEndStyle()) {
                object.put(KEY_LINE_END_STYLE, mLineEndStyle);
            }
            if (hasIcon()) {
                object.put(KEY_ICON, mIcon);
            }
            if (hasTextStyle()) {
                object.put(KEY_TEXT_SIZE, String.valueOf(mTextSize));
                object.put(KEY_TEXT_COLOR, mTextColor);
                object.put(KEY_RICH_CONTENT, mTextHTMLContent);
            }
            if (hasFont()) {
                object.put(KEY_FONT_PATH, mFont.getFilePath());
                object.put(KEY_FONT_NAME, mFont.getFontName());
                object.put(KEY_PDFTRON_NAME, mFont.getPDFTronName());
            }
            if (isMeasurement()) {
                object.put(KEY_RULER_BASE, String.valueOf(mRuler.mRulerBase));
                object.put(KEY_RULER_BASE_UNIT, mRuler.mRulerBaseUnit);
                object.put(KEY_RULER_TRANSLATE, String.valueOf(mRuler.mRulerTranslate));
                object.put(KEY_RULER_TRANSLATE_UNIT, mRuler.mRulerTranslateUnit);
                object.put(KEY_RULER_PRECISION, String.valueOf(mRuler.mPrecision));
                object.put(KEY_SNAP, mSnap);
            }
            if (isRedaction() || isWatermark()) {
                object.put(KEY_OVERLAY_TEXT, mOverlayText);
            }
            if (isEraser()) {
                object.put(KEY_ERASER_TYPE, mEraserType);
                object.put(KEY_INK_ERASER_MODE, mInkEraserMode);
            }
            if (isDateFreeText()) {
                object.put(KEY_DATE_FORMAT, mDateFormat);
            }
            if (hasPressureSensitivity()) {
                object.put(KEY_PRESSURE_SENSITIVE, mPressureSensitive);
            }
            if (hasStampId()) {
                object.put(KEY_STAMP_ID, mStampId);
            }
            if (hasTextAlignment()) {
                object.put(KEY_HORIZONTAL_ALIGNMENT, mHorizontalAlignment);
                object.put(KEY_VERTICAL_ALIGNMENT, mVerticalAlignment);
            }
            return object.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Convert JSON String to annotation style
     *
     * @param jsonStr json string
     * @return annotation style
     */
    public static AnnotStyle loadJSONString(String jsonStr) {
        return loadJSONString(null, jsonStr, -1);
    }

    /**
     * Convert JSON String to annotation style
     *
     * @param context   the context
     * @param jsonStr   json string
     * @param annotType annotation type
     * @return annotation style
     */
    public static AnnotStyle loadJSONString(Context context, String jsonStr, int annotType) {
        AnnotStyle annotStyle = new AnnotStyle();
        if (context != null && annotType > -1) {
            annotStyle = ToolStyleConfig.getInstance().getDefaultAnnotStyle(context, annotType);
        }
        if (!Utils.isNullOrEmpty(jsonStr)) {
            try {
                JSONObject object = new JSONObject(jsonStr);
                if (object.has(KEY_ANNOT_TYPE)) {
                    annotStyle.setAnnotType(Integer.valueOf(object.getString(KEY_ANNOT_TYPE)));
                }
                if (object.has(KEY_THICKNESS)) {
                    annotStyle.setThickness(Float.valueOf(object.getString(KEY_THICKNESS)));
                }
                if (object.has(KEY_STROKE_COLOR)) {
                    annotStyle.setStrokeColor(object.getInt(KEY_STROKE_COLOR));
                }
                if (object.has(KEY_FILL_COLOR)) {
                    annotStyle.setFillColor(object.getInt(KEY_FILL_COLOR));
                }
                if (object.has(KEY_OPACITY)) {
                    annotStyle.setOpacity(Float.valueOf(object.getString(KEY_OPACITY)));
                }
                if (object.has(KEY_TEXT_SIZE)) {
                    annotStyle.setTextSize(Float.valueOf(object.getString(KEY_TEXT_SIZE)));
                }
                if (object.has(KEY_TEXT_COLOR)) {
                    annotStyle.setTextColor(object.getInt(KEY_TEXT_COLOR));
                }
                if (object.has(KEY_RICH_CONTENT)) {
                    annotStyle.setTextHTMLContent(object.getString(KEY_RICH_CONTENT));
                }
                if (object.has(KEY_BORDER_STYLE)) {
                    annotStyle.setBorderStyle(ShapeBorderStyle.valueOf(object.getString(KEY_BORDER_STYLE)));
                } else if (object.has(KEY_LINE_STYLE)) {
                    annotStyle.setLineStyle(LineStyle.valueOf(object.getString(KEY_LINE_STYLE)));
                }
                if (object.has(KEY_LINE_START_STYLE)) {
                    annotStyle.setLineStartStyle(LineEndingStyle.valueOf(object.getString(KEY_LINE_START_STYLE)));
                }
                if (object.has(KEY_LINE_END_STYLE)) {
                    annotStyle.setLineEndStyle(LineEndingStyle.valueOf(object.getString(KEY_LINE_END_STYLE)));
                }
                if (object.has(KEY_ICON)) {
                    String icon = object.getString(KEY_ICON);
                    if (!Utils.isNullOrEmpty(icon)) {
                        annotStyle.setIcon(icon);
                    }
                }
                if (object.has(KEY_FONT_NAME)) {
                    String fontName = object.getString(KEY_FONT_NAME);
                    String fontPath = object.has(KEY_FONT_PATH) ? object.getString(KEY_FONT_PATH) : null;
                    String pdftronName = object.has(KEY_PDFTRON_NAME) ? object.getString(KEY_PDFTRON_NAME) : null;
                    if (!Utils.isNullOrEmpty(fontName) || !Utils.isNullOrEmpty(pdftronName)) {
                        FontResource f = new FontResource(fontName);
                        annotStyle.setFont(f);
                        if (object.has(KEY_FONT_PATH)) {
                            if (!Utils.isNullOrEmpty(fontPath)) {
                                f.setFilePath(fontPath);
                            }
                        }
                        if (object.has(KEY_PDFTRON_NAME)) {
                            if (!Utils.isNullOrEmpty(pdftronName)) {
                                f.setPDFTronName(pdftronName);
                                if (!f.hasFontName()) {
                                    f.setFontName(pdftronName);
                                }
                            }
                        }
                    }
                }
                if (object.has(KEY_RULER_BASE) &&
                        object.has(KEY_RULER_BASE_UNIT) &&
                        object.has(KEY_RULER_TRANSLATE) &&
                        object.has(KEY_RULER_TRANSLATE_UNIT) &&
                        object.has(KEY_RULER_PRECISION)) {
                    annotStyle.setRulerBaseValue(Float.valueOf(object.getString(KEY_RULER_BASE)));
                    annotStyle.setRulerBaseUnit(object.getString(KEY_RULER_BASE_UNIT));
                    annotStyle.setRulerTranslateValue(Float.valueOf(object.getString(KEY_RULER_TRANSLATE)));
                    annotStyle.setRulerTranslateUnit(object.getString(KEY_RULER_TRANSLATE_UNIT));
                    annotStyle.setRulerPrecision(Integer.valueOf(object.getString(KEY_RULER_PRECISION)));
                }
                if (object.has(KEY_SNAP)) {
                    annotStyle.setSnap(object.getBoolean(KEY_SNAP));
                }
                if (object.has(KEY_OVERLAY_TEXT)) {
                    annotStyle.setOverlayText(object.getString(KEY_OVERLAY_TEXT));
                }
                if (object.has(KEY_ERASER_TYPE)) {
                    annotStyle.setEraserType(Eraser.EraserType.valueOf(object.getString(KEY_ERASER_TYPE)));
                }
                if (object.has(KEY_INK_ERASER_MODE)) {
                    annotStyle.setInkEraserMode(Eraser.InkEraserMode.valueOf(object.getString(KEY_INK_ERASER_MODE)));
                }
                if (object.has(KEY_DATE_FORMAT)) {
                    annotStyle.setDateFormat(object.getString(KEY_DATE_FORMAT));
                }
                if (object.has(KEY_PRESSURE_SENSITIVE)) {
                    annotStyle.setPressureSensitivity(object.getBoolean(KEY_PRESSURE_SENSITIVE));
                }
                if (object.has(KEY_STAMP_ID)) {
                    annotStyle.setStampId(object.getString(KEY_STAMP_ID));
                }
                if (object.has(KEY_HORIZONTAL_ALIGNMENT)) {
                    annotStyle.setHorizontalAlignment(object.getInt(KEY_HORIZONTAL_ALIGNMENT));
                }
                if (object.has(KEY_VERTICAL_ALIGNMENT)) {
                    annotStyle.setVerticalAlignment(object.getInt(KEY_VERTICAL_ALIGNMENT));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e, "Failed converting annotStype from json to object");
            }
        }

        return annotStyle;
    }

    /**
     * Sets annotation type, can be obtained from {@link Annot#getType()}
     *
     * @param annotType annotation type
     */
    public void setAnnotType(int annotType) {
        mAnnotType = annotType;
    }

    /**
     * Sets annotation style
     *
     * @param strokeColor stroke color
     * @param fillColor   fill color
     * @param thickness   stroke thickness
     * @param opacity     opacity
     */
    public void setStyle(@ColorInt int strokeColor, @ColorInt int fillColor, float thickness, float opacity) {
        mStrokeColor = strokeColor;
        mFillColor = fillColor;
        mThickness = thickness;
        mOpacity = opacity;
        updatePreviewStyle();
    }

    /**
     * Copy other annotation style
     *
     * @param other other annotation style
     */
    public void setStyle(AnnotStyle other) {
        setStrokeColor(other.getColor());
        setFillColor(other.getFillColor());
        setThickness(other.getThickness());
        setOpacity(other.getOpacity());
        setIcon(other.getIcon());
        setFont(other.getFont());
        setTextSize(other.getTextSize());
        setTextColor(other.getTextColor());
        setBorderStyle(other.getBorderStyle());
        setLineStyle(other.getLineStyle());
        setLineStartStyle(other.getLineStartStyle());
        setLineEndStyle(other.getLineEndStyle());
        setHorizontalAlignment(other.getHorizontalAlignment());
        setVerticalAlignment(other.getVerticalAlignment());
    }

    /**
     * Sets stroke color.
     * For normal annotation, stroke color can be obtained from {@link  Annot#getColorAsRGB()};
     * For {@link com.pdftron.pdf.annots.FreeText} annotation, stroke color can be obtained from {@link  FreeText#getLineColor()}
     *
     * @param strokeColor stroke color
     */
    public void setStrokeColor(@ColorInt int strokeColor) {
        boolean update = strokeColor != mStrokeColor;
        mStrokeColor = strokeColor;
        updateStrokeColorListener(strokeColor, update);
        updatePreviewStyle();
    }

    /**
     * Sets fill color.
     * For {@link com.pdftron.pdf.annots.Markup} annotation, fill color can be obtained from {@link  Markup#getInteriorColor()};
     * For {@link FreeText} annotation, fill color can be obtained from {@link  FreeText#getColorAsRGB()}
     *
     * @param fillColor fill color
     */
    public void setFillColor(@ColorInt int fillColor) {
        boolean update = fillColor != mFillColor;
        mFillColor = fillColor;
        updateFillColorListener(fillColor, update);
        updatePreviewStyle();
    }

    /**
     * Sets stroke thickness.
     * Annotation stroke thickness can be obtained from {@link  Annot.BorderStyle#getWidth()}
     *
     * @param thickness border style thickness
     */
    public void setThickness(float thickness) {
        setThickness(thickness, true);
    }

    /**
     * Sets stroke thickness.
     * Annotation stroke thickness can be obtained from {@link  Annot.BorderStyle#getWidth()}
     *
     * @param thickness border style thickness
     * @param done      done sliding
     */
    public void setThickness(float thickness, boolean done) {
        boolean update = mThickness != thickness;
        mThickness = thickness;
        updateThicknessListener(thickness, update, done);
        updatePreviewStyle();
    }

    /**
     * Sets border style.
     *
     * @param borderStyle border style
     */
    public void setBorderStyle(ShapeBorderStyle borderStyle) {
        boolean update = ShapeBorderStyle.valueOf(mBorderStyle) != borderStyle;
        mBorderStyle = borderStyle.name();
        updateBorderStyleListener(borderStyle, update);
        updatePreviewStyle();
    }

    public ShapeBorderStyle getBorderStyle() {
        return ShapeBorderStyle.valueOf(mBorderStyle);
    }

    /**
     * Sets line style.
     *
     * @param lineStyle border style
     */
    public void setLineStyle(LineStyle lineStyle) {
        boolean update = LineStyle.valueOf(mLineStyle) != lineStyle;
        mLineStyle = lineStyle.name();
        updateLineStyleListener(lineStyle, update);
        updatePreviewStyle();
    }

    public LineStyle getLineStyle() {
        return LineStyle.valueOf(mLineStyle);
    }

    /**
     * Sets line start style.
     *
     * @param lineStartStyle line start style
     */
    public void setLineStartStyle(LineEndingStyle lineStartStyle) {
        boolean update = LineEndingStyle.valueOf(mLineStartStyle) != lineStartStyle;
        mLineStartStyle = lineStartStyle.name();
        updateLineStartStyleListener(lineStartStyle, update);
        updatePreviewStyle();
    }

    public LineEndingStyle getLineStartStyle() {
        return LineEndingStyle.valueOf(mLineStartStyle);
    }

    /**
     * Sets line ending style.
     *
     * @param lineEndStyle line ending style
     */
    public void setLineEndStyle(LineEndingStyle lineEndStyle) {
        boolean update = LineEndingStyle.valueOf(mLineEndStyle) != lineEndStyle;
        mLineEndStyle = lineEndStyle.name();
        updateLineEndStyleListener(lineEndStyle, update);
        updatePreviewStyle();
    }

    public LineEndingStyle getLineEndStyle() {
        return LineEndingStyle.valueOf(mLineEndStyle);
    }

    /**
     * Sets text size for {@link FreeText} annotation.
     * text size can be obtained from {@link  FreeText#getFontSize()}
     *
     * @param textSize text size
     */
    public void setTextSize(float textSize) {
        setTextSize(textSize, true);
    }

    /**
     * Sets text size for {@link FreeText} annotation.
     * text size can be obtained from {@link  FreeText#getFontSize()}
     *
     * @param textSize text size
     * @param done     done sliding
     */
    public void setTextSize(float textSize, boolean done) {
        boolean update = mTextSize != textSize;
        mTextSize = textSize;
        updateTextSizeListener(textSize, update, done);
        updatePreviewStyle();
    }

    /**
     * Sets text color for {@link FreeText} and {@link com.pdftron.pdf.annots.Widget} annotations.
     * text color can be obtained from {@link  FreeText#getTextColor()} or
     * {@link  com.pdftron.pdf.annots.Widget#getTextColor()}
     *
     * @param textColor text color
     */
    public void setTextColor(@ColorInt int textColor) {
        boolean update = mTextColor != textColor;
        mTextColor = textColor;
        updateTextColorListener(textColor, update);
        updatePreviewStyle();
    }

    /**
     * Sets content of {@link FreeText} annotation.
     *
     * @param content content
     */
    public void setTextContent(String content) {
        if (null != content) {
            mTextContent = content;
        }
    }

    public void setTextHTMLContent(String html) {
        if (null != html) {
            mTextHTMLContent = html;
        }
        if (Utils.isNullOrEmpty(html)) {
            updateRichContentEnabledListener(false);
        } else {
            updateRichContentEnabledListener(true);
        }
    }

    /**
     * Sets opacity for {@link Markup} annotation.
     * Opacity can be obtained from {@link Markup#getOpacity()}
     *
     * @param opacity opacity
     */
    public void setOpacity(float opacity) {
        setOpacity(opacity, true);
    }

    /**
     * Sets opacity for {@link Markup} annotation.
     * Opacity can be obtained from {@link Markup#getOpacity()}
     *
     * @param opacity opacity
     * @param done    done sliding
     */
    public void setOpacity(float opacity, boolean done) {
        boolean update = opacity != mOpacity;
        mOpacity = opacity;
        updateOpacityListener(opacity, update, done);
        updatePreviewStyle();
    }

    /**
     * Gets overlay text. Used for {@link com.pdftron.pdf.annots.Redaction} annotation and
     * {@link com.pdftron.pdf.annots.Watermark}
     *
     * @return overlay text
     */
    public String getOverlayText() {
        return mOverlayText;
    }

    /**
     * Sets overlay text. Used for {@link com.pdftron.pdf.annots.Redaction} annotation and
     * {@link com.pdftron.pdf.annots.Watermark}
     *
     * @param text overlay text
     */
    public void setOverlayText(String text) {
        updateOverlayTextListener(text);
        mOverlayText = text;
    }

    /**
     * Sets snap.
     *
     * @param snap whether to snap to points
     */
    public void setSnap(boolean snap) {
        if (isMeasurement()) {
            updateSnapListener(snap);
        }
        mSnap = snap;
    }

    /**
     * Sets eraser type
     *
     * @param type the eraser type
     */
    public void setEraserType(@NonNull Eraser.EraserType type) {
        mEraserType = type.name();
    }

    /**
     * Sets the ink eraser mode
     *
     * @param mode the eraser mode
     */
    public void setInkEraserMode(@NonNull Eraser.InkEraserMode mode) {
        mInkEraserMode = mode.name();
    }

    /**
     * Sets whether pressure sensitivity is enabled.
     *
     * @param hasPressure whether to enable pressure sensitivity.
     */
    public void setPressureSensitivity(boolean hasPressure) {
        mPressureSensitive = hasPressure;
    }

    /**
     * Sets the identifier of stamp or signature.
     *
     * @param stampId the unique identifier
     */
    public void setStampId(@NonNull String stampId) {
        mStampId = stampId;
    }

    /**
     * Sets the border effect intensity
     *
     * @param intensity border effect intensity
     */
    public void setBorderEffectIntensity(double intensity) {
        mBorderEffectIntensity = intensity;
    }

    /**
     * Gets the border effect intensity
     *
     * @return border effect intensity
     */
    public double getBorderEffectIntensity() {
        return mBorderEffectIntensity;
    }

    /**
     * Sets ruler properties
     *
     * @param ruler the ruler
     */
    public void setRulerItem(RulerItem ruler) {
        mRuler = ruler;
    }

    /**
     * Sets ruler base value
     *
     * @param value the value
     */
    public void setRulerBaseValue(float value) {
        updateRulerBaseValueListener(value);
        mRuler.mRulerBase = value;
    }

    /**
     * Sets ruler translate value
     *
     * @param value the value
     */
    public void setRulerTranslateValue(float value) {
        updateRulerTranslateValueListener(value);
        mRuler.mRulerTranslate = value;
    }

    /**
     * Sets ruler base unit
     *
     * @param unit the unit
     */
    public void setRulerBaseUnit(String unit) {
        updateRulerBaseUnitListener(unit);
        mRuler.mRulerBaseUnit = unit;
    }

    /**
     * Sets ruler translate unit
     *
     * @param unit the unit
     */
    public void setRulerTranslateUnit(String unit) {
        updateRulerTranslateUnitListener(unit);
        mRuler.mRulerTranslateUnit = unit;
    }

    public void setRulerPrecision(int precision) {
        updateRulerPrecisionListener(precision);
        mRuler.mPrecision = precision;
    }

    /**
     * Gets the ruler item
     *
     * @return the ruler item
     */
    public RulerItem getRulerItem() {
        return mRuler;
    }

    /**
     * Gets the ruler base value
     *
     * @return the value
     */
    public float getRulerBaseValue() {
        return mRuler.mRulerBase;
    }

    /**
     * Gets the ruler translate value
     *
     * @return the value
     */
    public float getRulerTranslateValue() {
        return mRuler.mRulerTranslate;
    }

    /**
     * Gets the ruler base unit
     *
     * @return the unit
     */
    public String getRulerBaseUnit() {
        // legacy check
        if (mRuler.mRulerBaseUnit.equals(UnitConverter.INCH)) { // legacy
            return MeasureUtils.U_IN;
        }
        return mRuler.mRulerBaseUnit;
    }

    /**
     * Gets the ruler translate unit
     *
     * @return the unit
     */
    public String getRulerTranslateUnit() {
        // legacy check
        if (mRuler.mRulerTranslateUnit.equals(UnitConverter.INCH)) { // legacy
            return MeasureUtils.U_IN;
        } else if (mRuler.mRulerTranslateUnit.equals(UnitConverter.YARD)) { // legacy
            return MeasureUtils.U_YD;
        }
        return mRuler.mRulerTranslateUnit;
    }

    public int getPrecision() {
        return mRuler.mPrecision;
    }

    /**
     * @hide
     */
    public void setHasAppearance(boolean hasAppearance) {
        mHasAppearance = hasAppearance;
    }

    /**
     * @hide
     */
    public boolean hasAppearance() {
        return mHasAppearance;
    }

    /**
     * Sets icon for {@link com.pdftron.pdf.annots.Text} annotation.
     * Icon can be obtained from {@link Text#getIconName()}
     *
     * @param icon icon
     */
    public void setIcon(String icon) {
        if (hasIcon() && !Utils.isNullOrEmpty(icon)) {
            updateIconListener(icon);
            mIcon = icon;
            updatePreviewStyle();
        }
    }

    /**
     * Sets font for {@link com.pdftron.pdf.annots.FreeText} annotation.
     * Font can be obtained from calling
     * <pre>
     * {@code
     *     Obj freeTextObj = freeText.getSDFObj();
     *     Obj drDict = freeTextObj.findObj("DR");
     *
     *     if (drDict != null && drDict.isDict()) {
     *       Obj fontDict = drDict.findObj("Font");
     *
     *       if (fontDict != null && fontDict.isDict()) {
     *         DictIterator fItr = fontDict.getDictIterator();
     *
     *         if (fItr.hasNext()) {
     *           Font f = new Font(fItr.value());
     *           String fontName = f.getName();
     *           FontResource font = new FontResource(fontName);
     *         }
     *       }
     *     }
     * }
     * </pre>
     *
     * @param font font
     */
    public void setFont(FontResource font) {
        updateFontListener(font);
        mFont = font;
        updatePreviewStyle();
    }

    public void setHorizontalAlignment(int alignment) {
        boolean update = alignment != mHorizontalAlignment;
        mHorizontalAlignment = alignment;
        updateTextAlignment(alignment, mVerticalAlignment, update);
    }

    public int getHorizontalAlignment() {
        return mHorizontalAlignment;
    }

    public void setVerticalAlignment(int alignment) {
        boolean update = alignment != mVerticalAlignment;
        mVerticalAlignment = alignment;
        updateTextAlignment(mHorizontalAlignment, alignment, update);
    }

    public int getVerticalAlignment() {
        return mVerticalAlignment;
    }

    public void setDateFormat(String format) {
        updateDateFormatListener(format);
        mDateFormat = format;
    }

    public String getDateFormat() {
        return mDateFormat;
    }

    public void setLetterSpacing(float spacing) {
        mLetterSpacing = spacing;
    }

    public float getLetterSpacing() {
        return mLetterSpacing;
    }

    /**
     * Disable update annotation change listener
     *
     * @param disable true then disable, false otherwise
     */
    public void disableUpdateListener(boolean disable) {
        mUpdateListener = !disable;
    }

    /**
     * Gets color of annotation style, for annotation style has border, it means stroke color
     *
     * @return color
     */
    public int getColor() {
        return mStrokeColor;
    }

    /**
     * Gets stroke thickness of annotation style
     *
     * @return thickness
     */
    public float getThickness() {
        return mThickness;
    }

    /**
     * Gets text size of free text annotation style
     *
     * @return text size
     */
    public float getTextSize() {
        return mTextSize;
    }

    /**
     * Gets text content of free text annotation
     *
     * @return content
     */
    public String getTextContent() {
        return mTextContent;
    }

    public String getTextHTMLContent() {
        return mTextHTMLContent;
    }

    /**
     * Gets text color of free text annotation style
     *
     * @return text color
     */
    public int getTextColor() {
        return mTextColor;
    }

    /**
     * Gets fill color of annotation style
     *
     * @return fill color
     */
    public int getFillColor() {
        return mFillColor;
    }

    /**
     * Gets opacity of annotation style
     *
     * @return opacity
     */
    public float getOpacity() {
        return mOpacity;
    }

    /**
     * Gets Icon for StickyNote {@link Text} annotation
     *
     * @return icon name
     */
    public String getIcon() {
        return mIcon;
    }

    /**
     * Gets font for free text annotation
     *
     * @return font resource
     */
    public FontResource getFont() {
        return mFont;
    }

    /**
     * Gets whether snap to points
     *
     * @return true if snap to points, false otherwise
     */
    public boolean getSnap() {
        return mSnap;
    }

    /**
     * Gets the eraser type
     *
     * @return the eraser type
     */
    public Eraser.EraserType getEraserType() {
        return Eraser.EraserType.valueOf(mEraserType);
    }

    /**
     * Gets the ink eraser mode
     *
     * @return the ink eraser mode
     */
    public Eraser.InkEraserMode getInkEraserMode() {
        return Eraser.InkEraserMode.valueOf(mInkEraserMode);
    }

    /**
     * Gets the identifier of stamp or signature.
     */
    public String getStampId() {
        return mStampId;
    }

    /**
     * Gets whether pressure sensitivity is used.
     *
     * @return true if pressure sensitivity is used.
     */
    public boolean getPressureSensitive() {
        return mPressureSensitive;
    }

    /**
     * Whether annotation style should have pressure sensitivity setting.
     *
     * @return true if annotation style should have pressure sensitivity setting.
     */
    public boolean hasPressureSensitivity() {
        switch (mAnnotType) {
            case Annot.e_Ink:
                return true;
            default:
                return false;
        }
    }

    /**
     * Gets PDFTron font name for free text annotation.
     * This is equivalent to calling {@code annotStyle.getFont().getPDFTronName(); }
     *
     * @return font pdftron font name
     */
    public String getPDFTronFontName() {
        if (mFont != null) {
            return mFont.getPDFTronName();
        }
        return null;
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    public int getAnnotType() {
        return mAnnotType;
    }

    /**
     * Whether annotation style has border thickness
     *
     * @return true then annotation has border thickness, false otherwise
     */
    public boolean hasThickness() {
        switch (mAnnotType) {
            case Annot.e_Highlight:
            case Annot.e_Text:
            case Annot.e_Sound:
            case Annot.e_Redact:
            case Annot.e_Watermark:
            case Annot.e_Widget:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_TEXT_FIELD:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT:
                return false;
            default:
                return true;
        }
    }

    /**
     * Whether annotation style has fill color
     *
     * @return true then annotation has fill color, false otherwise
     */
    public boolean hasFillColor() {
        switch (mAnnotType) {
            case Annot.e_Circle:
            case Annot.e_Square:
            case Annot.e_Polygon:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD:
            case Annot.e_FreeText:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_DATE:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_SPACING:
            case Annot.e_Redact:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style has color
     *
     * @return true then annotation has color, false otherwise
     */
    public boolean hasColor() {
        switch (mAnnotType) {
            case CUSTOM_ANNOT_TYPE_ERASER:
            case Annot.e_Watermark:
            case Annot.e_Widget:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_TEXT_FIELD:
                return false;
            default:
                return true;
        }
    }

    /**
     * Whether annotation style has opacity
     *
     * @return true then annotation has opacity, false otherwise
     */
    public boolean hasOpacity() {
        switch (mAnnotType) {
            case CUSTOM_ANNOT_TYPE_ERASER:
            case CUSTOM_ANNOT_TYPE_SIGNATURE:
            case Annot.e_Link:
            case Annot.e_Sound:
            case Annot.e_Widget:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_TEXT_FIELD:
                return false;
            default:
                return true;
        }
    }

    /**
     * Whether annotation style has icon
     *
     * @return true then annotation has icon, false otherwise
     */
    public boolean hasIcon() {
        switch (mAnnotType) {
            case Annot.e_Text:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT:
            case Annot.e_Sound:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style is sticky note
     *
     * @return true then annotation is sticky note, false otherwise
     */
    public boolean isStickyNote() {
        switch (mAnnotType) {
            case Annot.e_Text:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style is count measurement
     *
     * @return true then annotation is count measurement, false otherwise
     */
    public boolean isCountMeasurement() {
        return mAnnotType == AnnotStyle.CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT;
    }

    /**
     * Whether annotation style is sound
     *
     * @return true then annotation is sound, false otherwise
     */
    public boolean isSound() {
        switch (mAnnotType) {
            case Annot.e_Sound:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style is freetext annotation but not callout or contains rich content
     *
     * @return true then annotation is freetext annotation but not callout or contains rich content, false otherwise
     */
    public boolean isBasicFreeText() {
        if (isRCFreeText()) {
            return false;
        }
        switch (mAnnotType) {
            case Annot.e_FreeText:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style is freetext annotation
     *
     * @return true then annotation is freetext annotation, false otherwise
     */
    public boolean isFreeText() {
        switch (mAnnotType) {
            case Annot.e_FreeText:
            case CUSTOM_ANNOT_TYPE_CALLOUT:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style is square annotation
     *
     * @return true then annotation is square annotation, false otherwise
     */
    public boolean isSquare() {
        switch (mAnnotType) {
            case Annot.e_Square:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style is callout annotation
     *
     * @return true then annotation is callout annotation, false otherwise
     */
    public boolean isCallout() {
        switch (mAnnotType) {
            case CUSTOM_ANNOT_TYPE_CALLOUT:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style is free text with rich content
     *
     * @return true then annotation is free text with rich content, false otherwise
     */
    public boolean isRCFreeText() {
        if (!isFreeText() || isCallout()) {
            return false;
        }
        return !Utils.isNullOrEmpty(mTextHTMLContent);
    }

    /**
     * Whether annotation style is free text with spacing
     *
     * @return true then annotation is free text with spacing, false otherwise
     */
    public boolean isSpacingFreeText() {
        return mAnnotType == CUSTOM_ANNOT_TYPE_FREE_TEXT_SPACING;
    }

    /**
     * Whether annotation style is free text with date
     *
     * @return true if annotation is free text with date, false otherwise
     */
    public boolean isDateFreeText() {
        return mAnnotType == AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_DATE;
    }

    /**
     * Whether annotation style is Widget annotation
     *
     * @return true then annotation is Widget annotation, false otherwise
     */
    public boolean isWidget() {
        switch (mAnnotType) {
            case Annot.e_Widget:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_TEXT_FIELD:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style should have font.
     *
     * @return true if annotation style should have font information, false otherwise
     */
    public boolean hasFont() {
        switch (mAnnotType) {
            case Annot.e_Widget:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_TEXT_FIELD:
            case Annot.e_FreeText:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_DATE:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_SPACING:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style should have text alignment options
     *
     * @return true if annotation style should have text alignment options, false otherwise
     */
    public boolean hasTextAlignment() {
        if (isRCFreeText()) {
            return false;
        }
        switch (mAnnotType) {
            case Annot.e_FreeText:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style should have text style (i.e. text color and text size).
     *
     * @return true if annotation style should have text style, false otherwise
     */
    public boolean hasTextStyle() {
        switch (mAnnotType) {
            case Annot.e_Widget:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_TEXT_FIELD:
            case Annot.e_FreeText:
            case Annot.e_Watermark:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_DATE:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_SPACING:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style is rubber stamp annotation
     *
     * @return true then annotation is rubber stamp annotation, false otherwise
     */
    public boolean isRubberStamp() {
        switch (mAnnotType) {
            case Annot.e_Stamp:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CHECKMARK_STAMP:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CROSS_STAMP:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_DOT_STAMP:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style is signature annotation
     *
     * @return true then annotation is signature annotation, false otherwise
     */
    public boolean isSignature() {
        return mAnnotType == AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE;
    }

    /**
     * Whether annotation style should have stamp id
     *
     * @return true if annotation style should have stamp id, false otherwise
     */
    public boolean hasStampId() {
        switch (mAnnotType) {
            case Annot.e_Stamp:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style is line annotation
     *
     * @return true then annotation is line annotation, false otherwise
     */
    public boolean isLine() {
        switch (mAnnotType) {
            case Annot.e_Line:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style is ruler
     *
     * @return true then annotation is line annotation, false otherwise
     */
    public boolean isRuler() {
        switch (mAnnotType) {
            case CUSTOM_ANNOT_TYPE_RULER:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style is measurement annotation
     *
     * @return true if annotation is any of distance/perimeter/area, false otherwise
     */
    public boolean isMeasurement() {
        switch (mAnnotType) {
            case CUSTOM_ANNOT_TYPE_RULER:
            case CUSTOM_ANNOT_TYPE_PERIMETER_MEASURE:
            case CUSTOM_ANNOT_TYPE_AREA_MEASURE:
            case CUSTOM_ANNOT_TYPE_RECT_AREA_MEASURE:
            case CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style is redaction
     *
     * @return true then annotation is redaction annotation, false otherwise
     */
    public boolean isRedaction() {
        switch (mAnnotType) {
            case Annot.e_Redact:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style is watermark
     *
     * @return true if annotation is watermark, false otherwise
     */
    public boolean isWatermark() {
        switch (mAnnotType) {
            case Annot.e_Watermark:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style base is FreeText
     * This can mean any variation of FreeText
     *
     * @return true if annotation is a variant of FreeText, false otherwise
     */
    public boolean isFreeTextGroup() {
        return isFreeTextGroup(mAnnotType);
    }

    /**
     * Whether annotation type is any variant of FreeText
     *
     * @param annotType the annotation type
     * @return true if annotation type is a variant of FreeText, false otherwise
     */
    public static boolean isFreeTextGroup(int annotType) {
        switch (annotType) {
            case Annot.e_FreeText:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_DATE:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_SPACING:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether style is eraser
     *
     * @return true if is eraser, false otherwise
     */
    public boolean isEraser() {
        return mAnnotType == AnnotStyle.CUSTOM_ANNOT_TYPE_ERASER;
    }

    public boolean hasBorderStyleWithoutCloud() {
        switch (mAnnotType) {
            case Annot.e_Circle:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RECT_AREA_MEASURE:
                return true;
            default:
                return false;
        }
    }

    public boolean hasBorderStyle() {
        switch (mAnnotType) {
            case Annot.e_Square:
            case Annot.e_Circle:
            case Annot.e_Polygon:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_AREA_MEASURE:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RECT_AREA_MEASURE:
                return true;
            default:
                return false;
        }
    }

    public boolean hasLineStyle() {
        switch (mAnnotType) {
            case Annot.e_Line:
            case Annot.e_Polyline:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RULER:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_PERIMETER_MEASURE:
                return true;
            default:
                return false;
        }
    }

    public boolean hasLineStartStyle() {
        switch (mAnnotType) {
            case Annot.e_Line:
            case Annot.e_Polyline:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RULER:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_PERIMETER_MEASURE:
                return true;
            default:
                return false;
        }
    }

    public boolean hasLineEndStyle() {
        switch (mAnnotType) {
            case Annot.e_Line:
            case Annot.e_Polyline:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RULER:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_PERIMETER_MEASURE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Gets font path of font.
     * This is equivalent to calling {@code annotStyle.getFont().getFontPath();}
     *
     * @return font path
     */
    public String getFontPath() {
        if (mFont != null) {
            return mFont.getFilePath();
        }

        return null;
    }

    /**
     * Gets icon drawable
     *
     * @param context The context to get resource
     * @return drawable
     */
    public Drawable getIconDrawable(Context context) {
        return getIconDrawable(context, mIcon, mStrokeColor, mOpacity);
    }

    /**
     * Gets icon drawable based on icon name, opacity
     *
     * @param context     The context to get resources
     * @param iconOutline name of icon outline
     * @param iconFill    name of icon fill
     * @param opacity     opacity of icon. from [0, 1]
     * @return drawable
     */
    public static Drawable getIconDrawable(Context context, String iconOutline, String iconFill, int color, float opacity) {
        int alpha = (int) (255 * opacity);
        int iconOutlineID = context.getResources().getIdentifier(iconOutline, "drawable", context.getPackageName());
        int iconFillID = context.getResources().getIdentifier(iconFill, "drawable", context.getPackageName());
        if (iconOutlineID != 0 && iconFillID != 0) {
            try {
                Drawable[] layers = new Drawable[2];
                layers[0] = ContextCompat.getDrawable(context, iconFillID);
                layers[0].mutate();
                layers[0].setAlpha(alpha);
                layers[0].setColorFilter(color, PorterDuff.Mode.SRC_IN);
                layers[1] = ContextCompat.getDrawable(context, iconOutlineID);
                layers[1].mutate();
                layers[1].setAlpha(alpha);
                LayerDrawable layerDrawable = new LayerDrawable(layers);
                return layerDrawable;
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e, iconFillID + ", " + iconOutlineID);
            }
        }
        return null;
    }

    /**
     * Gets icon drawable based on icon name, opacity
     *
     * @param context The context to get resources
     * @param icon    icon name
     * @param opacity opacity of icon. from [0, 1]
     * @return drawable
     */
    public static Drawable getIconDrawable(Context context, String icon, int color, float opacity) {
        String iconOutline, iconFill;
        if (icon.equals(SoundCreate.SOUND_ICON)) {
            iconOutline = AnnotStyleView.SOUND_ICON_OUTLINE;
            iconFill = AnnotStyleView.SOUND_ICON_FILL;
        } else {
            iconOutline = com.pdftron.pdf.tools.Tool.ANNOTATION_NOTE_ICON_FILE_PREFIX + icon.toLowerCase() + Tool.ANNOTATION_NOTE_ICON_FILE_POSTFIX_OUTLINE;
            iconFill = Tool.ANNOTATION_NOTE_ICON_FILE_PREFIX + icon.toLowerCase() + Tool.ANNOTATION_NOTE_ICON_FILE_POSTFIX_FILL;
        }
        return getIconDrawable(context, iconOutline, iconFill, color, opacity);
    }

    @Override
    public String toString() {
        return "AnnotStyle{" +
                "mThickness=" + mThickness +
                ", mStrokeColor=" + mStrokeColor +
                ", mFillColor=" + mFillColor +
                ", mOpacity=" + mOpacity +
                ", mIcon='" + mIcon + '\'' +
                ", mFont=" + mFont.toString() +
                ", mRuler=" + mRuler.toString() +
                '}';
    }

    /**
     * Sets annotation style change listener
     *
     * @param listener annotation style change listener
     */
    public void setAnnotAppearanceChangeListener(OnAnnotStyleChangeListener listener) {
        mAnnotChangeListener = listener;
    }

    /**
     * Update thickness to listener
     *
     * @param thickness thickness
     * @param update    whether to invoke listener method
     */
    public void updateThicknessListener(float thickness, boolean update, boolean done) {
        if (mUpdateListener && mAnnotChangeListener != null && (update || done)) {
            mAnnotChangeListener.onChangeAnnotThickness(thickness, done);
        }
    }

    public void updateBorderStyleListener(ShapeBorderStyle borderStyle, boolean update) {
        if (mUpdateListener && mAnnotChangeListener != null && update) {
            mAnnotChangeListener.onChangeAnnotBorderStyle(borderStyle);
        }
    }

    public void updateLineStyleListener(LineStyle lineStyle, boolean update) {
        if (mUpdateListener && mAnnotChangeListener != null && update) {
            mAnnotChangeListener.onChangeAnnotLineStyle(lineStyle);
        }
    }

    public void updateLineStartStyleListener(LineEndingStyle lineStartStyle, boolean update) {
        if (mUpdateListener && mAnnotChangeListener != null && update) {
            mAnnotChangeListener.onChangeAnnotLineStartStyle(lineStartStyle);
        }
    }

    public void updateLineEndStyleListener(LineEndingStyle lineEndStyle, boolean update) {
        if (mUpdateListener && mAnnotChangeListener != null && update) {
            mAnnotChangeListener.onChangeAnnotLineEndStyle(lineEndStyle);
        }
    }

    private void updateTextSizeListener(float textSize, boolean done) {
        updateTextSizeListener(textSize, mTextSize != textSize, done);
    }

    /**
     * Update text size to listener
     *
     * @param textSize textSize
     * @param update   whether to invoke listener method
     */
    public void updateTextSizeListener(float textSize, boolean update, boolean done) {
        if (mUpdateListener && mAnnotChangeListener != null && (update || done)) {
            mAnnotChangeListener.onChangeAnnotTextSize(textSize, done);
        }
    }

    private void updateTextColorListener(@ColorInt int textColor) {
        updateTextColorListener(textColor, mTextColor != textColor);
    }

    private void updateTextColorListener(@ColorInt int textColor, boolean update) {
        if (mUpdateListener && mAnnotChangeListener != null && update) {
            mAnnotChangeListener.onChangeAnnotTextColor(textColor);
        }
    }

    public void updateTextAlignment(int horizontalAlignment,int verticalAlignment, boolean update) {
        if (mUpdateListener && mAnnotChangeListener != null && update) {
            mAnnotChangeListener.onChangeTextAlignment(horizontalAlignment, verticalAlignment);
        }
    }

    /**
     * Bind a preview view to this annotation style, whenever the annotation style updates, the preview updates
     *
     * @param previewView preview view
     */
    public void bindPreview(ActionButton previewView) {
        mPreview = previewView;
        updatePreviewStyle();
    }

    /**
     * Gets binded preview
     *
     * @return binded preview
     */
    public ActionButton getBindedPreview() {
        return mPreview;
    }

    private void updatePreviewStyle() {
        if (mPreview != null) {
            ArrayList<AnnotStyle> annotStyles = new ArrayList<>(1);
            annotStyles.add(this);
            mPreview.updateAppearance(annotStyles);
        }
    }

    public void updateAllListeners() {
        updateStrokeColorListener(mStrokeColor, true);
        updateFillColorListener(mFillColor, true);
        updateThicknessListener(mThickness, true, true);
        updateOpacityListener(mOpacity, true, true);
        if ((isStickyNote() || isCountMeasurement()) && !Utils.isNullOrEmpty(mIcon)) {
            updateIconListener(mIcon, true);
        }
        if (hasTextStyle()) {
            updateTextColorListener(mTextColor, true);
            updateTextSizeListener(mTextSize, true);
        }
        if (hasFont() && !Utils.isNullOrEmpty(mFont.getPDFTronName())) {
            updateFontListener(mFont, true);
        }
        if (isMeasurement()) {
            updateRulerBaseValueListener(mRuler.mRulerBase, true);
            updateRulerBaseUnitListener(mRuler.mRulerBaseUnit, true);
            updateRulerTranslateValueListener(mRuler.mRulerTranslate, true);
            updateRulerTranslateUnitListener(mRuler.mRulerTranslateUnit, true);
            updateRulerPrecisionListener(mRuler.mPrecision, true);
        }
        if (hasBorderStyle()) {
            updateBorderStyleListener(ShapeBorderStyle.valueOf(mBorderStyle), true);
        } else if (hasLineStyle()) {
            updateLineStyleListener(LineStyle.valueOf(mLineStyle), true);
        }
        if (hasLineStartStyle()) {
            updateLineStartStyleListener(LineEndingStyle.valueOf(mLineStartStyle), true);
        }
        if (hasLineEndStyle()) {
            updateLineEndStyleListener(LineEndingStyle.valueOf(mLineEndStyle), true);
        }
    }

    private void updateStrokeColorListener(@ColorInt int strokeColor) {
        updateStrokeColorListener(strokeColor, strokeColor != mStrokeColor);
    }

    private void updateStrokeColorListener(@ColorInt int strokeColor, boolean update) {
        if (mUpdateListener && mAnnotChangeListener != null && update) {
            mAnnotChangeListener.onChangeAnnotStrokeColor(strokeColor);
        }
    }

    private void updateFillColorListener(@ColorInt int color) {
        updateFillColorListener(color, color != mFillColor);
    }

    private void updateFillColorListener(@ColorInt int color, boolean update) {
        if (mUpdateListener && mAnnotChangeListener != null && update) {
            mAnnotChangeListener.onChangeAnnotFillColor(color);
        }
    }

    private void updateOpacityListener(float opacity, boolean done) {
        updateOpacityListener(opacity, opacity != mOpacity, done);
    }

    private void updateOpacityListener(float opacity, boolean update, boolean done) {
        if (mUpdateListener && mAnnotChangeListener != null && (update || done)) {
            mAnnotChangeListener.onChangeAnnotOpacity(opacity, done);
            if (isStickyNote() || isCountMeasurement()) {
                updateIconListener(mIcon, update);
            }
        }
    }

    private void updateSnapListener(boolean snap) {
        if (mUpdateListener && mAnnotChangeListener != null) {
            mAnnotChangeListener.onChangeSnapping(snap);
        }
    }

    private void updateRichContentEnabledListener(boolean enabled) {
        if (mUpdateListener && mAnnotChangeListener != null) {
            mAnnotChangeListener.onChangeRichContentEnabled(enabled);
        }
    }

    private void updateOverlayTextListener(String overlayText) {
        if (mUpdateListener && mAnnotChangeListener != null) {
            mAnnotChangeListener.onChangeOverlayText(overlayText);
        }
    }

    private void updateIconListener(String icon) {
        updateIconListener(icon, !icon.equals(mIcon));
    }

    private void updateIconListener(String icon, boolean update) {
        if (mUpdateListener && mAnnotChangeListener != null && update) {
            mAnnotChangeListener.onChangeAnnotIcon(icon);
        }
    }

    private void updateFontListener(FontResource font) {
        updateFontListener(font, !font.equals(mFont));
    }

    private void updateFontListener(FontResource font, boolean update) {
        if (mUpdateListener && mAnnotChangeListener != null && update) {
            mAnnotChangeListener.onChangeAnnotFont(font);
        }
    }

    private void updateDateFormatListener(String dateFormat) {
        if (mUpdateListener && mAnnotChangeListener != null) {
            mAnnotChangeListener.onChangeDateFormat(dateFormat);
        }
    }

    private void updateRulerBaseValueListener(float val) {
        updateRulerBaseValueListener(val, val != mRuler.mRulerBase);
    }

    private void updateRulerBaseValueListener(float val, boolean update) {
        if (mUpdateListener && mAnnotChangeListener != null && update) {
            if (mRulerCopy == null) {
                mRulerCopy = new RulerItem(mRuler);
            }
            mRulerCopy.mRulerBase = val;
            mAnnotChangeListener.onChangeRulerProperty(mRulerCopy);
        }
    }

    private void updateRulerBaseUnitListener(String val) {
        updateRulerBaseUnitListener(val, !val.equals(mRuler.mRulerBaseUnit));
    }

    private void updateRulerBaseUnitListener(String val, boolean update) {
        if (mUpdateListener && mAnnotChangeListener != null && update) {
            if (mRulerCopy == null) {
                mRulerCopy = new RulerItem(mRuler);
            }
            mRulerCopy.mRulerBaseUnit = val;
            mAnnotChangeListener.onChangeRulerProperty(mRulerCopy);
        }
    }

    private void updateRulerTranslateValueListener(float val) {
        updateRulerTranslateValueListener(val, val != mRuler.mRulerTranslate);
    }

    private void updateRulerTranslateValueListener(float val, boolean update) {
        if (mUpdateListener && mAnnotChangeListener != null && update) {
            if (mRulerCopy == null) {
                mRulerCopy = new RulerItem(mRuler);
            }
            mRulerCopy.mRulerTranslate = val;
            mAnnotChangeListener.onChangeRulerProperty(mRulerCopy);
        }
    }

    private void updateRulerTranslateUnitListener(String val) {
        updateRulerTranslateUnitListener(val, !val.equals(mRuler.mRulerTranslateUnit));
    }

    private void updateRulerTranslateUnitListener(String val, boolean update) {
        if (mUpdateListener && mAnnotChangeListener != null && update) {
            if (mRulerCopy == null) {
                mRulerCopy = new RulerItem(mRuler);
            }
            mRulerCopy.mRulerTranslateUnit = val;
            mAnnotChangeListener.onChangeRulerProperty(mRulerCopy);
        }
    }

    private void updateRulerPrecisionListener(int val) {
        updateRulerPrecisionListener(val, val != mRuler.mPrecision);
    }

    private void updateRulerPrecisionListener(int val, boolean update) {
        if (mUpdateListener && mAnnotChangeListener != null && update) {
            if (mRulerCopy == null) {
                mRulerCopy = new RulerItem(mRuler);
            }
            mRulerCopy.mPrecision = val;
            mAnnotChangeListener.onChangeRulerProperty(mRulerCopy);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AnnotStyle) {
            AnnotStyle other = (AnnotStyle) obj;
            boolean styleEquals = other.getThickness() == getThickness()
                    && other.getAnnotType() == getAnnotType()
                    && other.getOpacity() == getOpacity()
                    && other.getColor() == getColor()
                    && other.getFillColor() == getFillColor();

            if (hasBorderStyle()) {
                styleEquals = styleEquals && other.getBorderStyle() == getBorderStyle();
            } else if (hasLineStyle()) {
                styleEquals = styleEquals && other.getLineStyle() == getLineStyle();
            }
            if (hasLineStartStyle()) {
                styleEquals = styleEquals && other.getLineStartStyle() == getLineStartStyle();
            }
            if (hasLineEndStyle()) {
                styleEquals = styleEquals && other.getLineEndStyle() == getLineEndStyle();
            }
            if (hasTextAlignment()) {
                styleEquals = styleEquals && other.getHorizontalAlignment() == getHorizontalAlignment()
                        && other.getVerticalAlignment() == getVerticalAlignment();
            }

            boolean fontEquals = other.getFont().equals(getFont());
            boolean iconEquals = other.getIcon().equals(getIcon());
            boolean annotTypeEquals = getAnnotType() == other.getAnnotType();
            boolean textStyleEquals = other.getTextSize() == getTextSize()
                    && other.getTextColor() == getTextColor();

            if (!annotTypeEquals) {
                return false;
            }

            if (isStickyNote() || isCountMeasurement()) {
                return iconEquals && other.getOpacity() == getOpacity()
                        && other.getColor() == getColor();
            }

            if (hasTextStyle() && hasFont()) {
                return fontEquals && textStyleEquals && styleEquals;
            } else if (hasTextStyle()) {
                return textStyleEquals && styleEquals;
            } else if (hasFont()) {
                return fontEquals && styleEquals;
            }

            if (isMeasurement()) {
                return styleEquals
                        && other.getRulerItem().equals(getRulerItem());
            }

            return styleEquals;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        int result;
        result = (mThickness != +0.0f ? Float.floatToIntBits(mThickness) : 0);
        result = 31 * result + (mTextSize != +0.0f ? Float.floatToIntBits(mTextSize) : 0);
        result = 31 * result + mTextColor;
        result = 31 * result + (mTextContent != null ? mTextContent.hashCode() : 0);
        result = 31 * result + mStrokeColor;
        result = 31 * result + mFillColor;
        result = 31 * result + (mOpacity != +0.0f ? Float.floatToIntBits(mOpacity) : 0);
        result = 31 * result + (mOverlayText != null ? mOverlayText.hashCode() : 0);
        result = 31 * result + (mIcon != null ? mIcon.hashCode() : 0);
        result = 31 * result + (mFont != null ? mFont.hashCode() : 0);
        result = 31 * result + mAnnotType;
        result = 31 * result + (mRuler != null ? mRuler.hashCode() : 0);
        result = 31 * result + (mBorderStyle != null ? mBorderStyle.hashCode() : 0);
        result = 31 * result + (mLineStyle != null ? mLineStyle.hashCode() : 0);
        result = 31 * result + (mLineStartStyle != null ? mLineStartStyle.hashCode() : 0);
        result = 31 * result + (mLineEndStyle != null ? mLineEndStyle.hashCode() : 0);
        result = 31 * result + mHorizontalAlignment;
        result = 31 * result + mVerticalAlignment;
        return result;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public float getMaxInternalThickness() {
        switch (mAnnotType) {
            case Annot.e_Underline:
            case Annot.e_StrikeOut:
            case Annot.e_Highlight:
            case Annot.e_Squiggly:
                return 40;
            case CUSTOM_ANNOT_TYPE_RULER:
                return 10;
            default:
                return 70;
        }
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * This interface is used for changing annotation appearance
     */
    public interface OnAnnotStyleChangeListener {
        /**
         * The method is invoked when thickness is changed in style picker
         *
         * @param thickness thickness of annotation style.
         */
        void onChangeAnnotThickness(float thickness, boolean done);

        /**
         * The method is invoked when text size is changed in style picker
         *
         * @param textSize text size of annotation style.
         */
        void onChangeAnnotTextSize(float textSize, boolean done);

        /**
         * The method is invoked when text color is changed in style picker
         *
         * @param textColor text color of annotation style.
         */
        void onChangeAnnotTextColor(@ColorInt int textColor);

        /**
         * The method is invoked when opacity is changed in style picker
         *
         * @param opacity opacity of annotation
         */
        void onChangeAnnotOpacity(float opacity, boolean done);

        /**
         * The method is invoked when color is selected
         * If it is {@link com.pdftron.pdf.annots.Text} annotation, it will change sticky note icon color
         * If it is {@link com.pdftron.pdf.annots.FreeText} annotation, it will change text color
         *
         * @param color stroke color/ icon color/ text color of annotation
         */
        void onChangeAnnotStrokeColor(@ColorInt int color);

        /**
         * The method is invoked when fill color is selected
         *
         * @param color fill color of annotation
         */
        void onChangeAnnotFillColor(@ColorInt int color);

        /**
         * The method is invoked when icon is selected in icon picker
         *
         * @param icon icon name of sticky note
         */
        void onChangeAnnotIcon(String icon);

        /**
         * The method is invoked when font resource is selected in font spinner
         *
         * @param font font resource
         */
        void onChangeAnnotFont(FontResource font);

        /**
         * The method is invoked when any of the ruler properties change
         *
         * @param rulerItem the ruler item
         */
        void onChangeRulerProperty(RulerItem rulerItem);

        /**
         * The method is invoked when overlay text change
         *
         * @param overlayText the overlay text
         */
        void onChangeOverlayText(String overlayText);

        /**
         * The method is invoked when snap switch change
         *
         * @param snap whether to snap
         */
        void onChangeSnapping(boolean snap);

        /**
         * The method is invoked when rich content enabled change
         *
         * @param enabled whether to enable rich content
         */
        void onChangeRichContentEnabled(boolean enabled);

        /**
         * This method is invoked when date format change
         *
         * @param dateFormat the date format
         */
        void onChangeDateFormat(String dateFormat);

        /**
         * This method is invoked when the border style of an annotation is changed
         *
         * @param borderStyle the border style
         */
        void onChangeAnnotBorderStyle(ShapeBorderStyle borderStyle);

        /**
         * This method is invoked when the line style of an annotation is changed
         *
         * @param lineStyle the line style
         */
        void onChangeAnnotLineStyle(LineStyle lineStyle);

        /**
         * This method is invoked when the line start style of an annotation is changed
         *
         * @param lineStartStyle the line start style
         */
        void onChangeAnnotLineStartStyle(LineEndingStyle lineStartStyle);

        /**
         * This method is invoked when the line end style of an annotation is changed
         *
         * @param lineEndStyle the line end style
         */
        void onChangeAnnotLineEndStyle(LineEndingStyle lineEndStyle);

        /**
         * This method is invoked when the text alignment is changed
         * @param horizontalAlignment the horizontal text alignment
         * @param verticalAlignment the vertical text alignment
         */
        void onChangeTextAlignment(int horizontalAlignment, int verticalAlignment);
    }

    /**
     * This interface is for holding annotation style
     */
    public interface AnnotStyleHolder {
        /**
         * Abstract method for getting annotation style
         *
         * @return annotation style
         */
        AnnotStyle getAnnotStyle();

        /**
         * Abstract method for getting current annotation style preview view
         *
         * @return preview of annotation style
         */
        AnnotationPropertyPreviewView getAnnotPreview();

        /**
         * Abstract method for getting all annotation style preview views
         *
         * @return preview of annotation style
         */
        SparseArray<AnnotationPropertyPreviewView> getAnnotPreviews();

        /**
         * Abstract method for setting annotation style preview visibility
         */
        void setAnnotPreviewVisibility(int visibility);

        /**
         * Abstract method to notify when the annot style layout has changed
         */
        void onAnnotStyleLayoutUpdated();
    }
}
