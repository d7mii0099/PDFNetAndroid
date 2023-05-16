package com.pdftron.showcase.activities

import android.os.Bundle
import com.pdftron.pdf.utils.CommonToast
import com.pdftron.pdf.widget.toolbar.component.DefaultToolbars
import com.pdftron.showcase.R
import kotlinx.android.synthetic.main.content_bottom_sheet.*
import kotlinx.android.synthetic.main.content_bottomsheet_button.*

class RubberStampActivity : FeatureActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        initialToolbarTag = DefaultToolbars.TAG_INSERT_TOOLBAR
        super.onCreate(savedInstanceState)
        val bottomSheetContainer = feature_content_container
        layoutInflater.inflate(R.layout.control_button_simple, bottomSheetContainer, true)
        addControl()
    }

    private fun addControl() {
        button.text = getString(R.string.add_rubber_stamp)
        button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_annotation_stamp_black_24dp, 0, 0, 0)
        button.setOnClickListener {
            CommonToast.showText(it.context, R.string.add_rubber_stamp_guide)
            mPdfViewCtrlTabHostFragment!!.openToolbarWithTag(DefaultToolbars.TAG_INSERT_TOOLBAR)
            mPdfViewCtrlTabHostFragment!!.selectToolbarButton(DefaultToolbars.ButtonId.STAMP)
        }
    }
}