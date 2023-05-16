package com.pdftron.demo.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Debug;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.pdftron.demo.R;
import com.pdftron.demo.browser.db.tree.DocumentTreeDatabase;
import com.pdftron.demo.browser.db.tree.DocumentTreeEntity;
import com.pdftron.demo.navigation.adapter.BaseFileAdapter;
import com.pdftron.demo.navigation.callbacks.JumpNavigationCallbacks;
import com.pdftron.filters.FilterReader;
import com.pdftron.filters.FilterWriter;
import com.pdftron.filters.SecondaryFileFilter;
import com.pdftron.pdf.FileSpec;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.dialog.PortfolioDialogFragment;
import com.pdftron.pdf.model.ExternalFileInfo;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.CustomAsyncTask;
import com.pdftron.pdf.utils.ImageMemoryCache;
import com.pdftron.pdf.utils.Logger;
import com.pdftron.pdf.utils.PathPool;
import com.pdftron.pdf.utils.RequestCode;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class MiscUtils {
    // Cancellable copy operation
    public static int copy(InputStream input, OutputStream output, CustomAsyncTask task) throws IOException {
        long count = 0L;
        byte[] buffer = new byte[4096];

        int n1;
        for (; -1 != (n1 = input.read(buffer)) && !task.isCancelled(); count += (long) n1) {
            output.write(buffer, 0, n1);
        }

        return (count > 2147483647L) ? -1 : (int) count;
    }

    public static void manageOOM(@Nullable Context context) {
        manageOOM(context, null);
    }

    public static void manageOOM(@Nullable Context context, @Nullable PDFViewCtrl pdfViewCtrl) {
        String oldMemStr = "", newMemStr = "";
        String memoryClass = "";
        ActivityManager.MemoryInfo memInfo = null;
        ActivityManager activityManager = null;

        // get memory info before cleanup
        if (context != null) {
            activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                memInfo = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memInfo);
                oldMemStr = "available memory size before cleanup: " + memInfo.availMem / (1024f * 1024f) + "MB, ";
            }
        }

        // cleanup
        ImageMemoryCache.getInstance().clearAll();
        PathPool.getInstance().clear();
        if (pdfViewCtrl != null) {
            // in future replace it with purgeMemoryDueToOOM. for now we want to get some statistics
            pdfViewCtrl.purgeMemory();
        }
        ThumbnailPathCacheManager.getInstance().cleanupResources(context);

        // get memory info after cleanup
        if (activityManager != null) {
            activityManager.getMemoryInfo(memInfo);
            newMemStr = "available memory size after cleanup: " + memInfo.availMem / (1024f * 1024f) + "MB, ";
            memoryClass = ", memory class: " + activityManager.getMemoryClass();
        }

        if (context != null) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("OOM - "
                    + oldMemStr + newMemStr
                    + "native heap allocated size: " + Debug.getNativeHeapAllocatedSize() / (1024f * 1024f) + "MB"
                    + memoryClass));
        }
    }

    public static void handleLowMemory(@Nullable Context context) {
        handleLowMemory(context, null);
    }

    public static void handleLowMemory(@Nullable Context context, @Nullable BaseFileAdapter adapter) {
        ImageMemoryCache.getInstance().clearAll();
        PathPool.getInstance().clear();
        if (context != null) {
            ThumbnailPathCacheManager.getInstance().cleanupResources(context);
        }
        if (adapter != null) {
            adapter.cancelAllThumbRequests();
            adapter.cleanupResources();
        }
    }

    public static boolean showSDCardActionErrorDialog(Context context, final JumpNavigationCallbacks jumpNavigationCallbacks, String action) {
        if (Utils.isLollipop()) {
            String message = String.format(context.getString(R.string.dialog_external_file_readonly_action),
                    context.getString(R.string.local_folders), action, context.getString(R.string.app_name));

            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            builder.setMessage(Html.fromHtml(message))
                    .setCancelable(true)
                    .setPositiveButton(context.getString(R.string.dialog_external_file_readonly_action_positive_btn), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // go to external tab
                            if (null != jumpNavigationCallbacks) {
                                jumpNavigationCallbacks.gotoExternalTab();
                            }
                        }
                    });

            final AlertDialog d = builder.create();
            d.show();
            return true;
        } else {
            return false;
        }
    }

    static public void removeFiles(ArrayList<FileInfo> filesToDelete) {
        if (filesToDelete != null) {
            for (int i = 0; i < filesToDelete.size(); i++) {
                final FileInfo fileInfo = filesToDelete.get(i);
                if (fileInfo.exists()) {
                    new AsyncTask<Void, Void, Boolean>() {
                        @Override
                        protected Boolean doInBackground(Void... params) {
                            try {
                                return fileInfo.getFile().delete();
                            } catch (Exception ignored) {

                            }
                            return false;
                        }
                    };
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean isExternalFileUri(@Nullable Context context, Uri uri) {
        return context != null && Utils.isKitKat() && DocumentsContract.isDocumentUri(context, uri);
    }

    public static boolean isIntentActionMain(Intent intent) {
        return Intent.ACTION_MAIN.equals(intent.getAction());
    }

    /**
     * Given an Intent, try to parse it and get a File for the PDF document.
     * This will only work if the Intent action is ACTION_VIEW or ACTION_EDIT,
     * and the Intent data is a valid PDF file scheme.
     *
     * @param intent the Intent that holds the data
     * @return a File for the PDF document
     */
    public static File parseIntentGetPdfFile(Context context, Intent intent) {
        File pdfFile = null;

        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            Uri uri = intent.getData();

            if (uri != null) {
                if ((Intent.ACTION_VIEW.equals(action) || Intent.ACTION_EDIT.equals(action)) &&
                        (uri.getLastPathSegment() != null && uri.getLastPathSegment().toLowerCase().endsWith(".pdf")) &&
                        uri.getPath() != null && new File(uri.getPath()).exists()) {
                    pdfFile = new File(uri.getPath());
                } else if ((Intent.ACTION_VIEW.equals(action) || Intent.ACTION_EDIT.equals(action)) &&
                        intent.getType() != null && intent.getType().equals("application/pdf")) {
                    // deal with files with extension .bin downloaded from browser
                    String[] projection = {"_data"};
                    Cursor cursor = null;
                    ContentResolver contentResolver = Utils.getContentResolver(context);
                    if (contentResolver == null) {
                        return null;
                    }
                    try {
                        cursor = contentResolver.query(uri, projection, null, null, null);
                        if (cursor != null && cursor.moveToFirst() && cursor.getColumnCount() > 0 && cursor.getCount() > 0) {
                            int index = cursor.getColumnIndex(projection[0]);
                            if (index != -1) {
                                String contentFilePath = cursor.getString(index);
                                if (contentFilePath != null && contentFilePath.toLowerCase().endsWith(".bin")) {
                                    File contentFile = new File(contentFilePath);
                                    if (contentFile.exists() && contentFile.isFile()) {
                                        pdfFile = contentFile;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        if (cursor != null) { // otherwise couldn't find projection
                            AnalyticsHandlerAdapter.getInstance().sendException(e);
                        }
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                } else if (intent.getExtras() != null) {
                    if ((Intent.ACTION_VIEW.equals(action) || Intent.ACTION_EDIT.equals(action)) &&
                            (uri.toString().startsWith("file://") && uri.getLastPathSegment() != null &&
                                    uri.getLastPathSegment().toLowerCase().endsWith(".bin"))) {
                        // deal with files with extension .bin downloaded from browser
                        pdfFile = new File(uri.getPath());
                        if (!pdfFile.exists()) {
                            pdfFile = null;
                        }
                    }
                }
            }
        }

        return pdfFile;
    }

    public static String extractFileFromPortfolio(File portfolioFile, String fileName) {
        String retValue;
        PDFDoc doc = null;
        boolean shouldUnlock = false;
        try {
            doc = new PDFDoc(portfolioFile.getAbsolutePath());
            doc.lock();
            shouldUnlock = true;
            doc.initSecurityHandler();
            return ViewerUtils.extractFileFromPortfolio(PortfolioDialogFragment.FILE_TYPE_FILE, null, doc, portfolioFile.getParent(), fileName);
        } catch (Exception e) {
            retValue = "";
        } finally {
            if (shouldUnlock) {
                Utils.unlockQuietly(doc);
            }
            Utils.closeQuietly(doc);
        }
        return retValue;
    }

    public static String extractFileFromPortfolio(Context context, Uri portfolioFileUri, String fileName) {
        String retValue;
        PDFDoc doc = null;
        SecondaryFileFilter filter = null;
        try {
            filter = new SecondaryFileFilter(context, portfolioFileUri);
            doc = new PDFDoc(filter);
            doc.initSecurityHandler();
            Uri parentUri = getUriParent(portfolioFileUri);
            return ViewerUtils.extractFileFromPortfolio(PortfolioDialogFragment.FILE_TYPE_FILE_URI, context, doc, parentUri.toString(), fileName);
        } catch (Exception e) {
            retValue = "";
        } finally {
            Utils.closeQuietly(doc, filter);
        }
        return retValue;
    }

    public static void extractFileFromFileSpec(Context context, Uri destFileUri, FileSpec fileSpec) throws Exception {
        SecondaryFileFilter filter = null;
        try {
            if (destFileUri != null) {
                com.pdftron.filters.Filter stm = fileSpec.getFileData();
                if (stm != null) {
                    filter = new SecondaryFileFilter(context, destFileUri, SecondaryFileFilter.WRITE_MODE);
                    FilterWriter filterWriter = new FilterWriter(filter);
                    FilterReader filterReader = new FilterReader(stm);
                    filterWriter.writeFilter(filterReader);
                    filterWriter.flushAll();
                }
            }
        } finally {
            Utils.closeQuietly(filter);
        }
    }

    public static ExternalFileInfo extractFileFromFileSpec(Context context, ExternalFileInfo extractFolder, FileSpec fileSpec, String fileName) throws Exception {
        String newFileName;
        ExternalFileInfo newFileInfo = null;
        int i;
        SecondaryFileFilter filter = null;
        try {
            for (i = 0; i < Utils.MAX_NUM_DUPLICATED_FILES; i++) {
                if (i == 0) {
                    newFileName = fileName;
                } else {
                    String extension = Utils.getExtension(fileName);
                    newFileName = FilenameUtils.removeExtension(fileName) + " (" + String.valueOf(i) + ")." + extension;
                }

                if (extractFolder != null && extractFolder.findFile(newFileName) == null) {
                    Uri tempUri = ExternalFileInfo.appendPathComponent(extractFolder.getUri(), newFileName);
                    String extension = MimeTypeMap.getFileExtensionFromUrl(tempUri.toString());
                    MimeTypeMap mime = MimeTypeMap.getSingleton();
                    String newFileType = mime.getMimeTypeFromExtension(extension);
                    newFileInfo = extractFolder.createFile(newFileType, newFileName);
                    break;
                }
            }
            if (newFileInfo != null) {
                com.pdftron.filters.Filter stm = fileSpec.getFileData();
                if (stm != null) {
                    filter = new SecondaryFileFilter(context, newFileInfo.getUri(), SecondaryFileFilter.WRITE_MODE);
                    FilterWriter filterWriter = new FilterWriter(filter);
                    FilterReader filterReader = new FilterReader(stm);
                    filterWriter.writeFilter(filterReader);
                    filterWriter.flushAll();
                }
            }
        } finally {
            Utils.closeQuietly(filter);
        }
        return newFileInfo;
    }

    public static boolean isPDFFile(String filename) {
        return FilenameUtils.wildcardMatch(filename, "*.pdf", IOCase.INSENSITIVE);
    }

    // Get the specified uri's path directory, built from its path component.
    // Suitable for use with the DocumentsContract API.
    public static Uri getUriParent(Uri uri) {
        if (uri != null) {
            String path = uri.getPath();
            if (!Utils.isNullOrEmpty(path)) {
                int lastSeparator = path.lastIndexOf(File.separatorChar);
                int lastColon = path.lastIndexOf(':');

                if (lastSeparator - 1 >= 0 && lastSeparator > lastColon && lastSeparator + 1 < path.length()) {
                    // There is content before and after this separator, and it appears after the last colon
                    // Truncate at last separator (exclusive)
                    path = path.substring(0, lastSeparator);
                } else if (lastColon - 1 >= 0 && lastColon + 1 < path.length()) {
                    // There is content before and after this colon-separator
                    // Truncate at last colon (inclusive)
                    path = path.substring(0, lastColon + 1);
                }
                // Build uri with truncated path
                return uri.buildUpon().path(path).build();
            }
        }
        return null;
    }

    public static void showPermissionResultSnackbar(Activity activity, View layout, boolean hasPermission, int requestCode) {
        if (activity == null || layout == null) {
            return;
        }

        final WeakReference<Activity> activityRef = new WeakReference<>(activity);
        if (hasPermission) {
            Logger.INSTANCE.LogI("permission", "permission granted");
            int resId;
            switch (requestCode) {
                case RequestCode.STORAGE_1:
                case RequestCode.STORAGE_2:
                    resId = R.string.permission_storage_available;
                    break;
                default:
                    resId = R.string.permission_generic_available;
                    break;
            }
            Snackbar.make(layout, resId,
                    Snackbar.LENGTH_SHORT)
                    .show();
        } else {
            Logger.INSTANCE.LogI("permission", "permissions were NOT granted.");
            Snackbar.make(layout, R.string.permissions_not_granted,
                    Snackbar.LENGTH_LONG)
                    .setAction(activity.getString(R.string.permission_screen_settings).toUpperCase(),
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Activity activity = activityRef.get();
                                    if (activity != null) {
                                        activity.startActivity(getAppSettingsIntent(activity));
                                    }
                                }
                            })
                    .show();
        }
    }

    public static Intent getAppSettingsIntent(Context context) {
        Intent intent;
        try {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            String packageName = context.getApplicationContext().getPackageName();
            intent.setData(Uri.fromParts("package", packageName, null));
        } catch (Exception e) {
            // fall back to generic App page
            intent = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
        }
        return intent;
    }

    public static boolean verifyPermissions(int[] grantResults) {
        // At least one result must be checked.
        if (grantResults.length < 1) {
            return false;
        }

        // Verify that each required permission has been granted, otherwise return false.
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static void updateAdapterViewWidthAfterGlobalLayout(RecyclerView recyclerView, BaseFileAdapter adapter) {
        final WeakReference<RecyclerView> recyclerViewRef = new WeakReference<>(recyclerView);
        final WeakReference<BaseFileAdapter> adapterRef = new WeakReference<>(adapter);
        try {
            recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            RecyclerView recyclerView = recyclerViewRef.get();
                            if (recyclerView == null) {
                                return;
                            }
                            try {
                                recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            } catch (Exception ignored) {
                            }
                            BaseFileAdapter adapter = adapterRef.get();
                            if (adapter == null) {
                                return;
                            }
                            adapter.updateMainViewWidth(recyclerView.getMeasuredWidth());
                        }
                    });
        } catch (Exception ignored) {
        }
    }

    /**
     * Sorts file info list according to sort setting
     *
     * @param list     A list of file info
     * @param sortMode The sort mode
     */
    public static void sortFileInfoList(
            @NonNull List<FileInfo> list,
            Comparator<FileInfo> sortMode
    ) {

        Utils.throwIfOnMainThread();
        try {
            Collections.sort(list, sortMode);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e, "mode: " + sortMode.toString());
        }
    }

    /**
     * Sorts DocumentFile list according to sort setting
     *
     * @param list     A list of DocumentFile
     * @param sortMode The sort mode
     */
    public static void sortDocumentFileList(
            @NonNull List<DocumentFile> list,
            Comparator<DocumentFile> sortMode
    ) {

        Utils.throwIfOnMainThread();
        try {
            Collections.sort(list, sortMode);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e, "mode: " + sortMode.toString());
        }
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * Returns description of a file a given content uri
     *
     * @param uriString uri to a file
     * @param context   context to get string resources and content resolver
     * @return description for file
     */
    public static SpannableStringBuilder getFileDescriptionFromUri(
            @NonNull String uriString, @NonNull Context context) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        Uri uri = Uri.parse(uriString);
        // If uri is from local or external storage, show path. Otherwise if
        // from cloud storage show provider name if possible
        String filePath;
        String authority = uri.getAuthority();
        String header = context.getResources().getString(R.string.system_files);
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            filePath = FilenameUtils.getFullPath(uri.getPath());
        } else if ("com.android.externalstorage.documents".equals(authority)) {
            filePath = Utils.getUriDocumentPath(uri);
        } else if (authority != null && authority.contains("microsoft")
                && authority.contains("drive")) { // If
            filePath = String.format("%s: %s", header, "OneDrive");
        } else if (authority != null && authority.contains("google")
                && authority.contains("storage")) {
            filePath = String.format("%s: %s", header, "Google Drive");
        } else {
            filePath = String.format("%s: %s", header, "Cloud");
        }
        builder.append(filePath);
        builder.setSpan(new StyleSpan(Typeface.ITALIC), 0, builder.length(), 0);
        return builder;
    }

    /**
     * Launches the document tree picker via intent.
     *
     * @param fragment used to start the activity
     */
    public static void launchDocumentTreePicker(@NonNull Fragment fragment) {
        try {
            // Root folder, launch standard picker
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            // Force advanced devices (SD cards) to always be visible
            intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
            // Reference: https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/provider/DocumentsContract.java#109
            intent.putExtra("android.provider.extra.SHOW_ADVANCED", true);
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, true);
            // Only show local storage devices
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            fragment.startActivityForResult(intent, RequestCode.DOCUMENT_TREE);
        } catch (Exception e) {
            String message = String.format(fragment.getString(R.string.dialog_external_intent_not_supported),
                    fragment.getString(R.string.dialog_external_intent_not_supported_more_info));
            CommonToast.showText(fragment.getContext(), message, Toast.LENGTH_LONG);
        }
    }

    /**
     * Handles the document tree picker intent, typically in onActivityResult.
     * @param activity used to show toast
     * @param treeUri Tree Uri obtains from Intent.ACTION_OPEN_DOCUMENT_TREE
     * @param contentResolver used to obtain persistable permission
     * @param onComplete callback after document tree uri is added to database
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static void handleDocumentTreePickerResult(
            @NonNull final Activity activity,
            @NonNull final Uri treeUri,
            @NonNull final ContentResolver contentResolver,
            @NonNull final Consumer<Uri> onComplete)
    {
        try {
            contentResolver.takePersistableUriPermission(treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            final Context applicationContext = activity.getApplicationContext();
            if (Utils.isUsingDocumentTree()) {
                Single.just(treeUri)
                        .map(new Function<Uri, Uri>() {
                            @Override
                            public Uri apply(Uri uri) throws Exception {
                                DocumentTreeDatabase.getInstance(applicationContext)
                                        .folderDao()
                                        .insertRoot(new DocumentTreeEntity(treeUri.toString()));
                                return uri;
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<Uri>() {
                            @Override
                            public void accept(Uri uri) throws Exception {
                                onComplete.accept(uri);
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                throw new RuntimeException(throwable);
                            }
                        });
            }
        } catch (SecurityException e) {
            // Failure taking permissions
            CommonToast.showText(activity, R.string.error_failed_to_open_document_tree, Toast.LENGTH_SHORT);
        }
    }

    public static void restartWithTransition(@NonNull Activity activity) {
        // Instead of calling recreated, we finish and restart since we do not handle it very well
        activity.finish();
        activity.startActivity(activity.getIntent()); // we want to keep the intent
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    public static boolean validateStringFilenameUsingIO(Context context, String filename) {
        File file = new File(context.getCacheDir(), filename);
        boolean created = false;
        try {
            created = file.createNewFile();
            return created;
        } catch (IOException e) {
            return false;
        } finally {
            if (created) {
                file.delete();
            }
        }
    }
}
