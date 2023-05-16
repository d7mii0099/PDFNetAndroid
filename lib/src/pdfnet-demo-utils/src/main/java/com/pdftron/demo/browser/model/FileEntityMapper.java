package com.pdftron.demo.browser.model;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.pdftron.demo.browser.db.file.FileEntity;
import com.pdftron.demo.browser.db.folder.FolderDatabase;
import com.pdftron.demo.browser.db.folder.FolderEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FileEntityMapper {

    @Nullable
    private final List<FileEntity> mEntities;

    public FileEntityMapper(@Nullable List<FileEntity> entities) {
        mEntities = entities;
    }

    @NonNull
    public List<MultiItemEntity> fromFileEntitiesToItems() {
        List<MultiItemEntity> files = new ArrayList<>();
        if (mEntities != null) {
            for (FileEntity fileEntity : mEntities) {
                files.add(createFileItemFromEntitiy(fileEntity));
            }
        }
        return files;
    }

    @NonNull
    public List<MultiItemEntity> fromFileEntitiesToGroups(@NonNull Context context) {
        List<FileItem> files = fromFileEntitiesToFileItems();
        return getItemGroups(context, files);
    }

    @NonNull
    private List<FileItem> fromFileEntitiesToFileItems() {
        List<FileItem> files = new ArrayList<>();
        if (mEntities != null) {
            for (FileEntity fileEntity : mEntities) {
                files.add(createFileItemFromEntitiy(fileEntity));
            }
        }
        return files;
    }

    private FileItem createFileItemFromEntitiy(@NonNull FileEntity fileEntity) {
        // Get all required parameters
        String filePath = fileEntity.getFilePath();
        String fileParent = fileEntity.getFileParent();
        String fileName = fileEntity.getFilename();
        int type = fileEntity.getDocType();
        long date = fileEntity.getDate();
        String dateString = fileEntity.getDateString();
        long size = fileEntity.getSize();

        return new FileItem(filePath, fileParent, fileName, type, date, dateString, size, false, false);
    }

    private static List<MultiItemEntity> getItemGroups(@NonNull Context context, @NonNull List<FileItem> items) {
        // Group items
        HashMap<String, List<FileItem>> groupMap = new HashMap<>();
        List<String> keys = new ArrayList<>();
        for (FileItem item : items) {
            String key = item.fileParent;
            if (!groupMap.containsKey(key)) {
                List<FileItem> group = new ArrayList<>();
                group.add(item);
                groupMap.put(key, group);
                keys.add(key);
            } else {
                List<FileItem> group = groupMap.get(key);
                if (group != null) {
                    group.add(item);
                }
            }
        }

        // Create list of list items from grouped items
        List<MultiItemEntity> result = new ArrayList<>();
        for (String key : keys) {
            List<FileItem> groupItems = groupMap.get(key);
            if (groupItems != null && !groupItems.isEmpty()) {
                List<FolderEntity> dbResult = FolderDatabase.getInstance(context).folderDao().getFolder(key);
                boolean isCollapsed = false;
                if (dbResult.size() == 1) {
                    isCollapsed = dbResult.get(0).isCollapsed();
                }
                FolderItem folderItem = new FolderItem(key, isCollapsed);
                for (FileItem item : groupItems) {
                    folderItem.addSubItem(item);
                }
                result.add(folderItem);
            }
        }

        return result;
    }
}
