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
import com.pdftron.pdf.annots.ComboBoxWidget;
import com.pdftron.pdf.utils.Utils;

/**
 * This class is for creating combo box field
 */
@Keep
public class ComboBoxFieldCreate extends ChoiceFieldCreate {

    /**
     * Class constructor
     */
    public ComboBoxFieldCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
    }

    @Override
    protected boolean isCombo() {
        return true;
    }

    @Override
    protected boolean isSingleChoice() {
        return true;
    }

    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolManager.ToolMode.FORM_COMBO_BOX_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Widget;
    }

    @Override
    protected Annot createMarkup(@NonNull PDFDoc doc, Rect bbox) throws PDFNetException {
        ComboBoxWidget widget = ComboBoxWidget.create(doc, bbox);
        ColorPt colorPt = Utils.color2ColorPt(Color.WHITE);
        widget.setBackgroundColor(colorPt, 3);
        widget.getSDFObj().putString(PDFTRON_ID, "");

        setWidgetStyle(doc, widget, "");

        return widget;
    }
}
