package com.pdftron.pdf.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.CurvePainter;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.model.LineEndingStyle;
import com.pdftron.pdf.model.LineStyle;
import com.pdftron.pdf.model.RotateInfo;
import com.pdftron.pdf.model.RulerItem;
import com.pdftron.pdf.model.ShapeBorderStyle;
import com.pdftron.pdf.tools.AnnotEditAdvancedShape;
import com.pdftron.pdf.tools.FreeTextCreate;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.DrawingUtils;
import com.pdftron.pdf.utils.InlineEditText;
import com.pdftron.pdf.utils.RotationUtils;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import static com.pdftron.pdf.tools.AnnotEdit.e_ll;
import static com.pdftron.pdf.tools.AnnotEdit.e_lm;
import static com.pdftron.pdf.tools.AnnotEdit.e_lr;
import static com.pdftron.pdf.tools.AnnotEdit.e_ml;
import static com.pdftron.pdf.tools.AnnotEdit.e_mr;
import static com.pdftron.pdf.tools.AnnotEdit.e_ul;
import static com.pdftron.pdf.tools.AnnotEdit.e_um;
import static com.pdftron.pdf.tools.AnnotEdit.e_ur;

public class AnnotView extends RelativeLayout {

    private ViewGroup mView;

    private AnnotDrawingView mDrawingView;

    private PTCropImageView mCropImageView;

    private InlineEditText mTextEditor;

    private AnnotViewImpl mAnnotViewImpl;
    private RotationImpl mRotationImpl;

    private boolean mDelayViewRemoval;

    private long mCurvePainterId;

    private SelectionHandleView mTopLeft;
    private SelectionHandleView mTopMiddle;
    private SelectionHandleView mTopRight;
    private SelectionHandleView mMiddleLeft;
    private SelectionHandleView mMiddleRight;
    private SelectionHandleView mBottomLeft;
    private SelectionHandleView mBottomMiddle;
    private SelectionHandleView mBottomRight;

    private double mZoom;

    private ArrayList<SelectionHandleView> mSelectionHandleViews;

    private int mSelectionHandleRadius;

    private SelectionHandleView mActiveHandle;

    public AnnotView(Context context) {
        this(context, null);
    }

    public AnnotView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnnotView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void setCanDraw(boolean canDraw) {
        mDrawingView.setCanDraw(canDraw);
    }

    public boolean getCanDraw() {
        return mDrawingView.getCanDraw();
    }

    public long getCurvePainterId() {
        return mCurvePainterId;
    }

    public void setPage(int pageNum) {
        if (mAnnotViewImpl != null) {
            mAnnotViewImpl.mPageNum = pageNum;
        }
    }

    public void setAnnotStyle(PDFViewCtrl pdfViewCtrl, AnnotStyle annotStyle) {
        mAnnotViewImpl.setAnnotStyle(pdfViewCtrl, annotStyle);
        mDrawingView.setAnnotStyle(mAnnotViewImpl);
        mDrawingView.setVisibility(VISIBLE);
    }

    public void setInlineEditText(@NonNull InlineEditText inlineEditText) {
        mDrawingView.setVisibility(GONE);

        mTextEditor = inlineEditText;
        mTextEditor.getEditText().setEnabled(false);
        mTextEditor.getEditText().setFocusable(false);
        mTextEditor.getEditText().setFocusableInTouchMode(false);
        mTextEditor.getEditText().setCursorVisible(false);
        mTextEditor.getEditText().setVerticalScrollBarEnabled(false);

        if (mAnnotViewImpl != null && mAnnotViewImpl.mAnnotStyle != null) {
            if (mAnnotViewImpl.mAnnotStyle.getAnnotType() == Annot.e_FreeText) {
                if (!((ToolManager) mAnnotViewImpl.mPdfViewCtrl.getToolManager()).isAutoResizeFreeText()) {
                    mTextEditor.getEditText().setHorizontalTextAlignment(mAnnotViewImpl.mAnnotStyle.getHorizontalAlignment());
                    mTextEditor.getEditText().setVerticalTextAlignment(mAnnotViewImpl.mAnnotStyle.getVerticalAlignment());
                }
            }
            mTextEditor.setContents(mAnnotViewImpl.mAnnotStyle.getTextContent());

            if (Utils.isLollipop() && mAnnotViewImpl.mAnnotStyle.isSpacingFreeText()) {
                mTextEditor.getEditText().setLetterSpacing(mAnnotViewImpl.mAnnotStyle.getLetterSpacing());
                mTextEditor.getEditText().addLetterSpacingHandle();
            } else {
                setWillNotDraw(false);
                invalidate();
            }
        }
    }

