package com.pdftron.collab.ui.annotlist.component;

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

import com.pdftron.collab.ui.annotlist.CollabAnnotationListSorter;
import com.pdftron.collab.ui.annotlist.component.view.AnnotationListEvent;
import com.pdftron.collab.ui.annotlist.component.view.AnnotationListUIView;
import com.pdftron.collab.ui.base.component.BaseUIComponent;
import com.pdftron.collab.viewmodel.AnnotationViewModel;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Logger;

import java.util.ArrayList;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * A {@link BaseUIComponent} representing the annotation list. Responsible for updating
 * changes from {@link AnnotationViewModel} to the {@link AnnotationListViewModel}. Also in charge of
 * updating the {@link AnnotationListViewModel} when the data order is changes via sorting.
 */
public class AnnotationListUIComponent extends
        BaseUIComponent<AnnotationListUIView, AnnotationListEvent, AnnotationListViewModel> {

    public static final String TAG = AnnotationListUIComponent.class.getName();

    public AnnotationListUIComponent(@NonNull ViewGroup parent,
            @NonNull LifecycleOwner lifecycleOwner,
            @NonNull AnnotationListViewModel uiViewModel,
            @NonNull AnnotationViewModel annotViewModel,
            @NonNull PDFViewCtrl mPdfViewCtrl,
            @NonNull CollabAnnotationListSorter sorter,
            @NonNull ArrayList<Integer> excludedAnnotationListTypes) {

        super(parent, lifecycleOwner, uiViewModel, uiViewModel.getAnnotationListSubject());

        mDisposables.add(
                annotViewModel.getAnnotations()
                        .flatMap(listAnnots -> Flowable.fromIterable(listAnnots).filter(entity -> !excludedAnnotationListTypes.contains(entity.getType())).toList().toFlowable())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(annotationEntities -> {
                            if (annotationEntities != null) {
                                Logger.INSTANCE.LogD(TAG, "annotations: " + annotationEntities.size());
                                uiViewModel.setAnnotationList(sorter.getAnnotationList(parent.getContext(), mPdfViewCtrl, annotationEntities));
                            }
                        }, throwable -> AnalyticsHandlerAdapter.getInstance().sendException(new Exception(throwable)))
        );

        // This initializes the view state, and also updates the view on data changes
        uiViewModel.getAnnotationListLiveData().observe(lifecycleOwner, annotList -> {
            if (annotList != null) {
                mView.setAnnotList(annotList);
            }
        });
    }

    @NonNull
    @Override
    protected AnnotationListUIView inflateUIView(@NonNull ViewGroup parent) {
        return new AnnotationListUIView(parent);
    }
}
