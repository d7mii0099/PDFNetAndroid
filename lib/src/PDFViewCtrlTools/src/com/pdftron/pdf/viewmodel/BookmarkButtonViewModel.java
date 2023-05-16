package com.pdftron.pdf.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import com.pdftron.pdf.model.BookmarkButtonState;
import com.pdftron.pdf.model.PageState;
import com.pdftron.pdf.utils.cache.BookmarksCache;

/**
 * View Model in charge of the quick bookmark creation button.
 */
public class BookmarkButtonViewModel extends AndroidViewModel {

    @NonNull
    private final MediatorLiveData<BookmarkButtonState> mBookmarkUpdate = new MediatorLiveData<>();
    @Nullable
    private LiveData<PageState> mPage = null;
    @Nullable
    private LiveData<BookmarksCache> mBookmarks = null;

    public BookmarkButtonViewModel(@NonNull Application application) {
        super(application);
    }

    public final void observe(LifecycleOwner owner, Observer<BookmarkButtonState> observer) {
        mBookmarkUpdate.observe(owner, observer);
    }

    public void attachPageData(@NonNull LiveData<PageState> page) {
        if (mPage != null) {
            mBookmarkUpdate.removeSource(mPage);
        }
        mPage = page;
        mBookmarkUpdate.addSource(page, new Observer<PageState>() {
            @Override
            public void onChanged(PageState pageState) {
                updateState();
            }
        });
    }

    public void attachBookmarkData(@NonNull LiveData<BookmarksCache> bookmarkCache) {
        if (mBookmarks != null) {
            mBookmarkUpdate.removeSource(mBookmarks);
        }
        mBookmarks = bookmarkCache;
        mBookmarkUpdate.addSource(bookmarkCache, new Observer<BookmarksCache>() {
            @Override
            public void onChanged(BookmarksCache bookmarksCache) {
                updateState();
            }
        });
    }

    private void updateState() {
        if (mBookmarks != null && mPage != null) {
            PageState page = mPage.getValue();
            BookmarksCache bookmarksCache = mBookmarks.getValue();
            if (bookmarksCache != null && page != null) {
                mBookmarkUpdate.setValue(
                        new BookmarkButtonState(bookmarksCache.containsBookmark(page.getCurrentPage()))
                );
            }
        }
    }
}
