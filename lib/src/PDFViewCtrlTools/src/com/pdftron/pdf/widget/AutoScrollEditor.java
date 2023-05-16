package com.pdftron.pdf.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.fragment.app.FragmentActivity;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.CustomRelativeLayout;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.richtext.PTRichEditor;

import static com.pdftron.richeditor.Constants.ZERO_WIDTH_SPACE_STR;

/**
 * A {@link CustomRelativeLayout} that contains an {@link AutoScrollEditText}.
 */
public class AutoScrollEditor extends CustomRelativeLayout {

    private AutoScrollEditText mEditText;
    private PTRichEditor mRichEditor;

    private int mInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
    private int mParentMarginBottom = 0;

    private int mPadding;

    private RotationImpl mRotationImpl;

    /**
     * Gets edit text
     *
     * @return edit text
     */
    public AutoScrollEditText getEditText() {
        if (mRichEditor.getVisibility() == VISIBLE) {
            return mRichEditor;
        }
        return mEditText;
    }

    public PTRichEditor getRichEditor() {
        return mRichEditor;
    }

    public View getActiveEditor() {
        if (mRichEditor.getVisibility() == VISIBLE) {
            return mRichEditor;
        }
        return mEditText;
    }

    public String getActiveText() {
        if (mRichEditor.getVisibility() == VISIBLE) {
            return mRichEditor.getText().toString().trim().replaceAll(ZERO_WIDTH_SPACE_STR, "");
        }
        return mEditText.getText().toString();
    }

    public AutoScrollEditor(Context context) {
        this(context, null);
    }

    public AutoScrollEditor(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoScrollEditor(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.widget_auto_scroll_editor, this);
        mEditText = view.findViewById(R.id.edit_text);
        // Used to calculate padding for right and centered aligned text
        TextView tempTextView = view.findViewById(R.id.temp_text_view);
        mEditText.initTempTextView(tempTextView);
        mRichEditor = view.findViewById(R.id.rich_editor);
        mRichEditor.setPadding(0, 0, 0, 0);
        mRichEditor.setBackgroundColor(Color.TRANSPARENT);
        mZoomWithParent = true;

        mPadding = (int) Utils.convDp2Pix(getContext(), 8);
    }

    public void setCalculateAlignment(boolean calculateAlignment) {
        mEditText.setCalculateAlignment(calculateAlignment);
    }

    public boolean isRichContentEnabled() {
        return mRichEditor.getVisibility() == VISIBLE;
    }

    public void setRichContentEnabled(boolean enabled) {
        mEditText.setVisibility(enabled ? GONE : VISIBLE);
        mRichEditor.setVisibility(enabled ? VISIBLE : GONE);
        if (enabled) {
            mRichEditor.setDrawBackground(false); // we will apply style when apply to the annot
        }
    }

    public void setAnnotStyle(PDFViewCtrl pdfViewCtrl, AnnotStyle annotStyle) {
        mEditText.setAnnotStyle(pdfViewCtrl, annotStyle);
        mRichEditor.setAnnotStyle(pdfViewCtrl, annotStyle);
    }

    public void setRotateImpl(RotationImpl rotateImpl) {
        mRotationImpl = rotateImpl;
    }

    public void rotateToDegree() {
        if (mRotationImpl != null) {
            float degree = mRotationImpl.mRotating ? mRotationImpl.mRotDegreeSave + mRotationImpl.mRotDegree : mRotationImpl.mRotDegreeSave;
            setRotation(degree);
        }
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {

        if (isAdjustResize() && Utils.isLollipop()) {
            int insetBottom = insets.getSystemWindowInsetBottom();

            final int keyboardHeight = insets.getSystemWindowInsetBottom() - insets.getStableInsetBottom();

            if (mParentView.getLayoutParams() instanceof MarginLayoutParams) {
                MarginLayoutParams lp = (MarginLayoutParams) mParentView.getLayoutParams();
                if (keyboardHeight != 0) {
                    // keyboard shown
                    lp.bottomMargin = insetBottom;
                } else {
                    // keyboard hidden
                    lp.bottomMargin = mParentMarginBottom;
                }
                mParentView.setLayoutParams(lp);
            }
        }

        return super.onApplyWindowInsets(insets);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        Context context = getContext();
        if (context instanceof FragmentActivity) {
            mInputMode = ((FragmentActivity) context).getWindow().getAttributes().softInputMode;
        }

        if (isAdjustResize()) {
            mParentView.setPageViewMode(PDFViewCtrl.PageViewMode.ZOOM);

            if (mParentView.getLayoutParams() instanceof MarginLayoutParams) {
                MarginLayoutParams lp = (MarginLayoutParams) mParentView.getLayoutParams();
                mParentMarginBottom = lp.bottomMargin;
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (isAdjustResize() && mParentView.getLayoutParams() instanceof MarginLayoutParams) {
            MarginLayoutParams lp = (MarginLayoutParams) mParentView.getLayoutParams();
            if (lp.bottomMargin != mParentMarginBottom) {
                // reset margin
                lp.bottomMargin = mParentMarginBottom;
                mParentView.setLayoutParams(lp);
            }
        }
    }

    private Rect mViRect = new Rect();
    private Rect mTempRect = new Rect();

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (isAdjustResize() && mParentView != null) {
            mParentView.getDrawingRect(mViRect);
            mTempRect.set(l, t - mPadding, r, b + mPadding);
            if (!mViRect.intersect(mTempRect)) {
                // hidden by keyboard
                mParentView.scrollBy(0, b + mPadding - mParentView.getBottom());
            }
        }
    }

    protected boolean isAdjustResize() {
        return mInputMode == WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
    }
}
