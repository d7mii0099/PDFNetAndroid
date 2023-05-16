package com.pdftron.pdf.dialog.digitalsignature;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.pdftron.pdf.tools.R;

/**
 * Fragment that contains the user input/interaction for obtaining digital signing information
 * (such as the certificate and certificate password).
 */
public class DigitalSignatureUserInputFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tools_dialog_digital_signature_user_input, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewGroup viewContainer = view.findViewById(R.id.container);
        Fragment parentFragment = getParentFragment();
        if (parentFragment == null) {
            throw new RuntimeException("This fragment should run as a child fragment of a containing parent fragment.");
        }
        DigitalSignatureViewModel viewModel = ViewModelProviders.of(getActivity()).get(DigitalSignatureViewModel.class);

        // After creating component and passing in view model, the view model will be live
        new DigitalSignaturePasswordComponent(viewContainer, this, viewModel);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
