package com.pdftron.demo.browser.db.file;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class FileEntity {
    @NonNull
    @PrimaryKey
    private String filePath;

    @NonNull
    private String fileParent;

    @NonNull
    private String filename;

    private int docType; // One of: 0 for pdf, 1 for office, 2 for images, 3 for text files. otherwise -1. See Constants for more info.

    private long date;

    private String dateString;

    private long size;

    public FileEntity(@NonNull String filePath, @NonNull String fileParent, @NonNull String filename, int docType, long date, String dateString, long size) {
        this.fileParent = fileParent;
        this.filePath = filePath;
        this.filename = filename;
        this.docType = docType;
        this.date = date;
        this.dateString = dateString;
        this.size = size;
    }

    @NonNull
    public String getFilePath() {
        return filePath;
    }

    @NonNull
    public String getFilename() {
        return filename;
    }

    @NonNull
    public String getFileParent() {
        return fileParent;
    }

    public int getDocType() {
        return docType;
    }

    public long getDate() {
        return date;
    }

    public String getDateString() {
        return dateString;
    }

    public long getSize() {
        return size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileEntity that = (FileEntity) o;

        return filePath.equals(that.filePath);
    }

    @Override
    public int hashCode() {
        return filePath.hashCode();
    }
}
