package com.pdftron.pdf.dialog.digitalsignature.validation;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.pdftron.common.PDFNetException;
import com.pdftron.crypto.ObjectIdentifier;
import com.pdftron.crypto.X501AttributeTypeAndValue;
import com.pdftron.crypto.X501DistinguishedName;
import com.pdftron.crypto.X509Certificate;
import com.pdftron.pdf.Date;
import com.pdftron.pdf.DigitalSignatureField;
import com.pdftron.pdf.DigitalSignatureFieldIterator;
import com.pdftron.pdf.DisallowedChange;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.TrustVerificationResult;
import com.pdftron.pdf.VerificationOptions;
import com.pdftron.pdf.VerificationResult;
import com.pdftron.pdf.dialog.digitalsignature.validation.list.DigitalSignatureInfo;
import com.pdftron.pdf.dialog.digitalsignature.validation.list.DigitalSignatureListDialog;
import com.pdftron.pdf.dialog.digitalsignature.validation.properties.DigitalSignatureProperties;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class DigitalSignatureUtils {

    private static final String CERTIFICATE_FOLDER = "dig_sig_cert";

    /**
     * Saves the certificate to the internal cache.
     */
    public static void addCertificate(@NonNull Context context, @NonNull File cert) throws IOException {
        File certDir = getCertFolder(context);
        if (!certDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            certDir.mkdirs();
        }

        File copiedCert = new File(certDir, cert.getName());
        Utils.copy(cert, copiedCert);
    }

    @Nullable
    public static File[] getCertificates(@NonNull Context context) {
        File certDir = getCertFolder(context);
        if (!certDir.exists()) {
            return null;
        }
        return certDir.listFiles();
    }

    public static void clearCertificates(@NonNull Context context) {
        File certDir = getCertFolder(context);
        File[] files = certDir.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }

    private static File getCertFolder(@NonNull Context context) {
        File certDir = context.getFilesDir();
        return new File(certDir, CERTIFICATE_FOLDER);
    }

    @SuppressWarnings("UnnecessaryCallToStringValueOf")
    @Nullable
    public static String getDisallowedChanges(@NonNull Context context, VerificationResult result) throws PDFNetException {
        String disallowedChangesMsg = null;
        if (hasDisallowedChanges(result)) {
            DisallowedChange[] disallowedChanges = result.getDisallowedChanges();
            for (DisallowedChange disallowedChange : disallowedChanges) {
                String disallowedChangeFormat = context.getString(R.string.dialog_digital_signature_disallowed_changes);
                if (disallowedChangesMsg == null) {
                    disallowedChangesMsg = String.format(disallowedChangeFormat, disallowedChange.getTypeAsString(), Integer.toString(disallowedChange.getObjNum()));
                } else {
                    disallowedChangesMsg = disallowedChangesMsg + "\n\n" + String.format(disallowedChangeFormat, disallowedChange.getTypeAsString(), Integer.toString(disallowedChange.getObjNum()));
                }
            }
        }
        return disallowedChangesMsg;
    }

    public static boolean hasDisallowedChanges(@NonNull VerificationResult result) throws PDFNetException {
        VerificationResult.ModificationPermissionsStatus permissionStatus = result.getPermissionsStatus();
        return permissionStatus == VerificationResult.ModificationPermissionsStatus.e_invalidated_by_disallowed_changes;
    }

    public static String getPermissionStatus(@NonNull Context context, @NonNull VerificationResult result, @NonNull DigitalSignatureField digSigField) throws PDFNetException {
        VerificationResult.ModificationPermissionsStatus permissionStatus = result.getPermissionsStatus();
        String permission;
        switch (permissionStatus) {
            case e_no_permissions_status:
                permission = context.getString(R.string.dialog_digital_signature_no_permission_status);
                break;
            case e_permissions_verification_disabled:
                permission = context.getString(R.string.dialog_digital_signature_permission_status_verification_disabled);
                break;
            case e_has_allowed_changes:
                permission = context.getString(R.string.dialog_digital_signature_permission_status_has_allowed_changes);
                break;
            case e_invalidated_by_disallowed_changes:
                permission = context.getString(R.string.dialog_digital_signature_permission_status_invalidated_by_disallowed_changes);
                break;
            case e_unmodified:
                if (digSigField.isCertification()) {
                    permission = context.getString(R.string.dialog_digital_signature_permission_status_unmodified_certification);
                } else {
                    permission = context.getString(R.string.dialog_digital_signature_permission_status_unmodified_signature);
                }
                break;
            case e_unsupported_permissions_features:
                permission = context.getString(R.string.dialog_digital_signature_permission_status_unsupported);
                break;
            default:
                throw new RuntimeException("Could not obtain permission status.");
        }
        return permission;
    }

    @NonNull
    public static String getPermissionDetails(@NonNull Context context, @NonNull DigitalSignatureField digSigField) throws PDFNetException {
        DigitalSignatureField.DocumentPermissions documentPermissions = digSigField.getDocumentPermissions();
        boolean isCert = digSigField.isCertification();
        String permissionDetails;
        switch (documentPermissions) {
            case e_no_changes_allowed:
                if (isCert) {
                    permissionDetails = context.getString(R.string.dialog_digital_signature_document_permissions_no_changes_allowed_certifier);
                } else {
                    permissionDetails = context.getString(R.string.dialog_digital_signature_document_permissions_no_changes_allowed_signer);
                }
                break;
            case e_formfilling_signing_allowed:
                if (isCert) {
                    permissionDetails = context.getString(R.string.dialog_digital_signature_document_permissions_formfilling_signing_allowed_certifier);
                } else {
                    permissionDetails = context.getString(R.string.dialog_digital_signature_document_permissions_formfilling_signing_allowed_signer);
                }
                break;
            case e_annotating_formfilling_signing_allowed:
                if (isCert) {
                    permissionDetails = context.getString(R.string.dialog_digital_signature_document_permissions_annotating_formfilling_signing_allowed_certifier);
                } else {
                    permissionDetails = context.getString(R.string.dialog_digital_signature_document_permissions_annotating_formfilling_signing_allowed_signer);
                }
                break;
            case e_unrestricted:
                if (isCert) {
                    permissionDetails = context.getString(R.string.dialog_digital_signature_document_permissions_unrestricted_certifier);
                } else {
                    permissionDetails = context.getString(R.string.dialog_digital_signature_document_permissions_unrestricted_signer);
                }
                break;
            default:
                throw new RuntimeException("Missing definition for digital signature permissions.");
        }
        return permissionDetails;
    }

    @NonNull
    public static VerificationResult getVerificationResult(@NonNull Context context, @NonNull DigitalSignatureField digSigField) throws PDFNetException {
        VerificationOptions opts = new VerificationOptions(VerificationOptions.SecurityLevel.e_compatibility_and_archiving);
        File[] certificates = getCertificates(context);
        if (certificates != null && certificates.length > 0) {
            for (File certificate : certificates) {
                opts.addTrustedCertificate(certificate.getAbsolutePath(), VerificationOptions.CertificateTrustFlag.e_default_trust.value | VerificationOptions.CertificateTrustFlag.e_certification_trust.value);
            }
        }
        return digSigField.verify(opts);
    }

    @NonNull
    public static String getFieldVerificationStatus(@NonNull Context context, @NonNull VerificationResult result, DigitalSignatureField digSigField) throws PDFNetException {
        boolean isCert = digSigField.isCertification();
        String isValid;
        if (result.getVerificationStatus()) {
            if (isCert) {
                isValid = context.getString(R.string.dialog_digital_signature_is_valid_cert);
            } else {
                isValid = context.getString(R.string.dialog_digital_signature_is_valid_sig);
            }
        } else {
            if (isCert) {
                isValid = context.getString(R.string.dialog_digital_signature_failed_cert);
            } else {
                isValid = context.getString(R.string.dialog_digital_signature_failed_sig);
            }
        }
        return isValid;
    }

    @Nullable
    public static String getSigningTime(@NonNull DigitalSignatureField digSigField) throws PDFNetException {
        DigitalSignatureField.SubFilterType subfilter = digSigField.getSubFilter();
        if (subfilter != DigitalSignatureField.SubFilterType.e_ETSI_RFC3161) {
            Date signingTime = digSigField.getSigningTime();
            if (signingTime.isValid()) {
                Calendar calendar = Calendar.getInstance();
                int month = signingTime.getMonth() - 1; // android calendar month is 0-indexed
                calendar.set(signingTime.getYear(), month, signingTime.getDay(), signingTime.getHour(), signingTime.getMinute(), signingTime.getSecond());
                long localTime = calendar.getTimeInMillis();
                return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG).format(new java.util.Date(localTime));
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @NonNull
    public static String getSigner(@NonNull Context context, VerificationResult result, @NonNull DigitalSignatureField digSigField) throws PDFNetException {
        DigitalSignatureField.SubFilterType subfilter = digSigField.getSubFilter();
        String signer;
        // Try to get signer name from digital signature field
        if (subfilter != DigitalSignatureField.SubFilterType.e_ETSI_RFC3161) {
            String signatureName = digSigField.getSignatureName();
            if (Utils.isNullOrEmpty(signatureName)) {
                signer = digSigField.getContactInfo();
            } else {
                signer = signatureName;
            }
        } else {
            return context.getString(R.string.dialog_digital_signature_unknown);
        }

        // If signer name is null or empty, then check certificate
        if (Utils.isNullOrEmpty(signer)) {
            if (result.hasTrustVerificationResult()) {
                TrustVerificationResult trustVerificationResult = result.getTrustVerificationResult();
                X509Certificate[] certPath = trustVerificationResult.getCertPath();
                if (certPath.length > 0) {
                    X509Certificate firstX509Cert = certPath[0];
                    X501DistinguishedName retrievedSubjectField = firstX509Cert.getSubjectField();
                    Map<String, String> subjectField = processX501DistinguishedName(retrievedSubjectField);
                    signer = subjectField.get("e_commonName");
                }
            }
        }

        // Finally if the signing name is still null or empty, then show "Unknown"
        if (Utils.isNullOrEmpty(signer)) {
            return context.getString(R.string.dialog_digital_signature_unknown);
        }

        return signer;
    }

    @NonNull
    private static DigitalSignatureListDialog.DigitalSignatureBadge getDigitalSignatureBadge(@NonNull VerificationResult result) throws PDFNetException {
        DigitalSignatureListDialog.DigitalSignatureBadge badge;
        VerificationResult.DocumentStatus documentStatus = result.getDocumentStatus();
        VerificationResult.TrustStatus trustStatus = result.getTrustStatus();
        VerificationResult.ModificationPermissionsStatus permissionsStatus = result.getPermissionsStatus();
        VerificationResult.DigestStatus digestStatus = result.getDigestStatus();
        if (result.getVerificationStatus()) {
            badge = DigitalSignatureListDialog.DigitalSignatureBadge.VALID;
        } else if (
                documentStatus == VerificationResult.DocumentStatus.e_no_error &&
                        (digestStatus == VerificationResult.DigestStatus.e_digest_verified ||
                                digestStatus == VerificationResult.DigestStatus.e_digest_verification_disabled) &&
                        trustStatus != VerificationResult.TrustStatus.e_no_trust_status &&
                        (permissionsStatus == VerificationResult.ModificationPermissionsStatus.e_unmodified ||
                                permissionsStatus == VerificationResult.ModificationPermissionsStatus.e_has_allowed_changes ||
                                permissionsStatus == VerificationResult.ModificationPermissionsStatus.e_permissions_verification_disabled)
        ) {
            badge = DigitalSignatureListDialog.DigitalSignatureBadge.WARNING;
        } else {
            badge = DigitalSignatureListDialog.DigitalSignatureBadge.ERROR;
        }
        return badge;
    }

    @NonNull
    public static String getDigestAlgorithm(@NonNull Context context, VerificationResult result) throws PDFNetException {
        String digestAlgorithmFormat = context.getString(R.string.dialog_digital_signature_digest_algorithm);
        String digestAlgorithm;
        switch (result.getDigestAlgorithm()) {
            case e_sha1:
                digestAlgorithm = String.format(digestAlgorithmFormat, context.getString(R.string.dialog_digital_signature_digest_algorithm_sha1));
                break;
            case e_sha256:
                digestAlgorithm = String.format(digestAlgorithmFormat, context.getString(R.string.dialog_digital_signature_digest_algorithm_sha256));
                break;
            case e_sha384:
                digestAlgorithm = String.format(digestAlgorithmFormat, context.getString(R.string.dialog_digital_signature_digest_algorithm_sha384));
                break;
            case e_sha512:
                digestAlgorithm = String.format(digestAlgorithmFormat, context.getString(R.string.dialog_digital_signature_digest_algorithm_sha512));
                break;
            case e_ripemd160:
                digestAlgorithm = String.format(digestAlgorithmFormat, context.getString(R.string.dialog_digital_signature_digest_algorithm_ripemd160));
                break;
            case e_unknown_digest_algorithm:
            default:
                digestAlgorithm = context.getString(R.string.dialog_digital_signature_digest_algorithm_unknown);
                break;
        }
        return digestAlgorithm;
    }

    @NonNull
    public static String getDigestStatus(@NonNull Context context, VerificationResult result) throws PDFNetException {
        String digestStatus;
        switch (result.getDigestStatus()) {
            case e_digest_invalid:
                digestStatus = context.getString(R.string.dialog_digital_signature_digest_status_invalid);
                break;
            case e_digest_verified:
                digestStatus = context.getString(R.string.dialog_digital_signature_digest_status_verified);
                break;
            case e_digest_verification_disabled:
                digestStatus = context.getString(R.string.dialog_digital_signature_digest_status_verification_disabled);
                break;
            case e_weak_digest_algorithm_but_digest_verifiable:
                digestStatus = context.getString(R.string.dialog_digital_signature_digest_status_weak_digest_algorithm_but_digest_verifiable);
                break;
            case e_no_digest_status:
                digestStatus = context.getString(R.string.dialog_digital_signature_digest_status_none);
                break;
            case e_unsupported_digest_algorithm:
                digestStatus = context.getString(R.string.dialog_digital_signature_digest_status_unsupported_algorithm);
                break;
            case e_unsupported_encoding: // according to core, this should never be used.
                digestStatus = context.getString(R.string.dialog_digital_signature_digest_status_unsupported_encoding);
                break;
            default:
                throw new RuntimeException("Could not obtain digest status");
        }
        return digestStatus;
    }

    @NonNull
    public static String getGeneralErrorReport(@NonNull Context context, VerificationResult result) throws PDFNetException {
        String generalErrorReport;
        switch (result.getDocumentStatus()) {
            case e_no_error:
                generalErrorReport = context.getString(R.string.dialog_digital_signature_general_error_report_none);
                break;
            case e_corrupt_file:
                generalErrorReport = context.getString(R.string.dialog_digital_signature_general_error_report_corrupt_file);
                break;
            case e_unsigned:
                generalErrorReport = context.getString(R.string.dialog_digital_signature_general_error_report_unsigned);
                break;
            case e_bad_byteranges:
                generalErrorReport = context.getString(R.string.dialog_digital_signature_general_error_report_bad_byteranges);
                break;
            case e_corrupt_cryptographic_contents:
                generalErrorReport = context.getString(R.string.dialog_digital_signature_general_error_report_corrupt_cryptographic_contents);
                break;
            default:
                throw new RuntimeException("Could not obtain document status");
        }

        return generalErrorReport;
    }

    @NonNull
    public static String getTimeOfTrustVerificationDetails(@NonNull Context context, VerificationResult result) throws PDFNetException {
        String verificationTimeMessage;
        if (result.hasTrustVerificationResult()) {
            TrustVerificationResult trustVerificationResult = result.getTrustVerificationResult();
            // Date time message
            VerificationOptions.TimeMode timeMode = trustVerificationResult.getTimeOfTrustVerificationEnum();
            switch (timeMode) {
                case e_current:
                    verificationTimeMessage = context.getString(R.string.dialog_digital_signature_time_of_trust_details_current);
                    break;
                case e_signing:
                    verificationTimeMessage = context.getString(R.string.dialog_digital_signature_time_of_trust_details_signing);
                    break;
                case e_timestamp:
                    verificationTimeMessage = context.getString(R.string.dialog_digital_signature_time_of_trust_details_timestamp);
                    break;
                default:
                    throw new RuntimeException("Could not obtain time of trust verification");
            }
        } else {
            verificationTimeMessage = context.getString(R.string.dialog_digital_signature_verification_result_not_available);
        }

        return verificationTimeMessage;
    }

    @NonNull
    public static String getTimeOfTrustVerification(@NonNull Context context, VerificationResult result) throws PDFNetException {
        String dateTime;
        if (result.hasTrustVerificationResult()) {
            TrustVerificationResult trustVerificationResult = result.getTrustVerificationResult();
            // Date time
            long epochTimeOfTrustVerification = trustVerificationResult.getTimeOfTrustVerification();
            dateTime = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.LONG).format(epochTimeOfTrustVerification * 1000);
        } else {
            dateTime = context.getString(R.string.dialog_digital_signature_unknown);
        }
        return dateTime;
    }

    @NonNull
    public static String getTimeOfTrustVerificationWithExplanation(@NonNull Context context, VerificationResult result) throws PDFNetException {
        String timeOfTrustVerification;
        if (result.hasTrustVerificationResult() && result.getTrustVerificationResult().getResultString() != null) {
            TrustVerificationResult trustVerificationResult = result.getTrustVerificationResult();
            VerificationOptions.TimeMode timeOfTrustVerificationEnum = trustVerificationResult.getTimeOfTrustVerificationEnum();
            long epochTimeOfTrustVerification = trustVerificationResult.getTimeOfTrustVerification();
            String dateTime = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.LONG).format(epochTimeOfTrustVerification * 1000);

            switch (timeOfTrustVerificationEnum) {
                case e_current:
                    timeOfTrustVerification = context.getString(R.string.dialog_digital_signature_time_of_trust_explanation_current);
                    break;
                case e_signing:
                    timeOfTrustVerification = String.format(context.getString(R.string.dialog_digital_signature_time_of_trust_explanation_signing), dateTime);
                    break;
                case e_timestamp:
                    timeOfTrustVerification = String.format(context.getString(R.string.dialog_digital_signature_time_of_trust_explanation_timestamp), dateTime);
                    break;
                default:
                    throw new RuntimeException("Could not obtain time of trust verification");
            }
        } else {
            timeOfTrustVerification = context.getString(R.string.dialog_digital_signature_verification_result_not_available);
        }
        return timeOfTrustVerification;
    }

    @NonNull
    public static String getTrustStatus(@NonNull Context context, @NonNull DigitalSignatureField digSigField, VerificationResult result) throws PDFNetException {
        boolean isCert = digSigField.isCertification();
        VerificationResult.TrustStatus trustStatus = result.getTrustStatus();
        String trustStatusMessage;
        switch (trustStatus) {
            case e_trust_verified:
                if (isCert) {
                    trustStatusMessage = context.getString(R.string.dialog_digital_signature_trust_status_verified_certifier);
                } else {
                    trustStatusMessage = context.getString(R.string.dialog_digital_signature_trust_status_verified_signer);
                }
                break;
            case e_untrusted:
                trustStatusMessage = context.getString(R.string.dialog_digital_signature_trust_status_untrusted);
                break;
            case e_trust_verification_disabled:
                trustStatusMessage = context.getString(R.string.dialog_digital_signature_trust_status_verification_disabled);
                break;
            case e_no_trust_status:
                trustStatusMessage = context.getString(R.string.dialog_digital_signature_trust_status_none);
                break;
            case e_unsupported_trust_features:
                trustStatusMessage = context.getString(R.string.dialog_digital_signature_trust_status_unsupported);
                break;
            default:
                throw new RuntimeException("Could not obtain trust status");
        }
        return trustStatusMessage;
    }

    @NonNull
    public static String getValiditySummary(@NonNull Context context, @NonNull DigitalSignatureField digSigField, VerificationResult result) throws PDFNetException {
        DigitalSignatureListDialog.DigitalSignatureBadge badge = getDigitalSignatureBadge(result);
        boolean isCert = digSigField.isCertification();
        String status;
        switch (badge) {
            case VALID:
                if (isCert) {
                    status = context.getString(R.string.dialog_digital_signature_dig_cert_valid);
                } else {
                    status = context.getString(R.string.dialog_digital_signature_dig_sig_valid);
                }
                break;
            case ERROR:
                if (isCert) {
                    status = context.getString(R.string.dialog_digital_signature_dig_cert_invalid);
                } else {
                    status = context.getString(R.string.dialog_digital_signature_dig_sig_invalid);
                }
                break;
            case WARNING:
            default:
                if (isCert) {
                    status = context.getString(R.string.dialog_digital_signature_unknown_cert);
                } else {
                    status = context.getString(R.string.dialog_digital_signature_unknown_sig);
                }
                break;
        }

        return status;
    }

    @Nullable
    public static String getSignerSummary(@NonNull Context context, @NonNull DigitalSignatureField digSigField, VerificationResult result) throws PDFNetException {
        DigitalSignatureListDialog.DigitalSignatureBadge badge = getDigitalSignatureBadge(result);
        if (digSigField.isCertification()) {
            if (badge == DigitalSignatureListDialog.DigitalSignatureBadge.VALID) {
                String signer = getSigner(context, result, digSigField);
                return String.format(context.getString(R.string.dialog_digital_signature_certifier_name), signer);
            } else {
                return null;
            }
        } else {
            if (badge == DigitalSignatureListDialog.DigitalSignatureBadge.VALID) {
                String signer = getSigner(context, result, digSigField);
                return String.format(context.getString(R.string.dialog_digital_signature_signer_name), signer);
            } else {
                return null;
            }
        }
    }

    @NonNull
    public static DigitalSignatureInfo getDigitalSignatureInformation(@NonNull Context context, DigitalSignatureField digSigField, PDFViewCtrl pdfViewCtrl) throws PDFNetException {
        VerificationResult result = getVerificationResult(pdfViewCtrl.getContext(), digSigField);

        // *********************************************************************************
        // Get header string
        // *********************************************************************************
        String signer = getSigner(context, result, digSigField);
        final String signingTimeString = getSigningTime(digSigField);

        String header;
        if (digSigField.isCertification()) {
            if (signingTimeString != null) {
                header = String.format(context.getString(R.string.dialog_digital_signature_certifier_with_time), signer, signingTimeString);
            } else {
                header = String.format(context.getString(R.string.dialog_digital_signature_certifier_name), signer);
            }
        } else {
            if (signingTimeString != null) {
                header = String.format(context.getString(R.string.dialog_digital_signature_signer_with_time), signer, signingTimeString);
            } else {
                header = String.format(context.getString(R.string.dialog_digital_signature_signer_name), signer);
            }
        }

        // *********************************************************************************
        // Get verification message
        // *********************************************************************************
        final String verificationMsg = getFieldVerificationStatus(context, result, digSigField);

        // *********************************************************************************
        // Get permission status and disallowed changes if applicable
        // *********************************************************************************
        String permission = getPermissionStatus(context, result, digSigField);

        String disallowedChangesMsg = getDisallowedChanges(context, result);

        // *********************************************************************************
        // Get trust verification message, verification time, and verification time message
        // *********************************************************************************
        final String trustVerificationResultMsg;
        String dateTime;
        String dateTimeMsg;
        if (result.hasTrustVerificationResult() && result.getTrustVerificationResult().getResultString() != null) {
            // Verification message
            // Currently we do not show the verification result string
            trustVerificationResultMsg = context.getString(R.string.dialog_digital_signature_verification_result_verified);
            dateTime = getTimeOfTrustVerification(context, result);
            dateTimeMsg = getTimeOfTrustVerificationDetails(context, result);
        } else {
            trustVerificationResultMsg = context.getString(R.string.dialog_digital_signature_verification_result_not_available);
            dateTime = null;
            dateTimeMsg = null;
        }

        // *********************************************************************************
        // Get badge icon
        // *********************************************************************************
        DigitalSignatureListDialog.DigitalSignatureBadge badge = getDigitalSignatureBadge(result);

        // *********************************************************************************
        // Get additional details
        // *********************************************************************************
        DigitalSignatureField.SubFilterType subfilter = digSigField.getSubFilter();
        String contactInfo = null;
        String location = null;
        String reason = null;
        String signingTime = signingTimeString;
        boolean additionalDetailsAvailable = false;
        if (subfilter != DigitalSignatureField.SubFilterType.e_ETSI_RFC3161) {
            contactInfo = digSigField.getContactInfo();
            location = digSigField.getLocation();
            reason = digSigField.getReason();
            if (!(Utils.isNullOrEmpty(contactInfo) && Utils.isNullOrEmpty(location) && Utils.isNullOrEmpty(reason))) {
                additionalDetailsAvailable = true;
            }
        }

        if (Utils.isNullOrEmpty(contactInfo)) {
            contactInfo = context.getString(R.string.dialog_digital_signature_info_no_contact_info);
        }
        if (Utils.isNullOrEmpty(location)) {
            location = context.getString(R.string.dialog_digital_signature_info_no_location);
        }
        if (Utils.isNullOrEmpty(reason)) {
            reason = context.getString(R.string.dialog_digital_signature_info_no_reason);
        }
        if (Utils.isNullOrEmpty(signingTime)) {
            signingTime = context.getString(R.string.dialog_digital_signature_info_no_signing_time);
        }

        DigitalSignatureProperties digitalSignatureProperties = getDigitalSignatureProperties(context, digSigField, pdfViewCtrl);

        return new DigitalSignatureInfo(
                badge,
                additionalDetailsAvailable,
                header,
                verificationMsg,
                permission,
                disallowedChangesMsg,
                trustVerificationResultMsg,
                dateTime,
                dateTimeMsg,
                contactInfo,
                location,
                reason,
                signingTime,
                digitalSignatureProperties
        );
    }

    /**
     * Processes an instance of the PDFNet.X501DistinguishedName class into a
     * Javascript object that is human readable
     * <p>
     * Intended to process the objects returned from invoking
     * PDFNet.X509Certificate.GetIssuerField and
     * PDFNet.X509Certificate.GetSubjectField
     *
     * @param {PDFNet.X501DistinguishedName} x501DistinguishedNameObject An instance
     *                                       of the PDFNet.X501DistinguishedName class, to be processed into a Javascript
     *                                       object
     * @returns {object} Maps human readable keys (as opposed to the Botan Crpyto
     * OIDs, represented as Array<int> in PDFTron Core) to the corresponding values
     * they map to
     * @ignore
     */
    // Reference: WebViewer setVerificationResult.js
    private static Map<String, String> processX501DistinguishedName(X501DistinguishedName x501DistinguishedName) throws PDFNetException {
        Map<String, String> processedObject = new HashMap<>();
        X501AttributeTypeAndValue[] allAttributesAndValues = x501DistinguishedName.getAllAttributesAndValues();
        for (X501AttributeTypeAndValue x501AttributeTypeAndValue : allAttributesAndValues) {
            ObjectIdentifier objectIdentifier = x501AttributeTypeAndValue.getAttributeTypeOID();
            int[] key = objectIdentifier.getRawValue();
            String value = x501AttributeTypeAndValue.getStringValue();

            String map = translateObjectIdentifierBotanOID(key);
            if (map != null) {
                processedObject.put(map, value);
            }
        }
        return processedObject;
    }

    /**
     * Takes an Array<Number> argument (or its string representation from
     * JSON.stringify) and returns the enum it is supposed to represent based on
     * its OID representation in the Botan crypto C++ library
     * <p>
     * PDFTron Core represents the key from the original Map<string, string>
     * data-structure in the form of an array
     *
     * @param {string | Array<Number>} objectIdentifierOIDenum The array returned
     *                from the invocation of PDFNet.ObjectIdentifier.getRawValue, which can be
     *                accepted as the Array input (which the body of the function will convert to a
     *                string), or a string representation of the array
     * @example The key of the object
     * { "2.5.4.3", "X520.CommonName" }
     * Is represented as [2,5,4,3] in PDFTron Core
     * <p>
     * Source: https://botan.randombit.net/doxygen/oid__maps_8cpp_source.html
     * @returns {string} The human readable enum that the array represents
     * @ignore
     */
    // Reference: WebViewer setVerificationResult.js
    @Nullable
    private static String translateObjectIdentifierBotanOID(int[] objectIdentifierOIDenum) {
        JsonArray arrayAsJson = new JsonArray();
        for (int i : objectIdentifierOIDenum) {
            arrayAsJson.add(i);
        }
        String arrayAsString = arrayAsJson.toString();
        switch (arrayAsString) {
            case "[2,5,4,3]":
                return "e_commonName";
            case "[2,5,4,4]":
                return "e_surname";
            case "[2,5,4,6]":
                return "e_countryName";
            case "[2,5,4,7]":
                return "e_localityName";
            case "[2,5,4,8]":
                return "e_stateOrProvinceName";
            case "[2,5,4,9]":
                return "e_streetAddress";
            case "[2,5,4,10]":
                return "e_organizationName";
            case "[2,5,4,11]":
                return "e_organizationalUnitName";
            case "[1,2,840,113549,1,9,1]":
                return "e_emailAddress";
            default:
                return null;
        }
    }

    @NonNull
    public static DigitalSignatureProperties getDigitalSignatureProperties(@NonNull Context context, @NonNull DigitalSignatureField digSigField, @NonNull PDFViewCtrl pdfViewCtrl) throws PDFNetException {
        VerificationResult result = getVerificationResult(pdfViewCtrl.getContext(), digSigField);

        // *********************************************************************************
        // Summary box
        // *********************************************************************************
        String validitySummary = getValiditySummary(context, digSigField, result);
        String signerSummary = getSignerSummary(context, digSigField, result);
        // *********************************************************************************
        // Permission status and permission details
        // *********************************************************************************
        String permissionStatus = getPermissionStatus(context, result, digSigField);
        String permissionDetails = getPermissionDetails(context, digSigField);

        // *********************************************************************************
        // Identities and Trust
        // *********************************************************************************
        String trustStatus = getTrustStatus(context, digSigField, result);

        String timeOfTrustVerification = getTimeOfTrustVerificationWithExplanation(context, result);

        // *********************************************************************************
        // General Errors
        // *********************************************************************************
        String generalErrorReport = getGeneralErrorReport(context, result);

        // *********************************************************************************
        // Digest Status
        // *********************************************************************************
        String digestStatus = getDigestStatus(context, result);

        String digestAlgorithm = getDigestAlgorithm(context, result);
        return new DigitalSignatureProperties(
                getDigitalSignatureBadge(result),
                validitySummary,
                signerSummary,
                permissionStatus,
                permissionDetails,
                trustStatus,
                timeOfTrustVerification,
                generalErrorReport,
                digestStatus,
                digestAlgorithm
        );
    }

    public static boolean hasDigitalSignatures(@NonNull PDFDoc doc) {
        boolean shouldUnlockRead = false;
        try {
            doc.lockRead();
            shouldUnlockRead = true;
            DigitalSignatureFieldIterator digitalSignatureFieldIterator = doc.getDigitalSignatureFieldIterator();
            return digitalSignatureFieldIterator.hasNext();
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                Utils.unlockReadQuietly(doc);
            }
        }
        return false;
    }
}
