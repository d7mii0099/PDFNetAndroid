//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples

import android.app.Application
import android.content.Context
import com.pdftron.android.pdfnetsdksamples.samples.*
import java.util.*

class MiscellaneousSamplesApplication : Application() {

    private val mListSamples = ArrayList<PDFNetSample>()
    var context: Context? = null
        private set

    val content: List<PDFNetSample>
        get() = this.mListSamples

    override fun onCreate() {
        super.onCreate()
        instance = this
        mListSamples.add(AddImageTest())
        mListSamples.add(AnnotationTest())
        mListSamples.add(BookmarkTest())
        mListSamples.add(ContentReplacerTest())
        mListSamples.add(ConvertTest())
        mListSamples.add(DigitalSignaturesTest())
        mListSamples.add(ElementBuilderTest())
        mListSamples.add(ElementEditTest())
        mListSamples.add(ElementReaderTest())
        mListSamples.add(ElementReaderAdvTest())
        mListSamples.add(EncTest())
        mListSamples.add(FDFTest())
        mListSamples.add(ImageExtractTest())
        mListSamples.add(ImpositionTest())
        mListSamples.add(InteractiveFormsTest())
        mListSamples.add(JBIG2Test())
        mListSamples.add(LogicalStructureTest())
        mListSamples.add(OptimizerTest())
        mListSamples.add(PageLabelsTest())
        mListSamples.add(PatternTest())
        mListSamples.add(PDFATest())
        mListSamples.add(PDFDocMemoryTest())
        mListSamples.add(PDFDrawTest())
        mListSamples.add(PDFLayersTest())
        mListSamples.add(PDFPackageTest())
        mListSamples.add(PDFPageTest())
        mListSamples.add(PDFRedactTest())
        mListSamples.add(RectTest())
        mListSamples.add(SDFTest())
        mListSamples.add(StamperTest())
        mListSamples.add(TextExtractTest())
        mListSamples.add(TextSearchTest())
        mListSamples.add(U3DTest())
        mListSamples.add(UnicodeWriteTest())
        mListSamples.add(OfficeToPDFTest(applicationContext))
        mListSamples.add(UndoRedoTest())

        mListSamples.sortWith(Comparator { o1, o2 -> o1.title!!.compareTo(o2.title!!) })

        context = applicationContext
    }

    companion object {
        var instance: MiscellaneousSamplesApplication? = null
            private set
    }
}
