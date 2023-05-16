package com.pdftron.pdf.dialog.redaction;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.textfield.TextInputEditText;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.TextSearchResult;
import com.pdftron.pdf.controls.SearchResultsView;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.pdf.viewmodel.RedactionViewModel;
import com.pdftron.pdf.widget.redaction.RedactionSearchResultsView;

/**
 * Used for mark redaction annotation with search result.
 * A {@link PDFViewCtrl} must be supplied.
 */
public class SearchRedactionDialogFragment extends DialogFragment implements
        SearchResultsView.SearchResultsListener,
        RedactionSearchResultsView.RedactionSearchResultsListener,
        Toolbar.OnMenuItemClickListener {

    public static final String TAG = SearchRedactionDialogFragment.class.getName();

    public static SearchRedactionDialogFragment newInstance() {
        return new SearchRedactionDialogFragment();
    }

    private PDFViewCtrl mPdfViewCtrl;
    private RedactionSearchResultsView mSearchResultsView;
    private CheckBox mSelectAllCheckBox;

    private RedactionViewModel mRedactionViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_redact_by_search, null);

        FragmentActivity activity = getActivity();
        if (mPdfViewCtrl == null || activity == null) {
            return view;
        }
        mRedactionViewModel = ViewModelProviders.of(activity).get(RedactionViewModel.class);

        final Toolbar toolbar = view.findViewById(R.id.toolbar);
        final Toolbar searchToolbar = view.findViewById(R.id.search_toolbar);
        final TextInputEditText editText = view.findViewById(R.id.text_input);
        final View selectAllView = view.findViewById(R.id.select_all_view);
        mSelectAllCheckBox = view.findViewById(R.id.select_all_check_box);

        mSearchResultsView = view.findViewById(R.id.search_results_view);
        Button redactBtn = view.findViewById(R.id.redact_btn);

        // toolbar
        toolbar.setNavigationIcon(null);
        toolbar.setBackgroundColor(Utils.getBackgroundColor(activity));
        toolbar.setTitle(R.string.tools_qm_redact_by_search);
        toolbar.inflateMenu(R.menu.fragment_search_redaction_main);
        toolbar.setOnMenuItemClickListener(this);

        // search bar
        searchToolbar.inflateMenu(R.menu.fragment_search_redaction_search);
        searchToolbar.setOnMenuItemClickListener(this);
        editText.requestFocus();
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    mSearchResultsView.findText(v.getText().toString());
                    Utils.hideSoftKeyboard(v.getContext(), v);
                    return true;
                }
                return false;
            }
        });
        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && v instanceof EditText) {
                    mSearchResultsView.findText(((EditText) v).getText().toString());
                    Utils.hideSoftKeyboard(v.getContext(), v);
                    return true;
                }
                return false;
            }
        });

        // select all
        selectAllView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectAllCheckBox.isChecked()) {
                    mSearchResultsView.deselectAll();
                } else {
                    mSearchResultsView.selectAll();
                }
                mSelectAllCheckBox.setChecked(!mSelectAllCheckBox.isChecked());
            }
        });

        // search body
        mSearchResultsView.setPdfViewCtrl(mPdfViewCtrl);
        mSearchResultsView.setSearchResultsListener(this);
        mSearchResultsView.setRedactionSearchResultsListener(this);

        // button
        redactBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRedactionViewModel.onRedactBySearch(mSearchResultsView.getSelections());
                FragmentActivity activity = getActivity();
                if (null != activity) {
                    if (Utils.isLargeTablet(activity)) {
                        mRedactionViewModel.onRedactBySearchCloseClicked();
                    } else {
                        dismiss();
                    }
                }
            }
        });

        return view;
    }

    public void setPdfViewCtrl(@NonNull PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_close) {
            FragmentActivity activity = getActivity();
            if (null != activity) {
                if (Utils.isLargeTablet(activity)) {
                    mRedactionViewModel.onRedactBySearchCloseClicked();
                } else {
                    dismiss();
                }
            }
            return true;
        } else if (id == R.id.action_match_case) {
            boolean isChecked = item.isChecked();
            mSearchResultsView.setMatchCase(!isChecked);
            item.setChecked(!isChecked);
            return true;
        } else if (id == R.id.action_whole_word) {
            boolean isChecked = item.isChecked();
            mSearchResultsView.setWholeWord(!isChecked);
            item.setChecked(!isChecked);
            return true;
        }
        return false;
    }

    @Override
    public void onSearchResultClicked(TextSearchResult result) {
        FragmentActivity activity = getActivity();
        if (null != activity) {
            mPdfViewCtrl.setCurrentPage(result.getPageNum());
            if (Utils.isLargeTablet(activity)) {
                // also jump to the term
                Rect bbox = mSearchResultsView.getRectForResult(result);
                if (bbox != null) {
                    ViewerUtils.jumpToRect(mPdfViewCtrl, bbox, result.getPageNum());
                }
                mRedactionViewModel.onRedactBySearchItemClicked(result);
            }
            mSearchResultsView.toggleSelection();
            if (mSearchResultsView.isAllSelected()) {
                mSelectAllCheckBox.setChecked(true);
            } else {
                mSelectAllCheckBox.setChecked(false);
            }
        }
    }

    @Override
    public void onFullTextSearchStart() {

    }

    @Override
    public void onSearchResultFound(TextSearchResult result) {

    }

    @Override
    public void onRedactionSearchStart() {
        // a new search
        mSelectAllCheckBox.setChecked(false);
        mSearchResultsView.deselectAll();
    }
}
