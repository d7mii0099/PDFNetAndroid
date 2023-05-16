package com.pdftron.pdf.dialog.measurecount;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.transition.ChangeBounds;
import androidx.transition.Fade;
import androidx.transition.Slide;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.pdftron.pdf.controls.AnnotStyleDialogFragment;
import com.pdftron.pdf.controls.ColorPickerView;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.AnnotStyleProperty;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnnotationPropertyPreviewView;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.preset.component.PresetBarViewModel;

import java.util.HashMap;
import java.util.List;

public class CountToolCreatePresetDialog extends DialogFragment implements
        AnnotStyle.AnnotStyleHolder {

    public interface CountToolCreatePresetDialogListener {
        void onPresetChanged(String newLabel, String oldLabel, AnnotStyle annotStyle);
    }

    public static final String TAG = CountToolCreatePresetDialog.class.getName();

    protected static final String ARGS_KEY_ANNOT_STYLE = "annotStyle";
    private final String EDIT_MODE = "edit_mode";
    private final String CREATE_MODE = "create_mode";
    private String mCurrentMode;
    private List<MeasureCountTool> mMeasureCountTools;
    private CountToolCreatePresetDialogListener mListener;

    private AnnotStyle mAnnotStyle;
    private ColorPickerView mColorPickerView;
    private AnnotationPropertyPreviewView mPreview;
    private View mRootView;
    private View mMainView;
    private TextInputEditText mLabelName;
    private TextInputLayout mLabelNameLayout;
    private AnnotationPropertyPreviewView mFillPreview;
    private PresetBarViewModel mPresetBarViewModel;
    private MeasureCountToolViewModel mMeasureCountToolViewModel;
    private String mToolbarStyleId;

    @Nullable
    private HashMap<Integer, AnnotStyleProperty> mAnnotStyleProperties;

    /**
     * Creates a new instance of CountToolCreatePresetDialog
     *
     * @return a new CountToolCreatePresetDialog
     */
    public static CountToolCreatePresetDialog newInstance() {
        return new CountToolCreatePresetDialog();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_count_tool_create_preset, container, false);
        mMainView = view.findViewById(R.id.first_page);

        LinearLayout fillLayout = view.findViewById(R.id.count_tool_create_preset_fill_color_layout);
        mFillPreview = view.findViewById(R.id.count_tool_create_preset_fill_preview);

        mLabelName = view.findViewById(R.id.count_tool_create_preset_group_label);
        mLabelNameLayout = view.findViewById(R.id.count_tool_create_preset_group_label_layout);
        TextView title = view.findViewById(R.id.count_tool_create_preset_title);

        if (mAnnotStyle.hasStampId() && !mAnnotStyle.getStampId().isEmpty()) {
            title.setText(title.getContext().getString(R.string.count_measurement_edit_group));
            mLabelName.setText(mAnnotStyle.getStampId());
            mCurrentMode = EDIT_MODE;
        } else {
            mCurrentMode = CREATE_MODE;
            mAnnotStyle.setStrokeColor(ContextCompat.getColor(view.getContext(), R.color.fab_light_blue));
            title.setText(title.getContext().getString(R.string.count_measurement_add_group));
        }

        AppCompatImageView fillColorExpandBtn = view.findViewById(R.id.count_tool_create_preset_fill_color_expand_button);
        updateFillColour();
        fillLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openColorPickerView(AnnotStyleDialogFragment.COLOR);
            }
        });

        Button saveBtn = view.findViewById(R.id.count_tool_create_preset_button_positive);
        Button cancelBtn = view.findViewById(R.id.count_tool_create_preset_button_negative);
        if (mCurrentMode.equals(EDIT_MODE)) {
            saveBtn.setText(R.string.ok);
        } else {
            saveBtn.setText(R.string.add);
        }
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLabelName != null && mLabelName.getText() != null) {
                    String labelName = mLabelName.getText().toString();
                    if (isValidGroupName(labelName)) {
                        if (mCurrentMode.equals(CREATE_MODE)) {
                            MeasureCountTool preset = new MeasureCountTool();
                            mAnnotStyle.setStampId(labelName);
                            preset.annotStyleJson = mAnnotStyle.toJSONString();
                            preset.label = labelName;
                            if (mPresetBarViewModel != null) {
                                mPresetBarViewModel.saveCountMeasurementPreset(labelName, mAnnotStyle, mToolbarStyleId, 0);
                            }
                            if (mMeasureCountToolViewModel != null) {
                                mMeasureCountToolViewModel.insert(preset);
                            }
                        } else {
                            if (mListener != null) {
                                String originalLabel = mAnnotStyle.getStampId();
                                mListener.onPresetChanged(labelName, originalLabel, mAnnotStyle);
                            }
                        }
                        dismiss();
                    }
                }
            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mColorPickerView = view.findViewById(R.id.color_picker);

        // background
        Theme theme = Theme.fromContext(view.getContext());
        View contentContainer = view.findViewById(R.id.root_view);
        contentContainer.setBackgroundColor(theme.backgroundColor);
        fillColorExpandBtn.setColorFilter(theme.iconColor);

        mPreview = view.findViewById(R.id.preview);
        mPreview.setVisibility(View.GONE);

        mColorPickerView.setActivity(getActivity());
        init(mColorPickerView);

        mRootView = view;
        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARGS_KEY_ANNOT_STYLE, mAnnotStyle.toJSONString());
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            String annotStyleJSON = savedInstanceState.getString(ARGS_KEY_ANNOT_STYLE);
            if (!Utils.isNullOrEmpty(annotStyleJSON)) {
                mAnnotStyle = AnnotStyle.loadJSONString(annotStyleJSON);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        Bundle arguments = getArguments();
        if (arguments == null) {
            return;
        }
        if (arguments.containsKey(ARGS_KEY_ANNOT_STYLE)) {
            String annotStyleJSON = arguments.getString(ARGS_KEY_ANNOT_STYLE);
            if (!Utils.isNullOrEmpty(annotStyleJSON)) {
                mAnnotStyle = AnnotStyle.loadJSONString(annotStyleJSON);
            }
        }

        if (getActivity() != null) {
            mMeasureCountToolViewModel = ViewModelProviders.of(getActivity()).get(MeasureCountToolViewModel.class);
            mMeasureCountToolViewModel.observeCountToolPresets(getActivity(), new Observer<List<MeasureCountTool>>() {
                @Override
                public void onChanged(List<MeasureCountTool> measureCountTools) {
                    mMeasureCountTools = measureCountTools;
                }
            });
        }
    }

    /**
     * Sets the AnnotStyleProperties that will be used to hide elements of the AnnotStyleDialog.
     *
     * @param annotStyleProperties hash map of annot types and the AnnotStyleProperties
     */
    public void setAnnotStyleProperties(@Nullable HashMap<Integer, AnnotStyleProperty> annotStyleProperties) {
        this.mAnnotStyleProperties = annotStyleProperties;
    }

    public void setPresetBarViewModel(PresetBarViewModel presetBarViewModel) {
        mPresetBarViewModel = presetBarViewModel;
    }

    public void setListener(CountToolCreatePresetDialogListener listener) {
        mListener = listener;
    }

    public void setMeasureCountToolViewModel(MeasureCountToolViewModel measureCountToolViewModel) {
        mMeasureCountToolViewModel = measureCountToolViewModel;
    }

    public void setToolbarStyleId(String toolbarStyleId) {
        mToolbarStyleId = toolbarStyleId;
    }

    private void openColorPickerView(@AnnotStyleDialogFragment.SelectColorMode final int colorMode) {
        TransitionManager.beginDelayedTransition((ViewGroup) mRootView, getLayoutChangeTransition());
        mColorPickerView.setAnnotStyleProperties(mAnnotStyleProperties);
        mColorPickerView.show(colorMode);
        mMainView.setVisibility(View.GONE);
        onAnnotStyleLayoutUpdated();
    }

    private TransitionSet getLayoutChangeTransition() {
        TransitionSet transition = new TransitionSet();
        transition.addTransition(new ChangeBounds());
        Slide slideFromEnd = new Slide(Gravity.END);
        slideFromEnd.addTarget(mColorPickerView);
        transition.addTransition(slideFromEnd);
        Slide slideFromStart = new Slide(Gravity.START);
        slideFromStart.addTarget(mMainView);
        transition.addTransition(slideFromStart);
        Fade fade = new Fade();
        fade.setDuration(100);
        fade.setStartDelay(50);
        transition.addTransition(fade);
        return transition;
    }

    @Override
    public void dismiss() {
        super.dismiss();
        saveAnnotStyles();
    }

    public void setAnnotStyle(AnnotStyle annotStyle) {
        mAnnotStyle = annotStyle;
    }

    @Override
    public AnnotStyle getAnnotStyle() {
        return mAnnotStyle;
    }

    @Override
    public AnnotationPropertyPreviewView getAnnotPreview() {
        return mPreview;
    }

    @Override
    public SparseArray<AnnotationPropertyPreviewView> getAnnotPreviews() {
        SparseArray<AnnotationPropertyPreviewView> previews = new SparseArray<>();
        previews.put(0, mPreview);
        return previews;
    }

    @Override
    public void setAnnotPreviewVisibility(int visibility) {

    }

    @Override
    public void onAnnotStyleLayoutUpdated() {
        updateFillColour();
    }

    /**
     * Saves annotation styles to settings
     */
    public void saveAnnotStyles() {
        mColorPickerView.saveColors();
    }

    private void dismissColorPickerView() {
        TransitionManager.beginDelayedTransition((ViewGroup) mRootView, getLayoutChangeTransition());
        mColorPickerView.dismiss();
        mMainView.setVisibility(View.VISIBLE);
        onAnnotStyleLayoutUpdated();
    }

    private boolean isValidGroupName(String labelName) {
        mLabelNameLayout.setErrorEnabled(false);
        if (mCurrentMode.equals(EDIT_MODE)) {
            String originalLabel = mAnnotStyle.getStampId();
            if (!originalLabel.isEmpty() && originalLabel.equals(labelName)) {
                return true;
            }
        }
        if (!labelName.isEmpty()) {
            if (mMeasureCountTools != null && !mMeasureCountTools.isEmpty()) {
                for (int i = 0; i < mMeasureCountTools.size(); i++) {
                    MeasureCountTool preset = mMeasureCountTools.get(i);
                    if (preset.label.trim().equals(labelName.trim())) {
                        mLabelNameLayout.setError(mLabelNameLayout.getContext().getString(R.string.count_measurement_group_label_already_exists));
                        return false;
                    }
                }
            }
            return true;
        } else {
            mLabelNameLayout.setError(mLabelNameLayout.getContext().getString(R.string.count_measurement_name_required));
        }
        return false;
    }

    private void updateFillColour() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        int backgroundColor = Utils.getBackgroundColor(context);

        // set stroke preview
        Drawable drawable;
        if (mAnnotStyle.getColor() == Color.TRANSPARENT) {
            drawable = context.getResources().getDrawable(R.drawable.oval_fill_transparent);
        } else if (mAnnotStyle.getColor() == backgroundColor) {
            if (mAnnotStyle.hasFillColor()) {
                drawable = context.getResources().getDrawable(R.drawable.ring_stroke_preview);
            } else {
                drawable = context.getResources().getDrawable(R.drawable.oval_stroke_preview);
            }

            drawable.mutate();
            ((GradientDrawable) drawable).setStroke((int) Utils.convDp2Pix(getContext(), 1), Color.GRAY);
        } else {
            if (mAnnotStyle.hasFillColor()) {
                drawable = context.getResources().getDrawable(R.drawable.oval_stroke_preview);
            } else {
                drawable = context.getResources().getDrawable(R.drawable.oval_fill_preview);
            }
            drawable.mutate();
            drawable.setColorFilter(mAnnotStyle.getColor(), PorterDuff.Mode.SRC_IN);
        }
        mFillPreview.setImageDrawable(drawable);
    }

    private void init(ColorPickerView colorPickerView) {
        colorPickerView.setAnnotStyleHolder(this);
        colorPickerView.setIsDialogLayout(true); //fixes the colour wheel cut off issue in the alert dialog
        colorPickerView.setOnBackButtonPressedListener(new ColorPickerView.OnBackButtonPressedListener() {
            @Override
            public void onBackPressed() {
                dismissColorPickerView();
            }
        });
    }

    /**
     * Builder for building annotation style dialog
     */
    public static class Builder {
        Bundle bundle;

        /**
         * Creates a builder for an annotation style dialog
         */
        public Builder() {
            bundle = new Bundle();
        }

        /**
         * Creates a builder for an annotation style dialog with given annotation style
         *
         * @param annotStyle The annotation style for building the dialog
         */
        public Builder(AnnotStyle annotStyle) {
            bundle = new Bundle();
            setAnnotStyle(annotStyle);
        }

        /**
         * Sets annotation style to the builder, it is used for setting annotation style for dialog
         *
         * @param annotStyle The annotation style for building dialog. This is equivalent to call: {@code new Builder(annotStyle)}
         * @return The builder
         */
        public Builder setAnnotStyle(AnnotStyle annotStyle) {
            bundle.putString(ARGS_KEY_ANNOT_STYLE, annotStyle.toJSONString());
            return this;
        }

        public CountToolCreatePresetDialog build() {
            CountToolCreatePresetDialog dialog = newInstance();
            dialog.setArguments(bundle);
            return dialog;
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final class Theme {
        @ColorInt
        public final int backgroundColor;
        @ColorInt
        public final int textColor;
        @ColorInt
        public final int iconColor;

        public Theme(int backgroundColor,
                int textColor,
                int iconColor) {
            this.backgroundColor = backgroundColor;
            this.textColor = textColor;
            this.iconColor = iconColor;
        }

        public static Theme fromContext(@NonNull Context context) {

            final TypedArray a = context.obtainStyledAttributes(
                    null, R.styleable.CountPresetDialogTheme, R.attr.pt_count_preset_dialog_style, R.style.PTCountPresetDialogTheme);

            int backgroundColor = a.getColor(R.styleable.CountPresetDialogTheme_backgroundColor, Utils.getBackgroundColor(context));
            int textColor = a.getColor(R.styleable.CountPresetDialogTheme_textColor, Utils.getPrimaryTextColor(context));
            int iconColor = a.getColor(R.styleable.CountPresetDialogTheme_iconColor, Utils.getForegroundColor(context));
            a.recycle();

            return new Theme(backgroundColor, textColor, iconColor);
        }
    }
}

