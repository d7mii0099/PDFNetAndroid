package com.pdftron.pdf.dialog.menueditor.model;

public interface MenuEditorItem {

    int GROUP_SHOW_IF_ROOM = 0;
    int GROUP_SHOW_NEVER = 1;

    /**
     * @return true if this item is a header
     */
    boolean isHeader();
}
