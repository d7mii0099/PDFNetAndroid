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
import com.pdftron.pdf.Action;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.ColorSpace;
import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementBuilder;
import com.pdftron.pdf.ElementWriter;
import com.pdftron.pdf.Field;
import com.pdftron.pdf.FieldIterator;
import com.pdftron.pdf.FileSpec;
import com.pdftron.pdf.Font;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageIterator;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.CheckBoxWidget;
import com.pdftron.pdf.annots.ComboBoxWidget;
import com.pdftron.pdf.annots.ListBoxWidget;
import com.pdftron.pdf.annots.PushButtonWidget;
import com.pdftron.pdf.annots.RadioButtonGroup;
import com.pdftron.pdf.annots.RadioButtonWidget;
import com.pdftron.pdf.annots.SignatureWidget;
import com.pdftron.pdf.annots.TextWidget;
import com.pdftron.sdf.Obj;
import com.pdftron.sdf.SDFDoc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

//---------------------------------------------------------------------------------------
//This sample illustrates basic PDFNet capabilities related to interactive
//forms (also known as AcroForms).
//---------------------------------------------------------------------------------------

public class InteractiveFormsTest extends PDFNetSample {

	private static OutputListener mOutputListener;

	private static ArrayList<String> mFileList = new ArrayList<>();

    public InteractiveFormsTest() {
        setTitle(R.string.sample_interactiveforms_title);
        setDescription(R.string.sample_interactiveforms_description);
    }

