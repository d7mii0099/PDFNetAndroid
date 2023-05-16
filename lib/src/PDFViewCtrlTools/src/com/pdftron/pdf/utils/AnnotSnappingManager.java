package com.pdftron.pdf.utils;

import android.graphics.Point;
import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Rect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * This class is in charge of storing the data used for annotation snapping.
 *
 * Snapping rules:
 * - Snapping is separated into horizontal and vertical snapping, and we can only show at most one of each.
 * - Snapping will prioritize snapping in each axis as follows: page (center) > annot (center) > annot (edges)
 *   (for example, if an annot can snap to both an annot and page on the x axis, it will prioritize showing page snapping)
 * - It multiple snapping occurs, we prioritize showing snapping to the closest point
 */
public class AnnotSnappingManager {

    private static String TAG = "AnnotSnappingManager";

    private final HashMap<Integer, List<SnappingData>> mSnappingDatas = new HashMap<>(); // <Page Number, SnappingData>
    private final HashMap<Integer, Disposable> mDisposableMap = new HashMap<>();

    private double mThreshold = 24.0f;
    private boolean mEnabled;
    private boolean mPaused;

    /**
     * Returns information on whether the supplied rect can snap with any of the existing annotations
     *
     * @param pdfViewCtrl the PDFViewCtrl
     * @param annot the annotation of interest to applys snapping
     * @param rect the rect to compare against the cached information. Assumes rect is normalized.
     * @param pageNum the page number containg the annotation
     * @param pageCenterSnapping whether page center snapping is checked
     * @param annotCenterSnapping whether annot center snapping is checked
     * @param annotLeftSnapping whether annot left snapping is checked
     * @param annotTopSnapping whether annot top snapping is checked
     * @param annotRightSnapping whether annot right snapping is checked
     * @param annotBottomSnapping whether annot bottom snapping is checked
     *
     * @return the result of snapping
     */
    public SnappingResult checkSnapping(@NonNull PDFViewCtrl pdfViewCtrl,
            @NonNull Annot annot,
            @NonNull Rect rect,
            int pageNum,
            boolean pageCenterSnapping,
            boolean annotCenterSnapping,
            boolean annotLeftSnapping,
            boolean annotTopSnapping,
            boolean annotRightSnapping,
            boolean annotBottomSnapping) throws PDFNetException {
        List<SnappingData> pageSnappingData = mSnappingDatas.get(pageNum);
        if (pageSnappingData == null || pageSnappingData.isEmpty() || !mEnabled || mPaused) {
            return SnappingResult.EMPTY;
        }
        String snappingId = getSnappingId(annot);
        double minPageCenterHDiff = Double.MAX_VALUE;
        double minPageCenterVDiff = Double.MAX_VALUE;
        double minLeftDiff = Double.MAX_VALUE;
        double minRightDiff = Double.MAX_VALUE;
        double minBottomDiff = Double.MAX_VALUE;
        double minTopDiff = Double.MAX_VALUE;
        double minCenterHDiff = Double.MAX_VALUE;
        double minCenterVDiff = Double.MAX_VALUE;

        Pair<Point, Point> horizontalLineToDraw = new Pair<>(new Point(), new Point());
        Pair<Point, Point> verticalLineToDraw = new Pair<>(new Point(), new Point());

        float sx = pdfViewCtrl.getScrollX();
        float sy = pdfViewCtrl.getScrollY();

        // First check snapping for page
        Page page = pdfViewCtrl.getDoc().getPage(pageNum);
        Rect pageRect = page.getVisibleContentBox();

        double[] pageScreenPt1 = pdfViewCtrl.convPagePtToScreenPt(pageRect.getX1(), pageRect.getY1(), pageNum);
        double[] pageScreenPt2 = pdfViewCtrl.convPagePtToScreenPt(pageRect.getX2(), pageRect.getY2(), pageNum);

        double pageX1 = pageScreenPt1[0] + sx;
        double pageY1 = pageScreenPt1[1] + sy;
        double pageX2 = pageScreenPt2[0] + sx;
        double pageY2 = pageScreenPt2[1] + sy;

        double distPage;
        if (pageCenterSnapping) {
            // Check page x-axis center
            distPage = (rect.getX2() + rect.getX1()) / 2.0f - (pageX2 + pageX1) / 2.0f;
            if (Math.abs(distPage) < mThreshold && Math.abs(distPage) < minPageCenterHDiff) {
                minPageCenterHDiff = distPage;

                verticalLineToDraw.first.x = (int) ((pageX2 + pageX1) / 2.0f);
                verticalLineToDraw.first.y = (int) pageY1;
                verticalLineToDraw.second.x = (int) ((pageX2 + pageX1) / 2.0f);
                verticalLineToDraw.second.y = (int) pageY2;
            }

            // Check page y-axis center
            distPage = (rect.getY2() + rect.getY1()) / 2.0f - (pageY2 + pageY1) / 2.0f;
            if (Math.abs(distPage) < mThreshold && Math.abs(distPage) < minPageCenterVDiff) {
                minPageCenterVDiff = distPage;

                horizontalLineToDraw.first.x = (int) pageX1;
                horizontalLineToDraw.first.y = (int) ((pageY2 + pageY1) / 2.0f);
                horizontalLineToDraw.second.x = (int) pageX2;
                horizontalLineToDraw.second.y = (int) ((pageY2 + pageY1) / 2.0f);
            }
        }

        // Check snapping for annotations
        for (SnappingData snappingData : pageSnappingData) {
            if (snappingData.getSnappingId().equals(snappingId) || !isInSnappingGroup(annot.getType(), snappingData.getAnnotType())) {
                continue;
            }
            double[] screenPt1 = pdfViewCtrl.convPagePtToScreenPt(snappingData.getX1(), snappingData.getY1(), pageNum);
            double[] screenPt2 = pdfViewCtrl.convPagePtToScreenPt(snappingData.getX2(), snappingData.getY2(), pageNum);
            double annotScreenX1 = screenPt1[0] + sx;
            double annotScreenY1 = screenPt1[1] + sy;
            double annotScreenX2 = screenPt2[0] + sx;
            double annotScreenY2 = screenPt2[1] + sy;

            double dist;

            if (annotCenterSnapping) {
                // Check x-axis center
                if (minPageCenterHDiff == Double.MAX_VALUE) { // if we have not snapped to page, then we can check snapping to annot center
                    dist = (rect.getX2() + rect.getX1()) / 2.0f - (annotScreenX2 + annotScreenX1) / 2.0f;
                    if (Math.abs(dist) < mThreshold && Math.abs(dist) < minCenterHDiff) {
                        minCenterHDiff = dist;

                        verticalLineToDraw.first.x = (int) (int) ((annotScreenX2 + annotScreenX1) / 2.0f);
                        verticalLineToDraw.first.y = (int) Math.max(annotScreenY1, rect.getY1());
                        verticalLineToDraw.second.x = (int) (int) ((annotScreenX2 + annotScreenX1) / 2.0f);
                        verticalLineToDraw.second.y = (int) Math.min(annotScreenY2, rect.getY2());
                    }
                }

                // Check y-axis center
                if (minPageCenterVDiff == Double.MAX_VALUE) { // if we have not snapped to page, then we can check snapping to annot center
                    dist = (rect.getY2() + rect.getY1()) / 2.0f - (annotScreenY2 + annotScreenY1) / 2.0f;
                    if (Math.abs(dist) < mThreshold && Math.abs(dist) < minCenterVDiff) {
                        minCenterVDiff = dist;

                        horizontalLineToDraw.first.x = (int) Math.min(rect.getX1(), annotScreenX1);
                        horizontalLineToDraw.first.y = (int) (int) ((annotScreenY2 + annotScreenY1) / 2.0f);
                        horizontalLineToDraw.second.x = (int) Math.max(rect.getX2(), annotScreenX2);
                        horizontalLineToDraw.second.y = (int) (int) ((annotScreenY2 + annotScreenY1) / 2.0f);
                    }
                }
            }

            if (minCenterHDiff == Double.MAX_VALUE && minPageCenterHDiff == Double.MAX_VALUE) { // if we have not snapped to page and annot center, then we can check snapping to annot edge
                // Check left edge
                if (annotLeftSnapping) {
                    dist = rect.getX1() - annotScreenX1;
                    if (Math.abs(dist) < mThreshold && Math.abs(dist) < minLeftDiff) {
                        minLeftDiff = dist;

                        verticalLineToDraw.first.x = (int) annotScreenX1;
                        verticalLineToDraw.first.y = (int) Math.max(annotScreenY1, rect.getY1());
                        verticalLineToDraw.second.x = (int) annotScreenX1;
                        verticalLineToDraw.second.y = (int) Math.min(annotScreenY2, rect.getY2());
                    }
                    dist = rect.getX1() - annotScreenX2;
                    if (Math.abs(dist) < mThreshold && Math.abs(dist) < minLeftDiff) {
                        minLeftDiff = dist;

                        verticalLineToDraw.first.x = (int) annotScreenX2;
                        verticalLineToDraw.first.y = (int) Math.max(annotScreenY1, rect.getY1());
                        verticalLineToDraw.second.x = (int) annotScreenX2;
                        verticalLineToDraw.second.y = (int) Math.min(annotScreenY2, rect.getY2());
                    }
                }

                // Check right edge
                if (annotRightSnapping) {
                    dist = rect.getX2() - annotScreenX1;
                    if (Math.abs(dist) < mThreshold && Math.abs(dist) < minRightDiff) {
                        minRightDiff = dist;

                        verticalLineToDraw.first.x = (int) annotScreenX1;
                        verticalLineToDraw.first.y = (int) Math.max(annotScreenY1, rect.getY1());
                        verticalLineToDraw.second.x = (int) annotScreenX1;
                        verticalLineToDraw.second.y = (int) Math.min(annotScreenY2, rect.getY2());
                    }
                    dist = rect.getX2() - annotScreenX2;
                    if (Math.abs(dist) < mThreshold && Math.abs(dist) < minRightDiff) {
                        minRightDiff = dist;

                        verticalLineToDraw.first.x = (int) annotScreenX2;
                        verticalLineToDraw.first.y = (int) Math.max(annotScreenY1, rect.getY1());
                        verticalLineToDraw.second.x = (int) annotScreenX2;
                        verticalLineToDraw.second.y = (int) Math.min(annotScreenY2, rect.getY2());
                    }
                }
            }

            if (minCenterVDiff == Double.MAX_VALUE && minPageCenterVDiff == Double.MAX_VALUE) { // if we have not snapped to page and annot center, then we can check snapping to annot edge
                // Check bottom edge
                if (annotBottomSnapping) {
                    dist = rect.getY1() - annotScreenY1;
                    if (Math.abs(dist) < mThreshold && Math.abs(dist) < minBottomDiff) {
                        minBottomDiff = dist;

                        horizontalLineToDraw.first.x = (int) Math.min(rect.getX1(), annotScreenX1);
                        horizontalLineToDraw.first.y = (int) annotScreenY1;
                        horizontalLineToDraw.second.x = (int) Math.max(rect.getX2(), annotScreenX2);
                        horizontalLineToDraw.second.y = (int) annotScreenY1;
                    }
                    dist = rect.getY1() - annotScreenY2;
                    if (Math.abs(dist) < mThreshold && Math.abs(dist) < minBottomDiff) {
                        minBottomDiff = dist;

                        horizontalLineToDraw.first.x = (int) Math.min(rect.getX1(), annotScreenX1);
                        horizontalLineToDraw.first.y = (int) annotScreenY2;
                        horizontalLineToDraw.second.x = (int) Math.max(rect.getX2(), annotScreenX2);
                        horizontalLineToDraw.second.y = (int) annotScreenY2;
                    }
                }

                // Check top edge
                if (annotTopSnapping) {
                    dist = rect.getY2() - annotScreenY1;
                    if (Math.abs(dist) < mThreshold && Math.abs(dist) < minTopDiff) {
                        minTopDiff = dist;

                        horizontalLineToDraw.first.x = (int) Math.min(rect.getX1(), annotScreenX1);
                        horizontalLineToDraw.first.y = (int) annotScreenY1;
                        horizontalLineToDraw.second.x = (int) Math.max(rect.getX2(), annotScreenX2);
                        horizontalLineToDraw.second.y = (int) annotScreenY1;
                    }
                    dist = rect.getY2() - annotScreenY2;
                    if (Math.abs(dist) < mThreshold && Math.abs(dist) < minTopDiff) {
                        minTopDiff = dist;

                        horizontalLineToDraw.first.x = (int) Math.min(rect.getX1(), annotScreenX1);
                        horizontalLineToDraw.first.y = (int) annotScreenY2;
                        horizontalLineToDraw.second.x = (int) Math.max(rect.getX2(), annotScreenX2);
                        horizontalLineToDraw.second.y = (int) annotScreenY2;
                    }
                }
            }
        }

        double horizontalShift = 0;
        HashSet<SnappingType> snappingTypes = new HashSet<>();
        if (minLeftDiff != Double.MAX_VALUE && minRightDiff != Double.MAX_VALUE) {
            if (minLeftDiff < minRightDiff) {
                horizontalShift = minLeftDiff;
                snappingTypes.add(SnappingType.LEFT);
            } else {
                horizontalShift = minRightDiff;
                snappingTypes.add(SnappingType.RIGHT);
            }
        } else if (minLeftDiff != Double.MAX_VALUE) {
            horizontalShift = minLeftDiff;
            snappingTypes.add(SnappingType.LEFT);
        } else if (minRightDiff != Double.MAX_VALUE) {
            horizontalShift = minRightDiff;
            snappingTypes.add(SnappingType.RIGHT);
        } else if (minCenterHDiff != Double.MAX_VALUE) {
            horizontalShift = minCenterHDiff;
            snappingTypes.add(SnappingType.CENTER_HORIZONTAL);
        } else if (minPageCenterHDiff != Double.MAX_VALUE) {
            horizontalShift = minPageCenterHDiff;
            snappingTypes.add(SnappingType.CENTER_HORIZONTAL_PAGE);
        }

        double verticalShift = 0;
        if (minBottomDiff != Double.MAX_VALUE && minTopDiff != Double.MAX_VALUE) {
            if (minBottomDiff < minTopDiff) {
                verticalShift = minBottomDiff;
                snappingTypes.add(SnappingType.BOTTOM);
            } else {
                verticalShift = minTopDiff;
                snappingTypes.add(SnappingType.TOP);
            }
        } else if (minBottomDiff != Double.MAX_VALUE) {
            verticalShift = minBottomDiff;
            snappingTypes.add(SnappingType.BOTTOM);
        } else if (minTopDiff != Double.MAX_VALUE) {
            verticalShift = minTopDiff;
            snappingTypes.add(SnappingType.TOP);
        } else if (minCenterVDiff != Double.MAX_VALUE) {
            verticalShift = minCenterVDiff;
            snappingTypes.add(SnappingType.CENTER_VERTICAL);
        } else if (minPageCenterVDiff != Double.MAX_VALUE) {
            verticalShift = minPageCenterVDiff;
            snappingTypes.add(SnappingType.CENTER_VERTICAL_PAGE);
        }

        if (horizontalShift != 0 || verticalShift != 0) {
            Rect resultRect = new Rect(rect.getX1(), rect.getY1(), rect.getX2(), rect.getY2());
            resultRect.setX1(rect.getX1() - horizontalShift);
            resultRect.setX2(rect.getX2() - horizontalShift);
            resultRect.setY1(rect.getY1() - verticalShift);
            resultRect.setY2(rect.getY2() - verticalShift);
            List<Pair<Point, Point>> linesToDraw = new ArrayList<>();
            linesToDraw.add(verticalLineToDraw);
            linesToDraw.add(horizontalLineToDraw);
            return new SnappingResult(
                    snappingTypes,
                    resultRect,
                    linesToDraw
            );
        }

        return SnappingResult.EMPTY;
    }

