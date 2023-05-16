package com.pdftron.richeditor.styles;

import android.widget.EditText;

import androidx.annotation.NonNull;

import com.pdftron.richeditor.AREditText;
import com.pdftron.richeditor.spans.AreUnderlineSpan;

public class ARE_Underline extends ARE_ABS_Style<AreUnderlineSpan> {

    private boolean mUnderlineChecked;

    private AREditText mEditText;

    public ARE_Underline(@NonNull AREditText editText) {
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
        mUnderlineChecked = !mUnderlineChecked;
        ARE_Helper.updateCheckStatus(ARE_Underline.this, mUnderlineChecked);
        if (null != mEditText) {
            applyStyle(mEditText.getEditableText(), mEditText.getSelectionStart(), mEditText.getSelectionEnd());
        }
    }

    @Override
    public void setChecked(boolean isChecked) {
        this.mUnderlineChecked = isChecked;

        if (mEditText.getDecorationStateListener() != null) {
            mEditText.getDecorationStateListener().onStateChangeListener(AREditText.Type.UNDERLINE, isChecked);
        }
    }

    @Override
    public boolean getIsChecked() {
        return this.mUnderlineChecked;
    }

    @Override
    public AreUnderlineSpan newSpan() {
        return new AreUnderlineSpan();
    }
}