	@Override
	public void run(OutputListener outputListener) {
		super.run(outputListener);
		mOutputListener = outputListener;
		mFileList.clear();
		printHeader(outputListener);

        // string input_path =  "../../TestFiles/";

        // The vector used to store the name and count of all fields.
        // This is used later on to clone the fields
        Map<String, Integer> field_names = new HashMap<String, Integer>();

        //----------------------------------------------------------------------------------
        // Example 1: Programatically create new Form Fields and Widget Annotations.
        //----------------------------------------------------------------------------------
        try {
            PDFDoc doc = new PDFDoc();

            // Create a blank new page and add some form fields.
            Page blank_page = doc.pageCreate();

            // Text Widget Creation 
            // Create an empty text widget with black text.
            TextWidget text1 = TextWidget.create(doc, new Rect(110, 700, 380, 730));
            text1.setText("Basic Text Field");
            text1.refreshAppearance();
            blank_page.annotPushBack(text1);
            // Create a vertical text widget with blue text and a yellow background.
            TextWidget text2 = TextWidget.create(doc, new Rect(50, 400, 90, 730));
            text2.setRotation(90);
            // Set the text content.
            text2.setText("    ****Lucky Stars!****");
            // Set the font type, text color, font size, border color and background color.
            text2.setFont(Font.create(doc, Font.e_helvetica_oblique));
            text2.setFontSize(28);
            text2.setTextColor(new ColorPt(0, 0, 1), 3);
            text2.setBorderColor(new ColorPt(0, 0, 0), 3);
            text2.setBackgroundColor(new ColorPt(1, 1, 0), 3);
            text2.refreshAppearance();
            // Add the annotation to the page.
            blank_page.annotPushBack(text2);
            // Create two new text widget with Field names employee.name.first and employee.name.last
            // This logic shows how these widgets can be created using either a field name string or
            // a Field object
            TextWidget text3 = TextWidget.create(doc, new Rect(110, 660, 380, 690), "employee.name.first");
            text3.setText("Levi");
            text3.setFont(Font.create(doc, Font.e_times_bold));
            text3.refreshAppearance();
            blank_page.annotPushBack(text3);
            Field emp_last_name = doc.fieldCreate("employee.name.last", Field.e_text, "Ackerman");
            TextWidget text4 = TextWidget.create(doc, new Rect(110, 620, 380, 650), emp_last_name);
            text4.setFont(Font.create(doc, Font.e_times_bold));
            text4.refreshAppearance();
            blank_page.annotPushBack(text4);

            // Signature Widget Creation (unsigned)
            SignatureWidget signature1 = SignatureWidget.create(doc, new Rect(110, 560, 260, 610));
            signature1.refreshAppearance();
            blank_page.annotPushBack(signature1);

            // CheckBox Widget Creation
            // Create a check box widget that is not checked.
            CheckBoxWidget check1 = CheckBoxWidget.create(doc, new Rect(140, 490, 170, 520));
            check1.refreshAppearance();
            blank_page.annotPushBack(check1);
            // Create a check box widget that is checked.
            CheckBoxWidget check2 = CheckBoxWidget.create(doc, new Rect(190, 490, 250, 540), "employee.name.check1");
            check2.setBackgroundColor(new ColorPt(1, 1, 1), 3);
            check2.setBorderColor(new ColorPt(0, 0, 0), 3);
            // Check the widget (by default it is unchecked).
            check2.setChecked(true);
            check2.refreshAppearance();
            blank_page.annotPushBack(check2);

            // PushButton Widget Creation
            PushButtonWidget pushbutton1 = PushButtonWidget.create(doc, new Rect(380, 490, 520, 540));
            pushbutton1.setTextColor(new ColorPt(1, 1, 1), 3);
            pushbutton1.setFontSize(36);
            pushbutton1.setBackgroundColor(new ColorPt(0, 0, 0), 3);
            // Add a caption for the pushbutton.
            pushbutton1.setStaticCaptionText("PushButton");
            pushbutton1.refreshAppearance();
            blank_page.annotPushBack(pushbutton1);

            // ComboBox Widget Creation
            ComboBoxWidget combo1 = ComboBoxWidget.create(doc, new Rect(280, 560, 580, 610));
            // Add options to the combobox widget.
            combo1.addOption("Combo Box No.1");
            combo1.addOption("Combo Box No.2");
            combo1.addOption("Combo Box No.3");
            // Make one of the options in the combo box selected by default.
            combo1.setSelectedOption("Combo Box No.2");
            combo1.setTextColor(new ColorPt(1, 0, 0), 3);
            combo1.setFontSize(28);
            combo1.refreshAppearance();
            blank_page.annotPushBack(combo1);

            // ListBox Widget Creation
            ListBoxWidget list1 = ListBoxWidget.create(doc, new Rect(400, 620, 580, 730));
            // Add one option to the listbox widget.
            list1.addOption("List Box No.1");
            // Add multiple options to the listbox widget in a batch.
            String[] list_options = new String[] { "List Box No.2", "List Box No.3" };
            list1.addOptions(list_options);
            // Select some of the options in list box as default options
            list1.setSelectedOptions(list_options);
            // Enable list box to have multi-select when editing. 
            list1.getField().setFlag(Field.e_multiselect, true);
            list1.setFont(Font.create(doc,Font.e_times_italic));
            list1.setTextColor(new ColorPt(1, 0, 0), 3);
            list1.setFontSize(28);
            list1.setBackgroundColor(new ColorPt(1, 1, 1), 3);
            list1.refreshAppearance();
            blank_page.annotPushBack(list1);

            // RadioButton Widget Creation
            // Create a radio button group and add three radio buttons in it. 
            RadioButtonGroup radio_group = RadioButtonGroup.create(doc, "RadioGroup");
            RadioButtonWidget radiobutton1 = radio_group.add(new Rect(140, 410, 190, 460));
            radiobutton1.setBackgroundColor(new ColorPt(1, 1, 0), 3);
            radiobutton1.refreshAppearance();
            RadioButtonWidget radiobutton2 = radio_group.add(new Rect(310, 410, 360, 460));
            radiobutton2.setBackgroundColor(new ColorPt(0, 1, 0), 3);
            radiobutton2.refreshAppearance();
            RadioButtonWidget radiobutton3 = radio_group.add(new Rect(480, 410, 530, 460));
            // Enable the third radio button. By default the first one is selected
            radiobutton3.enableButton();
            radiobutton3.setBackgroundColor(new ColorPt(0, 1, 1), 3);
            radiobutton3.refreshAppearance();
            radio_group.addGroupButtonsToPage(blank_page);

            // Custom push button annotation creation
            PushButtonWidget custom_pushbutton1 = PushButtonWidget.create(doc, new Rect(260, 320, 360, 360));
            // Set the annotation appearance.
            custom_pushbutton1.setAppearance(createCustomButtonAppearance(doc, false), Annot.e_normal);
            // Create 'SubmitForm' action. The action will be linked to the button.
            FileSpec url = FileSpec.createURL(doc, "http://www.pdftron.com");
            Action button_action = Action.createSubmitForm(url);
            // Associate the above action with 'Down' event in annotations action dictionary.
            Obj annot_action = custom_pushbutton1.getSDFObj().putDict("AA");
            annot_action.put("D", button_action.getSDFObj());
            blank_page.annotPushBack(custom_pushbutton1);

            // Add the page as the last page in the document.
            doc.pagePushBack(blank_page);    

            // If you are not satisfied with the look of default auto-generated appearance
            // streams you can delete "AP" entry from the Widget annotation and set
            // "NeedAppearances" flag in AcroForm dictionary:
            //    doc.GetAcroForm().PutBool("NeedAppearances", true);
            // This will force the viewer application to auto-generate new appearance streams
            // every time the document is opened.
            //
            // Alternatively you can generate custom annotation appearance using ElementWriter
            // and then set the "AP" entry in the widget dictionary to the new appearance
            // stream.
            //
            // Yet another option is to pre-populate field entries with dummy text. When
            // you edit the field values using PDFNet the new field appearances will match
            // the old ones.

            //doc.GetAcroForm().Put("NeedAppearances", new Bool(true));
            doc.refreshFieldAppearances();

            doc.save(Utils.createExternalFile("forms_test1.pdf", mFileList).getAbsolutePath(), SDFDoc.SaveMode.NO_FLAGS, null);
            doc.close();
            mOutputListener.println("Done.");
        } catch (Exception e) {
            mOutputListener.printError(e.getStackTrace());
        }

        //----------------------------------------------------------------------------------
        // Example 2:
        // Fill-in forms / Modify values of existing fields.
        // Traverse all form fields in the document (and print out their names).
        // Search for specific fields in the document.
        //----------------------------------------------------------------------------------
        try {
            PDFDoc doc = new PDFDoc((Utils.createExternalFile("forms_test1.pdf", mFileList).getAbsolutePath()));
            doc.initSecurityHandler();

            FieldIterator itr = doc.getFieldIterator();
            while (itr.hasNext()) {
                Field current = itr.next();
                String cur_field_name = current.getName();
                // Add one to the count for this field name for later processing
                if (field_names.containsKey(cur_field_name)) {
                    field_names.put(cur_field_name, field_names.get(cur_field_name) + 1);
                }
                else {
                    field_names.put(cur_field_name, 1);
                }

                mOutputListener.println("Field name: " + current.getName());
                mOutputListener.println("Field partial name: " + current.getPartialName());

                mOutputListener.print("Field type: ");
                int type = current.getType();
                String str_val = current.getValueAsString();
                switch (type) {
                    case Field.e_button:
                        mOutputListener.println("Button");
                        break;
                    case Field.e_radio:
                        mOutputListener.println("Radio button: Value = " + str_val);
                        break;
                    case Field.e_check:
                        current.setValue(true);
                        mOutputListener.println("Check box: Value = " + str_val);
                        break;
                    case Field.e_text: {
                        mOutputListener.println("Text");
                        // Edit all variable text in the document
                        String old_value;
                        if (current.getValue() != null) {
                            old_value = current.getValueAsString();
                            current.setValue("This is a new value. The old one was: " + old_value);
                        }
                    }
                    break;
                    case Field.e_choice:
                        mOutputListener.println("Choice");
                        break;
                    case Field.e_signature:
                        mOutputListener.println("Signature");
                        break;
                }

                mOutputListener.println("------------------------------");
            }

            // Search for a specific field
            Field f = doc.getField("employee.name.first");
            if (f != null) {
                mOutputListener.println("Field search for " + f.getName() + " was successful");
            } else {
                mOutputListener.println("Field search failed");
            }

            // Regenerate field appearances.
            doc.refreshFieldAppearances();
            doc.save((Utils.createExternalFile("forms_test_edit.pdf", mFileList).getAbsolutePath()), SDFDoc.SaveMode.NO_FLAGS, null);
            doc.close();
            mOutputListener.println("Done.");
        } catch (Exception e) {
            mOutputListener.printError(e.getStackTrace());
        }

        //----------------------------------------------------------------------------------
        // Sample: Form templating
        // Replicate pages and form data within a document. Then rename field names to make
        // them unique.
        //----------------------------------------------------------------------------------
        try {
            // Sample: Copying the page with forms within the same document
            PDFDoc doc = new PDFDoc((Utils.createExternalFile("forms_test1.pdf", mFileList).getAbsolutePath()));
            doc.initSecurityHandler();

            Page src_page = (Page) (doc.getPage(1));
            doc.pagePushBack(src_page);  // Append several copies of the first page
            doc.pagePushBack(src_page);     // Note that forms are successfully copied
            doc.pagePushBack(src_page);
            doc.pagePushBack(src_page);

            // Now we rename fields in order to make every field unique.
            // You can use this technique for dynamic template filling where you have a 'master'
            // form page that should be replicated, but with unique field names on every page.
            for (String cur_field : field_names.keySet()) {
                renameAllFields(doc, cur_field, field_names.get(cur_field));
            }

            doc.save(Utils.createExternalFile("forms_test1_cloned.pdf", mFileList).getAbsolutePath(), SDFDoc.SaveMode.NO_FLAGS, null);
            doc.close();
            mOutputListener.println("Done.");
        } catch (Exception e) {
            mOutputListener.printError(e.getStackTrace());
        }

        //----------------------------------------------------------------------------------
        // Sample:
        // Flatten all form fields in a document.
        // Note that this sample is intended to show that it is possible to flatten
        // individual fields. PDFNet provides a utility function PDFDoc.flattenAnnotations()
        // that will automatically flatten all fields.
        //----------------------------------------------------------------------------------
        try {
            PDFDoc doc = new PDFDoc((Utils.createExternalFile("forms_test1.pdf", mFileList).getAbsolutePath()));
            doc.initSecurityHandler();

            // Traverse all pages
            if (true) {
                doc.flattenAnnotations();
            } else // Manual flattening
            {

                for (PageIterator pitr = doc.getPageIterator(); pitr.hasNext(); ) {
                    Page page = pitr.next();
                    for (int i = page.getNumAnnots() - 1; i >= 0; --i) {
                        Annot annot = page.getAnnot(i);
                        if (annot.getType() == Annot.e_Widget)
                        {
                            annot.flatten(page);
                        }
                    }
                }
            }

            doc.save(Utils.createExternalFile("forms_test1_flattened.pdf", mFileList).getAbsolutePath(), SDFDoc.SaveMode.NO_FLAGS, null);
            doc.close();
            mOutputListener.println("Done.");
        } catch (Exception e) {
            mOutputListener.printError(e.getStackTrace());
        }

		for (String file : mFileList) {
			addToFileList(file);
		}
		printFooter(outputListener);
	}

