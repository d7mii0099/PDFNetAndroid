package com.pdftron.pdf.tools;

import android.content.SharedPreferences;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import com.pdftron.common.Matrix2D;
import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.DigitalSignatureField;
import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementReader;
import com.pdftron.pdf.Field;
import com.pdftron.pdf.Image;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFDraw;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageSet;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.Stamper;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.annots.SignatureWidget;
import com.pdftron.pdf.annots.Widget;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.controls.AnnotStyleDialogFragment;
import com.pdftron.pdf.dialog.signature.SignatureDialogFragment;
import com.pdftron.pdf.dialog.signature.SignatureDialogFragmentBuilder;
import com.pdftron.pdf.interfaces.OnCreateSignatureListener;
import com.pdftron.pdf.interfaces.OnDialogDismissListener;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.StampManager;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.sdf.Obj;

import java.io.File;

/**
 * This class is for creating signature annotation.
 */
@Keep
public class Signature extends Tool {

    /**
     * Custom identifier added to the signature stamp added with this tool.
     */
    public static String SIGNATURE_ANNOTATION_ID = "pdftronSignatureStamp";
    public static String SIGNATURE_FIELD_ID = "pdftronSignatureFieldName";

    protected static String SIGNATURE_TEMP_FILE = "SignatureTempFile.jpg";

    protected PointF mTargetPoint = null;
    protected int mTargetPageNum;

    protected Widget mWidget;
    protected Annot mAssociatedAnnot;

    protected boolean mMenuBeingShown;

    protected int mColor;
    protected float mStrokeThickness;

    @StringRes
    protected int mConfirmBtnStrRes;

    protected int mQuickMenuAnalyticType = AnalyticsHandlerAdapter.QUICK_MENU_TYPE_TOOL_SELECT;

    @Nullable
    protected String mSignatureFilePath;

    protected boolean mHasFillAndSignPermission = true;

    protected boolean mFromLongPress = false; // flag used to determine if long press was used. Set to false at the end of onUp.
    private SignatureDialogFragment mSignatureDialogFragment; // store reference to prevent multiple dialogs from showing

    /**
     * Class constructor
     */
    public Signature(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        mNextToolMode = getToolMode();

        // Set signature color
        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        mColor = settings.getInt(getColorKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultColor(mPdfViewCtrl.getContext(), getCreateAnnotType()));
        mStrokeThickness = settings.getFloat(getThicknessKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultThickness(mPdfViewCtrl.getContext(), getCreateAnnotType()));

        mConfirmBtnStrRes = R.string.done;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (mAnnot != null) {
            mHasFillAndSignPermission = hasPermission(mAnnot, ANNOT_PERMISSION_FILL_AND_SIGN);
        }
    }

    @Override
    public void onClose() {
        super.onClose();

        handleDelayRemoveSignature();
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.SIGNATURE;
    }

    @Override
    public int getCreateAnnotType() {
        return AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE;
    }

    @Override
    public void setupAnnotProperty(AnnotStyle annotStyle) {
        super.setupAnnotProperty(annotStyle);

        mSignatureFilePath = annotStyle.getStampId();
    }

    /**
     * Sets the exact path to signature file, if this is set, when clicking on a target, the specified signature will be created.
     *
     * @param signatureFilePath the path to the signature file
     */
    public void setSignatureFilePath(@Nullable String signatureFilePath) {
        mSignatureFilePath = signatureFilePath;
    }

