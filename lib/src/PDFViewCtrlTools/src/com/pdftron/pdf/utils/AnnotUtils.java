//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.pdftron.common.PDFNetException;
import com.pdftron.fdf.FDFDoc;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.ColorSpace;
import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementReader;
import com.pdftron.pdf.ElementWriter;
import com.pdftron.pdf.Field;
import com.pdftron.pdf.Font;
import com.pdftron.pdf.GState;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFDraw;
import com.pdftron.pdf.PDFNet;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageIterator;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.QuadPoint;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.Redactor;
import com.pdftron.pdf.annots.FileAttachment;
import com.pdftron.pdf.annots.FreeText;
import com.pdftron.pdf.annots.Ink;
import com.pdftron.pdf.annots.Line;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.annots.PolyLine;
import com.pdftron.pdf.annots.Polygon;
import com.pdftron.pdf.annots.Popup;
import com.pdftron.pdf.annots.Redaction;
import com.pdftron.pdf.annots.RubberStamp;
import com.pdftron.pdf.annots.SignatureWidget;
import com.pdftron.pdf.annots.Square;
import com.pdftron.pdf.annots.Text;
import com.pdftron.pdf.annots.Widget;
import com.pdftron.pdf.asynctask.CreateBitmapFromCustomStampTask;
import com.pdftron.pdf.model.AnnotReviewState;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.model.FreeTextCacheStruct;
import com.pdftron.pdf.model.LineEndingStyle;
import com.pdftron.pdf.model.LineStyle;
import com.pdftron.pdf.model.RulerItem;
import com.pdftron.pdf.model.ShapeBorderStyle;
import com.pdftron.pdf.model.StandardStampOption;
import com.pdftron.pdf.model.StandardStampPreviewAppearance;
import com.pdftron.pdf.tools.AnnotManager;
import com.pdftron.pdf.tools.CountMeasurementCreateTool;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.Signature;
import com.pdftron.pdf.tools.SoundCreate;
import com.pdftron.pdf.tools.Stamper;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.widget.AutoScrollEditText;
import com.pdftron.pdf.widget.AutoScrollEditor;
import com.pdftron.pdf.widget.richtext.PTRichEditor;
import com.pdftron.sdf.DictIterator;
import com.pdftron.sdf.Obj;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;

import static com.pdftron.pdf.tools.TextFieldCreate.BACKGROUND_ALPHA;
import static com.pdftron.richeditor.Constants.ZERO_WIDTH_SPACE_STR_ESCAPE;

/**
 * A utility class for handling annotation.
 */
public class AnnotUtils {

    public static String KEY_InReplyTo = "IRT";
    public static String KEY_ReplyTo = "RT";
    public static String KEY_NM = "NM";
    public static String VALUE_Group = "Group";
    public static String KEY_RichContent = "RC";
    public static String KEY_RawRichContent = "rawRC"; // custom PDFTron attribute
    public static String KEY_FreeTextDate = "pdftron_freetext_date"; // custom PDFTron attribute
    public static String KEY_FreeTextFill = "pdftron_freetext_fill"; // custom PDFTron attribute
    public static String KEY_WidgetAuthor = "Author"; // custom PDFTron attribute
    public static String KEY_UNROTATED_BBOX = "trn-unrotated-rect"; // custom PDFTron attribute
    public static String KEY_ANNOT_ROTATION = "Rotate";

    public static String Key_State = "State";
    public static String Key_StateModel = "StateModel";
    public static String Key_StateModelMarked = "Marked";
    public static String Key_StateModelReview = "Review";
    public static String Key_StateAccepted = "Accepted";
    public static String Key_StateRejected = "Rejected";
    public static String Key_StateCancelled = "Cancelled";
    public static String Key_StateCompleted = "Completed";
    public static String Key_StateNone = "None";

    public static final String KEY_INK_LIST = "InkList";

    public static final String XFDF_ADD = "add";
    public static final String XFDF_MODIFY = "modify";
    public static final String XFDF_DELETE = "delete";

    @Nullable
    public static String getStandardStampImageFileFromName(@NonNull Context context, @NonNull StandardStampPreviewAppearance stampPreviewAppearance, int height) {
        String cacheLabel = stampPreviewAppearance.getText(context);
        if (StandardStampOption.checkStandardStamp(context, cacheLabel)) {
            return StandardStampOption.getStandardStampBitmapPath(context, cacheLabel);
        }
        StandardStampOption stampOption = new StandardStampOption(
                stampPreviewAppearance.getText(context), null,
                stampPreviewAppearance.previewAppearance.bgColorStart,
                stampPreviewAppearance.previewAppearance.bgColorEnd,
                stampPreviewAppearance.previewAppearance.textColor,
                stampPreviewAppearance.previewAppearance.borderColor,
                stampPreviewAppearance.previewAppearance.fillOpacity,
                stampPreviewAppearance.pointLeft, false);

        try {
            Bitmap bitmap = CreateBitmapFromCustomStampTask.createBitmapFromCustomStamp(stampOption, height, height);
            if (bitmap != null) {
                StandardStampOption.saveStandardStamp(context, cacheLabel, stampOption, bitmap);
                return StandardStampOption.getStandardStampBitmapPath(context, cacheLabel);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        return null;
    }

    @Nullable
    public static Bitmap getStandardStampBitmapFromPdf(@Nullable Context context, @NonNull String stampLabel, int bgColor, int height) {
        if (context == null || Utils.isNullOrEmpty(stampLabel)) {
            return null;
        }

        InputStream fis = null;
        PDFDoc template = null;
        PDFDraw pdfDraw = null;
        try {
            fis = context.getResources().openRawResource(R.raw.stamps_icons);
            template = new PDFDoc(fis);

            pdfDraw = new PDFDraw();

            int r = Color.red(bgColor);
            int g = Color.green(bgColor);
            int b = Color.blue(bgColor);
            pdfDraw.setDefaultPageColor((byte) r, (byte) g, (byte) b);

            int pageCount = template.getPageCount();
            int maxWidth = (int) Utils.convDp2Pix(context, 200);
            int marginWidth = (int) Utils.convDp2Pix(context, 175);
            for (int pageNum = 1; pageNum <= pageCount; ++pageNum) {
                if (stampLabel.equals(template.getPageLabel(pageNum).getPrefix())) {
                    Page page = template.getPage(pageNum);
                    int width = (int) Math.min(maxWidth, (height * page.getPageWidth() / page.getPageHeight() + .5));
                    if (width > marginWidth && width < maxWidth) {
                        width = maxWidth;
                    }
                    pdfDraw.setImageSize(width, height, false);
                    return pdfDraw.getBitmap(page);
                }
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            Utils.closeQuietly(template);
            Utils.closeQuietly(fis);
            if (pdfDraw != null) {
                try {
                    pdfDraw.destroy();
                } catch (PDFNetException ignored) {
                }
            }
        }
        return null;
    }

    /**
     * Flattens an annotation. A document lock is required around this method.
     */
    public static Annot flattenAnnot(@NonNull PDFViewCtrl pdfViewCtrl, @NonNull Annot annot, int pageNum) throws PDFNetException {
        PDFDoc pdfDoc = pdfViewCtrl.getDoc();
        // flatten annotation
        Page page = pdfDoc.getPage(pageNum);
        // handle direct object
        Obj annotObj = annot.getSDFObj();
        Obj indirectCopy = annotObj.isIndirect() ? annotObj : pdfDoc.getSDFDoc().importObj(annotObj, false);
        annot.flatten(page);
        // reset viewer state so you can select text after flatten
        pdfViewCtrl.update(true);

        return new Annot(indirectCopy);
    }

    /**
     * Static function to create a screenshot out of the bounds of an annot rect bbox. Note that this requires a read lock on the PDFDoc.
     *
     * @param tempDir temporary directory to store the created PNG file
     * @param pdfDoc  current PDFDoc
     * @param annot   annot to get the screenshot off of
     * @param pageNum current page number of the annot
     * @return the string of the path of the PNG file (in a temp file path)
     * @throws PDFNetException any exception thrown. will be handled somewhere else
     */
    public static String createScreenshot(@NonNull File tempDir, @NonNull PDFDoc pdfDoc, @NonNull Annot annot, int pageNum) {

        String tempFilePath = new File(tempDir, Utils.getScreenshotFileName() + ".png").getAbsolutePath();

        // get border width by getting the borderstyle width
        try {
            Square sq = new Square(annot);
            Rect bbox = sq.getContentRect();
            Annot.BorderStyle sqBorderStyle = sq.getBorderStyle();
            if (sqBorderStyle != null) {
                double borderWidth = sqBorderStyle.getWidth();
                bbox.inflate(borderWidth * -1);
            }
            Page page = pdfDoc.getPage(pageNum);
            PDFDraw draw = new PDFDraw();
            draw.setClipRect(bbox);
            draw.export(page, tempFilePath);
        } catch (PDFNetException e) {
            return null;
        }
        return tempFilePath;
    }

    /**
     * Asyncronous method for creating screenshots, as some complex files might take some time to render.
     *
     * @param tempDir         temporary directory path
     * @param pdfDoc          PDFDoc that contains the annotation
     * @param annot           (square) annot used to create the screenshot with
     * @param annotPageNumber page number that the annotation is on
     * @return loation of the temp screenshot created
     */
    public static Single<String> createScreenshotAsync(final File tempDir, final PDFDoc pdfDoc, final Annot annot, final int annotPageNumber) {
        return Single.create(new SingleOnSubscribe<String>() {
            @Override
            public void subscribe(SingleEmitter<String> emitter) throws Exception {
                String tempPng;
                boolean shouldUnlockRead = false;
                try {
                    pdfDoc.lockRead();
                    shouldUnlockRead = true;
                    tempPng = createScreenshot(tempDir, pdfDoc, annot, annotPageNumber);
                    if (tempPng != null) {
                        emitter.onSuccess(tempPng);
                    } else {
                        emitter.tryOnError(new IllegalStateException("Screenshot creation failed"));
                    }
                } catch (Exception e) {
                    emitter.tryOnError(new IllegalStateException("Screenshot creation failed"));
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } finally {
                    if (shouldUnlockRead) {
                        pdfDoc.unlockRead();
                    }
                }
            }
        });
    }

    public static Single<String> loadSystemFonts() {
        return Single.create(new SingleOnSubscribe<String>() {
            @Override
            public void subscribe(SingleEmitter<String> emitter) throws Exception {
                String fontList = PDFNet.getSystemFontList();
                if (!Utils.isNullOrEmpty(fontList)) {
                    emitter.onSuccess(fontList);
                } else {
                    emitter.tryOnError(new RuntimeException("Unable to get system fonts"));
                }
            }
        });
    }

    @NonNull
    public static String getCurrentTime(@NonNull String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        return dateFormat.format(cal.getTime());
    }

    /**
     * If the sticky note has a custom icon, this will set the appearance of the icon and return true.
     * If the sticky note does not have a custom icon, it will return false.
     *
     * @param context The Context
     * @param annot   The annotation
     * @return True if the sticky note icon has been changed successfully
     */
    private static boolean refreshCustomStickyNoteAppearance(
            @NonNull Context context,
            @NonNull Annot annot) {

        InputStream fis = null;
        PDFDoc template = null;
        ElementReader reader = null;
        ElementWriter writer = null;
        try {
            // get icon name
            Text text = new Text(annot);
            String iconName = text.getIconName();

            // Open pdf containing custom sticky note icons. Each page is a different custom icon
            // with the page label the icon's name.
            fis = context.getResources().openRawResource(R.raw.stickynote_icons);
            template = new PDFDoc(fis);

            // Loop through all pages, checking if the icon name equals the page label name.
            // If none of the page labels equals the icon name, then return false - the sticky note
            // icon is not a custom icon.
            for (int pageNum = 1, pageCount = template.getPageCount(); pageNum <= pageCount; ++pageNum) {
                if (iconName.equalsIgnoreCase(template.getPageLabel(pageNum).getPrefix())) {
                    Page iconPage = template.getPage(pageNum);
                    com.pdftron.sdf.Obj contents = iconPage.getContents();
                    com.pdftron.sdf.Obj importedContents = annot.getSDFObj().getDoc().importObj(contents, true);
                    com.pdftron.pdf.Rect bbox = iconPage.getMediaBox();
                    importedContents.putRect("BBox", bbox.getX1(), bbox.getY1(), bbox.getX2(), bbox.getY2());
                    importedContents.putName("Subtype", "Form");
                    importedContents.putName("Type", "XObject");
                    reader = new ElementReader();
                    writer = new ElementWriter();
                    reader.begin(importedContents);
                    writer.begin(importedContents, true);
                    ColorPt rgbColor = text.getColorAsRGB();
                    double opacity = text.getOpacity();
                    for (Element element = reader.next(); element != null; element = reader.next()) {
                        if (element.getType() == Element.e_path && !element.isClippingPath()) {
                            element.getGState().setFillColorSpace(ColorSpace.createDeviceRGB());
                            element.getGState().setFillColor(rgbColor);
                            element.getGState().setFillOpacity(opacity);
                            element.getGState().setStrokeOpacity(opacity);
                            element.setPathStroke(true);
                            element.setPathFill(true);
                        }
                        writer.writeElement(element);
                    }
                    reader.end();
                    writer.end();

                    // set the appearance of sticky note icon to the custom icon
                    text.setAppearance(importedContents);

                    // keep analytics for the icon
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.CATEGORY_ANNOTATIONTOOLBAR,
                            "sticky note icon: " + iconName, AnalyticsHandlerAdapter.LABEL_STYLEEDITOR);
                    return true;
                }
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.destroy();
                } catch (Exception ignored) {

                }
            }
            if (writer != null) {
                try {
                    writer.destroy();
                } catch (Exception ignored) {

                }
            }
            Utils.closeQuietly(template);
            Utils.closeQuietly(fis);
        }

        return false;
    }

    /**
     * If the sticky note has a custom icon, this will set the appearance of the icon and return true.
     * If the sticky note does not have a custom icon, it will return false.
     *
     * @param context The Context
     * @param annot   The annotation
     * @return True if the sticky note icon has been changed successfully
     */
    private static boolean refreshCustomStampAppearance(
            @NonNull Context context,
            @NonNull Annot annot) {

        InputStream fis = null;
        PDFDoc template = null;
        try {
            // get icon name
            RubberStamp stamp = new RubberStamp(annot);
            String iconName = stamp.getIconName();

            // Open pdf containing custom rubber stamp icons. Each page is a different custom icon
            // with the page label the icon's name.
            fis = context.getResources().openRawResource(R.raw.stamps_icons);
            template = new PDFDoc(fis);

            // Loop through all pages, checking if the icon name equals the page label name.
            // If none of the page labels equals the icon name, then return false - the rubber stamp
            // icon is not a custom icon.
            for (int pageNum = 1, pageCount = template.getPageCount(); pageNum <= pageCount; ++pageNum) {
                if (iconName.equalsIgnoreCase(template.getPageLabel(pageNum).getPrefix())) {
                    Page iconPage = template.getPage(pageNum);
                    com.pdftron.sdf.Obj contents = iconPage.getContents();
                    com.pdftron.sdf.Obj importedContents = annot.getSDFObj().getDoc().importObj(contents, true);
                    com.pdftron.pdf.Rect bbox = iconPage.getMediaBox();
                    importedContents.putRect("BBox", bbox.getX1(), bbox.getY1(), bbox.getX2(), bbox.getY2());
                    importedContents.putName("Subtype", "Form");
                    importedContents.putName("Type", "XObject");

                    // insert background color
                    com.pdftron.sdf.Obj res = iconPage.getResourceDict();
                    if (res != null) {
                        com.pdftron.sdf.Obj importedRes = annot.getSDFObj().getDoc().importObj(res, true);
                        importedContents.put("Resources", importedRes);
                    }

                    // set the appearance of rubber stamp icon to the custom icon
                    stamp.setAppearance(importedContents);

                    // keep analytics for the icon
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.CATEGORY_ANNOTATIONTOOLBAR,
                            "rubber stamp icon: " + iconName, AnalyticsHandlerAdapter.LABEL_STYLEEDITOR);
                    return true;
                }
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            Utils.closeQuietly(template);
            Utils.closeQuietly(fis);
        }

        return false;
    }

    public static boolean refreshCustomFreeTextAppearance(
            @NonNull File appearance,
            @NonNull Annot annot) {

        PDFDoc template = null;
        try {
            // get icon name
            FreeText freeText = new FreeText(annot);
            template = new PDFDoc(appearance.getAbsolutePath());
            Page editTextPage = template.getPage(1);
            com.pdftron.sdf.Obj contents = editTextPage.getContents();
            com.pdftron.sdf.Obj importedContents = annot.getSDFObj().getDoc().importObj(contents, true);
            com.pdftron.pdf.Rect bbox = editTextPage.getMediaBox();
            importedContents.putRect("BBox", bbox.getX1(), bbox.getY1(), bbox.getX2(), bbox.getY2());
            importedContents.putName("Subtype", "Form");
            importedContents.putName("Type", "XObject");

            // insert background color
            com.pdftron.sdf.Obj res = editTextPage.getResourceDict();
            if (res != null) {
                com.pdftron.sdf.Obj importedRes = annot.getSDFObj().getDoc().importObj(res, true);
                importedContents.put("Resources", importedRes);
            }

            // set the appearance
            freeText.setAppearance(importedContents);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            Utils.closeQuietly(template);
        }

        return false;
    }

    /**
     * helper function to refresh annotation appearance. Note that some
     * annotations may have their own custom appearance such as Text annotation.
     *
     * @param context The Context
     * @param annot   The annotation
     * @throws PDFNetException PDFNet exception
     */
    public static void refreshAnnotAppearance(
            @NonNull Context context,
            @NonNull Annot annot)
            throws PDFNetException {

        switch (annot.getType()) {
            case Annot.e_Ink:
                if (!PressureInkUtils.refreshCustomInkAppearanceForExistingAnnot(annot)) {
                    annot.refreshAppearance();
                }
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT:
            case Annot.e_Text:
                // if the icon is not a custom icon, then call refresh appearance.
                // if the icon is a custom icon, refreshCustomStickyNoteAppearance will set the appearance
                if (!refreshCustomStickyNoteAppearance(context, annot)) {
                    annot.refreshAppearance();
                }
                break;
            case Annot.e_Stamp:
                if (!refreshCustomStampAppearance(context, annot)) {
                    annot.refreshAppearance();
                }
                break;
            default:
                annot.refreshAppearance();
        }
    }

    /**
     * Applies custom appearance for a FreeText with custom spacing annotation.
     * Note: appearance will only be applied for Android Lollipop and above.
     * A write lock is expected around this call
     *
     * @param pdfViewCtrl the viewer
     * @param editText    the EditText where appearance is from
     * @param annot       the annotation to apply the appearance to
     * @param pageNum     the page number of the annot
     */
    public static void applyCustomFreeTextAppearance(@NonNull PDFViewCtrl pdfViewCtrl,
            @NonNull AutoScrollEditText editText, @NonNull Annot annot, int pageNum) throws PDFNetException {
        if (Utils.isLollipop()) {
            editText.removeSpacingHandle();
            float spacing = editText.getLetterSpacing();
            Rect bbox = editText.getBoundingRect();
            AnnotUtils.createCustomFreeTextAppearance(
                    editText,
                    pdfViewCtrl,
                    annot,
                    pageNum,
                    bbox
            );
            annot.setCustomData(AnnotUtils.KEY_FreeTextFill, String.valueOf(spacing));
        }
    }

    /**
     * Applies custom appearance for a FreeText annotation
     * that is generated from Android EditText
     * Note 1: This API only takes effect on KitKit and above
     * Note 2: A write lock is expected around this call
     *
     * @param view        the EditText where appearance is from
     * @param pdfViewCtrl the viewer
     * @param annot       the annotation to apply the appearance to
     * @param pageNum     the page number of the annot
     * @param bbox        when a bbox is supplied, this will be used as bbox of the annotation
     */
    public static void createCustomFreeTextAppearance(View view, final PDFViewCtrl pdfViewCtrl,
            Annot annot, final int pageNum, @Nullable final Rect bbox) throws PDFNetException {
        createCustomFreeTextAppearance(view, pdfViewCtrl, annot, pageNum, bbox, false);
    }

    /**
     * Applies custom appearance for a FreeText annotation
     * that is generated from Android EditText
     * Note 1: This API only takes effect on KitKit and above
     * Note 2: A write lock is expected around this call
     *
     * @param view            the EditText where appearance is from
     * @param pdfViewCtrl     the viewer
     * @param annot           the annotation to apply the appearance to
     * @param pageNum         the page number of the annot
     * @param bbox            when a bbox is supplied, this will be used as bbox of the annotation
     * @param useBBoxLocation when true, the bbox location will be used as the bbox of the annotation,
     *                        otherwise, only width/height will be used
     */
    public static void createCustomFreeTextAppearance(View view, final PDFViewCtrl pdfViewCtrl,
            Annot annot, final int pageNum, @Nullable final Rect bbox, boolean useBBoxLocation) throws PDFNetException {
        createCustomFreeTextAppearance(view, pdfViewCtrl, annot, pageNum, bbox, useBBoxLocation, null);
    }

    public static void createCustomFreeTextAppearance(View view, final PDFViewCtrl pdfViewCtrl,
            Annot annot, final int pageNum, @Nullable final Rect bbox, boolean useBBoxLocation, @Nullable AnnotStyle annotStyle) throws PDFNetException {
        if (!Utils.isKitKat()) {
            return;
        }
        if (null == view || null == pdfViewCtrl || null == annot) {
            return;
        }

        boolean cropRequired = false;
        if (view instanceof EditText) {
            ((EditText) view).clearFocus();
            ((EditText) view).setCursorVisible(false);
            ((EditText) view).clearComposingText();
            ((EditText) view).setVerticalScrollBarEnabled(false);
            if (view.getScrollX() > 0 || view.getScrollY() > 0) {
                // when a view contains scroll offset, the created PDF also shifts
                // here we want to correct it
                cropRequired = true;
            }
        }
        boolean rtl = false;
        Rect rtlBBox = null;
        if (bbox != null && useBBoxLocation) {
            // we only care about RTL if we are using edit text bbox
            if (view instanceof AutoScrollEditText) {
                rtl = ((AutoScrollEditText) view).getIsRTL();
                if (rtl) {
                    rtlBBox = ((AutoScrollEditText) view).getBoundingRect();
                }
            }
        }
        // create appearance
        final FreeText freeText = new FreeText(annot);

        Rect tempRect = bbox != null ? new Rect(bbox.getX1(), bbox.getY1(), bbox.getX2(), bbox.getY2()) : null;
        // if view is scrolled, we will need to capture the whole view then crop it
        if (bbox != null && cropRequired) {
            // capture the whole width and height of the view
            tempRect = new Rect(view.getLeft(), view.getTop(),
                    view.getScrollX() + view.getWidth(),
                    view.getScrollY() + view.getHeight());
        }

        final Rect viewRect = tempRect == null ? trimView(view) : tempRect;
        viewRect.normalize();
        if (annotStyle != null && view instanceof AutoScrollEditText) {
            // we need to calculate the actual size based thickness padding
            ((AutoScrollEditText) view).updateThickness(annotStyle.getThickness());
            int paddingRight = ((AutoScrollEditText) view).getPaddingRight();
            int paddingBottom = ((AutoScrollEditText) view).getPaddingBottom();
            viewRect.setX2(viewRect.getX2() + paddingRight);
            viewRect.setY2(viewRect.getY2() + paddingBottom);

            // apply styles
            ((AutoScrollEditText) view).setDrawBackground(true);
            ((AutoScrollEditText) view).setSize(Math.round(viewRect.getWidth()),
                    Math.round(viewRect.getHeight()));
            ((AutoScrollEditText) view).updateFillColor(annotStyle.getFillColor());
            ((AutoScrollEditText) view).updateOpacity(annotStyle.getOpacity());
            ((AutoScrollEditText) view).updateThickness(annotStyle.getThickness());
            ((AutoScrollEditText) view).updateColor(annotStyle.getColor());
        }

        // calculate new bounding box
        // here we will use screen points to make sure rotation is properly counted for
        Rect newBBoxScreen = pdfViewCtrl.getScreenRectForAnnot(freeText, pageNum);
        newBBoxScreen.normalize();

        Rect cropScreenRect = null;
        if (cropRequired) {
            cropScreenRect = new Rect(view.getScrollX(), view.getScrollY(),
                    view.getScrollX() + view.getWidth(),
                    view.getScrollY() + view.getHeight());
        }

        if (cropScreenRect != null) {
            newBBoxScreen.setX2(newBBoxScreen.getX1() + cropScreenRect.getWidth());
            newBBoxScreen.setY2(newBBoxScreen.getY1() + cropScreenRect.getHeight());
        } else {
            if (bbox != null && useBBoxLocation) {
                // use bbox directly
                newBBoxScreen.set(viewRect.getX1(), viewRect.getY1(), viewRect.getX2(), viewRect.getY2());
            } else {
                newBBoxScreen.setX2(newBBoxScreen.getX1() + viewRect.getWidth());
                newBBoxScreen.setY2(newBBoxScreen.getY1() + viewRect.getHeight());
            }
        }

        Rect newBBox = getPageRectFromScreenRect(pdfViewCtrl, newBBoxScreen, pageNum, freeText.getRotation());
        Rect unrotatedBBox = getUnrotatedBBox(freeText);
        if (unrotatedBBox != null) {
            newBBox = unrotatedBBox;
        }
        newBBox.normalize();

        int width = (int) (viewRect.getWidth() + 0.5);
        int height = (int) (viewRect.getHeight() + 0.5);
        if (rtl) {
            // for RTL, we will first obtain a big PDF with entire view, then crop it after
            width = view.getWidth();
            height = view.getHeight();
        }
        File ret = createPdfFromView(view,
                width,
                height,
                new File(view.getContext().getCacheDir(), "rc-FreeText.pdf")

        );
        if (ret != null) {
            // crop for rtl
            if (rtl && rtlBBox != null) {
                try {
                    PDFDoc pdfDoc = new PDFDoc(ret.getAbsolutePath());

                    Page page = pdfDoc.getPage(1);
                    Rect currentCropbox = page.getCropBox();

                    // pdf coordinate origin is bottom left
                    Rect newCropBox = new Rect(rtlBBox.getX1(), currentCropbox.getY2() - rtlBBox.getY2(),
                            rtlBBox.getX2(), currentCropbox.getY2());
                    page.setCropBox(newCropBox);
                    page.setMediaBox(newCropBox);
                    pdfDoc.save();

                    pdfDoc.close();
                } catch (Exception ignored) {

                }
            }
            // crop for scroll offset
            if (cropScreenRect != null) {
                try {
                    PDFDoc pdfDoc = new PDFDoc(ret.getAbsolutePath());

                    Page page = pdfDoc.getPage(1);
                    Rect currentCropBox = page.getCropBox();
                    Rect newCropBox = new Rect(currentCropBox.getX1(), currentCropBox.getY1(),
                            currentCropBox.getX1() + cropScreenRect.getWidth(),
                            currentCropBox.getY1() + cropScreenRect.getHeight());
                    page.setCropBox(newCropBox);
                    page.setMediaBox(newCropBox);
                    pdfDoc.save();

                    pdfDoc.close();
                } catch (Exception ignored) {

                }
            }

            AnnotUtils.refreshCustomFreeTextAppearance(ret, freeText);
            int baseRotation = getAnnotBaseRotation(pdfViewCtrl, pageNum); // multiple of 90
            if (baseRotation != 0) {
                // we need to rotate the 90-multiples first to get the bbox right
                freeText.rotateAppearance(baseRotation);
            }
            freeText.resize(newBBox);
            if (AnnotUtils.hasRotation(freeText)) {
                // rotate the remaining arbitrary degrees
                int annotRotation = getAnnotRotation(freeText);
                freeText.rotateAppearance(annotRotation - baseRotation);
            }
        }
    }

    /**
     * Creates the FreeText appearance from {@link PTRichEditor}
     * This method does not lock document, a write lock should be acquired outside
     */
    public static void createRCFreeTextAppearance(PTRichEditor richEditor, final PDFViewCtrl pdfViewCtrl,
            Annot annot, final int pageNum,
            @Nullable AnnotStyle annotStyle) throws PDFNetException {
        if (null == richEditor || null == pdfViewCtrl || null == annot) {
            return;
        }
        // create appearance
        final FreeText freeText = new FreeText(annot);
        createCustomFreeTextAppearance(richEditor, pdfViewCtrl, annot, pageNum, null, false, annotStyle);

        // create HTML
        final String rawHtml = richEditor.getHtml();
        freeText.setCustomData(AnnotUtils.KEY_RawRichContent, rawHtml);

        // format HTML

        final String plainText = richEditor.getText().toString();
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?><body xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:xfa=\"http://www.xfa.org/schema/xfa-data/1.0/\" xfa:APIVersion=\"Acrobat:10.1.3\" xfa:spec=\"2.0.2\">");
        String editTextHtml = Html.toHtml(richEditor.getEditableText());
        sb.append(editTextHtml);
        sb.append("</body>");
        String htmlContent = sb.toString().replaceAll(ZERO_WIDTH_SPACE_STR_ESCAPE, "");

        freeText.getSDFObj().putString(AnnotUtils.KEY_RichContent, htmlContent);
        freeText.getSDFObj().putString(Tool.PDFTRON_ID, "");
        freeText.setContents(plainText);
    }

