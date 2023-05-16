//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples;

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;
import com.pdftron.common.Matrix2D;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFDraw;
import com.pdftron.pdf.PDFRasterizer;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageIterator;
import com.pdftron.pdf.Rect;
import com.pdftron.sdf.Obj;
import com.pdftron.sdf.ObjSet;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

public class PDFDrawTest extends PDFNetSample {

	private static OutputListener mOutputListener;

	private static ArrayList<String> mFileList = new ArrayList<>();

    public PDFDrawTest() {
        setTitle(R.string.sample_pdfdraw_title);
        setDescription(R.string.sample_pdfdraw_description);
    }

	@Override
	public void run(OutputListener outputListener) {
		super.run(outputListener);
		mOutputListener = outputListener;
		mFileList.clear();
		printHeader(outputListener);
        try {
            // The first step in every application using PDFNet is to initialize the
            // library and set the path to common PDF resources. The library is usually
            // initialized only once, but calling Initialize() multiple times is also fine.

            // Optional: Set ICC color profiles to fine tune color conversion
            // for PDF 'device' color spaces...

            //PDFNet.setResourcesPath("../../../resources");
            //PDFNet.setColorManagement();
            //PDFNet.setDefaultDeviceCMYKProfile("D:/Misc/ICC/USWebCoatedSWOP.icc");
            //PDFNet.setDefaultDeviceRGBProfile("AdobeRGB1998.icc"); // will search in PDFNet resource folder.

            // ----------------------------------------------------
            // Optional: Set predefined font mappings to override default font
            // substitution for documents with missing fonts...

            // PDFNet.addFontSubst("StoneSans-Semibold", "C:/WINDOWS/Fonts/comic.ttf");
            // PDFNet.addFontSubst("StoneSans", "comic.ttf");  // search for 'comic.ttf' in PDFNet resource folder.
            // PDFNet.addFontSubst(PDFNet.e_Identity, "C:/WINDOWS/Fonts/arialuni.ttf");
            // PDFNet.addFontSubst(PDFNet.e_Japan1, "C:/Program Files/Adobe/Acrobat 7.0/Resource/CIDFont/KozMinProVI-Regular.otf");
            // PDFNet.addFontSubst(PDFNet.e_Japan2, "c:/myfonts/KozMinProVI-Regular.otf");
            // PDFNet.addFontSubst(PDFNet.e_Korea1, "AdobeMyungjoStd-Medium.otf");
            // PDFNet.addFontSubst(PDFNet.e_CNS1, "AdobeSongStd-Light.otf");
            // PDFNet.addFontSubst(PDFNet.e_GB1, "AdobeMingStd-Light.otf");

            PDFDraw draw = new PDFDraw();  // PDFDraw class is used to rasterize PDF pages.
            ObjSet hint_set = new ObjSet();

            //--------------------------------------------------------------------------------
            // Example 1) Convert the first page to PNG and TIFF at 92 DPI.
            // A three step tutorial to convert PDF page to an image.
            try {
                // A) Open the PDF document.
                PDFDoc doc = new PDFDoc((Utils.getAssetTempFile(INPUT_PATH + "tiger.pdf").getAbsolutePath()));

                // Initialize the security handler, in case the PDF is encrypted.
                doc.initSecurityHandler();

                // B) The output resolution is set to 92 DPI.
                draw.setDPI(92);

                // C) Rasterize the first page in the document and save the result as PNG.
                Page pg = doc.getPage(1);
                draw.export(pg, (Utils.createExternalFile("tiger_92dpi.png", mFileList).getAbsolutePath()));
                // output "tiger_92dpi.png"

                mOutputListener.println("Example 1: tiger_92dpi.png");

                // Export the same page as TIFF
                draw.export(pg, (Utils.createExternalFile("tiger_92dpi.tif", mFileList).getAbsolutePath()), "TIFF");
                // output "tiger_92dpi.tif"
                doc.close();
            } catch (Exception e) {
                mOutputListener.printError(e.getStackTrace());
            }

            //--------------------------------------------------------------------------------
            // Example 2) Convert the all pages in a given document to JPEG at 72 DPI.
            try {
                mOutputListener.println("Example 2:");
                PDFDoc doc = new PDFDoc((Utils.getAssetTempFile(INPUT_PATH + "newsletter.pdf").getAbsolutePath()));
                // Initialize the security handler, in case the PDF is encrypted.
                doc.initSecurityHandler();

                draw.setDPI(72); // Set the output resolution is to 72 DPI.

                // Use optional encoder parameter to specify JPEG quality.
                Obj encoder_param = hint_set.createDict();
                encoder_param.putNumber("Quality", 80);

                // Traverse all pages in the document.
                for (PageIterator itr = doc.getPageIterator(); itr.hasNext(); ) {
                    Page current = itr.next();
                    String filename = "newsletter" + current.getIndex() + ".jpg";
                    mOutputListener.println(filename);
                    draw.export(current, Utils.createExternalFile(filename, mFileList).getAbsolutePath(), "JPEG", encoder_param);
                }

                doc.close();
                mOutputListener.println("Done.");
            } catch (Exception e) {
                mOutputListener.printError(e.getStackTrace());
            }

            FileOutputStream fos = null;
            // Examples 3-5
            try {
                // Common code for remaining samples.
                PDFDoc tiger_doc = new PDFDoc((Utils.getAssetTempFile(INPUT_PATH + "tiger.pdf").getAbsolutePath()));
                // Initialize the security handler, in case the PDF is encrypted.
                tiger_doc.initSecurityHandler();
                Page page = tiger_doc.getPageIterator().next();

                //--------------------------------------------------------------------------------
                // Example 3) Convert the first page to raw bitmap. Also, rotate the
                // page 90 degrees and save the result as RAW.
                draw.setDPI(100); // Set the output resolution is to 100 DPI.
                draw.setRotate(Page.e_90);  // Rotate all pages 90 degrees clockwise.

                // create a Java image
                android.graphics.Bitmap image = draw.getBitmap(page);

                //
                int width = image.getWidth(), height = image.getHeight();
                int[] arr = new int[width * height];
                image.getPixels(arr, 0, width, 0, 0, width, height);
                // pg.grabPixels();

                // convert to byte array
                ByteBuffer byteBuffer = ByteBuffer.allocate(arr.length * 4);
                IntBuffer intBuffer = byteBuffer.asIntBuffer();
                intBuffer.put(arr);
                byte[] rawByteArray = byteBuffer.array();
                // finally write the file
                fos = new FileOutputStream(Utils.createExternalFile("tiger_100dpi_rot90.raw", mFileList).getAbsolutePath());
                fos.write(rawByteArray);
                mOutputListener.println("Example 3: tiger_100dpi_rot90.raw");

                draw.setRotate(Page.e_0);  // Disable image rotation for remaining samples.

                //--------------------------------------------------------------------------------
                // Example 4) Convert PDF page to a fixed image size. Also illustrates some
                // other features in PDFDraw class such as rotation, image stretching, exporting
                // to grayscale, or monochrome.

                // Initialize render 'gray_hint' parameter, that is used to control the
                // rendering process. In this case we tell the rasterizer to export the image as
                // 1 Bit Per Component (BPC) image.
                Obj mono_hint = hint_set.createDict();
                mono_hint.putNumber("BPC", 1);

                // SetImageSize can be used instead of SetDPI() to adjust page  scaling
                // dynamically so that given image fits into a buffer of given dimensions.
                draw.setImageSize(1000, 1000);        // Set the output image to be 1000 wide and 1000 pixels tall

                draw.export(page, (Utils.createExternalFile("tiger_1000x1000.png", mFileList).getAbsolutePath()), "PNG", mono_hint);
                mOutputListener.println("Example 4: tiger_1000x1000.png");

                draw.setImageSize(200, 400); // Set the output image to be 200 wide and 300 pixels tall
                draw.setRotate(Page.e_180); // Rotate all pages 90 degrees clockwise.

                // 'gray_hint' tells the rasterizer to export the image as grayscale.
                Obj gray_hint = hint_set.createDict();
                gray_hint.putName("ColorSpace", "Gray");

                draw.export(page, (Utils.createExternalFile("tiger_200x400_rot180.png", mFileList).getAbsolutePath()), "PNG", gray_hint);
                mOutputListener.println("Example 4: tiger_200x400_rot180.png");

                draw.setImageSize(400, 200, false);  // The third parameter sets 'preserve-aspect-ratio' to false.
                draw.setRotate(Page.e_0);    // Disable image rotation.
                draw.export(page, (Utils.createExternalFile("tiger_400x200_stretch.jpg", mFileList).getAbsolutePath()), "JPEG");
                // output "tiger_400x200_stretch.jpg"
                mOutputListener.println("Example 4: tiger_400x200_stretch.jpg");

                //--------------------------------------------------------------------------------
                // Example 5) Zoom into a specific region of the page and rasterize the
                // area at 200 DPI and as a thumbnail (i.e. a 50x50 pixel image).
                Rect zoom_rect = new Rect(216, 522, 330, 600);
                page.setCropBox(zoom_rect);    // Set the page crop box.

                // Select the crop region to be used for drawing.
                draw.setPageBox(Page.e_crop);
                draw.setDPI(900);  // Set the output image resolution to 900 DPI.
                draw.export(page, (Utils.createExternalFile("tiger_zoom_900dpi.png", mFileList).getAbsolutePath()), "PNG");
                // output "tiger_zoom_900dpi.png"
                mOutputListener.println("Example 5: tiger_zoom_900dpi.png");

                // -------------------------------------------------------------------------------
                // Example 6)
                draw.setImageSize(50, 50);       // Set the thumbnail to be 50x50 pixel image.
                draw.export(page, (Utils.createExternalFile("tiger_zoom_50x50.png", mFileList).getAbsolutePath()), "PNG");
                // output "tiger_zoom_50x50.png"
                mOutputListener.println("Example 6: tiger_zoom_50x50.png");

                tiger_doc.close();
            } catch (Exception e) {
                mOutputListener.printError(e.getStackTrace());
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (Exception ignored) {
                    }
                }
            }

            Obj cmyk_hint = hint_set.createDict();
            cmyk_hint.putName("ColorSpace", "CMYK");

            //--------------------------------------------------------------------------------
            // Example 7) Convert the first PDF page to CMYK TIFF at 92 DPI.
            // A three step tutorial to convert PDF page to an image
            try {
                // A) Open the PDF document.
                PDFDoc doc = new PDFDoc(Utils.getAssetTempFile(INPUT_PATH + "tiger.pdf").getAbsolutePath());
                // Initialize the security handler, in case the PDF is encrypted.
                doc.initSecurityHandler();

                // B) The output resolution is set to 92 DPI.
                draw.setDPI(92);

                // C) Rasterize the first page in the document and save the result as TIFF.
                Page pg = doc.getPage(1);
                draw.export(pg, Utils.createExternalFile("out1.tif", mFileList).getAbsolutePath(), "TIFF", cmyk_hint);
                // output "out1.tif"
                mOutputListener.println("Example 7: out1.tif");
                doc.close();
            } catch (Exception e) {
                mOutputListener.printError(e.getStackTrace());
            }

            //--------------------------------------------------------------------------------
            // Example 8) PDFRasterizer can be used for more complex rendering tasks, such as 
            // strip by strip or tiled document rendering. In particular, it is useful for 
            // cases where you cannot simply modify the page crop box (interactive viewing,
            // parallel rendering).  This example shows how you can rasterize the south-west
            // quadrant of a page.
            try {
                // A) Open the PDF document.
                PDFDoc doc = new PDFDoc(Utils.getAssetTempFile(INPUT_PATH + "tiger.pdf").getAbsolutePath());
                // Initialize the security handler, in case the PDF is encrypted.
                doc.initSecurityHandler();

                // B) Get the page matrix 
                Page pg = doc.getPage(1);
                int box = Page.e_crop;
                Matrix2D mtx = pg.getDefaultMatrix(true, box, 0);
                // We want to render a quadrant, so use half of width and height
                double pg_w = pg.getPageWidth(box) / 2;
                double pg_h = pg.getPageHeight(box) / 2;

                // C) Scale matrix from PDF space to buffer space
                double dpi = 96.0;
                double scale = dpi / 72.0; // PDF space is 72 dpi
                double buf_w = Math.floor(scale * pg_w);
                double buf_h = Math.floor(scale * pg_h);
                int bytes_per_pixel = 4; // BGRA buffer
                mtx.translate(0, -pg_h); // translate by '-pg_h' since we want south-west quadrant
                mtx = (new Matrix2D(scale, 0, 0, scale, 0, 0)).multiply(mtx);

                // D) Rasterize page into memory buffer, according to our parameters
                PDFRasterizer rast = new PDFRasterizer();
                byte[] buf = rast.rasterize(pg, (int) buf_w, (int) buf_h, (int) buf_w * bytes_per_pixel, bytes_per_pixel, true, mtx, null);

                mOutputListener.println("Example 8: Successfully rasterized into memory buffer.");
                doc.close();
            } catch (Exception e) {
                mOutputListener.printError(e.getStackTrace());
            }

            //--------------------------------------------------------------------------------
            // Example 9) Export raster content to PNG using different image smoothing settings.
            try {
                PDFDoc text_doc = new PDFDoc(Utils.getAssetTempFile(INPUT_PATH + "lorem_ipsum.pdf").getAbsolutePath());
                text_doc.initSecurityHandler();

                draw.setImageSmoothing(false, false);
                String filename = "raster_text_no_smoothing.png";
                draw.export(text_doc.getPageIterator().next(), Utils.createExternalFile(filename, mFileList).getAbsolutePath());
                mOutputListener.println("Example 9 a): " + filename + ". Done.");

                filename = "raster_text_smoothed.png";
                draw.setImageSmoothing(true, false /*default quality bilinear resampling*/);
                draw.export(text_doc.getPageIterator().next(), Utils.createExternalFile(filename, mFileList).getAbsolutePath());
                mOutputListener.println("Example 9 b): " + filename + ". Done.");

                filename = "raster_text_high_quality.png";
                draw.setImageSmoothing(true, true /*high quality area resampling*/);
                draw.export(text_doc.getPageIterator().next(), Utils.createExternalFile(filename, mFileList).getAbsolutePath());
                mOutputListener.println("Example 9 c): " + filename + ". Done.");
            } catch (Exception e) {
                mOutputListener.printError(e.getStackTrace());
            }

            //--------------------------------------------------------------------------------
            // Example 10) Export separations directly, without conversion to an output colorspace
            try {
                PDFDoc separation_doc = new PDFDoc(Utils.getAssetTempFile(INPUT_PATH + "op_blend_test.pdf").getAbsolutePath());
                separation_doc.initSecurityHandler();

                Obj separation_hint = hint_set.createDict();
                separation_hint.putName("ColorSpace", "Separation");
                draw.setDPI(96);
                draw.setImageSmoothing(true, true);
                // set overprint preview to always on
                draw.setOverprint(1);

                String filename = new String("merged_separations.png");
                draw.export(separation_doc.getPage(1), Utils.createExternalFile(filename, mFileList).getAbsolutePath(), "PNG");
                mOutputListener.println("Example 10 a): " + filename + ". Done.");

                filename = new String("separation");
                draw.export(separation_doc.getPage(1), Utils.createExternalFile(filename, mFileList).getAbsolutePath(), "PNG", separation_hint);
                mOutputListener.println("Example 10 b): " + filename + "_[ink].png. Done.");

                filename = new String("separation_NChannel.tif");
                draw.export(separation_doc.getPage(1), Utils.createExternalFile(filename, mFileList).getAbsolutePath(), "TIFF", separation_hint);
                mOutputListener.println("Example 10 c): " + filename + ". Done.");
            } catch (Exception e) {
                mOutputListener.printError(e.getStackTrace());
            }

            // Calling Terminate when PDFNet is no longer in use is a good practice, but
            // is not required.
        } catch (Exception e) {
            mOutputListener.printError(e.getStackTrace());
        }

		for (String file : mFileList) {
			addToFileList(file);
		}
		printFooter(outputListener);
	}

}