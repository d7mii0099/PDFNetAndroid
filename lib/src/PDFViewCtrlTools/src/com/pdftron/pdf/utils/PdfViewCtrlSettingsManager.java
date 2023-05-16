package com.pdftron.pdf.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.controls.ThumbnailsViewFragment;
import com.pdftron.pdf.dialog.annotlist.AnnotationListSortOrder;
import com.pdftron.pdf.dialog.annotlist.BaseAnnotationSortOrder;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.tools.DialogLinkEditor;
import com.pdftron.pdf.tools.ToolManager;

import java.util.HashSet;
import java.util.Set;

/**
 * A utility class helps saving things to {@link SharedPreferences}
 */
@SuppressWarnings("WeakerAccess")
public class PdfViewCtrlSettingsManager {

    public static String sCustomSharedPreferencesName = null;

    /*
     * Names of different views that can be used as a suffix to the setting name
     */
    /**
     * @hide
     */
    public static final String KEY_PREF_SUFFIX_RECENT_FILES = "recent";
    /**
     * @hide
     */
    public static final String KEY_PREF_SUFFIX_FAVOURITE_FILES = "favourites";
    /**
     * @hide
     */
    public static final String KEY_PREF_SUFFIX_FOLDER_FILES = "folders";
    /**
     * @hide
     */
    public static final String KEY_PREF_SUFFIX_EXTERNAL_FILES = "external";
    /**
     * @hide
     */
    public static final String KEY_PREF_SUFFIX_MERGE_FILES = "merge";
    /**
     * @hide
     */
    public static final String KEY_PREF_SUFFIX_PROCESSED_FILES = "processed";
    /**
     * @hide
     */
    public static final String KEY_PREF_SUFFIX_XODO_DRIVE_FILES = "xodo_drive";
    /*
     * Viewer Mode
     */
    /**
     * View mode key: continuous mode
     */
    public static final String KEY_PREF_VIEWMODE_CONTINUOUS_VALUE = "continuous";
    /**
     * View mode key: single page mode
     */
    public static final String KEY_PREF_VIEWMODE_SINGLEPAGE_VALUE = "singlepage";
    /**
     * View mode key: facing page mode
     */
    public static final String KEY_PREF_VIEWMODE_FACING_VALUE = "facing";
    /**
     * View mode key: facing cover page mode
     */
    public static final String KEY_PREF_VIEWMODE_FACINGCOVER_VALUE = "facingcover";
    /**
     * View mode key: facing continuous mode
     */
    public static final String KEY_PREF_VIEWMODE_FACING_CONT_VALUE = "facing_cont";
    /**
     * View mode key: facing cover continuous mode
     */
    public static final String KEY_PREF_VIEWMODE_FACINGCOVER_CONT_VALUE = "facingcover_cont";
    /**
     * View mode key: thumbnails mode
     */
    public static final String KEY_PREF_VIEWMODE_THUMBNAILS_VALUE = "thumbnails";
    /**
     * View mode key: rotation mode
     */
    public static final String KEY_PREF_VIEWMODE_ROTATION_VALUE = "rotation";
    /**
     * View mode key: user crop mode
     */
    public static final String KEY_PREF_VIEWMODE_USERCROP_VALUE = "user_crop";
    /**
     * View mode key: reading settings mode
     */
    public static final String KEY_PREF_VIEWMODE_READING_SETTINGS_VALUE = "reading_settings";

    /**
     * @hide
     */
    public static final String KEY_PREF_VIEWMODE = "pref_viewmode";
    /**
     * @hide
     */
    public static final String KEY_PREF_VIEWMODE_DEFAULT_VALUE = KEY_PREF_VIEWMODE_CONTINUOUS_VALUE;

    /*
     * Free Text Fonts
     */
    /**
     * @hide
     */
    public static final String KEY_PREF_FREE_TEXT_FONTS = "pref_free_text_fonts";
    /**
     * @hide
     */
    public static final String KEY_PREF_FREE_TEXT_FONTS_INIT = "pref_free_text_fonts_init";

    /*
     * Last used external folder uri
     */
    /**
     * @hide
     */
    public static final String KEY_PREF_SAVED_EXTERNAL_FOLDER_URI = "external_folder_uri";
    /**
     * @hide
     */
    public static final String KEY_PREF_SAVED_EXTERNAL_FOLDER_URI_DEFAULT_VALUE = "";

    /*
     * Last used external folder tree uri
     */
    /**
     * @hide
     */
    public static final String KEY_PREF_SAVED_EXTERNAL_FOLDER_TREE_URI = "external_folder_tree_uri";
    /**
     * @hide
     */
    public static final String KEY_PREF_SAVED_EXTERNAL_FOLDER_TREE_URI_DEFAULT_VALUE = "";
    /*
     * Last used local folder path
     */
    /**
     * @hide
     */
    public static final String KEY_PREF_LOCAL_FOLDER_PATH = "pref_local_folder_path";
    /**
     * @hide
     */
    public static final String KEY_PREF_LOCAL_FOLDER_PATH_DEFAULT_VALUE = "";
    /*
     * Last used local folder tree (used for breadcrumbs)
     */
    /**
     * @hide
     */
    public static final String KEY_PREF_LOCAL_FOLDER_TREE = "pref_local_folder_tree";
    /**
     * @hide
     */
    public static final String KEY_PREF_LOCAL_FOLDER_TREE_DEFAULT_VALUE = "";

    /*
     * Last used backup folder path
     */
    /**
     * @hide
     */
    public static final String KEY_PREF_BACKUP_FOLDER_PATH = "pref_backup_cache_folder_path";
    /**
     * @hide
     */
    public static final String KEY_PREF_BACKUP_FOLDER_PATH_DEFAULT_VALUE = "";
    /*
     * Last used backup folder tree (used for breadcrumbs)
     */
    /**
     * @hide
     */
    public static final String KEY_PREF_BACKUP_FOLDER_TREE = "pref_backup_folder_tree";
    /**
     * @hide
     */
    public static final String KEY_PREF_BACKUP_FOLDER_TREE_DEFAULT_VALUE = "";


    /*
     * App version bundle
     */
    /**
     * @hide
     */
    private final static String KEY_PREF_LOCAL_APP_VERSION = "pref_local_app_version";
    /**
     * @hide
     */
    private final static String KEY_PREF_LOCAL_APP_VERSION_DEFAULT_VALUE = "";

    /*
     * File browser file type filters
     */
    /**
     * @hide
     */
    public static final String KEY_PREF_SUFFIX_LOCAL_FILES = "all";
    /**
     * @hide
     */
    public static final String KEY_PREF_FILE_TYPE_FILTER = "pref_file_type_filter_";

    /**
     * @hide
     */
    public static final String KEY_PREF_FILE_TYPE_PDF = "_pdf";
    /**
     * @hide
     */
    public static final String KEY_PREF_FILE_TYPE_DOCX = "_docx";
    /**
     * @hide
     */
    public static final String KEY_PREF_FILE_TYPE_IMAGE = "_image";

    /*
     * Last used folder picker location
     */
    /**
     * @hide
     */
    public static final String KEY_PREF_SAVED_FOLDER_PICKER_LOCATION = "saved_folder_picker_location";
    /**
     * @hide
     */
    public static final String KEY_PREF_SAVED_FOLDER_PICKER_LOCATION_DEFAULT_VALUE = null;
    /**
     * @hide
     */
    public static final String KEY_PREF_SAVED_FOLDER_PICKER_FILE_TYPE = "saved_folder_picker_file_type";
    /**
     * @hide
     */
    public static final int KEY_PREF_SAVED_FOLDER_PICKER_FILE_TYPE_DEFAULT_VALUE = FileInfo.FILE_TYPE_UNKNOWN;

    /*
     * Last used file picker location
     */
    /**
     * @hide
     */
    public static final String KEY_PREF_SAVED_FILE_PICKER_LOCATION = "saved_file_picker_location";
    /**
     * @hide
     */
    public static final String KEY_PREF_SAVED_FILE_PICKER_LOCATION_DEFAULT_VALUE = null;
    /**
     * @hide
     */
    public static final String KEY_PREF_SAVED_FILE_PICKER_FILE_TYPE = "saved_file_picker_file_type";
    /**
     * @hide
     */
    public static final int KEY_PREF_SAVED_FILE_PICKER_FILE_TYPE_DEFAULT_VALUE = FileInfo.FILE_TYPE_UNKNOWN;

    /*
     * Browser
     */
    /**
     * @hide
     */
    public static final String KEY_PREF_GRID_SIZE = "pref_grid_size_new_";
    /**
     * @hide
     */
    public static final String KEY_PREF_GRID_SIZE_SMALL_VALUE = "small";
    /**
     * @hide
     */
    public static final String KEY_PREF_GRID_SIZE_MEDIUM_VALUE = "medium";
    /**
     * @hide
     */
    public static final String KEY_PREF_GRID_SIZE_LARGE_VALUE = "large";
    /**
     * @hide
     */
    public static final int KEY_PREF_GRID_SPAN_DEFAULT_VALUE = 0;
    /**
     * @hide
     */
    public static final String KEY_PREF_GRID_SIZE_DEFAULT_VALUE = KEY_PREF_GRID_SIZE_MEDIUM_VALUE;

    /*
     * Annotation list sort order
     */
    /**
     * @hide
     */
    public static final String KEY_PREF_ANNOT_LIST_SORT = "pref_annot_list_sort";

    /*
     * Thumbnails list filter mode
     */
    /**
     * @hide
     */
    public static final String KEY_PREF_THUMB_LIST_FILTER = "pref_thumbnails_list_filter";

    /*
     * File/folder sort order
     */
    /**
     * @hide
     */
    public static final String KEY_PREF_SORT = "pref_sort";
    /**
     * @hide
     */
    public static final String KEY_PREF_SORT_BY_ACTIVITY_DATE = "activity_date";
    /**
     * @hide
     */
    public static final String KEY_PREF_SORT_BY_CREATED_DATE = "created_date";
    /**
     * @hide
     */
    public static final String KEY_PREF_SORT_BY_FILE_NAME = "file_name";
    /**
     * @hide
     */
    public static final String KEY_PREF_SORT_BY_NAME = "name";
    /**
     * @hide
     */
    public static final String KEY_PREF_SORT_BY_DATE = "date";
    /**
     * @hide
     */
    public static final String KEY_PREF_SORT_DEFAULT_VALUE = KEY_PREF_SORT_BY_NAME;

