//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.navigation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.collection.LruCache;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.material.snackbar.Snackbar;
import com.pdftron.common.PDFNetException;
import com.pdftron.demo.R;
import com.pdftron.demo.asynctask.PopulateFolderTask;
import com.pdftron.demo.browser.db.trash.TrashEntity;
import com.pdftron.demo.browser.ui.FileBrowserTheme;
import com.pdftron.demo.databinding.BreadcrumbBarBinding;
import com.pdftron.demo.databinding.FragmentLocalFolderViewBinding;
import com.pdftron.demo.dialog.FilePickerDialogFragment;
import com.pdftron.demo.dialog.MergeDialogFragment;
import com.pdftron.demo.navigation.adapter.BaseFileAdapter;
import com.pdftron.demo.navigation.adapter.LocalFileAdapter;
import com.pdftron.demo.navigation.callbacks.FileManagementListener;
import com.pdftron.demo.navigation.callbacks.FileUtilCallbacks;
import com.pdftron.demo.navigation.callbacks.JumpNavigationCallbacks;
import com.pdftron.demo.navigation.callbacks.MainActivityListener;
import com.pdftron.demo.navigation.component.html2pdf.Html2PdfComponent;
import com.pdftron.demo.navigation.component.html2pdf.HtmlConversionComponent;
import com.pdftron.demo.navigation.viewmodel.FilterMenuViewModel;
import com.pdftron.demo.utils.AddDocPdfHelper;
import com.pdftron.demo.utils.FileInfoComparator;
import com.pdftron.demo.utils.FileListFilter;
import com.pdftron.demo.utils.FileManager;
import com.pdftron.demo.utils.LifecycleUtils;
import com.pdftron.demo.utils.MiscUtils;
import com.pdftron.demo.utils.RecursiveFileObserver;
import com.pdftron.demo.utils.ThumbnailPathCacheManager;
import com.pdftron.demo.utils.ThumbnailWorker;
import com.pdftron.demo.widget.ImageViewTopCrop;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFDocInfo;
import com.pdftron.pdf.PreviewHandler;
import com.pdftron.pdf.controls.AddPageDialogFragment;
import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.model.ExternalFileInfo;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.BookmarkManager;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.Constants;
import com.pdftron.pdf.utils.Logger;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.PdfViewCtrlTabsManager;
import com.pdftron.pdf.utils.RequestCode;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;
import com.pdftron.pdf.widget.recyclerview.ItemSelectionHelper;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerView;
import com.pdftron.sdf.SDFDoc;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class LocalFolderViewFragment extends FileBrowserViewFragment implements
        FileManagementListener,
        FilePickerDialogFragment.LocalFolderListener,
        FilePickerDialogFragment.ExternalFolderListener,
        BaseFileAdapter.AdapterListener,
        MergeDialogFragment.MergeDialogFragmentListener,
        ActionMode.Callback,
        MainActivityListener,
        PopulateFolderTask.Callback,
        HtmlConversionComponent.HtmlConversionListener {

    private static final String TAG = LocalFolderViewFragment.class.getName();
    private static final Boolean DEBUG = false;
    private static final int CACHED_SD_CARD_FOLDER_LIMIT = 25;

    protected SimpleRecyclerView mRecyclerView;
    protected TextView mEmptyTextView;
    protected ProgressBar mProgressBarView;
    protected HorizontalScrollView mBreadcrumbBarScrollView;
    protected LinearLayout mBreadcrumbBarLayout;
    protected FloatingActionMenu mFabMenu;
    protected boolean mShouldShowFab = true;
    protected ViewGroup mGoToSdCardView;
    protected Button mGoToSdCardButton;
    protected TextView mGoToSdCardDescription;

    protected final LruCache<String, Boolean> mSdCardFolderCache = new LruCache<>(CACHED_SD_CARD_FOLDER_LIMIT);

    protected ArrayList<FileInfo> mFileInfoList = new ArrayList<>();
    protected ArrayList<FileInfo> mFileInfoSelectedList = new ArrayList<>();
    protected FileInfo mSelectedFile;
    protected File mCurrentFolder;

    private File mRootFolder;

    private FileUtilCallbacks mFileUtilCallbacks;
    protected JumpNavigationCallbacks mJumpNavigationCallbacks;

    protected LocalFileAdapter mAdapter;
    protected ItemSelectionHelper mItemSelectionHelper;
    protected int mSpanCount;

    private PopulateFolderTask mPopulateFolderTask;
    private Comparator<FileInfo> mSortMode;
    private boolean mIsSearchMode;
    protected boolean mIsFullSearchDone;
    private boolean mViewerLaunching;
    private Menu mOptionsMenu;
    private MenuItem mSearchMenuItem;
    private FileInfoDrawer mFileInfoDrawer;
    private Snackbar mSnackBar;
    private Uri mOutputFileUri;
    @ColorInt
    protected int mCrumbColorActive;
    protected boolean mInSDCardFolder;
    private RecursiveFileObserver mFileObserver;
    private String mFilterText = "";
    private boolean mDataChanged;
    protected TextView mNotSupportedTextView;

    private FilterMenuViewModel mFilterViewModel;
    private MenuItem mFilterAll;
    private MenuItem mFilterPdf;
    private MenuItem mFilterDocx;
    private MenuItem mFilterImage;
    private MenuItem mFilterTextItem;

    private LocalFolderViewFragmentListener mLocalFolderViewFragmentListener;

    private HtmlConversionComponent mHtmlConversionComponent;

    public interface LocalFolderViewFragmentListener {

        void onLocalFolderShown();

        void onLocalFolderHidden();
    }

    public static LocalFolderViewFragment newInstance() {
        return new LocalFolderViewFragment();
    }

    private MenuItem itemDuplicate;
    private MenuItem itemEdit;
    private MenuItem itemDelete;
    private MenuItem itemMove;
    private MenuItem itemMerge;
    private MenuItem itemFavorite;
    private MenuItem itemShare;

    protected FragmentLocalFolderViewBinding mBinding;
    protected BreadcrumbBarBinding mBreadcrumbBarBinding;
    protected TextView mEmptyTextViewForFilter;

    protected FileBrowserTheme mTheme;

    // Hold state for Fragment to know what the last action taken was
    boolean mLastActionWasSearch = false;

    // Called each time the action mode is shown. Always called after
    // onCreateActionMode, but may be called multiple times if the mode is
    // invalidated.
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }
        if (mFileInfoSelectedList.isEmpty()) {
            return false;
        }

        if (mFileInfoSelectedList.size() > 1) {
            // Multiple selection of files and/or folders
            itemDelete.setVisible(true);
            itemDuplicate.setVisible(false);
            itemEdit.setVisible(false);
            itemMerge.setVisible(true);
            itemFavorite.setVisible(false);
            itemShare.setVisible(true);

            // If only files are selected, allow move action and merge
            itemMove.setVisible(true);
            for (FileInfo file : mFileInfoSelectedList) {
                if (file.getType() != BaseFileInfo.FILE_TYPE_FILE) {
                    itemMove.setVisible(false);
                    itemMerge.setVisible(false);
                    itemShare.setVisible(false);
                    break;
                }
            }
        } else {
            switch (mFileInfoSelectedList.get(0).getType()) {
                case BaseFileInfo.FILE_TYPE_FOLDER:
                    itemEdit.setVisible(true);
                    itemDuplicate.setVisible(false);
                    itemMove.setVisible(false);
                    itemDelete.setVisible(true);
                    itemMerge.setVisible(false);
                    itemFavorite.setVisible(true);
                    itemShare.setVisible(false);
                    break;
                case BaseFileInfo.FILE_TYPE_FILE:
                    itemEdit.setVisible(true);
                    itemDuplicate.setVisible(true);
                    itemMove.setVisible(true);
                    itemDelete.setVisible(true);
                    itemMerge.setVisible(true);
                    itemFavorite.setVisible(true);
                    itemShare.setVisible(true);
                    break;
                default:
                    itemEdit.setVisible(false);
                    itemDuplicate.setVisible(false);
                    itemMove.setVisible(false);
                    itemDelete.setVisible(false);
                    itemMerge.setVisible(false);
                    itemFavorite.setVisible(false);
                    itemShare.setVisible(false);
                    break;
            }
            if (canAddToFavorite(mFileInfoSelectedList.get(0))) {
                itemFavorite.setTitle(activity.getString(R.string.action_add_to_favorites));
            } else {
                itemFavorite.setTitle(activity.getString(R.string.action_remove_from_favorites));
            }
        }
        mode.setTitle(Utils.getLocaleDigits(Integer.toString(mFileInfoSelectedList.size())));
        // Ensure items are always shown
        itemEdit.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        itemDuplicate.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        itemMove.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        itemDelete.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return true;
    }

    // Called when the user exits the action mode.
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        super.onDestroyActionMode(mode);
        mActionMode = null;
        clearFileInfoSelectedList();
    }

    // Called when the action mode is created or startActionMode() was called.
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (super.onCreateActionMode(mode, menu)) {
            return true;
        }

        mode.getMenuInflater().inflate(R.menu.cab_fragment_file_operations, menu);

        itemEdit = menu.findItem(R.id.cab_file_rename);
        itemDuplicate = menu.findItem(R.id.cab_file_copy);
        itemMove = menu.findItem(R.id.cab_file_move);
        itemDelete = menu.findItem(R.id.cab_file_delete);
        itemMerge = menu.findItem(R.id.cab_file_merge);
        itemFavorite = menu.findItem(R.id.cab_file_favorite);
        itemShare = menu.findItem(R.id.cab_file_share);
        return true;
    }

    // Called when the user selects a contextual menu item.
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        FragmentActivity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return false;
        }
        if (mFileInfoSelectedList.isEmpty()) {
            return false;
        }

        if (item.getItemId() == R.id.cab_file_rename) {
            if (mInSDCardFolder && MiscUtils.showSDCardActionErrorDialog(getContext(), mJumpNavigationCallbacks, getString(R.string.controls_misc_rename))) {
                finishActionMode();
                return true;
            }
            FileManager.rename(activity, mFileInfoSelectedList.get(0).getFile(), LocalFolderViewFragment.this);
            return true;
        }
        if (item.getItemId() == R.id.cab_file_copy) {
            if (mInSDCardFolder && MiscUtils.showSDCardActionErrorDialog(getContext(), mJumpNavigationCallbacks, getString(R.string.controls_misc_duplicate))) {
                finishActionMode();
                return true;
            }
            FileManager.duplicate(activity, mFileInfoSelectedList.get(0).getFile(), LocalFolderViewFragment.this);
            return true;
        }
        if (item.getItemId() == R.id.cab_file_move) {
            if (mInSDCardFolder && MiscUtils.showSDCardActionErrorDialog(getContext(), mJumpNavigationCallbacks, getString(R.string.action_file_move))) {
                finishActionMode();
                return true;
            }
            // Creates the dialog in full screen mode
            FilePickerDialogFragment dialogFragment = FilePickerDialogFragment.newInstance(RequestCode.MOVE_FILE_LIST, mCurrentFolder);
            dialogFragment.setLocalFolderListener(LocalFolderViewFragment.this);
            dialogFragment.setExternalFolderListener(LocalFolderViewFragment.this);
            dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null) {
                dialogFragment.show(fragmentManager, "file_picker_dialog_fragment");
            }
            return true;
        }
        if (item.getItemId() == R.id.cab_file_delete) {
            if (mInSDCardFolder && MiscUtils.showSDCardActionErrorDialog(getContext(), mJumpNavigationCallbacks, getString(R.string.delete))) {
                finishActionMode();
                return true;
            }
            FileManager.delete(activity, mFileInfoSelectedList, LocalFolderViewFragment.this);
            return true;
        }
        if (item.getItemId() == R.id.cab_file_merge) {
            if (mInSDCardFolder && MiscUtils.showSDCardActionErrorDialog(getContext(), mJumpNavigationCallbacks, getString(R.string.merge))) {
                finishActionMode();
                return true;
            }
            handleMerge(mFileInfoSelectedList);
            return true;
        }
        if (item.getItemId() == R.id.cab_file_favorite) {
            handleAddToFavorite(mFileInfoSelectedList.get(0));

            finishActionMode();
            // Update favorite file indicators
            Utils.safeNotifyDataSetChanged(mAdapter);
            return true;
        }
        if (item.getItemId() == R.id.cab_file_share) {
            if (mFileInfoSelectedList.size() > 1) {
                if (mOnPdfFileSharedListener != null) {
                    Intent intent = Utils.createShareIntents(activity, mFileInfoSelectedList);
                    mOnPdfFileSharedListener.onPdfFileShared(intent);
                    finishActionMode();
                } else {
                    Utils.sharePdfFiles(activity, mFileInfoSelectedList);
                }
            } else {
                if (mOnPdfFileSharedListener != null) {
                    Intent intent = Utils.createShareIntent(activity, mFileInfoSelectedList.get(0).getFile());
                    mOnPdfFileSharedListener.onPdfFileShared(intent);
                    finishActionMode();
                } else {
                    Utils.sharePdfFile(activity, mFileInfoSelectedList.get(0).getFile());
                }
            }
            return true;
        }

        return false;
    }

    protected void moveCurrentFile() {
        FragmentActivity activity = getActivity();
        if (activity == null || mSelectedFile == null) {
            return;
        }

        if (mInSDCardFolder && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.action_file_move))) {
            hideFileInfoDrawer();
            return;
        }
        FilePickerDialogFragment dialogFragment = FilePickerDialogFragment.newInstance(RequestCode.MOVE_FILE,
                Environment.getExternalStorageDirectory());
        dialogFragment.setLocalFolderListener(LocalFolderViewFragment.this);
        dialogFragment.setExternalFolderListener(LocalFolderViewFragment.this);
        dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            dialogFragment.show(fragmentManager, "file_picker_dialog_fragment");
        }
    }

    protected void handleMerge(ArrayList<FileInfo> files) {
        // Create and show file merge dialog-fragment
        MergeDialogFragment mergeDialog = getMergeDialogFragment(files, AnalyticsHandlerAdapter.SCREEN_FOLDERS);
        mergeDialog.initParams(LocalFolderViewFragment.this);
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            mergeDialog.show(fragmentManager, "merge_dialog");
        }
    }

    protected LocalFileAdapter createAdapter() {
        return new LocalFileAdapter(getActivity(), mFileInfoList, mFileListLock,
                mSpanCount, this, mItemSelectionHelper);
    }

    protected boolean canAddToFavorite(FileInfo file) {
        FragmentActivity activity = getActivity();
        return !(activity == null || activity.isFinishing()) && (!getFavoriteFilesManager().containsFile(activity, file));
    }

    protected void addToFavorite(FileInfo file) {
        FragmentActivity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        getFavoriteFilesManager().addFile(activity, file);
    }

    protected void removeFromFavorite(FileInfo file) {
        FragmentActivity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        getFavoriteFilesManager().removeFile(activity, file);
    }

    protected void handleAddToFavorite(FileInfo file) {
        FragmentActivity activity = getActivity();
        if (canAddToFavorite(file)) {
            addToFavorite(file);
            CommonToast.showText(activity,
                    getString(R.string.file_added_to_favorites, file.getName()),
                    Toast.LENGTH_SHORT);
        } else {
            removeFromFavorite(file);
            CommonToast.showText(activity,
                    getString(R.string.file_removed_from_favorites, file.getName()),
                    Toast.LENGTH_SHORT);
        }
    }

    protected void handleFileUpdated(FileInfo oldFile, FileInfo newFile) {
        FragmentActivity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        getRecentFilesManager().updateFile(activity, oldFile, newFile);
        getFavoriteFilesManager().updateFile(activity, oldFile, newFile);
    }

    protected void handleFilesRemoved(ArrayList<FileInfo> files) {
        FragmentActivity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        getRecentFilesManager().removeFiles(activity, files);
        getFavoriteFilesManager().removeFiles(activity, files);
    }

    private FileInfoDrawer.Callback mFileInfoDrawerCallback = new FileInfoDrawer.Callback() {

        MenuItem itemDuplicate;
        MenuItem itemEdit;
        MenuItem itemDelete;
        MenuItem itemMove;
        MenuItem itemMerge;
        MenuItem itemFavorite;
        MenuItem itemShare;

        int mPageCount;
        String mAuthor;
        String mTitle;
        String mProducer;
        String mCreator;

        ThumbnailWorker mThumbnailWorker;
        WeakReference<ImageViewTopCrop> mImageViewReference;
        ThumbnailWorker.ThumbnailWorkerListener mThumbnailWorkerListener = new ThumbnailWorker.ThumbnailWorkerListener() {
            @Override
            public void onThumbnailReady(int result, int position, String iconPath, String identifier) {
                ImageViewTopCrop imageView = (mImageViewReference != null) ? mImageViewReference.get() : null;
                if (mSelectedFile == null || imageView == null) {
                    return;
                }

                if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_SECURITY_ERROR) {
                    // avoid flashing caused by the callback
                    mSelectedFile.setIsSecured(true);
                    if (mFileInfoDrawer != null) {
                        mFileInfoDrawer.setIsSecured(true);
                    }
                } else {
                    if (mFileInfoDrawer != null) {
                        mFileInfoDrawer.setIsSecured(false);
                    }
                }
                if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_PACKAGE_ERROR) {
                    // avoid flashing caused by the callback
                    mSelectedFile.setIsPackage(true);
                }

                if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_SECURITY_ERROR || result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_PACKAGE_ERROR) {
                    // Thumbnail has been generated before, and a placeholder icon should be used
                    int errorRes = Utils.getResourceDrawable(getContext(), getResources().getString(R.string.thumb_error_res_name));
                    imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    imageView.setImageResource(errorRes);
                } else if (mThumbnailWorker != null) {
                    // adds path to local cache for later access
                    ThumbnailPathCacheManager.getInstance().putThumbnailPath(mSelectedFile.getAbsolutePath(),
                            iconPath, mThumbnailWorker.getMinXSize(), mThumbnailWorker.getMinYSize());

                    imageView.setScaleType(ImageView.ScaleType.MATRIX);
                    mThumbnailWorker.tryLoadImageWithPath(position, mSelectedFile.getAbsolutePath(), iconPath, imageView);
                }
            }
        };

        @Override
        public CharSequence onPrepareTitle(FileInfoDrawer drawer) {
            return (mSelectedFile != null) ? mSelectedFile.getName() : null;
        }

        @Override
        public BaseFileInfo onPrepareFileInfo(FileInfoDrawer drawer) {
            return mSelectedFile;
        }

        @Override
        public void onPrepareHeaderImage(FileInfoDrawer drawer, ImageViewTopCrop imageView) {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }

            if (mImageViewReference == null ||
                    (mImageViewReference.get() != null && !mImageViewReference.get().equals(imageView))) {
                mImageViewReference = new WeakReference<>(imageView);
            }

            if (mSelectedFile == null) {
                return;
            }

            if (mSelectedFile.getType() == BaseFileInfo.FILE_TYPE_FOLDER) {
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER); // Adjust scale type for folder
                imageView.setImageResource(R.drawable.ic_folder_large);
                int folderColorRes = BaseFileAdapter.getFolderIconColor(activity);
                imageView.getDrawable().mutate().setColorFilter(folderColorRes, PorterDuff.Mode.SRC_IN);
            } else {
                // Setup thumbnail worker, if required
                if (mThumbnailWorker == null) {
                    Point dimensions = drawer.getDimensions();
                    mThumbnailWorker = new ThumbnailWorker(activity, dimensions.x, dimensions.y, null);
                    mThumbnailWorker.setListener(mThumbnailWorkerListener);
                }

                drawer.setIsSecured(mSelectedFile.isSecured());

                if (mSelectedFile.isSecured() || mSelectedFile.isPackage()) {
                    int errorRes = Utils.getResourceDrawable(activity, getResources().getString(R.string.thumb_error_res_name));
                    imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    imageView.setImageResource(errorRes);
                } else {
                    imageView.setScaleType(ImageView.ScaleType.MATRIX);
                    mThumbnailWorker.tryLoadImageWithPath(0, mSelectedFile.getAbsolutePath(), null, imageView);
                }
            }
        }

        @Override
        public boolean onPrepareIsSecured(FileInfoDrawer drawer) {
            return mSelectedFile != null && mSelectedFile.isSecured();
        }

        @Override
        public CharSequence onPrepareMainContent(FileInfoDrawer drawer) {
            return getFileInfoTextBody();
        }

        @Override
        public boolean onCreateDrawerMenu(FileInfoDrawer drawer, Menu menu) {
            Activity activity = getActivity();
            if (activity == null) {
                return false;
            }
            activity.getMenuInflater().inflate(R.menu.cab_fragment_file_operations, menu);

            itemEdit = menu.findItem(R.id.cab_file_rename);
            itemDuplicate = menu.findItem(R.id.cab_file_copy);
            itemMove = menu.findItem(R.id.cab_file_move);
            itemDelete = menu.findItem(R.id.cab_file_delete);
            itemMerge = menu.findItem(R.id.cab_file_merge);
            itemFavorite = menu.findItem(R.id.cab_file_favorite);
            itemShare = menu.findItem(R.id.cab_file_share);

            return true;
        }

        @Override
        public boolean onPrepareDrawerMenu(FileInfoDrawer drawer, Menu menu) {
            Activity activity = getActivity();
            if (activity == null || menu == null || mSelectedFile == null) {
                return true;
            }

            switch (mSelectedFile.getType()) {
                case BaseFileInfo.FILE_TYPE_FOLDER:
                    itemEdit.setVisible(true);
                    itemDuplicate.setVisible(false);
                    itemMove.setVisible(false);
                    itemDelete.setVisible(true);
                    itemMerge.setVisible(false);
                    itemFavorite.setVisible(true);
                    itemShare.setVisible(false);
                    break;
                case BaseFileInfo.FILE_TYPE_FILE:
                    itemEdit.setVisible(true);
                    itemDuplicate.setVisible(true);
                    itemMove.setVisible(true);
                    itemDelete.setVisible(true);
                    itemMerge.setVisible(true);
                    itemFavorite.setVisible(true);
                    itemShare.setVisible(true);
                    break;
                default:
                    itemEdit.setVisible(false);
                    itemDuplicate.setVisible(false);
                    itemMove.setVisible(false);
                    itemDelete.setVisible(false);
                    itemMerge.setVisible(false);
                    itemFavorite.setVisible(false);
                    itemShare.setVisible(false);
                    break;
            }

            if (canAddToFavorite(mSelectedFile)) {
                itemFavorite.setTitle(activity.getString(R.string.action_add_to_favorites));
                itemFavorite.setTitleCondensed(activity.getString(R.string.action_favorite));
                itemFavorite.setIcon(R.drawable.ic_star_white_24dp);
            } else {
                itemFavorite.setTitle(activity.getString(R.string.action_remove_from_favorites));
                itemFavorite.setTitleCondensed(activity.getString(R.string.action_unfavorite));
                itemFavorite.setIcon(R.drawable.ic_star_filled_white_24dp);
            }

            return true;
        }

        @Override
        public boolean onDrawerMenuItemClicked(FileInfoDrawer drawer, MenuItem menuItem) {
            FragmentActivity activity = getActivity();
            if (activity == null || mSelectedFile == null) {
                return false;
            }
            if (menuItem.getItemId() == R.id.cab_file_rename) {
                if (mInSDCardFolder && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.controls_misc_rename))) {
                    hideFileInfoDrawer();
                    return true;
                }
                renameFile(activity, mSelectedFile);
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_copy) {
                if (mInSDCardFolder && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.controls_misc_duplicate))) {
                    hideFileInfoDrawer();
                    return true;
                }
                duplicateFile(activity, mSelectedFile);
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_move) {
                moveCurrentFile();
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_delete) {
                if (mInSDCardFolder && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.delete))) {
                    hideFileInfoDrawer();
                    return true;
                }
                deleteFile(activity, mSelectedFile);
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_merge) {
                if (mInSDCardFolder && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.merge))) {
                    hideFileInfoDrawer();
                    return true;
                }
                // Create and show file merge dialog-fragment
                handleMerge(new ArrayList<>(Collections.singletonList(mSelectedFile)));
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_favorite) {
                favoriteFile(mSelectedFile);
                drawer.invalidate();
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_share) {
                shareFile(activity, mSelectedFile);
                return true;
            }
            return false;
        }

        @Override
        public void onActionClicked(FileInfoDrawer drawer) {
            if (getActivity() == null) {
                return;
            }
            finishActionMode();
            onClickAction();
        }

        @Override
        public void onThumbnailClicked(FileInfoDrawer drawer) {
            drawer.invalidate();
            if (mSelectedFile != null && mSelectedFile.getFile().exists()) {
                onFileClicked(mSelectedFile);
            }
            onHideDrawer(drawer);
        }

        @Override
        public void onShowDrawer(FileInfoDrawer drawer) {
            onShowFileInfoDrawer();
        }

        @Override
        public void onHideDrawer(FileInfoDrawer drawer) {
            cancelAllThumbRequests();

            mSelectedFile = null;
            mFileInfoDrawer = null;

            onHideFileInfoDrawer();
        }

        void cancelAllThumbRequests() {
            if (mThumbnailWorker != null) {
                mThumbnailWorker.abortCancelTask();
                mThumbnailWorker.cancelAllThumbRequests();
            }
        }

        private CharSequence getFileInfoTextBody() {
            Activity activity = getActivity();
            if (activity == null || mSelectedFile == null) {
                return null;
            }
            StringBuilder textBodyBuilder = new StringBuilder();
            Resources res = activity.getResources();

            if (mSelectedFile.getType() != BaseFileInfo.FILE_TYPE_FOLDER) {
                try {
                    PDFDoc doc = new PDFDoc(mSelectedFile.getAbsolutePath());
                    doc.initSecurityHandler();
                    loadDocInfo(doc);
                } catch (PDFNetException e) {
                    mAuthor = null;
                    mTitle = null;
                    mProducer = null;
                    mCreator = null;
                    mPageCount = -1;
                }
            } else {
                mTitle = null;
                mAuthor = null;
                mProducer = null;
                mCreator = null;
                mPageCount = -1;
            }

            switch (mSelectedFile.getType()) {
                case BaseFileInfo.FILE_TYPE_FILE:
                    textBodyBuilder.append(res.getString(R.string.file_info_document_title,
                            Utils.isNullOrEmpty(mTitle) ? res.getString(R.string.file_info_document_attr_not_available) : mTitle));
                    textBodyBuilder.append("<br>");

                    textBodyBuilder.append(res.getString(R.string.file_info_document_author,
                            Utils.isNullOrEmpty(mAuthor) ? res.getString(R.string.file_info_document_attr_not_available) : mAuthor));
                    textBodyBuilder.append("<br>");

                    String pageCountStr = "" + mPageCount;
                    textBodyBuilder.append(res.getString(R.string.file_info_document_pages,
                            mPageCount < 0 ? res.getString(R.string.file_info_document_attr_not_available) : Utils.getLocaleDigits(pageCountStr)));
                    textBodyBuilder.append("<br>");

                    // Directory
                    textBodyBuilder.append(res.getString(R.string.file_info_document_path, mSelectedFile.getAbsolutePath()));
                    textBodyBuilder.append("<br>");
                    // Size info
                    textBodyBuilder.append(res.getString(R.string.file_info_document_size, mSelectedFile.getSizeInfo()));
                    textBodyBuilder.append("<br>");
                    // Date modified
                    textBodyBuilder.append(res.getString(R.string.file_info_document_date_modified, mSelectedFile.getModifiedDate()));
                    textBodyBuilder.append("<br>");

                    //Producer
                    textBodyBuilder.append(res.getString(R.string.file_info_document_producer,
                            Utils.isNullOrEmpty(mProducer) ? res.getString(R.string.file_info_document_attr_not_available) : mProducer));
                    textBodyBuilder.append("<br>");

                    //Creator
                    textBodyBuilder.append(res.getString(R.string.file_info_document_creator,
                            Utils.isNullOrEmpty(mCreator) ? res.getString(R.string.file_info_document_attr_not_available) : mCreator));
                    textBodyBuilder.append("<br>");
                    break;
                case BaseFileInfo.FILE_TYPE_FOLDER:
                    if (textBodyBuilder.length() > 0) {
                        textBodyBuilder.append("<br>");
                    }
                    // Directory
                    textBodyBuilder.append(res.getString(R.string.file_info_document_path, mSelectedFile.getAbsolutePath()));
                    textBodyBuilder.append("<br>");
                    // Size info: x files, y folders
                    int[] fileFolderCount = mSelectedFile.getFileCount();
                    String sizeInfo = res.getString(R.string.dialog_folder_info_size, fileFolderCount[0], fileFolderCount[1]);
                    textBodyBuilder.append(res.getString(R.string.file_info_document_folder_contains, sizeInfo));
                    textBodyBuilder.append("<br>");
                    // Date modified
                    textBodyBuilder.append(res.getString(R.string.file_info_document_date_modified, mSelectedFile.getModifiedDate()));
                    break;
                default:
                    break;
            }

            return Html.fromHtml(textBodyBuilder.toString());
        }

        private void loadDocInfo(PDFDoc doc) {
            if (doc == null) {
                return;
            }
            boolean shouldUnlockRead = false;
            try {
                doc.lockRead();
                shouldUnlockRead = true;

                mPageCount = doc.getPageCount();

                PDFDocInfo docInfo = doc.getDocInfo();
                if (docInfo != null) {
                    mAuthor = docInfo.getAuthor();
                    mTitle = docInfo.getTitle();
                    mProducer = docInfo.getProducer();
                    mCreator = docInfo.getCreator();
                }
            } catch (PDFNetException e) {
                mPageCount = -1;
                mAuthor = null;
                mTitle = null;
                mProducer = null;
                mCreator = null;
            } finally {
                if (shouldUnlockRead) {
                    Utils.unlockReadQuietly(doc);
                }
            }
        }
    };

    protected void renameFile(Context context, FileInfo selectedFile) {
        FileManager.rename(context, selectedFile.getFile(), LocalFolderViewFragment.this);
    }

    protected void duplicateFile(Context context, FileInfo selectedFile) {
        FileManager.duplicate(context, selectedFile.getFile(), LocalFolderViewFragment.this);
    }

    protected void shareFile(Activity activity, FileInfo selectedFile) {
        if (mOnPdfFileSharedListener != null) {
            Intent intent = Utils.createShareIntent(activity, selectedFile.getFile());
            mOnPdfFileSharedListener.onPdfFileShared(intent);
        } else {
            Utils.sharePdfFile(activity, selectedFile.getFile());
        }
    }

    protected void deleteFile(Context context, FileInfo selectedFile) {
        FileManager.delete(context, new ArrayList<>(Collections.singletonList(selectedFile)),
                LocalFolderViewFragment.this);
    }

    protected void favoriteFile(FileInfo selectedFile) {
        handleAddToFavorite(selectedFile);
        // Update favorite file indicators
        Utils.safeNotifyDataSetChanged(mAdapter);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mCurrentFolder != null) {
            outState.putSerializable("current_folder", mCurrentFolder);
        }
        if (mOutputFileUri != null) {
            outState.putParcelable("output_file_uri", mOutputFileUri);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeFragment();
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseFragment();
        updateSpanCount(mSpanCount);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        initializeRootFolder(activity);

        FileManager.initCache(getContext());
        // When we use setRetainInstance, the Bundle (savedInstanceState) will always be null.
        setRetainInstance(true);

        // This Fragment wants to be able to have action bar items.
        setHasOptionsMenu(true);

        if (null != savedInstanceState) {
            mCurrentFolder = (File) savedInstanceState.getSerializable("current_folder");
            mOutputFileUri = savedInstanceState.getParcelable("output_file_uri");
        } else {
            mCurrentFolder = mRootFolder;
        }

        if (PdfViewCtrlSettingsManager.getSortMode(activity).equals(PdfViewCtrlSettingsManager.KEY_PREF_SORT_BY_NAME)) {
            mSortMode = FileInfoComparator.folderPathOrder();
        } else {
            mSortMode = FileInfoComparator.folderDateOrder();
        }

        mCrumbColorActive = getResources().getColor(R.color.breadcrumb_color);
        mFilterViewModel = ViewModelProviders.of(this).get(FilterMenuViewModel.class);

        mTheme = FileBrowserTheme.fromContext(requireActivity());
    }

    private void initializeRootFolder(@NonNull Context context) {
        if (shouldUseBackupFolder()) {
            mRootFolder = context.getExternalFilesDir(null);
        } else {
            mRootFolder = Environment.getExternalStorageDirectory();
        }
    }

    protected boolean shouldUseBackupFolder() {
        return Utils.isUsingDocumentTree();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Create the file observer
        updateFileObserver();
        // Creates our custom view for the folder list.
        mBinding = FragmentLocalFolderViewBinding.inflate(inflater, container, false);
        mToolbar = mBinding.fragmentToolbar;
        mBreadcrumbBarBinding = mBinding.breadcrumbBar;
        return mBinding.getRoot();
    }

    public void bindViews() {
        mRecyclerView = mBinding.recyclerView;
        mEmptyTextView = mBinding.emptyTextView;
        mProgressBarView = mBinding.progressBarView;
        mBreadcrumbBarScrollView = mBreadcrumbBarBinding.breadcrumbBarScrollView;
        mBreadcrumbBarLayout = mBreadcrumbBarBinding.breadcrumbBarLayout;
        mFabMenu = mBinding.fabMenu;
        mFabMenu.setVisibility(mShouldShowFab ? View.VISIBLE : View.GONE);
        mGoToSdCardView = mBinding.goToSdCardView;
        mGoToSdCardButton = mBinding.buttonGoToSdCard;
        mGoToSdCardDescription = mBinding.goToSdCardViewText;
        mEmptyTextViewForFilter = mBinding.emptyTextViewForFilter;
    }

    @SuppressLint("StringFormatInvalid")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews();

        mBreadcrumbBarScrollView.setVerticalScrollBarEnabled(false);
        mBreadcrumbBarScrollView.setHorizontalScrollBarEnabled(false);
        mBreadcrumbBarLayout.removeAllViews();
        mFabMenu.setClosedOnTouchOutside(true);
        mEmptyTextViewForFilter.setBackgroundColor(mTheme.emptyTextBackground);

        FloatingActionButton createFolderButton = mFabMenu.findViewById(R.id.add_folder);
        createFolderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity activity = getActivity();
                if (activity == null) {
                    return;
                }
                mFabMenu.close(true);
                FileManager.createFolder(getActivity(), mCurrentFolder, LocalFolderViewFragment.this);
            }
        });

        FloatingActionButton createPDFButton = mFabMenu.findViewById(R.id.blank_PDF);
        createPDFButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFabMenu.close(true);
                AddPageDialogFragment addPageDialogFragment = AddPageDialogFragment.newInstance();
                addPageDialogFragment.setOnCreateNewDocumentListener(new AddPageDialogFragment.OnCreateNewDocumentListener() {
                    @Override
                    public void onCreateNewDocument(PDFDoc doc, String title) {
                        saveCreatedDocument(doc, title);
                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CREATE_NEW,
                                AnalyticsParam.createNewParam(AnalyticsHandlerAdapter.CREATE_NEW_ITEM_BLANK_PDF, AnalyticsHandlerAdapter.SCREEN_FOLDERS));
                    }
                });
                FragmentManager fragmentManager = getFragmentManager();
                if (fragmentManager != null) {
                    addPageDialogFragment.show(fragmentManager, AddPageDialogFragment.ADD_PAGE_DIALOG_TAG);
                }
            }
        });

        FloatingActionButton imagePDFButton = mFabMenu.findViewById(R.id.image_pdf);
        imagePDFButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFabMenu.close(true);
                mOutputFileUri = ViewerUtils.openImageIntent(LocalFolderViewFragment.this);
            }
        });

        FloatingActionButton officePDFButton = mFabMenu.findViewById(R.id.office_PDF);
        officePDFButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentActivity activity = getActivity();
                FragmentManager fragmentManager = getFragmentManager();
                if (activity == null || fragmentManager == null) {
                    return;
                }

                mFabMenu.close(true);
                mAddDocPdfHelper = new AddDocPdfHelper(activity, fragmentManager, new AddDocPdfHelper.AddDocPDFHelperListener() {
                    @Override
                    public void onPDFReturned(String fileAbsolutePath, boolean external) {
                        if (external || getActivity() == null || getActivity().isFinishing())
                            return;
                        if (fileAbsolutePath == null) {
                            Utils.showAlertDialog(getActivity(), R.string.dialog_add_photo_document_filename_error_message, R.string.error);
                            return;
                        }

//                            reloadFileInfoList();
                        if (mCallbacks != null) {
                            mCallbacks.onFileSelected(new File(fileAbsolutePath), "");
                        }
                        CommonToast.showText(getContext(), getString(R.string.dialog_create_new_document_filename_success) + fileAbsolutePath);
                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CREATE_NEW,
                                AnalyticsParam.createNewParam(AnalyticsHandlerAdapter.CREATE_NEW_ITEM_PDF_FROM_DOCS, AnalyticsHandlerAdapter.SCREEN_FOLDERS));
                    }

                    @Override
                    public void onMultipleFilesSelected(int requestCode, ArrayList<FileInfo> fileInfoList) {
                        handleMultipleFilesSelected(fileInfoList, AnalyticsHandlerAdapter.SCREEN_FOLDERS);
                    }
                });
                mAddDocPdfHelper.pickFileAndCreate(mCurrentFolder);
            }
        });

        // Set up fab for HTML 2 PDF conversion using HTML2PDF, requires KitKat
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View btnView = inflater.inflate(R.layout.fab_btn_web_pdf, null);
        FloatingActionButton webpagePDFButton = btnView.findViewById(R.id.webpage_PDF);
        // HTML conversion hould not be visible if Android version is less than KitKat
        if (!Utils.isKitKat()) {
            webpagePDFButton.setVisibility(View.GONE);
        }
        webpagePDFButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFabMenu.close(true);
                convertHtml();
            }
        });
        mFabMenu.addMenuButton(webpagePDFButton);

        mSpanCount = PdfViewCtrlSettingsManager.getGridSize(getActivity(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_FOLDER_FILES);
        mRecyclerView.initView(mSpanCount);

        ItemClickHelper itemClickHelper = new ItemClickHelper();
        itemClickHelper.attachToRecyclerView(mRecyclerView);

        mItemSelectionHelper = new ItemSelectionHelper();
        mItemSelectionHelper.attachToRecyclerView(mRecyclerView);
        mItemSelectionHelper.setChoiceMode(ItemSelectionHelper.CHOICE_MODE_MULTIPLE);

        mAdapter = createAdapter();
        mRecyclerView.setAdapter(mAdapter);

        try {
            mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if (mRecyclerView == null) {
                                return;
                            }
                            try {
                                mRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            } catch (Exception ignored) {
                            }
                            if (mAdapter == null) {
                                return;
                            }
                            int viewWidth = mRecyclerView.getMeasuredWidth();
                            mAdapter.updateMainViewWidth(viewWidth);
                            mAdapter.getDerivedFilter().setFileTypeEnabledInFilterFromSettings(mRecyclerView.getContext(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_FOLDER_FILES);
                            updateFileListFilter();
                        }
                    });
        } catch (Exception ignored) {
        }

        itemClickHelper.setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView parent, View view, int position, long id) {
                FileInfo fileInfo = mAdapter.getItem(position);
                if (fileInfo == null) {
                    return;
                }

                if (mActionMode == null) {
                    // We are not in CAB mode, we don't want to let the item checked
                    // in this case... We are just opening the document, not selecting it.
                    mItemSelectionHelper.setItemChecked(position, false);

                    if (fileInfo.getFile().exists()) {
                        onFileClicked(fileInfo);
                    }
                } else {
                    if (mFileInfoSelectedList.contains(fileInfo)) {
                        mFileInfoSelectedList.remove(fileInfo);
                        mItemSelectionHelper.setItemChecked(position, false);
                    } else {
                        mFileInfoSelectedList.add(fileInfo);
                        mItemSelectionHelper.setItemChecked(position, true);
                    }

                    if (mFileInfoSelectedList.isEmpty()) {
                        finishActionMode();
                    } else {
                        mActionMode.invalidate();
                    }
                }
            }
        });

        itemClickHelper.setOnItemLongClickListener(new ItemClickHelper.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(RecyclerView parent, View view, int position, long id) {
                Activity activity = getActivity();
                if (activity == null) {
                    return false;
                }

                if (!useSupportActionBar()) {
                    // disable action mode in dialog fragment mode
                    return false;
                }

                FileInfo fileInfo = mAdapter.getItem(position);
                if (fileInfo == null) {
                    return false;
                }

                if (mActionMode == null) {
                    if (inSearchMode()) {
                        clearSearchFocus();
                    }
                    mFileInfoSelectedList.add(fileInfo);
                    mItemSelectionHelper.setItemChecked(position, true);

                    if (activity instanceof AppCompatActivity) {
                        mActionMode = ((AppCompatActivity) activity).startSupportActionMode(LocalFolderViewFragment.this);
                    }
                    if (mActionMode != null) {
                        mActionMode.invalidate();
                    }
                } else {
                    if (mFileInfoSelectedList.contains(fileInfo)) {
                        mFileInfoSelectedList.remove(fileInfo);
                        mItemSelectionHelper.setItemChecked(position, false);
                    } else {
                        mFileInfoSelectedList.add(fileInfo);
                        mItemSelectionHelper.setItemChecked(position, true);
                    }

                    if (mFileInfoSelectedList.isEmpty()) {
                        finishActionMode();
                    } else {
                        mActionMode.invalidate();
                    }
                }

                return true;
            }
        });

