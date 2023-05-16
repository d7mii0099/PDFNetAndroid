package com.pdftron.pdf.widget.preset.component.view;

import android.animation.Animator;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.preset.component.PresetBarTheme;
import com.pdftron.pdf.widget.preset.component.model.PresetBarState;
import com.pdftron.pdf.widget.preset.component.model.PresetButtonState;
import com.pdftron.pdf.widget.preset.component.model.SinglePresetState;
import com.pdftron.pdf.widget.toolbar.component.AnnotationToolbarTheme;
import com.pdftron.pdf.widget.toolbar.component.view.ActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that contains the Android views related to the preset bar,
 * and exposes necessary preset bar related API.
 */
public class PresetBarView {

    public static final int MAX_NUMBER_OF_PRESETS = 4;

    protected boolean mCompactMode;

    public interface OnPresetViewButtonClickListener {
        void onPresetButtonClicked(int index);
    }

    public interface OnCloseButtonClickListener {
        void onCloseButtonClicked();
    }

    public interface OnStyleButtonClickListener {
        void onStyleButtonClicked();
    }

    private final List<OnPresetViewButtonClickListener> mPresetListeners = new ArrayList<>();
    private final List<OnCloseButtonClickListener> mCloseListeners = new ArrayList<>();
    private final List<OnStyleButtonClickListener> mStyleListeners = new ArrayList<>();

    @NonNull
    protected ViewGroup mParent;
    protected ViewGroup mRootContainer;
    protected final PresetBarTheme mPresetBarTheme;

    protected LinearLayout mPresetContainer;
    protected FrameLayout mOverlayContainer;
    protected final List<PresetActionButton> mPresetButtons = new ArrayList<>(); // position of these buttons in the list correspond to preset index
    protected FrameLayout mCloseContainer;
    protected AppCompatImageView mClose;
    protected PresetSingleButton mSinglePresetButton;

    protected final List<FrameLayout> mPresetContainersList = new ArrayList<>();

    public PresetBarView(@NonNull ViewGroup parent) {
        Context context = parent.getContext();
        mPresetBarTheme = PresetBarTheme.fromContext(context);
        mParent = parent;
        LayoutInflater.from(parent.getContext()).inflate(R.layout.toolbar_preset_bar, parent, true);

        mRootContainer = mParent.findViewById(R.id.preset_bar_container);
        mRootContainer.setBackgroundColor(mPresetBarTheme.backgroundColor);
        mPresetContainer = mParent.findViewById(R.id.preset_button_container);
        mOverlayContainer = mParent.findViewById(R.id.overlay_container);
        hidePresetBar(false);

        // multi presets
        for (int i = 0; i < MAX_NUMBER_OF_PRESETS; i++) {
            // Create our preset button
            PresetActionButton actionButton = new PresetActionButton(parent.getContext());
            actionButton.setIconColor(mPresetBarTheme.iconColor);
            actionButton.setExpandStyleIconColor(mPresetBarTheme.expandIconColor);
            actionButton.setSelectedIconColor(mPresetBarTheme.selectedIconColor);
            actionButton.setDisabledIconColor(mPresetBarTheme.disabledIconColor);
            actionButton.setSelectedBackgroundColor(mPresetBarTheme.selectedBackgroundColor);
            actionButton.setClientBackgroundColor(mPresetBarTheme.backgroundColor);
            if (Utils.isTablet(context) || Utils.isLandscape(context)) {
                // tablet has different background color
                AnnotationToolbarTheme annotationToolbarTheme = AnnotationToolbarTheme.fromContext(context);
                actionButton.setClientBackgroundColor(annotationToolbarTheme.backgroundColorSecondary);

                // tablet has different selection background color
                actionButton.setSelectedBackgroundColor(mPresetBarTheme.selectedBackgroundColorSecondary);
            }
            actionButton.setCheckable(true);
            actionButton.setAlwaysShowIconHighlightColor(true);
            setupPreset(actionButton, i);
            actionButton.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
            ));

