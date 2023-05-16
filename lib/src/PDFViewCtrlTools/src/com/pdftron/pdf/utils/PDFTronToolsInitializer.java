package com.pdftron.pdf.utils;

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
import com.pdftron.pdfnet.PDFNetInitializer;
import com.pdftron.pdfnet.TrialKeyProvider;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Internal class to initialize PDFNet tools package.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PDFTronToolsInitializer extends ContentProvider {
    private static final String TAG = "PDFTronToolsInitializer";

    @Override
    public boolean onCreate() {
        Context applicationContext = getContext();
        String key = PDFNetInitializer.getLicenseKey(applicationContext);
        if (key != null && applicationContext != null) { // null if it's not defined in gradle.properties or trial key stored locally
            try {
                // first check if license key is valid and whether we need to generate a trial key
                boolean shouldGenerateKey = TrialKeyProvider.shouldGenerateTrialKey(applicationContext, key);
                if (shouldGenerateKey) {
                    handleGenerateTrialKey(applicationContext);
                }
                AppUtils.initializePDFNetApplication(applicationContext, key);
            } catch (PDFNetException e) {
                e.printStackTrace();
            }
        } else {
            // no key, we should generate a trial key
            handleGenerateTrialKey(applicationContext);
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

    public static void handleGenerateTrialKey(Context applicationContext) {
        if (!Utils.hasInternetConnection(applicationContext)) {
            // without internet access, we will use a preset trial key
            return;
        }
        // generated key will be used for the next session
        // for current session it will use a preset trial key
        //TODO uncomment when PWS is back in business
//        generateTrialKey()
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Consumer<String>() {
//                    @Override
//                    public void accept(String s) throws Exception {
//                        if (!Utils.isNullOrEmpty(s)) {
//                            Log.d(TAG, "trial key generated: " + s);
//                            TrialKeyProvider.setTrialKey(applicationContext, s);
//                        }
//                    }
//                }, new Consumer<Throwable>() {
//                    @Override
//                    public void accept(Throwable throwable) throws Exception {
//                        Log.e(TAG, "Error generating trial demo key. If issue persist, contact support at support@pdftron.com");
//                    }
//                });
    }

    private static Single<String> generateTrialKey() {
        return Single.create(new SingleOnSubscribe<String>() {
            @Override
            public void subscribe(@NonNull SingleEmitter<String> emitter) throws Exception {
                String key = TrialKeyProvider.generateTrialKey();
                if (!Utils.isNullOrEmpty(key)) {
                    emitter.onSuccess(key);
                } else {
                    emitter.tryOnError(new Exception("Could not generate key"));
                }
            }
        });
    }
}