    /**
     * @hide
     */
    public static final String KEY_PREF_SHOW_OPEN_READ_ONLY_SDCARD_FILE_WARNING = "pref_show_open_read_only_sdcard_file_warning";
    /**
     * @hide
     */
    public static final boolean KEY_PREF_SHOW_OPEN_READ_ONLY_SDCARD_FILE_WARNING_DEFAULT_VALUE = true;

    /*
     * Permission control
     */
    private static final String KEY_PREF_STORAGE_PERMISSION_ASKED = "pref_storage_permission_asked";
    private static final boolean KEY_PREF_STORAGE_PERMISSION_ASKED_DEFAULT_VALUE = false;
    private static final String KEY_PREF_STORAGE_PERMISSION_DENIED = "pref_storage_permission_denied";
    private static final boolean KEY_PREF_STORAGE_PERMISSION_DENIED_DEFAULT_VALUE = false;

    private static final String KEY_PREF_DOUBLE_ROW_TOOLBAR_IN_USE = "pref_double_row_toolbar_in_use";
    private static final boolean KEY_PREF_DOUBLE_ROW_TOOLBAR_IN_USE_DEFAULT_VALUE = false;

    // backup file
    public final static String KEY_PREF_EDIT_URI_BACKUP_FILE_PATH = "pref_edit_uri_backup_file_path";
    public final static String KEY_PREF_EDIT_URI_BACKUP_FILE_PATH_DEFAULT_VALUE = "";

    /**
     * @hide
     */
    public static final int KEY_PREF_COLOR_MODE_NORMAL = 1;
    /**
     * @hide
     */
    public static final int KEY_PREF_COLOR_MODE_SEPIA = 2;
    /**
     * @hide
     */
    public static final int KEY_PREF_COLOR_MODE_NIGHT = 3;
    /**
     * @hide
     */
    public static final int KEY_PREF_COLOR_MODE_CUSTOM = 4;

    /**
     * @hide
     */
    public static final String KEY_PREF_COLOR_MODE_CUSTOM_TEXTCOLOR = "pref_color_mode_custom_textcolor";
    /**
     * @hide
     */
    public static final int KEY_PREF_COLOR_MODE_CUSTOM_TEXTCOLOR_DEFAULT_VALUE = 0xFF000000;
    /**
     * @hide
     */
    public static final String KEY_PREF_COLOR_MODE_CUSTOM_BGCOLOR = "pref_color_mode_custom_bgcolor";
    /**
     * @hide
     */
    public static final int KEY_PREF_COLOR_MODE_CUSTOM_BGCOLOR_DEFAULT_VALUE = 0xFFFFFFFF;
    /**
     * @hide
     */
    public static final String KEY_PREF_COLOR_MODE_PRESETS = "pref_color_mode_presets";
    /**
     * @hide
     */
    public static final String KEY_PREF_COLOR_MODE_SELECTED_PRESET = "pref_color_mode_selected_preset";
    /**
     * @hide
     */
    public static final int KEY_PREF_COLOR_MODE_SELECTED_PRESET_DEFAULT_VALUE = -1;
    /**
     * @hide
     */
    private static final String KEY_PREF_COLOR_MODE = "pref_color_mode";
    /**
     * @hide
     */
    private static final int KEY_PREF_COLOR_MODE_DEFAULT_VALUE = KEY_PREF_COLOR_MODE_NORMAL;
    /**
     * @hide
     */
    public static final String KEY_PREF_FOLLOW_SYSTEM_DARK_MODE = "pref_follow_system_dark_mode";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_FOLLOW_SYSTEM_DARK_MODE_DEFAULT_VALUE = true;
    /**
     * @hide
     */
    private static final String KEY_PREF_PRINT_DOCUMENT = "pref_print_document";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_PRINT_DOCUMENT_DEFAULT_VALUE = true;
    /**
     * @hide
     */
    private static final String KEY_PREF_PRINT_ANNOTATIONS = "pref_print_annotations";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_PRINT_ANNOTATIONS_DEFAULT_VALUE = true;
    /**
     * @hide
     */
    private static final String KEY_PREF_PRINT_SUMMARY = "pref_print_summary";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_PRINT_SUMMARY_DEFAULT_VALUE = false;

    /**
     * @hide
     */
    public static final String KEY_PREF_RTLMODE = "pref_rtlmode";
    /**
     * @hide
     */
    public static final String KEY_PREF_REFLOWMODE = "pref_reflowmode";

    private static final boolean KEY_PREF_RTLMODE_DEFAULT_VALUE = false;

    /**
     * @hide
     */
    private static final String KEY_PREF_RTL_MODE_OPTION = "pref_rtl_mode_option";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_RTL_MODE_OPTION_DEFAULT_VALUE = false;

    /**
     * @hide
     */
    public static final String KEY_PREF_CONT_ANNOT_EDIT = "pref_cont_annot_edit";
    /**
     * @hide
     */
    public static final boolean KEY_PREF_CONT_ANNOT_EDIT_DEFAULT_VALUE = true;
    /**
     * @hide
     */

    public static final String KEY_PREF_FULL_SCREEN_MODE = "pref_full_screen_mode";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_FULL_SCREEN_MODE_DEFAULT_VALUE = true;

    /**
     * @hide
     */
    public static final String KEY_PREF_DESKTOP_UI_MODE = "pref_enable_desktop_ui";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_DESKTOP_UI_MODE_DEFAULT_VALUE = false;

    public static final String KEY_PREF_PAGE_VIEW_MODE = "pref_page_view_mode";
    /**
     * @hide
     */
    private static final String KEY_PREF_PAGE_VIEW_MODE_DEFAULT_VALUE = String.valueOf(PDFViewCtrl.PageViewMode.FIT_PAGE.getValue());

    /**
     * @hide
     */
    private static final String KEY_PREF_MULTI_TABS = "pref_multiple_tabs";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_MULTI_TABS_DEFAULT_VALUE = true;

    /**
     * @hide
     */
    private static final String KEY_PREF_SCREEN_STAY_LOCK = "pref_screen_stay_lock";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_SCREEN_STAY_LOCK_DEFAULT_VALUE = false;
    /**
     * @hide
     */
    private static final String KEY_PREF_NEW_UI_SHOW_STATUS_BAR = "pref_new_ui_show_status_bar";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_NEW_UI_SHOW_STATUS_BAR_DEFAULT_VALUE = false;
    /**
     * @hide
     */
    private static final String KEY_PREF_NEW_UI_SHOW_NAVIGATION_BAR = "pref_new_ui_show_navigation_bar";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_NEW_UI_SHOW_NAVIGATION_BAR_DEFAULT_VALUE = true;
    /**
     * @hide
     */
    public static final String KEY_PREF_NEW_UI_SHOW_TAB_BAR = "pref_new_ui_show_tab_bar";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_NEW_UI_SHOW_TAB_BAR_DEFAULT_VALUE = true;
    /**
     * @hide
     */
    public static final String KEY_PREF_NEW_UI_SHOW_TAB_BAR_PHONE = "pref_new_ui_show_tab_bar_phone";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_NEW_UI_SHOW_TAB_BAR_PHONE_DEFAULT_VALUE = false;
    /**
     * @hide
     */
    private static final String KEY_PREF_MAINTAIN_ZOOM_OPTION = "pref_maintain_zoom_option";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_MAINTAIN_ZOOM_OPTION_DEFAULT_VALUE = true;

    /**
     * @hide
     */
    private static final String KEY_PREF_IMAGE_SMOOTHING = "pref_image_smoothing";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_IMAGE_SMOOTHING_DEFAULT_VALUE = true;

    /**
     * @hide
     */
    private static final String KEY_PREF_COPY_ANNOTATED_TEXT_TO_NOTE = "pref_copy_annotated_text_to_note";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_COPY_ANNOTATED_TEXT_TO_NOTE_DEFAULT_VALUE = false;

    /**
     * @hide
     */
    public static final String KEY_PREF_STYLUS_AS_PEN = "pref_stylus_as_pen";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_STYLUS_AS_PEN_DEFAULT_VALUE = false;

    /**
     * @hide
     */
    private static final String KEY_PREF_INK_SMOOTHING = "pref_ink_smoothing";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_INK_SMOOTHING_DEFAULT_VALUE = true;

    /**
     * @hide
     */
    private static final String KEY_PREF_FREE_HIGHLIGHTER_SMOOTHING = "pref_free_highlighter_smoothing";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_FREE_HIGHLIGHTER_SMOOTHING_DEFAULT_VALUE = false;

    /**
     * @hide
     */
    private static final String KEY_PREF_ANNOT_LIST_SHOW_AUTHOR = "pref_annot_list_show_author";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_ANNOT_LIST_SHOW_AUTHOR_DEFAULT_VALUE = false;

    /**
     * @hide
     */
    public static final String KEY_PREF_ALLOW_PAGE_CHANGE_ANIMATION = "pref_page_change_animation";
    /**
     * @hide
     */
    public static final boolean KEY_PREF_ALLOW_PAGE_CHANGE_ANIMATION_DEFAULT_VALUE = true;

    /**
     * @hide
     */
    public static final String KEY_PREF_ALLOW_PAGE_CHANGE_ON_TAP = "pref_allow_page_change_on_tap";
    /**
     * @hide
     */
    public static final boolean KEY_PREF_ALLOW_PAGE_CHANGE_ON_TAP_DEFAULT_VALUE = true;

    /**
     * @hide
     */
    private static final String KEY_PREF_PAGE_NUMBER_OVERLAY = "pref_page_number_overlay";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_PAGE_NUMBER_OVERLAY_DEFAULT_VALUE = true;

    /**
     * @hide
     */
    public static final String KEY_PREF_SCROLLBAR_GUIDELINE = "pref_scrollbar_guideline";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_SCROLLBAR_GUIDELINE_DEFAULT_VALUE = false;

    /**
     * @hide
     */
    private static final String KEY_PREF_REMEMBER_LAST_PAGE = "pref_remember_last_page";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_REMEMBER_LAST_PAGE_DEFAULT_VALUE = true;

    /**
     * @hide
     */
    private static final String KEY_PREF_ENABLE_JAVASCRIPT = "pref_enable_javascript";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_ENABLE_JAVASCRIPT_DEFAULT_VALUE = true;

    /**
     * @hide
     */
    private static final String KEY_PREF_SHOW_ANNOT_INDICATOR = "pref_show_annot_indicator";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_SHOW_ANNOT_INDICATOR_DEFAULT_VALUE = true;

