
//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.MotionEvent;
import androidx.annotation.ColorInt;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.Field;
import com.pdftron.pdf.Font;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.TextWidget;
import com.pdftron.pdf.annots.Widget;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.AutoScrollEditor;

import java.io.File;
import java.util.UUID;

/**
 * This class is for creating multiline text field
 */
@Keep
public class TextFieldCreate extends RectCreate {

    private boolean mIsMultiline;
    private int mJustification;
    @ColorInt
    protected int mTextColor;
    protected float mTextSize;
    protected String mPDFTronFontName;
    private AutoScrollEditor mEditor;
    private boolean mCustomAppearanceSet = false;

    public static final String BACKGROUND_ALPHA = "BG_ALPHA";

    /**
     * Class constructor
     */
    public TextFieldCreate(PDFViewCtrl ctrl) {
        this(ctrl, true, Field.e_left_justified);
    }

    /**
     * Class constructor
     */
    public TextFieldCreate(PDFViewCtrl ctrl, boolean isMultiline, int justification) {
        super(ctrl);
        mIsMultiline = isMultiline;
        mJustification = justification;
    }

    @Override
    public void setupAnnotProperty(AnnotStyle annotStyle) {
        mAnnotStyle = annotStyle;
        super.setupAnnotProperty(annotStyle);

        mTextColor = annotStyle.getTextColor();
        mTextSize = annotStyle.getTextSize();
        mPDFTronFontName = annotStyle.getPDFTronFontName();

        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt(getTextColorKey(getCreateAnnotType()), mTextColor);
        editor.putFloat(getTextSizeKey(getCreateAnnotType()), mTextSize);
        editor.putString(getFontKey(getCreateAnnotType()), mPDFTronFontName);
        editor.apply();
    }

    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        boolean result = super.onUp(e, priorEventMode);
        if (getToolManager().isTextFieldCustomAppearanceEnabled()) {
            generateCustomAppearance();
        }
        return result;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        mCustomAppearanceSet = false;
        initTextField();
        return super.onDown(e);
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.FORM_TEXT_FIELD_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return AnnotStyle.CUSTOM_ANNOT_TYPE_TEXT_FIELD;
    }

    protected ColorPt getBackgroundColorPt() throws PDFNetException {
        return Utils.color2ColorPt(Color.WHITE);
    }

    protected double getBackgroundOpacity() {
        return 1d;
    }

    @Override
    protected Annot createMarkup(@NonNull PDFDoc doc, Rect bbox) throws PDFNetException {
        TextWidget widget = TextWidget.create(doc, bbox, UUID.randomUUID().toString());

        ColorPt backgroundColor = getBackgroundColorPt();
        widget.setBackgroundColor(backgroundColor, 3);

        widget.getSDFObj().putString(PDFTRON_ID, "");
        if (getToolManager().isTextFieldCustomAppearanceEnabled()) {
            widget.setCustomData(BACKGROUND_ALPHA, "" + getBackgroundOpacity());
        }

        Field field = widget.getField();
        field.setFlag(Field.e_multiline, mIsMultiline);
        field.setJustification(mJustification);

        setWidgetStyle(doc, widget, "");

        return widget;
    }

    protected void generateCustomAppearance() {
        // Generate appearance on creating TextField with an empty textEdit
        if (null == mAnnot || mCustomAppearanceSet) {
            return;
        }
        if (Utils.isKitKat() && getToolManager().isTextFieldCustomAppearanceEnabled()) {
            mCustomAppearanceSet = true;
            boolean shouldUnlockRead = false;
            try {
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;
                mEditor = new AutoScrollEditor(mPdfViewCtrl.getContext());
                mEditor.setAnnot(mPdfViewCtrl, mAnnot, mAnnotPageNum);
                if (mAnnotStyle != null) {
                    mEditor.setAnnotStyle(mPdfViewCtrl, mAnnotStyle);
                }
                mPdfViewCtrl.addView(mEditor);
                int a = (int) (Math.round(getBackgroundOpacity() * 255));
                int r = (int) Math.round(getBackgroundColorPt().get(0) * 255);
                int g = (int) Math.round(getBackgroundColorPt().get(1) * 255);
                int b = (int) Math.round(getBackgroundColorPt().get(2) * 255);
                int backgroundColor = Color.argb(a, r, g, b);
                mEditor.setBackgroundColor(backgroundColor);
                styleAutoScrollEditor(mEditor);
                File appearance = new File(mPdfViewCtrl.getContext().getCacheDir(), "rc-TextField.pdf");
                AnnotUtils.createPdfFromView(mEditor, mEditor.getWidth(), mEditor.getHeight(), appearance);
                AnnotUtils.refreshCustomFreeTextAppearance(appearance, mAnnot);
                mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
                // Clean up view
                mPdfViewCtrl.removeView(mEditor);
                resetPts();
                mPdfViewCtrl.invalidate();
            } catch (Exception ignored) {

            } finally {
                if (shouldUnlockRead) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }
        }
    }

    /**
     * Adds custom text editor styling
     */
    protected void styleAutoScrollEditor(AutoScrollEditor editor) throws Exception {
        // no-op, for customers
    }

    protected void setWidgetStyle(@NonNull PDFDoc doc, @NonNull Widget widget, @NonNull String contents) throws PDFNetException {
        ColorPt color = Utils.color2ColorPt(mTextColor);
        widget.setFontSize(mTextSize);
        widget.setTextColor(color, 3);

        // change font
        if (!Utils.isNullOrEmpty(mPDFTronFontName) && getToolManager().isFontLoaded()) {
            Font font = Font.create(doc, mPDFTronFontName, contents);
            String fontName = font.getName();
            widget.setFont(font);

            // save font name with font if not saved already
            updateFontMap(mPdfViewCtrl.getContext(), widget.getType(), mPDFTronFontName, fontName);
        }
    }

    public void initTextField() {
        Context context = mPdfViewCtrl.getContext();
        SharedPreferences settings = Tool.getToolPreferences(context);
        mTextColor = settings.getInt(getTextColorKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultTextColor(context, getCreateAnnotType()));
        mTextSize = settings.getFloat(getTextSizeKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultTextSize(context, getCreateAnnotType()));
        mStrokeColor = settings.getInt(getColorKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultColor(context, getCreateAnnotType()));
        mThickness = settings.getFloat(getThicknessKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultThickness(context, getCreateAnnotType()));
        mFillColor = settings.getInt(getColorFillKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultFillColor(context, getCreateAnnotType()));
        mOpacity = settings.getFloat(getOpacityKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultOpacity(context, getCreateAnnotType()));
        mPDFTronFontName = settings.getString(getFontKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultFont(context, getCreateAnnotType()));
    }

    private ToolManager getToolManager() {
        return (ToolManager) mPdfViewCtrl.getToolManager();
    }
}
