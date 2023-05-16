package com.pdftron.collab.utils;

import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.collab.db.dao.AnnotationDao;
import com.pdftron.collab.db.entity.AnnotationEntity;
import com.pdftron.collab.db.entity.ReplyEntity;
import com.pdftron.collab.service.CustomServiceUtils;
import com.pdftron.common.PDFNetException;
import com.pdftron.fdf.FDFDoc;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.model.AnnotReviewState;
import com.pdftron.pdf.tools.AnnotManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.Logger;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.sdf.Obj;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Utility class for XFDF
 */
public class XfdfUtils {

    private static final String TAG = XfdfUtils.class.getName();

    public static final String XFDF_START = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
            "<xfdf xmlns=\"http://ns.adobe.com/xfdf/\" xml:space=\"preserve\">";
    public static final String XFDF_END = "</xfdf>";

    public static final String XFDF_ADD = AnnotUtils.XFDF_ADD;
    public static final String XFDF_MODIFY = AnnotUtils.XFDF_MODIFY;
    public static final String XFDF_DELETE = AnnotUtils.XFDF_DELETE;

    private static final String EMPTY_XFDF = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><xfdf xmlns=\"http://ns.adobe.com/xfdf/\" xml:space=\"preserve\"><fields/><annots/></xfdf>";

    private static final String XML_ADD_START = "<add>";
    private static final String XML_ADD_END = "</add>";
    private static final String XML_MODIFY_START = "<modify>";
    private static final String XML_MODIFY_END = "</modify>";
    private static final String XML_DELETE_START = "<delete>";
    private static final String XML_DELETE_END = "</delete>";

    public static final String OP_ADD = "add";
    public static final String OP_MODIFY = "modify";
    public static final String OP_REMOVE = "remove";
    private static final String WS_DELETE = "delete";
    private static final String WS_CREATE = "create";

    /**
     * Converts {@link Annot} to {@link AnnotationEntity}
     *
     * @param pdfDoc the PDF document
     * @param annot  the annotation
     * @return the annotation entity
     * @throws PDFNetException
     */
    @Nullable
    public static AnnotationEntity toAnnotationEntity(@NonNull PDFDoc pdfDoc, @NonNull String documentId, @Nullable Annot annot) throws PDFNetException {
        if (null == annot || !annot.isValid() || annot.getUniqueID() == null) {
            return null;
        }
        String annotId = annot.getUniqueID().getAsPDFText();
        String authorId = (new Markup(annot)).getTitle();
        Annot[] annots = new Annot[]{annot};
        FDFDoc fdfDoc = pdfDoc.fdfExtract(annots);
        String xfdf = fdfDoc.saveAsXFDF();
        AnnotationEntity entity = new AnnotationEntity();
        entity.setId(annotId);
        entity.setAuthorId(authorId);
        entity.setAuthorName(authorId);
        entity.setXfdf(xfdf);
        XfdfUtils.fillAnnotationEntity(documentId, entity);
        return entity;
    }

    /**
     * Utility method to return a valid XFDF command string
     */
    public static String validateXfdf(@NonNull String xfdf) {
        if (!xfdf.endsWith(XFDF_END)) {
            return XFDF_START + xfdf + XFDF_END;
        }
        return xfdf;
    }

    public static String validateXfdf(@NonNull String xfdf, @NonNull String action) {
        String processedXfdf = validateXfdf(xfdf);
        String openTag = XML_ADD_START;
        String closeTag = XML_ADD_END;
        if (action.equals(AnnotManager.AnnotationAction.MODIFY)) {
            openTag = XML_MODIFY_START;
            closeTag = XML_MODIFY_END;
        }
        if (xfdf.contains("<annots>")) {
            processedXfdf = processedXfdf.replace("<annots>", openTag);
            processedXfdf = processedXfdf.replace("</annots>", closeTag);
        }
        return processedXfdf;
    }

    @Nullable
    public static Pair<String, HashMap<String, AnnotationEntity>> parseAndProcessXfdfCommand(@Nullable String documentId, @NonNull String xfdfCommand) {
        FDFDoc fdfDoc = null;
        try {
            xfdfCommand = validateXfdf(xfdfCommand);
            fdfDoc = new FDFDoc();
            fdfDoc.mergeAnnots(xfdfCommand);
            return parseAndProcessXfdf(documentId, fdfDoc.saveAsXFDF());
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (fdfDoc != null) {
                Utils.closeQuietly(fdfDoc);
            }
        }
        return null;
    }

