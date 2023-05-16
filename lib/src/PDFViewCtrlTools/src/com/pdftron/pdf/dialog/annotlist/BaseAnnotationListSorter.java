package com.pdftron.pdf.dialog.annotlist;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class BaseAnnotationListSorter<T> extends ViewModel {
    protected final MutableLiveData<BaseAnnotationSortOrder> mSortOrder = new MutableLiveData<>();

    public BaseAnnotationListSorter(@NonNull BaseAnnotationSortOrder annotationListSortOrder) {
        mSortOrder.setValue(annotationListSortOrder);
    }

    public void observeSortOrderChanges(@NonNull LifecycleOwner owner,
            @NonNull Observer<BaseAnnotationSortOrder> observer) {
        mSortOrder.observe(owner, observer);
    }

    public void publishSortOrderChange(@NonNull BaseAnnotationSortOrder sortOrder) {
        mSortOrder.setValue(sortOrder);
    }

    public void sort(@NonNull List<T> annotationInfos) {
        Collections.sort(annotationInfos, getComparator());
    }

    @NonNull
    public abstract Comparator<T> getComparator();
}
