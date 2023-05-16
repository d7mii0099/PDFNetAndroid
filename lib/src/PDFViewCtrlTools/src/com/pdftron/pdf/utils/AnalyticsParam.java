//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.content.ContentResolver;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.tabs.TabLayout;
import com.pdftron.pdf.controls.AnnotStyleDialogFragment;
import com.pdftron.pdf.controls.BookmarksTabLayout;
import com.pdftron.pdf.dialog.toolbarswitcher.model.ToolbarSwitcherItem;
import com.pdftron.pdf.model.CustomStampOption;
import com.pdftron.pdf.model.ExternalFileInfo;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarButtonType;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * A utility class for creating parameters used in Analytics handler
 */
public class AnalyticsParam {

    public static HashMap<String, String> annotationFilterParam(String state) {
        HashMap<String, String> result = new HashMap<>();
        result.put("state", state);
        return result;
    }

    public static HashMap<String, String> editOutlineParam(String eventType) {
        HashMap<String, String> result = new HashMap<>();
        result.put("eventType", eventType);
        return result;
    }

    public static HashMap<String, String> viewerSaveCopyParam(int typeId) {
        HashMap<String, String> result = new HashMap<>();
        result.put("type", AnalyticsHandlerAdapter.getInstance().getViewerSaveCopy(typeId));
        return result;
    }

    public static HashMap<String, String> lowMemoryParam(@NonNull String location) {
        HashMap<String, String> result = new HashMap<>();
        result.put("location", location);
        result.put("device", Utils.getDeviceName());
        return result;
    }

    public static HashMap<String, String> passFailParam(boolean passed) {
        HashMap<String, String> result = new HashMap<>();
        result.put("success", passed ? "pass" : "fail");
        return result;
    }

    public static HashMap<String, String> noActionParam(boolean hasEventAction) {
        HashMap<String, String> result = new HashMap<>();
        result.put("noaction", hasEventAction ? "false" : "true");
        return result;
    }

    public static HashMap<String, String> createNewParam(int itemId, int originId) {
        return createNewParam(itemId, AnalyticsHandlerAdapter.getInstance().getScreen(originId));
    }

    private static HashMap<String, String> createNewParam(int itemId, @NonNull String origin) {
        HashMap<String, String> result = new HashMap<>();
        result.put("item", AnalyticsHandlerAdapter.getInstance().getCreateNewItem(itemId));
        result.put("origin", origin);
        return result;
    }

    public static HashMap<String, String> openFileParam(@NonNull ExternalFileInfo externalFileInfo,
                                                        @NonNull ContentResolver contentResolver,
                                                        int screenId) {
        return openFileParam(
            Utils.getUriExtension(contentResolver, externalFileInfo.getUri()),
            AnalyticsHandlerAdapter.OPEN_FILE_IN_APP,
            AnalyticsHandlerAdapter.getInstance().getScreen(screenId));
    }

    public static HashMap<String, String> openFileParam(@NonNull FileInfo fileInfo,
                                                        int screenId) {
        return openFileParam(
            fileInfo.getExtension(),
            AnalyticsHandlerAdapter.OPEN_FILE_IN_APP,
            AnalyticsHandlerAdapter.getInstance().getScreen(screenId));
    }

    public static HashMap<String, String> openFileParam(@Nullable String extension,
                                                        int screenId) {
        return openFileParam(
            extension,
            AnalyticsHandlerAdapter.OPEN_FILE_IN_APP,
            AnalyticsHandlerAdapter.getInstance().getScreen(screenId));
    }

    public static HashMap<String, String> openFileParam(@Nullable String extension,
                                                        @Nullable String uri) {
        return openFileParam(
            extension,
            AnalyticsHandlerAdapter.OPEN_FILE_3RD_PARTY,
            uri);
    }

