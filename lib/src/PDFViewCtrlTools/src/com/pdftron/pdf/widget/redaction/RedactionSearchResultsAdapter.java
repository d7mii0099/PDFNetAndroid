package com.pdftron.pdf.widget.redaction;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.pdf.TextSearchResult;
import com.pdftron.pdf.controls.SearchResultsAdapter;
import com.pdftron.pdf.tools.R;

import java.util.ArrayList;
import java.util.HashSet;

public class RedactionSearchResultsAdapter extends SearchResultsAdapter {

    private final HashSet<Integer> mSelectedList = new HashSet<>();

    /**
     * Class constructor
     *
     * @param context  The context
     * @param resource The resource ID for a layout file containing a TextView to use when
     *                 instantiating views.
     * @param objects  The objects to represent in the ListView.
     * @param titles   The section titles
     */
    public RedactionSearchResultsAdapter(Context context, int resource, ArrayList<TextSearchResult> objects, ArrayList<String> titles) {
        super(context, resource, objects, titles);
    }

    @NonNull
    @Override
    public SearchResultsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(mLayoutResourceId, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder vh, final int position) {
        super.onBindViewHolder(vh, position);

        if (vh instanceof ViewHolder) {
            ViewHolder holder = (ViewHolder) vh;
            if (mSelectedList.contains(position)) {
                holder.mCheckBox.setChecked(true);
            } else {
                holder.mCheckBox.setChecked(false);
            }
        }
    }

    public void addSelected(int position) {
        mSelectedList.add(position);
        notifyItemChanged(position);
    }

    public void removeSelected(int position) {
        mSelectedList.remove(position);
        notifyItemChanged(position);
    }

    public boolean isSelected(int position) {
        return mSelectedList.contains(position);
    }

    public boolean isAllSelected() {
        return mSelectedList.size() == mResults.size();
    }

    public void selectAll() {
        mSelectedList.clear();
        for (int i = 0; i < mResults.size(); i++) {
            mSelectedList.add(i);
        }
        notifyDataSetChanged();
    }

    public void deselectAll() {
        mSelectedList.clear();
        notifyDataSetChanged();
    }

    @NonNull
    public HashSet<Integer> getSelected() {
        return mSelectedList;
    }

    static class ViewHolder extends SearchResultsAdapter.ViewHolder {
        CheckBox mCheckBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            mCheckBox = itemView.findViewById(R.id.checkbox);
        }
    }
}
