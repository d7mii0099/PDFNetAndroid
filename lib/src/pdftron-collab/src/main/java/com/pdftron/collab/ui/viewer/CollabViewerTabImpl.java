package com.pdftron.collab.ui.viewer;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.pdftron.collab.R;
import com.pdftron.collab.db.CollabDatabase;
import com.pdftron.collab.db.entity.AnnotationEntity;
import com.pdftron.collab.service.CustomServiceUtils;
import com.pdftron.collab.ui.reply.bottomsheet.BottomSheetReplyFragment;
import com.pdftron.collab.ui.reply.bottomsheet.BottomSheetReplyFragmentBuilder;
import com.pdftron.collab.ui.reply.bottomsheet.ReplyFragment;
import com.pdftron.collab.ui.reply.bottomsheet.ReplyFragmentBuilder;
import com.pdftron.collab.ui.reply.model.ReplyInput;
import com.pdftron.collab.ui.reply.model.ReplyMessage;
import com.pdftron.collab.utils.XfdfUtils;
import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageIterator;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.annots.Popup;
import com.pdftron.pdf.controls.AnnotStyleDialogFragment;
import com.pdftron.pdf.model.AnnotReviewState;
import com.pdftron.pdf.tools.AnnotManager;
import com.pdftron.pdf.tools.QuickMenu;
import com.pdftron.pdf.tools.QuickMenuItem;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.Logger;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

import io.reactivex.Completable;

class CollabViewerTabImpl {

    private static final String TAG = CollabViewerTabImpl.class.getName();

    private ToolManager mToolManager;
    @NonNull
    private PDFViewCtrl.AnnotationManagerMode mAnnotationManagerUndoMode = PDFViewCtrl.AnnotationManagerMode.ADMIN_UNDO_OWN;
    @NonNull
    private AnnotManager.EditPermissionMode mAnnotationManagerEditMode = AnnotManager.EditPermissionMode.EDIT_OWN;

    final ArrayList<String> mLastXfdfIdsToBeConsumed = new ArrayList<>();

    private boolean mAlwaysShowAsReply = false;

    public CollabViewerTabImpl() {

    }

    public void init(@NonNull ToolManager toolManager) {
        mToolManager = toolManager;
        mToolManager.setSkipReadOnlyCheck(true);
    }

    public void setAnnotationManagerUndoMode(@NonNull PDFViewCtrl.AnnotationManagerMode mode) {
        mAnnotationManagerUndoMode = mode;
    }

    public void setAnnotationManagerEditMode(@NonNull AnnotManager.EditPermissionMode mode) {
        mAnnotationManagerEditMode = mode;
    }

    public void setAlwaysShowAsReply(boolean alwaysShowAsReply) {
        mAlwaysShowAsReply = alwaysShowAsReply;
    }

    public PDFViewCtrl getPDFViewCtrl() {
        return mToolManager.getPDFViewCtrl();
    }

    public void doDocumentLoaded() {
        mToolManager.setReadOnly(false);
        mToolManager.disableToolMode(new ToolManager.ToolMode[]{
                ToolManager.ToolMode.SOUND_CREATE,
                ToolManager.ToolMode.RECT_LINK,
                ToolManager.ToolMode.TEXT_LINK_CREATE,
                ToolManager.ToolMode.ANNOT_EDIT_RECT_GROUP
        });
        mToolManager.setCopyAnnotatedTextToNoteEnabled(true);
        mToolManager.setStickyNoteShowPopup(false);
    }

