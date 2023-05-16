package com.pdftron.pdf.config;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.Gravity;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StyleRes;

import com.pdftron.pdf.controls.PdfViewCtrlTabFragment2;
import com.pdftron.pdf.controls.PdfViewCtrlTabHostFragment2;
import com.pdftron.pdf.controls.ReflowControl;
import com.pdftron.pdf.controls.ThumbnailsViewFragment;
import com.pdftron.pdf.controls.UserBookmarkDialogFragment;
import com.pdftron.pdf.dialog.ViewModePickerDialogFragment;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.bottombar.builder.BottomBarBuilder;
import com.pdftron.pdf.widget.toolbar.TopToolbarMenuId;
import com.pdftron.pdf.widget.toolbar.builder.AnnotationToolbarBuilder;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarButtonType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class is responsible for configuration
 * {@link PdfViewCtrlTabHostFragment2} and
 * {@link PdfViewCtrlTabFragment2}.
 * See {@link Builder} for details.
 */
@SuppressWarnings("JavaDoc")
public class ViewerConfig implements Parcelable {

    /**
     * @hide
     */
    public boolean isFullscreenModeEnabled() {
        return fullscreenModeEnabled;
    }

    /**
     * @hide
     */
    public boolean isMultiTabEnabled() {
        return multiTabEnabled;
    }

    /**
     * @hide
     */
    public boolean isDocumentEditingEnabled() {
        return documentEditingEnabled;
    }

    /**
     * @hide
     */
    public boolean isLongPressQuickMenuEnabled() {
        return longPressQuickMenuEnabled;
    }

    /**
     * @hide
     */
    public boolean isShowPageNumberIndicator() {
        return showPageNumberIndicator;
    }

    /**
     * @hide
     */
    public int getPageNumberIndicatorPosition() {
        return pageNumberIndicatorPosition;
    }

    /**
     * @hide
     */
    public boolean isPermanentPageNumberIndicator() {
        return permanentPageNumberIndicator;
    }

    /**
     * @hide
     */
    public boolean isShowQuickNavigationButton() {
        return showQuickNavigationButton;
    }

    /**
     * @hide
     */
    public boolean isShowBottomNavBar() {
        return showBottomNavBar;
    }

    /**
     * @hide
     */
    public boolean isShowDocumentSlider() {
        return showDocumentSlider;
    }

    /**
     * @hide
     */
    public boolean isShowAppBar() {
        return showAppBar;
    }

    /**
     * @hide
     */
    public boolean isShowTopToolbar() {
        return showAppBar && showTopToolbar;
    }

    /**
     * @hide
     */
    public boolean isShowBottomToolbar() {
        return showBottomToolbar;
    }

    /**
     * @hide
     */
    public boolean isPermanentToolbars() {
        return permanentToolbars;
    }

    /**
     * @hide
     */
    public boolean isShowThumbnailView() {
        return showThumbnailView;
    }

    /**
     * @hide
     */
    public boolean isShowBookmarksView() {
        return showBookmarksView;
    }

    /**
     * @hide
     */
    public String getToolbarTitle() {
        return toolbarTitle;
    }

    /**
     * @hide
     */
    public boolean isShowSearchView() {
        return showSearchView;
    }

    /**
     * @hide
     */
    public boolean isShowShareOption() {
        return showShareOption;
    }

    /**
     * @hide
     */
    public boolean isShowDocumentSettingsOption() {
        return showDocumentSettingsOption;
    }

    /**
     * @hide
     */
    public boolean isShowAnnotationToolbarOption() {
        return showAnnotationToolbarOption;
    }

    /**
     * @hide
     */
    public boolean isShowFormToolbarOption() {
        return showFormToolbarOption;
    }

    /**
     * @hide
     */
    public boolean isShowFillAndSignToolbarOption() {
        return showFillAndSignToolbarOption;
    }

    /**
     * @hide
     */
    public boolean isShowDigitalSignaturesToolbarOption() {
        return showDigitalSignaturesOption;
    }

    /**
     * @hide
     */
    public boolean isShowOpenFileOption() {
        return showOpenFileOption;
    }

    /**
     * @hide
     */
    public boolean isShowOpenUrlOption() {
        return showOpenUrlOption;
    }

    /**
     * @hide
     */
    public boolean isShowEditPagesOption() {
        return showEditPagesOption;
    }

    /**
     * @hide
     */
    public boolean isShowPrintOption() {
        return showPrintOption;
    }

    /**
     * @hide
     */
    public boolean isShowCloseTabOption() {
        return showCloseTabOption;
    }

    /**
     * @hide
     */
    public boolean isShowViewLayersToolbarOption() {
        return showViewLayersOption;
    }

    /**
     * @hide
     */
    public boolean isShowAnnotationReplyReviewState() {
        return showAnnotationReplyReviewState;
    }

    /**
     * @hide
     */
    public boolean isShowAnnotationsList() {
        return showAnnotationsList;
    }

    /**
     * @hide
     */
    public boolean isShowOutlineList() {
        return showOutlineList;
    }

    /**
     * @hide
     */
    public boolean isShowUserBookmarksList() {
        return showUserBookmarksList;
    }

    /**
     * @hide
     */
    public boolean isNavigationListAsSheetOnLargeDevice() {
        return navigationListAsSheetOnLargeDevice;
    }

    /**
     * @hide
     */
    public boolean isRightToLeftModeEnabled() {
        return rightToLeftModeEnabled;
    }

    /**
     * @hide
     */
    public boolean isShowRightToLeftOption() {
        return showRightToLeftOption;
    }

    /**
     * @hide
     */
    public PDFViewCtrlConfig getPdfViewCtrlConfig() {
        return pdfViewCtrlConfig;
    }

    /**
     * @hide
     */
    public int getToolManagerBuilderStyleRes() {
        return toolManagerBuilderStyleRes;
    }

    /**
     * @hide
     */
    public ToolManagerBuilder getToolManagerBuilder() {
        return toolManagerBuilder;
    }

    /**
     * @hide
     */
    public String getConversionOptions() {
        return conversionOptions;
    }

    /**
     * @hide
     */
    public String getOpenUrlCachePath() {
        return openUrlCachePath;
    }

    /**
     * @hide
     */
    public String getSaveCopyExportPath() {
        return saveCopyExportPath;
    }

    /**
     * @hide
     */
    public String getConversionCachePath() {
        return conversionCachePath;
    }

    /**
     * @hide
     */
    public boolean isUseSupportActionBar() {
        return useSupportActionBar;
    }

    /**
     * @hide
     */
    public boolean isShowSaveCopyOption() {
        return showSaveCopyOption;
    }

    /**
     * @hide
     */
    public boolean isShowCropOption() {
        return showCropOption;
    }

    /**
     * @hide
     */
    public boolean isRestrictDownloadUsage() {
        return restrictDownloadUsage;
    }

    /**
     * @hide
     */
    public int getLayoutInDisplayCutoutMode() {
        return layoutInDisplayCutoutMode;
    }

    /**
     * @hide
     */
    public boolean isThumbnailViewEditingEnabled() {
        return thumbnailViewEditingEnabled;
    }

    /**
     * @hide
     */
    public boolean isUserBookmarksListEditingEnabled() {
        return userBookmarksListEditingEnabled;
    }

    public int getUserBookmarksListEditingMode() {
        return userBookmarksListEditingMode;
    }

    /**
     * @hide
     */
    public boolean isOutlineListEditingEnabled() {
        return outlineListEditingEnabled;
    }

    /**
     * @hide
     */
    public boolean isQuickBookmarkCreationEnabled() {
        return quickBookmarkCreation;
    }

    /**
     * @hide
     */
    public boolean isUserBookmarkCreationEnabled() {
        return userBookmarkCreationEnabled;
    }

    /**
     * @hide
     */
    public boolean annotationsListEditingEnabled() {
        return annotationsListEditingEnabled;
    }

    /**
     * @hide
     */
    public boolean annotationsListFilterEnabled() {
        return annotationListFilterEnabled;
    }

    /**
     * @hide
     */
    public int getMaximumTabCount() {
        return maximumTabCount;
    }

    /**
     * @hide
     */
    public boolean isAutoHideToolbarEnabled() {
        return enableAutoHideToolbar;
    }

    /**
     * @hide
     */
    public boolean isUseStandardLibrary() {
        return useStandardLibrary;
    }

    /**
     * @hide
     */
    @Nullable
    public int[] getHideViewModeIds() {
        return hideViewModeIds;
    }

    /**
     * @hide
     */
    public boolean isShowReflowOption() {
        return showReflowOption;
    }

    /**
     * @hide
     */
    public int getReflowOrientation() {
        return reflowOrientation;
    }

    /**
     * @hide
     */
    public int getToolbarItemGravity() {
        return toolbarItemGravity;
    }

    /**
     * @hide
     */
    public boolean isShowEditMenuOption() {
        return showEditMenuOption;
    }

    /**
     * @hide
     */
    public boolean isPageStackEnabled() {
        return pageStackEnabled;
    }

    /**
     * @hide
     */
    public boolean isShowToolbarSwitcher() {
        return showToolbarSwitcher;
    }

    /**
     * @hide
     */
    @Nullable
    public String[] getToolbarsToHide() {
        return toolbarsToHide;
    }

    /**
     * @hide
     */
    public String getInitialToolbarTag() {
        return initialToolbarTag;
    }

    /**
     * @hide
     */
    public boolean isRememberLastToolbar() {
        return rememberLastToolbar;
    }

    /**
     * @hide
     */
    public boolean isRememberLastUsedTool() {
        return rememberLastUsedTool;
    }

    /**
     * @hide
     */
    public boolean skipReadOnlyCheck() {
        return skipReadOnlyCheck;
    }

    /**
     * @hide
     */
    public boolean showConversionDialog() {
        return showConversionDialog;
    }

    /**
     * @hide
     */
    public boolean showDownloadDialog() {
        return showDownloadDialog;
    }

