package com.pdftron.pdf.widget.seekbar;

import android.animation.Animator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.controls.MirrorSeekBar;
import com.pdftron.pdf.controls.PageIndicatorLayout;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;

public class DocumentSlider extends RelativeLayout implements
        PDFViewCtrl.DocumentLoadListener,
        PDFViewCtrl.PageChangeListener,
        PDFViewCtrl.OnCanvasSizeChangeListener {

    /**
     * Callback interface to be invoked when a tracking touch event occurs.
     */
    public interface OnDocumentSliderTrackingListener {
        /**
         * Called when a tracking touch on seek bar has started.
         */
        void onDocumentSliderStartTrackingTouch();

        /**
         * Called when the tracking touch on seek bar has been stopped.
         *
         * @param pageNum The current page number on seek bar
         */
        void onDocumentSliderStopTrackingTouch(int pageNum);
    }

    private MirrorSeekBar mSeekBar;
    private PDFViewCtrl mPdfViewCtrl;

    private PageIndicatorLayout mPageIndicatorLayout;
    private boolean mCanShowPageIndicator = true;

    private int mPageCount;
    private int mCurrentPage;
    private int mPDFViewCtrlId;
    private boolean mIsProgressChanging;
    private boolean mReflowMode;

    private OnDocumentSliderTrackingListener mListener;

    private DocumentSliderChip mDocumentSliderChip;

    /**
     * Class constructor
     */
    public DocumentSlider(Context context) {
        this(context, null);
    }

    /**
     * Class constructor
     */
    public DocumentSlider(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.pt_document_slider_style);
    }

    /**
     * Class constructor
     */
    public DocumentSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, R.style.DocumentSliderStyle);
    }

    /**
     * Class constructor
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public DocumentSlider(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mPageCount = 1;
        mIsProgressChanging = false;
        mListener = null;

        View layout = inflateLayout(context);
        mSeekBar = findViewById(R.id.controls_thumbnail_slider_scrubberview_seekbar);
        boolean showGuideline = PdfViewCtrlSettingsManager.getShowScrollbarGuideline(context);
        mSeekBar.setInteractThumbOnly(!showGuideline);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsProgressChanging = true;
                if (mListener != null) {
                    mListener.onDocumentSliderStartTrackingTouch();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mIsProgressChanging = false;
                if (mListener != null) {
                    mListener.onDocumentSliderStopTrackingTouch(mCurrentPage);
                }
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateSeekBarGroup(seekBar, progress);
                if (fromUser) {
                    setProgress(progress);
                }
                if (mPdfViewCtrl != null) {
                    mCurrentPage = mPdfViewCtrl.getCurrentPage();
                }
                if (mPageIndicatorLayout != null && isVertical() && mReflowMode) {
                    // in vertical reflow mode, the page number callback is not triggered from PDFViewCtrl
                    mPageIndicatorLayout.setCurrentPage(mCurrentPage);
                }
            }
        });

        // add chip
        mDocumentSliderChip = new DocumentSliderChip(context);
        addView(mDocumentSliderChip);

        mDocumentSliderChip.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        ShapeDrawable shape = new ShapeDrawable(new RectShape());
        //DEBUG
        shape.getPaint().setColor(Color.TRANSPARENT);
//        shape.getPaint().setColor(Color.BLUE);
//        shape.getPaint().setAlpha(127);
        shape.setIntrinsicWidth(mDocumentSliderChip.getMeasuredWidth());
        shape.setIntrinsicHeight(mDocumentSliderChip.getMeasuredHeight());
        mSeekBar.setThumb(shape);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.DocumentSlider, defStyleAttr, defStyleRes);
        try {
            mPDFViewCtrlId = typedArray.getResourceId(R.styleable.DocumentSlider_pdfviewctrlId, -1);

            // set seekbar color
            int seekbarColor = typedArray.getColor(R.styleable.DocumentSlider_seekbarColor, Color.BLACK);
            Drawable progressDrawable = mSeekBar.getProgressDrawable();
            if (showGuideline) {
                progressDrawable.setColorFilter(seekbarColor, PorterDuff.Mode.SRC_IN);
            } else {
                progressDrawable.setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.SRC_IN);
            }
            // the actual seek bar color is on the chip
            mDocumentSliderChip.setIconTint(seekbarColor);

            // background color
            int backgroundColor = typedArray.getColor(R.styleable.DocumentSlider_colorBackground, Color.TRANSPARENT);
            layout.setBackgroundColor(Color.TRANSPARENT);
            // the actual seek bar color is on the chip
            mDocumentSliderChip.setCardBackground(backgroundColor);

            // orientation
            int orientation = typedArray.getInt(R.styleable.DocumentSlider_android_orientation, LinearLayout.HORIZONTAL);
            if (orientation == LinearLayout.VERTICAL) {
                mSeekBar.setVertical(true);
                mDocumentSliderChip.setVertical(true);
            } else {
                mSeekBar.setVertical(false);
                mDocumentSliderChip.setVertical(false);
            }
        } finally {
            typedArray.recycle();
        }
    }

    private int getSeekBarPos(SeekBar seekBar, int progress) {
        int width, thumbPos;
        if (mSeekBar.isVertical()) {
            width = seekBar.getHeight() - seekBar.getPaddingTop() - seekBar.getPaddingBottom();
            double ratio = progress / (float) seekBar.getMax();
            thumbPos = seekBar.getPaddingTop() + (int) (width * ratio + .5);
        } else {
            width = seekBar.getWidth() - seekBar.getPaddingLeft() - seekBar.getPaddingRight();
            double ratio = progress / (float) seekBar.getMax();
            if (mPdfViewCtrl.getRightToLeftLanguage()) {
                thumbPos = seekBar.getRight() - (int) (width * ratio + .5) - seekBar.getPaddingLeft();
            } else {
                thumbPos = seekBar.getPaddingLeft() + (int) (width * ratio + .5);
            }
        }
        return thumbPos;
    }

    private void updateSeekBarGroup(SeekBar seekBar, int progress) {
        Context context = seekBar.getContext();
        int thumbPos = getSeekBarPos(seekBar, progress);
        // update chip
        int marginStart = thumbPos - mDocumentSliderChip.getWidth() / 2 + seekBar.getLeft();
        int posX, posY;
        if (mSeekBar.isVertical()) {
            // bound
            int chipOffset = context.getResources().getDimensionPixelSize(R.dimen.document_seek_bar_chip_offset_vert);
            marginStart = marginStart + chipOffset;
            marginStart = Math.min(marginStart, getHeight() - mDocumentSliderChip.getHeight());
            marginStart = Math.max(0, marginStart);
            posX = 0;
            posY = marginStart;
        } else {
            // bound
            marginStart = Math.min(marginStart, getWidth() - mDocumentSliderChip.getWidth());
            marginStart = Math.max(0, marginStart);
            posX = marginStart;
            posY = 0;
        }
        mDocumentSliderChip.setX(posX);
        mDocumentSliderChip.setY(posY);

        if (mPageIndicatorLayout != null) {
            // update page indicator
            if (mSeekBar.isVertical()) {
                int chipCenter = marginStart + mDocumentSliderChip.getHeight() / 2;
                if (Utils.isRtlLayout(context)) {
                    posX = posX + mDocumentSliderChip.getWidth();
                } else {
                    posX = getLeft() - mPageIndicatorLayout.getWidth();
                }
                posY = chipCenter - mPageIndicatorLayout.getHeight() / 2;
            } else {
                int[] coord = new int[2];
                mSeekBar.getLocationInWindow(coord);
                int chipCenter = marginStart + mDocumentSliderChip.getWidth() / 2;
                posX = chipCenter - mPageIndicatorLayout.getWidth() / 2 + coord[0];
                posY = getTop() - mPageIndicatorLayout.getHeight();
            }
            mPageIndicatorLayout.setX(posX);
            mPageIndicatorLayout.setY(posY);
        }
    }

    protected View inflateLayout(@NonNull Context context) {
        return LayoutInflater.from(context).inflate(R.layout.view_document_seek_bar, this);
    }

    /**
     * This method will free any bitmaps and other resources that are used internally by the
     * control. Only call this method when the control is not going to be used anymore (e.g., on the
     * onDestroy() of your Activity).
     */
    public void clearResources() {
        if (mPdfViewCtrl != null) {
            mPdfViewCtrl.removeDocumentLoadListener(this);
            mPdfViewCtrl.removePageChangeListener(this);
            mPdfViewCtrl.removeOnCanvasSizeChangeListener(this);
        }
    }

    public boolean isVertical() {
        return mSeekBar.isVertical();
    }

    private boolean useScrollPosition() {
        return mSeekBar.isVertical() && ViewerUtils.isContinuousPageMode(mPdfViewCtrl) && !mReflowMode;
    }

    public void setProgress(int progress) {
        if (null == mPdfViewCtrl) {
            return;
        }
        if (useScrollPosition()) {
            mPdfViewCtrl.setScrollY(progress);
        } else {
            mPdfViewCtrl.setCurrentPage(progress + 1); // page is 1 indexed
        }
    }

    public void updateProgress() {
        if (null == mPdfViewCtrl || !mPdfViewCtrl.isValid() || null == mSeekBar) {
            return;
        }
        syncProgress();
        updateSeekBarGroup(mSeekBar, mSeekBar.getProgress());
    }

    private void syncProgress() {
        if (useScrollPosition()) {
            mSeekBar.setProgress(mPdfViewCtrl.getScrollY());
        } else {
            mSeekBar.setProgress(mPdfViewCtrl.getCurrentPage() - 1); // page is 1 indexed
        }
    }

    /**
     * Sets the PDFViewCtrl.
     *
     * @param pdfViewCtrl The PDFViewCtrl
     */
    public void setPdfViewCtrl(PDFViewCtrl pdfViewCtrl) {
        if (pdfViewCtrl == null) {
            throw new NullPointerException("pdfViewCtrl can't be null");
        }
        mPdfViewCtrl = pdfViewCtrl;
        mSeekBar.setPdfViewCtrl(mPdfViewCtrl);
        mPdfViewCtrl.addDocumentLoadListener(this);
        mPdfViewCtrl.addPageChangeListener(this);
        mPdfViewCtrl.addOnCanvasSizeChangeListener(this);
    }

    @Deprecated
    public void setPageIndicatorLayout(PageIndicatorLayout pageIndicatorLayout) {
        if (pageIndicatorLayout == null) {
            throw new NullPointerException("PageIndicatorLayout can't be null");
        }
        mPageIndicatorLayout = pageIndicatorLayout;
    }

    @Deprecated
    public void setCanShowPageIndicator(boolean canShow) {
        mCanShowPageIndicator = canShow;
    }

    /**
     * Sets the seek bar listener.
     *
     * @param listener The listener
     */
    public void setOnDocumentSliderTrackingListener(OnDocumentSliderTrackingListener listener) {
        mListener = listener;
    }

    /**
     * @return True if progress is changing
     */
    public boolean isProgressChanging() {
        return mIsProgressChanging;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.DocumentLoadListener#onDocumentLoaded()}.
     */
    @Override
    public void onDocumentLoaded() {
        handleDocumentLoaded();
    }

    /**
     * Handles when the document is loaded.
     */
    public void handleDocumentLoaded() {
        refreshPageCount();
        updateProgress();
    }

    public void setReflowMode(boolean reflowMode) {
        mReflowMode = reflowMode;
        refreshPageCount();
    }

    /**
     * Refreshes the page count
     */
    public void refreshPageCount() {
        if (mPdfViewCtrl != null && mPdfViewCtrl.getDoc() != null) {
            mPageCount = 0;
            boolean shouldUnlockRead = false;
            try {
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;
                mPageCount = mPdfViewCtrl.getDoc().getPageCount();
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlockRead) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }

            if (mPageCount <= 0) {
                mPageCount = 1;
            }
            int seekBarMax;
            if (useScrollPosition()) {
                seekBarMax = (int) (mPdfViewCtrl.getCanvasHeight() - mPdfViewCtrl.getHeight() + .5);
            } else {
                seekBarMax = mPageCount - 1; // page is 1 indexed
            }
            if (seekBarMax <= 0) {
                seekBarMax = 1;
            }
            mSeekBar.setMax(seekBarMax);

            if (mPageCount == 1) {
                mSeekBar.setVisibility(GONE);
                mDocumentSliderChip.setVisibility(GONE);
            } else {
                mSeekBar.setVisibility(VISIBLE);
            }
        }
    }

    /**
     * @return True if the seek bar is reversed
     */
    public boolean isReversed() {
        return mSeekBar.isReversed();
    }

    /**
     * Reverses the seek bar.
     *
     * @param isReversed True if the seek bar should be reversed
     */
    public void setReversed(boolean isReversed) {
        mSeekBar.setReversed(isReversed);
        mSeekBar.invalidate();
    }

    public void show() {
        show(true);
    }

    public void show(boolean isAnimate) {
        handleDocumentLoaded();
        if (isAnimate) {
            animateSeekBarChip(true);
            animatePageIndicator(true);
        } else {
            setVisibility(VISIBLE);
            if (mDocumentSliderChip != null) {
                mDocumentSliderChip.setVisibility(VISIBLE);
            }
            if (mPageIndicatorLayout != null && mCanShowPageIndicator) {
                mPageIndicatorLayout.setVisibility(VISIBLE);
            }
        }
    }

    public void dismiss() {
        dismiss(true);
    }

    public void dismiss(boolean isAnimate) {
        if (isAnimate) {
            animateSeekBarChip(false);
            animatePageIndicator(false);
        } else {
            if (mDocumentSliderChip != null) {
                mDocumentSliderChip.animate().cancel();
                mDocumentSliderChip.setVisibility(GONE);
            }
            if (mPageIndicatorLayout != null) {
                mPageIndicatorLayout.animate().cancel();
                mPageIndicatorLayout.setVisibility(GONE);
            }
            setVisibility(GONE);
        }
    }

    /**
     * If it is has pdfViewCtrlId and current {@link #mPdfViewCtrl} is null,
     * set pdfViewCtrl when attached to window
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mPdfViewCtrl == null && mPDFViewCtrlId != -1) {
            View pdfView = getRootView().findViewById(mPDFViewCtrlId);
            if (pdfView instanceof PDFViewCtrl) {
                setPdfViewCtrl((PDFViewCtrl) pdfView);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        clearResources();
    }

    @Override
    public void onPageChange(int old_page, int cur_page, PDFViewCtrl.PageChangeState state) {
        refreshPageCount();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (getVisibility() != VISIBLE) {
            return;
        }

        updateProgress();
    }

    @Override
    public void onCanvasSizeChanged() {
        if (getVisibility() != VISIBLE) {
            return;
        }
        if (mSeekBar != null && mSeekBar.isVertical()) {
            // PDFViewCtrl size changed,
            // update scrollable region
            handleDocumentLoaded();
        }
    }

    private void animateSeekBarChip(final boolean show) {
        Runnable action = new Runnable() {
            @Override
            public void run() {
                setVisibility(show ? VISIBLE : GONE);
                mDocumentSliderChip.setVisibility(show ? VISIBLE : GONE);
            }
        };
        if (isVertical()) {
            animateVerticalChip(show, action);
        } else {
            animateView(mDocumentSliderChip, show, action);
        }
    }

    private void animatePageIndicator(final boolean show) {
        if (mPageIndicatorLayout == null) {
            return;
        }
        animateView(mPageIndicatorLayout, show, new Runnable() {
            @Override
            public void run() {
                mPageIndicatorLayout.setVisibility((show && mCanShowPageIndicator) ? VISIBLE : GONE);
            }
        });
    }

    private void animateVerticalChip(boolean show, @NonNull final Runnable onAction) {
        if (null == mDocumentSliderChip) {
            return;
        }
        if (show) {
            mDocumentSliderChip.setAlpha(0);
            mDocumentSliderChip.animate()
                    .alpha(1.0f)
                    .setDuration(200)
                    .setInterpolator(new DecelerateInterpolator())
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            onAction.run();
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {
                        }
                    });
        } else {
            int destX = Utils.isRtlLayout(getContext()) ? -mDocumentSliderChip.getWidth() : mDocumentSliderChip.getWidth();
            mDocumentSliderChip.animate()
                    .x(destX)
                    .alpha(0)
                    .setDuration(200)
                    .setInterpolator(new AccelerateInterpolator())
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            onAction.run();
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {
                        }
                    });
        }
    }

    // Starts the show/hide page indicator animation
    private void animateView(final View view, boolean show, @NonNull final Runnable onAction) {
        if (null == view) {
            return;
        }
        if (show) {
            view.setScaleX(0);
            view.setScaleY(0);
            view.setAlpha(0);
            view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(200)
                    .setInterpolator(new DecelerateInterpolator())
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            onAction.run();
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {
                        }
                    });
        } else {
            view.animate()
                    .scaleX(0)
                    .scaleY(0)
                    .alpha(0)
                    .setDuration(200)
                    .setInterpolator(new AccelerateInterpolator())
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            onAction.run();
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {
                        }
                    });
        }
    }
}