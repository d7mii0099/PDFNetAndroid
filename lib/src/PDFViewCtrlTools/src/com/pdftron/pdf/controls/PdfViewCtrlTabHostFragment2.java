//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.TooltipCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.MenuItemCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.transition.ChangeBounds;
import androidx.transition.Fade;
import androidx.transition.Slide;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import com.google.android.material.tabs.TabLayout;
import com.pdftron.common.RecentlyUsedCache;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.dialog.menueditor.MenuCreatorDialogFragment;
import com.pdftron.pdf.dialog.menueditor.MenuEditorDialogFragment;
import com.pdftron.pdf.dialog.menueditor.MenuEditorEvent;
import com.pdftron.pdf.dialog.menueditor.MenuEditorViewModel;
import com.pdftron.pdf.dialog.menueditor.model.MenuEditorItem;
import com.pdftron.pdf.dialog.menueditor.model.MenuEditorItemContent;
import com.pdftron.pdf.dialog.menueditor.model.MenuEditorItemHeader;
import com.pdftron.pdf.dialog.redaction.SearchRedactionDialogFragment;
import com.pdftron.pdf.dialog.tabswitcher.TabSwitcherDialogFragment;
import com.pdftron.pdf.dialog.tabswitcher.TabSwitcherEvent;
import com.pdftron.pdf.dialog.tabswitcher.TabSwitcherViewModel;
import com.pdftron.pdf.dialog.tabswitcher.model.TabSwitcherItem;
import com.pdftron.pdf.dialog.toolbarswitcher.ToolbarSwitcherViewModel;
import com.pdftron.pdf.dialog.toolbarswitcher.button.ToolbarSwitcherButton;
import com.pdftron.pdf.dialog.toolbarswitcher.button.ToolbarSwitcherCompactButton;
import com.pdftron.pdf.dialog.toolbarswitcher.dialog.ToolbarSwitcherDialog;
import com.pdftron.pdf.dialog.toolbarswitcher.model.ToolbarSwitcherItem;
import com.pdftron.pdf.dialog.toolbarswitcher.model.ToolbarSwitcherState;
import com.pdftron.pdf.model.BookmarkButtonState;
import com.pdftron.pdf.model.PageState;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.tools.UndoRedoManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.EventHandler;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.cache.BookmarksCache;
import com.pdftron.pdf.viewmodel.BookmarkButtonViewModel;
import com.pdftron.pdf.viewmodel.ViewerShortcutViewModel;
import com.pdftron.pdf.widget.AppBarLayout;
import com.pdftron.pdf.widget.bottombar.component.BottomBarComponent;
import com.pdftron.pdf.widget.preset.component.PresetBarComponent;
import com.pdftron.pdf.widget.preset.component.PresetBarViewModel;
import com.pdftron.pdf.widget.preset.component.model.PresetBarState;
import com.pdftron.pdf.widget.preset.component.view.PresetBarView;
import com.pdftron.pdf.widget.preset.signature.SignatureViewModel;
import com.pdftron.pdf.widget.toolbar.ToolManagerViewModel;
import com.pdftron.pdf.widget.toolbar.builder.AnnotationToolbarBuilder;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarButtonType;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarItem;
import com.pdftron.pdf.widget.toolbar.component.AnnotationToolbarComponent;
import com.pdftron.pdf.widget.toolbar.component.AnnotationToolbarViewModel;
import com.pdftron.pdf.widget.toolbar.component.DefaultToolbars;
import com.pdftron.pdf.widget.toolbar.component.TabletAnnotationToolbarComponent;
import com.pdftron.pdf.widget.toolbar.component.ToolbarSharedPreferences;
import com.pdftron.pdf.widget.toolbar.component.view.ActionButton;
import com.pdftron.pdf.widget.toolbar.component.view.AnnotationToolbarView;
import com.pdftron.pdf.widget.toolbar.component.view.TabActionButton;
import com.pdftron.pdf.widget.toolbar.component.view.UndoActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * The PdfViewCtrlTabHostFragment2 shows multiple {@link PdfViewCtrlTabFragment2}
 * in tab layout.
 */
