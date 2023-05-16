package com.pdftron.richeditor.styles;

import android.content.Context;
import android.widget.EditText;

public abstract class ARE_ABS_FreeStyle implements IARE_Style {

    protected Context mContext;
    protected EditText mEditText;

    public ARE_ABS_FreeStyle(Context context) {
        mContext = context;
    }

    public ARE_ABS_FreeStyle(EditText editText) {
        if (null != editText) {
            this.mContext = editText.getContext();
            this.mEditText = editText;
        }
    }

    @Override
    public EditText getEditText() {
        if (null != mEditText) {
            return mEditText;
        }
        return null;
    }

    // Dummy implementation
    @Override
    public boolean getIsChecked() {
        return false;
    }
}
