package com.pdftron.richeditor.styles;

import android.text.Editable;
import android.text.style.ForegroundColorSpan;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.pdftron.richeditor.AREditText;
import com.pdftron.richeditor.Util;
import com.pdftron.richeditor.spans.AreForegroundColorSpan;

public class ARE_FontColor extends ARE_ABS_Dynamic_Style<AreForegroundColorSpan> {

    private AREditText mEditText;

    private int mColor = -1;

    public ARE_FontColor(@NonNull AREditText editText) {
        super(editText.getContext());
        this.mEditText = editText;
    }

    /**
     * @param editText
     */
    public void setEditText(AREditText editText) {
        this.mEditText = editText;
    }

    public void apply(int color) {
        mColor = color;
        if (null != mEditText) {
            Editable editable = mEditText.getEditableText();
            int start = mEditText.getSelectionStart();
            int end = mEditText.getSelectionEnd();

            if (end > start) {
                applyNewStyle(editable, start, end, mColor);
            }
        }
    }

    @Override
    protected void changeSpanInsideStyle(Editable editable, int start, int end, AreForegroundColorSpan existingSpan) {
        int currentColor = existingSpan.getForegroundColor();
        if (currentColor != mColor) {
            Util.log("color changed before: " + currentColor + ", new == " + mColor);
            applyNewStyle(editable, start, end, mColor);
            logAllFontColorSpans(editable);
        }
    }

    private void logAllFontColorSpans(Editable editable) {
        ForegroundColorSpan[] listItemSpans = editable.getSpans(0,
                editable.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : listItemSpans) {
            int ss = editable.getSpanStart(span);
            int se = editable.getSpanEnd(span);
            Util.log("List All: " + " :: start == " + ss + ", end == " + se);
        }
    }

    @Override
    public AreForegroundColorSpan newSpan() {
        return new AreForegroundColorSpan(this.mColor);
    }

    @Override
    public void setChecked(boolean isChecked) {
        // Do nothing
    }

    @Override
    public boolean getIsChecked() {
        return this.mColor != -1;
    }

    @Override
    public EditText getEditText() {
        return this.mEditText;
    }

    @Override
    protected AreForegroundColorSpan newSpan(int color) {
        return new AreForegroundColorSpan(color);
    }

    @Override
    protected void featureChangedHook(int lastSpanColor) {
        mColor = lastSpanColor;
    }
}
