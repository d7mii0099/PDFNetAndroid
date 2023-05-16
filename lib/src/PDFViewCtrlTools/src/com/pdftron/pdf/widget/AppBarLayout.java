package com.pdftron.pdf.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;

import com.pdftron.pdf.utils.Utils;

/**
 * A Linear Layout that can adjust window insets in full screen
 */
public class AppBarLayout extends LinearLayout {

    public interface AppBarLayoutListener {
        void onVisibilityChanged(@NonNull View changedView, int visibility);
    }

    public AppBarLayoutListener mListener;

    public void setAppBarLayoutListener(AppBarLayoutListener listener) {
        mListener = listener;
    }

    public AppBarLayout(Context context) {
        this(context, null);
    }

    public AppBarLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppBarLayout(Context context, AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);

        if (Utils.isLollipop()) {
            setOutlineProvider(ViewOutlineProvider.PADDED_BOUNDS);
        }
    }

    /**
     * @hide
     */
    @SuppressWarnings("deprecation")
    @Override
    protected boolean fitSystemWindows(Rect insets) {
        if (!Utils.isLollipop()) {
            // Remove the bottom system window insets
            insets.bottom = 0;
        }
        return super.fitSystemWindows(insets);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        // Apply all system window insets *except* the bottom.
        WindowInsets newInsets = insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(),
                insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(),
                0);
        return super.onApplyWindowInsets(newInsets);
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        if (mListener != null) {
            mListener.onVisibilityChanged(changedView, visibility);
        }
    }
}
