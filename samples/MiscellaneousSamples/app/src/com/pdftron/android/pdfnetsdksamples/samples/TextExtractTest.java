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
import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementReader;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageIterator;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.TextExtractor;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class TextExtractTest extends PDFNetSample {

	private static OutputListener mOutputListener;

	private static ArrayList<String> mFileList = new ArrayList<>();

    public TextExtractTest() {
        setTitle(R.string.sample_textextract_title);
        setDescription(R.string.sample_textextract_description);
    }

	@Override
	public void run(OutputListener outputListener) {
		super.run(outputListener);
		mOutputListener = outputListener;
		mFileList.clear();
		printHeader(outputListener);

        // string output_path = "../../TestFiles/Output/";
        boolean example1_basic = false;
        boolean example2_xml = false;
        boolean example3_wordlist = false;
        boolean example4_advanced = true;
        boolean example5_low_level = false;

        // Sample code showing how to use high-level text extraction APIs.
        try {
            PDFDoc doc = new PDFDoc(Utils.getAssetTempFile(INPUT_PATH + "newsletter.pdf").getAbsolutePath());
            doc.initSecurityHandler();

            Page page = doc.getPage(1);
            if (page == null) {
                mOutputListener.println("Page not found.");
            }

            TextExtractor txt = new TextExtractor();
            txt.begin(page);  // Read the page.
            // Other options you may want to consider...
            // txt.Begin(page, 0, TextExtractor.ProcessingFlags.e_no_dup_remove);
            // txt.Begin(page, 0, TextExtractor.ProcessingFlags.e_remove_hidden_text);
            // ...

            // Example 1. Get all text on the page in a single string.
            // Words will be separated with space or new line characters.
            if (example1_basic) {
                // Get the word count.
                mOutputListener.println("Word Count: " + txt.getWordCount());

                mOutputListener.println("\n\n- GetAsText --------------------------\n" + txt.getAsText());
                mOutputListener.println("-----------------------------------------------------------");
            }

            // Example 2. Get XML logical structure for the page.
            if (example2_xml) {
                String text = txt.getAsXML(TextExtractor.e_words_as_elements | TextExtractor.e_output_bbox | TextExtractor.e_output_style_info);
                mOutputListener.println("\n\n- GetAsXML  --------------------------\n" + text);
                mOutputListener.println("-----------------------------------------------------------");
            }

            // Example 3. Extract words one by one.
            if (example3_wordlist) {
                TextExtractor.Word word;
                for (TextExtractor.Line line = txt.getFirstLine(); line.isValid(); line = line.getNextLine()) {
                    for (word = line.getFirstWord(); word.isValid(); word = word.getNextWord()) {
                        mOutputListener.println(word.getString());
                    }
                }
                mOutputListener.println("-----------------------------------------------------------");
            }

            // Example 4. A more advanced text extraction example.
            // The output is XML structure containing paragraphs, lines, words,
            // as well as style and positioning information.
            if (example4_advanced) {
                Rect bbox;
                int cur_flow_id = -1, cur_para_id = -1;

                TextExtractor.Line line;
                TextExtractor.Word word;
                TextExtractor.Style s, line_style;

                mOutputListener.println("<PDFText>");
                // For each line on the page...
                for (line = txt.getFirstLine(); line.isValid(); line = line.getNextLine()) {
                    if (line.getNumWords() == 0)
                        continue;
                    if (cur_flow_id != line.getFlowID()) {
                        if (cur_flow_id != -1) {
                            if (cur_para_id != -1) {
                                cur_para_id = -1;
                                mOutputListener.println("</Para>");
                            }
                            mOutputListener.println("</Flow>");
                        }
                        cur_flow_id = line.getFlowID();
                        mOutputListener.println("<Flow id=\"" + cur_flow_id + "\">");
                    }

                    if (cur_para_id != line.getParagraphID()) {
                        if (cur_para_id != -1)
                            mOutputListener.println("</Para>");
                        cur_para_id = line.getParagraphID();
                        mOutputListener.println("<Para id=\"" + cur_para_id + "\">");
                    }

                    bbox = line.getBBox();
                    line_style = line.getStyle();
                    mOutputListener.print("<Line box=\"" +  String.format("%.2f, %.2f, %.2f, %.2f", bbox.getX1(), bbox.getY1(), bbox.getX2(), bbox.getY2()) + "\"");
                    printStyle(line_style);
                    mOutputListener.println(" cur_num=\"" + line.getCurrentNum() + "\">");
                 

                    // For each word in the line...
                    for (word = line.getFirstWord(); word.isValid(); word = word.getNextWord()) {
                        // Output the bounding box for the word.
                        bbox = word.getBBox();
                        mOutputListener.print("<Word box=\"" +  String.format("%.2f, %.2f, %.2f, %.2f", bbox.getX1(), bbox.getY1(), bbox.getX2(), bbox.getY2()) + "\"");
                        mOutputListener.print(" cur_num=\"" + word.getCurrentNum() + "\"");
                        int sz = word.getStringLen();
                        if (sz == 0) continue;

                        // If the word style is different from the parent style, output the new style.
                        s = word.getStyle();
                        if (!s.equals(line_style)) {
                            printStyle(s);
                        }

                        mOutputListener.print(">" + word.getString());
                        mOutputListener.println("</Word>");
                    }
                    mOutputListener.println("</Line>");
                }

                if (cur_flow_id != -1) {
                    if (cur_para_id != -1) {
                        cur_para_id = -1;
                        mOutputListener.println("</Para>");
                    }
                    mOutputListener.println("</Flow>");
                }
            }
            txt.destroy();
            doc.close();
            mOutputListener.println("</PDFText>");
        } catch (PDFNetException e) {
            mOutputListener.printError(e.getStackTrace());
        }

        // Sample code showing how to use low-level text extraction APIs.
        if (example5_low_level) {
            try {
                PDFDoc doc = new PDFDoc((Utils.getAssetTempFile(INPUT_PATH + "newsletter.pdf").getAbsolutePath()));
                doc.initSecurityHandler();

                // Example 1. Extract all text content from the document

                ElementReader reader = new ElementReader();
                //  Read every page
                for (PageIterator itr = doc.getPageIterator(); itr.hasNext(); ) {
                    reader.begin(itr.next());
                    DumpAllText(reader);
                    reader.end();
                }

                // Example 2. Extract text content based on the
                // selection rectangle.
                mOutputListener.print("\n----------------------------------------------------");
                mOutputListener.print("\nExtract text based on the selection rectangle.");
                mOutputListener.println("\n----------------------------------------------------");

                Page first_page = doc.getPageIterator().next();
                String s1 = ReadTextFromRect(first_page, new Rect(27, 392, 563, 534), reader);
                mOutputListener.print("\nField 1: " + s1);

                s1 = ReadTextFromRect(first_page, new Rect(28, 551, 106, 623), reader);
                mOutputListener.print("\nField 2: " + s1);

                s1 = ReadTextFromRect(first_page, new Rect(208, 550, 387, 621), reader);
                mOutputListener.print("\nField 3: " + s1);

                // ...
                doc.close();
                mOutputListener.println("Done.");
            } catch (Exception e) {
                mOutputListener.printError(e.getStackTrace());
            }
        }

		for (String file : mFileList) {
			addToFileList(file);
		}
		printFooter(outputListener);
	}


    static void printStyle(TextExtractor.Style s) {
        byte r = s.getColor()[0];
		byte g = s.getColor()[1];
		byte b = s.getColor()[2];
        String rgb_hex =  String.format("%02X%02X%02X;", r, g, b );
        DecimalFormat df = new DecimalFormat("#.#");
        mOutputListener.print(" style=\"font-family:" + s.getFontName() + "; "
                + "font-size:" + df.format(s.getFontSize()) + ";"
                + (s.isSerif() ? " sans-serif; " : " ")
                + "color:#" + rgb_hex + "\"");
    }

    // A utility method used to dump all text content in the console window.
    static void DumpAllText(ElementReader reader) throws PDFNetException {
        Element element;
        while ((element = reader.next()) != null) {
            switch (element.getType()) {
                case Element.e_text_begin:
                    mOutputListener.println("\n--> Text Block Begin");
                    break;
                case Element.e_text_end:
                    mOutputListener.println("\n--> Text Block End");
                    break;
                case Element.e_text: {
                    Rect bbox = element.getBBox();
                    if (bbox == null) continue;
                    mOutputListener.println("\n--> BBox: " + bbox.getX1() + ", "
                            + bbox.getY1() + ", "
                            + bbox.getX2() + ", "
                            + bbox.getY2());

                    String arr = element.getTextString();
                    mOutputListener.println(arr);
                }
                break;
                case Element.e_text_new_line:
                    mOutputListener.println("\n--> New Line");
                    break;
                case Element.e_form:                // Process form XObjects
                    reader.formBegin();
                    DumpAllText(reader);
                    reader.end();
                    break;
            }
        }
    }

    // A helper method for ReadTextFromRect
    static String RectTextSearch(ElementReader reader, Rect pos) throws PDFNetException {
        Element element;
        String srch_str = new String();
        while ((element = reader.next()) != null) {
            switch (element.getType()) {
                case Element.e_text: {
                    Rect bbox = element.getBBox();
                    if (bbox == null) continue;
                    if (bbox.intersectRect(bbox, pos)) {
                        String arr = element.getTextString();
                        srch_str += arr;
                        srch_str += "\n"; // add a new line?
                    }
                    break;
                }
                case Element.e_text_new_line: {
                    break;
                }
                case Element.e_form: // Process form XObjects
                {
                    reader.formBegin();
                    srch_str += RectTextSearch(reader, pos);
                    reader.end();
                    break;
                }
            }
        }
        return srch_str;
    }

    // A utility method used to extract all text content from
    // a given selection rectangle. The rectangle coordinates are
    // expressed in PDF user/page coordinate system.
    static String ReadTextFromRect(Page page, Rect pos, ElementReader reader) throws PDFNetException {
        reader.begin(page);
        String srch_str = RectTextSearch(reader, pos);
        reader.end();
        return srch_str;
    }

}