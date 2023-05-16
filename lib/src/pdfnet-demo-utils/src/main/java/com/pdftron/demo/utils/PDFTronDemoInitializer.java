package com.pdftron.demo.utils;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.utils.PDFTronToolsInitializer;
import com.pdftron.pdfnet.PDFNetInitializer;
import com.pdftron.pdfnet.TrialKeyProvider;

/**
 * Internal class to initialize PDFNet demo package.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PDFTronDemoInitializer extends ContentProvider {
    private static final String TAG = "PDFTronDemoInitializer";

    public boolean onCreate() {
        Context applicationContext = getContext();
        String key = PDFNetInitializer.getLicenseKey(applicationContext);
        if (key != null && applicationContext != null) { // null if it's not defined in gradle.properties or trial key stored locally
            try {
                // first check if license key is valid and whether we need to generate a trial key
                boolean shouldGenerateKey = TrialKeyProvider.shouldGenerateTrialKey(applicationContext, key);
                if (shouldGenerateKey) {
                    PDFTronToolsInitializer.handleGenerateTrialKey(applicationContext);
                }
                AppUtils.initializePDFNetApplication(applicationContext, key);
            } catch (PDFNetException e) {
                e.printStackTrace();
            }
        } else {
            // no key, we should generate a trial key
            PDFTronToolsInitializer.handleGenerateTrialKey(applicationContext);
            Log.w(TAG, PDFNetInitializer.MSG);
        }
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        PDFNetInitializer.checkPackage(getContext(), this);
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
