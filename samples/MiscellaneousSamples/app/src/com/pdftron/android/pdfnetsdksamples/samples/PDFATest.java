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
import com.pdftron.pdf.PDFNet;
import com.pdftron.pdf.pdfa.PDFACompliance;

import java.util.ArrayList;

public class PDFATest extends PDFNetSample {

	private static OutputListener mOutputListener;

	private static ArrayList<String> mFileList = new ArrayList<>();

    public PDFATest() {
        setTitle(R.string.sample_pdfa_title);
        setDescription(R.string.sample_pdfa_description);

        // The standard library does not include PDF/A validation/conversion,
        // thus this sample will fail. Please, comment out this call
        // if using the full libraries.
        // DisableRun();
    }

	@Override
	public void run(OutputListener outputListener) {
		super.run(outputListener);
		mOutputListener = outputListener;
		mFileList.clear();
		printHeader(outputListener);
        try{ 
            PDFNet.setColorManagement(PDFNet.e_lcms); // Required for proper PDF/A validation and conversion.
        
            //-----------------------------------------------------------
            // Example 1: PDF/A Validation
            //-----------------------------------------------------------
        
            String filename = "newsletter.pdf";
            /* The max_ref_objs parameter to the PDFACompliance constructor controls the maximum number 
            of object numbers that are collected for particular error codes. The default value is 10 
            in order to prevent spam. If you need all the object numbers, pass 0 for max_ref_objs. */
            PDFACompliance pdf_a = new PDFACompliance(false, Utils.getAssetTempFile(INPUT_PATH + filename).getAbsolutePath(), null, PDFACompliance.e_Level2B, null, 10);
            printResults(pdf_a, filename);
            pdf_a.destroy();
        } catch (PDFNetException e) {
            System.out.println(e.getMessage());
        }
        
        
        
            //-----------------------------------------------------------
            // Example 2: PDF/A Conversion
            //-----------------------------------------------------------
        try {
            String filename = "fish.pdf";
            PDFACompliance pdf_a = new PDFACompliance(true, Utils.getAssetTempFile(INPUT_PATH + filename).getAbsolutePath(), null, PDFACompliance.e_Level2B, null, 10);
            filename = "pdfa.pdf";
            pdf_a.saveAs(Utils.createExternalFile(filename, mFileList).getAbsolutePath(), false);
            pdf_a.destroy();
            // output "pdf_a.pdf"

            // Re-validate the document after the conversion...
            pdf_a = new PDFACompliance(false, Utils.createExternalFile(filename, mFileList).getAbsolutePath(), null, PDFACompliance.e_Level2B, null, 10);
            printResults(pdf_a, filename);
            pdf_a.destroy();

        } catch (PDFNetException e) {
            System.out.println(e.getMessage());
        }

        mOutputListener.println("PDFACompliance test completed.");

		for (String file : mFileList) {
			addToFileList(file);
		}
		printFooter(outputListener);
	}


    static void printResults(PDFACompliance pdf_a, String filename) {
        try {
            int err_cnt = pdf_a.getErrorCount();
            mOutputListener.print(filename);
            if (err_cnt == 0) {
                mOutputListener.print(": OK.\n");
            } else {
                mOutputListener.println(" is NOT a valid PDFA.");
                for (int i = 0; i < err_cnt; ++i) {
                    int c = pdf_a.getError(i);
                    mOutputListener.println(" - e_PDFA " + c + ": " + PDFACompliance.getPDFAErrorMessage(c) + ".");
                    if (true) {
                        int num_refs = pdf_a.getRefObjCount(c);
                        if (num_refs > 0) {
                            mOutputListener.print("   Objects: ");
                            for (int j = 0; j < num_refs; ) {
                                mOutputListener.print(String.valueOf(pdf_a.getRefObj(c, j)));
                                if (++j != num_refs) mOutputListener.print(", ");
                            }
                            mOutputListener.println();
                        }
                    }
                }
                mOutputListener.println();
            }
        } catch (PDFNetException e) {
            System.out.println(e.getMessage());
        }
    }

}