    public static HashMap<String, String> openFileParam(@Nullable String extension, int originId, @Nullable String detail) {
        HashMap<String, String> result = new HashMap<>();
        if (Utils.isNullOrEmpty(extension)) {
            extension = "not_known";
        }
        if (Utils.isNullOrEmpty(detail)) {
            detail = "not_known";
        }
        result.put("format", extension.toLowerCase());
        result.put("origin", AnalyticsHandlerAdapter.getInstance().getOpenFileOrigin(originId));
        result.put("detail", detail);
        return result;
    }

    public static HashMap<String, String> navItemParam(int screenId, boolean fromSideNav) {
        HashMap<String, String> result = new HashMap<>();
        result.put("screen", AnalyticsHandlerAdapter.getInstance().getScreen(screenId));
        result.put("origin", fromSideNav ? "side_navigation" : "bottom_navigation");
        return result;
    }

    public static HashMap<String, String> mergeParam(int screenId, int numNonPdf, int numPdf) {
        HashMap<String, String> result = new HashMap<>();
        result.put("screen", AnalyticsHandlerAdapter.getInstance().getScreen(screenId));
        result.put("num_non_pdf", String.valueOf(numNonPdf));
        result.put("num_pdf", String.valueOf(numPdf));
        return result;
    }

    public static HashMap<String, String> viewerUndoRedoParam(@Nullable String action, int locationId) {
        HashMap<String, String> result = new HashMap<>();
        if (Utils.isNullOrEmpty(action)) {
            action = "not_known";
        }
        result.put("action", action);
        result.put("location", AnalyticsHandlerAdapter.getInstance().getLocation(locationId));
        return result;
    }

    public static HashMap<String, String> viewerUndoRedoParam(@Nullable String action, int locationId, int undoCount, int redoCount) {
        HashMap<String, String> result = viewerUndoRedoParam(action, locationId);
        result.put("undo_count", String.valueOf(undoCount));
        result.put("redo_count", String.valueOf(redoCount));
        return result;
    }

    public static HashMap<String, String> annotationToolbarParam(int selectId) {
        HashMap<String, String> result = new HashMap<>();
        result.put("select", AnalyticsHandlerAdapter.getInstance().getAnnotationTool(selectId));
        return result;
    }

    public static HashMap<String, String> annotationToolbarParam(@NonNull ToolbarItem type) {
        HashMap<String, String> result = new HashMap<>();
        result.put("select", type.toolbarButtonType.name());
        return result;
    }

    public static HashMap<String, String> toolToggleClose(boolean isToggle) {
        HashMap<String, String> result = new HashMap<>();
        result.put("toggle_close", isToggle ? "toggle" : "preset_close");
        return result;
    }

    public static HashMap<String, String> annotationToolbarSwitcherParam(@NonNull ToolbarSwitcherItem item) {
        HashMap<String, String> result = new HashMap<>();
        result.put("select", item.getTag());
        return result;
    }

    public static HashMap<String, String> annotationToolbarModifiedParam(@NonNull ToolbarSwitcherItem item, boolean hasReordered) {
        HashMap<String, String> result = new HashMap<>();
        result.put(item.getTag(), hasReordered ? "reordered" : "no_change");
        if (hasReordered) {
            result.put("counts", item.getTag());
        }
        return result;
    }

    public static List<Map<String, String>> favToolbarToolCountParams(@NonNull ToolbarSwitcherItem item) {
        List<Map<String, String>> results = new ArrayList<>();

        HashSet<ToolbarButtonType> toolTypes = new HashSet<>();
        List<ToolbarItem> toolbarItems = item.builder.getToolbarItems();
        for (ToolbarItem toolbarItem: toolbarItems) {
            ToolbarButtonType toolbarButtonType = toolbarItem.toolbarButtonType;
            if (!toolTypes.contains(toolbarButtonType)) {
                HashMap<String, String> result = new HashMap<>();
                result.put("fav_toolbar", toolbarButtonType.name());
                toolTypes.add(toolbarButtonType);
                results.add(result);
            }
        }
        return results;
    }

