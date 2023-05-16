package com.pdftron.richeditor.styles;

import android.widget.EditText;

import androidx.annotation.NonNull;

import com.pdftron.richeditor.AREditText;
import com.pdftron.richeditor.spans.AreItalicSpan;

public class ARE_Italic extends ARE_ABS_Style<AreItalicSpan> {

    private boolean mItalicChecked;

    private AREditText mEditText;

    public ARE_Italic(@NonNull AREditText editText) {
        super(editText.getContext());
        this.mEditText = editText;
    }

    public void setEditText(AREditText editText) {
        this.mEditText = editText;
    }

    @Override
    public EditText getEditText() {
        return mEditText;
    }

    public void apply() {
        mItalicChecked = !mItalicChecked;
        ARE_Helper.updateCheckStatus(ARE_Italic.this, mItalicChecked);
        if (null != mEditText) {
            applyStyle(mEditText.getEditableText(), mEditText.getSelectionStart(), mEditText.getSelectionEnd());
        }
    }

    @Override
    public void setChecked(boolean isChecked) {
        this.mItalicChecked = isChecked;

        if (mEditText.getDecorationStateListener() != null) {
            mEditText.getDecorationStateListener().onStateChangeListener(AREditText.Type.ITALIC, isChecked);
        }
    }

    @Override
    public boolean getIsChecked() {
        return this.mItalicChecked;
    }

    @Override
    public AreItalicSpan newSpan() {
        return new AreItalicSpan();
    }
}
