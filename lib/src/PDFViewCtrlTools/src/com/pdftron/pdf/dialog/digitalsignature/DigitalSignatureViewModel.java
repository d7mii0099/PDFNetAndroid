package com.pdftron.pdf.dialog.digitalsignature;

import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.squareup.picasso.Picasso;

import java.io.File;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;

/**
 * View model that holds the user specified information in the
 * {@link DigitalSignatureUserInputFragment}.
 */
public class DigitalSignatureViewModel extends AndroidViewModel {
    final MutableLiveData<File> signatureImageFile = new MutableLiveData<>();
    final MutableLiveData<String> mPassword = new MutableLiveData<>();
    final MutableLiveData<String> mFileName = new MutableLiveData<>();
    final MutableLiveData<Boolean> mIsPasswordState = new MutableLiveData<>();

    final MutableLiveData<Uri> mKeyStoreFile = new MutableLiveData<>();
    final MutableLiveData<ActivityResultIntent> mOnActivityResultIntent = new MutableLiveData<>();

    private final CompositeDisposable mDisposables = new CompositeDisposable();

    @NonNull
    private PublishSubject<DigitalSignaturePasswordView.UserEvent> mEventSubject = PublishSubject.create();
    @NonNull
    private PublishSubject<String> mPasswordChangeSubject = PublishSubject.create();

    public DigitalSignatureViewModel(@NonNull Application application) {
        super(application);
    }

    /**
     * Set the keystore file Uri that will be used in the current digital signature session.
     *
     * @param keystoreFileUri Uri that points to a keystore file
     */
    public void setKeystoreFileUri(@NonNull Uri keystoreFileUri) {
        mKeyStoreFile.setValue(keystoreFileUri);
    }

    void setFileName(@Nullable String fileName) {
        mFileName.setValue(fileName);
    }

    void setImageFilePath(@Nullable String fileName) {
        signatureImageFile.setValue(new File(fileName));
    }

    public void setActivityResultIntent(int requestCode, int resultCode, @Nullable Intent data) {
        mOnActivityResultIntent.setValue(new ActivityResultIntent(requestCode, resultCode, data));
    }

    @NonNull
    PublishSubject<DigitalSignaturePasswordView.UserEvent> getEventSubject() {
        return mEventSubject;
    }

    @NonNull
    PublishSubject<String> getPasswordChangeSubject() {
        return mPasswordChangeSubject;
    }

    void subscribeEventSubject(Consumer<DigitalSignaturePasswordView.UserEvent> onNext) {
        mDisposables.add(
                mEventSubject.subscribe(onNext)
        );
    }

    void subscribePasswordChangeSubject(Consumer<String> onNext) {
        mDisposables.add(
                mPasswordChangeSubject.subscribe(onNext)
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cleanUp();
    }

    void cleanUp() {
        File sigImageFile = signatureImageFile.getValue();
        if (sigImageFile != null) {
            Picasso.get().invalidate(sigImageFile);
        }
        mPassword.setValue(null);
        mFileName.setValue(null);
        signatureImageFile.setValue(null);
        mKeyStoreFile.setValue(null);
        mOnActivityResultIntent.setValue(null);
        mDisposables.clear();
    }

    class ActivityResultIntent {

        final int requestCode;
        final int resultCode;
        final Intent data;

        public ActivityResultIntent(int requestCode, int resultCode, Intent data) {
            this.requestCode = requestCode;
            this.resultCode = resultCode;
            this.data = data;
        }
    }
}
