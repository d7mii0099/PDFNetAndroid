package com.pdftron.pdf.widget.redaction;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.pdf.Rect;
import com.pdftron.pdf.TextSearchResult;
import com.pdftron.pdf.controls.SearchResultsAdapter;
import com.pdftron.pdf.controls.SearchResultsView;
import com.pdftron.pdf.tools.R;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Search result view used for redaction.
 */
public class RedactionSearchResultsView extends SearchResultsView {

    public interface RedactionSearchResultsListener {
        /**
         * Called when a new search has started.
         */
        void onRedactionSearchStart();
    }

    public RedactionSearchResultsView(Context context) {
        this(context, null);
    }

    public RedactionSearchResultsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RedactionSearchResultsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mFadeOnClickEnabled = false;
    }

    private RedactionSearchResultsListener mRedactionSearchResultsListener;

    /**
     * Sets the listener
     */
    public void setRedactionSearchResultsListener(RedactionSearchResultsListener listener) {
        this.mRedactionSearchResultsListener = listener;
    }

    @Override
    protected SearchResultsAdapter getAdapter() {
        return new RedactionSearchResultsAdapter(getContext(), R.layout.widget_redaction_search_results_list_item,
                mSearchResultList, mSectionTitleList);
    }

    @Override
    public void restartSearch() {
        super.restartSearch();

        if (mRedactionSearchResultsListener != null) {
            mRedactionSearchResultsListener.onRedactionSearchStart();
        }
    }

    public void toggleSelection() {
        if (mAdapter instanceof RedactionSearchResultsAdapter) {
            RedactionSearchResultsAdapter adapter = ((RedactionSearchResultsAdapter) mAdapter);
            if (adapter.isSelected(mCurrentResult)) {
                adapter.removeSelected(mCurrentResult);
            } else {
                adapter.addSelected(mCurrentResult);
            }
        }
    }

    public boolean isAllSelected() {
        if (mAdapter instanceof RedactionSearchResultsAdapter) {
            return ((RedactionSearchResultsAdapter) mAdapter).isAllSelected();
        }
        return false;
    }

    public void selectAll() {
        if (mAdapter instanceof RedactionSearchResultsAdapter) {
            ((RedactionSearchResultsAdapter) mAdapter).selectAll();
        }
    }

    public void deselectAll() {
        if (mAdapter instanceof RedactionSearchResultsAdapter) {
            ((RedactionSearchResultsAdapter) mAdapter).deselectAll();
        }
    }

    @NonNull
    public ArrayList<Pair<Integer, ArrayList<Double>>> getSelections() {
        ArrayList<Pair<Integer, ArrayList<Double>>> highlights = new ArrayList<>();
        if (mAdapter instanceof RedactionSearchResultsAdapter) {
            HashSet<Integer> selected = ((RedactionSearchResultsAdapter) mAdapter).getSelected();
            for (int position : selected) {
                TextSearchResult result = mSearchResultList.get(position);
                highlights.add(new Pair<>(result.getPageNum(), mSearchResultHighlightList.get(result)));
            }
        }
        return highlights;
    }

    @Nullable
    public Rect getRectForResult(@NonNull TextSearchResult result) {
        try {
            ArrayList<Double> quadList = mSearchResultHighlightList.get(result);
            if (quadList != null) {
                Double[] quads = quadList.toArray(new Double[0]);
                return new Rect(quads[0], quads[1], quads[4], quads[5]);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
