package com.pdftron.collab.ui.viewer;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.collab.db.CollabDatabase;
import com.pdftron.collab.db.entity.AnnotationEntity;
import com.pdftron.collab.db.entity.DocumentEntity;
import com.pdftron.collab.service.CustomService;
import com.pdftron.collab.service.CustomServiceUtils;
import com.pdftron.collab.utils.XfdfUtils;
import com.pdftron.collab.viewmodel.DocumentViewModel;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/**
 * The class responsible for import/export XFDF and/or XFDF command string.
 */
public class CollabManager implements CustomService {

    private static String TAG = "CollabManager";

    @Nullable
    protected String mDocumentId;

    private boolean mStarted;

    private final CompositeDisposable mDisposables = new CompositeDisposable();

    @Nullable
    private String mLastXfdf;

    public interface CollabManagerListener {
        /**
         * Called when local annotation changes need to be sent to the remote source
         *
         * @param action      one of add/modify/delete
         * @param annotations the array of changed annotations
         * @param documentId  the document identifier
         * @param userName    optional user name, user unique identifier should be part of the XFDF command instead
         */
        void onSendAnnotation(String action, ArrayList<AnnotationEntity> annotations, String documentId, @Nullable String userName);
    }

    public interface AnnotationCompletionListener {
        /**
         * Called when remote annotations have been imported to the document.
         * This call corresponds to a {@link #importAnnotationCommand(String)} call.
         */
        void onRemoteChangeImported();
    }

    interface AdvancedViewerListener {
        @Nullable
        PDFViewCtrl getPdfViewCtrl();
    }

    private CollabManagerListener mListener;
    private AnnotationCompletionListener mAnnotationCompletionListener;

    private AdvancedViewerListener mAdvancedViewerListener;

    /**
     * Sets the {@link CollabManagerListener}
     */
    public void setCollabManagerListener(CollabManagerListener listener) {
        mListener = listener;
    }

    /**
     * Sets the {@link AnnotationCompletionListener}
     */

    public void setAnnotationCompletionListener(AnnotationCompletionListener listener) {
        mAnnotationCompletionListener = listener;
    }

    void setAdvancedViewerListener(AdvancedViewerListener listener) {
        mAdvancedViewerListener = listener;
    }

    /**
     * Gets the {@link AnnotationCompletionListener}
     */
    public AnnotationCompletionListener getAnnotationCompletionListener() {
        return mAnnotationCompletionListener;
    }

    private CollabDatabase mCollabDatabase;
    private DocumentViewModel mDocumentViewModel;

    CollabManager(CollabDatabase db, DocumentViewModel viewModel) {
        mCollabDatabase = db;
        mDocumentViewModel = viewModel;
        cleanup();
        viewModel.setCustomConnection(this);
    }

    void destroy() {
        mDisposables.clear();
    }

    public DocumentViewModel getViewModel() {
        return mDocumentViewModel;
    }

    @Override
    public void sendAnnotation(String action, String xfdfCommand, ArrayList<AnnotationEntity> annotations, String documentId, @Nullable String userName) {
        if (mDocumentId == null) {
            Log.w(TAG, "No collaboration document associated.");
            return;
        }
        // optimistic handling
        for (AnnotationEntity entity : annotations) {
            XfdfUtils.fillAnnotationEntity(mDocumentId, entity);
            if (entity.getAt() != null) {
                switch (entity.getAt()) {
                    case XfdfUtils.OP_ADD:
                        addAnnotation(entity);
                        break;
                    case XfdfUtils.OP_MODIFY:
                        modifyAnnotation(entity);
                        break;
                    case XfdfUtils.OP_REMOVE:
                        deleteAnnotation(entity);
                        break;
                }
            }
        }
        mLastXfdf = xfdfCommand;
        if (mListener != null) {
            mListener.onSendAnnotation(action, annotations, documentId, userName);
        }
    }

    /**
     * Gets the last XFDF command string
     */
    @Nullable
    public String getLastXfdf() {
        return mLastXfdf;
    }

    /**
     * Uses a custom {@link CustomService} instead of the default one
     *
     * @param connection the custom {@link CustomService}
     *                   <p>
     *                   Note: CollabTabHostListener.onSendAnnotation will not be raised if a custom service is used
     */
    public void setCustomConnection(@NonNull CustomService connection) {
        mDocumentViewModel.setCustomConnection(connection);
    }

