package com.pdftron.pdf.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.WindowInsets;
import android.widget.RelativeLayout;

public class IgnoreTopInsetRelativeLayout extends RelativeLayout {
    public IgnoreTopInsetRelativeLayout(Context context) {
        super(context);
    }

    public IgnoreTopInsetRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IgnoreTopInsetRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
        return super.dispatchApplyWindowInsets(insets.replaceSystemWindowInsets(
                insets.getSystemWindowInsetLeft(), 0,
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()));
    }
}
