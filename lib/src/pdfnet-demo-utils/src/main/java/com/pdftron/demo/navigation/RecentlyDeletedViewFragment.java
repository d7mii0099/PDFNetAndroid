//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2021 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.navigation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.demo.R;
import com.pdftron.demo.browser.db.trash.TrashEntity;
import com.pdftron.demo.browser.ui.FileBrowserTheme;
import com.pdftron.demo.databinding.FragmentRecentlyDeletedViewBinding;
import com.pdftron.demo.dialog.FilePickerDialogFragment;
import com.pdftron.demo.navigation.adapter.TrashAdapter;
import com.pdftron.demo.navigation.callbacks.ExternalFileManagementListener;
import com.pdftron.demo.navigation.callbacks.FileManagementListener;
import com.pdftron.demo.utils.ExternalFileManager;
import com.pdftron.demo.utils.FileManager;
import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.model.ExternalFileInfo;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.RequestCode;
import com.pdftron.pdf.utils.ThemeProvider;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;
import com.pdftron.pdf.widget.recyclerview.ItemSelectionHelper;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerView;
import com.pdftron.pdf.widget.recyclerview.ViewHolderBindListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class RecentlyDeletedViewFragment extends DialogFragment
        implements
        ActionMode.Callback,
        FileManagementListener,
        ExternalFileManagementListener,
        FilePickerDialogFragment.LocalFolderListener,
        FilePickerDialogFragment.ExternalFolderListener {

    private static final String TAG = RecentlyDeletedViewFragment.class.getName();
    protected static final String ARGS_SHOW_ACTION_BAR = "RecentlyDeletedViewFragment_show_action_bar";
    protected SimpleRecyclerView mRecyclerView;
    protected ViewGroup mEmptyContainer;
    protected TextView mTrashDurationTextView;
    protected Toolbar mToolbar;
    protected TrashEntity mSelectedTrash;
    protected String mTrashMovedPath;
    protected List<TrashEntity> mTrashList;
    protected TrashAdapter mAdapter;
    protected FragmentRecentlyDeletedViewBinding mBinding;
    protected ItemSelectionHelper mItemSelectionHelper;
    protected ActionMode mActionMode;
    protected FileBrowserTheme mTheme;
    private MenuItem itemDelete;
    private MenuItem itemRestore;
    private MenuItem itemMove;
    protected AppCompatDelegate mDelegate;

    public static RecentlyDeletedViewFragment newInstance() {
        return new RecentlyDeletedViewFragment();
    }

    public static RecentlyDeletedViewFragment newInstance(boolean showActionBar) {
        RecentlyDeletedViewFragment fragment = new RecentlyDeletedViewFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean(ARGS_SHOW_ACTION_BAR, showActionBar);
        fragment.setArguments(bundle);
        return fragment;
    }

    protected boolean showActionBar() {
        Bundle arguments = getArguments();
        if (arguments != null) {
            return arguments.getBoolean(ARGS_SHOW_ACTION_BAR, true);
        }
        return true;
    }

    public void showDialog(FragmentManager fragmentManager) {
        ThemeProvider themeProvider = new ThemeProvider();
        themeProvider.setTheme(R.style.PTAllDocumentFileBrowserTheme);
        setStyle(DialogFragment.STYLE_NO_TITLE, themeProvider.getTheme());
        if (fragmentManager != null) {
            show(fragmentManager, RecentlyDeletedViewFragment.TAG);
        }
    }

    // Called each time the action mode is shown. Always called after
    // onCreateActionMode, but may be called multiple times if the mode is
    // invalidated.
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }
        if (mSelectedTrash == null) {
            return false;
        }

        if (mSelectedTrash.getIsDirectory()) {
            itemDelete.setVisible(true);
            itemRestore.setVisible(true);
            itemMove.setVisible(false);
        } else {
            itemDelete.setVisible(true);
            itemRestore.setVisible(true);
            itemMove.setVisible(true);
        }

        mode.setTitle(Utils.getLocaleDigits(mSelectedTrash.getOriginalName()));
        // Ensure items are always shown
        itemDelete.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        itemRestore.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        itemMove.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        finishActionMode();
    }

    // Called when the user exits the action mode.
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        Activity activity = getActivity();
        if (activity == null || !Utils.isLollipop()) {
            return;
        }
        Window w = activity.getWindow();
        if (w != null) {
            w.setStatusBarColor(getResources().getColor(android.R.color.transparent));
        }
        mActionMode = null;
        clearSelectedTrash();
    }

    // Called when the action mode is created or startActionMode() was called.
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (createActionMode()) {
            return true;
        }

        mode.getMenuInflater().inflate(R.menu.cab_fragment_trash_operations, menu);
        itemDelete = menu.findItem(R.id.cab_file_delete);
        setMenuIconColorFilter(itemDelete);
        itemRestore = menu.findItem(R.id.cab_file_restore);
        setMenuIconColorFilter(itemRestore);
        itemMove = menu.findItem(R.id.cab_file_move);
        setMenuIconColorFilter(itemMove);
        itemMove.setVisible(true);
        return true;
    }

    private void setMenuIconColorFilter(MenuItem menuItem) {
        Drawable drawable = menuItem.getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, mTheme.iconColor);
        menuItem.setIcon(drawable);
    }

    private boolean createActionMode() {
        // Fix transparent status-bar color from going black
        Activity activity = getActivity();
        if (activity == null || !Utils.isLollipop()) {
            return false;
        }
        Window w = activity.getWindow();
        if (w != null) {
            w.setStatusBarColor(Utils.getPrimaryDarkColor(activity));
        }
        return false;
    }

    // Called when the user selects a contextual menu item.
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        FragmentActivity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return false;
        }
        if (mSelectedTrash == null) {
            return false;
        }
        if (item.getItemId() == R.id.cab_file_delete) {
            deleteTrash(mSelectedTrash);
            return true;
        }
        if (item.getItemId() == R.id.cab_file_restore) {
            restoreTrash(mSelectedTrash);
            return true;
        }
        if (item.getItemId() == R.id.cab_file_move) {
            moveTrash();
            return true;
        }
        return false;
    }

    private void deleteTrash(TrashEntity trashEntity) {
        if (mSelectedTrash.getIsExternal()) {
            ExternalFileManager.deleteTrash(getContext(), trashEntity, false, RecentlyDeletedViewFragment.this);
        } else {
            FileManager.deleteTrash(getContext(), trashEntity, false, RecentlyDeletedViewFragment.this);
        }
    }

    private void restoreTrash(TrashEntity trashEntity) {
        if (mSelectedTrash.getIsExternal()) {
            ExternalFileManager.restoreTrash(getContext(), trashEntity, RecentlyDeletedViewFragment.this);
        } else {
            FileManager.restoreTrash(getContext(), trashEntity, RecentlyDeletedViewFragment.this);
        }
    }

    private void moveTrash() {
        FilePickerDialogFragment dialogFragment = FilePickerDialogFragment.newInstance(RequestCode.MOVE_FILE,
                Environment.getExternalStorageDirectory());
        dialogFragment.setLocalFolderListener(RecentlyDeletedViewFragment.this);
        dialogFragment.setExternalFolderListener(RecentlyDeletedViewFragment.this);
        dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            dialogFragment.show(fragmentManager, "file_picker_dialog_fragment");
        }
    }

    @Override
    public void onLocalFolderSelected(int requestCode, Object customObject, File outputFolder) {
        if (requestCode == RequestCode.MOVE_FILE) {
            mTrashMovedPath = outputFolder.getAbsolutePath();
            if (mSelectedTrash.getIsExternal()) {
                moveExternalTrashFile(outputFolder, null);
            } else {
                moveTrashFile(outputFolder, null);
            }
        }
    }

    @Override
    public void onExternalFolderSelected(int requestCode, Object customObject, ExternalFileInfo outputFolder) {
        if (requestCode == RequestCode.MOVE_FILE) {
            mTrashMovedPath = outputFolder.getAbsolutePath();
            if (mSelectedTrash.getIsExternal()) {
                moveExternalTrashFile(null, outputFolder);
            } else {
                moveTrashFile(null, outputFolder);
            }
        }
    }

    private void moveTrashFile(File outputFolder, ExternalFileInfo externalOutputFolder) {
        Context context = getContext();
        if (context != null) {
            FileInfo trashFileInfo = new FileInfo(mSelectedTrash.getIsDirectory() ?
                    BaseFileInfo.FILE_TYPE_FOLDER : BaseFileInfo.FILE_TYPE_FILE, new File(getTrashPath(mSelectedTrash)));
            if (!trashFileInfo.exists()) {
                CommonToast.showText(context,
                        context.getResources().getString(R.string.move_file_not_found_error_message, mSelectedTrash.getOriginalName()),
                        Toast.LENGTH_SHORT);
            } else if (outputFolder != null) {
                FileManager.move(context, new ArrayList<>(Collections.singletonList(trashFileInfo)),
                        outputFolder, RecentlyDeletedViewFragment.this);
            } else {
                FileManager.move(context, new ArrayList<>(Collections.singletonList(trashFileInfo)),
                        externalOutputFolder, RecentlyDeletedViewFragment.this);
            }
        }
    }

    private void moveExternalTrashFile(File outputFolder, ExternalFileInfo externalOutputFolder) {
        Context context = getContext();
        if (context != null) {
            ExternalFileInfo trashFileInfo = Utils.buildExternalFile(context, Uri.parse(getTrashPath(mSelectedTrash)));
            if (trashFileInfo == null) {
                CommonToast.showText(context,
                        context.getResources().getString(R.string.move_file_not_found_error_message, mSelectedTrash.getOriginalName()),
                        Toast.LENGTH_SHORT);
            } else if (outputFolder != null) {
                ExternalFileManager.move(context, new ArrayList<>(Collections.singletonList(trashFileInfo)),
                        outputFolder, RecentlyDeletedViewFragment.this);
            } else {
                ExternalFileManager.move(context, new ArrayList<>(Collections.singletonList(trashFileInfo)),
                        externalOutputFolder, RecentlyDeletedViewFragment.this);
            }
        }
    }

    @Override
    public void onFileMoved(Map<FileInfo, Boolean> filesMoved, File targetFolder) {
        if (filesMoved.size() > 0) {
            FileManager.moveTrash(getContext(), mSelectedTrash, mTrashMovedPath, RecentlyDeletedViewFragment.this);
        }
    }

    @Override
    public void onFileMoved(Map<FileInfo, Boolean> filesMoved, ExternalFileInfo targetFolder) {
        if (filesMoved.size() > 0) {
            ExternalFileManager.moveTrash(getContext(), mSelectedTrash, mTrashMovedPath, RecentlyDeletedViewFragment.this);
        }
    }

    @Override
    public void onExternalFileMoved(Map<ExternalFileInfo, Boolean> filesMoved, ExternalFileInfo targetFolder) {
        if (filesMoved.size() > 0) {
            ExternalFileManager.moveTrash(getContext(), mSelectedTrash, mTrashMovedPath, RecentlyDeletedViewFragment.this);
        }
    }

    @Override
    public void onExternalFileMoved(Map<ExternalFileInfo, Boolean> filesMoved, File targetFolder) {
        if (filesMoved.size() > 0) {
            FileManager.moveTrash(getContext(), mSelectedTrash, mTrashMovedPath, RecentlyDeletedViewFragment.this);
        }
    }

    private String getTrashPath(TrashEntity trashEntity) {
        return String.format("%s" + FileManager.TRASH_FILE_FORMAT,
                trashEntity.getTrashParentPath(), trashEntity.getOriginalName());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        mTheme = FileBrowserTheme.fromContext(requireActivity());
    }

    protected void handleOnBackPressed() {
        if (mActionMode != null) {
            // Exit action mode
            finishActionMode();
            //handled = true;
        } else if (getFragmentManager() != null) {
            dismiss();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Creates our custom view for the folder list.
        mBinding = FragmentRecentlyDeletedViewBinding.inflate(inflater, container, false);
        mToolbar = mBinding.fragmentToolbar;
        if (showActionBar()) {
            mToolbar.setVisibility(View.VISIBLE);
            mToolbar.setTitle(R.string.action_trash_bin);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleOnBackPressed();
                }
            });
        } else {
            mToolbar.setVisibility(View.GONE);
        }

        return mBinding.getRoot();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), getTheme()) {
            @Override
            public void onBackPressed() {
                handleOnBackPressed();
            }
        };
        mDelegate = AppCompatDelegate.create(dialog, null);
        return dialog;
    }

    public void bindViews() {
        mRecyclerView = mBinding.recyclerView;
        mEmptyContainer = mBinding.emptyContainer;
        mTrashDurationTextView = mBinding.textTrashDurationMessage.trashDurationMessage;
    }

    @SuppressLint("StringFormatInvalid")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews();
        mTrashDurationTextView.setBackgroundColor(mTheme.headerBackgroundColor);
        mTrashList = new ArrayList<>();
        mItemSelectionHelper = new ItemSelectionHelper();
        mItemSelectionHelper.attachToRecyclerView(mRecyclerView);
        mItemSelectionHelper.setChoiceMode(ItemSelectionHelper.CHOICE_MODE_SINGLE);
        mAdapter = getAdapter(mTrashList, mItemSelectionHelper);
        mRecyclerView.setAdapter(mAdapter);
        FileManager.loadTrashed(getContext(), RecentlyDeletedViewFragment.this);
        ExternalFileManager.loadTrashed(getContext(), RecentlyDeletedViewFragment.this);

        ItemClickHelper itemClickHelper = new ItemClickHelper();
        itemClickHelper.attachToRecyclerView(mRecyclerView);
        itemClickHelper.setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView parent, View view, int position, long id) {
                handleOnItemClick(position);
            }
        });
        itemClickHelper.setOnItemLongClickListener(new ItemClickHelper.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(RecyclerView parent, View view, int position, long id) {
                return handleOnItemClick(position);
            }
        });
    }

    protected TrashAdapter getAdapter(List<TrashEntity> trashList, ViewHolderBindListener bindListener) {
        return new TrashAdapter(trashList, bindListener);
    }

    private boolean handleOnItemClick(int position) {
        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }
        TrashEntity trashEntity = mAdapter.getItem(position);
        if (trashEntity == null) {
            return false;
        }

        if (mActionMode == null) {
            mSelectedTrash = trashEntity;
            mItemSelectionHelper.setItemChecked(position, true);

            if (activity instanceof AppCompatActivity) {
                mActionMode = getActionMode();
            }
            if (mActionMode != null) {
                mActionMode.invalidate();
            }
        } else {
            if (mSelectedTrash == trashEntity) {
                mSelectedTrash = null;
                mItemSelectionHelper.setItemChecked(position, false);
            } else {
                mSelectedTrash = trashEntity;
                mItemSelectionHelper.setItemChecked(position, true);
            }

            if (mSelectedTrash == null) {
                finishActionMode();
            } else {
                mActionMode.invalidate();
            }
        }
        return true;
    }

    @Nullable
    protected ActionMode getActionMode() {
        if (mDelegate != null) {
            return mDelegate.startSupportActionMode(RecentlyDeletedViewFragment.this);
        } else {
            Activity activity = getActivity();
            if (activity == null) {
                return null;
            } else {
                return ((AppCompatActivity) activity).startSupportActionMode(RecentlyDeletedViewFragment.this);
            }
        }
    }

    private void loadTrashes(List<TrashEntity> trashList) {
        if (mTrashList == null) {
            mTrashList = new ArrayList<>();
        }
        mTrashList.addAll(trashList);
        removeOlderThan30DaysTrashes();
        if (mTrashList.size() > 0) {
            mEmptyContainer.setVisibility(View.GONE);
            Collections.sort(mTrashList);
            mAdapter.notifyDataSetChanged();
        }
        refreshEmptyLabel();
    }

    private void removeOlderThan30DaysTrashes() {
        List<TrashEntity> olderThan30DaysTrashes = new ArrayList<>();
        for (TrashEntity trash : mTrashList) {
            if (olderThan30Days(trash.getTrashDate())) {
                if (trash.getIsExternal()) {
                    ExternalFileManager.deleteTrash(getContext(), trash, true, null);
                } else {
                    FileManager.deleteTrash(getContext(), trash, true, null);
                }
                olderThan30DaysTrashes.add(trash);
            }
        }
        mTrashList.removeAll(olderThan30DaysTrashes);
    }

    private boolean olderThan30Days(Date date) {
        long currentMillis = new Date().getTime();
        long millisIn30Days = 30L * 24 * 60 * 60 * 1000;
        return date.getTime() < (currentMillis - millisIn30Days);
    }

    private void refreshEmptyLabel() {
        if (mTrashList.size() > 0) {
            mTrashDurationTextView.setVisibility(View.VISIBLE);
            mEmptyContainer.setVisibility(View.GONE);
        } else {
            mTrashDurationTextView.setVisibility(View.GONE);
            mEmptyContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRecyclerView = null;
        mEmptyContainer = null;
    }

    protected void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
            mActionMode = null;
            clearSelectedTrash();
        }
    }

    private void clearSelectedTrash() {
        if (mItemSelectionHelper != null) {
            mItemSelectionHelper.clearChoices();
        }
        mSelectedTrash = null;
    }

    @Override
    public void onFileRenamed(FileInfo oldFile, FileInfo newFile) {

    }

    @Override
    public void onFolderCreated(FileInfo rootFolder, FileInfo newFolder) {

    }

    @Override
    public void onFileDuplicated(File fileCopy) {

    }

    @Override
    public void onFileDeleted(ArrayList<FileInfo> deletedFiles) {

    }

    @Override
    public void onFileTrashed(ArrayList<FileInfo> deletedFiles) {

    }

    @Override
    public void onTrashRemoved(TrashEntity trashEntity) {
        finishActionMode();
        mTrashList.remove(trashEntity);
        mAdapter.notifyDataSetChanged();
        refreshEmptyLabel();
    }

    @Override
    public void onTrashesLoaded(List<TrashEntity> trashEntityList) {
        loadTrashes(trashEntityList);
    }

    @Override
    public void onFileMerged(ArrayList<FileInfo> mergedFiles, ArrayList<FileInfo> filesToDelete, FileInfo newFile) {

    }

    @Override
    public void onFileChanged(String path, int event) {

    }

    @Override
    public void onFileClicked(FileInfo fileInfo) {

    }

    @Override
    public void onExternalFileRenamed(ExternalFileInfo oldFile, ExternalFileInfo newFile) {

    }

    @Override
    public void onExternalFileDuplicated(ExternalFileInfo fileCopy) {

    }

    @Override
    public void onExternalFileDeleted(ArrayList<ExternalFileInfo> deletedFiles) {

    }

    @Override
    public void onExternalFileTrashed(ArrayList<ExternalFileInfo> deletedFiles) {

    }

    @Override
    public void onExternalTrashRemoved(TrashEntity trashEntity) {
        finishActionMode();
        mTrashList.remove(trashEntity);
        mAdapter.notifyDataSetChanged();
        refreshEmptyLabel();
    }

    @Override
    public void onExternalTrashesLoaded(List<TrashEntity> trashEntityList) {
        loadTrashes(trashEntityList);
    }

    @Override
    public void onRootsRemoved(ArrayList<ExternalFileInfo> deletedFiles) {

    }

    @Override
    public void onExternalFolderCreated(ExternalFileInfo rootFolder, ExternalFileInfo newFolder) {

    }

    @Override
    public void onExternalFileMerged(ArrayList<FileInfo> mergedFiles, ArrayList<FileInfo> filesToDelete, FileInfo newFile) {

    }
}
