//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.pdf.TextSearchResult;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link TextSearchResult} array adapter for showing search results
 */
public class SearchResultsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    protected int mLayoutResourceId;
    protected ArrayList<TextSearchResult> mResults;
    private ArrayList<String> mSectionTitles;
    private boolean mIsRtlMode;
    private SearchResultsView.Theme mTheme;

    private boolean mWholeWord;

    /**
     * Class constructor
     *
     * @param context  The context
     * @param resource The resource ID for a layout file containing a TextView to use when
     *                 instantiating views.
     * @param objects  The objects to represent in the ListView.
     * @param titles   The section titles
     */
    public SearchResultsAdapter(Context context, int resource, ArrayList<TextSearchResult> objects,
            ArrayList<String> titles) {
        mLayoutResourceId = resource;
        mResults = objects;
        mSectionTitles = titles;
        mIsRtlMode = false;
    }

    public void setTheme(SearchResultsView.Theme theme) {
        mTheme = theme;
    }

    /**
     * Sets whether to use whole world
     *
     * @param wholeWord true if whole word, false otherwise
     */
    public void setWholeWord(boolean wholeWord) {
        mWholeWord = wholeWord;
    }

    /**
     * @param position Position of the item whose data we want within the adapter's
     *                 search result set.
     * @return The search result at the specified position.
     * Gets the {@link TextSearchResult} item associated with the specified position in the search result set.
     */
    @Nullable
    public TextSearchResult getItem(int position) {
        if (mResults != null && position >= 0 && position < mResults.size()) {
            return mResults.get(position);
        }
        return null;
    }

    private SpannableStringBuilder formatResultText(Context context, TextSearchResult result) {
        String match = result.getResultStr();
        String ambient = result.getAmbientStr();

        // a temporarily hack on right-to-left scripts until the core will support RTL in search
        ambient = Utils.getBidiString(ambient);
        match = Utils.getBidiString(match);

        // Break ambient string into two parts: before and after the match
        int start = ambient.indexOf(match); // Get first occurrence of match in ambient
        int end = start + match.length(); // Get end of first occurrence

        if (mWholeWord) {
            // use regex to find whole word match
            String pattern = "\\b(" + match + ")\\b";
            Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(ambient);
            if (m.find()) {
                start = m.start(0);
                end = m.end(0);
            }
        }

        if (start < 0 || end > ambient.length() || start > end) {
            // Should never happen
            AnalyticsHandlerAdapter.getInstance().sendException(
                    new Exception("start/end of result text is invalid -> " + "match: " + match + ", ambient: " + ambient + ", start: " + start + "end:" + end));
            start = end = 0;
        }

        SpannableStringBuilder builder = new SpannableStringBuilder(ambient);
        builder.setSpan(new ForegroundColorSpan(mTheme.selectedTextForegroundColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new BackgroundColorSpan(mTheme.selectedTextBackgroundColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }

    private String formatSectionTitle(int position) {
        if (!mSectionTitles.isEmpty()) {
            String text = mSectionTitles.get(position);
            if (text != null) {
                return text;
            }
        }
        // Bookmark list is not set or section can't be determined
        return "";
    }

    void setRtlMode(boolean isRtlMode) {
        mIsRtlMode = isRtlMode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(mLayoutResourceId, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder vh, int position) {
        if (vh instanceof ViewHolder) {
            ViewHolder holder = (ViewHolder) vh;
            if (Utils.isJellyBeanMR1()) {
                holder.mSectionTitle.setTextDirection(View.TEXT_DIRECTION_LOCALE);
                holder.mPageNumber.setTextDirection(View.TEXT_DIRECTION_LOCALE);
                if (Utils.isRtlLayout(holder.mSectionTitle.getContext())) {
                    holder.mSectionTitle.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                    holder.mPageNumber.setTextDirection(View.TEXT_DIRECTION_LTR);
                } else {
                    holder.mSectionTitle.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                    holder.mPageNumber.setTextDirection(View.TEXT_DIRECTION_RTL);
                }
                if (mIsRtlMode) {
                    holder.mSearchText.setTextDirection(View.TEXT_DIRECTION_RTL);
                    holder.mSearchText.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                } else {
                    holder.mSearchText.setTextDirection(View.TEXT_DIRECTION_LTR);
                    holder.mSearchText.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                }
            }

            TextSearchResult result = getItem(position);
            if (result != null) {
                holder.mSearchText.setText(formatResultText(holder.mSearchText.getContext(), result));
                holder.mPageNumber.setText(holder.mPageNumber.getContext().getResources().getString(R.string.controls_annotation_dialog_page, result.getPageNum()));
                holder.mPageNumber.setTextColor(mTheme.pageNumTextColor);
                holder.mSectionTitle.setText(formatSectionTitle(position));
            }
        }
    }

    @Override
    public int getItemCount() {
        if (mResults != null) {
            return mResults.size();
        } else {
            return 0;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView mSectionTitle;
        TextView mPageNumber;
        TextView mSearchText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            mSectionTitle = itemView.findViewById(R.id.section_title);
            mPageNumber = itemView.findViewById(R.id.page_number);
            mSearchText = itemView.findViewById(R.id.search_text);
        }
    }
}
