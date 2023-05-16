package com.pdftron.pdf.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

public class WrapContentViewPager extends ViewPager {

    private int mContentHeight = -1;

    private final ViewPager.SimpleOnPageChangeListener mSimpleOnPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            requestLayout();
        }
    };

    public WrapContentViewPager(@NonNull Context context) {
        super(context);
    }

    public WrapContentViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        addOnPageChangeListener(mSimpleOnPageChangeListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        removeOnPageChangeListener(mSimpleOnPageChangeListener);
    }

    public void setContentHeight(int height) {
        mContentHeight = height;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mode = MeasureSpec.getMode(heightMeasureSpec);
        // Unspecified means that the ViewPager is in a ScrollView WRAP_CONTENT.
        // At Most means that the ViewPager is not in a ScrollView WRAP_CONTENT.
        if (mode == MeasureSpec.UNSPECIFIED || mode == MeasureSpec.AT_MOST) {
            // super has to be called in the beginning so the child views can be initialized.
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            int height = 0;
            // first find tab layout height
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child instanceof TabLayout && child.getVisibility() != GONE) {
                    child.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
                    height += child.getMeasuredHeight();
                    break;
                }
            }
            // then find tab content height
            if (mContentHeight != -1) {
                height += mContentHeight;
            } else {
                // measuring here is less accurate in response to child view's visibility change
                // only as a fallback if content is not measured externally
                if (getChildCount() > (getCurrentItem() + 1)) {
                    View child = getChildAt(getCurrentItem() + 1); // first item is tab layout
                    if (child != null) {
                        child.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
                        height += child.getMeasuredHeight();
                    }
                }
            }

            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        }
        // super has to be called again so the new specs are treated as exact measurements
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
