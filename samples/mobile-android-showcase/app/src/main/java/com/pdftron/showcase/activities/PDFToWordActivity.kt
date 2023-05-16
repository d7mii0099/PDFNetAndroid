package com.pdftron.showcase.activities

import com.pdftron.showcase.R

class PDFToWordActivity : PDFToOfficeActivity() {

    override fun getTaskType(): String {
        return taskTypeWord
    }

    override fun getExtension(): String {
        return "docx"
    }

    override fun getButtonText(): String {
        return getString(R.string.convert_pdf_to_word)
    }
}