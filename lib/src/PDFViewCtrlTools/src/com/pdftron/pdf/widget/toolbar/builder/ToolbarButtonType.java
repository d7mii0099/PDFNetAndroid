package com.pdftron.pdf.widget.toolbar.builder;

import android.util.SparseArray;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntRange;
import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.R;

/**
 * Default toolbar items available in the annotation Toolbars. Only button types defined here and
 * in {@link com.pdftron.pdf.widget.toolbar.component.DefaultToolbars} will be customizable in the Favorite Toolbar.
 *
 * To add a new button mode:
 * <p>
 * 1. Create a new enum.
 * 2. Create a unique toolbar item id. This id is used to create a key for storing annotation style
 *      presets, and used as a unique identifier for the enum.
 * 3. Specify toolbar item title.
 * 4. Specify toolbar item icon.
 * 5. If it's a Tool, add to ToolModeMapper.
 * 6. Then implement functionality to handle the button in AnnotationToolbarComponent or in PdfViewCtrlTabHostFragment2
 * <p>
 * IMPORTANT: NAMES OF THESE ENUMS MUST NOT BE CHANGED
 */
@Keep
public enum ToolbarButtonType {

    /**
     * IMPORTANT: NAMES OF THESE ENUMS MUST NOT BE CHANGED
     */
    STICKY_NOTE(Annot.e_Text, R.string.controls_annotation_toolbar_tool_description_stickynote, R.drawable.ic_annotation_sticky_note_black_24dp),
    SOUND(Annot.e_Sound, R.string.controls_annotation_toolbar_tool_description_sound, R.drawable.ic_mic_black_24dp),
    TEXT_HIGHLIGHT(Annot.e_Highlight, R.string.controls_annotation_toolbar_tool_description_text_highlight, R.drawable.ic_annotation_highlight_black_24dp),
    TEXT_UNDERLINE(Annot.e_Underline, R.string.controls_annotation_toolbar_tool_description_text_underline, R.drawable.ic_annotation_underline_black_24dp),
    SIGNATURE(AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE, R.string.controls_annotation_toolbar_tool_description_signature, R.drawable.ic_annotation_signature_black_24dp),
    INK(Annot.e_Ink, R.string.controls_annotation_toolbar_tool_description_freehand, R.drawable.ic_annotation_freehand_black_24dp),
    FREE_TEXT(Annot.e_FreeText, R.string.controls_annotation_toolbar_tool_description_freetext, R.drawable.ic_annotation_freetext_black_24dp),
    CALLOUT(AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT, R.string.controls_annotation_toolbar_tool_description_callout, R.drawable.ic_annotation_callout_black_24dp),
    FREE_HIGHLIGHT(AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_HIGHLIGHTER, R.string.controls_annotation_toolbar_tool_description_free_highlighter, R.drawable.ic_annotation_free_highlight_black_24dp),
    TEXT_STRIKEOUT(Annot.e_StrikeOut, R.string.controls_annotation_toolbar_tool_description_text_strikeout, R.drawable.ic_annotation_strikeout_black_24dp),
    TEXT_SQUIGGLY(Annot.e_Squiggly, R.string.controls_annotation_toolbar_tool_description_text_squiggly, R.drawable.ic_annotation_squiggly_black_24dp),
    ERASER(AnnotStyle.CUSTOM_ANNOT_TYPE_ERASER, R.string.controls_annotation_toolbar_tool_description_eraser, R.drawable.ic_annotation_eraser_black_24dp),
    LINE(Annot.e_Line, R.string.controls_annotation_toolbar_tool_description_line, R.drawable.ic_annotation_line_black_24dp),
    ARROW(AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW, R.string.controls_annotation_toolbar_tool_description_arrow, R.drawable.ic_annotation_arrow_black_24dp),
    POLYLINE(Annot.e_Polyline, R.string.controls_annotation_toolbar_tool_description_polyline, R.drawable.ic_annotation_polyline_black_24dp),
    RULER(AnnotStyle.CUSTOM_ANNOT_TYPE_RULER, R.string.controls_annotation_toolbar_tool_description_ruler, R.drawable.ic_annotation_distance_black_24dp),
    PERIMETER(AnnotStyle.CUSTOM_ANNOT_TYPE_PERIMETER_MEASURE, R.string.controls_annotation_toolbar_tool_description_perimeter, R.drawable.ic_annotation_perimeter_black_24dp),
    SQUARE(Annot.e_Square, R.string.controls_annotation_toolbar_tool_description_rectangle, R.drawable.ic_annotation_square_black_24dp),
    CIRCLE(Annot.e_Circle, R.string.controls_annotation_toolbar_tool_description_oval, R.drawable.ic_annotation_circle_black_24dp),
    POLYGON(Annot.e_Polygon, R.string.controls_annotation_toolbar_tool_description_polygon, R.drawable.ic_annotation_polygon_black_24dp),
    POLY_CLOUD(AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD, R.string.controls_annotation_toolbar_tool_description_cloud, R.drawable.ic_annotation_cloud_black_24dp),
    AREA(AnnotStyle.CUSTOM_ANNOT_TYPE_AREA_MEASURE, R.string.controls_annotation_toolbar_tool_description_area, R.drawable.ic_annotation_poly_area_24dp),
    RECT_AREA(AnnotStyle.CUSTOM_ANNOT_TYPE_RECT_AREA_MEASURE, R.string.controls_annotation_toolbar_tool_description_rect_area, R.drawable.ic_annotation_area_black_24dp),
    DATE(AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_DATE, R.string.controls_fill_and_sign_toolbar_btn_description_date, R.drawable.ic_date_range_24px),
    FREE_TEXT_SPACING(AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_SPACING, R.string.annot_free_text, R.drawable.ic_fill_and_sign_spacing_text),
    RADIO_BUTTON(AnnotStyle.CUSTOM_ANNOT_TYPE_RADIO_BUTTON, R.string.tools_qm_form_radio_group, R.drawable.ic_radio_button_checked_black_24dp),
    LIST_BOX(AnnotStyle.CUSTOM_ANNOT_TYPE_LIST_BOX, R.string.tools_qm_form_list_box, R.drawable.ic_annotation_listbox_black),
    TEXT_FIELD(AnnotStyle.CUSTOM_ANNOT_TYPE_TEXT_FIELD, R.string.tools_qm_form_text, R.drawable.ic_text_fields_black_24dp),
    LINK(Annot.e_Link, R.string.tools_qm_link, R.drawable.ic_link_black_24dp),
    ATTACHMENT(Annot.e_FileAttachment, R.string.tools_qm_attach_file, R.drawable.ic_attach_file_black_24dp),
    TEXT_REDACTION(Annot.e_Redact, R.string.tools_qm_redact_by_text, R.drawable.ic_annotation_redact_text),
    RECT_REDACTION(AnnotStyle.CUSTOM_RECT_REDACTION, R.string.tools_qm_redact_by_area, R.drawable.ic_annotation_redact_area),
    PAGE_REDACTION(AnnotStyle.CUSTOM_PAGE_REDACTION, R.string.tools_qm_redact_by_page, R.drawable.ic_annotation_redact_page, false),
    SEARCH_REDACTION(AnnotStyle.CUSTOM_SEARCH_REDACTION, R.string.tools_qm_redact_by_search, R.drawable.ic_annotation_redact_search, false),

