//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.navigation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import android.text.Html;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.github.clans.fab.FloatingActionButton;
import com.pdftron.common.PDFNetException;
import com.pdftron.demo.R;
import com.pdftron.demo.browser.db.trash.TrashEntity;
import com.pdftron.demo.browser.ui.FileBrowserTheme;
import com.pdftron.demo.databinding.EmptyListViewBinding;
import com.pdftron.demo.databinding.FragmentRecentViewBinding;
import com.pdftron.demo.dialog.FilePickerDialogFragment;
import com.pdftron.demo.dialog.MergeDialogFragment;
import com.pdftron.demo.navigation.adapter.BaseFileAdapter;
import com.pdftron.demo.navigation.adapter.RecentAdapter;
import com.pdftron.demo.navigation.callbacks.ExternalFileManagementListener;
import com.pdftron.demo.navigation.callbacks.FileManagementListener;
import com.pdftron.demo.navigation.callbacks.FileUtilCallbacks;
import com.pdftron.demo.navigation.callbacks.MainActivityListener;
import com.pdftron.demo.navigation.component.html2pdf.Html2PdfComponent;
import com.pdftron.demo.navigation.component.html2pdf.HtmlConversionComponent;
import com.pdftron.demo.navigation.viewmodel.FileSelectionViewModel;
import com.pdftron.demo.navigation.viewmodel.FilterMenuViewModel;
import com.pdftron.demo.utils.AddDocPdfHelper;
import com.pdftron.demo.utils.ExternalFileManager;
import com.pdftron.demo.utils.FileListFilter;
import com.pdftron.demo.utils.FileManager;
import com.pdftron.demo.utils.LifecycleUtils;
import com.pdftron.demo.utils.MiscUtils;
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
import com.pdftron.pdf.utils.FileInfoManager;
import com.pdftron.pdf.utils.Logger;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.PdfViewCtrlTabsManager;
import com.pdftron.pdf.utils.RequestCode;
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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class RecentViewFragment extends FileBrowserViewFragment implements
        MainActivityListener,
        BaseFileAdapter.AdapterListener,
        FileManagementListener,
        ExternalFileManagementListener,
        FilePickerDialogFragment.LocalFolderListener,
        FilePickerDialogFragment.ExternalFolderListener,
        MergeDialogFragment.MergeDialogFragmentListener,
        ActionMode.Callback,
        FileInfoDrawer.Callback,
        HtmlConversionComponent.HtmlConversionListener {

    private static final String TAG = RecentViewFragment.class.getName();

    protected static final String ARGS_KEY_SELECTION_MODE = "RecentViewFragment_selection_mode";
    protected static final String ARGS_KEY_MULTI_SELECT = "RecentViewFragment_multi_select";

    protected ArrayList<FileInfo> mFileInfoList = new ArrayList<>();
    protected ArrayList<FileInfo> mFileInfoSelectedList = new ArrayList<>();
    protected FileInfo mSelectedFile;

    private FileUtilCallbacks mFileUtilCallbacks;

    protected RecentAdapter mAdapter;
    private FileInfoDrawer mFileInfoDrawer;
    private Menu mOptionsMenu;
    private boolean mFirstRun;
    private String mDocumentTitle;
    private PDFDoc mCreatedDoc;
    private String mCreatedDocumentTitle;
    private Uri mOutputFileUri;
    private String mImageFilePath;
    private Uri mImageUri;
    private String mImageFileDisplayName;
    private boolean mIsCamera;
    private String mEmptyText;

    protected ArrayList<FileInfo> mMergeFileList;
    protected ArrayList<FileInfo> mMergeTempFileList;

    protected int mSpanCount;
    protected ItemSelectionHelper mItemSelectionHelper;

    private RecentViewFragmentListener mRecentViewFragmentListener;
    private RecentFilesFetcher mRecentFilesFetcher;

    protected MenuItem itemFavorite;
    protected MenuItem itemRemove;
    protected MenuItem itemMerge;
    protected MenuItem itemShare;
    protected MenuItem itemRename;

    private FilterMenuViewModel mFilterViewModel;
    private MenuItem mFilterAll;
    private MenuItem mFilterPdf;
    private MenuItem mFilterDocx;
    private MenuItem mFilterImage;
    private MenuItem mFilterText;

    private HtmlConversionComponent mHtmlConversionComponent;
    private CompositeDisposable mDisposables;

    protected FragmentRecentViewBinding mBinding;
    protected EmptyListViewBinding mEmptyListViewBinding;

    private FileBrowserTheme mTheme;

    // Hold state for Fragment to know what the last action taken was
    boolean mLastActionWasSearch = false;

    // in selection mode, instead of info icon, we will show checkbox for item selection
    protected boolean mSelectionMode = false;
    protected boolean mMultiSelect = false;

    // file selection view model
    private FileSelectionViewModel mFileSelectionViewModel;

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (super.onCreateActionMode(mode, menu)) {
            return true;
        }

        mode.getMenuInflater().inflate(R.menu.cab_fragment_recent_view, menu);

        itemFavorite = menu.findItem(R.id.action_add_to_favorites);
        itemRemove = menu.findItem(R.id.action_recent_list_remove);
        itemMerge = menu.findItem(R.id.cab_file_merge);
        itemShare = menu.findItem(R.id.cab_file_share);
        itemRename = menu.findItem(R.id.cab_file_rename);
        itemRemove.setIcon(null); // Hide icon
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }
        if (itemShare != null) {
            itemShare.setVisible(true);
        }

        if (itemFavorite != null) {
            if (mFileInfoSelectedList.size() > 1) {
                itemFavorite.setVisible(false);
            } else {
                itemFavorite.setVisible(true);
                if (canAddToFavorite(activity, mFileInfoSelectedList.get(0))) {
                    itemFavorite.setTitle(activity.getString(R.string.action_add_to_favorites));
                } else {
                    itemFavorite.setTitle(activity.getString(R.string.action_remove_from_favorites));
                }
            }
        }
        if (itemMerge != null) {
            itemMerge.setVisible(mFileInfoSelectedList.size() >= 1);
        }
        if (itemRename != null) {
            if (mFileInfoSelectedList.size() == 1) {
                if (mFileInfoSelectedList.get(0).getType() == BaseFileInfo.FILE_TYPE_FOLDER ||
                        mFileInfoSelectedList.get(0).getType() == BaseFileInfo.FILE_TYPE_FILE) {
                    boolean isSDCardFile = Utils.isSdCardFile(activity, mFileInfoSelectedList.get(0).getFile());
                    if (isSDCardFile) {
                        itemRename.setVisible(false);
                    } else {
                        itemRename.setVisible(true);
                    }
                } else if (mFileInfoSelectedList.get(0).getType() == BaseFileInfo.FILE_TYPE_EXTERNAL ||
                        mFileInfoSelectedList.get(0).getType() == BaseFileInfo.FILE_TYPE_EXTERNAL_FOLDER) {
                    itemRename.setVisible(true);
                } else {
                    itemRename.setVisible(false);
                }
            } else {
                itemRename.setVisible(false);
            }
        }
        mode.setTitle(Utils.getLocaleDigits(Integer.toString(mFileInfoSelectedList.size())));
        // Ensure items are always shown

        if (itemFavorite != null) {
            itemFavorite.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        if (itemRemove != null) {
            itemRemove.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        if (itemRename != null) {
            itemRename.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

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

        if (item.getItemId() == R.id.cab_file_rename) {
            if (mFileInfoSelectedList.get(0).getType() == BaseFileInfo.FILE_TYPE_FILE ||
                    mFileInfoSelectedList.get(0).getType() == BaseFileInfo.FILE_TYPE_FOLDER) {
                FileManager.rename(activity, mFileInfoSelectedList.get(0).getFile(), RecentViewFragment.this);
            } else if (mFileInfoSelectedList.get(0).getType() == BaseFileInfo.FILE_TYPE_EXTERNAL ||
                    mFileInfoSelectedList.get(0).getType() == BaseFileInfo.FILE_TYPE_EXTERNAL_FOLDER) {
                ExternalFileInfo externalFileInfo = Utils.buildExternalFile(activity,
                        Uri.parse(mFileInfoSelectedList.get(0).getAbsolutePath()));
                ExternalFileManager.rename(activity, externalFileInfo, RecentViewFragment.this);
            }
        }
        if (item.getItemId() == R.id.action_add_to_favorites) {
            if (canAddToFavorite(activity, mFileInfoSelectedList.get(0))) {
                getFavoriteFilesManager().addFile(activity, mFileInfoSelectedList.get(0));
                CommonToast.showText(activity,
                        getString(R.string.file_added_to_favorites, mFileInfoSelectedList.get(0).getName()),
                        Toast.LENGTH_SHORT);
            } else {
                getFavoriteFilesManager().removeFile(activity, mFileInfoSelectedList.get(0));
                CommonToast.showText(activity,
                        getString(R.string.file_removed_from_favorites, mFileInfoSelectedList.get(0).getName()),
                        Toast.LENGTH_SHORT);
            }
            finishActionMode();
            // Update favorite file indicators
            Utils.safeNotifyDataSetChanged(mAdapter);
        }
        if (item.getItemId() == R.id.action_recent_list_remove) {
            getRecentFilesManager().removeFiles(activity, mFileInfoSelectedList);
            finishActionMode();
            reloadFileInfoList();
        }
        if (item.getItemId() == R.id.cab_file_merge) {
            // Create and show file merge dialog-fragment
            MergeDialogFragment mergeDialog = getMergeDialogFragment(mFileInfoSelectedList, AnalyticsHandlerAdapter.SCREEN_RECENT);
            mergeDialog.initParams(RecentViewFragment.this);
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null) {
                mergeDialog.show(fragmentManager, "merge_dialog");
            }
            return true;
        }
        if (item.getItemId() == R.id.cab_file_share) {
            if (mFileInfoSelectedList.size() > 1) {
                ArrayList<Uri> files = new ArrayList<>();
                for (FileInfo file : mFileInfoSelectedList) {
                    int fileType = file.getType();
                    if (fileType == BaseFileInfo.FILE_TYPE_FILE) {
                        Uri uri = Utils.getUriForFile(activity, file.getFile());
                        if (uri != null) {
                            files.add(uri);
                        }
                    } else if (fileType == BaseFileInfo.FILE_TYPE_EXTERNAL
                            || fileType == BaseFileInfo.FILE_TYPE_EDIT_URI
                            || fileType == BaseFileInfo.FILE_TYPE_OFFICE_URI) {
                        files.add(Uri.parse(file.getAbsolutePath()));
                    }
                }
                if (mOnPdfFileSharedListener != null) {
                    Intent intent = Utils.createGenericShareIntents(activity, files);
                    mOnPdfFileSharedListener.onPdfFileShared(intent);
                    finishActionMode();
                } else {
                    Utils.shareGenericFiles(activity, files);
                }
            } else if (mFileInfoSelectedList.size() == 1) {
                FileInfo selectedFile = mFileInfoSelectedList.get(0);
                int selectedFileType = selectedFile.getType();
                if (selectedFileType == BaseFileInfo.FILE_TYPE_FILE) {
                    if (mOnPdfFileSharedListener != null) {
                        File sharedFile = new File(selectedFile.getAbsolutePath());
                        Intent intent = Utils.createShareIntent(activity, sharedFile);
                        mOnPdfFileSharedListener.onPdfFileShared(intent);
                        finishActionMode();
                    } else {
                        Utils.sharePdfFile(activity, new File(selectedFile.getAbsolutePath()));
                    }
                } else if (selectedFileType == BaseFileInfo.FILE_TYPE_EXTERNAL
                        || selectedFileType == BaseFileInfo.FILE_TYPE_EDIT_URI
                        || selectedFileType == BaseFileInfo.FILE_TYPE_OFFICE_URI) {
                    if (mOnPdfFileSharedListener != null) {
                        Intent intent = Utils.createGenericShareIntent(activity, Uri.parse(selectedFile.getAbsolutePath()));
                        mOnPdfFileSharedListener.onPdfFileShared(intent);
                        finishActionMode();
                    } else {
                        Utils.shareGenericFile(activity, Uri.parse(selectedFile.getAbsolutePath()));
                    }
                }
            }
        }

        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        super.onDestroyActionMode(mode);
        mActionMode = null;
        clearFileInfoSelectedList();
    }

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

        if (mFileInfoDrawerHelper.mImageViewReference == null ||
                (mFileInfoDrawerHelper.mImageViewReference.get() != null &&
                        !mFileInfoDrawerHelper.mImageViewReference.get().equals(imageView))) {
            mFileInfoDrawerHelper.mImageViewReference = new WeakReference<>(imageView);
        }

        if (mSelectedFile == null) {
            return;
        }

        // Setup thumbnail worker, if required
        if (mFileInfoDrawerHelper.mThumbnailWorker == null) {
            Point dimensions = drawer.getDimensions();
            mFileInfoDrawerHelper.mThumbnailWorker = new ThumbnailWorker(activity, dimensions.x, dimensions.y, null);
            mFileInfoDrawerHelper.mThumbnailWorker.setListener(mThumbnailWorkerListener);
        }

        drawer.setIsSecured(mSelectedFile.isSecured());

        switch (mSelectedFile.getType()) {
            case BaseFileInfo.FILE_TYPE_FILE:
                if (mSelectedFile.isSecured() || mSelectedFile.isPackage()) {
                    int errorRes = Utils.getResourceDrawable(activity, getResources().getString(R.string.thumb_error_res_name));
                    imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    imageView.setImageResource(errorRes);
                } else {
                    imageView.setScaleType(ImageView.ScaleType.MATRIX);
                    mFileInfoDrawerHelper.mThumbnailWorker.tryLoadImageWithPath(0, mSelectedFile.getAbsolutePath(), null, imageView);
                }
                break;
            case BaseFileInfo.FILE_TYPE_FOLDER:
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER); // Adjust scale type for folder
                imageView.setImageResource(R.drawable.ic_folder_large);
                int folderColorRes = BaseFileAdapter.getFolderIconColor(activity);
                imageView.getDrawable().mutate().setColorFilter(folderColorRes, PorterDuff.Mode.SRC_IN);
                break;
            case BaseFileInfo.FILE_TYPE_EXTERNAL:
                if (mFileInfoDrawerHelper.getExternalFile() != null && mFileInfoDrawerHelper.getExternalFile().isDirectory()) {
                    imageView.setScaleType(ImageView.ScaleType.FIT_CENTER); // Adjust scale type for folder
                    imageView.setImageResource(R.drawable.ic_folder_large);
                    folderColorRes = BaseFileAdapter.getFolderIconColor(activity);
                    imageView.getDrawable().mutate().setColorFilter(folderColorRes, PorterDuff.Mode.SRC_IN);
                } else if (mSelectedFile.isSecured() || mSelectedFile.isPackage()) {
                    int errorRes = Utils.getResourceDrawable(activity, getResources().getString(R.string.thumb_error_res_name));
                    imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    imageView.setImageResource(errorRes);
                } else {
                    imageView.setScaleType(ImageView.ScaleType.MATRIX);
                    mFileInfoDrawerHelper.mThumbnailWorker.tryLoadImageWithUuid(0,
                            mSelectedFile.getFileName(), mSelectedFile.getIdentifier(),
                            null, imageView);
                }
                break;
            default:
                int errorRes = Utils.getResourceDrawable(activity, getResources().getString(R.string.thumb_error_res_name));
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                imageView.setImageResource(errorRes);
                break;
        }
    }

    @Override
    public boolean onPrepareIsSecured(FileInfoDrawer drawer) {
        return mSelectedFile != null && mSelectedFile.isSecured();
    }

    @Override
    public CharSequence onPrepareMainContent(FileInfoDrawer drawer) {
        return mFileInfoDrawerHelper.getFileInfoTextBody();
    }

    @Override
    public boolean onCreateDrawerMenu(FileInfoDrawer drawer, Menu menu) {
        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }
        activity.getMenuInflater().inflate(R.menu.cab_fragment_recent_view, menu);

        mFileInfoDrawerHelper.itemFavorite = menu.findItem(R.id.action_add_to_favorites);
        mFileInfoDrawerHelper.itemRemove = menu.findItem(R.id.action_recent_list_remove);
        mFileInfoDrawerHelper.itemShare = menu.findItem(R.id.cab_file_share);
        mFileInfoDrawerHelper.itemRename = menu.findItem(R.id.cab_file_rename);

        return true;
    }

    @Override
    public boolean onPrepareDrawerMenu(FileInfoDrawer drawer, Menu menu) {
        Activity activity = getActivity();
        if (activity == null || mSelectedFile == null) {
            return false;
        }
        if (mFileInfoDrawerHelper.itemFavorite != null) {
            if (canAddToFavorite(activity, mSelectedFile)) {
                mFileInfoDrawerHelper.itemFavorite.setTitle(activity.getString(R.string.action_add_to_favorites));
                mFileInfoDrawerHelper.itemFavorite.setTitleCondensed(activity.getString(R.string.action_favorite));
                mFileInfoDrawerHelper.itemFavorite.setIcon(R.drawable.ic_star_white_24dp);
            } else {
                mFileInfoDrawerHelper.itemFavorite.setTitle(activity.getString(R.string.action_remove_from_favorites));
                mFileInfoDrawerHelper.itemFavorite.setTitleCondensed(activity.getString(R.string.action_unfavorite));
                mFileInfoDrawerHelper.itemFavorite.setIcon(R.drawable.ic_star_filled_white_24dp);
            }
        }
        int selectedFileType = mSelectedFile.getType();
        if (mFileInfoDrawerHelper.itemShare != null) {
            if (selectedFileType == BaseFileInfo.FILE_TYPE_FILE
                    || selectedFileType == BaseFileInfo.FILE_TYPE_EXTERNAL
                    || selectedFileType == BaseFileInfo.FILE_TYPE_EDIT_URI
                    || selectedFileType == BaseFileInfo.FILE_TYPE_OFFICE_URI) {
                mFileInfoDrawerHelper.itemShare.setVisible(true);
            } else {
                mFileInfoDrawerHelper.itemShare.setVisible(false);
            }
        }
        if (mFileInfoDrawerHelper.itemRename != null) {
            if (selectedFileType == BaseFileInfo.FILE_TYPE_FOLDER ||
                    selectedFileType == BaseFileInfo.FILE_TYPE_FILE) {
                boolean isSDCardFile = Utils.isSdCardFile(activity, mSelectedFile.getFile());
                if (isSDCardFile) {
                    mFileInfoDrawerHelper.itemRename.setVisible(false);
                } else {
                    mFileInfoDrawerHelper.itemRename.setVisible(true);
                }
            } else if (selectedFileType == BaseFileInfo.FILE_TYPE_EXTERNAL ||
                    selectedFileType == BaseFileInfo.FILE_TYPE_EXTERNAL_FOLDER) {
                mFileInfoDrawerHelper.itemRename.setVisible(true);
            } else {
                mFileInfoDrawerHelper.itemRename.setVisible(false);
            }
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
            renameFile(activity, mSelectedFile);
        }
        if (menuItem.getItemId() == R.id.action_add_to_favorites) {
            favoriteFile(activity, mSelectedFile);
            drawer.invalidate();
        }
        if (menuItem.getItemId() == R.id.action_recent_list_remove) {
            removeFile(activity, mSelectedFile);
        }
        if (menuItem.getItemId() == R.id.cab_file_merge) {
            // Create and show file merge dialog-fragment
            MergeDialogFragment mergeDialog = getMergeDialogFragment(new ArrayList<>(Collections.singletonList(mSelectedFile)), AnalyticsHandlerAdapter.SCREEN_RECENT);
            mergeDialog.initParams(RecentViewFragment.this);
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null) {
                mergeDialog.show(fragmentManager, "merge_dialog");
            }
        }
        if (menuItem.getItemId() == R.id.cab_file_share) {
            shareFile(activity, mSelectedFile);
        }

        return true;
    }

    protected void renameFile(Context context, FileInfo selectedFile) {
        int selectedFileType = selectedFile.getType();
        if (selectedFileType == BaseFileInfo.FILE_TYPE_FILE ||
                selectedFileType == BaseFileInfo.FILE_TYPE_FOLDER) {
            FileManager.rename(context, selectedFile.getFile(), RecentViewFragment.this);
        } else if (selectedFileType == BaseFileInfo.FILE_TYPE_EXTERNAL ||
                selectedFileType == BaseFileInfo.FILE_TYPE_EXTERNAL_FOLDER) {
            ExternalFileInfo externalFileInfo = Utils.buildExternalFile(getContext(),
                    Uri.parse(selectedFile.getAbsolutePath()));
            ExternalFileManager.rename(context, externalFileInfo, RecentViewFragment.this);
        }
    }

    protected void shareFile(Activity activity, FileInfo selectedFile) {
        int selectedFileType = selectedFile.getType();
        if (selectedFileType == BaseFileInfo.FILE_TYPE_FILE) {
            if (mOnPdfFileSharedListener != null) {
                Intent intent = Utils.createShareIntent(activity, new File(selectedFile.getAbsolutePath()));
                mOnPdfFileSharedListener.onPdfFileShared(intent);
            } else {
                Utils.sharePdfFile(activity, new File(selectedFile.getAbsolutePath()));
            }
        } else if (selectedFileType == BaseFileInfo.FILE_TYPE_EXTERNAL
                || selectedFileType == BaseFileInfo.FILE_TYPE_EDIT_URI
                || selectedFileType == BaseFileInfo.FILE_TYPE_OFFICE_URI) {
            if (mOnPdfFileSharedListener != null) {
                Intent intent = Utils.createGenericShareIntent(activity, Uri.parse(selectedFile.getAbsolutePath()));
                mOnPdfFileSharedListener.onPdfFileShared(intent);
            } else {
                Utils.shareGenericFile(activity, Uri.parse(selectedFile.getAbsolutePath()));
            }
        }
    }

    protected void removeFile(Context context, FileInfo selectedFile) {
        getRecentFilesManager().removeFile(context, selectedFile);
        reloadFileInfoList();
        hideFileInfoDrawer();
    }

    protected void favoriteFile(Context context, FileInfo selectedFile) {
        if (canAddToFavorite(context, selectedFile)) {
            getFavoriteFilesManager().addFile(context, selectedFile);
            CommonToast.showText(context,
                    getString(R.string.file_added_to_favorites, selectedFile.getName()),
                    Toast.LENGTH_SHORT);
        } else {
            getFavoriteFilesManager().removeFile(context, selectedFile);
            CommonToast.showText(context,
                    getString(R.string.file_removed_from_favorites, selectedFile.getName()),
                    Toast.LENGTH_SHORT);
        }
        // Update favorite file indicators
        Utils.safeNotifyDataSetChanged(mAdapter);
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
        mFileInfoDrawerHelper.cancelAllThumbRequests();

        mSelectedFile = null;
        mFileInfoDrawer = null;

        onHideFileInfoDrawer();
    }

    public interface RecentViewFragmentListener {

        void onRecentShown();

        void onRecentHidden();
    }

    public static RecentViewFragment newInstance() {
        return new RecentViewFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logger.INSTANCE.LogV("LifeCycle", TAG + ".onCreate");

        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mSelectionMode = getArguments().getBoolean(ARGS_KEY_SELECTION_MODE, false);
            mMultiSelect = getArguments().getBoolean(ARGS_KEY_MULTI_SELECT, false);
        }

        setRetainInstance(true);
        setHasOptionsMenu(true);
        FileManager.initCache(getContext());
        mFirstRun = true;

        mTheme = FileBrowserTheme.fromContext(requireActivity());

        mFilterViewModel = ViewModelProviders.of(this).get(FilterMenuViewModel.class);
        mRecentFilesFetcher = getRecentFilesFetcher();
        mDisposables = new CompositeDisposable();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = FragmentRecentViewBinding.inflate(inflater, container, false);
        mEmptyListViewBinding = mBinding.emptyView;
        mToolbar = mBinding.fragmentToolbar;
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        mFileSelectionViewModel = ViewModelProviders.of(activity).get(FileSelectionViewModel.class);
        mFileSelectionViewModel.observe(getViewLifecycleOwner(), new Observer<ArrayList<FileInfo>>() {
            @Override
            public void onChanged(ArrayList<FileInfo> fileInfos) {
                addSelectedFilesAndReload(fileInfos);
            }
        });

        mHtmlConversionComponent = getHtmlConversionComponent(view);
        mBinding.emptyTextViewForFilter.setBackgroundColor(mTheme.emptyTextBackground);

        // if we are here after PopulateFileInfoListTask is complete
        if (mEmptyText != null) {
            mEmptyListViewBinding.body.setText(mEmptyText);
            mEmptyListViewBinding.container.setVisibility(View.VISIBLE);
        } else {
            mEmptyListViewBinding.container.setVisibility(View.GONE);
        }
        // since there are not many files to process, no need to show "loading files ..."

        mBinding.fabMenu.setClosedOnTouchOutside(true);

        if (mBinding.fabMenu.getLayoutParams() instanceof CoordinatorLayout.LayoutParams) {
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
                                AnalyticsParam.createNewParam(AnalyticsHandlerAdapter.CREATE_NEW_ITEM_BLANK_PDF, AnalyticsHandlerAdapter.SCREEN_RECENT));
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
                mOutputFileUri = ViewerUtils.openImageIntent(RecentViewFragment.this);
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
                        if (fileAbsolutePath == null) {
                            Utils.showAlertDialog(getActivity(), R.string.dialog_add_photo_document_filename_error_message, R.string.error);
                            return;
                        }

                        File file = new File(fileAbsolutePath);
                        if (external) {
                            Logger.INSTANCE.LogD(TAG, "external folder selected");
                            mFirstRun = true;
                            if (mCallbacks != null) {
                                mCallbacks.onExternalFileSelected(fileAbsolutePath, "");
                            }
                        } else {
                            FileInfo newFileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_FILE, file);
                            new FileManager.ChangeCacheFileTask(new ArrayList<FileInfo>(), newFileInfo, mCacheLock).execute();
                            if (mCallbacks != null) {
                                mCallbacks.onFileSelected(new File(fileAbsolutePath), "");
                            }
                        }
                        if (!external) {
                            CommonToast.showText(getContext(), getString(R.string.dialog_create_new_document_filename_success) + fileAbsolutePath);
                        }
                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CREATE_NEW,
                                AnalyticsParam.createNewParam(AnalyticsHandlerAdapter.CREATE_NEW_ITEM_PDF_FROM_DOCS, AnalyticsHandlerAdapter.SCREEN_RECENT));
                    }

                    @Override
                    public void onMultipleFilesSelected(int requestCode, ArrayList<FileInfo> fileInfoList) {
                        handleMultipleFilesSelected(fileInfoList, AnalyticsHandlerAdapter.SCREEN_RECENT);
                    }
                });
                mAddDocPdfHelper.pickFileAndCreate();
            }
        });

        // Set up fab for HTML 2 PDF conversion using HTML2PDF, requires KitKat
        LayoutInflater inflater = LayoutInflater.from(getActivity());
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

        mSpanCount = PdfViewCtrlSettingsManager.getGridSize(getActivity(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_RECENT_FILES);
        mBinding.recyclerView.initView(mSpanCount);

        ItemClickHelper itemClickHelper = new ItemClickHelper();
        itemClickHelper.attachToRecyclerView(mBinding.recyclerView);

        mItemSelectionHelper = new ItemSelectionHelper();
        mItemSelectionHelper.attachToRecyclerView(mBinding.recyclerView);
        mItemSelectionHelper.setChoiceMode(ItemSelectionHelper.CHOICE_MODE_MULTIPLE);

        mAdapter = getAdapter();

        mBinding.recyclerView.setAdapter(mAdapter);

        try {
            mBinding.recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if (mBinding.recyclerView == null) {
                                return;
                            }
                            try {
                                mBinding.recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            } catch (Exception ignored) {
                            }
                            if (mAdapter == null) {
                                return;
                            }
                            int viewWidth = mBinding.recyclerView.getMeasuredWidth();
                            mAdapter.updateMainViewWidth(viewWidth);
                            mAdapter.getDerivedFilter().setFileTypeEnabledInFilterFromSettings(mBinding.recyclerView.getContext(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_RECENT_FILES);
                            updateFileListFilter();
                        }
                    });
        } catch (Exception ignored) {
        }

        itemClickHelper.setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView parent, View view, int position, long id) {
                final FileInfo fileInfo = mAdapter.getItem(position);
                if (fileInfo == null) {
                    return;
                }

                if (mSelectionMode) {
                    int oldPosition = -1;
                    if (!mMultiSelect) {
                        if (!mAdapter.mFileInfoSelectedList.isEmpty()) {
                            FileInfo oldSelectedFileInfo = mAdapter.mFileInfoSelectedList.get(0);
                            oldPosition = mAdapter.getIndexOf(oldSelectedFileInfo);
                        }
                        mAdapter.mFileInfoSelectedList.clear();
                    }
                    if (mAdapter.mFileInfoSelectedList.contains(fileInfo)) {
                        mAdapter.mFileInfoSelectedList.remove(fileInfo);
                    } else {
                        mAdapter.mFileInfoSelectedList.add(fileInfo);
                    }
                    updateSelectedViewModel(mAdapter.mFileInfoSelectedList);
                    if (oldPosition != -1) {
                        mAdapter.notifyItemChanged(oldPosition);
                    }
                    mAdapter.notifyItemChanged(position);
                } else {
                    if (mActionMode == null) {
                        mItemSelectionHelper.setItemChecked(position, false);
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

        itemClickHelper.setOnItemLongClickListener(new ItemClickHelper.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(RecyclerView parent, View view, int position, long id) {
                FileInfo fileInfo = mAdapter.getItem(position);
                AppCompatActivity activity = (AppCompatActivity) getActivity();
                if (fileInfo == null || activity == null) {
                    return false;
                }

                if (!useSupportActionBar()) {
                    // disable action mode in dialog fragment mode
                    return false;
                }

                if (mActionMode == null) {
                    if (inSearchMode()) {
                        clearSearchFocus();
                    }
                    mFileInfoSelectedList.add(fileInfo);
                    mItemSelectionHelper.setItemChecked(position, true);

                    mActionMode = activity.startSupportActionMode(RecentViewFragment.this);
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

    protected RecentFilesFetcher getRecentFilesFetcher() {
        return new RecentFilesFetcher(getContext(), getRecentFilesManager());
    }

    @Override
    public void onStart() {
        super.onStart();
        mHtmlConversionComponent = getHtmlConversionComponent(getView());
        AnalyticsHandlerAdapter.getInstance().sendTimedEvent(AnalyticsHandlerAdapter.EVENT_SCREEN_RECENT);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mFileUtilCallbacks = (FileUtilCallbacks) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + e.getClass().toString());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFileUtilCallbacks = null;
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
    }

    public void cleanupResources() {
        if (mAdapter != null) {
            mAdapter.cancelAllThumbRequests(true);
            mAdapter.cleanupResources();
        }
    }

    @Override
    public void onStop() {
        AnalyticsHandlerAdapter.getInstance().endTimedEvent(AnalyticsHandlerAdapter.EVENT_SCREEN_RECENT);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        Logger.INSTANCE.LogD(TAG, "onDestroyView");
        super.onDestroyView();

        updateSelectedViewModel(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanupResources();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        MiscUtils.handleLowMemory(getContext(), mAdapter);
        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_LOW_MEMORY, AnalyticsParam.lowMemoryParam(TAG));
        Logger.INSTANCE.LogE(TAG, "low memory");
    }

    protected void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
            mActionMode = null;
            clearFileInfoSelectedList();
        }
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
    }

    protected void onShowFileInfoDrawer() {

    }

    protected void onHideFileInfoDrawer() {

    }

    protected void onClickAction() {
        hideFileInfoDrawer();
    }

    private void reloadFileInfoList() {
        mDisposables.clear(); // dispose of previous fetches
        //noinspection RedundantThrows
        final CancellationSignal cancellationSignal = new CancellationSignal();
        mDisposables.add(
                mRecentFilesFetcher.getRecentFilesOnce(cancellationSignal)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe(new Consumer<Disposable>() {
                            @Override
                            public void accept(Disposable disposable) throws Exception {
                                if (mFirstRun) {
                                    // We already started with the progress bar visible, so no need
                                    // to show it again.
                                    mFirstRun = false;
                                } else {
                                    if (mBinding.progressBarView != null) {
                                        mBinding.progressBarView.setVisibility(View.VISIBLE);
                                    }
                                }
                            }
                        })
                        .doOnDispose(new Action() {
                            @Override
                            public void run() throws Exception {
                                if (null != mBinding.progressBarView) {
                                    mBinding.progressBarView.setVisibility(View.GONE);
                                }
                                cancellationSignal.cancel();
                            }
                        })
                        .subscribe(new Consumer<List<FileInfo>>() {
                            @Override
                            public void accept(List<FileInfo> fileInfos) throws Exception {
                                if (mAdapter == null || getContext() == null) {
                                    return;
                                }

                                synchronized (mFileListLock) {
                                    mFileInfoList.clear();
                                    mFileInfoList.addAll(fileInfos);
                                }
                                if (fileInfos.isEmpty()) {
                                    mEmptyText = getString(R.string.recent_empty_view);
                                    mEmptyListViewBinding.heading.setText(R.string.textview_empty_recent_list);
                                    mEmptyListViewBinding.body.setText(mEmptyText);
                                    mEmptyListViewBinding.body.setTextColor(mTheme.contentSecondaryTextColor);
                                    mEmptyListViewBinding.container.setVisibility(View.VISIBLE);
                                } else {
                                    mEmptyListViewBinding.container.setVisibility(View.GONE);
                                }

                                if (null != mBinding.progressBarView) {
                                    mBinding.progressBarView.setVisibility(View.GONE);
                                }

                                updateFileListFilter();
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                AnalyticsHandlerAdapter.getInstance().sendException(new RuntimeException(throwable));
                            }
                        })
        );
    }

    public void deselectAll() {
        mAdapter.mFileInfoSelectedList.clear();
        Utils.safeNotifyDataSetChanged(mAdapter);
        updateSelectedViewModel(mAdapter.mFileInfoSelectedList);
    }

    private void addSelectedFilesAndReload(@Nullable ArrayList<FileInfo> files) {
        if (files == null) {
            return;
        }
        for (FileInfo fileInfo : files) {
            if (!mAdapter.mFileInfoSelectedList.contains(fileInfo)) {
                mAdapter.mFileInfoSelectedList.add(fileInfo);
            }
        }
        onDataChanged();
    }

    public void deselectedFiles(ArrayList<FileInfo> files) {
        for (FileInfo file : files) {
            int index = mAdapter.getIndexOf(file);
            mAdapter.mFileInfoSelectedList.remove(file);
            if (index != -1) {
                mAdapter.notifyItemChanged(index);
            }
        }
        updateSelectedViewModel(mAdapter.mFileInfoSelectedList);
    }

    private void updateSelectedViewModel(ArrayList<FileInfo> files) {
        if (mFileSelectionViewModel != null) {
            mFileSelectionViewModel.setSelectedFiles(files);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        MiscUtils.updateAdapterViewWidthAfterGlobalLayout(mBinding.recyclerView, mAdapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isAdded()) {
            return;
        }

        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_recent_view, menu);
        inflater.inflate(R.menu.menu_addon_file_type_filter, menu);
        inflater.inflate(R.menu.menu_addon_recently_deleted_files, menu);

        bindFilterViewModel(menu);
    }

    public void bindFilterViewModel(Menu menu) {
        mOptionsMenu = menu;

        // Clear the submenu header for Filter menu item
        MenuItem fileMenu = menu.findItem(R.id.menu_file_filter);
        Context context = getContext();
        if (fileMenu != null && context != null) {
            fileMenu.getSubMenu().clearHeader();
            mFilterAll = menu.findItem(R.id.menu_file_filter_all);
            mFilterPdf = menu.findItem(R.id.menu_file_filter_pdf);
            mFilterDocx = menu.findItem(R.id.menu_file_filter_docx);
            mFilterImage = menu.findItem(R.id.menu_file_filter_image);
            mFilterText = menu.findItem(R.id.menu_file_filter_text);
            if (Utils.isUsingDocumentTree()) {
                mFilterText.setVisible(false);
            }
            ViewerUtils.keepOnScreenAfterClick(context, mFilterAll);
            ViewerUtils.keepOnScreenAfterClick(context, mFilterPdf);
            ViewerUtils.keepOnScreenAfterClick(context, mFilterDocx);
            ViewerUtils.keepOnScreenAfterClick(context, mFilterImage);
            ViewerUtils.keepOnScreenAfterClick(context, mFilterText);

            // Set up file filter menu view model
            mFilterViewModel.initialize(PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_RECENT_FILES,
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
                                    mFilterText.setChecked(isChecked);
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
        // Set grid size radio buttons to correct value from settings
        int gridSize = PdfViewCtrlSettingsManager.getGridSize(context, PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_RECENT_FILES);
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
        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }

        boolean handled = false;
        if (item.getItemId() == R.id.action_clear_recent_list) {
            if (mAdapter != null && mAdapter.getItemCount() > 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setMessage(R.string.dialog_clear_recent_list_message)
                        .setTitle(R.string.dialog_clear_recent_list_title)
                        .setCancelable(true)
                        .setPositiveButton(R.string.clear, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Activity activity = getActivity();
                                if (activity == null) {
                                    return;
                                }
                                getRecentFilesManager().clearFiles(activity);
                                reloadFileInfoList();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .create().show();
            }
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

    protected RecentAdapter getAdapter() {
        return new RecentAdapter(getActivity(), mFileInfoList, mFileListLock,
                mSpanCount, this, mItemSelectionHelper);
    }

    protected boolean canAddToFavorite(Context context, FileInfo selectedFile) {
        return !getFavoriteFilesManager().containsFile(context, selectedFile);
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

    public String getFilterText() {
        // Currently only implemented under XodoFavoritesViewFragment, there is no search UI to bind in this standalone view
        return "";
    }

    protected void updateFileListFilter() {
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

    public void updateSpanCount(int count) {
        if (mSpanCount != count) {
            PdfViewCtrlSettingsManager.updateGridSize(getContext(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_RECENT_FILES, count);
        }
        mSpanCount = count;
        updateGridMenuState(mOptionsMenu);
        mBinding.recyclerView.updateSpanCount(count);
    }

    @Override
    public void onExternalFileRenamed(ExternalFileInfo oldFile, ExternalFileInfo newFile) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        finishActionMode();
        hideFileInfoDrawer();

        FileInfo oldFileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL, oldFile.getUri().toString(),
                oldFile.getFileName(), false, 1);
        FileInfo newFileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL, newFile.getUri().toString(),
                newFile.getFileName(), false, 1);
        getRecentFilesManager().updateFile(activity, oldFileInfo, newFileInfo);
        getFavoriteFilesManager().updateFile(activity, oldFileInfo, newFileInfo);
        try {
            PdfViewCtrlTabsManager.getInstance().updatePdfViewCtrlTabInfo(activity,
                    oldFile.getAbsolutePath(), newFile.getAbsolutePath(), newFile.getFileName());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        reloadFileInfoList();
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

    }

    @Override
    public void onExternalTrashesLoaded(List<TrashEntity> trashEntityList) {

    }

    @Override
    public void onRootsRemoved(ArrayList<ExternalFileInfo> deletedFiles) {

    }

    @Override
    public void onExternalFileMoved(Map<ExternalFileInfo, Boolean> filesMoved, ExternalFileInfo targetFolder) {

    }

    @Override
    public void onExternalFileMoved(Map<ExternalFileInfo, Boolean> filesMoved, File targetFolder) {

    }

    @Override
    public void onExternalFolderCreated(ExternalFileInfo rootFolder, ExternalFileInfo newFolder) {

    }

    @Override
    public void onExternalFileMerged(ArrayList<FileInfo> mergedFiles, ArrayList<FileInfo> filesToDelete, FileInfo newFile) {

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
        getRecentFilesManager().updateFile(activity, oldFile, newFile);
        getFavoriteFilesManager().updateFile(activity, oldFile, newFile);
        try {
            PdfViewCtrlTabsManager.getInstance().updatePdfViewCtrlTabInfo(activity,
                    oldFile.getAbsolutePath(), newFile.getAbsolutePath(), newFile.getFileName());
            // update user bookmarks
            BookmarkManager.updateUserBookmarksFilePath(activity, oldFile.getAbsolutePath(), newFile.getAbsolutePath());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        Utils.safeNotifyDataSetChanged(mAdapter);

        reloadFileInfoList();

        // delete old file in cache, add new file to cache
        new FileManager.ChangeCacheFileTask(oldFile, newFile, mCacheLock).execute();
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

    }

    @Override
    public void onTrashesLoaded(List<TrashEntity> trashEntityList) {

    }

    @Override
    public void onFileMoved(Map<FileInfo, Boolean> filesMoved, File targetFolder) {

    }

    @Override
    public void onFileMoved(Map<FileInfo, Boolean> filesMoved, ExternalFileInfo targetFolder) {

    }

    @Override
    public void onFolderCreated(FileInfo rootFolder, FileInfo newFolder) {

    }

    @Override
    public void onFileMerged(ArrayList<FileInfo> mergedFiles, ArrayList<FileInfo> filesToDelete, FileInfo newFile) {

    }

    @Override
    public void onFileChanged(String path, int event) {

    }

    @Override
    public void onFileClicked(FileInfo fileInfo) {
        switch (fileInfo.getType()) {
            case BaseFileInfo.FILE_TYPE_FILE:
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_OPEN_FILE,
                        AnalyticsParam.openFileParam(fileInfo, AnalyticsHandlerAdapter.SCREEN_RECENT));
                if (mCallbacks != null) {
                    mCallbacks.onFileSelected(fileInfo.getFile(), "");
                }
                break;
            case BaseFileInfo.FILE_TYPE_EXTERNAL:
                if (!Utils.isNullOrEmpty(fileInfo.getAbsolutePath())) {
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_OPEN_FILE,
                            AnalyticsParam.openFileParam(fileInfo, AnalyticsHandlerAdapter.SCREEN_RECENT));
                    if (mCallbacks != null) {
                        mCallbacks.onExternalFileSelected(fileInfo.getAbsolutePath(), "");
                    }
                }
                break;
            case BaseFileInfo.FILE_TYPE_EDIT_URI:
                if (!Utils.isNullOrEmpty(fileInfo.getAbsolutePath())) {
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_OPEN_FILE,
                            AnalyticsParam.openFileParam(fileInfo, AnalyticsHandlerAdapter.SCREEN_RECENT));
                    if (mCallbacks != null) {
                        mCallbacks.onEditUriSelected(fileInfo.getAbsolutePath());
                    }
                }
                break;
            case BaseFileInfo.FILE_TYPE_OFFICE_URI:
                if (!Utils.isNullOrEmpty(fileInfo.getAbsolutePath())) {
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_OPEN_FILE,
                            AnalyticsParam.openFileParam(fileInfo, AnalyticsHandlerAdapter.SCREEN_RECENT));
                    if (mCallbacks != null) {
                        mCallbacks.onOfficeUriSelected(Uri.parse(fileInfo.getAbsolutePath()));
                    }
                }
                break;
        }
    }

    protected static class RecentFilesFetcher {

        final Context mContext;
        final FileInfoManager mRecentFilesManager;

        protected RecentFilesFetcher(Context context, FileInfoManager recentFilesManager) {
            mContext = context;
            mRecentFilesManager = recentFilesManager;
        }

        private Single<List<FileInfo>> getRecentFilesOnce(@NonNull final CancellationSignal cancellationSignal) {
            return Single.fromCallable(new Callable<List<FileInfo>>() {
                @Override
                public List<FileInfo> call() throws Exception {
                    return getRecentFiles(cancellationSignal);
                }
            });
        }

        protected boolean fileExists(@NonNull FileInfo fileInfo, @NonNull CancellationSignal cancellationSignal) {
            boolean fileExists = true;
            if (fileInfo.getFile() != null) {
                // Check if the file exists
                try {
                    fileExists = fileInfo.getFile().exists();
                } catch (Exception ignored) {
                }
            }
            // for URI files, we want to check the actual uri permission instead
            if (!fileExists) {
                if (fileInfo.getType() == BaseFileInfo.FILE_TYPE_EXTERNAL ||
                        fileInfo.getType() == BaseFileInfo.FILE_TYPE_EDIT_URI ||
                        fileInfo.getType() == BaseFileInfo.FILE_TYPE_OFFICE_URI) {
                    // Check if the external file exists
                    Uri uri = Uri.parse(fileInfo.getAbsolutePath());
                    if (Utils.isKitKat()) {
                        fileExists = Utils.uriHasReadPermission(mContext, uri, cancellationSignal);
                    } else {
                        fileExists = Utils.uriHasReadPermission(mContext, uri);
                    }
                }
            }
            return fileExists;
        }

        @NonNull
        private List<FileInfo> getRecentFiles(@NonNull CancellationSignal cancellationSignal) {
            Utils.throwIfOnMainThread();
            List<FileInfo> filesToRemove = new ArrayList<>();
            List<FileInfo> fileInfoList = new ArrayList<>(mRecentFilesManager.getFiles(mContext));

            for (FileInfo fileInfo : fileInfoList) {
                if (fileInfo != null) {
                    boolean fileExists = fileExists(fileInfo, cancellationSignal);
                    if (!fileExists) { // Flag file for removal if it does not exist
                        filesToRemove.add(fileInfo);
                    }
                }
            }
            // Commit any changes to the manager
            if (filesToRemove.size() > 0) {
                mRecentFilesManager.removeFiles(mContext, filesToRemove);
                fileInfoList.removeAll(filesToRemove);
            }

            return fileInfoList;
        }
    }

    @Override
    public void onLocalFolderSelected(int requestCode, Object object, File folder) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (requestCode == RequestCode.SELECT_BLANK_DOC_FOLDER) {
            PDFDoc doc = null;
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
                String filePath = Utils.getFileNameNotInUse(documentFile.getAbsolutePath());
                if (Utils.isNullOrEmpty(filePath)) {
                    CommonToast.showText(getActivity(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                    return;
                }

                documentFile = new File(filePath);

                doc = mCreatedDoc;
                doc.save(documentFile.getAbsolutePath(), SDFDoc.SaveMode.REMOVE_UNUSED, null);
                String toastMsg = getString(R.string.dialog_create_new_document_filename_success) + filePath;
                CommonToast.showText(getActivity(), toastMsg, Toast.LENGTH_LONG);

                FileInfo newFileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_FILE, documentFile);
                new FileManager.ChangeCacheFileTask(new ArrayList<FileInfo>(), newFileInfo, mCacheLock).execute();

                if (mCallbacks != null) {
                    mCallbacks.onFileSelected(documentFile, "");
                }

                finishActionMode();
            } catch (Exception e) {
                CommonToast.showText(getActivity(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                Utils.closeQuietly(doc);
            }
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

                    FileInfo newFileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_FILE, documentFile);
                    new FileManager.ChangeCacheFileTask(new ArrayList<FileInfo>(), newFileInfo, mCacheLock).execute();

                    if (mCallbacks != null) {
                        mCallbacks.onFileSelected(documentFile, "");
                    }
                }

                finishActionMode();
            } catch (FileNotFoundException e) {
                CommonToast.showText(activity, getString(R.string.dialog_add_photo_document_filename_file_error), Toast.LENGTH_SHORT);
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } catch (Exception e) {
                CommonToast.showText(getActivity(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } catch (OutOfMemoryError oom) {
                com.pdftron.demo.utils.MiscUtils.manageOOM(activity);
                CommonToast.showText(activity, R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
            }

            // cleanup the image if it is from camera
            if (mIsCamera) {
                FileUtils.deleteQuietly(new File(mImageFilePath));
            }
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
            new FileManager.ChangeCacheFileTask(new ArrayList<FileInfo>(), newFileInfo, mCacheLock).execute();
            performMerge(newFileInfo);
        }
    }

    protected void performMerge(FileInfo newFileInfo) {
        FileManager.merge(getActivity(), mMergeFileList, mMergeTempFileList, newFileInfo, mFileManagementListener);
    }

    @Override
    public void onExternalFolderSelected(int requestCode, Object object, ExternalFileInfo folder) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

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
                CommonToast.showText(activity, getString(R.string.dialog_add_photo_document_filename_file_error), Toast.LENGTH_SHORT);
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } catch (Exception e) {
                CommonToast.showText(activity, R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } catch (OutOfMemoryError oom) {
                com.pdftron.demo.utils.MiscUtils.manageOOM(activity);
                CommonToast.showText(activity, R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
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
        } else if (requestCode == RequestCode.MERGE_FILE_LIST) {
            boolean hasExtension = FilenameUtils.isExtension(mDocumentTitle, "pdf");
            if (!hasExtension) {
                mDocumentTitle = mDocumentTitle + ".pdf";
            }
            String fileName = Utils.getFileNameNotInUse(folder, mDocumentTitle);
            if (folder == null || Utils.isNullOrEmpty(fileName)) {
                CommonToast.showText(getActivity(), R.string.dialog_merge_error_message_general, Toast.LENGTH_SHORT);
                return;
            }
            final ExternalFileInfo file = folder.createFile("application/pdf", fileName);
            if (file == null) {
                return;
            }
            FileInfo targetFile = new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL, file.getAbsolutePath(), file.getFileName(), false, 1);
            new FileManager.ChangeCacheFileTask(new ArrayList<FileInfo>(), targetFile, mCacheLock).execute();
            performMerge(targetFile);
        }
    }

    @Override
    public void onMergeConfirmed(ArrayList<FileInfo> filesToMerge, ArrayList<FileInfo> filesToDelete, String title) {
        mDocumentTitle = title;
        mMergeFileList = filesToMerge;
        mMergeTempFileList = filesToDelete;
        // Launch folder picker
        FilePickerDialogFragment dialogFragment = FilePickerDialogFragment.newInstance(RequestCode.MERGE_FILE_LIST,
                R.string.dialog_merge_save_location, Environment.getExternalStorageDirectory());
        dialogFragment.setLocalFolderListener(RecentViewFragment.this);
        dialogFragment.setExternalFolderListener(RecentViewFragment.this);
        dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            dialogFragment.show(fragmentManager, "file_picker_dialog_fragment");
        }
    }

    @Override
    public void onPreLaunchViewer() {

    }

    @Override
    public void onDataChanged() {
        if (isAdded()) {
            reloadFileInfoList();
        } // otherwise it will be reloaded in resumeFragment
    }

    @Override
    public void onProcessNewIntent() {
        finishActionMode();
    }

    @Override
    public void onDrawerOpened() {
        finishActionMode();
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
        if (mFileInfoDrawer != null) {
            // Hide file info drawer
            hideFileInfoDrawer();
            handled = true;
        } else if (mActionMode != null) {
            finishActionMode();
            handled = true;
        }
        return handled;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public void onShowFileInfo(int position) {
        if (mFileUtilCallbacks != null) {
            mSelectedFile = mAdapter.getItem(position);
            mFileInfoDrawer = mFileUtilCallbacks.showFileInfoDrawer(this);
        }
    }

    private void resumeFragment() {
        reloadFileInfoList();

        if (mRecentViewFragmentListener != null) {
            mRecentViewFragmentListener.onRecentShown();
        }
        updateSpanCount(mSpanCount);
    }

    private void pauseFragment() {
        mDisposables.clear();

        if (mAdapter != null) {
            mAdapter.cancelAllThumbRequests(true);
            mAdapter.cleanupResources();
        }

        if (mRecentViewFragmentListener != null) {
            mRecentViewFragmentListener.onRecentHidden();
        }
    }

    protected FileManagementListener mFileManagementListener = new FileManagementListener() {
        @Override
        public void onFileRenamed(FileInfo oldFile, FileInfo newFile) {

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

        }

        @Override
        public void onTrashesLoaded(List<TrashEntity> trashEntityList) {

        }

        @Override
        public void onFileMoved(Map<FileInfo, Boolean> filesMoved, File targetFolder) {

        }

        @Override
        public void onFileMoved(Map<FileInfo, Boolean> filesMoved, ExternalFileInfo targetFolder) {

        }

        @Override
        public void onFolderCreated(FileInfo rootFolder, FileInfo newFolder) {

        }

        @Override
        public void onFileMerged(ArrayList<FileInfo> mergedFiles, ArrayList<FileInfo> filesToDelete, FileInfo newFile) {
            finishActionMode();
            hideFileInfoDrawer();
            if (mCallbacks != null && newFile != null) {
                // Open merged file in viewer
                if (newFile.getType() == BaseFileInfo.FILE_TYPE_FILE) {
                    mCallbacks.onFileSelected(newFile.getFile(), "");
                } else if (newFile.getType() == BaseFileInfo.FILE_TYPE_EXTERNAL) {
                    mCallbacks.onExternalFileSelected(newFile.getAbsolutePath(), "");
                }
            }
            MiscUtils.removeFiles(filesToDelete);
        }

        @Override
        public void onFileChanged(String path, int event) {

        }

        @Override
        public void onFileClicked(FileInfo fileInfo) {

        }
    };

    protected FileInfoDrawerHelper mFileInfoDrawerHelper = new FileInfoDrawerHelper();

    protected class FileInfoDrawerHelper {
        MenuItem itemFavorite;
        MenuItem itemRemove;
        MenuItem itemShare;
        MenuItem itemRename;

        ExternalFileInfo externalFileInfo;

        int mPageCount;
        String mAuthor;
        String mTitle;
        String mProducer;
        String mCreator;

        public ThumbnailWorker mThumbnailWorker;
        WeakReference<ImageViewTopCrop> mImageViewReference;

        private void cancelAllThumbRequests() {
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

            PDFDoc doc = null;
            SecondaryFileFilter filter = null;
            try {
                switch (mSelectedFile.getType()) {
                    case BaseFileInfo.FILE_TYPE_FILE:
                        doc = new PDFDoc(mSelectedFile.getAbsolutePath());
                        doc.initSecurityHandler();
                        loadDocInfo(doc);
                        break;
                    case BaseFileInfo.FILE_TYPE_EXTERNAL:
                        if (getExternalFile() != null && !getExternalFile().isDirectory()) {
                            try {
                                filter = new SecondaryFileFilter(getActivity(), getExternalFile().getUri());
                                doc = new PDFDoc(filter);
                                doc.initSecurityHandler();
                                loadDocInfo(doc);
                            } catch (Exception e) {
                                mPageCount = -1;
                                mAuthor = null;
                                mTitle = null;
                                mCreator = null;
                                mProducer = null;
                            }
                        } else {
                            mPageCount = -1;
                            mAuthor = null;
                            mTitle = null;
                            mProducer = null;
                            mCreator = null;
                        }
                        break;
                    default:
                        mPageCount = -1;
                        mAuthor = null;
                        mTitle = null;
                        mCreator = null;
                        mProducer = null;
                        break;
                }
            } catch (PDFNetException e) {
                mPageCount = -1;
                mAuthor = null;
                mTitle = null;
                mCreator = null;
                mProducer = null;
            } finally {
                Utils.closeQuietly(doc, filter);
            }

            if (mSelectedFile.getType() == BaseFileInfo.FILE_TYPE_FILE || (mSelectedFile.getType() == BaseFileInfo.FILE_TYPE_EXTERNAL && getExternalFile() != null &&
                    !getExternalFile().isDirectory())) {
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
            }

            switch (mSelectedFile.getType()) {
                case BaseFileInfo.FILE_TYPE_FILE:
                    if (textBodyBuilder.length() > 0) {
                        textBodyBuilder.append("<br>");
                    }
                    // Parent Directory
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
                case BaseFileInfo.FILE_TYPE_EXTERNAL:
                    if (getExternalFile() != null) {
                        if (!getExternalFile().isDirectory()) {
                            try {
                                // Directory
                                textBodyBuilder.append(res.getString(R.string.file_info_document_path, getExternalFile().getParentRelativePath() + "/" + getExternalFile().getFileName()));
                                textBodyBuilder.append("<br>");
                            } catch (NullPointerException ignored) {

                            }
                            // Size info
                            textBodyBuilder.append(res.getString(R.string.file_info_document_size, getExternalFile().getSizeInfo()));
                            textBodyBuilder.append("<br>");
                            // Date modified
                            textBodyBuilder.append(res.getString(R.string.file_info_document_date_modified,
                                    DateFormat.getInstance().format(new Date(getExternalFile().getRawModifiedDate()))));
                            textBodyBuilder.append("<br>");

                            //Producer
                            textBodyBuilder.append(res.getString(R.string.file_info_document_producer,
                                    Utils.isNullOrEmpty(mProducer) ? res.getString(R.string.file_info_document_attr_not_available) : mProducer));
                            textBodyBuilder.append("<br>");

                            //Creator
                            textBodyBuilder.append(res.getString(R.string.file_info_document_creator,
                                    Utils.isNullOrEmpty(mCreator) ? res.getString(R.string.file_info_document_attr_not_available) : mCreator));
                            textBodyBuilder.append("<br>");
                        } else {
                            if (textBodyBuilder.length() > 0) {
                                textBodyBuilder.append("<br>");
                            }
                            try {
                                // Directory
                                textBodyBuilder.append(res.getString(R.string.file_info_document_path, getExternalFile().getParentRelativePath() + "/" + getExternalFile().getFileName()));
                                textBodyBuilder.append("<br>");
                            } catch (NullPointerException ignored) {

                            }
                            // Size info: x files, y folders
                            int[] externalFileFolderCount = getExternalFile().getFileCount();
                            String externalSizeInfo = res.getString(R.string.dialog_folder_info_size, externalFileFolderCount[0], externalFileFolderCount[1]);
                            textBodyBuilder.append(res.getString(R.string.file_info_document_folder_contains, externalSizeInfo));
                            textBodyBuilder.append("<br>");
                            // Date modified
                            textBodyBuilder.append(res.getString(R.string.file_info_document_date_modified,
                                    DateFormat.getInstance().format(new Date(getExternalFile().getRawModifiedDate()))));
                        }
                    }
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
                mCreator = null;
                mProducer = null;
            } finally {
                if (shouldUnlockRead) {
                    Utils.unlockReadQuietly(doc);
                }
            }
        }

        public ExternalFileInfo getExternalFile() {
            if (externalFileInfo == null && mSelectedFile != null) {
                externalFileInfo = new ExternalFileInfo(getActivity());
                externalFileInfo.setUri(Uri.parse(mSelectedFile.getAbsolutePath()));
                externalFileInfo.initFields();
            }
            return externalFileInfo;
        }
    }

    ThumbnailWorker.ThumbnailWorkerListener mThumbnailWorkerListener = new ThumbnailWorker.ThumbnailWorkerListener() {
        @Override
        public void onThumbnailReady(int result, int position, String iconPath, String identifier) {
            ImageViewTopCrop imageView = (mFileInfoDrawerHelper.mImageViewReference != null) ?
                    mFileInfoDrawerHelper.mImageViewReference.get() : null;
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
            if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_NOT_FOUNT &&
                    mSelectedFile.getType() == BaseFileInfo.FILE_TYPE_EXTERNAL) {
                // create this file instead
                mFileInfoDrawerHelper.mThumbnailWorker.tryLoadImageFromFilter(position, mSelectedFile.getIdentifier(), mSelectedFile.getAbsolutePath());
                return;
            }

            if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_SECURITY_ERROR || result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_PACKAGE_ERROR) {
                // Thumbnail has been generated before, and a placeholder icon should be used
                int errorRes = Utils.getResourceDrawable(getContext(), getResources().getString(R.string.thumb_error_res_name));
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageView.setImageResource(errorRes);
            } else if (mFileInfoDrawerHelper.mThumbnailWorker != null) {
                // adds path to local cache for later access
                ThumbnailPathCacheManager.getInstance().putThumbnailPath(mSelectedFile.getAbsolutePath(),
                        iconPath, mFileInfoDrawerHelper.mThumbnailWorker.getMinXSize(),
                        mFileInfoDrawerHelper.mThumbnailWorker.getMinYSize());

                imageView.setScaleType(ImageView.ScaleType.MATRIX);
                mFileInfoDrawerHelper.mThumbnailWorker.tryLoadImageWithPath(position, mSelectedFile.getAbsolutePath(), iconPath, imageView);
            }
        }
    };

    @Override
    protected void resetFilterResultsViews() {
        mEmptyListViewBinding.container.setVisibility(View.GONE);
        mEmptyListViewBinding.heading.setVisibility(View.GONE);
        mBinding.emptyTextViewForFilter.setVisibility(View.GONE);
    }

    @Override
    protected void showZeroSearchResults() {
        super.showZeroSearchResults();
        mBinding.emptyTextViewForFilter.setText(R.string.textview_empty_because_no_string_match);
        mBinding.emptyTextViewForFilter.setVisibility(View.VISIBLE);
        mBinding.progressBarView.setVisibility(View.GONE);
    }

    @Override
    protected void showNoTypeFilterInSearchResults() {
        super.showNoTypeFilterInSearchResults();
        mBinding.emptyTextViewForFilter.setText(R.string.textview_empty_because_no_files_of_selected_type);
        mBinding.emptyTextViewForFilter.setVisibility(View.VISIBLE);
        mBinding.progressBarView.setVisibility(View.GONE);
    }

    @Override
    protected void showNoTypeFilter() {
        super.showNoTypeFilter();
        mEmptyListViewBinding.container.setVisibility(View.VISIBLE);
        mEmptyListViewBinding.body.setText(R.string.textview_empty_because_no_files_of_selected_type);
    }

    @Override
    protected void showOriginalEmptyResults() {
        LifecycleUtils.runOnResume(this, () -> {
            if (mBinding.progressBarView.getVisibility() != View.VISIBLE) {
                super.showOriginalEmptyResults();
                mEmptyListViewBinding.container.setVisibility(View.VISIBLE);
                mEmptyListViewBinding.heading.setVisibility(View.VISIBLE);
                mEmptyListViewBinding.heading.setText(R.string.textview_empty_recent_list);
                mEmptyListViewBinding.body.setText(R.string.recent_empty_view);
            } else {
                mBinding.emptyTextViewForFilter.setVisibility(View.VISIBLE);
                mBinding.emptyTextViewForFilter.setText(R.string.loading_files_wait);
            }
        });
    }

    @Override
    public void onFilterResultsPublished(int resultCode) {
        resetFilterResultsViews();
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
            case FileListFilter.FILTER_RESULT_EMPTY_ORIGINAL_LIST:
                showOriginalEmptyResults();
            default:
                break;
        }
    }

    public void setRecentViewFragmentListener(RecentViewFragmentListener listener) {
        mRecentViewFragmentListener = listener;
    }

    public void saveCreatedDocument(PDFDoc doc, String title) {
        mCreatedDocumentTitle = title;
        mCreatedDoc = doc;
        // launch folder picker
        FilePickerDialogFragment dialogFragment = FilePickerDialogFragment.newInstance(RequestCode.SELECT_BLANK_DOC_FOLDER, Environment.getExternalStorageDirectory());
        dialogFragment.setLocalFolderListener(RecentViewFragment.this);
        dialogFragment.setExternalFolderListener(RecentViewFragment.this);
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

                    mIsCamera = ViewerUtils.isImageFromCamera(imageIntent);
                    mImageFilePath = ViewerUtils.getImageFilePath(imageIntent);
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
                    dialogFragment.setLocalFolderListener(this);
                    dialogFragment.setExternalFolderListener(this);
                    dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
                    FragmentManager fragmentManager = getFragmentManager();
                    if (fragmentManager != null) {
                        dialogFragment.show(fragmentManager, "create_document_folder_picker_dialog");
                    }

                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CREATE_NEW,
                            AnalyticsParam.createNewParam(mIsCamera ? AnalyticsHandlerAdapter.CREATE_NEW_ITEM_PDF_FROM_CAMERA : AnalyticsHandlerAdapter.CREATE_NEW_ITEM_PDF_FROM_IMAGE,
                                    AnalyticsHandlerAdapter.SCREEN_RECENT));
                } catch (FileNotFoundException e) {
                    CommonToast.showText(getContext(), getString(R.string.dialog_add_photo_document_filename_file_error), Toast.LENGTH_SHORT);
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } catch (Exception e) {
                    CommonToast.showText(getActivity(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
            }
        }
    }

    @Override
    public void onConversionFinished(String path, boolean isLocal) {
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
            mHtmlConversionComponent.handleWebpageToPDF(getActivity());
        }
    }

    @Override
    public boolean onQueryTextChange(String newSearchString) {
        // If search gets cleared revert context back to non-search action
        mLastActionWasSearch = newSearchString.length() > 0;

        // prevent clearing filter text when the fragment is hidden
        if (mAdapter != null) {
            mAdapter.cancelAllThumbRequests(true);
            mAdapter.getFilter().filter(newSearchString);
            boolean isEmpty = Utils.isNullOrEmpty(newSearchString);
            mAdapter.setInSearchMode(!isEmpty);
        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mLastActionWasSearch = true;
        mBinding.recyclerView.requestFocus();
        return false;
    }
}
