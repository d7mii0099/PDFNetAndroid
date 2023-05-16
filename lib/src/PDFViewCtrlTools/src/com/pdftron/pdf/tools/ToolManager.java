//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Pair;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.StyleRes;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Action;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.CurvePainter;
import com.pdftron.pdf.GeometryCollection;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.annots.FileAttachment;
import com.pdftron.pdf.config.ToolConfig;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.controls.AnnotIndicatorManger;
import com.pdftron.pdf.controls.AnnotStyleDialogFragment;
import com.pdftron.pdf.controls.PageIndicatorLayout;
import com.pdftron.pdf.interfaces.OnAnnotStyleChangedListener;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.AnnotStyleProperty;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.model.UserBookmarkItem;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotSnappingManager;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.DrawingUtils;
import com.pdftron.pdf.utils.Logger;
import com.pdftron.pdf.utils.RedactionManager;
import com.pdftron.pdf.utils.RequestCode;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.SnapUtils;
import com.pdftron.pdf.utils.SoundManager;
import com.pdftron.pdf.utils.ThemeProvider;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.pdf.widget.SelectionLoupe;
import com.pdftron.pdf.widget.toolbar.builder.QuickMenuToolbarItem;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarButtonType;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarItem;
import com.pdftron.pdf.widget.toolbar.component.ToolModeMapper;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static com.pdftron.pdf.widget.toolbar.component.AnnotationToolbarComponent.TOOLBAR_ITEM_BUNDLE;

