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
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.PageIterator
import com.pdftron.pdf.struct.ContentItem
import com.pdftron.pdf.struct.SElement
import com.pdftron.pdf.struct.STree
import com.pdftron.sdf.SDFDoc

class LogicalStructureTest : PDFNetSample() {
    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)
        try  // Extract logical structure from a PDF document
        {
            val doc = PDFDoc(Utils.getAssetTempFile(INPUT_PATH.toString() + "tagged.pdf")!!.getAbsolutePath())
            doc.initSecurityHandler()
            mOutputListener!!.println("____________________________________________________________")
            mOutputListener!!.println("Sample 1 - Traverse logical structure tree...")
            run({
                val tree: STree = doc.getStructTree()
                if (tree.isValid()) {
                    mOutputListener!!.println("Document has a StructTree root.")
                    for (i in 0 until tree.getNumKids()) {
                        // Recursively get structure  info for all all child elements.
                        ProcessStructElement(tree.getKid(i), 0)
                    }
                } else {
                    mOutputListener!!.println("This document does not contain any logical structure.")
                }
            })
            mOutputListener!!.println("\nDone 1.")
            mOutputListener!!.println("____________________________________________________________")
            mOutputListener!!.println("Sample 2 - Get parent logical structure elements from")
            mOutputListener!!.println("layout elements.")
            run({
                val reader: com.pdftron.pdf.ElementReader = com.pdftron.pdf.ElementReader()
                val itr: PageIterator = doc.getPageIterator()
                while (itr.hasNext()) {
                    reader.begin(itr.next())
                    ProcessElements(reader)
                    reader.end()
                }
            })
            mOutputListener!!.println("\nDone 2.")
            mOutputListener!!.println("____________________________________________________________")
            mOutputListener!!.println("Sample 3 - 'XML style' extraction of PDF logical structure and page content.")
            run({

                //A map which maps page numbers(as Integers)
                //to page Maps(which map from struct mcid(as Integers) to
                //text Strings)
                val mcid_doc_map: MutableMap<Int, Map<Int, String>> = java.util.TreeMap<Int, Map<Int, String>>()
                val reader: com.pdftron.pdf.ElementReader = com.pdftron.pdf.ElementReader()
                val itr: PageIterator = doc.getPageIterator()
                while (itr.hasNext()) {
                    val current: com.pdftron.pdf.Page? = itr.next()
                    reader.begin(current)
                    val page_mcid_map: MutableMap<Int, String> = java.util.TreeMap<Int, String>()
                    mcid_doc_map.put(current!!.getIndex(), page_mcid_map)
                    ProcessElements2(reader, page_mcid_map)
                    reader.end()
                }
                val tree: STree = doc.getStructTree()
                if (tree.isValid()) {
                    for (i in 0 until tree.getNumKids()) {
                        ProcessStructElement2(tree.getKid(i), mcid_doc_map, 0)
                    }
                }
            })
            mOutputListener!!.println("\nDone 3.")
            doc.save(Utils.createExternalFile("LogicalStructure.pdf", mFileList).getAbsolutePath(), SDFDoc.SaveMode.LINEARIZED, null)
            doc.close()
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
        fun PrintIndent(indent: Int) {
            mOutputListener!!.println()
            for (i in 0 until indent) mOutputListener!!.print("  ")
        }

        // Used in code snippet 1.
        @Throws(PDFNetException::class)
        fun ProcessStructElement(element: SElement, indent: Int) {
            var indent = indent
            if (!element.isValid()) {
                return
            }

            // Print out the type and title info, if any.
            PrintIndent(indent++)
            mOutputListener!!.print("Type: " + element.getType())
            if (element.hasTitle()) {
                mOutputListener!!.print(". Title: " + element.getTitle())
            }
            val num: Int = element.getNumKids()
            for (i in 0 until num) {
                // Check is the kid is a leaf node (i.e. it is a ContentItem).
                if (element.isContentItem(i)) {
                    val cont: ContentItem = element.getAsContentItem(i)
                    val type: Int = cont.getType()
                    val page: com.pdftron.pdf.Page = cont.getPage()
                    PrintIndent(indent)
                    mOutputListener!!.print("Content Item. Part of page #" + page.getIndex())
                    PrintIndent(indent)
                    when (type) {
                        ContentItem.e_MCID, ContentItem.e_MCR -> mOutputListener!!.print("MCID: " + cont.getMCID())
                        ContentItem.e_OBJR -> {
                            mOutputListener!!.print("OBJR ")
                            val ref_obj: com.pdftron.sdf.Obj = cont.getRefObj()
                            if (ref_obj != null) mOutputListener!!.print("- Referenced Object#: " + ref_obj.getObjNum())
                        }
                        else -> {
                        }
                    }
                } else {  // the kid is another StructElement node.
                    ProcessStructElement(element.getAsStructElem(i), indent)
                }
            }
        }

        // Used in code snippet 2.
        @Throws(PDFNetException::class)
        fun ProcessElements(reader: com.pdftron.pdf.ElementReader) {
            var element: com.pdftron.pdf.Element?
            // Read page contents
            while (true) {
                element = reader.next()
                if (element == null) {
                    break
                }
                // In this sample we process only paths & text, but the code can be
                // extended to handle any element type.
                val type: Int = element.getType()
                if (type == com.pdftron.pdf.Element.e_path || type == com.pdftron.pdf.Element.e_text || type == com.pdftron.pdf.Element.e_path) {
                    when (type) {
                        com.pdftron.pdf.Element.e_path -> mOutputListener!!.print("\nPATH: ")
                        com.pdftron.pdf.Element.e_text -> mOutputListener!!.print("""
    
    TEXT: ${element.getTextString()}
    
    """.trimIndent())
                        com.pdftron.pdf.Element.e_form -> mOutputListener!!.print("\nFORM XObject: ")
                    }

                    // Check if the element is associated with any structural element.
                    // Content items are leaf nodes of the structure tree.
                    val struct_parent: SElement = element.getParentStructElement()
                    if (struct_parent.isValid()) {
                        // Print out the parent structural element's type, title, and object number.
                        mOutputListener!!.print(" Type: " + struct_parent.getType()
                                + ", MCID: " + element.getStructMCID())
                        if (struct_parent.hasTitle()) {
                            mOutputListener!!.print(". Title: " + struct_parent.getTitle())
                        }
                        mOutputListener!!.print(", Obj#: " + struct_parent.getSDFObj().getObjNum())
                    }
                }
            }
        }

        // Used in code snippet 3.
        //typedef map<int, string> MCIDPageMap;
        //typedef map<int, MCIDPageMap> MCIDDocMap;
        // Used in code snippet 3.
        @Throws(PDFNetException::class)
        fun ProcessElements2(reader: com.pdftron.pdf.ElementReader, mcid_page_map: MutableMap<Int, String>) {
            var element: com.pdftron.pdf.Element?
            // Read page contents
            while (true) {
                element = reader.next()
                if (element == null) {
                    break
                }
                // In this sample we process only text, but the code can be extended
                // to handle paths, images, or any other Element type.
                val mcid: Int = element.getStructMCID()
                if (mcid >= 0 && element.getType() == com.pdftron.pdf.Element.e_text) {
                    val `val`: String = element.getTextString()
                    if (mcid_page_map.containsKey(mcid)) mcid_page_map.put(mcid, mcid_page_map[mcid] + `val`) else mcid_page_map.put(mcid, `val`)
                }
            }
        }

        // Used in code snippet 3.
        @Throws(PDFNetException::class)
        fun ProcessStructElement2(element: SElement, mcid_doc_map: Map<Int, Map<Int, String>>, indent: Int) {
            if (!element.isValid()) {
                return
            }

            // Print out the type and title info, if any.
            PrintIndent(indent)
            mOutputListener!!.print("<" + element.getType())
            if (element.hasTitle()) {
                mOutputListener!!.print(" title=\"" + element.getTitle() + "\"")
            }
            mOutputListener!!.print(">")
            val num: Int = element.getNumKids()
            for (i in 0 until num) {
                if (element.isContentItem(i)) {
                    val cont: ContentItem = element.getAsContentItem(i)
                    if (cont.getType() == ContentItem.e_MCID) {
                        val page_num: Int = cont.getPage().getIndex()
                        if (mcid_doc_map.containsKey(page_num)) {
                            val mcid_page_map = mcid_doc_map[page_num]!!
                            val mcid_key: Int = cont.getMCID()
                            if (mcid_page_map.containsKey(mcid_key)) {
                                mOutputListener!!.print(mcid_page_map[mcid_key])
                            }
                        }
                    }
                } else {  // the kid is another StructElement node.
                    ProcessStructElement2(element.getAsStructElem(i), mcid_doc_map, indent + 1)
                }
            }
            PrintIndent(indent)
            mOutputListener!!.print("</" + element.getType() + ">")
        }
        /**
         * @param args
         */
    }

    init {
        setTitle(R.string.sample_logicalstructure_title)
        setDescription(R.string.sample_logicalstructure_description)
    }
}