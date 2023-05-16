package com.pdftron.pdf.widget.toolbar.component;

import androidx.annotation.NonNull;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.widget.toolbar.builder.AnnotationToolbarBuilder;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarButtonType;

/**
 * Resources used by the viewer's default toolbars, which includes toolbar tags and toolbar button ids.
 */
public class DefaultToolbars {
    public static final String TAG_VIEW_TOOLBAR = "PDFTron_View";
    public static final String TAG_ANNOTATE_TOOLBAR = "PDFTron_Annotate";
    public static final String TAG_DRAW_TOOLBAR = "PDFTron_Draw";
    public static final String TAG_INSERT_TOOLBAR = "PDFTron_Insert";
    public static final String TAG_FILL_AND_SIGN_TOOLBAR = "PDFTron_Fill_and_Sign";
    public static final String TAG_PREPARE_FORM_TOOLBAR = "PDFTron_Prepare_Form";
    public static final String TAG_MEASURE_TOOLBAR = "PDFTron_Measure";
    public static final String TAG_PENS_TOOLBAR = "PDFTron_Pens";
    public static final String TAG_REDACTION_TOOLBAR = "PDFTron_Redact";
    public static final String TAG_FAVORITE_TOOLBAR = "PDFTron_Favorite";

    public static AnnotationToolbarBuilder defaultViewToolbar =
            AnnotationToolbarBuilder
                    .withTag(DefaultToolbars.TAG_VIEW_TOOLBAR)
                    .setIcon(R.drawable.ic_view)
                    .setToolbarName(R.string.toolbar_title_view);

    public static AnnotationToolbarBuilder defaultAnnotateToolbar =
            AnnotationToolbarBuilder.withTag(TAG_ANNOTATE_TOOLBAR)
                    .addToolButton(ToolbarButtonType.SMART_PEN, ButtonId.SMART_PEN.value())
                    .addToolButton(ToolbarButtonType.TEXT_HIGHLIGHT, ButtonId.TEXT_HIGHLIGHT.value())
                    .addToolButton(ToolbarButtonType.FREE_HIGHLIGHT, ButtonId.FREE_HIGHLIGHT.value())
                    .addToolButton(ToolbarButtonType.TEXT_UNDERLINE, ButtonId.TEXT_UNDERLINE.value())
                    .addToolButton(ToolbarButtonType.INK, ButtonId.INK.value())
                    .addToolButton(ToolbarButtonType.FREE_TEXT, ButtonId.FREE_TEXT.value())
                    .addToolButton(ToolbarButtonType.TEXT_STRIKEOUT, ButtonId.TEXT_STRIKEOUT.value())
                    .addToolButton(ToolbarButtonType.TEXT_SQUIGGLY, ButtonId.TEXT_SQUIGGLY.value())
                    .addToolButton(ToolbarButtonType.STICKY_NOTE, ButtonId.STICKY_NOTE.value())
                    .addToolButton(ToolbarButtonType.ERASER, ButtonId.ERASER.value())
                    .addToolButton(ToolbarButtonType.CALLOUT, ButtonId.CALLOUT.value())
                    .addToolButton(ToolbarButtonType.MULTI_SELECT, ButtonId.MULTI_SELECT.value())
                    .addToolButton(ToolbarButtonType.LASSO_SELECT, ButtonId.LASSO_SELECT.value())
                    .addToolButton(ToolbarButtonType.EDIT_TOOLBAR, ButtonId.CUSTOMIZE.value(), 999)
                    .addToolStickyOptionButton(ToolbarButtonType.UNDO, ButtonId.UNDO.value())
                    .setIcon(R.drawable.ic_annotation_underline_black_24dp)
                    .setToolbarName(R.string.toolbar_title_annotate);

    public static AnnotationToolbarBuilder defaultAnnotateToolbarCompact =
            defaultAnnotateToolbar.copy()
                    .addLeadingToolStickyButton(ToolbarButtonType.NAVIGATION, ButtonId.NAVIGATION.value());

