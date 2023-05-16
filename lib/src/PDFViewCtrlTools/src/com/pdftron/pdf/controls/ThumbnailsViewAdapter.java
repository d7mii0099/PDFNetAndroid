//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.common.PDFNetException;
import com.pdftron.filters.SecondaryFileFilter;
import com.pdftron.pdf.Convert;
import com.pdftron.pdf.DocumentConversion;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageIterator;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.WordToPDFOptions;
import com.pdftron.pdf.dialog.pagelabel.PageLabelUtils;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.BookmarkManager;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.CustomAsyncTask;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerViewAdapter;
import com.pdftron.pdf.widget.recyclerview.ViewHolderBindListener;
import com.pdftron.sdf.Obj;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import co.paulburke.android.itemtouchhelperdemo.helper.ItemTouchHelperAdapter;

/**
 * A Recycler view adapter for loading thumbnail views
 */
@SuppressWarnings("WeakerAccess")
public class ThumbnailsViewAdapter extends SimpleRecyclerViewAdapter<Integer, ThumbnailsViewAdapter.PageViewHolder>
        implements PDFViewCtrl.ThumbAsyncListener, ItemTouchHelperAdapter, PasswordDialogFragment.PasswordDialogFragmentListener {

    private static final String TAG = ThumbnailsViewAdapter.class.getName();

    private static boolean sDebug = false;

    /**
     * Callback interface to be invoked when pages of the document have been edited.
     */
    public interface EditPagesListener {
        /**
         * Called when a page was moved to a new position
         *
         * @param fromPageNum The page number from which the page moves
         * @param toPageNum   The page number to which the page moves
         */
        void onPageMoved(int fromPageNum, int toPageNum);

        /**
         * Called when new pages were added to the document.
         *
         * @param pageList The list of pages added to the document
         */
        void onPagesAdded(List<Integer> pageList);
    }

    private EditPagesListener mEditPageListener;

    private Context mContext;
    private FragmentManager mFragmentManager;
    private PDFViewCtrl mPdfViewCtrl;

    private LayoutInflater mLayoutInflater;

    private List<Integer> mDataList = new ArrayList<>(); // list of page number
    private ConcurrentHashMap<Integer, File> mThumbFileMap; // loaded thumbnail files
    private CopyOnWriteArrayList<File> mThumbFiles = new CopyOnWriteArrayList<>(); // for cleanup

    private SparseArray<LoadThumbnailTask> mTaskList;

    // For the page back and forward buttons:
    // When a page is moved, deleted or added the document has been modified.
    // If the document has been changed, the page back and forward stacks should be cleared.
    // However, if a page is added to the end of the document, the
    // page back and forward stacks are still considered valid.
    private boolean mDocPagesModified = false;

    private int mCurrentPage;
    private int mSpanCount;
    private int mRecyclerViewWidth;

    @ColorInt
    private int mPageNumberLabelColor = -1;
    @ColorInt
    private int mActivePageNumberLabelColor = -1;

    private final Object mPauseWorkLock = new Object();
    private boolean mPauseWork = false;

    private final Lock mDataLock = new ReentrantLock();

    private int mPwdRequestLastPage;
    private Uri mPwdRequestUri;

    private boolean mEditing;

    private ThumbnailsViewFragment.Theme mTheme;

    /**
     * The format of document
     */
    public enum DocumentFormat {
        /**
         * Specified PDF page(s)
         */
        PDF_PAGE,
        /**
         * Blank PDF page
         */
        BLANK_PDF_PAGE,
        /**
         * A PDF document
         */
        PDF_DOC,
        /**
         * An image
         */
        IMAGE
    }

    /**
     * Class constructor
     */
    public ThumbnailsViewAdapter(Context context, EditPagesListener listener, FragmentManager fragmentManager, PDFViewCtrl pdfViewCtrl,
            @Nullable List<Integer> dataList, int spanCount, ViewHolderBindListener bindListener, @NonNull ThumbnailsViewFragment.Theme theme) {
        super(bindListener);
        mContext = context;
        mEditPageListener = listener;
        mFragmentManager = fragmentManager;
        mPdfViewCtrl = pdfViewCtrl;
        if (dataList != null) {
            mDataList.addAll(dataList);
        }
        mThumbFileMap = new ConcurrentHashMap<>();
        mTaskList = new SparseArray<>();
        mSpanCount = spanCount;
        mCurrentPage = mPdfViewCtrl.getCurrentPage();
        mPdfViewCtrl.addThumbAsyncListener(this);
        mTheme = theme;
    }

    /**
     * The overload implementation of {@link SimpleRecyclerViewAdapter#onDetachedFromRecyclerView(RecyclerView)}.
     */
    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mPdfViewCtrl.removeThumbAsyncListener(this);
    }

    /**
     * Cleans up resources
     */
    public void finish() {
        mPdfViewCtrl.removeThumbAsyncListener(this);
    }

    /**
     * The overload implementation of {@link SimpleRecyclerViewAdapter#getItemCount()}.
     */
    @Override
    public int getItemCount() {
        return (mDataList != null) ? mDataList.size() : 0;
    }

    public void setItem(int position, int data) {
        if (mDataList != null && position >= 0 && position < mDataList.size()) {
            int oldPage = getItem(position);
            removeCachedPage(oldPage);
            mDataList.set(position, data);
        }
    }

    public void clear() {
        mDataLock.lock();
        mDataList.clear();
        mDataLock.unlock();
        clearResources();
    }

    public void setData(List<Integer> data) {
        clear();
        addAll(data);
    }

    public void addAll(List<Integer> data) {
        mDataLock.lock();
        mDataList.addAll(data);
        mDataLock.unlock();

        Utils.safeNotifyDataSetChanged(this);
    }

    public void setEditing(boolean editing) {
        mEditing = editing;
        Utils.safeNotifyDataSetChanged(this);
    }

    /**
     * The overload implementation of {@link SimpleRecyclerViewAdapter#getItem(int)}.
     */
    @Override
    public Integer getItem(int position) {
        if (mDataList != null && position >= 0 && position < mDataList.size()) {
            return mDataList.get(position);
        }
        return null;
    }

    /**
     * The overload implementation of {@link SimpleRecyclerViewAdapter#onCreateViewHolder(ViewGroup, int)}.
     */
    @Override
    public PageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = getLayoutInflater().inflate(R.layout.controls_thumbnails_view_grid_item, parent, false);
        return new PageViewHolder(view);
    }

    /**
     * The overload implementation of {@link SimpleRecyclerViewAdapter#onBindViewHolder(RecyclerView.ViewHolder, int)}.
     */
    @Override
    public void onBindViewHolder(PageViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        if (mPdfViewCtrl == null) {
            return;
        }

        ViewGroup.LayoutParams params = holder.imageLayout.getLayoutParams();
        params.width = mRecyclerViewWidth / mSpanCount;
        params.height = (int) (params.width * 1.29); // US-letter size
        holder.imageLayout.requestLayout();

        int pageNum = getItem(position);

        String pageLabel = PageLabelUtils.getPageLabelTitle(mPdfViewCtrl, pageNum);
        if (!Utils.isNullOrEmpty(pageLabel)) {
            holder.pageNumber.setText(pageLabel);
        } else {
            holder.pageNumber.setText(Utils.getLocaleDigits(Integer.toString(pageNum)));
        }

        if (pageNum == mCurrentPage) {
            int activeColor = mTheme.activePageNumberBackgroundColor;
            if (getActivePageNumberBackgroundLabelColor() != -1) {
                activeColor = getActivePageNumberBackgroundLabelColor();
            }
            Drawable drawable = mPdfViewCtrl.getResources().getDrawable(R.drawable.controls_thumbnails_view_rounded_edges_current);
            drawable.mutate();
            if (Utils.isLollipop()) {
                DrawableCompat.setTint(drawable, activeColor);
            } else {
                drawable.setColorFilter(new PorterDuffColorFilter(activeColor, PorterDuff.Mode.SRC_ATOP));
            }
            holder.pageNumber.setBackground(drawable);
            holder.pageNumber.setTextColor(mTheme.activePageNumberTextColor);
        } else {
            int pageColor = mTheme.pageNumberBackgroundColor;
            if (getPageNumberBackgroundLabelColor() != -1) {
                pageColor = getPageNumberBackgroundLabelColor();
            }
            holder.pageNumber.setBackgroundResource(R.drawable.controls_thumbnails_view_rounded_edges);
            holder.pageNumber.getBackground().setColorFilter(pageColor,
                    PorterDuff.Mode.SRC_ATOP);
            holder.pageNumber.setTextColor(mTheme.pageNumberTextColor);
        }

        holder.checkBox.setVisibility(mEditing ? View.VISIBLE : View.GONE);
        if (mEditing) {
            holder.checkBox.setChecked(holder.itemView.isActivated());
        }

        File thumbFile = mThumbFileMap.get(pageNum);
        if (thumbFile != null) {
            Picasso.get()
                    .load(thumbFile)
                    .into(holder.thumbImage);
        } else {
            try {
                mPdfViewCtrl.getThumbAsync(pageNum);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
            holder.thumbImage.setImageBitmap(null);
        }
    }

    @NonNull
    private LayoutInflater getLayoutInflater() {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(mContext);
        }
        return mLayoutInflater;
    }

    /**
     * The overload implementation of {@link SimpleRecyclerViewAdapter#add(Object)}.
     */
    @Override
    public void add(Integer item) {
        if (mDataList != null && item != null) {
            mDataList.add(item);
        }
    }

    /**
     * The overload implementation of {@link SimpleRecyclerViewAdapter#insert(Object, int)}.
     */
    @Override
    public void insert(Integer item, int position) {
        if (mDataList != null && item != null) {
            mDataList.add(position, item);
        }
    }

    /**
     * The overload implementation of {@link SimpleRecyclerViewAdapter#remove(Object)}.
     */
    @Override
    public boolean remove(Integer item) {
        return (mDataList != null && item != null && mDataList.remove(item));
    }

    /**
     * The overload implementation of {@link SimpleRecyclerViewAdapter#removeAt(int)}.
     */
    @Override
    public Integer removeAt(int location) {
        if (location < 0 || mDataList == null || location >= mDataList.size()) {
            return null;
        }
        return mDataList.remove(location);
    }

    /**
     * The overload implementation of {@link SimpleRecyclerViewAdapter#updateSpanCount(int)}.
     */
    @Override
    public void updateSpanCount(int count) {
        mSpanCount = count;
    }

    /**
     * The overload implementation of {@link ItemTouchHelperAdapter#onItemMove(int, int)}.
     */
    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        if (toPosition < getItemCount()) {
            // Move item
            Integer item = removeAt(fromPosition);
            insert(item, toPosition);
            // Update UI to reflect changes
            notifyItemMoved(fromPosition, toPosition);
            return true;
        }
        return false;
    }

    /**
     * The overload implementation of {@link ItemTouchHelperAdapter#onItemDrop(int, int)}.
     */
    @Override
    public void onItemDrop(int fromPosition, int toPosition) {
        if (fromPosition != RecyclerView.NO_POSITION && toPosition != RecyclerView.NO_POSITION &&
                fromPosition != toPosition) {
            // Update PDFDoc
            moveDocPage(fromPosition, toPosition);
        }
    }

    /**
     * The overload implementation of {@link ItemTouchHelperAdapter#onItemDismiss(int)}.
     */
    @Override
    public void onItemDismiss(int position) {
        // Do nothing
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ThumbAsyncListener#onThumbReceived(int, int[], int, int)}.
     */
    @Override
    public void onThumbReceived(int page, int[] buf, int width, int height) {
        if (sDebug) {
            Log.d(TAG, "onThumbReceived received page: " + Integer.toString(page));
        }
        if (null == mDataList) {
            return;
        }
        int position = getPositionForPage(page);

        if (mTaskList.get(page) == null) {
            if (sDebug) {
                Log.d(TAG, "startLoadBitmapTask for page: " + Integer.toString(page));
            }
            final LoadThumbnailTask task = new LoadThumbnailTask(position, page, buf, width, height); // it is OK for holder to be null
            mTaskList.put(page, task);
            task.execute();
        } else {
            if (sDebug) {
                Log.d(TAG, "A task is already running for page: " + Integer.toString(page));
            }
        }
    }

    /**
     * The overload implementation of {@link PasswordDialogFragment.PasswordDialogFragmentListener#onPasswordDialogPositiveClick(int, File, String, String, String)}.
     */
    @Override
    public void onPasswordDialogPositiveClick(int fileType, File file, String path, String password, String id) {
        addDocPages(mPwdRequestLastPage, DocumentFormat.PDF_DOC, mPwdRequestUri, password);
    }

    /**
     * The overload implementation of {@link PasswordDialogFragment.PasswordDialogFragmentListener#onPasswordDialogPositiveClick(int, File, String, String, String)}.
     */
    @Override
    public void onPasswordDialogNegativeClick(int fileType, File file, String path) {

    }

    /**
     * The overload implementation of {@link PasswordDialogFragment.PasswordDialogFragmentListener#onPasswordDialogPositiveClick(int, File, String, String, String)}.
     */
    @Override
    public void onPasswordDialogDismiss(boolean forcedDismiss) {

    }

    /**
     * Sets the current page.
     *
     * @param pageNum The page number to be set as the current
     */
    public void setCurrentPage(int pageNum) {
        mCurrentPage = pageNum;
    }

    /**
     * @return The current page
     */
    public int getCurrentPage() {
        return mCurrentPage;
    }

    /**
     * @return True if the the pages of the document have been modified
     */
    public boolean getDocPagesModified() {
        return mDocPagesModified;
    }

    /**
     * Updates the main view width
     *
     * @param width The width
     */
    public void updateMainViewWidth(int width) {
        mRecyclerViewWidth = width;
    }

    public void setPageNumberBackgroundLabelColor(@ColorInt int pageColorInt) {
        mPageNumberLabelColor = pageColorInt;
    }

    public int getPageNumberBackgroundLabelColor() {
        return mPageNumberLabelColor;
    }

    public void setActivePageNumberBackgroundLabelColor(@ColorInt int activePageColorInt) {
        mActivePageNumberLabelColor = activePageColorInt;
    }

    public int getActivePageNumberBackgroundLabelColor() {
        return mActivePageNumberLabelColor;
    }

    /**
     * Clears resources.
     */
    public void clearResources() {
        for (Map.Entry<Integer, File> entry : mThumbFileMap.entrySet()) {
            File tmp = entry.getValue();
            if (tmp != null) {
                tmp.delete();
            }
        }
        mThumbFileMap.clear();

        for (File file : mThumbFiles) {
            if (file != null && file.exists()) {
                if (sDebug) {
                    Log.d(TAG, "remove not cleaned up file: " + file.getAbsolutePath());
                }
                file.delete();
            }
        }
        mThumbFiles.clear();
    }

    private void moveDocPage(int fromPosition, int toPosition) {
        mDocPagesModified = true;

        // avoid index out of bounds
        if (fromPosition > -1 && fromPosition < getItemCount() &&
                toPosition > -1 && toPosition < getItemCount()) {
            // Update PDFDoc
            int fromPageNum = fromPosition + 1;
            int toPageNum = toPosition + 1;
            boolean shouldUnlock = false;
            try {
                mPdfViewCtrl.docLock(true);
                shouldUnlock = true;

                // get the page to move
                final PDFDoc doc = mPdfViewCtrl.getDoc();
                if (doc == null) {
                    return;
                }
                Page pageToMove = doc.getPage(fromPageNum);
                if (pageToMove == null) {
                    return;
                }

                // delete original page
                PageIterator itr = doc.getPageIterator(fromPageNum);
                doc.pageRemove(itr);
                // get destination page iterator
                PageIterator moveTo = doc.getPageIterator(toPageNum);
                // copy to destination
                doc.pageInsert(moveTo, pageToMove);

                // update user bookmarks
                updateUserBookmarks(fromPageNum, toPageNum, pageToMove.getSDFObj().getObjNum(),
                        doc.getPage(toPageNum).getSDFObj().getObjNum());

                if (mEditPageListener != null) {
                    mEditPageListener.onPageMoved(fromPageNum, toPageNum);
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    mPdfViewCtrl.docUnlock();
                }
            }

            // Adjust logical page numbers between original and new positions
            boolean currentPageUpdated = false;
            SparseArray<File> tempList = new SparseArray<>();
            for (int i = Math.min(fromPosition, toPosition); i <= Math.max(fromPosition, toPosition); i++) {
                Integer itemMap = getItem(i);
                if (itemMap != null) {
                    int oldPageNum = itemMap;
                    int newPageNum = i + 1;
                    // get old thumbnail
                    File oldThumb = mThumbFileMap.get(oldPageNum);
                    if (!currentPageUpdated && oldPageNum == mCurrentPage) {
                        // Update current page
                        mCurrentPage = newPageNum;
                        currentPageUpdated = true;
                    }
                    setItem(i, newPageNum);
                    if (oldThumb != null) {
                        tempList.put(newPageNum, oldThumb);
                    }
                }
            }
            for (int i = 0; i < tempList.size(); i++) {
                int key = tempList.keyAt(i);
                File file = tempList.get(key);
                mThumbFileMap.put(key, file);
            }

            Utils.safeNotifyDataSetChanged(this);
        }
    }

    private class AddDocPagesTask extends CustomAsyncTask<Void, Void, Void> {

        private static final int MIN_DELAY = 250;
        private ProgressDialog mProgressDialog;
        private CountDownTimer mCountDownTimer;
        private int mPosition;
        private DocumentFormat mDocumentFormat;
        private Object mData;
        private String mPassword;
        private boolean mInsert;
        private int mNewPageNum = 1;
        private boolean mIsNotPdf = false;
        private PDFDoc mDocTemp;

        AddDocPagesTask(Context context, int position, DocumentFormat documentFormat, Object data, String password) {
            super(context);
            mPosition = position;
            mDocumentFormat = documentFormat;
            mData = data;
            mPassword = password;

            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setIndeterminate(true);
            if (documentFormat == DocumentFormat.IMAGE) {
                mProgressDialog.setMessage(context.getResources().getString(R.string.add_image_wait));
                mProgressDialog.setCancelable(false);
            } else {
                mProgressDialog.setMessage(context.getResources().getString(R.string.add_pdf_wait));
                mProgressDialog.setCancelable(true);
            }
            mProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    AddDocPagesTask.this.cancel(true);
                }
            });

            mCountDownTimer = new CountDownTimer(MIN_DELAY, MIN_DELAY + 1) {
                @Override
                public void onTick(long millisUntilFinished) {
                }

                @Override
                public void onFinish() {
                    mProgressDialog.show();
                }
            };
        }

        @Override
        protected void onPreExecute() {
            final Context context = getContext();
            if (context == null) {
                return;
            }
            boolean toastNeeded = true;

            mCountDownTimer.start();
            // get the total page count and push back index

            int pageCount;
            boolean shouldUnlockRead = false;
            try {
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;
                final PDFDoc doc = mPdfViewCtrl.getDoc();
                if (doc == null) {
                    return;
                }
                pageCount = doc.getPageCount();
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
                cancel(true);
                return;
            } finally {
                if (shouldUnlockRead) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }
            if (mPosition < 0) {
                // add page to end of file
                mNewPageNum = pageCount + 1;
            } else {
                mNewPageNum = mPosition + 1;
            }
            mInsert = (mNewPageNum <= pageCount);

            // if another document is going to be added
            mDocTemp = null;
            SecondaryFileFilter filter = null;
            if (mDocumentFormat == DocumentFormat.PDF_DOC || mDocumentFormat == DocumentFormat.IMAGE) {
                boolean shouldUnlock = false;
                boolean canAdd;
                try {
                    ContentResolver cr = Utils.getContentResolver(context);
                    if (cr != null && Utils.isNotPdf(cr, (Uri) mData)) {
                        mIsNotPdf = true;
                        return;
                    }
                    // shouldn't close filter since it is attached to mDocTemp
                    filter = new SecondaryFileFilter(context, (Uri) mData);
                    mDocTemp = new PDFDoc(filter);

                    canAdd = true;
                    mDocTemp.lock();
                    shouldUnlock = true;
                    do {
                        // Is this doc password protected?
                        if (!mDocTemp.initSecurityHandler()) {
                            if (mPassword == null || !mDocTemp.initStdSecurityHandler(mPassword)) {
                                canAdd = false;
                                toastNeeded = false;
                                mPwdRequestLastPage = mPosition;
                                mPwdRequestUri = (Uri) mData;

                                PasswordDialogFragment passwordDialog = PasswordDialogFragment.newInstance(0, null, ((Uri) mData).getEncodedPath(), "");
                                passwordDialog.setListener(ThumbnailsViewAdapter.this);
                                passwordDialog.setMessage(R.string.dialog_password_message);
                                passwordDialog.show(mFragmentManager, "password_dialog");
                                break;
                            }
                        }

                        // Does this doc need XFA rendering?
                        Obj needsRenderingObj = mDocTemp.getRoot().findObj("NeedsRendering");
                        if (needsRenderingObj != null && needsRenderingObj.isBool() && needsRenderingObj.getBool()) {
                            canAdd = false;
                            toastNeeded = false;
                            Utils.showAlertDialogWithLink(context, context.getString(R.string.error_has_xfa_forms_message), "");
                            break;
                        }

                        // Is this doc a package/portfolio?
                        Obj collectionObj = mDocTemp.getRoot().findObj("Collection");
                        if (collectionObj != null) {
                            canAdd = false;
                            toastNeeded = false;
                            Utils.showAlertDialogWithLink(context, context.getString(R.string.error_has_portfolio_message), "");
                            break;
                        }
                    } while (false);
                } catch (Exception e) {
                    canAdd = false;
                } finally {
                    if (shouldUnlock) {
                        Utils.unlockQuietly(mDocTemp);
                    }
                    // note: shouldn't close mDocTemp here, it will be closed later in doInBackground
                    if (mDocTemp == null) {
                        Utils.closeQuietly(filter);
                    }
                }

                if (!canAdd) {
                    mDocTemp = null;
                    if (toastNeeded) {
                        CommonToast.showText(context, context.getResources().getString(R.string.dialog_add_pdf_document_error_message), Toast.LENGTH_SHORT);
                    }
                }
            }
        }

        @SuppressWarnings("WrongThread")
        @Override
        protected Void doInBackground(Void... args) {
            if (isCancelled()) {
                return null;
            }

            boolean shouldUnlockDocTemp = false;
            boolean shouldUnlock = false;
            SecondaryFileFilter filter = null;
            try {
                final PDFDoc doc = mPdfViewCtrl.getDoc();
                if (doc == null) {
                    return null;
                }

                mPdfViewCtrl.docLock(true);
                shouldUnlock = true;

                Page page;
                switch (mDocumentFormat) {
                    case PDF_PAGE:
                        if (mData != null && (mData instanceof Page || mData instanceof Page[])) {
                            Page[] pages;
                            if (mData instanceof Page[]) {
                                pages = (Page[]) mData;
                            } else { // instance of Page
                                pages = new Page[1];
                                pages[0] = (Page) mData;
                            }

                            for (Page p : pages) {
                                if (isCancelled()) {
                                    return null;
                                }
                                if (mInsert) {
                                    PageIterator pageIterator = doc.getPageIterator(mNewPageNum);
                                    doc.pageInsert(pageIterator, p);
                                } else {
                                    doc.pagePushBack(p);
                                }
                            }
                        }
                        break;
                    case BLANK_PDF_PAGE:
                        // create a new blank page and add to end of the document.
                        // the page before the destination is used to set the blank page's size.
                        page = doc.pageCreate();
                        Rect pageRect = getPDFPageRect(mNewPageNum - 1);
                        // uses pageSizeSource's page size
                        page.setMediaBox(pageRect);
                        page.setCropBox(pageRect);
                        if (mInsert) {
                            PageIterator pageIterator = doc.getPageIterator(mNewPageNum);
                            doc.pageInsert(pageIterator, page);
                        } else {
                            doc.pagePushBack(page);
                        }
                        break;
                    case IMAGE:
                    case PDF_DOC:
                        if (mIsNotPdf) {
                            ContentResolver cr = Utils.getContentResolver(getContext());
                            if (cr == null) {
                                return null;
                            }
                            filter = new SecondaryFileFilter(getContext(), (Uri) mData);
                            DocumentConversion conv = Convert.universalConversion(filter, new WordToPDFOptions(ViewerUtils.IMAGE_DPI_CONVERSION_OPTION));
                            while (conv.getConversionStatus() == DocumentConversion.e_incomplete) {
                                conv.convertNextPage();
                                if (isCancelled()) {
                                    return null;
                                }
                            }

                            if (conv.getConversionStatus() == DocumentConversion.e_failure || conv.getConversionStatus() != DocumentConversion.e_success) {
                                break;
                            }

                            mDocTemp = conv.getDoc();
                        }

                        if (isCancelled()) {
                            return null;
                        }

                        if (mDocTemp != null) {
                            mDocTemp.lock();
                            shouldUnlockDocTemp = true;
                            for (int p = 1; p <= mDocTemp.getPageCount(); p++) {
                                Page newPage = mDocTemp.getPage(p);
                                ViewerUtils.renameAllFields(newPage);
                            }
                            doc.insertPages(mNewPageNum, mDocTemp, 1, mDocTemp.getPageCount(), PDFDoc.InsertBookmarkMode.INSERT, null);
                        }
                        break;
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e, "AddDocPagesTask");
                return null;
            } finally {
                if (shouldUnlock) {
                    mPdfViewCtrl.docUnlock();
                }
                if (shouldUnlockDocTemp) {
                    Utils.unlockQuietly(mDocTemp);
                }
                Utils.closeQuietly(mDocTemp, filter);
            }

            return null;
        }

        @Override
        protected void onCancelled() {
            mCountDownTimer.cancel();
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
        }

        @Override
        protected void onPostExecute(Void arg) {
            Context context = getContext();
            if (context == null) {
                return;
            }
            mCountDownTimer.cancel();
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }

            if (!isCancelled()) {
                int pageCount;
                boolean shouldUnlockRead = false;
                try {
                    mPdfViewCtrl.docLockRead();
                    shouldUnlockRead = true;
                    final PDFDoc doc = mPdfViewCtrl.getDoc();
                    if (doc == null) {
                        return;
                    }
                    pageCount = doc.getPageCount();
                } catch (Exception ex) {
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                    CommonToast.showText(context, context.getResources().getString(R.string.dialog_add_pdf_document_error_message), Toast.LENGTH_SHORT);
                    return;
                } finally {
                    if (shouldUnlockRead) {
                        mPdfViewCtrl.docUnlockRead();
                    }
                }
                int pageAddedCnt = pageCount - mDataList.size();

                clear();
                for (int pageNum = 1; pageNum <= pageCount; pageNum++) {
                    add(pageNum);
                }

                Utils.safeNotifyDataSetChanged(ThumbnailsViewAdapter.this);
                safeScrollToPosition(mNewPageNum - 1);

                List<Integer> pageList = new ArrayList<>(pageAddedCnt);
                for (int i = 0; i < pageAddedCnt; i++) {
                    pageList.add(mNewPageNum + i);
                }

                if (mEditPageListener != null) {
                    mEditPageListener.onPagesAdded(pageList);
                }
            }
        }
    }

    private class DuplicateDocPagesTask extends CustomAsyncTask<Void, Void, Void> {

        private static final int MIN_DELAY = 250;
        private ProgressDialog mProgressDialog;
        private CountDownTimer mCountDownTimer;
        private List<Integer> mPageList;
        private int mNewPageNum = 1;

        DuplicateDocPagesTask(Context context, List<Integer> pageList) {
            super(context);
            mPageList = pageList;

            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage(context.getResources().getString(R.string.add_pdf_wait));
            mProgressDialog.setCancelable(true);

            mProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    cancel(true);
                }
            });

            mCountDownTimer = new CountDownTimer(MIN_DELAY, MIN_DELAY + 1) {
                @Override
                public void onTick(long millisUntilFinished) {
                }

                @Override
                public void onFinish() {
                    mProgressDialog.show();
                }
            };
        }

        @Override
        protected void onPreExecute() {
            mCountDownTimer.start();
        }

        @SuppressWarnings("WrongThread")
        @Override
        protected Void doInBackground(Void... args) {
            if (isCancelled()) {
                return null;
            }

            boolean shouldUnlock = false;
            try {
                final PDFDoc doc = mPdfViewCtrl.getDoc();
                if (doc == null) {
                    return null;
                }
                mPdfViewCtrl.docLock(true);
                shouldUnlock = true;

                // add duplicated pages after the last selected page
                Collections.sort(mPageList, Collections.<Integer>reverseOrder());
                int lastSelectedPage = mPageList.get(0);
                mNewPageNum = lastSelectedPage + 1;
                int count = mPageList.size();
                for (int i = 0; i < count; ++i) {
                    if (isCancelled()) {
                        break;
                    }
                    Page page = doc.getPage(mPageList.get(i));
                    PageIterator pageIterator = doc.getPageIterator(mNewPageNum);
                    doc.pageInsert(pageIterator, page);
                    Page newPage = doc.getPage(mNewPageNum);
                    ViewerUtils.renameAllFields(newPage);
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
                return null;
            } finally {
                if (shouldUnlock) {
                    mPdfViewCtrl.docUnlock();
                }
            }

            return null;
        }

        @Override
        protected void onCancelled() {
            mCountDownTimer.cancel();
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
        }

        @Override
        protected void onPostExecute(Void arg) {
            Context context = getContext();
            if (context == null) {
                return;
            }

            mCountDownTimer.cancel();
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }

            if (!isCancelled()) {
                int pageCount;
                boolean shouldUnlockRead = false;
                try {
                    mPdfViewCtrl.docLockRead();
                    shouldUnlockRead = true;
                    final PDFDoc doc = mPdfViewCtrl.getDoc();
                    if (doc == null) {
                        return;
                    }
                    pageCount = doc.getPageCount();
                } catch (Exception ex) {
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                    CommonToast.showText(context, context.getResources().getString(R.string.dialog_add_pdf_document_error_message), Toast.LENGTH_SHORT);
                    return;
                } finally {
                    if (shouldUnlockRead) {
                        mPdfViewCtrl.docUnlockRead();
                    }
                }
                int pageAddedCnt = pageCount - mDataList.size();

                clear();
                for (int pageNum = 1; pageNum <= pageCount; pageNum++) {
                    add(pageNum);
                }

                Utils.safeNotifyDataSetChanged(ThumbnailsViewAdapter.this);
                safeScrollToPosition(mNewPageNum - 1);

                List<Integer> pageList = new ArrayList<>(pageAddedCnt);
                for (int i = 0; i < pageAddedCnt; i++) {
                    pageList.add(mNewPageNum + i);
                }

                if (mEditPageListener != null) {
                    mEditPageListener.onPagesAdded(pageList);
                }
            }
        }
    }

    /**
     * Updates the adapter after pages addition.
     *
     * @param pageList The list of pages added to the document
     */
    public void updateAfterAddition(List<Integer> pageList) {
        int pageNum = Collections.min(pageList);
        mDocPagesModified = true;
        int pageCount;
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            final PDFDoc doc = mPdfViewCtrl.getDoc();
            if (doc == null) {
                return;
            }
            pageCount = doc.getPageCount();
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
            return;
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }

        try {
            clear();
            for (int p = 1; p <= pageCount; p++) {
                add(p);
            }
        } catch (Exception ignored) {
        }

        Utils.safeNotifyDataSetChanged(this);
        safeScrollToPosition(pageNum - 1);
    }

    /**
     * Updates the adapter after pages deletion.
     *
     * @param pageList The list of pages removed from the document
     */
    public void updateAfterDeletion(List<Integer> pageList) {
        if (pageList == null || pageList.size() == 0) {
            return;
        }

        mDocPagesModified = true;
        mCurrentPage -= pageList.size();

        Collections.sort(pageList); // since we will use binary search

        // Update page numbers
        ListIterator<Integer> it = mDataList.listIterator();
        int deleteCnt = 0;
        Integer pageNum;
        SparseArray<File> tempList = new SparseArray<>();
        while (it.hasNext()) {
            Integer item = it.next();
            pageNum = item;
            try {
                if (Collections.binarySearch(pageList, pageNum) >= 0) {
                    it.remove();

                    // Update cached page list
                    removeCachedPage(pageNum);

                    ++deleteCnt;
                } else {
                    File tmp = mThumbFileMap.get(pageNum);
                    int newPageNum = pageNum - deleteCnt;
                    setItem(it.previousIndex(), newPageNum);
                    if (tmp != null) {
                        tempList.put(newPageNum, tmp);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        for (int i = 0; i < tempList.size(); i++) {
            int key = tempList.keyAt(i);
            File file = tempList.get(key);
            mThumbFileMap.put(key, file);
        }

        Utils.safeNotifyDataSetChanged(this);

        // scroll to the item after the first deleted item
        int firstPageChanged = Collections.min(pageList);
        if (firstPageChanged == mDataList.size()) {
            --firstPageChanged;
        }
        safeScrollToPosition(firstPageChanged - 1);
    }

    /**
     * Updates the adapter after pages rotation.
     *
     * @param pageList The list of rotated pages
     */
    public void updateAfterRotation(List<Integer> pageList) {
        if (pageList == null || pageList.size() == 0) {
            return;
        }

        mDocPagesModified = true;

        Collections.sort(pageList); // since we will use binary search, safeScrollToPosition (end of method) depends on this

        // Update page numbers
        ListIterator<Integer> it = mDataList.listIterator();
        Integer pageNum = 1;
        while (it.hasNext()) {
            Integer item = it.next();
            pageNum = item;
            try {
                if (Collections.binarySearch(pageList, pageNum) >= 0) {
                    // Update cached page list
                    removeCachedPage(pageNum);
                }
            } catch (Exception ignored) {
            }
        }

        Utils.safeNotifyDataSetChanged(this);
        // Scroll to the first page that got modified, can call get(0) since pageList is sorted
        safeScrollToPosition(pageList.get(0) - 1);
    }

    /**
     * Updates the adapter after editing page labels.
     */
    public void updateAfterPageLabelEdit() {
        Utils.safeNotifyDataSetChanged(this);
    }

    /**
     * Updates the adapter after page movement.
     *
     * @param fromPageNum The page number from which the page was moved
     * @param toPageNum   The page number to which the page was moved
     */
    public void updateAfterMove(int fromPageNum, int toPageNum) {
        try {
            int start = Math.min(fromPageNum - 1, toPageNum - 1);
            int end = Math.max(fromPageNum - 1, toPageNum - 1);
            boolean currentPageUpdated = false;
            //check if mCurrent page is being moved. This covers scenarios where the mCurrentPage is moved more than one positions (eg move from position 1 to 5)
            if (fromPageNum == mCurrentPage) {
                mCurrentPage = toPageNum;
                currentPageUpdated = true;
            }

            if (start >= 0 && end < getItemCount() && start != end) {
                // Adjust logical page numbers and thumbnails between original and new positions
                if (fromPageNum > toPageNum) {
                    Integer itemMap = getItem(end);
                    if (itemMap != null) {
                        mDataLock.lock();
                        for (int i = start; i <= end; ++i) {
                            itemMap = getItem(i);
                            if (itemMap == null) {
                                break;
                            }
                            int oldPageNum = itemMap;
                            int currentPageNum = i + 1;
                            if (!currentPageUpdated && oldPageNum == mCurrentPage) {
                                // Update current page
                                int newPageNumber = oldPageNum + 1;
                                if (newPageNumber <= getItemCount() + 1) {
                                    mCurrentPage = newPageNumber;
                                }
                                currentPageUpdated = true;
                            }
                            mPdfViewCtrl.getThumbAsync(currentPageNum);
                        }
                        mDataLock.unlock();
                    }
                } else {
                    Integer itemMap = getItem(start);
                    if (itemMap != null) {
                        mDataLock.lock();
                        for (int i = end; i >= start; --i) {
                            itemMap = getItem(i);
                            if (itemMap == null) {
                                break;
                            }
                            int oldPageNum = itemMap;
                            int newPageNum = i + 1;
                            if (!currentPageUpdated && oldPageNum == mCurrentPage) {
                                // Update current page
                                int newPageNumber = oldPageNum - 1;
                                if (newPageNumber > 0) {
                                    mCurrentPage = newPageNumber;
                                }
                                currentPageUpdated = true;
                            }
                            mPdfViewCtrl.getThumbAsync(newPageNum);
                        }
                        mDataLock.unlock();
                    }
                }
                Utils.safeNotifyDataSetChanged(this);
                safeScrollToPosition(toPageNum - 1);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    private void safeScrollToPosition(int scrollIndex) {
        final RecyclerView recyclerView = getRecyclerView();
        if (recyclerView != null) {
            boolean scrollToPosition;
            if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int firstVisibleIndex = layoutManager.findFirstVisibleItemPosition();
                int lastVisibleIndex = layoutManager.findLastVisibleItemPosition();
                scrollToPosition = (scrollIndex < firstVisibleIndex || scrollIndex > lastVisibleIndex);
            } else {
                scrollToPosition = true;
            }
            if (scrollToPosition) {
                // View-holder is not ready or not in view - scroll to its position
                recyclerView.scrollToPosition(scrollIndex);
            }
        }
    }

    /**
     * Adds document pages at the specified (zero-indexed) position, or to end of file if position is -1.
     * Lastly scroll to newly created page, if necessary
     *
     * @param position       The position where the pages are added on
     * @param documentFormat The document format
     * @param data           The extra data including page(s)
     */
    public void addDocPages(int position, DocumentFormat documentFormat, Object data) {
        mDocPagesModified = true;
        new AddDocPagesTask(mContext, position, documentFormat, data, null).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Adds document pages at the specified (zero-indexed) position, or to end of file if position is -1.
     * Lastly scroll to newly created page, if necessary
     *
     * @param position       The position where the pages are inserted to
     * @param documentFormat The document format
     * @param data           The extra data including page(s)
     * @param password       The document password
     */
    public void addDocPages(int position, DocumentFormat documentFormat, Object data, String password) {
        mDocPagesModified = true;
        new AddDocPagesTask(mContext, position, documentFormat, data, password).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Duplicates pages.
     *
     * @param pageList The list of pages to be duplicated
     */
    public void duplicateDocPages(List<Integer> pageList) {
        mDocPagesModified = true;
        new DuplicateDocPagesTask(mContext, pageList).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Removes a document page.
     *
     * @param pageNum The page number to be removed
     */
    public void removeDocPage(int pageNum) {
        mDocPagesModified = true;

        boolean shouldUnlock = false;
        try {
            final PDFDoc doc = mPdfViewCtrl.getDoc();
            if (doc == null) {
                return;
            }

            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            Page pageToDelete = doc.getPage(pageNum);
            PageIterator pageIterator = doc.getPageIterator(pageNum);
            doc.pageRemove(pageIterator);

            removeUserBookmarks(pageToDelete.getSDFObj().getObjNum(), pageNum, doc.getPageCount());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }

        int position = updatePageNumberOnDelete(pageNum);
        if (position >= 0) {
            notifyItemRemoved(position);
        }
    }

    /**
     * Rotates the document page clockwise.
     *
     * @param pageNum The page number to be rotated
     */
    public void rotateDocPage(int pageNum) {
        rotateDocPage(pageNum, true);
    }

    /**
     * Rotates the document page.
     *
     * @param pageNum   The page number to be rotated
     * @param clockwise Direction of the rotation
     */
    public void rotateDocPage(int pageNum, boolean clockwise) {
        mDocPagesModified = true;
        boolean shouldUnlock = false;
        try {
            final PDFDoc doc = mPdfViewCtrl.getDoc();
            if (doc == null) {
                return;
            }
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            Page pageToRotate = doc.getPage(pageNum);
            int pageRotation = (pageToRotate.getRotation() + 1) % 4;
            if (!clockwise) {
                pageRotation = (pageToRotate.getRotation() + 3) % 4;
            }
            pageToRotate.setRotation(pageRotation);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }

        int position = getPositionForPage(pageNum);
        if (position < 0) {
            // fallback - assume that position is pageNum-1
            position = pageNum - 1;
        }

        removeCachedPage(pageNum); // page-indexed

        Utils.safeNotifyItemChanged(this, position);
    }

    private Rect getPDFPageRect(int pageNum) throws PDFNetException {
        final PDFDoc doc = mPdfViewCtrl.getDoc();
        if (doc == null) {
            return new Rect(0, 0, 0, 0);
        }
        Page page = doc.getPage(pageNum);
        double width = page.getPageWidth();
        double height = page.getPageHeight();
        return new Rect(0, 0, width, height);
    }

    private void updateUserBookmarks(int from, int to, Long pageToMoveObjNum, Long destPageObjNum) {
        try {
            final PDFDoc doc = mPdfViewCtrl.getDoc();
            if (doc == null) {
                return;
            }
            String filepath = doc.getFileName();
            ToolManager toolManager;
            try {
                toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            } catch (Exception e) {
                toolManager = null;
            }
            if (toolManager != null && !toolManager.isReadOnly()) {
                BookmarkManager.onPageMoved(mPdfViewCtrl, pageToMoveObjNum, destPageObjNum, to, false);
            } else {
                BookmarkManager.onPageMoved(mContext, mPdfViewCtrl, filepath, pageToMoveObjNum, destPageObjNum, from, to);
            }
        } catch (PDFNetException ignored) {
        }
    }

    private void removeUserBookmarks(Long objNum, int pageNum, int pageCount) {
        try {
            final PDFDoc doc = mPdfViewCtrl.getDoc();
            if (doc == null) {
                return;
            }
            String filepath = doc.getFileName();
            ToolManager toolManager;
            try {
                toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            } catch (Exception e) {
                toolManager = null;
            }
            if (toolManager != null && !toolManager.isReadOnly()) {
                BookmarkManager.onPageDeleted(mPdfViewCtrl, objNum);
            } else {
                BookmarkManager.onPageDeleted(mContext, mPdfViewCtrl, filepath, objNum, pageNum, pageCount);
            }
        } catch (PDFNetException ignored) {
        }
    }

    // This class loads processes bitmaps off the UI thread
    private class LoadThumbnailTask extends AsyncTask<Void, Void, Bitmap> {

        private final int mPosition;
        private final int mPage;

        private int mWidth;
        private int mHeight;
        private int[] mBuffer;

        LoadThumbnailTask(int position, int page, int[] buffer, int width, int height) {
            this.mPage = page;
            this.mPosition = position;
            this.mBuffer = buffer;
            this.mWidth = width;
            this.mHeight = height;
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            Bitmap bitmap = null;

            // wait if work is paused and the task is not cancelled
            synchronized (mPauseWorkLock) {
                while (mPauseWork && !isCancelled()) {
                    try {
                        if (sDebug) {
                            Log.d(TAG, "doInBackground - paused for page: " + Integer.toString(mPage));
                        }
                        mPauseWorkLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                if (mBuffer != null && mBuffer.length > 0) {
                    bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
                    bitmap.setPixels(mBuffer, 0, mWidth, 0, 0, mWidth, mHeight);

                    FileOutputStream fos = null;
                    try {
                        File prev = mThumbFileMap.get(mPage);
                        if (prev != null) {
                            prev.delete();
                            mThumbFileMap.remove(mPage);
                        }
                        File tmp = File.createTempFile("tmp", ".png");
                        mThumbFiles.add(tmp);
                        if (sDebug) {
                            Log.d(TAG, "create bitmap for page: " + mPage);
                        }
                        fos = new FileOutputStream(tmp);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        mThumbFileMap.put(mPage, tmp);
                    } catch (Exception e) {
                        AnalyticsHandlerAdapter.getInstance().sendException(e);
                    } finally {
                        Utils.closeQuietly(fos);
                    }

                    if (sDebug) {
                        Log.d(TAG, "doInBackground - finished work for page: " + Integer.toString(mPage));
                    }
                } else {
                    if (sDebug) {
                        Log.d(TAG, "doInBackground - Buffer is empty for page: " + Integer.toString(mPage));
                    }
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } catch (OutOfMemoryError oom) {
                Utils.manageOOM(mPdfViewCtrl);
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (sDebug) {
                Log.d(TAG, "onPostExecute " + Integer.toString(mPage));
            }
            if (isCancelled()) {
                if (sDebug) {
                    Log.d(TAG, "onPostExecute cancelled");
                }
                mTaskList.remove(mPage);
                return;
            }

            if (result != null) {
                if (sDebug) {
                    Log.d(TAG, "onPostExecute - notify change for page: " + mPage + " at position: " + mPosition);
                }
                Utils.safeNotifyItemChanged(ThumbnailsViewAdapter.this, mPosition);
            }
            mTaskList.remove(mPage);
        }

        @Override
        protected void onCancelled(Bitmap value) {
            if (sDebug) {
                Log.d(TAG, "onCancelled " + Integer.toString(mPage));
            }
            synchronized (mPauseWorkLock) {
                mPauseWorkLock.notifyAll();
            }
            mTaskList.remove(mPage);
        }
    }

    public int getPositionForPage(final int pageNum) {
        if (mDataList != null) {
            return mDataList.indexOf(pageNum);
        }
        return -1;
    }

    /**
     * Updates the page numbers after deleting a page
     *
     * @param deletedPage The page number of the page deleted
     * @return The deleted position
     */
    public int updatePageNumberOnDelete(final int deletedPage) {
        int deletedPosition = -1;

        // Update current page number
        if (deletedPage == mCurrentPage) {
            int pageCount;
            boolean shouldUnlockRead = false;
            try {
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;
                final PDFDoc doc = mPdfViewCtrl.getDoc();
                if (doc == null) {
                    return deletedPosition;
                }
                pageCount = doc.getPageCount();
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
                return deletedPosition;
            } finally {
                if (shouldUnlockRead) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }
            if (deletedPage >= pageCount) {
                mCurrentPage--;
            }
        } else if (mCurrentPage > deletedPage) {
            mCurrentPage--;
        }

        // Update page numbers
        ListIterator<Integer> it = mDataList.listIterator();
        while (it.hasNext()) {
            Integer item = it.next();
            int page = item;
            try {
                if (page > deletedPage) {
                    setItem(it.previousIndex(), page - 1);
                } else if (page == deletedPage) {
                    deletedPosition = it.previousIndex();
                    it.remove();
                }
            } catch (Exception ignored) {
            }
        }
        // Update cached page list
        removeCachedPage(deletedPage);

        return deletedPosition;
    }

    private void removeCachedPage(int pageNum) {
        File tmp = mThumbFileMap.get(pageNum);
        if (tmp != null) {
            tmp.delete();
            mThumbFileMap.remove(pageNum);
        }
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {

        RelativeLayout imageLayout;
        ImageView thumbImage;
        TextView pageNumber;
        CheckBox checkBox;

        PageViewHolder(View itemView) {
            super(itemView);

            this.imageLayout = itemView.findViewById(R.id.item_image_layout);
            this.thumbImage = itemView.findViewById(R.id.item_image);
            this.pageNumber = itemView.findViewById(R.id.item_text);
            this.checkBox = itemView.findViewById(R.id.item_check);
        }
    }

    public static void setDebug(boolean debug) {
        sDebug = debug;
    }
}
