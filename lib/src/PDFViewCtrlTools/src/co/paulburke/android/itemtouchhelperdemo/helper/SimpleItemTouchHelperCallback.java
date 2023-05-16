/*
 * Copyright (C) 2015 Paul Burke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.paulburke.android.itemtouchhelperdemo.helper;

import android.graphics.Canvas;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

/**
 * An implementation of {@link ItemTouchHelper.Callback} that enables basic drag & drop and
 * swipe-to-dismiss. Drag events are automatically started by an item long-press.<br/>
 * </br/>
 * Expects the <code>RecyclerView.Adapter</code> to listen for {@link
 * ItemTouchHelperAdapter} callbacks and the <code>RecyclerView.ViewHolder</code> to implement
 * {@link ItemTouchHelperViewHolder}.
 *
 * @author Paul Burke (ipaulpro)
 */
public class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {

    public static final float ALPHA_FULL = 1.0f;

    public static final int VIEW_TYPE_HEADER = 0x01;
    public static final int VIEW_TYPE_CONTENT = 0x00;
    public static final int VIEW_TYPE_FOOTER = 0x02;

    protected final ItemTouchHelperAdapter mAdapter;
    protected int mSpan;
    protected final boolean mLongPressDragEnabled;
    protected final boolean mItemViewSwipeEnabled;

    protected int mDragFromPosition;
    protected int mDragToPosition;

    protected boolean mAllowDragAmongSections;

    public SimpleItemTouchHelperCallback(ItemTouchHelperAdapter adapter, int span,
                                         boolean enableLongPress, boolean enableSwipe) {
        mAdapter = adapter;
        mSpan = span;
        mLongPressDragEnabled = enableLongPress;
        mItemViewSwipeEnabled = enableSwipe;

        mDragFromPosition = RecyclerView.NO_POSITION;
        mDragToPosition = RecyclerView.NO_POSITION;
    }

    public void setSpan(int span) {
        mSpan = span;
    }

    /**
     * Ability to drag items among different sections
     */
    public void setAllowDragAmongSections(boolean allowDragAmongSections) {
        this.mAllowDragAmongSections = allowDragAmongSections;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return mLongPressDragEnabled;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return mItemViewSwipeEnabled;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        // Set movement flags based on the layout manager
        if (viewHolder.getItemViewType() == VIEW_TYPE_HEADER) {
            return makeMovementFlags(0, 0);
        }
        if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
            final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
            final int swipeFlags = 0;
            return makeMovementFlags(dragFlags, swipeFlags);
        } else {
            final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            final int swipeFlags = 0;
            return makeMovementFlags(dragFlags, swipeFlags);
        }
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
        if (source.getItemViewType() != target.getItemViewType() && !mAllowDragAmongSections) {
            return false;
        }
        // Record only the first drag-start position
        if (mDragFromPosition == RecyclerView.NO_POSITION) {
            mDragFromPosition = source.getAdapterPosition();
        }
        mDragToPosition = target.getAdapterPosition();

        // Notify the adapter of the move
        mAdapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
        // Notify the adapter of the dismissal
        mAdapter.onItemDismiss(viewHolder.getAdapterPosition());
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            // Fade out the view as it is swiped out of the parent's bounds
            final float alpha = ALPHA_FULL - Math.abs(dX) / (float) viewHolder.itemView.getWidth();
            viewHolder.itemView.setAlpha(alpha);
            viewHolder.itemView.setTranslationX(dX);
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
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
        if (mAllowDragAmongSections && actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
            // notify UI the item has dropped without position change
            // useful in multi-section mode when section header text may change during dragging
            mAdapter.onItemDrop(-1, -1);
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
            mAdapter.onItemDrop(mDragFromPosition, mDragToPosition);
        }
        mDragFromPosition = mDragToPosition = RecyclerView.NO_POSITION;
    }
}
