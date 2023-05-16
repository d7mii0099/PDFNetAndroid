//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.AudioFormat;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.annotation.ColorInt;
import androidx.annotation.Keep;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.snackbar.Snackbar;
import com.pdftron.common.PDFNetException;
import com.pdftron.filters.Filter;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.Field;
import com.pdftron.pdf.Font;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.Redactor;
import com.pdftron.pdf.annots.ComboBoxWidget;
import com.pdftron.pdf.annots.FileAttachment;
import com.pdftron.pdf.annots.FreeText;
import com.pdftron.pdf.annots.Line;
import com.pdftron.pdf.annots.Link;
import com.pdftron.pdf.annots.ListBoxWidget;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.annots.PolyLine;
import com.pdftron.pdf.annots.Polygon;
import com.pdftron.pdf.annots.Popup;
import com.pdftron.pdf.annots.RadioButtonGroup;
import com.pdftron.pdf.annots.RadioButtonWidget;
import com.pdftron.pdf.annots.Redaction;
import com.pdftron.pdf.annots.Sound;
import com.pdftron.pdf.annots.Text;
import com.pdftron.pdf.annots.TextWidget;
import com.pdftron.pdf.annots.Widget;
import com.pdftron.pdf.controls.AnnotStyleDialogFragment;
import com.pdftron.pdf.dialog.SimpleDateTimePickerFragment;
import com.pdftron.pdf.dialog.measure.CalibrateDialog;
import com.pdftron.pdf.dialog.measure.CalibrateResult;
import com.pdftron.pdf.dialog.measure.CalibrateViewModel;
import com.pdftron.pdf.dialog.measurecount.CountToolDialogFragment;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.model.FreeTextCacheStruct;
import com.pdftron.pdf.model.FreeTextInfo;
import com.pdftron.pdf.model.LineEndingStyle;
import com.pdftron.pdf.model.LineStyle;
import com.pdftron.pdf.model.RotateInfo;
import com.pdftron.pdf.model.RulerItem;
import com.pdftron.pdf.model.ShapeBorderStyle;
import com.pdftron.pdf.tools.DialogAnnotNote.DialogAnnotNoteListener;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotSnappingManager;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.AnnotationClipboardHelper;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.DrawingUtils;
import com.pdftron.pdf.utils.Event;
import com.pdftron.pdf.utils.FreeTextAlignmentUtils;
import com.pdftron.pdf.utils.InlineEditText;
import com.pdftron.pdf.utils.MeasureUtils;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.PressureInkUtils;
import com.pdftron.pdf.utils.RotationUtils;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.pdf.viewmodel.RichTextViewModel;
import com.pdftron.pdf.widget.AnnotView;
import com.pdftron.pdf.widget.AutoScrollEditText;
import com.pdftron.pdf.widget.PTCropImageView;
import com.pdftron.pdf.widget.RotateHandleView;
import com.pdftron.sdf.DictIterator;
import com.pdftron.sdf.Obj;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * This class is responsible for editing a selected annotation, e.g., moving and resizing.
 */
