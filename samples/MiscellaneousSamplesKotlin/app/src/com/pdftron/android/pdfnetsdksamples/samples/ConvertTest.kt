package com.pdftron.android.pdfnetsdksamples.samples

import com.pdftron.android.pdfnetsdksamples.OutputListener
import com.pdftron.android.pdfnetsdksamples.PDFNetSample
import com.pdftron.android.pdfnetsdksamples.R
import com.pdftron.android.pdfnetsdksamples.util.Utils
import com.pdftron.common.PDFNetException
import com.pdftron.pdf.Convert
import com.pdftron.pdf.PDFDoc
import com.pdftron.sdf.ObjSet
import com.pdftron.sdf.SDFDoc
import java.util.ArrayList

class ConvertTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_convert_title)
        setDescription(R.string.sample_convert_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)

        var outputFile: String

        mOutputListener!!.println("-------------------------------------------------")

        try {
            mOutputListener!!.println("Converting Text to PDF with options")
            val objset = ObjSet()
            val options = objset.createDict()
            options.putNumber("FontSize", 15.0)
            options.putBool("UseSourceCodeFormatting", true)
            options.putNumber("PageWidth", 12.0)
            options.putNumber("PageHeight", 6.0)
            val doc = PDFDoc()
            outputFile = Utils.createExternalFile("simple-text.pdf", mFileList).absolutePath
            Convert.fromText(doc, Utils.getAssetTempFile(PDFNetSample.INPUT_PATH + "simple-text.txt")!!.absolutePath, options)
            doc.save(outputFile, SDFDoc.SaveMode.LINEARIZED, null)
            mOutputListener!!.println("Result saved in $outputFile")
            doc.close()
        } catch (e: PDFNetException) {
            mOutputListener!!.println("Unable to convert Plain Text document to PDF, error:")
            mOutputListener!!.printError(e.stackTrace)
        }

        // Convert the XPS document to PDF
        try {
            mOutputListener!!.println("Converting XPS document to PDF")
            val doc = PDFDoc()
            Convert.fromXps(doc, Utils.getAssetTempFile(PDFNetSample.INPUT_PATH + "simple-xps.xps")!!.absolutePath)
            outputFile = Utils.createExternalFile("xps2pdf_Java.pdf", mFileList).absolutePath
            doc.save(outputFile, SDFDoc.SaveMode.LINEARIZED, null)
            mOutputListener!!.println("Result saved in $outputFile")
            doc.close()
        } catch (e: PDFNetException) {
            mOutputListener!!.println("Unable to convert XPS document to PDF, error:")
            mOutputListener!!.printError(e.stackTrace)
        }

        // Convert the PDF document to SVG
        try {
            mOutputListener!!.println("Converting XPS document to SVG")
            val doc = PDFDoc(Utils.getAssetTempFile(INPUT_PATH + "tiger.pdf")!!.absolutePath)
            Convert.fromXps(doc, Utils.getAssetTempFile(PDFNetSample.INPUT_PATH + "simple-xps.xps")!!.absolutePath)
            mOutputListener!!.println("Converting PDF document to SVG")
            outputFile = Utils.createExternalFile("pdf2svg_Java.svg", mFileList).absolutePath
            Convert.toSvg(doc, outputFile)
            mOutputListener!!.println("Result saved in $outputFile")
            doc.close()
        } catch (e: PDFNetException) {
            mOutputListener!!.println("Unable to convert PDF document to SVG, error:")
            mOutputListener!!.printError(e.stackTrace)
        }

        // Convert the PDF document to XPS
        try {
            mOutputListener!!.println("Converting PDF document to XPS")
            val doc = PDFDoc(Utils.getAssetTempFile(INPUT_PATH + "newsletter.pdf")!!.absolutePath)
            outputFile = Utils.createExternalFile("pdf2xps_Java.xps", mFileList).absolutePath
            Convert.toXps(doc, outputFile)
            mOutputListener!!.println("Result saved in $outputFile")
            doc.close()
        } catch (e: PDFNetException) {
            mOutputListener!!.println("Unable to convert PDF document to SVG, error:")
            mOutputListener!!.printError(e.stackTrace)
        }

        // Convert an image to PDF using internal converter
        try {
            mOutputListener!!.println("Converting PNG image to PDF")
            val doc = PDFDoc()
            Convert.toPdf(doc, Utils.getAssetTempFile(PDFNetSample.INPUT_PATH + "butterfly.png")!!.absolutePath)
            outputFile = Utils.createExternalFile("png2pdf_Java.pdf", mFileList).absolutePath
            doc.save(outputFile, SDFDoc.SaveMode.LINEARIZED, null)
            mOutputListener!!.println("Result saved in $outputFile")
            doc.close()
        } catch (e: PDFNetException) {
            mOutputListener!!.println("Unable to convert PNG image to XPS, error:")
            mOutputListener!!.printError(e.stackTrace)
        }

        // Convert an image to XPS using internal converter
        try {
            mOutputListener!!.println("Converting PNG image to XPS")
            outputFile = Utils.createExternalFile("buttefly_Java.xps", mFileList).absolutePath
            Convert.toXps(Utils.getAssetTempFile(PDFNetSample.INPUT_PATH + "butterfly.png")!!.absolutePath, outputFile)
            mOutputListener!!.println("Result saved in $outputFile")
        } catch (e: PDFNetException) {
            mOutputListener!!.println("Unable to convert PNG image to XPS, error:")
            mOutputListener!!.printError(e.stackTrace)
        }

        // Convert a PDF document directly to XPS
        try {
            mOutputListener!!.println("Converting PDF to XPS")
            outputFile = Utils.createExternalFile("newsletter.xps", mFileList).absolutePath
            Convert.toXps(Utils.getAssetTempFile(PDFNetSample.INPUT_PATH + "newsletter.pdf")!!.absolutePath, outputFile)
            mOutputListener!!.println("Result saved in $outputFile")
        } catch (e: PDFNetException) {
            mOutputListener!!.println("Unable to convert PDF document to XPS, error:")
            mOutputListener!!.printError(e.stackTrace)
        }

        // Convert a PDF document to HTML
        try {
            mOutputListener!!.println("Converting PDF to HTML")
            outputFile = Utils.createExternalFile("newsletter", mFileList).absolutePath
            Convert.toHtml(Utils.getAssetTempFile(PDFNetSample.INPUT_PATH + "newsletter.pdf")!!.absolutePath, outputFile)
            mOutputListener!!.println("Result saved in $outputFile")
        } catch (e: PDFNetException) {
            mOutputListener!!.println("Unable to convert PDF document to HTML, error:")
            mOutputListener!!.printError(e.stackTrace)
        }

        // Convert a PDF document to EPUB
        try {
            mOutputListener!!.println("Converting PDF to EPUB")
            outputFile = Utils.createExternalFile("newsletter.epub", mFileList).absolutePath
            Convert.toEpub(Utils.getAssetTempFile(PDFNetSample.INPUT_PATH + "newsletter.pdf")!!.absolutePath, outputFile)
            mOutputListener!!.println("Result saved in $outputFile")
        } catch (e: PDFNetException) {
            mOutputListener!!.println("Unable to convert PDF document to EPUB, error:")
            mOutputListener!!.printError(e.stackTrace)
        }

        // Convert a PDF document to multipage TIFF
        try {
            mOutputListener!!.println("Converting PDF to multipage TIFF")
            outputFile = Utils.createExternalFile("newsletter.tiff", mFileList).absolutePath
            val tiff_options = Convert.TiffOutputOptions()
            tiff_options.setDPI(200.0)
            tiff_options.setMono(true)
            tiff_options.setDither(true)
            Convert.toTiff(Utils.getAssetTempFile(PDFNetSample.INPUT_PATH + "newsletter.pdf")!!.absolutePath, outputFile, tiff_options)
            mOutputListener!!.println("Result saved in $outputFile")
        } catch (e: PDFNetException) {
            mOutputListener!!.println("Unable to convert PDF document to TIFF, error:")
            mOutputListener!!.printError(e.stackTrace)
        }

        mOutputListener!!.println("Done.")

        for (file in mFileList) {
            addToFileList(file)
        }
        printFooter(outputListener)
    }

    companion object {

        private var mOutputListener: OutputListener? = null

        private val mFileList = ArrayList<String>()
    }
}