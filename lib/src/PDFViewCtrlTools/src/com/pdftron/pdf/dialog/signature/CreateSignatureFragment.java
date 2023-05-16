package com.pdftron.pdf.dialog.signature;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.pdftron.pdf.asynctask.LoadFontAsyncTask;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.controls.AnnotStyleDialogFragment;
import com.pdftron.pdf.interfaces.OnCreateSignatureListener;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.AnnotStyleProperty;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.model.LineEndingStyle;
import com.pdftron.pdf.model.LineStyle;
import com.pdftron.pdf.model.RulerItem;
import com.pdftron.pdf.model.ShapeBorderStyle;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.StampManager;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.signature.VariableWidthSignatureView;
import com.pdftron.pdf.widget.toolbar.component.view.ActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static com.pdftron.pdf.dialog.signature.SignatureDialogFragmentBuilder.BUNDLE_ANNOT_STYLE_PROPERTY;

public class CreateSignatureFragment extends Fragment {

    private final static String BUNDLE_COLOR = "bundle_color";
    private final static String BUNDLE_SIGNATURE_COLORS = "bundle_signature_colors";
    private final static String BUNDLE_STROKE_WIDTH = "bundle_stroke_width";
    private final static String BUNDLE_SHOW_SIGNATURE_FROM_IMAGE = "bundle_signature_from_image";
    private final static String BUNDLE_SHOW_TYPED_SIGNATURE = "bundle_typed_signature";
    private final static String BUNDLE_SHOW_SIGNATURE_PRESETS = "bundle_signature_presets";
    private final static String BUNDLE_SHOW_SAVED_SIGNATURES = "bundle_show_saved_signature";
    private final static String BUNDLE_PRESSURE_SENSITIVE = "bundle_pressure_sensitive";
    private final static String BUNDLE_DEFAULT_STORE_NEW_SIGNATURE = "bundle_store_new_signature";
    private final static String BUNDLE_PERSIST_STORE_SIGNATURE = "bundle_persist_store_signature";

    private static final String STYLE_TAG_1 = "style_tag_1";
    private static final String STYLE_TAG_2 = "style_tag_2";
    private static final String STYLE_TAG_3 = "style_tag_3";

    private final static String PREF_STORE_SIGNATURE = "CreateSignatureFragment_store_signature";
    private final static String PREF_SELECTED_STYLE_INDEX = "CreateSignatureFragment_selected_style_index";

    private OnCreateSignatureListener mOnCreateSignatureListener;
    private Toolbar mToolbar;
    private Button mClearButton;

    private ImageButton mStyleBtn1;
    private ImageButton mStyleBtn2;
    private ImageButton mStyleBtn3;
    private View mSelectedBtn;
    protected SwitchCompat mStoreSignatureSwitch;

    private RelativeLayout mSignatureContainer;
    private RelativeLayout mTypedSignatureContainer;

    private View mTopReserve;
    private View mBottomReserve;

    private TextView mTypedSignatureTextView;
    private EditText mTypedSignatureEditText;

    private int mColor;
    private float mStrokeWidth;
    private FontResource mFont;
    private boolean mShowSignatureFromImage;
    private boolean mShowTypedSignature;
    private boolean mShowSignaturePresets;
    private boolean mShowSavedSignatures;
    private boolean mIsPressureSensitive = true; // by default pressure sensitivity is enabled
    private HashMap<Integer, AnnotStyleProperty> mAnnotStyleProperties;

    private final ArrayList<AnnotStyle> mDrawStyles = new ArrayList<>();

    private VariableWidthSignatureView mSignatureView;

    private Theme mTheme;

    private boolean mDefaultStoreNewSignature = true;
    private boolean mPersistStoreSignatureSetting = true;

    private int[] mSignatureColors;

