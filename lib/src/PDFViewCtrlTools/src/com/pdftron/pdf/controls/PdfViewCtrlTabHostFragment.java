//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.transition.Slide;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.TextSearchResult;
import com.pdftron.pdf.dialog.BookmarksDialogFragment;
import com.pdftron.pdf.dialog.menueditor.MenuEditorDialogFragment;
import com.pdftron.pdf.dialog.menueditor.MenuEditorEvent;
import com.pdftron.pdf.dialog.menueditor.MenuEditorViewModel;
import com.pdftron.pdf.dialog.menueditor.model.MenuEditorItem;
import com.pdftron.pdf.dialog.menueditor.model.MenuEditorItemContent;
import com.pdftron.pdf.dialog.menueditor.model.MenuEditorItemHeader;
import com.pdftron.pdf.dialog.redaction.SearchRedactionDialogFragment;
import com.pdftron.pdf.tools.QuickMenuItem;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.tools.UndoRedoManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.pdf.widget.AppBarLayout;

import java.util.ArrayList;

import io.reactivex.functions.Consumer;

/**
 * @deprecated use {@link PdfViewCtrlTabHostFragment2} instead
 * <p>
 * The PdfViewCtrlTabHostFragment shows multiple {@link PdfViewCtrlTabFragment}
 * in tab layout.
 */
