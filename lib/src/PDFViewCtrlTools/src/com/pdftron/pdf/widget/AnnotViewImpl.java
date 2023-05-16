package com.pdftron.pdf.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.util.Pair;
import androidx.annotation.NonNull;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.CurvePainter;
import com.pdftron.pdf.PDFRasterizer;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.asynctask.LoadFontAsyncTask;
import com.pdftron.pdf.config.ToolConfig;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.model.LineEndingStyle;
import com.pdftron.pdf.model.LineStyle;
import com.pdftron.pdf.model.RulerItem;
import com.pdftron.pdf.model.ShapeBorderStyle;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.DrawingUtils;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class AnnotViewImpl {

    public interface AnnotViewImplListener {
        void fontLoaded();
    }

    public AnnotStyle mAnnotStyle;
    public CurvePainter mCurvePainter;

    public PDFViewCtrl mPdfViewCtrl;
    public int mPageNum;
    public int mAnnotUIRotation;
    public int mAnnotRotation;

    public PointF mPt1 = new PointF(0, 0);
    public PointF mPt2 = new PointF(0, 0);

    public Paint mPaint;
    public Paint mFillPaint;
    public Paint mCtrlPtsPaint;
    public Paint mBmpPaint;
    public Paint mBmpMultBlendPaint;
    public Paint mGuidelinePaint;
    public float mGuidelinExtend;
    public float mRotateCenterRadius;
    public float mThickness;
    public float mThicknessReserve;
    public float mThicknessDraw;
    public int mStrokeColor;
    public int mFillColor;
    public float mOpacity;
    public double mZoom = 1.0;
    public float mCtrlRadius;
    public boolean mHasSelectionPermission = true;

    public PointF[] mCtrlPts;
    public ArrayList<PointF> mVertices = new ArrayList<>();

    @NonNull
    public RectF mAnnotRectF = new RectF();
    public Rect mAnnotRect = new Rect();
    Path mGuidelinePath = new Path();

    public boolean mCanDrawCtrlPts = true;

    AnnotView.SnapMode mSnapMode;
    RectF mBBox = new RectF();

    List<Pair<Point, Point>> mAnnotPositionSnappingGuidelines = new ArrayList<>();

    public AnnotViewImpl(Context context) {
        init(context);
    }

    public AnnotViewImpl(PDFViewCtrl pdfViewCtrl, AnnotStyle annotStyle) {
        init(pdfViewCtrl.getContext());

        setAnnotStyle(pdfViewCtrl, annotStyle);
    }

    public void init(Context context) {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.MITER);
        mPaint.setStrokeCap(Paint.Cap.BUTT); // Butt is needed for dashed lines

        mFillPaint = new Paint(mPaint);
        mFillPaint.setStyle(Paint.Style.FILL);
        mFillPaint.setColor(Color.TRANSPARENT);

        mCtrlPtsPaint = new Paint(mPaint);

        mBmpPaint = new Paint();
        mBmpPaint.setStyle(Paint.Style.FILL);
        mBmpPaint.setAntiAlias(true);
        mBmpPaint.setFilterBitmap(false);

        mBmpMultBlendPaint = new Paint(mBmpPaint);
        mBmpMultBlendPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));

        mGuidelinePaint = new Paint(mPaint);
        mGuidelinePaint.setStyle(Paint.Style.STROKE);
        DashPathEffect dashPathEffect = new DashPathEffect(new float[]{Utils.convDp2Pix(context, 4.5f), Utils.convDp2Pix(context, 2.5f)}, 0);
        mGuidelinePaint.setPathEffect(dashPathEffect);
        mGuidelinePaint.setStrokeWidth(Utils.convDp2Pix(context, 1));
        mGuidelinePaint.setColor(context.getResources().getColor(R.color.tools_annot_edit_rotate_guideline));
        mGuidelinExtend = Utils.convDp2Pix(context, DrawingUtils.sSelectionBoxMargin) * 3;
        mRotateCenterRadius = context.getResources().getDimensionPixelSize(R.dimen.rotate_guideline_center_radius);

        mThicknessDraw = 1.0f;
        mOpacity = 1.0f;

        mCtrlRadius = context.getResources().getDimensionPixelSize(R.dimen.selection_widget_size_w_margin) / 2.0f;
    }

    public void setAnnotStyle(PDFViewCtrl pdfViewCtrl, AnnotStyle annotStyle) {
        mPdfViewCtrl = pdfViewCtrl;
        mAnnotStyle = annotStyle;

        mStrokeColor = annotStyle.getColor();
        mFillColor = annotStyle.getFillColor();
        mThickness = mThicknessReserve = annotStyle.getThickness();
        mOpacity = annotStyle.getOpacity();

        if (isAnnotFreeText()) {
            mPaint.setColor(mStrokeColor);
            mFillPaint.setColor(mFillColor);
        } else {
            mPaint.setColor(Utils.getPostProcessedColor(mPdfViewCtrl, mStrokeColor));
            mFillPaint.setColor(Utils.getPostProcessedColor(mPdfViewCtrl, mFillColor));
        }

        if (mPaint.getColor() != Color.TRANSPARENT) {
            mPaint.setAlpha((int) (255 * mOpacity));
        }
        if (mFillPaint.getColor() != Color.TRANSPARENT) {
            mFillPaint.setAlpha((int) (255 * mOpacity));
        }

        updateColor(mStrokeColor);
    }

    public void updateColor(int color) {
        mStrokeColor = color;
        if (isAnnotFreeText()) {
            mPaint.setColor(mStrokeColor);
        } else {
            mPaint.setColor(Utils.getPostProcessedColor(mPdfViewCtrl, mStrokeColor));
        }
        updateOpacity(mOpacity);

        updateThickness(mThicknessReserve);
    }

    public void updateFillColor(int color) {
        mFillColor = color;
        if (isAnnotFreeText()) {
            mFillPaint.setColor(mFillColor);
        } else {
            mFillPaint.setColor(Utils.getPostProcessedColor(mPdfViewCtrl, mFillColor));
        }
        updateOpacity(mOpacity);
    }

    public void updateThickness(float thickness) {
        mThickness = mThicknessReserve = thickness;
        if (mStrokeColor == Color.TRANSPARENT) {
            mThickness = 1.0f;
        } else {
            mThickness = thickness;
        }
        mThicknessDraw = (float) mZoom * mThickness;
        mPaint.setStrokeWidth(mThicknessDraw);
    }

    public void updateOpacity(float opacity) {
        mOpacity = opacity;
        if (mPaint.getColor() != Color.TRANSPARENT) {
            mPaint.setAlpha((int) (255 * mOpacity));
        }
        if (mFillPaint.getColor() != Color.TRANSPARENT) {
            mFillPaint.setAlpha((int) (255 * mOpacity));
        }
    }

    public void updateRulerItem(RulerItem rulerItem) {
        mAnnotStyle.setRulerItem(rulerItem);
    }

    public void updateBorderStyle(ShapeBorderStyle borderStyle) {
        mAnnotStyle.setBorderStyle(borderStyle);
    }

    public void updateLineStyle(LineStyle lineStyle) {
        mAnnotStyle.setLineStyle(lineStyle);
    }

    public void updateLineStartStyle(LineEndingStyle lineStartStyle) {
        mAnnotStyle.setLineStartStyle(lineStartStyle);
    }

    public void updateLineEndStyle(LineEndingStyle lineEndStyle) {
        mAnnotStyle.setLineEndStyle(lineEndStyle);
    }

    public void setZoom(double zoom) {
        mZoom = zoom;
        mThicknessDraw = (float) mZoom * mThickness;
        mPaint.setStrokeWidth(mThicknessDraw);
    }

    public void setVertices(PointF... points) {
        mVertices.clear();
        if (points != null) {
            mVertices.addAll(Arrays.asList(points));
        }
    }

    public void removeCtrlPts() {
        mCanDrawCtrlPts = false;
    }

    public void loadFont(final AnnotViewImplListener listener) {
        ArrayList<FontResource> fontList = ToolConfig.getInstance().getFontList();
        if (null == fontList || fontList.size() == 0) {
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();

            Set<String> whiteListFonts = toolManager.getFreeTextFonts();
            boolean isCustomFont = false;
            if (toolManager.getFreeTextFontsFromAssets() != null && !toolManager.getFreeTextFontsFromAssets().isEmpty()) {
                whiteListFonts = toolManager.getFreeTextFontsFromAssets();
                isCustomFont = true;
            } else if (toolManager.getFreeTextFontsFromStorage() != null && !toolManager.getFreeTextFontsFromStorage().isEmpty()) {
                whiteListFonts = toolManager.getFreeTextFontsFromStorage();
                isCustomFont = true;
            }

            LoadFontAsyncTask fontAsyncTask = new LoadFontAsyncTask(mPdfViewCtrl.getContext(), whiteListFonts);
            fontAsyncTask.setIsCustomFont(isCustomFont);
            fontAsyncTask.setCallback(new LoadFontAsyncTask.Callback() {
                @Override
                public void onFinish(ArrayList<FontResource> fonts) {
                    FontResource font = getMatchingFont(fonts);
                    mAnnotStyle.setFont(font);
                    if (listener != null) {
                        listener.fontLoaded();
                    }
                }
            });
            fontAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            FontResource font = getMatchingFont(fontList);
            mAnnotStyle.setFont(font);
        }
    }

    private FontResource getMatchingFont(ArrayList<FontResource> fonts) {
        for (FontResource font : fonts) {
            if (font.equals(mAnnotStyle.getFont())) {
                mAnnotStyle.getFont().setFilePath(font.getFilePath());
                break;
            }
        }
        return mAnnotStyle.getFont();
    }

    public boolean isNightMode() {
        try {
            return mPdfViewCtrl.getColorPostProcessMode() == PDFRasterizer.e_postprocess_night_mode ||
                    mPdfViewCtrl.getColorPostProcessMode() == PDFRasterizer.e_postprocess_invert;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isAnnotResizable() {
        return !(mAnnotStyle.getAnnotType() == Annot.e_Text) &&
                !(mAnnotStyle.getAnnotType() == Annot.e_Sound) &&
                !(mAnnotStyle.getAnnotType() == Annot.e_FileAttachment) &&
                !(mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT) &&
                !(mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_SPACING) &&
                !(mAnnotStyle.isRCFreeText());
    }

    public boolean isFreeHighlighter() {
        return mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_HIGHLIGHTER;
    }

    public boolean isStamp() {
        return mAnnotStyle.getAnnotType() == Annot.e_Stamp ||
                mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE; // signature is a type of stamp
    }

    public boolean isCallout() {
        return mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT;
    }

    public boolean isAnnotEditLine() {
        return mAnnotStyle.getAnnotType() == Annot.e_Line ||
                mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW ||
                mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_RULER;
    }

    public boolean isAnnotEditAdvancedShape() {
        return mAnnotStyle.getAnnotType() == Annot.e_Polyline ||
                mAnnotStyle.getAnnotType() == Annot.e_Polygon ||
                mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD ||
                mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT ||
                mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_PERIMETER_MEASURE ||
                mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_AREA_MEASURE;
    }

    boolean isAnnotFreeText() {
        return mAnnotStyle.getAnnotType() == Annot.e_FreeText;
    }

    boolean isAnnotWidget() {
        return mAnnotStyle.getAnnotType() == Annot.e_Widget;
    }

    public void addPositionGuidelines(Pair<Point, Point> guidelines) {
        mAnnotPositionSnappingGuidelines.add(guidelines);
    }
}
