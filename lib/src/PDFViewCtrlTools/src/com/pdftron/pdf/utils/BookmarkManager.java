//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Action;
import com.pdftron.pdf.Bookmark;
import com.pdftron.pdf.Destination;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.model.UserBookmarkItem;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.sdf.Obj;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A utility class for handling bookmarks in PDF
 */
public class BookmarkManager {

    private static final String TAG = BookmarkManager.class.getName();

    private static final String PREFS_CONTROLS_FILE_NAME = "com_pdftron_pdfnet_pdfviewctrl_controls_prefs_file";
    private static final String KEY_PREF_USER_BOOKMARK = "user_bookmarks_key";
    private static final String KEY_PREF_USER_BOOKMARK_OBJ_TITLE = "pdftronUserBookmarks";

    /**
     * Converts user bookmarks json to list of {@link UserBookmarkItem}
     *
     * @param bookmarkJson the user bookmark json in format: {"0":"Bookmark 1","1":"Bookmark 2"}
     * @return the list of {@link UserBookmarkItem}
     */
    public static ArrayList<UserBookmarkItem> fromJSON(String bookmarkJson) throws JSONException {
        JSONObject jsonObject = new JSONObject(bookmarkJson);
        ArrayList<UserBookmarkItem> bookmarkItems = new ArrayList<>();

        Iterator<String> keys = jsonObject.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            try {
                if (jsonObject.get(key) instanceof String) {
                    String title = jsonObject.getString(key);
                    int pageIndex = Integer.parseInt(key);

                    UserBookmarkItem item = new UserBookmarkItem();
                    item.pageNumber = pageIndex + 1; // 0-indexed
                    item.title = title;

                    bookmarkItems.add(item);
                }
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
            }
        }
        return bookmarkItems;
    }

    /**
     * Converts list of {@link UserBookmarkItem} to user bookmarks json in format: {"0":"Bookmark 1","1":"Bookmark 2"}
     *
     * @param bookmarkItems the list of {@link UserBookmarkItem}
     * @return the user bookmark json in format:
     */
    public static String toJSON(List<UserBookmarkItem> bookmarkItems) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        for (UserBookmarkItem item : bookmarkItems) {
            int pageIndex = item.pageNumber - 1; // 0-indexed
            jsonObject.put(String.valueOf(pageIndex), item.title);
        }
        return jsonObject.toString();
    }

    // START Bookmark using Bookmark object

    /**
     * Imports user bookmark json to a document
     * This will save the user bookmarks to the associated {@link PDFDoc}
     *
     * @param pdfViewCtrl  the {@link PDFViewCtrl} associated with the document
     * @param bookmarkJson the user bookmark json in format: {"0":"Bookmark 1","1":"Bookmark 2"}
     */
    public static void importPdfBookmarks(PDFViewCtrl pdfViewCtrl, String bookmarkJson) throws JSONException {
        ArrayList<UserBookmarkItem> bookmarkItems = fromJSON(bookmarkJson);
        // Sort the bookmarks by page number, to match WebViewer behavior.
        Collections.sort(bookmarkItems, new Comparator<UserBookmarkItem>() {
            @Override
            public int compare(UserBookmarkItem o1, UserBookmarkItem o2) {
                //noinspection UseCompareMethod
                return Integer.valueOf(o1.pageNumber).compareTo(o2.pageNumber);
            }
        });
        savePdfBookmarks(pdfViewCtrl, bookmarkItems, false, true);
    }

    /**
     * Exports user bookmark json from a document
     *
     * @param pdfDoc the document
     * @return the user bookmark json in format: {"0":"Bookmark 1","1":"Bookmark 2"}
     */
    public static String exportPdfBookmarks(PDFDoc pdfDoc) throws JSONException {
        List<UserBookmarkItem> bookmarkItems = getPdfBookmarks(pdfDoc, null);
        return toJSON(bookmarkItems);
    }

    /**
     * Returns the root PDF bookmark
     *
     * @param pdfDoc    The PDFDoc
     * @param createNew True if should create new bookmark object if doc doesn't have any
     * @return The root PDF bookmark
     */
    public static Bookmark getRootPdfBookmark(PDFDoc pdfDoc, boolean createNew) {
        Bookmark bookmark = null;
        if (null != pdfDoc) {
            boolean shouldUnlockRead = false;
            try {
                pdfDoc.lockRead();
                shouldUnlockRead = true;
                Obj catalog = pdfDoc.getRoot();
                Obj bookmark_obj = catalog.findObj(KEY_PREF_USER_BOOKMARK_OBJ_TITLE);
                if (null != bookmark_obj) {
                    // found existing bookmark obj
                    bookmark = new Bookmark(bookmark_obj);
                } else {
                    if (createNew) {
                        // create new bookmark obj
                        bookmark = Bookmark.create(pdfDoc, KEY_PREF_USER_BOOKMARK_OBJ_TITLE);
                        pdfDoc.getRoot().put(KEY_PREF_USER_BOOKMARK_OBJ_TITLE, bookmark.getSDFObj());
                    }
                }
            } catch (PDFNetException e) {
                bookmark = null;
            } finally {
                if (shouldUnlockRead) {
                    Utils.unlockReadQuietly(pdfDoc);
                }
            }
        }
        return bookmark;
    }

    /**
     * Returns array of integer indicate page number that contains bookmark
     */
    public static ArrayList<Integer> getPdfBookmarkedPageNumbers(PDFDoc pdfDoc) {
        List<UserBookmarkItem> bookmarkItems = getPdfBookmarks(pdfDoc, null);
        Set<Integer> pages = new HashSet<>();
        for (UserBookmarkItem item : bookmarkItems) {
            pages.add(item.pageNumber);
        }
        return new ArrayList<>(pages);
    }

    /**
     * Returns pdf bookmarks.
     *
     * @param pdfDoc the document
     * @return A list of user bookmarks
     */
    public static List<UserBookmarkItem> getPdfBookmarks(PDFDoc pdfDoc) {
        Bookmark bookmark = getRootPdfBookmark(pdfDoc, false);
        return getPdfBookmarks(bookmark, null);
    }

    /**
     * Returns pdf bookmarks.
     *
     * @param pdfDoc    the document
     * @param queryText The string used to filter bookmarks by title
     * @return A list of user bookmarks
     */
    public static List<UserBookmarkItem> getPdfBookmarks(PDFDoc pdfDoc, String queryText) {
        Bookmark bookmark = getRootPdfBookmark(pdfDoc, false);
        return getPdfBookmarks(bookmark, queryText);
    }

    /**
     * Returns pdf bookmarks.
     *
     * @param rootBookmark The root PDF bookmark
     * @return A list of user bookmarks
     */
    public static List<UserBookmarkItem> getPdfBookmarks(Bookmark rootBookmark) {
        return getPdfBookmarks(rootBookmark, null);
    }

    /**
     * Returns pdf bookmarks.
     *
     * @param rootBookmark The root PDF bookmark
     * @param queryText    The string used to filter bookmarks by title
     * @return A list of user bookmarks
     */
    public static List<UserBookmarkItem> getPdfBookmarks(Bookmark rootBookmark, String queryText) {
        ArrayList<UserBookmarkItem> data = new ArrayList<>();
        if (null != rootBookmark) {
            try {
                if (rootBookmark.hasChildren()) {
                    Bookmark item = rootBookmark.getFirstChild();
                    for (; item.isValid(); item = item.getNext()) {
                        UserBookmarkItem bookmarkItem = new UserBookmarkItem();
                        bookmarkItem.isBookmarkEdited = false;
                        bookmarkItem.pdfBookmark = item;
                        bookmarkItem.title = item.getTitle();
                        Action action = item.getAction();
                        if (null != action && action.isValid()) {
                            if (action.getType() == Action.e_GoTo) {
                                Destination dest = action.getDest();
                                if (null != dest && dest.isValid()) {
                                    bookmarkItem.pageNumber = dest.getPage().getIndex();
                                    bookmarkItem.pageObjNum = dest.getPage().getSDFObj().getObjNum();
                                    if (queryText != null && !queryText.isEmpty()) {
                                        if (bookmarkItem.title.toLowerCase().contains(queryText.toLowerCase())) {
                                            data.add(bookmarkItem);
                                        }
                                    } else {
                                        data.add(bookmarkItem);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (PDFNetException ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
                Log.e("PDFNet", ex.getMessage());
            }
        }
        return data;
    }

    /**
     * Saves PDF bookmarks.
     *
     * @param pdfViewCtrl            the PDFViewCtrl
     * @param data                   A list of user bookmarks
     * @param shouldTakeUndoSnapshot True if should take undo snapshot
     * @param rebuild                True if should rebuild the root bookmark
     */
    public static void savePdfBookmarks(PDFViewCtrl pdfViewCtrl, List<UserBookmarkItem> data, boolean shouldTakeUndoSnapshot, boolean rebuild) {
        if (pdfViewCtrl == null) {
            return;
        }

        final PDFDoc pdfDoc = pdfViewCtrl.getDoc();
        if (pdfDoc == null) {
            return;
        }

        if (data.size() > 0) {
            Bookmark rootBookmark = getRootPdfBookmark(pdfDoc, true);
            Bookmark firstBookmark = null;
            Bookmark currentBookmark = null;

            if (null != rootBookmark) {
                boolean hasChange = false;
                boolean shouldUnlock = false;
                try {
                    if (rootBookmark.hasChildren()) {
                        firstBookmark = rootBookmark.getFirstChild();
                    }
                    pdfDoc.lock();
                    shouldUnlock = true;

                    if (rebuild) {
                        Obj catalog = pdfDoc.getRoot();
                        if (catalog != null) {
                            catalog.erase(KEY_PREF_USER_BOOKMARK_OBJ_TITLE);
                        }
                        rootBookmark = getRootPdfBookmark(pdfDoc, true);
                        firstBookmark = null;
                    }
                    for (UserBookmarkItem item : data) {
                        if (null == item.pdfBookmark) {
                            if (null == currentBookmark) {
                                // No items in the list above this on are currently in the document
                                if (null == firstBookmark) {
                                    // this means there are no bookmarks at all, so create one.
                                    currentBookmark = rootBookmark.addChild(item.title);
                                    currentBookmark.setAction(Action.createGoto(Destination.createFit(pdfDoc.getPage(item.pageNumber))));
                                    firstBookmark = currentBookmark;
                                } else {
                                    // there already are bookmarks, so the new bookmark needs to be inserted in front of the first one
                                    currentBookmark = firstBookmark.addPrev(item.title);
                                    currentBookmark.setAction(Action.createGoto(Destination.createFit(pdfDoc.getPage(item.pageNumber))));
                                    firstBookmark = currentBookmark;
                                }
                            } else {
                                // at least one item in the list above the current item was in the list
                                currentBookmark = currentBookmark.addNext(item.title);
                                currentBookmark.setAction(Action.createGoto(Destination.createFit(pdfDoc.getPage(item.pageNumber))));
                            }
                            item.pdfBookmark = currentBookmark;
                        } else {
                            currentBookmark = item.pdfBookmark;
                            if (item.isBookmarkEdited) {
                                Action action = item.pdfBookmark.getAction();
                                Destination dest = action.getDest();
                                dest.setPage(pdfDoc.getPage(item.pageNumber));
                                item.pdfBookmark.setTitle(item.title);
                            }
                        }
                    }
                    hasChange = pdfDoc.hasChangesSinceSnapshot();
                } catch (Exception ex) {
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                    Log.e("PDFNet", ex.getMessage());
                } finally {
                    if (shouldUnlock) {
                        Utils.unlockQuietly(pdfDoc);
                    }
                }

                if (shouldTakeUndoSnapshot && hasChange) {
                    ToolManager toolManager = (ToolManager) pdfViewCtrl.getToolManager();
                    if (toolManager != null) {
                        // Create the list of existing bookmark items to pass to listener
                        ArrayList<UserBookmarkItem> bookmarks = new ArrayList<>();
                        for (UserBookmarkItem item : data) {
                            if (!item.deleted) {
                                bookmarks.add(item);
                            }
                        }
                        toolManager.raiseBookmarkModified(bookmarks);
                    }
                }
            }
        } else {
            removeRootPdfBookmark(pdfViewCtrl, shouldTakeUndoSnapshot);
        }
    }

    /**
     * Removes the root PDF bookmark.
     *
     * @param pdfViewCtrl            The PDFViewCtrl
     * @param shouldTakeUndoSnapshot True if should take undo snapshot
     * @return True if the root PDF bookmark is removed.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean removeRootPdfBookmark(PDFViewCtrl pdfViewCtrl, boolean shouldTakeUndoSnapshot) {
        if (pdfViewCtrl == null) {
            return false;
        }

        final PDFDoc pdfDoc = pdfViewCtrl.getDoc();
        if (pdfDoc == null) {
            return false;
        }

        boolean hasChange;
        boolean shouldUnlock = false;
        try {
            pdfDoc.lock();
            shouldUnlock = true;
            Obj catalog = pdfDoc.getRoot();
            if (catalog != null) {
                catalog.erase(KEY_PREF_USER_BOOKMARK_OBJ_TITLE);
            }
            hasChange = pdfDoc.hasChangesSinceSnapshot();
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            return false;
        } finally {
            if (shouldUnlock) {
                Utils.unlockQuietly(pdfDoc);
            }
        }

        if (shouldTakeUndoSnapshot && hasChange) {
            ToolManager toolManager = (ToolManager) pdfViewCtrl.getToolManager();
            if (toolManager != null) {
                toolManager.raiseBookmarkModified(new ArrayList<>());
            }
        }

        return true;
    }

    /**
     * Adds a PDF bookmark.
     *
     * @param context     The context
     * @param pdfViewCtrl The PDFViewCtrl
     * @param pageObjNum  The page object number
     * @param pageNumber  The page number
     */
    public static void addPdfBookmark(Context context, PDFViewCtrl pdfViewCtrl, long pageObjNum, int pageNumber) {
        if (context == null || pdfViewCtrl == null) {
            return;
        }

        final PDFDoc pdfDoc = pdfViewCtrl.getDoc();
        if (pdfDoc == null) {
            return;
        }

        List<UserBookmarkItem> bookmarks = getPdfBookmarks(getRootPdfBookmark(pdfDoc, true), null);
        UserBookmarkItem item = new UserBookmarkItem(context, pageObjNum, pageNumber);
        if (!bookmarks.contains(item)) {
            bookmarks.add(item);
            savePdfBookmarks(pdfViewCtrl, bookmarks, true, false);
        }
    }

    /**
     * Removes a PDF bookmark. A write lock is expected around this call.
     *
     * @param context     The context
     * @param pdfViewCtrl The PDFViewCtrl
     * @param pageObjNum  The page object number
     * @param pageNumber  The page number
     */
    public static void removePdfBookmark(Context context, PDFViewCtrl pdfViewCtrl, long pageObjNum, int pageNumber) throws PDFNetException {
        if (context == null || pdfViewCtrl == null) {
            return;
        }

        final PDFDoc pdfDoc = pdfViewCtrl.getDoc();
        if (pdfDoc == null) {
            return;
        }

        List<UserBookmarkItem> bookmarks = getPdfBookmarks(pdfDoc, null);
        boolean found = false;
        for (UserBookmarkItem item : bookmarks) {
            if (item.pageNumber == pageNumber && item.pageObjNum == pageObjNum) {
                if (item.pdfBookmark != null) {
                    item.pdfBookmark.delete();
                    item.deleted = true;
                    found = true;
                    break;
                }
            }
        }
        if (found) {
            savePdfBookmarks(pdfViewCtrl, bookmarks, true, false);
        }
    }

    /**
     * Handles bookmarks when a pdf bookmark is deleted.
     *
     * @param pdfViewCtrl The PDFViewCtrl
     * @param objNumber   The object number
     */
    public static void onPageDeleted(PDFViewCtrl pdfViewCtrl, Long objNumber) {
        // Note no need to take snapshot here, because snapshot should take place when page is
        //      deleted rather than when its bookmarks are deleted
        if (pdfViewCtrl == null) {
            return;
        }

        final PDFDoc pdfDoc = pdfViewCtrl.getDoc();
        if (pdfDoc == null) {
            return;
        }

        List<UserBookmarkItem> items = getPdfBookmarks(getRootPdfBookmark(pdfDoc, false), null);
        boolean shouldUnlock = false;
        try {
            pdfDoc.lock();
            shouldUnlock = true;
            for (UserBookmarkItem item : items) {
                if (item.pageObjNum == objNumber) {
                    item.pdfBookmark.delete();
                }
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                Utils.unlockQuietly(pdfDoc);
            }
        }
    }

    /**
     * Handles bookmarks when a pdf bookmarks is moved.
     *
     * @param pdfViewCtrl   The PDFViewCtrl
     * @param objNumber     The object number before moving
     * @param newObjNumber  The object number after moving
     * @param newPageNumber The new page number
     * @param rebuild       True if should rebuild the root bookmark
     */
    public static void onPageMoved(PDFViewCtrl pdfViewCtrl, long objNumber, long newObjNumber, int newPageNumber, @SuppressWarnings("SameParameterValue") boolean rebuild) {
        // Note no need to take snapshot here, because snapshot should take place when page is
        //      moved rather than when bookmarks are changed
        if (pdfViewCtrl == null) {
            return;
        }

        final PDFDoc pdfDoc = pdfViewCtrl.getDoc();
        if (pdfDoc == null) {
            return;
        }

        List<UserBookmarkItem> items = getPdfBookmarks(getRootPdfBookmark(pdfDoc, false), null);
        boolean shouldUnlock = false;
        try {
            pdfDoc.lock();
            shouldUnlock = true;
            for (UserBookmarkItem item : items) {
                if (item.pageObjNum == objNumber) {
                    item.pageObjNum = newObjNumber;
                    item.pageNumber = newPageNumber;
                    item.pdfBookmark.delete();
                    item.pdfBookmark = null;
                    break;
                }
            }
            savePdfBookmarks(pdfViewCtrl, items, false, rebuild);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                Utils.unlockQuietly(pdfDoc);
            }
        }
    }

    /**
     * Returns the list of sibling bookmarks after the specified bookmark.
     * Note: if the specified bookmark is null the sibling bookmarks after the first bookmark in
     * the document will be returned. If the specified bookmark is null and there is no sibling
     * then all bookmarks under the first bookmark will be returned.
     *
     * @param pdfDoc         The PDF doc
     * @param queryText      The string used to filter bookmarks by title
     * @param isSearchActive Boolean to indicate if search is active
     * @return The list of sibling bookmarks after the specified bookmark
     */
    @NonNull
    public static ArrayList<Bookmark> getBookmarkListByTitle(@NonNull PDFDoc pdfDoc, String queryText, boolean isSearchActive) {
        ArrayList<Bookmark> bookmarkList = new ArrayList<>();
        boolean shouldUnlockRead = false;
        try {
            pdfDoc.lockRead();
            shouldUnlockRead = true;
            Bookmark rootBookmark = pdfDoc.getFirstBookmark();

            if (rootBookmark != null) {
                if (rootBookmark.isValid()) {
                    Bookmark item = rootBookmark;
                    while (item.isValid()) {
                        if (queryText != null && isSearchActive) {
                            if (item.getTitle().toLowerCase().contains(queryText.toLowerCase())) {
                                bookmarkList.add(item);
                            }
                            if (item.hasChildren()) {
                                ArrayList<Bookmark> children = getChildBookmarksByTitle(pdfDoc, getBookmarkList(pdfDoc, item.getFirstChild()), queryText);
                                if (children.size() > 0) {
                                    bookmarkList.addAll(children);
                                }
                            }
                        } else {
                            bookmarkList.add(item);
                        }
                        item = item.getNext();
                    }
                }
            }
        } catch (PDFNetException e) {
            bookmarkList.clear();
        } finally {
            if (shouldUnlockRead) {
                Utils.unlockReadQuietly(pdfDoc);
            }
        }

        return bookmarkList;
    }

    private static ArrayList<Bookmark> getChildBookmarksByTitle(@NonNull PDFDoc pdfDoc, @NonNull ArrayList<Bookmark> childBookmarks, String queryText) throws PDFNetException {
        ArrayList<Bookmark> filteredList = new ArrayList<>();
        for (Bookmark child : childBookmarks) {
            if (child.getTitle().toLowerCase().contains(queryText.toLowerCase())) {
                filteredList.add(child);
            }
            if (child.hasChildren()) {
                ArrayList<Bookmark> children = getChildBookmarksByTitle(pdfDoc, getBookmarkList(pdfDoc, child.getFirstChild()), queryText);
                if (children.size() > 0) {
                    filteredList.addAll(children);
                }
            }
        }
        return filteredList;
    }

    /**
     * Returns the list of sibling bookmarks after the specified bookmark.
     * Note: if the specified bookmark is null the sibling bookmarks after the first bookmark in
     * the document will be returned. If the specified bookmark is null and there is no sibling
     * then all bookmarks under the first bookmark will be returned.
     *
     * @param pdfDoc       The PDF doc
     * @param firstSibling The first sibling
     * @return The list of sibling bookmarks after the specified bookmark
     */
    @NonNull
    public static ArrayList<Bookmark> getBookmarkList(@NonNull PDFDoc pdfDoc, @Nullable Bookmark firstSibling) {
        ArrayList<Bookmark> bookmarkList = new ArrayList<>();
        Bookmark current;
        boolean shouldUnlockRead = false;

        try {
            pdfDoc.lockRead();
            shouldUnlockRead = true;
            if (firstSibling == null || !firstSibling.isValid()) {
                current = pdfDoc.getFirstBookmark();
            } else {
                current = firstSibling;
            }

            while (current.isValid()) {
                bookmarkList.add(current);
                current = current.getNext();
            }
        } catch (PDFNetException e) {
            bookmarkList.clear();
        } finally {
            if (shouldUnlockRead) {
                Utils.unlockReadQuietly(pdfDoc);
            }
        }

        return bookmarkList;
    }
    // END Bookmark using Bookmark object

    // START Bookmark using SharedPreferences

    /**
     * ONLY USE WHEN DOCUMENT CANNOT BE ALTERED
     * Imports user bookmark json to SharedPreferences
     * This will NOT alter the document
     *
     * @param context      the Context
     * @param filePath     the file path
     * @param bookmarkJson the user bookmark json in format: {"0":"Bookmark 1","1":"Bookmark 2"}
     */
    @Deprecated
    public static void importUserBookmarks(Context context, String filePath, String bookmarkJson) throws JSONException {
        importUserBookmarks(context, null, filePath, bookmarkJson);
    }


    /**
     * ONLY USE WHEN DOCUMENT CANNOT BE ALTERED
     * Imports user bookmark json to SharedPreferences
     * This will NOT alter the document
     *
     * @param context      the Context
     * @param pdfViewCtrl  the PDFViewCtrl
     * @param filePath     the file path
     * @param bookmarkJson the user bookmark json in format: {"0":"Bookmark 1","1":"Bookmark 2"}
     */
    public static void importUserBookmarks(Context context, @Nullable PDFViewCtrl pdfViewCtrl, String filePath, String bookmarkJson) throws JSONException {
        ArrayList<UserBookmarkItem> bookmarkItems = fromJSON(bookmarkJson);
        saveUserBookmarks(context, pdfViewCtrl, filePath, bookmarkItems);
    }

    /**
     * ONLY USE WHEN DOCUMENT CANNOT BE ALTERED
     * Exports user bookmark json from a document
     *
     * @param context  the Context
     * @param filePath the file path
     * @return the user bookmark json in format: {"0":"Bookmark 1","1":"Bookmark 2"}
     */
    public static String exportUserBookmarks(@NonNull Context context, @NonNull String filePath) throws JSONException {
        List<UserBookmarkItem> bookmarkItems = getUserBookmarks(context, filePath, null);
        return toJSON(bookmarkItems);
    }

    /**
     * ONLY USE WHEN DOCUMENT CANNOT BE ALTERED
     * Returns user bookmarks.
     *
     * @param context  The context
     * @param filePath The file path
     * @return a list of user bookmarks
     */
    public static List<UserBookmarkItem> getUserBookmarks(@Nullable Context context, String filePath) {
        return getUserBookmarks(context, filePath, null);
    }

    /**
     * ONLY USE WHEN DOCUMENT CANNOT BE ALTERED
     * Returns user bookmarks.
     *
     * @param context   The context
     * @param filePath  The file path
     * @param queryText The string used to filter bookmarks by title
     * @return a list of user bookmarks
     */
    public static List<UserBookmarkItem> getUserBookmarks(@Nullable Context context, String filePath, String queryText) {
        ArrayList<UserBookmarkItem> userBookmarks = new ArrayList<>();
        if (context == null) {
            return userBookmarks;
        }
        SharedPreferences settings = context.getSharedPreferences(PREFS_CONTROLS_FILE_NAME, 0);
        if (settings != null) {
            String serializedDocs = settings.getString(KEY_PREF_USER_BOOKMARK + filePath, "");
            if (!Utils.isNullOrEmpty(serializedDocs)) {
                try {
                    JSONArray jsonArray = new JSONArray(serializedDocs);
                    int count = jsonArray.length();
                    for (int i = 0; i < count; ++i) {
                        UserBookmarkItem bookmarkItem = null;
                        try {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            bookmarkItem = new UserBookmarkItem(jsonObject);
                        } catch (Exception e) {
                            AnalyticsHandlerAdapter.getInstance().sendException(e);
                        }
                        if (bookmarkItem != null) {
                            if (queryText != null && !queryText.isEmpty()) {
                                if (bookmarkItem.title.toLowerCase().contains(queryText.toLowerCase())) {
                                    userBookmarks.add(bookmarkItem);
                                }
                            } else {
                                userBookmarks.add(bookmarkItem);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        }

        return userBookmarks;
    }

    /**
     * ONLY USE WHEN DOCUMENT CANNOT BE ALTERED
     * Saves user bookmarks.
     *
     * @param context  The context
     * @param filePath The file path
     * @param data     The user bookmarks
     */
    @Deprecated
    public static void saveUserBookmarks(Context context, String filePath, List<UserBookmarkItem> data) {
        saveUserBookmarks(context, null, filePath, data);
    }

    /**
     * ONLY USE WHEN DOCUMENT CANNOT BE ALTERED
     * Saves user bookmarks.
     *
     * @param context  The context
     * @param filePath The file path
     * @param data     The user bookmarks
     */
    public static void saveUserBookmarks(Context context, @Nullable PDFViewCtrl pdfViewCtrl, String filePath, List<UserBookmarkItem> data) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_CONTROLS_FILE_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        Gson gson = new Gson();
        Type collectionType = new TypeToken<ArrayList<UserBookmarkItem>>() {
        }.getType();
        String serializedDocs = gson.toJson(data, collectionType);

        editor.putString(KEY_PREF_USER_BOOKMARK + filePath, serializedDocs);
        editor.apply();

        if (pdfViewCtrl != null) {
            ToolManager toolManager = (ToolManager) pdfViewCtrl.getToolManager();
            if (toolManager != null) {
                toolManager.raiseBookmarkModified(data);
            }
        }
    }

    /**
     * ONLY USE WHEN DOCUMENT CANNOT BE ALTERED
     * Removes user bookmarks.
     *
     * @param context  The context
     * @param filePath The file path
     */
    public static void removeUserBookmarks(@Nullable Context context, String filePath) {
        if (context == null) {
            return;
        }
        SharedPreferences settings = context.getSharedPreferences(PREFS_CONTROLS_FILE_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(KEY_PREF_USER_BOOKMARK + filePath);
        editor.apply();
    }

    /**
     * ONLY USE WHEN DOCUMENT CANNOT BE ALTERED
     * Adds a user bookmark
     *
     * @param context    The context
     * @param filePath   The file path
     * @param pageObjNum The page object number
     * @param pageNumber The page number
     */
    @Deprecated
    public static void addUserBookmark(Context context, String filePath, long pageObjNum, int pageNumber) {
        addUserBookmark(context, null, filePath, pageObjNum, pageNumber);
    }

    /**
     * ONLY USE WHEN DOCUMENT CANNOT BE ALTERED
     * Adds a user bookmark
     *
     * @param context     The context
     * @param pdfViewCtrl The PDFViewCtrl
     * @param filePath    The file path
     * @param pageObjNum  The page object number
     * @param pageNumber  The page number
     */
    public static void addUserBookmark(Context context, @Nullable PDFViewCtrl pdfViewCtrl, String filePath, long pageObjNum, int pageNumber) {
        if (context == null || Utils.isNullOrEmpty(filePath)) {
            return;
        }
        List<UserBookmarkItem> bookmarks = getUserBookmarks(context, filePath, null);
        UserBookmarkItem item = new UserBookmarkItem(context, pageObjNum, pageNumber);
        if (!bookmarks.contains(item)) {
            bookmarks.add(item);
            saveUserBookmarks(context, pdfViewCtrl, filePath, bookmarks);
        }
    }

    /**
     * ONLY USE WHEN DOCUMENT CANNOT BE ALTERED
     * Removes a user bookmark
     *
     * @param context    The context
     * @param filePath   The file path
     * @param pageObjNum The page object number
     * @param pageNumber The page number
     */
    @Deprecated
    public static void removeUserBookmark(Context context, String filePath, long pageObjNum, int pageNumber) {
        removeUserBookmark(context, null, filePath, pageObjNum, pageNumber);
    }

    /**
     * ONLY USE WHEN DOCUMENT CANNOT BE ALTERED
     * Removes a user bookmark
     *
     * @param context     The context
     * @param pdfViewCtrl The PDFViewCtrl
     * @param filePath    The file path
     * @param pageObjNum  The page object number
     * @param pageNumber  The page number
     */
    public static void removeUserBookmark(Context context, @Nullable PDFViewCtrl pdfViewCtrl, String filePath, long pageObjNum, int pageNumber) {
        if (context == null || Utils.isNullOrEmpty(filePath)) {
            return;
        }
        List<UserBookmarkItem> bookmarks = getUserBookmarks(context, filePath, null);
        UserBookmarkItem item = new UserBookmarkItem(context, pageObjNum, pageNumber);
        if (bookmarks.contains(item)) {
            bookmarks.remove(item);
            saveUserBookmarks(context, pdfViewCtrl, filePath, bookmarks);
        }
    }

    /**
     * ONLY USE WHEN DOCUMENT CANNOT BE ALTERED
     * Updates user bookmark page object number.
     *
     * @param context       The context
     * @param filePath      The file path
     * @param pageObjNum    The old page object number
     * @param newPageObjNum The new page object number
     * @param newPageNum    The new page number
     */
    @SuppressWarnings("WeakerAccess")
    @Deprecated
    public static void updateUserBookmarkPageObj(Context context, String filePath, long pageObjNum, long newPageObjNum, int newPageNum) {
        updateUserBookmarkPageObj(context, null, filePath, pageObjNum, newPageObjNum, newPageNum);
    }

    /**
     * ONLY USE WHEN DOCUMENT CANNOT BE ALTERED
     * Updates user bookmark page object number.
     *
     * @param context       The context
     * @param pdfViewCtrl   The PDFViewCtrl
     * @param filePath      The file path
     * @param pageObjNum    The old page object number
     * @param newPageObjNum The new page object number
     * @param newPageNum    The new page number
     */
    @SuppressWarnings("WeakerAccess")
    public static void updateUserBookmarkPageObj(Context context, @Nullable PDFViewCtrl pdfViewCtrl, String filePath, long pageObjNum, long newPageObjNum, int newPageNum) {
        List<UserBookmarkItem> items = getUserBookmarks(context, filePath, null);
        for (UserBookmarkItem item : items) {
            if (item.pageObjNum == pageObjNum) {
                item.pageObjNum = newPageObjNum;
                item.pageNumber = newPageNum;
            }
        }
        saveUserBookmarks(context, pdfViewCtrl, filePath, items);
    }

    /**
     * ONLY USE WHEN DOCUMENT CANNOT BE ALTERED
     * Handles bookmarks when a user bookmarks is deleted.
     *
     * @param context    The context
     * @param filePath   The file path
     * @param objNumber  The page object number
     * @param pageNumber The page number
     * @param pageCount  The number of pages
     */
    @Deprecated
    public static void onPageDeleted(Context context, String filePath, Long objNumber, int pageNumber, int pageCount) {
        onPageDeleted(context, null, filePath, objNumber, pageNumber, pageCount);
    }

    /**
     * ONLY USE WHEN DOCUMENT CANNOT BE ALTERED
     * Handles bookmarks when a user bookmarks is deleted.
     *
     * @param context    The context
     * @param pdfViewCtrl   The PDFViewCtrl
     * @param filePath   The file path
     * @param objNumber  The page object number
     * @param pageNumber The page number
     * @param pageCount  The number of pages
     */
    public static void onPageDeleted(Context context, @Nullable PDFViewCtrl pdfViewCtrl, String filePath, Long objNumber, int pageNumber, int pageCount) {
        List<UserBookmarkItem> items = getUserBookmarks(context, filePath, null);
        List<UserBookmarkItem> newItems = new ArrayList<>();
        for (UserBookmarkItem item : items) {
            if (item.pageObjNum != objNumber) {
                newItems.add(item);
            }
        }
        saveUserBookmarks(context, pdfViewCtrl, filePath, newItems);
        updateUserBookmarksAfterRearranging(context, pdfViewCtrl, filePath, pageNumber, pageCount, false, -1);
    }

    /**
     * ONLY USE WHEN DOCUMENT CANNOT BE ALTERED
     * Handles bookmarks when a user bookmarks is moved.
     *
     * @param context       The context
     * @param filePath      The file path
     * @param objNumber     The old page object number
     * @param newObjNumber  The new page object number
     * @param oldPageNumber The page number before moving
     * @param newPageNumber the page number after moving
     */
    @Deprecated
    public static void onPageMoved(Context context, String filePath, long objNumber, long newObjNumber, int oldPageNumber, int newPageNumber) {
        onPageMoved(context, null, filePath, objNumber, newObjNumber, oldPageNumber, newPageNumber);
    }

    /**
     * ONLY USE WHEN DOCUMENT CANNOT BE ALTERED
     * Handles bookmarks when a user bookmarks is moved.
     *
     * @param context       The context
     * @param pdfViewCtrl   The PDFViewCtrl
     * @param filePath      The file path
     * @param objNumber     The old page object number
     * @param newObjNumber  The new page object number
     * @param oldPageNumber The page number before moving
     * @param newPageNumber the page number after moving
     */
    public static void onPageMoved(Context context, @Nullable PDFViewCtrl pdfViewCtrl, String filePath, long objNumber, long newObjNumber, int oldPageNumber, int newPageNumber) {
        updateUserBookmarkPageObj(context, pdfViewCtrl, filePath, objNumber, newObjNumber, newPageNumber);
        if (oldPageNumber < newPageNumber) {
            updateUserBookmarksAfterRearranging(context, pdfViewCtrl, filePath, oldPageNumber + 1, newPageNumber, false, newObjNumber);
        } else {
            updateUserBookmarksAfterRearranging(context, pdfViewCtrl, filePath, newPageNumber, oldPageNumber - 1, true, newObjNumber);
        }
    }

    private static void updateUserBookmarksAfterRearranging(Context context, @Nullable PDFViewCtrl pdfViewCtrl, String filePath, int fromPage, int toPage, boolean increment, long ignoreObjNumber) {
        if (fromPage > toPage) {
            int temp = fromPage;
            fromPage = toPage;
            toPage = temp;
        }
        int change = -1;
        if (increment) {
            change = 1;
        }
        List<UserBookmarkItem> items = getUserBookmarks(context, filePath, null);
        for (UserBookmarkItem item : items) {
            if (item.pageNumber >= fromPage && item.pageNumber <= toPage && item.pageObjNum != ignoreObjNumber) {
                item.pageNumber += change;
            }
        }
        saveUserBookmarks(context, pdfViewCtrl, filePath, items);
    }

    /**
     * ONLY USE WHEN DOCUMENT CANNOT BE ALTERED
     * Updates user bookmark file path.
     *
     * @param context The context
     * @param oldPath The old file path
     * @param newPath The new file path
     */
    public static void updateUserBookmarksFilePath(Context context, String oldPath, String newPath) {
        List<UserBookmarkItem> items = getUserBookmarks(context, oldPath, null);
        if (items.size() > 0) {
            saveUserBookmarks(context, null, newPath, items);
            removeUserBookmarks(context, oldPath);
        }
    }

    // END Bookmark using SharedPreferences
}
