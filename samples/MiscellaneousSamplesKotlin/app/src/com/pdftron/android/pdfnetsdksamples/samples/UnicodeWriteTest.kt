//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------
package com.pdftron.android.pdfnetsdksamples.samples

import com.pdftron.android.pdfnetsdksamples.OutputListener
import com.pdftron.android.pdfnetsdksamples.PDFNetSample
import com.pdftron.android.pdfnetsdksamples.R
import com.pdftron.android.pdfnetsdksamples.util.Utils
import com.pdftron.pdf.ElementBuilder
import com.pdftron.pdf.ElementWriter
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.ShapedText
import com.pdftron.sdf.SDFDoc
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

/**
 * This example illustrates how to create Unicode text and how to embed
 * composite fonts.
 *
 *
 * Note: This sample assumes that your device contains some of the regular
 * fonts distributed with the Android SDK. Since not all fonts are shipped
 * depending on the manufacturer, you may need to change the sample code
 * or add a font that covers the text you want to use.
 *
 *
 * In case some of the text used in this sample does not work properly
 * (squared or dot characters appear instead of the real characters) you can
 * search for the correct fonts in the Android SDK or download and use the
 * Cyberbit font, available here:
 * http://ftp.netscape.com/pub/communicator/extras/fonts/windows/
 *
 *
 * Add the font file to the assets\TestFiles folder and change the code
 * accordingly.
 */