//        mScroller.setRecyclerView(mRecyclerView);

        mGoToSdCardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // go to external tab
                if (null != mJumpNavigationCallbacks) {
                    finishActionMode();
                    mJumpNavigationCallbacks.gotoExternalTab();
                }
            }
        });
        String message = String.format(getString(R.string.dialog_files_go_to_sd_card_description),
                getString(R.string.app_name),
                String.format(getString(R.string.dialog_go_to_sd_card_description_more_info), getString(R.string.misc_read_more)));
        mGoToSdCardDescription.setText(Html.fromHtml(message));
        mGoToSdCardDescription.setMovementMethod(LinkMovementMethod.getInstance());

        mNotSupportedTextView = view.findViewById(R.id.num_no_supported_files);
    }

    @Override
    public void onStart() {
        super.onStart();
        mHtmlConversionComponent = getHtmlConversionComponent(getView());
        AnalyticsHandlerAdapter.getInstance().sendTimedEvent(AnalyticsHandlerAdapter.EVENT_SCREEN_FOLDERS);
    }

    @Override
    public void onStop() {
        super.onStop();
        AnalyticsHandlerAdapter.getInstance().endTimedEvent(AnalyticsHandlerAdapter.EVENT_SCREEN_FOLDERS);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRecyclerView = null;
        mEmptyTextView = null;
        mEmptyTextViewForFilter = null;
        mProgressBarView = null;
        mBreadcrumbBarScrollView = null;
        mBreadcrumbBarLayout = null;
        mFabMenu = null;
        mGoToSdCardView = null;
        mGoToSdCardButton = null;
        mGoToSdCardDescription = null;
    }

    @Override
    public void onDestroy() {
        // cleanup previous resource
        if (null != mAdapter) {
            mAdapter.cancelAllThumbRequests(true);
            mAdapter.cleanupResources();
        }

        super.onDestroy();
    }

    public void onLowMemory() {
        super.onLowMemory();
        MiscUtils.handleLowMemory(getContext(), mAdapter);
        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_LOW_MEMORY, AnalyticsParam.lowMemoryParam(TAG));
        Logger.INSTANCE.LogE(TAG, "low memory");
    }

    // Let's make sure that the parent activity implements all necessary interfaces.
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mFileUtilCallbacks = (FileUtilCallbacks) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + e.getClass().toString());
        }

        try {
            mJumpNavigationCallbacks = (JumpNavigationCallbacks) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + e.getClass().toString());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFileUtilCallbacks = null;
        mJumpNavigationCallbacks = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isAdded()) {
            return;
        }

        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_folder_search_view, menu);
        inflater.inflate(R.menu.fragment_folder_view, menu);
        inflater.inflate(R.menu.menu_addon_file_type_filter, menu);
        inflater.inflate(R.menu.menu_addon_recently_deleted_files, menu);

        bindFilterViewModel(menu);
    }

    public void bindFilterViewModel(Menu menu) {
        mOptionsMenu = menu;

        mSearchMenuItem = menu.findItem(R.id.menu_action_search);
        if (mSearchMenuItem != null) {
            SearchView searchView = (SearchView) mSearchMenuItem.getActionView();
            searchView.setQueryHint(getString(R.string.action_file_filter));
            searchView.setOnQueryTextListener(this);
            searchView.setSubmitButtonEnabled(false);

            if (!Utils.isNullOrEmpty(mFilterText)) {
                mSearchMenuItem.expandActionView();
                searchView.setQuery(mFilterText, true);
                mFilterText = "";
            }

            // Disable long-click context menu
            EditText editText = searchView.findViewById(R.id.search_src_text);
            if (editText != null) {
                editText.setCustomSelectionActionModeCallback(new android.view.ActionMode.Callback() {
                    @Override
                    public boolean onCreateActionMode(android.view.ActionMode mode, Menu menu) {
                        return false;
                    }

                    @Override
                    public boolean onPrepareActionMode(android.view.ActionMode mode, Menu menu) {
                        return false;
                    }

                    @Override
                    public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {
                        return false;
                    }

                    @Override
                    public void onDestroyActionMode(android.view.ActionMode mode) {

                    }
                });
            }

            final MenuItem reloadMenuItem = menu.findItem(R.id.menu_action_reload);
            final MenuItem listToggleMenuItem = menu.findItem(R.id.menu_grid_toggle);
            // We need to override this method to get the collapse event, so we can
            // clear the filter.
            mSearchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {

                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    // Let's return true to expand the view.
                    reloadMenuItem.setVisible(false);
                    listToggleMenuItem.setVisible(false);
                    mIsSearchMode = true;
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    reloadMenuItem.setVisible(true);
                    listToggleMenuItem.setVisible(true);
                    resetFileListFilter();
                    mIsSearchMode = false;
                    return true;
                }
            });
        }

        // Clear the submenu header for Filter menu item
        MenuItem fileMenu = menu.findItem(R.id.menu_file_filter);
        Context context = getContext();
        if (fileMenu != null && context != null) {
            fileMenu.getSubMenu().clearHeader();
            mFilterAll = menu.findItem(R.id.menu_file_filter_all);
            mFilterPdf = menu.findItem(R.id.menu_file_filter_pdf);
            mFilterDocx = menu.findItem(R.id.menu_file_filter_docx);
            mFilterImage = menu.findItem(R.id.menu_file_filter_image);
            mFilterTextItem = menu.findItem(R.id.menu_file_filter_text);
            ViewerUtils.keepOnScreenAfterClick(context, mFilterAll);
            ViewerUtils.keepOnScreenAfterClick(context, mFilterPdf);
            ViewerUtils.keepOnScreenAfterClick(context, mFilterDocx);
            ViewerUtils.keepOnScreenAfterClick(context, mFilterImage);
            ViewerUtils.keepOnScreenAfterClick(context, mFilterTextItem);

            // Set up file filter menu view model
            mFilterViewModel.initialize(PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_FOLDER_FILES,
                    new FilterMenuViewModel.OnFilterTypeChangeListener() {
                        @Override
                        public void setChecked(int fileType, boolean isChecked) {
                            switch (fileType) {
                                case Constants.FILE_TYPE_PDF:
                                    mFilterPdf.setChecked(isChecked);
                                    break;
                                case Constants.FILE_TYPE_DOC:
                                    mFilterDocx.setChecked(isChecked);
                                    break;
                                case Constants.FILE_TYPE_IMAGE:
                                    mFilterImage.setChecked(isChecked);
                                    break;
                                case Constants.FILE_TYPE_TEXT:
                                    mFilterTextItem.setChecked(isChecked);
                                    break;
                                default:
                            }
                        }

                        @Override
                        public void setAllChecked(boolean isChecked) {
                            mFilterAll.setChecked(isChecked);
                        }

                        @Override
                        public void updateFilter(int fileType, boolean isEnabled) {
                            mLastActionWasSearch = false;
                            mAdapter.getDerivedFilter().setFileTypeEnabledInFilter(fileType, isEnabled);
                            updateFileListFilter();
                        }
                    });
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        Context context = getContext();
        if (menu == null || context == null) {
            return;
        }

        MenuItem menuItem;
        if (PdfViewCtrlSettingsManager.getSortMode(context).equals(PdfViewCtrlSettingsManager.KEY_PREF_SORT_BY_NAME)) {
            mSortMode = FileInfoComparator.folderPathOrder();
            menuItem = menu.findItem(R.id.menu_file_sort_by_name);
        } else {
            mSortMode = FileInfoComparator.folderDateOrder();
            menuItem = menu.findItem(R.id.menu_file_sort_by_date);
        }
        if (menuItem != null) {
            menuItem.setChecked(true);
        }

        // Set grid size radio buttons to correct value from settings
        int gridSize = PdfViewCtrlSettingsManager.getGridSize(getContext(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_FOLDER_FILES);
        if (gridSize == 1) {
            menuItem = menu.findItem(R.id.menu_grid_count_1);
        } else if (gridSize == 2) {
            menuItem = menu.findItem(R.id.menu_grid_count_2);
        } else if (gridSize == 3) {
            menuItem = menu.findItem(R.id.menu_grid_count_3);
        } else if (gridSize == 4) {
            menuItem = menu.findItem(R.id.menu_grid_count_4);
        } else if (gridSize == 5) {
            menuItem = menu.findItem(R.id.menu_grid_count_5);
        } else if (gridSize == 6) {
            menuItem = menu.findItem(R.id.menu_grid_count_6);
        } else {
            menuItem = menu.findItem(R.id.menu_grid_count_0);
        }
        if (menuItem != null) {
            menuItem.setChecked(true);
        }

        updateGridMenuState(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Context context = getContext();
        if (context == null) {
            return false;
        }

        boolean handled = false;
        if (item.getItemId() == R.id.menu_action_search) {
            finishSearchView();
            handled = true;
        }
        if (item.getItemId() == R.id.menu_action_reload) {
            ThumbnailPathCacheManager.getInstance().cleanupResources(getContext());
            reloadFileInfoList(true);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_file_sort_by_name) {
            // Update sort mode setting
            mSortMode = FileInfoComparator.folderPathOrder();
            PdfViewCtrlSettingsManager.updateSortMode(context, PdfViewCtrlSettingsManager.KEY_PREF_SORT_BY_NAME);
            item.setChecked(true);
            reloadFileInfoList(false);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_file_sort_by_date) {
            // Update sort mode setting
            mSortMode = FileInfoComparator.folderDateOrder();
            PdfViewCtrlSettingsManager.updateSortMode(context, PdfViewCtrlSettingsManager.KEY_PREF_SORT_BY_DATE);
            item.setChecked(true);
            reloadFileInfoList(false);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_action_recently_deleted) {
            showTrashBin();
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_0) {
            item.setChecked(true);
            updateSpanCount(0);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_1) {
            item.setChecked(true);
            updateSpanCount(1);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_2) {
            item.setChecked(true);
            updateSpanCount(2);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_3) {
            item.setChecked(true);
            updateSpanCount(3);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_4) {
            item.setChecked(true);
            updateSpanCount(4);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_5) {
            item.setChecked(true);
            updateSpanCount(5);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_6) {
            item.setChecked(true);
            updateSpanCount(6);
            handled = true;
        }
        // Check "all" and uncheck other filters
        if (item.getItemId() == R.id.menu_file_filter_all) {
            mFilterViewModel.clearFileFilters();
        }
        // Uncheck "all" filter on click, and check pdf filter
        if (item.getItemId() == R.id.menu_file_filter_pdf) {
            mFilterViewModel.toggleFileFilter(Constants.FILE_TYPE_PDF);
        }
        // Uncheck "all" filter on click, and check docx filter
        if (item.getItemId() == R.id.menu_file_filter_docx) {
            mFilterViewModel.toggleFileFilter(Constants.FILE_TYPE_DOC);
        }
        // Uncheck "all" filter on click, and check image filter
        if (item.getItemId() == R.id.menu_file_filter_image) {
            mFilterViewModel.toggleFileFilter(Constants.FILE_TYPE_IMAGE);
        }
        // Uncheck "all" filter on click, and check text filter
        if (item.getItemId() == R.id.menu_file_filter_text) {
            mFilterViewModel.toggleFileFilter(Constants.FILE_TYPE_TEXT);
        }

        return handled;
    }

    @Override
    public void onMergeConfirmed(ArrayList<FileInfo> filesToMerge, ArrayList<FileInfo> filesToDelete, String title) {
        boolean hasExtension = FilenameUtils.isExtension(title, "pdf");
        if (!hasExtension) {
            title = title + ".pdf";
        }
        File documentFile = new File(mCurrentFolder, title);
        String filePath = Utils.getFileNameNotInUse(documentFile.getAbsolutePath());
        if (Utils.isNullOrEmpty(filePath)) {
            CommonToast.showText(getActivity(), R.string.dialog_merge_error_message_general, Toast.LENGTH_SHORT);
            return;
        }
        documentFile = new File(filePath);

        performMerge(filesToMerge, filesToDelete, documentFile);
    }

    protected void performMerge(ArrayList<FileInfo> filesToMerge, ArrayList<FileInfo> filesToDelete, File documentFile) {
        FileManager.merge(getActivity(), filesToMerge, filesToDelete, new FileInfo(BaseFileInfo.FILE_TYPE_FILE, documentFile), LocalFolderViewFragment.this);
    }

    private void updateGridMenuState(Menu menu) {
        if (menu == null) {
            return;
        }
        // Set grid/list icon & text based on current mode
        MenuItem menuItem = menu.findItem(R.id.menu_grid_toggle);
        if (menuItem == null) {
            return;
        }
        MenuItem menuItem1 = menu.findItem(R.id.menu_grid_count_1);
        menuItem1.setTitle(getString(R.string.columns_count, 1));
        MenuItem menuItem2 = menu.findItem(R.id.menu_grid_count_2);
        menuItem2.setTitle(getString(R.string.columns_count, 2));
        MenuItem menuItem3 = menu.findItem(R.id.menu_grid_count_3);
        menuItem3.setTitle(getString(R.string.columns_count, 3));
        MenuItem menuItem4 = menu.findItem(R.id.menu_grid_count_4);
        menuItem4.setTitle(getString(R.string.columns_count, 4));
        MenuItem menuItem5 = menu.findItem(R.id.menu_grid_count_5);
        menuItem5.setTitle(getString(R.string.columns_count, 5));
        MenuItem menuItem6 = menu.findItem(R.id.menu_grid_count_6);
        menuItem6.setTitle(getString(R.string.columns_count, 6));
        if (mSpanCount > 0) {
            // In grid mode
            menuItem.setTitle(R.string.dialog_add_page_grid);
            menuItem.setIcon(R.drawable.ic_view_module_white_24dp);
        } else {
            // In list mode
            menuItem.setTitle(R.string.action_list_view);
            menuItem.setIcon(R.drawable.ic_view_list_white_24dp);
        }
    }

    private void setReloadActionButtonState(boolean reloading) {
        if (mOptionsMenu != null) {
            MenuItem reloadItem = mOptionsMenu.findItem(R.id.menu_action_reload);
            if (reloadItem != null) {
                if (reloading) {
                    reloadItem.setActionView(R.layout.actionbar_indeterminate_progress);
                } else {
                    reloadItem.setActionView(null);
                }
            }
        }
    }

    public void updateSpanCount(int count) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        if (mSpanCount != count) {
            PdfViewCtrlSettingsManager.updateGridSize(context, PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_FOLDER_FILES, count);
        }
        mSpanCount = count;
        updateGridMenuState(mOptionsMenu);
        mRecyclerView.updateSpanCount(count);
    }

    public void resetFileListFilter() {
        String filterText = getFilterText();
        if (!Utils.isNullOrEmpty(filterText) && mAdapter != null) {
            mAdapter.getFilter().filter("");
            mAdapter.setInSearchMode(false);
        }
    }

    public String getFilterText() {
        if (!Utils.isNullOrEmpty(mFilterText)) {
            return mFilterText;
        }

        String filterText = "";
        if (mSearchMenuItem != null) {
            SearchView searchView = (SearchView) mSearchMenuItem.getActionView();
            filterText = searchView.getQuery().toString();
        }
        return filterText;
    }

    protected void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
            mActionMode = null;
            clearFileInfoSelectedList();
        }
        closeSearch();
    }

    private void clearFileInfoSelectedList() {
        if (mItemSelectionHelper != null) {
            mItemSelectionHelper.clearChoices();
        }
        mFileInfoSelectedList.clear();
    }

    @Override
    public void hideFileInfoDrawer() {
        if (mFileInfoDrawer != null) {
            mFileInfoDrawer.hide();
            mFileInfoDrawer = null;
        }
        mSelectedFile = null;
    }

    protected void onShowFileInfoDrawer() {

    }

    protected void onHideFileInfoDrawer() {

    }

    protected void onClickAction() {
        hideFileInfoDrawer();
    }

    private void finishSearchView() {
        if (mSearchMenuItem != null && mSearchMenuItem.isActionViewExpanded()) {
            mSearchMenuItem.collapseActionView();
        }
        resetFileListFilter();
    }

    private void updateFileListFilter() {
        if (mAdapter == null) {
            return;
        }

        String constraint = getFilterText();
        if (constraint == null) {
            constraint = "";
        }
        mAdapter.getFilter().filter(constraint);
        boolean isEmpty = Utils.isNullOrEmpty(constraint);
        mAdapter.setInSearchMode(!isEmpty);
    }

    private void rebuildBreadcrumbBar(Context context, File leafFile) {
        mBreadcrumbBarLayout.removeAllViews();

        File parent;
        if (leafFile != null) { // Start generating crumbs at leafFile, if provided
            parent = leafFile;
        } else {
            parent = mCurrentFolder;
        }
        if (Utils.isLollipop()) {
            while (parent != null && !(shouldUseBackupFolder() && parent.getAbsolutePath().equals(mRootFolder.getParentFile().getAbsolutePath()))) {
                createBreadcrumb(context, parent, 0); // Add to start
                if (parent.getParentFile() != null && !parent.getParent().equalsIgnoreCase("/")) {
                    parent = parent.getParentFile();
                } else {
                    break;
                }
            }
        } else {
            while (parent != null) {
                createBreadcrumb(context, parent, 0); // Add to start
                if (parent.getParentFile() != null && !parent.getParent().equalsIgnoreCase("/")) {
                    parent = parent.getParentFile();
                } else {
                    break;
                }
            }
        }
    }

    private int findBreadcrumb(File folder) {
        int position = -1;
        if (folder != null) {
            for (int i = 0; i < mBreadcrumbBarLayout.getChildCount(); i++) {
                LinearLayout crumb = (LinearLayout) mBreadcrumbBarLayout.getChildAt(i);
                Object tag = crumb.getTag();
                if (tag != null && tag instanceof File) {
                    File file = (File) tag;
                    if (file.getAbsolutePath().equals(folder.getAbsolutePath())) {
                        position = i;
                        break;
                    }
                }
            }
        }
        return position;
    }

    // Create and append a new breadcrumb to the bar
    // NOTE: this should be called BEFORE the current folder is changed
    private void appendBreadcrumb(Context context, File newFolder) {
        int currentCrumb = -1;
        if (mBreadcrumbBarLayout.getChildCount() > 0) {
            // Find the current folder's crumb in the bar
            if (mCurrentFolder != null) {
                currentCrumb = findBreadcrumb(mCurrentFolder);
            }
            // Check if the next crumb (right) corresponds to the new folder
            if (currentCrumb >= 0) {
                if (currentCrumb + 1 < mBreadcrumbBarLayout.getChildCount()) {
                    boolean clearToRight = true;
                    LinearLayout crumb = (LinearLayout) mBreadcrumbBarLayout.getChildAt(currentCrumb + 1);
                    Object tag = crumb.getTag();
                    if (tag != null && tag instanceof File) {
                        File file = (File) tag;
                        if (file.getAbsolutePath().equals(newFolder.getAbsolutePath())) {
                            // New folder is already in breadcrumb bar
                            clearToRight = false;
                        }
                    }
                    if (clearToRight) {
                        // let's rebuild bread crumb bar from scratch, since it can be a
                        // non-immediate child
                        rebuildBreadcrumbBar(getContext(), newFolder);
                        // New subtree - update local folder tree setting
                        if (shouldUseBackupFolder()) {
                            PdfViewCtrlSettingsManager.updateBackupCacheFolderTree(context, newFolder.getAbsolutePath());
                        } else {
                            PdfViewCtrlSettingsManager.updateLocalFolderTree(context, newFolder.getAbsolutePath());
                        }
                    }
                } else {
                    // let's rebuild bread crumb bar from scratch, since it can be a
                    // non-immediate child
                    rebuildBreadcrumbBar(getContext(), newFolder);
                    // Traversing down tree - update local folder tree setting
                    if (shouldUseBackupFolder()) {
                        PdfViewCtrlSettingsManager.updateBackupCacheFolderTree(context, newFolder.getAbsolutePath());
                    } else {
                        PdfViewCtrlSettingsManager.updateLocalFolderTree(context, newFolder.getAbsolutePath());
                    }
                }
                setCurrentBreadcrumb(currentCrumb + 1, true);
            }
        }
        if (currentCrumb < 0) {
            // Current crumb could not be found or bar is not built, try (re)building the bar
            rebuildBreadcrumbBar(context, null);
            // Create a new crumb and add to end of bar
            createBreadcrumb(context, newFolder, -1);
            // Update local folder tree setting
            if (shouldUseBackupFolder()) {
                PdfViewCtrlSettingsManager.updateBackupCacheFolderTree(context, newFolder.getAbsolutePath());
            } else {
                PdfViewCtrlSettingsManager.updateLocalFolderTree(context, newFolder.getAbsolutePath());
            }

            setCurrentBreadcrumb(-1, true);
        }
    }

    private void createBreadcrumb(Context context, File folder, int position) {
        @SuppressLint("InflateParams") final LinearLayout crumb =
                (LinearLayout) LayoutInflater.from(context).inflate(R.layout.breadcrumb, null);
        TextView dirTextView = crumb.findViewById(R.id.crumbText);
        if (!Utils.isLollipop() && folder.getParentFile() == null) { // Show "ROOT" instead of "/"
            dirTextView.setText(getString(R.string.root_folder).toUpperCase());
        } else {
            dirTextView.setText(folder.getName().toUpperCase());
        }
        crumb.setTag(folder);
        crumb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Object tag = crumb.getTag();
                if (tag != null && tag instanceof File) {
                    File file = (File) tag;
                    if (mCurrentFolder != null &&
                            !FilenameUtils.equals(file.getAbsolutePath(), mCurrentFolder.getAbsolutePath())) {
                        mCurrentFolder = file;
                        updateFileObserver();

                        finishSearchView();
                        finishActionMode();
                        reloadFileInfoList(false);
                    }
                }
            }
        });

        mBreadcrumbBarLayout.addView(crumb, position);
    }

    // Adjust crumb text and chevron colors to indicate current crumb
    private void setCurrentBreadcrumb(int position, boolean focusCurrent) {
        if (position < 0 || position >= mBreadcrumbBarLayout.getChildCount()) {
            position = mBreadcrumbBarLayout.getChildCount() - 1;
        }
        boolean showChevron = false;
        for (int i = mBreadcrumbBarLayout.getChildCount() - 1; i >= 0; i--) {
            LinearLayout crumb = (LinearLayout) mBreadcrumbBarLayout.getChildAt(i);
            TextView dirTextView = crumb.findViewById(R.id.crumbText);
            ImageView chevronImageView = crumb.findViewById(R.id.crumbChevron);
            Utils.applySecondaryTextTintToButton(chevronImageView);
            if (Utils.isRtlLayout(getContext())) {
                chevronImageView.setScaleX(-1f);
            }
            boolean active = i == position;
            dirTextView.setTextColor(mCrumbColorActive);
            dirTextView.setAlpha(active ? 1f : 0.54f);
            if (showChevron) {
                chevronImageView.setColorFilter(mCrumbColorActive, PorterDuff.Mode.SRC_IN);
                chevronImageView.setAlpha(active ? 1f : 0.54f);
                chevronImageView.setVisibility(View.VISIBLE);
            } else {
                chevronImageView.setVisibility(View.GONE);
            }
            showChevron = true;
        }
        if (focusCurrent) {
            View lastChild = mBreadcrumbBarLayout.getChildAt(position);
            mBreadcrumbBarScrollView.requestChildFocus(mBreadcrumbBarLayout, lastChild);
        }
    }

    private void reloadFileInfoList(boolean focusCurrentCrumb) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        if (mAdapter != null) {
            mAdapter.cancelAllThumbRequests(true);
        }

        if (mPopulateFolderTask != null) {
            mPopulateFolderTask.cancel(true);
        }
        mPopulateFolderTask = new PopulateFolderTask(context, mCurrentFolder,
                mFileInfoList, mFileListLock, getSortMode(), true, true, true, mSdCardFolderCache, this);
        mPopulateFolderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        File leafFile = null;
        String leaf = shouldUseBackupFolder() ?
                PdfViewCtrlSettingsManager.getBackupCacheFolderTree(context) :
                PdfViewCtrlSettingsManager.getLocalFolderTree(context);
        if (!Utils.isNullOrEmpty(leaf)) {
            leafFile = new File(leaf);
        }
        while (leafFile != null && !leafFile.exists()) {
            leafFile = leafFile.getParentFile();
        }
        rebuildBreadcrumbBar(context, leafFile);

        focusCurrentCrumb = (focusCurrentCrumb || leafFile != null);
        if (mCurrentFolder != null) {
            setCurrentBreadcrumb(findBreadcrumb(mCurrentFolder), focusCurrentCrumb);
        } else {
            setCurrentBreadcrumb(0, focusCurrentCrumb);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        MiscUtils.updateAdapterViewWidthAfterGlobalLayout(mRecyclerView, mAdapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (Activity.RESULT_OK == resultCode) {
            if (RequestCode.PICK_PHOTO_CAM == requestCode) {
                boolean isCamera = false;
                String imageFilePath = "";
                try {
                    Map imageIntent = ViewerUtils.readImageIntent(data, activity, mOutputFileUri);
                    if (!ViewerUtils.checkImageIntent(imageIntent)) {
                        Utils.handlePdfFromImageFailed(activity, imageIntent);
                        return;
                    }
                    imageFilePath = ViewerUtils.getImageFilePath(imageIntent);
                    Uri selectedImage = ViewerUtils.getImageUri(imageIntent);
                    isCamera = ViewerUtils.isImageFromCamera(imageIntent);

                    // try to get display name
                    String filename = Utils.getDisplayNameFromImageUri(getContext(), selectedImage, imageFilePath);
                    if (Utils.isNullOrEmpty(filename)) {
                        // cannot get a valid filename
                        Utils.handlePdfFromImageFailed(activity, imageIntent);
                        return;
                    }

                    File documentFile = new File(mCurrentFolder, filename + ".pdf");
                    documentFile = new File(Utils.getFileNameNotInUse(documentFile.getAbsolutePath()));
                    String outputPath = ViewerUtils.imageIntentToPdf(activity, selectedImage, imageFilePath, documentFile.getAbsolutePath());
                    if (outputPath != null) {
                        String toastMsg = getString(R.string.dialog_create_new_document_filename_success) + mCurrentFolder.getPath();
                        CommonToast.showText(getActivity(), toastMsg, Toast.LENGTH_LONG);
                        if (mCallbacks != null) {
                            mCallbacks.onFileSelected(documentFile, "");
                        }
                    }

                    finishActionMode();

                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CREATE_NEW,
                            AnalyticsParam.createNewParam(isCamera ? AnalyticsHandlerAdapter.CREATE_NEW_ITEM_PDF_FROM_CAMERA : AnalyticsHandlerAdapter.CREATE_NEW_ITEM_PDF_FROM_IMAGE,
                                    AnalyticsHandlerAdapter.SCREEN_FOLDERS));
                } catch (FileNotFoundException e) {
                    CommonToast.showText(getContext(), getString(R.string.dialog_add_photo_document_filename_file_error), Toast.LENGTH_SHORT);
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } catch (Exception e) {
                    CommonToast.showText(getActivity(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } catch (OutOfMemoryError oom) {
                    MiscUtils.manageOOM(getContext());
                    CommonToast.showText(getContext(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                }

                // cleanup the image if it is from camera
                if (isCamera) {
                    FileUtils.deleteQuietly(new File(imageFilePath));
                }
            }
        }
    }

    public void saveCreatedDocument(PDFDoc doc, String title) {
        try {
            boolean hasExtension = FilenameUtils.isExtension(title, "pdf");
            if (!hasExtension) {
                title = title + ".pdf";
            }
            File documentFile = new File(mCurrentFolder, title);
            String filePath = Utils.getFileNameNotInUse(documentFile.getAbsolutePath());
            if (Utils.isNullOrEmpty(filePath)) {
                CommonToast.showText(getActivity(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                return;
            }
            documentFile = new File(filePath);

            doc.save(documentFile.getAbsolutePath(), SDFDoc.SaveMode.REMOVE_UNUSED, null);
            doc.close();

            String toastMsg = getString(R.string.dialog_create_new_document_filename_success) + filePath;
            CommonToast.showText(getActivity(), toastMsg, Toast.LENGTH_LONG);

            if (mCallbacks != null) {
                mCallbacks.onFileSelected(documentFile, "");
            }

            finishActionMode();
//            reloadFileInfoList();
        } catch (Exception e) {
            CommonToast.showText(getActivity(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        // If search gets cleared revert context back to non-search action
        mLastActionWasSearch = newText.length() > 0;

        // prevent clearing filter text when the fragment is hidden
        if (mAdapter != null && Utils.isNullOrEmpty(mFilterText)) {
            mAdapter.cancelAllThumbRequests(true);
            mAdapter.getFilter().filter(newText);
            boolean isEmpty = Utils.isNullOrEmpty(newText);
            mAdapter.setInSearchMode(!isEmpty);
        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mLastActionWasSearch = true;
        if (mRecyclerView != null) {
            mRecyclerView.requestFocus();
        }
        return false;
    }

    @Override
    public void onFileRenamed(FileInfo oldFile, FileInfo newFile) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (mSelectedFile == null || oldFile.getName().equals(mSelectedFile.getName())) {
            mSelectedFile = newFile; // update mSelectedFile
        }
        finishActionMode();
        hideFileInfoDrawer();
        //      if changes not in current folder, reload everything
        if (!mCurrentFolder.getAbsolutePath().equals(oldFile.getAbsolutePath())
                || !mCurrentFolder.getAbsolutePath().equals(newFile.getAbsolutePath())) {
            reloadFileInfoList(false);
        }
        handleFileUpdated(oldFile, newFile);
        Utils.safeNotifyDataSetChanged(mAdapter);
        try {
            PdfViewCtrlTabsManager.getInstance().updatePdfViewCtrlTabInfo(activity,
                    oldFile.getAbsolutePath(), newFile.getAbsolutePath(), newFile.getFileName());
            // update user bookmarks
            BookmarkManager.updateUserBookmarksFilePath(activity, oldFile.getAbsolutePath(), newFile.getAbsolutePath());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    @Override
    public void onFolderCreated(FileInfo rootFolder, FileInfo newFolder) {
        if (newFolder != null) {
            appendBreadcrumb(getActivity(), newFolder.getFile());
            // Refresh the view with new content.
            mCurrentFolder = newFolder.getFile();
            updateFileObserver();
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CREATE_NEW,
                    AnalyticsParam.createNewParam(AnalyticsHandlerAdapter.CREATE_NEW_ITEM_FOLDER, AnalyticsHandlerAdapter.SCREEN_FOLDERS));
        }
        reloadFileInfoList(true);
    }

    @Override
    public void onFileDuplicated(File fileCopy) {
        finishActionMode();
        hideFileInfoDrawer();
//        reloadFileInfoList();
        if (!mCurrentFolder.getAbsolutePath().equals(fileCopy.getAbsolutePath())) {
            reloadFileInfoList(false);
        }
    }

    @Override
    public void onFileDeleted(ArrayList<FileInfo> deletedFiles) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        finishActionMode();
        hideFileInfoDrawer();
        if (deletedFiles != null && deletedFiles.size() > 0) {
            boolean rebuildBreadcrumbs = false;
            boolean reloadList = false;
            File parent = null;
            // update user bookmarks
            for (FileInfo info : deletedFiles) {
                if (!rebuildBreadcrumbs && info.getType() == BaseFileInfo.FILE_TYPE_FOLDER && findBreadcrumb(info.getFile()) != -1) {
                    // Folder is part of breadcrumb bar - rebuild the breadcrumbs
                    rebuildBreadcrumbs = true;
                    parent = info.getFile().getParentFile();
                }
                BookmarkManager.removeUserBookmarks(activity, info.getAbsolutePath());
                if (!info.getParentDirectoryPath().equals(mCurrentFolder.getAbsolutePath())) {
                    reloadList = true;
                }
                if (mAdapter != null) {
                    mAdapter.evictFromMemoryCache(info.getAbsolutePath());
                }
            }
            if (rebuildBreadcrumbs) {
                rebuildBreadcrumbBar(activity, parent);

                if (mCurrentFolder != null) {
                    setCurrentBreadcrumb(findBreadcrumb(mCurrentFolder), true);
                } else {
                    setCurrentBreadcrumb(0, true);
                }
                String folderTree = getCrumbFolderTree();
                if (shouldUseBackupFolder()) {
                    PdfViewCtrlSettingsManager.updateBackupCacheFolderTree(activity, folderTree);
                } else {
                    PdfViewCtrlSettingsManager.updateLocalFolderTree(activity, folderTree);
                }
            }

            if (reloadList) {
                reloadFileInfoList(true);
            }
            handleFilesRemoved(deletedFiles);
        }
    }

    @Override
    public void onFileTrashed(ArrayList<FileInfo> deletedFiles) {
        onFileDeleted(deletedFiles);
        reloadFileInfoList(false);
    }

    @Override
    public void onTrashRemoved(TrashEntity trashEntity) {

    }

    @Override
    public void onTrashesLoaded(List<TrashEntity> trashEntityList) {

    }

    @Override
    public void onFileMoved(Map<FileInfo, Boolean> filesMoved, File targetFolder) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        finishActionMode();
        hideFileInfoDrawer();
//        reloadFileInfoList();
        ArrayList<FileInfo> targetFileList = new ArrayList<>();
        boolean reloadList = false;
        for (Map.Entry<FileInfo, Boolean> entry : filesMoved.entrySet()) {
            // only update if the move operation was successful
            if (entry.getValue()) {
                FileInfo info = entry.getKey();
                File targetFile = new File(targetFolder, info.getName());
                FileInfo targetFileInfo = new FileInfo(info.getType(), targetFile);
                // update recent and favorite lists
                handleFileUpdated(info, targetFileInfo);
                // update user bookmarks
                BookmarkManager.updateUserBookmarksFilePath(activity, info.getAbsolutePath(), targetFile.getAbsolutePath());
                targetFileList.add(targetFileInfo);
                if (!info.getAbsolutePath().equals(mCurrentFolder.getAbsolutePath())
                        || !targetFile.getAbsolutePath().equals(mCurrentFolder.getAbsolutePath())) {
                    reloadList = true;
                }
            }
        }

        if (reloadList) {
            reloadFileInfoList(false);
        }
        new FileManager.ChangeCacheFileTask(new ArrayList<FileInfo>(), targetFileList, mCacheLock).execute();
    }

    @Override
    public void onFileMoved(Map<FileInfo, Boolean> filesMoved, ExternalFileInfo targetFolder) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        finishActionMode();
        hideFileInfoDrawer();
//        reloadFileInfoList();
        ArrayList<FileInfo> targetFileList = new ArrayList<>();
        boolean reloadList = false;
        for (Map.Entry<FileInfo, Boolean> entry : filesMoved.entrySet()) {
            // only update if the move operation was successful
            if (entry.getValue()) {
                try {
                    FileInfo fileInfo = entry.getKey();
                    String targetFilePath = ExternalFileInfo.appendPathComponent(targetFolder.getUri(), fileInfo.getName()).toString();
                    FileInfo targetFileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL, targetFilePath,
                            fileInfo.getName(), false, 1);
                    // Update recent and favorite lists
                    handleFileUpdated(fileInfo, targetFileInfo);
                    // Update tab info
                    PdfViewCtrlTabsManager.getInstance().updatePdfViewCtrlTabInfo(activity,
                            fileInfo.getAbsolutePath(), targetFileInfo.getAbsolutePath(), targetFileInfo.getFileName());
                    // update user bookmarks
                    BookmarkManager.updateUserBookmarksFilePath(activity, fileInfo.getAbsolutePath(), targetFileInfo.getAbsolutePath());
                    // add target file to cache
                    targetFileList.add(targetFileInfo);
                    if (!fileInfo.getAbsolutePath().equals(mCurrentFolder.getAbsolutePath())
                            || !targetFilePath.equals(mCurrentFolder.getAbsolutePath())) {
                        reloadList = true;
                    }
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
            }
        }
        if (reloadList) {
            reloadFileInfoList(false);
        }
        new FileManager.ChangeCacheFileTask(new ArrayList<FileInfo>(), targetFileList, mCacheLock).execute();
    }

    @Override
    public void onFileMerged(ArrayList<FileInfo> mergedFiles, ArrayList<FileInfo> filesToDelete, FileInfo newFile) {
        finishActionMode();
        hideFileInfoDrawer();

        if (newFile == null) {
            return;
        }

        if (mCallbacks != null) {
            if (newFile.getType() == BaseFileInfo.FILE_TYPE_FILE) {
                mCallbacks.onFileSelected(newFile.getFile(), "");
            } else if (newFile.getType() == BaseFileInfo.FILE_TYPE_EXTERNAL) {
                mCallbacks.onExternalFileSelected(newFile.getAbsolutePath(), "");
            }
        }

        if (!newFile.getAbsolutePath().equals(mCurrentFolder.getAbsolutePath())) {
            reloadFileInfoList(false);
        }
        MiscUtils.removeFiles(filesToDelete);
    }

    @Override
    public void onFileChanged(String path, int event) {
        // this method is called from background thread

        if (path == null) {
            return;
        }

        File file = new File(path);

        Logger.INSTANCE.LogI(TAG, "file changes: "
                + file.getAbsolutePath() + " is file: "
                + file.isFile() + " is dir: "
                + file.isDirectory());

        boolean fileChanged = true;
        ArrayList<FileInfo> tempList;
        synchronized (mFileListLock) {
            tempList = new ArrayList<>(mFileInfoList);
        }
        switch (event &= RecursiveFileObserver.ALL_EVENTS) {
            case RecursiveFileObserver.MOVED_FROM:
            case RecursiveFileObserver.DELETE:
                if (event == RecursiveFileObserver.MOVED_FROM)
                    Logger.INSTANCE.LogI(TAG, "MOVE_FROM: ");
                else
                    Logger.INSTANCE.LogI(TAG, "DELETE: " + tempList.size());
                for (int i = 0; i < tempList.size(); i++) {
                    FileInfo fileInfo = tempList.get(i);
                    if (fileInfo.getAbsolutePath().equals(path)) {
                        // FileManager.deleteFileInCache(fileInfo);
                        new FileManager.ChangeCacheFileTask(fileInfo, new ArrayList<FileInfo>(), mCacheLock).execute();
                        tempList.remove(i);
                        break;
                    }
                }
                break;
            case RecursiveFileObserver.MOVED_TO:
            case RecursiveFileObserver.CREATE:
                Logger.INSTANCE.LogI(TAG, "CREATE: ");
                // add the new file to the right position based on sort method

                FileInfo fileInfo;
                if (file.isDirectory()) {
                    fileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_FOLDER, file);
                } else if (file.isFile()) {
                    fileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_FILE, file);
                } else {
                    fileChanged = false;
                    break;
                }

                // add the new file to the right position based on sort method
                for (int i = 0, count = tempList.size(); i < count; ++i) {
                    FileInfo f = tempList.get(i);
                    if (compare(f, fileInfo) > 0) {
                        tempList.add(i, fileInfo);
                        if (file.isFile()) {
                            //FileManager.addFileToCache(fileInfo);
                            new FileManager.ChangeCacheFileTask(new ArrayList<FileInfo>(), fileInfo, mCacheLock).execute();
                        }
                        break;
                    }
                }
                break;
            default:
                Logger.INSTANCE.LogI(TAG, "OTHER: " + event);
                fileChanged = false;
                break;
        }

        if (fileChanged) {
            synchronized (mFileListLock) {
                mFileInfoList.clear();
                mFileInfoList.addAll(tempList);
            }

            // run it in UI thread
            Handler handler = new Handler(Looper.getMainLooper());
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    updateFileListFilter();
                }
            };
            handler.post(runnable);
        }
    }

    private Comparator<FileInfo> getSortMode(
    ) {

        if (mSortMode != null) {
            return mSortMode;
        }

        return FileInfoComparator.folderPathOrder();
    }

    private int compare(FileInfo file1, FileInfo file2) {
        if (mSortMode != null) {
            return mSortMode.compare(file1, file2);
        }
        return FileInfoComparator.folderPathOrder().compare(file1, file2);
    }

    @Override
    public void onFileClicked(FileInfo fileInfo) {
        if (mIsSearchMode) {
            hideSoftKeyboard();
        }

        if (fileInfo.getType() == BaseFileInfo.FILE_TYPE_FILE) {
            onDocumentClicked(fileInfo);
        } else if (fileInfo.getType() == BaseFileInfo.FILE_TYPE_FOLDER) {
            appendBreadcrumb(getActivity(), fileInfo.getFile());
            // Refresh the view with new content.
            mCurrentFolder = fileInfo.getFile();
            updateFileObserver();
            reloadFileInfoList(true);
        }
    }

    protected void onDocumentClicked(FileInfo fileInfo) {
        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_OPEN_FILE,
                AnalyticsParam.openFileParam(fileInfo, AnalyticsHandlerAdapter.SCREEN_FOLDERS));
        if (mCallbacks != null) {
            mCallbacks.onFileSelected(fileInfo.getFile(), "");
        }
    }

    @Override
    public void onLocalFolderSelected(int requestCode, Object object, File folder) {
        if (requestCode == RequestCode.MOVE_FILE) {
            if (mSelectedFile != null) {
                FileManager.move(getActivity(), new ArrayList<>(Collections.singletonList(mSelectedFile)), folder, LocalFolderViewFragment.this);
            }
        } else if (requestCode == RequestCode.MOVE_FILE_LIST) {
            FileManager.move(getActivity(), mFileInfoSelectedList, folder, LocalFolderViewFragment.this);
        }
    }

    @Override
    public void onExternalFolderSelected(int requestCode, Object object, ExternalFileInfo folder) {
        if (requestCode == RequestCode.MOVE_FILE) {
            if (mSelectedFile != null) {
                FileManager.move(getActivity(), new ArrayList<>(Collections.singletonList(mSelectedFile)), folder, LocalFolderViewFragment.this);
            }
        } else if (requestCode == RequestCode.MOVE_FILE_LIST) {
            FileManager.move(getActivity(), mFileInfoSelectedList, folder, LocalFolderViewFragment.this);
        }
    }

    @Override
    public void onPreLaunchViewer() {
        mViewerLaunching = true;
    }

    @Override
    public void onDataChanged() {
        if (isAdded()) {
            reloadFileInfoList(true);
        } else {
            // otherwise it will be reloaded in resumeFragment
            mDataChanged = true;
        }
    }

    @Override
    public void onProcessNewIntent() {
        finishActionMode();
    }

    @Override
    public void onDrawerOpened() {
        finishActionMode();
        if (mIsSearchMode) {
            hideSoftKeyboard();
        }
    }

    @Override
    public void onDrawerSlide() {
        finishActionMode();
    }

    @Override
    public boolean onBackPressed() {
        if (!isAdded()) {
            return false;
        }

        boolean handled = false;
        if (mFabMenu != null && mFabMenu.isOpened()) {
            // Close fab menu
            mFabMenu.close(true);
            handled = true;
        } else if (mFileInfoDrawer != null) {
            // Hide file info drawer
            hideFileInfoDrawer();
            handled = true;
        } else if (mActionMode != null) {
            // Exit action mode
            finishActionMode();
            handled = true;
        } else if (mIsSearchMode) {
            // Exit search mode
            finishSearchView();
            handled = true;
        } else if (shouldNavigateParentOnBack()) {
            if (shouldUseBackupFolder()) {
                if (!mCurrentFolder.getAbsolutePath().equals(mRootFolder.getAbsolutePath())) {
                    handled = navigateParentDirectories();
                }
            } else {
                handled = navigateParentDirectories();
            }
        }
        return handled;
    }

    protected boolean shouldNavigateParentOnBack() {
        return true;
    }

    private boolean navigateParentDirectories() {
        boolean handled = false;
        // Navigate up through directories
        File rootDir = mRootFolder;
        if (Utils.isLollipop()) {
            while (rootDir != null && rootDir.getParentFile() != null && (!rootDir.getParentFile().getAbsolutePath().equalsIgnoreCase("/") && !DEBUG)) {
                rootDir = rootDir.getParentFile();
            }
        } else {
            while (rootDir != null && rootDir.getParentFile() != null) {
                rootDir = rootDir.getParentFile();
            }
        }
        if (!mCurrentFolder.equals(rootDir) && mCurrentFolder.getParentFile() != null && !mCurrentFolder.getParent().equals("/")) {
            mCurrentFolder = mCurrentFolder.getParentFile();
            updateFileObserver();
            reloadFileInfoList(true);
            handled = true;
        }

        return handled;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (ShortcutHelper.isFind(keyCode, event)) {
            SearchView searchView = (SearchView) mSearchMenuItem.getActionView();
            if (searchView.isShown()) {
                searchView.setFocusable(true);
                searchView.requestFocus();
            } else {
                mSearchMenuItem.expandActionView();
            }
            return true;
        }
        return false;
    }

    @Override
    public void onShowFileInfo(int position) {
        if (mFileUtilCallbacks != null) {
            mSelectedFile = mAdapter.getItem(position);
            mFileInfoDrawer = mFileUtilCallbacks.showFileInfoDrawer(mFileInfoDrawerCallback);
        }
    }

    protected void onFilterResultsNoStringMatch() {
    }

    protected void onResetFilterResultsNoStringMatch() {
    }

    protected boolean inSearchMode() {
        return false;
    }

    @Override
    protected void showZeroSearchResults() {
        super.showZeroSearchResults();
        LifecycleUtils.runOnResume(this, () -> {
            mEmptyTextViewForFilter.setVisibility(View.VISIBLE);
            mEmptyTextViewForFilter.setText(R.string.textview_empty_because_no_string_match);
            onFilterResultsNoStringMatch();
        });
    }

    @Override
    protected void showNoTypeFilterInSearchResults() {
        super.showNoTypeFilterInSearchResults();
        LifecycleUtils.runOnResume(this, () -> {
            mEmptyTextViewForFilter.setVisibility(View.VISIBLE);
            mEmptyTextViewForFilter.setText(R.string.textview_empty_because_no_files_of_selected_type);
            onFilterResultsNoStringMatch();
        });
    }

    @Override
    protected void showNoTypeFilter() {
        super.showNoTypeFilter();
        LifecycleUtils.runOnResume(this, () -> {
            mEmptyTextView.setVisibility(View.VISIBLE);
            mEmptyTextView.setText(R.string.textview_empty_because_no_files_of_selected_type);
        });
    }

    @Override
    protected void showOriginalEmptyResults() {
        super.showOriginalEmptyResults();
        LifecycleUtils.runOnResume(this, () -> {
            mEmptyTextView.setVisibility(View.VISIBLE);
            mEmptyTextView.setText(R.string.textview_empty_file_list);
        });
    }

    @Override
    public void onFilterResultsPublished(int resultCode) {
        LifecycleUtils.runOnResume(this, () -> {
            mEmptyTextViewForFilter.setVisibility(View.GONE);
            mEmptyTextView.setVisibility(View.GONE);
            mBreadcrumbBarScrollView.setVisibility(View.VISIBLE);
            onResetFilterResultsNoStringMatch();
            if (mAdapter != null) {
                if (mEmptyTextView != null) {
                    if (mAdapter.getItemCount() > 0) {
                        scrollToCurrentFile(mRecyclerView);
                    } else if (mIsFullSearchDone) {
                        switch (resultCode) {
                            case FileListFilter.FILTER_RESULT_NO_STRING_MATCH:
                                showZeroSearchResults();
                                break;
                            case FileListFilter.FILTER_RESULT_NO_ITEMS_OF_SELECTED_FILE_TYPES:
                                if (mLastActionWasSearch) {
                                    showZeroSearchResults();
                                } else if (inSearchMode()) {
                                    showNoTypeFilterInSearchResults();
                                } else {
                                    showNoTypeFilter();
                                }
                                break;
                            default:
                                showOriginalEmptyResults();
                                break;
                        }
                    }
                }

                mNotSupportedTextView.setVisibility(View.GONE);
                if (mCurrentFolder != null && getContext() != null) {
                    String[] list = mCurrentFolder.list();
                    if (list != null) {
                        int fileCount = list.length;
                        if (fileCount > mAdapter.getItemCount()) {
                            int extraCount = fileCount - mAdapter.getItemCount();
                            String fileStr = extraCount > 1 ? getString(R.string.files) : getString(R.string.file);
                            mNotSupportedTextView.setText(getString(R.string.num_files_not_supported, extraCount, fileStr));
                            mNotSupportedTextView.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        });
    }

    // Must be called after onCreateView
    public void updateFileObserver() {
        if (mFileObserver != null) { // stop watching and observing file observer
            mFileObserver.stopObserving(getViewLifecycleOwner());
        }
        if (mCurrentFolder == null) {
            return;
        }
        mFileObserver =
                new RecursiveFileObserver(
                        mCurrentFolder.getAbsolutePath(),
                        RecursiveFileObserver.CHANGES_ONLY,
                        this,
                        getViewLifecycleOwner()
                );
    }

    private void resumeFragment() {
        Logger.INSTANCE.LogD(TAG, "resumeFragment");

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        mViewerLaunching = false;

        String directory = shouldUseBackupFolder() ?
                PdfViewCtrlSettingsManager.getBackupFolderPath(activity) :
                PdfViewCtrlSettingsManager.getLocalFolderPath(activity);
        if (!Utils.isNullOrEmpty(directory)) {
            mCurrentFolder = new File(directory);
            updateFileObserver();
        }

        reloadFileInfoList(mDataChanged);
        mDataChanged = false;

        if (mLocalFolderViewFragmentListener != null) {
            mLocalFolderViewFragmentListener.onLocalFolderShown();
        }
    }

    private void pauseFragment() {
        Logger.INSTANCE.LogD(TAG, "pauseFragment");

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (mIsSearchMode && !mViewerLaunching) {
            finishSearchView();
        }

        if (mPopulateFolderTask != null) {
            mPopulateFolderTask.cancel(true);
        }

        if (mAdapter != null) {
            mAdapter.cancelAllThumbRequests(true);
            mAdapter.cleanupResources();
        }

        // Save the current folder path
        if (shouldUseBackupFolder()) {
            PdfViewCtrlSettingsManager.updateBackupCacheFolderPath(activity, mCurrentFolder.getAbsolutePath());
        } else {
            PdfViewCtrlSettingsManager.updateLocalFolderPath(activity, mCurrentFolder.getAbsolutePath());
        }
        // Save the folder tree, ie. the right-most breadcrumb
        String folderTree = getCrumbFolderTree();
        if (shouldUseBackupFolder()) {
            PdfViewCtrlSettingsManager.updateBackupCacheFolderTree(activity, folderTree);
        } else {
            PdfViewCtrlSettingsManager.updateLocalFolderTree(activity, folderTree);
        }

        if (mLocalFolderViewFragmentListener != null) {
            mLocalFolderViewFragmentListener.onLocalFolderHidden();
        }
    }

    public void setLocalFolderViewFragmentListener(LocalFolderViewFragmentListener listener) {
        mLocalFolderViewFragmentListener = listener;
    }

    private String getCrumbFolderTree() {
        // Save the folder tree, ie. the right-most breadcrumb
        String folderTree = "";
        if (mBreadcrumbBarLayout != null && mBreadcrumbBarLayout.getChildCount() > 0) {
            View crumb = mBreadcrumbBarLayout.getChildAt(mBreadcrumbBarLayout.getChildCount() - 1);
            Object tag = crumb.getTag();
            if (tag != null && tag instanceof File) {
                File file = (File) tag;
                folderTree = file.getAbsolutePath();
            }
        }
        return folderTree;
    }

    // close soft keyboard if in searching mode
    private void closeSearch() {
        if (mIsSearchMode && mSearchMenuItem != null) {
            SearchView searchView = (SearchView) mSearchMenuItem.getActionView();
            EditText editText = searchView.findViewById(R.id.search_src_text);
            if (editText.isFocused()) {
                editText.onEditorAction(EditorInfo.IME_ACTION_SEARCH);
            }
        }
    }

    @Override
    public void onPopulateFolderTaskStarted(
    ) {

        Context context = getContext();
        if (context == null) {
            return;
        }

        synchronized (mFileListLock) {
            mFileInfoList.clear();
        }
        updateFileListFilter();

        if (mEmptyTextView != null) {
            mLoadingFileHandler.sendEmptyMessageDelayed(0, 100);
        }
        if (mProgressBarView != null) {
            mProgressBarView.setVisibility(View.VISIBLE);
        }

        setReloadActionButtonState(true);
        mIsFullSearchDone = false;
    }

    @Override
    public void onPopulateFolderTaskProgressUpdated(
            File currentFolder
    ) {

        mLoadingFileHandler.removeMessages(0);
        if (mProgressBarView != null) {
            mProgressBarView.setVisibility(View.GONE);
        }

        showPopulatedFolder(currentFolder);
        updateFileListFilter();
        setReloadActionButtonState(false);
    }

    @Override
    public void onPopulateFolderTaskFinished(
    ) {

        mIsFullSearchDone = true;
        updateFileListFilter();
    }

    void showPopulatedFolder(
            File currentFolder
    ) {

        if (currentFolder == null) {
            return;
        }

        mInSDCardFolder = false;
        Boolean isSdCardFolder = mSdCardFolderCache.get(currentFolder.getAbsolutePath());
        if (isSdCardFolder != null && isSdCardFolder) {
            mInSDCardFolder = true;
        }

        if (mGoToSdCardView == null || mRecyclerView == null || mFabMenu == null) {
            return;
        }

        if (Utils.isLollipop()) {
            if (mInSDCardFolder) {
                if (mSnackBar == null) {
                    mSnackBar = Snackbar.make(mRecyclerView, R.string.snack_bar_local_folder_read_only, Snackbar.LENGTH_INDEFINITE);
                    mSnackBar.setAction(getString(R.string.snack_bar_local_folder_read_only_redirect).toUpperCase(),
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // go to external tab
                                    if (null != mJumpNavigationCallbacks) {
                                        finishActionMode();
                                        mJumpNavigationCallbacks.gotoExternalTab();
                                    }
                                }
                            });

                    mSnackBar.addCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            mSnackBar = null;
                        }
                    });
                    //mSnackBar.show();
                }
                // Show dialog re-direct user to SD card tab
                mRecyclerView.setVisibility(View.GONE);
                mFabMenu.setVisibility(View.GONE);
                mGoToSdCardView.setVisibility(View.VISIBLE);
            } else {
                if (mSnackBar != null) {
                    if (mSnackBar.isShown()) {
                        mSnackBar.dismiss();
                    }
                    mSnackBar = null;
                }
                mGoToSdCardView.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
                mFabMenu.setVisibility(mShouldShowFab ? View.VISIBLE : View.GONE);
            }
        }
    }

    @Override
    public void onConversionFinished(String path, boolean isLocal) {
        if (mFileObserver != null) {
            mFileObserver.startWatching();
        }
        if (isLocal) {
            if (mCallbacks != null) {
                mCallbacks.onFileSelected(new File(path), "");
            }
        } else {
            if (mCallbacks != null) {
                mCallbacks.onExternalFileSelected(path, "");
            }
        }
    }

    @Override
    public void onConversionFailed(String errorMessage) {
        if (mFileObserver != null) {
            mFileObserver.startWatching();
        }
        Utils.safeShowAlertDialog(getActivity(),
                R.string.import_webpage_error_message_title,
                R.string.create_file_invalid_error_message);
    }

    protected HtmlConversionComponent getHtmlConversionComponent(View view) {
        return new Html2PdfComponent(view.getContext(), this);
    }

    protected void convertHtml() {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            if (mFileObserver != null) {
                mFileObserver.stopWatching();
            }
            mHtmlConversionComponent.handleWebpageToPDF(getActivity(), Uri.parse(mCurrentFolder.getAbsolutePath()));
        }
    }

    private static class LoadingFileHandler extends Handler {
        private final WeakReference<LocalFolderViewFragment> mFragment;

        LoadingFileHandler(LocalFolderViewFragment fragment) {
            mFragment = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            LocalFolderViewFragment fragment = mFragment.get();
            if (fragment != null && fragment.mEmptyTextView != null) {
                fragment.mEmptyTextView.setText(R.string.loading_files_wait);
                fragment.mEmptyTextView.setVisibility(View.VISIBLE);
            }
            removeMessages(0);
        }
    }

    private final LoadingFileHandler mLoadingFileHandler = new LoadingFileHandler(this);
}
