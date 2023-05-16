package com.pdftron.pdf.controls;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.TextHighlighter;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.Utils;

public class FindTextOverlay extends ConstraintLayout implements
        PDFViewCtrl.TextSearchListener {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    static final class Theme {
        @ColorInt
        final int backgroundColor;
        @ColorInt
        final int iconColor;

        public Theme(int backgroundColor, int iconColor) {
            this.backgroundColor = backgroundColor;
            this.iconColor = iconColor;
        }

        public static Theme fromContext(@NonNull Context context) {
            final TypedArray a = context.obtainStyledAttributes(
                    null, R.styleable.FloatingNavTheme, R.attr.pt_floating_nav_style, R.style.DefaultFloatingButtonNavStyle);
            int backgroundColor = a.getColor(R.styleable.FloatingNavTheme_backgroundTint, context.getResources().getColor(R.color.pt_background_color));
            int iconColor = a.getColor(R.styleable.FloatingNavTheme_iconTint, context.getResources().getColor(R.color.pt_secondary_color));
            a.recycle();

            return new Theme(backgroundColor, iconColor);
        }
    }

    public interface FindTextOverlayListener {

        /**
         * Called when next icon clicked.
         *
         * @param useFullTextResults
         */
        void onGotoNextSearch(boolean useFullTextResults);

        /**
         * Called when previous icon clicked.
         *
         * @param useFullTextResults
         */
        void onGotoPreviousSearch(boolean useFullTextResults);

        /**
         * The implementation should show the search progress.
         */
        void onSearchProgressShow();

        /**
         * The implementation should hide the search progress.
         */
        void onSearchProgressHide();
    }

    private FindTextOverlayListener mFindTextOverlayListener;

    public void setFindTextOverlayListener(FindTextOverlayListener listener) {
        mFindTextOverlayListener = listener;
    }

    protected FloatingActionButton mButtonSearchNext;
    protected FloatingActionButton mButtonSearchPrev;

    protected PDFViewCtrl mPdfViewCtrl;

    protected boolean mSearchMatchCase;
    protected boolean mSearchWholeWord;
    protected boolean mSearchUp;
    protected String mSearchQuery = "";
    protected boolean mSearchSettingsChanged;
    protected boolean mUseFullTextResults;

    protected int mNumSearchRunning;
    protected boolean mShowSearchCancelMessage = true;

    public FindTextOverlay(Context context) {
        this(context, null);
    }

    public FindTextOverlay(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FindTextOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.controls_find_text_overlay, this);

        // Search prev/next buttons.
        mButtonSearchNext = view.findViewById(R.id.search_button_next);
        mButtonSearchNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoNextSearch();
            }
        });
        mButtonSearchPrev = view.findViewById(R.id.search_button_prev);
        mButtonSearchPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoPreviousSearch();
            }
        });

        if (Utils.isRtlLayout(context)) {
            mButtonSearchNext.setImageResource(R.drawable.ic_keyboard_arrow_left_white_24dp);
            mButtonSearchPrev.setImageResource(R.drawable.ic_keyboard_arrow_right_white_24dp);
        }
        Theme theme = Theme.fromContext(context);
        mButtonSearchNext.setBackgroundColor(theme.backgroundColor);
        mButtonSearchNext.setColorFilter(theme.iconColor);
        mButtonSearchPrev.setBackgroundColor(theme.backgroundColor);
        mButtonSearchPrev.setColorFilter(theme.iconColor);
    }

    /**
     * Sets the PDFViewCtrl
     *
     * @param pdfViewCtrl the PDFViewCtrl
     */
    public void setPdfViewCtrl(@NonNull PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;

        mPdfViewCtrl.addTextSearchListener(this);
    }

    /**
     * Goes to the next text in search.
     */
    public void gotoNextSearch() {
        if (mPdfViewCtrl == null) {
            return;
        }

        mSearchUp = false;
        if (mFindTextOverlayListener != null) {
            mFindTextOverlayListener.onGotoNextSearch(mUseFullTextResults);
        } else {
            findText();
        }
        highlightSearchResults();
    }

    /**
     * Goes to the previous text in search.
     */
    public void gotoPreviousSearch() {
        if (mPdfViewCtrl == null) {
            return;
        }

        mSearchUp = true;
        if (mFindTextOverlayListener != null) {
            mFindTextOverlayListener.onGotoPreviousSearch(mUseFullTextResults);
        } else {
            findText();
        }

        highlightSearchResults();
    }

    /**
     * Specifies the search query.
     *
     * @param text The search query
     */
    public void setSearchQuery(String text) {
        // If the search query has actually changed, stop using full text search results
        if (mSearchQuery != null && !mSearchQuery.equals(text)) {
            mUseFullTextResults = false;
        }
        mSearchQuery = text;
    }

    /**
     * Sets the search rule for match case.
     *
     * @param matchCase True if match case is enabled
     */
    public void setSearchMatchCase(boolean matchCase) {
        setSearchSettings(matchCase, mSearchWholeWord);
    }

    /**
     * Sets the search rule for whole word.
     *
     * @param wholeWord True if whole word is enabled
     */
    public void setSearchWholeWord(boolean wholeWord) {
        setSearchSettings(mSearchMatchCase, wholeWord);
    }

    /**
     * Sets the search rules for match case and whole word.
     *
     * @param matchCase True if match case is enabled
     * @param wholeWord True if whole word is enabled
     */
    public void setSearchSettings(boolean matchCase, boolean wholeWord) {
        mSearchMatchCase = matchCase;
        mSearchWholeWord = wholeWord;
        mSearchSettingsChanged = true;
    }

    /**
     * Resets full text results.
     */
    public void resetFullTextResults() {
        mUseFullTextResults = false;
        highlightSearchResults();
    }

    /**
     * Submits the query text.
     *
     * @param text The query text
     */
    public void queryTextSubmit(String text) {
        mSearchQuery = text;
        findText();
        highlightSearchResults();
    }

    /**
     * Submits the query text. Starting from a specific page
     *
     * @param text         The query text
     * @param startPageNum The starting page for the search
     */
    public void queryTextSubmit(String text, int startPageNum) {
        mSearchQuery = text;
        findText(startPageNum);
        highlightSearchResults();
    }

    public void findText() {
        findText(-1);
    }

    public void findText(int pageNum) {
        if (mPdfViewCtrl != null && mSearchQuery != null && mSearchQuery.trim().length() > 0) {
            mUseFullTextResults = false;
            mPdfViewCtrl.findText(mSearchQuery, mSearchMatchCase, mSearchWholeWord, mSearchUp, false, pageNum);
        }
    }

    /**
     * Starts the TextHighlighter tool.
     */
    public void highlightSearchResults() {
        if (mPdfViewCtrl == null) {
            return;
        }
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        String prevPattern = null;
        if (toolManager.getTool() instanceof TextHighlighter) { // Get previous pattern, if highlighter is already running
            TextHighlighter prevTool = (TextHighlighter) toolManager.getTool();
            prevPattern = prevTool.getSearchPattern();
        }
        // Restart text-highlighter only if query changed from last time, if applicable
        // NOTE: if the highlighter is not running, it will always be started here
        if (prevPattern == null || !mSearchQuery.equals(prevPattern) || mSearchSettingsChanged) {
            if (mSearchQuery.trim().length() > 0) {
                ToolManager.Tool tool = toolManager.createTool(ToolManager.ToolMode.TEXT_HIGHLIGHTER, null);
                if (tool instanceof TextHighlighter) {
                    TextHighlighter highlighter = (TextHighlighter) tool;
                    toolManager.setTool(highlighter);
                    highlighter.start(mSearchQuery, mSearchMatchCase, mSearchWholeWord, false);
                }
            }

            mSearchSettingsChanged = false;
        }
    }

    /**
     * Exits the search mode.
     */
    public void exitSearchMode() {
        if (mPdfViewCtrl == null) {
            return;
        }
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();

        if (toolManager.getTool() instanceof TextHighlighter) {
            TextHighlighter highlighter = (TextHighlighter) toolManager.getTool();
            mPdfViewCtrl.clearSelection();
            highlighter.clear();
            mPdfViewCtrl.invalidate();
        }
        toolManager.setTool(toolManager.createTool(ToolManager.ToolMode.PAN, null));
        //mPdfViewCtrl.requestFocus(); // Hide soft keyboard
        // Full doc. text search will be stopped by host fragment, so use findTextAsync for next search
        mUseFullTextResults = false;
    }

    /**
     * Highlights the results of full text search.
     *
     * @param result The {@link com.pdftron.pdf.TextSearchResult}
     */
    public void highlightFullTextSearchResult(com.pdftron.pdf.TextSearchResult result) {
        if (mPdfViewCtrl == null) {
            return;
        }
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();

        if (result.getCode() == com.pdftron.pdf.TextSearchResult.e_found) {
            mPdfViewCtrl.requestFocus(); // Removes focus from the SearchView, so the soft keyboard is not shown.
            mShowSearchCancelMessage = false;
            cancelFindText();
            mPdfViewCtrl.selectAndJumpWithHighlights(result.getHighlights());

            if (toolManager.getTool() instanceof TextHighlighter) {
                TextHighlighter highlighter = (TextHighlighter) toolManager.getTool();
                highlighter.update();
                highlighter.highlightSelection();
            }

            mUseFullTextResults = true;
        }
    }

    /**
     * Cancels finding text.
     */
    public void cancelFindText() {
        if (mPdfViewCtrl != null) {
            mPdfViewCtrl.cancelFindText();
        }
    }

    /**
     * Handles when {@link com.pdftron.pdf.PDFViewCtrl} starts to search text.
     */
    @Override
    public void onTextSearchStart() {
        mNumSearchRunning++;
        if (mFindTextOverlayListener != null) {
            mFindTextOverlayListener.onSearchProgressShow();
        }
    }

    /**
     * Handles when {@link com.pdftron.pdf.PDFViewCtrl} has progress on search text.
     *
     * @param progress progress indicator in the range [0, 100]
     */
    @Override
    public void onTextSearchProgress(int progress) {

    }

    /**
     * Handles when {@link com.pdftron.pdf.PDFViewCtrl} has progress on search text.
     *
     * @param result search result.
     */
    @Override
    public void onTextSearchEnd(PDFViewCtrl.TextSearchResult result) {
        if (mPdfViewCtrl == null) {
            return;
        }
        mNumSearchRunning--;
        if (mPdfViewCtrl != null) {
            mPdfViewCtrl.requestFocus();
        }

        if (mFindTextOverlayListener != null) {
            mFindTextOverlayListener.onSearchProgressHide();
            if (mNumSearchRunning > 0) { // Re-show progress, after delay
                mFindTextOverlayListener.onSearchProgressShow();
            }
        }
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();

        switch (result) {
            case NOT_FOUND:
                CommonToast.showText(getContext(), getContext().getString(R.string.search_results_none), Toast.LENGTH_SHORT, Gravity.CENTER, 0, 0);
                break;
            case FOUND:
                if (toolManager.getTool() instanceof TextHighlighter) {
                    TextHighlighter highlighter = (TextHighlighter) toolManager.getTool();
                    highlighter.update();
                    highlighter.highlightSelection();
                }
                break;
            case CANCELED:
                // If search is cancelled, we do not need to show any toasts or dialogs
                // to the user because user should not need to care about these events
                break;
            case INVALID_INPUT:
                CommonToast.showText(getContext(), getContext().getString(R.string.search_results_invalid), Toast.LENGTH_SHORT, Gravity.CENTER, 0, 0);
                break;
        }
        mShowSearchCancelMessage = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mPdfViewCtrl != null) {
            mPdfViewCtrl.removeTextSearchListener(this);
        }
    }
}
