package com.pdftron.demo.browser.model;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.entity.AbstractExpandableItem;
import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.pdftron.demo.browser.ui.AllFilesListAdapter;
import com.pdftron.pdf.model.list.ListItem;

public class FolderItem extends AbstractExpandableItem<FileItem> implements MultiItemEntity, ListItem {

    @NonNull
    public final String filePath;

    private boolean collapsed;

    public FolderItem(@NonNull String filePath, boolean collapsed) {
        this.filePath = filePath;
        this.collapsed = collapsed;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean isCollapsed) {
        collapsed = isCollapsed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        FolderItem that = (FolderItem) o;

        return collapsed == that.collapsed;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (collapsed ? 1 : 0);
        return result;
    }

    @Override
    public int getItemType() {
        return AllFilesListAdapter.VIEW_TYPE_HEADER;
    }

    @Override
    public int getLevel() {
        return 1;
    }

    @Override
    public boolean isHeader() {
        return true;
    }
}

