package com.pdftron.pdf.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Layout;
import android.text.StaticLayout;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.FreeText;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.DrawingUtils;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * An EditText that can auto scroll
 */
public class AutoScrollEditText extends AppCompatEditText {

    private SelectionHandleView mSpacingHandle;
    private int mSelectionHandleRadius;

    private AutoScrollEditTextListener mListener;
    private AutoScrollEditTextSpacingListener mSpacingListener;

    @Nullable
    private AnnotViewImpl mAnnotViewImpl;
    private float mTextSize = 12;
    private int mTextColor;

    private int mMaxCharacterCount;

    private PointF mDownPt;

    private float mPureTextWidth;

    private boolean mIsSpacingText;
    private boolean mShowSpacingBox;

    private Paint mPaint;
    private PointF mTempPt1 = new PointF();
    private PointF mTempPt2 = new PointF();

    private android.graphics.Rect mViewBounds;

    private boolean mAutoResize;
    @Nullable
    private Rect mDefaultRect;

    private boolean mRTL;

    private boolean mDrawBackground = true;

    private boolean mCalculateAlignment = false;
    private boolean mGravityInitialized = false; // we set the gravity when the user starts typing, to ensure the cursor is in the correct location
    private int mHorizontalTextAlignment;
    private int mVerticalTextAlignment;
    @Nullable
    private TextView mTempTextView; // Used to calculate padding for right and centered aligned text
    private int mOldRightPadding = -1; // if this has been set to not -1, then right alignment padding has been used

    /**
     * Listener interface for key up event
     */
    public interface AutoScrollEditTextListener {
        /**
         * This method will be invoked when user released a key
         *
         * @param keyCode Released key code
         * @param event   The key event
         * @return true then intercept the key up event, false otherwise
         */
        boolean onKeyUp(int keyCode, KeyEvent event);

        boolean onKeyPreIme(int keyCode, KeyEvent event);
    }

    public interface AutoScrollEditTextSpacingListener {
        void onUp();
    }

    public AutoScrollEditText(Context context) {
        super(context);
        init();
    }

