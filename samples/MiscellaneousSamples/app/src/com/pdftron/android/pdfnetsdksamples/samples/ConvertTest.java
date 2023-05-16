//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples;

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;
import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Convert;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.sdf.Obj;
import com.pdftron.sdf.ObjSet;
import com.pdftron.sdf.SDFDoc;

import java.util.ArrayList;

public class ConvertTest extends PDFNetSample {

    private static OutputListener mOutputListener;

    private static ArrayList<String> mFileList = new ArrayList<>();

    public ConvertTest() {
        setTitle(R.string.sample_convert_title);
        setDescription(R.string.sample_convert_description);
    }

    @Override
    public void run(OutputListener outputListener) {
        super.run(outputListener);
        mOutputListener = outputListener;
        mFileList.clear();
        printHeader(outputListener);

        String outputFile;

        mOutputListener.println("-------------------------------------------------");

        try {
            mOutputListener.println("Converting Text to PDF with options");
            ObjSet objset = new ObjSet();
            Obj options = objset.createDict();
            options.putNumber("FontSize", 15);
            options.putBool("UseSourceCodeFormatting", true);
            options.putNumber("PageWidth", 12);
            options.putNumber("PageHeight", 6);
            PDFDoc doc = new PDFDoc();
            outputFile = Utils.createExternalFile("simple-text.pdf", mFileList).getAbsolutePath();
            Convert.fromText(doc, Utils.getAssetTempFile(INPUT_PATH + "simple-text.txt").getAbsolutePath(), options);
            doc.save(outputFile, SDFDoc.SaveMode.LINEARIZED, null);
            mOutputListener.println("Result saved in " + outputFile);
            doc.close();
        } catch (PDFNetException e) {
            mOutputListener.println("Unable to convert Plain Text document to PDF, error:");
            mOutputListener.printError(e.getStackTrace());
        }

        // Convert the XPS document to PDF
        try {
            mOutputListener.println("Converting XPS document to PDF");
            PDFDoc doc = new PDFDoc();
            Convert.fromXps(doc, Utils.getAssetTempFile(INPUT_PATH + "simple-xps.xps").getAbsolutePath());
            outputFile = Utils.createExternalFile("xps2pdf_Java.pdf", mFileList).getAbsolutePath();
            doc.save(outputFile, SDFDoc.SaveMode.LINEARIZED, null);
            mOutputListener.println("Result saved in " + outputFile);
            doc.close();
        } catch (PDFNetException e) {
            mOutputListener.println("Unable to convert XPS document to PDF, error:");
            mOutputListener.printError(e.getStackTrace());
        }

        // Convert the PDF document to SVG
        try {
            mOutputListener.println("Converting XPS document to SVG");
            PDFDoc doc = new PDFDoc(Utils.getAssetTempFile(INPUT_PATH + "tiger.pdf").getAbsolutePath());
            Convert.fromXps(doc, Utils.getAssetTempFile(INPUT_PATH + "simple-xps.xps").getAbsolutePath());
            mOutputListener.println("Converting PDF document to SVG");
            outputFile = Utils.createExternalFile("pdf2svg_Java.svg", mFileList).getAbsolutePath();
            Convert.toSvg(doc, outputFile);
            mOutputListener.println("Result saved in " + outputFile);
            doc.close();
        } catch (PDFNetException e) {
            mOutputListener.println("Unable to convert PDF document to SVG, error:");
            mOutputListener.printError(e.getStackTrace());
        }

        // Convert the PDF document to XPS
        try {
            mOutputListener.println("Converting PDF document to XPS");
            PDFDoc doc = new PDFDoc(Utils.getAssetTempFile(INPUT_PATH + "newsletter.pdf").getAbsolutePath());
            outputFile = Utils.createExternalFile("pdf2xps_Java.xps", mFileList).getAbsolutePath();
            Convert.toXps(doc, outputFile);
            mOutputListener.println("Result saved in " + outputFile);
            doc.close();
        } catch (PDFNetException e) {
            mOutputListener.println("Unable to convert PDF document to SVG, error:");
            mOutputListener.printError(e.getStackTrace());
        }

        // Convert an image to PDF using internal converter
        try {
            mOutputListener.println("Converting PNG image to PDF");
            PDFDoc doc = new PDFDoc();
            Convert.toPdf(doc, Utils.getAssetTempFile(INPUT_PATH + "butterfly.png").getAbsolutePath());
            outputFile = Utils.createExternalFile("png2pdf_Java.pdf", mFileList).getAbsolutePath();
            doc.save(outputFile, SDFDoc.SaveMode.LINEARIZED, null);
            mOutputListener.println("Result saved in " + outputFile);
            doc.close();
        } catch (PDFNetException e) {
            mOutputListener.println("Unable to convert PNG image to XPS, error:");
            mOutputListener.printError(e.getStackTrace());
        }

        // Convert an image to XPS using internal converter
        try {
            mOutputListener.println("Converting PNG image to XPS");
            outputFile = Utils.createExternalFile("buttefly_Java.xps", mFileList).getAbsolutePath();
            Convert.toXps(Utils.getAssetTempFile(INPUT_PATH + "butterfly.png").getAbsolutePath(), outputFile);
            mOutputListener.println("Result saved in " + outputFile);
        } catch (PDFNetException e) {
            mOutputListener.println("Unable to convert PNG image to XPS, error:");
            mOutputListener.printError(e.getStackTrace());
        }

        // Convert a PDF document directly to XPS
        try {
            mOutputListener.println("Converting PDF to XPS");
            outputFile = Utils.createExternalFile("newsletter.xps", mFileList).getAbsolutePath();
            Convert.toXps(Utils.getAssetTempFile(INPUT_PATH + "newsletter.pdf").getAbsolutePath(), outputFile);
            mOutputListener.println("Result saved in " + outputFile);
        } catch (PDFNetException e) {
            mOutputListener.println("Unable to convert PDF document to XPS, error:");
            mOutputListener.printError(e.getStackTrace());
        }

        // Convert a PDF document to HTML
        try {
            mOutputListener.println("Converting PDF to HTML");
            outputFile = Utils.createExternalFile("newsletter", mFileList).getAbsolutePath();
            Convert.toHtml(Utils.getAssetTempFile(INPUT_PATH + "newsletter.pdf").getAbsolutePath(), outputFile);
            mOutputListener.println("Result saved in " + outputFile);
        } catch (PDFNetException e) {
            mOutputListener.println("Unable to convert PDF document to HTML, error:");
            mOutputListener.printError(e.getStackTrace());
        }

        // Convert a PDF document to EPUB
        try {
            mOutputListener.println("Converting PDF to EPUB");
            outputFile = Utils.createExternalFile("newsletter.epub", mFileList).getAbsolutePath();
            Convert.toEpub(Utils.getAssetTempFile(INPUT_PATH + "newsletter.pdf").getAbsolutePath(), outputFile);
            mOutputListener.println("Result saved in " + outputFile);
        } catch (PDFNetException e) {
            mOutputListener.println("Unable to convert PDF document to EPUB, error:");
            mOutputListener.printError(e.getStackTrace());
        }

        // Convert a PDF document to multipage TIFF
        try {
            mOutputListener.println("Converting PDF to multipage TIFF");
            outputFile = Utils.createExternalFile("newsletter.tiff", mFileList).getAbsolutePath();
            Convert.TiffOutputOptions tiff_options = new Convert.TiffOutputOptions();
            tiff_options.setDPI(200);
            tiff_options.setMono(true);
            tiff_options.setDither(true);
            Convert.toTiff(Utils.getAssetTempFile(INPUT_PATH + "newsletter.pdf").getAbsolutePath(), outputFile, tiff_options);
            mOutputListener.println("Result saved in " + outputFile);
        } catch (PDFNetException e) {
            mOutputListener.println("Unable to convert PDF document to TIFF, error:");
            mOutputListener.printError(e.getStackTrace());
        }

        mOutputListener.println("Done.");

        for (String file : mFileList) {
            addToFileList(file);
        }
        printFooter(outputListener);
    }
}