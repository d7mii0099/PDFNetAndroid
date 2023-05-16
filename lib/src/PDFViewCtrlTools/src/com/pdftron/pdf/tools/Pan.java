//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.Field;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.annots.Widget;
import com.pdftron.pdf.config.ToolConfig;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.ActionUtils;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.AnnotationClipboardHelper;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.SelectionLoupe;

import java.util.ArrayList;

import static com.pdftron.pdf.tools.BaseTool.LOUPE_TYPE_TEXT;

/**
 * Pan tool implements the following functions:
 * <ol>
 * <li>Select the hit annotation and switch to annotation edit tool on single tap event;</li>
 * <li>Bring up annotation creation menu upon long press event.</li>
 * </ol>
 */
@SuppressWarnings("WeakerAccess")
@Keep
public class Pan extends Tool {
    private Paint mPaint;
    boolean mSuppressSingleTapConfirmed;
    private QuickMenuItem mPasteMenuEntry;

    private ToolMode mDefaultStylusToolMode;
    private PointF mTargetPoint;
    private RectF mAnchor;
    private boolean mPresetMode = false;

    /**
     * Class constructor
     */
    public Pan(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mSuppressSingleTapConfirmed = false;
        mDefaultStylusToolMode = PdfViewCtrlSettingsManager.getDefaultStylusToolMode(mPdfViewCtrl.getContext());
        // Enable page turning (in non-continuous page presentation mode).
        // It is only turned on in Pan tool.
        mPdfViewCtrl.setBuiltInPageSlidingState(true);
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.PAN;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Unknown;
    }

    /**
     * The overload implementation of {@link Tool#onCreate()}.
     */
    @Override
    public void onCreate() {
        mPasteMenuEntry = new QuickMenuItem(mPdfViewCtrl.getContext(), R.id.qm_paste, QuickMenuItem.FIRST_ROW_MENU);
        mPasteMenuEntry.setTitle(R.string.tools_qm_paste);
    }

    @Override
    protected QuickMenu createQuickMenu() {
        QuickMenu quickMenu = super.createQuickMenu();
        quickMenu.inflate(R.menu.pan);

        if (AnnotationClipboardHelper.isItemCopied(mPdfViewCtrl.getContext()) &&
                !((ToolManager) mPdfViewCtrl.getToolManager()).isReadOnly()) {
            QuickMenuItem menuItem = (QuickMenuItem) quickMenu.getMenu().add(R.id.qm_first_row_group, R.id.qm_paste, QuickMenuItem.ORDER_START, R.string.tools_qm_paste);
            menuItem.setIcon(R.drawable.ic_content_paste_black_24dp);
        }
        quickMenu.addMenuEntries(QM_MAX_ROW_SIZE);
        quickMenu.setDividerVisibility(View.INVISIBLE);
        return quickMenu;
    }

    /**
     * The overload implementation of {@link Tool#onDown(MotionEvent)}.
     */
    @Override
    public boolean onDown(MotionEvent e) {
        super.onDown(e);
        SelectionLoupe selectionLoupe = ((ToolManager) mPdfViewCtrl.getToolManager()).getSelectionLoupe(LOUPE_TYPE_TEXT);
        if (selectionLoupe != null) {
            selectionLoupe.dismiss();
        }
        boolean stylusAsPen = ((ToolManager) mPdfViewCtrl.getToolManager()).isStylusAsPen();
        if (stylusAsPen && e.getPointerCount() == 1 && e.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
            if (e.getButtonState() == MotionEvent.BUTTON_STYLUS_PRIMARY) { // stylus as pen mode
                mNextToolMode = ToolMode.INK_ERASER;
            } else { // stylus as pen mode with eraser button pressed
                mNextToolMode = mDefaultStylusToolMode;
            }
        }
        if (mNextToolMode == ToolMode.PAN && ShortcutHelper.isTextSelect(e)) {
            int x = (int) (e.getX() + 0.5);
            int y = (int) (e.getY() + 0.5);
            if (!Utils.isNougat() || mPdfViewCtrl.isThereTextInRect(x - 1, y - 1, x + 1, y + 1)) {
                mNextToolMode = ToolMode.TEXT_SELECT;
            }
        }

        return false;
    }

