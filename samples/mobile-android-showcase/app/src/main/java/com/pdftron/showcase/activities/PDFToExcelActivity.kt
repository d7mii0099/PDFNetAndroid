package com.pdftron.showcase.activities

import android.os.Bundle
import com.pdftron.showcase.R

class PDFToExcelActivity : PDFToOfficeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        sampleFileName = "sample_table"
        super.onCreate(savedInstanceState)
    }

    override fun getTaskType(): String {
        return taskTypeExcel
    }

    override fun getExtension(): String {
        return "xlsx"
    }

    override fun getButtonText(): String {
        return getString(R.string.convert_pdf_to_excel)
    }
}