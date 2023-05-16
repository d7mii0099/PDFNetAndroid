package com.pdftron.pdf.viewmodel;

import android.util.Pair;
import androidx.annotation.NonNull;

import com.pdftron.pdf.TextSearchResult;

import java.util.ArrayList;

public class RedactionEvent {
    private final Type mType;
    private ArrayList<Integer> mPages = new ArrayList<>();
    private ArrayList<Pair<Integer, ArrayList<Double>>> mSearchResults = new ArrayList<>();
    private TextSearchResult mSelectedItem;

    RedactionEvent(@NonNull Type eventType) {
        this.mType = eventType;
    }

    public void setPages(@NonNull ArrayList<Integer> pages) {
        mPages.clear();
        mPages.addAll(pages);
    }

    public void setSearchResults(@NonNull ArrayList<Pair<Integer, ArrayList<Double>>> searchResults) {
        mSearchResults.clear();
        mSearchResults.addAll(searchResults);
    }

    public void setSelectedItem(@NonNull TextSearchResult selectedItem) {
        mSelectedItem = selectedItem;
    }

    @NonNull
    public Type getEventType() {
        return mType;
    }

    @NonNull
    public ArrayList<Integer> getPages() {
        return mPages;
    }

    @NonNull
    public ArrayList<Pair<Integer, ArrayList<Double>>> getSearchResults() {
        return mSearchResults;
    }

    public enum Type {
        REDACT_BY_PAGE,
        REDACT_BY_SEARCH,
        REDACT_BY_SEARCH_OPEN_SHEET,
        REDACT_BY_SEARCH_ITEM_CLICKED,
        REDACT_BY_SEARCH_CLOSE_CLICKED
    }
}