    public static List<Map<String, String>> favToolbarDuplicateToolCountParams(@NonNull ToolbarSwitcherItem item) {
        List<Map<String, String>> results = new ArrayList<>();

        HashMap<ToolbarButtonType, Integer> toolCounts = new HashMap<>();
        List<ToolbarItem> toolbarItems = item.builder.getToolbarItems();
        for (ToolbarItem toolbarItem: toolbarItems) {
            ToolbarButtonType toolbarButtonType = toolbarItem.toolbarButtonType;
            Integer count = toolCounts.get(toolbarButtonType);
            if (count != null) {
                toolCounts.put(toolbarButtonType, count + 1);
            } else {
                toolCounts.put(toolbarButtonType, 1);
            }
        }

        // Add tools that have been added multiple times
        for (Map.Entry<ToolbarButtonType, Integer> entry : toolCounts.entrySet()) {
            ToolbarButtonType type = entry.getKey();
            Integer count = entry.getValue();
            if (count > 1) {
                HashMap<String, String> result = new HashMap<>();
                result.put("fav_toolbar", type.name());
                results.add(result);
            }
        }

        return results;
    }

    public static List<Map<String, String>> annotationToolbarItemParams(@NonNull ToolbarSwitcherItem item) {
        List<Map<String, String>> results = new ArrayList<>();
        List<ToolbarItem> toolbarItems = item.builder.getToolbarItems();
        for (int i = 0; i < toolbarItems.size() || i < 2; i++) {
            ToolbarItem toolbarItem = toolbarItems.get(i);
            if (toolbarItem.order == 0 || toolbarItem.order == 1 || toolbarItem.order == 2) {
                HashMap<String, String> result = new HashMap<>();
                result.put(item.getTag(), toolbarItem.toolbarButtonType.name());
                results.add(result);
            }
        }
        return results;
    }

    public static HashMap<String, String> editToolbarParam(int selectId) {
        HashMap<String, String> result = new HashMap<>();
        result.put("select", AnalyticsHandlerAdapter.getInstance().getEditToolbarTool(selectId));
        return result;
    }

    public static HashMap<String, String> viewModeParam(int selectId) {
        HashMap<String, String> result = new HashMap<>();
        result.put("select", AnalyticsHandlerAdapter.getInstance().getViewMode(selectId));
        return result;
    }

    public static HashMap<String, String> viewModeParam(int selectId, boolean isCurrentSameMode) {
        HashMap<String, String> result = viewModeParam(selectId);
        result.put("current", String.valueOf(isCurrentSameMode));
        return result;
    }

    public static HashMap<String, String> thumbnailsViewParam(int actionId) {
        HashMap<String, String> result = new HashMap<>();
        result.put("action", AnalyticsHandlerAdapter.getInstance().getThumbnailsView(actionId));
        return result;
    }

    public static HashMap<String, String> thumbnailsViewCountParam(int actionId, int pageCount) {
        HashMap<String, String> result = thumbnailsViewParam(actionId);
        result.put("count", String.valueOf(pageCount));
        return result;
    }

    public static HashMap<String, String> navigationListsTabParam(int tabId) {
        HashMap<String, String> result = new HashMap<>();
        result.put("tab", AnalyticsHandlerAdapter.getInstance().getNavigationListsTab(tabId));
        return result;
    }

    public static HashMap<String, String> userBookmarksActionParam(int actionId) {
        HashMap<String, String> result = new HashMap<>();
        result.put("action", AnalyticsHandlerAdapter.getInstance().getUerBookmarksAction(actionId));
        return result;
    }

    public static HashMap<String, String> annotationsListActionParam(int actionId) {
        HashMap<String, String> result = new HashMap<>();
        result.put("action", AnalyticsHandlerAdapter.getInstance().getAnnotationsListAction(actionId));
        return result;
    }

    public static HashMap<String, String> viewerNavigateByParam(int itemId) {
        HashMap<String, String> result = new HashMap<>();
        result.put("by", AnalyticsHandlerAdapter.getInstance().getViewerNavigateBy(itemId));
        return result;
    }

