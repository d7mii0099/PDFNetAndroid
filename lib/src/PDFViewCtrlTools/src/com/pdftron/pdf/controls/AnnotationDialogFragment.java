//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.clans.fab.FloatingActionButton;
import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Print;
import com.pdftron.pdf.TextExtractor;
import com.pdftron.pdf.dialog.BookmarksDialogFragment;
import com.pdftron.pdf.dialog.annotlist.AnnotationFilterDialogFragment;
import com.pdftron.pdf.dialog.annotlist.AnnotationListFilterInfo;
import com.pdftron.pdf.dialog.annotlist.AnnotationListSortOrder;
import com.pdftron.pdf.dialog.annotlist.AnnotationListSorter;
import com.pdftron.pdf.dialog.annotlist.AnnotationListUtil;
import com.pdftron.pdf.dialog.annotlist.BaseAnnotationListSorter;
import com.pdftron.pdf.dialog.annotlist.BaseAnnotationSortOrder;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.EventHandler;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.pdf.viewmodel.AnnotationFilterViewModel;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;
import com.pdftron.sdf.Obj;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * The AnnotationDialogFragment shows a list of all the annotations in a
 * document being viewed by a {@link com.pdftron.pdf.PDFViewCtrl}. The list will
 * contain any comments that have been added to the annotations and clicking on
 * an annotation will show it in the PDFViewCtrl.
 */
public class AnnotationDialogFragment extends NavigationListDialogFragment implements SearchView.OnQueryTextListener, BookmarksDialogFragment.IBookmarksDialogSearchView {

    /**
     * Bundle key to specify whether the document is read only or not
     */
    public static final String BUNDLE_IS_READ_ONLY = "is_read_only";
    public static final String BUNDLE_IS_RTL = "is_right_to_left";
    public static final String BUNDLE_KEY_SORT_MODE = "sort_mode_as_int";
    public static final String BUNDLE_ENABLE_ANNOTATION_FILTER = "enable_annotation_filter";
    public static final String BUNDLE_ANNOTATION_FILTER_ICON = "annotation_filter_icon";
    public static final String BUNDLE_ANNOTATION_TYPE_EXCLUDE_LIST = "annotation_type_exclude_list";

    protected boolean mIsReadOnly;
    protected boolean mIsRtl;
    private boolean mIsLoading;
    private boolean mHasAnnotation;
    private boolean mAnnotationFilterEnabled;
    private @DrawableRes
    int mAnnotationFilterIcon = R.drawable.ic_filter;
    protected BaseAnnotationSortOrder mAnnotationListSortOrder;

    protected final ArrayList<AnnotationInfo> mAnnotationsInList = new ArrayList<>(); // annotations that are in the list
    private final ArrayList<AnnotationInfo> mAllAnnotations = new ArrayList<>(); // all annotations in the doc
    protected AnnotationsAdapter mAnnotationsAdapter;
    protected RecyclerView mRecyclerView;
    private TextView mEmptyTextView;
    private TextView mIndicatorTitleView;
    protected PDFViewCtrl mPdfViewCtrl;
    protected final ArrayList<Integer> mExcludedAnnotationListTypes = new ArrayList<>();
    private FloatingActionButton mFab;
    protected AnnotationDialogListener mAnnotationDialogListener;
    private ProgressBar mProgressBarView;
    protected BaseAnnotationListSorter mSorter;
    private AnnotationFilterViewModel mAnnotationFilterViewModel;

    private Observable<List<AnnotationInfo>> mAnnotListObservable;
    private final CompositeDisposable mDisposables = new CompositeDisposable();

    protected BaseAnnotationSortOrder mSortOrder;
    private MenuItem mSearchMenuItem;
    private boolean mIsSearchMode;
    private String mQueryText = "";

    /**
     * Callback interface to be invoked when an interaction is needed.
     */
    public interface AnnotationDialogListener {
        /**
         * Called when an annotation has been clicked.
         *
         * @param annotation The annotation
         * @param pageNum    The page number that holds the annotation
         */
        void onAnnotationClicked(Annot annotation, int pageNum);

        /**
         * Called when document annotations have been exported.
         *
         * @param outputDoc The PDFDoc containing the exported annotations
         */
        void onExportAnnotations(PDFDoc outputDoc);
    }

    /**
     * Creates a default instance of {@link AnnotationDialogFragment}.
     *
     * @return a new default instance of this class
     */
    public static AnnotationDialogFragment newInstance() {
        return new AnnotationDialogFragment();
    }

