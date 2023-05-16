package com.pdftron.richeditor.styles;

import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.widget.EditText;

import com.pdftron.pdf.model.FontResource;
import com.pdftron.richeditor.Constants;
import com.pdftron.richeditor.spans.MyTypefaceSpan;

import java.io.File;

public class ARE_Fontface extends ARE_ABS_FreeStyle {

    public ARE_Fontface(EditText editText) {
        super(editText);
    }

    public void apply(FontResource fontResource) {
        EditText editText = getEditText();
        int selectionStart = editText.getSelectionStart();
        int selectionEnd = editText.getSelectionEnd();
        if (selectionStart > selectionEnd) {
            int temp = selectionStart;
            selectionStart = selectionEnd;
            selectionEnd = temp;
        }
        if (!(new File(fontResource.getFilePath()).exists())) {
            return;
        }
        Typeface typeface = Typeface.createFromFile(fontResource.getFilePath());
        MyTypefaceSpan typefaceSpan = new MyTypefaceSpan(typeface, fontResource.getFilePath());
        if (selectionStart == selectionEnd) {
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append(Constants.ZERO_WIDTH_SPACE_STR);
            ssb.setSpan(typefaceSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            editText.getEditableText().replace(selectionStart, selectionEnd, ssb);
        } else {
            editText.getEditableText().setSpan(typefaceSpan, selectionStart, selectionEnd,
                    Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        }
    }

    @Override
    public void applyStyle(Editable editable, int start, int end) {
        // Do nothing
    }

    @Override
    public void setChecked(boolean isChecked) {
        // Do nothing.
    }
}
