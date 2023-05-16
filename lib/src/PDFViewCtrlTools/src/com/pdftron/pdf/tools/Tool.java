//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;
import androidx.core.graphics.ColorUtils;
import androidx.core.widget.EdgeEffectCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Action;
import com.pdftron.pdf.ActionParameter;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.CurvePainter;
import com.pdftron.pdf.Field;
import com.pdftron.pdf.Font;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.ComboBoxWidget;
import com.pdftron.pdf.annots.FreeText;
import com.pdftron.pdf.annots.Link;
import com.pdftron.pdf.annots.ListBoxWidget;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.annots.Widget;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.dialog.widgetchoice.ChoiceDialogFragment;
import com.pdftron.pdf.dialog.widgetchoice.ChoiceResult;
import com.pdftron.pdf.dialog.widgetchoice.ChoiceViewModel;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.ActionUtils;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.AnnotationClipboardHelper;
import com.pdftron.pdf.utils.Event;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.SnapUtils;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.pdf.widget.AnnotView;
import com.pdftron.pdf.widget.RotateHandleView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * The base class that implements the ToolManager.Tool interface and several
 * basic tool functionalities.
 */
@SuppressWarnings("ALL")
public abstract class Tool implements ToolManager.Tool {

    private static final String TAG = Tool.class.getName();

    protected static boolean sDebug;

    public static final String PDFTRON_ID = "pdftron";
    public static final String PDFTRON_THICKNESS = "pdftron_thickness";

    public static final String KEYS = "PDFTRON_KEYS";
    public static final String METHOD_FROM = "METHOD_FROM";
    public static final String PAGE_NUMBER = "PAGE_NUMBER";
    public static final String IS_LINK = "IS_LINK";
    public static final String LINK_URL = "LINK_URL";
    public static final String LINK_RECTF = "LINK_RECTF";
    public static final String FLATTENED = "flattened";

    private static final String PREFS_FILE_NAME = "com_pdftron_pdfnet_pdfviewctrl_prefs_file";

    // user-default annotation properties
    public static final String PREF_ANNOTATION_CREATION_LINE = "annotation_creation"; // line
    public static final String PREF_ANNOTATION_CREATION_ARROW = "annotation_creation_arrow";
    public static final String PREF_ANNOTATION_CREATION_POLYLINE = "annotation_creation_polyline";
    public static final String PREF_ANNOTATION_CREATION_RECTANGLE = "annotation_creation_rectangle";
    public static final String PREF_ANNOTATION_CREATION_OVAL = "annotation_creation_oval";
    public static final String PREF_ANNOTATION_CREATION_POLYGON = "annotation_creation_polygon";
    public static final String PREF_ANNOTATION_CREATION_CLOUD = "annotation_creation_cloud";
    public static final String PREF_ANNOTATION_CREATION_HIGHLIGHT = "annotation_creation_highlight";
    public static final String PREF_ANNOTATION_CREATION_LINK = "annotation_creation_link";
    public static final String PREF_ANNOTATION_CREATION_UNDERLINE = "annotation_creation_text_markup"; // underline
    public static final String PREF_ANNOTATION_CREATION_STRIKEOUT = "annotation_creation_strikeout";
    public static final String PREF_ANNOTATION_CREATION_SQUIGGLY = "annotation_creation_squiggly";
    public static final String PREF_ANNOTATION_CREATION_FREETEXT = "annotation_creation_freetext";
    public static final String PREF_ANNOTATION_CREATION_FREEHAND = "annotation_creation_freehand";
    public static final String PREF_ANNOTATION_CREATION_NOTE = "annotation_creation_note";
    public static final String PREF_ANNOTATION_CREATION_ERASER = "annotation_creation_eraser";
    public static final String PREF_ANNOTATION_CREATION_SIGNATURE = "annotation_creation_signature";
    public static final String PREF_ANNOTATION_CREATION_FREE_HIGHLIGHTER = "annotation_creation_free_highlighter";
    public static final String PREF_ANNOTATION_CREATION_COLOR = "_color";
    public static final String PREF_ANNOTATION_CREATION_TEXT_COLOR = "_text_color";
    public static final String PREF_ANNOTATION_CREATION_FILL_COLOR = "_fill_color";
    public static final String PREF_ANNOTATION_CREATION_OPACITY = "_opacity";
    public static final String PREF_ANNOTATION_CREATION_THICKNESS = "_thickness";
    public static final String PREF_ANNOTATION_CREATION_TEXT_SIZE = "_text_size";
    public static final String PREF_ANNOTATION_CREATION_ICON = "_icon";
    public static final String PREF_ANNOTATION_CREATION_FONT = "_font";

    public static final String ANNOTATION_NOTE_ICON_FILE_PREFIX = "annotation_note_icon_";
    public static final String ANNOTATION_NOTE_ICON_FILE_POSTFIX_FILL = "_fill";
    public static final String ANNOTATION_NOTE_ICON_FILE_POSTFIX_OUTLINE = "_outline";
    public static final String ANNOTATION_TOOLBAR_SIGNATURE_STATE = "annotation_toolbar_signature_state";
    public static final String ANNOTATION_FREE_TEXT_FONTS = "annotation_property_free_text_fonts_list"; // Note: this font list is also used for widgets
    public static final String ANNOTATION_FREE_TEXT_JSON_FONT_FILE_PATH = "filepath";
    public static final String ANNOTATION_FREE_TEXT_JSON_FONT_DISPLAY_NAME = "display name";
    public static final String ANNOTATION_FREE_TEXT_JSON_FONT_DISPLAY_IN_LIST = "display font";
    public static final String ANNOTATION_FREE_TEXT_JSON_FONT_PDFTRON_NAME = "pdftron name";
    public static final String ANNOTATION_FREE_TEXT_JSON_FONT_NAME = "font name";
    public static final String ANNOTATION_FREE_TEXT_JSON_FONT = "fonts";
    public static final String STAMP_SHOW_FLATTEN_WARNING = "stamp_show_flatten_warning";
    // list of white listed annotation free text fonts
    // the fonts on this list are from: https://www.microsoft.com/typography/fonts/popular.aspx,
    // the Base14 Fonts and other common fonts found on devices
    public static final String[] ANNOTATION_FREE_TEXT_WHITELIST_FONTS = {
            "Gill", "Calibri", "Arial",
            "SimSun", "Curlz", "Times", "Lucida", "Rockwell",
            "Old English", "Abadi", "Twentieth Century",
            "News Gothic", "Bodoni", "Candara", "PMingLiU",
            "Palace Script", "Helvetica", "Courier", "Roboto",
            "Comic", "Droid", "Georgia", "MotoyaLManu", "NanumGothic",
            "Kaiti", "Miaowu", "ShaoNV", "Rosemary",
            "Coming Soon", "Dancing Script", "Dancing Script Bold"
    };
    public static final int ANNOTATION_FREE_TEXT_PREFERENCE_INLINE = 1;
    public static final int ANNOTATION_FREE_TEXT_PREFERENCE_DIALOG = 2;
    public static final String ANNOTATION_FREE_TEXT_PREFERENCE_EDITING = "annotation_free_text_preference_editing";
    public static final int ANNOTATION_FREE_TEXT_PREFERENCE_EDITING_DEFAULT = ANNOTATION_FREE_TEXT_PREFERENCE_INLINE;

    // form field appearance constants
    public static final String FORM_FIELD_SYMBOL_CHECKBOX = "4";
    public static final String FORM_FIELD_SYMBOL_CIRCLE = "l";
    public static final String FORM_FIELD_SYMBOL_CROSS = "8";
    public static final String FORM_FIELD_SYMBOL_DIAMOND = "u";
    public static final String FORM_FIELD_SYMBOL_SQUARE = "n";
    public static final String FORM_FIELD_SYMBOL_STAR = "H";

    // Translation languages properties
    public static final String PREF_TRANSLATION_SOURCE_LANGUAGE_CODE_KEY = "translation_source_language_code";
    public static final String PREF_TRANSLATION_TARGET_LANGUAGE_CODE_KEY = "translation_target_language_code";
    public static final String LAST_DEVICE_LOCALE_LANGUAGE = "last_device_locale_language";
    public static final String PREF_TRANSLATION_SOURCE_LANGUAGE_CODE_DEFAULT = "en";    // english
    public static final String PREF_TRANSLATION_TARGET_LANGUAGE_CODE_DEFAULT = "fr";    // french
    public static final int ANNOT_PERMISSION_SELECTION = 0;
    public static final int ANNOT_PERMISSION_MENU = 1;
    public static final int ANNOT_PERMISSION_FILL_AND_SIGN = 2;
    public static final int ANNOT_PERMISSION_INTERACT = 3;

    public static final int QM_MAX_ROW_SIZE = 4;   // maximum size for quick menu row

    protected PDFViewCtrl mPdfViewCtrl;
    protected ToolManager.ToolModeBase mNextToolMode;
    protected ToolManager.ToolModeBase mCurrentDefaultToolMode; // the default tool in continuous annotating mode, used for allowing editing
    protected Annot mAnnot;
    protected int mAnnotPageNum;
    protected int mSelectPageNum;
    protected RectF mAnnotBBox; // In page space
    protected QuickMenu mQuickMenu;
    protected String mMruMenuItems[];
    protected String mOverflowMenuItems[];
    protected boolean mJustSwitchedFromAnotherTool;
    protected boolean mAvoidLongPressAttempt;
    protected float mPageNumPosAdjust;
    protected RectF mTempPageDrawingRectF;
    protected boolean mForceSameNextToolMode;
    protected boolean mAnnotPushedBack;
    protected boolean mAllowTwoFingerScroll;
    protected boolean mAllowOneFingerScrollWithStylus;
    protected boolean mAllowScrollWithTapTool;

    // Custom ink flags
    protected boolean mAllowTapToSelect = false;
    protected boolean mMultiStrokeMode = true;
    protected boolean mTimedModeEnabled = true;

    protected boolean mUpFromCalloutCreate;

    // group annots
    protected ArrayList<Annot> mGroupAnnots;

    // Stylus support
    protected boolean mIsStylus;
    protected boolean mStylusUsed;

    protected boolean mAllowZoom;
    protected boolean mHasMenuPermission = true;
    protected boolean mHasSelectionPermission = true;
    protected boolean mHasInteractPermission = true;

    // edge effect
    private EdgeEffectCompat mEdgeEffectLeft;
    private EdgeEffectCompat mEdgeEffectRight;
    private Markup mMarkupToAuthor;
    private boolean mPageNumberIndicatorVisible;

    protected AnnotView mAnnotView;
    protected RotateHandleView mRotateHandle;
    protected AnnotStyle mAnnotStyle;

    // snap to point
    private boolean mSnappingEnabled;
    private SnapUtils mSnapUtils;

    /*
     * Used to remove the shown page number
     */
    private PageNumberRemovalHandler mPageNumberRemovalHandler = new PageNumberRemovalHandler(this);

    protected CompositeDisposable mBitmapDisposable = new CompositeDisposable();

    @NonNull
    private Bundle mBundle = new Bundle();

