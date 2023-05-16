package com.pdftron.showcase.activities

import android.os.Bundle
import android.view.MenuItem
import com.pdftron.pdf.config.ViewerConfig
import com.pdftron.pdf.utils.CommonToast
import com.pdftron.pdf.widget.bottombar.builder.BottomBarBuilder
import com.pdftron.pdf.widget.toolbar.builder.AnnotationToolbarBuilder
import com.pdftron.pdf.widget.toolbar.builder.ToolbarButtonType
import com.pdftron.pdf.widget.toolbar.component.DefaultToolbars
import com.pdftron.showcase.R

class ToolbarBuilderActivity : FeatureActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun getViewerConfigBuilder(): ViewerConfig.Builder {
        val annotToolbarBuilder = AnnotationToolbarBuilder.withTag("MyCustomToolbar")
                .addToolButton(ToolbarButtonType.TEXT_HIGHLIGHT, DefaultToolbars.ButtonId.TEXT_HIGHLIGHT.value())
                .addToolButton(ToolbarButtonType.POLY_CLOUD, DefaultToolbars.ButtonId.POLY_CLOUD.value())
                .addToolButton(ToolbarButtonType.DATE, DefaultToolbars.ButtonId.DATE.value())
                .addToolButton(ToolbarButtonType.INK, DefaultToolbars.ButtonId.INK.value())
                .addToolButton(ToolbarButtonType.ERASER, DefaultToolbars.ButtonId.ERASER.value())
                .addToolButton(ToolbarButtonType.CHECKMARK, DefaultToolbars.ButtonId.CHECKMARK.value())
                .addToolButton(ToolbarButtonType.SIGNATURE, DefaultToolbars.ButtonId.SIGNATURE.value())
                .addToolButton(ToolbarButtonType.LASSO_SELECT, DefaultToolbars.ButtonId.LASSO_SELECT.value())
                .addToolButton(ToolbarButtonType.EDIT_TOOLBAR, DefaultToolbars.ButtonId.CUSTOMIZE.value())
                .addToolStickyButton(ToolbarButtonType.UNDO, DefaultToolbars.ButtonId.UNDO.value())
                .addCustomStickyButton(R.string.favorites, R.drawable.ic_star_white_24dp, 1000)

        val bottomBarBuilder = BottomBarBuilder.withTag("MyCustomBottomBar")
                .addCustomButton(R.string.pref_viewmode_title, R.drawable.ic_viewing_mode_white_24dp, R.id.bottom_bar_view_mode)
                .addCustomButton(R.string.pref_viewmode_reflow, R.drawable.ic_view_mode_reflow_black_24dp, R.id.bottom_bar_reflow)
                .addCustomButton(R.string.dialog_add_page_title, R.drawable.ic_add_blank_page_white, R.id.bottom_bar_addpage);

        return super.getViewerConfigBuilder()
                .toolbarTitle("My Toolbar")
                .showToolbarSwitcher(false)
                .bottomBarBuilder(bottomBarBuilder)
                .addToolbarBuilder(annotToolbarBuilder)
    }

    override fun handleToolbarOptionsItemSelected(p0: MenuItem?): Boolean {
        when (p0?.itemId) {
            1000 -> {
                CommonToast.showText(this, "Star clicked!")
                return true
            }
            R.id.bottom_bar_view_mode -> {
                mPdfViewCtrlTabHostFragment?.onViewModeOptionSelected()
                return true
            }
            R.id.bottom_bar_reflow -> {
                mPdfViewCtrlTabHostFragment?.onToggleReflow()
                return true
            }
            R.id.bottom_bar_addpage -> {
                mPdfViewCtrlTabHostFragment?.addNewPage()
                return true
            }
        }
        return super.handleToolbarOptionsItemSelected(p0)
    }

}