    /**
     * Sets the current user
     *
     * @param userId   the unique identifier of the user
     * @param userName the name of the user
     */
    public void setCurrentUser(@NonNull String userId, @Nullable String userName) {
        mDisposables.add(setCurrentUserImpl(userId, userName).subscribeOn(Schedulers.io()).subscribe());
        mStarted = true;
    }

    private Completable setCurrentUserImpl(@NonNull String userId, @Nullable String userName) {
        return Completable.fromAction(() -> setCurrentUser(mCollabDatabase, userId, userName));
    }

    /**
     * Adds a user
     *
     * @param userId   the unique identifier of the user
     * @param userName the name of the user
     */
    public void addUser(@NonNull String userId, @Nullable String userName) {
        mDisposables.add(addUserImpl(userId, userName).subscribeOn(Schedulers.io()).subscribe());
    }

    private Completable addUserImpl(@NonNull String userId, @Nullable String userName) {
        return Completable.fromAction(() -> addUser(mCollabDatabase, userId, userName));
    }

    /**
     * Sets the current document
     *
     * @param documentId the unique identifier of the document
     */
    public void setCurrentDocument(@NonNull String documentId) {
        mDocumentId = documentId;
        mDisposables.add(addDocumentImpl(documentId).subscribeOn(Schedulers.io()).subscribe());
    }

    private Completable addDocumentImpl(@NonNull String documentId) {
        return Completable.fromAction(() -> addDocument(mCollabDatabase, documentId));
    }

    /**
     * Sets the current document
     *
     * @param entity the {@link DocumentEntity}
     */
    public void setCurrentDocument(@NonNull DocumentEntity entity) {
        mDocumentId = entity.getId();
        mDisposables.add(addDocumentImpl(entity).subscribeOn(Schedulers.io()).subscribe());
    }

    /**
     * Gets the current document
     *
     * @return the document ID
     */
    @Nullable
    public String getCurrentDocument() {
        return mDocumentId;
    }

    private Completable addDocumentImpl(@NonNull DocumentEntity entity) {
        return Completable.fromAction(() -> addDocument(mCollabDatabase, entity));
    }

    /**
     * From remote: add annotations, commonly used for syncing initial annotations
     *
     * @param annotations map of key=id, value={@link AnnotationEntity}
     */
    public void addAnnotations(HashMap<String, AnnotationEntity> annotations) {
        mDisposables.add(addAnnotationsImpl(annotations).subscribeOn(Schedulers.io()).subscribe());
    }

    private Completable addAnnotationsImpl(HashMap<String, AnnotationEntity> annotations) {
        return Completable.fromAction(() -> addAnnotations(mCollabDatabase, annotations));
    }

    /**
     * Imports the XFDF string to the document
     *
     * @param xfdf          the XFDF string
     * @param isInitialLoad whether this is the initial load XFDF string
     */
    public void importAnnotations(@NonNull String xfdf, boolean isInitialLoad) {
        mDisposables.add(importAnnotationsImpl(xfdf, isInitialLoad).subscribeOn(Schedulers.io()).subscribe());
    }

    private Completable importAnnotationsImpl(@NonNull String xfdf, boolean isInitialLoad) {
        return Completable.fromAction(() -> importAnnotations(mCollabDatabase, mDocumentId, xfdf, isInitialLoad));
    }

    /**
     * Imports the XFDF command string to the document
     *
     * @param xfdfCommand   the XFDF command string
     * @param isInitialLoad whether this is the initial load XFDF string
     */
    public void importAnnotationCommand(@NonNull String xfdfCommand, boolean isInitialLoad) {
        mDisposables.add(importAnnotationCommandImpl(xfdfCommand, isInitialLoad).subscribeOn(Schedulers.io()).subscribe());
    }

    /**
     * Imports the XFDF command string to the document, commonly used for syncing initial annotations
     *
     * @param xfdfCommand the XFDF command string
     */
    public void importAnnotationCommand(@NonNull String xfdfCommand) {
        importAnnotationCommand(xfdfCommand, false);
    }

    private Completable importAnnotationCommandImpl(@NonNull String xfdfCommand, boolean isInitialLoad) {
        return Completable.fromAction(() -> importAnnotationCommand(mCollabDatabase, mDocumentId, xfdfCommand, isInitialLoad));
    }

    /**
     * From remote: add annotation
     *
     * @param annotation the {@link AnnotationEntity}
     */
    public void addAnnotation(@NonNull AnnotationEntity annotation) {
        if (!XfdfUtils.isValidInsertEntity(annotation)) {
            throw new IllegalArgumentException("The AnnotationEntity must fulfill all NonNull fields.");
        }
        mDisposables.add(addAnnotationImpl(annotation).subscribeOn(Schedulers.io()).subscribe());
    }