@Deprecated
public class PdfViewCtrlTabHostFragment extends PdfViewCtrlTabHostBaseFragment implements
        PdfViewCtrlTabFragment.TabListener,
        AnnotationToolbar.AnnotationToolbarListener {

    private static final String TAG = PdfViewCtrlTabHostFragment.class.getName();

    private UndoRedoPopupWindow mUndoRedoPopupWindow;

    // menus
    protected MenuItem mMenuAnnotToolbar;
    protected MenuItem mMenuFormToolbar;
    protected MenuItem mMenuFillAndSignToolbar;
    protected MenuItem mMenuUndo;
    protected MenuItem mMenuRedo;

    /**
     * Callback interface to be invoked when AppBar visibility changes.
     */
    public interface AppBarVisibilityListener extends PdfViewCtrlTabHostBaseFragment.AppBarVisibilityListener {
    }

    public interface ReflowControlListener extends PdfViewCtrlTabHostBaseFragment.ReflowControlListener {
    }

    /**
     * Callback interface to be invoked when an interaction is needed.
     */
    public interface TabHostListener extends PdfViewCtrlTabHostBaseFragment.TabHostListener {
    }

    @Override
    protected void handleAutoHideUi() {
        hideUI();
    }

    @Override
    protected void handleAutoHideNavBar() {
        final PdfViewCtrlTabFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment != null && currentFragment.isAnnotationMode()) {
            showSystemStatusBar();
        }
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.controls_fragment_tabbed_pdfviewctrl;
    }

    @Override
    protected int getContainerId() {
        return mViewerConfig != null && !mViewerConfig.isAutoHideToolbarEnabled() ?
                R.id.adjust_fragment_container : R.id.realtabcontent;
    }

    @Override
    protected int getTabLayoutRes() {
        return R.layout.controls_fragment_tabbed_pdfviewctrl_tab;
    }

    @Override
    protected void updateUndoRedoState() {
        // no op
    }

    /**
     * Returns a new instance of the class
     */
    public static PdfViewCtrlTabHostFragment newInstance(Bundle args) {
        // args has information about the new tab
        // the information about other tabs (already added) is accessible from PdfViewCtrlTabsManager
        PdfViewCtrlTabHostFragment fragment = new PdfViewCtrlTabHostFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        restoreSavedMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (super.onOptionsItemSelected(item)) {
            return true;
        }

        FragmentActivity activity = getActivity();
        PdfViewCtrlTabFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return false;
        }

        final int id = item.getItemId();

        if (id == R.id.action_annotation_toolbar) {
            if (currentFragment.isReflowMode()) {
                CommonToast.showText(activity, R.string.reflow_disable_markup_clicked);
            } else if (currentFragment.isDocumentReady()) {
                showAnnotationToolbar(AnnotationToolbar.START_MODE_NORMAL_TOOLBAR, null);
            }
        } else if (id == R.id.action_form_toolbar) {
            if (currentFragment.isReflowMode()) {
                CommonToast.showText(activity, R.string.reflow_disable_markup_clicked);
            } else if (currentFragment.isDocumentReady()) {
                showAnnotationToolbar(AnnotationToolbar.START_MODE_FORM_TOOLBAR, null);
            }
        } else if (id == R.id.action_fill_and_sign_toolbar) {
            if (currentFragment.isReflowMode()) {
                CommonToast.showText(activity, R.string.reflow_disable_markup_clicked);
            } else if (currentFragment.isDocumentReady()) {
                showAnnotationToolbar(AnnotationToolbar.START_MODE_FILL_AND_SIGN_TOOLBAR, null);
            }
        } else {
            return false;
        }

        return true;
    }

    /**
     * The overload implementation of {@link SearchResultsView.SearchResultsListener#onSearchResultClicked(TextSearchResult)}.
     */
    @Override
    public void onSearchResultClicked(TextSearchResult result) {
        super.onSearchResultClicked(result);

        final PdfViewCtrlTabFragment currentFragment = getCurrentPdfViewCtrlFragment();
        Activity activity = getActivity();
        if (activity == null || currentFragment == null) {
            return;
        }

        PDFViewCtrl pdfViewCtrl = currentFragment.getPDFViewCtrl();
        if (pdfViewCtrl != null && pdfViewCtrl.getCurrentPage() != result.getPageNum()) {
            currentFragment.resetHidePageNumberIndicatorTimer();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mUndoRedoPopupWindow != null && mUndoRedoPopupWindow.isShowing()) {
            mUndoRedoPopupWindow.dismiss();
        }
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabFragment.TabListener#onTabDocumentLoaded(String)}.
     */
    @Override
    public void onTabDocumentLoaded(String tag) {
        // update undo button visibility
        updateUndoButtonVisibility(true);

        super.onTabDocumentLoaded(tag);
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabFragment.TabListener#onInkEditSelected(Annot, int)}.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onInkEditSelected(Annot inkAnnot, int pageNum) {
        showAnnotationToolbar(AnnotationToolbar.START_MODE_EDIT_TOOLBAR, inkAnnot, pageNum, ToolMode.INK_CREATE);
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabFragment.TabListener#onOpenAnnotationToolbar(ToolMode)}.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onOpenAnnotationToolbar(ToolMode mode) {
        showAnnotationToolbar(AnnotationToolbar.START_MODE_NORMAL_TOOLBAR, mode);
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabFragment.TabListener#onOpenEditToolbar(ToolMode)}.
     */
    @Override
    public void onOpenEditToolbar(ToolMode mode) {
        showAnnotationToolbar(AnnotationToolbar.START_MODE_EDIT_TOOLBAR, mode);
    }

    @Nullable
    private ArrayList<MenuEditorItem> getToolbarMenuItems() {
        if (null == mToolbar) {
            return null;
        }
        int size = mToolbar.getMenu().size();
        SparseArray<ArrayList<MenuEditorItem>> menuItemMap = new SparseArray<>(size);
        menuItemMap.put(0, new ArrayList<MenuEditorItem>());
        menuItemMap.put(1, new ArrayList<MenuEditorItem>());
        for (int i = 0; i < size; i++) {
            MenuItem menuItem = mToolbar.getMenu().getItem(i);
            if (menuItem.isVisible() && menuItem.getIcon() != null && menuItem.getIcon().getConstantState() != null) {

                if (menuItem instanceof MenuItemImpl) {
                    MenuEditorItemContent itemContent = new MenuEditorItemContent(
                            menuItem.getItemId(),
                            menuItem.getTitle().toString(),
                            DrawableCompat.wrap(menuItem.getIcon().getConstantState().newDrawable()).mutate());

                    MenuItemImpl menuItemImpl = (MenuItemImpl) menuItem;
                    if (menuItemImpl.isActionButton()) {
                        menuItemMap.get(0).add(itemContent);
                    } else {
                        menuItemMap.get(1).add(itemContent);
                    }
                }
            }
        }
        ArrayList<MenuEditorItem> items = new ArrayList<>();
        MenuEditorItemHeader header1 = new MenuEditorItemHeader(
                MenuEditorItem.GROUP_SHOW_IF_ROOM,
                R.string.menu_editor_if_room_section_header,
                R.string.menu_editor_section_desc);
        header1.setDraggingTitleId(R.string.menu_editor_dragging_if_room_section_header);
        items.add(header1);
        items.addAll(menuItemMap.get(0));
        MenuEditorItemHeader header2 = new MenuEditorItemHeader(
                MenuEditorItem.GROUP_SHOW_NEVER,
                R.string.menu_editor_never_section_header,
                0);
        header2.setDraggingTitleId(R.string.menu_editor_dragging_never_section_header);
        items.add(header2);
        items.addAll(menuItemMap.get(1));

        return items;
    }

    private void updateToolbarMenuOrder(ArrayList<MenuEditorItem> menuEditorItems) {
        int group = -1;
        for (int index = 0; index < menuEditorItems.size(); index++) {
            MenuEditorItem newItem = menuEditorItems.get(index);
            if (newItem.isHeader()) {
                MenuEditorItemHeader header = (MenuEditorItemHeader) newItem;
                group = header.getGroup();
            } else {
                MenuEditorItemContent itemContent = (MenuEditorItemContent) newItem;
                MenuItem oldMenu = mToolbar.getMenu().findItem(itemContent.getId());
                if (oldMenu != null && oldMenu.getOrder() != index) {
                    mToolbar.getMenu().removeItem(oldMenu.getItemId());
                    MenuItem newMenu = mToolbar.getMenu().add(oldMenu.getGroupId(),
                            oldMenu.getItemId(), index, oldMenu.getTitle());
                    newMenu.setIcon(oldMenu.getIcon());
                    newMenu.setCheckable(oldMenu.isCheckable());
                    newMenu.setChecked(oldMenu.isChecked());
                    newMenu.setEnabled(oldMenu.isEnabled());
                    newMenu.setVisible(oldMenu.isVisible());
                    if (group == MenuEditorItemContent.GROUP_SHOW_IF_ROOM) {
                        if (index <= MAX_TOOLBAR_VISIBLE_ICON_COUNT) {
                            newMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                        } else {
                            newMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                        }
                    } else {
                        newMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                    }
                }
            }
        }
        initOptionsMenu(mToolbar.getMenu());
    }

    @Override
    public void onEditToolbarMenu() {
        ArrayList<MenuEditorItem> items = getToolbarMenuItems();
        if (null == items) {
            return;
        }

        stopHideToolbarsTimer();

        final MenuEditorViewModel viewModel = ViewModelProviders.of(this).get(MenuEditorViewModel.class);
        viewModel.setItems(items);
        viewModel.getItemsLiveData().observe(getViewLifecycleOwner(), new Observer<ArrayList<MenuEditorItem>>() {
            @Override
            public void onChanged(ArrayList<MenuEditorItem> menuEditorItems) {
                updateToolbarMenuOrder(menuEditorItems);
            }
        });

        mDisposables.add(viewModel.getObservable()
                .subscribe(new Consumer<MenuEditorEvent>() {
                    @Override
                    public void accept(MenuEditorEvent menuEditorEvent) throws Exception {
                        if (menuEditorEvent.getEventType() == MenuEditorEvent.Type.RESET) {
                            FragmentActivity activity = getActivity();
                            if (activity == null || mToolbar == null) {
                                return;
                            }
                            mToolbar.getMenu().clear();
                            for (int res : mToolbarMenuResArray) {
                                mToolbar.inflateMenu(res);
                            }
                            initOptionsMenu(mToolbar.getMenu());
                            setOptionsMenuVisible(true);
                            onPrepareOptionsMenu(mToolbar.getMenu());
                            viewModel.setItems(getToolbarMenuItems());
                        }
                    }
                })
        );

        MenuEditorDialogFragment editorFragment = MenuEditorDialogFragment.newInstance();
        editorFragment.setStyle(DialogFragment.STYLE_NORMAL, mThemeProvider.getTheme());
        editorFragment.show(getChildFragmentManager(), MenuEditorDialogFragment.TAG);

        editorFragment.setMenuEditorDialogFragmentListener(new MenuEditorDialogFragment.MenuEditorDialogFragmentListener() {
            @Override
            public void onMenuEditorDialogDismiss() {
                resetHideToolbarsTimer();

                // save icons
                ArrayList<MenuEditorItem> newMenuItems = viewModel.getItemsLiveData().getValue();
                if (newMenuItems != null) {
                    try {
                        Activity activity = getActivity();
                        if (activity != null) {
                            String menuJson = ViewerUtils.getMenuEditorItemsJSON(newMenuItems);
                            PdfViewCtrlSettingsManager.setSavedHomeToolbarMenu(activity, menuJson);
                        }
                    } catch (Exception ex) {
                        AnalyticsHandlerAdapter.getInstance().sendException(ex);
                    }
                }
            }
        });
    }

    @Override
    protected int getDefaultTheme() {
        return R.style.CustomAppTheme;
    }

    @Override
    int[] getDefaultToolbarMenu() {
        return new int[]{R.menu.fragment_viewer};
    }

    public void restoreSavedMenu() {
        Activity activity = getActivity();
        if (activity != null) {
            String savedToolbarMenu = PdfViewCtrlSettingsManager.getSavedHomeToolbarMenu(activity);
            if (savedToolbarMenu != null) {
                try {
                    ArrayList<MenuEditorItem> menuEditorItems = ViewerUtils.getMenuEditorItemsArray(savedToolbarMenu);
                    updateToolbarMenuOrder(menuEditorItems);
                } catch (Exception ex) {
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                }
            }
        }
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabFragment.TabListener#onTabSingleTapConfirmed()}.
     */
    @Override
    public void onTabSingleTapConfirmed() {
        final PdfViewCtrlTabFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }

        if (!currentFragment.isAnnotationMode() && !mIsSearchMode) {
            if (currentFragment.isThumbSliderVisible()) {
                hideUI();
            } else {
                showUI();
            }
        }
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabFragment.TabListener#onUndoRedoPopupClosed()}.
     */
    @Override
    public void onUndoRedoPopupClosed() {
        if (mUndoRedoPopupWindow != null && mUndoRedoPopupWindow.isShowing()) {
            mUndoRedoPopupWindow.dismiss();
        }
    }

    /**
     * Undoes the last operation.
     */
    protected void undo() {
        super.undo();
        Activity activity = getActivity();
        final PdfViewCtrlTabFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return;
        }

        if (currentFragment.getToolManager() != null) {
            UndoRedoManager undoRedoManager = currentFragment.getToolManager().getUndoRedoManger();
            if (undoRedoManager != null) {
                setToolbarsVisible(false);

                try {
                    if (mUndoRedoPopupWindow != null && mUndoRedoPopupWindow.isShowing()) {
                        mUndoRedoPopupWindow.dismiss();
                    }
                    mUndoRedoPopupWindow = new UndoRedoPopupWindow(activity, undoRedoManager,
                            new UndoRedoPopupWindow.OnUndoRedoListener() {
                                @Override
                                public void onUndoRedoCalled() {
                                    currentFragment.refreshPageCount();
                                }
                            }, AnalyticsHandlerAdapter.LOCATION_VIEWER);
                    mUndoRedoPopupWindow.showAtLocation(currentFragment.getView(), Gravity.TOP | Gravity.END, 0, 0);
                } catch (Exception ex) {
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                }
            }
        }
    }

    /**
     * Redoes the last undo.
     */
    protected void redo() {
        super.redo();
        Activity activity = getActivity();
        final PdfViewCtrlTabFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return;
        }

        if (currentFragment.getToolManager() != null) {
            UndoRedoManager undoRedoManager = currentFragment.getToolManager().getUndoRedoManger();
            if (undoRedoManager != null) {
                setToolbarsVisible(false);

                try {
                    if (mUndoRedoPopupWindow != null && mUndoRedoPopupWindow.isShowing()) {
                        mUndoRedoPopupWindow.dismiss();
                    }
                    mUndoRedoPopupWindow = new UndoRedoPopupWindow(activity, undoRedoManager,
                            new UndoRedoPopupWindow.OnUndoRedoListener() {
                                @Override
                                public void onUndoRedoCalled() {
                                    currentFragment.refreshPageCount();
                                }
                            }, AnalyticsHandlerAdapter.LOCATION_VIEWER);
                    mUndoRedoPopupWindow.showAtLocation(currentFragment.getView(), Gravity.TOP | Gravity.END, 0, 0);
                } catch (Exception ex) {
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                }
            }
        }
    }

    /**
     * The overloaded implementation of {@link BookmarksDialogFragment.BookmarksDialogListener#onBookmarksDialogDismissed(int)}.
     **/
    @Override
    public void onBookmarksDialogDismissed(int tabIndex) {
        super.onBookmarksDialogDismissed(tabIndex);

        final PdfViewCtrlTabFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment != null) {
            currentFragment.resetHidePageNumberIndicatorTimer();
        }
    }

    @Override
    protected void openBookmarksDialog() {
        final PdfViewCtrlTabFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }
        if (canOpenNavigationListAsSideSheet()) {
            int topMargin = getToolbarHeight();
            if (mViewerConfig != null && !mViewerConfig.isAutoHideToolbarEnabled()) {
                topMargin = 0;
            }
            currentFragment.openNavigationList(mBookmarksDialog, topMargin, mSystemWindowInsetBottom);
            mBookmarksDialog = null;
        } else {
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null) {
                mBookmarksDialog.show(fragmentManager, "bookmarks_dialog");
            }
        }
    }

    @Override
    protected void openRedactionDialog(SearchRedactionDialogFragment dialogFragment) {
        final PdfViewCtrlTabFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }
        int topMargin = getToolbarHeight();
        if (mViewerConfig != null && !mViewerConfig.isAutoHideToolbarEnabled()) {
            topMargin = 0;
        }
        currentFragment.openRedactionSearchList(dialogFragment, topMargin, mSystemWindowInsetBottom);
    }

    @Override
    public void onAnnotationToolbarShown() {

    }

    /**
     * The overloaded implementation of {@link AnnotationToolbar.AnnotationToolbarListener#onAnnotationToolbarClosed()}.
     **/
    @Override
    public void onAnnotationToolbarClosed() {
        Activity activity = getActivity();
        final PdfViewCtrlTabFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return;
        }

        setToolbarsVisible(true);
        showSystemUI();

        if (currentFragment.isNavigationListShowing()) {
            // force apply inset
            View view = getView();
            if (view != null) {
                ViewCompat.requestApplyInsets(view);
            }
        }
    }

    /**
     * The overloaded implementation of {@link AnnotationToolbar.AnnotationToolbarListener#onShowAnnotationToolbarByShortcut(int)}.
     **/
    @Override
    public void onShowAnnotationToolbarByShortcut(int mode) {
        showAnnotationToolbar(mode, null);
    }

    public boolean showAnnotationToolbar(final int mode, final ToolMode toolMode) {
        return showAnnotationToolbar(mode, null, 0, toolMode);
    }

    /**
     * Shows an annotation toolbar starting with the certain mode and selected (ink) annotation
     *
     * @param mode     The mode that annotation toolbar should start with. Possible values are
     *                 {@link AnnotationToolbar#START_MODE_NORMAL_TOOLBAR},
     *                 {@link AnnotationToolbar#START_MODE_EDIT_TOOLBAR},
     *                 {@link AnnotationToolbar#START_MODE_FORM_TOOLBAR}
     * @param inkAnnot The selected (ink) annotation
     * @param pageNum  The page number
     * @param toolMode The startup tool mode
     * @return <code>true</code> if annotation toolbar is shown; <code>false</code> otherwise
     */
    protected boolean showAnnotationToolbar(final int mode, final Annot inkAnnot, final int pageNum, final ToolMode toolMode) {
        Activity activity = getActivity();
        final PdfViewCtrlTabFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return false;
        }

        currentFragment.localFileWriteAccessCheck();

        // If skip read only check, then converted files are allowed
        boolean skipReadOnlyCheck = mViewerConfig != null && mViewerConfig.skipReadOnlyCheck();
        if (checkTabConversionAndAlert(R.string.cant_edit_while_converting_message, skipReadOnlyCheck)) {
            return false;
        }

        mWillShowAnnotationToolbar = true;

        // should force top toolbars hide since they should be replaced with annotation toolbar even for large screen devices
        boolean autoHideEnabled = mAutoHideEnabled;
        mAutoHideEnabled = true;
        setToolbarsVisible(false);
        mAutoHideEnabled = autoHideEnabled;

        if (Utils.isLollipop()) {
            showSystemStatusBar();
        } else {
            showSystemUI();
        }
        if (currentFragment.isNavigationListShowing()) {
            // force apply inset
            View view = getView();
            if (view != null) {
                ViewCompat.requestApplyInsets(view);
            }
        }

        // Showing the annotation toolbar should be after hiding the other toolbars.
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Ensure that system UI is not hidden by the timer.
                stopHideToolbarsTimer();

                currentFragment.showAnnotationToolbar(mode, inkAnnot, pageNum, toolMode, !currentFragment.isAnnotationMode());
            }
        }, ANIMATE_DURATION_HIDE);
        return true;
    }

    /**
     * The overloaded implementation of {@link ToolManager.QuickMenuListener#onQuickMenuClicked(QuickMenuItem)}.
     */
    @Override
    public boolean onQuickMenuClicked(QuickMenuItem menuItem) {
        hideUI();
        return super.onQuickMenuClicked(menuItem);
    }

    /**
     * The overloaded implementation of {@link ToolManager.QuickMenuListener#onQuickMenuShown()}.
     **/
    @Override
    public void onQuickMenuShown() {
        hideUI();
    }

    /**
     * The overloaded implementation of {@link ToolManager.QuickMenuListener#onQuickMenuDismissed()}.
     **/
    @Override
    public void onQuickMenuDismissed() {
        hideUI();
    }

    protected void updateTabLayout() {
        Activity activity = getActivity();
        if (activity == null || mTabLayout == null) {
            return;
        }

        // push the PDFView down below the toolbar in large screens
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        boolean isLargeScreen = Utils.isLargeScreen(activity);
        boolean permanentToolbars = mViewerConfig != null && mViewerConfig.isPermanentToolbars();
        if (permanentToolbars && !isInFullScreenMode()) {
            mFragmentContainer.setFitsSystemWindows(false);
        }
        params.addRule(RelativeLayout.BELOW, (isLargeScreen || permanentToolbars) ? R.id.app_bar_layout : R.id.parent);
        mFragmentContainer.setLayoutParams(params);
        super.updateTabLayout();
    }

    /**
     * Updates the icons (enable/disable) when reflow mode has been changed.
     */
    @Override
    protected void updateIconsInReflowMode() {
        final PdfViewCtrlTabFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }
        MenuItem menuReflowMode = getToolbarOptionMenuItem(R.id.action_reflow_mode);
        MenuItem menuSearch = getToolbarOptionMenuItem(R.id.action_search);
        if (currentFragment.isReflowMode()) {
            if (menuReflowMode != null) {
                menuReflowMode.setChecked(true);
            }

            int alpha = getResources().getInteger(R.integer.reflow_disabled_button_alpha);

            if (mMenuAnnotToolbar != null) {
                if (mMenuAnnotToolbar.getIcon() != null) {
                    mMenuAnnotToolbar.getIcon().setAlpha(alpha);
                }
                mMenuAnnotToolbar.setEnabled(false);
            }
            if (menuSearch != null) {
                if (menuSearch.getIcon() != null) {
                    menuSearch.getIcon().setAlpha(alpha);
                }
                menuSearch.setEnabled(false);
            }
            if (mMenuFormToolbar != null) {
                if (mMenuFormToolbar.getIcon() != null) {
                    mMenuFormToolbar.getIcon().setAlpha(alpha);
                }
                mMenuFormToolbar.setEnabled(false);
            }
            if (mMenuFillAndSignToolbar != null) {
                if (mMenuFillAndSignToolbar.getIcon() != null) {
                    mMenuFillAndSignToolbar.getIcon().setAlpha(alpha);
                }
                mMenuFillAndSignToolbar.setEnabled(false);
            }
        } else {
            if (menuReflowMode != null) {
                menuReflowMode.setChecked(false);
            }
            if (mMenuAnnotToolbar != null) {
                if (mMenuAnnotToolbar.getIcon() != null) {
                    mMenuAnnotToolbar.getIcon().setAlpha(255);
                }
                mMenuAnnotToolbar.setEnabled(true);
            }
            if (menuSearch != null) {
                if (menuSearch.getIcon() != null) {
                    menuSearch.getIcon().setAlpha(255);
                }
                menuSearch.setEnabled(true);
            }
            if (mMenuFormToolbar != null) {
                if (mMenuFormToolbar.getIcon() != null) {
                    mMenuFormToolbar.getIcon().setAlpha(255);
                }
                mMenuFormToolbar.setEnabled(true);
            }
            if (mMenuFillAndSignToolbar != null) {
                if (mMenuFillAndSignToolbar.getIcon() != null) {
                    mMenuFillAndSignToolbar.getIcon().setAlpha(255);
                }
                mMenuFillAndSignToolbar.setEnabled(true);
            }
        }
    }

    private void updateButtonUndo(boolean hasUndo) {
        if (null == mMenuUndo) {
            return;
        }
        updateUndoButtonVisibility(true);
        if (hasUndo) {
            mMenuUndo.setEnabled(true);
            if (mMenuUndo.getIcon() != null) {
                mMenuUndo.getIcon().setAlpha(255);
            }
        } else {
            mMenuUndo.setEnabled(false);
            if (mMenuUndo.getIcon() != null) {
                int alpha = getResources().getInteger(R.integer.reflow_disabled_button_alpha);
                mMenuUndo.getIcon().setAlpha(alpha);
            }
        }
    }

    /**
     * Shows the UI.
     */
    // TODO: Rename.
    @Override
    public void showUI() {
        Activity activity = getActivity();
        final PdfViewCtrlTabFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return;
        }

        final boolean canShowToolbars = currentFragment.onShowToolbar();
        final boolean canExitFullscreenMode = currentFragment.onExitFullscreenMode();
        final boolean isThumbSliderVisible = currentFragment.isThumbSliderVisible();
        final boolean isAnnotationMode = currentFragment.isAnnotationMode();

        // Toolbars can only be shown if fullscreen mode will be exited.
        if (!isThumbSliderVisible && canShowToolbars && canExitFullscreenMode) {
            setToolbarsVisible(true);
        }

        if (!isAnnotationMode && canExitFullscreenMode) {
            showSystemUI();
        }

        if (!isInFullScreenMode() && currentFragment.isNavigationListShowing()) {
            // in non full screen mode, inset is not applied when toggle toolbar visibility
            View view = getView();
            if (view != null) {
                ViewCompat.requestApplyInsets(view);
            }
        }
    }

    /**
     * Hides the UI.
     */
    // TODO: Rename.
    @Override
    public void hideUI() {
        if (mViewerConfig != null && !mViewerConfig.isAutoHideToolbarEnabled()) {
            setThumbSliderVisibility(false, true);
        } else {
            Activity activity = getActivity();
            final PdfViewCtrlTabFragment currentFragment = getCurrentPdfViewCtrlFragment();
            if (activity == null || currentFragment == null) {
                return;
            }

            final boolean canHideToolbars = currentFragment.onHideToolbars();
            final boolean canEnterFullscreenMode = currentFragment.onEnterFullscreenMode();
            final boolean isThumbSliderVisible = currentFragment.isThumbSliderVisible();
            final boolean isAnnotationMode = currentFragment.isAnnotationMode();

            if (isThumbSliderVisible && canHideToolbars) {
                setToolbarsVisible(false);
            }

            // Fullscreen mode only be entered if the toolbars will hide or if they are not visible.
            if ((isThumbSliderVisible && canHideToolbars && canEnterFullscreenMode)
                    || (!isThumbSliderVisible && canEnterFullscreenMode)) {
                if (isAnnotationMode) {
                    showSystemStatusBar();
                } else {
                    hideSystemUI();
                }
            }

            if (!isInFullScreenMode() && currentFragment.isNavigationListShowing()) {
                // in non full screen mode, inset is not applied when toggle toolbar visibility
                View view = getView();
                if (view != null) {
                    ViewCompat.requestApplyInsets(view);
                }
            }
        }
    }

    /**
     * Handles changing the visibility of toolbars.
     *
     * @param visible            True if toolbar is visible
     * @param animateThumbSlider True if visibility should be changed with animation
     */
    @Override
    public void setToolbarsVisible(boolean visible, boolean animateThumbSlider) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        final PdfViewCtrlTabFragment currentFragment = getCurrentPdfViewCtrlFragment();
        boolean isAnnotationMode = currentFragment != null && currentFragment.isAnnotationMode();
        if (isAnnotationMode || mIsSearchMode) {
            // Do nothing if in annotation or search mode.
            return;
        }

        if (visible) {
            resetHideToolbarsTimer();
            if (currentFragment != null) {
                currentFragment.resetHidePageNumberIndicatorTimer();
            }
        } else {
            stopHideToolbarsTimer();
            if (currentFragment != null) {
                currentFragment.hidePageNumberIndicator();
            }
        }
        if (visible || mAutoHideEnabled) {
            animateToolbars(visible);
        }
        setThumbSliderVisibility(visible, animateThumbSlider);
    }

    /**
     * Shows the system status bar.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected void showSystemStatusBar() {
        Activity activity = getActivity();
        final PdfViewCtrlTabFragment currentFragment = getCurrentPdfViewCtrlFragment();
        View view = getView();
        if (activity == null || currentFragment == null || view == null) {
            return;
        }
        if (isInFullScreenMode()) {
            int oldFlags = view.getSystemUiVisibility();
            int newFlags = oldFlags;

            // Remove the system UI flag to hide the status bar.
            newFlags &= ~(View.SYSTEM_UI_FLAG_FULLSCREEN);

            // Add the system UI flags to hide the navigation bar.
            // (Sticky immersion means the navigation bar can be "peeked").
            newFlags |= (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

            if (newFlags != oldFlags) {
                view.setSystemUiVisibility(newFlags);
                view.requestLayout(); // Force a layout invalidation.
            }
        }
    }

    /**
     * Shows the system UI.
     */
    // This snippet shows the system bars. It does this by removing all the flags
    // except for the ones that make the content appear under the system bars.
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void showSystemUI() {
        Activity activity = getActivity();
        final PdfViewCtrlTabFragment currentFragment = getCurrentPdfViewCtrlFragment();
        View view = getView();
        if (activity == null || currentFragment == null || view == null) {
            return;
        }

        if (isInFullScreenMode()) {
            int oldFlags = view.getSystemUiVisibility();
            int newFlags = oldFlags;

            // Remove the fullscreen system UI flags.
            newFlags &= ~(View.SYSTEM_UI_FLAG_FULLSCREEN // show status bar
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // show nav bar
                    | View.SYSTEM_UI_FLAG_IMMERSIVE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

            if (newFlags != oldFlags) {
                view.setSystemUiVisibility(newFlags);
                view.requestLayout(); // Force a layout invalidation.
            }
        }

        if (sDebug)
            Log.d(TAG, "show system UI called");
    }

    /**
     * Hides the system UI.
     */
    // This snippet hides the system bars.
    // http://stackoverflow.com/a/33551538
    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected void hideSystemUI() {
        Activity activity = getActivity();
        View view = getView();
        if (activity == null || view == null) {
            return;
        }

        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        if (isInFullScreenMode()) {
            int oldFlags = view.getSystemUiVisibility();
            int newFlags = oldFlags;

            // Add the fullscreen system UI flags.
            newFlags |= (View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    | View.SYSTEM_UI_FLAG_IMMERSIVE);

            if (newFlags != oldFlags) {
                view.setSystemUiVisibility(newFlags);
                view.requestLayout(); // Force a layout invalidation.
            }
        }

        if (sDebug)
            Log.d(TAG, "hide system UI called");
    }

    /**
     * Update the system UI layout flags as appropriate for the current
     * fullscreen mode setting.
     * <p>
     * NOTE:
     * The {@link AppBarLayout} can only request stable system window insets when in fullscreen mode.
     * This is due to an apparent issue with {@link CoordinatorLayout}
     * where if the CoordinatorLayout is not receiving stable insets (as required for the tab
     * fragments' root-level CoordinatorLayout), {@link View#requestLayout()} becomes broken
     * (All layouts passes skip the CoordinatorLayout and its descendants).
     */
    @Override
    protected void updateFullScreenModeLayout() {
        Activity activity = getActivity();
        final View view = getView();
        if (activity == null || view == null || mAppBarLayout == null) {
            return;
        }

        if (Utils.isKitKat()) {
            int oldRootFlags = view.getSystemUiVisibility();
            int newRootFlags = oldRootFlags;

            int oldAppBarFlags = mAppBarLayout.getSystemUiVisibility();
            int newAppBarFlags = oldAppBarFlags;

            if (PdfViewCtrlSettingsManager.getFullScreenMode(activity)) {
                // Add the fullscreen system UI layout flags.
                newRootFlags |= (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

                // Add the stable layout flag.
                newAppBarFlags |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            } else {
                // Remove the fullscreen system UI layout flags.
                newRootFlags &= ~(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

                // Remove the stable layout flag.
                newAppBarFlags &= ~View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            }

            view.setSystemUiVisibility(newRootFlags);
            mAppBarLayout.setSystemUiVisibility(newAppBarFlags);

            // View has an internal check for whether the flags changed,
            // but we need our own to prevent unnecessary requestLayout()'s.
            if (newRootFlags != oldRootFlags || newAppBarFlags != oldAppBarFlags) {
                view.requestLayout(); // Force a layout invalidation.
            }
        }

        // Request a new dispatch of system window insets.
        ViewCompat.requestApplyInsets(view);
    }

    /**
     * Starts the search mode.
     */
    public void startSearchMode() {
        FragmentActivity activity = getActivity();
        PdfViewCtrlTabFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null || mTabLayout == null) {
            return;
        }
        super.startSearchMode();

        if (!Utils.isLargeScreen(activity)) {
            mSearchToolbar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int toolbarHeight = mSearchToolbar.getMeasuredHeight();
            currentFragment.setViewerTopMargin(toolbarHeight + mSystemWindowInsetTop);
        }
    }

    /**
     * Exits the search mode.
     */
    public void exitSearchMode() {
        final PdfViewCtrlTabFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (!mIsSearchMode || currentFragment == null || mTabLayout == null) {
            return;
        }
        setThumbSliderVisibility(true, true);

        currentFragment.setViewerTopMargin(0);

        super.exitSearchMode();
    }

    private void adjustShareButtonShowAs(Activity activity) {
        MenuItem menuShare = getToolbarOptionMenuItem(R.id.action_share);
        if (menuShare == null || menuShare.getOrder() != 0) { // skip if it has been customized
            return;
        }
        if (Utils.isScreenTooNarrow(activity)) {
            int count = Utils.toolbarIconMaxCount(activity);
            if (count >= MAX_TOOLBAR_ICON_COUNT) {
                menuShare.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            } else {
                menuShare.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
        } else {
            menuShare.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
    }

    protected void updateUndoButtonVisibility(boolean visible) {
        if (null == mMenuUndo) {
            return;
        }
        final PdfViewCtrlTabFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment != null) {
            ToolManager toolManager = currentFragment.getToolManager();
            if (toolManager != null) {
                mMenuUndo.setVisible(visible && toolManager.isShowUndoRedo() && (mViewerConfig == null || mViewerConfig.isDocumentEditingEnabled()));
            }
        }
    }

    protected void initOptionsMenu(Menu menu) {
        super.initOptionsMenu(menu);
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        mMenuUndo = menu.findItem(R.id.undo);
        mMenuRedo = menu.findItem(R.id.redo);
        mMenuAnnotToolbar = menu.findItem(R.id.action_annotation_toolbar);
        mMenuFormToolbar = menu.findItem(R.id.action_form_toolbar);
        mMenuFillAndSignToolbar = menu.findItem(R.id.action_fill_and_sign_toolbar);
        adjustShareButtonShowAs(activity);
    }

    /**
     * Sets the visibility of options menu
     *
     * @param visible True if visible
     */
    protected void setOptionsMenuVisible(boolean visible) {
        super.setOptionsMenuVisible(visible);
        MenuItem menuSearch = getToolbarOptionMenuItem(R.id.action_search);
        if (menuSearch != null) {
            menuSearch.setVisible(mViewerConfig == null || mViewerConfig.isShowSearchView());
        }
        if (mMenuUndo != null) {
            mMenuUndo.setVisible(visible && (mViewerConfig == null || mViewerConfig.isDocumentEditingEnabled()));
        }
        if (mMenuRedo != null) {
            mMenuRedo.setVisible(visible && (mViewerConfig == null || mViewerConfig.isDocumentEditingEnabled()));
        }
        if (mMenuAnnotToolbar != null) {
            mMenuAnnotToolbar.setVisible(visible && (mViewerConfig == null || mViewerConfig.isShowAnnotationToolbarOption()));
        }
        if (mMenuFormToolbar != null) {
            mMenuFormToolbar.setVisible(visible && (mViewerConfig == null || mViewerConfig.isShowFormToolbarOption()));
        }
        if (mMenuFillAndSignToolbar != null) {
            mMenuFillAndSignToolbar.setVisible(visible && (mViewerConfig == null || mViewerConfig.isShowFillAndSignToolbarOption()));
        }
        MenuItem menuReflowMode = getToolbarOptionMenuItem(R.id.action_reflow_mode);
        if (menuReflowMode != null) {
            menuReflowMode.setVisible(visible && (mViewerConfig == null || mViewerConfig.isShowReflowOption()));
        }

        updateUndoButtonVisibility(visible);
    }

    @Override
    public boolean handleBackPressed() {
        final PdfViewCtrlTabFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return false;
        }

        if (currentFragment.isAnnotationMode()) {
            currentFragment.hideAnnotationToolbar();
            return true;
        }
        return super.handleBackPressed();
    }

    @Override
    protected void animateToolbars(final boolean visible) {
        Activity activity = getActivity();
        if (activity == null || mAppBarLayout == null) {
            return;
        }

        if ((mAppBarLayout.getVisibility() == View.VISIBLE) == visible) {
            return;
        }

        // workaround for not showing hand icon when mouse pointer is over tabs but tab layout
        // is not visible
        // it is a mystery why if tab layout is forced to be hidden in onAnimationEnd
        // then it doesn't work properly!
        // even this workaround doesn't solve hiding hand icon completely since if mouse goes
        // over a narrow bar on top of tabs it still shows hand icon
        if (Utils.isNougat()) {
            if (getCurrentPdfViewCtrlFragment() != null && getCurrentPdfViewCtrlFragment().getPDFViewCtrl() != null) {
                PointF point = getCurrentPdfViewCtrlFragment().getPDFViewCtrl().getCurrentMousePosition();
                if (point.x != 0f || point.y != 0f) {
                    setTabLayoutVisible(visible);
                }
            }
        }
        if (mViewerConfig == null || mViewerConfig.isShowAppBar()) {
            int duration = visible ? ANIMATE_DURATION_SHOW : ANIMATE_DURATION_HIDE;
            Transition slide = new Slide(Gravity.TOP).setDuration(duration);
            TransitionManager.beginDelayedTransition(mAppBarLayout, slide);
            if (visible) {
                mAppBarLayout.setVisibility(View.VISIBLE);
            } else {
                mAppBarLayout.setVisibility(View.GONE);
            }
        } else {
            mAppBarLayout.setVisibility(View.GONE);
        }
        if (mAppBarVisibilityListener != null) {
            mAppBarVisibilityListener.onAppBarVisibilityChanged(visible);
        }
    }

    /**
     * Called when the fragment is resumed.
     */
    protected void resumeFragment() {
        super.resumeFragment();
    }

    /**
     * Called when the fragment is paused.
     */
    protected void pauseFragment() {
        super.pauseFragment();

        if (mUndoRedoPopupWindow != null && mUndoRedoPopupWindow.isShowing()) {
            mUndoRedoPopupWindow.dismiss();
        }
    }

    /**
     * Returns a {@link PdfViewCtrlTabFragment} class object that will be used to
     * instantiate viewer tabs.
     *
     * @return a {@code CollabPdfViewCtrlTabFragment} class to instantiate later
     */
    @NonNull
    protected Class<? extends PdfViewCtrlTabFragment> getDefaultTabFragmentClass() {
        return PdfViewCtrlTabFragment.class;
    }

    @Override
    protected void setFragmentListeners(Fragment fragment) {
        super.setFragmentListeners(fragment);
        if (fragment instanceof PdfViewCtrlTabFragment) {
            PdfViewCtrlTabFragment tabFragment = (PdfViewCtrlTabFragment) fragment;
            tabFragment.addAnnotationToolbarListener(this);
        }
    }

    @Override
    protected void removeFragmentListeners(Fragment fragment) {
        super.removeFragmentListeners(fragment);
        if (fragment instanceof PdfViewCtrlTabFragment) {
            PdfViewCtrlTabFragment tabFragment = (PdfViewCtrlTabFragment) fragment;
            tabFragment.removeAnnotationToolbarListener(this);
        }
    }

    public PdfViewCtrlTabFragment getCurrentPdfViewCtrlFragment() {
        PdfViewCtrlTabBaseFragment fragment = super.getCurrentPdfViewCtrlFragment();
        if (fragment instanceof PdfViewCtrlTabFragment) {
            return (PdfViewCtrlTabFragment) fragment;
        }
        return null;
    }
}
