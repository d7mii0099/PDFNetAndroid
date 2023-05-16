package com.pdftron.collab.ui.reply.component;

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.pdftron.collab.R;
import com.pdftron.collab.ui.base.component.BaseUIComponent;
import com.pdftron.collab.ui.reply.bottomsheet.view.ReplyInputUIView;
import com.pdftron.collab.ui.reply.component.header.HeaderEvent;
import com.pdftron.collab.ui.reply.component.input.BaseTextInputUIView;
import com.pdftron.collab.ui.reply.component.input.InputEvent;
import com.pdftron.collab.ui.reply.model.ReplyHeader;
import com.pdftron.collab.ui.reply.model.ReplyInput;
import com.pdftron.collab.ui.reply.model.ReplyMessage;
import com.pdftron.collab.ui.reply.model.ReplyMessageContent;
import com.pdftron.collab.ui.reply.model.User;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;

import java.util.Date;

import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;

/**
 * A {@link BaseUIComponent} representing the reply text input field. Responsible for updating
 * changes from {@link ReplyUIViewModel} to the {@link BaseTextInputUIView}, likely from local changes
 * (for example, the text input is cleared when the message is sent).
 */
public class ReplyInputUIComponent extends BaseUIComponent<BaseTextInputUIView, InputEvent, ReplyUIViewModel> {

    public ReplyInputUIComponent(@NonNull ViewGroup parent,
            @NonNull LifecycleOwner lifecycleOwner,
            @NonNull ReplyUIViewModel model,
            @NonNull PublishSubject<InputEvent> observable) {
        super(parent, lifecycleOwner, model, observable);
        setupComponent(lifecycleOwner, model);
    }

    @NonNull
    @Override
    protected BaseTextInputUIView inflateUIView(@NonNull ViewGroup parent) {
        return new ReplyInputUIView(parent);
    }

