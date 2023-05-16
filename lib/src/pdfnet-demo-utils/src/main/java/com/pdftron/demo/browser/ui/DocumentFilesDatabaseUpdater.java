package com.pdftron.demo.browser.ui;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.documentfile.provider.DocumentFile;

import com.pdftron.demo.browser.db.file.DocumentFileDatabase;
import com.pdftron.demo.browser.db.file.FileDao;
import com.pdftron.demo.browser.db.file.FileEntity;
import com.pdftron.demo.browser.db.folder.FolderDatabase;
import com.pdftron.demo.browser.db.folder.FolderEntity;
import com.pdftron.demo.browser.db.tree.DocumentTreeDatabase;
import com.pdftron.demo.browser.db.tree.DocumentTreeEntity;
import com.pdftron.demo.utils.MiscUtils;
import com.pdftron.pdf.model.ExternalFileInfo;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Constants;
import com.pdftron.pdf.utils.Logger;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;

import static com.pdftron.demo.browser.db.file.AllFilesDataSource.documentFileToFileEntity;

/**
 * Recursively fetch the local files stored on the device (i.e. shared storage and external
 * storage on SD card).
 */
@SuppressWarnings("RedundantThrows")
class DocumentFilesDatabaseUpdater implements DatabaseUpdater {
    private final String TAG = DocumentFilesDatabaseUpdater.class.toString();
    private final Set<String> mSuffixSet = new HashSet<>();
    private final List<String> mSuffixExceptions = new ArrayList<>(Arrays.asList("txt", "md"));
    private final Comparator<DocumentFile> mDefaultFolderComparator = new Comparator<DocumentFile>() {
        @Override
        public int compare(DocumentFile o1, DocumentFile o2) {
            return o1.getUri().toString().compareTo(o2.getUri().toString());
        }
    };

    @NonNull
    private DocumentFileDatabase filesDb;
    @NonNull
    private FolderDatabase foldersDb;
    @NonNull
    private DocumentTreeDatabase rootDb;

    DocumentFilesDatabaseUpdater(@Nullable Context applicationContext) {
        filesDb = DocumentFileDatabase.getInstance(applicationContext);
        foldersDb = FolderDatabase.getInstance(applicationContext);
        rootDb = DocumentTreeDatabase.getInstance(applicationContext);
        mSuffixSet.addAll(Arrays.asList(Constants.FILE_NAME_EXTENSIONS_VALID));
        mSuffixSet.removeAll(mSuffixExceptions);
    }

    @Override
    @NonNull
    public Single<Boolean> getFiles(@NonNull final Context applicationContext) {

        return Single.create(new SingleOnSubscribe<Boolean>() {
            @Override
            public void subscribe(SingleEmitter<Boolean> emitter) throws Exception {
                List<DocumentTreeEntity> roots = rootDb.folderDao().getRoots();

                if (roots.isEmpty()) {
                    emitter.onError(new MissingRootDirectoryException("Roots not mounted"));
                    return;
                }
                DocumentFile[] documentFiles = new DocumentFile[roots.size()];
                for (int i = 0; i < roots.size(); i++) {
                    documentFiles[i] = DocumentFile.fromTreeUri(applicationContext, Uri.parse(roots.get(i).getDocumentFileUri()));
                }
                // First look through database and delete missing files
                deleteMissingFiles(applicationContext, emitter);
                // Then iterate through folders to look for documents
                Logger.INSTANCE.LogD(TAG, "Subscribe RecursiveFetchedFiles");
                traverseFileEmitter(documentFiles, emitter);
                Logger.INSTANCE.LogD(TAG, "Finished RecursiveFetchedFiles");
                emitter.onSuccess(Boolean.TRUE);
            }
        });
    }

    public void deleteMissingFiles(Context context, SingleEmitter<Boolean> emitter) {
        FileDao fileDao = filesDb.fileDao();
        List<FileEntity> fileEntities = fileDao.getFiles();

        for (FileEntity fileEntity : fileEntities) {
            if (isCancelled(emitter)) {
                return;
            }
            if (!(DocumentFile.fromSingleUri(context, Uri.parse(fileEntity.getFilePath()))).exists()) {
                fileDao.deleteFiles(fileEntity);
            }
        }
    }

