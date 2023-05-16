package com.pdftron.collab.ui.reply.bottomsheet.view;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pdftron.collab.R;
import com.pdftron.collab.ui.reply.bottomsheet.BottomSheetReplyFragment;
import com.pdftron.collab.ui.reply.component.messages.BaseMessagesUIView;
import com.pdftron.collab.ui.reply.component.messages.MessageEvent;
import com.pdftron.collab.ui.reply.model.ReplyMessage;
import com.pdftron.collab.ui.reply.model.ReplyMessages;
import com.pdftron.collab.utils.date.BaseDateFormat;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.subjects.PublishSubject;
/**
 * Base class for {@link BaseMessagesUIView} that represents reply messages/comments in the
 * {@link BottomSheetReplyFragment}
 */
public class ReplyMessagesUIView extends BaseMessagesUIView {

    private final RecyclerView mMessageList;

    public ReplyMessagesUIView(@NonNull ViewGroup parent) {
        super(parent);
        Context context = parent.getContext();
        View container = LayoutInflater.from(context)
                .inflate(R.layout.content_reply_messages, parent, true);

        // Setup recycler view
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setStackFromEnd(true);
        mMessageList = container.findViewById(R.id.message_list);
        mMessageList.setLayoutManager(layoutManager);
    }

    @Override
    public void setMessages(@NonNull ReplyMessages replyMessages) {
        if (mBaseMessageAdapter != null) {
            mBaseMessageAdapter.setMessages(replyMessages.getMessages());
            if (mBaseMessageAdapter.getItemCount() != 0) {
                safeScrollTo(mBaseMessageAdapter.getItemCount() - 1);
            }
        }
    }

    @Override
    public void setMessagesAdapter(@NonNull BaseMessagesAdapter adapter) {
        super.setMessagesAdapter(adapter);
        mMessageList.setAdapter(adapter);
    }

    private void safeScrollTo(int position) {
        try {
            mMessageList.scrollToPosition(position);
        } catch (Exception e) {
            // no-op, fail silently
        }
    }

    /**
     * The default message adapter used in the messages recycler view.
     */
    public static class MessageAdapter extends BaseMessagesAdapter {
        @NonNull
        private List<ReplyMessage> mMessages = new ArrayList<>();
        @NonNull
        private final BaseDateFormat mReplyDateFormat;

        private final AvatarAdapter mAvatarAdapter;
        private boolean mDisableReplyEdit;

        public MessageAdapter(@NonNull BaseDateFormat replyDateFormat,
                @NonNull PublishSubject<MessageEvent> eventObservable,
                @NonNull AvatarAdapter avatarAdapter) {
            super(eventObservable);
            mReplyDateFormat = replyDateFormat;
            mAvatarAdapter = avatarAdapter;
        }

        public MessageAdapter(@NonNull BaseDateFormat replyDateFormat,
                @NonNull PublishSubject<MessageEvent> eventObservable,
                @NonNull AvatarAdapter avatarAdapter,
                boolean disableReplyEdit) {
            super(eventObservable);
            mReplyDateFormat = replyDateFormat;
            mAvatarAdapter = avatarAdapter;
            mDisableReplyEdit = disableReplyEdit;
        }

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_reply_message, viewGroup, false);
            MessageViewHolder vh = new MessageViewHolder(v);
            mAvatarAdapter.onInflateAvatar(vh.getAvatarContainer());
            return vh;
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder messageViewHolder, int position) {
            ReplyMessage msg = mMessages.get(position);
            String username = msg.getUser().getUserName();
            Date date = msg.getTimestamp();
            String comment = msg.getContent().getContentString();

            mAvatarAdapter.onBindAvatar(messageViewHolder.getAvatarContainer(), msg);
            messageViewHolder.getUsername().setText(username);
            messageViewHolder.getTimestamp().setText(mReplyDateFormat.getDateString(date));
            messageViewHolder.getMessage().setText(comment);

            messageViewHolder.getMoreButton().setVisibility(msg.isEditable() ? View.VISIBLE : View.INVISIBLE);
            messageViewHolder.getMoreButton().setOnClickListener(v -> onShowPopupMenu(messageViewHolder, v));
        }

        private void onShowPopupMenu(MessageViewHolder messageViewHolder, View anchor) {
            Context context = anchor.getContext();
            Utils.hideSoftKeyboard(context, anchor); // hide keyboard as the menu location will be incorrect
            Context wrapper = new ContextThemeWrapper(context, R.style.ReplyPopupTheme);
            PopupMenu popupMenu = new PopupMenu(wrapper, anchor);
            popupMenu.inflate(R.menu.popup_reply_message_more);

            if (mDisableReplyEdit) {
                popupMenu.getMenu().findItem(R.id.reply_message_edit).setVisible(false);
            }
            ReplyMessage replyMessage = mMessages.get(messageViewHolder.getAdapterPosition());

            popupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.reply_message_delete) {
                    onMessageDeleteClicked(replyMessage);
                } else if (id == R.id.reply_message_edit) {
                    onMessageEditClicked(replyMessage);
                } else {
                    return false;
                }
                return true;
            });

            popupMenu.show();
        }

        @Override
        public int getItemCount() {
            return mMessages.size();
        }

        @Override
        public void setMessages(List<ReplyMessage> messages) {
            mMessages = messages;
            notifyDataSetChanged(); // todo diffutils?
        }
    }
}
