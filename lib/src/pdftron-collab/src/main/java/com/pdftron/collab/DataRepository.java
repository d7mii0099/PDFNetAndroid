package com.pdftron.collab;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;

import com.pdftron.collab.db.CollabDatabase;
import com.pdftron.collab.db.dao.AnnotationDao;
import com.pdftron.collab.db.dao.DocumentDao;
import com.pdftron.collab.db.dao.LastAnnotationDao;
import com.pdftron.collab.db.dao.ReplyDao;
import com.pdftron.collab.db.dao.UserDao;
import com.pdftron.collab.db.entity.AnnotationEntity;
import com.pdftron.collab.db.entity.DocumentEntity;
import com.pdftron.collab.db.entity.LastAnnotationEntity;
import com.pdftron.collab.db.entity.ReplyEntity;
import com.pdftron.collab.db.entity.UserEntity;
import com.pdftron.collab.service.CustomService;
import com.pdftron.collab.utils.JsonUtils;
import com.pdftron.collab.utils.XfdfUtils;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;

/**
 * Repository handling the work with documents and annotations.
 */
public class DataRepository {

    private static volatile DataRepository sInstance;

    private UserDao mUserDao;
    private DocumentDao mDocumentDao;
    private AnnotationDao mAnnotationDao;
    private LastAnnotationDao mLastAnnotationDao;
    private ReplyDao mReplyDao;

    private LiveData<DocumentEntity> mDocument;
    private LiveData<UserEntity> mUser;

    private CustomService mCustomService;

    private DataRepository(Application application) {
        CollabDatabase database = CollabDatabase.getInstance(application);
        mUserDao = database.userDao();
        mDocumentDao = database.documentDao();
        mAnnotationDao = database.annotationDao();
        mLastAnnotationDao = database.lastAnnotationDao();
        mReplyDao = database.replyDao();

        mDocument = mDocumentDao.getDocument();
        mUser = mUserDao.getCurrentUser();
    }

    public static DataRepository getInstance(Application application) {
        if (sInstance == null) {
            synchronized (DataRepository.class) {
                if (sInstance == null) {
                    sInstance = new DataRepository(application);
                }
            }
        }
        return sInstance;
    }

    /**
     * Link to your choice of backend service
     * @param connection the backend service
     */
    public void setCustomService(@NonNull CustomService connection) {
        mCustomService = connection;
    }

    /**
     * Get the active document
     */
    public LiveData<DocumentEntity> getDocument() {
        return mDocument;
    }

    /**
     * Get the active user
     */
    public LiveData<UserEntity> getUser() {
        return mUser;
    }

    /**
     * Get the last annotation
     */
    public LiveData<List<LastAnnotationEntity>> getLastAnnotations() {
        return mLastAnnotationDao.getLastAnnotations();
    }

    /**
     * Get all annotations associated with a document
     * @param documentId the document unique identifier
     */
    public Flowable<List<AnnotationEntity>> getAnnotations(final String documentId) {
        return mAnnotationDao.getAnnotations(documentId);
    }

    /**
     * Get annotation by id
     * @param annotationId the annotation unique identifier
     */
    public Flowable<AnnotationEntity> getAnnotation(final String annotationId) {
        return mAnnotationDao.getAnnotation(annotationId);
    }

    /**
     * Get all annotation replies associated with an annotation
     * @param annotationId the annotation unique identifier
     */
    public Flowable<List<ReplyEntity>> getReplies(final String annotationId) {
        return mReplyDao.getReplies(annotationId);
    }

    /**
     * Gets the annotation reply that indicates the review state of an annotation
     * @param annotationId the annotation unique identifier
     */
    public Flowable<ReplyEntity> getReplyReviewState(final String annotationId) {
        return mReplyDao.getReplyReviewState(annotationId);
    }

