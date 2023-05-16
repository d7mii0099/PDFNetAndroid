package com.pdftron.pdf.widget.toolbar.component;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarButtonType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Helper class to map {@link ToolbarButtonType} to {@link com.pdftron.pdf.tools.ToolManager.ToolMode}.
 *
 * @hide
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ToolModeMapper {

    private static final Map<ToolbarButtonType, ToolManager.ToolMode> sToolModeMap = new HashMap<>();
    static {
        sToolModeMap.put(ToolbarButtonType.STICKY_NOTE, ToolManager.ToolMode.TEXT_ANNOT_CREATE);
        sToolModeMap.put(ToolbarButtonType.SOUND, ToolManager.ToolMode.SOUND_CREATE);
        sToolModeMap.put(ToolbarButtonType.TEXT_HIGHLIGHT, ToolManager.ToolMode.TEXT_HIGHLIGHT);
        sToolModeMap.put(ToolbarButtonType.TEXT_UNDERLINE, ToolManager.ToolMode.TEXT_UNDERLINE);
        sToolModeMap.put(ToolbarButtonType.SIGNATURE, ToolManager.ToolMode.SIGNATURE);
        sToolModeMap.put(ToolbarButtonType.INK, ToolManager.ToolMode.INK_CREATE);
        sToolModeMap.put(ToolbarButtonType.FREE_TEXT, ToolManager.ToolMode.TEXT_CREATE);
        sToolModeMap.put(ToolbarButtonType.CALLOUT, ToolManager.ToolMode.CALLOUT_CREATE);
        sToolModeMap.put(ToolbarButtonType.FREE_HIGHLIGHT, ToolManager.ToolMode.FREE_HIGHLIGHTER);
        sToolModeMap.put(ToolbarButtonType.TEXT_STRIKEOUT, ToolManager.ToolMode.TEXT_STRIKEOUT);
        sToolModeMap.put(ToolbarButtonType.TEXT_SQUIGGLY, ToolManager.ToolMode.TEXT_SQUIGGLY);
        sToolModeMap.put(ToolbarButtonType.ERASER, ToolManager.ToolMode.INK_ERASER);
        sToolModeMap.put(ToolbarButtonType.LINE, ToolManager.ToolMode.LINE_CREATE);
        sToolModeMap.put(ToolbarButtonType.ARROW, ToolManager.ToolMode.ARROW_CREATE);
        sToolModeMap.put(ToolbarButtonType.POLYLINE, ToolManager.ToolMode.POLYLINE_CREATE);
        sToolModeMap.put(ToolbarButtonType.RULER, ToolManager.ToolMode.RULER_CREATE);
        sToolModeMap.put(ToolbarButtonType.PERIMETER, ToolManager.ToolMode.PERIMETER_MEASURE_CREATE);
        sToolModeMap.put(ToolbarButtonType.SQUARE, ToolManager.ToolMode.RECT_CREATE);
        sToolModeMap.put(ToolbarButtonType.CIRCLE, ToolManager.ToolMode.OVAL_CREATE);
        sToolModeMap.put(ToolbarButtonType.POLYGON, ToolManager.ToolMode.POLYGON_CREATE);
        sToolModeMap.put(ToolbarButtonType.POLY_CLOUD, ToolManager.ToolMode.CLOUD_CREATE);
        sToolModeMap.put(ToolbarButtonType.AREA, ToolManager.ToolMode.AREA_MEASURE_CREATE);
        sToolModeMap.put(ToolbarButtonType.RECT_AREA, ToolManager.ToolMode.RECT_AREA_MEASURE_CREATE);
        sToolModeMap.put(ToolbarButtonType.PAN, ToolManager.ToolMode.PAN);
        sToolModeMap.put(ToolbarButtonType.MULTI_SELECT, ToolManager.ToolMode.ANNOT_EDIT_RECT_GROUP);
        sToolModeMap.put(ToolbarButtonType.COUNT_MEASUREMENT, ToolManager.ToolMode.COUNT_MEASUREMENT);
        sToolModeMap.put(ToolbarButtonType.LASSO_SELECT, ToolManager.ToolMode.ANNOT_EDIT_RECT_GROUP);
        sToolModeMap.put(ToolbarButtonType.IMAGE, ToolManager.ToolMode.STAMPER);
        sToolModeMap.put(ToolbarButtonType.DOT, ToolManager.ToolMode.RUBBER_STAMPER);
        sToolModeMap.put(ToolbarButtonType.STAMP, ToolManager.ToolMode.RUBBER_STAMPER);
        sToolModeMap.put(ToolbarButtonType.CROSS, ToolManager.ToolMode.RUBBER_STAMPER);
        sToolModeMap.put(ToolbarButtonType.CHECKMARK, ToolManager.ToolMode.RUBBER_STAMPER);
        sToolModeMap.put(ToolbarButtonType.DATE, ToolManager.ToolMode.FREE_TEXT_DATE_CREATE);
        sToolModeMap.put(ToolbarButtonType.FREE_TEXT_SPACING, ToolManager.ToolMode.FREE_TEXT_SPACING_CREATE);
        sToolModeMap.put(ToolbarButtonType.TEXT_FIELD, ToolManager.ToolMode.FORM_TEXT_FIELD_CREATE);
        sToolModeMap.put(ToolbarButtonType.COMBO_BOX, ToolManager.ToolMode.FORM_COMBO_BOX_CREATE);
        sToolModeMap.put(ToolbarButtonType.LIST_BOX, ToolManager.ToolMode.FORM_LIST_BOX_CREATE);
        sToolModeMap.put(ToolbarButtonType.CHECKBOX, ToolManager.ToolMode.FORM_CHECKBOX_CREATE);
        sToolModeMap.put(ToolbarButtonType.RADIO_BUTTON, ToolManager.ToolMode.FORM_RADIO_GROUP_CREATE);
        sToolModeMap.put(ToolbarButtonType.SIGNATURE_FIELD, ToolManager.ToolMode.FORM_SIGNATURE_CREATE);
        sToolModeMap.put(ToolbarButtonType.LINK, ToolManager.ToolMode.RECT_LINK);
        sToolModeMap.put(ToolbarButtonType.ATTACHMENT, ToolManager.ToolMode.FILE_ATTACHMENT_CREATE);
        sToolModeMap.put(ToolbarButtonType.TEXT_REDACTION, ToolManager.ToolMode.TEXT_REDACTION);
        sToolModeMap.put(ToolbarButtonType.RECT_REDACTION, ToolManager.ToolMode.RECT_REDACTION);
        sToolModeMap.put(ToolbarButtonType.SMART_PEN, ToolManager.ToolMode.SMART_PEN_INK);
    }

    public static boolean hasAccentedIcon(@NonNull ToolbarButtonType buttonType) {
        switch (buttonType) {
            case SIGNATURE:
            case PAN:
            case ERASER:
            case CHECKMARK:
            case STAMP:
            case IMAGE:
            case CROSS:
            case DOT:
            case RADIO_BUTTON:
            case CHECKBOX:
            case SIGNATURE_FIELD:
            case MULTI_SELECT:
            case LASSO_SELECT:
            case LINK:
            case SMART_PEN:
                return false;
            default:
                return getToolMode(buttonType) != null;
        }
    }

    public static boolean isTool(@NonNull ToolbarButtonType buttonType) {
        return getToolMode(buttonType) != null;
    }

    @Nullable
    public static ToolManager.ToolMode getToolMode(@NonNull ToolbarButtonType buttonType) {
        return sToolModeMap.get(buttonType);
    }

    @Nullable
    public static ToolbarButtonType getButtonType(ToolManager.ToolModeBase toolMode) {
        Set<ToolbarButtonType> keys = sToolModeMap.keySet();
        for (ToolbarButtonType key: keys) {
            if (sToolModeMap.get(key) == toolMode) {
                return key;
            }
        }
        return null;
    }

    @Nullable
    public static ToolbarButtonType getButtonType(int annotType) {
        for (ToolbarButtonType toolbarButtonType: ToolbarButtonType.values()) {
            if (toolbarButtonType.getValue() == annotType) {
                return toolbarButtonType;
            }
        }
        return null;
    }
}