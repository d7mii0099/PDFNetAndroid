package com.pdftron.richeditor.styles;

import android.text.style.StrikethroughSpan;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.pdftron.richeditor.AREditText;

public class ARE_Strikethrough extends ARE_ABS_Style<StrikethroughSpan> {

    private boolean mStrikethroughChecked;

    private AREditText mEditText;

    public ARE_Strikethrough(@NonNull AREditText editText) {
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
        mStrikethroughChecked = !mStrikethroughChecked;
        ARE_Helper.updateCheckStatus(ARE_Strikethrough.this, mStrikethroughChecked);
        if (null != mEditText) {
            applyStyle(mEditText.getEditableText(), mEditText.getSelectionStart(), mEditText.getSelectionEnd());
        }
    }

    @Override
    public void setChecked(boolean isChecked) {
        this.mStrikethroughChecked = isChecked;

        if (mEditText.getDecorationStateListener() != null) {
            mEditText.getDecorationStateListener().onStateChangeListener(AREditText.Type.STRIKETHROUGH, isChecked);
        }
    }

    @Override
    public boolean getIsChecked() {
        return this.mStrikethroughChecked;
    }

    @Override
    public StrikethroughSpan newSpan() {
        return new StrikethroughSpan();
    }
}
