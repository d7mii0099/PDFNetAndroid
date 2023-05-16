//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------
package com.pdftron.android.pdfnetsdksamples.samples

import com.pdftron.android.pdfnetsdksamples.OutputListener
import com.pdftron.android.pdfnetsdksamples.PDFNetSample
import com.pdftron.android.pdfnetsdksamples.R
import com.pdftron.android.pdfnetsdksamples.util.Utils
import com.pdftron.common.PDFNetException
import com.pdftron.crypto.DigestAlgorithm
import com.pdftron.crypto.X501AttributeTypeAndValue
import com.pdftron.crypto.X509Certificate
import com.pdftron.pdf.*
import com.pdftron.pdf.annots.SignatureWidget
import com.pdftron.pdf.annots.TextWidget
import com.pdftron.sdf.SDFDoc

//----------------------------------------------------------------------------------------------------------------------
// This sample demonstrates the basic usage of the high-level digital signatures API in PDFNet.
//
// The following steps reflect typical intended usage of the digital signatures API:
//
//	0.	Start with a PDF with or without form fields in it that one would like to lock (or, one can add a field, see (1)).
//
//	1.	EITHER:
//		(a) Call doc.CreateDigitalSignatureField, optionally providing a name. You receive a DigitalSignatureField.
//		-OR-
//		(b) If you didn't just create the digital signature field that you want to sign/certify, find the existing one within the
//		document by using PDFDoc.DigitalSignatureFieldIterator or by using PDFDoc.GetField to get it by its fully qualified name.
//
//	2.	Create a signature widget annotation, and pass the DigitalSignatureField that you just created or found.
//		If you want it to be visible, provide a Rect argument with a non-zero width or height, and don't set the
//		NoView and Hidden flags. [Optionally, add an appearance to the annotation when you wish to sign/certify.]
//
//	[3. (OPTIONAL) Add digital signature restrictions to the document using the field modification permissions (SetFieldPermissions)
//		or document modification permissions functions (SetDocumentPermissions) of DigitalSignatureField. These features disallow
//		certain types of changes to be made to the document without invalidating the cryptographic digital signature's hash once it
//		is signed.]
//
//	4. 	Call either CertifyOnNextSave or SignOnNextSave. There are three overloads for each one (six total):
//		a.	Taking a PKCS #12 keyfile path and its password
//		b.	Taking a buffer containing a PKCS #12 private keyfile and its password
//		c.	Taking a unique identifier of a signature handler registered with the PDFDoc. This overload is to be used
//			in the following fashion:
//			i)		Extend and implement a new SignatureHandler. The SignatureHandler will be used to add or
//					validate/check a digital signature.
//			ii)		Create an instance of the implemented SignatureHandler and register it with PDFDoc with
//					pdfdoc.AddSignatureHandler(). The method returns a SignatureHandlerId.
//			iii)	Call SignOnNextSaveWithCustomHandler/CertifyOnNextSaveWithCustomHandler with the SignatureHandlerId.
//		NOTE: It is only possible to sign/certify one signature per call to the Save function.
//
//	5.	Call pdfdoc.Save(). This will also create the digital signature dictionary and write a cryptographic hash to it.
//		IMPORTANT: If there are already signed/certified digital signature(s) in the document, you must save incrementally
//		so as to not invalidate the other signature's('s) cryptographic hashes.
//
// Additional processing can be done before document is signed. For example, UseSignatureHandler() returns an instance
// of SDF dictionary which represents the signature dictionary (or the /V entry of the form field). This can be used to
// add additional information to the signature dictionary (e.g. Name, Reason, Location, etc.).
//
// Although the steps above describes extending the SignatureHandler class, this sample demonstrates the use of
// StdSignatureHandler (a built-in SignatureHandler in PDFNet) to sign a PDF file.
//----------------------------------------------------------------------------------------------------------------------
class DigitalSignaturesTest : PDFNetSample() {
    override fun run(outputListener: OutputListener?) {
        mOutputListener = outputListener
        printHeader(outputListener!!)
        printFooter(outputListener)
    }

