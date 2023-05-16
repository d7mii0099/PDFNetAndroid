package com.pdftron.collab.ui.reply.component;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.pdftron.collab.db.entity.AnnotationEntity;
import com.pdftron.collab.ui.reply.component.header.HeaderEvent;
import com.pdftron.collab.ui.reply.component.input.InputEvent;
import com.pdftron.collab.ui.reply.component.messages.MessageEvent;
import com.pdftron.collab.ui.reply.model.ReplyHeader;
import com.pdftron.collab.ui.reply.model.ReplyInput;
import com.pdftron.collab.ui.reply.model.ReplyMessage;
import com.pdftron.collab.ui.reply.model.ReplyMessages;
import com.pdftron.collab.ui.reply.model.User;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.model.AnnotReviewState;

import java.util.List;

import io.reactivex.subjects.PublishSubject;

/**
 * {@link ViewModel} containing data required by the reply dialog, such as the list of messages, the header,
 * text input field, and the current user.
 * <p>
 * The {@link ReplyUIViewModel} object will be reused and reinitialized for every reply dialog pertaining to a
 * specific annotation in the document viewer.
 */
public class ReplyUIViewModel extends ViewModel {

    private final MutableLiveData<ReplyHeader> mHeaderLiveData = new MutableLiveData<>();
    private final MutableLiveData<ReplyMessages> mMessagesLiveData = new MutableLiveData<>();
    private final MutableLiveData<ReplyInput> mWriteMessageLiveData = new MutableLiveData<>();
    private final MutableLiveData<ReplyInput> mEditMessageLiveData = new MutableLiveData<>();
    private final MutableLiveData<ReplyInput> mEditCommentLiveData = new MutableLiveData<>();
    private int mPage;
    @Nullable
    private User mUser; // user never changes so it doesn't have to be a live data object

    @NonNull
    private final PublishSubject<HeaderEvent> mHeaderSubject = PublishSubject.create();
    @NonNull
    private final PublishSubject<MessageEvent> mMessagesSubject = PublishSubject.create();
    @NonNull
    private final PublishSubject<InputEvent> mWriteMessageSubject = PublishSubject.create();

    public ReplyUIViewModel() {
    }

    /**
     * Initialize the data in the {@link ReplyUIViewModel}. Data usually comes from the current
     * selected annotation.
     *
     * @param replyHeader   contains the annotation information to be displayed
     * @param replyMessages contains the reply message to be displayed
     * @param user         contains information about the current user (used to edit and write comments)
     * @param page         current page of the annotation that this view model describes
     */
    public void set(@NonNull ReplyHeader replyHeader,
            @NonNull ReplyMessages replyMessages,
            @NonNull User user,
            int page) {
        mHeaderLiveData.setValue(replyHeader);
        mMessagesLiveData.setValue(replyMessages);
        mUser = user;
        mPage = page;
    }

    /**
     * Returns a {@link LiveData< ReplyHeader >} that calls its observers when the header information
     * has changed. It is used internally to initialize the view state, but can be used externally
     * to listen to header state changes. Currently observers are only notified when
     * replies have changed (i.e. the notification icon) and preview changes are not supported.
     *
     * @return the {@link LiveData< ReplyHeader >}
     */
    @NonNull
    public LiveData<ReplyHeader> getHeaderLiveData() {
        return mHeaderLiveData;
    }

    /**
     * Returns a {@link LiveData< ReplyMessages >} that calls its observers when the list of messages/
     * comments have changed. It is used internally to initialize the view state, but can also
     * be used externally to listen to message list changes.
     *
     * @return the {@link LiveData< ReplyMessages >}
     */
    @NonNull
    public LiveData<ReplyMessages> getMessagesLiveData() {
        return mMessagesLiveData;
    }

    /**
     * Returns a {@link LiveData< ReplyInput >} that calls its observers when ever there is a change
     * in the new message edit text input field.
     *
     * @return the {@link LiveData< ReplyInput >}
     */
    @NonNull
    public LiveData<ReplyInput> getWriteMessageLiveData() {
        return mWriteMessageLiveData;
    }

    /**
     * Similar to {@link #getWriteMessageLiveData()}, however returns a {@link LiveData< ReplyInput >}
     * that calls its observers when ever there is a change in the new message edit text input field.
     *
     * @return the {@link LiveData< ReplyInput >}
     */
    @NonNull
    public LiveData<ReplyInput> getEditMessageLiveData() {
        return mEditMessageLiveData;
    }

