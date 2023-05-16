package com.pdftron.showcase.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.pdftron.pdf.config.ToolManagerBuilder
import com.pdftron.pdf.config.ViewerConfig
import com.pdftron.pdf.tools.ToolManager
import com.pdftron.pdf.utils.CommonToast
import com.pdftron.pdf.widget.toolbar.builder.AnnotationToolbarBuilder
import com.pdftron.pdf.widget.toolbar.component.DefaultToolbars
import com.pdftron.showcase.R
import com.pdftron.showcase.helpers.AppUtils
import com.pdftron.showcase.helpers.SimpleScannerActivity
import com.pdftron.showcase.tools.barcode.BarcodeCreate
import com.pdftron.showcase.tools.barcode.BarcodeEdit

class BarcodeActivity : FeatureActivity() {

    private val BARCODE_ID = 1001
    private val BARCODE_SCANNER_ID = 1002

    private val CAMERA_PERMISSION_REQUEST = 1000
    private val SCANNER_REQUEST = 1001

    private var mHandleBarcode = false
    private var mBarcodeLink: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun getToolManagerBuilder(): ToolManagerBuilder {
        return super.getToolManagerBuilder()
                .addCustomizedTool(BarcodeCreate.MODE, BarcodeCreate::class.java)
                .addCustomizedTool(ToolManager.ToolMode.ANNOT_EDIT, BarcodeEdit::class.java)
    }

    override fun getViewerConfigBuilder(): ViewerConfig.Builder {
        val barcodeToolbarBuilder = AnnotationToolbarBuilder.withTag("Barcode")
                .addCustomSelectableButton(R.string.tool_barcode_stamp,
                        R.drawable.ic_qr_code_black_24dp, BARCODE_ID)
                .addCustomButton(R.string.tool_barcode_scanner_stamp,
                        R.drawable.ic_qr_code_scanner_black_24dp, BARCODE_SCANNER_ID)
                .addCustomStickyButton(R.string.undo, R.drawable.ic_undo_black_24dp, DefaultToolbars.ButtonId.UNDO.value())
                .addCustomStickyButton(R.string.redo, R.drawable.ic_redo_black_24dp, DefaultToolbars.ButtonId.REDO.value())
        return super.getViewerConfigBuilder()
                .addToolbarBuilder(barcodeToolbarBuilder)
    }

    override fun handleToolbarOptionsItemSelected(p0: MenuItem?): Boolean {
        if (p0?.itemId == BARCODE_ID) {
            val toolManager = getToolManager()
            val tool = toolManager!!.createTool(BarcodeCreate.MODE, null)
            toolManager.tool = tool
            return true
        } else if (p0?.itemId == BARCODE_SCANNER_ID) {
            if (!AppUtils.hasCameraPermission(this)) {
                AppUtils.requestCameraPermissions(this, CAMERA_PERMISSION_REQUEST)
            } else {
                startScannerActivity()
            }
            return true
        }
        return super.handleToolbarOptionsItemSelected(p0)
    }

    private fun startScannerActivity() {
        val intent = Intent(this, SimpleScannerActivity::class.java)
        startActivityForResult(intent, SCANNER_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SCANNER_REQUEST) {
            mHandleBarcode = true
            mBarcodeLink = data?.getStringExtra("scan_result")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (AppUtils.verifyPermissions(grantResults)) {
                startScannerActivity()
            }
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (mHandleBarcode && mBarcodeLink != null) {
            mHandleBarcode = false

            CommonToast.showText(this, R.string.tool_barcode_tap_toast)

            val toolManager = getToolManager()
            val tool = BarcodeCreate(getPDFViewCtrl()!!)
            tool.setLink(mBarcodeLink)
            toolManager!!.tool = tool
        }
    }
}