    /**
     * Called when client has new annotation events
     * Must run on a background thread.
     * @param action      one of add/modify/delete
     * @param xfdfCommand the annotation XFDF command string
     * @param xfdfJSON the annotation XFDF information
     * @param documentId  the document identifier
     * @param userName    optional user name, user unique identifier should be part of the XFDF command instead
     */
    @WorkerThread
    public void sendAnnotation(String action, String xfdfCommand, String xfdfJSON, String documentId, String userName) {
        Utils.throwIfOnMainThread();
        if (null == mCustomService) {
            throw new RuntimeException("CustomService is required for collaboration.");
        }

        try {
            ArrayList<AnnotationEntity> annotations = XfdfUtils.convToAnnotations(xfdfJSON, xfdfCommand, documentId, userName, mAnnotationDao);
            mCustomService.sendAnnotation(action, xfdfCommand, annotations, documentId, userName);
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
    }

    public Completable consumeLastAnnotations(@NonNull ArrayList<String> ids) {
        return Completable.fromAction(() -> consumeLastAnnotationsImpl(ids));
    }

    public void consumeLastAnnotationsImpl(@NonNull ArrayList<String> ids) {
        mLastAnnotationDao.deleteByIds(ids);
    }

    /**
     * Updates unread count for the document
     * rxjava version
     *
     * @param docId the document id
     * @param annotId the annotation id
     */
    public Completable addDocumentUnread(String docId, @NonNull String annotId) {
        return Completable.fromAction(() -> addDocumentUnreadImpl(docId, annotId));
    }

    /**
     * Update unread count for the document
     * Must run on background thread
     * @param docId the document id
     * @param annotId the annotation id
     * @throws JSONException
     */
    public void addDocumentUnreadImpl(@NonNull String docId, @NonNull String annotId) throws JSONException {
        List<DocumentEntity> documents = mDocumentDao.getDocumentsSync();
        for (DocumentEntity document : documents) {
            if (document != null && document.getId().equals(docId)) {
                String unreads = document.getUnreads();
                String newUnreads = JsonUtils.addItemToArray(unreads, annotId);
                mDocumentDao.updateUnreads(docId, newUnreads);
            }
        }
    }

    /**
     * Updates unread count
     * rxjava version
     *
     * @param docId the document id
     * @param annotId the annotation id
     */
    public Completable removeDocumentUnread(String docId, @NonNull String annotId) {
        return Completable.fromAction(() -> removeDocumentUnreadImpl(docId, annotId));
    }

    /**
     * Updates unread count
     * Must run on background thread
     * @param docId the document id
     * @param annotId the annotation id
     * @throws JSONException
     */
    public void removeDocumentUnreadImpl(@NonNull String docId, @NonNull String annotId) throws JSONException {
        List<DocumentEntity> documents = mDocumentDao.getDocumentsSync();
        for (DocumentEntity document : documents) {
            if (document != null && document.getId().equals(docId)) {
                String unreads = document.getUnreads();
                if (unreads != null) {
                    String newUnreads = JsonUtils.removeAllItemFromArray(unreads, annotId);
                    if (newUnreads != null) {
                        mDocumentDao.updateUnreads(docId, newUnreads);
                    }
                }
            }
        }
    }

    /**
     * Updates last read timestamp for an annotation
     * rxjava version
     *
     * @param annotId the annotation id
     */
    public Completable updateAnnotationUnreads(@NonNull String annotId) {
        return Completable.fromAction(() -> updateAnnotationUnreadsImpl(annotId));
    }

    /**
     * Updates last read timestamp for an annotation
     * Must run on background thread
     *
     * @param annotId the annotation id
     */
    public void updateAnnotationUnreadsImpl(@NonNull String annotId) {
        mAnnotationDao.resetUnreadCount(annotId);
    }

    /**
     * Updates current active annotation
     * rxjava version
     *
     * @param userId  the user id
     * @param annotId the active annotation id
     */
    public Completable updateActiveAnnotation(@NonNull String userId, @NonNull String annotId) {
        return Completable.fromAction(() -> updateActiveAnnotationImpl(userId, annotId));
    }

    /**
     * Updates current active annotation
     * Must run on background thread
     * @param userId the user id
     * @param annotId the active annotation id
     */
    public void updateActiveAnnotationImpl(@NonNull String userId, @NonNull String annotId) {
        mUserDao.update(userId, annotId);
    }
}
