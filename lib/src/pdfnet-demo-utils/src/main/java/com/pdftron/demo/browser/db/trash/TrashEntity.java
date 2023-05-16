package com.pdftron.demo.browser.db.trash;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "trash_table")
public class TrashEntity implements Comparable<TrashEntity> {

    @NonNull
    @PrimaryKey(autoGenerate = true)
    private Long id;

    @NonNull
    @ColumnInfo(name = "is_directory")
    private Boolean isDirectory;

    @NonNull
    @ColumnInfo(name = "is_external")
    private Boolean isExternal;

    @NonNull
    @ColumnInfo(name = "original_name")
    private String originalName;

    @NonNull
    @ColumnInfo(name = "trash_date")
    private Date trashDate;

    @NonNull
    @ColumnInfo(name = "trash_parent_path")
    private String trashParentPath;

    @NonNull
    @ColumnInfo(name = "file_size")
    private String fileSize;

    @NonNull
    public Long getId() {
        return id;
    }

    public void setId(@NonNull Long id) {
        this.id = id;
    }

    @NonNull
    public Boolean getIsDirectory() {
        return isDirectory;
    }

    public void setIsDirectory(@NonNull Boolean isDirectory) {
        this.isDirectory = isDirectory;
    }

    @NonNull
    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(@NonNull String originalName) {
        this.originalName = originalName;
    }

    @NonNull
    public Date getTrashDate() {
        return trashDate;
    }

    public void setTrashDate(@NonNull Date trashDate) {
        this.trashDate = trashDate;
    }

    @NonNull
    public Boolean getIsExternal() {
        return isExternal;
    }

    public void setIsExternal(@NonNull Boolean external) {
        isExternal = external;
    }

    @NonNull
    public String getTrashParentPath() {
        return trashParentPath;
    }

    public void setTrashParentPath(@NonNull String trashParentPath) {
        this.trashParentPath = trashParentPath;
    }

    @NonNull
    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(@NonNull String fileSize) {
        this.fileSize = fileSize;
    }

    @Override
    public int compareTo(TrashEntity otherTrash) {
        return -1 * getTrashDate().compareTo(otherTrash.getTrashDate());
    }
}
