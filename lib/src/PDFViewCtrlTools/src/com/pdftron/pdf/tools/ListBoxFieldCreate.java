package com.pdftron.pdf.tools;

import android.graphics.Color;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.Field;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.ListBoxWidget;
import com.pdftron.pdf.utils.Utils;

/**
 * This class is for creating list box field
 */
@Keep
public class ListBoxFieldCreate extends ChoiceFieldCreate {

    /**
     * Class constructor
     */
    public ListBoxFieldCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);

        mNextToolMode = getToolMode();
    }

    @Override
    protected boolean isCombo() {
        return false;
    }

    @Override
    protected boolean isSingleChoice() {
        return false;
    }

    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolManager.ToolMode.FORM_LIST_BOX_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Widget;
    }

    @Override
    protected Annot createMarkup(@NonNull PDFDoc doc, Rect bbox) throws PDFNetException {
        ListBoxWidget widget = ListBoxWidget.create(doc, bbox);
        widget.getField().setFlag(Field.e_multiselect, true);
        ColorPt colorPt = Utils.color2ColorPt(Color.WHITE);
        widget.setBackgroundColor(colorPt, 3);
        widget.getSDFObj().putString(PDFTRON_ID, "");

        setWidgetStyle(doc, widget, "");

        return widget;
    }
}
