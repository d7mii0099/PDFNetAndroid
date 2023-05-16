package com.pdftron.pdf.utils;

import android.view.MotionEvent;

/**
 * A simple swipe detector for View
 */
public class SwipeDetector {

    private float mDownX, mDownY;

    private boolean mHorizontalSwipe;
    private boolean mVerticalSwipe;

    public void handleOnDown(MotionEvent ev) {
        mDownX = ev.getX();
        mDownY = ev.getY();

        mHorizontalSwipe = false;
        mVerticalSwipe = false;
    }

    public void handleOnUp(MotionEvent ev) {

        float upX = ev.getX();
        float upY = ev.getY();

        float deltaX = mDownX - upX;
        float deltaY = mDownY - upY;

        int distanceThreshold = 100;

        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            // horizontal scroll
            if (Math.abs(deltaX) > distanceThreshold) {
                mHorizontalSwipe = true;
            }
        } else {
            // vertical scroll
            if (Math.abs(deltaY) > distanceThreshold) {
                mVerticalSwipe = true;
            }
        }
    }

    public boolean isHorizontalSwipe() {
        return mHorizontalSwipe;
    }

    public boolean isVerticalSwipe() {
        return mVerticalSwipe;
    }
}
