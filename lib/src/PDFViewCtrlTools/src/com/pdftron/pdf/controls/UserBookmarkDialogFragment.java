//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.github.clans.fab.FloatingActionButton;
import com.pdftron.pdf.Bookmark;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.asynctask.PopulateUserBookmarkListTask;
import com.pdftron.pdf.dialog.BookmarksDialogFragment;
import com.pdftron.pdf.dialog.simplelist.EditListAdapter;
import com.pdftron.pdf.dialog.simplelist.EditListItemTouchHelperCallback;
import com.pdftron.pdf.dialog.simplelist.EditListViewHolder;
import com.pdftron.pdf.model.UserBookmarkItem;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.BookmarkManager;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerView;
import com.pdftron.pdf.widget.recyclerview.ViewHolderBindListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import co.paulburke.android.itemtouchhelperdemo.helper.ItemTouchHelperAdapter;

/**
 * The UserBookmarkDialogFragment shows a list of user-defined bookmarks that can be used to navigate
 * the document in the {@link com.pdftron.pdf.PDFViewCtrl}. This is different from {@link com.pdftron.pdf.controls.OutlineDialogFragment}
 * as user can add new custom bookmarks with any name at any time.
 * Modification to existing bookmarks is also supported.
 */
public class UserBookmarkDialogFragment extends NavigationListDialogFragment implements SearchView.OnQueryTextListener, BookmarksDialogFragment.IBookmarksDialogSearchView {

    @SuppressWarnings("unused")
    private static final String TAG = UserBookmarkDialogFragment.class.getName();

    /**
     * Bundle key to set the file path
     */
    public static final String BUNDLE_FILE_PATH = "file_path";

    /**
     * Bundle key to specify whether the document is read only or not
     */
    public static final String BUNDLE_IS_READ_ONLY = "is_read_only";
    public static final String BUNDLE_ALLOW_EDITING = "allow_editing";
    public static final String BUNDLE_BOOKMARK_CREATION_ENABLED = "bookmark_creation_enabled";
    public static final String BUNDLE_AUTO_SORT_BOOKMARKS = "auto_sort_bookmarks";
    public static final String BUNDLE_EDITING_MODE = "editing_mode";

    private static final int CONTEXT_MENU_EDIT_ITEM = 0;
    private static final int CONTEXT_MENU_DELETE_ITEM = 1;
    private static final int CONTEXT_MENU_DELETE_ALL = 2;

    public static final int CONTEXT_MENU_EDIT_ITEM_BIT = 0x01 << 0;
    public static final int CONTEXT_MENU_DELETE_ITEM_BIT = 0x01 << 1;
    public static final int CONTEXT_MENU_DELETE_ALL_BIT = 0x01 << 2;

    private PopulateUserBookmarkListTask mTask;

    private ArrayList<UserBookmarkItem> mSource = new ArrayList<>();
    private UserBookmarksAdapter mUserBookmarksAdapter;

    private SimpleRecyclerView mRecyclerView;
    private ItemTouchHelper mItemTouchHelper;
    private EditListItemTouchHelperCallback mTouchCallback;

    private FloatingActionButton mFab;

    private PDFViewCtrl mPdfViewCtrl;
    private boolean mReadOnly;
    private boolean mAllowEditing = true;
    private boolean mBookmarkCreationEnabled = true;
    private String mFilePath;

    private MenuItem mSearchMenuItem;
    private boolean mIsSearchMode;
    private String mQueryText = "";

    private boolean mModified;
    private boolean mRebuild;

    private boolean mAutoSortBookmarks = true;

    private boolean mIsCustomEditingMode;
    private int mEditingMode = UserBookmarkDialogFragment.CONTEXT_MENU_EDIT_ITEM_BIT | UserBookmarkDialogFragment.CONTEXT_MENU_DELETE_ITEM_BIT | UserBookmarkDialogFragment.CONTEXT_MENU_DELETE_ALL_BIT;

    private UserBookmarkDialogListener mUserBookmarkDialogListener;

    /**
     * Callback interface to be invoked when an interaction is needed.
     */
    public interface UserBookmarkDialogListener {
        /**
         * Called when a user bookmark has been clicked.
         *
         * @param pageNum The page number
         */
        void onUserBookmarkClicked(int pageNum);

