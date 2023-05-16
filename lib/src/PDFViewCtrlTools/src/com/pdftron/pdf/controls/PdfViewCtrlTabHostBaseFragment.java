//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.CallSuper;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.DisplayCutoutCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;
import androidx.transition.Fade;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.pdftron.common.PDFNetException;
import com.pdftron.filters.SecondaryFileFilter;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.Bookmark;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageSet;
import com.pdftron.pdf.TextSearchResult;
import com.pdftron.pdf.config.ViewerConfig;
import com.pdftron.pdf.dialog.BookmarksDialogFragment;
import com.pdftron.pdf.dialog.CustomColorModeDialogFragment;
import com.pdftron.pdf.dialog.OptimizeDialogFragment;
import com.pdftron.pdf.dialog.RotateDialogFragment;
import com.pdftron.pdf.dialog.ViewModePickerDialogFragment;
import com.pdftron.pdf.dialog.annotlist.AnnotationListSortOrder;
import com.pdftron.pdf.dialog.digitalsignature.validation.DigitalSignatureUtils;
import com.pdftron.pdf.dialog.digitalsignature.validation.list.DigitalSignatureListDialog;
import com.pdftron.pdf.dialog.pdflayer.PdfLayerDialogFragment;
import com.pdftron.pdf.dialog.pdflayer.PdfLayerUtils;
import com.pdftron.pdf.dialog.redaction.SearchRedactionDialogFragment;
import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.model.ExternalFileInfo;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.model.OptimizeParams;
import com.pdftron.pdf.model.PdfViewCtrlTabInfo;
import com.pdftron.pdf.tools.QuickMenu;
import com.pdftron.pdf.tools.QuickMenuItem;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.DialogFragmentTab;
import com.pdftron.pdf.utils.ExceptionHandlerCallback;
import com.pdftron.pdf.utils.PaneBehavior;
import com.pdftron.pdf.utils.PdfDocManager;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.PdfViewCtrlTabsManager;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.ThemeProvider;
import com.pdftron.pdf.utils.UserCropUtilities;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.pdf.utils.cache.UriCacheManager;
import com.pdftron.pdf.viewmodel.RedactionEvent;
import com.pdftron.pdf.viewmodel.RedactionViewModel;
import com.pdftron.pdf.widget.AppBarLayout;
import com.pdftron.pdf.widget.toolbar.component.DefaultToolbars;
import com.pdftron.pdf.widget.toolbar.component.ToolbarSharedPreferences;
import com.pdftron.sdf.SDFDoc;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * The PdfViewCtrlTabHostBaseFragment is a base class for other viewer host classes.
 */
public abstract class PdfViewCtrlTabHostBaseFragment extends Fragment implements
        PdfViewCtrlTabBaseFragment.TabListener,
        ToolManager.QuickMenuListener,
        TabLayout.BaseOnTabSelectedListener,
        SearchResultsView.SearchResultsListener,
        ViewModePickerDialogFragment.ViewModePickerDialogFragmentListener,
        BookmarksDialogFragment.BookmarksDialogListener,
        BookmarksTabLayout.BookmarksTabsListener,
        UserCropSelectionDialogFragment.UserCropSelectionDialogFragmentListener,
        UserCropDialogFragment.OnUserCropDialogDismissListener,
        UserCropUtilities.AutoCropInBackgroundTask.AutoCropTaskListener,
        ThumbnailsViewFragment.OnThumbnailsViewDialogDismissListener,
        ThumbnailsViewFragment.OnThumbnailsEditAttemptWhileReadOnlyListener,
        ThumbnailsViewFragment.OnExportThumbnailsListener,
        View.OnLayoutChangeListener,
        View.OnSystemUiVisibilityChangeListener {

    private static final String TAG = PdfViewCtrlTabHostBaseFragment.class.getName();

    public static final String BUNDLE_THEME = "bundle_theme";
    public static final String BUNDLE_TAB_HOST_NAV_ICON = "bundle_tab_host_nav_icon";
    public static final String BUNDLE_TAB_HOST_TOOLBAR_MENU = "bundle_tab_host_toolbar_menu";
    public static final String BUNDLE_TAB_HOST_CONFIG = "bundle_tab_host_config";
    public static final String BUNDLE_TAB_HOST_QUIT_APP_WHEN_DONE_VIEWING = "bundle_tab_host_quit_app_when_done_viewing";
    public static final String BUNDLE_TAB_FRAGMENT_CLASS = "PdfViewCtrlTabHostFragment_tab_fragment_class";

    // Customizable fields
    protected Class<? extends PdfViewCtrlTabBaseFragment> mTabFragmentClass; // default tab fragment class

    protected static final int MAX_TOOLBAR_ICON_COUNT = 7;
    protected static final int MAX_TOOLBAR_VISIBLE_ICON_COUNT = 5;

    protected static final int HIDE_TOOLBARS_TIMER = 5000; // 5 sec
    protected static final int HIDE_NAVIGATION_BAR_TIMER = 2000; // 2 sec (set to match immersive-sticky show duration)

    protected static final String KEY_IS_SEARCH_MODE = "is_search_mode";
    protected static final String KEY_IS_RESTARTED = "is_fragment_restarted";

    public static final int ANIMATE_DURATION_SHOW = 250;
    public static final int ANIMATE_DURATION_HIDE = 250;

    protected static boolean sDebug;

    protected boolean mQuitAppWhenDoneViewing;

    protected int mToolbarNavRes = R.drawable.ic_menu_white_24dp;
    protected int[] mToolbarMenuResArray;
    @Nullable
    protected ViewerConfig mViewerConfig;

    protected View mFragmentView;
    protected ViewGroup mRootView;
    protected AppBarLayout mAppBarLayout;
    protected Toolbar mToolbar;
    protected SearchToolbar mSearchToolbar;
    protected CustomFragmentTabLayout mTabLayout;
    protected FrameLayout mFragmentContainer;
    protected boolean mHidePresetBar;

    protected String mStartupTabTag;
    protected boolean mMultiTabModeEnabled = true;
    protected int mCurTabIndex = -1;

    protected ThumbnailsViewFragment mThumbFragment;
    protected BookmarksDialogFragment mBookmarksDialog;
    protected UserCropDialogFragment mUserCropDialog;

    // controls fields for bookmark dialog
    protected Bookmark mCurrentBookmark;

    protected int mLastSystemUIVisibility;

    protected AtomicBoolean mFileSystemChanged = new AtomicBoolean();

    protected boolean mFragmentPaused = true;

    private UserCropUtilities.AutoCropInBackgroundTask mAutoCropTask;
    private boolean mAutoCropTaskPaused;
    private String mAutoCropTaskTabTag;

    // UI elements
    protected SearchResultsView mSearchResultsView;
    protected boolean mSearchMatchCase = false;
    protected boolean mSearchMatchWord = false;
    protected boolean mIsSearchMode;
    protected boolean mIsRestarted = false;
    protected boolean mAutoHideEnabled = true;

    protected boolean mWillShowAnnotationToolbar; // only used by PdfViewCtrlTabHostFragment

    // overflow
    protected PopupMenu mViewOverflowMenu;

    protected List<TabHostListener> mTabHostListeners;
    protected AppBarVisibilityListener mAppBarVisibilityListener;
    protected List<ReflowControlListener> mReflowControlListeners;

    protected int mSystemWindowInsetTop = 0;
    protected int mSystemWindowInsetBottom = 0;

    // Disposables
    protected CompositeDisposable mDisposables = new CompositeDisposable();

    // theme
    protected ThemeProvider mThemeProvider = new ThemeProvider();
    protected ToolbarSharedPreferences mToolbarSharedPreferences = new ToolbarSharedPreferences();

    /**
     * Callback interface to be invoked when AppBar visibility changes.
     */
    public interface AppBarVisibilityListener {
        void onAppBarVisibilityChanged(boolean visible);
    }

    public interface ReflowControlListener {
        void onToggleReflowMode();
    }

    /**
     * Callback interface to be invoked when an interaction is needed.
     */
    public interface TabHostListener {

        /**
         * Called when the tab host has been shown.
         */
        void onTabHostShown();

        /**
         * Called when the tab host has been hidden.
         */
        void onTabHostHidden();

        /**
         * Called when the last tab in the tab host has been closed, and therefore there is no more tab.
         */
        void onLastTabClosed();

        /**
         * Called when a new tab has been selected excluding the initial tab.
         *
         * @param tag the tab tag changed to
         */
        @SuppressWarnings("unused")
        void onTabChanged(String tag);

        /**
         * Called when an error has been happened when opening a document.
         */
        boolean onOpenDocError();

        /**
         * Called when navigation button has been pressed.
         */
        void onNavButtonPressed();

        /**
         * The implementation should browse to the specified file in the folder.
         *
         * @param fileName   The file name
         * @param filepath   The file path
         * @param itemSource The item source of the file
         */
        void onShowFileInFolder(String fileName, String filepath, int itemSource);

        /**
         * The implementation should determine whether the long press on tab widget should show file info.
         *
         * @return true if long press shows file info, false otherwise
         */
        boolean canShowFileInFolder();

        /**
         * The implementation should determine whether closing a tab should show re-open snackbar.
         *
         * @return true if can show snackbar, false otherwise
         */
        boolean canShowFileCloseSnackbar();

        /**
         * Called when creating Toolbar options menu
         *
         * @param menu     the menu
         * @param inflater the inflater
         */
        @SuppressWarnings("unused")
        boolean onToolbarCreateOptionsMenu(Menu menu, MenuInflater inflater);

        /**
         * Called when preparing Toolbar options menu
         *
         * @param menu the menu
         */
        @SuppressWarnings("unused")
        boolean onToolbarPrepareOptionsMenu(Menu menu);

        /**
         * Called when Toolbar options menu selected
         *
         * @param item the menu item
         */
        boolean onToolbarOptionsItemSelected(MenuItem item);

        /**
         * Called when search view expanded
         */
        void onStartSearchMode();

        /**
         * Called when search view collapsed
         */
        void onExitSearchMode();

        /**
         * Called when about the re-create Activity for day/night mode
         */
        boolean canRecreateActivity();

        /**
         * Called when the fragment is paused.
         *
         * @param fileInfo                  The file shown when tab has been paused
         * @param isDocModifiedAfterOpening True if document has been modified
         *                                  after opening; False otherwise
         */
        void onTabPaused(FileInfo fileInfo, boolean isDocModifiedAfterOpening);

        /**
         * Called when an SD card file is opened as a local file
         */
        void onJumpToSdCardFolder();

        /**
         * Called when document associated with a tab is loaded
         *
         * @param tag the document tag
         */
        void onTabDocumentLoaded(String tag);
    }

    // Handlers
    // Hide toolbar setup if there is any
    private boolean mToolbarTimerDisabled;
    private Handler mHideToolbarsHandler = new Handler(Looper.getMainLooper());
    private Runnable mHideToolbarsRunnable = new Runnable() {
        @Override
        public void run() {
            handleAutoHideUi();
        }
    };

    // Hide navigation bar
    private Handler mHideNavigationBarHandler = new Handler(Looper.getMainLooper());
    private Runnable mHideNavigationBarRunnable = new Runnable() {
        @Override
        public void run() {
            handleAutoHideNavBar();
        }
    };

    protected abstract void handleAutoHideUi();

    protected abstract void handleAutoHideNavBar();

    protected abstract void openBookmarksDialog();

    protected abstract void openRedactionDialog(SearchRedactionDialogFragment dialogFragment);

    protected abstract @LayoutRes
    int getLayoutRes();

    protected abstract @IdRes
    int getContainerId();

    protected abstract @LayoutRes
    int getTabLayoutRes();

    protected abstract void updateUndoRedoState();

    public abstract void onEditToolbarMenu();

    protected abstract int getDefaultTheme();

    // Used to keep track if user is currently in a text edit mode
    private boolean mEditTextFocus = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (sDebug)
            Log.v("LifeCycle", "HostFragment.onCreate");

        final Activity activity = getActivity();
        int theme = getDefaultTheme();
        if (getArguments() != null) {
            int bundleTheme = getArguments().getInt(BUNDLE_THEME, getDefaultTheme());
            if (bundleTheme != 0) {
                theme = bundleTheme;
            }
        }
        mThemeProvider.setTheme(theme);
        if (activity != null && mThemeProvider.getTheme() != 0) {
            activity.setTheme(mThemeProvider.getTheme());
        }

        super.onCreate(savedInstanceState);

        if (canRecreateActivity() && activity instanceof AppCompatActivity && applyTheme((AppCompatActivity) activity)) {
            return;
        }

        if (getArguments() != null) {
            mToolbarNavRes = getArguments().getInt(BUNDLE_TAB_HOST_NAV_ICON, R.drawable.ic_menu_white_24dp);
            int[] menus = getArguments().getIntArray(BUNDLE_TAB_HOST_TOOLBAR_MENU);
            if (menus != null) {
                mToolbarMenuResArray = menus;
            } else {
                mToolbarMenuResArray = getDefaultToolbarMenu();
            }
            mViewerConfig = getArguments().getParcelable(BUNDLE_TAB_HOST_CONFIG);
            mQuitAppWhenDoneViewing = getArguments().getBoolean(BUNDLE_TAB_HOST_QUIT_APP_WHEN_DONE_VIEWING, false);
            //noinspection unchecked
            mTabFragmentClass = (Class<? extends PdfViewCtrlTabBaseFragment>) getArguments().getSerializable(BUNDLE_TAB_FRAGMENT_CLASS);

            mHidePresetBar = (mViewerConfig != null && mViewerConfig.isHidePresetBar());
        }
        mTabFragmentClass = mTabFragmentClass == null ? getDefaultTabFragmentClass() : mTabFragmentClass;

        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            mIsSearchMode = savedInstanceState.getBoolean(KEY_IS_SEARCH_MODE);
            mIsRestarted = savedInstanceState.getBoolean(KEY_IS_RESTARTED);
        }
    }

    protected boolean applyTheme(@NonNull AppCompatActivity activity) {
        return Utils.applyDayNight(activity);
    }

    abstract int[] getDefaultToolbarMenu();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (sDebug)
            Log.v("LifeCycle", "HostFragment.onCreateView");

        return inflater.inflate(getLayoutRes(), container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (sDebug)
            Log.v("LifeCycle", "HostFragment.onViewCreated");

        super.onViewCreated(view, savedInstanceState);

        // Should set configurations before creating tabs
        adjustConfiguration();

        mFragmentView = view;
        initViews();
        updateFullScreenModeLayout();

        createTabs(getArguments());

        updateTabLayout();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Activity activity = getActivity();
        if (activity instanceof AppCompatActivity && useSupportActionBar()) {
            AppCompatActivity appCompatActivity = (AppCompatActivity) activity;
            appCompatActivity.setSupportActionBar(mToolbar);

            ActionBar actionBar = appCompatActivity.getSupportActionBar();
            if (actionBar != null) {
                if (mViewerConfig != null && !Utils.isNullOrEmpty(mViewerConfig.getToolbarTitle())) {
                    actionBar.setDisplayShowTitleEnabled(true);
                    actionBar.setTitle(mViewerConfig.getToolbarTitle());
                } else {
                    actionBar.setDisplayShowTitleEnabled(false);
                }

                // NOTE: If the Toolbar menu is inflated manually then the visibility listener here
                // and any in the Activity **WILL NOT WORK**!
                actionBar.addOnMenuVisibilityListener(new ActionBar.OnMenuVisibilityListener() {
                    @Override
                    public void onMenuVisibilityChanged(boolean isVisible) {
                        if (isVisible) {
                            // Do not hide the toolbars while menus are open.
                            final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
                            boolean isAnnotationMode = currentFragment != null && currentFragment.isAnnotationMode();
                            if (!mIsSearchMode && !isAnnotationMode) {
                                stopHideToolbarsTimer();
                            }
                        } else {
                            final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
                            boolean isAnnotationMode = currentFragment != null && currentFragment.isAnnotationMode();
                            if (!mIsSearchMode && !isAnnotationMode) {
                                resetHideToolbarsTimer();
                            }
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        AnalyticsHandlerAdapter.getInstance().sendTimedEvent(AnalyticsHandlerAdapter.EVENT_SCREEN_VIEWER);
    }

    @Override
    public void onResume() {
        if (sDebug)
            Log.v("LifeCycle", "HostFragment.onResume");

        super.onResume();

        if (isHidden()) {
            return;
        }

        resumeFragment();
    }

    @Override
    public void onPause() {
        if (sDebug)
            Log.v("LifeCycle", "HostFragment.onPause");

        pauseFragment();

        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        Activity activity = getActivity();
        if (activity != null && Utils.isPie() && PdfViewCtrlSettingsManager.getFullScreenMode(activity)) {
            // reset cutout
            WindowManager.LayoutParams params = activity.getWindow().getAttributes();
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
        }
        AnalyticsHandlerAdapter.getInstance().endTimedEvent(AnalyticsHandlerAdapter.EVENT_SCREEN_VIEWER);
    }

    @Override
    public void onDestroyView() {
        if (sDebug)
            Log.v("LifeCycle", "HostFragment.onDestroy");
        super.onDestroyView();
        mTabLayout.removeOnTabSelectedListener(this);
    }

    @Override
    public void onDestroy() {
        if (sDebug)
            Log.v("LifeCycle", "HostFragment.onDestroy");

        try {
            mTabLayout.removeAllFragments();
        } catch (Exception ignored) {
        }

        // Dispose of all observables
        if (mDisposables != null && !mDisposables.isDisposed()) {
            mDisposables.dispose();
        }

        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_IS_SEARCH_MODE, mIsSearchMode);
        outState.putBoolean(KEY_IS_RESTARTED, true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu, @NonNull MenuInflater inflater) {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (getToolbarMenuResArray() == null) {
            return;
        }
        if (!useSupportActionBar() && menu != mToolbar.getMenu()) {
            // when not using support action bar, we need to make sure the menu is actually the toolbar menu
            return;
        }

        if (mTabHostListeners != null) {
            for (TabHostListener listener : mTabHostListeners) {
                if (listener.onToolbarCreateOptionsMenu(menu, inflater)) {
                    return;
                }
            }
        }

        menu.clear();
        for (int res : getToolbarMenuResArray()) {
            mToolbar.inflateMenu(res);
        }

        initOptionsMenu(menu);
        setOptionsMenuVisible(true);
    }

    @Nullable
    protected int[] getToolbarMenuResArray() {
        return mToolbarMenuResArray;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Activity activity = getActivity();
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return;
        }

        if (mTabHostListeners != null) {
            for (TabHostListener listener : mTabHostListeners) {
                if (listener.onToolbarPrepareOptionsMenu(menu)) {
                    return;
                }
            }
        }

        // when overflow menu is open, stop the toolbar timer
        stopHideToolbarsTimer();

        if (menu != null) {
            // Update close menu item for current tab-mode.
            if (!mIsSearchMode) {
                updateCloseTabButtonVisibility(true);
            }

            updateAttachmentState();
            updateLayersState();
            updateDigitalSignaturesState();

            MenuItem menuPasswordSave = menu.findItem(R.id.menu_export_password_copy);
            if (menuPasswordSave != null) {
                if (currentFragment.isPasswordProtected()) {
                    menuPasswordSave.setTitle(getString(R.string.action_export_password_existing));
                } else {
                    menuPasswordSave.setTitle(getString(R.string.action_export_password));
                }
            }

            updateUndoRedoState();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mTabHostListeners != null) {
            for (TabHostListener listener : mTabHostListeners) {
                if (listener.onToolbarOptionsItemSelected(item)) {
                    return true;
                }
            }
        }

        FragmentActivity activity = getActivity();
        PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return false;
        }

        final PDFViewCtrl pdfViewCtrl = currentFragment.getPDFViewCtrl();
        if (pdfViewCtrl == null) {
            return false;
        }

        if (!mIsSearchMode) {
            resetHideToolbarsTimer();
        }

        if (currentFragment.getToolManager() != null
                && currentFragment.getToolManager().getTool() != null) {
            ToolMode mode = ToolManager.getDefaultToolMode(currentFragment.getToolManager().getTool().getToolMode());
            if (mode == ToolMode.TEXT_CREATE ||
                    mode == ToolMode.CALLOUT_CREATE ||
                    mode == ToolMode.ANNOT_EDIT ||
                    mode == ToolMode.FORM_FILL) {
                pdfViewCtrl.closeTool();
            }
        }

        final int id = item.getItemId();

        if (id == android.R.id.home) {
            handleNavIconClick();
        } else if (!mIsSearchMode) {
            resetHideToolbarsTimer();
        }

        if (id == R.id.undo) {
            undo();
        } else if (id == R.id.redo) {
            redo();
        } else if (id == R.id.action_share) {
            if (currentFragment.isDocumentReady()) {
                onShareOptionSelected();
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_SHARE);
            }
        } else if (id == R.id.action_viewmode) {
            if (currentFragment.isDocumentReady()) {
                onViewModeOptionSelected();
            }
        } else if (id == R.id.action_print) {
            if (currentFragment.isDocumentReady()) {
                currentFragment.handlePrintAnnotationSummary();
            }
        } else if (id == R.id.action_close_tab) {
            // if tabs not enabled, show close tab option
            if (!PdfViewCtrlSettingsManager.getMultipleTabs(activity)) {
                closeTab(currentFragment.getTabTag(), currentFragment.getTabSource());
            }
        } else if (id == R.id.action_addpage) {
            if (!checkTabConversionAndAlert(R.string.cant_edit_while_converting_message, false)) {
                addNewPage();
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_EDIT_PAGES_ADD);
            }
        } else if (id == R.id.action_deletepage) {
            if (!checkTabConversionAndAlert(R.string.cant_edit_while_converting_message, false)) {
                requestDeleteCurrentPage();
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_EDIT_PAGES_DELETE);
            }
        } else if (id == R.id.action_rotatepage) {
            if (!checkTabConversionAndAlert(R.string.cant_edit_while_converting_message, false)) {
                showRotateDialog();
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_EDIT_PAGES_ROTATE);
            }
        } else if (id == R.id.action_export_pages) {
            if (currentFragment.isDocumentReady()) {
                if (!checkTabConversionAndAlert(R.string.cant_edit_while_converting_message, false)) {
                    onViewModeSelected(PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_THUMBNAILS_VALUE, true,
                            pdfViewCtrl.getCurrentPage());
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_EDIT_PAGES_REARRANGE);
                }
            }
        } else if (id == R.id.menu_export_copy) {
            if (currentFragment.isDocumentReady()) {
                onSaveAsOptionSelected();
            }
        } else if (id == R.id.menu_export_flattened_copy) {
            if (currentFragment.isDocumentReady()) {
                onFlattenOptionSelected();
            }
        } else if (id == R.id.menu_export_optimized_copy) {
            if (currentFragment.isDocumentReady()) {
                onSaveOptimizedCopySelected();
            }
        } else if (id == R.id.menu_export_cropped_copy) {
            if (currentFragment.isDocumentReady()) {
                onSaveCroppedCopySelected();
            }
        } else if (id == R.id.menu_export_password_copy) {
            if (currentFragment.isDocumentReady()) {
                onSavePasswordCopySelected();
            }
        } else if (id == R.id.action_file_attachment) {
            if (currentFragment.isDocumentReady()) {
                currentFragment.handleViewFileAttachments();
            }
        } else if (id == R.id.action_pdf_layers) {
            if (currentFragment.isDocumentReady()) {
                showPdfLayersDialog(pdfViewCtrl);
            }
        } else if (id == R.id.action_reflow_mode) {
            if (currentFragment.isDocumentReady() && !checkTabConversionAndAlert(R.string.cant_reflow_while_converting_message, true)) {
                onToggleReflow();
            }
        } else if (id == R.id.action_edit_menu || id == DefaultToolbars.ButtonId.CUSTOMIZE.value()) {
            onEditToolbarMenu();
        } else if (id == R.id.action_search) {
            onSearchOptionSelected();
        } else if (id == R.id.action_digital_signatures) {
            FragmentManager fragmentManager = getFragmentManager();
            if (currentFragment.getToolManager() != null && fragmentManager != null) {
                showDigitalSignatureList();
            }
        } else {
            return false;
        }

        return true;
    }

    protected void showPdfLayersDialog(@NonNull PDFViewCtrl pdfViewCtrl) {
        PdfLayerDialogFragment pdfLayerDialogFragment = PdfLayerDialogFragment.newInstance();
        pdfLayerDialogFragment.setPdfViewCtrl(pdfViewCtrl);
        pdfLayerDialogFragment.setStyle(DialogFragment.STYLE_NO_TITLE, mThemeProvider.getTheme());
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            pdfLayerDialogFragment.show(fragmentManager, PdfLayerDialogFragment.TAG);
        }
    }

    public void showDigitalSignatureList() {
        PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment != null) {
            PDFViewCtrl pdfViewCtrl = currentFragment.getPDFViewCtrl();
            ToolManager toolManager = currentFragment.getToolManager();
            FragmentManager fragmentManager = getFragmentManager();
            if (pdfViewCtrl != null && toolManager != null && fragmentManager != null) {
                DigitalSignatureListDialog dialog = DigitalSignatureListDialog.newInstance();
                dialog.setStyle(DialogFragment.STYLE_NO_TITLE, toolManager.getTheme());
                dialog.setPDFViewCtrl(pdfViewCtrl);
                dialog.show(fragmentManager, "digital_sig_list_dialog");
            }
        }
    }

    protected void handleNavIconClick() {
        PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment != null) {
            currentFragment.closeKeyboard();
        }
        stopHideToolbarsTimer();
        if (mTabHostListeners != null) {
            for (TabHostListener listener : mTabHostListeners) {
                listener.onNavButtonPressed();
            }
        }
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }

        // check for setDoc error
        if (currentFragment.isOpenFileFailed()) {
            if (canShowOpenFileError()) {
                handleOpenFileFailed(currentFragment.getTabErrorCode());
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        FragmentActivity activity = getActivity();
        PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return;
        }

        int diff = mLastSystemUIVisibility ^ visibility;
        if ((diff & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) {
            // Check if the current fragment is in annotation mode.
            if (currentFragment.isAnnotationMode()) {
                if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) {
                    // The navigation bar was hidden - stop and remove any timers.
                    stopHideNavigationBarTimer();
                } else {
                    // The navigation bar was shown - start a timer to hide it again.
                    resetHideNavigationBarTimer();
                }
            }
        }

        mLastSystemUIVisibility = visibility;
    }

    /**
     * Creates tabs.
     *
     * @param args The arguments
     */
    @CallSuper
    public void createTabs(Bundle args) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        String startupTitle = null;
        String startupFileExtension = null;
        int startupItemSource = BaseFileInfo.FILE_TYPE_UNKNOWN;
        // args will be null if we just want to go back to viewer without adding any new tabs
        if (args != null) {
            mStartupTabTag = mIsRestarted ? null : args.getString(PdfViewCtrlTabBaseFragment.BUNDLE_TAB_TAG);
            startupTitle = args.getString(PdfViewCtrlTabBaseFragment.BUNDLE_TAB_TITLE);
            startupFileExtension = args.getString(PdfViewCtrlTabBaseFragment.BUNDLE_TAB_FILE_EXTENSION);
            startupItemSource = args.getInt(PdfViewCtrlTabBaseFragment.BUNDLE_TAB_ITEM_SOURCE);

            // if startup tag is null
            // assume we want to open last opened file
            if (null != mStartupTabTag) {
                // error checking
                if (Utils.isNullOrEmpty(mStartupTabTag)
                        || Utils.isNullOrEmpty(startupTitle)
                        || (startupItemSource == BaseFileInfo.FILE_TYPE_FILE && !Utils.isNotPdf(mStartupTabTag) && !(new File(mStartupTabTag).exists()))) {

                    if (canShowOpenFileError()) {
                        CommonToast.showText(activity, getString(R.string.error_opening_doc_message), Toast.LENGTH_SHORT);
                    }
                    return;
                }
            }
        }

        // if Tabs is enabled, we add tabs to local tab list and show tab widget
        // else, clear tab list and hide tab widget
        if (!PdfViewCtrlSettingsManager.getMultipleTabs(activity)) {
            mMultiTabModeEnabled = false;
        }
        setTabLayoutVisible(mMultiTabModeEnabled);

        // if single tab remove all current tabs
        if (!mMultiTabModeEnabled) {
            if (mStartupTabTag != null) {
                PdfViewCtrlTabsManager.getInstance().cleanup();
                PdfViewCtrlTabsManager.getInstance().clearAllPdfViewCtrlTabInfo(activity);
            } else {
                // otherwise we should keep the latest viewed document
                String latestViewedTabTag = PdfViewCtrlTabsManager.getInstance().getLatestViewedTabTag(activity);
                if (latestViewedTabTag != null) {
                    ArrayList<String> documents = PdfViewCtrlTabsManager.getInstance().getDocuments(activity);
                    for (String document : documents) {
                        if (!latestViewedTabTag.equals(document)) {
                            PdfViewCtrlTabsManager.getInstance().removeDocument(activity, document);
                        }
                    }
                }
            }
        }
        // add new document to tab list
