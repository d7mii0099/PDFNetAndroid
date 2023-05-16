package com.pdftron.demo.browser.db.file;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;

import com.pdftron.pdf.model.ExternalFileInfo;
import com.pdftron.pdf.model.FileInfo;

// Only used for Android Q in LocalDocumentFileViewFragment
@TargetApi(Build.VERSION_CODES.Q)
public class AllDocumentFilesDataSource extends AllFilesDataSource {
    public AllDocumentFilesDataSource(FileDao fileDao) {
        super(fileDao);
    }

    @Override
    public void delete(FileInfo fileInfo) {
        FileEntity fileEntity = fileInfoToFileEntity(fileInfo);
        mFileDao.deleteFiles(fileEntity);
    }

    @Override
    public void add(FileInfo fileInfo) {
        FileEntity fileEntity = fileInfoToFileEntity(fileInfo);
        mFileDao.insertFiles(fileEntity);
    }

    private static FileEntity fileInfoToFileEntity(@NonNull FileInfo file) {
        String uriString = file.getAbsolutePath();
        long date = file.getRawModifiedDate();
        String name = file.getName();
        int fileType = getFileType(name);
        String parent = ExternalFileInfo.getParentRelativePath(Uri.parse(uriString), name);
        long fileSize = file.getSize();
        return new FileEntity(uriString, parent, name, fileType, date, getDateString(date), fileSize);
    }
}
