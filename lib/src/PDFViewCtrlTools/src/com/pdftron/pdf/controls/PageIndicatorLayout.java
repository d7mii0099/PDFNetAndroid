//------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------
package com.pdftron.pdf.controls;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.card.MaterialCardView;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.dialog.pagelabel.PageLabelUtils;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;

/**
 * A LinearLayout that shows page indicator. It includes current page number and total page number.
 */
public class PageIndicatorLayout extends LinearLayout implements PDFViewCtrl.PageChangeListener, View.OnAttachStateChangeListener {

    private static final String TAG = PageIndicatorLayout.class.getName();
    private TextView mIndicator;
    private ProgressBar mSpinner;
    private PDFViewCtrl mPDFViewCtrl;
    private View mMainView;
    private boolean mAutoAdjustPosition = false;
    private boolean mHasAttachedToWindow = false;
    private ViewTreeObserver.OnGlobalLayoutListener mPdfViewCtrlLayoutListener;
    private int mGravity = Gravity.BOTTOM | Gravity.START;
    private int mPdfViewCtrlVisibility = VISIBLE;
    private OnPDFViewVisibilityChanged mPDFVisibilityChangeListener;

    public static final int POSITION_BOTTOM_START = 0;
    public static final int POSITION_TOP_START = 1;
    public static final int POSITION_TOP_END = 2;

    /**
     * Class constructor
     */
    public PageIndicatorLayout(Context context) {
        this(context, null);
    }

    /**
     * Class constructor
     */
    public PageIndicatorLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Class constructor
     */
    public PageIndicatorLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * Class constructor for API > 21
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PageIndicatorLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mMainView = LayoutInflater.from(getContext()).inflate(R.layout.view_page_indicator, this);
        mIndicator = findViewById(R.id.page_number_indicator_all_pages);
        mSpinner = findViewById(R.id.page_number_indicator_spinner);
        // Set theme colors
        Theme theme = Theme.fromContext(getContext());
        MaterialCardView cardView = findViewById(R.id.card_view);
        cardView.setCardBackgroundColor(theme.backgroundColor);
        cardView.setStrokeColor(theme.outlineColor);
        mIndicator.setTextColor(theme.textColor);

