package com.pdftron.collab.service;

import androidx.annotation.Nullable;

import com.pdftron.collab.db.CollabDatabase;
import com.pdftron.collab.db.entity.AnnotationEntity;
import com.pdftron.collab.db.entity.DocumentEntity;
import com.pdftron.pdf.tools.AnnotManager;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Interface for functions required to observe incoming and outgoing annotation changes.
 * Default implementation is provided for merging incoming changes.
 */
public interface CustomService {

    /**
     * Set the current user
     * Must run on background thread
     *
     * @param db       the {@link CollabDatabase}
     * @param userId   the unique identifier of the user
     * @param userName the name of the user
     */
    default void setCurrentUser(CollabDatabase db, String userId, String userName) {
        CustomServiceUtils.setCurrentUser(db, userId, userName);
    }

    /**
     * Add a user
     * Must run on background thread
     *
     * @param db       the {@link CollabDatabase}
     * @param userId   the unique identifier of the user
     * @param userName the name of the user
     */
    default void addUser(CollabDatabase db, String userId, String userName) {
        CustomServiceUtils.addUser(db, userId, userName);
    }

    /**
     * Set current document
     * Must run on background thread
     *
     * @param db         the {@link CollabDatabase}
     * @param documentId the unique identifier of the document
     */
    default void addDocument(CollabDatabase db, String documentId) {
        CustomServiceUtils.addDocument(db, documentId);
    }

    /**
     * Set current document
     * Must run on background thread
     *
     * @param db     the {@link CollabDatabase}
     * @param entity the {@link DocumentEntity}
     */
    default void addDocument(CollabDatabase db, DocumentEntity entity) {
        CustomServiceUtils.addDocument(db, entity);
    }

    /**
     * Send local change
     *
     * @param action      one of add/modify/delete
     * @param xfdfCommand the annotation XFDF command string
     * @param annotations the annotation XFDF information
     * @param documentId  the document identifier
     * @param userName    optional user name, user unique identifier should be part of the XFDF command instead
     */
    void sendAnnotation(String action, String xfdfCommand, ArrayList<AnnotationEntity> annotations, String documentId, @Nullable String userName);

    /**
     * From remote: add annotations, commonly used for syncing initial annotations
     * Must run on background thread
     *
     * @param db          the {@link CollabDatabase}
     * @param annotations map of key=id, value={@link AnnotationEntity}
     */
    default void addAnnotations(CollabDatabase db, HashMap<String, AnnotationEntity> annotations) {
        String xfdf = CustomServiceUtils.addAnnotations(db, annotations);
        CustomServiceUtils.addLastXfdf(db, AnnotManager.AnnotationAction.ADD, xfdf);
    }

    /**
     * From remote: add annotation
     * Must run on background thread
     *
     * @param db         the {@link CollabDatabase}
     * @param annotation the {@link AnnotationEntity}
     */
    default void addAnnotation(CollabDatabase db, AnnotationEntity annotation) {
        CustomServiceUtils.addAnnotation(db, annotation);
    }

    /**
     * From remote: modify annotation
     * Must run on background thread
     *
     * @param db         the {@link CollabDatabase}
     * @param annotation the {@link AnnotationEntity}
     */
    default void modifyAnnotation(CollabDatabase db, AnnotationEntity annotation) {
        CustomServiceUtils.modifyAnnotation(db, annotation);
    }

    /**
     * From remote: delete annotation
     * Must run on background thread
     *
     * @param db      the {@link CollabDatabase}
     * @param annotId the unique identification of the annotation
     */
    default void deleteAnnotation(CollabDatabase db, String annotId) {
        CustomServiceUtils.deleteAnnotation(db, annotId);
    }

    /**
     * From remote: delete annotation
     * Must run on background thread
     *
     * @param db      the {@link CollabDatabase}
     * @param annotation the {@link AnnotationEntity}
     */
    default void deleteAnnotation(CollabDatabase db, AnnotationEntity annotation) {
        CustomServiceUtils.deleteAnnotation(db, annotation);
    }

    /**
     * Imports the XFDF string to the document
     *
     * @param db            the {@link CollabDatabase}
     * @param documentId    the unique identification of the document
     * @param xfdf          the XFDF string
     * @param isInitialLoad whether this is the initial load XFDF string
     */
    default void importAnnotations(CollabDatabase db, String documentId, String xfdf, boolean isInitialLoad) {
        CustomServiceUtils.importAnnotations(db, documentId, xfdf, isInitialLoad);
    }

    /**
     * Imports the XFDF command string to the document
     *
     * @param db            the {@link CollabDatabase}
     * @param documentId    the unique identification of the document
     * @param xfdfCommand   the XFDF command string
     * @param isInitialLoad whether this is the initial load XFDF string
     */
    default void importAnnotationCommand(CollabDatabase db, String documentId, String xfdfCommand, boolean isInitialLoad) {
        CustomServiceUtils.importAnnotationCommand(db, documentId, xfdfCommand, isInitialLoad);
    }

    /**
     * Cleanup all local cache
     * Must run on background thread
     */
    default void cleanup(CollabDatabase db) {
        CustomServiceUtils.cleanup(db);
    }
}