/**
 * This class implements the {@link com.pdftron.pdf.PDFViewCtrl.ToolManager}
 * interface. The ToolManager interface is basically a listener for the several
 * different events triggered by PDFViewCtrl, including gesture, layout and
 * custom events.
 * <p>
 * The Tool interface defined in this class is used to propagate these events
 * to the different tools, so making it possible to control which actions to
 * execute upon such events. Each concrete Tool implementation decides which
 * is the next tool, and the ToolManager uses the {@link Tool#getNextToolMode()}
 * to check if it must stop the event loop or create a new tool
 * and continue to propagate the event.
 * <p>
 * For example, the code for {@link #onDown(MotionEvent)} is as below:
 * <p>
 * <pre>
 * if (mTool != null) {
 *     ToolMode prev_tm = mTool.getAnnotType(), next_tm;
 *     do {
 *         mTool.onDown(e);
 *         next_tm = mTool.getNextToolMode();
 *         if (prev_tm != next_tm) {
 *             setTool(createTool(next_tm, mTool));
 *             prev_tm = next_tm;
 *         } else {
 *             break;
 *         }
 *     } while (true);
 * }
 * </pre>
 * <p>
 * With this being said, a Tool implementation should prevent forming tools in
 * a cyclic way.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ToolManager implements
        PDFViewCtrl.ToolManager,
        PDFViewCtrl.ActionCompletedListener {

    /**
     * This interface is used to forward events from {@link com.pdftron.pdf.PDFViewCtrl.ToolManager}
     * to the actual implementation of the Tool.
     */
    public interface Tool {
        /**
         * Gets the tool mode.
         *
         * @return the mode/identifier of this tool.
         */
        ToolModeBase getToolMode();

        /**
         * Gets what annotation type this tool can create
         *
         * @return annot type for annotation creation tool, or unknown for non-creation tool.
         */
        int getCreateAnnotType();

        /**
         * Gets the next tool mode.
         *
         * @return the mode of the next tool. Via this method, a tool can
         * indicate the next tool to switch to.
         */
        ToolModeBase getNextToolMode();

        /**
         * Called when night mode has been updated.
         */
        void onNightModeUpdated(boolean isNightMode);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onSetDoc()} to the tools.
         */
        void onSetDoc();

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onKeyUp(int, KeyEvent)} to the tools.
         */
        boolean onKeyUp(int keyCode, KeyEvent event);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onDoubleTap(MotionEvent)} to the tools.
         */
        boolean onDoubleTap(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onDoubleTapEnd(MotionEvent)} to the tools.
         */
        void onDoubleTapEnd(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onDoubleTapEvent(MotionEvent)} to the tools.
         */
        boolean onDoubleTapEvent(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onDown(MotionEvent)} to the tools.
         */
        boolean onDown(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onPointerDown(MotionEvent)} to the tools.
         */
        boolean onPointerDown(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)} to the tools.
         */
        boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onFlingStop()} to the tools.
         */
        boolean onFlingStop();

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onLayout(boolean, int, int, int, int)} to the tools.
         */
        void onLayout(boolean changed, int l, int t, int r, int b);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onLongPress(MotionEvent)} to the tools.
         */
        boolean onLongPress(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onScaleBegin(float, float)} to the tools.
         */
        boolean onScaleBegin(float x, float y);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onScale(float, float)} to the tools.
         */
        boolean onScale(float x, float y);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onScaleEnd(float, float)} to the tools.
         */
        boolean onScaleEnd(float x, float y);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onSingleTapConfirmed(MotionEvent)} to the tools.
         */
        boolean onSingleTapConfirmed(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onMove(MotionEvent, MotionEvent, float, float)} to the tools.
         */
        boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onScrollChanged(int, int, int, int)} to the tools.
         */
        void onScrollChanged(int l, int t, int oldl, int oldt);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onShowPress(MotionEvent)} to the tools.
         */
        boolean onShowPress(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onSingleTapUp(MotionEvent)} to the tools.
         */
        boolean onSingleTapUp(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onClose()} to the tools.
         */
        void onClose();

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onConfigurationChanged(Configuration)} to the tools.
         */
        void onConfigurationChanged(Configuration newConfig);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onPageTurning(int, int)} to the tools.
         */
        void onPageTurning(int old_page, int cur_page);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onPostSingleTapConfirmed()} to the tools.
         */
        void onPostSingleTapConfirmed();

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onCustomEvent(Object)} to the tools.
         */
        void onCustomEvent(Object obj);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onDocumentDownloadEvent(PDFViewCtrl.DownloadState, int, int, int, String)} to the tools.
         */
        void onDocumentDownloadEvent(PDFViewCtrl.DownloadState state, int page_num, int page_downloaded, int page_count, String message);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onDraw(Canvas, Matrix)} to the tools.
         */
        void onDraw(Canvas canvas, Matrix tfm);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onDrawEdgeEffects(Canvas, int, int)} to the tools.
         */
        boolean onDrawEdgeEffects(Canvas canvas, int width, int verticalOffset);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onReleaseEdgeEffects()} to the tools.
         */
        void onReleaseEdgeEffects();

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onPullEdgeEffects(int, float)} to the tools.
         */
        void onPullEdgeEffects(int which_edge, float delta_distance);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onDoubleTapZoomAnimationBegin()} to the tools.
         */
        void onDoubleTapZoomAnimationBegin();

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onDoubleTapZoomAnimationEnd()} to the tools.
         */
        void onDoubleTapZoomAnimationEnd();

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onRenderingFinished()} to the tools.
         */
        void onRenderingFinished();

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#isCreatingAnnotation()} to the tools.
         */
        boolean isCreatingAnnotation();

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onAnnotPainterUpdated(int, long, com.pdftron.pdf.CurvePainter)} to the tools.
         */
        void onAnnotPainterUpdated(int page, long which, CurvePainter painter);
    }

    /**
     * This interface can be used to listen for when a new tool is set as current tool of ToolManager.
     */
    public interface ToolSetListener {

        /**
         * Event called when a new tool is set as current tool of ToolManager.
         *
         * @param newTool the new tool
         */
        void onToolSet(Tool newTool);
    }

    /**
     * This interface can be used to listen for when the current tool changes.
     */
    public interface ToolChangedListener {

        /**
         * Event called when the tool changes.
         *
         * @param newTool the new tool
         * @param oldTool the old tool
         */
        void toolChanged(Tool newTool, @Nullable Tool oldTool);
    }

    /**
     * This interface can be used to listen for when the PDFViewCtrl's onLayout() is triggered.
     */
    public interface OnLayoutListener {

        /**
         * Event called when the PDFViewCtrl's onLayout() is triggered.
         *
         * @param changed This is a new size or position for this view.
         * @param l       Left position, relative to parent.
         * @param t       Top position, relative to parent.
         * @param r       Right position, relative to parent.
         * @param b       Bottom position, relative to parent.
         */
        void onLayout(boolean changed, int l, int t, int r, int b);
    }

    public interface PreFlingListener {
        /**
         * Propagates {@link PDFViewCtrl} fling event.
         */
        boolean onFling(MotionEvent motionEvent);

        /**
         * Propagates {@link PDFViewCtrl} fling stopped event.
         */
        boolean onFlingStop();
    }

    /**
     * This interface can be used to avoid executing Tool's code in the {@link com.pdftron.pdf.tools.ToolManager}
     * implementation (the events will be called before Tool's ones).
     */
    public interface PreToolManagerListener {

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onSingleTapConfirmed(MotionEvent)} to the tools.
         */
        boolean onSingleTapConfirmed(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onMove(MotionEvent, MotionEvent, float, float)} to the tools.
         */
        boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onDown(MotionEvent)}
         */
        boolean onDown(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)} to the tools.
         */
        boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onScaleBegin(float, float)} to the tools.
         */
        boolean onScaleBegin(float x, float y);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onScale(float, float)} to the tools.
         */
        boolean onScale(float x, float y);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onScaleEnd(float, float)} to the tools.
         */
        boolean onScaleEnd(float x, float y);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onLongPress(MotionEvent)} to the tools.
         */
        boolean onLongPress(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onScrollChanged(int, int, int, int)} to the tools.
         */
        void onScrollChanged(int l, int t, int oldl, int oldt);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onDoubleTap(MotionEvent)} to the tools.
         */
        boolean onDoubleTap(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onKeyUp(int, KeyEvent)} to the tools.
         */
        boolean onKeyUp(int keyCode, KeyEvent event);
    }

    /**
     * This interface can be used to detect if the built-in Tools quick menu item has been clicked.
     */
    public interface QuickMenuListener {

        /**
         * Called when a menu in quick menu has been clicked.
         *
         * @param menuItem The quick menu item
         * @return True if handled
         */
        boolean onQuickMenuClicked(QuickMenuItem menuItem);

        /**
         * Called when quick menu is about to show.
         *
         * @param quickmenu the quick menu that is about to show
         * @param annot     the selected annot, null if no annot is selected
         * @return true if quick menu should not show
         */
        boolean onShowQuickMenu(QuickMenu quickmenu, @Nullable Annot annot);

        /**
         * Called when quick menu has been shown.
         */
        void onQuickMenuShown();

        /**
         * Called when quick menu has been dismissed.
         */
        void onQuickMenuDismissed();
    }

    /**
     * This interface can be used to monitor any modification events that change the structure of
     * the PDF document such as page manipulation, bookmark modification, etc.
     * <p>
     * For listening to annotation modification events see {@link AnnotationModificationListener}.
     */
    public interface PdfDocModificationListener {

        /**
         * Called when PDF bookmarks have been modified.
         *
         * @param bookmarkItems the new bookmarks after modification
         */
        void onBookmarkModified(@NonNull List<UserBookmarkItem> bookmarkItems);

        /**
         * Called when document pages haven cropped.
         */
        void onPagesCropped();

        /**
         * Called when pages have been added to the document.
         *
         * @param pageList The list of pages added to the document
         */
        void onPagesAdded(final List<Integer> pageList);

        /**
         * Called when pages have been deleted from the document.
         *
         * @param pageList The list of pages deleted from the document
         */
        void onPagesDeleted(final List<Integer> pageList);

        /**
         * Called when pages have been rotated.
         *
         * @param pageList The list of rotated pages
         */
        void onPagesRotated(final List<Integer> pageList);

        /**
         * Called when a page has been moved.
         *
         * @param from The page number on which the page moved from
         * @param to   The page number on which the page moved to
         */
        void onPageMoved(int from, int to);

        /**
         * Called when page labels have been changed
         */
        void onPageLabelsChanged();

        /**
         * Called when all annotations in the document have been removed.
         */
        void onAllAnnotationsRemoved();

        /**
         * Called when an annotations action has been taken place.
         */
        void onAnnotationAction();
    }

    public interface PdfOutlineModificationListener {
        void onOutlineChanged();
    }

    /**
     * This interface can be used to monitor PDF text modification events
     */
    public interface PdfTextModificationListener {
        void onPdfTextChanged();
    }

    /**
     * This interface can be used to monitor annotation modification events such as added/edited/removed.
     * <p>
     * For listening to events that change the structure of PDF document such as page manipulation see
     * {@link PdfDocModificationListener}.
     */
    public interface AnnotationModificationListener {

        /**
         * Called when annotations have been added to the document.
         *
         * @param annots The list of annotations (a pair of annotation and the page number
         *               where the annotation is on)
         */
        void onAnnotationsAdded(Map<Annot, Integer> annots);

        /**
         * Called right before annotations have been modified.
         *
         * @param annots The list of annotations (a pair of annotation and the page number
         *               where the annotation is on)
         */
        void onAnnotationsPreModify(Map<Annot, Integer> annots);

        /**
         * Called when annotations have been modified.
         *
         * @param annots The list of annotations (a pair of annotation and the page number
         *               where the annotation is on)
         */
        void onAnnotationsModified(Map<Annot, Integer> annots, Bundle extra);

        /**
         * Called right before annotations have been removed from the document.
         * All actions to be performed on the annots need to be done here
         *
         * @param annots The list of annotations (a pair of annotation and the page number
         *               where the annotation is on)
         */
        void onAnnotationsPreRemove(Map<Annot, Integer> annots);

        /**
         * Called when annotations have been removed from the document.
         * No action should be performed on the annots, use onAnnotationsPreRemove instead.
         *
         * @param annots The list of annotations (a pair of annotation and the page number
         *               where the annotation is on)
         */
        void onAnnotationsRemoved(Map<Annot, Integer> annots);

        /**
         * Called when all annotations in the specified page have been removed.
         *
         * @param pageNum The page number where the annotations are on
         */
        void onAnnotationsRemovedOnPage(int pageNum);

        /**
         * Called when annotations couldn't have been added
         *
         * @param errorMessage The error message
         */
        void annotationsCouldNotBeAdded(String errorMessage);
    }

    /**
     * This interface can be used to monitor annotation selection changed event.
     */
    public interface AnnotationsSelectionListener {
        /**
         * Called when multiple annotations have been selected.
         * The map contains: key is the annotation, value is the page number
         *
         * @param annots the annotations map currently selected
         */
        void onAnnotationsSelectionChanged(HashMap<Annot, Integer> annots);
    }

    /**
     * This interface can be used to monitor basic annotation events such as selected/unselected.
     */
    public interface BasicAnnotationListener {
        /**
         * Called when an annotation has been selected.
         *
         * @param annot   The selected annotation
         * @param pageNum The page number where the annotation is on
         */
        @Deprecated
        void onAnnotationSelected(Annot annot, int pageNum);

        /**
         * Called when an annotation has been unselected.
         */
        @Deprecated
        void onAnnotationUnselected();

        /**
         * Intercept tool's response to user actions (such as clicking on links,
         * clicking on form widget, or about to change annotation properties etc.)
         * If handled, tool will stop default logic.
         *
         * @param annot    annotation
         * @param extra    extra information
         * @param toolMode tool mode that handles annotation
         * @return true then intercept the subclass function, false otherwise
         */
        boolean onInterceptAnnotationHandling(@Nullable Annot annot, Bundle extra, ToolMode toolMode);

        /**
         * Intercept handling of dialog
         *
         * @param dialog the dialog about to show up
         * @return true if intercept the subclass function, false otherwise
         * @deprecated see {@link DialogListener}
         */
        @Deprecated
        boolean onInterceptDialog(AlertDialog dialog);
    }

    /**
     * This interface can be used to intercept dialog events.
     */
    public interface DialogListener {

        /**
         * Intercept handling of dialog fragments
         *
         * @param dialog the dialog about to show up
         * @return true if intercept the subclass function, false otherwise
         */
        boolean onInterceptDialog(DialogFragment dialog);
    }

    /**
     * This interface can be used to monitor advanced annotation events from various tools.
     */
    public interface AdvancedAnnotationListener {
        /**
         * Enum to define the annotation actions for the fileCreated event
         */
        enum AnnotAction {
            SCREENSHOT_CREATE
        }

        /**
         * Called when a file attachment has been selected.
         *
         * @param attachment The file attachment
         */
        void fileAttachmentSelected(FileAttachment attachment);

        /**
         * Called when free hand stylus has been used for the firs time.
         */
        void freehandStylusUsedFirstTime();

        /**
         * Called when a location has been selected for adding the image stamp.
         *
         * @param targetPoint The target location to add the image stamp
         */
        void imageStamperSelected(PointF targetPoint);

        /**
         * Called when a location or widget has been selected for adding the image signature
         * Only one of the param should be valid
         *
         * @param targetPoint the target location in page point
         * @param targetPage  the target page
         * @param widget      the target widget
         */
        void imageSignatureSelected(PointF targetPoint, int targetPage, Long widget);

        /**
         * Called when a location has been selected for adding the file attachment.
         *
         * @param targetPoint The target location to add the file attachment
         */
        void attachFileSelected(PointF targetPoint);

        /**
         * Called when free text inline editing has started.
         */
        void freeTextInlineEditingStarted();

        boolean newFileSelectedFromTool(String filePath, int pageNumber);

        /**
         * Called when an annotation needs to perform an action with a file created
         *
         * @param fileLocation location of the file associated with annotation
         * @param action       the specific action associated with the file
         */
        void fileCreated(String fileLocation, AnnotAction action);
    }

    public interface SpecialAnnotationListener {
        /**
         * @param text     The text
         * @param anchor   The anchor
         * @param isDefine True if it is define, False if it is translate
         * @hide Called when define or translate has been selected.
         */
        void defineTranslateSelected(String text, RectF anchor, Boolean isDefine);
    }

    public interface FileAttachmentAnnotationListener {
        void onSaveFileAttachmentSelected(FileAttachment fileAttachment, Intent intent);
    }

    public interface ActionGoBackListener {
        void onActionGoBack();
    }

    /**
     * This interface can be used to monitor tools interaction with annotation toolbar
     */
    public interface AnnotationToolbarListener {
        /**
         * Called when ink edit has been selected.
         *
         * @param annot The ink annotation
         */
        void inkEditSelected(Annot annot, int pageNum);

        /**
         * The implementation should specify the annotation toolbar height.
         *
         * @return The annotation toolbar height
         */
        int annotationToolbarHeight();

        /**
         * The implementation should specify the toolbar height.
         *
         * @return The annotation toolbar height
         */
        int toolbarHeight();

        /**
         * The implementation should open the edit toolbar for the mode
         *
         * @param mode The tool mode
         */
        void openEditToolbar(ToolMode mode);

        /**
         * The implementation should open the annotation toolbar for the mode
         *
         * @param mode The tool mode
         */
        @Deprecated
        void openAnnotationToolbar(ToolMode mode);
    }

    public interface StampDialogListener {
        void onSaveStampPreset(int annotType, @NonNull String stampId);
    }

    public interface PresetsListener {
        void onUpdatePresets(int annotType);
    }

    public interface SnackbarListener {
        void onShowSnackbar(@NonNull CharSequence text, int duration, @Nullable CharSequence actionText, View.OnClickListener action);
    }

    /**
     * This interface can be used to monitor generic motion event
     */
    public interface OnGenericMotionEventListener {
        /**
         * Called when a generic motion occurred.
         *
         * @param event The motion event
         */
        void onGenericMotionEvent(MotionEvent event);

        /**
         * The implementation should change the pointer icon.
         *
         * @param pointerIcon The pointer icon
         */
        void onChangePointerIcon(PointerIcon pointerIcon);
    }

    /**
     * This interface can be used to provide custom key for annotation creation
     */
    public interface ExternalAnnotationManagerListener {
        /**
         * The implementation should generate a string key.
         *
         * @return The generated string key
         */
        String onGenerateKey();
    }

    /**
     * This interface can be used to listen to viewer edge effect events
     */
    public interface EdgeEffectListener {
        /**
         * Called when edge effect is drawn in onDraw. If this method returns true, the internal
         * logic will not be called.
         *
         * @param canvas         canvas used to draw edge effect
         * @param width          width of the edge effect
         * @param verticalOffset vertical offset to draw edge effect
         * @return True if intercepted and handled in this callback, false otherwise
         */
        boolean onPreDrawEdgeEffects(Canvas canvas, int width, int verticalOffset);

        /**
         * Called when edge effect is released in onUp.
         */
        void onPreReleaseEdgeEffects();

        /**
         * Called when edge effect should occur.
         *
         * @param whichEdge     -1 for left edge, 1 for right edge
         * @param deltaDistance distance of edge effect
         */
        void onPrePullEdgeEffects(int whichEdge, float deltaDistance);
    }

    /**
     * Callback to notify subscribers that a full PDF save is required
     */
    public interface FullSaveListener {
        void onFullSaveRequired();
    }

    /**
     * Callback for annotation permissions
     */
    public interface AnnotationPermissionListener {
        boolean hasPermissions(Annot annot, int kind);
    }

    /**
     * Base tool mode
     */
    public interface ToolModeBase {
        /**
         * Gets value of this tool mode
         *
         * @return value
         */
        int getValue();
    }

    /**
     * Tool modes
     */
    public enum ToolMode implements ToolModeBase {
        /**
         * Identifier of the Pan tool.
         */
        PAN(1),
        /**
         * Identifier of the Annotation Edit tool.
         */
        ANNOT_EDIT(2),
        /**
         * Identifier of the Line tool.
         */
        LINE_CREATE(3),
        /**
         * Identifier of the Arrow tool.
         */
        ARROW_CREATE(4),
        /**
         * Identifier of the Rectangle tool.
         */
        RECT_CREATE(5),
        /**
         * Identifier of the Oval/Ellipse tool
         */
        OVAL_CREATE(6),
        /**
         * Identifier of the Ink tool.
         */
        INK_CREATE(7),
        /**
         * Identifier of the Note tool.
         */
        TEXT_ANNOT_CREATE(8),
        /**
         * Identifier of the Link tool.
         */
        LINK_ACTION(9),
        /**
         * Identifier of the Text Selection tool.
         */
        TEXT_SELECT(10),
        /**
         * Identifier of the Form Filling too.
         */
        FORM_FILL(11),
        /**
         * Identifier of the Text tool.
         */
        TEXT_CREATE(12),
        /**
         * Identifier of the Annotation Edit for Line/Arrow tool.
         */
        ANNOT_EDIT_LINE(13),
        /**
         * Identifier of the Rich Media tool.
         */
        RICH_MEDIA(14),
        /**
         * Identifier of the Digital Signature tool.
         */
        DIGITAL_SIGNATURE(15),
        /**
         * Identifier of the Text Underline tool.
         */
        TEXT_UNDERLINE(16),
        /**
         * Identifier of the Text Highlight tool.
         */
        TEXT_HIGHLIGHT(17),
        /**
         * Identifier of the Text Squiggly tool.
         */
        TEXT_SQUIGGLY(18),
        /**
         * Identifier of the Text Strikeout tool.
         */
        TEXT_STRIKEOUT(19),
        /**
         * Identifier of the Eraser tool.
         */
        INK_ERASER(20),
        /**
         * Identifier of the Annotation Edit for text markup tool.
         */
        ANNOT_EDIT_TEXT_MARKUP(21),
        /**
         * Identifier of the TextHighlighter tool.
         */
        TEXT_HIGHLIGHTER(22),
        /**
         * Identifier of the (floating) Signature tool.
         */
        SIGNATURE(23),
        /**
         * Identifier of the Image Stamper tool.
         */
        STAMPER(24),
        /**
         * Identifier of the Rubber Stamper tool.
         */
        RUBBER_STAMPER(25),
        /**
         * Identifier of the Stamper tool.
         */
        RECT_LINK(26),
        /**
         * Identifier of the Signature form field tool.
         */
        FORM_SIGNATURE_CREATE(27),
        /**
         * Identifier of the form field text tool.
         */
        FORM_TEXT_FIELD_CREATE(28),
        /**
         * Identifier of the Text Link tool.
         */
        TEXT_LINK_CREATE(29),
        /**
         * Identifier of the form checkbox tool.
         */
        FORM_CHECKBOX_CREATE(30),
        /**
         * Identifier of the form radio group tool.
         */
        FORM_RADIO_GROUP_CREATE(31),
        /**
         * Identifier of the text redaction tool.
         */
        TEXT_REDACTION(32),
        /**
         * Identifier of the free highlighter tool (Ink in blend mode).
         */
        FREE_HIGHLIGHTER(33),
        /**
         * Identifier of the polygon tool.
         */
        POLYLINE_CREATE(34),
        /**
         * Identifier of the polygon tool.
         */
        POLYGON_CREATE(35),
        /**
         * Identifier of the polygon cloud tool.
         */
        CLOUD_CREATE(36),
        /**
         * Identifier of the multi select annotation edit tool (Select annots by draw a rectangle).
         */
        ANNOT_EDIT_RECT_GROUP(37),
        /**
         * Identifier of the editing tool for polyline/polygon/cloud tool.
         */
        ANNOT_EDIT_ADVANCED_SHAPE(38),
        /**
         * Identifier of the ruler tool.
         */
        RULER_CREATE(39),
        /**
         * Identifier of the Callout tool
         */
        CALLOUT_CREATE(40),
        /**
         * Identifier of the Sound tool
         */
        SOUND_CREATE(41),
        /**
         * Identifier of the FileAttachment tool
         */
        FILE_ATTACHMENT_CREATE(42),
        /**
         * Identifier of the RectRedactionCreate tool
         */
        RECT_REDACTION(43),
        /**
         * Identifier of the PerimeterMeasureCreate tool
         */
        PERIMETER_MEASURE_CREATE(44),
        /**
         * Identifier of the AreaMeasureCreate tool
         */
        AREA_MEASURE_CREATE(45),
        /**
         * Identifier of the ComboBoxFieldCreate tool
         */
        FORM_COMBO_BOX_CREATE(46),
        /**
         * Identifier of the ListBoxFieldCreate tool
         */
        FORM_LIST_BOX_CREATE(47),
        /**
         * Identifier of the FreeTextSpacingCreate tool
         */
        FREE_TEXT_SPACING_CREATE(48),
        /**
         * Identifier of the FreeTextDateCreate tool
         */
        FREE_TEXT_DATE_CREATE(49),
        /**
         * Identifier of the AreaMeasureCreate tool
         */
        RECT_AREA_MEASURE_CREATE(50),
        /**
         * Identifier of the convenience ink and highligther in one tool
         */
        SMART_PEN_INK(51),
        SMART_PEN_TEXT_MARKUP(52),
        COUNT_MEASUREMENT(53);
        // Note: when adding a new entry, update NUM_TOOL_MODE

        private final static int NUM_TOOL_MODE = 53;

        private final int mode;

        ToolMode(int mode) {
            this.mode = mode;
        }

        public int getValue() {
            return this.mode;
        }

        private static SparseArray<ToolModeBase> map = new SparseArray<>();

        static {
            for (ToolMode toolMode : ToolMode.values()) {
                map.put(toolMode.getValue(), toolMode);
            }
        }

        private static AtomicInteger modeGenerator = new AtomicInteger(NUM_TOOL_MODE * 2);
        // multiply to 2 for safety just in case forgot to update NUM_TOOL_MODE

        /**
         * Gets tool mode based on tool mode value
         *
         * @param toolMode tool mode value
         * @return tool mode
         */
        public static ToolModeBase toolModeFor(int toolMode) {
            return map.get(toolMode);
        }

        /**
         * Add a new tool mode
         *
         * @return new tool mode
         */
        public static ToolModeBase addNewMode() {
            return addNewMode(Annot.e_Unknown);
        }

        /**
         * Add a new creator tool mode
         *
         * @param annotType annotation type that this tool creates
         * @return new tool mode
         */
        public static ToolModeBase addNewMode(final int annotType) {
            final int newMode = modeGenerator.incrementAndGet();
            ToolModeBase mode = new ToolModeBase() {
                @Override
                public int getValue() {
                    return newMode;
                }
            };
            map.put(newMode, mode);
            return mode;
        }
    }

    public static ToolMode getDefaultToolMode(ToolModeBase toolModeBase) {
        if (toolModeBase instanceof ToolMode) {
            return (ToolMode) toolModeBase;
        }
        return ToolMode.PAN;
    }

    public static ToolModeBase getDefaultToolModeBase(ToolModeBase toolModeBase) {
        if (toolModeBase != null) {
            return toolModeBase;
        }
        return ToolMode.PAN;
    }

    private ArrayList<FullSaveListener> mFullSaveListeners;
    private ArrayList<ActionGoBackListener> mActionGoBackListeners;
    private ArrayList<ToolSetListener> mToolSetListeners;
    private ArrayList<ToolChangedListener> mToolChangedListeners;
    private ArrayList<ToolChangedListener> mToolCreatedListeners;
    private CopyOnWriteArray<OnLayoutListener> mOnLayoutListeners;
    private PreToolManagerListener mPreToolManagerListener;
    private PreFlingListener mPreFlingListener;
    private QuickMenuListener mQuickMenuListener;
    private ArrayList<AnnotationModificationListener> mAnnotationModificationListeners;
    private ArrayList<PdfDocModificationListener> mPdfDocModificationListeners;
    private ArrayList<PdfTextModificationListener> mPdfTextModificationListeners;
    private ArrayList<PdfOutlineModificationListener> mPdfOutlineModificationListeners;
    private AdvancedAnnotationListener mAdvancedAnnotationListener;
    private SpecialAnnotationListener mSpecialAnnotationListener;
    private FileAttachmentAnnotationListener mFileAttachmentAnnotationListener;
    private BasicAnnotationListener mBasicAnnotationListener;
    @Nullable
    private ArrayList<DialogListener> mDialogListeners;
    private ArrayList<AnnotationsSelectionListener> mAnnotationsSelectionListeners;
    private AnnotationToolbarListener mAnnotationToolbarListener;
    private OnGenericMotionEventListener mOnGenericMotionEventListener;
    private ExternalAnnotationManagerListener mExternalAnnotationManagerListener;
    private EdgeEffectListener mEdgeEffectListener;
    private StampDialogListener mStampDialogListener;
    private PresetsListener mPresetsListener;
    private SnackbarListener mSnackbarListener;
    private OnAnnotStyleChangedListener mOnAnnotStyleChangedListener;

    private boolean mPageNumberIndicatorVisible = true;

    private boolean mSkipNextTapEvent = false;
    private boolean mSkipNextTouchEvent = false;
    private boolean mPermissionToRecordAccepted = false;

    private Tool mTool;
    private PDFViewCtrl mPdfViewCtrl;
    private UndoRedoManager mUndoRedoManger;
    private AnnotManager mAnnotManager = null;

    private RedactionManager mRedactionManager;

    private SoundManager mSoundManager;

    private boolean mReadOnly = false;
    private boolean mTextMarkupAdobeHack = true;

    private boolean mCanCheckAnnotPermission = false;

    private boolean mShowAuthorDialog = false;

    // permission
    private String mAuthorId;
    private String mAuthorName;

    private boolean mCopyAnnotatedTextToNote = false;
    private boolean mStylusAsPen = false;
    private boolean mIsNightMode = false;
    private boolean mInkSmoothing = true;
    private boolean mStickyNoteShowPopup = true;
    private boolean mEditInkAnnots = false;
    private boolean mCanOpenEditToolbarFromPan = true;
    private boolean mAddImageStamperTool = false;
    private @ColorInt int mFreeTextNoteToggleButtonColorInt = -1;

    private Set<ToolMode> mDisabledToolModes;
    private Set<ToolMode> mDisabledToolModesSave;
    private ArrayList<ToolMode> mAnnotToolbarPrecedence;

    private TextToSpeech mTTS;

    // loupe
    private SelectionLoupe mSelectionLoupe; // required due to tool loop
    private SelectionLoupe mSelectionLoupeRound; // required due to tool loop
    private Canvas mSelectionLoupeCanvas;
    private Canvas mSelectionLoupeCanvasRound;
    private Bitmap mSelectionLoupeBitmap;
    private Bitmap mSelectionLoupeBitmapRound;

    // draw UI until rendering finishes to prevent flash
    private ArrayList<Tool> mOldTools;

    private String mSelectedAnnotId;
    private int mSelectedAnnotPageNum = -1;

    private boolean mQuickMenuJustClosed = false;

    private boolean mIsAutoSelectAnnotation = true;

    private boolean mDisableQuickMenu = false;

    private boolean mDoubleTapToZoom = true;

    private boolean mAutoResizeFreeText = false;

    private boolean mRealTimeAnnotEdit = true;

    private boolean mEditFreeTextOnTap = false;

    private boolean freeTextInlineToggleEnabled = true;

    private boolean mDeleteEmptyFreeText = true;

    private boolean mShowSavedSignature = true;

    private boolean mDefaultStoreNewSignature = true;

    private boolean mPersistStoreSignatureSetting = true;

    private boolean mShowSignaturePresets = true;

    private boolean mShowSignatureFromImage = true;

    private boolean mShowTypedSignature = true;

    private boolean mUsingDigitalSignature;

    private String mDigitalSignatureKeystorePath;

    private String mDigitalSignatureKeystorePassword;

    private boolean mRestrictedTapAnnotCreation;

    private boolean mUsePressureSensitiveSignatures = true;

    private Eraser.EraserType mEraserType = Eraser.EraserType.INK_ERASER;

    private AnnotEditRectGroup.SelectionMode mMultiSelectMode = AnnotEditRectGroup.SelectionMode.RECTANGULAR;

    private boolean mShowUndoRedo = true;

    private int mSelectionBoxMargin = DrawingUtils.sSelectionBoxMargin;
    private int mTapToCreateShapeHalfWidth = SimpleShapeCreate.sTapToCreateHalfWidth;

    private int[] mSignatureColors;

    // for caching related
    private String mCacheFileName;

    // for lifecycle
    private boolean mCanResumePdfDocWithoutReloading;

    // snapping
    private boolean mSnappingEnabled;
    private int mSnappingModes = GeometryCollection.SnappingMode.e_default_snap_mode;

    // rich content
    private boolean mRichContentEnabled;
    private boolean mShowRichContentOption;

    // font
    private boolean mFontLoaded;

    // edit pdf
    private boolean mPdfContentEditingEnabled;

    @SuppressWarnings("FieldCanBeLocal")
    private boolean mShowAnnotIndicators;
    private AnnotIndicatorManger mAnnotIndicatorManger;
    private HashMap<ToolModeBase, Class<? extends com.pdftron.pdf.tools.Tool>> mCustomizedToolClassMap;
    private HashMap<ToolModeBase, Object[]> mCustomizedToolParamMap;
    private Class<? extends Pan> mDefaultToolClass = Pan.class;
    // built in page number indicator
    private PopupWindow mPageIndicatorPopup;

    private @Nullable
    WeakReference<FragmentActivity> mCurrentActivity;

    protected CompositeDisposable mDisposables = new CompositeDisposable();
    final TextSearchSelections mTextSearchSelections = new TextSearchSelections();

    private final HashMap<Integer, AnnotStyleProperty> mAnnotStyleProperties = new HashMap<>();

    // XFDF cannot generate Signature appearance hack, remove when core fixes
    private boolean mSignSignatureFieldsWithStamps;

    // theme
    private ThemeProvider mThemeProvider = new ThemeProvider();

    // life cycle
    private boolean mPaused;

    // Whether to skip the read only check in PDFViewCtrl
    private boolean mSkipReadOnlyCheck = false;

    private boolean mShowRotateHandle = true;

    private boolean mSkipSameToolCreation;

    private boolean mInkMultiStrokeEnabled = true;

    private boolean mMoveAnnotBetweenPages = false;

    private boolean mFreehandTimerEnabled = true;

    private boolean mLoupeEnabled = true;

    private float mFreeHighlighterAutoSmoothingRange = FreeHighlighterCreate.AUTO_SMOOTH_RANGE_DEFAULT;

    @Nullable
    private AnnotAction mLastAnnotAction = null;

    @NonNull
    private AnnotSnappingManager mAnnotSnappingManager = new AnnotSnappingManager();

    private final SnapUtils mSnapUtils;

    private AnnotationPermissionListener mAnnotationPermissionListener;

    private boolean mTextFieldCustomAppearanceEnabled = false;

    /**
     * Class constructor.
     *
     * @param pdfViewCtrl the {@link com.pdftron.pdf.PDFViewCtrl}. It must not be null.
     */
    public ToolManager(@NonNull PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
        mSnapUtils = new SnapUtils(mPdfViewCtrl);
        mPdfViewCtrl.setActionCompletedListener(this);
        try {
            mPdfViewCtrl.enableUndoRedo();
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        mUndoRedoManger = new UndoRedoManager(this);
        mOldTools = new ArrayList<>();

        // load system fonts
        mDisposables.add(AnnotUtils.loadSystemFonts()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        mFontLoaded = true;
                        Logger.INSTANCE.LogD("PDFNet", "getSystemFontList completed.");

                        // save to shared pref
                        SharedPreferences settings = com.pdftron.pdf.tools.Tool.getToolPreferences(mPdfViewCtrl.getContext());
                        String fontInfo = settings.getString(com.pdftron.pdf.tools.Tool.ANNOTATION_FREE_TEXT_FONTS, "");
                        if (fontInfo.equals("")) {
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putString(com.pdftron.pdf.tools.Tool.ANNOTATION_FREE_TEXT_FONTS, s);
                            editor.apply();
                            Logger.INSTANCE.LogD("PDFNet", "getSystemFontList write to disk.");
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        mFontLoaded = false;
                    }
                })
        );
    }

    /**
     * Resets annotation indicators
     */
    public void resetIndicator() {
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.reset(true);
        }
    }

    /**
     * Creates the default tool (Pan tool).
     * If instantiate default tool failed, it will create {@link Pan} tool
     *
     * @return the default tool
     */
    public Pan createDefaultTool() {
        Pan tool;
        try {
            tool = mDefaultToolClass.getDeclaredConstructor(mPdfViewCtrl.getClass()).newInstance(mPdfViewCtrl);
            sendToolCreatedEvent(tool, null);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e, "failed to instantiate default tool");
            tool = (Pan) createTool(ToolMode.PAN, null);
        }
        // Must be set for new toolbar, allows for quick menu tools to have preset bar
        tool.enablePresetMode();
        tool.setPageNumberIndicatorVisible(mPageNumberIndicatorVisible);
        tool.onCreate();

        return tool;
    }

    /**
     * Creates the specified tool and copies the necessary info from the
     * previous tool if provided. Calls the {@link ToolChangedListener#toolChanged(Tool, Tool)}
     * if possible.
     * <p>
     * It showPreset is true, then the default presets are shown when using the tool.
     *
     * @param newTool     the identifier for the tool to be created
     * @param currentTool the current tool before this call
     * @param showPreset  whether this tool should be associated with preset bundle data
     */
    public Tool createTool(ToolModeBase newTool, @Nullable Tool currentTool, boolean showPreset) {
        if (showPreset) {
            ToolbarButtonType toolbarButtonType = ToolModeMapper.getButtonType(newTool);
            if (toolbarButtonType != null) {
                ToolbarItem quickMenuItem = new QuickMenuToolbarItem(
                        "default_preset",
                        toolbarButtonType,
                        -1000, // can be anything, only used for preset shared preferences in this case
                        false,
                        toolbarButtonType.title,
                        toolbarButtonType.icon,
                        MenuItem.SHOW_AS_ACTION_IF_ROOM,
                        0
                );
                Bundle bundle = new Bundle();
                bundle.putParcelable(TOOLBAR_ITEM_BUNDLE, quickMenuItem);
                return createTool(newTool, currentTool, bundle);
            } else {
                return createTool(newTool, currentTool, null);
            }
        } else {
            return createTool(newTool, currentTool, null);
        }
    }

    /**
     * Creates the specified tool and copies the necessary info from the
     * previous tool if provided. Calls the {@link ToolChangedListener#toolChanged(Tool, Tool)}
     * if possible.
     *
     * @param newTool     the identifier for the tool to be created
     * @param currentTool the current tool before this call
     */
    public Tool createTool(ToolModeBase newTool, @Nullable Tool currentTool) {
        return createTool(newTool, currentTool, null);
    }

    /**
     * Creates the specified tool and copies the necessary info from the
     * previous tool if provided. Calls the {@link ToolChangedListener#toolChanged(Tool, Tool)}
     * if possible.
     *
     * @param newTool     the identifier for the tool to be created
     * @param currentTool the current tool before this call
     * @param bundle      bundle containing metadata
     */
    public Tool createTool(ToolModeBase newTool, @Nullable Tool currentTool, @Nullable Bundle bundle) {
        return createTool(newTool, currentTool, bundle, false);
    }

    public Tool createTool(ToolModeBase newTool, @Nullable Tool currentTool, @Nullable Bundle bundle, boolean force) {
        if (!force) {
            // when forced, we will always create the new tool
            if (mPaused) {
                Pan defaultTool = createDefaultTool();
                if (bundle != null) {
                    defaultTool.setBundle(bundle);
                }
                return defaultTool;
            }
            if (this.mSkipSameToolCreation) {
                if (currentTool != null) {
                    if (currentTool.getToolMode() == newTool) {
                        return currentTool;
                    }
                }
            }
        }
        com.pdftron.pdf.tools.Tool tool = safeCreateTool(newTool);
        if (bundle != null) {
            tool.setBundle(bundle);
        }

        if (tool.getToolMode() == ToolMode.SOUND_CREATE) {
            if (mCurrentActivity != null && mCurrentActivity.get() != null) {
                if (!Utils.hasRecordAudioPermission(mCurrentActivity.get())) {
                    Utils.requestRecordAudioPermissions(mCurrentActivity.get(), mPdfViewCtrl.getRootView(), RequestCode.RECORD_AUDIO);
                    return mTool;
                }
            }
        }
        tool.setPageNumberIndicatorVisible(mPageNumberIndicatorVisible);

        if (currentTool == null || tool.getToolMode() != currentTool.getToolMode()) {
            if (mTool != null && currentTool == null) {
                mTool.onClose();
            }
            if (mToolChangedListeners != null) {
                for (ToolChangedListener listener : mToolChangedListeners) {
                    listener.toolChanged(tool, currentTool);
                }
            }
        }

        if (currentTool != null) {
            com.pdftron.pdf.tools.Tool oldTool = (com.pdftron.pdf.tools.Tool) currentTool;
            tool.mAnnot = oldTool.mAnnot;
            tool.mAnnotBBox = oldTool.mAnnotBBox;
            tool.mAnnotPageNum = oldTool.mAnnotPageNum;
            tool.mGroupAnnots = oldTool.mGroupAnnots;
            tool.mAvoidLongPressAttempt = oldTool.mAvoidLongPressAttempt;
            if (oldTool instanceof SimpleShapeCreate &&
                    ((SimpleShapeCreate) oldTool).canTapToCreate() &&
                    tool instanceof AnnotEdit) {
                // since mAnnotPushedBack is always set onDown of creation tool
                // this flag will only affect the next edit tool in the tool loop
                tool.mAnnotPushedBack = oldTool.mAnnotPushedBack;
            }
            if (tool.getToolMode() != ToolMode.PAN) {
                tool.mCurrentDefaultToolMode = oldTool.mCurrentDefaultToolMode;
            } else {
                tool.mCurrentDefaultToolMode = ToolMode.PAN;
            }
            tool.mForceSameNextToolMode = oldTool.mForceSameNextToolMode;
            if (oldTool.mForceSameNextToolMode) {
                tool.mStylusUsed = oldTool.mStylusUsed;
                tool.mAnnotStyle = oldTool.mAnnotStyle;
            }
            if (oldTool.mAllowTapToSelect) {
                tool.mAllowTapToSelect = oldTool.mAllowTapToSelect;
                tool.mMultiStrokeMode = oldTool.mMultiStrokeMode;
                tool.mTimedModeEnabled = oldTool.mTimedModeEnabled;
            }
            tool.mAnnotView = oldTool.mAnnotView;
            oldTool.onClose();   // Close the old tool; old tool can use this to clean up things.

            if (oldTool.getToolMode() != tool.getToolMode()) {
                tool.setJustCreatedFromAnotherTool();
            }

            if (oldTool.mCurrentDefaultToolMode != tool.getToolMode()) {
                setQuickMenuJustClosed(false);
            }

            // When creating sticky note, let annotation edit tool pop up the note dialog
            // directly, instead of showing the menu as the intermediate step.
            if (oldTool.getToolMode() == ToolMode.TEXT_ANNOT_CREATE && tool.getToolMode() == ToolMode.ANNOT_EDIT) {
                //noinspection ConstantConditions
                AnnotEdit at = (AnnotEdit) tool;
                at.setUpFromStickyCreate(true);
                at.mForceSameNextToolMode = oldTool.mForceSameNextToolMode;
            }

            // When creating free text, let annotation edit tool pop up the note dialog
            // directly, instead of showing the menu as the intermediate step.
            if ((oldTool.getToolMode() == ToolMode.TEXT_CREATE ||
                    oldTool.getToolMode() == ToolMode.CALLOUT_CREATE) &&
                    (tool.getToolMode() == ToolMode.ANNOT_EDIT ||
                            tool.getToolMode() == ToolMode.ANNOT_EDIT_ADVANCED_SHAPE)) {
                //noinspection ConstantConditions
                AnnotEdit at = (AnnotEdit) tool;
                if (oldTool.getToolMode() == ToolMode.TEXT_CREATE) {
                    at.setUpFromFreeTextCreate(true);
                } else if (oldTool.getToolMode() == ToolMode.CALLOUT_CREATE) {
                    at.mUpFromCalloutCreate = oldTool.mUpFromCalloutCreate;
                }
                at.mForceSameNextToolMode = oldTool.mForceSameNextToolMode;
            }

            // When erase using single tap, do not show annot edit bounding box
            if (oldTool.getToolMode() == ToolMode.INK_ERASER && tool.getToolMode() == ToolMode.PAN) {
                //noinspection ConstantConditions
                Pan pan = (Pan) tool;
                pan.mSuppressSingleTapConfirmed = true;
            }
        } else if (getTool() != null && getTool() instanceof com.pdftron.pdf.tools.Tool) {
            // cleanup
            com.pdftron.pdf.tools.Tool oldTool = (com.pdftron.pdf.tools.Tool) getTool();
            if (oldTool.mAnnotView != null) {
                oldTool.removeAnnotView(false);
            }
        }

        // Class a tool's onCreate() function in which the tool can initialize things.
        tool.onCreate();

        sendToolCreatedEvent(tool, currentTool);

        return tool;
    }

    private void sendToolCreatedEvent(@NonNull com.pdftron.pdf.tools.Tool tool, @Nullable Tool currentTool) {
        if (mToolCreatedListeners != null) {
            for (ToolChangedListener listener : mToolCreatedListeners) {
                listener.toolChanged(tool, currentTool);
            }
        }
    }

    private void sendToolSetEvent(@NonNull ToolManager.Tool tool) {
        if (mToolSetListeners != null) {
            for (ToolSetListener listener : mToolSetListeners) {
                listener.onToolSet(tool);
            }
        }
    }

    /**
     * Used to force the next tool to be the default tool if previously set
     */
    public void backToDefaultTool() {
        if (getTool() instanceof com.pdftron.pdf.tools.Tool) {
            com.pdftron.pdf.tools.Tool tool = ((com.pdftron.pdf.tools.Tool) getTool());
            tool.backToDefaultTool();
        }
    }

    /**
     * Creates tool safely, if there is any exception, creates default tool
     *
     * @param mode tool mode
     * @return tool
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public com.pdftron.pdf.tools.Tool safeCreateTool(ToolModeBase mode) {
        com.pdftron.pdf.tools.Tool tool;
        ToolModeBase actualMode = mode;
        if (mDisabledToolModes != null && mDisabledToolModes.contains(mode)) {
            actualMode = ToolMode.PAN;
        }
        try {
            Object[] toolArgs = getToolArguments(actualMode);
            Class toolClass = getToolClassByMode(actualMode);
            tool = instantiateTool(toolClass, toolArgs);
        } catch (Exception e) {
            tool = createDefaultTool();
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } catch (OutOfMemoryError oom) {
            Utils.manageOOM(mPdfViewCtrl);
            tool = createDefaultTool();
        }
        return tool;
    }

    /**
     * get tool class by tool mode
     *
     * @param modeBase tool mode
     * @return tool class
     */
    private Class<? extends com.pdftron.pdf.tools.Tool> getToolClassByMode(ToolModeBase modeBase) {
        if (null != mCustomizedToolClassMap && mCustomizedToolClassMap.containsKey(modeBase)) {
            return mCustomizedToolClassMap.get(modeBase);
        }
        ToolMode mode = getDefaultToolMode(modeBase);
        switch (mode) {
            case PAN:
                return Pan.class;
            case ANNOT_EDIT:
                return AnnotEdit.class;
            case LINE_CREATE:
                return LineCreate.class;
            case ARROW_CREATE:
                return ArrowCreate.class;
            case RULER_CREATE:
                return RulerCreate.class;
            case PERIMETER_MEASURE_CREATE:
                return PerimeterMeasureCreate.class;
            case AREA_MEASURE_CREATE:
                return AreaMeasureCreate.class;
            case RECT_AREA_MEASURE_CREATE:
                return RectAreaMeasureCreate.class;
            case POLYLINE_CREATE:
                return PolylineCreate.class;
            case RECT_CREATE:
                return RectCreate.class;
            case OVAL_CREATE:
                return OvalCreate.class;
            case SOUND_CREATE:
                return SoundCreate.class;
            case FILE_ATTACHMENT_CREATE:
                return FileAttachmentCreate.class;
            case POLYGON_CREATE:
                return PolygonCreate.class;
            case CLOUD_CREATE:
                return CloudCreate.class;
            case INK_CREATE:
                return FreehandCreate.class;
            case FREE_HIGHLIGHTER:
                return FreeHighlighterCreate.class;
            case TEXT_ANNOT_CREATE:
                return StickyNoteCreate.class;
            case LINK_ACTION:
                return LinkAction.class;
            case TEXT_SELECT:
                return TextSelect.class;
            case FORM_FILL:
                return FormFill.class;
            case TEXT_CREATE:
                return FreeTextCreate.class;
            case CALLOUT_CREATE:
                return CalloutCreate.class;
            case ANNOT_EDIT_LINE:
                return AnnotEditLine.class;
            case ANNOT_EDIT_ADVANCED_SHAPE:
                return AnnotEditAdvancedShape.class;
            case RICH_MEDIA:
                return RichMedia.class;
            case TEXT_UNDERLINE:
                return TextUnderlineCreate.class;
            case TEXT_HIGHLIGHT:
                return TextHighlightCreate.class;
            case TEXT_SQUIGGLY:
                return TextSquigglyCreate.class;
            case TEXT_STRIKEOUT:
                return TextStrikeoutCreate.class;
            case TEXT_REDACTION:
                return TextRedactionCreate.class;
            case RECT_REDACTION:
                return RectRedactionCreate.class;
            case INK_ERASER:
                return Eraser.class;
            case ANNOT_EDIT_TEXT_MARKUP:
                return AnnotEditTextMarkup.class;
            case TEXT_HIGHLIGHTER:
                return TextHighlighter.class;
            case DIGITAL_SIGNATURE:
                return DigitalSignature.class;
            case SIGNATURE:
                return Signature.class;
            case STAMPER:
                return Stamper.class;
            case RUBBER_STAMPER:
                return RubberStampCreate.class;
            case RECT_LINK:
                return RectLinkCreate.class;
            case FORM_SIGNATURE_CREATE:
                return SignatureFieldCreate.class;
            case FORM_TEXT_FIELD_CREATE:
                return TextFieldCreate.class;
            case TEXT_LINK_CREATE:
                return TextLinkCreate.class;
            case FORM_CHECKBOX_CREATE:
                return CheckboxFieldCreate.class;
            case FORM_COMBO_BOX_CREATE:
                return ComboBoxFieldCreate.class;
            case FORM_LIST_BOX_CREATE:
                return ListBoxFieldCreate.class;
            case FORM_RADIO_GROUP_CREATE:
                return RadioGroupFieldCreate.class;
            case ANNOT_EDIT_RECT_GROUP:
                return AnnotEditRectGroup.class;
            case FREE_TEXT_SPACING_CREATE:
                return FreeTextSpacingCreate.class;
            case FREE_TEXT_DATE_CREATE:
                return FreeTextDateCreate.class;
            case SMART_PEN_INK:
                return SmartPenInk.class;
            case SMART_PEN_TEXT_MARKUP:
                return SmartPenMarkup.class;
            case COUNT_MEASUREMENT:
                return CountMeasurementCreateTool.class;
            default:
                return Pan.class;
        }
    }

    /**
     * get tool arguments by tool mode for instantiating tool
     * By default, instantiating new tool needs {@link #mPdfViewCtrl}
     *
     * @param mode tool mode
     * @return arguments for instantiating new tool, default is [ @link #mPdfViewCtrl} ]
     */
    private Object[] getToolArguments(ToolModeBase mode) {
        if (null != mCustomizedToolParamMap && mCustomizedToolParamMap.containsKey(mode)) {
            return mCustomizedToolParamMap.get(mode);
        }
        if (mode == ToolMode.INK_ERASER) {
            return new Object[]{mPdfViewCtrl, mEraserType};
        } else if (mode == ToolMode.ANNOT_EDIT_RECT_GROUP) {
            return new Object[]{mPdfViewCtrl, mMultiSelectMode};
        }
        return new Object[]{mPdfViewCtrl};
    }

    /**
     * Creates next tool by tool class and arguments
     *
     * @param toolClass tool class
     * @param args      arguments for instantiate the tool
     * @return next tool@throws
     * @throws NoSuchMethodException       if a matching method is not found.
     *                                     Please add a public static newInstance(Object... args) to your custom Tool class
     * @throws IllegalAccessException      if this {@code Constructor} object
     *                                     is enforcing Java language access control and the underlying
     *                                     constructor is inaccessible.
     * @throws IllegalArgumentException    if the number of actual
     *                                     and formal parameters differ; if an unwrapping
     *                                     conversion for primitive arguments fails; or if,
     *                                     after possible unwrapping, a parameter value
     *                                     cannot be converted to the corresponding formal
     *                                     parameter type by a method invocation conversion; if
     *                                     this constructor pertains to an enum type.
     * @throws InstantiationException      if the class that declares the
     *                                     underlying constructor represents an abstract class.
     * @throws InvocationTargetException   if the underlying constructor
     *                                     throws an exception.
     * @throws ExceptionInInitializerError if the initialization provoked
     *                                     by this method fails.
     */
    private com.pdftron.pdf.tools.Tool instantiateTool(Class<? extends Tool> toolClass, Object... args) throws NoSuchMethodException,
            IllegalAccessException,
            InvocationTargetException,
            InstantiationException {
        Class[] cArg = processArguments(args);
        return (com.pdftron.pdf.tools.Tool) toolClass.getDeclaredConstructor(cArg).newInstance(args);
    }

    private Class[] processArguments(Object... args) {
        Class[] cArg = new Class[args.length];
        int i = 0;
        for (Object arg : args) {
            if (arg instanceof PDFViewCtrl) {
                cArg[i] = PDFViewCtrl.class;
            } else {
                cArg[i] = arg.getClass();
            }
            i++;
        }
        return cArg;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onControlReady()}.
     */
    @Override
    public void onControlReady() {
        if (mTool == null) {
            setTool(createDefaultTool());
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onClose()}.
     */
    @Override
    public void onClose() {
        if (mTool != null) {
            mTool.onClose();
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onCustomEvent(Object)}.
     */
    @Override
    public void onCustomEvent(Object obj) {
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onCustomEvent(obj);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onKeyUp(int, KeyEvent)}.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mPreToolManagerListener != null) {
            if (mPreToolManagerListener.onKeyUp(keyCode, event)) {
                return true;
            }
        }
        boolean handled = false;

        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                handled = mTool.onKeyUp(keyCode, event);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }

        return handled;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onDoubleTap(MotionEvent)}.
     */
    @Override
    public boolean onDoubleTap(MotionEvent e) {
        if (mPreToolManagerListener != null) {
            if (mPreToolManagerListener.onDoubleTap(e)) {
                return true;
            }
        }
        boolean handled = false;

        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                handled = mTool.onDoubleTap(e);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }

        return handled;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onDoubleTapEvent(MotionEvent)}.
     */
    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        boolean handled = false;

        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                handled = mTool.onDoubleTapEvent(e);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }

        return handled;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onDoubleTapEnd(MotionEvent)}.
     */
    @Override
    public void onDoubleTapEnd(MotionEvent e) {
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onDoubleTapEnd(e);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onDown(MotionEvent)}.
     */
    @Override
    public boolean onDown(MotionEvent e) {
        if (mSkipNextTouchEvent) {
            return true;
        }

        if (mPreToolManagerListener != null) {
            if (mPreToolManagerListener.onDown(e)) {
                return true;
            }
        }

        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onDown(e);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));

                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }

        return false;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onPointerDown(MotionEvent)}.
     */
    @Override
    public boolean onPointerDown(MotionEvent e) {
        if (mSkipNextTouchEvent) {
            return true;
        }

        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onPointerDown(e);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));

                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }

        return false;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onDocumentDownloadEvent(PDFViewCtrl.DownloadState, int, int, int, String)}.
     */
    @Override
    public void onDocumentDownloadEvent(PDFViewCtrl.DownloadState mode, int page_num, int page_downloaded, int page_count, String message) {
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onDocumentDownloadEvent(mode, page_num, page_downloaded, page_count, message);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onDraw(Canvas, Matrix)}.
     */
    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        // Draw old Tools UI to prevent flash when waiting for rendering
        if (mOldTools != null) {
            for (Tool t : mOldTools) {
                t.onDraw(canvas, tfm);
            }
        }

        if (mTool != null) {
            mTool.onDraw(canvas, tfm);
        }

        if (mAnnotIndicatorManger != null) {
            boolean canDrawIndicator = true;
            if (mTool instanceof BaseTool) {
                canDrawIndicator = !((BaseTool) mTool).isDrawingLoupe();
            }
            if (canDrawIndicator) {
                mAnnotIndicatorManger.drawAnnotIndicators(canvas);
            }
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onFlingStop()}.
     */
    @Override
    public boolean onFlingStop() {
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.updateState(AnnotIndicatorManger.STATE_IS_NORMAL);
        }
        if (mPreFlingListener != null) {
            if (mPreFlingListener.onFlingStop()) {
                return true;
            }
        }

        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onFlingStop();
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }

        return false;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onLayout(boolean, int, int, int, int)}.
     */
    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.reset(true);
        }

        if (mTool != null) {
            mTool.onLayout(changed, l, t, r, b);
        }

        if (mOldTools != null) {
            for (Tool tool : mOldTools) {
                tool.onLayout(changed, l, t, r, b);
            }
        }

        final CopyOnWriteArray<OnLayoutListener> listeners = mOnLayoutListeners;
        if (listeners != null && listeners.size() > 0) {
            CopyOnWriteArray.Access<OnLayoutListener> access = listeners.start();
            try {
                int count = access.size();
                for (int i = 0; i < count; i++) {
                    access.get(i).onLayout(changed, l, t, r, b);
                }
            } finally {
                listeners.end();
            }
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onLongPress(MotionEvent)}.
     */
    @Override
    public boolean onLongPress(MotionEvent e) {
        if (mPreToolManagerListener != null) {
            if (mPreToolManagerListener.onLongPress(e)) {
                return true;
            }
        }
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                boolean handled = mTool.onLongPress(e);
                if (!handled) {
                    next_tm = mTool.getNextToolMode();
                    if (prev_tm != next_tm) {
                        setTool(createTool(next_tm, mTool));
                        prev_tm = next_tm;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            } while (true);
        }

        return false;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        if (mSkipNextTouchEvent) {
            return true;
        }
        if (mPreToolManagerListener != null) {
            if (mPreToolManagerListener.onMove(e1, e2, x_dist, y_dist)) {
                return true;
            }
        }

        boolean handled = false;
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                // to improve freehand writing, onMove is added to PDFViewCtrl.onTouchEvent
                // however, we do not want this to affect anything else
                if (mTool.getToolMode() == ToolMode.INK_CREATE ||
                        mTool.getToolMode() == ToolMode.SMART_PEN_INK ||
                        mTool.getToolMode() == ToolMode.INK_ERASER) {
                    // handle all move events for ink create or eraser
                    handled |= mTool.onMove(e1, e2, x_dist, y_dist);
                } else {
                    // for any other tools, only handle if x_dist and y_dist not equal to -1
                    //noinspection SimplifiableIfStatement
                    if ((Float.compare(x_dist, -1) == 0) && (Float.compare(y_dist, -1) == 0)) {
                        handled |= true;
                    } else {
                        handled |= mTool.onMove(e1, e2, x_dist, y_dist);
                    }
                }

                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }

        return handled;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onScrollChanged(int, int, int, int)}.
     */
    @Override
    public void onScrollChanged(int l, int t, int oldl, int oldt) {
        mQuickMenuJustClosed = false; // clear this flag if it is not consumed by onSingleTapConfirmed
        if (mPreToolManagerListener != null) {
            mPreToolManagerListener.onScrollChanged(l, t, oldl, oldt);
        }
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onScrollChanged(l, t, oldl, oldt);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onPageTurning(int, int)}.
     */
    @Override
    public void onPageTurning(int old_page, int cur_page) {
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.updateState(AnnotIndicatorManger.STATE_IS_NORMAL);
            mAnnotIndicatorManger.reset(false);
        }

        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onPageTurning(old_page, cur_page);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onScale(float, float)}.
     */
    @Override
    public boolean onScale(float x, float y) {
        if (mPreToolManagerListener != null) {
            if (mPreToolManagerListener.onScale(x, y)) {
                return true;
            }
        }

        if (mTool != null) {
            boolean handled;
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;

            do {
                handled = mTool.onScale(x, y);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);

            if (handled) {
                return true;
            }
        }

        return false;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onScaleBegin(float, float)}.
     */
    @Override
    public boolean onScaleBegin(float x, float y) {
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.updateState(AnnotIndicatorManger.STATE_IS_ZOOMING);
        }

        if (mPreToolManagerListener != null) {
            if (mPreToolManagerListener.onScaleBegin(x, y)) {
                return true;
            }
        }

        if (mTool != null) {
            boolean handled;
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;

            do {
                handled = mTool.onScaleBegin(x, y);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);

            if (handled) {
                return true;
            }
        }

        return false;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onScaleEnd(float, float)}.
     */
    @Override
    public boolean onScaleEnd(float x, float y) {
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.updateState(AnnotIndicatorManger.STATE_IS_NORMAL);
            mAnnotIndicatorManger.reset(true);
        }

        if (mPreToolManagerListener != null) {
            if (mPreToolManagerListener.onScaleEnd(x, y)) {
                return true;
            }
        }

        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onScaleEnd(x, y);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));
                    prev_tm = next_tm;
                } else {
                    break;
                }
            }
            while (true);
        }

        return false;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onSetDoc()}.
     */
    @Override
    public void onSetDoc() {
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onSetDoc();
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onShowPress(MotionEvent)}.
     */
    @Override
    public boolean onShowPress(MotionEvent e) {
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onShowPress(e);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }

        return false;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onSingleTapConfirmed(MotionEvent)}.
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        if (mSkipNextTapEvent) {
            mSkipNextTapEvent = false;
            return true;
        }
        if (mPreToolManagerListener != null) {
            if (mPreToolManagerListener.onSingleTapConfirmed(e)) {
                return true;
            }
        }
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                boolean handled = mTool.onSingleTapConfirmed(e);
                if (!handled) {
                    next_tm = mTool.getNextToolMode();
                    if (prev_tm != next_tm) {
                        setTool(createTool(next_tm, mTool));
                        prev_tm = next_tm;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            } while (true);
        }

        // for a single tap, onSingleTapConfirmed event is called after all generic motion events,
        // so we may need to take care of generic motion event handler if new tool is selected
        onGenericMotionEvent(e);

        return false;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onPostSingleTapConfirmed()}.
     */
    @Override
    public void onPostSingleTapConfirmed() {
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onPostSingleTapConfirmed();
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onSingleTapUp(MotionEvent)}.
     */
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onSingleTapUp(e);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }

        return false;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        if (mAnnotIndicatorManger != null && priorEventMode == PDFViewCtrl.PriorEventMode.FLING) {
            mAnnotIndicatorManger.updateState(AnnotIndicatorManger.STATE_IS_FLUNG);
        }

        if (mSkipNextTouchEvent) {
            mSkipNextTouchEvent = false;
            return true;
        }
        if (mPreToolManagerListener != null) {
            if (mPreToolManagerListener.onUp(e, priorEventMode)) {
                return true;
            }
        }
        if (mPreFlingListener != null && priorEventMode == PDFViewCtrl.PriorEventMode.FLING) {
            if (mPreFlingListener.onFling(e)) {
                return true;
            }
        }

        boolean handled = false;
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                handled |= mTool.onUp(e, priorEventMode);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }

        return handled;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onGenericMotionEvent(MotionEvent)}.
     */
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mOnGenericMotionEventListener != null) {
            mOnGenericMotionEventListener.onGenericMotionEvent(event);
        }

        if (ShortcutHelper.isLongPress(event)) {
            onLongPress(event);
            return true;
        }

        return mTool != null && ((com.pdftron.pdf.tools.Tool) mTool).onGenericMotionEvent(event);
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onChangePointerIcon(PointerIcon)}.
     */
    @Override
    public void onChangePointerIcon(PointerIcon pointerIcon) {
        if (mOnGenericMotionEventListener != null) {
            mOnGenericMotionEventListener.onChangePointerIcon(pointerIcon);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onConfigurationChanged(Configuration)}.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onConfigurationChanged(newConfig);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onDrawEdgeEffects(Canvas, int, int)}.
     */
    @Override
    public boolean onDrawEdgeEffects(Canvas canvas, int width, int verticalOffset) {
        boolean handled = false;
        if (mTool != null) {
            if (mEdgeEffectListener != null) {
                handled = mEdgeEffectListener.onPreDrawEdgeEffects(canvas, width, verticalOffset);
            }
            if (!handled) {
                handled = mTool.onDrawEdgeEffects(canvas, width, verticalOffset);
            }
        }

        return handled;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onReleaseEdgeEffects()}.
     */
    @Override
    public void onReleaseEdgeEffects() {
        if (mTool != null) {
            if (mEdgeEffectListener != null) {
                mEdgeEffectListener.onPreReleaseEdgeEffects();
            }
            mTool.onReleaseEdgeEffects();
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onPullEdgeEffects(int, float)}.
     */
    @Override
    public void onPullEdgeEffects(int which_edge, float delta_distance) {
        if (mTool != null) {
            if (mEdgeEffectListener != null) {
                mEdgeEffectListener.onPrePullEdgeEffects(which_edge, delta_distance);
            }
            mTool.onPullEdgeEffects(which_edge, delta_distance);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onDoubleTapZoomAnimationBegin()}.
     */
    @Override
    public void onDoubleTapZoomAnimationBegin() {
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.updateState(AnnotIndicatorManger.STATE_IS_ZOOMING);
            mAnnotIndicatorManger.reset(true);
        }

        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onDoubleTapZoomAnimationBegin();
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onDoubleTapZoomAnimationEnd()}.
     */
    @Override
    public void onDoubleTapZoomAnimationEnd() {
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.updateState(AnnotIndicatorManger.STATE_IS_NORMAL);
        }

        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onDoubleTapZoomAnimationEnd();
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onRenderingFinished()}.
     */
    @Override
    public void onRenderingFinished() {
        if (mPdfViewCtrl.isAnnotationLayerEnabled()) {
            // if annotation layer is enabled
            // we do not need to wait for rendering
            return;
        }
        if (mOldTools != null) {
            for (Tool tool : mOldTools) {
                tool.onRenderingFinished();
            }
            mOldTools.clear();
        }

        if (mTool != null &&
                (mTool.getToolMode() == ToolMode.ANNOT_EDIT ||
                        mTool.getToolMode() == ToolMode.ANNOT_EDIT_LINE ||
                        mTool.getToolMode() == ToolMode.ANNOT_EDIT_ADVANCED_SHAPE ||
                        mTool.getToolMode() == ToolMode.TEXT_CREATE ||
                        mTool.getToolMode() == ToolMode.CALLOUT_CREATE)) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onRenderingFinished();
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#isCreatingAnnotation()}.
     */
    @Override
    public boolean isCreatingAnnotation() {
        return mTool != null && mTool.isCreatingAnnotation();
    }

    @Override
    public void onDestroy() {
        destroy();
    }

    @Override
    public void onResume() {
        mPaused = false;
    }

    @Override
    public void onPause() {
        mPaused = true;
    }

    /**
     * Indicates lifecycle onPause
     */
    public boolean isPaused() {
        return mPaused;
    }

    @Override
    public void onAnnotPainterUpdated(final int page, final long which, final CurvePainter painter) {
        FragmentActivity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mTool != null) {
                    mTool.onAnnotPainterUpdated(page, which, painter);
                }
            }
        });
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ActionCompletedListener#onActionCompleted(Action)}.
     */
    @Override
    public void onActionCompleted(Action action) {
        boolean hasChange = false;
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            hasChange = mPdfViewCtrl.getDoc().hasChangesSinceSnapshot();
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
                if (hasChange) {
                    raiseAnnotationActionEvent();
                }
            }
        }
    }

    /**
     * Gets the current {@link com.pdftron.pdf.tools.ToolManager.Tool} instance.
     */
    public Tool getTool() {
        return mTool;
    }

    /**
     * Sets the current {@link com.pdftron.pdf.tools.ToolManager.Tool} instance.
     * <p>
     * <p>
     * There are two ways to set the current tool. One is via {@link #createTool(ToolModeBase, com.pdftron.pdf.tools.ToolManager.Tool)},
     * which is used during events. The other way is through this method, which
     * allows for setting the tool without any events.
     */
    public void setTool(Tool t) {
        mTool = t;
        sendToolSetEvent(mTool);
    }

    /**
     * @return The PDFViewCtrl
     */
    public PDFViewCtrl getPDFViewCtrl() {
        return mPdfViewCtrl;
    }

    /**
     * Gets the undo redo manager
     *
     * @return undo redo manager
     */
    @NonNull
    public UndoRedoManager getUndoRedoManger() {
        return mUndoRedoManger;
    }

    /**
     * Enables annotation manager for annotation syncing
     *
     * @param userId the unique identifier of the current user
     */
    public void enableAnnotManager(String userId) {
        enableAnnotManager(userId, null);
    }

    /**
     * Enables annotation manager for annotation syncing
     *
     * @param userId   the unique identifier of the current user
     * @param listener the {@link AnnotManager.AnnotationSyncingListener}
     */
    public void enableAnnotManager(String userId, AnnotManager.AnnotationSyncingListener listener) {
        enableAnnotManager(userId, null, listener);
    }

    /**
     * Enables annotation manager for annotation syncing
     *
     * @param userId   the unique identifier of the current user
     * @param userName the name of current user
     * @param listener the {@link AnnotManager.AnnotationSyncingListener}
     */
    public void enableAnnotManager(String userId, String userName, AnnotManager.AnnotationSyncingListener listener) {
        enableAnnotManager(userId, userName, PDFViewCtrl.AnnotationManagerMode.ADMIN_UNDO_OWN, AnnotManager.EditPermissionMode.EDIT_OWN, listener);
    }

    /**
     * Enables annotation manager for annotation syncing
     *
     * @param userId   the unique identifier of the current user
     * @param userName the name of the current user
     * @param undoMode one of {@link com.pdftron.pdf.PDFViewCtrl.AnnotationManagerMode#ADMIN_UNDO_OTHERS}
     *                 {@link com.pdftron.pdf.PDFViewCtrl.AnnotationManagerMode#ADMIN_UNDO_OWN}
     * @param editMode one of {@link com.pdftron.pdf.tools.AnnotManager.EditPermissionMode#EDIT_OTHERS}
     *                 {@link com.pdftron.pdf.tools.AnnotManager.EditPermissionMode#EDIT_OWN}
     * @param listener the {@link AnnotManager.AnnotationSyncingListener}
     */
    public void enableAnnotManager(String userId, String userName,
            @NonNull PDFViewCtrl.AnnotationManagerMode undoMode,
            @NonNull AnnotManager.EditPermissionMode editMode,
            AnnotManager.AnnotationSyncingListener listener) {
        if (null == userId) {
            mAnnotManager = null;
            return;
        }
        try {
            mAnnotManager = new AnnotManager(this, userId, userName,
                    null, undoMode, editMode, listener);
        } catch (Exception e) {
            e.printStackTrace();
            mAnnotManager = null;
        }
    }

    /**
     * @return The annotation manager
     */
    @Nullable
    public AnnotManager getAnnotManager() {
        return mAnnotManager;
    }

    /**
     * @return The redaction manager
     */
    @NonNull
    public RedactionManager getRedactionManager() {
        if (null == mRedactionManager) {
            mRedactionManager = new RedactionManager(mPdfViewCtrl);
        }
        return mRedactionManager;
    }

    @NonNull
    public SoundManager getSoundManager() {
        if (null == mSoundManager) {
            mSoundManager = new SoundManager();
        }
        return mSoundManager;
    }

    /**
     * Adds the {@link com.pdftron.pdf.tools.ToolManager.FullSaveListener}.
     *
     * @param listener the listener
     */
    public void addFullSaveListener(FullSaveListener listener) {
        if (mFullSaveListeners == null) {
            mFullSaveListeners = new ArrayList<>();
        }
        if (!mFullSaveListeners.contains(listener)) {
            mFullSaveListeners.add(listener);
        }
    }

    /**
     * Removes the {@link com.pdftron.pdf.tools.ToolManager.FullSaveListener}.
     *
     * @param listener the listener
     */
    public void removeFullSaveListener(FullSaveListener listener) {
        if (mFullSaveListeners != null) {
            mFullSaveListeners.remove(listener);
        }
    }

    /**
     * Adds the {@link com.pdftron.pdf.tools.ToolManager.ActionGoBackListener}.
     *
     * @param listener the listener
     */
    public void addActionGoBackListener(ActionGoBackListener listener) {
        if (mActionGoBackListeners == null) {
            mActionGoBackListeners = new ArrayList<>();
        }
        if (!mActionGoBackListeners.contains(listener)) {
            mActionGoBackListeners.add(listener);
        }
    }

    /**
     * Removes the {@link com.pdftron.pdf.tools.ToolManager.ActionGoBackListener}.
     *
     * @param listener the listener
     */
    public void removeActionGoBackListener(ActionGoBackListener listener) {
        if (mActionGoBackListeners != null) {
            mActionGoBackListeners.remove(listener);
        }
    }

    /**
     * Adds the {@link com.pdftron.pdf.tools.ToolManager.ToolChangedListener}.
     *
     * @param listener the listener
     */
    public void addToolChangedListener(ToolChangedListener listener) {
        if (mToolChangedListeners == null) {
            mToolChangedListeners = new ArrayList<>();
        }
        if (!mToolChangedListeners.contains(listener)) {
            mToolChangedListeners.add(listener);
        }
    }

    /**
     * Removes the {@link com.pdftron.pdf.tools.ToolManager.ToolChangedListener}.
     *
     * @param listener the listener
     */
    public void removeToolChangedListener(ToolChangedListener listener) {
        if (mToolChangedListeners != null) {
            mToolChangedListeners.remove(listener);
        }
    }

    /**
     * Adds the {@link com.pdftron.pdf.tools.ToolManager.ToolSetListener}.
     *
     * @param listener the listener
     */
    public void addToolSetListener(ToolSetListener listener) {
        if (mToolSetListeners == null) {
            mToolSetListeners = new ArrayList<>();
        }
        if (!mToolSetListeners.contains(listener)) {
            mToolSetListeners.add(listener);
        }
    }

    /**
     * Removes the {@link com.pdftron.pdf.tools.ToolManager.ToolSetListener}.
     *
     * @param listener the listener
     */
    public void removeToolSetListener(ToolSetListener listener) {
        if (mToolSetListeners != null) {
            mToolSetListeners.remove(listener);
        }
    }

    /**
     * Adds the {@link com.pdftron.pdf.tools.ToolManager.ToolChangedListener} called when tool created.
     *
     * @param listener the listener
     */
    public void addToolCreatedListener(ToolChangedListener listener) {
        if (mToolCreatedListeners == null) {
            mToolCreatedListeners = new ArrayList<>();
        }
        if (!mToolCreatedListeners.contains(listener)) {
            mToolCreatedListeners.add(listener);
        }
    }

    /**
     * Removes the {@link com.pdftron.pdf.tools.ToolManager.ToolChangedListener} called when tool created.
     *
     * @param listener the listener
     */
    public void removeToolCreatedListener(ToolChangedListener listener) {
        if (mToolCreatedListeners != null) {
            mToolCreatedListeners.remove(listener);
        }
    }

    /**
     * Adds the {@link com.pdftron.pdf.tools.ToolManager.OnLayoutListener}.
     *
     * @param listener the listener
     */
    public void addOnLayoutListener(OnLayoutListener listener) {
        if (mOnLayoutListeners == null) {
            mOnLayoutListeners = new CopyOnWriteArray<>();
        }

        mOnLayoutListeners.add(listener);
    }

    /**
     * Removes the {@link com.pdftron.pdf.tools.ToolManager.OnLayoutListener}.
     *
     * @param listener the listener
     */
    public void removeOnLayoutListener(OnLayoutListener listener) {
        if (mOnLayoutListeners != null) {
            mOnLayoutListeners.remove(listener);
        }
    }

    /**
     * Sets the {@link com.pdftron.pdf.tools.ToolManager.PreToolManagerListener}.
     *
     * @param listener the listener
     */
    public void setPreToolManagerListener(PreToolManagerListener listener) {
        mPreToolManagerListener = listener;
    }

    /**
     * Sets the {@link PreFlingListener}.
     *
     * @param listener the listener
     */
    public void setPreFlingListener(PreFlingListener listener) {
        mPreFlingListener = listener;
    }

    /**
     * Sets the {@link com.pdftron.pdf.tools.ToolManager.QuickMenuListener}.
     *
     * @param listener the listener
     */
    public void setQuickMenuListener(QuickMenuListener listener) {
        mQuickMenuListener = listener;
    }

    /**
     * Adds the {@link com.pdftron.pdf.tools.ToolManager.AnnotationModificationListener}.
     *
     * @param listener the listener
     */
    public void addAnnotationModificationListener(AnnotationModificationListener listener) {
        if (mAnnotationModificationListeners == null) {
            mAnnotationModificationListeners = new ArrayList<>();
        }
        if (!mAnnotationModificationListeners.contains(listener)) {
            mAnnotationModificationListeners.add(listener);
        }
    }

    /**
     * Removes the {@link com.pdftron.pdf.tools.ToolManager.AnnotationModificationListener}.
     *
     * @param listener the listener
     */
    public void removeAnnotationModificationListener(AnnotationModificationListener listener) {
        if (mAnnotationModificationListeners != null) {
            mAnnotationModificationListeners.remove(listener);
        }
    }

    /**
     * Adds the {@link com.pdftron.pdf.tools.ToolManager.PdfDocModificationListener}.
     *
     * @param listener the listener
     */
    public void addPdfDocModificationListener(PdfDocModificationListener listener) {
        if (mPdfDocModificationListeners == null) {
            mPdfDocModificationListeners = new ArrayList<>();
        }
        if (!mPdfDocModificationListeners.contains(listener)) {
            mPdfDocModificationListeners.add(listener);
        }
    }

    /**
     * Removes the {@link com.pdftron.pdf.tools.ToolManager.PdfDocModificationListener}.
     *
     * @param listener the listener
     */
    public void removePdfDocModificationListener(PdfDocModificationListener listener) {
        if (mPdfDocModificationListeners != null) {
            mPdfDocModificationListeners.remove(listener);
        }
    }

    /**
     * Adds the {@link com.pdftron.pdf.tools.ToolManager.PdfTextModificationListener}.
     *
     * @param listener the listener
     */
    public void addPdfTextModificationListener(PdfTextModificationListener listener) {
        if (mPdfTextModificationListeners == null) {
            mPdfTextModificationListeners = new ArrayList<>();
        }
        if (!mPdfTextModificationListeners.contains(listener)) {
            mPdfTextModificationListeners.add(listener);
        }
    }

    /**
     * Removes the {@link com.pdftron.pdf.tools.ToolManager.PdfTextModificationListener}.
     *
     * @param listener the listener
     */
    public void removePdfTextModificationListener(PdfTextModificationListener listener) {
        if (mPdfTextModificationListeners != null) {
            mPdfTextModificationListeners.remove(listener);
        }
    }

    /**
     * Adds the {@link com.pdftron.pdf.tools.ToolManager.PdfOutlineModificationListener}.
     *
     * @param listener the listener
     */
    public void addPdfOutlineModificationListener(PdfOutlineModificationListener listener) {
        if (mPdfOutlineModificationListeners == null) {
            mPdfOutlineModificationListeners = new ArrayList<>();
        }
        if (!mPdfOutlineModificationListeners.contains(listener)) {
            mPdfOutlineModificationListeners.add(listener);
        }
    }

    /**
     * Removes the {@link com.pdftron.pdf.tools.ToolManager.PdfOutlineModificationListener}.
     *
     * @param listener the listener
     */
    public void removePdfOutlineModificationListener(PdfOutlineModificationListener listener) {
        if (mPdfOutlineModificationListeners != null) {
            mPdfOutlineModificationListeners.remove(listener);
        }
    }

    /**
     * Adds the {@link com.pdftron.pdf.tools.ToolManager.AnnotationsSelectionListener}.
     *
     * @param listener the listener
     */
    public void addAnnotationsSelectionListener(AnnotationsSelectionListener listener) {
        if (mAnnotationsSelectionListeners == null) {
            mAnnotationsSelectionListeners = new ArrayList<>();
        }
        if (!mAnnotationsSelectionListeners.contains(listener)) {
            mAnnotationsSelectionListeners.add(listener);
        }
    }

    /**
     * Removes the {@link com.pdftron.pdf.tools.ToolManager.AnnotationsSelectionListener}.
     *
     * @param listener the listener
     */
    public void removeAnnotationsSelectionListener(AnnotationsSelectionListener listener) {
        if (mAnnotationsSelectionListeners != null) {
            mAnnotationsSelectionListeners.remove(listener);
        }
    }

    /**
     * Sets the {@link com.pdftron.pdf.tools.ToolManager.BasicAnnotationListener}.
     *
     * @param listener the listener
     */
    public void setBasicAnnotationListener(BasicAnnotationListener listener) {
        mBasicAnnotationListener = listener;
    }

    /**
     * Adds the {@link DialogListener}
     *
     * @param listener the lsitener
     */
    public void addDialogListener(@NonNull DialogListener listener) {
        if (mDialogListeners == null) {
            mDialogListeners = new ArrayList<>();
        }
        if (!mDialogListeners.contains(listener)) {
            mDialogListeners.add(listener);
        }
    }

    /**
     * Sets the {@link com.pdftron.pdf.tools.ToolManager.AdvancedAnnotationListener}.
     *
     * @param listener the listener
     */
    public void setAdvancedAnnotationListener(AdvancedAnnotationListener listener) {
        mAdvancedAnnotationListener = listener;
    }

    /**
     * Sets the {@link FileAttachmentAnnotationListener}.
     *
     * @param listener the listener
     */
    public void setFileAttachmentAnnotationListener(FileAttachmentAnnotationListener listener) {
        mFileAttachmentAnnotationListener = listener;
    }

    /**
     * @hide
     */
    public void setSpecialAnnotationListener(SpecialAnnotationListener listener) {
        mSpecialAnnotationListener = listener;
    }

    /**
     * Sets the {@link com.pdftron.pdf.tools.ToolManager.QuickMenuListener}.
     *
     * @param menuItem the menu item. See: {@link QuickMenuItem}
     */
    public boolean onQuickMenuClicked(QuickMenuItem menuItem) {

        if (mQuickMenuListener != null && mQuickMenuListener.onQuickMenuClicked(menuItem)) {
            return true;
        }

        boolean handled = false;
        if (mTool != null && mTool instanceof com.pdftron.pdf.tools.Tool) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                handled = ((com.pdftron.pdf.tools.Tool) mTool).onQuickMenuClicked(menuItem);
                next_tm = mTool.getNextToolMode();

                if (!handled && prev_tm != next_tm) {
                    setTool(createTool(next_tm, mTool));
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }

        return handled;
    }

    /**
     * a callback to be invoked when quick menu is about to show
     */
    public boolean onShowQuickMenu(QuickMenu quickMenu, Annot annot) {
        return mQuickMenuListener != null && mQuickMenuListener.onShowQuickMenu(quickMenu, annot);
    }

    public void onQuickMenuLocationRequested() {
        if (mTool instanceof AnnotEdit) {
            ((AnnotEdit) mTool).updateRotateView();
        }
    }

    /**
     * a callback to be invoked when quick menu is shown
     */
    public void onQuickMenuShown() {
        if (mQuickMenuListener != null) {
            mQuickMenuListener.onQuickMenuShown();
        }
    }

    /**
     * a callback to be invoked when quick menu is dismissed
     */
    public void onQuickMenuDismissed() {
        if (mQuickMenuListener != null) {
            mQuickMenuListener.onQuickMenuDismissed();
        }
    }

    /**
     * Handle annotation
     *
     * @param annot    annotation
     * @param extra    @return true then intercept the subclass function, false otherwise
     * @param toolMode tool mode
     */
    public boolean raiseInterceptAnnotationHandlingEvent(@Nullable Annot annot, Bundle extra, ToolMode toolMode) {
        return mBasicAnnotationListener != null && mBasicAnnotationListener.onInterceptAnnotationHandling(annot, extra, toolMode);
    }

    /**
     * Call this function when a dialog is about to show up.
     *
     * @deprecated see {@link #raiseInterceptDialogFragmentEvent(DialogFragment)}
     */
    @Deprecated
    public boolean raiseInterceptDialogEvent(AlertDialog dialog) {
        return mBasicAnnotationListener != null && mBasicAnnotationListener.onInterceptDialog(dialog);
    }

    /**
     * Call this function when a dialog fragment is about to show up.
     */
    public boolean raiseInterceptDialogFragmentEvent(DialogFragment dialogFragment) {
        boolean intercepted = false;
        if (mDialogListeners != null) {
            for (DialogListener listener : mDialogListeners) {
                if (listener.onInterceptDialog(dialogFragment)) {
                    intercepted = true;
                }
            }
        }
        return intercepted;
    }

    /**
     * Call this function when annotations have been added to the document.
     *
     * @param annots map of annotations added
     */
    public void raiseAnnotationsAddedEvent(Map<Annot, Integer> annots) {
        mLastAnnotAction = AnnotAction.ADD;
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.reset(true);
        }

        if (mAnnotationModificationListeners != null) {
            for (AnnotationModificationListener listener : mAnnotationModificationListeners) {
                listener.onAnnotationsAdded(annots);
            }
        }
    }

    /**
     * Call this function before annotations in the document are modified.
     *
     * @param annots map of annotations about to be modified
     */
    public void raiseAnnotationsPreModifyEvent(Map<Annot, Integer> annots) {
        if (mAnnotationModificationListeners != null) {
            for (AnnotationModificationListener listener : mAnnotationModificationListeners) {
                listener.onAnnotationsPreModify(annots);
            }
        }
    }

    /**
     * Call this function when annotations in the document have been modified.
     *
     * @param annots map of annotations modified
     */
    public void raiseAnnotationsModifiedEvent(Map<Annot, Integer> annots, Bundle bundle) {
        mLastAnnotAction = AnnotAction.MODIFY;
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.reset(true);
        }

        if (mAnnotationModificationListeners != null) {
            for (AnnotationModificationListener listener : mAnnotationModificationListeners) {
                listener.onAnnotationsModified(annots, bundle);
            }
        }
    }

    /**
     * Call this function before annotations are removed from the document.
     *
     * @param annots map of annotations about to be removed
     */
    public void raiseAnnotationsPreRemoveEvent(Map<Annot, Integer> annots) {
        if (mAnnotationModificationListeners != null) {
            for (AnnotationModificationListener listener : mAnnotationModificationListeners) {
                listener.onAnnotationsPreRemove(annots);
            }
        }
    }

    /**
     * Call this function when annotations have been removed from the document.
     *
     * @param annots map of annotations removed
     */
    public void raiseAnnotationsRemovedEvent(Map<Annot, Integer> annots) {
        raiseAnnotationsRemovedEvent(annots, null);
    }

    /**
     * Call this function when annotations have been removed from the document.
     *
     * @param annots map of annotations removed
     * @param bundle The Bundle containing additional metadata for the event
     */
    public void raiseAnnotationsRemovedEvent(Map<Annot, Integer> annots, @Nullable Bundle bundle) {
        String flattenedKey = com.pdftron.pdf.tools.Tool.FLATTENED;
        if (bundle != null && bundle.containsKey(flattenedKey) && bundle.getBoolean(flattenedKey)) {
            mLastAnnotAction = AnnotAction.FLATTEN;
        } else {
            mLastAnnotAction = AnnotAction.REMOVE;
        }
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.reset(true);
        }

        if (mAnnotationModificationListeners != null) {
            for (AnnotationModificationListener listener : mAnnotationModificationListeners) {
                listener.onAnnotationsRemoved(annots);
            }
        }
    }

    /**
     * Call this function when all annotations in the specified page have been removed from the document.
     *
     * @param pageNum The page number where the annotations are on
     */
    public void raiseAnnotationsRemovedEvent(int pageNum) {
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.reset(true);
        }

        if (mAnnotationModificationListeners != null) {
            for (AnnotationModificationListener listener : mAnnotationModificationListeners) {
                listener.onAnnotationsRemovedOnPage(pageNum);
            }
        }
    }

    /**
     * Lets various tools raise the annotation could not be
     * add event from a unified location.
     */
    public void annotationCouldNotBeAdded(String errorMessage) {
        if (mAnnotationModificationListeners != null) {
            for (AnnotationModificationListener listener : mAnnotationModificationListeners) {
                listener.annotationsCouldNotBeAdded(errorMessage);
            }
        }
    }

    /**
     * Call this function when document outline has been modified..
     */
    public void raisePdfOutlineModified() {
        if (mPdfOutlineModificationListeners != null) {
            for (PdfOutlineModificationListener listener : mPdfOutlineModificationListeners) {
                listener.onOutlineChanged();
            }
        }
    }

    /**
     * Call this function when document text content has been modified..
     */
    public void raisePdfTextModified() {
        if (mPdfTextModificationListeners != null) {
            for (PdfTextModificationListener listener : mPdfTextModificationListeners) {
                listener.onPdfTextChanged();
            }
        }
    }

    /**
     * Call this function when document bookmark has been modified.
     *
     * @param bookmarkItems the new bookmarks after modification
     */
    public void raiseBookmarkModified(@NonNull List<UserBookmarkItem> bookmarkItems) {
        if (mPdfDocModificationListeners != null) {
            for (PdfDocModificationListener listener : mPdfDocModificationListeners) {
                listener.onBookmarkModified(bookmarkItems);
            }
        }
    }

    /**
     * Call this function when pages of the document have been cropped.
     */
    public void raisePagesCropped() {
        if (mPdfDocModificationListeners != null) {
            for (PdfDocModificationListener listener : mPdfDocModificationListeners) {
                listener.onPagesCropped();
            }
        }
    }

    /**
     * Call this function when new pages have been added to the document.
     */
    public void raisePagesAdded(List<Integer> pageList) {
        if (mPdfDocModificationListeners != null) {
            for (PdfDocModificationListener listener : mPdfDocModificationListeners) {
                listener.onPagesAdded(pageList);
            }
        }
    }

    /**
     * Call this function when pages have been deleted from the document.
     */
    public void raisePagesDeleted(List<Integer> pageList) {
        if (mPdfDocModificationListeners != null) {
            for (PdfDocModificationListener listener : mPdfDocModificationListeners) {
                listener.onPagesDeleted(pageList);
            }
        }
    }

    /**
     * Call this function when pages in the document have been rotated.
     */
    public void raisePagesRotated(List<Integer> pageList) {
        if (mPdfDocModificationListeners != null) {
            for (PdfDocModificationListener listener : mPdfDocModificationListeners) {
                listener.onPagesRotated(pageList);
            }
        }
    }

    /**
     * Call this function when a page in the document have been moved to a new position.
     */
    public void raisePageMoved(int from, int to) {
        if (mPdfDocModificationListeners != null) {
            for (PdfDocModificationListener listener : mPdfDocModificationListeners) {
                listener.onPageMoved(from, to);
            }
        }
    }

    /**
     * Call this function when all annotations in the document have been removed.
     */
    public void raiseAllAnnotationsRemovedEvent() {
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.reset(true);
        }

        if (mPdfDocModificationListeners != null) {
            for (PdfDocModificationListener listener : mPdfDocModificationListeners) {
                listener.onAllAnnotationsRemoved();
            }
        }
    }

    /**
     * Call this function when an action has taken place that changes the document.
     */
    public void raiseAnnotationActionEvent() {
        if (mPdfDocModificationListeners != null) {
            for (PdfDocModificationListener listener : mPdfDocModificationListeners) {
                listener.onAnnotationAction();
            }
        }
    }

    /**
     * Call this function when the page labels have changed in this document.
     */
    public void raisePageLabelChangedEvent() {
        if (mPdfDocModificationListeners != null) {
            for (PdfDocModificationListener listener : mPdfDocModificationListeners) {
                listener.onPageLabelsChanged();
            }
        }
    }

    /**
     * Lets various tools raise the annotations selection changed event.
     *
     * @param annots the selected annotations
     */
    public void raiseAnnotationsSelectionChangedEvent(HashMap<Annot, Integer> annots) {
        if (mAnnotationsSelectionListeners != null) {
            for (AnnotationsSelectionListener listener : mAnnotationsSelectionListeners) {
                listener.onAnnotationsSelectionChanged(annots);
            }
        }
    }

    /**
     * Lets various tools raise the file attachment selected event.
     *
     * @param fileAttachment the selected file attachment
     */
    public void onFileAttachmentSelected(FileAttachment fileAttachment) {
        if (mAdvancedAnnotationListener != null) {
            mAdvancedAnnotationListener.fileAttachmentSelected(fileAttachment);
        }
    }

    /**
     * Raise event for file attachment annotation
     */
    public void onSaveFileAttachmentSelected(FileAttachment fileAttachment, Intent intent) {
        if (mFileAttachmentAnnotationListener != null) {
            mFileAttachmentAnnotationListener.onSaveFileAttachmentSelected(fileAttachment, intent);
        }
    }

    /**
     * Lets various tools raise the onFileCreated event.
     *
     * @param fileLocation file location
     * @param action       Annotation action associated with the raised event
     */
    public void onFileCreated(String fileLocation, AdvancedAnnotationListener.AnnotAction action) {
        if (mAdvancedAnnotationListener != null) {
            mAdvancedAnnotationListener.fileCreated(fileLocation, action);
        }
    }

    /**
     * Lets various tools raise the freehand stylus used for the first time event.
     */
    public void onFreehandStylusUsedFirstTime() {
        if (mAdvancedAnnotationListener != null) {
            mAdvancedAnnotationListener.freehandStylusUsedFirstTime();
        }
    }

    /**
     * Pass the image stamper selected event.
     *
     * @param targetPoint target location to add the image stamp
     */
    public void onImageStamperSelected(PointF targetPoint) {
        if (mAdvancedAnnotationListener != null) {
            mAdvancedAnnotationListener.imageStamperSelected(targetPoint);
        }
    }

    /**
     * Pass the image signature selected event.
     *
     * @param targetPoint target page location to add the image signature
     * @param targetPage  target page number to add the image signature
     * @param widget      target form widget to add the image signature (only one of target point or widget will be valid)
     */
    public void onImageSignatureSelected(PointF targetPoint, int targetPage, Long widget) {
        if (mAdvancedAnnotationListener != null) {
            mAdvancedAnnotationListener.imageSignatureSelected(targetPoint, targetPage, widget);
        }
    }

    /**
     * Pass the file attachment selected event.
     *
     * @param targetPoint target location to add the image stamp
     */
    public void onAttachFileSelected(PointF targetPoint) {
        if (mAdvancedAnnotationListener != null) {
            mAdvancedAnnotationListener.attachFileSelected(targetPoint);
        }
    }

    /**
     * Pass inline free text editing started event.
     */
    public void onInlineFreeTextEditingStarted() {
        if (mAdvancedAnnotationListener != null) {
            mAdvancedAnnotationListener.freeTextInlineEditingStarted();
        }
    }

    /**
     * Opens a newly created file.
     *
     * @param filePath local file path of the document
     * @return whether a new file was opened.
     */
    public boolean onNewFileCreated(String filePath) {
        return onNewFileCreated(filePath, -1);
    }

    /**
     * Opens a newly created file.
     *
     * @param filePath   local file path of the document
     * @param pageNumber the initial page number to scroll to when the document is opened
     * @return whether a new file was opened
     */
    public boolean onNewFileCreated(String filePath, int pageNumber) {
        if (mAdvancedAnnotationListener != null) {
            return mAdvancedAnnotationListener.newFileSelectedFromTool(filePath, pageNumber);
        }
        return false;
    }

    /**
     * Goes back to previous page when action is GoBack
     */
    public void onGoBackSelected() {
        if (mActionGoBackListeners != null) {
            for (ActionGoBackListener listener : mActionGoBackListeners) {
                listener.onActionGoBack();
            }
        }
    }

    public void raiseFullSaveRequired() {
        if (mFullSaveListeners != null) {
            for (FullSaveListener listener : mFullSaveListeners) {
                listener.onFullSaveRequired();
            }
        }
    }

    /**
     * @param text     the text
     * @param anchor   the anchor
     * @param isDefine true if in define mode, false if in translation mode
     * @hide Pass the define and translate event.
     */
    public void defineTranslateSelected(String text, RectF anchor, Boolean isDefine) {
        if (mSpecialAnnotationListener != null) {
            mSpecialAnnotationListener.defineTranslateSelected(text, anchor, isDefine);
        }
    }

    /**
     * Pass the ink edit selected event.
     *
     * @param inkAnnot the ink annotation to be modified
     */
    public void onInkEditSelected(Annot inkAnnot, int pageNum) {
        if (mAnnotationToolbarListener != null) {
            mAnnotationToolbarListener.inkEditSelected(inkAnnot, pageNum);
        }
    }

    /**
     * Called when the annotation toolbar should open for a tool
     *
     * @param mode the tool mode
     */
    @Deprecated
    public void onOpenAnnotationToolbar(ToolMode mode) {
        if (mAnnotationToolbarListener != null) {
            mAnnotationToolbarListener.openAnnotationToolbar(mode);
        }
    }

    /**
     * Called when the edit toolbar should open for a tool
     *
     * @param mode the tool mode
     */
    public void onOpenEditToolbar(ToolMode mode) {
        if (mAnnotationToolbarListener != null) {
            mAnnotationToolbarListener.openEditToolbar(mode);
        }
    }

    /**
     * Gets the annotation toolbar height
     *
     * @return annotation toolbar height in pixel, -1 if annotation toolbar not visible
     */
    @Deprecated
    public int getAnnotationToolbarHeight() {
        if (mAnnotationToolbarListener != null) {
            return mAnnotationToolbarListener.annotationToolbarHeight();
        }
        return -1;
    }

    /**
     * Gets the toolbar height
     *
     * @return toolbar height in pixel, -1 if annotation toolbar not visible
     */
    public int getToolbarHeight() {
        if (mAnnotationToolbarListener != null) {
            return mAnnotationToolbarListener.toolbarHeight();
        }
        return -1;
    }

    /**
     * Sets the {@link com.pdftron.pdf.tools.ToolManager.AnnotationToolbarListener}.
     *
     * @param annotationToolbarListener the listener
     */
    public void setAnnotationToolbarListener(AnnotationToolbarListener annotationToolbarListener) {
        mAnnotationToolbarListener = annotationToolbarListener;
    }

    /**
     * Sets the {@link com.pdftron.pdf.tools.ToolManager.OnGenericMotionEventListener}.
     *
     * @param onGenericMotionEventListener the listener
     */
    public void setOnGenericMotionEventListener(OnGenericMotionEventListener onGenericMotionEventListener) {
        mOnGenericMotionEventListener = onGenericMotionEventListener;
    }

    public void setExternalAnnotationManagerListener(ExternalAnnotationManagerListener externalAnnotationManagerListener) {
        mExternalAnnotationManagerListener = externalAnnotationManagerListener;
    }

    public OnAnnotStyleChangedListener getOnStyleChangedListener() {
        return mOnAnnotStyleChangedListener;
    }

    public void setOnStyleChangedListener(OnAnnotStyleChangedListener onAnnotStyleChangedListener) {
        mOnAnnotStyleChangedListener = onAnnotStyleChangedListener;
    }

    public void onAnnotStyleColorChange(ArrayList<AnnotStyle> styles) {
        if (mOnAnnotStyleChangedListener != null) {
            mOnAnnotStyleChangedListener.onAnnotStyleColorChange(styles);
        }
    }

    public void onAnnotStyleDismiss(AnnotStyleDialogFragment annotStyleDialog) {
        if (mOnAnnotStyleChangedListener != null) {
            mOnAnnotStyleChangedListener.OnAnnotStyleDismiss(annotStyleDialog);
        }
    }

    public void setStampDialogListener(StampDialogListener listener) {
        mStampDialogListener = listener;
    }

    public StampDialogListener getStampDialogListener() {
        return mStampDialogListener;
    }

    public void setPresetsListener(PresetsListener listener) {
        mPresetsListener = listener;
    }

    public PresetsListener getPresetsListener() {
        return mPresetsListener;
    }

    public void setSnackbarListener(SnackbarListener listener) {
        mSnackbarListener = listener;
    }

    public SnackbarListener getSnackbarListener() {
        return mSnackbarListener;
    }

    public void setEdgeEffectListener(EdgeEffectListener edgeEffectListner) {
        mEdgeEffectListener = edgeEffectListner;
    }

    @Nullable
    public EdgeEffectListener getEdgeEffectListener() {
        return mEdgeEffectListener;
    }

    /**
     * Indicates whether to use/show the built-in page number indicator.
     *
     * @param visible true to show the built-in page number indicator, false
     *                otherwise.
     */
    public void setBuiltInPageNumberIndicatorVisible(boolean visible) {
        mPageNumberIndicatorVisible = visible;
    }

    /**
     * Indicates whether to use/show the built-in page number indicator.
     *
     * @return true to show the built-in page number indicator, false
     * otherwise.
     */
    public boolean isBuiltInPageNumberIndicatorVisible() {
        return mPageNumberIndicatorVisible;
    }

    /**
     * Indicates whether the file associated with PDFViewCtrl is read-only.
     */
    public void setReadOnly(boolean readOnly) {
        if (mSkipReadOnlyCheck) {
            return;
        }
        mReadOnly = readOnly;
        ToolMode[] editableToolModes = new ToolManager.ToolMode[]{
                ToolMode.ANNOT_EDIT_RECT_GROUP,
                ToolMode.FORM_FILL,
                ToolMode.RECT_LINK,
                ToolMode.INK_CREATE,
                ToolMode.FREE_HIGHLIGHTER,
                ToolMode.LINE_CREATE,
                ToolMode.ARROW_CREATE,
                ToolMode.RULER_CREATE,
                ToolMode.PERIMETER_MEASURE_CREATE,
                ToolMode.AREA_MEASURE_CREATE,
                ToolMode.RECT_AREA_MEASURE_CREATE,
                ToolMode.POLYLINE_CREATE,
                ToolMode.RECT_CREATE,
                ToolMode.OVAL_CREATE,
                ToolMode.SOUND_CREATE,
                ToolMode.FILE_ATTACHMENT_CREATE,
                ToolMode.POLYGON_CREATE,
                ToolMode.CLOUD_CREATE,
                ToolMode.TEXT_CREATE,
                ToolMode.CALLOUT_CREATE,
                ToolMode.TEXT_ANNOT_CREATE,
                ToolMode.TEXT_LINK_CREATE,
                ToolMode.FORM_CHECKBOX_CREATE,
                ToolMode.FORM_COMBO_BOX_CREATE,
                ToolMode.FORM_LIST_BOX_CREATE,
                ToolMode.FORM_SIGNATURE_CREATE,
                ToolMode.FORM_TEXT_FIELD_CREATE,
                ToolMode.FORM_RADIO_GROUP_CREATE,
                ToolMode.SIGNATURE,
                ToolMode.STAMPER,
                ToolMode.RUBBER_STAMPER,
                ToolMode.INK_ERASER,
                ToolMode.TEXT_HIGHLIGHT,
                ToolMode.TEXT_SQUIGGLY,
                ToolMode.TEXT_STRIKEOUT,
                ToolMode.TEXT_UNDERLINE,
                ToolMode.TEXT_REDACTION,
                ToolMode.RECT_REDACTION,
                ToolMode.FREE_TEXT_SPACING_CREATE,
                ToolMode.FREE_TEXT_DATE_CREATE,
                ToolMode.SMART_PEN_INK,
                ToolMode.SMART_PEN_TEXT_MARKUP,
                ToolMode.COUNT_MEASUREMENT
        };
        if (readOnly) {
            // first let's store the previously disabled modes so we can recover later
            if (mDisabledToolModes != null && mDisabledToolModes.size() > 0) {
                if (mDisabledToolModesSave == null) {
                    mDisabledToolModesSave = new HashSet<>();
                }
                mDisabledToolModesSave.clear();
                mDisabledToolModesSave.addAll(mDisabledToolModes);
            }

            disableToolMode(editableToolModes);
        } else {
            enableToolMode(editableToolModes);

            // now recover from the previous disabled modes
            if (mDisabledToolModesSave != null && mDisabledToolModesSave.size() > 0) {
                disableToolMode(mDisabledToolModesSave.toArray(new ToolManager.ToolMode[mDisabledToolModesSave.size()]));
                mDisabledToolModesSave.clear();
            }
        }
    }

    /**
     * Gets whether the file associated with PDFViewCtrl is read-only.
     */
    public boolean isReadOnly() {
        return mReadOnly;
    }

    /**
     * Sets whether to check annotation author permission
     *
     * @param enable if true, annotation created by user A cannot be modified by user B,
     *               else anyone can modify any annotation
     */
    public void setAnnotPermissionCheckEnabled(boolean enable) {
        mCanCheckAnnotPermission = enable;
    }

    /**
     * Gets whether annotation author permission is enabled
     *
     * @return true if enabled, false otherwise
     */
    public boolean isAnnotPermissionCheckEnabled() {
        return mCanCheckAnnotPermission;
    }

    /**
     * Sets the user ID used for checking whether an annotation is created by current user.
     *
     * @param authorId author identification
     */
    public void setAuthorId(String authorId) {
        mAuthorId = authorId;
    }

    /**
     * Gets the user ID used for checking whether an annotation is created by current user.
     *
     * @return author identification
     */
    public String getAuthorId() {
        return mAuthorId;
    }

    /**
     * Sets the user ID used for checking whether an annotation is created by current user.
     *
     * @param authorName author name
     */
    public void setAuthorName(String authorName) {
        mAuthorName = authorName;
    }

    /**
     * Gets the user ID used for checking whether an annotation is created by current user.
     *
     * @return author name
     */
    public String getAuthorName() {
        return mAuthorName;
    }

    /**
     * Sets the selected annotation
     *
     * @param annot   the annotation
     * @param pageNum the page number where the annotation is on
     */
    public void setSelectedAnnot(Annot annot, int pageNum) {
        try {
            mSelectedAnnotId = annot == null ? null : (annot.getUniqueID() == null ? null : annot.getUniqueID().getAsPDFText());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        mSelectedAnnotPageNum = pageNum;
        if (mBasicAnnotationListener != null) {
            if (null == annot) {
                mBasicAnnotationListener.onAnnotationUnselected();
            } else {
                mBasicAnnotationListener.onAnnotationSelected(annot, pageNum);
            }
        }
        if (null == annot) {
            raiseAnnotationsSelectionChangedEvent(new HashMap<Annot, Integer>());
        } else {
            HashMap<Annot, Integer> map = new HashMap<>(1);
            map.put(annot, pageNum);
            raiseAnnotationsSelectionChangedEvent(map);
        }
    }

    /**
     * Gets the identification of the selected annotation
     *
     * @return the annotation identification
     */
    public String getSelectedAnnotId() {
        return mSelectedAnnotId;
    }

    /**
     * Gets the page number of selected annotation
     *
     * @return the page number
     */
    public int getSelectedAnnotPageNum() {
        return mSelectedAnnotPageNum;
    }

    /**
     * Deselects all annotations
     */
    public void deselectAll() {
        mTool.onClose();
        // keep current annotation mode
        setTool(createTool(getTool().getToolMode(), null));
        mPdfViewCtrl.invalidate();
    }

    /**
     * Selects the annotation.
     *
     * @param annotId The annotation ID
     * @param pageNum The page number where the annotation is on
     */
    public void selectAnnot(String annotId, int pageNum) {
        Annot annot = ViewerUtils.getAnnotById(mPdfViewCtrl, annotId, pageNum);
        if (null != annot) {
            selectAnnot(annot, pageNum);
        }
    }

    /**
     * Selects an annotation
     *
     * @param annot   the annotation
     * @param pageNum the page number where the annotation is on
     */
    public void selectAnnot(
            @Nullable Annot annot,
            int pageNum) {

        try {
            if (annot == null || !annot.isValid()) {
                return;
            }

            ToolMode mode = ToolMode.ANNOT_EDIT;
            int annotType = annot.getType();
            if (annotType == Annot.e_Line) {
                mode = ToolMode.ANNOT_EDIT_LINE;
            } else if (annotType == Annot.e_Polyline
                    || (annotType == Annot.e_Polygon && !AnnotUtils.isRectAreaMeasure(annot))
                    || AnnotUtils.isCallout(annot)) {
                mode = ToolMode.ANNOT_EDIT_ADVANCED_SHAPE;
            } else if (annotType == Annot.e_Highlight
                    || annotType == Annot.e_StrikeOut
                    || annotType == Annot.e_Underline
                    || annotType == Annot.e_Squiggly) {
                mode = ToolMode.ANNOT_EDIT_TEXT_MARKUP;
            }

            Pair<ToolMode, ArrayList<Annot>> pair = com.pdftron.pdf.tools.Tool.canSelectGroupAnnot(mPdfViewCtrl, annot, pageNum);
            if (pair != null && pair.first != null) {
                mode = pair.first;
            }

            if (mTool.getToolMode() != mode) {
                setTool(createTool(mode, mTool));
            }
            if (mTool instanceof AnnotEdit ||
                    mTool instanceof AnnotEditTextMarkup) {
                ((com.pdftron.pdf.tools.Tool) mTool).selectAnnot(annot, pageNum);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * Re-selects the last selected annotation
     */
    public void reselectAnnot() {
        if (null != mSelectedAnnotId && mSelectedAnnotPageNum > 0 && mPdfViewCtrl.getDoc() != null) {
            Annot annot = ViewerUtils.getAnnotById(mPdfViewCtrl, mSelectedAnnotId, mSelectedAnnotPageNum);
            if (annot != null) {
                selectAnnot(annot, mSelectedAnnotPageNum);
            }
        }
    }

    /**
     * Sets whether the quick menu is just closed.
     *
     * @param closed True if the quick menu is just closed
     */
    public void setQuickMenuJustClosed(boolean closed) {
        mQuickMenuJustClosed = closed;
    }

    /**
     * @return True if the quick menu is just closed
     */
    public boolean isQuickMenuJustClosed() {
        boolean result = mQuickMenuJustClosed;
        mQuickMenuJustClosed = false;
        return result;
    }

    /**
     * Sets whether to show author dialog the first time when user annotates.
     */
    public void setShowAuthorDialog(boolean show) {
        mShowAuthorDialog = show;
    }

    /**
     * Gets whether to show author dialog the first time when user annotates.
     */
    public boolean isShowAuthorDialog() {
        return mShowAuthorDialog;
    }

    /**
     * Sets whether the TextMarkup annotations are compatible with Adobe
     * (Adobe's quads don't follow the specification, but they don't handle quads that do).
     */
    public void setTextMarkupAdobeHack(boolean enable) {
        mTextMarkupAdobeHack = enable;
    }

    /**
     * Gets whether the TextMarkup annotations are compatible with Adobe
     * (Adobe's quads don't follow the specification, but they don't handle quads that do).
     */
    public boolean isTextMarkupAdobeHack() {
        return mTextMarkupAdobeHack;
    }

    /**
     * Sets whether to copy annotated text to note
     *
     * @param enable enable copy annotated text to note
     */
    public void setCopyAnnotatedTextToNoteEnabled(boolean enable) {
        mCopyAnnotatedTextToNote = enable;
    }

    /**
     * Gets whether to copy annotated text to note
     *
     * @return true if enabled, false otherwise
     */
    public boolean isCopyAnnotatedTextToNoteEnabled() {
        return mCopyAnnotatedTextToNote;
    }

    /**
     * Sets whether to use stylus to draw without entering ink tool
     *
     * @param stylusAsPen enable inking with stylus in pan mode
     */
    public void setStylusAsPen(boolean stylusAsPen) {
        mStylusAsPen = stylusAsPen;
    }

    /**
     * Gets whether to use stylus to draw without entering ink tool
     *
     * @return true if enabled, false otherwise
     */
    public boolean isStylusAsPen() {
        return mStylusAsPen;
    }

    /**
     * Sets whether to smooth ink annotation
     *
     * @param enable enable ink smoothing
     */
    public void setInkSmoothingEnabled(boolean enable) {
        mInkSmoothing = enable;
    }

    /**
     * Gets whether to smooth ink annotation
     *
     * @return true if enabled, false otherwise
     */
    public boolean isInkSmoothingEnabled() {
        return mInkSmoothing;
    }

    /**
     * Sets list of free text fonts to have as
     * options in the properties popup.
     * (Sets whiteList fonts among the PDFNet system fonts)
     */
    public void setFreeTextFonts(Set<String> freeTextFonts) {
        ToolStyleConfig.getInstance().setFreeTextFonts(freeTextFonts);
    }

    /**
     * Sets custom font list from Assets for free text tool
     * if sets font list from Assets, then it is not possible to set font list from storage
     * The system fonts list won't load anymore
     *
     * @param fontNameList array of custom font's absolute path from Assets
     */
    public void setFreeTextFontsFromAssets(Set<String> fontNameList) {
        ToolStyleConfig.getInstance().setFreeTextFontsFromAssets(fontNameList);
        if (fontNameList != null && !fontNameList.isEmpty()) {
            mDisposables.add(
                    FontResource.writeFontAssetsToCache(mPdfViewCtrl.getContext(), fontNameList)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<Set<String>>() {
                                @Override
                                public void accept(Set<String> strings) throws Exception {
                                    ToolStyleConfig.getInstance().setFreeTextFontsFromAssets(strings);
                                }
                            }, new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Exception {
                                    getFreeTextFontsFromAssets().clear();
                                }
                            })
            );
        }
    }

    /**
     * Sets custom font list from device storage for free text tool
     * if font list from Assets is not set, then it's possible to set font list from storage
     * The system fonts list won't load anymore
     *
     * @param fontPathList array of custom font's absolute path from device storage
     */
    public void setFreeTextFontsFromStorage(Set<String> fontPathList) {
        ToolStyleConfig.getInstance().setFreeTextFontsFromStorage(fontPathList);
        if (fontPathList != null && !fontPathList.isEmpty()) {
            mDisposables.add(
                    FontResource.writeFontFileToCache(mPdfViewCtrl.getContext(), fontPathList)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<Set<String>>() {
                                @Override
                                public void accept(Set<String> strings) throws Exception {
                                    ToolStyleConfig.getInstance().setFreeTextFontsFromStorage(strings);
                                }
                            }, new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Exception {
                                    getFreeTextFontsFromStorage().clear();
                                }
                            })
            );
        }
    }

    /**
     * Gets the list of free text fonts to have as
     * options in the properties popup
     * (Gets whiteList fonts among the PDFNet system fonts)
     */
    public Set<String> getFreeTextFonts() {
        return ToolStyleConfig.getInstance().getFreeTextFonts();
    }

    /**
     * Gets custom font list from Assets for free text tool
     */
    public Set<String> getFreeTextFontsFromAssets() {
        return ToolStyleConfig.getInstance().getFreeTextFontsFromAssets();
    }

    /**
     * Gets custom font list from Storage for free text tool
     */
    public Set<String> getFreeTextFontsFromStorage() {
        return ToolStyleConfig.getInstance().getFreeTextFontsFromStorage();
    }

    /**
     * Sets whether night mode is enabled
     *
     * @param isNightMode enable night mode for tools
     */
    public void setNightMode(boolean isNightMode) {
        mIsNightMode = isNightMode;
        if (mTool != null) {
            mTool.onNightModeUpdated(isNightMode);
        }
    }

    /**
     * Gets whether night mode is enabled for tools
     *
     * @return true if enabled, false otherwise
     */
    public boolean isNightMode() {
        return mIsNightMode;
    }

    /**
     * Sets whether annotation indicator is showing for
     * annotations with comments
     *
     * @param showAnnotIndicators true if enabled, false otherwise
     */
    public void setShowAnnotIndicators(boolean showAnnotIndicators) {
        mShowAnnotIndicators = showAnnotIndicators;
        if (showAnnotIndicators) {
            mAnnotIndicatorManger = new AnnotIndicatorManger(this);
        } else {
            if (mAnnotIndicatorManger != null) {
                mAnnotIndicatorManger.cleanup();
            }
            mAnnotIndicatorManger = null;
        }
    }

    /**
     * Gets whether annotation indicator is showing for
     * annotations with comments
     *
     * @return true if enabled, false otherwise
     */
    public boolean isShowAnnotIndicators() {
        return mShowAnnotIndicators;
    }

    /**
     * @return the list of old tools
     */
    public ArrayList<Tool> getOldTools() {
        if (mOldTools == null) {
            mOldTools = new ArrayList<>();
        }
        return mOldTools;
    }

    /**
     * Initialize system Text To Speech
     */
    public void initTTS() {
        try {
            // initialize text to speech
            mTTS = new TextToSpeech(mPdfViewCtrl.getContext().getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {

                }
            });
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    /**
     * Gets the Text To Speech object
     *
     * @return TextToSpeech object
     */
    public TextToSpeech getTTS() {
        return mTTS;
    }

    /**
     * Sets whether editing ink annotation should open the annotation toolbar
     *
     * @param editInkAnnots if true, can edit ink annotation via annotation toolbar, false otherwise
     */
    public void setEditInkAnnots(boolean editInkAnnots) {
        mEditInkAnnots = editInkAnnots;
    }

    /**
     * Gets whether editing ink annotation should open the annotation toolbar
     *
     * @return true if can edit ink annotation via annotation toolbar, false otherwise
     */
    public boolean editInkAnnots() {
        return mEditInkAnnots;
    }

    /**
     * Sets whether to enable image stamper tool
     *
     * @param addImageStamperTool if true, image stamper tool will be available
     * @Deprecated Use {@link Tool#disableToolMode(ToolMode[])}
     */
    public void setAddImageStamperTool(boolean addImageStamperTool) {
        mAddImageStamperTool = addImageStamperTool;
    }

    /**
     * Sets whether edit tool will open when tools selected in pan quick menu
     *
     * @param canOpenEditToolbarFromPan if true, click tools from quick menu will
     *                                  open the edit toolbar in pan mode,
     *                                  false otherwise
     */
    public void setCanOpenEditToolbarFromPan(boolean canOpenEditToolbarFromPan) {
        mCanOpenEditToolbarFromPan = canOpenEditToolbarFromPan;
    }

    /**
     * Gets whether edit tool will open when tools selected in pan quick menu
     *
     * @return true if click tools from quick menu will open the edit toolbar in pan mode,
     * false otherwise
     */
    public boolean isOpenEditToolbarFromPan() {
        return mCanOpenEditToolbarFromPan;
    }

    /**
     * Sets whether auto select annotation after annotation is created
     *
     * @param autoSelect if true, after creating annotation, it will auto select it and show quick menu
     */
    public void setAutoSelectAnnotation(boolean autoSelect) {
        mIsAutoSelectAnnotation = autoSelect;
    }

    /**
     * Gets auto select annotation after annotation is created
     *
     * @return true if auto select, false otherwise
     */
    public boolean isAutoSelectAnnotation() {
        return mIsAutoSelectAnnotation;
    }

    /**
     * Sets whether disable showing the long press quick menu
     *
     * @param disabled if true, disable showing the long press quick menu
     */
    public void setDisableQuickMenu(boolean disabled) {
        mDisableQuickMenu = disabled;
    }

    /**
     * Gets whether the long press quick menu is disabled
     *
     * @return true if disabled, false otherwise
     */
    public boolean isQuickMenuDisabled() {
        return mDisableQuickMenu;
    }

    /**
     * Sets whether can double tap to zoom the viewer
     *
     * @param doubleTapToZoom if true, can double tap to zoom, false otherwise
     */
    public void setDoubleTapToZoom(boolean doubleTapToZoom) {
        mDoubleTapToZoom = doubleTapToZoom;
    }

    /**
     * Gets whether can double tap to zoom
     *
     * @return true if can double tap to zoom, false otherwise
     */
    public boolean isDoubleTapToZoom() {
        return mDoubleTapToZoom;
    }

    /**
     * Gets whether can auto resize free text when editing
     *
     * @return true if can auto resize, false otherwise
     */
    public boolean isAutoResizeFreeText() {
        return mAutoResizeFreeText;
    }

    /**
     * Sets whether can auto resize free text when editing
     *
     * @param autoResizeFreeText if true can auto resize, false otherwise
     */
    public void setAutoResizeFreeText(boolean autoResizeFreeText) {
        this.mAutoResizeFreeText = autoResizeFreeText;
    }

    /**
     * Gets whether annotation editing is real time
     *
     * @return true if real time, false otherwise
     */
    public boolean isRealTimeAnnotEdit() {
        return mRealTimeAnnotEdit;
    }

    /**
     * Sets whether annotation editing is real time
     *
     * @param realTimeAnnotEdit true if real time, false otherwise
     */
    public void setRealTimeAnnotEdit(boolean realTimeAnnotEdit) {
        this.mRealTimeAnnotEdit = realTimeAnnotEdit;
    }

    /**
     * Gets whether can edit FreeText on tap
     *
     * @return true if start edit on tap, false otherwise
     */
    public boolean isEditFreeTextOnTap() {
        return mEditFreeTextOnTap;
    }

    /**
     * Sets whether can edit FreeText on tap
     *
     * @param editFreeTextOnTap true if start edit on tap, false otherwise
     */
    public void setEditFreeTextOnTap(boolean editFreeTextOnTap) {
        this.mEditFreeTextOnTap = editFreeTextOnTap;
    }

    /**
     * Gets whether show the FreeText inline toggle button
     *
     * @return true if showing the FreeText inline toggle button, false otherwise
     */
    public boolean isfreeTextInlineToggleEnabled() {
        return freeTextInlineToggleEnabled;
    }

    /**
     * Sets whether to show the FreeText inline toggle button
     *
     * @param freeTextInlineToggleEnabled true if showing the FreeText inline toggle button, false otherwise
     */
    public void freeTextInlineToggleEnabled(boolean freeTextInlineToggleEnabled) {
        this.freeTextInlineToggleEnabled = freeTextInlineToggleEnabled;
    }

    /**
     * Gets whether can delete FreeText if empty content was entered
     *
     * @return true if delete FreeText if empty content was entered, false otherwise
     */
    public boolean isDeleteEmptyFreeText() {
        return mDeleteEmptyFreeText;
    }

    /**
     * Sets whether can delete FreeText if empty content was entered
     *
     * @param deleteEmptyFreeText true if delete FreeText if empty content was entered, false otherwise
     */
    public void setDeleteEmptyFreeText(boolean deleteEmptyFreeText) {
        mDeleteEmptyFreeText = deleteEmptyFreeText;
    }

    /**
     * Sets whether can show saved signature in signature dialog
     *
     * @param showSavedSignature true if show saved signatures, false otherwise
     */
    public void setShowSavedSignatures(boolean showSavedSignature) {
        this.mShowSavedSignature = showSavedSignature;
    }

    /**
     * Gets whether can show saved signature in signature dialog
     *
     * @return true if show saved signatures, false otherwise
     */
    public boolean isShowSavedSignature() {
        return mShowSavedSignature;
    }

    /**
     * Sets whether "Store signature" setting in the CreateSignatureFragment dialog should
     * be enabled or disabled by default.
     * <p>
     * If {@link #isPersistStoreSignatureSetting()} is true, then this default value is used the
     * very first time the user creates a new signature. When the user creates the next
     * signatures, this value is ignored and instead will read their last used "Store signature" setting.
     * <p>
     * If {@link #isPersistStoreSignatureSetting()} is false, then this default
     * value is used every time the user creates a new signature
     * <p>
     * Default true.
     *
     * @param defaultStoreNewSignature true if show saved signatures, false otherwise
     */
    public void setDefaultStoreNewSignature(boolean defaultStoreNewSignature) {
        this.mDefaultStoreNewSignature = defaultStoreNewSignature;
    }

    /**
     * Gets whether "Store signature" setting in the CreateSignatureFragment dialog should
     * be enabled or disabled by default.
     * <p>
     * See {@link #setDefaultStoreNewSignature(boolean)} for details.
     *
     * @return true if "Store signature" is enabled by default, false otherwise
     */
    public boolean getDefaultStoreNewSignature() {
        return mDefaultStoreNewSignature;
    }

    /**
     * Sets whether to use the user's last set "Store signature" setting in the
     * CreateSignatureFragment dialog.
     * <p>
     * Default true.
     *
     * @param persistStoreSignatureSetting true if "Store signature" setting should persist the next
     *                                     time a user creates signature, false otherwise
     */
    public void setPersistStoreSignatureSetting(boolean persistStoreSignatureSetting) {
        this.mPersistStoreSignatureSetting = persistStoreSignatureSetting;
    }

    /**
     * Gets whether to use the user's last set "Store signature" setting in the
     * CreateSignatureFragment dialog.
     *
     * @return true if "Store signature" setting should persist the next time a user creates
     * a signature, false otherwise
     */
    public boolean isPersistStoreSignatureSetting() {
        return mPersistStoreSignatureSetting;
    }

    /**
     * Sets whether to show color presets in the signature dialog
     *
     * @param showSignaturePresets true if color presets should be shown in the signature dialog, false otherwise
     */
    public void setShowSignaturePresets(boolean showSignaturePresets) {
        this.mShowSignaturePresets = showSignaturePresets;
    }

    /**
     * Gets whether signature presets are shown in the signature dialog
     *
     * @return true if color presets are shown in the signature dialog, false otherwise
     */
    public boolean isShowSignaturePresets() {
        return mShowSignaturePresets;
    }

    /**
     * Sets whether can show pick signature from image in signature dialog
     *
     * @param showSignatureFromImage true if show pick from image button, false otherwise
     */
    public void setShowSignatureFromImage(boolean showSignatureFromImage) {
        this.mShowSignatureFromImage = showSignatureFromImage;
    }

    /**
     * Sets whether can show typed siganture button in signature dialog
     *
     * @param showTypedSignature true if show typed signature button button, false otherwise
     */
    public void setShowTypedSignature(boolean showTypedSignature) {
        this.mShowTypedSignature = showTypedSignature;
    }

    /**
     * Gets whether can show pick signature from image in signature dialog
     *
     * @return true if show image picking icon, false otherwise
     */
    public boolean isShowSignatureFromImage() {
        return mShowSignatureFromImage;
    }

    /**
     * Gets whether can show typed signature button in signature dialog
     *
     * @return true if show typed signature button, false otherwise
     */
    public boolean isShowTypedSignature() {
        return mShowTypedSignature;
    }

    /**
     * Sets whether can use digital signature for signature widget
     *
     * @param usingDigitalSignature true if can use digital signature
     */
    public void setUsingDigitalSignature(boolean usingDigitalSignature) {
        this.mUsingDigitalSignature = usingDigitalSignature;
    }

    /**
     * Gets whether can use digital signature for signature widget
     *
     * @return true if can use digital signature
     */
    public boolean isUsingDigitalSignature() {
        return this.mUsingDigitalSignature;
    }

    /**
     * Sets digital signature keystore file path
     */
    public void setDigitalSignatureKeystorePath(String digitalSignatureKeystore) {
        this.mDigitalSignatureKeystorePath = digitalSignatureKeystore;
    }

    /**
     * Gets digital signature keystore file path
     *
     * @return the digital signature keystore file path
     */
    public String getDigitalSignatureKeystore() {
        return mDigitalSignatureKeystorePath;
    }

    /**
     * Sets digital signature keystore password
     */
    public void setDigitalSignatureKeystorePassword(String digitalSignatureKeystore) {
        this.mDigitalSignatureKeystorePassword = digitalSignatureKeystore;
    }

    /**
     * Gets digital signature keystore password
     *
     * @return the digital signature keystore password
     */
    public String getDigitalSignatureKeystorePassword() {
        return mDigitalSignatureKeystorePassword;
    }

    /**
     * Sets whether to show rotate handle when annotation selected
     *
     * @param showRotateHandle true if rotation handle should be shown. Default to true.
     */
    public void setShowRotateHandle(boolean showRotateHandle) {
        this.mShowRotateHandle = showRotateHandle;
    }

    /**
     * Gets whether to show rotate handle when annotation selected
     *
     * @return true if rotation handle should be shown.
     */
    public boolean isShowRotateHandle() {
        return mShowRotateHandle;
    }

    /**
     * Sets how wide the range would be for auto smoothing vertical and horizontal strokes, in dp independent of zoom.
     *
     * @param autoSmoothingRange indicating how wide is the range to auto smooth vertical and horizontal strokes, in dp.
     */
    public void setFreeHighlighterAutoSmoothingRange(float autoSmoothingRange) {
        this.mFreeHighlighterAutoSmoothingRange = autoSmoothingRange;
    }

    /**
     * Gets how wide the range would be for auto smoothing vertical and horizontal strokes, in pixel.
     */
    public float getFreeHighlighterAutoSmoothingRange() {
        return this.mFreeHighlighterAutoSmoothingRange;
    }

    /**
     * Sets whether multi-stroke is enabled for ink tool
     *
     * @param multiStrokeEnabled true if enabled. Default to true.
     */
    public void setInkMultiStrokeEnabled(boolean multiStrokeEnabled) {
        mInkMultiStrokeEnabled = multiStrokeEnabled;
    }

    /**
     * Sets whether free hand annotations are saved on a timer basis. This flag is ignored if
     * isInkMultiStrokeEnabled() returns false.
     *
     * @param freehandTimerEnabled true if enabled. Default to true.
     */
    public void setFreeHandTimerEnabled(boolean freehandTimerEnabled) {
        mFreehandTimerEnabled = freehandTimerEnabled;
    }

    /**
     * Sets whether loupe is disabled for tools
     * setLoupeEnabled(false) will disable the loupe on text markup annotation
     *
     * @param loupeEnabled true if enabled. Default to true.
     */
    public void setLoupeEnabled(boolean loupeEnabled) {
        mLoupeEnabled = loupeEnabled;
    }

    /**
     * Gets whether multi-stroke is enabled for ink tool
     *
     * @return true if enabled
     */
    public boolean isInkMultiStrokeEnabled() {
        return mInkMultiStrokeEnabled;
    }

    /**
     * Gets whether free hand annotations are saved on a timer basis. This flag is ignored if
     * isInkMultiStrokeEnabled() returns false.
     *
     * @return true if enabled
     */
    public boolean isFreehandTimerEnabled() {
        return mFreehandTimerEnabled;
    }

    /**
     * Gets whether loupe is enabled or not on text markup annotation
     *
     * @return true if enabled
     */
    public boolean isLoupeEnabled() {
        return mLoupeEnabled;
    }

    /**
     * Sets whether moving annotations between pages is enabled
     *
     * @param moveAnnotBetweenPages true if enabled. Default to false.
     */
    public void setMoveAnnotationBetweenPages(boolean moveAnnotBetweenPages) {
        mMoveAnnotBetweenPages = moveAnnotBetweenPages;
    }

    /**
     * Gets whether moving annotations between pages is enabled
     *
     * @return true if enabled
     */
    public boolean isMoveAnnotationBetweenPages() {
        return mMoveAnnotBetweenPages;
    }

    /**
     * Sets whether to allow tap to create annotations on another type of annotation, for example for free text and sticky note.
     *
     * @param restrictedTapAnnotCreation true if creating tap type annotations is not allowed on other type of annotations. Default to false.
     */
    public void setRestrictedTapAnnotCreation(boolean restrictedTapAnnotCreation) {
        this.mRestrictedTapAnnotCreation = restrictedTapAnnotCreation;
    }

    /**
     * Gets whether to allow tap to create annotations on another type of annotation, for example for free text and sticky note.
     *
     * @return true if creating tap type annotations is not allowed on other type of annotations.
     */
    public boolean isRestrictedTapAnnotCreation() {
        return this.mRestrictedTapAnnotCreation;
    }

    /**
     * Enable separate rendering layer for annotations.
     * Once enabled, can no longer disable in the same session.
     */
    public void enableAnnotationLayer() {
        mPdfViewCtrl.enableAnnotationLayer();
    }

    /**
     * Skips the next tap event.
     */
    public void skipNextTapEvent() {
        this.mSkipNextTouchEvent = true;
        this.mSkipNextTapEvent = true;
    }

    /**
     * @return True if next tap event is skipped.
     */
    public boolean isSkipNextTapEvent() {
        return this.mSkipNextTouchEvent || this.mSkipNextTapEvent;
    }

    /**
     * Resets skipping the next tap event.
     */
    public void resetSkipNextTapEvent() {
        this.mSkipNextTouchEvent = false;
        this.mSkipNextTapEvent = false;
    }

    /**
     * @return The cache file name
     */
    public String getCacheFileName() {
        return mCacheFileName;
    }

    /**
     * Sets the cache file name
     *
     * @param tag The tag which will be used to generate cache file name
     */
    public void setCacheFileName(String tag) {
        mCacheFileName = String.valueOf(tag.hashCode());
    }

    /**
     * @return The free text cache file name
     */
    public String getFreeTextCacheFileName() {
        return "freetext_" + mCacheFileName + ".srl";
    }

    /**
     * @return True if can resume PDF Doc without reloading
     */
    public boolean canResumePdfDocWithoutReloading() {
        return mCanResumePdfDocWithoutReloading;
    }

    /**
     * Sets if can resume PDF Doc without reloading.
     *
     * @param canResumePdfDocWithoutReloading True if can resume PDF Doc without reloading
     */
    public void setCanResumePdfDocWithoutReloading(boolean canResumePdfDocWithoutReloading) {
        this.mCanResumePdfDocWithoutReloading = canResumePdfDocWithoutReloading;
    }

    /**
     * Disables annotation editing by type.
     *
     * @param annotTypes annot types to be disabled
     */
    public void disableAnnotEditing(Integer[] annotTypes) {
        ToolConfig.getInstance().disableAnnotEditing(annotTypes);
    }

    /**
     * Enables annotation editing by type.
     *
     * @param annotTypes annot types to be enabled
     */
    public void enableAnnotEditing(Integer[] annotTypes) {
        ToolConfig.getInstance().enableAnnotEditing(annotTypes);
    }

    /**
     * Sets whether to use pressure sensitivity on styluses for signatures.
     *
     * @param usePressureSensitiveSignatures true if using pressure sensitivity for signatures.
     */
    public void setUsePressureSensitiveSignatures(boolean usePressureSensitiveSignatures) {
        this.mUsePressureSensitiveSignatures = usePressureSensitiveSignatures;
    }

    /**
     * Gets whether the viewer will use pressure sensitivity for signatures
     *
     * @return true if show image picking icon, false otherwise
     */
    public boolean isUsingPressureSensitiveSignatures() {
        return mUsePressureSensitiveSignatures;
    }

    /**
     * Sets the type of eraser.
     *
     * @param type the type of eraser
     */
    public void setEraserType(Eraser.EraserType type) {
        this.mEraserType = type;
        if (type != null) {
            ToolStyleConfig.getInstance().setDefaultEraserType(type);
        }
    }

    /**
     * Gets the type of eraser.
     *
     * @return the type of eraser
     */
    public Eraser.EraserType getEraserType() {
        return this.mEraserType;
    }

    /**
     * Sets the mode of multi-select tool.
     *
     * @param mode the mode of multi-select tool
     */
    public void setMultiSelectMode(AnnotEditRectGroup.SelectionMode mode) {
        this.mMultiSelectMode = mode;
    }

    /**
     * Gets the mode of multi-select tool.
     *
     * @return the mode of multi-select tool
     */
    public AnnotEditRectGroup.SelectionMode getMultiSelectMode() {
        return this.mMultiSelectMode;
    }

    /**
     * Sets whether to show undo/redo buttons in the toolbar
     *
     * @param showUndoRedo true if showing undo/redo buttons in the toolbar.
     */
    public void setShowUndoRedo(boolean showUndoRedo) {
        this.mShowUndoRedo = showUndoRedo;

        // Call listeners about show undo/redo state
        if (mToolManagerChangedListeners != null) {
            for (ToolManagerChangedListener listener : mToolManagerChangedListeners) {
                listener.onUndoRedoShownChanged(showUndoRedo);
            }
        }
    }

    /**
     * Gets whether the viewer show undo/redo buttons in the toolbar.
     *
     * @return true if showing undo/redo buttons in the toolbar.
     */
    public boolean isShowUndoRedo() {
        return mShowUndoRedo;
    }

    /**
     * Sets the margin between selection box and annotation bounding box
     *
     * @param marginInDp the margin in DP, default to 16dp.
     */
    public void setSelectionBoxMargin(int marginInDp) {
        this.mSelectionBoxMargin = marginInDp;
    }

    /**
     * Gets the margin between selection box and annotation bounding box
     *
     * @return the margin in dp
     */
    public int getSelectionBoxMargin() {
        return this.mSelectionBoxMargin;
    }

    /**
     * Sets the half width for tap to create shapes
     *
     * @param halfWidthInDp the half width in dp, default to 50dp
     */
    public void setTapToCreateShapeHalfWidth(int halfWidthInDp) {
        this.mTapToCreateShapeHalfWidth = halfWidthInDp;
    }

    /**
     * Gets the half width for tap to create shapes
     *
     * @return the half width in dp
     */
    public int getTapToCreateShapeHalfWidth() {
        return this.mTapToCreateShapeHalfWidth;
    }

    /**
     * Checks whether the editing of an annot type is disabled.
     *
     * @param annotType The annot type
     * @return True if editing of the annot type is disabled
     */
    public boolean isAnnotEditingDisabled(int annotType) {
        return ToolConfig.getInstance().isAnnotEditingDisabled(annotType);
    }

    private List<ToolManagerChangedListener> mToolManagerChangedListeners;

    /**
     * Disables tool modes. Pan tool cannot be disabled.
     *
     * @param toolModes tool modes to be disabled
     */
    public void disableToolMode(ToolMode[] toolModes) {
        if (mDisabledToolModes == null) {
            mDisabledToolModes = new HashSet<>();
        }
        Collections.addAll(mDisabledToolModes, toolModes);

        // Call listeners about disabled tool modes
        if (mToolManagerChangedListeners != null) {
            for (ToolManagerChangedListener listener : mToolManagerChangedListeners) {
                listener.onDisabledToolModeChanged(mDisabledToolModes);
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void addToolManagerChangedListener(@NonNull ToolManagerChangedListener listener) {
        if (mToolManagerChangedListeners == null) {
            mToolManagerChangedListeners = new ArrayList<>();
        }
        mToolManagerChangedListeners.add(listener);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void removeToolManagerChangedListener(@NonNull ToolManagerChangedListener listener) {
        if (mToolManagerChangedListeners != null) {
            mToolManagerChangedListeners.remove(listener);
        }
    }

    /**
     * Listener called when the ToolManager has changed.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface ToolManagerChangedListener {
        void onDisabledToolModeChanged(@NonNull Set<ToolMode> disabledToolModes);

        void onUndoRedoShownChanged(@NonNull Boolean isShown);
    }

    /**
     * @return the set of disabled tool modes
     */
    @Nullable
    public Set<ToolMode> getDisabledToolModes() {
        return mDisabledToolModes;
    }

    /**
     * Enables tool modes.
     *
     * @param toolModes tool modes to be enabled
     */
    public void enableToolMode(ToolMode[] toolModes) {
        if (mDisabledToolModes == null) {
            return;
        }
        List<ToolMode> toolModeList = Arrays.asList(toolModes);
        mDisabledToolModes.removeAll(toolModeList);

        // Call listeners about disabled tool modes
        if (mToolManagerChangedListeners != null) {
            for (ToolManagerChangedListener listener : mToolManagerChangedListeners) {
                listener.onDisabledToolModeChanged(mDisabledToolModes);
            }
        }
    }

    /**
     * Checks whether the specified tool mode is disabled.
     *
     * @param toolMode The tool mode
     * @return True if the tool mode is disabled
     */
    public boolean isToolModeDisabled(ToolMode toolMode) {
        return mDisabledToolModes != null && mDisabledToolModes.contains(toolMode);
    }

    /**
     * Sets an array that determines which tool icon should show when space is limited
     * (portrait mode on small devices when collapsed)
     *
     * @param toolModes the tool modes in the order of importance
     */
    public void setAnnotToolbarPrecedence(ToolMode[] toolModes) {
        if (mAnnotToolbarPrecedence == null) {
            mAnnotToolbarPrecedence = new ArrayList<>();
        }
        mAnnotToolbarPrecedence.clear();
        Collections.addAll(mAnnotToolbarPrecedence, toolModes);
    }

    /**
     * Gets the precedence of tool mode in Annotation Toolbar
     *
     * @return the tool modes in the order of importance
     */
    public ArrayList<ToolMode> getAnnotToolbarPrecedence() {
        return mAnnotToolbarPrecedence;
    }

    /**
     * Gets whether precedence is set in Annotation Toolbar
     *
     * @return true if there is precedence, false otherwise
     */
    public boolean hasAnnotToolbarPrecedence() {
        return mAnnotToolbarPrecedence != null && mAnnotToolbarPrecedence.size() > 0;
    }

    /**
     * Sets whether snapping is enabled for measurement tools
     *
     * @param enabled true if enabled, false otherwise
     */
    public void setSnappingEnabledForMeasurementTools(boolean enabled) {
        mSnappingEnabled = enabled;
        if (getTool() != null && getTool() instanceof com.pdftron.pdf.tools.Tool) {
            ((com.pdftron.pdf.tools.Tool) getTool()).setSnappingEnabled(enabled);
        }
    }

    /**
     * Set the point snapping mode for the SnapToNearestInDoc method.
     * By default SnapToNearestInDoc will snap to line endpoints, midpoints, and intersections.
     *
     * @param modeFlags a bitwise combination of the members of `GeometryCollection.SnappingMode`
     */
    public void setSnappingMode(int modeFlags) {
        mSnappingModes = modeFlags;
        if (mSnapUtils != null) {
            mSnapUtils.setSnappingMode(mSnappingModes);
        }
    }

    /**
     * Gets snapping modes for measurement tools
     *
     * @return a bitwise combination of the members of `GeometryCollection.SnappingMode`
     */
    public int getSnappingModes() {
        return mSnappingModes;
    }

    /**
     * Gets whether snapping is enabled for measurement tools
     *
     * @return true if enabled, false otherwise
     */
    public boolean isSnappingEnabledForMeasurementTools() {
        return mSnappingEnabled;
    }

    /**
     * Sets whether rich content is enabled for FreeText tool
     *
     * @param enabled true if enabled, false otherwise
     *                Note: this will only take effect on Lollipop and up.
     */
    public void setRichContentEnabledForFreeText(boolean enabled) {
        if (!mShowRichContentOption) {
            enabled = false;
        }
        mRichContentEnabled = enabled;
        if (getTool() != null && getTool() instanceof FreeTextCreate) {
            ((FreeTextCreate) getTool()).setRichContentEnabled(enabled);
        }
    }

    /**
     * Gets whether rich content is enabled for FreeText tool
     *
     * @return true if enabled, false otherwise
     */
    public boolean isRichContentEnabledForFreeText() {
        return mRichContentEnabled;
    }

    /**
     * Sets whether to show rich content switch for FreeText tool
     * Note: this will only take effect on Lollipop and up.
     *
     * @param showRichContentOption true if show switch, false otherwise
     */
    public void setShowRichContentOption(boolean showRichContentOption) {
        if (Utils.isLollipop()) {
            this.mShowRichContentOption = showRichContentOption;
        } else {
            this.mShowRichContentOption = false;
        }
    }

    /**
     * Sets whether to show option to edit original PDF content.
     *
     * @param pdfContentEditingEnabled true if show option to edit original PDF content. Default to false.
     */
    public void setPdfContentEditingEnabled(boolean pdfContentEditingEnabled) {
        this.mPdfContentEditingEnabled = pdfContentEditingEnabled;
    }

    /**
     * Gets whether to show option to edit original PDF content.
     *
     * @return true if show option to edit original PDF content, false otherwise.
     */
    public boolean isPdfContentEditingEnabled() {
        return this.mPdfContentEditingEnabled;
    }

    /**
     * Gets whether to show rich content switch for FreeText tool
     * Note: this will only take effect on Lollipop and up.
     *
     * @return true if show switch, false otherwise
     */
    public boolean isShowRichContentOption() {
        return this.mShowRichContentOption;
    }

    /**
     * Sets the colors used for signing. Minimum one color and maximum three colors, any additional colors are ignored.
     * <p>
     * When set, users will not be able to change these colors through the annotation style dialog.
     *
     * @param signatureColors color resources used for creating signatures.
     */
    public void setSignatureColors(@ColorInt int... signatureColors) {
        if (mSignatureColors == null) {
            mSignatureColors = new int[signatureColors.length];
        }
        System.arraycopy(signatureColors, 0, this.mSignatureColors, 0, signatureColors.length);
    }

    /**
     * Returns the colors used for signing.
     *
     * @return array of color resources used for signing.
     */
    public int[] getSignatureColors() {
        return mSignatureColors;
    }

    /**
     * @return The generated key
     */
    public String generateKey() {
        if (mExternalAnnotationManagerListener != null) {
            return mExternalAnnotationManagerListener.onGenerateKey();
        }
        return UUID.randomUUID().toString();
    }

    /**
     * Cleans up resources.
     */
    public void destroy() {
        mDisposables.clear();
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.cleanup();
        }
        if (mSelectionLoupeBitmap != null) {
            mSelectionLoupeBitmap.recycle();
            mSelectionLoupeBitmap = null;
        }
        if (mSelectionLoupeBitmapRound != null) {
            mSelectionLoupeBitmapRound.recycle();
            mSelectionLoupeBitmapRound = null;
        }
        // clean all listeners
        if (mToolChangedListeners != null) {
            mToolChangedListeners.clear();
        }
        if (mToolCreatedListeners != null) {
            mToolCreatedListeners.clear();
        }
        if (mToolSetListeners != null) {
            mToolSetListeners.clear();
        }
        if (mOnLayoutListeners != null) {
            mOnLayoutListeners.clear();
        }
        if (mAnnotationModificationListeners != null) {
            mAnnotationModificationListeners.clear();
        }
        if (mPdfDocModificationListeners != null) {
            mPdfDocModificationListeners.clear();
        }
        if (mAnnotationsSelectionListeners != null) {
            mAnnotationsSelectionListeners.clear();
        }
        if (mRedactionManager != null) {
            mRedactionManager.destroy();
        }
        if (mUndoRedoManger != null) {
            mUndoRedoManger.destroy();
        }

        mCurrentActivity = null;
    }

    /**
     * Copy on write array. This array is not thread safe, and only one loop can
     * iterate over this array at any given time. This class avoids allocations
     * until a concurrent modification happens.
     * <p>
     * Usage:
     * <p>
     * CopyOnWriteArray.Access<MyData> access = array.start();
     * try {
     * for (int i = 0; i < access.size(); i++) {
     * MyData d = access.get(i);
     * }
     * } finally {
     * access.end();
     * }
     */
    // This class is taken from ViewTreeObserver class
    private static class CopyOnWriteArray<T> {
        private ArrayList<T> mData = new ArrayList<T>();
        private ArrayList<T> mDataCopy;

        private final Access<T> mAccess = new Access<T>();

        private boolean mStart;

        static class Access<T> {
            private ArrayList<T> mData;
            private int mSize;

            T get(int index) {
                return mData.get(index);
            }

            int size() {
                return mSize;
            }
        }

        CopyOnWriteArray() {
        }

        private ArrayList<T> getArray() {
            if (mStart) {
                if (mDataCopy == null) mDataCopy = new ArrayList<T>(mData);
                return mDataCopy;
            }
            return mData;
        }

        Access<T> start() {
            if (mStart) throw new IllegalStateException("Iteration already started");
            mStart = true;
            mDataCopy = null;
            mAccess.mData = mData;
            mAccess.mSize = mData.size();
            return mAccess;
        }

        void end() {
            if (!mStart) throw new IllegalStateException("Iteration not started");
            mStart = false;
            if (mDataCopy != null) {
                mData = mDataCopy;
                mAccess.mData.clear();
                mAccess.mSize = 0;
            }
            mDataCopy = null;
        }

        int size() {
            return getArray().size();
        }

        void add(T item) {
            getArray().add(item);
        }

        void addAll(CopyOnWriteArray<T> array) {
            getArray().addAll(array.mData);
        }

        void remove(T item) {
            getArray().remove(item);
        }

        void clear() {
            getArray().clear();
        }
    }

    /**
     * Sets whether show pop up dialog when sticky note is added/ selected/ etc.
     *
     * @param show if true, show sticky note pop up when sticky note is created/ prepare to modify
     */
    public void setStickyNoteShowPopup(boolean show) {
        mStickyNoteShowPopup = show;
    }

    /**
     * Gets whether shows stick note pop up dialog
     *
     * @return true if shows, false otherwise
     */
    public boolean getStickyNoteShowPopup() {
        return mStickyNoteShowPopup;
    }

    /**
     * Add a custom tool to tool class map
     *
     * @param tool customized tool
     */
    public void addCustomizedTool(com.pdftron.pdf.tools.Tool tool) {
        if (null == mCustomizedToolClassMap) {
            mCustomizedToolClassMap = new HashMap<>();
        }
        mCustomizedToolClassMap.put(tool.getToolMode(), tool.getClass());
    }

    /**
     * Add a custom tool to tool class map
     *
     * @param toolClassMap customized tool mode and class map
     */
    public void addCustomizedTool(HashMap<ToolModeBase, Class<? extends com.pdftron.pdf.tools.Tool>> toolClassMap) {
        if (null == mCustomizedToolClassMap) {
            mCustomizedToolClassMap = new HashMap<>();
        }
        mCustomizedToolClassMap.putAll(toolClassMap);
    }

    /**
     * Add a custom tool to tool class map
     *
     * @param tool   customized tool.
     * @param params parameter for instantiate tool
     */
    public void addCustomizedTool(com.pdftron.pdf.tools.Tool tool, Object... params) {
        addCustomizedTool(tool);
        if (null == mCustomizedToolParamMap) {
            mCustomizedToolParamMap = new HashMap<>();
        }
        mCustomizedToolParamMap.put(tool.getToolMode(), params);
    }

    /**
     * Add a custom tool to tool class map
     *
     * @param toolParamMap tool mode and tool initialize parameter map
     */
    public void addCustomizedToolParams(HashMap<ToolModeBase, Object[]> toolParamMap) {
        if (null == mCustomizedToolParamMap) {
            mCustomizedToolParamMap = new HashMap<>();
        }
        mCustomizedToolParamMap.putAll(toolParamMap);
    }

    /**
     * set default tool class
     *
     * @param cLass default tool class
     */
    public void setDefaultToolCLass(Class<? extends Pan> cLass) {
        mDefaultToolClass = cLass;
    }

    /**
     * Show built in page number indicator
     */
    public void showBuiltInPageNumber() {
        if (!mPageNumberIndicatorVisible || (mPageIndicatorPopup != null && mPageIndicatorPopup.isShowing())) {
            return;
        }

        PageIndicatorLayout pageIndicator;
        if (mPageIndicatorPopup != null) {
            pageIndicator = (PageIndicatorLayout) mPageIndicatorPopup.getContentView();
        } else {
            pageIndicator = new PageIndicatorLayout(mPdfViewCtrl.getContext());
            pageIndicator.setPdfViewCtrl(mPdfViewCtrl);
            pageIndicator.setAutoAdjustPosition(false);
            pageIndicator.setVisibility(View.VISIBLE);
            ViewGroup.MarginLayoutParams mlp = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pageIndicator.setLayoutParams(mlp);
            pageIndicator.setOnPdfViewCtrlVisibilityChangeListener(new PageIndicatorLayout.OnPDFViewVisibilityChanged() {
                @Override
                public void onPDFViewVisibilityChanged(int prevVisibility, int currVisibility) {
                    if (currVisibility != View.VISIBLE) {
                        hideBuiltInPageNumber();
                    }
                }
            });

            // initialize page indicator popup
            mPageIndicatorPopup = new PopupWindow(pageIndicator, ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        int[] position = pageIndicator.calculateAutoAdjustPosition();
        mPageIndicatorPopup.showAtLocation(mPdfViewCtrl, Gravity.TOP | Gravity.START, position[0], position[1]);
        pageIndicator.onPageChange(0, mPdfViewCtrl.getCurrentPage(), PDFViewCtrl.PageChangeState.END);
    }

    /**
     * Hide built in page number indicator
     */
    public void hideBuiltInPageNumber() {
        if (mPageNumberIndicatorVisible && mPageIndicatorPopup != null) {
            mPageIndicatorPopup.dismiss();
        }
    }

    /**
     * Add an AnnotStyleProperty used to hide annotation style properties in the annotation style dialog.
     *
     * @param annotStyleProperty the AnnotStyleProperty used to hide annotation style properties
     */
    public void addAnnotStyleProperty(@NonNull AnnotStyleProperty annotStyleProperty) {
        if (annotStyleProperty == null) {
            throw new RuntimeException("AnnotStyleProperty cannot be null");
        }
        mAnnotStyleProperties.put(annotStyleProperty.getAnnotType(), annotStyleProperty);
    }

    /**
     * Returns list of AnnotStyleProperty used to hide elements of the AnnotStyleDialgFragment
     *
     * @return
     */
    @NonNull
    public HashMap<Integer, AnnotStyleProperty> getAnnotStyleProperties() {
        return mAnnotStyleProperties;
    }

    public void setThemeProvider(@NonNull ThemeProvider themeProvider) {
        mThemeProvider = themeProvider;
    }

    @StyleRes
    public int getTheme() {
        return mThemeProvider.getTheme();
    }

    /**
     * For collaboration apps that requires syncing signature widget, set to true.
     */
    public void setSignSignatureFieldsWithStamps(boolean useStamp) {
        mSignSignatureFieldsWithStamps = useStamp;
    }

    /**
     * @hide
     */
    public boolean isSignSignatureFieldsWithStamps() {
        return mSignSignatureFieldsWithStamps;
    }

    /**
     * Whether ToolManager is currently attached with an activity
     */
    public boolean hasCurrentActivity() {
        return getCurrentActivity() != null;
    }

    /**
     * Set the activity to which the ToolManager is currently attached, or {@code null} if not attached.
     */
    public void setCurrentActivity(@Nullable FragmentActivity activity) {
        mCurrentActivity = new WeakReference<>(activity);
    }

    /**
     * Gets the annotation at the (x, y) position expressed in screen coordinates.
     * <p>
     * <b> Note: This method is permission aware.
     *
     * @param x x coordinate of the screen point
     * @param y y coordinate of the screen point
     * @return The annotation found. If no annotation was found, it returns a null pointer.
     */
    public Annot getAnnotationAt(int x, int y) {
        Annot annot = mPdfViewCtrl.getAnnotationAt(x, y);
        if (null != annot && AnnotUtils.hasPermission(mPdfViewCtrl, annot, com.pdftron.pdf.tools.Tool.ANNOT_PERMISSION_INTERACT)) {
            return annot;
        }
        return null;
    }

    /**
     * Sets whether the viewer should skip read only checks on a document. Default false.
     *
     * @param skipReadOnlyCheck True if viewer should skip read only check, false otherwise
     */
    public void setSkipReadOnlyCheck(boolean skipReadOnlyCheck) {
        this.mSkipReadOnlyCheck = skipReadOnlyCheck;
    }

    /**
     * Returns whether the viewer should skip read only checks on a document.
     */
    public boolean skipReadOnlyCheck() {
        return mSkipReadOnlyCheck;
    }

    /**
     * Sets whether to skip tool creation if same tool creation is requested
     *
     * @param skipSameToolCreation true if current tool will be returned
     *                             by {@link #createTool(ToolModeBase, Tool)}
     *                             when new tool mode is the same as current tool mode.
     *                             Default to false.
     */
    public void setSkipSameToolCreation(boolean skipSameToolCreation) {
        this.mSkipSameToolCreation = skipSameToolCreation;
    }

    /**
     * Get the activity to which the ToolManager is currently attached, or {@code null} if not attached.
     * DO NOT HOLD LONG-LIVED REFERENCES TO THE OBJECT RETURNED BY THIS METHOD, AS THIS WILL CAUSE
     * MEMORY LEAKS.
     */
    public @Nullable
    FragmentActivity getCurrentActivity() {
        FragmentActivity activity = null;
        if (mPdfViewCtrl != null && (mPdfViewCtrl.getContext() instanceof FragmentActivity)) {
            activity = (FragmentActivity) mPdfViewCtrl.getContext();
        } else if (mCurrentActivity != null && mCurrentActivity.get() != null) {
            activity = mCurrentActivity.get();
        }
        return activity;
    }

    @Nullable
    SelectionLoupe getSelectionLoupe(int type) {
        if (null == mPdfViewCtrl) {
            return null;
        }
        if (null == mSelectionLoupe) {
            int width = mPdfViewCtrl.getContext().getResources().getDimensionPixelSize(R.dimen.pdftron_magnifier_width);
            int height = mPdfViewCtrl.getContext().getResources().getDimensionPixelSize(R.dimen.pdftron_magnifier_height);
            mSelectionLoupeBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mSelectionLoupeCanvas = new Canvas();
            mSelectionLoupeCanvas.setBitmap(mSelectionLoupeBitmap);
            mSelectionLoupe = new SelectionLoupe(mPdfViewCtrl);
            mSelectionLoupe.setup(mSelectionLoupeBitmap);
        }
        if (null == mSelectionLoupeRound) {
            int loupeSize = (int) Utils.convDp2Pix(mPdfViewCtrl.getContext(), BaseTool.LOUPE_SIZE);
            int loupeRadius = (int) Utils.convDp2Pix(mPdfViewCtrl.getContext(), BaseTool.LOUPE_RADIUS);
            mSelectionLoupeBitmapRound = Bitmap.createBitmap(loupeSize, loupeSize, Bitmap.Config.ARGB_8888);
            mSelectionLoupeCanvasRound = new Canvas();
            mSelectionLoupeCanvasRound.setBitmap(mSelectionLoupeBitmapRound);
            mSelectionLoupeRound = new SelectionLoupe(mPdfViewCtrl, BaseTool.LOUPE_TYPE_MEASURE);
            mSelectionLoupeRound.setup(mSelectionLoupeBitmapRound, loupeRadius);
        }
        if (BaseTool.LOUPE_TYPE_MEASURE == type) {
            return mSelectionLoupeRound;
        }
        return mSelectionLoupe;
    }

    @Nullable
    Bitmap getSelectionLoupeBitmap(int type) {
        if (BaseTool.LOUPE_TYPE_MEASURE == type) {
            return mSelectionLoupeBitmapRound;
        }
        return mSelectionLoupeBitmap;
    }

    @Nullable
    Canvas getSelectionLoupeCanvas(int type) {
        if (BaseTool.LOUPE_TYPE_MEASURE == type) {
            return mSelectionLoupeCanvasRound;
        }
        return mSelectionLoupeCanvas;
    }

    boolean isFontLoaded() {
        return mFontLoaded;
    }

    /**
     * @return the previous annotation action that was applied.
     */
    @Nullable
    public AnnotAction getLastAnnotAction() {
        return mLastAnnotAction;
    }

    @NonNull
    CompositeDisposable getDisposable() {
        return mDisposables;
    }

    /**
     * Returns the class associated with calculating and storing annotation snapping.
     */
    @NonNull
    public AnnotSnappingManager getAnnotSnappingManager() {
        return mAnnotSnappingManager;
    }

    /**
     * Returns the class associated with handling drawing and calculating snapping modes.
     */
    @NonNull
    public SnapUtils getSnapUtils() {
        return mSnapUtils;
    }

    public void setAnnotationPermissionListener(AnnotationPermissionListener annotationPermissionListener) {
        mAnnotationPermissionListener = annotationPermissionListener;
    }

    public AnnotationPermissionListener getAnnotationPermissionListener() {
        return mAnnotationPermissionListener;
    }

    public enum AnnotAction {
        ADD, REMOVE, MODIFY, FLATTEN
    }

    public void setFreeTextToggleButtonColorInt(@ColorInt int color) {
        mFreeTextNoteToggleButtonColorInt = color;
    }

    @ColorInt
    public int getFreeTextToggleButtonColorInt() {
        return mFreeTextNoteToggleButtonColorInt;
    }

    public void setTextFieldCustomAppearanceEnabled(boolean enabled) {
        mTextFieldCustomAppearanceEnabled = enabled;
    }

    public boolean isTextFieldCustomAppearanceEnabled() {
        return mTextFieldCustomAppearanceEnabled;
    }
}
