package com.pdftron.showcase.activities

import android.os.Bundle
import com.pdftron.pdf.controls.AnnotationToolbar
import com.pdftron.pdf.controls.PdfViewCtrlTabHostFragment2
import com.pdftron.pdf.utils.Utils
import com.pdftron.pdf.widget.toolbar.component.DefaultToolbars
import com.pdftron.showcase.R
import kotlinx.android.synthetic.main.content_bottom_sheet.*
import kotlinx.android.synthetic.main.content_bottomsheet_button.*

class AnnotationToolbarActivity : FeatureActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        initialToolbarTag = DefaultToolbars.TAG_ANNOTATE_TOOLBAR
        super.onCreate(savedInstanceState)
        val bottomSheetContainer = feature_content_container
        layoutInflater.inflate(R.layout.control_button_simple, bottomSheetContainer, true)
        addControl()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        mPdfViewCtrlTabHostFragment!!.addOnToolbarChangedListener { newToolbar ->
            if (newToolbar == DefaultToolbars.TAG_VIEW_TOOLBAR) {
                button.text = getString(R.string.open_annotation_toolbar)
            } else {
                button.text = getString(R.string.close_annotation_toolbar)
            }
        }
    }

    private fun addControl() {
        button.text = getString(R.string.close_annotation_toolbar)
        button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_mode_edit_white, 0, 0, 0)
        button.setPadding(Utils.convDp2Pix(this, 30f).toInt(),
                button.paddingTop,
                Utils.convDp2Pix(this, 30f).toInt(),
                button.paddingBottom)
        button.setOnClickListener {
            if (mPdfViewCtrlTabHostFragment!!.currentToolbarTag != null) {
                if (mPdfViewCtrlTabHostFragment!!.currentToolbarTag == DefaultToolbars.TAG_VIEW_TOOLBAR) {
                    mPdfViewCtrlTabHostFragment!!.openToolbarWithTag(DefaultToolbars.TAG_ANNOTATE_TOOLBAR)
                } else {
                    mPdfViewCtrlTabHostFragment!!.openToolbarWithTag(DefaultToolbars.TAG_VIEW_TOOLBAR)
                }
            }
        }
    }
}