    public static HashMap<String, String> shortcutParam(int itemId) {
        HashMap<String, String> result = new HashMap<>();
        result.put("action", AnalyticsHandlerAdapter.getInstance().getShortcut(itemId));
        return result;
    }

    public static HashMap<String, String> quickMenuOpenParam(@AnalyticsHandlerAdapter.QuickMenuType int typeId) {
        HashMap<String, String> result = new HashMap<>();
        result.put("type", AnalyticsHandlerAdapter.getInstance().getQuickMenuType(typeId));
        return result;
    }

    public static HashMap<String, String> quickMenuDismissParam(@AnalyticsHandlerAdapter.QuickMenuType int typeId, boolean hasAction) {
        HashMap<String, String> result = noActionParam(hasAction);
        result.putAll(quickMenuOpenParam(typeId));
        return result;
    }

    public static HashMap<String, String> quickMenuParam(
        @AnalyticsHandlerAdapter.QuickMenuType int typeId,
        @NonNull String action
    ) {

        return quickMenuParam(typeId, action, null);

    }

    public static HashMap<String, String> quickMenuParam(
        @AnalyticsHandlerAdapter.QuickMenuType int typeId,
        @NonNull String action,
        @Nullable String nextAction
    ) {

        HashMap<String, String> result = quickMenuOpenParam(typeId);
        result.put("action", action);
        if (nextAction != null) {
            result.put("next_action", nextAction);
        }
        return result;

    }

    public static HashMap<String, String> stylePickerSelectColorParam(String colorStr,
                                                                      @AnalyticsHandlerAdapter.StylePickerLocation int picker,
                                                                      @AnnotStyleDialogFragment.SelectColorMode int type,
                                                                      @AnalyticsHandlerAdapter.StylePickerOpenedLocation int location,
                                                                      String annotation,
                                                                      int presetIndex) {
        HashMap<String, String> result = stylePickerBasicParam(location, annotation);
        result.put("color", colorStr);
        result.put("picker", AnalyticsHandlerAdapter.getInstance().getStylePickerLocation(picker));
        result.put("type", AnalyticsHandlerAdapter.getInstance().getColorPickerType(type));
        result.put("preset", presetIndex > -1 ? String.valueOf(presetIndex + 1) : "none");
        return result;
    }

    public static HashMap<String, String> stylePickerSelectThicknessParam(float thickness,
                                                                          @AnalyticsHandlerAdapter.StylePickerOpenedLocation int location,
                                                                          String annotation,
                                                                          int presetIndex) {
        HashMap<String, String> result = stylePickerBasicParam(location, annotation);
        result.put("thickness", String.valueOf(thickness));
        result.put("preset", presetIndex > -1 ? String.valueOf(presetIndex + 1) : "none");
        return result;
    }

    public static HashMap<String, String> stylePickerSelectOpacityParam(float opacity,
                                                                        @AnalyticsHandlerAdapter.StylePickerOpenedLocation int location,
                                                                        String annotation,
                                                                        int presetIndex) {
        HashMap<String, String> result = stylePickerBasicParam(location, annotation);
        result.put("opacity", String.valueOf(opacity * 100));
        result.put("preset", presetIndex > -1 ? String.valueOf(presetIndex + 1) : "none");
        return result;
    }

    public static HashMap<String, String> stylePickerSelectTextSizeParam(float textSize,
                                                                         @AnalyticsHandlerAdapter.StylePickerOpenedLocation int location,
                                                                         String annotation,
                                                                         int presetIndex) {
        HashMap<String, String> result = stylePickerBasicParam(location, annotation);
        result.put("textSize", String.valueOf(textSize));
        result.put("preset", presetIndex > -1 ? String.valueOf(presetIndex + 1) : "none");
        return result;
    }

    public static HashMap<String, String> stylePickerSelectRulerBaseParam(float base,
                                                                          @AnalyticsHandlerAdapter.StylePickerOpenedLocation int location,
                                                                          String annotation,
                                                                          int presetIndex) {
        HashMap<String, String> result = stylePickerBasicParam(location, annotation);
        result.put("rulerBase", String.valueOf(base));
        result.put("preset", presetIndex > -1 ? String.valueOf(presetIndex + 1) : "none");
        return result;
    }