    public static AnnotationToolbarBuilder defaultDrawToolbar =
            AnnotationToolbarBuilder.withTag(TAG_DRAW_TOOLBAR)
                    .addToolButton(ToolbarButtonType.INK, ButtonId.INK.value())
                    .addToolButton(ToolbarButtonType.ERASER, ButtonId.ERASER.value())
                    .addToolButton(ToolbarButtonType.SQUARE, ButtonId.SQUARE.value())
                    .addToolButton(ToolbarButtonType.CIRCLE, ButtonId.CIRCLE.value())
                    .addToolButton(ToolbarButtonType.POLYGON, ButtonId.POLYGON.value())
                    .addToolButton(ToolbarButtonType.POLY_CLOUD, ButtonId.POLY_CLOUD.value())
                    .addToolButton(ToolbarButtonType.LINE, ButtonId.LINE.value())
                    .addToolButton(ToolbarButtonType.ARROW, ButtonId.ARROW.value())
                    .addToolButton(ToolbarButtonType.POLYLINE, ButtonId.POLYLINE.value())
                    .addToolButton(ToolbarButtonType.MULTI_SELECT, ButtonId.MULTI_SELECT.value())
                    .addToolButton(ToolbarButtonType.LASSO_SELECT, ButtonId.LASSO_SELECT.value())
                    .addToolButton(ToolbarButtonType.EDIT_TOOLBAR, ButtonId.CUSTOMIZE.value(), 999)
                    .addToolStickyOptionButton(ToolbarButtonType.UNDO, ButtonId.UNDO.value())
                    .setIcon(R.drawable.ic_pens_and_shapes)
                    .setToolbarName(R.string.toolbar_title_draw);

    public static AnnotationToolbarBuilder defaultDrawToolbarCompact =
            defaultDrawToolbar.copy()
                    .addLeadingToolStickyButton(ToolbarButtonType.NAVIGATION, ButtonId.NAVIGATION.value());

    public static AnnotationToolbarBuilder defaultInsertToolbar =
            AnnotationToolbarBuilder.withTag(TAG_INSERT_TOOLBAR)
                    .addToolButton(ToolbarButtonType.ADD_PAGE, ButtonId.ADD_PAGE.value())
                    .addToolButton(ToolbarButtonType.IMAGE, ButtonId.IMAGE.value())
                    .addToolButton(ToolbarButtonType.STAMP, ButtonId.STAMP.value())
                    .addToolButton(ToolbarButtonType.SIGNATURE, ButtonId.SIGNATURE.value())
                    .addToolButton(ToolbarButtonType.LINK, ButtonId.LINK.value())
                    .addToolButton(ToolbarButtonType.SOUND, ButtonId.SOUND.value())
                    .addToolButton(ToolbarButtonType.ATTACHMENT, ButtonId.ATTACHMENT.value())
                    .addToolButton(ToolbarButtonType.MULTI_SELECT, ButtonId.MULTI_SELECT.value())
                    .addToolButton(ToolbarButtonType.EDIT_TOOLBAR, ButtonId.CUSTOMIZE.value(), 999)
                    .addToolStickyOptionButton(ToolbarButtonType.UNDO, ButtonId.UNDO.value())
                    .setIcon(R.drawable.ic_add_image_white)
                    .setToolbarName(R.string.toolbar_title_insert);

    public static AnnotationToolbarBuilder defaultInsertToolbarCompact =
            defaultInsertToolbar.copy()
                    .addLeadingToolStickyButton(ToolbarButtonType.NAVIGATION, ButtonId.NAVIGATION.value());