    /**
     * Parses XFDF string into map of properties
     */
    @Nullable
    public static HashMap<String, AnnotationEntity> parseXfdf(@Nullable String documentId, @NonNull String xfdf) {
        Pair<String, HashMap<String, AnnotationEntity>> result = parseAndProcessXfdf(documentId, xfdf);
        return result != null ? result.second : null;
    }

    /**
     * Parses XFDF string into map of properties, also return processed XFDF string
     */
    @Nullable
    public static Pair<String, HashMap<String, AnnotationEntity>> parseAndProcessXfdf(@Nullable String documentId, @NonNull String xfdf) {
        HashMap<String, AnnotationEntity> map = new HashMap<>();
        FDFDoc fdfDoc = null;
        try {
            String action = AnnotManager.AnnotationAction.ADD;
            if (xfdf.contains("<modify>")) {
                action = AnnotManager.AnnotationAction.MODIFY;
            }
            xfdf = xfdf.replace("<add>", "");
            xfdf = xfdf.replace("</add>", "");
            xfdf = xfdf.replace("<modify>", "");
            xfdf = xfdf.replace("</modify>", "");
            xfdf = validateXfdf(xfdf);

            fdfDoc = FDFDoc.createFromXFDF(xfdf);
            Obj fdf = fdfDoc.getFDF();
            if (fdf != null) {
                Obj annots = fdf.findObj(Keys.FDF_ANNOTS);
                if (annots != null && annots.isArray()) {
                    long size = annots.size();
                    for (int i = 0; i < size; i++) {
                        Obj annotObj = annots.getAt(i);

                        if (annotObj != null) {
                            AnnotationEntity annotationEntity = new AnnotationEntity();
                            Annot annot = new Annot(annotObj);

                            int type = AnnotUtils.getAnnotType(annot);
                            if (!annot.isMarkup() && type != Annot.e_Widget) {
                                continue;
                            }

                            Markup markup = new Markup(annot);

                            String id = safeGetObjAsString(annotObj, Keys.FDF_ANNOT_ID);

                            if (Utils.isNullOrEmpty(id)) {
                                // missing unique identifier, let's add it
                                id = UUID.randomUUID().toString();
                                annotObj.putName(Keys.FDF_ANNOT_ID, id);
                            }

                            annotationEntity.setId(id);

                            String currentAnnotXfdf = null;

                            FDFDoc tempFdfDoc = null;
                            try {
                                tempFdfDoc = FDFDoc.createFromXFDF(EMPTY_XFDF);
                                Obj tempFdf = tempFdfDoc.getFDF();
                                Obj newAnnots = tempFdfDoc.getSDFDoc().createIndirectArray();

                                Obj destAnnot = tempFdfDoc.getSDFDoc().importObj(annotObj, true);
                                newAnnots.pushBack(destAnnot);

                                // create fdf with only one annotation
                                tempFdf.put(Keys.FDF_ANNOTS, newAnnots);
                                currentAnnotXfdf = tempFdfDoc.saveAsXFDF();
                            } catch (Exception ex) {
                                AnalyticsHandlerAdapter.getInstance().sendException(ex);
                            } finally {
                                if (tempFdfDoc != null) {
                                    tempFdfDoc.close();
                                }
                            }

                            if (currentAnnotXfdf == null) {
                                // failed to parse current annotation
                                continue;
                            }
                            annotationEntity.setXfdf(currentAnnotXfdf);
                            if (!Utils.isNullOrEmpty(documentId)) {
                                annotationEntity.setDocumentId(documentId);
                            }

                            Rect rect = annot.getRect();
                            rect.normalize();
                            double yPos = rect.getY2();
                            int color = AnnotUtils.getAnnotColor(annot);
                            float opacity = AnnotUtils.getAnnotOpacity(annot);

                            String authorId = markup.getTitle();

                            String content = annot.getContents();
                            String inReplyTo = safeGetObjAsString(annotObj, Keys.FDF_IN_REPLY_TO);

                            Date creationDate = deserializeDate(safeGetObjAsString(annotObj, Keys.FDF_CREATION_DATE));

                            Date date = deserializeDate(safeGetObjAsString(annotObj, Keys.FDF_DATE));
                            if (type == Annot.e_Widget && creationDate == null) {
                                // widget only has 1 date
                                creationDate = date;
                            }

                            Integer page = safeGetObjAsInteger(annotObj, Keys.FDF_PAGE);

                            String reviewState = safeGetObjAsString(annotObj, AnnotUtils.Key_State);
                            String reviewStateModel = safeGetObjAsString(annotObj, AnnotUtils.Key_StateModel);
                            int iReviewState = -1;
                            if (reviewStateModel != null) {
                                // only take state into account if we got state model
                                iReviewState = AnnotReviewState.NONE.getValue();
                                AnnotReviewState stateEnum = reviewState != null ? AnnotReviewState.from(reviewState) : null;
                                if (stateEnum != null) {
                                    iReviewState = stateEnum.getValue();
                                }
                            }
                            annotationEntity.setReviewState(iReviewState);

                            if (authorId != null) {
                                annotationEntity.setAuthorId(authorId);
                                annotationEntity.setAuthorName(authorId);
                            }
                            if (content != null) {
                                annotationEntity.setContents(content);
                            }
                            if (inReplyTo != null) {
                                annotationEntity.setInReplyTo(inReplyTo);
                            }
                            if (creationDate != null) {
                                annotationEntity.setCreationDate(creationDate);
                            }
                            if (date != null) {
                                annotationEntity.setDate(date);
                            }
                            if (page != null) {
                                annotationEntity.setPage(page + 1);
                            }
                            annotationEntity.setType(type);
                            annotationEntity.setYPos(yPos);
                            annotationEntity.setColor(color);
                            annotationEntity.setOpacity(opacity);

                            if (isValidInsertEntity(annotationEntity)) {
                                map.put(id, annotationEntity);
                            }
                        }
                    }
                    String processedXfdf = validateXfdf(fdfDoc.saveAsXFDF(), action);
                    return new Pair<>(processedXfdf, map);
                }
            }
        } catch (Exception ex) {
            Logger.INSTANCE.LogE(TAG, "error parsing XML: " + xfdf);
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (fdfDoc != null) {
                Utils.closeQuietly(fdfDoc);
            }
        }
        return null;
    }

