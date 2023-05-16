//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.RelativeLayout;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.FreeText;
import com.pdftron.pdf.utils.AnnotUtils;

/**
 * a {@link RelativeLayout} inside {@link PDFViewCtrl} with
 * specified page position posX, posY, and page_num.
 * See: {@link #setPagePosition(double, double, int)}
 * <p>
 * <div class='info'>
 * The position of this layout is calculated in PDF page coordinates.
 * In page coordinate, the origin location (0, 0) is at the bottom left corner of the PDF page.
 * The x axis extends horizontally to the right and y axis extends vertically upward.
 * </div>
 */
public class CustomRelativeLayout extends RelativeLayout implements
        PDFViewCtrl.OnCanvasSizeChangeListener,
        PDFViewCtrl.PageSlidingListener {

    private static final String TAG = CustomRelativeLayout.class.getName();

    private static final boolean DEFAULT_ZOOM_WITH_PARENT = true;
    protected PDFViewCtrl mParentView;
    protected double mPagePosLeft = 0;
    protected double mPagePosRight = 0;
    protected double mPagePosTop = 0;
    protected double mPagePosBottom = 0;
    protected double[] mScreenPt1 = new double[2];
    protected double[] mScreenPt2 = new double[2];
    protected int mPageNum = 1;
    protected boolean mZoomWithParent = DEFAULT_ZOOM_WITH_PARENT;

    // take account of PDFViewCtrl translation offset
    protected int mScrollOffsetX;
    protected int mScrollOffsetY;

    protected double mScreenX1Save = 0;
    protected double mScreenY1Save = 0;
    protected double mScreenX2Save = 0;
    protected double mScreenY2Save = 0;

    /**
     * Constructor
     *
     * @param context  context of view
     * @param parent   parent view
     * @param x        x coordinates in page pt.
     * @param y        y coordinates in page pt.
     * @param page_num pdf page number
     */
    public CustomRelativeLayout(Context context, PDFViewCtrl parent, double x, double y, int page_num) {
        this(context);
        mParentView = parent;
        setPagePosition(x, y, page_num);
    }

    public CustomRelativeLayout(Context context) {
        this(context, null);
    }

    public CustomRelativeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CustomRelativeLayout, defStyleAttr, defStyleRes);
        try {
            double x = a.getFloat(R.styleable.CustomRelativeLayout_posX, 0);
            double y = a.getFloat(R.styleable.CustomRelativeLayout_posY, 0);
            int page = a.getInt(R.styleable.CustomRelativeLayout_pageNum, 1);
            setPagePosition(x, y, page);
            setZoomWithParent(a.getBoolean(R.styleable.CustomRelativeLayout_zoomWithParent, DEFAULT_ZOOM_WITH_PARENT));
        } finally {
            a.recycle();
        }
    }

    /**
     * decide if the view will zoom while parent view is zooming
     *
     * @param zoomWithParent if true, when parent view is zooming, this view will also zoom;
     *                       otherwise this view will remain same size
     */
    public void setZoomWithParent(boolean zoomWithParent) {
        mZoomWithParent = zoomWithParent;
    }

    /**
     * Sets page position of this view
     * This will only take effect if {@link #setRect(PDFViewCtrl, Rect, int)} is called already
     * <div class='info'>
     * The position of this layout is calculated in PDF page coordinates.
     * In page coordinate, the origin location (0, 0) is at the bottom left corner of the PDF page.
     * The x axis extends horizontally to the right and y axis extends vertically upward.
     * </div>
     *
     * @param x       the x coordinates in page pt
     * @param y       the y  coordinates in page pt
     * @param pageNum the page number
     */
    public void setPagePosition(double x, double y, int pageNum) {
        mPagePosLeft = x;
        mPagePosBottom = y;
        mPageNum = pageNum;
    }

    /**
     * Sets screen position of this view
     * This will only take effect if {@link #setRect(PDFViewCtrl, Rect, int)} is called already
     * <div class='info'>
     * This specifies the top left corner of the view.
     * </div>
     *
     * @param left    the left in screen pt
     * @param top     the top in screen pt
     * @param pageNum the page number
     */
    public void setScreenPosition(double left, double top, int pageNum) {
        double[] pt1 = mParentView.convScreenPtToPagePt(left, top, pageNum);
        mPagePosLeft = pt1[0];
        mPagePosTop = pt1[1];
        mPageNum = pageNum;

        requestLayout();
        invalidate();
    }

    public void setScreenRect(double left, double top, double right, double bottom, int pageNum) {
        try {
            double minX = Math.min(left, right);
            double minY = Math.min(top, bottom);
            double maxX = Math.max(left, right);
            double maxY = Math.max(top, bottom);

            // now obtain the actual bottom left point in page space
            double[] pt1 = mParentView.convScreenPtToPagePt(minX, maxY, pageNum); // convert to page coordinate system
            double[] pt2 = mParentView.convScreenPtToPagePt(maxX, minY, pageNum);
            minX = pt1[0];
            minY = pt1[1];
            maxX = pt2[0];
            maxY = pt2[1];

            setPagePosition(minX, minY, pageNum);
            mPagePosRight = maxX;
            mPagePosTop = maxY;

            setRectImpl();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * set view position and size by given annotation bounding box
     *
     * @param pdfViewCtrl  the PDFViewCtrl
     * @param annot        the annotation
     * @param annotPageNum annotation page number
     */
    public void setAnnot(PDFViewCtrl pdfViewCtrl, Annot annot, int annotPageNum) {
        if (null == pdfViewCtrl || null == annot) {
            return;
        }
        boolean shouldUnlockRead = false;
        try {
            pdfViewCtrl.docLockRead();
            shouldUnlockRead = true;

            if (!annot.isValid()) {
                return;
            }

            com.pdftron.pdf.Rect r;
            try {
                r = annot.getVisibleContentBox();
            } catch (PDFNetException ex) {
                r = annot.getRect();
            }
            if (annot.getType() == Annot.e_FreeText) {
                // first if there is a un-rotated bbox, we need to use that
                Rect bbox = AnnotUtils.getUnrotatedBBox(annot);
                if (bbox != null) {
                    r = bbox;
                } else {
                    FreeText freeText = new FreeText(annot);
                    r = freeText.getContentRect();
                }
            }
            r.normalize();

            setRect(pdfViewCtrl, r, annotPageNum);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (shouldUnlockRead) {
                pdfViewCtrl.docUnlockRead();
            }
        }
    }

    /**
     * set view position and size by given rect
     *
     * @param pdfViewCtrl the PDFViewCtrl
     * @param rect        the rect
     * @param pageNum     the page number
     */
    public void setRect(PDFViewCtrl pdfViewCtrl, com.pdftron.pdf.Rect rect, int pageNum) {
        try {
            double minX = Math.min(rect.getX1(), rect.getX2());
            double minY = Math.min(rect.getY1(), rect.getY2());
            double maxX = Math.max(rect.getX1(), rect.getX2());
            double maxY = Math.max(rect.getY1(), rect.getY2());

            mParentView = pdfViewCtrl;

            // we need to use screen points to determine min/max due to rotation
            double[] pt1 = mParentView.convPagePtToScreenPt(minX, minY, pageNum);
            double[] pt2 = mParentView.convPagePtToScreenPt(maxX, maxY, pageNum);

            double screenMinX = Math.min(pt1[0], pt2[0]);
            double screenMinY = Math.min(pt1[1], pt2[1]);
            double screenMaxX = Math.max(pt1[0], pt2[0]);
            double screenMaxY = Math.max(pt1[1], pt2[1]);

            pt1 = mParentView.convScreenPtToPagePt(screenMinX, screenMaxY, pageNum); // convert to page coordinate system
            pt2 = mParentView.convScreenPtToPagePt(screenMaxX, screenMinY, pageNum);
            minX = pt1[0];
            minY = pt1[1];
            maxX = pt2[0];
            maxY = pt2[1];

            setPagePosition(minX, minY, pageNum);
            mPagePosRight = maxX;
            mPagePosTop = maxY;

            setRectImpl();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setRectImpl() {
        double[] screenBounds = mParentView.convPagePtToHorizontalScrollingPt(mPagePosRight, mPagePosTop, mPageNum);
        mScreenPt1 = mParentView.convPagePtToHorizontalScrollingPt(mPagePosLeft, mPagePosBottom, mPageNum);

        int width = (int) (Math.abs(screenBounds[0] - mScreenPt1[0]) + .5);
        int height = (int) (Math.abs(screenBounds[1] - mScreenPt1[1]) + .5);

        measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        setLayoutParams(new ViewGroup.LayoutParams(width, height));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ViewParent parent = getParent();
        if (parent instanceof PDFViewCtrl) {
            mParentView = (PDFViewCtrl) parent;
            mParentView.addOnCanvasSizeChangeListener(this);
            mParentView.addPageSlidingListener(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mParentView != null) {
            mParentView.removeOnCanvasSizeChangeListener(this);
            mParentView.removePageSlidingListener(this);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mParentView == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (mParentView.isDuringFling()) {
            // with PDFViewCtrl's fling prediction, the end value is obtained from core right away
            // so here we want to preserve the original value to ensure smooth fling
            mScreenPt1[0] = mScreenX1Save;
            mScreenPt1[1] = mScreenY1Save;
        } else {
            mScreenPt1 = mParentView.convPagePtToHorizontalScrollingPt(mPagePosLeft, mPagePosBottom, mPageNum);
            mScreenX1Save = mScreenPt1[0];
            mScreenY1Save = mScreenPt1[1];
        }
        if (mZoomWithParent) {
            if (mParentView.isDuringFling()) {
                mScreenPt2[0] = mScreenX2Save;
                mScreenPt2[1] = mScreenY2Save;
            } else {
                mScreenPt2 = mParentView.convPagePtToHorizontalScrollingPt(mPagePosRight, mPagePosTop, mPageNum);
                mScreenX2Save = mScreenPt2[0];
                mScreenY2Save = mScreenPt2[1];
            }

            width = (int) (Math.abs(mScreenPt2[0] - mScreenPt1[0]) + .5);
            height = (int) (Math.abs(mScreenPt2[1] - mScreenPt1[1]) + .5);
            int nextWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            int nextHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            super.onMeasure(nextWidthMeasureSpec, nextHeightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        if (mParentView.isMaintainZoomEnabled()) {
            int dx = (mParentView.isCurrentSlidingCanvas(mPageNum) ? 0 : mScrollOffsetX) - mParentView.getScrollXOffsetInTools(mPageNum);
            int dy = (mParentView.isCurrentSlidingCanvas(mPageNum) ? 0 : mScrollOffsetY) - mParentView.getScrollYOffsetInTools(mPageNum);

            setTranslationX(-dx);
            setTranslationY(dy);
        }

        int l = (int) mScreenPt1[0];
        int t = (int) mScreenPt1[1] - height;
        int r = (int) mScreenPt1[0] + width;
        int b = (int) mScreenPt1[1];

        layout(l, t, r, b);
    }

    @Override
    public void onCanvasSizeChanged() {
        measure(getMeasuredWidthAndState(), getMeasuredHeightAndState());
        requestLayout();
    }

    @Override
    public void onScrollOffsetChanged(int x, int y) {
        if (mParentView.isMaintainZoomEnabled()) {
            mScrollOffsetX = x;
            mScrollOffsetY = y;
            requestLayout();
        }
    }
}
