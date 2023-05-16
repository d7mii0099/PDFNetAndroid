package com.pdftron.showcase.activities

import android.os.Bundle
import com.pdftron.pdf.widget.toolbar.component.DefaultToolbars
import com.pdftron.showcase.R
import kotlinx.android.synthetic.main.content_bottom_sheet.*
import kotlinx.android.synthetic.main.control_undo_redo.*

class UndoRedoActivity : FeatureActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        autoSelect = false
        initialToolbarTag = DefaultToolbars.TAG_ANNOTATE_TOOLBAR

        super.onCreate(savedInstanceState)

        val bottomSheetContainer = feature_content_container
        layoutInflater.inflate(R.layout.control_undo_redo, bottomSheetContainer, true)
        addControls()
    }

    override fun handleTabDocumentLoaded(tag: String) {
        super.handleTabDocumentLoaded(tag)
        mPdfViewCtrlTabHostFragment!!.openToolbarWithTag(DefaultToolbars.TAG_ANNOTATE_TOOLBAR)
        mPdfViewCtrlTabHostFragment!!.selectToolbarButton(DefaultToolbars.ButtonId.FREE_HIGHLIGHT)
    }

    private fun addControls() {
        undo.setOnClickListener {
            mPdfViewCtrlTabHostFragment?.currentPdfViewCtrlFragment?.undo()
        }
        redo.setOnClickListener {
            mPdfViewCtrlTabHostFragment?.currentPdfViewCtrlFragment?.redo()
        }
    }

}