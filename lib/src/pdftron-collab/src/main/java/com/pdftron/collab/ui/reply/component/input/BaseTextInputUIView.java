package com.pdftron.collab.ui.reply.component.input;

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;

import com.pdftron.collab.ui.base.component.BaseUIView;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class BaseTextInputUIView extends BaseUIView<InputEvent> implements TextInputInteraction {

    public BaseTextInputUIView(@NonNull ViewGroup parent) {
        super(parent);
    }

    @Override
    public void onMessageWriteChanged(@Nullable String input) {
        if (mEventObservable != null) {
            mEventObservable.onNext(new InputEvent(InputEvent.Type.MESSAGE_WRITE_TEXT_CHANGED, input));
        }
    }

    @Override
    public void onMessageWriteFinished(@Nullable String input) {
        if (mEventObservable != null) {
            mEventObservable.onNext(new InputEvent(InputEvent.Type.MESSAGE_WRITE_FINISHED, input));
        }
    }

    @Override
    public void onMessageEditChanged(@Nullable String input) {
        if (mEventObservable != null) {
            mEventObservable.onNext(new InputEvent(InputEvent.Type.MESSAGE_EDIT_TEXT_CHANGED, input));
        }
    }

    @Override
    public void onMessageEditFinished(@Nullable String newMessage) {
        if (mEventObservable != null) {
            mEventObservable.onNext(new InputEvent(InputEvent.Type.MESSAGE_EDIT_FINISHED, newMessage));
        }
    }

    @Override
    public void onMessagedEditCancelled() {
        if (mEventObservable != null) {
            mEventObservable.onNext(new InputEvent(InputEvent.Type.MESSAGE_EDIT_CANCELED, null));
        }
    }

    @Override
    public void onCommentEditChanged(@Nullable String input) {
        if (mEventObservable != null) {
            mEventObservable.onNext(new InputEvent(InputEvent.Type.COMMENT_EDIT_TEXT_CHANGED, input));
        }
    }

    @Override
    public void onCommentEditFinished(@Nullable String newComment) {
        if (mEventObservable != null) {
            mEventObservable.onNext(new InputEvent(InputEvent.Type.COMMENT_EDIT_FINISHED, newComment));
        }
    }

    @Override
    public void onCommentEditCancelled() {
        if (mEventObservable != null) {
            mEventObservable.onNext(new InputEvent(InputEvent.Type.COMMENT_EDIT_CANCELED, null));
        }
    }

    public abstract void clearInput(); // todo make this reactive

    public abstract void showMessageEditView(String content);

    public abstract void closeMessageEditView();

    public abstract void showEditCommentView(String content);

    public abstract void closeCommentEditView();

    public abstract void setReplyEditTextHint(@StringRes int hint);
}
