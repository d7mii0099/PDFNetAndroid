package com.pdftron.pdf.dialog.digitalsignature.validation.properties;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;

import com.pdftron.pdf.tools.R;

public class DigitalSignaturePropertiesDialog extends DialogFragment {

    private Toolbar mToolbar;
    @Nullable
    private ImageView mBadge;
    @Nullable
    private TextView mValiditySummaryBox;
    @Nullable
    private TextView mSignerSummaryBox;

    @Nullable
    private TextView mPermissionStatus;
    @Nullable
    private TextView mPermissionDetails;

    @Nullable
    private TextView mTrustStatus;
    @Nullable
    private TextView mTimeOfTrustVerification;

    @Nullable
    private TextView mErrorReport;

    @Nullable
    private TextView mDigestStatus;
    @Nullable
    private TextView mDigestAlgorithm;

    @Nullable
    private DigitalSignatureProperties mDigitalSignatureProperties;

    public static DigitalSignaturePropertiesDialog newInstance() {
        return new DigitalSignaturePropertiesDialog();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_digital_signature_properties_dialog, container, false);

        mBadge = root.findViewById(R.id.badge);
        mValiditySummaryBox = root.findViewById(R.id.validity_summary_box);
        mSignerSummaryBox = root.findViewById(R.id.signer_summary_box);

        mPermissionStatus = root.findViewById(R.id.permission_status);
        mPermissionDetails = root.findViewById(R.id.permission_details);

        mTrustStatus = root.findViewById(R.id.trust_status);
        mTimeOfTrustVerification = root.findViewById(R.id.trust_verification_time);

        mErrorReport = root.findViewById(R.id.error_report);

        mDigestStatus = root.findViewById(R.id.digest_status);
        mDigestAlgorithm = root.findViewById(R.id.digest_algorithm);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mToolbar = view.findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.dialog_digital_signature_info_properties);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        bindTextViews();
    }

    public void bindTextViews() {
        Context context = getContext();
        if (mDigitalSignatureProperties != null && context != null) {
            if (mBadge != null) {
                switch (mDigitalSignatureProperties.badge) {
                    case ERROR:
                        mBadge.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_digital_signature_error));
                        break;
                    case VALID:
                        mBadge.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_digital_signature_valid));
                        break;
                    case WARNING:
                        mBadge.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_digital_signature_warning));
                }
            }
            if (mValiditySummaryBox != null) {
                mValiditySummaryBox.setText(mDigitalSignatureProperties.validitySummary);
            }
            if (mSignerSummaryBox != null) {
                if (mDigitalSignatureProperties.signerSummary != null) {
                    mSignerSummaryBox.setVisibility(View.VISIBLE);
                    mSignerSummaryBox.setText(mDigitalSignatureProperties.signerSummary);
                } else {
                    mSignerSummaryBox.setVisibility(View.GONE);
                }
            }
            if (mPermissionStatus != null) {
                mPermissionStatus.setText(mDigitalSignatureProperties.permissionStatus);
            }
            if (mPermissionDetails != null) {
                mPermissionDetails.setText(mDigitalSignatureProperties.permissionDetails);
            }
            if (mTrustStatus != null) {
                mTrustStatus.setText(mDigitalSignatureProperties.trustStatus);
            }
            if (mTimeOfTrustVerification != null) {
                mTimeOfTrustVerification.setText(mDigitalSignatureProperties.trustVerificationTime);
            }
            if (mErrorReport != null) {
                mErrorReport.setText(mDigitalSignatureProperties.generalErrorReport);
            }
            if (mDigestStatus != null) {
                mDigestStatus.setText(mDigitalSignatureProperties.digestStatus);
            }
            if (mDigestAlgorithm != null) {
                mDigestAlgorithm.setText(mDigitalSignatureProperties.digestAlgorithm);
            }
        }
    }

    public void setDigitalSignatureProperties(@NonNull DigitalSignatureProperties digitalSignatureProperties) {
        this.mDigitalSignatureProperties = digitalSignatureProperties;
        bindTextViews();
    }
}