    // field_nums has to be greater than 0.
    static void renameAllFields(PDFDoc doc, String name, int field_nums) throws PDFNetException {
        FieldIterator itr = doc.getFieldIterator(name);
        for (int counter = 1; itr.hasNext(); itr = doc.getFieldIterator(name), ++counter) {
            Field f = itr.next();
            int update_count = (int)java.lang.Math.ceil(counter/(double)field_nums);
            f.rename(name + "-" + update_count);
        }
    }

    static Obj createCustomButtonAppearance(PDFDoc doc, boolean button_down) throws PDFNetException {
        // Create a button appearance stream ------------------------------------
        ElementBuilder build = new ElementBuilder();
        ElementWriter writer = new ElementWriter();
        writer.begin(doc);

        // Draw background
        Element element = build.createRect(0, 0, 101, 37);
        element.setPathFill(true);
        element.setPathStroke(false);
        element.getGState().setFillColorSpace(ColorSpace.createDeviceGray());
        element.getGState().setFillColor(new ColorPt(0.75, 0, 0));
        writer.writeElement(element);

        // Draw 'Submit' text
        writer.writeElement(build.createTextBegin());
        {
            String text = "Submit";
            element = build.createTextRun(text, Font.create(doc, Font.e_helvetica_bold), 12);
            element.getGState().setFillColor(new ColorPt(0, 0, 0));

            if (button_down)
                element.setTextMatrix(1, 0, 0, 1, 33, 10);
            else
                element.setTextMatrix(1, 0, 0, 1, 30, 13);
            writer.writeElement(element);
        }
        writer.writeElement(build.createTextEnd());

        Obj stm = writer.end();

        // Set the bounding box
        stm.putRect("BBox", 0, 0, 101, 37);
        stm.putName("Subtype", "Form");
        return stm;
    }

}