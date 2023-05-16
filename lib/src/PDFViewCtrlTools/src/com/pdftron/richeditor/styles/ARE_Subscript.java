package com.pdftron.richeditor.styles;

import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.pdftron.richeditor.AREditText;
import com.pdftron.richeditor.spans.AreSubscriptSpan;

public class ARE_Subscript extends ARE_ABS_Style<AreSubscriptSpan> {

    private ImageView mSubscriptImage;

    private boolean mSubscriptChecked;

    private AREditText mEditText;

    public ARE_Subscript(@NonNull AREditText editText) {
        super(editText.getContext());
        this.mEditText = editText;
    }

    /**
     * @param editText edit text
     */
    public void setEditText(AREditText editText) {
        this.mEditText = editText;
    }

    @Override
    public EditText getEditText() {
        return mEditText;
    }

    public void apply() {
        mSubscriptChecked = !mSubscriptChecked;
        ARE_Helper.updateCheckStatus(ARE_Subscript.this, mSubscriptChecked);
        if (null != mEditText) {
            applyStyle(mEditText.getEditableText(),
                    mEditText.getSelectionStart(),
                    mEditText.getSelectionEnd());
        }
    }

    @Override
    public void setChecked(boolean isChecked) {
        this.mSubscriptChecked = isChecked;

        if (mEditText.getDecorationStateListener() != null) {
            mEditText.getDecorationStateListener().onStateChangeListener(AREditText.Type.SUBSCRIPT, isChecked);
        }
    }

    @Override
    public boolean getIsChecked() {
        return this.mSubscriptChecked;
    }

    @Override
    public AreSubscriptSpan newSpan() {
        return new AreSubscriptSpan();
    }
}
