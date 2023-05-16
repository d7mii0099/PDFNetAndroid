package com.pdftron.pdf.tools;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.Utils;

import org.json.JSONException;

/**
 * This class is responsible for creating FreeText annotation with custom spacing.
 * Custom spacing is only applied for Lollipop and above.
 * Otherwise it will behave like default FreeText
 */
@Keep
public class FreeTextSpacingCreate extends FreeTextCreate {

    /**
     * Class constructor
     */
    public FreeTextSpacingCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);

        mFreeTextInlineToggleEnabled = false;
    }

    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolManager.ToolMode.FREE_TEXT_SPACING_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_SPACING;
    }

    @Override
    protected int getEditMode() {
        return ANNOTATION_FREE_TEXT_PREFERENCE_INLINE;
    }

    @Override
    protected void inlineTextEditing(String interimText) {
        super.inlineTextEditing(interimText);

        // add resize widget
        if (Utils.isLollipop()) {
            mInlineEditText.getEditText().addLetterSpacingHandle();
        }
    }

    @Override
    protected void createAnnot(String contents) throws PDFNetException, JSONException {
        super.createAnnot(contents);

        // apply custom appearance
        if (mInlineEditText != null && mInlineEditText.getEditText() != null) {
            AnnotUtils.applyCustomFreeTextAppearance(mPdfViewCtrl,
                    mInlineEditText.getEditText(),
                    mAnnot, mAnnotPageNum);
            mPdfViewCtrl.update(mAnnot, mPageNum);
        }
    }
}
