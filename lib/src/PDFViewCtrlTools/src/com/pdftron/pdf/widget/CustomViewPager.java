//------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------

package com.pdftron.pdf.widget;

import android.content.Context;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * A custom view pager that can toggle off swipe event.
 */
public class CustomViewPager extends ViewPager {

    private boolean mIsSwippingEnabled = true;

    public CustomViewPager(Context context) {
        super(context);
    }

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Sets whether this view pager can be swiped
     * @param enabled true then this view pager can be swiped, false otherwise
     */
    public void setSwippingEnabled(boolean enabled) {
        mIsSwippingEnabled = enabled;
    }

    /**
     * @param ev motion event
     * @return true if this view pager can be swipe, false otherwise
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mIsSwippingEnabled && super.onInterceptTouchEvent(ev);
    }

    /**
     * @param ev motion event
     * @return true if this view pager can be swipe, false otherwise
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return mIsSwippingEnabled && super.onTouchEvent(ev);
    }
}