    public void tryUpdateCache(@NonNull PDFViewCtrl pdfViewCtrl, boolean forceUpdate) {
        if (!mEnabled) {
            return;
        }
        PDFDoc doc = pdfViewCtrl.getDoc();
        if (doc != null) {
            int[] visiblePages = null;
            try {
                visiblePages = pdfViewCtrl.getVisiblePages();
            } catch (Exception e) {
                // do nothing
            }
            if (visiblePages != null) {
                for (int visiblePage : visiblePages) {
                    tryUpdateCache(doc, visiblePage, forceUpdate);
                }
            }
        }
    }

    private void tryUpdateCache(@NonNull PDFDoc pdfDoc, int pageNum, boolean forceUpdate) {
        if ((mDisposableMap.containsKey(pageNum) || mSnappingDatas.containsKey(pageNum)) && !forceUpdate) {
            return;
        }
        if (forceUpdate) {
            dispose(pageNum);
        }
        mDisposableMap.put(
                pageNum,
                freshSnappingData(pdfDoc, pageNum)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally(new Action() {
                            @Override
                            public void run() throws Exception {
                                Utils.throwIfNotOnMainThread();
                                mDisposableMap.remove(pageNum);
                            }
                        })
                        .subscribe(new Consumer<HashMap<Integer, List<SnappingData>>>() {
                            @Override
                            public void accept(HashMap<Integer, List<SnappingData>> newSnappingData) throws Exception {
                                Utils.throwIfNotOnMainThread();
                                if (newSnappingData.size() == 1) {
                                    for (Map.Entry<Integer, List<SnappingData>> integerListEntry : newSnappingData.entrySet()) {
                                        Integer page = integerListEntry.getKey();
                                        List<SnappingData> snappingDatas = integerListEntry.getValue();
                                        mSnappingDatas.put(page, snappingDatas);
                                        Logger.INSTANCE.LogD(TAG, "Added snapping data for page " + pageNum + ", " + snappingDatas.size() + " annots found");
                                    }
                                } else {
                                    Logger.INSTANCE.LogE(TAG, "More than one page obtained, this should not happen");
                                }
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                AnalyticsHandlerAdapter.getInstance().sendException(new Exception(throwable));
                            }
                        })
        );
    }