    public AutoScrollEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AutoScrollEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        this.setFilters(getDefaultInputFilters());
    }

    // This overridden method will prevent images from being inserted from the keyboard.
    // Reference: https://blog.danlew.net/2019/03/25/preventing-unwanted-content-from-google-keyboard/
    @Override
    public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
        if (getInputType() == InputType.TYPE_NULL) {
            return super.onCreateInputConnection(editorInfo);
        }
        InputConnection ic = super.onCreateInputConnection(editorInfo);
        EditorInfoCompat.setContentMimeTypes(editorInfo, new String[]{"image/*"});
        InputConnectionCompat.OnCommitContentListener callback = new InputConnectionCompat.OnCommitContentListener() {
            @Override
            public boolean onCommitContent(InputContentInfoCompat inputContentInfo, int flags, Bundle opts) {
                showInvalidInputToast();
                return true;
            }
        };
        return InputConnectionCompat.createWrapper(ic, editorInfo, callback);
    }

    /**
     * Sets auto scroll edit text listener
     *
     * @param listener The listener
     */
    public void setAutoScrollEditTextListener(AutoScrollEditTextListener listener) {
        mListener = listener;
    }

    public void setAutoScrollEditTextSpacingListener(AutoScrollEditTextSpacingListener listener) {
        mSpacingListener = listener;
    }

    public boolean getDynamicLetterSpacingEnabled() {
        return mShowSpacingBox;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void removeSpacingHandle() {
        mShowSpacingBox = false;
        PDFViewCtrl pdfViewCtrl = getPdfViewCtrl();
        if (pdfViewCtrl != null && mSpacingHandle != null) {
            pdfViewCtrl.removeView(mSpacingHandle);
        }
    }

    /**
     * Must be called after {@link #setAnnotStyle(PDFViewCtrl, AnnotStyle)}
     * with a valid AnnotStyle
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void addLetterSpacingHandle() {
        mShowSpacingBox = true;
        mIsSpacingText = true;

        // init resize widget
        if (null == mSpacingHandle) {
            mSpacingHandle = new SelectionHandleView(getContext());
            mSpacingHandle.setImageResource(R.drawable.ic_fill_and_sign_resizing);
            mSpacingHandle.setCustomSize(R.dimen.resize_widget_size);
        }
        PDFViewCtrl pdfViewCtrl = getPdfViewCtrl();
        if (pdfViewCtrl != null && mSpacingHandle.getParent() == null) {
            // view needs to be added to PDFViewCtrl
            // because when setLetterSpacing will null layout causing subview to stop layout
            pdfViewCtrl.addView(mSpacingHandle);
        }

        if (null == mPaint) {
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setColor(Color.BLACK);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeJoin(Paint.Join.MITER);
            mPaint.setStrokeCap(Paint.Cap.SQUARE);
            mPaint.setStrokeWidth(Utils.convDp2Pix(getContext(), 1));
            mSelectionHandleRadius = getContext().getResources().getDimensionPixelSize(R.dimen.resize_widget_size_w_margin) / 2;
        }
    }

    /**
     * Sets default rect for the FreeText annotation, see {@link com.pdftron.pdf.tools.FreeTextCreate#putDefaultRect(FreeText, Rect)}
     *
     * @param defaultRect the default rect in page space
     */
    public void setDefaultRect(@Nullable Rect defaultRect) {
        if (null == defaultRect) {
            return;
        }
        // conv to screen space
        try {
            mDefaultRect = Utils.convertFromPageRectToScreenRect(mAnnotViewImpl.mPdfViewCtrl, defaultRect, mAnnotViewImpl.mPageNum);
        } catch (Exception ex) {
            mDefaultRect = null;
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return mListener != null && mListener.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        return mListener != null && mListener.onKeyPreIme(keyCode, event);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mAnnotViewImpl != null) {
            mAnnotViewImpl.mPt1.set(getLeft(), getTop());
            mAnnotViewImpl.mPt2.set(getRight(), getBottom());
            updatePadding(getLeft(), getTop(), getRight(), getBottom());
        }
        updateBBox();
        updateResizeHandle();
    }

    @Override
    protected void onScrollChanged(int horiz, int vert, int oldHoriz, int oldVert) {
        super.onScrollChanged(horiz, vert, oldHoriz, oldVert);

        if (mAnnotViewImpl != null) {
            mAnnotViewImpl.mPt1.set(getScrollX(), getScrollY());
            mAnnotViewImpl.mPt2.set(getScrollX() + getWidth(), getScrollY() + getHeight());
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);

        if (mSpacingHandle != null) {
            float x = event.getX();
            float y = event.getY();

            int offsetX = 0;
            int offsetY = 0;
            if (mViewBounds != null) {
                offsetX = mViewBounds.left;
                offsetY = mViewBounds.top;
            }

            int left = mSpacingHandle.getLeft() - offsetX;
            int top = mSpacingHandle.getTop() - offsetY;
            int right = mSpacingHandle.getRight() - offsetX;
            int bottom = mSpacingHandle.getBottom() - offsetY;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (Utils.isLollipop()) {
                        if (x >= left && x <= right &&
                                y >= top && y <= bottom) {
                            Paint fakeTextPaint = new Paint(getPaint());
                            fakeTextPaint.setLetterSpacing(0);
                            mPureTextWidth = getLetterOnlyWidth(this, fakeTextPaint);

                            mDownPt = new PointF(x, y);
                        }
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mDownPt != null && Utils.isLollipop()) {
                        float fullSize = x - mSelectionHandleRadius * 2 - mAnnotViewImpl.mPt1.x;
                        float spacingSize = fullSize - mPureTextWidth;

                        float font_sz = mTextSize * (float) mAnnotViewImpl.mZoom;
                        float letterSpacing = (spacingSize / font_sz / mMaxCharacterCount);

                        // letter spacing is calculated as follows:
                        // 1. obtain width when there is 0 spacing
                        // 2. obtain spacing width (total width minus 0 spacing text width)
                        // 3. obtain per character spacing then convert to em
                        setLetterSpacing(Math.max(0, letterSpacing));
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (mDownPt != null) {
                        requestLayout();
                        invalidate();
                        if (mSpacingListener != null) {
                            mSpacingListener.onUp();
                        }
                    }
                    mDownPt = null;
                    break;
            }
        }

        if (mDownPt != null) {
            return true;
        } else {
            if (isEnabled()) {
                return result;
            }
            return false;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        mViewBounds = getViewBounds();
        updateBBox();
        updateResizeHandle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mAnnotViewImpl != null && !isWidget() && mDrawBackground) {
            DrawingUtils.drawRectangle(canvas,
                    mAnnotViewImpl.mPt1, mAnnotViewImpl.mPt2,
                    mAnnotViewImpl.mThicknessDraw,
                    mAnnotViewImpl.mFillColor, mAnnotViewImpl.mStrokeColor,
                    mAnnotViewImpl.mFillPaint, mAnnotViewImpl.mPaint, null);
        }

        if (mShowSpacingBox) {
            drawResizeBox(canvas);
        }

        super.onDraw(canvas);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);

        if (!mGravityInitialized) {
            setGravity(mHorizontalTextAlignment | mVerticalTextAlignment);
            mGravityInitialized = true;
        }

        if (mCalculateAlignment && mAnnotViewImpl.mAnnotStyle.hasTextAlignment()) {
            if (mTempTextView != null && mAnnotViewImpl != null) {
                mTempTextView.setText(text);
                adjustInlineEditTextPadding(mTempTextView, mAnnotViewImpl);
            }
        }

        updateBBox();
        updateResizeHandle();
    }

    @Override
    public void setBackgroundColor(int color) {
        if (mAnnotViewImpl != null && !isWidget()) {
            // we will draw manually in onDraw instead
            return;
        }
        super.setBackgroundColor(color);
    }

    @Override
    public boolean isSuggestionsEnabled() {
        return false;
    }

    public void setDrawBackground(boolean drawBackground) {
        mDrawBackground = drawBackground;
    }

    public void setSize(float width, float height) {
        if (mAnnotViewImpl != null) {
            mAnnotViewImpl.mPt2.set(width, height);
        }
    }

    private static float getLetterOnlyWidth(TextView textView, Paint textPaint) {
        List<CharSequence> contents = getLines(textView);
        float maxWidth = 0;
        for (CharSequence c : contents) {
            String text = c.toString().trim();
            float width = textPaint.measureText(text);
            maxWidth = Math.max(width, maxWidth);
        }
        return maxWidth;
    }

    public void updateBBox() {
        if (mCalculateAlignment) {
            return;
        }
        if (getText().toString().isEmpty()) {
            return;
        }
        if (mAnnotViewImpl != null) {
            if (mIsSpacingText || mAutoResize) {
                PointF topLeft = mAnnotViewImpl.mPt1;
                PointF bottomRight = mAnnotViewImpl.mPt2;
                // when creating free text, the bottom right is not the actual bbox
                List<CharSequence> contents = getLines(this);
                if (!contents.isEmpty()) {
                    float maxWidth = 0;
                    float maxHeight = 0;
                    mMaxCharacterCount = 0;
                    for (CharSequence c : contents) {
                        String text = c.toString().trim();
                        float width = getPaint().measureText(text);
                        maxWidth = Math.max(width, maxWidth);
                        Paint.FontMetrics metrics = getPaint().getFontMetrics();
                        maxHeight += metrics.bottom - metrics.top;
                        mMaxCharacterCount = Math.max(text.length(), mMaxCharacterCount);
                    }
                    boolean isRTL = isRTL(Math.round(maxWidth));

                    if (isRTL) {
                        topLeft.set(mAnnotViewImpl.mPt2.x - getPaddingRight() - maxWidth - getPaddingLeft(),
                                mAnnotViewImpl.mPt1.y);
                        bottomRight.set(mAnnotViewImpl.mPt2.x,
                                mAnnotViewImpl.mPt1.y + getPaddingTop() + maxHeight + getPaddingBottom());
                        if (mAutoResize && mDefaultRect != null) {
                            try {
                                topLeft.x = Math.min(topLeft.x, mAnnotViewImpl.mPt2.x - (int) (mDefaultRect.getWidth() + .5));
                                bottomRight.y = Math.max(bottomRight.y, mAnnotViewImpl.mPt1.y + (int) (mDefaultRect.getHeight() + .5));
                            } catch (Exception ignored) {
                            }
                        }
                        mAnnotViewImpl.mPt1.set(topLeft);
                        mAnnotViewImpl.mPt2.set(bottomRight);
                    } else {
                        bottomRight.set(mAnnotViewImpl.mPt1.x + getPaddingLeft() + maxWidth + getPaddingRight(),
                                mAnnotViewImpl.mPt1.y + getPaddingTop() + maxHeight + getPaddingBottom());
                        if (mAutoResize && mDefaultRect != null) {
                            try {
                                bottomRight.x = Math.max(bottomRight.x, mAnnotViewImpl.mPt1.x + (int) (mDefaultRect.getWidth() + .5));
                                bottomRight.y = Math.max(bottomRight.y, mAnnotViewImpl.mPt1.y + (int) (mDefaultRect.getHeight() + .5));
                            } catch (Exception ignored) {
                            }
                        }
                        mAnnotViewImpl.mPt2.set(bottomRight);
                    }
                }
            } else if (mAnnotViewImpl.mAnnotStyle.isBasicFreeText()) {
                mAnnotViewImpl.mPt1.set(getScrollX(), getScrollY());
                mAnnotViewImpl.mPt2.set(getScrollX() + getWidth(), getScrollY() + getHeight());
            }
        }
    }

    private boolean isRTL(int maxWidth) {
        StaticLayout layout = new StaticLayout(getText(), getPaint(), maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        int direction = layout.getParagraphDirection(0);
        mRTL = direction == Layout.DIR_RIGHT_TO_LEFT;
        return mRTL;
    }

    public boolean getIsRTL() {
        return mRTL;
    }

    private void updateResizeHandle() {
        if (mSpacingHandle != null && mAnnotViewImpl != null) {
            if (mMaxCharacterCount > 1) {
                // no point to show handle if got no text
                mSpacingHandle.setVisibility(VISIBLE);
            } else {
                mSpacingHandle.setVisibility(GONE);
            }
            int x = Math.round(mAnnotViewImpl.mPt2.x) + mSelectionHandleRadius; // add some padding
            int y = Math.round((mAnnotViewImpl.mPt2.y - mAnnotViewImpl.mPt1.y) / 2.f);

            int offsetX = 0;
            int offsetY = 0;
            if (mViewBounds != null) {
                offsetX += mViewBounds.left;
                offsetY += mViewBounds.top;
            }
            x += offsetX;
            y += offsetY;

            mSpacingHandle.layout(x,
                    y - mSelectionHandleRadius,
                    x + mSelectionHandleRadius * 2,
                    y + mSelectionHandleRadius);
        }
    }

    private void drawResizeBox(Canvas canvas) {
        // draw resize box
        if (mAnnotViewImpl != null) {
            float width = mAnnotViewImpl.mPt2.x - mAnnotViewImpl.mPt1.x - mAnnotViewImpl.mThicknessDraw * 2;
            float eachWidth = width / mMaxCharacterCount;
            mPaint.setColor(mTextColor);

            float x1 = mAnnotViewImpl.mPt1.x + mAnnotViewImpl.mThicknessDraw;
            float y1 = mAnnotViewImpl.mPt1.y + mAnnotViewImpl.mThicknessDraw;
            float y2 = mAnnotViewImpl.mPt2.y - mAnnotViewImpl.mThicknessDraw;
            for (int i = 1; i <= mMaxCharacterCount; i++) {
                if (i == 1) {
                    mTempPt1.set(x1, y1);
                    mTempPt2.set(mTempPt1.x + eachWidth, y2);
                } else {
                    mTempPt1.set(mTempPt2.x, y1);
                    mTempPt2.set(mTempPt1.x + eachWidth, y2);
                }
                DrawingUtils.drawRectangle(canvas,
                        mTempPt1, mTempPt2,
                        0,
                        Color.TRANSPARENT, mTextColor,
                        mAnnotViewImpl.mFillPaint, mPaint, null);
            }
        }
    }

    @Nullable
    private android.graphics.Rect getViewBounds() {
        PDFViewCtrl pdfViewCtrl = getPdfViewCtrl();
        if (pdfViewCtrl != null) {
            android.graphics.Rect bounds = new android.graphics.Rect();
            //returns the visible bounds
            this.getDrawingRect(bounds);
            // calculates the relative coordinates to the parent
            pdfViewCtrl.offsetDescendantRectToMyCoords(this, bounds);
            return bounds;
        }
        return null;
    }

    @Nullable
    private PDFViewCtrl getPdfViewCtrl() {
        return mAnnotViewImpl != null ? mAnnotViewImpl.mPdfViewCtrl : null;
    }

    @Nullable
    public Rect getBoundingRect() {
        updateBBox();
        if (mAnnotViewImpl != null) {
            if (mIsSpacingText || mAutoResize) {
                // here the bounding box is a tight box around the text
                try {
                    return new Rect(mAnnotViewImpl.mPt1.x, mAnnotViewImpl.mPt1.y, mAnnotViewImpl.mPt2.x, mAnnotViewImpl.mPt2.y);
                } catch (Exception ignored) {
                }
            } else {
                // here the bounding box is the widget size
                try {
                    return new Rect(getLeft(), getTop(), getRight(), getBottom());
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    static List<CharSequence> getLines(@NonNull TextView view) {
        final List<CharSequence> lines = new ArrayList<>();
        final Layout layout = view.getLayout();

        if (layout != null) {
            // Get the number of lines currently in the layout
            final int lineCount = layout.getLineCount();

            // Get the text from the layout.
            final CharSequence text = layout.getText();

            // Initialize a start index of 0, and iterate for all lines
            for (int i = 0, startIndex = 0; i < lineCount; i++) {
                // Get the end index of the current line (use getLineVisibleEnd()
                // instead if you don't want to include whitespace)
                final int endIndex = layout.getLineEnd(i);

                // Add the subSequence between the last start index
                // and the end index for the current line.
                lines.add(text.subSequence(startIndex, endIndex));

                // Update the start index, since the indices are relative
                // to the full text.
                startIndex = endIndex;
            }
        }
        return lines;
    }

    private boolean isWidget() {
        if (mAnnotViewImpl != null) {
            return mAnnotViewImpl.mAnnotStyle.getAnnotType() == Annot.e_Widget ||
                    mAnnotViewImpl.mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_TEXT_FIELD;
        }
        return false;
    }

    private void updatePadding(int left, int top, int right, int bottom) {
        if (mAnnotViewImpl == null || isWidget()) {
            return;
        }
        int paddingH = (int) (mAnnotViewImpl.mThicknessDraw * 2 + 0.5);
        int paddingV = (int) (mAnnotViewImpl.mThicknessDraw * 2 + 0.5);
        if (paddingH > ((right - left) / 2)) {
            paddingH = (int) (mAnnotViewImpl.mThicknessDraw + 0.5);
        }
        if (paddingV > ((bottom - top) / 2)) {
            paddingV = (int) (mAnnotViewImpl.mThicknessDraw + 0.5);
        }
        if (mTempTextView != null) {
            mTempTextView.setPadding(paddingH, paddingV, paddingH, paddingV);
        }
        setPadding(paddingH, paddingV, paddingH, paddingV);
    }

    private void updatePadding() {
        updatePadding(getLeft(), getTop(), getRight(), getBottom());
    }

    public void setAnnotStyle(AnnotViewImpl annotViewImpl) {
        mAnnotViewImpl = annotViewImpl;

        mTextSize = mAnnotViewImpl.mAnnotStyle.getTextSize();
        mTextColor = mAnnotViewImpl.mAnnotStyle.getTextColor();
        updateTextColor(mTextColor);
        updateTextSize(mTextSize);

        mAnnotViewImpl.loadFont(new AnnotViewImpl.AnnotViewImplListener() {
            @Override
            public void fontLoaded() {
                if (mAnnotViewImpl != null && mAnnotViewImpl.mAnnotStyle != null) {
                    updateFont(mAnnotViewImpl.mAnnotStyle.getFont());
                }
            }
        });
        updateFont(mAnnotViewImpl.mAnnotStyle.getFont());

        PDFViewCtrl pdfViewCtrl = getPdfViewCtrl();
        if (pdfViewCtrl != null && pdfViewCtrl.getToolManager() instanceof ToolManager) {
            ToolManager tm = (ToolManager) pdfViewCtrl.getToolManager();
            mAutoResize = tm.isAutoResizeFreeText();
        }
    }

    public void setAnnotStyle(PDFViewCtrl pdfViewCtrl, AnnotStyle annotStyle) {
        mAnnotViewImpl = new AnnotViewImpl(pdfViewCtrl, annotStyle);
        mAnnotViewImpl.setZoom(pdfViewCtrl.getZoom());

        setAnnotStyle(mAnnotViewImpl);
    }

    public void updateTextColor(int textColor) {
        mTextColor = textColor;
        int color;
        if (mAnnotViewImpl.isAnnotFreeText()) {
            color = mTextColor;
        } else {
            color = Utils.getPostProcessedColor(mAnnotViewImpl.mPdfViewCtrl, mTextColor);
        }
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        int opacity = (int) (mAnnotViewImpl.mOpacity * 255);
        color = Color.argb(opacity, r, g, b);
        setTextColor(color);
    }

    public void updateTextSize(float textSize) {
        mTextSize = textSize;
        float font_sz = mTextSize * (float) mAnnotViewImpl.mZoom;
        setTextSize(TypedValue.COMPLEX_UNIT_PX, font_sz);
        if (mTempTextView != null) {
            mTempTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, font_sz);
        }
    }

    public void updateColor(int color) {
        mAnnotViewImpl.updateColor(color);
        updatePadding();
        invalidate();
    }

    public void updateFillColor(int color) {
        mAnnotViewImpl.updateFillColor(color);
        invalidate();
    }

    public void updateThickness(float thickness) {
        mAnnotViewImpl.updateThickness(thickness);
        updatePadding();
        invalidate();
    }

    public void updateOpacity(float opacity) {
        mAnnotViewImpl.updateOpacity(opacity);
        updateTextColor(mTextColor);
    }

    public void updateFont(FontResource font) {
        if (null == font || Utils.isNullOrEmpty(font.getFilePath())) {
            return;
        }

        try {
            Typeface typeFace = Typeface.createFromFile(font.getFilePath());
            setTypeface(typeFace);
            if (mTempTextView != null) {
                mTempTextView.setTypeface(typeFace);
            }
        } catch (Exception ignored) { // when font not found

        }
    }

    public void setZoom(double zoom) {
        mAnnotViewImpl.setZoom(zoom);

        updatePadding();

        float font_sz = mTextSize * (float) mAnnotViewImpl.mZoom;
        setTextSize(TypedValue.COMPLEX_UNIT_PX, font_sz);
    }

    public void setUseAutoResize(boolean useAutoResize) {
        mAutoResize = useAutoResize;
    }

    /**
     * Gets the default input filters used internally
     */
    public InputFilter[] getDefaultInputFilters() {
        return new InputFilter[]{};
    }

    private void showInvalidInputToast() {
        CommonToast.showText(getContext(), R.string.edit_text_invalid_content, Toast.LENGTH_SHORT);
    }

    void initTempTextView(@NonNull TextView textView) {
        mTempTextView = textView;
    }

    private void adjustInlineEditTextPadding(@NonNull TextView tempTextView, @NonNull AnnotViewImpl annotViewImpl) {
        tempTextView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        int right = getWidth() - tempTextView.getMeasuredWidth();
        if (getWidth() == tempTextView.getWidth()) {
            setRightAlignmentPadding(0);
        } else {
            setRightAlignmentPadding(right);
        }
        annotViewImpl.mPt2.x = annotViewImpl.mPt1.x + tempTextView.getWidth();
        annotViewImpl.mPt2.y = annotViewImpl.mPt1.y + tempTextView.getHeight();
    }

    private void setRightAlignmentPadding(int rightAlignPadding) {
        int paddingRight = getPaddingRight();
        if (mOldRightPadding == -1) {
            mOldRightPadding = paddingRight;
        }
        if (paddingRight != rightAlignPadding) {
            setPadding(getPaddingLeft(), getPaddingTop(), mOldRightPadding + rightAlignPadding, getPaddingBottom());
        }
    }

    /**
     * Sets whether the alignment should be calculated manually.
     * If set to true, then a dummy text view will be used to calculate the bounding box.
     */
    public void setCalculateAlignment(boolean calculateAlignment) {
        mCalculateAlignment = calculateAlignment;
    }

    /**
     * Sets the horizontal text alignment without changing the gravity. The gravity will be set the next
     * time the user starts typing.
     */
    public void setHorizontalTextAlignment(int gravity) {
        mHorizontalTextAlignment = gravity;
        mGravityInitialized = false;
    }

    /**
     * Sets the vertical text alignment without changing the gravity. The gravity will be set the next
     * time the user starts typing.
     */
    public void setVerticalTextAlignment(int gravity) {
        mVerticalTextAlignment = gravity;
        mGravityInitialized = false;
    }
}
