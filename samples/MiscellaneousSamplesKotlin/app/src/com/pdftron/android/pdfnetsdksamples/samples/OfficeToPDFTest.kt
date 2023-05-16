//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------
package com.pdftron.android.pdfnetsdksamples.samples

import android.content.Context
import com.pdftron.android.pdfnetsdksamples.OutputListener
import com.pdftron.android.pdfnetsdksamples.PDFNetSample
import com.pdftron.android.pdfnetsdksamples.R
import com.pdftron.android.pdfnetsdksamples.util.Utils
import com.pdftron.common.PDFNetException
import com.pdftron.pdf.*
import com.pdftron.sdf.SDFDoc
import java.util.*

/**
 * The following sample illustrates how to use the PDF.Convert utility class to convert
 * .docx files to PDF
 *
 *
 * This conversion is performed entirely within the PDFNet and has *no* external or
 * system dependencies dependencies -- Conversion results will be the sam whether
 * on Windows, Linux or Android.
 *
 *
 * Please contact us if you have any questions.
 */
class OfficeToPDFTest(context: Context?) : PDFNetSample() {

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        Companion.mFileList.clear()
        printHeader(outputListener!!)

        // first the one-line conversion interface
        simpleDocxConvert("Fishermen.docx", "Fishermen.pdf")

        // then the more flexible line-by-line interface
        flexibleDocxConvert("the_rime_of_the_ancient_mariner.docx", "the_rime_of_the_ancient_mariner.pdf")

        // conversion of RTL content
        flexibleDocxConvert("factsheet_Arabic.docx", "factsheet_Arabic.pdf")
        for (file in Companion.mFileList) {
            addToFileList(file)
        }
        printFooter(outputListener!!)
    }

    companion object {
        private var mOutputListener: OutputListener? = null
        private val mFileList = ArrayList<String>()
        private var sLayoutSmartPluginPath: String? = null
        fun simpleDocxConvert(inputFilename: String, outputFilename: String?) {
            try {

                // perform the conversion with no optional parameters
                val pdfdoc = PDFDoc()
                Convert.officeToPdf(pdfdoc, Utils.getAssetTempFile(INPUT_PATH + inputFilename)!!.absolutePath, null)

                // save the result
                pdfdoc.save(Utils.createExternalFile(outputFilename!!, mFileList).absolutePath, SDFDoc.SaveMode.INCREMENTAL, null)

                // And we're done!
                mOutputListener!!.println("Done conversion " + Utils.createExternalFile(outputFilename, mFileList).absolutePath)
            } catch (e: PDFNetException) {
                mOutputListener!!.println("Unable to convert MS Office document, error:")
                mOutputListener!!.printError(e.stackTrace)
                mOutputListener!!.printError(e.stackTrace)
            }
        }

        fun flexibleDocxConvert(inputFilename: String, outputFilename: String?) {
            try {
                val options = OfficeToPDFOptions()
                options.smartSubstitutionPluginPath = sLayoutSmartPluginPath

                // create a conversion object -- this sets things up but does not yet
                // perform any conversion logic.
                // in a multithreaded environment, this object can be used to monitor
                // the conversion progress and potentially cancel it as well
                val conversion = Convert.streamingPdfConversion(
                        Utils.getAssetTempFile(INPUT_PATH + inputFilename)!!.absolutePath, options)
                mOutputListener!!.println(inputFilename + ": " + Math.round(conversion.progress * 100.0)
                        + "% " + conversion.progressLabel)

                // actually perform the conversion
                while (conversion.conversionStatus == DocumentConversion.e_incomplete) {
                    conversion.convertNextPage()
                    mOutputListener!!.println(inputFilename + ": " + Math.round(conversion.progress * 100.0)
                            + "% " + conversion.progressLabel)
                }
                if (conversion.tryConvert() == DocumentConversion.e_success) {
                    val num_warnings = conversion.numWarnings

                    // print information about the conversion
                    for (i in 0 until num_warnings) {
                        mOutputListener!!.println("Warning: " + conversion.getWarningString(i))
                    }

                    // save the result
                    val doc = conversion.doc
                    doc.save(Utils.createExternalFile(outputFilename!!, mFileList).absolutePath, SDFDoc.SaveMode.INCREMENTAL, null)

                    // done
                    mOutputListener!!.println("Done conversion " + Utils.createExternalFile(outputFilename, mFileList).absolutePath)
                } else {
                    mOutputListener!!.println("Encountered an error during conversion: " + conversion.errorString)
                }
            } catch (e: PDFNetException) {
                mOutputListener!!.println("Unable to convert MS Office document, error:")
                mOutputListener!!.printError(e.stackTrace)
                mOutputListener!!.printError(e.stackTrace)
            }
        }
    }

    init {
        try {
            val layoutPluginPath = Utils.copyResourceToTempFolder(context, R.raw.pdftron_layout_resources, false, "pdftron_layout_resources.plugin")
            PDFNet.addResourceSearchPath(layoutPluginPath)
            sLayoutSmartPluginPath = Utils.copyResourceToTempFolder(context, R.raw.pdftron_smart_substitution, false, "pdftron_smart_substitution.plugin")
            PDFNet.addResourceSearchPath(sLayoutSmartPluginPath)
        } catch (e: Exception) {
            mOutputListener!!.printError(e.stackTrace)
        }
        setTitle(R.string.sample_officetopdf_title)
        setDescription(R.string.sample_officetopdf_description)
    }
}