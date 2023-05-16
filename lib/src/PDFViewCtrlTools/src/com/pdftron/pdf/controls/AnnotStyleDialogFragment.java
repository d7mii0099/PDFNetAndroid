//------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------
package com.pdftron.pdf.controls;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.transition.ChangeBounds;
import androidx.transition.Fade;
import androidx.transition.Slide;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayout;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.dialog.base.BaseBottomDialogFragment;
import com.pdftron.pdf.interfaces.OnDialogDismissListener;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.AnnotStyleProperty;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AnalyticsAnnotStylePicker;
import com.pdftron.pdf.utils.AnnotationPropertyPreviewView;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.WrapContentViewPager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Annotation style dialog fragment is a DialogFragment that shows annotation style properties
 * in a bottom sheet. With the style dialog, users can edit annotation styles easily with style presets
 * as well as choosing their own colors through the advanced color picker. The style dialog also provides
 * a recent and a favorite list for quick access.
 * <p>
 * <p>
 * You can show annotation style dialog as follows:
 * <pre>
 * AnnotStyle annotStyle = new AnnotStyle();
 * // set annotation type to annot Style
 * annotStyle.setAnnotType(Annot.e_Square);
 * // set blue stroke, yellow fill color, thickness 5, opacity 0.8 to annotation style
 * annotStyle.setStyle(Color.BLUE, Color.YELLOW, 5, 0.8);
 * AnnotStyleDialogFragment annotStyleDialog = new AnnotStyleDialogFragment.Builder(annotStyle).build();
 * annotStyleDialog.show(getActivity().getSupportFragmentManager());
 * </pre>
 */
