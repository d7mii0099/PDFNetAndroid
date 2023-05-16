package com.pdftron.demo.browser.ui;

import android.content.Context;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.pdftron.demo.browser.db.file.FileDao;
import com.pdftron.demo.browser.db.file.FileDatabase;
import com.pdftron.demo.browser.db.file.FileEntity;
import com.pdftron.demo.browser.db.folder.FolderDatabase;
import com.pdftron.demo.browser.db.folder.FolderEntity;
import com.pdftron.demo.utils.MiscUtils;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Constants;
import com.pdftron.pdf.utils.Logger;
import com.pdftron.pdf.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;

import static com.pdftron.demo.browser.db.file.AllFilesDataSource.fileToFileEntity;
import static com.pdftron.pdf.model.BaseFileInfo.FILE_TYPE_FOLDER;

/**
 * Recursively fetch the local files stored on the device (i.e. shared storage and external
 * storage on SD card).
 */
@SuppressWarnings("RedundantThrows")
class FilesDatabaseUpdater implements DatabaseUpdater {
    private final String TAG = FilesDatabaseUpdater.class.toString();
    private final Set<String> mSuffixSet = new HashSet<>();
    private final Comparator<FileInfo> mDefaultFolderComparator = new Comparator<FileInfo>() {
        @Override
        public int compare(FileInfo o1, FileInfo o2) {
            return o1.getAbsolutePath().compareTo(o2.getAbsolutePath());
        }
    };

    private boolean mEmulatedExist = false;
    private FileDatabase filesDb;
    private FolderDatabase foldersDb;
    private Context mContext;

    FilesDatabaseUpdater(@Nullable Context context) {
        mContext= context;
        filesDb = FileDatabase.getInstance(context);
        foldersDb = FolderDatabase.getInstance(context);
        mSuffixSet.addAll(Arrays.asList(Constants.FILE_NAME_EXTENSIONS_VALID));
    }

    /**
     * @return an {@link Observable} that fetches files from disk. Emits list of supported files
     * in each directory on disk.
     */
    @NonNull
    public Single<Boolean> getFiles(@Nullable final File[] rootDirs) {

        return Single.create(new SingleOnSubscribe<Boolean>() {
            @Override
            public void subscribe(SingleEmitter<Boolean> emitter) throws Exception {
                // First look through database and delete missing files
                deleteMissingFiles(emitter);
                // Then iterate through folders to look for documents
                Logger.INSTANCE.LogD(TAG, "Subscribe RecursiveFetchedFiles");
                traverseFileEmitter(rootDirs, emitter);
                Logger.INSTANCE.LogD(TAG, "Finished RecursiveFetchedFiles");
                emitter.onSuccess(Boolean.TRUE);
            }
        });
    }

    public void deleteMissingFiles(SingleEmitter<Boolean> emitter) {
        FileDao fileDao = filesDb.fileDao();
        List<FileEntity> fileEntities = fileDao.getFiles();

        for (FileEntity fileEntity : fileEntities) {
            if (isCancelled(emitter)) {
                return;
            }
            File file = new File(fileEntity.getFilePath());
            if (!file.exists() || !file.canRead()) {
                fileDao.deleteFiles(fileEntity);
            }
        }
    }

    @Override
    @NonNull
    public Single<Boolean> getFiles(@Nullable Context context) {
        return getFiles(context != null && Utils.isKitKat() ?
                context.getExternalFilesDirs(null) : null);
    }

