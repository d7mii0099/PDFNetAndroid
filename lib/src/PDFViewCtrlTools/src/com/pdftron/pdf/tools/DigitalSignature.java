package com.pdftron.pdf.tools;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Environment;
import android.view.MotionEvent;
import android.widget.Toast;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.Date;
import com.pdftron.pdf.DigitalSignatureField;
import com.pdftron.pdf.Field;
import com.pdftron.pdf.Image;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFDraw;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.SignatureWidget;
import com.pdftron.pdf.annots.Widget;
import com.pdftron.pdf.dialog.digitalsignature.DialogSignatureInfo;
import com.pdftron.pdf.dialog.digitalsignature.DigitalSignatureDialogFragment;
import com.pdftron.pdf.dialog.signature.SignatureDialogFragment;
import com.pdftron.pdf.dialog.signature.SignatureDialogFragmentBuilder;
import com.pdftron.pdf.interfaces.OnCreateSignatureListener;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.Logger;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.sdf.SDFDoc;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.reactivex.disposables.CompositeDisposable;

@Keep
public class DigitalSignature extends Signature {

    private static final String TAG = DigitalSignature.class.getName();

    private final String mDefaultFileSignedFilePath;

    // Disposables
    private final CompositeDisposable mDisposables;
    private static final String DEFAULT_FILENAME = "sample_signed_0.pdf";

    @Nullable
    private Uri mKeystore;
    @Nullable
    private String mPassword;

    /**
     * Class constructor
     */
    public DigitalSignature(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);

        mConfirmBtnStrRes = R.string.tools_qm_sign_and_save;
        mDisposables = new CompositeDisposable();

