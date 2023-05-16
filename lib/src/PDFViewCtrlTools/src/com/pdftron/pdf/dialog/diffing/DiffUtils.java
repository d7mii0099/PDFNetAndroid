package com.pdftron.pdf.dialog.diffing;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.filters.SecondaryFileFilter;
import com.pdftron.pdf.DiffOptions;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.controls.PdfViewCtrlTabFragment2;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.RequestCode;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.sdf.SDFDoc;

import java.io.File;
import java.util.ArrayList;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;

import static android.app.Activity.RESULT_OK;

/**
 * View model for diffing functionality.
 */
public class DiffUtils {

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void selectFile(Activity activity) {
        if (!Utils.isKitKat()) {
            return;
        }
        Intent intent = createIntent();
        activity.startActivityForResult(intent, RequestCode.PICK_PDF_FILE);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static FileInfo handleActivityResult(Context context, int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == RequestCode.PICK_PDF_FILE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                Uri dataUri = data.getData();
                return getUriInfo(context, dataUri);
            }
        }

        return null;
    }

    public static FileInfo getUriInfo(Context context, Uri dataUri) {
        String title = Utils.getUriDisplayName(context, dataUri);
        return new FileInfo(FileInfo.FILE_TYPE_EDIT_URI, dataUri.toString(), title, false, 1);
    }

    private static File getDefaultDiffFile(@NonNull Context context) {
        String fileName = "pdf-diff.pdf";
        File downloadFolder = Utils.getExternalDownloadDirectory(context);
        File diffFile = new File(downloadFolder, fileName);
        String diffName = Utils.getFileNameNotInUse(diffFile.getAbsolutePath());
        diffFile = new File(diffName);
        return diffFile;
    }

    public static Single<Uri> compareFiles(final Context context, final ArrayList<Uri> fileUris,
            @ColorInt final int color1, @ColorInt final int color2,
            final int blendMode) {
        File diffFile = getDefaultDiffFile(context);
        return compareFiles(context, fileUris, color1, color2, blendMode, diffFile);
    }

    public static Single<Uri> compareFiles(final Context context, final ArrayList<Uri> fileUris,
            @ColorInt final int color1, @ColorInt final int color2,
            final int blendMode, final File diffFile) {
        return Single.create(new SingleOnSubscribe<Uri>() {
            @Override
            public void subscribe(SingleEmitter<Uri> emitter) throws Exception {
                Uri uri = compareFilesImpl(context, fileUris, color1, color2, blendMode, diffFile);
                if (uri != null) {
                    emitter.onSuccess(uri);
                } else {
                    emitter.tryOnError(new IllegalStateException("Invalid state when comparing files"));
                }
            }
        });
    }

    @Nullable
    public static Uri compareFilesImpl(final Context context, final ArrayList<Uri> fileUris,
            @ColorInt final int color1, @ColorInt final int color2,
            final int blendMode) {
        File diffFile = getDefaultDiffFile(context);
        return compareFilesImpl(context, fileUris, color1, color2, blendMode, diffFile);
    }

    @Nullable
    public static Uri compareFilesImpl(final Context context, final ArrayList<Uri> fileUris,
            @ColorInt final int color1, @ColorInt final int color2,
            final int blendMode, final File diffFile) {
        if (fileUris.size() == 2) {
            Uri firstFile = fileUris.get(0);
            Uri secondFile = fileUris.get(1);

            PDFDoc pdfDoc1 = null;
            PDFDoc pdfDoc2 = null;
            PDFDoc diffDoc = null;

            try {
                pdfDoc1 = getPdfDoc(context, firstFile);
                pdfDoc2 = getPdfDoc(context, secondFile);

                diffDoc = diff(pdfDoc1, pdfDoc2, color1, color2, blendMode);
                diffDoc.lock();
                diffDoc.save(diffFile.getAbsolutePath(), SDFDoc.SaveMode.REMOVE_UNUSED, null);
                diffDoc.unlock();
                return Uri.fromFile(diffFile);
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                Utils.closeQuietly(pdfDoc1);
                Utils.closeQuietly(pdfDoc2);
                Utils.closeQuietly(diffDoc);
            }
        }

        return null;
    }

    public static void updateDiff(PdfViewCtrlTabFragment2 pdfViewCtrlTabFragment, ArrayList<Uri> fileUris,
            @ColorInt int color1, @ColorInt int color2, int blendMode) {
        if (pdfViewCtrlTabFragment == null) {
            return;
        }

        PDFViewCtrl pdfViewCtrl = pdfViewCtrlTabFragment.getPDFViewCtrl();
        if (pdfViewCtrl == null) {
            return;
        }

        if (fileUris.size() == 2) {
            Uri firstFile = fileUris.get(0);
            Uri secondFile = fileUris.get(1);

            PDFDoc pdfDoc1 = null;
            PDFDoc pdfDoc2 = null;
            PDFDoc diffDoc = null;
            boolean shouldUnlock = false;
            try {
                pdfViewCtrl.docLock(true);
                shouldUnlock = true;

                PDFDoc currentDoc = pdfViewCtrl.getDoc();

                pdfDoc1 = getPdfDoc(pdfViewCtrl.getContext(), firstFile);
                pdfDoc2 = getPdfDoc(pdfViewCtrl.getContext(), secondFile);

                diffDoc = diff(pdfDoc1, pdfDoc2, color1, color2, blendMode);

                currentDoc.pageRemove(currentDoc.getPageIterator(1));
                currentDoc.insertPages(0, diffDoc, 1, diffDoc.getPageCount(), PDFDoc.InsertBookmarkMode.NONE, null);
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                Utils.closeQuietly(pdfDoc1);
                Utils.closeQuietly(pdfDoc2);
                Utils.closeQuietly(diffDoc);
                if (shouldUnlock) {
                    pdfViewCtrl.docUnlock();
                }
                try {
                    pdfViewCtrl.docLockRead(() -> pdfViewCtrl.updatePageLayout());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static PDFDoc diff(PDFDoc pdfDoc1, PDFDoc pdfDoc2,
            @ColorInt int color1, @ColorInt int color2, int blendMode) throws Exception {
        int count1 = pdfDoc1.getPageCount();
        int count2 = pdfDoc2.getPageCount();

        PDFDoc outDoc = new PDFDoc();
        DiffOptions diffOptions = new DiffOptions();
        diffOptions.setColorA(Utils.color2ColorPt(color1));
        diffOptions.setColorB(Utils.color2ColorPt(color2));
        diffOptions.setBlendMode(blendMode);

        for (int i = 1; i <= Math.max(count1, count2); i++) {
            Page page1 = pdfDoc1.getPage(i);
            Page page2 = pdfDoc2.getPage(i);

            outDoc.appendVisualDiff(page1, page2, diffOptions);
        }

        return outDoc;
    }

    public static PDFDoc getPdfDoc(Context context, Uri fileUri) throws Exception {
        SecondaryFileFilter fileFilter = new SecondaryFileFilter(context, fileUri);
        return new PDFDoc(fileFilter);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static Intent createIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
        // Reference: https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/provider/DocumentsContract.java#109
        intent.putExtra("android.provider.extra.SHOW_ADVANCED", true);
        return intent;
    }
}
