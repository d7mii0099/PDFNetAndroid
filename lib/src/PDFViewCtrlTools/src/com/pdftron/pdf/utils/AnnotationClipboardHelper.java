//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import androidx.annotation.Nullable;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.sdf.Obj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Helper class for annotation copy/paste
 */
public class AnnotationClipboardHelper {
    private static CopyOnWriteArrayList<Obj> sCurrentAnnotations = null;
    private static PDFDoc sTempDoc;
    private static RectF sUnionBound; // union rect in page space
    private static Lock sClipboardLock = new ReentrantLock();

    /**
     * Callback interface to be invoked when clipboard copy/paste task is finished.
     */
    public interface OnClipboardTaskListener {
        /**
         * Called when clipboard copy/paste task has been done.
         *
         * @param error The error message. Null if there is no error.
         * @param pastedAnnotList null when copy, a list of pasted annot when paste.
         */
        void onClipboardTaskDone(String error, ArrayList<Annot> pastedAnnotList);
    }

    /**
     * Copies an annotation to the clipboard.
     *
     * @param context         The context
     * @param annot           The annotation to be copied
     * @param pdfViewCopyFrom The PDFViewCtrl containing the annotation
     * @param listener        The listener to be called when the task is finished
     */
    public static void copyAnnot(Context context, Annot annot, PDFViewCtrl pdfViewCopyFrom, OnClipboardTaskListener listener) {
        ArrayList<Annot> annotsToCopy = new ArrayList<>();
        annotsToCopy.add(annot);
        copyAnnot(context, annotsToCopy, pdfViewCopyFrom, listener);
    }