    private static String safeGetObjAsString(Obj obj, String key) throws PDFNetException {
        if (obj != null) {
            Obj result = obj.findObj(key);
            if (result != null && result.isString()) {
                return result.getAsPDFText();
            }
        }
        return null;
    }

    private static Integer safeGetObjAsInteger(Obj obj, String key) throws PDFNetException {
        if (obj != null) {
            Obj result = obj.findObj(key);
            if (result != null && result.isNumber()) {
                double number = result.getNumber();
                return (int) number;
            }
        }
        return null;
    }

    /**
     * Converts date string to {@link java.util.Date}
     */
    @Nullable
    public static Date deserializeDate(String dateStr) {
        if (dateStr != null) {
            try {
                int year = Integer.parseInt(dateStr.substring(2, 6), 10);
                int month = Integer.parseInt(dateStr.substring(6, 8), 10) - 1;
                int day = Integer.parseInt(dateStr.substring(8, 10), 10);
                int hour = Integer.parseInt(dateStr.substring(10, 12), 10);
                int minute = Integer.parseInt(dateStr.substring(12, 14), 10);
                int second = Integer.parseInt(dateStr.substring(14, 16), 10);

                TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
                Calendar calendar = Calendar.getInstance(utcTimeZone);
                calendar.set(year, month, day, hour, minute, second);

                String timezone = dateStr.substring(16);
                // e.g. -08'00' or Z00'00'
                if (timezone.length() == 7) {
                    String timezoneDirection = timezone.substring(0, 1);
                    if (!timezoneDirection.equals("Z")) {
                        // if the timezone is subtracting from UTC then we need to add back the hours
                        int timezoneMultiplier = timezoneDirection.equals("-") ? 1 : -1;
                        int timezoneHours = Integer.parseInt(timezone.substring(1, 3), 10);
                        int timezoneMinutes = Integer.parseInt(timezone.substring(4, 6), 10);
                        hour = hour + (timezoneMultiplier * timezoneHours);
                        minute = minute + (timezoneMultiplier * timezoneMinutes);
                        calendar.set(year, month, day, hour, minute, second);
                    }
                }

                int offset = utcTimeZone.getRawOffset() + utcTimeZone.getDSTSavings();
                long localTime = calendar.getTimeInMillis() + offset;
                return new Date(localTime);
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            }
        }
        return null;
    }

    /**
     * Parse reply information from {@link AnnotationEntity}
     */
    @Nullable
    public static ReplyEntity parseReplyEntity(@NonNull AnnotationEntity annotation) {
        if (annotation.getId() != null &&
                annotation.getAuthorId() != null &&
                annotation.getInReplyTo() != null &&
                annotation.getContents() != null &&
                annotation.getCreationDate() != null &&
                annotation.getDate() != null) {
            ReplyEntity replyEntity = new ReplyEntity();
            replyEntity.setId(annotation.getId());
            replyEntity.setAuthorId(annotation.getAuthorId());
            replyEntity.setAuthorName(annotation.getAuthorName());
            replyEntity.setInReplyTo(annotation.getInReplyTo());
            replyEntity.setContents(annotation.getContents());
            replyEntity.setCreationDate(annotation.getCreationDate());
            replyEntity.setDate(annotation.getDate());
            replyEntity.setPage(annotation.getPage());
            replyEntity.setReviewState(annotation.getReviewState());
            return replyEntity;
        }
        return null;
    }

