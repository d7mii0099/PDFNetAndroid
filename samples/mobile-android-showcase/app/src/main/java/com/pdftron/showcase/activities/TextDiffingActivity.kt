package com.pdftron.showcase.activities

import android.net.Uri
import android.view.View
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.controls.PdfViewCtrlTabFragment2
import com.pdftron.pdf.dialog.diffing.DiffUtils
import com.pdftron.pdf.model.BaseFileInfo
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager
import com.pdftron.pdf.utils.Utils
import com.pdftron.sdf.SDFDoc
import com.pdftron.showcase.R
import kotlinx.android.synthetic.main.content_bottomsheet_button.*
import kotlinx.coroutines.*
import org.apache.commons.io.FilenameUtils
import java.io.File

class TextDiffingActivity : DocumentTabbedViewerActivity() {

    override var files = arrayOf("text_diff_doc_1", "text_diff_doc_2")
    private var mResultFile: Uri? = null
    private val resultFile = "compare-result.pdf"

    override fun addControl() {
        button.text = getString(R.string.diff_compare)
        button.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_compare_arrows_black_24dp,
            0,
            0,
            0
        )
        button.visibility = View.GONE
        button.setOnClickListener {
            compare()
        }
    }

    override fun handleTabDocumentLoaded(tag: String) {
        if (tag.contains(resultFile)) {
            mPdfViewCtrlTabHostFragment!!.onViewModeSelected(PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_FACING_VALUE)
        }
    }

    override fun tabsAdded() {
        button.visibility = View.VISIBLE
    }

    private fun compare() {
        if (null != mResultFile) {
            mPdfViewCtrlTabHostFragment?.removeTabAt(2)
        }

        val resultFile = File(this.filesDir, resultFile)
        CoroutineScope(Job() + Dispatchers.IO).launch {

            val doc1 = DiffUtils.getPdfDoc(this@TextDiffingActivity, mFileArray!![0])
            val doc2 = DiffUtils.getPdfDoc(this@TextDiffingActivity, mFileArray!![1])
            val resultDoc = PDFDoc()
            doc1.lockRead()
            doc2.lockRead()
            resultDoc.lock()
            resultDoc.appendTextDiff(doc1, doc2)
            resultDoc.save(resultFile.absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
            doc1.unlockRead()
            doc2.unlockRead()
            resultDoc.unlock()
            resultDoc.close()
            doc1.close()
            doc2.close()

            withContext(Dispatchers.Main) {
                mResultFile = Uri.fromFile(resultFile)

                // open in viewer
                var name = FilenameUtils.getName(mResultFile!!.path)
                name = FilenameUtils.removeExtension(name)
                val tabFile = PdfViewCtrlTabFragment2.createBasicPdfViewCtrlTabBundle(
                    this@TextDiffingActivity,
                    mResultFile!!,
                    null
                )
                mPdfViewCtrlTabHostFragment!!.addTab(
                    tabFile,
                    resultFile.absolutePath,
                    Utils.capitalize(name),
                    "pdf",
                    null,
                    BaseFileInfo.FILE_TYPE_FILE
                )
                mPdfViewCtrlTabHostFragment!!.setCurrentTabByTag(resultFile.absolutePath)
            }
        }

    }

}