package com.pdftron.pdf.tools;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Redaction;

@Keep
public class TextRedactionCreate extends TextMarkupCreate {

    /**
     * Class constructor
     */
    public TextRedactionCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
    }

    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolManager.ToolMode.TEXT_REDACTION;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Redact;
    }

    @Override
    protected Annot createMarkup(PDFDoc doc, Rect bbox) throws PDFNetException {
        return Redaction.create(doc, bbox);
    }

    @Override
    protected void createTextMarkup() {
        super.createTextMarkup();

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        if (toolManager.isAutoSelectAnnotation()) {
            mNextToolMode = ToolManager.ToolMode.ANNOT_EDIT;
        } else {
            mNextToolMode = mForceSameNextToolMode ? getToolMode() : ToolManager.ToolMode.PAN;
        }
    }
}
