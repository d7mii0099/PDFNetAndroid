package com.pdftron.pdf.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.pdftron.pdf.model.PageState;

public class PageChangeViewModel extends AndroidViewModel {

    @Nullable
    private MutableLiveData<PageState> mPage = new MutableLiveData<>();

    public PageChangeViewModel(@NonNull Application application) {
        super(application);
    }

    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<PageState> observer) {
        if (mPage != null) {
            mPage.observe(owner, observer);
        }
    }

    public void onPageChange(PageState pageState) {
        if (mPage != null) {
            mPage.setValue(pageState);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mPage = null;
    }

    @Nullable
    public MutableLiveData<PageState> getPageState() {
        return mPage;
    }
}