    public static HashMap<String, String> stylePickerSelectRulerTranslateParam(float translate,
                                                                               @AnalyticsHandlerAdapter.StylePickerOpenedLocation int location,
                                                                               String annotation,
                                                                               int presetIndex) {
        HashMap<String, String> result = stylePickerBasicParam(location, annotation);
        result.put("rulerTranslate", String.valueOf(translate));
        result.put("preset", presetIndex > -1 ? String.valueOf(presetIndex + 1) : "none");
        return result;
    }

    public static HashMap<String, String> stylePickerSelectPresetParam(@AnalyticsHandlerAdapter.StylePickerOpenedLocation int location,
                                                                       String annotation,
                                                                       int presetIndex,
                                                                       boolean defaultPreset) {
        HashMap<String, String> result = stylePickerBasicParam(location, annotation);
        result.put("preset", presetIndex > -1 ? String.valueOf(presetIndex + 1) : "none");
        result.put("default", String.valueOf(defaultPreset));
        return result;
    }

    public static HashMap<String, String> stylePickerDeselectPresetParam(@AnalyticsHandlerAdapter.StylePickerOpenedLocation int location,
                                                                         String annotation,
                                                                         int presetIndex) {
        HashMap<String, String> result = stylePickerBasicParam(location, annotation);
        result.put("preset", presetIndex > -1 ? String.valueOf(presetIndex + 1) : "none");
        return result;
    }

    public static HashMap<String, String> stylePickerBasicParam(@AnalyticsHandlerAdapter.StylePickerOpenedLocation int location, String annotation) {
        HashMap<String, String> result = new HashMap<>();
        result.put("location", AnalyticsHandlerAdapter.getInstance().getStylePickerOpenedFromLocation(location));
        result.put("annotation", annotation);
        return result;
    }

    public static HashMap<String, String> stylePickerSelectRichTextParam(boolean enabled, @AnalyticsHandlerAdapter.StylePickerOpenedLocation int location,
            String annotation) {
        HashMap<String, String> result = stylePickerBasicParam(location, annotation);
        result.put("option", enabled ? "rich_content_on" : "rich_content_off");
        return result;
    }

    public static HashMap<String, String> stylePickerSelectEraserParam(boolean enabled, @AnalyticsHandlerAdapter.StylePickerOpenedLocation int location,
                                                                       String annotation) {
        HashMap<String, String> result = stylePickerBasicParam(location, annotation);
        result.put("option", enabled ? "erase_ink_only_on" : "erase_ink_only_off");
        return result;
    }

    public static HashMap<String, String> stylePickerSelectPressureParam(boolean enabled, @AnalyticsHandlerAdapter.StylePickerOpenedLocation int location,
                                                                       String annotation) {
        HashMap<String, String> result = stylePickerBasicParam(location, annotation);
        result.put("option", enabled ? "pressure_on" : "pressure_off");
        return result;
    }

    public static HashMap<String, String> stylePickerCloseParam(boolean hasAction, @AnalyticsHandlerAdapter.StylePickerOpenedLocation int location,
                                                                String annotation) {
        HashMap<String, String> result = noActionParam(hasAction);
        result.putAll(stylePickerBasicParam(location, annotation));
        return result;
    }

    public static HashMap<String, String> colorParam(String colorStr) {
        HashMap<String, String> result = new HashMap<>();
        result.put("color", colorStr);
        return result;
    }

    public static HashMap<String, String> hugeThumbParam(int width, int height, int bufferLength, int location) {
        HashMap<String, String> result = new HashMap<>();
        result.put("width", String.valueOf(width));
        result.put("height", String.valueOf(height));
        result.put("buffer_length", String.valueOf(bufferLength));
        result.put("location", AnalyticsHandlerAdapter.getInstance().getLocation(location));
        return result;
    }

