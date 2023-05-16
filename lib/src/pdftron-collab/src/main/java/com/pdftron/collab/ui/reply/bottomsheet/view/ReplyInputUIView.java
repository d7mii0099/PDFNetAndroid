package com.pdftron.collab.ui.reply.bottomsheet.view;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.pdftron.collab.R;
import com.pdftron.collab.ui.reply.bottomsheet.BottomSheetReplyFragment;
import com.pdftron.collab.ui.reply.component.input.BaseTextInputUIView;
import com.pdftron.pdf.utils.Utils;

/**
 * Base class for {@link BaseTextInputUIView} that represents a the text input field in the
 * {@link BottomSheetReplyFragment}
 */
public class ReplyInputUIView extends BaseTextInputUIView {

    private EditText mWriteMessageEditText;
    private EditText mEditMessageEditText;
    private EditText mEditCommentEditText;

    private final ViewGroup mParent;

    private final ConstraintLayout mWriteMessageComponent;
    private final ConstraintLayout mEditMessageComponent;
    private final ConstraintLayout mEditCommentComponent;

    public ReplyInputUIView(@NonNull ViewGroup parent) {
        super(parent);

        // Inflate views
        mParent = parent;
        Context context = mParent.getContext();
        mWriteMessageComponent = (ConstraintLayout) LayoutInflater.from(context)
                .inflate(R.layout.content_reply_message_write, mParent, false);
        mEditMessageComponent = (ConstraintLayout) LayoutInflater.from(context)
                .inflate(R.layout.content_reply_message_edit, mParent, false);
        mEditCommentComponent = (ConstraintLayout) LayoutInflater.from(context)
                .inflate(R.layout.content_reply_message_edit, mParent, false);

        // Setup views
        setupWriteMessage(mWriteMessageComponent);
        mParent.addView(mWriteMessageComponent); // we want to initially have the write message editor
        setupEditMessage(mEditMessageComponent);
        setupEditComment(mEditCommentComponent);
    }

    private void setupWriteMessage(@NonNull ConstraintLayout editorContainer) {

        mWriteMessageEditText = editorContainer.findViewById(R.id.reply_editor);
        mWriteMessageEditText.setHint(R.string.reply_editor_write_hint);
        Button sendButton = editorContainer.findViewById(R.id.reply_send);

        mWriteMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ReplyInputUIView.this.onMessageWriteChanged(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // no-op
            }
        });
        sendButton.setOnClickListener(v -> onMessageWriteFinished(mWriteMessageEditText.getText().toString()));
    }

    private void setupEditMessage(@NonNull ConstraintLayout editorContainer) {

        // Inflate views
        mEditMessageEditText = editorContainer.findViewById(R.id.reply_editor);
        mEditMessageEditText.setHint(R.string.reply_editor_edit_reply);
        Button enterButton = editorContainer.findViewById(R.id.reply_send);
        Button closeButton = editorContainer.findViewById(R.id.editor_close);
        TextView editHeaderText = editorContainer.findViewById(R.id.header_text);
        editHeaderText.setText(R.string.reply_editor_edit_reply);

        mEditMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ReplyInputUIView.this.onMessageEditChanged(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // no-op
            }
        });

        // Handle send and close buttons
        enterButton.setOnClickListener(v -> onMessageEditFinished(mEditMessageEditText.getText().toString()));
        closeButton.setOnClickListener(v -> onMessagedEditCancelled());
    }

    private void setupEditComment(@NonNull ConstraintLayout editorContainer) {

        // Inflate views
        mEditCommentEditText = editorContainer.findViewById(R.id.reply_editor);
        mEditCommentEditText.setHint(R.string.reply_editor_edit_subtitle);
        Button enterButton = editorContainer.findViewById(R.id.reply_send);
        Button closeButton = editorContainer.findViewById(R.id.editor_close);
        TextView editHeaderText = editorContainer.findViewById(R.id.header_text);
        editHeaderText.setText(R.string.reply_editor_edit_subtitle);

        mEditCommentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ReplyInputUIView.this.onCommentEditChanged(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // no-op
            }
        });

        // Handle send and close buttons
        enterButton.setOnClickListener(v -> onCommentEditFinished(mEditCommentEditText.getText().toString()));
        closeButton.setOnClickListener(v -> onCommentEditCancelled());
    }

    @Override
    public void clearInput() {
        mWriteMessageEditText.setText("");
    }

    @Override
    public void showMessageEditView(String content) {
        showEditText(mEditMessageComponent, mEditMessageEditText, content);
    }

    @Override
    public void closeMessageEditView() {
        closeEditText(mEditMessageEditText);
    }

    @Override
    public void showEditCommentView(String content) {
        showEditText(mEditCommentComponent, mEditCommentEditText, content);
    }

    @Override
    public void closeCommentEditView() {
        closeEditText(mEditCommentEditText);
    }

    @Override
    public void setReplyEditTextHint(@StringRes int hint) {
        mWriteMessageEditText.setHint(hint);
    }

    private void closeEditText(@NonNull EditText editText) {
        // Hide the keyboard when finished editing message and clear the edit text
        editText.setText("");
        Utils.hideSoftKeyboard(editText.getContext(), editText);
        editText.clearFocus();

        // Detach the edit message view
        mParent.removeAllViews();
        mParent.addView(mWriteMessageComponent);
    }

    private void showEditText(@NonNull ConstraintLayout constraintLayout, @NonNull EditText editText, @NonNull String content) {
        // Show the keyboard when editing message
        editText.requestFocus();
        Utils.showSoftKeyboard(editText.getContext(), editText);

        // Attach the edit message view and initialize the edit text
        mParent.removeAllViews();
        mParent.addView(constraintLayout);
        editText.setText("");
        editText.append(content);
    }
}
