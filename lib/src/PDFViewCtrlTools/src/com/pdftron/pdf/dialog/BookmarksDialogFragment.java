//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.dialog;

import android.app.Dialog;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import com.pdftron.pdf.Bookmark;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.controls.AnnotationDialogFragment;
import com.pdftron.pdf.controls.BookmarksTabLayout;
import com.pdftron.pdf.controls.NavigationListDialogFragment;
import com.pdftron.pdf.controls.OutlineDialogFragment;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.DialogFragmentTab;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;

/**
 * This class shows a dialog containing other dialogs in separate tabs. The possible dialogs that
 * can be shown inside this master dialog are
 * user-defined bookmarks (See {@link com.pdftron.pdf.controls.UserBookmarkDialogFragment}),
 * document outline (See {@link com.pdftron.pdf.controls.OutlineDialogFragment}),
 * annotations (See {@link AnnotationDialogFragment})
 * or any classes that are inherited from them.
 */
public class BookmarksDialogFragment extends DialogFragment implements
        TabLayout.BaseOnTabSelectedListener,
        Toolbar.OnMenuItemClickListener {

    @SuppressWarnings("unused")
    private static final String TAG = BookmarksDialogFragment.class.getName();

    public static final String BUNDLE_MODE = "BookmarksDialogFragment_mode";

    public enum DialogMode {
        DIALOG,
        SHEET
    }

    protected BookmarksTabLayout mTabLayout;
    protected Toolbar mToolbar;

    private ArrayList<DialogFragmentTab> mDialogFragmentTabs;
    private PDFViewCtrl mPdfViewCtrl;
    private Bookmark mCurrentBookmark;
    protected int mInitialTabIndex;
    private boolean mHasEventAction;
    private BookmarksTabLayout.BookmarksTabsListener mBookmarksTabsListener;
    private BookmarksDialogListener mBookmarksDialogListener;

    private DialogMode mDialogMode = DialogMode.DIALOG;

    private OnBackPressedCallback mOnBackPressedCallback;

    @DrawableRes
    private int mAnnotationFilterIconRes = 0;
    private String mOutlineEditButtonText;
    private String mOutlineCreateButtonText;

    /**
     * Callback interface to be invoked when an interaction is needed.
     */
    public interface BookmarksDialogListener {

        void onBookmarksDialogWillDismiss(int tabIndex);

        /**
         * Called when the bookmarks dialog has been dismissed.
         *
         * @param tabIndex The index of selected tab when dismissed
         */
        void onBookmarksDialogDismissed(int tabIndex);
    }

    /**
     * Returns a new instance of the class.
     */
    public static BookmarksDialogFragment newInstance() {
        return newInstance(null);
    }

    /**
     * Returns a new instance of the class.
     */
    public static BookmarksDialogFragment newInstance(DialogMode mode) {
        Bundle args = new Bundle();
        args.putString(BUNDLE_MODE, mode != null ? mode.name() : DialogMode.DIALOG.name());
        BookmarksDialogFragment fragment = new BookmarksDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public BookmarksDialogFragment() {

    }

    /**
     * Sets the {@link PDFViewCtrl}
     *
     * @param pdfViewCtrl The {@link PDFViewCtrl}
     * @return This class
     */
    public BookmarksDialogFragment setPdfViewCtrl(@NonNull PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
        return this;
    }

    /**
     * Sets the dialog fragment tabs.
     *
     * @param dialogFragmentTabs A list of dialog fragments that should be shown in separate tabs
     * @return This class
     */
    @SuppressWarnings("unused")
    public BookmarksDialogFragment setDialogFragmentTabs(@NonNull ArrayList<DialogFragmentTab> dialogFragmentTabs) {
        return setDialogFragmentTabs(dialogFragmentTabs, 0);
    }

    /**
     * Sets the dialog fragment tabs.
     *
     * @param dialogFragmentTabs A list of dialog fragments that should be shown in separate tabs
     * @param initialTabIndex    The initial tab index
     * @return This class
     */
    public BookmarksDialogFragment setDialogFragmentTabs(@NonNull ArrayList<DialogFragmentTab> dialogFragmentTabs, int initialTabIndex) {
        mDialogFragmentTabs = dialogFragmentTabs;
        if (dialogFragmentTabs.size() > initialTabIndex) {
            mInitialTabIndex = initialTabIndex;
        }
        return this;
    }

    /**
     * Sets the current bookmark.
     *
     * @param currentBookmark The current bookmark
     * @return This class
     */
    @SuppressWarnings("unused")
    public BookmarksDialogFragment setCurrentBookmark(Bookmark currentBookmark) {
        mCurrentBookmark = currentBookmark;
        return this;
    }

    /**
     * Sets the BookmarksDialogListener listener.
     *
     * @param listener The listener
     */
    @SuppressWarnings("unused")
    public void setBookmarksDialogListener(BookmarksDialogListener listener) {
        mBookmarksDialogListener = listener;
    }

    /**
     * Sets the BookmarksTabsListener listener.
     *
     * @param listener The listener
     */
    @SuppressWarnings("unused")
    public void setBookmarksTabsListener(BookmarksTabLayout.BookmarksTabsListener listener) {
        mBookmarksTabsListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mDialogMode = DialogMode.valueOf(args.getString(BUNDLE_MODE, DialogMode.DIALOG.name()));
        }

        mOnBackPressedCallback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                if (mBookmarksDialogListener != null) {
                    mBookmarksDialogListener.onBookmarksDialogWillDismiss(mTabLayout.getSelectedTabPosition());
                }
            }
        };

        requireActivity().getOnBackPressedDispatcher().addCallback(this, mOnBackPressedCallback);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new Dialog(requireContext(), getTheme()) {
            @Override
            public void onBackPressed() {
                boolean handled = false;
                if (mTabLayout != null && mTabLayout.getCurrentFragment() instanceof NavigationListDialogFragment) {
                    handled = ((NavigationListDialogFragment) mTabLayout.getCurrentFragment()).handleBackPress();
                }
                if (!handled) {
                    super.onBackPressed();
                }
            }
        };
    }

    /**
     * The overloaded implementation of {@link DialogFragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bookmarks_dialog, null);

        FragmentActivity activity = getActivity();

        if (mPdfViewCtrl == null || activity == null) {
            // it can be happened because the dialog is recreated from activity
            // and since this fragment is relying on PDFViewCtrl and PDFViewCtrl cannot be
            // retrieved after re-creation there is no way to reuse the dialog
            return view;
        }

        mToolbar = view.findViewById(R.id.toolbar);

        if (mDialogMode == DialogMode.SHEET) {
            mToolbar.setNavigationIcon(null);

            if (Utils.isLollipop()) {
                AppBarLayout appBarLayout = view.findViewById(R.id.toolbar_container);
                appBarLayout.setStateListAnimator(null);
            }
        }

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean handled = false;
                if (mTabLayout.getCurrentFragment() instanceof NavigationListDialogFragment) {
                    handled = ((NavigationListDialogFragment) mTabLayout.getCurrentFragment()).handleBackPress();
                }
                if (!handled) {
                    if (mBookmarksDialogListener != null) {
                        mBookmarksDialogListener.onBookmarksDialogWillDismiss(mTabLayout.getSelectedTabPosition());
                    }
                }
            }
        });

        // Tab layout
        mTabLayout = view.findViewById(R.id.tabhost);

        if (mDialogMode == DialogMode.SHEET) {
            if (Utils.isLollipop()) {
                mTabLayout.setElevation(0);
            }
        }
        mTabLayout.setBackgroundColor(getBackgroundColor(mTabLayout));

        ViewPager viewPager = view.findViewById(R.id.view_pager);
        viewPager.setOffscreenPageLimit(2);

        if (mDialogFragmentTabs == null) {
            throw new NullPointerException("DialogFragmentTabs cannot be null. Call setDialogFragmentTabs(ArrayList<DialogFragmentTab>)");
        }

        mTabLayout.setup(activity, getChildFragmentManager(), R.id.view_pager,
                mPdfViewCtrl, mCurrentBookmark);
        for (DialogFragmentTab dialogFragmentTab : mDialogFragmentTabs) {
            if (dialogFragmentTab._class == null || dialogFragmentTab.tabTag == null) {
                continue;
            }

            TabLayout.Tab tab = mTabLayout.newTab().setTag(dialogFragmentTab.tabTag);
            if (dialogFragmentTab.tabIcon != null) {
                dialogFragmentTab.tabIcon.mutate();
                tab.setIcon(dialogFragmentTab.tabIcon);
            }
            if (dialogFragmentTab.tabText != null) {
                tab.setText(dialogFragmentTab.tabText);
            }
            mTabLayout.addTab(tab, dialogFragmentTab._class, dialogFragmentTab.bundle);
        }

        mTabLayout.setupWithViewPager(viewPager);

        TabLayout.Tab selectedTab = mTabLayout.getTabAt(mInitialTabIndex);
        if (selectedTab != null) {
            selectedTab.select();
            setToolbarTitleBySelectedTab((String) selectedTab.getTag());
            mTabLayout.onTabSelected(selectedTab);
        }

        int selectedColor = getSelectedColor(mTabLayout);
        int normalColor = getNormalColor(mTabLayout);
        mTabLayout.setTabTextColors(normalColor, selectedColor);

        for (int i = 0, cnt = mTabLayout.getTabCount(); i < cnt; ++i) {
            TabLayout.Tab tab = mTabLayout.getTabAt(i);
            if (tab == null) {
                continue;
            }
            Drawable icon = tab.getIcon();
            if (icon != null) {
                icon.mutate().setColorFilter(tab.isSelected() ? selectedColor : normalColor, PorterDuff.Mode.SRC_IN);
            }
        }

        // If only one tab item is supplied, hide the TabLayout
        if (mDialogFragmentTabs.size() == 1) {
            mTabLayout.setVisibility(View.GONE);
        }

        if (mBookmarksTabsListener != null) {
            mTabLayout.setBookmarksTabsListener(mBookmarksTabsListener);
        }
        mTabLayout.setAnalyticsEventListener(
                new NavigationListDialogFragment.AnalyticsEventListener() {
                    @Override
                    public void onEventAction() {
                        mHasEventAction = true;
                    }
                });

        mHasEventAction = false;

        // click events
        mTabLayout.addOnTabSelectedListener(this);
        return view;
    }

    private void setToolbarTitleBySelectedTab(String tag) {
        String toolbarTitle = getString(R.string.bookmark_dialog_fragment_bookmark_tab_title);
        for (DialogFragmentTab dialogFragmentTab : mDialogFragmentTabs) {
            if (dialogFragmentTab._class == null || dialogFragmentTab.tabTag == null) {
                continue;
            }
            if (dialogFragmentTab.tabTag.equals(tag)) {
                toolbarTitle = dialogFragmentTab.toolbarTitle;
                break;
            }
        }

        mToolbar.setTitle(toolbarTitle);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == R.id.action_close) {
            if (mBookmarksDialogListener != null) {
                mBookmarksDialogListener.onBookmarksDialogWillDismiss(mTabLayout.getSelectedTabPosition());
            }
            return true;
        } else if (id == R.id.action_sort || id == R.id.menu_annotlist_search || id == R.id.menu_action_user_bookmark_search || id == R.id.action_outline_search) {
            if (mTabLayout != null && mToolbar != null) {
                mTabLayout.onPrepareMenu(mToolbar.getMenu(), mTabLayout.getCurrentFragment());
            }
            return true;
        } else {
            if (mTabLayout != null) {
                return mTabLayout.onMenuItemClicked(menuItem, mTabLayout.getCurrentFragment());
            }
            return false;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mTabLayout != null) {
            TabLayout.Tab selectedTab = mTabLayout.getTabAt(mInitialTabIndex);
            AnalyticsHandlerAdapter.getInstance().sendTimedEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_NAVIGATION_LISTS_OPEN,
                    AnalyticsParam.navigationListsTabParam(BookmarksTabLayout.getNavigationId(selectedTab)));
        }

        setMenuVisible(mInitialTabIndex);
    }

    @Override
    public void onStop() {
        super.onStop();
        AnalyticsHandlerAdapter.getInstance().endTimedEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_NAVIGATION_LISTS_OPEN);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mTabLayout != null) {
            TabLayout.Tab selectedTab = mTabLayout.getTabAt(mTabLayout.getSelectedTabPosition());
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_NAVIGATION_LISTS_CLOSE,
                    AnalyticsParam.navigateListCloseParam(selectedTab, mHasEventAction));

            mTabLayout.removeAllFragments();
            mTabLayout.removeAllViews();
            mTabLayout.removeOnTabSelectedListener(this);
            if (mBookmarksDialogListener != null) {
                mBookmarksDialogListener.onBookmarksDialogDismissed(mTabLayout.getSelectedTabPosition());
            }
        }
    }

    /**
     * @hide
     */
    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        mInitialTabIndex = mTabLayout.getSelectedTabPosition();
        setToolbarTitleBySelectedTab((String) tab.getTag());
        Drawable icon = tab.getIcon();
        if (icon != null) {
            setTabIconColor(icon, true);
        }
        setMenuVisible(tab.getPosition());

        DialogFragmentTab tabInfo = mDialogFragmentTabs.get(tab.getPosition());
        if (tabInfo.tabTag.equals(BookmarksTabLayout.TAG_TAB_OUTLINE) &&
                mTabLayout.getCurrentFragment() instanceof OutlineDialogFragment) {
            ((OutlineDialogFragment) mTabLayout.getCurrentFragment()).exitEditMode(false);
        }
        if (mTabLayout.getCurrentFragment() instanceof IBookmarksDialogSearchView) {
            ((IBookmarksDialogSearchView) mTabLayout.getCurrentFragment()).finishSearchView();
        }
        if (mAnnotationFilterIconRes != 0) {
            updateAnnotationFilterIcon(mAnnotationFilterIconRes);
        }
        if (mOutlineCreateButtonText != null && mOutlineEditButtonText != null) {
            updateEditButtonText(mOutlineEditButtonText, mOutlineCreateButtonText);
        }
    }

    /**
     * @hide
     */
    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        Drawable icon = tab.getIcon();
        FragmentActivity activity = getActivity();
        if (icon != null && activity != null) {
            setTabIconColor(icon, false);
        }
    }

    /**
     * @hide
     */
    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

    @Nullable
    public Fragment getCurrentFragment() {
        return mTabLayout != null ? mTabLayout.getCurrentFragment() : null;
    }

    public void updateEditButtonText(String editBtnText, String createBtnText) {
        mOutlineEditButtonText = editBtnText;
        mOutlineCreateButtonText = createBtnText;
        if (mTabLayout != null &&
                mTabLayout.getCurrentFragment() instanceof OutlineDialogFragment) {
            OutlineDialogFragment fragment = (OutlineDialogFragment) mTabLayout.getCurrentFragment();
            fragment.setEditButtonText(editBtnText, createBtnText);
        }
    }

    public void updateAnnotationFilterIcon(@DrawableRes int iconRes) {
        mAnnotationFilterIconRes = iconRes;
        if (mTabLayout != null &&
                mTabLayout.getCurrentFragment() instanceof AnnotationDialogFragment) {
            AnnotationDialogFragment fragment = (AnnotationDialogFragment) mTabLayout.getCurrentFragment();
            fragment.setAnnotationFilterIcon(iconRes);
        }
    }

    private int getBackgroundColor(@NonNull BookmarksTabLayout tabLayout) {
        return mDialogMode == DialogMode.SHEET ?
                tabLayout.getTabLayoutBackgroundSheet() :
                tabLayout.getTabLayoutBackgroundDialog();
    }

    private int getSelectedColor(@NonNull BookmarksTabLayout tabLayout) {
        return mDialogMode == DialogMode.SHEET ?
                tabLayout.getTabTintSelectedColorSheet() :
                tabLayout.getTabTintSelectedColorDialog();
    }

    private int getNormalColor(@NonNull BookmarksTabLayout tabLayout) {
        return mDialogMode == DialogMode.SHEET ?
                tabLayout.getTabTintColorSheet() :
                tabLayout.getTabTintColorDialog();
    }

    private void setTabIconColor(Drawable icon, boolean selected) {
        if (icon != null && mTabLayout != null) {
            int selectedColor = getSelectedColor(mTabLayout);
            int normalColor = getNormalColor(mTabLayout);
            icon.mutate().setColorFilter(selected ? selectedColor : normalColor, PorterDuff.Mode.SRC_IN);
        }
    }

    private void setMenuVisible(int position) {
        if (mToolbar != null && mTabLayout != null &&
                position >= 0 && position < mDialogFragmentTabs.size()) {
            mToolbar.getMenu().clear();
            DialogFragmentTab tabInfo = mDialogFragmentTabs.get(position);
            if (tabInfo != null && tabInfo.menuResId != 0) {
                mToolbar.inflateMenu(tabInfo.menuResId);
                if (tabInfo.tabTag.equals(BookmarksTabLayout.TAG_TAB_ANNOTATION) &&
                        mTabLayout.getCurrentFragment() instanceof AnnotationDialogFragment) {
                    AnnotationDialogFragment fragment = (AnnotationDialogFragment) mTabLayout.getCurrentFragment();
                    if (fragment != null) {
                        MenuItem item = mToolbar.getMenu().findItem(R.id.action_filter);
                        if (item != null) {
                            item.setIcon(fragment.getAnnotationFilterIcon());
                            if (!fragment.isAnnotationFilterEnabled()) {
                                item.setVisible(false);
                            } else if (fragment.isFilterOn()) {
                                item.setIcon(getResources().getDrawable(R.drawable.ic_filter_with_indicator));
                            }
                        }
                    }
                } else if (tabInfo.tabTag.equals(BookmarksTabLayout.TAG_TAB_OUTLINE) &&
                        mTabLayout.getCurrentFragment() instanceof OutlineDialogFragment) {
                    OutlineDialogFragment fragment = (OutlineDialogFragment) mTabLayout.getCurrentFragment();
                    if (fragment != null) {
                        MenuItem item = mToolbar.getMenu().findItem(R.id.action_edit);
                        if (item != null) {
                            item.setVisible(fragment.isEditingEnabled());
                            if (fragment.isEmpty()) {
                                item.setTitle(getString(R.string.create));
                            } else {
                                item.setTitle(getString(R.string.tools_qm_edit));
                            }
                        }
                    }
                }
            }
            if (mDialogMode == DialogMode.SHEET) {
                mToolbar.inflateMenu(R.menu.fragment_navigation_list);
            }
            mToolbar.setOnMenuItemClickListener(this);
        }
    }

    public interface IBookmarksDialogSearchView {
        void finishSearchView();
    }
}
