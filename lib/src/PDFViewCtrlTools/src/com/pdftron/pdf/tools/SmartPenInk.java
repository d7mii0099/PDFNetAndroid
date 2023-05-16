package com.pdftron.pdf.tools;

import android.graphics.RectF;
import android.view.MotionEvent;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;

@Keep
public class SmartPenInk extends FreehandCreate {

    public static float sVERTICAL_THRESHOLD = 12f;
    public static float sHORIZONTAL_THRESHOLD = 4f;

    private final float mHorizontalOffset;
    private final float mVerticalOffset;

    /**
     * Class constructor
     */
    public SmartPenInk(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);

        mHorizontalOffset = Utils.convDp2Pix(mPdfViewCtrl.getContext(), SmartPenInk.sHORIZONTAL_THRESHOLD);
        mVerticalOffset = Utils.convDp2Pix(mPdfViewCtrl.getContext(), SmartPenInk.sVERTICAL_THRESHOLD);
    }

    @Override
    public void setupAnnotStyles(@NonNull ArrayList<AnnotStyle> annotStyles) {
        if (annotStyles.size() == 2) {
            setupAnnotProperty(annotStyles.get(0));

            // save annot style for smart text markup
            // should not use createTool here as it will generate extra events
            // here we just want to save the styles
            SmartPenMarkup smartPenMarkup = new SmartPenMarkup(mPdfViewCtrl);
            smartPenMarkup.setupAnnotProperty(annotStyles.get(1));
        }
    }

    @Override
    public boolean onDown(MotionEvent e) {
        mPdfViewCtrl.setStylusScaleEnabled(false); // needed due to tool-lopping

        RectF textSelectRect = getTextSelectRect(e.getX(), e.getY());
        textSelectRect.left = textSelectRect.left - mHorizontalOffset;
        textSelectRect.right = textSelectRect.right + mHorizontalOffset;
        textSelectRect.top = textSelectRect.top - mVerticalOffset;
        textSelectRect.bottom = textSelectRect.bottom + mVerticalOffset;
        boolean isTextSelect = mPdfViewCtrl.selectByRect(textSelectRect.left, textSelectRect.top, textSelectRect.right, textSelectRect.bottom);
        if (isTextSelect) {
            mNextToolMode = ToolManager.ToolMode.SMART_PEN_TEXT_MARKUP;
            return super.onDown(e);
        }
        mNextToolMode = getToolMode();
        return super.onDown(e);
    }

    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolManager.ToolMode.SMART_PEN_INK;
    }
}