    // New toolbar item ids start at 2000 onward
    PAN(AnnotStyle.CUSTOM_ANNOT_TYPE_PAN, R.string.controls_annotation_toolbar_tool_description_pan, R.drawable.ic_pan_black_24dp),
    MULTI_SELECT(AnnotStyle.CUSTOM_ANNOT_TYPE_RECT_MULTI_SELECT, R.string.controls_annotation_toolbar_tool_description_multi_select, R.drawable.ic_select_rectangular_black_24dp),
    COUNT_MEASUREMENT(AnnotStyle.CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT, R.string.controls_annotation_toolbar_tool_description_count_tool, R.drawable.ic_measurement_count),
    LASSO_SELECT(AnnotStyle.CUSTOM_ANNOT_TYPE_LASSO_MULTI_SELECT, R.string.controls_annotation_toolbar_tool_description_multi_select, R.drawable.ic_select_lasso),
    IMAGE(AnnotStyle.CUSTOM_ANNOT_TYPE_IMAGE_STAMP, R.string.controls_annotation_toolbar_tool_description_image, R.drawable.ic_annotation_image_black_24dp),
    STAMP(Annot.e_Stamp, R.string.controls_annotation_toolbar_tool_description_stamp, R.drawable.ic_annotation_stamp_black_24dp),
    CHECKMARK(AnnotStyle.CUSTOM_ANNOT_TYPE_CHECKMARK_STAMP, R.string.controls_fill_and_sign_toolbar_btn_description_checkmark, R.drawable.ic_fill_and_sign_checkmark),
    CROSS(AnnotStyle.CUSTOM_ANNOT_TYPE_CROSS_STAMP, R.string.controls_fill_and_sign_toolbar_btn_description_cross, R.drawable.ic_fill_and_sign_crossmark),
    DOT(AnnotStyle.CUSTOM_ANNOT_TYPE_DOT_STAMP, R.string.controls_fill_and_sign_toolbar_btn_description_dot, R.drawable.ic_fill_and_sign_dot),
    CHECKBOX(AnnotStyle.CUSTOM_ANNOT_TYPE_CHECKBOX_FIELD, R.string.tools_qm_form_checkbox, R.drawable.ic_annotation_checkbox_field),
    COMBO_BOX(AnnotStyle.CUSTOM_ANNOT_TYPE_COMBO_BOX, R.string.tools_qm_form_combo_box, R.drawable.ic_annotation_combo_black),
    SIGNATURE_FIELD(AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE_FIELD, R.string.tools_qm_signature, R.drawable.ic_annotation_signature_field),
    UNDO(AnnotStyle.CUSTOM_TOOL_UNDO, R.string.undo, R.drawable.ic_undo_black_24dp, false),
    REDO(AnnotStyle.CUSTOM_TOOL_REDO, R.string.redo, R.drawable.ic_redo_black_24dp, false),
    EDIT_TOOLBAR(AnnotStyle.CUSTOM_EDIT_TOOLBAR, R.string.action_edit_menu, R.drawable.ic_toolbar_customization, false),
    SMART_PEN(AnnotStyle.CUSTOM_SMART_PEN, R.string.annot_smart_pen, R.drawable.ic_smart_pen),
    ADD_PAGE(AnnotStyle.CUSTOM_ADD_PAGE, R.string.dialog_add_page_title, R.drawable.ic_add_blank_page_white, false),

