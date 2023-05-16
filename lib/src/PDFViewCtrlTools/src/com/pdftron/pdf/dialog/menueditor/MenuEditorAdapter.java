package com.pdftron.pdf.dialog.menueditor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.pdf.dialog.menueditor.model.MenuEditorItem;
import com.pdftron.pdf.dialog.menueditor.model.MenuEditorItemContent;
import com.pdftron.pdf.dialog.menueditor.model.MenuEditorItemHeader;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.recyclerview.ItemTouchHelperCallback;

import java.util.ArrayList;

import co.paulburke.android.itemtouchhelperdemo.helper.ItemTouchHelperAdapter;

public class MenuEditorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements
        ItemTouchHelperAdapter {

    public static final int VIEW_TYPE_HEADER = ItemTouchHelperCallback.VIEW_TYPE_HEADER;
    public static final int VIEW_TYPE_CONTENT = ItemTouchHelperCallback.VIEW_TYPE_CONTENT;

    protected final ArrayList<MenuEditorItem> mMenuEditorItems = new ArrayList<>();
    protected MenuEditorViewModel mViewModel;
    private boolean mOrderHasBeenModified = false;

    protected boolean mDragging;
    @Nullable
    private Theme mTheme = null; // lazy initialized later

    public MenuEditorAdapter() {

    }

    public MenuEditorAdapter(@NonNull ArrayList<MenuEditorItem> menuEditorItems) {
        mMenuEditorItems.addAll(menuEditorItems);
    }

    public void setData(@NonNull ArrayList<MenuEditorItem> menuEditorItems) {
        mMenuEditorItems.clear();
        mMenuEditorItems.addAll(menuEditorItems);
        notifyDataSetChanged();
    }

    public void setViewModel(MenuEditorViewModel viewModel) {
        mViewModel = viewModel;
    }

    public void setDragging(boolean dragging) {
        mDragging = dragging;
    }

    public boolean isDragging() {
        return mDragging;
    }

    @Override
    public int getItemViewType(int position) {
        if (mMenuEditorItems.get(position).isHeader()) {
            return VIEW_TYPE_HEADER;
        } else {
            return VIEW_TYPE_CONTENT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mTheme == null) {
            mTheme = Theme.fromContext(parent.getContext());
        }
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu_editor_header, parent, false);
            HeaderViewHolder headerViewHolder = new HeaderViewHolder(view);
            headerViewHolder.mTitle.setTextColor(mTheme.headerColor);
            headerViewHolder.mDescription.setTextColor(mTheme.headerColor);
            return headerViewHolder;
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu_editor_content, parent, false);
            ContentViewHolder contentViewHolder = new ContentViewHolder(view);
            contentViewHolder.mIcon.setColorFilter(mTheme.iconColor);
            contentViewHolder.mTitle.setTextColor(mTheme.textColor);
            return contentViewHolder;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MenuEditorItem item = mMenuEditorItems.get(position);
        if (holder.getItemViewType() == VIEW_TYPE_HEADER && item instanceof MenuEditorItemHeader) {
            initItemHeader((MenuEditorItemHeader) item, (HeaderViewHolder) holder);
        } else if (holder.getItemViewType() == VIEW_TYPE_CONTENT && item instanceof MenuEditorItemContent) {
            initItemContent((MenuEditorItemContent) item, (ContentViewHolder) holder);
        }
    }

    private void initItemHeader(@NonNull MenuEditorItemHeader itemHeader, @NonNull HeaderViewHolder headerViewHolder) {
        if (mDragging) {
            if (itemHeader.getDraggingTitle() != null) {
                headerViewHolder.mTitle.setText(itemHeader.getDraggingTitle());
            } else if (itemHeader.getDraggingTitleId() != 0) {
                headerViewHolder.mTitle.setText(itemHeader.getDraggingTitleId());
            } else {
                headerViewHolder.mTitle.setVisibility(View.GONE);
                headerViewHolder.mLayout.setPadding(0, 0, 0, 0);
            }
        } else {
            if (itemHeader.getTitle() != null) {
                headerViewHolder.mTitle.setVisibility(View.VISIBLE);
                headerViewHolder.mTitle.setText(itemHeader.getTitle());
            } else if (itemHeader.getTitleId() != 0) {
                headerViewHolder.mTitle.setVisibility(View.VISIBLE);
                headerViewHolder.mTitle.setText(itemHeader.getTitleId());
            } else {
                headerViewHolder.mTitle.setVisibility(View.GONE);
                headerViewHolder.mLayout.setPadding(0, 0, 0, 0);
            }
        }
        if (!Utils.isNullOrEmpty(itemHeader.getDescription())) {
            headerViewHolder.mDescription.setText(itemHeader.getDescription());
            headerViewHolder.mDescription.setVisibility(View.VISIBLE);
        } else if (itemHeader.getDescriptionId() != 0) {
            headerViewHolder.mDescription.setText(itemHeader.getDescriptionId());
            headerViewHolder.mDescription.setVisibility(View.VISIBLE);
        } else {
            headerViewHolder.mDescription.setVisibility(View.GONE);
        }
    }

    private void initItemContent(@NonNull MenuEditorItemContent itemContent, @NonNull ContentViewHolder contentViewHolder) {
        contentViewHolder.mTitle.setText(itemContent.getTitle());
        if (itemContent.getDrawable() != null) {
            contentViewHolder.mIcon.setImageDrawable(itemContent.getDrawable());
            contentViewHolder.mIcon.getDrawable().setAlpha(255);
        } else if (itemContent.getIconRes() != 0) {
            contentViewHolder.mIcon.setImageResource(itemContent.getIconRes());
        }
    }

    @Override
    public int getItemCount() {
        return mMenuEditorItems.size();
    }

    public void insert(MenuEditorItem item, int position) {
        if (item != null) {
            mMenuEditorItems.add(position, item);
        }
    }

    public MenuEditorItem removeAt(int position) {
        return mMenuEditorItems.remove(position);
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        if (toPosition == 0) {
            // should not drag before the first header
            return false;
        }
        MenuEditorItem item = mMenuEditorItems.remove(fromPosition);
        if (item != null) {
            mMenuEditorItems.add(toPosition, item);
            notifyItemMoved(fromPosition, toPosition);
            return true;
        }
        return false;
    }

    @Override
    public void onItemDrop(int fromPosition, int toPosition) {
        // If item has been moved to a different position, then assume it has been modified
        if (fromPosition != toPosition) {
            mOrderHasBeenModified = true;
        }
        mDragging = false;
        notifyHeadersChanged();
        if (mViewModel != null) {
            mViewModel.setItems(mMenuEditorItems);
        }
    }

    public MenuEditorItem getItem(int position) {
        return mMenuEditorItems.get(position);
    }

    public boolean isHeader(int position) {
        MenuEditorItem item = mMenuEditorItems.get(position);
        return item.isHeader();
    }

    public void notifyHeadersChanged() {
        for (int i = 0; i < mMenuEditorItems.size(); i++) {
            MenuEditorItem item = mMenuEditorItems.get(i);
            if (item.isHeader()) {
                notifyItemChanged(i);
            }
        }
    }

    @Override
    public void onItemDismiss(int position) {

    }

    boolean orderHasBeenModified() {
        return mOrderHasBeenModified;
    }

    static class ContentViewHolder extends RecyclerView.ViewHolder {

        LinearLayout mLayout;
        TextView mTitle;
        AppCompatImageView mIcon;

        ContentViewHolder(@NonNull View itemView) {
            super(itemView);
            mLayout = itemView.findViewById(R.id.layout_root);
            mTitle = itemView.findViewById(R.id.title);
            mIcon = itemView.findViewById(R.id.icon);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {

        LinearLayout mLayout;
        TextView mTitle;
        TextView mDescription;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            mLayout = itemView.findViewById(R.id.layout_root);
            mTitle = itemView.findViewById(R.id.title);
            mDescription = itemView.findViewById(R.id.description);
        }
    }
}