    /**
     * @return The internal storage and external SD card root directories
     */
    @WorkerThread
    private List<FileInfo> getRootFolders(@Nullable File[] mRootDirs) {
        ArrayList<FileInfo> rootFolders = new ArrayList<>();
        File storageDirectory = Environment.getExternalStorageDirectory();
        // For API19+, we can get the external SD card directory from system API
        // here, we add the internal storage dir and external storage dir
        // (bumped to Marshmallow to resolve duplicate file issue)
        if (Utils.isMarshmallow()) {
            rootFolders.add(new FileInfo(FILE_TYPE_FOLDER, storageDirectory));
            if (mRootDirs == null) {
                return rootFolders;
            }
            for (File file : mRootDirs) {
                boolean canAdd = true;
                while (file != null) {
                    file = file.getParentFile();
                    if (file == null) {
                        break;
                    }
                    String path = file.getAbsolutePath();
                    if (path.equalsIgnoreCase("/storage")
                            || path.equalsIgnoreCase("/")) {
                        break;
                    }
                    if (file.getParent() != null &&
                            file.getParent().equalsIgnoreCase("/storage") && !path.equals("emulated")) {
                        // add SD cards
                        break;
                    }

                    if (file.equals(storageDirectory)) {
                        // we want the internal storage dir from system API instead
                        canAdd = false;
                        break;
                    }
                }
                if (canAdd) {
                    rootFolders.add(new FileInfo(FILE_TYPE_FOLDER, file));
                }
            }
        } else {
            File rootDir = storageDirectory;
            while (rootDir != null && rootDir.getParentFile() != null && !rootDir.getParentFile()
                    .getAbsolutePath()
                    .equalsIgnoreCase("/")) {
                rootDir = rootDir.getParentFile();
            }
            rootFolders.add(new FileInfo(FILE_TYPE_FOLDER, rootDir));
        }

        // Add backup folder
        rootFolders.add(new FileInfo(FILE_TYPE_FOLDER, mContext.getExternalFilesDir(null)));

        MiscUtils.sortFileInfoList(rootFolders, mDefaultFolderComparator);
        return rootFolders;
    }

    /**
     * Emits files for each folder in the directory
     */
    @WorkerThread
    private void traverseFileEmitter(File[] rootDirs, SingleEmitter<Boolean> emitter) {
        mSuffixSet.addAll(Arrays.asList(Constants.FILE_NAME_EXTENSIONS_VALID));
        File emulate = new File("/storage/emulated");
        mEmulatedExist = emulate.exists();

        List<FileInfo> rootFolders = getRootFolders(rootDirs);
        for (FileInfo folderInfo : rootFolders) {
            if (isCancelled(emitter)) {
                return;
            }
            File folder = folderInfo.getFile();
            traverseFiles(folder, emitter);
        }
    }

    @WorkerThread
    private void traverseFiles(@Nullable File folder, SingleEmitter<Boolean> emitter) {
        if (folder == null || !folder.isDirectory() || isCancelled(emitter)) {
            if (isCancelled(emitter)) {
                return;
            }
            return;
        }
        try {
            Logger.INSTANCE.LogD(TAG, "Traversing folder " + folder.getAbsolutePath());
            File[] files = folder.listFiles();
            Logger.INSTANCE.LogD(TAG, "Folders fetched");
            if (files != null) {
                ArrayList<FileInfo> folderInfoList = new ArrayList<>();
                ArrayList<FileEntity> addFileList = new ArrayList<>();
                ArrayList<FolderEntity> folderList = new ArrayList<>();
                for (File file : files) {
                    if (isCancelled(emitter)) {
                        return;
                    }
                    if (accept(file)) {
                        if (file.isDirectory()) {
                            String absolutePath = file.getAbsolutePath();
                            folderInfoList.add(new FileInfo(FILE_TYPE_FOLDER, file));
                            FolderEntity folderEntity = new FolderEntity(absolutePath, false);
                            folderList.add(folderEntity);
                        } else {
                            FileEntity fileEntity = fileToFileEntity(file);
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
                if (!folderInfoList.isEmpty()) {
                    MiscUtils.sortFileInfoList(folderInfoList, mDefaultFolderComparator);
                }
                for (FileInfo folderInfo : folderInfoList) {
                    if (isCancelled(emitter)) {
                        return;
                    }
                    traverseFiles(folderInfo.getFile(), emitter);
                }
            }
        } catch (Exception e) {
            emitter.onError(e);
            emitter.onSuccess(Boolean.FALSE);
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            Logger.INSTANCE.LogD(TAG, "Error RecursiveFetchedFiles");
        }
    }

    /**
     * @param file to check
     * @return true the file is a supported file ({@link #mSuffixSet}) or a directory.
     */
    private boolean accept(@Nullable File file) {
        if (file == null || file.isHidden()) {
            return false;
        }
        if (!Utils.isMarshmallow()) {
            String path = file.getAbsolutePath();
            // workaround issue where same file shows up multiple times
            if (path.contains("/emulated/legacy/")
                    || (mEmulatedExist && path.contains("/storage/sdcard0/"))) {
                return false;
            }
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
}