    public static CreateSignatureFragment newInstance(int color, float strokeWidth,
            boolean showSignatureFromImage, boolean showTypedSignature, boolean showSignaturePresets, boolean showSavedSignatures,
            boolean isPressureSensitive, HashMap<Integer, AnnotStyleProperty> annotStyleProperties,
            boolean defaultStoreNewSignature, boolean persistStoreSignatureSetting, int... signatureColors) {
        CreateSignatureFragment fragment = new CreateSignatureFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(BUNDLE_COLOR, color);
        bundle.putFloat(BUNDLE_STROKE_WIDTH, strokeWidth);
        bundle.putBoolean(BUNDLE_SHOW_SIGNATURE_FROM_IMAGE, showSignatureFromImage);
        bundle.putBoolean(BUNDLE_SHOW_TYPED_SIGNATURE, showTypedSignature);
        bundle.putBoolean(BUNDLE_SHOW_SIGNATURE_PRESETS, showSignaturePresets);
        bundle.putBoolean(BUNDLE_SHOW_SAVED_SIGNATURES, showSavedSignatures);
        bundle.putBoolean(BUNDLE_PRESSURE_SENSITIVE, isPressureSensitive);
        bundle.putBoolean(BUNDLE_DEFAULT_STORE_NEW_SIGNATURE, defaultStoreNewSignature);
        bundle.putBoolean(BUNDLE_PERSIST_STORE_SIGNATURE, persistStoreSignatureSetting);
        bundle.putSerializable(BUNDLE_ANNOT_STYLE_PROPERTY, annotStyleProperties);
        bundle.putIntArray(BUNDLE_SIGNATURE_COLORS, signatureColors);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static CreateSignatureFragment newInstance(int color, float strokeWidth,
            boolean showSignatureFromImage, boolean showSignaturePresets, boolean showSavedSignatures,
            boolean isPressureSensitive, HashMap<Integer, AnnotStyleProperty> annotStyleProperties,
            boolean defaultStoreNewSignature, boolean persistStoreSignatureSetting) {
        return newInstance(
                color,
                strokeWidth,
                showSignatureFromImage,
                true,
                showSignaturePresets,
                showSavedSignatures,
                isPressureSensitive,
                annotStyleProperties,
                defaultStoreNewSignature,
                persistStoreSignatureSetting
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        mTheme = Theme.fromContext(activity);

        Bundle arg = getArguments();
        if (arg != null) {
            mColor = arg.getInt(BUNDLE_COLOR);
            mStrokeWidth = arg.getFloat(BUNDLE_STROKE_WIDTH);
            mShowSignatureFromImage = arg.getBoolean(BUNDLE_SHOW_SIGNATURE_FROM_IMAGE, true);
            mShowTypedSignature = arg.getBoolean(BUNDLE_SHOW_TYPED_SIGNATURE, true);
            mShowSignaturePresets = arg.getBoolean(BUNDLE_SHOW_SIGNATURE_PRESETS, true);
            mShowSavedSignatures = arg.getBoolean(BUNDLE_SHOW_SAVED_SIGNATURES, true);
            mIsPressureSensitive = arg.getBoolean(BUNDLE_PRESSURE_SENSITIVE, mIsPressureSensitive);
            mDefaultStoreNewSignature = arg.getBoolean(BUNDLE_DEFAULT_STORE_NEW_SIGNATURE, mDefaultStoreNewSignature);
            mPersistStoreSignatureSetting = arg.getBoolean(BUNDLE_PERSIST_STORE_SIGNATURE, mPersistStoreSignatureSetting);
            mAnnotStyleProperties = (HashMap<Integer, AnnotStyleProperty>) arg.getSerializable(BUNDLE_ANNOT_STYLE_PROPERTY);
            mSignatureColors = arg.getIntArray(BUNDLE_SIGNATURE_COLORS);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tools_dialog_create_signature, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSignatureContainer = view.findViewById(R.id.tools_dialog_floating_sig_signature_view);
        mTypedSignatureContainer = view.findViewById(R.id.tools_dialog_signature_typed_container);

        mTopReserve = view.findViewById(R.id.top_reserve);
        mBottomReserve = view.findViewById(R.id.bottom_reserve);

        adjustForOrientation(getResources().getConfiguration().orientation);

        mSignatureView = new VariableWidthSignatureView(view.getContext(), null);
        mSignatureView.setPressureSensitivity(mIsPressureSensitive);
        mSignatureView.setColor(mColor);
        mSignatureView.setStrokeWidth(mStrokeWidth);
        mSignatureView.addListener(new VariableWidthSignatureView.InkListener() {
            @Override
            public void onInkStarted() {
                // Enable/disable buttons
                setClearButtonEnabled(true);
            }

            @Override
            public void onInkCompleted(List<double[]> mStrokeOutline) {
                createSignature(getContext(), mStrokeOutline);
            }
        });
        mSignatureContainer.addView(mSignatureView);

        // Clear button
        mClearButton = view.findViewById(R.id.tools_dialog_floating_sig_button_clear);
        setClearButtonEnabled(false);
        mClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSignatureView.clear();
                mTypedSignatureEditText.setText("");
                setClearButtonEnabled(false);
            }
        });

        // image button
        ImageButton imageButton = view.findViewById(R.id.tools_dialog_floating_sig_button_image);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setStoreSignature(v.getContext(), mStoreSignatureSwitch.isChecked());
                if (mOnCreateSignatureListener != null) {
                    mOnCreateSignatureListener.onSignatureFromImage(null, -1, null); // will be set in hosting signature fragment
                }
            }
        });
        if (mShowSignatureFromImage) {
            imageButton.setVisibility(View.VISIBLE);
        } else {
            imageButton.setVisibility(View.GONE);
        }

        imageButton.setColorFilter(mTheme.iconColor);

        // style icons
        // add presets
        for (int i = 0; i < 3; ++i) {
            AnnotStyle annotStyle = ToolStyleConfig.getInstance().getCustomAnnotStyle(view.getContext(), AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE, getStyleTag(i));
            mDrawStyles.add(annotStyle);
        }

        mStyleBtn1 = view.findViewById(R.id.color1);
        mStyleBtn2 = view.findViewById(R.id.color2);
        mStyleBtn3 = view.findViewById(R.id.color3);
        mStyleBtn1.setVisibility(mShowSignaturePresets ? View.VISIBLE : View.GONE);
        mStyleBtn2.setVisibility(mShowSignaturePresets ? View.VISIBLE : View.GONE);
        mStyleBtn3.setVisibility(mShowSignaturePresets ? View.VISIBLE : View.GONE);

        if (hasCustomSignatureColors()) {
            if (mSignatureColors.length == 1) {
                mStyleBtn2.setVisibility(View.GONE);
                mStyleBtn3.setVisibility(View.GONE);
            } else if (mSignatureColors.length == 2) {
                mStyleBtn3.setVisibility(View.GONE);
            }
        }

        mStyleBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedBtn != null && mSelectedBtn.getId() == v.getId() && !hasCustomSignatureColors()) {
                    showStylePicker(v, 0);
                    return;
                }
                updateButtonChecked((ImageButton) v);
            }
        });
        mStyleBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedBtn != null && mSelectedBtn.getId() == v.getId() && !hasCustomSignatureColors()) {
                    showStylePicker(v, 1);
                    return;
                }
                updateButtonChecked((ImageButton) v);
            }
        });
        mStyleBtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedBtn != null && mSelectedBtn.getId() == v.getId() && !hasCustomSignatureColors()) {
                    showStylePicker(v, 2);
                    return;
                }
                updateButtonChecked((ImageButton) v);
            }
        });

        // store signature switch
        mStoreSignatureSwitch = view.findViewById(R.id.btn_store_signature);
        mStoreSignatureSwitch.setVisibility(mShowSavedSignatures ? View.VISIBLE : View.INVISIBLE);

        // Save store signature setting if we have not done so yet. Or if not persisting, then reset
        // shared preferences to the the default value
        if (!containsStoreSignature(view.getContext()) || !mPersistStoreSignatureSetting) {
            setStoreSignature(view.getContext(), mDefaultStoreNewSignature);
        }
        mStoreSignatureSwitch.setChecked(getStoreSignature(view.getContext()));

        // typed signature
        mTypedSignatureEditText = view.findViewById(R.id.tools_dialog_floating_typed_signature_edittext);
        mTypedSignatureTextView = view.findViewById(R.id.tools_dialog_floating_typed_signature_textview); // invisible view used for auto-sizing
        mTypedSignatureEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mTypedSignatureTextView.setText(s);
                applyAutoResize();
                setClearButtonEnabled(!Utils.isNullOrEmpty(s.toString()));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ActionButton typedSignatureToggle = view.findViewById(R.id.tools_dialog_floating_sig_typed_signature_toggle);
        typedSignatureToggle.setCheckable(true);
        typedSignatureToggle.setShowIconHighlightColor(false);
        typedSignatureToggle.setIcon(view.getContext().getResources().getDrawable(R.drawable.ic_annotation_freetext_black_24dp));
        typedSignatureToggle.setIconColor(mTheme.iconColor);
        typedSignatureToggle.setSelectedIconColor(mTheme.selectedIconColor);
        typedSignatureToggle.setSelectedBackgroundColor(mTheme.selectedBackgroundColor);
        typedSignatureToggle.setSelected(false);
        typedSignatureToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                typedSignatureToggle.setSelected(!typedSignatureToggle.isSelected());
                setTypedMode(typedSignatureToggle.isSelected());
            }
        });
        typedSignatureToggle.setVisibility(mShowTypedSignature ? View.VISIBLE : View.GONE);

        loadFont();
        int selectedStyleIndex = getSelectedStyleIndex(view.getContext());
        if (hasCustomSignatureColors()) {
            // If there are less signature colors than buttons, then we should just select the first button
            if (mSignatureColors.length > selectedStyleIndex) {
                updateButtonChecked(getButtonFromIndex(selectedStyleIndex));
            } else {
                updateButtonChecked(getButtonFromIndex(0));
            }
        }

        setTypedMode(false);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        adjustForOrientation(newConfig.orientation);
    }

    private void adjustForOrientation(int orientation) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setConstraintHeightPercent(mTopReserve, 0);
            setConstraintHeightPercent(mBottomReserve, 0);
        } else {
            // Constraint height percent defined in tools_dialog_create_signature.xml
            setConstraintHeightPercent(mTopReserve, 0.15f);
            setConstraintHeightPercent(mBottomReserve, 0.25f);
        }
    }

    private void setConstraintHeightPercent(View view, float percent) {
        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) view.getLayoutParams();
        lp.matchConstraintPercentHeight = percent;
        view.setLayoutParams(lp);
    }

    private void updateButtonChecked(@NonNull ImageButton checkedButton) {
        mSelectedBtn = checkedButton;
        int index = getButtonIndex(checkedButton);
        if (hasCustomSignatureColors()) {
            Context context = checkedButton.getContext();
            switch (mSignatureColors.length) {
                default:
                    setButtonChecked(mStyleBtn3, R.drawable.layer_floating_sig_style_bg, mSignatureColors[2], checkedButton.getId() == mStyleBtn3.getId());
                case 2:
                    setButtonChecked(mStyleBtn2, R.drawable.layer_floating_sig_style_bg, mSignatureColors[1], checkedButton.getId() == mStyleBtn2.getId());
                case 1:
                    setButtonChecked(mStyleBtn1, R.drawable.layer_floating_sig_style_bg, mSignatureColors[0], checkedButton.getId() == mStyleBtn1.getId());

            }
            if (index < mSignatureColors.length) {
                mColor = mSignatureColors[index];
            }
        } else {
            setButtonChecked(mStyleBtn1, R.drawable.layer_floating_sig_style_bg, mDrawStyles.get(0).getColor(), checkedButton.getId() == mStyleBtn1.getId());
            setButtonChecked(mStyleBtn2, R.drawable.layer_floating_sig_style_bg, mDrawStyles.get(1).getColor(), checkedButton.getId() == mStyleBtn2.getId());
            setButtonChecked(mStyleBtn3, R.drawable.layer_floating_sig_style_bg, mDrawStyles.get(2).getColor(), checkedButton.getId() == mStyleBtn3.getId());
            mColor = mDrawStyles.get(index).getColor();
        }
        mFont = mDrawStyles.get(index).getFont();
        mSignatureView.setColor(mColor);
        setTextColor(mColor);
        setTextFont(mFont);
        setSelectedStyleIndex(checkedButton.getContext(), index);
    }

    @NonNull
    private ImageButton getButtonFromIndex(int index) {
        switch (index) {
            case 1:
                return mStyleBtn2;
            case 2:
                return mStyleBtn3;
            default:
                return mStyleBtn1;
        }
    }

    private int getButtonIndex(@NonNull ImageButton button) {
        if (button.getId() == mStyleBtn2.getId()) {
            return 1;
        } else if (button.getId() == mStyleBtn3.getId()) {
            return 2;
        }
        return 0;
    }

    private void setButtonChecked(@NonNull ImageButton button, @DrawableRes int iconId, @ColorInt int color, boolean checked) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        try {
            Drawable drawable = AppCompatResources.getDrawable(context, iconId);
            if (drawable instanceof LayerDrawable) {
                LayerDrawable layerDrawable = (LayerDrawable) drawable.mutate();
                GradientDrawable shape = (GradientDrawable) (layerDrawable.findDrawableByLayerId(R.id.selectable_shape));
                if (shape != null) {
                    int w = (int) Utils.convDp2Pix(context, 2.0f);
                    int strokeColor = checked ? Utils.getAccentColor(context) : Color.TRANSPARENT;
                    shape.setStroke(w, strokeColor);
                }

                Drawable circle = layerDrawable.findDrawableByLayerId(R.id.circle_shape);
                if (circle != null) {
                    circle.mutate();
                    // Apparently DrawableCompat.setTint is not good enough for pre Lollipop.
                    // We will need to set color filter manually for pre Lollipop.
                    if (Utils.isMarshmallow()) {
                        DrawableCompat.setTint(circle, color);
                    } else {
                        circle.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                    }
                }
                button.setImageDrawable(layerDrawable);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    private String getStyleTag(int index) {
        switch (index) {
            case 0:
                return STYLE_TAG_1;
            case 1:
                return STYLE_TAG_2;
            case 2:
                return STYLE_TAG_3;
        }
        return "";
    }

    private void setClearButtonEnabled(boolean enabled) {
        if (enabled) {
            mClearButton.setAlpha(1f);
        } else {
            mClearButton.setAlpha(0.54f);
        }
    }

    private void showStylePicker(View v, final int drawIndex) {
        // create style popup window
        AnnotStyle annotStyle = mDrawStyles.get(drawIndex);
        // get current signature button on screen
        int[] pos = new int[2];
        v.getLocationOnScreen(pos);

        Rect anchor = new Rect(pos[0], pos[1], pos[0] + v.getWidth(), pos[1] + v.getHeight());
        final AnnotStyleDialogFragment popupWindow =
                new AnnotStyleDialogFragment.Builder(annotStyle)
                        .setAnchorInScreen(anchor)
                        .setWhiteListFont(ToolStyleConfig.getInstance().getFreeTextFonts())
                        .setFontListFromAsset(ToolStyleConfig.getInstance().getFreeTextFontsFromAssets())
                        .setFontListFromStorage(ToolStyleConfig.getInstance().getFreeTextFontsFromStorage())
                        .setShowPressureSensitivePreview(mIsPressureSensitive)
                        .setShowPreview(!isTypedMode()) // in typed mode, the text hint is the preview
                        .build();
        AnnotStyleProperty styleProperty = mAnnotStyleProperties.get(AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE);
        if (styleProperty == null) {
            styleProperty = new AnnotStyleProperty(AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE);
            mAnnotStyleProperties.put(AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE, styleProperty);
        }
        if (isTypedMode()) {
            styleProperty.setCanShowThickness(false);
            styleProperty.setCanShowFont(true);
        } else {
            styleProperty.setCanShowThickness(true);
            styleProperty.setCanShowFont(false);
        }
        popupWindow.setAnnotStyleProperties(mAnnotStyleProperties);

        try {
            FragmentActivity activity = getActivity();
            if (activity == null) {
                AnalyticsHandlerAdapter.getInstance().sendException(new Exception("SignaturePickerDialog is not attached with an Activity"));
                return;
            }
            popupWindow.show(activity.getSupportFragmentManager(),
                    AnalyticsHandlerAdapter.STYLE_PICKER_LOC_SIGNATURE,
                    AnalyticsHandlerAdapter.getInstance().getAnnotationTool(AnalyticsHandlerAdapter.ANNOTATION_TOOL_SIGNATURE));
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }

        popupWindow.setOnAnnotStyleChangeListener(new AnnotStyle.OnAnnotStyleChangeListener() {
            @Override
            public void onChangeAnnotThickness(float thickness, boolean done) {
                if (done) {
                    mSignatureView.setStrokeWidth(thickness);
                    mStrokeWidth = thickness;

                    if (!isTypedMode()) {
                        CommonToast.showText(getContext(), R.string.signature_thickness_toast, Toast.LENGTH_LONG);
                    }
                }
            }

            @Override
            public void onChangeAnnotTextSize(float textSize, boolean done) {

            }

            @Override
            public void onChangeAnnotTextColor(int textColor) {

            }

            @Override
            public void onChangeAnnotOpacity(float opacity, boolean done) {

            }

            @Override
            public void onChangeAnnotStrokeColor(int color) {
                if (mSelectedBtn instanceof ImageButton) {
                    setButtonChecked((ImageButton) mSelectedBtn, R.drawable.layer_floating_sig_style_bg, color, true);
                }
                if (isTypedMode()) {
                    setTextColor(color);
                } else {
                    setInkColor(color);
                }
            }

            @Override
            public void onChangeAnnotFillColor(int color) {

            }

            @Override
            public void onChangeAnnotIcon(String icon) {

            }

            @Override
            public void onChangeAnnotFont(FontResource font) {
                if (isTypedMode()) {
                    setTextFont(font);
                }
            }

            @Override
            public void onChangeRulerProperty(RulerItem rulerItem) {

            }

            @Override
            public void onChangeOverlayText(String overlayText) {

            }

            @Override
            public void onChangeSnapping(boolean snap) {

            }

            @Override
            public void onChangeRichContentEnabled(boolean enabled) {

            }

            @Override
            public void onChangeDateFormat(String dateFormat) {

            }

            @Override
            public void onChangeAnnotBorderStyle(ShapeBorderStyle borderStyle) {

            }

            @Override
            public void onChangeAnnotLineStyle(LineStyle lineStyle) {

            }

            @Override
            public void onChangeAnnotLineStartStyle(LineEndingStyle lineStartStyle) {

            }

            @Override
            public void onChangeAnnotLineEndStyle(LineEndingStyle lineEndStyle) {

            }

            @Override
            public void onChangeTextAlignment(int horizontalAlignment, int verticalAlignment) {

            }
        });
        popupWindow.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                AnnotStyle annotStyle = popupWindow.getAnnotStyle();
                String extraTag = getStyleTag(drawIndex);
                ToolStyleConfig.getInstance().saveAnnotStyle(popupWindow.getContext(), annotStyle, extraTag);

                mDrawStyles.set(drawIndex, annotStyle);

                mColor = annotStyle.getColor();
                if (isTypedMode()) {
                    mFont = annotStyle.getFont();
                } else {
                    mStrokeWidth = annotStyle.getThickness();
                }
                updateButtonChecked(getButtonFromIndex(drawIndex));

                if (mOnCreateSignatureListener != null) {
                    mOnCreateSignatureListener.onAnnotStyleDialogFragmentDismissed(popupWindow);
                }
            }
        });
    }

    public static boolean getStoreSignature(@NonNull Context context) {
        SharedPreferences settings = Tool.getToolPreferences(context);
        return settings.getBoolean(PREF_STORE_SIGNATURE, true);
    }

    private boolean containsStoreSignature(@NonNull Context context) {
        SharedPreferences settings = Tool.getToolPreferences(context);
        return settings.contains(PREF_STORE_SIGNATURE);
    }

    private void setStoreSignature(@NonNull Context context, boolean storeSignature) {
        SharedPreferences settings = Tool.getToolPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREF_STORE_SIGNATURE, storeSignature);
        editor.apply();
    }

    private int getSelectedStyleIndex(@NonNull Context context) {
        SharedPreferences settings = Tool.getToolPreferences(context);
        return settings.getInt(PREF_SELECTED_STYLE_INDEX, 0);
    }

    private void setSelectedStyleIndex(@NonNull Context context, int index) {
        SharedPreferences settings = Tool.getToolPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(PREF_SELECTED_STYLE_INDEX, index);
        editor.apply();
    }

    /**
     * Sets the listener to {@link OnCreateSignatureListener}.
     *
     * @param listener The listener
     */
    public void setOnCreateSignatureListener(OnCreateSignatureListener listener) {
        mOnCreateSignatureListener = listener;
    }

    /**
     * Sets the main and cab toolbars.
     *
     * @param toolbar The toolbar with one action called Add
     */
    public void setToolbar(@NonNull Toolbar toolbar) {
        mToolbar = toolbar;
    }

    public void resetToolbar(final Context context) {
        if (mToolbar != null) {
            mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (mToolbar == null) {
                        return false;
                    }
                    if (item.getItemId() == R.id.controls_action_edit) {
                        if (isTypedMode()) {
                            createSignature();
                        } else {
                            mSignatureView.finish();
                        }
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    private void createSignature(@Nullable Context context, @NonNull List<double[]> strokes) {
        if (context == null) {
            return;
        }
        if (strokes.isEmpty()) {
            return;
        }
        setStoreSignature(context, mStoreSignatureSwitch.isChecked());
        String signatureFilePath = StampManager.getInstance().getSignatureFilePath(context);
        boolean success = StampManager.getInstance().createVariableThicknessSignature(signatureFilePath,
                mSignatureView.getBoundingBox(),
                strokes,
                mColor, mStrokeWidth * 2.0f); // use stroke x 2 just in case
        if (mOnCreateSignatureListener != null) {
            mOnCreateSignatureListener.onSignatureCreated(success ? signatureFilePath : null, mStoreSignatureSwitch.isChecked());
        }
    }

    private void createSignature() {
        // https://support.pdftron.com/a/tickets/39330
        // Add null check for crash mitigation
        if (mTypedSignatureTextView.getText().toString().isEmpty() || mFont == null) {
            return;
        }
        setStoreSignature(mTypedSignatureTextView.getContext(), mStoreSignatureSwitch.isChecked());
        String signatureFilePath = StampManager.getInstance().getSignatureFilePath(mTypedSignatureTextView.getContext());
        boolean success = StampManager.getInstance().createTypedSignature(signatureFilePath, mTypedSignatureTextView, mColor, mFont.getPDFTronName());
        if (mOnCreateSignatureListener != null) {
            mOnCreateSignatureListener.onSignatureCreated(success ? signatureFilePath : null, mStoreSignatureSwitch.isChecked());
        }
    }

    private void setTypedMode(boolean typedMode) {
        if (typedMode) {
            mSignatureContainer.setVisibility(View.GONE);
            mTypedSignatureContainer.setVisibility(View.VISIBLE);

            mTypedSignatureEditText.requestFocus();
            Utils.showSoftKeyboard(getContext(), mTypedSignatureEditText);
        } else {
            mTypedSignatureContainer.setVisibility(View.GONE);
            mSignatureContainer.setVisibility(View.VISIBLE);

            mTypedSignatureEditText.clearFocus();
            Utils.hideSoftKeyboard(getContext(), mTypedSignatureEditText);
        }
    }

    private boolean isTypedMode() {
        return mTypedSignatureContainer.getVisibility() == View.VISIBLE;
    }

    private void setInkColor(int color) {
        mColor = color;
        mSignatureView.setColor(color);
    }

    private void setTextColor(int color) {
        mColor = color;
        mTypedSignatureTextView.setTextColor(color);
        mTypedSignatureEditText.setTextColor(color);
        mTypedSignatureEditText.setHintTextColor(color);
    }

    private void setTextFont(FontResource font) {
        mFont = font;
        try {
            Typeface typeFace = Typeface.createFromFile(font.getFilePath());
            mTypedSignatureTextView.setTypeface(typeFace);
            mTypedSignatureEditText.setTypeface(typeFace);
            applyAutoResize();
        } catch (Exception ignored) { // when font not found

        }
    }

    private void applyAutoResize() {
        mTypedSignatureEditText.post(new Runnable() {
            @Override
            public void run() {
                mTypedSignatureEditText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTypedSignatureTextView.getTextSize());
            }
        });
    }

    private boolean hasCustomSignatureColors() {
        return mSignatureColors != null && mSignatureColors.length > 0;
    }

    private void loadFont() {
        // font saved in annot style does not contain the absolute path
        // so we want to load and update it here
        Set<String> whiteListFonts = ToolStyleConfig.getInstance().getFreeTextFonts();
        Set<String> fontListFromAsset = ToolStyleConfig.getInstance().getFreeTextFontsFromAssets();
        Set<String> fontListFromStorage = ToolStyleConfig.getInstance().getFreeTextFontsFromStorage();
        boolean isCustomFont = false;
        if (fontListFromAsset != null && !fontListFromAsset.isEmpty()) {
            whiteListFonts = fontListFromAsset;
            isCustomFont = true;
        } else if (fontListFromStorage != null && !fontListFromStorage.isEmpty()) {
            whiteListFonts = fontListFromStorage;
            isCustomFont = true;
        }

        LoadFontAsyncTask fontAsyncTask = new LoadFontAsyncTask(getContext(), whiteListFonts);
        fontAsyncTask.setIsCustomFont(isCustomFont);
        fontAsyncTask.setCallback(new LoadFontAsyncTask.Callback() {
            @Override
            public void onFinish(ArrayList<FontResource> fonts) {
                // update styles
                for (FontResource font : fonts) {
                    if (font.equals(mDrawStyles.get(0).getFont())) {
                        mDrawStyles.get(0).getFont().setFilePath(font.getFilePath());
                    }
                    if (font.equals(mDrawStyles.get(1).getFont())) {
                        mDrawStyles.get(1).getFont().setFilePath(font.getFilePath());
                    }
                    if (font.equals(mDrawStyles.get(2).getFont())) {
                        mDrawStyles.get(2).getFont().setFilePath(font.getFilePath());
                    }
                }
                updateButtonChecked(getButtonFromIndex(getSelectedStyleIndex(mTypedSignatureContainer.getContext())));
            }
        });
        fontAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    static class Theme {
        @ColorInt
        public final int iconColor;
        @ColorInt
        public final int selectedIconColor;
        @ColorInt
        public final int selectedBackgroundColor;

        Theme(int iconColor, int selectedIconColor, int selectedBackgroundColor) {
            this.iconColor = iconColor;
            this.selectedIconColor = selectedIconColor;
            this.selectedBackgroundColor = selectedBackgroundColor;
        }

        public static Theme fromContext(@NonNull Context context) {
            final TypedArray a = context.obtainStyledAttributes(
                    null, R.styleable.CreateSignatureDialogTheme, R.attr.pt_create_signature_dialog_style, R.style.PTCreateSignatureDialogTheme);
            int iconColor = a.getColor(R.styleable.CreateSignatureDialogTheme_iconColor, context.getResources().getColor(R.color.tools_dialog_floating_sig_add_image_color));
            int selectedIconColor = a.getColor(R.styleable.CreateSignatureDialogTheme_selectedIconColor, context.getResources().getColor(R.color.annot_toolbar_selected_icon));
            int selectedBackgroundColor = a.getColor(R.styleable.CreateSignatureDialogTheme_selectedBackgroundColor, context.getResources().getColor(R.color.annot_toolbar_selected_background));
            a.recycle();
            return new Theme(iconColor, selectedIconColor, selectedBackgroundColor);
        }
    }
}
