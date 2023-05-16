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
import com.pdftron.filters.FlateEncode
import com.pdftron.filters.MappedFile
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.PDFNet
import com.pdftron.sdf.PDFTronCustomSecurityHandler
import com.pdftron.sdf.SDFDoc
import com.pdftron.sdf.SecurityHandler

class EncTest : PDFNetSample() {
    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)

        // Example 1:
        // secure a document with password protection and
        // adjust permissions
        try {
            // Open the test file
            mOutputListener!!.println("Securing an existing document ...")
            val doc = PDFDoc(Utils.getAssetTempFile(INPUT_PATH.toString() + "fish.pdf")!!.getAbsolutePath())
            doc.initSecurityHandler()

            // Perform some operation on the document. In this case we use low level SDF API
            // to replace the content stream of the first page with contents of file 'my_stream.txt'
            if (true) // Optional
            {
                mOutputListener!!.println("Replacing the content stream, use flate compression...")

                // Get the page dictionary using the following path: trailer/Root/Pages/Kids/0
                val page_dict: com.pdftron.sdf.Obj = doc.getTrailer().get("Root").value()
                        .get("Pages").value()
                        .get("Kids").value()
                        .getAt(0)

                // Embed a custom stream (file mystream.txt) using Flate compression.
                val embed_file = MappedFile(Utils.getAssetTempFile(INPUT_PATH.toString() + "my_stream.txt")!!.getAbsolutePath())
                val mystm: com.pdftron.filters.FilterReader = com.pdftron.filters.FilterReader(embed_file)
                page_dict.put("Contents",
                        doc.createIndirectStream(mystm,
                                FlateEncode(null)))
            }

            //encrypt the document

            // Apply a new security handler with given security settings.
            // In order to open saved PDF you will need a user password 'test'.
            val new_handler = SecurityHandler()

            // Set a new password required to open a document
            val user_password = "test"
            new_handler.changeUserPassword(user_password)

            // Set Permissions
            new_handler.setPermission(SecurityHandler.e_print, true)
            new_handler.setPermission(SecurityHandler.e_extract_content, false)

            // Note: document takes the ownership of new_handler.
            doc.setSecurityHandler(new_handler)

            // Save the changes.
            mOutputListener!!.println("Saving modified file...")
            doc.save(Utils.createExternalFile("secured.pdf", mFileList).getAbsolutePath(), SDFDoc.SaveMode.NO_FLAGS, null)
            doc.close()
        } catch (e: PDFNetException) {
            mOutputListener!!.printError(e.getStackTrace())
        }

        // Example 2:
        // Opens the encrypted document and removes all of
        // its security.
        try {
            val doc = PDFDoc(Utils.createExternalFile("secured.pdf", mFileList).getAbsolutePath())

            //If the document is encrypted prompt for the password
            if (!doc.initSecurityHandler()) {
                var success = false
                mOutputListener!!.println("The password is: test")
                for (count in 0..2) {
                    val r: java.io.BufferedReader = java.io.BufferedReader(java.io.InputStreamReader(java.lang.System.`in`))
                    mOutputListener!!.println("A password required to open the document.")
                    mOutputListener!!.print("Please enter the password: ")
                    // String password = r.readLine();
                    if (doc.initStdSecurityHandler("test")) {
                        success = true
                        mOutputListener!!.println("The password is correct.")
                        break
                    } else if (count < 3) {
                        mOutputListener!!.println("The password is incorrect, please try again")
                    }
                }
                if (!success) {
                    mOutputListener!!.println("Document authentication error....")
                }
            }

            //remove all security on the document
            doc.removeSecurity()
            doc.save(Utils.createExternalFile("not_secured.pdf", mFileList).getAbsolutePath(), SDFDoc.SaveMode.NO_FLAGS, null)
            doc.close()
        } catch (e: java.lang.Exception) {
            mOutputListener!!.printError(e.getStackTrace())
        }

        // Example 3:
        // Encrypt/Decrypt a PDF using PDFTron custom security handler
        try {
            mOutputListener!!.println("-------------------------------------------------")
            mOutputListener!!.println("Encrypt a document using PDFTron Custom Security handler with a custom id and password...")
            val doc = PDFDoc(Utils.getAssetTempFile(INPUT_PATH.toString() + "BusinessCardTemplate.pdf")!!.getAbsolutePath())

            // Create PDFTron custom security handler with a custom id. Replace this with your own integer
            val custom_id = 123456789
            val custom_handler = PDFTronCustomSecurityHandler(custom_id)

            // Add a password to the custom security handler
            val pass = "test"
            custom_handler.changeUserPassword(pass)

            // Save the encrypted document
            doc.setSecurityHandler(custom_handler)
            doc.save(Utils.createExternalFile("BusinessCardTemplate_enc.pdf", mFileList).getAbsolutePath(), SDFDoc.SaveMode.NO_FLAGS, null)
            mOutputListener!!.println("Decrypt the PDFTron custom security encrypted document above...")
            // Register the PDFTron Custom Security handler with the same custom id used in encryption
            PDFNet.addPDFTronCustomHandler(custom_id)
            val doc_enc = PDFDoc(Utils.createExternalFile("BusinessCardTemplate_enc.pdf", mFileList).getAbsolutePath())
            doc_enc.initStdSecurityHandler(pass)
            doc_enc.removeSecurity()
            // Save the decrypted document
            doc_enc.save(Utils.createExternalFile("BusinessCardTemplate_enc_dec.pdf", mFileList).getAbsolutePath(), SDFDoc.SaveMode.NO_FLAGS, null)
            mOutputListener!!.println("Done. Result saved in BusinessCardTemplate_enc_dec.pdf")
        } catch (e: java.lang.Exception) {
            mOutputListener!!.printError(e.getStackTrace())
        }
        mOutputListener!!.println("-------------------------------------------------")
        mOutputListener!!.println("Tests completed.")
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
        setTitle(R.string.sample_encryption_title)
        setDescription(R.string.sample_encryption_description)
    }
}