    /**
     * Creates an instance of {@link AnnotationDialogFragment}, with specified settings.
     *
     * @param isReadOnly              true if the annotation list should be read only (default false)
     * @param isRtl                   true if the the annotations are displayed right-to-left (default false)
     * @param annotationListSortOrder sorting order of the annotations
     * @return a new instance of this class with specified settings.
     */
    public static AnnotationDialogFragment newInstance(boolean isReadOnly, boolean isRtl, @NonNull AnnotationListSortOrder annotationListSortOrder) {
        Bundle args = newBundle(isReadOnly, isRtl, annotationListSortOrder);

        AnnotationDialogFragment fragment = new AnnotationDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Creates a bundle containing arguments for {@link AnnotationDialogFragment}
     *
     * @param isReadOnly              true if the annotation list should be read only (default false)
     * @param isRtl                   true if the the annotations are displayed right-to-left (default false)
     * @param annotationListSortOrder sorting order of the annotations
     * @return arguments for {@link AnnotationDialogFragment}
     */
    public static Bundle newBundle(boolean isReadOnly, boolean isRtl, @NonNull AnnotationListSortOrder annotationListSortOrder) {
        Bundle args = new Bundle();
        args.putBoolean(BUNDLE_IS_READ_ONLY, isReadOnly);
        args.putBoolean(BUNDLE_IS_RTL, isRtl);
        args.putInt(BUNDLE_KEY_SORT_MODE, annotationListSortOrder.value);
        return args;
    }

    /**
     * Sets the {@link PDFViewCtrl}
     *
     * @param pdfViewCtrl The {@link PDFViewCtrl}
     * @return This class
     */
    public AnnotationDialogFragment setPdfViewCtrl(@NonNull PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
        return this;
    }

    /**
     * Sets if the document is read only
     *
     * @param isReadOnly True if the document is read only
     * @return This class
     */
    @SuppressWarnings("unused")
    public AnnotationDialogFragment setReadOnly(boolean isReadOnly) {
        Bundle args = getArguments();
        if (args == null) {
            args = new Bundle();
        }
        args.putBoolean(BUNDLE_IS_READ_ONLY, isReadOnly);
        setArguments(args);

        return this;
    }

    /**
     * Sets if the document is right-to-left
     *
     * @param isRtl True if the document is right-to-left
     * @return This class
     */
    @SuppressWarnings("unused")
    public AnnotationDialogFragment setRtlMode(boolean isRtl) {
        Bundle args = getArguments();
        if (args == null) {
            args = new Bundle();
        }
        args.putBoolean(BUNDLE_IS_RTL, isRtl);
        setArguments(args);

        return this;
    }

    /**
     * Sets the listener to {@link AnnotationDialogListener}
     *
     * @param listener The listener
     */
    public void setAnnotationDialogListener(AnnotationDialogListener listener) {
        mAnnotationDialogListener = listener;
    }

    private Observer<BaseAnnotationSortOrder> mSortOrderObserver = new Observer<BaseAnnotationSortOrder>() {

        // Helper method to update annotation list sorting order in shared prefs
        private void updateSharedPrefs(AnnotationListSortOrder sortOrder) {
            Context context = getContext();
            if (context != null) {
                PdfViewCtrlSettingsManager.updateAnnotListSortOrder(context, sortOrder);
            }
        }

        @Override
        public void onChanged(@Nullable BaseAnnotationSortOrder sortOrder) {
            if (sortOrder instanceof AnnotationListSortOrder)
                switch (((AnnotationListSortOrder) sortOrder)) {
                    case DATE_ASCENDING:
                        updateSharedPrefs(AnnotationListSortOrder.DATE_ASCENDING);
                        mSortOrder = AnnotationListSortOrder.DATE_ASCENDING;
                        break;
                    case POSITION_ASCENDING:
                        updateSharedPrefs(AnnotationListSortOrder.POSITION_ASCENDING);
                        mSortOrder = AnnotationListSortOrder.POSITION_ASCENDING;
                        break;
                }
        }
    };

    public void prepareOptionsMenu(Menu menu) {
        if (null == menu) {
            return;
        }
        MenuItem sortByDateItem = menu.findItem(R.id.menu_annotlist_sort_by_date);
        MenuItem sortByPosItem = menu.findItem(R.id.menu_annotlist_sort_by_position);
        prepareAnnotationSearch(menu);

        if (sortByDateItem == null || sortByPosItem == null) {
            return;
        }
        if (mSortOrder instanceof AnnotationListSortOrder) {
            switch (((AnnotationListSortOrder) mSortOrder)) {
                case DATE_ASCENDING:
                    sortByDateItem.setChecked(true);
                    break;
                case POSITION_ASCENDING:
                    sortByPosItem.setChecked(true);
                    break;
            }
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (mRecyclerView != null) {
            mRecyclerView.requestFocus();
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mQueryText = newText;
        refreshAnnotationList();
        return false;
    }

    public void initOptionsMenu(Menu menu) {
        prepareAnnotationSearch(menu);
    }

    private void prepareAnnotationSearch(Menu menu) {
        mSearchMenuItem = menu.findItem(R.id.menu_annotlist_search);
        if (mSearchMenuItem != null) {
            SearchView searchView = (SearchView) mSearchMenuItem.getActionView();
            searchView.setOnQueryTextListener(this);
            searchView.setSubmitButtonEnabled(false);

            if (!Utils.isNullOrEmpty(mQueryText)) {
                mSearchMenuItem.expandActionView();
                searchView.setQuery(mQueryText, true);
                mQueryText = "";
            }

            EditText editText = searchView.findViewById(R.id.search_src_text);
            if (editText != null) {
                // Disable long-click context menu
                editText.setCustomSelectionActionModeCallback(new android.view.ActionMode.Callback() {
                    @Override
                    public boolean onCreateActionMode(android.view.ActionMode mode, Menu menu) {
                        return false;
                    }

                    @Override
                    public boolean onPrepareActionMode(android.view.ActionMode mode, Menu menu) {
                        return false;
                    }

                    @Override
                    public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {
                        return false;
                    }

                    @Override
                    public void onDestroyActionMode(android.view.ActionMode mode) {

                    }
                });
            }
            final MenuItem menuSort = menu.findItem(R.id.action_sort);
            final MenuItem menuFilter = menu.findItem(R.id.action_filter);
            // We need to override this method to get the collapse event, so we can
            // clear the filter.
            mSearchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {

                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    // Let's return true to expand the view.
                    if (menuSort != null) {
                        menuSort.setVisible(false);
                    }
                    if (menuFilter != null) {
                        menuFilter.setVisible(false);
                    }
                    mIsSearchMode = true;
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    if (menuSort != null) {
                        menuSort.setVisible(true);
                    }
                    // If Annotation Filter has been disabled we should not re-show the filter button
                    if (menuFilter != null && mAnnotationFilterEnabled) {
                        menuFilter.setVisible(true);
                    }
                    resetAnnotationListSearchFilter();
                    mIsSearchMode = false;
                    return true;
                }
            });
        }
    }

    public void resetAnnotationListSearchFilter() {
        String filterText = getQueryText();
        if (!Utils.isNullOrEmpty(filterText)) {
            if (mAnnotationsAdapter != null) {
                onQueryTextSubmit("");
            }
        }
    }

    public String getQueryText() {
        if (!Utils.isNullOrEmpty(mQueryText)) {
            return mQueryText;
        }

        String queryText = "";
        if (mSearchMenuItem != null) {
            SearchView searchView = (SearchView) mSearchMenuItem.getActionView();
            queryText = searchView.getQuery().toString();
        }
        return queryText;
    }

    public void finishSearchView() {
        if (mSearchMenuItem != null && mSearchMenuItem.isActionViewExpanded()) {
            mSearchMenuItem.collapseActionView();
        }
        resetAnnotationListSearchFilter();
        mIsSearchMode = false;
    }

    private void goToAnnotationFilterDialog() {
        if (mHasAnnotation) {
            if (!mIsLoading) {

                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_ANNOTATION_FILTER,
                        AnalyticsParam.annotationFilterParam("normal"));

                AnnotationFilterDialogFragment fragment = AnnotationFilterDialogFragment.newInstance();
                fragment.setDialogDismissListener(new CustomSizeDialogFragment.DialogDismissListener() {
                    @Override
                    public void onMenuEditorDialogDismiss() {
                        refreshAnnotationList();
                    }
                });
                fragment.setStyle(DialogFragment.STYLE_NORMAL, ((ToolManager) mPdfViewCtrl.getToolManager()).getTheme());
                fragment.show(getChildFragmentManager(), AnnotationFilterDialogFragment.TAG);
            } else {
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_ANNOTATION_FILTER,
                        AnalyticsParam.annotationFilterParam("loading"));

                Toast.makeText(getContext(), getResources().getString(R.string.annotation_filter_wait_for_loading), Toast.LENGTH_SHORT).show();
            }
        } else {
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_ANNOTATION_FILTER,
                    AnalyticsParam.annotationFilterParam("no_annotation"));