// Note that mAnnot will be null in some situations, for example in AnnotEditRectGroup
@SuppressWarnings("WeakerAccess")
@Keep
public class AnnotEdit extends BaseTool implements DialogAnnotNoteListener,
        InlineEditText.InlineEditTextListener, TextWatcher,
        AnnotStyle.OnAnnotStyleChangeListener {

    public static final int RECTANGULAR_CTRL_PTS_CNT = 8;

    private static final String TAG = AnnotEdit.class.getName();
    private static boolean sDebug;

    /**
     * unknown control point
     */
    public static final int e_unknown = -1;
    /**
     * moving control
     */
    public static final int e_moving = -2;
    /**
     * lower left control point
     */
    public static final int e_ll = 0;
    /**
     * lower right control point
     */
    public static final int e_lr = 1;
    /**
     * upper right control point
     */
    public static final int e_ur = 2;
    /**
     * upper left control point
     */
    public static final int e_ul = 3;
    /**
     * middle right control point
     */
    public static final int e_mr = 4;
    /**
     * upper middle control point
     */
    public static final int e_um = 5;
    /**
     * lower middle control point
     */
    public static final int e_lm = 6;
    /**
     * middle left control point
     */
    public static final int e_ml = 7;

    protected final DashPathEffect mDashPathEffect;

    protected RectF mBBox = new RectF();
    protected RectF mBBoxOnDown = new RectF();
    protected RectF mContentBox;
    protected RectF mContentBoxOnDown;
    protected RectF mPageCropOnClientF;
    protected int mEffCtrlPtId = e_unknown;
    protected boolean mModifiedAnnot;
    protected boolean mCtrlPtsSet;
    private boolean mAnnotIsSticky;
    private boolean mAnnotIsSound;
    private boolean mAnnotIsFileAttachment;
    protected boolean mAnnotIsTextMarkup;
    private boolean mAnnotIsFreeText;
    private boolean mAnnotIsBasicFreeText;
    private boolean mAnnotIsStamper;
    private boolean mAnnotIsImageStamp;
    private boolean mAnnotIsSignature;
    private boolean mAnnotIsLine;
    private boolean mAnnotIsMeasurement;
    private boolean mAnnotHasFont;
    protected boolean mScaled;
    protected Paint mPaint = new Paint();
    private boolean mUpFromStickyCreate;
    private boolean mUpFromFreeTextCreate;
    private boolean mUpFromStickyCreateDlgShown;
    protected boolean mMaintainAspectRatio; // if set to true, maintain aspect ratio of annotation's bounding box

    @Nullable
    private InlineEditText mInlineEditText;
    private boolean mTapToSaveFreeTextAnnot;
    private boolean mSaveFreeTextAnnotInOnUp;
    private boolean mHasOnCloseCalled;
    private boolean mIsScaleBegun;
    private boolean mStamperToolSelected;
    boolean hasSnapped = false; // flag to check whether the annot rect has snapped because it was outside the page, if so we need to reset annot view and ctrl points

    @Nullable
    private DialogFreeTextNote mDialogFreeTextNote;

    protected int CTRL_PTS_CNT = RECTANGULAR_CTRL_PTS_CNT;

    protected PointF[] mCtrlPts = new PointF[CTRL_PTS_CNT];
    protected PointF[] mCtrlPtsOnDown = new PointF[CTRL_PTS_CNT];

    protected PointF[] mCtrlPtsInflated = new PointF[CTRL_PTS_CNT];

    protected float mCtrlRadius; // radius of the control point
    protected boolean mHideCtrlPts;

    private int mAnnotButtonPressed;

    private int mCurrentFreeTextEditMode;
    private boolean mUpdateFreeTextEditMode;
    private boolean mInEditMode;

    private float mAspectRatio; // aspect ratio of the bounding box surrounding the annotation (height / width)

    private String mCacheFileName;
    // freetext caching
    private long mStoredTimeStamp;
    private DialogStickyNote mDialogStickyNote;
    private DialogAnnotNote mDialogAnnotNote;
    protected AnnotStyleDialogFragment mAnnotStyleDialog;
    protected boolean mHandleEffCtrlPtsDisabled;

    // rotation
    private PointF mRotateDown = new PointF();
    private PointF mRotateMove = new PointF();
    private float mRotateDegree;
    private float mRotateStartDegree;
    private final float mRotateThreshold = 6f;
    private Integer mSnapDegree;

    // selection box snap
    private static final float sSnapAspectRatioThreshold = 0.1f;
    private static final int sSnapThresholdDP = 8;
    private final float mSnapThreshold;
    protected boolean mSnapEnabled = true;
    private static final int sTranslateSnapThresholdDP = 6;
    private final float mTranslateSnapThreshold;

    protected int mSelectionBoxMargin;

    @Nullable
    private RichTextViewModel mRichTextViewModel;

    private boolean mSnappingPaused = false;
    private boolean mShowSnappingMenu = false;

    /**
     * Class constructor
     */
    public AnnotEdit(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);

        // The radius size can be stored in the res/values folder.
        // This way we can pick up different sizes depending on the
        // device's size/resolution.
        mDashPathEffect = new DashPathEffect(new float[]{this.convDp2Pix(4.5f), this.convDp2Pix(2.5f)}, 0);

        for (int i = 0; i < CTRL_PTS_CNT; ++i) {
            mCtrlPts[i] = new PointF();
            mCtrlPtsOnDown[i] = new PointF();
            mCtrlPtsInflated[i] = new PointF();
        }

        mPaint.setAntiAlias(true);

        mSelectionBoxMargin = (int) Utils.convDp2Pix(mPdfViewCtrl.getContext(), ((ToolManager) mPdfViewCtrl.getToolManager()).getSelectionBoxMargin());
        mCtrlRadius = mPdfViewCtrl.getContext().getResources().getDimensionPixelSize(R.dimen.selection_widget_size_w_margin) / 2f;
        mSnapThreshold = Utils.convDp2Pix(mPdfViewCtrl.getContext(), sSnapThresholdDP);
        mTranslateSnapThreshold = Utils.convDp2Pix(mPdfViewCtrl.getContext(), sTranslateSnapThresholdDP);
    }

    /**
     * The overload implementation of {@link Tool#onCreate()}.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        if (mAnnot == null) {
            return;
        }

        boolean shouldUnlockRead = false;
        try {
            // Locks the document first as accessing annotation/doc information isn't thread
            // safe. Since we are not going to modify the doc here, we can use the read lock.
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;

            mHasSelectionPermission = hasPermission(mAnnot, ANNOT_PERMISSION_SELECTION);
            mHasMenuPermission = hasPermission(mAnnot, ANNOT_PERMISSION_MENU);
            mHasInteractPermission = hasPermission(mAnnot, ANNOT_PERMISSION_INTERACT);

            int type = mAnnot.getType();
            mAnnotStyle = AnnotUtils.getAnnotStyle(mAnnot);

            mAnnotIsLine = (type == Annot.e_Line);
            mAnnotIsSticky = (type == Annot.e_Text && !AnnotUtils.isCountMeasurement(mAnnot));
            mAnnotIsSound = (type == Annot.e_Sound);
            mAnnotIsFileAttachment = (type == Annot.e_FileAttachment);
            mAnnotIsFreeText = (type == Annot.e_FreeText);
            mAnnotIsTextMarkup = (type == Annot.e_Highlight ||
                    type == Annot.e_Underline ||
                    type == Annot.e_StrikeOut ||
                    type == Annot.e_Squiggly);
            mAnnotHasFont = mAnnotStyle.hasFont();

            if (type == Annot.e_Redact) {
                // follow adobe behaviour where redaction
                mHasSelectionPermission = false;
            }
            mAnnotIsMeasurement = mAnnotStyle.isMeasurement();

            // Create menu items based on the type of the selected annotation
            if (mAnnot.isMarkup() && type == Annot.e_Stamp) {
                Obj sigObj = mAnnot.getSDFObj();
                sigObj = sigObj.findObj(Signature.SIGNATURE_ANNOTATION_ID);
                mMaintainAspectRatio = true;
                if (sigObj != null) {
                    mAnnotIsSignature = true;
                } else {
                    mAnnotIsStamper = true;
                    mRotateStartDegree = AnnotUtils.getAnnotRotation(mAnnot);
                }
                mAnnotIsImageStamp = AnnotUtils.isImageStamp(mAnnot);
                mStamperToolSelected = true;
            } else if (mAnnotStyle.getAnnotType() == Annot.e_FreeText) {
                mAnnotIsBasicFreeText = mAnnotStyle.isBasicFreeText();
                mRotateStartDegree = AnnotUtils.getAnnotRotation(mAnnot);
                mMaintainAspectRatio = RotationUtils.shouldMaintainAspectRatio(Math.round(mRotateStartDegree));
            }

            // Remember the page bounding box in client space; this is used to ensure while
            // moving/resizing, the widget doesn't go beyond the page boundary.
            mPageCropOnClientF = Utils.buildPageBoundBoxOnClient(mPdfViewCtrl, mAnnotPageNum);

            if (!isAnnotResizable() || mAnnotIsMeasurement) {
                // don't use margin for non-resizable items
                mSelectionBoxMargin = 0;
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        if (mAnnotIsMeasurement) {
            setSnappingEnabled(toolManager.isSnappingEnabledForMeasurementTools());
        }
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.ANNOT_EDIT;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Unknown;
    }

    @SuppressWarnings("SameParameterValue")
    public void setUpFromStickyCreate(boolean flag) {
        mUpFromStickyCreate = flag;
    }

    @SuppressWarnings("SameParameterValue")
    public void setUpFromFreeTextCreate(boolean flag) {
        mUpFromFreeTextCreate = flag;
    }

    /**
     * The overload implementation of {@link Tool#isEditAnnotTool()}
     *
     * @return true
     */
    @Override
    public boolean isEditAnnotTool() {
        return true;
    }

    /**
     * get quick menu resource by clicked annotation
     *
     * @param annot annotation
     * @return menu resource
     */
    protected @MenuRes
    int getMenuResByAnnot(@Nullable Annot annot) throws PDFNetException {
        if (annot == null) {
            return R.menu.annot_general;
        }

        int type = Annot.e_Unknown;
        int fieldType = Field.e_null;
        int rotation = 0;

        boolean shouldUnlockRead = false;
        try {
            // Locks the document first as accessing annotation/doc information isn't thread
            // safe. Since we are not going to modify the doc here, we can use the read lock.
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;

            type = annot.getType();

            if (Annot.e_Widget == type) {
                Widget widget = new Widget(annot);
                Field field = widget.getField();
                fieldType = field.getType();
            } else if (Annot.e_Stamp == type) {
                rotation = AnnotUtils.getStampDegree(annot);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }

        switch (type) {
            case Annot.e_Sound:
                return R.menu.annot_edit_sound;
            case Annot.e_Square:
                return R.menu.annot_rect_create;
            case Annot.e_Circle:
            case Annot.e_Polygon:
            case Annot.e_Polyline:
            case Annot.e_Line:
            case Annot.e_Text:
                if (mAnnotIsMeasurement && type == Annot.e_Line) {
                    return R.menu.annot_ruler;
                } else if (AnnotUtils.isCountMeasurement(mAnnot)) {
                    return R.menu.annot_count_measurement;
                }
                return R.menu.annot_simple_shape;
            case Annot.e_Highlight:
            case Annot.e_Underline:
            case Annot.e_StrikeOut:
            case Annot.e_Squiggly:
                return R.menu.annot_edit_text_markup;
            case Annot.e_Redact:
                return R.menu.annot_edit_text_redaction;
            case Annot.e_FreeText:
                return R.menu.annot_free_text;
            case Annot.e_Link:
                return R.menu.annot_link;
            case Annot.e_Stamp:
                if (mAnnotIsSignature) {
                    return R.menu.annot_signature;
                }
                if (mAnnotIsImageStamp) {
                    // ensure it has no rotation first
                    if (0 == rotation) {
                        return R.menu.annot_image_stamper;
                    }
                }
                if (mAnnotIsStamper) {
                    return R.menu.annot_stamper;
                }
            case Annot.e_FileAttachment:
                return R.menu.annot_file_attachment;
            case Annot.e_Ink:
                if (AnnotUtils.isFreeHighlighter(annot)) {
                    return R.menu.annot_simple_shape;
                }
                return R.menu.annot_free_hand;
            case Annot.e_Widget:
                if (fieldType == Field.e_radio) {
                    return R.menu.annot_radio_field;
                } else if (fieldType == Field.e_text) {
                    return R.menu.annot_text_field;
                } else if (fieldType == Field.e_choice) {
                    return R.menu.annot_choice_field;
                }
            default:
                return R.menu.annot_general;
        }
    }

    /**
     * The overload implementation of {@link Tool#createQuickMenu()}.
     */
    @Override
    protected QuickMenu createQuickMenu() {
        QuickMenu quickMenu = super.createQuickMenu();
        try {
            if (mShowSnappingMenu) {
                quickMenu.inflate(R.menu.annot_snapping);
            } else {
                quickMenu.inflate(getMenuResByAnnot(mAnnot));
                customizeQuickMenuItems(quickMenu);
            }
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        quickMenu.initMenuEntries();
        return quickMenu;
    }

    /**
     * The overload implementation of {@link Tool#selectAnnot(Annot, int)}.
     */
    @Override
    public void selectAnnot(Annot annot, int pageNum) {
        super.selectAnnot(annot, pageNum);

        mNextToolMode = getToolMode();
        Pair<ToolMode, ArrayList<Annot>> pair = canSelectGroupAnnot(mPdfViewCtrl, annot, pageNum);
        if (null == pair) {
            if (mAnnotStyle != null && mAnnotStyle.isSpacingFreeText()) {
                setCtrlPts();
            } else {
                setCtrlPts(false);
            }
            mPdfViewCtrl.invalidate((int) Math.floor(mBBox.left), (int) Math.floor(mBBox.top), (int) Math.ceil(mBBox.right), (int) Math.ceil(mBBox.bottom));
            showMenu(getAnnotRect());
        } else {
            mAnnot = null;
            mGroupAnnots = pair.second;
        }
    }

    private void countAnnots(Annot selectedAnnot) {
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;

            String label = selectedAnnot.getCustomData(CountMeasurementCreateTool.COUNT_MEASURE_LABEL_KEY);
            if (!Utils.isNullOrEmpty(label)) {
                Text text = new Text(selectedAnnot);
                String customData = text.getCustomData(CountMeasurementCreateTool.COUNT_MEASURE_KEY);
                if (customData.equals("true")) {
                    int annotCount = 0;
                    int pageCount = mPdfViewCtrl.getPageCount();
                    for (int i = 1; i <= pageCount; i++) {
                        ArrayList<Annot> annots = mPdfViewCtrl.getAnnotationsOnPage(i);
                        for (Annot annot : annots) {
                            if (annot.isValid()) {
                                if (annot.getCustomData(CountMeasurementCreateTool.COUNT_MEASURE_LABEL_KEY).equals(label)) {
                                    annotCount++;
                                }
                            }
                        }
                    }
                    if (((ToolManager) mPdfViewCtrl.getToolManager()).getSnackbarListener() != null) {
                        String stampLabel = mAnnot.getCustomData(CountMeasurementCreateTool.COUNT_MEASURE_LABEL_KEY);
                        ((ToolManager) mPdfViewCtrl.getToolManager()).getSnackbarListener().onShowSnackbar(
                                (stampLabel + ": " + annotCount),
                                Snackbar.LENGTH_SHORT,
                                mPdfViewCtrl.getContext().getResources().getString(R.string.count_measurement_show_all),
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        if (((ToolManager) mPdfViewCtrl.getToolManager()) != null) {
                                            FragmentActivity activity = ((ToolManager) mPdfViewCtrl.getToolManager()).getCurrentActivity();
                                            if (activity != null) {
                                                CountToolDialogFragment dialog = new CountToolDialogFragment();
                                                dialog.setMode(CountToolDialogFragment.COUNT_MODE);
                                                dialog.setPDFViewCtrl(mPdfViewCtrl);
                                                dialog.setStyle(DialogFragment.STYLE_NO_TITLE, ((ToolManager) mPdfViewCtrl.getToolManager()).getTheme());
                                                dialog.show(activity.getSupportFragmentManager(), CountToolDialogFragment.TAG);
                                            }
                                        }
                                    }
                                }
                        );
                    }
                }
            }
        } catch (PDFNetException e) {
            e.printStackTrace();
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
    }

    @Override
    public void setupAnnotProperty(AnnotStyle annotStyle) {
        super.setupAnnotProperty(annotStyle);

        mAnnotStyle = annotStyle;
    }

    protected Rect getAnnotScreenBBox() throws PDFNetException {
        if (mAnnot == null) {
            return null;
        }
        return mPdfViewCtrl.getScreenRectForAnnot(mAnnot, mAnnotPageNum);
    }

    protected Rect getAnnotScreenContentBox() throws PDFNetException {
        if (mAnnot == null) {
            return null;
        }
        if (AnnotUtils.isCallout(mAnnot) || mAnnot.getType() == Annot.e_Square) {
            Markup markup = new Markup(mAnnot);
            Rect contentRect = markup.getContentRect();
            double[] pts1 = mPdfViewCtrl.convPagePtToScreenPt(contentRect.getX1(), contentRect.getY1(), mAnnotPageNum);
            double[] pts2 = mPdfViewCtrl.convPagePtToScreenPt(contentRect.getX2(), contentRect.getY2(), mAnnotPageNum);
            double x1 = pts1[0];
            double y1 = pts1[1];
            double x2 = pts2[0];
            double y2 = pts2[1];
            return new Rect(x1, y1, x2, y2);
        }
        return null;
    }

    /**
     * @return The screen rect of the annotation
     */
    protected RectF getScreenRect(Rect screen_rect) {
        if (screen_rect == null) {
            return null;
        }

        float x1 = 0, x2 = 0, y1 = 0, y2 = 0; // in screen pts
        try {
            x1 = (float) screen_rect.getX1();
            y1 = (float) screen_rect.getY1();
            x2 = (float) screen_rect.getX2();
            y2 = (float) screen_rect.getY2();
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        float sx = mPdfViewCtrl.getScrollX();
        float sy = mPdfViewCtrl.getScrollY();

        // Compute the control points. In case that the page is rotated, have to
        // ensure the control points are properly positioned.
        float min_x, max_x;
        float min_y, max_y;
        float x, y;

        //double[] pts;
        //pts = mPdfViewCtrl.convPagePtToScreenPt(x1, y2, mAnnotPageNum);
        min_x = max_x = x1 + sx;
        min_y = max_y = y2 + sy;

        //pts = mPdfViewCtrl.convPagePtToScreenPt((x1 + x2) / 2, y2, mAnnotPageNum);
        x = (x1 + x2) / 2 + sx;
        y = y2 + sy;
        min_x = Math.min(x, min_x);
        max_x = Math.max(x, max_x);
        min_y = Math.min(y, min_y);
        max_y = Math.max(y, max_y);

        //pts = mPdfViewCtrl.convPagePtToScreenPt(x2, y2, mAnnotPageNum);
        x = x2 + sx;
        y = y2 + sy;
        min_x = Math.min(x, min_x);
        max_x = Math.max(x, max_x);
        min_y = Math.min(y, min_y);
        max_y = Math.max(y, max_y);

        //pts = mPdfViewCtrl.convPagePtToScreenPt(x2, (y1 + y2) / 2, mAnnotPageNum);
        x = x2 + sx;
        y = (y1 + y2) / 2 + sy;
        min_x = Math.min(x, min_x);
        max_x = Math.max(x, max_x);
        min_y = Math.min(y, min_y);
        max_y = Math.max(y, max_y);

        //pts = mPdfViewCtrl.convPagePtToScreenPt(x2, y1, mAnnotPageNum);
        x = x2 + sx;
        y = y1 + sy;
        min_x = Math.min(x, min_x);
        max_x = Math.max(x, max_x);
        min_y = Math.min(y, min_y);
        max_y = Math.max(y, max_y);

        //pts = mPdfViewCtrl.convPagePtToScreenPt((x1 + x2) / 2, y1, mAnnotPageNum);
        x = (x1 + x2) / 2 + sx;
        y = y1 + sy;
        min_x = Math.min(x, min_x);
        max_x = Math.max(x, max_x);
        min_y = Math.min(y, min_y);
        max_y = Math.max(y, max_y);

        //pts = mPdfViewCtrl.convPagePtToScreenPt(x1, y1, mAnnotPageNum);
        x = x1 + sx;
        y = y1 + sy;
        min_x = Math.min(x, min_x);
        max_x = Math.max(x, max_x);
        min_y = Math.min(y, min_y);
        max_y = Math.max(y, max_y);

        //pts = mPdfViewCtrl.convPagePtToScreenPt(x1, (y1 + y2) / 2, mAnnotPageNum);
        x = x1 + sx;
        y = (y1 + y2) / 2 + sy;
        min_x = Math.min(x, min_x);
        max_x = Math.max(x, max_x);
        min_y = Math.min(y, min_y);
        max_y = Math.max(y, max_y);
        return new RectF(min_x, min_y, max_x, max_y);
    }

    @Override
    protected boolean canAddAnnotView(Annot annot, AnnotStyle annotStyle) {
        if (!((ToolManager) mPdfViewCtrl.getToolManager()).isRealTimeAnnotEdit()) {
            return false;
        }
        return mPdfViewCtrl.isAnnotationLayerEnabled() || !annotStyle.hasAppearance();
    }

    @Override
    protected boolean canAddRotateView(Annot annot) {
        if (!((ToolManager) mPdfViewCtrl.getToolManager()).isShowRotateHandle()) {
            return false;
        }
        if (!mHasSelectionPermission) {
            return false;
        }
        if (mAnnotIsStamper || mAnnotIsBasicFreeText) {
            // if annot has no-rotate flag, should skip as well
            boolean shouldUnlockRead = false;
            try {
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;

                return !annot.getFlag(Annot.e_no_rotate);
            } catch (Exception ignored) {
            } finally {
                if (shouldUnlockRead) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }
        }
        return false;
    }

    /**
     * Initializes the positions of the eight control points based on
     * the bounding box of the annotation.
     */
    protected void setCtrlPts() {
        setCtrlPts(true);
    }

    protected void setCtrlPts(boolean resetAnnotView) {
        if (onInterceptAnnotationHandling(mAnnot)) {
            return;
        }

        if (mInlineEditText != null && mInlineEditText.isEditing()) {
            // in wrappers such as Flutter, it is possible that onLayout gets triggered if adjustResize is used
            // so if we are already editing, let's skip setting control points
            return;
        }

        if (mAnnot != null && ((ToolManager) mPdfViewCtrl.getToolManager()).getSelectedAnnotId() == null) {
            // due to tool loop, somehow we did not set the selected id yet, let's try again
            setAnnot(mAnnot, mAnnotPageNum);
        }

        RectF screenRect = null;
        RectF contentRect = null;
        try {
            screenRect = getScreenRect(getAnnotScreenBBox());
            contentRect = getScreenRect(getAnnotScreenContentBox());
        } catch (PDFNetException ignored) {
        }
        if (screenRect == null) {
            return;
        }

        mCtrlPtsSet = true;
        float min_x = screenRect.left;
        float min_y = screenRect.top;
        float max_x = screenRect.right;
        float max_y = screenRect.bottom;

        mBBox.left = min_x - mCtrlRadius;
        mBBox.top = min_y - mCtrlRadius;
        mBBox.right = max_x + mCtrlRadius;
        mBBox.bottom = max_y + mCtrlRadius;

        if (contentRect != null) {
            min_x = contentRect.left;
            min_y = contentRect.top;
            max_x = contentRect.right;
            max_y = contentRect.bottom;

            if (mContentBox == null) {
                mContentBox = new RectF();
            }
            mContentBox.left = min_x - mCtrlRadius;
            mContentBox.top = min_y - mCtrlRadius;
            mContentBox.right = max_x + mCtrlRadius;
            mContentBox.bottom = max_y + mCtrlRadius;
        }

        if (resetAnnotView || null == mAnnotView) {
            addAnnotView();
            boolean addInlineText = false;
            if (mAnnotStyle != null) {
                if (mAnnotStyle.isSpacingFreeText()) {
                    addInlineText = true;
                } else if (mAnnotStyle.isBasicFreeText()) {
                    addInlineText = FreeTextCreate.sUseEditTextAppearance;
                } else if (mAnnotStyle.isWidget() && getToolManager().isTextFieldCustomAppearanceEnabled()) {
                    boolean shouldUnlockRead = false;
                    try {
                        mPdfViewCtrl.docLockRead();
                        shouldUnlockRead = true;
                        if (AnnotUtils.isTextField(mAnnot)) {
                            addInlineText = true;
                        }
                    } catch (PDFNetException e) {
                        e.printStackTrace();
                    } finally {
                        if (shouldUnlockRead) {
                            mPdfViewCtrl.docUnlockRead();
                        }
                    }
                }
            }
            if (!mHasMenuPermission) {
                addInlineText = false;
            }
            if (addInlineText) {
                final InlineEditText textView = new InlineEditText(
                        mPdfViewCtrl,
                        mAnnot,
                        mAnnotPageNum,
                        null,
                        false,
                        false,
                        false,
                        this
                );
                if (AnnotUtils.hasRotation(mPdfViewCtrl, mAnnot)) {
                    textView.getEditor().setRotation(mAnnotView.getAnnotUIRotation());
                }
                if (mAnnotStyle.isSpacingFreeText()) {
                    textView.getEditText().setAutoScrollEditTextSpacingListener(new AutoScrollEditText.AutoScrollEditTextSpacingListener() {
                        @Override
                        public void onUp() {
                            editTextSpacing();
                        }
                    });
                }
                if (mAnnotStyle.isBasicFreeText()) {
                    boolean shouldUnlockRead = false;
                    try {
                        mPdfViewCtrl.docLockRead();
                        shouldUnlockRead = true;

                        setupFreeTextProperties(textView);
                    } catch (Exception ignored) {
                    } finally {
                        if (shouldUnlockRead) {
                            mPdfViewCtrl.docUnlockRead();
                        }
                    }
                } else if (mAnnotStyle.isWidget() && getToolManager().isTextFieldCustomAppearanceEnabled()) {
                    boolean shouldUnlockRead = false;
                    try {
                        mPdfViewCtrl.docLockRead();
                        shouldUnlockRead = true;
                        if (AnnotUtils.isTextField(mAnnot)) {
                            setupTextFieldProperties(textView);
                        }
                    } catch (Exception ignored) {
                    } finally {
                        if (shouldUnlockRead) {
                            mPdfViewCtrl.docUnlockRead();
                        }
                    }
                }
                mAnnotView.setInlineEditText(textView);
            }
        } else {
            updateAnnotViewBitmap();
        }
        if (mAnnotView != null) {
            if (mAnnotView.getDrawingView() != null) {
                int xOffset = mPdfViewCtrl.getScrollX();
                int yOffset = mPdfViewCtrl.getScrollY();
                boolean shouldUnlockRead = false;
                try {
                    mPdfViewCtrl.docLockRead();
                    shouldUnlockRead = true;

                    mAnnotView.getDrawingView().initInkItem(mAnnot, mAnnotPageNum,
                            new PointF(xOffset, yOffset));

                    if (AnnotUtils.isRectAreaMeasure(mAnnot)) {
                        // this is a special case where the annotation is polygon,
                        // however we want to resize it like a rectangle
                        Polygon poly = new Polygon(mAnnot);
                        PointF[] pts = Utils.getVerticesFromPoly(mPdfViewCtrl, poly, mAnnotPageNum);
                        mAnnotView.setVertices(pts);
                    }
                } catch (Exception ignored) {
                } finally {
                    if (shouldUnlockRead) {
                        mPdfViewCtrl.docUnlockRead();
                    }
                }
            }
            updateAnnotView(min_x,
                    min_y,
                    max_x,
                    max_y);
        }
        addRotateHandle();
        if (mRotateHandle != null) {
            final int[] viewOrigin = new int[2];
            mPdfViewCtrl.getLocationInWindow(viewOrigin);

            mRotateHandle.setListener(new RotateHandleView.RotateHandleViewListener() {

                @Override
                public void onDown(float rawX, float rawY) {
                    closeQuickMenu();

                    if (sDebug) Log.d(TAG, "mRotateStartDegree: " + mRotateStartDegree);
                    mRotateDown.set(rawX - viewOrigin[0], rawY - viewOrigin[1]);
                }

                @Override
                public void onMove(float rawX, float rawY) {
                    mRotateMove.set(rawX - viewOrigin[0], rawY - viewOrigin[1]);

                    if (mAnnotView != null) {
                        RotateInfo info = mAnnotView.handleRotation(mRotateDown, mRotateMove, false);
                        mRotateDegree = info.getDegree();

                        boolean canSnap = false;
                        float totalDegree = mRotateDegree + mRotateStartDegree;
                        totalDegree = totalDegree % 360;
                        if (totalDegree > 270) {
                            totalDegree -= 360;
                        }
                        if (sDebug) Log.d(TAG, "totalDegree: " + totalDegree);
                        if (totalDegree > 0) {
                            if (mAnnotIsStamper && Math.abs(totalDegree - 45) < mRotateThreshold) {
                                mSnapDegree = 45;
                                canSnap = true;
                            } else if (Math.abs(totalDegree - 90) < mRotateThreshold) {
                                mSnapDegree = 90;
                                canSnap = true;
                            } else if (mAnnotIsStamper && Math.abs(totalDegree - 135) < mRotateThreshold) {
                                mSnapDegree = 135;
                                canSnap = true;
                            } else if (Math.abs(totalDegree - 180) < mRotateThreshold) {
                                mSnapDegree = 180;
                                canSnap = true;
                            } else if (mAnnotIsStamper && Math.abs(totalDegree - 225) < mRotateThreshold) {
                                mSnapDegree = 225;
                                canSnap = true;
                            } else if (totalDegree < mRotateThreshold) {
                                mSnapDegree = 0;
                                canSnap = true;
                            }
                        } else {
                            if (mAnnotIsStamper && Math.abs(Math.abs(totalDegree) - 45) < mRotateThreshold) {
                                mSnapDegree = -45;
                                canSnap = true;
                            } else if (Math.abs(Math.abs(totalDegree) - 90) < mRotateThreshold) {
                                mSnapDegree = -90;
                                canSnap = true;
                            } else if (Math.abs(totalDegree) < mRotateThreshold) {
                                mSnapDegree = 0;
                                canSnap = true;
                            }
                        }
                        if (!canSnap) {
                            mSnapDegree = null;
                        }
                        if (mAnnotView != null) {
                            mAnnotView.snapToDegree(mSnapDegree, mRotateStartDegree);
                        }
                        mPdfViewCtrl.invalidate();
                        if (sDebug) Log.d(TAG, "mSnapDegree: " + mSnapDegree);
                    }
                }

                @Override
                public void onUp(float rawX, float rawY, float x, float y) {
                    mRotateMove.set(rawX - viewOrigin[0], rawY - viewOrigin[1]);

                    if (mAnnotView != null) {
                        RotateInfo info = mAnnotView.handleRotation(mRotateDown, mRotateMove, true);
                        mRotateDegree = info.getDegree();
                    }

                    int degree = (int) (mRotateDegree + 0.5);
                    if (mSnapDegree != null) {
                        degree = mSnapDegree;
                    }
                    rotateStampAnnot(degree, mSnapDegree != null);

                    if (mRotateHandle != null) {
                        int l = (int) (mRotateMove.x - x + 0.5);
                        int t = (int) (mRotateMove.y - y + 0.5);

                        int xOffset = mPdfViewCtrl.getScrollX();
                        int yOffset = mPdfViewCtrl.getScrollY();

                        updateRotateView(xOffset + l, yOffset + t);
                    }

                    mSnapDegree = null;
                    if (mAnnotView != null) {
                        mAnnotView.snapToDegree(mSnapDegree, mRotateStartDegree);
                    }
                    mPdfViewCtrl.invalidate();
                }
            });
            updateRotateView(min_x, min_y, max_x, max_y);
        }

        // if maintaining aspect ratio, calculate aspect ratio
        float height = max_y - min_y;
        float width = max_x - min_x;
        mAspectRatio = height / width;

        if (!mHandleEffCtrlPtsDisabled) {
            mCtrlPts[e_ll].x = min_x;
            mCtrlPts[e_ll].y = max_y;

            mCtrlPts[e_lm].x = (min_x + max_x) / 2.0f;
            mCtrlPts[e_lm].y = max_y;

            mCtrlPts[e_lr].x = max_x;
            mCtrlPts[e_lr].y = max_y;

            mCtrlPts[e_mr].x = max_x;
            mCtrlPts[e_mr].y = (min_y + max_y) / 2.0f;

            mCtrlPts[e_ur].x = max_x;
            mCtrlPts[e_ur].y = min_y;

            mCtrlPts[e_um].x = (min_x + max_x) / 2.0f;
            mCtrlPts[e_um].y = min_y;

            mCtrlPts[e_ul].x = min_x;
            mCtrlPts[e_ul].y = min_y;

            mCtrlPts[e_ml].x = min_x;
            mCtrlPts[e_ml].y = (min_y + max_y) / 2.0f;

            // update inflated
            mCtrlPtsInflated[e_ll].x = mCtrlPts[e_ll].x - mSelectionBoxMargin;
            mCtrlPtsInflated[e_ll].y = mCtrlPts[e_ll].y + mSelectionBoxMargin;

            mCtrlPtsInflated[e_lm].x = mCtrlPts[e_lm].x;
            mCtrlPtsInflated[e_lm].y = mCtrlPts[e_lm].y + mSelectionBoxMargin;

            mCtrlPtsInflated[e_lr].x = mCtrlPts[e_lr].x + mSelectionBoxMargin;
            mCtrlPtsInflated[e_lr].y = mCtrlPts[e_lr].y + mSelectionBoxMargin;

            mCtrlPtsInflated[e_mr].x = mCtrlPts[e_mr].x + mSelectionBoxMargin;
            mCtrlPtsInflated[e_mr].y = mCtrlPts[e_mr].y;

            mCtrlPtsInflated[e_ur].x = mCtrlPts[e_ur].x + mSelectionBoxMargin;
            mCtrlPtsInflated[e_ur].y = mCtrlPts[e_ur].y - mSelectionBoxMargin;

            mCtrlPtsInflated[e_um].x = mCtrlPts[e_um].x;
            mCtrlPtsInflated[e_um].y = mCtrlPts[e_um].y - mSelectionBoxMargin;

            mCtrlPtsInflated[e_ul].x = mCtrlPts[e_ul].x - mSelectionBoxMargin;
            mCtrlPtsInflated[e_ul].y = mCtrlPts[e_ul].y - mSelectionBoxMargin;

            mCtrlPtsInflated[e_ml].x = mCtrlPts[e_ml].x - mSelectionBoxMargin;
            mCtrlPtsInflated[e_ml].y = mCtrlPts[e_ml].y;

            updateAnnotViewCtrlPt();
        }
    }

    protected boolean isAnnotResizable() {
        if (mAnnotStyle != null &&
                (mAnnotStyle.isRCFreeText() || mAnnotStyle.isSpacingFreeText())) {
            return false;
        }
        return !mAnnotIsSticky && !mAnnotIsTextMarkup && !mAnnotIsSound && !mAnnotIsFileAttachment && (mAnnotStyle != null && !mAnnotStyle.isCountMeasurement());
    }

    /**
     * The overload implementation of {@link Tool#onDraw(Canvas, Matrix)}.
     */
    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        super.onDraw(canvas, tfm);

        if (!hasAnnotSelected()) {
            return;
        }

        if (mHideCtrlPts) {
            return;
        }

        drawLoupe();

        if (mAnnotIsLine) {
            // let child handle it
            return;
        }

        if (mAnnotStyle != null && mAnnotStyle.isSpacingFreeText() && mHasMenuPermission) {
            // let spacing text view handle it
            return;
        }

        float left = mBBox.left + mCtrlRadius;
        float right = mBBox.right - mCtrlRadius;
        float top = mBBox.top + mCtrlRadius;
        float bottom = mBBox.bottom - mCtrlRadius;
        if (mContentBox != null) {
            left = mContentBox.left + mCtrlRadius;
            right = mContentBox.right - mCtrlRadius;
            top = mContentBox.top + mCtrlRadius;
            bottom = mContentBox.bottom - mCtrlRadius;
        }

        if (right - left <= 0 && bottom - top <= 0) {
            return;
        }

        // draw selection box when we have no permission
        if (!mHasSelectionPermission || mSelectionBoxMargin == 0) {
            drawSelectionBox(canvas,
                    left,
                    top,
                    right,
                    bottom
            );
        }
    }

    /**
     * The overload implementation of {@link Tool#onSingleTapConfirmed(MotionEvent)}.
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        super.onSingleTapConfirmed(e);

        if (mAnnot == null) {
            mNextToolMode = mCurrentDefaultToolMode;
            return false;
        }
        // If annotation was just pushed back in single tap create mode,
        // avoid re-entry due to tool loop
        if (mAnnotPushedBack) {
            return false;
        }

        int x = (int) (e.getX() + 0.5);
        int y = (int) (e.getY() + 0.5);
        Annot tempAnnot = ((ToolManager) mPdfViewCtrl.getToolManager()).getAnnotationAt(x, y);
        if (mAnnot.equals(tempAnnot) || mUpFromStickyCreate || mUpFromFreeTextCreate) {
            if (mCtrlPtsSet && mAnnotIsFreeText && !mUpFromStickyCreate && !mUpFromFreeTextCreate &&
                    (mInlineEditText == null || !mInlineEditText.isEditing())) {
                // if free text is already selected, tap to edit it
                enterText();
                return false;
            }

            // Single clicked within the annotation, set the control points, draw the widget and
            // show the menu.
            mNextToolMode = getToolMode();
            setCtrlPts();
            mPdfViewCtrl.invalidate((int) Math.floor(mBBox.left), (int) Math.floor(mBBox.top), (int) Math.ceil(mBBox.right), (int) Math.ceil(mBBox.bottom));

            if (mAnnotIsSticky) {
                handleStickyNote(mForceSameNextToolMode, mUpFromStickyCreate);
            } else if (!mUpFromStickyCreate && !mUpFromFreeTextCreate) {
                if (mInlineEditText == null || !mInlineEditText.isEditing()) {
                    // don't show menu if we are editing
                    if (mAnnotIsFreeText) {
                        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                        if (toolManager.isEditFreeTextOnTap()) {
                            enterText();
                        } else {
                            showMenu(getAnnotRect());
                        }
                    } else {
                        showMenu(getAnnotRect());
                        if (AnnotUtils.isCountMeasurement(mAnnot)) {
                            countAnnots(mAnnot);
                        }
                    }
                }
            }
        } else if (mTapToSaveFreeTextAnnot) {
            mTapToSaveFreeTextAnnot = false;
            return true;
        } else {
            // Otherwise goes back to the pan mode.
            if (sDebug) Log.d(TAG, "going to unsetAnnot: onSingleTapConfirmed");
            unsetAnnot();
            mNextToolMode = mCurrentDefaultToolMode;//ToolMode.PAN;

            // enable selecting another annotation with the same kind for signature, image/rubber stamper
            if (mCurrentDefaultToolMode == ToolMode.SIGNATURE
                    || mCurrentDefaultToolMode == ToolMode.STAMPER
                    || mCurrentDefaultToolMode == ToolMode.RUBBER_STAMPER) {
                mNextToolMode = ToolMode.PAN;
            }

            setCtrlPts();
            // Draw away the edit widget
            mPdfViewCtrl.invalidate((int) Math.floor(mBBox.left), (int) Math.floor(mBBox.top), (int) Math.ceil(mBBox.right), (int) Math.ceil(mBBox.bottom));
        }

        return false;
    }

    /**
     * The overload implementation of {@link Tool#onPageTurning(int, int)}.
     */
    @Override
    public void onPageTurning(int old_page, int cur_page) {
        super.onPageTurning(old_page, cur_page);
        mNextToolMode = mCurrentDefaultToolMode;
    }

    /**
     * The overload implementation of {@link Tool#onClose()}.
     */
    @Override
    public void onClose() {
        super.onClose();

        if (mHasOnCloseCalled) {
            return;
        }
        mHasOnCloseCalled = true;

        // save dialog version first as we use inline version for appearance
        if (mDialogFreeTextNote != null && mDialogFreeTextNote.isShowing()) {
            mAnnotButtonPressed = DialogInterface.BUTTON_POSITIVE;
            prepareDialogFreeTextNoteDismiss();
            mDialogFreeTextNote.dismiss();
        }

        if (mInlineEditText != null && mInlineEditText.isEditing()) {
            InputMethodManager imm = (InputMethodManager) mPdfViewCtrl.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(mPdfViewCtrl.getRootView().getWindowToken(), 0);
            }

            saveAndQuitInlineEditText(false);
        }

        if (mDialogStickyNote != null && mDialogStickyNote.isShowing()) {
            // force to save the content
            mAnnotButtonPressed = DialogInterface.BUTTON_POSITIVE;
            prepareDialogStickyNoteDismiss(false);
            mDialogStickyNote.dismiss();
        }

        unsetAnnot();
        closeQuickMenu(); // make sure all quick menu is closed by now
    }

    @Override
    protected boolean canDrawLoupe() {
        if (null == mAnnot) {
            return false;
        }
        if (mAnnotIsMeasurement) {
            return !mDrawingLoupe;
        }
        return false;
    }

    @Override
    protected int getLoupeType() {
        return LOUPE_TYPE_MEASURE;
    }

    /**
     * The overload implementation of {@link Tool#onQuickMenuClicked(QuickMenuItem)}.
     */
    @Override
    public boolean onQuickMenuClicked(QuickMenuItem menuItem) {
        if (super.onQuickMenuClicked(menuItem)) {
            return true;
        }

        if (!hasAnnotSelected()) {
            mNextToolMode = ToolMode.PAN;
            return true;
        }

        int type = Annot.e_Unknown;
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            type = mAnnot.getType();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }

        if (menuItem.getItemId() == R.id.qm_delete) {
            deleteAnnot();
        }
        // Add note to the annotation
        else if (menuItem.getItemId() == R.id.qm_note) {
            if (mAnnotIsSticky) {
                handleStickyNote(false, false);
            } else {
                handleAnnotNote(false);
            }
        }
        // Show Appearance Popup window
        else if (menuItem.getItemId() == R.id.qm_appearance) {
            changeAnnotAppearance();
        }
        // flatten the signature annotation
        else if (menuItem.getItemId() == R.id.qm_flatten) {
            handleFlattenAnnot();
            mNextToolMode = mCurrentDefaultToolMode;
        } else if (menuItem.getItemId() == R.id.qm_screencap_create) {
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            CompositeDisposable disposable = toolManager.getDisposable();
            final Context appContext = mPdfViewCtrl.getContext().getApplicationContext();

            disposable.add(AnnotUtils.createScreenshotAsync(mPdfViewCtrl.getContext().getCacheDir(), mPdfViewCtrl.getDoc(), mAnnot, mAnnotPageNum)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe(new Consumer<Disposable>() {
                        @Override
                        public void accept(Disposable disposable) throws Exception {
                            CommonToast.showText(appContext, R.string.tools_screenshot_creating, Toast.LENGTH_LONG);
                        }
                    })
                    .subscribe(new Consumer<String>() {
                        @Override
                        public void accept(String s) {
                            ((ToolManager) (mPdfViewCtrl.getToolManager())).onFileCreated(s, ToolManager.AdvancedAnnotationListener.AnnotAction.SCREENSHOT_CREATE);
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            CommonToast.showText(appContext, R.string.tools_screenshot_creating_error, Toast.LENGTH_LONG);
                        }
                    })
            );
            mNextToolMode = ToolMode.PAN;
        }
        // if mAnnot is a text markup, let AnnotEditTextMarkup handles the rest quick menu item
        else if (type == Annot.e_Underline
                || type == Annot.e_Highlight
                || type == Annot.e_StrikeOut
                || type == Annot.e_Squiggly) {
            mNextToolMode = ToolMode.ANNOT_EDIT_TEXT_MARKUP;
            return false;
        } else if (menuItem.getItemId() == R.id.qm_text) {
            if (mAnnotStyle != null && mAnnotStyle.isDateFreeText()) {
                enterDate();
            } else {
                enterText();
            }
        } else if (menuItem.getItemId() == R.id.qm_copy) {
            AnnotationClipboardHelper.copyAnnot(mPdfViewCtrl.getContext(), mAnnot, mPdfViewCtrl,
                    new AnnotationClipboardHelper.OnClipboardTaskListener() {
                        @Override
                        public void onClipboardTaskDone(String error, ArrayList<Annot> pastedAnnotList) {
                            if (error == null && mPdfViewCtrl.getContext() != null) {
                                CommonToast.showText(mPdfViewCtrl.getContext(), R.string.tools_copy_annot_confirmation, Toast.LENGTH_SHORT);
                            }
                        }
                    });
        } else if (menuItem.getItemId() == R.id.qm_open_attachment) {
            FileAttachment fileAttachment = AnnotUtils.getFileAttachment(mPdfViewCtrl, mAnnot);
            // no lock is held here in case user needs to call setDoc on the attachment file
            ((ToolManager) (mPdfViewCtrl.getToolManager())).onFileAttachmentSelected(fileAttachment);
            mNextToolMode = ToolMode.PAN;
        } else if (menuItem.getItemId() == R.id.qm_save_attachment) {
            FileAttachment fileAttachment = AnnotUtils.getFileAttachment(mPdfViewCtrl, mAnnot);
            Intent intent = getFolderPickerIntent(fileAttachment);
            ((ToolManager) (mPdfViewCtrl.getToolManager())).onSaveFileAttachmentSelected(fileAttachment, intent);
            mNextToolMode = ToolMode.PAN;
        } else if (menuItem.getItemId() == R.id.qm_edit) {
            if (type == Annot.e_Ink) {
                editInk();
            } else if (type == Annot.e_Widget) {
                editWidget();
            }
        } else if (menuItem.getItemId() == R.id.qm_link) {
            if (type == Annot.e_Link) {
                shouldUnlockRead = false;
                try {
                    mPdfViewCtrl.docLockRead();
                    shouldUnlockRead = true;

                    Link link = new Link(mAnnot);
                    DialogLinkEditor linkEditorDialog = new DialogLinkEditor(mPdfViewCtrl, this, link);
                    linkEditorDialog.show();
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } finally {
                    if (shouldUnlockRead) {
                        mPdfViewCtrl.docUnlockRead();
                    }
                }
            }
        } else if (menuItem.getItemId() == R.id.qm_form_radio_add_item) {
            RadioButtonGroup group = null;
            shouldUnlockRead = false;
            try {
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;
                RadioButtonWidget radioWidget = new RadioButtonWidget(mAnnot);
                group = radioWidget.getGroup();
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlockRead) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }
            mNextToolMode = ToolMode.FORM_RADIO_GROUP_CREATE;
            RadioGroupFieldCreate radioGroupTool = (RadioGroupFieldCreate) ((ToolManager) mPdfViewCtrl.getToolManager()).createTool(ToolMode.FORM_RADIO_GROUP_CREATE, this);
            ((ToolManager) mPdfViewCtrl.getToolManager()).setTool(radioGroupTool);
            radioGroupTool.setTargetGroup(group);
        } else if (menuItem.getItemId() == R.id.qm_redact) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mPdfViewCtrl.getContext());
            builder.setMessage(R.string.redact_apply_single_warning_body)
                    .setTitle(R.string.redact_apply_warning_title)
                    .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            redactAnnot();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .create()
                    .show();
        } else if (menuItem.getItemId() == R.id.qm_play_sound) {
            playSoundAnnot();
        } else if (menuItem.getItemId() == R.id.qm_calibrate) {
            calibration();
        } else if (menuItem.getItemId() == R.id.qm_crop) {
            showImageCropper();
        } else if (menuItem.getItemId() == R.id.qm_crop_ok) {
            cropImageAnnot();
        } else if (menuItem.getItemId() == R.id.qm_crop_cancel) {
            hideImageCropper();
        } else if (menuItem.getItemId() == R.id.qm_duplicate) {
            duplicateAnnot();
        } else if (menuItem.getItemId() == R.id.qm_disable_snapping) {
            mSnappingPaused = true;
            mShowSnappingMenu = false;
        }
        return true;
    }

    /**
     * Handles annotation appearance change request
     */
    protected void changeAnnotAppearance() {
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlock = true;
            if (!hasAnnotSelected()) {
                return;
            }

            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            if (mAnnot != null && null == mAnnotStyle) {
                mAnnotStyle = AnnotUtils.getAnnotStyle(mAnnot);
            }
            mAnnotStyle.setSnap(toolManager.isSnappingEnabledForMeasurementTools());

            int[] pdfViewCtrlOrigin = new int[2];
            mPdfViewCtrl.getLocationInWindow(pdfViewCtrlOrigin);
            RectF annotRect = getAnnotRect();
            annotRect.offset(pdfViewCtrlOrigin[0], pdfViewCtrlOrigin[1]);
            mAnnotStyleDialog = getAnnotStyleBuilder().setAnchor(annotRect).setShowPreview(false).build();
            mAnnotStyleDialog.setCanShowTextAlignment(!toolManager.isAutoResizeFreeText());
            mAnnotStyleDialog.setAnnotStyleProperties(toolManager.getAnnotStyleProperties());
            mAnnotStyleDialog.setOnAnnotStyleChangeListener(this);
            mAnnotStyleDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    if (mAnnot != null && mAnnotStyle != null) {
                        int annotType = mAnnotStyle.getAnnotType();
                        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                        Tool fakeTool = toolManager.safeCreateTool(getCurrentDefaultToolMode());
                        if (fakeTool.getCreateAnnotType() == annotType && mAnnotStyleDialog != null) {
                            toolManager.onAnnotStyleDismiss(mAnnotStyleDialog);
                        }
                    }
                    mAnnotStyleDialog = null;
                    toolManager.selectAnnot(mAnnot, mAnnotPageNum);
                }
            });
            FragmentActivity activity = ((ToolManager) mPdfViewCtrl.getToolManager()).getCurrentActivity();
            if (activity == null) {
                AnalyticsHandlerAdapter.getInstance().sendException(new Exception("ToolManager is not attached with an Activity"));
                return;
            }
            mAnnotStyleDialog.show(activity.getSupportFragmentManager(),
                    AnalyticsHandlerAdapter.STYLE_PICKER_LOC_QM,
                    AnalyticsHandlerAdapter.getInstance().getAnnotToolByAnnotType(mAnnotStyle.getAnnotType()));
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
    }

    protected AnnotStyleDialogFragment.Builder getAnnotStyleBuilder() {
        AnnotStyleDialogFragment.Builder styleDialogBuilder = new AnnotStyleDialogFragment.Builder();
        if (mAnnotHasFont) {
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            styleDialogBuilder
                    .setWhiteListFont(toolManager.getFreeTextFonts())
                    .setFontListFromAsset(toolManager.getFreeTextFontsFromAssets())
                    .setFontListFromStorage(toolManager.getFreeTextFontsFromStorage());
        }
        return styleDialogBuilder.setAnnotStyle(mAnnotStyle);
    }

    protected void rotateStampAnnot(int degree, boolean snap) {
        if (!(mAnnotIsStamper || mAnnotIsBasicFreeText) || mAnnot == null) {
            return;
        }

        // If user has done the same rotation as before shortcut method to prevent overcalling
        if (degree == 0 && !snap) {
            removeAnnotView(false, false);
            selectAnnot(mAnnot, mAnnotPageNum);
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "rotateStampAnnot");
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);

            // get stamps current rotation
            int prevRotation = AnnotUtils.getAnnotRotation(mAnnot);
            int rotation = prevRotation;
            rotation = degree + rotation;
            if (snap) {
                rotation = degree;
            } else {
                rotation = rotation % 360;
                if (rotation > 270) {
                    rotation -= 360;
                }
            }
            // keep degree in the range of [0, 360]
            rotation = (360 + rotation) % 360;
            if (mAnnotIsStamper) {
                AnnotUtils.putStampDegree(mAnnot, rotation);
            }
            mAnnot.setRotation(rotation);
            if (mAnnotIsBasicFreeText) {
                Markup markup = new Markup(mAnnot);
                markup.rotateAppearance(rotation - prevRotation);

                // save the unrotated bbox for freetext when appropriate
                AnnotUtils.saveUnrotatedBBox(mPdfViewCtrl, mAnnot, mAnnotPageNum);
            } else {
                mAnnot.refreshAppearance();
            }
            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);

            if (mAnnotIsBasicFreeText) {
                mMaintainAspectRatio = RotationUtils.shouldMaintainAspectRatio(rotation);
            }

            raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }

        removeAnnotView(false, false);
        selectAnnot(mAnnot, mAnnotPageNum);
    }

    public void enterDate() {
        SimpleDateTimePickerFragment picker = SimpleDateTimePickerFragment.newInstance(SimpleDateTimePickerFragment.MODE_DATE, false);
        picker.setSimpleDatePickerListener(new SimpleDateTimePickerFragment.SimpleDatePickerListener() {

            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                if (mAnnotStyle != null && mAnnotStyle.getDateFormat() != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat(mAnnotStyle.getDateFormat(), Locale.getDefault());
                    Calendar cal = Calendar.getInstance();
                    cal.set(view.getYear(), view.getMonth(), view.getDayOfMonth());
                    String dateStr = dateFormat.format(cal.getTime());
                    updateFreeText(dateStr);
                    setCtrlPts();
                }
            }

            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

            }

            @Override
            public void onClear() {

            }

            @Override
            public void onDismiss(boolean manuallyEnterValue, boolean dismissedWithNoSelection) {
                showMenu(getAnnotRect());
            }
        });
        Activity activity = ((ToolManager) mPdfViewCtrl.getToolManager()).getCurrentActivity();
        if (activity != null) {
            picker.show(((FragmentActivity) activity).getSupportFragmentManager(), SimpleDateTimePickerFragment.TAG);
        }
    }

    public void enterText() {
        if (!mHasMenuPermission) {
            return;
        }
        if (mAnnotStyle != null && mAnnotStyle.isDateFreeText()) {
            // date free text will be edited through calendar
            showMenu(getAnnotRect());
            return;
        }
        if (isQuickMenuShown()) {
            closeQuickMenu();
        }
        mInEditMode = true;
        mSaveFreeTextAnnotInOnUp = true;
        if (!mCtrlPtsSet) {
            setCtrlPts();
        }
        // get last used free text edit mode, either inline or dialog, and open
        // the free text annot in that mode
        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        mCurrentFreeTextEditMode = settings.getInt(ANNOTATION_FREE_TEXT_PREFERENCE_EDITING, ANNOTATION_FREE_TEXT_PREFERENCE_EDITING_DEFAULT);
        mCacheFileName = ((ToolManager) mPdfViewCtrl.getToolManager()).getFreeTextCacheFileName();
        try {
            if (!Utils.isTablet(mPdfViewCtrl.getContext()) &&
                    mPdfViewCtrl.getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                fallbackFreeTextDialog(null, true);
            } else if (mCurrentFreeTextEditMode == ANNOTATION_FREE_TEXT_PREFERENCE_DIALOG) {
                fallbackFreeTextDialog(null, false);
            } else {
                initInlineFreeTextEditing(null);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    private void editInk() {
        try {
            if (((ToolManager) (mPdfViewCtrl.getToolManager())).editInkAnnots() && mAnnot != null) {
                ((ToolManager) (mPdfViewCtrl.getToolManager())).onInkEditSelected(mAnnot, mAnnotPageNum);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    private void editWidget() {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        FragmentActivity activity = toolManager.getCurrentActivity();
        if (activity == null) {
            Log.e(Signature.class.getName(), "ToolManager is not attached to with an Activity");
            return;
        }

        String[] options = null;
        boolean isSingleChoice = false;
        boolean isCombo = false;
        boolean shouldUnlockRead = false;
        long widget = 0;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            Field field = (new Widget(mAnnot)).getField();
            isCombo = field.getFlag(Field.e_combo);
            isSingleChoice = isCombo || !field.getFlag(Field.e_multiselect);
            if (isCombo) {
                ComboBoxWidget combo = new ComboBoxWidget(mAnnot);
                options = combo.getOptions();
            } else {
                ListBoxWidget list = new ListBoxWidget(mAnnot);
                options = list.getOptions();
            }
            widget = mAnnot.__GetHandle();
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
            options = null;
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
        // show options dialog
        showWidgetChoiceDialog(widget, mAnnotPageNum, isSingleChoice, isCombo, options);
    }

    /**
     * Updates the size, location and appearance of the annotation.
     *
     * @throws PDFNetException PDFNet exception
     */
    protected void updateAnnot() throws PDFNetException {
        if (mAnnot == null || onInterceptAnnotationHandling(mAnnot)) {
            return;
        }

        // obtain new and old annotation (for update) positions before refresh appearance
        mAnnotPageNum = moveAnnotToNewPage(mBBox, mAnnot, mAnnotPageNum);
        Rect newAnnotRect = getNewAnnotPagePosition();
        if (newAnnotRect == null) {
            return;
        }
        Rect oldUpdateRect = null;
        if (!mPdfViewCtrl.isAnnotationLayerEnabled()) {
            oldUpdateRect = getOldAnnotScreenPosition();
        }

        // It is possible during viewing that GetRect does not return the most accurate bounding box
        // of what is actually rendered, to obtain the correct behavior when resizing/moving, we
        // need to call refreshAppearance before resize
        boolean canRefreshAppearance = mAnnot.getType() != Annot.e_Stamp;
        if (AnnotUtils.isBasicFreeText(mAnnot) && FreeTextCreate.sUseEditTextAppearance) {
            // for basic freetext using client appearance, skip core refresh
            canRefreshAppearance = false;
        }
        if (AnnotUtils.isTextField(mAnnot) && getToolManager().isTextFieldCustomAppearanceEnabled()) {
            // for textfield using client appearance, skip core refresh
            canRefreshAppearance = false;
        }
        if (mEffCtrlPtId != e_moving && canRefreshAppearance) {
            // DO NOT REFRESH APPEARANCE WHEN MOVING
            AnnotUtils.refreshAnnotAppearance(mPdfViewCtrl.getContext(), mAnnot);
        }

        if (mContentBox != null && (mEffCtrlPtId != e_moving || mAnnot.getType() == Annot.e_Square)) {
            Markup markup = new Markup(mAnnot);
            Rect oldContentRect = markup.getContentRect();
            Rect newContentRect = getNewAnnotPagePosition(mContentBox);
            resizeCallout(markup, oldContentRect, newContentRect);
        } else {
            mAnnot.resize(newAnnotRect);
            if (e_moving != mEffCtrlPtId) {
                // resizing
                // update list box font
                if (AnnotUtils.isListBox(mAnnot)) {
                    ListBoxWidget listBoxWidget = new ListBoxWidget(mAnnot);
                    String[] options = listBoxWidget.getSelectedOptions();
                    if (options != null) {
                        String str = Arrays.toString(options);
                        try {
                            Tool.updateFont(mPdfViewCtrl, listBoxWidget, str);
                        } catch (JSONException ignored) {
                        }
                    }
                }

                if (AnnotUtils.isRectAreaMeasure(mAnnot)) {
                    // update rect area measurement
                    // update ruler property if needed
                    Polygon poly = new Polygon(mAnnot);
                    RulerItem rulerItem = MeasureUtils.getRulerItemFromAnnot(poly);
                    ArrayList<Point> points = AnnotUtils.getPolyVertices(poly);
                    if (null != rulerItem && null != points) {
                        AreaMeasureCreate.adjustContents(poly, rulerItem, points);
                    }
                }
            }
        }
        // save the unrotated bbox for freetext when appropriate
        if (mAnnotIsBasicFreeText) {
            AnnotUtils.saveUnrotatedBBox(mPdfViewCtrl, mAnnot, mAnnotPageNum);
        }
        // We do not want to call refreshAppearance for stamps
        // to not alter their original appearance.
        if (mEffCtrlPtId != e_moving && mAnnot.getType() != Annot.e_Stamp) {
            boolean isValidFreeTextAnnot = AnnotUtils.isBasicFreeText(mAnnot) && FreeTextCreate.sUseEditTextAppearance &&
                    mAnnotView.getTextView() != null;
            boolean isValidTextFieldAnnot = false;
            if (getToolManager().isTextFieldCustomAppearanceEnabled()) {
                boolean shouldUnlockRead = false;
                try {
                    mPdfViewCtrl.docLockRead();
                    shouldUnlockRead = true;
                    isValidTextFieldAnnot = AnnotUtils.isTextField(mAnnot);
                } catch (Exception ignored) {

                } finally {
                    if (shouldUnlockRead) {
                        mPdfViewCtrl.docUnlockRead();
                    }
                }
            }
            if (isValidFreeTextAnnot || isValidTextFieldAnnot) {
                AnnotUtils.createCustomFreeTextAppearance(
                        mAnnotView.getTextView(),
                        mPdfViewCtrl,
                        mAnnot,
                        mAnnotPageNum,
                        mAnnotView.getTextView().getBoundingRect()
                );
            } else {
                AnnotUtils.refreshAnnotAppearance(mPdfViewCtrl.getContext(), mAnnot);
            }
        }
        buildAnnotBBox();

        if (null != oldUpdateRect) {
            mPdfViewCtrl.update(oldUpdateRect);
        }
        mPdfViewCtrl.update(mAnnot, mAnnotPageNum);

        if (!mMaintainAspectRatio) {
            mAspectRatio = (float) (newAnnotRect.getHeight() / newAnnotRect.getWidth());
        }

        if (mAnnotStyle != null && mAnnotStyle.isSpacingFreeText()) {
            // reset to make sure resize handle can be used properly
            setCtrlPts();
        }

        if (hasSnapped) {
            setCtrlPts(true);
            hasSnapped = false;
        }
    }

    protected void adjustExtraFreeTextProps(Rect oldContentRect, Rect newContentRect) {
        // override in child handling class
    }

    /**
     * Move the annotation to a new page if possible.
     *
     * @param bbox         the rect of the new position of the annotation
     * @param annot        the annot to copy
     * @param annotPageNum the current page number containing the annot
     * @return page number that the annot was copied to
     */
    protected int moveAnnotToNewPage(RectF bbox, Annot annot, int annotPageNum) {
        float x1 = bbox.left + mCtrlRadius - mPdfViewCtrl.getScrollX();
        float y1 = bbox.top + mCtrlRadius - mPdfViewCtrl.getScrollY();

        if (shouldCopyAnnot(annotPageNum)) {
            float midX = x1 + bbox.width() / 2.0f - mCtrlRadius;
            float midY = y1 + bbox.height() / 2.0f - mCtrlRadius;
            int newPageNumber = mPdfViewCtrl.getPageNumberFromScreenPt(midX, midY);

            // This occurs when the screen point is outside of a page
            if (newPageNumber < 1) {
                newPageNumber = annotPageNum;
                return newPageNumber;
            }

            return copyAnnotToNewPage(annot, annotPageNum, newPageNumber);
        }

        return annotPageNum;
    }

    protected int copyAnnotToNewPage(Annot annot, int annotPageNum, int newPageNumber) {
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            if (annotPageNum == newPageNumber) {
                return annotPageNum;
            }

            Page currentPage = mPdfViewCtrl.getDoc().getPage(annotPageNum);
            Page newPage = mPdfViewCtrl.getDoc().getPage(newPageNumber);

            if (annot.getSDFObj().isIndirect()) { // handle only if indirect
                // Remove annot from previous page
                raiseAnnotationPreRemoveEvent(annot, annotPageNum);
                currentPage.annotRemove(annot);
                raiseAnnotationRemovedEvent(annot, annotPageNum);

                // Add annot to new page
                newPage.annotPushBack(annot);
                raiseAnnotationAddedEvent(annot, newPageNumber);
                return newPageNumber;
            }
        } catch (PDFNetException e) {
            e.printStackTrace();
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }

        return annotPageNum;
    }

    /**
     * @return The new annotation position in page space
     * @throws PDFNetException PDFNet exception
     */
    protected Rect getNewAnnotPagePosition() throws PDFNetException {
        return getNewAnnotPagePosition(mBBox);
    }

    private Rect getNewAnnotPagePosition(RectF rect) throws PDFNetException {
        if (mAnnot == null) {
            return null;
        }

        // Compute the new annotation position
        RectF snappedRect = snapAnnotScreenRectToPage(rect);
        float x1 = snappedRect.left - mPdfViewCtrl.getScrollX();
        float y1 = snappedRect.top - mPdfViewCtrl.getScrollY();
        float x2 = snappedRect.right - mPdfViewCtrl.getScrollX();
        float y2 = snappedRect.bottom - mPdfViewCtrl.getScrollY();

        double[] pts1, pts2;
        pts1 = mPdfViewCtrl.convScreenPtToPagePt(x1, y1, mAnnotPageNum);
        pts2 = mPdfViewCtrl.convScreenPtToPagePt(x2, y2, mAnnotPageNum);

        Rect newAnnotRect;
        if (mAnnot.getFlag(Annot.e_no_zoom)) {
            newAnnotRect = new Rect(pts1[0], pts1[1] - mAnnot.getRect().getHeight(), pts1[0] + mAnnot.getRect().getWidth(), pts1[1]);
        } else {
            newAnnotRect = new Rect(pts1[0], pts1[1], pts2[0], pts2[1]);
        }
        newAnnotRect.normalize();
        return newAnnotRect;
    }

    /**
     * @return whether the annot should be copied instead of moved/modified. Annot should be copied
     * if it has been moved to another page.
     */
    protected boolean shouldCopyAnnot(int currentPage) {
        float x1 = mBBox.left + mCtrlRadius - mPdfViewCtrl.getScrollX();
        float y1 = mBBox.top + mCtrlRadius - mPdfViewCtrl.getScrollY();
        float midX = x1 + mBBox.width() / 2.0f - mCtrlRadius;
        float midY = y1 + mBBox.height() / 2.0f - mCtrlRadius;
        int newPage = mPdfViewCtrl.getPageNumberFromScreenPt(midX, midY);
        return newPage != currentPage;
    }

    /**
     * Snaps the annot rect (minus the control points) in screen space to the current page.
     *
     * @return the snapped rect in screen space
     */
    protected RectF snapAnnotScreenRectToPage(RectF rect) {
        // Compute annot screen rect minus the control points
        float x1 = rect.left + mCtrlRadius;
        float y1 = rect.top + mCtrlRadius;
        float x2 = rect.right - mCtrlRadius;
        float y2 = rect.bottom - mCtrlRadius;

        RectF screenRectWithoutCtrlPoints = new RectF(x1, y1, x2, y2);
        hasSnapped = snapRectToPage(screenRectWithoutCtrlPoints, mAnnotPageNum);

        return screenRectWithoutCtrlPoints;
    }

    protected boolean snapPtToPage(PointF point, int pageNum) {
        RectF rect = new RectF(point.x, point.y, point.x, point.y);
        boolean result = snapRectToPage(rect, pageNum);

        float newX = point.x;
        float newY = point.y;
        if (rect.left != point.x) {
            newX = rect.left;
        }

        if (rect.right != point.x) {
            newX = rect.right;
        }

        if (rect.top != point.y) {
            newY = rect.top;
        }

        if (rect.bottom != point.y) {
            newY = rect.top;
        }

        point.x = newX;
        point.y = newY;

        return result;
    }

    protected boolean snapRectToPage(RectF annotRect, int pageNumber) {
        PDFDoc doc = mPdfViewCtrl.getDoc();

        Page page;

        boolean shouldUnlockRead = false;
        boolean hasSnapped = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            page = doc.getPage(pageNumber);

            RectF pageRect = Utils.buildPageBoundBoxOnClient(mPdfViewCtrl, pageNumber);

            float leftBound = pageRect.left;
            float rightBound = pageRect.right;
            float topBound = pageRect.top;
            float bottomBound = pageRect.bottom;

            float width = annotRect.width();
            float height = annotRect.height();
            if (annotRect.left < leftBound) {
                annotRect.left = leftBound;
                annotRect.right = annotRect.left + width;
                hasSnapped = true;
            }
            if (annotRect.right > rightBound) {
                annotRect.right = rightBound;
                annotRect.left = annotRect.right - width;
                hasSnapped = true;
            }
            if (annotRect.top < topBound) {
                annotRect.top = topBound;
                annotRect.bottom = annotRect.top + height;
                hasSnapped = true;
            }
            if (annotRect.bottom > bottomBound) {
                annotRect.bottom = bottomBound;
                annotRect.top = annotRect.bottom - height;
                hasSnapped = true;
            }
        } catch (PDFNetException ignored) {

        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }

        return hasSnapped;
    }

    /**
     * @return The new annotation content rect position in page space
     * @throws PDFNetException PDFNet exception
     */
    protected Rect getNewContentRectPagePosition() throws PDFNetException {
        if (mAnnot == null || mContentBox == null) {
            return null;
        }

        // Compute the new annotation position
        float x1 = mContentBox.left + mCtrlRadius - mPdfViewCtrl.getScrollX();
        float y1 = mContentBox.top + mCtrlRadius - mPdfViewCtrl.getScrollY();
        float x2 = mContentBox.right - mCtrlRadius - mPdfViewCtrl.getScrollX();
        float y2 = mContentBox.bottom - mCtrlRadius - mPdfViewCtrl.getScrollY();
        double[] pts1, pts2;
        pts1 = mPdfViewCtrl.convScreenPtToPagePt(x1, y1, mAnnotPageNum);
        pts2 = mPdfViewCtrl.convScreenPtToPagePt(x2, y2, mAnnotPageNum);

        Rect newAnnotRect;
        if (mAnnot.getFlag(Annot.e_no_zoom)) {
            newAnnotRect = new Rect(pts1[0], pts1[1] - mAnnot.getRect().getHeight(), pts1[0] + mAnnot.getRect().getWidth(), pts1[1]);
        } else {
            newAnnotRect = new Rect(pts1[0], pts1[1], pts2[0], pts2[1]);
        }
        newAnnotRect.normalize();
        return newAnnotRect;
    }

    /**
     * The overload implementation of {@link Tool#onKeyUp(int, KeyEvent)}.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mInlineEditText != null && mInlineEditText.isEditing()) {
            return true;
        }

        if (mAnnot != null && isQuickMenuShown()) {
            if (hasMenuEntry(R.id.qm_copy) && ShortcutHelper.isCopy(keyCode, event)) {
                closeQuickMenu();
                AnnotationClipboardHelper.copyAnnot(mPdfViewCtrl.getContext(), mAnnot, mPdfViewCtrl,
                        new AnnotationClipboardHelper.OnClipboardTaskListener() {
                            @Override
                            public void onClipboardTaskDone(String error, ArrayList<Annot> pastedAnnotList) {
                                if (error == null && mPdfViewCtrl.getContext() != null) {
                                    if (PdfViewCtrlSettingsManager.shouldShowHowToPaste(mPdfViewCtrl.getContext())) {
                                        // not show the copy/paste teach if mouse is not connected
                                        PointF point = mPdfViewCtrl.getCurrentMousePosition();
                                        if (point.x != 0f || point.y != 0f) {
                                            CommonToast.showText(mPdfViewCtrl.getContext(), R.string.tools_copy_annot_teach, Toast.LENGTH_SHORT);
                                        }
                                    }
                                }
                            }
                        });

                return true;
            }

            if (hasMenuEntry(R.id.qm_copy) && hasMenuEntry(R.id.qm_delete) && ShortcutHelper.isCut(keyCode, event)) {
                closeQuickMenu();
                AnnotationClipboardHelper.copyAnnot(mPdfViewCtrl.getContext(), mAnnot, mPdfViewCtrl,
                        new AnnotationClipboardHelper.OnClipboardTaskListener() {
                            @Override
                            public void onClipboardTaskDone(String error, ArrayList<Annot> pastedAnnotList) {
                                if (error == null && mPdfViewCtrl.getContext() != null) {
                                    if (PdfViewCtrlSettingsManager.shouldShowHowToPaste(mPdfViewCtrl.getContext())) {
                                        // not show the copy/paste teach if mouse is not connected
                                        PointF point = mPdfViewCtrl.getCurrentMousePosition();
                                        if (point.x != 0f || point.y != 0f) {
                                            CommonToast.showText(mPdfViewCtrl.getContext(), R.string.tools_copy_annot_teach, Toast.LENGTH_SHORT);
                                        }
                                    }
                                    deleteAnnot();
                                }
                            }
                        });
                return true;
            }

            if (hasMenuEntry((R.id.qm_delete)) && ShortcutHelper.isDeleteAnnot(keyCode, event)) {
                closeQuickMenu();
                deleteAnnot();
                return true;
            }

            if (ShortcutHelper.isStartEdit(keyCode, event)) {
                if (hasMenuEntry(R.id.qm_text)) {
                    closeQuickMenu();
                    enterText();
                    return true;
                } else if (hasMenuEntry(R.id.qm_edit)) { // ink
                    closeQuickMenu();
                    editInk();
                    return true;
                }
            }
        }

        if (mInEditMode) {
            if (ShortcutHelper.isCommitText(keyCode, event)) {
                // if DialogFreeTextNote is open then it swallows keys event, hence we only handle
                // inline edit
                saveAndQuitInlineEditText(false);
                // hide soft keyboard
                InputMethodManager imm = (InputMethodManager) mPdfViewCtrl.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(mPdfViewCtrl.getRootView().getWindowToken(), 0);
                }
            }
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    /**
     * Edits selected annotation size
     *
     * @param priorEventMode prior event mode
     * @return true is successfully modified annotation size, false otherwise
     */
    protected boolean editAnnotSize(PDFViewCtrl.PriorEventMode priorEventMode) {
        if (mAnnot == null) {
            return false;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            if (mModifiedAnnot) {
                mModifiedAnnot = false;
                raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);
                updateAnnot();
                raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum);
            } else if (priorEventMode == PDFViewCtrl.PriorEventMode.PINCH || priorEventMode == PDFViewCtrl.PriorEventMode.DOUBLE_TAP) {
                setCtrlPts();
            }

            // Show sticky note dialog directly, if set so.
            if (mAnnotIsSticky && mUpFromStickyCreate && !mUpFromStickyCreateDlgShown) {
                handleStickyNote(mForceSameNextToolMode, true);
                return false;
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
        return true;
    }

    /**
     * The overload implementation of {@link Tool#onScaleBegin(float, float)}.
     */
    @Override
    public boolean onScaleBegin(float x, float y) {
        mIsScaleBegun = true;
        // hide edit text during scaling
        if (mInlineEditText != null && mInlineEditText.isEditing()) {
            saveAndQuitInlineEditText(true);
        }
        if (mAnnotView != null) {
            closeQuickMenu();
            setCtrlPts();
        }
        animateLoupe(false);
        return super.onScaleBegin(x, y);
    }

    @Override
    public boolean onScale(float x, float y) {
        if (mAnnotView != null) {
            setCtrlPts();
        }
        return super.onScale(x, y);
    }

    /**
     * The overload implementation of {@link Tool#onScaleEnd(float, float)}.
     */
    @Override
    public boolean onScaleEnd(float x, float y) {
        super.onScaleEnd(x, y);

        mIsScaleBegun = false;

        if (mAnnot != null) {
            // Scaled and if while moving, disable moving and set the control points back to where
            // the annotation is; this is to avoid complications.
            mScaled = true;
            setCtrlPts();
            mPdfViewCtrl.invalidate((int) Math.floor(mBBox.left), (int) Math.floor(mBBox.top), (int) Math.ceil(mBBox.right), (int) Math.ceil(mBBox.bottom));
            if (isQuickMenuShown()) {
                closeQuickMenu();
            }
            showMenu(getAnnotRect());
        }
        return false;
    }

    /**
     * The overload implementation of {@link Tool#onFlingStop()}.
     */
    @Override
    public boolean onFlingStop() {
        super.onFlingStop();

        mIsScaleBegun = false;

        if (mAnnot != null) {
            if (!mCtrlPtsSet) {
                setCtrlPts(); // May be preceded by annotation creation touch up.
            }
            mPdfViewCtrl.invalidate((int) Math.floor(mBBox.left), (int) Math.floor(mBBox.top), (int) Math.ceil(mBBox.right), (int) Math.ceil(mBBox.bottom));
            if (isQuickMenuShown()) {
                closeQuickMenu();
                showMenu(getAnnotRect());
            }
        }
        return false;
    }

    /**
     * The overload implementation of {@link Tool#onLayout(boolean, int, int, int, int)}.
     */
    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (sDebug) Log.d("AnnotEdit", "onLayout: " + changed);
        if (mAnnot != null) {
            if (!mPdfViewCtrl.isContinuousPagePresentationMode(mPdfViewCtrl.getPagePresentationMode())) {
                if (mAnnotPageNum != mPdfViewCtrl.getCurrentPage()) {
                    // Now in single page mode, and the annotation is not on this page, quit this
                    // tool mode.
                    if (sDebug) Log.d(TAG, "going to unsetAnnot: onLayout");
                    unsetAnnot();
                    mNextToolMode = ToolMode.PAN;
                    setCtrlPts();
                    mEffCtrlPtId = e_unknown;
                    closeQuickMenu();
                    return;
                }
            }

            setCtrlPts();
            if (isQuickMenuShown() && changed) {
                closeQuickMenu();
                showMenu(getAnnotRect());
            }
        }
    }

    /**
     * The overload implementation of {@link Tool#onLongPress(MotionEvent)}.
     */
    @Override
    public boolean onLongPress(MotionEvent e) {
        super.onLongPress(e);

        if (!hasAnnotSelected()) {
            return false;
        }

        // if we are editing a free text annot, consume the event
        if (mInlineEditText != null && mInlineEditText.isEditing()) {
            return true;
        }

        // if the annot has been selected through Pan and not through AnnotEdit
        // we require to set bounding box and control point in onLongPress
        if (mEffCtrlPtId == e_unknown) {
            int x = (int) (e.getX() + 0.5);
            int y = (int) (e.getY() + 0.5);
            Annot tempAnnot = ((ToolManager) mPdfViewCtrl.getToolManager()).getAnnotationAt(x, y);
            if (mAnnot != null && mAnnot.equals(tempAnnot)) {
                setCtrlPts();
                mEffCtrlPtId = e_moving;
            }
        }

        if (mEffCtrlPtId != e_unknown) {
            mNextToolMode = getToolMode();
            setCtrlPts();
            mEffCtrlPtId = e_moving;
            try {
                if (mAnnot != null && (mAnnot.getType() == Annot.e_Link || mAnnot.getType() == Annot.e_Widget)) {
                    showMenu(getAnnotRect());
                }
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            }
        } else {
            if (sDebug) Log.d(TAG, "going to unsetAnnot");
            unsetAnnot();
            mNextToolMode = ToolMode.PAN;
            setCtrlPts();
            mEffCtrlPtId = e_unknown;
        }
        // onDown will not be called after onLongPress, so we need to set mBBoxOnDown:
        mBBoxOnDown.set(mBBox);
        if (mContentBox != null) {
            if (mContentBoxOnDown == null) {
                mContentBoxOnDown = new RectF();
            }
            mContentBoxOnDown.set(mContentBox);
        }
        mPdfViewCtrl.invalidate((int) Math.floor(mBBox.left), (int) Math.floor(mBBox.top), (int) Math.ceil(mBBox.right), (int) Math.ceil(mBBox.bottom));

        return false;
    }

    /**
     * The overload implementation of {@link Tool#onScrollChanged(int, int, int, int)}.
     */
    @Override
    public void onScrollChanged(int l, int t, int oldl, int oldt) {
        // don't show the quick menu during scale
        if (!mIsScaleBegun && mAnnot != null && (Math.abs(t - oldt) <= 1) && !isQuickMenuShown() && !isCreatingAnnotation()) {
            showMenu(getAnnotRect());
        }
    }

    /**
     * The overload implementation of {@link Tool#onDown(MotionEvent)}.
     */
    @Override
    public boolean onDown(MotionEvent e) {
        super.onDown(e);

        float x = e.getX() + mPdfViewCtrl.getScrollX();
        float y = e.getY() + mPdfViewCtrl.getScrollY();

        // Check if editing a free text annot and tapped out side text box
        if (!mBBox.contains(x, y) && (mInlineEditText != null && mInlineEditText.isEditing()) && mAnnotIsFreeText) {
            // saveAndQuitInlineEditText(); // do it in onUp rather than here in onDown
            mTapToSaveFreeTextAnnot = true;
            mSaveFreeTextAnnotInOnUp = true;
            return true;
        }

        // setup loupe
        mLoupeEnabled = mAnnotIsMeasurement;
        mPressedPoint.x = x;
        mPressedPoint.y = y;
        setLoupeInfo(e.getX(), e.getY());
        animateLoupe(true);

        if (mAnnotIsLine) {
            // let child handle it
            return false;
        }

        // Re-compute the annotation's bounding box on screen, since the zoom
        // factor may have been changed.
        if (mAnnot != null) {
            mPageCropOnClientF = Utils.buildPageBoundBoxOnClient(mPdfViewCtrl, mAnnotPageNum);
        }

        // Check if any control point is hit
        mEffCtrlPtId = e_unknown;
        float thresh = mCtrlRadius * 2.25f;
        float shortest_dist = -1;
        int pointsCnt = CTRL_PTS_CNT;
        if (mAnnotIsSignature || mAnnotIsStamper) {
            pointsCnt = 4;
        }
        for (int i = 0; i < pointsCnt; ++i) {
            if (isAnnotResizable()) {
                // Sticky note and text markup cannot be re-sized
                float s = getVisualCtrlPts()[i].x;
                float t = getVisualCtrlPts()[i].y;

                float dist = (x - s) * (x - s) + (y - t) * (y - t);
                dist = (float) Math.sqrt(dist);
                if (dist <= thresh && (dist < shortest_dist || shortest_dist < 0)) {
                    mEffCtrlPtId = i;
                    shortest_dist = dist;
                }
            }

            mCtrlPtsOnDown[i].set(mCtrlPts[i]);
        }
        mBBoxOnDown.set(mBBox);
        if (mContentBox != null) {
            if (mContentBoxOnDown == null) {
                mContentBoxOnDown = new RectF();
            }
            mContentBoxOnDown.set(mContentBox);
        }

        // Check if hit within the bounding box without hitting any control point.
        // Note that text markup cannot be moved.
        if (isSupportMove() && mEffCtrlPtId == e_unknown && mBBox.contains(x, y)) {
            mEffCtrlPtId = e_moving;
        }

        if (mAnnotView != null) {
            mAnnotView.setActiveHandle(mEffCtrlPtId);
        }

        if (mAnnot != null) {
            if (!isInsideAnnot(e) && mEffCtrlPtId == e_unknown) {
                if (mInlineEditText == null || !mInlineEditText.isEditing()) {
                    if (sDebug) Log.d(TAG, "going to unsetAnnot: onDown");
                    removeAnnotView(true);
                    unsetAnnot();
                    mNextToolMode = mCurrentDefaultToolMode;
                    setCtrlPts();
                    // Draw away the edit widget
                    mPdfViewCtrl.invalidate((int) Math.floor(mBBox.left), (int) Math.floor(mBBox.top), (int) Math.ceil(mBBox.right), (int) Math.ceil(mBBox.bottom));
                }
            }
        }

        return false;
    }

    /**
     * The overload implementation of {@link Tool#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        if (mScaled) {
            // Scaled and if while moving, disable moving to avoid complications.
            return false;
        }
        if (!mHasSelectionPermission) {
            // does not have permission to modify annotation
            return false;
        }

        if (mEffCtrlPtId != e_unknown) {
            PointF snapPoint = snapToNearestIfEnabled(new PointF(e2.getX(), e2.getY()));

            // setup loupe
            float sx = mPdfViewCtrl.getScrollX();
            float sy = mPdfViewCtrl.getScrollY();
            mPressedPoint.x = snapPoint.x + sx;
            mPressedPoint.y = snapPoint.y + sy;
            setLoupeInfo(snapPoint.x, snapPoint.y);

            float totalMoveX = snapPoint.x - e1.getX();
            float totalMoveY = snapPoint.y - e1.getY();
            float thresh = 2f * mCtrlRadius;
            RectF tempRect = new RectF(mBBox);

            float left = mBBoxOnDown.left + mCtrlRadius;
            float right = mBBoxOnDown.right - mCtrlRadius;
            float top = mBBoxOnDown.top + mCtrlRadius;
            float bottom = mBBoxOnDown.bottom - mCtrlRadius;

            if (mEffCtrlPtId == e_moving) {
                if (mContentBox != null) {
                    // for content box type annot, we want to update both content box and the bbox
                    left += totalMoveX;
                    right += totalMoveX;
                    top += totalMoveY;
                    bottom += totalMoveY;
                    updateCtrlPts(true, left, right, top, bottom, mBBox);
                }
                if (mContentBoxOnDown != null) {
                    left = mContentBoxOnDown.left + mCtrlRadius;
                    right = mContentBoxOnDown.right - mCtrlRadius;
                    top = mContentBoxOnDown.top + mCtrlRadius;
                    bottom = mContentBoxOnDown.bottom - mCtrlRadius;
                }
                left += totalMoveX;
                right += totalMoveX;
                top += totalMoveY;
                bottom += totalMoveY;

                RectF snappedRect = applyTranslationSnapping(left, top, right, bottom);
                if (snappedRect != null) {
                    left = snappedRect.left;
                    right = snappedRect.right;
                    top = snappedRect.top;
                    bottom = snappedRect.bottom;
                }

                if (mContentBox != null) {
                    updateCtrlPts(true, left, right, top, bottom, mContentBox);
                } else {
                    updateCtrlPts(true, left, right, top, bottom, mBBox);
                }
                if (mContentBox != null) {
                    // adjust content box to match bbox
                    float diffLeft = mBBox.left - tempRect.left;
                    float diffRight = mBBox.right - tempRect.right;
                    float diffTop = mBBox.top - tempRect.top;
                    float diffBottom = mBBox.bottom - tempRect.bottom;
                    mContentBox.left += diffLeft;
                    mContentBox.right += diffRight;
                    mContentBox.top += diffTop;
                    mContentBox.bottom += diffBottom;
                }
                mModifiedAnnot = true;
            } else if (!mHandleEffCtrlPtsDisabled) {
                if (mContentBoxOnDown != null) {
                    left = mContentBoxOnDown.left + mCtrlRadius;
                    right = mContentBoxOnDown.right - mCtrlRadius;
                    top = mContentBoxOnDown.top + mCtrlRadius;
                    bottom = mContentBoxOnDown.bottom - mCtrlRadius;
                }

                boolean leftSnap = false;
                boolean topSnap = false;
                boolean rightSnap = false;
                boolean bottomSnap = false;

                boolean valid = false;
                switch (mEffCtrlPtId) {
                    case e_ll:
                        if (mCtrlPtsOnDown[e_ll].x + totalMoveX < mCtrlPtsOnDown[e_lr].x - thresh && mCtrlPtsOnDown[e_ll].y + totalMoveY > mCtrlPtsOnDown[e_ul].y + thresh) {
                            left = mCtrlPtsOnDown[e_ll].x + totalMoveX;
                            if (mMaintainAspectRatio) {
                                bottom = mCtrlPtsOnDown[e_ll].y + ((totalMoveX * -1) * mAspectRatio);
                            } else {
                                bottom = mCtrlPtsOnDown[e_ll].y + totalMoveY;

                                boolean snap = snapToAspectRatio(left, right, top, bottom);
                                if (snap) {
                                    bottom = mCtrlPtsOnDown[e_ll].y + ((totalMoveX * -1) * mAspectRatio);
                                }
                            }
                            valid = true;
                        }
                        leftSnap = true;
                        bottomSnap = true;
                        break;
                    case e_lm:
                        if (!mMaintainAspectRatio && mCtrlPtsOnDown[e_lm].y + totalMoveY > mCtrlPtsOnDown[e_ul].y + thresh) {
                            bottom = mCtrlPtsOnDown[e_lm].y + totalMoveY;
                            valid = true;

                            Float snap = snapToPerfectShape(left, right, top, bottom);
                            if (snap != null) {
                                bottom = top + snap;
                            }
                        }
                        bottomSnap = true;
                        break;
                    case e_lr:
                        if (mCtrlPtsOnDown[e_ll].x < mCtrlPtsOnDown[e_lr].x + totalMoveX - thresh && mCtrlPtsOnDown[e_ll].y + totalMoveY > mCtrlPtsOnDown[e_ul].y + thresh) {
                            right = mCtrlPtsOnDown[e_lr].x + totalMoveX;
                            if (mMaintainAspectRatio) {
                                bottom = mCtrlPtsOnDown[e_lr].y + (totalMoveX * mAspectRatio);
                            } else {
                                bottom = mCtrlPtsOnDown[e_lr].y + totalMoveY;

                                boolean snap = snapToAspectRatio(left, right, top, bottom);
                                if (snap) {
                                    bottom = mCtrlPtsOnDown[e_lr].y + (totalMoveX * mAspectRatio);
                                }
                            }
                            valid = true;
                        }
                        rightSnap = true;
                        bottomSnap = true;
                        break;
                    case e_mr:
                        if (!mMaintainAspectRatio && mCtrlPtsOnDown[e_ll].x < mCtrlPtsOnDown[e_lr].x + totalMoveX - thresh) {
                            right = mCtrlPtsOnDown[e_mr].x + totalMoveX;
                            valid = true;

                            Float snap = snapToPerfectShape(left, right, top, bottom);
                            if (snap != null) {
                                right = left + snap;
                            }
                        }
                        rightSnap = true;
                        break;
                    case e_ur:
                        if (mCtrlPtsOnDown[e_ll].x < mCtrlPtsOnDown[e_lr].x + totalMoveX - thresh && mCtrlPtsOnDown[e_ll].y > mCtrlPtsOnDown[e_ul].y + totalMoveY + thresh) {
                            right = mCtrlPtsOnDown[e_ur].x + totalMoveX;
                            if (mMaintainAspectRatio) {
                                top = mCtrlPtsOnDown[e_ur].y + ((totalMoveX * -1) * mAspectRatio);
                            } else {
                                top = mCtrlPtsOnDown[e_ur].y + totalMoveY;

                                boolean snap = snapToAspectRatio(left, right, top, bottom);
                                if (snap) {
                                    top = mCtrlPtsOnDown[e_ur].y + ((totalMoveX * -1) * mAspectRatio);
                                }
                            }
                            valid = true;
                        }
                        rightSnap = true;
                        topSnap = true;
                        break;
                    case e_um:
                        if (!mMaintainAspectRatio && mCtrlPtsOnDown[e_ll].y > mCtrlPtsOnDown[e_ul].y + totalMoveY + thresh) {
                            top = mCtrlPtsOnDown[e_um].y + totalMoveY;
                            valid = true;

                            Float snap = snapToPerfectShape(left, right, top, bottom);
                            if (snap != null) {
                                top = bottom - snap;
                            }
                        }
                        topSnap = true;
                        break;
                    case e_ul:
                        if (mCtrlPtsOnDown[e_ll].x + totalMoveX < mCtrlPtsOnDown[e_lr].x - thresh && mCtrlPtsOnDown[e_ll].y > mCtrlPtsOnDown[e_ul].y + totalMoveY + thresh) {
                            left = mCtrlPtsOnDown[e_ul].x + totalMoveX;
                            if (mMaintainAspectRatio) {
                                top = mCtrlPtsOnDown[e_ul].y + (totalMoveX * mAspectRatio);
                            } else {
                                top = mCtrlPtsOnDown[e_ul].y + totalMoveY;

                                boolean snap = snapToAspectRatio(left, right, top, bottom);
                                if (snap) {
                                    top = mCtrlPtsOnDown[e_ul].y + (totalMoveX * mAspectRatio);
                                }
                            }
                            valid = true;
                        }
                        leftSnap = true;
                        topSnap = true;
                        break;
                    case e_ml:
                        if (!mMaintainAspectRatio && mCtrlPtsOnDown[e_ll].x + totalMoveX < mCtrlPtsOnDown[e_lr].x - thresh) {
                            left = mCtrlPtsOnDown[e_ml].x + totalMoveX;
                            valid = true;

                            Float snap = snapToPerfectShape(left, right, top, bottom);
                            if (snap != null) {
                                left = right - snap;
                            }
                        }
                        leftSnap = true;
                        break;
                }

                RectF snappedRect = applyResizeSnapping(
                        left,
                        top,
                        right,
                        bottom,
                        leftSnap,
                        topSnap,
                        rightSnap,
                        bottomSnap
                );
                if (snappedRect != null) {
                    left = snappedRect.left;
                    right = snappedRect.right;
                    top = snappedRect.top;
                    bottom = snappedRect.bottom;
                }

                if (valid) {
                    if (mContentBox != null) {
                        updateCtrlPts(false, left, right, top, bottom, mContentBox);
                    } else {
                        updateCtrlPts(false, left, right, top, bottom, mBBox);
                    }
                    mModifiedAnnot = true;
                }
            }

            float min_x = Math.min(tempRect.left, mBBox.left);
            float max_x = Math.max(tempRect.right, mBBox.right);
            float min_y = Math.min(tempRect.top, mBBox.top);
            float max_y = Math.max(tempRect.bottom, mBBox.bottom);
            mPdfViewCtrl.invalidate((int) min_x - 1, (int) min_y - 1, (int) Math.ceil(max_x) + 1, (int) Math.ceil(max_y) + 1);
            return true;
        } else {
            showTransientPageNumber();
            return false;
        }
    }

    @Nullable
    protected RectF applyTranslationSnapping(float left, float top, float right, float bottom) {
        if (mAnnot == null || mAnnotView == null) {
            return null;
        }
        AnnotSnappingManager annotSnappingManager = getAnnotSnappingManager();
        try {
            Rect moveRect = new Rect(left, bottom, right, top);
            annotSnappingManager.setThreshold(mTranslateSnapThreshold);
            AnnotSnappingManager.SnappingResult snappingResult = annotSnappingManager.checkSnapping(
                    mPdfViewCtrl,
                    mAnnot,
                    moveRect,
                    mAnnotPageNum,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true
            );
            if (snappingResult.isSnapping() && snappingResult.getRect() != null) {
                mShowSnappingMenu = true;
                left = (float) snappingResult.getRect().getX1();
                bottom = (float) snappingResult.getRect().getY1();
                right = (float) snappingResult.getRect().getX2();
                top = (float) snappingResult.getRect().getY2();
                mAnnotView.setPositionGuidelines(snappingResult.getSnappingLines());
                return new RectF((int) left, (int) top, (int) right, (int) bottom);
            } else {
                mAnnotView.clearPositionGuidelines();
            }
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            mAnnotView.clearPositionGuidelines();
        }
        return null;
    }

    protected RectF applyResizeSnapping(float left,
            float top,
            float right,
            float bottom,
            boolean annotLeftSnapping,
            boolean annotTopSnapping,
            boolean annotRightSnapping,
            boolean annotBottomSnapping) {
        if (mAnnot == null || mAnnotView == null) {
            return null;
        }
        AnnotSnappingManager annotSnappingManager = getAnnotSnappingManager();
        try {
            Rect moveRect = new Rect(left, bottom, right, top);
            annotSnappingManager.setThreshold(mTranslateSnapThreshold);
            AnnotSnappingManager.SnappingResult snappingResult = annotSnappingManager.checkSnapping(
                    mPdfViewCtrl,
                    mAnnot,
                    moveRect,
                    mAnnotPageNum,
                    false,
                    false,
                    annotLeftSnapping,
                    annotTopSnapping,
                    annotRightSnapping,
                    annotBottomSnapping
            );
            if (snappingResult.isSnapping() && snappingResult.getRect() != null && snappingResult.getSnappingType() != null) {
                mShowSnappingMenu = true;
                if (snappingResult.getSnappingType().contains(AnnotSnappingManager.SnappingType.LEFT)) {
                    left = (float) snappingResult.getRect().getX1();
                }
                if (snappingResult.getSnappingType().contains(AnnotSnappingManager.SnappingType.TOP)) {
                    top = (float) snappingResult.getRect().getY2();
                }
                if (snappingResult.getSnappingType().contains(AnnotSnappingManager.SnappingType.RIGHT)) {
                    right = (float) snappingResult.getRect().getX2();
                }
                if (snappingResult.getSnappingType().contains(AnnotSnappingManager.SnappingType.BOTTOM)) {
                    bottom = (float) snappingResult.getRect().getY1();
                }
                mAnnotView.setPositionGuidelines(snappingResult.getSnappingLines());
                return new RectF((int) left, (int) top, (int) right, (int) bottom);
            } else {
                mAnnotView.clearPositionGuidelines();
            }
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            mAnnotView.clearPositionGuidelines();
        }
        return null;
    }

    @NonNull
    private AnnotSnappingManager getAnnotSnappingManager() {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        AnnotSnappingManager annotSnappingManager = toolManager.getAnnotSnappingManager();
        annotSnappingManager.setPaused(mSnappingPaused);
        return annotSnappingManager;
    }

    /**
     * The overload implementation of {@link Tool#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        animateLoupe(false);
        super.onUp(e, priorEventMode);
        if (sDebug) Log.d(TAG, "onUp");

        if (mAnnotView != null) {
            mAnnotView.setActiveHandle(e_unknown);
            mAnnotView.clearPositionGuidelines();
        }

        // Avoid double entry, if double tapped.
        if (mUpFromStickyCreateDlgShown) {
            return false;
        }

        if (mUpFromCalloutCreate) {
            mUpFromCalloutCreate = false;
            closeQuickMenu();
            enterText();

            mNextToolMode = getToolMode();
            return false;
        }

        if (mAnnotIsLine) {
            // let child handle it
            return false;
        }

        if (mScaled) {
            mScaled = false;
            if (mAnnot != null) {
                if (mModifiedAnnot) {
                    mModifiedAnnot = false;
                }
            }
            return false;
        }

        if (mSaveFreeTextAnnotInOnUp) {
            saveAndQuitInlineEditText(false);
            mSaveFreeTextAnnotInOnUp = false;
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            if (toolManager.isEditFreeTextOnTap()) {
                unsetAnnot();
            }
        }

        if (!mHasMenuPermission && mAnnot != null) {
            showMenu(getAnnotRect());
        }

        mNextToolMode = getToolMode();
        mScaled = false;

        if (hasAnnotSelected()
                && (mModifiedAnnot || !mCtrlPtsSet
                || priorEventMode == PDFViewCtrl.PriorEventMode.SCROLLING
                || priorEventMode == PDFViewCtrl.PriorEventMode.PINCH
                || priorEventMode == PDFViewCtrl.PriorEventMode.DOUBLE_TAP)) {
            if (!mCtrlPtsSet) {
                setCtrlPts();
            }

            if (!editAnnotSize(priorEventMode)) {
                return false;
            }

            showMenu(getAnnotRect());

            if (e_moving != mEffCtrlPtId) {
                updateAnnotViewBitmap();
                if (mAnnotView != null && mAnnotView.getDrawingView() != null) {
                    mAnnotView.snapToPerfectShape(null);
                }
            }

            return priorEventMode == PDFViewCtrl.PriorEventMode.SCROLLING || priorEventMode == PDFViewCtrl.PriorEventMode.FLING;
        } else {
            return false;
        }
    }

    protected boolean snapToAspectRatio(float left,
            float right,
            float top,
            float bottom) {
        if (!mSnapEnabled) {
            return false;
        }

        float width = right - left;
        float height = bottom - top;

        float aspectRatio = height / width;

        if (Math.abs(aspectRatio - mAspectRatio) < sSnapAspectRatioThreshold) {
            switch (mEffCtrlPtId) {
                case e_ll:
                case e_ur:
                    if (mAnnotView != null && mAnnotView.getDrawingView() != null) {
                        mAnnotView.snapToPerfectShape(AnnotView.SnapMode.ASPECT_RATIO_L);
                    }
                    return true;
                case e_lr:
                case e_ul:
                    if (mAnnotView != null && mAnnotView.getDrawingView() != null) {
                        mAnnotView.snapToPerfectShape(AnnotView.SnapMode.ASPECT_RATIO_R);
                    }
                    return true;
            }
        }
        if (mAnnotView != null && mAnnotView.getDrawingView() != null) {
            mAnnotView.snapToPerfectShape(null);
        }
        return false;
    }

    protected Float snapToPerfectShape(float left,
            float right,
            float top,
            float bottom) {
        if (!mSnapEnabled) {
            return null;
        }

        float width = right - left;
        float height = bottom - top;

        if (Math.abs(width - height) < mSnapThreshold) {
            switch (mEffCtrlPtId) {
                case e_lm:
                case e_um:
                    if (mAnnotView != null && mAnnotView.getDrawingView() != null) {
                        mAnnotView.snapToPerfectShape(AnnotView.SnapMode.VERTICAL);
                    }
                    return width;
                case e_mr:
                case e_ml:
                    if (mAnnotView != null && mAnnotView.getDrawingView() != null) {
                        mAnnotView.snapToPerfectShape(AnnotView.SnapMode.HORIZONTAL);
                    }
                    return height;
            }
        }
        if (mAnnotView != null && mAnnotView.getDrawingView() != null) {
            mAnnotView.snapToPerfectShape(null);
        }
        return null;
    }

    /**
     * Updates control points with new position.
     *
     * @param translate True if all control points were translated; False otherwise
     * @param left      The leftmost of control points
     * @param right     The rightmost of control points
     * @param top       The topmost of control points
     * @param bottom    The bottommost of control points
     */
    protected void updateCtrlPts(boolean translate,
            float left,
            float right,
            float top,
            float bottom,
            RectF which) {

        RectF tempRect = new RectF(left, top, right, bottom);
        if (mPageCropOnClientF != null) {
            PDFViewCtrl.PagePresentationMode pagePresentationMode = mPdfViewCtrl.getPagePresentationMode();

            int pageCount = mPdfViewCtrl.getPageCount();
            RectF pageRect = Utils.buildPageBoundBoxOnClient(mPdfViewCtrl, mAnnotPageNum);
            RectF firstPageRect = Utils.buildPageBoundBoxOnClient(mPdfViewCtrl, 1);
            RectF lastPageRect = Utils.buildPageBoundBoxOnClient(mPdfViewCtrl, pageCount);

            float rightBound = pageRect.right;
            float leftBound = pageRect.left;
            float topBound = pageRect.top;
            float bottomBound = pageRect.bottom;

            float firstPageMinX = firstPageRect.left;
            float firstPageMinY = firstPageRect.top;
            float firstPageMaxY = firstPageRect.bottom;

            float lastPageMaxX = lastPageRect.right;
            float lastPageMinY = lastPageRect.top;
            float lastPageMaxY = lastPageRect.bottom;

            float minX = firstPageMinX;

            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            if (translate && toolManager.isMoveAnnotationBetweenPages() && (mAnnotStyle == null || !mAnnotStyle.isSpacingFreeText())) {
                switch (pagePresentationMode) {
                    case SINGLE:
                    case SINGLE_VERT: {
                        boundBBox(tempRect, leftBound, topBound, rightBound, bottomBound, true);
                        break;
                    }
                    case SINGLE_CONT: {
                        if (tempRect.left < leftBound) {
                            boundBBoxLeft(tempRect, leftBound, true);
                        }
                        if (tempRect.right > rightBound) {
                            boundBBoxRight(tempRect, rightBound, true);
                        }
                        if (tempRect.top < firstPageMinY) {
                            boundBBoxTop(tempRect, firstPageMinY, true);
                        }
                        if (tempRect.bottom > lastPageRect.bottom) {
                            boundBBoxBottom(tempRect, lastPageMaxY, true);
                        }
                        break;
                    }
                    case FACING_COVER_VERT:
                    case FACING_COVER: {
                        if (mAnnotPageNum == 1 || pageCount == 1) {
                            boundBBox(tempRect, leftBound, topBound, rightBound, bottomBound, true);
                            break;
                        }

                        RectF secondPage = Utils.buildPageBoundBoxOnClient(mPdfViewCtrl, 2);
                        minX = secondPage.left;
                    }
                    case FACING_VERT:
                    case FACING: {
                        if (tempRect.left < minX) {
                            boundBBoxLeft(tempRect, minX, true);
                        }

                        float maxX = lastPageMaxX;
                        if (pageCount > 1 && mAnnotPageNum != pageCount) {
                            RectF secondLastPage = Utils.buildPageBoundBoxOnClient(mPdfViewCtrl, pageCount - 1);
                            maxX = Math.max(secondLastPage.right, maxX);
                        }

                        if (tempRect.right > maxX) {
                            boundBBoxRight(tempRect, maxX, true);
                        }
                        if (tempRect.top < topBound) {
                            boundBBoxTop(tempRect, topBound, true);
                        }
                        if (tempRect.bottom > bottomBound) {
                            boundBBoxBottom(tempRect, bottomBound, true);
                        }
                        break;
                    }
                    case FACING_COVER_CONT: {
                        if (tempRect.left < firstPageMinX && tempRect.top < firstPageMaxY) {
                            if ((firstPageMinX - tempRect.left) < (firstPageMaxY - tempRect.top)) {
                                boundBBoxLeft(tempRect, firstPageMinX, true);
                            } else {
                                boundBBoxTop(tempRect, firstPageMaxY, true);
                            }
                        } else {
                            if (tempRect.left < 1) {
                                boundBBoxLeft(tempRect, 1, true);
                            }
                            if (tempRect.top < firstPageMinY) {
                                boundBBoxTop(tempRect, firstPageMinY, true);
                            }
                        }

                        if (pageCount % 2 == 0 && tempRect.right > firstPageMinX && tempRect.bottom > lastPageMinY) {
                            if ((tempRect.right - firstPageMinX) < (tempRect.bottom - lastPageMinY)) {
                                boundBBoxRight(tempRect, firstPageMinX, true);
                            } else {
                                boundBBoxBottom(tempRect, lastPageMinY, true);
                            }
                        } else {
                            float firstPageMaxX = firstPageRect.right;
                            if (tempRect.right > firstPageMaxX) {
                                boundBBoxRight(tempRect, firstPageMaxX, true);
                            }
                            if (tempRect.bottom > lastPageMaxY) {
                                boundBBoxBottom(tempRect, lastPageMaxY, true);
                            }
                        }
                        break;
                    }
                    case FACING_CONT: {
                        if (tempRect.left < minX) {
                            boundBBoxLeft(tempRect, minX, true);
                        }
                        if (tempRect.top < firstPageMinY) {
                            boundBBoxTop(tempRect, firstPageMinY, true);
                        }
                        if (pageCount > 1 && (pageCount % 2 == 1) && (tempRect.right > lastPageMaxX) && (tempRect.bottom > lastPageMinY)) {
                            if ((tempRect.right - lastPageMaxX) < (tempRect.bottom - lastPageMinY)) {
                                boundBBoxRight(tempRect, lastPageMaxX, true);
                            } else {
                                boundBBoxBottom(tempRect, lastPageMinY, true);
                            }
                        } else {
                            if (tempRect.bottom > lastPageMaxY) {
                                boundBBoxBottom(tempRect, lastPageMaxY, true);
                            }
                            float maxX = lastPageMaxX;
                            if (pageCount > 1) {
                                RectF secondLastPage = Utils.buildPageBoundBoxOnClient(mPdfViewCtrl, pageCount - 1);
                                maxX = Math.max(secondLastPage.right, maxX);
                            }
                            if (tempRect.right > maxX) {
                                boundBBoxRight(tempRect, maxX, true);
                            }
                        }
                        break;
                    }
                }
            } else {
                boundBBox(tempRect, leftBound, topBound, rightBound, bottomBound, translate);
            }
        }

        left = tempRect.left;
        top = tempRect.top;
        right = tempRect.right;
        bottom = tempRect.bottom;

        updateAnnotView(left,
                top,
                right,
                bottom);

        updateRotateView(left,
                top,
                right,
                bottom);

        if (!mHandleEffCtrlPtsDisabled && mEffCtrlPtId < RECTANGULAR_CTRL_PTS_CNT) {
            // update control points
            mCtrlPts[e_ll].x = mCtrlPts[e_ul].x = mCtrlPts[e_ml].x = left;
            mCtrlPts[e_lr].x = mCtrlPts[e_ur].x = mCtrlPts[e_mr].x = right;
            mCtrlPts[e_ll].y = mCtrlPts[e_lr].y = mCtrlPts[e_lm].y = bottom;
            mCtrlPts[e_ur].y = mCtrlPts[e_ul].y = mCtrlPts[e_um].y = top;
            mCtrlPts[e_ml].y = mCtrlPts[e_mr].y = (bottom + top) / 2;
            mCtrlPts[e_lm].x = mCtrlPts[e_um].x = (left + right) / 2;

            // update inflated
            mCtrlPtsInflated[e_ll].x = mCtrlPtsInflated[e_ul].x = mCtrlPtsInflated[e_ml].x = mCtrlPts[e_ll].x - mSelectionBoxMargin;
            mCtrlPtsInflated[e_lr].x = mCtrlPtsInflated[e_ur].x = mCtrlPtsInflated[e_mr].x = mCtrlPts[e_lr].x + mSelectionBoxMargin;
            mCtrlPtsInflated[e_ll].y = mCtrlPtsInflated[e_lr].y = mCtrlPtsInflated[e_lm].y = mCtrlPts[e_ll].y + mSelectionBoxMargin;
            mCtrlPtsInflated[e_ur].y = mCtrlPtsInflated[e_ul].y = mCtrlPtsInflated[e_um].y = mCtrlPts[e_ur].y - mSelectionBoxMargin;
            mCtrlPtsInflated[e_ml].y = mCtrlPtsInflated[e_mr].y = mCtrlPts[e_ml].y;
            mCtrlPtsInflated[e_lm].x = mCtrlPtsInflated[e_um].x = mCtrlPts[e_lm].x;

            updateAnnotViewCtrlPt();
        }

        // update BBox
        if (which != null) {
            which.left = left - mCtrlRadius;
            which.top = top - mCtrlRadius;
            which.right = right + mCtrlRadius;
            which.bottom = bottom + mCtrlRadius;
        }
    }

    private void boundBBoxLeft(RectF rect, float bound, boolean translate) {
        boundBBox(rect, bound, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, translate);
    }

    private void boundBBoxTop(RectF rect, float bound, boolean translate) {

        boundBBox(rect, Float.NEGATIVE_INFINITY, bound, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, translate);
    }

    private void boundBBoxRight(RectF rect, float bound, boolean translate) {
        boundBBox(rect, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, bound, Float.POSITIVE_INFINITY, translate);
    }

    private void boundBBoxBottom(RectF rect, float bound, boolean translate) {
        boundBBox(rect, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, bound, translate);
    }

    private void boundBBox(RectF rect, float leftBound, float topBound, float rightBound, float bottomBound, boolean translate) {
        final float w = rect.width();
        final float h = rect.height();
        // Bounding along x-axis
        if (rect.right > rightBound) {
            rect.right = rightBound;
            if (translate) {
                rect.left = rect.right - w;
            } else if (mMaintainAspectRatio) {
                // If maintaining aspect ratio, must adjust height of box to
                // correspond with new width. Use newly adjusted width and the aspect ratio
                // to calculate the new height of the box.
                float width = rect.right - rect.left;
                float height = width * mAspectRatio;

                if (mEffCtrlPtId == e_lr) {
                    // change box height from bottom
                    rect.bottom = rect.top + height;
                } else if (mEffCtrlPtId == e_ur) {
                    // change box height from top
                    rect.top = rect.bottom - height;
                }
            }
        }

        if (rect.left < leftBound) {
            rect.left = leftBound;
            if (translate) {
                rect.right = rect.left + w;
            } else if (mMaintainAspectRatio) {
                // If maintaining aspect ratio, must adjust height of box to
                // correspond with new width. Use newly adjusted width and the aspect ratio
                // to calculate the new height of the box.
                float width = rect.right - rect.left;
                float height = width * mAspectRatio;

                if (mEffCtrlPtId == e_ll) {
                    // change box height from bottom
                    rect.bottom = rect.top + height;
                } else if (mEffCtrlPtId == e_ul) {
                    // change box height from top
                    rect.top = rect.bottom - height;
                }
            }
        }

        // Bounding along y-axis
        if (rect.top < topBound) {
            rect.top = topBound;
            if (translate) {
                rect.bottom = rect.top + h;
            } else if (mMaintainAspectRatio) {
                // If maintaining aspect ratio, must adjust width of box to
                // correspond with new height. Use newly adjusted height and the aspect ratio
                // to calculate the new width of the box.
                float height = rect.bottom - rect.top;
                float width = height * (1 / mAspectRatio);

                if (mEffCtrlPtId == e_ul) {
                    // change box width from left
                    rect.left = rect.right - width;
                } else if (mEffCtrlPtId == e_ur) {
                    // change box width from right
                    rect.right = rect.left + width;
                }
            }
        }
        if (rect.bottom > bottomBound) {
            rect.bottom = bottomBound;
            if (translate) {
                rect.top = rect.bottom - h;
            } else if (mMaintainAspectRatio) {
                // If maintaining aspect ratio, must adjust width of box to
                // correspond with new height. Use newly adjusted height and the aspect ratio
                // to calculate the new width of the box.
                float height = rect.bottom - rect.top;
                float width = height * (1 / mAspectRatio);

                if (mEffCtrlPtId == e_ll) {
                    // change box width from left
                    rect.left = rect.right - width;
                } else if (mEffCtrlPtId == e_lr) {
                    // change box width from right
                    rect.right = rect.left + width;
                }
            }
        }
    }

    protected void updateAnnotView(float left, float top, float right, float bottom) {
        if (mAnnotView != null) {
            int xOffset = mPdfViewCtrl.getScrollX();
            int yOffset = mPdfViewCtrl.getScrollY();
            mAnnotView.setAnnotRect(
                    new android.graphics.RectF(
                            left - xOffset,
                            top - yOffset,
                            right - xOffset,
                            bottom - yOffset));
            mAnnotView.layout(xOffset,
                    yOffset,
                    xOffset + mPdfViewCtrl.getWidth(),
                    yOffset + mPdfViewCtrl.getHeight());
            mAnnotView.invalidate();
        }
    }

    protected void updateRotateView(float min_x, float min_y, float max_x, float max_y) {
        if (mRotateHandle != null) {
            int size = mPdfViewCtrl.getContext().getResources().getDimensionPixelSize(R.dimen.rotate_button_size_w_margin);
            int left = (int) ((max_x - min_x) / 2 + min_x - size / 2.0 + 0.5);
            int top = (int) (max_y + size + 0.5);

            if (mPageCropOnClientF != null) {
                // adjust location based on page bounding box
                if ((top + size) > mPageCropOnClientF.bottom || (min_y - mPageCropOnClientF.top) < size * 2) {
                    top = (int) ((max_y - min_y) / 2 + min_y - size / 2.0 + 0.5);
                    if (min_x <= mPageCropOnClientF.centerX() && (mPageCropOnClientF.right - max_x) > size * 2) {
                        left = (int) (Math.min(max_x + size, mPageCropOnClientF.right - size) + 0.5);
                    } else {
                        left = (int) (Math.max(min_x - size * 2, mPageCropOnClientF.left) + 0.5);
                    }
                }
            }

            updateRotateView(left, top);
        }
    }

    protected void updateRotateView(int left, int top) {
        if (mRotateHandle != null) {
            int size = mPdfViewCtrl.getContext().getResources().getDimensionPixelSize(R.dimen.rotate_button_size_w_margin);
            mRotateHandle.layout(left,
                    top,
                    left + size,
                    top + size);
        }
    }

    public void updateRotateView() {
        if (mQuickMenu != null && mQuickMenu.isShowing() && mRotateHandle != null) {
            // gets location of rotation handle in the type of RectF[left,top,right,bottom] within bounds of screen
            final int[] rotateViewOrigin = new int[2];
            // gets location of mPdfViewCtrl in type of RectF[left,top,right,bottom] within bounds of screen
            final int[] mPdfViewOrigin = new int[2];

            //gets location on window of viewer rather than entire screen
            mRotateHandle.getLocationInWindow(rotateViewOrigin);
            mPdfViewCtrl.getLocationInWindow(mPdfViewOrigin);

            RectF rotateRectF = new RectF(rotateViewOrigin[0], rotateViewOrigin[1],
                    rotateViewOrigin[0] + mRotateHandle.getWidth(), rotateViewOrigin[1] + mRotateHandle.getHeight());
            RectF mPdfViewCtrlF = new RectF(mPdfViewOrigin[0], mPdfViewOrigin[1],
                    mPdfViewOrigin[0] + mPdfViewCtrl.getWidth(), mPdfViewOrigin[1] + mPdfViewCtrl.getHeight());

            RectF screenRect;
            try {
                // gets the bounding box of the annotation
                screenRect = getScreenRect(getAnnotScreenBBox());
                if (screenRect != null) {
                    float annotScreenBboxLeft = screenRect.left;
                    float annotScreenBboxTop = screenRect.top;
                    float annotScreenBboxRight = screenRect.right;
                    float annotScreenBboxBottom = screenRect.bottom;

                    int sizeRotationHandle = mPdfViewCtrl.getContext().getResources().getDimensionPixelSize(R.dimen.rotate_button_size_w_margin);
                    int rotationHandlerLeft = mRotateHandle.getLeft();
                    int rotationHandlerTop = mRotateHandle.getTop();

                    // if the rotation handle is being overlapped or cut off at the bottom or top of screen,
                    // move the rotation handle to the center of the annotation bbox
                    int moveRotationHandleCenter = (int) ((annotScreenBboxBottom - annotScreenBboxTop) / 2 + annotScreenBboxTop - sizeRotationHandle / 2.0 + 0.5);
                    int moveRotationHandleLeft = (int) annotScreenBboxLeft - sizeRotationHandle * 2;
                    int moveRotationHandleRight = (int) annotScreenBboxRight + sizeRotationHandle;

                    // Handles issues when rotation handle is being overlapped by quick menu in viewer
                    // check if overlapping 1st - if not need more logic to make sure it doesn't overlap
                    if (isQuickMenuOverlapping()) {
                        rotationHandlerTop = moveRotationHandleCenter;
                        // check if rotation handle gets cut off on either right or left side
                        // also checks if rotation handle crosses midpoint of viewer
                        if (rotateRectF.right > mPdfViewCtrl.getRight() || rotateRectF.right > (mPdfViewCtrlF.width() / 2f)) {
                            rotationHandlerLeft = moveRotationHandleLeft;
                        } else if (rotateRectF.left < mPdfViewCtrlF.left || rotateRectF.left <= (mPdfViewCtrlF.width() / 2f)) {
                            rotationHandlerLeft = moveRotationHandleRight;
                        }
                    }

                    // Handles rotation handle cut off in viewer
                    // check if rotation handle gets cut off on left side of view or right side of view
                    if (rotateRectF.right > mPdfViewCtrlF.right) {
                        rotationHandlerLeft = moveRotationHandleLeft;
                    }
                    if (rotateRectF.left < mPdfViewCtrlF.left) {
                        rotationHandlerLeft = moveRotationHandleRight;
                    }
                    // check if rotation handle gets cut off past bottom or top of view
                    if (rotateRectF.bottom >= mPdfViewCtrlF.bottom || rotateRectF.top <= mPdfViewCtrlF.top) {
                        rotationHandlerTop = moveRotationHandleCenter;
                        // check if rotation handle gets cut off on either right or left side
                        // also checks if rotation handle crosses midpoint of viewer
                        if (rotateRectF.right > mPdfViewCtrlF.right || rotateRectF.right > (mPdfViewCtrlF.width() / 2f)) {
                            rotationHandlerLeft = moveRotationHandleLeft;
                        } else if (rotateRectF.left < mPdfViewCtrlF.left || rotateRectF.left <= (mPdfViewCtrlF.width() / 2f)) {
                            rotationHandlerLeft = moveRotationHandleRight;
                        }
                    }
                    // update position of rotation handle
                    updateRotateView(rotationHandlerLeft, rotationHandlerTop);
                }
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
    }

    private boolean isQuickMenuOverlapping() {
        final int[] rotateViewOrigin = new int[2];
        final int[] quickMenuViewOrigin = new int[2];

        mRotateHandle.getLocationInWindow(rotateViewOrigin);
        mQuickMenu.getLocationInWindow(quickMenuViewOrigin);

        RectF rotateRectF = new RectF(rotateViewOrigin[0], rotateViewOrigin[1],
                rotateViewOrigin[0] + mRotateHandle.getWidth(), rotateViewOrigin[1] + mRotateHandle.getHeight());
        RectF quickMenuRectF = new RectF(quickMenuViewOrigin[0], quickMenuViewOrigin[1],
                quickMenuViewOrigin[0] + mQuickMenu.getWidth(), quickMenuViewOrigin[1] + mQuickMenu.getHeight());

        return rotateRectF.intersect(quickMenuRectF);
    }

    protected void updateAnnotViewCtrlPt() {
        if (mAnnotView != null) {
            int xOffset = mPdfViewCtrl.getScrollX();
            int yOffset = mPdfViewCtrl.getScrollY();
            PointF[] ctrlPts = new PointF[CTRL_PTS_CNT];
            ctrlPts[e_ul] = new PointF(mCtrlPtsInflated[e_ul].x - xOffset, mCtrlPtsInflated[e_ul].y - yOffset);
            ctrlPts[e_lr] = new PointF(mCtrlPtsInflated[e_lr].x - xOffset, mCtrlPtsInflated[e_lr].y - yOffset);
            ctrlPts[e_lm] = new PointF(mCtrlPtsInflated[e_lm].x - xOffset, mCtrlPtsInflated[e_lm].y - yOffset);
            ctrlPts[e_ml] = new PointF(mCtrlPtsInflated[e_ml].x - xOffset, mCtrlPtsInflated[e_ml].y - yOffset);
            mAnnotView.setCtrlPts(ctrlPts);
        }
    }

    /**
     * The overload implementation of {@link Tool#onConfigurationChanged(Configuration)}.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mInlineEditText != null && mInlineEditText.isEditing()) {
            saveAndQuitInlineEditText(false);
            closeQuickMenu();
        }

        ViewTreeObserver observer = mPdfViewCtrl.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                mPdfViewCtrl.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (mQuickMenu != null && mQuickMenu.isShowing()) {
                    mQuickMenu.requestLocation();
                }
            }
        });
    }

    /**
     * The overload implementation of {@link Tool#isCreatingAnnotation()}.
     */
    @Override
    public boolean isCreatingAnnotation() {
        return ((mInlineEditText != null && mInlineEditText.isEditing()) ||
                (mAnnotView != null && mAnnotView.isCropMode()));
    }

    /**
     * The overload implementation of {@link DialogAnnotNoteListener#onAnnotButtonPressed(int)}.
     */
    @Override
    public void onAnnotButtonPressed(int button) {
        mAnnotButtonPressed = button;
    }

    /**
     * The overload implementation of {@link Tool#showMenu(RectF, QuickMenu)}.
     */
    @Override
    public boolean showMenu(RectF anchor_rect, QuickMenu quickMenu) {
        if (anchor_rect != null && mHasSelectionPermission) {
            // adjust for selection box margin
            anchor_rect.set(anchor_rect.left - mSelectionBoxMargin,
                    anchor_rect.top - mSelectionBoxMargin,
                    anchor_rect.right + mSelectionBoxMargin,
                    anchor_rect.bottom + mSelectionBoxMargin);
        }
        return !onInterceptAnnotationHandling(mAnnot) && super.showMenu(anchor_rect, quickMenu);
    }

    private void deleteStickyAnnot() {
        if (mAnnot == null || onInterceptAnnotationHandling(mAnnot)) {
            return;
        }
        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc information isn't thread
            // safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            raiseAnnotationPreRemoveEvent(mAnnot, mAnnotPageNum);
            Page page = mPdfViewCtrl.getDoc().getPage(mAnnotPageNum);
            mAnnot = AnnotUtils.safeDeleteAnnotAndUpdate(mPdfViewCtrl, page, mAnnot, mAnnotPageNum);

            // make sure to raise remove event after mPdfViewCtrl.update and before unsetAnnot
            raiseAnnotationRemovedEvent(mAnnot, mAnnotPageNum);

            unsetAnnot();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    private void cancelNoteCreate(boolean forceSameNextToolMode, boolean backToPan) {
        mUpFromStickyCreate = false;
        mUpFromStickyCreateDlgShown = false;

        if (backToPan) {
            mNextToolMode = ToolMode.PAN;
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            ToolManager.Tool tool = toolManager.createTool(mNextToolMode, null);
            ((com.pdftron.pdf.tools.Tool) tool).mForceSameNextToolMode = forceSameNextToolMode;
            toolManager.setTool(tool);
        } else if (mAnnot != null) {
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            toolManager.selectAnnot(mAnnot, mAnnotPageNum);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void handleAnnotNote(final boolean forceSameNextToolMode) {
        if (mAnnot == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putStringArray(KEYS, new String[]{"forceSameNextTool"});
        bundle.putBoolean("forceSameNextTool", forceSameNextToolMode);
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }

        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            final Markup markup = new Markup(mAnnot);

            // adding/editing a note to a pen or shape annotation
            mDialogAnnotNote = new DialogAnnotNote(mPdfViewCtrl, markup.getContents(), mHasMenuPermission);
            mDialogAnnotNote.setAnnotNoteListener(this);
            // set buttons
            mDialogAnnotNote.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    prepareDialogAnnotNoteDismiss(forceSameNextToolMode);
                }
            });
            mDialogAnnotNote.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    cancelNoteCreate(forceSameNextToolMode, false);
                }
            });

            mDialogAnnotNote.show();
            mUpFromStickyCreateDlgShown = true;
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
    }

    private void handleStickyNote(final boolean forceSameNextToolMode, final boolean upFromStickyCreate) {
        if (mAnnot == null || mUpFromStickyCreateDlgShown) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putStringArray(KEYS, new String[]{"forceSameNextTool", "upFromStickyCreate"});
        bundle.putBoolean("forceSameNextTool", forceSameNextToolMode);
        bundle.putBoolean("upFromStickyCreate", upFromStickyCreate);
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }

        boolean canShow = ((ToolManager) mPdfViewCtrl.getToolManager()).getStickyNoteShowPopup();
        if (!canShow) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlock = true;

            final Markup markup = new Markup(mAnnot);

            // if user opens existing sticky note
            boolean existingStickyNote = !upFromStickyCreate;
            Text t = new Text(mAnnot);
            String iconType = t.getIconName();

            ColorPt colorPt = mAnnot.getColorAsRGB();
            int r = (int) (Math.round(colorPt.get(0) * 255));
            int g = (int) (Math.round(colorPt.get(1) * 255));
            int b = (int) (Math.round(colorPt.get(2) * 255));
            int iconColor = Color.rgb(r, g, b);
            double iconOpacity = markup.getOpacity();
            String contents = markup.getContents();
            if (!Utils.isNullOrEmpty(contents)) {
                existingStickyNote = true;
            }

            mDialogStickyNote = new DialogStickyNote(mPdfViewCtrl, markup.getContents(), existingStickyNote, iconType, iconColor, (float) iconOpacity, mHasMenuPermission);
            mDialogStickyNote.setAnnotNoteListener(this);
            // set buttons
            mDialogStickyNote.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    prepareDialogStickyNoteDismiss(forceSameNextToolMode);
                }
            });
            mDialogStickyNote.setAnnotAppearanceChangeListener(this);
            mDialogStickyNote.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    cancelNoteCreate(forceSameNextToolMode, false);
                }
            });

            mDialogStickyNote.show();
            mUpFromStickyCreateDlgShown = true;
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
    }

    private void prepareDialogStickyNoteDismiss(boolean forceSameNextToolMode) {
        if (mPdfViewCtrl == null || mAnnot == null || mDialogStickyNote == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "prepareDialogStickyNoteDismiss");
        bundle.putStringArray(KEYS, new String[]{"contents", "pressedButton", "forceSameNextTool"});
        bundle.putBoolean("forceSameNextTool", forceSameNextToolMode);
        bundle.putInt("pressedButton", mAnnotButtonPressed);
        bundle.putString("contents", mDialogStickyNote.getNote());
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }

        try {
            Markup markup = new Markup(mAnnot);
            boolean existingStickyNote = mDialogStickyNote.isExistingNote();

            // positive button
            if (mAnnotButtonPressed == DialogInterface.BUTTON_POSITIVE) {
                boolean shouldUnlock = false;
                // SAVE button
                if (!existingStickyNote || mDialogStickyNote.isEditEnabled()) {
                    try {
                        String newContent = mDialogStickyNote.getNote();
                        Popup popup = markup.getPopup();
                        if (!existingStickyNote || (newContent != null && (popup == null || !popup.isValid()
                                || !newContent.equals(popup.getContents())))) {
                            // Locks the document first as accessing annotation/doc
                            // information isn't thread safe.
                            mPdfViewCtrl.docLock(true);
                            shouldUnlock = true;
                            raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);
                            Utils.handleEmptyPopup(mPdfViewCtrl.getDoc(), markup);
                            popup = markup.getPopup();
                            popup.setContents(newContent);
                            if (!existingStickyNote) {
                                setAuthor(markup);
                            }
                            raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);
                        }
                    } catch (Exception e) {
                        AnalyticsHandlerAdapter.getInstance().sendException(e);
                    } finally {
                        if (shouldUnlock) {
                            mPdfViewCtrl.docUnlock();
                        }
                    }
                    mUpFromStickyCreate = false;
                    mUpFromStickyCreateDlgShown = false;

                    showMenu(getAnnotRect());
                } else {
                    // CLOSE button
                    showMenu(getAnnotRect());
                    cancelNoteCreate(forceSameNextToolMode, false);
                }
            } else if (mAnnotButtonPressed == DialogInterface.BUTTON_NEGATIVE && existingStickyNote) {
                // negative button
                // CANCEL button
                // Don't save note edits and show menu
                if (mDialogStickyNote.isEditEnabled()) {
                    showMenu(getAnnotRect());
                    cancelNoteCreate(forceSameNextToolMode, false);
                } else {
                    // DELETE button
                    deleteStickyAnnot();
                    cancelNoteCreate(forceSameNextToolMode, !mForceSameNextToolMode);
                }
            } else {
                // cancelled (through back button)
                if (!existingStickyNote) {
                    cancelNoteCreate(forceSameNextToolMode, true);
                }
            }
            mAnnotButtonPressed = 0;
            mDialogStickyNote.prepareDismiss();
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    private void prepareDialogAnnotNoteDismiss(boolean forceSameNextToolMode) {
        if (mPdfViewCtrl == null || mAnnot == null || mDialogAnnotNote == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "prepareDialogAnnotNoteDismiss");
        bundle.putStringArray(KEYS, new String[]{"contents", "pressedButton", "forceSameNextTool"});
        bundle.putBoolean("forceSameNextTool", forceSameNextToolMode);
        bundle.putInt("pressedButton", mAnnotButtonPressed);
        bundle.putString("contents", mDialogAnnotNote.getNote());
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }

        try {
            Markup markup = new Markup(mAnnot);
            // positive button
            if (mAnnotButtonPressed != DialogInterface.BUTTON_NEGATIVE) {
                boolean shouldUnlock = false;
                try {
                    // Locks the document first as accessing annotation/doc
                    // information isn't thread safe.
                    mPdfViewCtrl.docLock(true);
                    shouldUnlock = true;
                    raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);
                    AnnotUtils.setAnnotContents(mPdfViewCtrl.getDoc(), markup, mDialogAnnotNote.getNote());
                    updateQuickMenuNoteText(mDialogAnnotNote.getNote());
                    raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } finally {
                    if (shouldUnlock) {
                        mPdfViewCtrl.docUnlock();
                    }
                }
                mUpFromStickyCreate = false;
                mUpFromStickyCreateDlgShown = false;

                if (forceSameNextToolMode) {
                    if (mCurrentDefaultToolMode != ToolMode.PAN) {
                        mNextToolMode = mCurrentDefaultToolMode;
                    } else {
                        mNextToolMode = ToolMode.TEXT_ANNOT_CREATE;
                    }
                    ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                    ToolManager.Tool tool = toolManager.createTool(mNextToolMode, null);
                    ((Tool) tool).mForceSameNextToolMode = true;
                    ((Tool) tool).mCurrentDefaultToolMode = mCurrentDefaultToolMode;
                    toolManager.setTool(tool);
                } else {
                    ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                    toolManager.selectAnnot(mAnnot, mAnnotPageNum);
                }
            } else {
                // negative button
                if (!mDialogAnnotNote.isEditEnabled()) {
                    boolean shouldUnlock = false;
                    try {
                        // Locks the document first as accessing annotation/doc
                        // information isn't thread safe.
                        mPdfViewCtrl.docLock(true);
                        shouldUnlock = true;
                        raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);
                        Utils.handleEmptyPopup(mPdfViewCtrl.getDoc(), markup);
                        Popup popup = markup.getPopup();
                        popup.setContents("");
                        Utils.removeTextCopy(markup);
                        setAuthor(markup);
                        updateQuickMenuNoteText("");
                        raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);
                    } catch (Exception e) {
                        AnalyticsHandlerAdapter.getInstance().sendException(e);
                    } finally {
                        if (shouldUnlock) {
                            mPdfViewCtrl.docUnlock();
                        }
                    }
                }
                cancelNoteCreate(forceSameNextToolMode, false);
            }
            mAnnotButtonPressed = 0;
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    // note that changing the border effect values will change the bounding box
    protected void editBorderStyle(ShapeBorderStyle borderStyle) {
        editBorderStyle(mAnnot, mAnnotPageNum, borderStyle, true);
    }

    protected void editLineStyle(LineStyle lineStyle) {
        editLineStyle(mAnnot, mAnnotPageNum, lineStyle, true);
    }

    protected void editLineStartStyle(LineEndingStyle lineStartStyle) {
        editLineStartStyle(mAnnot, mAnnotPageNum, lineStartStyle, true);
    }

    protected void editLineEndStyle(LineEndingStyle lineEndStyle) {
        editLineEndStyle(mAnnot, mAnnotPageNum, lineEndStyle, true);
    }

    protected void editBorderStyle(@Nullable Annot annot, int pageNum, ShapeBorderStyle borderStyle, boolean raiseEvent) {
        if (annot == null) {
            return;
        }
        Bundle interceptInfo = new Bundle();
        interceptInfo.putString("borderStyle", borderStyle.name());
        interceptInfo.putStringArray(KEYS, new String[]{"borderStyle"});
        if (onInterceptAnnotationHandling(annot, interceptInfo)) {
            return;
        }
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            if (annot.getType() == Annot.e_Square ||
                    annot.getType() == Annot.e_Circle ||
                    annot.getType() == Annot.e_Polygon) {
                if (raiseEvent) {
                    raiseAnnotationPreModifyEvent(annot, pageNum);
                }

                switch (borderStyle) {
                    case CLOUDY:
                        AnnotUtils.setBorderStyle(annot, Markup.e_Cloudy, Annot.BorderStyle.e_solid, null);
                        break;
                    case DASHED:
                        double[] dash = DrawingUtils.getShapesDashIntervals();
                        AnnotUtils.setBorderStyle(annot, Markup.e_None, Annot.BorderStyle.e_dashed, dash);
                        break;
                    case DEFAULT:
                        AnnotUtils.setBorderStyle(annot, Markup.e_None, Annot.BorderStyle.e_solid, null);
                        break;
                }

                annot.refreshAppearance();
                mPdfViewCtrl.update(annot, pageNum);

                if (raiseEvent) {
                    raiseAnnotationModifiedEvent(annot, pageNum);
                }
            }

            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(getBorderStyleKey(AnnotUtils.getAnnotType(annot)), borderStyle.name());
            editor.apply();
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    protected void editLineStyle(@Nullable Annot annot, int pageNum, LineStyle lineStyle, boolean raiseEvent) {
        if (annot == null) {
            return;
        }
        Bundle interceptInfo = new Bundle();
        interceptInfo.putString("lineStyle", lineStyle.name());
        interceptInfo.putStringArray(KEYS, new String[]{"lineStyle"});
        if (onInterceptAnnotationHandling(annot, interceptInfo)) {
            return;
        }
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            if (annot.getType() == Annot.e_Line ||
                    annot.getType() == Annot.e_Polyline) {
                if (raiseEvent) {
                    raiseAnnotationPreModifyEvent(annot, pageNum);
                }

                switch (lineStyle) {
                    case DASHED:
                        double[] dash = DrawingUtils.getShapesDashIntervals();
                        AnnotUtils.setBorderStyle(annot, Markup.e_None, Annot.BorderStyle.e_dashed, dash);
                        break;
                    case DEFAULT:
                        AnnotUtils.setBorderStyle(annot, Markup.e_None, Annot.BorderStyle.e_solid, null);
                        break;
                }
                annot.refreshAppearance();
                mPdfViewCtrl.update(annot, pageNum);

                if (raiseEvent) {
                    raiseAnnotationModifiedEvent(annot, pageNum);
                }
            }
            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(getLineStyleKey(AnnotUtils.getAnnotType(annot)), lineStyle.name());
            editor.apply();
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    protected void editLineStartStyle(@Nullable Annot annot, int pageNum, LineEndingStyle lineStartStyle, boolean raiseEvent) {
        if (annot == null) {
            return;
        }
        Bundle interceptInfo = new Bundle();
        interceptInfo.putString("lineStartStyle", lineStartStyle.name());
        interceptInfo.putStringArray(KEYS, new String[]{"lineStartStyle"});
        if (onInterceptAnnotationHandling(annot, interceptInfo)) {
            return;
        }
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            if (annot.getType() == Annot.e_Line || annot.getType() == Annot.e_Polyline) {
                if (raiseEvent) {
                    raiseAnnotationPreModifyEvent(annot, pageNum);
                }
                Line lineAnnot = new Line(annot);
                AnnotUtils.setLineEndingStyle(lineAnnot, lineStartStyle, true);
                annot.refreshAppearance();
                mPdfViewCtrl.update(annot, pageNum);

                if (raiseEvent) {
                    raiseAnnotationModifiedEvent(annot, pageNum);
                }
            }

            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(getLineStartStyleKey(AnnotUtils.getAnnotType(annot)), lineStartStyle.name());
            editor.apply();
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    protected void editLineEndStyle(@Nullable Annot annot, int pageNum, LineEndingStyle lineEndStyle, boolean raiseEvent) {
        if (annot == null) {
            return;
        }
        Bundle interceptInfo = new Bundle();
        interceptInfo.putString("lineEndStyle", lineEndStyle.name());
        interceptInfo.putStringArray(KEYS, new String[]{"lineEndStyle"});
        if (onInterceptAnnotationHandling(annot, interceptInfo)) {
            return;
        }
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            if (annot.getType() == Annot.e_Line || annot.getType() == Annot.e_Polyline) {
                if (raiseEvent) {
                    raiseAnnotationPreModifyEvent(annot, pageNum);
                }
                Line lineAnnot = new Line(annot);
                AnnotUtils.setLineEndingStyle(lineAnnot, lineEndStyle, false);
                annot.refreshAppearance();
                mPdfViewCtrl.update(annot, pageNum);

                if (raiseEvent) {
                    raiseAnnotationModifiedEvent(annot, pageNum);
                }
            }

            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(getLineEndStyleKey(AnnotUtils.getAnnotType(annot)), lineEndStyle.name());
            editor.apply();
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    // Note, should not assume Annot is FreeText anymore, since Widgets now also support text color
    protected void editTextColor(@ColorInt int color) {
        if (mAnnot == null) {
            return;
        }

        Bundle interceptInfo = new Bundle();
        interceptInfo.putInt("textColor", color);
        interceptInfo.putStringArray(KEYS, new String[]{"textColor"});
        if (onInterceptAnnotationHandling(mAnnot, interceptInfo)) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc information
            // isn't thread safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);

            int annotType = mAnnot.getType();
            ColorPt colorPt = Utils.color2ColorPt(color);
            switch (annotType) {
                case Annot.e_Widget: {
                    Widget widget = new Widget(mAnnot);
                    widget.setTextColor(colorPt, 3);
                    widget.refreshAppearance();
                    break;
                }
                case Annot.e_FreeText: {
                    FreeText freeText = new FreeText(mAnnot);
                    freeText.setTextColor(colorPt, 3);
                    if (AnnotUtils.isCallout(freeText)) {
                        annotType = AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT;
                    } else if (AnnotUtils.isFreeTextDate(freeText)) {
                        annotType = AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_DATE;
                    } else if (AnnotUtils.isFreeTextSpacing(freeText)) {
                        annotType = AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_SPACING;
                    }
                    refreshAppearanceImpl(mAnnot, mAnnotPageNum);
                    break;
                }
                default:
                    throw new RuntimeException("Annotation should not have text color style");
            }

            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);

            raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum);

            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(getTextColorKey(annotType), color);
            editor.apply();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    protected void editColor(int color) {
        editColor(mAnnot, mAnnotPageNum, color, true);
    }

    protected void editColor(@Nullable Annot annot, int pageNum, int color, boolean raiseEvent) {
        if (annot == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "editColor");
        bundle.putInt("color", color);
        bundle.putStringArray(KEYS, new String[]{"color"});
        if (onInterceptAnnotationHandling(annot, bundle)) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc information
            // isn't thread safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            if (raiseEvent) {
                raiseAnnotationPreModifyEvent(annot, pageNum);
            }

            ColorPt colorPt = Utils.color2ColorPt(color);

            if (annot.getType() == Annot.e_FreeText) {
                FreeText freeText = new FreeText(annot);
                if (color != Color.TRANSPARENT) {
                    freeText.setLineColor(colorPt, 3);
                } else {
                    freeText.setLineColor(colorPt, 0);
                }
            } else {
                if (color != Color.TRANSPARENT) {
                    annot.setColor(colorPt, 3);
                } else {
                    annot.setColor(colorPt, 0);
                }
            }

            if (annot.getType() != Annot.e_Text) {
                // if color is transparent, then set thickness to 0,
                // and stored the original thickness to SDFObj
                if (color == Color.TRANSPARENT) {
                    com.pdftron.pdf.Annot.BorderStyle bs = annot.getBorderStyle();
                    double thickness = bs.getWidth();
                    if (thickness > 0) {
                        annot.getSDFObj().putNumber(PDFTRON_THICKNESS, thickness);
                    }
                    bs.setWidth(0);
                    annot.setBorderStyle(bs);
                    annot.getSDFObj().erase("AP");
                } else {
                    // if color is not transparent and it contains thickness object
                    // restore the thickness
                    Obj sdfObj = annot.getSDFObj();
                    Obj thicknessObj = sdfObj.findObj(PDFTRON_THICKNESS);
                    if (thicknessObj != null) {
                        // restore thickness
                        double storedThickness = thicknessObj.getNumber();
                        com.pdftron.pdf.Annot.BorderStyle bs = annot.getBorderStyle();
                        bs.setWidth(storedThickness);
                        annot.setBorderStyle(bs);
                        annot.getSDFObj().erase("AP");
                        // erase thickness obj
                        annot.getSDFObj().erase(PDFTRON_THICKNESS);
                    }
                }
            }
            refreshAppearanceImpl(annot, pageNum);
            mPdfViewCtrl.update(annot, pageNum);

            if (raiseEvent) {
                raiseAnnotationModifiedEvent(annot, pageNum, bundle);
            }

            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(getColorKey(AnnotUtils.getAnnotType(annot)), color);
            editor.apply();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    protected void editIcon(String icon) {
        editIcon(mAnnot, mAnnotPageNum, icon, true);
    }

    protected void editIcon(@Nullable Annot annot, int pageNum, String icon, boolean raiseEvent) {
        if (annot == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "editIcon");
        bundle.putString("icon", icon);
        bundle.putStringArray(KEYS, new String[]{"icon"});
        if (onInterceptAnnotationHandling(annot, bundle)) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc information
            // isn't thread safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            if (raiseEvent) {
                raiseAnnotationPreModifyEvent(annot, pageNum);
            }

            if (annot.getType() == Annot.e_Text) {
                Text text = new Text(annot);
                text.setIcon(icon);
            }
            AnnotUtils.refreshAnnotAppearance(mPdfViewCtrl.getContext(), annot);
            mPdfViewCtrl.update(annot, pageNum);

            if (raiseEvent) {
                raiseAnnotationModifiedEvent(annot, pageNum, bundle);
            }

            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(getIconKey(AnnotUtils.getAnnotType(annot)), icon);
            editor.apply();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    // Note, should not assume Annot is FreeText anymore, since Widgets now also support fonts
    protected void editFont(String pdftronFontName) {
        if (mAnnot == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "editFont");
        bundle.putString("fontName", pdftronFontName);
        bundle.putStringArray(KEYS, new String[]{"fontName"});
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }
        if (Utils.isNullOrEmpty(pdftronFontName)) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);

            int annotType = mAnnot.getType();

            String fontName;
            switch (annotType) {
                case Annot.e_Widget: {
                    Widget widget = new Widget(mAnnot);
                    Field field = widget.getField(); // TODO: bfung, re-setting the font might not be needed, check with core team
                    Font font = Font.create(mPdfViewCtrl.getDoc(), pdftronFontName, field.getValueAsString());
                    fontName = font.getName();
                    widget.setFont(font);
                    widget.refreshAppearance();
                    break;
                }
                case Annot.e_FreeText: {
                    FreeText textAnnot = new FreeText(mAnnot);
                    String fontDRName = "F0";

                    // Create a DR entry for embedding the font
                    Obj annotObj = textAnnot.getSDFObj();
                    Obj drDict = annotObj.putDict("DR");
                    Obj fontDict = drDict.putDict("Font");

                    // Embed the font
                    Font font = Font.create(mPdfViewCtrl.getDoc(), pdftronFontName, textAnnot.getContents());
                    fontDict.put(fontDRName, font.GetSDFObj());
                    fontName = font.getName();

                    boolean changed = FreeTextInfo.setFont(mPdfViewCtrl, textAnnot, pdftronFontName);
                    if (changed) {
                        refreshAppearanceImpl(mAnnot, mAnnotPageNum);
                    }
                    break;
                }
                default:
                    throw new RuntimeException("Annotation should not have font style.");
            }

            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);

            raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);

            // save font name with font if not saved already
            updateFontMap(mPdfViewCtrl.getContext(), annotType, pdftronFontName, fontName);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    protected void editFillColor(int color) {
        editFillColor(mAnnot, mAnnotPageNum, color, true);
    }

    protected void editFillColor(@Nullable Annot annot, int pageNum, int color, boolean raiseEvent) {
        if (annot == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "editFillColor");
        bundle.putInt("color", color);
        bundle.putStringArray(KEYS, new String[]{"color"});
        if (onInterceptAnnotationHandling(annot, bundle)) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc information
            // isn't thread safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            if (raiseEvent) {
                raiseAnnotationPreModifyEvent(annot, pageNum);
            }

            if (annot.isMarkup() && annot.getType() != Annot.e_FreeText) {
                Markup m = new Markup(annot);

                if (color == Color.TRANSPARENT) {
                    ColorPt emptyColorPt = new ColorPt(0, 0, 0, 0);
                    m.setInteriorColor(emptyColorPt, 0);
                } else {
                    ColorPt colorPt = Utils.color2ColorPt(color);
                    m.setInteriorColor(colorPt, 3);
                }
                SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt(getColorFillKey(AnnotUtils.getAnnotType(annot)), color);
                editor.apply();
            } else if (annot.getType() == Annot.e_FreeText) {
                FreeText freeText = new FreeText(annot);
                if (color == Color.TRANSPARENT) {
                    ColorPt emptyColorPt = new ColorPt(0, 0, 0, 0);
                    freeText.setColor(emptyColorPt, 0);
                } else {
                    ColorPt colorPt = Utils.color2ColorPt(color);
                    freeText.setColor(colorPt, 3);
                }
                SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt(getColorFillKey(AnnotUtils.getAnnotType(annot)), color);
                editor.apply();
            }

            refreshAppearanceImpl(annot, pageNum);
            mPdfViewCtrl.update(annot, pageNum);

            if (raiseEvent) {
                raiseAnnotationModifiedEvent(annot, pageNum, bundle);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    // Note, should not assume Annot is FreeText anymore, since Widgets now also support text size
    protected void editTextSize(float textSize) {
        if (mAnnot == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "editTextSize");
        bundle.putFloat("textSize", textSize);
        bundle.putStringArray(KEYS, new String[]{"textSize"});
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);

            int type = mAnnot.getType();

            switch (type) {
                case Annot.e_Widget: {
                    Widget widget = new Widget(mAnnot);
                    widget.setFontSize(textSize);
                    widget.refreshAppearance();
                    break;
                }
                case Annot.e_FreeText: {
                    FreeText freeText = new FreeText(mAnnot);
                    freeText.setFontSize(textSize);
                    freeText.refreshAppearance();

                    if (AnnotUtils.isCallout(freeText)) {
                        type = AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT;
                    } else if (AnnotUtils.isFreeTextDate(freeText)) {
                        type = AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_DATE;
                    } else if (AnnotUtils.isFreeTextSpacing(freeText)) {
                        type = AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_SPACING;
                    }
                    refreshAppearanceImpl(mAnnot, mAnnotPageNum);

                    // Try to get default rect
                    ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                    boolean isRightToLeft = false;
                    if (toolManager.isAutoResizeFreeText()) {
                        isRightToLeft = Utils.isRightToLeftString(freeText.getContents());
                    }

                    Rect defaultRect = null;
                    if (!toolManager.isDeleteEmptyFreeText() && toolManager.isAutoResizeFreeText()) {
                        defaultRect = FreeTextCreate.getDefaultRect(freeText);
                    }
                    resizeFreeText(freeText, freeText.getContentRect(), isRightToLeft, defaultRect);

                    // Let's recalculate the selection bounding box
                    buildAnnotBBox();
                    setCtrlPts();
                    break;
                }
                default:
                    throw new RuntimeException("Annotation should not have text size.");
            }

            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);

            raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);

            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putFloat(getTextSizeKey(type), textSize);
            editor.apply();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    protected void editHorizontalAlignment(int alignment) {
        if (mAnnot == null) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            if (mAnnot.getType() == Annot.e_FreeText) {
                FreeText freeText = new FreeText(mAnnot);
                raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);
                FreeTextAlignmentUtils.setHorizontalAlignment(freeText, alignment);
                refreshAppearanceImpl(mAnnot, mAnnotPageNum);
                mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
                raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum);
            }
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    protected void editVerticalAlignment(int alignment) {
        if (mAnnot == null) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            if (mAnnot.getType() == Annot.e_FreeText) {
                FreeText freeText = new FreeText(mAnnot);
                raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);
                FreeTextAlignmentUtils.setVerticalAlignment(freeText, alignment);
                refreshAppearanceImpl(mAnnot, mAnnotPageNum);
                mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
                raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum);
            }
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    protected void editFreeTextDateFormat(String dateFormat) {
        if (mAnnot == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "editFreeTextDateFormat");
        bundle.putString("format", dateFormat);
        bundle.putStringArray(KEYS, new String[]{"format"});
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }

        String newContent = null;
        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc
            // information isn't thread safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            if (mAnnot.getType() == Annot.e_FreeText) {
                FreeText freeText = new FreeText(mAnnot);
                String currentDate = freeText.getContents();
                String currentFormat = freeText.getCustomData(AnnotUtils.KEY_FreeTextDate);
                if (currentFormat != null) {
                    SimpleDateFormat formatter = new SimpleDateFormat(currentFormat, Locale.getDefault());
                    Date date = formatter.parse(currentDate);
                    SimpleDateFormat newFormat = new SimpleDateFormat(dateFormat, Locale.getDefault());
                    if (date != null) {
                        newContent = newFormat.format(date);
                        freeText.setCustomData(AnnotUtils.KEY_FreeTextDate, dateFormat);
                    }
                }
            }

            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(getDateFormatKey(AnnotUtils.getAnnotType(mAnnot)), dateFormat);
            editor.apply();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }

        if (newContent != null) {
            updateFreeText(newContent);
            setCtrlPts();
        }
    }

    protected void editThickness(float thickness) {
        editThickness(mAnnot, mAnnotPageNum, thickness, true);
    }

    protected void editThickness(@Nullable Annot annot, int pageNum, float thickness, boolean raiseEvent) {
        if (annot == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "editThickness");
        bundle.putFloat("thickness", thickness);
        bundle.putStringArray(KEYS, new String[]{"thickness"});
        if (onInterceptAnnotationHandling(annot, bundle)) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc
            // information isn't thread safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            int colorCompNum;
            if (annot.getType() == Annot.e_FreeText) {
                FreeText freeText = new FreeText(annot);
                colorCompNum = freeText.getLineColorCompNum();
            } else {
                colorCompNum = annot.getColorCompNum();
            }
            if (raiseEvent) {
                raiseAnnotationPreModifyEvent(annot, pageNum);
            }

            com.pdftron.pdf.Annot.BorderStyle bs = annot.getBorderStyle();
            double annotThickness = bs.getWidth();
            boolean canSetWidth = true;
            // if the annot color is transparent and already stored pdftron thickness value,
            // updates stored pdftron thickness value and don't set thickness
            if (colorCompNum == 0 && annotThickness == 0) {
                Obj storedThicknessObj = annot.getSDFObj().findObj(PDFTRON_THICKNESS);
                if (storedThicknessObj != null) {
                    annot.getSDFObj().putNumber(PDFTRON_THICKNESS, thickness);
                    canSetWidth = false;
                }
            }
            if (canSetWidth) {
                bs.setWidth(thickness);
                annot.setBorderStyle(bs);
                if (thickness == 0) {
                    annot.getSDFObj().erase("AP");
                }
            }
            if (annot.getType() == Annot.e_Ink && PressureInkUtils.isPressureSensitive(annot)) {
                PressureInkUtils.refreshCustomInkAppearanceForExistingAnnot(annot);
            } else {
                refreshAppearanceImpl(annot, pageNum);
            }
            mPdfViewCtrl.update(annot, pageNum);

            if (raiseEvent) {
                raiseAnnotationModifiedEvent(annot, pageNum, bundle);
            }

            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putFloat(getThicknessKey(AnnotUtils.getAnnotType(annot)), thickness);
            editor.apply();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    protected void editOpacity(float opacity) {
        editOpacity(mAnnot, mAnnotPageNum, opacity, true);
    }

    protected void editOpacity(@Nullable Annot annot, int pageNum, float opacity, boolean raiseEvent) {
        if (annot == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "editOpacity");
        bundle.putFloat("opacity", opacity);
        bundle.putStringArray(KEYS, new String[]{"opacity"});
        if (onInterceptAnnotationHandling(annot, bundle)) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc
            // information isn't thread safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            if (raiseEvent) {
                raiseAnnotationPreModifyEvent(annot, pageNum);
            }

            if (annot.isMarkup()) {
                Markup m = new Markup(annot);
                m.setOpacity(opacity);
            }
            refreshAppearanceImpl(annot, pageNum);
            mPdfViewCtrl.update(annot, pageNum);

            if (raiseEvent) {
                raiseAnnotationModifiedEvent(annot, pageNum, bundle);
            }

            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putFloat(getOpacityKey(AnnotUtils.getAnnotType(annot)), opacity);
            editor.apply();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    protected void editRuler(RulerItem rulerItem) {
        editRuler(mAnnot, mAnnotPageNum, rulerItem, true);
    }

    protected void editRuler(@Nullable Annot annot, int pageNum, RulerItem rulerItem, boolean raiseEvent) {
        if (annot == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "editRuler");
        bundle.putParcelable("rulerItem", rulerItem);
        bundle.putStringArray(KEYS, new String[]{"rulerItem"});
        if (onInterceptAnnotationHandling(annot, bundle)) {
            return;
        }
        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc
            // information isn't thread safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            if (AnnotUtils.isRuler(annot) ||
                    AnnotUtils.isPerimeterMeasure(annot) ||
                    AnnotUtils.isAreaMeasure(annot)) {
                if (raiseEvent) {
                    raiseAnnotationPreModifyEvent(annot, pageNum);
                }

                if (AnnotUtils.isRuler(annot)) {
                    Line line = new Line(annot);
                    RulerItem.removeRulerItem(line); // remove legacy key
                    RulerCreate.adjustContents(line, rulerItem, line.getStartPoint().x, line.getStartPoint().y,
                            line.getEndPoint().x, line.getEndPoint().y);
                } else if (AnnotUtils.isPerimeterMeasure(annot)) {
                    PolyLine polyLine = new PolyLine(annot);
                    ArrayList<Point> points = AnnotUtils.getPolyVertices(polyLine);
                    PerimeterMeasureCreate.adjustContents(polyLine, rulerItem, points);
                } else if (AnnotUtils.isAreaMeasure(annot)) {
                    Polygon polygon = new Polygon(annot);
                    ArrayList<Point> points = AnnotUtils.getPolyVertices(polygon);
                    AreaMeasureCreate.adjustContents(polygon, rulerItem, points);
                }

                AnnotUtils.refreshAnnotAppearance(mPdfViewCtrl.getContext(), annot);
                mPdfViewCtrl.update(annot, pageNum);

                if (raiseEvent) {
                    raiseAnnotationModifiedEvent(annot, pageNum, bundle);
                }

                SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
                SharedPreferences.Editor editor = settings.edit();
                editor.putFloat(getRulerBaseValueKey(AnnotUtils.getAnnotType(annot)), rulerItem.mRulerBase);
                editor.putString(getRulerBaseUnitKey(AnnotUtils.getAnnotType(annot)), rulerItem.mRulerBaseUnit);
                editor.putFloat(getRulerTranslateValueKey(AnnotUtils.getAnnotType(annot)), rulerItem.mRulerTranslate);
                editor.putString(getRulerTranslateUnitKey(AnnotUtils.getAnnotType(annot)), rulerItem.mRulerTranslateUnit);
                editor.apply();
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    protected void editRedactionOverlayText(String overlayText) {
        editRedactionOverlayText(mAnnot, mAnnotPageNum, overlayText, true);
    }

    protected void editRedactionOverlayText(@Nullable Annot annot, int pageNum, String overlayText, boolean raiseEvent) {
        if (annot == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "editRedactionOverlayText");
        bundle.putString("overlayText", overlayText);
        bundle.putStringArray(KEYS, new String[]{"overlayText"});
        if (onInterceptAnnotationHandling(annot, bundle)) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc
            // information isn't thread safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            if (raiseEvent) {
                raiseAnnotationPreModifyEvent(annot, pageNum);
            }

            Redaction redaction = new Redaction(annot);
            redaction.setOverlayText(overlayText);

            annot.refreshAppearance();
            mPdfViewCtrl.update(annot, pageNum);

            if (raiseEvent) {
                raiseAnnotationModifiedEvent(annot, pageNum, bundle);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    private void editTextSpacing() {
        if (mAnnot == null) {
            return;
        }

        float spacing = 0;
        if (Utils.isLollipop()) {
            spacing = mAnnotView != null && mAnnotView.getTextView() != null ?
                    mAnnotView.getTextView().getLetterSpacing() : 0;
        }
        final Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "editTextSpacing");
        bundle.putFloat("spacing", spacing);
        bundle.putStringArray(KEYS, new String[]{"spacing"});
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }
        if (mAnnotView != null && mAnnotView.getTextView() != null && mAnnot != null) {
            try {
                mPdfViewCtrl.docLock(true, new PDFViewCtrl.LockRunnable() {
                    @Override
                    public void run() throws Exception {
                        raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);

                        AnnotUtils.applyCustomFreeTextAppearance(
                                mPdfViewCtrl, mAnnotView.getTextView(),
                                mAnnot, mAnnotPageNum
                        );
                        // reload style as spacing is changed
                        mAnnotStyle = AnnotUtils.getAnnotStyle(mAnnot);

                        mPdfViewCtrl.update(mAnnot, mAnnotPageNum);

                        raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);
                    }
                });

                setCtrlPts();
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
    }

    // this is used for refresh appearance when editing style with the style picker
    protected void refreshAppearanceImpl(Annot annot, int pageNum) throws PDFNetException {
        if (null == annot) {
            return;
        }

        // for fill and sign we will create our own appearance
        if (mAnnotStyle != null && mAnnotStyle.isSpacingFreeText() && mAnnotView != null) {
            AnnotUtils.applyCustomFreeTextAppearance(mPdfViewCtrl,
                    mAnnotView.getTextView(),
                    annot, pageNum);
            resumeEditing(mAnnotView.getTextView(), false);
            mAnnotStyle = AnnotUtils.getAnnotStyle(annot);
        } else if (mAnnotStyle != null && mAnnotStyle.isBasicFreeText() &&
                mAnnotView != null && FreeTextCreate.sUseEditTextAppearance) {
            AnnotUtils.createCustomFreeTextAppearance(
                    mAnnotView.getTextView(),
                    mPdfViewCtrl,
                    annot,
                    pageNum,
                    mAnnotView.getTextView().getBoundingRect()
            );
        } else {
            AnnotUtils.refreshAnnotAppearance(mPdfViewCtrl.getContext(), annot);
        }
    }

    private void resumeEditing(AutoScrollEditText editText, boolean textEditingEnabled) {
        if (editText != null) {
            if (Utils.isLollipop()) {
                editText.addLetterSpacingHandle();
            }
            if (textEditingEnabled) {
                editText.requestFocus();
                editText.setCursorVisible(true);
            }
        }
    }

    /**
     * The overload implementation of {@link Tool#onRenderingFinished()}.
     */
    @Override
    public void onRenderingFinished() {
        super.onRenderingFinished();

        if (mInlineEditText != null && mInlineEditText.delayViewRemoval()) {
            mInlineEditText.removeView();
            mInlineEditText = null;

            if (!mHasOnCloseCalled) {
                android.os.Handler handler = new android.os.Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        RectF annotRect = getAnnotRect();
                        if (annotRect != null) {
                            showMenu(annotRect);
                        }
                    }
                }, 100);
            }
        }

        if (mInlineEditText != null && mInlineEditText.delaySetContents()) {
            mInlineEditText.setContents();
        }
    }

    /**
     * @return True if the free text annotation is editable.
     */
    public boolean isFreeTextEditing() {
        if (mInlineEditText != null) {
            return mInlineEditText.isEditing();
        }
        return false;
    }

    /**
     * The overload implementation of {@link InlineEditText.InlineEditTextListener#getInlineEditTextPosition()}.
     */
    @Override
    public RectF getInlineEditTextPosition() {
        RectF box = mBBox;
        if (mContentBox != null) {
            box = mContentBox;
        }
        int left = (int) (box.left + mCtrlRadius);
        int right = (int) (box.right - mCtrlRadius);
        int top = (int) (box.top + mCtrlRadius);
        int bottom = (int) (box.bottom - mCtrlRadius);

        // the max width of the edit text is the screen size, shrink
        // the edit text if necessary
        int screenWidth = Utils.getScreenWidth(mPdfViewCtrl.getContext());
        if (box.width() > screenWidth) {
            right = left + screenWidth;
        }

        return new RectF(left, top, right, bottom);
    }

    protected void saveAndQuitInlineEditText(boolean immediatelyRemoveView) {
        if (mRichTextViewModel != null) {
            mRichTextViewModel.onCloseToolbar();
        }
        if (mPdfViewCtrl.isAnnotationLayerEnabled()) {
            // if we are using separate annotation layer, always remove immediately
            immediatelyRemoveView = true;
        }
        mInEditMode = false;
        if (mInlineEditText != null) {
            final String contents = mInlineEditText.getContents();
            updateFreeText(contents);
            postSaveAndQuitInlineEditText(immediatelyRemoveView);
        }

        // save new edit mode in settings
        if (mUpdateFreeTextEditMode) {
            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(ANNOTATION_FREE_TEXT_PREFERENCE_EDITING, mCurrentFreeTextEditMode);
            editor.apply();
        }
    }

    private void postSaveAndQuitInlineEditText(boolean immediatelyRemoveView) {
        if (null == mInlineEditText) {
            return;
        }
        mInlineEditText.close(immediatelyRemoveView);
        addOldTools();

        mHideCtrlPts = false;
        setCtrlPts();
        mPdfViewCtrl.invalidate((int) Math.floor(mBBox.left), (int) Math.floor(mBBox.top), (int) Math.ceil(mBBox.right), (int) Math.ceil(mBBox.bottom));

        if (immediatelyRemoveView) {
            mInlineEditText = null;
            if (isQuickMenuShown()) {
                closeQuickMenu();
            }

            // show menu is delayed so that the view shift from the keyboard is gone
            android.os.Handler handler = new android.os.Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showMenu(getAnnotRect());
                }
            }, 300);
        }
    }

    /**
     * The overload implementation of {@link InlineEditText.InlineEditTextListener#toggleToFreeTextDialog(String)}.
     */
    @Override
    public void toggleToFreeTextDialog(String interImText) {
        mCurrentFreeTextEditMode = ANNOTATION_FREE_TEXT_PREFERENCE_DIALOG;
        mUpdateFreeTextEditMode = true;

        try {
            fallbackFreeTextDialog(interImText, false);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    private void fallbackFreeTextDialog(String contents, boolean disableToggleButton) throws PDFNetException {
        Bundle bundle = new Bundle();
        bundle.putString("contents", contents);
        bundle.putBoolean("disableToggleButton", disableToggleButton);
        bundle.putStringArray(KEYS, new String[]{"contents", "disableToggleButton"});
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }

        removeAnnotView();

        boolean enableSave = true;
        if (contents == null && mAnnot != null) {
            boolean shouldUnlockRead = false;
            try {
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;
                Markup m = new Markup(mAnnot);
                contents = m.getContents();
            } finally {
                if (shouldUnlockRead) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }
            enableSave = false;
        }
        mDialogFreeTextNote = new DialogFreeTextNote(mPdfViewCtrl, contents, enableSave);
        mDialogFreeTextNote.setHorizontalTextAlignment(mAnnotStyle.getHorizontalAlignment());
        mDialogFreeTextNote.setVerticalTextAlignment(mAnnotStyle.getVerticalAlignment());
        mDialogFreeTextNote.addTextWatcher(this);
        mDialogFreeTextNote.setAnnotNoteListener(this);
        mDialogFreeTextNote.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                prepareDialogFreeTextNoteDismiss();
            }
        });
        mDialogFreeTextNote.show();
        mStoredTimeStamp = System.currentTimeMillis();
        if (disableToggleButton) {
            mDialogFreeTextNote.disableToggleButton();
        }

        if (mInlineEditText == null) {
            // always add the inline text
            initInlineFreeTextEditing(contents);
        }
    }

    private void prepareDialogFreeTextNoteDismiss() {
        mInEditMode = false;
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        if (mAnnotButtonPressed == DialogInterface.BUTTON_POSITIVE) {
            if (mInlineEditText != null) {
                mInlineEditText.setContents(mDialogFreeTextNote.getNote());
            }
            saveAndQuitInlineEditText(true);

            // update editing free text annots preference
            if (mUpdateFreeTextEditMode) {
                SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt(ANNOTATION_FREE_TEXT_PREFERENCE_EDITING, mCurrentFreeTextEditMode);
                editor.apply();
            }

            // remove inline edit text and reshow control points
            if (mInlineEditText != null && mInlineEditText.isEditing()) {
                mInlineEditText.close(true);
                mInlineEditText = null;
            }
            if (!toolManager.isEditFreeTextOnTap()) {
                mHideCtrlPts = false;
                setCtrlPts();
                // show menu is delayed so that the view shift from the keyboard is gone
                android.os.Handler handler = new android.os.Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showMenu(getAnnotRect());
                    }
                }, 300);
            } else {
                unsetAnnot();
                closeQuickMenu();
                mNextToolMode = ToolMode.PAN;
            }
        } else if (mAnnotButtonPressed == DialogInterface.BUTTON_NEUTRAL) {
            // switch to inline editing
            mCurrentFreeTextEditMode = ANNOTATION_FREE_TEXT_PREFERENCE_INLINE;
            mUpdateFreeTextEditMode = true;

            // if inline editing is already attached to the view, show the cursor and
            // update the contents
            if (mInlineEditText != null) {
                mInlineEditText.setContents(mDialogFreeTextNote.getNote());
                // force show keyboard
                Utils.showSoftKeyboard(mPdfViewCtrl.getContext(), null);
            } else {
                // otherwise initialized inline editing
                try {
                    initInlineFreeTextEditing(mDialogFreeTextNote.getNote());
                    // force show keyboard
                    Utils.showSoftKeyboard(mPdfViewCtrl.getContext(), null);
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
            }
        } else {
            // if inline editing was initialized, remove the view and
            // reshow the annotation
            if (mAnnot != null && mInlineEditText != null && mInlineEditText.isEditing()) {
                mInlineEditText.close(true);
                mInlineEditText = null;

                // show annotation
                try {
                    mPdfViewCtrl.showAnnotation(mAnnot);
                    mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
                    mPdfViewCtrl.invalidate();
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
                Utils.deleteCacheFile(mPdfViewCtrl.getContext(), mCacheFileName);
            }
            if (!toolManager.isEditFreeTextOnTap()) {
                mHideCtrlPts = false;
                setCtrlPts();
                // show menu is delayed so that the view shift from the keyboard is gone
                android.os.Handler handler = new android.os.Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showMenu(getAnnotRect());
                    }
                }, 300);
            } else {
                unsetAnnot();
                closeQuickMenu();
                mNextToolMode = ToolMode.PAN;
            }

            if (mUpdateFreeTextEditMode) {
                SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt(ANNOTATION_FREE_TEXT_PREFERENCE_EDITING, mCurrentFreeTextEditMode);
                editor.apply();
            }
        }
        mAnnotButtonPressed = 0;
    }

    private void initInlineFreeTextEditing(String interimText) throws PDFNetException {
        if (mAnnot == null) {
            return;
        }

        removeAnnotView(false, true, false);

        if (interimText == null) {
            boolean shouldUnlockRead = false;
            try {
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;
                Markup m = new Markup(mAnnot);
                interimText = m.getContents();
            } finally {
                if (shouldUnlockRead) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }
        }
        boolean isRC = mAnnotStyle != null && mAnnotStyle.isRCFreeText();
        if (null == mRichTextViewModel) {
            FragmentActivity activity = ((ToolManager) mPdfViewCtrl.getToolManager()).getCurrentActivity();
            if (activity != null) {
                mRichTextViewModel = ViewModelProviders.of(activity).get(RichTextViewModel.class);
            }
        }
        if (mRichTextViewModel != null && isRC) {
            mRichTextViewModel.onOpenToolbar();
        }
        ToolManager toolManager = (ToolManager) (mPdfViewCtrl.getToolManager());
        boolean freeTextInlineToggleEnabled = toolManager.isfreeTextInlineToggleEnabled();
        if (mAnnotStyle != null && mAnnotStyle.isSpacingFreeText()) {
            freeTextInlineToggleEnabled = false;
        }
        mInlineEditText = new InlineEditText(
                mPdfViewCtrl,
                mAnnot,
                mAnnotPageNum,
                null,
                freeTextInlineToggleEnabled,
                isRC,
                this
        );
        if (AnnotUtils.hasRotation(mPdfViewCtrl, mAnnot)) {
            int rotation = AnnotUtils.getAnnotUIRotation(mPdfViewCtrl, mAnnot, mAnnotPageNum);
            mInlineEditText.getEditor().setRotation(rotation);
        }
        mInlineEditText.setRichTextViewModel(mRichTextViewModel);
        if (mAnnotStyle.hasTextAlignment()) {
            if (!toolManager.isAutoResizeFreeText()) {
                mInlineEditText.getEditText().setHorizontalTextAlignment(mAnnotStyle.getHorizontalAlignment());
                mInlineEditText.getEditText().setVerticalTextAlignment(mAnnotStyle.getVerticalAlignment());
            }
        }
        mInlineEditText.addTextWatcher(this);
        // keyboard shortcut
        mInlineEditText.getEditText().setAutoScrollEditTextListener(new AutoScrollEditText.AutoScrollEditTextListener() {
            @Override
            public boolean onKeyUp(int keyCode, KeyEvent event) {
                if (ShortcutHelper.isCommitText(keyCode, event)) {
                    // if DialogFreeTextNote is open then it swallows keys event, hence we only handle
                    // inline edit
                    saveAndQuitInlineEditText(false);
                    // hide soft keyboard
                    InputMethodManager imm = (InputMethodManager) mPdfViewCtrl.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(mPdfViewCtrl.getRootView().getWindowToken(), 0);
                    }
                }
                return true;
            }

            @Override
            public boolean onKeyPreIme(int keyCode, KeyEvent event) {
                return false;
            }
        });
        if (mAnnotStyle != null && mAnnotStyle.isSpacingFreeText()) {
            if (Utils.isLollipop()) {
                mInlineEditText.getEditText().setLetterSpacing(mAnnotStyle.getLetterSpacing());
                mInlineEditText.getEditText().addLetterSpacingHandle();
            }
            mInlineEditText.getEditText().setAutoScrollEditTextSpacingListener(new AutoScrollEditText.AutoScrollEditTextSpacingListener() {
                @Override
                public void onUp() {
                    editTextSpacing();
                    if (mInlineEditText != null) {
                        resumeEditing(mInlineEditText.getEditText(), true);
                    }
                }
            });
        }
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.hideAnnotation(mAnnot);
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);

            mHideCtrlPts = true;
            mPdfViewCtrl.invalidate((int) mBBox.left - 1, (int) mBBox.top - 1, (int) mBBox.right + 1, (int) mBBox.bottom + 1);

            setupFreeTextProperties(mInlineEditText);

            // set edit text contents
            if (isRC) {
                mInlineEditText.setHTMLContents(mAnnotStyle.getTextHTMLContent());
            } else {
                mInlineEditText.setDelaySetContents(interimText);
            }
            mStoredTimeStamp = System.currentTimeMillis();

            // give user a chance to intercept the edit text
            Bundle bundle = new Bundle();
            bundle.putString(METHOD_FROM, "initInlineFreeTextEditing");
            bundle.putString("text", interimText);
            bundle.putStringArray(KEYS, new String[]{"text"});
            onInterceptAnnotationHandling(mAnnot, bundle);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
    }

    // read lock is required
    protected void setupFreeTextProperties(@NonNull InlineEditText inlineEditText) throws PDFNetException {
        if (null == mAnnot) {
            return;
        }
        // change style of text to match style of free text annot
        // font size
        FreeText freeText = new FreeText(mAnnot);
        int fontSize = (int) freeText.getFontSize();
        if (fontSize == 0) {
            fontSize = 12;
        }
        inlineEditText.setTextSize(fontSize);

        // opacity
        Markup m = new Markup(mAnnot);
        int alpha = (int) (m.getOpacity() * 0xFF);

        // font color
        int r, g, b;
        if (freeText.getTextColorCompNum() == 3) {
            ColorPt fillColorPt = freeText.getTextColor();
            r = (int) (Math.round(fillColorPt.get(0) * 255));
            g = (int) (Math.round(fillColorPt.get(1) * 255));
            b = (int) (Math.round(fillColorPt.get(2) * 255));
            int fontColor = Color.argb(alpha, r, g, b);
            inlineEditText.setTextColor(fontColor);
        }

        // default fill color is white, use free text background
        // color if it has one at full opacity.
        int fillColor = Color.TRANSPARENT;
        if (freeText.getColorCompNum() == 3) {
            ColorPt fillColorPt = freeText.getColorAsRGB();
            r = (int) (Math.round(fillColorPt.get(0) * 255));
            g = (int) (Math.round(fillColorPt.get(1) * 255));
            b = (int) (Math.round(fillColorPt.get(2) * 255));
            fillColor = Color.argb(alpha, r, g, b);
        }
        inlineEditText.setBackgroundColor(fillColor);
    }

    // read lock is required
    protected void setupTextFieldProperties(@NonNull InlineEditText inlineEditText) throws PDFNetException {
        if (null == mAnnot || !getToolManager().isTextFieldCustomAppearanceEnabled()) {
            return;
        }
        // change style of text to match style of text widget
        // font size
        TextWidget textWidget = new TextWidget(mAnnot);
        int fontSize = (int) textWidget.getFontSize();
        if (fontSize == 0) {
            fontSize = 12;
        }
        inlineEditText.setTextSize(fontSize);

        // opacity
        Markup m = new Markup(mAnnot);
        int alpha = (int) (m.getOpacity() * 0xFF);

        // font color
        int r, g, b;
        if (textWidget.getTextColorCompNum() == 3) {
            ColorPt textColorPt = textWidget.getTextColor();
            r = (int) (Math.round(textColorPt.get(0) * 255));
            g = (int) (Math.round(textColorPt.get(1) * 255));
            b = (int) (Math.round(textColorPt.get(2) * 255));
            int fontColor = Color.argb(alpha, r, g, b);
            inlineEditText.setTextColor(fontColor);
        }

        double backgroundOpacity = AnnotUtils.getFormFieldBackgroundOpacity(mAnnot);
        ColorPt fillColorPt = textWidget.getBackgroundColor();
        r = (int) (Math.round(fillColorPt.get(0) * 255));
        g = (int) (Math.round(fillColorPt.get(1) * 255));
        b = (int) (Math.round(fillColorPt.get(2) * 255));
        alpha = (int) (Math.round(backgroundOpacity * 255));
        int fillColor = Color.argb(alpha, r, g, b);
        inlineEditText.setBackgroundColor(fillColor);
    }

    private void resizeFreeText(FreeText freeText, Rect adjustedAnnotRect, boolean isRightToLeft, @Nullable Rect defaultRect) throws PDFNetException {
        if (mAnnotStyle != null && mAnnotStyle.isSpacingFreeText()) {
            return;
        }
        double left = adjustedAnnotRect.getX1();
        double top = adjustedAnnotRect.getY1();
        double right = adjustedAnnotRect.getX2();
        double bottom = adjustedAnnotRect.getY2();

        boolean isCallout = AnnotUtils.isCallout(freeText);
        Rect temp = freeText.getContentRect();

        // set the new annot rect
        if (((ToolManager) (mPdfViewCtrl.getToolManager())).isAutoResizeFreeText()) {
            double[] pt1s = mPdfViewCtrl.convPagePtToScreenPt(left, top, mAnnotPageNum);
            double[] pt2s = mPdfViewCtrl.convPagePtToScreenPt(right, bottom, mAnnotPageNum);
            double scLeft = pt1s[0];
            double scTop = pt1s[1];
            double scRight = pt2s[0];
            double scBottom = pt2s[1];

            // find top left (LTR) or top right (RTL)
            double x = Math.min(scLeft, scRight);
            double y = Math.min(scTop, scBottom);
            if (isRightToLeft) {
                x = Math.max(scLeft, scRight);
            }

            double[] pt3s = mPdfViewCtrl.convScreenPtToPagePt(x, y, mAnnotPageNum);
            Point targetPoint = new Point(pt3s[0], pt3s[1]);

            Rect bbox = FreeTextCreate.getTextBBoxOnPage(mPdfViewCtrl, mAnnotPageNum, targetPoint);
            if (bbox != null) {
                if (isCallout) {
                    freeText.setRect(bbox);
                    freeText.setContentRect(bbox);
                } else {
                    freeText.resize(bbox);
                }
                freeText.refreshAppearance();

                Rect resizeRect = FreeTextCreate.calcFreeTextBBox(mPdfViewCtrl, freeText, mAnnotPageNum,
                        isRightToLeft, targetPoint);
                if (isCallout) {
                    resizeCallout(freeText, temp, resizeRect);
                } else {
                    if (defaultRect != null) {
                        // get max box
                        resizeRect.setX2(resizeRect.getX1() + Math.max(resizeRect.getWidth(), defaultRect.getWidth()));
                        resizeRect.setY1(resizeRect.getY2() - Math.max(resizeRect.getHeight(), defaultRect.getHeight()));
                    }
                    freeText.resize(resizeRect);
                }
                freeText.refreshAppearance();
            }
        } else {
            if (isCallout) {
                resizeCallout(freeText, temp, adjustedAnnotRect);
            } else {
                freeText.setRect(adjustedAnnotRect);
            }
        }
    }

    protected void resizeCallout(Markup markup,
            Rect originalAnnotRect,
            Rect adjustedAnnotRect) throws PDFNetException {
        if (mAnnotIsFreeText) {
            adjustExtraFreeTextProps(originalAnnotRect, adjustedAnnotRect);
        }
        markup.setRect(adjustedAnnotRect);
        markup.setContentRect(adjustedAnnotRect);
        markup.refreshAppearance();
        setCtrlPts(); // update ctrl points
    }

    private void updateFreeText(String contents) {
        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "updateFreeText");
        bundle.putStringArray(KEYS, new String[]{"contents"});
        bundle.putString("contents", contents);
        if (mAnnot == null || onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }
        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc
            // information isn't thread safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            final FreeText freeText = new FreeText(mAnnot);

            // Try to get default rect
            Rect defaultRect = null;
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            if (!toolManager.isDeleteEmptyFreeText() && toolManager.isAutoResizeFreeText()) {
                defaultRect = FreeTextCreate.getDefaultRect(freeText);
            }

            String oldContents = freeText.getContents();
            if (!Utils.isNullOrEmpty(contents)) {
                raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);
                freeText.setContents(contents);

                boolean isRightToLeft = false;
                if (toolManager.isAutoResizeFreeText()) {
                    isRightToLeft = Utils.isRightToLeftString(contents);
                    if (isRightToLeft) {
                        freeText.setQuaddingFormat(2); // right justification
                    } else if (Utils.isLeftToRightString(contents)) {
                        freeText.setQuaddingFormat(0);
                    }
                } else {
                    FreeTextAlignmentUtils.setHorizontalAlignment(freeText, mAnnotStyle.getHorizontalAlignment());
                    FreeTextAlignmentUtils.setVerticalAlignment(freeText, mAnnotStyle.getVerticalAlignment());
                }

                // resize the edit text to accommodate the contents
                // but only if the inline edit text was used

                com.pdftron.pdf.Rect contentRect = freeText.getContentRect();
                RectF contentBox = new RectF(
                        (float) contentRect.getX1(),
                        (float) contentRect.getY1(),
                        (float) contentRect.getX2(),
                        (float) contentRect.getY2());
                // adjust the original annots bounding box using the
                // new height
                double right, left, top, bottom;
                right = contentBox.right;
                left = contentBox.left;
                top = contentBox.bottom;
                bottom = contentBox.top;

                if (mInlineEditText != null) {
                    // determine the rotation of the page based on the users
                    // perspective
                    int pageRotation = mPdfViewCtrl.getDoc().getPage(mAnnotPageNum).getRotation();
                    int viewRotation = mPdfViewCtrl.getPageRotation();
                    int annotRotation = ((pageRotation + viewRotation) % 4) * 90;

                    // get the edit texts height and width
                    EditText editText = mInlineEditText.getEditText();
                    int editTextHeight = editText.getHeight();
                    int editTextWidth = editText.getWidth();

                    // get the annotations original width (which can be
                    // its height depending on the page & view rotation
                    float annotBBoxWidth = contentBox.width();
                    if (annotRotation == 90 || annotRotation == 270) {
                        annotBBoxWidth = contentBox.height();
                    }

                    // calculate the pixels to page units conversion
                    // use this to calculate the height of the edit text in page
                    // space
                    float convRatio = annotBBoxWidth / editTextWidth;
                    int heightInPageUnits = (int) (editTextHeight * convRatio);

                    if (annotRotation == 0) {
                        bottom = contentBox.bottom - heightInPageUnits;
                        if (bottom > contentBox.top) {
                            bottom = contentBox.top;
                        }
                    } else if (annotRotation == 90) {
                        right = contentBox.left + heightInPageUnits;
                        if (right < contentBox.right) {
                            right = contentBox.right;
                        }
                    } else if (annotRotation == 180) {
                        top = contentBox.top + heightInPageUnits;
                        if (top < contentBox.bottom) {
                            top = contentBox.bottom;
                        }
                    } else {
                        left = contentBox.right - heightInPageUnits;
                        if (left > contentBox.left) {
                            left = contentBox.left;
                        }
                    }
                    boolean canRefreshAppearance = true;
                    if (AnnotUtils.isBasicFreeText(freeText) && FreeTextCreate.sUseEditTextAppearance) {
                        // for basic freetext using client appearance, skip core refresh
                        canRefreshAppearance = false;
                    }
                    if (canRefreshAppearance) {
                        freeText.refreshAppearance();
                    }
                }
                Rect adjustedAnnotRect = new Rect(left, top, right, bottom);
                adjustedAnnotRect.normalize();
                resizeFreeText(freeText, adjustedAnnotRect, isRightToLeft, defaultRect);

                // re-embed font if we originally embedded it
                // check if we embedded the font
                String fontName = "";
                Obj freeTextObj = freeText.getSDFObj();
                Obj drDict = freeTextObj.findObj("DR");
                if (drDict != null && drDict.isDict()) {
                    Obj fontDict = drDict.findObj("Font");
                    if (fontDict != null && fontDict.isDict()) {
                        DictIterator fItr = fontDict.getDictIterator();
                        if (fItr.hasNext()) {
                            Font f = new Font(fItr.value());
                            fontName = f.getName();
                        }
                    }
                }

                // if we have embedded the font, use the name to
                // get the PDFTron name
                if (!fontName.equals("")) {

                    // find a fontName and pdftronFontName match if possible
                    String pdftronFontName = Tool.findPDFTronFontName(mPdfViewCtrl.getContext(), fontName);

                    // if there was a match between the PDFTron Name and
                    // the font name
                    if (!pdftronFontName.equals("")) {
                        String fontDRName = "F0";

                        // Create a DR entry for embedding the font
                        Obj annotObj = freeText.getSDFObj();
                        drDict = annotObj.putDict("DR");
                        Obj fontDict = drDict.putDict("Font");

                        // Embed the font
                        Font font = Font.create(mPdfViewCtrl.getDoc(), pdftronFontName, contents);
                        fontDict.put(fontDRName, font.GetSDFObj());
                    } else {
                        // use default
                        // Set DA string
                        String DA = freeText.getDefaultAppearance();
                        int slashPosition = DA.indexOf("/", 0);

                        // if DR string contains '/' which it always should.
                        if (slashPosition > 0) {
                            String beforeSlash = DA.substring(0, slashPosition);
                            String afterSlash = DA.substring(slashPosition);
                            String afterFont = afterSlash.substring(afterSlash.indexOf(" "));
                            String updatedDA = beforeSlash + "/helv" + afterFont;

                            freeText.setDefaultAppearance(updatedDA);
                        }
                    }
                }

                buildAnnotBBox();
                mPdfViewCtrl.showAnnotation(mAnnot);
                mAnnot.refreshAppearance();

                if (mInlineEditText != null) {
                    // for fill and sign we will create our own appearance
                    if (mInlineEditText.getRichEditor().getVisibility() == View.VISIBLE) {
                        AnnotUtils.createRCFreeTextAppearance(
                                mInlineEditText.getRichEditor(),
                                mPdfViewCtrl,
                                mAnnot,
                                mAnnotPageNum,
                                mAnnotStyle
                        );
                    } else if (mAnnotStyle.isSpacingFreeText()) {
                        AnnotUtils.applyCustomFreeTextAppearance(mPdfViewCtrl,
                                mInlineEditText.getEditText(),
                                mAnnot, mAnnotPageNum);
                        // reload the style as contents changed
                        mAnnotStyle = AnnotUtils.getAnnotStyle(mAnnot);
                    } else if (mAnnotStyle.isBasicFreeText() && FreeTextCreate.sUseEditTextAppearance) {
                        AnnotUtils.createCustomFreeTextAppearance(
                                mInlineEditText.getEditText(),
                                mPdfViewCtrl,
                                mAnnot,
                                mAnnotPageNum,
                                mInlineEditText.getEditText().getBoundingRect()
                        );
                        // reload style as content is changed
                        mAnnotStyle = AnnotUtils.getAnnotStyle(mAnnot);
                    }
                }

                mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
                raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);
                Utils.deleteCacheFile(mPdfViewCtrl.getContext(), mCacheFileName);
            } else {
                if (toolManager.isDeleteEmptyFreeText()) {
                    // if the free text annotation is an empty string, delete it
                    raiseAnnotationPreRemoveEvent(mAnnot, mAnnotPageNum);
                    Page page = mPdfViewCtrl.getDoc().getPage(mAnnotPageNum);
                    mAnnot = AnnotUtils.safeDeleteAnnotAndUpdate(mPdfViewCtrl, page, mAnnot, mAnnotPageNum);

                    // make sure to raise remove event after mPdfViewCtrl.update and before unsetAnnot
                    raiseAnnotationRemovedEvent(mAnnot, mAnnotPageNum);
                    Utils.deleteCacheFile(mPdfViewCtrl.getContext(), mCacheFileName);
                    if (sDebug) Log.d(TAG, "update free text");

                    unsetAnnot();
                } else {
                    // allow keeping empty free text
                    if (!Utils.isNullOrEmpty(oldContents)) {
                        // remove old content
                        raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);
                        // try to restore default FreeText rect
                        if (defaultRect != null) {
                            freeText.setRect(defaultRect);
                        }
                        freeText.setContents("");
                        freeText.refreshAppearance();
                        raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);
                    }
                    mPdfViewCtrl.showAnnotation(mAnnot);
                    mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
                }
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            dismissUpdatingFreeText();
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    private void dismissUpdatingFreeText() {
        if (mInlineEditText != null) {
            mInlineEditText.close(true);
        }
        if (mPdfViewCtrl == null || mAnnot == null) {
            return;
        }
        try {
            mPdfViewCtrl.showAnnotation(mAnnot);
            mAnnot.refreshAppearance();
            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        // no need to save as we cannot handle it
        Utils.deleteCacheFile(mPdfViewCtrl.getContext(), mCacheFileName);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (mPdfViewCtrl == null || mAnnot == null) {
            return;
        }

        long currentTimeStamp = System.currentTimeMillis();
        if (currentTimeStamp - mStoredTimeStamp > 3000) {
            mStoredTimeStamp = currentTimeStamp;
            if (s != null && s.length() > 0) {
                Bundle bundle = new Bundle();
                bundle.putStringArray(KEYS, new String[]{"contents"});
                bundle.putCharSequence("contents", s);
                if (onInterceptAnnotationHandling(mAnnot, bundle)) {
                    return;
                }
                try {
                    FreeTextCacheStruct freeTextCacheStruct = new FreeTextCacheStruct();
                    freeTextCacheStruct.contents = s.toString();
                    freeTextCacheStruct.pageNum = mAnnotPageNum;
                    Rect rect = mPdfViewCtrl.getScreenRectForAnnot(mAnnot, mAnnotPageNum);
                    freeTextCacheStruct.x = (float) (Math.min(rect.getX1(), rect.getX2()));
                    freeTextCacheStruct.y = (float) (Math.min(rect.getX2(), rect.getY2()));
                    AnnotUtils.saveFreeTextCache(freeTextCacheStruct, mPdfViewCtrl);
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    /**
     * The overload implementation of {@link Tool#getModeAHLabel()}.
     */
    @Override
    protected int getModeAHLabel() {
        if (mStamperToolSelected) {
            return AnalyticsHandlerAdapter.LABEL_QM_STAMPERSELECT;
        }
        return super.getModeAHLabel();
    }

    /**
     * Returns which effective control point is closest to the specified coordinate.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @return The effective control point which can be one of this
     * {@link #e_unknown},
     * {@link #e_moving},
     * {@link #e_ll},
     * {@link #e_lm},
     * {@link #e_lr},
     * {@link #e_mr},
     * {@link #e_ur},
     * {@link #e_um},
     * {@link #e_ul},
     * {@link #e_ml}
     */
    public int getEffectCtrlPointId(float x, float y) {
        if (mHandleEffCtrlPtsDisabled) {
            return e_unknown;
        }

        int effCtrlPtId = e_unknown;
        float thresh = mCtrlRadius * 2.25f;
        float shortest_dist = -1;
        for (int i = 0; i < RECTANGULAR_CTRL_PTS_CNT; ++i) {
            if (isAnnotResizable()) {
                // Sticky note and text markup cannot be re-sized
                float s = getVisualCtrlPts()[i].x;
                float t = getVisualCtrlPts()[i].y;

                float dist = (x - s) * (x - s) + (y - t) * (y - t);
                dist = (float) Math.sqrt(dist);
                if (dist <= thresh && (dist < shortest_dist || shortest_dist < 0)) {
                    effCtrlPtId = i;
                    shortest_dist = dist;
                }
            }
        }

        // Check if hit within the bounding box without hitting any control point.
        // Note that text markup cannot be moved.
        if (isSupportMove() && effCtrlPtId == e_unknown && mBBox.contains(x, y)) {
            effCtrlPtId = e_moving;
        }

        return effCtrlPtId;
    }

    protected PointF[] getVisualCtrlPts() {
        if (mSelectionBoxMargin > 0) {
            return mCtrlPtsInflated;
        }
        return mCtrlPts;
    }

    /**
     * @return True if control points are hidden
     */
    public boolean isCtrlPtsHidden() {
        return mHideCtrlPts;
    }

    @Override
    public void onChangeAnnotThickness(float thickness, boolean done) {
        if (mAnnotView != null) {
            mAnnotView.updateThickness(thickness);
        }
        if (done) {
            // change border thickness
            editThickness(thickness);
            updateAnnotViewBitmap();
        }
    }

    @Override
    public void onChangeAnnotBorderStyle(ShapeBorderStyle borderStyle) {
        if (mAnnotView != null) {
            mAnnotView.updateBorderStyle(borderStyle);
        }
        editBorderStyle(borderStyle);
    }

    @Override
    public void onChangeAnnotLineStyle(LineStyle lineStyle) {
        if (mAnnotView != null) {
            mAnnotView.updateLineStyle(lineStyle);
        }
        editLineStyle(lineStyle);
    }

    @Override
    public void onChangeAnnotLineStartStyle(LineEndingStyle lineStartStyle) {
        if (mAnnotView != null) {
            mAnnotView.updateLineStartStyle(lineStartStyle);
        }
        editLineStartStyle(lineStartStyle);
    }

    @Override
    public void onChangeAnnotLineEndStyle(LineEndingStyle lineEndStyle) {
        if (mAnnotView != null) {
            mAnnotView.updateLineEndStyle(lineEndStyle);
        }
        editLineEndStyle(lineEndStyle);
    }

    @Override
    public void onChangeTextAlignment(int horizontalAlignment, int verticalAlignment) {

        if (mAnnotView != null) {
            mAnnotView.updateAlignment(horizontalAlignment, verticalAlignment);
        }
        editHorizontalAlignment(horizontalAlignment);
        editVerticalAlignment(verticalAlignment);
        updateAnnotViewBitmap();
    }

    @Override
    public void onChangeAnnotTextSize(float textSize, boolean done) {
        if (mAnnotView != null) {
            mAnnotView.updateTextSize(textSize);
        }
        if (done) {
            editTextSize(textSize);
            updateAnnotViewBitmap();
        }
    }

    @Override
    public void onChangeAnnotOpacity(float opacity, boolean done) {
        if (mAnnotView != null) {
            mAnnotView.updateOpacity(opacity);
        }
        if (done) {
            editOpacity(opacity);
            updateAnnotViewBitmap();
        }
    }

    @Override
    public void onChangeAnnotStrokeColor(int color) {
        if (mAnnotView != null) {
            mAnnotView.updateColor(color);
        }

        editColor(color);
        updateAnnotViewBitmap();
        if (mAnnot != null) {
            // check annot color and set quick menu style color
            try {
                if (mAnnot.getType() == Annot.e_Square || mAnnot.getType() == Annot.e_Circle) {
                    Markup markup = new Markup(mAnnot);
                    ColorPt fillColorPt = markup.getInteriorColor();
                    int fillColor = Utils.colorPt2color(fillColorPt);
                    if (fillColor == Color.TRANSPARENT) {
                        updateQuickMenuStyleColor(color);
                    }
                } else {
                    updateQuickMenuStyleColor(color);
                }

                raiseAnnotStyleChange();
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
    }

    @Override
    public void onChangeAnnotFillColor(int color) {
        if (mAnnotView != null) {
            mAnnotView.updateFillColor(color);
        }
        editFillColor(color);
        updateAnnotViewBitmap();
        // if has fill need to check fill first
        if (color != Color.TRANSPARENT) {
            updateQuickMenuStyleColor(color);
        }
        raiseAnnotStyleChange();
    }

    @Override
    public void onChangeAnnotIcon(String icon) {
        if (mAnnotView != null) {
            mAnnotView.updateIcon(icon);
        }
        editIcon(icon);
        updateAnnotViewBitmap();
    }

    @Override
    public void onChangeAnnotFont(FontResource font) {
        if (mAnnotView != null) {
            mAnnotView.updateFont(font);
        }
        editFont(font.getPDFTronName());
        updateAnnotViewBitmap();
    }

    @Override
    public void onChangeAnnotTextColor(int textColor) {
        if (mAnnotView != null) {
            mAnnotView.updateTextColor(textColor);
        }
        editTextColor(textColor);
        updateAnnotViewBitmap();
        raiseAnnotStyleChange();
    }

    @Override
    public void onChangeRulerProperty(RulerItem rulerItem) {
        if (mAnnotView != null) {
            mAnnotView.updateRulerItem(rulerItem);
        }

        editRuler(rulerItem);
    }

    @Override
    public void onChangeOverlayText(String overlayText) {
        editRedactionOverlayText(overlayText);
    }

    @Override
    public void onChangeSnapping(boolean snap) {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        toolManager.setSnappingEnabledForMeasurementTools(snap);
    }

    @Override
    public void onChangeRichContentEnabled(boolean enabled) {

    }

    @Override
    public void onChangeDateFormat(String dateFormat) {
        if (mAnnotStyle != null && mAnnotStyle.isDateFreeText() && dateFormat != null) {
            editFreeTextDateFormat(dateFormat);
        }
    }

    /**
     * @return True if any annot is selected.
     * Note that in {@link AnnotEditRectGroup} multiple annots can be selected
     * while {@link Tool#mAnnot} is null
     */
    @Override
    public boolean hasAnnotSelected() {
        return mAnnot != null;
    }

    private void redactAnnot() {
        if (mAnnot == null) {
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "redactAnnot");
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            Redaction redactAnnot = new Redaction(mAnnot);

            // extract information out of the redaction annotation
            // then remove it
            ArrayList<Redactor.Redaction> arr = AnnotUtils.getRedactionArray(redactAnnot, mAnnotPageNum);

            UndoRedoManager undoRedoManager = toolManager.getUndoRedoManger();

            JSONObject jsonObject = new JSONObject();
            if (undoRedoManager != null) {
                jsonObject = undoRedoManager.getAnnotSnapshot(mAnnot, mAnnotPageNum);
            }

            deleteAnnot();

            AnnotUtils.applyRedaction(mPdfViewCtrl, redactAnnot, arr);

            mPdfViewCtrl.update(true);

            if (undoRedoManager != null) {
                undoRedoManager.onRedaction(jsonObject);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
        toolManager.raiseFullSaveRequired();
    }

    private void playSoundAnnot() {
        if (mAnnot == null) {
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "playSoundAnnot");
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        FragmentActivity activity = toolManager.getCurrentActivity();
        if (activity == null) {
            return;
        }

        String audioPath = activity.getCacheDir().getAbsolutePath();
        audioPath += "/audiorecord.out";
        Integer sampleRate = null;
        int encodingBitRate = 8;
        int numChannel = 1;

        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;

            Sound sound = new Sound(mAnnot);
            Obj soundStream = sound.getSoundStream();
            if (soundStream != null) {
                Obj item = soundStream.findObj("R");
                if (item != null && item.isNumber()) {
                    sampleRate = (int) item.getNumber();
                }
                item = soundStream.findObj("B");
                if (item != null && item.isNumber()) {
                    encodingBitRate = (int) item.getNumber();
                }
                item = soundStream.findObj("C");
                if (item != null && item.isNumber()) {
                    numChannel = (int) item.getNumber();
                }

                Filter soundFilter = soundStream.getDecodedStream();
                soundFilter.writeToFile(audioPath, false);
            }
        } catch (Exception e) {
            audioPath = null;
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }

        mNextToolMode = ToolMode.PAN;

        if (!Utils.isNullOrEmpty(audioPath) && sampleRate != null) {
            encodingBitRate = encodingBitRate == 16 ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
            numChannel = numChannel == 2 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;

            toolManager.getSoundManager().createSoundView(mPdfViewCtrl, audioPath, sampleRate, encodingBitRate, numChannel).show();
        }
    }

    protected void calibration() {
        if (mAnnot == null || mPdfViewCtrl == null) {
            return;
        }
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        FragmentActivity activity = toolManager.getCurrentActivity();
        if (activity == null) {
            return;
        }
        final RulerItem rulerItem = MeasureUtils.getRulerItemFromAnnot(mAnnot);
        if (null == rulerItem) {
            return;
        }
        CalibrateDialog dialog = CalibrateDialog.newInstance(mAnnot.__GetHandle(), mAnnotPageNum, rulerItem.mRulerTranslateUnit);
        dialog.setStyle(DialogFragment.STYLE_NO_TITLE, toolManager.getTheme());
        dialog.show(activity.getSupportFragmentManager(), CalibrateDialog.TAG);

        CalibrateViewModel viewModel = ViewModelProviders.of(activity).get(CalibrateViewModel.class);
        viewModel.observeOnComplete(activity, new Observer<Event<CalibrateResult>>() {
            @Override
            public void onChanged(@Nullable Event<CalibrateResult> resultEvent) {
                if (resultEvent != null && !resultEvent.hasBeenHandled()) {
                    CalibrateResult result = resultEvent.getContentIfNotHandled();
                    RulerItem newRulerItem = null;
                    Annot annot = null;
                    int pageNum = -1;
                    if (result != null) {
                        annot = Annot.__Create(result.annot, mPdfViewCtrl.getDoc());
                        pageNum = result.page;
                    }
                    if (result != null && result.userInput != null) {
                        Bundle bundle = new Bundle();
                        bundle.putString(METHOD_FROM, "calibration");
                        bundle.putParcelable("calibrateResult", result);
                        bundle.putStringArray(KEYS, new String[]{"calibrateResult"});
                        if (onInterceptAnnotationHandling(annot, bundle)) {
                            return;
                        }
                        boolean shouldUnlock = false;
                        try {
                            // Locks the document first as accessing annotation/doc
                            // information isn't thread safe.
                            mPdfViewCtrl.docLock(true);
                            shouldUnlock = true;

                            raiseAnnotationPreModifyEvent(annot, pageNum);

                            rulerItem.mRulerTranslateUnit = result.worldUnit;
                            newRulerItem = MeasureUtils.calibrate(annot, rulerItem, result.userInput);

                            AnnotUtils.refreshAnnotAppearance(mPdfViewCtrl.getContext(), annot);
                            mPdfViewCtrl.update(annot, pageNum);

                            raiseAnnotationModifiedEvent(annot, pageNum, bundle);
                        } catch (Exception e) {
                            AnalyticsHandlerAdapter.getInstance().sendException(e);
                        } finally {
                            if (shouldUnlock) {
                                mPdfViewCtrl.docUnlock();
                            }
                        }
                        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                        toolManager.selectAnnot(annot, pageNum);
                        if (newRulerItem != null) {
                            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
                            SharedPreferences.Editor editor = settings.edit();
                            // update all measurement types
                            int[] types = new int[]{
                                    AnnotStyle.CUSTOM_ANNOT_TYPE_RULER,
                                    AnnotStyle.CUSTOM_ANNOT_TYPE_PERIMETER_MEASURE,
                                    AnnotStyle.CUSTOM_ANNOT_TYPE_AREA_MEASURE,
                                    AnnotStyle.CUSTOM_ANNOT_TYPE_RECT_AREA_MEASURE
                            };
                            for (int type : types) {
                                editor.putFloat(getRulerBaseValueKey(type), newRulerItem.mRulerBase);
                                editor.putString(getRulerBaseUnitKey(type), newRulerItem.mRulerBaseUnit);
                                editor.putFloat(getRulerTranslateValueKey(type), newRulerItem.mRulerTranslate);
                                editor.putString(getRulerTranslateUnitKey(type), newRulerItem.mRulerTranslateUnit);
                            }
                            editor.apply();
                        }
                    } else {
                        // action cancelled
                        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                        toolManager.selectAnnot(annot, pageNum);
                    }
                }
            }
        });
    }

    protected QuickMenu createImageCropperMenu() {
        QuickMenu quickMenu = new QuickMenu(mPdfViewCtrl);
        quickMenu.inflate(R.menu.view_image_crop);
        quickMenu.initMenuEntries();
        return quickMenu;
    }

    protected void hideImageCropper() {
        if (mAnnotView != null) {
            mAnnotView.setCropMode(false);
            if (mRotateHandle != null) {
                mRotateHandle.setVisibility(View.VISIBLE);
            }
            // enable interaction for PDFViewCtrl
            mPdfViewCtrl.setInteractionEnabled(true);
            showMenu(getAnnotRect());
        }
    }

    protected void showImageCropper() {
        if (mAnnotView != null) {
            mAnnotView.setCropMode(true);
            if (mRotateHandle != null) {
                mRotateHandle.setVisibility(View.GONE);
            }
            // disable interaction for PDFViewCtrl
            mPdfViewCtrl.setInteractionEnabled(false);

            // quick menu
            QuickMenu quickMenu = createImageCropperMenu();
            showMenu(getAnnotRect(), quickMenu);

            mAnnotView.getCropImageView().setCropImageViewListener(new PTCropImageView.CropImageViewListener() {
                @Override
                public void onDown(MotionEvent event) {
                    closeQuickMenu();
                }

                @Override
                public void onUp(MotionEvent event) {
                    QuickMenu quickMenu = createImageCropperMenu();
                    showMenu(getAnnotRect(), quickMenu);
                }
            });
        }
    }

    protected void cropImageAnnot() {
        if (!mAnnotIsImageStamp || mAnnot == null || mAnnotView == null) {
            return;
        }

        // first check if cropping is actually needed
        RectF percentageRect = mAnnotView.getCropImageView().getCropRectPercentageMargins();
        boolean canCrop = false;
        if (Math.abs(percentageRect.width()) > 0 || Math.abs(percentageRect.height()) > 0) {
            canCrop = true;
        }
        if (!canCrop) {
            hideImageCropper();
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "cropImageAnnot");
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            // modify appearance bbox
            Obj appearance = mAnnot.getAppearance(Annot.e_normal, null);
            if (appearance != null && appearance.getType() == Obj.e_stream) {
                raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);

                // appearance bbox
                Obj bboxObj = appearance.findObj("BBox");
                Rect appBBox = new Rect(bboxObj);
                appBBox.normalize();

                Rect newAppBBox = new Rect();
                newAppBBox.setX1(appBBox.getX1() + (percentageRect.left * appBBox.getWidth()));
                newAppBBox.setX2(appBBox.getX2() - (percentageRect.right * appBBox.getWidth()));
                newAppBBox.setY1(appBBox.getY1() + (percentageRect.bottom * appBBox.getHeight()));
                newAppBBox.setY2(appBBox.getY2() - (percentageRect.top * appBBox.getHeight()));
                newAppBBox.normalize();

                appearance.putRect("BBox", newAppBBox.getX1(), newAppBBox.getY1(), newAppBBox.getX2(), newAppBBox.getY2());

                // annot bbox
                Rect bbox = mAnnot.getRect();
                bbox.normalize();

                Rect newBBox = new Rect();
                newBBox.setX1(bbox.getX1() + (percentageRect.left * bbox.getWidth()));
                newBBox.setX2(bbox.getX2() - (percentageRect.right * bbox.getWidth()));
                newBBox.setY1(bbox.getY1() + (percentageRect.bottom * bbox.getHeight()));
                newBBox.setY2(bbox.getY2() - (percentageRect.top * bbox.getHeight()));
                newBBox.normalize();

                mAnnot.setRect(newBBox);

                mAnnot.refreshAppearance();
                mPdfViewCtrl.update(mAnnot, mAnnotPageNum);

                raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
        // enable interaction for PDFViewCtrl
        mPdfViewCtrl.setInteractionEnabled(true);

        removeAnnotView(false, false);
        selectAnnot(mAnnot, mAnnotPageNum);
    }

    protected void duplicateAnnot() {
        if (mAnnot == null || mPdfViewCtrl == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "duplicateAnnot");
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }

        // get annot location
        PointF targetPoint = null;
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            Rect pageRect = mAnnot.getRect();
            pageRect.normalize();
            double centerX = (pageRect.getX1() + pageRect.getX2()) / 2f + 20f;
            double centerY = (pageRect.getY1() + pageRect.getY2()) / 2f - 20f;
            double[] pts = mPdfViewCtrl.convPagePtToScreenPt(centerX, centerY, mAnnotPageNum);
            targetPoint = new PointF((float) pts[0], (float) pts[1]);
        } catch (Exception ignored) {
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
        if (targetPoint != null) {
            PointF finalTargetPoint = targetPoint;
            int finalPageNum = mAnnotPageNum;
            AnnotationClipboardHelper.copyAnnot(mPdfViewCtrl.getContext(), mAnnot, mPdfViewCtrl,
                    new AnnotationClipboardHelper.OnClipboardTaskListener() {
                        @Override
                        public void onClipboardTaskDone(String error, ArrayList<Annot> pastedAnnotList) {
                            if (error == null) {
                                AnnotationClipboardHelper.pasteAnnot(mPdfViewCtrl.getContext(),
                                        mPdfViewCtrl, mAnnotPageNum, finalTargetPoint,
                                        new AnnotationClipboardHelper.OnClipboardTaskListener() {
                                            @Override
                                            public void onClipboardTaskDone(String error, ArrayList<Annot> pastedAnnotList) {
                                                if (pastedAnnotList != null && !pastedAnnotList.isEmpty()) {
                                                    ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                                                    toolManager.selectAnnot(pastedAnnotList.get(0), finalPageNum);
                                                }
                                            }
                                        });
                            }
                            removeAnnotView(false);
                            unsetAnnot();
                        }
                    });
        }
    }

    @Nullable
    protected Intent getFolderPickerIntent(FileAttachment fileAttachment) {
        if (fileAttachment == null) {
            return null;
        }
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            String filename = fileAttachment.getFileSpec().getFilePath();
            String extension = Utils.getExtension(filename);
            filename = FilenameUtils.getName(filename);
            if (Utils.isNullOrEmpty(extension)) {
                // no extension, let's try to open it as PDF
                filename = filename + ".pdf";
                extension = "pdf";
            }
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            return ViewerUtils.getFileIntent(filename, mimeType);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
        return null;
    }

    /**
     * Draws the annotation selection box. Color and style of the box depends on whether
     * selection permission is granted. If permission granted, draws a blue rectangle with no padding,
     * otherwise draw a red dashed rectangle with padding.
     *
     * @param canvas to draw the box
     * @param left   The left side of the rectangle to be drawn
     * @param top    The top side of the rectangle to be drawn
     * @param right  The right side of the rectangle to be drawn
     * @param bottom The bottom side of the rectangle to be drawn
     */
    protected void drawSelectionBox(@NonNull Canvas canvas, float left, float top, float right, float bottom) {
        DrawingUtils.drawSelectionBox(mPaint, mPdfViewCtrl.getContext(),
                canvas, left, top, right, bottom, mHasSelectionPermission);
    }

    /**
     * Sets whether disables handling original 8 control points.
     * This should be called if an inherited class
     * has different number of control points and needs special handling.
     * See {@link AnnotEditAdvancedShape}
     */
    protected void setOriginalCtrlPtsDisabled(boolean disabled) {
        mHandleEffCtrlPtsDisabled = disabled;
    }

    protected boolean isSupportMove() {
        return !mAnnotIsTextMarkup;
    }

    protected void raiseAnnotStyleChange() {
        if (mAnnot != null && mAnnotStyle != null) {
            int annotType = mAnnotStyle.getAnnotType();
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            Tool fakeTool = toolManager.safeCreateTool(getCurrentDefaultToolMode());
            if (fakeTool.getCreateAnnotType() == annotType && mAnnotStyleDialog != null) {
                toolManager.onAnnotStyleColorChange(mAnnotStyleDialog.getAnnotStyles());
            }
        }
    }

    public static void setDebug(boolean debug) {
        sDebug = debug;
    }

    /**
     * Returns the edit text used for editing Free Text annotations, and returns null when not editing a
     * Free Text annotation.
     *
     * @return the edit text used for editing Free Text annotation
     */
    @Nullable
    public EditText getFreeTextEditText() {
        return mInlineEditText != null ? mInlineEditText.getEditText() : null;
    }

    private ToolManager getToolManager() {
        return (ToolManager) mPdfViewCtrl.getToolManager();
    }
}
