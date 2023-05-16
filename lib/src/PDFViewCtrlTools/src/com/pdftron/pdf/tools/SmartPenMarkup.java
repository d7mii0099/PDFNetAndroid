package com.pdftron.pdf.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.RectF;
import android.view.MotionEvent;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Highlight;
import com.pdftron.pdf.annots.Squiggly;
import com.pdftron.pdf.annots.StrikeOut;
import com.pdftron.pdf.annots.Underline;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;

@Keep
public class SmartPenMarkup extends TextMarkupCreate {

    private int mTextMarkupType = Annot.e_Highlight;

    private final float mHorizontalOffset;
    private final float mVerticalOffset;

    /**
     * Class constructor
     */
    public SmartPenMarkup(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);

        mHorizontalOffset = Utils.convDp2Pix(mPdfViewCtrl.getContext(), SmartPenInk.sHORIZONTAL_THRESHOLD);
        mVerticalOffset = Utils.convDp2Pix(mPdfViewCtrl.getContext(), SmartPenInk.sVERTICAL_THRESHOLD);

        mPdfViewCtrl.setStylusScaleEnabled(false);
    }

    @Override
    public void setupAnnotProperty(AnnotStyle annotStyle) {
        mTextMarkupType = annotStyle.getAnnotType();

        // save the last used text markup type
        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt(getTextMarkupTypeKey(AnnotStyle.CUSTOM_SMART_PEN), getCreateAnnotType());

        editor.apply();

        super.setupAnnotProperty(annotStyle);
    }

    @Override
    public void setupAnnotStyles(@NonNull ArrayList<AnnotStyle> annotStyles) {
        if (annotStyles.size() == 2) {
            setupAnnotProperty(annotStyles.get(1));

            // save annot style for smart ink
            // should not use createTool here as it will generate extra events
            // here we just want to save the styles
            SmartPenInk smartPenInk = new SmartPenInk(mPdfViewCtrl);
            smartPenInk.setupAnnotProperty(annotStyles.get(0));
        }
    }

    @Override
    public boolean onDown(MotionEvent e) {
        mPdfViewCtrl.setStylusScaleEnabled(false); // needed due to tool-lopping

        // consume quick menu
        if (((ToolManager) mPdfViewCtrl.getToolManager()).isQuickMenuJustClosed()) {
            return true;
        }

        RectF textSelectRect = getTextSelectRect(e.getX(), e.getY());
        textSelectRect.left = textSelectRect.left - mHorizontalOffset;
        textSelectRect.right = textSelectRect.right + mHorizontalOffset;
        textSelectRect.top = textSelectRect.top - mVerticalOffset;
        textSelectRect.bottom = textSelectRect.bottom + mVerticalOffset;
        boolean isTextSelect = mPdfViewCtrl.selectByRect(textSelectRect.left, textSelectRect.top, textSelectRect.right, textSelectRect.bottom);
        if (!isTextSelect) {
            mNextToolMode = ToolManager.ToolMode.SMART_PEN_INK;
            return super.onDown(e);
        }

        Context context = mPdfViewCtrl.getContext();
        SharedPreferences settings = Tool.getToolPreferences(context);
        mTextMarkupType = settings.getInt(getTextMarkupTypeKey(AnnotStyle.CUSTOM_SMART_PEN), Annot.e_Highlight);

        mNextToolMode = getToolMode();
        return super.onDown(e);
    }

    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolManager.ToolMode.SMART_PEN_TEXT_MARKUP;
    }

    @Override
    public int getCreateAnnotType() {
        return mTextMarkupType;
    }

    @Override
    protected Annot createMarkup(PDFDoc doc, Rect bbox) throws PDFNetException {
        if (getCreateAnnotType() == Annot.e_Underline) {
            return Underline.create(doc, bbox);
        } else if (getCreateAnnotType() == Annot.e_StrikeOut) {
            return StrikeOut.create(doc, bbox);
        } else if (getCreateAnnotType() == Annot.e_Squiggly) {
            return Squiggly.create(doc, bbox);
        }
        return Highlight.create(doc, bbox);
    }
}
