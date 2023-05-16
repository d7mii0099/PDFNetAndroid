package com.pdftron.pdf.dialog.reflow;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.widget.bottombar.component.BottomBarTheme;

import java.util.ArrayList;
import java.util.List;

public class ReflowAnnotEditAdapter extends RecyclerView.Adapter<ReflowAnnotEditAdapter.ContentViewHolder> {

    private final List<ReflowAnnotEditItem> mItems = new ArrayList<>();

    public ReflowAnnotEditAdapter(List<ReflowAnnotEditItem> items) {
        mItems.addAll(items);
    }

    @NonNull
    @Override
    public ContentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ContentViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reflow_annot_edit, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ReflowAnnotEditAdapter.ContentViewHolder holder, int position) {
        ReflowAnnotEditItem item = mItems.get(position);
        holder.icon.setImageResource(item.getIcon());
        holder.title.setText(item.getTitle());
        BottomBarTheme theme = BottomBarTheme.fromContext(holder.icon.getContext());
        holder.icon.setColorFilter(new PorterDuffColorFilter(theme.iconColor, PorterDuff.Mode.SRC_ATOP));
        holder.title.setTextColor(theme.iconColor);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public static class ContentViewHolder extends RecyclerView.ViewHolder {

        public final AppCompatImageView icon;
        public final TextView title;

        public ContentViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            title = itemView.findViewById(R.id.title);
        }
    }
}
