package com.pdftron.collab.ui.reply.model;

import android.content.Context;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.collab.R;
import com.pdftron.collab.db.entity.AnnotationEntity;
import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.model.AnnotReviewState;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.Utils;

import java.util.Objects;

/**
 * View state model representing the content/information in the reply header.
 */
public class ReplyHeader {

    @NonNull
    private final String title;

    @NonNull
    private final String previewContent;
    @DrawableRes
    private final int previewIcon;
    @ColorInt
    private final int previewIconColor;
    private final float previewIconOpacity;

    private final boolean hasUnreadReplies;
    private boolean hasAnnotList;
    private boolean hasReviewState;
    @Nullable
    private final AnnotReviewState reviewState;
    private boolean commentEditable;
    private boolean disableCommentEdit;

    private String annotId;
    private int pageNum;

    public ReplyHeader(@NonNull Context context, @NonNull Annot annot, boolean hasUnreadReplies, boolean hasAnnotList, boolean hasReviewState) {
        this.title = Utils.capitalize(AnnotUtils.getAnnotTypeAsString(context, getType(annot)));
        this.previewContent = getContents(annot);
        this.previewIcon = AnnotUtils.getAnnotImageResId(getType(annot));
        this.previewIconColor = AnnotUtils.getAnnotColor(annot);
        this.previewIconOpacity = AnnotUtils.getAnnotOpacity(annot);
        this.hasUnreadReplies = hasUnreadReplies;
        this.hasAnnotList = hasAnnotList;
        this.hasReviewState = hasReviewState;
        this.reviewState = null;
        this.disableCommentEdit = false;
    }

    public ReplyHeader(@NonNull Context context, @NonNull Annot annot, boolean hasUnreadReplies, boolean hasAnnotList, boolean hasReviewState, boolean disableCommentEdit) {
        this.title = Utils.capitalize(AnnotUtils.getAnnotTypeAsString(context, getType(annot)));
        this.previewContent = getContents(annot);
        this.previewIcon = AnnotUtils.getAnnotImageResId(getType(annot));
        this.previewIconColor = AnnotUtils.getAnnotColor(annot);
        this.previewIconOpacity = AnnotUtils.getAnnotOpacity(annot);
        this.hasUnreadReplies = hasUnreadReplies;
        this.hasAnnotList = hasAnnotList;
        this.hasReviewState = hasReviewState;
        this.reviewState = null;
        this.disableCommentEdit = disableCommentEdit;
    }

    public ReplyHeader(@NonNull Context context, @NonNull AnnotationEntity annotationEntity) {
        int type = annotationEntity.getType();
        this.title = generateTitle(context, type, annotationEntity.getAuthorName());
        this.previewContent = annotationEntity.getContents() == null ? "" : annotationEntity.getContents();
        this.previewIcon = AnnotUtils.getAnnotImageResId(type);
        this.previewIconColor = annotationEntity.getColor();
        this.previewIconOpacity = annotationEntity.getOpacity();
        this.hasUnreadReplies = false;
        this.reviewState = null;
    }

    public ReplyHeader(@NonNull String title,
            @NonNull String previewContent,
            int previewIcon,
            int previewIconColor,
            float previewIconOpacity,
            boolean hasUnreadReplies,
            boolean hasAnnotList,
            boolean hasReviewState,
            @Nullable AnnotReviewState reviewState) {
        this.title = title;
        this.previewContent = previewContent;
        this.previewIcon = previewIcon;
        this.previewIconColor = previewIconColor;
        this.previewIconOpacity = previewIconOpacity;
        this.hasUnreadReplies = hasUnreadReplies;
        this.reviewState = reviewState;
        this.hasReviewState = hasReviewState;
        this.hasAnnotList = hasAnnotList;
    }

    @Nullable
    public AnnotReviewState getReviewState() {
        return reviewState;
    }

    @NonNull
    private String getContents(@NonNull Annot annot) {
        try {
            return annot.getContents();
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            return "";
        }
    }

    public boolean hasUnreadReplies() {
        return hasUnreadReplies;
    }

    public void setHasAnnotList(boolean hasAnnotList) {
        this.hasAnnotList = hasAnnotList;
    }

    public boolean hasAnnotationList() {
        return hasAnnotList;
    }

    public void setHasReviewState(boolean hasReviewState) {
        this.hasReviewState = hasReviewState;
    }

    public boolean hasReviewState() {
        return hasReviewState;
    }

    private int getType(@NonNull Annot annot) {
        try {
            return AnnotUtils.getAnnotType(annot);
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            return -1;
        }
    }

    @NonNull
    public String getPreviewContent() {
        return previewContent;
    }

    public int getPreviewIcon() {
        return previewIcon;
    }

    @ColorInt
    public int getPreviewIconColor() {
        return previewIconColor;
    }

