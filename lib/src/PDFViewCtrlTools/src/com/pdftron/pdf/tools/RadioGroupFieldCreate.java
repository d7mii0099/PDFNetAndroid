package com.pdftron.pdf.tools;

import android.graphics.Color;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.RadioButtonGroup;
import com.pdftron.pdf.annots.RadioButtonWidget;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.Utils;

import java.util.UUID;

/**
 * This class is for creating radio group field
 */
@Keep
public class RadioGroupFieldCreate extends RectCreate {

    private RadioButtonGroup mTargetGroup = null;

    /**
     * Class constructor
     *
     * @param ctrl the PDFViewCtrl
     */
    public RadioGroupFieldCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
    }

    public void setTargetGroup(RadioButtonGroup group) {
        mTargetGroup = group;
    }

    @Override
    protected Annot createMarkup(@NonNull PDFDoc doc, Rect bbox) throws PDFNetException {
        if (null == mTargetGroup) {
            mTargetGroup = RadioButtonGroup.create(doc, UUID.randomUUID().toString());
        }
        RadioButtonWidget annot = mTargetGroup.add(bbox);
        annot.getSDFObj().putString(PDFTRON_ID, "");
        ColorPt colorPt = Utils.color2ColorPt(Color.WHITE);
        annot.setBackgroundColor(colorPt, 3);
        return annot;
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.FORM_RADIO_GROUP_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Widget;
    }
}
