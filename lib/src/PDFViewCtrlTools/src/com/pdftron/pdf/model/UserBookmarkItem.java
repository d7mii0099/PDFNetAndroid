//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.model;

import android.content.Context;
import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.pdftron.pdf.Bookmark;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;

import org.json.JSONObject;

// IF YOU WANT TO CHANGE THIS FILE PLEASE READ THE NOTE BELOW FIRST

/**
 * Structure for user bookmark
 */
public class UserBookmarkItem {

    private static final String VAR_PAGE_NUMBER = "pageNumber";
    private static final String VAR_PAGE_OBJ_NUM = "pageObjNum";
    private static final String VAR_TITLE = "title";
    private static final String VAR_PDF_BOOKMARK = "pdfBookmark";

    // NOTE: PLEASE MAKE SURE YOU'LL MAKE NECESSARY CHANGES TO UserBookmarkItem(JSONObject) FUNCTION
    //       IF YOU INSERT NEW PARAMS OR CHANGE VARIABLE NAMES;
    //       OTHERWISE EXISTING USERS MAY LOSE ENTIRE DATA

    /**
     * Page number
     */
    public int pageNumber;
    /**
     * Page object number
     */
    public long pageObjNum;
    /**
     * Title
     */
    public String title;
    /**
     * Whether bookmark was edited
     */
    public boolean isBookmarkEdited;
    /**
     * PDF bookmark
     */
    public Bookmark pdfBookmark;

    /**
     * Whether this bookmark item was deleted
     */
    public boolean deleted;

    /**
     * Class constructor
     */
    public UserBookmarkItem() {

    }

    /**
     * Class constructor
     *
     * @param context    The context
     * @param pageObjNum The page obj number
     * @param pageNumber The page number
     */
    public UserBookmarkItem(@NonNull Context context, long pageObjNum, int pageNumber) {
        this.pageObjNum = pageObjNum;
        this.pageNumber = pageNumber;
        this.title = context.getString(R.string.controls_bookmark_dialog_default_title) + Integer.toString(pageNumber);
    }

    /**
     * Class constructor
     * <p>
     * Extracts fields from the specified JSON object
     *
     * @param jsonObject The JSON object
     */
    public UserBookmarkItem(JSONObject jsonObject) {
        try {
            pageNumber = jsonObject.getInt(VAR_PAGE_NUMBER);
            pageObjNum = jsonObject.getLong(VAR_PAGE_OBJ_NUM);
            title = jsonObject.getString(VAR_TITLE);
            if (jsonObject.has(VAR_PDF_BOOKMARK)) {
                JSONObject bookmarkObject = jsonObject.getJSONObject(VAR_PDF_BOOKMARK);
                Gson gson = new Gson();
                pdfBookmark = gson.fromJson(bookmarkObject.toString(), Bookmark.class);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e, "\nJson from: " + jsonObject);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserBookmarkItem that = (UserBookmarkItem) o;

        if (pageNumber != that.pageNumber) return false;
        if (pageObjNum != that.pageObjNum) return false;
        return title != null ? title.equals(that.title) : that.title == null;
    }

    @Override
    public int hashCode() {
        int result = pageNumber;
        result = 31 * result + (int) (pageObjNum ^ (pageObjNum >>> 32));
        result = 31 * result + (title != null ? title.hashCode() : 0);
        return result;
    }
}
