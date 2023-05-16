package com.pdftron.pdf.tools;

import android.content.SharedPreferences;
import android.graphics.PointF;
import android.view.MotionEvent;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;

@Keep
public abstract class SimpleTapShapeCreate extends SimpleShapeCreate {

    private static final int ICON_SIZE = 25;

    protected int mIconWidth = ICON_SIZE;
    protected int mIconHeight = ICON_SIZE;

    /**
     * Class constructor
     */
    public SimpleTapShapeCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        mNextToolMode = getToolMode();

        mAllowScrollWithTapTool = true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        mAnnotPushedBack = false;
        return super.onDown(e);
    }

    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        super.onMove(e1, e2, x_dist, y_dist);

        // allow scrolling
        return false;
    }

    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {

        // With a fling motion, onUp is called twice. We want
        // to ignore the second call
        if (mAnnotPushedBack) {
            return false;
        }

        // We are scrolling
        if (mAllowTwoFingerScroll) {
            doneTwoFingerScrolling();
            return false;
        }

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();

        // consume quick menu
        if (toolManager.isQuickMenuJustClosed()) {
            return true;
        }

        if (priorEventMode == PDFViewCtrl.PriorEventMode.PAGE_SLIDING) {
            return false;
        }

        // If we are just up from fling or pinch, do not add new note
        if (priorEventMode == PDFViewCtrl.PriorEventMode.FLING ||
                priorEventMode == PDFViewCtrl.PriorEventMode.PINCH) {
            // allow scrolling
            return false;
        }

        boolean shouldCreate = true;
        int x = (int) e.getX();
        int y = (int) e.getY();
        Annot tappedAnnot = didTapOnSameTypeAnnot(e);
        int page = mPdfViewCtrl.getPageNumberFromScreenPt(x, y);
        if (tappedAnnot != null) {
            shouldCreate = false;
            // force ToolManager to select the annotation
            toolManager.selectAnnot(tappedAnnot, page);
        }

        if (shouldCreate && page > 0) {
            mPt2 = new PointF(e.getX(), e.getY());
            addAnnotation();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets the target point.
     *
     * @param point       The target point
     * @param createAnnot True if should create annot
     */
    public void setTargetPoint(PointF point, boolean createAnnot) {
        mPt2.x = point.x;
        mPt2.y = point.y;
        mDownPageNum = mPdfViewCtrl.getPageNumberFromScreenPt(point.x, point.y);

        if (createAnnot) {
            addAnnotation();
        }
    }

    /**
     * The implementation will provide UI for annotation creation
     */
    abstract public void addAnnotation();

    protected boolean createAnnotation(@NonNull PointF targetScreenPoint, int pageNum) {
        return createAnnotation(targetScreenPoint, null, pageNum);
    }

    protected boolean createAnnotation(@Nullable PointF targetScreenPoint, @Nullable PointF targetPagePoint, int pageNum) {
        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        mStrokeColor = settings.getInt(getColorKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultColor(mPdfViewCtrl.getContext(), getCreateAnnotType()));
        mOpacity = settings.getFloat(getOpacityKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultOpacity(mPdfViewCtrl.getContext(), getCreateAnnotType()));

        boolean success = false;
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            com.pdftron.pdf.Rect rect = null;
            if (targetScreenPoint != null) {
                rect = getBBox(targetScreenPoint, pageNum);
            } else if (targetPagePoint != null) {
                rect = getBBoxFromPagePoint(targetPagePoint);
            }
            if (rect != null) {
                Annot annot = createMarkup(mPdfViewCtrl.getDoc(), rect);
                setStyle(annot);
                Page page = mPdfViewCtrl.getDoc().getPage(pageNum);
                if (annot != null && page != null) {
                    annot.refreshAppearance();
                    page.annotPushBack(annot);
                    mAnnotPushedBack = true;
                    setAnnot(annot, pageNum);
                    buildAnnotBBox();
                    mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
                    raiseAnnotationAddedEvent(mAnnot, mAnnotPageNum);
                    success = true;
                }
            }
        } catch (Exception ex) {
            mNextToolMode = ToolManager.ToolMode.PAN;
            ((ToolManager) mPdfViewCtrl.getToolManager()).annotationCouldNotBeAdded(ex.getMessage());
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
            onCreateMarkupFailed(ex);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
            clearTargetPoint();
            setNextToolModeHelper();
        }
        return success;
    }

    @Nullable
    protected Rect getBBox(PointF targetScreenPoint, int pageNum) throws PDFNetException {
        if (targetScreenPoint == null) {
            return null;
        }
        double[] pts = mPdfViewCtrl.convScreenPtToPagePt(targetScreenPoint.x, targetScreenPoint.y, pageNum);
        return new com.pdftron.pdf.Rect(pts[0], pts[1], pts[0] + mIconWidth, pts[1] + mIconHeight);
    }

    @Nullable
    protected Rect getBBoxFromPagePoint(PointF targetPagePoint) throws PDFNetException {
        if (targetPagePoint == null) {
            return null;
        }
        return new com.pdftron.pdf.Rect(targetPagePoint.x, targetPagePoint.y, targetPagePoint.x + mIconWidth, targetPagePoint.y + mIconHeight);
    }

    protected void setNextToolModeHelper() {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        if (toolManager.isAutoSelectAnnotation()) {
            mNextToolMode = getDefaultNextTool();
        } else {
            mNextToolMode = mForceSameNextToolMode ? getToolMode() : ToolManager.ToolMode.PAN;
        }
    }
}
