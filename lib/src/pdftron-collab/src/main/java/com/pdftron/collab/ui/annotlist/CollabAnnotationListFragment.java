package com.pdftron.collab.ui.annotlist;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.pdftron.collab.R;
import com.pdftron.collab.ui.annotlist.component.AnnotationListUIComponent;
import com.pdftron.collab.ui.annotlist.component.AnnotationListViewModel;
import com.pdftron.collab.ui.annotlist.model.list.item.AnnotationListContent;
import com.pdftron.collab.viewmodel.AnnotationViewModel;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.controls.AnnotationDialogFragment;
import com.pdftron.pdf.dialog.annotlist.BaseAnnotationListSorter;
import com.pdftron.pdf.dialog.annotlist.BaseAnnotationSortOrder;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.ViewerUtils;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class CollabAnnotationListFragment extends AnnotationDialogFragment {

    private static final String TAG = CollabAnnotationListFragment.class.getName();
    private static final String DOCUMENT_ID_KEY = "CollabAnnotationListFragment_document_id";

    private AnnotationViewModel mAnnotationViewModel;
    private AnnotationListViewModel mListViewModel;
    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    @SuppressWarnings("NullableProblems")
    @NonNull
    private String mDocumentId;

    public static CollabAnnotationListFragment newInstance() {
        return new CollabAnnotationListFragment();
    }

    /**
     * Creates a bundle containing arguments for <code>AnnotationDialogFragment</code>
     *
     * @param documentId unique identifier for the collaborative document
     * @return arguments for <code>AnnotationDialogFragment</code>
     */
    public static Bundle newBundle(@NonNull String documentId) {
        Bundle bundle = new Bundle();
        bundle.putString(DOCUMENT_ID_KEY, documentId);
        return bundle;
    }

    /**
     * Creates a bundle containing arguments for {@link AnnotationDialogFragment}
     *
     * @param documentId              unique identifier for the collaborative document
     * @param isReadOnly              true if the annotation list should be read only (default false)
     * @param isRtl                   true if the the annotations are displayed right-to-left (default false)
     * @param annotationListSortOrder sorting order of the annotations
     * @return arguments for {@link AnnotationDialogFragment}
     */
    public static Bundle newBundle(@NonNull String documentId, boolean isReadOnly, boolean isRtl,
            @NonNull CollabAnnotationListSortOrder annotationListSortOrder) {
        Bundle args = new Bundle();
        args.putBoolean(BUNDLE_IS_READ_ONLY, isReadOnly);
        args.putBoolean(BUNDLE_IS_RTL, isRtl);
        args.putInt(BUNDLE_KEY_SORT_MODE, annotationListSortOrder.value);
        args.putString(DOCUMENT_ID_KEY, documentId);
        return args;
    }

    @NonNull
    @Override
    protected BaseAnnotationSortOrder getSortOrder(Bundle args) {
        return args != null && args.containsKey(BUNDLE_KEY_SORT_MODE) ?
                CollabAnnotationListSortOrder.fromValue(
                        args.getInt(BUNDLE_KEY_SORT_MODE, CollabAnnotationListSortOrder.LAST_ACTIVITY.value)
                ) :
                CollabAnnotationListSortOrder.LAST_ACTIVITY; // default sort by date
    }

    @NonNull
    @Override
    protected BaseAnnotationListSorter getSorter() {
        return ViewModelProviders.of(this,
                new CollabAnnotationListSorter.Factory(mAnnotationListSortOrder))
                .get(CollabAnnotationListSorter.class);
    }

    private final Observer<BaseAnnotationSortOrder> mSortOrderObserver = new Observer<BaseAnnotationSortOrder>() {

        // Helper method to update annotation list sorting order in shared prefs
        private void updateSharedPrefs(CollabAnnotationListSortOrder sortOrder) {
            Context context = getContext();
            if (context != null) {
                PdfViewCtrlSettingsManager.updateAnnotListSortOrder(context,
                        sortOrder);
            }
        }

        @Override
        public void onChanged(@Nullable BaseAnnotationSortOrder sortOrder) {
            if (sortOrder != null) {
                if (sortOrder instanceof CollabAnnotationListSortOrder)
                    switch (((CollabAnnotationListSortOrder) sortOrder)) {
                        case DATE_DESCENDING:
                            updateSharedPrefs(CollabAnnotationListSortOrder.DATE_DESCENDING);
                            mSortOrder = CollabAnnotationListSortOrder.DATE_DESCENDING;
                            break;
                        case POSITION_ASCENDING:
                            updateSharedPrefs(CollabAnnotationListSortOrder.POSITION_ASCENDING);
                            mSortOrder = CollabAnnotationListSortOrder.POSITION_ASCENDING;
                            break;
                        case LAST_ACTIVITY:
                            updateSharedPrefs(CollabAnnotationListSortOrder.LAST_ACTIVITY);
                            mSortOrder = CollabAnnotationListSortOrder.LAST_ACTIVITY;
                            break;
                    }
            }
        }
    };

    @Override
    public void prepareOptionsMenu(Menu menu) {
        if (null == menu) {
            return;
        }

        MenuItem sortByReplyDateItem = menu.findItem(R.id.menu_annotlist_sort_by_reply_date);
        MenuItem sortByDateItem = menu.findItem(R.id.menu_annotlist_sort_by_date);
        MenuItem sortByPosItem = menu.findItem(R.id.menu_annotlist_sort_by_position);

        if (sortByReplyDateItem == null || sortByDateItem == null || sortByPosItem == null) {
            return;
        }

        if (mSortOrder instanceof CollabAnnotationListSortOrder) {
            switch (((CollabAnnotationListSortOrder) mSortOrder)) {
                case DATE_DESCENDING:
                    sortByDateItem.setChecked(true);
                    break;
                case POSITION_ASCENDING:
                    sortByPosItem.setChecked(true);
                    break;
                case LAST_ACTIVITY:
                    sortByReplyDateItem.setChecked(true);
                    break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_annotlist_sort_by_date) {
            mSorter.publishSortOrderChange(CollabAnnotationListSortOrder.DATE_DESCENDING);
        } else if (id == R.id.menu_annotlist_sort_by_position) {
            mSorter.publishSortOrderChange(CollabAnnotationListSortOrder.POSITION_ASCENDING);
        } else if (id == R.id.menu_annotlist_sort_by_reply_date) {
            mSorter.publishSortOrderChange(CollabAnnotationListSortOrder.LAST_ACTIVITY);
        } else {
            return false;
        }
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            String docId = args.getString(DOCUMENT_ID_KEY);
            if (docId == null) {
                throw new IllegalArgumentException();
            } else {
                mDocumentId = docId;
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_collab_annot_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Setup view models
        LifecycleOwner lifecycleOwner = getViewLifecycleOwner();
        FrameLayout listContainer = view.findViewById(R.id.list_container);
        FragmentActivity activity = requireActivity();
        mListViewModel = ViewModelProviders.of(activity).get(AnnotationListViewModel.class);
        // must be scoped to this fragment as we reference pdfviewCtrl. We probably want to move this to the parent fragment?
        mAnnotationViewModel = ViewModelProviders.of(this,
                new AnnotationViewModel.Factory(
                        activity.getApplication(),
                        mDocumentId
                )
        ).get(AnnotationViewModel.class);

        // Initialize components
        final AnnotationListUIComponent component =
                new AnnotationListUIComponent(listContainer,
                        lifecycleOwner,
                        mListViewModel,
                        mAnnotationViewModel,
                        mPdfViewCtrl,
                        (CollabAnnotationListSorter) mSorter,
                        mExcludedAnnotationListTypes
                );

        mCompositeDisposable.add(
                component.getObservable()
                        .subscribe(annotationListEvent -> {
                            switch (annotationListEvent.getEventType()) {
                                case ANNOTATION_ITEM_CLICKED: {
                                    AnnotationListContent annotContent = annotationListEvent.getData();
                                    if (annotContent != null) {
                                        Annot annot = ViewerUtils.getAnnotById(mPdfViewCtrl, annotContent.getId(), annotContent.getPageNum());
                                        int pageNum = annotContent.getPageNum();
                                        // Notify listeners
                                        if (mAnnotationDialogListener != null) {
                                            mAnnotationDialogListener.onAnnotationClicked(annot, pageNum);
                                        }
                                    }
                                    break;
                                }
                            }
                        }, throwable -> AnalyticsHandlerAdapter.getInstance()
                                .sendException(new RuntimeException(throwable)))
        );

        mSorter.observeSortOrderChanges(getViewLifecycleOwner(), sortOrder -> {
            mCompositeDisposable.add(
                    mAnnotationViewModel.getAnnotations()
                            .flatMap(listAnnots -> Flowable.fromIterable(listAnnots).filter(entity -> !mExcludedAnnotationListTypes.contains(entity.getType())).toList().toFlowable())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(entities -> {
                                if (entities != null && sortOrder != null && mSorter instanceof CollabAnnotationListSorter) {
                                    mListViewModel.setAnnotationList(
                                            ((CollabAnnotationListSorter) mSorter).getAnnotationList(
                                                    activity,
                                                    mPdfViewCtrl,
                                                    entities
                                            )
                                    );
                                }
                            }, throwable -> AnalyticsHandlerAdapter.getInstance().sendException(new Exception(throwable)))
            );
        });

        // Observe sort order to update menu UI
        mSorter.observeSortOrderChanges(getViewLifecycleOwner(), mSortOrderObserver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mCompositeDisposable.clear();
    }

    @Override
    protected void setupLiveUpdate() {
        // automatic
    }
}
