//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.net.Uri;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.common.PDFNetException;
import com.pdftron.filters.SecondaryFileFilter;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.Annot.BorderStyle;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.ColorSpace;
import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementBuilder;
import com.pdftron.pdf.ElementWriter;
import com.pdftron.pdf.GState;
import com.pdftron.pdf.Image;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFDraw;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageSet;
import com.pdftron.pdf.PathData;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.FreeText;
import com.pdftron.pdf.model.FreeTextInfo;
import com.pdftron.sdf.Obj;
import com.pdftron.sdf.ObjSet;
import com.pdftron.sdf.SDFDoc;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;

/**
 * Singleton class to manage stamp signatures.
 */
public class StampManager {

    private StampManager() {
    }

    private static class LazyHolder {
        private static final StampManager INSTANCE = new StampManager();
    }

    private SignatureListener mSignatureListener;

    public void setSignatureListener(SignatureListener listener) {
        mSignatureListener = listener;
    }

    private boolean hasSignatureListener() {
        return mSignatureListener != null;
    }

    public interface SignatureListener {
        void onSavedSignatureDeleted();

        void onSavedSignatureCreated();
    }

    public static StampManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static String SIGNATURE_FILE_NAME_LEGACY = "SignatureFile.CompleteReader";
    private static String SIGNATURE_FILE_NAME = "_pdftron_Signature.pdf";
    private static String SIGNATURE_FOLDER_NAME = "_pdftron_Signature";
    private static String SIGNATURE_JPG_FOLDER_NAME = "_pdftron_SignatureJPG";

    private static int PAGE_BUFFER = 20;

    private String mDefaultSignatureFilename;

    private String mDelayRemoveSignatureFilePath;

    private PDFDoc getStampDoc(File file) {
        PDFDoc pdfDoc = null;
        try {
            if (file.exists()) {
                pdfDoc = new PDFDoc(file.getAbsolutePath());
            } else {
                pdfDoc = new PDFDoc();
            }
        } catch (PDFNetException e) {
        }
        return pdfDoc;
    }

    @Deprecated
    private File getStampFile(Context context) {
        if (mDefaultSignatureFilename == null) {
            return new File(context.getFilesDir().getAbsolutePath() + "/" + SIGNATURE_FILE_NAME_LEGACY);
        }
        return new File(mDefaultSignatureFilename);
    }

    public void savedSignatureCreated() {
        if (hasSignatureListener()) {
            mSignatureListener.onSavedSignatureCreated();
        }
    }

    public void savedSignatureDeleted() {
        if (hasSignatureListener()) {
            mSignatureListener.onSavedSignatureDeleted();
        }
    }

    public File getSavedSignatureJpgFolder(Context context) {
        File signatureFolder = new File(context.getFilesDir().getAbsolutePath() + "/" + SIGNATURE_JPG_FOLDER_NAME);
        if (!signatureFolder.exists()) {
            boolean success = signatureFolder.mkdir();
            if (!success) {
                return null;
            }
        }
        if (!signatureFolder.isDirectory()) {
            return null;
        }
        return signatureFolder;
    }

    public File getSavedSignatureFolder(Context context) {
        File signatureFolder = new File(context.getFilesDir().getAbsolutePath() + "/" + SIGNATURE_FOLDER_NAME);
        if (!signatureFolder.exists()) {
            boolean success = signatureFolder.mkdir();
            if (!success) {
                return null;
            }
        }
        if (!signatureFolder.isDirectory()) {
            return null;
        }
        return signatureFolder;
    }

    public File[] getSavedSignatures(Context context) {
        File signatureFolder = getSavedSignatureFolder(context);
        if (null == signatureFolder || context == null) {
            return null;
        }

        File[] files = signatureFolder.listFiles();
        if (files == null || files.length == 0) {
            importDefaultSignature(context, signatureFolder);
        }

        return signatureFolder.listFiles();
    }

