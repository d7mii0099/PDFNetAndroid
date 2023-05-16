//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.browser.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.github.clans.fab.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.pdftron.common.PDFNetException;
import com.pdftron.demo.R;
import com.pdftron.demo.browser.db.file.AllFilesDataSource;
import com.pdftron.demo.browser.db.file.FileDao;
import com.pdftron.demo.browser.db.file.FileDataSource;
import com.pdftron.demo.browser.db.file.FileDatabase;
import com.pdftron.demo.browser.db.file.FileEntity;
import com.pdftron.demo.browser.db.trash.TrashEntity;
import com.pdftron.demo.browser.model.FileEntityMapper;
import com.pdftron.demo.browser.model.FileItem;
import com.pdftron.demo.databinding.FragmentLocalFileViewBinding;
import com.pdftron.demo.dialog.FilePickerDialogFragment;
import com.pdftron.demo.dialog.MergeDialogFragment;
import com.pdftron.demo.navigation.FileBrowserViewFragment;
import com.pdftron.demo.navigation.FileInfoDrawer;
import com.pdftron.demo.navigation.callbacks.FileManagementListener;
import com.pdftron.demo.navigation.callbacks.FileUtilCallbacks;
import com.pdftron.demo.navigation.callbacks.JumpNavigationCallbacks;
import com.pdftron.demo.navigation.callbacks.MainActivityListener;
import com.pdftron.demo.navigation.component.html2pdf.Html2PdfComponent;
import com.pdftron.demo.navigation.component.html2pdf.HtmlConversionComponent;
import com.pdftron.demo.utils.AddDocPdfHelper;
import com.pdftron.demo.utils.FileManager;
import com.pdftron.demo.utils.LifecycleUtils;
import com.pdftron.demo.utils.MiscUtils;
import com.pdftron.demo.utils.RecursiveFileObserver;
import com.pdftron.demo.utils.SettingsManager;
import com.pdftron.demo.utils.ThumbnailPathCacheManager;
import com.pdftron.demo.utils.ThumbnailWorker;
import com.pdftron.demo.widget.ImageViewTopCrop;
import com.pdftron.demo.widget.MoveUpwardBehaviour;
import com.pdftron.filters.SecondaryFileFilter;
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
import com.pdftron.sdf.SDFDoc;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