    public void clearCache() {
        if (!mEnabled) {
            return;
        }
        disposeAll();
        mSnappingDatas.clear();
    }

    private void disposeAll() {
        List<Disposable> disposablesToDispose = new ArrayList<>();
        for (Map.Entry<Integer, Disposable> disposableEntry : mDisposableMap.entrySet()) {
            Disposable disposable = disposableEntry.getValue();
            if (!disposable.isDisposed()) {
                disposablesToDispose.add(disposable);
            }
        }
        mDisposableMap.clear();
        for (Disposable disposable : disposablesToDispose) {
            disposable.dispose();
        }
    }

    private void dispose(int pageToClear) {
        Disposable disposable = mDisposableMap.get(pageToClear);
        if (disposable != null) {
            if (!disposable.isDisposed()) {
                mDisposableMap.remove(pageToClear);
                disposable.dispose();
            }
        }
    }

    @NonNull
    private Single<HashMap<Integer, List<SnappingData>>> freshSnappingData(@NonNull PDFDoc pdfDoc, int currPage) {
        return Single.create(new SingleOnSubscribe<HashMap<Integer, List<SnappingData>>>() {
            @Override
            public void subscribe(SingleEmitter<HashMap<Integer, List<SnappingData>>> emitter) throws Exception {
                Utils.throwIfOnMainThread();
                Logger.INSTANCE.LogD(TAG, "Parsing page " + currPage);
                HashMap<Integer, List<SnappingData>> snappingData = new HashMap<>();
                boolean shouldUnlockRead = false;
                try {
                    pdfDoc.lockRead();
                    shouldUnlockRead = true;
                    Page page = pdfDoc.getPage(currPage);
                    if (page != null && page.isValid()) {
                        final int numAnnots = page.getNumAnnots();
                        List<SnappingData> pageSnappingData = new ArrayList<>();
                        for (int i = 0; i < numAnnots; i++) {
                            if (emitter.isDisposed()) {
                                break;
                            }
                            Annot annot = page.getAnnot(i);
                            if (annot != null && annot.isValid()) {
                                String annotId = getSnappingId(annot);
                                Rect rect = annot.getRect();
                                pageSnappingData.add(
                                        new SnappingData(
                                                annotId,
                                                annot.getType(),
                                                rect.getX1(),
                                                rect.getY1(),
                                                rect.getX2(),
                                                rect.getY2(),
                                                currPage)
                                );
                            }
                        }
                        snappingData.put(currPage, pageSnappingData);
                    }
                } catch (PDFNetException e) {
                    emitter.onError(e);
                } finally {
                    if (shouldUnlockRead) {
                        Utils.unlockReadQuietly(pdfDoc);
                    }
                }
                emitter.onSuccess(snappingData);
            }
        });
    }

