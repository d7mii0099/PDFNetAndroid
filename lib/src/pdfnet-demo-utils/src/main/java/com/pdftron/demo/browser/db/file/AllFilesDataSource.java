package com.pdftron.demo.browser.db.file;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.pdftron.demo.browser.ui.FilterSettingsViewModel;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.Constants;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import io.reactivex.Flowable;

/**
 * Database source for all documents browser.
 */
public class AllFilesDataSource implements FileDataSource {

    protected final FileDao mFileDao;

    public AllFilesDataSource(FileDao fileDao) {
        mFileDao = fileDao;
    }

    @Override
    public Flowable<List<FileEntity>> getFilesFlowable(QueryParams queryParams) {
        String searchQuery = queryParams.searchQuery;
        List<Integer> documentTypes = queryParams.documentTypes;
        FilterSettingsViewModel.SortMode sortMode = queryParams.sortMode;
        boolean isGridMode = queryParams.gridCount > 0;
        switch (sortMode) {
            case NAME:
                if (isGridMode) {
                    return mFileDao.getFlatFilesFlowableByName(searchQuery, documentTypes);
                } else {
                    return mFileDao.getGroupedFilesFlowableByName(searchQuery, documentTypes);
                }
            case DATE_MODIFIED:
                if (isGridMode) {
                    return mFileDao.getFlatFlowableByDate(searchQuery, documentTypes);
                } else {
                    return mFileDao.getGroupedFlowableByDate(searchQuery, documentTypes);
                }
            default:
                throw new RuntimeException("Unknown sort type");
        }
    }

    @Override
    public void delete(FileInfo fileInfo) {
        FileEntity fileEntity = fileToFileEntity(fileInfo.getFile());
        mFileDao.deleteFiles(fileEntity);
    }

    @Override
    public void add(FileInfo fileInfo) {
        FileEntity fileEntity = fileToFileEntity(fileInfo.getFile());
        mFileDao.insertFiles(fileEntity);
    }

    @Override
    public void clear() {
        mFileDao.clearFiles();
    }

    public static FileEntity documentFileToFileEntity(@NonNull DocumentFile file, @NonNull String folderPath) {
        String uriString = file.getUri().toString();
        long date = file.lastModified();
        String name = file.getName();
        // Name should never be null here if referring to a file
        //noinspection ConstantConditions
        int fileType = getFileType(name);
        long fileSize = file.length();
        return new FileEntity(uriString, folderPath, name, fileType, date, getDateString(date), fileSize);
    }

    public static FileEntity fileToFileEntity(@NonNull File file) {
        String absolutePath = file.getAbsolutePath();
        long date = file.lastModified();
        String name = file.getName();
        int fileType = getFileType(name);
        String parent = file.getParent();
        long fileSize = file.length();
        // parent should not be null in this case
        //noinspection ConstantConditions
        return new FileEntity(absolutePath, parent, name, fileType, date, getDateString(date), fileSize);
    }

    protected static String getDateString(long date) {
        return DateFormat.getInstance().format(new Date(date));
    }

    public static int getFileType(@NonNull String filename) {
        filename = filename.toLowerCase();
        if (FilenameUtils.isExtension(filename, Constants.FILE_NAME_EXTENSIONS_PDF)) {
            return 0;
        } else if (FilenameUtils.isExtension(filename, Constants.FILE_NAME_EXTENSIONS_IMAGE)) {
            return 2;
        } else if (FilenameUtils.isExtension(filename, Constants.FILE_EXTENSIONS_TEXT)) {
            return 3;
        } else if (FilenameUtils.isExtension(filename, Constants.FILE_NAME_EXTENSIONS_OFFICE)) {
            return 1;
        } else {
            return -1;
        }
    }
}