    override fun runOnBackground() {
        super.runOnBackground()
        mFileList.clear()
        // Initialize PDFNet
        var result = true

        //////////////////// TEST 0:
        /* Create an approval signature field that we can sign after certifying.
		(Must be done before calling CertifyOnNextSave/SignOnNextSave/WithCustomHandler.) */try {
            val doc = PDFDoc(Utils.getAssetTempFile(INPUT_PATH.toString() + "waiver.pdf")!!.getAbsolutePath())
            val approval_signature_field: DigitalSignatureField = doc.createDigitalSignatureField("PDFTronApprovalSig")
            val widgetAnnotApproval: SignatureWidget = SignatureWidget.create(doc, com.pdftron.pdf.Rect(300.0, 287.0, 376.0, 306.0), approval_signature_field)
            val page1: com.pdftron.pdf.Page = doc.getPage(1)
            page1.annotPushBack(widgetAnnotApproval)
            doc.save(Utils.createExternalFile("waiver_withApprovalField_output.pdf", mFileList).getAbsolutePath(), SDFDoc.SaveMode.REMOVE_UNUSED, null)
        } catch (e: java.lang.Exception) {
            java.lang.System.err.println(e.message)
            e.printStackTrace(java.lang.System.err)
            result = false
        }

        //////////////////// TEST 1: certify a PDF.
        try {
            certifyPDF(Utils.getAssetTempFile(INPUT_PATH.toString() + "waiver_withApprovalField.pdf")!!.getAbsolutePath(),
                    "PDFTronCertificationSig",
                    Utils.getAssetTempFile(INPUT_PATH.toString() + "pdftron.pfx")!!.getAbsolutePath(),
                    "password",
                    Utils.getAssetTempFile(INPUT_PATH.toString() + "pdftron.bmp")!!.getAbsolutePath(),
                    Utils.createExternalFile("waiver_withApprovalField_certified_output.pdf", mFileList).getAbsolutePath())
            printSignaturesInfo(Utils.createExternalFile("waiver_withApprovalField_certified_output.pdf", mFileList).getAbsolutePath())
        } catch (e: java.lang.Exception) {
            java.lang.System.err.println(e.message)
            e.printStackTrace(java.lang.System.err)
            result = false
        }

        //////////////////// TEST 2: approval-sign an existing, unsigned signature field in a PDF that already has a certified signature field.
        try {
            signPDF(Utils.getAssetTempFile(INPUT_PATH.toString() + "waiver_withApprovalField_certified.pdf")!!.getAbsolutePath(),
                    "PDFTronApprovalSig",
                    Utils.getAssetTempFile(INPUT_PATH.toString() + "pdftron.pfx")!!.getAbsolutePath(),
                    "password",
                    Utils.getAssetTempFile(INPUT_PATH.toString() + "signature.jpg")!!.getAbsolutePath(),
                    Utils.createExternalFile("waiver_withApprovalField_certified_approved_output.pdf", mFileList).getAbsolutePath())
            printSignaturesInfo(Utils.createExternalFile("waiver_withApprovalField_certified_approved_output.pdf", mFileList).getAbsolutePath())
        } catch (e: java.lang.Exception) {
            java.lang.System.err.println(e.message)
            e.printStackTrace(java.lang.System.err)
            result = false
        }

        //////////////////// TEST 3: Clear a certification from a document that is certified and has an approval signature.
        try {
            clearSignature(Utils.getAssetTempFile(INPUT_PATH.toString() + "waiver_withApprovalField_certified_approved.pdf")!!.getAbsolutePath(),
                    "PDFTronCertificationSig",
                    Utils.createExternalFile("waiver_withApprovalField_certified_approved_certcleared_output.pdf", mFileList).getAbsolutePath())
            printSignaturesInfo(Utils.createExternalFile("waiver_withApprovalField_certified_approved_certcleared_output.pdf", mFileList).getAbsolutePath())
        } catch (e: java.lang.Exception) {
            java.lang.System.err.println(e.message)
            e.printStackTrace(java.lang.System.err)
            result = false
        }

        //////////////////// TEST 4: Verify a document's digital signatures.
        try {
            if (!verifyAllAndPrint(Utils.getAssetTempFile(INPUT_PATH.toString() + "waiver_withApprovalField_certified_approved.pdf")!!.getAbsolutePath(),
                            Utils.getAssetTempFile(INPUT_PATH.toString() + "pdftron.cer")!!.getAbsolutePath())) {
                result = false
            }
        } catch (e: java.lang.Exception) {
            java.lang.System.err.println(e.message)
            e.printStackTrace(java.lang.System.err)
            result = false
        }
        //////////////////// TEST 5: Verify a document's digital signatures in a simple fashion using the document API.
        try {
            if (!verifySimple(Utils.getAssetTempFile(INPUT_PATH.toString() + "waiver_withApprovalField_certified_approved.pdf")!!.getAbsolutePath(),
                            Utils.getAssetTempFile(INPUT_PATH.toString() + "pdftron.cer")!!.getAbsolutePath())) {
                result = false
            }
        } catch (e: java.lang.Exception) {
            java.lang.System.err.println(e.message)
            e.printStackTrace(java.lang.System.err)
            result = false
        }
        //////////////////// TEST 6: Timestamp a document, then add Long Term Validation (LTV) information for the DocTimeStamp.
        //try
        //{
        //	if (!timestampAndEnableLTV(Utils.getAssetTempFile(INPUT_PATH + "waiver.pdf").getAbsolutePath(),
        //	Utils.getAssetTempFile(INPUT_PATH + "GlobalSignRootForTST.cer").getAbsolutePath(),
        //	Utils.getAssetTempFile(INPUT_PATH + "signature.jpg").getAbsolutePath(),
        //	Utils.createExternalFile("waiver_DocTimeStamp_LTV.pdf", mFileList).getAbsolutePath()))
        //	{
        //		result = false;
        //	}
        //}
        //catch (Exception e)
        //{
        //	System.err.println(e.getMessage());
        //	e.printStackTrace(System.err);
        //	result = false;
        //
        //}

        //////////////////// End of tests. ////////////////////
        if (result) {
            println("Tests successful.\n==========")
        } else {
            println("Tests FAILED!!!\n==========")
        }
        for (file in mFileList) {
            addToFileList(file)
        }
    }

