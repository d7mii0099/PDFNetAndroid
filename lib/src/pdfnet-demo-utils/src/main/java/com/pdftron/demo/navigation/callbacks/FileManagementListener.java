package com.pdftron.demo.navigation.callbacks;

import com.pdftron.demo.browser.db.trash.TrashEntity;
import com.pdftron.pdf.model.ExternalFileInfo;
import com.pdftron.pdf.model.FileInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface FileManagementListener {

    void onFileRenamed(FileInfo oldFile, FileInfo newFile);

    void onFileDuplicated(File fileCopy);

    void onFileDeleted(ArrayList<FileInfo> deletedFiles);

    void onFileTrashed(ArrayList<FileInfo> deletedFiles);

    void onTrashRemoved(TrashEntity trashEntity);

    void onTrashesLoaded(List<TrashEntity> trashEntityList);

    void onFileMoved(Map<FileInfo, Boolean> filesMoved, File targetFolder);

    void onFileMoved(Map<FileInfo, Boolean> filesMoved, ExternalFileInfo targetFolder);

    void onFolderCreated(FileInfo rootFolder, FileInfo newFolder);

    void onFileMerged(ArrayList<FileInfo> mergedFiles, ArrayList<FileInfo> filesToDelete, FileInfo newFile);

    void onFileChanged(String path, int event);

    void onFileClicked(FileInfo fileInfo);
}
