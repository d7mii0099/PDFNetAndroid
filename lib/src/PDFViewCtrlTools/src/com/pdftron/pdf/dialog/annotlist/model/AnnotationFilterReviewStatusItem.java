package com.pdftron.pdf.dialog.annotlist.model;

public class AnnotationFilterReviewStatusItem extends AnnotationFilterItem {

    private String title;
    private String tag;
    private boolean isSelected;

    public AnnotationFilterReviewStatusItem() {
    }

    public AnnotationFilterReviewStatusItem(String title, String tag, boolean isSelected, boolean isEnabled) {
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

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return this.tag;
    }

    public boolean isSelected() {
        return isSelected;
    }
}
