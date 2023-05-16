package com.pdftron.pdf.widget.preset.signature;

import android.app.Application;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.StampManager;
import com.pdftron.pdf.widget.preset.signature.model.SignatureData;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class SignatureViewModel extends AndroidViewModel {

    private final MutableLiveData<List<SignatureData>> mSignatures = new MutableLiveData<>(new ArrayList<>());

    private final CompositeDisposable mDisposable = new CompositeDisposable();

    public SignatureViewModel(@NonNull Application application) {
        super(application);
        populateSignaturesAsync(application);
    }

    public void populateSignaturesAsync(@NonNull Context context) {
        populateSignaturesAsync(context, null);
    }

    public void populateSignaturesAsync(@NonNull Context context, @Nullable Consumer<List<SignatureData>> onCompleteListener) {
        mDisposable.add(
                getSignatures(context)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                new Consumer<List<SignatureData>>() {
                                    @Override
                                    public void accept(List<SignatureData> signatures) throws Exception {
                                        mSignatures.setValue(signatures);
                                        if (onCompleteListener != null) {
                                            onCompleteListener.accept(Collections.unmodifiableList(signatures));
                                        }
                                    }
                                },
                                new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable throwable) throws Exception {
                                        AnalyticsHandlerAdapter.getInstance().sendException(new Exception(throwable));
                                    }
                                })
        );
    }

    private Single<List<SignatureData>> getSignatures(@NonNull Context context) {
        Context applicationContext = context.getApplicationContext();
        return Single.create(new SingleOnSubscribe<List<SignatureData>>() {
            @Override
            public void subscribe(SingleEmitter<List<SignatureData>> emitter) throws Exception {
                try {
                    File[] savedSignatures = StampManager.getInstance().getSavedSignatures(applicationContext);
                    List<SignatureData> signatures = new ArrayList<>();
                    for (File file : savedSignatures) {
                        signatures.add(
                                new SignatureData(
                                        file.getAbsolutePath(),
                                        file.lastModified()
                                )
                        );
                    }
                    Collections.sort(signatures,
                            new Comparator<SignatureData>() {
                                @Override
                                public int compare(SignatureData signature1, SignatureData signature2) {
                                    // Sort by last modified first
                                    return -Long.compare(signature1.getLastUsedRawDate(), signature2.getLastUsedRawDate());
                                }
                            });
                    emitter.onSuccess(signatures);
                } catch (Exception e) {
                    emitter.tryOnError(new Exception("Could not retrieve signatures"));
                }
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDisposable.clear();
    }

    public void observeSignatures(@NonNull LifecycleOwner owner, @NonNull Observer<List<SignatureData>> listener) {
        mSignatures.observe(owner, listener);
    }

    public boolean hasSignatures() {
        if (mSignatures.getValue() != null) {
            return !mSignatures.getValue().isEmpty();
        } else {
            return false;
        }
    }

    @Nullable
    public List<SignatureData> getSignatures() {
        return mSignatures.getValue();
    }
}
