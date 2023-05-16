package com.pdftron.pdf.dialog.digitalsignature;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.pdftron.pdf.Page;
import com.pdftron.pdf.dialog.signature.SignatureDialogFragment;
import com.pdftron.pdf.dialog.signature.SignatureDialogFragmentBuilder;
import com.pdftron.pdf.interfaces.OnCreateSignatureListener;
import com.pdftron.pdf.tools.DigitalSignature;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.RequestCode;
import com.pdftron.pdf.utils.StampManager;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;

import io.reactivex.functions.Consumer;

import static com.pdftron.pdf.utils.RequestCode.DIGITAL_SIGNATURE_KEYSTORE;

/**
 * A {@link SignatureDialogFragment} that allows users to pick a certificate keystore and associated
 * password to digitally sign the signature.
 */
public class DigitalSignatureDialogFragment extends SignatureDialogFragment {

    public static boolean HANDLE_INTENT_IN_ACTIVITY = false; // whether the intent from onActivityResult should be handled in the activity

    private static final String DIGITAL_SIG_USER_INPUT_FRAGMENT_ID = "digital_signature_user_input_fragment";

    private boolean mHasDefaultKeystore = SignatureDialogFragmentBuilder.HAS_DEFAULT_KEYSTORE;

    private Uri mImageSignature = null;
    private OnKeystoreUpdatedListener mOnKeystoreUpdatedListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mHasDefaultKeystore = args.getBoolean(SignatureDialogFragmentBuilder.BUNDLE_HAS_DEFAULT_KEYSTORE,
                    SignatureDialogFragmentBuilder.HAS_DEFAULT_KEYSTORE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        DigitalSignatureViewModel viewmodel = getViewModel(getActivity());
        viewmodel.mKeyStoreFile.observe(this,
                new Observer<Uri>() {
                    @Override
                    public void onChanged(@Nullable Uri uri) {
                        if (mOnKeystoreUpdatedListener != null) {
                            mOnKeystoreUpdatedListener.onKeystoreFileUpdated(uri);
                        }
                    }
                });
        viewmodel.mPassword.observe(this,
                new Observer<String>() {
                    @Override
                    public void onChanged(@Nullable String s) {
                        if (mOnKeystoreUpdatedListener != null) {
                            mOnKeystoreUpdatedListener.onKeystorePasswordUpdated(s);
                        }
                    }
                });
        viewmodel.mOnActivityResultIntent.observe(this,
                new Observer<DigitalSignatureViewModel.ActivityResultIntent>() {
                    @Override
                    public void onChanged(@Nullable DigitalSignatureViewModel.ActivityResultIntent activityResultIntent) {
                        if (activityResultIntent != null) {
                            if (isDigitalSignatureIntent(activityResultIntent.requestCode)) {
                                onActivityResult(activityResultIntent.requestCode, activityResultIntent.resultCode, activityResultIntent.data);
                            }
                        }
                    }
                });
        return root;
    }

    @Override
    public void onActivityResult(final int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (requestCode == RequestCode.DIGITAL_SIGNATURE_KEYSTORE) {
            if (data == null) {
                return;
            }
            Uri uri = data.getData();
            if (uri != null) {

                ContentResolver contentResolver = activity.getContentResolver();
                if (contentResolver != null) {

                    if (Utils.uriHasReadPermission(getContext(), uri)) {
                        DigitalSignatureViewModel vm = getViewModel(activity);

                        vm.setKeystoreFileUri(uri);

                        String name = Utils.getUriDisplayName(activity, uri);
                        vm.setFileName(name);
                    }
                }
            }
        } else if (requestCode == RequestCode.DIGITAL_SIGNATURE_IMAGE) { // If signature from image file picker
            String signatureFilePath = ViewerUtils.getImageSignaturePath(activity, data, mImageSignature);
            if (signatureFilePath != null) {
                showInputScreen(signatureFilePath, true);
            }
        }
    }

