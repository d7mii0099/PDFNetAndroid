package com.pdftron.pdf.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class IgnoreTopInsetFrameLayout extends FrameLayout {

    public interface IgnoreTopInsetFrameLayoutListener {
        void onVisibilityChanged(@NonNull View changedView, int visibility);
    }

    public IgnoreTopInsetFrameLayoutListener mListener;

    public void setIgnoreTopInsetFrameLayoutListener(IgnoreTopInsetFrameLayoutListener listener) {
        mListener = listener;
    }

    public IgnoreTopInsetFrameLayout(@NonNull Context context) {
        super(context);
    }

    public IgnoreTopInsetFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public IgnoreTopInsetFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
        return super.dispatchApplyWindowInsets(insets.replaceSystemWindowInsets(
                insets.getSystemWindowInsetLeft(), 0,
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()));
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        if (mListener != null) {
            mListener.onVisibilityChanged(changedView, visibility);
        }
    }
}
