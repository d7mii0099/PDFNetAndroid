package com.pdftron.pdf.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;

public class InertWebView extends WebView {
    public InertWebView(Context context) {
        super(context);
    }

    public InertWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InertWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }
}
