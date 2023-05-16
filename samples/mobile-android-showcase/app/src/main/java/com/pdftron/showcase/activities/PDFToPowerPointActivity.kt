package com.pdftron.showcase.activities

import com.pdftron.showcase.R

class PDFToPowerPointActivity : PDFToOfficeActivity() {

    override fun getTaskType(): String {
        return taskTypePPT
    }

    override fun getExtension(): String {
        return "pptx"
    }

    override fun getButtonText(): String {
        return getString(R.string.convert_pdf_to_ppt)
    }
}