    public void setCurvePainter(long id, CurvePainter curvePainter) {
        mCurvePainterId = id;
        mDrawingView.setCurvePainter(curvePainter);
    }

    public void setAnnotBitmap(Bitmap bitmap) {
        mDrawingView.setAnnotBitmap(bitmap);

        mCropImageView.setImageBitmap(bitmap);
        mCropImageView.setZoom(mZoom);
        mCropImageView.setCropRectPercentageMargins(new RectF(0, 0, 0, 0));
    }

    public void removeView() {
        if (mTextEditor != null && mTextEditor.delayViewRemoval()) {
            mTextEditor.removeView();
            mTextEditor = null;
        }
    }

    public void prepareRemoval() {
        if (mTextEditor != null) {
            mTextEditor.close(!mDelayViewRemoval);
        }
    }

    public void setDelayViewRemoval(boolean delayViewRemoval) {
        mDelayViewRemoval = delayViewRemoval;
        if (delayViewRemoval) {
            mAnnotViewImpl.removeCtrlPts();
            invalidate();

            setSelectionHandleVisible(false);
        }
    }

    public boolean isDelayViewRemoval() {
        return mDelayViewRemoval;
    }

    public AnnotDrawingView getDrawingView() {
        return mDrawingView;
    }

    @Nullable
    public AutoScrollEditText getTextView() {
        return mTextEditor != null ? mTextEditor.getEditText() : null;
    }

    public PTCropImageView getCropImageView() {
        return mCropImageView;
    }

    public void setZoom(double zoom) {
        mZoom = zoom;
        mDrawingView.setZoom(zoom);
        if (mTextEditor != null) {
            mTextEditor.getEditText().setZoom(zoom);
        }
    }

    public void setAnnotUIRotation(int rotation) {
        if (mAnnotViewImpl != null) {
            mAnnotViewImpl.mAnnotUIRotation = rotation;
        }
    }

    public int getAnnotUIRotation() {
        if (mAnnotViewImpl != null) {
            return mAnnotViewImpl.mAnnotUIRotation;
        }
        return 0;
    }

    public void setAnnotRotation(int rotation) {
        if (mAnnotViewImpl != null) {
            mAnnotViewImpl.mAnnotRotation = rotation;
        }
    }

    public int getAnnotRotation() {
        if (mAnnotViewImpl != null) {
            return mAnnotViewImpl.mAnnotRotation;
        }
        return 0;
    }

    public void setPageNum(int pageNum) {
        mDrawingView.setPageNum(pageNum);
    }

    public void setHasPermission(boolean hasPermission) {
        mAnnotViewImpl.mHasSelectionPermission = hasPermission;
    }

    public void setActiveHandle(int which) {
        if (mAnnotViewImpl.isAnnotEditLine() || mAnnotViewImpl.isAnnotEditAdvancedShape()) {
            if (which >= 0 && mSelectionHandleViews != null && which < mSelectionHandleViews.size()) {
                mActiveHandle = mSelectionHandleViews.get(which);
            } else {
                mActiveHandle = null;
            }
        } else {
            switch (which) {
                case e_ll:
                    mActiveHandle = mBottomLeft;
                    break;
                case e_lm:
                    mActiveHandle = mBottomMiddle;
                    break;
                case e_lr:
                    mActiveHandle = mBottomRight;
                    break;
                case e_mr:
                    mActiveHandle = mMiddleRight;
                    break;
                case e_ur:
                    mActiveHandle = mTopRight;
                    break;
                case e_um:
                    mActiveHandle = mTopMiddle;
                    break;
                case e_ul:
                    mActiveHandle = mTopLeft;
                    break;
                case e_ml:
                    mActiveHandle = mMiddleLeft;
                    break;
                default:
                    mActiveHandle = null;
                    break;
            }
        }
        animateActiveHandle();
    }

    public void setCtrlPts(PointF[] pts) {
        mAnnotViewImpl.mCtrlPts = pts;

        layoutSelectionHandle(pts);
    }