        /**
         * Called when user bookmark's edittext gains or loses focus
         *
         * @param isActive EditText is in focus
         */
        void onEditBookmarkFocusChanged(boolean isActive);
    }

    /**
     * Returns a new instance of the class
     */
    public static UserBookmarkDialogFragment newInstance() {
        return new UserBookmarkDialogFragment();
    }

    /**
     * Sets the {@link PDFViewCtrl}
     *
     * @param pdfViewCtrl The PDFViewCtrl
     * @return This class
     */
    public UserBookmarkDialogFragment setPdfViewCtrl(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
        return this;
    }

    /**
     * Sets the file path. If not specified it is extracted from PDFViewCtrl.
     *
     * @param filePath The file path
     * @return This class
     */
    public UserBookmarkDialogFragment setFilePath(String filePath) {
        Bundle args = getArguments();
        if (args == null) {
            args = new Bundle();
        }
        args.putString(BUNDLE_FILE_PATH, filePath);
        setArguments(args);

        return this;
    }

    /**
     * Sets if the document is read only
     *
     * @param isReadOnly True if the document is read only
     * @return This class
     */
    @SuppressWarnings("unused")
    public UserBookmarkDialogFragment setReadOnly(boolean isReadOnly) {
        Bundle args = getArguments();
        if (args == null) {
            args = new Bundle();
        }
        args.putBoolean(BUNDLE_IS_READ_ONLY, isReadOnly);
        setArguments(args);

        return this;
    }

    /**
     * Sets if the user bookmark dialog allow modification to existing items
     *
     * @param allowEditing True if allow modification to existing items
     * @return This class
     */
    public UserBookmarkDialogFragment setAllowEditing(boolean allowEditing) {
        Bundle args = getArguments();
        if (args == null) {
            args = new Bundle();
        }
        args.putBoolean(BUNDLE_ALLOW_EDITING, allowEditing);
        setArguments(args);

        return this;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mReadOnly = args.getBoolean(BUNDLE_IS_READ_ONLY, false);
            mAllowEditing = args.getBoolean(BUNDLE_ALLOW_EDITING, true);
            if (args.containsKey(BUNDLE_EDITING_MODE)) {
                mIsCustomEditingMode = true;
                mEditingMode = args.getInt(BUNDLE_EDITING_MODE);
            }
            mBookmarkCreationEnabled = args.getBoolean(BUNDLE_BOOKMARK_CREATION_ENABLED, true);
            mAutoSortBookmarks = args.getBoolean(BUNDLE_AUTO_SORT_BOOKMARKS, true);
            mFilePath = args.getString(BUNDLE_FILE_PATH, null);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.controls_fragment_bookmark_dialog, null);

        mUserBookmarksAdapter = new UserBookmarksAdapter(getActivity(), mSource, null);
        mRecyclerView = view.findViewById(R.id.controls_bookmark_recycler_view);
        mRecyclerView.initView(0, 0);

        mRecyclerView.setAdapter(mUserBookmarksAdapter);

        mUserBookmarksAdapter.setAllowEditing(mAllowEditing);

