//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.navigation.adapter;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.common.RecentlyUsedCache;
import com.pdftron.demo.R;
import com.pdftron.demo.browser.ui.FileBrowserTheme;
import com.pdftron.demo.navigation.adapter.viewholder.ContentViewHolder;
import com.pdftron.demo.navigation.adapter.viewholder.FooterViewHolder;
import com.pdftron.demo.navigation.adapter.viewholder.HeaderViewHolder;
import com.pdftron.demo.utils.FileListFilter;
import com.pdftron.demo.utils.ThumbnailPathCacheManager;
import com.pdftron.demo.utils.ThumbnailWorker;
import com.pdftron.pdf.PreviewHandler;
import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.model.ExternalFileInfo;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.recyclerview.ItemTouchHelperCallback;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerViewAdapter;
import com.pdftron.pdf.widget.recyclerview.ViewHolderBindListener;

import org.apache.commons.io.FilenameUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public abstract class BaseFileAdapter<FileInfo extends BaseFileInfo> extends SimpleRecyclerViewAdapter<FileInfo, RecyclerView.ViewHolder> implements
        Filterable,
        ThumbnailWorker.ThumbnailWorkerListener,
        FileListFilter.FilterPublishListener<FileInfo> {

    private static final String TAG = BaseFileAdapter.class.getName();

    public static final int VIEW_TYPE_HEADER = ItemTouchHelperCallback.VIEW_TYPE_HEADER;
    public static final int VIEW_TYPE_CONTENT = ItemTouchHelperCallback.VIEW_TYPE_CONTENT;
    public static final int VIEW_TYPE_FOOTER = ItemTouchHelperCallback.VIEW_TYPE_FOOTER;

    private WeakReference<Context> mContext;
    protected List<FileInfo> mFiles;
    private ArrayList<FileInfo> mOriginalFiles;
    private FileListFilter mFilter;
    private final Object mOriginalFilesLock;

    protected AdapterListener mAdapterListener;
    protected DragEventListener mDragEventListener;

    protected int mHeaderLayoutResourceId;
    protected int mFooterLayoutResourceId;
    protected int mListLayoutResourceId;
    protected int mGridLayoutResourceId;

    protected Bitmap mGridLoadingBitmap;
    protected Bitmap mListLoadingBitmap;
    private Bitmap mLoadingBitmap;
    protected int mSpanCount;
    private int mRecyclerViewWidth = 0;

    protected ThumbnailWorker mThumbnailWorker;
    protected int mMinXSize;
    protected int mMinYSize;
    private int mLockSize;

    private boolean mShowInfoButton;
    private boolean mShowFavoriteIndicator;
    private boolean mSelectionMode;
    private boolean mMultiSelect;

    private boolean mIsInSearchMode = false;

    public final ArrayList<FileInfo> mFileInfoSelectedList = new ArrayList<>();

    private final CompositeDisposable mDisposable = new CompositeDisposable();

    @NonNull
    private FileBrowserTheme mTheme;

    public interface AdapterListener {
        void onShowFileInfo(int position);

        void onFilterResultsPublished(int resultCode);
    }

    public interface DragEventListener {
        void onDragClicked(int position);
    }

    public BaseFileAdapter(Context context, ArrayList<FileInfo> objects, Object objectsLock,
            int spanCount, AdapterListener adapterListener, ViewHolderBindListener bindListener) {
        super(bindListener);

        mTheme = FileBrowserTheme.fromContext(context);

        mContext = new WeakReference<>(context);
        mOriginalFiles = objects;
        // Don't assign mFiles to objects if a lock is provided
        // (This assumes that when a lock IS provided, mFiles will be populated after mOriginalFiles is filtered)
        mFiles = (objectsLock != null) ? null : objects;
        mOriginalFilesLock = (objectsLock != null) ? objectsLock : new Object();
        mAdapterListener = adapterListener;

        mSpanCount = spanCount;

        int listLoadingRes = Utils.getResourceDrawable(context, getResources().getString(R.string.list_loading_res_name));
        int gridLoadingRes = Utils.getResourceDrawable(context, getResources().getString(R.string.grid_loading_res_name));
        if (listLoadingRes == 0) {
            listLoadingRes = R.drawable.ic_file_blank_24dp;
        }
        if (gridLoadingRes == 0) {
            gridLoadingRes = R.drawable.white_square;
        }

        mListLoadingBitmap = BitmapFactory.decodeResource(context.getResources(), listLoadingRes);
        mGridLoadingBitmap = BitmapFactory.decodeResource(context.getResources(), gridLoadingRes);

        if (spanCount > 0) {
            mLoadingBitmap = mGridLoadingBitmap;
        } else {
            mLoadingBitmap = mGridLoadingBitmap;
        }
        mHeaderLayoutResourceId = R.layout.recyclerview_header_item;
        mFooterLayoutResourceId = R.layout.recyclerview_footer_item;
        mListLayoutResourceId = R.layout.listview_item_file_list;
        mGridLayoutResourceId = R.layout.gridview_item_file_list;

        // ensure the size of thumbnail is not zero
        updateSpanCount(mSpanCount);
        mThumbnailWorker = new ThumbnailWorker(context, mMinXSize, mMinYSize, mLoadingBitmap);
        mThumbnailWorker.setListener(this);

        mShowInfoButton = true;
        mShowFavoriteIndicator = true;
    }

    public void setDragEventListener(DragEventListener listener) {
        mDragEventListener = listener;
    }

    protected Context getContext() {
        return mContext.get();
    }

    protected Resources getResources() {
        Context context = getContext();
        if (context == null) {
            return null;
        }
        return context.getResources();
    }

    public int getSpanCount() {
        return mSpanCount;
    }

    protected List<FileInfo> getItems() {
        return mFiles;
    }

    @NonNull
    protected Object getListLock() {
        return mOriginalFilesLock;
    }

    @Override
    public FileInfo getItem(int position) {
        if (mFiles != null && position >= 0 && position < mFiles.size()) {
            return mFiles.get(position);
        }
        return null;
    }

    public int getIndexOf(FileInfo info) {
        return mFiles.indexOf(info);
    }

    @Override
    public final void add(FileInfo item) {
        if (mFiles != null) {
            mFiles.add(item);
        }
    }

    @Override
    public final boolean remove(FileInfo item) {
        return (mFiles != null && mFiles.remove(item));
    }

    @Override
    public FileInfo removeAt(int location) {
        return (mFiles != null) ? mFiles.remove(location) : null;
    }

    public void removeAll() {
        if (mFiles != null) {
            mFiles.clear();
        }
    }

    @Override
    public void insert(FileInfo item, int position) {
        if (mFiles != null) {
            mFiles.add(position, item);
        }
    }

    @Override
    public int getItemCount() {
        return (mFiles != null) ? mFiles.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        return isHeader(position) ? VIEW_TYPE_HEADER : VIEW_TYPE_CONTENT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final RecyclerView.ViewHolder holder;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case VIEW_TYPE_CONTENT:
                View contentView;
                boolean isGrid = mSpanCount > 0;
                if (isGrid) {
                    contentView = inflater.inflate(mGridLayoutResourceId, parent, false);
                } else {
                    contentView = inflater.inflate(mListLayoutResourceId, parent, false);
                }
                holder = new ContentViewHolder(contentView);
                if (!mSelectionMode) {
                    ((ContentViewHolder) holder).infoButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final int position = holder.getAdapterPosition();
                            if (position != RecyclerView.NO_POSITION && mAdapterListener != null) {
                                mAdapterListener.onShowFileInfo(position);
                            }
                        }
                    });
                }
                View dragButton = ((ContentViewHolder) holder).dragButton;
                if (dragButton != null) {
                    dragButton.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            final int position = holder.getAdapterPosition();
                            if (position != RecyclerView.NO_POSITION && mDragEventListener != null) {
                                mDragEventListener.onDragClicked(position);
                                return true;
                            }
                            return false;
                        }
                    });
                }
                ((ContentViewHolder) holder).textViewFileName.setTextColor(mTheme.contentBodyTextColor);
                ((ContentViewHolder) holder).textViewFileInfo.setTextColor(mTheme.contentSecondaryTextColor);
                ((ContentViewHolder) holder).imageViewInfoIcon.setColorFilter(mTheme.iconColor);
                if (((ContentViewHolder) holder).dragButton != null) {
                    ((ContentViewHolder) holder).dragButton.setColorFilter(mTheme.iconColor);
                }
                break;
            case VIEW_TYPE_FOOTER:
                View footerView = inflater.inflate(mFooterLayoutResourceId, parent, false);
                holder = new FooterViewHolder(footerView);
                break;
            case VIEW_TYPE_HEADER:
                View headerView = inflater.inflate(mHeaderLayoutResourceId, parent, false);
                holder = new HeaderViewHolder(headerView);
                if (mSpanCount == 0) {
                    ((HeaderViewHolder) holder).header_view.setVisibility(View.VISIBLE);
                    ((HeaderViewHolder) holder).container.setBackgroundColor(mTheme.headerBackgroundColor);
                    ((HeaderViewHolder) holder).foldingBtn.setVisibility(View.GONE);
                    ((HeaderViewHolder) holder).textViewTitle.setTextColor(mTheme.headerTextColor);
                } else {
                    ((HeaderViewHolder) holder).header_view.setVisibility(View.GONE);
                }
                break;
            default:
                throw new IllegalArgumentException("View type " + viewType + " not supported");
        }

        return holder;
    }

    public RecyclerView.Adapter getAdapter() {
        return this;
    }

    @Override
    public long getItemId(int position) {
        final FileInfo file = mFiles.get(position);
        return file.getAbsolutePath().hashCode();
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        Context context = getContext();
        if (context == null) {
            return;
        }

        switch (getItemViewType(position)) {
            case VIEW_TYPE_HEADER:
                onBindViewHolderHeader(holder, position);
                break;
            case VIEW_TYPE_FOOTER:
                break;
            default:
            case VIEW_TYPE_CONTENT:
                onBindViewHolderContent(holder, position);
                break;
        }
    }

    public void onBindViewHolderHeader(final RecyclerView.ViewHolder holder, int position) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        FileInfo file = mFiles.get(position);
        ((HeaderViewHolder) holder).textViewTitle.setText(file.getHeaderText());
    }

    public void onBindViewHolderContent(final RecyclerView.ViewHolder holder, int position) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        final FileInfo file = mFiles.get(position);
        ContentViewHolder contentViewHolder = (ContentViewHolder) holder;
        contentViewHolder.imageViewFileIcon.setImageDrawable(null);
        contentViewHolder.imageViewFileLockIcon.getLayoutParams().width = mLockSize;
        contentViewHolder.imageViewFileLockIcon.getLayoutParams().height = mLockSize;
        contentViewHolder.imageViewFileLockIcon.requestLayout();
        contentViewHolder.itemView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        if (file.isSecured()) {
            contentViewHolder.imageViewFileLockIcon.setVisibility(View.VISIBLE);
        } else {
            contentViewHolder.imageViewFileLockIcon.setVisibility(View.GONE);
        }

        if (mShowInfoButton) {
            contentViewHolder.imageViewInfoIcon.setVisibility(View.VISIBLE);
            contentViewHolder.infoButton.setVisibility(View.VISIBLE);
        } else {
            contentViewHolder.imageViewInfoIcon.setVisibility(View.GONE);
            contentViewHolder.infoButton.setVisibility(View.GONE);
        }

        if (mSelectionMode) {
            contentViewHolder.infoButton.setVisibility(View.INVISIBLE);
        }
        if (mSelectionMode && mMultiSelect && contentViewHolder.checkBox != null) {
            contentViewHolder.infoButton.setVisibility(View.VISIBLE);
            contentViewHolder.imageViewInfoIcon.setVisibility(View.GONE);
            contentViewHolder.checkBox.setVisibility(View.VISIBLE);
            contentViewHolder.checkBox.setChecked(mFileInfoSelectedList.contains(file));
        }

        String fileTitle = file.getFileName();

        if (mShowFavoriteIndicator && isFavoriteFile(position, file)) {
            fileTitle = fileTitle + " ";
            SpannableString ss = new SpannableString(fileTitle);
            Drawable drawable = AppCompatResources.getDrawable(context, R.drawable.star);
            if (drawable != null) {
                drawable = drawable.mutate();
                drawable.mutate().setColorFilter(getResources().getColor(R.color.orange), PorterDuff.Mode.SRC_IN);
                drawable.setBounds(0, 0, (int) Utils.convDp2Pix(context, 16), (int) Utils.convDp2Pix(context, 16));
                ImageSpan span = new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE);
                ss.setSpan(span, fileTitle.length() - 1, fileTitle.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            }

            contentViewHolder.textViewFileName.setText(ss);
        } else {
            contentViewHolder.textViewFileName.setText(fileTitle);
        }

        CharSequence description = getFileDescription(file);
        if (description != null && !Utils.isNullOrEmpty(description.toString())) {
            contentViewHolder.textViewFileInfo.setText(description);
            contentViewHolder.textViewFileInfo.setVisibility(View.VISIBLE);
        } else {
            contentViewHolder.textViewFileInfo.setVisibility(View.GONE);
        }
        contentViewHolder.docTextPlaceHolder.setVisibility(View.GONE);
        setFileIcon(holder, position);
    }

    public void setFileIcon(RecyclerView.ViewHolder holder, int position) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        // Document Preview
        final FileInfo file = mFiles.get(position);
        ContentViewHolder contentViewHolder = (ContentViewHolder) holder;
        int type = getFileType(file);
        int folderColorRes = getFolderIconColor(context);
        switch (type) {
            default:
            case BaseFileInfo.FILE_TYPE_FILE:
            case BaseFileInfo.FILE_TYPE_EDIT_URI:
            case BaseFileInfo.FILE_TYPE_OFFICE_URI:
            case BaseFileInfo.FILE_TYPE_EXTERNAL:
                if (mSpanCount <= 0) {
                    contentViewHolder.imageViewFileIcon.setBackgroundResource(0);
                }
                if (file.isSecured() || file.isPackage()) {
                    // Thumbnail has been generated before, and a placeholder icon should be used
                    if (mSpanCount > 0) {
                        contentViewHolder.imageViewFileIcon.setImageBitmap(mGridLoadingBitmap);
                    } else {
                        contentViewHolder.imageViewFileIcon.setImageBitmap(mListLoadingBitmap);
                    }
                } else {
                    String imagePath = ThumbnailPathCacheManager.getInstance().getThumbnailPath(file.getIdentifier(), mMinXSize, mMinYSize);

                    if (type == BaseFileInfo.FILE_TYPE_EXTERNAL
                            || type == BaseFileInfo.FILE_TYPE_EDIT_URI
                            || type == BaseFileInfo.FILE_TYPE_OFFICE_URI) {
                        String path = file.getAbsolutePath();
                        if (!Utils.isNullOrEmpty(path)) {
                            ContentResolver contentResolver = Utils.getContentResolver(context);
                            if (contentResolver == null) {
                                return;
                            }
                            // it is empty when, for example, retrieve it from Cache in FileManager
                            setDocTextPlaceHolderAsync(contentResolver, path, contentViewHolder.docTextPlaceHolder);
                            if (type == BaseFileInfo.FILE_TYPE_EXTERNAL) {
                                mThumbnailWorker.tryLoadImageWithUuid(position,
                                        file.getFileName(), file.getIdentifier(),
                                        imagePath, contentViewHolder.imageViewFileIcon);
                            } else {
                                loadPreviewForCloudFiles(file, position, contentViewHolder);
                            }
                        }
                    } else if (type == BaseFileInfo.FILE_TYPE_FILE) {
                        if (Utils.isDoNotRequestThumbFile(file.getAbsolutePath())) {
                            contentViewHolder.docTextPlaceHolder.setVisibility(View.VISIBLE);
                            String ext = Utils.getExtension(file.getAbsolutePath());
                            contentViewHolder.docTextPlaceHolder.setText(ext);
                        } else {
                            contentViewHolder.docTextPlaceHolder.setVisibility(View.GONE);
                        }
                        mThumbnailWorker.tryLoadImageWithPath(position, file.getAbsolutePath(),
                                imagePath, contentViewHolder.imageViewFileIcon);
                    }
                }
                break;
            case BaseFileInfo.FILE_TYPE_FOLDER:
            case BaseFileInfo.FILE_TYPE_EXTERNAL_FOLDER:
                if (mSpanCount > 0) {
                    contentViewHolder.imageViewFileIcon.setImageResource(R.drawable.ic_folder_large);
                    contentViewHolder.imageViewFileIcon.getDrawable().mutate().setColorFilter(folderColorRes, PorterDuff.Mode.SRC_IN);
                } else {
                    contentViewHolder.imageViewFileIcon.setImageResource(R.drawable.ic_folder_large);
                    contentViewHolder.imageViewFileIcon.getDrawable().mutate().setColorFilter(folderColorRes, PorterDuff.Mode.SRC_IN);
                    contentViewHolder.imageViewFileIcon.setBackgroundResource(0);
                }
                break;
            case BaseFileInfo.FILE_TYPE_EXTERNAL_ROOT:
                if (mSpanCount > 0) {
                    contentViewHolder.imageViewFileIcon.setImageResource(R.drawable.ic_sd_storage_black_24dp);
                    contentViewHolder.imageViewFileIcon.getDrawable().mutate().setColorFilter(folderColorRes, PorterDuff.Mode.SRC_IN);
                } else {
                    contentViewHolder.imageViewFileIcon.setImageResource(R.drawable.ic_sd_storage_black_24dp);
                    contentViewHolder.imageViewFileIcon.getDrawable().mutate().setColorFilter(folderColorRes, PorterDuff.Mode.SRC_IN);
                    contentViewHolder.imageViewFileIcon.setBackgroundResource(0);
                }
                break;
        }
    }

    private void setDocTextPlaceHolderAsync(@NonNull ContentResolver cr, @NonNull String path, @NonNull final TextView textView) {
        mDisposable.add(getDoNotRequestThumbFile(cr, path)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean isDoNotRequestThumbFile) throws Exception {
                        if (isDoNotRequestThumbFile) {
                            textView.setVisibility(View.VISIBLE);
                            String ext = Utils.getExtension(path);
                            textView.setText(ext);
                        } else {
                            textView.setVisibility(View.GONE);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable ignored) throws Exception {
                        // no-op
                    }
                }));
    }

    private Single<Boolean> getDoNotRequestThumbFile(@NonNull ContentResolver cr, @NonNull String path) {
        return Single.create(new SingleOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@NonNull SingleEmitter<Boolean> emitter) throws Exception {
                try {
                    boolean notRequestThumb = Utils.isDoNotRequestThumbFile(cr, path);
                    emitter.onSuccess(notRequestThumb);
                } catch (Exception e) {
                    emitter.onError(e);
                }
            }
        });
    }

    protected void loadPreviewForCloudFiles(FileInfo file, int position, ContentViewHolder contentViewHolder) {
        loadPreviewForCloudFiles(file, position, contentViewHolder,
                R.drawable.ic_file_blank_24dp, R.drawable.ic_file_blank_24dp);
    }

    protected void loadPreviewForCloudFiles(FileInfo file, int position, ContentViewHolder contentViewHolder, int loadingSmallRes, int loadingLargeRes) {
        if (mSpanCount > 0) {
            contentViewHolder.imageViewFileIcon.setImageResource(loadingLargeRes);
        } else {
            contentViewHolder.imageViewFileIcon.setImageResource(loadingSmallRes);
            contentViewHolder.imageViewFileIcon.setBackgroundResource(0);
        }
        if (!file.isSecured() && !file.isPackage()) {
            String imagePath;
            // Try to load thumbnail preview for file
            imagePath = RecentlyUsedCache.getBitmapPathIfExists(file.getAbsolutePath());
            if (Utils.isNullOrEmpty(imagePath)) { // Empty image paths are not considered by ThumbnailWorker
                imagePath = null;
            }
            if (null != imagePath) {
                if (mSpanCount <= 0) {
                    contentViewHolder.imageViewFileIcon.setBackgroundResource(0);
                }
                mThumbnailWorker.tryLoadImageWithUuid(position,
                        file.getFileName(), file.getAbsolutePath(),
                        imagePath, contentViewHolder.imageViewFileIcon);
            } else {
                ContentResolver cr = Utils.getContentResolver(getContext());
                if (cr != null) {
                    Uri uri = Uri.parse(file.getAbsolutePath());
                    mThumbnailWorker.tryLoadImage(position,
                            uri.toString(), null,
                            contentViewHolder.imageViewFileIcon,
                            ThumbnailWorker.MODE_UUID
                    );
                }
            }
        }
    }

    public int getFileType(FileInfo file) {
        return file.getFileType();
    }

    public CharSequence getFileDescription(FileInfo file) {
        Context context = getContext();
        if (context == null) {
            return "";
        }

        String description;

        if (isInSearchMode()) {
            if (file.getFileType() == BaseFileInfo.FILE_TYPE_EXTERNAL || file.getFileType() == BaseFileInfo.FILE_TYPE_EXTERNAL_FOLDER) {
                ExternalFileInfo fileInfo = Utils.buildExternalFile(context, Uri.parse(file.getAbsolutePath()));
                if (fileInfo == null) {
                    return "";
                }
                description = fileInfo.getParentRelativePath();
                if (description == null) {
                    description = "";
                }
                description += "/" + fileInfo.getFileName();
            } else {
                description = FilenameUtils.getPath(file.getAbsolutePath());
            }
        } else if (!(mSpanCount > 0) &&
                (file.getFileType() == BaseFileInfo.FILE_TYPE_FILE || file.getFileType() == BaseFileInfo.FILE_TYPE_EXTERNAL)) {
            description = file.getModifiedDate() + " · " + file.getSizeInfo();
        } else {
            description = file.getModifiedDate();
        }
        return description;
    }

    @Override
    public void updateSpanCount(int count) {
        Resources resources = getResources();
        if (resources == null) {
            return;
        }

        if (count > 0) {
            mMinXSize = mRecyclerViewWidth / count;
            mMinYSize = (int) (mMinXSize * 1.29);
            mLockSize = resources.getDimensionPixelSize(R.dimen.thumbnail_lock_size_medium);
            mLoadingBitmap = mGridLoadingBitmap;
            if (mMinXSize == 0 || mMinYSize == 0) {
                mMinXSize = resources.getDimensionPixelSize(R.dimen.thumbnail_height_large);
                mMinYSize = resources.getDimensionPixelSize(R.dimen.thumbnail_height_large);
            }
        } else {
            mMinXSize = resources.getDimensionPixelSize(R.dimen.list_thumbnail_width);
            mMinYSize = resources.getDimensionPixelSize(R.dimen.list_thumbnail_height);
            mLockSize = resources.getDimensionPixelSize(R.dimen.thumbnail_lock_size_list);
            mLoadingBitmap = mGridLoadingBitmap;
        }
        if (mThumbnailWorker != null) {
            mThumbnailWorker.setMinXSize(mMinXSize);
            mThumbnailWorker.setMinYSize(mMinYSize);
            mThumbnailWorker.setLoadingBitmap(mLoadingBitmap);
        }
        mSpanCount = count;
    }

    public void setShowInfoButton(boolean show) {
        mShowInfoButton = show;
    }

    public void setSelectionMode(boolean selectionMode) {
        mSelectionMode = selectionMode;
    }

    public void setMultiSelect(boolean multiSelect) {
        mMultiSelect = multiSelect;
    }

    public void setShowFavoriteIndicator(boolean show) {
        mShowFavoriteIndicator = show;
    }

    protected boolean isFavoriteFile(int position, FileInfo file) {
        return false;
    }

    public boolean isHeader(int position) {
        List<FileInfo> files = getItems();
        if (files == null || position < 0 || position >= files.size()) {
            return false;
        }
        FileInfo fileInfo = files.get(position);
        return fileInfo.isHeader();
    }

    public void updateMainViewWidth(int width) {
        mRecyclerViewWidth = width;
    }

    @Override
    public void onThumbnailReady(int result, final int position, String iconPath, String identifier) {
        final FileInfo file = getItem(position); // getItem will perform bounds checks
        if (file == null || !identifier.contains(file.getAbsolutePath())) {
            return;
        }

        boolean canAdd = true;

        if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_SECURITY_ERROR) {
            // avoid flashing caused by the callback
            file.setIsSecured(true);
            canAdd = false;
        }
        if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_PACKAGE_ERROR) {
            // avoid flashing caused by the callback
            file.setIsPackage(true);
            canAdd = false;
        }
        if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_NOT_FOUNT) {
            // create this file instead
            mThumbnailWorker.tryLoadImageFromFilter(position, identifier, file.getAbsolutePath());
            return;
        } else if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_CANCEL ||
                result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_PREVIOUS_CRASH ||
                result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_POSTPONED) {
            canAdd = false;
        }

        // adds path to local cache for later access
        if (canAdd) {
            ThumbnailPathCacheManager.getInstance().putThumbnailPath(identifier,
                    iconPath, mMinXSize, mMinYSize);
        }

        // update this position only
        if (getRecyclerView() != null) {
            RecyclerView.ViewHolder holder = getRecyclerView().findViewHolderForLayoutPosition(position);
            if (holder != null) {
                updateViewHolder(holder, position, result, iconPath);
            } else {
                // if cannot update view holder at the moment, the next time the view holder
                // is available its thumbnail should be generated; otherwise it will be loaded
                // with an empty thumbnail
                getRecyclerView().post((new Runnable() {
                    @Override
                    public void run() {
                        if (position < getItemCount()) {
                            Utils.safeNotifyItemChanged(BaseFileAdapter.this, position);
                        }
                    }
                }));
            }
        }
    }

    private void updateViewHolder(RecyclerView.ViewHolder holder, int position, int result, String iconPath) {
        if (!(holder instanceof ContentViewHolder)) {
            return;
        }
        final ContentViewHolder contentViewHolder = (ContentViewHolder) holder;

        if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_SECURITY_ERROR) {
            contentViewHolder.imageViewFileLockIcon.setVisibility(View.VISIBLE);
        } else {
            contentViewHolder.imageViewFileLockIcon.setVisibility(View.GONE);
        }
        if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_FAILURE ||
                result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_SECURITY_ERROR ||
                result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_PACKAGE_ERROR ||
                result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_CANCEL ||
                result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_PREVIOUS_CRASH ||
                result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_POSTPONED) {
            // Thumbnail has been generated before, and a placeholder icon should be used
            if (mSpanCount > 0) {
                contentViewHolder.imageViewFileIcon.setImageBitmap(mGridLoadingBitmap);
            } else {
                contentViewHolder.imageViewFileIcon.setImageBitmap(mListLoadingBitmap);
            }
        } else {
            String filePath = mFiles.get(position).getAbsolutePath();
            mThumbnailWorker.tryLoadImageWithPath(position, filePath, iconPath, contentViewHolder.imageViewFileIcon);
        }
    }

    public void cancelAllThumbRequests(boolean removePreviewHandler) {
        abortCancelThumbRequests();
        if (removePreviewHandler) {
            mThumbnailWorker.removePreviewHandler();
        }
        mThumbnailWorker.cancelAllThumbRequests();
        mDisposable.clear();
    }

    public void cancelAllThumbRequests() {
        cancelAllThumbRequests(false);
    }

    public void abortCancelThumbRequests() {
        mThumbnailWorker.abortCancelTask();
    }

    public void cleanupResources() {
        mThumbnailWorker.cleanupResources();
        mDisposable.clear();
    }

    public void evictFromMemoryCache(String uuid) {
        mThumbnailWorker.evictFromMemoryCache(uuid);
    }

    public boolean isInSearchMode() {
        return mIsInSearchMode;
    }

    public void setInSearchMode(boolean isInSearchMode) {
        mIsInSearchMode = isInSearchMode;
    }

    @Override
    public Filter getFilter() {
        return getDerivedFilter();
    }

    public FileListFilter getDerivedFilter() {
        if (mFilter == null) {
            mFilter = new FileListFilter<>(mOriginalFiles, this, mOriginalFilesLock);
        }
        return mFilter;
    }

    @Override
    public void onFilterResultsPublished(final ArrayList<FileInfo> filteredFiles, final int resultCode) {

        mFiles = filteredFiles;
        int code = resultCode;
        try {
            notifyDataSetChanged();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            code = FileListFilter.FILTER_RESULT_FAILURE;
        }

        if (mAdapterListener != null) {
            mAdapterListener.onFilterResultsPublished(code);
        }
    }

    @ColorInt
    public static int getFolderIconColor(@NonNull Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.file_browser_folder_icon_color, typedValue, true);
        int colorAttr = typedValue.data;
        if (colorAttr == 0) {
            int folderColorRes = Utils.getResourceColor(context, context.getResources().getString(R.string.folder_color));
            if (folderColorRes == 0) {
                folderColorRes = android.R.color.black;
            }

            return context.getResources().getColor(folderColorRes);
        } else {
            return typedValue.data;
        }
    }
}
