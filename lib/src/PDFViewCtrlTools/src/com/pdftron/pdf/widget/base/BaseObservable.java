package com.pdftron.pdf.widget.base;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.List;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BaseObservable {
    @NonNull
    private List<OnPropertyChangedCallback> mCallbacks = new ArrayList<>();

    public BaseObservable() {
    }

    public synchronized void addOnPropertyChangedCallback(@NonNull OnPropertyChangedCallback callback) {
        mCallbacks.add(callback);
    }

    public synchronized void removeOnPropertyChangedCallback(@NonNull OnPropertyChangedCallback callback) {
        mCallbacks.remove(callback);
    }

    public synchronized void clearAllCallbacks() {
        mCallbacks.clear();
    }

    public synchronized void notifyChange() {
        for (OnPropertyChangedCallback onPropertyChangedCallback: mCallbacks) {
            onPropertyChangedCallback.onPropertyChanged();
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface OnPropertyChangedCallback {
        void onPropertyChanged();
    }
}
