package com.pdftron.collab.ui.annotlist.model.list.item;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.controls.AnnotationDialogFragment;
import com.pdftron.pdf.model.AnnotReviewState;

import java.util.Date;

/**
 * Immutable data class representing an annotation item displayed in the annotation list.
 */
public class AnnotationListContent extends AnnotationDialogFragment.AnnotationInfo
        implements AnnotationListItem {

    private final String mId;
    private final String mLastReplyComment;
    private final Date mLastReplyDate;
    private final String mLastReplyAuthor;
    private final int mColor;
    private final float mOpacity;
    private final Date mCreationDate; // this is the same as mDate in AnnotationInfo
    private final int mUnreadCount;
    private final AnnotReviewState mReviewState;

    public AnnotationListContent(
            @NonNull String id,
            int type,
            int pageNum,
            String content,
            String author,
            Date creationDate,
            @Nullable Annot annotation,
            double y2,
            String lastReplyComment,
            @Nullable Date lastReplyDate,
            String lastReplyAuthor,
            int color,
            float opacity,
            int unreadCount,
            @Nullable AnnotReviewState reviewState) {
        super(type, pageNum, content, author, creationDate.toString(), annotation, y2);
        this.mId = id;
        this.mLastReplyComment = lastReplyComment;
        this.mLastReplyDate = lastReplyDate;
        this.mLastReplyAuthor = lastReplyAuthor;
        this.mColor = color;
        this.mOpacity = opacity;
        this.mCreationDate = creationDate;
        this.mUnreadCount = unreadCount;
        this.mReviewState = reviewState;
    }

    public int getUnreadCount() {
        return mUnreadCount;
    }

    @Nullable
    public AnnotReviewState getReviewState() {
        return mReviewState;
    }

    @Override
    public boolean isHeader() {
        return false;
    }

    public String getLastReplyComment() {
        return mLastReplyComment;
    }

    @Nullable
    public Date getLastReplyDate() {
        return mLastReplyDate;
    }

    public String getLastReplyAuthor() {
        return mLastReplyAuthor;
    }

    public int getColor() {
        return mColor;
    }

    public float getOpacity() {
        return mOpacity;
    }

    public Date getCreationDate() {
        return mCreationDate;
    }

    @NonNull
    public String getId() {
        return mId;
    }
}