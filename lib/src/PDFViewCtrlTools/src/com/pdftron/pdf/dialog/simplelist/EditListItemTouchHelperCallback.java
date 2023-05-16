package com.pdftron.pdf.dialog.simplelist;

import android.graphics.Canvas;
import android.graphics.PorterDuff;
import androidx.annotation.ColorInt;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import co.paulburke.android.itemtouchhelperdemo.helper.ItemTouchHelperAdapter;
import co.paulburke.android.itemtouchhelperdemo.helper.SimpleItemTouchHelperCallback;

public class EditListItemTouchHelperCallback extends SimpleItemTouchHelperCallback {

    private boolean mDragging;
    private @ColorInt
    int mDragColor;

    public EditListItemTouchHelperCallback(ItemTouchHelperAdapter adapter, boolean enableLongPress, @ColorInt int dragColor) {
        super(adapter, 1, enableLongPress, false);
        mDragColor = dragColor;
    }

    public void setDragging(boolean dragging) {
        mDragging = dragging;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        // Set movement flags based on the layout manager
        if (viewHolder.getItemViewType() == VIEW_TYPE_HEADER) {
            return makeMovementFlags(0, 0);
        }
        final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        final int swipeFlags = 0;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        if (mDragging) {
            viewHolder.itemView.getBackground().mutate().setColorFilter(mDragColor, PorterDuff.Mode.MULTIPLY);
            viewHolder.itemView.getBackground().invalidateSelf();
        }
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        if (mDragging) {
            viewHolder.itemView.getBackground().setColorFilter(null);
            viewHolder.itemView.getBackground().invalidateSelf();
            mDragging = false;
        }
    }
}
