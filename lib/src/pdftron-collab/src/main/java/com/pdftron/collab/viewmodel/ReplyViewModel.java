package com.pdftron.collab.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.pdftron.collab.DataRepository;
import com.pdftron.collab.db.entity.AnnotationEntity;
import com.pdftron.collab.db.entity.ReplyEntity;

import java.util.List;

import io.reactivex.Flowable;

/**
 * ViewModel for annotation reply
 */
public class ReplyViewModel extends AndroidViewModel {

    private Flowable<List<ReplyEntity>> mReplies;
    private Flowable<ReplyEntity> mReplyReviewState;
    private Flowable<AnnotationEntity> mParentAnnotation;

    public ReplyViewModel(@NonNull Application application, String annotationId) {
        super(application);

        DataRepository repository = DataRepository.getInstance(application);

        mReplies = repository.getReplies(annotationId);
        mReplyReviewState = repository.getReplyReviewState(annotationId);
        mParentAnnotation = repository.getAnnotation(annotationId);
    }

    public Flowable<List<ReplyEntity>> getReplies() {
        return mReplies;
    }

    public Flowable<ReplyEntity> getReplyReviewState() {
        return mReplyReviewState;
    }

    public Flowable<AnnotationEntity> getParentAnnotation() {
        return mParentAnnotation;
    }

    public static class Factory extends ViewModelProvider.NewInstanceFactory {

        @NonNull
        private final Application mApplication;

        private final String mAnnotationId;

        public Factory(@NonNull Application application, String annotationId) {
            mApplication = application;
            mAnnotationId = annotationId;
        }

        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            //noinspection unchecked
            return (T) new ReplyViewModel(mApplication, mAnnotationId);
        }
    }
}
