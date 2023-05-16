package com.pdftron.demo.browser.ui;

import android.content.Context;
import android.view.Menu;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.pdftron.pdf.utils.Constants;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

class FilterSettingsComponent {

    @Nullable
    private FilterSettingsView mView;
    @NonNull
    private final FilterSettingsViewModel mViewModel;

    FilterSettingsComponent(@NonNull Context context,
            @NonNull LifecycleOwner lifecycleOwner,
            @Nullable Menu parentMenu,
            @NonNull FilterSettingsViewModel viewModel) {
        setMenu(context, parentMenu);
        mViewModel = viewModel;
        mViewModel.observeFilterUiUpdates(lifecycleOwner, new Observer<FilterSettingsViewModel.FilterState>() {
            @Override
            public void onChanged(@Nullable FilterSettingsViewModel.FilterState filterState) {
                if (mView != null) {
                    mView.updateView(filterState);
                }
            }
        });
    }

    public void setMenu(@NonNull Context context, @Nullable Menu parentMenu) {
        if (parentMenu != null) {
            mView = new FilterSettingsView(context, parentMenu);
            if (mViewModel != null) {
                mView.updateView(mViewModel.getCurrentUIState());
            }
        } else {
            mView = null;
        }
    }

    Disposable observeListUpdates(@NonNull Consumer<FilterSettingsViewModel.FilterState> consumer) {
        return mViewModel.observeListUpdates(consumer);
    }

    void updateSearchString(@Nullable String searchString) {
        mViewModel.setSearchQuery(searchString);
    }

    void toggleEvent(@NonNull FilterEvent filterEvent) {
        switch (filterEvent) {
            case ALL_FILTER_CLICKED: {
                mViewModel.toggleFilterAll();
                break;
            }
            case PDF_FILTER_CLICKED: {
                mViewModel.toggleFileFilter(Constants.FILE_TYPE_PDF);
                break;
            }
            case OFFICE_FILTER_CLICKED: {
                mViewModel.toggleFileFilter(Constants.FILE_TYPE_DOC);
                break;
            }
            case IMAGE_FILTER_CLICKED: {
                mViewModel.toggleFileFilter(Constants.FILE_TYPE_IMAGE);
                break;
            }
            case TEXT_FILTER_CLICKED: {
                mViewModel.toggleFileFilter(Constants.FILE_TYPE_TEXT);
                break;
            }
            case SORT_BY_DATE_CLICKED: {
                mViewModel.toggleSortByDate();
                break;
            }
            case SORT_BY_NAME_CLICKED: {
                mViewModel.toggleSortByName();
                break;
            }
        }
    }

    void updateGridCount(@IntRange(from=0, to=6) int gridCount) {
        mViewModel.setGridCount(gridCount);
    }

    enum FilterEvent {
        // Event for checking filters
        ALL_FILTER_CLICKED, PDF_FILTER_CLICKED, OFFICE_FILTER_CLICKED, IMAGE_FILTER_CLICKED,
        TEXT_FILTER_CLICKED,
        // Event for checking sort modes
        SORT_BY_NAME_CLICKED, SORT_BY_DATE_CLICKED
    }
}