    public static AnnotationToolbarBuilder defaultFillAndSignToolbar =
            AnnotationToolbarBuilder.withTag(TAG_FILL_AND_SIGN_TOOLBAR)
                    .addToolButton(ToolbarButtonType.SIGNATURE, ButtonId.SIGNATURE.value())
                    .addToolButton(ToolbarButtonType.FREE_TEXT, ButtonId.FREE_TEXT.value())
                    .addToolButton(ToolbarButtonType.FREE_TEXT_SPACING, ButtonId.FREE_TEXT_SPACING.value())
                    .addToolButton(ToolbarButtonType.DATE, ButtonId.DATE.value())
                    .addToolButton(ToolbarButtonType.CHECKMARK, ButtonId.CHECKMARK.value())
                    .addToolButton(ToolbarButtonType.CROSS, ButtonId.CROSS.value())
                    .addToolButton(ToolbarButtonType.DOT, ButtonId.DOT.value())
                    .addToolButton(ToolbarButtonType.STAMP, ButtonId.STAMP.value())
                    .addToolButton(ToolbarButtonType.MULTI_SELECT, ButtonId.MULTI_SELECT.value())
                    .addToolButton(ToolbarButtonType.EDIT_TOOLBAR, ButtonId.CUSTOMIZE.value(), 999)
                    .addToolStickyOptionButton(ToolbarButtonType.UNDO, ButtonId.UNDO.value())
                    .setIcon(R.drawable.ic_fill_and_sign)
                    .setToolbarName(R.string.toolbar_title_fill_and_sign);

    public static AnnotationToolbarBuilder defaultFillAndSignToolbarCompact =
            defaultFillAndSignToolbar.copy()
                    .addLeadingToolStickyButton(ToolbarButtonType.NAVIGATION, ButtonId.NAVIGATION.value());

    public static AnnotationToolbarBuilder defaultPrepareFormToolbar =
            AnnotationToolbarBuilder.withTag(TAG_PREPARE_FORM_TOOLBAR)
                    .addToolButton(ToolbarButtonType.LIST_BOX, ButtonId.LIST_BOX.value())
                    .addToolButton(ToolbarButtonType.TEXT_FIELD, ButtonId.TEXT_FIELD.value())
                    .addToolButton(ToolbarButtonType.CHECKBOX, ButtonId.CHECKBOX.value())
                    .addToolButton(ToolbarButtonType.COMBO_BOX, ButtonId.COMBO_BOX.value())
                    .addToolButton(ToolbarButtonType.SIGNATURE_FIELD, ButtonId.SIGNATURE_FIELD.value())
                    .addToolButton(ToolbarButtonType.RADIO_BUTTON, ButtonId.RADIO_BUTTON.value())
                    .addToolButton(ToolbarButtonType.MULTI_SELECT, ButtonId.MULTI_SELECT.value())
                    .addToolButton(ToolbarButtonType.EDIT_TOOLBAR, ButtonId.CUSTOMIZE.value(), 999)
                    .addToolStickyOptionButton(ToolbarButtonType.UNDO, ButtonId.UNDO.value())
                    .setIcon(R.drawable.ic_prepare_form)
                    .setToolbarName(R.string.toolbar_title_prepare_form);

    public static AnnotationToolbarBuilder defaultPrepareFormToolbarCompact =
            defaultPrepareFormToolbar.copy()
                    .addLeadingToolStickyButton(ToolbarButtonType.NAVIGATION, ButtonId.NAVIGATION.value());

    public static AnnotationToolbarBuilder defaultMeasureToolbar =
            AnnotationToolbarBuilder.withTag(TAG_MEASURE_TOOLBAR)
                    .addToolButton(ToolbarButtonType.RULER, ButtonId.RULER.value())
                    .addToolButton(ToolbarButtonType.PERIMETER, ButtonId.PERIMETER.value())
                    .addToolButton(ToolbarButtonType.AREA, ButtonId.AREA.value())
                    .addToolButton(ToolbarButtonType.RECT_AREA, ButtonId.RECT_AREA.value())
                    .addToolButton(ToolbarButtonType.MULTI_SELECT, ButtonId.MULTI_SELECT.value())
                    .addToolButton(ToolbarButtonType.COUNT_MEASUREMENT, ButtonId.COUNT_TOOL.value())
                    .addToolButton(ToolbarButtonType.EDIT_TOOLBAR, ButtonId.CUSTOMIZE.value(), 999)
                    .addToolStickyOptionButton(ToolbarButtonType.UNDO, ButtonId.UNDO.value())
                    .setIcon(R.drawable.ic_annotation_distance_black_24dp)
                    .setToolbarName(R.string.toolbar_title_measure);

