//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import androidx.annotation.NonNull;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.ShortcutHelper;

import java.util.ArrayList;

/**
 * @deprecated use {@link PdfViewCtrlTabFragment2} instead
 *
 * <p>
 * The PdfViewCtrlTabFragment shows {@link com.pdftron.pdf.PDFViewCtrl} out of the box with a various
 * of controls such as {@link com.pdftron.pdf.controls.AnnotationToolbar}, {@link com.pdftron.pdf.controls.ThumbnailSlider},
 * {@link com.pdftron.pdf.controls.ThumbnailsViewFragment} etc.
 */
@Deprecated
public class PdfViewCtrlTabFragment extends PdfViewCtrlTabBaseFragment implements
        ThumbnailSlider.OnThumbnailSliderTrackingListener,
        UndoRedoPopupWindow.OnUndoRedoListener {

    private static final String TAG = PdfViewCtrlTabFragment.class.getName();

    // UI elements
    protected ThumbnailSlider mBottomNavBar;
    protected AnnotationToolbar mAnnotationToolbar;

    // Listeners
    protected ArrayList<AnnotationToolbar.AnnotationToolbarListener> mAnnotationToolbarListeners;

    /**
     * Callback interface to be invoked when an interaction is needed.
     */
    public interface TabListener extends PdfViewCtrlTabBaseFragment.TabListener {

    }

    /**
     * Add {@link AnnotationToolbar.AnnotationToolbarListener} listener.
     *
     * @param listener The listener
     */
    public void addAnnotationToolbarListener(AnnotationToolbar.AnnotationToolbarListener listener) {
        if (mAnnotationToolbarListeners == null) {
            mAnnotationToolbarListeners = new ArrayList<>();
        }
        if (!mAnnotationToolbarListeners.contains(listener)) {
            mAnnotationToolbarListeners.add(listener);
        }
    }

    public void removeAnnotationToolbarListener(AnnotationToolbar.AnnotationToolbarListener listener) {
        if (mAnnotationToolbarListeners != null) {
            mAnnotationToolbarListeners.remove(listener);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (isAnnotationMode()) {
            mAnnotationToolbar.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // show annotation toolbar after recreation if it is currently visible and don't need Annot
        boolean showAnnotationToolbar = mAnnotationToolbarMode == AnnotationToolbar.START_MODE_NORMAL_TOOLBAR
                && isAnnotationMode();
        outState.putBoolean(BUNDLE_ANNOTATION_TOOLBAR_SHOW, showAnnotationToolbar);
        if (showAnnotationToolbar) {
            ToolManager.ToolModeBase toolModeBase = mToolManager.getTool().getToolMode();
            outState.putString(BUNDLE_ANNOTATION_TOOLBAR_TOOL_MODE, toolModeBase.toString());
        }
    }

    @Override
    protected int getContentLayoutRes() {
        return R.layout.controls_fragment_tabbed_pdfviewctrl_tab_content;
    }

    @Override
    protected void sliderRefreshPageCount() {
        mBottomNavBar.refreshPageCount();
    }

    @Override
    protected void sliderUpdateProgress(int curPage) {
        if (mBottomNavBar != null) {
            mBottomNavBar.setProgress(curPage);
        }
    }

    @Override
    protected void sliderSetReversed(boolean reversed) {
        if (mBottomNavBar != null) {
            mBottomNavBar.setReversed(reversed);
        }
    }

    @Override
    protected void sliderSetVisibility(int visibility) {
        if (mBottomNavBar != null) {
            mBottomNavBar.setVisibility(visibility);
        }
    }

    @Override
    protected void preparingNavChange() {
        // in old UI no op
    }

    @Override
    protected void postNavChange() {
        // in old UI no op
    }

    @Override
    protected View[] getGenericMotionEnabledViews() {
        return new View[]{mBottomNavBar, mPageNumberIndicatorAll, mPageBackButton, mPageForwardButton};
    }

    @Override
    public boolean handleKeyShortcut(int keyCode, KeyEvent event) {
        if (ShortcutHelper.isUndo(keyCode, event)) {
            if (mAnnotationToolbar != null) {
                mAnnotationToolbar.closePopups();
            }
            if (mTabListener != null) {
                mTabListener.onUndoRedoPopupClosed();
            }
        }

        if (ShortcutHelper.isRedo(keyCode, event)) {
            if (mAnnotationToolbar != null) {
                mAnnotationToolbar.closePopups();
            }
            if (mTabListener != null) {
                mTabListener.onUndoRedoPopupClosed();
            }
        }
        return super.handleKeyShortcut(keyCode, event);
    }

    @Override
    public boolean handleKeyUp(int keyCode, KeyEvent event) {
        if (mAnnotationToolbar == null) {
            createAnnotationToolbar();
        }

        if (mAnnotationToolbar.handleKeyUp(keyCode, event)) {
            return true;
        }
        return super.handleKeyUp(keyCode, event);
    }

    /**
     * Event called when the tool changes.
     *
     * @param newTool the new tool
     * @param oldTool the old tool
     */
    @Override
    public void toolChanged(ToolManager.Tool newTool, ToolManager.Tool oldTool) {
        super.toolChanged(newTool, oldTool);
        if (newTool != null && newTool.getToolMode().equals(ToolMode.FORM_FILL)) {
            if (mTabListener != null) {
                mTabListener.setToolbarsVisible(false);
            }
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

        super.onPageChange(old_page, cur_page, state);

        if (PdfViewCtrlSettingsManager.getPageNumberOverlayOption(activity)) {
            resetHidePageNumberIndicatorTimer();
        }
    }

    /**
     * Handles terminating document load.
     */
    @Override
    public void onDocumentLoaded() {
        if (getActivity() == null || mPdfViewCtrl == null) {
            return;
        }

        // Since we subscribe to DocumentLoaded, this needs to be done if ThumbSlider does not get the event
        if (mBottomNavBar != null) {
            // We pass a reference of the PDFViewCtrl to the slider so it can
            // interact with it (know number of pages, change pages, get thumbnails...
            // At this point no doc is set and the slider has no enough data
            // to initialize itself. When a doc is set we need to reset its data.
            mBottomNavBar.setPdfViewCtrl(mPdfViewCtrl);

            mBottomNavBar.setThumbSliderListener(this);
            mBottomNavBar.handleDocumentLoaded();
        }

        super.onDocumentLoaded();

        resetHidePageNumberIndicatorTimer();
    }

    /**
     * The overloaded implementation of {@link ThumbnailSlider.OnThumbnailSliderTrackingListener#onThumbSliderStartTrackingTouch()}.
     **/
    @Override
    public void onThumbSliderStartTrackingTouch() {
        animatePageIndicator(false);
        hideBackAndForwardButtons();

        updateCurrentPageInfo();
    }

    /**
     * The overloaded implementation of {@link ThumbnailSlider.OnThumbnailSliderTrackingListener#onThumbSliderStopTrackingTouch(int)}.
     **/
    @Override
    public void onThumbSliderStopTrackingTouch(int pageNum) {
        if (mTabListener != null) {
            mTabListener.onTabThumbSliderStopTrackingTouch();
        }

        resetHidePageNumberIndicatorTimer();
        setCurrentPageHelper(pageNum, false);
    }

    /**
     * Dismisses the user crop dialog.
     */
    public void userCropDialogDismiss() {
        resetHidePageNumberIndicatorTimer();

        super.userCropDialogDismiss();
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
     * Updates the page indicator
     */
    protected void updatePageIndicator() {
        super.updatePageIndicator();
        Activity activity = getActivity();
        if (activity != null && PdfViewCtrlSettingsManager.getPageNumberOverlayOption(activity)) {
            resetHidePageNumberIndicatorTimer();
        }
    }

    /**
     * Returns {@link AnnotationToolbar} associated with this tab.
     *
     * @return The AnnotationToolbar
     */
    public AnnotationToolbar getAnnotationToolbar() {
        return mAnnotationToolbar;
    }

    /**
     * Checks if the tab is in annotation mode.
     *
     * @return True if the tab is in annotation mode.
     */
    @Override
    public boolean isAnnotationMode() {
        return mAnnotationToolbar != null && mAnnotationToolbar.getVisibility() == View.VISIBLE;
    }

    /**
     * Shows the annotation toolbar.
     *
     * @param mode            The mode that annotation toolbar should start with. Possible values are
     *                        {@link AnnotationToolbar#START_MODE_NORMAL_TOOLBAR},
     *                        {@link AnnotationToolbar#START_MODE_EDIT_TOOLBAR},
     *                        {@link AnnotationToolbar#START_MODE_FORM_TOOLBAR}
     * @param inkAnnot        The ink annotation
     * @param pageNum         The page number
     * @param toolMode        The tool mode annotation toolbar should start with
     * @param dismissAfterUse Whether should dismiss after use
     */
    public void showAnnotationToolbar(int mode, Annot inkAnnot, int pageNum, ToolMode toolMode, boolean dismissAfterUse) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        mToolManager.deselectAll();
        createAnnotationToolbar();
        mAnnotationToolbar.show(mode, inkAnnot, pageNum, toolMode, dismissAfterUse);
        mAnnotationToolbarMode = mode;
    }

    public void createAnnotationToolbar() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (mAnnotationToolbar == null) {
            // annotation toolbar
            mAnnotationToolbar = mRootView.findViewById(R.id.annotation_toolbar);
            mAnnotationToolbar.setup(mToolManager, this);
            mAnnotationToolbar.setButtonStayDown(PdfViewCtrlSettingsManager.getContinuousAnnotationEdit(activity));
            mAnnotationToolbar.setAnnotationToolbarListener(new AnnotationToolbar.AnnotationToolbarListener() {
                @Override
                public void onAnnotationToolbarClosed() {
                    if (mAnnotationToolbarListeners != null) {
                        for (AnnotationToolbar.AnnotationToolbarListener listener : mAnnotationToolbarListeners) {
                            listener.onAnnotationToolbarClosed();
                        }
                    }

                    setVisibilityOfImaginedToolbar(false);
                }

                @Override
                public void onAnnotationToolbarShown() {
                    if (mAnnotationToolbarListeners != null) {
                        for (AnnotationToolbar.AnnotationToolbarListener listener : mAnnotationToolbarListeners) {
                            listener.onAnnotationToolbarShown();
                        }
                    }

                    setVisibilityOfImaginedToolbar(true);
                }

                @Override
                public void onShowAnnotationToolbarByShortcut(final int mode) {
                    if (mAnnotationToolbarListeners != null) {
                        for (AnnotationToolbar.AnnotationToolbarListener listener : mAnnotationToolbarListeners) {
                            listener.onShowAnnotationToolbarByShortcut(mode);
                        }
                    }
                }
            });
        }
    }

    // TODO: Tackle this beast...
    public void setVisibilityOfImaginedToolbar(boolean visible) {
        if (mPdfViewCtrl == null) {
            return;
        }

        View toolbar = mAnnotationToolbar;
        if (toolbar == null) {
            return;
        }

        // calculate new scroll position and how much we need to translate the PDFViewCtrl to make
        // the content appear at the same place
        int translateOffset;
        int canvasHeight = mPdfViewCtrl.getViewCanvasHeight();
        int viewHeight = mPdfViewCtrl.getHeight();
        int scrollY = mPdfViewCtrl.getScrollY();
        mPdfViewCtrl.setPageViewMode(PDFViewCtrl.PageViewMode.ZOOM); // so it doesn't re-fit
        int toolbarHeight = toolbar.getHeight();
        if (visible) {
            int newViewHeight = viewHeight - toolbarHeight;

            // need to know how tall the content after resizing
            int[] offsets = new int[2];
            if (canvasHeight > viewHeight) {
                offsets[1] = canvasHeight;
            } else {
                mPdfViewCtrl.getContentSize(offsets);
            }
            int newScrollableHeight = Math.max(offsets[1] - newViewHeight, 0);

            int newScrollY = Math.min(newScrollableHeight, scrollY + toolbarHeight);
            translateOffset = (toolbarHeight - newScrollY + scrollY) / 2;
            mPdfViewCtrl.setNextOnLayoutAdjustments(0, newScrollY - scrollY, true);
            if (translateOffset > 0) {
                mPdfViewCtrl.setTranslationY(-translateOffset);
                ViewPropertyAnimator ani = mPdfViewCtrl.animate();
                ani.translationY(0);
                ani.setDuration(300);
                ani.start();
            }
        } else {
            int newViewHeight = viewHeight + toolbarHeight;
            int newGraySpace = Math.max(newViewHeight - canvasHeight, 0);

            // if we are at the bottom of page and re-size, the PDFViewCtrl will do that
            // automatically since it's at the bottom
            int scrollYHandledByPdfViewCtrl = 0;
            if (canvasHeight > viewHeight) {
                int distanceFromBottom = canvasHeight - (viewHeight + scrollY);
                scrollYHandledByPdfViewCtrl = Math.max(0, toolbarHeight - distanceFromBottom);
            }

            int newScrollY = Math.max(scrollY - toolbarHeight, 0);

            int graySpaceOffset = newGraySpace / 2;
            translateOffset = (toolbarHeight - scrollY + newScrollY) - graySpaceOffset;
            mPdfViewCtrl.setNextOnLayoutAdjustments(0, newScrollY - scrollY + scrollYHandledByPdfViewCtrl, true);
            if (translateOffset > 0) {
                mPdfViewCtrl.setTranslationY(translateOffset);
                ViewPropertyAnimator ani = mPdfViewCtrl.animate();
                ani.translationY(0);
                ani.setDuration(300);
                ani.start();
            }
        }
    }

    /**
     * Hides the annotation toolbar.
     */
    public void hideAnnotationToolbar() {
        if (mAnnotationToolbar != null) {
            mAnnotationToolbar.close();
        }
    }

    /**
     * Handles when undo/redo operation is done.
     */
    @Override
    public void onUndoRedoCalled() {
        refreshPageCount();
    }

    protected void loadOverlayView() {
        super.loadOverlayView();
        if (mOverlayStub == null) {
            return;
        }

        mBottomNavBar = mOverlayStub.findViewById(R.id.thumbseekbar);
        mBottomNavBar.setOnMenuItemClickedListener(new ThumbnailSlider.OnMenuItemClickedListener() {
            @Override
            public void onMenuItemClicked(int menuItemPosition) {
                if (menuItemPosition == ThumbnailSlider.POSITION_LEFT) {
                    if (mTabListener != null) {
                        mTabListener.onPageThumbnailOptionSelected(false, null);
                    }
                } else {
                    if (mTabListener != null) {
                        mTabListener.onOutlineOptionSelected();
                    }
                }
            }
        });

        mPageNumberIndicator.setVisibility(View.VISIBLE);
        animatePageIndicator(false);
    }

    /**
     * Checks whether right-to-left mode is enabled
     *
     * @return True if right-to-left mode is enabled.
     */
    public boolean isRtlMode() {
        return mIsRtlMode;
    }

    protected void doDocumentLoaded() {
        super.doDocumentLoaded();

        if (mBottomNavBar != null && mViewerConfig != null) {
            boolean canShowBookmark = mViewerConfig.isShowBookmarksView() &&
                    (mViewerConfig.isShowAnnotationsList() ||
                            mViewerConfig.isShowOutlineList() ||
                            mViewerConfig.isShowUserBookmarksList());
            if (!canShowBookmark) {
                mBottomNavBar.setMenuItemVisibility(ThumbnailSlider.POSITION_RIGHT, View.GONE);
            }
            if (!mViewerConfig.isShowThumbnailView()) {
                mBottomNavBar.setMenuItemVisibility(ThumbnailSlider.POSITION_LEFT, View.GONE);
            }
        }
    }

    /**
     * Handles when thumbnails view dialog is dismissed.
     *
     * @param pageNum          The selected page number
     * @param docPagesModified True if the document pages are modified.
     */
    public void onThumbnailsViewDialogDismiss(int pageNum, boolean docPagesModified) {
        resetHidePageNumberIndicatorTimer();
        super.onThumbnailsViewDialogDismiss(pageNum, docPagesModified);
    }

    /**
     * Called when the {@link androidx.appcompat.widget.Toolbar toolbar} of the containing
     * {@link PdfViewCtrlTabHostFragment host fragment} will be hidden.
     *
     * @return {@code true} if the toolbar can be hidden, {@code false} otherwise.
     */
    public boolean onHideToolbars() {
        return mBottomNavBar != null && !mBottomNavBar.isProgressChanging();
    }

    public void setViewerTopMargin(int height) {
        if (mPdfViewCtrl == null || !(mPdfViewCtrl.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mPdfViewCtrl.getLayoutParams();
        params.topMargin = height;
        mPdfViewCtrl.setLayoutParams(params);
        mPdfViewCtrl.requestLayout();
    }

    /**
     * Sets the visibility of thumbnail slider.
     *
     * @param visible            True if the thumbnail slider should be visible
     * @param animateThumbSlider True if the visibility should be changed with animation
     */
    public void setThumbSliderVisible(boolean visible, boolean animateThumbSlider) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (mBottomNavBar == null) {
            return;
        }

        boolean isVisible = mBottomNavBar.getVisibility() == View.VISIBLE;

        if (visible) {
            if (!isVisible) {
                if (mViewerConfig == null || mViewerConfig.isShowBottomNavBar()) {
                    mBottomNavBar.show(animateThumbSlider);
                }

                // show page back and forward buttons if their stacks are not empty
                if (mPageBackButton != null && !mPageBackStack.isEmpty()) {
                    showPageBackButton();
                }
                if (mPageForwardButton != null && !mPageForwardStack.isEmpty()) {
                    showPageForwardButton();
                }
            }
        } else {
            if (isVisible) {
                mBottomNavBar.dismiss(animateThumbSlider);
            }
        }
    }

    public boolean isThumbSliderVisible() {
        return mBottomNavBar != null && mBottomNavBar.getVisibility() == View.VISIBLE;
    }
}
