package com.pdftron.pdf.utils;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.QuadPoint;
import com.pdftron.pdf.Redactor;
import com.pdftron.pdf.annots.Redaction;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.dialog.redaction.PageRedactionDialogFragment;
import com.pdftron.pdf.dialog.redaction.SearchRedactionDialogFragment;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.viewmodel.RedactionEvent;
import com.pdftron.pdf.viewmodel.RedactionViewModel;

import java.util.ArrayList;
import java.util.HashMap;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;

public class RedactionManager {

    @NonNull
    private PDFViewCtrl mPdfViewCtrl;

    private CompositeDisposable mDisposables = new CompositeDisposable();

    public RedactionManager(@NonNull PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        FragmentActivity activity = toolManager.getCurrentActivity();
        if (activity == null) {
            return;
        }
        final RedactionViewModel redactionViewModel = ViewModelProviders.of(activity).get(RedactionViewModel.class);

        mDisposables.add(redactionViewModel.getObservable()
                .subscribe(new Consumer<RedactionEvent>() {
                    @Override
                    public void accept(RedactionEvent redactionEvent) throws Exception {
                        if (redactionEvent.getEventType() == RedactionEvent.Type.REDACT_BY_PAGE) {
                            ArrayList<Integer> pages = redactionEvent.getPages();
                            redactPages(pages);
                        } else if (redactionEvent.getEventType() == RedactionEvent.Type.REDACT_BY_SEARCH) {
                            ArrayList<Pair<Integer, ArrayList<Double>>> results = redactionEvent.getSearchResults();
                            redactSearchResults(results);
                        } else if (redactionEvent.getEventType() == RedactionEvent.Type.REDACT_BY_SEARCH_ITEM_CLICKED) {

                        }
                    }
                })
        );
    }

    /**
     * Cleans up resources.
     */
    public void destroy() {
        mDisposables.clear();
    }

    public void openPageRedactionDialog() {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        FragmentActivity activity = toolManager.getCurrentActivity();
        if (activity == null) {
            return;
        }
        PageRedactionDialogFragment dialog = PageRedactionDialogFragment.newInstance(
                mPdfViewCtrl.getCurrentPage(), mPdfViewCtrl.getPageCount());
        dialog.setStyle(DialogFragment.STYLE_NO_TITLE, toolManager.getTheme());
        dialog.show(activity.getSupportFragmentManager(), PageRedactionDialogFragment.TAG);
    }

    public void openRedactionBySearchDialog() {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        FragmentActivity activity = toolManager.getCurrentActivity();
        if (activity == null) {
            return;
        }
        if (Utils.isLargeTablet(activity)) {
            final RedactionViewModel redactionViewModel = ViewModelProviders.of(activity).get(RedactionViewModel.class);
            redactionViewModel.onRedactBySearchOpenSheet();
        } else {
            SearchRedactionDialogFragment dialog = SearchRedactionDialogFragment.newInstance();
            dialog.setPdfViewCtrl(mPdfViewCtrl);
            dialog.setStyle(DialogFragment.STYLE_NORMAL, toolManager.getTheme());
            dialog.show(activity.getSupportFragmentManager(), SearchRedactionDialogFragment.TAG);
        }
    }

    public void redactPages(@NonNull final ArrayList<Integer> pages) throws Exception {
        mPdfViewCtrl.docLock(true, new PDFViewCtrl.LockRunnable() {
            @Override
            public void run() throws Exception {
                PDFDoc pdfDoc = mPdfViewCtrl.getDoc();

                HashMap<Annot, Integer> annots = new HashMap<>(pages.size());
                for (int pageNum : pages) {
                    Page page = pdfDoc.getPage(pageNum);
                    Redaction redaction = Redaction.create(pdfDoc, page.getCropBox());

                    setStyle(redaction);

                    redaction.refreshAppearance();
                    page.annotPushBack(redaction);

                    mPdfViewCtrl.update(redaction, pageNum);
                    annots.put(redaction, pageNum);
                }

                // raise event
                if (!annots.isEmpty()) {
                    ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                    toolManager.raiseAnnotationsAddedEvent(annots);
                }
            }
        });
    }

