package com.pdftron.pdf.dialog.pdflayer;

import androidx.annotation.Nullable;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.ocg.Config;
import com.pdftron.pdf.ocg.Context;
import com.pdftron.pdf.ocg.Group;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.sdf.Obj;

import java.util.ArrayList;

/**
 * Utility for PDF layer.
 */
public class PdfLayerUtils {

    /**
     * Gets the ordered OCG layer.
     *
     * @param pdfViewCtrl the {@link PDFViewCtrl}
     * @param pdfDoc      the {@link PDFDoc}
     * @return An array list of ordered OCG layers.
     * @throws PDFNetException
     */
    public static ArrayList<PdfLayer> getLayers(@Nullable PDFViewCtrl pdfViewCtrl, @Nullable PDFDoc pdfDoc) throws PDFNetException {
        if (pdfDoc == null) {
            return new ArrayList<>();
        }
        ArrayList<PdfLayer> result = new ArrayList<>();
        if (pdfDoc.hasOC()) {
            Config config = pdfDoc.getOCGConfig();
            Obj ocgs = config.getOrder();
            if (ocgs != null) {
                buildLayersTree(ocgs, pdfViewCtrl, config, result, 0, null);
            }
        }
        return result;
    }

    private static void buildLayersTree(Obj ocglayersArray, PDFViewCtrl pdfViewCtrl, Config config,
            ArrayList<PdfLayer> pdfLayerList, int level, PdfLayer parent) throws PDFNetException {
        for (int i = 0; i < ocglayersArray.size(); ++i) {
            Obj layer = ocglayersArray.getAt(i);
            // ocg layer with children
            if (layer.isArray() && layer.size() > 0) {
                //parent ocg layer which is located in the first index of children array
                if (layer.getAt(0).isString()) {
                    Group ocg = new Group(layer.getAt(0));
                    pdfLayerList.add(new PdfLayer(ocg, layer.getAt(0).getAsPDFText(), null,
                            false, level, parent));
                }
                PdfLayer lastLayer = pdfLayerList.get(pdfLayerList.size() - 1);
                //Build children list of current ocg layer
                buildLayersTree(layer, pdfViewCtrl, config, lastLayer.getChildren(), level + 1, lastLayer);
            }
            // normal ocg layer with a title and without children
            else if (!layer.isString()) {
                Group ocg = new Group(layer);
                if (ocg.isValid()) {
                    pdfLayerList.add(new PdfLayer(ocg, ocg.getName(), getChecked(pdfViewCtrl, ocg),
                            ocg.isLocked(config), level, parent));
                }
            }
        }
    }

    /**
     * Gets the checked state of the OCG layer group.
     *
     * @param pdfViewCtrl the {@link PDFViewCtrl}
     * @param group       the {@link PDFDoc}
     * @return true if OCG layer group is checked, false otherwise.
     * @throws PDFNetException
     */
    public static boolean getChecked(@Nullable PDFViewCtrl pdfViewCtrl, @Nullable Group group) throws PDFNetException {
        if (pdfViewCtrl == null || group == null) {
            return false;
        }
        Context ctx = pdfViewCtrl.getOCGContext();
        return ctx.getState(group);
    }

    /**
     * Sets the checked state of the OCG layer group.
     *
     * @param pdfViewCtrl the {@link PDFViewCtrl}
     * @param group       the OCG group
     * @param checked     true if layer is checked, false otherwise
     * @throws PDFNetException
     */
    public static void setLayerCheckedChange(@Nullable PDFViewCtrl pdfViewCtrl, @Nullable Group group, boolean checked) throws PDFNetException {
        if (pdfViewCtrl == null || group == null) {
            return;
        }
        Context ctx = pdfViewCtrl.getOCGContext();
        ctx.setState(group, checked);
        pdfViewCtrl.update(true);
    }

    public static boolean hasPdfLayer(PDFDoc doc) {
        boolean shouldUnlockRead = false;
        try {
            doc.lockRead();
            shouldUnlockRead = true;
            return doc.hasOC();
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                Utils.unlockReadQuietly(doc);
            }
        }
        return false;
    }
}
