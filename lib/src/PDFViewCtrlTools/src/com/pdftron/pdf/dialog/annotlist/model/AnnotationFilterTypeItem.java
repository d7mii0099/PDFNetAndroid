package com.pdftron.pdf.dialog.annotlist.model;

public class AnnotationFilterTypeItem extends AnnotationFilterItem {
    private String title;
    private int tag;
    private boolean isSelected;

    public AnnotationFilterTypeItem() {
    }

    public AnnotationFilterTypeItem(String title, int tag, boolean isSelected, boolean isEnabled) {
        this.title = title;
        this.tag = tag;
        this.isSelected = isSelected;
        super.isEnabled = isEnabled;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public int getTag() {
        return this.tag;
    }

    public boolean isSelected() {
        return isSelected;
    }
}