        mFab = view.findViewById(R.id.control_bookmark_add);
        if (!mAllowEditing || !mBookmarkCreationEnabled) {
            mFab.setVisibility(View.GONE);
        }
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getContext();
                if (context == null || mPdfViewCtrl == null || mPdfViewCtrl.getDoc() == null) {
                    return;
                }
                try {
                    int curPageNum = mPdfViewCtrl.getCurrentPage();
                    long curObjNum = mPdfViewCtrl.getDoc().getPage(curPageNum).getSDFObj().getObjNum();
                    UserBookmarkItem item = new UserBookmarkItem(context, curObjNum, curPageNum);
                    if (!mUserBookmarksAdapter.containsBookmarkOnPage(item.pageNumber)) {
                        mUserBookmarksAdapter.add(item);
                        mModified = true;
                        if (mAutoSortBookmarks) {
                            int index = mUserBookmarksAdapter.indexOf(item);
                            mUserBookmarksAdapter.notifyDataSetChanged();
                            mRecyclerView.smoothScrollToPosition(index);
                        } else {
                            mUserBookmarksAdapter.notifyItemInserted(mUserBookmarksAdapter.getItemCount() - 1);
                            mRecyclerView.smoothScrollToPosition(mUserBookmarksAdapter.getItemCount() - 1);
                        }
                        commitData();
                    } else {
                        // show toast notify user that the bookmark exists
                        CommonToast.showText(getContext(), getContext().getResources().getString(R.string.controls_user_bookmark_dialog_bookmark_exist_warning), Toast.LENGTH_SHORT);
                    }
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
                onEventAction();
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_USER_BOOKMARKS,
                        AnalyticsParam.userBookmarksActionParam(AnalyticsHandlerAdapter.USER_BOOKMARKS_ADD));
            }
        });

        ItemClickHelper itemClickHelper = new ItemClickHelper();
        itemClickHelper.attachToRecyclerView(mRecyclerView);

        // for dragging
        if (!mAutoSortBookmarks) {
            mTouchCallback = new EditListItemTouchHelperCallback(mUserBookmarksAdapter, mAllowEditing, getResources().getColor(R.color.gray));
            mItemTouchHelper = new ItemTouchHelper(mTouchCallback);
            mItemTouchHelper.attachToRecyclerView(mRecyclerView);
        }

        itemClickHelper.setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView recyclerView, View view, int position, long id) {
                if (mUserBookmarkDialogListener != null) {
                    UserBookmarkItem item = mUserBookmarksAdapter.getItem(position);
                    if (item == null) {
                        return;
                    }
                    mUserBookmarkDialogListener.onUserBookmarkClicked(item.pageNumber);
                    onEventAction();
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_NAVIGATE_BY,
                            AnalyticsParam.viewerNavigateByParam(AnalyticsHandlerAdapter.VIEWER_NAVIGATE_BY_USER_BOOKMARK));
                }
            }
        });

        if (!mAutoSortBookmarks) {
            itemClickHelper.setOnItemLongClickListener(new ItemClickHelper.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(RecyclerView recyclerView, View v, final int position, final long id) {
                    if (!mAllowEditing) {
                        return true;
                    }
                    mRecyclerView.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mTouchCallback != null) {
                                mTouchCallback.setDragging(true);
                            }
                            RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(position);
                            if (holder != null) {
                                mItemTouchHelper.startDrag(holder);
                            }
                        }
                    });

                    return true;
                }
            });
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBookmarks();
    }

    @Override
    public void onPause() {
        if (mUserBookmarksAdapter != null) {
            // clear focus for any edit text
            mUserBookmarksAdapter.commitEditing();
        }
        commitData();
        if (mIsSearchMode) {
            finishSearchView();
        }
        super.onPause();
    }

    @Override
    public boolean handleBackPress() {
        if (mIsSearchMode) {
            finishSearchView();
            return true;
        }
        return super.handleBackPress();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (mRecyclerView != null) {
            mRecyclerView.requestFocus();
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mQueryText = newText;
        loadBookmarks();
        return false;
    }

    public void finishSearchView() {
        if (mSearchMenuItem != null && mSearchMenuItem.isActionViewExpanded()) {
            mSearchMenuItem.collapseActionView();
        }
        resetBookmarkListFilter();
        mIsSearchMode = false;
    }

    public void prepareOptionsMenu(Menu menu) {
        if (null == menu) {
            return;
        }
        mSearchMenuItem = menu.findItem(R.id.menu_action_user_bookmark_search);
        if (mSearchMenuItem != null) {
            SearchView searchView = (SearchView) mSearchMenuItem.getActionView();
            searchView.setOnQueryTextListener(this);
            searchView.setSubmitButtonEnabled(false);

            if (!Utils.isNullOrEmpty(mQueryText)) {
                mSearchMenuItem.expandActionView();
                searchView.setQuery(mQueryText, true);
                mQueryText = "";
            }

            // We need to override this method to get the collapse event, so we can
            // clear the filter.
            mSearchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {

                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    // Let's return true to expand the view.
                    mIsSearchMode = true;
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    resetBookmarkListFilter();
                    mIsSearchMode = false;
                    return true;
                }
            });
        }
    }

    public void resetBookmarkListFilter() {
        String filterText = getQueryText();
        if (!Utils.isNullOrEmpty(filterText)) {
            if (mUserBookmarksAdapter != null) {
                onQueryTextSubmit("");
            }
        }
    }

    public String getQueryText() {
        if (!Utils.isNullOrEmpty(mQueryText)) {
            return mQueryText;
        }

        String queryText = "";
        if (mSearchMenuItem != null) {
            SearchView searchView = (SearchView) mSearchMenuItem.getActionView();
            queryText = searchView.getQuery().toString();
        }
        return queryText;
    }

    /**
     * Sets the listener to {@link UserBookmarkDialogFragment}
     *
     * @param listener The listener
     */
    public void setUserBookmarkListener(UserBookmarkDialogListener listener) {
        mUserBookmarkDialogListener = listener;
    }

    private void onShowPopupMenu(final int position, View anchor) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), anchor);
        Menu menu = popupMenu.getMenu();

        String[] menuItems = getResources().getStringArray(R.array.user_bookmark_dialog_context_menu);
        menu.add(Menu.NONE, CONTEXT_MENU_EDIT_ITEM, CONTEXT_MENU_EDIT_ITEM, menuItems[CONTEXT_MENU_EDIT_ITEM]);
        menu.add(Menu.NONE, CONTEXT_MENU_DELETE_ITEM, CONTEXT_MENU_DELETE_ITEM, menuItems[CONTEXT_MENU_DELETE_ITEM]);
        menu.add(Menu.NONE, CONTEXT_MENU_DELETE_ALL, CONTEXT_MENU_DELETE_ALL, menuItems[CONTEXT_MENU_DELETE_ALL]);
        MenuItem.OnMenuItemClickListener listener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                onPopupItemSelected(item, position);
                return true;
            }
        };
        menu.getItem(CONTEXT_MENU_EDIT_ITEM).setOnMenuItemClickListener(listener);
        menu.getItem(CONTEXT_MENU_DELETE_ITEM).setOnMenuItemClickListener(listener);
        menu.getItem(CONTEXT_MENU_DELETE_ALL).setOnMenuItemClickListener(listener);

        if (mIsCustomEditingMode) {
            menu.findItem(CONTEXT_MENU_EDIT_ITEM).setVisible((mEditingMode & CONTEXT_MENU_EDIT_ITEM_BIT) != 0);
            menu.findItem(CONTEXT_MENU_DELETE_ITEM).setVisible((mEditingMode & CONTEXT_MENU_DELETE_ITEM_BIT) != 0);
            menu.findItem(CONTEXT_MENU_DELETE_ALL).setVisible((mEditingMode & CONTEXT_MENU_DELETE_ALL_BIT) != 0);
        }

        popupMenu.show();
    }

    private void onPopupItemSelected(MenuItem item, int position) {
        if (mPdfViewCtrl == null) {
            return;
        }

        int menuItemIndex = item.getItemId();
        switch (menuItemIndex) {
            case CONTEXT_MENU_EDIT_ITEM:
                mModified = true;
                mUserBookmarksAdapter.setEditing(true);
                mFab.setVisibility(View.GONE);
                mUserBookmarksAdapter.setSelectedIndex(position);
                Utils.safeNotifyDataSetChanged(mUserBookmarksAdapter);
                onEventAction();
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_USER_BOOKMARKS,
                        AnalyticsParam.userBookmarksActionParam(AnalyticsHandlerAdapter.USER_BOOKMARKS_RENAME));
                break;
            case CONTEXT_MENU_DELETE_ITEM:
                UserBookmarkItem userBookmarkItem = mUserBookmarksAdapter.getItem(position);
                if (userBookmarkItem == null) {
                    return;
                }
                mModified = true;
                boolean hasChange = false;
                ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                if (!mReadOnly) {
                    PDFDoc pdfDoc = mPdfViewCtrl.getDoc();
                    if (pdfDoc != null) {
                        boolean shouldUnlock = false;
                        try {
                            pdfDoc.lock();
                            shouldUnlock = true;
                            if (userBookmarkItem.pdfBookmark != null) {
                                userBookmarkItem.pdfBookmark.delete();
                            }
                            hasChange = pdfDoc.hasChangesSinceSnapshot();
                        } catch (Exception e) {
                            AnalyticsHandlerAdapter.getInstance().sendException(e);
                        } finally {
                            if (shouldUnlock) {
                                Utils.unlockQuietly(pdfDoc);
                            }
                        }
                    }
                }
                mUserBookmarksAdapter.remove(userBookmarkItem);
                if (hasChange) {
                    if (toolManager != null) {
                        toolManager.raiseBookmarkModified(mUserBookmarksAdapter.mBookmarks);
                    }
                }
                Utils.safeNotifyDataSetChanged(mUserBookmarksAdapter);
                commitData();

                onEventAction();
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_USER_BOOKMARKS,
                        AnalyticsParam.userBookmarksActionParam(AnalyticsHandlerAdapter.USER_BOOKMARKS_DELETE));
                break;
            case CONTEXT_MENU_DELETE_ALL:
                mModified = true;

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.controls_bookmark_dialog_delete_all_message)
                        .setTitle(R.string.controls_misc_delete_all)
                        .setPositiveButton(R.string.tools_misc_yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (!mReadOnly) {
                                    BookmarkManager.removeRootPdfBookmark(mPdfViewCtrl, true);
                                }
                                mUserBookmarksAdapter.clear();
                                Utils.safeNotifyDataSetChanged(mUserBookmarksAdapter);
                                commitData();

                                onEventAction();
                                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_USER_BOOKMARKS,
                                        AnalyticsParam.userBookmarksActionParam(AnalyticsHandlerAdapter.USER_BOOKMARKS_DELETE_ALL));
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .create()
                        .show();
                break;
        }
    }

    public void loadBookmarks() {
        if (mPdfViewCtrl == null || mPdfViewCtrl.getDoc() == null) {
            return;
        }

        if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED) {
            mTask.cancel(true);
        }

        if (mUserBookmarksAdapter != null) {
            // clear focus for any edit text
            mUserBookmarksAdapter.setEditing(false);
            Utils.safeNotifyDataSetChanged(mUserBookmarksAdapter);
        }

        try {
            Bookmark rootBookmark = BookmarkManager.getRootPdfBookmark(mPdfViewCtrl.getDoc(), false);
            if (Utils.isNullOrEmpty(mFilePath)) {
                mFilePath = mPdfViewCtrl.getDoc().getFileName();
            }
            mTask = new PopulateUserBookmarkListTask(mPdfViewCtrl.getContext(), mFilePath, rootBookmark, mReadOnly, mQueryText);
            mTask.setCallback(new PopulateUserBookmarkListTask.Callback() {
                @Override
                public void getUserBookmarks(List<UserBookmarkItem> bookmarks, boolean modified) {
                    mModified = modified;
                    mUserBookmarksAdapter.clear();
                    mUserBookmarksAdapter.addAll(bookmarks);
                    Utils.safeNotifyDataSetChanged(mUserBookmarksAdapter);
                }
            });
            mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    private void commitData() {
        if (mPdfViewCtrl == null || mPdfViewCtrl.getDoc() == null) {
            return;
        }

        if (!mModified) {
            return;
        }
        if (mReadOnly) {
            try {
                if (Utils.isNullOrEmpty(mFilePath)) {
                    mFilePath = mPdfViewCtrl.getDoc().getFileName();
                }
                BookmarkManager.saveUserBookmarks(mPdfViewCtrl.getContext(), mPdfViewCtrl, mFilePath, mSource);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        } else {
            BookmarkManager.savePdfBookmarks(mPdfViewCtrl, mSource, true, mRebuild);
            mRebuild = false;
        }
    }

    private class UserBookmarksAdapter extends EditListAdapter<UserBookmarkItem>
            implements ItemTouchHelperAdapter {

        private ArrayList<UserBookmarkItem> mBookmarks;
        private Context mContext;

        UserBookmarksAdapter(Context context, ArrayList<UserBookmarkItem> bookmarks, ViewHolderBindListener bindListener) {
            super(bindListener);

            mContext = context;
            mBookmarks = bookmarks;
        }

        public void clear() {
            mBookmarks.clear();
        }

        public void addAll(List<UserBookmarkItem> listBookmarks) {
            mBookmarks.addAll(listBookmarks);
            sort();
        }

        @Override
        public boolean onItemMove(int fromPosition, int toPosition) {
            UserBookmarkItem oldItem = mBookmarks.get(fromPosition);
            UserBookmarkItem item = new UserBookmarkItem();
            item.pageObjNum = oldItem.pageObjNum;
            item.pageNumber = oldItem.pageNumber;
            item.title = oldItem.title;

            for (UserBookmarkItem uitem : mBookmarks) {
                uitem.pdfBookmark = null;
            }
            mRebuild = true;

            mBookmarks.remove(fromPosition);
            mBookmarks.add(toPosition, item);

            notifyItemMoved(fromPosition, toPosition);
            mModified = true;
            commitData();

            return true;
        }

        @Override
        public void onItemDrop(int fromPosition, int toPosition) {

        }

        @Override
        public void onItemDismiss(int position) {

        }

        @Override
        public UserBookmarkItem getItem(int position) {
            if (mBookmarks != null && isValidIndex(position)) {
                return mBookmarks.get(position);
            }
            return null;
        }

        @Override
        public void add(UserBookmarkItem item) {
            mBookmarks.add(item);
            sort();
        }

        public int indexOf(UserBookmarkItem item) {
            return mBookmarks.indexOf(item);
        }

        public boolean contains(UserBookmarkItem item) {
            return mBookmarks.contains(item);
        }

        /**
         * Returns whether a specified page contains a bookmark.
         *
         * @param pageNumber page to check for bookmarks
         * @return True if the given page has a bookmark, false otherwise
         */
        public boolean containsBookmarkOnPage(int pageNumber) {
            for (UserBookmarkItem bookmark : mBookmarks) {
                if (bookmark.pageNumber == pageNumber) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean remove(UserBookmarkItem item) {
            if (mBookmarks.contains(item)) {
                mBookmarks.remove(item);
                return true;
            }
            return false;
        }

        @Override
        public UserBookmarkItem removeAt(int location) {
            if (isValidIndex(location)) {
                return mBookmarks.remove(location);
            }
            return null;
        }

        @Override
        public void insert(UserBookmarkItem item, int position) {
            mBookmarks.add(position, item);
            sort();
        }

        @Override
        public void updateSpanCount(int count) {

        }

        private void sort() {
            if (mAutoSortBookmarks) {
                Collections.sort(mBookmarks, new Comparator<UserBookmarkItem>() {
                    @Override
                    public int compare(UserBookmarkItem o1, UserBookmarkItem o2) {
                        //noinspection UseCompareMethod
                        return Integer.valueOf(o1.pageNumber).compareTo(o2.pageNumber);
                    }
                });
            }
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull final EditListViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);

            UserBookmarkItem item = mBookmarks.get(position);

            holder.itemView.getBackground().setColorFilter(null);
            holder.itemView.getBackground().invalidateSelf();
            holder.textView.setText(item.title);

            holder.pageNumber.setText(Integer.toString(item.pageNumber));

            if (mEditing && position == mSelectedIndex) {
                holder.editText.setText(item.title);
                holder.editText.requestFocus();
                holder.editText.selectAll();
                Utils.showSoftKeyboard(holder.editText.getContext(), null); // force
            }

            if (mEditing) {
                holder.pageNumber.setVisibility(View.GONE);
            } else {
                holder.pageNumber.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void contextButtonClicked(@NonNull EditListViewHolder holder, View v) {
            if (!mEditing) {
                onShowPopupMenu(holder.getAdapterPosition(), v);
            } else {
                holder.itemView.requestFocus();
            }
        }

        @Override
        protected void handleEditTextFocusChange(@NonNull EditListViewHolder holder, View v, boolean hasFocus) {
            if (hasFocus) {
                mUserBookmarkDialogListener.onEditBookmarkFocusChanged(true);
                return;
            } else {
                mUserBookmarkDialogListener.onEditBookmarkFocusChanged(false);
            }
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) {
                return;
            }

            Utils.hideSoftKeyboard(v.getContext(), v);

            saveEditTextChanges((TextView) v, pos);
        }

        @Override
        public int getItemCount() {
            return mBookmarks.size();
        }

        private void saveEditTextChanges(TextView v, int position) {
            v.clearFocus();

            setEditing(false);
            mFab.setVisibility(View.VISIBLE);
            String title = v.getText().toString();
            if (title.isEmpty()) {
                title = mContext.getString(R.string.empty_title);
            }
            UserBookmarkItem userBookmarkItem = mUserBookmarksAdapter.getItem(position);
            if (userBookmarkItem == null) {
                return;
            }
            userBookmarkItem.title = title;
            userBookmarkItem.isBookmarkEdited = true;
            Utils.safeNotifyDataSetChanged(this);
            commitData();
        }
    }
}
