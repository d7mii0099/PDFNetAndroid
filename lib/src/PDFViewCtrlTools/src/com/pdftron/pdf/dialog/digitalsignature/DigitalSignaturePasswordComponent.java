package com.pdftron.pdf.dialog.digitalsignature;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.ViewGroup;

import java.io.File;

import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;

class DigitalSignaturePasswordComponent {

    private final DigitalSignaturePasswordView mView;
    private final DigitalSignatureViewModel mViewmodel;

    DigitalSignaturePasswordComponent(@NonNull ViewGroup parent,
            @NonNull LifecycleOwner lifecycleOwner,
            @NonNull DigitalSignatureViewModel viewmodel) {

        PublishSubject<DigitalSignaturePasswordView.UserEvent> eventSubject = viewmodel.getEventSubject();
        PublishSubject<String> passwordChangeSubject = viewmodel.getPasswordChangeSubject();
        mView = new DigitalSignaturePasswordView(parent, eventSubject, passwordChangeSubject);
        mViewmodel = viewmodel;
        mViewmodel.subscribePasswordChangeSubject(new Consumer<String>() {
            @Override
            public void accept(String password) throws Exception {
                mViewmodel.mPassword.setValue(password);
            }
        });

        mViewmodel.signatureImageFile.observe(lifecycleOwner, new Observer<File>() {
            @Override
            public void onChanged(@Nullable File file) {
                if (file != null) {
                    mView.setSignaturePreview(file);
                }
            }
        });

        mViewmodel.mFileName.observe(lifecycleOwner, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                if (s != null) {
                    mViewmodel.mIsPasswordState.setValue(Boolean.TRUE);
                    mView.setFileName(s);
                } else {
                    mViewmodel.mIsPasswordState.setValue(Boolean.FALSE);
                }
            }
        });

        mViewmodel.mIsPasswordState.observe(lifecycleOwner, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean isPasswordState) {
                if (isPasswordState != null) {
                    if (isPasswordState) {
                        mView.enablePasswordMode();
                    } else {
                        mView.disablePasswordMode();
                    }
                }
            }
        });
    }
}
