package com.pdftron.demo.navigation.adapter;

import android.content.Context;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.demo.navigation.adapter.viewholder.ContentViewHolder;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.widget.recyclerview.ViewHolderBindListener;

import java.util.ArrayList;

public class MergeAdapter extends FavoriteAdapter {

    private boolean mDragAndDropEnabled = true;

    public MergeAdapter(Context context, ArrayList<FileInfo> objects, Object objectsLock, int spanCount, AdapterListener adapterListener, ViewHolderBindListener bindListener) {
        super(context, objects, objectsLock, spanCount, adapterListener, bindListener);
        setShowInfoButton(false);
    }

    public void setDragAndDropEnabled(boolean enabled) {
        mDragAndDropEnabled = enabled;
    }

    @Override
    public void onBindViewHolderContent(RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolderContent(holder, position);

        ContentViewHolder contentViewHolder = (ContentViewHolder) holder;
        if (contentViewHolder.dragButton != null) {
            contentViewHolder.dragButton.setVisibility(mDragAndDropEnabled ? View.VISIBLE : View.GONE);
        }
    }
}