    public static AnnotationToolbarBuilder defaultMeasureToolbarCompact =
            defaultMeasureToolbar.copy()
                    .addLeadingToolStickyButton(ToolbarButtonType.NAVIGATION, ButtonId.NAVIGATION.value());

    public static AnnotationToolbarBuilder defaultPensToolbar =
            AnnotationToolbarBuilder.withTag(TAG_PENS_TOOLBAR)
                    .addToolButton(ToolbarButtonType.INK, ButtonId.INK_1.value())
                    .addToolButton(ToolbarButtonType.INK, ButtonId.INK_2.value())
                    .addToolButton(ToolbarButtonType.FREE_HIGHLIGHT, ButtonId.FREE_HIGHLIGHT1.value())
                    .addToolButton(ToolbarButtonType.FREE_HIGHLIGHT, ButtonId.FREE_HIGHLIGHT2.value())
                    .addToolButton(ToolbarButtonType.ERASER, ButtonId.ERASER.value())
                    .addToolButton(ToolbarButtonType.MULTI_SELECT, ButtonId.MULTI_SELECT.value())
                    .addToolButton(ToolbarButtonType.LASSO_SELECT, ButtonId.LASSO_SELECT.value())
                    .addToolButton(ToolbarButtonType.EDIT_TOOLBAR, ButtonId.CUSTOMIZE.value(), 999)
                    .addToolStickyOptionButton(ToolbarButtonType.UNDO, ButtonId.UNDO.value())
                    .setIcon(R.drawable.ic_annotation_freehand_black_24dp)
                    .setToolbarName(R.string.toolbar_title_pens);

    public static AnnotationToolbarBuilder defaultPensToolbarCompact =
            defaultPensToolbar.copy()
                    .addLeadingToolStickyButton(ToolbarButtonType.NAVIGATION, ButtonId.NAVIGATION.value());

    public static AnnotationToolbarBuilder defaultRedactionToolbar =
            AnnotationToolbarBuilder.withTag(TAG_REDACTION_TOOLBAR)
                    .addToolButton(ToolbarButtonType.TEXT_REDACTION, ButtonId.TEXT_REDACTION.value())
                    .addToolButton(ToolbarButtonType.RECT_REDACTION, ButtonId.RECT_REDACTION.value())
                    .addToolButton(ToolbarButtonType.PAGE_REDACTION, ButtonId.REDACT_PAGE.value())
                    .addToolButton(ToolbarButtonType.SEARCH_REDACTION, ButtonId.REDACT_SEARCH.value())
                    .addToolButton(ToolbarButtonType.EDIT_TOOLBAR, ButtonId.CUSTOMIZE.value(), 999)
                    .addToolStickyOptionButton(ToolbarButtonType.UNDO, ButtonId.UNDO.value())
                    .setIcon(R.drawable.ic_annotation_redact_black_24dp)
                    .setToolbarName(R.string.tools_qm_redact);

    public static AnnotationToolbarBuilder defaultRedactionToolbarCompact =
            defaultRedactionToolbar.copy()
                    .addLeadingToolStickyButton(ToolbarButtonType.NAVIGATION, ButtonId.NAVIGATION.value());

    public static AnnotationToolbarBuilder defaultFavoriteToolbar = AnnotationToolbarBuilder.withTag(TAG_FAVORITE_TOOLBAR)
            .addToolButton(ToolbarButtonType.EDIT_TOOLBAR, ButtonId.CUSTOMIZE.value(), 999)
            .addToolStickyOptionButton(ToolbarButtonType.UNDO, ButtonId.UNDO.value())
            .setIcon(R.drawable.ic_star_white_24dp)
            .setToolbarName(R.string.toolbar_title_favorite);

