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
import com.pdftron.pdf.Element
import com.pdftron.pdf.ElementReader
import com.pdftron.pdf.Image
import com.pdftron.pdf.PDFDoc
import java.util.*

///-----------------------------------------------------------------------------------
/// This sample illustrates one approach to PDF image extraction
/// using PDFNet.
///
/// Note: Besides direct image export, you can also convert PDF images
/// to Java image, or extract uncompressed/compressed image data directly
/// using element.GetImageData() (e.g. as illustrated in ElementReaderAdv
/// sample project).
///-----------------------------------------------------------------------------------

class ImageExtractTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_imageextract_title)
        setDescription(R.string.sample_imageextract_description)

        // The standard library does not support exporting to
        // PNG/TIFF formats, thus trying to export the PDF to
        // PNG or TIFF will fail. Please, comment out this call
        // if using the full library.
        // DisableRun();
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)
        // Initialize PDFNet

        // Example 1:
        // Extract images by traversing the display list for
        // every page. With this approach it is possible to obtain
        // image positioning information and DPI.
        // Initialize PDFNet

        // Example 1:
        // Extract images by traversing the display list for
        // every page. With this approach it is possible to obtain
        // image positioning information and DPI.
        try {
            val doc = PDFDoc(Utils.getAssetTempFile(INPUT_PATH + "newsletter.pdf")!!.absolutePath)
            doc.initSecurityHandler()
            val reader = ElementReader()
            //  Read every page
            val itr = doc.pageIterator
            while (itr.hasNext()) {
                reader.begin(itr.next())
                ImageExtract(reader)
                reader.end()
            }
            doc.close()
            mOutputListener!!.println("Done.")
        } catch (e: Exception) {
            mOutputListener!!.printError(e.stackTrace)
        }

        mOutputListener!!.println("----------------------------------------------------------------")

        // Example 2:
        // Extract images by scanning the low-level document.

        // Example 2:
        // Extract images by scanning the low-level document.
        try {
            val doc = PDFDoc(Utils.getAssetTempFile(INPUT_PATH + "newsletter.pdf")!!.absolutePath)
            doc.initSecurityHandler()
            image_counter = 0
            val cos_doc = doc.sdfDoc
            val num_objs = cos_doc.xRefSize()
            for (i in 1 until num_objs) {
                val obj = cos_doc.getObj(i)
                if (obj != null && !obj.isFree && obj.isStream) {
                    // Process only images
                    var itr = obj.find("Type")
                    if (!itr.hasNext() || itr.value().name != "XObject") continue
                    itr = obj.find("Subtype")
                    if (!itr.hasNext() || itr.value().name != "Image") continue
                    val image = Image(obj)
                    mOutputListener!!.println("--> Image: " + ++image_counter)
                    mOutputListener!!.println("    Width: " + image.imageWidth)
                    mOutputListener!!.println("    Height: " + image.imageHeight)
                    mOutputListener!!.println("    BPC: " + image.bitsPerComponent)
                    val fname = "image_extract2_$image_counter"
                    val path = Utils.createExternalFile(fname, mFileList).absolutePath
                    image.export(path)

                    //String path= Utils.createExternalFile(fname + ".tif", mFileList).getAbsolutePath();
                    //image.exportAsTiff(path);

                    //String path = Utils.createExternalFile(fname + ".png", mFileList).getAbsolutePath();
                    //image.exportAsPng(path);
                }
            }
            doc.close()
            mOutputListener!!.println("Done.")
        } catch (e: Exception) {
            mOutputListener!!.printError(e.stackTrace)
        }

        for (file in mFileList) {
            addToFileList(file!!)
        }
        printFooter(outputListener!!)
    }

    companion object {

        private var mOutputListener: OutputListener? = null

        private val mFileList = ArrayList<String>()

        // Relative paths to folders containing test files.

        internal var image_counter = 0

        @Throws(PDFNetException::class)
        fun ImageExtract(reader: ElementReader) {
            var element: Element
            while (reader.next().also { element = it } != null) {
                when (element.type) {
                    Element.e_image, Element.e_inline_image -> {
                        mOutputListener!!.println("--> Image: " + ++image_counter)
                        mOutputListener!!.println("    Width: " + element.imageWidth)
                        mOutputListener!!.println("    Height: " + element.imageHeight)
                        mOutputListener!!.println("    BPC: " + element.bitsPerComponent)
                        val ctm = element.ctm
                        val x2 = 1.0
                        val y2 = 1.0
                        val p = ctm.multPoint(x2, y2)
                        mOutputListener!!.println(String.format("    Coords: x1=%.2f, y1=%.2f, x2=%.2f, y2=%.2f", ctm.h, ctm.v, p.x, p.y))
                        if (element.type == Element.e_image) {
                            val image = Image(element.xObject)
                            val fname = "image_extract1_$image_counter"
                            val path = Utils.createExternalFile(fname, mFileList).absolutePath
                            image.export(path)

                            //String path2 = Utils.createExternalFile(fname + ".tif", mFileList).getAbsolutePath();
                            //image.exportAsTiff(path2);

                            //String path3 = Utils.createExternalFile(fname + ".png", mFileList).getAbsolutePath();
                            //image.exportAsPng(path3);
                        }
                    }
                    Element.e_form -> {
                        reader.formBegin()
                        ImageExtract(reader)
                        reader.end()
                    }
                }
            }
        }
    }

}