    /**
     * Emits files for each folder in the directory
     */
    @WorkerThread
    private void traverseFileEmitter(DocumentFile[] rootFiles, SingleEmitter<Boolean> emitter) {
        List<DocumentFile> rootFolders = new ArrayList<>();

        for (DocumentFile documentFile : rootFiles) {
            if (documentFile.isDirectory()) {
                rootFolders.add(documentFile);
            }
        }
        for (DocumentFile folder : rootFolders) {
            if (isCancelled(emitter)) {
                return;
            }
            foldersDb.folderDao().insertFolderInBackground(new FolderEntity(Utils.getUriDocumentPath(folder.getUri()), false));
            traverseFiles(folder, emitter);
        }
    }

    @WorkerThread
    private void traverseFiles(@Nullable DocumentFile folder, SingleEmitter<Boolean> emitter) {
        if (folder == null || !folder.isDirectory() || isCancelled(emitter)) {
            if (isCancelled(emitter)) {
                return;
            }
            return;
        }
        try {
            Logger.INSTANCE.LogD(TAG, "Traversing folder " + folder.getUri());
            DocumentFile[] files = folder.listFiles();
            Logger.INSTANCE.LogD(TAG, "Folders fetched");
            if (files != null) {
                ArrayList<DocumentFile> documentFolderList = new ArrayList<>();
//                ArrayList<FileEntity> fileList = new ArrayList<>();
                ArrayList<FileEntity> addFileList = new ArrayList<>();
//                ArrayList<FileEntity> updateFileList = new ArrayList<>();
                ArrayList<FolderEntity> folderList = new ArrayList<>();
                for (DocumentFile file : files) {
                    if (isCancelled(emitter)) {
                        return;
                    }
                    if (accept(file)) {
                        if (file.isDirectory()) {
                            documentFolderList.add(file);
                            FolderEntity folderEntity = new FolderEntity(Utils.getUriDocumentPath(file.getUri()), false);
                            folderList.add(folderEntity);
                        } else {
                            FileEntity fileEntity =
                                    documentFileToFileEntity(file,
                                            ExternalFileInfo.getParentRelativePath(
                                                    file.getUri(),
                                                    file.getName()
                                            )
                                    );
                            addFileList.add(fileEntity);
                        }
                    }
                }
                Logger.INSTANCE.LogD(TAG, "Files parsed");

                if (!addFileList.isEmpty()) {
                    filesDb.fileDao().insertFilesInBackground(addFileList);
                }
                Logger.INSTANCE.LogD(TAG, "Files Added");

                if (!folderList.isEmpty()) {
                    foldersDb.folderDao().insertFolders(folderList);
                }
                Logger.INSTANCE.LogD(TAG, "Folders Added");

                if (isCancelled(emitter)) {
                    return;
                }
                if (!documentFolderList.isEmpty()) {
                    MiscUtils.sortDocumentFileList(documentFolderList, mDefaultFolderComparator);
                }
                for (DocumentFile documentFolder : documentFolderList) {
                    if (isCancelled(emitter)) {
                        return;
                    }
                    traverseFiles(documentFolder, emitter);
                }
            }
        } catch (Exception e) {
            emitter.onSuccess(Boolean.FALSE);
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * @param file to check
     * @return true the file is a supported file ({@link #mSuffixSet}) or a directory.
     */
    private boolean accept(@Nullable DocumentFile file) {
        if (file == null || !file.canRead()) {
            return false;
        }
        if (file.isDirectory()) {
            return true;
        }
        String name = file.getName();
        String ext = Utils.getExtension(name);
        return mSuffixSet.contains(ext) && file.canRead();
    }

    private boolean isCancelled(SingleEmitter<Boolean> emitter) {
        boolean cancelled = emitter.isDisposed();
        if (cancelled) {
            Logger.INSTANCE.LogD(TAG, "Cancelled RecursiveFetchedFiles");
        }
        return cancelled;
    }

    static class MissingRootDirectoryException extends Exception {
        public MissingRootDirectoryException(String message) {
            super(message);
        }
    }
}
