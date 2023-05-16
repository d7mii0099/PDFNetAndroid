package com.pdftron.showcase.activities

import android.net.Uri
import android.os.Bundle
import com.pdftron.actions.easypdf.EasyPdfService
import com.pdftron.pdf.controls.PdfViewCtrlTabFragment2
import com.pdftron.pdf.model.BaseFileInfo
import com.pdftron.pdf.utils.CommonToast
import com.pdftron.pdf.utils.Utils
import com.pdftron.showcase.R
import kotlinx.android.synthetic.main.content_bottom_sheet.*
import kotlinx.android.synthetic.main.content_bottomsheet_button.*
import kotlinx.coroutines.*
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.util.*

abstract class PDFToOfficeActivity : FeatureActivity() {

    // Client ID. You can get your client ID from the developer page
    // (for registered user, you must sign in before visiting this page)
    // https://www.easypdfcloud.com/developer
    //
    // To test the sample code quickly for demo purpose, you can use
    // the following demo client ID:
    //
    //   05ee808265f24c66b2b8e31d90c31ab1
    //
    private val clientId = "05ee808265f24c66b2b8e31d90c31ab1"

    // Client secret. You can get your client secret from the developer page
    // (for registered user, you must sign in before visiting this page)
    // https://www.easypdfcloud.com/developer
    //
    // To test the sample code quickly for demo purpose, you can use
    // the following demo client secret:
    //
    //   16ABE87E052059F147BB2A491944BF7EA7876D0F843DED105CBA094C887CBC99
    //
    private val clientSecret = "16ABE87E052059F147BB2A491944BF7EA7876D0F843DED105CBA094C887CBC99"

    protected val taskTypeWord = "EPWF_CONVERTER_CONVERT_TO_WORD"
    protected val taskTypePPT = "EPWF_CONVERTER_CONVERT_TO_POWERPOINT"
    protected val taskTypeExcel = "EPWF_CONVERTER_CONVERT_TO_EXCEL"
    private val taskRevision = 2

    private var originalPdf: File? = null

    abstract fun getTaskType(): String
    abstract fun getExtension(): String
    abstract fun getButtonText(): String

    override fun onCreate(savedInstanceState: Bundle?) {
        showTab = true

        super.onCreate(savedInstanceState)

        val bottomSheetContainer = feature_content_container
        layoutInflater.inflate(R.layout.control_button_simple, bottomSheetContainer, true)
        addControl()
    }

    override fun handleTabDocumentLoaded(tag: String) {
        if (originalPdf == null) {
            originalPdf = File(tag)
        }
    }

    private fun addControl() {
        button.text = getButtonText()
        button.setOnClickListener {
            // save document first
            mPdfViewCtrlTabHostFragment?.currentPdfViewCtrlFragment?.save(false, true, true)

            // export to format
            exportToFormat(
                originalPdf
            )
        }
    }

    private fun exportToFormat(inputFile: File?) {

        if (mPdfViewCtrlTabHostFragment == null || inputFile == null) {
            return
        }

        CommonToast.showText(this, R.string.export_to_word_message)

        CoroutineScope(Job() + Dispatchers.IO).launch {
            try {

                EasyPdfService.initialize(clientId, clientSecret)
                val outputPath = EasyPdfService.startDynamicJob(
                    getTaskType(),
                    taskRevision,
                    inputFile,
                    cacheDir,
                    UUID.randomUUID().toString() + "." + getExtension()
                )

                withContext(Dispatchers.Main) {
                    outputPath?.let {
                        val resultFile = resources.getString(
                            R.string.export_to_word_result_file_path,
                            outputPath
                        )
                        CommonToast.showText(this@PDFToOfficeActivity, resultFile)

                        var name = FilenameUtils.getName(it)
                        name = FilenameUtils.removeExtension(name)
                        val tabFile = PdfViewCtrlTabFragment2.createBasicPdfViewCtrlTabBundle(
                            this@PDFToOfficeActivity,
                            Uri.fromFile(File(it)),
                            null
                        )
                        mPdfViewCtrlTabHostFragment!!.addTab(
                            tabFile,
                            it,
                            Utils.capitalize(name),
                            getExtension(),
                            null,
                            BaseFileInfo.FILE_TYPE_FILE
                        )
                        mPdfViewCtrlTabHostFragment!!.setCurrentTabByTag(it)
                    }
                }
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    CommonToast.showText(
                        this@PDFToOfficeActivity,
                        R.string.export_to_office_failed_message
                    )
                }
            }
        }
    }
}