    public void redactSearchResults(@NonNull final ArrayList<Pair<Integer, ArrayList<Double>>> searchResults) throws Exception {
        mPdfViewCtrl.docLock(true, new PDFViewCtrl.LockRunnable() {
            @Override
            public void run() throws Exception {
                PDFDoc pdfDoc = mPdfViewCtrl.getDoc();
                ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();

                HashMap<Annot, Integer> annots = new HashMap<>(searchResults.size());

                for (Pair<Integer, ArrayList<Double>> pair : searchResults) {
                    int pageNum = pair.first;
                    ArrayList<Double> quadList = pair.second;
                    if (quadList == null) {
                        continue;
                    }
                    Double[] quads = quadList.toArray(new Double[0]);

                    int quad_count = quads.length / 8;

                    if (quad_count == 0) {
                        continue;
                    }

                    com.pdftron.pdf.Rect bbox = new com.pdftron.pdf.Rect(quads[0], quads[1], quads[4], quads[5]); //just use the first quad to temporarily populate the bbox
                    Redaction redaction = Redaction.create(pdfDoc, bbox);
                    Page page = pdfDoc.getPage(pageNum);
                    page.annotPushBack(redaction);

                    boolean useAdobeHack = toolManager.isTextMarkupAdobeHack();

                    Point p1 = new Point();
                    Point p2 = new Point();
                    Point p3 = new Point();
                    Point p4 = new Point();
                    QuadPoint qp = new QuadPoint(p1, p2, p3, p4);

                    int k = 0;
                    for (int i = 0; i < quad_count; ++i, k += 8) {
                        p1.x = quads[k];
                        p1.y = quads[k + 1];

                        p2.x = quads[k + 2];
                        p2.y = quads[k + 3];

                        p3.x = quads[k + 4];
                        p3.y = quads[k + 5];

                        p4.x = quads[k + 6];
                        p4.y = quads[k + 7];

                        if (useAdobeHack) {
                            qp.p1 = p4;
                            qp.p2 = p3;
                            qp.p3 = p1;
                            qp.p4 = p2;
                        } else {
                            qp.p1 = p1;
                            qp.p2 = p2;
                            qp.p3 = p3;
                            qp.p4 = p4;
                        }
                        redaction.setQuadPoint(i, qp);
                    }
                    setStyle(redaction);
                    redaction.refreshAppearance();

                    mPdfViewCtrl.update(redaction, pageNum);
                    annots.put(redaction, pageNum);
                }

                // raise event
                if (!annots.isEmpty()) {
                    toolManager.raiseAnnotationsAddedEvent(annots);
                }
            }
        });
    }

    public void applyRedaction() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mPdfViewCtrl.getContext());
        builder.setMessage(R.string.redact_apply_warning_body)
                .setTitle(R.string.redact_apply_warning_title)
                .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        applyRedactionImpl();
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
    }

    public void applyRedactionImpl() {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            // get all redactions
            ArrayList<Pair<Redaction, Integer>> rarr = AnnotUtils.getAllRedactions(mPdfViewCtrl);
            if (rarr.isEmpty()) {
                return;
            }
            // delete all
            AnnotUtils.deleteAllAnnotsByType(mPdfViewCtrl.getDoc(), Annot.e_Redact);
            // snapshot
            toolManager.raiseAllAnnotationsRemovedEvent();
            // redact
            for (Pair<Redaction, Integer> p : rarr) {
                ArrayList<Redactor.Redaction> arr = AnnotUtils.getRedactionArray(p.first, p.second);
                AnnotUtils.applyRedaction(mPdfViewCtrl, p.first, arr);
            }
            mPdfViewCtrl.update(true);
            // snapshot
            toolManager.getUndoRedoManger().onRedaction(null);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
        toolManager.raiseFullSaveRequired();
    }

    private void setStyle(@NonNull Redaction redaction) throws PDFNetException {
        // obtain style
        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        final float thickness = settings.getFloat(ToolStyleConfig.getInstance().getThicknessKey(Annot.e_Redact, ""),
                ToolStyleConfig.getInstance().getDefaultThickness(mPdfViewCtrl.getContext(), Annot.e_Redact));
        final int strokeColor = settings.getInt(ToolStyleConfig.getInstance().getColorKey(Annot.e_Redact, ""),
                ToolStyleConfig.getInstance().getDefaultColor(mPdfViewCtrl.getContext(), Annot.e_Redact));
        final int fillColor = settings.getInt(ToolStyleConfig.getInstance().getFillColorKey(Annot.e_Redact, ""),
                ToolStyleConfig.getInstance().getDefaultFillColor(mPdfViewCtrl.getContext(), Annot.e_Redact));
        final float opacity = settings.getFloat(ToolStyleConfig.getInstance().getOpacityKey(Annot.e_Redact, ""),
                ToolStyleConfig.getInstance().getDefaultOpacity(mPdfViewCtrl.getContext(), Annot.e_Redact));

        // set style
        AnnotUtils.setStyle(redaction, true,
                strokeColor, fillColor,
                thickness, opacity);
    }
}