public class PdfViewCtrlTabHostFragment2 extends PdfViewCtrlTabHostBaseFragment implements
        PdfViewCtrlTabFragment2.TabListener,
        ToolManager.SnackbarListener,
        PdfViewCtrlTabFragment2.ComponentListener {

    private static final String TAG = PdfViewCtrlTabHostFragment2.class.getName();

    protected ViewGroup mBottomBarContainer;
    protected View mBottomBarShadow;
    protected boolean mBottomBarVisible;

    // menus
    @Nullable
    protected MenuItem mMenuTabs;
    @Nullable
    protected MenuItem mMenuUndo;
    @Nullable
    protected MenuItem mMenuBookmark;
    protected boolean mCustomMenuUsed = false;

    // View Models
    protected ToolbarSwitcherViewModel mSwitcherViewModel;
    protected ToolManagerViewModel mToolManagerViewModel;
    protected SignatureViewModel mSignatureViewModel;
    protected PresetBarViewModel mPresetViewModel;
    protected AnnotationToolbarViewModel mAnnotationToolbarViewModel;
    protected TabSwitcherViewModel mTabSwitcherViewModel;
    @Nullable
    private BookmarkButtonViewModel mBookmarkButtonViewModel;

    protected TabletAnnotationToolbarComponent mAnnotationToolbarComponent;
    protected PresetBarComponent mPresetBarComponent;
    protected BottomBarComponent mBottomNavComponent;

    protected ToolbarSwitcherButton mSwitcherButton;
    protected ToolbarSwitcherCompactButton mSwitcherCompactButton;
    protected TabActionButton mTabActionView;
    protected UndoActionButton mUndoActionButton;
    private boolean mHasCustomAnnotationToolbars;
    private boolean mHasCustomBottomBars;
    private boolean mIsShowAnnotationToolbarOption = true;
    private boolean mIsShowToolbarSwitcher = true;

    // used to detect whether there is inset on the top
    // to determine whether to show the status bar even if option is turned off
    protected boolean mShouldShowStatusBar = false;

    // Listeners
    private final List<OnToolbarChangedListener> mOnToolbarChangedListeners = new ArrayList<>();
    private final List<OnPreBuildToolbarListener> mOnPreBuildToolbarListeners = new ArrayList<>();
    private ToolManager.ToolManagerChangedListener mListener = new ToolManager.ToolManagerChangedListener() {
        @Override
        public void onDisabledToolModeChanged(@NonNull Set<ToolMode> disabledToolModes) {
            // Set tool mode filter which will re-inflate the annotation toolbar
            if (mAnnotationToolbarViewModel != null) {
                mAnnotationToolbarViewModel.setToolModeFilter(disabledToolModes);
            }
            // After we want to update undo/redo state
            updateUndoRedoState();
        }

        @Override
        public void onUndoRedoShownChanged(@NonNull Boolean isShown) {
            // Set tool mode filter which will re-inflate the annotation toolbar
            mAnnotationToolbarViewModel.setToolbarButtonVisibility(ToolbarButtonType.UNDO, isShown);
            mAnnotationToolbarViewModel.setToolbarButtonVisibility(ToolbarButtonType.REDO, isShown);
            // After we want to update undo/redo state
            updateUndoRedoState();
        }
    };

    @Override
    public PresetBarComponent getPresetBarComponent() {
        return mPresetBarComponent;
    }

    @Override
    public void onShowSnackbar(@NonNull CharSequence text, int duration, @Nullable CharSequence actionText, View.OnClickListener action) {
        showSnackbar(text.toString(), actionText != null ? actionText.toString() : "", action, duration);
    }

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
        // no op
    }

    @Override
    protected void handleAutoHideNavBar() {
        hideSystemUI();
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        super.onSystemUiVisibilityChange(visibility);

        // in new ui, we have option to show/hide system bars
        Activity activity = getActivity();
        if (activity != null) {
            if (!PdfViewCtrlSettingsManager.getShowNavigationBarForNewUI(activity)) {
                resetHideNavigationBarTimer();
            }
        }
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_tabbed_pdfviewctrl_new;
    }

    @Override
    protected int getContainerId() {
        return R.id.realtabcontent;
    }

    @Override
    protected int getTabLayoutRes() {
        return R.layout.fragment_tabbed_pdfviewctrl_tab_new;
    }

    @Override
    int[] getDefaultToolbarMenu() {
        return new int[]{R.menu.fragment_viewer_new};
    }

    /**
     * Returns a new instance of the class
     */
    public static PdfViewCtrlTabHostFragment2 newInstance(Bundle args) {
        // args has information about the new tab
        // the information about other tabs (already added) is accessible from PdfViewCtrlTabsManager
        PdfViewCtrlTabHostFragment2 fragment = new PdfViewCtrlTabHostFragment2();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // View models must be initialized before returning from onCreate
        mSwitcherViewModel = ViewModelProviders.of(this).get(ToolbarSwitcherViewModel.class);
        mToolManagerViewModel = ViewModelProviders.of(this).get(ToolManagerViewModel.class);
        mPresetViewModel = ViewModelProviders.of(this).get(PresetBarViewModel.class);
        mAnnotationToolbarViewModel = ViewModelProviders.of(this).get(AnnotationToolbarViewModel.class);
        mBookmarkButtonViewModel = ViewModelProviders.of(this).get(BookmarkButtonViewModel.class);

        final FragmentActivity activity = getActivity();
        if (canRecreateActivity() && activity instanceof AppCompatActivity && applyTheme((AppCompatActivity) activity)) {
            return;
        }
        mSignatureViewModel = ViewModelProviders.of(activity).get(SignatureViewModel.class);

        mHasCustomAnnotationToolbars = mViewerConfig != null && !mViewerConfig.getToolbarBuilders().isEmpty();
        mIsShowAnnotationToolbarOption = mViewerConfig == null || mViewerConfig.isShowAnnotationToolbarOption();
        mIsShowToolbarSwitcher = mViewerConfig == null || mViewerConfig.isShowToolbarSwitcher();
        mHasCustomBottomBars = mViewerConfig != null && mViewerConfig.getBottomBarBuilder() != null;
        mBottomBarVisible = mViewerConfig == null || mViewerConfig.isShowBottomToolbar();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        FragmentActivity activity = getActivity();
        if (activity != null) {
            if (!Utils.isTablet(activity) &&
                    mAnnotationToolbarComponent != null &&
                    mFragmentView != null) {
                // update to either phone or tablet layout based on orientation
                boolean useTabletAnnotationToolbar = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
                mAnnotationToolbarComponent.setTabletMode(useTabletAnnotationToolbar);
                FrameLayout presetsContainer = mFragmentView.findViewById(R.id.presets_container);
                presetsContainer.setVisibility(mHidePresetBar || useTabletAnnotationToolbar ? View.GONE : View.VISIBLE);
                inflateToolbarState(activity);

                // bottom bar
                FrameLayout bottomNavContainer = mFragmentView.findViewById(R.id.bottom_nav_container);
                if (useTabletAnnotationToolbar || !mBottomBarVisible) {
                    bottomNavContainer.setVisibility(View.GONE);
                } else {
                    bottomNavContainer.setVisibility(View.VISIBLE);
                }

                // also recreate options menu
                onCreateOptionsMenu(mToolbar.getMenu(), new MenuInflater(activity));
            }
        }
    }

    protected boolean useTabletLayout() {
        if (mViewerConfig == null || mViewerConfig.isTabletLayoutEnabled()) {
            Context context = getContext();
            if (context != null) {
                return (Utils.isTablet(context) || Utils.isLandscape(context));
            }
        }
        return false;
    }

    protected void inflateToolbarState(@NonNull final Activity activity) {
        mDisposables.add(
                Single.just(mHasCustomAnnotationToolbars)
                        .map(new Function<Boolean, List<ToolbarSwitcherItem>>() {
                            @Override
                            public List<ToolbarSwitcherItem> apply(@io.reactivex.annotations.NonNull Boolean hasCustomToolbars) throws Exception {
                                Utils.throwIfOnMainThread();
                                List<ToolbarSwitcherItem> items;
                                if (!mIsShowAnnotationToolbarOption) {
                                    items = new ArrayList<>();
                                    items.add(
                                            new ToolbarSwitcherItem(
                                                    DefaultToolbars.defaultViewToolbar
                                            )
                                    );
                                    selectInitialToolbar(activity, items);
                                } else if (mViewerConfig == null || !hasCustomToolbars) {
                                    // Get default toolbars
                                    items = getToolbarSwitcherList(activity);
                                } else {
                                    // Create toolbars from builder
                                    List<AnnotationToolbarBuilder> toolbarBuilders = mViewerConfig.getToolbarBuilders();
                                    items = new ArrayList<>();

                                    for (AnnotationToolbarBuilder toolbarBuilder : toolbarBuilders) {
                                        AnnotationToolbarBuilder storedToolbar;
                                        if (mViewerConfig.isSaveToolbarItemOrder()) {
                                            storedToolbar = mToolbarSharedPreferences.getCustomToolbar(activity, toolbarBuilder);
                                        } else {
                                            storedToolbar = toolbarBuilder;
                                        }
                                        items.add(new ToolbarSwitcherItem(
                                                storedToolbar)
                                        );
                                    }
                                    removeToolbarUsingConfig(items);
                                    selectInitialToolbar(activity, items);
                                }
                                return items;
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<List<ToolbarSwitcherItem>>() {
                            @Override
                            public void accept(List<ToolbarSwitcherItem> toolbarSwitcherItems) throws Exception {
                                ToolbarSwitcherState state = new ToolbarSwitcherState(toolbarSwitcherItems);
                                mSwitcherViewModel.setState(state);
                            }
                        })
        );
        mSwitcherViewModel.observeToolbarSwitcherState(this, new Observer<ToolbarSwitcherState>() {
            @Override
            public void onChanged(ToolbarSwitcherState toolbarSwitcherState) {
                if (toolbarSwitcherState != null) {
                    ToolbarSwitcherItem selectedToolbar = toolbarSwitcherState.getSelectedToolbar();

                    // Inflate the toolbar
                    mAnnotationToolbarComponent.inflateWithBuilder(selectedToolbar.builder);

                    // Update some menu item states
                    updateAllMenuItems();

                    if (mViewerConfig != null && mAnnotationToolbarComponent != null) {
                        mAnnotationToolbarComponent.setToolbarItemGravity(mViewerConfig.getToolbarItemGravity());
                    }
                    // Update our switcher button title, also make it clickable only if we have multiple toolbars
                    mSwitcherButton.setText(selectedToolbar.getToolbarName(activity));
                    mSwitcherCompactButton.setText(selectedToolbar.getToolbarName(activity));
                    if (toolbarSwitcherState.hasMultipleToolbars()) {
                        mSwitcherButton.setClickable(true);
                        mSwitcherButton.showSwitcherIcon();
                        mSwitcherCompactButton.setClickable(true);
                        mSwitcherCompactButton.showSwitcherIcon();
                    } else {
                        mSwitcherButton.setClickable(false);
                        mSwitcherButton.hideSwitcherIcon();
                        mSwitcherCompactButton.setClickable(false);
                        mSwitcherCompactButton.hideSwitcherIcon();
                    }

                    // Finally save the recently used toolbar to shared pref
                    boolean rememberLastToolbar = mViewerConfig == null || mViewerConfig.isRememberLastToolbar();
                    if (rememberLastToolbar) {
                        ToolbarSharedPreferences.setLastOpenedToolbarTag(activity, selectedToolbar.getTag());
                    }

                    // deal with compact ui
                    if (useCompactViewer()) {
                        if (selectedToolbar.getTag().equals(DefaultToolbars.TAG_VIEW_TOOLBAR)) {
                            showTopToolbar();
                        } else {
                            mToolbar.setVisibility(View.GONE);
                        }
                    }
                    // undo redo button
                    if (selectedToolbar.getTag().equals(DefaultToolbars.TAG_VIEW_TOOLBAR)) {
                        updateUndoButtonVisibility(true);
                    } else {
                        if (mMenuUndo != null) {
                            mMenuUndo.setVisible(false);
                        }
                    }
                }
            }
        });
        mPresetViewModel.observePresetState(this, new Observer<PresetBarState>() {
            @Override
            public void onChanged(PresetBarState presetBarState) {
                final PdfViewCtrlTabFragment2 currentFragment = getCurrentPdfViewCtrlFragment();
                if (presetBarState != null && currentFragment != null) {
                    if (presetBarState.isVisible) {
                        currentFragment.setThumbSliderVisible(false, true);
                    } else if (mAppBarLayout.getVisibility() == View.VISIBLE) {
                        currentFragment.setThumbSliderVisible(true, true);
                    }
                }
            }
        });
    }

    @Nullable
    @Override
    protected int[] getToolbarMenuResArray() {
        if (useCompactViewer() && !useTabletLayout() && !hasCustomTopToolbarMenu()) {
            // for compact phone, we show options menu at the bottom bar instead
            return new int[]{
                    R.menu.fragment_viewer_compact_phone
            };
        }
        return super.getToolbarMenuResArray();
    }

    @WorkerThread
    private List<ToolbarSwitcherItem> getToolbarSwitcherList(@NonNull Activity activity) {
        List<ToolbarSwitcherItem> items = getToolbarSwitcherListImpl(activity);

        removeToolbarUsingConfig(items);
        selectInitialToolbar(activity, items);
        return items;
    }

    @WorkerThread
    protected List<ToolbarSwitcherItem> getToolbarSwitcherListImpl(@NonNull Activity activity) {
        List<ToolbarSwitcherItem> items = new ArrayList<>();
        boolean useCompact = useCompactViewer();
        items.add(
                getToolbarSwitcherItem(activity, DefaultToolbars.TAG_VIEW_TOOLBAR, useCompact)
        );
        items.add(
                getToolbarSwitcherItem(activity, DefaultToolbars.TAG_ANNOTATE_TOOLBAR, useCompact)
        );
        items.add(
                getToolbarSwitcherItem(activity, DefaultToolbars.TAG_DRAW_TOOLBAR, useCompact)
        );
        items.add(
                getToolbarSwitcherItem(activity, DefaultToolbars.TAG_FILL_AND_SIGN_TOOLBAR, useCompact)
        );
        items.add(
                getToolbarSwitcherItem(activity, DefaultToolbars.TAG_PREPARE_FORM_TOOLBAR, useCompact)
        );
        items.add(
                getToolbarSwitcherItem(activity, DefaultToolbars.TAG_INSERT_TOOLBAR, useCompact)
        );
        items.add(
                getToolbarSwitcherItem(activity, DefaultToolbars.TAG_MEASURE_TOOLBAR, useCompact)
        );
        items.add(
                getToolbarSwitcherItem(activity, DefaultToolbars.TAG_PENS_TOOLBAR, useCompact)
        );
        items.add(
                getToolbarSwitcherItem(activity, DefaultToolbars.TAG_REDACTION_TOOLBAR, useCompact)
        );
        items.add(
                getToolbarSwitcherItem(activity, DefaultToolbars.TAG_FAVORITE_TOOLBAR, useCompact)
        );

        return items;
    }

    protected boolean useCompactViewer() {
        return mViewerConfig == null || mViewerConfig.isUseCompactViewer();
    }

    @Nullable
    protected ToolbarSwitcherItem getToolbarSwitcherItem(@NonNull Activity activity,
            @NonNull String tag, boolean useCompact) {
        if (tag.equals(DefaultToolbars.TAG_VIEW_TOOLBAR)) {
            return new ToolbarSwitcherItem(
                    mToolbarSharedPreferences.getViewToolbar());
        }
        if (useCompact) {
            switch (tag) {
                case DefaultToolbars.TAG_ANNOTATE_TOOLBAR: {
                    AnnotationToolbarBuilder builder = mToolbarSharedPreferences.getCompactAnnotateToolbar(activity);
                    if (useTabletLayout()) {
                        builder.addCustomStickyButton(R.string.more, R.drawable.ic_overflow_white_24dp, DefaultToolbars.ButtonId.MORE.value());
                    }
                    return new ToolbarSwitcherItem(builder);
                }
                case DefaultToolbars.TAG_DRAW_TOOLBAR: {
                    AnnotationToolbarBuilder builder = mToolbarSharedPreferences.getCompactDrawToolbar(activity);
                    if (useTabletLayout()) {
                        builder.addCustomStickyButton(R.string.more, R.drawable.ic_overflow_white_24dp, DefaultToolbars.ButtonId.MORE.value());
                    }
                    return new ToolbarSwitcherItem(builder);
                }
                case DefaultToolbars.TAG_INSERT_TOOLBAR: {
                    AnnotationToolbarBuilder builder = mToolbarSharedPreferences.getCompactInsertToolbar(activity);
                    if (useTabletLayout()) {
                        builder.addCustomStickyButton(R.string.more, R.drawable.ic_overflow_white_24dp, DefaultToolbars.ButtonId.MORE.value());
                    }
                    return new ToolbarSwitcherItem(builder);
                }
                case DefaultToolbars.TAG_FILL_AND_SIGN_TOOLBAR: {
                    AnnotationToolbarBuilder builder = mToolbarSharedPreferences.getCompactFillAndSignToolbar(activity);
                    if (useTabletLayout()) {
                        builder.addCustomStickyButton(R.string.more, R.drawable.ic_overflow_white_24dp, DefaultToolbars.ButtonId.MORE.value());
                    }
                    return new ToolbarSwitcherItem(builder);
                }
                case DefaultToolbars.TAG_PREPARE_FORM_TOOLBAR: {
                    AnnotationToolbarBuilder builder = mToolbarSharedPreferences.getCompactPrepareFormToolbar(activity);
                    if (useTabletLayout()) {
                        builder.addCustomStickyButton(R.string.more, R.drawable.ic_overflow_white_24dp, DefaultToolbars.ButtonId.MORE.value());
                    }
                    return new ToolbarSwitcherItem(builder);
                }
                case DefaultToolbars.TAG_PENS_TOOLBAR: {
                    AnnotationToolbarBuilder builder = mToolbarSharedPreferences.getCompactPensToolbar(activity);
                    if (useTabletLayout()) {
                        builder.addCustomStickyButton(R.string.more, R.drawable.ic_overflow_white_24dp, DefaultToolbars.ButtonId.MORE.value());
                    }
                    return new ToolbarSwitcherItem(builder);
                }
                case DefaultToolbars.TAG_MEASURE_TOOLBAR: {
                    AnnotationToolbarBuilder builder = mToolbarSharedPreferences.getCompactMeasureToolbar(activity);
                    if (useTabletLayout()) {
                        builder.addCustomStickyButton(R.string.more, R.drawable.ic_overflow_white_24dp, DefaultToolbars.ButtonId.MORE.value());
                    }
                    return new ToolbarSwitcherItem(builder);
                }
                case DefaultToolbars.TAG_REDACTION_TOOLBAR: {
                    AnnotationToolbarBuilder builder = mToolbarSharedPreferences.getCompactRedactToolbar(activity);
                    if (useTabletLayout()) {
                        builder.addCustomStickyButton(R.string.more, R.drawable.ic_overflow_white_24dp, DefaultToolbars.ButtonId.MORE.value());
                    }
                    return new ToolbarSwitcherItem(builder);
                }
                case DefaultToolbars.TAG_FAVORITE_TOOLBAR: {
                    AnnotationToolbarBuilder builder = mToolbarSharedPreferences.getCompactFavoriteToolbar(activity);
                    if (useTabletLayout()) {
                        builder.addCustomStickyButton(R.string.more, R.drawable.ic_overflow_white_24dp, DefaultToolbars.ButtonId.MORE.value());
                    }
                    return new ToolbarSwitcherItem(builder);
                }
            }
        } else {
            switch (tag) {
                case DefaultToolbars.TAG_ANNOTATE_TOOLBAR:
                    return new ToolbarSwitcherItem(
                            mToolbarSharedPreferences.getAnnotateToolbar(activity));
                case DefaultToolbars.TAG_DRAW_TOOLBAR:
                    return new ToolbarSwitcherItem(
                            mToolbarSharedPreferences.getDrawToolbar(activity));
                case DefaultToolbars.TAG_INSERT_TOOLBAR:
                    return new ToolbarSwitcherItem(
                            mToolbarSharedPreferences.getInsertToolbar(activity));
                case DefaultToolbars.TAG_FILL_AND_SIGN_TOOLBAR:
                    return new ToolbarSwitcherItem(
                            mToolbarSharedPreferences.getFillAndSignToolbar(activity));
                case DefaultToolbars.TAG_PREPARE_FORM_TOOLBAR:
                    return new ToolbarSwitcherItem(
                            mToolbarSharedPreferences.getPrepareFormToolbar(activity));
                case DefaultToolbars.TAG_PENS_TOOLBAR:
                    return new ToolbarSwitcherItem(
                            mToolbarSharedPreferences.getPensToolbar(activity));
                case DefaultToolbars.TAG_MEASURE_TOOLBAR:
                    return new ToolbarSwitcherItem(
                            mToolbarSharedPreferences.getMeasureToolbar(activity));
                case DefaultToolbars.TAG_REDACTION_TOOLBAR:
                    return new ToolbarSwitcherItem(
                            mToolbarSharedPreferences.getRedactToolbar(activity));
                case DefaultToolbars.TAG_FAVORITE_TOOLBAR:
                    return new ToolbarSwitcherItem(
                            mToolbarSharedPreferences.getFavoriteToolbar(activity));
            }
        }
        return null;
    }

    private void removeToolbarUsingConfig(@NonNull List<ToolbarSwitcherItem> items) {
        // Now, remove all toolbars that is hidden by viewer config
        Set<String> toolbarsToHide = new HashSet<>();
        if (mViewerConfig != null && mViewerConfig.getToolbarsToHide() != null) {
            String[] toolbarsToHideArray = mViewerConfig.getToolbarsToHide();
            toolbarsToHide.addAll(Arrays.asList(toolbarsToHideArray));
        }

        Iterator<ToolbarSwitcherItem> iterator = items.iterator();

        while (iterator.hasNext()) {
            ToolbarSwitcherItem next = iterator.next();
            if (toolbarsToHide.contains(next.getTag())) {
                iterator.remove();
            }
        }
    }

    private void selectInitialToolbar(@NonNull Activity activity, @NonNull List<ToolbarSwitcherItem> items) {
        String lastOpenedToolbar = ToolbarSharedPreferences.getLastOpenedToolbarTag(activity);

        boolean toolbarSelected = false;

        // Select last opened toolbar
        boolean rememberLastToolbar = mViewerConfig == null || mViewerConfig.isRememberLastToolbar(); // default true
        if (lastOpenedToolbar != null && rememberLastToolbar) {
            for (ToolbarSwitcherItem item : items) {
                if (item.getTag().equals(lastOpenedToolbar)) {
                    item.setSelected(true);
                    toolbarSelected = true;
                    break;
                }
            }
        }

        // If we cannot select last opened toolbar, then see if we can select toolbar set from shared preference
        String initialToolbarTag = mViewerConfig != null ? mViewerConfig.getInitialToolbarTag() : null;
        if (!toolbarSelected && initialToolbarTag != null) {
            for (ToolbarSwitcherItem item : items) {
                if (item.getTag().equals(initialToolbarTag)) {
                    item.setSelected(true);
                    toolbarSelected = true;
                    break;
                }
            }
        }

        // If we cannot select last opened toolbar, then just select the annotation toolbar
        if (!toolbarSelected) {
            for (ToolbarSwitcherItem item : items) {
                if (item.getTag().equals(DefaultToolbars.TAG_ANNOTATE_TOOLBAR)) {
                    item.setSelected(true);
                    toolbarSelected = true;
                    break;
                }
            }
        }

        // If we cannot select annotation toolbar, then select the first one
        if (!toolbarSelected) {
            for (ToolbarSwitcherItem item : items) {
                item.setSelected(true);
                toolbarSelected = true;
                break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (super.onOptionsItemSelected(item)) {
            return true;
        }

        FragmentActivity activity = getActivity();
        PdfViewCtrlTabFragment2 currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return false;
        }
        if (!(item.getActionView() instanceof ActionButton)) {
            if (mAnnotationToolbarComponent != null && mAnnotationToolbarComponent.isEditing()) {
                currentFragment.getToolManager().setTool(currentFragment.getToolManager().createTool(ToolMode.PAN, null));
            }
        }

        final int id = item.getItemId();

        if (id == R.id.action_tabs) {
            if (currentFragment.isDocumentReady()) {
                onOpenTabSwitcher();
            }
        } else if (id == R.id.action_outline) {
            onOutlineOptionSelected();
            // First we toggle the outline button
            mBottomNavComponent.setItemSelected(currentFragment.isNavigationListShowing(), R.id.action_outline);
        } else if (id == R.id.action_thumbnails) {
            onPageThumbnailOptionSelected(false, null);
        } else if (id == R.id.action_navigation || id == DefaultToolbars.ButtonId.NAVIGATION.value()) {
            handleNavIconClick();
        } else if (id == R.id.toolbar_switcher) {
            if (item.getActionView() != null) {
                showToolbarSwitcherDialog(item.getActionView());
            }
        } else if (id == R.id.action_overflow || id == DefaultToolbars.ButtonId.MORE.value()) {
            if (item.getActionView() != null) {
                showViewOverflowMenu(item.getActionView());
            }
        } else if (id == R.id.action_bookmark_add) {
            PdfViewCtrlTabFragment2 currentTabFragment = getCurrentPdfViewCtrlFragment();
            if (currentTabFragment != null) {
                currentTabFragment.togglePageBookmark();
            }
        } else {
            return false;
        }

        return true;
    }

    private void showToolbarSwitcherDialog(View anchorView) {
        FragmentActivity activity = getActivity();
        PdfViewCtrlTabFragment2 currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return;
        }
        ToolbarSwitcherDialog popupWindow = buildToolBarSwitcherDialog(activity, anchorView);
        popupWindow.setTargetFragment(this, 0);
        popupWindow.show(this.getFragmentManager());
    }

    protected ToolbarSwitcherDialog buildToolBarSwitcherDialog(FragmentActivity activity, View anchorView) {
        return new ToolbarSwitcherDialog.Builder()
                .setAnchorView(anchorView)
                .build(activity);
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabFragment2.TabListener#onTabDocumentLoaded(String)}.
     */
    @Override
    public void onTabDocumentLoaded(String tag) {
        // update undo button visibility
        updateUndoRedoState();

        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment != null) {
            // Clear our annotation toolbar when switching tabs, then set the tool manager
            mAnnotationToolbarComponent.clearState();
            setToolManagerForViewModel();

            // If document tab is read only, then switch to view toolbar
            if (tabNeedsReadOnlyCheck()) {
                openToolbarWithTag(DefaultToolbars.TAG_VIEW_TOOLBAR);
            }

            if (mBookmarkButtonViewModel != null && isQuickBookmarkCreationEnabled()) {
                attachBookmarkButtonData(currentFragment);
            }
        }

        // Need to call after toolmanager is set
        super.onTabDocumentLoaded(tag);
    }

    private void setToolManagerForViewModel() {
        PdfViewCtrlTabFragment2 currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment != null) {
            ToolManager newToolManager = currentFragment.getToolManager();
            // Remove old listener
            ToolManager oldToolManager = mToolManagerViewModel.getToolManager();
            if (oldToolManager != null) {
                oldToolManager.removeToolManagerChangedListener(mListener);
            }
            // Add new listener
            mToolManagerViewModel.setToolManager(newToolManager); // order matters, add listener first
            if (newToolManager != null) {
                Set<ToolMode> disabledToolModes = newToolManager.getDisabledToolModes();
                mAnnotationToolbarViewModel.setToolModeFilter(disabledToolModes == null ? new HashSet<ToolMode>() : disabledToolModes);
                newToolManager.addToolManagerChangedListener(mListener);
            }
        }
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabFragment2.TabListener#onInkEditSelected(Annot, int)}.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onInkEditSelected(Annot inkAnnot, int pageNum) {
        openEditToolbar(ToolMode.INK_CREATE, inkAnnot, pageNum);
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabFragment2.TabListener#onOpenAnnotationToolbar(ToolMode)}.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onOpenAnnotationToolbar(ToolMode mode) {
        // no op
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabFragment2.TabListener#onOpenEditToolbar(ToolMode)}.
     */
    @Override
    public void onOpenEditToolbar(ToolMode mode) {
        openEditToolbar(mode, null, 0);
    }

    private void openEditToolbar(ToolMode mode, Annot annot, int pageNum) {
        // Show UI just in case the toolbar is hidden
        showUI();
        // Switch to draw toolbar, as the annotation toolbar may be hidden by being in view toolbar
        ToolbarSwitcherState state = mSwitcherViewModel.getState();
        if (state != null && state.getSelectedToolbar().getTag().equals(DefaultToolbars.TAG_VIEW_TOOLBAR)) {
            openToolbarWithTag(DefaultToolbars.TAG_DRAW_TOOLBAR);
        }
        // Open the edit toolbar
        mAnnotationToolbarComponent.showEditToolbar(mode, annot, pageNum, false);
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabFragment2.TabListener#onToggleReflow()}.
     */
    @Override
    public void onToggleReflow() {
        super.onToggleReflow();
        final PdfViewCtrlTabFragment2 currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }

        if (mHasCustomAnnotationToolbars) {
            if (currentFragment.isReflowMode()) {
                mAnnotationToolbarComponent.hide(true);
            } else {
                mAnnotationToolbarComponent.show(true);
            }
        } else {
            if (currentFragment.isReflowMode()) {
                openToolbarWithTag(DefaultToolbars.TAG_VIEW_TOOLBAR);
            }
        }
    }

    @Nullable
    private ArrayList<MenuEditorItem> getToolbarMenuItems(boolean isFavorite) {
        ToolbarSwitcherState state = mSwitcherViewModel.getState();
        if (state == null) {
            return null;
        }

        AnnotationToolbarBuilder builder = state.getSelectedToolbar().builder;
        return getToolbarMenuItems(builder, isFavorite);
    }

    @Nullable
    private ArrayList<MenuEditorItem> getToolbarMenuItems(AnnotationToolbarBuilder builder, boolean isFavorite) {
        List<ToolbarItem> toolbarItems = new ArrayList<>(builder.getToolbarItems());

        // Remove items that are disabled in tool manager
        ToolManager toolManager = mToolManagerViewModel.getToolManager();
        if (toolManager != null && toolManager.getDisabledToolModes() != null) {
            AnnotationToolbarBuilder.removeItems(toolbarItems, toolManager.getDisabledToolModes());
        }

        // First sort by order before adding to editor
        List<ToolbarItem> sortedToolbarItems = new ArrayList<>(toolbarItems);
        Collections.sort(sortedToolbarItems, new Comparator<ToolbarItem>() {
            @Override
            public int compare(ToolbarItem o1, ToolbarItem o2) {
                return o1.order - o2.order;
            }
        });

        ArrayList<MenuEditorItem> actionButtons = new ArrayList<>();

        for (ToolbarItem toolbarItem : sortedToolbarItems) {
            if (toolbarItem.buttonId == DefaultToolbars.ButtonId.CUSTOMIZE.value()) {
                continue;
            }
            // skip action edit menu, we do not allow this to be customized
            String itemTitle = toolbarItem.titleRes != 0 ? getResources().getString(toolbarItem.titleRes) : toolbarItem.title;
            MenuEditorItemContent itemContent = new MenuEditorItemContent(
                    toolbarItem.buttonId,
                    toolbarItem.toolbarButtonType,
                    itemTitle,
                    DrawableCompat.wrap(getResources().getDrawable(toolbarItem.icon)).mutate());
            actionButtons.add(itemContent);
        }

        ArrayList<MenuEditorItem> items = new ArrayList<>();
        if (!isFavorite) {
            MenuEditorItemHeader header1 = new MenuEditorItemHeader(
                    MenuEditorItem.GROUP_SHOW_IF_ROOM,
                    0,
                    0);
            items.add(header1);
        }
        items.addAll(actionButtons);

        return items;
    }

    @Override
    public void onEditToolbarMenu() {
        ToolbarSwitcherState state = mSwitcherViewModel.getState();
        if (state == null) {
            return;
        }
        // Special case for favorite toolbar since it has a different UI
        if (state.getSelectedToolbar().builder.getToolbarTag().equals(DefaultToolbars.TAG_FAVORITE_TOOLBAR)) {
            showEditFavoriteToolbarDialog();
        } else {
            showEditDefaultToolbarDialog(state);
        }
    }

    @Override
    protected int getDefaultTheme() {
        return R.style.PDFTronAppTheme;
    }

    @Nullable
    private ArrayList<MenuEditorItem> getAllToolbarMenuItems() {
        Context context = getContext();
        if (context == null) {
            return null;
        }

        LinkedHashMap<String, List<ToolbarItem>> allToolbarItems = new LinkedHashMap<>();

        ToolbarSwitcherState state = mSwitcherViewModel.getState();
        // try to fetch actual items from current toolbar state
        if (state != null) {
            int toolbarSize = state.size();
            for (int i = 0; i < toolbarSize; i++) {
                ToolbarSwitcherItem item = state.get(i);
                if (!item.getTag().equals(DefaultToolbars.TAG_FAVORITE_TOOLBAR)) {
                    allToolbarItems.put(item.getToolbarName(context), ToolbarSharedPreferences.getCustomizableSublist(item.getToolbarItems()));
                }
            }
        } else {
            allToolbarItems = mToolbarSharedPreferences.getAllDefaultToolbarItems(context);
        }

        ToolManager toolManager = mToolManagerViewModel.getToolManager();
        ArrayList<MenuEditorItem> items = new ArrayList<>();

        for (Map.Entry<String, List<ToolbarItem>> entry : allToolbarItems.entrySet()) {
            String title = entry.getKey();
            List<ToolbarItem> toolbarItems = entry.getValue();

            // Remove items that are disabled in tool manager
            if (toolManager != null && toolManager.getDisabledToolModes() != null) {
                AnnotationToolbarBuilder.removeItems(toolbarItems, toolManager.getDisabledToolModes());
            }

            // If we have removed all items, or if only remaining item is the multi-selection tool
            // then skip adding this toolbar since there won't be any useful tools to add
            if (toolbarItems.isEmpty()) {
                continue;
            } else {
                boolean shouldSkipToolbar = true;
                // Go through all toolbar items, if any items are not multi-selection
                // then we can go ahead these toolbar items to the list of toolbar menu items.
                // Otherwise, we'll skip this toolbar and go to the next one.
                for (ToolbarItem toolbarItem : toolbarItems) {
                    if (toolbarItem.toolbarButtonType != ToolbarButtonType.MULTI_SELECT &&
                            toolbarItem.toolbarButtonType != ToolbarButtonType.LASSO_SELECT) {
                        shouldSkipToolbar = false;
                        break;
                    }
                }
                if (shouldSkipToolbar) {
                    continue;
                }
            }

            // Convert ToolbarItem to MenuEditorItem
            ArrayList<MenuEditorItem> actionButtons = new ArrayList<>();
            for (ToolbarItem toolbarItem : toolbarItems) {
                String itemTitle = toolbarItem.titleRes != 0 ? getResources().getString(toolbarItem.titleRes) : toolbarItem.title;
                MenuEditorItemContent itemContent = new MenuEditorItemContent(
                        toolbarItem.buttonId,
                        toolbarItem.toolbarButtonType,
                        itemTitle,
                        DrawableCompat.wrap(getResources().getDrawable(toolbarItem.icon)).mutate());
                actionButtons.add(itemContent);
            }
            MenuEditorItemHeader header = new MenuEditorItemHeader(
                    MenuEditorItem.GROUP_SHOW_IF_ROOM,
                    title,
                    ""
            );
            header.setDraggingTitle(title);
            items.add(header);
            items.addAll(actionButtons);
        }

        return items;
    }

    private void showEditDefaultToolbarDialog(@NonNull final ToolbarSwitcherState state) {
        ArrayList<MenuEditorItem> items = getToolbarMenuItems(false);
        if (null == items) {
            return;
        }

        stopHideToolbarsTimer();

        final MenuEditorViewModel viewModel = ViewModelProviders.of(this).get(MenuEditorViewModel.class);
        viewModel.setItems(items);
        viewModel.getItemsLiveData().removeObservers(getViewLifecycleOwner());
        viewModel.getItemsLiveData().observe(getViewLifecycleOwner(), new Observer<ArrayList<MenuEditorItem>>() {
            @Override
            public void onChanged(ArrayList<MenuEditorItem> menuEditorItems) {
                updateToolbarItemsInDb(menuEditorItems, false);
            }
        });

        mDisposables.add(
                viewModel.getObservable()
                        .subscribe(new Consumer<MenuEditorEvent>() {
                            @Override
                            public void accept(MenuEditorEvent menuEditorEvent) throws Exception {
                                if (menuEditorEvent.getEventType() == MenuEditorEvent.Type.RESET) {

                                    String toolbarTag = state.getSelectedToolbar().getTag();
                                    AnnotationToolbarBuilder builder = DefaultToolbars.getDefaultAnnotationToolbarBuilderByTag(toolbarTag);
                                    viewModel.setItems(getToolbarMenuItems(builder, false));
                                }
                            }
                        })
        );

        String toolbarTitle = state.getSelectedToolbar().getToolbarName(getContext());

        final MenuEditorDialogFragment editorFragment = MenuEditorDialogFragment.newInstance(toolbarTitle);
        editorFragment.setStyle(DialogFragment.STYLE_NORMAL, mThemeProvider.getTheme());
        editorFragment.show(getChildFragmentManager(), MenuEditorDialogFragment.TAG);
        editorFragment.setDialogDismissListener(new CustomSizeDialogFragment.DialogDismissListener() {
            @Override
            public void onMenuEditorDialogDismiss() {
                FragmentActivity context = getActivity();
                if (context != null) {
                    inflateToolbarState(context);
                    resetHideToolbarsTimer();

                    // Check if toolbar has changed, if so then send analytics for the type of toolbar
                    // that got modified, and its top 3 tools
                    ToolbarSwitcherState state = mSwitcherViewModel.getState();
                    if (state != null) {
                        ToolbarSwitcherItem selectedToolbar = state.getSelectedToolbar();
                        boolean toolbarModified = editorFragment.hasModifiedToolbar();

                        AnalyticsHandlerAdapter.getInstance().sendEvent(
                                AnalyticsHandlerAdapter.EVENT_TOOLBAR_REORDER,
                                AnalyticsParam.annotationToolbarModifiedParam(selectedToolbar, toolbarModified)
                        );

                        if (toolbarModified) {
                            AnalyticsHandlerAdapter.getInstance().sendEvent(
                                    AnalyticsHandlerAdapter.EVENT_TOOLBAR_REORDER_TOP_3,
                                    AnalyticsParam.annotationToolbarItemParams(selectedToolbar)
                            );
                        }
                    }
                }
            }
        });
    }

    protected void showEditFavoriteToolbarDialog() {
        ArrayList<MenuEditorItem> allItems = getAllToolbarMenuItems();
        ArrayList<MenuEditorItem> favToolbarItems = getToolbarMenuItems(true);
        if (allItems == null || favToolbarItems == null) {
            return;
        }
        final MenuEditorViewModel viewModel = ViewModelProviders.of(this).get(MenuEditorViewModel.class);
        viewModel.getPinnedItemsLiveData().removeObservers(getViewLifecycleOwner());
        viewModel.setAllItems(allItems);
        viewModel.setPinnedItems(favToolbarItems);
        viewModel.getPinnedItemsLiveData().observe(getViewLifecycleOwner(), new Observer<ArrayList<MenuEditorItem>>() {
            @Override
            public void onChanged(ArrayList<MenuEditorItem> menuEditorItems) {
                updateToolbarItemsInDb(menuEditorItems, true);
            }
        });
        MenuCreatorDialogFragment fragment = MenuCreatorDialogFragment.newInstance();
        fragment.setStyle(DialogFragment.STYLE_NORMAL, mThemeProvider.getTheme());
        fragment.show(getChildFragmentManager(), MenuCreatorDialogFragment.TAG);
        fragment.setDialogDismissListener(new CustomSizeDialogFragment.DialogDismissListener() {
            @Override
            public void onMenuEditorDialogDismiss() {
                FragmentActivity context = getActivity();
                if (context != null) {
                    inflateToolbarState(context);
                    resetHideToolbarsTimer();

                    // Check if toolbar has changed, if so then send analytics for the tools added
                    // and the tools that are duplicated
                    ToolbarSwitcherState state = mSwitcherViewModel.getState();
                    if (state != null) {
                        ToolbarSwitcherItem selectedToolbar = state.getSelectedToolbar();

                        AnalyticsHandlerAdapter.getInstance().sendEvent(
                                AnalyticsHandlerAdapter.EVENT_FAV_TOOLBAR_TOOL_COUNT,
                                AnalyticsParam.favToolbarToolCountParams(selectedToolbar)
                        );

                        AnalyticsHandlerAdapter.getInstance().sendEvent(
                                AnalyticsHandlerAdapter.EVENT_FAV_TOOLBAR_TOOL_DUPLICATE,
                                AnalyticsParam.favToolbarDuplicateToolCountParams(selectedToolbar)
                        );
                    }
                }
            }
        });
    }

    private void updateToolbarItemsInDb(@NonNull ArrayList<MenuEditorItem> menuEditorItems, boolean shouldReplace) {
        Context context = getActivity();
        ToolbarSwitcherState state = mSwitcherViewModel.getState();
        if (context != null && state != null) {
            // Update the toolbar
            ToolbarSwitcherItem selectedToolbar = state.getSelectedToolbar();
            String toolbarTag = selectedToolbar.getTag();
            String toolbarName = selectedToolbar.getToolbarName(context);
            mDisposables.add(
                    ToolbarSharedPreferences.updateToolbarItemsInDb
                                    (
                                            context,
                                            toolbarTag,
                                            toolbarName,
                                            menuEditorItems,
                                            shouldReplace
                                    )
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<Boolean>() {
                                @Override
                                public void accept(Boolean successful) throws Exception {
                                    if (successful) {
                                        // Update menu item states
                                        updateAllMenuItems();
                                    }
                                }
                            })
            );
        }
    }

    public void onOpenTabSwitcher() {
        if (null == mTabLayout) {
            return;
        }

        ArrayList<TabSwitcherItem> items = new ArrayList<>();
        int count = mTabLayout.getTabCount();
        for (int i = 0; i < count; i++) {
            TabLayout.Tab tab = mTabLayout.getTabAt(i);
            if (tab != null && tab.getTag() instanceof String) {
                String tag = (String) tab.getTag();
                if (tab.getCustomView() != null) {
                    TextView textView = tab.getCustomView().findViewById(R.id.tab_pdfviewctrl_text);
                    String title = textView.getText().toString();
                    String preview = null;
                    if (mViewerConfig == null || !mViewerConfig.isUseStandardLibrary()) {
                        preview = getTabThumbnail(tag);
                    }
                    items.add(new TabSwitcherItem(tag, title, preview));
                }
            }
        }

        mTabSwitcherViewModel = ViewModelProviders.of(this).get(TabSwitcherViewModel.class);
        mTabSwitcherViewModel.setItems(items);
        mTabSwitcherViewModel.setSelectedTag(mTabLayout.getCurrentTabTag());

        mDisposables.add(mTabSwitcherViewModel.getObservable()
                .subscribe(new Consumer<TabSwitcherEvent>() {
                    @Override
                    public void accept(TabSwitcherEvent tabSwitcherEvent) throws Exception {
                        if (tabSwitcherEvent.getEventType() == TabSwitcherEvent.Type.CLOSE_TAB) {
                            String tabTag = tabSwitcherEvent.getTabTag();
                            if (!Utils.isNullOrEmpty(tabTag)) {
                                closeTab(tabTag);
                            }
                        } else if (tabSwitcherEvent.getEventType() == TabSwitcherEvent.Type.SELECT_TAB) {
                            String tabTag = tabSwitcherEvent.getTabTag();
                            if (!Utils.isNullOrEmpty(tabTag)) {
                                if (mTabLayout != null) {
                                    TabLayout.Tab tab = mTabLayout.getTabByTag(tabTag);
                                    if (tab != null) {
                                        tab.select();
                                    }
                                }
                            }
                        }
                    }
                })
        );

        TabSwitcherDialogFragment fragment = createTabSwitcherDialog();
        fragment.setStyle(DialogFragment.STYLE_NORMAL, mThemeProvider.getTheme());
        fragment.show(getChildFragmentManager(), TabSwitcherDialogFragment.TAG);
    }

    /**
     * Returns the thumbnail path of a particular tab in the tab switcher
     */
    protected String getTabThumbnail(String tag) {
        return RecentlyUsedCache.getBitmapPathIfExists(tag);
    }

    protected TabSwitcherDialogFragment createTabSwitcherDialog() {
        return TabSwitcherDialogFragment.newInstance(mTabLayout.getCurrentTabTag());
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabFragment2.TabListener#onTabSingleTapConfirmed()}.
     */
    @Override
    public void onTabSingleTapConfirmed() {
        final PdfViewCtrlTabFragment2 currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }
        if (mViewerConfig != null && mViewerConfig.isPermanentToolbars()) {
            return;
        }

        if (!currentFragment.isAnnotationMode() && !mIsSearchMode) {
            if (mAppBarLayout.getVisibility() == View.VISIBLE || mBottomBarContainer.getVisibility() == View.VISIBLE) {
                hideUI();
            } else {
                showUI();
            }
        }
    }

    @SuppressLint("RestrictedApi")
    protected void initViews() {
        super.initViews();
        final FragmentActivity activity = getActivity();
        if (activity == null || mFragmentView == null) {
            return;
        }

        ViewerShortcutViewModel shortcutViewModel = ViewModelProviders.of(activity)
                .get(ViewerShortcutViewModel.class);
        shortcutViewModel.observeKeyboardEvents(this, new Observer<ViewerShortcutViewModel.KeyboardShortcut>() {
            @Override
            public void onChanged(ViewerShortcutViewModel.KeyboardShortcut keyboardShortcut) {
                if (ShortcutHelper.isEnabled()) {
                    PdfViewCtrlTabFragment2 currentFragment = getCurrentPdfViewCtrlFragment();
                    if (currentFragment != null) {
                        int keyCode = keyboardShortcut.getKeyCode();
                        KeyEvent event = keyboardShortcut.getEvent();
                        if (ShortcutHelper.isCloseMenu(keyCode, event)) { // if close shortcut, toggle view toolbar
                            showUI();
                            openToolbarWithTag(DefaultToolbars.TAG_VIEW_TOOLBAR);
                        } else if (ShortcutHelper.isCancelTool(keyCode, event)) { // if cancel tool shortcut, toggle pan tool
                            showUI();
                            selectToolbarButton(-1);
                        } else { // else check if it is tool shortcut, if so select toolbar button
                            DefaultToolbars.ButtonId buttonId = ShortcutHelper.getButtonId(
                                    currentFragment.getToolManager(),
                                    keyCode,
                                    event);

                            // If a toolbar button id is found, then select the button.
                            if (buttonId != null) {
                                showUI();
                                selectToolbarButtonInToolbars(buttonId.value());
                            }
                        }
                    }
                }
            }
        });
        inflateToolbarState(activity);

        mTabLayout.setOnTabModificationListener(new CustomFragmentTabLayout.OnTabModificationListener() {
            @Override
            public void onTabAdded(TabLayout.Tab tab) {
                if (mTabLayout != null && mTabActionView != null) {
                    mTabActionView.setTabCount(mTabLayout.getTabCount());
                }
            }

            @Override
            public void onTabRemoved(TabLayout.Tab tab) {
                if (mTabLayout != null && mTabActionView != null) {
                    mTabActionView.setTabCount(mTabLayout.getTabCount());
                }
            }
        });

        mSwitcherButton = mToolbar.findViewById(R.id.switcher_button);
        mSwitcherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleToolSwitcherClicked(v);
            }
        });
        mSwitcherCompactButton = mToolbar.findViewById(R.id.switcher_compact_button);
        mSwitcherCompactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleToolSwitcherClicked(v);
            }
        });
        if (!mIsShowAnnotationToolbarOption) {
            mSwitcherButton.setVisibility(View.GONE);
        } else if (useCompactViewer()) {
            mSwitcherButton.setVisibility(View.GONE);
            if (mIsShowToolbarSwitcher) {
                mSwitcherCompactButton.setVisibility(View.VISIBLE);
            } else {
                mSwitcherCompactButton.setVisibility(View.GONE);
            }
            mToolbar.setContentInsetsRelative(0, 0);
            mToolbar.setContentInsetStartWithNavigation(0);
        } else {
            if (mIsShowToolbarSwitcher) {
                mSwitcherButton.setVisibility(View.VISIBLE);
            } else {
                mSwitcherButton.setVisibility(View.GONE);
            }
            mSwitcherCompactButton.setVisibility(View.GONE);
        }

        boolean useCompact = useCompactViewer();

        FrameLayout annotationToolbarContainer = mFragmentView.findViewById(R.id.toolbar_container);
        FrameLayout presetsContainer = mFragmentView.findViewById(R.id.presets_container);
        presetsContainer.setVisibility(mHidePresetBar || useTabletLayout() ? View.GONE : View.VISIBLE);
        int[] presetBarsToHideIds = mViewerConfig != null ? mViewerConfig.getPresetBarsToHide() : null;
        HashSet<ToolbarButtonType> presetBarsToHide = new HashSet<>();
        if (presetBarsToHideIds != null) {
            for (int presetBarsToHideId : presetBarsToHideIds) {
                presetBarsToHide.add(ToolbarButtonType.valueOf(presetBarsToHideId));
            }
        }
        assert getFragmentManager() != null;
        mAnnotationToolbarComponent =
                new TabletAnnotationToolbarComponent(
                        this,
                        getFragmentManager(),
                        mAnnotationToolbarViewModel,
                        mPresetViewModel,
                        mToolManagerViewModel,
                        mSignatureViewModel,
                        new AnnotationToolbarView(annotationToolbarContainer),
                        useTabletLayout(),
                        presetBarsToHide,
                        mHidePresetBar
                );
        mPresetBarComponent = new PresetBarComponent(
                this,
                getFragmentManager(),
                mPresetViewModel,
                mToolManagerViewModel,
                mSignatureViewModel,
                new PresetBarView(presetsContainer),
                presetBarsToHide
        );
        mPresetBarComponent.setCompactMode(useCompact);

        mAnnotationToolbarComponent.setCompactMode(useCompact);
        if (useCompact) {
            mAnnotationToolbarComponent.setNavigationIcon(mToolbarNavRes);
            boolean navIconVisible = true;
            if (Utils.isLargeScreenWidth(activity) && null == mViewerConfig) {
                navIconVisible = false;
            } else if (mToolbarNavRes == 0) {
                navIconVisible = false;
            }
            mAnnotationToolbarComponent.setNavigationIconVisible(navIconVisible);
            mAnnotationToolbarComponent.setToolbarSwitcherVisible(mViewerConfig == null || mViewerConfig.isShowToolbarSwitcher());
            // in compact mode, we want to match the annotation toolbar with the top toolbar
            mToolbar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int width = -1;
            int childCount = mToolbar.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View v = mToolbar.getChildAt(i);
                if (v.getContentDescription() != null) {
                    if (v.getContentDescription().toString().equals(activity.getResources().getString(R.string.abc_action_bar_up_description))) {
                        width = v.getMeasuredWidth();
                        break;
                    }
                }
            }
            mAnnotationToolbarComponent.setNavigationIconProperty(mToolbar.getPaddingLeft(), width);
            mAnnotationToolbarComponent.setToolbarHeight(mToolbar.getMeasuredHeight());
        }
        mAnnotationToolbarComponent.addButtonClickListener(new AnnotationToolbarComponent.AnnotationButtonClickListener() {
            @Override
            public boolean onInterceptItemClick(@Nullable ToolbarItem toolbarItem, MenuItem item) {
                if (toolbarItem != null) {
                    HashMap<String, String> metadata = new HashMap<>();
                    metadata.put(EventHandler.ANNOT_TOOLBAR_BUTTON_TYPE_METADATA_KEY, String.valueOf(toolbarItem.toolbarButtonType.getValue()));
                    metadata.put(EventHandler.TOOLBAR_METADATA_KEY, String.valueOf(toolbarItem.toolbarId));
                    return EventHandler.sendPreEvent(
                            EventHandler.ANNOT_TOOLBAR_TOOL_EVENT,
                            metadata
                    );
                }
                // ignore
                return false;
            }

            @Override
            public void onPreItemClick(@Nullable ToolbarItem toolbarItem, MenuItem item) {
                ToolbarButtonType buttonType = toolbarItem != null ? toolbarItem.toolbarButtonType : null;
                PdfViewCtrlTabFragment2 currentFragment = getCurrentPdfViewCtrlFragment();
                ToolManager toolManager = currentFragment != null ? currentFragment.getToolManager() : null;
                if (buttonType == ToolbarButtonType.ADD_PAGE) {
                    if (!checkTabConversionAndAlert(R.string.cant_edit_while_converting_message, false)) {
                        if (toolManager != null) {
                            // back to pan
                            toolManager.setTool(toolManager.createTool(ToolManager.ToolMode.PAN, toolManager.getTool()));
                        }
                        addNewPage();
                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_EDIT_PAGES_ADD);
                    }
                } else if (buttonType == ToolbarButtonType.PAGE_REDACTION) {
                    if (!checkTabConversionAndAlert(R.string.cant_edit_while_converting_message, false)) {
                        if (toolManager != null) {
                            // back to pan
                            toolManager.setTool(toolManager.createTool(ToolManager.ToolMode.PAN, toolManager.getTool()));
                            toolManager.getRedactionManager().openPageRedactionDialog();
                        }
                    }
                } else if (buttonType == ToolbarButtonType.SEARCH_REDACTION) {
                    if (!checkTabConversionAndAlert(R.string.cant_edit_while_converting_message, false)) {
                        if (toolManager != null) {
                            // back to pan
                            toolManager.setTool(toolManager.createTool(ToolManager.ToolMode.PAN, toolManager.getTool()));
                            toolManager.getRedactionManager().openRedactionBySearchDialog();
                        }
                    }
                } else {
                    if (item != null && !onOptionsItemSelected(item)) {
                        // Check tab conversion and shows the alert if needed. Do not need to show dialog for customize toolbar button
                        if (item.getItemId() != DefaultToolbars.ButtonId.CUSTOMIZE.value() &&
                                checkTabConversionAndAlert(R.string.cant_edit_while_converting_message, false)) {
                        }
                    }
                }
            }

            @Override
            public void onPostItemClick(@Nullable ToolbarItem toolbarItem, MenuItem item) {
                PdfViewCtrlTabFragment2 currentFragment = getCurrentPdfViewCtrlFragment();
                if (currentFragment == null) {
                    return;
                }
                int id = item.getItemId();
                if (id == DefaultToolbars.ButtonId.UNDO.value() || id == DefaultToolbars.ButtonId.REDO.value()) {
                    // Undo/redo action is handled in AnnotationToolbarComponent, but we still need to refresh
                    // Page count of page indicator
                    currentFragment.refreshPageCount();
                }
            }
        });

        if (!mOnToolbarChangedListeners.isEmpty()) {
            for (OnToolbarChangedListener listener : mOnToolbarChangedListeners) {
                mAnnotationToolbarComponent.addOnToolbarChangedListener(listener);
            }
            mOnToolbarChangedListeners.clear();
        }

        if (!mOnPreBuildToolbarListeners.isEmpty()) {
            for (OnPreBuildToolbarListener listener : mOnPreBuildToolbarListeners) {
                mAnnotationToolbarComponent.addOnPreBuildToolbarListener(listener);
            }
            mOnPreBuildToolbarListeners.clear();
        }

        // Set whether to remember last used tool for each toolbar
        if (mViewerConfig != null) {
            mAnnotationToolbarComponent.rememberLastUsedTool(mViewerConfig.isRememberLastUsedTool());
        }

        // Filter for disabled tool modes
        if (mViewerConfig != null && mViewerConfig.getToolManagerBuilder() != null) {
            Set<ToolMode> disabledToolModes = mViewerConfig.getToolManagerBuilder().getDisabledToolModes(activity);
            mAnnotationToolbarComponent.setToolModeFilter(disabledToolModes);

            if (!mViewerConfig.getToolManagerBuilder().getShowUndoRedo()) {
                mAnnotationToolbarComponent.setToolbarButtonVisibility(ToolbarButtonType.UNDO, false);
                mAnnotationToolbarComponent.setToolbarButtonVisibility(ToolbarButtonType.REDO, false);
            }
        }
        // Initialize bottom toolbar
        FrameLayout bottomNavContainer = mFragmentView.findViewById(R.id.bottom_nav_container);
        if (useTabletLayout() || !mBottomBarVisible) {
            bottomNavContainer.setVisibility(View.GONE);
        }

        mBottomBarContainer = mFragmentView.findViewById(R.id.bottom_container);
        mBottomBarShadow = mFragmentView.findViewById(R.id.bottom_bar_shadow);
        mBottomNavComponent = new BottomBarComponent(this, bottomNavContainer);

        // If we are hiding the app bar, then we should move the search toolbar to the bottom
        if (mViewerConfig != null && !mViewerConfig.isShowAppBar()) {
            ((ViewGroup) mSearchToolbar.getParent()).removeView(mSearchToolbar);
            mBottomBarContainer.addView(mSearchToolbar);
        }

        mBottomNavComponent.inflateWithBuilder(getBottomNavBuilder());

        mBottomNavComponent.addOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onOptionsItemSelected(item);
            }
        });

        // Initialize top and bottom toolbar
        // do this last so it will init both top and bottom components
        onCreateOptionsMenu(mToolbar.getMenu(), new MenuInflater(activity));

        if (isQuickBookmarkCreationEnabled() && mBookmarkButtonViewModel != null) {
            mBookmarkButtonViewModel.observe(this, new Observer<BookmarkButtonState>() {
                @Override
                public void onChanged(BookmarkButtonState buttonState) {
                    if (buttonState != null) {
                        updateBookmarkIcon(buttonState);
                    }
                }
            });
        }
    }

    /**
     * Returns annotation toolbar builder
     */
    protected AnnotationToolbarBuilder getBottomNavBuilder() {
        AnnotationToolbarBuilder bottomNavBuilder;
        // If custom bottom bar exists then we use it, otherwise use out default bottom bar
        if (mHasCustomBottomBars && mViewerConfig.getBottomBarBuilder() != null) {
            bottomNavBuilder = mViewerConfig.getBottomBarBuilder().getBuilder();
        } else {
            bottomNavBuilder = AnnotationToolbarBuilder.withTag("BottomNav")
                    .addCustomButton(R.string.pref_viewmode_thumbnails, R.drawable.ic_thumbnails_grid_black_24dp, R.id.action_thumbnails)
                    .addCustomButton(R.string.action_search, R.drawable.ic_search_white_24dp, R.id.action_search)
                    .addCustomSelectableButton(R.string.pref_viewmode_reflow, R.drawable.ic_view_mode_reflow_black_24dp, R.id.action_reflow_mode);
            if (isQuickBookmarkCreationEnabled()) {
                bottomNavBuilder = bottomNavBuilder.addCustomSelectableButton(R.string.action_add_bookmark, R.drawable.ic_bookmarks_white_24dp, R.id.action_bookmark_add);
            }
            if (canOpenNavigationListAsSideSheet()) {
                bottomNavBuilder.addCustomSelectableButton(R.string.action_outline, R.drawable.ic_outline_white_24dp, R.id.action_outline);
            } else {
                bottomNavBuilder.addCustomButton(R.string.action_outline, R.drawable.ic_outline_white_24dp, R.id.action_outline);
            }
            if (useCompactViewer()) {
                bottomNavBuilder.addCustomButton(R.string.more, R.drawable.ic_overflow_white_24dp, R.id.action_overflow);
            }
        }
        return bottomNavBuilder;
    }

    /**
     * Updates the bookmark button state
     */
    private void updateBookmarkIcon(@NonNull BookmarkButtonState buttonState) {
        if (getActivity() == null) {
            return;
        }
        boolean isSelected = buttonState.isSelected();
        int bookmarkId = R.id.action_bookmark_add;
        Drawable drawable = isSelected ?
                ResourcesCompat.getDrawable(getResources(), R.drawable.ic_bookmarks_filled, null) :
                ResourcesCompat.getDrawable(getResources(), R.drawable.ic_bookmarks_white_24dp, null);

        // Update bottom bar button appearance
        mBottomNavComponent.setItemSelected(isSelected, bookmarkId);
        mBottomNavComponent.setShowBackground(bookmarkId, false);
        if (drawable != null) {
            mBottomNavComponent.setItemIcon(bookmarkId, drawable);
        }

        // Update toolbar menu item appearance
        Context context = getActivity();
        MenuItem menuBookmark = getToolbarOptionMenuItem(R.id.action_bookmark_add);
        MenuItem menuBookmark2 = getViewOverflowOptionMenuItem(R.id.action_bookmark_add);
        if (menuBookmark != null) {
            menuBookmark.setIcon(isSelected ? R.drawable.ic_bookmarks_filled : R.drawable.ic_bookmarks_white_24dp);
            if (isSelected) {
                menuBookmark.setIcon(Utils.tintDrawable(context, menuBookmark.getIcon()));
            }
            menuBookmark.setTitle(isSelected ? R.string.action_remove_bookmark : R.string.action_add_bookmark);
        }
        if (menuBookmark2 != null) {
            menuBookmark2.setIcon(isSelected ? R.drawable.ic_bookmarks_filled : R.drawable.ic_bookmarks_white_24dp);
            if (isSelected) {
                menuBookmark2.setIcon(Utils.tintDrawable(context, menuBookmark2.getIcon()));
            }
            menuBookmark2.setTitle(isSelected ? R.string.action_remove_bookmark : R.string.action_add_bookmark);
        }
    }

    protected void showViewOverflowMenu(final View v) {
        if (mToolbarMenuResArray == null) {
            return;
        }
        if (mViewOverflowMenu == null) {
            mViewOverflowMenu = new PopupMenu(v.getContext(), v);
            for (int res : mToolbarMenuResArray) {
                mViewOverflowMenu.inflate(res);
            }
            mViewOverflowMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem menuItem) {
                    return onOptionsItemSelected(menuItem);
                }
            });
            setOptionsMenuVisible(true);
        }
        MenuItem tabs = mViewOverflowMenu.getMenu().findItem(R.id.action_tabs);
        if (tabs != null) {
            // based on current design, we only show tabs in View toolbar for phone portrait
            // for tablet, tabs is always in the overflow so it's ok to show
            tabs.setVisible(useTabletLayout());
        }
        onPrepareOptionsMenu(mViewOverflowMenu.getMenu());

        if (mViewOverflowMenu.getMenu() instanceof MenuBuilder) {
            // reset anchor
            MenuPopupHelper menuHelper = new MenuPopupHelper(v.getContext(), (MenuBuilder) mViewOverflowMenu.getMenu(), v);
            menuHelper.show();
        } else {
            mViewOverflowMenu.show();
        }
    }

    /**
     * Shows toolbar switcher when appropriate
     *
     * @param v the anchor view
     */
    public void handleToolSwitcherClicked(View v) {
        PdfViewCtrlTabFragment2 currentPdfViewCtrlFragment = getCurrentPdfViewCtrlFragment();
        if (currentPdfViewCtrlFragment != null) {
            currentPdfViewCtrlFragment.localFileWriteAccessCheck();
            if (tabNeedsReadOnlyCheck()) {
                currentPdfViewCtrlFragment.handleSpecialFile();
            } else if (!currentPdfViewCtrlFragment.isReflowMode()) {
                showToolbarSwitcherDialog(v);
            }
        }
    }

    /**
     * Removes the specified tab.
     *
     * @param filepath           The file path
     * @param nextTabTagToSelect The tab tag of the tab that should be selected thereafter
     */
    public void removeTab(String filepath, final String nextTabTagToSelect) {
        super.removeTab(filepath, nextTabTagToSelect);

        // update selected tab for tab switcher
        if (mTabSwitcherViewModel != null) {
            mTabSwitcherViewModel.setSelectedTag(nextTabTagToSelect);
        }
    }

    /**
     * @hide
     **/
    @Override
    public void onTabSelected(TabLayout.Tab tab) {

        Activity activity = getActivity();
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            // the document has not yet been ready, wait until it will be ready which will be notified
            // by TabListener.onTabDocumentLoaded
            return;
        }

        // Needs to be called before calling super, as mCurTabIndex is set in super so we need
        // to get old value before it is set to new value
        if (mCurTabIndex != -1 && mCurTabIndex != tab.getPosition()) {
            mAnnotationToolbarComponent.clearState();
            setToolManagerForViewModel();
        }
        super.onTabSelected(tab);

        attachBookmarkButtonData(currentFragment);
    }

    /**
     * @hide
     **/
    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        mAnnotationToolbarComponent.clearState();
        // Clear listener
        ToolManager oldToolManager = mToolManagerViewModel.getToolManager();
        if (oldToolManager != null) {
            oldToolManager.removeToolManagerChangedListener(mListener);
        }
        // Add new listener
        mToolManagerViewModel.setToolManager(null);
        super.onTabUnselected(tab);
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        super.onTabReselected(tab);
    }

    @Override
    public void onSearchOptionSelected() {
        Activity activity = getActivity();
        PdfViewCtrlTabFragment2 currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null || !currentFragment.isDocumentReady()) {
            return;
        }

        if (currentFragment.isReflowMode()) {
            int messageID = R.string.reflow_disable_search_clicked;
            CommonToast.showText(activity, messageID);
            return;
        }

        if (checkTabConversionAndAlert(R.string.cant_search_while_converting_message, true)) {
            return;
        }

        if (mSearchToolbar == null || mToolbar == null) {
            return;
        }
        if (currentFragment.isDocumentReady()) {
            startSearchMode();
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_SEARCH);
        }
    }

    @Override
    protected void openBookmarksDialog() {
        final PdfViewCtrlTabFragment2 currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }
        if (canOpenNavigationListAsSideSheet()) {
            currentFragment.openNavigationList(mBookmarksDialog);
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
        final PdfViewCtrlTabFragment2 currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }
        currentFragment.openRedactionSearchList(dialogFragment);
    }

    protected void updateTabLayout() {
        Activity activity = getActivity();
        if (activity == null || mTabLayout == null) {
            return;
        }

        if (isInFullScreenMode()) {
            boolean requiresUpdate = false;
            if (PdfViewCtrlSettingsManager.getShowStatusBarForNewUI(activity) || mShouldShowStatusBar ||
                    PdfViewCtrlSettingsManager.getShowNavigationBarForNewUI(activity)) {
                mAppBarLayout.setFitsSystemWindows(true);
                requiresUpdate = true;
            }
            if (PdfViewCtrlSettingsManager.getShowNavigationBarForNewUI(activity)) {
                mFragmentView.findViewById(R.id.bottom_container).setFitsSystemWindows(true);
                mFragmentView.findViewById(R.id.bottom_nav_container).setFitsSystemWindows(true);
                mFragmentView.findViewById(R.id.ignore_top_inset_preset_container).setFitsSystemWindows(true);
                mFragmentView.findViewById(R.id.presets_container).setFitsSystemWindows(true);
                requiresUpdate = true;
            }
            if (requiresUpdate) {
                ViewCompat.requestApplyInsets(mFragmentView);
            }
        } else {
            mRootView.setFitsSystemWindows(false);
            mAppBarLayout.setFitsSystemWindows(false);
            mFragmentContainer.setFitsSystemWindows(false);
            ViewCompat.requestApplyInsets(mFragmentView);
        }

        super.updateTabLayout();
    }

    private void updateAllMenuItems() {
        // Also update some menu item states
        updateIconsInReflowMode();
        updateUndoRedoState();
        updateAttachmentState();
        updateLayersState();
        updateDigitalSignaturesState();
    }

    /**
     * Returns whether we should check read only state for the current viewer fragment tab.
     */
    private boolean tabNeedsReadOnlyCheck() {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment != null) {
            ToolManager toolManager = currentFragment.getToolManager();
            return currentFragment.isTabReadOnly() && !(toolManager != null && toolManager.skipReadOnlyCheck());
        } else {
            return true;
        }
    }

    /**
     * Updates the icons (enable/disable) when reflow mode has been changed.
     */
    @Override
    protected void updateIconsInReflowMode() {
        final PdfViewCtrlTabFragment2 currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }

        // First we toggle the reflow button
        mBottomNavComponent.setItemSelected(currentFragment.isReflowMode(), R.id.action_reflow_mode);

        MenuItem menuReflowMode = getToolbarOptionMenuItem(R.id.action_reflow_mode);
        MenuItem menuSearch = getToolbarOptionMenuItem(R.id.action_search);
        MenuItem menuReflowMode2 = getViewOverflowOptionMenuItem(R.id.action_reflow_mode);
        MenuItem menuSearch2 = getViewOverflowOptionMenuItem(R.id.action_search);

        // Then gray out the other buttons related to editing
        if (currentFragment.isReflowMode()) {
            if (mBottomNavComponent != null) {
                mBottomNavComponent.setItemEnabled(false, R.id.action_search);
            }
            if (menuReflowMode != null) {
                menuReflowMode.setChecked(true);
            }
            if (menuSearch != null) {
                if (menuSearch.getIcon() != null) {
                    int alpha = getResources().getInteger(R.integer.reflow_disabled_button_alpha);
                    menuSearch.getIcon().setAlpha(alpha);
                }
                menuSearch.setEnabled(false);
            }
            if (menuReflowMode2 != null) {
                menuReflowMode2.setChecked(true);
            }
            if (menuSearch2 != null) {
                if (menuSearch2.getIcon() != null) {
                    int alpha = getResources().getInteger(R.integer.reflow_disabled_button_alpha);
                    menuSearch2.getIcon().setAlpha(alpha);
                }
                menuSearch2.setEnabled(false);
            }
            if (mMenuUndo != null) {
                mMenuUndo.setVisible(false);
            }
        } else {
            if (mBottomNavComponent != null) {
                mBottomNavComponent.setItemEnabled(true, R.id.action_search);
            }
            if (menuReflowMode != null) {
                menuReflowMode.setChecked(false);
            }
            if (menuReflowMode2 != null) {
                menuReflowMode2.setChecked(false);
            }
            if (menuSearch != null) {
                menuSearch.setChecked(false);
            }
            if (menuSearch != null) {
                if (menuSearch.getIcon() != null) {
                    menuSearch.getIcon().setAlpha(255);
                }
                menuSearch.setEnabled(true);
            }
            if (menuSearch2 != null) {
                menuSearch2.setChecked(false);
            }
            if (menuSearch2 != null) {
                if (menuSearch2.getIcon() != null) {
                    menuSearch2.getIcon().setAlpha(255);
                }
                menuSearch2.setEnabled(true);
            }
            updateUndoButtonVisibility(true);
        }
    }

    @Override
    protected void updateUndoRedoState() {
        final PdfViewCtrlTabFragment2 currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }
        if (mAnnotationToolbarComponent == null) {
            return;
        }
        // Update undo/redo menu items for current mode.
        ToolManager toolManager = currentFragment.getToolManager();
        UndoRedoManager undoRedoManager = (toolManager != null) ? toolManager.getUndoRedoManger() : null;
        // Undo/Redo can be shown when in "normal" viewing mode.
        if (!currentFragment.isReflowMode() && !mIsSearchMode && undoRedoManager != null) {
            if (undoRedoManager.canUndo() && toolManager.isShowUndoRedo()) {
                mAnnotationToolbarComponent.setItemEnabled(DefaultToolbars.ButtonId.UNDO.value(), true);
                if (mUndoActionButton != null) {
                    mUndoActionButton.enable();
                }
            } else {
                mAnnotationToolbarComponent.setItemEnabled(DefaultToolbars.ButtonId.UNDO.value(), false);
                if (mUndoActionButton != null) {
                    mUndoActionButton.disable();
                }
            }

            if (undoRedoManager.canRedo() && toolManager.isShowUndoRedo()) {
                mAnnotationToolbarComponent.setItemEnabled(DefaultToolbars.ButtonId.REDO.value(), true);
            } else {
                mAnnotationToolbarComponent.setItemEnabled(DefaultToolbars.ButtonId.REDO.value(), false);
            }
        } else {
            mAnnotationToolbarComponent.setItemEnabled(DefaultToolbars.ButtonId.UNDO.value(), false);
            mAnnotationToolbarComponent.setItemEnabled(DefaultToolbars.ButtonId.REDO.value(), false);
            if (mUndoActionButton != null) {
                mUndoActionButton.disable();
            }
        }
    }

    @Override
    public void setViewerOverlayUIVisible(boolean visible) {
        if (!visible) {
            // only hide toolbars in View toolbar
            ToolbarSwitcherState state = mSwitcherViewModel.getState();
            if (state != null && !state.getSelectedToolbar().getTag().equals(DefaultToolbars.TAG_VIEW_TOOLBAR)) {
                return;
            }
        }
        super.setViewerOverlayUIVisible(visible);
    }

    /**
     * Shows the UI.
     */
    // TODO: Rename.
    @Override
    public void showUI() {
        Activity activity = getActivity();
        final PdfViewCtrlTabFragment2 currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return;
        }

        final boolean canShowToolbars = currentFragment.onShowToolbar();
        final boolean canExitFullscreenMode = currentFragment.onExitFullscreenMode();
        final boolean isBarsVisible = mBottomBarContainer.getVisibility() == View.VISIBLE ||
                mAppBarLayout.getVisibility() == View.VISIBLE;

        // Toolbars can only be shown if fullscreen mode will be exited.
        if (!isBarsVisible && canShowToolbars && canExitFullscreenMode) {
            setToolbarsVisible(true);
        }

        if (canExitFullscreenMode) {
            showSystemUI();
        }
    }

    /**
     * Hides the UI.
     */
    // TODO: Rename.
    @Override
    public void hideUI() {
        Activity activity = getActivity();
        final PdfViewCtrlTabFragment2 currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return;
        }

        final boolean canHideToolbars = currentFragment.onHideToolbars();
        final boolean canEnterFullscreenMode = currentFragment.onEnterFullscreenMode();
        final boolean isBarsVisible = mBottomBarContainer.getVisibility() == View.VISIBLE ||
                mAppBarLayout.getVisibility() == View.VISIBLE;

        if (isBarsVisible && canHideToolbars) {
            setToolbarsVisible(false);
        }

        // Fullscreen mode only be entered if the toolbars will hide or if they are not visible.
        if ((isBarsVisible && canHideToolbars && canEnterFullscreenMode)
                || (!isBarsVisible && canEnterFullscreenMode)) {
            hideSystemUI();
        }
    }

    /**
     * Handles changing the visibility of toolbars.
     *
     * @param visible          True if toolbar is visible
     * @param animateBottomBar True if visibility should be changed with animation
     */
    @Override
    public void setToolbarsVisible(boolean visible, boolean animateBottomBar) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        final PdfViewCtrlTabFragment2 currentFragment = getCurrentPdfViewCtrlFragment();
        boolean isAnnotationMode = currentFragment != null && currentFragment.isAnnotationMode();
        if (isAnnotationMode || mIsSearchMode) {
            // Do nothing if in annotation or search mode.
            return;
        }

        if (visible) {
            resetHideToolbarsTimer();
        } else {
            stopHideToolbarsTimer();
        }
        if (visible || mAutoHideEnabled) {
            animateToolbars(visible);
        }
        setThumbSliderVisibility(visible, animateBottomBar);

        // Finally deselect any tools if toolbar becomes invisible, otherwise will get stuck in
        // full screen state.
        if (!visible) {
            mAnnotationToolbarComponent.clearState();
        }
    }

    @Override
    protected boolean canShowTabLayout() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return false;
        }
        if (!mMultiTabModeEnabled) {
            return false;
        }
        if (Utils.isTablet(getContext())) {
            return PdfViewCtrlSettingsManager.getShowTabBarForNewUI(activity);
        } else {
            return PdfViewCtrlSettingsManager.getShowTabBarForPhone(activity);
        }
    }

    /**
     * Shows the system status bar.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected void showSystemStatusBar() {
        Activity activity = getActivity();
        final PdfViewCtrlTabFragment2 currentFragment = getCurrentPdfViewCtrlFragment();
        View view = getView();
        if (activity == null || currentFragment == null || view == null) {
            return;
        }
        if (isInFullScreenMode() && (PdfViewCtrlSettingsManager.getShowStatusBarForNewUI(activity) || mShouldShowStatusBar)) {
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
        if (activity == null || activity.getWindow() == null) {
            return;
        }
        View view = activity.getWindow().getDecorView();

        if (isInFullScreenMode()) {
            int oldFlags = view.getSystemUiVisibility();
            int newFlags = oldFlags;

            // Remove the fullscreen system UI flags.
            if (PdfViewCtrlSettingsManager.getShowStatusBarForNewUI(activity) || mShouldShowStatusBar) {
                if (PdfViewCtrlSettingsManager.getShowNavigationBarForNewUI(activity)) {
                    newFlags &= ~(View.SYSTEM_UI_FLAG_FULLSCREEN |  // show status bar
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // show nav bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                } else {
                    newFlags &= ~(View.SYSTEM_UI_FLAG_FULLSCREEN // show status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
            } else {
                if (PdfViewCtrlSettingsManager.getShowNavigationBarForNewUI(activity)) {
                    newFlags &= ~(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // show nav bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                } else {
                    newFlags &= ~(View.SYSTEM_UI_FLAG_IMMERSIVE
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
            }

            if (!PdfViewCtrlSettingsManager.getShowNavigationBarForNewUI(activity)) {
                // Add the system UI flags to hide the navigation bar.
                // (Sticky immersion means the navigation bar can be "peeked").
                newFlags |= (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }

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
        if (activity == null || activity.getWindow() == null) {
            return;
        }
        View view = activity.getWindow().getDecorView();

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
        if (activity == null || activity.getWindow() == null || mAppBarLayout == null) {
            return;
        }
        View view = activity.getWindow().getDecorView();

        if (Utils.isKitKat()) {
            int oldRootFlags = view.getSystemUiVisibility();
            int newRootFlags = oldRootFlags;

            int oldAppBarFlags = mAppBarLayout.getSystemUiVisibility();
            int newAppBarFlags = oldAppBarFlags;

            if (PdfViewCtrlSettingsManager.getFullScreenMode(activity)) {
                // Add the fullscreen system UI layout flags.
                newRootFlags |= (View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

                // Add the stable layout flag.
                newAppBarFlags |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            } else {
                // Remove the fullscreen system UI layout flags.
                newRootFlags &= ~(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

                // Remove the stable layout flag.
                newAppBarFlags &= ~View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            }

            view.setSystemUiVisibility(newRootFlags);
//            mAppBarLayout.setSystemUiVisibility(newAppBarFlags);

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
        super.startSearchMode();
    }

    /**
     * Exits the search mode.
     */
    public void exitSearchMode() {
        super.exitSearchMode();
    }

    @Override
    protected void showSearchToolbarTransition() {
        TransitionSet transition = new TransitionSet();
        transition.addTransition(new ChangeBounds());
        transition.addTransition(new Fade());
        transition.setDuration(100);
        TransitionManager.beginDelayedTransition(mAppBarLayout, transition);
        if (mAppBarLayout != null && mToolbar != null && mSearchToolbar != null) {
            mToolbar.setVisibility(View.GONE);
            mSearchToolbar.setVisibility(View.VISIBLE);
        }
        mAnnotationToolbarComponent.hide(false);
    }

    @Override
    protected void hideSearchToolbarTransition() {
        TransitionSet transition = new TransitionSet();
        transition.addTransition(new ChangeBounds());
        transition.addTransition(new Fade());
        transition.setDuration(100);
        TransitionManager.beginDelayedTransition(mAppBarLayout, transition);
        if (mAppBarLayout != null && mToolbar != null && mSearchToolbar != null) {
            showTopToolbar();
            mSearchToolbar.setVisibility(View.GONE);
        }
        if (mSwitcherViewModel.getState() != null &&
                !mSwitcherViewModel.getState().getSelectedToolbar().getTag().equals(DefaultToolbars.TAG_VIEW_TOOLBAR)) {
            mAnnotationToolbarComponent.show(false);
        }
    }

    protected void showTopToolbar() {
        if (mViewerConfig == null || mViewerConfig.isShowTopToolbar()) {
            if (useCompactViewer()) {
                ToolbarSwitcherState state = mSwitcherViewModel.getState();
                if (state != null && state.getSelectedToolbar().getTag().equals(DefaultToolbars.TAG_VIEW_TOOLBAR)) {
                    mToolbar.setVisibility(View.VISIBLE);
                }
            } else {
                mToolbar.setVisibility(View.VISIBLE);
            }
        }
    }

    protected void updateUndoButtonVisibility(boolean visible) {
        if (mMenuUndo != null) {
            ToolbarSwitcherState state = mSwitcherViewModel.getState();
            PdfViewCtrlTabFragment2 currentFragment = getCurrentPdfViewCtrlFragment();
            boolean canShowUndo = state != null && state.getSelectedToolbar().getTag().equals(DefaultToolbars.TAG_VIEW_TOOLBAR) &&
                    currentFragment != null && currentFragment.getToolManager() != null &&
                    currentFragment.getToolManager().isShowUndoRedo() && !currentFragment.isReflowMode();
            mMenuUndo.setVisible(
                    visible &&
                            (mViewerConfig == null || mViewerConfig.isDocumentEditingEnabled()) &&
                            canShowUndo);
        }
    }

    protected void initOptionsMenu(Menu menu) {
        super.initOptionsMenu(menu);
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        // custom menu array
        if (hasCustomTopToolbarMenu()) {
            mCustomMenuUsed = true;
            // first loop through to obtain existing items
            SparseArray<MenuItem> oldMenuItems = new SparseArray<>();
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                oldMenuItems.put(item.getItemId(), item);
            }
            menu.clear();
            // create new menu based on custom array
            for (int item : mViewerConfig.getTopToolbarMenuIds()) {
                MenuItem oldItem = oldMenuItems.get(item);
                if (oldItem != null) {
                    MenuItem newItem = menu.add(oldItem.getGroupId(), item, Menu.NONE, oldItem.getTitle());
                    newItem.setVisible(oldItem.isVisible());
                    newItem.setIcon(oldItem.getIcon());
                    newItem.setActionView(oldItem.getActionView());
                    MenuItemCompat.setActionProvider(newItem, MenuItemCompat.getActionProvider(oldItem));
                    newItem.setAlphabeticShortcut(oldItem.getAlphabeticShortcut());
                    newItem.setIntent(oldItem.getIntent());
                    newItem.setCheckable(oldItem.isCheckable());
                    newItem.setEnabled(oldItem.isEnabled());
                    newItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                }
            }
        }
        // tabs
        mMenuTabs = menu.findItem(R.id.action_tabs);
        mTabActionView = new TabActionButton(activity);
        mTabActionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMenuTabs != null) {
                    onOptionsItemSelected(mMenuTabs);
                }
            }
        });
        if (mTabLayout != null) {
            mTabActionView.setTabCount(mTabLayout.getTabCount());
        }
        if (mMenuTabs != null) {
            TooltipCompat.setTooltipText(mTabActionView, mMenuTabs.getTitle());
            mMenuTabs.setActionView(mTabActionView);
            mMenuTabs.setShowAsAction(useTabletLayout() ? MenuItem.SHOW_AS_ACTION_NEVER : MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        // undo/redo
        mMenuUndo = menu.findItem(R.id.undo);
        mUndoActionButton = new UndoActionButton(activity);
        mUndoActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMenuUndo != null) {
                    onOptionsItemSelected(mMenuUndo);
                }
            }
        });
        mUndoActionButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mAnnotationToolbarComponent != null) {
                    mAnnotationToolbarComponent.showUndoRedoPopup(v);
                }
                return true;
            }
        });
        if (mMenuUndo != null) {
            mMenuUndo.setActionView(mUndoActionButton);
        }

        MenuItem menuShare = getToolbarOptionMenuItem(R.id.action_share);
        MenuItem menuShare2 = getViewOverflowOptionMenuItem(R.id.action_share);
        adjustMenuButtonShowAs(menuShare, activity);
        adjustMenuButtonShowAs(menuShare2, activity);

        MenuItem menuViewMode = getToolbarOptionMenuItem(R.id.action_viewmode);
        MenuItem menuViewMode2 = getViewOverflowOptionMenuItem(R.id.action_viewmode);
        adjustMenuButtonShowAs(menuViewMode, activity);
        adjustMenuButtonShowAs(menuViewMode2, activity);

        showTabletActionItems(menu);
    }

    protected void showTabletActionItems(Menu menu) {
        // On tablet or landscape phone, always show 4 action items
        if (useTabletLayout() && !mCustomMenuUsed) {
            // Force the 4 specific items to always show as action
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                if (item != null && (item.getItemId() == R.id.action_search ||
                        item.getItemId() == R.id.action_viewmode ||
                        item.getItemId() == R.id.action_thumbnails ||
                        item.getItemId() == R.id.action_outline)) {
                    item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                }
            }
        }
    }

    protected boolean hasCustomTopToolbarMenu() {
        return mViewerConfig != null &&
                mViewerConfig.getTopToolbarMenuIds() != null &&
                mViewerConfig.getTopToolbarMenuIds().length > 0;
    }

    @Override
    protected void adjustMenuButtonShowAs(MenuItem item, Activity activity) {
        if (mCustomMenuUsed) {
            return;
        }
        super.adjustMenuButtonShowAs(item, activity);
    }

    /**
     * Sets the visibility of options menu
     *
     * @param visible True if visible
     */
    protected void setOptionsMenuVisible(boolean visible) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        super.setOptionsMenuVisible(visible);
        MenuItem menuSearch = getToolbarOptionMenuItem(R.id.action_search);
        MenuItem menuReflowMode = getToolbarOptionMenuItem(R.id.action_reflow_mode);
        MenuItem menuBookmark = getToolbarOptionMenuItem(R.id.action_bookmark_add);
        MenuItem menuSearch2 = getViewOverflowOptionMenuItem(R.id.action_search);
        MenuItem menuReflowMode2 = getViewOverflowOptionMenuItem(R.id.action_reflow_mode);
        MenuItem menuBookmark2 = getViewOverflowOptionMenuItem(R.id.action_bookmark_add);
        if (menuSearch != null) {
            if (useTabletLayout() || mCustomMenuUsed) {
                menuSearch.setVisible(mViewerConfig == null || mViewerConfig.isShowSearchView());
            } else {
                menuSearch.setVisible(false);
            }
        }
        if (menuReflowMode != null) {
            if (useTabletLayout() || mCustomMenuUsed) {
                menuReflowMode.setVisible(mViewerConfig == null || mViewerConfig.isShowReflowOption());
            } else {
                menuReflowMode.setVisible(false);
            }
        }
        if (menuBookmark != null) {
            if (useTabletLayout() || mCustomMenuUsed) {
                menuBookmark.setVisible(isQuickBookmarkCreationEnabled());
            } else {
                menuBookmark.setVisible(false);
            }
        }
        if (menuSearch2 != null) {
            if (useTabletLayout() || mCustomMenuUsed) {
                menuSearch2.setVisible(mViewerConfig == null || mViewerConfig.isShowSearchView());
            } else {
                menuSearch2.setVisible(false);
            }
        }
        if (menuReflowMode2 != null) {
            if (useTabletLayout() || mCustomMenuUsed) {
                menuReflowMode2.setVisible(mViewerConfig == null || mViewerConfig.isShowReflowOption());
            } else {
                menuReflowMode2.setVisible(false);
            }
        }
        if (menuBookmark2 != null) {
            if (useTabletLayout() || mCustomMenuUsed) {
                menuBookmark2.setVisible(isQuickBookmarkCreationEnabled());
            } else {
                menuBookmark2.setVisible(false);
            }
        }
        boolean canShowBookmark = mViewerConfig != null && mViewerConfig.isShowBookmarksView() &&
                (mViewerConfig.isShowAnnotationsList() ||
                        mViewerConfig.isShowOutlineList() ||
                        mViewerConfig.isShowUserBookmarksList());
        if (mBottomNavComponent != null) {
            mBottomNavComponent.setItemVisibility(visible && (mViewerConfig == null || mViewerConfig.isShowSearchView()), R.id.action_search);
            mBottomNavComponent.setItemVisibility(visible && (mViewerConfig == null || mViewerConfig.isShowDocumentSettingsOption()), R.id.action_viewmode);
            mBottomNavComponent.setItemVisibility(visible && (mViewerConfig == null || mViewerConfig.isShowThumbnailView()), R.id.action_thumbnails);
            mBottomNavComponent.setItemVisibility(visible && (mViewerConfig == null || canShowBookmark), R.id.action_outline);
            mBottomNavComponent.setItemVisibility(visible && (mViewerConfig == null || mViewerConfig.isShowReflowOption()), R.id.action_reflow_mode);
            mBottomNavComponent.setItemVisibility(visible && isQuickBookmarkCreationEnabled(), R.id.action_bookmark_add);
            if (!mBottomNavComponent.hasVisibleItems()) {
                mBottomBarContainer.setVisibility(View.GONE);
                mBottomBarVisible = false;
            }
        }
        if (mAnnotationToolbarComponent != null) {
            mAnnotationToolbarComponent.setItemVisibility(DefaultToolbars.ButtonId.UNDO.value(), visible && (mViewerConfig == null || mViewerConfig.isDocumentEditingEnabled()));
            mAnnotationToolbarComponent.setItemVisibility(DefaultToolbars.ButtonId.REDO.value(), visible && (mViewerConfig == null || mViewerConfig.isDocumentEditingEnabled()));
        }
        updateUndoButtonVisibility(true);

        MenuItem menuThumbnailsView = getToolbarOptionMenuItem(R.id.action_thumbnails);
        MenuItem menuNavigationList = getToolbarOptionMenuItem(R.id.action_outline);
        MenuItem menuThumbnailsView2 = getViewOverflowOptionMenuItem(R.id.action_thumbnails);
        MenuItem menuNavigationList2 = getViewOverflowOptionMenuItem(R.id.action_outline);
        if (menuThumbnailsView != null) {
            if (useTabletLayout() || mCustomMenuUsed) {
                menuThumbnailsView.setVisible(mViewerConfig == null || mViewerConfig.isShowThumbnailView());
            } else {
                menuThumbnailsView.setVisible(false);
            }
        }
        if (menuNavigationList != null) {
            if (useTabletLayout() || mCustomMenuUsed) {
                menuNavigationList.setVisible(mViewerConfig == null || canShowBookmark);
            } else {
                menuNavigationList.setVisible(false);
            }
        }
        if (menuThumbnailsView2 != null) {
            if (useTabletLayout() || mCustomMenuUsed) {
                menuThumbnailsView2.setVisible(mViewerConfig == null || mViewerConfig.isShowThumbnailView());
            } else {
                menuThumbnailsView2.setVisible(false);
            }
        }
        if (menuNavigationList2 != null) {
            if (useTabletLayout() || mCustomMenuUsed) {
                menuNavigationList2.setVisible(mViewerConfig == null || canShowBookmark);
            } else {
                menuNavigationList2.setVisible(false);
            }
        }
        if (mMenuTabs != null) {
            mMenuTabs.setVisible(mViewerConfig == null || mViewerConfig.isMultiTabEnabled());
        }

        updateUndoRedoState();
    }

    @Override
    protected void animateToolbars(final boolean visible) {
        Activity activity = getActivity();
        if (activity == null || mAppBarLayout == null) {
            return;
        }

        if ((mAppBarLayout.getVisibility() == View.VISIBLE) == visible &&
                (mBottomBarContainer.getVisibility() == View.VISIBLE) == visible) {
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
        if (mViewerConfig == null ||
                mViewerConfig.isShowAppBar() ||
                mBottomBarVisible) {
            int duration = visible ? ANIMATE_DURATION_SHOW : ANIMATE_DURATION_HIDE;
            TransitionSet transitionSet = new TransitionSet();
            transitionSet.addTransition(new ChangeBounds());
            if (mBottomBarVisible) {
                Slide bottomSlide = new Slide(Gravity.BOTTOM);
                bottomSlide.addTarget(mBottomBarContainer);
                transitionSet.addTransition(bottomSlide);
            }
            if (mViewerConfig == null || mViewerConfig.isShowAppBar()) {
                Slide topSlide = new Slide(Gravity.TOP);
                topSlide.addTarget(mAppBarLayout);
                transitionSet.addTransition(topSlide);
            }
            transitionSet.setDuration(duration);
            transitionSet.excludeTarget(R.id.realtabcontent, true);
            transitionSet.addListener(new Transition.TransitionListener() {
                @Override
                public void onTransitionStart(@NonNull Transition transition) {
                    final PdfViewCtrlTabFragment2 currentFragment = getCurrentPdfViewCtrlFragment();
                    if (currentFragment != null && currentFragment.getPDFViewCtrl() != null && mAppBarLayout != null) {
                        if (visible) {
                            currentFragment.getPDFViewCtrl().scrollBy(0, mAppBarLayout.getHeight());
                        } else {
                            currentFragment.getPDFViewCtrl().scrollBy(0, -mAppBarLayout.getHeight());
                        }
                    }
                }

                @Override
                public void onTransitionEnd(@NonNull Transition transition) {

                }

                @Override
                public void onTransitionCancel(@NonNull Transition transition) {

                }

                @Override
                public void onTransitionPause(@NonNull Transition transition) {

                }

                @Override
                public void onTransitionResume(@NonNull Transition transition) {

                }
            });
            TransitionManager.beginDelayedTransition(mRootView, transitionSet);
            mBottomBarContainer.setVisibility(
                    (visible && mBottomBarVisible)
                            ? View.VISIBLE : View.GONE);
            mAppBarLayout.setVisibility(
                    (visible && (mViewerConfig == null || mViewerConfig.isShowAppBar()))
                            ? View.VISIBLE : View.GONE);

            // The shadow will fly across the screen for some reason.
            // so we'll hide it manually
            if (visible) {
                mBottomBarShadow.setVisibility(View.VISIBLE);
            } else {
                mBottomBarShadow.setVisibility(View.GONE);
            }
        } else {
            if (!mViewerConfig.isShowAppBar()) {
                mAppBarLayout.setVisibility(View.GONE);
            }
            if (!mBottomBarVisible) {
                mBottomBarContainer.setVisibility(View.GONE);
            }
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

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (isInFullScreenMode() && !PdfViewCtrlSettingsManager.getShowStatusBarForNewUI(activity)) {
            if (activity.getWindow() != null) {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (isInFullScreenMode() && !PdfViewCtrlSettingsManager.getShowStatusBarForNewUI(activity)) {
            if (activity.getWindow() != null) {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }

        if (isInFullScreenMode() && activity.getWindow() != null) {
            // https://developer.android.com/training/system-ui/dim#reveal
            View decorView = activity.getWindow().getDecorView();
            // Calling setSystemUiVisibility() with a value of 0 clears
            // all flags.
            decorView.setSystemUiVisibility(0);
        }
    }

    @Override
    protected void handleSystemWindowInsetChanged(int insetTop, int insetBottom) {
        super.handleSystemWindowInsetChanged(insetTop, insetBottom);
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (insetTop > 0) {
            if (isInFullScreenMode() && !PdfViewCtrlSettingsManager.getShowStatusBarForNewUI(activity)) {
                // found inset and we were hiding the status bar
                // unhide the status bar
                mShouldShowStatusBar = true;
                if (activity.getWindow() != null) {
                    activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                    activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    activity.getWindow().getDecorView().requestLayout();
                }
            }
        }
    }

    /**
     * Returns a {@link PdfViewCtrlTabFragment2} class object that will be used to
     * instantiate viewer tabs.
     *
     * @return a {@code CollabPdfViewCtrlTabFragment} class to instantiate later
     */
    @NonNull
    protected Class<? extends PdfViewCtrlTabFragment2> getDefaultTabFragmentClass() {
        return PdfViewCtrlTabFragment2.class;
    }

    public PdfViewCtrlTabFragment2 getCurrentPdfViewCtrlFragment() {
        PdfViewCtrlTabBaseFragment fragment = super.getCurrentPdfViewCtrlFragment();
        if (fragment instanceof PdfViewCtrlTabFragment2) {
            return (PdfViewCtrlTabFragment2) fragment;
        }
        return null;
    }

    @Override
    protected void setFragmentListeners(Fragment fragment) {
        super.setFragmentListeners(fragment);
        if (fragment instanceof PdfViewCtrlTabFragment2) {
            ((PdfViewCtrlTabFragment2) fragment).setComponentListener(this);
            ((PdfViewCtrlTabFragment2) fragment).setSnackbarListener(this);
        }
    }

    /**
     * Looks through all toolbars and select the tool with given button id. If the current toolbar
     * contains the tool, then select that instead. Must only be called after onViewCreated. 3.
     * Passing in -1 will clear the toolbar state and select pan tool.
     *
     * @param buttonId of the toolbar button
     */
    private void selectToolbarButtonInToolbars(int buttonId) {
        if (buttonId == -1) {
            mAnnotationToolbarComponent.clearState();
        } else {
            ToolbarSwitcherState state = mSwitcherViewModel.getState();
            if (state != null) {
                // Check toolbars to see if a button can be found
                String toolbarTag = state.getToolbarTagWithButtonId(buttonId);
                // If found, then switch to that toolbar and select the tool
                if (toolbarTag != null) {
                    // If tool does exists in current toolbar, then switch to the toolbar before
                    // selecting the toolbar button
                    if (!toolbarTag.equals(state.getSelectedToolbar().getTag())) {
                        openToolbarWithTag(toolbarTag);
                    }
                    selectToolbarButton(buttonId);
                }
            }
        }
    }

    /**
     * Switch to the toolbar with the given Toolbar tag. Must only be called after onCreate.
     *
     * @param toolbarTag the toolbar tag defined by the toolbar to open
     */
    public void openToolbarWithTag(@NonNull String toolbarTag) {
        mSwitcherViewModel.selectToolbar(toolbarTag);
    }

    /**
     * Hides the toolbar with given toolbar tag. Must only be called after onCreate.
     *
     * @param toolbarTag the toolbar tag defined by the toolbar to hide
     */
    public void hideToolbarWithTag(@NonNull String toolbarTag) {
        mSwitcherViewModel.hideToolbar(toolbarTag);
    }

    /**
     * Show the toolbar with given toolbar tag. Must only be called after onCreate.
     *
     * @param toolbarTag the toolbar tag defined by the toolbar to show
     */
    public void showToolbarWithTag(@NonNull String toolbarTag) {
        mSwitcherViewModel.showToolbar(toolbarTag);
    }

    /**
     * Selects the tool in the current toolbar, if available. Must only be called after onViewCreated.
     * Passing in -1 will clear the toolbar state and select pan tool.
     *
     * @param buttonId of the toolbar button
     */
    public void selectToolbarButton(int buttonId) {
        if (buttonId == -1) {
            mAnnotationToolbarComponent.clearState();
        } else {
            mAnnotationToolbarComponent.selectToolbarButton(buttonId);
        }
    }

    /**
     * Selects the tool in the toolbar, if available. Must only be called after onViewCreated.
     *
     * @param defaultToolbarId the id used in the default toolbar
     */
    public void selectToolbarButton(DefaultToolbars.ButtonId defaultToolbarId) {
        selectToolbarButton(defaultToolbarId.value());
    }

    /**
     * Set visibility of annotation toolbar button
     *
     * @param buttonType of toolbar button
     * @param visibility of toolbar button to set
     */
    public void toolbarButtonVisibility(@NonNull ToolbarButtonType buttonType, boolean visibility) {
        mAnnotationToolbarComponent.setToolbarButtonVisibility(buttonType, visibility);
    }

    /**
     * Gets the current active toolbar tag
     *
     * @return the toolbar tag
     */
    @Nullable
    public String getCurrentToolbarTag() {
        ToolbarSwitcherState state = mSwitcherViewModel.getState();
        if (state != null) {
            return state.getSelectedToolbar().builder.getToolbarTag();
        }

        return null;
    }

    @Override
    public void onBookmarksDialogDismissed(int tabIndex) {
        super.onBookmarksDialogDismissed(tabIndex);
        mBottomNavComponent.setItemSelected(false, R.id.action_outline);
    }

    /**
     * Add listener to notify when the toolbar changes.
     *
     * @param listener to add
     */
    public void addOnToolbarChangedListener(@NonNull OnToolbarChangedListener listener) {
        if (mAnnotationToolbarComponent != null) {
            mAnnotationToolbarComponent.addOnToolbarChangedListener(listener);
        } else {
            mOnToolbarChangedListeners.add(listener);
        }
    }

    /**
     * Remove {@link OnToolbarChangedListener} listener.
     *
     * @param listener to remove
     */
    public void removeOnToolbarChangedListener(@NonNull OnToolbarChangedListener listener) {
        if (mAnnotationToolbarComponent != null) {
            mAnnotationToolbarComponent.removeOnToolbarChangedListener(listener);
        }
        mOnToolbarChangedListeners.remove(listener);
    }

    /**
     * Add listener to notify prior to building a new AnnotationToolbar.
     *
     * @param listener to add
     */
    public void addOnPreBuildToolbarListener(@NonNull OnPreBuildToolbarListener listener) {
        if (mAnnotationToolbarComponent != null) {
            mAnnotationToolbarComponent.addOnPreBuildToolbarListener(listener);
        } else {
            mOnPreBuildToolbarListeners.add(listener);
        }
    }

    /**
     * Remove {@link OnPreBuildToolbarListener} listener.
     *
     * @param listener to remove
     */
    public void removeOnPreBuildToolbarListener(@NonNull OnPreBuildToolbarListener listener) {
        if (mAnnotationToolbarComponent != null) {
            mAnnotationToolbarComponent.removeOnPreBuildToolbarListener(listener);
        }
        mOnPreBuildToolbarListeners.remove(listener);
    }

    /**
     * Updates the toolbar state depending on ToolManager state.
     */
    public void updateToolbarState() {
        mAnnotationToolbarComponent.updateToolbarState();
    }

    /**
     * Sets the color of the selected tool's icon.
     */
    public void setSelectedIconColor(int buttonId, @ColorInt int iconColor) {
        if (mAnnotationToolbarComponent != null) {
            mAnnotationToolbarComponent.setSelectedIconColor(buttonId, iconColor);
        }
    }

    /**
     * Sets the color of the tool's icon.
     */
    public void setIconColor(int buttonId, @ColorInt int iconColor) {
        if (mAnnotationToolbarComponent != null) {
            mAnnotationToolbarComponent.setIconColor(buttonId, iconColor);
        }
    }

    /**
     * Sets the toolbar switcher button visibility to View.VISIBLE or View.GONE.
     *
     * @param visible whether the toolbar switcher button should be visible
     */
    public void setToolbarSwitcherVisible(boolean visible) {
        if (useCompactViewer()) {
            if (mSwitcherCompactButton != null) {
                mSwitcherCompactButton.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        } else {
            if (mSwitcherButton != null) {
                mSwitcherButton.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        }
    }

    /**
     * Sets the toolbars of the current viewer. If the given toolbars are empty, the the annotation toolbar
     * and toolbar switcher button will automatically hide. If the given toolbars are non-empty, then
     * the annotation toolbar and toolbar switcher button will automatically show. The first toolbar
     * will be selected after setting the toolbars
     *
     * @param toolbarBuilders the toolbars to set to the viewer.
     */
    public void setAnnotationToolbars(@NonNull List<AnnotationToolbarBuilder> toolbarBuilders) {
        if (toolbarBuilders.isEmpty()) {
            // Hide annotation toolbar and switcher
            setAnnotationToolbarVisible(false, false);
            setToolbarSwitcherVisible(false);
            mSwitcherViewModel.setState(null);
        } else {
            // Set our toolbars
            List<ToolbarSwitcherItem> items = new ArrayList<>();
            for (int i = 0; i < toolbarBuilders.size(); i++) {
                AnnotationToolbarBuilder builderCopy = toolbarBuilders.get(i).copy();
                ToolbarSwitcherItem switcherItem = new ToolbarSwitcherItem(builderCopy);
                // Always select the first item
                if (i == 0) {
                    switcherItem.setSelected(true);
                }
                items.add(switcherItem);
            }
            mSwitcherViewModel.setState(new ToolbarSwitcherState(items));

            // Show annotation toolbar and switcher, in case they are hidden
            setAnnotationToolbarVisible(true, false);
            setToolbarSwitcherVisible(true);
        }
    }

    /**
     * Sets the visibility of the annotation toolbar, and animates visibility change if specified.
     * <p>
     * Must only be called after onViewCreated
     *
     * @param visible  whether the annotation toolbar should be visible
     * @param animated whether changing the visibility should be animated
     */
    public void setAnnotationToolbarVisible(boolean visible, boolean animated) {
        if (mAnnotationToolbarComponent != null) {
            if (visible && mSwitcherViewModel != null && mSwitcherViewModel.getState() == null) {
                throw new RuntimeException("Can not show an empty toolbar");
            }
            if (visible) {
                mAnnotationToolbarComponent.show(animated);
            } else {
                // Switch to pan before hiding toolbar
                selectToolbarButton(-1);
                mAnnotationToolbarComponent.hide(animated);
            }
        }
    }

    /**
     * Returns whether quick bookmark creation is enabled in the viewer.
     */
    protected boolean isQuickBookmarkCreationEnabled() {
        return mViewerConfig == null ?
                getActivity() != null && PdfViewCtrlSettingsManager.getQuickBookmarkCreation(getActivity()) :
                mViewerConfig.isQuickBookmarkCreationEnabled();
    }

    /**
     * Attaches the live data to the bookmark button view model.
     *
     * @param currentFragment the fragment containing the data to attach
     */
    private void attachBookmarkButtonData(@NonNull PdfViewCtrlTabBaseFragment currentFragment) {
        if (mBookmarkButtonViewModel != null && isQuickBookmarkCreationEnabled()) {
            LiveData<BookmarksCache> bookmarks = currentFragment.getBookmarks();
            MutableLiveData<PageState> pageChange = currentFragment.getPageChange();
            if (bookmarks != null && pageChange != null) {
                mBookmarkButtonViewModel.attachBookmarkData(bookmarks);
                mBookmarkButtonViewModel.attachPageData(pageChange);
            }
        }
    }

    /**
     * Listener called when toolbar changes.
     */
    public interface OnToolbarChangedListener extends AnnotationToolbarComponent.OnToolbarChangedListener {
    }

    /**
     * Listener called prior to building a new AnnotationToolbar.
     */
    public interface OnPreBuildToolbarListener extends AnnotationToolbarComponent.OnPreBuildToolbarListener {
    }
}
