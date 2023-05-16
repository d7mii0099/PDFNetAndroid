package com.pdftron.pdf.dialog.menueditor;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.pdf.dialog.menueditor.model.MenuEditorItem;

public class MenuCreatorAdapter extends MenuEditorAdapter {

    public interface OnItemActionListener {
        void onItemMove(int toPosition);
        void onItemDrop();
    }

    private View.OnDragListener mOnDragListener;
    private OnItemActionListener mOnItemActionListener;

    public void setOnDragListener(View.OnDragListener listener) {
        mOnDragListener = listener;
    }

    public void setOnItemActionListener(OnItemActionListener listener) {
        mOnItemActionListener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        if (holder.getItemViewType() == VIEW_TYPE_CONTENT) {
            ContentViewHolder contentViewHolder = (ContentViewHolder) holder;
            contentViewHolder.mLayout.setTag(position);
            contentViewHolder.mLayout.setOnDragListener(mOnDragListener);
        }
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        if (super.onItemMove(fromPosition, toPosition)) {
            if (mOnItemActionListener != null) {
                mOnItemActionListener.onItemMove(toPosition);
            }
            return true;
        }
        return false;
    }

    @Override
    public void insert(MenuEditorItem item, int position) {
        super.insert(item, position);
        // notify view model of data change last
        notifyPinnedItemsChanged();
    }

    @Override
    public void onItemDrop(int fromPosition, int toPosition) {
        mDragging = false;
        notifyHeadersChanged();
        if (mOnItemActionListener != null) {
            mOnItemActionListener.onItemDrop();
        }
        // notify view model of data change last
        notifyPinnedItemsChanged();
    }

    public void notifyPinnedItemsChanged() {
        if (mViewModel != null) {
            mViewModel.setPinnedItems(mMenuEditorItems);
        }
    }
}
