//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Slide;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import com.google.android.material.textfield.TextInputLayout;
import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Action;
import com.pdftron.pdf.ActionParameter;
import com.pdftron.pdf.Bookmark;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.dialog.BookmarksDialogFragment;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.ActionUtils;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.BookmarkManager;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.EventHandler;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.recyclertreeview.BookmarkNode;
import com.pdftron.recyclertreeview.BookmarkNodeBinder;
import com.pdftron.recyclertreeview.EditOutlineMoveDialog;
import com.pdftron.recyclertreeview.ItemMoveCallback;
import com.pdftron.recyclertreeview.OutlineTreeViewAdapter;
import com.pdftron.recyclertreeview.TreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * The OutlineDialogFragment shows a document outline (bookmarks) that can be
 * used to navigate the document in the PDFViewCtrl.
 */
public class OutlineDialogFragment extends NavigationListDialogFragment implements SearchView.OnQueryTextListener, BookmarkNodeBinder.BookmarkNodeClickListener, OutlineTreeViewAdapter.OnBookmarkTreeNodeListener, EditOutlineMoveDialog.EditOutlineMoveClickListener, BookmarksDialogFragment.IBookmarksDialogSearchView {

    public static final String BUNDLE_EDITING_ENABLED = "OutlineDialogFragment_editing_enabled";
    public static final String BUNDLE_CREATE_BUTTON = "OutlineDialogFragment_create_button";
    public static final String BUNDLE_EDIT_BUTTON = "OutlineDialogFragment_edit_button";

    private PDFViewCtrl mPdfViewCtrl;
    private OutlineTreeViewAdapter mTreeViewAdapter;
    private ItemTouchHelper mItemTouchHelper;
    private RecyclerView mRecyclerView;
    private View mFragmentView;
    private AlertDialog mEditEntryDialog;
    private AlertDialog mAddEntryDialog;
    private AlertDialog mDeleteEntryDialog;
    private EditOutlineMoveDialog mMoveEntryDialogFragment;
    private final List<TreeNode<BookmarkNode>> mSelectedNodes = new ArrayList<>();

    private final CompositeDisposable mDisposables = new CompositeDisposable();

    /**
     * The last clicked bookmark so we can scroll back to it
     */
    private Bookmark mCurrentBookmark;

    private OutlineDialogListener mOutlineDialogListener;

    private boolean mEditingEnabled = true;
    private String mEditButtonTxt;
    private String mCreateButtonTxt;
    private boolean mModified;

    private MenuItem mSearchMenuItem;
    private boolean mIsSearchMode;
    private String mQueryText = "";

    private Theme mTheme;

    /**
     * Callback interface to be invoked when an interaction is needed.
     */
    public interface OutlineDialogListener {
        /**
         * Called when an outline has been clicked.
         *
         * @param parent   The parent bookmark if any
         * @param bookmark The clicked bookmark
         */
        void onOutlineClicked(Bookmark parent, Bookmark bookmark);
    }

    /**
     * Returns a new instance of the class
     */
    public static OutlineDialogFragment newInstance() {
        return new OutlineDialogFragment();
    }

    public static OutlineDialogFragment newInstance(boolean editingEnabled, String editButtonEditTxt, String createButtonEditText) {
        Bundle args = new Bundle();
        args.putBoolean(BUNDLE_EDITING_ENABLED, editingEnabled);
        args.putString(BUNDLE_EDIT_BUTTON, editButtonEditTxt);
        args.putString(BUNDLE_CREATE_BUTTON, createButtonEditText);
        OutlineDialogFragment fragment = new OutlineDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Sets the {@link PDFViewCtrl}
     *
     * @param pdfViewCtrl The {@link PDFViewCtrl}
     * @return This class
     */
    public OutlineDialogFragment setPdfViewCtrl(@NonNull PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
        return this;
    }

    /**
     * Sets the current bookmark.
     *
     * @param currentBookmark The current bookmark
     * @return This class
     */
    public OutlineDialogFragment setCurrentBookmark(@Nullable Bookmark currentBookmark) {
        mCurrentBookmark = currentBookmark;
        return this;
    }

    /**
     * Sets the OutlineDialogListener listener.
     *
     * @param listener The listener
     */
    public void setOutlineDialogListener(OutlineDialogListener listener) {
        mOutlineDialogListener = listener;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindUi(view);
    }

    private void bindUi(View view) {
        mFragmentView = view;
        view.findViewById(R.id.edit_outline_add).setOnClickListener(addBookmarkClickListener());
        view.findViewById(R.id.edit_outline_edit_entry).setOnClickListener(editClickListener());
        view.findViewById(R.id.edit_outline_move).setOnClickListener(moveBookmarkClickListener());
        view.findViewById(R.id.edit_outline_delete).setOnClickListener(deleteBookmarkClickListener());
    }

    @Override
    public boolean handleBackPress() {
        if (mTreeViewAdapter.isEditingOutline()) {
            exitEditMode(false);
            return true;
        }
        if (mIsSearchMode) {
            finishSearchView();
            return true;
        }
        return super.handleBackPress();
    }

    public void setEditButtonText(String editBtnText, String createButtonTxt) {
        mEditButtonTxt = editBtnText;
        mCreateButtonTxt = createButtonTxt;
        setMenuItemText();
    }

    private OnClickListener moveBookmarkClickListener() {
        EditOutlineMoveDialog.EditOutlineMoveClickListener listener = this;
        return new OnClickListener() {
            @Override
            public void onClick(View view) {
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_EDIT_OUTLINE,
                        AnalyticsParam.editOutlineParam(AnalyticsHandlerAdapter.EVENT_EDIT_OUTLINE_MOVE));
                if (mSelectedNodes.size() != 1) {
                    return;
                }
                if (mPdfViewCtrl == null) {
                    return;
                }
                BookmarkNode selected = mSelectedNodes.get(0).getContent();
                ArrayList<Bookmark> temp = new ArrayList<>(BookmarkManager.getBookmarkList(mPdfViewCtrl.getDoc(), null));
                mMoveEntryDialogFragment = new EditOutlineMoveDialog(R.string.edit_pdf_outline_move_to_entry, temp, mPdfViewCtrl, listener, selected.getBookmark());
                mMoveEntryDialogFragment.setStyle(DialogFragment.STYLE_NORMAL, ((ToolManager) mPdfViewCtrl.getToolManager()).getTheme());
                if (getActivity() != null) {
                    mMoveEntryDialogFragment.show(getActivity().getSupportFragmentManager(), "edit_outline_move_dialog");
                }
            }
        };
    }