    public void setAnnotRect(@Nullable RectF rect) {
        if (null == rect) {
            return;
        }
        RectF unrotatedRect = null;
        if (mAnnotViewImpl.mAnnotStyle.getAnnotType() == Annot.e_FreeText) {
            if (mAnnotViewImpl.mAnnotUIRotation != 0) {
                try {
                    unrotatedRect = RotationUtils.getUnrotatedDimensionsFromBBoxRectF(
                            new Rect(rect.left, rect.top, rect.right, rect.top + rect.height()),
                            mAnnotViewImpl.mAnnotRotation);
                } catch (Exception ignored) {
                }
            }
            if (unrotatedRect != null) {
                mAnnotViewImpl.mAnnotRectF.set(unrotatedRect);
            }
        } else {
            mAnnotViewImpl.mAnnotRectF.set(rect);
        }

        mAnnotViewImpl.mBBox.set(rect);
        mAnnotViewImpl.mPt1.set(rect.left, rect.top);
        mAnnotViewImpl.mPt2.set(rect.right, rect.bottom);
        if (unrotatedRect != null) {
            mDrawingView.setAnnotRect(unrotatedRect);
        } else {
            mDrawingView.setAnnotRect(rect);
        }

        if (mTextEditor != null) {
            // since we will layout the text view as a subview with offset,
            // we need to remove the offset for spacing text view
            mAnnotViewImpl.mPt1.set(0, 0);
            mAnnotViewImpl.mPt2.set(rect.width(), rect.height());
            if (unrotatedRect != null) {
                mAnnotViewImpl.mPt2.set(unrotatedRect.width(), unrotatedRect.height());
            }
        }

        layoutSubViews();
    }

    public void setVertices(PointF... points) {
        if (mAnnotViewImpl.mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_RECT_AREA_MEASURE) {
            // for rect area measurement, we are trying to resize a polygon like a rectangle,
            // so transform is used instead of vertices after initial points are set
            if (mAnnotViewImpl.mVertices.isEmpty()) {
                mAnnotViewImpl.setVertices(points);
            }
        } else {
            if (mAnnotViewImpl.mVertices.size() == 2 && points.length == 2) {
                mAnnotViewImpl.mVertices.set(0, points[0]);
                mAnnotViewImpl.mVertices.set(1, points[1]);
            } else {
                mAnnotViewImpl.setVertices(points);
            }
        }
        layoutSelectionHandle(points);
    }

    public void updateVertices(int index, PointF point) {
        if (index >= mAnnotViewImpl.mVertices.size()) {
            return;
        }
        mAnnotViewImpl.mVertices.set(index, point);

        layoutSelectionHandle(mAnnotViewImpl.mVertices.toArray(new PointF[0]));
    }

    public void snapToDegree(@Nullable Integer degree, float startDegree) {
        if (mRotationImpl != null) {
            mRotationImpl.snapToDegree(degree, startDegree);
            mDrawingView.invalidate();
            if (mTextEditor != null) {
                mTextEditor.getEditor().rotateToDegree();
                invalidate();
            }
        }
    }

    public RotateInfo handleRotation(PointF downPt, PointF movePt, boolean done) {
        if (mRotationImpl == null) {
            mRotationImpl = new RotationImpl(mAnnotViewImpl);
            mDrawingView.setRotateImpl(mRotationImpl);
            if (mTextEditor != null) {
                mTextEditor.getEditor().setRotateImpl(mRotationImpl);
            }
        }
        if (mTopLeft != null && mTopLeft.getVisibility() == VISIBLE) {
            setSelectionHandleVisible(false);
        }
        RotateInfo rotateInfo = mRotationImpl.handleRotation(downPt, movePt, done);
        mDrawingView.invalidate();
        if (mTextEditor != null) {
            mTextEditor.getEditor().rotateToDegree();
        }
        return rotateInfo;
    }

    public void snapToPerfectShape(@Nullable SnapMode mode) {
        mAnnotViewImpl.mSnapMode = mode;
        invalidate();
    }

    public void updateTextColor(int textColor) {
        if (mTextEditor != null) {
            mTextEditor.getEditText().updateTextColor(textColor);
        }
    }

    public void updateTextSize(float textSize) {
        if (mTextEditor != null) {
            mTextEditor.getEditText().updateTextSize(textSize);
        }
    }

