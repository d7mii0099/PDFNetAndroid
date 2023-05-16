package com.pdftron.pdf.utils.cache;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.io.File;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class UriCacheManager {

    public static final String BUNDLE_USE_CACHE_FOLDER = "PdfViewCtrlTabFragment_bundle_cache_folder_uri";
    private static final String cacheFolder = "uri_cache";
    private static final String cacheFolder2 = "uri_cache2";

    // Used for generic caching
    // In backup folder, files in this folder is moved to backup folder list
    public static File getCacheDir(@NonNull Context context) {
           return new File(context.getCacheDir(), cacheFolder);
    }

    // Used for edit uri file in the viewer when the file is a read-only file
    // so we can retrieve the file later on in recent list
    public static File getCacheDir2(@NonNull Context context) {
        return new File(context.getCacheDir(), cacheFolder2);
    }
}
