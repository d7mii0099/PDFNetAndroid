package com.pdftron.showcase.activities

import android.os.Bundle
import com.pdftron.pdf.dialog.digitalsignature.validation.DigitalSignatureUtils
import com.pdftron.pdf.utils.Utils
import com.pdftron.showcase.R
import kotlinx.android.synthetic.main.content_bottom_sheet.*
import kotlinx.android.synthetic.main.content_bottomsheet_button.*

class DigitalSignatureValidationActivity : FeatureActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        sampleFileName = "tiger_with_approval_field_certified_approved"
        super.onCreate(savedInstanceState)

        val bottomSheetContainer = feature_content_container
        layoutInflater.inflate(R.layout.control_button_simple, bottomSheetContainer, true)
        addControl()
    }

    private fun addControl() {
        val cert = Utils.copyResourceToLocal(baseContext, R.raw.pdftron_cert, "pdftron_cert", ".cer")
        DigitalSignatureUtils.addCertificate(baseContext, cert)

        button.text = getString(R.string.verify_digital_signatures)
        button.setOnClickListener {
            mPdfViewCtrlTabHostFragment!!.showDigitalSignatureList()
        }
    }
}