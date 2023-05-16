package com.pdftron.pdf.dialog.digitalsignature.validation.properties;

import androidx.annotation.Nullable;

import com.pdftron.pdf.dialog.digitalsignature.validation.list.DigitalSignatureListDialog;

public class DigitalSignatureProperties {
    public final DigitalSignatureListDialog.DigitalSignatureBadge badge;
    public final String validitySummary;
    @Nullable
    public final String signerSummary;
    public final String permissionStatus;
    public final String permissionDetails;
    public final String trustStatus;
    public final String trustVerificationTime;
    public final String generalErrorReport;
    public final String digestStatus;
    public final String digestAlgorithm;

    public DigitalSignatureProperties(
            DigitalSignatureListDialog.DigitalSignatureBadge badge,
            String validitySummary,
            @Nullable String signerSummary,
            String permissionStatus,
            String permissionDetails,
            String trustStatus,
            String trustVerificationTime,
            String generalErrorReport,
            String digestStatus,
            String digestAlgorithm) {
        this.badge = badge;
        this.validitySummary = validitySummary;
        this.signerSummary = signerSummary;
        this.permissionStatus = permissionStatus;
        this.permissionDetails = permissionDetails;
        this.trustStatus = trustStatus;
        this.trustVerificationTime = trustVerificationTime;
        this.generalErrorReport = generalErrorReport;
        this.digestStatus = digestStatus;
        this.digestAlgorithm = digestAlgorithm;
    }
}