    /**
     * Similar to {@link #getWriteMessageLiveData()}, however returns a {@link LiveData< ReplyInput >}
     * that calls its observers when ever there is a change in the comment edit text input field.
     *
     * @return the {@link LiveData< ReplyInput >}
     */
    @NonNull
    public LiveData<ReplyInput> getEditCommentLiveData() {
        return mEditCommentLiveData;
    }

    /**
     * Returns the current {@link User}.
     */
    @Nullable
    public User getUser() {
        return mUser;
    }

    /**
     * Returns the current page of the annotation that this {@link ReplyUIViewModel} represents.
     */
    public int getPage() {
        return mPage;
    }

    // Helper methods to update live data
    void updateWriteMessageInput(@NonNull ReplyMessage content) {
        mWriteMessageLiveData.setValue(new ReplyInput(content));
    }

    void updateEditMessageInput(@NonNull ReplyMessage content) {
        mEditMessageLiveData.setValue(new ReplyInput(content));
    }

    void updateEditCommentInput(@NonNull ReplyMessage content) {
        mEditCommentLiveData.setValue(new ReplyInput(content));
    }

    /**
     * Updates the LiveData containing the reply messages with a new list of comments/messages.
     *
     * @param newMessages used to update the view model.
     */
    public void setMessages(@NonNull List<ReplyMessage> newMessages) {
        ReplyMessages replyMessages = new ReplyMessages(newMessages);
        mMessagesLiveData.setValue(replyMessages);
    }

    /**
     * Updates the notification flag in the LiveData containing the reply header data.
     *
     * @param hasUnreadReplies whether there are any unread replies/comments
     */
    public void setHasUnreadReplies(boolean hasUnreadReplies) {
        ReplyHeader oldReplyHeader = mHeaderLiveData.getValue();
        if (oldReplyHeader != null) {
            ReplyHeader replyHeader = ReplyHeader.updateUnreadReplies(oldReplyHeader, hasUnreadReplies);
            mHeaderLiveData.setValue(replyHeader);
        }
    }

    /**
     * Updates the review state in the LiveData containing the reply header data.
     *
     * @param reviewState the review state of the reply
     */
    public void setReviewState(AnnotReviewState reviewState) {
        ReplyHeader oldReplyHeader = mHeaderLiveData.getValue();
        if (oldReplyHeader != null) {
            ReplyHeader replyHeader = ReplyHeader.updateReviewState(oldReplyHeader, reviewState);
            mHeaderLiveData.setValue(replyHeader);
        }
    }

    /**
     * Updates the parent annotation's content.
     *
     * @param content the content
     */
    public void setParentAnnotationContent(String content) {
        ReplyHeader oldReplyHeader = mHeaderLiveData.getValue();
        if (oldReplyHeader != null) {
            ReplyHeader replyHeader = ReplyHeader.updatePreviewContent(oldReplyHeader, content);
            mHeaderLiveData.setValue(replyHeader);
        }
    }

    /**
     * Updates the parent annotation associated with the reply header.
     *
     * @param parentAnnot containing the parent annotation information.
     */
    public void setParentAnnotation(@NonNull AnnotationEntity parentAnnot) {
        ReplyHeader oldReplyHeader = mHeaderLiveData.getValue();
        if (oldReplyHeader != null && mUser != null) {
            String contents = parentAnnot.getContents();
            ReplyHeader replyHeader = ReplyHeader.updatePreviewContent(oldReplyHeader, contents == null ? "" : contents);
            replyHeader.setId(parentAnnot.getId());
            replyHeader.setPageNumber(parentAnnot.getPage());
            int type = parentAnnot.getType();
            boolean isTextMarkup = type == Annot.e_Highlight || type == Annot.e_Underline || type == Annot.e_StrikeOut || type == Annot.e_Squiggly;
            replyHeader.setCommentEditable(!isTextMarkup && mUser.getId().equals(parentAnnot.getAuthorId()));
            mHeaderLiveData.setValue(replyHeader);
        }
    }

    @NonNull
    public PublishSubject<HeaderEvent> getHeaderObservable() {
        return mHeaderSubject;
    }

    @NonNull
    public PublishSubject<MessageEvent> getMessagesObservable() {
        return mMessagesSubject;
    }

    @NonNull
    public PublishSubject<InputEvent> getWriteMessageObservable() {
        return mWriteMessageSubject;
    }
}
