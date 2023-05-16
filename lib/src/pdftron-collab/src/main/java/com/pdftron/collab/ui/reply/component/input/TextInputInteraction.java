package com.pdftron.collab.ui.reply.component.input;

import androidx.annotation.Nullable;

interface TextInputInteraction {

    void onMessageWriteChanged(@Nullable String input);

    void onMessageWriteFinished(@Nullable String input);

    void onMessageEditChanged(@Nullable String input);

    void onMessageEditFinished(@Nullable String newMessage);

    void onMessagedEditCancelled();

    void onCommentEditChanged(@Nullable String input);

    void onCommentEditFinished(@Nullable String newComment);

    void onCommentEditCancelled();
}
