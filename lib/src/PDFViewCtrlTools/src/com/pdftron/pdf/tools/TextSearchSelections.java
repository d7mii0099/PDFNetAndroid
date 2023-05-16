package com.pdftron.pdf.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.List;

class TextSearchSelections {
    private final List<CustomRelativeLayout> mSelectionViews = new ArrayList<>();

    void createAndAddSelectionView(PDFViewCtrl pdfViewCtrl, Rect rect, int pageNum, @ColorInt int color, PorterDuff.Mode blendMode) {
        TextSearchSelectionView selectionView = new TextSearchSelectionView(pdfViewCtrl.getContext());
        selectionView.setZoomWithParent(true);
        selectionView.setSelectionColor(color);
        selectionView.setBlendMode(blendMode);
        selectionView.setRect(pdfViewCtrl, rect, pageNum);
        animateSelectionView(selectionView, pdfViewCtrl);
        mSelectionViews.add(selectionView);
    }

    private void animateSelectionView(final View view, final PDFViewCtrl pdfViewCtrl) {
        view.setScaleX(0.9f);
        view.setScaleY(0.9f);
        pdfViewCtrl.addView(view);
        view.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(200)
                .setInterpolator(new OvershootInterpolator(5.0f));
    }

    void clear(ViewGroup parent) {
        for (CustomRelativeLayout selectionView : mSelectionViews) {
            parent.removeView(selectionView);
        }
        mSelectionViews.clear();
    }

    private static class TextSearchSelectionView extends CustomRelativeLayout {
        private String TAG = "TextSearchSelectionView";
        private Paint mPaint;
        private float mCornerRadius;
        private RectF mRect = new RectF();

        public TextSearchSelectionView(Context context, PDFViewCtrl parent, double x, double y, int page_num) {
            super(context, parent, x, y, page_num);
            init();
        }

        public TextSearchSelectionView(Context context) {
            super(context);
            init();
        }

        public TextSearchSelectionView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        public TextSearchSelectionView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            init();
        }

        public TextSearchSelectionView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
            init();
        }

        private void init() {
            this.setWillNotDraw(false);
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setAntiAlias(true);
            mPaint.setStyle(Paint.Style.FILL);
            mCornerRadius = getCornerRadius(getContext());
            setTag(TAG);
        }

        private static float getCornerRadius(@NonNull Context context) {
            return Utils.convDp2Pix(context, 2.0f);
        }

        private void setSelectionColor(@ColorInt int color) {
            mPaint.setColor(color);
        }

        private void setBlendMode(PorterDuff.Mode blendMode) {
            mPaint.setXfermode(new PorterDuffXfermode(blendMode));
        }

        @SuppressLint("NewApi")
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            mRect.set(0, 0, getWidth(), getHeight());
            canvas.drawRoundRect(mRect, mCornerRadius, mCornerRadius, mPaint);
        }
    }
}