    /**
     * Returns the id that is used for annotation snapping. Should read lock around this method.
     */
    private static String getSnappingId(@NonNull Annot annot) {
        return Long.toString(annot.__GetHandle());
    }

    public void setThreshold(double threshold) {
        mThreshold = threshold;
    }

    private static boolean isInSnappingGroup(int thisAnnotType, int thatAnnotType) {
        SnappingGroup thisSnappingGroup = getSnappingGroup(thisAnnotType);
        SnappingGroup thatSnappingGroup = getSnappingGroup(thatAnnotType);
        return thisSnappingGroup != null && thatSnappingGroup != null && thisSnappingGroup == thatSnappingGroup;
    }

    @Nullable
    private static SnappingGroup getSnappingGroup(int annotType) {
        switch (annotType) {
            case Annot.e_Line:
            case Annot.e_Circle:
            case Annot.e_Square:
            case Annot.e_Polygon:
            case Annot.e_Polyline:
                return SnappingGroup.SHAPE;
            case Annot.e_FreeText:
                return SnappingGroup.FREE_TEXT;
            case Annot.e_Widget:
                return SnappingGroup.FORM_FIELD;
            default:
                return null;
        }
    }

    /**
     * Sets whether snapping should be disabled. When disabled, no snapping will occur and the cache
     * will not be populated.
     */
    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    /**
     * Sets whether snapping should be paused. When paused, no snapping will occur but the cache
     * will continue to be updated.
     */
    public void setPaused(boolean paused) {
        mPaused = paused;
    }