    private OnClickListener deleteBookmarkClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {

                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_EDIT_OUTLINE,
                        AnalyticsParam.editOutlineParam(AnalyticsHandlerAdapter.EVENT_EDIT_OUTLINE_DELETE));
                deleteBookmark(v);
            }
        };
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mModified && mPdfViewCtrl != null) {
            ((ToolManager) mPdfViewCtrl.getToolManager()).raisePdfOutlineModified();
        }
        if (mTreeViewAdapter != null) { // required to prevent state where mTreeViewAdapter has not been initialized
            exitEditMode(true);
        }
        if (mIsSearchMode) {
            finishSearchView();
        }
    }

    public boolean isEditingEnabled() {
        return mEditingEnabled;
    }

    public boolean isEmpty() {
        return mTreeViewAdapter == null || mTreeViewAdapter.getItemCount() == 0;
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
        populateBookmarksTreeView(false);
        return false;
    }

    public void prepareOutlineSearch(Menu menu) {
        mSearchMenuItem = menu.findItem(R.id.action_outline_search);
        if (mSearchMenuItem != null) {
            SearchView searchView = (SearchView) mSearchMenuItem.getActionView();
            searchView.setOnQueryTextListener(this);
            searchView.setSubmitButtonEnabled(false);

            if (!Utils.isNullOrEmpty(mQueryText)) {
                mSearchMenuItem.expandActionView();
                searchView.setQuery(mQueryText, true);
                mQueryText = "";
            }

            final MenuItem editOutlineItem = menu.findItem(R.id.action_edit);
            final MenuItem collapseItem = menu.findItem(R.id.action_collapse);
            // We need to override this method to get the collapse event, so we can
            // clear the filter.
            mSearchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {

                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    // Let's return true to expand the view.
                    if (editOutlineItem != null) {
                        editOutlineItem.setVisible(false);
                    }
                    if (collapseItem != null) {
                        collapseItem.setVisible(false);
                    }
                    mIsSearchMode = true;
                    mTreeViewAdapter.setIsSearchMode(true);
                    populateBookmarksTreeView(false);
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    if (editOutlineItem != null) {
                        editOutlineItem.setVisible(isEditingEnabled());
                    }
                    if (collapseItem != null) {
                        collapseItem.setVisible(true);
                    }
                    mIsSearchMode = false;
                    mTreeViewAdapter.setIsSearchMode(false);
                    resetBookmarkListFilter();
                    populateBookmarksTreeView(false);
                    return true;
                }
            });
        }
    }

    public void resetBookmarkListFilter() {
        String filterText = getQueryText();
        if (!Utils.isNullOrEmpty(filterText)) {
            if (mTreeViewAdapter != null) {
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

    public void finishSearchView() {
        if (mSearchMenuItem != null && mSearchMenuItem.isActionViewExpanded()) {
            mSearchMenuItem.collapseActionView();
        }
        resetBookmarkListFilter();
        mIsSearchMode = false;
    }

    private OnClickListener addBookmarkClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_EDIT_OUTLINE,
                        AnalyticsParam.editOutlineParam(AnalyticsHandlerAdapter.EVENT_EDIT_OUTLINE_ADD));
                if (mPdfViewCtrl == null) {
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                if (mSelectedNodes.isEmpty()) {
                    builder.setTitle(R.string.edit_pdf_outline_add_entry);
                } else if (mSelectedNodes.size() == 1) {
                    builder.setTitle(R.string.edit_pdf_outline_add_sub_entry);
                }
                final View viewInflated = LayoutInflater.from(v.getContext()).inflate(R.layout.dialog_edit_outline_add_entry, (ViewGroup) getView(), false);
                final EditText bookmarkNameEditText = viewInflated.findViewById(R.id.dialog_add_entry_name);
                final EditText pageNumberEditText = viewInflated.findViewById(R.id.dialog_add_entry_page_number);
                final TextInputLayout pageNumberInputLayout = viewInflated.findViewById(R.id.dialog_add_entry_page_number_input_layout);
                String hint = String.format(v.getContext().getResources().getString(R.string.dialog_gotopage_number), 1, mPdfViewCtrl.getPageCount());
                pageNumberInputLayout.setHint(hint);

                builder.setView(viewInflated);
                builder.setPositiveButton(getString(R.string.ok), null);
                builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                mAddEntryDialog = builder.create();
                mAddEntryDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        bookmarkNameEditText.requestFocus();
                        Button okButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                        okButton.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                resetAddEntryErrors(viewInflated);
                                String name = bookmarkNameEditText.getText().toString();
                                String page = pageNumberEditText.getText().toString();
                                if (isValidNameAndPageNumber(viewInflated, v.getContext(), name, page)) {
                                    mModified = true;
                                    if (mPdfViewCtrl != null && mPdfViewCtrl.getDoc() != null) {
                                        BookmarkNode parentBookmarkNode = null;
                                        if (mSelectedNodes.size() == 1) {
                                            parentBookmarkNode = mSelectedNodes.get(0).getContent();
                                        }
                                        BookmarkNode bookmarkNode = new BookmarkNode(mPdfViewCtrl.getDoc(), null);
                                        bookmarkNode.setTitle(name);
                                        bookmarkNode.setPageNumber(Integer.parseInt(page));
                                        addBookmark(bookmarkNode.commitAdd(parentBookmarkNode));
                                    }
                                    dialog.dismiss();
                                }
                            }
                        });
                    }
                });
                mAddEntryDialog.show();
            }
        };
    }

    private void resetAddEntryErrors(View viewInflated) {
        TextInputLayout nameInputLayout = viewInflated.findViewById(R.id.dialog_add_entry_name_input_layout);
        nameInputLayout.setErrorEnabled(false);
        TextInputLayout numberInputLayout = viewInflated.findViewById(R.id.dialog_add_entry_page_number_input_layout);
        numberInputLayout.setErrorEnabled(false);
    }

    private boolean isValidNameAndPageNumber(View viewInflated, Context context, String name, String page) {
        if (name.isEmpty()) {
            TextInputLayout inputLayout = viewInflated.findViewById(R.id.dialog_add_entry_name_input_layout);
            inputLayout.setError(context.getResources().getString(R.string.edit_pdf_outline_invalid_name));
            return false;
        }
        try {
            if (page.isEmpty()) {
                throw new Exception("Invalid page number");
            }
            int pageNum = Integer.parseInt(page);
            if (pageNum <= 0 || pageNum > mPdfViewCtrl.getPageCount()) {
                throw new Exception("Invalid page number");
            }
        } catch (Exception ex) {
            TextInputLayout inputLayout = viewInflated.findViewById(R.id.dialog_add_entry_page_number_input_layout);
            inputLayout.setError(context.getResources().getString(R.string.edit_pdf_outline_invalid_page));
            return false;
        }
        return true;
    }

    private OnClickListener editClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_EDIT_OUTLINE,
                        AnalyticsParam.editOutlineParam(AnalyticsHandlerAdapter.EVENT_EDIT_OUTLINE_EDIT));
                if (mSelectedNodes.size() != 1) {
                    return;
                }
                String currentName = mSelectedNodes.get(0).getContent().getTitle();
                String currentPage = String.valueOf(mSelectedNodes.get(0).getContent().getPageNumber());
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle(R.string.edit_pdf_outline_edit_entry);
                View viewInflated = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_outline_add_entry, (ViewGroup) getView(), false);
                final EditText bookmarkNameEditText = viewInflated.findViewById(R.id.dialog_add_entry_name);
                final EditText pageNumberEditText = viewInflated.findViewById(R.id.dialog_add_entry_page_number);
                final TextInputLayout pageNumberInputLayout = viewInflated.findViewById(R.id.dialog_add_entry_page_number_input_layout);
                String hint = String.format(v.getContext().getResources().getString(R.string.dialog_gotopage_number), 1, mPdfViewCtrl.getPageCount());
                pageNumberInputLayout.setHint(hint);
                bookmarkNameEditText.setText(currentName);
                bookmarkNameEditText.setSelection(bookmarkNameEditText.getText().length());
                pageNumberEditText.setText(currentPage);
                builder.setView(viewInflated);
                builder.setPositiveButton(getString(R.string.ok), null);
                builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                mEditEntryDialog = builder.create();
                mEditEntryDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        bookmarkNameEditText.requestFocus();
                        Button okButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                        okButton.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                resetAddEntryErrors(viewInflated);
                                String name = bookmarkNameEditText.getText().toString();
                                String page = pageNumberEditText.getText().toString();
                                if (isValidNameAndPageNumber(viewInflated, v.getContext(), name, page)) {
                                    editBookmark(name, page);
                                    dialog.dismiss();
                                }
                            }
                        });
                    }
                });
                mEditEntryDialog.show();
            }
        };
    }

    private void collapseAllBookmarks(List<Bookmark> bookmarks) {
        if (mPdfViewCtrl != null) {
            for (Bookmark bookmark : bookmarks) {
                try {
                    saveOpenState(bookmark, false);
                    if (bookmark.hasChildren()) {
                        List<Bookmark> children = BookmarkManager.getBookmarkList(mPdfViewCtrl.getDoc(), bookmark.getFirstChild());
                        collapseAllBookmarks(children);
                    }
                } catch (PDFNetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void saveOpenState(Bookmark bookmark, Boolean isOpen) {
        if (mPdfViewCtrl != null && mPdfViewCtrl.getDoc() != null && bookmark != null) {
            boolean shouldUnlock = false;
            try {
                mPdfViewCtrl.docLock(true);
                shouldUnlock = true;

                bookmark.setOpen(isOpen);
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    mPdfViewCtrl.docUnlock();
                }
            }
        }
    }

    private Toolbar getToolbar() {
        if (getParentFragment() != null && getParentFragment().getView() != null) {
            return getParentFragment().getView().findViewById(R.id.toolbar);
        }
        return null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_collapse) {
            if (mPdfViewCtrl != null && mPdfViewCtrl.getDoc() != null) {
                if (mSelectedNodes.size() > 0) {
                    clearSelections();
                    setEditOutlineTitle();
                }
                List<Bookmark> bookmarks = BookmarkManager.getBookmarkList(mPdfViewCtrl.getDoc(), null);
                collapseAllBookmarks(bookmarks);
                populateBookmarksTreeView(false);//call this so all previously expanded tree nodes(and children) will be updated with new collapsed state
            }
        }
        if (id == R.id.action_edit) {
            boolean intercept = EventHandler.sendPreEvent(EventHandler.EDIT_OUTLINE_EVENT);
            if (!intercept) {
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_EDIT_OUTLINE,
                        AnalyticsParam.editOutlineParam(AnalyticsHandlerAdapter.EVENT_EDIT_OUTLINE_OPEN));
                if (mSearchMenuItem == null) {
                    Toolbar toolbar = getToolbar();
                    if (toolbar != null) {
                        Menu menu = toolbar.getMenu();
                        mSearchMenuItem = menu.findItem(R.id.action_outline_search);
                    }
                }
                if (mTreeViewAdapter.isEditingOutline()) {
                    mSearchMenuItem.setVisible(true);
                    exitEditMode(false);
                } else {
                    mSearchMenuItem.setVisible(false);
                    enterEditMode();
                    setMenuItemText();
                }
            }
        }
        return true;
    }

    public void enterEditMode() {
        clearSelections();
        mTreeViewAdapter.enableEditOutline();
        editModeTransition();
        mFragmentView.findViewById(R.id.bottom_container).setVisibility(View.VISIBLE);
        updateBottomBarButtonState();

        // also update empty view
        mFragmentView.findViewById(R.id.control_outline_textview_empty).setVisibility(View.GONE);
        setEditOutlineTitle();
    }

    public void exitEditMode(Boolean isOnPause) {
        clearSelections();
        mTreeViewAdapter.disableEditOutline();
        editModeTransition();
        mFragmentView.findViewById(R.id.bottom_container).setVisibility(View.GONE);

        if (!isOnPause) {
            // also update empty view
            mFragmentView.findViewById(R.id.control_outline_textview_empty).setVisibility(isEmpty() ? View.VISIBLE : View.GONE);
            setEditOutlineTitle();
        }
        closeEditDialogs();
        setMenuItemText();
    }

    private void setMenuItemText() {
        Toolbar toolbar = getToolbar();
        if (toolbar != null) {
            Menu menu = toolbar.getMenu();
            if (menu == null) {
                return;
            }
            MenuItem editItem = menu.findItem(R.id.action_edit);
            if (editItem == null) {
                return;
            }

            if (mTreeViewAdapter.isEditingOutline()) {
                editItem.setTitle(getString(R.string.done));
            } else {
                MenuItem collapseItem = menu.findItem(R.id.action_collapse);
                if (isEmpty()) {
                    if (collapseItem != null) {
                        collapseItem.setVisible(false);
                    }
                    editItem.setTitle(mCreateButtonTxt);
                } else {
                    if (collapseItem != null && !mIsSearchMode) {
                        collapseItem.setVisible(true);
                    }
                    editItem.setTitle(mEditButtonTxt);
                }
            }
        }
    }

    private void closeEditDialogs() {
        if (mEditEntryDialog != null && mEditEntryDialog.isShowing()) {
            mEditEntryDialog.dismiss();
        }
        if (mDeleteEntryDialog != null && mDeleteEntryDialog.isShowing()) {
            mDeleteEntryDialog.dismiss();
        }
        if (mAddEntryDialog != null && mAddEntryDialog.isShowing()) {
            mAddEntryDialog.dismiss();
        }
        if (mMoveEntryDialogFragment != null) {
            Dialog dialog = mMoveEntryDialogFragment.getDialog();
            if (dialog != null && dialog.isShowing()) {
                mMoveEntryDialogFragment.dismiss();
            }
        }
    }

    private void clearSelections() {
        for (TreeNode<BookmarkNode> node : mSelectedNodes) {
            BookmarkNode bookmarkNode = node.getContent();
            bookmarkNode.mIsSelected = !bookmarkNode.mIsSelected;
        }
        safeClearSelectedNodes();
        mTreeViewAdapter.notifyDataSetChanged(); // we need to update the drag icon
    }

    private void safeClearSelectedNodes() {
        mSelectedNodes.clear();
        mTreeViewAdapter.setSelectionCount(mSelectedNodes.size());
        updateBottomBarButtonState();
    }

    private void updateBottomBarButtonState() {
        if (mSelectedNodes.isEmpty()) {
            setAddButtonEnabled(true);
            setEditEntryButtonEnabled(false);
            setMoveButtonEnabled(false);
            setDeleteButtonEnabled(false);
        } else if (mSelectedNodes.size() == 1) {
            setAddButtonEnabled(true);
            setEditEntryButtonEnabled(true);
            setDeleteButtonEnabled(true);
            if (mTreeViewAdapter != null && mTreeViewAdapter.getItemCount() > 1) {
                setMoveButtonEnabled(true);
            }
        } else {
            setAddButtonEnabled(false);
            setEditEntryButtonEnabled(false);
            setMoveButtonEnabled(false);
            setDeleteButtonEnabled(true);
        }
    }

    private void setAddButtonEnabled(boolean enabled) {
        ((AppCompatImageView) mFragmentView.findViewById(R.id.btn_add)).setColorFilter(enabled ? mTheme.iconColor : mTheme.disabledIconColor);
        ((TextView) mFragmentView.findViewById(R.id.edit_outline_add_txt)).setTextColor(enabled ? mTheme.textColor : mTheme.disabledIconColor);
        mFragmentView.findViewById(R.id.edit_outline_add).setEnabled(enabled);
    }

    private void setEditEntryButtonEnabled(boolean enabled) {
        ((AppCompatImageView) mFragmentView.findViewById(R.id.btn_edit_entry)).setColorFilter(enabled ? mTheme.iconColor : mTheme.disabledIconColor);
        ((TextView) mFragmentView.findViewById(R.id.edit_outline_edit_entry_txt)).setTextColor(enabled ? mTheme.textColor : mTheme.disabledIconColor);
        mFragmentView.findViewById(R.id.edit_outline_edit_entry).setEnabled(enabled);
    }

    private void setMoveButtonEnabled(boolean enabled) {
        ((AppCompatImageView) mFragmentView.findViewById(R.id.btn_move)).setColorFilter(enabled ? mTheme.iconColor : mTheme.disabledIconColor);
        ((TextView) mFragmentView.findViewById(R.id.edit_outline_move_txt)).setTextColor(enabled ? mTheme.textColor : mTheme.disabledIconColor);
        mFragmentView.findViewById(R.id.edit_outline_move).setEnabled(enabled);
    }

    private void setDeleteButtonEnabled(boolean enabled) {
        ((AppCompatImageView) mFragmentView.findViewById(R.id.btn_delete)).setColorFilter(enabled ? mTheme.iconColor : mTheme.disabledIconColor);
        ((TextView) mFragmentView.findViewById(R.id.edit_outline_delete_txt)).setTextColor(enabled ? mTheme.textColor : mTheme.disabledIconColor);
        mFragmentView.findViewById(R.id.edit_outline_delete).setEnabled(enabled);
    }

    private void editModeTransition() {
        if (mFragmentView instanceof ViewGroup) {
            TransitionSet transitionSet = new TransitionSet();
            Slide bottomSlide = new Slide(Gravity.BOTTOM);
            bottomSlide.addTarget(mFragmentView.findViewById(R.id.bottom_container));
            transitionSet.addTransition(bottomSlide);
            TransitionManager.beginDelayedTransition((ViewGroup) mFragmentView, transitionSet);
        }
    }

    @Override
    public void onExpandNode(TreeNode<BookmarkNode> selectedNode, int position) {
        int positionStart = mTreeViewAdapter.getExpandableStartPosition(selectedNode);
        if (!selectedNode.isExpand()) {
            // update state on bookmark
            selectedNode.getContent().setOpen(true).commitOpen();
            // ui
            OutlineTreeViewAdapter.setBookMarkTreeNode(mPdfViewCtrl, selectedNode, mIsSearchMode);
            mTreeViewAdapter.notifyItemRangeInserted(positionStart, mTreeViewAdapter.addChildNodes(selectedNode, positionStart));
        } else {
            // update state on bookmark
            selectedNode.getContent().setOpen(false).commitOpen();
            deselectChildren(selectedNode);
            if (mSelectedNodes.isEmpty()) {
                mTreeViewAdapter.setSelectionCount(mSelectedNodes.size());
                mTreeViewAdapter.notifyDataSetChanged();
            }
            setEditOutlineTitle();
            // ui
            mTreeViewAdapter.notifyItemRangeRemoved(positionStart, mTreeViewAdapter.removeChildNodes(selectedNode, true));
        }
    }

    private void deselectChildren(TreeNode<BookmarkNode> parent) {
        List<TreeNode<BookmarkNode>> childList = parent.getChildList();
        if (childList == null) {
            return;
        }
        for (int i = 0; i < childList.size(); i++) {
            TreeNode<BookmarkNode> childNode = childList.get(i);
            if (childNode.isExpand()) {
                deselectChildren(childNode);
            }
            if (childNode.getContent().mIsSelected) {
                childNode.getContent().mIsSelected = false;
                mSelectedNodes.remove(childNode);
            }
        }
        updateBottomBarButtonState();
    }

    @Override
    public void onNodeCheckBoxSelected
            (TreeNode<BookmarkNode> treeNode, RecyclerView.ViewHolder viewHolder) {
        onClick(treeNode, viewHolder); //follows same logic
    }

    @Override
    public void onStartDrag(TreeNode<BookmarkNode> treeNode, int position, RecyclerView.
            ViewHolder viewHolder) {
        mModified = true;
        mItemTouchHelper.startDrag(viewHolder);
        mTreeViewAdapter.onItemDrag(treeNode, position);
    }

    @Override
    public boolean onClick(TreeNode<BookmarkNode> node, RecyclerView.ViewHolder holder) {
        if (mTreeViewAdapter.isEditingOutline()) {
            BookmarkNode bookmarkNode = node.getContent();
            if (bookmarkNode != null && holder instanceof BookmarkNodeBinder.ViewHolder) {
                boolean refreshAll = mSelectedNodes.isEmpty(); // from no selection to selection
                if (mSelectedNodes.contains(node)) {
                    mSelectedNodes.remove(node);
                } else {
                    mSelectedNodes.add(node);
                }
                if (mSelectedNodes.isEmpty()) {
                    refreshAll = true; // from selection to no selection
                }
                mTreeViewAdapter.setSelectionCount(mSelectedNodes.size());
                bookmarkNode.mIsSelected = !bookmarkNode.mIsSelected;
                int pos = mTreeViewAdapter.getSelectedPosition(node);
                if (refreshAll) {
                    mTreeViewAdapter.notifyDataSetChanged();
                } else {
                    mTreeViewAdapter.notifyItemChanged(pos);
                }
                updateBottomBarButtonState();
                setEditOutlineTitle();
            }
        } else {
            handleItemClicked(node);
        }
        return true;
    }

    @Override
    public void onToggle(boolean isExpand, RecyclerView.ViewHolder holder) {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mEditingEnabled = args.getBoolean(BUNDLE_EDITING_ENABLED, true);
            mEditButtonTxt = args.getString(BUNDLE_EDIT_BUTTON, getString(R.string.tools_qm_edit));
            mCreateButtonTxt = args.getString(BUNDLE_CREATE_BUTTON, getString(R.string.create));
        }

        if (mCreateButtonTxt == null) {
            mCreateButtonTxt = getString(R.string.create);
        }
        if (mEditButtonTxt == null) {
            mEditButtonTxt = getString(R.string.tools_qm_edit);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        View view = inflater.inflate(R.layout.controls_fragment_outline_dialog, null);
        if (mPdfViewCtrl == null || mPdfViewCtrl.getDoc() == null) {
            return view;
        }

        mTheme = Theme.fromContext(view.getContext());

        mRecyclerView = view.findViewById(R.id.recyclerview_control_outline);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));

        float scale = getResources().getDisplayMetrics().density;
        mTreeViewAdapter = new OutlineTreeViewAdapter(new ArrayList<>(), Collections.singletonList(new BookmarkNodeBinder(this)), mPdfViewCtrl, scale);
        mTreeViewAdapter.setTheme(mTheme);
        mTreeViewAdapter.setOnTreeNodeListener(this);
        mItemTouchHelper = new ItemTouchHelper(new ItemMoveCallback(mTreeViewAdapter));
        mRecyclerView.setAdapter(mTreeViewAdapter);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        Toolbar toolbar = getToolbar();
        if (toolbar != null) {
            MenuItem item = toolbar.getMenu().findItem(R.id.action_edit);
            if (item != null) {
                item.setVisible(isEditingEnabled());
            }
        }
        populateBookmarksTreeView(false);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mDisposables.clear();
    }

    @Override
    public void onResume() {
        super.onResume();
        populateBookmarksTreeView(true);
    }

    private void populateBookmarksTreeView(Boolean onResume) {
        if (mPdfViewCtrl != null && mPdfViewCtrl.getDoc() != null) {
            final ArrayList<Bookmark> bookmarks = new ArrayList<>(BookmarkManager.getBookmarkListByTitle(mPdfViewCtrl.getDoc(), mQueryText, mIsSearchMode));
            mDisposables.add(buildBookmarks(bookmarks)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<List<TreeNode<BookmarkNode>>>() {
                        @Override
                        public void accept(List<TreeNode<BookmarkNode>> treeNodes) throws Exception {
                            mTreeViewAdapter.setItems(treeNodes);
                            if (mCurrentBookmark != null) {
                                TreeNode<BookmarkNode> targetNode = mTreeViewAdapter.findNode(mCurrentBookmark);
                                mRecyclerView.scrollToPosition(mTreeViewAdapter.getSelectedPosition(targetNode));
                            }
                            setMenuItemText();

                            if (onResume) {
                                // also update empty view
                                mFragmentView.findViewById(R.id.control_outline_textview_empty).setVisibility(isEmpty() ? View.VISIBLE : View.GONE);
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            AnalyticsHandlerAdapter.getInstance().sendException(new RuntimeException(throwable));
                        }
                    }));
        }
    }

    private void setEditOutlineTitle() {
        Toolbar toolbar = getToolbar();
        if (toolbar != null) {
            if (mSelectedNodes.isEmpty()) {
                //Edit Outline
                toolbar.setTitle(R.string.edit_pdf_outline);
            } else {
                // X Selected
                int count = mSelectedNodes.size();
                toolbar.setTitle(getString(R.string.edit_pdf_outline_selected, count));
            }
            if (!mTreeViewAdapter.isEditingOutline()) {
                toolbar.setTitle(R.string.bookmark_dialog_fragment_outline_tab_title);
            }
        }
    }

    private Observable<List<TreeNode<BookmarkNode>>> buildBookmarks
            (@NonNull ArrayList<Bookmark> bookmarks) {
        return Observable.create(new ObservableOnSubscribe<List<TreeNode<BookmarkNode>>>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<List<TreeNode<BookmarkNode>>> emitter) throws Exception {
                if (mPdfViewCtrl != null) {
                    List<TreeNode<BookmarkNode>> treeNodes = new ArrayList<>();
                    for (Bookmark bookmark : bookmarks) {
                        if (emitter.isDisposed()) {
                            emitter.onComplete();
                            return;
                        }
                        BookmarkNode bNode = new BookmarkNode(mPdfViewCtrl.getDoc(), bookmark);
                        TreeNode<BookmarkNode> treeNode = new TreeNode<>(bNode);
                        boolean shouldUnlockRead = false;
                        try {
                            mPdfViewCtrl.docLockRead();
                            shouldUnlockRead = true;

                            if (bookmark.hasChildren()) {
                                if (bookmark.isOpen() && !mIsSearchMode) {
                                    List<TreeNode<BookmarkNode>> childNodes = OutlineTreeViewAdapter.buildBookmarkTreeNodeList(mPdfViewCtrl, bookmark, mIsSearchMode);
                                    treeNode.setChildList(childNodes);
                                    treeNode.expand();
                                } else {
                                    OutlineTreeViewAdapter.addPlaceHolderNode(treeNode); //add placeholder for initial load
                                }
                            }
                            treeNodes.add(treeNode);
                        } catch (PDFNetException ignored) {

                        } finally {
                            if (shouldUnlockRead) {
                                mPdfViewCtrl.docUnlockRead();
                            }
                        }
                    }
                    emitter.onNext(treeNodes);
                }
            }
        });
    }

    private void handleItemClicked(TreeNode<BookmarkNode> node) {
        try {
            BookmarkNode bookmarkNode = node.getContent();
            if (bookmarkNode.getBookmark() == null) {
                return;
            }

            onEventAction();
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_NAVIGATE_BY,
                    AnalyticsParam.viewerNavigateByParam(AnalyticsHandlerAdapter.VIEWER_NAVIGATE_BY_OUTLINE));

            Action action = bookmarkNode.getBookmark().getAction();
            if (action != null && action.isValid()) {
                if (mPdfViewCtrl != null) {
                    boolean shouldUnlock = false;
                    boolean shouldUnlockRead = false;
                    boolean hasChanges = false;
                    try {
                        if (action.needsWriteLock()) {
                            mPdfViewCtrl.docLock(true);
                            shouldUnlock = true;
                        } else {
                            mPdfViewCtrl.docLockRead();
                            shouldUnlockRead = true;
                        }
                        ActionParameter action_param = new ActionParameter(action);
                        ActionUtils.getInstance().executeAction(action_param, mPdfViewCtrl);
                        hasChanges = mPdfViewCtrl.getDoc().hasChangesSinceSnapshot();
                    } catch (Exception e) {
                        AnalyticsHandlerAdapter.getInstance().sendException(e);
                    } finally {
                        if (shouldUnlock || shouldUnlockRead) {
                            if (shouldUnlock) {
                                mPdfViewCtrl.docUnlock();
                            }
                            if (shouldUnlockRead) {
                                mPdfViewCtrl.docUnlockRead();
                            }
                            if (hasChanges) {
                                ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                                toolManager.raiseAnnotationActionEvent();
                            }
                        }
                        if (mIsSearchMode) {
                            finishSearchView();
                        }
                    }
                }
                if (mOutlineDialogListener != null) {
                    mOutlineDialogListener.onOutlineClicked(bookmarkNode.getBookmark().getParent(), bookmarkNode.getBookmark());
                }
            }
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            CommonToast.showText(getActivity(), "This bookmark has an invalid action", Toast.LENGTH_SHORT);
        }
    }

    private void editBookmark(String newTitle, String newPage) {
        if (mSelectedNodes.size() != 1) {
            return;
        }

        mModified = true;
        TreeNode<BookmarkNode> node = mSelectedNodes.get(0);
        BookmarkNode selected = node.getContent();
        selected.setTitle(newTitle);
        selected.setPageNumber(Integer.parseInt(newPage));
        selected.commitEditEntry();
        int position = mTreeViewAdapter.getSelectedPosition(node);
        mTreeViewAdapter.notifyItemChanged(position);
    }

    private void addBookmark(BookmarkNode bNode) {
        TreeNode<BookmarkNode> newTreeNode = new TreeNode<>(bNode);
        if (mSelectedNodes.isEmpty()) {
            mTreeViewAdapter.addToRoot(newTreeNode);
            int scrollTo = mTreeViewAdapter.getSelectedPosition(newTreeNode);
            mRecyclerView.scrollToPosition(scrollTo);
        } else if (mSelectedNodes.size() == 1) {
            TreeNode<BookmarkNode> node = mSelectedNodes.get(0);
            mTreeViewAdapter.addChildNode(node, newTreeNode, mIsSearchMode);
            int scrollTo = mTreeViewAdapter.getSelectedPosition(newTreeNode);
            mRecyclerView.scrollToPosition(scrollTo);
        }
    }

    @Override
    public boolean moveBookmarkSelected(Bookmark destination) {
        if (mSelectedNodes.size() != 1) {
            return false;
        }
        TreeNode<BookmarkNode> nodeToMove = mSelectedNodes.get(0);
        TreeNode<BookmarkNode> parentTreeNode = null;
        BookmarkNode parentNode = null;
        if (destination != null) {
            // find dest node from tree
            parentTreeNode = mTreeViewAdapter.findNode(destination);
            if (parentTreeNode != null) {
                parentNode = parentTreeNode.getContent();
            }
        }
        nodeToMove.getContent().commitMoveToNewParent(parentNode);

        // ui
        mTreeViewAdapter.moveChildNode(parentTreeNode, nodeToMove, mIsSearchMode);
        int scrollTo = mTreeViewAdapter.getSelectedPosition(nodeToMove);
        mRecyclerView.scrollToPosition(scrollTo);

        clearSelections();
        setEditOutlineTitle();
        return true;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Activity activity = getActivity();
        if (activity != null) {
            // bottom bar does not scale properly on tablet after a rotation, reapply the view fixes this issue
            if (Utils.isTablet(activity)) {
                ViewGroup viewGroup = (ViewGroup) getView();
                if (viewGroup != null) {
                    boolean isEditingOutline = false;
                    if (mTreeViewAdapter != null) {
                        isEditingOutline = mTreeViewAdapter.isEditingOutline();
                        if (isEditingOutline) {
                            exitEditMode(false);
                        }
                    }
                    viewGroup.removeAllViewsInLayout();
                    View view = onCreateView(activity.getLayoutInflater(), viewGroup, null);
                    if (view != null) {
                        viewGroup.addView(view);
                        bindUi(view);
                        if (mTreeViewAdapter != null && isEditingOutline) {
                            enterEditMode();
                            setMenuItemText();
                        }
                    }
                }
            }
        }
    }

    private void deleteBookmark(View v) {
        int count = mSelectedNodes.size();
        String body = String.format(v.getContext().getResources().getString(R.string.edit_pdf_outline_delete_entry_body), count);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(v.getContext())
                .setTitle(R.string.edit_pdf_outline_delete_entry)
                .setMessage(body)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mModified = true;
                        // sort and loop from back
                        Collections.sort(mSelectedNodes, new Comparator<TreeNode<BookmarkNode>>() {
                            @Override
                            public int compare(TreeNode<BookmarkNode> o1, TreeNode<BookmarkNode> o2) {
                                Integer pos1 = mTreeViewAdapter.getSelectedPosition(o1);
                                Integer pos2 = mTreeViewAdapter.getSelectedPosition(o2);
                                return pos1.compareTo(pos2);
                            }
                        });

                        for (int i = mSelectedNodes.size() - 1; i >= 0; i--) {
                            TreeNode<BookmarkNode> node = mSelectedNodes.get(i);
                            if (node.getParent() != null && mSelectedNodes.contains(node.getParent())) {
                                // parent node will handle deletion of children
                                continue;
                            }
                            BookmarkNode selected = node.getContent();
                            selected.commitDelete();
                            mTreeViewAdapter.removeNode(node);
                        }
                        safeClearSelectedNodes();
                        mTreeViewAdapter.notifyDataSetChanged(); // cannot use remove range because the parent's chevron needs to be updated
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

        mDeleteEntryDialog = dialogBuilder.create();
        mDeleteEntryDialog.show();
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final class Theme {
        @ColorInt
        public final int headerTextColor;
        @ColorInt
        public final int headerBackgroundColor;
        @ColorInt
        public final int textColor;
        @ColorInt
        public final int iconColor;
        @ColorInt
        public final int disabledIconColor;
        @ColorInt
        public final int secondaryTextColor;
        @ColorInt
        public final int selectedBackgroundColor;
        @ColorInt
        public final int backgroundColor;

        Theme(int headerTextColor, int headerBackgroundColor, int textColor, int iconColor, int disabledIconColor, int secondaryTextColor, int selectedBackgroundColor, int backgroundColor) {
            this.headerTextColor = headerTextColor;
            this.headerBackgroundColor = headerBackgroundColor;
            this.textColor = textColor;
            this.iconColor = iconColor;
            this.disabledIconColor = disabledIconColor;
            this.secondaryTextColor = secondaryTextColor;
            this.selectedBackgroundColor = selectedBackgroundColor;
            this.backgroundColor = backgroundColor;
        }

        public static Theme fromContext(@NonNull Context context) {

            final TypedArray a = context.obtainStyledAttributes(
                    null, R.styleable.OutlineDialogTheme, R.attr.pt_outline_dialog_style, R.style.PTOutlineDialogTheme);

            int headerTextColor = a.getColor(R.styleable.OutlineDialogTheme_headerTextColor, context.getResources().getColor(R.color.pt_heading_color));
            int headerBackgroundColor = a.getColor(R.styleable.OutlineDialogTheme_headerBackgroundColor, context.getResources().getColor(R.color.pt_utility_variant_color));
            int textColor = a.getColor(R.styleable.OutlineDialogTheme_textColor, Utils.getPrimaryTextColor(context));
            int iconColor = a.getColor(R.styleable.OutlineDialogTheme_iconColor, context.getResources().getColor(R.color.pt_body_text_color));
            int disabledIconColor = a.getColor(R.styleable.OutlineDialogTheme_disabledIconColor, context.getResources().getColor(R.color.pt_disabled_state_color));
            int secondaryTextColor = a.getColor(R.styleable.OutlineDialogTheme_secondaryTextColor, context.getResources().getColor(R.color.pt_secondary_color));
            int selectedBackgroundColor = a.getColor(R.styleable.OutlineDialogTheme_selectedBackgroundColor, context.getResources().getColor(R.color.pt_utility_variant_color));
            int backgroundColor = a.getColor(R.styleable.OutlineDialogTheme_backgroundColor, context.getResources().getColor(R.color.pt_background_color));
            a.recycle();

            return new Theme(headerTextColor, headerBackgroundColor, textColor, iconColor, disabledIconColor, secondaryTextColor, selectedBackgroundColor, backgroundColor);
        }
    }
}