    public void updateAlignment(int horizontalAlignment, int verticalAlignment) {
        if (mTextEditor != null) {
            mTextEditor.getEditText().setHorizontalTextAlignment(horizontalAlignment);
            mTextEditor.getEditText().setVerticalTextAlignment(verticalAlignment);
            mTextEditor.getEditText().setGravity(horizontalAlignment | verticalAlignment); // force the layout to update graivty
        }
    }

    public void updateColor(int color) {
        mDrawingView.updateColor(color);
        if (mTextEditor != null) {
            mTextEditor.getEditText().updateColor(color);
        }
    }

    public void updateFillColor(int color) {
        mDrawingView.updateFillColor(color);
        if (mTextEditor != null) {
            mTextEditor.getEditText().updateFillColor(color);
        }
    }

    public void updateThickness(float thickness) {
        mDrawingView.updateThickness(thickness);
        if (mTextEditor != null) {
            mTextEditor.getEditText().updateThickness(thickness);
        }
    }

    public void updateOpacity(float opacity) {
        mDrawingView.updateOpacity(opacity);
        if (mTextEditor != null) {
            mTextEditor.getEditText().updateOpacity(opacity);
        }
    }

    public void updateIcon(String icon) {
        mDrawingView.updateIcon(icon);
    }

    public void updateFont(FontResource font) {
        if (mTextEditor != null) {
            mTextEditor.getEditText().updateFont(font);
        }
    }

    public void updateRulerItem(RulerItem rulerItem) {
        mDrawingView.updateRulerItem(rulerItem);
    }

    public void updateBorderStyle(ShapeBorderStyle borderStyle) {
        mDrawingView.updateBorderStyle(borderStyle);
    }

    public void updateLineStyle(LineStyle lineStyle) {
        mDrawingView.updateLineStyle(lineStyle);
    }

    public void updateLineStartStyle(LineEndingStyle lineStartStyle) {
        mDrawingView.updateLineStartStyle(lineStartStyle);
    }

    public void updateLineEndStyle(LineEndingStyle lineEndStyle) {
        mDrawingView.updateLineEndStyle(lineEndStyle);
    }

    public void setCropMode(boolean cropMode) {
        if (cropMode) {
            mCropImageView.setVisibility(View.VISIBLE);
            mDrawingView.setVisibility(View.GONE);
            setSelectionHandleVisible(false);
        } else {
            mDrawingView.setVisibility(View.VISIBLE);
            mCropImageView.setVisibility(View.GONE);
            setSelectionHandleVisible(true);
        }
        layoutSubViews();
    }

    public boolean isCropMode() {
        return mCropImageView.getVisibility() == View.VISIBLE;
    }

    private void init(Context context) {
        mAnnotViewImpl = new AnnotViewImpl(context);

        mView = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.annot_view_layout, null);
        mDrawingView = mView.findViewById(R.id.drawing_view);
        mCropImageView = mView.findViewById(R.id.image_crop_view);

        mSelectionHandleRadius = getResources().getDimensionPixelSize(R.dimen.selection_widget_size_w_margin) / 2;

