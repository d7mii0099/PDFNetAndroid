package com.pdftron.pdf.interfaces;

import android.graphics.PointF;
import android.net.Uri;
import androidx.annotation.Nullable;

import com.pdftron.pdf.controls.AnnotStyleDialogFragment;

/**
 * Callback interface to be invoked when a signature has been created.
 */
public interface OnCreateSignatureListener {
    /**
     * Called when signature is created.
     */
    void onSignatureCreated(@Nullable String filepath, boolean saveSignature);

    /**
     * Called when signature from image is selected.
     */
    void onSignatureFromImage(@Nullable PointF targetPoint, int targetPage, @Nullable Long widget);

    /**
     * Called when style picker is dismissed.
     * @param styleDialog the style picker
     */
    void onAnnotStyleDialogFragmentDismissed(AnnotStyleDialogFragment styleDialog);

    /**
     * Callback for updating keystore/certificate information used for digital signatures.
     */
    interface OnKeystoreUpdatedListener {
        void onKeystoreFileUpdated(@Nullable Uri keystore);
        void onKeystorePasswordUpdated(@Nullable String password);
    }
}
