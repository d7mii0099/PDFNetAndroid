package com.pdftron.pdf.widget.base;

import androidx.annotation.RestrictTo;
import androidx.lifecycle.MutableLiveData;

/**
 * A Live Data object that is updated when it's observable data class is updated.
 * @param <T> the data class that extends {@link BaseObservable} and updates its listeners when
 *           data has changed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ObservingLiveData<T extends BaseObservable> extends MutableLiveData<T> {

    private BaseObservable.OnPropertyChangedCallback callback = new BaseObservable.OnPropertyChangedCallback() {
        @Override
        public void onPropertyChanged() {
            setValue(getValue());
        }
    };

    public ObservingLiveData() {
        super();
    }

    public ObservingLiveData(T value) {
        super(value);
        value.addOnPropertyChangedCallback(callback);
    }

    @Override
    public void setValue(T value) {
        if (getValue() != null) {
            getValue().clearAllCallbacks();
        }
        super.setValue(value);
        if (value != null) {
            value.addOnPropertyChangedCallback(callback);
        }
    }
}
