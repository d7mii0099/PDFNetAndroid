//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples;

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.Redactor;
import com.pdftron.sdf.SDFDoc;

import java.util.ArrayList;

/**
 * PDF Redactor is a separately licensable Add-on that offers options to remove
 * (not just covering or obscuring) content within a region of PDF.
 * With printed pages, redaction involves blacking-out or cutting-out areas of
 * the printed page. With electronic documents that use formats such as PDF,
 * redaction typically involves removing sensitive content within documents for
 * safe distribution to courts, patent and government institutions, the media,
 * customers, vendors or any other audience with restricted access to the content.
 * <p>
 * The redaction process in PDFNet consists of two steps:
 * <p>
 * a) Content identification: A user applies redact annotations that specify the
 * pieces or regions of content that should be removed. The content for redaction
 * can be identified either interactively (e.g. using 'com.pdftron.pdf.PDFViewCtrl'
 * as shown in PDFView sample) or programmatically (e.g. using 'com.pdftron.pdf.TextSearch'
 * or 'com.pdftron.pdf.TextExtractor'). Up until the next step is performed, the user
 * can see, move and redefine these annotations.
 * b) Content removal: Using 'com.pdftron.pdf.Redactor.Redact()' the user instructs
 * PDFNet to apply the redact regions, after which the content in the area specified
 * by the redact annotations is removed. The redaction function includes number of
 * options to control the style of the redaction overlay (including color, text,
 * font, border, transparency, etc.).
 * <p>
 * PDFTron Redactor makes sure that if a portion of an image, text, or vector graphics
 * is contained in a redaction region, that portion of the image or path data is
 * destroyed and is not simply hidden with clipping or image masks. PDFNet API can also
 * be used to review and remove metadata and other content that can exist in a PDF
 * document, including XML Forms Architecture (XFA) content and Extensible Metadata
 * Platform (XMP) content.
 */

public class PDFRedactTest extends PDFNetSample {

	private static OutputListener mOutputListener;

	private static ArrayList<String> mFileList = new ArrayList<>();

    public PDFRedactTest() {
        setTitle(R.string.sample_pdfredact_title);
        setDescription(R.string.sample_pdfredact_description);

        // The standard library does not include the Redaction feature.
        // If using the full library, please comment out the following
        // call.
        // DisableRun();
    }

	@Override
	public void run(OutputListener outputListener) {
		super.run(outputListener);
		mOutputListener = outputListener;
		mFileList.clear();
		printHeader(outputListener);
        // Relative paths to folders containing test files.

        try {
            Redactor.Redaction[] vec = new Redactor.Redaction[7];
            vec[0] = new Redactor.Redaction(1, new Rect(100, 100, 550, 600), false, "Top Secret");
            vec[1] = new Redactor.Redaction(2, new Rect(30, 30, 450, 450), true, "Negative Redaction");
            vec[2] = new Redactor.Redaction(2, new Rect(0, 0, 100, 100), false, "Positive");
            vec[3] = new Redactor.Redaction(2, new Rect(100, 100, 200, 200), false, "Positive");
            vec[4] = new Redactor.Redaction(2, new Rect(300, 300, 400, 400), false, "");
            vec[5] = new Redactor.Redaction(2, new Rect(500, 500, 600, 600), false, "");
            vec[6] = new Redactor.Redaction(3, new Rect(0, 0, 700, 20), false, "");
            
            Redactor.Appearance app = new Redactor.Appearance();
            app.redactionOverlay = true;
            app.border = false;
            app.showRedactedContentRegions = true;

            redact(Utils.getAssetTempFile(INPUT_PATH + "newsletter.pdf").getAbsolutePath(), Utils.createExternalFile("redacted.pdf", mFileList).getAbsolutePath(), vec, app);

            mOutputListener.println("Done...");
        } catch (Exception e) {
            mOutputListener.printError(e.getStackTrace());
        }

		for (String file : mFileList) {
			addToFileList(file);
		}
		printFooter(outputListener);
	}

    public static void redact(String input, String output, Redactor.Redaction[] vec, Redactor.Appearance app) {
        try {
            PDFDoc doc = new PDFDoc(input);
            if (doc.initSecurityHandler()) {
                Redactor.redact(doc, vec, app, false, true);
                doc.save(output, SDFDoc.SaveMode.REMOVE_UNUSED, null);
                doc.close();
            }
        } catch (Exception e) {
            mOutputListener.printError(e.getStackTrace());
        }
    }

}