    private static Rect trimView(View view) throws PDFNetException {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        Bitmap trimmed = createTrimmedBitmap(bitmap);
        return new Rect(0, 0, trimmed.getWidth(), trimmed.getHeight());
    }

    private static Bitmap createTrimmedBitmap(Bitmap bmp) {
        int imgHeight = bmp.getHeight();
        int imgWidth = bmp.getWidth();
        int smallX = 0, largeX = imgWidth, smallY = 0, largeY = imgHeight;
        int left = imgWidth, right = imgWidth, top = imgHeight, bottom = imgHeight;
        for (int i = 0; i < imgWidth; i++) {
            for (int j = 0; j < imgHeight; j++) {
                if (bmp.getPixel(i, j) != Color.TRANSPARENT) {
                    if ((i - smallX) < left) {
                        left = (i - smallX);
                    }
                    if ((largeX - i) < right) {
                        right = (largeX - i);
                    }
                    if ((j - smallY) < top) {
                        top = (j - smallY);
                    }
                    if ((largeY - j) < bottom) {
                        bottom = (largeY - j);
                    }
                }
            }
        }
        left = 0;
        top = 0;
        bmp = Bitmap.createBitmap(bmp, left, top, imgWidth - left - right, imgHeight - top - bottom);

        return bmp;
    }

    private static void removeWhiteFillForElement(ElementWriter writer, ElementReader reader) throws PDFNetException {
        Element element;
        while ((element = reader.next()) != null) {
            switch (element.getType()) {
                case Element.e_path: {
                    GState gs = element.getGState();
                    // fill
                    ColorPt fillColor = gs.getFillColor();
                    ColorSpace fillCS = gs.getFillColorSpace();
                    ColorPt fillColorRgb = fillCS.convert2RGB(fillColor);
                    if (fillColorRgb.get(0) == 1.0 && fillColorRgb.get(1) == 1.0 && fillColorRgb.get(2) == 1.0) {
                        element.setPathFill(false);
                    }
                    writer.writeElement(element);
                }
                break;
                default:
                    writer.writeElement(element);
                    break;
            }
        }
    }

    private static int convPixelToPoint(double pixel) {
        return (int) (pixel * 72 / 96 + 0.5);
    }