    /**
     * Defines annotation groups that should snap to each other.
     */
    public enum SnappingGroup {
        FREE_TEXT,
        SHAPE,
        FORM_FIELD
    }

    public enum SnappingType {
        LEFT,
        TOP,
        RIGHT,
        BOTTOM,
        CENTER_VERTICAL,
        CENTER_HORIZONTAL,
        CENTER_VERTICAL_PAGE,
        CENTER_HORIZONTAL_PAGE
    }

    public static class SnappingResult {
        private static final SnappingResult EMPTY = new SnappingResult();
        @Nullable
        private Set<SnappingType> mSnappingType = null;
        @Nullable
        private Rect mRect = null;

        @Nullable
        private List<Pair<Point, Point>> mSnappingLines = null;

        public SnappingResult() {
        }

        public SnappingResult(@Nullable Set<SnappingType> snappingType, @Nullable Rect rect, @Nullable List<Pair<Point, Point>> snappingLines) {
            mSnappingType = snappingType;
            mRect = rect;
            mSnappingLines = snappingLines;
        }

        @Nullable
        public Set<SnappingType> getSnappingType() {
            return mSnappingType;
        }

        @Nullable
        public Rect getRect() {
            return mRect;
        }

        public boolean isSnapping() {
            return mRect != null;
        }

        @Nullable
        public List<Pair<Point, Point>> getSnappingLines() {
            return mSnappingLines;
        }
    }

    private static class SnappingData {
        @NonNull
        private String mSnappingId;
        private int mAnnotType;
        // X, Y are in page space
        private double mX1;
        private double mY1;
        private double mX2;
        private double mY2;
        private double mPageNum;

        public SnappingData(@NonNull String snappingId, int annotType, double x1, double y1, double x2, double y2, double pageNum) {
            mSnappingId = snappingId;
            mAnnotType = annotType;
            mX1 = x1;
            mY1 = y1;
            mX2 = x2;
            mY2 = y2;
            mPageNum = pageNum;
        }

        @NonNull
        public String getSnappingId() {
            return mSnappingId;
        }

        public int getAnnotType() {
            return mAnnotType;
        }

        public double getX1() {
            return mX1;
        }

        public double getY1() {
            return mY1;
        }

        public double getX2() {
            return mX2;
        }

        public double getY2() {
            return mY2;
        }

        public double getPageNum() {
            return mPageNum;
        }
    }
}