    // Non tool related
    CUSTOM_UNCHECKABLE(3006, R.string.widget_choice_default_item, R.drawable.radio_checked_default, false),
    CUSTOM_CHECKABLE(3007, R.string.widget_choice_default_item, R.drawable.radio_checked_default, true),
    // Navigation
    NAVIGATION(3008, R.string.navigation_description, R.drawable.ic_menu_white_24dp, false),
    ;

    private static SparseArray<ToolbarButtonType> sID_MAP = new SparseArray<>(ToolbarButtonType.values().length);
    static {
        for (ToolbarButtonType buttonType : ToolbarButtonType.values()) {
            // Ensure no ids are equal. Otherwise developer should fix this.
            if (sID_MAP.indexOfKey(buttonType.toolbarButtonTypeId) >= 0) {
                throw new RuntimeException("Should not have same IDs!!");
            }
            sID_MAP.put(buttonType.toolbarButtonTypeId, buttonType);
        }
    }

    @IntRange(from = 0, to = 9999)
    private final int toolbarButtonTypeId;
    public final boolean isCheckable;
    @StringRes
    public final int title;
    @DrawableRes
    public final int icon;

    ToolbarButtonType(int toolbarButtonTypeId, @StringRes int title, @DrawableRes int icon, boolean isCheckable) {
        this.toolbarButtonTypeId = toolbarButtonTypeId;
        this.title = title;
        this.icon = icon;
        this.isCheckable = isCheckable;
    }

    ToolbarButtonType(int toolbarButtonTypeId, @StringRes int title, @DrawableRes int icon) {
        this.toolbarButtonTypeId = toolbarButtonTypeId;
        this.title = title;
        this.icon = icon;
        this.isCheckable = true;
    }

    public int getValue() {
        return toolbarButtonTypeId;
    }

    @Nullable
    public static ToolbarButtonType valueOf(int id) {
        if (sID_MAP.indexOfKey(id) >= 0) {
            return sID_MAP.get(id);
        }
        return null;
    }
}
