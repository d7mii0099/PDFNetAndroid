package com.pdftron.showcase.activities

import android.os.Bundle
import android.view.View
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.tools.AnnotManager
import com.pdftron.pdf.widget.toolbar.component.DefaultToolbars
import kotlinx.android.synthetic.main.control_extraction.*

class XfdfExtractionActivity : TextExtractionActivity() {

    override val sFileName: String
        get() = "sample"

    override fun onCreate(savedInstanceState: Bundle?) {
        initialToolbarTag = DefaultToolbars.TAG_ANNOTATE_TOOLBAR
        super.onCreate(savedInstanceState)
    }

    override fun addControl() {
        inner_scroll_view.visibility = View.GONE
        getToolManager()!!.enableAnnotManager("pdftron", AnnotManager.AnnotationSyncingListener { action, xfdfCommand, xfdfJSON ->
            inner_scroll_view.visibility = View.VISIBLE
            extraction_text.text = "$xfdfCommand"
        })
    }

    override fun handleTabDocumentLoaded(tag: String) {
        super.handleTabDocumentLoaded(tag)

        mPdfViewCtrlTabHostFragment!!.openToolbarWithTag(DefaultToolbars.TAG_ANNOTATE_TOOLBAR)
        mPdfViewCtrlTabHostFragment!!.selectToolbarButton(DefaultToolbars.ButtonId.FREE_HIGHLIGHT)

        val pdfViewCtrl = getPDFViewCtrl()
        val pdfDoc = getPDFDoc()
        var shouldUnlockRead = false
        try {
            pdfViewCtrl!!.docLockRead()
            shouldUnlockRead = true
            // extract initial xfdf
            val annots = pdfDoc!!.fdfExtract(PDFDoc.e_annots_only)
            val xfdf = annots.saveAsXFDF()
            extraction_text.text = "$xfdf"
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            if (shouldUnlockRead) {
                pdfViewCtrl?.docUnlockRead()
            }
        }
    }
}