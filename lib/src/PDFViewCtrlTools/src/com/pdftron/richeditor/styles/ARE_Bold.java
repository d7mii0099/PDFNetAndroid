package com.pdftron.richeditor.styles;

import android.widget.EditText;
import androidx.annotation.NonNull;

import com.pdftron.richeditor.AREditText;
import com.pdftron.richeditor.spans.AreBoldSpan;

import java.util.ArrayList;

public class ARE_Bold extends ARE_ABS_Style<AreBoldSpan> {

    private boolean mBoldChecked;

    private AREditText mEditText;

    public ARE_Bold(@NonNull AREditText editText) {
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
        mBoldChecked = !mBoldChecked;
        ARE_Helper.updateCheckStatus(this, mBoldChecked);
        if (null != mEditText) {
            applyStyle(mEditText.getEditableText(),
                    mEditText.getSelectionStart(),
                    mEditText.getSelectionEnd());
        }
    }

    @Override
    public void setChecked(boolean isChecked) {
        this.mBoldChecked = isChecked;

        if (mEditText.getDecorationStateListener() != null) {
            mEditText.getDecorationStateListener().onStateChangeListener(AREditText.Type.BOLD, isChecked);
        }
    }

    @Override
    public boolean getIsChecked() {
        return this.mBoldChecked;
    }

    @Override
    public AreBoldSpan newSpan() {
        return new AreBoldSpan();
    }
}
