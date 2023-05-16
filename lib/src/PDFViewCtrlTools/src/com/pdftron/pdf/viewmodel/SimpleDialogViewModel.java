package com.pdftron.pdf.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.pdftron.pdf.utils.Event;

public abstract class SimpleDialogViewModel<T> extends ViewModel {
    @NonNull
    private final MutableLiveData<Event<T>> mCompletable = new MutableLiveData<>();
    @NonNull
    private MutableLiveData<T> mResult = new MutableLiveData<>();

    @Nullable
    private Observer<Event<T>> mObserver;

    public SimpleDialogViewModel() {
        mCompletable.setValue(null); // initialize view model
        mResult.setValue(null);
    }

    public void set(T result) {
        mResult.setValue(result);
    }

    public void complete() {
        mCompletable.setValue(mResult.getValue() == null ?
                null : new Event<>(mResult.getValue()));
        mResult.setValue(null);
        if (mObserver != null) {
            mCompletable.removeObserver(mObserver);
        }
        mObserver = null;
    }

    public void observeOnComplete(@NonNull LifecycleOwner owner,
            @NonNull Observer<Event<T>> observer) {
        mCompletable.observe(owner, observer);
        mObserver = observer;
    }

    public void observeChanges(@NonNull LifecycleOwner owner,
            @NonNull Observer<T> observer) {
        mResult.observe(owner, observer);
    }
}
