package com.pdftron.demo.browser.ui;

import android.app.Application;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.pdftron.pdf.utils.Constants;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;

import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.BehaviorSubject;

public class FilterSettingsViewModel extends AndroidViewModel {

    @NonNull
    private String mSearchQuery = "";
    private boolean mShowAll;
    private boolean mFilterPdf;
    private boolean mFilterOffice;
    private boolean mFilterImage;
    private boolean mFilterText;
    private boolean mSortByName;
    private boolean mSortByDate;
    @IntRange(from=0, to=6)
    private int mGridCount; // 0 means it's in list mode

    private final MutableLiveData<FilterState> mFilterStateForUi = new MutableLiveData<>();
    private final BehaviorSubject<FilterState> mFilterStateForFileFetcher = BehaviorSubject.create();

    private String mSettingsSuffix;

    public FilterSettingsViewModel(@NonNull Application application) {
        super(application);
        initializeViews();
    }

    public void initializeViews() {
        mSettingsSuffix = PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_LOCAL_FILES;

        // Load shared pref for document filtering and sorting
        Application application = getApplication();
        mFilterPdf = PdfViewCtrlSettingsManager.getFileFilter(application,
                Constants.FILE_TYPE_PDF, mSettingsSuffix);
        mFilterOffice = PdfViewCtrlSettingsManager.getFileFilter(application,
                Constants.FILE_TYPE_DOC, mSettingsSuffix);
        mFilterImage = PdfViewCtrlSettingsManager.getFileFilter(application,
                Constants.FILE_TYPE_IMAGE, mSettingsSuffix);
        mFilterText = PdfViewCtrlSettingsManager.getFileFilter(application,
                Constants.FILE_TYPE_TEXT, mSettingsSuffix);
        mShowAll = !mFilterPdf && !mFilterOffice && !mFilterImage && !mFilterText;
        mSortByName = PdfViewCtrlSettingsManager.getSortMode(application).equals(PdfViewCtrlSettingsManager.KEY_PREF_SORT_BY_NAME);
        mSortByDate = PdfViewCtrlSettingsManager.getSortMode(application).equals(PdfViewCtrlSettingsManager.KEY_PREF_SORT_BY_DATE);

        mGridCount = PdfViewCtrlSettingsManager.getGridSize(application, mSettingsSuffix);

        // Create the new filter state and emit it
        FilterState newFilterState = new FilterState(mSearchQuery, mShowAll,
                mFilterPdf, mFilterOffice, mFilterImage, mFilterText, getSortMode(), mGridCount);
        mFilterStateForUi.setValue(newFilterState);
        mFilterStateForFileFetcher.onNext(newFilterState);
    }

    private SortMode getSortMode() {
        if (mSortByName && !mSortByDate) {
            return SortMode.NAME;
        } else if (mSortByDate && !mSortByName) {
            return SortMode.DATE_MODIFIED;
        } else {
            throw new RuntimeException("Invalid sort state");
        }
    }

    void setSearchQuery(@Nullable String newSearchQuery) {
        mSearchQuery = newSearchQuery != null ? newSearchQuery : "";
        emitFilterStateIfChanged();
    }

    void toggleFileFilter(int fileTypePdf) {
        switch (fileTypePdf) {
            case Constants.FILE_TYPE_PDF: {
                mFilterPdf = !mFilterPdf;
                break;
            }
            case Constants.FILE_TYPE_DOC: {
                mFilterOffice = !mFilterOffice;
                break;
            }
            case Constants.FILE_TYPE_IMAGE: {
                mFilterImage = !mFilterImage;
                break;
            }
            case Constants.FILE_TYPE_TEXT: {
                mFilterText = !mFilterText;
                break;
            }
        }
        updateShowAllState();
    }

    void toggleSortByName() {
        if (!mSortByName) {
            mSortByName = true;
            mSortByDate = false;
            updateFilterState();
        }
    }

    void toggleSortByDate() {
        if (!mSortByDate) {
            mSortByDate = true;
            mSortByName = false;
            updateFilterState();
        }
    }

    void toggleFilterAll() {
        // Only change toggle if it is not already enabled
        if (!mShowAll) {
            mShowAll = true;
            updateFilterState();
        }
    }

    void setGridCount(@IntRange(from=0, to=6) int gridCount) {
        mGridCount = gridCount;
        emitFilterStateIfChanged();
    }

    @IntRange(from=0, to=6)
    int getGridCount() {
        return mGridCount;
    }

    void observeFilterUiUpdates(LifecycleOwner owner, Observer<FilterState> observer) {
        mFilterStateForUi.observe(owner, observer);
    }

    Disposable observeListUpdates(@NonNull Consumer<FilterState> consumer) {
        return mFilterStateForFileFetcher.debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(consumer, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throw new RuntimeException("Error occurred observing item list", throwable);
                    }
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }

    private void emitFilterStateIfChanged() {
        // Create the new filter state
        FilterState newFilterState = new FilterState(mSearchQuery, mShowAll,
                mFilterPdf, mFilterOffice, mFilterImage, mFilterText, getSortMode(), mGridCount);

        // Now compare the old and new filter states
        FilterState oldFilterState = mFilterStateForUi.getValue();
        if (oldFilterState == null || !oldFilterState.equals(newFilterState)) {
            updateSharedPreferences();
            // Update UI
            mFilterStateForUi.setValue(newFilterState);
            // Send event to update the list data
            mFilterStateForFileFetcher.onNext(newFilterState);
        }
    }

    private void updateSharedPreferences() {
        Application application = getApplication();

        // Update Filter Type
        PdfViewCtrlSettingsManager.updateFileFilter(
                application,
                Constants.FILE_TYPE_PDF,
                mSettingsSuffix,
                mFilterPdf
        );
        PdfViewCtrlSettingsManager.updateFileFilter(
                application,
                Constants.FILE_TYPE_DOC,
                mSettingsSuffix,
                mFilterOffice
        );
        PdfViewCtrlSettingsManager.updateFileFilter(
                application,
                Constants.FILE_TYPE_IMAGE,
                mSettingsSuffix,
                mFilterImage
        );
        PdfViewCtrlSettingsManager.updateFileFilter(
                application,
                Constants.FILE_TYPE_TEXT,
                mSettingsSuffix,
                mFilterText
        );

        // Update sort order
        if (mSortByName && !mSortByDate) {
            PdfViewCtrlSettingsManager.updateSortMode(
                    application,
                    PdfViewCtrlSettingsManager.KEY_PREF_SORT_BY_NAME
            );
        } else if (mSortByDate && !mSortByName) {
            PdfViewCtrlSettingsManager.updateSortMode(
                    application,
                    PdfViewCtrlSettingsManager.KEY_PREF_SORT_BY_DATE
            );
        }

        // Update item list mode (grid or list)
        PdfViewCtrlSettingsManager.updateGridSize(application, mSettingsSuffix, mGridCount);
    }

    private void updateShowAllState() {
        mShowAll = !mFilterPdf && !mFilterOffice && !mFilterImage && !mFilterText;
        emitFilterStateIfChanged();
    }

    private void updateFilterState() {
        if (mShowAll) {
            mFilterPdf = false;
            mFilterOffice = false;
            mFilterImage = false;
            mFilterText = false;
        }
        emitFilterStateIfChanged();
    }

    public FilterState getCurrentUIState() {
        return mFilterStateForUi.getValue();
    }

    public enum SortMode {
        NAME, DATE_MODIFIED
    }

    static class FilterState {

        @NonNull
        final String searchQuery;
        final boolean shouldShowAll;
        final boolean shouldFilterPdf;
        final boolean shouldFilterOffice;
        final boolean shouldFilterImage;
        final boolean shouldFilterText;
        @NonNull
        final FilterSettingsViewModel.SortMode mSortMode;
        @IntRange(from = 0, to = 6)
        final int mGridCount;

        FilterState(@NonNull String searchQuery,
                boolean shouldShowAll,
                boolean shouldFilterPdf,
                boolean shouldFilterOffice,
                boolean shouldFilterImage,
                boolean shouldFilterText,
                @NonNull SortMode sortByName,
                @IntRange(from = 0, to = 6) int gridCount) {
            this.searchQuery = searchQuery;
            this.shouldShowAll = shouldShowAll;
            this.shouldFilterPdf = shouldFilterPdf;
            this.shouldFilterOffice = shouldFilterOffice;
            this.shouldFilterImage = shouldFilterImage;
            this.shouldFilterText = shouldFilterText;
            this.mSortMode = sortByName;
            mGridCount = gridCount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FilterState that = (FilterState) o;

            if (shouldShowAll != that.shouldShowAll) return false;
            if (shouldFilterPdf != that.shouldFilterPdf) return false;
            if (shouldFilterOffice != that.shouldFilterOffice) return false;
            if (shouldFilterImage != that.shouldFilterImage) return false;
            if (shouldFilterText != that.shouldFilterText) return false;
            if (mGridCount != that.mGridCount) return false;
            if (!searchQuery.equals(that.searchQuery)) return false;
            return mSortMode == that.mSortMode;
        }

        @Override
        public int hashCode() {
            int result = searchQuery.hashCode();
            result = 31 * result + (shouldShowAll ? 1 : 0);
            result = 31 * result + (shouldFilterPdf ? 1 : 0);
            result = 31 * result + (shouldFilterOffice ? 1 : 0);
            result = 31 * result + (shouldFilterImage ? 1 : 0);
            result = 31 * result + (shouldFilterText ? 1 : 0);
            result = 31 * result + mSortMode.hashCode();
            result = 31 * result + mGridCount;
            return result;
        }
    }
}
