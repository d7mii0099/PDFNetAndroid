package com.pdftron.demo.widget;

import android.content.Context;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * FrameLayout that moves upwards when a Snackbar is shown.
 */
@CoordinatorLayout.DefaultBehavior(MoveUpwardBehaviour.class)
public class MoveUpwardFrameLayout extends FrameLayout {

    public MoveUpwardFrameLayout(Context context) {
        super(context);
    }

    public MoveUpwardFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MoveUpwardFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
