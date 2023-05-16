package com.pdftron.collab.ui.reply.bottomsheet.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.pdftron.collab.R;
import com.pdftron.collab.ui.reply.bottomsheet.BottomSheetReplyFragment;
import com.pdftron.collab.ui.reply.component.header.BaseHeaderUIView;
import com.pdftron.collab.ui.view.NotificationImageButton;
import com.pdftron.pdf.model.AnnotReviewState;
import com.pdftron.pdf.utils.Utils;

/**
 * Base class for {@link BaseHeaderUIView} that represents a reply header in the
 * {@link BottomSheetReplyFragment}
 */
public class ReplyHeaderUIView extends BaseHeaderUIView {

    private final TextView mHeaderTitle;
    private final ImageView mAnnotIcon;
    private final TextView mAnnotText;
    private final ConstraintLayout mPreviewContainer;
    private final NotificationImageButton mHeaderList;
    private final ImageView mReviewStateIcon;
    private final ImageView mOverflowButton;

    private @Nullable
    AnnotReviewState mAnnotReviewState;
    private @Nullable
    Menu mAnnotReviewStateMenu;

    public ReplyHeaderUIView(@NonNull ViewGroup parent) {
        super(parent);

        Context context = parent.getContext();
        View container = LayoutInflater.from(context)
                .inflate(R.layout.content_reply_header, parent, true);

        LayoutInflater.from(context)
                .inflate(R.layout.view_horizontal_divider, parent, true);

        mPreviewContainer = LayoutInflater.from(context)
                .inflate(R.layout.content_reply_header_preview, parent, true)
                .findViewById(R.id.reply_header_preview_container);

        // Inflate views
        mHeaderTitle = container.findViewById(R.id.header_title);
        ImageView close = container.findViewById(R.id.header_close);
        mHeaderList = container.findViewById(R.id.header_list);
        mAnnotIcon = container.findViewById(R.id.annot_icon);
        mAnnotText = container.findViewById(R.id.annot_text);
        mReviewStateIcon = container.findViewById(R.id.header_state);
        mOverflowButton = container.findViewById(R.id.overflow_button);

        mReviewStateIcon.setOnClickListener(v -> this.showReviewStateMenu());
        close.setOnClickListener(v -> this.onCloseClicked(null));
        mHeaderList.setOnClickListener(v -> this.onListClicked(null));
        mOverflowButton.setOnClickListener(v -> showEditOverflow());
    }

    private void showEditOverflow() {
        View anchor = mOverflowButton;
        Context context = anchor.getContext();
        Utils.hideSoftKeyboard(context, anchor); // hide keyboard as the menu location will be incorrect
        Context wrapper = new ContextThemeWrapper(anchor.getContext(), R.style.ReplyPopupTheme);
        PopupMenu popupMenu = new PopupMenu(wrapper, anchor);
        popupMenu.inflate(R.menu.popup_reply_content_overflow);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.item_edit_comment) {
                    onAnnotCommentModifyClicked(null);
                } else {
                    return false;
                }
                return true;
            }
        });
        popupMenu.show();
    }

    @Override
    public void showPreviewHeader() {
        mPreviewContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void hidePreviewHeader() {
        mPreviewContainer.setVisibility(View.GONE);
    }

    @Override
    public void setReviewStateIcon(boolean visible) {
        mReviewStateIcon.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setHeaderTitle(@NonNull String title) {
        mHeaderTitle.setText(title);
    }

    @Override
    public void setNotificationIcon(boolean hasUnreadReplies) {
        mHeaderList.setNotificationVisibility(hasUnreadReplies);
    }

    @Override
    public void setAnnotationListIcon(boolean visible) {
        mHeaderList.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setAnnotationReviewState(@Nullable AnnotReviewState reviewState) {
        mAnnotReviewState = reviewState;
        updateReviewState();
    }

    @Override
    public void setCommentEditButton(boolean visible) {
        mOverflowButton.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setAnnotationPreviewIcon(@DrawableRes int iconRed, @ColorInt int color, float opacity) {
        mAnnotIcon.setImageResource(iconRed);
        mAnnotIcon.setColorFilter(color);
        mAnnotIcon.setAlpha(opacity);
    }

    @Override
    public void setAnnotationPreviewText(@NonNull String text) {
        mAnnotText.setText(text);
    }

    @SuppressLint("RestrictedApi")
    private void showReviewStateMenu() {
        Context context = mReviewStateIcon.getContext();
        Utils.hideSoftKeyboard(context, mReviewStateIcon); // hide keyboard as the menu location will be incorrect
        Context wrapper = new ContextThemeWrapper(context, R.style.ReplyPopupTheme);
        PopupMenu popupMenu = new PopupMenu(wrapper, mReviewStateIcon);
        popupMenu.inflate(R.menu.popup_annot_review_state);
        mAnnotReviewStateMenu = popupMenu.getMenu();
        updateReviewState();
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_review_state_none) {
                this.onReviewStateClicked(null, AnnotReviewState.NONE);
            } else if (id == R.id.menu_review_state_accepted) {
                this.onReviewStateClicked(null, AnnotReviewState.ACCEPTED);
            } else if (id == R.id.menu_review_state_cancelled) {
                this.onReviewStateClicked(null, AnnotReviewState.CANCELLED);
            } else if (id == R.id.menu_review_state_completed) {
                this.onReviewStateClicked(null, AnnotReviewState.COMPLETED);
            } else if (id == R.id.menu_review_state_rejected) {
                this.onReviewStateClicked(null, AnnotReviewState.REJECTED);
            } else {
                return false;
            }
            return true;
        });
        popupMenu.setOnDismissListener(menu -> mAnnotReviewStateMenu = null);
        MenuPopupHelper menuHelper = new MenuPopupHelper(wrapper, (MenuBuilder) mAnnotReviewStateMenu, mReviewStateIcon);
        menuHelper.setForceShowIcon(true);

        menuHelper.show();
    }

    private void updateReviewState() {
        if (mAnnotReviewState != null) {
            switch (mAnnotReviewState) {
                case COMPLETED:
                    if (mAnnotReviewStateMenu != null) {
                        mAnnotReviewStateMenu.findItem(R.id.menu_review_state_completed).setChecked(true);
                    }
                    mReviewStateIcon.setImageResource(R.drawable.ic_state_completed);
                    break;
                case CANCELLED:
                    if (mAnnotReviewStateMenu != null) {
                        mAnnotReviewStateMenu.findItem(R.id.menu_review_state_cancelled).setChecked(true);
                    }
                    mReviewStateIcon.setImageResource(R.drawable.ic_state_cancelled);
                    break;
                case REJECTED:
                    if (mAnnotReviewStateMenu != null) {
                        mAnnotReviewStateMenu.findItem(R.id.menu_review_state_rejected).setChecked(true);
                    }
                    mReviewStateIcon.setImageResource(R.drawable.ic_state_rejected);
                    break;
                case ACCEPTED:
                    if (mAnnotReviewStateMenu != null) {
                        mAnnotReviewStateMenu.findItem(R.id.menu_review_state_accepted).setChecked(true);
                    }
                    mReviewStateIcon.setImageResource(R.drawable.ic_state_accepted);
                    break;
                case NONE:
                    if (mAnnotReviewStateMenu != null) {
                        mAnnotReviewStateMenu.findItem(R.id.menu_review_state_none).setChecked(true);
                    }
                    mReviewStateIcon.setImageResource(R.drawable.ic_state_none);
                    break;
            }
        }
    }
}