    /**
     * @hide
     */
    public boolean isShowFileAttachmentOption() {
        return showFileAttachmentOption;
    }

    /**
     * @hide
     */
    @NonNull
    public List<AnnotationToolbarBuilder> getToolbarBuilders() {
        return toolbarBuilders;
    }

    /**
     * @hide
     */
    public int[] getExcludedAnnotationListTypes() {
        return excludedAnnotationListTypes;
    }

    /**
     * @hide
     */
    public boolean isHidePresetBar() {
        return hidePresetBar;
    }

    /**
     * @hide
     */
    public int[] getPresetBarsToHide() {
        return presetBarsToHideIds;
    }

    /**
     * @hide
     */
    public int[] getTopToolbarMenuIds() {
        return topToolbarMenuIds;
    }

    /**
     * @hide
     */
    @NonNull
    public boolean isSaveToolbarItemOrder() {
        return saveToolbarItemOrder;
    }

    /**
     * @hide
     */
    @Nullable
    public BottomBarBuilder getBottomBarBuilder() {
        return bottomBarBuilder;
    }

    /**
     * @hide
     */
    @Nullable
    public int[] getHideThumbnailFilterModes() {
        return hideThumbnailFilterModes;
    }

    /**
     * @hide
     */
    @Nullable
    public String[] getHideThumbnailEditOptions() {
        return hideThumbnailEditOptions;
    }

    /**
     * @hide
     */
    @Nullable
    public int[] getHideSaveCopyOptions() {
        return hideSaveCopyOptions;
    }

    /**
     * @hide
     */
    public boolean isImageInReflowEnabled() {
        return imageInReflowEnabled;
    }

    /**
     * @hide
     */
    public boolean isUseCompactViewer() {
        return useCompactViewer;
    }

    /**
     * @hide
     */
    public boolean isAutoSortUserBookmarks() {
        return autoSortUserBookmarks;
    }

    /**
     * @hide
     */
    public boolean isTabletLayoutEnabled() {
        return tabletLayoutEnabled;
    }

    /**
     * @hide
     */
    public boolean isOpenSavedCopyInNewTab() {
        return openSavedCopyInNewTab;
    }

    /**
     * @hide
     */
    public boolean isOpenUrlPasswordCheckEnabled() {
        return openUrlPasswordCheckEnabled;
    }

    /**
     * @hide
     */
    public boolean annotationPositionSnappingEnabled() {
        return annotationPositionSnappingEnabled;
    }

    /**
     * @hide
     */
    @NonNull
    public boolean alwaysShowAsReply() {
        return alwaysShowAsReply;
    }

    private boolean fullscreenModeEnabled = true;
    private boolean multiTabEnabled = true;
    private boolean documentEditingEnabled = true;
    private boolean longPressQuickMenuEnabled = true;
    private boolean showPageNumberIndicator = true;
    private int pageNumberIndicatorPosition = 0;
    private boolean permanentPageNumberIndicator;
    private boolean showQuickNavigationButton = true;
    private boolean showBottomNavBar = true;
    private boolean showDocumentSlider = true;
    private boolean showThumbnailView = true;
    private boolean showBookmarksView = true;
    private String toolbarTitle;
    private boolean showSearchView = true;
    private boolean showShareOption = true;
    private boolean showDocumentSettingsOption = true;
    private boolean showAnnotationToolbarOption = true;
    private boolean showOpenFileOption = true;
    private boolean showOpenUrlOption = true;
    private boolean showEditPagesOption = true;
    private boolean showPrintOption = true;
    private boolean showCloseTabOption = true;
    private boolean showAnnotationsList = true;
    private boolean showAnnotationReplyReviewState = true;
    private boolean showOutlineList = true;
    private boolean showUserBookmarksList = true;
    private boolean rightToLeftModeEnabled = false;
    private boolean showRightToLeftOption = false;
    private PDFViewCtrlConfig pdfViewCtrlConfig;
    private int toolManagerBuilderStyleRes = 0;
    private ToolManagerBuilder toolManagerBuilder;
    private String conversionOptions;
    private String openUrlCachePath;
    private String saveCopyExportPath;
    @Nullable
    private String conversionCachePath;
    private boolean useSupportActionBar = true;
    private boolean showSaveCopyOption = true;
    private boolean showCropOption = true;
    private boolean restrictDownloadUsage;
    private int layoutInDisplayCutoutMode = Utils.isPie() ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES : 0;
    private boolean thumbnailViewEditingEnabled = true;
    private boolean userBookmarksListEditingEnabled = true;
    private int userBookmarksListEditingMode = UserBookmarkDialogFragment.CONTEXT_MENU_EDIT_ITEM_BIT | UserBookmarkDialogFragment.CONTEXT_MENU_DELETE_ITEM_BIT | UserBookmarkDialogFragment.CONTEXT_MENU_DELETE_ALL_BIT;
    private boolean outlineListEditingEnabled = true;
    private boolean annotationsListEditingEnabled = true;
    private int maximumTabCount = 0;
    private boolean enableAutoHideToolbar = true;
    private boolean showFormToolbarOption = true;
    private boolean showFillAndSignToolbarOption = true;
    private boolean navigationListAsSheetOnLargeDevice = true;
    private boolean showViewLayersOption = true;
    private boolean showAppBar = true;
    private boolean showTopToolbar = true;
    private boolean showBottomToolbar = true;
    private boolean permanentToolbars;
    private boolean useStandardLibrary;
    private int[] hideViewModeIds;
    private int[] excludedAnnotationListTypes;
    private boolean showReflowOption = true;
    private int reflowOrientation = ReflowControl.FOLLOW_PDFVIEWCTRL;
    private boolean showEditMenuOption = true;
    @NonNull
    private List<AnnotationToolbarBuilder> toolbarBuilders = new ArrayList<>();
    @Nullable
    private BottomBarBuilder bottomBarBuilder;
    private boolean pageStackEnabled = true;
    private boolean showToolbarSwitcher = true;
    private String[] toolbarsToHide;
    private String initialToolbarTag = null;
    private boolean rememberLastToolbar = true;
    private boolean rememberLastUsedTool = true;
    // Whether to skip the read only check in PDFViewCtrl
    private boolean skipReadOnlyCheck = false;
    private boolean showConversionDialog = true;
    private boolean showDownloadDialog = true;
    private boolean showFileAttachmentOption = true;
    private int[] hideThumbnailFilterModes;
    private String[] hideThumbnailEditOptions;
    private boolean imageInReflowEnabled = true;
    private boolean userBookmarkCreationEnabled = true;
    private boolean useCompactViewer;
    private boolean showDigitalSignaturesOption = true;
    private boolean autoSortUserBookmarks = true;
    private boolean saveToolbarItemOrder = true;
    private boolean tabletLayoutEnabled = true;
    private boolean openSavedCopyInNewTab = true;
    private int[] hideSaveCopyOptions;
    private boolean hidePresetBar = false;
    @Nullable
    private int[] presetBarsToHideIds;
    private boolean openUrlPasswordCheckEnabled;
    private boolean annotationListFilterEnabled = true;
    private int[] topToolbarMenuIds;
    private int toolbarItemGravity = Gravity.END;
    private boolean quickBookmarkCreation = false;
    private boolean annotationPositionSnappingEnabled = true;
    private boolean alwaysShowAsReply = false;

    public ViewerConfig() {
    }