    public float getPreviewIconOpacity() {
        return previewIconOpacity;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReplyHeader that = (ReplyHeader) o;

        if (previewIcon != that.previewIcon) return false;
        if (previewIconColor != that.previewIconColor) return false;
        if (Float.compare(that.previewIconOpacity, previewIconOpacity) != 0) return false;
        if (hasUnreadReplies != that.hasUnreadReplies) return false;
        if (!title.equals(that.title)) return false;
        if (!previewContent.equals(that.previewContent)) return false;
        if (commentEditable != that.commentEditable) return false;
        if (disableCommentEdit != that.disableCommentEdit) return false;
        return reviewState == that.reviewState;
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, previewContent, previewIcon, previewIconColor, previewIconOpacity, hasUnreadReplies, reviewState, commentEditable, disableCommentEdit);
    }

    public void setCommentEditable(boolean editable) {
        this.commentEditable = editable;
    }

    public boolean isCommentEditable() {
        return commentEditable && !disableCommentEdit;
    }

    /**
     * Helper method to get a reply header title from a given {@link Annot}. It will use the
     * annotation content and if that is unavailable it will use the annotation type name.
     *
     * @param context used to get string resources
     * @param annot   used to determine the title
     * @return reply header title generated from the given {@link Annot}
     */
    @NonNull
    public static String getTitleFromAnnot(@NonNull Context context, @NonNull Annot annot) {
        String content;
        int type;
        try {
            content = annot.getContents();
            type = annot.getType();
        } catch (PDFNetException e) {
            e.printStackTrace();
            content = "";
            type = -1;
        }

        if (Utils.isNullOrEmpty(content)) {
            return Utils.capitalize(AnnotUtils.getAnnotTypeAsString(context, type));
        } else {
            return content;
        }
    }

    /**
     * Returns string from {@link #getTitleFromContent(Context, String, int)} with the author
     * name appended to the beginning of the string.
     *
     * @param context used to get string resources
     * @param content content of the annotation
     * @param type    annotation type ID
     * @param author    author name
     * @return reply header title generated from the given content and type
     */
    public static String getTitleFromContent(@NonNull Context context, @Nullable String content, int type, @Nullable String author) {
        if (Utils.isNullOrEmpty(author)) {
            return getTitleFromContent(context, content, type);
        } else {
            return String.format("%s: %s", author, getTitleFromContent(context, content, type));
        }
    }

    /**
     * Helper method to get a reply header title from a given annotation content and type. It will
     * use the annotation contents and if that is unavailable it will use the annotation type name.
     *
     * @param context used to get string resources
     * @param content content of the annotation
     * @param type    annotation type ID
     * @return reply header title generated from the given content and type
     */
    public static String getTitleFromContent(@NonNull Context context, @Nullable String content, int type) {
        if (Utils.isNullOrEmpty(content)) {
            return Utils.capitalize(AnnotUtils.getAnnotTypeAsString(context, type));
        } else {
            return content;
        }
    }

    public static ReplyHeader updateUnreadReplies(@NonNull ReplyHeader oldReplyHeader, boolean hasUnreadReplies) {
        ReplyHeader replyHeader = new ReplyHeader(
                oldReplyHeader.getTitle(),
                oldReplyHeader.getPreviewContent(),
                oldReplyHeader.getPreviewIcon(),
                oldReplyHeader.getPreviewIconColor(),
                oldReplyHeader.getPreviewIconOpacity(),
                hasUnreadReplies,
                oldReplyHeader.hasAnnotationList(),
                oldReplyHeader.hasReviewState(),
                oldReplyHeader.getReviewState()
        );
        replyHeader.commentEditable = oldReplyHeader.commentEditable;
        replyHeader.annotId = oldReplyHeader.annotId;
        replyHeader.pageNum = oldReplyHeader.pageNum;
        replyHeader.disableCommentEdit = oldReplyHeader.disableCommentEdit;
        return replyHeader;
    }

    public static ReplyHeader updateReviewState(@NonNull ReplyHeader oldReplyHeader, @NonNull AnnotReviewState reviewState) {
        ReplyHeader replyHeader = new ReplyHeader(
                oldReplyHeader.getTitle(),
                oldReplyHeader.getPreviewContent(),
                oldReplyHeader.getPreviewIcon(),
                oldReplyHeader.getPreviewIconColor(),
                oldReplyHeader.getPreviewIconOpacity(),
                oldReplyHeader.hasUnreadReplies(),
                oldReplyHeader.hasAnnotationList(),
                oldReplyHeader.hasReviewState(),
                reviewState
        );
        replyHeader.commentEditable = oldReplyHeader.commentEditable;
        replyHeader.annotId = oldReplyHeader.annotId;
        replyHeader.pageNum = oldReplyHeader.pageNum;
        replyHeader.disableCommentEdit = oldReplyHeader.disableCommentEdit;
        return replyHeader;
    }

    public static ReplyHeader updatePreviewContent(@NonNull ReplyHeader oldReplyHeader, @NonNull String previewContent) {
        ReplyHeader replyHeader = new ReplyHeader(
                oldReplyHeader.getTitle(),
                previewContent,
                oldReplyHeader.getPreviewIcon(),
                oldReplyHeader.getPreviewIconColor(),
                oldReplyHeader.getPreviewIconOpacity(),
                oldReplyHeader.hasUnreadReplies(),
                oldReplyHeader.hasAnnotationList(),
                oldReplyHeader.hasReviewState(),
                oldReplyHeader.getReviewState()
        );
        replyHeader.commentEditable = oldReplyHeader.commentEditable;
        replyHeader.annotId = oldReplyHeader.annotId;
        replyHeader.pageNum = oldReplyHeader.pageNum;
        replyHeader.disableCommentEdit = oldReplyHeader.disableCommentEdit;
        return replyHeader;
    }

    public void setId(@Nullable String id) {
        annotId = id;
    }

    public void setPageNumber(int pageNumber) {
        pageNum = pageNumber;
    }

    public String getId() {
        return annotId;
    }

    public int getPageNum() {
        return pageNum;
    }

    private static String generateTitle(@NonNull Context context, int annotType, @Nullable String authorName) {
        String type = Utils.capitalize(AnnotUtils.getAnnotTypeAsString(context, annotType));
        if (authorName != null) {
            return String.format(context.getString(R.string.reply_title_with_author), type, authorName);
        } else {
            return type;
        }
    }
}
