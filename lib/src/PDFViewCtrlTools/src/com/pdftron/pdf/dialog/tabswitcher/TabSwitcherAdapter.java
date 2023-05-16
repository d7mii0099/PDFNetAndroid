package com.pdftron.pdf.dialog.tabswitcher;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.pdftron.pdf.dialog.tabswitcher.model.TabSwitcherItem;
import com.pdftron.pdf.tools.R;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

public class TabSwitcherAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final ArrayList<TabSwitcherItem> mTabSwitcherItems = new ArrayList<>();
    private TabSwitcherViewModel mViewModel;
    private String mSelectedTabTag;
    private TabSwitcherDialogFragment.Theme mTheme;

    public TabSwitcherAdapter(@NonNull Context context) {
        mTheme = TabSwitcherDialogFragment.Theme.fromContext(context);
    }

    public void setData(@NonNull ArrayList<TabSwitcherItem> menuEditorItems) {
        mTabSwitcherItems.clear();
        mTabSwitcherItems.addAll(menuEditorItems);
        notifyDataSetChanged();
    }

    public void setViewModel(TabSwitcherViewModel viewModel) {
        mViewModel = viewModel;
    }

    public void setSelectedTab(@NonNull String tabTag) {
        mSelectedTabTag = tabTag;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tab_switcher_content, parent, false);
        return new ContentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        final TabSwitcherItem item = mTabSwitcherItems.get(position);
        ContentViewHolder viewHolder = (ContentViewHolder) holder;
        viewHolder.mTabTitle.setText(item.getTitle());

        if (item.getPreviewPath() != null) {
            File previewFile = new File(item.getPreviewPath());
            if (previewFile.exists()) {
                Picasso.get()
                        .load(previewFile)
                        .into(viewHolder.mTabPreview);
            }
        }
        // only show close button for selected tab
        // show border for selected tab
        if (mSelectedTabTag != null && mSelectedTabTag.equals(item.getTabTag())) {
            viewHolder.mCardView.setStrokeColor(mTheme.selectedBorderColor);
        } else {
            viewHolder.mCardView.setStrokeColor(Color.TRANSPARENT);
        }
        viewHolder.mCloseIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mViewModel != null) {
                    mViewModel.onCloseTab(item.getTabTag());
                }
                if (position >= 0 && position < mTabSwitcherItems.size()) {
                    mTabSwitcherItems.remove(position);
                    notifyItemRemoved(position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mTabSwitcherItems.size();
    }

    public TabSwitcherItem getItem(int position) {
        return mTabSwitcherItems.get(position);
    }

    private class ContentViewHolder extends RecyclerView.ViewHolder {

        MaterialCardView mCardView;
        TextView mTabTitle;
        AppCompatImageView mCloseIcon;
        AppCompatImageView mTabPreview;

        ContentViewHolder(@NonNull View itemView) {
            super(itemView);
            mCardView = itemView.findViewById(R.id.card_view);
            mTabTitle = itemView.findViewById(R.id.tab_title);
            mCloseIcon = itemView.findViewById(R.id.close_btn);
            mTabPreview = itemView.findViewById(R.id.tab_preview);

            mCloseIcon.setColorFilter(mTheme.iconColor, PorterDuff.Mode.SRC_IN);
            mCardView.setBackgroundColor(mTheme.backgrounColor);
            mTabTitle.setTextColor(mTheme.textColor);
        }
    }
}
