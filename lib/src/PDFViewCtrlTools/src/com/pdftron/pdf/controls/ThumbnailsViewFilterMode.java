package com.pdftron.pdf.controls;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class ThumbnailsViewFilterMode extends ViewModel {
    private final MutableLiveData<Integer> mFilterMode = new MutableLiveData<>();

    public ThumbnailsViewFilterMode(@NonNull Integer filterMode) {
        mFilterMode.setValue(filterMode);
    }

    public void observeFilterTypeChanges(@NonNull LifecycleOwner owner,
            @NonNull Observer<Integer> observer) {
        mFilterMode.observe(owner, observer);
    }

    public void publishFilterTypeChange(@NonNull Integer sortOrder) {
        mFilterMode.setValue(sortOrder);
    }

    public Integer getFilterMode() {
        return  mFilterMode.getValue();
    }

    public static class Factory implements ViewModelProvider.Factory {

        private Integer mFilterMode;

        public Factory(Integer filterMode) {
            mFilterMode = filterMode;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(ThumbnailsViewFilterMode.class)) {
                return (T) new ThumbnailsViewFilterMode(mFilterMode);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}
