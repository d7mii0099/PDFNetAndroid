package com.pdftron.richeditor.styles;

import android.widget.EditText;

import androidx.annotation.NonNull;

import com.pdftron.richeditor.AREditText;
import com.pdftron.richeditor.spans.AreSuperscriptSpan;

public class ARE_Superscript extends ARE_ABS_Style<AreSuperscriptSpan> {

    private boolean mSuperscriptChecked;

    private AREditText mEditText;

    public ARE_Superscript(@NonNull AREditText editText) {
        super(editText.getContext());
        this.mEditText = editText;
    }

    /**
     * @param editText
     */
    public void setEditText(AREditText editText) {
        this.mEditText = editText;
    }

    @Override
    public EditText getEditText() {
        return mEditText;
    }

    public void apply() {
        mSuperscriptChecked = !mSuperscriptChecked;
        ARE_Helper.updateCheckStatus(ARE_Superscript.this, mSuperscriptChecked);
        if (null != mEditText) {
            applyStyle(mEditText.getEditableText(),
                    mEditText.getSelectionStart(),
                    mEditText.getSelectionEnd());
        }
    }

    @Override
    public void setChecked(boolean isChecked) {
        this.mSuperscriptChecked = isChecked;

        if (mEditText.getDecorationStateListener() != null) {
            mEditText.getDecorationStateListener().onStateChangeListener(AREditText.Type.SUPERSCRIPT, isChecked);
        }
    }

    @Override
    public boolean getIsChecked() {
        return this.mSuperscriptChecked;
    }

    @Override
    public AreSuperscriptSpan newSpan() {
        return new AreSuperscriptSpan();
    }
}
