package com.pdftron.richeditor.styles;

import android.text.Editable;
import android.text.Spannable;
import android.widget.EditText;

import com.pdftron.richeditor.AREditText;
import com.pdftron.richeditor.Util;
import com.pdftron.richeditor.spans.AreLeadingMarginSpan;

public class ARE_IndentLeft extends ARE_ABS_FreeStyle {

    public ARE_IndentLeft(AREditText editText) {
        super(editText);
    }

    public void apply() {

        EditText editText = getEditText();
        int currentLine = Util.getCurrentCursorLine(editText);
        int start = Util.getThisLineStart(editText, currentLine);
        int end = Util.getThisLineEnd(editText, currentLine);

        Editable editable = editText.getText();
        AreLeadingMarginSpan[] existingLMSpans = editable.getSpans(start, end, AreLeadingMarginSpan.class);
        if (null != existingLMSpans && existingLMSpans.length == 1) {
            AreLeadingMarginSpan currentLeadingMarginSpan = existingLMSpans[0];
            int originalEnd = editable.getSpanEnd(currentLeadingMarginSpan);
            editable.removeSpan(currentLeadingMarginSpan);
            int currentLevel = currentLeadingMarginSpan.decreaseLevel();
            if (currentLevel > 0) {
                editable.setSpan(currentLeadingMarginSpan, start, originalEnd, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
        } else {
            // No leading margin span found
            // Do nothing
        }
    }

    @Override
    public void applyStyle(Editable editable, int start, int end) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setChecked(boolean isChecked) {
        // TODO Auto-generated method stub

    }
}
