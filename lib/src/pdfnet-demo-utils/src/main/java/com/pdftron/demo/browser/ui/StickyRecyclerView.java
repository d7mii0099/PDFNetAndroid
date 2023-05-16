package com.pdftron.demo.browser.ui;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;

public class StickyRecyclerView extends RecyclerView {

    public StickyRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public StickyRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StickyRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setLayoutManager(@Nullable LayoutManager layout) {
        super.setLayoutManager(layout);
        if (layout instanceof StickLinearLayoutManager) {
            ((StickLinearLayoutManager) layout).mStickyHeader.setRecyclerView(this);
        }
    }

    static class StickLinearLayoutManager extends LinearLayoutManager {
        @NonNull
        private final StickyHeader mStickyHeader;

        StickLinearLayoutManager(Context context, @NonNull StickyHeader stickyHeader) {
            super(context);
            mStickyHeader = stickyHeader;
        }

        @Override
        public void onLayoutCompleted(State state) {
            super.onLayoutCompleted(state);

            if (findFirstVisibleItemPosition() >= 0
                    && findLastVisibleItemPosition() > findFirstVisibleItemPosition()
                    && mStickyHeader != null && mStickyHeader.isDisabled()) {
                mStickyHeader.enable(findFirstVisibleItemPosition());
            }
        }
    }
}
