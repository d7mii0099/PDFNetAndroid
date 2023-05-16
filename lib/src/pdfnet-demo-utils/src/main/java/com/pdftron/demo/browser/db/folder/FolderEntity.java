package com.pdftron.demo.browser.db.folder;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity
public class FolderEntity {
    @NonNull
    @PrimaryKey
    private String folderPath;

    private boolean isCollapsed; // by default is false

    public FolderEntity(@NonNull String folderPath, boolean isCollapsed) {
        this.folderPath = folderPath;
        this.isCollapsed = isCollapsed;
    }

    @NonNull
    public String getFolderPath() {
        return folderPath;
    }

    public boolean isCollapsed() {
        return isCollapsed;
    }
}
