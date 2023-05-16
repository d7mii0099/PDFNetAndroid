package com.pdftron.pdf.viewmodel;

import android.app.Application;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.model.UserBookmarkItem;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.pdf.utils.cache.BookmarksCache;
import com.pdftron.pdf.widget.base.ObservingLiveData;

import java.util.List;

/**
 * View model containing up-to-date bookmark information in the document.
 */
public class BookmarksViewModel extends AndroidViewModel {

    @Nullable
    private ObservingLiveData<BookmarksCache> mBookmarks = new ObservingLiveData<>(new BookmarksCache());

    public BookmarksViewModel(@NonNull Application application) {
        super(application);
    }

    @Nullable
    public LiveData<BookmarksCache> getBookmarks() {
        return mBookmarks;
    }

    public final void observe(LifecycleOwner owner, Observer<BookmarksCache> observer) {
        if (mBookmarks != null) {
            mBookmarks.observe(owner, observer);
        }
    }

    public void loadBookmarks(@NonNull PDFViewCtrl pdfViewCtrl, boolean isReadOnly) {
        if (mBookmarks != null) {
            BookmarksCache bookmarksCache = mBookmarks.getValue();
            if (bookmarksCache != null) {
                bookmarksCache.updateCache(pdfViewCtrl, isReadOnly);
            }
        }
    }

    public void setBookmarks(@NonNull List<UserBookmarkItem> bookmarkItems) {
        if (mBookmarks != null) {
            BookmarksCache bookmarksCache = mBookmarks.getValue();
            if (bookmarksCache != null) {
                bookmarksCache.setBookmarkItems(bookmarkItems);
            }
        }
    }

    public void toggleBookmark(@NonNull Context context, boolean isReadOnly,
            @NonNull PDFViewCtrl pdfViewCtrl, int page) {
        if (mBookmarks != null && mBookmarks.getValue() != null) {
            if (mBookmarks.getValue().containsBookmark(page)) {
                ViewerUtils.removePageBookmark(context, isReadOnly, pdfViewCtrl, page);
            } else {
                ViewerUtils.addPageToBookmark(context, isReadOnly, pdfViewCtrl, page);
            }
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mBookmarks = null;
    }
}
