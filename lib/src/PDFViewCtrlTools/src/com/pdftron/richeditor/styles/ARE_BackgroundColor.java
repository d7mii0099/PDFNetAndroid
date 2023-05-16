package com.pdftron.richeditor.styles;

import android.text.style.BackgroundColorSpan;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.pdftron.richeditor.AREditText;

public class ARE_BackgroundColor extends ARE_ABS_Style<BackgroundColorSpan> {

    private boolean mBackgroundChecked;

    private int mColor;

    private AREditText mEditText;

    public ARE_BackgroundColor(@NonNull AREditText editText, int backgroundColor) {
        super(editText.getContext());
        this.mEditText = editText;
        this.mColor = backgroundColor;
    }

    public void setEditText(AREditText editText) {
        this.mEditText = editText;
    }

    @Override
    public EditText getEditText() {
        return mEditText;
    }

    public void apply() {
        mBackgroundChecked = !mBackgroundChecked;
        ARE_Helper.updateCheckStatus(ARE_BackgroundColor.this, mBackgroundChecked);
        if (null != mEditText) {
            applyStyle(mEditText.getEditableText(), mEditText.getSelectionStart(), mEditText.getSelectionEnd());
        }
    }

    @Override
    public void setChecked(boolean isChecked) {
        this.mBackgroundChecked = isChecked;
    }

    @Override
    public boolean getIsChecked() {
        return this.mBackgroundChecked;
    }

    @Override
    public BackgroundColorSpan newSpan() {
        return new BackgroundColorSpan(this.mColor);
    }
}
