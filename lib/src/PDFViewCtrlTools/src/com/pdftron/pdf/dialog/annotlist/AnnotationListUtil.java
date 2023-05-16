package com.pdftron.pdf.dialog.annotlist;

import androidx.annotation.Nullable;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageIterator;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.TextExtractor;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.controls.AnnotationDialogFragment;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.Utils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

/**
 * Utility class for fetching annotations in {@link PDFViewCtrl}.
 */
public class AnnotationListUtil {

    /**
     * Create an {@link Observable} that retrieves all annotations in a {@link PDFViewCtrl}, and
     * emits a list of annotations one page at a time.
     *
     * @param pdfViewCtrl the PDFViewCtrl to obtain
     * @return observable that emits annotations each page at a time.
     */
    public static Observable<List<AnnotationDialogFragment.AnnotationInfo>> from(@Nullable final PDFViewCtrl pdfViewCtrl, @Nullable ArrayList<Integer> excludedAnnotationListTypes) {
        return Observable.create(new ObservableOnSubscribe<List<AnnotationDialogFragment.AnnotationInfo>>() {
            @Override
            public void subscribe(ObservableEmitter<List<AnnotationDialogFragment.AnnotationInfo>> emitter) throws Exception {
                if (pdfViewCtrl == null) {
                    emitter.onComplete();
                    return;
                }
                PageIterator pageIterator = null;
                boolean shouldUnlockRead = false;
                try {
                    try {
                        pdfViewCtrl.docLockRead();
                        shouldUnlockRead = true;
                        PDFDoc doc = pdfViewCtrl.getDoc();
                        if (doc != null) {
                            pageIterator = doc.getPageIterator(1);
                        }
                    } catch (Exception e) {
                        AnalyticsHandlerAdapter.getInstance().sendException(e);
                    } finally {
                        if (shouldUnlockRead) {
                            pdfViewCtrl.docUnlockRead();
                        }
                        shouldUnlockRead = false;
                    }
                    if (pageIterator != null) {

                        int pageNum = 0;
                        TextExtractor textExtractor = new TextExtractor();

                        while (pageIterator.hasNext() || pdfViewCtrl == null) {
                            if (emitter.isDisposed()) {
                                emitter.onComplete();
                                return;
                            }

                            pageNum++;
                            Page page = null;
                            try {
                                pdfViewCtrl.docLockRead();
                                shouldUnlockRead = true;
                                page = pageIterator.next();
                            } catch (Exception e) {
                                AnalyticsHandlerAdapter.getInstance().sendException(e);
                            } finally {
                                if (shouldUnlockRead) {
                                    pdfViewCtrl.docUnlockRead();
                                }
                                shouldUnlockRead = false;
                            }

                            List<AnnotationDialogFragment.AnnotationInfo> pageAnnots = new ArrayList<>();
                            if (page != null && page.isValid()) {
                                ArrayList<Annot> annotations = new ArrayList<>();
                                try {
                                    pdfViewCtrl.docLockRead();
                                    shouldUnlockRead = true;
                                    annotations = pdfViewCtrl.getAnnotationsOnPage(pageNum);
                                } catch (Exception ex) {
                                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                                } finally {
                                    if (shouldUnlockRead) {
                                        pdfViewCtrl.docUnlockRead();
                                    }
                                    shouldUnlockRead = false;
                                }
                                for (Annot annotation : annotations) {
                                    if (emitter.isDisposed() || pdfViewCtrl == null) {
                                        emitter.onComplete();
                                        return;
                                    }
                                    try {
                                        pdfViewCtrl.docLockRead();
                                        shouldUnlockRead = true;
                                        AnnotationDialogFragment.AnnotationInfo info = toAnnotationInfo(annotation, page, textExtractor, excludedAnnotationListTypes);
                                        if (info == null) {
                                            continue;
                                        }
                                        pageAnnots.add(info);
                                    } catch (PDFNetException ignored) {
                                        // this annotation has some problem, let's skip it and continue with others
                                    } finally {
                                        if (shouldUnlockRead) {
                                            pdfViewCtrl.docUnlockRead();
                                        }
                                        shouldUnlockRead = false;
                                    }
                                }
                                // Emit this page's annotations
                                emitter.onNext(pageAnnots);
                            }
                        }
                    }
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                    emitter.onError(e);
                } finally {
                    emitter.onComplete();
                }
            }
        });
    }

    @Nullable
    public static AnnotationDialogFragment.AnnotationInfo toAnnotationInfo(Annot annotation, Page page, TextExtractor textExtractor, ArrayList<Integer> excludedAnnotationListTypes) {
        try {
            if (annotation == null || !annotation.isValid()) {
                return null;
            }

            String contents = "";
            int type = AnnotUtils.getAnnotType(annotation);

            if (excludedAnnotationListTypes != null && excludedAnnotationListTypes.size() > 0) {
                if (excludedAnnotationListTypes.contains(type)) {
                    return null;
                }
            }

            if (AnnotUtils.getAnnotImageResId(type) == android.R.id.empty) {
                return null;
            }

            Markup markup = new Markup(annotation);
            switch (type) {
                case Annot.e_FreeText:
                case AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT:
                    contents = annotation.getContents();
                    break;
                case Annot.e_Underline:
                case Annot.e_StrikeOut:
                case Annot.e_Highlight:
                case Annot.e_Squiggly:
                    // For text markup we show the text itself as the contents
                    textExtractor.begin(page);
                    contents = textExtractor.getTextUnderAnnot(annotation);
                    break;
                case Annot.e_Link:
                case Annot.e_Widget:
                    if (!AnnotUtils.isMadeByPDFTron(annotation)) {
                        return null;
                    }
                    break;
                default:
                    break;
            }
            if (markup.getPopup() != null && markup.getPopup().isValid()) {
                String popupContent = markup.getPopup().getContents();
                if (!Utils.isNullOrEmpty(popupContent)) {
                    contents = popupContent;
                }
            }
            java.util.Date annotLocalDate = AnnotUtils.getAnnotLocalDate(annotation);
            String dateStr = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(annotLocalDate);
            Rect rect = markup.getRect();
            rect.normalize();

            return new AnnotationDialogFragment.AnnotationInfo(
                    type,
                    page.getIndex(),
                    contents,
                    markup.getTitle(),
                    dateStr,
                    annotation,
                    rect.getY2());
        } catch (PDFNetException ignored) {
            // this annotation has some problem, let's skip it and continue with others
        }
        return null;
    }
}