class UnicodeWriteTest : PDFNetSample() {
    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)
        try {
            val doc = PDFDoc()
            val eb = ElementBuilder()
            val writer = ElementWriter()

            // Start a new page ------------------------------------
            val page: com.pdftron.pdf.Page = doc.pageCreate(com.pdftron.pdf.Rect(0.0, 0.0, 612.0, 794.0))
            writer.begin(page) // begin writing to this page
            var fontLocation: String? = Utils.getAssetTempFile(INPUT_PATH.toString() + "ARIALUNI.TTF")!!.getAbsolutePath()
            var fnt: com.pdftron.pdf.Font? = null
            try {
                // Embed and subset the font
                fnt = com.pdftron.pdf.Font.createCIDTrueTypeFont(doc, fontLocation, true, true)
            } catch (e: java.lang.Exception) {
                fontLocation = "C:/Windows/Fonts/ARIALUNI.TTF"
                try {
                    fnt = com.pdftron.pdf.Font.createCIDTrueTypeFont(doc, fontLocation, true, true)
                } catch (e2: java.lang.Exception) {
                    fontLocation = null
                }
            }
            if (fnt != null) {
                mOutputListener!!.println("Note: using $fontLocation for unshaped unicode text")
            } else {
                mOutputListener!!.println("Note: using system font substitution for unshaped unicode text")
                fnt = com.pdftron.pdf.Font.create(doc, "Helvetica", "")
            }
            var element: com.pdftron.pdf.Element = eb.createTextBegin(fnt, 1.0)
            element.setTextMatrix(10.0, 0.0, 0.0, 10.0, 50.0, 600.0)
            element.getGState().setLeading(2.0) // Set the spacing between lines
            writer.writeElement(element)

            // Hello World!
            val hello = charArrayOf('H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd', '!')
            writer.writeElement(eb.createUnicodeTextRun(String(hello)))
            writer.writeElement(eb.createTextNewLine())

            // Latin
            val latin = charArrayOf(
                    'a', 'A', 'b', 'B', 'c', 'C', 'd', 'D', 0x45.toChar(), 0x0046.toChar(), 0x00C0.toChar(),
                    0x00C1.toChar(), 0x00C2.toChar(), 0x0143.toChar(), 0x0144.toChar(), 0x0145.toChar(), 0x0152.toChar(), '1', '2' // etc.
            )
            writer.writeElement(eb.createUnicodeTextRun(String(latin)))
            writer.writeElement(eb.createTextNewLine())

            // Greek
            val greek = charArrayOf(
                    0x039E.toChar(), 0x039F.toChar(), 0x03A0.toChar(), 0x03A1.toChar(), 0x03A3.toChar(), 0x03A6.toChar(), 0x03A8.toChar(), 0x03A9 // etc.
                    .toChar())
            writer.writeElement(eb.createUnicodeTextRun(String(greek)))
            writer.writeElement(eb.createTextNewLine())

            // Cyrillic
            val cyrilic = charArrayOf(
                    0x0409.toChar(), 0x040A.toChar(), 0x040B.toChar(), 0x040C.toChar(), 0x040E.toChar(), 0x040F.toChar(), 0x0410.toChar(), 0x0411.toChar(),
                    0x0412.toChar(), 0x0413.toChar(), 0x0414.toChar(), 0x0415.toChar(), 0x0416.toChar(), 0x0417.toChar(), 0x0418.toChar(), 0x0419 // etc.
                    .toChar())
            writer.writeElement(eb.createUnicodeTextRun(String(cyrilic)))
            writer.writeElement(eb.createTextNewLine())

            // Hebrew
            val hebrew = charArrayOf(
                    0x05D0.toChar(), 0x05D1.toChar(), 0x05D3.toChar(), 0x05D3.toChar(), 0x05D4.toChar(), 0x05D5.toChar(), 0x05D6.toChar(), 0x05D7.toChar(), 0x05D8.toChar(),
                    0x05D9.toChar(), 0x05DA.toChar(), 0x05DB.toChar(), 0x05DC.toChar(), 0x05DD.toChar(), 0x05DE.toChar(), 0x05DF.toChar(), 0x05E0.toChar(), 0x05E1 // etc.
                    .toChar())
            writer.writeElement(eb.createUnicodeTextRun(String(hebrew)))
            writer.writeElement(eb.createTextNewLine())

            // Arabic
            val arabic = charArrayOf(
                    0x0624.toChar(), 0x0625.toChar(), 0x0626.toChar(), 0x0627.toChar(), 0x0628.toChar(), 0x0629.toChar(), 0x062A.toChar(), 0x062B.toChar(), 0x062C.toChar(),
                    0x062D.toChar(), 0x062E.toChar(), 0x062F.toChar(), 0x0630.toChar(), 0x0631.toChar(), 0x0632.toChar(), 0x0633.toChar(), 0x0634.toChar(), 0x0635 // etc.
                    .toChar())
            writer.writeElement(eb.createUnicodeTextRun(String(arabic)))
            writer.writeElement(eb.createTextNewLine())

            // Thai
            val thai = charArrayOf(
                    0x0E01.toChar(), 0x0E02.toChar(), 0x0E03.toChar(), 0x0E04.toChar(), 0x0E05.toChar(), 0x0E06.toChar(), 0x0E07.toChar(), 0x0E08.toChar(), 0x0E09.toChar(),
                    0x0E0A.toChar(), 0x0E0B.toChar(), 0x0E0C.toChar(), 0x0E0D.toChar(), 0x0E0E.toChar(), 0x0E0F.toChar(), 0x0E10.toChar(), 0x0E11.toChar(), 0x0E12 // etc.
                    .toChar())
            writer.writeElement(eb.createUnicodeTextRun(String(thai)))
            writer.writeElement(eb.createTextNewLine())

            // Hiragana - Japanese
            val hiragana = charArrayOf(
                    0x3041.toChar(), 0x3042.toChar(), 0x3043.toChar(), 0x3044.toChar(), 0x3045.toChar(), 0x3046.toChar(), 0x3047.toChar(), 0x3048.toChar(), 0x3049.toChar(),
                    0x304A.toChar(), 0x304B.toChar(), 0x304C.toChar(), 0x304D.toChar(), 0x304E.toChar(), 0x304F.toChar(), 0x3051.toChar(), 0x3051.toChar(), 0x3052 // etc.
                    .toChar())
            writer.writeElement(eb.createUnicodeTextRun(String(hiragana)))
            writer.writeElement(eb.createTextNewLine())

            // CJK Unified Ideographs
            val cjk_uni = charArrayOf(
                    0x5841.toChar(), 0x5842.toChar(), 0x5843.toChar(), 0x5844.toChar(), 0x5845.toChar(), 0x5846.toChar(), 0x5847.toChar(), 0x5848.toChar(), 0x5849.toChar(),
                    0x584A.toChar(), 0x584B.toChar(), 0x584C.toChar(), 0x584D.toChar(), 0x584E.toChar(), 0x584F.toChar(), 0x5850.toChar(), 0x5851.toChar(), 0x5852 // etc.
                    .toChar())
            writer.writeElement(eb.createUnicodeTextRun(String(cjk_uni)))
            writer.writeElement(eb.createTextNewLine())

            // Simplified Chinese
            val chinese_simplified = charArrayOf(
                    0x4e16.toChar(), 0x754c.toChar(), 0x60a8.toChar(), 0x597d
                    .toChar())
            writer.writeElement(eb.createUnicodeTextRun(String(chinese_simplified)))
            writer.writeElement(eb.createTextNewLine())

            // Finish the block of text
            writer.writeElement(eb.createTextEnd())
            mOutputListener!!.println("Now using text shaping logic to place text")

            // Create a font in indexed encoding mode
            // normally this would mean that we are required to provide glyph indices
            // directly to CreateUnicodeTextRun, but instead, we will use the GetShapedText
            // method to take care of this detail for us.
            val indexedFont: com.pdftron.pdf.Font = com.pdftron.pdf.Font.createCIDTrueTypeFont(doc, Utils.getAssetTempFile(INPUT_PATH.toString() + "NotoSans_with_hindi.ttf")!!.getAbsolutePath(), true, true, com.pdftron.pdf.Font.e_Indices)
            element = eb.createTextBegin(indexedFont, 10.0)
            writer.writeElement(element)
            var linePos = 350.0
            val lineSpace = 20.0

            // Transform unicode text into an abstract collection of glyph indices and positioning info
            var shapedText: ShapedText = indexedFont.getShapedText("Shaped Hindi Text:")

            // transform the shaped text info into a PDF element and write it to the page
            element = eb.createShapedTextRun(shapedText)
            element.setTextMatrix(1.5, 0.0, 0.0, 1.5, 50.0, linePos)
            linePos -= lineSpace
            writer.writeElement(element)

            // read in unicode text lines from a file
            val hindiTextLines: List<String> = Files.readAllLines(Paths.get(Utils.getAssetTempFile(INPUT_PATH + "hindi_sample_utf16le.txt")!!.absolutePath), StandardCharsets.UTF_16LE)
            mOutputListener!!.println("Read in " + hindiTextLines.size + " lines of Unicode text from file")
            for (textLine in hindiTextLines) {
                shapedText = indexedFont.getShapedText(textLine)
                element = eb.createShapedTextRun(shapedText)
                element.setTextMatrix(1.5, 0.0, 0.0, 1.5, 50.0, linePos)
                linePos -= lineSpace
                writer.writeElement(element)
                mOutputListener!!.println("Wrote shaped line to page")
            }

            // Finish the shaped block of text
            writer.writeElement(eb.createTextEnd())
            writer.end() // save changes to the current page
            doc.pagePushBack(page)
            doc.save(Utils.createExternalFile("unicodewrite.pdf", mFileList).getAbsolutePath(), arrayOf<SDFDoc.SaveMode>(SDFDoc.SaveMode.REMOVE_UNUSED, SDFDoc.SaveMode.HEX_STRINGS), null)
            doc.close()
            mOutputListener!!.println("Done. Result saved in unicodewrite.pdf...")
        } catch (e: java.lang.Exception) {
            mOutputListener!!.printError(e.getStackTrace())
        }
        for (file in mFileList) {
            addToFileList(file)
        }
        printFooter(outputListener)
    }

    companion object {
        private var mOutputListener: OutputListener? = null
        private val mFileList: java.util.ArrayList<String> = java.util.ArrayList<String>()
    }

    init {
        setTitle(R.string.sample_unicodewrite_title)
        setDescription(R.string.sample_unicodewrite_description)
    }
}