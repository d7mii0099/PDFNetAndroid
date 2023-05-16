package com.pdftron.pdf.tools;

import android.graphics.PointF;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.annots.Text;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.utils.Utils;

@Keep
public class CountMeasurementCreateTool extends StickyNoteCreate {

    public static final String COUNT_MEASURE_KEY = "trn-is-count";
    public static final String COUNT_MEASURE_LABEL_KEY = "count-label";
    public static final String COUNT_MEASURE_CHECKMARK_ICON = "CheckMark";

    @Nullable
    private String mStampLabel;

    /**
     * Class constructor
     *
     * @param ctrl
     */
    public CountMeasurementCreateTool(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        mNextToolMode = getToolMode();
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolManager.ToolMode.COUNT_MEASUREMENT;
    }

    @Override
    public int getCreateAnnotType() {
        return AnnotStyle.CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT;
    }

    @Override
    public void setupAnnotProperty(int color, float opacity, float thickness, int fillColor, String icon, String pdfTronFontName) {
        super.setupAnnotProperty(color, opacity, thickness, fillColor, COUNT_MEASURE_CHECKMARK_ICON, pdfTronFontName);
    }

    @Override
    public void setupAnnotProperty(AnnotStyle annotStyle) {
        super.setupAnnotProperty(annotStyle);

        mStampLabel = annotStyle.getStampId();
    }

    /**
     * Sets the target point.
     *
     * @param point The target point
     */
    @Override
    public void setTargetPoint(PointF point) {
        mPt1.x = point.x + mPdfViewCtrl.getScrollX();
        mPt1.y = point.y + mPdfViewCtrl.getScrollY();
        mDownPageNum = mPdfViewCtrl.getPageNumberFromScreenPt(point.x, point.y);

        setCurrentDefaultToolModeHelper(getToolMode());

        if (!Utils.isNullOrEmpty(mStampLabel)) {
            createStickyNote();
        }
    }

    @Override
    protected void setCustomData(Text text) throws PDFNetException {
        super.setCustomData(text);
        text.setCustomData(COUNT_MEASURE_KEY, "true");
        text.setCustomData(COUNT_MEASURE_LABEL_KEY, mStampLabel);
        text.setAnchorPosition(new Point(0.5, 0.5));
    }
}
