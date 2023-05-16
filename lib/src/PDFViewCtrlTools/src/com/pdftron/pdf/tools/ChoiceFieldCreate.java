package com.pdftron.pdf.tools;

import androidx.annotation.Keep;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;

@Keep
public abstract class ChoiceFieldCreate extends TextFieldCreate {

    public ChoiceFieldCreate(PDFViewCtrl ctrl) {
        super(ctrl);
    }

    @Override
    protected void raiseAnnotationAddedEvent(Annot annot, int page) {
        super.raiseAnnotationAddedEvent(annot, page);

        if (annot != null) {
            // show options dialog
            showWidgetChoiceDialog(annot.__GetHandle(), page, isSingleChoice(), isCombo(), null);
        }
    }

    protected abstract boolean isCombo();

    protected abstract boolean isSingleChoice();
}