    public void doShowQuickMenu(Context context, QuickMenu quickMenu, Annot annot) {
        if (annot != null && quickMenu != null && context != null) {

            // Remove copy and duplicate button in quick menu
            ArrayList<QuickMenuItem> toRemove = new ArrayList<>();
            QuickMenuItem copyQmItem = new QuickMenuItem(context, R.id.qm_copy, QuickMenuItem.FIRST_ROW_MENU);
            QuickMenuItem duplicateQmItem = new QuickMenuItem(context, R.id.qm_duplicate, QuickMenuItem.OVERFLOW_ROW_MENU);
            toRemove.add(copyQmItem);
            toRemove.add(duplicateQmItem);
            quickMenu.removeMenuEntries(toRemove);

            try {
                // Add note button for free text
                if (annot.getType() == Annot.e_FreeText) {
                    QuickMenuItem noteQmItem = new QuickMenuItem(context, R.id.qm_note, QuickMenuItem.FIRST_ROW_MENU);
                    noteQmItem.setTitle(R.string.tools_qm_note);
                    noteQmItem.setIcon(R.drawable.ic_annotation_sticky_note_black_24dp);
                    noteQmItem.setOrder(QuickMenuItem.ORDER_START);
                    quickMenu.addMenuEntries(Collections.singletonList(noteQmItem));
                }
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
    }

    public void showReplyFragment(@NonNull Fragment collabFragment,
            @NonNull String selectedAnnotId,
            @NonNull String authorId, int selectedAnnotPageNum,
            String documentId, String tabTag, int replyTheme,
            boolean isNavigationListShowing) {
        showReplyFragment(collabFragment, selectedAnnotId, authorId, selectedAnnotPageNum, documentId, tabTag, replyTheme, isNavigationListShowing, false, false);
    }

    public void showReplyFragment(@NonNull Fragment collabFragment,
            @NonNull String selectedAnnotId,
            @NonNull String authorId, int selectedAnnotPageNum,
            String documentId, String tabTag, int replyTheme,
            boolean isNavigationListShowing,
            boolean disableReplyEdit,
            boolean disableCommentEdit) {
        FragmentActivity activity = collabFragment.getActivity();
        if (activity != null) {
            // Show bottom sheet or side sheet
            ReplyFragmentBuilder builder = ReplyFragmentBuilder
                    .withAnnot(documentId, selectedAnnotId, authorId)
                    .setDisableReplyEdit(disableReplyEdit)
                    .setDisableCommentEdit(disableCommentEdit)
                    .usingTheme(replyTheme);
            Fragment fragment;
            if (isNavigationListShowing) {
                fragment = builder.build(activity, ReplyFragment.class);
            } else {
                BottomSheetReplyFragmentBuilder bottomSheetBuilder = builder
                        .setDisableReplyEdit(disableReplyEdit)
                        .setDisableCommentEdit(disableCommentEdit)
                        .asBottomSheet();
                fragment = bottomSheetBuilder.build(activity, BottomSheetReplyFragment.class);
            }

            // Reselect the annotation when bottom sheet is dismissed
            fragment.getViewLifecycleOwnerLiveData()
                    .observe(collabFragment, new Observer<LifecycleOwner>() {
                        @Override
                        public void onChanged(@Nullable LifecycleOwner lifecycleOwner) {
                            if (lifecycleOwner == null) { // null when bottom sheet is destroyed (i.e. after onDestroyView)
                                mToolManager.reselectAnnot();
                                fragment.getViewLifecycleOwnerLiveData().removeObserver(this); // just to be safe, manually remove this
                            }
                        }
                    });

            if (fragment instanceof BottomSheetReplyFragment) {
                ((BottomSheetReplyFragment) fragment).show(activity.getSupportFragmentManager(), BottomSheetReplyFragment.TAG);
            } else {
                FragmentTransaction ft = collabFragment.getChildFragmentManager().beginTransaction();
                ft.setCustomAnimations(R.anim.slide_in_right, android.R.anim.slide_out_right);
                ft.add(R.id.navigation_list, fragment, ReplyFragment.TAG + tabTag);
                ft.commitAllowingStateLoss();
            }
        }
    }

    public void closeReplyFragment(@NonNull Fragment collabFragment, @NonNull String tabTag) {
        Fragment fragment = collabFragment.getChildFragmentManager().findFragmentByTag(ReplyFragment.TAG + tabTag);
        // try tablet panel
        if (fragment instanceof ReplyFragment) {
            FragmentTransaction ft = collabFragment.getChildFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.slide_in_right, android.R.anim.slide_out_right);
            ft.remove(fragment);
            ft.commitAllowingStateLoss();
            return;
        }
        FragmentActivity activity = collabFragment.getActivity();
        // try bottom sheet
        if (activity != null) {
            fragment = activity.getSupportFragmentManager().findFragmentByTag(BottomSheetReplyFragment.TAG);
            if (fragment instanceof BottomSheetReplyFragment) {
                ((BottomSheetReplyFragment) fragment).dismiss();
            }
        }
    }

    public void sendReviewStateReply(AnnotReviewState state) {
        if (null == state) {
            return;
        }
        try {
            Annot reply = AnnotUtils.createAnnotationStateReply(
                    mToolManager.getSelectedAnnotId(),
                    mToolManager.getSelectedAnnotPageNum(),
                    getPDFViewCtrl(),
                    mToolManager.getAuthorId(),
                    mToolManager.getAuthorName(),
                    state
            );
            HashMap<Annot, Integer> annots = new HashMap<>(1);
            annots.put(reply, mToolManager.getSelectedAnnotPageNum());
            mToolManager.raiseAnnotationsAddedEvent(annots);
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
    }

    public void sendReply(String contents) {
        if (Utils.isNullOrEmpty(contents)) {
            return;
        }
        try {
            Annot parent = ViewerUtils.getAnnotById(getPDFViewCtrl(),
                    mToolManager.getSelectedAnnotId(),
                    mToolManager.getSelectedAnnotPageNum());
            if (parent != null && parent.isValid() && parent.isMarkup()) {
                String authorId = AnnotUtils.getAuthor(parent);
                if (!mAlwaysShowAsReply && (authorId != null && authorId.equals(mToolManager.getAuthorId()))) {
                    HashMap<Annot, Integer> annots = new HashMap<>(1);
                    annots.put(parent, mToolManager.getSelectedAnnotPageNum());
                    Markup markup = new Markup(parent);
                    Popup popup = markup.getPopup();
                    boolean preEventRaised = false;
                    if (popup == null || !popup.isValid()) {
                        preEventRaised = true;
                        mToolManager.raiseAnnotationsPreModifyEvent(annots);
                    }
                    Utils.handleEmptyPopup(getPDFViewCtrl().getDoc(), markup);
                    popup = markup.getPopup();
                    String popupContents = popup.getContents();
                    if (Utils.isNullOrEmpty(popupContents)) {
                        // parent does not contain content, we can add as content first if same author
                        if (!preEventRaised) {
                            preEventRaised = true;
                            mToolManager.raiseAnnotationsPreModifyEvent(annots);
                        }
                        popup.setContents(contents);
                        AnnotUtils.setDateToNow(getPDFViewCtrl(), parent);
                        mToolManager.raiseAnnotationsModifiedEvent(annots, Tool.getAnnotationModificationBundle(null));
                        return;
                    }
                }
            }
            Annot reply = AnnotUtils.createAnnotationReply(
                    mToolManager.getSelectedAnnotId(),
                    mToolManager.getSelectedAnnotPageNum(),
                    getPDFViewCtrl(),
                    mToolManager.getAuthorId(),
                    contents
            );
            HashMap<Annot, Integer> annots = new HashMap<>(1);
            annots.put(reply, mToolManager.getSelectedAnnotPageNum());
            mToolManager.raiseAnnotationsAddedEvent(annots);
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
    }

    public void editReply(@NonNull ReplyInput replyInput) {
        ReplyMessage message = replyInput.getMessage();
        String replyId = message.getReplyId();
        String newMessage = message.getContent().getContentString();
        int pageNum = message.getPage();
        try {
            AnnotUtils.updateAnnotationReply(replyId, pageNum, getPDFViewCtrl(),
                    mToolManager, newMessage);
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
    }

    public void removeReply(String replyId, int page) {
        try {
            AnnotUtils.deleteAnnotationReply(replyId, page, getPDFViewCtrl(), mToolManager);
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
    }

    public void editComment(ReplyInput replyInput) {
        ReplyMessage message = replyInput.getMessage();
        String replyId = message.getReplyId();
        String newComment = message.getContent().getContentString();
        int pageNum = message.getPage();

        Annot annot = ViewerUtils.getAnnotById(getPDFViewCtrl(), replyId, pageNum);
        String newContent = newComment == null ? "" : newComment;

        try {
            if (annot != null && annot.isValid() && annot.isMarkup()) {
                HashMap<Annot, Integer> annots = new HashMap<>(1);
                annots.put(annot, mToolManager.getSelectedAnnotPageNum());
                Markup markup = new Markup(annot);
                Popup popup = markup.getPopup();
                Utils.handleEmptyPopup(getPDFViewCtrl().getDoc(), markup);
                popup = markup.getPopup();
                String popupContents = popup.getContents();
                if (!newContent.equals(popupContents)) {
                    mToolManager.raiseAnnotationsPreModifyEvent(annots);
                    popup.setContents(newContent);
                    AnnotUtils.setDateToNow(getPDFViewCtrl(), annot);
                    mToolManager.raiseAnnotationsModifiedEvent(annots, Tool.getAnnotationModificationBundle(null));
                }
            }
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    public void addLocalAnnotations(CollabDatabase db, @NonNull String documentId, @NonNull String tabTag) {
        // if the document is not a "clean" copy, we need to first parse out all its annotations
        Objects.requireNonNull(documentId);
        String cachePath = tabTag;
        PDFDoc pdfDoc = null;
        try {
            // PDF file check
            File cacheFile = new File(cachePath);
            if (!cacheFile.exists() || Utils.isNotPdf(cachePath)) {
                return;
            }
            pdfDoc = new PDFDoc(cachePath);
            for (PageIterator itr = pdfDoc.getPageIterator(); itr.hasNext(); ) {
                Page page = itr.next();
                HashMap<String, AnnotationEntity> map = new HashMap<>();
                int num_annots = page.getNumAnnots();
                for (int i = 0; i < num_annots; ++i) {
                    Annot annot = page.getAnnot(i);
                    AnnotationEntity entity = XfdfUtils.toAnnotationEntity(pdfDoc, documentId, annot);
                    if (entity != null) {
                        entity.setAt("create");
                        if (XfdfUtils.isValidInsertEntity(entity)) {
                            map.put(entity.getId(), entity);
                        }
                    }
                }
                // update per page
                CustomServiceUtils.addAnnotations(db, map);
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            Utils.closeQuietly(pdfDoc);
        }
    }

    public Completable addLocalAnnotationsAsync(@NonNull Fragment collabFragment, @NonNull String documentId, @NonNull String tabTag) {
        FragmentActivity activity = collabFragment.getActivity();
        Objects.requireNonNull(activity);
        CollabDatabase db = CollabDatabase.getInstance(activity.getApplicationContext());
        return Completable.fromAction(() -> addLocalAnnotations(db, documentId, tabTag));
    }

    public void safeOnRemoteChange(Fragment collabFragment, String tabTag, String xfdfString, boolean initialAnnotsMerged) {
        ToolManager toolManager = mToolManager;
        AnnotManager annotManager = toolManager != null ? mToolManager.getAnnotManager() : null;
        if (annotManager != null) {
            String selectedId = toolManager.getSelectedAnnotId();
            boolean modifySelected = selectedId != null && xfdfString.contains(selectedId);
            if (xfdfString.contains("<delete>") && modifySelected && initialAnnotsMerged) {
                toolManager.deselectAll();
            }
            toolManager.getAnnotManager().onRemoteChange(xfdfString);
            Logger.INSTANCE.LogD(TAG, "done lastAnnot merge");
            if (xfdfString.contains("<modify>") && modifySelected && initialAnnotsMerged && !xfdfString.contains(toolManager.getAuthorId())) {
                closeReplyFragment(collabFragment, tabTag);
                boolean canShow = true;
                FragmentActivity activity = collabFragment.getActivity();
                if (activity != null) {
                    Fragment annotStyleDialog = activity.getSupportFragmentManager().findFragmentByTag(AnnotStyleDialogFragment.TAG);
                    canShow = annotStyleDialog == null;
                }
                if (canShow) {
                    // re-select is not needed for style changes, only position/size changes
                    toolManager.reselectAnnot();
                }
            }
        }
    }

    public void enableCollab(String userId, String userName, AnnotManager.AnnotationSyncingListener listener) {
        ToolManager toolManager = mToolManager;
        if (toolManager != null) {
            if (toolManager.getAnnotManager() != null) {
                return;
            }
            toolManager.enableAnnotManager(
                    userId,
                    userName,
                    mAnnotationManagerUndoMode,
                    mAnnotationManagerEditMode,
                    listener
            );
            toolManager.setExternalAnnotationManagerListener(() -> UUID.randomUUID().toString());
        }
    }
}
