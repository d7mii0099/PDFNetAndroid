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
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.ColorSpace;
import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementReader;
import com.pdftron.pdf.ElementWriter;
import com.pdftron.pdf.GState;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageIterator;
import com.pdftron.sdf.Obj;
import com.pdftron.sdf.SDFDoc;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

public class ElementEditTest extends PDFNetSample {

	private static OutputListener mOutputListener;

	private static ArrayList<String> mFileList = new ArrayList<>();

    public ElementEditTest() {
        setTitle(R.string.sample_elementedit_title);
        setDescription(R.string.sample_elementedit_description);
    }

	@Override
	public void run(OutputListener outputListener) {
		super.run(outputListener);
		mOutputListener = outputListener;
		mFileList.clear();
		printHeader(outputListener);

        String input_filename = "newsletter.pdf";
        String output_filename = "newsletter_edited.pdf";

        try {
            mOutputListener.println("Opening the input file...");
            PDFDoc doc = new PDFDoc((Utils.getAssetTempFile(INPUT_PATH + input_filename).getAbsolutePath()));
            doc.initSecurityHandler();

            ElementWriter writer = new ElementWriter();
            ElementReader reader = new ElementReader();
            Set<Integer> visited = new TreeSet<Integer>();

            PageIterator itr = doc.getPageIterator();
            while (itr.hasNext()) {
				try{
					Page page = itr.next();
					visited.add((int) page.getSDFObj().getObjNum());

					reader.begin(page);
					writer.begin(page, ElementWriter.e_replacement, false, true, page.getResourceDict());

					processElements(writer, reader, visited);
					writer.end();
					reader.end();
				} catch (Exception e) {
					mOutputListener.printError(e.getStackTrace());
				}
            }

            // Save modified document
            doc.save(Utils.createExternalFile(output_filename, mFileList).getAbsolutePath(), SDFDoc.SaveMode.REMOVE_UNUSED, null);
            doc.close();
            mOutputListener.println("Done. Result saved in " + output_filename + "...");
        } catch (Exception e) {
            mOutputListener.printError(e.getStackTrace());
        }

		for (String file : mFileList) {
			addToFileList(file);
		}
		printFooter(outputListener);
	}
    public static void processElements(ElementWriter writer, ElementReader reader, Set<Integer> visited)  throws PDFNetException {
        Element element;
		while ((element = reader.next()) != null) {
			switch (element.getType()) {
				case Element.e_image:
				case Element.e_inline_image:
					// remove all images by skipping them
					break;
				case Element.e_path: {
					// Set all paths to red color.
					GState gs = element.getGState();
					gs.setFillColorSpace(ColorSpace.createDeviceRGB());
					gs.setFillColor(new ColorPt(1, 0, 0));
					writer.writeElement(element);
				}
				break;
				case Element.e_text: {
					// Set all text to blue color.
					GState gs = element.getGState();
					gs.setFillColorSpace(ColorSpace.createDeviceRGB());
					gs.setFillColor(new ColorPt(0, 0, 1));
					writer.writeElement(element);
				}
				break;
				case Element.e_form: {
					writer.writeElement(element); // write Form XObject reference to current stream
					Obj form_obj = element.getXObject();
					if (!visited.contains((int) form_obj.getObjNum())) // if this XObject has not been processed
					{
						// recursively process the Form XObject
						visited.add((int) form_obj.getObjNum());
						ElementWriter new_writer = new ElementWriter();
						reader.formBegin();
						new_writer.begin(form_obj);

						reader.clearChangeList();
						new_writer.setDefaultGState(reader);  

						processElements(new_writer, reader, visited);
						new_writer.end();
						reader.end();
					}
				}
				break;
				default:
					writer.writeElement(element);
					break;
			}
		}
  
    }

}