            // Add the preset button into a frame layout (centered in middle) so that we can use
            // linear layout weight without effecting the button background
            FrameLayout container = new FrameLayout(parent.getContext());
            container.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
            ));
            container.addView(actionButton);
            mPresetContainersList.add(container);

            // Finally add our button to the list of presets
            mPresetContainer.addView(container);
            mPresetButtons.add(actionButton); // keep a reference to our buttons for later
        }

        // single preset
        mSinglePresetButton = new PresetSingleButton(parent.getContext());
        mSinglePresetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (OnStyleButtonClickListener listener : mStyleListeners) {
                    listener.onStyleButtonClicked();
                }
            }
        });
        mSinglePresetButton.setBackgroundColor(mPresetBarTheme.selectedBackgroundColor);
        mSinglePresetButton.setIconColor(mPresetBarTheme.iconColor);
        mSinglePresetButton.setTextColor(mPresetBarTheme.accentColor);
        mSinglePresetButton.setExpandStyleIconColor(mPresetBarTheme.selectedIconColor);

        mCloseContainer = mParent.findViewById(R.id.close_container);
        mClose = mCloseContainer.findViewById(R.id.close);
        mClose.setColorFilter(mPresetBarTheme.iconColor);
        mCloseContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (OnCloseButtonClickListener listener : mCloseListeners) {
                    listener.onCloseButtonClicked();
                }
            }
        });
    }

    public void setCompactMode(boolean compactMode) {
        mCompactMode = compactMode;
        updateTheme();
    }

    public void updateTheme() {
        Context context = getContext();
        if (Utils.isTablet(context) || Utils.isLandscape(context)) {
            AnnotationToolbarTheme annotationToolbarTheme = AnnotationToolbarTheme.fromContext(context);
            if (mCompactMode) {
                mRootContainer.setBackgroundColor(annotationToolbarTheme.backgroundColor);
                for (PresetActionButton actionButton : mPresetButtons) {
                    // tablet has different background color
                    actionButton.setClientBackgroundColor(annotationToolbarTheme.backgroundColor);

                    // tablet has different selection color - background color
                    actionButton.setSelectedBackgroundColor(mPresetBarTheme.selectedBackgroundColor);
                }
            } else {
                mRootContainer.setBackgroundColor(annotationToolbarTheme.backgroundColorSecondary);
                for (PresetActionButton actionButton : mPresetButtons) {
                    // tablet has different background color
                    actionButton.setClientBackgroundColor(annotationToolbarTheme.backgroundColorSecondary);

                    // tablet has different selection background color
                    actionButton.setSelectedBackgroundColor(mPresetBarTheme.selectedBackgroundColorSecondary);
                }
            }
        }
    }

    public void setSinglePreset(boolean singlePreset) {
        if (singlePreset) {
            mPresetContainer.setVisibility(View.GONE);
            mPresetContainer.removeAllViews();

            mOverlayContainer.setVisibility(View.VISIBLE);
            mOverlayContainer.removeAllViews();
            mOverlayContainer.addView(mSinglePresetButton);
            Context context = mSinglePresetButton.getContext();
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mSinglePresetButton.getLayoutParams();
            // Add margins for the single preset
            if (!Utils.isLandscape(getContext()) && !Utils.isTablet(getContext())) {
                int horizontalMargin = (int) Utils.convDp2Pix(context, 16);
                int verticalMargin = (int) Utils.convDp2Pix(context, 8);
                layoutParams.setMargins(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin);
            } else {
                layoutParams.setMargins(0, 0, 0, 0);
            }
            layoutParams.height = (int) Utils.convDp2Pix(context, 40);
        } else {
            mPresetContainer.setVisibility(View.VISIBLE);

            mOverlayContainer.setVisibility(View.GONE);
            mPresetContainer.removeAllViews();
            mOverlayContainer.removeAllViews();
            for (FrameLayout layout : mPresetContainersList) {
                mPresetContainer.addView(layout);
            }
        }
    }

    public void singlePresetWithBackground(boolean enabled) {
        mSinglePresetButton.presetIconWithBackground(enabled);
    }

    public void updateSinglePreset(@NonNull SinglePresetState presetState) {
        if (presetState.getImageFile() != null) {
            mSinglePresetButton.setPresetFile(presetState.getImageFile());
        } else if (presetState.getEmptyStateDesc() != 0) {
            mSinglePresetButton.setEmptyState(presetState.getEmptyStateDesc());
        } else if (presetState.getAnnotStyle() != null) {
            mSinglePresetButton.setPresetAnnotStyle(presetState.getAnnotStyle(), presetState.getIconRes());
        } else {
            mSinglePresetButton.setPresetBitmap(presetState.getBitmap());
        }
    }

    private View setupPreset(@NonNull final ActionButton presetButton, final int index) {
        presetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (OnPresetViewButtonClickListener listener : mPresetListeners) {
                    listener.onPresetButtonClicked(index);
                }
            }
        });
        return presetButton;
    }

    public void updatePresetStyle(@NonNull PresetBarState presetBarState) {
        if (presetBarState.isVisible) {
            for (int i = 0; i < presetBarState.getNumberOfPresetStates(); i++) {
                PresetButtonState presetButtonState = presetBarState.getPresetState(i);
                if (presetButtonState.isSelected()) {
                    PresetActionButton view = mPresetButtons.get(i);
                    ArrayList<AnnotStyle> annotStyles = presetButtonState.getAnnotStyles();
                    if (annotStyles != null && !annotStyles.isEmpty()) {
                        view.updateAppearance(annotStyles);
                    }
                    return;
                }
            }
        }
    }

    public void updatePresetState(@NonNull PresetBarState presetBarState) {
        if (presetBarState.isVisible) {
            showPresetBar(true);
            for (int i = 0; i < presetBarState.getNumberOfPresetStates(); i++) {
                PresetButtonState presetButtonState = presetBarState.getPresetState(i);
                PresetActionButton view = mPresetButtons.get(i);
                ArrayList<AnnotStyle> annotStyles = presetButtonState.getAnnotStyles();
                if (annotStyles != null && !annotStyles.isEmpty()) {
                    view.setIcon(view.getResources().getDrawable(presetBarState.toolbarItem.icon));
                    view.updateAppearance(annotStyles);
                    boolean enabled = true;
                    for (AnnotStyle annotStyle : annotStyles) {
                        if (!annotStyle.isEnabled()) {
                            enabled = false;
                            break;
                        }
                    }
                    view.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
                    if (presetButtonState.isSelected()) {
                        view.select();
                    } else {
                        view.deselect();
                    }
                    if (presetBarState.styleDialogState == PresetBarState.PRESET_SELECTED) {
                        view.openStyle();
                    } else {
                        view.resetStyle();
                    }
                }
            }
        } else {
            hidePresetBar(true);
        }
    }

    public void addOnPresetButtonClickListener(@NonNull OnPresetViewButtonClickListener listener) {
        mPresetListeners.add(listener);
    }

    public void removeOnPresetButtonClickListener(@NonNull OnPresetViewButtonClickListener listener) {
        mPresetListeners.remove(listener);
    }

    public void addOnCloseButtonClickListener(@NonNull OnCloseButtonClickListener listener) {
        mCloseListeners.add(listener);
    }

    public void removeOnCloseButtonClickListener(@NonNull OnCloseButtonClickListener listener) {
        mCloseListeners.remove(listener);
    }

    public void addOnStyleButtonClickListener(@NonNull OnStyleButtonClickListener listener) {
        mStyleListeners.add(listener);
    }

    public void removeOnStyleButtonClickListener(@NonNull OnStyleButtonClickListener listener) {
        mStyleListeners.remove(listener);
    }

    public void showPresetBar(boolean animate) {
        mRootContainer.animate().cancel();
        if (mRootContainer.getVisibility() != View.INVISIBLE) { // already showing
            return;
        }
        if (animate) {
            mRootContainer.animate().translationY(0)
                    .setDuration(100)
                    .setInterpolator(new DecelerateInterpolator())
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            mRootContainer.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            mRootContainer.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    });
        } else {
            mRootContainer.setTranslationY(0);
            mRootContainer.setVisibility(View.VISIBLE);
        }
    }

    public void hidePresetBar(boolean animate) {
        mRootContainer.animate().cancel();
        if (mRootContainer.getVisibility() != View.VISIBLE) { // already hidden
            return;
        }
        if (animate) {
            mRootContainer.animate().translationY(mRootContainer.getHeight())
                    .setDuration(100)
                    .setInterpolator(new AccelerateInterpolator())
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mRootContainer.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            mRootContainer.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    });
        } else {
            mRootContainer.setTranslationY(mRootContainer.getHeight());
            mRootContainer.setVisibility(View.INVISIBLE);
        }
    }

    public Context getContext() {
        return mParent.getContext();
    }

    public View getParent() {
        return mParent;
    }
}