//        if (canAddNewDocumentToTabList(startupItemSource)) {
        PdfViewCtrlTabsManager.getInstance().addDocument(activity, mStartupTabTag);
//        }
        // remove extra tabs
        if (mMultiTabModeEnabled) {
            removeExtraTabs();
        }

        // add tabs
        ArrayList<String> documents = PdfViewCtrlTabsManager.getInstance().getDocuments(activity);
        for (String tag : documents) {
            if (mTabLayout.getTabByTag(tag) != null) {
                // it has already been added
                continue;
            }
            if (!mMultiTabModeEnabled && mStartupTabTag != null) {
                if (!tag.equals(mStartupTabTag)) {
                    continue;
                }
            }
            PdfViewCtrlTabInfo info = PdfViewCtrlTabsManager.getInstance().getPdfFViewCtrlTabInfo(activity, tag);
            int itemSource = BaseFileInfo.FILE_TYPE_UNKNOWN;
            String title = "";
            String fileExtension = null;
            String password = "";

            if (info != null) {
                itemSource = info.tabSource;
                title = info.tabTitle;
                fileExtension = info.fileExtension;
            }

            if (args != null && tag.equals(mStartupTabTag)) {
                itemSource = startupItemSource;
                password = args.getString(PdfViewCtrlTabBaseFragment.BUNDLE_TAB_PASSWORD);
                title = startupTitle;
                try {
                    // get rid of the extension so tab title looks more user friendly
                    int index = FilenameUtils.indexOfExtension(title);
                    if (index != -1 && title != null) {
                        title = title.substring(0, index);
                        args.putString(PdfViewCtrlTabBaseFragment.BUNDLE_TAB_TITLE, title);
                    }
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
                fileExtension = startupFileExtension;
            }
            if (canAddNewDocumentToTabList(itemSource) && !Utils.isNullOrEmpty(title)) {
                // args may contain more info that should pass to the viewer fragment
                addTab(args, tag, title, fileExtension, password, itemSource);
            }
        }
        if (documents.isEmpty()) {
            onLastTabClosed();
        }

        if (mStartupTabTag == null) {
            mStartupTabTag = PdfViewCtrlTabsManager.getInstance().getLatestViewedTabTag(activity);
        }
        setCurrentTabByTag(mStartupTabTag);
    }

    /**
     * The overload implementation of {@link SearchResultsView.SearchResultsListener#onSearchResultClicked(TextSearchResult)}.
     */
    @Override
    public void onSearchResultClicked(TextSearchResult result) {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        Activity activity = getActivity();
        if (activity == null || currentFragment == null) {
            return;
        }

        currentFragment.highlightFullTextSearchResult(result);
        currentFragment.setCurrentPageHelper(result.getPageNum(), false);

        if (!Utils.isTablet(activity)) {
            hideSearchResults();
        }
    }

    /**
     * The overload implementation of {@link SearchResultsView.SearchResultsListener#onFullTextSearchStart()}.
     */
    @Override
    public void onFullTextSearchStart() {
        if (mSearchToolbar != null) {
            mSearchToolbar.setSearchProgressBarVisible(true);
        }
    }

    /**
     * The overload implementation of {@link SearchResultsView.SearchResultsListener#onSearchResultFound(TextSearchResult)}.
     */
    @Override
    public void onSearchResultFound(TextSearchResult result) {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (mSearchToolbar != null) {
            mSearchToolbar.setSearchProgressBarVisible(false);
        }
        if (result != null && currentFragment != null) {
            currentFragment.highlightFullTextSearchResult(result);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (isHidden()) {
            return;
        }

        if (PdfViewCtrlSettingsManager.getFullScreenMode(activity)) {
            setToolbarsVisible(false);
            hideSystemUI();
        }

        if (mSearchResultsView != null) {
            PaneBehavior paneBehavior = PaneBehavior.from(mSearchResultsView);
            if (paneBehavior != null) {
                paneBehavior.onOrientationChanged(mSearchResultsView, newConfig.orientation);
            }
        }

        updateTabLayout();
    }

    /**
     * The overload implementation of {@link BookmarksTabLayout.BookmarksTabsListener#onUserBookmarkClicked(int)}.
     */
    @Override
    public void onUserBookmarkClick(int pageNum) {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment != null) {
            if (!currentFragment.isNavigationListShowing() && mBookmarksDialog != null) {
                mBookmarksDialog.dismiss();
            }
            currentFragment.setCurrentPageHelper(pageNum, true);
        }
    }

    /**
     * The overload implementation of {@link BookmarksTabLayout.BookmarksTabsListener#onOutlineClicked(Bookmark, Bookmark)}.
     */
    @Override
    public void onOutlineClicked(Bookmark parent, Bookmark bookmark) {
        // Save the parent bookmark of the clicked bookmark
        mCurrentBookmark = bookmark;

        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment != null) {
            if (!currentFragment.isNavigationListShowing() && mBookmarksDialog != null) {
                mBookmarksDialog.dismiss();
            }
            PDFViewCtrl pdfViewCtrl = currentFragment.getPDFViewCtrl();
            if (pdfViewCtrl != null) {
                currentFragment.setCurrentPageHelper(pdfViewCtrl.getCurrentPage(), false);
            }
        }
    }

    /**
     * The overload implementation of {@link BookmarksTabLayout.BookmarksTabsListener#onAnnotationClicked(Annot, int)}.
     */
    @Override
    public void onAnnotationClicked(Annot annotation, int pageNum) {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment != null) {
            if (!currentFragment.isNavigationListShowing() && mBookmarksDialog != null) {
                mBookmarksDialog.dismiss();
            }
            if (currentFragment.getToolManager() != null) {
                currentFragment.getToolManager().deselectAll();
                currentFragment.getToolManager().selectAnnot(annotation, pageNum);
            }
            currentFragment.setCurrentPageHelper(pageNum, false);
        }
    }

    /**
     * The overload implementation of {@link BookmarksTabLayout.BookmarksTabsListener#onExportAnnotations(PDFDoc)}.
     */
    @Override
    public void onExportAnnotations(PDFDoc pdfDoc) {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment != null) {
            if (!currentFragment.isNavigationListShowing() && mBookmarksDialog != null) {
                mBookmarksDialog.dismiss();
            }
            currentFragment.onExportAnnotations(pdfDoc);
        }
    }

    /**
     * The overload implementation of {@link UserCropSelectionDialogFragment.UserCropSelectionDialogFragmentListener#onUserCropMethodSelected(int)}.
     */
    @Override
    public void onUserCropMethodSelected(int cropMode) {
        FragmentActivity activity = getActivity();
        PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return;
        }

        currentFragment.save(false, true, false);

        final PDFViewCtrl pdfViewCtrl = currentFragment.getPDFViewCtrl();
        if (pdfViewCtrl == null) {
            return;
        }

        if (cropMode == UserCropSelectionDialogFragment.MODE_AUTO_CROP) {
            if (mAutoCropTask != null && mAutoCropTask.getStatus() == AsyncTask.Status.RUNNING) {
                mAutoCropTask.cancel(true);
            }
            mAutoCropTask = new UserCropUtilities.AutoCropInBackgroundTask(activity, pdfViewCtrl, this);
            mAutoCropTask.execute();
        } else if (cropMode == UserCropSelectionDialogFragment.MODE_MANUAL_CROP) {
            showUserCropDialog(pdfViewCtrl);
        } else {
            mDisposables.add(
                    UserCropUtilities.removeUserCropAsync(pdfViewCtrl.getDoc())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Action() {
                                @Override
                                public void run() throws Exception {
                                    ViewerUtils.safeUpdatePageLayout(pdfViewCtrl, new ExceptionHandlerCallback() {
                                        @Override
                                        public void onException(Exception e) {
                                            AnalyticsHandlerAdapter.getInstance().sendException(e);
                                        }
                                    });
                                }
                            }, new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Exception {
                                    AnalyticsHandlerAdapter.getInstance().sendException(new Exception(throwable));
                                }
                            }));
        }

        currentFragment.clearPageBackAndForwardStacks();
    }

    public void showUserCropDialog(PDFViewCtrl pdfViewCtrl) {
        mUserCropDialog = createUserCropDialog();
        mUserCropDialog.setOnUserCropDialogDismissListener(this);
        mUserCropDialog.setPdfViewCtrl(pdfViewCtrl);
        // Creates the dialog in full screen mode
        mUserCropDialog.setStyle(DialogFragment.STYLE_NO_TITLE, mThemeProvider.getTheme());
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            mUserCropDialog.show(fragmentManager, "usercrop_dialog");
        }
    }

    protected UserCropDialogFragment createUserCropDialog() {
        return UserCropDialogFragment.newInstance();
    }

    /**
     * The overload implementation of {@link UserCropSelectionDialogFragment.UserCropSelectionDialogFragmentListener#onUserCropSelectionDialogFragmentDismiss()}.
     */
    @Override
    public void onUserCropSelectionDialogFragmentDismiss() {
        resetHideToolbarsTimer();
    }

    /**
     * The overload implementation of {@link UserCropDialogFragment.OnUserCropDialogDismissListener#onUserCropDialogDismiss(int)}.
     */
    @Override
    public void onUserCropDialogDismiss(int pageNumberAtDismiss) {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }
        currentFragment.setCurrentPageHelper(pageNumberAtDismiss, true);
        currentFragment.userCropDialogDismiss();
    }

    /**
     * The overload implementation of {@link UserCropUtilities.AutoCropInBackgroundTask.AutoCropTaskListener#onAutoCropTaskDone()}.
     */
    @Override
    public void onAutoCropTaskDone() {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }
        currentFragment.userCropDialogDismiss();
    }

    /**
     * The overload implementation of {@link ThumbnailsViewFragment.OnThumbnailsViewDialogDismissListener#onThumbnailsViewDialogDismiss(int, boolean)}.
     */
    @Override
    public void onThumbnailsViewDialogDismiss(int pageNum, boolean docPagesModified) {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }

        currentFragment.onThumbnailsViewDialogDismiss(pageNum, docPagesModified);
    }

    /**
     * The overload implementation of {@link ThumbnailsViewFragment.OnThumbnailsEditAttemptWhileReadOnlyListener#onThumbnailsEditAttemptWhileReadOnly()}.
     */
    @Override
    public void onThumbnailsEditAttemptWhileReadOnly() {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }

        currentFragment.showReadOnlyAlert(mThumbFragment);
    }

    /**
     * The overload implementation of {@link ThumbnailsViewFragment.OnExportThumbnailsListener#onExportThumbnails(SparseBooleanArray)}.
     */
    @Override
    public void onExportThumbnails(SparseBooleanArray pageNums) {
        FragmentActivity activity = getActivity();
        PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return;
        }
        if (currentFragment.mTabSource == BaseFileInfo.FILE_TYPE_FILE) {
            handleThumbnailsExport(currentFragment.mCurrentFile.getParentFile(), pageNums);
        } else if (currentFragment.mTabSource == BaseFileInfo.FILE_TYPE_EXTERNAL) {
            ExternalFileInfo fileInfo = Utils.buildExternalFile(activity, currentFragment.mCurrentUriFile);
            if (fileInfo != null) {
                handleThumbnailsExport(fileInfo.getParent(), pageNums);
            }
        }
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabBaseFragment.TabListener#onTabDocumentLoaded(String)}.
     */
    @Override
    public void onTabDocumentLoaded(String tag) {
        // theme
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment != null) {
            ToolManager toolManager = currentFragment.getToolManager();
            if (toolManager != null) {
                toolManager.setThemeProvider(mThemeProvider);
            }
        }

        setToolbarsVisible(true, false);

        // update print summary annotations modes
        updatePrintDocumentMode();
        updatePrintAnnotationsMode();
        updatePrintSummaryMode();

        // update view mode button icon
        updateButtonViewModeIcon();

        // update undo button visibility
        updateUndoRedoState();
        // update share button visibility
        updateShareButtonVisibility(true);

        // update buttons when in reflow mode
        updateIconsInReflowMode();

        if (mStartupTabTag != null && mStartupTabTag.equals(tag)) {
            setCurrentTabByTag(mStartupTabTag);
        }

        if (mAutoCropTaskPaused && mAutoCropTaskTabTag != null && mAutoCropTaskTabTag.equals(getCurrentTabTag())) {
            mAutoCropTaskPaused = false;
            onUserCropMethodSelected(UserCropSelectionDialogFragment.MODE_AUTO_CROP);
        }

        setupRedaction();

        if (mTabHostListeners != null) {
            for (TabHostListener listener : mTabHostListeners) {
                listener.onTabDocumentLoaded(tag);
            }
        }
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabBaseFragment.TabListener#onTabError(int, String)}.
     */
    @Override
    public void onTabError(int errorCode, String info) {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }

        if (currentFragment.isOpenFileFailed()) {
            AnalyticsHandlerAdapter.getInstance().setString(AnalyticsHandlerAdapter.CustomKeys.TAB_ERROR,
                    String.format(Locale.US, "Error code %d: %s", errorCode, info));
            if (canShowOpenFileError()) {
                handleOpenFileFailed(errorCode, info);
            }
        }
    }

    protected boolean canShowOpenFileError() {
        boolean canShow = true;
        if (mTabHostListeners != null) {
            for (TabHostListener listener : mTabHostListeners) {
                if (listener.onOpenDocError()) {
                    // listener consumed the event
                    canShow = false;
                }
            }
        }
        return canShow;
    }

    /**
     * Creates and opens a new tab.
     *
     * @param args The arguments needed to create a new tab
     */
    public void onOpenAddNewTab(Bundle args) {
        if (args != null) {
            String tag = args.getString(PdfViewCtrlTabBaseFragment.BUNDLE_TAB_TAG);
            String title = args.getString(PdfViewCtrlTabBaseFragment.BUNDLE_TAB_TITLE);
            String password = args.getString(PdfViewCtrlTabBaseFragment.BUNDLE_TAB_PASSWORD);
            int itemSource = args.getInt(PdfViewCtrlTabBaseFragment.BUNDLE_TAB_ITEM_SOURCE);
            int initialPage = args.getInt(PdfViewCtrlTabBaseFragment.BUNDLE_TAB_INITIAL_PAGE, -1);
            onOpenAddNewTab(itemSource, tag, title, password, initialPage);
        }
    }

    public void onOpenAddNewTab(int itemSource, String tag, String title, String password) {
        onOpenAddNewTab(itemSource, tag, title, password, -1);
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabBaseFragment.TabListener#onOpenAddNewTab(int, String, String, String, int)}.
     */
    @Override
    public void onOpenAddNewTab(int itemSource, String tag, String title, String password, int initialPage) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        // error checking
        if (Utils.isNullOrEmpty(tag)
                || Utils.isNullOrEmpty(title)
                || (itemSource == BaseFileInfo.FILE_TYPE_FILE && !Utils.isNotPdf(tag) && !(new File(tag).exists()))) {

            if (canShowOpenFileError()) {
                CommonToast.showText(activity, R.string.error_opening_doc_message, Toast.LENGTH_SHORT);
            }
            return;
        }

        mFileSystemChanged.set(true);

        String fileExtension = FilenameUtils.getExtension(title);
        String name = FilenameUtils.removeExtension(title);
        TabLayout.Tab newTab = addTab(null, tag, name, fileExtension, password, itemSource, initialPage);
        newTab.select();

        // remove tabs after adding new tab to avoid any confusions from indexes shift
        // add new document to tab list
        if (itemSource != BaseFileInfo.FILE_TYPE_OPEN_URL) {
            // we will add open url files separately
            PdfViewCtrlTabsManager.getInstance().addDocument(activity, tag);
        }
        removeExtraTabs();
    }

    @Override
    public void onShowTabInfo(String tag, String title, String fileExtension, int itemSource, int duration) {
        handleShowTabInfo(tag, title, fileExtension, itemSource, duration);
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabBaseFragment.TabListener#onTabIdentityChanged(String, String, String, String, int)}.
     */
    @Override
    public void onTabIdentityChanged(String oldTabTag, String newTabTag, String newTabTitle,
            String newFileExtension, int newTabSource) {
        mFileSystemChanged.set(true);

        if (mTabLayout != null) {
            TabLayout.Tab tab = mTabLayout.getTabByTag(oldTabTag);
            if (tab != null) {
                mTabLayout.replaceTag(tab, newTabTag);
                setTabView(tab.getCustomView(), newTabTag, newTabTitle, newFileExtension, newTabSource);
            }
        }
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabBaseFragment.TabListener#onPageThumbnailOptionSelected(boolean, Integer)}.
     */
    @Override
    public void onPageThumbnailOptionSelected(boolean thumbnailEditMode, Integer checkedItem) {
        FragmentActivity activity = getActivity();
        PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return;
        }

        final PDFViewCtrl pdfViewCtrl = currentFragment.getPDFViewCtrl();
        if (pdfViewCtrl == null) {
            return;
        }

        // keep previously selected mode
        // display thumbnails view control
        if (checkTabConversionAndAlert(R.string.cant_edit_while_converting_message, true)) {
            return;
        }

        currentFragment.save(false, true, false);
        pdfViewCtrl.pause();

        boolean readonly = currentFragment.isTabReadOnly();
        if (!readonly) {
            if (mViewerConfig != null && !mViewerConfig.isThumbnailViewEditingEnabled()) {
                // if document is editable, user can specify if a particular control is editable
                readonly = true;
            }
            if (!pageThumbnailEditingEnabled()) {
                // for extended classes
                readonly = true;
            }
        }

        int[] hideFilterModes = mViewerConfig != null ? mViewerConfig.getHideThumbnailFilterModes() : null;
        String[] hideEditOptions = mViewerConfig != null ? mViewerConfig.getHideThumbnailEditOptions() : null;

        Bundle args = ThumbnailsViewFragment.createThumbnailViewFragmentBundle(readonly,
                thumbnailEditMode, hideFilterModes, hideEditOptions);

        mThumbFragment = getThumbnailViewFragment();
        mThumbFragment.setArguments(args);
        mThumbFragment.setPdfViewCtrl(pdfViewCtrl);
        mThumbFragment.setOnExportThumbnailsListener(this);
        mThumbFragment.setOnThumbnailsViewDialogDismissListener(this);
        mThumbFragment.setOnThumbnailsEditAttemptWhileReadOnlyListener(this);
        mThumbFragment.setStyle(DialogFragment.STYLE_NO_TITLE, mThemeProvider.getTheme());
        mThumbFragment.setTitle(getString(R.string.pref_viewmode_thumbnails_title));
        if (checkedItem != null) {
            mThumbFragment.setItemChecked(checkedItem - 1);
        }

        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            mThumbFragment.show(fragmentManager, ThumbnailsViewFragment.TAG);
        }
    }

    protected ThumbnailsViewFragment getThumbnailViewFragment() {
        return ThumbnailsViewFragment.newInstance();
    }

    protected boolean pageThumbnailEditingEnabled() {
        return true;
    }

    @Override
    public boolean onBackPressed() {
        return handleBackPressed();
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabBaseFragment.TabListener#onTabPaused(FileInfo, boolean)}.
     */
    @Override
    public void onTabPaused(
            FileInfo fileInfo,
            boolean isDocModifiedAfterOpening) {

        if (mTabHostListeners != null) {
            for (TabHostListener listener : mTabHostListeners) {
                listener.onTabPaused(fileInfo, isDocModifiedAfterOpening);
            }
        }
    }

    @Override
    public void onTabJumpToSdCardFolder() {
        if (mTabHostListeners != null) {
            for (TabHostListener listener : mTabHostListeners) {
                listener.onJumpToSdCardFolder();
            }
        }
    }

    @Override
    public void onUpdateOptionsMenu() {
        if (mToolbar != null) {
            onPrepareOptionsMenu(mToolbar.getMenu());
        }
    }

    @Override
    public boolean onHandleKeyShortcutEvent(int keyCode, KeyEvent event) {
        return handleKeyShortcutEvent(keyCode, event);
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabBaseFragment.TabListener#onInkEditSelected(Annot, int)}.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onInkEditSelected(Annot inkAnnot, int pageNum) {
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabBaseFragment.TabListener#onOpenAnnotationToolbar(ToolMode)}.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public abstract void onOpenAnnotationToolbar(ToolMode mode);

    /**
     * The overload implementation of {@link PdfViewCtrlTabBaseFragment.TabListener#onOpenEditToolbar(ToolMode)}.
     */
    @Override
    public abstract void onOpenEditToolbar(ToolMode mode);

    /**
     * The overload implementation of {@link PdfViewCtrlTabBaseFragment.TabListener#onToggleReflow()}.
     */
    @Override
    public void onToggleReflow() {
        if (mIsSearchMode) {
            exitSearchMode();
        }

        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }

        currentFragment.toggleReflow();
        updateIconsInReflowMode();

        if (mReflowControlListeners != null) {
            for (ReflowControlListener listener : mReflowControlListeners) {
                listener.onToggleReflowMode();
            }
        }
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabBaseFragment.TabListener#onFullTextSearchFindText(boolean)}.
     */
    @Override
    public SearchResultsView.SearchResultStatus onFullTextSearchFindText(boolean searchUp) {
        SearchResultsView.SearchResultStatus status = SearchResultsView.SearchResultStatus.NOT_HANDLED;
        if (mSearchResultsView != null && mSearchResultsView.isActive()) {
            status = mSearchResultsView.getResult(searchUp);
        }
        return status;
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabBaseFragment.TabListener#onTabThumbSliderStopTrackingTouch()}.
     */
    @Override
    public void onTabThumbSliderStopTrackingTouch() {
        resetHideToolbarsTimer();
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabBaseFragment.TabListener#onTabSingleTapConfirmed()}.
     */
    @Override
    public abstract void onTabSingleTapConfirmed();

    /**
     * The overload implementation of {@link PdfViewCtrlTabBaseFragment.TabListener#onSearchProgressShow()}.
     */
    @Override
    public void onSearchProgressShow() {
        if (mSearchToolbar != null) {
            mSearchToolbar.setSearchProgressBarVisible(true);
        }
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabBaseFragment.TabListener#onSearchProgressHide()}.
     */
    @Override
    public void onSearchProgressHide() {
        if (mSearchToolbar != null) {
            mSearchToolbar.setSearchProgressBarVisible(false);
        }
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabBaseFragment.TabListener#getToolbarHeight()}.
     */
    @Override
    public int getToolbarHeight() {
        if (mToolbar != null && mToolbar.isShown()) {
            if (isInFullScreenMode()) {
                return mAppBarLayout.getHeight();
            } else {
                return mToolbar.getHeight() + mTabLayout.getHeight();
            }
        }
        return -1;
    }

    public boolean isInFullScreenMode() {
        Activity activity = getActivity();
        if (activity != null) {
            return ViewerUtils.isInFullScreenMode(activity);
        }
        return false;
    }

    public Toolbar getToolbar() {
        return mToolbar;
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabBaseFragment.TabListener#onDownloadedSuccessful()}.
     */
    @Override
    public void onDownloadedSuccessful() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        final String currentTabTag = getCurrentTabTag();
        if (currentTabTag != null) {
            PdfViewCtrlTabsManager.getInstance().addDocument(activity, currentTabTag);
        } else {
            PdfViewCtrlTabsManager.getInstance().addDocument(activity, mStartupTabTag);
        }
        removeExtraTabs();
    }

    /**
     * Adds the {@link TabHostListener} listener.
     *
     * @param listener The listener
     */
    public void addHostListener(TabHostListener listener) {
        if (mTabHostListeners == null) {
            mTabHostListeners = new ArrayList<>();
        }
        if (!mTabHostListeners.contains(listener)) {
            mTabHostListeners.add(listener);
        }
    }

    /**
     * Removes the {@link TabHostListener} listener.
     *
     * @param listener The listener
     */
    @SuppressWarnings("unused")
    public void removeHostListener(TabHostListener listener) {
        if (mTabHostListeners != null) {
            mTabHostListeners.remove(listener);
        }
    }

    /**
     * Sets the {@link AppBarVisibilityListener} listener.
     *
     * @param listener The listener
     */
    public void setAppBarVisibilityListener(AppBarVisibilityListener listener) {
        mAppBarVisibilityListener = listener;
    }

    /**
     * Adds a {@link ReflowControlListener} listener.
     *
     * @param listener The listener
     */
    public void addReflowControlListener(ReflowControlListener listener) {
        if (mReflowControlListeners == null) {
            mReflowControlListeners = new ArrayList<>();
        }
        if (!mReflowControlListeners.contains(listener)) {
            mReflowControlListeners.add(listener);
        }
    }

    /**
     * Removes the {@link ReflowControlListener} listener.
     *
     * @param listener The listener
     */
    @SuppressWarnings("unused")
    public void removeReflowControlListener(ReflowControlListener listener) {
        if (mReflowControlListeners != null) {
            mReflowControlListeners.remove(listener);
        }
    }

    /**
     * Removes all {@link TabHostListener} listeners.
     */
    @SuppressWarnings("unused")
    public void clearHostListeners() {
        if (mTabHostListeners != null) {
            mTabHostListeners.clear();
        }
    }

    /**
     * Removes all {@link ReflowControlListener} listeners.
     */
    @SuppressWarnings("unused")
    public void clearReflowControlListeners() {
        if (mReflowControlListeners != null) {
            mReflowControlListeners.clear();
        }
    }

    public void onSaveAsOptionSelected() {
        final PdfViewCtrlTabBaseFragment fragment = getCurrentPdfViewCtrlFragment();
        if (fragment != null) {
            fragment.save(false, true, true);
            fragment.handleSaveAsCopy();
        }
    }

    public void onFlattenOptionSelected() {
        if (checkTabConversionAndAlert(R.string.cant_save_while_converting_message, false, true)) {
            return;
        }

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        String msg = String.format(getString(R.string.dialog_flatten_message), getString(R.string.app_name));
        String title = getString(R.string.dialog_flatten_title);

        Utils.getAlertDialogBuilder(activity, msg, title)
                .setPositiveButton(R.string.tools_qm_flatten, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final PdfViewCtrlTabBaseFragment fragment = getCurrentPdfViewCtrlFragment();
                        if (fragment != null) {
                            fragment.save(false, true, true);
                            fragment.handleSaveFlattenedCopy();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .create().show();
    }

    public void onSaveOptimizedCopySelected() {
        if (checkTabConversionAndAlert(R.string.cant_save_while_converting_message, false, true)) {
            return;
        }

        PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }

        currentFragment.save(false, true, true);

        OptimizeDialogFragment dialog = OptimizeDialogFragment.newInstance();
        dialog.setListener(new OptimizeDialogFragment.OptimizeDialogFragmentListener() {
            @Override
            public void onOptimizeClicked(OptimizeParams result) {
                final PdfViewCtrlTabBaseFragment fragment = getCurrentPdfViewCtrlFragment();
                if (fragment == null) {
                    return;
                }
                fragment.handleSaveOptimizedCopy(result);
            }
        });
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            dialog.show(fragmentManager, "optimize_dialog");
        }
    }

    public void onSavePasswordCopySelected() {
        if (checkTabConversionAndAlert(R.string.cant_save_while_converting_message, false, true)) {
            return;
        }

        final PdfViewCtrlTabBaseFragment fragment = getCurrentPdfViewCtrlFragment();
        if (fragment != null) {
            fragment.save(false, true, true);
            fragment.handleSavePasswordCopy();
        }
    }

    public void onSaveCroppedCopySelected() {
        if (checkTabConversionAndAlert(R.string.cant_save_while_converting_message, false)) {
            return;
        }

        final FragmentActivity activity = getActivity();
        final PdfViewCtrlTabBaseFragment fragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || fragment == null) {
            return;
        }
        final PDFViewCtrl pdfViewCtrl = fragment.getPDFViewCtrl();
        if (pdfViewCtrl == null) {
            return;
        }
        fragment.save(false, true, true);

        final ProgressDialog progressDialog = new ProgressDialog(activity);

        mDisposables.add(fragment.hasUserCropBoxDisposable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        progressDialog.setMessage(getString(R.string.save_crop_wait));
                        progressDialog.setCancelable(false);
                        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        progressDialog.setIndeterminate(true);
                    }
                })
                .subscribe(new Consumer<Boolean>() {
                               @Override
                               public void accept(Boolean params) throws Exception {
                                   progressDialog.dismiss();

                                   pdfViewCtrl.requestRendering();
                                   if (progressDialog.isShowing()) {
                                       progressDialog.dismiss();
                                   }
                                   if (params != null) {
                                       if (params) {
                                           fragment.handleSaveCroppedCopy();
                                       } else {
                                           AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                                           builder.setMessage(getString(R.string.save_crop_no_cropbox_warning_msg))
                                                   .setCancelable(true);
                                           int posButton = R.string.save_crop_no_cropbox_warning_positive;
                                           int negButton = R.string.cancel;

                                           builder.setPositiveButton(posButton, new DialogInterface.OnClickListener() {
                                               @Override
                                               public void onClick(DialogInterface dialog, int which) {
                                                   dialog.dismiss();

                                                   UserCropSelectionDialogFragment cropDialog = UserCropSelectionDialogFragment.newInstance();
                                                   cropDialog.setUserCropSelectionDialogFragmentListener(PdfViewCtrlTabHostBaseFragment.this);
                                                   FragmentManager fragmentManager = getFragmentManager();
                                                   if (fragmentManager != null) {
                                                       cropDialog.show(fragmentManager, "user_crop_mode_picker");
                                                   }
                                                   stopHideToolbarsTimer();
                                               }
                                           }).setNegativeButton(negButton, new DialogInterface.OnClickListener() {
                                               @Override
                                               public void onClick(DialogInterface dialog, int which) {
                                                   dialog.dismiss();
                                               }
                                           }).create().show();
                                       }
                                   }
                               }
                           }, new Consumer<Throwable>() {
                               @Override
                               public void accept(Throwable throwable) throws Exception {
                                   progressDialog.dismiss();
                               }
                           }
                ));
    }

    @SuppressLint("RestrictedApi")
    protected void initViews() {
        final FragmentActivity activity = getActivity();
        if (activity == null || mFragmentView == null) {
            return;
        }

        mFragmentView.addOnLayoutChangeListener(this);

        if (Utils.isKitKat()) {
            // See {@link #onSystemUiVisibilityChange(int)}.
            mFragmentView.setOnSystemUiVisibilityChangeListener(this);
            mLastSystemUIVisibility = mFragmentView.getWindowSystemUiVisibility();
        }

        mRootView = mFragmentView.findViewById(R.id.pdfviewctrl_tab_host);

        mTabLayout = mFragmentView.findViewById(R.id.doc_tabs);
        mTabLayout.setup(activity,
                getChildFragmentManager(),
                getContainerId()
        );
        mTabLayout.addOnTabSelectedListener(this);

        mToolbar = mFragmentView.findViewById(R.id.toolbar);
        if (mViewerConfig != null && !mViewerConfig.isShowTopToolbar()) {
            mToolbar.setVisibility(View.GONE);
        }
        if (!useSupportActionBar()) {
            if (mViewerConfig != null && !Utils.isNullOrEmpty(mViewerConfig.getToolbarTitle())) {
                mToolbar.setTitle(mViewerConfig.getToolbarTitle());
            }
            onCreateOptionsMenu(mToolbar.getMenu(), new MenuInflater(activity));
            mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    return onOptionsItemSelected(menuItem);
                }
            });
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleNavIconClick();
                }
            });
            mToolbar.setMenuCallbacks(null, new MenuBuilder.Callback() {
                @Override
                public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
                    return false;
                }

                @Override
                public void onMenuModeChange(MenuBuilder menu) {
                    onUpdateOptionsMenu();
                }
            });
        }

        mSearchToolbar = mFragmentView.findViewById(R.id.search_toolbar);
        mSearchToolbar.setSearchToolbarListener(new SearchToolbar.SearchToolbarListener() {

            @Override
            public void onExitSearch() {
                // onBackPressed is skipped when a support ActionBar menu-item is expanded,
                // so hide the search results list before exiting search mode.
                if (mSearchResultsView != null && mSearchResultsView.getVisibility() == View.VISIBLE) {
                    hideSearchResults();
                } else {
                    exitSearchMode();
                }
            }

            @Override
            public void onClearSearchQuery() {
                // Cancel search
                final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
                if (currentFragment == null) {
                    return;
                }
                currentFragment.cancelFindText();
                if (mSearchResultsView != null) {
                    if (mSearchResultsView.isActive()) {
                        mSearchResultsView.cancelGetResult();
                    }
                    hideSearchResults();
                }
            }

            @Override
            public void onSearchQuerySubmit(String s) {
                final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
                if (currentFragment != null) {
                    currentFragment.queryTextSubmit(s);
                }

                if (mSearchResultsView != null) {
                    mSearchResultsView.findText(s);
                }
            }

            @Override
            public void onSearchQueryChange(String s) {
                final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
                if (currentFragment != null) {
                    currentFragment.setSearchQuery(s);
                }

                if (mSearchResultsView != null && mSearchResultsView.isActive() && !mSearchResultsView.getSearchPattern().equals(s)) {
                    mSearchResultsView.cancelGetResult();
                }
            }

            @Override
            public void onSearchOptionsItemSelected(MenuItem item, String searchQuery) {
                final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
                if (currentFragment == null) {
                    return;
                }
                final int id = item.getItemId();
                if (id == R.id.action_list_all) {
                    if (currentFragment.isDocumentReady()) {
                        onListAllOptionSelected(searchQuery);
                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_SEARCH_LIST_ALL);
                    }
                } else if (id == R.id.action_match_case) {
                    if (currentFragment.isDocumentReady()) {
                        boolean isChecked = item.isChecked();
                        onSearchMatchCaseOptionSelected(!isChecked);
                        item.setChecked(!isChecked);
                    }
                } else if (id == R.id.action_whole_word) {
                    if (currentFragment.isDocumentReady()) {
                        boolean isChecked = item.isChecked();
                        onSearchWholeWordOptionSelected(!isChecked);
                        item.setChecked(!isChecked);
                    }
                }
            }
        });

        updateToolbarDrawable();

        mAppBarLayout = mFragmentView.findViewById(R.id.app_bar_layout);
        if (mViewerConfig != null && !mViewerConfig.isShowAppBar()) {
            mAppBarLayout.setVisibility(View.GONE);
        }

        mFragmentContainer = mFragmentView.findViewById(R.id.realtabcontent);
        if (mFragmentContainer != null) {
            // The following listener will only be used for v21+.
            // When *not* in fullscreen mode, the tab fragment container needs to use the insets as
            // padding so that the tab fragments are not obscured by the system bars.
            ViewCompat.setOnApplyWindowInsetsListener(mFragmentContainer, new OnApplyWindowInsetsListener() {
                @Override
                public WindowInsetsCompat onApplyWindowInsets(View view, WindowInsetsCompat insets) {
                    WindowInsetsCompat result = insets;
                    Context context = (view != null) ? view.getContext() : null;
                    if (context != null && !PdfViewCtrlSettingsManager.getFullScreenMode(context)) {
                        // Apply the default insets policy handler.
                        try {
                            // This will throw exception on motorola devices with "Screen on the Edges" feature
                            result = ViewCompat.onApplyWindowInsets(view, insets);
                        } catch (Exception ignored) {
                        }
                    }

                    // screen cutout
                    DisplayCutoutCompat cutout = insets.getDisplayCutout();
                    final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
                    if (cutout != null) {
                        // we found cutout!
                        // only handle cutout if hiding toolbars

                        if (currentFragment != null && mAppBarLayout != null && !mWillShowAnnotationToolbar) {
                            if (mAppBarLayout.getVisibility() == View.VISIBLE) {
                                currentFragment.applyCutout(0, 0);
                            } else {
                                currentFragment.applyCutout(cutout.getSafeInsetTop(), cutout.getSafeInsetBottom());
                            }
                        }
                    }

                    mSystemWindowInsetTop = result.getSystemWindowInsetTop();
                    mSystemWindowInsetBottom = result.getSystemWindowInsetBottom();
                    if (cutout != null) {
                        handleSystemWindowInsetChanged(cutout.getSafeInsetTop(), cutout.getSafeInsetBottom());
                    }

                    return result;
                }
            });
        }
    }

    protected void handleSystemWindowInsetChanged(int insetTop, int insetBottom) {
        // no op
    }

    /**
     * Adds a new tab
     *
     * @param args          The argument needed to create PdfViewCtrlTabBaseFragment
     * @param tag           The tab tag
     * @param title         The title
     * @param fileExtension The file extension
     * @param password      The password
     * @param itemSource    The item source of the document
     * @return The created tab
     */
    public TabLayout.Tab addTab(@Nullable Bundle args, String tag, String title, String fileExtension, String password, int itemSource) {
        return addTab(args, tag, title, fileExtension, password, itemSource, -1);
    }

    /**
     * Adds a new tab
     *
     * @param args          The argument needed to create PdfViewCtrlTabBaseFragment
     * @param tag           The tab tag
     * @param title         The title
     * @param fileExtension The file extension
     * @param password      The password
     * @param itemSource    The item source of the document
     * @param initialPage   The initial page to scroll to when the document is opened, unused if less than 1
     * @return The created tab
     */
    public TabLayout.Tab addTab(@Nullable Bundle args, String tag, String title, String fileExtension, String password, int itemSource, int initialPage) {
        if (!tag.equals(mStartupTabTag) || args == null) {
            boolean useCacheFolder = true;
            if (args != null) {
                useCacheFolder = args.getBoolean(UriCacheManager.BUNDLE_USE_CACHE_FOLDER, true);
            }
            args = PdfViewCtrlTabBaseFragment.createBasicPdfViewCtrlTabBundle(tag, title,
                    fileExtension, password, itemSource, initialPage, mViewerConfig);
            args.putBoolean(UriCacheManager.BUNDLE_USE_CACHE_FOLDER, useCacheFolder);
        }

        TabLayout.Tab tab = createTab(tag, title, fileExtension, itemSource);
        if (tab != null) {
            mTabLayout.addTab(tab, mTabFragmentClass, args);
        }

        return tab;
    }

    /**
     * Creates a tab.
     *
     * @param tag           The tab tag
     * @param title         The title of tab
     * @param fileExtension The file extension
     * @param itemSource    The item source of the file
     * @return The tab
     */
    protected TabLayout.Tab createTab(final String tag, final String title, final String fileExtension, final int itemSource) {
        TabLayout.Tab tab = mTabLayout.newTab().setTag(tag).setCustomView(getTabLayoutRes());
        setTabView(tab.getCustomView(), tag, title, fileExtension, itemSource);
        return tab;
    }

    /**
     * Sets the tab view.
     *
     * @param view          The view
     * @param tag           The tab tag
     * @param title         The title fo tab
     * @param fileExtension The file extension
     * @param itemSource    The item source of the file
     */
    protected void setTabView(View view, final String tag, final String title,
            final String fileExtension, final int itemSource) {
        if (view == null) {
            return;
        }

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTabLayout != null) {
                    TabLayout.Tab currentTab = mTabLayout.getTabByTag(tag);
                    if (currentTab != null) {
                        currentTab.select();
                    }
                }
            }
        });

        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                handleShowTabInfo(tag, title, fileExtension, itemSource, Snackbar.LENGTH_LONG);
                return true;
            }
        });

        if (Utils.isMarshmallow()) {
            view.setOnContextClickListener(new View.OnContextClickListener() {
                @Override
                public boolean onContextClick(View v) {
                    return v.performLongClick();
                }
            });
        }

        TextView textView = view.findViewById(R.id.tab_pdfviewctrl_text);
        if (textView != null) {
            textView.setText(title);
            if (mTabLayout != null) {
                ColorStateList tint = mTabLayout.getTabTextColors();
                textView.setTextColor(tint);
            }
        }

        View closeButton = view.findViewById(R.id.tab_pdfviewctrl_close_button);
        if (closeButton != null) {
            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closeTab(tag, itemSource);
                }
            });
        }
    }

    /**
     * Removes the specified tab at index.
     *
     * @param index index of the tab
     */
    public void removeTabAt(int index) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (mTabLayout.getTabCount() > index && index >= 0) {
            TabLayout.Tab tab = mTabLayout.getTabAt(index);
            if (tab != null) {
                PdfViewCtrlTabsManager.getInstance().removeDocument(activity, (String) tab.getTag());
                mTabLayout.removeTab(tab);
            }
        }
    }

    /**
     * Removes the specified tab.
     *
     * @param filepath The file path
     */
    public void removeTab(String filepath) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        final String currentTabTag = getCurrentTabTag();
        if (currentTabTag == null) {
            return;
        }

        PdfViewCtrlTabsManager.getInstance().removeDocument(activity, filepath);

        // calculate new tab to be selected
        String nextTabTagToSelect = currentTabTag;
        if (currentTabTag.equals(filepath)) {
            // if current tab is closed, set new current tab to the most recently viewed tab
            nextTabTagToSelect = PdfViewCtrlTabsManager.getInstance().getLatestViewedTabTag(activity);
        }

        removeTab(filepath, nextTabTagToSelect);
    }

    /**
     * Removes the specified tab.
     *
     * @param filepath           The file path
     * @param nextTabTagToSelect The tab tag of the tab that should be selected thereafter
     */
    public void removeTab(String filepath, final String nextTabTagToSelect) {
        Activity activity = getActivity();
        if (activity == null || mTabLayout == null) {
            return;
        }

        if (Utils.isNullOrEmpty(filepath)) {
            return;
        }

        // first select the target tab and then remove the current tab; otherwise, TabLayout
        // will select the previous tab if the current tab is removed
        setCurrentTabByTag(nextTabTagToSelect);

        TabLayout.Tab closedTab = mTabLayout.getTabByTag(filepath);
        if (closedTab != null) {
            mTabLayout.removeTab(closedTab);
        }

        // selecting and removing tabs in one UI thread run may result in undesired behavior;
        // for example, if closedTab has lower index compared to nextTabTagToSelect then the tab
        // indicator will be set incorrectly. As a workaround we select tab later in the
        // next UI thread run
        mTabLayout.post(new Runnable() {
            @Override
            public void run() {
                setCurrentTabByTag(nextTabTagToSelect);
            }
        });

        if (mTabLayout.getTabCount() == 0) {
            onLastTabClosed();
        }
    }

    private void onLastTabClosed() {
        if (mTabHostListeners != null) {
            for (TabHostListener listener : mTabHostListeners) {
                listener.onLastTabClosed();
            }
        }
    }

    /**
     * Closes all tabs.
     */
    public void closeAllTabs() {
        Activity activity = getActivity();
        if (activity == null || mTabLayout == null) {
            return;
        }
        while (mTabLayout.getTabCount() > 0) {
            TabLayout.Tab tab = mTabLayout.getTabAt(0);
            if (tab != null) {
                PdfViewCtrlTabsManager.getInstance().removeDocument(activity, (String) tab.getTag());
                mTabLayout.removeTab(tab);
            }
        }
    }

    /**
     * Closes the specified tab.
     *
     * @param tag the tab tag
     */
    public void closeTab(final String tag) {
        Activity activity = getActivity();
        if (activity == null || mTabLayout == null) {
            return;
        }

        // if the tab is closed then shouldn't be added to PDFViewCrtTabsManager
        Fragment fragment = mTabLayout.getFragmentByTag(tag);
        if (fragment instanceof PdfViewCtrlTabBaseFragment) {
            closeTab(tag, ((PdfViewCtrlTabBaseFragment) fragment).getTabSource());
        } else {
            // tab source is used for snackbar,
            // here we will not use snackbar since tab source cannot be retrieved
            closeTab(tag, BaseFileInfo.FILE_TYPE_UNKNOWN);
        }
    }

    private void closeTab(final String tag, final int itemSource) {
        Activity activity = getActivity();
        if (activity == null || mTabLayout == null) {
            return;
        }

        // if the tab is closed then shouldn't be added to PDFViewCrtTabsManager
        Fragment fragment = mTabLayout.getFragmentByTag(tag);
        boolean isDocModified = false;
        boolean isTabReadOnly = true;
        PdfViewCtrlTabBaseFragment pdfViewCtrlTabFragment = null;
        if (fragment instanceof PdfViewCtrlTabBaseFragment) {
            pdfViewCtrlTabFragment = (PdfViewCtrlTabBaseFragment) fragment;
            isDocModified = pdfViewCtrlTabFragment.isDocModifiedAfterOpening();
            isTabReadOnly = pdfViewCtrlTabFragment.isTabReadOnly();
            pdfViewCtrlTabFragment.setCanAddToTabInfo(false);
        }

        if (mTabLayout.getTabCount() > 1) {
            final PdfViewCtrlTabInfo info = PdfViewCtrlTabsManager.getInstance().getPdfFViewCtrlTabInfo(activity, tag);

            if (itemSource != BaseFileInfo.FILE_TYPE_OPEN_URL &&
                    itemSource != BaseFileInfo.FILE_TYPE_UNKNOWN) {
                boolean canShowSnack = true;
                if (mTabHostListeners != null) {
                    for (TabHostListener listener : mTabHostListeners) {
                        if (!listener.canShowFileCloseSnackbar()) {
                            canShowSnack = false;
                        }
                    }
                }
                if (canShowSnack) {
                    String desc = getString(isDocModified && !isTabReadOnly ? R.string.snack_bar_tab_saved_and_closed : R.string.snack_bar_tab_closed);
                    showSnackbar(desc, getString(R.string.reopen),
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (info != null) {
                                        PdfViewCtrlTabsManager.getInstance().addDocument(v.getContext(), tag);
                                        addTab(null, tag, info.tabTitle, info.fileExtension, "", itemSource);
                                        setCurrentTabByTag(tag);

                                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_UNDO,
                                                AnalyticsParam.viewerUndoRedoParam("close_tab", AnalyticsHandlerAdapter.LOCATION_VIEWER));
                                    }
                                }
                            });
                }
                if (pdfViewCtrlTabFragment != null) {
                    pdfViewCtrlTabFragment.setSavedAndClosedShown();
                }
            }
        }

        removeTab(tag);
    }

    /**
     * Removes extra tabs.
     */
    public void removeExtraTabs() {
        Activity activity = getActivity();
        if (activity == null || mTabLayout == null) {
            return;
        }

        if (!PdfViewCtrlSettingsManager.getMultipleTabs(activity)) {
            while (mTabLayout.getTabCount() > 1) {
                TabLayout.Tab tab = mTabLayout.getTabAt(0);
                if (tab != null) {
                    PdfViewCtrlTabsManager.getInstance().removeDocument(activity, (String) tab.getTag());
                    mTabLayout.removeTab(tab);
                }
            }
            return;
        }

        while (PdfViewCtrlTabsManager.getInstance().getDocuments(activity).size() > getMaxTabCount()) {
            String removedTabTag = PdfViewCtrlTabsManager.getInstance().removeOldestViewedTab(activity);
            TabLayout.Tab removedTab = mTabLayout.getTabByTag(removedTabTag);
            if (removedTab != null) {
                mTabLayout.removeTab(removedTab);
            }
        }
    }

    /**
     * @return the maximum number of tabs for this fragment.
     */
    protected int getMaxTabCount() {
        Activity activity = getActivity();
        if (mViewerConfig != null && mViewerConfig.getMaximumTabCount() > 0) {
            return mViewerConfig.getMaximumTabCount();
        } else if (activity == null) {
            return 0;
        } else if (PdfViewCtrlSettingsManager.getUnlimitedTabsEnabled(activity, PdfViewCtrlSettingsManager.KEY_PREF_UNLIMITED_TABS_DEFAULT_VALUE)) {
            return 1000; // likely safe to assume users will not have more than 1000 tabs
        } else {
            return Utils.isTablet(activity)
                    ? PdfViewCtrlTabsManager.MAX_NUM_TABS_TABLET
                    : PdfViewCtrlTabsManager.MAX_NUM_TABS_PHONE;
        }
    }

    /**
     * The overload implementation of {@link PdfViewCtrlTabBaseFragment.TabListener#onUndoRedoPopupClosed()}.
     */
    @Override
    @Deprecated
    public void onUndoRedoPopupClosed() {
    }

    /**
     * Undoes the last operation.
     */
    protected void undo() {
        Activity activity = getActivity();
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return;
        }

        currentFragment.undo(false);
    }

    /**
     * Redoes the last undo.
     */
    protected void redo() {
        Activity activity = getActivity();
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return;
        }

        currentFragment.redo(false);
    }

    /**
     * Checks whether can add new document to the tab list.
     *
     * @param itemSource The item source
     * @return True if can add new document to the tab list
     */
    protected boolean canAddNewDocumentToTabList(int itemSource) {
        return true;
    }

    /**
     * Sets all needed listeners for PdfViewCtrlTabBaseFragment fragment
     *
     * @param fragment The PdfViewCtrlTabBaseFragment fragment
     */
    protected void setFragmentListeners(Fragment fragment) {
        if (fragment instanceof PdfViewCtrlTabBaseFragment) {
            PdfViewCtrlTabBaseFragment tabFragment = (PdfViewCtrlTabBaseFragment) fragment;
            tabFragment.setTabListener(this);
            tabFragment.addQuickMenuListener(this);
        }
    }

    protected void removeFragmentListeners(Fragment fragment) {
        if (fragment instanceof PdfViewCtrlTabBaseFragment) {
            PdfViewCtrlTabBaseFragment tabFragment = (PdfViewCtrlTabBaseFragment) fragment;
            tabFragment.removeQuickMenuListener(this);
        }
    }

    /**
     * Returns the selected {@link PdfViewCtrlTabBaseFragment}.
     *
     * @return The PdfViewCtrlTabBaseFragment
     */
    public PdfViewCtrlTabBaseFragment getCurrentPdfViewCtrlFragment() {
        if (mTabLayout == null) {
            return null;
        }
        Fragment fragment = mTabLayout.getCurrentFragment();
        if (fragment instanceof PdfViewCtrlTabBaseFragment) {
            return (PdfViewCtrlTabBaseFragment) fragment;
        }

        return null;
    }

    /**
     * Shows tab information in a snack bar
     *
     * @param message    The message
     * @param path       The file path
     * @param tag        The tab tag
     * @param itemSource The item source of the document
     */
    public void showTabInfo(String message, String path, String tag, final int itemSource, final int duration) {
        Activity activity = getActivity();
        final PdfViewCtrlTabBaseFragment fragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || fragment == null || fragment.getView() == null) {
            return;
        }

        final String filepath;
        if (itemSource == BaseFileInfo.FILE_TYPE_EXTERNAL) {
            Uri uri = Uri.parse(tag);
            ExternalFileInfo info = Utils.buildExternalFile(activity, uri);
            if (info != null) {
                String uriFilename = Uri.encode(info.getFileName());
                if (!Utils.isNullOrEmpty(uriFilename) && tag.endsWith(uriFilename)) {
                    filepath = tag.substring(0, tag.length() - uriFilename.length());
                } else {
                    filepath = "";
                }
            } else {
                filepath = "";
            }
        } else {
            filepath = path;
        }
        if (sDebug) {
            if (Utils.isNullOrEmpty(filepath)) {
                String tempPath = "";
                FileInfo info = fragment.getCurrentFileInfo();
                if (info != null) {
                    tempPath = info.getAbsolutePath();
                }
                CommonToast.showText(activity, "DEBUG: [" + itemSource + "] [" + tempPath + "]");
            } else {
                CommonToast.showText(activity, "DEBUG: [" + filepath + "]");
            }
        }
        final String filename = message;
        if ((itemSource == BaseFileInfo.FILE_TYPE_FILE
                || itemSource == BaseFileInfo.FILE_TYPE_OPEN_URL
                || itemSource == BaseFileInfo.FILE_TYPE_EXTERNAL
                || itemSource == BaseFileInfo.FILE_TYPE_EDIT_URI
                || itemSource == BaseFileInfo.FILE_TYPE_OFFICE_URI)
                && !Utils.isNullOrEmpty(filepath)) {
            View.OnClickListener snackbarActionListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String path = null;
                    String name = null;
                    if (mTabHostListeners != null) {
                        if (mTabLayout == null) {
                            return;
                        }
                        if (itemSource == BaseFileInfo.FILE_TYPE_OPEN_URL) {
                            ArrayList<Fragment> fragments = mTabLayout.getLiveFragments();
                            for (Fragment fragment : fragments) {
                                if (fragment instanceof PdfViewCtrlTabBaseFragment) {
                                    PdfViewCtrlTabBaseFragment pdfViewCtrlTabFragment = (PdfViewCtrlTabBaseFragment) fragment;
                                    if (pdfViewCtrlTabFragment.mTabTag.contains(filepath)
                                            && pdfViewCtrlTabFragment.mTabTag.contains(filename)) {
                                        String fullFilePath = pdfViewCtrlTabFragment.getFilePath();
                                        if (!Utils.isNullOrEmpty(fullFilePath)) {
                                            path = FilenameUtils.getPath(fullFilePath);
                                            name = FilenameUtils.getName(fullFilePath);
                                        }
                                    }
                                }
                            }
                        } else {
                            path = filepath;
                            name = filename;
                        }

                        for (TabHostListener listener : mTabHostListeners) {
                            listener.onShowFileInFolder(name, path, itemSource);
                        }
                    }
                }
            };
            showSnackbar(message, getString(R.string.snack_bar_file_info_message), snackbarActionListener, duration);
        } else {
            showSnackbar(message, null, null, duration);
        }
    }

    /**
     * Sets the visibility of thumbnail slider.
     *
     * @param visible True if visible
     * @param animate True if visibility should be changed with animation
     */
    public void setThumbSliderVisibility(boolean visible, boolean animate) {
        Activity activity = getActivity();
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return;
        }

        currentFragment.setThumbSliderVisible(visible, animate);
    }

    /**
     * Selects the tab that has the specified tag.
     *
     * @param tag The tab tag
     */
    public void setCurrentTabByTag(String tag) {
        if (tag == null || mTabLayout == null) {
            return;
        }

        try {
            for (int i = 0, sz = mTabLayout.getTabCount(); i < sz; ++i) {
                TabLayout.Tab tab = mTabLayout.getTabAt(i);
                if (tab != null) {
                    String tabTag = (String) tab.getTag();
                    if (tabTag != null && tabTag.equals(tag)) {
                        tab.select();
                        return;
                    }
                }
            }
        } catch (Exception ignored) {

        }
    }

    /**
     * @return The current selected tab tag
     */
    protected String getCurrentTabTag() {
        if (mTabLayout == null) {
            return null;
        }
        int curPosition = mTabLayout.getSelectedTabPosition();
        if (curPosition != -1) {
            TabLayout.Tab tab = mTabLayout.getTabAt(curPosition);
            if (tab != null) {
                return (String) tab.getTag();
            }
        }

        return null;
    }

    /**
     * @hide
     **/
    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        if (sDebug) {
            Log.d(TAG, "Tab " + tab.getTag() + " is selected");
        }
        Activity activity = getActivity();
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            // the document has not yet been ready, wait until it will be ready which will be notified
            // by PdfViewCtrlTabBaseFragment.TabListener.onTabDocumentLoaded
            return;
        }

        String tabTag = (String) tab.getTag();
        if (tabTag != null) {
            setFragmentListeners(mTabLayout.getFragmentByTag(tabTag));
        }

        if (mCurTabIndex != -1 && mCurTabIndex != tab.getPosition()) {
            if (mTabHostListeners != null) {
                for (TabHostListener listener : mTabHostListeners) {
                    listener.onTabChanged(tabTag);
                }
                mQuitAppWhenDoneViewing = false;
            }
        }
        mCurTabIndex = tab.getPosition();

        // reset last-used bookmark
        mCurrentBookmark = null;

        exitSearchMode();
        updateTabLayout();
        setToolbarsVisible(true, false);
        if (!currentFragment.isDocumentReady()) {
            // reset hide toolbars timer later when document is loaded
            stopHideToolbarsTimer();
        }

        // update print summary annotations modes
        updatePrintDocumentMode();
        updatePrintAnnotationsMode();
        updatePrintSummaryMode();

        // update view mode button icon
        updateButtonViewModeIcon();

        // update share button visibility
        updateShareButtonVisibility(true);

        // update buttons when in reflow mode
        updateIconsInReflowMode();
    }

    /**
     * @hide
     **/
    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        if (sDebug) {
            Log.d(TAG, "Tab " + tab.getTag() + " is unselected");
        }
        String tabTag = (String) tab.getTag();
        if (tabTag != null) {
            removeFragmentListeners(mTabLayout.getFragmentByTag(tabTag));
        }
    }

    /**
     * @hide
     **/
    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        onTabSelected(tab);
    }

    /**
     * The overloaded implementation of {@link ViewModePickerDialogFragment.ViewModePickerDialogFragmentListener#onViewModeColorSelected(int)}.
     **/
    @Override
    public boolean onViewModeColorSelected(int colorMode) {
        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }

        PdfViewCtrlSettingsManager.setColorMode(activity, colorMode);
        return updateColorMode();
    }

    /**
     * The overloaded implementation of {@link ViewModePickerDialogFragment.ViewModePickerDialogFragmentListener#onViewModeSelected(String)}.
     **/
    @Override
    public void onViewModeSelected(String viewMode) {
        onViewModeSelected(viewMode, false, null);
    }

    /**
     * Handles when view mode is selected.
     *
     * @param viewMode          the view mode
     * @param thumbnailEditMode True if thumbnail is in edit mode
     * @param checkedItem       The checked item
     */
    public void onViewModeSelected(String viewMode, boolean thumbnailEditMode, Integer checkedItem) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }
        final PDFViewCtrl pdfViewCtrl = currentFragment.getPDFViewCtrl();
        if (pdfViewCtrl == null) {
            return;
        }

        // Update per document view mode setting
        PDFViewCtrl.PagePresentationMode mode = PDFViewCtrl.PagePresentationMode.SINGLE;
        boolean updateViewMode = false;
        FragmentManager fragmentManager = getFragmentManager();
        switch (viewMode) {
            case PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_CONTINUOUS_VALUE:
                mode = PDFViewCtrl.PagePresentationMode.SINGLE_CONT;
                PdfViewCtrlSettingsManager.updateViewMode(activity, PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_CONTINUOUS_VALUE);
                updateViewMode = true;
                break;
            case PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_SINGLEPAGE_VALUE:
                mode = PDFViewCtrl.PagePresentationMode.SINGLE;
                PdfViewCtrlSettingsManager.updateViewMode(activity, PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_SINGLEPAGE_VALUE);
                updateViewMode = true;
                break;
            case PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_FACING_VALUE:
                mode = PDFViewCtrl.PagePresentationMode.FACING;
                PdfViewCtrlSettingsManager.updateViewMode(activity, PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_FACING_VALUE);
                updateViewMode = true;
                break;
            case PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_FACINGCOVER_VALUE:
                mode = PDFViewCtrl.PagePresentationMode.FACING_COVER;
                PdfViewCtrlSettingsManager.updateViewMode(activity, PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_FACINGCOVER_VALUE);
                updateViewMode = true;
                break;
            case PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_FACING_CONT_VALUE:
                mode = PDFViewCtrl.PagePresentationMode.FACING_CONT;
                PdfViewCtrlSettingsManager.updateViewMode(activity, PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_FACING_CONT_VALUE);
                updateViewMode = true;
                break;
            case PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_FACINGCOVER_CONT_VALUE:
                mode = PDFViewCtrl.PagePresentationMode.FACING_COVER_CONT;
                PdfViewCtrlSettingsManager.updateViewMode(activity, PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_FACINGCOVER_CONT_VALUE);
                updateViewMode = true;
                break;
            case PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_ROTATION_VALUE:
                pdfViewCtrl.rotateClockwise();
                ViewerUtils.safeUpdatePageLayout(pdfViewCtrl, new ExceptionHandlerCallback() {
                    @Override
                    public void onException(Exception e) {
                        AnalyticsHandlerAdapter.getInstance().sendException(e);
                    }
                });
                break;
            case PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_THUMBNAILS_VALUE:
                onPageThumbnailOptionSelected(thumbnailEditMode, checkedItem);
                break;
            case PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_USERCROP_VALUE:
                if (!currentFragment.isDocumentReady()) {
                    return;
                }
                UserCropSelectionDialogFragment dialog = UserCropSelectionDialogFragment.newInstance();
                dialog.setUserCropSelectionDialogFragmentListener(this);
                if (fragmentManager != null) {
                    dialog.show(fragmentManager, "user_crop_mode_picker");
                }
                break;
            case PdfViewCtrlSettingsManager.KEY_PREF_REFLOWMODE:
                onToggleReflow();
                break;
            case PdfViewCtrlSettingsManager.KEY_PREF_RTLMODE:
                onToggleRtlMode();
                break;
            case PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_READING_SETTINGS_VALUE:

                ReadingModeSettingsDialog readingModeSettingsDialog = ReadingModeSettingsDialog.newInstance();
                if (fragmentManager != null && currentFragment.mReflowControl != null) {
                    readingModeSettingsDialog.setReflowControl(currentFragment.mReflowControl);
                    readingModeSettingsDialog.show(fragmentManager, "reading_mode_settings");
                }
                break;
        }

        if (updateViewMode) {
            if (currentFragment.isReflowMode()) {
                // Switch off reflow mode
                onToggleReflow();
            }
            // Update the PDFViewCtrl with the new mode
            currentFragment.updateViewMode(mode);

            // Update view mode button icon
            updateButtonViewModeIcon();
        }

        // Reset the toolbars timer
        resetHideToolbarsTimer();
    }

    /**
     * The overloaded implementation of {@link ViewModePickerDialogFragment.ViewModePickerDialogFragmentListener#onViewModePickerDialogFragmentDismiss()}.
     **/
    @Override
    public void onViewModePickerDialogFragmentDismiss() {
        resetHideToolbarsTimer();
    }

    /**
     * The overloaded implementation of {@link CustomColorModeDialogFragment.CustomColorModeSelectedListener#onCustomColorModeSelected(int, int)}.
     **/
    @Override
    public boolean onCustomColorModeSelected(int bgColor, int txtColor) {
        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }

        PdfViewCtrlSettingsManager.setCustomColorModeTextColor(activity, txtColor);
        PdfViewCtrlSettingsManager.setCustomColorModeBGColor(activity, bgColor);
        PdfViewCtrlSettingsManager.setColorMode(activity, PdfViewCtrlSettingsManager.KEY_PREF_COLOR_MODE_CUSTOM);

        return updateColorMode();
    }

    @Override
    public void onBookmarksDialogWillDismiss(int tabIndex) {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment != null && currentFragment.isNavigationListShowing()) {
            currentFragment.closeNavigationList();
        } else {
            if (mBookmarksDialog != null) {
                mBookmarksDialog.dismiss();
            }
        }
    }

    /**
     * The overloaded implementation of {@link BookmarksDialogFragment.BookmarksDialogListener#onBookmarksDialogDismissed(int)}.
     **/
    @Override
    public void onBookmarksDialogDismissed(int tabIndex) {
        resetHideToolbarsTimer();

        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment != null) {
            currentFragment.setBookmarkDialogCurrentTab(tabIndex);
        }
    }

    /**
     * The overloaded implementation of {@link ViewModePickerDialogFragment.ViewModePickerDialogFragmentListener#onReflowZoomInOut(boolean)}.
     **/
    @Override
    public int onReflowZoomInOut(boolean flagZoomIn) {
        PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();

        if (currentFragment == null) {
            return 0;
        }

        currentFragment.zoomInOutReflow(flagZoomIn);
        return currentFragment.getReflowTextSize();
    }

    /**
     * Whether to disable the auto hide Toolbar timer
     *
     * @param disable true if timer is disabled, false otherwise
     */
    public void setToolbarTimerDisabled(boolean disable) {
        mToolbarTimerDisabled = disable;
    }

    /**
     * The overloaded implementation of {@link PdfViewCtrlTabBaseFragment.TabListener#resetHideToolbarsTimer()}.
     **/
    @Override
    public void resetHideToolbarsTimer() {
        stopHideToolbarsTimer();
        if (mToolbarTimerDisabled) {
            return;
        }
        if (mHideToolbarsHandler != null) {
            mHideToolbarsHandler.postDelayed(mHideToolbarsRunnable, HIDE_TOOLBARS_TIMER);
        }
    }

    /**
     * Stops timer for hiding toolbar.
     */
    public void stopHideToolbarsTimer() {
        if (mHideToolbarsHandler != null) {
            mHideToolbarsHandler.removeCallbacksAndMessages(null);
        }
    }

    protected void resetHideNavigationBarTimer() {
        stopHideNavigationBarTimer();
        if (mHideNavigationBarHandler != null) {
            mHideNavigationBarHandler.postDelayed(mHideNavigationBarRunnable, HIDE_NAVIGATION_BAR_TIMER);
        }
    }

    protected void stopHideNavigationBarTimer() {
        if (mHideNavigationBarHandler != null) {
            mHideNavigationBarHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * Called when share option has been selected.
     */
    protected void onShareOptionSelected() {
        PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }

        // Tries to save most recent changes before sharing
        if (!checkTabConversionAndAlert(R.string.cant_share_while_converting_message, true)) {
            currentFragment.save(false, true, true);
            currentFragment.handleOnlineShare();
        }
    }

    /**
     * Adds a new page.
     */
    public void addNewPage() {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null || !currentFragment.isDocumentReady()) {
            return;
        }

        double pageWidth = 0;
        double pageHeight = 0;
        PDFViewCtrl pdfViewCtrl = currentFragment.getPDFViewCtrl();
        if (pdfViewCtrl != null) {
            boolean shouldUnlockRead = false;
            try {
                pdfViewCtrl.docLockRead();
                shouldUnlockRead = true;
                Page lastPage = pdfViewCtrl.getDoc().getPage(pdfViewCtrl.getDoc().getPageCount());
                if (lastPage == null)
                    return;
                pageWidth = lastPage.getPageWidth();
                pageHeight = lastPage.getPageHeight();
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
                return;
            } finally {
                if (shouldUnlockRead) {
                    pdfViewCtrl.docUnlockRead();
                }
            }
        }

        AddPageDialogFragment addPageDialogFragment = AddPageDialogFragment.newInstance(pageWidth, pageHeight)
                .setInitialPageSize(AddPageDialogFragment.PageSize.Custom);
        addPageDialogFragment.setOnAddNewPagesListener(new AddPageDialogFragment.OnAddNewPagesListener() {
            @Override
            public void onAddNewPages(Page[] pages) {
                if (pages == null || pages.length == 0) {
                    return;
                }

                currentFragment.onAddNewPages(pages);
            }
        });
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            addPageDialogFragment.show(fragmentManager, "add_page_overflow_menu");
        }
    }

    /**
     * Handles deleting the current page.
     */
    protected void requestDeleteCurrentPage() {
        Activity activity = getActivity();
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null || !currentFragment.isDocumentReady()) {
            return;
        }

        final PDFViewCtrl pdfViewCtrl = currentFragment.getPDFViewCtrl();
        if (pdfViewCtrl == null) {
            return;
        }

        PDFDoc doc = pdfViewCtrl.getDoc();
        try {
            if (doc.getPageCount() < 2) {
                CommonToast.showText(activity, R.string.controls_thumbnails_view_delete_msg_all_pages);
                return;
            }
        } catch (PDFNetException e) {
            return;
        }

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(activity);
        alertBuilder.setTitle(R.string.action_delete_current_page);
        alertBuilder.setMessage(R.string.dialog_delete_current_page);
        alertBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                currentFragment.onDeleteCurrentPage();
                dialog.dismiss();
            }
        });
        alertBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertBuilder.setNeutralButton(R.string.action_delete_multiple, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                onViewModeSelected(PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_THUMBNAILS_VALUE, true,
                        pdfViewCtrl.getCurrentPage());
            }
        });
        alertBuilder.create().show();
    }

    /**
     * Shows the rotate dialog.
     */
    protected void showRotateDialog() {
        PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager == null || currentFragment == null || !currentFragment.isDocumentReady()) {
            return;
        }

        PDFViewCtrl pdfViewCtrl = currentFragment.getPDFViewCtrl();
        if (pdfViewCtrl != null) {
            RotateDialogFragment.newInstance()
                    .setPdfViewCtrl(pdfViewCtrl)
                    .show(fragmentManager, "rotate_dialog");
        }
    }

    /**
     * Called when view mode option has been selected.
     */
    @Override
    public void onViewModeOptionSelected() {
        PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null || !currentFragment.isDocumentReady()) {
            return;
        }

        // save the current page state for the back button - when the user
        // changes pages through grid view
        currentFragment.updateCurrentPageInfo();
        PDFViewCtrl.PagePresentationMode currentViewMode = PDFViewCtrl.PagePresentationMode.SINGLE_CONT;
        final PDFViewCtrl pdfViewCtrl = currentFragment.getPDFViewCtrl();
        if (pdfViewCtrl != null) {
            currentViewMode = pdfViewCtrl.getPagePresentationMode();
        }
        if (currentViewMode == PDFViewCtrl.PagePresentationMode.SINGLE_VERT) {
            currentViewMode = PDFViewCtrl.PagePresentationMode.SINGLE_CONT;
        } else if (currentViewMode == PDFViewCtrl.PagePresentationMode.FACING_VERT) {
            currentViewMode = PDFViewCtrl.PagePresentationMode.FACING_CONT;
        } else if (currentViewMode == PDFViewCtrl.PagePresentationMode.FACING_COVER_VERT) {
            currentViewMode = PDFViewCtrl.PagePresentationMode.FACING_COVER_CONT;
        }
        boolean isRtlMode = currentFragment.isRtlMode();
        boolean isReflowMode = currentFragment.isReflowMode();
        int reflowTextSize = currentFragment.getReflowTextSize();
        ArrayList<Integer> hiddenViewModeItems = new ArrayList<>();
        if (mViewerConfig != null && !mViewerConfig.isShowCropOption()) {
            hiddenViewModeItems.add(ViewModePickerDialogFragment.ViewModePickerItems.ITEM_ID_USERCROP.getValue());
        }
        if (mViewerConfig != null && !mViewerConfig.isShowReflowOption()) {
            hiddenViewModeItems.add(ViewModePickerDialogFragment.ViewModePickerItems.ITEM_ID_REFLOW.getValue());
        }
        if (mViewerConfig != null && mViewerConfig.getHideViewModeIds() != null) {
            for (int item : mViewerConfig.getHideViewModeIds()) {
                hiddenViewModeItems.add(item);
            }
        }
        if (mViewerConfig != null && !mViewerConfig.isImageInReflowEnabled()) {
            hiddenViewModeItems.add(ViewModePickerDialogFragment.ViewModePickerItems.ITEM_ID_READING_MODE.getValue());
        }
        ViewModePickerDialogFragment dialog =
                ViewModePickerDialogFragment.newInstance(
                        currentViewMode,
                        isRtlMode,
                        isReflowMode,
                        reflowTextSize,
                        hiddenViewModeItems
                );
        dialog.setViewModePickerDialogFragmentListener(this);
        dialog.setStyle(DialogFragment.STYLE_NORMAL, mThemeProvider.getTheme());
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            dialog.show(fragmentManager, "view_mode_picker");
        }

        stopHideToolbarsTimer();
    }

    @Override
    public void onSearchOptionSelected() {
        Activity activity = getActivity();
        PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
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

    /**
     * Called when outline option has been selected.
     */
    @Override
    public void onOutlineOptionSelected() {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment != null && currentFragment.isDocumentReady()) {
            onOutlineOptionSelected(currentFragment.getBookmarkDialogCurrentTab());
        }
    }

    /**
     * Called when outline option has been selected.
     *
     * @param initialTabIndex The tab index which should be selected after bookmarks dialog is created
     */
    public void onOutlineOptionSelected(int initialTabIndex) {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }
        PDFViewCtrl pdfViewCtrl = currentFragment.getPDFViewCtrl();
        if (pdfViewCtrl == null) {
            return;
        }

        // save the current page state for the back button - when the user
        // changes pages through the annotation list or outline
        currentFragment.updateCurrentPageInfo();

        if (currentFragment.isNavigationListShowing()) {
            // Creates the dialog as side sheet
            currentFragment.closeNavigationList();
            return;
        } else {
            // Creates the dialog in full screen mode
            if (mBookmarksDialog != null) {
                mBookmarksDialog.dismiss();
            }
        }
        mBookmarksDialog = createBookmarkDialogFragmentInstance();

        mBookmarksDialog.setPdfViewCtrl(pdfViewCtrl)
                .setDialogFragmentTabs(getBookmarksDialogTabs(), initialTabIndex)
                .setCurrentBookmark(mCurrentBookmark);
        mBookmarksDialog.setBookmarksDialogListener(this);
        mBookmarksDialog.setBookmarksTabsListener(this);
        mBookmarksDialog.setStyle(DialogFragment.STYLE_NO_TITLE, mThemeProvider.getTheme());

        openBookmarksDialog();

        stopHideToolbarsTimer();
    }

    public void reloadUserBookmarks() {
        BookmarksDialogFragment bookmarksDialogFragment = mBookmarksDialog;
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (bookmarksDialogFragment == null && currentFragment != null) {
            bookmarksDialogFragment = currentFragment.getBookmarksNavigationList();
        }
        if (bookmarksDialogFragment != null && bookmarksDialogFragment.getCurrentFragment() instanceof UserBookmarkDialogFragment) {
            ((UserBookmarkDialogFragment) bookmarksDialogFragment.getCurrentFragment()).loadBookmarks();
        }
    }

    protected ArrayList<DialogFragmentTab> getBookmarksDialogTabs() {
        DialogFragmentTab userBookmarkTab = createUserBookmarkDialogTab();
        DialogFragmentTab outlineTab = createOutlineDialogTab();
        DialogFragmentTab annotationTab = createAnnotationDialogTab();
        ArrayList<DialogFragmentTab> dialogFragmentTabs = new ArrayList<>(3);
        if (userBookmarkTab != null) {
            boolean canAdd = mViewerConfig == null || mViewerConfig.isShowUserBookmarksList();
            if (canAdd) {
                dialogFragmentTabs.add(userBookmarkTab);
            }
        }
        if (outlineTab != null) {
            boolean canAdd = mViewerConfig == null || mViewerConfig.isShowOutlineList();
            if (canAdd) {
                dialogFragmentTabs.add(outlineTab);
            }
        }
        if (annotationTab != null) {
            boolean canAdd = mViewerConfig == null || mViewerConfig.isShowAnnotationsList();
            if (canAdd) {
                dialogFragmentTabs.add(annotationTab);
            }
        }
        return dialogFragmentTabs;
    }

    protected boolean canOpenNavigationListAsSideSheet() {
        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }

        if (mViewerConfig != null && !mViewerConfig.isNavigationListAsSheetOnLargeDevice()) {
            return false;
        }

        return Utils.isLargeTablet(activity);
    }

    /**
     * Creates an instance of {@link BookmarksDialogFragment}.
     *
     * @return an instance of {@link BookmarksDialogFragment}
     */
    protected BookmarksDialogFragment createBookmarkDialogFragmentInstance() {
        BookmarksDialogFragment.DialogMode mode = canOpenNavigationListAsSideSheet() ?
                BookmarksDialogFragment.DialogMode.SHEET :
                BookmarksDialogFragment.DialogMode.DIALOG;
        return BookmarksDialogFragment.newInstance(mode);
    }

    /**
     * Creates the user bookmark dialog fragment tab
     *
     * @return The user bookmark dialog fragment tab
     */
    protected DialogFragmentTab createUserBookmarkDialogTab() {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return null;
        }

        PDFViewCtrl pdfViewCtrl = currentFragment.getPDFViewCtrl();
        if (pdfViewCtrl == null) {
            return null;
        }

        Bundle bundle = new Bundle();
        boolean readonly = currentFragment.isTabReadOnly();
        boolean allowEditing = mViewerConfig == null || mViewerConfig.isUserBookmarksListEditingEnabled();
        boolean allowBookmarkCreation = mViewerConfig == null || mViewerConfig.isUserBookmarkCreationEnabled();
        boolean autoSort = mViewerConfig != null && mViewerConfig.isAutoSortUserBookmarks();
        bundle.putBoolean(UserBookmarkDialogFragment.BUNDLE_IS_READ_ONLY, readonly);
        bundle.putBoolean(UserBookmarkDialogFragment.BUNDLE_ALLOW_EDITING, allowEditing);
        bundle.putBoolean(UserBookmarkDialogFragment.BUNDLE_BOOKMARK_CREATION_ENABLED, allowBookmarkCreation);
        bundle.putBoolean(UserBookmarkDialogFragment.BUNDLE_AUTO_SORT_BOOKMARKS, autoSort);
        if (mViewerConfig != null) {
            bundle.putInt(UserBookmarkDialogFragment.BUNDLE_EDITING_MODE, mViewerConfig.getUserBookmarksListEditingMode());
        }
        return new DialogFragmentTab(UserBookmarkDialogFragment.class,
                BookmarksTabLayout.TAG_TAB_BOOKMARK,
                Utils.getDrawable(getContext(), R.drawable.ic_bookmarks_white_24dp),
                null,
                getString(R.string.bookmark_dialog_fragment_bookmark_tab_title),
                bundle,
                R.menu.fragment_user_bookmark);
    }

    /**
     * Creates the outline dialog fragment tab
     *
     * @return The outline dialog fragment tab
     */
    protected DialogFragmentTab createOutlineDialogTab() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(OutlineDialogFragment.BUNDLE_EDITING_ENABLED,
                mViewerConfig == null || mViewerConfig.isOutlineListEditingEnabled());
        return new DialogFragmentTab(OutlineDialogFragment.class,
                BookmarksTabLayout.TAG_TAB_OUTLINE,
                Utils.getDrawable(getContext(), R.drawable.ic_outline_white_24dp),
                null,
                getString(R.string.bookmark_dialog_fragment_outline_tab_title),
                bundle,
                R.menu.fragment_outline);
    }

    /**
     * Creates the annotation dialog fragment tab
     *
     * @return The annotation dialog fragment tab
     */
    protected DialogFragmentTab createAnnotationDialogTab() {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        final Context context = getContext();
        if (currentFragment == null || context == null) {
            return null;
        }

        Bundle bundle = new Bundle();
        boolean readonly = currentFragment.isTabReadOnly();
        if (!readonly && mViewerConfig != null && !mViewerConfig.annotationsListEditingEnabled()) {
            // if document is editable, user can specify if a particular control is editable
            readonly = true;
        }
        bundle.putBoolean(AnnotationDialogFragment.BUNDLE_IS_READ_ONLY, readonly);
        bundle.putBoolean(AnnotationDialogFragment.BUNDLE_IS_RTL, currentFragment.isRtlMode());
        bundle.putInt(AnnotationDialogFragment.BUNDLE_KEY_SORT_MODE,
                PdfViewCtrlSettingsManager.getAnnotListSortOrder(context,
                        AnnotationListSortOrder.DATE_ASCENDING) // default sort order
        );
        if (mViewerConfig != null && mViewerConfig.getExcludedAnnotationListTypes() != null) {
            bundle.putIntArray(AnnotationDialogFragment.BUNDLE_ANNOTATION_TYPE_EXCLUDE_LIST, mViewerConfig.getExcludedAnnotationListTypes());
        }
        bundle.putBoolean(AnnotationDialogFragment.BUNDLE_ENABLE_ANNOTATION_FILTER,
                mViewerConfig == null || mViewerConfig.annotationsListFilterEnabled());
        return new DialogFragmentTab(AnnotationDialogFragment.class,
                BookmarksTabLayout.TAG_TAB_ANNOTATION,
                Utils.getDrawable(context, R.drawable.ic_annotations_white_24dp),
                null,
                getString(R.string.bookmark_dialog_fragment_annotation_tab_title),
                bundle,
                R.menu.fragment_annotlist_sort);
    }

    /**
     * Called when list all option has been selected.
     */
    protected void onListAllOptionSelected(String searchQuery) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        if (mSearchResultsView != null && mSearchResultsView.getVisibility() == View.VISIBLE) {
            hideSearchResults();
        } else if (!Utils.isNullOrEmpty(searchQuery)) {
            showSearchResults(searchQuery);
        }
    }

    /**
     * Called when search match case option has been selected.
     *
     * @param isChecked True if checked
     */
    protected void onSearchMatchCaseOptionSelected(boolean isChecked) {
        mSearchMatchCase = isChecked;
        PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }
        currentFragment.setSearchMatchCase(isChecked);
        currentFragment.resetFullTextResults();

        if (mSearchResultsView == null) {
            return;
        }
        if (mSearchResultsView.getDoc() == null || mSearchResultsView.getDoc() != currentFragment.getPdfDoc()) {
            mSearchResultsView.setPdfViewCtrl(currentFragment.getPDFViewCtrl());
        }
        mSearchResultsView.setMatchCase(isChecked);
    }

    /**
     * Called when search whole word option has been selected.
     *
     * @param isChecked True if checked
     */
    protected void onSearchWholeWordOptionSelected(boolean isChecked) {
        mSearchMatchWord = isChecked;
        PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }
        currentFragment.setSearchWholeWord(isChecked);
        currentFragment.resetFullTextResults();

        if (mSearchResultsView == null) {
            return;
        }
        if (mSearchResultsView.getDoc() == null || mSearchResultsView.getDoc() != currentFragment.getPdfDoc()) {
            mSearchResultsView.setPdfViewCtrl(currentFragment.getPDFViewCtrl());
        }
        mSearchResultsView.setWholeWord(isChecked);
    }

    /**
     * The overloaded implementation of {@link ToolManager.QuickMenuListener#onQuickMenuClicked(QuickMenuItem)}.
     */
    @Override
    public boolean onQuickMenuClicked(QuickMenuItem menuItem) {
        if (menuItem.getItemId() == R.id.qm_free_text) {
            showSystemStatusBar();
        }
        return false;
    }

    @Override
    public boolean onShowQuickMenu(QuickMenu quickmenu, Annot annot) {
        return false;
    }

    /**
     * The overloaded implementation of {@link ToolManager.QuickMenuListener#onQuickMenuShown()}.
     **/
    @Override
    public void onQuickMenuShown() {
    }

    /**
     * The overloaded implementation of {@link ToolManager.QuickMenuListener#onQuickMenuDismissed()}.
     **/
    @Override
    public void onQuickMenuDismissed() {
    }

    /**
     * The overloaded implementation of {@link ViewModePickerDialogFragment.ViewModePickerDialogFragmentListener#checkTabConversionAndAlert(int, boolean)}.
     **/
    @Override
    public boolean checkTabConversionAndAlert(int messageID, boolean allowConverted) {
        return checkTabConversionAndAlert(messageID, allowConverted, false);
    }

    /**
     * Checks tab conversion and shows the alert.
     *
     * @param messageID            The message ID
     * @param allowConverted       True if conversion is allowed
     * @param skipSpecialFileCheck True if spcecial files should be skipped
     * @return True if handled
     */
    public boolean checkTabConversionAndAlert(int messageID, boolean allowConverted, boolean skipSpecialFileCheck) {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        return currentFragment != null && currentFragment.checkTabConversionAndAlert(messageID, allowConverted, skipSpecialFileCheck);
    }

    /**
     * Handles exporting pages.
     *
     * @param folder    The file folder to put the new document
     * @param positions The page positions to be exported
     */
    protected void handleThumbnailsExport(File folder, SparseBooleanArray positions) {
        Activity activity = getActivity();
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return;
        }

        PDFDoc newDoc = null;
        boolean error = true;
        boolean shouldUnlock = false;
        try {
            newDoc = exportPages(ViewerUtils.getPageSet(positions));
            if (newDoc != null) {
                newDoc.lock();
                shouldUnlock = true;
                File tempFile = new File(folder.getAbsolutePath(), currentFragment.getTabTitle() + " export.pdf");
                String filename = Utils.getFileNameNotInUse(tempFile.getAbsolutePath());
                File outputFile = new File(filename);
                newDoc.save(filename, SDFDoc.SaveMode.REMOVE_UNUSED, null);
                showExportPagesSuccess(BaseFileInfo.FILE_TYPE_FILE, filename, outputFile.getName());
                error = false;
            }
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                Utils.unlockQuietly(newDoc);
            }
            Utils.closeQuietly(newDoc);
        }
        if (error) {
            Utils.showAlertDialog(activity, getString(R.string.error_export_file), getString(R.string.error));
        }
    }

    /**
     * Handles exporting pages.
     *
     * @param folder    The external file folder to put the new document
     * @param positions The page positions to be exported
     */
    protected void handleThumbnailsExport(ExternalFileInfo folder, SparseBooleanArray positions) {
        Activity activity = getActivity();
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return;
        }

        String filename = Utils.getFileNameNotInUse(folder, currentFragment.getTabTitle() + " export.pdf");
        if (folder == null || Utils.isNullOrEmpty(filename)) {
            Utils.showAlertDialog(activity, getString(R.string.error_export_file), getString(R.string.error));
            return;
        }
        ExternalFileInfo file = folder.createFile("application/pdf", filename);
        if (file == null) {
            return;
        }
        boolean error = true;
        PDFDoc newDoc = null;
        SecondaryFileFilter filter = null;
        try {
            newDoc = exportPages(ViewerUtils.getPageSet(positions));
            if (newDoc != null) {
                filter = new SecondaryFileFilter(activity, file.getUri());
                newDoc.save(filter, SDFDoc.SaveMode.REMOVE_UNUSED);
                showExportPagesSuccess(BaseFileInfo.FILE_TYPE_EXTERNAL, file.getUri().toString(), file.getName());
                error = false;
            }
        } catch (PDFNetException | IOException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            Utils.closeQuietly(newDoc, filter);
        }
        if (error) {
            Utils.showAlertDialog(activity, getString(R.string.error_export_file), getString(R.string.error));
        }
    }

    /**
     * Handles exporting pages.
     *
     * @param destFileUri The destination URI to create the file
     * @param positions   The page positions to be exported
     */
    protected void handleThumbnailsExport(Uri destFileUri, SparseBooleanArray positions) {
        Activity activity = getActivity();
        if (activity == null || destFileUri == null || positions == null) {
            return;
        }
        boolean error = true;
        PDFDoc newDoc = null;
        SecondaryFileFilter filter = null;
        try {
            newDoc = exportPages(ViewerUtils.getPageSet(positions));
            if (newDoc != null) {
                filter = new SecondaryFileFilter(activity, destFileUri);
                newDoc.save(filter, SDFDoc.SaveMode.REMOVE_UNUSED);
                showExportPagesSuccess(BaseFileInfo.FILE_TYPE_EDIT_URI, destFileUri.toString(), Utils.getUriDisplayName(activity, destFileUri));
                error = false;
            }
        } catch (PDFNetException | IOException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            Utils.closeQuietly(newDoc, filter);
        }
        if (error) {
            Utils.showAlertDialog(activity, getString(R.string.error_export_file), getString(R.string.error));
        }
    }

    /**
     * Exports pages.
     *
     * @param pageSet The page set to be exported
     * @return The new pdf doc with exported pages
     * @throws PDFNetException PDFNet Exception
     */
    protected PDFDoc exportPages(PageSet pageSet) throws PDFNetException {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment != null) {
            PDFDoc newDoc = new PDFDoc();
            newDoc.insertPages(0, currentFragment.getPdfDoc(), pageSet, PDFDoc.InsertBookmarkMode.INSERT, null);
            return newDoc;
        }
        return null;
    }

    /**
     * Lets the user know the pages have been successfully exported
     *
     * @param itemSource The item source of the file
     * @param tag        The tab tag
     * @param filename   The file name
     */
    protected void showExportPagesSuccess(final int itemSource, final String tag, final String filename) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        AlertDialog.Builder successDialogBuilder = Utils.getAlertDialogBuilder(activity, "", "");
        successDialogBuilder.setNegativeButton(R.string.open, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onOpenAddNewTab(itemSource, tag, filename, "");
                mThumbFragment.dismiss();
            }
        });
        successDialogBuilder.setPositiveButton(R.string.ok, null);
        successDialogBuilder.setMessage(Html.fromHtml(getString(R.string.export_success, filename)));
        successDialogBuilder.create().show();
    }

    protected void adjustConfiguration() {
        Activity activity = getActivity();
        if (null == activity || null == mViewerConfig) {
            return;
        }
        PdfViewCtrlSettingsManager.setFullScreenMode(activity, mViewerConfig.isFullscreenModeEnabled());
        PdfViewCtrlSettingsManager.setMultipleTabs(activity, mViewerConfig.isMultiTabEnabled());
        mMultiTabModeEnabled = mViewerConfig.isMultiTabEnabled();
        setTabLayoutVisible(mMultiTabModeEnabled);
    }

    private void onToggleRtlMode() {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment != null) {
            currentFragment.toggleRtlMode();
        }
    }

    protected void updateTabLayout() {
        Activity activity = getActivity();
        if (activity == null || mTabLayout == null) {
            return;
        }

        boolean isLargeScreen = Utils.isLargeScreen(activity);
        boolean permanentToolbars = mViewerConfig != null && mViewerConfig.isPermanentToolbars();
        mAutoHideEnabled = !(isLargeScreen || permanentToolbars);
        if (!mAutoHideEnabled) {
            showUI();
        }

        if (getMaxTabCount() <= PdfViewCtrlTabsManager.MAX_NUM_TABS_PHONE && mTabLayout.getTabCount() > 1) {
            mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
            mTabLayout.setTabMode(TabLayout.MODE_FIXED);
        } else {
            mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        }

        final int tabCount = mTabLayout.getTabCount();
        for (int i = 0; i < tabCount; i++) {
            TabLayout.Tab tab = mTabLayout.getTabAt(i);
            if (tab != null) {
                View view = tab.getCustomView();
                if (view != null) {
                    ImageButton button = view.findViewById(R.id.tab_pdfviewctrl_close_button);
                    if (button != null) {
                        ColorStateList tint = mTabLayout.getTabTextColors();
                        // Set close button icon's tint colour according to its current state (selected or not).
                        if (tint != null) {
                            button.setColorFilter(tint.getColorForState(button.getDrawableState(), tint.getDefaultColor()), PorterDuff.Mode.SRC_IN);
                        }

                        if (!Utils.isTablet(getContext()) && Utils.isPortrait(getContext()) && !tab.isSelected()) {
                            button.setVisibility(View.GONE);
                        } else {
                            button.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        }
    }

    /**
     * Updates color mode.
     *
     * @return True if the view mode picker dialog should be dismissed
     */
    public boolean updateColorMode() {
        Activity activity = getActivity();
        if (activity == null || mTabLayout == null) {
            return false;
        }

        if (canRecreateActivity() && activity instanceof AppCompatActivity && applyTheme((AppCompatActivity) activity)) {
            return true;
        }

        ArrayList<Fragment> fragments = mTabLayout.getLiveFragments();
        PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        for (Fragment fragment : fragments) {
            if (fragment instanceof PdfViewCtrlTabBaseFragment) {
                PdfViewCtrlTabBaseFragment pdfViewCtrlTabFragment = (PdfViewCtrlTabBaseFragment) fragment;
                if (fragment == currentFragment) {
                    pdfViewCtrlTabFragment.updateColorMode();
                } else {
                    pdfViewCtrlTabFragment.setColorModeChanged();
                }
            }
        }
        return false;
    }

    private void updatePrintDocumentMode() {
        Activity activity = getActivity();
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return;
        }

        currentFragment.updatePrintDocumentMode(PdfViewCtrlSettingsManager.isPrintDocumentMode(activity));
    }

    private void updatePrintAnnotationsMode() {
        Activity activity = getActivity();
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return;
        }

        currentFragment.updatePrintAnnotationsMode(PdfViewCtrlSettingsManager.isPrintAnnotationsMode(activity));
    }

    private void updatePrintSummaryMode() {
        Activity activity = getActivity();
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return;
        }

        currentFragment.updatePrintSummaryMode(PdfViewCtrlSettingsManager.isPrintSummaryMode(activity));
    }

    /**
     * Updates the icons (enable/disable) when reflow mode has been changed.
     */
    protected abstract void updateIconsInReflowMode();

    private void updateButtonViewModeIcon() {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }
    }

    protected void updateAttachmentState() {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }
        boolean isVisible = (mViewerConfig == null || mViewerConfig.isShowFileAttachmentOption()) &&
                currentFragment.getPdfDoc() != null &&
                Utils.hasFileAttachments(currentFragment.getPdfDoc());

        MenuItem menuViewFileAttachment = getToolbarOptionMenuItem(R.id.action_file_attachment);
        if (menuViewFileAttachment != null) {
            menuViewFileAttachment.setVisible(isVisible);
        }
        menuViewFileAttachment = getViewOverflowOptionMenuItem(R.id.action_file_attachment);
        if (menuViewFileAttachment != null) {
            menuViewFileAttachment.setVisible(isVisible);
        }
    }

    protected void updateLayersState() {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }
        boolean isVisible = (mViewerConfig == null || mViewerConfig.isShowViewLayersToolbarOption()) &&
                (currentFragment.getPdfDoc() != null && PdfLayerUtils.hasPdfLayer(currentFragment.getPdfDoc()));

        MenuItem menuPdfLayers = getToolbarOptionMenuItem(R.id.action_pdf_layers);
        if (menuPdfLayers != null) {
            menuPdfLayers.setVisible(isVisible);
        }
        menuPdfLayers = getViewOverflowOptionMenuItem(R.id.action_pdf_layers);
        if (menuPdfLayers != null) {
            menuPdfLayers.setVisible(isVisible);
        }
    }

    protected void updateDigitalSignaturesState() {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }
        MenuItem mMenuDigitalSignatures = getToolbarOptionMenuItem(R.id.action_digital_signatures);
        boolean isVisible = (mViewerConfig == null || mViewerConfig.isShowDigitalSignaturesToolbarOption()) &&
                (currentFragment.getPdfDoc() != null && DigitalSignatureUtils.hasDigitalSignatures(currentFragment.getPdfDoc()));
        if (mMenuDigitalSignatures != null) {
            mMenuDigitalSignatures.setVisible(isVisible);
        }
        mMenuDigitalSignatures = getViewOverflowOptionMenuItem(R.id.action_digital_signatures);
        if (mMenuDigitalSignatures != null) {
            mMenuDigitalSignatures.setVisible(isVisible);
        }
    }

    /**
     * Shows the UI.
     */
    // TODO: Rename.
    public abstract void showUI();

    /**
     * Hides the UI.
     */
    // TODO: Rename.
    public abstract void hideUI();

    /**
     * Handles changing the visibility of toolbars.
     *
     * @param visible True if toolbar is visible
     */
    @Override
    public void setToolbarsVisible(boolean visible) {
        setToolbarsVisible(visible, true);
    }

    @Override
    public void setViewerOverlayUIVisible(boolean visible) {
        if (!mAutoHideEnabled) {
            return;
        }
        if (mViewerConfig != null && mViewerConfig.isPermanentToolbars()) {
            return;
        }
        if (visible) {
            showUI();
        } else {
            hideUI();
        }
    }

    /**
     * Handles changing the visibility of toolbars.
     *
     * @param visible          True if toolbar is visible
     * @param animateBottomBar True if visibility should be changed with animation
     */
    public abstract void setToolbarsVisible(boolean visible, boolean animateBottomBar);

    /**
     * Sets the visibility of tab layout.
     *
     * @param visible True if visible
     */
    protected void setTabLayoutVisible(boolean visible) {
        Activity activity = getActivity();
        if (activity == null || mTabLayout == null) {
            return;
        }

        boolean canHide = mAutoHideEnabled || mIsSearchMode;
        visible |= !canHide; // always show tab layout for large screen devices

        if (!canShowTabLayout()) {
            // Ensure that tab layout is not shown when multi-tab mode is disabled.
            if (mTabLayout.getVisibility() == View.VISIBLE) {
                mTabLayout.setVisibility(View.GONE);
            }
        } else if ((mTabLayout.getVisibility() == View.VISIBLE) != visible) {
            // Requested visibility is different from current value.
            mTabLayout.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    protected boolean canShowTabLayout() {
        return mMultiTabModeEnabled;
    }

    /**
     * Shows the system status bar.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected abstract void showSystemStatusBar();

    /**
     * Shows the system UI.
     */
    // This snippet shows the system bars. It does this by removing all the flags
    // except for the ones that make the content appear under the system bars.
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    protected abstract void showSystemUI();

    /**
     * Hides the system UI.
     */
    // This snippet hides the system bars.
    // http://stackoverflow.com/a/33551538
    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected abstract void hideSystemUI();

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
    protected abstract void updateFullScreenModeLayout();

    public void startSearchMode() {
        FragmentActivity activity = getActivity();
        PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null || mTabLayout == null) {
            return;
        }
        if (mToolbar == null || mSearchToolbar == null) {
            return;
        }

        showSearchToolbarTransition();

        if (mTabHostListeners != null) {
            for (TabHostListener listener : mTabHostListeners) {
                listener.onStartSearchMode();
            }
        }

        // hide tab widget
        setToolbarsVisible(true);
        setTabLayoutVisible(false);
        setThumbSliderVisibility(false, true);
        stopHideToolbarsTimer();
        setSearchNavButtonsVisible(true);

        mIsSearchMode = true;
        currentFragment.setSearchMode(true);

        currentFragment.hideBackAndForwardButtons();
    }

    /**
     * Exits the search mode.
     */
    public void exitSearchMode() {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (!mIsSearchMode || currentFragment == null || mTabLayout == null) {
            return;
        }

        mIsSearchMode = false;
        currentFragment.setSearchMode(false);

        if (mTabHostListeners != null) {
            for (TabHostListener listener : mTabHostListeners) {
                listener.onExitSearchMode();
            }
        }

        hideSearchToolbarTransition();

        // show tab widget
        setTabLayoutVisible(true);
        setToolbarsVisible(true);
        if (mSearchToolbar != null) {
            mSearchToolbar.setSearchProgressBarVisible(false);
        }
        setSearchNavButtonsVisible(false);

        // Cancel search and hide progress bar
        currentFragment.cancelFindText();

        currentFragment.exitSearchMode();
        // Dismiss and reset full doc search
        if (mSearchResultsView != null) {
            hideSearchResults();
            mSearchResultsView.reset();
        }
    }

    protected void showSearchToolbarTransition() {
        Transition fade = new Fade();
        if (mAppBarLayout != null && mToolbar != null && mSearchToolbar != null) {
            TransitionManager.beginDelayedTransition(mAppBarLayout, fade);
            mToolbar.setVisibility(View.GONE);
            mSearchToolbar.setVisibility(View.VISIBLE);
        }
    }

    protected void hideSearchToolbarTransition() {
        Transition fade = new Fade();
        if (mAppBarLayout != null && mToolbar != null && mSearchToolbar != null) {
            TransitionManager.beginDelayedTransition(mAppBarLayout, fade);
            if (mViewerConfig == null || mViewerConfig.isShowTopToolbar()) {
                mToolbar.setVisibility(View.VISIBLE);
            }
            mSearchToolbar.setVisibility(View.GONE);
        }
    }

    /**
     * Hides the search results.
     */
    protected void hideSearchResults() {
        if (mSearchResultsView != null) {
            mSearchResultsView.setVisibility(View.GONE);
        }
    }

    private void showSearchResults(String searchQuery) {
        PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return;
        }

        if (mSearchResultsView == null) {
            mSearchResultsView = inflateSearchResultsView(this);
        }
        if (mSearchResultsView != null) {
            if (mSearchResultsView.getDoc() == null || mSearchResultsView.getDoc() != currentFragment.getPdfDoc()) {
                mSearchResultsView.setPdfViewCtrl(currentFragment.getPDFViewCtrl());
            }
            mSearchResultsView.setMatchCase(mSearchMatchCase);
            mSearchResultsView.setWholeWord(mSearchMatchWord);
            mSearchResultsView.setVisibility(View.VISIBLE);
            mSearchResultsView.findText(searchQuery);

            onShowSearchResults(searchQuery);
        }
    }

    private void onShowSearchResults(String searchQuery) {
        PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (mSearchResultsView == null || currentFragment == null) {
            return;
        }

        mSearchResultsView.requestFocus();

        currentFragment.setSearchQuery(searchQuery);
        currentFragment.highlightSearchResults();
    }

    private SearchResultsView inflateSearchResultsView(SearchResultsView.SearchResultsListener listener) {
        View view = getView();
        if (view == null) {
            return null;
        }

        ViewStub stub = view.findViewById(R.id.controls_search_results_stub);
        if (stub != null) {
            SearchResultsView searchResultsView = (SearchResultsView) stub.inflate();
            CoordinatorLayout.LayoutParams clp = (CoordinatorLayout.LayoutParams) searchResultsView.getLayoutParams();
            clp.setBehavior(new PaneBehavior());
            clp.gravity = PaneBehavior.getGravityForOrientation(getContext(), getResources().getConfiguration().orientation);
            if (Utils.isLollipop()) {
                searchResultsView.setElevation(getResources().getDimension(R.dimen.actionbar_elevation));
            }
            searchResultsView.setSearchResultsListener(listener);
            return searchResultsView;
        }
        return null;
    }

    protected void adjustMenuButtonShowAs(MenuItem item, Activity activity) {
        if (item != null) {
            if (Utils.isTablet(activity)) {
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            } else {
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            }
        }
    }

    /**
     * Updates the visibility of the share button
     *
     * @param visible True if visible
     */
    protected void updateShareButtonVisibility(boolean visible) {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment != null) {
            MenuItem menuShare = getToolbarOptionMenuItem(R.id.action_share);
            if (menuShare != null) {
                menuShare.setVisible(visible && (mViewerConfig == null || mViewerConfig.isShowShareOption()));
            }
            menuShare = getViewOverflowOptionMenuItem(R.id.action_share);
            if (menuShare != null) {
                menuShare.setVisible(visible && (mViewerConfig == null || mViewerConfig.isShowShareOption()));
            }
        }
    }

    /**
     * Updates the visibility of the close tab button
     *
     * @param visible True if visible
     */
    protected void updateCloseTabButtonVisibility(boolean visible) {
        Activity activity = getActivity();
        if (null == activity) {
            return;
        }
        MenuItem menuCloseTab = getToolbarOptionMenuItem(R.id.action_close_tab);
        if (menuCloseTab != null) {
            if (!PdfViewCtrlSettingsManager.getMultipleTabs(activity)) {
                menuCloseTab.setVisible(visible && (mViewerConfig == null || mViewerConfig.isShowCloseTabOption()));
            } else {
                menuCloseTab.setVisible(false);
            }
        }
        menuCloseTab = getViewOverflowOptionMenuItem(R.id.action_close_tab);
        if (menuCloseTab != null) {
            if (!PdfViewCtrlSettingsManager.getMultipleTabs(activity)) {
                menuCloseTab.setVisible(visible && (mViewerConfig == null || mViewerConfig.isShowCloseTabOption()));
            } else {
                menuCloseTab.setVisible(false);
            }
        }
    }

    @Nullable
    protected MenuItem getToolbarOptionMenuItem(int id) {
        return mToolbar.getMenu().findItem(id);
    }

    @Nullable
    protected MenuItem getViewOverflowOptionMenuItem(int id) {
        if (mViewOverflowMenu != null) {
            return mViewOverflowMenu.getMenu().findItem(id);
        }
        return null;
    }

    protected void initOptionsMenu(Menu menu) {
    }

    /**
     * Sets the visibility of options menu
     *
     * @param visible True if visible
     */
    protected void setOptionsMenuVisible(boolean visible) {
        setOptionsMenuVisibleHelper(R.id.action_share, visible && (mViewerConfig == null || mViewerConfig.isShowShareOption()));
        setOptionsMenuVisibleHelper(R.id.action_edit_menu, visible && (mViewerConfig == null || mViewerConfig.isShowEditMenuOption()));
        MenuItem menuPrint = getToolbarOptionMenuItem(R.id.action_print);
        if (menuPrint != null) {
            if (Utils.isKitKat()) {
                menuPrint.setVisible(visible && (mViewerConfig == null || mViewerConfig.isShowPrintOption()));
            } else {
                menuPrint.setVisible(false);
            }
        }
        menuPrint = getViewOverflowOptionMenuItem(R.id.action_print);
        if (menuPrint != null) {
            if (Utils.isKitKat()) {
                menuPrint.setVisible(visible && (mViewerConfig == null || mViewerConfig.isShowPrintOption()));
            } else {
                menuPrint.setVisible(false);
            }
        }
        setOptionsMenuVisibleHelper(R.id.action_editpages, visible && (mViewerConfig == null || mViewerConfig.isShowEditPagesOption()));
        setOptionsMenuVisibleHelper(R.id.action_export_options, visible && (mViewerConfig == null || mViewerConfig.isShowSaveCopyOption()));
        // each of the export option
        ArrayList<Integer> saveOptions = null;
        if (mViewerConfig != null && mViewerConfig.getHideSaveCopyOptions() != null) {
            // we only care about these options if they need to be hidden
            Integer[] integerArray = ArrayUtils.toObject(mViewerConfig.getHideSaveCopyOptions());
            saveOptions = new ArrayList<>(Arrays.asList(integerArray));
            setOptionsMenuVisibleHelper(R.id.menu_export_copy, visible && !saveOptions.contains(R.id.menu_export_copy));
            setOptionsMenuVisibleHelper(R.id.menu_export_flattened_copy, visible && !saveOptions.contains(R.id.menu_export_flattened_copy));
            setOptionsMenuVisibleHelper(R.id.menu_export_cropped_copy, visible && !saveOptions.contains(R.id.menu_export_cropped_copy));
            setOptionsMenuVisibleHelper(R.id.menu_export_password_copy, visible && !saveOptions.contains(R.id.menu_export_password_copy));
        }
        // optimize is treated separately as it has multiple flags
        if (mViewerConfig != null && mViewerConfig.isUseStandardLibrary()) {
            setOptionsMenuVisibleHelper(R.id.menu_export_optimized_copy, visible && (mViewerConfig == null || !mViewerConfig.isUseStandardLibrary()));
        } else if (saveOptions != null) {
            setOptionsMenuVisibleHelper(R.id.menu_export_optimized_copy, visible && !saveOptions.contains(R.id.menu_export_optimized_copy));
        }
        setOptionsMenuVisibleHelper(R.id.action_viewmode, visible && (mViewerConfig == null || mViewerConfig.isShowDocumentSettingsOption()));

        updateShareButtonVisibility(visible);
        updateCloseTabButtonVisibility(visible);
        updateIconsInReflowMode();
    }

    private void setOptionsMenuVisibleHelper(int menuId, boolean visibility) {
        MenuItem menuItem = getToolbarOptionMenuItem(menuId);
        if (menuItem != null) {
            menuItem.setVisible(visibility);
        }
        menuItem = getViewOverflowOptionMenuItem(menuId);
        if (menuItem != null) {
            menuItem.setVisible(visibility);
        }
    }

    private void setSearchNavButtonsVisible(boolean visible) {
        Activity activity = getActivity();
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || currentFragment == null) {
            return;
        }

        currentFragment.setSearchNavButtonsVisible(visible);
    }

    /**
     * Handles when opening file has been failed.
     *
     * @param errorCode The error code
     */
    protected void handleOpenFileFailed(int errorCode) {
        handleOpenFileFailed(errorCode, "");
    }

    /**
     * Handles when opening file has been failed.
     *
     * @param errorCode The error code
     * @param info      The extra information
     */
    protected void handleOpenFileFailed(int errorCode, String info) {
        Activity activity = getActivity();
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (activity == null || activity.isFinishing() || currentFragment == null) {
            return;
        }

        int messageId = R.string.error_opening_doc_message;
        String message = null;
        boolean shouldShowErrorMessage = true;
        switch (errorCode) {
            case PdfDocManager.DOCUMENT_SETDOC_ERROR_ZERO_PAGE:
                messageId = R.string.error_empty_file_message;
                break;
            case PdfDocManager.DOCUMENT_SETDOC_ERROR_OPENURL_CANCELLED:
                messageId = R.string.download_cancelled_message;
                break;
            case PdfDocManager.DOCUMENT_SETDOC_ERROR_WRONG_PASSWORD:
                messageId = R.string.password_not_valid_message;
                break;
            case PdfDocManager.DOCUMENT_SETDOC_ERROR_NOT_EXIST:
                messageId = R.string.file_does_not_exist_message;
                break;
            case PdfDocManager.DOCUMENT_SETDOC_ERROR_DOWNLOAD_CANCEL:
                messageId = R.string.download_size_cancelled_message;
                break;
            case PdfDocManager.DOCUMENT_ERROR_MISSING_PERMISSIONS:
                if (currentFragment.getTabSource() == BaseFileInfo.FILE_TYPE_EDIT_URI) {
                    File realFile = currentFragment.getAndroidQEditUriRealPath();
                    message = getString(R.string.error_opening_doc_uri_permission_message,
                            realFile != null ? realFile.getAbsolutePath() : "");
                }
                break;
        }

        if (shouldShowErrorMessage) {
            if (null == message) {
                message = getString(messageId);
            }
            if (mQuitAppWhenDoneViewing) {
                CommonToast.showText(activity, message, Toast.LENGTH_LONG);
            } else {
                String title = currentFragment.getTabTitle();
                title = shortenTitle(title);
                Utils.showAlertDialog(activity, message, title);
            }
        }

        if (errorCode != PdfDocManager.DOCUMENT_SETDOC_ERROR_WRONG_PASSWORD) {
            currentFragment.removeFromRecentList();
        }

        removeTab(currentFragment.getTabTag());
    }

    /**
     * Returns the short-version of title
     *
     * @param title The title
     * @return The shorten title
     */
    protected String shortenTitle(String title) {
        // let's substring the title to make sure it's not too large
        final int maxTitleCount = 20;
        if ((title.length() - 1) > maxTitleCount) {
            title = title.substring(0, maxTitleCount);
            title = title + "...";
        }

        return title;
    }

    /**
     * Called when user clicked on tab widget.
     *
     * @param tag           The tab tag
     * @param title         The title of tab
     * @param fileExtension The file extension
     * @param itemSource    The item source of the file
     * @param duration      The snackbar duration
     */
    protected void handleShowTabInfo(String tag, String title, String fileExtension, int itemSource, int duration) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (mTabHostListeners != null) {
            for (TabHostListener listener : mTabHostListeners) {
                if (!listener.canShowFileInFolder()) {
                    return;
                }
            }
        }

        String message;
        String path = "";
        try {
            if (itemSource == BaseFileInfo.FILE_TYPE_EXTERNAL) {
                Uri uri = Uri.parse(tag);
                message = Utils.getUriDisplayName(activity, uri);
                path = Utils.getUriDocumentPath(uri);
            } else if (itemSource == BaseFileInfo.FILE_TYPE_EDIT_URI
                    || itemSource == BaseFileInfo.FILE_TYPE_OFFICE_URI) {
                message = title;
            } else {
                message = FilenameUtils.getName(tag);
                path = FilenameUtils.getPath(tag);
            }
        } catch (Exception e) {
            message = title;
        }

        if (message == null) {
            message = title;
        }

        showTabInfo(message, path, tag, itemSource, duration);
    }

    /**
     * Handles when back button is pressed.
     *
     * @return Ture if the back event is handled
     */
    public boolean handleBackPressed() {
        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return false;
        }

        if (mIsSearchMode) {
            if (mSearchResultsView != null && mSearchResultsView.getVisibility() == View.VISIBLE) {
                hideSearchResults();
            } else {
                exitSearchMode();
            }
            return true;
        }
        return false;
    }

    /**
     * Reads and Unsets file system changed.
     *
     * @return True if file system changed
     */
    public boolean readAndUnsetFileSystemChanged() {
        return mFileSystemChanged.getAndSet(false);
    }

    // CTRL based keys
    public boolean handleKeyShortcutEvent(int keyCode, KeyEvent event) {
        Activity activity = getActivity();
        if (activity == null || mEditTextFocus) {
            return false;
        }

        if (ShortcutHelper.isCloseApp(keyCode, event)) {
            activity.finish();
            return true;
        }

        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null || !currentFragment.isDocumentReady()) {
            return false;
        }

        if (mSearchToolbar != null &&
                mSearchToolbar.getSearchView() != null &&
                currentFragment.isSearchMode()) {
            if (ShortcutHelper.isGotoPreviousSearch(keyCode, event)) {
                currentFragment.gotoPreviousSearch();
                // shouldn't be focused otherwise the shortcut for previous search (Shift+Enter)
                // just adds a space
                mSearchToolbar.getSearchView().clearFocus();
                return true;
            }

            // in search mode, swallow all other shortcuts
            return false;
        }

        if (currentFragment.handleKeyShortcut(keyCode, event)) {
            return true;
        }

        if (mTabLayout != null) {
            boolean isNextDoc = ShortcutHelper.isGotoNextDoc(keyCode, event);
            boolean isPreviousDoc = ShortcutHelper.isGotoPreviousDoc(keyCode, event);

            if (isNextDoc || isPreviousDoc) {
                int currentPosition = mTabLayout.getSelectedTabPosition();
                int tabCounts = mTabLayout.getTabCount();
                if (currentPosition == -1) {
                    return false;
                }
                if (isNextDoc) {
                    ++currentPosition;
                } else {
                    currentPosition += tabCounts - 1;
                }
                currentPosition %= tabCounts;
                TabLayout.Tab tab = mTabLayout.getTabAt(currentPosition);
                if (tab != null) {
                    tab.select();
                    return true;
                }
            }
        }

        if (mSearchToolbar != null) {
            if (ShortcutHelper.isFind(keyCode, event)) {
                if (mSearchToolbar.isShown()) {
                    if (mSearchToolbar.getSearchView() != null) {
                        mSearchToolbar.getSearchView().setFocusable(true);
                        mSearchToolbar.getSearchView().requestFocus();
                    }
                } else {
                    setToolbarsVisible(true);
                    onSearchOptionSelected();
                }
                return true;
            }
        }

        if (ShortcutHelper.isCloseTab(keyCode, event)) {
            closeTab(currentFragment.getTabTag(), currentFragment.getTabSource());
            return true;
        }

        if (ShortcutHelper.isOpenDrawer(keyCode, event)) {
            if (mTabHostListeners != null) {
                for (PdfViewCtrlTabHostBaseFragment.TabHostListener listener : mTabHostListeners) {
                    listener.onNavButtonPressed();
                }
                return true;
            }
        }

        return false;
    }

    // Non-CTRL based keys

    /**
     * Handles key when pressed up.
     *
     * @param keyCode The key code
     * @param event   The key event
     * @return True if the key is handled
     */
    public boolean handleKeyUp(int keyCode, KeyEvent event) {
        if (sDebug) {
            String output = "";
            if (event.isShiftPressed()) {
                output += "SHIFT ";
            }
            if (event.isCtrlPressed()) {
                output += "CTRL ";
            }
            if (event.isAltPressed()) {
                output += "ALT ";
            }
            output += keyCode;
            Log.d(TAG, "key: " + output);
        }

        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }

        // Ignore hotkey if the user is currently editing the Bookmark EditText view
        if (mEditTextFocus) {
            return false;
        }

        if (keyCode == KeyEvent.KEYCODE_ENTER && mSearchToolbar != null && mSearchToolbar.isJustSubmittedQuery()) {
            mSearchToolbar.setJustSubmittedQuery(false);
            return false;
        }

        final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
        if (currentFragment == null || !currentFragment.isDocumentReady()) {
            return false;
        }

        if (mSearchToolbar != null &&
                mSearchToolbar.getSearchView() != null &&
                currentFragment.isSearchMode()) {
            if (ShortcutHelper.isGotoNextSearch(keyCode, event)) {
                currentFragment.gotoNextSearch();
                // shouldn't be focused otherwise the shortcut for previous search (Shift+Enter)
                // just adds a space
                mSearchToolbar.getSearchView().clearFocus();
                return true;
            }
            if (ShortcutHelper.isCloseMenu(keyCode, event)) {
                exitSearchMode();
                return true;
            }

            // in search mode, swallow all other shortcuts
            return false;
        }

        if (currentFragment.handleKeyUp(keyCode, event)) {
            return true;
        }

        return false;
    }

    protected abstract void animateToolbars(final boolean visible);

    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected void showSnackbar(String mainMessage, String actionMessage, final View.OnClickListener clickListener) {
        showSnackbar(mainMessage, actionMessage, clickListener, Snackbar.LENGTH_LONG);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected void showSnackbar(String mainMessage, String actionMessage, final View.OnClickListener clickListener, final int duration) {
        View snackbarHolderView = mFragmentView.findViewById(R.id.controls_pane_coordinator_layout);
        final Snackbar snackbar = Snackbar.make(snackbarHolderView, mainMessage, duration);
        if (actionMessage != null && clickListener != null) {
            View.OnClickListener listener = new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    snackbar.dismiss();
                    clickListener.onClick(v);
                }
            };

            snackbar.setAction(actionMessage.toUpperCase(), listener);
        }
        snackbar.show();
    }

    /**
     * Called when the fragment is resumed.
     */
    protected void resumeFragment() {
        if (mTabHostListeners != null) {
            for (TabHostListener listener : mTabHostListeners) {
                listener.onTabHostShown();
            }
        }

        if (!mFragmentPaused) {
            return;
        }
        mFragmentPaused = false;

        if (sDebug) {
            Log.d(TAG, "resume HostFragment");
        }

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        updateIconsInReflowMode();

        if (PdfViewCtrlSettingsManager.getScreenStayLock(activity)) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        if (Utils.isPie() && PdfViewCtrlSettingsManager.getFullScreenMode(activity)) {
            int mode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            if (mViewerConfig != null) {
                mode = mViewerConfig.getLayoutInDisplayCutoutMode();
            }
            WindowManager.LayoutParams params = activity.getWindow().getAttributes();
            params.layoutInDisplayCutoutMode = mode;
        }

        updateFullScreenModeLayout();
        showSystemUI();

        if (mIsSearchMode) {
            // Re-start search mode to ensure toolbar visibility is correct
            startSearchMode();
        }
    }

    /**
     * Called when the fragment is paused.
     */
    protected void pauseFragment() {
        if (mTabHostListeners != null) {
            for (TabHostListener listener : mTabHostListeners) {
                listener.onTabHostHidden();
            }
        }

        if (mFragmentPaused) {
            return;
        }
        mFragmentPaused = true;

        if (sDebug) {
            Log.d(TAG, "pause HostFragment");
        }

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        stopHideToolbarsTimer();
        if (mSearchToolbar != null) {
            mSearchToolbar.pause();
        }

        if (PdfViewCtrlSettingsManager.getScreenStayLock(activity)) {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // release document lock
        if (mAutoCropTask != null && mAutoCropTask.getStatus() == AsyncTask.Status.RUNNING) {
            mAutoCropTask.cancel(true);
            mAutoCropTaskPaused = true;
            mAutoCropTaskTabTag = getCurrentTabTag();
        } else {
            mAutoCropTaskPaused = false;
        }
        MenuItem menuSearch = getToolbarOptionMenuItem(R.id.action_search);
        if (menuSearch != null) {
            menuSearch.getIcon().setAlpha(255);
        }
        menuSearch = getViewOverflowOptionMenuItem(R.id.action_search);
        if (menuSearch != null) {
            menuSearch.getIcon().setAlpha(255);
        }
    }

    /**
     * Sets if the host can dispatch long press event.
     *
     * @param enabled True to make the host able to read long press event
     */
    public void setLongPressEnabled(boolean enabled) {
        PdfViewCtrlTabBaseFragment tabFragment = getCurrentPdfViewCtrlFragment();
        if (tabFragment != null) {
            PDFViewCtrl pdfViewCtrl = tabFragment.getPDFViewCtrl();
            if (pdfViewCtrl != null) {
                pdfViewCtrl.setLongPressEnabled(enabled);
            }
        }
    }

    public void setupRedaction() {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            final RedactionViewModel redactionViewModel = ViewModelProviders.of(activity).get(RedactionViewModel.class);
            mDisposables.add(redactionViewModel.getObservable()
                    .subscribe(new Consumer<RedactionEvent>() {
                        @Override
                        public void accept(RedactionEvent redactionEvent) throws Exception {
                            if (redactionEvent.getEventType() == RedactionEvent.Type.REDACT_BY_SEARCH_OPEN_SHEET) {
                                final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
                                if (currentFragment == null) {
                                    return;
                                }
                                PDFViewCtrl pdfViewCtrl = currentFragment.getPDFViewCtrl();
                                if (pdfViewCtrl == null) {
                                    return;
                                }
                                SearchRedactionDialogFragment dialog = SearchRedactionDialogFragment.newInstance();
                                dialog.setPdfViewCtrl(pdfViewCtrl);
                                dialog.setStyle(DialogFragment.STYLE_NORMAL, mThemeProvider.getTheme());
                                openRedactionDialog(dialog);
                            } else if (redactionEvent.getEventType() == RedactionEvent.Type.REDACT_BY_SEARCH_CLOSE_CLICKED) {
                                final PdfViewCtrlTabBaseFragment currentFragment = getCurrentPdfViewCtrlFragment();
                                if (currentFragment == null) {
                                    return;
                                }
                                currentFragment.closeRedactionSearchList();
                            }
                        }
                    })
            );
        }
    }

    /**
     * Returns the number of tabs.
     *
     * @return The number of tabs
     */
    public int getTabCount() {
        if (mTabLayout == null) {
            return 0;
        }
        return mTabLayout.getTabCount();
    }

    public void updateToolbarDrawable() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (mToolbar != null) {
            if (Utils.isLargeScreenWidth(activity) && null == mViewerConfig) {
                mToolbar.setNavigationIcon(null);
            } else {
                if (mToolbarNavRes == 0) {
                    mToolbar.setNavigationIcon(null);
                } else {
                    mToolbar.setNavigationIcon(mToolbarNavRes);
                }
            }
        }
    }

    protected boolean canRecreateActivity() {
        boolean canRecreate = true;
        if (mTabHostListeners != null) {
            for (TabHostListener listener : mTabHostListeners) {
                if (!listener.canRecreateActivity()) {
                    canRecreate = false;
                }
            }
        }
        return canRecreate;
    }

    private boolean useSupportActionBar() {
        return mViewerConfig == null || mViewerConfig.isUseSupportActionBar();
    }

    public static void setDebug(boolean debug) {
        sDebug = debug;
    }

    /**
     * Returns a {@link PdfViewCtrlTabBaseFragment} class object that will be used to
     * instantiate viewer tabs.
     *
     * @return a {@code CollabPdfViewCtrlTabFragment} class to instantiate later
     */
    @NonNull
    protected Class<? extends PdfViewCtrlTabBaseFragment> getDefaultTabFragmentClass() {
        return PdfViewCtrlTabBaseFragment.class;
    }

    @Override
    public void onEditBookmarkFocusChanged(boolean isActive) {
        mEditTextFocus = isActive;
    }
}
