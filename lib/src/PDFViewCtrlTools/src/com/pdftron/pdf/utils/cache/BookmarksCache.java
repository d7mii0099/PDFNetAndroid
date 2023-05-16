package com.pdftron.pdf.utils.cache;

import android.content.Context;
import androidx.annotation.NonNull;

import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.model.UserBookmarkItem;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.BookmarkManager;
import com.pdftron.pdf.widget.base.BaseObservable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cache used to hold update-to-date state of the bookmark list.
 */
public class BookmarksCache extends BaseObservable {

    @NonNull
    private List<UserBookmarkItem> mBookmarkItems = new ArrayList<>();

    public void setBookmarkItems(@NonNull List<UserBookmarkItem> bookmarkItems) {
        mBookmarkItems = bookmarkItems;
        notifyChange();
    }

    public List<UserBookmarkItem> getBookmarkItems() {
        return Collections.unmodifiableList(mBookmarkItems);
    }

    /**
     * Updates the cache with bookmarks from the given PDFViewCtrl
     *
     * @param pdfViewCtrl The PDFViewCtrl
     * @param isReadOnly  Whether the document is read only
     */
    public void updateCache(@NonNull PDFViewCtrl pdfViewCtrl, boolean isReadOnly) {
        Context context = pdfViewCtrl.getContext();
        PDFDoc doc = pdfViewCtrl.getDoc();
        if (doc == null) {
            return;
        }
        boolean shouldUnlockRead = false;
        List<UserBookmarkItem> bookmarks = null;
        try {
            pdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            if (isReadOnly) {
                bookmarks = BookmarkManager.getUserBookmarks(context, doc.getFileName(), null);
            } else {
                bookmarks = BookmarkManager.getPdfBookmarks(doc);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                pdfViewCtrl.docUnlockRead();
            }
        }
        if (bookmarks != null) {
            setBookmarkItems(bookmarks);
        }
    }

    /**
     * Checks whether the specified page contains a bookmark. If the document is read only, then
     * will check user bookmarks, otherwise check PDF bookmarks.
     *
     * @param pageNum The page to check for bookmarks
     * @return whether the specified page contains a bookmark
     */
    public boolean containsBookmark(int pageNum) {
        if (!mBookmarkItems.isEmpty()) {
            for (UserBookmarkItem bookmarkItem : mBookmarkItems) {
                if (bookmarkItem.pageNumber == pageNum) {
                    return true;
                }
            }
        }
        return false;
    }
}