    /**
     * The overload implementation of {@link Tool#onQuickMenuClicked(QuickMenuItem)}.
     */
    @Override
    public boolean onQuickMenuClicked(QuickMenuItem menuItem) {
        if (super.onQuickMenuClicked(menuItem)) {
            return true;
        }

        mMenuBeingShown = false;
        safeSetNextToolMode();

        if (menuItem.getItemId() == R.id.qm_use_saved_sig) {
            Page page = StampManager.getInstance().getDefaultSignature(mPdfViewCtrl.getContext());
            if (page != null) {
                ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                if (mWidget != null && !toolManager.isSignSignatureFieldsWithStamps()) {
                    addSignatureStampToWidget(page);
                    unsetAnnot();
                } else {
                    if (isAddStampToWidget(mWidget)) {
                        mTargetPageNum = mAnnotPageNum;
                    }
                    addSignatureStamp(page);
                }
            }
            mTargetPoint = null;
        } else if (menuItem.getItemId() == R.id.qm_new_signature ||
                menuItem.getItemId() == R.id.qm_edit) {
            showSignaturePickerDialog();
        } else if (menuItem.getItemId() == R.id.qm_delete) {
            boolean shouldUnlock = false;
            try {
                mPdfViewCtrl.docLock(true);
                shouldUnlock = true;
                raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);
                mWidget.getSDFObj().erase("AP");
                mWidget.refreshAppearance();

                mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
                raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum);

                deleteAssociatedSignature();
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    mPdfViewCtrl.docUnlock();
                }
            }
            unsetAnnot();
        }
        return true;
    }

    /**
     * The overload implementation of {@link Tool#onSingleTapConfirmed(MotionEvent)}.
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return handleWidgetMotionEvent(e);
    }

    @Override
    public boolean onLongPress(MotionEvent e) {
        boolean result = handleWidgetMotionEvent(e);
        // If the event is handled here, set the flag so we don't handle it again in onUp
        if (result) {
            mFromLongPress = true;
        }
        return result;
    }

    /**
     * Method to call to handle motion events with the widget (i.e. on tap and long press)
     *
     * @return true if the event was handled here, false otherwise
     */
    protected boolean handleWidgetMotionEvent(@NonNull MotionEvent e) {
        if (mAnnot != null) { // when tap on signature form field
            if (mJustSwitchedFromAnotherTool) {
                mJustSwitchedFromAnotherTool = false;

                // At this point we know we are looking for the signature field.
                mWidget = null;
                setWidget(mAnnot);

                if (mWidget != null) {
                    // Does the widget already have an appearance?
                    try {
                        SignatureWidget signatureWidget = new SignatureWidget(mAnnot);
                        boolean hasExistingSignature = hasVisibleAppearance(signatureWidget);
                        if (!hasExistingSignature) {
                            // even if there is no existing signature, it may be possible there is a floating signature on top of it
                            mAssociatedAnnot = AnnotUtils.getAssociatedAnnotation(mPdfViewCtrl, signatureWidget, mAnnotPageNum);
                        }
                        if (hasExistingSignature || mAssociatedAnnot != null) {
                            int x = (int) (e.getX() + 0.5);
                            int y = (int) (e.getY() + 0.5);
                            handleExistingSignatureWidget(x, y);
                            return true;
                        }
                        showSignaturePickerDialog();
                        return true;
                    } catch (PDFNetException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
        return false;
    }

    protected void handleExistingSignatureWidget(int x, int y) {
        if (mHasFillAndSignPermission) {
            QuickMenu quickMenu = new QuickMenu(mPdfViewCtrl);
            quickMenu.initMenuEntries(R.menu.annot_widget_signature);
            mQuickMenuAnalyticType = AnalyticsHandlerAdapter.QUICK_MENU_TYPE_ANNOTATION_SELECT;

            RectF anchor = new RectF(x - 5, y, x + 5, y + 1);
            showMenu(anchor, quickMenu);
            mMenuBeingShown = true;
        }
    }

    private void setWidget(Annot annot) {
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            if (annot.getType() == Annot.e_Widget) {
                mWidget = new Widget(annot);
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
     * The overload implementation of {@link Tool#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {

        // If from long press, don't handle onUp code
        if (mFromLongPress) {
            // Reset flag here as long press is done
            mFromLongPress = false;
            return false;
        }

        // Deal with touches outside the quick menu when it is being shown.
        if (mMenuBeingShown) {
            safeSetNextToolMode();
            mTargetPoint = null;
            mMenuBeingShown = false;
            return true;
        }

        // If onUp() was fired due to a fling motion, return.
        if (mTargetPoint != null) {
            return false;
        }

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();

        // consume quick menu
        if (toolManager.isQuickMenuJustClosed()) {
            return true;
        }

        if (priorEventMode == PDFViewCtrl.PriorEventMode.PINCH ||
                priorEventMode == PDFViewCtrl.PriorEventMode.SCROLLING ||
                priorEventMode == PDFViewCtrl.PriorEventMode.FLING) {
            return false;
        }

        // if tap on the same kind, select the annotation instead of create a new one
        boolean shouldCreate = true;
        int x = (int) e.getX();
        int y = (int) e.getY();

        setCurrentDefaultToolModeHelper(getToolMode());

        Annot tappedAnnot = didTapOnSameTypeAnnot(e);
        int page = mPdfViewCtrl.getPageNumberFromScreenPt(x, y);
        if (tappedAnnot != null) {
            shouldCreate = false;
            // force ToolManager to select the annotation
            toolManager.selectAnnot(tappedAnnot, page);
        }

        if (shouldCreate && page > 0) {
            createSignature(e.getX(), e.getY());
            return true;
        } else {
            return false;
        }
    }

    /**
     * Adds signature stamp to widget.
     *
     * @param page The page
     * @return true if successful, false otherwise
     */
    protected boolean addSignatureStampToWidget(Page page) {
        if (mAnnot == null) {
            return false;
        }
        boolean shouldUnlock = false;
        PDFDraw pdfDraw = null;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);

            // todo bfung can probably improve this by just passing the original image path to Image
            String sigTempFilePath = mPdfViewCtrl.getContext().getFilesDir().getAbsolutePath() + "/" + SIGNATURE_TEMP_FILE;

            Rect cropBox = page.getCropBox();
            int width = (int) cropBox.getWidth();
            int height = (int) cropBox.getHeight();

            pdfDraw = new PDFDraw();
            pdfDraw.setPageTransparent(true);
            pdfDraw.setImageSize(width, height, true);
            pdfDraw.export(page, sigTempFilePath, "jpeg");

            // Set the appearance of the signature
            SignatureWidget signatureWidget = new SignatureWidget(mAnnot);
            Image img = Image.create(mPdfViewCtrl.getDoc(), sigTempFilePath);
            signatureWidget.createSignatureAppearance(img);

            File sigTempFile = new File(sigTempFilePath);
            if (sigTempFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                sigTempFile.delete();
            }

            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
            raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum);

            deleteAssociatedSignature();
            return true;
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            return false;
        } finally {
            if (pdfDraw != null) {
                try {
                    pdfDraw.destroy();
                } catch (PDFNetException ignored) {
                }
            }
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    private void deleteAssociatedSignature() throws PDFNetException {
        if (mAssociatedAnnot != null) {
            raiseAnnotationPreRemoveEvent(mAssociatedAnnot, mAnnotPageNum);
            Page page = mPdfViewCtrl.getDoc().getPage(mAnnotPageNum);
            mAssociatedAnnot = AnnotUtils.safeDeleteAnnotAndUpdate(mPdfViewCtrl, page, mAssociatedAnnot, mAnnotPageNum);
            raiseAnnotationRemovedEvent(mAssociatedAnnot, mAnnotPageNum);
            mAssociatedAnnot = null;
        }
    }

    private void showSignaturePickerDialog() {
        showSignaturePickerDialog(new OnCreateSignatureListener() {
            @Override
            public void onSignatureCreated(@Nullable String filepath, boolean saveSignature) {
                create(filepath, mWidget);
                ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                if (toolManager.getStampDialogListener() != null && !Utils.isNullOrEmpty(filepath)) {
                    toolManager.getStampDialogListener().onSaveStampPreset(getCreateAnnotType(), filepath);
                }
                if (filepath != null && !saveSignature) {
                    // when not store signature is selected
                    // delete the signature when we done with the file
                    StampManager.getInstance().deleteSignature(mPdfViewCtrl.getContext(), filepath);
                }
            }

            @Override
            public void onSignatureFromImage(@Nullable PointF targetPoint, int targetPage, @Nullable Long widget) {
                if (widget == null && targetPoint == null) {
                    AnalyticsHandlerAdapter.getInstance().sendException(
                            new Exception("both target point and widget are not specified for signature."));
                    return;
                }

                ((ToolManager) (mPdfViewCtrl.getToolManager())).onImageSignatureSelected(
                        targetPoint,
                        targetPage,
                        widget);
            }

            @Override
            public void onAnnotStyleDialogFragmentDismissed(AnnotStyleDialogFragment styleDialog) {
                handleAnnotStyleDialogFragmentDismissed(styleDialog);
            }
        }, null);
    }

    public void showSignaturePickerDialog(final OnCreateSignatureListener createListener, final OnDialogDismissListener dismissListener) {
        showSignaturePickerDialog(createListener, dismissListener, null);
    }

    public void showSignaturePickerDialog(final OnCreateSignatureListener createListener, final OnDialogDismissListener dismissListener, SignatureDialogFragment.DialogMode dialogMode) {
        setCurrentDefaultToolModeHelper(getToolMode());
        mNextToolMode = getToolMode();

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        FragmentActivity activity = toolManager.getCurrentActivity();
        if (activity == null) {
            Log.e(Signature.class.getName(), "ToolManager is not attached to with an Activity");
            return;
        }

        Long targetWidget = mWidget != null ? mWidget.__GetHandle() : null;

        if (!mHasFillAndSignPermission) {
            mNextToolMode = ToolMode.PAN;
            return;
        }
        if (mSignatureDialogFragment == null) {
            mSignatureDialogFragment = createSignatureDialogFragment(targetWidget, toolManager, dialogMode);
            mSignatureDialogFragment.setStyle(DialogFragment.STYLE_NORMAL, toolManager.getTheme());
            mSignatureDialogFragment.addOnCreateSignatureListener(createListener);

            mSignatureDialogFragment.setOnDialogDismissListener(new OnDialogDismissListener() {
                @Override
                public void onDialogDismiss() {
                    mTargetPoint = null;
                    safeSetNextToolMode();

                    if (dismissListener != null) {
                        dismissListener.onDialogDismiss();
                    }
                    mSignatureDialogFragment = null;
                }
            });
            if (!onInterceptDialogFragmentEvent(mSignatureDialogFragment)) { // if not handled, then handle internally
                mSignatureDialogFragment.show(activity.getSupportFragmentManager(), SignatureDialogFragment.TAG);
            } else {
                mSignatureDialogFragment = null;
                mTargetPoint = null;
                safeSetNextToolMode();
            }
        }
    }

    public void handleAnnotStyleDialogFragmentDismissed(AnnotStyleDialogFragment styleDialog) {
        ToolStyleConfig.getInstance().saveAnnotStyle(mPdfViewCtrl.getContext(), styleDialog.getAnnotStyle(), "");
        int color = styleDialog.getAnnotStyle().getColor();
        float thickness = styleDialog.getAnnotStyle().getThickness();
        editColor(color);
        editThickness(thickness);
    }

    private void editColor(int color) {
        mColor = color;

        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(getColorKey(getCreateAnnotType()), color);
        editor.apply();
    }

    private void editThickness(float thickness) {
        mStrokeThickness = thickness;

        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat(getThicknessKey(getCreateAnnotType()), thickness);
        editor.apply();
    }

    protected SignatureDialogFragment createSignatureDialogFragment(Long targetWidget, ToolManager toolManager) {
        return createSignatureDialogFragment(targetWidget, toolManager, null);
    }

    protected SignatureDialogFragment createSignatureDialogFragment(Long targetWidget, ToolManager toolManager, @Nullable SignatureDialogFragment.DialogMode dialogMode) {
        return new SignatureDialogFragmentBuilder()
                .usingTargetPoint(mTargetPoint)
                .usingTargetPage(mTargetPageNum)
                .usingTargetWidget(targetWidget)
                .usingColor(mColor)
                .usingSignatureColors(toolManager.getSignatureColors())
                .usingStrokeWidth(mStrokeThickness)
                .usingShowSavedSignatures(toolManager.isShowSavedSignature())
                .usingShowSignaturePresets(toolManager.isShowSignaturePresets())
                .usingShowSignatureFromImage(toolManager.isShowSignatureFromImage())
                .usingShowTypedSignature(toolManager.isShowTypedSignature())
                .usingAnnotStyleProperties(toolManager.getAnnotStyleProperties())
                .usingConfirmBtnStrRes(mConfirmBtnStrRes)
                .usingPressureSensitive(toolManager.isUsingPressureSensitiveSignatures())
                .usingDefaultStoreNewSignature(toolManager.getDefaultStoreNewSignature())
                .usingPersistStoreSignatureSetting(toolManager.isPersistStoreSignatureSetting())
                .usingDialogMode(dialogMode)
                .build(mPdfViewCtrl.getContext());
    }

    protected void addSignatureStamp(Page stampPage) {
        // there are 2 cases
        // 1. sign at target location
        // 2. sign on top of signature widget for XFDF cannot merge Signature appearance bug in core

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            String fieldId = null;

            // Special case for signatures stamps with widgets, where the
            // widget will be read-only when signature stamp is created
            if (isAddStampToWidget(mWidget)) {
                Field field = mWidget.getField();
                fieldId = field.getName();
                raiseAnnotationPreModifyEvent(mWidget, mTargetPageNum);
                mWidget.setFlag(Annot.e_hidden, true);
                mPdfViewCtrl.update(mWidget, mTargetPageNum);
                raiseAnnotationModifiedEvent(mWidget, mTargetPageNum);
            }

            PDFDoc doc = mPdfViewCtrl.getDoc();
            Rect stampRect = stampPage.getCropBox();
            Page page = doc.getPage(mTargetPageNum);
            Rect pageCropBox = page.getCropBox();
            Rect pageViewBox = page.getBox(mPdfViewCtrl.getPageBox());

            int viewRotation = mPdfViewCtrl.getPageRotation();
            int pageRotation = page.getRotation();

            // If the page itself is rotated, we want to "rotate" width and height as well
            double pageWidth = pageViewBox.getWidth();
            if (pageRotation == Page.e_90 || pageRotation == Page.e_270) {
                pageWidth = pageViewBox.getHeight();
            }
            double pageHeight = pageViewBox.getHeight();
            if (pageRotation == Page.e_90 || pageRotation == Page.e_270) {
                pageHeight = pageViewBox.getWidth();
            }

            double maxWidth = 200;
            double maxHeight = 200;

            if (pageWidth < maxWidth) {
                maxWidth = pageWidth;
            }
            if (pageHeight < maxHeight) {
                maxHeight = pageHeight;
            }
            double stampWidth = stampRect.getWidth();
            double stampHeight = stampRect.getHeight();

            // if the viewer rotates pages, we want to treat it as if it's the stamp that's rotated
            if (viewRotation == Page.e_90 || viewRotation == Page.e_270) {
                double temp = stampWidth;
                //noinspection SuspiciousNameCombination
                stampWidth = stampHeight;
                stampHeight = temp;
            }

            double scaleFactor = Math.min(maxWidth / stampWidth, maxHeight / stampHeight);
            stampWidth *= scaleFactor;
            stampHeight *= scaleFactor;

            com.pdftron.pdf.Rect widgetRect = null;
            if (isAddStampToWidget(mWidget)) {
                SignatureWidget signatureWidget = new SignatureWidget(mAnnot);
                try {
                    widgetRect = signatureWidget.getVisibleContentBox();
                } catch (PDFNetException ex) {
                    widgetRect = signatureWidget.getRect();
                }
                widgetRect.normalize();
                stampWidth = widgetRect.getWidth();
                stampHeight = widgetRect.getHeight();
            }

            Stamper stamper = new Stamper(Stamper.e_absolute_size, stampWidth, stampHeight);
            stamper.setAlignment(Stamper.e_horizontal_left, Stamper.e_vertical_bottom);
            stamper.setAsAnnotation(true);

            Matrix2D mtx = page.getDefaultMatrix();// This matrix takes into account page rotation and crop box

            double targetPointX = 0;
            double targetPointY = 0;
            if (widgetRect != null) {
                // center of the widget
                targetPointX = (widgetRect.getX2() - widgetRect.getX1()) / 2.0 + widgetRect.getX1();
                targetPointY = (widgetRect.getY2() - widgetRect.getY1()) / 2.0 + widgetRect.getY1();
            } else {
                targetPointX = mTargetPoint.x;
                targetPointY = mTargetPoint.y;
            }
            Point pt = mtx.multPoint(targetPointX, targetPointY);

            double xPos = pt.x - (stampWidth / 2);
            double yPos = pt.y - (stampHeight / 2);

            // Note, stamper stamps relative to the CropBox,
            // i.e. (0, 0) is the bottom right corner of crop box.
            double leftEdge = pageViewBox.getX1() - pageCropBox.getX1();
            double bottomEdge = pageViewBox.getY1() - pageCropBox.getY1();

            if (xPos > leftEdge + pageWidth - stampWidth) {
                xPos = leftEdge + pageWidth - stampWidth;
            }
            if (xPos < leftEdge) {
                xPos = leftEdge;
            }

            if (yPos > bottomEdge + pageHeight - stampHeight) {
                yPos = bottomEdge + pageHeight - stampHeight;
            }
            if (yPos < bottomEdge) {
                yPos = bottomEdge;
            }

            stamper.setPosition(xPos, yPos);

            int stampRotation = (4 - viewRotation) % 4; // 0 = 0, 90 = 1; 180 = 2, and 270 = 3
            stamper.setRotation(stampRotation * 90.0);
            stamper.stampPage(doc, stampPage, new PageSet(mTargetPageNum));

            int numAnnots = page.getNumAnnots();
            Annot annot = page.getAnnot(numAnnots - 1);
            Obj obj = annot.getSDFObj();
            obj.putString(SIGNATURE_ANNOTATION_ID, "");
            if (!Utils.isNullOrEmpty(fieldId)) {
                annot.setCustomData(SIGNATURE_FIELD_ID, fieldId);
            }

            if (annot.isMarkup()) {
                Markup markup = new Markup(annot);
                setAuthor(markup);
            }

            setAnnot(annot, mTargetPageNum);
            buildAnnotBBox();

            mPdfViewCtrl.update(annot, mTargetPageNum);
            raiseAnnotationAddedEvent(annot, mTargetPageNum);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    protected Element getFirstElementUsingReader(ElementReader reader, Obj obj, int type) {
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            if (obj != null) {
                reader.begin(obj);
                try {
                    Element element;
                    while ((element = reader.next()) != null) {
                        if (element.getType() == type) {
                            return element;
                        }
                    }
                } finally {
                    reader.end();
                }
            }
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
     * Sets the target point.
     *
     * @param point The target point
     */
    public void setTargetPoint(PointF point) {
        createSignature(point.x, point.y);

        safeSetNextToolMode();
    }

    public void setTargetPoint(PointF pagePoint, int page) {
        mTargetPoint = pagePoint;
        mTargetPageNum = page;

        safeSetNextToolMode();
    }

    public void create(String filepath, Annot widget) {
        if (filepath != null) {
            Page page = StampManager.getInstance().getSignature(filepath);
            if (page != null) {
                ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                if (widget != null) {
                    mAnnot = widget;
                    setWidget(mAnnot);
                }
                if (widget != null && !toolManager.isSignSignatureFieldsWithStamps()) {
                    addSignatureStampToWidget(page);
                    unsetAnnot();
                } else {
                    if (isAddStampToWidget(widget)) {
                        mTargetPageNum = mAnnotPageNum;
                    }
                    addSignatureStamp(page);
                }
            }
            handleDelayRemoveSignature();
        }
    }

    private boolean hasVisibleAppearance(SignatureWidget signatureWidget) {
        final int SIG_APPEARANCE_EMPTY = 0; // The signature field has no appearance set.
        final int SIG_APPEARANCE_PATHS = 1; // The signature field has paths as its appearance.
        final int SIG_APPEARANCE_IMAGE = 2; // The signature field has an image as its appearance.

        boolean shouldUnlockRead = false;
        try {
            // Locks the document first as accessing annotation/doc information isn't thread safe.
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;

            // first check with core's built-in method
            DigitalSignatureField digitalSignatureField = signatureWidget.getDigitalSignatureField();
            if (!digitalSignatureField.hasVisibleAppearance()) {
                return false;
            }

            int elementType = SIG_APPEARANCE_EMPTY;
            Obj app = signatureWidget.getAppearance();
            if (app != null) {
                ElementReader reader = new ElementReader();
                Element formElement = getFirstElementUsingReader(reader, app, Element.e_form);
                if (formElement != null) {
                    Obj o = formElement.getXObject();
                    Element element = getFirstElementUsingReader(reader, o, Element.e_path);
                    if (element != null) {
                        elementType = SIG_APPEARANCE_PATHS;
                    } else {
                        element = getFirstElementUsingReader(reader, o, Element.e_image);
                        if (element != null) {
                            elementType = SIG_APPEARANCE_IMAGE;
                        }
                    }
                }
            }
            return (elementType == SIG_APPEARANCE_IMAGE || elementType == SIG_APPEARANCE_PATHS);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
        return false;
    }

    private void createSignature(float ptx, float pty) {
        setCurrentDefaultToolModeHelper(getToolMode());
        // Gets the target point (in page space) and page.
        mTargetPageNum = mPdfViewCtrl.getPageNumberFromScreenPt(ptx, pty);
        double[] pts = mPdfViewCtrl.convScreenPtToPagePt(ptx, pty, mTargetPageNum);
        mTargetPoint = new PointF();
        mTargetPoint.x = (float) pts[0];
        mTargetPoint.y = (float) pts[1];

        if (!Utils.isNullOrEmpty(mSignatureFilePath)) {
            create(mSignatureFilePath, mWidget);
            mTargetPoint = null;
            safeSetNextToolMode();
        } else {
            showSignaturePickerDialog();
        }
    }

    private void safeSetNextToolMode() {
        if (mForceSameNextToolMode) {
            mNextToolMode = mCurrentDefaultToolMode;
        } else {
            mNextToolMode = ToolMode.PAN;
        }
    }

    private void handleDelayRemoveSignature() {
        if (mPdfViewCtrl == null) {
            return;
        }
        StampManager.getInstance().consumeDelayRemoveSignature(mPdfViewCtrl.getContext());
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        if (toolManager.getPresetsListener() != null) {
            toolManager.getPresetsListener().onUpdatePresets(AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE);
        }
    }

    @Override
    protected int getQuickMenuAnalyticType() {
        return mQuickMenuAnalyticType;
    }

    // read lock required around this method
    private boolean isAddStampToWidget(@Nullable Annot widget) {
        try {
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            return widget != null && widget.isValid() && toolManager.isSignSignatureFieldsWithStamps();
        } catch (Exception ignored) {
        }
        return false;
    }
}
