package com.pdftron.pdf.widget.richtext;

import android.content.Context;
import android.util.AttributeSet;

import com.pdftron.richeditor.PTAREditText;

public class PTRichEditor extends PTAREditText {

    public PTRichEditor(Context context) {
        this(context, null);
    }

    public PTRichEditor(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PTRichEditor(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
