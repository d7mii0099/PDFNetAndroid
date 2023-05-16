package com.pdftron.pdf.controls;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;

import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.LineEndingStyle;
import com.pdftron.pdf.model.LineStyle;
import com.pdftron.pdf.model.ShapeBorderStyle;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnnotationPropertyPreviewView;

import java.util.ArrayList;
import java.util.List;

import static com.pdftron.pdf.controls.AnnotStyleDialogFragment.BORDER_STYLE;
import static com.pdftron.pdf.controls.AnnotStyleDialogFragment.LINE_END_STYLE;
import static com.pdftron.pdf.controls.AnnotStyleDialogFragment.LINE_START_STYLE;
import static com.pdftron.pdf.controls.AnnotStyleDialogFragment.LINE_STYLE;

public class StylePickerView extends LinearLayout {
    // UI
    private ImageButton mBackButton;
    private TextView mToolbarTitle;
    private FrameLayout mContainer;
    private PresetStyleGridView mPresetStyleGridView;
    private AnnotStyle.AnnotStyleHolder mAnnotStyleHolder;

    private OnBackButtonPressedListener mBackPressedListener;

    private @AnnotStyleDialogFragment.SelectStyleMode
    int mStyleMode;

    private AnnotStyleDialogFragment.Theme mTheme;

    /**
     * Class constructor
     */
    public StylePickerView(Context context) {
        this(context, null);
    }

