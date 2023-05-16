package com.pdftron.pdf.dialog.annotlist;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnnotUtils;

/**
 * Utility class for filtering annotations in {@link PDFViewCtrl}.
 */
public class AnnotationListFilterUtil {

    public static int getReviewStatusImageResId(String status) {
        if (status.equals(AnnotUtils.Key_StateAccepted)) {
            return R.drawable.ic_state_accepted;
        } else if (status.equals(AnnotUtils.Key_StateCancelled)) {
            return R.drawable.ic_state_cancelled;
        } else if (status.equals(AnnotUtils.Key_StateCompleted)) {
            return R.drawable.ic_state_completed;
        } else if (status.equals(AnnotUtils.Key_StateRejected)) {
            return R.drawable.ic_state_rejected;
        } else {
            return R.drawable.ic_state_none;
        }
    }
}