public class LocalFileViewFragment extends FileBrowserViewFragment implements
        FileManagementListener,
        FilePickerDialogFragment.LocalFolderListener,
        FilePickerDialogFragment.ExternalFolderListener,
        MainActivityListener,
        MergeDialogFragment.MergeDialogFragmentListener,
        ActionMode.Callback,
        HtmlConversionComponent.HtmlConversionListener {

    private static final String TAG = LocalFileViewFragment.class.getName();
    private static final boolean DEBUG = false;

    protected final Object mFileChangeLock = new Object();
    protected ArrayList<FileInfo> mFileInfoSelectedList = new ArrayList<>();
    protected ArrayList<FileInfo> mMergeFileList;
    protected ArrayList<FileInfo> mMergeTempFileList;
    protected FileInfo mSelectedFile;

    protected String mDocumentTitle;

    private FileUtilCallbacks mFileUtilCallbacks;
    protected JumpNavigationCallbacks mJumpNavigationCallbacks;

    protected BaseQuickAdapter mAdapter;
    protected AdapterHelper mAdapterHelper;
    protected ItemSelectionHelper mItemSelectionHelper;

    private Menu mOptionsMenu;
    private MenuItem mSearchMenuItem;
    private FileInfoDrawer mFileInfoDrawer;
    private RecursiveFileObserver mFileObserver;
    private PDFDoc mCreatedDoc;
    private String mCreatedDocumentTitle;
    private String mImageFilePath;
    private Uri mImageUri;
    private String mImageFileDisplayName;
    private Uri mOutputFileUri;
    private boolean mIsCamera;
    private boolean mIsSearchMode;
    private boolean mViewerLaunching;
    private boolean mActionLock = true;
    private boolean mFileEventLock;
    protected String mFilterText = "";

    private MenuItem itemDuplicate;
    private MenuItem itemEdit;
    protected MenuItem itemDelete;
    private MenuItem itemMove;
    private MenuItem itemMerge;
    private MenuItem itemFavorite;
    private MenuItem itemShare;

    private LocalFileViewFragmentListener mLocalFileViewFragmentListener;
    private HtmlConversionComponent mHtmlConversionComponent;

    private FilesViewModel mFilesViewModel;
    private FilterSettingsComponent mFilterSettingsComponent;
    private FilterSettingsViewModel mFilterSettingsViewModel;

    private final CompositeDisposable mFileListUpdaterDisposable = new CompositeDisposable();
    private final CompositeDisposable mFilterUpdateDisposable = new CompositeDisposable();

    public static LocalFileViewFragment newInstance() {
        return new LocalFileViewFragment();
    }

    protected FileBrowserTheme mTheme;

    protected FragmentLocalFileViewBinding mBinding;

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

        itemDelete.setVisible(true);
        itemMove.setVisible(true);
        itemMerge.setVisible(true);
        itemShare.setVisible(true);

        if (mFileInfoSelectedList.size() > 1) {
            itemDuplicate.setVisible(false);
            itemEdit.setVisible(false);
            itemFavorite.setVisible(false);
        } else {
            itemDuplicate.setVisible(true);
            itemEdit.setVisible(true);
            itemFavorite.setVisible(true);
            if (!mFileInfoSelectedList.isEmpty()) {
                if (canAddToFavorite(mFileInfoSelectedList.get(0))) {
                    itemFavorite.setTitle(activity.getString(R.string.action_add_to_favorites));
                } else {
                    itemFavorite.setTitle(activity.getString(R.string.action_remove_from_favorites));
                }
            }
        }
        mode.setTitle(Utils.getLocaleDigits(Integer.toString(mFileInfoSelectedList.size())));
        // Ensure items are always shown
        itemDuplicate.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        itemEdit.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        itemDelete.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        itemMove.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        super.onDestroyActionMode(mode);
        mActionMode = null;
        clearFileInfoSelectedList();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (super.onCreateActionMode(mode, menu)) {
            return true;
        }

        mode.getMenuInflater().inflate(R.menu.cab_fragment_file_operations, menu);

        itemDuplicate = menu.findItem(R.id.cab_file_copy);
        itemEdit = menu.findItem(R.id.cab_file_rename);
        itemDelete = menu.findItem(R.id.cab_file_delete);
        itemMove = menu.findItem(R.id.cab_file_move);
        itemMerge = menu.findItem(R.id.cab_file_merge);
        itemFavorite = menu.findItem(R.id.cab_file_favorite);
        itemShare = menu.findItem(R.id.cab_file_share);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        FragmentActivity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return false;
        }
        if (mFileInfoSelectedList.isEmpty()) {
            return false;
        }

        boolean isSDCardFile = Utils.isSdCardFile(activity, mFileInfoSelectedList.get(0).getFile());
        mFileEventLock = true;
        if (item.getItemId() == R.id.cab_file_rename) {
            if (isSDCardFile && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.controls_misc_rename))) {
                finishActionMode();
                return true;
            }
            FileManager.rename(activity, mFileInfoSelectedList.get(0).getFile(), LocalFileViewFragment.this);
            return true;
        } else if (item.getItemId() == R.id.cab_file_copy) {
            if (isSDCardFile && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.controls_misc_duplicate))) {
                finishActionMode();
                return true;
            }
            FileManager.duplicate(activity, mFileInfoSelectedList.get(0).getFile(), LocalFileViewFragment.this);
            return true;
        } else if (item.getItemId() == R.id.cab_file_move) {
            if (isSDCardFile && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.action_file_move))) {
                finishActionMode();
                return true;
            }

            // Creates the dialog in full screen mode
            FilePickerDialogFragment dialogFragment = FilePickerDialogFragment.newInstance(RequestCode.MOVE_FILE_LIST,
                    Environment.getExternalStorageDirectory());
            dialogFragment.setLocalFolderListener(LocalFileViewFragment.this);
            dialogFragment.setExternalFolderListener(LocalFileViewFragment.this);
            dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null) {
                dialogFragment.show(fragmentManager, "file_picker_dialog_fragment");
            }
            return true;
        } else if (item.getItemId() == R.id.cab_file_delete) {
            if (isSDCardFile && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.delete))) {
                finishActionMode();
                return true;
            }

            FileManager.delete(activity, mFileInfoSelectedList, LocalFileViewFragment.this);
            return true;
        } else if (item.getItemId() == R.id.cab_file_merge) {
            if (isSDCardFile && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.merge))) {
                finishActionMode();
                return true;
            }
            handleMerge(mFileInfoSelectedList);
            return true;
        } else if (item.getItemId() == R.id.cab_file_favorite) {
            handleAddToFavorite(mFileInfoSelectedList.get(0));

            finishActionMode();
            // Update favorite file indicators
            Utils.safeNotifyDataSetChanged(mAdapter);
            return true;
        } else if (item.getItemId() == R.id.cab_file_share) {
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

    protected void handleMerge(ArrayList<FileInfo> files) {
        // Create and show file merge dialog-fragment
        MergeDialogFragment mergeDialog = getMergeDialogFragment(files, AnalyticsHandlerAdapter.SCREEN_ALL_DOCUMENTS);
        mergeDialog.initParams(LocalFileViewFragment.this);

        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            mergeDialog.show(fragmentManager, "merge_dialog");
        }
    }

    protected void performMerge(FileInfo targetFile) {
        FileManager.merge(getActivity(), mMergeFileList, mMergeTempFileList, targetFile, LocalFileViewFragment.this);
    }

    private AllFilesListAdapter createAndBindListAdapter(@NonNull RecyclerView recyclerView, @NonNull StickyHeader stickyHeader) {
        Context context = getContext();
        if (context != null) {
            AllFilesListAdapter allFilesListAdapter = createNewAllFilesListAdapter(context);
            allFilesListAdapter.setItemSelectionListener(mItemSelectionHelper);
            allFilesListAdapter.bindToRecyclerView(mBinding.recyclerView); // prefer this instead of mBinding.recyclerView.setAdapter(mAdapter);
            setShowInfoButton(allFilesListAdapter);
            setStickyHeaderSettings(stickyHeader);
            recyclerView.setLayoutManager(new StickyRecyclerView.StickLinearLayoutManager(getContext(), stickyHeader));
            return allFilesListAdapter;
        } else {
            throw new RuntimeException("Context must not be null when creating a file adapter");
        }
    }

    protected AllFilesListAdapter createNewAllFilesListAdapter(@NonNull Context context) {
        return new AllFilesListAdapter(context);
    }

    private AllFilesGridAdapter createAndBindGridAdapter(@NonNull RecyclerView recyclerView,
            @NonNull StickyHeader stickyHeader, @IntRange(from = 0, to = 6) int gridCount) {
        Activity activity = getActivity();
        if (activity != null) {
            AllFilesGridAdapter allFilesGridAdapter = createNewAllFilesGridAdapter(activity, gridCount);
            //noinspection unchecked
            allFilesGridAdapter.setItemSelectionListener(mItemSelectionHelper);
            allFilesGridAdapter.bindToRecyclerView(mBinding.recyclerView); // prefer this instead of mBinding.recyclerView.setAdapter(mAdapter);
            setShowInfoButton(allFilesGridAdapter);
            recyclerView.setLayoutManager(new GridLayoutManager(activity, gridCount));
            stickyHeader.disable();
            return allFilesGridAdapter;
        } else {
            throw new RuntimeException("Context must not be null when creating a file adapter");
        }
    }

    protected AllFilesGridAdapter createNewAllFilesGridAdapter(@NonNull Activity context, @IntRange(from = 0, to = 6) int gridCount) {
        return new AllFilesGridAdapter(context, gridCount);
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

    protected void handleFileRemoved(FileInfo file) {
        FragmentActivity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        getRecentFilesManager().removeFile(activity, file);
        getFavoriteFilesManager().removeFile(activity, file);
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
            // Setup thumbnail worker, if required
            if (mThumbnailWorker == null) {
                Point dimensions = drawer.getDimensions();
                mThumbnailWorker = new ThumbnailWorker(activity, dimensions.x, dimensions.y, null);
                mThumbnailWorker.setListener(mThumbnailWorkerListener);
            }

            if (mSelectedFile != null) {
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
            return true;
        }

        @Override
        public boolean onPrepareDrawerMenu(FileInfoDrawer drawer, Menu menu) {
            Activity activity = getActivity();
            if (activity == null || mSelectedFile == null || menu == null) {
                return false;
            }
            boolean changed = false;
            MenuItem menuItem = menu.findItem(R.id.cab_file_favorite);
            if (menuItem != null) {
                if (canAddToFavorite(mSelectedFile)) {
                    menuItem.setTitle(activity.getString(R.string.action_add_to_favorites));
                    menuItem.setTitleCondensed(activity.getString(R.string.action_favorite));
                    menuItem.setIcon(R.drawable.ic_star_white_24dp);
                } else {
                    menuItem.setTitle(activity.getString(R.string.action_remove_from_favorites));
                    menuItem.setTitleCondensed(activity.getString(R.string.action_unfavorite));
                    menuItem.setIcon(R.drawable.ic_star_filled_white_24dp);
                }
                changed = true;
            }
            return changed;
        }

        @Override
        public boolean onDrawerMenuItemClicked(FileInfoDrawer drawer, MenuItem menuItem) {
            FragmentActivity activity = getActivity();
            if (activity == null || mSelectedFile == null || mActionLock) {
                return false;
            }

            boolean isSDCardFile = Utils.isSdCardFile(activity, mSelectedFile.getFile());
            mFileEventLock = true;
            if (menuItem.getItemId() == R.id.cab_file_rename) {
                if (isSDCardFile && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.controls_misc_rename))) {
                    hideFileInfoDrawer();
                    return true;
                }
                renameFile(activity, mSelectedFile);
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_copy) {
                if (isSDCardFile && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.controls_misc_duplicate))) {
                    hideFileInfoDrawer();
                    return true;
                }
                duplicateFile(activity, mSelectedFile);
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_move) {
                if (isSDCardFile && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.action_file_move))) {
                    hideFileInfoDrawer();
                    return true;
                }
                moveFile();
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_delete) {
                if (isSDCardFile && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.delete))) {
                    hideFileInfoDrawer();
                    return true;
                }
                deleteFile(activity, mSelectedFile);
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_merge) {
                if (isSDCardFile && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.merge))) {
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
            if (mSelectedFile != null) {
                onFileClicked(mSelectedFile);
            }
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

            try {
                PDFDoc doc = new PDFDoc(mSelectedFile.getAbsolutePath());
                doc.initSecurityHandler();
                loadDocInfo(doc);
            } catch (PDFNetException e) {
                mTitle = null;
                mAuthor = null;
                mProducer = null;
                mCreator = null;
                mPageCount = -1;
            }

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
                    docInfo.getProducer();
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
        FileManager.rename(context, selectedFile.getFile(), LocalFileViewFragment.this);
    }

    protected void duplicateFile(Context context, FileInfo selectedFile) {
        FileManager.duplicate(context, selectedFile.getFile(), LocalFileViewFragment.this);
    }

    protected void moveFile() {
        FilePickerDialogFragment dialogFragment = FilePickerDialogFragment.newInstance(RequestCode.MOVE_FILE,
                Environment.getExternalStorageDirectory());
        dialogFragment.setLocalFolderListener(LocalFileViewFragment.this);
        dialogFragment.setExternalFolderListener(LocalFileViewFragment.this);
        dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            dialogFragment.show(fragmentManager, "file_picker_dialog_fragment");
        }
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
                LocalFileViewFragment.this);
    }

    protected void favoriteFile(FileInfo selectedFile) {
        handleAddToFavorite(selectedFile);
        // Update favorite file indicators
        Utils.safeNotifyDataSetChanged(mAdapter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        mTheme = FileBrowserTheme.fromContext(activity);
        Logger.INSTANCE.LogD(TAG, "onCreate");
        // Control whether a fragment instance is retained across Activity re-creation (such as from a configuration change).
        // This can only be used with fragments not in the back stack. If set, the fragment lifecycle will be slightly different when an activity is recreated
        setRetainInstance(true);

        // This Fragment wants to be able to have action bar items.
        setHasOptionsMenu(true);

        if (null != savedInstanceState) {
            mOutputFileUri = savedInstanceState.getParcelable("output_file_uri");
            mIsCamera = savedInstanceState.getBoolean("is_photo_from_camera");
        }

        // Add listeners for file database
        FileDatabase db = FileDatabase.getInstance(activity);
        final FileDao fileDao = db.fileDao();
        FileDataSource dataSource = new AllFilesDataSource(fileDao);

        mFilesViewModel = FilesViewModel.from(this, dataSource);
        mFilesViewModel.getFileListState()
                .observe(this,
                        new Observer<FilesViewModel.FileListState>() {
                            @Override
                            public void onChanged(@Nullable FilesViewModel.FileListState fileListState) {
                                if (fileListState != null) {
                                    handleRecyclerViewEvents(fileListState);
                                }
                            }
                        });

        mFilterSettingsViewModel = ViewModelProviders.of(this)
                .get(FilterSettingsViewModel.class);
    }

    @Override
    public void onResume() {
        Logger.INSTANCE.LogD(TAG, "onResume");
        super.onResume();
        resumeFragment();
    }

    @Override
    public void onPause() {
        Logger.INSTANCE.LogD(TAG, "onPause");
        super.onPause();
        pauseFragment();
    }

    public void cleanupResources() {
        // cleanup previous resource
        if (mAdapter != null) {
            mAdapterHelper.cancelAllThumbRequests(true);
            mAdapterHelper.cleanupResources();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        AnalyticsHandlerAdapter.getInstance().endTimedEvent(AnalyticsHandlerAdapter.EVENT_SCREEN_ALL_DOCUMENTS);
    }

    @Override
    public void onDestroyView() {
        Logger.INSTANCE.LogD(TAG, "onDestroyView");
        super.onDestroyView();

        mFileListUpdaterDisposable.clear();
        mFilterUpdateDisposable.clear();

        mAdapter = null;
        mAdapterHelper = null;
    }

    @Override
    public void onDestroy() {
        Logger.INSTANCE.LogD(TAG, "onDestroy");
        cleanupResources();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mOutputFileUri != null) {
            outState.putParcelable("output_file_uri", mOutputFileUri);
        }
        outState.putBoolean("is_photo_from_camera", mIsCamera);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        MiscUtils.handleLowMemory(getContext());
        mAdapterHelper.cancelAllThumbRequests();
        mAdapterHelper.cleanupResources();
        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_LOW_MEMORY, AnalyticsParam.lowMemoryParam(TAG));
        Logger.INSTANCE.LogE(TAG, "low memory");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Setup file observer, if one already exists then stop observing the old one before setting a new one
        if (mFileObserver != null) {
            mFileObserver.stopObserving(getViewLifecycleOwner());
        }

        File downloadFolder = Utils.getExternalDownloadDirectory(container.getContext());
        if (downloadFolder != null) {
            String downloadFolderPath = downloadFolder.getAbsolutePath();
            mFileObserver =
                    new RecursiveFileObserver(
                            downloadFolderPath,
                            RecursiveFileObserver.CHANGES_ONLY,
                            this,
                            getViewLifecycleOwner()
                    );
        }

        // Clear the submenu header for Filter menu item
        final Context context = getContext();
        if (context != null) {
            mFilterSettingsComponent =
                    new FilterSettingsComponent(
                            context,
                            this,
                            null,
                            mFilterSettingsViewModel
                    );

            final int gridCount = mFilterSettingsViewModel.getGridCount();

            // Set up file filter view model
            //noinspection RedundantThrows
            mFilterUpdateDisposable.add(
                    mFilterSettingsComponent.observeListUpdates(new Consumer<FilterSettingsViewModel.FilterState>() {
                        int lastGridCount = gridCount;

                        @Override
                        public void accept(FilterSettingsViewModel.FilterState filterState) throws Exception {
                            if (filterState != null) {

                                // Update the adapter if grid count changed
                                int gridCount = filterState.mGridCount;
                                if (lastGridCount != gridCount) {
                                    updateAdapter(gridCount);
                                    lastGridCount = gridCount;
                                }

                                // Emit load started event to show the progress bar, then subscribe
                                // to a new update file list observable with new filter state
                                FilesViewModel.FileListState onFinishState;
                                FilesViewModel.FileListState onEmptyState;

                                if (Utils.isNullOrEmpty(filterState.searchQuery)) {
                                    onFinishState = FilesViewModel.FileListState.FILTER_FINISHED;
                                    onEmptyState = FilesViewModel.FileListState.FILTER_NO_MATCHES;
                                } else {
                                    onFinishState = FilesViewModel.FileListState.FILTER_FINISHED;
                                    onEmptyState = FilesViewModel.FileListState.SEARCH_NO_MATCHES;
                                }
                                mFilesViewModel.emitStateUpdate(FilesViewModel.FileListState.LOADING_STARTED);
                                updateFileListAsync(filterState, onFinishState, onEmptyState);
                            }
                        }
                    })
            );
        }

        mBinding = FragmentLocalFileViewBinding.inflate(inflater, container, false);
        mToolbar = mBinding.fragmentToolbar;

        handleBackupBanner();

        return mBinding.getRoot();
    }

    @SuppressLint("CheckResult")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Logger.INSTANCE.LogD(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);

        final Context context = getContext();
        // Setup recycler view animations

        mBinding.emptyTextViewForFilter.setBackgroundColor(mTheme.emptyTextBackground);
        mHtmlConversionComponent = getHtmlConversionComponent(view);
        mBinding.fabMenu.setClosedOnTouchOutside(true);

        if (!Utils.isTablet(getActivity()) & mBinding.fabMenu.getLayoutParams() instanceof CoordinatorLayout.LayoutParams) {
            CoordinatorLayout.LayoutParams clp = (CoordinatorLayout.LayoutParams) mBinding.fabMenu.getLayoutParams();
            clp.setBehavior(new MoveUpwardBehaviour());
        }

        FloatingActionButton createPDFButton = mBinding.fabMenu.findViewById(R.id.blank_PDF);
        createPDFButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBinding.fabMenu.close(true);
                AddPageDialogFragment addPageDialogFragment = AddPageDialogFragment.newInstance();
                addPageDialogFragment.setOnCreateNewDocumentListener(new AddPageDialogFragment.OnCreateNewDocumentListener() {
                    @Override
                    public void onCreateNewDocument(PDFDoc doc, String title) {
                        saveCreatedDocument(doc, title);
                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CREATE_NEW,
                                AnalyticsParam.createNewParam(AnalyticsHandlerAdapter.CREATE_NEW_ITEM_BLANK_PDF, AnalyticsHandlerAdapter.SCREEN_ALL_DOCUMENTS));
                    }
                });
                FragmentManager fragmentManager = getFragmentManager();
                if (fragmentManager != null) {
                    addPageDialogFragment.show(fragmentManager, AddPageDialogFragment.ADD_PAGE_DIALOG_TAG);
                }
            }
        });

        FloatingActionButton imagePDFButton = mBinding.fabMenu.findViewById(R.id.image_pdf);
        imagePDFButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBinding.fabMenu.close(true);
                mOutputFileUri = ViewerUtils.openImageIntent(LocalFileViewFragment.this);
            }
        });

        FloatingActionButton officePDFButton = mBinding.fabMenu.findViewById(R.id.office_PDF);
        officePDFButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentActivity activity = getActivity();
                FragmentManager fragmentManager = getFragmentManager();
                if (activity == null || fragmentManager == null) {
                    return;
                }

                mBinding.fabMenu.close(true);
                mAddDocPdfHelper = new AddDocPdfHelper(activity, fragmentManager, new AddDocPdfHelper.AddDocPDFHelperListener() {
                    @Override
                    public void onPDFReturned(String fileAbsolutePath, boolean external) {
                        Activity activity = getActivity();
                        if (activity == null) {
                            return;
                        }

                        if (fileAbsolutePath == null) {
                            Utils.showAlertDialog(activity, R.string.dialog_add_photo_document_filename_error_message, R.string.error);
                            return;
                        }

                        File file = new File(fileAbsolutePath);
                        if (external) {
                            Logger.INSTANCE.LogD(TAG, "external folder selected");
                            if (mCallbacks != null) {
                                mCallbacks.onExternalFileSelected(fileAbsolutePath, "");
                            }
                        } else {
                            FileInfo fileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_FILE, file);
                            addFileToList(fileInfo);
                            Logger.INSTANCE.LogD(TAG, "inside");
                            if (mCallbacks != null) {
                                mCallbacks.onFileSelected(new File(fileAbsolutePath), "");
                            }
                        }
                        if (!external) {
                            CommonToast.showText(context, getString(R.string.dialog_create_new_document_filename_success) + fileAbsolutePath);
                        }
                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CREATE_NEW,
                                AnalyticsParam.createNewParam(AnalyticsHandlerAdapter.CREATE_NEW_ITEM_PDF_FROM_DOCS, AnalyticsHandlerAdapter.SCREEN_ALL_DOCUMENTS));
                    }

                    @Override
                    public void onMultipleFilesSelected(int requestCode, ArrayList<FileInfo> fileInfoList) {
                        handleMultipleFilesSelected(fileInfoList, AnalyticsHandlerAdapter.SCREEN_ALL_DOCUMENTS);
                    }
                });
                mAddDocPdfHelper.pickFileAndCreate();
            }
        });

        // Set up fab for HTML 2 PDF conversion using HTML2PDF, requires KitKat
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        @SuppressLint("InflateParams")
        View btnView = inflater.inflate(R.layout.fab_btn_web_pdf, null);
        FloatingActionButton webpagePDFButton = btnView.findViewById(R.id.webpage_PDF);
        // HTML conversion should not be visible if Android version is less than KitKat
        if (!Utils.isKitKat()) {
            webpagePDFButton.setVisibility(View.GONE);
        }
        webpagePDFButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBinding.fabMenu.close(true);
                convertHtml();
            }
        });
        mBinding.fabMenu.addMenuButton(webpagePDFButton);

        ItemClickHelper itemClickHelper = new ItemClickHelper();
        itemClickHelper.attachToRecyclerView(mBinding.recyclerView);

        mItemSelectionHelper = new ItemSelectionHelper();
        mItemSelectionHelper.attachToRecyclerView(mBinding.recyclerView);
        mItemSelectionHelper.setChoiceMode(ItemSelectionHelper.CHOICE_MODE_MULTIPLE);

        updateAdapter(mFilterSettingsViewModel.getGridCount());
        mBinding.recyclerView.setHasFixedSize(true);
        mBinding.recyclerView.setItemViewCacheSize(20);

        if (Utils.isLollipop()) {
            mBinding.stickyHeader.setElevation(getResources().getDimensionPixelSize(R.dimen.card_elevation));
        }
        mBinding.stickyHeader.setVisibility(View.VISIBLE);
        mBinding.stickyHeader.disable();
        handleStickyHeader();

        if (Utils.hasStoragePermission(context)) {
            // Fetch for updates in the background and update DB accordingly
            updateDatabase();
        }

        handleFabMenuUI();
    }

    private void updateAdapter(@IntRange(from = 0, to = 6) int gridCount) {
        if (gridCount > 0) {
            AllFilesGridAdapter gridAdapter = createAndBindGridAdapter(mBinding.recyclerView, mBinding.stickyHeader, gridCount);
            mAdapter = gridAdapter;
            mAdapterHelper = gridAdapter;
        } else {
            AllFilesListAdapter listAdapter = createAndBindListAdapter(mBinding.recyclerView, mBinding.stickyHeader);
            mAdapter = listAdapter;
            mAdapterHelper = listAdapter;
        }
        // Grab the last opened file position and clear it
        int lastOpenedFilePosition = SettingsManager.getLastOpenedFilePositionInAllDocuments(mBinding.recyclerView.getContext());
        SettingsManager.clearLastOpenedFilePositionInAllDocuments(mBinding.recyclerView.getContext());
        mAdapterHelper.setLastOpenedFilePosition(lastOpenedFilePosition);
        setAdapterListeners(mAdapter);
    }

    private void setAdapterListeners(@NonNull BaseQuickAdapter adapter) {
        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                Object adapterItem = mAdapter.getItem(position);
                if (!(adapterItem instanceof MultiItemEntity)) {
                    return;
                }
                MultiItemEntity item = (MultiItemEntity) adapterItem;

                if (item instanceof FileItem) {
                    final FileItem listItem = (FileItem) item;

                    final FileInfo fileInfo = fileItemToFileInfo(listItem);
                    if (mActionMode == null) {
                        // We are not in CAB mode, we don't want to let the item checked
                        // in this case... We are just opening the document, not selecting it.
                        mItemSelectionHelper.setItemChecked(position, false);
                        // Save the visible position
                        RecyclerView.LayoutManager layoutManager = mBinding.recyclerView.getLayoutManager();
                        if (layoutManager instanceof LinearLayoutManager) {
                            int firstVisibleItemPosition = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
                            SettingsManager.updateLastOpenedFilePositionInAllDocuments(
                                    mBinding.recyclerView.getContext(),
                                    firstVisibleItemPosition
                            );
                        }
                        onFileClicked(fileInfo);
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
            }
        });

        adapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                Object adapterItem = mAdapter.getItem(position);
                if (!(adapterItem instanceof MultiItemEntity)) {
                    return;
                }
                MultiItemEntity item = (MultiItemEntity) adapterItem;
                if (item instanceof FileItem) {
                    onShowFileInfo((FileItem) item);
                }
            }
        });

        adapter.setOnItemLongClickListener(new BaseQuickAdapter.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(BaseQuickAdapter adapter, View view, int position) {
                if (!useSupportActionBar()) {
                    // disable action mode in dialog fragment mode
                    return false;
                }
                Object adapterItem = mAdapter.getItem(position);
                if (!(adapterItem instanceof MultiItemEntity)) {
                    return false;
                }
                MultiItemEntity item = (MultiItemEntity) adapterItem;
                if (!(item instanceof FileItem)) {
                    return false;
                }
                final FileItem listItem = (FileItem) item;
                if (mActionLock || mAdapter.getItemViewType(position) == AllFilesListAdapter.VIEW_TYPE_HEADER) {
                    // locked or Header item - ignore click
                    return false;
                }

                final FileInfo fileInfo = fileItemToFileInfo(listItem);
                closeSearch();
                if (mActionMode == null) {
                    if (inSearchMode()) {
                        clearSearchFocus();
                    }
                    mFileInfoSelectedList.add(fileInfo);
                    mItemSelectionHelper.setItemChecked(position, true);

                    mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(LocalFileViewFragment.this);
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
    }

    private void updateFileListAsync(@NonNull final FilterSettingsViewModel.FilterState filterState,
            @NonNull final FilesViewModel.FileListState onFinishState,
            @NonNull final FilesViewModel.FileListState onEmptyState) {
        // Get query params
        String searchQuery = "%" + filterState.searchQuery + "%";
        FilterSettingsViewModel.SortMode sortMode = filterState.mSortMode;
        List<Integer> filterTypes = new ArrayList<>();
        if (filterState.shouldShowAll) {
            filterTypes.add(Constants.FILE_TYPE_PDF);
            filterTypes.add(Constants.FILE_TYPE_DOC);
            filterTypes.add(Constants.FILE_TYPE_IMAGE);
            filterTypes.add(Constants.FILE_TYPE_TEXT);
        } else {
            if (filterState.shouldFilterPdf) {
                filterTypes.add(Constants.FILE_TYPE_PDF);
            }
            if (filterState.shouldFilterOffice) {
                filterTypes.add(Constants.FILE_TYPE_DOC);
            }
            if (filterState.shouldFilterImage) {
                filterTypes.add(Constants.FILE_TYPE_IMAGE);
            }
            if (filterState.shouldFilterText) {
                filterTypes.add(Constants.FILE_TYPE_TEXT);
            }
        }
        final int gridCount = filterState.mGridCount;
        final Context applicationContext = getContext().getApplicationContext();

        FileDataSource.QueryParams queryParams =
                new FileDataSource.QueryParams(
                        searchQuery,
                        Collections.unmodifiableList(filterTypes),
                        sortMode,
                        gridCount);

        // Stop all current updates
        mFileListUpdaterDisposable.clear();

        //noinspection RedundantThrows
        mFileListUpdaterDisposable.add(
                mFilesViewModel.getFilesFlowable(queryParams)
                        // Here we filter when the database size has changed, in order to not
                        // spam the adapter
                        .filter(new Predicate<List<FileEntity>>() {
                            private int lastSize = -1;

                            @Override
                            public boolean test(List<FileEntity> fileEntities) throws Exception {
                                int size = fileEntities.size();
                                if (lastSize != size) {
                                    lastSize = size;
                                    return true;
                                } else {
                                    return false;
                                }
                            }
                        })
                        .map(new Function<List<FileEntity>, List<FileEntity>>() {
                            @Override
                            public List<FileEntity> apply(List<FileEntity> fileEntities) throws Exception {
                                Iterator<FileEntity> iterator = fileEntities.iterator();

                                while (iterator.hasNext()) {
                                    FileEntity fileEntity = iterator.next();
                                    if (!new File(fileEntity.getFilePath()).exists() || shouldListItemBeFiltered(fileEntity)) {
                                        iterator.remove();
                                    }
                                }
                                return fileEntities;
                            }
                        })
                        .flatMap(new Function<List<FileEntity>, Flowable<List<MultiItemEntity>>>() {
                            @Override
                            public Flowable<List<MultiItemEntity>> apply(List<FileEntity> fileEntities) throws Exception {
                                if (gridCount > 0) { // grid mode
                                    return Flowable.just(fileEntities)
                                            .map(new Function<List<FileEntity>, List<MultiItemEntity>>() {
                                                @Override
                                                public List<MultiItemEntity> apply(List<FileEntity> fileEntities) throws Exception {
                                                    FileEntityMapper mapper = new FileEntityMapper(fileEntities);
                                                    return mapper.fromFileEntitiesToItems();
                                                }
                                            });
                                } else { // list mode
                                    return Flowable.just(fileEntities)
                                            .map(new Function<List<FileEntity>, List<MultiItemEntity>>() {
                                                @Override
                                                public List<MultiItemEntity> apply(List<FileEntity> fileEntities) throws Exception {
                                                    FileEntityMapper mapper = new FileEntityMapper(fileEntities);
                                                    return mapper.fromFileEntitiesToGroups(applicationContext);
                                                }
                                            });
                                }
                            }
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<List<MultiItemEntity>>() {
                            @Override
                            public void accept(List<MultiItemEntity> items) throws Exception {
                                mAdapterHelper.setItems(items);
                                if (items.isEmpty()) {
                                    mFilesViewModel.emitStateUpdate(onEmptyState);
                                } else {
                                    mFilesViewModel.emitStateUpdate(onFinishState);
                                }
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                throw new RuntimeException("Error setting items", throwable);
                            }
                        })
        );
    }

    protected boolean inSearchMode() {
        return false;
    }

    @Override
    protected void showZeroSearchResults() {
        LifecycleUtils.runOnResume(this, () -> {
            super.showZeroSearchResults();
            safeSetEmptyTextContent(R.string.textview_empty_because_no_string_match);
            safeSetEmptyTextVisibility(View.VISIBLE);
            safeHideProgressBar();
            setReloadActionButtonState(false);
            onFilterResultsNoStringMatch();
        });
    }

    @Override
    protected void showNoTypeFilterInSearchResults() {
        LifecycleUtils.runOnResume(this, () -> {
            super.showNoTypeFilterInSearchResults();
            safeSetEmptyTextContent(R.string.textview_empty_because_no_files_of_selected_type);
            safeSetEmptyTextVisibility(View.VISIBLE);
            safeHideProgressBar();
            setReloadActionButtonState(false);
            onFilterResultsNoStringMatch();
        });
    }

    @Override
    protected void showNoTypeFilter() {
        LifecycleUtils.runOnResume(this, () -> {
            super.showNoTypeFilter();
            if (mBinding.emptyTextView.getVisibility() == View.GONE) {
                mBinding.emptyTextView.setVisibility(View.VISIBLE);
            }
            mBinding.emptyTextView.setText(R.string.textview_empty_because_no_files_of_selected_type);
            safeHideProgressBar();
            setReloadActionButtonState(false);
        });
    }

    @Override
    protected void showOriginalEmptyResults() {
        LifecycleUtils.runOnResume(this, () -> {
            super.showOriginalEmptyResults();
            safeSetEmptyTextContent(R.string.textview_empty_file_list);
            safeSetEmptyTextVisibility(View.VISIBLE);
            safeHideProgressBar();
            setReloadActionButtonState(false);
        });
    }

    private void handleRecyclerViewEvents(FilesViewModel.FileListState state) {
        LifecycleUtils.runOnResume(this, () -> {
            if (mBinding.emptyTextView.getVisibility() == View.VISIBLE) {
                mBinding.emptyTextView.setVisibility(View.GONE);
            }
            onResetFilterResultsNoStringMatch();
            switch (state) {
                case LOADING_STARTED: {
                    safeShowProgressBar();
                    mActionLock = true;
                    setReloadActionButtonState(true);
                    break;
                }
                case LOADING_INTERRUPTED:
                case FILTER_FINISHED: {
                    safeSetEmptyTextVisibility(View.GONE);
                    safeHideProgressBar();
                    setReloadActionButtonState(false);
                    mActionLock = false;
                    break;
                }
                case LOADING_FINISHED: {
                    Snackbar.make(mBinding.recyclerView, "File List Updated", Snackbar.LENGTH_LONG).show();
                    mBinding.recyclerView.setVerticalScrollBarEnabled(true);

                    safeSetEmptyTextVisibility(View.GONE);
                    safeHideProgressBar();
                    setReloadActionButtonState(false);
                    mActionLock = false;
                    break;
                }
                case LOADING_ERRORED: {
                    Snackbar.make(mBinding.recyclerView, "File List Failed to Update", Snackbar.LENGTH_LONG).show();
                    mBinding.recyclerView.setVerticalScrollBarEnabled(true);

                    mBinding.emptyTextViewForFilter.setVisibility(View.GONE);
                    safeHideProgressBar();
                    setReloadActionButtonState(false);
                    mActionLock = false;
                    break;
                }
                case SEARCH_NO_MATCHES: {
                    if (mLastActionWasSearch) {
                        showZeroSearchResults();
                    } else {
                        showNoTypeFilterInSearchResults();
                    }
                    break;
                }
                case EMPTY_LIST: {
                    showOriginalEmptyResults();
                    break;
                }
                case FILTER_NO_MATCHES: {
                    if (mLastActionWasSearch) {
                        showZeroSearchResults();
                    } else if (inSearchMode()) {
                        showNoTypeFilterInSearchResults();
                    } else {
                        showNoTypeFilter();
                    }
                    break;
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mHtmlConversionComponent = getHtmlConversionComponent(getView());
        AnalyticsHandlerAdapter.getInstance().sendTimedEvent(AnalyticsHandlerAdapter.EVENT_SCREEN_ALL_DOCUMENTS);
    }

    @Override
    public void onAttach(Context context) {
        Logger.INSTANCE.LogV("LifeCycle", TAG + ".onAttach");
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
        Logger.INSTANCE.LogV("LifeCycle", TAG + ".onDetach");
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
        inflater.inflate(R.menu.fragment_local_file_view, menu);
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

            EditText editText = searchView.findViewById(R.id.search_src_text);
            if (editText != null) {
                // Disable long-click context menu
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
    }

    public void bindFilterView(@Nullable Menu menu) {
        if (mFilterSettingsComponent != null) {
            mFilterSettingsComponent.setMenu(requireContext(), menu);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        Context context = getContext();
        if (context == null || menu == null) {
            return;
        }

        mFilterSettingsViewModel.initializeViews();

        updateGridMenuState(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }

        boolean handled = false;
        if (item.getItemId() == R.id.menu_action_search) {
            finishSearchView();
            handled = true;
        }
        if (item.getItemId() == R.id.menu_action_reload) {
            ThumbnailPathCacheManager.getInstance().cleanupResources(getContext());
            if (DEBUG) Log.d(TAG, "onOptionsItemSelected");
            reloadFileInfoList();
            handled = true;
        }
        if (item.getItemId() == R.id.menu_file_sort_by_name) {
            mFilterSettingsComponent.toggleEvent(FilterSettingsComponent.FilterEvent.SORT_BY_NAME_CLICKED);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_file_sort_by_date) {
            mFilterSettingsComponent.toggleEvent(FilterSettingsComponent.FilterEvent.SORT_BY_DATE_CLICKED);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_action_recently_deleted) {
            showTrashBin();
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_0) {
            mFilterSettingsComponent.updateGridCount(0);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_1) {
            mFilterSettingsComponent.updateGridCount(1);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_2) {
            mFilterSettingsComponent.updateGridCount(2);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_3) {
            mFilterSettingsComponent.updateGridCount(3);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_4) {
            mFilterSettingsComponent.updateGridCount(4);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_5) {
            mFilterSettingsComponent.updateGridCount(5);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_6) {
            mFilterSettingsComponent.updateGridCount(6);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_file_filter_all) {
            mLastActionWasSearch = false;
            mFilterSettingsComponent.toggleEvent(FilterSettingsComponent.FilterEvent.ALL_FILTER_CLICKED);
        }
        if (item.getItemId() == R.id.menu_file_filter_pdf) {
            mLastActionWasSearch = false;
            mFilterSettingsComponent.toggleEvent(FilterSettingsComponent.FilterEvent.PDF_FILTER_CLICKED);
        }
        if (item.getItemId() == R.id.menu_file_filter_docx) {
            mLastActionWasSearch = false;
            mFilterSettingsComponent.toggleEvent(FilterSettingsComponent.FilterEvent.OFFICE_FILTER_CLICKED);
        }
        if (item.getItemId() == R.id.menu_file_filter_image) {
            mLastActionWasSearch = false;
            mFilterSettingsComponent.toggleEvent(FilterSettingsComponent.FilterEvent.IMAGE_FILTER_CLICKED);
        }
        if (item.getItemId() == R.id.menu_file_filter_text) {
            mLastActionWasSearch = false;
            mFilterSettingsComponent.toggleEvent(FilterSettingsComponent.FilterEvent.TEXT_FILTER_CLICKED);
        }
        return handled;
    }

    @Override
    public void onMergeConfirmed(ArrayList<FileInfo> filesToMerge, ArrayList<FileInfo> filesToDelete, String title) {
        mDocumentTitle = title;
        mMergeFileList = filesToMerge;
        mMergeTempFileList = filesToDelete;
        // Launch folder picker
        FilePickerDialogFragment dialogFragment = FilePickerDialogFragment.newInstance(RequestCode.MERGE_FILE_LIST,
                Environment.getExternalStorageDirectory());
        dialogFragment.setLocalFolderListener(LocalFileViewFragment.this);
        dialogFragment.setExternalFolderListener(LocalFileViewFragment.this);
        dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            dialogFragment.show(fragmentManager, "file_picker_dialog_fragment");
        }
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

    public void resetFileListFilter() {
        String filterText = getFilterText();
        if (!Utils.isNullOrEmpty(filterText)) {
            if (mAdapter != null && mFilterSettingsComponent != null) {
                mFilterSettingsComponent.updateSearchString("");
            }
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

    protected void clearFileInfoSelectedList() {
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

    protected void finishSearchView() {
        if (mSearchMenuItem != null && mSearchMenuItem.isActionViewExpanded()) {
            mSearchMenuItem.collapseActionView();
        }
        resetFileListFilter();
    }

    public void reloadFileInfoList() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        mFilesViewModel.onCleared();
        mFilesViewModel.clearFiles(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                mFilesViewModel.updateDatabase(context.getApplicationContext(), new FilesDatabaseUpdater(context.getApplicationContext()));
            }
        });
    }

    private void updateDatabase() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        mFilesViewModel.updateDatabase(context.getApplicationContext(), new FilesDatabaseUpdater(context.getApplicationContext()));
    }

    private void saveCreatedDocument(PDFDoc doc, String title) {
        mCreatedDocumentTitle = title;
        mCreatedDoc = doc;
        // launch folder picker
        FilePickerDialogFragment dialogFragment = FilePickerDialogFragment.newInstance(RequestCode.SELECT_BLANK_DOC_FOLDER, Environment.getExternalStorageDirectory());
        dialogFragment.setLocalFolderListener(LocalFileViewFragment.this);
        dialogFragment.setExternalFolderListener(LocalFileViewFragment.this);
        dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            dialogFragment.show(fragmentManager, "create_document_folder_picker_dialog");
        }
        Logger.INSTANCE.LogD(TAG, "new blank folder");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (Activity.RESULT_OK == resultCode) {
            if (RequestCode.PICK_PHOTO_CAM == requestCode) {
                try {
                    Map imageIntent = ViewerUtils.readImageIntent(data, activity, mOutputFileUri);
                    if (!ViewerUtils.checkImageIntent(imageIntent)) {
                        Utils.handlePdfFromImageFailed(activity, imageIntent);
                        return;
                    }

                    mImageFilePath = ViewerUtils.getImageFilePath(imageIntent);
                    mIsCamera = ViewerUtils.isImageFromCamera(imageIntent);
                    mImageUri = ViewerUtils.getImageUri(imageIntent);

                    // try to get display name
                    mImageFileDisplayName = Utils.getDisplayNameFromImageUri(activity, mImageUri, mImageFilePath);
                    // cannot get a valid filename
                    if (Utils.isNullOrEmpty(mImageFileDisplayName)) {
                        Utils.handlePdfFromImageFailed(activity, imageIntent);
                        return;
                    }

                    // launch folder picker
                    FilePickerDialogFragment dialogFragment = FilePickerDialogFragment.newInstance(
                            RequestCode.SELECT_PHOTO_DOC_FOLDER, Environment.getExternalStorageDirectory());
                    dialogFragment.setLocalFolderListener(LocalFileViewFragment.this);
                    dialogFragment.setExternalFolderListener(LocalFileViewFragment.this);
                    dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
                    FragmentManager fragmentManager = getFragmentManager();
                    if (fragmentManager != null) {
                        dialogFragment.show(fragmentManager, "create_document_folder_picker_dialog");
                    }

                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CREATE_NEW,
                            AnalyticsParam.createNewParam(mIsCamera ? AnalyticsHandlerAdapter.CREATE_NEW_ITEM_PDF_FROM_CAMERA : AnalyticsHandlerAdapter.CREATE_NEW_ITEM_PDF_FROM_IMAGE,
                                    AnalyticsHandlerAdapter.SCREEN_ALL_DOCUMENTS));
                } catch (FileNotFoundException e) {
                    CommonToast.showText(activity, getString(R.string.dialog_add_photo_document_filename_file_error), Toast.LENGTH_SHORT);
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } catch (Exception e) {
                    CommonToast.showText(activity, R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
            }
        }
    }

    @Override
    public boolean onQueryTextChange(String newSearchString) {
        // If search gets cleared revert context back to non-search action
        mLastActionWasSearch = newSearchString.length() > 0;

        // prevent clearing filter text when the fragment is hidden
        if (mAdapter != null && Utils.isNullOrEmpty(mFilterText) && mFilterSettingsComponent != null) {
            mAdapterHelper.cancelAllThumbRequests(true);
            mFilterSettingsComponent.updateSearchString(newSearchString);
        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mLastActionWasSearch = true;
        if (mBinding.recyclerView != null) {
            mBinding.recyclerView.requestFocus();
        }
        return false;
    }

    private void updateFile(@NonNull final FileInfo fileInfo) {
        // No need to remove first, all we need to do is add new files. Any old files
        // will be cleaned up in the database automatically
        mFilesViewModel.add(fileInfo);
    }

    private void deleteFileFromList(FileInfo fileInfo) {
        Logger.INSTANCE.LogD(TAG, "Deleted file from list: " + fileInfo);
        mFilesViewModel.delete(fileInfo);
    }

    private void addFileToList(FileInfo fileInfo) {
        Logger.INSTANCE.LogD(TAG, "Added file from list: " + fileInfo);
        mFilesViewModel.add(fileInfo);
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
        updateFile(newFile);
        handleFileUpdated(oldFile, newFile);
        try {
            PdfViewCtrlTabsManager.getInstance().updatePdfViewCtrlTabInfo(activity,
                    oldFile.getAbsolutePath(), newFile.getAbsolutePath(), newFile.getFileName());
            // update user bookmarks
            BookmarkManager.updateUserBookmarksFilePath(activity, oldFile.getAbsolutePath(), newFile.getAbsolutePath());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        mFileEventLock = false;
    }

    @Override
    public void onFileDuplicated(File fileCopy) {
        finishActionMode();
        hideFileInfoDrawer();
        FileInfo fileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_FILE, fileCopy);
        addFileToList(fileInfo);
        mFileEventLock = false;
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
            // update user bookmarks
            for (FileInfo info : deletedFiles) {
                handleFileRemoved(info);
                BookmarkManager.removeUserBookmarks(activity, info.getAbsolutePath());
                if (mAdapter != null) {
                    mAdapterHelper.evictFromMemoryCache(info.getAbsolutePath());
                }
                deleteFileFromList(info);
            }
            handleFilesRemoved(deletedFiles);
            mFileEventLock = false;
        }
    }

    @Override
    public void onFileTrashed(ArrayList<FileInfo> deletedFiles) {
        onFileDeleted(deletedFiles);
        reloadFileInfoList();
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
        for (Map.Entry<FileInfo, Boolean> entry : filesMoved.entrySet()) {
            // only update if the move operation was successful
            if (entry.getValue()) {
                FileInfo fileInfo = entry.getKey();
                File targetFile = new File(targetFolder, fileInfo.getName());
                FileInfo targetFileInfo = new FileInfo(fileInfo.getType(), targetFile);

                // update recent and favorite lists
                handleFileUpdated(fileInfo, targetFileInfo);
                Utils.safeNotifyDataSetChanged(mAdapter);

                try {
                    // Update tab info
                    PdfViewCtrlTabsManager.getInstance().updatePdfViewCtrlTabInfo(activity,
                            fileInfo.getAbsolutePath(), targetFile.getAbsolutePath(), targetFileInfo.getFileName());
                    // update user bookmarks
                    BookmarkManager.updateUserBookmarksFilePath(activity, fileInfo.getAbsolutePath(), targetFile.getAbsolutePath());
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
                addFileToList(targetFileInfo);
            }
        }
        mFileEventLock = false;
    }

    @Override
    public void onFileMoved(Map<FileInfo, Boolean> filesMoved, ExternalFileInfo targetFolder) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        Logger.INSTANCE.LogD(TAG, "onExternalFileMoved: " + targetFolder.getAbsolutePath());
        finishActionMode();
        hideFileInfoDrawer();
        for (Map.Entry<FileInfo, Boolean> entry : filesMoved.entrySet()) {
            // only update if the move operation was successful
            if (entry.getValue()) {
                FileInfo fileInfo = entry.getKey();
                String targetFilePath = ExternalFileInfo.appendPathComponent(targetFolder.getUri(), fileInfo.getName()).toString();
                FileInfo targetFileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL, targetFilePath,
                        fileInfo.getName(), false, 1);
                // Update recent and favorite lists
                handleFileUpdated(fileInfo, targetFileInfo);
                Utils.safeNotifyDataSetChanged(mAdapter);

                try {
                    // Update tab info
                    PdfViewCtrlTabsManager.getInstance().updatePdfViewCtrlTabInfo(activity,
                            fileInfo.getAbsolutePath(), targetFileInfo.getAbsolutePath(), targetFileInfo.getFileName());
                    // update user bookmarks
                    BookmarkManager.updateUserBookmarksFilePath(activity, fileInfo.getAbsolutePath(), targetFileInfo.getAbsolutePath());
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
                updateDatabase();
            }
        }
        mFileEventLock = false;
    }

    @Override
    public void onFileChanged(final String path, int event) {
        // this method is called from background thread

        synchronized (mFileChangeLock) {
            Logger.INSTANCE.LogD(TAG, "onFileChanged: " + path + "; isValid: " + FileManager.isValidFile(path) + ", mFileEventLock:" + mFileEventLock);
            if (!FileManager.isValidFile(path) || mFileEventLock) {
                return;
            }

            Handler handler = new Handler(Looper.getMainLooper());
            Runnable runnable;

            switch (event) {
                case FileObserver.MOVED_TO:
                case FileObserver.CREATE:
                    // run it in UI thread
                    runnable = new Runnable() {
                        @Override
                        public void run() {
                            File file = new File(path);
                            FileInfo fileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_FILE, file);
                            addFileToList(fileInfo);
                        }
                    };
                    handler.post(runnable);
                    break;
                case FileObserver.MOVED_FROM:
                case FileObserver.DELETE:
                    // run it in UI thread
                    runnable = new Runnable() {
                        @Override
                        public void run() {
                            File file = new File(path);
                            FileInfo fileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_FILE, file);
                            deleteFileFromList(fileInfo);
                        }
                    };
                    handler.post(runnable);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onFolderCreated(FileInfo rootFolder, FileInfo newFolder) {
        mFileEventLock = false;
        updateDatabase();
    }

    @Override
    public void onFileMerged(ArrayList<FileInfo> mergedFiles, ArrayList<FileInfo> filesToDelete, FileInfo newFile) {
        Logger.INSTANCE.LogD(TAG, "onFileMerged");
        finishActionMode();
        hideFileInfoDrawer();
        if (newFile == null) {
            return;
        }

        mFileEventLock = false;
        if (mCallbacks != null) {
            // Open merged file in viewer
            if (newFile.getType() == BaseFileInfo.FILE_TYPE_FILE) {
                addFileToList(newFile);
                mCallbacks.onFileSelected(newFile.getFile(), "");
            } else if (newFile.getType() == BaseFileInfo.FILE_TYPE_EXTERNAL) {
                mCallbacks.onExternalFileSelected(newFile.getAbsolutePath(), "");
            }
        }
        MiscUtils.removeFiles(filesToDelete);
    }

    @Override
    public void onFileClicked(final FileInfo fileInfo) {
        Activity activity = getActivity();
        if (activity == null || fileInfo == null) {
            return;
        }
        final File file = fileInfo.getFile();
        if (file == null) {
            return;
        }

        if (mIsSearchMode) {
            hideSoftKeyboard();
        }
        if (Utils.isLollipop()
                && Utils.isSdCardFile(activity, file)
                && PdfViewCtrlSettingsManager.getShowOpenReadOnlySdCardFileWarning(activity)) {
            LayoutInflater inflater = LayoutInflater.from(activity);
            @SuppressLint("InflateParams")
            View customLayout = inflater.inflate(R.layout.alert_dialog_with_checkbox, null);
            String message = String.format(getString(R.string.dialog_files_go_to_sd_card_description),
                    getString(R.string.app_name),
                    String.format(getString(R.string.dialog_go_to_sd_card_description_more_info), getString(R.string.misc_read_more)));
            final TextView dialogTextView = customLayout.findViewById(R.id.dialog_message);
            dialogTextView.setText(Html.fromHtml(message));
            dialogTextView.setMovementMethod(LinkMovementMethod.getInstance());
            final CheckBox dialogCheckBox = customLayout.findViewById(R.id.dialog_checkbox);
            dialogCheckBox.setChecked(true);

            AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                    .setView(customLayout)
                    .setPositiveButton(R.string.dialog_folder_go_to_sd_card_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Activity activity = getActivity();
                            if (activity == null) {
                                return;
                            }
                            boolean showAgain = !dialogCheckBox.isChecked();
                            PdfViewCtrlSettingsManager.updateShowOpenReadOnlySdCardFileWarning(activity, showAgain);
                            if (null != mJumpNavigationCallbacks) {
                                mJumpNavigationCallbacks.gotoExternalTab();
                            }
                        }
                    }).setNegativeButton(R.string.document_read_only_warning_negative, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Activity activity = getActivity();
                            if (activity == null) {
                                return;
                            }
                            boolean showAgain = !dialogCheckBox.isChecked();
                            PdfViewCtrlSettingsManager.updateShowOpenReadOnlySdCardFileWarning(activity, showAgain);
                            if (file.exists()) {
                                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_OPEN_FILE,
                                        AnalyticsParam.openFileParam(fileInfo, AnalyticsHandlerAdapter.SCREEN_ALL_DOCUMENTS));
                                if (mCallbacks != null) {
                                    mCallbacks.onFileSelected(file, "");
                                }
                            }
                        }
                    })
                    .setCancelable(true);

            final AlertDialog alertDialog = builder.create();
            alertDialog.show();

            // Make the textview clickable. Must be called after show()
            TextView textView = alertDialog.findViewById(android.R.id.message);
            if (textView != null) {
                textView.setMovementMethod(LinkMovementMethod.getInstance());
            }
            return;
        }

        if (file.exists()) {
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_OPEN_FILE,
                    AnalyticsParam.openFileParam(fileInfo, AnalyticsHandlerAdapter.SCREEN_ALL_DOCUMENTS));
            if (mCallbacks != null) {
                mCallbacks.onFileSelected(file, "");
            }
        }
    }

    @Override
    public void onLocalFolderSelected(int requestCode, Object object, final File folder) {
        mFileEventLock = true;
        Logger.INSTANCE.LogD(TAG, "onLocalFolderSelected");
        if (requestCode == RequestCode.MOVE_FILE) {
            if (mSelectedFile != null) {
                FileManager.move(getActivity(), new ArrayList<>(Collections.singletonList(mSelectedFile)), folder, LocalFileViewFragment.this);
            }
        } else if (requestCode == RequestCode.MOVE_FILE_LIST) {
            FileManager.move(getActivity(), mFileInfoSelectedList, folder, LocalFileViewFragment.this);
        } else if (requestCode == RequestCode.SELECT_BLANK_DOC_FOLDER) {
            PDFDoc doc = null;
            String filePath = "";
            try {
                if (mCreatedDocumentTitle == null) {
                    CommonToast.showText(getActivity(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                    return;
                }
                boolean hasExtension = FilenameUtils.isExtension(mCreatedDocumentTitle, "pdf");
                if (!hasExtension) {
                    mCreatedDocumentTitle = mCreatedDocumentTitle + ".pdf";
                }
                File documentFile = new File(folder, mCreatedDocumentTitle);
                filePath = Utils.getFileNameNotInUse(documentFile.getAbsolutePath());
                if (Utils.isNullOrEmpty(filePath)) {
                    CommonToast.showText(getActivity(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                    return;
                }

                documentFile = new File(filePath);

                doc = mCreatedDoc;
                doc.save(filePath, SDFDoc.SaveMode.REMOVE_UNUSED, null);
                String toastMsg = getString(R.string.dialog_create_new_document_filename_success) + filePath;
                CommonToast.showText(getActivity(), toastMsg, Toast.LENGTH_LONG);
                addFileToList(new FileInfo(BaseFileInfo.FILE_TYPE_FILE, documentFile));

                if (mCallbacks != null) {
                    mCallbacks.onFileSelected(documentFile, "");
                }

                finishActionMode();
                Logger.INSTANCE.LogD(TAG, "finisheActionMode");
            } catch (Exception e) {
                CommonToast.showText(getActivity(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                AnalyticsHandlerAdapter.getInstance().sendException(e, filePath);
            } finally {
                Utils.closeQuietly(doc);
            }
            mFileEventLock = false;
        } else if (requestCode == RequestCode.SELECT_PHOTO_DOC_FOLDER) {
            if (Utils.isNullOrEmpty(mImageFileDisplayName)) {
                CommonToast.showText(getActivity(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                return;
            }
            try {
                File documentFile = new File(folder, mImageFileDisplayName + ".pdf");
                documentFile = new File(Utils.getFileNameNotInUse(documentFile.getAbsolutePath()));
                String outputPath = ViewerUtils.imageIntentToPdf(getActivity(), mImageUri, mImageFilePath, documentFile.getAbsolutePath());
                if (outputPath != null) {
                    String toastMsg = getString(R.string.dialog_create_new_document_filename_success) + folder.getPath();
                    CommonToast.showText(getActivity(), toastMsg, Toast.LENGTH_LONG);

                    addFileToList(new FileInfo(BaseFileInfo.FILE_TYPE_FILE, documentFile));

                    if (mCallbacks != null) {
                        mCallbacks.onFileSelected(documentFile, "");
                    }
                }

                finishActionMode();

                mFileEventLock = false;
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
            if (mIsCamera) {
                FileUtils.deleteQuietly(new File(mImageFilePath));
            }
            mFileEventLock = false;
        } else if (requestCode == RequestCode.MERGE_FILE_LIST) {
            boolean hasExtension = FilenameUtils.isExtension(mDocumentTitle, "pdf");
            if (!hasExtension) {
                mDocumentTitle = mDocumentTitle + ".pdf";
            }
            File documentFile = new File(folder, mDocumentTitle);
            String filePath = Utils.getFileNameNotInUse(documentFile.getAbsolutePath());
            if (Utils.isNullOrEmpty(filePath)) {
                CommonToast.showText(getActivity(), R.string.dialog_merge_error_message_general, Toast.LENGTH_SHORT);
                return;
            }
            documentFile = new File(filePath);
            FileInfo newFileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_FILE, documentFile);
            performMerge(newFileInfo);
        }
    }

    @Override
    public void onExternalFolderSelected(int requestCode, Object object, final ExternalFileInfo folder) {
        Logger.INSTANCE.LogD(TAG, "onExternalFolderSelected");

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        mFileEventLock = true;
        if (requestCode == RequestCode.MOVE_FILE) {
            Logger.INSTANCE.LogD(TAG, "MOVE_FILE REQUEST");
            if (mSelectedFile != null) {
                FileManager.move(activity, new ArrayList<>(Collections.singletonList(mSelectedFile)), folder, LocalFileViewFragment.this);
            }
        } else if (requestCode == RequestCode.MOVE_FILE_LIST) {
            Logger.INSTANCE.LogD(TAG, "MOVE_FILE_LIST REQUEST");
            FileManager.move(activity, mFileInfoSelectedList, folder, LocalFileViewFragment.this);
        } else {
            if (requestCode == RequestCode.SELECT_BLANK_DOC_FOLDER) {
                PDFDoc doc = null;
                SecondaryFileFilter filter = null;
                try {
                    if (mCreatedDocumentTitle == null) {
                        CommonToast.showText(activity, R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                        return;
                    }
                    boolean hasExtension = FilenameUtils.isExtension(mCreatedDocumentTitle, "pdf");
                    if (!hasExtension) {
                        mCreatedDocumentTitle = mCreatedDocumentTitle + ".pdf";
                    }
                    String fileName = Utils.getFileNameNotInUse(folder, mCreatedDocumentTitle);
                    if (folder == null || Utils.isNullOrEmpty(fileName)) {
                        CommonToast.showText(activity, R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                        return;
                    }
                    ExternalFileInfo documentFile = folder.createFile("application/pdf", fileName);
                    if (documentFile == null) {
                        return;
                    }

                    doc = mCreatedDoc;

                    Uri uri = documentFile.getUri();
                    if (uri == null) {
                        return;
                    }
                    filter = new SecondaryFileFilter(activity, uri);
                    doc.save(filter, SDFDoc.SaveMode.REMOVE_UNUSED);

                    String toastMsg = getString(R.string.dialog_create_new_document_filename_success)
                            + documentFile.getDocumentPath();
                    CommonToast.showText(activity, toastMsg, Toast.LENGTH_LONG);

                    finishActionMode();

                    if (mCallbacks != null) {
                        mCallbacks.onExternalFileSelected(documentFile.getAbsolutePath(), "");
                    }
                } catch (Exception e) {
                    CommonToast.showText(activity, R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } finally {
                    Utils.closeQuietly(doc, filter);
                }
                mFileEventLock = false;
            } else if (requestCode == RequestCode.SELECT_PHOTO_DOC_FOLDER) {
                String pdfFilePath = Utils.getFileNameNotInUse(folder, mImageFileDisplayName + ".pdf");
                if (folder == null || Utils.isNullOrEmpty(pdfFilePath)) {
                    CommonToast.showText(activity, R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                    return;
                }

                try {
                    ExternalFileInfo documentFile = folder.createFile("application/pdf", pdfFilePath);
                    if (documentFile == null) {
                        return;
                    }

                    String outputPath = ViewerUtils.imageIntentToPdf(activity, mImageUri, mImageFilePath, documentFile);
                    if (outputPath != null) {
                        String toastMsg = getString(R.string.dialog_create_new_document_filename_success)
                                + folder.getAbsolutePath();
                        CommonToast.showText(activity, toastMsg, Toast.LENGTH_LONG);
                        if (mCallbacks != null) {
                            mCallbacks.onExternalFileSelected(documentFile.getAbsolutePath(), "");
                        }
                    }

                    finishActionMode();
                } catch (FileNotFoundException e) {
                    CommonToast.showText(getContext(), getString(R.string.dialog_add_photo_document_filename_file_error), Toast.LENGTH_SHORT);
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } catch (Exception e) {
                    CommonToast.showText(activity, R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } catch (OutOfMemoryError oom) {
                    MiscUtils.manageOOM(getContext());
                    CommonToast.showText(getContext(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                }

                String pdfFilename = Utils.getFileNameNotInUse(mImageFileDisplayName + ".pdf");
                if (Utils.isNullOrEmpty(pdfFilename)) {
                    CommonToast.showText(activity, R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                    return;
                }

                // cleanup the image if it is from camera
                if (mIsCamera) {
                    FileUtils.deleteQuietly(new File(mImageFilePath));
                }
                mFileEventLock = false;
            } else if (requestCode == RequestCode.MERGE_FILE_LIST) {
                boolean hasExtension = FilenameUtils.isExtension(mDocumentTitle, "pdf");
                if (!hasExtension) {
                    mDocumentTitle = mDocumentTitle + ".pdf";
                }
                String fileName = Utils.getFileNameNotInUse(folder, mDocumentTitle);
                if (folder == null || Utils.isNullOrEmpty(fileName)) {
                    CommonToast.showText(activity, R.string.dialog_merge_error_message_general, Toast.LENGTH_SHORT);
                    return;
                }
                final ExternalFileInfo file = folder.createFile("application/pdf", fileName);
                if (file == null) {
                    return;
                }
                FileInfo targetFile = new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL, file.getAbsolutePath(), file.getFileName(), false, 1);
                performMerge(targetFile);
            }
        }
    }

    @Override
    public void onPreLaunchViewer() {
        mViewerLaunching = true;
    }

    @Override
    public void onDataChanged() {
        if (isAdded()) {
            if (DEBUG) Log.d(TAG, "onDataChanged");
            updateDatabase();
        } // otherwise it will be reloaded in resumeFragment
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
        if (mBinding.fabMenu != null && mBinding.fabMenu.isOpened()) {
            // Close fab menu
            mBinding.fabMenu.close(true);
            handled = true;
        } else if (mFileInfoDrawer != null) {
            // Hide file info drawer
            hideFileInfoDrawer();
            handled = true;
        } else if (mActionMode != null) {
            finishActionMode();
            handled = true;
        } else if (mIsSearchMode) {
            finishSearchView();
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

    protected void onShowFileInfo(FileItem fileItem) {
        if (mFileUtilCallbacks != null) {
            mSelectedFile = new FileInfo(BaseFileInfo.FILE_TYPE_FILE, new File(fileItem.filePath));
            mFileInfoDrawer = mFileUtilCallbacks.showFileInfoDrawer(mFileInfoDrawerCallback);
        }
    }

    private void resumeFragment() {
        mViewerLaunching = false;

        if (mLocalFileViewFragmentListener != null) {
            mLocalFileViewFragmentListener.onLocalFileShown();
        }
    }

    private void pauseFragment() {

        if (mIsSearchMode && !mViewerLaunching) {
            finishSearchView();
        }

        safeHideProgressBar();

        if (mAdapter != null) {
            mAdapterHelper.cancelAllThumbRequests(true);
            mAdapterHelper.cleanupResources();
        }
        finishActionMode();

        if (mLocalFileViewFragmentListener != null) {
            mLocalFileViewFragmentListener.onLocalFileHidden();
        }
    }

    public void setLocalFileViewFragmentListener(LocalFileViewFragmentListener listener) {
        mLocalFileViewFragmentListener = listener;
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

    private void safeShowProgressBar() {
        mBinding.progressBarView.setVisibility(View.VISIBLE);
    }

    private void safeHideProgressBar() {
        if (mBinding.progressBarView.getVisibility() == View.VISIBLE) {
            mBinding.progressBarView.setVisibility(View.GONE);
        }
    }

    private void safeSetEmptyTextContent(@StringRes int strRes) {
        mBinding.emptyTextViewForFilter.setText(strRes);
    }

    private void safeSetEmptyTextVisibility(int visibility) {
        if (mBinding.emptyTextViewForFilter.getVisibility() != visibility) {
            mBinding.emptyTextViewForFilter.setVisibility(visibility);
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
        FragmentActivity activity = getActivity();
        if (activity != null) {
            Utils.safeShowAlertDialog(activity,
                    R.string.import_webpage_error_message_title,
                    R.string.create_file_invalid_error_message);
        }
    }

    @NonNull
    private FileInfo fileItemToFileInfo(@NonNull FileItem fileItem) {
        return new FileInfo(BaseFileInfo.FILE_TYPE_FILE, new File(fileItem.filePath));
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
            mHtmlConversionComponent.handleWebpageToPDF(activity);
        }
    }

    protected void moveCurrentFile() {
        // Unused
    }

    protected void onFilterResultsNoStringMatch() {
        // Unused
    }

    protected void onResetFilterResultsNoStringMatch() {
        // Unused
    }

    // Following methods used for branching logic on BackupViewFragment

    protected void setShowInfoButton(AllFilesListAdapter allFilesListAdapter) {
        allFilesListAdapter.setShowInfoButton(true);
    }

    protected void setShowInfoButton(AllFilesGridAdapter allFilesGridAdapter) {
        allFilesGridAdapter.setShowInfoButton(true);
    }

    protected void setStickyHeaderSettings(StickyHeader stickyHeader) {
        stickyHeader.setBackupFolder(false);
    }

    protected void handleBackupBanner() {
        mBinding.backupBanner.setVisibility(View.GONE);
    }

    protected void handleStickyHeader() {
        mBinding.stickyHeader.setBackupFolder(false);
    }

    protected void handleFabMenuUI() {
        mBinding.fabMenu.setVisibility(View.VISIBLE);
    }

    protected boolean shouldListItemBeFiltered(FileEntity fileEntity) {
        Context context = getContext();
        if (context != null) {
            return fileEntity.getFilePath().contains(context.getExternalFilesDir(null).toString());
        }
        return false;
    }
}