        mNextToolMode = getToolMode();
        mDefaultFileSignedFilePath =
                Utils.isUsingDocumentTree() ?
                        new File(Utils.getExternalDownloadDirectory(ctrl.getContext()), DEFAULT_FILENAME).getAbsolutePath() :
                        new File(Environment.getExternalStorageDirectory(), DEFAULT_FILENAME).getAbsolutePath();
    }

    @Override
    public void onClose() {
        super.onClose();

        if (mDisposables != null && !mDisposables.isDisposed()) {
            mDisposables.dispose();
        }
    }

    @Override
    protected SignatureDialogFragment createSignatureDialogFragment(Long targetWidget, ToolManager toolManager, SignatureDialogFragment.DialogMode dialogMode) {

        DigitalSignatureDialogFragment fragment = new SignatureDialogFragmentBuilder()
                .usingTargetPoint(mTargetPoint)
                .usingTargetPage(mTargetPageNum)
                .usingAnnotStyleProperties(toolManager.getAnnotStyleProperties())
                .usingTargetWidget(targetWidget)
                .usingColor(mColor)
                .usingStrokeWidth(mStrokeThickness)
                .usingShowSavedSignatures(toolManager.isShowSavedSignature())
                .usingShowSignaturePresets(toolManager.isShowSignaturePresets())
                .usingShowSignatureFromImage(toolManager.isShowSignatureFromImage())
                .usingShowTypedSignature(toolManager.isShowTypedSignature())
                .usingConfirmBtnStrRes(mConfirmBtnStrRes)
                .usingPressureSensitive(toolManager.isUsingPressureSensitiveSignatures())
                .usingDefaultKeystore(toolManager.getDigitalSignatureKeystore() != null)
                .usingDefaultStoreNewSignature(toolManager.getDefaultStoreNewSignature())
                .usingPersistStoreSignatureSetting(toolManager.isPersistStoreSignatureSetting())
                .usingDialogMode(dialogMode)
                .build(mPdfViewCtrl.getContext(), DigitalSignatureDialogFragment.class);

        fragment.setOnKeystoreUpdatedListener(new OnCreateSignatureListener.OnKeystoreUpdatedListener() {
            @Override
            public void onKeystoreFileUpdated(@Nullable Uri keystore) {
                mKeystore = keystore;
            }

            @Override
            public void onKeystorePasswordUpdated(@Nullable String password) {
                mPassword = password;
            }
        });
        return fragment;
    }

    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolManager.ToolMode.DIGITAL_SIGNATURE;
    }

    @Override
    public int getCreateAnnotType() {
        return AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE;
    }

    @Override
    protected boolean addSignatureStampToWidget(final Page page) {
        // Create the signature image file
        String signatureImagePath = createSignatureImageFile(mPdfViewCtrl.getContext(), page);
        // Sign the document
        return signPdf(signatureImagePath);
    }

    @Override
    protected void handleExistingSignatureWidget(int x, int y) {
        boolean handled = false;
        if (mWidget != null) {
            try {
                Field field = mWidget.getField();
                if (field.isValid() && field.getValue() != null) {
                    showSignatureInfo();
                    handled = true;
                }
            } catch (Exception ignored) {
            }
        }
        if (!handled) {
            super.handleExistingSignatureWidget(x, y);
        }
    }

    @Override
    public boolean onLongPress(MotionEvent e) {
        return onSingleTapConfirmed(e);
    }

    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        return false;
    }

    protected boolean signPdf(final String signaturePath) {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        String keystorePath = toolManager.getDigitalSignatureKeystore();
        if (keystorePath != null) {
            mKeystore = Uri.fromFile(new File(keystorePath));
            mPassword = toolManager.getDigitalSignatureKeystorePassword();
        }

        // Check keystore
        if (mKeystore == null) {
            CommonToast.showText(
                    mPdfViewCtrl.getContext(),
                    R.string.tools_digitalsignature_missing_keystore,
                    Toast.LENGTH_SHORT
            );
            return false;
        }
        return signPdfImpl(signaturePath, mKeystore);
    }

    /**
     * Signs the PDF document.
     */
    protected boolean signPdfImpl(@NonNull String signaturePath, @NonNull Uri keystore) {
        String newFilePath;
        String origFilePath = null;
        // Try to use the current doc filename
        try {
            newFilePath = mPdfViewCtrl.getDoc().getFileName();
            origFilePath = mPdfViewCtrl.getDoc().getFileName();
            if (newFilePath == null || newFilePath.length() == 0) {
                newFilePath = mDefaultFileSignedFilePath;
            } else {
                String s = newFilePath.substring(0, newFilePath.lastIndexOf("."));
                newFilePath = s + "_signed_0.pdf";
            }
        } catch (Exception e) {
            newFilePath = mDefaultFileSignedFilePath;
        }

        // Check for existing signed files and pick up a new name
        // so to not overwrite them.
        int i = 1;
        do {
            File signedFile = new File(newFilePath);
            if (signedFile.exists()) {
                String s = newFilePath.substring(0, newFilePath.lastIndexOf("_"));
                newFilePath = s + "_" + (i++) + ".pdf";
            } else {
                break;
            }
        } while (true);

        if (null != origFilePath) {
            saveFile(origFilePath);
            copyFile(origFilePath, newFilePath);
            File copiedFile = new File(newFilePath);
            int currPage = mPdfViewCtrl.getCurrentPage();
            boolean shouldUnlock = false;
            PDFDoc copiedDoc = null;

            try {
                Annot selectedAnnot = null;
                SignatureWidget widget = null;
                copiedDoc = new PDFDoc(copiedFile.getAbsolutePath());
                // get field from new PDFDoc
                copiedDoc.lock();
                shouldUnlock = true;
                Page page = copiedDoc.getPage(currPage);
                int annotationCount = page.getNumAnnots();
                for (int a = 0; a < annotationCount; a++) {
                    try {
                        Annot annotation = page.getAnnot(a);
                        if (null != annotation &&
                                annotation.isValid() &&
                                annotation.getSDFObj().getObjNum() == mAnnot.getSDFObj().getObjNum()) {
                            selectedAnnot = annotation;
                            break;
                        }
                    } catch (PDFNetException e) {
                        // this annotation has some problem, let's skip it and continue with others
                        AnalyticsHandlerAdapter.getInstance().sendException(e);
                    }
                }

                if (selectedAnnot != null) {
                    widget = new SignatureWidget(selectedAnnot);
                }

                if (widget != null) {
                    signDigitalSignatureField(copiedDoc, signaturePath, widget, keystore, mPassword == null ? "" : mPassword);

                    // IMPORTANT: If there are already signed/certified digital signature(s) in the document, you must save incrementally
                    // so as to not invalidate the other signature's('s) cryptographic hashes.
                    copiedDoc.save(copiedFile.getAbsolutePath(), SDFDoc.SaveMode.INCREMENTAL, null);

                    // Open the newly signed file
                    ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                    toolManager.onNewFileCreated(copiedFile.getAbsolutePath());

                    CommonToast.showText(mPdfViewCtrl.getContext(), String.format(getStringFromResId(R.string.tools_digitalsignature_msg_saved), copiedFile.getAbsolutePath()), Toast.LENGTH_LONG);
                } else {
                    CommonToast.showText(mPdfViewCtrl.getContext(), R.string.tools_digitalsignature_msg_file_locked, Toast.LENGTH_LONG);
                    return false;
                }
                return true;
            } catch (PDFNetException e) {
                CommonToast.showText(mPdfViewCtrl.getContext(), getStringFromResId(R.string.tools_digitalsignature_msg_failed_to_save), Toast.LENGTH_LONG);
                return false;
            } catch (IOException e) {
                CommonToast.showText(mPdfViewCtrl.getContext(), getStringFromResId(R.string.tools_digitalsignature_msg_failed_to_save), Toast.LENGTH_LONG);
                return false;
            } finally {
                if (shouldUnlock) {
                    Utils.unlockQuietly(copiedDoc);
                }
                Utils.closeQuietly(copiedDoc);
            }
        } else {
            return false;
        }
    }

    /**
     * Sign the digital signature field by creating the appearance, selecting the certificate,
     * and adding additional signing information.
     *
     * @param outputDoc     the output document that will be signed (copied from original document)
     * @param signaturePath the path of the image file that contains the signature
     * @param widget        signature widget that will be used for signing
     * @param keystore      the certificate file that will be used to digitally sign
     * @param password      the password associated with the certificate file
     */
    protected void signDigitalSignatureField(@NonNull PDFDoc outputDoc,
            @NonNull String signaturePath,
            @NonNull SignatureWidget widget,
            @NonNull Uri keystore,
            @NonNull String password) throws PDFNetException, IOException {

        // Create the signature appearance
        Image img = Image.create(outputDoc, signaturePath);
        widget.createSignatureAppearance(img);

        // Get the field and sign it
        DigitalSignatureField signatureField = new DigitalSignatureField(widget.getField());

        // Do not pass in null
        signatureField.signOnNextSave(getByteArrayFromUri(keystore), password);
    }

    @Nullable
    private byte[] getByteArrayFromUri(@NonNull Uri uri) throws IOException {
        ContentResolver cr = Utils.getContentResolver(mPdfViewCtrl.getContext());
        if (cr != null) {
            InputStream inputStream = null;
            try {
                inputStream = cr.openInputStream(uri);
                if (inputStream != null) {
                    return IOUtils.toByteArray(inputStream);
                }
            } finally {
                Utils.closeQuietly(inputStream);
            }
        }
        return null;
    }

    @Nullable
    public static String createSignatureImageFile(Context context, Page page) {

        String sigTempFilePath = context.getFilesDir().getAbsolutePath() + "/" + SIGNATURE_TEMP_FILE;

        // Create the signature image
        PDFDraw pdfDraw;
        try {
            Rect cropBox = page.getCropBox();
            int width = (int) cropBox.getWidth();
            int height = (int) cropBox.getHeight();
            pdfDraw = new PDFDraw();
            pdfDraw.setPageTransparent(true);
            pdfDraw.setImageSize(width, height, true);
            pdfDraw.export(page, sigTempFilePath, "jpeg");
        } catch (PDFNetException e) {
            e.printStackTrace();
            return null;
        }

        return sigTempFilePath;
    }

    /**
     * Shows the signature info
     */
    protected void showSignatureInfo() {
        // Get some information from the /V entry
        if (mAnnot != null) {
            boolean shouldUnlock = false;
            try {
                mPdfViewCtrl.docLockRead();
                shouldUnlock = true;
                Widget widget = new Widget(mAnnot);
                Field field = widget.getField();
                DigitalSignatureField signatureField = new DigitalSignatureField(field);
                if (field.isValid() && field.getValue() != null && signatureField.hasCryptographicSignature()) {

                    String documentPermissionMsg = getPermissionString(mPdfViewCtrl.getContext(), signatureField.getDocumentPermissions());
                    String location = null;
                    String reason = null;
                    String signatureName = null;
                    String contactInfo = null;
                    String signingTimeString = null;
                    if (hasSigningInfo(signatureField)) {
                        location = signatureField.getLocation();
                        reason = signatureField.getReason();
                        signatureName = signatureField.getSignatureName();
                        contactInfo = signatureField.getContactInfo();
                        Date signingTime = signatureField.getSigningTime();
                        signingTimeString = signingTime.isValid() ? AnnotUtils.getLocalDate(signingTime).toString() : null;
                    }

                    DialogSignatureInfo dialog = new DialogSignatureInfo(mPdfViewCtrl.getContext());
                    dialog.setLocation(location);
                    dialog.setReason(reason);
                    dialog.setName(signatureName);
                    dialog.setContactInfo(contactInfo);
                    dialog.setSigningTime(signingTimeString);
                    dialog.setDocumentPermission(documentPermissionMsg);
                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            unsetAnnot();
                            mNextToolMode = ToolManager.ToolMode.PAN;
                        }
                    });
                    dialog.show();
                }
            } catch (Exception e) {
                // Do nothing...
            } finally {
                if (shouldUnlock) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }
        }
    }

    private void saveFile(String oldPath) {
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            mPdfViewCtrl.getDoc().save(oldPath, SDFDoc.SaveMode.INCREMENTAL, null);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    @Nullable
    private String getPermissionString(@NonNull Context context, @NonNull DigitalSignatureField.DocumentPermissions permissions) {
        @StringRes
        int result;
        switch (permissions) {
            case e_no_changes_allowed:
                result = R.string.tools_digitalsignature_doc_permission_no_changes;
                break;
            case e_formfilling_signing_allowed:
                result = R.string.tools_digitalsignature_doc_permission_formfill_sign;
                break;
            case e_annotating_formfilling_signing_allowed:
                result = R.string.tools_digitalsignature_doc_permission_formfill_sign_annot;
                break;
            case e_unrestricted:
                result = R.string.tools_digitalsignature_doc_permission_unrestricted;
                break;
            default:
                Logger.INSTANCE.LogE(TAG, "Unrecognized digital signature document permission level.");
                return null;
        }
        return context.getString(result);
    }

    private boolean hasSigningInfo(@NonNull DigitalSignatureField signatureField) throws PDFNetException {
        return signatureField.getSubFilter() != DigitalSignatureField.SubFilterType.e_ETSI_RFC3161;
    }

    private void copyFile(String oldPath, String newPath) {
        InputStream is = null;
        OutputStream fos = null;
        try {
            is = new FileInputStream(oldPath);
            fos = new FileOutputStream(newPath);
            IOUtils.copy(is, fos);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            Utils.closeQuietly(fos);
            Utils.closeQuietly(is);
        }
    }
}
