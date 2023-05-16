package com.pdftron.pdf.controls;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.dialog.pagelabel.PageLabelDialog;
import com.pdftron.pdf.dialog.pagelabel.PageLabelSetting;
import com.pdftron.pdf.dialog.pagelabel.PageLabelSettingViewModel;
import com.pdftron.pdf.dialog.pagelabel.PageLabelUtils;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.tools.UndoRedoManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.BookmarkManager;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.DialogGoToPage;
import com.pdftron.pdf.utils.Event;
import com.pdftron.pdf.utils.ExceptionHandlerCallback;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.RequestCode;
import com.pdftron.pdf.utils.ToolbarActionMode;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;
import com.pdftron.pdf.widget.recyclerview.ItemSelectionHelper;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerView;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import co.paulburke.android.itemtouchhelperdemo.helper.SimpleItemTouchHelperCallback;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * The ThumbnailsViewFragment uses the {@link com.pdftron.pdf.PDFViewCtrl#getThumbAsync(int)}
 * to show thumbnails of the documents as a grid view. It supports add/remove/re-arrange pages,
 * as well as rotate/duplicate and export pages. Undo/Redo is also supported.
 */
public class ThumbnailsViewFragment extends DialogFragment implements ThumbnailsViewAdapter.EditPagesListener {

    public enum ThumbnailsViewEditOptions {
        OPTION_INSERT_PAGES,
        OPTION_INSERT_FROM_IMAGE,
        OPTION_INSERT_FROM_DOCUMENT,
        OPTION_EXPORT_PAGES,
        OPTION_DUPLICATE_PAGES,
        OPTION_ROTATE_PAGES,
        OPTION_DELETE_PAGES,
        OPTION_PAGE_LABEL
    }

    public static boolean HANDLE_INTENT_IN_ACTIVITY = false; // whether the intent from onActivityResult should be handled in the activity
    public static final String TAG = "thumbnails_fragment";

    protected static final String BUNDLE_READ_ONLY_DOC = "read_only_doc";
    protected static final String BUNDLE_EDIT_MODE = "edit_mode";
    protected static final String BUNDLE_OUTPUT_FILE_URI = "output_file_uri";
    protected static final String BUNDLE_HIDE_FILTER_MODES = "hide_filter_modes";
    protected static final String BUNDLE_HIDE_EDIT_OPTIONS = "hide_edit_options";

    public static final int FILTER_MODE_NORMAL = 0;
    public static final int FILTER_MODE_ANNOTATED = 1;
    public static final int FILTER_MODE_BOOKMARKED = 2;

    public enum FilterModes {
        ANNOTATED(FILTER_MODE_ANNOTATED),
        BOOKMARKED(FILTER_MODE_BOOKMARKED);

        final int mode;

        FilterModes(int mode) {
            this.mode = mode;
        }

        public int getValue() {
            return this.mode;
        }
    }

    private Theme mTheme;

    protected FloatingActionMenu mFabMenu;
    private Uri mOutputFileUri;
    protected boolean mIsReadOnly;
    protected boolean mIsReadOnlySave;

    private Integer mInitSelectedItem;

    protected PDFViewCtrl mPdfViewCtrl;

    protected Toolbar mToolbar;
    protected Toolbar mCabToolbar;
    protected SimpleRecyclerView mRecyclerView;
    protected ThumbnailsViewAdapter mAdapter;
    private ProgressBar mProgressBarView;

    protected ItemSelectionHelper mItemSelectionHelper;
    protected ItemClickHelper mItemClickHelper;
    protected ItemTouchHelper mItemTouchHelper;
    protected ToolbarActionMode mActionMode;

    protected MenuItem mMenuItemUndo;
    protected MenuItem mMenuItemRedo;
    protected MenuItem mMenuItemRotate;
    protected MenuItem mMenuItemDelete;
    protected MenuItem mMenuItemDuplicate;
    protected MenuItem mMenuItemExport;
    protected MenuItem mMenuItemPageLabel;
    protected MenuItem mMenuItemEdit;
    protected MenuItem mMenuItemAddBookmark;
    protected MenuItem mMenuItemRemoveBookmark;

    protected MenuItem mMenuItemFilter;
    protected MenuItem mMenuItemFilterAll;
    protected MenuItem mMenuItemFilterAnnotated;
    protected MenuItem mMenuItemFilterBookmarked;

    private int mSpanCount;
    private String mTitle = "";
    protected boolean mHasEventAction;

    private boolean mAddDocPagesDelay;
    private int mPositionDelay;
    private ThumbnailsViewAdapter.DocumentFormat mDocumentFormatDelay;
    private Object mDataDelay;

    protected OnThumbnailsViewDialogDismissListener mOnThumbnailsViewDialogDismissListener;
    protected OnThumbnailsEditAttemptWhileReadOnlyListener mOnThumbnailsEditAttemptWhileReadOnlyListener;
    protected OnExportThumbnailsListener mOnExportThumbnailsListener;

    private final CompositeDisposable mDisposables = new CompositeDisposable();
    protected ThumbnailsViewFilterMode mFilterMode;

    private boolean mStartInEdit;

    @Nullable
    private ArrayList<Integer> mHideFilterModes;
    @NonNull
    protected final ArrayList<ThumbnailsViewEditOptions> mHideEditOptions = new ArrayList<>();

    /**
     * Returns a new instance of the class
     */
    public static ThumbnailsViewFragment newInstance() {
        return newInstance(false);
    }

    /**
     * Returns a new instance of the class
     */
    public static ThumbnailsViewFragment newInstance(boolean readOnly) {
        return newInstance(readOnly, false);
    }

    /**
     * Returns a new instance of the class
     */
    public static ThumbnailsViewFragment newInstance(boolean readOnly, boolean editMode) {
        return newInstance(readOnly, editMode, null);
    }

    /**
     * Returns a new instance of the class
     */
    public static ThumbnailsViewFragment newInstance(boolean readOnly, boolean editMode, @Nullable int[] hideFilterModes) {
        return newInstance(readOnly, editMode, hideFilterModes, null);
    }

    /**
     * Returns a new instance of the class
     */
    public static ThumbnailsViewFragment newInstance(boolean readOnly, boolean editMode,
            @Nullable int[] hideFilterModes, @Nullable String[] hideEditOptions) {
        ThumbnailsViewFragment fragment = new ThumbnailsViewFragment();
        Bundle args = createThumbnailViewFragmentBundle(readOnly, editMode, hideFilterModes, hideEditOptions);
        fragment.setArguments(args);
        return fragment;
    }

    public static Bundle createThumbnailViewFragmentBundle(boolean readOnly, boolean editMode,
            @Nullable int[] hideFilterModes, @Nullable String[] hideEditOptions) {
        Bundle args = new Bundle();
        args.putBoolean(BUNDLE_READ_ONLY_DOC, readOnly);
        args.putBoolean(BUNDLE_EDIT_MODE, editMode);
        if (hideFilterModes != null) {
            args.putIntArray(BUNDLE_HIDE_FILTER_MODES, hideFilterModes);
        }
        if (hideEditOptions != null) {
            args.putStringArray(BUNDLE_HIDE_EDIT_OPTIONS, hideEditOptions);
        }
        return args;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getContext();
        if (context == null) {
            return;
        }

        mTheme = ThumbnailsViewFragment.Theme.fromContext(context);

        if (savedInstanceState != null) {
            mOutputFileUri = savedInstanceState.getParcelable(BUNDLE_OUTPUT_FILE_URI);
        }

        int defaultFilterMode = PdfViewCtrlSettingsManager.getThumbListFilterMode(context, FILTER_MODE_NORMAL);

        if (getArguments() != null) {
            if (getArguments().getBoolean(BUNDLE_EDIT_MODE, false)) {
                mStartInEdit = true;
                // in edit mode, we will always open in all page mode
                defaultFilterMode = FILTER_MODE_NORMAL;
            }
            if (getArguments().getIntArray(BUNDLE_HIDE_FILTER_MODES) != null) {
                int[] hideFilterModesInt = getArguments().getIntArray(BUNDLE_HIDE_FILTER_MODES);
                mHideFilterModes = new ArrayList<>(hideFilterModesInt.length);
                for (int mode : hideFilterModesInt) {
                    mHideFilterModes.add(mode);
                }
            }
            if (getArguments().getStringArray(BUNDLE_HIDE_EDIT_OPTIONS) != null) {
                String[] hideEditOptions = getArguments().getStringArray(BUNDLE_HIDE_EDIT_OPTIONS);
                for (String option : hideEditOptions) {
                    mHideEditOptions.add(ThumbnailsViewEditOptions.valueOf(option));
                }
            }
        }
        mFilterMode = ViewModelProviders.of(this,
                new ThumbnailsViewFilterMode.Factory(defaultFilterMode)).get(ThumbnailsViewFilterMode.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.controls_fragment_thumbnails_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (mPdfViewCtrl == null) {
            return;
        }

        if (Utils.isNullOrEmpty(mTitle)) {
            mTitle = getString(R.string.controls_thumbnails_view_description);
        }

        int viewWidth = getDisplayWidth();
        int thumbSize = getResources().getDimensionPixelSize(R.dimen.controls_thumbnails_view_image_width);
        int thumbSpacing = getResources().getDimensionPixelSize(R.dimen.controls_thumbnails_view_grid_spacing);
        // Calculate number of columns
        mSpanCount = (int) Math.floor(viewWidth / (double) (thumbSize + thumbSpacing));

        mToolbar = view.findViewById(R.id.controls_thumbnails_view_toolbar);

        mCabToolbar = view.findViewById(R.id.controls_thumbnails_view_cab);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!onBackPressed()) {
                    dismiss();
                }
            }
        });

        if (getArguments() != null) {
            Bundle args = getArguments();
            mIsReadOnly = args.getBoolean(BUNDLE_READ_ONLY_DOC, false);
            mIsReadOnlySave = mIsReadOnly;
        }

        mToolbar.inflateMenu(R.menu.controls_fragment_thumbnail_browser_toolbar);
        mMenuItemEdit = mToolbar.getMenu().findItem(R.id.controls_action_edit);
        if (mMenuItemEdit != null) {
            mMenuItemEdit.setVisible(!mIsReadOnly);
        }
        mMenuItemAddBookmark = mToolbar.getMenu().findItem(R.id.controls_thumbnails_view_action_add_bookmark);
        if (mMenuItemAddBookmark != null) {
            mMenuItemAddBookmark.setVisible(!mIsReadOnly);
        }
        mMenuItemFilter = mToolbar.getMenu().findItem(R.id.action_filter);
        mMenuItemFilterAll = mToolbar.getMenu().findItem(R.id.menu_filter_all);
        mMenuItemFilterAnnotated = mToolbar.getMenu().findItem(R.id.menu_filter_annotated);
        mMenuItemFilterBookmarked = mToolbar.getMenu().findItem(R.id.menu_filter_bookmarked);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.controls_action_edit) {
                    // Start edit-mode
                    startActionMode();
                    return true;
                } else if (item.getItemId() == R.id.action_filter) {
                    if (mMenuItemFilterAll != null &&
                            mMenuItemFilterAnnotated != null &&
                            mMenuItemFilterBookmarked != null) {
                        Integer mode = mFilterMode.getFilterMode();
                        if (mode != null) {
                            switch (mode) {
                                case FILTER_MODE_NORMAL:
                                    mMenuItemFilterAll.setChecked(true);
                                    break;
                                case FILTER_MODE_ANNOTATED:
                                    mMenuItemFilterAnnotated.setChecked(true);
                                    break;
                                case FILTER_MODE_BOOKMARKED:
                                    mMenuItemFilterBookmarked.setChecked(true);
                                    break;
                            }
                        }
                    }
                } else if (item.getItemId() == R.id.menu_filter_all) {
                    mFilterMode.publishFilterTypeChange(FILTER_MODE_NORMAL);
                    return true;
                } else if (item.getItemId() == R.id.menu_filter_annotated) {
                    mFilterMode.publishFilterTypeChange(FILTER_MODE_ANNOTATED);
                    return true;
                } else if (item.getItemId() == R.id.menu_filter_bookmarked) {
                    mFilterMode.publishFilterTypeChange(FILTER_MODE_BOOKMARKED);
                    return true;
                } else if (item.getItemId() == R.id.controls_thumbnails_view_action_add_bookmark) {
                    Context context = getContext();
                    if (context != null) {
                        DialogGoToPage dlgGotoPage = new DialogGoToPage(context, mPdfViewCtrl, new DialogGoToPage.DialogGoToPageListener() {
                            @Override
                            public void onPageSet(int pageNum) {
                                ViewerUtils.addPageToBookmark(mPdfViewCtrl.getContext(), mIsReadOnlySave, mPdfViewCtrl, pageNum);
                                if (isBookmarkFilterMode()) {
                                    populateThumbList(FILTER_MODE_BOOKMARKED);
                                }
                            }
                        });
                        dlgGotoPage.show(R.string.action_add_bookmark, R.string.add, String.valueOf(mPdfViewCtrl.getCurrentPage()));
                    }
                    return true;
                }
                return false;
            }
        });

        mToolbar.setTitle(mTitle);

        mProgressBarView = view.findViewById(R.id.progress_bar_view);
        mProgressBarView.setVisibility(View.GONE);

        mRecyclerView = view.findViewById(R.id.controls_thumbnails_view_recycler_view);
        mRecyclerView.initView(mSpanCount, getResources().getDimensionPixelSize(R.dimen.controls_thumbnails_view_grid_spacing));
        mRecyclerView.setItemViewCacheSize(mSpanCount * 2);

        try {
            mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if (mRecyclerView == null) {
                                return;
                            }
                            try {
                                mRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            } catch (Exception ignored) {
                            }
                            if (mAdapter == null) {
                                return;
                            }
                            mAdapter.updateMainViewWidth(getMainViewWidth());
                            updateSpanCount(mSpanCount);
                        }
                    });
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        mItemClickHelper = new ItemClickHelper();
        mItemClickHelper.attachToRecyclerView(mRecyclerView);

        mItemSelectionHelper = new ItemSelectionHelper();
        mItemSelectionHelper.attachToRecyclerView(mRecyclerView);
        mItemSelectionHelper.setChoiceMode(ItemSelectionHelper.CHOICE_MODE_MULTIPLE);

        mAdapter = new ThumbnailsViewAdapter(getActivity(), this, getFragmentManager(), mPdfViewCtrl, null, mSpanCount,
                mItemSelectionHelper, mTheme);
        mAdapter.registerAdapterDataObserver(mItemSelectionHelper.getDataObserver());
        mAdapter.updateMainViewWidth(getMainViewWidth());
        mRecyclerView.setAdapter(mAdapter);

        // filter
        mFilterMode.observeFilterTypeChanges(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer mode) {
                if (mode != null) {
                    populateThumbList(mode);
                    updateSharedPrefs(mode);
                    switch (mode) {
                        case FILTER_MODE_NORMAL:
                            mToolbar.setTitle(mTitle);
                            mIsReadOnly = mIsReadOnlySave;
                            break;
                        case FILTER_MODE_ANNOTATED:
                            mToolbar.setTitle(String.format("%s (%s)",
                                    mTitle,
                                    getResources().getString(R.string.action_filter_thumbnails_annotated)));
                            mIsReadOnly = true;
                            break;
                        case FILTER_MODE_BOOKMARKED:
                            mToolbar.setTitle(String.format("%s (%s)",
                                    mTitle,
                                    getResources().getString(R.string.action_filter_thumbnails_bookmarked)));
                            mIsReadOnly = mIsReadOnlySave;
                            break;
                    }
                    updateReadOnlyUI();
                }
            }
        });

        mItemTouchHelper = new ItemTouchHelper(new SimpleItemTouchHelperCallback(mAdapter, mSpanCount, false, false));
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        mItemClickHelper.setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView recyclerView, View v, final int position, final long id) {
                if (mActionMode == null) {
                    int page = mAdapter.getItem(position);
                    mAdapter.setCurrentPage(page);
                    mHasEventAction = true;
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_NAVIGATE_BY,
                            AnalyticsParam.viewerNavigateByParam(AnalyticsHandlerAdapter.VIEWER_NAVIGATE_BY_THUMBNAILS_VIEW));
                    dismiss();
                } else {
                    mItemSelectionHelper.setItemChecked(position, !mItemSelectionHelper.isItemChecked(position));
                    mActionMode.invalidate();
                }
            }
        });

        mItemClickHelper.setOnItemLongClickListener(new ItemClickHelper.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(RecyclerView recyclerView, View v, final int position, final long id) {
                if (mIsReadOnly) {
                    if (mOnThumbnailsEditAttemptWhileReadOnlyListener != null) {
                        mOnThumbnailsEditAttemptWhileReadOnlyListener.onThumbnailsEditAttemptWhileReadOnly();
                    }
                    return true;
                }
                if (mActionMode == null) {
                    mItemSelectionHelper.setItemChecked(position, true);

                    startActionMode();
                } else {
                    if (isNormalFilterMode()) {
                        mRecyclerView.post(new Runnable() {
                            @Override
                            public void run() {
                                RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(position);
                                if (holder != null && mItemTouchHelper != null) {
                                    mItemTouchHelper.startDrag(holder);
                                }
                            }
                        });
                    }
                }

                return true;
            }
        });

        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                        // Back key has been pressed
                        if (onBackPressed()) {
                            // Back key handled
                            return true;
                        } else {
                            dialog.dismiss();
                        }
                    }
                    return false;
                }
            });
        }

        mFabMenu = view.findViewById(R.id.fab_menu);
        mFabMenu.setClosedOnTouchOutside(true);
        if (mIsReadOnly) {
            mFabMenu.setVisibility(View.GONE);
        }

        FloatingActionButton pagePdfButton = mFabMenu.findViewById(R.id.page_pdf);
        if (mHideEditOptions.contains(ThumbnailsViewEditOptions.OPTION_INSERT_PAGES)) {
            pagePdfButton.setVisibility(View.GONE);
        }
        pagePdfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFabMenu.close(true);
                if (mIsReadOnly) {
                    if (mOnThumbnailsEditAttemptWhileReadOnlyListener != null)
                        mOnThumbnailsEditAttemptWhileReadOnlyListener.onThumbnailsEditAttemptWhileReadOnly();
                    return;
                }
                boolean shouldUnlockRead = false;
                try {
                    mPdfViewCtrl.docLockRead();
                    shouldUnlockRead = true;
                    Page lastPage = mPdfViewCtrl.getDoc().getPage(mPdfViewCtrl.getDoc().getPageCount());
                    AddPageDialogFragment addPageDialogFragment = AddPageDialogFragment.newInstance(
                                    lastPage.getPageWidth(), lastPage.getPageHeight())
                            .setInitialPageSize(AddPageDialogFragment.PageSize.Custom);
                    addPageDialogFragment.setOnAddNewPagesListener(new AddPageDialogFragment.OnAddNewPagesListener() {
                        @Override
                        public void onAddNewPages(Page[] pages) {
                            if (pages == null || pages.length == 0) {
                                return;
                            }

                            mAdapter.addDocPages(getLastSelectedPage(), ThumbnailsViewAdapter.DocumentFormat.PDF_PAGE, pages);
                            mHasEventAction = true;
                            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_THUMBNAILS_VIEW,
                                    AnalyticsParam.thumbnailsViewCountParam(AnalyticsHandlerAdapter.THUMBNAILS_VIEW_ADD_BLANK_PAGES, pages.length));
                        }
                    });
                    FragmentActivity activity = getActivity();
                    if (activity != null) {
                        addPageDialogFragment.show(activity.getSupportFragmentManager(), "add_page_dialog");
                    }
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } finally {
                    if (shouldUnlockRead) {
                        mPdfViewCtrl.docUnlockRead();
                    }
                }
            }
        });

        FloatingActionButton pdfDocButton = mFabMenu.findViewById(R.id.pdf_doc);
        if (mHideEditOptions.contains(ThumbnailsViewEditOptions.OPTION_INSERT_FROM_DOCUMENT)) {
            pdfDocButton.setVisibility(View.GONE);
        }
        pdfDocButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFabMenu.close(true);
                if (mIsReadOnly) {
                    if (mOnThumbnailsEditAttemptWhileReadOnlyListener != null)
                        mOnThumbnailsEditAttemptWhileReadOnlyListener.onThumbnailsEditAttemptWhileReadOnly();
                    return;
                }
                launchAndroidFilePicker();
            }
        });

        FloatingActionButton imagePdfButton = mFabMenu.findViewById(R.id.image_pdf);
        if (mHideEditOptions.contains(ThumbnailsViewEditOptions.OPTION_INSERT_FROM_IMAGE)) {
            imagePdfButton.setVisibility(View.GONE);
        }
        imagePdfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFabMenu.close(true);
                if (mIsReadOnly) {
                    if (mOnThumbnailsEditAttemptWhileReadOnlyListener != null)
                        mOnThumbnailsEditAttemptWhileReadOnlyListener.onThumbnailsEditAttemptWhileReadOnly();
                    return;
                }
                if (HANDLE_INTENT_IN_ACTIVITY) {
                    Activity activity = getActivity();
                    if (activity != null) {
                        mOutputFileUri = ViewerUtils.openImageIntent(activity);
                    }
                } else {
                    mOutputFileUri = ViewerUtils.openImageIntent(ThumbnailsViewFragment.this);
                }
            }
        });

        FragmentActivity activity = getActivity();
        if (activity != null) {
            PageLabelSettingViewModel mPageLabelViewModel =
                    ViewModelProviders.of(activity).get(PageLabelSettingViewModel.class);
            mPageLabelViewModel.observeOnComplete(getViewLifecycleOwner(), new Observer<Event<PageLabelSetting>>() {
                @Override
                public void onChanged(@Nullable Event<PageLabelSetting> pageLabelSettingEvent) {
                    if (pageLabelSettingEvent != null && !pageLabelSettingEvent.hasBeenHandled()) {
                        boolean isSuccessful = PageLabelUtils.setPageLabel(mPdfViewCtrl,
                                pageLabelSettingEvent.getContentIfNotHandled()
                        );
                        if (isSuccessful) {
                            mHasEventAction = true;
                            // Update the UI
                            mAdapter.updateAfterPageLabelEdit();
                            // Update undo/redo
                            managePageLabelChanged();
                            CommonToast.showText(getContext(), getString(R.string.page_label_success), Toast.LENGTH_LONG);
                        } else {
                            CommonToast.showText(getContext(), getString(R.string.page_label_failed), Toast.LENGTH_LONG);
                        }
                    }
                }
            });
        }

        loadAttributes();
    }

    private void loadAttributes() {
        Context context = getContext();
        if (null == context) {
            return;
        }
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ThumbnailBrowser, R.attr.thumbnail_browser, R.style.ThumbnailBrowserStyle);
        try {
            // show filter menu
            boolean showFilterMenuItem = a.getBoolean(R.styleable.ThumbnailBrowser_showFilterMenuItem, true);
            boolean showAnnotatedMenuItem = a.getBoolean(R.styleable.ThumbnailBrowser_showFilterAnnotated, true);
            boolean showBookmarkedMenuItem = a.getBoolean(R.styleable.ThumbnailBrowser_showFilterBookmarked, true);
            boolean showAddBookmarkMenuItem = a.getBoolean(R.styleable.ThumbnailBrowser_showAddBookmarkMenuItem, true);

            if (mHideFilterModes != null) {
                if (mHideFilterModes.contains(FILTER_MODE_ANNOTATED)) {
                    showAnnotatedMenuItem = false;
                }
                if (mHideFilterModes.contains(FILTER_MODE_BOOKMARKED)) {
                    showBookmarkedMenuItem = false;
                }
            }

            if (!showAnnotatedMenuItem && !showBookmarkedMenuItem) {
                // both filter options are disabled, should just disable the filter menu
                showFilterMenuItem = false;
            }
            if (mMenuItemFilter != null) {
                mMenuItemFilter.setVisible(showFilterMenuItem);
            }
            if (mMenuItemFilterAnnotated != null) {
                mMenuItemFilterAnnotated.setVisible(showAnnotatedMenuItem);
            }
            if (mMenuItemFilterBookmarked != null) {
                mMenuItemFilterBookmarked.setVisible(showBookmarkedMenuItem);
            }
            if (mMenuItemAddBookmark != null) {
                mMenuItemAddBookmark.setVisible(showAddBookmarkMenuItem);
            }
        } finally {
            a.recycle();
        }
    }

    protected void startActionMode() {
        mActionMode = new ToolbarActionMode(getActivity(), mCabToolbar);
        mActionMode.setMainToolbar(mToolbar);
        mActionMode.startActionMode(mActionModeCallback);
        setEditing(true);
    }

    protected void setEditing(boolean editing) {
        mAdapter.setEditing(editing);
    }

    protected void populateThumbList(final int mode) {
        if (mPdfViewCtrl == null || mPdfViewCtrl.getDoc() == null) {
            return;
        }
        mDisposables.clear();
        mDisposables.add(
                getPages(mPdfViewCtrl, mode)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe(new Consumer<Disposable>() {
                            @Override
                            public void accept(Disposable disposable) throws Exception {
                                mAdapter.clear();
                                mAdapter.notifyDataSetChanged();
                                mProgressBarView.setVisibility(View.VISIBLE);
                                mRecyclerView.setVisibility(View.GONE);
                            }
                        })
                        .subscribe(new Consumer<List<Integer>>() {
                            @Override
                            public void accept(List<Integer> integers) throws Exception {
                                mAdapter.addAll(integers);
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                // show error toast
                                mProgressBarView.setVisibility(View.GONE);
                                CommonToast.showText(getActivity(), R.string.error_generic_message, Toast.LENGTH_SHORT);

                                AnalyticsHandlerAdapter.getInstance().sendException(new RuntimeException(throwable));
                            }
                        }, new Action() {
                            @Override
                            public void run() throws Exception {
                                // on complete
                                updateUIVisibilityOnLoadComplete();

                                // adjust scroll position
                                if (mRecyclerView != null && mAdapter != null && mPdfViewCtrl != null) {
                                    int pos = mAdapter.getPositionForPage(mPdfViewCtrl.getCurrentPage());
                                    if (pos >= 0 && pos < mAdapter.getItemCount()) {
                                        mRecyclerView.scrollToPosition(pos);
                                    }
                                }
                                if (mStartInEdit) {
                                    // clear edit flag, we only want this to happen once
                                    mStartInEdit = false;
                                    if (mode == FILTER_MODE_NORMAL) {
                                        // edit mode only available in all page mode
                                        startActionMode();
                                        if (mInitSelectedItem != null) {
                                            mItemSelectionHelper.setItemChecked(mInitSelectedItem, true);
                                            mActionMode.invalidate();
                                            mInitSelectedItem = null;
                                        }
                                    }
                                }
                            }
                        })
        );
    }

    protected void updateUIVisibilityOnLoadComplete() {
        mProgressBarView.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    // we want to trigger a reload when switch filter because pages could have all changed in edit mode
    public static Observable<List<Integer>> getPages(final PDFViewCtrl pdfViewCtrl, final int mode) {
        return Observable.create(new ObservableOnSubscribe<List<Integer>>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<List<Integer>> emitter) throws Exception {
                if (pdfViewCtrl == null) {
                    emitter.onComplete();
                    return;
                }
                boolean shouldUnlockRead = false;
                try {
                    pdfViewCtrl.docLockRead();
                    shouldUnlockRead = true;

                    ArrayList<Integer> excludeList = new ArrayList<>();
                    excludeList.add(Annot.e_Link);
                    excludeList.add(Annot.e_Widget);

                    ArrayList<Integer> bookmarkedPages = new ArrayList<>();
                    if (mode == FILTER_MODE_BOOKMARKED) {
                        try {
                            bookmarkedPages = BookmarkManager.getPdfBookmarkedPageNumbers(pdfViewCtrl.getDoc());
                        } catch (Exception ignored) {
                        }
                    }

                    int pageCount = pdfViewCtrl.getDoc().getPageCount();
                    for (int pageNum = 1; pageNum <= pageCount; pageNum++) {
                        boolean canAdd = true;
                        if (mode == FILTER_MODE_ANNOTATED) {
                            int annotCount = AnnotUtils.getAnnotationCountOnPage(pdfViewCtrl, pageNum, excludeList);
                            canAdd = annotCount > 0;
                        }
                        if (mode == FILTER_MODE_BOOKMARKED) {
                            canAdd = bookmarkedPages.contains(pageNum);
                        }
                        if (canAdd) {
                            ArrayList<Integer> pages = new ArrayList<>();
                            pages.add(pageNum);
                            emitter.onNext(pages);
                        }
                    }
                } catch (Exception ex) {
                    emitter.onError(ex);
                } finally {
                    if (shouldUnlockRead) {
                        pdfViewCtrl.docUnlockRead();
                    }
                    emitter.onComplete();
                }
            }
        });
    }

    private void updateSharedPrefs(int filterMode) {
        Context context = getContext();
        if (context != null) {
            PdfViewCtrlSettingsManager.updateThumbListFilterMode(context, filterMode);
        }
    }

    protected void updateReadOnlyUI() {
        finishActionMode();
        if (mMenuItemEdit != null) {
            mMenuItemEdit.setVisible(!mIsReadOnly);
        }
        if (mMenuItemAddBookmark != null) {
            mMenuItemAddBookmark.setVisible(!mIsReadOnly);
        }
        if (mFabMenu != null) {
            boolean canShowFab = !mIsReadOnly &&
                    hasInsertOption() &&
                    !isBookmarkFilterMode();
            mFabMenu.setVisibility(canShowFab ? View.VISIBLE : View.GONE);
        }
    }

    protected boolean hasInsertOption() {
        return !(mHideEditOptions.contains(ThumbnailsViewEditOptions.OPTION_INSERT_PAGES) &&
                mHideEditOptions.contains(ThumbnailsViewEditOptions.OPTION_INSERT_FROM_IMAGE) &&
                mHideEditOptions.contains(ThumbnailsViewEditOptions.OPTION_INSERT_FROM_DOCUMENT));
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mPdfViewCtrl != null && mPdfViewCtrl.getToolManager() != null) {
            if (((ToolManager) mPdfViewCtrl.getToolManager()).canResumePdfDocWithoutReloading()) {
                addDocPages();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        AnalyticsHandlerAdapter.getInstance().sendTimedEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_THUMBNAILS_VIEW_OPEN);
    }

    @Override
    public void onStop() {
        super.onStop();
        AnalyticsHandlerAdapter.getInstance().endTimedEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_THUMBNAILS_VIEW_OPEN);
    }

    /**
     * Adds document pages.
     */
    public void addDocPages() {
        if (mAddDocPagesDelay && mDataDelay != null) {
            mAddDocPagesDelay = false;
            mAdapter.addDocPages(mPositionDelay, mDocumentFormatDelay, mDataDelay);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mOutputFileUri != null) {
            outState.putParcelable(BUNDLE_OUTPUT_FILE_URI, mOutputFileUri);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == RequestCode.PICK_PDF_FILE || requestCode == RequestCode.PICK_PHOTO_CAM) {
            // save the data and add pages to the document after onResume is called.
            mPositionDelay = getLastSelectedPage();
            if (requestCode == RequestCode.PICK_PDF_FILE) {
                mDocumentFormatDelay = ThumbnailsViewAdapter.DocumentFormat.PDF_DOC;
                if (data == null || data.getData() == null) {
                    return;
                }
                mDataDelay = data.getData();
            } else {
                mDocumentFormatDelay = ThumbnailsViewAdapter.DocumentFormat.IMAGE;
                try {
                    Map imageIntent = ViewerUtils.readImageIntent(data, activity, mOutputFileUri);
                    if (!ViewerUtils.checkImageIntent(imageIntent)) {
                        Utils.handlePdfFromImageFailed(activity, imageIntent);
                        return;
                    }
                    mDataDelay = ViewerUtils.getImageUri(imageIntent);
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_THUMBNAILS_VIEW,
                            AnalyticsParam.thumbnailsViewParam(ViewerUtils.isImageFromCamera(imageIntent) ?
                                    AnalyticsHandlerAdapter.THUMBNAILS_VIEW_ADD_PAGE_FROM_CAMERA : AnalyticsHandlerAdapter.THUMBNAILS_VIEW_ADD_PAGE_FROM_IMAGE));
                } catch (FileNotFoundException e) {
                    // ignore
                }
            }
            if (mDataDelay != null) {
                mAddDocPagesDelay = true;
                mHasEventAction = true;
            }
        }
    }

    /**
     * Sets the PDFViewCtrl.
     *
     * @param pdfViewCtrl Sets the PDFViewCtrl
     * @return The instance of the class
     */
    public ThumbnailsViewFragment setPdfViewCtrl(@NonNull PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
        return this;
    }

    /**
     * Sets the OnThumbnailsViewDialogDismissListener listener
     *
     * @param listener The listener
     */
    public void setOnThumbnailsViewDialogDismissListener(OnThumbnailsViewDialogDismissListener listener) {
        mOnThumbnailsViewDialogDismissListener = listener;
    }

    /**
     * Sets the OnThumbnailsViewDialogDismissListener listener
     *
     * @param listener The listener
     */
    public void setOnThumbnailsEditAttemptWhileReadOnlyListener(OnThumbnailsEditAttemptWhileReadOnlyListener listener) {
        mOnThumbnailsEditAttemptWhileReadOnlyListener = listener;
    }

    /**
     * Sets the OnThumbnailsViewDialogDismissListener listener
     *
     * @param listener The listener
     */
    public void setOnExportThumbnailsListener(OnExportThumbnailsListener listener) {
        mOnExportThumbnailsListener = listener;
    }

    /**
     * Sets the specified item as checked.
     *
     * @param position The position
     */
    public void setItemChecked(int position) {
        mInitSelectedItem = position;
    }

    private void launchAndroidFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // Show pdf files only
        intent.setType("*/*");
        // Restrict to URIs that can be opened with ContentResolver#openFileDescriptor(Uri, String)
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Force advanced devices (SD cards) to always be visible
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
        // Reference: https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/provider/DocumentsContract.java#109
        intent.putExtra("android.provider.extra.SHOW_ADVANCED", true);
        if (HANDLE_INTENT_IN_ACTIVITY) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.startActivityForResult(intent, RequestCode.PICK_PDF_FILE);
            }
        } else {
            startActivityForResult(intent, RequestCode.PICK_PDF_FILE);
        }
    }

    private int getLastSelectedPage() {
        int lastSelectedPage = -1;
        if (mItemSelectionHelper != null && mAdapter != null) {
            if (mItemSelectionHelper.getCheckedItemCount() > 0) {
                lastSelectedPage = Integer.MIN_VALUE; // page-indexed
                SparseBooleanArray selectedItems = mItemSelectionHelper.getCheckedItemPositions();
                for (int i = 0; i < selectedItems.size(); i++) {
                    if (selectedItems.valueAt(i)) {
                        int position = selectedItems.keyAt(i);
                        Integer itemMap = mAdapter.getItem(position);
                        if (itemMap != null) {
                            int pageNum = itemMap;
                            if (pageNum > lastSelectedPage) {
                                lastSelectedPage = pageNum;
                            }
                        }
                    }
                }
            }
        }

        // (page-indexed, so conversion to zero-index is (lastSelectedPage-1)+1)
        // NOTE: pageCount+1 is allowed
        return lastSelectedPage;
    }

    /**
     * Sets the title.
     *
     * @param title The title
     */
    public void setTitle(String title) {
        mTitle = title;
        if (mToolbar != null)
            mToolbar.setTitle(title);
    }

    public void setPageLabelBackgroundColor(@ColorInt int pageLabelBackgroundColorInt) {
        if (mAdapter != null) {
            mAdapter.setPageNumberBackgroundLabelColor(pageLabelBackgroundColorInt);
            mAdapter.notifyDataSetChanged();
        }
    }

    public void setActivePageLabelBackgroundColor(@ColorInt int activePageLabelBackgroundColorInt) {
        if (mAdapter != null) {
            mAdapter.setActivePageNumberBackgroundLabelColor(activePageLabelBackgroundColorInt);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mAdapter != null) {
            int viewWidth = getDisplayWidth();
            int thumbSize = getResources().getDimensionPixelSize(R.dimen.controls_thumbnails_view_image_width);
            int thumbSpacing = getResources().getDimensionPixelSize(R.dimen.controls_thumbnails_view_grid_spacing);
            // Calculate number of columns
            mSpanCount = (int) Math.floor(viewWidth / (double) (thumbSize + thumbSpacing));

            mAdapter.updateMainViewWidth(viewWidth);
            updateSpanCount(mSpanCount); // notifyDataSetChanged will be called
        }

        if (mActionMode != null) {
            mActionMode.invalidate();
        }
    }

    private int getMainViewWidth() {
        if (mRecyclerView != null && ViewCompat.isLaidOut(mRecyclerView)) {
            return mRecyclerView.getMeasuredWidth();
        } else {
            return getDisplayWidth();
        }
    }

    private int getDisplayWidth() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        return metrics.widthPixels;
    }

    /**
     * Updates span count.
     *
     * @param count The span count
     */
    public void updateSpanCount(int count) {
        mSpanCount = count;
        mRecyclerView.updateSpanCount(count);
    }

    /**
     * @return The attached adapter
     */
    public ThumbnailsViewAdapter getAdapter() {
        return mAdapter;
    }

    protected boolean finishActionMode() {
        boolean success = false;
        if (mActionMode != null) {
            success = true;
            mActionMode.finish();
            mActionMode = null;
        }
        setEditing(false);
        clearSelectedList();
        return success;
    }

    protected void clearSelectedList() {
        if (mItemSelectionHelper != null) {
            mItemSelectionHelper.clearChoices();
        }
        if (mActionMode != null) {
            mActionMode.invalidate();
        }
    }

    private boolean onBackPressed() {
        if (!isAdded()) {
            return false;
        }

        boolean handled = false;
        if (mActionMode != null) {
            handled = finishActionMode();
        }
        return handled;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mDisposables.clear();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        if (mPdfViewCtrl == null || mPdfViewCtrl.getDoc() == null) {
            // only proceed if the document is still open
            return;
        }

        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_THUMBNAILS_VIEW_CLOSE,
                AnalyticsParam.noActionParam(mHasEventAction));

        if (mAdapter.getDocPagesModified()) {
            // update page layout if document is modified
            ViewerUtils.safeUpdatePageLayout(mPdfViewCtrl, new ExceptionHandlerCallback() {
                @Override
                public void onException(Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
            });
        }

        try {
            // set current page to updated current page
            mPdfViewCtrl.setCurrentPage(mAdapter.getCurrentPage());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        // free resource
        mAdapter.clearResources();
        mAdapter.finish();
        // cancel remaining request
        try {
            mPdfViewCtrl.cancelAllThumbRequests();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        // callback
        if (mOnThumbnailsViewDialogDismissListener != null) {
            mOnThumbnailsViewDialogDismissListener.onThumbnailsViewDialogDismiss(mAdapter.getCurrentPage(), mAdapter.getDocPagesModified());
        }
    }

    protected void rotateSelectedPages(boolean clockwise) {
        // rotate all selected pages
        SparseBooleanArray selectedItems = mItemSelectionHelper.getCheckedItemPositions();
        List<Integer> pageList = new ArrayList<>();
        for (int i = 0; i < selectedItems.size(); i++) {
            if (selectedItems.valueAt(i)) {
                int position = selectedItems.keyAt(i);
                mAdapter.rotateDocPage(position + 1, clockwise);
                pageList.add(position + 1);
            }
        }
        manageRotatePages(pageList);
        mHasEventAction = true;
        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_THUMBNAILS_VIEW,
                AnalyticsParam.thumbnailsViewCountParam(AnalyticsHandlerAdapter.THUMBNAILS_VIEW_ROTATE, selectedItems.size()));
    }

    private final ToolbarActionMode.Callback mActionModeCallback = new ToolbarActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ToolbarActionMode mode, Menu menu) {
            mode.inflateMenu(R.menu.cab_controls_fragment_thumbnails_view);

            mMenuItemUndo = menu.findItem(R.id.controls_thumbnails_view_action_undo);
            mMenuItemRedo = menu.findItem(R.id.controls_thumbnails_view_action_redo);
            mMenuItemRotate = menu.findItem(R.id.controls_thumbnails_view_action_rotate);
            mMenuItemDelete = menu.findItem(R.id.controls_thumbnails_view_action_delete);
            mMenuItemDuplicate = menu.findItem(R.id.controls_thumbnails_view_action_duplicate);
            mMenuItemExport = menu.findItem(R.id.controls_thumbnails_view_action_export);
            mMenuItemPageLabel = menu.findItem(R.id.controls_thumbnails_view_action_page_label);
            mMenuItemRemoveBookmark = menu.findItem(R.id.controls_thumbnails_view_action_remove_bookmark);

            if (isNormalFilterMode()) {
                if (mMenuItemRemoveBookmark != null) {
                    mMenuItemRemoveBookmark.setVisible(false);
                }
                if (mMenuItemExport != null) {
                    mMenuItemExport.setVisible(mOnExportThumbnailsListener != null);
                }
            } else if (isBookmarkFilterMode()) {
                if (mMenuItemUndo != null) {
                    mMenuItemUndo.setVisible(false);
                }
                if (mMenuItemRedo != null) {
                    mMenuItemRedo.setVisible(false);
                }
                if (mMenuItemRotate != null) {
                    mMenuItemRotate.setVisible(false);
                }
                if (mMenuItemDelete != null) {
                    mMenuItemDelete.setVisible(false);
                }
                if (mMenuItemDuplicate != null) {
                    mMenuItemDuplicate.setVisible(false);
                }
                if (mMenuItemExport != null) {
                    mMenuItemExport.setVisible(false);
                }
                if (mMenuItemPageLabel != null) {
                    mMenuItemPageLabel.setVisible(false);
                }
            }
            if (mHideEditOptions.contains(ThumbnailsViewEditOptions.OPTION_DELETE_PAGES)) {
                if (mMenuItemDelete != null) {
                    mMenuItemDelete.setVisible(false);
                }
            }
            if (mHideEditOptions.contains(ThumbnailsViewEditOptions.OPTION_EXPORT_PAGES)) {
                if (mMenuItemExport != null) {
                    mMenuItemExport.setVisible(false);
                }
            }
            if (mHideEditOptions.contains(ThumbnailsViewEditOptions.OPTION_DUPLICATE_PAGES)) {
                if (mMenuItemDuplicate != null) {
                    mMenuItemDuplicate.setVisible(false);
                }
            }
            if (mHideEditOptions.contains(ThumbnailsViewEditOptions.OPTION_PAGE_LABEL)) {
                if (mMenuItemPageLabel != null) {
                    mMenuItemPageLabel.setVisible(false);
                }
            }
            if (mHideEditOptions.contains(ThumbnailsViewEditOptions.OPTION_ROTATE_PAGES)) {
                if (mMenuItemRotate != null) {
                    mMenuItemRotate.setVisible(false);
                }
            }

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ToolbarActionMode mode, Menu menu) {
            boolean isEnabled = mItemSelectionHelper.getCheckedItemCount() > 0;

            if (mMenuItemRotate != null) {
                mMenuItemRotate.setEnabled(isEnabled);
                if (mMenuItemRotate.getIcon() != null) {
                    mMenuItemRotate.getIcon().setAlpha(isEnabled ? 255 : 150);
                }
            }
            if (mMenuItemDelete != null) {
                mMenuItemDelete.setEnabled(isEnabled);
                if (mMenuItemDelete.getIcon() != null) {
                    mMenuItemDelete.getIcon().setAlpha(isEnabled ? 255 : 150);
                }
            }
            if (mMenuItemDuplicate != null) {
                mMenuItemDuplicate.setEnabled(isEnabled);
                if (mMenuItemDuplicate.getIcon() != null) {
                    mMenuItemDuplicate.getIcon().setAlpha(isEnabled ? 255 : 150);
                }
            }
            if (mMenuItemExport != null) {
                mMenuItemExport.setEnabled(isEnabled);
                if (mMenuItemExport.getIcon() != null) {
                    mMenuItemExport.getIcon().setAlpha(isEnabled ? 255 : 150);
                }
            }
            if (mMenuItemPageLabel != null) {
                mMenuItemPageLabel.setEnabled(isEnabled);
                if (mMenuItemPageLabel.getIcon() != null) {
                    mMenuItemPageLabel.getIcon().setAlpha(isEnabled ? 255 : 150);
                }
            }
            if (mMenuItemRemoveBookmark != null) {
                mMenuItemRemoveBookmark.setEnabled(isEnabled);
            }

            if (Utils.isTablet(getContext()) || getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mode.setTitle(getString(R.string.controls_thumbnails_view_selected,
                        Utils.getLocaleDigits(Integer.toString(mItemSelectionHelper.getCheckedItemCount()))));
            } else {
                mode.setTitle(Utils.getLocaleDigits(Integer.toString(mItemSelectionHelper.getCheckedItemCount())));
            }
            updateUndoRedoIcons();
            return true;
        }

        @Override
        public boolean onActionItemClicked(ToolbarActionMode mode, MenuItem item) {
            if (mPdfViewCtrl == null) {
                throw new NullPointerException("setPdfViewCtrl() must be called with a valid PDFViewCtrl");
            }

            if (item.getItemId() == R.id.controls_thumbnails_view_action_rotate) {
                if (mIsReadOnly) {
                    if (mOnThumbnailsEditAttemptWhileReadOnlyListener != null)
                        mOnThumbnailsEditAttemptWhileReadOnlyListener.onThumbnailsEditAttemptWhileReadOnly();
                    return true;
                }
                rotateSelectedPages(true);
            } else if (item.getItemId() == R.id.controls_thumbnails_view_action_delete) {
                if (mIsReadOnly) {
                    if (mOnThumbnailsEditAttemptWhileReadOnlyListener != null)
                        mOnThumbnailsEditAttemptWhileReadOnlyListener.onThumbnailsEditAttemptWhileReadOnly();
                    return true;
                }
                // Need to convert checked-positions to a sortable list
                List<Integer> pageList = new ArrayList<>();
                SparseBooleanArray selectedItems = mItemSelectionHelper.getCheckedItemPositions();

                int pageCount;
                boolean shouldUnlockRead = false;
                try {
                    mPdfViewCtrl.docLockRead();
                    shouldUnlockRead = true;
                    pageCount = mPdfViewCtrl.getDoc().getPageCount();
                } catch (Exception ex) {
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                    return true;
                } finally {
                    if (shouldUnlockRead) {
                        mPdfViewCtrl.docUnlockRead();
                    }
                }

                if (selectedItems.size() >= pageCount) {
                    CommonToast.showText(getContext(), R.string.controls_thumbnails_view_delete_msg_all_pages);
                    clearSelectedList();
                    return true;
                }

                for (int i = 0; i < selectedItems.size(); i++) {
                    if (selectedItems.valueAt(i)) {
                        pageList.add(selectedItems.keyAt(i) + 1);
                    }
                }
                // delete should start from back
                Collections.sort(pageList, Collections.reverseOrder());
                int count = pageList.size();
                for (int i = 0; i < count; ++i) {
                    mAdapter.removeDocPage(pageList.get(i));
                }
                clearSelectedList();
                manageDeletePages(pageList);
                mHasEventAction = true;
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_THUMBNAILS_VIEW,
                        AnalyticsParam.thumbnailsViewCountParam(AnalyticsHandlerAdapter.THUMBNAILS_VIEW_DELETE, selectedItems.size()));
            } else if (item.getItemId() == R.id.controls_thumbnails_view_action_duplicate) {
                if (mAdapter != null) {
                    List<Integer> pageList = new ArrayList<>();
                    SparseBooleanArray selectedItems = mItemSelectionHelper.getCheckedItemPositions();
                    for (int i = 0; i < selectedItems.size(); i++) {
                        if (selectedItems.valueAt(i)) {
                            pageList.add(selectedItems.keyAt(i) + 1);
                        }
                    }
                    mAdapter.duplicateDocPages(pageList);
                    mHasEventAction = true;
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_THUMBNAILS_VIEW,
                            AnalyticsParam.thumbnailsViewCountParam(AnalyticsHandlerAdapter.THUMBNAILS_VIEW_DUPLICATE, selectedItems.size()));
                }
            } else if (item.getItemId() == R.id.controls_thumbnails_view_action_export) {
                if (mOnExportThumbnailsListener != null) {
                    SparseBooleanArray selectedItems = mItemSelectionHelper.getCheckedItemPositions();
                    mOnExportThumbnailsListener.onExportThumbnails(selectedItems);
                    mHasEventAction = true;
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_THUMBNAILS_VIEW,
                            AnalyticsParam.thumbnailsViewCountParam(AnalyticsHandlerAdapter.THUMBNAILS_VIEW_EXPORT, selectedItems.size()));
                }
            } else if (item.getItemId() == R.id.controls_thumbnails_view_action_page_label) {
                if (mAdapter == null) {
                    return true;
                }

                SparseBooleanArray selectedItems = mItemSelectionHelper.getCheckedItemPositions();
                int fromPage = Integer.MAX_VALUE;
                int toPage = -1;
                for (int i = 0; i < selectedItems.size(); i++) {
                    if (selectedItems.valueAt(i)) {
                        int page = selectedItems.keyAt(i) + 1;
                        toPage = Math.max(page, toPage);
                        fromPage = Math.min(page, fromPage);
                    }
                }

                final int numPages = mPdfViewCtrl.getPageCount();
                // If this is true, then return. We are somehow at an invalid state.
                if (fromPage < 1 || toPage < 1 || toPage < fromPage || fromPage > numPages) {
                    CommonToast.showText(getContext(), getString(R.string.page_label_failed), Toast.LENGTH_LONG);
                    return true;
                }

                // If only one page is selected, then just create a page label
                // component for the select page, otherwise create a component
                // for the page range
                FragmentActivity activity = getActivity();
                FragmentManager fragManager = getFragmentManager();
                if (fragManager != null && activity != null) {
                    String prefix = PageLabelUtils.getPageLabelPrefix(mPdfViewCtrl, fromPage);
                    PageLabelDialog dialog = PageLabelDialog.newInstance(fromPage, toPage, numPages, prefix);
                    dialog.setStyle(DialogFragment.STYLE_NO_TITLE, getTheme());
                    dialog.show(fragManager, PageLabelDialog.TAG);
                }
            } else if (item.getItemId() == R.id.controls_thumbnails_view_action_undo) {
                ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                if (toolManager != null) {
                    String undoInfo = toolManager.getUndoRedoManger().undo(AnalyticsHandlerAdapter.LOCATION_THUMBNAILS_VIEW, true);
                    updateUndoRedoIcons();
                    if (!Utils.isNullOrEmpty(undoInfo)) {
                        try {
                            if (UndoRedoManager.isDeletePagesAction(getContext(), undoInfo)) {
                                List<Integer> pageList = UndoRedoManager.getPageList(undoInfo);
                                if (pageList.size() != 0) {
                                    mAdapter.updateAfterAddition(pageList);
                                }
                            } else if (UndoRedoManager.isAddPagesAction(getContext(), undoInfo)) {
                                List<Integer> pageList = UndoRedoManager.getPageList(undoInfo);
                                if (pageList.size() != 0) {
                                    mAdapter.updateAfterDeletion(pageList);
                                }
                            } else if (UndoRedoManager.isRotatePagesAction(getContext(), undoInfo)) {
                                List<Integer> pageList = UndoRedoManager.getPageList(undoInfo);
                                if (pageList.size() != 0) {
                                    mAdapter.updateAfterRotation(pageList);
                                }
                            } else if (UndoRedoManager.isMovePageAction(getContext(), undoInfo)) {
                                mAdapter.updateAfterMove(UndoRedoManager.getPageTo(undoInfo),
                                        UndoRedoManager.getPageFrom(undoInfo));
                            } else if (UndoRedoManager.isEditPageLabelsAction(getContext(), undoInfo)) {
                                mAdapter.updateAfterPageLabelEdit();
                            }
                        } catch (Exception e) {
                            AnalyticsHandlerAdapter.getInstance().sendException(e);
                        }
                    }
                }
            } else if (item.getItemId() == R.id.controls_thumbnails_view_action_redo) {
                ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                if (toolManager != null) {
                    String redoInfo = toolManager.getUndoRedoManger().redo(AnalyticsHandlerAdapter.LOCATION_THUMBNAILS_VIEW, true);
                    updateUndoRedoIcons();
                    if (!Utils.isNullOrEmpty(redoInfo)) {
                        try {
                            if (UndoRedoManager.isDeletePagesAction(getContext(), redoInfo)) {
                                List<Integer> pageList = UndoRedoManager.getPageList(redoInfo);
                                if (pageList.size() != 0) {
                                    mAdapter.updateAfterDeletion(pageList);
                                }
                            } else if (UndoRedoManager.isAddPagesAction(getContext(), redoInfo)) {
                                List<Integer> pageList = UndoRedoManager.getPageList(redoInfo);
                                if (pageList.size() != 0) {
                                    mAdapter.updateAfterAddition(pageList);
                                }
                            } else if (UndoRedoManager.isRotatePagesAction(getContext(), redoInfo)) {
                                List<Integer> pageList = UndoRedoManager.getPageList(redoInfo);
                                if (pageList.size() != 0) {
                                    mAdapter.updateAfterRotation(pageList);
                                }
                            } else if (UndoRedoManager.isMovePageAction(getContext(), redoInfo)) {
                                mAdapter.updateAfterMove(UndoRedoManager.getPageFrom(redoInfo),
                                        UndoRedoManager.getPageTo(redoInfo));
                            } else if (UndoRedoManager.isEditPageLabelsAction(getContext(), redoInfo)) {
                                mAdapter.updateAfterPageLabelEdit();
                            }
                        } catch (Exception e) {
                            AnalyticsHandlerAdapter.getInstance().sendException(e);
                        }
                    }
                }
            } else if (item.getItemId() == R.id.controls_thumbnails_view_action_remove_bookmark) {
                SparseBooleanArray selectedItems = mItemSelectionHelper.getCheckedItemPositions();
                for (int i = 0; i < selectedItems.size(); i++) {
                    if (selectedItems.valueAt(i)) {
                        int position = selectedItems.keyAt(i);
                        Integer page = mAdapter.getItem(position);
                        if (page != null) {
                            ViewerUtils.removePageBookmark(mPdfViewCtrl.getContext(), mIsReadOnlySave, mPdfViewCtrl, page);
                        }
                    }
                }
                if (isBookmarkFilterMode()) {
                    populateThumbList(FILTER_MODE_BOOKMARKED);
                }
                finishActionMode();
            }

            return true;
        }

        @Override
        public void onDestroyActionMode(ToolbarActionMode mode) {
            mActionMode = null;
            finishActionMode();
        }
    };

    protected void manageAddPages(List<Integer> pageList) {
        if (mPdfViewCtrl == null) {
            throw new NullPointerException("setPdfViewCtrl() must be called with a valid PDFViewCtrl");
        }

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        if (toolManager != null) {
            toolManager.raisePagesAdded(pageList);
        }

        updateUndoRedoIcons();
    }

    protected void manageDeletePages(List<Integer> pageList) {
        if (mPdfViewCtrl == null) {
            throw new NullPointerException("setPdfViewCtrl() must be called with a valid PDFViewCtrl");
        }

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        if (toolManager != null) {
            toolManager.raisePagesDeleted(pageList);
        }

        updateUndoRedoIcons();
    }

    protected void manageRotatePages(List<Integer> pageList) {
        if (mPdfViewCtrl == null) {
            throw new NullPointerException("setPdfViewCtrl() must be called with a valid PDFViewCtrl");
        }

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        if (toolManager != null) {
            toolManager.raisePagesRotated(pageList);
        }

        updateUndoRedoIcons();
    }

    private void manageMovePage(int fromPageNum, int toPageNum) {
        if (mPdfViewCtrl == null) {
            throw new NullPointerException("setPdfViewCtrl() must be called with a valid PDFViewCtrl");
        }

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        if (toolManager != null) {
            toolManager.raisePageMoved(fromPageNum, toPageNum);
        }

        updateUndoRedoIcons();
    }

    /**
     * Calls {@link ToolManager#raisePageLabelChangedEvent()} to save state
     * for undo/redo. Also updates the undo/redo UI buttons accordingly
     */
    private void managePageLabelChanged() {
        if (mPdfViewCtrl == null) {
            throw new NullPointerException("setPdfViewCtrl() must be called with a valid PDFViewCtrl");
        }

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        if (toolManager != null) {
            toolManager.raisePageLabelChangedEvent();
        }

        updateUndoRedoIcons();
    }

    /**
     * The overload implementation of {@link ThumbnailsViewAdapter.EditPagesListener#onPagesAdded(List)}.
     */
    @Override
    public void onPagesAdded(List<Integer> pageList) {
        manageAddPages(pageList);
        if (mDocumentFormatDelay != null) {
            if (mDocumentFormatDelay == ThumbnailsViewAdapter.DocumentFormat.PDF_DOC) {
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_THUMBNAILS_VIEW,
                        AnalyticsParam.thumbnailsViewCountParam(AnalyticsHandlerAdapter.THUMBNAILS_VIEW_ADD_PAGES_FROM_DOCS, pageList.size()));
            }
            mDocumentFormatDelay = null;
        }
    }

    /**
     * The overload implementation of {@link ThumbnailsViewAdapter.EditPagesListener#onPageMoved(int, int)}.
     */
    @Override
    public void onPageMoved(int fromPageNum, int toPageNum) {
        manageMovePage(fromPageNum, toPageNum);
        // move pages event
        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_THUMBNAILS_VIEW, AnalyticsParam.thumbnailsViewParam(AnalyticsHandlerAdapter.THUMBNAILS_VIEW_MOVE));
    }

    /**
     * Updates undo/redo icons
     */
    public void updateUndoRedoIcons() {
        if (mPdfViewCtrl == null) {
            throw new NullPointerException("setPdfViewCtrl() must be called with a valid PDFViewCtrl");
        }

        if (mMenuItemUndo != null && mMenuItemRedo != null) {
            boolean undoEnabled = false;
            boolean redoEnabled = false;

            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            if (toolManager != null) {
                UndoRedoManager undoRedoManager = toolManager.getUndoRedoManger();
                undoEnabled = undoRedoManager.isNextUndoEditPageAction();
                redoEnabled = undoRedoManager.isNextRedoEditPageAction();
            }

            mMenuItemUndo.setEnabled(undoEnabled);
            if (mMenuItemUndo.getIcon() != null) {
                mMenuItemUndo.getIcon().setAlpha(undoEnabled ? 255 : 150);
            }
            mMenuItemRedo.setEnabled(redoEnabled);
            if (mMenuItemRedo.getIcon() != null) {
                mMenuItemRedo.getIcon().setAlpha(redoEnabled ? 255 : 150);
            }
        }
    }

    protected boolean isNormalFilterMode() {
        Integer mode = mFilterMode.getFilterMode();
        return (mode == null || mode == FILTER_MODE_NORMAL);
    }

    protected boolean isBookmarkFilterMode() {
        Integer mode = mFilterMode.getFilterMode();
        return (mode != null && mode == FILTER_MODE_BOOKMARKED);
    }

    /**
     * Callback interface to be invoked when the dialog fragment is dismissed.
     */
    public interface OnThumbnailsViewDialogDismissListener {
        /**
         * Called when the thumbnails view dialog has been dismissed.
         *
         * @param pageNum          The selected page number
         * @param docPagesModified True if the pages of the document has been modified
         */
        void onThumbnailsViewDialogDismiss(int pageNum, boolean docPagesModified);
    }

    /**
     * Callback interface to be invoked when the user attempts to edit pages while the document is read only.
     */
    public interface OnThumbnailsEditAttemptWhileReadOnlyListener {
        /**
         * Called when the user attempts to edit pages while the document is read only.
         */
        void onThumbnailsEditAttemptWhileReadOnly();
    }

    /**
     * Callback interface to be invoked when pages should be exported.
     */
    public interface OnExportThumbnailsListener {
        /**
         * The implementation should export given pages.
         *
         * @param pageNums The page numbers to be exported
         */
        void onExportThumbnails(SparseBooleanArray pageNums);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class Theme {

        @ColorInt
        public final int pageNumberTextColor;
        @ColorInt
        public final int pageNumberBackgroundColor;
        @ColorInt
        public final int activePageNumberTextColor;
        @ColorInt
        public final int activePageNumberBackgroundColor;

        public Theme(int pageNumberTextColor,
                int pageNumberBackgroundColor,
                int activePageNumberTextColor,
                int activePageNumberBackgroundColor) {
            this.pageNumberTextColor = pageNumberTextColor;
            this.pageNumberBackgroundColor = pageNumberBackgroundColor;
            this.activePageNumberTextColor = activePageNumberTextColor;
            this.activePageNumberBackgroundColor = activePageNumberBackgroundColor;
        }

        public static ThumbnailsViewFragment.Theme fromContext(@NonNull Context context) {

            final TypedArray a = context.obtainStyledAttributes(
                    null, R.styleable.ThumbnailsViewTheme, R.attr.pt_thumbnails_view_style, R.style.PTThumbnailsViewTheme);
            int pageNumberTextColor = a.getColor(R.styleable.ThumbnailsViewTheme_textColor,
                    context.getResources().getColor(R.color.controls_thumbnails_view_page_label_text));
            int pageNumberBackgroundColor = a.getColor(R.styleable.ThumbnailsViewTheme_backgroundColor,
                    context.getResources().getColor(R.color.controls_thumbnails_view_page_label_background));
            int activePageNumberTextColor = a.getColor(R.styleable.ThumbnailsViewTheme_activeTextColor,
                    context.getResources().getColor(R.color.controls_thumbnails_view_active_page_label_text));
            int activePageNumberBackgroundColor = a.getColor(R.styleable.ThumbnailsViewTheme_activeBackgroundColor,
                    context.getResources().getColor(R.color.controls_thumbnails_view_active_page_label_background));

            a.recycle();

            return new ThumbnailsViewFragment.Theme(pageNumberTextColor, pageNumberBackgroundColor, activePageNumberTextColor, activePageNumberBackgroundColor);
        }
    }
}
