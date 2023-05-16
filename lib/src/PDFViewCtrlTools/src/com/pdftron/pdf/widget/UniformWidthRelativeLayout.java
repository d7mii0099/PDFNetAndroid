package com.pdftron.pdf.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class UniformWidthRelativeLayout extends RelativeLayout {
    public UniformWidthRelativeLayout(Context context) {
        super(context);
    }

    public UniformWidthRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UniformWidthRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
