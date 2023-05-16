package com.pdftron.collab.service;

import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.collab.db.CollabDatabase;
import com.pdftron.collab.db.entity.AnnotationEntity;
import com.pdftron.collab.db.entity.DocumentEntity;
import com.pdftron.collab.db.entity.LastAnnotationEntity;
import com.pdftron.collab.db.entity.ReplyEntity;
import com.pdftron.collab.db.entity.UserEntity;
import com.pdftron.collab.utils.JsonUtils;
import com.pdftron.collab.utils.Keys;
import com.pdftron.collab.utils.XfdfUtils;
import com.pdftron.pdf.tools.AnnotManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.Utils;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class CustomServiceUtils {

    /**
     * Set the current user
     * Must run on background thread
     *
     * @param db       the {@link CollabDatabase}
     * @param userId   the unique identifier of the user
     * @param userName the name of the user
     */
    public static void setCurrentUser(CollabDatabase db, String userId, String userName) {
        addUser(db, userId, userName);
        db.userDao().updateIsCurrentUser(userId, true);
    }

    /**
     * Add a user
     * Must run on background thread
     *
     * @param db       the {@link CollabDatabase}
     * @param userId   the unique identifier of the user
     * @param userName the name of the user
     */
    public static void addUser(CollabDatabase db, String userId, String userName) {
        UserEntity entity = new UserEntity(userId, userName, System.currentTimeMillis(), null);
        db.userDao().insert(entity);
        // Update all usernames in annotationDB table
        db.annotationDao().updateAuthorName(userId, userName);
    }

    /**
     * Set the current document
     * Must run on background thread
     *
     * @param db         the {@link CollabDatabase}
     * @param documentId the unique identifier of the document
     */
    public static void addDocument(CollabDatabase db, String documentId) {
        DocumentEntity entity = new DocumentEntity(documentId, null,
                System.currentTimeMillis(), null);
        db.documentDao().insert(entity);
    }

    /**
     * Set the current document
     * Must run on background thread
     *
     * @param db     the {@link CollabDatabase}
     * @param entity the {@link DocumentEntity}
     */
    public static void addDocument(CollabDatabase db, DocumentEntity entity) {
        db.documentDao().insert(entity);
    }

    /**
     * From remote: add annotations, commonly used for syncing initial annotations
     * Must run on background thread
     *
     * @param db          the {@link CollabDatabase}
     * @param annotations map of key=id, value={@link AnnotationEntity}
     */
    public static String addAnnotations(CollabDatabase db, HashMap<String, AnnotationEntity> annotations) {
        if (db != null && annotations != null) {
            ArrayList<ReplyEntity> replies = new ArrayList<>();
            HashMap<String, Date> lastReplyDateMap = new HashMap<>();
            HashMap<String, Date> lastReviewStateDateMap = new HashMap<>();
            StringBuilder builder = new StringBuilder();
            for (AnnotationEntity entity : annotations.values()) {
                fillAuthorName(db, entity);
                builder.append(XfdfUtils.validateXfdf(getXfdfFromFile(entity.getXfdf())));
                ReplyEntity replyEntity = XfdfUtils.parseReplyEntity(entity);
                if (replyEntity != null) {
                    String parentId = replyEntity.getInReplyTo();
                    AnnotationEntity aEntity = annotations.get(parentId);
                    // update the last reply for parent annotation
                    boolean canUpdate = false;
                    Date lastReplyDate = lastReplyDateMap.get(parentId);
                    if (null == lastReplyDate) {
                        lastReplyDate = replyEntity.getCreationDate();
                        canUpdate = true;
                        lastReplyDateMap.put(parentId, lastReplyDate);
                    } else if (replyEntity.getCreationDate().compareTo(lastReplyDate) > 0) {
                        lastReplyDate = replyEntity.getCreationDate();
                        canUpdate = true;
                        lastReplyDateMap.put(parentId, lastReplyDate);
                    }
                    if (aEntity != null && canUpdate) {
                        String author = replyEntity.getAuthorName() != null ? replyEntity.getAuthorName() : replyEntity.getAuthorId();
                        aEntity.setLastReplyAuthor(author);
                        aEntity.setLastReplyContents(replyEntity.getContents());
                        aEntity.setLastReplyDate(replyEntity.getCreationDate());
                    }
                    if (replyEntity.getReviewState() != -1) {
                        // update the review state for parent annotation
                        canUpdate = false;
                        Date lastReviewStateDate = lastReviewStateDateMap.get(parentId);
                        if (lastReviewStateDate == null) {
                            lastReviewStateDate = replyEntity.getCreationDate();
                            canUpdate = true;
                            lastReviewStateDateMap.put(parentId, lastReviewStateDate);
                        } else if (replyEntity.getCreationDate().compareTo(lastReviewStateDate) > 0 || (aEntity != null && aEntity.getReviewState() == -1)) {
                            // if it is a newer reply with review state, or if we have not found any state yet, try update
                            lastReviewStateDate = replyEntity.getCreationDate();
                            canUpdate = true;
                            lastReviewStateDateMap.put(parentId, lastReviewStateDate);
                        }
                        if (aEntity != null && canUpdate) {
                            aEntity.setReviewState(replyEntity.getReviewState());
                        }
                    }
                    replies.add(replyEntity);
                }
            }

            db.annotationDao().insertAnnotations(new ArrayList<>(annotations.values()));
            db.replyDao().insertReplies(replies);
            return builder.toString();
        }
        return null;
    }

    /**
     * Add the last XFDF command string to database
     *
     * @param db   the {@link CollabDatabase}
     * @param xfdf the XFDF command string
     */
    public static String addLastXfdf(CollabDatabase db, String action, String xfdf) {
        String xfdfCommand = XfdfUtils.validateXfdf(xfdf, action);
        String key = Keys.ANNOT_XFDF + UUID.randomUUID().toString();
        LastAnnotationEntity lastAnnotationEntity = new LastAnnotationEntity(key, xfdfCommand, System.currentTimeMillis());
        db.lastAnnotationDao().insert(lastAnnotationEntity);
        return key;
    }

    @NonNull
    public static String getXfdfFile(String xfdf) {
        File test;
        try {
            // it is possible that the XFDF passed in is already in the form of a file
            // let's try to see if it is a valid file first
            test = new File(xfdf);
        } catch (Exception ex) {
            test = null;
        }
        try {
            if (test != null && test.exists() && test.isFile()) {
                return test.getAbsolutePath();
            }
            // XFDF needs to be stored to a file
            File tmp = File.createTempFile("tmp", ".txt", CollabDatabase.getXfdfCachePath());
            FileUtils.writeStringToFile(tmp, xfdf, Charsets.UTF_8);
            return tmp.getAbsolutePath();
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
        return "";
    }

    @NonNull
    public static String getXfdfFromFile(@Nullable String filePath) {
        if (filePath != null) {
            try {
                File file = new File(filePath);
                if (file.exists()) {
                    return FileUtils.readFileToString(file, Charsets.UTF_8);
                }
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            }
        }
        return "";
    }

    /**
     * From remote: add annotation
     * Must run on background thread
     *
     * @param db         the {@link CollabDatabase}
     * @param annotation the {@link AnnotationEntity}
     */
    public static void addAnnotation(CollabDatabase db, AnnotationEntity annotation) {
        addAnnotation(db, annotation, true);
    }

    private static void addAnnotation(CollabDatabase db, AnnotationEntity annotation, boolean addLastXfdf) {
        if (db != null && annotation != null) {
            fillAuthorName(db, annotation);
            db.annotationDao().insert(annotation);

            ReplyEntity replyEntity = XfdfUtils.parseReplyEntity(annotation);
            if (replyEntity != null) {
                String parentId = replyEntity.getInReplyTo();
                try {
                    // update the last reply for parent annotation
                    String author = replyEntity.getAuthorName() != null ? replyEntity.getAuthorName() : replyEntity.getAuthorId();
                    db.annotationDao().updateReply(parentId, author, replyEntity.getContents(), replyEntity.getCreationDate());
                    // update review state
                    if (replyEntity.getReviewState() != -1) {
                        db.annotationDao().updateReviewState(parentId, replyEntity.getReviewState());
                    }
                    // update unreads
                    String docId = annotation.getDocumentId();
                    DocumentEntity documentEntity = db.documentDao().getDocumentSync(docId);
                    AnnotationEntity annotationEntity = db.annotationDao().getAnnotationSync(parentId);
                    if (documentEntity != null && annotationEntity != null) {
                        UserEntity userEntity = db.userDao().getCurrentUserSync();
                        String activeAnnot = userEntity.getActiveAnnotation();
                        if (activeAnnot == null ||
                                !activeAnnot.equals(parentId)) {
                            // only update unreads if it is not active
                            String unreads = documentEntity.getUnreads();
                            String newUnreads = JsonUtils.addItemToArray(unreads, parentId);
                            db.documentDao().updateUnreads(docId, newUnreads);
                            unreads = annotationEntity.getUnreads();
                            JSONArray newUnreadsArr = JsonUtils.addItemToArrayImpl(unreads, replyEntity.getId());
                            db.annotationDao().updateUnreads(parentId, newUnreadsArr.toString(), newUnreadsArr.length());
                        }
                    }
                } catch (Exception ex) {
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                }
                db.replyDao().insert(replyEntity);
            }

            if (addLastXfdf) {
                addLastXfdf(db, AnnotManager.AnnotationAction.ADD, getXfdfFromFile(annotation.getXfdf()));
            }
        }
    }

    /**
     * From remote: modify annotation
     * Must run on background thread
     *
     * @param db         the {@link CollabDatabase}
     * @param annotation the {@link AnnotationEntity}
     */
    public static void modifyAnnotation(CollabDatabase db, AnnotationEntity annotation) {
        modifyAnnotation(db, annotation, true);
    }

    private static void modifyAnnotation(CollabDatabase db, AnnotationEntity annotation, boolean addLastXfdf) {
        if (null == db || null == annotation) {
            return;
        }
        db.annotationDao().update(annotation.getId(),
                annotation.getXfdf(),
                annotation.getAt(),
                annotation.getContents(),
                annotation.getYPos(),
                annotation.getColor(),
                annotation.getOpacity(),
                annotation.getDate());

        String contents = annotation.getContents();
        if (contents != null) {
            db.replyDao().update(annotation.getId(), contents, annotation.getPage(), annotation.getDate());

            ReplyEntity replyEntity = db.replyDao().getReplySync(annotation.getId());
            if (replyEntity != null) {
                ReplyEntity lastReply = db.replyDao().getLastReplySync(replyEntity.getInReplyTo());
                if (lastReply != null && replyEntity.getId().equals(lastReply.getId())) {
                    String author = replyEntity.getAuthorName() != null ? replyEntity.getAuthorName() : replyEntity.getAuthorId();
                    db.annotationDao().updateReply(replyEntity.getInReplyTo(), author, contents, replyEntity.getCreationDate());
                }
            }
        }

        if (addLastXfdf) {
            addLastXfdf(db, AnnotManager.AnnotationAction.MODIFY, getXfdfFromFile(annotation.getXfdf()));
        }
    }

    public static void updateServerId(CollabDatabase db, String annotId, String serverId) {
        if (null == db || Utils.isNullOrEmpty(annotId) || Utils.isNullOrEmpty(serverId)) {
            return;
        }
        db.annotationDao().updateServerId(annotId, serverId);
    }

    /**
     * From remote: delete annotation
     * Must run on background thread
     *
     * @param db      the {@link CollabDatabase}
     * @param annotId the unique identification of the annotation
     */
    public static void deleteAnnotation(CollabDatabase db, String annotId) {
        AnnotationEntity annotationEntity = new AnnotationEntity();
        annotationEntity.setId(annotId);
        deleteAnnotation(db, annotationEntity, true);
    }

    /**
     * From remote: delete annotation
     * Must run on background thread
     *
     * @param db         the {@link CollabDatabase}
     * @param annotation the {@link AnnotationEntity}
     */
    public static void deleteAnnotation(CollabDatabase db, AnnotationEntity annotation) {
        deleteAnnotation(db, annotation, true);
    }

    private static void deleteAnnotation(CollabDatabase db, AnnotationEntity annotation, boolean addLastXfdf) {
        if (null == db || null == annotation || null == annotation.getId()) {
            return;
        }
        String annotId = annotation.getId();

        AnnotationEntity deletedAnnot = db.annotationDao().getAnnotationSync(annotId);
        db.annotationDao().delete(annotId);

        ReplyEntity replyEntity = db.replyDao().getReplySync(annotId);
        if (replyEntity != null) {
            // this is a reply
            // let's update the last reply
            ReplyEntity lastReply = db.replyDao().getLastReplySync(replyEntity.getInReplyTo());
            if (lastReply != null && replyEntity.getId().equals(lastReply.getId())) {
                try {
                    List<ReplyEntity> replies = db.replyDao().getSortedRepliesSync(replyEntity.getInReplyTo());
                    String author = null;
                    String contents = null;
                    Date replyDate = null;
                    if (replies != null && replies.size() > 1) {
                        // grab the reply before the one about to get deleted
                        ReplyEntity secondLastReply = replies.get(1);
                        author = secondLastReply.getAuthorName() != null ? secondLastReply.getAuthorName() : secondLastReply.getAuthorId();
                        contents = secondLastReply.getContents();
                        replyDate = secondLastReply.getCreationDate();
                    }
                    db.annotationDao().updateReply(replyEntity.getInReplyTo(), author, contents, replyDate);
                } catch (Exception ex) {
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                }
            }
            if (deletedAnnot != null) {
                String docId = deletedAnnot.getDocumentId();
                DocumentEntity documentEntity = db.documentDao().getDocumentSync(docId);
                if (documentEntity != null) {
                    UserEntity userEntity = db.userDao().getCurrentUserSync();
                    if (userEntity != null) {
                        String activeAnnot = userEntity.getActiveAnnotation();
                        if (activeAnnot == null ||
                                !activeAnnot.equals(replyEntity.getInReplyTo())) {
                            // only update unreads if it is not active
                            try {
                                String unreads = documentEntity.getUnreads();
                                if (unreads != null) {
                                    String newUnreads = JsonUtils.removeItemFromArray(unreads, replyEntity.getInReplyTo());
                                    db.documentDao().updateUnreads(docId, newUnreads);
                                }
                                AnnotationEntity parentAnnot = db.annotationDao().getAnnotationSync(replyEntity.getInReplyTo());
                                unreads = parentAnnot.getUnreads();
                                if (unreads != null) {
                                    JSONArray newUnreadsArr = JsonUtils.removeItemFromArrayImpl(unreads, replyEntity.getId());
                                    if (newUnreadsArr != null) {
                                        db.annotationDao().updateUnreads(replyEntity.getInReplyTo(), newUnreadsArr.toString(), newUnreadsArr.length());
                                    }
                                }
                            } catch (Exception ex) {
                                AnalyticsHandlerAdapter.getInstance().sendException(ex);
                            }
                        }
                    }
                }
            }
        }

        db.replyDao().delete(annotId);

        if (addLastXfdf) {
            String xfdf = annotation.getXfdf();
            if (Utils.isNullOrEmpty(xfdf)) {
                xfdf = XfdfUtils.wrapDeleteXfdf(annotId);
            }
            addLastXfdf(db, AnnotManager.AnnotationAction.DELETE, xfdf);
        }
    }

    /**
     * Imports the XFDF string to the document
     *
     * @param db            the {@link CollabDatabase}
     * @param documentId    the unique identification of the document
     * @param xfdf          the XFDF string
     * @param isInitialLoad whether this is the initial load XFDF string
     */
    public static void importAnnotations(CollabDatabase db, String documentId, String xfdf, boolean isInitialLoad) {
        if (Utils.isNullOrEmpty(documentId) || Utils.isNullOrEmpty(xfdf)) {
            return;
        }
        Pair<String, HashMap<String, AnnotationEntity>> result = XfdfUtils.parseAndProcessXfdf(documentId, xfdf);
        if (result != null) {
            importAnnotationsImpl(db, result.first, result.second, isInitialLoad);
        } else {
            importAnnotationsImpl(db, xfdf, null, isInitialLoad);
        }
    }

    /**
     * Imports the XFDF command string to the document
     *
     * @param db            the {@link CollabDatabase}
     * @param documentId    the unique identification of the document
     * @param xfdfCommand   the XFDF command string
     * @param isInitialLoad whether this is the initial load XFDF string
     */
    public static void importAnnotationCommand(CollabDatabase db, String documentId, String xfdfCommand, boolean isInitialLoad) {
        if (Utils.isNullOrEmpty(documentId) || Utils.isNullOrEmpty(xfdfCommand)) {
            return;
        }
        Pair<String, HashMap<String, AnnotationEntity>> result = XfdfUtils.parseAndProcessXfdfCommand(documentId, xfdfCommand);
        if (result != null) {
            importAnnotationsImpl(db, xfdfCommand, result.second, isInitialLoad);
        } else {
            importAnnotationsImpl(db, xfdfCommand, null, isInitialLoad);
        }
    }

    private static void importAnnotationsImpl(CollabDatabase db, String xfdfCommand, @Nullable HashMap<String, AnnotationEntity> map, boolean isInitialLoad) {
        if (map != null) {
            if (isInitialLoad) {
                addAnnotations(db, map);
                addLastXfdf(db, AnnotManager.AnnotationAction.ADD, xfdfCommand);
            } else {
                // with import XFDF string, we no longer knows whether it is add/modify/delete command
                // thus xml parsing is required
                try {
                    JSONObject jsonObject = AnnotUtils.simpleXmlParser(xfdfCommand);
                    // add
                    JSONArray addArr = jsonObject.getJSONArray(XfdfUtils.XFDF_ADD);
                    for (int i = 0; i < addArr.length(); i++) {
                        String id = addArr.getString(i);
                        AnnotationEntity entity = map.get(id);
                        addAnnotation(db, entity, false);
                    }
                    // modify
                    JSONArray modifyArr = jsonObject.getJSONArray(XfdfUtils.XFDF_MODIFY);
                    for (int i = 0; i < modifyArr.length(); i++) {
                        String id = modifyArr.getString(i);
                        AnnotationEntity entity = map.get(id);
                        modifyAnnotation(db, entity, false);
                    }
                    addLastXfdf(db, AnnotManager.AnnotationAction.MODIFY, xfdfCommand);
                } catch (Exception ex) {
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                }
            }
        } else {
            // delete
            try {
                JSONObject jsonObject = AnnotUtils.simpleXmlParser(xfdfCommand);
                JSONArray deleteArr = jsonObject.getJSONArray(XfdfUtils.XFDF_DELETE);
                for (int i = 0; i < deleteArr.length(); i++) {
                    String id = deleteArr.getString(i);
                    AnnotationEntity annotationEntity = new AnnotationEntity();
                    annotationEntity.setId(id);
                    deleteAnnotation(db, annotationEntity, false);
                }
                addLastXfdf(db, AnnotManager.AnnotationAction.DELETE, xfdfCommand);
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            }
        }
    }

    private static void fillAuthorName(@NonNull CollabDatabase db, @NonNull AnnotationEntity annotation) {
        if (!Utils.isNullOrEmpty(annotation.getAuthorName()) &&
                !annotation.getAuthorId().equals(annotation.getAuthorName())) {
            return;
        }
        // process author id to author name
        String authorId = annotation.getAuthorId();
        UserEntity userEntity = db.userDao().getUserSync(authorId);
        if (userEntity != null) {
            annotation.setAuthorName(userEntity.getName());
        }
    }

    /**
     * Cleanup all local cache
     * Must run on background thread
     */
    public static void cleanup(CollabDatabase db) {
        db.clearAllTables();
        // clear cache path
        try {
            FileUtils.cleanDirectory(CollabDatabase.getXfdfCachePath());
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
    }
}
