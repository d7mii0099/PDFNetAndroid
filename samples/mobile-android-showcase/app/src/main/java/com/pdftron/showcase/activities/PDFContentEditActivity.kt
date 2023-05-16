package com.pdftron.showcase.activities

import com.pdftron.pdf.config.ToolManagerBuilder

class PDFContentEditActivity : FeatureActivity() {
    override fun getToolManagerBuilder(): ToolManagerBuilder {
        return super.getToolManagerBuilder().setPdfContentEditingEnabled(true)
    }
}