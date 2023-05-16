//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.webkit.WebView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.pdf.controls.ReflowControl;
import com.pdftron.pdf.tools.R;

import io.reactivex.disposables.Disposable;

/**
 * WebView for Reflow.
 */
public class ReflowWebView extends WebView {

    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetector mGestureDetector;

    // for vertical scrolling
    private int mOrientation;
    private float mFlingThreshSpeed;
    private boolean mPageTop;
    private boolean mPageBottom;
    private boolean mDone;
    @Nullable
    private Disposable mDisposable;

    public void setDisposable(@NonNull Disposable disposable) {
        dispose();
        mDisposable = disposable;
    }

    public void dispose() {
        if (mDisposable != null && !mDisposable.isDisposed()) {
            mDisposable.dispose();
            mDisposable = null;
        }
    }

    /**
     * Callback interface to be invoked when a gesture occurs.
     */
    public interface ReflowWebViewCallback {

        /**
         * Called when a scale gesture begins.
         *
         * @param detector The {@link ScaleGestureDetector}
         * @return True if handled
         */
        boolean onReflowWebViewScaleBegin(WebView webView, ScaleGestureDetector detector);

        /**
         * Called when user scales.
         *
         * @param detector The {@link ScaleGestureDetector}
         * @return True if handled
         */
        boolean onReflowWebViewScale(WebView webView, ScaleGestureDetector detector);

        /**
         * Called when a scale gesture ends.
         *
         * @param detector The {@link ScaleGestureDetector}
         */
        void onReflowWebViewScaleEnd(WebView webView, ScaleGestureDetector detector);

        /**
         * Called when a tap occurs with the up event.
         *
         * @param event The {@link MotionEvent}
         */
        void onReflowWebViewSingleTapUp(WebView webView, MotionEvent event);

        /**
         * Called when a long press event occurs.
         *
         * @param event The {@link MotionEvent}
         */
        void onReflowWebViewLongPress(WebView webView, MotionEvent event);

        /**
         * Called when the top of this WebView is reached.
         */
        void onPageTop(WebView webView);

        /**
         * Called when the bottom of this WebView is reached.
         */
        void onPageBottom(WebView webView);
    }

    public interface TextSelectionCallback {
        boolean onMenuItemClick(WebView webView, MenuItem item);
    }

    private ReflowWebViewCallback mCallback;
    private TextSelectionCallback mTextSelectionCallback;

    /**
     * Sets the {@link ReflowWebViewCallback} listener
     *
     * @param listener The listener
     */
    public void setListener(ReflowWebViewCallback listener) {
        mCallback = listener;
    }

    public void setTextSelectionCallback(TextSelectionCallback listener) {
        mTextSelectionCallback = listener;
    }

    /**
     * Class constructor
     */
    public ReflowWebView(Context context) {
        super(context);
        init(context);
    }

    /**
     * Class constructor
     */
    public ReflowWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        mGestureDetector = new GestureDetector(getContext(), new TapListener());

        mFlingThreshSpeed = Utils.convDp2Pix(context, 1000);
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    public boolean isVertical() {
        return mOrientation == ReflowControl.VERTICAL;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);
        if (mGestureDetector != null) {
            mGestureDetector.onTouchEvent(ev);
        }
        if (mScaleGestureDetector != null) {
            mScaleGestureDetector.onTouchEvent(ev);
        }

        return true;
    }

    private void detectPageEnds() {
        if (mOrientation != ReflowControl.VERTICAL) {
            return;
        }
        mPageTop = false;
        mPageBottom = false;
        if (this.computeVerticalScrollRange() <= (this.computeVerticalScrollOffset() +
                this.computeVerticalScrollExtent())) {
            mPageBottom = true;
        }
        if (getScrollY() == 0) {
            mPageTop = true;
        }
    }

    private void onPageBottom() {
        if (mDone) {
            return;
        }
        if (mCallback != null) {
            mCallback.onPageBottom(this);
        }
        mDone = true;
    }

    private void onPageTop() {
        if (mDone) {
            return;
        }
        if (mCallback != null) {
            mCallback.onPageTop(this);
        }
        mDone = true;
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback) {
        ActionMode actionMode = super.startActionMode(callback);
        return resolveActionMode(actionMode);
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback, int type) {
        ActionMode actionMode = super.startActionMode(callback, type);
        return resolveActionMode(actionMode);
    }

    private ActionMode resolveActionMode(ActionMode actionMode) {
        if (mTextSelectionCallback == null) {
            return actionMode;
        }
        if (actionMode != null) {
            final Menu menu = actionMode.getMenu();
            // Add our text markup menu items
            actionMode.getMenuInflater().inflate(R.menu.reflow, menu);

            // Then loop through all menu items to change order
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                if (isTextMarkupMenu(item)) {
                    item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                } else {
                    item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                }
            }
            for (int i = 0; i < menu.size(); i++) {
                MenuItem menuItem = menu.getItem(i);
                if (isTextMarkupMenu(menuItem)) {
                    menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if (isTextMarkupMenu(item)) {
                                boolean intercept = EventHandler.sendPreEvent(EventHandler.REFLOW_TEXT_MARKUP);
                                if (!intercept) {
                                    if (mTextSelectionCallback != null) {
                                        return mTextSelectionCallback.onMenuItemClick(ReflowWebView.this, item);
                                    }
                                }
                            }
                            return false;
                        }
                    });
                }
            }
            actionMode.invalidate();
        }
        return actionMode;
    }

    private boolean isTextMarkupMenu(@NonNull MenuItem item) {
        return item.getItemId() == R.id.qm_highlight ||
                item.getItemId() == R.id.qm_underline ||
                item.getItemId() == R.id.qm_strikeout ||
                item.getItemId() == R.id.qm_squiggly;
    }

    private class TapListener implements GestureDetector.OnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            if (mCallback != null) {
                mCallback.onReflowWebViewSingleTapUp(ReflowWebView.this, event);
            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent event) {
            mDone = false;
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                float distanceY) {
            if (mOrientation == ReflowControl.VERTICAL) {
                detectPageEnds();
            }
            return true;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                float velocityX, float velocityY) {
            if (mOrientation == ReflowControl.VERTICAL) {
                if (Math.abs(velocityY) > mFlingThreshSpeed) {
                    if (velocityY < 0) {
                        if (mPageBottom) {
                            onPageBottom();
                        }
                    } else {
                        if (mPageTop) {
                            onPageTop();
                        }
                    }
                }
                detectPageEnds();
            }
            return true;
        }

        @Override
        public void onShowPress(MotionEvent event) {
        }

        @Override
        public void onLongPress(MotionEvent event) {
            if (mCallback != null) {
                mCallback.onReflowWebViewLongPress(ReflowWebView.this, event);
            }
        }
    }

    private class ScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return mCallback == null || mCallback.onReflowWebViewScaleBegin(ReflowWebView.this, detector);
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return mCallback == null || mCallback.onReflowWebViewScale(ReflowWebView.this, detector);
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (mCallback != null) {
                mCallback.onReflowWebViewScaleEnd(ReflowWebView.this, detector);
            }
        }
    }
}
