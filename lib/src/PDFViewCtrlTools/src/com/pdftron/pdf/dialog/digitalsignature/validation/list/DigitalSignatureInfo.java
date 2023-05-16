package com.pdftron.pdf.dialog.digitalsignature.validation.list;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.pdf.dialog.digitalsignature.validation.properties.DigitalSignatureProperties;

public class DigitalSignatureInfo implements DigitalSignatureInfoBase {
    @NonNull
    final DigitalSignatureListDialog.DigitalSignatureBadge badge;
    boolean expanded = true;
    boolean additionalDetailsExpanded = false;
    final boolean additionDetailsAvailable;
    @NonNull
    final String header;
    @NonNull
    final String verificationStatus;
    @NonNull
    final String permissionStatus;
    @Nullable
    final String disallowedChanges;
    @NonNull
    final String trustVerificationResult;
    @Nullable
    final String trustVerificationResultDateTime;
    @Nullable
    final String trustVerificationDateTimeMsg;
    @Nullable
    final String contactInfo;
    @Nullable
    final String location;
    @Nullable
    final String reason;
    @Nullable
    final String signingTime;
    @NonNull
    public final DigitalSignatureProperties digitalSignatureProperties;

    public DigitalSignatureInfo(
            @NonNull DigitalSignatureListDialog.DigitalSignatureBadge badge,
            boolean additionDetailsAvailable, @NonNull String header,
            @NonNull String verificationStatus,
            @NonNull String permissionStatus,
            @Nullable String disallowedChanges,
            @NonNull String trustVerificationResult,
            @Nullable String trustVerificationResultDateTime,
            @Nullable String trustVerificationDateTimeMsg,
            @Nullable String contactInfo,
            @Nullable String location,
            @Nullable String reason,
            @Nullable String signingTime,
            @NonNull DigitalSignatureProperties digitalSignatureProperties
    ) {
        this.badge = badge;
        this.additionDetailsAvailable = additionDetailsAvailable;
        this.header = header;
        this.verificationStatus = verificationStatus;
        this.permissionStatus = permissionStatus;
        this.disallowedChanges = disallowedChanges;
        this.trustVerificationResult = trustVerificationResult;
        this.trustVerificationResultDateTime = trustVerificationResultDateTime;
        this.trustVerificationDateTimeMsg = trustVerificationDateTimeMsg;
        this.contactInfo = contactInfo;
        this.location = location;
        this.reason = reason;
        this.signingTime = signingTime;
        this.digitalSignatureProperties = digitalSignatureProperties;
    }
}
