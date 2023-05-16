package com.pdftron.showcase.activities

import android.net.Uri
import android.os.Bundle
import androidx.core.net.toFile
import com.pdftron.pdf.Convert
import com.pdftron.pdf.OfficeToPDFOptions
import com.pdftron.pdf.PDFDoc
import com.pdftron.sdf.SDFDoc
import com.pdftron.showcase.R
import com.pdftron.showcase.helpers.AppUtils
import kotlinx.android.synthetic.main.content_bottom_sheet.*
import kotlinx.android.synthetic.main.control_document_generation.*
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DocumentGenerationActivity : FeatureActivity() {

    private lateinit var mFileUrl: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mFileUrl = AppUtils.getUriFromRawName(applicationContext, "syh_letter_red.docx")
        setUpSampleView(mFileUrl.toString(), true)

        val bottomSheetContainer = feature_content_container
        layoutInflater.inflate(R.layout.control_document_generation, bottomSheetContainer, true)
        addControl()
    }

    private fun addControl() {
        submit_button.setOnClickListener {
            buildDocument()
        }
    }

    private fun buildDocument() {
        try {
            val dictJson = JSONObject()

            var key = given_name.tag as String
            var value = given_name.editText?.text.toString()
            dictJson.put(key, value)

            key = surname.tag as String
            value = surname.editText?.text.toString()
            dictJson.put(key, value)

            key = street_address.tag as String
            value = street_address.editText?.text.toString()
            dictJson.put(key, value)

            key = dest_title.tag as String
            value = dest_title.editText?.text.toString()
            dictJson.put(key, value)

            key = land_location.tag as String
            value = land_location.editText?.text.toString()
            dictJson.put(key, value)

            key = lease_problem.tag as String
            value = lease_problem.editText?.text.toString()
            dictJson.put(key, value)

            key = sender_name.tag as String
            value = sender_name.editText?.text.toString()
            dictJson.put(key, value)

            // date
            val c: Date = Calendar.getInstance().time
            val df = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val formattedDate: String = df.format(c)
            dictJson.put("Date", formattedDate)

            // logo
            val logoFile = AppUtils.getUriFromRawName(applicationContext, "logo_red.png")
            val logoJson = JSONObject()
            logoJson.put("image_url", logoFile.toFile().absolutePath)
            logoJson.put("width", 64)
            logoJson.put("height", 64)
            dictJson.put("logo", logoJson)

            val doc = PDFDoc()
            val options = OfficeToPDFOptions()
            options.templateParamsJson = dictJson.toString()
            Convert.officeToPdf(doc, mFileUrl.toFile().absolutePath, options)

            val resultFile = File.createTempFile("tmp", ".pdf", this.filesDir)
            doc.save(resultFile.absolutePath, SDFDoc.SaveMode.LINEARIZED, null)

            setUpSampleView(resultFile.toString(), true)

            closeBottomSheet()

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

}