public class AnnotStyleDialogFragment extends BaseBottomDialogFragment implements
        AnnotStyleView.OnPresetSelectedListener,
        AnnotStyle.AnnotStyleHolder,
        ViewPager.OnPageChangeListener {

    public static final String TAG = AnnotStyleDialogFragment.class.getName();

    /**
     * The selected color is the stroke color of annotation.
     */
    public static final int STROKE_COLOR = 0;
    /**
     * The selected color is the fill color of annotation.
     */
    public static final int FILL_COLOR = 1;
    /**
     * The selected color is the text color of {@link com.pdftron.pdf.annots.FreeText} annotation.
     */
    public static final int TEXT_COLOR = 2;

    /**
     * The selected color is the color of annotation obtained from {@link Annot#getColorAsRGB()}
     */
    public static final int COLOR = 3;

    /**
     * The selected style is the border style of annotation.
     */
    public static final int BORDER_STYLE = 0;

    /**
     * The selected style is the line style of annotation.
     */
    public static final int LINE_STYLE = 1;

    /**
     * The selected style is the line starting style of annotation.
     */
    public static final int LINE_START_STYLE = 2;

    /**
     * The selected style is the line ending style of annotation.
     */
    public static final int LINE_END_STYLE = 3;

    protected static final String ARGS_KEY_ANNOT_STYLE = "annotStyle";
    protected static final String ARGS_KEY_WHITE_LIST_FONT = "whiteListFont";
    protected static final String ARGS_KEY_FONT_LIST_FROM_ASSET = "fontListFromAsset";
    protected static final String ARGS_KEY_FONT_LIST_FROM_STORAGE = "fontListFromStorage";
    protected static final String ARGS_KEY_MORE_ANNOT_TYPES = "more_tools";
    protected static final String ARGS_KEY_MORE_ANNOT_TYPES_TAB_INDEX = "more_tools_tab_index";
    protected static final String ARGS_KEY_PRESSURE_SENSITIVE = "show_pressure_sensitive_preview";
    protected static final String ARGS_KEY_SHOW_PRESET = "show_preset";
    protected static final String ARGS_KEY_SHOW_PREVIEW = "show_preview";
    protected static final String ARGS_KEY_EXTRA_ANNOT_STYLES = "extraAnnotStyle";
    protected static final String ARGS_KEY_INITIAL_TAB_INDEX = "initialTabIndex";
    protected static final String ARGS_KEY_TAB_TITLES = "tabTitles";
    protected static final String ARGS_KEY_GROUP_ANNOT_TYPES = "group_annot_styles";

    private WrapContentViewPager mViewPager;
    private AnnotStylePagerAdapter mViewPagerAdapter;
    private TabLayout mTabLayout;

    private AnnotStyle.OnAnnotStyleChangeListener mAnnotAppearanceListener;

    private final SparseArray<AnnotStyle> mAnnotStyleMap = new SparseArray<>();
    private ArrayList<AnnotStyle> mExtraAnnotStyles;

    private Set<String> mWhiteFontList;
    private Set<String> mFontListFromAsset;
    private Set<String> mFontListFromStorage;
    private ArrayList<Integer> mMoreAnnotTypes;
    private int mMoreAnnotTypesTabIndex;
    private boolean mShowPressureSensitivePreview = false;
    private boolean mShowPresets = true;
    private boolean mShowPreview = true;
    private AnnotStyleView.OnMoreAnnotTypeClickedListener mMoreAnnotTypesClickListener;

    // rich text enabled
    private boolean mCanShowRCOption;
    private boolean mCanShowPressureOption;
    @Nullable
    private HashMap<Integer, AnnotStyleProperty> mAnnotStyleProperties;

    private boolean mCanShowTextAlignment = true;

    // tabs
    private int mInitialTabIndex;
    @Nullable
    private String[] mTabTitles;

    // group selection
    private ArrayList<AnnotStyle> mGroupAnnotStyles;

    /**
     * Creates a new instance of AnnotStyleDialogFragment
     *
     * @return a new AnnotStyleDialogFragment
     */
    public static AnnotStyleDialogFragment newInstance() {
        return new AnnotStyleDialogFragment();
    }

    @Override
    protected Dialog onCreateDialogImpl(@NonNull Context context) {
        return new Dialog(context, R.style.FullScreenDialogStyle) {
            @Override
            public void onBackPressed() {
                if (mViewPagerAdapter.getCurrentAnnotStyleView().getVisibility() == View.VISIBLE) {
                    AnnotStyleDialogFragment.this.dismiss();
                } else {
                    dismissColorPickerView();
                    dismissStylePickerView();
                }
            }
        };
    }

    @Override
    protected String getFragmentTag() {
        return TAG;
    }

    @Override
    protected int getContentLayoutResource() {
        return R.layout.controls_annot_style_content;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARGS_KEY_ANNOT_STYLE, mAnnotStyleMap.valueAt(0).toJSONString());
        if (mWhiteFontList != null) {
            outState.putStringArrayList(ARGS_KEY_WHITE_LIST_FONT, new ArrayList<>(mWhiteFontList));
        }
        if (mFontListFromAsset != null) {
            outState.putStringArrayList(ARGS_KEY_FONT_LIST_FROM_ASSET, new ArrayList<>(mFontListFromAsset));
        }
        if (mFontListFromStorage != null) {
            outState.putStringArrayList(ARGS_KEY_FONT_LIST_FROM_STORAGE, new ArrayList<>(mFontListFromStorage));
        }
        if (mExtraAnnotStyles != null && !mExtraAnnotStyles.isEmpty()) {
            ArrayList<String> extraAnnotStylesStr = new ArrayList<>(mExtraAnnotStyles.size());
            for (AnnotStyle annotStyle : mExtraAnnotStyles) {
                extraAnnotStylesStr.add(annotStyle.toJSONString());
            }
            outState.putStringArrayList(ARGS_KEY_EXTRA_ANNOT_STYLES, extraAnnotStylesStr);
        }
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            String annotStyleJSON = savedInstanceState.getString(ARGS_KEY_ANNOT_STYLE);
            if (!Utils.isNullOrEmpty(annotStyleJSON)) {
                AnnotStyle annotStyle = AnnotStyle.loadJSONString(annotStyleJSON);
                mAnnotStyleMap.put(0, annotStyle);
            }
            if (savedInstanceState.containsKey(ARGS_KEY_WHITE_LIST_FONT)) {
                ArrayList<String> whiteFontList = savedInstanceState.getStringArrayList(ARGS_KEY_WHITE_LIST_FONT);
                if (whiteFontList != null) {
                    mWhiteFontList = new LinkedHashSet<>(whiteFontList);
                }
            }
            if (savedInstanceState.containsKey(ARGS_KEY_FONT_LIST_FROM_ASSET)) {
                ArrayList<String> fontListFromAsset = savedInstanceState.getStringArrayList(ARGS_KEY_FONT_LIST_FROM_ASSET);
                if (fontListFromAsset != null) {
                    mFontListFromAsset = new LinkedHashSet<>(fontListFromAsset);
                }
            }
            if (savedInstanceState.containsKey(ARGS_KEY_FONT_LIST_FROM_STORAGE)) {
                ArrayList<String> fontListFromStorage = savedInstanceState.getStringArrayList(ARGS_KEY_FONT_LIST_FROM_STORAGE);
                if (fontListFromStorage != null) {
                    mFontListFromStorage = new LinkedHashSet<>(fontListFromStorage);
                }
            }
            if (savedInstanceState.containsKey(ARGS_KEY_EXTRA_ANNOT_STYLES)) {
                ArrayList<String> extraAnnotStylesJson = savedInstanceState.getStringArrayList(ARGS_KEY_EXTRA_ANNOT_STYLES);
                if (extraAnnotStylesJson != null && !extraAnnotStylesJson.isEmpty()) {
                    mExtraAnnotStyles = new ArrayList<>(extraAnnotStylesJson.size());
                    for (String annotStyleStr : extraAnnotStylesJson) {
                        mExtraAnnotStyles.add(AnnotStyle.loadJSONString(annotStyleStr));
                    }
                }
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
                // key: tab position, value: AnnotStyle
                mAnnotStyleMap.put(0, AnnotStyle.loadJSONString(annotStyleJSON));
            }
        }

        if (arguments.containsKey(ARGS_KEY_WHITE_LIST_FONT)) {
            ArrayList<String> whiteListFont = arguments.getStringArrayList(ARGS_KEY_WHITE_LIST_FONT);
            if (whiteListFont != null) {
                mWhiteFontList = new LinkedHashSet<>(whiteListFont);
            }
        }

        if (arguments.containsKey(ARGS_KEY_FONT_LIST_FROM_ASSET)) {
            ArrayList<String> fontListFromAsset = arguments.getStringArrayList(ARGS_KEY_FONT_LIST_FROM_ASSET);
            if (fontListFromAsset != null) {
                mFontListFromAsset = new LinkedHashSet<>(fontListFromAsset);
            }
        }

        if (arguments.containsKey(ARGS_KEY_FONT_LIST_FROM_STORAGE)) {
            ArrayList<String> fontListFromStorage = arguments.getStringArrayList(ARGS_KEY_FONT_LIST_FROM_STORAGE);
            if (fontListFromStorage != null) {
                mFontListFromStorage = new LinkedHashSet<>(fontListFromStorage);
            }
        }

        if (arguments.containsKey(ARGS_KEY_EXTRA_ANNOT_STYLES)) {
            ArrayList<String> extraAnnotStylesJson = arguments.getStringArrayList(ARGS_KEY_EXTRA_ANNOT_STYLES);
            if (extraAnnotStylesJson != null && !extraAnnotStylesJson.isEmpty()) {
                mExtraAnnotStyles = new ArrayList<>(extraAnnotStylesJson.size());
                int start = 1;
                for (String annotStyleStr : extraAnnotStylesJson) {
                    mExtraAnnotStyles.add(AnnotStyle.loadJSONString(annotStyleStr));
                    // key: tab position, value: AnnotStyle
                    mAnnotStyleMap.put(start++, AnnotStyle.loadJSONString(annotStyleStr));
                }
            }
        }

        if (arguments.containsKey(ARGS_KEY_MORE_ANNOT_TYPES)) {
            ArrayList<Integer> annotTypes = arguments.getIntegerArrayList(ARGS_KEY_MORE_ANNOT_TYPES);
            if (annotTypes != null) {
                mMoreAnnotTypes = new ArrayList<>(annotTypes);
                mMoreAnnotTypesTabIndex = arguments.getInt(ARGS_KEY_MORE_ANNOT_TYPES_TAB_INDEX, 0);
            }
        }

        if (arguments.containsKey(ARGS_KEY_PRESSURE_SENSITIVE)) {
            mShowPressureSensitivePreview = arguments.getBoolean(ARGS_KEY_PRESSURE_SENSITIVE);
        }

        if (arguments.containsKey(ARGS_KEY_SHOW_PRESET)) {
            mShowPresets = arguments.getBoolean(ARGS_KEY_SHOW_PRESET);
        }

        if (arguments.containsKey(ARGS_KEY_SHOW_PREVIEW)) {
            mShowPreview = arguments.getBoolean(ARGS_KEY_SHOW_PREVIEW);
        }

        if (arguments.containsKey(ARGS_KEY_INITIAL_TAB_INDEX)) {
            mInitialTabIndex = arguments.getInt(ARGS_KEY_INITIAL_TAB_INDEX, 0);
        }

        if (arguments.containsKey(ARGS_KEY_TAB_TITLES)) {
            mTabTitles = arguments.getStringArray(ARGS_KEY_TAB_TITLES);
        }

        if (arguments.containsKey(ARGS_KEY_GROUP_ANNOT_TYPES)) {
            ArrayList<String> annotStylesJson = arguments.getStringArrayList(ARGS_KEY_GROUP_ANNOT_TYPES);
            if (annotStylesJson != null && !annotStylesJson.isEmpty()) {
                mGroupAnnotStyles = new ArrayList<>(annotStylesJson.size());
                for (String annotStyleStr : annotStylesJson) {
                    mGroupAnnotStyles.add(AnnotStyle.loadJSONString(annotStyleStr));
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        mViewPager = view.findViewById(R.id.pager);
        mTabLayout = view.findViewById(R.id.tab_layout);
        mTabLayout.setupWithViewPager(mViewPager);

        mViewPagerAdapter = new AnnotStylePagerAdapter(this);
        mViewPager.setAdapter(mViewPagerAdapter);
        mViewPager.addOnPageChangeListener(this);

        if (mExtraAnnotStyles != null && !mExtraAnnotStyles.isEmpty()) {
            mTabLayout.setVisibility(View.VISIBLE);
        } else {
            mTabLayout.setVisibility(View.GONE);
        }

        mViewPager.setCurrentItem(mInitialTabIndex);

        view.findViewById(R.id.background).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mViewPagerAdapter.getCurrentAnnotStyleView().findFocus() != null &&
                        mViewPagerAdapter.getCurrentAnnotStyleView().findFocus() instanceof EditText) {
                    mViewPagerAdapter.getCurrentAnnotStyleView().findFocus().clearFocus();
                } else {
                    dismiss();
                }
            }
        });

        Theme theme = Theme.fromContext(view.getContext());
        mTabLayout.setBackgroundColor(theme.backgroundColor);
        mTabLayout.setTabTextColors(theme.tabTextColor, theme.tabSelectedTextColor);
        mTabLayout.setSelectedTabIndicatorColor(theme.tabIndicatorColor);

        return view;
    }

    public void setAnnotStyle(AnnotStyle annotStyle) {
        setAnnotStyle(getCurrentTabIndex(), annotStyle);
    }

    public void setAnnotStyle(int tabIndex, AnnotStyle annotStyle) {
        mAnnotStyleMap.put(tabIndex, annotStyle);
        if (mAnnotAppearanceListener != null) {
            annotStyle.setAnnotAppearanceChangeListener(mAnnotAppearanceListener);
        }
        if (mTabLayout != null) {
            if (mExtraAnnotStyles != null && !mExtraAnnotStyles.isEmpty()) {
                mTabLayout.setVisibility(View.VISIBLE);
            } else {
                mTabLayout.setVisibility(View.GONE);
            }
        }
        if (mViewPagerAdapter != null) {
            AnnotStyleView annotStyleView = mViewPagerAdapter.getAnnotStyleView(tabIndex);
            if (annotStyleView != null) {
                annotStyleView.setAnnotType(tabIndex, annotStyle.getAnnotType());
                annotStyleView.updateLayout();
                annotStyleView.deselectAllPresetsPreview();
                annotStyleView.checkPresets();
                annotStyleView.updateAnnotTypes();
            }
        }
    }

    public void setCanShowRichContentSwitch(boolean canShow) {
        setCanShowRichContentSwitch(getCurrentTabIndex(), canShow);
    }

    public void setCanShowRichContentSwitch(int tabIndex, boolean canShow) {
        mCanShowRCOption = canShow;
        if (mViewPagerAdapter != null) {
            AnnotStyleView annotStyleView = mViewPagerAdapter.getAnnotStyleView(tabIndex);
            if (annotStyleView != null) {
                annotStyleView.setCanShowRichContentSwitch(canShow);
            }
        }
    }

    public void setCanShowPressureSwitch(boolean canShow) {
        setCanShowPressureSwitch(getCurrentTabIndex(), canShow);
    }

    public void setCanShowPressureSwitch(int tabIndex, boolean canShow) {
        mCanShowPressureOption = canShow;
        if (mViewPagerAdapter != null) {
            AnnotStyleView annotStyleView = mViewPagerAdapter.getAnnotStyleView(tabIndex);
            if (annotStyleView != null) {
                annotStyleView.setCanShowPressureSwitch(canShow);
            }
        }
    }

    /**
     * Sets Annotation style change listener.
     *
     * @param listener annotation style change listener
     */
    public void setOnAnnotStyleChangeListener(AnnotStyle.OnAnnotStyleChangeListener listener) {
        mAnnotAppearanceListener = listener;
    }

    private void openColorPickerView(@SelectColorMode final int colorMode) {
        TransitionManager.beginDelayedTransition(mBottomSheet, getLayoutChangeTransition());
        mViewPagerAdapter.getCurrentAnnotStyleView().dismiss();
        mViewPagerAdapter.getCurrentColorPickerView().setAnnotStyleProperties(mAnnotStyleProperties);
        mViewPagerAdapter.getCurrentColorPickerView().show(colorMode);
        onAnnotStyleLayoutUpdated();
    }

    private void dismissColorPickerView() {
        TransitionManager.beginDelayedTransition(mBottomSheet, getLayoutChangeTransition());
        mViewPagerAdapter.getCurrentColorPickerView().dismiss();
        mViewPagerAdapter.getCurrentAnnotStyleView().show();
        onAnnotStyleLayoutUpdated();
    }

    private void openStylePickerView(@SelectColorMode final int styleMode) {
        TransitionManager.beginDelayedTransition(mBottomSheet, getLayoutChangeTransition());
        mViewPagerAdapter.getCurrentAnnotStyleView().dismiss();
        mViewPagerAdapter.getCurrentStylePickerView().show(styleMode);
        onAnnotStyleLayoutUpdated();
    }

    private void dismissStylePickerView() {
        TransitionManager.beginDelayedTransition(mBottomSheet, getLayoutChangeTransition());
        mViewPagerAdapter.getCurrentStylePickerView().dismiss();
        mViewPagerAdapter.getCurrentAnnotStyleView().show();
        onAnnotStyleLayoutUpdated();
    }

    private TransitionSet getLayoutChangeTransition() {
        TransitionSet transition = new TransitionSet();
        transition.addTransition(new ChangeBounds());
        Slide slideFromEnd = new Slide(Gravity.END);
        slideFromEnd.addTarget(mViewPagerAdapter.getCurrentColorPickerView());
        transition.addTransition(slideFromEnd);
        Slide slideFromStart = new Slide(Gravity.START);
        slideFromStart.addTarget(mViewPagerAdapter.getCurrentAnnotStyleView());
        transition.addTransition(slideFromStart);
        Fade fade = new Fade();
        fade.setDuration(100);
        fade.setStartDelay(50);
        transition.addTransition(fade);
        return transition;
    }

    /**
     * Overload implementation of {@link AnnotStyleView.OnPresetSelectedListener#onPresetSelected(AnnotStyle)}
     *
     * @param presetStyle presetStyle
     */
    @Override
    public void onPresetSelected(AnnotStyle presetStyle) {
        // call setAnnotStyle will update the real annotation
        if (mAnnotAppearanceListener != null) {
            presetStyle.setAnnotAppearanceChangeListener(mAnnotAppearanceListener);
        }

        AnnotStyle annotStyle = getAnnotStyle();
        mAnnotStyleMap.put(getCurrentTabIndex(), presetStyle);

        if (!presetStyle.equals(annotStyle)) {
            presetStyle.updateAllListeners();
        }
        mViewPagerAdapter.getCurrentAnnotStyleView().updateLayout();
        mViewPagerAdapter.getCurrentAnnotStyleView().deselectAllPresetsPreview();
        if (presetStyle.getBindedPreview() != null) {
            presetStyle.getBindedPreview().setSelected(true);
        }
    }

    /**
     * Overload implementation of {@link AnnotStyleView.OnPresetSelectedListener#onPresetDeselected(AnnotStyle)}
     *
     * @param presetStyle presetStyle
     */
    @Override
    public void onPresetDeselected(AnnotStyle presetStyle) {
        AnnotStyle annotStyle = new AnnotStyle(presetStyle);
        annotStyle.bindPreview(null);
        if (mAnnotAppearanceListener != null) {
            annotStyle.setAnnotAppearanceChangeListener(mAnnotAppearanceListener);
        }
        mAnnotStyleMap.put(getCurrentTabIndex(), annotStyle);
        mViewPagerAdapter.getCurrentAnnotStyleView().deselectAllPresetsPreview();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        saveAnnotStyles();

        mViewPager.removeOnPageChangeListener(this);
    }

    /**
     * Saves annotation styles to settings
     */
    public void saveAnnotStyles() {
        for (int i = 0; i < mViewPagerAdapter.getAnnotStyleViews().size(); i++) {
            AnnotStyleView annotStyleView = mViewPagerAdapter.getAnnotStyleViews().valueAt(i);
            annotStyleView.savePresets();
        }
        for (int i = 0; i < mViewPagerAdapter.getColorPickerViews().size(); i++) {
            ColorPickerView colorPickerView = mViewPagerAdapter.getColorPickerViews().valueAt(i);
            colorPickerView.saveColors();
        }
    }

    /**
     * Overload implementation of {@link AnnotStyle.AnnotStyleHolder#getAnnotStyle()}
     * Gets annotation style
     *
     * @return annotation style
     */
    @Override
    public AnnotStyle getAnnotStyle() {
        if (mAnnotStyleMap.size() > 0) {
            return mAnnotStyleMap.valueAt(getCurrentTabIndex());
        } else if (getArguments() != null && getArguments().containsKey(ARGS_KEY_ANNOT_STYLE) &&
                !Utils.isNullOrEmpty(getArguments().getString(ARGS_KEY_ANNOT_STYLE))) {
            String annotStyleJSON = getArguments().getString(ARGS_KEY_ANNOT_STYLE);
            AnnotStyle annotStyle = AnnotStyle.loadJSONString(annotStyleJSON);
            mAnnotStyleMap.put(getCurrentTabIndex(), annotStyle);
            return annotStyle;
        }
        return null;
    }

    @NonNull
    public ArrayList<AnnotStyle> getAnnotStyles() {
        ArrayList<AnnotStyle> annotStyles = new ArrayList<>();
        for (int i = 0; i < mAnnotStyleMap.size(); i++) {
            annotStyles.add(mAnnotStyleMap.valueAt(i));
        }
        return annotStyles;
    }

    public int getCurrentTabIndex() {
        return mViewPager != null ? mViewPager.getCurrentItem() : 0;
    }

    @Override
    public AnnotationPropertyPreviewView getAnnotPreview() {
        return mViewPagerAdapter.getCurrentPreview();
    }

    @Override
    public SparseArray<AnnotationPropertyPreviewView> getAnnotPreviews() {
        return mViewPagerAdapter.getPreviews();
    }

    @Override
    public void setAnnotPreviewVisibility(int visibility) {
        if (!mShowPreview) {
            visibility = View.GONE;
        }
        SparseArray<AnnotationPropertyPreviewView> previews = mViewPagerAdapter.getPreviews();
        for (int i = 0; i < previews.size(); i++) {
            AnnotationPropertyPreviewView preview = previews.valueAt(i);
            preview.setVisibility(visibility);
        }
        if (getView() != null && getView().findViewById(R.id.divider) != null) {
            getView().findViewById(R.id.divider).setVisibility(visibility);
        }
    }

    @Override
    public void onAnnotStyleLayoutUpdated() {
        if (mViewPagerAdapter != null && mViewPager != null) {
            View view = mViewPagerAdapter.getItem(mViewPager.getCurrentItem());
            if (view != null) {
                view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                mViewPager.setContentHeight(view.getMeasuredHeight());
                mViewPager.requestLayout();
            }
        }
    }

    /**
     * Sets more annot types row item click event listener
     *
     * @param listener The listener
     */
    public void setOnMoreAnnotTypesClickListener(AnnotStyleView.OnMoreAnnotTypeClickedListener listener) {
        mMoreAnnotTypesClickListener = listener;
        if (mViewPagerAdapter != null) {
            for (int i = 0; i < mViewPagerAdapter.getAnnotStyleViews().size(); i++) {
                AnnotStyleView annotStyleView = mViewPagerAdapter.getAnnotStyleViews().valueAt(i);
                annotStyleView.setOnMoreAnnotTypesClickListener(mMoreAnnotTypesClickListener);
            }
        }
    }

    /**
     * Sets the AnnotStyleProperties that will be used to hide elements of the AnnotStyleDialog.
     *
     * @param annotStyleProperties hash map of annot types and the AnnotStyleProperties
     */
    public void setAnnotStyleProperties(@Nullable HashMap<Integer, AnnotStyleProperty> annotStyleProperties) {
        this.mAnnotStyleProperties = annotStyleProperties;
        if (mViewPagerAdapter != null) {
            for (int i = 0; i < mViewPagerAdapter.getAnnotStyleViews().size(); i++) {
                AnnotStyleView annotStyleView = mViewPagerAdapter.getAnnotStyleViews().valueAt(i);
                annotStyleView.setAnnotStyleProperties(mAnnotStyleProperties);
            }
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        if (mViewPagerAdapter != null &&
                mViewPagerAdapter.getAnnotStyleViews().size() == mViewPagerAdapter.getCount()) {
            AnnotStyleView annotStyleView = mViewPagerAdapter.getAnnotStyleViews().valueAt(position);
            annotStyleView.updateLayout();
            annotStyleView.deselectAllPresetsPreview();
            annotStyleView.checkPresets();
            annotStyleView.updateAnnotTypes();
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }


    /**
     * Selected color mode for color picker view
     */
    @IntDef({STROKE_COLOR, FILL_COLOR, TEXT_COLOR, COLOR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SelectColorMode {
    }

    /**
     * Selected style mode for style picker view
     */
    @IntDef({BORDER_STYLE, LINE_STYLE, LINE_START_STYLE, LINE_END_STYLE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SelectStyleMode {
    }

    @Override
    public void dismiss(boolean waitBottomSheet) {
        super.dismiss(waitBottomSheet);
        if (!(waitBottomSheet && (mDialogBehavior instanceof BottomSheetBehavior))) {
            AnalyticsAnnotStylePicker.getInstance().dismissAnnotStyleDialog();
        }
    }

    /**
     * Sets whether to show text alignment in the annot style dialog
     *
     */
    public void setCanShowTextAlignment(boolean canShowTextAlignment) {
        mCanShowTextAlignment = canShowTextAlignment;
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
         * Sets whether to show pressure sensitive thickness preview for signatures.
         *
         * @param showPressurePreview true to show pressure sensitive thickness preview, false otherwise
         * @return The builder
         */
        public Builder setShowPressureSensitivePreview(boolean showPressurePreview) {
            bundle.putBoolean(ARGS_KEY_PRESSURE_SENSITIVE, showPressurePreview);
            return this;
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

        /**
         * Sets extra annotation styles to the builder, if valid, extra annotation styles will be shown in tabs
         *
         * @param extraAnnotStyles The extra annotation styles for building dialog.
         * @return The builder
         */
        public Builder setExtraAnnotStyles(@Nullable ArrayList<AnnotStyle> extraAnnotStyles) {
            if (extraAnnotStyles != null && !extraAnnotStyles.isEmpty()) {
                ArrayList<String> extraAnnotStylesJson = new ArrayList<>(extraAnnotStyles.size());
                for (AnnotStyle annotStyle : extraAnnotStyles) {
                    extraAnnotStylesJson.add(annotStyle.toJSONString());
                }
                bundle.putStringArrayList(ARGS_KEY_EXTRA_ANNOT_STYLES, extraAnnotStylesJson);
            }
            return this;
        }

        /**
         * Whether to show the preset list. By default this is true.
         *
         * @param showPreset true if showing the preset
         * @return The builder
         */
        public Builder setShowPreset(boolean showPreset) {
            bundle.putBoolean(ARGS_KEY_SHOW_PRESET, showPreset);
            return this;
        }

        /**
         * Whether to show the preview. By default this is true.
         *
         * @param showPreview true if showing the preview
         * @return The builder
         */
        public Builder setShowPreview(boolean showPreview) {
            bundle.putBoolean(ARGS_KEY_SHOW_PREVIEW, showPreview);
            return this;
        }

        /**
         * Sets white list fonts. You can get white list fonts from {@link ToolManager#getFreeTextFonts()}
         *
         * @param whiteListFont The white list fonts.
         * @return The builder
         */
        public Builder setWhiteListFont(@Nullable Set<String> whiteListFont) {
            if (whiteListFont != null) {
                ArrayList<String> whiteListFontArr = new ArrayList<>(whiteListFont);
                bundle.putStringArrayList(ARGS_KEY_WHITE_LIST_FONT, whiteListFontArr);
            }
            return this;
        }

        /**
         * Sets custom font list from Asset for free text tool.
         * You can get font list fonts from {@link ToolManager#getFreeTextFontsFromAssets()}
         *
         * @param fontList The font list from asset
         * @return The builder
         */
        public Builder setFontListFromAsset(@Nullable Set<String> fontList) {
            if (fontList != null) {
                ArrayList<String> fontListFromAsset = new ArrayList<>(fontList);
                bundle.putStringArrayList(ARGS_KEY_FONT_LIST_FROM_ASSET, fontListFromAsset);
            }
            return this;
        }

        /**
         * Sets custom font list from Storage for free text tool.
         * You can get font list fonts from {@link ToolManager#getFreeTextFontsFromStorage()}
         *
         * @param fontList The font list from storage
         * @return The builder
         */
        public Builder setFontListFromStorage(@Nullable Set<String> fontList) {
            if (fontList != null) {
                ArrayList<String> fontListFromStorage = new ArrayList<>(fontList);
                bundle.putStringArrayList(ARGS_KEY_FONT_LIST_FROM_STORAGE, fontListFromStorage);
            }
            return this;
        }

        /**
         * Sets anchor rectangle in window location for tablet mode.
         * The annotation style dialog will be displayed around the anchor rectangle.
         * <br/>
         * You can get window location of a view as follows:
         * <pre>
         * int[] pos = new int[2];
         * view.getLocationInWindow(pos);
         * RectF rect = new RectF(pos[0], pos[1], pos[0] + view.getWidth(), pos[1] + view.getHeight());
         * builder.setAnchor(rect);
         * </pre>
         * <p>
         * where {@code view} is an instance of View, and {@code builder} is an instance of {@link Builder}
         *
         * @param anchor The anchor rectangle
         * @return The builder
         */
        public Builder setAnchor(RectF anchor) {
            Bundle rect = new Bundle();
            rect.putInt("left", (int) anchor.left);
            rect.putInt("top", (int) anchor.top);
            rect.putInt("right", (int) anchor.right);
            rect.putInt("bottom", (int) anchor.bottom);
            bundle.putBundle(ARGS_KEY_ANCHOR, rect);
            return this;
        }

        /**
         * Sets anchor rectangle in window location for tablet mode. The annotation style dialog will be displayed around the anchor rectangle.
         * This is equivalent to call {@link #setAnchor(RectF)}
         *
         * @param anchor The anchor rectangle
         * @return The builder
         */
        public Builder setAnchor(Rect anchor) {
            Bundle rect = new Bundle();
            rect.putInt("left", anchor.left);
            rect.putInt("top", anchor.top);
            rect.putInt("right", anchor.right);
            rect.putInt("bottom", anchor.bottom);
            bundle.putBundle(ARGS_KEY_ANCHOR, rect);
            return this;
        }

        /**
         * Sets anchor view for tablet mode. The annotation style dialog will be displayed around the anchor rectangle.
         *
         * @param view The anchor view
         * @return The builder
         */
        public Builder setAnchorView(View view) {
            int[] pos = new int[2];
            view.getLocationInWindow(pos);
            return setAnchor(new Rect(pos[0], pos[1], pos[0] + view.getWidth(), pos[1] + view.getHeight()));
        }

        /**
         * Sets anchor rectangle in screen position for tablet mode. The annotation style dialog will be displayed around the anchor rectangle.
         * <br/>
         * You can get screen location of a view as follows:
         * <pre>
         * int[] pos = new int[2];
         * view.getLocationInScreen(pos);
         * RectF rect = new RectF(pos[0], pos[1], pos[0] + view.getWidth(), pos[1] + view.getHeight());
         * builder.setAnchorInScreen(rect);
         * </pre>
         * <p>
         * where {@code view} is an instance of View, and {@code builder} is an instance of {@link Builder}
         *
         * @param anchor The anchor rectangle
         * @return The builder
         */
        public Builder setAnchorInScreen(Rect anchor) {
            setAnchor(anchor);
            bundle.putBoolean(ARGS_KEY_ANCHOR_SCREEN, true);
            return this;
        }

        /**
         * Sets more annot types to show in annotation style dialog
         *
         * @param annotTypes annot types to add
         */
        public Builder setMoreAnnotTypes(@Nullable ArrayList<Integer> annotTypes) {
            if (annotTypes != null) {
                bundle.putIntegerArrayList(ARGS_KEY_MORE_ANNOT_TYPES, annotTypes);
            }
            return this;
        }

        /**
         * Sets more annot types to show in annotation style dialog
         *
         * @param annotTypes annot types to add
         */
        public Builder setMoreAnnotTypes(int tabIndex, @Nullable ArrayList<Integer> annotTypes) {
            if (annotTypes != null) {
                bundle.putIntegerArrayList(ARGS_KEY_MORE_ANNOT_TYPES, annotTypes);
                bundle.putInt(ARGS_KEY_MORE_ANNOT_TYPES_TAB_INDEX, tabIndex);
            }
            return this;
        }

        public Builder setGroupAnnotTypes(@Nullable ArrayList<AnnotStyle> annotStyles) {
            if (annotStyles != null && !annotStyles.isEmpty()) {
                ArrayList<String> annotStylesJson = new ArrayList<>(annotStyles.size());
                for (AnnotStyle annotStyle : annotStyles) {
                    annotStylesJson.add(annotStyle.toJSONString());
                }
                bundle.putStringArrayList(ARGS_KEY_GROUP_ANNOT_TYPES, annotStylesJson);
            }
            return this;
        }

        public Builder setInitialTabIndex(int tabIndex) {
            bundle.putInt(ARGS_KEY_INITIAL_TAB_INDEX, tabIndex);
            return this;
        }

        public Builder setTabTitles(@Nullable String[] tabTitles) {
            if (tabTitles != null) {
                bundle.putStringArray(ARGS_KEY_TAB_TITLES, tabTitles);
            }
            return this;
        }

        /**
         * Creates an {@link AnnotStyleDialogFragment} with the arguments supplied to this builder.
         *
         * @return An new instance of {@link AnnotStyleDialogFragment}
         */
        public AnnotStyleDialogFragment build() {
            AnnotStyleDialogFragment annotStyleDialogFragment = newInstance();
            annotStyleDialogFragment.setArguments(bundle);
            return annotStyleDialogFragment;
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final class Theme {
        @ColorInt
        public final int backgroundColor;
        @ColorInt
        public final int annotPreviewBackgroundColor;
        @ColorInt
        public final int textColor;
        @ColorInt
        public final int iconColor;
        @ColorInt
        public final int selectedBackgroundColor;
        @ColorInt
        public final int presetIconColor;
        @ColorInt
        public final int selectedPresetIconColor;
        @ColorInt
        public final int selectedPresetBackgroundColor;
        @ColorInt
        public final int tabIndicatorColor;
        @ColorInt
        public final int tabTextColor;
        @ColorInt
        public final int tabSelectedTextColor;

        public Theme(int backgroundColor, int annotPreviewBackgroundColor, int textColor,
                int iconColor, int selectedBackgroundColor,
                int presetIconColor, int selectedPresetIconColor, int selectedPresetBackgroundColor,
                int tabIndicatorColor, int tabTextColor, int tabSelectedTextColor) {
            this.backgroundColor = backgroundColor;
            this.annotPreviewBackgroundColor = annotPreviewBackgroundColor;
            this.textColor = textColor;
            this.iconColor = iconColor;
            this.selectedBackgroundColor = selectedBackgroundColor;
            this.presetIconColor = presetIconColor;
            this.selectedPresetIconColor = selectedPresetIconColor;
            this.selectedPresetBackgroundColor = selectedPresetBackgroundColor;
            this.tabIndicatorColor = tabIndicatorColor;
            this.tabTextColor = tabTextColor;
            this.tabSelectedTextColor = tabSelectedTextColor;
        }

        public static Theme fromContext(@NonNull Context context) {

            final TypedArray a = context.obtainStyledAttributes(
                    null, R.styleable.AnnotStyleDialogTheme, R.attr.pt_annot_style_dialog_style, R.style.AnnotStyleDialogTheme);

            int backgroundColor = a.getColor(R.styleable.AnnotStyleDialogTheme_backgroundColor, Utils.getBackgroundColor(context));
            int annotPreviewBackgroundColor = a.getColor(R.styleable.AnnotStyleDialogTheme_annotPreviewBackgroundColor, context.getResources().getColor(R.color.annot_toolbar_background_secondary));
            int textColor = a.getColor(R.styleable.AnnotStyleDialogTheme_textColor, Utils.getPrimaryTextColor(context));
            int iconColor = a.getColor(R.styleable.AnnotStyleDialogTheme_iconColor, Utils.getForegroundColor(context));
            int selectedBackgroundColor = a.getColor(R.styleable.AnnotStyleDialogTheme_selectedBackgroundColor, context.getResources().getColor(R.color.annot_toolbar_selected_background));
            int presetIconColor = a.getColor(R.styleable.AnnotStyleDialogTheme_presetIconColor, context.getResources().getColor(R.color.annot_toolbar_icon));
            int selectedPresetIconColor = a.getColor(R.styleable.AnnotStyleDialogTheme_selectedPresetIconColor, context.getResources().getColor(R.color.annot_toolbar_selected_icon));
            int selectedPresetBackgroundColor = a.getColor(R.styleable.AnnotStyleDialogTheme_selectedPresetBackgroundColor, Utils.getBackgroundColor(context));
            int tabIndicatorColor = a.getColor(R.styleable.AnnotStyleDialogTheme_tabIndicatorColor, Utils.getAccentColor(context));
            int tabTextColor = a.getColor(R.styleable.AnnotStyleDialogTheme_tabTextColor, context.getResources().getColor(R.color.annot_toolbar_icon));
            int tabSelectedTextColor = a.getColor(R.styleable.AnnotStyleDialogTheme_tabSelectedTextColor, context.getResources().getColor(R.color.annot_toolbar_icon));
            a.recycle();

            return new Theme(backgroundColor, annotPreviewBackgroundColor, textColor,
                    iconColor, selectedBackgroundColor,
                    presetIconColor, selectedPresetIconColor, selectedPresetBackgroundColor,
                    tabIndicatorColor, tabTextColor, tabSelectedTextColor);
        }
    }

    private class AnnotStylePagerAdapter extends PagerAdapter {

        private final SparseArray<View> mContainers = new SparseArray<>();

        private final SparseArray<AnnotStyleView> mAnnotStyleViews = new SparseArray<>();
        private final SparseArray<ColorPickerView> mColorPickerViews = new SparseArray<>();
        private final SparseArray<StylePickerView> mStylePickerViews = new SparseArray<>();
        private final SparseArray<AnnotationPropertyPreviewView> mPreviews = new SparseArray<>();

        private final AnnotStyleDialogFragment mAnnotStyleFragment;

        public AnnotStylePagerAdapter(@NonNull AnnotStyleDialogFragment annotStyleFragment) {
            this.mAnnotStyleFragment = annotStyleFragment;
        }

        public SparseArray<AnnotStyleView> getAnnotStyleViews() {
            return mAnnotStyleViews;
        }

        public SparseArray<ColorPickerView> getColorPickerViews() {
            return mColorPickerViews;
        }

        public AnnotStyleView getCurrentAnnotStyleView() {
            return getAnnotStyleView(getCurrentTabIndex());
        }

        public AnnotStyleView getAnnotStyleView(int index) {
            return mAnnotStyleViews.get(index);
        }

        public ColorPickerView getCurrentColorPickerView() {
            return mColorPickerViews.get(getCurrentTabIndex());
        }

        public StylePickerView getCurrentStylePickerView() {
            return mStylePickerViews.get(getCurrentTabIndex());
        }

        public AnnotationPropertyPreviewView getCurrentPreview() {
            return mPreviews.get(getCurrentTabIndex());
        }

        public SparseArray<AnnotationPropertyPreviewView> getPreviews() {
            return mPreviews;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View view = LayoutInflater.from(container.getContext()).inflate(R.layout.controls_annot_style_content_tab, container, false);

            final AnnotStyleView annotStyleView = view.findViewById(R.id.annot_style);
            annotStyleView.setCanShowRichContentSwitch(mCanShowRCOption);
            annotStyleView.setCanShowTextAlignment(mCanShowTextAlignment);
            annotStyleView.setCanShowPressureSwitch(mCanShowPressureOption);
            annotStyleView.setShowPreset(mShowPresets);
            annotStyleView.setAnnotStyleProperties(mAnnotStyleProperties);
            annotStyleView.setGroupAnnotStyles(mGroupAnnotStyles);
            mAnnotStyleViews.put(position, annotStyleView);
            ColorPickerView colorPickerView = view.findViewById(R.id.color_picker);
            mColorPickerViews.put(position, colorPickerView);
            StylePickerView stylePickerView = view.findViewById(R.id.style_picker);
            mStylePickerViews.put(position, stylePickerView);

            // background
            Theme theme = Theme.fromContext(view.getContext());
            View contentContainer = view.findViewById(R.id.root_view);
            contentContainer.setBackgroundColor(theme.annotPreviewBackgroundColor);

            AnnotationPropertyPreviewView previewView = view.findViewById(R.id.preview);
            previewView.setVisibility(mShowPreview ? View.VISIBLE : View.GONE);
            View previewDivider = view.findViewById(R.id.divider);
            previewDivider.setVisibility(mShowPreview ? View.VISIBLE : View.GONE);
            previewView.setShowPressurePreview(mShowPressureSensitivePreview);
            mPreviews.put(position, previewView);

            colorPickerView.setActivity(getActivity());
            init(annotStyleView, colorPickerView, stylePickerView, previewView, position);

            container.addView(view);

            mContainers.put(position, view);

            return view;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        public View getItem(int position) {
            return mContainers.get(position);
        }

        @Override
        public int getCount() {
            return mExtraAnnotStyles != null ? (mExtraAnnotStyles.size() + 1) : 1;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            if (mTabTitles != null && mTabTitles.length == getCount()) {
                return mTabTitles[position];
            }
            return null;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        private void init(AnnotStyleView annotStyleView, ColorPickerView colorPickerView, StylePickerView stylePickerView,
                AnnotationPropertyPreviewView previewView, int position) {
            annotStyleView.setAnnotStyleHolder(this.mAnnotStyleFragment);
            colorPickerView.setAnnotStyleHolder(this.mAnnotStyleFragment);
            stylePickerView.setAnnotStyleHolder(this.mAnnotStyleFragment);
            annotStyleView.setOnPresetSelectedListener(this.mAnnotStyleFragment);
            annotStyleView.setOnColorLayoutClickedListener(new AnnotStyleView.OnColorLayoutClickedListener() {
                @Override
                public void onColorLayoutClicked(int colorMode) {
                    openColorPickerView(colorMode);
                }
            });
            colorPickerView.setOnBackButtonPressedListener(new ColorPickerView.OnBackButtonPressedListener() {
                @Override
                public void onBackPressed() {
                    dismissColorPickerView();
                }
            });
            annotStyleView.setOnStyleLayoutClickedListener(new AnnotStyleView.OnStyleLayoutClickedListener() {
                @Override
                public void onStyleLayoutClicked(int styleMode) {
                    openStylePickerView(styleMode);
                }
            });
            stylePickerView.setOnBackButtonPressedListener(new StylePickerView.OnBackButtonPressedListener() {
                @Override
                public void onBackPressed() {
                    dismissStylePickerView();
                }
            });
            if (mMoreAnnotTypesClickListener != null) {
                annotStyleView.setOnMoreAnnotTypesClickListener(mMoreAnnotTypesClickListener);
            }
            if (mWhiteFontList != null && !mWhiteFontList.isEmpty()) {
                annotStyleView.setWhiteFontList(mWhiteFontList);
            }
            if (mFontListFromAsset != null && !mFontListFromAsset.isEmpty()) {
                annotStyleView.setFontListFromAsset(mFontListFromAsset);
            }
            if (mFontListFromStorage != null && !mFontListFromStorage.isEmpty()) {
                annotStyleView.setFontListFromStorage(mFontListFromStorage);
            }
            if (mMoreAnnotTypes != null && position == mMoreAnnotTypesTabIndex) {
                annotStyleView.setMoreAnnotTypes(mMoreAnnotTypes);
            }
            annotStyleView.setOnDismissListener(new OnDialogDismissListener() {
                @Override
                public void onDialogDismiss() {
                    dismiss();
                }
            });
            AnnotStyle annotStyle = mAnnotStyleMap.valueAt(position);
            annotStyleView.setAnnotType(position, annotStyle.getAnnotType());
            annotStyleView.updateLayout();

            annotStyleView.checkPresets();

            if (mAnnotAppearanceListener != null) {
                annotStyle.setAnnotAppearanceChangeListener(mAnnotAppearanceListener);
            }

            previewView.setAnnotType(annotStyle.getAnnotType());
        }
    }
}
