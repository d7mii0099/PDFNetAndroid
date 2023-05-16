package com.pdftron.pdf.interfaces;

import androidx.annotation.NonNull;

/**
 * Callback interface to be invoked when a signature has been selected.
 */
public interface OnSavedSignatureListener {
    /**
     * Called when a signature is selected.
     *
     * @param filepath The file path of the saved signature
     */
    void onSignatureSelected(@NonNull String filepath);

    /**
     * Called when create signature is selected.
     */
    void onCreateSignatureClicked();

    /**
     * Called when edit mode changed.
     * @param isEdit whether is editing
     */
    void onEditModeChanged(boolean isEdit);
}