        setVisibility(GONE);
        setClickable(false);
        if (Utils.isJellyBeanMR1()) {
            mIndicator.setTextDirection(View.TEXT_DIRECTION_LTR);
        }
        mPdfViewCtrlLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (mPDFViewCtrl == null) {
                    return;
                }
                if (mPDFViewCtrl.getVisibility() != mPdfViewCtrlVisibility && mPDFVisibilityChangeListener != null) {
                    // notify pdfViewCtrl visibility changed
                    mPDFVisibilityChangeListener.onPDFViewVisibilityChanged(mPdfViewCtrlVisibility, mPDFViewCtrl.getVisibility());
                    mPdfViewCtrlVisibility = mPDFViewCtrl.getVisibility();
                }
            }
        };
    }

    private void addPdfViewCtrlListeners() {
        if (mPDFViewCtrl != null) {
            mPDFViewCtrl.addPageChangeListener(this);
            mPdfViewCtrlVisibility = mPDFViewCtrl.getVisibility();
            mPDFViewCtrl.getViewTreeObserver().addOnGlobalLayoutListener(mPdfViewCtrlLayoutListener);
            mPDFViewCtrl.addOnAttachStateChangeListener(this);
        }
    }

    /**
     * Sets PDFViewCtrl
     *
     * @param pdfViewCtrl The PDFViewCtrl
     */
    public void setPdfViewCtrl(PDFViewCtrl pdfViewCtrl) {
        mPDFViewCtrl = pdfViewCtrl;
        if (mHasAttachedToWindow) {
            autoAdjustPosition();
            addPdfViewCtrlListeners();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        autoAdjustPosition();
        addPdfViewCtrlListeners();
        mHasAttachedToWindow = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mPDFViewCtrl != null) {
            mPDFViewCtrl.removePageChangeListener(this);
            mPDFViewCtrl.getViewTreeObserver().removeOnGlobalLayoutListener(mPdfViewCtrlLayoutListener);
            mPDFViewCtrl.removeOnAttachStateChangeListener(this);
        }
    }

    /**
     * This is called when there is a page changes in {@link PDFViewCtrl}
     *
     * @param old_page the old page number
     * @param cur_page the current (new) page number
     * @param state    in non-continuous page presentation modes and when the
     *                 built-in page sliding is in process, this flag is used to
     */
    @Override
    public void onPageChange(int old_page, int cur_page, PDFViewCtrl.PageChangeState state) {
        setCurrentPage(cur_page);
    }

    public void setCurrentPage(int currentPage) {
        String pageLabel = PageLabelUtils.getPageNumberIndicator(mPDFViewCtrl, currentPage);
        mIndicator.setText(pageLabel);
    }

    /**
     * Gets page indicator text view
     *
     * @return page indicator text view
     */
    public TextView getIndicator() {
        return mIndicator;
    }

    /**
     * Gets the loading spinner
     *
     * @return loading spinner
     */
    public ProgressBar getSpinner() {
        return mSpinner;
    }

    /**
     * Sets whether to auto adjust position when there is layout changes
     *
     * @param autoAdjust true then auto adjust position, false otherwise
     */
    public void setAutoAdjustPosition(boolean autoAdjust) {
        mAutoAdjustPosition = autoAdjust;
    }

    private void autoAdjustPosition() {
        if (mPDFViewCtrl == null || !mAutoAdjustPosition) {
            return;
        }
        int[] position = calculateAutoAdjustPosition();
        setX(position[0]);
        setY(position[1]);
    }

    /**
     * Calculates the position of page indicator if it is going to adjust position automatically.
     *
     * @return This page indicator position
     */
    public int[] calculateAutoAdjustPosition() {
        int[] result = new int[2];
        if (mMainView.getLayoutParams() == null || !(mMainView.getLayoutParams() instanceof MarginLayoutParams)) {
            return result;
        }
        // adjust position so it will align pdfviewctrl layout
        measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        MarginLayoutParams mlp = (MarginLayoutParams) mMainView.getLayoutParams();

        int y;
        int verticalGravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
        switch (verticalGravity) {
            case Gravity.TOP:
                y = mPDFViewCtrl.getTop() + mlp.topMargin;
                break;
            case Gravity.CENTER_VERTICAL:
                y = mPDFViewCtrl.getTop() + mPDFViewCtrl.getHeight() / 2 - getMeasuredHeight() / 2 + mlp.topMargin;
                break;
            default:
                y = mPDFViewCtrl.getBottom() - getMeasuredHeight() - mlp.bottomMargin;
                break;
        }

        result[1] = y;

        int x;
        int gravity = Utils.isJellyBeanMR1() ? Gravity.getAbsoluteGravity(mGravity, getLayoutDirection()) : mGravity;
        int horizontalGravity = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        switch (horizontalGravity) {
            case Gravity.RIGHT:
                x = mPDFViewCtrl.getRight() - getMeasuredWidth()
                        - mlp.leftMargin - mlp.rightMargin;
                break;
            case Gravity.CENTER_HORIZONTAL:
                x = mPDFViewCtrl.getLeft() + mPDFViewCtrl.getWidth() / 2 - getMeasuredWidth() / 2 + mlp.leftMargin;
                break;
            default:
                x = mPDFViewCtrl.getLeft() + mlp.leftMargin;
                break;
        }
        result[0] = x;
        return result;
    }

    @Override
    public void setGravity(int gravity) {
        super.setGravity(gravity);
        mGravity = gravity;
    }

    /**
     * Sets {@link PDFViewCtrl} visibility change listener
     *
     * @param listener PDFViewCtrl visibility change listener
     */
    public void setOnPdfViewCtrlVisibilityChangeListener(OnPDFViewVisibilityChanged listener) {
        mPDFVisibilityChangeListener = listener;
    }

    @Override
    public void onViewAttachedToWindow(View v) {
        if (mPDFVisibilityChangeListener != null) {
            mPDFVisibilityChangeListener.onPDFViewVisibilityChanged(View.GONE, mPDFViewCtrl.getVisibility());
        }
    }

    @Override
    public void onViewDetachedFromWindow(View v) {
        if (mPDFVisibilityChangeListener != null) {
            mPDFVisibilityChangeListener.onPDFViewVisibilityChanged(mPDFViewCtrl.getVisibility(), View.GONE);
        }
    }

    /**
     * Listener for PDFViewCtrl visibility change event
     */
    public interface OnPDFViewVisibilityChanged {
        /**
         * This method will be invoked when PDFViewCtrl visibility has changed
         *
         * @param prevVisibility previous PDFViewCtrl visibility
         * @param currVisibility current PDFViewCtrl visibility
         */
        void onPDFViewVisibilityChanged(int prevVisibility, int currVisibility);
    }

    static class Theme {
        @ColorInt
        final int backgroundColor;
        @ColorInt
        final int textColor;
        @ColorInt
        final int outlineColor;

        Theme(int backgroundColor, int textColor, int outlineColor) {
            this.backgroundColor = backgroundColor;
            this.textColor = textColor;
            this.outlineColor = outlineColor;
        }

        @NonNull
        static Theme fromContext(@NonNull Context context) {

            final TypedArray a = context.obtainStyledAttributes(
                    null, R.styleable.PageIndicatorTheme, R.attr.pt_page_indicator_style, R.style.PTPageIndicatorTheme);
            int backgroundColor = a.getColor(R.styleable.PageIndicatorTheme_backgroundColor, context.getResources().getColor(R.color.page_number_indicator_bg));
            int textColor = a.getColor(R.styleable.PageIndicatorTheme_textColor, context.getResources().getColor(R.color.tools_pageindicator_number));
            int outlineColor = a.getColor(R.styleable.PageIndicatorTheme_outlineColor, context.getResources().getColor(R.color.page_number_indicator_outline));
            a.recycle();

            return new Theme(backgroundColor, textColor, outlineColor);
        }
    }
}
