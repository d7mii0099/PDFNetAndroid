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
import com.pdftron.pdf.annots.CheckBoxWidget;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.Utils;

import java.util.UUID;

/**
 * This class is for creating checkbox field
 */
@Keep
public class CheckboxFieldCreate extends RectCreate {

    /**
     * Class constructor
     *
     * @param ctrl the PDFViewCtrl
     */
    public CheckboxFieldCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.FORM_CHECKBOX_CREATE;
    }

    @Override
    protected Annot createMarkup(@NonNull PDFDoc doc, Rect bbox) throws PDFNetException {
        CheckBoxWidget widget = CheckBoxWidget.create(doc, bbox, UUID.randomUUID().toString());
        ColorPt colorPt = Utils.color2ColorPt(Color.WHITE);
        widget.setBackgroundColor(colorPt, 3);
        widget.getSDFObj().putString(PDFTRON_ID, "");
        return widget;
    }
}
