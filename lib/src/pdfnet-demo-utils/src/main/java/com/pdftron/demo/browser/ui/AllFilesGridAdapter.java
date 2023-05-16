package com.pdftron.demo.browser.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.pdftron.demo.R;
import com.pdftron.demo.browser.model.FileItem;
import com.pdftron.demo.utils.ThumbnailPathCacheManager;
import com.pdftron.demo.utils.ThumbnailWorker;
import com.pdftron.demo.widget.ImageViewTopCrop;
import com.pdftron.pdf.PreviewHandler;
import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.FavoriteFilesManager;
import com.pdftron.pdf.utils.FileInfoManager;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.recyclerview.ViewHolderBindListener;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;

public class AllFilesGridAdapter extends BaseQuickAdapter<MultiItemEntity, BaseViewHolder>
        implements ThumbnailWorker.ThumbnailWorkerListener, AdapterHelper {

    private PublishSubject<List<MultiItemEntity>> mNewDataUpdateSubject = PublishSubject.create();
    private Bitmap mLoadingBitmap;
    @DrawableRes
    private int mDefaultFileRes;
    protected int mMinXSize;
    protected int mMinYSize;
    protected boolean mIsLocal = true;
    protected ThumbnailWorker mThumbnailWorker;
    protected ViewHolderBindListener<BaseViewHolder> mViewHolderBindListener;
    private int mLastOpenedFilePosition = -1;
    private boolean mShowInfoButton;

    protected AllFilesGridAdapter(@NonNull Activity activity, int gridCount, boolean isLocal) {
        this(activity, gridCount);
        mIsLocal = isLocal;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("CheckResult")
    protected AllFilesGridAdapter(@NonNull Activity activity, @IntRange(from = 0, to = 6) int gridCount) {
        super(R.layout.gridview_item_file_list, null);
        Resources resources = activity.getResources();
        mLoadingBitmap = BitmapFactory.decodeResource(resources, R.drawable.white_square);
        mDefaultFileRes = Utils.getResourceDrawable(activity, resources.getString(R.string.grid_loading_res_name));

        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager()
                .getDefaultDisplay()
                .getMetrics(displayMetrics);
        mMinXSize = displayMetrics.widthPixels / gridCount;
        mMinYSize = (int) (mMinXSize * 1.29);
        mThumbnailWorker = new ThumbnailWorker(activity, mMinXSize, mMinYSize, mLoadingBitmap);
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
                        setNewData(items);
                        if (mLastOpenedFilePosition >= 0) {
                            getRecyclerView().scrollToPosition(mLastOpenedFilePosition);
                            mLastOpenedFilePosition = -1;
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
    protected void convert(@NonNull BaseViewHolder holder, MultiItemEntity item) {
        mViewHolderBindListener.onBindViewHolder(holder, holder.getAdapterPosition());
        if (!(item instanceof FileItem)) {
            return;
        }

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

        CharSequence description = fileItem.dateString;
        if (description != null && !Utils.isNullOrEmpty(description.toString())) {
            holder.setText(R.id.file_info, description);
            holder.setGone(R.id.file_info, true);
        } else {
            holder.setGone(R.id.file_info, false);
        }
        holder.setGone(R.id.docTextPlaceHolder, false);
        setFileIcon(holder, fileItem, holder.getAdapterPosition());
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

        if (mShowInfoButton) {
            holder.setGone(R.id.info_icon, true);
        } else {
            holder.setGone(R.id.info_icon, false);
        }
    }

    public void setItems(List<MultiItemEntity> items) {
        mNewDataUpdateSubject.onNext(items);
    }

    @Override
    public void setLastOpenedFilePosition(int lastOpenedFilePosition) {
        mLastOpenedFilePosition = lastOpenedFilePosition;
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

    @Override
    public void onThumbnailReady(int result, final int position, String iconPath, String identifier) {
        final FileItem file = (FileItem) getItem(position); // getItem will perform bounds checks
        if (file == null || !identifier.contains(file.filePath)) {
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
                        Utils.safeNotifyItemChanged(AllFilesGridAdapter.this, position);
                    }
                }
            }));
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
