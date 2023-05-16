package com.pdftron.demo.browser.model;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.pdftron.demo.browser.ui.AllFilesListAdapter;
import com.pdftron.pdf.model.list.ListItem;

public class FileItem implements MultiItemEntity, ListItem {

    @NonNull
    public final String filePath;
    @NonNull
    public final String filename;
    @NonNull
    public final String fileParent;

    public final int docType; // One of: 0 for pdf, 1 for office, 2 for images

    public final long date;
    public final String dateString;
    public final long size;

    public boolean isSecured;

    public boolean isPackage;

    public FileItem(@NonNull String filePath,
            @NonNull String fileParent,
            @NonNull String filename,
            int docType,
            long date,
            @NonNull String dateString,
            long size,
            boolean isSecured,
            boolean isPackage) {
        this.filePath = filePath;
        this.fileParent = fileParent;
        this.filename = filename;
        this.docType = docType;
        this.date = date;
        this.dateString = dateString;
        this.size = size;
        this.isSecured = isSecured;
        this.isPackage = isPackage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        FileItem fileItem = (FileItem) o;

        if (docType != fileItem.docType) return false;
        if (date != fileItem.date) return false;
        if (isSecured != fileItem.isSecured) return false;
        if (!filename.equals(fileItem.filename)) return false;
        return fileParent != null ? fileParent.equals(fileItem.fileParent) : fileItem.fileParent == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + filename.hashCode();
        result = 31 * result + (fileParent != null ? fileParent.hashCode() : 0);
        result = 31 * result + docType;
        result = 31 * result + (int) (date ^ (date >>> 32));
        result = 31 * result + (isSecured ? 1 : 0);
        return result;
    }

    @Override
    public int getItemType() {
        return AllFilesListAdapter.VIEW_TYPE_CONTENT;
    }

    @Override
    public boolean isHeader() {
        return false;
    }
}
