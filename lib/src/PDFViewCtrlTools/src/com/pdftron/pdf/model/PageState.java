package com.pdftron.pdf.model;

/**
 * Represents current page state.
 */
public class PageState {
    private final int mCurrentPage;

    public PageState(int currentPage) {
        this.mCurrentPage = currentPage;
    }

    public int getCurrentPage() {
        return mCurrentPage;
    }
}
