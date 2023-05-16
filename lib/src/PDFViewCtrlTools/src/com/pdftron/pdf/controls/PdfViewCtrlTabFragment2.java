//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.app.Activity;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.dialog.BookmarksDialogFragment;
import com.pdftron.pdf.interfaces.OnAnnotStyleChangedListener;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.Pan;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.pdf.viewmodel.ViewerShortcutViewModel;
import com.pdftron.pdf.widget.preset.component.PresetBarComponent;
import com.pdftron.pdf.widget.preset.component.PresetBarViewModel;
import com.pdftron.pdf.widget.preset.component.model.PresetButtonState;
import com.pdftron.pdf.widget.seekbar.DocumentSlider;

import java.util.ArrayList;

/**
 * The PdfViewCtrlTabFragment2 shows {@link com.pdftron.pdf.PDFViewCtrl} out of the box with a various
 * of controls such as {@link com.pdftron.pdf.widget.toolbar.component.AnnotationToolbarComponent},
 * {@link com.pdftron.pdf.widget.bottombar.component.BottomBarComponent}, and
 * {@link com.pdftron.pdf.controls.ThumbnailsViewFragment} etc.
 */
public class PdfViewCtrlTabFragment2 extends PdfViewCtrlTabBaseFragment implements
        DocumentSlider.OnDocumentSliderTrackingListener {

    private static final String TAG = PdfViewCtrlTabFragment2.class.getName();

    protected static final int HIDE_NAVIGATION_BAR_TIMER = 2000; // 2 sec

    // UI elements
    protected DocumentSlider mDocumentSlider;
    protected DocumentSlider mDocumentSliderVertical;

    protected boolean mShowSliderAfterAction;

    protected ComponentListener mComponentListener;
    protected ToolManager.SnackbarListener mSnackbarListener;

    /**
     * Callback interface to be invoked when an interaction is needed.
     */
    public interface TabListener extends PdfViewCtrlTabBaseFragment.TabListener {

    }

    public interface ComponentListener {
        PresetBarComponent getPresetBarComponent();
    }

    public void setComponentListener(ComponentListener listener) {
        mComponentListener = listener;
    }

    public void setSnackbarListener(ToolManager.SnackbarListener listener) {
        mSnackbarListener = listener;
    }

    @Override
    protected int getContentLayoutRes() {
        return R.layout.fragment_tabbed_pdfviewctrl_tab_content_new;
    }

    @Override
    protected void sliderRefreshPageCount() {
        getActiveSeekBar().refreshPageCount();
    }

    @Override
    protected void sliderUpdateProgress(int curPage) {
        if (getActiveSeekBar() != null && mToolManager.getTool() instanceof Pan && !mInSearchMode) {
            getActiveSeekBar().updateProgress();
        }
    }

    @Override
    protected void sliderSetReversed(boolean reversed) {
        if (mDocumentSlider != null) {
            mDocumentSlider.setReversed(reversed);
        }
    }

    @Override
    protected void sliderSetVisibility(int visibility) {
        if (getActiveSeekBar() != null) {
            getActiveSeekBar().setVisibility(visibility);
        }
    }

    @Override
    protected void preparingNavChange() {
        if (getActiveSeekBar() != null) {
            mShowSliderAfterAction = getActiveSeekBar().getVisibility() == View.VISIBLE;
        }
        // remove nav first as we are going through layout changes
        mDocumentSlider.dismiss(false);
        mDocumentSliderVertical.dismiss(false);
    }

    @Override
    protected void postNavChange() {
        if (mShowSliderAfterAction) {
            mShowSliderAfterAction = false;
            setThumbSliderVisible(true, true);
        }
    }

    @Override
    protected View[] getGenericMotionEnabledViews() {
        return new View[]{
                mDocumentSlider, mDocumentSliderVertical,
                mPageNumberIndicatorAll, mPageBackButton, mPageForwardButton
        };
    }

    @Override
    public boolean isAnnotationMode() {
        // TODO for keyboard shortcut
        return false;
    }

    @Override
    protected void consumeImageSignature() {
        if (mAnnotTargetPoint == null && (mTargetWidget == null || mTargetWidget == 0) && getParentFragment() != null) {
            // from preset bar
            PresetBarViewModel presetViewModel = ViewModelProviders.of(getParentFragment()).get(PresetBarViewModel.class);
            presetViewModel.saveStampPreset(AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE,
                    ViewerUtils.getImageSignaturePath(getActivity(),
                            mAnnotIntentData, mOutputFileUri));
        } else {
            super.consumeImageSignature();
        }
    }

    /**
     * Handles changes the page number.
     *
     * @param old_page the old page number
     * @param cur_page the current (new) page number
     * @param state    in non-continuous page presentation modes and when the
     *                 built-in page sliding is in process, this flag is used to
     *                 indicate the state of page change.
     */
    @Override
    public void onPageChange(int old_page, int cur_page, PDFViewCtrl.PageChangeState state) {
        Activity activity = getActivity();
        if (activity == null || mPdfViewCtrl == null) {
            return;
        }

        if (getActiveSeekBar() != null && getActiveSeekBar().isProgressChanging()) {
            // skip if we are still changing pages
            return;
        }

        super.onPageChange(old_page, cur_page, state);
    }

    /**
     * The overloaded implementation of {@link ToolManager.PreToolManagerListener#onScaleBegin(float, float)}.
     **/
    @Override
    public boolean onScaleBegin(float x, float y) {
        mScaling = true;
        if (getActiveSeekBar() != null) {
            mShowSliderAfterAction = getActiveSeekBar().getVisibility() == View.VISIBLE;
        }
        setThumbSliderVisible(false, false);
        return super.onScaleBegin(x, y);
    }

    /**
     * The overloaded implementation of {@link ToolManager.PreToolManagerListener#onScaleEnd(float, float)}.
     **/
    @Override
    public boolean onScaleEnd(float x, float y) {
        mScaling = false;
        if (mShowSliderAfterAction) {
            mShowSliderAfterAction = false;
            setThumbSliderVisible(true, true);
        }
        return super.onScaleEnd(x, y);
    }

    /**
     * The overloaded implementation of {@link ToolManager.PreToolManagerListener#onScrollChanged(int, int, int, int)}.
     **/
    @Override
    public void onScrollChanged(int l, int t, int oldl, int oldt) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
    }

    /**
     * The overloaded implementation of {@link DocumentSlider.OnDocumentSliderTrackingListener#onDocumentSliderStartTrackingTouch()}.
     **/
    @Override
    public void onDocumentSliderStartTrackingTouch() {
        hideBackAndForwardButtons();

        updateCurrentPageInfo();
    }

    /**
     * The overloaded implementation of {@link DocumentSlider.OnDocumentSliderTrackingListener#onDocumentSliderStopTrackingTouch(int)}.
     **/
    @Override
    public void onDocumentSliderStopTrackingTouch(int pageNum) {
        if (mTabListener != null) {
            mTabListener.onTabThumbSliderStopTrackingTouch();
        }

        setCurrentPageHelper(pageNum, false);
    }

    /**
     * Sets the {@link TabListener} listener.
     *
     * @param listener The listener
     */
    public void setTabListener(TabListener listener) {
        mTabListener = listener;
    }

    /**
     * Opens the navigation list as a side sheet
     */
    public void openNavigationList(BookmarksDialogFragment bookmarksDialogFragment) {
        openSideSheet(bookmarksDialogFragment,
                "bookmarks_dialog_" + mTabTag,
                0,
                0);
    }

    protected void loadPDFViewCtrlView() {
        super.loadPDFViewCtrlView();

        mToolManager.setStampDialogListener(new ToolManager.StampDialogListener() {
            @Override
            public void onSaveStampPreset(int annotType, @NonNull String stampId) {
                Fragment parentFragment = getParentFragment();
                if (parentFragment != null) {
                    PresetBarViewModel presetViewModel = ViewModelProviders.of(parentFragment).get(PresetBarViewModel.class);
                    presetViewModel.saveStampPreset(annotType, stampId);
                }
            }
        });
        mToolManager.setPresetsListener(new ToolManager.PresetsListener() {
            @Override
            public void onUpdatePresets(int annotType) {
                Fragment parentFragment = getParentFragment();
                if (parentFragment != null) {
                    PresetBarViewModel presetViewModel = ViewModelProviders.of(parentFragment).get(PresetBarViewModel.class);
                    presetViewModel.reloadPreset(parentFragment.getContext(), annotType);
                }
            }
        });
        mToolManager.setOnStyleChangedListener(new OnAnnotStyleChangedListener() {
            @Override
            public void onAnnotStyleColorChange(ArrayList<AnnotStyle> styles) {
                Fragment parentFragment = getParentFragment();
                if (parentFragment != null) {
                    PresetBarViewModel presetViewModel = ViewModelProviders.of(parentFragment).get(PresetBarViewModel.class);
                    Pair<PresetButtonState, Integer> presetPair = presetViewModel.getPresetBarState() != null ? presetViewModel.getPresetBarState().getActivePresetState() : null;
                    if (presetPair != null && presetPair.second != null) {
                        presetViewModel.updateAnnotStyles(styles, presetPair.second);
                    }
                }
            }

            @Override
            public void OnAnnotStyleDismiss(AnnotStyleDialogFragment styleDialogFragment) {
                if (mComponentListener != null) {
                    PresetBarComponent presetBarComponent = mComponentListener.getPresetBarComponent();
                    if (presetBarComponent != null) {
                        presetBarComponent.handleAnnotStyleDialogDismiss(styleDialogFragment);
                    }
                }
            }
        });
        mToolManager.setSnackbarListener(new ToolManager.SnackbarListener() {
            @Override
            public void onShowSnackbar(@NonNull CharSequence text, int duration, @Nullable CharSequence actionText, View.OnClickListener action) {
                if (mSnackbarListener != null) {
                    mSnackbarListener.onShowSnackbar(text, duration, actionText, action);
                }
            }
        });
    }

    protected void loadOverlayView() {
        super.loadOverlayView();
        if (mOverlayStub == null) {
            return;
        }

        mDocumentSlider = mOverlayStub.findViewById(R.id.thumbseekbar);
        mDocumentSlider.setPdfViewCtrl(mPdfViewCtrl);
        mDocumentSlider.setOnDocumentSliderTrackingListener(this);
        mDocumentSliderVertical = mOverlayStub.findViewById(R.id.thumbseekbar_vert);
        mDocumentSliderVertical.setPdfViewCtrl(mPdfViewCtrl);
        mDocumentSliderVertical.setOnDocumentSliderTrackingListener(this);
    }

    protected void stopHandlers() {
        super.stopHandlers();
    }

    /**
     * Called when the {@link androidx.appcompat.widget.Toolbar toolbar} of the containing
     * {@link PdfViewCtrlTabHostFragment2 host fragment} will be hidden.
     *
     * @return {@code true} if the toolbar can be hidden, {@code false} otherwise.
     */
    public boolean onHideToolbars() {
        return getActiveSeekBar() != null && !getActiveSeekBar().isProgressChanging();
    }

    /**
     * Sets the visibility of thumbnail slider.
     *
     * @param visible            True if the thumbnail slider should be visible
     * @param animateThumbSlider True if the visibility should be changed with animation
     */
    public void setThumbSliderVisible(boolean visible, boolean animateThumbSlider) {
        setThumbSliderVisible(visible, animateThumbSlider, true);
    }

    /**
     * Sets the visibility of thumbnail slider.
     *
     * @param visible                 True if the thumbnail slider should be visible
     * @param animateThumbSlider      True if the visibility should be changed with animation
     * @param showPageNumberIndicator True if the page number indicator will show together with the slider
     */
    public void setThumbSliderVisible(boolean visible, boolean animateThumbSlider, boolean showPageNumberIndicator) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (getActiveSeekBar() == null) {
            return;
        }

        Fragment parentFragment = getParentFragment();
        boolean presetVisible = false;
        if (parentFragment != null) {
            PresetBarViewModel presetBarViewModel = ViewModelProviders.of(parentFragment).get(PresetBarViewModel.class);
            presetVisible = presetBarViewModel.getPresetBarState() != null && presetBarViewModel.getPresetBarState().isVisible;
        }

        if (visible) {
            // should not do visible check as we could be in the middle of animation
            if (!presetVisible) {
                if (mViewerConfig == null || mViewerConfig.isShowDocumentSlider()) {
                    getActiveSeekBar().show(animateThumbSlider);
                }

                // show page back and forward buttons if their stacks are not empty
                if (mPageBackButton != null && !mPageBackStack.isEmpty()) {
                    showPageBackButton();
                }
                if (mPageForwardButton != null && !mPageForwardStack.isEmpty()) {
                    showPageForwardButton();
                }
                if (showPageNumberIndicator) {
                    if (mPageNumberIndicator != null) {
                        boolean canShow = shouldShowPageIndicator();
                        animatePageIndicator(canShow);
                    }
                }
            }
        } else {
            // should not do visible check as we could be in the middle of animation
            getActiveSeekBar().dismiss(animateThumbSlider);
            hidePageNumberIndicator();
        }
    }

    public DocumentSlider getActiveSeekBar() {
        if (!isSinglePageMode()) {
            return mDocumentSliderVertical;
        } else {
            return mDocumentSlider;
        }
    }

    public boolean isThumbSliderVisible() {
        return getActiveSeekBar() != null && getActiveSeekBar().getVisibility() == View.VISIBLE;
    }

    @Override
    public boolean handleKeyUp(int keyCode, KeyEvent event) {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            if (ShortcutHelper.canHandleShortcut(mToolManager, keyCode, event)) {
                ViewerShortcutViewModel shortcutViewModel = ViewModelProviders.of(activity)
                        .get(ViewerShortcutViewModel.class);
                shortcutViewModel.setKeyboardEvent(keyCode, event);
                return true;
            }
        }
        return super.handleKeyUp(keyCode, event);
    }

    @Override
    public void setReflowMode(boolean isReflowMode) {
        super.setReflowMode(isReflowMode);
        if (getActiveSeekBar() != null) {
            getActiveSeekBar().setReflowMode(isReflowMode);
        }
    }
}
