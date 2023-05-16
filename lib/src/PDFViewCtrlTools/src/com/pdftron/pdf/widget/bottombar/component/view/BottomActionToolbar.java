package com.pdftron.pdf.widget.bottombar.component.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.pdftron.pdf.widget.toolbar.builder.AnnotationToolbarBuilder;
import com.pdftron.pdf.widget.toolbar.component.AnnotationToolbarTheme;
import com.pdftron.pdf.widget.toolbar.component.view.ActionToolbar;

class BottomActionToolbar extends ActionToolbar {
    public BottomActionToolbar(Context context) {
        super(context);
    }

    public BottomActionToolbar(Context context, AnnotationToolbarTheme annotToolbarTheme) {
        super(context, annotToolbarTheme);
    }

    public BottomActionToolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BottomActionToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BottomActionToolbar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void inflateWithBuilder(AnnotationToolbarBuilder builder) {
        super.inflateWithBuilder(builder);
        setFillWidth();
    }

    private void setFillWidth() {
        // Remove the ActionMenuView from the scrollable view and add to parent frame layout
        mScrollView.removeAllViews();
        mScrollView.setVisibility(GONE);

        // Adjust layout params so that the items are evenly spaced out
        ConstraintLayout.LayoutParams layoutParamsContainer = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT);
        mToolbarViewContainer.setLayoutParams(layoutParamsContainer);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        mToolbarActions.setLayoutParams(layoutParams);
        mToolbarViewContainer.addView(mToolbarActions);
    }
}
