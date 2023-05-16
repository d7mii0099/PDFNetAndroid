package com.pdftron.collab.ui.reply.component.header;

import android.view.ViewGroup;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.pdftron.collab.ui.base.component.BaseUIView;
import com.pdftron.collab.ui.reply.model.ReplyHeader;
import com.pdftron.pdf.model.AnnotReviewState;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class BaseHeaderUIView extends BaseUIView<HeaderEvent> {

    public BaseHeaderUIView(@NonNull ViewGroup parent) {
        super(parent);
    }

    protected void onCloseClicked(@Nullable ReplyHeader data) {
        if (mEventObservable != null) {
            mEventObservable.onNext(new HeaderEvent(HeaderEvent.Type.CLOSE_CLICKED, data));
        }
    }

    public void onListClicked(@Nullable ReplyHeader data) {
        if (mEventObservable != null) {
            mEventObservable.onNext(new HeaderEvent(HeaderEvent.Type.LIST_CLICKED, data));
        }
    }

    public void onAnnotCommentModifyClicked(@Nullable ReplyHeader data) {
        if (mEventObservable != null) {
            mEventObservable.onNext(new HeaderEvent(HeaderEvent.Type.ANNOT_COMMENT_MODIFY, data));
        }
    }

    public void onReviewStateClicked(@Nullable ReplyHeader data, @NonNull AnnotReviewState reviewState) {
        if (mEventObservable != null) {
            switch (reviewState) {
                case NONE:
                    mEventObservable.onNext(new HeaderEvent(HeaderEvent.Type.REVIEW_STATE_NONE_CLICKED, data));
                    break;
                case ACCEPTED:
                    mEventObservable.onNext(new HeaderEvent(HeaderEvent.Type.REVIEW_STATE_ACCEPTED_CLICKED, data));
                    break;
                case REJECTED:
                    mEventObservable.onNext(new HeaderEvent(HeaderEvent.Type.REVIEW_STATE_REJECTED_CLICKED, data));
                    break;
                case CANCELLED:
                    mEventObservable.onNext(new HeaderEvent(HeaderEvent.Type.REVIEW_STATE_CANCELLED_CLICKED, data));
                    break;
                case COMPLETED:
                    mEventObservable.onNext(new HeaderEvent(HeaderEvent.Type.REVIEW_STATE_COMPLETED_CLICKED, data));
                    break;
            }
        }
    }

    public abstract void setHeaderTitle(@NonNull String title);

    public abstract void showPreviewHeader();

    public abstract void hidePreviewHeader();

    public abstract void setAnnotationPreviewIcon(@DrawableRes int icon, @ColorInt int color, float opacity);

    public abstract void setAnnotationPreviewText(@NonNull String content);

    public abstract void setNotificationIcon(boolean hasUnreadReplies);

    public abstract void setAnnotationListIcon(boolean visible);

    public abstract void setReviewStateIcon(boolean visible);

    public abstract void setAnnotationReviewState(@Nullable AnnotReviewState reviewState);

    public abstract void setCommentEditButton(boolean visible);
}
