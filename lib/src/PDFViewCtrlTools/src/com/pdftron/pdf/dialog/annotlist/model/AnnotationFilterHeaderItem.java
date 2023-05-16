package com.pdftron.pdf.dialog.annotlist.model;

import java.util.ArrayList;

public class AnnotationFilterHeaderItem extends AnnotationFilterItem {

    private String title;

    public AnnotationFilterHeaderItem() {
    }

    public AnnotationFilterHeaderItem(String title) {
        this.title = title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return this.title;
    }
}
