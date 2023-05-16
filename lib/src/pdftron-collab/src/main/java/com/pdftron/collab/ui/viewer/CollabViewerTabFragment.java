package com.pdftron.collab.ui.viewer;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProviders;

import com.pdftron.collab.R;
import com.pdftron.collab.db.entity.LastAnnotationEntity;
import com.pdftron.collab.service.CustomServiceUtils;
import com.pdftron.collab.ui.annotlist.component.AnnotationListViewModel;
import com.pdftron.collab.ui.annotlist.model.list.item.AnnotationListContent;
import com.pdftron.collab.ui.reply.component.ReplyUIViewModel;
import com.pdftron.collab.ui.reply.model.ReplyHeader;
import com.pdftron.collab.ui.reply.model.ReplyInput;
import com.pdftron.collab.ui.reply.model.ReplyMessage;
import com.pdftron.collab.ui.reply.model.ReplyMessages;
import com.pdftron.collab.ui.reply.model.User;
import com.pdftron.collab.viewmodel.AnnotationViewModel;
import com.pdftron.collab.viewmodel.DocumentViewModel;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.config.ToolConfig;
import com.pdftron.pdf.controls.PdfViewCtrlTabFragment;
import com.pdftron.pdf.model.AnnotReviewState;
import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.tools.AnnotManager;
import com.pdftron.pdf.tools.QuickMenu;
import com.pdftron.pdf.tools.QuickMenuItem;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.Logger;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/**
 * @deprecated use {@link CollabViewerTabFragment2} instead
 * <p>
 * A {@link PdfViewCtrlTabFragment} that has real-time annotation collaboration functionality.
 * Implements a bottom sheet reply fragment that allows for real-time comments on annotations
 */
@Deprecated
public class CollabViewerTabFragment extends PdfViewCtrlTabFragment {

    private static final String TAG = CollabViewerTabFragment.class.getName();

    public interface CollabTabListener {
        CollabManager getCollabManager();
    }

    public static final String BUNDLE_REPLY_THEME = "bundle_tab_reply_style";

    @StyleRes
    protected int mReplyTheme;
    protected String mDocumentId;

    @Nullable
    protected DocumentViewModel mDocumentViewModel;
    @Nullable
    protected AnnotationViewModel mAnnotationViewModel;
    @Nullable
    protected ReplyUIViewModel mReplyUiViewModel;

    private boolean mInitialAnnotsMerged = false;

    @Nullable
    private OpenAnnotationListListener mOpenAnnotListListener;

    @Nullable
    private CollabTabListener mCollabTabListener;

    protected final CompositeDisposable mDisposables = new CompositeDisposable();

    protected final CollabViewerTabImpl mImpl = new CollabViewerTabImpl();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBookmarkDialogCurrentTab = 2;
        mLocalReadOnlyChecked = true; // skip readonly check
        mSavingEnabled = false; // disable saving in collab
        Bundle bundle = getArguments();
        if (bundle == null) {
            throw new NullPointerException("bundle cannot be null");
        }

        mContentLayout = bundle.getInt(BUNDLE_TAB_CONTENT_LAYOUT, getContentLayoutRes());
        int annotManagerUndoMode = bundle.getInt(BUNDLE_TAB_ANNOTATION_MANAGER_UNDO_MODE, PDFViewCtrl.AnnotationManagerMode.ADMIN_UNDO_OWN.getValue());
        mImpl.setAnnotationManagerUndoMode(PDFViewCtrl.AnnotationManagerMode.valueOf(annotManagerUndoMode));

        String annotManagerEditMode = bundle.getString(BUNDLE_TAB_ANNOTATION_MANAGER_EDIT_MODE, AnnotManager.EditPermissionMode.EDIT_OWN.name());
        mImpl.setAnnotationManagerEditMode(AnnotManager.EditPermissionMode.valueOf(annotManagerEditMode));

        if (mViewerConfig != null) {
            mImpl.setAlwaysShowAsReply(mViewerConfig.alwaysShowAsReply());
        }

        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        setUpReply(activity);
        setUpAnnotList(activity);
        mReplyTheme = bundle.getInt(BUNDLE_REPLY_THEME);