    protected ViewerConfig(Parcel in) {
        fullscreenModeEnabled = in.readByte() != 0;
        multiTabEnabled = in.readByte() != 0;
        documentEditingEnabled = in.readByte() != 0;
        longPressQuickMenuEnabled = in.readByte() != 0;
        showPageNumberIndicator = in.readByte() != 0;
        pageNumberIndicatorPosition = in.readInt();
        permanentPageNumberIndicator = in.readByte() != 0;
        showQuickNavigationButton = in.readByte() != 0;
        showBottomNavBar = in.readByte() != 0;
        showDocumentSlider = in.readByte() != 0;
        showThumbnailView = in.readByte() != 0;
        showBookmarksView = in.readByte() != 0;
        toolbarTitle = in.readString();
        showSearchView = in.readByte() != 0;
        showShareOption = in.readByte() != 0;
        showDocumentSettingsOption = in.readByte() != 0;
        showAnnotationToolbarOption = in.readByte() != 0;
        showOpenFileOption = in.readByte() != 0;
        showOpenUrlOption = in.readByte() != 0;
        showEditPagesOption = in.readByte() != 0;
        showPrintOption = in.readByte() != 0;
        showCloseTabOption = in.readByte() != 0;
        showAnnotationsList = in.readByte() != 0;
        showAnnotationReplyReviewState = in.readByte() != 0;
        showOutlineList = in.readByte() != 0;
        showUserBookmarksList = in.readByte() != 0;
        rightToLeftModeEnabled = in.readByte() != 0;
        showRightToLeftOption = in.readByte() != 0;
        pdfViewCtrlConfig = in.readParcelable(PDFViewCtrlConfig.class.getClassLoader());
        toolManagerBuilderStyleRes = in.readInt();
        toolManagerBuilder = in.readParcelable(ToolManagerBuilder.class.getClassLoader());
        conversionOptions = in.readString();
        openUrlCachePath = in.readString();
        saveCopyExportPath = in.readString();
        useSupportActionBar = in.readByte() != 0;
        showSaveCopyOption = in.readByte() != 0;
        restrictDownloadUsage = in.readByte() != 0;
        showCropOption = in.readByte() != 0;
        layoutInDisplayCutoutMode = in.readInt();
        thumbnailViewEditingEnabled = in.readByte() != 0;
        userBookmarksListEditingEnabled = in.readByte() != 0;
        userBookmarksListEditingMode = in.readInt();
        outlineListEditingEnabled = in.readByte() != 0;
        annotationsListEditingEnabled = in.readByte() != 0;
        maximumTabCount = in.readInt();
        enableAutoHideToolbar = in.readByte() != 0;
        showFormToolbarOption = in.readByte() != 0;
        showFillAndSignToolbarOption = in.readByte() != 0;
        navigationListAsSheetOnLargeDevice = in.readByte() != 0;
        showViewLayersOption = in.readByte() != 0;
        showAppBar = in.readByte() != 0;
        showTopToolbar = in.readByte() != 0;
        showBottomToolbar = in.readByte() != 0;
        permanentToolbars = in.readByte() != 0;
        useStandardLibrary = in.readByte() != 0;
        int hideViewModeIdsSize = in.readInt();
        hideViewModeIds = new int[hideViewModeIdsSize];
        in.readIntArray(hideViewModeIds);
        int excludedAnnotationListTypesSize = in.readInt();
        excludedAnnotationListTypes = new int[excludedAnnotationListTypesSize];
        in.readIntArray(excludedAnnotationListTypes);
        showReflowOption = in.readByte() != 0;
        reflowOrientation = in.readInt();
        showEditMenuOption = in.readByte() != 0;
        toolbarBuilders = in.createTypedArrayList(AnnotationToolbarBuilder.CREATOR);
        pageStackEnabled = in.readByte() != 0;
        showToolbarSwitcher = in.readByte() != 0;
        int toolbarsToHideSize = in.readInt();
        toolbarsToHide = new String[toolbarsToHideSize];
        in.readStringArray(toolbarsToHide);
        initialToolbarTag = in.readString();
        rememberLastToolbar = in.readByte() != 0;
        rememberLastUsedTool = in.readByte() != 0;
        conversionCachePath = in.readString();
        bottomBarBuilder = in.readParcelable(BottomBarBuilder.class.getClassLoader());
        skipReadOnlyCheck = in.readByte() != 0;
        showConversionDialog = in.readByte() != 0;
        showFileAttachmentOption = in.readByte() != 0;
        int hideThumbnailFilterModesSize = in.readInt();
        hideThumbnailFilterModes = new int[hideThumbnailFilterModesSize];
        in.readIntArray(hideThumbnailFilterModes);
        int hideThumbnailEditOptionsSize = in.readInt();
        hideThumbnailEditOptions = new String[hideThumbnailEditOptionsSize];
        in.readStringArray(hideThumbnailEditOptions);
        imageInReflowEnabled = in.readByte() != 0;
        userBookmarkCreationEnabled = in.readByte() != 0;
        useCompactViewer = in.readByte() != 0;
        showDigitalSignaturesOption = in.readByte() != 0;
        autoSortUserBookmarks = in.readByte() != 0;
        saveToolbarItemOrder = in.readByte() != 0;
        tabletLayoutEnabled = in.readByte() != 0;
        openSavedCopyInNewTab = in.readByte() != 0;
        int hideSaveCopyOptionsSize = in.readInt();
        hideSaveCopyOptions = new int[hideSaveCopyOptionsSize];
        in.readIntArray(hideSaveCopyOptions);
        int presetBarsToHideSize = in.readInt();
        presetBarsToHideIds = new int[presetBarsToHideSize];
        in.readIntArray(presetBarsToHideIds);
        showDownloadDialog = in.readByte() != 0;
        openUrlPasswordCheckEnabled = in.readByte() != 0;
        annotationListFilterEnabled = in.readByte() != 0;
        hidePresetBar = in.readByte() != 0;
        int topToolbarMenuIdSize = in.readInt();
        topToolbarMenuIds = new int[topToolbarMenuIdSize];
        in.readIntArray(topToolbarMenuIds);
        toolbarItemGravity = in.readInt();
        quickBookmarkCreation = in.readByte() != 0;
        annotationPositionSnappingEnabled = in.readByte() != 0;
        alwaysShowAsReply = in.readByte() != 0;
    }

