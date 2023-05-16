package com.pdftron.showcase.activities

import android.os.Bundle
import android.view.View
import android.widget.Switch
import com.pdftron.pdf.tools.ToolManager
import com.pdftron.pdf.widget.toolbar.component.DefaultToolbars
import com.pdftron.showcase.R
import kotlinx.android.synthetic.main.content_bottom_sheet.*


class UiCustomizationActivity : FeatureActivity() {

    private val TAG = "UiCustomizationActivity"
    val toolsMap = mapOf("TEXT_ANNOT_CREATE" to ToolManager.ToolMode.TEXT_ANNOT_CREATE, "TEXT_HIGHLIGHT" to ToolManager.ToolMode.TEXT_HIGHLIGHT,
            "TEXT_STRIKEOUT" to ToolManager.ToolMode.TEXT_STRIKEOUT, "FREE_HIGHLIGHTER" to ToolManager.ToolMode.FREE_HIGHLIGHTER,
            "INK_CREATE" to ToolManager.ToolMode.INK_CREATE, "TEXT_CREATE" to ToolManager.ToolMode.TEXT_CREATE)
    var checked = hashMapOf("TEXT_ANNOT_CREATE" to true, "TEXT_HIGHLIGHT" to true, "TEXT_STRIKEOUT" to true,
            "FREE_HIGHLIGHTER" to true, "INK_CREATE" to true, "TEXT_CREATE" to true)

    override fun onCreate(savedInstanceState: Bundle?) {
        initialToolbarTag = DefaultToolbars.TAG_ANNOTATE_TOOLBAR
        super.onCreate(savedInstanceState)
        val bottomSheetContainer = feature_content_container
        layoutInflater.inflate(R.layout.control_ui_customization, bottomSheetContainer, true)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        updateCodeSnippet()
    }

    fun onClick(view: View) {
        val tag = view.tag.toString()
        val switch = view as Switch
        if (switch.isChecked) {
            getToolManager()!!.enableToolMode(arrayOf(toolsMap[tag]!!))
            checked[tag] = true
        } else {
            getToolManager()!!.disableToolMode(arrayOf(toolsMap[tag]!!))
            checked[tag] = false
        }

        if (mPdfViewCtrlTabHostFragment!!.currentToolbarTag != initialToolbarTag) {
            mPdfViewCtrlTabHostFragment!!.openToolbarWithTag(initialToolbarTag)
        }
        updateCodeSnippet()
    }

    private fun updateCodeSnippet() {
        var enables = ""
        var disables = ""
        for (entry in checked.entries) {
            if (entry.value) {
                enables += if (enables.isEmpty()) "\n\t\t\t" + (entry.key) else ",\n\t\t\t" + (entry.key)
            } else {
                disables += if (disables.isEmpty()) "\n\t\t\t" + (entry.key) else ",\n\t\t\t" + (entry.key)
            }
        }

        var s = (if (enables.isNotEmpty()) "mToolManager" +
                ".enableToolMode(\n\tnew ToolManager.ToolMode[]{" + enables + "\n\t\t});\n" else "") +
                (if (disables.isNotEmpty()) "mToolManager" +
                ".disableToolMode(\n\tnew ToolManager.ToolMode[]{" + disables + "\n\t\t});\n" else "") +
                "mAnnotationToolbar.updateButtonsVisibility()"
        highlightCodeSnippet(s)
    }
}