    private void showInputScreen(@NonNull final String signatureFilePath, final boolean saveSignature) {
        if (mHasDefaultKeystore) { // If we have a default keystore from tool manager, then just continue
            if (mOnCreateSignatureListener != null && signatureFilePath != null) {
                for (OnCreateSignatureListener listener : mOnCreateSignatureListener) {
                    listener.onSignatureCreated(signatureFilePath, saveSignature);
                }
            }
            dismiss();
        } else {

            FragmentActivity activity = getActivity();
            if (activity == null) {
                throw new RuntimeException("This fragment must have a contexactivity");
            }

            final Fragment fragment = new DigitalSignatureUserInputFragment();
            final DigitalSignatureViewModel viewModel = getViewModel(activity);

            Page page = StampManager.getInstance().getSignature(signatureFilePath);
            String imageFile = DigitalSignature.createSignatureImageFile(activity, page);
            viewModel.setImageFilePath(imageFile);
            viewModel.subscribeEventSubject(new Consumer<DigitalSignaturePasswordView.UserEvent>() {
                @Override
                public void accept(DigitalSignaturePasswordView.UserEvent userEvent) throws Exception {
                    switch (userEvent) {
                        case ON_CANCEL: {
                            dismiss();
                            break;
                        }
                        case ON_FINISH_PASSWORD: {

                            if (mOnCreateSignatureListener != null && signatureFilePath != null) {
                                for (OnCreateSignatureListener listener : mOnCreateSignatureListener) {
                                    listener.onSignatureCreated(signatureFilePath, saveSignature);
                                }
                            }
                            dismiss();
                            break;
                        }
                        case ON_ADD_CERTIFICATE: {
                            startKeystoreFilePicker();
                            break;
                        }
                    }
                }
            });

            getChildFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, fragment, DIGITAL_SIG_USER_INPUT_FRAGMENT_ID)
                    .addToBackStack(DIGITAL_SIG_USER_INPUT_FRAGMENT_ID)
                    .commit();
        }
    }

    @Override
    public void onSignatureFromImage(@Nullable PointF targetPoint, int targetPage,
            @Nullable Long widget) {
        if (HANDLE_INTENT_IN_ACTIVITY) {
            mImageSignature = ViewerUtils.openImageIntent(getActivity(), RequestCode.DIGITAL_SIGNATURE_IMAGE);
        } else {
            mImageSignature = ViewerUtils.openImageIntent(this, RequestCode.DIGITAL_SIGNATURE_IMAGE);
        }
    }

    @Override
    public void onSignatureSelected(@NonNull String filepath) {
        showInputScreen(filepath, true);
    }

    @Override
    public void onSignatureCreated(@Nullable String filepath, boolean saveSignature) {
        if (filepath != null) {
            showInputScreen(filepath, saveSignature);
        }
    }

    protected void startKeystoreFilePicker() {
        CommonToast.showText(getContext(), R.string.tools_digitalsignature_add_certificate, Toast.LENGTH_LONG);

        Intent intent;
        if (Utils.isKitKat()) {
            String[] fileMimeTypes = {
                    "application/x-pkcs12"
            };
            intent = Utils.createSystemPickerIntent(fileMimeTypes);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("application/x-pkcs12");
            intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
        }

        if (HANDLE_INTENT_IN_ACTIVITY) {
            getActivity().startActivityForResult(intent, DIGITAL_SIGNATURE_KEYSTORE);
        } else {
            startActivityForResult(intent, DIGITAL_SIGNATURE_KEYSTORE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity() != null) {
            getViewModel(getActivity()).cleanUp();
        }
    }

    public void setOnKeystoreUpdatedListener(OnKeystoreUpdatedListener
            onKeystoreUpdatedListener) {
        mOnKeystoreUpdatedListener = onKeystoreUpdatedListener;
    }

    public static DigitalSignatureViewModel getViewModel(@NonNull FragmentActivity activity) {
        return ViewModelProviders.of(activity).get(DigitalSignatureViewModel.class);
    }

    public static boolean isDigitalSignatureIntent(int requestCode) {
        return requestCode == RequestCode.DIGITAL_SIGNATURE_KEYSTORE
                || requestCode == RequestCode.DIGITAL_SIGNATURE_IMAGE;
    }
}