    public static HashMap<String, String> firstTimeParam(boolean rtl) {
        HashMap<String, String> result = new HashMap<>();
        result.put("rtl_mode", String.valueOf(rtl));
        return result;
    }

    public static HashMap<String, String> navigateListCloseParam(TabLayout.Tab tab, boolean hasAction) {
        HashMap<String, String> result = navigationListsTabParam(BookmarksTabLayout.getNavigationId(tab));
        result.putAll(noActionParam(hasAction));
        return result;
    }

    private static HashMap<String, String> actionParam(String action) {
        HashMap<String, String> result = new HashMap<>();
        result.put("action", action);
        return result;
    }

    public static HashMap<String, String> cropPageParam(@AnalyticsHandlerAdapter.CropPageAction int action) {
        return actionParam(AnalyticsHandlerAdapter.getInstance().getCropPageAction(action));
    }

    public static HashMap<String, String> cropPageParam(@AnalyticsHandlerAdapter.CropPageAction int action, @AnalyticsHandlerAdapter.CropPageDetails int details) {
        HashMap<String, String> result = cropPageParam(action);
        result.put("details", AnalyticsHandlerAdapter.getInstance().getCropPageDetails(details));
        return result;
    }

    public static HashMap<String, String> customColorParam(int action) {
        return actionParam(AnalyticsHandlerAdapter.getInstance().getCustomColorModeAction(action));
    }

    public static HashMap<String, String> customColorParam(int action, int position, boolean isDefault, int bgColor, int textColor) {
        HashMap<String, String> result = customColorParam(action);
        result.put("position", String.valueOf(position + 1));
        result.put("default", String.valueOf(isDefault));
        result.put("colors", Utils.getColorHexString(bgColor) + " " + Utils.getColorHexString(textColor));
        return result;
    }

    public static HashMap<String, String> customColorParam(int action, int position, boolean isDefault, int bgColor, int textColor, boolean applySelection) {
        HashMap<String, String> result = customColorParam(action, position, isDefault, bgColor, textColor);
        result.put("apply_selection", String.valueOf(applySelection));
        return result;
    }

    public static HashMap<String, String> showAllToolsParam(boolean showAllTools) {
        HashMap<String, String> result = new HashMap<>();
        result.put("show_all", String.valueOf(showAllTools));
        return result;
    }

    public static HashMap<String, String> addRubberStampParam(int stampType, @NonNull String stampName) {
        HashMap<String, String> result = new HashMap<>();
        result.put("type", AnalyticsHandlerAdapter.getInstance().getRubberStampType(stampType));
        result.put("details", stampName);
        return result;
    }

    public static HashMap<String, String> addCustomStampParam(int stampType, @NonNull CustomStampOption stamp, @Nullable String colorName) {
        if (Utils.isNullOrEmpty(colorName)) {
            colorName = "not_known";
        }

        String shape;
        if (stamp.isPointingLeft) {
            shape = "left";
        } else if (stamp.isPointingRight) {
            shape = "right";
        } else {
            shape = "rounded";
        }

        HashMap<String, String> result = new HashMap<>();
        result.put("type", AnalyticsHandlerAdapter.getInstance().getRubberStampType(stampType));
        result.put("details", stamp.text);
        result.put("date", String.valueOf(stamp.hasDateStamp()));
        result.put("time", String.valueOf(stamp.hasTimeStamp()));
        result.put("shape", shape);
        result.put("color", colorName);

        return result;
    }

    public static HashMap<String, String> rageScrollingParam(@NonNull String action, boolean neverShowAgain) {
        HashMap<String, String> result = new HashMap<>();
        result.put("action", action);
        result.put("never_show_again", String.valueOf(neverShowAgain));
        return result;
    }

    public static HashMap<String, String> autoDrawParam(@NonNull String action, @NonNull String svg) {
        HashMap<String, String> result = new HashMap<>();
        result.put("action", action);
        result.put("svg", svg);
        return result;
    }

}