    companion object {
        private var mOutputListener: OutputListener? = null
        private val mFileList: java.util.ArrayList<String> = java.util.ArrayList<String>()
        @Throws(PDFNetException::class)
        fun verifySimple(in_docpath: String?, in_public_key_file_path: String?): Boolean {
            val doc = PDFDoc(in_docpath)
            println("==========")
            val opts = VerificationOptions(VerificationOptions.SecurityLevel.e_compatibility_and_archiving)

            // Add trust root to store of trusted certificates contained in VerificationOptions.
            opts.addTrustedCertificate(in_public_key_file_path, (
                    VerificationOptions.CertificateTrustFlag.e_default_trust.value or VerificationOptions.CertificateTrustFlag.e_certification_trust.value).toLong())
            val result: PDFDoc.SignaturesVerificationStatus = doc.verifySignedDigitalSignatures(opts)
            when (result) {
                PDFDoc.SignaturesVerificationStatus.e_unsigned -> {
                    println("Document has no signed signature fields.")
                    return false
                }
                PDFDoc.SignaturesVerificationStatus.e_failure -> {
                    println("Hard failure in verification on at least one signature.")
                    return false
                }
                PDFDoc.SignaturesVerificationStatus.e_untrusted -> {
                    println("Could not verify trust for at least one signature.")
                    return false
                }
                PDFDoc.SignaturesVerificationStatus.e_unsupported -> {
                    /*If necessary, call GetUnsupportedFeatures on VerificationResult to check which
			unsupported features were encountered (requires verification using 'detailed' APIs) */println("At least one signature contains unsupported features.")
                    return false
                }
                PDFDoc.SignaturesVerificationStatus.e_verified -> {
                    println("All signed signatures in document verified.")
                    return true
                }
                else -> {
                    java.lang.System.err.println("unrecognized document verification status")
                    assert(false)
                }
            }
            return false
        }

        @Throws(PDFNetException::class)
        fun verifyAllAndPrint(in_docpath: String?, in_public_key_file_path: String?): Boolean {
            val doc = PDFDoc(in_docpath)
            println("==========")
            val opts = VerificationOptions(VerificationOptions.SecurityLevel.e_compatibility_and_archiving)

            // Add trust root to store of trusted certificates contained in VerificationOptions.
            opts.addTrustedCertificate(in_public_key_file_path, (
                    VerificationOptions.CertificateTrustFlag.e_default_trust.value or VerificationOptions.CertificateTrustFlag.e_certification_trust.value).toLong())

            // Iterate over the signatures and verify all of them.
            val digsig_fitr: DigitalSignatureFieldIterator = doc.getDigitalSignatureFieldIterator()
            var verification_status = true
            while (digsig_fitr.hasNext()) {
                val curr: DigitalSignatureField? = digsig_fitr.next()
                val result: VerificationResult = curr!!.verify(opts)
                if (result.getVerificationStatus()) {
                    print("Signature verified, ")
                } else {
                    print("Signature verification failed, ")
                    verification_status = false
                }
                println(String.format(java.util.Locale.US, "objnum: %d", curr.getSDFObj().getObjNum()))
                when (result.getDigestAlgorithm()) {
                    DigestAlgorithm.e_sha1 -> println("Digest algorithm: SHA-1")
                    DigestAlgorithm.e_sha256 -> println("Digest algorithm: SHA-256")
                    DigestAlgorithm.e_sha384 -> println("Digest algorithm: SHA-384")
                    DigestAlgorithm.e_sha512 -> println("Digest algorithm: SHA-512")
                    DigestAlgorithm.e_ripemd160 -> println("Digest algorithm: RIPEMD-160")
                    DigestAlgorithm.e_unknown_digest_algorithm -> println("Digest algorithm: unknown")
                    else -> {
                        java.lang.System.err.println("unrecognized digest algorithm")
                        assert(false)
                    }
                }
                println(String.format("Detailed verification result: \n\t%s\n\t%s\n\t%s\n\t%s",
                        result.getDocumentStatusAsString(),
                        result.getDigestStatusAsString(),
                        result.getTrustStatusAsString(),
                        result.getPermissionsStatusAsString()))
                val changes: Array<DisallowedChange> = result.getDisallowedChanges()
                for (it2 in changes) {
                    println(String.format(java.util.Locale.US, "\tDisallowed change: %s, objnum: %d", it2.getTypeAsString(), it2.getObjNum()))
                }

                // Get and print all the detailed trust-related results, if they are available.
                if (result.hasTrustVerificationResult()) {
                    val trust_verification_result: TrustVerificationResult = result.getTrustVerificationResult()
                    println(if (trust_verification_result.wasSuccessful()) "Trust verified." else "Trust not verifiable.")
                    println(trust_verification_result.getResultString())
                    val time_of_verification: Long = trust_verification_result.getTimeOfTrustVerification()
                    when (trust_verification_result.getTimeOfTrustVerificationEnum()) {
                        VerificationOptions.TimeMode.e_current -> println(String.format(java.util.Locale.US, "Trust verification attempted with respect to current time (as epoch time): %d", time_of_verification))
                        VerificationOptions.TimeMode.e_signing -> println(String.format(java.util.Locale.US, "Trust verification attempted with respect to signing time (as epoch time): %d", time_of_verification))
                        VerificationOptions.TimeMode.e_timestamp -> println(String.format(java.util.Locale.US, "Trust verification attempted with respect to secure embedded timestamp (as epoch time): %d", time_of_verification))
                        else -> {
                            java.lang.System.err.println("unrecognized time enum value")
                            assert(false)
                        }
                    }
                    if (trust_verification_result.getCertPath().size == 0) {
                        println("Could not print certificate path.")
                    } else {
                        println("Certificate path:")
                        val cert_path: Array<X509Certificate> = trust_verification_result.getCertPath()
                        for (j in cert_path.indices) {
                            println("\tCertificate:")
                            val full_cert: X509Certificate = cert_path[j]
                            println("\t\tIssuer names:")
                            val issuer_dn: Array<X501AttributeTypeAndValue> = full_cert.getIssuerField().getAllAttributesAndValues()
                            for (i in issuer_dn.indices) {
                                java.lang.System.out.println("\t\t\t" + issuer_dn[i].getStringValue())
                            }
                            println("\t\tSubject names:")
                            val subject_dn: Array<X501AttributeTypeAndValue> = full_cert.getSubjectField().getAllAttributesAndValues()
                            for (i in subject_dn.indices) {
                                java.lang.System.out.println("\t\t\t" + subject_dn[i].getStringValue())
                            }
                            println("\t\tExtensions:")
                            for (i in 0 until full_cert.getExtensions().size) {
                                java.lang.System.out.println("\t\t\t" + full_cert.getExtensions().get(i).toString())
                            }
                        }
                    }
                } else {
                    println("No detailed trust verification result available.")
                }
                val unsupported_features: Array<String> = result.getUnsupportedFeatures()
                if (unsupported_features.size > 0) {
                    println("Unsupported features:")
                    for (unsupported_feature in unsupported_features) {
                        println("\t" + unsupported_feature)
                    }
                }
                println("==========")
            }
            return verification_status
        }

        @Throws(PDFNetException::class)
        fun certifyPDF(in_docpath: String?,
                in_cert_field_name: String?,
                in_private_key_file_path: String?,
                in_keyfile_password: String?,
                in_appearance_image_path: String?,
                in_outpath: String?) {
            println("================================================================================")
            println("Certifying PDF document")

            // Open an existing PDF
            val doc = PDFDoc(in_docpath)
            if (doc.hasSignatures()) {
                println("PDFDoc has signatures")
            } else {
                println("PDFDoc has no signatures")
            }
            val page1: com.pdftron.pdf.Page = doc.getPage(1)

            // Create a text field that we can lock using the field permissions feature.
            val annot1: TextWidget = TextWidget.create(doc, com.pdftron.pdf.Rect(143.0, 440.0, 350.0, 460.0), "asdf_test_field")
            page1.annotPushBack(annot1)

            /* Create a new signature form field in the PDFDoc. The name argument is optional;
		leaving it empty causes it to be auto-generated. However, you may need the name for later.
		Acrobat doesn't show digsigfield in side panel if it's without a widget. Using a
		Rect with 0 width and 0 height, or setting the NoPrint/Invisible flags makes it invisible. */
            val certification_sig_field: DigitalSignatureField = doc.createDigitalSignatureField(in_cert_field_name)
            val widgetAnnot: SignatureWidget = SignatureWidget.create(doc, com.pdftron.pdf.Rect(143.0, 287.0, 219.0, 306.0), certification_sig_field)
            page1.annotPushBack(widgetAnnot)

            // (OPTIONAL) Add an appearance to the signature field.
            val img: com.pdftron.pdf.Image = com.pdftron.pdf.Image.create(doc, in_appearance_image_path)
            widgetAnnot.createSignatureAppearance(img)

            // Prepare the document locking permission level. It will be applied upon document certification.
            println("Adding document permissions.")
            certification_sig_field.setDocumentPermissions(DigitalSignatureField.DocumentPermissions.e_annotating_formfilling_signing_allowed)

            // Prepare to lock the text field that we created earlier.
            println("Adding field permissions.")
            val fields_to_lock = arrayOf("asdf_test_field")
            certification_sig_field.setFieldPermissions(DigitalSignatureField.FieldPermissions.e_include, fields_to_lock)
            certification_sig_field.certifyOnNextSave(in_private_key_file_path, in_keyfile_password)

            // (OPTIONAL) Add more information to the signature dictionary.
            certification_sig_field.setLocation("Vancouver, BC")
            certification_sig_field.setReason("Document certification.")
            certification_sig_field.setContactInfo("www.pdftron.com")

            // Save the PDFDoc. Once the method below is called, PDFNet will also sign the document using the information provided.
            doc.save(in_outpath, SDFDoc.SaveMode.NO_FLAGS, null)
            println("================================================================================")
        }

        @Throws(PDFNetException::class)
        fun signPDF(in_docpath: String?,
                in_approval_field_name: String?,
                in_private_key_file_path: String?,
                in_keyfile_password: String?,
                in_appearance_img_path: String?,
                in_outpath: String?) {
            println("================================================================================")
            println("Signing PDF document")

            // Open an existing PDF
            val doc = PDFDoc(in_docpath)

            // Retrieve the unsigned approval signature field.
            val found_approval_field: com.pdftron.pdf.Field = doc.getField(in_approval_field_name)
            val found_approval_signature_digsig_field = DigitalSignatureField(found_approval_field)

            // (OPTIONAL) Add an appearance to the signature field.
            val img: com.pdftron.pdf.Image = com.pdftron.pdf.Image.create(doc, in_appearance_img_path)
            val found_approval_signature_widget = SignatureWidget(found_approval_field.getSDFObj())
            found_approval_signature_widget.createSignatureAppearance(img)

            // Prepare the signature and signature handler for signing.
            found_approval_signature_digsig_field.signOnNextSave(in_private_key_file_path, in_keyfile_password)

            // The actual approval signing will be done during the following incremental save operation.
            doc.save(in_outpath, SDFDoc.SaveMode.INCREMENTAL, null)
            println("================================================================================")
        }

        @Throws(PDFNetException::class)
        fun clearSignature(in_docpath: String?,
                in_digsig_field_name: String,
                in_outpath: String?) {
            println("================================================================================")
            println("Clearing certification signature")
            val doc = PDFDoc(in_docpath)
            val digsig = DigitalSignatureField(doc.getField(in_digsig_field_name))
            println("Clearing signature: $in_digsig_field_name")
            digsig.clearSignature()
            if (!digsig.hasCryptographicSignature()) {
                println("Cryptographic signature cleared properly.")
            }

            // Save incrementally so as to not invalidate other signatures from previous saves.
            doc.save(in_outpath, SDFDoc.SaveMode.INCREMENTAL, null)
            println("================================================================================")
        }

        @Throws(PDFNetException::class)
        fun printSignaturesInfo(in_docpath: String?) {
            println("================================================================================")
            println("Reading and printing digital signature information")
            val doc = PDFDoc(in_docpath)
            if (!doc.hasSignatures()) {
                println("Doc has no signatures.")
                println("================================================================================")
                return
            } else {
                println("Doc has signatures.")
            }
            val fitr: FieldIterator = doc.getFieldIterator()
            while (fitr.hasNext()) {
                val current: com.pdftron.pdf.Field? = fitr.next()
                if (current!!.isLockedByDigitalSignature()) {
                    println("==========\nField locked by a digital signature")
                } else {
                    println("==========\nField not locked by a digital signature")
                }
                println("Field name: " + current.getName())
                println("==========")
            }
            println("====================\nNow iterating over digital signatures only.\n====================")
            val digsig_fitr: DigitalSignatureFieldIterator = doc.getDigitalSignatureFieldIterator()
            while (digsig_fitr.hasNext()) {
                val current: DigitalSignatureField? = digsig_fitr.next()
                println("==========")
                println("Field name of digital signature: " + com.pdftron.pdf.Field(current!!.getSDFObj()).getName())
                val digsigfield: DigitalSignatureField = current
                if (!digsigfield.hasCryptographicSignature()) {
                    println("""
    Either digital signature field lacks a digital signature dictionary, or digital signature dictionary lacks a cryptographic Contents entry. Digital signature field is not presently considered signed.
    ==========
    """.trimIndent())
                    continue
                }
                val cert_count: Int = digsigfield.getCertCount()
                println("Cert count: $cert_count")
                for (i in 0 until cert_count) {
                    val cert: ByteArray = digsigfield.getCert(i)
                    println("Cert #" + i + " size: " + cert.size)
                }
                val subfilter: DigitalSignatureField.SubFilterType = digsigfield.getSubFilter()
                println("Subfilter type: " + subfilter.ordinal)
                if (subfilter != DigitalSignatureField.SubFilterType.e_ETSI_RFC3161) {
                    println("Signature's signer: " + digsigfield.getSignatureName())
                    val signing_time: com.pdftron.pdf.Date = digsigfield.getSigningTime()
                    if (signing_time.isValid()) {
                        println("Signing time is valid.")
                    }
                    println("Location: " + digsigfield.getLocation())
                    println("Reason: " + digsigfield.getReason())
                    println("Contact info: " + digsigfield.getContactInfo())
                } else {
                    println("SubFilter == e_ETSI_RFC3161 (DocTimeStamp; no signing info)")
                }
                if (digsigfield.hasVisibleAppearance()) {
                    println("Visible")
                } else {
                    println("Not visible")
                }
                val digsig_doc_perms: DigitalSignatureField.DocumentPermissions = digsigfield.getDocumentPermissions()
                val locked_fields: Array<String> = digsigfield.getLockedFields()
                for (it in locked_fields) {
                    println("This digital signature locks a field named: $it")
                }
                when (digsig_doc_perms) {
                    DigitalSignatureField.DocumentPermissions.e_no_changes_allowed -> println("No changes to the document can be made without invalidating this digital signature.")
                    DigitalSignatureField.DocumentPermissions.e_formfilling_signing_allowed -> println("Page template instantiation, form filling, and signing digital signatures are allowed without invalidating this digital signature.")
                    DigitalSignatureField.DocumentPermissions.e_annotating_formfilling_signing_allowed -> println("Annotating, page template instantiation, form filling, and signing digital signatures are allowed without invalidating this digital signature.")
                    DigitalSignatureField.DocumentPermissions.e_unrestricted -> println("Document not restricted by this digital signature.")
                    else -> {
                        java.lang.System.err.println("Unrecognized digital signature document permission level.")
                        assert(false)
                    }
                }
                println("==========")
            }
            println("================================================================================")
        }

        @Throws(PDFNetException::class)
        fun timestampAndEnableLTV(in_docpath: String?,
                in_trusted_cert_path: String?,
                in_appearance_img_path: String?,
                in_outpath: String?): Boolean {
            val doc = PDFDoc(in_docpath)
            val doctimestamp_signature_field: DigitalSignatureField = doc.createDigitalSignatureField()
            val tst_config = TimestampingConfiguration("http://rfc3161timestamp.globalsign.com/advanced")
            val opts = VerificationOptions(VerificationOptions.SecurityLevel.e_compatibility_and_archiving)
            /* It is necessary to add to the VerificationOptions a trusted root certificate corresponding to
		the chain used by the timestamp authority to sign the timestamp token, in order for the timestamp
		response to be verifiable during DocTimeStamp signing. It is also necessary in the context of this
		function to do this for the later LTV section, because one needs to be able to verify the DocTimeStamp
		in order to enable LTV for it, and we re-use the VerificationOptions opts object in that part. */opts.addTrustedCertificate(in_trusted_cert_path)
            /* By default, we only check online for revocation of certificates using the newer and lighter
		OCSP protocol as opposed to CRL, due to lower resource usage and greater reliability. However,
		it may be necessary to enable online CRL revocation checking in order to verify some timestamps
		(i.e. those that do not have an OCSP responder URL for all non-trusted certificates). */opts.enableOnlineCRLRevocationChecking(true)
            val widgetAnnot: SignatureWidget = SignatureWidget.create(doc, com.pdftron.pdf.Rect(0.0, 100.0, 200.0, 150.0), doctimestamp_signature_field)
            doc.getPage(1).annotPushBack(widgetAnnot)

            // (OPTIONAL) Add an appearance to the signature field.
            val img: com.pdftron.pdf.Image = com.pdftron.pdf.Image.create(doc, in_appearance_img_path)
            widgetAnnot.createSignatureAppearance(img)
            println("Testing timestamping configuration.")
            val config_result: TimestampingResult = tst_config.testConfiguration(opts)
            if (config_result.getStatus()) {
                println("Success: timestamping configuration usable. Attempting to timestamp.")
            } else {
                // Print details of timestamping failure.
                println(config_result.getString())
                if (config_result.hasResponseVerificationResult()) {
                    val tst_result: EmbeddedTimestampVerificationResult = config_result.getResponseVerificationResult()
                    println(String.format("CMS digest status: %s", tst_result.getCMSDigestStatusAsString()))
                    println(String.format("Message digest status: %s", tst_result.getMessageImprintDigestStatusAsString()))
                    println(String.format("Trust status: %s", tst_result.getTrustStatusAsString()))
                }
                return false
            }
            doctimestamp_signature_field.timestampOnNextSave(tst_config, opts)

            // Save/signing throws if timestamping fails.
            doc.save(in_outpath, SDFDoc.SaveMode.INCREMENTAL, null)
            println("Timestamping successful. Adding LTV information for DocTimeStamp signature.")

            // Add LTV information for timestamp signature to document.
            val timestamp_verification_result: VerificationResult = doctimestamp_signature_field.verify(opts)
            if (!doctimestamp_signature_field.enableLTVOfflineVerification(timestamp_verification_result)) {
                println("Could not enable LTV for DocTimeStamp.")
                return false
            }
            doc.save(in_outpath, SDFDoc.SaveMode.INCREMENTAL, null)
            println("Added LTV information for DocTimeStamp signature successfully.")
            return true
        }
    }

    init {
        setTitle(R.string.sample_digitalsignatures_title)
        setDescription(R.string.sample_digitalsignatures_description)

        // After proper setup (eg. Spongy Castle libs installed,
        // MySignatureHandler.createSignature() returns a valid buffer), please comment line below
        // to enable sample.
        // If you are using the full library, you do not need to use the custom signature handler.
        // PDFDoc has a standard signature handler that can be used instead. Check the code below
        // for more info.
        //DisableRun();
    }
}