            Toast.makeText(getContext(), getResources().getString(R.string.controls_annotation_dialog_empty), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_filter) {
            boolean intercept = EventHandler.sendPreEvent(EventHandler.ANNOT_LIST_FILTER_EVENT);
            if (!intercept) {
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_ANNOTATION_FILTER,
                        AnalyticsParam.annotationFilterParam("annotation_filter_opened"));
                goToAnnotationFilterDialog();
            }
        } else if (id == R.id.menu_annotlist_sort_by_date) {
            mSorter.publishSortOrderChange(AnnotationListSortOrder.DATE_ASCENDING);
        } else if (id == R.id.menu_annotlist_sort_by_position) {
            mSorter.publishSortOrderChange(AnnotationListSortOrder.POSITION_ASCENDING);
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mIsReadOnly = args.getBoolean(BUNDLE_IS_READ_ONLY);
            mAnnotationFilterEnabled = args.getBoolean(BUNDLE_ENABLE_ANNOTATION_FILTER, true);
            mAnnotationFilterIcon = args.getInt(BUNDLE_ANNOTATION_FILTER_ICON, R.drawable.ic_filter);
            mIsRtl = args.getBoolean(BUNDLE_IS_RTL);
            int[] excludedTypes = args.getIntArray(BUNDLE_ANNOTATION_TYPE_EXCLUDE_LIST);
            if (excludedTypes != null) {
                Integer[] integerArray = ArrayUtils.toObject(excludedTypes);
                mExcludedAnnotationListTypes.clear();
                mExcludedAnnotationListTypes.addAll(Arrays.asList(integerArray));
            }
        }
        mAnnotationListSortOrder = getSortOrder(args);

        mAnnotListObservable = AnnotationListUtil.from(mPdfViewCtrl, mExcludedAnnotationListTypes);
        mSorter = getSorter();
    }

    @NonNull
    protected BaseAnnotationSortOrder getSortOrder(@Nullable Bundle args) {
        return args != null && args.containsKey(BUNDLE_KEY_SORT_MODE) ?
                AnnotationListSortOrder.fromValue(
                        args.getInt(BUNDLE_KEY_SORT_MODE, AnnotationListSortOrder.DATE_ASCENDING.value)
                ) :
                AnnotationListSortOrder.DATE_ASCENDING; // default sort by date
    }

    @NonNull
    protected BaseAnnotationListSorter getSorter() {
        return ViewModelProviders.of(this,
                        new AnnotationListSorter.Factory(mAnnotationListSortOrder))
                .get(AnnotationListSorter.class);
    }

    /**
     * @hide
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.controls_fragment_annotation_dialog, null);
        // Get reference to controls
        mRecyclerView = view.findViewById(R.id.recyclerview_control_annotation);
        mEmptyTextView = view.findViewById(R.id.control_annotation_textview_empty);
        mProgressBarView = view.findViewById(R.id.progress_bar_view);
        mFab = view.findViewById(R.id.export_annotations_button);
        if (mIsReadOnly) {
            mFab.setVisibility(View.GONE);
        }
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAnnotationDialogListener != null) {
                    mDisposables.add(prepareAnnotations()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnSubscribe(new Consumer<Disposable>() {
                                @Override
                                public void accept(Disposable disposable) throws Exception {
                                    mProgressBarView.setVisibility(View.VISIBLE);
                                }
                            })
                            .subscribe(new Consumer<PDFDoc>() {
                                           @Override
                                           public void accept(PDFDoc pdfDoc) throws Exception {
                                               mProgressBarView.setVisibility(View.GONE);
                                               if (mAnnotationDialogListener != null) {
                                                   mAnnotationDialogListener.onExportAnnotations(pdfDoc);
                                               }
                                           }
                                       },
                                    new Consumer<Throwable>() {
                                        @Override
                                        public void accept(Throwable throwable) throws Exception {
                                            mProgressBarView.setVisibility(View.GONE);
                                            AnalyticsHandlerAdapter.getInstance().sendException(new Exception(throwable));
                                        }
                                    }
                            ));
                }
                onEventAction();
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_ANNOTATIONS_LIST,
                        AnalyticsParam.annotationsListActionParam(AnalyticsHandlerAdapter.ANNOTATIONS_LIST_EXPORT));
            }
        });
        TextView editButton = view.findViewById(R.id.annotation_filter_indicator_edit_button);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToAnnotationFilterDialog();
            }
        });
        // Add click listener to the list
        ItemClickHelper itemClickHelper = new ItemClickHelper();
        itemClickHelper.attachToRecyclerView(mRecyclerView);
        itemClickHelper.setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView recyclerView, View view, int position, long id) {
                onEventAction();
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_NAVIGATE_BY,
                        AnalyticsParam.viewerNavigateByParam(AnalyticsHandlerAdapter.VIEWER_NAVIGATE_BY_ANNOTATIONS_LIST));

                AnnotationInfo annotInfo = mAnnotationsInList.get(position);
                if (mPdfViewCtrl != null) {
                    ViewerUtils.jumpToAnnotation(mPdfViewCtrl, annotInfo.getAnnotation(), annotInfo.getPageNum());
                }

                // Notify listeners
                if (mAnnotationDialogListener != null) {
                    mAnnotationDialogListener.onAnnotationClicked(annotInfo.getAnnotation(), annotInfo.getPageNum());
                }
            }
        });
        return view;
    }

    /**
     * @hide
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAnnotationsAdapter = new AnnotationsAdapter(mAnnotationsInList, mIsReadOnly, mRecyclerView, mPdfViewCtrl, mAnalyticsEventListener);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        mRecyclerView.setAdapter(mAnnotationsAdapter);
        mEmptyTextView.setText(R.string.controls_annotation_dialog_loading);
        mAnnotationFilterViewModel = ViewModelProviders.of(this,
                new AnnotationFilterViewModel.Factory(
                        getActivity().getApplication(),
                        new AnnotationListFilterInfo(AnnotationListFilterInfo.FilterState.OFF)
                )

        ).get(AnnotationFilterViewModel.class);
        if (!mAnnotationFilterEnabled || mAnnotationFilterViewModel.getAnnotationFilterLiveData().getValue() == null) {
            //failed to load ViewModel, hide AnnotationFilter icon
            Toolbar toolBar = getToolbar();
            if (toolBar != null) {
                MenuItem item = toolBar.getMenu().findItem(R.id.action_filter);
                if (item != null) {
                    item.setVisible(false);
                }
            }
        }
        mSorter.observeSortOrderChanges(getViewLifecycleOwner(), new Observer<BaseAnnotationSortOrder>() {
            @Override
            public void onChanged(@Nullable BaseAnnotationSortOrder annotationListSortOrder) {
                if (annotationListSortOrder != null) {
                    refreshAnnotationList();
                }
            }
        });

        // Observe sort order to update menu UI
        mSorter.observeSortOrderChanges(getViewLifecycleOwner(), mSortOrderObserver);
        setupLiveUpdate();
    }

    private ToolManager.AnnotationModificationListener mModificationListener = new ToolManager.AnnotationModificationListener() {
        @Override
        public void onAnnotationsAdded(Map<Annot, Integer> annots) {
            if (mAnnotationsAdapter != null) {
                mAnnotationsAdapter.addAll(getAnnotationInfoForChangeSet(annots));
            }
        }

        @Override
        public void onAnnotationsPreModify(Map<Annot, Integer> annots) {

        }

        @Override
        public void onAnnotationsModified(Map<Annot, Integer> annots, Bundle extra) {
            if (mAnnotationsAdapter != null) {
                mAnnotationsAdapter.replaceAll(getAnnotationInfoForChangeSet(annots));
            }
        }

        @Override
        public void onAnnotationsPreRemove(Map<Annot, Integer> annots) {
            if (mAnnotationsAdapter != null) {
                mAnnotationsAdapter.removeAll(getAnnotationInfoForChangeSet(annots));
            }
        }

        @Override
        public void onAnnotationsRemoved(Map<Annot, Integer> annots) {
            // we could loop through and only deselected selected annotation
            // however we support delete all annotations in the whole document
            // which looping again might be too heavy for this purpose
            if (mPdfViewCtrl != null && mPdfViewCtrl.getToolManager() instanceof ToolManager) {
                ((ToolManager) mPdfViewCtrl.getToolManager()).deselectAll();
            }
        }

        @Override
        public void onAnnotationsRemovedOnPage(int pageNum) {

        }

        @Override
        public void annotationsCouldNotBeAdded(String errorMessage) {

        }
    };

    protected void setupLiveUpdate() {
        if (mPdfViewCtrl != null) {
            final ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            toolManager.addAnnotationModificationListener(mModificationListener);
        }
    }

    private ArrayList<AnnotationInfo> getAnnotationInfoForChangeSet(Map<Annot, Integer> annots) {
        ArrayList<AnnotationInfo> annotList = new ArrayList<>();
        TextExtractor textExtractor = new TextExtractor();
        for (Map.Entry<Annot, Integer> entry : annots.entrySet()) {
            Annot key = entry.getKey();
            Integer value = entry.getValue();
            if (key != null && value != null) {
                try {
                    AnnotationInfo info = AnnotationListUtil.toAnnotationInfo(key, mPdfViewCtrl.getDoc().getPage(value), textExtractor, mExcludedAnnotationListTypes);
                    if (info != null) {
                        annotList.add(info);
                    }
                } catch (PDFNetException e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
            }
        }
        return annotList;
    }

    private Single<PDFDoc> prepareAnnotations() {
        return Single.fromCallable(new Callable<PDFDoc>() {
            @Override
            public PDFDoc call() throws Exception {
                return Print.exportAnnotations(mPdfViewCtrl.getDoc(), mIsRtl);
            }
        });
    }

    /**
     * Updates visible of annotations in PDFViewCtrl based on annotation list filter state
     */
    public void updateAnnotationVisibilityInfo(List<AnnotationInfo> annotationInfos) {
        AnnotationListFilterInfo filterInfo = mAnnotationFilterViewModel.getAnnotationFilterLiveData().getValue();
        try {
            if (filterInfo != null) {
                AnnotationListFilterInfo.FilterState filterState = filterInfo.getFilterState();
                if (filterState == AnnotationListFilterInfo.FilterState.ON ||
                        filterState == AnnotationListFilterInfo.FilterState.ON_LIST_ONLY) {
                    for (AnnotationInfo info : annotationInfos) {
                        String color = Utils.getColorHexString(AnnotUtils.getAnnotColor(info.getAnnotation())).toLowerCase();
                        if (info.getAnnotation() != null) {
                            Obj stateObj = info.getAnnotation().getSDFObj().findObj(AnnotUtils.Key_State);
                            String author = info.getAuthor();
                            int type = info.getType();
                            boolean statusToShow = false;
                            boolean authorToShow = false;
                            boolean typeToShow = false;
                            boolean colorToShow = false;
                            if (!filterInfo.isAnyStatusSelected() || (stateObj != null && filterInfo.isStatusSelected(stateObj.getAsPDFText()))) {
                                statusToShow = true;
                            }
                            if (!filterInfo.isAnyAuthorSelected() || filterInfo.isAuthorSelected(author)) {
                                authorToShow = true;
                            }
                            if (!filterInfo.isAnyTypeSelected() || filterInfo.isTypeSelected(type)) {
                                typeToShow = true;
                            }
                            if (!filterInfo.isAnyColorSelected() || filterInfo.isColorSelected(color)) {
                                colorToShow = true;
                            }
                            if (statusToShow && authorToShow && typeToShow && colorToShow) {
                                mAnnotationFilterViewModel.removeAnnotToHide(info.getAnnotation());
                            } else {
                                mAnnotationFilterViewModel.addAnnotToHide(info.getAnnotation());
                            }
                        }
                    }
                } else { // otherwise hide all annots, or show all annots
                    for (AnnotationInfo info : annotationInfos) {
                        if (info.getAnnotation() != null) {
                            if (filterState == AnnotationListFilterInfo.FilterState.HIDE_ALL) {
                                mAnnotationFilterViewModel.addAnnotToHide(info.getAnnotation());
                            } else if (filterState == AnnotationListFilterInfo.FilterState.OFF) {
                                mAnnotationFilterViewModel.removeAnnotToHide(info.getAnnotation());
                            }
                        }
                    }
                }
            }
        } catch (PDFNetException e) {
            e.printStackTrace();
        }
    }

    public void updateAnnotationVisibility() {
        if (mPdfViewCtrl == null) {
            return;
        }
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            AnnotationListFilterInfo filterInfo = mAnnotationFilterViewModel.getAnnotationFilterLiveData().getValue();
            for (AnnotationInfo info : mAllAnnotations) {
                Annot annot = info.getAnnotation();
                if (mAnnotationFilterViewModel.shouldHideAnnot(annot) &&
                        (filterInfo != null && !filterInfo.getFilterState().equals(AnnotationListFilterInfo.FilterState.ON_LIST_ONLY))) {
                    mPdfViewCtrl.hideAnnotation(info.getAnnotation());
                } else {
                    mPdfViewCtrl.showAnnotation(info.getAnnotation());
                }
            }
            mPdfViewCtrl.update(true);
        } catch (PDFNetException e) {
            e.printStackTrace();
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
    }

    /**
     * Helper method to populate and re-populate the annotation list with a specified sort order.
     * Will clear the list before populating.
     */
    @SuppressWarnings("RedundantThrows")
    private void populateAnnotationList() {
        mHasAnnotation = false;
        mIsLoading = true;
        mProgressBarView.setVisibility(View.VISIBLE);
        final HashSet<Integer> typeSet = new HashSet<>();
        final HashSet<String> authorSet = new HashSet<>();
        final HashSet<String> statusSet = new HashSet<>();
        final HashSet<String> colorSet = new HashSet<>();
        // This will populate mAnnotation
        mDisposables.add(
                mAnnotListObservable
                        .map(new Function<List<AnnotationInfo>, List<AnnotationInfo>>() {
                            @Override
                            public List<AnnotationInfo> apply(List<AnnotationInfo> annotationInfos) throws Exception {
                                if (mSorter instanceof AnnotationListSorter) {
                                    //noinspection RedundantCast
                                    ((AnnotationListSorter) mSorter).sort(annotationInfos);
                                    return annotationInfos;
                                } else {
                                    return annotationInfos;
                                }
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .map(new Function<List<AnnotationInfo>, List<AnnotationInfo>>() {
                            @Override
                            public List<AnnotationInfo> apply(List<AnnotationInfo> annotationInfos) throws Exception {
                                if (!annotationInfos.isEmpty()) {
                                    mHasAnnotation = true;
                                }
                                // If annotation filter is enabled, then process filter information
                                if (mAnnotationFilterEnabled) {
                                    for (AnnotationInfo info : annotationInfos) {
                                        int type = info.getType();
                                        String author = info.getAuthor();
                                        String color = Utils.getColorHexString(AnnotUtils.getAnnotColor(info.getAnnotation())).toLowerCase();

                                        typeSet.add(type);
                                        mAnnotationFilterViewModel.addType(type);
                                        authorSet.add(author);
                                        mAnnotationFilterViewModel.addAuthor(author);
                                        colorSet.add(color);
                                        mAnnotationFilterViewModel.addColor(color);
                                        Obj stateObj = info.getAnnotation().getSDFObj().findObj(AnnotUtils.Key_State);
                                        if (stateObj != null) {
                                            statusSet.add(stateObj.getAsPDFText());
                                            mAnnotationFilterViewModel.addStatus(stateObj.getAsPDFText());
                                        }
                                    }
                                }
                                updateAnnotationVisibilityInfo(annotationInfos);
                                return annotationInfos;
                            }
                        })
                        .doOnComplete(new Action() {
                            @Override
                            public void run() throws Exception {
                                mAnnotationFilterViewModel.updateFilterOptions(
                                        typeSet,
                                        authorSet,
                                        statusSet,
                                        colorSet
                                );
                            }
                        })
                        .doOnSubscribe(new Consumer<Disposable>() {
                            @Override
                            public void accept(Disposable disposable) throws Exception {
                                mAllAnnotations.clear();
                            }
                        })
                        .subscribe(new Consumer<List<AnnotationInfo>>() {
                            @Override
                            public void accept(List<AnnotationInfo> annotationInfos) throws Exception {
                                mAllAnnotations.addAll(annotationInfos);
                                // Only show annotations that are visible
                                String queryText = getQueryText();
                                List<AnnotationInfo> annotationInfosToDisplay = new ArrayList<>(annotationInfos);
                                Iterator<AnnotationInfo> iterator = annotationInfosToDisplay.iterator();
                                while (iterator.hasNext()) {
                                    AnnotationInfo info = iterator.next();
                                    if (mAnnotationFilterViewModel.shouldHideAnnot(info.mAnnotation)) {
                                        iterator.remove();
                                    } else if (!queryText.isEmpty() && !info.mContent.toLowerCase().contains(queryText.toLowerCase())) {
                                        iterator.remove();
                                    }
                                }
                                mAnnotationsAdapter.addAll(annotationInfosToDisplay);
                                // hide loading text when anything is added to the list
                                if (mAnnotationsAdapter.getItemCount() > 0) {
                                    mEmptyTextView.setVisibility(View.GONE);
                                }
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                // show error toast
                                mProgressBarView.setVisibility(View.GONE);
                                mFab.setVisibility(View.GONE);
                                mEmptyTextView.setText(R.string.controls_annotation_dialog_empty);
                                CommonToast.showText(getActivity(), R.string.error_generic_message, Toast.LENGTH_SHORT);

                                AnalyticsHandlerAdapter.getInstance().sendException(new RuntimeException(throwable));
                            }
                        }, new Action() {
                            @Override
                            public void run() throws Exception {

                                if (mIndicatorTitleView != null) {
                                    if (mHasAnnotation && mAnnotationsAdapter.getItemCount() == 0) {
                                        mIndicatorTitleView.setText(getResources().getText(R.string.annotation_filter_hidden));
                                    } else {
                                        mIndicatorTitleView.setText(getResources().getText(R.string.annotation_filter_indicator));
                                    }
                                }

                                mIsLoading = false;
                                mProgressBarView.setVisibility(View.GONE);
                                updateAnnotationVisibility();

                                if (mFab != null) {
                                    mFab.setVisibility(mAnnotationsAdapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
                                    if (mIsReadOnly) {
                                        mFab.setVisibility(View.GONE);
                                    }
                                    mEmptyTextView.setText(R.string.controls_annotation_dialog_empty);
                                    if (mAnnotationsAdapter.getItemCount() == 0) {
                                        mRecyclerView.setVisibility(View.GONE);
                                    } else {
                                        mRecyclerView.setVisibility(View.VISIBLE);
                                    }
                                    mEmptyTextView.setVisibility(mHasAnnotation ? View.GONE : View.VISIBLE);
                                    mProgressBarView.setVisibility(View.GONE);
                                }
                            }
                        })
        );
    }

    /**
     * @hide
     */
    @Override
    public void onStop() {
        super.onStop();
    }

    public boolean isFilterOn() {
        AnnotationListFilterInfo mFilterInfo = mAnnotationFilterViewModel.getAnnotationFilterLiveData().getValue();
        return mFilterInfo != null &&
                (mFilterInfo.getFilterState() == AnnotationListFilterInfo.FilterState.HIDE_ALL ||
                        (mFilterInfo.getFilterState() == AnnotationListFilterInfo.FilterState.ON &&
                                !(mFilterInfo.isAnyStatusSelected() && mFilterInfo.isAnyAuthorSelected()
                                        && mFilterInfo.isAnyTypeSelected() && mFilterInfo.isAnyColorSelected())) ||
                        (mFilterInfo.getFilterState() == AnnotationListFilterInfo.FilterState.ON_LIST_ONLY &&
                                !(mFilterInfo.isAnyStatusSelected() && mFilterInfo.isAnyAuthorSelected()
                                        && mFilterInfo.isAnyTypeSelected() && mFilterInfo.isAnyColorSelected())));
    }

    /**
     * Gets the icon for annotation filter
     */
    @DrawableRes
    public int getAnnotationFilterIcon() {
        return mAnnotationFilterIcon;
    }

    /**
     * Sets the icon for annotation filter
     */
    public void setAnnotationFilterIcon(@DrawableRes int iconRes) {
        mAnnotationFilterIcon = iconRes;
        prepareAnnotationListUI();
    }

    private void prepareAnnotationListUI() {
        if (getParentFragment() != null) {
            Toolbar toolBar = getToolbar();
            if (toolBar != null && getView() != null) {
                View indicator = getView().findViewById(R.id.annotation_filter_indicator_container);
                mIndicatorTitleView = indicator.findViewById(R.id.annotation_filter_indicator_title);
                MenuItem item = toolBar.getMenu().findItem(R.id.action_filter);
                if (item != null) {
                    if (isFilterOn()) {
                        AnnotationListFilterInfo mFilterInfo = mAnnotationFilterViewModel.getAnnotationFilterLiveData().getValue();
                        item.setIcon(getResources().getDrawable(R.drawable.ic_filter_with_indicator));
                        indicator.setVisibility(View.VISIBLE);
                        if (mFilterInfo != null && mFilterInfo.getFilterState() == AnnotationListFilterInfo.FilterState.HIDE_ALL) {
                            mIndicatorTitleView.setText(getResources().getText(R.string.annotation_filter_hidden));
                        } else {
                            mIndicatorTitleView.setText(getResources().getText(R.string.annotation_filter_indicator));
                        }
                    } else {
                        item.setIcon(getResources().getDrawable(mAnnotationFilterIcon));
                        indicator.setVisibility(View.GONE);
                    }
                }
            }
        }
    }

    public void refreshAnnotationList() {
        prepareAnnotationListUI();
        mAnnotationsAdapter.clear();
        mAnnotationsAdapter.notifyDataSetChanged();
        populateAnnotationList();
    }

    public boolean isAnnotationFilterEnabled() {
        return mAnnotationFilterEnabled;
    }

    @Override
    public boolean handleBackPress() {
        if (mIsSearchMode) {
            finishSearchView();
            return true;
        }
        return super.handleBackPress();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mIsSearchMode) {
            finishSearchView();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mPdfViewCtrl != null) {
            final ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            toolManager.removeAnnotationModificationListener(mModificationListener);
        }
        mDisposables.clear();
    }

    private Toolbar getToolbar() {
        if (getParentFragment() != null && getParentFragment().getView() != null) {
            return getParentFragment().getView().findViewById(R.id.toolbar);
        }
        return null;
    }

    /**
     * Annotation Info class. Internal use in {@link AnnotationListUtil}
     */
    public static class AnnotationInfo {
        /**
         * The annotation type is one of the types found in com.pdftron.pdf.Annot.
         */
        private int mType;

        /**
         * Holds the page where this annotation is found.
         */
        private int mPageNum;

        /**
         * The contents of the annotation are used in the list view of the
         * BookmarkDialogFragment.
         */
        private String mContent;

        /**
         * The author for this annotation.
         */
        private String mAuthor;

        private Annot mAnnotation;

        /**
         * This date and time info for this annotation
         */
        private String mDate;

        /**
         * Y-position of of the top edge of the <code>Rect</code> containing this annotation.
         * Obtained from normalized <code>Rect</code>'s Y2 field.
         */
        private final double mY2;

        /**
         * Default constructor. Creates an empty annotation entry.
         */
        @SuppressWarnings("unused")
        AnnotationInfo() {
            this(0, 0, "", "", "", null, 0);
        }

        /**
         * Class constructor specifying the type, page and content of the
         * annotation.
         *
         * @param type       the type of the annotation
         * @param pageNum    the page where this annotation lies in
         * @param content    the content of the annotation
         * @param author     the author of this annotation
         * @param date       the date
         * @param annotation the annotation
         * @param yPos       y-position of of the top edge of the <code>Rect</code> containing this annotation.
         * @see {@link Annot}
         */
        public AnnotationInfo(int type,
                int pageNum,
                String content,
                String author,
                String date,
                @Nullable Annot annotation,
                double yPos) {
            this.mType = type;
            this.mPageNum = pageNum;
            this.mContent = content;
            this.mAuthor = author;
            this.mDate = date;
            this.mAnnotation = annotation;
            this.mY2 = yPos;
        }

        /**
         * Returns the type of this annotation.
         *
         * @return The type of the annotation
         * @see com.pdftron.pdf.Annot
         */
        public int getType() {
            return mType;
        }

        /**
         * Sets the type of the annotation.
         *
         * @param mType The type of the annotation
         * @see com.pdftron.pdf.Annot
         */
        public void setType(int mType) {
            this.mType = mType;
        }

        /**
         * @return The page number where the annotation is on
         */
        public int getPageNum() {
            return mPageNum;
        }

        /**
         * Sets he page number where the annotation is on.
         *
         * @param mPageNum The page number
         */
        public void setPageNum(int mPageNum) {
            this.mPageNum = mPageNum;
        }

        /**
         * @return The content
         */
        public String getContent() {
            return mContent;
        }

        /**
         * Sets the content.
         *
         * @param mContent The content
         */
        public void setContent(String mContent) {
            this.mContent = mContent;
        }

        /**
         * @return The author
         */
        public String getAuthor() {
            return mAuthor;
        }

        /**
         * Sets the author.
         *
         * @param author The author
         */
        public void setAuthor(String author) {
            this.mAuthor = author;
        }

        /**
         * @return The annotation
         */
        @Nullable
        public Annot getAnnotation() {
            return mAnnotation;
        }

        /**
         * Get date in string format
         *
         * @return Date of the annotation
         */
        public String getDate() {
            return mDate;
        }

        /**
         * Get the Y-position of of the top edge of the <code>Rect</code> containing this annotation.
         * Obtained from normalized <code>Rect</code>'s Y2 field.
         *
         * @return y-position of the top edge of the Rect containing this annotation.
         */
        public double getY2() {
            return mY2;
        }

        @SuppressWarnings("EqualsReplaceableByObjectsCall")
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AnnotationInfo info = (AnnotationInfo) o;

            return mAnnotation != null ? mAnnotation.equals(info.mAnnotation) : info.mAnnotation == null;
        }

        @Override
        public int hashCode() {
            return mAnnotation != null ? mAnnotation.hashCode() : 0;
        }
    }
}