    /**
     * @hide
     */
    public static final String KEY_PREF_SHOW_QUICK_MENU = "pref_show_quick_menu";
    /**
     * @hide
     */
    public static final boolean KEY_PREF_SHOW_QUICK_MENU_DEFAULT_VALUE = true;

    /**
     * @hide
     */
    private static final String KEY_PREF_COPY_ANNOT_TEACH_SHOWN = "copy_annot_teach_shown_count";
    /**
     * @hide
     */
    private static final int KEY_PREF_COPY_ANNOT_TEACH_SHOWN_MAX = 3;
    /**
     * @hide
     */
    private static final String KEY_PREF_COLOR_PICKER_PAGE = "pref_color_picker_page";
    /**
     * @hide
     */
    private static final int KEY_PREF_COLOR_PICKER_DEFAULT_PAGE = 1;

    private static final String KEY_PREF_TOOLBAR_VISIBLE_ANNOT_TYPE = "pref_annot_toolbar_visible_annot_types";

    /**
     * @hide
     */
    private static final String KEY_PREF_UNLIMITED_TABS = "pref_unlimited_tabs";
    /**
     * @hide
     */
    public static final boolean KEY_PREF_UNLIMITED_TABS_DEFAULT_VALUE = false;

    /**
     * @hide
     */
    private static final String KEY_PREF_QUICK_BOOKMARK_CREATION = "pref_quick_bookmark_creation";
    /**
     * @hide
     */
    private static final boolean KEY_PREF_QUICK_BOOKMARK_CREATION_DEFAULT_VALUE = false;

    /*
     * Annotation Author
     */
    private static final String KEY_PREF_AUTHOR_NAME = "pref_author_name";
    private static final String KEY_PREF_AUTHOR_NAME_DEFAULT_VALUE = "";
    private static final String KEY_PREF_AUTHOR_NAME_HAS_BEEN_ASKED = "pref_author_name_has_been_asked";
    private static final boolean KEY_PREF_AUTHOR_NAME_HAS_BEEN_ASKED_DEFAULT_VALUE = false;

    /**
     * Last used option in edit link dialog
     */
    private static final String KEY_PREF_LINK_EDIT_OPTION = "pref_link_edit_option";
    private static final int KEY_PREF_LINK_EDIT_OPTION_DEFAULT_VALUE = DialogLinkEditor.LINK_OPTION_URL;

    private static final String KEY_PREF_PRESET_ANNOT_STYLE = "pref_preset_ annot_style";
    private static final String KEY_PREF_PRESET_ANNOT_STYLE_DEFAULT_VALUE = null;

    private static final String KEY_PREF_PRESET_ANNOT_STYLE_TAB_INDEX = "pref_preset_annot_style_tab_index";
    private static final int KEY_PREF_PRESET_ANNOT_STYLE_TAB_INDEX_DEFAULT_VALUE = 0;

    private static final String KEY_PREF_PRESET_ANNOT_STYLE_MORE_ANNOT_TYPE = "pref_preset_annot_style_more_annot_type";

    private static final String KEY_PREF_RECENT_COLORS = "pref_recent_colors";
    private static final String KEY_PREF_FAVORITE_COLORS = "pref_favorite_colors";

    private static final String KEY_PREF_OPEN_URL_LAST_PAGE = "pref_open_url_last_page";
    private static final String KEY_PREF_OPEN_URL_LAST_PAGE_STATE = "pref_open_url_last_page_state";

    private static final String KEY_PREF_SHOW_RAGE_SCROLLING_INFO = "pref_show_rage_scrolling_info_new";
    public final static boolean KEY_PREF_SHOW_RAGE_SCROLLING_INFO_DEFAULT_VALUE = true;

    private static final String KEY_PREF_VERTICAL_SCROLL_SNAP = "pref_vertical_page_snapping";
    public final static boolean KEY_PREF_VERTICAL_SCROLL_SNAP_DEFAULT_VALUE = false;

    private static final String KEY_PREF_HOME_TOOLBAR_MENU = "toolbar_menu_home_saved";

    public static final String KEY_PREF_COLOR_MANAGEMENT = "pref_color_management";

    private static final int mDefaultToolValue = ToolManager.ToolMode.INK_CREATE.getValue();
    private static final String KEY_PREF_DEFAULT_STYLUS_TOOL_MODE = "default_stylus_tool_mode";

    private static final String KEY_PREF_DRAW_WITH_FINGER = "pref_draw_with_finger";
    private static final boolean KEY_PREF_DRAW_WITH_FINGER_DEFAULT_VALUE = true;

    /**
     * @hide
     */
    private static final boolean KEY_PREF_COLOR_MANAGEMENT_DEFAULT_VALUE = true;

    /**
     * Gets default shared preference
     *
     * @param context The context
     * @return Default shared preference
     */
    public static SharedPreferences getDefaultSharedPreferences(@NonNull Context context) {
        if (Utils.isNullOrEmpty(sCustomSharedPreferencesName)) {
            return PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        }
        Context applicationContext = context.getApplicationContext();
        return applicationContext.getSharedPreferences(
                applicationContext.getPackageName() + sCustomSharedPreferencesName,
                Context.MODE_PRIVATE
        );
    }

    /**
     * Gets the view mode. Possible values are {@link #KEY_PREF_VIEWMODE_CONTINUOUS_VALUE}
     * and {@link #KEY_PREF_VIEWMODE_SINGLEPAGE_VALUE}.
     *
     * @param context the Context
     * @return the view mode
     */
    public static String getViewMode(@NonNull Context context) {
        return getDefaultSharedPreferences(context)
                .getString(KEY_PREF_VIEWMODE, KEY_PREF_VIEWMODE_DEFAULT_VALUE);
    }

