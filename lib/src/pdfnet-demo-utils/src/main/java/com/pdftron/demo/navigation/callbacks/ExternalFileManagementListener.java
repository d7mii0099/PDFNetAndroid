package com.pdftron.demo.navigation.callbacks;

import com.pdftron.demo.browser.db.trash.TrashEntity;
import com.pdftron.pdf.model.ExternalFileInfo;
import com.pdftron.pdf.model.FileInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface ExternalFileManagementListener {

    void onExternalFileRenamed(ExternalFileInfo oldFile, ExternalFileInfo newFile);

    void onExternalFileDuplicated(ExternalFileInfo fileCopy);

    void onExternalFileDeleted(ArrayList<ExternalFileInfo> deletedFiles);

    void onExternalFileTrashed(ArrayList<ExternalFileInfo> deletedFiles);

    void onExternalTrashRemoved(TrashEntity trashEntity);

    void onExternalTrashesLoaded(List<TrashEntity> trashEntityList);

    void onRootsRemoved(ArrayList<ExternalFileInfo> deletedFiles);

    void onExternalFileMoved(Map<ExternalFileInfo, Boolean> filesMoved, ExternalFileInfo targetFolder);

    void onExternalFileMoved(Map<ExternalFileInfo, Boolean> filesMoved, File targetFolder);

    void onExternalFolderCreated(ExternalFileInfo rootFolder, ExternalFileInfo newFolder);

    void onExternalFileMerged(ArrayList<FileInfo> mergedFiles, ArrayList<FileInfo> filesToDelete, FileInfo newFile);
}
