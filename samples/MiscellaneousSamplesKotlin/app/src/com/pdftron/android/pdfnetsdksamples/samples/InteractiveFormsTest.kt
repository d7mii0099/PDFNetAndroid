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
import com.pdftron.pdf.*
import com.pdftron.pdf.annots.*
import com.pdftron.sdf.Obj
import com.pdftron.sdf.SDFDoc
import java.util.*

//---------------------------------------------------------------------------------------
//This sample illustrates basic PDFNet capabilities related to interactive
//forms (also known as AcroForms).
//---------------------------------------------------------------------------------------

class InteractiveFormsTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_interactiveforms_title)
        setDescription(R.string.sample_interactiveforms_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)

        // string input_path =  "../../TestFiles/";

        // The vector used to store the name and count of all fields.
        // This is used later on to clone the fields
        val field_names = HashMap<String, Int>()

        //----------------------------------------------------------------------------------
        // Example 1: Programatically create new Form Fields and Widget Annotations.
        //----------------------------------------------------------------------------------
        try {
            val doc = PDFDoc()

            // Create a blank new page and add some form fields.
            val blank_page = doc.pageCreate()

            // Text Widget Creation
            // Create an empty text widget with black text.
            val text1 = TextWidget.create(doc, Rect(110.0, 700.0, 380.0, 730.0))
            text1.text = "Basic Text Field"
            text1.refreshAppearance()
            blank_page.annotPushBack(text1)
            // Create a vertical text widget with blue text and a yellow background.
            val text2 = TextWidget.create(doc, Rect(50.0, 400.0, 90.0, 730.0))
            text2.rotation = 90
            // Set the text content.
            text2.text = "    ****Lucky Stars!****"
            // Set the font type, text color, font size, border color and background color.
            text2.font = Font.create(doc, Font.e_helvetica_oblique)
            text2.fontSize = 28.0
            text2.setTextColor(ColorPt(0.0, 0.0, 1.0), 3)
            text2.setBorderColor(ColorPt(0.0, 0.0, 0.0), 3)
            text2.setBackgroundColor(ColorPt(1.0, 1.0, 0.0), 3)
            text2.refreshAppearance()
            // Add the annotation to the page.
            blank_page.annotPushBack(text2)
            // Create two new text widget with Field names employee.name.first and employee.name.last
            // This logic shows how these widgets can be created using either a field name string or
            // a Field object
            val text3 = TextWidget.create(doc, Rect(110.0, 660.0, 380.0, 690.0), "employee.name.first")
            text3.text = "Levi"
            text3.font = Font.create(doc, Font.e_times_bold)
            text3.refreshAppearance()
            blank_page.annotPushBack(text3)
            val emp_last_name = doc.fieldCreate("employee.name.last", Field.e_text, "Ackerman")
            val text4 = TextWidget.create(doc, Rect(110.0, 620.0, 380.0, 650.0), emp_last_name)
            text4.font = Font.create(doc, Font.e_times_bold)
            text4.refreshAppearance()
            blank_page.annotPushBack(text4)

            // Signature Widget Creation (unsigned)
            val signature1 = SignatureWidget.create(doc, Rect(110.0, 560.0, 260.0, 610.0))
            signature1.refreshAppearance()
            blank_page.annotPushBack(signature1)

            // CheckBox Widget Creation
            // Create a check box widget that is not checked.
            val check1 = CheckBoxWidget.create(doc, Rect(140.0, 490.0, 170.0, 520.0))
            check1.refreshAppearance()
            blank_page.annotPushBack(check1)
            // Create a check box widget that is checked.
            val check2 = CheckBoxWidget.create(doc, Rect(190.0, 490.0, 250.0, 540.0), "employee.name.check1")
            check2.setBackgroundColor(ColorPt(1.0, 1.0, 1.0), 3)
            check2.setBorderColor(ColorPt(0.0, 0.0, 0.0), 3)
            // Check the widget (by default it is unchecked).
            check2.isChecked = true
            check2.refreshAppearance()
            blank_page.annotPushBack(check2)

            // PushButton Widget Creation
            val pushbutton1 = PushButtonWidget.create(doc, Rect(380.0, 490.0, 520.0, 540.0))
            pushbutton1.setTextColor(ColorPt(1.0, 1.0, 1.0), 3)
            pushbutton1.fontSize = 36.0
            pushbutton1.setBackgroundColor(ColorPt(0.0, 0.0, 0.0), 3)
            // Add a caption for the pushbutton.
            pushbutton1.staticCaptionText = "PushButton"
            pushbutton1.refreshAppearance()
            blank_page.annotPushBack(pushbutton1)

            // ComboBox Widget Creation
            val combo1 = ComboBoxWidget.create(doc, Rect(280.0, 560.0, 580.0, 610.0))
            // Add options to the combobox widget.
            combo1.addOption("Combo Box No.1")
            combo1.addOption("Combo Box No.2")
            combo1.addOption("Combo Box No.3")
            // Make one of the options in the combo box selected by default.
            combo1.selectedOption = "Combo Box No.2"
            combo1.setTextColor(ColorPt(1.0, 0.0, 0.0), 3)
            combo1.fontSize = 28.0
            combo1.refreshAppearance()
            blank_page.annotPushBack(combo1)

            // ListBox Widget Creation
            val list1 = ListBoxWidget.create(doc, Rect(400.0, 620.0, 580.0, 730.0))
            // Add one option to the listbox widget.
            list1.addOption("List Box No.1")
            // Add multiple options to the listbox widget in a batch.
            val list_options = arrayOf("List Box No.2", "List Box No.3")
            list1.addOptions(list_options)
            // Select some of the options in list box as default options
            list1.selectedOptions = list_options
            // Enable list box to have multi-select when editing.
            list1.field.setFlag(Field.e_multiselect, true)
            list1.font = Font.create(doc, Font.e_times_italic)
            list1.setTextColor(ColorPt(1.0, 0.0, 0.0), 3)
            list1.fontSize = 28.0
            list1.setBackgroundColor(ColorPt(1.0, 1.0, 1.0), 3)
            list1.refreshAppearance()
            blank_page.annotPushBack(list1)

            // RadioButton Widget Creation
            // Create a radio button group and add three radio buttons in it.
            val radio_group = RadioButtonGroup.create(doc, "RadioGroup")
            val radiobutton1 = radio_group.add(Rect(140.0, 410.0, 190.0, 460.0))
            radiobutton1.setBackgroundColor(ColorPt(1.0, 1.0, 0.0), 3)
            radiobutton1.refreshAppearance()
            val radiobutton2 = radio_group.add(Rect(310.0, 410.0, 360.0, 460.0))
            radiobutton2.setBackgroundColor(ColorPt(0.0, 1.0, 0.0), 3)
            radiobutton2.refreshAppearance()
            val radiobutton3 = radio_group.add(Rect(480.0, 410.0, 530.0, 460.0))
            // Enable the third radio button. By default the first one is selected
            radiobutton3.enableButton()
            radiobutton3.setBackgroundColor(ColorPt(0.0, 1.0, 1.0), 3)
            radiobutton3.refreshAppearance()
            radio_group.addGroupButtonsToPage(blank_page)

            // Custom push button annotation creation
            val custom_pushbutton1 = PushButtonWidget.create(doc, Rect(260.0, 320.0, 360.0, 360.0))
            // Set the annotation appearance.
            custom_pushbutton1.setAppearance(createCustomButtonAppearance(doc, false), Annot.e_normal)
            // Create 'SubmitForm' action. The action will be linked to the button.
            val url = FileSpec.createURL(doc, "http://www.pdftron.com")
            val button_action = Action.createSubmitForm(url)
            // Associate the above action with 'Down' event in annotations action dictionary.
            val annot_action = custom_pushbutton1.sdfObj.putDict("AA")
            annot_action.put("D", button_action.sdfObj)
            blank_page.annotPushBack(custom_pushbutton1)

            // Add the page as the last page in the document.
            doc.pagePushBack(blank_page)

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
            doc.refreshFieldAppearances()

            doc.save(Utils.createExternalFile("forms_test1.pdf", mFileList).absolutePath, SDFDoc.SaveMode.NO_FLAGS, null)
            doc.close()
            mOutputListener!!.println("Done.")
        } catch (e: Exception) {
            mOutputListener!!.printError(e.stackTrace)
        }

        //----------------------------------------------------------------------------------
        // Example 2:
        // Fill-in forms / Modify values of existing fields.
        // Traverse all form fields in the document (and print out their names).
        // Search for specific fields in the document.
        //----------------------------------------------------------------------------------
        try {
            val doc = PDFDoc(Utils.createExternalFile("forms_test1.pdf", mFileList).absolutePath)
            doc.initSecurityHandler()

            val itr = doc.fieldIterator
            while (itr.hasNext()) {
                val current = itr.next()!!
                val cur_field_name = current.getName()
                // Add one to the count for this field name for later processing
                if (field_names.containsKey(cur_field_name)) {
                    field_names[cur_field_name] = field_names[cur_field_name]!! + 1
                } else {
                    field_names[cur_field_name] = 1
                }

                mOutputListener!!.println("Field name: " + current.getName())
                mOutputListener!!.println("Field partial name: " + current.getPartialName())

                mOutputListener!!.print("Field type: ")
                val type = current.getType()
                val str_val = current.getValueAsString()
                when (type) {
                    Field.e_button -> mOutputListener!!.println("Button")
                    Field.e_radio -> mOutputListener!!.println("Radio button: Value = $str_val")
                    Field.e_check -> {
                        current.setValue(true)
                        mOutputListener!!.println("Check box: Value = $str_val")
                    }
                    Field.e_text -> {
                        mOutputListener!!.println("Text")
                        // Edit all variable text in the document
                        val old_value: String
                        if (current.getValue() != null) {
                            old_value = current.getValueAsString()
                            current.setValue("This is a new value. The old one was: $old_value")
                        }
                    }
                    Field.e_choice -> mOutputListener!!.println("Choice")
                    Field.e_signature -> mOutputListener!!.println("Signature")
                }

                mOutputListener!!.println("------------------------------")
            }

            // Search for a specific field
            val f = doc.getField("employee.name.first")
            if (f != null) {
                mOutputListener!!.println("Field search for " + f.name + " was successful")
            } else {
                mOutputListener!!.println("Field search failed")
            }

            // Regenerate field appearances.
            doc.refreshFieldAppearances()
            doc.save(Utils.createExternalFile("forms_test_edit.pdf", mFileList).absolutePath, SDFDoc.SaveMode.NO_FLAGS, null)
            doc.close()
            mOutputListener!!.println("Done.")
        } catch (e: Exception) {
            mOutputListener!!.printError(e.stackTrace)
        }

        //----------------------------------------------------------------------------------
        // Sample: Form templating
        // Replicate pages and form data within a document. Then rename field names to make
        // them unique.
        //----------------------------------------------------------------------------------
        try {
            // Sample: Copying the page with forms within the same document
            val doc = PDFDoc(Utils.createExternalFile("forms_test1.pdf", mFileList).absolutePath)
            doc.initSecurityHandler()

            val src_page = doc.getPage(1) as Page
            doc.pagePushBack(src_page)  // Append several copies of the first page
            doc.pagePushBack(src_page)     // Note that forms are successfully copied
            doc.pagePushBack(src_page)
            doc.pagePushBack(src_page)

            // Now we rename fields in order to make every field unique.
            // You can use this technique for dynamic template filling where you have a 'master'
            // form page that should be replicated, but with unique field names on every page.
            for (cur_field in field_names.keys) {
                renameAllFields(doc, cur_field, field_names[cur_field]!!)
            }

            doc.save(Utils.createExternalFile("forms_test1_cloned.pdf", mFileList).absolutePath, SDFDoc.SaveMode.NO_FLAGS, null)
            doc.close()
            mOutputListener!!.println("Done.")
        } catch (e: Exception) {
            mOutputListener!!.printError(e.stackTrace)
        }

        //----------------------------------------------------------------------------------
        // Sample:
        // Flatten all form fields in a document.
        // Note that this sample is intended to show that it is possible to flatten
        // individual fields. PDFNet provides a utility function PDFDoc.flattenAnnotations()
        // that will automatically flatten all fields.
        //----------------------------------------------------------------------------------
        try {
            val doc = PDFDoc(Utils.createExternalFile("forms_test1.pdf", mFileList).absolutePath)
            doc.initSecurityHandler()

            // Traverse all pages
            if (true) {
                doc.flattenAnnotations()
            } else
            // Manual flattening
            {

                val pitr = doc.pageIterator
                while (pitr.hasNext()) {
                    val page = pitr.next()!!
                    for (i in page.getNumAnnots() - 1 downTo 0) {
                        val annot = page.getAnnot(i)
                        if (annot.type == Annot.e_Widget) {
                            annot.flatten(page)
                        }
                    }
                }
            }

            doc.save(Utils.createExternalFile("forms_test1_flattened.pdf", mFileList).absolutePath, SDFDoc.SaveMode.NO_FLAGS, null)
            doc.close()
            mOutputListener!!.println("Done.")
        } catch (e: Exception) {
            mOutputListener!!.printError(e.stackTrace)
        }

        for (file in mFileList) {
            addToFileList(file)
        }
        printFooter(outputListener)
    }

    companion object {

        private var mOutputListener: OutputListener? = null

        private val mFileList = ArrayList<String>()

        // field_nums has to be greater than 0.
        @Throws(PDFNetException::class)
        internal fun renameAllFields(doc: PDFDoc, name: String, field_nums: Int) {
            var itr = doc.getFieldIterator(name)
            var counter = 1
            while (itr.hasNext()) {
                val f = itr.next()!!
                val update_count = java.lang.Math.ceil(counter / field_nums.toDouble()).toInt()
                f.rename("$name-$update_count")
                itr = doc.getFieldIterator(name)
                ++counter
            }
        }

        @Throws(PDFNetException::class)
        internal fun createCustomButtonAppearance(doc: PDFDoc, button_down: Boolean): Obj {
            // Create a button appearance stream ------------------------------------
            val build = ElementBuilder()
            val writer = ElementWriter()
            writer.begin(doc)

            // Draw background
            var element = build.createRect(0.0, 0.0, 101.0, 37.0)
            element.setPathFill(true)
            element.setPathStroke(false)
            element.gState.fillColorSpace = ColorSpace.createDeviceGray()
            element.gState.fillColor = ColorPt(0.75, 0.0, 0.0)
            writer.writeElement(element)

            // Draw 'Submit' text
            writer.writeElement(build.createTextBegin())
            run {
                val text = "Submit"
                element = build.createTextRun(text, Font.create(doc, Font.e_helvetica_bold), 12.0)
                element.gState.fillColor = ColorPt(0.0, 0.0, 0.0)

                if (button_down)
                    element.setTextMatrix(1.0, 0.0, 0.0, 1.0, 33.0, 10.0)
                else
                    element.setTextMatrix(1.0, 0.0, 0.0, 1.0, 30.0, 13.0)
                writer.writeElement(element)
            }
            writer.writeElement(build.createTextEnd())

            val stm = writer.end()

            // Set the bounding box
            stm.putRect("BBox", 0.0, 0.0, 101.0, 37.0)
            stm.putName("Subtype", "Form")
            return stm
        }
    }

}