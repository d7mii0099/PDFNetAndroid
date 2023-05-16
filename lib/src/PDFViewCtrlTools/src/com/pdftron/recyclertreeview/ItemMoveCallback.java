package com.pdftron.recyclertreeview;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import co.paulburke.android.itemtouchhelperdemo.helper.ItemTouchHelperViewHolder;

import static co.paulburke.android.itemtouchhelperdemo.helper.SimpleItemTouchHelperCallback.ALPHA_FULL;

public class ItemMoveCallback extends ItemTouchHelper.SimpleCallback {

    private TreeViewAdapter mAdapter;
    protected int mDragFromPosition;
    protected int mDragToPosition;

    public ItemMoveCallback(TreeViewAdapter adapter) {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END, 0);
        mAdapter = adapter;
        mDragFromPosition = RecyclerView.NO_POSITION;
        mDragToPosition = RecyclerView.NO_POSITION;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END, 0);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {

        mDragFromPosition = viewHolder.getAdapterPosition();
        mDragToPosition = target.getAdapterPosition();

        mAdapter.itemMoved(viewHolder, mDragFromPosition, mDragToPosition);
        return true;
    }

    @Override
    public void onMoved(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, int fromPos, @NonNull RecyclerView.ViewHolder target, int toPos, int x, int y) {
        super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y);
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        // We only want the active item to change
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            if (viewHolder instanceof ItemTouchHelperViewHolder) {
                // Let the view holder know that this item is being moved or dragged
                ItemTouchHelperViewHolder itemViewHolder = (ItemTouchHelperViewHolder) viewHolder;
                itemViewHolder.onItemSelected();
            }
        }
        if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
            // notify UI the item has dropped without position change
            // useful in multi-section mode when section header text may change during dragging
            if (viewHolder != null)
                mAdapter.onItemDrop(viewHolder, mDragFromPosition, mDragToPosition);
        }

        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        viewHolder.itemView.setAlpha(ALPHA_FULL);

        if (viewHolder instanceof ItemTouchHelperViewHolder) {
            // Tell the view holder it's time to restore the idle state
            ItemTouchHelperViewHolder itemViewHolder = (ItemTouchHelperViewHolder) viewHolder;
            itemViewHolder.onItemClear();
        }

        if (mDragFromPosition != RecyclerView.NO_POSITION && mDragToPosition != RecyclerView.NO_POSITION) {
            // Notify the adapter of the drop
            mAdapter.onItemDrop(viewHolder, mDragFromPosition, mDragToPosition);
        }
        mDragFromPosition = mDragToPosition = RecyclerView.NO_POSITION;
    }
}
