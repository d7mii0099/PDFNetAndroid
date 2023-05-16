package com.pdftron.collab.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.pdftron.collab.DataRepository;
import com.pdftron.collab.db.entity.AnnotationEntity;
import com.pdftron.collab.db.entity.LastAnnotationEntity;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;

/**
 * ViewModel for annotation
 */
public class AnnotationViewModel extends AndroidViewModel {

    private DataRepository mRepository;

    private LiveData<List<LastAnnotationEntity>> mLastAnnotations;
    private Flowable<List<AnnotationEntity>> mAnnotations;

    private String mDocumentId;

    public AnnotationViewModel(@NonNull Application application, final String documentId) {
        super(application);

        mDocumentId = documentId;

        mRepository = DataRepository.getInstance(application);

        mLastAnnotations = mRepository.getLastAnnotations();
        mAnnotations = mRepository.getAnnotations(mDocumentId);
    }

    public LiveData<List<LastAnnotationEntity>> getLastAnnotations() {
        return mLastAnnotations;
    }

    public Flowable<List<AnnotationEntity>> getAnnotations() {
        return mAnnotations;
    }

    /**
     * Must run on a background thread.
     */
    @WorkerThread
    public void sendAnnotation(String action, String xfdfCommand, String xfdfJSON, String userName) {
        Utils.throwIfOnMainThread();
        mRepository.sendAnnotation(action, xfdfCommand, xfdfJSON, mDocumentId, userName);
    }

    public Completable consumeLastAnnotations(@NonNull ArrayList<String> ids) {
        return mRepository.consumeLastAnnotations(ids);
    }

    public static class Factory extends ViewModelProvider.NewInstanceFactory {

        @NonNull
        private final Application mApplication;

        private final String mDocumentId;

        public Factory(@NonNull Application application, String documentId) {
            mApplication = application;
            mDocumentId = documentId;
        }

        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            //noinspection unchecked
            return (T) new AnnotationViewModel(mApplication, mDocumentId);
        }
    }
}