    /**
     * Parse XFDF information from JSON into {@link AnnotationEntity}
     */
    @Nullable
    public static AnnotationEntity parseAnnotationEntity(String documentId, JSONObject annotation) {
        AnnotationEntity entity = xfdfToAnnotationEntity(documentId, annotation);
        if (entity == null) {
            return null;
        }
        if (documentId != null) {
            entity.setDocumentId(documentId);
        }

        if (annotation.has(Keys.ANNOT_ID) &&
                annotation.has(Keys.ANNOT_XFDF)) {
            return entity;
        }
        return null;
    }

    /**
     * Parse required fields for {@link AnnotationEntity} from its XFDF information
     */
    public static void fillAnnotationEntity(@NonNull String documentId, @NonNull AnnotationEntity input) {
        HashMap<String, AnnotationEntity> xfdfMap = parseXfdf(documentId, CustomServiceUtils.getXfdfFromFile(input.getXfdf()));
        if (xfdfMap != null) {
            AnnotationEntity source = xfdfMap.get(input.getId());
            if (source != null) {
                input.setId(source.getId());
                input.setAuthorId(source.getAuthorId());
                input.setDocumentId(documentId);
                if (input.getAuthorName() == null) {
                    input.setAuthorName(source.getAuthorName());
                }
                Date creationDate = source.getCreationDate();
                if (creationDate != null) {
                    input.setCreationDate(creationDate);
                }
                Date date = source.getDate();
                if (date != null) {
                    input.setDate(date);
                }
                input.setReviewState(source.getReviewState());
                input.setYPos(source.getYPos());
                input.setColor(source.getColor());
                input.setOpacity(source.getOpacity());
                input.setPage(source.getPage());
                input.setType(source.getType());
                String content = source.getContents();
                if (content != null) {
                    input.setContents(content);
                }
                String inReplyTo = source.getInReplyTo();
                if (inReplyTo != null) {
                    input.setInReplyTo(inReplyTo);
                }
            }
        }
    }

