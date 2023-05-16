package com.pdftron.completereader

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.pdftron.demo.app.SimpleReaderActivity
import com.pdftron.pdf.config.PDFViewCtrlConfig
import com.pdftron.pdf.config.ToolManagerBuilder
import com.pdftron.pdf.config.ViewerConfig
import com.pdftron.pdf.controls.DiffActivity
import com.pdftron.pdf.controls.DocumentActivity
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager
import com.pdftron.pdf.utils.Utils

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PdfViewCtrlSettingsManager.setFollowSystemDarkMode(this, false)

        val newUiView = findViewById<View>(R.id.simpleReaderLayoutNew)
        val simpleReaderButtonNew = newUiView.findViewById<Button>(R.id.simpleReaderButton)
        simpleReaderButtonNew.setOnClickListener { openSimpleReaderActivity(true) }
        val simpleReaderImageNew = newUiView.findViewById<ImageView>(R.id.simpleReaderImage)
        simpleReaderImageNew.setOnClickListener { openSimpleReaderActivity(true) }
        val diffButton = findViewById<Button>(R.id.diffButton)
        diffButton.setOnClickListener { openDiffActivity() }
        val diffImage = findViewById<ImageView>(R.id.diffImage)
        diffImage.setOnClickListener { openDiffActivity() }
    }

    private fun openDiffActivity() {
        DiffActivity.open(this, R.raw.diff_doc_1, R.raw.diff_doc_2)
    }

    private fun openSimpleReaderActivity(newUi: Boolean) {
        PdfViewCtrlSettingsManager.setFollowSystemDarkMode(
            this,
            false
        ) // for better dark mode experience in demo
        val tmBuilder = ToolManagerBuilder.from()
            .setUseDigitalSignature(false)
            .setAutoResizeFreeText(false)
        var cutoutMode = 0
        if (Utils.isPie()) {
            cutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        var builder = ViewerConfig.Builder()
        builder = builder
            .useCompactViewer(false)
            .fullscreenModeEnabled(true)
            .multiTabEnabled(true)
            .documentEditingEnabled(true)
            .longPressQuickMenuEnabled(true)
            .showPageNumberIndicator(true)
            .permanentPageNumberIndicator(false)
            .pageStackEnabled(true)
            .hideToolbars(arrayOf())
            .showDocumentSlider(true)
            .showTopToolbar(true)
            .showBottomToolbar(true)
            .permanentToolbars(false)
            .showThumbnailView(true)
            .showBookmarksView(true)
            .toolbarTitle("")
            .showSearchView(true)
            .showShareOption(true)
            .showDocumentSettingsOption(true)
            .showAnnotationToolbarOption(true)
            .showFormToolbarOption(true)
            .showFillAndSignToolbarOption(true)
            .showReflowOption(true)
            .showEditMenuOption(true)
            .showViewLayersToolbarOption(true)
            .showOpenFileOption(true)
            .showOpenUrlOption(true)
            .showEditPagesOption(true)
            .showPrintOption(true)
            .showSaveCopyOption(true)
            .hideThumbnailFilterModes(arrayOf())
            .hideViewModeItems(arrayOf())
            .hideSaveCopyOptions(intArrayOf())
            .showCloseTabOption(true)
            .showAnnotationsList(true)
            .showOutlineList(true)
            .showUserBookmarksList(true)
            .navigationListAsSheetOnLargeDevice(true)
            .rightToLeftModeEnabled(false)
            .showRightToLeftOption(false)
            .openUrlCachePath(this.cacheDir.absolutePath)
            .saveCopyExportPath(this.cacheDir.absolutePath)
            .thumbnailViewEditingEnabled(true)
            .userBookmarksListEditingEnabled(true)
            .quickBookmarkCreation(false)
            .annotationsListEditingEnabled(true)
            .outlineListEditingEnabled(true)
            .useStandardLibrary(false)
            .showToolbarSwitcher(true)
            .imageInReflowEnabled(true)
            .pdfViewCtrlConfig(PDFViewCtrlConfig.getDefaultConfig(this))
            .toolManagerBuilder(tmBuilder)
        if (Utils.isPie()) {
            builder = builder.layoutInDisplayCutoutMode(cutoutMode)
        }
        val config = builder.build()
        val intent: Intent =
            DocumentActivity.IntentBuilder.fromActivityClass(this, SimpleReaderActivity::class.java)
                .usingConfig(config)
                .usingNewUi(newUi)
                .build()
        startActivity(intent)
    }
}