    @SuppressWarnings("unused")
    private void setupComponent(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull ReplyUIViewModel model) {

        // Subscribe to UI events from this component so we can update the view model and the view
        mDisposables.add(
                mSubject.serialize().subscribe(inputEvent -> {
                    switch (inputEvent.getEventType()) {
                        case MESSAGE_WRITE_TEXT_CHANGED: {
                            User user = model.getUser();
                            if (user != null) {
                                // Update the view model
                                model.updateWriteMessageInput(
                                        new ReplyMessage(
                                                null,
                                                user,
                                                new ReplyMessageContent(inputEvent.getData()),
                                                new Date(),
                                                ReplyEntityMapper.getInitials(user.getUserName()),
                                                model.getPage(),
                                                true
                                        )
                                );
                            }
                            break;
                        }
                        case MESSAGE_EDIT_TEXT_CHANGED: {
                            User user = model.getUser();
                            ReplyInput value = model.getEditMessageLiveData().getValue();
                            if (user != null && value != null) {
                                // Update the view model
                                model.updateEditMessageInput(
                                        new ReplyMessage(
                                                value.getMessage().getReplyId(),
                                                user,
                                                new ReplyMessageContent(inputEvent.getData()),
                                                new Date(),
                                                ReplyEntityMapper.getInitials(user.getUserName()),
                                                model.getPage(),
                                                true
                                        )
                                );
                            }
                            break;
                        }
                        case COMMENT_EDIT_TEXT_CHANGED: {
                            User user = model.getUser();
                            ReplyInput value = model.getEditCommentLiveData().getValue();
                            if (user != null && value != null) {
                                // Update the view model
                                model.updateEditCommentInput(
                                        new ReplyMessage(
                                                value.getMessage().getReplyId(),
                                                user,
                                                new ReplyMessageContent(inputEvent.getData()),
                                                new Date(),
                                                ReplyEntityMapper.getInitials(user.getUserName()),
                                                model.getPage(),
                                                true
                                        )
                                );
                            }
                            break;
                        }
                        case MESSAGE_WRITE_FINISHED: {
                            if (!Utils.isNullOrEmpty(inputEvent.getData())) {
                                User user = model.getUser();
                                if (user != null) {
                                    // Reset the view model
                                    model.updateWriteMessageInput(
                                            new ReplyMessage(
                                                    null,
                                                    user,
                                                    new ReplyMessageContent(""),
                                                    new Date(),
                                                    ReplyEntityMapper.getInitials(user.getUserName()),
                                                    model.getPage(),
                                                    true
                                            )
                                    );
                                    mView.clearInput();
                                    break;
                                }
                            }
                            break;
                        }
                        case MESSAGE_EDIT_FINISHED: {
                            User user = model.getUser();
                            if (user != null) {
                                // Update the view model
                                model.updateEditMessageInput(
                                        new ReplyMessage(
                                                null,
                                                user,
                                                new ReplyMessageContent(""),
                                                new Date(),
                                                ReplyEntityMapper.getInitials(user.getUserName()),
                                                model.getPage(),
                                                true
                                        )
                                );
                                mView.closeMessageEditView();
                            }
                            break;
                        }
                        case COMMENT_EDIT_FINISHED: {
                            User user = model.getUser();
                            if (user != null) {
                                // Update the view model
                                model.updateEditCommentInput(
                                        new ReplyMessage(
                                                null,
                                                user,
                                                new ReplyMessageContent(""),
                                                new Date(),
                                                ReplyEntityMapper.getInitials(user.getUserName()),
                                                model.getPage(),
                                                true
                                        )
                                );
                                mView.closeCommentEditView();
                            }
                            break;
                        }
                        case MESSAGE_EDIT_CANCELED: {
                            mView.closeMessageEditView();
                            break;
                        }
                        case COMMENT_EDIT_CANCELED: {
                            mView.closeCommentEditView();
                            break;
                        }
                    }
                })
        );

        // Subscribe to UI events from ReplyMessagesView component
        mDisposables.add(
                model.getMessagesObservable().serialize()
                        .subscribe(messageEvent -> {
                            switch (messageEvent.getEventType()) {
                                case MESSAGE_EDIT_CLICKED: // we inflate the edit message view
                                    model.updateEditMessageInput(messageEvent.getData());
                                    mView.showMessageEditView(messageEvent.getData().getContent().getContentString());
                                    break;
                            }
                        }, throwable -> AnalyticsHandlerAdapter.getInstance().sendException(new RuntimeException(throwable)))
        );

        // Subscribe to UI events from ReplyHeaderView component
        mDisposables.add(
                model.getHeaderObservable().serialize()
                        .subscribe(new Consumer<HeaderEvent>() {
                            @Override
                            public void accept(HeaderEvent headerEvent) throws Exception {
                                switch (headerEvent.getEventType()) {
                                    case ANNOT_COMMENT_MODIFY:
                                        ReplyHeader value = model.getHeaderLiveData().getValue();
                                        User user = model.getUser();
                                        if (value != null && user != null) {
                                            model.updateEditCommentInput(
                                                    new ReplyMessage(
                                                            value.getId(),
                                                            user,
                                                            new ReplyMessageContent(value.getPreviewContent()),
                                                            new Date(),
                                                            ReplyEntityMapper.getInitials(user.getUserName()),
                                                            model.getPage(),
                                                            true
                                                    )
                                            );
                                            mView.showEditCommentView(value.getPreviewContent());
                                        }
                                }
                            }
                        })
        );

        model.getHeaderLiveData().observe(lifecycleOwner,
                new Observer<ReplyHeader>() {
                    @Override
                    public void onChanged(ReplyHeader replyHeader) {
                        if (replyHeader != null) {
                            String comment = replyHeader.getPreviewContent();
                            if (Utils.isNullOrEmpty(comment) && replyHeader.isCommentEditable()) {
                                mView.setReplyEditTextHint(R.string.reply_editor_add_comment);
                            } else {
                                mView.setReplyEditTextHint(R.string.reply_editor_write_hint);
                            }
                        }
                    }
                });
    }
}