    /**
     * Update the view mode in the shared preferences.
     *
     * @param context the Context
     * @param mode    the view mode. Possible values are {@link #KEY_PREF_VIEWMODE_CONTINUOUS_VALUE}
     *                and {@link #KEY_PREF_VIEWMODE_SINGLEPAGE_VALUE}.
     */
    public static void updateViewMode(@NonNull Context context, String mode) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_VIEWMODE, mode);
        editor.apply();
    }

    /**
     * Get whether the viewer should be in RTL mode.
     *
     * @param context the Context
     * @return whether RTL was in use
     */
    public static boolean getInRTLMode(@NonNull Context context) {
        return getDefaultSharedPreferences(context)
                .getBoolean(KEY_PREF_RTLMODE, KEY_PREF_RTLMODE_DEFAULT_VALUE);
    }

    /**
     * Update whether the viewer should be in RTL mode.
     *
     * @param context the Context
     * @param isRTL   whether RTL was in use
     */
    public static void updateInRTLMode(@NonNull Context context, boolean isRTL) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_RTLMODE, isRTL);
        editor.apply();
    }

    /**
     * Returns the color mode. Possible values are {@link #KEY_PREF_COLOR_MODE_NORMAL},
     * {@link #KEY_PREF_COLOR_MODE_SEPIA} and {@link #KEY_PREF_COLOR_MODE_NIGHT}.
     *
     * @param context The context
     * @return The color mode
     */
    public static int getColorMode(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getInt(KEY_PREF_COLOR_MODE, KEY_PREF_COLOR_MODE_DEFAULT_VALUE);
    }

    /**
     * Sets the color mode in the shared preferences.
     *
     * @param context The context
     * @param mode    The color mode. Possible values are {@link #KEY_PREF_COLOR_MODE_NORMAL},
     *                {@link #KEY_PREF_COLOR_MODE_SEPIA}
     *                and {@link #KEY_PREF_COLOR_MODE_NIGHT}
     */
    public static void setColorMode(@NonNull Context context, int mode) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putInt(KEY_PREF_COLOR_MODE, mode);
        editor.apply();

        int newMode = getColorMode(context);
        if (mode != newMode) {
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.CATEGORY_GENERAL, "Error: SharedPreferences.Editor apply value does not match get");
        }
    }

    /**
     * Gets whether to follow the system setting for dark mode
     *
     * @param context The context
     * @return true if follow the system dark mode, false otherwise
     */
    public static boolean getFollowSystemDarkMode(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_FOLLOW_SYSTEM_DARK_MODE, KEY_PREF_FOLLOW_SYSTEM_DARK_MODE_DEFAULT_VALUE);
    }

    /**
     * Sets whether to follow the system setting for dark mode
     *
     * @param context      The context
     * @param followSystem true if follow the system dark mode, false otherwise
     */
    public static void setFollowSystemDarkMode(@NonNull Context context, boolean followSystem) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_FOLLOW_SYSTEM_DARK_MODE, followSystem);
        editor.apply();
    }

    /**
     * Returns whether the viewer's page display dialog should include an option to enable
     * right-to-left document support.
     *
     * @param context The context
     * @return True if the right-to-left mode option is enabled
     */
    public static boolean hasRtlModeOption(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_RTL_MODE_OPTION,
                KEY_PREF_RTL_MODE_OPTION_DEFAULT_VALUE);
    }

    /**
     * Updates the right-to-left mode option in the shared preferences.
     *
     * @param context       The context
     * @param rtlModeOption The right-to-left mode option
     */
    public static void updateRtlModeOption(@NonNull Context context, boolean rtlModeOption) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_RTL_MODE_OPTION, rtlModeOption);
        editor.apply();
    }

    /**
     * Returns whether the continuous annotation edit mode is enabled.
     *
     * @param context The context
     * @return True if the continuous annotation edit mode is enabled
     */
    public static boolean getContinuousAnnotationEdit(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_CONT_ANNOT_EDIT,
                KEY_PREF_CONT_ANNOT_EDIT_DEFAULT_VALUE);
    }

    /**
     * Sets whether the continuous annotation edit mode is enabled.
     *
     * @param context The context
     * @param value   Whether the continuous annotation edit mode is enabled
     */
    public static void setContinuousAnnotationEdit(@NonNull Context context, boolean value) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_CONT_ANNOT_EDIT, value);
        editor.apply();
    }

    /**
     * Returns whether the show quick menu mode is enabled.
     *
     * @param context The context
     * @return True if the show quick menu mode is enabled
     */
    public static boolean isAutoSelectAnnotation(@NonNull Context context) {
        return isAutoSelectAnnotation(context, KEY_PREF_SHOW_QUICK_MENU_DEFAULT_VALUE);
    }

    /**
     * Returns whether the show quick menu mode is enabled.
     *
     * @param context      The context
     * @param defaultValue The default value
     * @return True if the show quick menu mode is enabled
     */
    public static boolean isAutoSelectAnnotation(@NonNull Context context, boolean defaultValue) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_SHOW_QUICK_MENU, defaultValue);
    }

    /**
     * Sets whether the show quick menu mode is enabled.
     *
     * @param context The context
     * @param value   True if the show quick menu mode is enabled
     */
    public static void setAutoSelectAnnotation(@NonNull Context context, boolean value) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_SHOW_QUICK_MENU, value);
        editor.apply();
    }

    /**
     * Sets whether the full screen mode is enabled.
     *
     * @param context The context
     * @param enabled True if the full screen mode is enabled
     */
    public static void setFullScreenMode(@NonNull Context context, boolean enabled) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_FULL_SCREEN_MODE, enabled);
        editor.apply();
    }

    /**
     * Returns whether the full screen mode is enabled.
     *
     * @param context The context
     * @return True if the full screen mode is enabled
     */
    public static boolean getFullScreenMode(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_FULL_SCREEN_MODE,
                KEY_PREF_FULL_SCREEN_MODE_DEFAULT_VALUE);
    }

    /**
     * Returns whether the desktop UI mode is enabled.
     *
     * @param context The context
     * @return True if the desktop UI mode is enabled
     */
    public static boolean isDesktopUI(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_DESKTOP_UI_MODE,
                KEY_PREF_DESKTOP_UI_MODE_DEFAULT_VALUE);
    }

    /**
     * Returns whether the vertical scrolling will snap to page is enabled.
     *
     * @param context The context
     * @return True if the vertical scrolling will snap to page is enabled
     */
    public static boolean isVerticalScrollSnap(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_VERTICAL_SCROLL_SNAP,
                KEY_PREF_VERTICAL_SCROLL_SNAP_DEFAULT_VALUE);
    }

    /**
     * Sets whether the vertical scrolling will snap to page is enabled.
     *
     * @param context The context
     * @param enabled True if the vertical scrolling will snap to page is enabled
     */
    public static void setVerticalScrollSnap(@NonNull Context context, boolean enabled) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_VERTICAL_SCROLL_SNAP, enabled);
        editor.apply();
    }

    /**
     * Returns whether document itself should be printed.
     *
     * @param context The context
     * @return True if document itself should be printed
     */
    public static boolean isPrintDocumentMode(@NonNull Context context) {
        return getDefaultSharedPreferences(context)
                .getBoolean(KEY_PREF_PRINT_DOCUMENT, KEY_PREF_PRINT_DOCUMENT_DEFAULT_VALUE);
    }

    /**
     * Sets whether document itself should be printed in the shared preferences.
     *
     * @param context The context
     * @param enabled True if document itself should be printed
     */
    public static void setPrintDocumentMode(@NonNull Context context, boolean enabled) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_PRINT_DOCUMENT, enabled);
        editor.apply();
    }

    /**
     * Returns whether annotation should be printed along with the document.
     *
     * @param context The context
     * @return True if annotation should be printed
     */
    public static boolean isPrintAnnotationsMode(@NonNull Context context) {
        return getDefaultSharedPreferences(context)
                .getBoolean(KEY_PREF_PRINT_ANNOTATIONS, KEY_PREF_PRINT_ANNOTATIONS_DEFAULT_VALUE);
    }

    /**
     * Sets whether annotation should be printed in the shared preferences.
     *
     * @param context The context
     * @param enabled True if annotation should be printed
     */
    public static void setPrintAnnotationsMode(@NonNull Context context, boolean enabled) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_PRINT_ANNOTATIONS, enabled);
        editor.apply();
    }

    /**
     * Returns whether summary of annotations should be printed.
     *
     * @param context The context
     * @return True if summary of annotations should be printed
     */
    public static boolean isPrintSummaryMode(@NonNull Context context) {
        return getDefaultSharedPreferences(context)
                .getBoolean(KEY_PREF_PRINT_SUMMARY, KEY_PREF_PRINT_SUMMARY_DEFAULT_VALUE);
    }

    /**
     * Sets whether summary of annotations should be printed in the shared preferences.
     *
     * @param context The context
     * @param enabled True if summary of annotations should be printed
     */
    public static void setPrintSummaryMode(@NonNull Context context, boolean enabled) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_PRINT_SUMMARY, enabled);
        editor.apply();
    }

    /**
     * Sets whether the multiple tabs mode is enabled.
     *
     * @param context The context
     * @param enabled True if the multiple tabs mode is enabled
     */
    public static void setMultipleTabs(@NonNull Context context, boolean enabled) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_MULTI_TABS, enabled);
        editor.apply();
    }

    /**
     * Returns whether the multiple tabs mode is enabled.
     *
     * @param context The context
     * @return True if the multiple tabs mode is enabled
     */
    public static boolean getMultipleTabs(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_MULTI_TABS,
                KEY_PREF_MULTI_TABS_DEFAULT_VALUE);
    }

    /**
     * Returns whether the system status bar is visible in the new UI.
     *
     * @param context The context
     * @return True is the system status bar is visible, default to false.
     */
    public static boolean getShowStatusBarForNewUI(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_NEW_UI_SHOW_STATUS_BAR,
                KEY_PREF_NEW_UI_SHOW_STATUS_BAR_DEFAULT_VALUE);
    }

    /**
     * Sets whether the system status bar is visible in the new UI.
     *
     * @param context The context
     * @param show    whether the system status bar is visible
     */
    public static void setShowStatusBarForNewUI(@NonNull Context context, boolean show) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_NEW_UI_SHOW_STATUS_BAR, show);
        editor.apply();
    }

    /**
     * Returns whether the system navigation bar is visible in the new UI.
     *
     * @param context The context
     * @return True is the system navigation bar is visible, default to true.
     */
    public static boolean getShowNavigationBarForNewUI(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_NEW_UI_SHOW_NAVIGATION_BAR,
                KEY_PREF_NEW_UI_SHOW_NAVIGATION_BAR_DEFAULT_VALUE);
    }

    /**
     * Sets whether the system navigation bar is visible in the new UI.
     *
     * @param context The context
     * @param show    whether the navigation status bar is visible
     */
    public static void setShowNavigationBarForNewUI(@NonNull Context context, boolean show) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_NEW_UI_SHOW_NAVIGATION_BAR, show);
        editor.apply();
    }

    /**
     * Returns whether the tab bar is visible in the new UI for tablets.
     *
     * @param context The context
     * @return True is the tab bar is visible for tablets, default to true.
     */
    public static boolean getShowTabBarForNewUI(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_NEW_UI_SHOW_TAB_BAR,
                KEY_PREF_NEW_UI_SHOW_TAB_BAR_DEFAULT_VALUE);
    }

    /**
     * Sets whether the tab bar is visible in the new UI for tablets.
     *
     * @param context The context
     * @param show    whether the tab bar is visible for tablets
     */
    public static void setShowTabBarForNewUI(@NonNull Context context, boolean show) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_NEW_UI_SHOW_TAB_BAR, show);
        editor.apply();
    }

    /**
     * Returns whether the tab bar is visible in the new UI for phones.
     *
     * @param context The context
     * @return True if the tab bar is visible for phones, default to false.
     */
    public static boolean getShowTabBarForPhone(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_NEW_UI_SHOW_TAB_BAR_PHONE,
                KEY_PREF_NEW_UI_SHOW_TAB_BAR_PHONE_DEFAULT_VALUE);
    }

    /**
     * Sets whether the tab bar is visible in the new UI for phones.
     *
     * @param context The context
     * @param show    whether the tab bar is visible for phones
     */
    public static void setShowTabBarForPhone(@NonNull Context context, boolean show) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_NEW_UI_SHOW_TAB_BAR_PHONE, show);
        editor.apply();
    }

    /**
     * Returns whether the screen sleep lock mode is enabled.
     *
     * @param context The context
     * @return True if the screen sleep lock mode is enabled
     */
    public static boolean getScreenStayLock(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_SCREEN_STAY_LOCK,
                KEY_PREF_SCREEN_STAY_LOCK_DEFAULT_VALUE);
    }

    /**
     * Returns whether the maintain zoom level mode is enabled.
     *
     * @param context The context
     * @return True if the maintain zoom level mode is enabled
     */
    public static boolean getMaintainZoomOption(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_MAINTAIN_ZOOM_OPTION,
                KEY_PREF_MAINTAIN_ZOOM_OPTION_DEFAULT_VALUE);
    }

    /**
     * Returns whether the image smoothing mode is enabled.
     *
     * @param context The context
     * @return True if the image smoothing mode is enabled
     */
    public static boolean getImageSmoothing(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_IMAGE_SMOOTHING,
                KEY_PREF_IMAGE_SMOOTHING_DEFAULT_VALUE);
    }

    /**
     * Returns whether the text of a text markup annotation should be automatically copied into
     * the annotation's note.
     *
     * @param context The context
     * @return True if the copy annotated text to note mode is enabled
     */
    public static boolean getCopyAnnotatedTextToNote(@NonNull Context context) {
        return getCopyAnnotatedTextToNote(context, KEY_PREF_COPY_ANNOTATED_TEXT_TO_NOTE_DEFAULT_VALUE);
    }

    /**
     * Returns whether the text of a text markup annotation should be automatically copied into
     * the annotation's note.
     *
     * @param context      The context
     * @param defaultValue The default value
     * @return True if the copy annotated text to note mode is enabled
     */
    public static boolean getCopyAnnotatedTextToNote(@NonNull Context context, boolean defaultValue) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_COPY_ANNOTATED_TEXT_TO_NOTE,
                defaultValue);
    }

    /**
     * Sets whether the text of a text markup annotation should be automatically copied into
     * the annotation's note.
     *
     * @param context The context
     * @param value   True if the copy annotated text to note mode is enabled
     */
    public static void setCopyAnnotatedTextToNote(@NonNull Context context, boolean value) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_COPY_ANNOTATED_TEXT_TO_NOTE, value);
        editor.apply();
    }

    /**
     * Returns whether a stylus should act as a pen tool instead of a finger when touching the viewer.
     *
     * @param context The context
     * @return True if the stylus as pen mode is enabled
     */
    public static boolean getStylusAsPen(@NonNull Context context) {
        return getStylusAsPen(context, KEY_PREF_STYLUS_AS_PEN_DEFAULT_VALUE);
    }

    /**
     * Returns whether a stylus should act as a pen tool instead of a finger when touching the viewer.
     *
     * @param context      The context
     * @param defaultValue The default value
     * @return True if the stylus as pen mode is enabled
     */
    public static boolean getStylusAsPen(@NonNull Context context, boolean defaultValue) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_STYLUS_AS_PEN, defaultValue);
    }

    /**
     * Sets whether a stylus should act as a pen tool instead of a finger when touching the viewer.
     *
     * @param context The context
     * @param enable  True if the stylus as pen mode is enabled
     */
    public static void updateStylusAsPen(@NonNull Context context, boolean enable) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_STYLUS_AS_PEN, enable);
        editor.apply();
    }

    /**
     * Returns whether ink drawn with a finger should be smoothed.
     *
     * @param context The context
     * @return True if the ink smoothing mode is enabled
     */
    public static boolean getInkSmoothing(@NonNull Context context) {
        return getInkSmoothing(context, KEY_PREF_INK_SMOOTHING_DEFAULT_VALUE);
    }

    /**
     * Returns whether ink drawn with a finger should be smoothed.
     *
     * @param context      The context
     * @param defaultValue The default value
     * @return True if the ink smoothing mode is enabled
     */
    public static boolean getInkSmoothing(@NonNull Context context, boolean defaultValue) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_INK_SMOOTHING, defaultValue);
    }

    /**
     * Sets whether ink drawn with a finger should be smoothed.
     *
     * @param context The context
     * @param value   True if the ink smoothing mode is enabled
     */
    public static void setInkSmoothing(@NonNull Context context, boolean value) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_INK_SMOOTHING, value);
        editor.apply();
    }

    /**
     * Returns whether free highlighter should be auto-smoothed.
     *
     * @param context The context
     * @return True if the free highlighter should be auto-smoothed. Default to true.
     */
    public static boolean getFreeHighlighterSmoothing(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_FREE_HIGHLIGHTER_SMOOTHING, KEY_PREF_FREE_HIGHLIGHTER_SMOOTHING_DEFAULT_VALUE);
    }

    /**
     * Returns whether the annotation list should display the annotation's author.
     *
     * @param context The context
     * @return True if the annotation list shows author mode is enabled
     */
    public static boolean getAnnotListShowAuthor(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_ANNOT_LIST_SHOW_AUTHOR,
                KEY_PREF_ANNOT_LIST_SHOW_AUTHOR_DEFAULT_VALUE);
    }

    /**
     * Sets whether the annotation list should display the annotation's author.
     *
     * @param context    The context
     * @param showAuthor Whether the annotation list shows author
     */
    public static void setAnnotListShowAuthor(@NonNull Context context, boolean showAuthor) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_ANNOT_LIST_SHOW_AUTHOR, showAuthor);
        editor.apply();
    }

    /**
     * Returns available fonts for free text annotations.
     *
     * @param context The context
     * @return available fonts for free text annotations
     */
    public static Set<String> getFreeTextFonts(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getStringSet(KEY_PREF_FREE_TEXT_FONTS, new HashSet<String>());
    }

    /**
     * Sets available fonts for free text annotations.
     *
     * @param context The context
     * @return available fonts for free text annotations
     */
    public static void setFreeTextFonts(@NonNull Context context, Set<String> value) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putStringSet(KEY_PREF_FREE_TEXT_FONTS, value);
        editor.apply();
    }

    /**
     * Returns whether page should turn with animation when tapping on the left of right edge of
     * the viewer.
     *
     * @param context The context
     * @return True if the turn page with animation mode is enabled
     */
    public static boolean getAllowPageChangeAnimation(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_ALLOW_PAGE_CHANGE_ANIMATION,
                KEY_PREF_ALLOW_PAGE_CHANGE_ANIMATION_DEFAULT_VALUE);
    }

    /**
     * Returns whether the page should turn when tapping on the left of right edge of the viewer.
     *
     * @param context The context
     * @return True if the turn page on tap mode is enabled
     */
    public static boolean getAllowPageChangeOnTap(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_ALLOW_PAGE_CHANGE_ON_TAP,
                KEY_PREF_ALLOW_PAGE_CHANGE_ON_TAP_DEFAULT_VALUE);
    }

    /**
     * Sets whether the page should turn when tapping on the left of right edge of the viewer.
     *
     * @param context    The context
     * @param changePage Whether the page should turn
     */
    public static void setAllowPageChangeOnTap(@NonNull Context context, boolean changePage) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PdfViewCtrlSettingsManager.KEY_PREF_ALLOW_PAGE_CHANGE_ON_TAP, changePage);
        editor.apply();
    }

    /**
     * Returns whether the page number should be shown when turning pages.
     *
     * @param context The context
     * @return True if the page number should be shown when turning pages
     */
    public static boolean getPageNumberOverlayOption(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_PAGE_NUMBER_OVERLAY,
                KEY_PREF_PAGE_NUMBER_OVERLAY_DEFAULT_VALUE);
    }

    /**
     * Returns whether the scrollbar guideline should be shown with the scrollbar.
     *
     * @param context The context
     * @return True if the scrollbar guideline should be shown with the scrollbar.
     */
    public static boolean getShowScrollbarGuideline(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_SCROLLBAR_GUIDELINE,
                KEY_PREF_SCROLLBAR_GUIDELINE_DEFAULT_VALUE);
    }

    /**
     * Sets whether the scrollbar guideline should be shown with the scrollbar.
     *
     * @param context       The context
     * @param showGuideline Whether the scrollbar guideline should be shown with the scrollbar.
     */
    public static void setShowScrollbarGuideline(@NonNull Context context, boolean showGuideline) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PdfViewCtrlSettingsManager.KEY_PREF_SCROLLBAR_GUIDELINE, showGuideline);
        editor.apply();
    }

    /**
     * Returns whether documents opened from the recent list should be reopened on the last page
     * they were on.
     *
     * @param context The context
     * @return True if the remember last page mode is enabled
     */
    public static boolean getRememberLastPage(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_REMEMBER_LAST_PAGE,
                KEY_PREF_REMEMBER_LAST_PAGE_DEFAULT_VALUE);
    }

    /**
     * Returns whether JavaScript actions embedded in PDF files should be executed.
     *
     * @param context The context
     * @return True if the javascript mode is enabled
     */
    public static boolean getEnableJavaScript(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_ENABLE_JAVASCRIPT,
                KEY_PREF_ENABLE_JAVASCRIPT_DEFAULT_VALUE);
    }

    /**
     * Sets whether JavaScript actions embedded in PDF files should be executed.
     *
     * @param context The context
     * @param value   Whether the javascript mode is enabled
     */
    public static void setEnableJavaScript(@NonNull Context context, boolean value) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_ENABLE_JAVASCRIPT, value);
        editor.apply();
    }

    /**
     * Returns whether annotation note indicator should be displayed retrieved from shared preferences,
     * returns default value if not in shared preferences.
     *
     * @param context The context
     * @return True if annotation note indicator is should be displayed.
     */
    public static boolean getShowAnnotationIndicator(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_SHOW_ANNOT_INDICATOR,
                KEY_PREF_SHOW_ANNOT_INDICATOR_DEFAULT_VALUE);
    }

    /**
     * Returns whether annotation note indicator should be displayed retrieved from shared preferences,
     * returns default value if not in shared preferences.
     *
     * @param context      The context
     * @param defaultValue default value to return if not set in shared preferences
     * @return True if annotation note indicator is should be displayed.
     */
    public static boolean getShowAnnotationIndicator(@NonNull Context context, boolean defaultValue) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_SHOW_ANNOT_INDICATOR,
                defaultValue);
    }

    /**
     * Sets whether annotation note indicator should be displayed retrieved from shared preferences,
     * returns default value if not in shared preferences.
     *
     * @param context The context
     * @param value   True if annotation note indicator is should be displayed.
     */
    public static void setShowAnnotationIndicator(@NonNull Context context, boolean value) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_SHOW_ANNOT_INDICATOR, value);
        editor.apply();
    }

    /**
     * Returns page view mode value
     *
     * @param context The context
     * @return The page view mode value
     * one of  {@link PDFViewCtrl.PageViewMode#FIT_PAGE}, {@link PDFViewCtrl.PageViewMode#FIT_WIDTH}
     */
    public static PDFViewCtrl.PageViewMode getPageViewMode(@NonNull Context context) {
        int mode = Integer.parseInt(getDefaultSharedPreferences(context).getString(KEY_PREF_PAGE_VIEW_MODE,
                KEY_PREF_PAGE_VIEW_MODE_DEFAULT_VALUE));
        return PDFViewCtrl.PageViewMode.valueOf(mode);
    }

    /**
     * Sets page view mode value
     *
     * @param context The context
     * @param value   The page view mode value
     */
    public static void setPageViewMode(@NonNull Context context, @NonNull PDFViewCtrl.PageViewMode value) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_PAGE_VIEW_MODE, String.valueOf(value.getValue()));
        editor.apply();
    }

    /**
     * Returns the local app version.
     *
     * @param context The context
     * @return The local app version
     */
    public static String getLocalAppVersion(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getString(KEY_PREF_LOCAL_APP_VERSION,
                KEY_PREF_LOCAL_APP_VERSION_DEFAULT_VALUE);
    }

    /**
     * Updates the local app version in the shared preferences.
     *
     * @param context The context
     */
    public static void updateLocalAppVersion(@NonNull Context context) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_LOCAL_APP_VERSION, getAppVersionName(context));
        editor.apply();
    }

    /**
     * Returns whether the app gets updated.
     *
     * @param context The context
     * @return True if the app gets updated
     */
    public static boolean getAppUpdated(Context context) {
        return !getAppVersionName(context).equalsIgnoreCase(getLocalAppVersion(context));
    }

    /**
     * Returns the app version name.
     *
     * @param context The context
     * @return The app version name
     */
    public static String getAppVersionName(Context context) {
        String versionName = "";
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (info.versionName.length() > 0) {
                versionName = info.versionName;
            }
        } catch (PackageManager.NameNotFoundException e) {
            versionName = "";
        }
        return versionName;
    }

    /**
     * Returns whether storage permission request has been asked before.
     *
     * @param context The context
     * @return True if storage permission request has been asked before
     */
    public static boolean getStoragePermissionHasBeenAsked(@NonNull Context context) {
        return getDefaultSharedPreferences(context)
                .getBoolean(KEY_PREF_STORAGE_PERMISSION_ASKED, KEY_PREF_STORAGE_PERMISSION_ASKED_DEFAULT_VALUE);
    }

    /**
     * Updates whether storage permission request has been asked before
     *
     * @param context The context
     * @param value   True if storage permission request has been asked before
     */
    public static void updateStoragePermissionHasBeenAsked(@NonNull Context context, boolean value) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_STORAGE_PERMISSION_ASKED, value);
        editor.apply();
    }

    /**
     * Returns whether storage permission has been denied
     *
     * @param context The context
     * @return True if storage permission has been denied
     */
    public static boolean getStoragePermissionDenied(@NonNull Context context) {
        return getDefaultSharedPreferences(context)
                .getBoolean(KEY_PREF_STORAGE_PERMISSION_DENIED, KEY_PREF_STORAGE_PERMISSION_DENIED_DEFAULT_VALUE);
    }

    /**
     * Updates whether storage permission has been denied.
     *
     * @param context The context
     * @param value   True if storage permission has been denied
     */
    public static void updateStoragePermissionDenied(@NonNull Context context, boolean value) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_STORAGE_PERMISSION_DENIED, value);
        editor.apply();
    }

    /**
     * Returns whether double row toolbar is in use
     *
     * @param context The context
     * @return True if double row toolbar is in use
     */
    public static boolean getDoubleRowToolbarInUse(@NonNull Context context) {
        return getDefaultSharedPreferences(context)
                .getBoolean(KEY_PREF_DOUBLE_ROW_TOOLBAR_IN_USE, KEY_PREF_DOUBLE_ROW_TOOLBAR_IN_USE_DEFAULT_VALUE);
    }

    /**
     * Updates whether double row toolbar is in use
     *
     * @param context The context
     * @param value   True if double row toolbar is in use
     */
    public static void updateDoubleRowToolbarInUse(@NonNull Context context, boolean value) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_DOUBLE_ROW_TOOLBAR_IN_USE, value);
        editor.apply();
    }

    /**
     * Returns whether should teach the user how to paste.
     *
     * @param context The context
     * @return True if should teach the user how to paste
     */
    public static boolean shouldShowHowToPaste(@NonNull Context context) {
        int curCount = getDefaultSharedPreferences(context).getInt(KEY_PREF_COPY_ANNOT_TEACH_SHOWN, 0);
        if (curCount > KEY_PREF_COPY_ANNOT_TEACH_SHOWN_MAX) {
            return false;
        }
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putInt(KEY_PREF_COPY_ANNOT_TEACH_SHOWN, curCount + 1);
        editor.apply();
        return true;
    }

    /**
     * Returns the author's name.
     *
     * @param context The context
     * @return The author's name
     */
    public static String getAuthorName(@NonNull Context context) {
        return getDefaultSharedPreferences(context)
                .getString(KEY_PREF_AUTHOR_NAME, KEY_PREF_AUTHOR_NAME_DEFAULT_VALUE);
    }

    /**
     * Updates the author's name.
     *
     * @param context The context
     * @param value   The author's name
     */
    public static void updateAuthorName(@NonNull Context context, String value) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_AUTHOR_NAME, value);
        editor.apply();
    }

    /**
     * Returns whether the author's name has been asked before.
     *
     * @param context The context
     * @return True if the author's name has been asked before
     */
    public static boolean getAuthorNameHasBeenAsked(@NonNull Context context) {
        return getDefaultSharedPreferences(context)
                .getBoolean(KEY_PREF_AUTHOR_NAME_HAS_BEEN_ASKED, KEY_PREF_AUTHOR_NAME_HAS_BEEN_ASKED_DEFAULT_VALUE);
    }

    /**
     * Sets that the author's name has been asked before.
     *
     * @param context The context
     */
    public static void setAuthorNameHasBeenAsked(@NonNull Context context) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_AUTHOR_NAME_HAS_BEEN_ASKED, true);
        editor.apply();
    }

    private static String getKeyPrefFromFileType(int fileType) {
        switch (fileType) {
            case Constants.FILE_TYPE_PDF:
                return KEY_PREF_FILE_TYPE_PDF;
            case Constants.FILE_TYPE_DOC:
                return KEY_PREF_FILE_TYPE_DOCX;
            case Constants.FILE_TYPE_IMAGE:
                return KEY_PREF_FILE_TYPE_IMAGE;
        }
        return "";
    }

    private static boolean getFileTypeDefaultVisibility(String fileType, String suffix) {
        if (suffix.equals(KEY_PREF_SUFFIX_LOCAL_FILES)) {
            if (fileType.equals(KEY_PREF_FILE_TYPE_PDF) || fileType.equals(KEY_PREF_FILE_TYPE_DOCX))
                return true;
        }
        return false;
    }

    /**
     * Gets whether filtering files.
     *
     * @param context  The context
     * @param fileType The file type
     * @param suffix   The suffix for filtering file.
     * @return true then filter files, false otherwise
     */
    public static boolean getFileFilter(@NonNull Context context, int fileType, String suffix) {
        String fileTypeString = getKeyPrefFromFileType(fileType);
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_FILE_TYPE_FILTER + fileTypeString + suffix,
                getFileTypeDefaultVisibility(fileTypeString, suffix));
    }

    /**
     * Updates file filter
     *
     * @param context    The context
     * @param fileType   The file type
     * @param suffix     The suffix
     * @param visibility The visibility of the file
     */
    public static void updateFileFilter(@NonNull Context context, int fileType, String suffix, boolean visibility) {
        String fileTypeString = getKeyPrefFromFileType(fileType);
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_FILE_TYPE_FILTER + fileTypeString + suffix, visibility);
        editor.apply();
    }

    /**
     * Updates saved folder picker location
     *
     * @param context  The context
     * @param location The location
     */
    public static void updateSavedFolderPickerLocation(@NonNull Context context, String location) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_SAVED_FOLDER_PICKER_LOCATION, location);
        editor.apply();
    }

    /**
     * Gets saved folder picker location
     *
     * @param context The context
     * @return The location
     */
    public static int getSavedFolderPickerFileType(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getInt(KEY_PREF_SAVED_FOLDER_PICKER_FILE_TYPE,
                KEY_PREF_SAVED_FOLDER_PICKER_FILE_TYPE_DEFAULT_VALUE);
    }

    /**
     * Updates saved folder picker file type
     *
     * @param context  The context
     * @param fileType The file type
     */
    public static void updateSavedFolderPickerFileType(@NonNull Context context, int fileType) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putInt(KEY_PREF_SAVED_FOLDER_PICKER_FILE_TYPE, fileType);
        editor.apply();
    }

    /**
     * Gets saved file picker location
     *
     * @param context The context
     * @return The file picker locaiton
     */
    public static String getSavedFilePickerLocation(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getString(KEY_PREF_SAVED_FILE_PICKER_LOCATION,
                KEY_PREF_SAVED_FILE_PICKER_LOCATION_DEFAULT_VALUE);
    }

    /**
     * Updates saved file picker locaiton
     *
     * @param context  The context
     * @param location The location
     */
    public static void updateSavedFilePickerLocation(@NonNull Context context, String location) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_SAVED_FILE_PICKER_LOCATION, location);
        editor.apply();
    }

    /**
     * Gets saved file picker file type
     *
     * @param context The context
     * @return The savedd file picker file type
     */
    public static int getSavedFilePickerFileType(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getInt(KEY_PREF_SAVED_FILE_PICKER_FILE_TYPE,
                KEY_PREF_SAVED_FILE_PICKER_FILE_TYPE_DEFAULT_VALUE);
    }

    /**
     * Update saved file picker file type
     *
     * @param context  The context
     * @param fileType Saved file picker file type
     */
    public static void updateSavedFilePickerFileType(@NonNull Context context, int fileType) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putInt(KEY_PREF_SAVED_FILE_PICKER_FILE_TYPE, fileType);
        editor.apply();
    }

    /**
     * Gets saved folder picker location
     *
     * @param context The context
     * @return The saved folder picker lcoation
     */
    public static String getSavedFolderPickerLocation(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getString(KEY_PREF_SAVED_FOLDER_PICKER_LOCATION,
                KEY_PREF_SAVED_FOLDER_PICKER_LOCATION_DEFAULT_VALUE);
    }

    /**
     * Gets Grid size.
     *
     * @param context The context
     * @param suffix  The suffix
     * @return Grid size
     */
    public static int getGridSize(@NonNull Context context, String suffix) {
        return getDefaultSharedPreferences(context).getInt(KEY_PREF_GRID_SIZE + suffix, KEY_PREF_GRID_SPAN_DEFAULT_VALUE);
    }

    /**
     * Updates grid size
     *
     * @param context The context
     * @param suffix  The suffix
     * @param size    The grid size
     */
    public static void updateGridSize(@NonNull Context context, String suffix, int size) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putInt(KEY_PREF_GRID_SIZE + suffix, size);
        editor.apply();
    }

    /**
     * Gets sort mode
     *
     * @param context The context
     * @return Sort mode
     */
    public static String getSortMode(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getString(KEY_PREF_SORT,
                KEY_PREF_SORT_DEFAULT_VALUE);
    }

    /**
     * Get the {@link AnnotationListSortOrder#value} saved in shared preferences.
     *
     * @param context the context to get shared preferences
     * @return the saved {@link AnnotationListSortOrder#value}
     */
    public static int getAnnotListSortOrder(@NonNull Context context,
            @NonNull BaseAnnotationSortOrder defaultSortOrder) {
        return getDefaultSharedPreferences(context).getInt(KEY_PREF_ANNOT_LIST_SORT + defaultSortOrder.getType(),
                defaultSortOrder.getValue()); // default use date ascending
    }

    /**
     * Updates sort mode
     *
     * @param context The context
     * @param mode    The sort mode
     */
    public static void updateSortMode(@NonNull Context context, String mode) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_SORT, mode);
        editor.apply();
    }

    /**
     * Updates sort order for annotation list
     *
     * @param context   context to save shared preferences
     * @param sortOrder {@link AnnotationListSortOrder}
     */
    public static void updateAnnotListSortOrder(@NonNull Context context,
            @NonNull BaseAnnotationSortOrder sortOrder) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putInt(KEY_PREF_ANNOT_LIST_SORT + sortOrder.getType(), sortOrder.getValue());
        editor.apply();
    }

    /**
     * Gets filter mode for thumbnails view
     *
     * @param context     context to shared preferences
     * @param defaultMode one of {@link ThumbnailsViewFragment#FILTER_MODE_NORMAL}
     *                    {@link ThumbnailsViewFragment#FILTER_MODE_ANNOTATED}
     *                    {@link ThumbnailsViewFragment#FILTER_MODE_BOOKMARKED}
     * @return
     */
    public static int getThumbListFilterMode(@NonNull Context context, int defaultMode) {
        return getDefaultSharedPreferences(context).getInt(KEY_PREF_THUMB_LIST_FILTER, defaultMode);
    }

    /**
     * Sets filter mode for thumbnails view
     *
     * @param context context to shared preferences
     * @param mode    one of {@link ThumbnailsViewFragment#FILTER_MODE_NORMAL}
     *                {@link ThumbnailsViewFragment#FILTER_MODE_ANNOTATED}
     *                {@link ThumbnailsViewFragment#FILTER_MODE_BOOKMARKED}
     */
    public static void updateThumbListFilterMode(@NonNull Context context, int mode) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putInt(KEY_PREF_THUMB_LIST_FILTER, mode);
        editor.apply();
    }

    /**
     * Gets whether it shows open read only sd card file warning dialog,
     *
     * @param context The context
     * @return Show open read only sd card file warning dialog
     */
    public static boolean getShowOpenReadOnlySdCardFileWarning(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_SHOW_OPEN_READ_ONLY_SDCARD_FILE_WARNING,
                KEY_PREF_SHOW_OPEN_READ_ONLY_SDCARD_FILE_WARNING_DEFAULT_VALUE);
    }

    /**
     * Updates show open read only SD card file warning dialog
     *
     * @param context The context
     * @param val     whether it shows warning dialog
     */
    public static void updateShowOpenReadOnlySdCardFileWarning(@NonNull Context context, boolean val) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_SHOW_OPEN_READ_ONLY_SDCARD_FILE_WARNING, val);
        editor.apply();
    }

    /**
     * Gets saved external folder Uri
     *
     * @param context The context
     * @return Saved external folder Uri
     */
    public static String getSavedExternalFolderUri(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getString(KEY_PREF_SAVED_EXTERNAL_FOLDER_URI,
                KEY_PREF_SAVED_EXTERNAL_FOLDER_URI_DEFAULT_VALUE);
    }

    /**
     * Updates Saved external folder uri
     *
     * @param context The context
     * @param uri     The uri
     */
    public static void updateSavedExternalFolderUri(@NonNull Context context, String uri) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_SAVED_EXTERNAL_FOLDER_URI, uri);
        editor.apply();
    }

    /**
     * Gets saved external folder tree uri
     *
     * @param context The context
     * @return eExternal folder tree uri
     */
    public static String getSavedExternalFolderTreeUri(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getString(KEY_PREF_SAVED_EXTERNAL_FOLDER_TREE_URI,
                KEY_PREF_SAVED_EXTERNAL_FOLDER_TREE_URI_DEFAULT_VALUE);
    }

    /**
     * Updates saved external folder tree uri
     *
     * @param context The context
     * @param uri     The uri
     */
    public static void updateSavedExternalFolderTreeUri(@NonNull Context context, String uri) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_SAVED_EXTERNAL_FOLDER_TREE_URI, uri);
        editor.apply();
    }

    /**
     * gets local folder path
     *
     * @param context The context
     * @return Local folder path
     */
    public static String getLocalFolderPath(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getString(KEY_PREF_LOCAL_FOLDER_PATH,
                KEY_PREF_LOCAL_FOLDER_PATH_DEFAULT_VALUE);
    }

    /**
     * Updates local folder path
     *
     * @param context The context
     * @param path    The local folder path
     */
    public static void updateLocalFolderPath(@NonNull Context context, String path) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_LOCAL_FOLDER_PATH, path);
        editor.apply();
    }

    /**
     * gets backup folder path
     *
     * @param context The context
     * @return backup folder path
     */
    public static String getBackupFolderPath(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getString(KEY_PREF_BACKUP_FOLDER_PATH,
                KEY_PREF_BACKUP_FOLDER_PATH_DEFAULT_VALUE);
    }

    /**
     * Updates backup folder path
     *
     * @param context The context
     * @param path    The local folder path
     */
    public static void updateBackupCacheFolderPath(@NonNull Context context, String path) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_BACKUP_FOLDER_PATH, path);
        editor.apply();
    }

    /**
     * Gets local folder tree path
     *
     * @param context The context
     * @return Local folder tree path
     */
    public static String getLocalFolderTree(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getString(KEY_PREF_LOCAL_FOLDER_TREE,
                KEY_PREF_LOCAL_FOLDER_TREE_DEFAULT_VALUE);
    }

    /**
     * Updates local folder tree
     *
     * @param context The context
     * @param path    Local folder tree path
     */
    public static void updateLocalFolderTree(@NonNull Context context, String path) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_LOCAL_FOLDER_TREE, path);
        editor.apply();
    }

    /**
     * Gets backup folder tree path
     *
     * @param context The context
     * @return backup folder tree path
     */
    public static String getBackupCacheFolderTree(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getString(KEY_PREF_BACKUP_FOLDER_TREE,
                KEY_PREF_BACKUP_FOLDER_TREE_DEFAULT_VALUE);
    }

    /**
     * Updates backup folder tree
     *
     * @param context The context
     * @param path    backup folder tree path
     */
    public static void updateBackupCacheFolderTree(@NonNull Context context, String path) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_BACKUP_FOLDER_TREE, path);
        editor.apply();
    }

    /**
     * Gets custom color mode background color
     *
     * @param context The context
     * @return The custom color mode background color
     */
    public static int getCustomColorModeBGColor(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getInt(KEY_PREF_COLOR_MODE_CUSTOM_BGCOLOR, KEY_PREF_COLOR_MODE_CUSTOM_BGCOLOR_DEFAULT_VALUE);
    }

    /**
     * Sets custom color mode background color
     *
     * @param context The context
     * @param color   Background color
     */
    public static void setCustomColorModeBGColor(@NonNull Context context, int color) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putInt(KEY_PREF_COLOR_MODE_CUSTOM_BGCOLOR, color);
        editor.apply();
    }

    /**
     * Gets custom color mode text color
     *
     * @param context The context
     * @return The text color
     */
    public static int getCustomColorModeTextColor(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getInt(KEY_PREF_COLOR_MODE_CUSTOM_TEXTCOLOR, KEY_PREF_COLOR_MODE_CUSTOM_TEXTCOLOR_DEFAULT_VALUE);
    }

    /**
     * Sets custom color mode text color
     *
     * @param context The context
     * @param color   text color
     */
    public static void setCustomColorModeTextColor(@NonNull Context context, int color) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putInt(KEY_PREF_COLOR_MODE_CUSTOM_TEXTCOLOR, color);
        editor.apply();
    }

    /**
     * Sets color mode presets
     *
     * @param context             The context
     * @param jsonSerializedArray Json Serialized array of color mode presets
     */
    public static void setColorModePresets(@NonNull Context context, String jsonSerializedArray) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_COLOR_MODE_PRESETS, jsonSerializedArray);
        editor.apply();
    }

    /**
     * Gets color mode presets
     *
     * @param context The context
     * @return Color mode presets
     */
    public static String getColorModePresets(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getString(KEY_PREF_COLOR_MODE_PRESETS, "");
    }

    /**
     * Sets selected color mode preset
     *
     * @param context  The context
     * @param position position of color mode
     */
    public static void setSelectedColorModePreset(@NonNull Context context, int position) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putInt(KEY_PREF_COLOR_MODE_SELECTED_PRESET, position);
        editor.apply();
    }

    /**
     * Gets selected color mode presets
     *
     * @param context The context
     * @return Selected color mode presets
     */
    public static int getSelectedColorModePreset(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getInt(KEY_PREF_COLOR_MODE_SELECTED_PRESET, KEY_PREF_COLOR_MODE_SELECTED_PRESET_DEFAULT_VALUE);
    }

    /**
     * set edit link dialog last option
     *
     * @param context the context
     * @param option  last selected option
     */
    public static void setLinkEditLastOption(@NonNull Context context, @DialogLinkEditor.LinkOption int option) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putInt(KEY_PREF_LINK_EDIT_OPTION, option);
        editor.apply();
    }

    /**
     * get edit link dialog last option
     *
     * @param context the context
     * @return edit link dialog last selected option
     */
    public static int getLinkEditLastOption(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getInt(KEY_PREF_LINK_EDIT_OPTION, KEY_PREF_LINK_EDIT_OPTION_DEFAULT_VALUE);
    }

    /**
     * Whether it is in dark mode
     *
     * @param context The context
     * @return true then dark mode, false otherwise
     */
    public static boolean isDarkMode(@NonNull Context context) {
        return (getColorMode(context) == KEY_PREF_COLOR_MODE_NIGHT
                || (getColorMode(context) == KEY_PREF_COLOR_MODE_CUSTOM && Utils.isColorDark(getCustomColorModeBGColor(context))));
    }

    /**
     * Sets annotation style preset
     *
     * @param context        the context
     * @param annotType      annotation type
     * @param presetIndex    preset index
     * @param annotStyleJSON annotation style in JSON string format
     */
    public static void setAnnotStylePreset(@NonNull Context context, int annotType, int presetIndex, String annotStyleJSON) {
        setAnnotStylePreset(context, annotType, presetIndex, null, annotStyleJSON);
    }

    /**
     * Sets annotation style preset
     *
     * @param context        the context
     * @param annotType      annotation type
     * @param presetIndex    preset index
     * @param suffix         suffix string to uniquely identify this annot style
     * @param annotStyleJSON annotation style in JSON string format
     */
    public static void setAnnotStylePreset(@NonNull Context context, int annotType, int presetIndex, @Nullable String suffix, String annotStyleJSON) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        String key = KEY_PREF_PRESET_ANNOT_STYLE + "_" + annotType + "_" + presetIndex;
        if (suffix != null && !suffix.isEmpty()) {
            key = key + "_" + suffix;
        }
        editor.putString(key, annotStyleJSON);
        editor.apply();
    }

    /**
     * Gets annotation style preset by given tool mode and preset index
     *
     * @param context     the context
     * @param annotType   annotation type
     * @param presetIndex preset index
     * @return annotaiton style preset in JSON string format
     */
    public static String getAnnotStylePreset(@NonNull Context context, int annotType, int presetIndex) {
        return getAnnotStylePreset(context, annotType, presetIndex, null);
    }

    /**
     * Gets annotation style preset by given tool mode and preset index
     *
     * @param context     the context
     * @param annotType   annotation type
     * @param presetIndex preset index
     * @param suffix      suffix string to uniquely identify this annot style
     * @return annotaiton style preset in JSON string format
     */
    public static String getAnnotStylePreset(@NonNull Context context, int annotType, int presetIndex, @Nullable String suffix) {
        String key = KEY_PREF_PRESET_ANNOT_STYLE + "_" + annotType + "_" + presetIndex;
        if (suffix != null && !suffix.isEmpty()) {
            key = key + "_" + suffix;
        }
        return getDefaultSharedPreferences(context).getString(key, KEY_PREF_PRESET_ANNOT_STYLE_DEFAULT_VALUE);
    }

    /**
     * Sets annotation preset style picker tab index
     *
     * @param context     the context
     * @param annotType   annotation type
     * @param presetIndex preset index
     * @param suffix      suffix string to uniquely identify this annot style
     * @param tabIndex    the tab index
     */
    public static void setAnnotStylesTabIndex(@NonNull Context context, int annotType, int presetIndex, @NonNull String suffix, int tabIndex) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putInt(KEY_PREF_PRESET_ANNOT_STYLE_TAB_INDEX + "_" + annotType + "_" + presetIndex + "_" + suffix, tabIndex);
        editor.apply();
    }

    /**
     * Gets annotation preset style picker tab index
     *
     * @param context     the context
     * @param annotType   annotation type
     * @param presetIndex preset index
     * @param suffix      suffix string to uniquely identify this annot style
     * @return the tab index
     */
    public static int getAnnotStylesTabIndex(@NonNull Context context, int annotType, int presetIndex, @Nullable String suffix) {
        String key = KEY_PREF_PRESET_ANNOT_STYLE_TAB_INDEX + "_" + annotType + "_" + presetIndex;
        if (suffix != null && !suffix.isEmpty()) {
            key = key + "_" + suffix;
        }
        return getDefaultSharedPreferences(context).getInt(key, KEY_PREF_PRESET_ANNOT_STYLE_TAB_INDEX_DEFAULT_VALUE);
    }

    /**
     * Sets annotation preset style picker more annot type selected type
     *
     * @param context       the context
     * @param annotType     annotation type
     * @param presetIndex   preset index
     * @param suffix        suffix string to uniquely identify this annot style
     * @param moreAnnotType more annot type selected
     */
    public static void setAnnotStylesMoreAnnotType(@NonNull Context context, int annotType, int presetIndex, @NonNull String suffix, int moreAnnotType) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putInt(KEY_PREF_PRESET_ANNOT_STYLE_MORE_ANNOT_TYPE + "_" + annotType + "_" + presetIndex + "_" + suffix, moreAnnotType);
        editor.apply();
    }

    /**
     * Gets annotation preset style picker more annot type selected type
     *
     * @param context     the context
     * @param annotType   annotation type
     * @param presetIndex preset index
     * @param suffix      suffix string to uniquely identify this annot style
     * @return more annot type selected
     */
    public static int getAnnotStylesMoreAnnotType(@NonNull Context context, int annotType, int presetIndex, @Nullable String suffix, int defaultValue) {
        String key = KEY_PREF_PRESET_ANNOT_STYLE_MORE_ANNOT_TYPE + "_" + annotType + "_" + presetIndex;
        if (suffix != null && !suffix.isEmpty()) {
            key = key + "_" + suffix;
        }
        return getDefaultSharedPreferences(context).getInt(key, defaultValue);
    }

    /**
     * Gets a set of recent used colors string
     *
     * @param context the context
     * @return a set of recently used colors
     */
    public static String getRecentColors(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getString(KEY_PREF_RECENT_COLORS, "");
    }

    /**
     * Sets recently used colors
     *
     * @param context the context
     * @param colors  colors
     */
    public static void setRecentColors(@NonNull Context context, String colors) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_RECENT_COLORS, colors);
        editor.apply();
    }

    /**
     * Gets a set of recent used colors string
     *
     * @param context the context
     * @return a set of recently used colors
     */
    public static String getFavoriteColors(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getString(KEY_PREF_FAVORITE_COLORS, "");
    }

    /**
     * Sets recently used colors
     *
     * @param context the context
     * @param colors  colors
     */
    public static void setFavoriteColors(@NonNull Context context, String colors) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_FAVORITE_COLORS, colors);
        editor.apply();
    }

    /**
     * Sets color picker current page
     *
     * @param context The context
     * @param page    The current page
     */
    public static void setColorPickerPage(@NonNull Context context, int page) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putInt(KEY_PREF_COLOR_PICKER_PAGE, page);
        editor.apply();
    }

    /**
     * Gets color picker current page
     *
     * @param context The context
     * @return The stored page
     */
    public static int getColorPickerPage(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getInt(KEY_PREF_COLOR_PICKER_PAGE, KEY_PREF_COLOR_PICKER_DEFAULT_PAGE);
    }

    /**
     * Sets visible annotation types in annotation toolbar
     *
     * @param context The context
     */
    public static void setAnnotToolbarVisibleAnnotTypes(@NonNull Context context, String visibleAnnotTypes) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_TOOLBAR_VISIBLE_ANNOT_TYPE, visibleAnnotTypes);
        editor.apply();
    }

    /**
     * Gets visible annotation types in annotation toolbar
     *
     * @param context The context
     * @return visible annotation types
     */
    public static String getAnnotToolbarVisibleAnnotTypes(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getString(KEY_PREF_TOOLBAR_VISIBLE_ANNOT_TYPE, "");
    }

    /**
     * Sets the open url last viewed page cache
     *
     * @param context    the context
     * @param jsonString the cache json string
     */
    public static void setOpenUrlAsyncCache(@NonNull Context context, String jsonString) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_OPEN_URL_LAST_PAGE, jsonString);
        editor.apply();
    }

    /**
     * Gets the open url last viewed page cache
     *
     * @param context the context
     * @return the cache json string
     */
    public static String getOpenUrlAsyncCache(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getString(KEY_PREF_OPEN_URL_LAST_PAGE, "");
    }

    /**
     * Sets the open url last viewed zoom, h pos, v pos, rotation cache
     *
     * @param context    the context
     * @param jsonString the cache json string
     */
    public static void setOpenUrlPageStateAsyncCache(@NonNull Context context, String jsonString) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_OPEN_URL_LAST_PAGE_STATE, jsonString);
        editor.apply();
    }

    /**
     * Gets the open url last viewed zoom, h pos, v pos, rotation cache
     *
     * @param context the context
     * @return the cache json string
     */
    public static String getOpenUrlPageStateAsyncCache(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getString(KEY_PREF_OPEN_URL_LAST_PAGE_STATE, "");
    }

    /**
     * Gets whether to show rage scroll dialog
     */
    public static boolean getShowRageScrollingInfo(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_SHOW_RAGE_SCROLLING_INFO,
                KEY_PREF_SHOW_RAGE_SCROLLING_INFO_DEFAULT_VALUE);
    }

    /**
     * Sets whether to show rage scroll dialog
     */
    public static void updateShowRageScrollingInfo(Context context, boolean value) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_SHOW_RAGE_SCROLLING_INFO, value);
        editor.apply();
    }

    /**
     * Returns whether the number of tabs should be unlimited retrieved from shared preferences,
     * returns default value if not in shared preferences. This will be ignored if
     * {@link com.pdftron.pdf.config.ViewerConfig.Builder#maximumTabCount(int)} is used
     * when initializing the document viewer.
     *
     * @param context      The context
     * @param defaultValue default value to return if not set in shared preferences
     * @return True if unlimited tabs is enabled
     */
    public static boolean getUnlimitedTabsEnabled(@NonNull Context context, boolean defaultValue) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_UNLIMITED_TABS,
                defaultValue);
    }

    // todo bfung remove this later if not used

    public static void setSavedHomeToolbarMenu(@NonNull Context context, @NonNull String toolbarMenu) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_HOME_TOOLBAR_MENU, toolbarMenu);
        editor.apply();
    }

    public static String getSavedHomeToolbarMenu(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getString(KEY_PREF_HOME_TOOLBAR_MENU, null);
    }

    public static String getEditUriBackupFilePath(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getString(KEY_PREF_EDIT_URI_BACKUP_FILE_PATH,
                KEY_PREF_EDIT_URI_BACKUP_FILE_PATH_DEFAULT_VALUE);
    }

    public static void updateEditUriBackupFilePath(@NonNull Context context, String filepath) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_EDIT_URI_BACKUP_FILE_PATH, filepath);
        editor.apply();
    }

    /**
     * Sets whether the color management is enabled.
     *
     * @param context The context
     * @param enabled True if the color management is enabled
     */
    public static void setColorManagement(@NonNull Context context, boolean enabled) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_COLOR_MANAGEMENT, enabled);
        editor.apply();
    }

    /**
     * Returns whether the color management is enabled.
     *
     * @param context The context
     * @return True if the color management is enabled
     */
    public static boolean getColorManagement(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_COLOR_MANAGEMENT,
                KEY_PREF_COLOR_MANAGEMENT_DEFAULT_VALUE);
    }

    /**
     * Gets default stylus tool mode from shared preferences
     *
     * @param context The context
     * @return Default stylus tool mode
     */
    public static ToolManager.ToolMode getDefaultStylusToolMode(@NonNull Context context) {
        String toolModeValue = getDefaultSharedPreferences(context)
                .getString(KEY_PREF_DEFAULT_STYLUS_TOOL_MODE, String.valueOf(mDefaultToolValue));
        return (ToolManager.ToolMode) ToolManager.ToolMode.toolModeFor(Integer.parseInt(toolModeValue));
    }

    /**
     * Sets the default stylus tool mode in the shared preferences.
     *
     * @param context The context
     * @param mode    The tool mode
     */
    public static void setDefaultStylusToolMode(@NonNull Context context, ToolManager.ToolMode mode) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_DEFAULT_STYLUS_TOOL_MODE, String.valueOf(mode.getValue()));
        editor.apply();
    }

    /**
     * Gets draw with finger mode from shared preferences
     *
     * @param context The context
     * @return true if finger should draw when stylus as pen is enabled
     */
    public static boolean getDrawWithFinger(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_DRAW_WITH_FINGER,
                KEY_PREF_DRAW_WITH_FINGER_DEFAULT_VALUE);
    }

    /**
     * Sets whether the finger should draw when stylus as pen is enabled in the shared preferences.
     *
     * @param context        The context
     * @param drawWithFinger whether the finger should draw when stylus as pen is enabled
     */
    public static void setDrawWithFinger(@NonNull Context context, boolean drawWithFinger) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_DRAW_WITH_FINGER, drawWithFinger);
        editor.apply();
    }

    /**
     * Returns whether quick bookmark creation is enabled in the viewer.
     *
     * @param context The context
     * @return True whether quick bookmark creation is enabled.
     */
    public static boolean getQuickBookmarkCreation(@NonNull Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_QUICK_BOOKMARK_CREATION, KEY_PREF_QUICK_BOOKMARK_CREATION_DEFAULT_VALUE);
    }
}
