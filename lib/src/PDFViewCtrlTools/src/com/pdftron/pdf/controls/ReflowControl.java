//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.webkit.WebView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.model.AnnotStyleProperty;
import com.pdftron.pdf.tools.ToolManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class provides convenient methods for interacting with {@link com.pdftron.pdf.controls.ReflowPagerAdapter} class
 * and takes care of throwing an exception if {@link com.pdftron.pdf.controls.ReflowPagerAdapter} is not set up.
 */
public class ReflowControl extends ViewPager implements ReflowPagerAdapter.ReflowPagerAdapterCallback {

    private static final String TAG = ReflowControl.class.getName();
    private static final String THROW_MESSAGE = "No PDF document has been set. Call setup(PDFDoc) or setup(PDFDoc, OnPostProcessColorListener) first.";

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;
    public static final int FOLLOW_PDFVIEWCTRL = 2;

    private ReflowPagerAdapter mReflowPagerAdapter;
    Context mContext;
    private int mOrientation = HORIZONTAL;

    private boolean mImageInReflowEnabled = true;
    private boolean mIsHideBackgroundImages = false;
    private boolean mIsHideImagesUnderText = false;
    private boolean mIsDoNotReflowTextOverImages = false;
    private boolean mIsHideImagesUnderInvisibleText = false;

    /**
     * Class constructor
     */
    public ReflowControl(Context context) {
        this(context, null);
    }