        addView(mView);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mTextEditor != null) {
            // draw selection box
            if (mRotationImpl == null || !mRotationImpl.mRotated) {
                drawSelectionBox(canvas);
            }
            // draw rotation guideline
            if (mRotationImpl != null && mRotationImpl.mSnapDegree != null) {
                DrawingUtils.drawGuideline(mRotationImpl.mSnapDegree, mAnnotViewImpl.mRotateCenterRadius,
                        canvas, mAnnotViewImpl.mBBox, mAnnotViewImpl.mGuidelinePath, mAnnotViewImpl.mGuidelinePaint);
            }
            // draw snap guideline
            if (mAnnotViewImpl.mSnapMode != null) {
                DrawingUtils.drawGuideline(mAnnotViewImpl.mSnapMode,
                        mAnnotViewImpl.mGuidelinExtend, canvas, mAnnotViewImpl.mBBox, mAnnotViewImpl.mGuidelinePath, mAnnotViewImpl.mGuidelinePaint);
            }

            // draw annotation position snapping guideline
            if (mAnnotViewImpl.mAnnotPositionSnappingGuidelines != null && !mAnnotViewImpl.mAnnotPositionSnappingGuidelines.isEmpty()) {
                for (Pair<Point, Point> annotPositionSnappingGuideline : mAnnotViewImpl.mAnnotPositionSnappingGuidelines) {
                    DrawingUtils.drawGuideline(
                            canvas,
                            mAnnotViewImpl.mGuidelinePath,
                            annotPositionSnappingGuideline.first.x - mAnnotViewImpl.mPdfViewCtrl.getScrollX(),
                            annotPositionSnappingGuideline.first.y - mAnnotViewImpl.mPdfViewCtrl.getScrollY(),
                            annotPositionSnappingGuideline.second.x - mAnnotViewImpl.mPdfViewCtrl.getScrollX(),
                            annotPositionSnappingGuideline.second.y - mAnnotViewImpl.mPdfViewCtrl.getScrollY(),
                            mAnnotViewImpl.mGuidelinePaint
                    );
                }
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        mView.layout(0, 0, r - l, b - t);
        mDrawingView.layout(0, 0, r - l, b - t);

        layoutSubViews();
    }

    @Override
    public void invalidate() {
        super.invalidate();

        if (mTextEditor != null) {
            mTextEditor.getEditText().invalidate();
        }
        if (VISIBLE == mCropImageView.getVisibility()) {
            mCropImageView.invalidate();
        }
        mDrawingView.invalidate();
    }

    private void drawSelectionBox(Canvas canvas) {
        if (!mAnnotViewImpl.mCanDrawCtrlPts) {
            return;
        }
        if (!mAnnotViewImpl.mHasSelectionPermission) {
            return;
        }
        float left = mAnnotViewImpl.mCtrlPts[e_ul].x;
        float top = mAnnotViewImpl.mCtrlPts[e_ul].y;
        float right = mAnnotViewImpl.mCtrlPts[e_lr].x;
        float bottom = mAnnotViewImpl.mCtrlPts[e_lr].y;
        DrawingUtils.drawSelectionBox(mAnnotViewImpl.mCtrlPtsPaint, getContext(),
                canvas, left, top, right, bottom, mAnnotViewImpl.mHasSelectionPermission);
    }

    private void layoutSubViews() {
        if (mTextEditor != null && mAnnotViewImpl != null) {
            boolean autoResize = ((ToolManager) mAnnotViewImpl.mPdfViewCtrl.getToolManager()).isAutoResizeFreeText();
            if (mTextEditor.getEditText().getDynamicLetterSpacingEnabled() ||
                    (mAnnotViewImpl.mAnnotStyle.isBasicFreeText() && autoResize && FreeTextCreate.sUseEditTextAppearance)) {
                // here we want to extend to the bottom right of the screen for text view spacing
                mTextEditor.getEditor().setScreenPosition(
                        mAnnotViewImpl.mAnnotRectF.left,
                        mAnnotViewImpl.mAnnotRectF.top,
                        mAnnotViewImpl.mPageNum);
            } else if (mAnnotViewImpl.mAnnotStyle.isBasicFreeText() && FreeTextCreate.sUseEditTextAppearance) {
                mTextEditor.getEditor().setScreenRect(
                        mAnnotViewImpl.mAnnotRectF.left,
                        mAnnotViewImpl.mAnnotRectF.top,
                        mAnnotViewImpl.mAnnotRectF.right,
                        mAnnotViewImpl.mAnnotRectF.bottom,
                        mAnnotViewImpl.mPageNum);
            } else if (((ToolManager) mAnnotViewImpl.mPdfViewCtrl.getToolManager()).isTextFieldCustomAppearanceEnabled() && mAnnotViewImpl.isAnnotWidget()) {
                // Assuming widget is TextField here
                mTextEditor.getEditor().setScreenRect(
                        mAnnotViewImpl.mAnnotRectF.left,
                        mAnnotViewImpl.mAnnotRectF.top,
                        mAnnotViewImpl.mAnnotRectF.right,
                        mAnnotViewImpl.mAnnotRectF.bottom,
                        mAnnotViewImpl.mPageNum);
            }
        }
        if (VISIBLE == mCropImageView.getVisibility()) {
            mCropImageView.layout(mAnnotViewImpl.mAnnotRect.left - mCropImageView.getPaddingLeft(),
                    mAnnotViewImpl.mAnnotRect.top - mCropImageView.getPaddingTop(),
                    mAnnotViewImpl.mAnnotRect.right + mCropImageView.getPaddingRight(),
                    mAnnotViewImpl.mAnnotRect.bottom + mCropImageView.getPaddingBottom());
        }
    }

    public void layoutSelectionHandle(PointF[] pts) {
        if (pts == null) {
            return;
        }
        if (null == mAnnotViewImpl) {
            return;
        }
        if (!mAnnotViewImpl.mCanDrawCtrlPts) {
            return;
        }
        if (!mAnnotViewImpl.mHasSelectionPermission) {
            return;
        }
        if (!mAnnotViewImpl.isAnnotResizable()) {
            // do not draw control points for annotation that is not resizable
            return;
        }

        if (mAnnotViewImpl.isAnnotEditLine() || mAnnotViewImpl.isAnnotEditAdvancedShape()) {
            selectionHandleAnnotEditAdvancedShape(pts);
        } else {
            selectionHandleAnnotEdit(pts);
        }
        invalidate();
    }

    private void selectionHandleAnnotEditAdvancedShape(PointF[] pts) {
        int len = pts.length;
        if (null == mSelectionHandleViews) {
            mSelectionHandleViews = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                SelectionHandleView v = new SelectionHandleView(getContext());
                mView.addView(v);
                mSelectionHandleViews.add(v);
            }
        }
        for (int i = 0; i < mSelectionHandleViews.size(); i++) {
            SelectionHandleView v = mSelectionHandleViews.get(i);
            if (mAnnotViewImpl.isCallout() && i == AnnotEditAdvancedShape.CALLOUT_END_POINT_INDEX) {
                // for callout we want to skip the end point (3rd point)
                v.setVisibility(GONE);
                continue;
            }
            PointF p = pts[i];
            if (p != null) {
                v.setVisibility(VISIBLE);
                v.layout((int) (p.x + .5) - mSelectionHandleRadius,
                        (int) (p.y + .5) - mSelectionHandleRadius,
                        (int) (p.x + .5) + mSelectionHandleRadius,
                        (int) (p.y + .5) + mSelectionHandleRadius);
            } else {
                v.setVisibility(GONE);
            }
        }
    }

    private void addSelectionWidget() {
        if (null == mSelectionHandleViews) {
            mSelectionHandleViews = new ArrayList<>(8);
        }
        if (null == mTopLeft) {
            mTopLeft = new SelectionHandleView(getContext());
            mView.addView(mTopLeft);
            mSelectionHandleViews.add(mTopLeft);
        }
        if (null == mTopMiddle) {
            mTopMiddle = new SelectionHandleView(getContext());
            addView(mTopMiddle);
            mSelectionHandleViews.add(mTopMiddle);
        }
        if (null == mTopRight) {
            mTopRight = new SelectionHandleView(getContext());
            addView(mTopRight);
            mSelectionHandleViews.add(mTopRight);
        }
        if (null == mMiddleLeft) {
            mMiddleLeft = new SelectionHandleView(getContext());
            addView(mMiddleLeft);
            mSelectionHandleViews.add(mMiddleLeft);
        }
        if (null == mMiddleRight) {
            mMiddleRight = new SelectionHandleView(getContext());
            addView(mMiddleRight);
            mSelectionHandleViews.add(mMiddleRight);
        }
        if (null == mBottomLeft) {
            mBottomLeft = new SelectionHandleView(getContext());
            addView(mBottomLeft);
            mSelectionHandleViews.add(mBottomLeft);
        }
        if (null == mBottomMiddle) {
            mBottomMiddle = new SelectionHandleView(getContext());
            addView(mBottomMiddle);
            mSelectionHandleViews.add(mBottomMiddle);
        }
        if (null == mBottomRight) {
            mBottomRight = new SelectionHandleView(getContext());
            addView(mBottomRight);
            mSelectionHandleViews.add(mBottomRight);
        }
    }

    private void selectionHandleAnnotEdit(PointF[] pts) {
        addSelectionWidget();

        if (mAnnotViewImpl.isStamp() ||
                (mAnnotViewImpl.mAnnotStyle.isBasicFreeText() && RotationUtils.shouldMaintainAspectRatio(mAnnotViewImpl.mAnnotRotation))) {
            mMiddleLeft.setVisibility(GONE);
            mMiddleRight.setVisibility(GONE);
            mTopMiddle.setVisibility(GONE);
            mBottomMiddle.setVisibility(GONE);
        }

        PointF pt1 = pts[e_ul];
        PointF pt2 = pts[e_lr];
        PointF midH = pts[e_lm];
        PointF midV = pts[e_ml];

        int left = (int) (Math.min(pt1.x, pt2.x) + .5);
        int right = (int) (Math.max(pt1.x, pt2.x) + .5);
        int top = (int) (Math.min(pt1.y, pt2.y) + .5);
        int bottom = (int) (Math.max(pt1.y, pt2.y) + .5);

        int middle_x = (int) (midH.x + .5);
        int middle_y = (int) (midV.y + .5);

        mTopLeft.layout(left - mSelectionHandleRadius,
                top - mSelectionHandleRadius,
                left + mSelectionHandleRadius,
                top + mSelectionHandleRadius);

        mTopMiddle.layout(middle_x - mSelectionHandleRadius,
                top - mSelectionHandleRadius,
                middle_x + mSelectionHandleRadius,
                top + mSelectionHandleRadius);

        mTopRight.layout(right - mSelectionHandleRadius,
                top - mSelectionHandleRadius,
                right + mSelectionHandleRadius,
                top + mSelectionHandleRadius);

        mMiddleLeft.layout(left - mSelectionHandleRadius,
                middle_y - mSelectionHandleRadius,
                left + mSelectionHandleRadius,
                middle_y + mSelectionHandleRadius);

        mMiddleRight.layout(right - mSelectionHandleRadius,
                middle_y - mSelectionHandleRadius,
                right + mSelectionHandleRadius,
                middle_y + mSelectionHandleRadius);

        mBottomLeft.layout(left - mSelectionHandleRadius,
                bottom - mSelectionHandleRadius,
                left + mSelectionHandleRadius,
                bottom + mSelectionHandleRadius);

        mBottomMiddle.layout(middle_x - mSelectionHandleRadius,
                bottom - mSelectionHandleRadius,
                middle_x + mSelectionHandleRadius,
                bottom + mSelectionHandleRadius);

        mBottomRight.layout(right - mSelectionHandleRadius,
                bottom - mSelectionHandleRadius,
                right + mSelectionHandleRadius,
                bottom + mSelectionHandleRadius);
    }

    public void setSelectionHandleVisible(boolean visible) {
        if (mSelectionHandleViews != null) {
            for (SelectionHandleView v : mSelectionHandleViews) {
                if (v != null) {
                    v.setVisibility(visible ? VISIBLE : GONE);
                }
            }
        }
    }

    public void animateActiveHandle() {
        if (mSelectionHandleViews != null) {
            for (SelectionHandleView v : mSelectionHandleViews) {
                if (v != null) {
                    if (mActiveHandle != null) {
                        if (v == mActiveHandle) {
                            // active one
                            v.animate()
                                    .scaleX(1.5f)
                                    .scaleY(1.5f)
                                    .setInterpolator(new DecelerateInterpolator())
                                    .setDuration(50)
                                    .start();
                        } else {
                            // others
                            v.animate()
                                    .scaleX(0.5f)
                                    .scaleY(0.5f)
                                    .setInterpolator(new AccelerateInterpolator())
                                    .setDuration(50)
                                    .start();
                        }
                    } else {
                        v.animate()
                                .scaleX(1)
                                .scaleY(1)
                                .setInterpolator(new AccelerateInterpolator())
                                .setDuration(50)
                                .start();
                    }
                }
            }
        }
    }

    public void setPositionGuidelines(List<Pair<Point, Point>> guidelines) {
        mAnnotViewImpl.mAnnotPositionSnappingGuidelines.clear();
        mAnnotViewImpl.mAnnotPositionSnappingGuidelines.addAll(guidelines);
        invalidate();
    }

    public void clearPositionGuidelines() {
        if (!mAnnotViewImpl.mAnnotPositionSnappingGuidelines.isEmpty()) {
            mAnnotViewImpl.mAnnotPositionSnappingGuidelines.clear();
            invalidate();
        }
    }

    public enum SnapMode {
        HORIZONTAL,
        VERTICAL,
        ASPECT_RATIO_L,
        ASPECT_RATIO_R
    }
}
