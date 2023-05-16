package com.pdftron.pdf.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.MotionEvent;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.utils.AnnotUtils;

import org.json.JSONException;

@Keep
public class FreeTextDateCreate extends FreeTextCreate {

    private String mDateFormat;

    /**
     * Class constructor
     */
    public FreeTextDateCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
    }

    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolManager.ToolMode.FREE_TEXT_DATE_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_DATE;
    }

    @Override
    public void setupAnnotProperty(AnnotStyle annotStyle) {
        super.setupAnnotProperty(annotStyle);

        mDateFormat = annotStyle.getDateFormat();

        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(getDateFormatKey(getCreateAnnotType()), mDateFormat);
        editor.apply();
    }

    @Override
    protected void initTextStyle() {
        super.initTextStyle();

        Context context = mPdfViewCtrl.getContext();
        SharedPreferences settings = Tool.getToolPreferences(context);
        mDateFormat = settings.getString(getDateFormatKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultDateFormat(context, getCreateAnnotType()));
    }

    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        if (mOnUpOccurred) {
            return false;
        }
        mOnUpOccurred = true;

        // We are scrolling
        if (mAllowTwoFingerScroll) {
            doneTwoFingerScrolling();
            return false;
        }

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();

        // consume quick menu
        if (toolManager.isQuickMenuJustClosed()) {
            return true;
        }

        if (priorEventMode == PDFViewCtrl.PriorEventMode.PAGE_SLIDING) {
            return false;
        }

        // If we are just up from fling or pinch, do not add new note
        if (priorEventMode == PDFViewCtrl.PriorEventMode.FLING ||
                priorEventMode == PDFViewCtrl.PriorEventMode.PINCH) {
            // allow scrolling
            return false;
        }

        // If annotation was already pushed back, avoid re-entry due to fling motion
        // but allow when creating multiple strokes.
        if (mAnnotPushedBack && mForceSameNextToolMode) {
            return true;
        }

        if (mPageNum >= 1) {
            // prevents creating annotation outside of page bounds

            // if tap on the same kind, select the annotation instead of create a new one
            Annot tappedAnnot = didTapOnSameTypeAnnot(e);
            int x = (int) e.getX();
            int y = (int) e.getY();
            int page = mPdfViewCtrl.getPageNumberFromScreenPt(x, y);
            if (tappedAnnot != null) {
                // force ToolManager to select the annotation
                setCurrentDefaultToolModeHelper(getToolMode());
                toolManager.selectAnnot(tappedAnnot, page);
            } else {
                if (mDateFormat != null) {
                    String dateStr = AnnotUtils.getCurrentTime(mDateFormat);
                    commitFreeTextImpl(dateStr, true);
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    protected void createAnnot(String contents) throws PDFNetException, JSONException {
        super.createAnnot(contents);

        // add custom dict
        if (mAnnot != null) {
            mAnnot.setCustomData(AnnotUtils.KEY_FreeTextDate, mDateFormat);
        }
    }
}
