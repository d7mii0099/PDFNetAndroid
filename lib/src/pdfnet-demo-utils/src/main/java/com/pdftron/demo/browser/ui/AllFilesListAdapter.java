package com.pdftron.demo.browser.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.View;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.pdftron.demo.R;
import com.pdftron.demo.browser.db.folder.FolderDatabase;
import com.pdftron.demo.browser.db.folder.FolderEntity;
import com.pdftron.demo.browser.model.FileItem;
import com.pdftron.demo.browser.model.FolderItem;
import com.pdftron.demo.utils.ThumbnailPathCacheManager;
import com.pdftron.demo.utils.ThumbnailWorker;
import com.pdftron.demo.widget.ImageViewTopCrop;
import com.pdftron.pdf.PreviewHandler;
import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.FavoriteFilesManager;
import com.pdftron.pdf.utils.FileInfoManager;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.recyclerview.ItemTouchHelperCallback;
import com.pdftron.pdf.widget.recyclerview.ViewHolderBindListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class AllFilesListAdapter extends BaseMultiItemQuickAdapter<MultiItemEntity, BaseViewHolder>
        implements ThumbnailWorker.ThumbnailWorkerListener, AdapterHelper {

    public static final int VIEW_TYPE_HEADER = ItemTouchHelperCallback.VIEW_TYPE_HEADER;
    public static final int VIEW_TYPE_CONTENT = ItemTouchHelperCallback.VIEW_TYPE_CONTENT;

    private Bitmap mLoadingBitmap;
    @DrawableRes
    private int mDefaultFileRes;
    protected int mMinXSize;
    protected int mMinYSize;
    protected boolean mIsLocal = true;
    protected ThumbnailWorker mThumbnailWorker;
    protected ViewHolderBindListener<BaseViewHolder> mViewHolderBindListener;
    private int mLastOpenedFilePosition = -1;

    private FileBrowserTheme mTheme;

    private PublishSubject<List<MultiItemEntity>> mNewDataUpdateSubject = PublishSubject.create();

    private boolean mShowInfoButton;
    private boolean mIsBackupView;

    protected AllFilesListAdapter(@NonNull Context context, boolean isLocal) {
        this(context);
        mIsLocal = isLocal;
    }

    protected AllFilesListAdapter(@NonNull Context context, boolean isLocal, boolean isBackupView) {
        this(context);
        mIsLocal = isLocal;
        mIsBackupView = isBackupView;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("CheckResult")
    protected AllFilesListAdapter(@NonNull Context context) {
        super(null);
        mTheme = FileBrowserTheme.fromContext(context);
        addItemType(VIEW_TYPE_HEADER, R.layout.recyclerview_header_item);
        addItemType(VIEW_TYPE_CONTENT, R.layout.listview_item_file_list);

        Resources resources = context.getResources();
        mLoadingBitmap = BitmapFactory.decodeResource(resources, R.drawable.white_square);
        mDefaultFileRes = Utils.getResourceDrawable(context, resources.getString(R.string.list_loading_res_name));
        mMinXSize = resources.getDimensionPixelSize(R.dimen.list_thumbnail_width);
        mMinYSize = resources.getDimensionPixelSize(R.dimen.list_thumbnail_height);
        mThumbnailWorker = new ThumbnailWorker(context, mMinXSize, mMinYSize, mLoadingBitmap);
        mThumbnailWorker.setListener(this);
        mShowInfoButton = true;
        // Here we want to have a custom observable, that immediately emits the first group of items, but
        // will sample the rest of the groups. This is to ensure that the first group is immediately loaded
        // (in order to quickly populate the UI) and the rest of the items are not loaded too quickly (which
        // will cause stuttering due to UI loading overhead).
        Observable<List<MultiItemEntity>> firstItem = mNewDataUpdateSubject.serialize().take(1);
        Observable<List<MultiItemEntity>> restOfItems = mNewDataUpdateSubject.serialize().skip(1).throttleLast(500, TimeUnit.MILLISECONDS);
        Observable.merge(firstItem, restOfItems)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<MultiItemEntity>>() {
                    @Override
                    public void accept(List<MultiItemEntity> items) throws Exception {
                        // Here we must call expand after setting the data, since expand is
                        // async. So we'll gather the expanded positions ahead of time.
                        List<Integer> expandPositions = new ArrayList<>();

                        for (int position = items.size() - 1; position >= 0; position--) {
                            FolderItem folder = (FolderItem) items.get(position);
                            if (!folder.isCollapsed()) {
                                expandPositions.add(position);
                            }
                        }
                        // Then set the data
                        setNewData(items);
                        if (mLastOpenedFilePosition >= 0) {
                            getRecyclerView().scrollToPosition(mLastOpenedFilePosition);
                            mLastOpenedFilePosition = -1;
                        }
                        // And finally expand the folders
                        for (Integer expandPosition : expandPositions) {
                            expand(expandPosition);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throw new RuntimeException(throwable);
                    }
                });
    }

    void setItemSelectionListener(ViewHolderBindListener<BaseViewHolder> bindListener) {
        mViewHolderBindListener = bindListener;
    }

    @Override
    protected void convert(@NonNull final BaseViewHolder holder, final MultiItemEntity item) {
        mViewHolderBindListener.onBindViewHolder(holder, holder.getAdapterPosition());
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_HEADER: {
                final FolderItem folderItem = (FolderItem) item;
                // Obscure absolute path if showing as a Backup view
                if (mIsBackupView) {
                    String filePath = mContext.getExternalFilesDir(null).toString();
                    holder.setText(R.id.title, folderItem.filePath.replace(filePath, ""));
                } else {
                    holder.setText(R.id.title, folderItem.filePath);
                }
                // Set theme colors
                holder.itemView.setBackgroundColor(mTheme.headerBackgroundColor);
                holder.setTextColor(R.id.title, mTheme.headerTextColor);
                AppCompatImageView chevron = holder.getView(R.id.folding_btn);
                chevron.setColorFilter(mTheme.headerChevronColor);
                if (folderItem.isCollapsed()) {
                    holder.setImageResource(R.id.folding_btn, R.drawable.ic_keyboard_arrow_down_black_24dp);
                    holder.setGone(R.id.divider, true);
                } else {
                    holder.setImageResource(R.id.folding_btn, R.drawable.ic_keyboard_arrow_up_black_24dp);
                    holder.setGone(R.id.divider, false);
                }
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clickHeader(holder.itemView.getContext(), holder.getAdapterPosition());
                    }
                });
                break;
            }
            case VIEW_TYPE_CONTENT: {
                FileItem fileItem = (FileItem) item;
                if (fileItem.isSecured) {
                    holder.setGone(R.id.file_lock_icon, true);
                } else {
                    holder.setGone(R.id.file_lock_icon, false);
                }

                String fileTitle = fileItem.filename;

                if (isFavoriteFile(mContext, fileItem)) {
                    fileTitle = fileTitle + " ";
                    SpannableString ss = new SpannableString(fileTitle);
                    Drawable drawable = mContext.getResources().getDrawable(R.drawable.star);
                    drawable = drawable.mutate();
                    drawable.mutate().setColorFilter(mContext.getResources().getColor(R.color.orange), PorterDuff.Mode.SRC_IN);
                    drawable.setBounds(0, 0, (int) Utils.convDp2Pix(mContext, 16), (int) Utils.convDp2Pix(mContext, 16));

                    ImageSpan span = new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE);
                    ss.setSpan(span, fileTitle.length() - 1, fileTitle.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

                    holder.setText(R.id.file_name, ss);
                } else {
                    holder.setText(R.id.file_name, fileTitle);
                }

                holder.addOnClickListener(R.id.info_button);

                CharSequence description = fileItem.dateString + "   " + Utils.humanReadableByteCount(fileItem.size, false);
                if (description != null && !Utils.isNullOrEmpty(description.toString())) {
                    holder.setText(R.id.file_info, description);
                    holder.setGone(R.id.file_info, true);
                } else {
                    holder.setGone(R.id.file_info, false);
                }
                holder.setGone(R.id.docTextPlaceHolder, false);
                setFileIcon(holder, fileItem, holder.getAdapterPosition());

                if (mShowInfoButton) {
                    holder.setGone(R.id.info_icon, true);
                } else {
                    holder.setGone(R.id.info_icon, false);
                }
                break;
            }
            default: {
                throw new RuntimeException("Unknown list item.");
            }
        }
    }

    public void cancelAllThumbRequests(boolean removePreviewHandler) {
        abortCancelThumbRequests();
        if (removePreviewHandler) {
            mThumbnailWorker.removePreviewHandler();
        }
        mThumbnailWorker.cancelAllThumbRequests();
    }

    public void abortCancelThumbRequests() {
        mThumbnailWorker.abortCancelTask();
    }

    public void cleanupResources() {
        mThumbnailWorker.cleanupResources();
    }

    public void cancelAllThumbRequests() {
        cancelAllThumbRequests(false);
    }

    public void evictFromMemoryCache(String uuid) {
        mThumbnailWorker.evictFromMemoryCache(uuid);
    }

    public void setItems(List<MultiItemEntity> items) {
        mNewDataUpdateSubject.onNext(items);
    }

    @Override
    public void setLastOpenedFilePosition(int lastOpenedFilePosition) {
        mLastOpenedFilePosition = lastOpenedFilePosition;
    }

    public void setFileIcon(BaseViewHolder holder, FileItem fileItem, int position) {
        // Document Preview
        if (fileItem.isSecured || fileItem.isPackage) {
            holder.setImageResource(R.id.file_icon, mDefaultFileRes);
        } else {
            String imagePath = ThumbnailPathCacheManager.getInstance().getThumbnailPath(fileItem.filePath, mMinXSize, mMinYSize);
            String path = fileItem.filePath;
            if (Utils.isDoNotRequestThumbFile(fileItem.filePath)) {
                holder.setVisible(R.id.docTextPlaceHolder, true);

                String ext = Utils.getExtension(path);
                holder.setText(R.id.docTextPlaceHolder, ext);
            } else {
                holder.setGone(R.id.docTextPlaceHolder, false);
            }
            if (mIsLocal) {
                mThumbnailWorker.tryLoadImageWithPath(position, path,
                        imagePath, (ImageViewTopCrop) holder.getView(R.id.file_icon));
            } else {
                long fileSize = fileItem.size;
                mThumbnailWorker.tryLoadImageWithUuid(position, path, (fileSize + path),
                        imagePath, (ImageViewTopCrop) holder.getView(R.id.file_icon));
            }
        }
    }

    @Override
    public void onThumbnailReady(int result, final int position, String iconPath, String identifier) {
        MultiItemEntity item = getItem(position);
        if (!(item instanceof FileItem)) {
            return;
        }

        final FileItem file = (FileItem) item; // getItem will perform bounds checks
        if (!identifier.contains(file.filePath)) {
            return;
        }

        if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_SECURITY_ERROR) {
            // avoid flashing caused by the callback
            file.isSecured = true;
        }
        if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_PACKAGE_ERROR) {
            // avoid flashing caused by the callback
            file.isPackage = true;
        }

        boolean canAdd = true;
        if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_NOT_FOUNT) {
            // create this file instead
            mThumbnailWorker.tryLoadImageFromFilter(position, identifier, file.filePath);
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
            // if cannot update view holder at the moment, the next time the view holder
            // is available its thumbnail should be generated; otherwise it will be loaded
            // with an empty thumbnail
            getRecyclerView().post((new Runnable() {
                @Override
                public void run() {
                    if (position < getItemCount()) {
                        Utils.safeNotifyItemChanged(AllFilesListAdapter.this, position);
                    }
                }
            }));
        }
    }

    public int findHeaderPositionFromChild(int childPosition) {
        int i = childPosition - 1;
        while (i >= 0) {
            if (getItemViewType(i) == VIEW_TYPE_HEADER) {
                return i;
            }
            i--;
        }
        return i;
    }

    void clickHeader(Context context, int headerPosition) {
        MultiItemEntity item = getItem(headerPosition);
        if (getItemViewType(headerPosition) != VIEW_TYPE_HEADER || !(item instanceof FolderItem)) {
            return;
        }

        FolderItem folderItem = (FolderItem) item;
        clickHeader(context, headerPosition, folderItem);
    }

    @SuppressLint("CheckResult")
    private void clickHeader(@NonNull final Context context, int position, @NonNull final FolderItem folderItem) {
        if (folderItem.isExpanded()) {
            collapse(position);
            folderItem.setCollapsed(true);
            Single.just(new FolderEntity(folderItem.filePath, true))
                    .observeOn(Schedulers.io())
                    .subscribe(new Consumer<FolderEntity>() {
                        @Override
                        public void accept(FolderEntity folderEntity) throws Exception {
                            FolderDatabase.getInstance(context)
                                    .folderDao()
                                    .updateCollapseState(folderEntity);
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            throw new RuntimeException("Error updating folder state", throwable);
                        }
                    });
        } else {
            expand(position);
            folderItem.setCollapsed(false);
            Single.just(new FolderEntity(folderItem.filePath, false))
                    .observeOn(Schedulers.io())
                    .subscribe(new Consumer<FolderEntity>() {
                        @Override
                        public void accept(FolderEntity folderEntity) throws Exception {
                            FolderDatabase.getInstance(context)
                                    .folderDao()
                                    .updateCollapseState(folderEntity);
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            throw new RuntimeException("Error updating folder state", throwable);
                        }
                    });
        }
    }

    boolean isFavoriteFile(Context context, FileItem fileItem) {
        return context != null && getFileInfoManager().containsFile(context, new FileInfo(BaseFileInfo.FILE_TYPE_FILE, new File(fileItem.filePath)));
    }

    protected FileInfoManager getFileInfoManager() {
        return FavoriteFilesManager.getInstance();
    }

    public void setShowInfoButton(boolean show) {
        mShowInfoButton = show;
    }
}
