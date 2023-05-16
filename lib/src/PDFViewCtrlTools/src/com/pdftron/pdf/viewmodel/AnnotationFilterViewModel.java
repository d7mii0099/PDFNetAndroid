package com.pdftron.pdf.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.dialog.annotlist.AnnotationListFilterInfo;
import com.pdftron.pdf.widget.base.ObservingLiveData;

import java.util.HashSet;

/**
 * A {@link ViewModel} that contains data for the annotation filter UI.
 */
public class AnnotationFilterViewModel extends AndroidViewModel {

    @NonNull
    private final ObservingLiveData<AnnotationListFilterInfo> mAnnotFilterLiveData = new ObservingLiveData<>();

    @NonNull
    private final MutableLiveData<HashSet<Annot>> mAnnotsToHide = new MutableLiveData<>(new HashSet<Annot>()); // set of ids of annotations to hide

    public AnnotationFilterViewModel(@NonNull Application application, final AnnotationListFilterInfo filterInfo) {
        super(application);
        mAnnotFilterLiveData.setValue(filterInfo);
    }

    /**
     * Deselects all selected filter settings.
     */
    public void deselectAllFilters() {
        AnnotationListFilterInfo filterInfo = mAnnotFilterLiveData.getValue();
        if (filterInfo != null) {
            filterInfo.deselectAll();
        }
    }

    /**
     * Removes all available filter settings from filter.
     */
    public void clearFilters() {
        AnnotationListFilterInfo filterInfo = mAnnotFilterLiveData.getValue();
        if (filterInfo != null) {
            filterInfo.clear();
        }
    }

    @NonNull
    public LiveData<AnnotationListFilterInfo> getAnnotationFilterLiveData() {
        return mAnnotFilterLiveData;
    }

    public void setAnnotationFilterInfo(@NonNull AnnotationListFilterInfo filterInfo) {
        mAnnotFilterLiveData.setValue(filterInfo);
    }

    public void onShowAllPressed() {
        AnnotationListFilterInfo filterInfo = mAnnotFilterLiveData.getValue();
        if (filterInfo != null) {
            filterInfo.setFilterState(AnnotationListFilterInfo.FilterState.OFF);
        }
    }

    public void onHideAllPressed() {
        AnnotationListFilterInfo filterInfo = mAnnotFilterLiveData.getValue();
        if (filterInfo != null) {
            filterInfo.setFilterState(AnnotationListFilterInfo.FilterState.HIDE_ALL);
        }
    }

    public void onApplyFilterPressed() {
        AnnotationListFilterInfo filterInfo = mAnnotFilterLiveData.getValue();
        if (filterInfo != null) {
            filterInfo.setFilterState(AnnotationListFilterInfo.FilterState.ON);
        }
    }

    public void onApplyFilterToAnnotationListPressed() {
        AnnotationListFilterInfo filterInfo = mAnnotFilterLiveData.getValue();
        if (filterInfo != null) {
            filterInfo.setFilterState(AnnotationListFilterInfo.FilterState.ON_LIST_ONLY);
        }
    }

    public void onTypeClicked(int type) {
        AnnotationListFilterInfo filterInfo = mAnnotFilterLiveData.getValue();
        if (filterInfo != null) {
            filterInfo.toggleType(type);
        }
    }

    public void onAuthorClicked(String author) {
        AnnotationListFilterInfo filterInfo = mAnnotFilterLiveData.getValue();
        if (filterInfo != null) {
            filterInfo.toggleAuthor(author);
        }
    }

    public void onStatusClicked(String status) {
        AnnotationListFilterInfo filterInfo = mAnnotFilterLiveData.getValue();
        if (filterInfo != null) {
            filterInfo.toggleStatus(status);
        }
    }

    public void onColorClicked(String color) {
        AnnotationListFilterInfo filterInfo = mAnnotFilterLiveData.getValue();
        if (filterInfo != null) {
            filterInfo.toggleColor(color);
        }
    }

    public void addType(int type) {
        AnnotationListFilterInfo filterInfo = mAnnotFilterLiveData.getValue();
        if (filterInfo != null) {
            filterInfo.addType(false, type); // default none are selected
        }
    }

    public void addAuthor(@NonNull String author) {
        AnnotationListFilterInfo filterInfo = mAnnotFilterLiveData.getValue();
        if (filterInfo != null) {
            filterInfo.addAuthor(false, author); // default none are selected
        }
    }

    public void addStatus(@NonNull String status) {
        AnnotationListFilterInfo filterInfo = mAnnotFilterLiveData.getValue();
        if (filterInfo != null) {
            filterInfo.addStatus(false, status); // default none are selected
        }
    }

    public void addColor(@NonNull String color) {
        AnnotationListFilterInfo filterInfo = mAnnotFilterLiveData.getValue();
        if (filterInfo != null) {
            filterInfo.addColor(false, color); // default none are selected
        }
    }

    public boolean shouldHideAnnot(Annot annot) {
        HashSet<Annot> value = mAnnotsToHide.getValue();
        if (value != null) {
            return value.contains(annot);
        }
        return false;
    }

    public void addAnnotToHide(@NonNull Annot annot) {
        HashSet<Annot> annotsToHide = mAnnotsToHide.getValue();
        if (annotsToHide != null) {
            HashSet<Annot> allAnnotsToHide = new HashSet<>(annotsToHide);
            allAnnotsToHide.add(annot);
            mAnnotsToHide.setValue(allAnnotsToHide);
        }
    }

    public void removeAnnotToHide(@NonNull Annot annot) {
        HashSet<Annot> annotsToHide = mAnnotsToHide.getValue();
        if (annotsToHide != null) {
            HashSet<Annot> allAnnotsToHide = new HashSet<>(annotsToHide);
            allAnnotsToHide.remove(annot);
            mAnnotsToHide.setValue(allAnnotsToHide);
        }
    }

    public void updateFilterOptions(HashSet<Integer> typeSet, HashSet<String> authorSet, HashSet<String> statusSet, HashSet<String> colorSet) {
        AnnotationListFilterInfo filterInfo = mAnnotFilterLiveData.getValue();
        if (filterInfo != null) {
            filterInfo.updateFilterOptions(typeSet, authorSet, statusSet, colorSet);
        }
    }

    public static class Factory extends ViewModelProvider.NewInstanceFactory {

        @NonNull
        private final Application mApplication;

        private final AnnotationListFilterInfo mFilterInfo;

        public Factory(@NonNull Application application, @NonNull AnnotationListFilterInfo filterInfo) {
            mApplication = application;
            mFilterInfo = filterInfo;
        }

        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            //noinspection unchecked
            return (T) new AnnotationFilterViewModel(mApplication, mFilterInfo);
        }
    }
}