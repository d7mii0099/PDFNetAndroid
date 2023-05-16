package com.pdftron.demo.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;

public class SettingsManager extends PdfViewCtrlSettingsManager {

    /*
     * Navigation Drawer tab
     */
    public static final String KEY_PREF_NAV_TAB_NONE = "none";
    public static final String KEY_PREF_NAV_TAB_VIEWER = "viewer";
    public static final String KEY_PREF_NAV_TAB_RECENT = "recent";
    public static final String KEY_PREF_NAV_TAB_FAVORITES = "favorites";
    public static final String KEY_PREF_NAV_TAB_FOLDERS = "folders";
    public static final String KEY_PREF_NAV_TAB_FILES = "files";
    public static final String KEY_PREF_NAV_TAB_EXTERNAL = "external";
    public static final String KEY_PREF_NAV_TAB_INTERNAL_CACHE = "internal_cache";
    public static final String KEY_PREF_LAST_OPENED_FILE_POSITION_IN_ALL_DOCUMENTS = "last_opened_file_in_all_documents";
    public static final int KEY_PREF_LAST_OPENED_FILE_POSITION_IN_ALL_DOCUMENTS_DEFAULT = -1;

    public static final String KEY_PREF_NAV_TAB = "pref_nav_tab";
    public static final String KEY_PREF_NAV_TAB_DEFAULT_VALUE = KEY_PREF_NAV_TAB_NONE;
    public static final String KEY_PREF_BROWSER_NAV_TAB = "browser_nav_tab";
    public static final String KEY_PREF_BROWSER_NAV_TAB_DEFAULT_VALUE = KEY_PREF_NAV_TAB_NONE;

    /*
     * Setting Menu
     */
    public final static String KEY_PREF_CATEGORY_GENERAL = "pref_category_general";
    public final static String KEY_PREF_CATEGORY_VIEWING = "pref_category_viewing";
    public final static String KEY_PREF_CATEGORY_TABS = "pref_category_tabs";
    public final static String KEY_PREF_CATEGORY_ANNOTATING = "pref_category_annotating";
    public final static String KEY_PREF_CATEGORY_STYLUS = "pref_category_stylus";
    public final static String KEY_PREF_CATEGORY_ABOUT = "pref_category_about";

    /*
     * Getting Started bundle
     */
    public final static String KEY_PREF_FIRST_TIME_RUN = "pref_first_time_run";
    public final static boolean KEY_PREF_FIRST_TIME_RUN_DEFAULT_VALUE = true;

    public static String getNavTab(Context context) {
        return getDefaultSharedPreferences(context).getString(KEY_PREF_NAV_TAB,
            KEY_PREF_NAV_TAB_DEFAULT_VALUE);
    }

    public static void updateNavTab(Context context, String navTab) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_NAV_TAB, navTab);
        editor.apply();
    }

    public static String getBrowserNavTab(Context context) {
        return getDefaultSharedPreferences(context).getString(KEY_PREF_BROWSER_NAV_TAB,
            KEY_PREF_BROWSER_NAV_TAB_DEFAULT_VALUE);
    }

    public static void updateBrowserNavTab(Context context, String navTab) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_BROWSER_NAV_TAB, navTab);
        editor.apply();
    }

    public static boolean getFirstTimeRun(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_FIRST_TIME_RUN,
            KEY_PREF_FIRST_TIME_RUN_DEFAULT_VALUE);
    }

    public static void updateFirstTimeRun(Context context, boolean value) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_FIRST_TIME_RUN, value);
        editor.apply();
    }


    /**
     * Gets the previously opened file position in the all documents browser. Returns -1 if file position
     * is not initalized or has been cleared
     */
    public static int getLastOpenedFilePositionInAllDocuments(Context context) {
        return getDefaultSharedPreferences(context).getInt(KEY_PREF_LAST_OPENED_FILE_POSITION_IN_ALL_DOCUMENTS,
            KEY_PREF_LAST_OPENED_FILE_POSITION_IN_ALL_DOCUMENTS_DEFAULT);
    }

    /**
     * Clears the previously opened file position in the all documents browser (sets value to -1)
     */
    public static void clearLastOpenedFilePositionInAllDocuments(Context context) {
        updateLastOpenedFilePositionInAllDocuments(context, -1);
    }

    /**
     * Sets the previously opened file position in the all documents browser
     */
    public static void updateLastOpenedFilePositionInAllDocuments(Context context, int filePositionInAdapter) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putInt(KEY_PREF_LAST_OPENED_FILE_POSITION_IN_ALL_DOCUMENTS, filePositionInAdapter);
        editor.apply();
    }

}