    /**
     * Class constructor
     */
    public Tool(@NonNull PDFViewCtrl ctrl) {
        mPdfViewCtrl = ctrl;
        mNextToolMode = ToolMode.PAN;
        mCurrentDefaultToolMode = ToolMode.PAN;
        mAnnot = null;
        mAnnotBBox = new RectF();
        mJustSwitchedFromAnotherTool = false;
        mForceSameNextToolMode = false;
        mAvoidLongPressAttempt = false;
        mAnnotPushedBack = false;
        mPageNumPosAdjust = 0;
        mTempPageDrawingRectF = new RectF();

        mPageNumberIndicatorVisible = true;

        mAllowTwoFingerScroll = false;
        mAllowOneFingerScrollWithStylus = false;
        mAllowZoom = true;

        // Disable page turning (in non-continuous page presentation mode);
        // it is only turned on in Pan tool.
        mPdfViewCtrl.setBuiltInPageSlidingState(false);

        // Sets up edge effects
        mEdgeEffectLeft = new EdgeEffectCompat(ctrl.getContext());
        mEdgeEffectRight = new EdgeEffectCompat(ctrl.getContext());

        // find quick menu
        int childCount = mPdfViewCtrl.getChildCount();

        for (int i = 0; i < mPdfViewCtrl.getChildCount(); i++) {
            if (mPdfViewCtrl.getChildAt(i) instanceof QuickMenu) {
                mQuickMenu = (QuickMenu) mPdfViewCtrl.getChildAt(i);
                break;
            }
        }
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        boolean stylusAsPen = toolManager.isStylusAsPen();
        mPdfViewCtrl.setStylusScaleEnabled(!stylusAsPen);

        mSnapUtils = toolManager.getSnapUtils();
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#getToolMode()}.
     */
    @Override
    public abstract ToolManager.ToolModeBase getToolMode();

    public abstract int getCreateAnnotType();

    /**
     * The overload implementation of {@link ToolManager.Tool#getNextToolMode()}.
     */
    @Override
    final public ToolManager.ToolModeBase getNextToolMode() {
        return mNextToolMode;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#isCreatingAnnotation()}.
     */
    @Override
    public boolean isCreatingAnnotation() {
        return false;
    }

    /**
     * Whether the purpose of this tool is editing annotation
     *
     * @return false
     */
    public boolean isEditAnnotTool() {
        return false;
    }

    /**
     * Whether the current quick menu includes a certain menu type
     *
     * @param menuId the menu item id
     */
    public boolean hasMenuEntry(@IdRes int menuId) {
        if (!isQuickMenuShown()) {
            return false;
        }
        if (mQuickMenu.getMenu().findItem(menuId) != null) {
            return true;
        }
        return false;
    }

    /**
     * paste annotation
     *
     * @param targetPoint that target position the annotation is going to paste to
     */
    void pasteAnnot(PointF targetPoint) {
        if (mPdfViewCtrl == null || !AnnotationClipboardHelper.isItemCopied(mPdfViewCtrl.getContext())) {
            return;
        }

        int pageNumber = mPdfViewCtrl.getPageNumberFromScreenPt(targetPoint.x, targetPoint.y);
        if (pageNumber == -1) {
            pageNumber = mPdfViewCtrl.getCurrentPage();
        }

        if (Utils.isImageCopied(mPdfViewCtrl.getContext())) {
            try {
                ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                ToolManager.Tool tool = toolManager.createTool(ToolMode.STAMPER, this);
                toolManager.setTool(tool);
                ((Stamper) tool).addStampFromClipboard(targetPoint);
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            }
        } else if (AnnotationClipboardHelper.isAnnotCopied()) {
            AnnotationClipboardHelper.pasteAnnot(mPdfViewCtrl.getContext(), mPdfViewCtrl, pageNumber, targetPoint, null);
        }
    }

    /**
     * get acutal next tool mode safely
     *
     * @param toolMode next tool mode
     * @return next tool mode
     */
    protected ToolManager.ToolModeBase safeSetNextToolMode(ToolManager.ToolModeBase toolMode) {
        if (null != toolMode && toolMode instanceof ToolMode) {
            boolean disabled = ((ToolManager) mPdfViewCtrl.getToolManager()).isToolModeDisabled((ToolMode) toolMode);
            if (disabled) {
                return ToolMode.PAN;
            }
        }
        return toolMode;
    }

    /**
     * creates a quick menu. It is used for {@link #showMenu(RectF)}
     *
     * @return quick menu
     */
    protected QuickMenu createQuickMenu() {
        QuickMenu quickMenu = new QuickMenu(mPdfViewCtrl, mHasMenuPermission, getToolMode());
        return quickMenu;
    }

    /**
     * Customize quick menu item inside quick menu
     *
     * @param menuBuilder quick menu builder, can be obtained by calling {@link QuickMenu#getMenu()}
     */
    protected void customizeQuickMenuItems(QuickMenu quickMenu) {
        if (null == mAnnot) {
            return;
        }
        boolean shouldUnlockRead = false;
        try {
            // Locks the document first as accessing annotation/doc information isn't thread
            // safe. Since we are not going to modify the doc here, we can use the read lock.
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;

            // appearance
            QuickMenuItem appearanceMenuItem = quickMenu.findMenuItem(R.id.qm_appearance);
            if (appearanceMenuItem != null) {
                int color;
                float opacity = 1.0f;
                ColorPt colorPt = mAnnot.getColorAsRGB();
                color = Utils.colorPt2color(colorPt);
                if (mAnnot.getType() == Annot.e_FreeText) {
                    FreeText freeText = new FreeText(mAnnot);
                    if (freeText.getTextColorCompNum() == 3) {
                        ColorPt fillColorPt = freeText.getTextColor();
                        color = Utils.colorPt2color(fillColorPt);
                    }
                    // if it is rich text created, hide style icon
                    String rawHTML = freeText.getCustomData(AnnotUtils.KEY_RawRichContent);
                    if (!Utils.isNullOrEmpty(rawHTML)) {
                        appearanceMenuItem.setVisible(false);
                    }
                }
                if (mAnnot.isMarkup()) {
                    // if has fill color, use fill color
                    Markup m = new Markup(mAnnot);
                    if (m.getInteriorColorCompNum() == 3) {
                        ColorPt fillColorPt = m.getInteriorColor();
                        int fillColor = Utils.colorPt2color(fillColorPt);
                        if (fillColor != Color.TRANSPARENT) {
                            color = fillColor;
                        }
                    }
                    // opacity
                    opacity = (float) m.getOpacity();
                }
                int background = Utils.getBackgroundColor(mPdfViewCtrl.getContext());
                int foreground = ColorUtils.compositeColors(Color.argb((int) (opacity * 255), Color.red(color), Color.green(color), Color.blue(color)), background);

                boolean isColorSpaceClose = Utils.isTwoColorSimilar(background, foreground, 12);

                if (!isColorSpaceClose) {
                    appearanceMenuItem.setColor(color);
                    appearanceMenuItem.setOpacity(opacity);
                }
            }
            // note
            QuickMenuItem noteItem = quickMenu.findMenuItem(R.id.qm_note);
            if (noteItem != null
                    && mAnnot.isMarkup()
                    && mAnnot.getType() != Annot.e_FreeText) {
                String contents = mAnnot.getContents();
                if (Utils.isNullOrEmpty(contents)) {
                    if (noteItem.getIcon() != null) {
                        noteItem.setIcon(R.drawable.ic_annotation_sticky_note_black_24dp);
                    }
                    noteItem.setTitle(R.string.tools_qm_add_note);
                } else {
                    if (noteItem.getIcon() != null) {
                        noteItem.setIcon(R.drawable.ic_chat_black_24dp);
                    }
                    noteItem.setTitle(R.string.tools_qm_view_note);
                }
            }
            // type item
            QuickMenuItem typeItem = quickMenu.findMenuItem(R.id.qm_type);
            if (typeItem != null && typeItem.hasSubMenu()) {
                QuickMenuBuilder subMenu = (QuickMenuBuilder) typeItem.getSubMenu();
                if (mAnnot != null) {
                    if (mAnnot.getType() == Annot.e_Highlight) {
                        subMenu.removeItem(R.id.qm_highlight);
                    } else if (mAnnot.getType() == Annot.e_StrikeOut) {
                        subMenu.removeItem(R.id.qm_strikeout);
                    } else if (mAnnot.getType() == Annot.e_Underline) {
                        subMenu.removeItem(R.id.qm_underline);
                    } else if (mAnnot.getType() == Annot.e_Squiggly) {
                        subMenu.removeItem(R.id.qm_squiggly);
                    }
                }
            }
            // edit
            QuickMenuItem editItem = quickMenu.findMenuItem(R.id.qm_edit);
            if (editItem != null) {
                if (mAnnot.getType() == Annot.e_Ink) {
                    editItem.setVisible(((ToolManager) mPdfViewCtrl.getToolManager()).editInkAnnots());
                } else if (mAnnot.getType() == Annot.e_Widget) {
                    Field field = (new Widget(mAnnot)).getField();
                    if (field.isValid()) {
                        boolean readOnly = field.getFlag(Field.e_read_only);
                        editItem.setVisible(!readOnly);
                    }
                }
            }
        } catch (PDFNetException e) {
            e.printStackTrace();
            AnalyticsHandlerAdapter.getInstance().sendException(e, "failed in AnnotEdit.createQuickMenu");
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
    }

    /**
     * Checks whether the annotation has the specified permission
     *
     * @param annot The annotation
     * @param kind  The kind of permission. Possible values are
     *              {@link ANNOT_PERMISSION_SELECTION},
     *              {@link ANNOT_PERMISSION_MENU}
     *              {@link ANNOT_PERMISSION_FILL_AND_SIGN}
     *              {@link ANNOT_PERMISSION_INTERACT}
     * @return True if the annotation has permission
     */
    protected boolean hasPermission(Annot annot, int kind) {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        if (toolManager.getAnnotationPermissionListener() != null) {
            return toolManager.getAnnotationPermissionListener().hasPermissions(annot, kind);
        }
        return AnnotUtils.hasPermission(mPdfViewCtrl, annot, kind);
    }

    /**
     * @return analytics handler label mode
     */
    protected int getModeAHLabel() {
        return AnalyticsHandlerAdapter.LABEL_QM_ANNOTSELECT;
    }

    /**
     * Executes an action with the specified parameters.
     * <p>
     * <div class="warning">
     * Note that the PDF doc should have been locked when call this method.
     * In addition, ToolManager's raise annotation should be handled in the caller function.
     * </div>
     *
     * @param actionParam The action parameters
     */
    public void executeAction(ActionParameter actionParam) {
        ActionUtils.getInstance().executeAction(actionParam, mPdfViewCtrl);
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onDocumentDownloadEvent(PDFViewCtrl.DownloadState, int, int, int, String)}.
     */
    @Override
    public void onDocumentDownloadEvent(PDFViewCtrl.DownloadState state, int page_num, int page_downloaded, int page_count, String message) {
    }

    /**
     * Handles generic motion event.
     *
     * @param event The motion event
     * @return True if handled
     */
    boolean onGenericMotionEvent(MotionEvent event) {
        if (mPdfViewCtrl == null) {
            return false;
        }

        if (ShortcutHelper.isZoomIn(event)) {
            mPdfViewCtrl.setZoom((int) event.getX(), (int) event.getY(),
                    mPdfViewCtrl.getZoom() * PDFViewCtrl.SCROLL_ZOOM_FACTOR, true, true);
            return true;
        } else if (ShortcutHelper.isZoomOut(event)) {
            mPdfViewCtrl.setZoom((int) event.getX(), (int) event.getY(),
                    mPdfViewCtrl.getZoom() / PDFViewCtrl.SCROLL_ZOOM_FACTOR, true, true);
            return true;
        } else if (ShortcutHelper.isScroll(event)) {
            int dx = (int) (event.getAxisValue(MotionEvent.AXIS_HSCROLL) * 100);
            int dy = (int) (event.getAxisValue(MotionEvent.AXIS_VSCROLL) * 100);
            if (dx != 0 || dy != 0) {
                mPdfViewCtrl.scrollBy(dx, -dy);
            }
            return true;
        }

        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onKeyUp(int, KeyEvent)}.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mPdfViewCtrl == null) {
            return false;
        }

        if (ShortcutHelper.isPaste(keyCode, event)) {
            pasteAnnot(mPdfViewCtrl.getCurrentMousePosition());
            return true;
        }

        if (isQuickMenuShown() && ShortcutHelper.isCloseMenu(keyCode, event)) {
            closeQuickMenu();
            unsetAnnot();
            mNextToolMode = mCurrentDefaultToolMode;
            mPdfViewCtrl.invalidate();
            return true;
        }

        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onDown(MotionEvent)}.
     */
    @Override
    public boolean onDown(MotionEvent e) {
        mAllowZoom = !(!mPdfViewCtrl.isZoomingInAddingAnnotationEnabled() && isCreatingAnnotation());
        mPdfViewCtrl.setZoomEnabled(mAllowZoom);
        closeQuickMenu();
        setSnapCanDrawShapeIfEnabled(true);

        if (isCreatingAnnotation()) {
            // stylus
            if (mIsStylus && e.getPointerCount() == 1 && e.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) {
                mIsStylus = false;
            } else if (!mIsStylus && e.getPointerCount() == 1 && e.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
                mIsStylus = true;
            }

            if (!mStylusUsed) {
                mStylusUsed = mIsStylus;
            }
        }
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onPointerDown(MotionEvent)}.
     */
    @Override
    public boolean onPointerDown(MotionEvent e) {
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        setSnapCanDrawShapeIfEnabled(false);
        mPageNumberRemovalHandler.sendEmptyMessageDelayed(1, 3000);
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onFlingStop()}.
     */
    @Override
    public boolean onFlingStop() {
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        if (ShortcutHelper.isZoomInOut(e2)) {
            if (e1.getY() < e2.getY()) {
                mPdfViewCtrl.setZoom((int) e2.getX(), (int) e2.getY(),
                        mPdfViewCtrl.getZoom() * PDFViewCtrl.SCROLL_ZOOM_FACTOR, true, true);
                return true;
            } else if (e1.getY() > e2.getY()) {
                mPdfViewCtrl.setZoom((int) e2.getX(), (int) e2.getY(),
                        mPdfViewCtrl.getZoom() / PDFViewCtrl.SCROLL_ZOOM_FACTOR, true, true);
                return true;
            }
        }

        if (isCreatingAnnotation()) {
            if (e1.getPointerCount() == 2 || e2.getPointerCount() == 2) {
                mAllowTwoFingerScroll = true;
            }

            // check to see whether use finger to scroll or not
            mAllowOneFingerScrollWithStylus = mStylusUsed && e2.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS;
        } else {
            mAllowTwoFingerScroll = false;
        }

        // Enable page turning (in non-continuous page presentation mode);
        // it is always enabled for pan mode and text-highlighter
        // it is enabled only if scrolled with two fingers in other modes
        if (getToolMode() == ToolMode.PAN ||
                getToolMode() == ToolMode.TEXT_SELECT ||
                getToolMode() == ToolMode.TEXT_HIGHLIGHTER) {
            mPdfViewCtrl.setBuiltInPageSlidingState(true);
        } else {
            if (mAllowTwoFingerScroll || mAllowOneFingerScrollWithStylus || mAllowScrollWithTapTool) {
                mPdfViewCtrl.setBuiltInPageSlidingState(true);
            } else {
                mPdfViewCtrl.setBuiltInPageSlidingState(false);
            }
        }

        showTransientPageNumber();
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onScrollChanged(int, int, int, int)}.
     */
    @Override
    public void onScrollChanged(int l, int t, int oldl, int oldt) {
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onPageTurning(int, int)}.
     */
    @Override
    public void onPageTurning(int old_page, int cur_page) {
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onSingleTapConfirmed(MotionEvent)}.
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onPostSingleTapConfirmed()}.
     */
    @Override
    public void onPostSingleTapConfirmed() {
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onSingleTapUp(MotionEvent)}.
     */
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onDoubleTap(MotionEvent)}.
     */
    @Override
    public boolean onDoubleTap(MotionEvent e) {
        showTransientPageNumber();

        // The following code shows how to override the double tap behavior of PDFViewCtrl.
        boolean customize = true;
        if (!customize) {
            // Let PDFViewCtrl handle how double tap zooms
            return false;
        } else {
            if (isCreatingAnnotation()) {
                // Disable double tap in annotation creation mode
                if (mStylusUsed && mIsStylus) {
                    return true;
                }
            }
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            if (!toolManager.isDoubleTapToZoom()) {
                // disable double tap to zoom
                return true;
            }
            boolean animate = true;
            // I want to customize how double tap zooms
            int x = (int) (e.getX() + 0.5);
            int y = (int) (e.getY() + 0.5);
            final PDFViewCtrl.PageViewMode refMode;
            if (mPdfViewCtrl.isMaintainZoomEnabled()) {
                refMode = mPdfViewCtrl.getPreferredViewMode();
            } else {
                refMode = mPdfViewCtrl.getPageRefViewMode();
            }
            if (!ViewerUtils.isViewerZoomed(mPdfViewCtrl)) {
                // Let's try smart zoom first
                boolean result = mPdfViewCtrl.smartZoom(x, y, animate);
                if (!result) {
                    // If not, just zoom in
                    boolean use_snapshot = true;
                    mPdfViewCtrl.setZoom(x, y, mPdfViewCtrl.getZoom() * 2.5, use_snapshot, animate);
                }
            } else {
                mPdfViewCtrl.setPageViewMode(refMode, x, y, animate);
                if (mPdfViewCtrl.isMaintainZoomEnabled()) {
                    mPdfViewCtrl.setPageViewMode(PDFViewCtrl.PageViewMode.ZOOM); // so it doesn't re-fit
                }
            }
            return true;    // This tells PDFViewCtrl to skip its internal logic
        }
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onDoubleTapEnd(MotionEvent)}.
     */
    @Override
    public void onDoubleTapEnd(MotionEvent e) {
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onDoubleTapEvent(MotionEvent)}.
     */
    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onLayout(boolean, int, int, int, int)}.
     */
    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onLongPress(MotionEvent)}.
     */
    @Override
    public boolean onLongPress(MotionEvent e) {
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onScaleBegin(float, float)}.
     */
    @Override
    public boolean onScaleBegin(float x, float y) {
        //(x, y) is the scaling focal point in client space
        if (mAnnot == null && mAnnotView != null) {
            removeAnnotView(false);
        }
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onScale(float, float)}.
     */
    @Override
    public boolean onScale(float x, float y) {
        //(x, y) is the scaling focal point in client space
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onScaleEnd(float, float)}.
     */
    @Override
    public boolean onScaleEnd(float x, float y) {
        //(x, y) is the scaling focal point in client space
        showTransientPageNumber();
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onConfigurationChanged(Configuration)}.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onShowPress(MotionEvent)}.
     */
    @Override
    public boolean onShowPress(MotionEvent e) {
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onClose()}.
     */
    @Override
    public void onClose() {
        mPageNumberRemovalHandler.removeCallbacksAndMessages(null);
        if (mPdfViewCtrl.hasSelection()) {
            mPdfViewCtrl.clearSelection();
        }
        closeQuickMenu();
        mBitmapDisposable.clear();
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onCustomEvent(Object)}.
     */
    @Override
    public void onCustomEvent(Object o) {
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onSetDoc()}.
     */
    @Override
    public void onSetDoc() {
        boolean shouldUnlock = false;
        boolean hasExecutionChanges = false;
        try {
            Action open_action = mPdfViewCtrl.getDoc().getOpenAction();
            if (open_action.isValid() && (open_action.getType() == Action.e_JavaScript)) {
                mPdfViewCtrl.docLock(true);
                shouldUnlock = true;
                ActionParameter action_param = new ActionParameter(open_action);
                executeAction(action_param);
                hasExecutionChanges = mPdfViewCtrl.getDoc().hasChangesSinceSnapshot();
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
                if (hasExecutionChanges) {
                    raiseAnnotationActionEvent();
                }
            }
        }
    }

    /**
     * Called after the tool is created by ToolManager.
     */
    public void onCreate() {
        if (mPageNumberIndicatorVisible) {
            showTransientPageNumber();
        }
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onDraw(Canvas, Matrix)}.
     */
    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {

    }

    protected void drawSnapToShapeIfEnabled(Canvas canvas) {
        if (mSnappingEnabled) {
            mSnapUtils.drawSnapToShape(canvas);
        }
    }

    protected void setSnapCanDrawShapeIfEnabled(boolean canDrawShape) {
        if (mSnappingEnabled) {
            mSnapUtils.setCanDraw(canDrawShape);
        }
    }

    /**
     * Lets the tool know that it is just switched from annotation tool.
     */
    protected void setJustCreatedFromAnotherTool() {
        mJustSwitchedFromAnotherTool = true;
    }

    /**
     * Clears the target point.
     */
    public void clearTargetPoint() {
        if (getToolMode() == ToolMode.STAMPER) {
            try {
                ((Stamper) this).clearTargetPoint();
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        } else if (getToolMode() == ToolMode.FILE_ATTACHMENT_CREATE) {
            try {
                ((FileAttachmentCreate) this).clearTargetPoint();
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
    }

    /**
     * Closes the quick menu
     */
    public void closeQuickMenu() {
        if (mQuickMenu != null && mQuickMenu.isShowing()) {
            mQuickMenu.dismiss();
        }
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onDrawEdgeEffects(Canvas, int, int)}.
     */
    @Override
    public boolean onDrawEdgeEffects(Canvas canvas, int width, int verticalOffset) {
        boolean needsInvalidate = false;

        if (!mEdgeEffectLeft.isFinished()) {
            canvas.save();
            try {
                canvas.translate(0, canvas.getHeight() + verticalOffset);
                canvas.rotate(-90, 0, 0);
                mEdgeEffectLeft.setSize(canvas.getHeight(), canvas.getWidth());
                if (mEdgeEffectLeft.draw(canvas)) {
                    needsInvalidate = true;
                }
            } finally {
                canvas.restore();
            }
        }

        if (!mEdgeEffectRight.isFinished()) {
            canvas.save();
            try {
                canvas.translate(width, verticalOffset);
                canvas.rotate(90, 0, 0);
                mEdgeEffectRight.setSize(canvas.getHeight(), canvas.getWidth());
                if (mEdgeEffectRight.draw(canvas)) {
                    needsInvalidate = true;
                }
            } finally {
                canvas.restore();
            }
        }
        return needsInvalidate;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onReleaseEdgeEffects()}.
     */
    @Override
    public void onReleaseEdgeEffects() {
        mEdgeEffectLeft.onRelease();
        mEdgeEffectRight.onRelease();
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onPullEdgeEffects(int, float)}.
     */
    @Override
    public void onPullEdgeEffects(int whichEdge, float deltaDistance) {
        if (whichEdge < 0) {
            // left
            mEdgeEffectLeft.onPull(deltaDistance);
        } else if (whichEdge > 0) {
            // right
            mEdgeEffectRight.onPull(deltaDistance);
        }
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onDoubleTapZoomAnimationBegin()}.
     */
    @Override
    public void onDoubleTapZoomAnimationBegin() {
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onDoubleTapZoomAnimationEnd()}.
     */
    @Override
    public void onDoubleTapZoomAnimationEnd() {
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onNightModeUpdated(boolean)}.
     */
    @Override
    public void onNightModeUpdated(boolean isNightMode) {
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onRenderingFinished()}.
     */
    @Override
    public void onRenderingFinished() {
        if (mAnnotView != null && mAnnotView.isDelayViewRemoval()) {
            mAnnotView.removeView();
            mPdfViewCtrl.removeView(mAnnotView);
            mAnnotView = null;
        }
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onAnnotPainterUpdated(int, long, CurvePainter)}.
     */
    @Override
    public void onAnnotPainterUpdated(int page, long which, CurvePainter painter) {
        if (mAnnotView != null && mAnnotView.getCurvePainterId() == which) {
            mAnnotView.setCurvePainter(which, painter);
        }
    }

    /**
     * Called when a menu in quick menu has been clicked.
     *
     * @param menuItem The clicked menu item.
     * @return True if handled
     */
    public boolean onQuickMenuClicked(QuickMenuItem menuItem) {
        mNextToolMode = getToolMode();
        return false;
    }

    /**
     * Checks whether the specified point is inside the quick menu layout.
     *
     * @param x X coordinates
     * @param y Y coordinates
     * @return True if it is inside quick menu, false otherwise
     */
    public boolean isInsideQuickMenu(float x, float y) {
        return isQuickMenuShown() && x > mQuickMenu.getLeft() && x < mQuickMenu.getRight() && y < mQuickMenu.getBottom() && y > mQuickMenu.getTop();
    }

    /**
     * check if quick menu is showing
     *
     * @return true if quick menu is visible
     */
    public boolean isQuickMenuShown() {
        return mQuickMenu != null && mQuickMenu.isShowing();
    }

    /**
     * Update quick menu note text based on if the note has contents
     *
     * @param note the content of note
     */
    public void updateQuickMenuNoteText(String note) {
        if (!isQuickMenuShown()) {
            return;
        }
        QuickMenuItem menuItem = (QuickMenuItem) mQuickMenu.getMenu().findItem(R.id.qm_note);
        if (menuItem != null) {
            // update note text
            if (note != null && !note.equals("")) {
                menuItem.setTitle(R.string.tools_qm_view_note);
            } else {
                menuItem.setTitle(R.string.tools_qm_add_note);
            }
        }
    }

    /**
     * Update quick menu appearance item color
     *
     * @param color
     */
    public void updateQuickMenuStyleColor(int color) {
        if (color == Color.TRANSPARENT || mQuickMenu == null) {
            // ignore transparent color
            return;
        }
        QuickMenuItem menuItem = (QuickMenuItem) mQuickMenu.getMenu().findItem(R.id.qm_appearance);
        if (menuItem != null) {
            menuItem.setColor(color);
        }
    }

    /**
     * @param opacity
     */
    public void updateQuickMenuStyleOpacity(float opacity) {
        if (mQuickMenu == null) {
            // ignore transparent color
            return;
        }
        QuickMenuItem menuItem = (QuickMenuItem) mQuickMenu.getMenu().findItem(R.id.qm_appearance);
        if (menuItem != null) {
            menuItem.setOpacity(opacity);
        }
    }

    /**
     * Setup annotation properties.
     *
     * @param color           The color
     * @param opacity         The opacity
     * @param thickness       The thickness
     * @param fillColor       The color for filling
     * @param icon            The icon
     * @param pdfTronFontName The PDFTron font name
     */
    public void setupAnnotProperty(int color, float opacity, float thickness, int fillColor, String icon, String pdfTronFontName) {

    }

    /**
     * Setup annotation properties.
     *
     * @param color           The color
     * @param opacity         The opacity
     * @param thickness       The thickness
     * @param fillColor       The color for filling
     * @param icon            The icon
     * @param pdfTronFontName The PDFTron font name
     * @param textColor       The text color
     * @param textSize        The text size
     */
    public void setupAnnotProperty(int color, float opacity, float thickness, int fillColor, String icon, String pdfTronFontName, @ColorInt int textColor, float textSize) {
        setupAnnotProperty(color, opacity, thickness, fillColor, icon, pdfTronFontName);
    }

    /**
     * Setup annotation properties.
     *
     * @param style The annot style
     */
    public void setupAnnotProperty(AnnotStyle annotStyle) {
        int color = annotStyle.getColor();
        int fill = annotStyle.getFillColor();
        float thickness = annotStyle.getThickness();
        float opacity = annotStyle.getOpacity();
        String icon = annotStyle.getIcon();
        String pdftronFontName = annotStyle.getPDFTronFontName();
        float textSize = annotStyle.getTextSize();
        int textColor = annotStyle.getTextColor();

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        toolManager.setSnappingEnabledForMeasurementTools(annotStyle.getSnap());

        setupAnnotProperty(color, opacity, thickness, fill, icon, pdftronFontName, textColor, textSize);
    }

    public void setupAnnotStyles(@NonNull ArrayList<AnnotStyle> annotStyles) {
        if (annotStyles.size() == 1) {
            setupAnnotProperty(annotStyles.get(0));
        }
    }

    /**
     * Specifies the mode that should forcefully remains.
     *
     * @param mode
     */
    public void setForceSameNextToolMode(boolean mode) {
        mForceSameNextToolMode = mode;
    }

    /**
     * Gets whether the mode that should forcefully remains.
     *
     * @return
     */
    public boolean isForceSameNextToolMode() {
        return mForceSameNextToolMode;
    }

    /**
     * @return True if the annotation is editing
     */
    public boolean isEditingAnnot() {
        try {
            if (getToolMode() == ToolMode.TEXT_CREATE || getToolMode() == ToolMode.CALLOUT_CREATE) {
                return ((FreeTextCreate) this).isFreeTextEditing();
            } else if (getToolMode() == ToolMode.ANNOT_EDIT) {
                return ((AnnotEdit) this).isFreeTextEditing();
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        return false;
    }

    /**
     * Sets the visiblity of page number indicator.
     *
     * @param visible True if visibla
     */
    public void setPageNumberIndicatorVisible(boolean visible) {
        mPageNumberIndicatorVisible = visible;
    }

    /**
     * Uses a helper to set the next tool mode.
     *
     * @param nextToolMode The next tool mode
     */
    public void setNextToolModeHelper(ToolMode nextToolMode) {
        if (mForceSameNextToolMode) {
            mNextToolMode = getToolMode();
        } else {
            mNextToolMode = nextToolMode;
        }
    }

    /**
     * Uses a helper to set current default tool mode.
     * This flag is used for a creation tool to go into editing tool then go back to the original creation tool.
     *
     * @param defaultToolMode The current default tool mode
     */
    public void setCurrentDefaultToolModeHelper(ToolManager.ToolModeBase defaultToolMode) {
        if (mForceSameNextToolMode) {
            mCurrentDefaultToolMode = defaultToolMode;
        } else {
            mCurrentDefaultToolMode = ToolMode.PAN;
        }
    }

    /**
     * @return The current default tool mode
     */
    public ToolManager.ToolModeBase getCurrentDefaultToolMode() {
        return mCurrentDefaultToolMode;
    }

    public void backToDefaultTool() {
        if (mCurrentDefaultToolMode != null) {
            mNextToolMode = mCurrentDefaultToolMode;
        }
    }

    public boolean hasAnnotSelected() {
        return false;
    }

    /**
     * Checks whether should skip on up based on the prior event
     *
     * @param priorEventMode The prior event
     * @return
     */
    protected boolean skipOnUpPriorEvent(PDFViewCtrl.PriorEventMode priorEventMode) {
        return priorEventMode == PDFViewCtrl.PriorEventMode.FLING
                || priorEventMode == PDFViewCtrl.PriorEventMode.SCROLLING;
    }

    /**
     * Shows transient page number.
     */
    protected void showTransientPageNumber() {
        ((ToolManager) mPdfViewCtrl.getToolManager()).showBuiltInPageNumber();
        mPageNumberRemovalHandler.removeMessages(1);
        mPageNumberRemovalHandler.sendEmptyMessageDelayed(1, 3000);
    }

    /**
     * Builds the bounding box of the annotation.
     */
    protected void buildAnnotBBox() throws PDFNetException {
        if (isValidAnnot(mAnnot)) {
            mAnnotBBox.set(0, 0, 0, 0);
            try {
                com.pdftron.pdf.Rect r = mAnnot.getVisibleContentBox();
                mAnnotBBox.set((float) r.getX1(), (float) r.getY1(), (float) r.getX2(), (float) r.getY2());
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
    }

    /**
     * Checks if given screen point is inside selected annotaiton
     *
     * @param e        The motion point
     * @param screen_y y coordinates in screen pt
     * @return true if inside, false otherwise
     */
    protected boolean isInsideAnnot(MotionEvent e) {
        double x = e.getX();
        double y = e.getY();
        if (mAnnot != null && mAnnotPageNum == mPdfViewCtrl.getPageNumberFromScreenPt(x, y)) {
            double[] pts = mPdfViewCtrl.convScreenPtToPagePt(x, y, mAnnotPageNum);
            if (mAnnotBBox.contains((float) pts[0], (float) pts[1])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets annotation rectangle in screen pt
     *
     * @return annnotation rectangle in screen pt.
     */
    protected RectF getAnnotRect() {
        if (mAnnot != null && mAnnotPageNum > 0) {
            double[] pts1 = mPdfViewCtrl.convPagePtToScreenPt(mAnnotBBox.left, mAnnotBBox.bottom, mAnnotPageNum);
            double[] pts2 = mPdfViewCtrl.convPagePtToScreenPt(mAnnotBBox.right, mAnnotBBox.top, mAnnotPageNum);
            float left = (float) (pts1[0] < pts2[0] ? pts1[0] : pts2[0]);
            float right = (float) (pts1[0] > pts2[0] ? pts1[0] : pts2[0]);
            float top = (float) (pts1[1] < pts2[1] ? pts1[1] : pts2[1]);
            float bottom = (float) (pts1[1] > pts2[1] ? pts1[1] : pts2[1]);
            return new RectF(left, top, right, bottom);
        } else {
            return null;
        }
    }

    protected android.graphics.Rect getRectFromRectF(@Nullable RectF rectF) {
        if (rectF == null) {
            return null;
        }
        android.graphics.Rect rect = new android.graphics.Rect();
        rectF.round(rect);
        return rect;
    }

    /**
     * Gets annotation rectangle in canvas pt.
     *
     * @return annotation rectangle in canvas pt.
     */
    protected RectF getAnnotCanvasRect() {
        if (mAnnot != null) {
            double[] pts1 = mPdfViewCtrl.convPagePtToCanvasPt(mAnnotBBox.left, mAnnotBBox.bottom, mAnnotPageNum);
            double[] pts2 = mPdfViewCtrl.convPagePtToCanvasPt(mAnnotBBox.right, mAnnotBBox.top, mAnnotPageNum);
            float left = (float) (pts1[0] < pts2[0] ? pts1[0] : pts2[0]);
            float right = (float) (pts1[0] > pts2[0] ? pts1[0] : pts2[0]);
            float top = (float) (pts1[1] < pts2[1] ? pts1[1] : pts2[1]);
            float bottom = (float) (pts1[1] > pts2[1] ? pts1[1] : pts2[1]);
            return new RectF(left, top, right, bottom);
        } else {
            return null;
        }
    }

    /**
     * Called when doen with two finger scrolling.
     */
    protected void doneTwoFingerScrolling() {
        mAllowTwoFingerScroll = false;
    }

    /**
     * Called when done with one finger scrolling with stylus.
     */
    protected void doneOneFingerScrollingWithStylus() {
        mAllowOneFingerScrollWithStylus = false;
    }

    /**
     * Shows the quick menu.
     */
    public boolean showMenu(RectF anchor_rect) {
        return showMenu(anchor_rect, createQuickMenu());
    }

    /**
     * Gets quick menu type for analytics
     *
     * @return quick menu type
     */
    protected @AnalyticsHandlerAdapter.QuickMenuType
    int getQuickMenuAnalyticType() {
        return AnalyticsHandlerAdapter.QUICK_MENU_TYPE_ANNOTATION_SELECT;
    }

    /**
     * Shows the quick menu with given menu.
     */
    public boolean showMenu(RectF anchor_rect, QuickMenu quickMenu) {
        if (anchor_rect == null) {
            return false;
        }
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();

        if (mQuickMenu != null) {
            if (mQuickMenu.isShowing() && mAnnot != null && mQuickMenu.getAnnot() != null) {
                if (mAnnot.equals(mQuickMenu.getAnnot())) {
                    return false;
                }
            }
            closeQuickMenu();
            mQuickMenu = null;
        }

        RectF client_r = new RectF(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
        if (!client_r.intersect(anchor_rect)) {
            return false;
        }

        toolManager.setQuickMenuJustClosed(false);

        mQuickMenu = quickMenu;

        mQuickMenu.setAnchorRect(anchor_rect);
        mQuickMenu.setAnnot(mAnnot);

        mQuickMenu.setOnDismissListener(new QuickMenuDismissListener());

        mQuickMenu.show(getQuickMenuAnalyticType());

        return true;
    }

    /**
     * Calculates quick menu anchor
     *
     * @param anchorRect The anchor rectangle
     * @return The rectangle
     */
    protected RectF calculateQMAnchor(RectF anchorRect) {
        if (anchorRect != null) {
            int left = (int) anchorRect.left;
            int top = (int) anchorRect.top;
            int right = (int) anchorRect.right;
            int bottom = (int) anchorRect.bottom;

            try {
                // normalize the rect
                com.pdftron.pdf.Rect rect = new com.pdftron.pdf.Rect((double) anchorRect.left, (double) anchorRect.top, (double) anchorRect.right, (double) anchorRect.bottom);
                rect.normalize();
                left = (int) rect.getX1();
                top = (int) rect.getY1();
                right = (int) rect.getX2();
                bottom = (int) rect.getY2();
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }

            int[] location = new int[2];
            mPdfViewCtrl.getLocationInWindow(location);

            int atop = top + location[1];
            int aleft = left + location[0];
            int aright = right + location[0];
            int abottom = bottom + location[1];

            RectF qmAnchor = new RectF(aleft, atop, aright, abottom);
            return qmAnchor;
        }
        return null;
    }

    /**
     * Selects the specified annotation.
     *
     * @param annot   The annotaion
     * @param pageNum The page number where the annotaion is on
     */
    public void selectAnnot(Annot annot, int pageNum) {
        // Since find text locks the document, cancel it to release the document.
        mPdfViewCtrl.cancelFindText();
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            if (isValidAnnot(annot)) {
                setAnnot(annot, pageNum);
                buildAnnotBBox();
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }

        // since new annotation is selected
        // let's run through create again
        onCreate();
    }

    /**
     * Check whether an annotation is part of a group annotation,
     * proceed to select the group instead.
     *
     * @return null if no such group found,
     * otherwise returns the {@link Pair} of {@link ToolMode} and the annotation group
     * that can then be used to select the group
     */
    public static Pair<ToolMode, ArrayList<Annot>> canSelectGroupAnnot(PDFViewCtrl pdfViewCtrl, Annot annot, int pageNum) {
        if (null == annot) {
            return null;
        }
        try {
            // check if it is a group
            ArrayList<Annot> annotsInGroup = AnnotUtils.getAnnotationsInGroup(pdfViewCtrl, annot, pageNum);
            if (annotsInGroup != null && annotsInGroup.size() > 1) {
                // this is a group of annotations
                return new Pair<>(ToolMode.ANNOT_EDIT_RECT_GROUP, annotsInGroup);
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
        return null;
    }

    /**
     * Converts density independent pixels to physical pixels.
     */
    protected float convDp2Pix(float dp) {
        return Utils.convDp2Pix(mPdfViewCtrl.getContext(), dp);
    }

    /**
     * Converts physical pixels to density independent pixels.
     */
    protected float convPix2Dp(float pix) {
        return Utils.convPix2Dp(mPdfViewCtrl.getContext(), pix);
    }

    /**
     * Gets a rectangle to use when selecting text.
     */
    public RectF getTextSelectRect(float x, float y) {
        float delta = 0.5f;
        float x2 = x + delta;
        float y2 = y + delta;
        delta *= 2;
        float x1 = x2 - delta >= 0 ? x2 - delta : 0;
        float y1 = y2 - delta >= 0 ? y2 - delta : 0;

        return new RectF(x1, y1, x2, y2);
    }

    /**
     * Returns string from resource ID
     *
     * @param id The resource ID
     * @return The string from resource ID
     */
    protected String getStringFromResId(@StringRes int id) {
        return mPdfViewCtrl.getResources().getString(id);
    }

    /**
     * Sets the author
     *
     * @param annot The markup annotation
     */
    protected void setAuthor(Markup annot) {
        final Context context = mPdfViewCtrl.getContext();
        if (context == null) {
            return;
        }
        try {
            boolean generateUID = annot.getUniqueID() == null;
            if (annot.getUniqueID() != null) {
                String uid = annot.getUniqueID().getAsPDFText();
                if (Utils.isNullOrEmpty(uid)) {
                    generateUID = true;
                }
            }
            if (generateUID) {
                setUniqueID(annot);
            }
        } catch (PDFNetException e) {
            e.printStackTrace();
        }
        if (mPdfViewCtrl.getToolManager() instanceof ToolManager) {
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            if (toolManager.getAuthorId() != null) {
                setAuthor(annot, toolManager.getAuthorId());
                return;
            }
        }

        boolean authorNameHasBeenAsked = PdfViewCtrlSettingsManager.getAuthorNameHasBeenAsked(context);
        String authorName = PdfViewCtrlSettingsManager.getAuthorName(context);
        if (!authorNameHasBeenAsked && authorName.isEmpty()) {
            // Show dialog to get the author name.
            boolean askAuthor = false;
            if (mPdfViewCtrl.getToolManager() instanceof ToolManager) {
                if (((ToolManager) mPdfViewCtrl.getToolManager()).isShowAuthorDialog()) {
                    askAuthor = true;
                }
            }

            mMarkupToAuthor = annot;

            String possibleName = "";
            // If the author name in the preferences is empty, we try to get
            // the name of the current user in the device.
            int res = context.checkCallingOrSelfPermission("android.permission.GET_ACCOUNTS");
            if (res == PackageManager.PERMISSION_GRANTED) {
                Pattern emailPattern = Patterns.EMAIL_ADDRESS;
                Account[] accounts = AccountManager.get(context).getAccounts();
                for (Account account : accounts) {
                    if (emailPattern.matcher(account.name).matches()) {
                        possibleName = account.name;
                        break;
                    }
                }
            }

            PdfViewCtrlSettingsManager.setAuthorNameHasBeenAsked(context);

            if (askAuthor) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View authorNameDialog = inflater.inflate(R.layout.tools_dialog_author_name, null);
                final EditText authorNameEditText = (EditText) authorNameDialog.findViewById(R.id.tools_dialog_author_name_edittext);
                authorNameEditText.setText(possibleName);
                authorNameEditText.selectAll();

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                final AlertDialog authorDialog = builder.setView(authorNameDialog)
                        .setTitle(R.string.tools_dialog_author_name_title)
                        .setPositiveButton(R.string.ok, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String author = authorNameEditText.getText().toString().trim();
                                // Set author information on the markup
                                setAuthor(mMarkupToAuthor, author);
                                // Update preferences with the new name
                                PdfViewCtrlSettingsManager.updateAuthorName(context, author);
                            }
                        })
                        .setNegativeButton(R.string.tools_misc_skip, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).create();
                authorDialog.show();
                if (authorNameEditText.getText().length() == 0) {
                    // empty, don't allow OK
                    authorDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                } else {
                    authorDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
                authorNameEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (authorDialog != null) {
                            if (s.length() == 0) {
                                // empty, don't allow OK
                                authorDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                            } else {
                                authorDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            }
                        }
                    }
                });
            } else {
                // Set author information on the markup
                setAuthor(mMarkupToAuthor, possibleName);
                // Update preferences with the new name
                PdfViewCtrlSettingsManager.updateAuthorName(context, possibleName);
            }
        } else {
            // Use author name in the preferences
            String author = PdfViewCtrlSettingsManager.getAuthorName(context);
            setAuthor(annot, author);
        }
    }

    private void setAuthor(Markup annot, String author) {
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            annot.setTitle(author);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    /**
     * set unique id to annotation
     *
     * @param annot mark up annotation
     */
    protected void setUniqueID(Markup annot) {
        boolean shouldUnlock = false;
        try {
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            String key = toolManager.generateKey();
            if (key != null) {
                mPdfViewCtrl.docLock(true);
                shouldUnlock = true;
                annot.setUniqueID(key);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    /**
     * set annotation date to now
     *
     * @param annot annotation
     */
    protected void setDateToNow(Annot annot) {
        AnnotUtils.setDateToNow(mPdfViewCtrl, annot);
    }

    // Helper functions to obtain the storage key string for each of the annotation property

    /**
     * Returns the storage key string for color
     *
     * @param annotType The annotation type
     * @return The storage key string
     */
    protected String getColorKey(int annotType) {
        return ToolStyleConfig.getInstance().getColorKey(annotType, "");

//        String defaultResult = PREF_ANNOTATION_CREATION_LINE +
//            PREF_ANNOTATION_CREATION_COLOR;
//        if (!(mode instanceof ToolMode)) {
//            return defaultResult;
//        }
//
//        switch ((ToolMode) mode) {
//            case TEXT_HIGHLIGHT:
//                return PREF_ANNOTATION_CREATION_HIGHLIGHT +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case TEXT_UNDERLINE:
//                return PREF_ANNOTATION_CREATION_UNDERLINE +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case RECT_LINK:
//            case TEXT_LINK_CREATE:
//                return PREF_ANNOTATION_CREATION_LINK +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case TEXT_STRIKEOUT:
//                return PREF_ANNOTATION_CREATION_STRIKEOUT +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case TEXT_SQUIGGLY:
//                return PREF_ANNOTATION_CREATION_SQUIGGLY +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case TEXT_CREATE:
//                return PREF_ANNOTATION_CREATION_FREETEXT +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case INK_CREATE:
//                return PREF_ANNOTATION_CREATION_FREEHAND +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case RECT_CREATE:
//                return PREF_ANNOTATION_CREATION_RECTANGLE +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case OVAL_CREATE:
//                return PREF_ANNOTATION_CREATION_OVAL +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case POLYGON_CREATE:
//                return PREF_ANNOTATION_CREATION_POLYGON +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case CLOUD_CREATE:
//                return PREF_ANNOTATION_CREATION_CLOUD +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case TEXT_ANNOT_CREATE:
//                return PREF_ANNOTATION_CREATION_NOTE +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case SIGNATURE:
//                return PREF_ANNOTATION_CREATION_SIGNATURE +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case ARROW_CREATE:
//                return PREF_ANNOTATION_CREATION_ARROW +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case POLYLINE_CREATE:
//                return PREF_ANNOTATION_CREATION_POLYLINE +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case FREE_HIGHLIGHTER:
//                return PREF_ANNOTATION_CREATION_FREE_HIGHLIGHTER +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            default:
//                return defaultResult;
//        }
    }

    /**
     * Returns the storage key string for text color
     *
     * @return the storage key string for text color
     */
    protected String getTextColorKey(int annotType) {
        return ToolStyleConfig.getInstance().getTextColorKey(annotType, "");

//        return PREF_ANNOTATION_CREATION_FREETEXT +
//            PREF_ANNOTATION_CREATION_TEXT_COLOR;
    }

    protected String getTextSizeKey(int annotType) {
        return ToolStyleConfig.getInstance().getTextSizeKey(annotType, "");

//        return PREF_ANNOTATION_CREATION_FREETEXT +
//            PREF_ANNOTATION_CREATION_TEXT_SIZE;
    }

    protected String getDateFormatKey(int annotType) {
        return ToolStyleConfig.getInstance().getDateFormatKey(annotType, "");
    }

    protected String getBorderStyleKey(int annotType) {
        return ToolStyleConfig.getInstance().getBorderStyleKey(annotType, "");
    }

    protected String getLineStyleKey(int annotType) {
        return ToolStyleConfig.getInstance().getLineStyleKey(annotType, "");
    }

    protected String getLineStartStyleKey(int annotType) {
        return ToolStyleConfig.getInstance().getLineStartStyleKey(annotType, "");
    }

    protected String getLineEndStyleKey(int annotType) {
        return ToolStyleConfig.getInstance().getLineEndStyleKey(annotType, "");
    }

    /**
     * Returns the storage key string for thickness
     *
     * @param annotType The annotation type
     * @return The storage key string
     */
    protected String getThicknessKey(int annotType) {
        return ToolStyleConfig.getInstance().getThicknessKey(annotType, "");

//        String defaultResult = PREF_ANNOTATION_CREATION_LINE +
//            PREF_ANNOTATION_CREATION_THICKNESS;
//        if (!(mode instanceof ToolMode)) {
//            return defaultResult;
//        }
//        switch ((ToolMode) mode) {
//            case TEXT_UNDERLINE:
//                return PREF_ANNOTATION_CREATION_UNDERLINE +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case RECT_LINK:
//            case TEXT_LINK_CREATE:
//                return PREF_ANNOTATION_CREATION_LINK +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case TEXT_STRIKEOUT:
//                return PREF_ANNOTATION_CREATION_STRIKEOUT +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case TEXT_SQUIGGLY:
//                return PREF_ANNOTATION_CREATION_SQUIGGLY +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case TEXT_CREATE:
//                return PREF_ANNOTATION_CREATION_FREETEXT +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case INK_CREATE:
//                return PREF_ANNOTATION_CREATION_FREEHAND +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case RECT_CREATE:
//                return PREF_ANNOTATION_CREATION_RECTANGLE +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case OVAL_CREATE:
//                return PREF_ANNOTATION_CREATION_OVAL +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case POLYGON_CREATE:
//                return PREF_ANNOTATION_CREATION_POLYGON +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case CLOUD_CREATE:
//                return PREF_ANNOTATION_CREATION_CLOUD +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case INK_ERASER:
//                return PREF_ANNOTATION_CREATION_ERASER +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case SIGNATURE:
//                return PREF_ANNOTATION_CREATION_SIGNATURE +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case ARROW_CREATE:
//                return PREF_ANNOTATION_CREATION_ARROW +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case POLYLINE_CREATE:
//                return PREF_ANNOTATION_CREATION_POLYLINE +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case FREE_HIGHLIGHTER:
//                return PREF_ANNOTATION_CREATION_FREE_HIGHLIGHTER +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            default:
//                return defaultResult;
//        }
    }

    /**
     * Returns the storage key string for opacity
     *
     * @param annotType The annotation type
     * @return The storage key string
     */
    protected String getOpacityKey(int annotType) {
        return ToolStyleConfig.getInstance().getOpacityKey(annotType, "");

//        String defaultResult = PREF_ANNOTATION_CREATION_LINE +
//            PREF_ANNOTATION_CREATION_OPACITY;
//        if (!(mode instanceof ToolMode)) {
//            return defaultResult;
//        }
//        switch ((ToolMode) mode) {
//            case TEXT_HIGHLIGHT:
//                return PREF_ANNOTATION_CREATION_HIGHLIGHT +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case TEXT_UNDERLINE:
//                return PREF_ANNOTATION_CREATION_UNDERLINE +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case RECT_LINK:
//            case TEXT_LINK_CREATE:
//                return PREF_ANNOTATION_CREATION_LINK +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case TEXT_STRIKEOUT:
//                return PREF_ANNOTATION_CREATION_STRIKEOUT +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case TEXT_SQUIGGLY:
//                return PREF_ANNOTATION_CREATION_SQUIGGLY +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case TEXT_CREATE:
//                return PREF_ANNOTATION_CREATION_FREETEXT +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case INK_CREATE:
//                return PREF_ANNOTATION_CREATION_FREEHAND +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case RECT_CREATE:
//                return PREF_ANNOTATION_CREATION_RECTANGLE +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case OVAL_CREATE:
//                return PREF_ANNOTATION_CREATION_OVAL +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case POLYGON_CREATE:
//                return PREF_ANNOTATION_CREATION_POLYGON +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case CLOUD_CREATE:
//                return PREF_ANNOTATION_CREATION_CLOUD +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case TEXT_ANNOT_CREATE:
//                return PREF_ANNOTATION_CREATION_NOTE +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case ARROW_CREATE:
//                return PREF_ANNOTATION_CREATION_ARROW +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case POLYLINE_CREATE:
//                return PREF_ANNOTATION_CREATION_POLYLINE +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case FREE_HIGHLIGHTER:
//                return PREF_ANNOTATION_CREATION_FREE_HIGHLIGHTER +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            default:
//                return defaultResult;
//        }
    }

    /**
     * Returns the storage key string for color fill
     *
     * @param annotType The annotation type
     * @return The storage key string
     */
    protected String getColorFillKey(int annotType) {
        return ToolStyleConfig.getInstance().getFillColorKey(annotType, "");

//        String defaultResult = PREF_ANNOTATION_CREATION_FREEHAND +
//            PREF_ANNOTATION_CREATION_FILL_COLOR;
//        if (!(mode instanceof ToolMode)) {
//            return defaultResult;
//        }
//        switch ((ToolMode) mode) {
//            case TEXT_CREATE:
//                return PREF_ANNOTATION_CREATION_FREETEXT +
//                    PREF_ANNOTATION_CREATION_FILL_COLOR;
//            case RECT_CREATE:
//                return PREF_ANNOTATION_CREATION_RECTANGLE +
//                    PREF_ANNOTATION_CREATION_FILL_COLOR;
//            case OVAL_CREATE:
//                return PREF_ANNOTATION_CREATION_OVAL +
//                    PREF_ANNOTATION_CREATION_FILL_COLOR;
//            case POLYGON_CREATE:
//                return PREF_ANNOTATION_CREATION_POLYGON +
//                    PREF_ANNOTATION_CREATION_FILL_COLOR;
//            case CLOUD_CREATE:
//                return PREF_ANNOTATION_CREATION_CLOUD +
//                    PREF_ANNOTATION_CREATION_FILL_COLOR;
//            default:
//                return defaultResult;
//        }
    }

    /**
     * Returns the storage key string for icon
     *
     * @param annotType The annotation type
     * @return The storage key string
     */
    protected String getIconKey(int annotType) {
        return ToolStyleConfig.getInstance().getIconKey(annotType, "");

//        String defaultResult = PREF_ANNOTATION_CREATION_NOTE +
//            PREF_ANNOTATION_CREATION_ICON;
//        if (!(mode instanceof ToolMode)) {
//            return defaultResult;
//        }
//        switch ((ToolMode) mode) {
//            case TEXT_ANNOT_CREATE:
//                return PREF_ANNOTATION_CREATION_NOTE +
//                    PREF_ANNOTATION_CREATION_ICON;
//            default:
//                return defaultResult;
//        }
    }

    /**
     * Returns the storage key string for font
     *
     * @param annotType The annotation type
     * @return The storage key string
     */
    public static String getFontKey(int annotType) {
        return ToolStyleConfig.getInstance().getFontKey(annotType, "");

//        String defaultResult = PREF_ANNOTATION_CREATION_FREETEXT +
//            PREF_ANNOTATION_CREATION_FONT;
//        if (!(mode instanceof ToolMode)) {
//            return defaultResult;
//        }
//        switch ((ToolMode) mode) {
//            case TEXT_CREATE:
//                return PREF_ANNOTATION_CREATION_FREETEXT +
//                    PREF_ANNOTATION_CREATION_FONT;
//            default:
//                return defaultResult;
//        }
    }

    /**
     * Returns the storage key string for ruler base unit
     *
     * @return The storage key string
     */
    protected String getRulerBaseUnitKey(int annotType) {
        return ToolStyleConfig.getInstance().getRulerBaseUnitKey(annotType, "");
    }

    /**
     * Returns the storage key string for ruler translate unit
     *
     * @return The storage key string
     */
    protected String getRulerTranslateUnitKey(int annotType) {
        return ToolStyleConfig.getInstance().getRulerTranslateUnitKey(annotType, "");
    }

    /**
     * Returns the storage key string for ruler base value
     *
     * @return The storage key string
     */
    protected String getRulerBaseValueKey(int annotType) {
        return ToolStyleConfig.getInstance().getRulerBaseValueKey(annotType, "");
    }

    /**
     * Returns the storage key string for ruler translate value
     *
     * @return The storage key string
     */
    protected String getRulerTranslateValueKey(int annotType) {
        return ToolStyleConfig.getInstance().getRulerTranslateValueKey(annotType, "");
    }

    /**
     * Returns the storage key string for eraser type
     *
     * @return The storage key string
     */
    protected String getEraserTypeKey(int annotType) {
        return ToolStyleConfig.getInstance().getEraserTypeKey(annotType, "");
    }

    /**
     * Returns the storage key string for ink eraser mode
     *
     * @return The storage key string
     */
    protected String getInkEraserModeKey(int annotType) {
        return ToolStyleConfig.getInstance().getInkEraserModeKey(annotType, "");
    }

    /**
     * Returns the storage key string for text markup type
     *
     * @return The storage key string
     */
    protected String getTextMarkupTypeKey(int annotType) {
        return ToolStyleConfig.getInstance().getTextMarkupTypeKey(annotType, "");
    }

    /**
     * Returns the storage key string for horizontal text alignment
     *
     * @param annotType The annotation type
     * @return The storage key string
     */
    public static String getHorizontalAlignmentKey(int annotType) {
        return ToolStyleConfig.getInstance().getHorizontalAlignmentKey(annotType, "");
    }

    /**
     * Returns the storage key string for vertical text alignment
     *
     * @param annotType The annotation type
     * @return The storage key string
     */
    public static String getVerticalAlignmentKey(int annotType) {
        return ToolStyleConfig.getInstance().getVerticalAlignmentKey(annotType, "");
    }

    /**
     * get tool mode from annotation type
     *
     * @param annot annotation
     * @return tool mode
     */
    protected static ToolMode getModeFromAnnotType(Annot annot) {
        ToolMode mode = ToolMode.LINE_CREATE;
        if (annot != null) {
            try {
                int annotType = annot.getType();
                switch (annotType) {
                    case Annot.e_Line:
                        if (AnnotUtils.isRuler(annot)) {
                            return ToolMode.RULER_CREATE;
                        } else if (AnnotUtils.isArrow(annot)) {
                            return ToolMode.ARROW_CREATE;
                        }
                        return ToolMode.LINE_CREATE;
                    case Annot.e_Polyline:
                        if (AnnotUtils.isPerimeterMeasure(annot)) {
                            return ToolMode.PERIMETER_MEASURE_CREATE;
                        }
                        return ToolMode.POLYLINE_CREATE;
                    case Annot.e_Square:
                        return ToolMode.RECT_CREATE;
                    case Annot.e_Circle:
                        return ToolMode.OVAL_CREATE;
                    case Annot.e_Sound:
                        return ToolMode.SOUND_CREATE;
                    case Annot.e_FileAttachment:
                        return ToolMode.FILE_ATTACHMENT_CREATE;
                    case Annot.e_Polygon:
                        if (AnnotUtils.isCloud(annot)) {
                            return ToolMode.CLOUD_CREATE;
                        } else if (AnnotUtils.isAreaMeasure(annot)) {
                            if (AnnotUtils.isRectAreaMeasure(annot)) {
                                return ToolMode.RECT_AREA_MEASURE_CREATE;
                            }
                            return ToolMode.AREA_MEASURE_CREATE;
                        }
                        return ToolMode.POLYGON_CREATE;
                    case Annot.e_Highlight:
                        return ToolMode.TEXT_HIGHLIGHT;
                    case Annot.e_Underline:
                        return ToolMode.TEXT_UNDERLINE;
                    case Annot.e_StrikeOut:
                        return ToolMode.TEXT_STRIKEOUT;
                    case Annot.e_Squiggly:
                        return ToolMode.TEXT_SQUIGGLY;
                    case Annot.e_FreeText:
                        if (AnnotUtils.isCallout(annot)) {
                            return ToolMode.CALLOUT_CREATE;
                        }
                        return ToolMode.TEXT_CREATE;
                    case Annot.e_Ink:
                        if (AnnotUtils.isFreeHighlighter(annot)) {
                            return ToolMode.FREE_HIGHLIGHTER;
                        }
                        return ToolMode.INK_CREATE;
                    case Annot.e_Text:
                        return ToolMode.TEXT_ANNOT_CREATE;
                    case Annot.e_Link:
                        return ToolMode.RECT_LINK;
                    default:
                        return ToolMode.LINE_CREATE;
                }
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
        return mode;
    }

    /**
     * Called when an annotation is added.
     *
     * @param annot The added annotation
     * @param page  The page where the annotation is on
     */
    protected void raiseAnnotationAddedEvent(Annot annot, int page) {
        if (annot == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("Annot is null"));
            return;
        }
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        HashMap<Annot, Integer> annots = new HashMap<>(1);
        annots.put(annot, page);
        toolManager.raiseAnnotationsAddedEvent(annots);
    }

    /**
     * Called when annotation are added.
     *
     * @param annots The map of added annotations (pairs of annotation and the page number
     *               where the annotation is on)
     */
    protected void raiseAnnotationAddedEvent(Map<Annot, Integer> annots) {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        toolManager.raiseAnnotationsAddedEvent(annots);
    }

    /**
     * Called right before an annotation is modified.
     *
     * @param annot The annotation to be modified
     * @param page  The page where the annotation is on
     */
    protected void raiseAnnotationPreModifyEvent(Annot annot, int page) {
        if (annot == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("Annot is null"));
            return;
        }
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        HashMap<Annot, Integer> annots = new HashMap<>(1);
        annots.put(annot, page);
        toolManager.raiseAnnotationsPreModifyEvent(annots);
    }

    /**
     * Called when annotation are going to be modified.
     *
     * @param annots The map of annotations to be modified (pairs of annotation and the page number
     *               where the annotation is on)
     */
    protected void raiseAnnotationPreModifyEvent(Map<Annot, Integer> annots) {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        toolManager.raiseAnnotationsPreModifyEvent(annots);
    }

    /**
     * Called when an annotation is modified.
     *
     * @param annot The modified annotation
     * @param page  The page where the annotation is on
     */
    protected void raiseAnnotationModifiedEvent(Annot annot, int page) {
        if (annot == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("Annot is null"));
            return;
        }
        setDateToNow(annot);
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        HashMap<Annot, Integer> annots = new HashMap<>(1);
        annots.put(annot, page);
        toolManager.raiseAnnotationsModifiedEvent(annots, getAnnotationModificationBundle(null));
    }

    protected void raiseAnnotationModifiedEvent(Annot annot, int page, Bundle bundle) {
        if (annot == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("Annot is null"));
            return;
        }
        setDateToNow(annot);
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        HashMap<Annot, Integer> annots = new HashMap<>(1);
        annots.put(annot, page);
        toolManager.raiseAnnotationsModifiedEvent(annots, getAnnotationModificationBundle(bundle));
    }

    /**
     * Called when annotation are modified.
     *
     * @param annots The map of modified annotations (pairs of annotation and the page number
     *               where the annotation is on)
     */
    protected void raiseAnnotationModifiedEvent(Map<Annot, Integer> annots) {
        for (Map.Entry<Annot, Integer> entry : annots.entrySet()) {
            setDateToNow(entry.getKey());
        }
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        toolManager.raiseAnnotationsModifiedEvent(annots, getAnnotationModificationBundle(null));
    }

    /**
     * Called right before an annotation is removed.
     *
     * @param annot The annotation to removed
     * @param page  The page where the annotation is on
     */
    protected void raiseAnnotationPreRemoveEvent(Annot annot, int page) {
        if (annot == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("Annot is null"));
            return;
        }
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        HashMap<Annot, Integer> annots = new HashMap<>(1);
        annots.put(annot, page);
        toolManager.raiseAnnotationsPreRemoveEvent(annots);
    }

    /**
     * Called when annotation are going to be removed.
     *
     * @param annots The map of annotations to be removed (pairs of annotation and the page number
     *               where the annotation is on)
     */
    protected void raiseAnnotationPreRemoveEvent(Map<Annot, Integer> annots) {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        toolManager.raiseAnnotationsPreRemoveEvent(annots);
    }

    /**
     * Called when an annotation is removed.
     *
     * @param annot  The removed annotation
     * @param page   The page where the annotation is on
     * @param bundle The Bundle containing additional metadata for the event
     */
    protected void raiseAnnotationRemovedEvent(Annot annot, int page) {
        raiseAnnotationRemovedEvent(annot, page, null);
    }

    /**
     * Called when an annotation is removed.
     *
     * @param annot  The removed annotation
     * @param page   The page where the annotation is on
     * @param bundle The Bundle containing additional metadata for the event
     */
    protected void raiseAnnotationRemovedEvent(Annot annot, int page, @Nullable Bundle bundle) {
        if (annot == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("Annot is null"));
            return;
        }
        HashMap<Annot, Integer> annots = new HashMap<>(1);
        annots.put(annot, page);
        raiseAnnotationRemovedEvent(annots, bundle);
    }

    /**
     * Called when annotation are removed.
     *
     * @param annots The map of removed annotations (pairs of annotation and the page number
     *               where the annotation is on)
     * @param bundle The Bundle containing additional metadata for the event
     */
    protected void raiseAnnotationRemovedEvent(Map<Annot, Integer> annots, @Nullable Bundle bundle) {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        toolManager.raiseAnnotationsRemovedEvent(annots, bundle);
    }

    /**
     * Called when annotation are removed.
     *
     * @param annots The map of removed annotations (pairs of annotation and the page number
     *               where the annotation is on)
     */
    protected void raiseAnnotationRemovedEvent(Map<Annot, Integer> annots) {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        toolManager.raiseAnnotationsRemovedEvent(annots);
    }

    /**
     * Called when an annotations action has been taken place.
     */
    protected void raiseAnnotationActionEvent() {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        toolManager.raiseAnnotationActionEvent();
    }

    /**
     * Handle annotation
     *
     * @param annot annotation
     * @return true then intercept the subclass function, false otherwise
     */
    protected final boolean onInterceptAnnotationHandling(@Nullable Annot annot) {
        Bundle bundle = getAnnotationModificationBundle(null);
        return onInterceptAnnotationHandling(annot, bundle);
    }

    /**
     * Handle annotation
     *
     * @param annot  annotation which is pending to be handled
     * @param bundle information going to pass to {@link ToolManager}
     * @return true then intercept the subclass function, false otherwise
     */
    protected final boolean onInterceptAnnotationHandling(@Nullable Annot annot, @NonNull Bundle bundle) {
        bundle = getAnnotationModificationBundle(bundle);
        bundle.putInt(PAGE_NUMBER, mAnnotPageNum);
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        return toolManager.raiseInterceptAnnotationHandlingEvent(annot, bundle, ToolManager.getDefaultToolMode(getToolMode()));
    }

    /**
     * Handle annotation
     *
     * @param annot  annotation which is pending to be handled
     * @param bundle information going to pass to {@link ToolManager}
     * @return true then intercept the subclass function, false otherwise
     */
    protected final boolean onInterceptAnnotationHandling(@Nullable PDFViewCtrl.LinkInfo linkInfo, int pageNum) {
        Bundle bundle = new Bundle();
        bundle.putInt(PAGE_NUMBER, pageNum);
        if (linkInfo != null) {
            bundle.putBoolean(IS_LINK, true);
            bundle.putString(LINK_URL, linkInfo.getURL());
            com.pdftron.pdf.Rect rect = linkInfo.getRect();
            try {
                android.graphics.RectF rectF = AnnotUtils.getScreenRectFromPageRect(mPdfViewCtrl, rect, pageNum);
                bundle.putParcelable(LINK_RECTF, rectF);
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            }
        }
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        return toolManager.raiseInterceptAnnotationHandlingEvent(new Link(), bundle, ToolManager.getDefaultToolMode(getToolMode()));
    }

    /**
     * Call this function when a dialog is about to show up.
     *
     * @deprecated see {@link #onInterceptDialogFragmentEvent(DialogFragment)}
     */
    @Deprecated
    protected boolean onInterceptDialogEvent(AlertDialog dialog) {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        return toolManager.raiseInterceptDialogEvent(dialog);
    }

    /**
     * Call this function when a dialog fragment is about to show up.
     */
    protected boolean onInterceptDialogFragmentEvent(DialogFragment dialogFragment) {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        return toolManager.raiseInterceptDialogFragmentEvent(dialogFragment);
    }

    /**
     * Adds old tools.
     */
    protected void addOldTools() {
        if (!mPdfViewCtrl.isAnnotationLayerEnabled()) {
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            if (null != toolManager) {
                toolManager.getOldTools().add(this);
            }
        }
    }

    /**
     * Converts from page rect to screen rect
     *
     * @param pageRect The page rect
     * @param page     The page number
     * @return The screen rect
     */
    protected com.pdftron.pdf.Rect convertFromPageRectToScreenRect(com.pdftron.pdf.Rect pageRect, int page) {
        com.pdftron.pdf.Rect screenRect = null;

        if (pageRect != null) {
            try {
                float sx = mPdfViewCtrl.getScrollX();
                float sy = mPdfViewCtrl.getScrollY();

                float x1, y1, x2, y2;

                double[] pts1 = mPdfViewCtrl.convPagePtToScreenPt(pageRect.getX1(), pageRect.getY1(), page);
                double[] pts2 = mPdfViewCtrl.convPagePtToScreenPt(pageRect.getX2(), pageRect.getY2(), page);

                x1 = (float) pts1[0] + sx;
                y1 = (float) pts1[1] + sy;
                x2 = (float) pts2[0] + sx;
                y2 = (float) pts2[1] + sy;

                screenRect = new com.pdftron.pdf.Rect(x1, y1, x2, y2);
            } catch (PDFNetException ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            }
        }

        return screenRect;
    }

    /**
     * Sets the annotataion
     *
     * @param annot The annotation
     * @param page  The page number
     */
    protected void setAnnot(Annot annot, int pageNum) {
        mAnnot = annot;
        mAnnotPageNum = pageNum;
        try {
            if (mPdfViewCtrl.getToolManager() instanceof ToolManager) {
                ((ToolManager) mPdfViewCtrl.getToolManager()).setSelectedAnnot(mAnnot, mAnnotPageNum);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * Returns the annotation associated with this tool. This annotation should only be read with a read lock
     * and should not be modified.
     */
    @Nullable
    public Annot getAnnot() {
        return mAnnot;
    }

    /**
     * Unsets the annotation.
     */
    protected void unsetAnnot() {
        removeAnnotView();
        mAnnot = null;
        try {
            if (mPdfViewCtrl.getToolManager() instanceof ToolManager) {
                ((ToolManager) mPdfViewCtrl.getToolManager()).setSelectedAnnot(null, -1);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    protected Annot didTapOnSameTypeAnnot(MotionEvent e) {
        // if tap on the same kind, select the annotation instead of create a new one
        int x = (int) e.getX();
        int y = (int) e.getY();
        ArrayList<Annot> annots = mPdfViewCtrl.getAnnotationListAt(x - 8, y - 8, x + 8, y + 8);
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        try {
            for (Annot annot : annots) {
                if (annot.isValid()) {
                    if (toolManager.isRestrictedTapAnnotCreation()) {
                        return annot;
                    } else if (AnnotUtils.getAnnotType(annot) == getCreateAnnotType()) {
                        return annot;
                    }
                }
            }
        } catch (PDFNetException ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }

        return null;
    }

    protected boolean addRotateHandle() {
        if (mAnnot == null) {
            return false;
        }
        if (mRotateHandle != null) {
            removeRotateHandle();
        }
        if (canAddRotateView(mAnnot)) {
            mRotateHandle = new RotateHandleView(mPdfViewCtrl.getContext());
            mPdfViewCtrl.addView(mRotateHandle);
        }
        return true;
    }

    /**
     * Adds real time annotation view
     *
     * @return true if a view is added, false otherwise
     */
    protected boolean addAnnotView() {
        if (mAnnot == null) {
            return false;
        }
        if (mAnnotView != null) {
            removeAnnotView(false);
        }
        try {
            if (null == mAnnotStyle) {
                boolean shouldUnlockRead = false;
                try {
                    mPdfViewCtrl.docLockRead();
                    shouldUnlockRead = true;
                    mAnnotStyle = AnnotUtils.getAnnotStyle(mAnnot);
                } finally {
                    if (shouldUnlockRead) {
                        mPdfViewCtrl.docUnlockRead();
                    }
                }
            }
            boolean canDraw = canAddAnnotView(mAnnot, mAnnotStyle);

            // rotation
            int finalRotation = AnnotUtils.getAnnotUIRotation(mPdfViewCtrl, mAnnot, mAnnotPageNum);

            mAnnotView = new AnnotView(mPdfViewCtrl.getContext());
            mAnnotView.setAnnotStyle(mPdfViewCtrl, mAnnotStyle);
            mAnnotView.setPage(mAnnotPageNum);
            mAnnotView.setAnnotUIRotation(finalRotation);
            mAnnotView.setAnnotRotation(AnnotUtils.getAnnotRotationRelToPage(mPdfViewCtrl, mAnnot, mAnnotPageNum));
            mAnnotView.setCanDraw(canDraw);
            mAnnotView.setCurvePainter(mAnnot.__GetHandle(), mPdfViewCtrl.getAnnotationPainter(mAnnotPageNum, mAnnot.__GetHandle()));
            // keep the annotation visible if cannot draw
            if (canDraw) {
                if (!AnnotUtils.canUseBitmapAppearance(mAnnot)) {
                    hideAnnot();
                }
                updateAnnotViewBitmap(true);
            }
            mAnnotView.setZoom(mPdfViewCtrl.getZoom());
            mAnnotView.setPageNum(mAnnotPageNum);
            mAnnotView.setHasPermission(mHasSelectionPermission);
            mPdfViewCtrl.addView(mAnnotView);
            return true;
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
        return false;
    }

    protected void removeRotateHandle() {
        if (mRotateHandle != null) {
            mRotateHandle.setListener(null);
            mPdfViewCtrl.removeView(mRotateHandle);
            mRotateHandle = null;
        }
    }

    protected void removeAnnotView() {
        removeAnnotView(!mPdfViewCtrl.isAnnotationLayerEnabled());
    }

    protected void removeAnnotView(boolean delayRemoval) {
        removeAnnotView(delayRemoval, true);
    }

    protected void removeAnnotView(boolean delayRemoval, boolean removeRotateView) {
        removeAnnotView(delayRemoval, removeRotateView, true);
    }

    protected void removeAnnotView(boolean delayRemoval, boolean removeRotateView, boolean showAnnotation) {
        if (removeRotateView) {
            removeRotateHandle();
        }
        mBitmapDisposable.clear();
        if (mAnnotView != null) {
            mAnnotView.setDelayViewRemoval(delayRemoval);
            mAnnotView.prepareRemoval();
            if (delayRemoval) {
                addOldTools();
            } else {
                mPdfViewCtrl.removeView(mAnnotView);
                mAnnotView = null;
            }

            if (mAnnot != null) {
                if (showAnnotation) {
                    mPdfViewCtrl.showAnnotation(mAnnot);
                    boolean shouldUnlockRead = false;
                    try {
                        if (!mPdfViewCtrl.isAnnotationLayerEnabled()) {
                            mPdfViewCtrl.docLockRead();
                            shouldUnlockRead = true;
                            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
                        }
                    } catch (Exception ex) {
                        AnalyticsHandlerAdapter.getInstance().sendException(ex);
                    } finally {
                        if (shouldUnlockRead) {
                            mPdfViewCtrl.docUnlockRead();
                        }
                    }
                }
            }
        }
    }

    protected boolean canAddAnnotView(Annot annot, AnnotStyle annotStyle) {
        return false;
    }

    protected boolean canAddRotateView(Annot annot) {
        return false;
    }

    void hideAnnot() throws PDFNetException {
        mPdfViewCtrl.hideAnnotation(mAnnot);
        boolean shouldUnlockRead = false;
        try {
            if (!mPdfViewCtrl.isAnnotationLayerEnabled()) {
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;
                mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
            }
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
    }

    void updateAnnotViewBitmap() {
        updateAnnotViewBitmap(false);
    }

    void updateAnnotViewBitmap(final boolean shouldHideAnnot) {
        if (mAnnotView != null && mAnnotView.getCanDraw() && !mPdfViewCtrl.isAnnotationLayerEnabled()) {
            mBitmapDisposable.add(AnnotUtils.getAnnotationAppearanceAsync(mPdfViewCtrl, mAnnot)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Bitmap>() {
                        @Override
                        public void accept(Bitmap bitmap) throws Exception {
                            if (shouldHideAnnot) {
                                hideAnnot();
                            }
                            if (mAnnotView != null) {
                                mAnnotView.setAnnotBitmap(bitmap);
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            // ignored
                        }
                    })
            );
        }
    }

    /**
     * Deletes the annotation.
     */
    protected void deleteAnnot() {
        if (mAnnot == null || mPdfViewCtrl == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "deleteAnnot");
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            mNextToolMode = mCurrentDefaultToolMode;
            raiseAnnotationPreRemoveEvent(mAnnot, mAnnotPageNum);
            Page page = mPdfViewCtrl.getDoc().getPage(mAnnotPageNum);
            mAnnot = AnnotUtils.safeDeleteAnnotAndUpdate(mPdfViewCtrl, page, mAnnot, mAnnotPageNum);

            // make sure to raise remove event after mPdfViewCtrl.update and before unsetAnnot
            raiseAnnotationRemovedEvent(mAnnot, mAnnotPageNum);
            if (sDebug) Log.d(TAG, "going to unsetAnnot: onQuickMenuclicked");

            // Special case for signatures stamps with widgets, where the
            // widget will not longer be read-only when stamp deleted
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            if (mAnnot.getType() == mAnnot.e_Stamp && toolManager != null && toolManager.isSignSignatureFieldsWithStamps()) {
                String fieldId = mAnnot.getCustomData(Signature.SIGNATURE_FIELD_ID);
                if (!Utils.isNullOrEmpty(fieldId)) {
                    ArrayList<Annot> annotationsOnPage = mPdfViewCtrl.getAnnotationsOnPage(mAnnotPageNum);
                    for (Annot annot : annotationsOnPage) {
                        if (annot.isValid() && annot.getType() == Annot.e_Widget) {
                            Widget widget = new Widget(annot);
                            Field field = widget.getField();
                            if (field != null && field.getType() == Field.e_signature && fieldId.equals(field.getName())) {
                                annot.setFlag(Annot.e_hidden, false);
                                mPdfViewCtrl.update(field);
                                break;
                            }
                        }
                    }
                }
            }

            unsetAnnot();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    /**
     * Flattens the annotation.
     */
    protected void flattenAnnot() {
        if (mAnnot == null || mPdfViewCtrl == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "flattenAnnot");
        bundle.putStringArray(KEYS, new String[]{FLATTENED});
        bundle.putBoolean(FLATTENED, true);

        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc information isn't thread
            // safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            raiseAnnotationPreRemoveEvent(mAnnot, mAnnotPageNum);

            mAnnot = AnnotUtils.flattenAnnot(mPdfViewCtrl, mAnnot, mAnnotPageNum);

            raiseAnnotationRemovedEvent(mAnnot, mAnnotPageNum, bundle);

            unsetAnnot();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    /**
     * Handles flattening the annotation.
     * Checks if the user does indeed want to flatten the annotation.
     */
    protected void handleFlattenAnnot() {
        SharedPreferences settings = getToolPreferences(mPdfViewCtrl.getContext());
        if (settings.getBoolean(STAMP_SHOW_FLATTEN_WARNING, true)) {
            // show flatten alert dialog
            LayoutInflater inflater = LayoutInflater.from(mPdfViewCtrl.getContext());
            View customLayout = inflater.inflate(R.layout.alert_dialog_with_checkbox, null);
            String text = mPdfViewCtrl.getContext().getResources().getString(R.string.tools_dialog_flatten_dialog_msg);
            final TextView dialogTextView = customLayout.findViewById(R.id.dialog_message);
            dialogTextView.setText(text);
            final CheckBox dialogCheckBox = customLayout.findViewById(R.id.dialog_checkbox);
            dialogCheckBox.setChecked(true);

            final long annotImpl = mAnnot != null ? mAnnot.__GetHandle() : 0;
            final int pageNum = mAnnotPageNum;

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mPdfViewCtrl.getContext())
                    .setView(customLayout)
                    .setTitle(mPdfViewCtrl.getContext().getResources().getString(R.string.tools_dialog_flatten_dialog_title))
                    .setPositiveButton(R.string.tools_qm_flatten, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            boolean showAgain = !dialogCheckBox.isChecked();
                            SharedPreferences settings = getToolPreferences(mPdfViewCtrl.getContext());
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putBoolean(STAMP_SHOW_FLATTEN_WARNING, showAgain);
                            editor.apply();

                            mAnnot = Annot.__Create(annotImpl, mPdfViewCtrl.getDoc());
                            mAnnotPageNum = pageNum;

                            // flatten
                            flattenAnnot();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            boolean showAgain = !dialogCheckBox.isChecked();
                            SharedPreferences settings = getToolPreferences(mPdfViewCtrl.getContext());
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putBoolean(STAMP_SHOW_FLATTEN_WARNING, showAgain);
                            editor.apply();

                            // show quick menu
                            showMenu(getAnnotRect());
                        }
                    });

            dialogBuilder.create().show();
        } else {
            // skip flatten alert dialog
            // flatten
            flattenAnnot();
        }
    }

    protected boolean isAnnotSupportEdit(Annot annot) throws PDFNetException {
        if (isValidAnnot(annot)) {
            if (annot.getType() == Annot.e_Widget || annot.getType() == Annot.e_Link) {
                return isMadeByPDFTron(annot);
            }
            return true;
        }
        return false;
    }

    /**
     * @hide
     */
    protected final boolean isMadeByPDFTron(Annot annot) throws PDFNetException {
        return AnnotUtils.isMadeByPDFTron(annot);
    }

    protected void initializeSnapToNearest() {
        // The API needs to be called once to initialize.
        mPdfViewCtrl.snapToNearestInDoc(0, 0);
    }

    public void setSnappingEnabled(boolean enabled) {
        mSnappingEnabled = enabled;
        if (enabled) {
            initializeSnapToNearest();
        }
    }

    public boolean getSnappingEnabled() {
        return mSnappingEnabled;
    }

    protected PointF snapToNearestIfEnabled(PointF point) {
        if (mSnappingEnabled) {
            return mSnapUtils.snapToNearest(point.x, point.y);
        } else {
            return point;
        }
    }

    public static void setDebug(boolean debug) {
        sDebug = debug;
    }

    public static Bundle getAnnotationModificationBundle(Bundle bundle) {
        if (bundle == null) {
            bundle = new Bundle();
        }
        return bundle;
    }

    public static SharedPreferences getToolPreferences(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_FILE_NAME, 0);
    }

    /**
     * Returns the PDFTron font name associated with the given font name.
     *
     * @param context  used to get shared preferences.
     * @param fontName font name mapped to a PDFTron font name.
     * @return the PDFTron font name, or empty string if not available.
     * @throws JSONException
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public static String findPDFTronFontName(@NonNull Context context, @NonNull String fontName) throws JSONException {
        String pdftronFontName = "";
        SharedPreferences settings = Tool.getToolPreferences(context);
        String fontInfo = settings.getString(ANNOTATION_FREE_TEXT_FONTS, "");
        if (!Utils.isNullOrEmpty(fontInfo)) {
            JSONObject systemFontObject = new JSONObject(fontInfo);
            JSONArray systemFontArray = systemFontObject.getJSONArray(ANNOTATION_FREE_TEXT_JSON_FONT);

            for (int i = 0; i < systemFontArray.length(); i++) {
                // check if font is selected in settings
                JSONObject fontJson = systemFontArray.getJSONObject(i);
                if (fontJson.has(ANNOTATION_FREE_TEXT_JSON_FONT_NAME)) {
                    String fontNameCompare = fontJson.getString(ANNOTATION_FREE_TEXT_JSON_FONT_NAME);
                    if (fontName.equals(fontNameCompare)) {
                        pdftronFontName = fontJson.getString(ANNOTATION_FREE_TEXT_JSON_FONT_PDFTRON_NAME);
                        break;
                    }
                }
            }
        }

        return pdftronFontName;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void updateFontMap(@NonNull Context context, int annotType, @NonNull String pdfFontName, @NonNull String fontName) {
        SharedPreferences settings = Tool.getToolPreferences(context);
        String fontInfo = settings.getString(Tool.ANNOTATION_FREE_TEXT_FONTS, "");
        try {
            if (!Utils.isNullOrEmpty(fontInfo)) {
                JSONObject systemFontObject = new JSONObject(fontInfo);
                JSONArray systemFontArray = systemFontObject.getJSONArray(Tool.ANNOTATION_FREE_TEXT_JSON_FONT);

                for (int i = 0; i < systemFontArray.length(); i++) {
                    JSONObject fontObj = systemFontArray.getJSONObject(i);
                    // if has the same file name as the selected font, save the font name
                    if (fontObj.getString(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_PDFTRON_NAME).equals(pdfFontName)) {
                        fontObj.put(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_NAME, fontName);
                        break;
                    }
                }
                fontInfo = systemFontObject.toString();
            }
        } catch (JSONException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(Tool.ANNOTATION_FREE_TEXT_FONTS, fontInfo);
        editor.putString(Tool.getFontKey(annotType), pdfFontName);
        editor.apply();
    }

    /**
     * Updates font of widget. A write lock is expected around this method.
     */
    public static void updateFont(@NonNull PDFViewCtrl pdfViewCtrl, @NonNull Widget widget, String contents) throws PDFNetException, JSONException {
        Font font = widget.getFont();
        String fontName = font.getName();

        // find a fontName and pdftronFontName match if possible
        String pdftronFontName = Tool.findPDFTronFontName(pdfViewCtrl.getContext(), fontName);
        ToolManager toolManager = (ToolManager) pdfViewCtrl.getToolManager();
        if (!Utils.isNullOrEmpty(pdftronFontName) && toolManager.isFontLoaded()) {
            Font newFont = Font.create(pdfViewCtrl.getDoc(), pdftronFontName, contents);
            widget.setFont(newFont);
            widget.refreshAppearance();
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    void setBundle(@Nullable Bundle bundle) {
        mBundle = bundle;
    }

    @Nullable
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Bundle getBundle() {
        return mBundle;
    }

    protected void showWidgetChoiceDialog(long widget, int page,
            boolean isSingleChoice, boolean isCombo, @Nullable String[] options) {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        FragmentActivity activity = toolManager.getCurrentActivity();
        if (activity == null) {
            Log.e(Signature.class.getName(), "ToolManager is not attached to with an Activity");
            return;
        }

        ChoiceViewModel viewModel = ViewModelProviders.of(activity).get(ChoiceViewModel.class);
        viewModel.observeOnComplete(activity, new Observer<Event<ChoiceResult>>() {
            @Override
            public void onChanged(@Nullable Event<ChoiceResult> resultEvent) {
                if (resultEvent != null && !resultEvent.hasBeenHandled()) {
                    ChoiceResult result = resultEvent.getContentIfNotHandled();
                    if (result != null) {
                        String[] options = result.getOptions();
                        if (null == options) {
                            return;
                        }
                        boolean shouldUnlock = false;
                        Annot annot = null;
                        int pageNum = 0;
                        try {
                            mPdfViewCtrl.docLock(true);
                            shouldUnlock = true;

                            annot = Annot.__Create(result.getWidget(), mPdfViewCtrl.getDoc());
                            pageNum = result.getPage();
                            boolean singleChoice = result.isSingleChoice();
                            if (isValidAnnot(annot)) {
                                raiseAnnotationPreModifyEvent(annot, pageNum);

                                Widget widget = new Widget(annot);

                                // update multiselect flag
                                Field field = widget.getField();
                                field.setFlag(Field.e_multiselect, !singleChoice);

                                boolean isCombo = field.getFlag(Field.e_combo);
                                if (isCombo) {
                                    ComboBoxWidget combo = new ComboBoxWidget(annot);
                                    combo.replaceOptions(options);
                                } else {
                                    ListBoxWidget list = new ListBoxWidget(annot);
                                    list.replaceOptions(options);
                                }

                                // update font
                                String allOptions = Arrays.toString(options);
                                updateFont(mPdfViewCtrl, widget, allOptions);

                                // force font to show for combo box
                                if (field.getFlag(Field.e_combo) && options.length > 0) {
                                    ComboBoxWidget combo = new ComboBoxWidget(annot);
                                    String selected = combo.getSelectedOption();
                                    if (Utils.isNullOrEmpty(selected) || !Arrays.asList(options).contains(selected)) {
                                        combo.setSelectedOption(options[0]);
                                    }
                                }

                                widget.refreshAppearance();
                                mPdfViewCtrl.update(annot, pageNum);

                                raiseAnnotationModifiedEvent(annot, pageNum);
                            }
                        } catch (Exception ex) {
                            AnalyticsHandlerAdapter.getInstance().sendException(ex);
                        } finally {
                            if (shouldUnlock) {
                                mPdfViewCtrl.docUnlock();
                            }
                        }
                        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                        if (annot != null && toolManager.isAutoSelectAnnotation()) {
                            toolManager.selectAnnot(annot, pageNum);
                        }
                    }
                }
            }
        });

        // show options dialog
        ChoiceDialogFragment fragment = ChoiceDialogFragment.newInstance(widget, page, isSingleChoice, isCombo, options);
        fragment.setStyle(DialogFragment.STYLE_NORMAL, toolManager.getTheme());
        fragment.show(activity.getSupportFragmentManager(), ChoiceDialogFragment.TAG);
    }

    protected boolean isValidAnnot(Annot annot) throws PDFNetException {
        return annot != null && annot.isValid();
    }

    /**
     * @return The old annotation position in screen space
     * @throws PDFNetException PDFNet exception
     */
    protected Rect getOldAnnotScreenPosition() throws PDFNetException {
        return AnnotUtils.getOldAnnotScreenPosition(mPdfViewCtrl, mAnnot, mAnnotPageNum);
    }

    private static class PageNumberRemovalHandler extends Handler {
        private final WeakReference<Tool> mTool;

        public PageNumberRemovalHandler(Tool tool) {
            mTool = new WeakReference<Tool>(tool);
        }

        @Override
        public void handleMessage(Message msg) {
            Tool tool = mTool.get();
            if (tool != null) {
                ToolManager toolManager = (ToolManager) tool.mPdfViewCtrl.getToolManager();
                toolManager.hideBuiltInPageNumber();
            }
        }
    }

    private class QuickMenuDismissListener implements QuickMenu.OnDismissListener {

        @Override
        public void onDismiss() {
            if (mPdfViewCtrl.getToolManager() instanceof ToolManager) {
                ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                toolManager.setQuickMenuJustClosed(true);
                toolManager.onQuickMenuDismissed();
            }
            // When dismissed, trigger the menu-clicked call-back function.
            if (mQuickMenu != null) {
                QuickMenuItem selectedMenuItem = mQuickMenu.getSelectedMenuItem();
                if (selectedMenuItem != null) {
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.CATEGORY_QUICKTOOL, selectedMenuItem.getText(), getModeAHLabel());
                    ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                    toolManager.onQuickMenuClicked(selectedMenuItem);
                }
            }
        }
    }
}