    /**
     * The overload implementation of {@link Tool#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        super.onMove(e1, e2, x_dist, y_dist);
        mJustSwitchedFromAnotherTool = false;
        return false;
    }

    /**
     * The overload implementation of {@link Tool#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        super.onUp(e, priorEventMode);
        mJustSwitchedFromAnotherTool = false;
        return false;
    }

    /**
     * The overload implementation of {@link Tool#onSingleTapConfirmed(MotionEvent)}.
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        super.onSingleTapConfirmed(e);
        showTransientPageNumber();

        if (mSuppressSingleTapConfirmed) {
            mSuppressSingleTapConfirmed = false;
            mJustSwitchedFromAnotherTool = false;
            return false;
        }

        int x = (int) (e.getX() + 0.5);
        int y = (int) (e.getY() + 0.5);
        selectAnnot(x, y);

        boolean isRTReply = false;
        try {
            isRTReply = AnnotUtils.hasReplyTypeReply(mAnnot);
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }

        if (mAnnot != null && !isRTReply) {
            boolean isLink = false;
            boolean shouldUnlockRead = false;
            try {
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;

                isLink = mAnnot.getType() == Annot.e_Link;

                mNextToolMode = safeSetNextToolMode(ToolConfig.getInstance().getAnnotationHandlerToolMode(AnnotUtils.getAnnotType(mAnnot)));
                mAnnotPageNum = mPdfViewCtrl.getPageNumberFromScreenPt(x, y);
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            } finally {
                if (shouldUnlockRead) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }
            if (!isLink) {
                Pair<ToolMode, ArrayList<Annot>> pair = canSelectGroupAnnot(mPdfViewCtrl, mAnnot, mAnnotPageNum);
                if (pair != null && pair.first != null) {
                    mNextToolMode = pair.first;
                    mAnnot = null;
                    mGroupAnnots = pair.second;
                }
            }
        } else {
            mNextToolMode = ToolMode.PAN;

            // If PDFViewCtrl.setUrlExtraction is enabled, do the test for a possible link here.
            try {
                PDFViewCtrl.LinkInfo linkInfo = mPdfViewCtrl.getLinkAt(x, y);
                if (linkInfo != null) {
                    int page = mPdfViewCtrl.getPageNumberFromScreenPt(x, y);
                    if (onInterceptAnnotationHandling(linkInfo, page)) {
                        return true;
                    }

                    String url = linkInfo.getURL();
                    if (url.startsWith("mailto:") || android.util.Patterns.EMAIL_ADDRESS.matcher(url).matches()) {
                        if (url.startsWith("mailto:")) {
                            url = url.substring(7);
                        }
                        ActionUtils.launchEmailIntent(mPdfViewCtrl.getContext(), url);
                    } else if (url.startsWith("tel:") || android.util.Patterns.PHONE.matcher(url).matches()) {
                        // this is a phone intent
                        if (url.startsWith("tel:")) {
                            url = url.substring(4);
                        }
                        ActionUtils.launchPhoneIntent(mPdfViewCtrl.getContext(), url);
                    } else {
                        // ACTION_VIEW needs the address to have http or https
                        if (!url.startsWith("https://") && !url.startsWith("http://")) {
                            url = "http://" + url;
                        }
                        ActionUtils.launchWebPageIntent(mPdfViewCtrl.getContext(), url);
                    }
                }
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            }
        }
        mPdfViewCtrl.invalidate();

        mJustSwitchedFromAnotherTool = false;
        return false;
    }

    /**
     * The overload implementation of {@link Tool#onLayout(boolean, int, int, int, int)}.
     */
    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && isQuickMenuShown() && mAnnot == null) {
            closeQuickMenu();
        }
    }

    /**
     * The overload implementation of {@link Tool#onLongPress(MotionEvent)}.
     */
    @Override
    public boolean onLongPress(MotionEvent e) {
        if (mAvoidLongPressAttempt) {
            mAvoidLongPressAttempt = false;
            return false;
        }

        int x = (int) (e.getX() + 0.5);
        int y = (int) (e.getY() + 0.5);
        selectAnnot(x, y);

        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            boolean is_form = mAnnot != null && mAnnot.getType() == Annot.e_Widget;

            RectF textSelectRect = getTextSelectRect(e.getX(), e.getY());
            boolean isTextSelect = !is_form && mPdfViewCtrl.selectByRect(textSelectRect.left, textSelectRect.top, textSelectRect.right, textSelectRect.bottom);

            boolean isMadeByPDFTron = isMadeByPDFTron(mAnnot);

            // get next tool mode by long press callback
            ToolMode toolMode = ToolConfig.getInstance()
                    .getPanLongPressSwitchToolCallback()
                    .onPanLongPressSwitchTool(mAnnot, isMadeByPDFTron, isTextSelect);

            mNextToolMode = toolMode;

            if (mAnnot != null) {
                mAnnotPageNum = mPdfViewCtrl.getPageNumberFromScreenPt(x, y);
            }

            // if remain in Pan mode, show menu
            if (toolMode == ToolMode.PAN) {
                mSelectPageNum = mPdfViewCtrl.getPageNumberFromScreenPt(x, y);
                if (mSelectPageNum > 0) {
                    mAnchor = new RectF(x - 5, y, x + 5, y + 1);
                    mTargetPoint = new PointF(e.getX(), e.getY());
                    showMenu(mAnchor);
                }
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }

        mJustSwitchedFromAnotherTool = false;
        return false;
    }

    /**
     * @return The target point
     */
    @SuppressWarnings("unused")
    protected PointF getTargetPoint() {
        return mTargetPoint;
    }

    /**
     * The overload implementation of {@link Tool#onScaleBegin(float, float)}.
     */
    @Override
    public boolean onScaleBegin(float x, float y) {
        return super.onScaleBegin(x, y);
    }

    /**
     * The overload implementation of {@link Tool#onScaleEnd(float, float)}.
     */
    @Override
    public boolean onScaleEnd(float x, float y) {
        super.onScaleEnd(x, y);
        mJustSwitchedFromAnotherTool = false;
        return false;
    }

    /**
     * The overload implementation of {@link Tool#onDraw(Canvas, Matrix)}.
     */
    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        mPageNumPosAdjust = 0;
        super.onDraw(canvas, tfm);
    }

    /**
     * The overload implementation of {@link Tool#onQuickMenuClicked(QuickMenuItem)}.
     */
    @Override
    public boolean onQuickMenuClicked(QuickMenuItem menuItem) {
        if (super.onQuickMenuClicked(menuItem)) {
            return true;
        }

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        if (toolManager.isReadOnly()) {
            mNextToolMode = ToolMode.PAN;
            return true;
        }

        if (menuItem.getItemId() == R.id.qm_line) {
            mNextToolMode = ToolMode.LINE_CREATE;
            ToolManager.Tool tool = toolManager.createTool(mNextToolMode, this, mPresetMode);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_arrow) {
            mNextToolMode = ToolMode.ARROW_CREATE;
            ToolManager.Tool tool = toolManager.createTool(mNextToolMode, this, mPresetMode);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_ruler) {
            mNextToolMode = ToolMode.RULER_CREATE;
            ToolManager.Tool tool = toolManager.createTool(mNextToolMode, this, mPresetMode);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_perimeter_measure) {
            mNextToolMode = ToolMode.PERIMETER_MEASURE_CREATE;
            ToolManager.Tool tool = toolManager.createTool(mNextToolMode, this, mPresetMode);
            toolManager.setTool(tool);
            if (toolManager.isOpenEditToolbarFromPan()) {
                toolManager.onOpenEditToolbar(ToolMode.PERIMETER_MEASURE_CREATE);
            }
        } else if (menuItem.getItemId() == R.id.qm_area_measure) {
            mNextToolMode = ToolMode.AREA_MEASURE_CREATE;
            ToolManager.Tool tool = toolManager.createTool(mNextToolMode, this, mPresetMode);
            toolManager.setTool(tool);
            if (toolManager.isOpenEditToolbarFromPan()) {
                toolManager.onOpenEditToolbar(ToolMode.AREA_MEASURE_CREATE);
            }
        } else if (menuItem.getItemId() == R.id.qm_rect_area_measure) {
            mNextToolMode = ToolMode.RECT_AREA_MEASURE_CREATE;
            ToolManager.Tool tool = toolManager.createTool(mNextToolMode, this, mPresetMode);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_polyline) {
            mNextToolMode = ToolMode.POLYLINE_CREATE;
            ToolManager.Tool tool = toolManager.createTool(mNextToolMode, this, mPresetMode);
            toolManager.setTool(tool);
            if (toolManager.isOpenEditToolbarFromPan()) {
                toolManager.onOpenEditToolbar(ToolMode.POLYLINE_CREATE);
            }
        } else if (menuItem.getItemId() == R.id.qm_rectangle) {
            mNextToolMode = ToolMode.RECT_CREATE;
            ToolManager.Tool tool = toolManager.createTool(mNextToolMode, this, mPresetMode);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_oval) {
            mNextToolMode = ToolMode.OVAL_CREATE;
            ToolManager.Tool tool = toolManager.createTool(mNextToolMode, this, mPresetMode);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_sound) {
            if (null != mTargetPoint) {
                mNextToolMode = ToolMode.SOUND_CREATE;
                // instance check is required here because we may not have record permission
                ToolManager.Tool tool = toolManager.createTool(mNextToolMode, this, mPresetMode);
                if (tool instanceof SoundCreate) {
                    toolManager.setTool(tool);
                    ((SoundCreate) tool).setTargetPoint(mTargetPoint, true);
                }
            }
        } else if (menuItem.getItemId() == R.id.qm_file_attachment) {
            if (null != mTargetPoint) {
                mNextToolMode = ToolMode.FILE_ATTACHMENT_CREATE;
                FileAttachmentCreate tool = (FileAttachmentCreate) toolManager.createTool(mNextToolMode, this, mPresetMode);
                toolManager.setTool(tool);
                tool.setTargetPoint(mTargetPoint, true);
            }
        } else if (menuItem.getItemId() == R.id.qm_polygon) {
            mNextToolMode = ToolMode.POLYGON_CREATE;
            ToolManager.Tool tool = toolManager.createTool(mNextToolMode, this, mPresetMode);
            toolManager.setTool(tool);
            if (toolManager.isOpenEditToolbarFromPan()) {
                toolManager.onOpenEditToolbar(ToolMode.POLYGON_CREATE);
            }
        } else if (menuItem.getItemId() == R.id.qm_cloud) {
            mNextToolMode = ToolMode.CLOUD_CREATE;
            ToolManager.Tool tool = toolManager.createTool(mNextToolMode, this, mPresetMode);
            toolManager.setTool(tool);
            if (toolManager.isOpenEditToolbarFromPan()) {
                toolManager.onOpenEditToolbar(ToolMode.CLOUD_CREATE);
            }
        } else if (menuItem.getItemId() == R.id.qm_free_hand) {
            mNextToolMode = ToolMode.INK_CREATE;
            ToolManager.Tool tool = toolManager.createTool(mNextToolMode, this, mPresetMode);
            ((FreehandCreate) tool).setMultiStrokeMode(false);
            ((FreehandCreate) tool).setTimedModeEnabled(false);
            toolManager.setTool(tool);
            if (toolManager.isOpenEditToolbarFromPan()) {
                toolManager.onInkEditSelected(null, 0);
            }
        } else if (menuItem.getItemId() == R.id.qm_free_highlighter) {
            mNextToolMode = ToolMode.FREE_HIGHLIGHTER;
            ToolManager.Tool tool = toolManager.createTool(mNextToolMode, this, mPresetMode);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_free_text) {
            if (null != mTargetPoint) {
                mNextToolMode = ToolMode.TEXT_CREATE;
                FreeTextCreate freeTextTool = (FreeTextCreate) toolManager.createTool(mNextToolMode, this, mPresetMode);
                toolManager.setTool(freeTextTool);
                freeTextTool.initFreeText(mTargetPoint);
            }
        } else if (menuItem.getItemId() == R.id.qm_callout) {
            if (null != mTargetPoint) {
                mNextToolMode = ToolMode.CALLOUT_CREATE;
                CalloutCreate calloutCreate = (CalloutCreate) toolManager.createTool(mNextToolMode, this, mPresetMode);
                toolManager.setTool(calloutCreate);
                calloutCreate.initFreeText(mTargetPoint);
                AnnotEditAdvancedShape annotEdit = (AnnotEditAdvancedShape) toolManager.createTool(ToolMode.ANNOT_EDIT_ADVANCED_SHAPE, calloutCreate);
                toolManager.setTool(annotEdit);
                annotEdit.enterText();
                annotEdit.mNextToolMode = ToolMode.ANNOT_EDIT_ADVANCED_SHAPE;
            }
        } else if (menuItem.getItemId() == R.id.qm_sticky_note) {
            if (null != mTargetPoint) {
                mNextToolMode = ToolMode.TEXT_ANNOT_CREATE;
                StickyNoteCreate stickyNoteTool = (StickyNoteCreate) toolManager.createTool(mNextToolMode, this, mPresetMode);
                toolManager.setTool(stickyNoteTool);
                stickyNoteTool.setTargetPoint(mTargetPoint);
            }
        } else if (menuItem.getItemId() == R.id.qm_floating_sig) {
            if (null != mTargetPoint) {
                mNextToolMode = ToolMode.SIGNATURE;
                Signature signatureTool = (Signature) toolManager.createTool(mNextToolMode, this, mPresetMode);
                toolManager.setTool(signatureTool);
                signatureTool.setSignatureFilePath(null);
                signatureTool.setTargetPoint(mTargetPoint);
            }
        } else if (menuItem.getItemId() == R.id.qm_image_stamper) {
            if (null != mTargetPoint) {
                mNextToolMode = ToolMode.STAMPER;
                Stamper stamperTool = (Stamper) toolManager.createTool(mNextToolMode, this, mPresetMode);
                toolManager.setTool(stamperTool);
                stamperTool.setTargetPoint(mTargetPoint, true);
            }
        } else if (menuItem.getItemId() == R.id.qm_rubber_stamper) {
            if (null != mTargetPoint) {
                mNextToolMode = ToolMode.RUBBER_STAMPER;
                RubberStampCreate tool = (RubberStampCreate) toolManager.createTool(mNextToolMode, this, mPresetMode);
                toolManager.setTool(tool);
                tool.setStampName(null);
                tool.setTargetPoint(mTargetPoint, true);
            }
        } else if (menuItem.getItemId() == R.id.qm_paste) {
            if (null != mTargetPoint) {
                pasteAnnot(mTargetPoint);
            }
        } else if (menuItem.getItemId() == R.id.qm_rect_link) {
            mNextToolMode = ToolMode.RECT_LINK;
            ToolManager.Tool tool = toolManager.createTool(mNextToolMode, this, mPresetMode);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_ink_eraser) {
            mNextToolMode = ToolMode.INK_ERASER;
            ToolManager.Tool tool = toolManager.createTool(mNextToolMode, this, mPresetMode);
            toolManager.setTool(tool);
            if (toolManager.isOpenEditToolbarFromPan()) {
                toolManager.onOpenAnnotationToolbar(ToolMode.INK_ERASER);
            }
        } else if (menuItem.getItemId() == R.id.qm_form_text) {
            mNextToolMode = ToolMode.FORM_TEXT_FIELD_CREATE;
            TextFieldCreate tool = (TextFieldCreate) toolManager.createTool(mNextToolMode, this, mPresetMode);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_form_check_box) {
            mNextToolMode = ToolMode.FORM_CHECKBOX_CREATE;
            ToolManager.Tool tool = toolManager.createTool(mNextToolMode, this, mPresetMode);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_form_combo_box) {
            mNextToolMode = ToolMode.FORM_COMBO_BOX_CREATE;
            ComboBoxFieldCreate tool = (ComboBoxFieldCreate) toolManager.createTool(mNextToolMode, this, mPresetMode);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_form_list_box) {
            mNextToolMode = ToolMode.FORM_LIST_BOX_CREATE;
            ListBoxFieldCreate tool = (ListBoxFieldCreate) toolManager.createTool(mNextToolMode, this, mPresetMode);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_form_signature) {
            mNextToolMode = ToolMode.FORM_SIGNATURE_CREATE;
            ToolManager.Tool tool = toolManager.createTool(mNextToolMode, this, mPresetMode);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_form_radio_group) {
            mNextToolMode = ToolMode.FORM_RADIO_GROUP_CREATE;
            ToolManager.Tool tool = toolManager.createTool(mNextToolMode, this, mPresetMode);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_rect_group_select) {
            mNextToolMode = ToolMode.ANNOT_EDIT_RECT_GROUP;
            ToolManager.Tool tool = toolManager.createTool(mNextToolMode, this, mPresetMode);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_rect_redaction) {
            mNextToolMode = ToolMode.RECT_REDACTION;
            ToolManager.Tool tool = toolManager.createTool(mNextToolMode, this, mPresetMode);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_page_redaction) {
            toolManager.getRedactionManager().openPageRedactionDialog();
        } else if (menuItem.getItemId() == R.id.qm_search_redaction) {
            toolManager.getRedactionManager().openRedactionBySearchDialog();
        } else {
            // if mNextToolMode is not set, return false and let the child class handles
            return false;
        }

        return true;
    }

    @Override
    public boolean showMenu(RectF anchor_rect) {
        if (onInterceptAnnotationHandling(mAnnot)) {
            return true;
        }

        if (mPdfViewCtrl == null) {
            return false;
        }

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        return toolManager != null && !toolManager.isQuickMenuDisabled() && super.showMenu(anchor_rect);
    }

    private void selectAnnot(int x, int y) {
        unsetAnnot();

        mAnnotPageNum = 0;
        // Since find text locks the document, cancel it to release the document.
        mPdfViewCtrl.cancelFindText();
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            Annot a = ((ToolManager) mPdfViewCtrl.getToolManager()).getAnnotationAt(x, y);

            if (a != null && a.isValid()) {
                boolean isValidAnnot = true;
                if (a.getType() == Annot.e_Widget) {
                    Widget w = new Widget(a);
                    Field f = w.getField();
                    isValidAnnot = !f.getFlag(Field.e_read_only);
                }
                if (isValidAnnot) {
                    setAnnot(a, mPdfViewCtrl.getPageNumberFromScreenPt(x, y));
                    buildAnnotBBox();
                }
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
    }

    /**
     * Allow tools created from quick menu to have a preset bar if possible.
     */
    public void enablePresetMode() {
        mPresetMode = true;
    }

    /**
     * The overload implementation of {@link Tool#getModeAHLabel()}.
     */
    @Override
    protected int getModeAHLabel() {
        return AnalyticsHandlerAdapter.LABEL_QM_EMPTY;
    }

    /**
     * @return {@link AnalyticsHandlerAdapter#QUICK_MENU_TYPE_EMPTY_SPACE}
     */
    @Override
    protected int getQuickMenuAnalyticType() {
        return AnalyticsHandlerAdapter.QUICK_MENU_TYPE_EMPTY_SPACE;
    }
}
