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
import com.pdftron.pdf.PDFNet
import com.pdftron.pdf.pdfa.PDFACompliance

import java.util.ArrayList

class PDFATest : PDFNetSample() {
    init {
        setTitle(R.string.sample_pdfa_title)
        setDescription(R.string.sample_pdfa_description)

        // The standard library does not include PDF/A validation/conversion,
        // thus this sample will fail. Please, comment out this call
        // if using the full libraries.
        // DisableRun();
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)
        try {
            PDFNet.setColorManagement(PDFNet.e_lcms) // Required for proper PDF/A validation and conversion.

            //-----------------------------------------------------------
            // Example 1: PDF/A Validation
            //-----------------------------------------------------------

            val filename = "newsletter.pdf"
            /* The max_ref_objs parameter to the PDFACompliance constructor controls the maximum number
            of object numbers that are collected for particular error codes. The default value is 10
            in order to prevent spam. If you need all the object numbers, pass 0 for max_ref_objs. */
            val pdf_a = PDFACompliance(false, Utils.getAssetTempFile(PDFNetSample.INPUT_PATH + filename)!!.absolutePath, null, PDFACompliance.e_Level2B, null, 10)
            printResults(pdf_a, filename)
            pdf_a.destroy()
        } catch (e: PDFNetException) {
            println(e.message)
        }

        //-----------------------------------------------------------
        // Example 2: PDF/A Conversion
        //-----------------------------------------------------------
        try {
            var filename = "fish.pdf"
            var pdf_a = PDFACompliance(true, Utils.getAssetTempFile(PDFNetSample.INPUT_PATH + filename)!!.absolutePath, null, PDFACompliance.e_Level2B, null, 10)
            filename = "pdfa.pdf"
            pdf_a.saveAs(Utils.createExternalFile(filename, mFileList).absolutePath, false)
            pdf_a.destroy()

            // Re-validate the document after the conversion...
            pdf_a = PDFACompliance(false, Utils.createExternalFile(filename, mFileList).absolutePath, null, PDFACompliance.e_Level2B, null, 10)
            printResults(pdf_a, filename)
            pdf_a.destroy()

        } catch (e: PDFNetException) {
            println(e.message)
        }

        mOutputListener!!.println("PDFACompliance test completed.")

        for (file in mFileList) {
            addToFileList(file)
        }
        printFooter(outputListener)
    }

    companion object {

        private var mOutputListener: OutputListener? = null

        private val mFileList = ArrayList<String>()

        internal fun printResults(pdf_a: PDFACompliance, filename: String) {
            try {
                val err_cnt = pdf_a.errorCount
                mOutputListener!!.print(filename)
                if (err_cnt == 0) {
                    mOutputListener!!.print(": OK.\n")
                } else {
                    mOutputListener!!.println(" is NOT a valid PDFA.")
                    for (i in 0 until err_cnt) {
                        val c = pdf_a.getError(i)
                        mOutputListener!!.println(" - e_PDFA " + c + ": " + PDFACompliance.getPDFAErrorMessage(c) + ".")
                        if (true) {
                            val num_refs = pdf_a.getRefObjCount(c)
                            if (num_refs > 0) {
                                mOutputListener!!.print("   Objects: ")
                                var j = 0
                                while (j < num_refs) {
                                    mOutputListener!!.print(pdf_a.getRefObj(c, j).toString())
                                    if (++j != num_refs) mOutputListener!!.print(", ")
                                }
                                mOutputListener!!.println()
                            }
                        }
                    }
                    mOutputListener!!.println()
                }
            } catch (e: PDFNetException) {
                println(e.message)
            }

        }
    }

}