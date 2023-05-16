package com.pdftron.pdf.widget.preset.signature;

import android.content.Context;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.pdftron.pdf.dialog.signature.SignatureDialogFragment;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.StampManager;
import com.pdftron.pdf.widget.preset.signature.model.SignatureData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class SignaturePresetComponent {
    @NonNull
    public final Context mContext;
    @NonNull
    private final SignatureViewModel mSignatureViewModel;
    @NonNull
    private final SignatureSelectionView mSignatureSelectionView;
    @NonNull
    private final CompositeDisposable mImageConvertDisposables = new CompositeDisposable();
    private final LifecycleOwner mLifecycleOwner;

    public SignaturePresetComponent(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull SignatureViewModel signatureViewModel,
            @NonNull SignatureSelectionView signatureSelectionView,
            @NonNull final Context context) {

        mContext = context;
        mSignatureViewModel = signatureViewModel;
        mLifecycleOwner = lifecycleOwner;
        mSignatureSelectionView = signatureSelectionView;

        // Clean up our disposables on fragment destroyed
        mLifecycleOwner.getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    mImageConvertDisposables.clear();
                }
            }
        });
    }

    public void attachDialogToViewModel() {
        mSignatureViewModel.observeSignatures(mLifecycleOwner, new Observer<List<SignatureData>>() {
            @Override
            public void onChanged(List<SignatureData> signatures) {
                final int numSigs = signatures.size();
                int inputSize = Math.min(numSigs, 2);
                List<SignatureData> input = signatures.subList(0, inputSize);
                if (input.size() > 0) {
                    List<String> imageSignatures = new ArrayList<>();
                    mImageConvertDisposables.add(
                            Single.just(input)
                                    .flatMapObservable(new Function<List<SignatureData>, ObservableSource<SignatureData>>() {
                                        @Override
                                        public ObservableSource<SignatureData> apply(List<SignatureData> signatureData) throws Exception {
                                            return Observable.fromIterable(signatureData);
                                        }
                                    })
                                    .flatMapSingle(new Function<SignatureData, SingleSource<File>>() {
                                        @Override
                                        public SingleSource<File> apply(SignatureData signatureData) throws Exception {
                                            return StampManager.getSignaturePreview(mContext, signatureData.getFilePath());
                                        }
                                    })
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .doOnComplete(new Action() {
                                        @Override
                                        public void run() throws Exception {
                                            mSignatureSelectionView.setSignatures(imageSignatures);
                                            int createVisibility = SignatureDialogFragment.atMaxSignatureCount(numSigs) ? View.GONE : View.VISIBLE;
                                            mSignatureSelectionView.setViewVisibility(R.id.create_button, createVisibility);
                                            if (SignatureDialogFragment.MAX_SIGNATURES == 1) {
                                                int additionalSigVisibility = SignatureDialogFragment.atMaxSignatureCount(numSigs) ? View.GONE : View.VISIBLE;
                                                mSignatureSelectionView.setViewVisibility(R.id.additional_signature, additionalSigVisibility);
                                            }
                                        }
                                    })
                                    .subscribe(new Consumer<File>() {
                                        @Override
                                        public void accept(File file) throws Exception {
                                            imageSignatures.add(file.getAbsolutePath());
                                        }
                                    })
                    );
                } else {
                    mSignatureSelectionView.setSignatures(new ArrayList<>());
                    close();
                }
            }
        });
    }

    public void setButtonEventListener(@NonNull SignatureSelectionDialog.ButtonClickListener buttonEventListener) {
        mSignatureSelectionView.setButtonEventListener(buttonEventListener);
    }

    public void show() {
        attachDialogToViewModel();
        mSignatureSelectionView.show();
    }

    public void close() {
        mSignatureSelectionView.close();
    }
}