    /**
     * Copies an annotation to the clipboard.
     *
     * @param context         The context
     * @param annots          The list of annotations to be copied
     * @param pdfViewCopyFrom The PDFViewCtrl containing the annotation
     * @param listener        The listener to be called when the task is finished
     */
    public static void copyAnnot(Context context, ArrayList<Annot> annots, PDFViewCtrl pdfViewCopyFrom, OnClipboardTaskListener listener) {
        new CopyPasteTask(context, pdfViewCopyFrom, annots, null, 0, null, listener).execute();
        // clear system clipboard
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("text", "");
            clipboard.setPrimaryClip(clip);
        }
    }

    /**
     * Paste the annotation from the clipboard.
     *
     * @param context        The context
     * @param pdfViewPasteTo The PDFViewCtrl to which the annotation should be pasted
     * @param pageNo         The destination page number
     * @param target         The destination location
     * @param listener       The listener to be called when the task is finished
     */
    public static void pasteAnnot(Context context, PDFViewCtrl pdfViewPasteTo, int pageNo, PointF target, OnClipboardTaskListener listener) {
        new CopyPasteTask(context, null, null, pdfViewPasteTo, pageNo, target, listener).execute();
    }

    /**
     * Removes any annotation existing on the clipboard.
     */
    @SuppressWarnings("unused")
    public static void clearClipboard() {
        sClipboardLock.lock();
        sCurrentAnnotations = null;
        sUnionBound = null;
        if (sTempDoc != null) {
            Utils.closeQuietly(sTempDoc);
        }
        sTempDoc = null;
        sClipboardLock.unlock();
    }

    /**
     * @return True if there is annotation in clipboard.
     */
    public static boolean isAnnotCopied() {
        sClipboardLock.lock();
        try {
            return sCurrentAnnotations != null && !sCurrentAnnotations.isEmpty();
        } finally {
            sClipboardLock.unlock();
        }
    }

    public static boolean isItemCopied(@Nullable Context context) {
        return isAnnotCopied() || Utils.isImageCopied(context);
    }

    private static class CopyPasteTask extends CustomAsyncTask<Void, Void, String> {
        private ArrayList<Annot> mAnnotsToCopy;
        private PDFViewCtrl mPdfViewCopyFrom;
        private PDFViewCtrl mPdfViewToPaste;
        private int mPageNoToPaste;
        private PDFDoc mDoc;
        private Handler mHandler;
        private ProgressDialog mProgress = null;
        private PointF mTarget;
        private double[] mPageTarget;
        private ArrayList<Annot> mPastedAnnots;
        private OnClipboardTaskListener mOnClipboardTaskListener;

        CopyPasteTask(Context context, PDFViewCtrl pdfViewCopyFrom, @Nullable ArrayList<Annot> annotsToCopy,
                PDFViewCtrl pdfViewToPaste, int pageNoToPaste, PointF target,
                OnClipboardTaskListener listener) {
            super(context);
            mAnnotsToCopy = null;
            if (annotsToCopy != null && !annotsToCopy.isEmpty()) {
                mAnnotsToCopy = new ArrayList<>(annotsToCopy);

                sClipboardLock.lock();
                sCurrentAnnotations = new CopyOnWriteArrayList<>();
                sUnionBound = null;
                sClipboardLock.unlock();
            }
            mPdfViewToPaste = pdfViewToPaste;
            mPdfViewCopyFrom = pdfViewCopyFrom;
            mPageNoToPaste = pageNoToPaste;
            mHandler = new Handler();
            mTarget = target;
            mOnClipboardTaskListener = listener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            final Context context = getContext();
            if (context == null) {
                return;
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mProgress = new ProgressDialog(context);
                    mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    mProgress.setMessage(context.getString((mAnnotsToCopy != null ? R.string.tools_copy_annot_waiting : R.string.tools_paste_annot_waiting)));
                    mProgress.show();
                }
            }, 750);
            if (mPdfViewToPaste != null) {
                mDoc = mPdfViewToPaste.getDoc();
                mPageTarget = mPdfViewToPaste.convScreenPtToPagePt(mTarget.x, mTarget.y, mPageNoToPaste);
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            String error = null;
            if (mAnnotsToCopy != null) {
                boolean shouldUnlockRead = false;
                boolean shouldUnlockClipboard = false;

                try {
                    sTempDoc = new PDFDoc();
                } catch (Exception ex) {
                    sTempDoc = null;
                }

                if (null == sTempDoc) {
                    sCurrentAnnotations = null;
                    return "Unable to create temp doc";
                }

                for (Annot annot : mAnnotsToCopy) {
                    try {
                        //noinspection WrongThread
                        mPdfViewCopyFrom.docLockRead();
                        shouldUnlockRead = true;

                        Obj srcAnnotation = annot.getSDFObj();

                        Obj p = srcAnnotation.findObj("P");
                        if (p == null) {
                            return "Cannot find the object";
                        }
                        Obj[] pageArray = {p};
                        Obj[] srcAnnotArray = {srcAnnotation};

                        sClipboardLock.lock();
                        shouldUnlockClipboard = true;
                        Rect bbox = annot.getRect();
                        bbox.normalize();
                        if (null == sUnionBound) {
                            sUnionBound = new RectF((float) bbox.getX1(), (float) bbox.getY1(), (float) bbox.getX2(), (float) bbox.getY2());
                        } else {
                            sUnionBound.union((float) bbox.getX1(), (float) bbox.getY1(), (float) bbox.getX2(), (float) bbox.getY2());
                        }
                        sCurrentAnnotations.add(sTempDoc.getSDFDoc().importObjs(srcAnnotArray, pageArray)[0]);
                    } catch (Exception ex) {
                        // make sure we always have a non-null error string
                        error = (ex.getMessage() != null) ? ex.getMessage() : "Unknown Error";
                        AnalyticsHandlerAdapter.getInstance().sendException(ex);
                    } finally {
                        if (shouldUnlockRead) {
                            //noinspection WrongThread
                            mPdfViewCopyFrom.docUnlockRead();
                        }
                        if (shouldUnlockClipboard) {
                            sClipboardLock.unlock();
                        }
                    }
                }
            } else {
                boolean shouldUnlock = false;
                try {
                    if (!isAnnotCopied()) {
                        return null;
                    }
                    mPastedAnnots = new ArrayList<>();
                    sClipboardLock.lock();
                    RectF unionRect = new RectF(sUnionBound);
                    sClipboardLock.unlock();

                    Rect pcClipBound = new Rect(unionRect.left, Math.min(unionRect.top, unionRect.bottom),
                            unionRect.right, Math.max(unionRect.top, unionRect.bottom));
                    pcClipBound.normalize();

                    //noinspection WrongThread
                    mPdfViewToPaste.docLock(true);
                    shouldUnlock = true;

                    Page page = mDoc.getPage(mPageNoToPaste);
                    Rect cropBox = page.getBox(Page.e_user_crop);
                    cropBox.normalize();

                    // bound target point
                    // right edge
                    if ((mPageTarget[0] + unionRect.width() / 2) > cropBox.getX2()) {
                        mPageTarget[0] = cropBox.getX2() - unionRect.width() / 2;
                    }
                    // left edge
                    if (mPageTarget[0] - unionRect.width() / 2 < cropBox.getX1()) {
                        mPageTarget[0] = cropBox.getX1() + unionRect.width() / 2;
                    }
                    // top
                    if (mPageTarget[1] + unionRect.height() / 2 > cropBox.getY2()) {
                        mPageTarget[1] = cropBox.getY2() - unionRect.height() / 2;
                    }
                    // bottom
                    if (mPageTarget[1] - unionRect.height() / 2 < cropBox.getY1()) {
                        mPageTarget[1] = cropBox.getY1() + unionRect.height() / 2;
                    }

                    for (Obj annotObj : sCurrentAnnotations) {

                        Obj destAnnot = mDoc.getSDFDoc().importObj(annotObj, true);
                        Annot newAnnot = new Annot(destAnnot);

                        if (mPdfViewToPaste.getToolManager() instanceof ToolManager) {
                            ToolManager toolManager = (ToolManager) mPdfViewToPaste.getToolManager();
                            String key = toolManager.generateKey();
                            if (key != null) {
                                newAnnot.setUniqueID(key);
                            }
                            if (newAnnot.isMarkup()) {
                                Markup markup = new Markup(newAnnot);
                                String authorName = toolManager.getAuthorId();
                                if (authorName == null && getContext() != null) {
                                    authorName = PdfViewCtrlSettingsManager.getAuthorName(getContext());
                                }
                                if (authorName != null) {
                                    markup.setTitle(authorName);
                                }
                            }
                        }

                        if (newAnnot.getType() == Annot.e_FreeText) {
                            // remove RC appearance
                            newAnnot.deleteCustomData(AnnotUtils.KEY_RawRichContent);
                        }

                        Rect boundingBox = newAnnot.getRect();
                        boundingBox.normalize();

                        // calculate new bbox
                        Point pcAnnotLeft = new Point(boundingBox.getX1(), boundingBox.getY1());
                        Point pcUnionLeft = new Point(pcClipBound.getX1(), pcClipBound.getY1());
                        Point pcDisp = new Point(pcAnnotLeft.x - pcUnionLeft.x, pcAnnotLeft.y - pcUnionLeft.y);
                        double width = boundingBox.getWidth();
                        double height = boundingBox.getHeight();
                        // shift to center
                        Point pcAnnotBtmLeft = new Point(mPageTarget[0] - pcClipBound.getWidth() / 2 + pcDisp.x, mPageTarget[1] - pcClipBound.getHeight() / 2 + pcDisp.y);
                        Rect pcAnnotDestRect = new Rect(pcAnnotBtmLeft.x, pcAnnotBtmLeft.y,
                                pcAnnotBtmLeft.x + width, pcAnnotBtmLeft.y + height);

                        page.annotPushBack(newAnnot);
                        newAnnot.resize(pcAnnotDestRect);

                        if (newAnnot.getType() == Annot.e_FreeText && !AnnotUtils.hasRotation(newAnnot)) {
                            int pageRotation = mPdfViewToPaste.getDoc().getPage(mPageNoToPaste).getRotation();
                            int viewRotation = mPdfViewToPaste.getPageRotation();
                            int annotRotation = ((pageRotation + viewRotation) % 4) * 90;
                            newAnnot.setRotation(annotRotation);
                        }

                        final Context context = getContext();
                        if (context != null) {
                            AnnotUtils.refreshAnnotAppearance(context, newAnnot);
                        }
                        mPastedAnnots.add(newAnnot);
                    }
                } catch (Exception ex) {
                    // make sure we always have a non-null error string
                    error = (ex.getMessage() != null) ? ex.getMessage() : "Unknown Error";
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                } finally {
                    if (shouldUnlock) {
                        //noinspection WrongThread
                        mPdfViewToPaste.docUnlock();
                    }
                }
            }
            return error;
        }

        @Override
        public void onPostExecute(String error) {
            if (null != error) {
                // something went wrong, report error callback
                if (mPdfViewToPaste != null && mPdfViewToPaste.getToolManager() != null &&
                        mPdfViewToPaste.getToolManager() instanceof ToolManager) {
                    ToolManager toolManager = (ToolManager) mPdfViewToPaste.getToolManager();
                    toolManager.annotationCouldNotBeAdded(error);
                }
            } else {
                if (mPdfViewToPaste != null && isAnnotCopied() && mPastedAnnots != null) {
                    HashMap<Annot, Integer> annots = new HashMap<>(mPastedAnnots.size());
                    for (Annot annot : mPastedAnnots) {
                        try {
                            mPdfViewToPaste.update(annot, mPageNoToPaste);
                            annots.put(annot, mPageNoToPaste);
                        } catch (Exception ex) {
                            AnalyticsHandlerAdapter.getInstance().sendException(ex);
                        }
                    }
                    if (mPdfViewToPaste.getToolManager() != null &&
                            mPdfViewToPaste.getToolManager() instanceof ToolManager) {
                        ToolManager toolManager = (ToolManager) mPdfViewToPaste.getToolManager();
                        toolManager.raiseAnnotationsAddedEvent(annots);
                    }
                }
            }

            mHandler.removeCallbacksAndMessages(null);
            if (mProgress != null) {
                if (mProgress.isShowing())
                    mProgress.dismiss();
                mProgress = null;
            }

            if (mOnClipboardTaskListener != null) {
                mOnClipboardTaskListener.onClipboardTaskDone(error, mPastedAnnots);
            }
        }
    }
}