    /**
     * Attempts to create a new single page PDF document using the appearance from an Android view.
     * Returns the output file if successfully created. If a file already exists, it will get overwritten.
     *
     * @param content    view used to create a PDF document
     * @param width      width of the document
     * @param height     height of the document
     * @param outputFile output PDF file to write to, this file will be overwritten.
     * @return the converted output PDF file, or null if the process failed.
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Nullable
    public static File createPdfFromView(@NonNull View content, int width, int height, @NonNull File outputFile) {
        if (!Utils.isKitKat()) {
            return null;
        }
        try {
            // create a new document
            PdfDocument document = new PdfDocument();

            PdfDocument.PageInfo pageInfo = (new PdfDocument.PageInfo.Builder(
                    width,
                    height, 1)).create();

            // start a page
            PdfDocument.Page page = document.startPage(pageInfo);

            // draw something on the page
            content.draw(page.getCanvas());

            // finish the page
            document.finishPage(page);

            // write the document content
            outputFile.createNewFile();
            try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
                document.writeTo(fileOutputStream);
            }

            // close the document
            document.close();

            return outputFile;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Gets the page corresponding to a certain stamp in our stamps repository.
     *
     * @param context   The context
     * @param stampName The name of stamp icon
     * @return The page size; null if cannot find the stamp
     */
    @Nullable
    public static double[] getStampSize(
            @NonNull Context context,
            @NonNull String stampName) {

        InputStream fis = null;
        PDFDoc template = null;
        try {
            fis = context.getResources().openRawResource(R.raw.stamps_icons);
            template = new PDFDoc(fis);

            for (int pageNum = 1, pageCount = template.getPageCount(); pageNum <= pageCount; ++pageNum) {
                if (stampName.equalsIgnoreCase(template.getPageLabel(pageNum).getPrefix())) {
                    Page page = template.getPage(pageNum);
                    double[] size = new double[2];
                    size[0] = page.getPageWidth();
                    size[1] = page.getPageHeight();
                    return size;
                }
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            Utils.closeQuietly(template);
            Utils.closeQuietly(fis);
        }

        return null;
    }

    /**
     * Returns annotation type as string.
     *
     * @param context The context
     * @param typeId  The annotation type ID
     * @return The annotation type as string
     */
    static public String getAnnotTypeAsString(
            @NonNull Context context,
            int typeId) {

        switch (typeId) {
            case Annot.e_Text:
                return context.getResources().getString(R.string.annot_text).toLowerCase();
            case Annot.e_Link:
                return context.getResources().getString(R.string.annot_link).toLowerCase();
            case Annot.e_FreeText:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_DATE:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_SPACING:
                return context.getResources().getString(R.string.annot_free_text).toLowerCase();
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT:
                return context.getResources().getString(R.string.annot_callout).toLowerCase();
            case Annot.e_Line:
                return context.getResources().getString(R.string.annot_line).toLowerCase();
            case Annot.e_Square:
                return context.getResources().getString(R.string.annot_square).toLowerCase();
            case Annot.e_Circle:
                return context.getResources().getString(R.string.annot_circle).toLowerCase();
            case Annot.e_Polygon:
                return context.getResources().getString(R.string.annot_polygon).toLowerCase();
            case Annot.e_Polyline:
                return context.getResources().getString(R.string.annot_polyline).toLowerCase();
            case Annot.e_Highlight:
                return context.getResources().getString(R.string.annot_highlight).toLowerCase();
            case Annot.e_Underline:
                return context.getResources().getString(R.string.annot_underline).toLowerCase();
            case Annot.e_Squiggly:
                return context.getResources().getString(R.string.annot_squiggly).toLowerCase();
            case Annot.e_StrikeOut:
                return context.getResources().getString(R.string.annot_strikeout).toLowerCase();
            case Annot.e_Stamp:
                return context.getResources().getString(R.string.annot_stamp).toLowerCase();
            case Annot.e_Caret:
                return context.getResources().getString(R.string.annot_caret).toLowerCase();
            case Annot.e_Ink:
                return context.getResources().getString(R.string.annot_ink).toLowerCase();
            case Annot.e_Redact:
                return context.getResources().getString(R.string.annot_redaction).toLowerCase();
            case AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE:
                return context.getResources().getString(R.string.annot_signature).toLowerCase();
            case AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW:
                return context.getResources().getString(R.string.annot_arrow).toLowerCase();
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RULER:
                return context.getResources().getString(R.string.annot_ruler).toLowerCase();
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_HIGHLIGHTER:
                return context.getResources().getString(R.string.annot_free_highlight).toLowerCase();
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD:
                return context.getResources().getString(R.string.annot_cloud).toLowerCase();
            case Annot.e_FileAttachment:
                return context.getResources().getString(R.string.annot_file_attachment).toLowerCase();
            case Annot.e_Sound:
                return context.getResources().getString(R.string.annot_sound).toLowerCase();
            case AnnotStyle.CUSTOM_ANNOT_TYPE_PERIMETER_MEASURE:
                return context.getResources().getString(R.string.annot_perimeter_measure).toLowerCase();
            case AnnotStyle.CUSTOM_ANNOT_TYPE_AREA_MEASURE:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RECT_AREA_MEASURE:
                return context.getResources().getString(R.string.annot_area_measure).toLowerCase();
            case Annot.e_Popup:
            case Annot.e_Movie:
            case Annot.e_Widget:
            case Annot.e_Screen:
            case Annot.e_PrinterMark:
            case Annot.e_TrapNet:
            case Annot.e_Watermark:
            case Annot.e_3D:
            case Annot.e_Projection:
            case Annot.e_RichMedia:
            default:
                return context.getResources().getString(R.string.annot_misc).toLowerCase();
        }
    }

    /**
     * Returns annotation type as string.
     *
     * @param context The context
     * @param annot   The annotation
     * @return The annotation type as string
     */
    static public String getAnnotTypeAsString(
            @NonNull Context context,
            @NonNull Annot annot)
            throws PDFNetException {

        int typeId = getAnnotType(annot);
        return getAnnotTypeAsString(context, typeId);
    }

    public static int getAnnotType(@NonNull Annot annot) throws PDFNetException {
        int typeId = annot.getType();
        switch (typeId) {
            case Annot.e_Text: {
                if (isCountMeasurement(annot)) {
                    return AnnotStyle.CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT;
                }
            }
            case Annot.e_Line:
                if (isRuler(annot)) {
                    return AnnotStyle.CUSTOM_ANNOT_TYPE_RULER;
                } else if (isArrow(annot)) {
                    return AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW;
                }
            case Annot.e_Polyline:
                if (isPerimeterMeasure(annot)) {
                    return AnnotStyle.CUSTOM_ANNOT_TYPE_PERIMETER_MEASURE;
                }
            case Annot.e_Polygon:
                if (isCloud(annot)) {
                    return AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD;
                } else if (isAreaMeasure(annot)) {
                    if (isRectAreaMeasure(annot)) {
                        return AnnotStyle.CUSTOM_ANNOT_TYPE_RECT_AREA_MEASURE;
                    }
                    return AnnotStyle.CUSTOM_ANNOT_TYPE_AREA_MEASURE;
                }
            case Annot.e_Ink:
                if (isFreeHighlighter(annot)) {
                    return AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_HIGHLIGHTER;
                }
            case Annot.e_FreeText:
                if (isCallout(annot)) {
                    return AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT;
                } else if (isFreeTextDate(annot)) {
                    return AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_DATE;
                } else if (isFreeTextSpacing(annot)) {
                    return AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_SPACING;
                }
            case Annot.e_Stamp:
                if (isSignature(annot)) {
                    return AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE;
                }
        }
        return annot.getType();
    }

    public static LineEndingStyle getLineEndingStyle(@NonNull Line line, boolean lineStart) throws PDFNetException {
        int endingStyle = lineStart ? line.getStartStyle() : line.getEndStyle();
        switch (endingStyle) {
            case Line.e_Butt:
                return LineEndingStyle.BUTT;
            case Line.e_Diamond:
                return LineEndingStyle.DIAMOND;
            case Line.e_Circle:
                return LineEndingStyle.CIRCLE;
            case Line.e_OpenArrow:
                return LineEndingStyle.OPEN_ARROW;
            case Line.e_ClosedArrow:
                return LineEndingStyle.CLOSED_ARROW;
            case Line.e_ROpenArrow:
                return LineEndingStyle.R_OPEN_ARROW;
            case Line.e_RClosedArrow:
                return LineEndingStyle.R_CLOSED_ARROW;
            case Line.e_Slash:
                return LineEndingStyle.SLASH;
            case Line.e_Square:
                return LineEndingStyle.SQUARE;
            case Line.e_None:
            default:
                return LineEndingStyle.NONE;
        }
    }

    /**
     * Gets the annot style
     * A read lock is required for this function.
     *
     * @param annot the annotation
     * @return the annot style
     * @throws PDFNetException PDFNet exception
     */
    public static AnnotStyle getAnnotStyle(@NonNull Annot annot) throws PDFNetException {
        int annotType = annot.getType();
        boolean annotIsSticky = (annotType == Annot.e_Text);
        boolean annotIsFreeText = AnnotStyle.isFreeTextGroup(annotType);
        boolean annotIsWidget = annotType == Annot.e_Widget;

        // thickness
        float thickness = (float) annot.getBorderStyle().getWidth();

        if (thickness == 0) {
            Obj sdfObj = annot.getSDFObj().findObj(Tool.PDFTRON_THICKNESS);
            if (sdfObj != null) {
                thickness = (float) sdfObj.getNumber();
            }
        }

        // stroke color
        ColorPt colorPt;
        int compNum;

        if (annotIsFreeText) {
            FreeText freeText = new FreeText(annot);
            colorPt = freeText.getLineColor();
            compNum = freeText.getLineColorCompNum();
        } else {
            colorPt = annot.getColorAsRGB();
            compNum = annot.getColorCompNum();
        }
        int color = Utils.colorPt2color(colorPt);
        if (compNum == 0) {
            color = Color.TRANSPARENT;
        }

        AnnotStyle annotStyle = new AnnotStyle();

        // get fill color, border style
        int fillColor = Color.TRANSPARENT;
        float opacity = 1.0f;
        if (annot.isMarkup()) {
            Markup m = new Markup(annot);
            opacity = (float) m.getOpacity();

            if (annot.getType() == Annot.e_Square ||
                    annot.getType() == Annot.e_Circle ||
                    annot.getType() == Annot.e_Polygon) {
                int borderEffect = m.getBorderEffect();
                if (borderEffect == Markup.e_Cloudy) {
                    annotStyle.setBorderStyle(ShapeBorderStyle.CLOUDY);
                } else if (annot.getBorderStyle().getStyle() == Annot.BorderStyle.e_dashed) {
                    annotStyle.setBorderStyle(ShapeBorderStyle.DASHED);
                } else {
                    annotStyle.setBorderStyle(ShapeBorderStyle.DEFAULT);
                }
            }

            if (annot.getType() == Annot.e_Line ||
                    annot.getType() == Annot.e_Polyline) {
                if (annot.getBorderStyle().getStyle() == Annot.BorderStyle.e_dashed) {
                    annotStyle.setLineStyle(LineStyle.DASHED);
                } else {
                    annotStyle.setLineStyle(LineStyle.DEFAULT);
                }
            }

            if (annot.getType() == Annot.e_Line ||
                    annot.getType() == Annot.e_Polyline) {
                Line lineAnnot = new Line(annot);
                annotStyle.setLineStartStyle(getLineEndingStyle(lineAnnot, true));
                annotStyle.setLineEndStyle(getLineEndingStyle(lineAnnot, false));
            }

            if (annotIsFreeText) {
                FreeText freeText = new FreeText(annot);
                if (freeText.getColorCompNum() == 3) {
                    ColorPt fillColorPt = freeText.getColorAsRGB();
                    fillColor = Utils.colorPt2color(fillColorPt);
                }
            } else {
                if (m.getInteriorColorCompNum() == 3) {
                    ColorPt fillColorPt = m.getInteriorColor();
                    fillColor = Utils.colorPt2color(fillColorPt);
                }
            }
        }

        if (annotIsWidget && AnnotUtils.isTextField(annot)) {
            Widget widget = new Widget(annot);
            fillColor = Utils.colorPt2color(widget.getBackgroundColor());
        }

        // get icon
        String icon = "";
        if (annotIsSticky) {
            Text t = new Text(annot);
            icon = t.getIconName();
        }

        if (annotType == Annot.e_Sound) {
            icon = SoundCreate.SOUND_ICON;
        }

        annotStyle.setAnnotType(annot.getType());
        annotStyle.setStyle(color, fillColor, thickness, opacity);

        if (!Utils.isNullOrEmpty(icon)) {
            annotStyle.setIcon(icon);
        }

        if (annotType == Annot.e_Circle ||
                annotType == Annot.e_Square ||
                annotType == Annot.e_Line ||
                annotType == Annot.e_Polyline ||
                annotType == Annot.e_Polygon ||
                annotType == Annot.e_Ink ||
                annotType == Annot.e_Text ||
                canUseBitmapAppearance(annot)) {
            // these are annotation types we can draw
            // with exceptions that we will go through below
            annotStyle.setHasAppearance(false);
        }

        if (annotType == Annot.e_Text && AnnotUtils.isCountMeasurement(annot)) {
            annotStyle.setAnnotType(AnnotStyle.CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT);
        }

        // text style
        if (annotIsFreeText) {
            String fontName = "";
            @ColorInt int textColor;
            float textSize;
            FreeText freeText = new FreeText(annot);
            textSize = (float) freeText.getFontSize();
            textColor = Utils.colorPt2color(freeText.getTextColor());
            Obj freeTextObj = freeText.getSDFObj();
            Obj drDict = freeTextObj.findObj("DR");
            Obj dsString = freeTextObj.findObj("DS"); //Default Style
            Obj daString = freeTextObj.findObj("DA"); //Default Appearance

            // DS or "Default Style" should be taken in account first before DR for FreeText annots
            // e.g. "font: Roboto 16pt;color: #000000"
            if (dsString != null && dsString.getAsPDFText() != null) {
                String[] dsArray = dsString.getAsPDFText().trim().split("font:");
                if (dsArray.length >= 2) {
                    String font = dsArray[1]
                            .trim()
                            .split(";")[0]
                            .trim();

                    String[] fontComponents = font.split("\\s");
                    // Two possible cases, either provided as fontName + fontSize
                    // e.g. font: Roboto 16pt;
                    if (!fontComponents[0].matches("^\\d+[a-zA-Z]*")) {
                        fontName = fontComponents[0].trim();
                    } else if (fontComponents.length >= 2) {
                        //or fontSize + fontName
                        // e.g. font: 16pt Roboto;
                        fontName = fontComponents[1].trim();
                    }
                }
            }
            // DA or "Default Appearance" should be taken into account after DS
            // e.g. "1 1 1 RG 1 1 1 rg /DroidSansMono 38 Tf"
            if (Utils.isNullOrEmpty(fontName) && daString != null && daString.getAsPDFText() != null) {
                String[] daArray = daString.getAsPDFText().trim().split("\\s");
                for (int i = 0; i < daArray.length; i++) {
                    // Find "Tf" string and look for 2 elements before, the format is always "/FontName FontSize Tf"
                    if (daArray[i].trim().equalsIgnoreCase("tf") && i >= 2) {
                        // Get rid of beginning slash if present
                        if (daArray[i - 2].charAt(0) == '/') {
                            fontName = daArray[i - 2].substring(1).trim();
                        } else {
                            fontName = daArray[i - 2].trim();
                        }
                    }
                }
            }

            // Use DR as fallback if either of the above are missing
            if (Utils.isNullOrEmpty(fontName) && drDict != null && drDict.isDict()) {
                Obj fontDict = drDict.findObj("Font");
                if (fontDict != null && fontDict.isDict()) {
                    DictIterator fItr = fontDict.getDictIterator();
                    if (fItr.hasNext()) {
                        Font f = new Font(fItr.value());
                        fontName = f.getName();
                    }
                }
            }

            String rawHTML = freeText.getCustomData(KEY_RawRichContent);
            if (!Utils.isNullOrEmpty(rawHTML)) {
                // only handle PDFTron RC
                annotStyle.setTextHTMLContent(rawHTML);
            }

            annotStyle.setFont(new FontResource(fontName));
            annotStyle.setTextColor(textColor);
            if (textSize == 0) {
                textSize = 12;
            }
            annotStyle.setTextSize(textSize);
            int horizontalAlignment = FreeTextAlignmentUtils.getHorizontalAlignment(freeText);
            int verticalAlignment = FreeTextAlignmentUtils.getVerticalAlignment(freeText);
            annotStyle.setHorizontalAlignment(horizontalAlignment);
            annotStyle.setVerticalAlignment(verticalAlignment);
        }

        if (annotIsWidget) {
            @ColorInt int textColor;
            float textSize;
            Widget widget = new Widget(annot);
            textSize = (float) widget.getFontSize();
            textColor = Utils.colorPt2color(widget.getTextColor());
            Font font = widget.getFont();
            String fontName = font != null ? font.getName() : "";

            annotStyle.setFont(new FontResource(fontName));
            annotStyle.setTextColor(textColor);
            if (textSize == 0) {
                textSize = 12;
            }
            annotStyle.setTextSize(textSize);
        }

        if (annotType == Annot.e_Line) {
            if (AnnotUtils.isRuler(annot)) {
                annotType = AnnotStyle.CUSTOM_ANNOT_TYPE_RULER;
                annotStyle.setAnnotType(annotType);
                RulerItem rulerItem = MeasureUtils.getRulerItemFromAnnot(annot);
                if (null != rulerItem) {
                    annotStyle.setRulerItem(rulerItem);
                } else {
                    rulerItem = RulerItem.getRulerItem(annot); // legacy
                    if (null != rulerItem) {
                        annotStyle.setRulerItem(rulerItem);
                    }
                }
            } else if (AnnotUtils.isArrow(annot)) {
                annotType = AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW;
                annotStyle.setAnnotType(annotType);
            }
        }
        if (annotType == Annot.e_Ink) {
            if (AnnotUtils.isFreeHighlighter(annot)) {
                annotType = AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_HIGHLIGHTER;
                annotStyle.setAnnotType(annotType);
                annotStyle.setHasAppearance(false);
            }
        }
        if (annotType == Annot.e_Polyline) {
            if (AnnotUtils.isPerimeterMeasure(annot)) {
                annotType = AnnotStyle.CUSTOM_ANNOT_TYPE_PERIMETER_MEASURE;
                annotStyle.setAnnotType(annotType);
                RulerItem rulerItem = MeasureUtils.getRulerItemFromAnnot(annot);
                if (null != rulerItem) {
                    annotStyle.setRulerItem(rulerItem);
                }
            }
        }
        if (annotType == Annot.e_Polygon) {
            if (AnnotUtils.isCloud(annot)) {
                annotType = AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD;
                annotStyle.setAnnotType(annotType);
                Polygon polygon = new Polygon(annot);
                double intensity = polygon.getBorderEffectIntensity();
                annotStyle.setBorderEffectIntensity(intensity);
            } else if (AnnotUtils.isAreaMeasure(annot)) {
                annotType = AnnotUtils.isRectAreaMeasure(annot) ?
                        AnnotStyle.CUSTOM_ANNOT_TYPE_RECT_AREA_MEASURE : AnnotStyle.CUSTOM_ANNOT_TYPE_AREA_MEASURE;
                annotStyle.setAnnotType(annotType);
                RulerItem rulerItem = MeasureUtils.getRulerItemFromAnnot(annot);
                if (null != rulerItem) {
                    annotStyle.setRulerItem(rulerItem);
                }
            }
        }
        if (annotType == Annot.e_FreeText) {
            if (AnnotUtils.isCallout(annot)) {
                annotType = AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT;
                annotStyle.setAnnotType(annotType);
                annotStyle.setHasAppearance(true);
            } else if (AnnotUtils.isFreeTextDate(annot)) {
                annotType = AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_DATE;
                annotStyle.setAnnotType(annotType);
                annotStyle.setDateFormat(annot.getCustomData(KEY_FreeTextDate));
            } else if (AnnotUtils.isFreeTextSpacing(annot)) {
                annotType = AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_SPACING;
                annotStyle.setAnnotType(annotType);
                String spacing = annot.getCustomData(KEY_FreeTextFill);
                try {
                    float spacingF = Float.parseFloat(spacing);
                    annotStyle.setLetterSpacing(spacingF);
                } catch (Exception ex) {
                    annotStyle.setLetterSpacing(0);
                }
            }
        }
        if (annotType == Annot.e_Redact) {
            Redaction redaction = new Redaction(annot);
            annotStyle.setOverlayText(redaction.getOverlayText());
        }
        if (annotType == Annot.e_Stamp) {
            if (AnnotUtils.isSignature(annot)) {
                annotType = AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE;
                annotStyle.setAnnotType(annotType);
            }
        }

        try {
            // check if border style is not solid, then client side cannot draw properly
            Annot.BorderStyle borderStyle = annot.getBorderStyle();
            if (borderStyle.getStyle() != Annot.BorderStyle.e_solid &&
                    borderStyle.getStyle() != Annot.BorderStyle.e_dashed) {
                annotStyle.setHasAppearance(true);
            }
        } catch (Exception ignored) {
        }

        if (isTextField(annot)) {
            Widget widget = new Widget(annot);
            Field field = widget.getField();
            annotStyle.setTextContent(field.getValueAsString());
        } else {
            annotStyle.setTextContent(annot.getContents());
        }

        return annotStyle;
    }

    /**
     * Returns annotation type as plural string.
     *
     * @param context The context
     * @param typeId  The annotation type ID
     * @return The annotation type as string
     */
    private static String getAnnotTypeAsPluralString(
            @NonNull Context context,
            int typeId) {

        switch (typeId) {
            case Annot.e_Text:
                return context.getResources().getString(R.string.annot_text_plural).toLowerCase();
            case Annot.e_Link:
                return context.getResources().getString(R.string.annot_link_plural).toLowerCase();
            case Annot.e_FreeText:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_DATE:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_SPACING:
                return context.getResources().getString(R.string.annot_free_text_plural).toLowerCase();
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT:
                return context.getResources().getString(R.string.annot_callout_plural).toLowerCase();
            case Annot.e_Line:
                return context.getResources().getString(R.string.annot_line_plural).toLowerCase();
            case Annot.e_Square:
                return context.getResources().getString(R.string.annot_square_plural).toLowerCase();
            case Annot.e_Circle:
                return context.getResources().getString(R.string.annot_circle_plural).toLowerCase();
            case Annot.e_Polygon:
                return context.getResources().getString(R.string.annot_polygon_plural).toLowerCase();
            case Annot.e_Polyline:
                return context.getResources().getString(R.string.annot_polyline).toLowerCase();
            case Annot.e_Highlight:
                return context.getResources().getString(R.string.annot_highlight_plural).toLowerCase();
            case Annot.e_Underline:
                return context.getResources().getString(R.string.annot_underline_plural).toLowerCase();
            case Annot.e_Squiggly:
                return context.getResources().getString(R.string.annot_squiggly_plural).toLowerCase();
            case Annot.e_StrikeOut:
                return context.getResources().getString(R.string.annot_strikeout_plural).toLowerCase();
            case Annot.e_Stamp:
                return context.getResources().getString(R.string.annot_stamp_plural).toLowerCase();
            case Annot.e_Caret:
                return context.getResources().getString(R.string.annot_caret_plural).toLowerCase();
            case Annot.e_Ink:
                return context.getResources().getString(R.string.annot_ink_plural).toLowerCase();
            case Annot.e_Redact:
                return context.getResources().getString(R.string.annot_redaction_plural).toLowerCase();
            case AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE:
                return context.getResources().getString(R.string.annot_signature_plural).toLowerCase();
            case AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW:
                return context.getResources().getString(R.string.annot_arrow_plural).toLowerCase();
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RULER:
                return context.getResources().getString(R.string.annot_ruler_plural).toLowerCase();
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_HIGHLIGHTER:
                return context.getResources().getString(R.string.annot_free_highlight_plural).toLowerCase();
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD:
                return context.getResources().getString(R.string.annot_cloud_plural).toLowerCase();
            case Annot.e_FileAttachment:
                return context.getResources().getString(R.string.annot_file_attachment_plural).toLowerCase();
            case Annot.e_Sound:
                return context.getResources().getString(R.string.annot_sound).toLowerCase();
            case AnnotStyle.CUSTOM_ANNOT_TYPE_PERIMETER_MEASURE:
                return context.getResources().getString(R.string.annot_perimeter_measure).toLowerCase();
            case AnnotStyle.CUSTOM_ANNOT_TYPE_AREA_MEASURE:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RECT_AREA_MEASURE:
                return context.getResources().getString(R.string.annot_area_measure).toLowerCase();
            case Annot.e_Popup:
            case Annot.e_Movie:
            case Annot.e_Widget:
            case Annot.e_Screen:
            case Annot.e_PrinterMark:
            case Annot.e_TrapNet:
            case Annot.e_Watermark:
            case Annot.e_3D:
            case Annot.e_Projection:
            case Annot.e_RichMedia:
            default:
                return context.getResources().getString(R.string.annot_misc_plural).toLowerCase();
        }
    }

    /**
     * Returns annotation type as string.
     *
     * @param context The context
     * @param annot   The annotation
     * @return The annotation type as string
     */
    static public String getAnnotTypeAsPluralString(
            @NonNull Context context,
            @NonNull Annot annot)
            throws PDFNetException {

        int typeId = getAnnotType(annot);
        return getAnnotTypeAsPluralString(context, typeId);
    }

    /**
     * Returns annotation type as raw string (not localized).
     *
     * @param annot The annotation
     * @return The annotation type as string
     */
    static public String getAnnotTypeAsString(
            Annot annot)
            throws PDFNetException {

        int annotType = annot.getType();
        switch (annotType) {
            case Annot.e_Text:
                return "sticky_note";
            case Annot.e_Link:
                return "link";
            case Annot.e_FreeText:
                if (isCallout(annot)) {
                    return "callout";
                }
                return "free_text";
            case Annot.e_Line:
                if (isRuler(annot)) {
                    return "ruler";
                }
                if (isArrow(annot)) {
                    return "arrow";
                }
                return "line";
            case Annot.e_Square:
                return "square";
            case Annot.e_Circle:
                return "circle";
            case Annot.e_Polygon:
                if (isCloud(annot)) {
                    return "cloud";
                }
                return "polygon";
            case Annot.e_Polyline:
                return "polyline";
            case Annot.e_Highlight:
                return "highlight";
            case Annot.e_Underline:
                return "underline";
            case Annot.e_Squiggly:
                return "squiggly";
            case Annot.e_StrikeOut:
                return "strikeout";
            case Annot.e_Stamp:
                return "stamp";
            case Annot.e_Caret:
                return "caret";
            case Annot.e_Ink:
                if (isFreeHighlighter(annot)) {
                    return "free_highlighter";
                }
                return "ink";
            default:
                return "annotation";
        }
    }

    /**
     * Returns annotation type as raw plural string (not localized).
     *
     * @param annot The annotation
     * @return The annotation type as string
     */
    static public String getAnnotTypeAsPluralString(
            Annot annot)
            throws PDFNetException {

        int annotType = annot.getType();
        switch (annotType) {
            case Annot.e_Text:
                return "sticky_notes";
            case Annot.e_Line:
                if (isRuler(annot)) {
                    return "rulers";
                }
                if (isArrow(annot)) {
                    return "arrows";
                }
                return "lines";
            case Annot.e_FreeText:
                if (isCallout(annot)) {
                    return "callouts";
                }
                return "free_texts";
            case Annot.e_Link:
                return "links";
            case Annot.e_Square:
                return "squares";
            case Annot.e_Circle:
                return "circles";
            case Annot.e_Polygon:
                if (isCloud(annot)) {
                    return "clouds";
                }
                return "polygons";
            case Annot.e_Polyline:
                return "polylines";
            case Annot.e_Highlight:
                return "highlights";
            case Annot.e_Underline:
                return "underlines";
            case Annot.e_Squiggly:
                return "squiggles";
            case Annot.e_StrikeOut:
                return "strikeouts";
            case Annot.e_Stamp:
                return "stamps";
            case Annot.e_Caret:
                return "carets";
            case Annot.e_Ink:
                if (isFreeHighlighter(annot)) {
                    return "free_highlighters";
                }
                return "inks";
            default:
                return "annotations";
        }
    }

    /**
     * Checks if the annotation is a line with no ending styles
     * A read lock is expected around this method.
     *
     * @param annot The annotation
     * @return True if the annotation is arrow
     * @throws PDFNetException PDFNet exception
     */
    public static boolean isSimpleLine(
            @NonNull Annot annot)
            throws PDFNetException {

        Line line = new Line(annot);
        return line.isValid() && line.getEndStyle() == Line.e_None && line.getStartStyle() == Line.e_None;
    }

    /**
     * Checks if the annotation is an arrow
     * A read lock is expected around this method.
     *
     * @param annot The annotation
     * @return True if the annotation is arrow
     * @throws PDFNetException PDFNet exception
     */
    public static boolean isArrow(
            @NonNull Annot annot)
            throws PDFNetException {

        Line line = new Line(annot);
        return line.isValid() && line.getEndStyle() == Line.e_OpenArrow;
    }

    /**
     * Checks if the annotation is an ruler
     * A read lock is expected around this method.
     *
     * @param annot The annotation
     * @return True if the annotation is ruler
     */
    public static boolean isRuler(
            @NonNull Annot annot) {

        try {
            String itField = MeasureUtils.getIT(annot);
            return itField != null && itField.equals(MeasureUtils.K_LineDimension);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return RulerItem.getRulerItem(annot) != null; // legacy
    }

    /**
     * Checks if the annotation is perimeter measurement
     * A read lock is expected around this method.
     *
     * @param annot The annotation
     * @return True if the annotation is perimeter measurement
     */
    public static boolean isPerimeterMeasure(
            @NonNull Annot annot) {

        try {
            String itField = MeasureUtils.getIT(annot);
            return itField != null && itField.equals(MeasureUtils.K_PolyLineDimension);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * Checks if the annotation is area measurement
     * This could return a polygon area or a rectangular area measure
     * A read lock is expected around this method.
     *
     * @param annot The annotation
     * @return True if the annotation is area measurement
     */
    public static boolean isAreaMeasure(
            @NonNull Annot annot) {
        try {
            String itField = MeasureUtils.getIT(annot);
            return itField != null && itField.equals(MeasureUtils.K_PolygonDimension);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * Checks if the annotation is rectangular area measurement
     * A read lock is expected around this method.
     *
     * @param annot The annotation
     * @return True if the annotation is rectangular area measurement
     */
    public static boolean isRectAreaMeasure(
            @NonNull Annot annot) {
        try {
            if (Annot.e_Polygon == annot.getType()) {
                if (annot.isValid() &&
                        !Utils.isNullOrEmpty(annot.getCustomData(MeasureUtils.K_RECT_AREA))) {
                    String itField = MeasureUtils.getIT(annot);
                    return itField != null && itField.equals(MeasureUtils.K_PolygonDimension);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * Checks if the annotation is measure count tool
     * A read lock is expected around this method.
     *
     * @param annot The annotation
     * @return True if the annotation is measure count tool
     */
    public static boolean isCountMeasurement(@NonNull Annot annot) {
        try {
            if (Annot.e_Text == annot.getType()) {
                if (annot.isValid() &&
                        !Utils.isNullOrEmpty(annot.getCustomData(CountMeasurementCreateTool.COUNT_MEASURE_KEY))) {
                    return true;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * Checks if the annotation is a cloud polygon
     * A read lock is expected around this method.
     *
     * @param annot The annotation
     * @return True if the annotation is cloud
     * @throws PDFNetException PDFNet exception
     */
    public static boolean isCloud(
            @NonNull Annot annot)
            throws PDFNetException {

        Polygon polygon = new Polygon(annot);
        return polygon.isValid() && polygon.getBorderEffect() == Markup.e_Cloudy;
    }

    /**
     * Checks if the annotation is a free highlighter
     * A read lock is expected around this method.
     *
     * @param annot The annotation
     * @return True if the annotation is free highlighter
     * @throws PDFNetException PDFNet exception
     */
    public static boolean isFreeHighlighter(
            @NonNull Annot annot)
            throws PDFNetException {

        Ink ink = new Ink(annot);
        return ink.isValid() && ink.getHighlightIntent();
    }

    /**
     * Checks if the annotation is a freetext, but not callout
     * A read lock is expected around this method.
     *
     * @param annot The annotation
     * @return True if the annotation is a freetext, but not callout
     */
    public static boolean isBasicFreeText(@NonNull Annot annot)
            throws PDFNetException {
        if (Annot.e_FreeText == annot.getType()) {
            FreeText freeText = new FreeText(annot);
            return freeText.isValid() &&
                    freeText.getIntentName() != FreeText.e_FreeTextCallout;
        }
        return false;
    }

    /**
     * Checks if the annotation is a callout
     * A read lock is expected around this method.
     *
     * @param annot The annotation
     * @return True if the annotation is callout
     */
    public static boolean isCallout(@NonNull Annot annot)
            throws PDFNetException {
        if (Annot.e_FreeText == annot.getType()) {
            FreeText freeText = new FreeText(annot);
            return freeText.isValid() &&
                    freeText.getIntentName() == FreeText.e_FreeTextCallout;
        }
        return false;
    }

    /**
     * Checks if the annotation is a date text
     * A read lock is expected around this method.
     *
     * @param annot The annotation
     * @return True if the annotation is date text
     */
    public static boolean isFreeTextDate(@NonNull Annot annot)
            throws PDFNetException {
        if (Annot.e_FreeText == annot.getType()) {
            FreeText freeText = new FreeText(annot);
            return freeText.isValid() &&
                    !Utils.isNullOrEmpty(freeText.getCustomData(KEY_FreeTextDate));
        }
        return false;
    }

    /**
     * Checks if the annotation is a fill and sign type free text
     * A read lock is expected around this method.
     *
     * @param annot The annotation
     * @return True if the annotation is date text
     */
    public static boolean isFreeTextSpacing(@NonNull Annot annot)
            throws PDFNetException {
        if (Annot.e_FreeText == annot.getType()) {
            FreeText freeText = new FreeText(annot);
            return freeText.isValid() &&
                    !Utils.isNullOrEmpty(freeText.getCustomData(KEY_FreeTextFill));
        }
        return false;
    }

    /**
     * Checks if the annotation is a list box widget
     * A read lock is expected around this method.
     *
     * @param annot The annotation
     * @return True if the annotation is list box widget
     * @throws PDFNetException
     */
    public static boolean isListBox(@NonNull Annot annot) throws PDFNetException {
        if (Annot.e_Widget == annot.getType()) {
            Widget widget = new Widget(annot);
            Field field = widget.getField();
            if (field != null && field.isValid()) {
                int field_type = field.getType();
                boolean isCombo = field.getFlag(Field.e_combo);
                return Field.e_choice == field_type && !isCombo;
            }
        }
        return false;
    }

    /**
     * Checks if the annotation is an image stamp
     * A read lock is expected around this method.
     *
     * @param annot The annotation
     * @return True if the annotation is an image stamp
     * @throws PDFNetException
     */
    public static boolean isImageStamp(@NonNull Annot annot) throws PDFNetException {
        if (Annot.e_Stamp == annot.getType()) {
            if (annot.isValid()) {
                // Get the annot's appearance stream.
                Obj appearance = annot.getAppearance(Annot.e_normal, null);
                if (appearance != null && appearance.getType() == Obj.e_stream) {
                    Obj obj = appearance.findObj("PDFTRON");
                    if (obj != null) {
                        obj = obj.findObj("Private");
                        if (obj != null && obj.isName()) {
                            String n = obj.getName();
                            if ("Watermark".equals(n)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if the annotation is a signature stamp
     * A read lock is expected around this method.
     *
     * @param annot The annotation
     * @return True if the annotation is signature stamp
     * @throws PDFNetException
     */
    public static boolean isSignature(@NonNull Annot annot) throws PDFNetException {
        if (Annot.e_Stamp == annot.getType()) {
            Obj sigObj = annot.getSDFObj();
            sigObj = sigObj.findObj(Signature.SIGNATURE_ANNOTATION_ID);
            if (sigObj != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if annot is a TextField Widget
     * A read lock is expected around this method.
     * @param annot The annotation
     * @return True if the annotation is TextField widget
     * @throws PDFNetException
     */
    public static boolean isTextField(@NonNull Annot annot) throws PDFNetException {
        if (annot.getType() == Annot.e_Widget) {
            Widget widget = new Widget(annot);
            Field field = widget.getField();
            if (field.getType() == Field.e_text) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the annotation image resource ID.
     *
     * @param type The type of annotation
     * @return The annotation image resource ID
     */
    public static int getAnnotImageResId(int type) {
        int resId = android.R.id.empty;

        switch (type) {
            case Annot.e_Text:
                resId = R.drawable.ic_annotation_sticky_note_black_24dp;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT:
                resId = R.drawable.ic_measurement_count;
                break;
            case Annot.e_Line:
                resId = R.drawable.ic_annotation_line_black_24dp;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW:
                resId = R.drawable.ic_annotation_arrow_black_24dp;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RULER:
                resId = R.drawable.ic_annotation_distance_black_24dp;
                break;
            case Annot.e_Polyline:
                resId = R.drawable.ic_annotation_polyline_black_24dp;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_PERIMETER_MEASURE:
                resId = R.drawable.ic_annotation_perimeter_black_24dp;
                break;
            case Annot.e_Square:
                resId = R.drawable.ic_annotation_square_black_24dp;
                break;
            case Annot.e_Circle:
                resId = R.drawable.ic_annotation_circle_black_24dp;
                break;
            case Annot.e_Polygon:
                resId = R.drawable.ic_annotation_polygon_black_24dp;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_AREA_MEASURE:
                resId = R.drawable.ic_annotation_poly_area_24dp;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RECT_AREA_MEASURE:
                resId = R.drawable.ic_annotation_area_black_24dp;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD:
                resId = R.drawable.ic_annotation_cloud_black_24dp;
                break;
            case Annot.e_Underline:
                resId = R.drawable.ic_annotation_underline_black_24dp;
                break;
            case Annot.e_StrikeOut:
                resId = R.drawable.ic_annotation_strikeout_black_24dp;
                break;
            case Annot.e_Ink:
                resId = R.drawable.ic_annotation_freehand_black_24dp;
                break;
            case Annot.e_Highlight:
                resId = R.drawable.ic_annotation_highlight_black_24dp;
                break;
            case Annot.e_FreeText:
                resId = R.drawable.ic_annotation_freetext_black_24dp;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT:
                resId = R.drawable.ic_annotation_callout_black_24dp;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_SPACING:
                resId = R.drawable.ic_fill_and_sign_spacing_text;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_DATE:
                resId = R.drawable.ic_date_range_24px;
                break;
            case Annot.e_Squiggly:
                resId = R.drawable.ic_annotation_squiggly_black_24dp;
                break;
            case Annot.e_Stamp:
                resId = R.drawable.ic_annotation_stamp_black_24dp;
                break;
            case Annot.e_Caret:
                resId = R.drawable.ic_annotation_caret_black_24dp;
                break;
            case Annot.e_Redact:
                resId = R.drawable.ic_annotation_redact_black_24dp;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE:
                resId = R.drawable.ic_annotation_signature_black_24dp;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_ERASER:
                resId = R.drawable.ic_annotation_eraser_black_24dp;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_HIGHLIGHTER:
                resId = R.drawable.ic_annotation_free_highlight_black_24dp;
                break;
            case Annot.e_Sound:
                resId = R.drawable.ic_mic_black_24dp;
                break;
            case Annot.e_FileAttachment:
                resId = R.drawable.ic_attach_file_black_24dp;
                break;
            case Annot.e_Link:
                resId = R.drawable.ic_link_black_24dp;
                break;
            case Annot.e_Widget:
                resId = R.drawable.ic_prepare_form;
                break;
            default:
                break;
        }

        return resId;
    }

    /**
     * Returns the annotation color of the specified annotation.
     *
     * @param annot The annotation
     * @return The annotation color
     */
    public static int getAnnotColor(Annot annot) {
        int color;
        try {
            int type = annot.getType();
            ColorPt colorPt = annot.getColorAsRGB();
            color = Utils.colorPt2color(colorPt);
            if (type == Annot.e_FreeText) {
                FreeText freeText = new FreeText(annot);
                if (freeText.getTextColorCompNum() == 3) {
                    ColorPt fillColorPt = freeText.getTextColor();
                    color = Utils.colorPt2color(fillColorPt);
                }
            }
            if (annot.isMarkup()) {
                // if has fill color, use fill color
                Markup m = new Markup(annot);
                if (m.getInteriorColorCompNum() == 3) {
                    ColorPt fillColorPt = m.getInteriorColor();
                    int fillColor = Utils.colorPt2color(fillColorPt);
                    if (fillColor != Color.TRANSPARENT) {
                        color = fillColor;
                    }
                }
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            color = Color.BLACK;
        }
        return color;
    }

    /**
     * Returns the annotation opacity of the specified annotation.
     *
     * @param annot The annotation
     * @return The annotation opacity
     */
    public static float getAnnotOpacity(Annot annot) {
        float opacity = 1.0f;
        try {
            if (annot.isMarkup()) {
                Markup m = new Markup(annot);
                opacity = (float) m.getOpacity();
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            opacity = 1.0f;
        }
        return opacity;
    }

    /**
     * Returns the bounding box of the specified annotation in screen space.
     *
     * @param pdfViewCtrl The PDFViewCtrl
     * @param annot       The annotation
     * @param pg          The page number
     * @return The bounding box
     */
    public static com.pdftron.pdf.Rect computeAnnotInbox(PDFViewCtrl pdfViewCtrl, Annot annot, int pg) {
        try {
            com.pdftron.pdf.Rect r = annot.getRect();
            com.pdftron.pdf.Rect ur = new com.pdftron.pdf.Rect();
            r.normalize();
            double[] pts;
            pts = pdfViewCtrl.convPagePtToScreenPt(r.getX1(), r.getY2(), pg);
            ur.setX1(pts[0]);
            ur.setY1(pts[1]);
            pts = pdfViewCtrl.convPagePtToScreenPt(r.getX2(), r.getY1(), pg);
            ur.setX2(pts[0]);
            ur.setY2(pts[1]);
            return ur;
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        return null;
    }

    public static com.pdftron.pdf.Rect quadToRect(QuadPoint qp) throws PDFNetException {
        float x1 = (float) Math.min(Math.min(Math.min(qp.p1.x, qp.p2.x), qp.p3.x), qp.p4.x);
        float y1 = (float) Math.min(Math.min(Math.min(qp.p1.y, qp.p2.y), qp.p3.y), qp.p4.y);
        float x2 = (float) Math.max(Math.max(Math.max(qp.p1.x, qp.p2.x), qp.p3.x), qp.p4.x);
        float y2 = (float) Math.max(Math.max(Math.max(qp.p1.y, qp.p2.y), qp.p3.y), qp.p4.y);
        Rect quadRect = new Rect(x1, y1, x2, y2);
        quadRect.normalize();
        return quadRect;
    }

    public static Rect getPageRectFromScreenRect(PDFViewCtrl pdfViewCtrl, Rect screenRect, int pg) throws PDFNetException {
        return getPageRectFromScreenRect(pdfViewCtrl, screenRect, pg, 0);
    }

    public static Rect getPageRectFromScreenRect(PDFViewCtrl pdfViewCtrl, Rect screenRect, int pg, int rotation) throws PDFNetException {
        if (null == pdfViewCtrl || null == screenRect) {
            return null;
        }
        screenRect.normalize();
        double[] pts1 = pdfViewCtrl.convScreenPtToPagePt(screenRect.getX1(), screenRect.getY2(), pg);
        double[] pts2 = pdfViewCtrl.convScreenPtToPagePt(screenRect.getX2(), screenRect.getY1(), pg);
        if (rotation == 90 || rotation == 270) {
            pts1 = pdfViewCtrl.convScreenPtToPagePt(screenRect.getX2(), screenRect.getY2(), pg);
            pts2 = pdfViewCtrl.convScreenPtToPagePt(screenRect.getX1(), screenRect.getY1(), pg);
        }
        Rect result = new Rect(pts1[0], pts1[1], pts2[0], pts2[1]);
        result.normalize();
        return result;
    }

    public static android.graphics.RectF getScreenRectFromPageRect(PDFViewCtrl pdfViewCtrl, Rect pageRect, int pg) throws PDFNetException {
        if (null == pdfViewCtrl || null == pageRect) {
            return null;
        }
        android.graphics.RectF rectF = new RectF();
        pageRect.normalize();
        double[] pts;
        pts = pdfViewCtrl.convPagePtToScreenPt(pageRect.getX1(), pageRect.getY2(), pg);
        rectF.left = (float) pts[0];
        rectF.top = (float) pts[1];
        pts = pdfViewCtrl.convPagePtToScreenPt(pageRect.getX2(), pageRect.getY1(), pg);
        rectF.right = (float) pts[0];
        rectF.bottom = (float) pts[1];
        return rectF;
    }

    private static JSONObject createFreeTextJson(FreeTextCacheStruct freeTextCacheStruct) {
        JSONObject object = new JSONObject();
        try {
            object.put(FreeTextCacheStruct.CONTENTS, freeTextCacheStruct.contents);
            object.put(FreeTextCacheStruct.PAGE_NUM, freeTextCacheStruct.pageNum);
            JSONObject targetPoint = new JSONObject();
            targetPoint.put(FreeTextCacheStruct.X, freeTextCacheStruct.x);
            targetPoint.put(FreeTextCacheStruct.Y, freeTextCacheStruct.y);
            object.put(FreeTextCacheStruct.TARGET_POINT, targetPoint);
        } catch (Exception e) {
            e.printStackTrace();
            object = new JSONObject();
        }
        return object;
    }

    /**
     * Saves the free text in cache.
     *
     * @param freeTextCacheStruct The FreeTextCacheStruct
     * @param pdfViewCtrl         The PDFViewCtrl
     */
    public static void saveFreeTextCache(FreeTextCacheStruct freeTextCacheStruct, PDFViewCtrl pdfViewCtrl) {
        if (null == freeTextCacheStruct || null == pdfViewCtrl) {
            return;
        }
        if (pdfViewCtrl.getToolManager() == null) {
            return;
        }
        if (Utils.isNullOrEmpty(freeTextCacheStruct.contents)) {
            return;
        }
        String cacheFileName = ((ToolManager) pdfViewCtrl.getToolManager()).getFreeTextCacheFileName();

        JSONObject obj = createFreeTextJson(freeTextCacheStruct);
        ObjectOutput out = null;
        try {
            if (!cacheFileName.trim().isEmpty()) {
                out = new ObjectOutputStream(new FileOutputStream(new File(pdfViewCtrl.getContext().getCacheDir(), "") + cacheFileName));
                out.writeObject(obj.toString());
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception ignored) {

                }
            }
        }
    }

    /**
     * Gets annotation count on page excluding certain types
     * A read lock should be acquired outside of this method
     */
    public static int getAnnotationCountOnPage(@NonNull PDFViewCtrl pdfViewCtrl, int pageNum, @Nullable ArrayList<Integer> excludeType) throws PDFNetException {
        int count = 0;
        ArrayList<Annot> annotsOnPage = pdfViewCtrl.getAnnotationsOnPage(pageNum);
        for (Annot annot : annotsOnPage) {
            int type = annot.getType();
            if (excludeType != null) {
                if (excludeType.contains(type)) {
                    continue;
                }
            }
            count++;
        }
        return count;
    }

    /**
     * Delete all annotations except for links and form fields on pages
     * This method does not lock document, a write lock should be acquired outside
     *
     * @param doc the PDFDoc
     */
    @SuppressWarnings("unused")
    public static void safeDeleteAnnotsOnPage(PDFDoc doc, ArrayList<Integer> pages) {
        // delete all annotations except for links and form fields
        if (null == doc || null == pages) {
            return;
        }
        try {
            for (int pageNum : pages) {
                if (pageNum > -1) {
                    pageNum = pageNum + 1; // webviewer is 0-indexed
                    Page page = doc.getPage(pageNum);
                    if (page.isValid()) {
                        int annotationCount = page.getNumAnnots();
                        for (int a = annotationCount - 1; a >= 0; a--) {
                            try {
                                Annot annotation = page.getAnnot(a);
                                if (annotation == null || !annotation.isValid()) {
                                    continue;
                                }
                                if (annotation.getType() != Annot.e_Link &&
                                        annotation.getType() != Annot.e_Widget) {
                                    page.annotRemove(annotation);
                                }
                            } catch (PDFNetException e) {
                                // this annotation has some problem, let's skip it and continue with others
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    /**
     * Delete all annotations except for links and form fields
     * This method does not lock document, a write lock should be acquired outside
     *
     * @param doc the PDFDoc
     */
    public static void safeDeleteAllAnnots(PDFDoc doc) {
        // delete all annotations except for links and form fields
        if (null == doc) {
            return;
        }
        try {
            PageIterator pageIterator = doc.getPageIterator();
            while (pageIterator.hasNext()) {
                Page page = pageIterator.next();
                if (page.isValid()) {
                    int annotationCount = page.getNumAnnots();
                    for (int a = annotationCount - 1; a >= 0; a--) {
                        try {
                            Annot annotation = page.getAnnot(a);
                            if (annotation == null || !annotation.isValid()) {
                                continue;
                            }
                            if (annotation.getType() != Annot.e_Link &&
                                    annotation.getType() != Annot.e_Widget) {
                                page.annotRemove(annotation);
                            }
                        } catch (PDFNetException e) {
                            // this annotation has some problem, let's skip it and continue with others
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    public static Rect getOldAnnotScreenPosition(PDFViewCtrl pdfViewCtrl, Annot annot, int annotPageNum) throws PDFNetException {
        if (annot == null) {
            return null;
        }

        Rect oldUpdateRect = pdfViewCtrl.getScreenRectForAnnot(annot, annotPageNum);
        oldUpdateRect.normalize();
        return oldUpdateRect;
    }

    /**
     * Used in replacement of {@link Page#annotRemove(Annot)} to handle direct object and updates the viewer
     * The returned Annot object should be used after this function.
     * The Annot parameter should no longer be used after this function.
     */
    public static Annot safeDeleteAnnotAndUpdate(@NonNull PDFViewCtrl pdfViewCtrl, @NonNull Page page, @NonNull Annot annot, int annotPageNum) throws PDFNetException {
        boolean isIndirect = annot.getSDFObj().isIndirect();
        Rect oldScreenRect = null;
        if (!isIndirect) {
            oldScreenRect = getOldAnnotScreenPosition(pdfViewCtrl, annot, annotPageNum);
        }
        annot = AnnotUtils.safeDeleteAnnot(pdfViewCtrl.getDoc(), page, annot);
        if (oldScreenRect != null) {
            pdfViewCtrl.update(oldScreenRect);
        } else {
            pdfViewCtrl.update(annot, annotPageNum);
        }
        return annot;
    }

    /**
     * Used in replacement of {@link Page#annotRemove(Annot)} to handle direct object
     * The returned Annot object should be used after this function.
     * The Annot parameter should no longer be used after this function.
     */
    public static Annot safeDeleteAnnot(@NonNull PDFDoc pdfDoc, @NonNull Page page, @NonNull Annot annot) throws PDFNetException {
        Obj annotObj = annot.getSDFObj();
        Obj indirectCopy = annotObj.isIndirect() ? annotObj : pdfDoc.getSDFDoc().importObj(annotObj, false);

        page.annotRemove(annot);

        return new Annot(indirectCopy);
    }

    /**
     * Sets the author for the specified annotation, if possible.
     * <p>
     * <b>This method does not lock the document, so a write lock should be acquired outside.</b>
     *
     * @param annot  the Annot
     * @param author the desired author name
     */
    public static void setAuthor(Annot annot, String author) {
        try {
            if (annot != null && annot.isMarkup()) {
                Markup markup = new Markup(annot);
                setAuthor(markup, author);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the annotation date in local date time
     *
     * @param annot the annotation
     * @return the date
     */
    public static Date getAnnotLocalDate(@NonNull Annot annot) throws PDFNetException {
        com.pdftron.pdf.Date date = annot.getDate();
        return getLocalDate(date);
    }

    /**
     * Converts a com.pdftron.pdf.Date to local Date object
     *
     * @param date to convert
     * @return the local date
     */
    public static Date getLocalDate(@NonNull com.pdftron.pdf.Date date) throws PDFNetException {
        Calendar calendar = Calendar.getInstance();
        int month = date.getMonth() - 1; // android calendar month is 0-indexed
        calendar.set(date.getYear(), month, date.getDay(), date.getHour(), date.getMinute(), date.getSecond());
        int offset = TimeZone.getDefault().getRawOffset();
        boolean daylightSavingsEnabled = TimeZone.getDefault().inDaylightTime(new Date(calendar.getTimeInMillis()));
        if (daylightSavingsEnabled) {
            offset += TimeZone.getDefault().getDSTSavings();
        }
        long localTime = calendar.getTimeInMillis() + offset;
        return new Date(localTime);
    }

    /**
     * Gets the annotation creation date in local date time
     *
     * @param annot the annotation
     * @return the date
     */
    public static Date getAnnotLocalCreationDate(@NonNull Annot annot) throws PDFNetException {
        com.pdftron.pdf.Date date = annot.getDate();
        if (annot.isMarkup()) {
            date = (new Markup(annot)).getCreationDates();
        }
        Calendar calendar = Calendar.getInstance();
        int month = date.getMonth() - 1; // android calendar month is 0-indexed
        calendar.set(date.getYear(), month, date.getDay(), date.getHour(), date.getMinute(), date.getSecond());
        int offset = TimeZone.getDefault().getRawOffset() + TimeZone.getDefault().getDSTSavings();
        long localTime = calendar.getTimeInMillis() + offset;
        return new Date(localTime);
    }

    /**
     * Sets the author for the specified markup annotation.
     * <p>
     * <b>This method does not lock the document, so a write lock should be acquired outside.</b>
     *
     * @param markup the Markup annotation
     * @param author the desired author name
     */
    public static void setAuthor(Markup markup, String author) {
        if (markup == null) {
            return;
        }
        try {
            markup.setTitle(author);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the author for the specified annotation.
     * Annotation needs to be either type of markup or widget.
     *
     * @param annot the annotation
     * @return the author
     * @throws PDFNetException
     */
    @Nullable
    public static String getAuthor(Annot annot) throws PDFNetException {
        if (annot == null || !annot.isValid()) {
            return null;
        }
        if (annot.isMarkup()) {
            Markup mp = new Markup(annot);
            return mp.getTitle();
        } else if (annot.getType() == Annot.e_Widget) {
            Widget widget = new Widget(annot);
            if (widget.getSDFObj().get(KEY_WidgetAuthor) != null) {
                return widget.getSDFObj().get(KEY_WidgetAuthor).value().getAsPDFText();
            }
        }
        return null;
    }

    /**
     * Sets the unique identifier for the specified annotation, if possible.
     * <p>
     * <b>This method does not lock the document, so a write lock should be acquired outside.</b>
     *
     * @param annot the Annot
     * @param id    the unique identifier
     */
    public static void setUniqueId(Annot annot, String id) {
        try {
            if (annot != null) {
                annot.setUniqueID(id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the vertices of a Polyline or a Polygon.
     * <b>This method does not lock the document, so a read lock should be acquired outside.</b>
     *
     * @param annot annotation with type of polyline or polygon
     * @return the vertices
     */
    @Nullable
    public static ArrayList<Point> getPolyVertices(Annot annot) {
        try {
            if (annot == null) {
                return null;
            }
            if (annot.getType() == Annot.e_Polyline ||
                    annot.getType() == Annot.e_Polygon) {
                PolyLine polyLine = new PolyLine(annot);
                int count = polyLine.getVertexCount();

                ArrayList<Point> points = new ArrayList<>();
                for (int i = 0; i < count; ++i) {
                    Point pagePoint = polyLine.getVertex(i);
                    points.add(pagePoint);
                }
                return points;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * @hide
     */
    public static boolean isMadeByPDFTron(Annot annot) throws PDFNetException {
        String[] supportedTag = new String[]{Tool.PDFTRON_ID, "pdftronlink"};
        if (annot != null && annot.getSDFObj() != null) {
            Obj sdfObj = annot.getSDFObj();
            for (String tag : supportedTag) {
                Object selfMadeObj = sdfObj.findObj(tag);
                if (selfMadeObj != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks whether the annotation has the specified permission
     *
     * @param annot The annotation
     * @param kind  The kind of permission. Possible values are
     *              {@link Tool#ANNOT_PERMISSION_SELECTION},
     *              {@link Tool#ANNOT_PERMISSION_MENU}
     *              {@link Tool#ANNOT_PERMISSION_FILL_AND_SIGN}
     *              {@link Tool#ANNOT_PERMISSION_INTERACT}
     * @return True if the annotation has permission
     */
    public static boolean hasPermission(PDFViewCtrl pdfViewCtrl, Annot annot, int kind) {
        return hasPermission(pdfViewCtrl, null, null, annot, kind);
    }

    public static boolean hasPermission(PDFDoc pdfDoc, ToolManager toolManager, Annot annot, int kind) {
        return hasPermission(null, pdfDoc, toolManager, annot, kind);
    }

    public static boolean hasPermission(@Nullable PDFViewCtrl pdfViewCtrl,
            @Nullable PDFDoc pdfDoc, @Nullable ToolManager tm,
            Annot annot, int kind) {
        boolean hasPermission = true;
        ToolManager toolManager = pdfViewCtrl != null ? ((ToolManager) pdfViewCtrl.getToolManager()) : tm;
        if (toolManager != null) {
            // if read-only, assume no permission
            if (toolManager.isReadOnly()) {
                if (kind != Tool.ANNOT_PERMISSION_INTERACT) {
                    return false;
                }
            }
            // first check annot manager for author permission
            boolean shouldUnlockRead = false;
            if (toolManager.getAnnotManager() != null &&
                    (kind == Tool.ANNOT_PERMISSION_SELECTION || kind == Tool.ANNOT_PERMISSION_MENU)) { // only check for menu and selection permission
                AnnotManager.EditPermissionMode mode = toolManager.getAnnotManager().getEditMode();
                if (mode == AnnotManager.EditPermissionMode.EDIT_OTHERS) {
                    // no author check needed if we are in edit others mode
                    hasPermission = true;
                } else {
                    // check author in undo own mode
                    try {
                        if (pdfViewCtrl != null) {
                            pdfViewCtrl.docLockRead();
                        } else if (pdfDoc != null) {
                            pdfDoc.lockRead();
                        }
                        shouldUnlockRead = true;
                        String currentAuthor = toolManager.getAuthorId();
                        String userId = AnnotUtils.getAuthor(annot);
                        if (null != userId && null != currentAuthor) {
                            hasPermission = currentAuthor.equals(userId);
                        }
                    } catch (Exception e) {
                        hasPermission = true;
                        AnalyticsHandlerAdapter.getInstance().sendException(e);
                    } finally {
                        if (shouldUnlockRead) {
                            if (pdfViewCtrl != null) {
                                pdfViewCtrl.docUnlockRead();
                            } else if (pdfDoc != null) {
                                Utils.unlockReadQuietly(pdfDoc);
                            }
                        }
                        shouldUnlockRead = false;
                    }
                }
            }
            // if has author permission, second, check if has annot individual permission
            if (hasPermission && toolManager.isAnnotPermissionCheckEnabled()) {
                try {
                    if (pdfViewCtrl != null) {
                        pdfViewCtrl.docLockRead();
                    } else if (pdfDoc != null) {
                        pdfDoc.lockRead();
                    }
                    shouldUnlockRead = true;
                    if (kind == Tool.ANNOT_PERMISSION_SELECTION || kind == Tool.ANNOT_PERMISSION_FILL_AND_SIGN) {
                        if (annot.getFlag(Annot.e_locked)) {
                            hasPermission = false;
                        }
                    } else if (kind == Tool.ANNOT_PERMISSION_MENU) {
                        if (annot.getFlag(Annot.e_locked)) {
                            hasPermission = false;
                        }
                    } else if (kind == Tool.ANNOT_PERMISSION_INTERACT) {
                        if (annot.isMarkup() && annot.getFlag(Annot.e_read_only)) {
                            // spec says ignore widget
                            hasPermission = false;
                        }
                    }
                } catch (Exception e) {
                    hasPermission = true;
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } finally {
                    if (shouldUnlockRead) {
                        if (pdfViewCtrl != null) {
                            pdfViewCtrl.docUnlockRead();
                        } else if (pdfDoc != null) {
                            Utils.unlockReadQuietly(pdfDoc);
                        }
                    }
                }
            }
        }
        return hasPermission;
    }

    /**
     * Checks if the annotation is an annotation reply
     *
     * @param annot the annotation
     * @return true if it is an annotation reply, false otherwise
     */
    public static boolean hasReplyTypeReply(Annot annot) throws PDFNetException {
        if (annot == null) {
            return false;
        }
        Obj annotSDFObj = annot.getSDFObj();
        if (annotSDFObj != null) {
            Obj irt = annotSDFObj.findObj(KEY_InReplyTo);
            // if annotation contains IRT and does not contain RT of "Group"
            // then it is a reply
            return irt != null && !hasReplyTypeGroup(annot);
        }
        return false;
    }

    @Nullable
    public static Annot createAnnotationStateReply(@NonNull String parentAnnotId, int pageNum,
            @NonNull PDFViewCtrl pdfViewCtrl, @NonNull String authorId, @Nullable String authorName,
            @NonNull AnnotReviewState state) throws PDFNetException {
        Resources resources = pdfViewCtrl.getContext().getResources();
        String stateStr = "";
        String stateStrKey = Key_StateNone;
        switch (state) {
            case ACCEPTED:
                stateStr = resources.getString(R.string.annot_review_state_accepted);
                stateStrKey = Key_StateAccepted;
                break;
            case REJECTED:
                stateStr = resources.getString(R.string.annot_review_state_rejected);
                stateStrKey = Key_StateRejected;
                break;
            case CANCELLED:
                stateStr = resources.getString(R.string.annot_review_state_cancelled);
                stateStrKey = Key_StateCancelled;
                break;
            case COMPLETED:
                stateStr = resources.getString(R.string.annot_review_state_completed);
                stateStrKey = Key_StateCompleted;
                break;
            case NONE:
                stateStr = resources.getString(R.string.annot_review_state_none);
                stateStrKey = Key_StateNone;
                break;
        }
        String replyMessage = String.format(resources.getString(R.string.annot_review_state_set_by), stateStr, authorName != null ? authorName : authorId);

        Annot reply = createAnnotationReply(parentAnnotId, pageNum, pdfViewCtrl, authorId, replyMessage);
        // add state spec
        if (reply != null) {
            boolean shouldUnlock = false;
            try {
                pdfViewCtrl.docLock(true);
                shouldUnlock = true;
                reply.getSDFObj().putString(Key_State, stateStrKey);
                reply.getSDFObj().putString(Key_StateModel, Key_StateModelReview);
            } finally {
                if (shouldUnlock) {
                    pdfViewCtrl.docUnlock();
                }
            }
        }
        return reply;
    }

    @Nullable
    public static Annot createAnnotationReply(@NonNull String parentAnnotId, int pageNum,
            @NonNull PDFViewCtrl pdfViewCtrl, @NonNull String authorId,
            @NonNull String contents) throws PDFNetException {
        Annot parent = ViewerUtils.getAnnotById(pdfViewCtrl, parentAnnotId, pageNum);
        if (parent == null) {
            return null;
        }
        boolean shouldUnlock = false;
        try {
            if (!parent.isValid()) {
                return null;
            }
            pdfViewCtrl.docLock(true);
            shouldUnlock = true;
            Rect rect = parent.getRect();
            rect.normalize();
            double left = rect.getX1();
            double top = rect.getY2();
            // use parent top left corner as reply annot rect
            Point p = new Point(left, top);
            Text reply = Text.create(pdfViewCtrl.getDoc(), p);
            reply.setIcon(Text.e_Comment); // WV sets comment to replies
            setAuthor(reply, authorId);
            if (pdfViewCtrl.getToolManager() instanceof ToolManager) {
                String id = ((ToolManager) pdfViewCtrl.getToolManager()).generateKey();
                setUniqueId(reply, id);
            }
            reply.getSDFObj().putString(KEY_InReplyTo, parentAnnotId);
            reply.setContents(contents);

            Page page = pdfViewCtrl.getDoc().getPage(pageNum);
            page.annotPushBack(reply);

            return reply;
        } finally {
            if (shouldUnlock) {
                pdfViewCtrl.docUnlock();
            }
        }
    }

    public static Annot updateAnnotationReply(@NonNull String replyId, int pageNum,
            @NonNull PDFViewCtrl pdfViewCtrl,
            @Nullable ToolManager toolManager,
            @NonNull String contents) throws PDFNetException {
        Annot reply = ViewerUtils.getAnnotById(pdfViewCtrl, replyId, pageNum);
        if (reply == null || !reply.isMarkup()) {
            return null;
        }
        boolean shouldUnlock = false;
        try {
            pdfViewCtrl.docLock(true);
            shouldUnlock = true;
            Markup markup = new Markup(reply);
            HashMap<Annot, Integer> annots = new HashMap<>(1);
            annots.put(reply, pageNum);
            if (toolManager != null) {
                toolManager.raiseAnnotationsPreModifyEvent(annots);
            }
            // edit contents
            Utils.handleEmptyPopup(pdfViewCtrl.getDoc(), markup);
            Popup popup = markup.getPopup();
            popup.setContents(contents);
            markup.setDateToNow();

            if (toolManager != null) {
                toolManager.raiseAnnotationsModifiedEvent(annots, new Bundle());
            }
            return reply;
        } finally {
            if (shouldUnlock) {
                pdfViewCtrl.docUnlock();
            }
        }
    }

    public static void deleteAnnotationReply(@NonNull String replyId, int pageNum,
            @NonNull PDFViewCtrl pdfViewCtrl,
            @Nullable ToolManager toolManager) throws PDFNetException {
        Annot reply = ViewerUtils.getAnnotById(pdfViewCtrl, replyId, pageNum);
        if (reply == null) {
            return;
        }
        boolean shouldUnlock = false;
        try {
            pdfViewCtrl.docLock(true);
            shouldUnlock = true;
            HashMap<Annot, Integer> annots = new HashMap<>(1);
            annots.put(reply, pageNum);
            if (toolManager != null) {
                toolManager.raiseAnnotationsPreRemoveEvent(annots);
            }
            // remove from page
            Page page = pdfViewCtrl.getDoc().getPage(pageNum);
            page.annotRemove(reply);
            if (toolManager != null) {
                toolManager.raiseAnnotationsRemovedEvent(annots);
            }
        } finally {
            if (shouldUnlock) {
                pdfViewCtrl.docUnlock();
            }
        }
    }

    public static void setDateToNow(PDFViewCtrl pdfViewCtrl, Annot annot) {
        boolean shouldUnlock = false;
        try {
            pdfViewCtrl.docLock(true);
            shouldUnlock = true;
            annot.setDateToNow();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                pdfViewCtrl.docUnlock();
            }
        }
    }

    /**
     * Gets the unique Id of the IRT parent annotation of this annotation
     */
    @Nullable
    public static String getIRTAsString(Annot annot) throws PDFNetException {
        if (annot == null) {
            return null;
        }
        Obj annotSDFObj = annot.getSDFObj();
        if (annotSDFObj != null) {
            Obj irt = annotSDFObj.findObj(KEY_InReplyTo);
            if (irt != null) {
                // first check string, for backwards compatibility
                if (irt.isString()) {
                    return irt.getAsPDFText();
                }
                // IRT is dict link
                if (irt.isDict()) {
                    Obj nm = irt.findObj(KEY_NM);
                    if (nm != null && nm.isString()) {
                        return nm.getAsPDFText();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Checks if the annotation is part of a group annotation
     *
     * @param annot the annotation
     * @return true if it is part of a group annotation, false otherwise
     */
    public static boolean hasReplyTypeGroup(Annot annot) throws PDFNetException {
        if (annot == null) {
            return false;
        }
        // group annotation
        // RT is only meaningful if IRT is present
        // valid values are "R" and "Group"
        Obj annotSDFObj = annot.getSDFObj();
        if (annotSDFObj != null) {
            Obj irt = annotSDFObj.findObj(KEY_InReplyTo);
            Obj rt = annotSDFObj.findObj(KEY_ReplyTo);
            if (irt != null && rt != null && rt.isName()) {
                String rtVal = rt.getName();
                return VALUE_Group.equals(rtVal);
            }
        }
        return false;
    }

    /**
     * Check whether the selected annotations match an existing group
     *
     * @param selected the selected annotations
     * @return whether the selected annotations match an existing group
     */
    public static boolean isGroupSelected(PDFViewCtrl pdfViewCtrl, ArrayList<Annot> selected, int page) throws PDFNetException {
        if (null == pdfViewCtrl || null == selected) {
            return false;
        }
        boolean shouldUnlockRead = false;
        try {
            pdfViewCtrl.docLockRead();
            shouldUnlockRead = true;

            for (Annot annot : selected) {
                ArrayList<Annot> annotsInGroup = getAnnotationsInGroup(pdfViewCtrl, annot, page);
                if (null == annotsInGroup || annotsInGroup.isEmpty()) {
                    return false;
                }
                if (selected.size() != annotsInGroup.size()) {
                    return false;
                }
                if (!annotsInGroup.containsAll(selected)) {
                    return false;
                }
            }
        } finally {
            if (shouldUnlockRead) {
                pdfViewCtrl.docUnlockRead();
            }
        }
        return true;
    }

    /**
     * Creates a group of annotations
     *
     * @param primary    the primary annotation
     * @param allInGroup all annotations to be added to this group
     */
    public static void createAnnotationGroup(PDFViewCtrl pdfViewCtrl, Annot primary, ArrayList<Annot> allInGroup) throws PDFNetException {
        if (null == pdfViewCtrl || null == primary || null == allInGroup) {
            return;
        }
        boolean shouldUnlock = false;
        try {
            pdfViewCtrl.docLock(true);
            shouldUnlock = true;
            for (Annot ann : allInGroup) {
                // first set uuid if not exist yet
                if (ann.getUniqueID() == null) {
                    setUniqueId(ann, UUID.randomUUID().toString());
                }
                if (ann.equals(primary)) {
                    // primary
                    ann.getSDFObj().erase(KEY_ReplyTo);
                    ann.getSDFObj().erase(KEY_InReplyTo);
                } else {
                    ann.getSDFObj().putName(KEY_ReplyTo, VALUE_Group);
                    ann.getSDFObj().put(KEY_InReplyTo, primary.getSDFObj());
                }
            }
        } finally {
            if (shouldUnlock) {
                pdfViewCtrl.docUnlock();
            }
        }
    }

    public static void ungroupAnnotations(PDFViewCtrl pdfViewCtrl, ArrayList<Annot> allInGroup) throws PDFNetException {
        if (null == pdfViewCtrl || null == allInGroup) {
            return;
        }
        boolean shouldUnlock = false;
        try {
            pdfViewCtrl.docLock(true);
            shouldUnlock = true;
            for (Annot ann : allInGroup) {
                // remove all related keys
                ann.getSDFObj().erase(KEY_ReplyTo);
                ann.getSDFObj().erase(KEY_InReplyTo);
            }
        } finally {
            if (shouldUnlock) {
                pdfViewCtrl.docUnlock();
            }
        }
    }

    @Nullable
    public static Annot getPrimaryAnnotInGroup(PDFViewCtrl pdfViewCtrl, ArrayList<Annot> annots) throws PDFNetException {
        if (null == pdfViewCtrl || null == annots) {
            return null;
        }
        for (Annot ann : annots) {
            if (!hasReplyTypeGroup(ann)) {
                return ann;
            }
        }
        return null;
    }

    /**
     * Returns all annotations in this group if the annotation passed in is in an annotation group
     */
    @Nullable
    public static ArrayList<Annot> getAnnotationsInGroup(PDFViewCtrl pdfViewCtrl, Annot annot, int pageNum) throws PDFNetException {
        if (null == pdfViewCtrl || null == annot) {
            return null;
        }
        if (!annot.isValid()) {
            return null;
        }
        boolean shouldUnlockRead = false;
        try {
            ArrayList<Annot> annotsInGroup = new ArrayList<>();
            pdfViewCtrl.docLockRead();
            shouldUnlockRead = true;

            String mainAnnotId = null;
            if (hasReplyTypeGroup(annot)) {
                // this is a sub annot in a group
                // its IRT will be the primary annot in the group
                mainAnnotId = getIRTAsString(annot);
            } else if (annot.getUniqueID() != null) {
                // this is a primary annot
                mainAnnotId = annot.getUniqueID().getAsPDFText();
            }
            if (Utils.isNullOrEmpty(mainAnnotId)) {
                return null;
            }

            ArrayList<Annot> annotsInPage = pdfViewCtrl.getAnnotationsOnPage(pageNum);
            for (Annot ann : annotsInPage) {
                if (ann != null && ann.isValid() && ann.getUniqueID() != null) {
                    // find all annotations in the group
                    String id = ann.getUniqueID().getAsPDFText();
                    if (id != null && id.equals(mainAnnotId)) {
                        // add primary annot
                        annotsInGroup.add(ann);
                    }
                    String irt = getIRTAsString(ann);
                    if (hasReplyTypeGroup(ann) && irt != null && irt.equals(mainAnnotId)) {
                        // add subordinate annot
                        annotsInGroup.add(ann);
                    }
                }
            }
            return annotsInGroup;
        } finally {
            if (shouldUnlockRead) {
                pdfViewCtrl.docUnlockRead();
            }
        }
    }

    /**
     * A read lock is expected around this method.
     */
    public static boolean canUseBitmapAppearance(Annot annot) throws PDFNetException {
        int annotType = annot.getType();
        return annotType == Annot.e_Stamp ||
                annotType == Annot.e_Text ||
                annotType == Annot.e_FreeText ||
                annotType == Annot.e_Sound ||
                annotType == Annot.e_FileAttachment ||
                annotType == Annot.e_Redact ||
                annotType == Annot.e_Widget ||
                annotType == Annot.e_Link;
    }

    public static Single<Bitmap> getAnnotationAppearanceAsync(final PDFViewCtrl pdfViewCtrl, final Annot annot) {
        return Single.create(new SingleOnSubscribe<Bitmap>() {
            @Override
            public void subscribe(SingleEmitter<Bitmap> emitter) throws Exception {
                Bitmap bitmap = getAnnotationAppearance(pdfViewCtrl, annot);
                if (bitmap != null) {
                    emitter.onSuccess(bitmap);
                } else {
                    emitter.tryOnError(new IllegalStateException("Invalid state when creating annotation appearance"));
                }
            }
        });
    }

    @Nullable
    public static Bitmap getAnnotationAppearance(PDFViewCtrl pdfViewCtrl, Annot annot) {
        if (null == pdfViewCtrl || null == annot) {
            return null;
        }
        boolean shouldUnlockRead = false;
        PDFDoc doc = null;
        try {
            pdfViewCtrl.docLockRead();
            shouldUnlockRead = true;

            if (!annot.isValid() || !canUseBitmapAppearance(annot)) {
                return null;
            }

            PDFDraw draw = new PDFDraw();
            double dpi = pdfViewCtrl.getZoom() * 72.0 * pdfViewCtrl.getContext().getResources().getDisplayMetrics().density;
            draw.setDPI(Math.min(dpi, 360.0)); // cap at 360
            draw.setPageTransparent(true);
            draw.setAntiAliasing(true);

            Rect annotRect = annot.getRect();

            // Create a new transparent page
            doc = new PDFDoc();
            Rect pageRect = new Rect(0, 0, annotRect.getWidth(), annotRect.getHeight());
            Page page = doc.pageCreate(pageRect);
            doc.pagePushBack(page);

            // copy the annotation
            Obj srcAnnotation = annot.getSDFObj();
            Obj pEntry = srcAnnotation.findObj("P");

            Obj[] objList = new Obj[]{
                    srcAnnotation
            };

            Obj[] exclList = null;
            if (pEntry != null) {
                exclList = new Obj[]{
                        pEntry
                };

                // set page rotation
                Page p = new Page(pEntry);
                int pageRotation = Page.e_0;
                int viewRotation = pdfViewCtrl.getPageRotation();
                if (p.isValid()) {
                    pageRotation = p.getRotation();
                }
                int annotRotation = (pageRotation + viewRotation) % 4;
                if (!annot.getFlag(Annot.e_no_rotate)) {
                    page.setRotation(annotRotation);
                }
            }
            Obj[] destAnnot = doc.getSDFDoc().importObjs(objList, exclList);
            if (destAnnot != null && destAnnot.length > 0) {
                Annot dest = new Annot(destAnnot[0]);
                if (!dest.getSDFObj().isIndirect()) {
                    // let's not bother with direct annot object
                    return null;
                }
                dest.setRect(pageRect);
                page.annotPushBack(dest);
                if (!annot.getFlag(Annot.e_no_rotate)) {
                    dest.setRotation(annot.getRotation());
                }
                dest.flatten(page);

                if (annot.getFlag(Annot.e_no_zoom) || annot.getType() == Annot.e_Text) {
                    // crop
                    Rect visibleRect = page.getVisibleContentBox();

                    // maintain aspect ratio
                    double width = visibleRect.getWidth();
                    double height = visibleRect.getHeight();
                    if (width < height) {
                        width = height * annotRect.getWidth() / annotRect.getHeight();
                        // center it
                        double diff = Math.abs(visibleRect.getWidth() - width) * .5;
                        visibleRect.setX1(visibleRect.getX1() - diff);
                        visibleRect.setX2(visibleRect.getX1() + width);
                    } else {
                        height = width * annotRect.getWidth() / annotRect.getHeight();
                        // center it
                        double diff = Math.abs(visibleRect.getHeight() - height) * .5;
                        visibleRect.setY1(visibleRect.getY1() - diff);
                        visibleRect.setY2(visibleRect.getY1() + height);
                    }

                    page.setCropBox(visibleRect);
                    page.setMediaBox(visibleRect);
                }

                return draw.getBitmap(page);
            }
        } catch (Exception ignored) {
        } catch (OutOfMemoryError oom) {
            Utils.manageOOM(pdfViewCtrl);
        } finally {
            Utils.closeQuietly(doc);
            if (shouldUnlockRead) {
                pdfViewCtrl.docUnlockRead();
            }
        }
        return null;
    }

    /**
     * Gets whether the annotation rotation is not a multiple of 90
     * A read lock is needed around this method.
     */
    public static boolean hasRotation(Annot annot) throws PDFNetException {
        int degree = getAnnotRotation(annot);
        return degree != 0;
    }

    /**
     * Gets whether the annotation rotation is not a multiple of 90
     */
    public static boolean hasRotation(PDFViewCtrl pdfViewCtrl, Annot annot) {
        boolean shouldUnlockRead = false;
        try {
            pdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            return hasRotation(annot);
        } catch (Exception ignored) {
        } finally {
            if (shouldUnlockRead) {
                pdfViewCtrl.docUnlockRead();
            }
        }
        return false;
    }

    /**
     * Gets annotation rotation used in UI, read lock is not required.
     */
    public static int getAnnotUIRotation(PDFViewCtrl pdfViewCtrl, Annot annot, int pageNumber) {
        int pageRotation = Page.e_0;
        int viewRotation = Page.e_0;
        int annotRotation = 0;

        boolean shouldUnlockRead = false;
        try {
            pdfViewCtrl.docLockRead();
            shouldUnlockRead = true;

            pageRotation = pdfViewCtrl.getDoc().getPage(pageNumber).getRotation();
            viewRotation = pdfViewCtrl.getPageRotation();
            annotRotation = AnnotUtils.getAnnotRotation(annot);
        } catch (Exception ignored) {
        } finally {
            if (shouldUnlockRead) {
                pdfViewCtrl.docUnlockRead();
            }
        }

        int baseRotation = ((pageRotation + viewRotation) % 4) * 90;
        return baseRotation - annotRotation;
    }

    /**
     * Viewer and page rotation
     */
    public static int getAnnotBaseRotation(PDFViewCtrl pdfViewCtrl, int pageNumber) {
        int pageRotation = Page.e_0;
        int viewRotation = Page.e_0;

        boolean shouldUnlockRead = false;
        try {
            pdfViewCtrl.docLockRead();
            shouldUnlockRead = true;

            pageRotation = pdfViewCtrl.getDoc().getPage(pageNumber).getRotation();
            viewRotation = pdfViewCtrl.getPageRotation();
        } catch (Exception ignored) {
        } finally {
            if (shouldUnlockRead) {
                pdfViewCtrl.docUnlockRead();
            }
        }

        return ((pageRotation + viewRotation) % 4) * 90;
    }

    /**
     * Annotation rotation reletive to page and viewer rotation
     */
    public static int getAnnotRotationRelToPage(PDFViewCtrl pdfViewCtrl, Annot annot, int pageNumber) {
        int pageRotation = Page.e_0;
        int viewRotation = Page.e_0;
        int annotRotation = 0;

        boolean shouldUnlockRead = false;
        try {
            pdfViewCtrl.docLockRead();
            shouldUnlockRead = true;

            pageRotation = pdfViewCtrl.getDoc().getPage(pageNumber).getRotation();
            viewRotation = pdfViewCtrl.getPageRotation();
            annotRotation = AnnotUtils.getAnnotRotation(annot);
        } catch (Exception ignored) {
        } finally {
            if (shouldUnlockRead) {
                pdfViewCtrl.docUnlockRead();
            }
        }

        int baseRotation = ((pageRotation + viewRotation) % 4) * 90;
        return annotRotation - baseRotation;
    }

    /**
     * A read lock is needed around this method.
     */
    public static int getAnnotRotation(Annot annot) throws PDFNetException {
        int rotation = getStampDegree(annot);
        if (rotation != 0) {
            return rotation;
        }
        Obj rotationObj = annot.getSDFObj().findObj(KEY_ANNOT_ROTATION);
        if (rotationObj != null && rotationObj.isNumber()) {
            rotation = (int) rotationObj.getNumber();
        }
        if (rotation != 0) {
            return rotation;
        }
        return annot.getRotation();
    }

    /**
     * Sets the rotation degree from Stamp annotation.
     * A write lock is expected around this method.
     */
    public static void putStampDegree(Annot annot, int rotation) throws PDFNetException {
        Obj stampObj = annot.getSDFObj();
        stampObj.putNumber(Stamper.STAMPER_ROTATION_DEGREE_ID, rotation);
    }

    /**
     * Gets the rotation degree from Stamp annotation.
     * A read lock is expected around this method.
     */
    public static int getStampDegree(Annot annot) throws PDFNetException {
        Obj stampObj = annot.getSDFObj();
        Obj rotationObj = stampObj.findObj(Stamper.STAMPER_ROTATION_DEGREE_ID);
        int rotation = 0;
        if (rotationObj != null && rotationObj.isNumber()) {
            rotation = (int) rotationObj.getNumber();
            return rotation;
        }
        // try old stamp degree
        int oldDegree = getStampDegreeOld(annot);
        // old degree is clockwise, new degree is counter clockwise
        // let's convert here
        switch (oldDegree) {
            case 90:
                return 270;
            case 180:
                return 180;
            case 270:
                return 90;
        }
        return 0;
    }

    /**
     * Gets the deprecated rotation degree from Stamp annotation.
     * A read lock is expected around this method.
     */
    public static int getStampDegreeOld(Annot annot) throws PDFNetException {
        Obj stampObj = annot.getSDFObj();
        Obj rotationObj = stampObj.findObj(Stamper.STAMPER_ROTATION_ID);
        int rotation = 0;
        if (rotationObj != null && rotationObj.isNumber()) {
            rotation = (int) rotationObj.getNumber();
        }
        return rotation;
    }

    /**
     * A write lock is required.
     */
    public static void saveUnrotatedBBox(@NonNull PDFViewCtrl pdfViewCtrl, @NonNull Annot annot, int pageNum) throws PDFNetException {
        if (!annot.isValid()) {
            return;
        }
        if (annot.getType() == Annot.e_FreeText) {
            int pageRotation = pdfViewCtrl.getDoc().getPage(pageNum).getRotation();
            int viewRotation = pdfViewCtrl.getPageRotation();
            int baseRotation = ((pageRotation + viewRotation) % 4) * 90;
            FreeText freeText = new FreeText(annot);
            int annotRotation = AnnotUtils.getAnnotRotation(annot);
            if (annotRotation == 0 || (baseRotation == annotRotation)) {
                freeText.deleteCustomData(KEY_UNROTATED_BBOX);
            } else {
                Rect unrotated = RotationUtils.getUnrotatedDimensionsFromBBox(
                        annot.getRect(),
                        annotRotation - baseRotation
                );
                freeText.setCustomData(KEY_UNROTATED_BBOX, serializeRect(unrotated));
            }
        }
    }

    /**
     * A read lock is required.
     */
    public static Rect getUnrotatedBBox(@NonNull Annot annot) throws PDFNetException {
        String bboxStr = annot.getCustomData(KEY_UNROTATED_BBOX);
        return AnnotUtils.deserializeRect(bboxStr);
    }

    public static String serializeRect(Rect rect) throws PDFNetException {
        rect.normalize();
        StringBuilder sb = new StringBuilder();
        sb.append(rect.getX1())
                .append(",")
                .append(rect.getY1())
                .append(",")
                .append(rect.getX2())
                .append(",")
                .append(rect.getY2());
        return sb.toString();
    }

    @Nullable
    public static Rect deserializeRect(@Nullable String rectStr) {
        if (rectStr == null) {
            return null;
        }
        String[] numbers = rectStr.split(",");
        if (numbers.length == 4) {
            try {
                double[] pts = new double[4];
                for (int i = 0; i < numbers.length; i++) {
                    String num = numbers[i];
                    double d = Double.parseDouble(num);
                    pts[i] = d;
                }
                return new Rect(pts[0], pts[1], pts[2], pts[3]);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static int compareCreationDate(Annot thisObj,
            Annot thatObj) {

        if (thisObj == null || thatObj == null) {
            return 0;
        }
        try {
            Date thisDate = AnnotUtils.getAnnotLocalCreationDate(thisObj);
            Date thatDate = AnnotUtils.getAnnotLocalCreationDate(thatObj);
            return thisDate.compareTo(thatDate);
        } catch (PDFNetException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int compareDate(Annot thisObj,
            Annot thatObj) {

        if (thisObj == null || thatObj == null) {
            return 0;
        }
        try {
            Date thisDate = AnnotUtils.getAnnotLocalDate(thisObj);
            Date thatDate = AnnotUtils.getAnnotLocalDate(thatObj);
            return thisDate.compareTo(thatDate);
        } catch (PDFNetException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * A write lock is expected around this call
     */
    public static void setStyle(Annot annot, boolean hasFill,
            int strokeColor, int fillColor,
            float thickness, float opacity) throws PDFNetException {
        ColorPt color = Utils.color2ColorPt(strokeColor);
        annot.setColor(color, 3);

        if (hasFill && annot instanceof Markup) {
            color = Utils.color2ColorPt(fillColor);
            if (fillColor == Color.TRANSPARENT) {
                ((Markup) annot).setInteriorColor(color, 0);
            } else {
                ((Markup) annot).setInteriorColor(color, 3);
            }
        }

        if (annot instanceof Markup) {
            ((Markup) annot).setOpacity(opacity);
        }

        Annot.BorderStyle bs = annot.getBorderStyle();
        if (hasFill && strokeColor == Color.TRANSPARENT) {
            bs.setWidth(0);
        } else {
            bs.setWidth(thickness);
        }
        annot.setBorderStyle(bs);
    }

    /**
     * A write lock is expected around this call
     */
    public static void setBorderStyle(Annot annot, int effect, int style, double[] dash) throws PDFNetException {
        if (annot.isMarkup()) {
            Markup markup = new Markup(annot);
            markup.setBorderEffect(effect);
        }
        double strokeThickness = annot.getBorderStyle().getWidth();
        Annot.BorderStyle bs;
        if (dash != null) {
            bs = new Annot.BorderStyle(style,
                    (int) annot.getBorderStyle().getWidth(),
                    annot.getBorderStyle().getHR(),
                    annot.getBorderStyle().getVR(), dash);
        } else {
            bs = new Annot.BorderStyle(style,
                    (int) annot.getBorderStyle().getWidth(),
                    annot.getBorderStyle().getHR(),
                    annot.getBorderStyle().getVR());
        }
        // reset stroke width as border style will alter the width of stroke
        bs.setWidth(strokeThickness);
        annot.setBorderStyle(bs);
    }

    /**
     * Returns an XFDF command with add, modify, and delete tags containing annotations associated
     * with the given annotation ids and PDFDoc.
     * <p>
     * Requires caller to hold a read lock.
     *
     * @param pdfDoc           the PDFDoc used to create the XFDF command
     * @param annotIdsAdded    the list of annotation ids associated with annotations in the
     *                         doc that should be included in the add tag
     * @param annotIdsModified the list of annotation ids associated with annotations in the
     *                         doc that should be included in the modify tag
     * @param annotIdsDeleted  the list of annotation ids associated with annotations in the
     *                         doc that should be included in the delete tag
     * @return the XFDF command with populated add, modify, and delete tags where applicable
     */
    @Nullable
    public static String xfdfCommandExtract(@NonNull PDFDoc pdfDoc,
            @Nullable final List<String> annotIdsAdded,
            @Nullable final List<String> annotIdsModified,
            @Nullable final List<String> annotIdsDeleted) {

        try {
            final ArrayList<Annot> annotsToAdd = new ArrayList<>();
            final ArrayList<Annot> annotsToModify = new ArrayList<>();

            // Traverse all annotations and collect added/modified annotations
            traverseAnnots(pdfDoc, new AnnotVisitor() {
                @Override
                public void visit(@NonNull Annot annot) {
                    try {
                        Obj uniqueIdObj = annot.getUniqueID();
                        if (uniqueIdObj != null) {
                            String uniqueID = uniqueIdObj.getAsPDFText();
                            if (annotIdsAdded != null && annotIdsAdded.contains(uniqueID)) {
                                annotsToAdd.add(annot);
                            } else if (annotIdsModified != null && annotIdsModified.contains(uniqueID)) {
                                annotsToModify.add(annot);
                            }
                        }
                    } catch (PDFNetException e) {
                        // no op
                    }
                }
            });

            // Now create XFDF containing added and modified annotations
            FDFDoc fdfDoc = pdfDoc.fdfExtract(annotsToAdd, annotsToModify, new ArrayList<Annot>());
            String xfdfAddedAndModified = fdfDoc.saveAsXFDF();

            if (annotIdsDeleted != null && !annotIdsDeleted.isEmpty()) { // merge deleted ids if available
                // Combine deleted annotation ids into an XFDF command
                StringBuilder xfdfDeleteCommand = new StringBuilder("<delete>");
                if (annotIdsDeleted != null) {
                    for (String annotId : annotIdsDeleted) {
                        xfdfDeleteCommand.append("<id>").append(annotId).append("</id>");
                    }
                }
                xfdfDeleteCommand.append("</delete>");
                return xfdfAddedAndModified.replace("<delete />", xfdfDeleteCommand);
            } else { // otherwise just return
                return xfdfAddedAndModified;
            }
        } catch (PDFNetException e) {
            return null;
        }
    }

    /**
     * Traverse over all pages in the specified document. The {@link PageVisitor visitor} will be
     * called for each page via {@link PageVisitor#visit(Page)}, allowing for arbitrary operations
     * to be performed on the page.
     * <p>
     * <b>This method does not lock the document, so a read lock should be acquired outside.</b>
     *
     * @param doc     the PDFDoc
     * @param visitor the {@link PageVisitor} to visit each page
     */
    @SuppressWarnings("WeakerAccess")
    public static void traversePages(PDFDoc doc, PageVisitor visitor) {
        if (doc == null || visitor == null) {
            return;
        }
        try {
            PageIterator iterator = doc.getPageIterator();
            while (iterator.hasNext()) {
                Page page = iterator.next();
                if (page != null && page.isValid()) {
                    visitor.visit(page);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Traverse over all annotations on the specified page. The {@link AnnotVisitor visitor} will be
     * called for each annot via {@link AnnotVisitor#visit(Annot)}, allowing for arbitrary operations
     * to be performed on the annot.
     * <p>
     * <b>This method does not lock the document, so a read lock should be acquired outside.</b>
     *
     * @param page    the Page
     * @param visitor the {@link AnnotVisitor} to visit each annot
     */
    @SuppressWarnings("WeakerAccess")
    public static void traverseAnnots(Page page, AnnotVisitor visitor) {
        if (page == null || visitor == null) {
            return;
        }
        try {
            final int numAnnots = page.getNumAnnots();
            for (int i = 0; i < numAnnots; i++) {
                Annot annot = page.getAnnot(i);
                if (annot != null && annot.isValid()) {
                    visitor.visit(annot);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Traverse over all annotations on the specified page. The {@link AnnotWithPageVisitor visitor} will be
     * called for each annot via {@link AnnotWithPageVisitor#visit(Pair)}, allowing for arbitrary operations
     * to be performed on the annot.
     * <p>
     * <b>This method does not lock the document, so a read lock should be acquired outside.</b>
     *
     * @param page    the Page
     * @param visitor the {@link AnnotWithPageVisitor} to visit each annot
     */
    @SuppressWarnings("WeakerAccess")
    public static void traverseAnnotsWithPage(Page page, AnnotWithPageVisitor visitor) {
        if (page == null || visitor == null) {
            return;
        }
        try {
            final int numAnnots = page.getNumAnnots();
            for (int i = 0; i < numAnnots; i++) {
                Annot annot = page.getAnnot(i);
                if (annot != null && annot.isValid()) {
                    visitor.visit(new Pair<>(annot, page.getIndex()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Traverse over all annotations in the specified document. The {@link AnnotVisitor} will be
     * called for each annot via {@link AnnotVisitor#visit(Annot)}, allowing for arbitrary operations
     * to be performed on the annot.
     * <p>
     * <b>This method does not lock the document, so a read lock should be acquired outside.</b>
     *
     * @param doc     the PDFDoc
     * @param visitor the {@link AnnotVisitor} to visit each annot
     */
    public static void traverseAnnots(PDFDoc doc, final AnnotVisitor visitor) {
        if (doc == null || visitor == null) {
            return;
        }
        traversePages(doc, new PageVisitor() {
            @Override
            public void visit(@NonNull Page page) {
                traverseAnnots(page, visitor);
            }
        });
    }

    /**
     * Traverse over all annotations in the specified document. The {@link AnnotWithPageVisitor} will be
     * called for each annot via {@link AnnotWithPageVisitor#visit(Pair)}, allowing for arbitrary operations
     * to be performed on the annot.
     * <p>
     * <b>This method does not lock the document, so a read lock should be acquired outside.</b>
     *
     * @param doc     the PDFDoc
     * @param visitor the {@link AnnotWithPageVisitor} to visit each annot
     */
    public static void traverseAnnotsWithPage(PDFDoc doc, final AnnotWithPageVisitor visitor) {
        if (doc == null || visitor == null) {
            return;
        }
        traversePages(doc, new PageVisitor() {
            @Override
            public void visit(@NonNull Page page) {
                traverseAnnotsWithPage(page, visitor);
            }
        });
    }

    public static JSONObject simpleXmlParser(String xfdfCommand) throws IOException, SAXException, ParserConfigurationException, JSONException {
        JSONObject jsonObject = new JSONObject();

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        InputStream is = null;
        try {
            is = IOUtils.toInputStream(xfdfCommand);
            Document doc = dBuilder.parse(is);
            org.w3c.dom.Element element = doc.getDocumentElement();
            element.normalize();

            JSONArray addArr = new JSONArray();
            NodeList addList = doc.getElementsByTagName(XFDF_ADD);
            if (addList.getLength() > 0) {
                Node node = addList.item(0);
                NodeList childList = node.getChildNodes();
                for (int i = 0; i < childList.getLength(); i++) {
                    Node childNode = childList.item(i);
                    if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                        org.w3c.dom.Element childElement = (org.w3c.dom.Element) childNode;
                        String id = childElement.getAttribute("name");
                        if (!Utils.isNullOrEmpty(id)) {
                            addArr.put(id);
                        }
                    }
                }
            }
            jsonObject.put(XFDF_ADD, addArr);

            JSONArray modifyArr = new JSONArray();
            NodeList modifyList = doc.getElementsByTagName(XFDF_MODIFY);
            if (modifyList.getLength() > 0) {
                Node node = modifyList.item(0);
                NodeList childList = node.getChildNodes();
                for (int i = 0; i < childList.getLength(); i++) {
                    Node childNode = childList.item(i);
                    if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                        org.w3c.dom.Element childElement = (org.w3c.dom.Element) childNode;
                        String id = childElement.getAttribute("name");
                        if (!Utils.isNullOrEmpty(id)) {
                            modifyArr.put(id);
                        }
                    }
                }
            }
            jsonObject.put(XFDF_MODIFY, modifyArr);

            JSONArray deleteArr = new JSONArray();
            NodeList deleteList = doc.getElementsByTagName(XFDF_DELETE);
            if (deleteList.getLength() > 0) {
                Node node = deleteList.item(0);
                NodeList childList = node.getChildNodes();
                for (int i = 0; i < childList.getLength(); i++) {
                    Node childNode = childList.item(i);
                    if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                        org.w3c.dom.Element childElement = (org.w3c.dom.Element) childNode;
                        if (childElement.getTagName().equals("id")) {
                            String id = childElement.getFirstChild().getNodeValue();
                            if (!Utils.isNullOrEmpty(id)) {
                                deleteArr.put(id);
                            }
                        }
                    }
                }
            }
            jsonObject.put(XFDF_DELETE, deleteArr);
            return jsonObject;
        } finally {
            Utils.closeQuietly(is);
        }
    }

    public static void setLineEndingStyle(Line line, LineEndingStyle lineEndingStyle, Boolean lineStart) throws PDFNetException {
        switch (lineEndingStyle) {
            case BUTT:
                if (lineStart) {
                    line.setStartStyle(Line.e_Butt);
                } else {
                    line.setEndStyle(Line.e_Butt);
                }
                break;
            case DIAMOND:
                if (lineStart) {
                    line.setStartStyle(Line.e_Diamond);
                } else {
                    line.setEndStyle(Line.e_Diamond);
                }
                break;
            case CIRCLE:
                if (lineStart) {
                    line.setStartStyle(Line.e_Circle);
                } else {
                    line.setEndStyle(Line.e_Circle);
                }
                break;
            case OPEN_ARROW:
                if (lineStart) {
                    line.setStartStyle(Line.e_OpenArrow);
                } else {
                    line.setEndStyle(Line.e_OpenArrow);
                }
                break;
            case CLOSED_ARROW:
                if (lineStart) {
                    line.setStartStyle(Line.e_ClosedArrow);
                } else {
                    line.setEndStyle(Line.e_ClosedArrow);
                }
                break;
            case R_OPEN_ARROW:
                if (lineStart) {
                    line.setStartStyle(Line.e_ROpenArrow);
                } else {
                    line.setEndStyle(Line.e_ROpenArrow);
                }
                break;
            case R_CLOSED_ARROW:
                if (lineStart) {
                    line.setStartStyle(Line.e_RClosedArrow);
                } else {
                    line.setEndStyle(Line.e_RClosedArrow);
                }
                break;
            case SLASH:
                if (lineStart) {
                    line.setStartStyle(Line.e_Slash);
                } else {
                    line.setEndStyle(Line.e_Slash);
                }
                break;
            case SQUARE:
                if (lineStart) {
                    line.setStartStyle(Line.e_Square);
                } else {
                    line.setEndStyle(Line.e_Square);
                }
                break;
            case NONE:
                if (lineStart) {
                    line.setStartStyle(Line.e_None);
                } else {
                    line.setEndStyle(Line.e_None);
                }
                break;
        }
    }

    /**
     * Base interface for a visitor that will visit all members of a collection.
     *
     * @param <T> the collection member type
     */
    private interface Visitor<T> {
        void visit(@NonNull T node);
    }

    /**
     * Visitor that visits pages within a document.
     */
    public interface PageVisitor extends Visitor<Page> {
        /**
         * The overloaded implementation of {@link Visitor#visit(Object)}
         */
        @Override
        void visit(@NonNull Page page);
    }

    /**
     * Visitor that visits annotations on a single page or in an entire document.
     */
    public interface AnnotVisitor extends Visitor<Annot> {
        /**
         * The overloaded implementation of {@link Visitor#visit(Object)}
         */
        @Override
        void visit(@NonNull Annot annot);
    }

    /**
     * Visitor that visits annotations on a single page or in an entire document.
     */
    public interface AnnotWithPageVisitor extends Visitor<Pair<Annot, Integer>> {
        /**
         * The overloaded implementation of {@link Visitor#visit(Object)}
         */
        @Override
        void visit(@NonNull Pair<Annot, Integer> annotPair);
    }

    public static ArrayList<Redactor.Redaction> getRedactionArray(@NonNull Redaction redactAnnot, int annotPageNum) throws PDFNetException {
        String overlayText = redactAnnot.getOverlayText();
        if (overlayText == null) {
            overlayText = "";
        }
        int sz = redactAnnot.getQuadPointCount();
        ArrayList<Redactor.Redaction> rarr = new ArrayList<>();
        double minX = 0;
        double maxX = 0;
        double minY = 0;
        double maxY = 0;
        if (sz > 0) {
            for (int i = 0; i < sz; ++i) {
                QuadPoint qp = redactAnnot.getQuadPoint(i);
                Rect quadRect = AnnotUtils.quadToRect(qp);
                rarr.add(new Redactor.Redaction(annotPageNum, quadRect, false, overlayText));
            }
        }
        return rarr;
    }

    public static void applyRedaction(@NonNull PDFViewCtrl pdfViewCtrl, @NonNull Redaction redactAnnot, @NonNull ArrayList<Redactor.Redaction> arr) throws PDFNetException {
        ColorPt fillColorPt = redactAnnot.getInteriorColor();
        Redactor.Appearance app = new Redactor.Appearance();
        app.useOverlayText = true;
        app.positiveOverlayColor = fillColorPt;
        app.redactionOverlay = true;
        app.border = false;
        Redactor.redact(pdfViewCtrl.getDoc(), arr.toArray(new Redactor.Redaction[arr.size()]), app, false, false);
    }

    public static void deleteAllAnnotsByType(PDFDoc doc, int type) {
        // delete all annotations except for links and form fields
        if (null == doc) {
            return;
        }
        try {
            PageIterator pageIterator = doc.getPageIterator();
            while (pageIterator.hasNext()) {
                Page page = pageIterator.next();
                if (page.isValid()) {
                    int annotationCount = page.getNumAnnots();
                    for (int a = annotationCount - 1; a >= 0; a--) {
                        try {
                            Annot annotation = page.getAnnot(a);
                            if (annotation == null || !annotation.isValid()) {
                                continue;
                            }
                            if (annotation.getType() == type) {
                                page.annotRemove(annotation);
                            }
                        } catch (PDFNetException e) {
                            // this annotation has some problem, let's skip it and continue with others
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    public static ArrayList<Pair<Redaction, Integer>> getAllRedactions(@NonNull PDFViewCtrl pdfViewCtrl) throws PDFNetException {
        int pageNum = pdfViewCtrl.getDoc().getPageCount();
        ArrayList<Pair<Redaction, Integer>> redactionArr = new ArrayList<>();
        for (int i = 1; i <= pageNum; i++) {
            ArrayList<Annot> arr = pdfViewCtrl.getAnnotationsOnPage(i);
            for (Annot annot : arr) {
                if (annot.getType() == Annot.e_Redact) {
                    Redaction redactAnnot = new Redaction(annot);
                    redactionArr.add(new Pair<Redaction, Integer>(redactAnnot, i));
                }
            }
        }
        return redactionArr;
    }

    /**
     * Checks if the signature widget overlaps with a signature annotation within a certain distance
     * Used to check if any annotations should be linked to the widget
     */
    public static Annot getAssociatedAnnotation(@NonNull PDFViewCtrl pdfViewCtrl,
            @NonNull SignatureWidget signatureWidget, int pageNum) {
        boolean shouldUnlockRead = false;
        try {
            pdfViewCtrl.docLockRead();
            shouldUnlockRead = true;

            ArrayList<Annot> annots = pdfViewCtrl.getAnnotationsOnPage(pageNum);
            for (Annot annot : annots) {
                if (isAssociatedAnnotation(signatureWidget, annot)) {
                    return annot;
                }
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (shouldUnlockRead) {
                pdfViewCtrl.docUnlockRead();
            }
        }
        return null;
    }

    /**
     * A read lock is expected around this method
     */
    private static boolean isAssociatedAnnotation(@NonNull SignatureWidget signatureWidget,
            @NonNull Annot annotation) throws PDFNetException {
        if (annotation.isValid()) {
            int type = annotation.getType();
            if (type == Annot.e_Ink || type == Annot.e_Stamp) {
                // mobile signs floating signature as stamp
                // web signs floating signature as ink
                Rect widgetRect = signatureWidget.getRect();
                widgetRect.normalize();
                Rect annotRect = annotation.getRect();
                annotRect.normalize();
                if (widgetRect.contains(annotRect.getX1(), annotRect.getY1()) && widgetRect.contains(annotRect.getX2(), annotRect.getY2())) {
                    return true;
                }
                // web aligns the rect of a signature annotation to 'Sign here' when it's added to the widget based on the rotation
                // we need to take that into account and use different points for comparison
                double threshold = 1;
                int widgetRotation = signatureWidget.getRotation();
                if (widgetRotation == 90) {
                    // compare the bottom left point
                    return Math.abs(annotRect.getX1() - widgetRect.getX1()) <= threshold && Math.abs(annotRect.getY2() - widgetRect.getY2()) <= threshold;
                } else if (widgetRotation == 180) {
                    // compare the bottom right point
                    return Math.abs(annotRect.getX2() - widgetRect.getX2()) <= threshold && Math.abs(annotRect.getY2() - widgetRect.getY2()) <= threshold;
                } else if (widgetRotation == 270) {
                    // compare the top right point
                    return Math.abs(annotRect.getX2() - widgetRect.getX2()) <= threshold && Math.abs(annotRect.getY1() - widgetRect.getY1()) <= threshold;
                } else {
                    // compare the top left point
                    return Math.abs(annotRect.getX1() - widgetRect.getX1()) <= threshold && Math.abs(annotRect.getY1() - widgetRect.getY1()) <= threshold;
                }
            }
        }
        return false;
    }

    public static String getAnnotContents(@NonNull PDFDoc pdfDoc, @NonNull Annot annot) {
        boolean shouldUnlockRead = false;
        try {
            pdfDoc.lockRead();
            shouldUnlockRead = true;

            return annot.getContents();
        } catch (Exception e) {
            return "";
        } finally {
            if (shouldUnlockRead) {
                Utils.unlockReadQuietly(pdfDoc);
            }
        }
    }

    // A write lock is expected around this method
    public static void setAnnotContents(PDFDoc pdfDoc, Markup markup, String newContent) throws PDFNetException {
        Utils.handleEmptyPopup(pdfDoc, markup);
        Popup popup = markup.getPopup();
        String oldContent = popup.getContents();
        popup.setContents(newContent);
        if (Utils.isTextCopy(markup) && newContent != null && !newContent.equals(oldContent)) {
            Utils.removeTextCopy(markup);
        }
        markup.setDateToNow();
    }

    @Nullable
    public static FileAttachment getFileAttachment(PDFViewCtrl pdfViewCtrl, Annot annot) {
        boolean shouldUnlockRead = false;
        try {
            pdfViewCtrl.docLockRead();
            shouldUnlockRead = true;

            return new FileAttachment(annot);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                pdfViewCtrl.docUnlockRead();
            }
        }
        return null;
    }

    public static void createSoundAnnotation(PDFViewCtrl pdfViewCtrl, PointF targetPagePoint, int pageNum, String outputFile) {
        boolean shouldUnlock = false;
        try {
            pdfViewCtrl.docLock(true);
            shouldUnlock = true;

            ToolManager tm = (ToolManager) pdfViewCtrl.getToolManager();
            ToolManager.Tool tool = tm.createTool(ToolManager.ToolMode.SOUND_CREATE, null, null, true);
            if (tool instanceof SoundCreate) {
                ((SoundCreate) tool).createSound(targetPagePoint, pageNum, outputFile);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                pdfViewCtrl.docUnlock();
            }
        }
    }

    public static double getFormFieldBackgroundOpacity(Annot annot) {
        double backgroundOpacity = 1.0D;
        try {
            backgroundOpacity= Double.parseDouble(annot.getCustomData(BACKGROUND_ALPHA));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return backgroundOpacity;
    }
}