    /**
     * Class constructor
     */
    public ReflowControl(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    /**
     * Setups the reflow control
     *
     * @param pdfDoc The PDF doc
     */
    public void setup(@NonNull PDFDoc pdfDoc) {
        setup(pdfDoc, null);
    }

    /**
     * Setups the reflow control
     *
     * @param pdfDoc   The PDF doc
     * @param listener The listener for post processing colors
     */
    public void setup(@NonNull PDFDoc pdfDoc, OnPostProcessColorListener listener) {
        setup(pdfDoc, null, listener);
    }

    /**
     * Setups the reflow control
     *
     * @param pdfDoc      The PDF doc
     * @param toolManager The ToolManager for tracking edits
     * @param listener    The listener for post processing colors
     */
    public void setup(@NonNull PDFDoc pdfDoc, @Nullable ToolManager toolManager, OnPostProcessColorListener listener) {
        mReflowPagerAdapter = new ReflowPagerAdapter(this, mContext, pdfDoc);
        mReflowPagerAdapter.setImageInReflowEnabled(mImageInReflowEnabled);
        mReflowPagerAdapter.setHideImagesUnderInvisibleText(mIsHideBackgroundImages);
        mReflowPagerAdapter.setHideImagesUnderText(mIsHideImagesUnderText);
        mReflowPagerAdapter.setDoNotReflowTextOverImages(mIsDoNotReflowTextOverImages);
        mReflowPagerAdapter.setHideImagesUnderInvisibleText(mIsHideImagesUnderInvisibleText);
        mReflowPagerAdapter.setListener(this);
        mReflowPagerAdapter.setOnPostProcessColorListener(listener);
        mReflowPagerAdapter.setToolManager(toolManager);
        setAdapter(mReflowPagerAdapter);
    }

    /**
     * Sets the AnnotStyleProperties that will be used to hide elements of the AnnotStyleDialog
     */
    public void setAnnotStyleProperties(@NonNull HashMap<Integer, AnnotStyleProperty> annotStyleProperties) {
        if (mReflowPagerAdapter != null) {
            mReflowPagerAdapter.setAnnotStyleProperties(annotStyleProperties);
        }
    }

    /**
     * On Nougat and up, it is possible to edit text markups.
     * Sets whether editing text markup is enabled.
     */
    @TargetApi(Build.VERSION_CODES.N)
    public void setEditingEnabled(boolean editingEnabled) {
        if (mReflowPagerAdapter != null) {
            mReflowPagerAdapter.setEditingEnabled(editingEnabled);
        }
    }

    /**
     * Gets the paging direction
     */
    public int getOrientation() {
        return mOrientation;
    }

    /**
     * Sets the paging direction
     */
    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    /**
     * Sets whether to show images in reflow mode, default to true.
     */
    public void setImageInReflowEnabled(boolean imageInReflowEnabled) {
        this.mImageInReflowEnabled = imageInReflowEnabled;
        if (mReflowPagerAdapter != null) {
            mReflowPagerAdapter.setImageInReflowEnabled(this.mImageInReflowEnabled);
        }
    }

    /**
     * Sets whether to show background images in reflow mode, default to false.
     */
    public void setHideBackgroundImages(boolean hideBackgroundImages) {
        this.mIsHideBackgroundImages = hideBackgroundImages;
        if (mReflowPagerAdapter != null) {
            mReflowPagerAdapter.setHideBackgroundImages(this.mIsHideBackgroundImages);
        }
    }

    /**
     * Gets value to show background images in reflow mode, default to false.
     */
    public boolean getIsHideBackgroundImages() {
        return mIsHideBackgroundImages;
    }

    /**
     * Sets whether to show images under text in reflow mode, default to false.
     */
    public void setHideImagesUnderText(boolean hideImagesUnderText) {
        this.mIsHideImagesUnderText = hideImagesUnderText;
        if (mReflowPagerAdapter != null) {
            mReflowPagerAdapter.setHideImagesUnderText(this.mIsHideImagesUnderText);
        }
    }

    /**
     * Gets value to show images under text in reflow mode, default to false.
     */
    public boolean getIsHideImagesUnderText() {
        return mIsHideImagesUnderText;
    }

    /**
     * Sets whether to show text over images in reflow mode, default to false.
     */
    public void setDoNotReflowTextOverImages(boolean doNotReflowTextOverImages) {
        this.mIsDoNotReflowTextOverImages = doNotReflowTextOverImages;
        if (mReflowPagerAdapter != null) {
            mReflowPagerAdapter.setDoNotReflowTextOverImages(this.mIsDoNotReflowTextOverImages);
        }
    }

    /**
     * Gets value to show text over images in reflow mode, default to false.
     */
    public boolean getIsDoNotReflowTextOverImages() {
        return mIsDoNotReflowTextOverImages;
    }

    /**
     * Sets whether to show images under invisible text in reflow mode, default to false.
     */
    public void setHideImagesUnderInvisibleText(boolean hideImagesUnderInvisibleText) {
        this.mIsHideImagesUnderInvisibleText = hideImagesUnderInvisibleText;
        if (mReflowPagerAdapter != null) {
            mReflowPagerAdapter.setHideImagesUnderInvisibleText(this.mIsHideImagesUnderInvisibleText);
        }
    }

    /**
     * Gets value to show images under invisible text in reflow mode, default to false.
     */
    public boolean getIsHideImagesUnderInvisibleText() {
        return mIsHideImagesUnderInvisibleText;
    }

    /**
     * Checks whether the reflow control is ready
     *
     * @return True if the reflow control is ready
     */
    public boolean isReady() {
        return mReflowPagerAdapter != null;
    }

    /**
     * Notifies that pages are modified
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     *                         See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public void notifyPagesModified() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.onPagesModified();
        }
    }

    /**
     * Resets the reflow control
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     *                         See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public void reset() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.notifyDataSetChanged();
        }
    }

    public void clearAdapterCacheAndReset() {
        if (mReflowPagerAdapter != null) {
            mReflowPagerAdapter.clearCacheAndReset();
        }
    }

    /**
     * Cleans up.
     */
    public void cleanUp() {
        if (mReflowPagerAdapter != null) {
            mReflowPagerAdapter.cleanup();
        }
    }

    /**
     * Sets the text size.
     *
     * @param textSizeInPercent The text size. The possible values are
     *                          5, 10, 25, 50, 75, 100, 125, 150, 200, 40, 800, 1600
     * @throws PDFNetException if ReflowControl has not been set up.
     *                         See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public void setTextSizeInPercent(int textSizeInPercent) throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.setTextSizeInPercent(textSizeInPercent);
        }
    }

    /**
     * Returns the text size.
     *
     * @return The text size ranging from 0 to 100
     * @throws PDFNetException if ReflowControl has not been set up.
     *                         See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public int getTextSizeInPercent() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            return mReflowPagerAdapter.getTextSizeInPercent();
        }
    }

    /**
     * Update text size for current page
     */
    public void updateTextSize() {
        if (mReflowPagerAdapter != null) {
            mReflowPagerAdapter.setTextZoom();
        }
    }

    /**
     * Sets the current page.
     *
     * @param pageNum The page number (starts from 1)
     * @throws PDFNetException if ReflowControl has not been set up.
     *                         See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public void setCurrentPage(int pageNum) throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.setCurrentPage(pageNum);
        }
    }

    /**
     * Gets the current page.
     *
     * @return The page number
     * @throws PDFNetException if ReflowControl has not been set up.
     *                         See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public int getCurrentPage() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            return mReflowPagerAdapter.getCurrentPage();
        }
    }

    /**
     * Sets reflow in day mode.
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     *                         See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public void setDayMode() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.setDayMode();
        }
    }

    /**
     * Sets reflow in night mode.
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     *                         See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public void setNightMode() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.setNightMode();
        }
    }

    /**
     * Sets reflow in custom color mode.
     *
     * @param backgroundColorMode The background color
     * @throws PDFNetException if ReflowControl has not been set up.
     *                         See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public void setCustomColorMode(int backgroundColorMode) throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.setCustomColorMode(backgroundColorMode);
        }
    }

    /**
     * Checks whether reflow is in day mode.
     *
     * @return True if reflow is in day mode
     * @throws PDFNetException if ReflowControl has not been set up.
     *                         See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    @SuppressWarnings("unused")
    public boolean isDayMode() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            return mReflowPagerAdapter.isDayMode();
        }
    }

    /**
     * Checks whether reflow is in night mode.
     *
     * @return True if reflow is in night mode
     * @throws PDFNetException if ReflowControl has not been set up.
     *                         See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    @SuppressWarnings("unused")
    public boolean isNightMode() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            return mReflowPagerAdapter.isNightMode();
        }
    }

    /**
     * Checks whether reflow is in custom color mode.
     *
     * @return True if reflow is in custom color mode
     * @throws PDFNetException if ReflowControl has not been set up.
     *                         See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    @SuppressWarnings("unused")
    public boolean isCustomColorMode() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            return mReflowPagerAdapter.isCustomColorMode();
        }
    }

    /**
     * Sets right-to-left mode.
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     *                         See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public void setRightToLeftDirection(boolean isRtlMode) throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.setRightToLeftDirection(isRtlMode);
        }
    }

    /**
     * Checks whether right-to-left mode is enabled.
     *
     * @return True if right-to-left mode is enabled
     * @throws PDFNetException if ReflowControl has not been set up.
     *                         See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    @SuppressWarnings("unused")
    public boolean isRightToLeftDirection() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            return mReflowPagerAdapter.isRightToLeftDirection();
        }
    }

    /**
     * Enables turn page on tap.
     *
     * @param enabled True if should turn page on tap
     * @throws PDFNetException if ReflowControl has not been set up.
     *                         See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public void enableTurnPageOnTap(boolean enabled) throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.enableTurnPageOnTap(enabled);
        }
    }

    /**
     * Zooms in.
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     *                         See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public void zoomIn() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.zoomIn();
        }
    }

    /**
     * Zooms out.
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     *                         See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public void zoomOut() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.zoomOut();
        }
    }

    /**
     * Checks whether an internal link is clicked.
     *
     * @return True if an internal link is clicked
     * @throws PDFNetException if ReflowControl has not been set up.
     *                         See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public boolean isInternalLinkClicked() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            return mReflowPagerAdapter.isInternalLinkClicked();
        }
    }

    /**
     * Resets that an internal link is clicked.
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     *                         See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public void resetInternalLinkClicked() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.resetInternalLinkClicked();
        }
    }

    /**
     * Sets the post process color listener
     *
     * @param listener The listener to add
     * @throws PDFNetException if ReflowControl has not been set up.
     *                         See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    @SuppressWarnings("unused")
    public void setOnPostProcessColorListener(OnPostProcessColorListener listener) throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.setOnPostProcessColorListener(listener);
        }
    }

    /**
     * Adds a listener that will be invoked by {@link OnReflowTapListener}.
     * <p>Components that add a listener should take care to remove it when finished.
     * Other components that take ownership of a view may call {@link #clearReflowOnTapListeners()}
     * to remove all attached listeners.</p>
     *
     * @param listener listener to add
     */
    public void addReflowOnTapListener(OnReflowTapListener listener) {
        if (mOnTapListeners == null) {
            mOnTapListeners = new ArrayList<>();
        }
        if (!mOnTapListeners.contains(listener)) {
            mOnTapListeners.add(listener);
        }
    }

    /**
     * Removes a listener that was previously added via
     * {@link #addReflowOnTapListener(OnReflowTapListener)}.
     *
     * @param listener listener to remove
     */
    public void removeReflowOnTapListener(OnReflowTapListener listener) {
        if (mOnTapListeners != null) {
            mOnTapListeners.remove(listener);
        }
    }

    /**
     * Remove all listeners that are notified of any callback from OnTapListener.
     */
    public void clearReflowOnTapListeners() {
        if (mOnTapListeners != null) {
            mOnTapListeners.clear();
        }
    }

    /**
     * Adds a listener that will be invoked by {@link OnReflowLongPressListener}.
     * <p>Components that add a listener should take care to remove it when finished.
     * Other components that take ownership of a view may call {@link #clearReflowLongPressListeners()}
     * to remove all attached listeners.</p>
     *
     * @param listener listener to add
     */
    public void addReflowLongPressListener(OnReflowLongPressListener listener) {
        if (mLongPressListeners == null) {
            mLongPressListeners = new ArrayList<>();
        }
        if (!mLongPressListeners.contains(listener)) {
            mLongPressListeners.add(listener);
        }
    }

    /**
     * Removes a listener that was previously added via
     * {@link #addReflowLongPressListener(OnReflowLongPressListener)}.
     *
     * @param listener listener to remove
     */
    public void removeReflowLongPressListener(OnReflowLongPressListener listener) {
        if (mLongPressListeners != null) {
            mLongPressListeners.remove(listener);
        }
    }

    /**
     * Remove all listeners that are notified of any callback from OnReflowLongPressListener.
     */
    public void clearReflowLongPressListeners() {
        if (mLongPressListeners != null) {
            mLongPressListeners.clear();
        }
    }

    /**
     * Called when a single tap up event happens
     */
    @Override
    public void onReflowPagerSingleTapUp(WebView webView, MotionEvent event) {
        if (mOnTapListeners != null) {
            for (OnReflowTapListener listener : mOnTapListeners) {
                listener.onReflowSingleTapUp(event);
            }
        }
    }

    /**
     * Called when a long press event happens
     */
    @Override
    public void onReflowPagerLongPress(WebView webView, MotionEvent event) {
        if (mLongPressListeners != null) {
            for (OnReflowLongPressListener listener : mLongPressListeners) {
                listener.onReflowLongPress(webView, event);
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mOrientation == VERTICAL) {
            return false;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mOrientation == VERTICAL) {
            return false;
        }
        return super.onTouchEvent(ev);
    }

    /**
     * Sets a ReflowUrlLoadedListener for ReflowPagerAdapterCallback
     *
     * @param listener The listener
     */
    public void setReflowUrlLoadedListener(ReflowUrlLoadedListener listener) {
        if (mReflowPagerAdapter != null) {
            mReflowPagerAdapter.setReflowUrlLoadedListener(listener);
        }
    }

    /**
     * Callback interface to be invoked to get the processed color
     */
    public interface OnPostProcessColorListener {
        /**
         * The implementation should return the post-processed color.
         *
         * @param cp The original color
         * @return The output color after post processing
         */
        ColorPt getPostProcessedColor(ColorPt cp);
    }

    /**
     * Callback interface to be invoked when a single tap up gesture occurs.
     */
    public interface OnReflowTapListener {

        /**
         * Called when a single tap up gesture occurred.
         *
         * @param event The motion event
         */
        void onReflowSingleTapUp(MotionEvent event);
    }

    private List<OnReflowTapListener> mOnTapListeners;

    /**
     * Callback interface to be invoked when a single tap up gesture occurs.
     */
    public interface OnReflowLongPressListener {

        /**
         * Called when a single tap up gesture occurred.
         *
         * @param event The motion event
         */
        void onReflowLongPress(WebView webView, MotionEvent event);
    }

    private List<OnReflowLongPressListener> mLongPressListeners;

    public interface ReflowUrlLoadedListener {
        /**
         * Called when an external URL is about to be loaded.
         *
         * @param view The WebView that is initiating the callback.
         * @param url  The URL to be loaded.
         * @return True if the link was handled.
         */
        boolean onReflowExternalUrlLoaded(WebView view, String url);

        /**
         * Called when an internal URL is about to be loaded.
         *
         * @param view The WebView that is initiating the callback.
         * @param url  The URL to be loaded.
         * @return True if the link was handled.
         */
        boolean onReflowInternalUrlLoaded(WebView view, String url);
    }
}
