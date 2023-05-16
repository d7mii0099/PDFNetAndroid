package com.pdftron.pdf.dialog.digitalsignature;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.android.material.textfield.TextInputEditText;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.pdftron.pdf.tools.R;
import com.squareup.picasso.Picasso;

import java.io.File;

import io.reactivex.subjects.PublishSubject;

class DigitalSignaturePasswordView {

    private final ConstraintLayout mCertificateForm;
    private final ConstraintLayout mPasswordForm;
    private final ImageView mSigPreview;
    private final TextView mFileName;

    DigitalSignaturePasswordView(final @NonNull ViewGroup parent,
            final @NonNull PublishSubject<UserEvent> eventSubject,
            final @NonNull PublishSubject<String> passwordChangeSubject) {

        mCertificateForm = parent.findViewById(R.id.certificate_form);
        mPasswordForm = parent.findViewById(R.id.password_form);
        mSigPreview = parent.findViewById(R.id.signature_preview);
        mFileName = parent.findViewById(R.id.file_name);
        TextInputEditText editText = parent.findViewById(R.id.fragment_password_dialog_password);
        Button okayButton = parent.findViewById(R.id.okay_button);
        Button addCertificateButton = parent.findViewById(R.id.add_cert_button);
        Toolbar navToolbar = parent.findViewById(R.id.digital_sig_input_toolbar);

        navToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eventSubject.onNext(UserEvent.ON_CANCEL);
            }
        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                passwordChangeSubject.onNext(s.toString());
            }
        });

        okayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eventSubject.onNext(UserEvent.ON_FINISH_PASSWORD);
            }
        });

        addCertificateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eventSubject.onNext(UserEvent.ON_ADD_CERTIFICATE);
            }
        });
    }

    void enablePasswordMode() {
        mCertificateForm.setVisibility(View.GONE);
        mPasswordForm.setVisibility(View.VISIBLE);
    }

    void disablePasswordMode() {
        mCertificateForm.setVisibility(View.VISIBLE);
        mPasswordForm.setVisibility(View.GONE);
    }

    void setSignaturePreview(@NonNull File sigImage) {
        Picasso.get().load(sigImage).into(mSigPreview);
    }

    void setFileName(@NonNull String fileName) {
        mFileName.setText(fileName);
    }

    enum UserEvent {
        ON_FINISH_PASSWORD, ON_CANCEL, ON_ADD_CERTIFICATE
    }
}