    public static AnnotationToolbarBuilder defaultFavoriteToolbarCompact =
            defaultFavoriteToolbar.copy()
                    .addLeadingToolStickyButton(ToolbarButtonType.NAVIGATION, ButtonId.NAVIGATION.value());

    public static AnnotationToolbarBuilder getDefaultAnnotationToolbarBuilderByTag(@NonNull String tag) {
        if (TAG_ANNOTATE_TOOLBAR.equals(tag)) {
            return defaultAnnotateToolbar.copy();
        } else if (TAG_DRAW_TOOLBAR.equals(tag)) {
            return defaultDrawToolbar.copy();
        } else if (TAG_INSERT_TOOLBAR.equals(tag)) {
            return defaultInsertToolbar.copy();
        } else if (TAG_FILL_AND_SIGN_TOOLBAR.equals(tag)) {
            return defaultFillAndSignToolbar.copy();
        } else if (TAG_PREPARE_FORM_TOOLBAR.equals(tag)) {
            return defaultPrepareFormToolbar.copy();
        } else if (TAG_MEASURE_TOOLBAR.equals(tag)) {
            return defaultMeasureToolbar.copy();
        } else if (TAG_PENS_TOOLBAR.equals(tag)) {
            return defaultPensToolbar.copy();
        } else if (TAG_REDACTION_TOOLBAR.equals(tag)) {
            return defaultRedactionToolbar.copy();
        } else if (TAG_FAVORITE_TOOLBAR.equals(tag)) {
            return defaultFavoriteToolbar.copy();
        }
        return defaultViewToolbar.copy();
    }

    /**
     * Collection of toolbar button ids used by the default viewer toolbars.
     */
    public enum ButtonId {

        // Annotate Toolbar Ids
        TEXT_HIGHLIGHT(8000),
        FREE_HIGHLIGHT(8001),
        TEXT_UNDERLINE(8002),
        TEXT_STRIKEOUT(8003),
        TEXT_SQUIGGLY(8004),
        STICKY_NOTE(8005),
        FREE_TEXT(8006),
        CALLOUT(8007),
        INK(8008),
        ERASER(8009),
        MULTI_SELECT(8010),
        LASSO_SELECT(8011),
        CUSTOMIZE(8012),
        UNDO(8013),
        REDO(8014),

        // Draw Toolbar Ids
        SQUARE(8015),
        CIRCLE(8016),
        POLYGON(8017),
        POLY_CLOUD(8018),
        LINE(8019),
        ARROW(8020),
        POLYLINE(8021),

        // Insert Toolbar Ids
        ADD_PAGE(8022),
        IMAGE(8023),
        STAMP(8024),
        SIGNATURE(8025),
        LINK(8026),
        SOUND(8027),
        ATTACHMENT(8028),

        // Fill and Sign Toolbar Ids
        FREE_TEXT_SPACING(8029),
        DATE(8030),
        CHECKMARK(8031),
        CROSS(8032),
        DOT(8033),

        // Prepare Form Toolbar Ids
        LIST_BOX(8034),
        TEXT_FIELD(8035),
        CHECKBOX(8036),
        COMBO_BOX(8037),
        SIGNATURE_FIELD(8038),
        RADIO_BUTTON(8039),

        // Measure Toolbar Ids
        RULER(8040),
        PERIMETER(8041),
        AREA(8042),
        RECT_AREA(8043),

        // Redact toolbar
        TEXT_REDACTION(8044),
        RECT_REDACTION(8045),
        REDACT_PAGE(8046),
        REDACT_SEARCH(8047),

        // Pens toolbar Ids
        INK_1(8048),
        INK_2(8049),
        FREE_HIGHLIGHT1(8050),
        FREE_HIGHLIGHT2(8051),

        // Draw Toolbar
        SMART_PEN(8052),

        // Navigation for compact mode
        NAVIGATION(8053),

        // Menu Items
        MORE(8054),

        // Measure toolbar
        COUNT_TOOL(8055);

        private final int mId;

        ButtonId(int id) {
            mId = id;
        }

        public int value() {
            return mId;
        }
    }
}