    // for backwards compatibility
    private boolean importDefaultSignature(@NonNull Context context, File signatureFolder) {
        File src = getStampFile(context);
        File dest = new File(signatureFolder, SIGNATURE_FILE_NAME);
        try {
            if (src.exists()) {
                FileUtils.copyFile(src, dest);
                return true;
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
        return false;
    }

    // Saves the signature image to the app sandbox directory vis context.getFilesDir()
    @Nullable
    public File getSavedSignatureJpegFile(Context context, File signatureFile) {
        if (context == null || signatureFile == null || !signatureFile.exists()) {
            return null;
        }
        PDFDraw pdfDraw = null;
        try {
            File signatureJpgFolder = getSavedSignatureJpgFolder(context);
            String filename = FilenameUtils.getName(signatureFile.getAbsolutePath());
            String sigTempFilePath = signatureJpgFolder.getAbsolutePath() + "/" + filename + ".jpg";
            File outputFile = new File(sigTempFilePath);

            if (outputFile.exists()) {
                return outputFile;
            }

            Page page = getSignature(signatureFile.getAbsolutePath());
            if (page == null) {
                return null;
            }

            Rect cropBox = page.getCropBox();
            int width = (int) cropBox.getWidth();
            int height = (int) cropBox.getHeight();

            pdfDraw = new PDFDraw();
            pdfDraw.setPageTransparent(true);
            pdfDraw.setImageSize(width, height, true);
            pdfDraw.export(page, sigTempFilePath, "jpeg");
            return outputFile;
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
        return null;
    }

    public Bitmap getSavedSignatureBitmap(Context context, File signatureFile) {
        File outputFile = getSavedSignatureJpegFile(context, signatureFile);
        if (outputFile == null) {
            return null;
        } else {
            return BitmapFactory.decodeFile(outputFile.getAbsolutePath());
        }
    }

    @Nullable
    public String getSignatureFilePath(@NonNull Context context) {
        File signatureFolder = getSavedSignatureFolder(context);
        if (null == signatureFolder) {
            return null;
        }
        File signatureFile = new File(signatureFolder, SIGNATURE_FILE_NAME);
        return Utils.getFileNameNotInUse(signatureFile.getAbsolutePath());
    }

    /**
     * Sets the default signature.
     *
     * @param pdfFilename The file name of PDF containing the signature image
     */
    @Deprecated
    public void setDefaultSignatureFile(String pdfFilename) {
        mDefaultSignatureFilename = pdfFilename;
    }

    /**
     * @return The default signature file name
     */
    @Deprecated
    public String getDefaultSignatureFile() {
        return mDefaultSignatureFilename;
    }

    @Deprecated
    public boolean hasDefaultSignature(Context context) {
        boolean hasDefaultSignature = false;

        File stampFile = getStampFile(context);
        if (stampFile.exists()) {
            PDFDoc doc = getStampDoc(stampFile);
            boolean shouldUnlockRead = false;
            try {
                doc.lockRead();
                shouldUnlockRead = true;
                if (doc.getPageCount() > 0) {
                    hasDefaultSignature = true;
                }
            } catch (PDFNetException e) {
            } finally {
                if (shouldUnlockRead) {
                    Utils.unlockReadQuietly(doc);
                }
            }
        }
        return hasDefaultSignature;
    }

    @Deprecated
    public Page getDefaultSignature(Context context) {
        Page page = null;

        File stampFile = getStampFile(context);
        if (stampFile.exists()) {
            PDFDoc doc = getStampDoc(stampFile);
            boolean shouldUnlockRead = false;
            try {
                doc.lockRead();
                shouldUnlockRead = true;
                if (doc.getPageCount() > 0) {
                    page = doc.getPage(1);
                }
            } catch (PDFNetException e) {
            } finally {
                if (shouldUnlockRead) {
                    Utils.unlockReadQuietly(doc);
                }
            }
        }
        return page;
    }

    public void deleteSignature(Context context, String filepath) {
        if (null == context || null == filepath) {
            return;
        }
        File file = new File(filepath);
        if (file.exists()) {
            File savedSignatureJpegFile = StampManager.getInstance().getSavedSignatureJpegFile(context, file);
            if (savedSignatureJpegFile != null && savedSignatureJpegFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                savedSignatureJpegFile.delete();
            }
            boolean success = file.delete();
            if (success) {
                savedSignatureDeleted();
            }
        }
    }

    @Deprecated
    public void deleteDefaultSignature(Context context) {
        File stampFile = getStampFile(context);
        if (stampFile.exists()) {
            PDFDoc doc = getStampDoc(stampFile);
            boolean shouldUnlock = false;
            try {
                doc.lock();
                shouldUnlock = true;
                doc.pageRemove(doc.getPageIterator(1));
                doc.save(stampFile.getAbsolutePath(), SDFDoc.SaveMode.REMOVE_UNUSED, null);
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    Utils.unlockQuietly(doc);
                }
            }
        }
    }

    public Page getSignature(String signatureFilename) {
        Page page = null;

        File stampFile = new File(signatureFilename);
        if (stampFile.exists()) {
            PDFDoc doc = getStampDoc(stampFile);
            boolean shouldUnlockRead = false;
            try {
                doc.lockRead();
                shouldUnlockRead = true;
                if (doc.getPageCount() > 0) {
                    page = doc.getPage(1);
                }
            } catch (PDFNetException e) {
            } finally {
                if (shouldUnlockRead) {
                    Utils.unlockReadQuietly(doc);
                }
            }
        }
        return page;
    }

    @Nullable
    public String createSignatureFromImage(Context context, Uri imageUri, int imageRotation) {
        SecondaryFileFilter filter = null;
        PDFDoc newDoc = null;
        try {
            filter = new SecondaryFileFilter(context, imageUri);

            // set encoder hints
            ObjSet hintSet = new ObjSet();
            Obj encoderHints = hintSet.createArray();
            encoderHints.pushBackName("JPEG");
            encoderHints.pushBackName("Quality");
            encoderHints.pushBackNumber(85);

            // create an image
            // then stamp on the new document
            newDoc = new PDFDoc();
            Image img = Image.create(newDoc.getSDFDoc(), filter, encoderHints);

            double stampWidth = img.getImageWidth();
            double stampHeight = img.getImageHeight();
            if (imageRotation == 90 || imageRotation == 270) {
                double temp = stampWidth;
                stampWidth = stampHeight;
                stampHeight = temp;
            }
            Page newPage = newDoc.pageCreate(new Rect(0, 0, stampWidth, stampHeight));
            newDoc.pagePushBack(newPage);
            com.pdftron.pdf.Stamper stamper = new com.pdftron.pdf.Stamper(com.pdftron.pdf.Stamper.e_absolute_size, stampWidth, stampHeight);
            stamper.setPosition(0, 0);
            stamper.setRotation(imageRotation);
            stamper.stampImage(newDoc, img, new PageSet(1));

            String signaturePath = getSignatureFilePath(context);
            if (null != signaturePath) {
                newDoc.save(signaturePath, SDFDoc.SaveMode.NO_FLAGS, null);
                savedSignatureCreated();
                return signaturePath;
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            Utils.closeQuietly(filter);
            Utils.closeQuietly(newDoc);
        }
        return null;
    }

    // Create signature from svg
    public Single<Page> createSignature(final Context context, final String signatureFileName, final String svgString) {
        return Single.create(new SingleOnSubscribe<Page>() {
            @Override
            public void subscribe(final SingleEmitter<Page> emitter) throws Exception {
                File svgFile = new File(Utils.getFileNameNotInUse(
                        new File(new File(signatureFileName).getParentFile(), "temp_svg.svg").getAbsolutePath())
                );
                try {
                    FileWriter out = new FileWriter(svgFile);
                    out.write(svgString);
                    out.close();

                    HTML2PDF.fromUrl(context, Uri.fromFile(svgFile).toString(), Uri.fromFile(new File(signatureFileName).getParentFile()), new File(signatureFileName).getName(), new HTML2PDF.HTML2PDFListener() {
                        @Override
                        public void onConversionFinished(String pdfOutput, boolean isLocal) {
                            PDFDoc doc = getStampDoc(new File(pdfOutput));
                            if (null != doc) {
                                try {
                                    Page page = doc.getPage(1);
                                    emitter.onSuccess(page);
                                } catch (Exception ex) {
                                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                                    emitter.tryOnError(ex);
                                }
                            }
                        }

                        @Override
                        public void onConversionFailed(String error) {
                            emitter.tryOnError(new RuntimeException(error));
                        }
                    });
                } catch (IOException e) {
                    emitter.tryOnError(new RuntimeException(e));
                }
            }
        });
    }

    public boolean createTypedSignature(String signatureFilename, @NonNull TextView editText, int textColor, String pdfFontName) {
        PDFDoc doc = createDocumentWithText(signatureFilename, editText, textColor, pdfFontName);
        if (null != doc) {
            try {
                Page page = doc.getPage(1);
                boolean success = page != null && page.isValid();
                if (success) {
                    savedSignatureCreated();
                }
                return success;
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            } finally {
                Utils.closeQuietly(doc);
            }
        }
        return false;
    }

    @Nullable
    private PDFDoc createDocumentWithText(String signatureFilePath,
            @NonNull TextView testView, int textColor, @NonNull String pdfFontName) {
        if (null == signatureFilePath) {
            return null;
        }

        String contents = testView.getText().toString();
        PDFDoc doc = null;
        boolean shouldUnlock = false;
        try {
            File signatureFile = new File(signatureFilePath);
            doc = getStampDoc(signatureFile);
            doc.lock();
            shouldUnlock = true;
            if (doc.getPageCount() > 0) {
                doc.pageRemove(doc.getPageIterator(1));
            }

            // some large width blank page
            double width = 17 * 72;
            double height = 11 * 72;
            Page page = doc.pageCreate(new
                    Rect(0,
                    0,
                    width,
                    height)
            );
            doc.pagePushBack(page);

            FreeText freeText = FreeText.create(doc, page.getCropBox());
            freeText.setQuaddingFormat(1);
            freeText.setContents(contents);
            freeText.setFontSize(48);
            Annot.BorderStyle border = freeText.getBorderStyle();
            border.setWidth(0);
            freeText.setBorderStyle(border);
            freeText.setTextColor(Utils.color2ColorPt(textColor), 3);
            freeText.refreshAppearance();
            FreeTextInfo.setFontSimple(freeText, pdfFontName);
            page.annotPushBack(freeText);
            freeText.flatten(page);

            Rect resizeRect = FreeTextInfo.getFreeTextBBoxSimple(freeText);
            resizeRect.inflate(10);
            page.setCropBox(resizeRect);
            page.setMediaBox(resizeRect);

            doc.save(signatureFilePath, SDFDoc.SaveMode.REMOVE_UNUSED, null);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            Logger.INSTANCE.LogE("StampManager", e.getMessage());
        } finally {
            if (shouldUnlock) {
                Utils.unlockQuietly(doc);
            }
        }

        return doc;
    }

    public boolean createVariableThicknessSignature(String signatureFilename,
            RectF signatureBBox, List<double[]> strokes,
            int strokeColor, float strokeThickness) {
        PDFDoc doc = createDocumentWithVariableThickness(signatureFilename, signatureBBox, strokes, strokeColor, strokeThickness);
        if (null != doc) {
            try {
                Page page = doc.getPage(1);
                boolean success = page != null && page.isValid();
                if (success) {
                    savedSignatureCreated();
                }
                return success;
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            } finally {
                Utils.closeQuietly(doc);
            }
        }
        return false;
    }

    @Nullable
    private PDFDoc createDocumentWithVariableThickness(String signatureFilePath,
            RectF signatureBBox, List<double[]> paths,
            int strokeColor, float strokeThickness) {
        if (paths == null || paths.isEmpty()) {
            return null;
        }
        if (null == signatureFilePath) {
            return null;
        }
        PDFDoc doc = null;
        boolean shouldUnlock = false;
        try {
            File signatureFile = new File(signatureFilePath);
            doc = getStampDoc(signatureFile);
            doc.lock();
            shouldUnlock = true;
            if (doc.getPageCount() > 0) {
                doc.pageRemove(doc.getPageIterator(1));
            }

            ElementBuilder eb = new ElementBuilder();        // ElementBuilder is used to build new
            // Element objects
            ElementWriter writer = new ElementWriter();    // ElementWriter is used to write
            // Elements to the page

            Element element;
            GState gstate;

            // Create a new page with a "buffer" on each side.
            float left = signatureBBox.left;
            float top = signatureBBox.top;
            float bottom = signatureBBox.bottom;
            float right = signatureBBox.right;
            Page page = doc.pageCreate(new
                    Rect(0,
                    0,
                    right - left + 2 * strokeThickness,
                    top - bottom + 2 * strokeThickness)
            );

            writer.begin(page);    // begin writing to this page
            eb.pathBegin();        // start constructing the path

            float delX = -left + strokeThickness;
            float delY = top + strokeThickness;

            for (double[] path : paths) {

                if (path.length == 0) {
                    continue;
                }

                // Create the operators
                int numOperators = (path.length / 2 - 1) / 3 + 1;
                byte[] operators = new byte[numOperators];
                operators[0] = PathData.e_moveto;
                for (int j = 1; j < numOperators; j++) {
                    operators[j] = PathData.e_cubicto;
                }

                // Add them to element builder
                eb.createPath(path, operators);

                element = eb.pathEnd();            // the path is now finished
                element.setPathFill(true);        // the path should be filled
                element.setWindingFill(true);
                gstate = element.getGState();
                gstate.setTransform(1.0f, 0, 0, -1.0d, delX, delY);
                gstate.setFillColorSpace(ColorSpace.createDeviceRGB());
                gstate.setFillColor(Utils.color2ColorPt(strokeColor));
                writer.writePlacedElement(element);
            }

            writer.end();  // save changes to the current page
            doc.pagePushBack(page); // todo bfung this should really push directly to the doc but will work for signatures for now, should refactor when added to inking

            doc.save(signatureFilePath, SDFDoc.SaveMode.REMOVE_UNUSED, null);
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            Logger.INSTANCE.LogE("StampManager", e.getMessage());
        } finally {
            if (shouldUnlock) {
                Utils.unlockQuietly(doc);
            }
        }

        return doc;
    }

    @Nullable
    public Page createSignature(String signatureFilename,
            RectF signatureBBox, LinkedList<LinkedList<PointF>> paths,
            int strokeColor, float strokeThickness) {
        Page page = null;

        PDFDoc doc = createDocument(signatureFilename, signatureBBox, paths, strokeColor, strokeThickness);
        if (null != doc) {
            try {
                page = doc.getPage(1);
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            }
        }
        return page;
    }

    @Nullable
    private PDFDoc createDocument(String signatureFilePath,
            RectF signatureBBox, LinkedList<LinkedList<PointF>> paths,
            int strokeColor, float strokeThickness) {
        if (null == signatureFilePath) {
            return null;
        }
        PDFDoc doc = null;
        boolean shouldUnlock = false;

        try {
            File signatureFile = new File(signatureFilePath);
            doc = getStampDoc(signatureFile);
            doc.lock();
            shouldUnlock = true;
            if (doc.getPageCount() > 0) {
                doc.pageRemove(doc.getPageIterator(1));
            }

            // Create a new page with a "buffer" on each side.
            Page page = doc.pageCreate(new Rect(0, 0, signatureBBox.right - signatureBBox.left + (PAGE_BUFFER * 2), signatureBBox.top - signatureBBox.bottom + (PAGE_BUFFER * 2)));
            doc.pagePushBack(page);

            // Create the annotation in the middle of the page.
            com.pdftron.pdf.annots.Ink ink = com.pdftron.pdf.annots.Ink.create(doc, new Rect(PAGE_BUFFER, PAGE_BUFFER, signatureBBox.right - signatureBBox.left + PAGE_BUFFER, signatureBBox.top - signatureBBox.bottom + PAGE_BUFFER));
            BorderStyle bs = ink.getBorderStyle();
            bs.setWidth(strokeThickness);
            ink.setBorderStyle(bs);

            // Shove the points to the ink annotation
            int i = 0;
            Point pdfPoint = new Point();
            for (LinkedList<PointF> pointList : paths) {
                int j = 0;
                for (PointF p : pointList) {
                    pdfPoint.x = p.x - signatureBBox.left + PAGE_BUFFER;
                    pdfPoint.y = signatureBBox.top - p.y + PAGE_BUFFER;
                    ink.setPoint(i, j++, pdfPoint);
                }
                i++;
            }

            double r = (double) Color.red(strokeColor) / 255;
            double g = (double) Color.green(strokeColor) / 255;
            double b = (double) Color.blue(strokeColor) / 255;
            ink.setColor(new ColorPt(r, g, b), 3);
            ink.refreshAppearance();

            // Make the page crop box the same as the annotation bounding box, so that there's no gaps.
            Rect newBoundRect = ink.getRect();
            page.setCropBox(newBoundRect);

            ink.refreshAppearance();

            page.annotPushBack(ink);

            doc.save(signatureFilePath, SDFDoc.SaveMode.REMOVE_UNUSED, null);
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                Utils.unlockQuietly(doc);
            }
        }

        return doc;
    }

    public static Single<File> getSignaturePreview(@NonNull Context context, @NonNull final String stampId) {
        Context applicationContext = context.getApplicationContext();
        return Single.create(new SingleOnSubscribe<File>() {
            @Override
            public void subscribe(SingleEmitter<File> emitter) throws Exception {
                File[] files = getInstance().getSavedSignatures(applicationContext);
                if (files != null && files.length > 0) {
                    File jpg = null;
                    for (File file : files) {
                        if (stampId.equals(file.getAbsolutePath())) {
                            jpg = getInstance().getSavedSignatureJpegFile(applicationContext, file);
                            break;
                        }
                    }
                    if (jpg != null) {
                        emitter.onSuccess(jpg);
                    } else {
                        emitter.tryOnError(new Exception("Could not find the matching signature"));
                    }
                } else {
                    emitter.tryOnError(new Exception("Could not create signature preview"));
                }
            }
        });
    }

    public void setDelayRemoveSignature(String filePath) {
        mDelayRemoveSignatureFilePath = filePath;
    }

    public void consumeDelayRemoveSignature(Context context) {
        if (mDelayRemoveSignatureFilePath != null) {
            deleteSignature(context, mDelayRemoveSignatureFilePath);
        }
        mDelayRemoveSignatureFilePath = null;
    }
}