    private Completable addAnnotationImpl(@NonNull AnnotationEntity annotation) {
        return Completable.fromAction(() -> addAnnotation(mCollabDatabase, annotation));
    }

    /**
     * From remote: modify annotation
     *
     * @param annotation the {@link AnnotationEntity}
     */
    public void modifyAnnotation(@NonNull AnnotationEntity annotation) {
        mDisposables.add(modifyAnnotationImpl(annotation).subscribeOn(Schedulers.io()).subscribe());
    }

    private Completable modifyAnnotationImpl(@NonNull AnnotationEntity annotation) {
        return Completable.fromAction(() -> modifyAnnotation(mCollabDatabase, annotation));
    }

    /**
     * Updates annotation entry with the server database Id if different from annotation Id
     *
     * @param annotId  the annotation Id
     * @param serverId the server database Id if different from annotation Id
     */
    public void updateAnnotationServerId(@NonNull String annotId, @NonNull String serverId) {
        mDisposables.add(updateAnnotationServerIdImpl(annotId, serverId).subscribeOn(Schedulers.io()).subscribe());
    }

    private Completable updateAnnotationServerIdImpl(@NonNull String annotId, @NonNull String serverId) {
        return Completable.fromAction(() -> CustomServiceUtils.updateServerId(mCollabDatabase, annotId, serverId));
    }

    public void updateAnnotationServerIdSync(@NonNull String annotId, @NonNull String serverId) {
        CustomServiceUtils.updateServerId(mCollabDatabase, annotId, serverId);
    }

    /**
     * From remote: delete annotation
     *
     * @param annotId the unique identification of the annotation
     */
    public void deleteAnnotation(@NonNull String annotId) {
        mDisposables.add(deleteAnnotationImpl(annotId).subscribeOn(Schedulers.io()).subscribe());
    }

    private Completable deleteAnnotationImpl(@NonNull String annotId) {
        return Completable.fromAction(() -> deleteAnnotation(mCollabDatabase, annotId));
    }

    /**
     * From remote: delete annotation
     *
     * @param annotation the {@link AnnotationEntity}
     */
    public void deleteAnnotation(@NonNull AnnotationEntity annotation) {
        mDisposables.add(deleteAnnotationImpl(annotation).subscribeOn(Schedulers.io()).subscribe());
    }

    private Completable deleteAnnotationImpl(@NonNull AnnotationEntity annotation) {
        return Completable.fromAction(() -> deleteAnnotation(mCollabDatabase, annotation));
    }

    @Nullable
    public List<AnnotationEntity> getAnnotations() {
        if (mDocumentId == null) {
            return null;
        }
        if (mAdvancedViewerListener != null) {
            PDFViewCtrl pdfViewCtrl = mAdvancedViewerListener.getPdfViewCtrl();
            if (pdfViewCtrl != null) {
                ArrayList<AnnotationEntity> annotations = new ArrayList<>();
                boolean shouldUnlockRead = false;
                try {
                    pdfViewCtrl.docLockRead();
                    shouldUnlockRead = true;

                    int pageNum = pdfViewCtrl.getDoc().getPageCount();
                    for (int i = 1; i <= pageNum; i++) {
                        ArrayList<Annot> arr = pdfViewCtrl.getAnnotationsOnPage(i);
                        for (Annot annot : arr) {
                            if (annot.isValid()) {
                                AnnotationEntity annotationEntity = XfdfUtils.toAnnotationEntity(
                                        pdfViewCtrl.getDoc(), mDocumentId, annot
                                );
                                if (annotationEntity != null) {
                                    annotations.add(annotationEntity);
                                }
                            }
                        }
                    }
                    return annotations;
                } catch (Exception e) {
                    return null;
                } finally {
                    if (shouldUnlockRead) {
                        pdfViewCtrl.docUnlockRead();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Cleanup all local cache
     */
    public void cleanup() {
        mDisposables.add(cleanupImpl().subscribeOn(Schedulers.newThread()).subscribe());
    }

    private Completable cleanupImpl() {
        return Completable.fromAction(() -> cleanup(mCollabDatabase));
    }

    /**
     * Returns whether the collaboration session has a user registered
     * If true, it is safe to start to subscribe events, false otherwise
     */
    public boolean isStarted() {
        return mStarted;
    }
}
