//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import com.pdftron.demo.R;
import com.pdftron.demo.databinding.DialogImportWebpageUrlSelectorBinding;
import com.pdftron.demo.databinding.ViewCustomAlertDialogButtonsBinding;
import com.pdftron.pdf.controls.PasswordDialogFragment;
import com.pdftron.pdf.utils.Utils;

public class ImportWebpageUrlSelectorDialogFragment extends DialogFragment {

    @SuppressWarnings("unused")
    private static final String TAG = ImportWebpageUrlSelectorDialogFragment.class.getName();

    private boolean mCtaPressed = false;

    private OnLinkSelectedListener mOnLinkSelectedListener;
    private DialogInterface.OnDismissListener mOnDismissListener;
    protected DialogImportWebpageUrlSelectorBinding mBinding;

    public interface OnLinkSelectedListener {
        void linkSelected(String link);
    }

    public static ImportWebpageUrlSelectorDialogFragment newInstance() {
        return new ImportWebpageUrlSelectorDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        FragmentActivity activity = getActivity();
        assert (activity != null);

        final PasswordDialogFragment.Theme theme = PasswordDialogFragment.Theme.fromContext(activity);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        mBinding = DialogImportWebpageUrlSelectorBinding.inflate(activity.getLayoutInflater());
        builder.setView(mBinding.getRoot());

        final ViewCustomAlertDialogButtonsBinding buttonContainerBinding = mBinding.buttonContainer;

        buttonContainerBinding.positiveBtn.setText(R.string.dialog_webpage_pdf_convert);
        buttonContainerBinding.positiveBtn.setEnabled(false);
        buttonContainerBinding.positiveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCtaPressed = true;
                if (mOnLinkSelectedListener != null) {
                    mOnLinkSelectedListener.linkSelected(mBinding.webpageUrl.getText().toString());
                }
                dismiss();
            }
        });

        buttonContainerBinding.negativeBtn.setText(R.string.cancel);
        buttonContainerBinding.negativeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mBinding.title.setText(R.string.dialog_webpage_pdf_title);
        mBinding.message.setText(R.string.dialog_webpage_pdf_body);

        mBinding.webpageUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                String link = s.toString();
                if (!Utils.isNullOrEmpty(link)
                        && !link.contains(" ")
                        && Patterns.WEB_URL.matcher(link).matches()) {
                    buttonContainerBinding.positiveBtn.setEnabled(true);
                } else {
                    buttonContainerBinding.positiveBtn.setEnabled(false);
                }
                updateButtonState(buttonContainerBinding, theme);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        updateButtonState(buttonContainerBinding, theme);

        return builder.create();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        if (!mCtaPressed) {
            if (mOnDismissListener != null) {
                mOnDismissListener.onDismiss(dialog);
            }
        }
    }

    public void setOnLinkSelectedListener(OnLinkSelectedListener listener) {
        mOnLinkSelectedListener = listener;
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        mOnDismissListener = listener;
    }

    private void updateButtonState(ViewCustomAlertDialogButtonsBinding binding, PasswordDialogFragment.Theme theme) {
        int color = binding.positiveBtn.isEnabled() ? Utils.getAccentColor(binding.positiveBtn.getContext()) : theme.buttonDisabledColor;
        binding.positiveBtn.setBackgroundTintList(ColorStateList.valueOf(color));

        color = binding.negativeBtn.isEnabled() ? Utils.getAccentColor(binding.negativeBtn.getContext()) : theme.buttonDisabledColor;
        binding.negativeBtn.setStrokeColor(ColorStateList.valueOf(color));
        binding.negativeBtn.setTextColor(color);
    }
}