        if (mReplyTheme == 0) {// if not set from bundle...
            TypedArray a = null;
            try { // ...then try to find a styles attribute in the activity theme
                a = activity.obtainStyledAttributes(R.styleable.BaseCollabViewer);
                mReplyTheme = a.getResourceId(R.styleable.BaseCollabViewer_replyTheme,
                        R.style.ReplyBaseTheme_DayNight); // ...otherwise, use default theme
            } catch (Resources.NotFoundException e) {
                mReplyTheme = R.style.ReplyBaseTheme_DayNight;
            } finally {
                if (a != null) {
                    a.recycle();
                }
            }
        }
    }

    @LayoutRes
    protected int getContentLayoutRes() {
        return R.layout.controls_fragment_tabbed_pdfviewctrl_tab_content;
    }

    /**
     * Called when the reply fragment needs to be shown for a specific
     * annotation. Only called if the Fragment's lifecycle state is at least
     * {@link Lifecycle.State#STARTED}.
     *
     * @param selectedAnnotId      id of the selected annotation
     * @param authorId             author of the selected annotation
     * @param selectedAnnotPageNum page number of the selected annotation
     */
    @SuppressWarnings("unused")
    protected void showReplyFragment(@NonNull String selectedAnnotId,
            @NonNull String authorId, int selectedAnnotPageNum) {

        mImpl.showReplyFragment(this,
                selectedAnnotId,
                authorId, selectedAnnotPageNum,
                mDocumentId, mTabTag, mReplyTheme,
                isNavigationListShowing(),
                getDisableReplyEdit(),
                getDisableCommentEdit()
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mDisposables.clear();
    }

    @Override
    protected void loadPDFViewCtrlView() {
        super.loadPDFViewCtrlView();
        ToolConfig.getInstance().removeQMHideItem(R.id.qm_note);
        mImpl.init(mToolManager);
    }

    @Override
    public void onDocumentLoaded() {
        mImpl.doDocumentLoaded();
        initializeCollaboration();
        super.onDocumentLoaded();
    }

    @Override
    protected boolean handleSpecialFile(boolean close) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        boolean handled = false;
        mAnnotationSelected = false;
        int x = (int) (e.getX() + 0.5);
        int y = (int) (e.getY() + 0.5);
        // don't show sticky note popup
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            Annot annot = mPdfViewCtrl.getAnnotationAt(x, y);
            int page = mPdfViewCtrl.getPageNumberFromScreenPt(x, y);
            if (annot != null && annot.isValid()) {
                mAnnotationSelected = true;
                if (annot.getType() == Annot.e_Text && !AnnotUtils.hasReplyTypeReply(annot)) {
                    // this is a sticky note
                    handled = true;
                    mToolManager.selectAnnot(annot, page);
                }
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
        if (!handled) {
            handled = super.onSingleTapConfirmed(e);
        }
        if (!mAnnotationSelected) {
            mToolManager.setQuickMenuJustClosed(false);
        }
        return handled;
    }

    @Override
    public boolean onQuickMenuClicked(QuickMenuItem menuItem) {
        boolean result = super.onQuickMenuClicked(menuItem);
        if (menuItem.getItemId() == R.id.qm_note) {
            if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                showReplyComponent();
                return true;
            }
        }
        return result;
    }

    @Override
    public boolean onShowQuickMenu(QuickMenu quickMenu, Annot annot) {
        mImpl.doShowQuickMenu(getContext(), quickMenu, annot);

        return super.onShowQuickMenu(quickMenu, annot);
    }

    @Override
    public boolean isTabReadOnly() {
        return false;
    }

    @Override
    public void handleOnlineShare() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        Uri toBeCopied = null;

        switch (mTabSource) {
            case BaseFileInfo.FILE_TYPE_FILE:
            case BaseFileInfo.FILE_TYPE_EDIT_URI:
                toBeCopied = Uri.fromFile(mCurrentFile);
                break;
            case BaseFileInfo.FILE_TYPE_OPEN_URL:
                // check if is non-PDF
                if (mTabConversionTempPath != null && (new File(mTabConversionTempPath)).exists()) {
                    toBeCopied = Uri.fromFile(new File(mTabConversionTempPath));
                } else if (mCurrentFile != null && mCurrentFile.isFile() && mCurrentFile.exists()) {
                    toBeCopied = Uri.fromFile(mCurrentFile);
                }
                break;
            case BaseFileInfo.FILE_TYPE_EXTERNAL:
                toBeCopied = mCurrentUriFile;
                break;
            case BaseFileInfo.FILE_TYPE_OFFICE_URI:
                if (mTabConversionTempPath != null) {
                    File file = new File(mTabConversionTempPath);
                    if (file.isFile() && file.exists()) {
                        toBeCopied = Uri.fromFile(file);
                    }
                }
                break;
        }

        if (toBeCopied != null) {
            mDisposables.add(
                    Utils.duplicateInFolder(
                            Utils.getContentResolver(activity),
                            toBeCopied,
                            getTabTitleWithExtension(),
                            activity.getCacheDir())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(file -> {
                                if (mToolManager != null && mToolManager.getAnnotManager() != null) {
                                    mToolManager.getAnnotManager().exportToFile(file);
                                    Utils.sharePdfFile(activity, file);
                                }
                            }, throwable -> {

                            })
            );
        }
    }

    /**
     * Set listener for opening annotation list. Should be implemented by
     * {@link CollabViewerTabHostFragment}
     *
     * @param listener with callbacks for opening the annotation list
     */
    public void setOpenAnnotationListListener(OpenAnnotationListListener listener) {
        mOpenAnnotListListener = listener;
    }

    /**
     * Set listener for collaboration related tab events.
     */
    void setCollabTabListener(CollabTabListener listener) {
        mCollabTabListener = listener;
    }

    private void setUpReply(@NonNull FragmentActivity activity) {

        mReplyUiViewModel = ViewModelProviders.of(activity).get(ReplyUIViewModel.class);

        // Observe header UI events
        mDisposables.add(
                mReplyUiViewModel.getHeaderObservable()
                        .subscribe(headerEvent -> {
                            switch (headerEvent.getEventType()) {
                                case CLOSE_CLICKED: {
                                    mImpl.closeReplyFragment(this, mTabTag);
                                    break;
                                }
                                case LIST_CLICKED: {
                                    if (mOpenAnnotListListener != null) {
                                        mOpenAnnotListListener.openAnnotationList();
                                    }
                                    break;
                                }
                                case REVIEW_STATE_NONE_CLICKED: {
                                    mImpl.sendReviewStateReply(AnnotReviewState.NONE);
                                    break;
                                }
                                case REVIEW_STATE_ACCEPTED_CLICKED: {
                                    mImpl.sendReviewStateReply(AnnotReviewState.ACCEPTED);
                                    break;
                                }
                                case REVIEW_STATE_REJECTED_CLICKED: {
                                    mImpl.sendReviewStateReply(AnnotReviewState.REJECTED);
                                    break;
                                }
                                case REVIEW_STATE_CANCELLED_CLICKED: {
                                    mImpl.sendReviewStateReply(AnnotReviewState.CANCELLED);
                                    break;
                                }
                                case REVIEW_STATE_COMPLETED_CLICKED: {
                                    mImpl.sendReviewStateReply(AnnotReviewState.COMPLETED);
                                    break;
                                }
                            }
                        }, throwable -> AnalyticsHandlerAdapter.getInstance().sendException(new RuntimeException(throwable)))
        );

        // Observe message list UI events
        mDisposables.add(
                mReplyUiViewModel.getMessagesObservable()
                        .subscribe(messageEvent -> {
                            switch (messageEvent.getEventType()) {
                                case MESSAGE_DELETE_CLICKED: {
                                    ReplyMessage data = messageEvent.getData();
                                    String replyId = data.getReplyId();
                                    int page = data.getPage();
                                    mImpl.removeReply(replyId, page);
                                    break;
                                }
                            }
                        }, throwable -> AnalyticsHandlerAdapter.getInstance().sendException(new RuntimeException(throwable)))
        );

        // Observe send message events
        mDisposables.add(
                mReplyUiViewModel.getWriteMessageObservable()
                        .subscribe(inputEvent -> {
                            switch (inputEvent.getEventType()) {
                                case MESSAGE_WRITE_FINISHED: {
                                    ReplyInput replyInput = mReplyUiViewModel.getWriteMessageLiveData().getValue();
                                    if (replyInput != null) {
                                        mImpl.sendReply(replyInput.getMessage().getContent().getContentString());
                                    }
                                    break;
                                }
                                case MESSAGE_EDIT_FINISHED: {
                                    ReplyInput replyInput = mReplyUiViewModel.getEditMessageLiveData().getValue();
                                    if (replyInput != null) {
                                        mImpl.editReply(replyInput);
                                    }
                                    break;
                                }
                            }
                        }, throwable -> AnalyticsHandlerAdapter.getInstance().sendException(new RuntimeException(throwable)))
        );
    }

    private void setUpAnnotList(@NonNull FragmentActivity activity) {
        // Handle annotation list click events by jumping to the selected annotation
        // and toggling the reply bottom sheet
        AnnotationListViewModel annotListViewModel = ViewModelProviders.of(activity).get(AnnotationListViewModel.class);
        mDisposables.add(
                annotListViewModel.getAnnotationListObservable()
                        .subscribe(annotationListEvent -> {
                            switch (annotationListEvent.getEventType()) {
                                case ANNOTATION_ITEM_CLICKED: {
                                    AnnotationListContent annotContent = annotationListEvent.getData();
                                    Annot annot = ViewerUtils.getAnnotById(mPdfViewCtrl, annotContent.getId(), annotContent.getPageNum());
                                    int pageNum = annotContent.getPageNum();
                                    ToolManager toolManager = getToolManager();

                                    if (annot != null && mPdfViewCtrl != null && toolManager != null) {
                                        // Might be selected, when annot list button is clicked from reply fragment,
                                        // so deselect all first
                                        toolManager.deselectAll();
                                        // Show annotation jump animation
                                        ViewerUtils.jumpToAnnotation(mPdfViewCtrl, annot, pageNum);

                                        // Then show the bottom sheet for that annotation
                                        toolManager.selectAnnot(annot, pageNum);
                                        ((Tool) toolManager.getTool()).closeQuickMenu();
                                        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                                            showReplyComponent();
                                        }
                                    }
                                    break;
                                }
                            }
                        }, throwable -> AnalyticsHandlerAdapter.getInstance().sendException(new RuntimeException(throwable)))
        );
    }

    private void showReplyComponent() {
        // Get the parameters to initialize the bottom sheet state
        FragmentActivity activity = getActivity();
        int selectedAnnotPageNum = getToolManager().getSelectedAnnotPageNum();
        String selectedAnnotId = getToolManager().getSelectedAnnotId();
        Annot selectedAnnot = ViewerUtils.getAnnotById(mPdfViewCtrl,
                selectedAnnotId,
                selectedAnnotPageNum
        );
        if (activity != null) {
            if (selectedAnnot != null && mReplyUiViewModel != null) {
                // Set up the view model for UI
                boolean hasAnnotList = mViewerConfig == null || mViewerConfig.isShowAnnotationsList();
                boolean hasReviewState = mViewerConfig == null || mViewerConfig.isShowAnnotationReplyReviewState();
                String authorId = getToolManager().getAuthorId();
                String authorName = getToolManager().getAuthorName();
                ReplyHeader initialHeader = new ReplyHeader(activity, selectedAnnot, false, hasAnnotList, hasReviewState, getDisableCommentEdit());
                initialHeader.setId(selectedAnnotId);
                initialHeader.setPageNumber(selectedAnnotPageNum);
                ReplyMessages initialReplyMessages = new ReplyMessages();
                User user = new User(authorId, authorName);
                mReplyUiViewModel.set(initialHeader, initialReplyMessages, user, selectedAnnotPageNum);

                if (mDocumentId == null) {
                    Logger.INSTANCE.LogD(TAG, "Document is not ready for collab.");
                    return;
                }

                showReplyFragment(selectedAnnotId, authorId, selectedAnnotPageNum);
            } else {
                CommonToast.showText(activity, getString(R.string.toast_no_selected_annot), Toast.LENGTH_SHORT);
                Logger.INSTANCE.LogD(TAG, "Could not get selected annotation");
            }
        }
    }

    @Nullable
    String getDocumentId() {
        return mDocumentId;
    }

    /**
     * Called when the document is downloaded and need to initialize for collaboration.
     */
    protected void initializeCollaboration() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (mDocumentViewModel != null) {
            return;
        }
        LifecycleOwner lifecycleOwner = getViewLifecycleOwner();

        // Setup view models
        mDocumentViewModel = ViewModelProviders.of(this).get(DocumentViewModel.class);
        mDocumentViewModel.getUser().observe(
                lifecycleOwner,
                userEntity -> {
                    if (mCollabTabListener != null && mCollabTabListener.getCollabManager() != null &&
                            mCollabTabListener.getCollabManager().isStarted() && userEntity != null) {
                        mImpl.enableCollab(userEntity.getId(), userEntity.getName(),
                                (action, xfdfCommand, xfdfJSON) -> {
                                    if (mAnnotationViewModel != null) {
                                        mDisposables.add(
                                                Completable.fromAction(() ->
                                                        mAnnotationViewModel.sendAnnotation(action, xfdfCommand, xfdfJSON, userEntity.getName())
                                                ).subscribeOn(Schedulers.io()).subscribe());
                                    }
                                });
                    }
                }
        );

        mDocumentViewModel.getDocument().observe(
                lifecycleOwner,
                documentEntity -> {
                    if (documentEntity != null) {
                        if (!documentEntity.getId().equals(mDocumentId)) {
                            mDocumentId = documentEntity.getId();
                            mAnnotationViewModel =
                                    ViewModelProviders.of(CollabViewerTabFragment.this,
                                            new AnnotationViewModel.Factory(
                                                    getActivity().getApplication(),
                                                    documentEntity.getId()
                                            )
                                    ).get(AnnotationViewModel.class);
                            mDisposables.add(mImpl.addLocalAnnotationsAsync(
                                    this, mDocumentId, mTabTag
                            ).subscribeOn(Schedulers.io()).subscribe());
                            setupAnnotation(lifecycleOwner);
                        }
                    }
                }
        );

        mDisposables.add(
                mDocumentViewModel.getUnreadObservable(lifecycleOwner)
                        .subscribe(this::updateUnread,
                                throwable ->
                                        AnalyticsHandlerAdapter.getInstance()
                                                .sendException(new RuntimeException(throwable))
                        )
        );
    }

    private void updateUnread(boolean hasUnread) {
        // TODO bfung
    }

    private void setupAnnotation(LifecycleOwner lifecycleOwner) {
        Objects.requireNonNull(mAnnotationViewModel);
        mAnnotationViewModel.getLastAnnotations().observe(lifecycleOwner, lastAnnotationEntities -> {
            if (lastAnnotationEntities != null && !lastAnnotationEntities.isEmpty()) {
                Logger.INSTANCE.LogD(TAG, "lastAnnots: " + lastAnnotationEntities.size());
                ArrayList<String> ids = new ArrayList<>();
                for (LastAnnotationEntity lastAnnotationEntity : lastAnnotationEntities) {
                    ids.add(lastAnnotationEntity.getId());
                    String lastXfdf = CustomServiceUtils.getXfdfFromFile(lastAnnotationEntity.getXfdf());
                    mImpl.safeOnRemoteChange(this, mTabTag, lastXfdf, mInitialAnnotsMerged);
                }
                mDisposables.add(mAnnotationViewModel.consumeLastAnnotations(ids)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(() -> {
                                    // success no op?
                                }, throwable ->
                                        AnalyticsHandlerAdapter.getInstance()
                                                .sendException(new RuntimeException(throwable))
                        )
                );
                if (!mInitialAnnotsMerged) {
                    try {
                        mPdfViewCtrl.update(true);
                    } catch (Exception ignored) {
                    }
                }
                mInitialAnnotsMerged = true;
                if (mCollabTabListener != null && mCollabTabListener.getCollabManager() != null &&
                        mCollabTabListener.getCollabManager().getAnnotationCompletionListener() != null) {
                    mCollabTabListener.getCollabManager().getAnnotationCompletionListener().onRemoteChangeImported();
                }
            }
        });
    }

    // Disable Reply Edit option
    protected boolean getDisableReplyEdit() {
        return false;
    }

    // Disable Reply Header Edit option
    protected boolean getDisableCommentEdit() {
        return false;
    }
}
