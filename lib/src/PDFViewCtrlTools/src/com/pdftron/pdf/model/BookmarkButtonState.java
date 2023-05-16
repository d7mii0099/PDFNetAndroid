package com.pdftron.pdf.model;

/**
 * State representing the quick bookmark button.
 */
public class BookmarkButtonState {

    private boolean mSelected = false;

    public boolean isSelected() {
        return mSelected;
    }

    public BookmarkButtonState(boolean selected) {
        this.mSelected = selected;
    }
}
