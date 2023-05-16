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
import com.pdftron.filters.MappedFile
import com.pdftron.pdf.ElementWriter
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.PageIterator
import com.pdftron.sdf.SDFDoc

class PDFDocMemoryTest : PDFNetSample() {
    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)

        // The following sample illustrates how to read/write a PDF document from/to
        // a memory buffer.  This is useful for applications that work with dynamic PDF
        // documents that don't need to be saved/read from a disk.
        try {
            // Read a PDF document in a memory buffer.
            val file = MappedFile(Utils.getAssetTempFile(INPUT_PATH.toString() + "tiger.pdf")!!.getAbsolutePath())
            val file_sz: Long = file.fileSize()
            val file_reader: com.pdftron.filters.FilterReader = com.pdftron.filters.FilterReader(file)
            val mem = ByteArray(file_sz.toInt())
            val bytes_read: Long = file_reader.read(mem)
            val doc = PDFDoc(mem)
            doc.initSecurityHandler()
            val num_pages: Int = doc.getPageCount()
            val writer = ElementWriter()
            val reader: com.pdftron.pdf.ElementReader = com.pdftron.pdf.ElementReader()
            var element: com.pdftron.pdf.Element?

            // Create a duplicate of every page but copy only path objects
            for (i in 1..num_pages) {
                val itr: PageIterator = doc.getPageIterator(2 * i - 1)
                val current: com.pdftron.pdf.Page? = itr.next()
                reader.begin(current)
                val new_page: com.pdftron.pdf.Page = doc.pageCreate(current!!.getMediaBox())
                doc.pageInsert(itr, new_page)
                writer.begin(new_page)
                // Read page contents
                while (true) {
                    element = reader.next()
                    if (element == null) {
                        break
                    }
                    //if (element.getType() == Element.e_path)
                    writer.writeElement(element)
                }
                writer.end()
                reader.end()
            }
            doc.save(Utils.createExternalFile("doc_memory_edit.pdf", mFileList).getAbsolutePath(), SDFDoc.SaveMode.REMOVE_UNUSED, null)

            // Save the document to a memory buffer.
            val buf: ByteArray = doc.save(SDFDoc.SaveMode.REMOVE_UNUSED, null)
            // doc.Save(buf, buf_sz, Doc::e_linearized, NULL);

            // Write the contents of the buffer to the disk
            run({
                val outfile: java.io.File = java.io.File(Utils.createExternalFile("doc_memory_edit.txt", mFileList).getAbsolutePath())
                val fop: java.io.FileOutputStream = java.io.FileOutputStream(outfile)
                if (!outfile.exists()) {
                    outfile.createNewFile()
                }
                fop.write(buf)
                fop.flush()
                fop.close()
            })

            // Read some data from the file stored in memory
            reader.begin(doc.getPage(1))
            while (true) {
                element = reader.next()
                if (element == null) {
                    break
                }
                if (element.getType() == com.pdftron.pdf.Element.e_path) mOutputListener!!.print("Path, ")
            }
            reader.end()
            doc.close()
            mOutputListener!!.println("\n\nDone. Result saved in doc_memory_edit.pdf and doc_memory_edit.txt ...")
        } catch (e: PDFNetException) {
            mOutputListener!!.printError(e.getStackTrace())
            mOutputListener!!.printError(e.getStackTrace())
        } catch (e: java.lang.Exception) {
            mOutputListener!!.printError(e.getStackTrace())
        }
        for (file in mFileList) {
            addToFileList(file)
        }
        printFooter(outputListener)
    }

    companion object {
        private var mOutputListener: OutputListener? = null
        private val mFileList: java.util.ArrayList<String> = java.util.ArrayList<String>()
    }

    init {
        setTitle(R.string.sample_pdfdocmemory_title)
        setDescription(R.string.sample_pdfdocmemory_description)
    }
}