    /**
     * Creates {@link AnnotationEntity} from JSON that contains required information
     */
    @Nullable
    public static AnnotationEntity xfdfToAnnotationEntity(String documentId, JSONObject annotation) {
        AnnotationEntity annotationEntity = new AnnotationEntity();
        try {
            if (annotation.has(Keys.ANNOT_ID)) {
                annotationEntity.setId(annotation.getString(Keys.ANNOT_ID));
            }
            if (annotation.has(Keys.ANNOT_DOCUMENT_ID)) {
                annotationEntity.setDocumentId(annotation.getString(Keys.ANNOT_DOCUMENT_ID));
            }
            if (annotation.has(Keys.ANNOT_AUTHOR_ID)) {
                annotationEntity.setAuthorId(annotation.getString(Keys.ANNOT_AUTHOR_ID));
            }
            if (annotation.has(Keys.ANNOT_AUTHOR_NAME)) {
                annotationEntity.setAuthorName(annotation.getString(Keys.ANNOT_AUTHOR_NAME));
            }
            if (annotation.has(Keys.ANNOT_PARENT)) {
                annotationEntity.setParent(annotation.getString(Keys.ANNOT_PARENT));
            }
            if (annotation.has(Keys.ANNOT_XFDF)) {
                String xfdf = annotation.getString(Keys.ANNOT_XFDF);
                annotationEntity.setXfdf(xfdf);
                fillAnnotationEntity(documentId, annotationEntity);
            }
            if (annotation.has(Keys.ANNOT_ACTION)) {
                annotationEntity.setAt(annotation.getString(Keys.ANNOT_ACTION));
            }
            annotationEntity.setUnreadCount(0);
            return annotationEntity;
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
        return null;
    }

    /**
     * Gets delete XFDF command
     */
    public static String wrapDeleteXfdf(String id) {
        return "<delete><id>" + id + "</id></delete>";
    }

    /**
     * Converts JSON with annotation properties to list of {@link AnnotationEntity}
     */
    @NonNull
    public static ArrayList<AnnotationEntity> convToAnnotations(
            @NonNull String xfdfJSON, @NonNull String xfdfCommand,
            @NonNull String documentId, @Nullable String userName) throws JSONException {
        return convToAnnotations(xfdfJSON, xfdfCommand, documentId, userName, null);
    }

    /**
     * Converts JSON with annotation properties to list of {@link AnnotationEntity}
     * This version will fetch from existing annotation entity
     */
    @NonNull
    public static ArrayList<AnnotationEntity> convToAnnotations(
            @NonNull String xfdfJSON, @NonNull String xfdfCommand,
            @NonNull String documentId, @Nullable String userName, @Nullable AnnotationDao annotationDao) throws JSONException {
        ArrayList<AnnotationEntity> annotationList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(xfdfJSON);
        if (jsonObject.has(Keys.CORE_DATA_ANNOTS)) {
            JSONArray annots = jsonObject.getJSONArray(Keys.CORE_DATA_ANNOTS);
            for (int i = 0; i < annots.length(); i++) {
                boolean canAdd = true;
                JSONObject xfdfObj = annots.getJSONObject(i);
                String annotId = null;
                if (xfdfObj.has(Keys.CORE_DATA_ID)) {
                    annotId = xfdfObj.getString(Keys.CORE_DATA_ID);
                } else {
                    canAdd = false;
                }
                AnnotationEntity annotationEntity = null;
                if (annotationDao != null && annotId != null) {
                    annotationEntity = annotationDao.getAnnotationSync(annotId);
                }
                if (annotationEntity == null) {
                    annotationEntity = new AnnotationEntity();
                    if (annotId != null) {
                        annotationEntity.setId(annotId);
                    }
                }
                annotationEntity.setDocumentId(documentId);
                String op = "";
                if (xfdfObj.has(Keys.CORE_DATA_OP)) {
                    op = xfdfObj.getString(Keys.CORE_DATA_OP);
                    annotationEntity.setAt(op);
                } else {
                    canAdd = false;
                }
                if (xfdfObj.has(Keys.CORE_DATA_ID)) {
                    annotationEntity.setId(xfdfObj.getString(Keys.CORE_DATA_ID));
                } else {
                    canAdd = false;
                }
                if (op.equals(OP_ADD)) {
                    if (xfdfObj.has(Keys.CORE_DATA_AUTHOR)) {
                        annotationEntity.setAuthorId(xfdfObj.getString(Keys.CORE_DATA_AUTHOR));
                    }
                    if (userName != null) {
                        annotationEntity.setAuthorName(userName);
                    }
                }
                annotationEntity.setXfdf(xfdfCommand);
                if (canAdd) {
                    annotationList.add(annotationEntity);
                }
            }
        }
        return annotationList;
    }

    @Nullable
    public static String prepareAnnotation(@NonNull String action,
            @NonNull ArrayList<AnnotationEntity> annotationEntities,
            @NonNull String documentId,
            @Nullable String userName) throws JSONException {
        JSONObject result = new JSONObject();
        JSONArray resultArray = new JSONArray();

        for (AnnotationEntity annotation : annotationEntities) {
            JSONObject resultObj = new JSONObject();
            String op = annotation.getAt();
            if (op != null) {
                switch (op) {
                    case OP_ADD:
                        resultObj.put(Keys.WS_DATA_AT, WS_CREATE);
                        break;
                    case OP_REMOVE:
                        resultObj.put(Keys.WS_DATA_AT, WS_DELETE);
                        break;
                    default:
                        resultObj.put(Keys.WS_DATA_AT, op);
                        break;
                }
                resultObj.put(Keys.WS_DATA_AID, annotation.getId());
                if (op.equals(OP_ADD)) {
                    resultObj.put(Keys.WS_DATA_AUTHOR, annotation.getAuthorId());
                    if (userName != null) {
                        resultObj.put(Keys.WS_DATA_ANAME, userName);
                    }
                }
                if (op.equals(OP_ADD) || op.equals(OP_MODIFY)) {
                    resultObj.put(Keys.WS_DATA_XFDF, CustomServiceUtils.getXfdfFromFile(annotation.getXfdf()));
                }
                resultArray.put(resultObj);
            }
        }
        if (resultArray.length() > 0) {
            result.put(Keys.WS_DATA_ANNOTS, resultArray);
            result.put(Keys.WS_ACTION_KEY_T, "a_" + action);
            result.put(Keys.WS_DATA_DID, documentId);
            return result.toString();
        }
        return null;
    }

    public static boolean isValidInsertEntity(@NonNull AnnotationEntity entity) {
        return entity.getId() != null &&
                entity.getDocumentId() != null &&
                entity.getAuthorId() != null &&
                entity.getXfdf() != null &&
                entity.getCreationDate() != null &&
                entity.getDate() != null;
    }
}
