package com.pdftron.richeditor.styles;

import android.text.Editable;
import android.widget.EditText;
import androidx.annotation.NonNull;

import com.pdftron.richeditor.AREditText;
import com.pdftron.richeditor.Constants;
import com.pdftron.richeditor.spans.AreFontSizeSpan;

public class ARE_FontSize extends ARE_ABS_Dynamic_Style<AreFontSizeSpan> {

    private AREditText mEditText;

    private int mSize = Constants.DEFAULT_FONT_SIZE;

    private boolean mIsChecked;

    public ARE_FontSize(@NonNull AREditText editText) {
        super(editText.getContext());
        this.mEditText = editText;
    }

    @Override
    public EditText getEditText() {
        return mEditText;
    }

    @Override
    protected void changeSpanInsideStyle(Editable editable, int start, int end, AreFontSizeSpan existingSpan) {
        int currentSize = existingSpan.getSize();
        if (currentSize != mSize) {
            applyNewStyle(editable, start, end, mSize);
        }
    }

    @Override
    public AreFontSizeSpan newSpan() {
        return new AreFontSizeSpan(mSize);
    }

    @Override
    public void setChecked(boolean isChecked) {
        // Do nothing.
    }

    @Override
    public boolean getIsChecked() {
        return mIsChecked;
    }

    public void apply(int fontSize) {
        mIsChecked = true;
        mSize = fontSize;
        if (null != mEditText) {
            Editable editable = mEditText.getEditableText();
            int start = mEditText.getSelectionStart();
            int end = mEditText.getSelectionEnd();

            if (end > start) {
                applyNewStyle(editable, start, end, mSize);
            }
        }
    }

    @Override
    protected void featureChangedHook(int lastSpanFontSize) {
        mSize = lastSpanFontSize;
    }

    @Override
    protected AreFontSizeSpan newSpan(int size) {
        return new AreFontSizeSpan(size);
    }
}