    /**
     * Class constructor
     */
    public StylePickerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Class constructor
     */
    public StylePickerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mTheme = AnnotStyleDialogFragment.Theme.fromContext(getContext());
        LayoutInflater.from(getContext()).inflate(R.layout.style_picker_layout, this);
        mBackButton = findViewById(R.id.back_btn);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackButtonPressed();
            }
        });
        mToolbarTitle = findViewById(R.id.toolbar_title);
        mContainer = findViewById(R.id.style_picker_container);
        mPresetStyleGridView = new PresetStyleGridView(getContext());

        // layout params
        ViewGroup.MarginLayoutParams mlp = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mPresetStyleGridView.setLayoutParams(mlp);
        mPresetStyleGridView.setClipToPadding(false);
        mContainer.addView(mPresetStyleGridView);

        mBackButton.setColorFilter(mTheme.iconColor, PorterDuff.Mode.SRC_IN);
    }

    /**
     * Sets annotation style holder
     *
     * @param annotStyleHolder annotation style holder
     */
    public void setAnnotStyleHolder(AnnotStyle.AnnotStyleHolder annotStyleHolder) {
        mAnnotStyleHolder = annotStyleHolder;
    }

    private AnnotStyle getAnnotStyle() {
        return mAnnotStyleHolder.getAnnotStyle();
    }

    private AnnotationPropertyPreviewView getAnnotStylePreview() {
        return mAnnotStyleHolder.getAnnotPreview();
    }

    /**
     * Show style picker view with given styleMode, must be one of:
     * {@link com.pdftron.pdf.controls.AnnotStyleDialogFragment.SelectStyleMode#BORDER_STYLE},
     * {@link com.pdftron.pdf.controls.AnnotStyleDialogFragment.SelectStyleMode#LINE_STYLE},
     * {@link com.pdftron.pdf.controls.AnnotStyleDialogFragment.SelectStyleMode#LINE_START_STYLE},
     * {@link com.pdftron.pdf.controls.AnnotStyleDialogFragment.SelectStyleMode#LINE_END_STYLE}
     *
     * @param styleMode style mode
     */
    public void show(@AnnotStyleDialogFragment.SelectStyleMode int styleMode) {
        mStyleMode = styleMode;
        AnnotStyle annotStyle = getAnnotStyle();
        getAnnotStylePreview().setAnnotType(annotStyle.getAnnotType());
        getAnnotStylePreview().updateFillPreview(annotStyle);

        switch (styleMode) {
            case BORDER_STYLE:
                List<Integer> borderStyleList;
                if (mAnnotStyleHolder.getAnnotStyle().hasBorderStyleWithoutCloud()) {
                    borderStyleList = getBorderStylesWithoutCloud();
                } else {
                    borderStyleList = getBorderStyles();
                }
                mPresetStyleGridView.setStyleList(borderStyleList);
                mToolbarTitle.setText(R.string.tools_annotation_border_style);
                break;

            case LINE_STYLE:
                mPresetStyleGridView.setStyleList(getLineStyles());
                mToolbarTitle.setText(R.string.tools_annotation_border_style);
                break;

            case LINE_START_STYLE: {
                mPresetStyleGridView.setStyleList(getLineEndingStyles());
                mToolbarTitle.setText(R.string.tools_qm_line_start);
                break;
            }

            case LINE_END_STYLE: {
                mPresetStyleGridView.setStyleList(getLineEndingStyles(), true);
                mToolbarTitle.setText(R.string.tools_qm_line_end);
                break;
            }
        }
        mPresetStyleGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onPresetStyleGridItemClicked(parent, position);
            }
        });

        setVisibility(VISIBLE);
    }

    private List<Integer> getBorderStylesWithoutCloud() {
        List<Integer> borderStyles = new ArrayList<>();
        for (ShapeBorderStyle value : ShapeBorderStyle.values()) {
            if (value != ShapeBorderStyle.CLOUDY) {
                borderStyles.add(value.getResource());
            }
        }
        return borderStyles;
    }

    private List<Integer> getBorderStyles() {
        List<Integer> borderStyles = new ArrayList<>();
        for (ShapeBorderStyle value : ShapeBorderStyle.values()) {
            borderStyles.add(value.getResource());
        }
        return borderStyles;
    }

    private List<Integer> getLineStyles() {
        List<Integer> lineEndingStyles = new ArrayList<>();
        for (LineStyle value : LineStyle.values()) {
            lineEndingStyles.add(value.getResource());
        }
        return lineEndingStyles;
    }

    private List<Integer> getLineEndingStyles() {
        List<Integer> lineEndingStyles = new ArrayList<>();
        for (LineEndingStyle value : LineEndingStyle.values()) {
            lineEndingStyles.add(value.getResource());
        }
        return lineEndingStyles;
    }

    /**
     * Hide Style picker view
     */
    public void dismiss() {
        setVisibility(GONE);
    }

    private void onBackButtonPressed() {
        if (mBackPressedListener != null) {
            mBackPressedListener.onBackPressed();
        }
    }

    /**
     * Sets on back button pressed listener
     *
     * @param listener back button pressed listener
     */
    public void setOnBackButtonPressedListener(OnBackButtonPressedListener listener) {
        mBackPressedListener = listener;
    }

    private void onStyleChanged(View view, int style) {
        switch (mStyleMode) {
            case BORDER_STYLE:
                getAnnotStyle().setBorderStyle(ShapeBorderStyle.fromInteger(style));
                break;
            case LINE_STYLE:
                getAnnotStyle().setLineStyle(LineStyle.fromInteger(style));
                break;
            case LINE_START_STYLE:
                getAnnotStyle().setLineStartStyle(LineEndingStyle.fromInteger(style));
                break;
            case LINE_END_STYLE:
                getAnnotStyle().setLineEndStyle(LineEndingStyle.fromInteger(style));
                break;
        }
        getAnnotStylePreview().updateFillPreview(getAnnotStyle());
    }

    private void onPresetStyleGridItemClicked(AdapterView<?> parent, int position) {
        onStyleChanged(parent, position);
        onBackButtonPressed();
    }

    /**
     * This method is used for back button in toolbar pressed event
     */
    public interface OnBackButtonPressedListener {
        /**
         * This method is invoked when back button in toolbar is pressed
         */
        void onBackPressed();
    }
}