    public static final Creator<ViewerConfig> CREATOR = new Creator<ViewerConfig>() {
        @Override
        public ViewerConfig createFromParcel(Parcel in) {
            return new ViewerConfig(in);
        }

        @Override
        public ViewerConfig[] newArray(int size) {
            return new ViewerConfig[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByte((byte) (fullscreenModeEnabled ? 1 : 0));
        parcel.writeByte((byte) (multiTabEnabled ? 1 : 0));
        parcel.writeByte((byte) (documentEditingEnabled ? 1 : 0));
        parcel.writeByte((byte) (longPressQuickMenuEnabled ? 1 : 0));
        parcel.writeByte((byte) (showPageNumberIndicator ? 1 : 0));
        parcel.writeInt(pageNumberIndicatorPosition);
        parcel.writeByte((byte) (permanentPageNumberIndicator ? 1 : 0));
        parcel.writeByte((byte) (showQuickNavigationButton ? 1 : 0));
        parcel.writeByte((byte) (showBottomNavBar ? 1 : 0));
        parcel.writeByte((byte) (showDocumentSlider ? 1 : 0));
        parcel.writeByte((byte) (showThumbnailView ? 1 : 0));
        parcel.writeByte((byte) (showBookmarksView ? 1 : 0));
        parcel.writeString(toolbarTitle);
        parcel.writeByte((byte) (showSearchView ? 1 : 0));
        parcel.writeByte((byte) (showShareOption ? 1 : 0));
        parcel.writeByte((byte) (showDocumentSettingsOption ? 1 : 0));
        parcel.writeByte((byte) (showAnnotationToolbarOption ? 1 : 0));
        parcel.writeByte((byte) (showOpenFileOption ? 1 : 0));
        parcel.writeByte((byte) (showOpenUrlOption ? 1 : 0));
        parcel.writeByte((byte) (showEditPagesOption ? 1 : 0));
        parcel.writeByte((byte) (showPrintOption ? 1 : 0));
        parcel.writeByte((byte) (showCloseTabOption ? 1 : 0));
        parcel.writeByte((byte) (showAnnotationsList ? 1 : 0));
        parcel.writeByte((byte) (showAnnotationReplyReviewState ? 1 : 0));
        parcel.writeByte((byte) (showOutlineList ? 1 : 0));
        parcel.writeByte((byte) (showUserBookmarksList ? 1 : 0));
        parcel.writeByte((byte) (rightToLeftModeEnabled ? 1 : 0));
        parcel.writeByte((byte) (showRightToLeftOption ? 1 : 0));
        parcel.writeParcelable(pdfViewCtrlConfig, i);
        parcel.writeInt(toolManagerBuilderStyleRes);
        parcel.writeParcelable(toolManagerBuilder, i);
        parcel.writeString(conversionOptions);
        parcel.writeString(openUrlCachePath);
        parcel.writeString(saveCopyExportPath);
        parcel.writeByte((byte) (useSupportActionBar ? 1 : 0));
        parcel.writeByte((byte) (showSaveCopyOption ? 1 : 0));
        parcel.writeByte((byte) (restrictDownloadUsage ? 1 : 0));
        parcel.writeByte((byte) (showCropOption ? 1 : 0));
        parcel.writeInt(layoutInDisplayCutoutMode);
        parcel.writeByte((byte) (thumbnailViewEditingEnabled ? 1 : 0));
        parcel.writeByte((byte) (userBookmarksListEditingEnabled ? 1 : 0));
        parcel.writeInt(userBookmarksListEditingMode);
        parcel.writeByte((byte) (outlineListEditingEnabled ? 1 : 0));
        parcel.writeByte((byte) (annotationsListEditingEnabled ? 1 : 0));
        parcel.writeInt(maximumTabCount);
        parcel.writeByte((byte) (enableAutoHideToolbar ? 1 : 0));
        parcel.writeByte((byte) (showFormToolbarOption ? 1 : 0));
        parcel.writeByte((byte) (showFillAndSignToolbarOption ? 1 : 0));
        parcel.writeByte((byte) (navigationListAsSheetOnLargeDevice ? 1 : 0));
        parcel.writeByte((byte) (showViewLayersOption ? 1 : 0));
        parcel.writeByte((byte) (showAppBar ? 1 : 0));
        parcel.writeByte((byte) (showTopToolbar ? 1 : 0));
        parcel.writeByte((byte) (showBottomToolbar ? 1 : 0));
        parcel.writeByte((byte) (permanentToolbars ? 1 : 0));
        parcel.writeByte((byte) (useStandardLibrary ? 1 : 0));
        if (null == hideViewModeIds) {
            hideViewModeIds = new int[0];
        }
        int hideViewModeIdsSize = hideViewModeIds.length;
        parcel.writeInt(hideViewModeIdsSize);
        parcel.writeIntArray(hideViewModeIds);
        if (null == excludedAnnotationListTypes) {
            excludedAnnotationListTypes = new int[0];
        }
        int excludedAnnotationListTypesSize = excludedAnnotationListTypes.length;
        parcel.writeInt(excludedAnnotationListTypesSize);
        parcel.writeIntArray(excludedAnnotationListTypes);
        parcel.writeByte((byte) (showReflowOption ? 1 : 0));
        parcel.writeInt(reflowOrientation);
        parcel.writeByte((byte) (showEditMenuOption ? 1 : 0));
        parcel.writeTypedList(this.toolbarBuilders);
        parcel.writeByte((byte) (pageStackEnabled ? 1 : 0));
        parcel.writeByte((byte) (showToolbarSwitcher ? 1 : 0));
        if (null == toolbarsToHide) {
            toolbarsToHide = new String[0];
        }
        int toolbarsToHideSize = toolbarsToHide.length;
        parcel.writeInt(toolbarsToHideSize);
        parcel.writeStringArray(toolbarsToHide);
        parcel.writeString(initialToolbarTag);
        parcel.writeByte((byte) (rememberLastToolbar ? 1 : 0));
        parcel.writeByte((byte) (rememberLastUsedTool ? 1 : 0));
        parcel.writeString(conversionCachePath);
        parcel.writeParcelable(bottomBarBuilder, i);
        parcel.writeByte((byte) (skipReadOnlyCheck ? 1 : 0));
        parcel.writeByte((byte) (showConversionDialog ? 1 : 0));
        parcel.writeByte((byte) (showFileAttachmentOption ? 1 : 0));
        if (null == hideThumbnailFilterModes) {
            hideThumbnailFilterModes = new int[0];
        }
        int hideThumbnailFilterModesSize = hideThumbnailFilterModes.length;
        parcel.writeInt(hideThumbnailFilterModesSize);
        parcel.writeIntArray(hideThumbnailFilterModes);
        if (null == hideThumbnailEditOptions) {
            hideThumbnailEditOptions = new String[0];
        }
        int hideThumbnailEditOptionsSize = hideThumbnailEditOptions.length;
        parcel.writeInt(hideThumbnailEditOptionsSize);
        parcel.writeStringArray(hideThumbnailEditOptions);
        parcel.writeByte((byte) (imageInReflowEnabled ? 1 : 0));
        parcel.writeByte((byte) (userBookmarkCreationEnabled ? 1 : 0));
        parcel.writeByte((byte) (useCompactViewer ? 1 : 0));
        parcel.writeByte((byte) (showDigitalSignaturesOption ? 1 : 0));
        parcel.writeByte((byte) (autoSortUserBookmarks ? 1 : 0));
        parcel.writeByte((byte) (saveToolbarItemOrder ? 1 : 0));
        parcel.writeByte((byte) (tabletLayoutEnabled ? 1 : 0));
        parcel.writeByte((byte) (openSavedCopyInNewTab ? 1 : 0));
        if (null == hideSaveCopyOptions) {
            hideSaveCopyOptions = new int[0];
        }
        int hideSaveCopyOptionsSize = hideSaveCopyOptions.length;
        parcel.writeInt(hideSaveCopyOptionsSize);
        parcel.writeIntArray(hideSaveCopyOptions);
        if (null == presetBarsToHideIds) {
            presetBarsToHideIds = new int[0];
        }
        int presetBarToHideSize = presetBarsToHideIds.length;
        parcel.writeInt(presetBarToHideSize);
        parcel.writeIntArray(presetBarsToHideIds);
        parcel.writeByte((byte) (showDownloadDialog ? 1 : 0));
        parcel.writeByte((byte) (openUrlPasswordCheckEnabled ? 1 : 0));
        parcel.writeByte((byte) (annotationListFilterEnabled ? 1 : 0));
        parcel.writeByte((byte) (hidePresetBar ? 1 : 0));
        if (null == topToolbarMenuIds) {
            topToolbarMenuIds = new int[0];
        }
        int topToolbarMenuIdSize = topToolbarMenuIds.length;
        parcel.writeInt(topToolbarMenuIdSize);
        parcel.writeIntArray(topToolbarMenuIds);
        parcel.writeInt(toolbarItemGravity);
        parcel.writeByte((byte) (quickBookmarkCreation ? 1 : 0));
        parcel.writeByte((byte) (annotationPositionSnappingEnabled ? 1 : 0));
        parcel.writeByte((byte) (alwaysShowAsReply ? 1 : 0));
    }

    /**
     * Builder class used to create an instance of {@link ViewerConfig}.
     */
    public static class Builder {
        private final ViewerConfig mViewerConfig = new ViewerConfig();

        /**
         * Whether to enable full screen mode.
         */
        public Builder fullscreenModeEnabled(boolean fullscreenModeEnabled) {
            mViewerConfig.fullscreenModeEnabled = fullscreenModeEnabled;
            return this;
        }

        /**
         * Whether to enable multi-tab mode.
         */
        public Builder multiTabEnabled(boolean multiTab) {
            mViewerConfig.multiTabEnabled = multiTab;
            return this;
        }

        /**
         * Whether to enable document editing.
         * When disabled, all menu options that will edit the document will be gone.
         */
        public Builder documentEditingEnabled(boolean documentEditingEnabled) {
            mViewerConfig.documentEditingEnabled = documentEditingEnabled;
            return this;
        }

        /**
         * Whether to enable long press quick menu.
         */
        public Builder longPressQuickMenuEnabled(boolean longPressQuickMenuEnabled) {
            mViewerConfig.longPressQuickMenuEnabled = longPressQuickMenuEnabled;
            return this;
        }

        /**
         * Whether to show page number indicator overlay. Default to true.
         */
        public Builder showPageNumberIndicator(boolean showPageNumberIndicator) {
            mViewerConfig.showPageNumberIndicator = showPageNumberIndicator;
            return this;
        }

        /**
         * Sets the position of page number indicator.
         * Default to {@link com.pdftron.pdf.controls.PageIndicatorLayout#POSITION_BOTTOM_START}.
         */
        public Builder pageNumberIndicatorPosition(int pageNumberIndicatorPosition) {
            mViewerConfig.pageNumberIndicatorPosition = pageNumberIndicatorPosition;
            return this;
        }

        /**
         * Whether to show page number indicator always. Default to false.
         * This value is ignored if showPageNumberIndicator returns false.
         */
        public Builder permanentPageNumberIndicator(boolean permanentPageNumberIndicator) {
            mViewerConfig.permanentPageNumberIndicator = permanentPageNumberIndicator;
            return this;
        }

        /**
         * Whether to show quick page navigation forward and backward buttons. Default true.
         */
        public Builder showQuickNavigationButton(boolean showQuickNavigationButton) {
            mViewerConfig.showQuickNavigationButton = showQuickNavigationButton;
            return this;
        }

        /**
         * Whether to show page stack navigation buttons.
         */
        public Builder pageStackEnabled(boolean pageStackEnabled) {
            mViewerConfig.pageStackEnabled = pageStackEnabled;
            return this;
        }

        /**
         * Set which toolbars to hide in the toolbar switcher dialog. Must have at least 1 toolbar
         * showing.
         *
         * @param toolbarsToHide array referencing toolbar tags for toolbars that should be hidden
         *                       in the toolbar switcher
         */
        public Builder hideToolbars(String[] toolbarsToHide) {
            mViewerConfig.toolbarsToHide = new String[toolbarsToHide.length];
            for (int i = 0; i < toolbarsToHide.length; i++) {
                mViewerConfig.toolbarsToHide[i] = toolbarsToHide[i];
            }
            return this;
        }

        /**
         * @deprecated Whether to show bottom navigation bar. Default to true. For legacy UI only.
         */
        @Deprecated
        public Builder showBottomNavBar(boolean showBottomNavBar) {
            mViewerConfig.showBottomNavBar = showBottomNavBar;
            return this;
        }

        /**
         * Whether to show document slider. Default to true.
         */
        public Builder showDocumentSlider(boolean showDocumentSlider) {
            mViewerConfig.showDocumentSlider = showDocumentSlider;
            return this;
        }

        /**
         * Whether to show top toolbar and annotation toolbar. Default to true.
         */
        public Builder showAppBar(boolean showAppBar) {
            mViewerConfig.showAppBar = showAppBar;
            return this;
        }

        /**
         * Whether to show top toolbar. Default to true.
         * If {@link ViewerConfig#showAppBar} returns false,
         * then this value is ignored.
         */
        public Builder showTopToolbar(boolean showTopToolbar) {
            mViewerConfig.showTopToolbar = showTopToolbar;
            return this;
        }

        /**
         * Whether to show bottom toolbar. Default to true. For new UI only.
         */
        public Builder showBottomToolbar(boolean showBottomToolbar) {
            mViewerConfig.showBottomToolbar = showBottomToolbar;
            return this;
        }

        /**
         * @deprecated use {@link #permanentToolbars(boolean)} instead.
         * Whether to permanently show top toolbar. Default to false.
         * When true, the toolbar will be shown above the viewer instead of an overlay.
         * If {@link ViewerConfig#showTopToolbar} returns false,
         * then this value is ignored.
         */
        @Deprecated
        public Builder permanentTopToolbar(boolean permanentTopToolbar) {
            mViewerConfig.permanentToolbars = permanentTopToolbar;
            return this;
        }

        /**
         * Whether to permanently show top and bottom toolbars. Default to false.
         * When true, the viewer will be shown in between the toolbars. Single tap will
         * not hide the toolbars.
         * Both top and bottom applies for new UI, however only top applies for old UI.
         * If {@link ViewerConfig#showTopToolbar} returns false,
         * then this value is ignored.
         */
        public Builder permanentToolbars(boolean permanentToolbars) {
            mViewerConfig.permanentToolbars = permanentToolbars;
            return this;
        }

        /**
         * If {@link ViewerConfig#showBottomNavBar} returns false,
         * then this value is ignored.
         * Whether to show thumbnail view icon.
         */
        public Builder showThumbnailView(boolean showThumbnailView) {
            mViewerConfig.showThumbnailView = showThumbnailView;
            return this;
        }

        /**
         * If {@link ViewerConfig#showBottomNavBar} returns false,
         * then this value is ignored.
         * If all of {@link ViewerConfig#showAnnotationsList},
         * {@link ViewerConfig#showOutlineList}, and
         * {@link ViewerConfig#showUserBookmarksList} return false,
         * then this value is ignored.
         * Whether to show bookmarks view icon.
         */
        public Builder showBookmarksView(boolean showBookmarksView) {
            mViewerConfig.showBookmarksView = showBookmarksView;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Toolbar title.
         */
        public Builder toolbarTitle(String toolbarTitle) {
            mViewerConfig.toolbarTitle = toolbarTitle;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show search view icon.
         */
        public Builder showSearchView(boolean showSearchView) {
            mViewerConfig.showSearchView = showSearchView;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show share icon.
         */
        public Builder showShareOption(boolean showShareOption) {
            mViewerConfig.showShareOption = showShareOption;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show bookmarks view icon.
         */
        public Builder showDocumentSettingsOption(boolean showDocumentSettingsOption) {
            mViewerConfig.showDocumentSettingsOption = showDocumentSettingsOption;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show annotation toolbar view icon.
         */
        public Builder showAnnotationToolbarOption(boolean showAnnotationToolbarOption) {
            mViewerConfig.showAnnotationToolbarOption = showAnnotationToolbarOption;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show prepare form toolbar menu option.
         */
        public Builder showFormToolbarOption(boolean showFormToolbarOption) {
            mViewerConfig.showFormToolbarOption = showFormToolbarOption;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show fill and sign toolbar menu option.
         */
        public Builder showFillAndSignToolbarOption(boolean showFillAndSignToolbarOption) {
            mViewerConfig.showFillAndSignToolbarOption = showFillAndSignToolbarOption;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show reflow menu option.
         * <p>
         * If {@link #useStandardLibrary(boolean)} is set to true,
         * then this value is ignored.
         */
        public Builder showReflowOption(boolean showReflowOption) {
            mViewerConfig.showReflowOption = showReflowOption;
            return this;
        }

        /**
         * Sets the scrolling direction of the reflow control.
         * Value has to be one of {@link ReflowControl#HORIZONTAL} or {@link ReflowControl#VERTICAL},
         * default to horizontal.
         */
        public Builder reflowOrientation(int orientation) {
            mViewerConfig.reflowOrientation = orientation;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show edit menu menu option.
         */
        public Builder showEditMenuOption(boolean showEditMenuOption) {
            mViewerConfig.showEditMenuOption = showEditMenuOption;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show the view layers toolbar menu option.
         */
        public Builder showViewLayersToolbarOption(boolean showViewLayersOption) {
            mViewerConfig.showViewLayersOption = showViewLayersOption;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show open file option.
         */
        public Builder showOpenFileOption(boolean showOpenFileOption) {
            mViewerConfig.showOpenFileOption = showOpenFileOption;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show open url option.
         */
        public Builder showOpenUrlOption(boolean showOpenUrlOption) {
            mViewerConfig.showOpenUrlOption = showOpenUrlOption;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show edit pages option.
         */
        public Builder showEditPagesOption(boolean showEditPagesOption) {
            mViewerConfig.showEditPagesOption = showEditPagesOption;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show print option.
         */
        public Builder showPrintOption(boolean showPrintOption) {
            mViewerConfig.showPrintOption = showPrintOption;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show save a copy option.
         */
        public Builder showSaveCopyOption(boolean showSaveCopyOption) {
            mViewerConfig.showSaveCopyOption = showSaveCopyOption;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show digital signatures toolbar menu option.
         */
        public Builder showDigitalSignaturesOption(boolean showDigitalSignaturesOption) {
            mViewerConfig.showDigitalSignaturesOption = showDigitalSignaturesOption;
            return this;
        }

        /**
         * Set which items to hide in {@link com.pdftron.pdf.dialog.ViewModePickerDialogFragment}
         *
         * @param ids
         * @return
         */
        public Builder hideViewModeItems(@NonNull ViewModePickerDialogFragment.ViewModePickerItems[] ids) {
            mViewerConfig.hideViewModeIds = new int[ids.length];
            for (int i = 0; i < ids.length; i++) {
                mViewerConfig.hideViewModeIds[i] = ids[i].getValue();
            }
            return this;
        }

        /**
         * Sets the list of excluded annotation types that will not be displayed in the AnnotationDialogFragment
         *
         * @param excludedAnnotationListTypes
         */
        public Builder excludeAnnotationListTypes(int[] excludedAnnotationListTypes) {
            mViewerConfig.excludedAnnotationListTypes = new int[excludedAnnotationListTypes.length];
            System.arraycopy(excludedAnnotationListTypes, 0, mViewerConfig.excludedAnnotationListTypes, 0, excludedAnnotationListTypes.length);
            return this;
        }

        /**
         * Whether to hide preset bar. Default to false.
         */
        public Builder hidePresetBar(boolean hidePresetBar) {
            mViewerConfig.hidePresetBar = hidePresetBar;
            return this;
        }

        /**
         * Hides the preset bars for the selected tool types.
         *
         * @param toolbarButtonTypes
         */
        public Builder hidePresetBars(@NonNull ToolbarButtonType[] toolbarButtonTypes) {
            mViewerConfig.presetBarsToHideIds = new int[toolbarButtonTypes.length];
            for (int i = 0; i < toolbarButtonTypes.length; i++) {
                mViewerConfig.presetBarsToHideIds[i] = toolbarButtonTypes[i].getValue();
            }
            return this;
        }

        /**
         * Defines custom top toolbar menu items.
         *
         * @param toolbarButtonTypes
         */
        public Builder topToolbarMenuIds(@NonNull TopToolbarMenuId[] toolbarButtonTypes) {
            mViewerConfig.topToolbarMenuIds = new int[toolbarButtonTypes.length];
            for (int i = 0; i < toolbarButtonTypes.length; i++) {
                mViewerConfig.topToolbarMenuIds[i] = toolbarButtonTypes[i].value();
            }
            return this;
        }

        /**
         * @deprecated use {@link #hideViewModeItems(ViewModePickerDialogFragment.ViewModePickerItems[])} instead.
         * <p>
         * Set showCropOption to true to show the cropping
         * option in {@link com.pdftron.pdf.dialog.ViewModePickerDialogFragment}
         */
        @Deprecated
        public Builder showCropOption(boolean showCropOption) {
            mViewerConfig.showCropOption = showCropOption;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show close document option in the overflow menu.
         */
        public Builder showCloseTabOption(boolean showCloseTabOption) {
            mViewerConfig.showCloseTabOption = showCloseTabOption;
            return this;
        }

        /**
         * Whether to show annotation reply review state in collaboration viewer.
         * Default to true.
         */
        public Builder showAnnotationReplyReviewState(boolean showAnnotationReplyReviewState) {
            mViewerConfig.showAnnotationReplyReviewState = showAnnotationReplyReviewState;
            return this;
        }

        /**
         * If {@link ViewerConfig#showBookmarksView} returns false,
         * then this value is ignored.
         * Whether to show annotation list.
         */
        public Builder showAnnotationsList(boolean showAnnotationsList) {
            mViewerConfig.showAnnotationsList = showAnnotationsList;
            return this;
        }

        /**
         * If {@link ViewerConfig#showBookmarksView} returns false,
         * then this value is ignored.
         * Whether to show outline list.
         */
        public Builder showOutlineList(boolean showOutlineList) {
            mViewerConfig.showOutlineList = showOutlineList;
            return this;
        }

        /**
         * If {@link ViewerConfig#showBookmarksView} returns false,
         * then this value is ignored.
         * Whether to show user bookmarks list.
         */
        public Builder showUserBookmarksList(boolean showUserBookmarksList) {
            mViewerConfig.showUserBookmarksList = showUserBookmarksList;
            return this;
        }

        /**
         * Whether navigation list is opened as side sheet on large tablet or dialog.
         */
        public Builder navigationListAsSheetOnLargeDevice(boolean navigationListAsSheetOnLargeDevice) {
            mViewerConfig.navigationListAsSheetOnLargeDevice = navigationListAsSheetOnLargeDevice;
            return this;
        }

        /**
         * Whether to view documents from right to left.
         * If {@link ViewerConfig#showRightToLeftOption} return false,
         * then this value is ignored.
         */
        public Builder rightToLeftModeEnabled(boolean rightToLeftModeEnabled) {
            mViewerConfig.rightToLeftModeEnabled = rightToLeftModeEnabled;
            return this;
        }

        /**
         * Whether to enable RTL option in the view mode dialog.
         * If {@link #useStandardLibrary(boolean)} is set to true,
         * then this value is ignored.
         */
        public Builder showRightToLeftOption(boolean showRightToLeftOption) {
            mViewerConfig.showRightToLeftOption = showRightToLeftOption;
            return this;
        }

        /**
         * Sets the {@link PDFViewCtrlConfig} for {@link com.pdftron.pdf.PDFViewCtrl}
         */
        public Builder pdfViewCtrlConfig(PDFViewCtrlConfig config) {
            mViewerConfig.pdfViewCtrlConfig = config;
            return this;
        }

        /**
         * Sets the style resource ID used for {@link ToolManagerBuilder}
         */
        public Builder toolManagerBuilderStyleRes(@StyleRes int styleRes) {
            mViewerConfig.toolManagerBuilderStyleRes = styleRes;
            return this;
        }

        /**
         * @deprecated replaced by {@link #toolManagerBuilder} instead
         * Sets tool manager builder for building tool manager
         */
        @Deprecated
        public Builder setToolManagerBuilder(ToolManagerBuilder toolManagerBuilder) {
            mViewerConfig.toolManagerBuilder = toolManagerBuilder;
            return this;
        }

        /**
         * Sets tool manager builder for building tool manager
         */
        public Builder toolManagerBuilder(ToolManagerBuilder toolManagerBuilder) {
            mViewerConfig.toolManagerBuilder = toolManagerBuilder;
            return this;
        }

        /**
         * Sets {@link com.pdftron.pdf.ConversionOptions} for non-pdf conversion
         */
        public Builder conversionOptions(String conversionOptions) {
            mViewerConfig.conversionOptions = conversionOptions;
            return this;
        }

        /**
         * Sets the cache folder path for open URL files
         */
        public Builder openUrlCachePath(String openUrlCachePath) {
            mViewerConfig.openUrlCachePath = openUrlCachePath;
            return this;
        }

        /**
         * Sets the folder path for all save a copy options
         */
        public Builder saveCopyExportPath(String exportPath) {
            mViewerConfig.saveCopyExportPath = exportPath;
            return this;
        }

        /**
         * Sets the folder path for converted files
         */
        public Builder conversionCachePath(String convertedPath) {
            mViewerConfig.conversionCachePath = convertedPath;
            return this;
        }

        /**
         * Sets whether to use SupportActionBar for inflating ToolBar menu
         */
        public Builder useSupportActionBar(boolean useSupportActionBar) {
            mViewerConfig.useSupportActionBar = useSupportActionBar;
            return this;
        }

        /**
         * Sets whether to restrict data used when viewing an online PDF
         */
        public Builder restrictDownloadUsage(boolean restrictDownloadUsage) {
            mViewerConfig.restrictDownloadUsage = restrictDownloadUsage;
            return this;
        }

        /**
         * Sets the display cutout mode. Only available when in full screen mode.
         */
        @RequiresApi(api = Build.VERSION_CODES.P)
        public Builder layoutInDisplayCutoutMode(int cutoutMode) {
            mViewerConfig.layoutInDisplayCutoutMode = cutoutMode;
            return this;
        }

        /**
         * Sets whether the {@link com.pdftron.pdf.controls.ThumbnailsViewFragment} can modify the document
         * Default to true
         * If {@link #documentEditingEnabled} is false, this value is ignored
         */
        public Builder thumbnailViewEditingEnabled(boolean thumbnailViewEditingEnabled) {
            mViewerConfig.thumbnailViewEditingEnabled = thumbnailViewEditingEnabled;
            return this;
        }

        /**
         * Sets whether the {@link com.pdftron.pdf.controls.UserBookmarkDialogFragment} can modify the document
         * Default to true
         * If {@link #documentEditingEnabled} is false, this value is ignored
         */
        public Builder userBookmarksListEditingEnabled(boolean userBookmarksListEditingEnabled) {
            mViewerConfig.userBookmarksListEditingEnabled = userBookmarksListEditingEnabled;
            return this;
        }

        /**
         * Sets which editing options are allowed in the {@link com.pdftron.pdf.controls.UserBookmarkDialogFragment}
         * Default to show all options.
         * Valid inputs are
         * CONTEXT_MENU_DELETE_ITEM_BIT | CONTEXT_MENU_DELETE_ALL_BIT | CONTEXT_MENU_EDIT_ITEM_BIT
         * For example, the following will disable rename only:
         * userBookmarksListEditingMode(
         * UserBookmarkDialogFragment.CONTEXT_MENU_DELETE_ITEM_BIT |
         * UserBookmarkDialogFragment.CONTEXT_MENU_DELETE_ALL_BIT)
         */
        public Builder userBookmarksListEditingMode(int userBookmarksListEditingMode) {
            mViewerConfig.userBookmarksListEditingMode = userBookmarksListEditingMode;
            return this;
        }

        /**
         * Sets whether the {@link com.pdftron.pdf.controls.OutlineDialogFragment} can modify the document
         * Default to true.
         * If {@link #documentEditingEnabled} is false, this value is ignored
         */
        public Builder outlineListEditingEnabled(boolean outlineListEditingEnabled) {
            mViewerConfig.outlineListEditingEnabled = outlineListEditingEnabled;
            return this;
        }

        /**
         * Sets whether bookmark creation is enabled in {@link com.pdftron.pdf.controls.UserBookmarkDialogFragment}.
         * Default to true.
         * If {@link #userBookmarksListEditingEnabled} is false, this value is ignored.
         */
        public Builder userBookmarkCreationEnabled(boolean userBookmarkCreationEnabled) {
            mViewerConfig.userBookmarkCreationEnabled = userBookmarkCreationEnabled;
            return this;
        }

        /**
         * Sets whether the bookmark button is visible in the viewer to create bookmarks with a single tap.
         * Default to false.
         */
        public Builder quickBookmarkCreation(boolean quickBookmarkCreation) {
            mViewerConfig.quickBookmarkCreation = quickBookmarkCreation;
            return this;
        }

        /**
         * Sets whether the {@link com.pdftron.pdf.controls.AnnotationDialogFragment} can modify the document
         * Default to true.
         * If {@link #documentEditingEnabled} is false, this value is ignored
         */
        public Builder annotationsListEditingEnabled(boolean annotationsListEditingEnabled) {
            mViewerConfig.annotationsListEditingEnabled = annotationsListEditingEnabled;
            return this;
        }

        /**
         * Sets whether the filter in {@link com.pdftron.pdf.controls.AnnotationDialogFragment} is enabled.
         */
        public Builder annotationsListFilterEnabled(boolean annotationListFilterEnabled) {
            mViewerConfig.annotationListFilterEnabled = annotationListFilterEnabled;
            return this;
        }

        /**
         * Sets the maximum number of tabs allowed. By default, the maximum count is 3 on phone and 5 on tablet.
         * Adding subsequent tabs will remove other tabs to respect the limit.
         */
        public Builder maximumTabCount(int count) {
            mViewerConfig.maximumTabCount = count;
            return this;
        }

        /**
         * @deprecated The viewer will no longer auto hide from v7.1.1. This flag is ignored.
         * Sets whether the options toolbar should automatically hide when the user is interacting
         * with the document viewer. By default, the toolbar will automatically hide.
         */
        @Deprecated
        public Builder autoHideToolbarEnabled(boolean shouldAutoHide) {
            mViewerConfig.enableAutoHideToolbar = shouldAutoHide;
            return this;
        }

        /**
         * Sets whether the PDFTron library used is Standard version or Full version.
         * When Standard version is used, all features that are not available
         * in Standard version will be hidden and will not be able to enable.
         * Default to false.
         */
        public Builder useStandardLibrary(boolean useStandard) {
            mViewerConfig.useStandardLibrary = useStandard;
            return this;
        }

        /**
         * Sets whether to use 2-line toolbar or 1-line toolbar
         *
         * @param useCompactViewer true if use 1-line toolbar, false otherwise.
         *                         Default to false.
         */
        public Builder useCompactViewer(boolean useCompactViewer) {
            mViewerConfig.useCompactViewer = useCompactViewer;
            return this;
        }

        /**
         * Sets which filter modes to hide in {@link com.pdftron.pdf.controls.ThumbnailsViewFragment}
         *
         * @param modes the filter modes to hide
         */
        public Builder hideThumbnailFilterModes(@NonNull ThumbnailsViewFragment.FilterModes[] modes) {
            mViewerConfig.hideThumbnailFilterModes = new int[modes.length];
            for (int i = 0; i < modes.length; i++) {
                mViewerConfig.hideThumbnailFilterModes[i] = modes[i].getValue();
            }
            return this;
        }

        /**
         * Sets which edit options to hide in {@link com.pdftron.pdf.controls.ThumbnailsViewFragment}
         *
         * @param options the edit options to hide
         */
        public Builder hideThumbnailEditOptions(@NonNull ThumbnailsViewFragment.ThumbnailsViewEditOptions[] options) {
            mViewerConfig.hideThumbnailEditOptions = new String[options.length];
            for (int i = 0; i < options.length; i++) {
                mViewerConfig.hideThumbnailEditOptions[i] = options[i].name();
            }
            return this;
        }

        /**
         * Sets which save copy options to hide in the save copy menu
         * If {@link #showSaveCopyOption} is true, this value is ignored
         * Some menu options may be ignored if {@link #useStandardLibrary} is true
         *
         * @param modes the menu options to hide
         */
        public Builder hideSaveCopyOptions(@NonNull int[] modes) {
            mViewerConfig.hideSaveCopyOptions = new int[modes.length];
            System.arraycopy(modes, 0, mViewerConfig.hideSaveCopyOptions, 0, modes.length);
            return this;
        }

        public ViewerConfig build() {
            if (mViewerConfig.useStandardLibrary) {
                // turn off features
                mViewerConfig.showReflowOption = false;
                mViewerConfig.showRightToLeftOption = false;
            }
            if (!mViewerConfig.documentEditingEnabled) {
                // turn off editing features
                mViewerConfig.outlineListEditingEnabled = false;
                mViewerConfig.annotationsListEditingEnabled = false;
                mViewerConfig.thumbnailViewEditingEnabled = false;
                mViewerConfig.showEditPagesOption = false;
                mViewerConfig.showAnnotationToolbarOption = false;
                mViewerConfig.showFormToolbarOption = false;
                mViewerConfig.showFillAndSignToolbarOption = false;
            }
            return mViewerConfig;
        }

        /**
         * Whether to show the toolbar switcher if using {@link PdfViewCtrlTabHostFragment2}
         */
        public Builder showToolbarSwitcher(boolean showToolbarSwitcher) {
            mViewerConfig.showToolbarSwitcher = showToolbarSwitcher;
            return this;
        }

        /**
         * Adds the toolbar builder to list of toolbars that will be created in the viewer
         * using {@link PdfViewCtrlTabHostFragment2}.
         *
         * @param builderToAdd the builder containing toolbar information that will be created in the viewer
         */
        public Builder addToolbarBuilder(@NonNull AnnotationToolbarBuilder builderToAdd) {
            // Ensure no two toolbars are added with the same tag
            for (AnnotationToolbarBuilder toolbarBuilder : mViewerConfig.toolbarBuilders) {
                if (toolbarBuilder.getToolbarTag().equals(builderToAdd.getToolbarTag())) {
                    throw new RuntimeException("Toolbars in a single viewer should not have the same tag.");
                }
            }
            mViewerConfig.toolbarBuilders.add(builderToAdd);
            return this;
        }

        /**
         * Sets whether the toolbars should save the ordering of the tools.
         */
        public Builder saveToolbarItemOrder(boolean saveToolbarItemOrder) {
            mViewerConfig.saveToolbarItemOrder = saveToolbarItemOrder;
            return this;
        }

        /**
         * Set the toolbar builder that will be used to inflate the bottom bar
         *
         * @param builderToSet the builder containing toolbar information that will be used to inflate the bottom bar
         */
        public Builder bottomBarBuilder(@NonNull BottomBarBuilder builderToSet) {
            if (builderToSet == null) {
                throw new RuntimeException("BottomBarBuilder cannot be null");
            }
            mViewerConfig.bottomBarBuilder = builderToSet;
            return this;
        }

        /**
         * Sets the initial toolbar to show when the viewer is shown. If {@link #rememberLastUsedToolbar} is
         * set to true, the initial toolbar will be overridden by the last used toolbar.
         * For {@link PdfViewCtrlTabHostFragment2}
         *
         * @param toolbarTag related to the toolbar that we want to show
         * @deprecated see {@link #initialToolbarTag}
         */
        @Deprecated
        public Builder setInitialToolbarTag(String toolbarTag) {
            return initialToolbarTag(toolbarTag);
        }

        /**
         * Sets the initial toolbar to show when the viewer is shown. If {@link #rememberLastUsedToolbar} is
         * set to true, the initial toolbar will be overridden by the last used toolbar.
         * For {@link PdfViewCtrlTabHostFragment2}
         *
         * @param toolbarTag related to the toolbar that we want to show
         */
        public Builder initialToolbarTag(String toolbarTag) {
            mViewerConfig.initialToolbarTag = toolbarTag;
            return this;
        }

        /**
         * Whether to show the last used toolbar when the viewer is shown, for {@link PdfViewCtrlTabHostFragment2}
         */
        public Builder rememberLastUsedToolbar(boolean rememberLastToolbar) {
            mViewerConfig.rememberLastToolbar = rememberLastToolbar;
            return this;
        }

        /**
         * Whether to select the last used tool when the viewer is launched, for {@link PdfViewCtrlTabHostFragment2}
         */
        public Builder rememberLastUsedTool(boolean rememberLastToolbar) {
            mViewerConfig.rememberLastUsedTool = rememberLastToolbar;
            return this;
        }

        /**
         * Whether the viewer should skip read only checks on a document. Default false.
         */
        public Builder skipReadOnlyCheck(boolean skipReadOnlyCheck) {
            mViewerConfig.skipReadOnlyCheck = skipReadOnlyCheck;
            return this;
        }

        /**
         * Whether the viewer should show the conversion progress dialog. Default true.
         */
        public Builder showConversionDialog(boolean showConversionDialog) {
            mViewerConfig.showConversionDialog = showConversionDialog;
            return this;
        }

        /**
         * Whether the viewer should show the download progress dialog. Default true.
         */
        public Builder showDownloadDialog(boolean showDownloadDialog) {
            mViewerConfig.showDownloadDialog = showDownloadDialog;
            return this;
        }

        /**
         * Whether the viewer will show file attachment menu if the document contains attachments.
         * Default true.
         */
        public Builder showFileAttachmentOption(boolean showFileAttachmentOption) {
            mViewerConfig.showFileAttachmentOption = showFileAttachmentOption;
            return this;
        }

        /**
         * Whether to show images in reflow mode.
         * Default true.
         */
        public Builder imageInReflowEnabled(boolean imageInReflowEnabled) {
            mViewerConfig.imageInReflowEnabled = imageInReflowEnabled;
            return this;
        }

        /**
         * Sets whether to automatically sort the User Bookmarks when they are added.
         * User will not be able to sort the list manually in the UI.
         * Otherwise, user will be able to sort the list manually in the UI but
         * User Bookmarks are appended to the end when they are added.
         *
         * @param autoSortUserBookmarks true if auto sorting of user bookmarks is enabled.
         *                              Default to true.
         */
        public Builder autoSortUserBookmarks(boolean autoSortUserBookmarks) {
            mViewerConfig.autoSortUserBookmarks = autoSortUserBookmarks;
            return this;
        }

        /**
         * Sets whether the tablet layout should be enabled on tablet devices
         */
        public Builder tabletLayoutEnabled(boolean tabletLayoutEnabled) {
            mViewerConfig.tabletLayoutEnabled = tabletLayoutEnabled;
            return this;
        }

        /**
         * Sets whether the new saved file should open after saving
         * Default to true.
         */
        public Builder openSavedCopyInNewTab(boolean openSavedCopyInNewTab) {
            mViewerConfig.openSavedCopyInNewTab = openSavedCopyInNewTab;
            return this;
        }

        /**
         * Sets whether to allow user to enter password for open URL files
         * Default to false.
         */
        public Builder openUrlPasswordCheckEnabled(boolean openUrlPasswordCheckEnabled) {
            mViewerConfig.openUrlPasswordCheckEnabled = openUrlPasswordCheckEnabled;
            return this;
        }

        /**
         * Sets the toolbar item layout gravity
         */
        public Builder toolbarItemGravity(int layoutGravity) {
            mViewerConfig.toolbarItemGravity = layoutGravity;
            return this;
        }

        /**
         * Sets whether an annotation's position will snap to the page center or other annotations of the same group when
         * the user moves the annotation in the viewer.
         * <p>
         * Default to true.
         */
        public Builder annotationPositionSnappingEnabled(boolean annotationPositionSnappingEnabled) {
            mViewerConfig.annotationPositionSnappingEnabled = annotationPositionSnappingEnabled;
            return this;
        }

        /**
         * Sets whether an annotation reply always instead of annotation comment in collaboration viewer only
         * <p>
         * Defaults to false
         */
        public Builder alwaysShowAsReply(boolean alwaysShowAsReply) {
            mViewerConfig.alwaysShowAsReply = alwaysShowAsReply;
            return this;
        }
    }

    @SuppressWarnings("EqualsReplaceableByObjectsCall")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ViewerConfig that = (ViewerConfig) o;

        if (fullscreenModeEnabled != that.fullscreenModeEnabled) return false;
        if (multiTabEnabled != that.multiTabEnabled) return false;
        if (documentEditingEnabled != that.documentEditingEnabled) return false;
        if (longPressQuickMenuEnabled != that.longPressQuickMenuEnabled) return false;
        if (showPageNumberIndicator != that.showPageNumberIndicator) return false;
        if (pageNumberIndicatorPosition != that.pageNumberIndicatorPosition) return false;
        if (permanentPageNumberIndicator != that.permanentPageNumberIndicator) return false;
        if (showQuickNavigationButton != that.showQuickNavigationButton) return false;
        if (showBottomNavBar != that.showBottomNavBar) return false;
        if (showDocumentSlider != that.showDocumentSlider) return false;
        if (showThumbnailView != that.showThumbnailView) return false;
        if (showBookmarksView != that.showBookmarksView) return false;
        if (showSearchView != that.showSearchView) return false;
        if (showShareOption != that.showShareOption) return false;
        if (showDocumentSettingsOption != that.showDocumentSettingsOption) return false;
        if (showAnnotationToolbarOption != that.showAnnotationToolbarOption) return false;
        if (showOpenFileOption != that.showOpenFileOption) return false;
        if (showOpenUrlOption != that.showOpenUrlOption) return false;
        if (showEditPagesOption != that.showEditPagesOption) return false;
        if (showPrintOption != that.showPrintOption) return false;
        if (showCloseTabOption != that.showCloseTabOption) return false;
        if (showAnnotationsList != that.showAnnotationsList) return false;
        if (showAnnotationReplyReviewState != that.showAnnotationReplyReviewState) return false;
        if (showOutlineList != that.showOutlineList) return false;
        if (showUserBookmarksList != that.showUserBookmarksList) return false;
        if (rightToLeftModeEnabled != that.rightToLeftModeEnabled) return false;
        if (showRightToLeftOption != that.showRightToLeftOption) return false;
        if (toolManagerBuilderStyleRes != that.toolManagerBuilderStyleRes) return false;
        if (useSupportActionBar != that.useSupportActionBar) return false;
        if (showSaveCopyOption != that.showSaveCopyOption) return false;
        if (showCropOption != that.showCropOption) return false;
        if (restrictDownloadUsage != that.restrictDownloadUsage) return false;
        if (layoutInDisplayCutoutMode != that.layoutInDisplayCutoutMode) return false;
        if (thumbnailViewEditingEnabled != that.thumbnailViewEditingEnabled) return false;
        if (userBookmarksListEditingEnabled != that.userBookmarksListEditingEnabled) return false;
        if (userBookmarksListEditingMode != that.userBookmarksListEditingMode) return false;
        if (outlineListEditingEnabled != that.outlineListEditingEnabled) return false;
        if (annotationsListEditingEnabled != that.annotationsListEditingEnabled) return false;
        if (maximumTabCount != that.maximumTabCount) return false;
        if (enableAutoHideToolbar != that.enableAutoHideToolbar) return false;
        if (showFormToolbarOption != that.showFormToolbarOption) return false;
        if (showFillAndSignToolbarOption != that.showFillAndSignToolbarOption) return false;
        if (navigationListAsSheetOnLargeDevice != that.navigationListAsSheetOnLargeDevice)
            return false;
        if (showViewLayersOption != that.showViewLayersOption) return false;
        if (showAppBar != that.showAppBar) return false;
        if (showTopToolbar != that.showTopToolbar) return false;
        if (showBottomToolbar != that.showBottomToolbar) return false;
        if (permanentToolbars != that.permanentToolbars) return false;
        if (useStandardLibrary != that.useStandardLibrary) return false;
        if (showReflowOption != that.showReflowOption) return false;
        if (reflowOrientation != that.reflowOrientation) return false;
        if (toolbarItemGravity != that.toolbarItemGravity) return false;
        if (showEditMenuOption != that.showEditMenuOption) return false;
        if (pageStackEnabled != that.pageStackEnabled) return false;
        if (showToolbarSwitcher != that.showToolbarSwitcher) return false;
        if (toolbarTitle != null ? !toolbarTitle.equals(that.toolbarTitle) : that.toolbarTitle != null)
            return false;
        if (pdfViewCtrlConfig != null ? !pdfViewCtrlConfig.equals(that.pdfViewCtrlConfig) : that.pdfViewCtrlConfig != null)
            return false;
        if (toolManagerBuilder != null ? !toolManagerBuilder.equals(that.toolManagerBuilder) : that.toolManagerBuilder != null)
            return false;
        if (conversionOptions != null ? !conversionOptions.equals(that.conversionOptions) : that.conversionOptions != null)
            return false;
        if (openUrlCachePath != null ? !openUrlCachePath.equals(that.openUrlCachePath) : that.openUrlCachePath != null)
            return false;
        if (saveCopyExportPath != null ? !saveCopyExportPath.equals(that.saveCopyExportPath) : that.saveCopyExportPath != null)
            return false;
        if (!toolbarBuilders.equals(that.toolbarBuilders))
            return false;
        if (!Arrays.equals(excludedAnnotationListTypes, that.excludedAnnotationListTypes)) {
            return false;
        }
        if (!Arrays.equals(hideViewModeIds, that.hideViewModeIds)) {
            return false;
        }
        if (!Arrays.equals(hideThumbnailFilterModes, that.hideThumbnailFilterModes)) {
            return false;
        }
        if (!Arrays.equals(hideThumbnailEditOptions, that.hideThumbnailEditOptions)) {
            return false;
        }
        if (!Arrays.equals(hideSaveCopyOptions, that.hideSaveCopyOptions)) {
            return false;
        }
        if (initialToolbarTag != null ? !initialToolbarTag.equals(that.initialToolbarTag) : that.initialToolbarTag != null)
            return false;
        if (rememberLastToolbar != that.rememberLastToolbar) return false;
        if (rememberLastUsedTool != that.rememberLastUsedTool) return false;
        if (conversionCachePath != null ? !conversionCachePath.equals(that.conversionCachePath) : that.conversionCachePath != null)
            return false;
        if (bottomBarBuilder != null ? !bottomBarBuilder.equals(that.bottomBarBuilder) : that.bottomBarBuilder != null)
            return false;
        if (skipReadOnlyCheck != that.skipReadOnlyCheck) return false;
        if (showConversionDialog != that.showConversionDialog) return false;
        if (showDownloadDialog != that.showDownloadDialog) return false;
        if (showFileAttachmentOption != that.showFileAttachmentOption) return false;
        if (imageInReflowEnabled != that.imageInReflowEnabled) return false;
        if (userBookmarkCreationEnabled != that.userBookmarkCreationEnabled) return false;
        if (useCompactViewer != that.useCompactViewer) return false;
        if (showDigitalSignaturesOption != that.showDigitalSignaturesOption) return false;
        if (autoSortUserBookmarks != that.autoSortUserBookmarks) return false;
        if (saveToolbarItemOrder != that.saveToolbarItemOrder) return false;
        if (tabletLayoutEnabled != that.tabletLayoutEnabled) return false;
        if (openSavedCopyInNewTab != that.openSavedCopyInNewTab) return false;
        if (openUrlPasswordCheckEnabled != that.openUrlPasswordCheckEnabled) return false;
        if (hidePresetBar != that.hidePresetBar) return false;
        if (!Arrays.equals(presetBarsToHideIds, that.presetBarsToHideIds)) {
            return false;
        }
        if (annotationListFilterEnabled != that.annotationListFilterEnabled) return false;
        if (!Arrays.equals(topToolbarMenuIds, that.topToolbarMenuIds)) {
            return false;
        }
        if (quickBookmarkCreation != that.quickBookmarkCreation) return false;
        if (annotationPositionSnappingEnabled != that.annotationPositionSnappingEnabled) return false;
        if (alwaysShowAsReply != that.alwaysShowAsReply) return false;
        return Arrays.equals(toolbarsToHide, that.toolbarsToHide);
    }

    @Override
    public int hashCode() {
        int result = (fullscreenModeEnabled ? 1 : 0);
        result = 31 * result + (multiTabEnabled ? 1 : 0);
        result = 31 * result + (documentEditingEnabled ? 1 : 0);
        result = 31 * result + (longPressQuickMenuEnabled ? 1 : 0);
        result = 31 * result + (showPageNumberIndicator ? 1 : 0);
        result = 31 * result + pageNumberIndicatorPosition;
        result = 31 * result + (permanentPageNumberIndicator ? 1 : 0);
        result = 31 * result + (showQuickNavigationButton ? 1 : 0);
        result = 31 * result + (showBottomNavBar ? 1 : 0);
        result = 31 * result + (showDocumentSlider ? 1 : 0);
        result = 31 * result + (showThumbnailView ? 1 : 0);
        result = 31 * result + (showBookmarksView ? 1 : 0);
        result = 31 * result + (toolbarTitle != null ? toolbarTitle.hashCode() : 0);
        result = 31 * result + (showSearchView ? 1 : 0);
        result = 31 * result + (showShareOption ? 1 : 0);
        result = 31 * result + (showDocumentSettingsOption ? 1 : 0);
        result = 31 * result + (showAnnotationToolbarOption ? 1 : 0);
        result = 31 * result + (showOpenFileOption ? 1 : 0);
        result = 31 * result + (showOpenUrlOption ? 1 : 0);
        result = 31 * result + (showEditPagesOption ? 1 : 0);
        result = 31 * result + (showPrintOption ? 1 : 0);
        result = 31 * result + (showCloseTabOption ? 1 : 0);
        result = 31 * result + (showAnnotationsList ? 1 : 0);
        result = 31 * result + (showAnnotationReplyReviewState ? 1 : 0);
        result = 31 * result + (showOutlineList ? 1 : 0);
        result = 31 * result + (showUserBookmarksList ? 1 : 0);
        result = 31 * result + (rightToLeftModeEnabled ? 1 : 0);
        result = 31 * result + (showRightToLeftOption ? 1 : 0);
        result = 31 * result + (pdfViewCtrlConfig != null ? pdfViewCtrlConfig.hashCode() : 0);
        result = 31 * result + toolManagerBuilderStyleRes;
        result = 31 * result + (toolManagerBuilder != null ? toolManagerBuilder.hashCode() : 0);
        result = 31 * result + (conversionOptions != null ? conversionOptions.hashCode() : 0);
        result = 31 * result + (openUrlCachePath != null ? openUrlCachePath.hashCode() : 0);
        result = 31 * result + (saveCopyExportPath != null ? saveCopyExportPath.hashCode() : 0);
        result = 31 * result + (useSupportActionBar ? 1 : 0);
        result = 31 * result + (showSaveCopyOption ? 1 : 0);
        result = 31 * result + (showCropOption ? 1 : 0);
        result = 31 * result + (restrictDownloadUsage ? 1 : 0);
        result = 31 * result + layoutInDisplayCutoutMode;
        result = 31 * result + (thumbnailViewEditingEnabled ? 1 : 0);
        result = 31 * result + (userBookmarksListEditingEnabled ? 1 : 0);
        result = 31 * result + userBookmarksListEditingMode;
        result = 31 * result + (outlineListEditingEnabled ? 1 : 0);
        result = 31 * result + (annotationsListEditingEnabled ? 1 : 0);
        result = 31 * result + maximumTabCount;
        result = 31 * result + (enableAutoHideToolbar ? 1 : 0);
        result = 31 * result + (showFormToolbarOption ? 1 : 0);
        result = 31 * result + (showFillAndSignToolbarOption ? 1 : 0);
        result = 31 * result + (navigationListAsSheetOnLargeDevice ? 1 : 0);
        result = 31 * result + (showViewLayersOption ? 1 : 0);
        result = 31 * result + (showAppBar ? 1 : 0);
        result = 31 * result + (showTopToolbar ? 1 : 0);
        result = 31 * result + (showBottomToolbar ? 1 : 0);
        result = 31 * result + (permanentToolbars ? 1 : 0);
        result = 31 * result + (useStandardLibrary ? 1 : 0);
        result = 31 * result + (showReflowOption ? 1 : 0);
        result = 31 * result + reflowOrientation;
        result = 31 * result + toolbarItemGravity;
        result = 31 * result + (showEditMenuOption ? 1 : 0);
        result = 31 * result + (pageStackEnabled ? 1 : 0);
        result = 31 * result + Arrays.hashCode(hideViewModeIds);
        result = 31 * result + Arrays.hashCode(excludedAnnotationListTypes);
        result = 31 * result + Arrays.hashCode(hideThumbnailFilterModes);
        result = 31 * result + Arrays.hashCode(hideThumbnailEditOptions);
        result = 31 * result + Arrays.hashCode(hideSaveCopyOptions);
        result = 31 * result + toolbarBuilders.hashCode();
        result = 31 * result + (showToolbarSwitcher ? 1 : 0);
        result = 31 * result + Arrays.hashCode(toolbarsToHide);
        result = 31 * result + (initialToolbarTag != null ? initialToolbarTag.hashCode() : 0);
        result = 31 * result + (rememberLastToolbar ? 1 : 0);
        result = 31 * result + (rememberLastUsedTool ? 1 : 0);
        result = 31 * result + (conversionCachePath != null ? conversionCachePath.hashCode() : 0);
        result = 31 * result + (bottomBarBuilder != null ? bottomBarBuilder.hashCode() : 0);
        result = 31 * result + (skipReadOnlyCheck ? 1 : 0);
        result = 31 * result + (showConversionDialog ? 1 : 0);
        result = 31 * result + (showFileAttachmentOption ? 1 : 0);
        result = 31 * result + (imageInReflowEnabled ? 1 : 0);
        result = 31 * result + (userBookmarkCreationEnabled ? 1 : 0);
        result = 31 * result + (useCompactViewer ? 1 : 0);
        result = 31 * result + (showDigitalSignaturesOption ? 1 : 0);
        result = 31 * result + (autoSortUserBookmarks ? 1 : 0);
        result = 31 * result + (saveToolbarItemOrder ? 1 : 0);
        result = 31 * result + (tabletLayoutEnabled ? 1 : 0);
        result = 31 * result + (openSavedCopyInNewTab ? 1 : 0);
        result = 31 * result + (openUrlPasswordCheckEnabled ? 1 : 0);
        result = 31 * result + (hidePresetBar ? 1 : 0);
        result = 31 * result + Arrays.hashCode(presetBarsToHideIds);
        result = 31 * result + (showDownloadDialog ? 1 : 0);
        result = 31 * result + (annotationListFilterEnabled ? 1 : 0);
        result = 31 * result + Arrays.hashCode(topToolbarMenuIds);
        result = 31 * result + (quickBookmarkCreation ? 1 : 0);
        result = 31 * result + (annotationPositionSnappingEnabled ? 1 : 0);
        result = 31 * result + (alwaysShowAsReply ? 1 : 0);
        return result;
    }
}
