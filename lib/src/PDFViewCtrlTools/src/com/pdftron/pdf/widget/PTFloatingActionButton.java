package com.pdftron.pdf.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;

public class PTFloatingActionButton extends com.github.clans.fab.FloatingActionButton {

    public PTFloatingActionButton(Context context) {
        this(context, null);
    }

    public PTFloatingActionButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PTFloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    public PTFloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray attr = context.obtainStyledAttributes(attrs, R.styleable.PTFloatingActionButton, defStyleAttr, 0);
        try {
            Drawable d = null;
            if (attr.hasValue(R.styleable.PTFloatingActionButton_srcCompat)) {
                final int id = attr.getResourceId(R.styleable.PTFloatingActionButton_srcCompat, -1);
                if (id != -1) {
                    d = context.getResources().getDrawable(id);
                }
            }
            if (attr.hasValue(R.styleable.PTFloatingActionButton_android_tint)) {
                int tintColor = attr.getColor(R.styleable.PTFloatingActionButton_android_tint,
                        Utils.getThemeAttrColor(context, android.R.attr.textColorPrimary));
                if (d != null) {
                    d = d.mutate();
                    d.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);
                } else {
                    setColorFilter(tintColor);
                }
            }
            if (d != null) {
                setImageDrawable(d);
            }
        } finally {
            attr.recycle();
        }
    }
}
