package com.pdftron.pdf.dialog.redaction;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.viewmodel.RedactionViewModel;

import java.util.ArrayList;

/**
 * Used to mark redaction annotation with page bounding box.
 */
public class PageRedactionDialogFragment extends DialogFragment {
    public static final String TAG = PageRedactionDialogFragment.class.getName();

    private static final String CURRENT_PAGE = "RedactByPageDialog_Initial_currentpage";
    private static final String FROM_PAGE = "RedactByPageDialog_Initial_frompage";
    private static final String TO_PAGE = "RedactByPageDialog_Initial_topage";
    private static final String MAX_PAGE = "RedactByPageDialog_Initial_maxpage";

    private RadioButton mCurrentPageBtn;
    private RadioButton mPageRangeBtn;
    private EditText mFromPageEditText;
    private EditText mToPageEditText;

    private int mCurrentPage;
    private int mPageCount;

    private int mFromPage;
    private int mToPage;

    public static PageRedactionDialogFragment newInstance(int currentPage, int maxPage) {
        return newInstance(currentPage, 0, 0, maxPage);
    }

    public static PageRedactionDialogFragment newInstance(int currentPage, int fromPage, int toPage, int maxPage) {
        PageRedactionDialogFragment fragment = new PageRedactionDialogFragment();
        Bundle args = new Bundle();
        args.putInt(CURRENT_PAGE, currentPage);
        args.putInt(FROM_PAGE, fromPage);
        args.putInt(TO_PAGE, toPage);
        args.putInt(MAX_PAGE, maxPage);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        mCurrentPage = 1;
        mFromPage = 0;
        mToPage = 0;

        Bundle args = getArguments();
        if (args != null) {
            mCurrentPage = args.getInt(CURRENT_PAGE);
            mFromPage = args.getInt(FROM_PAGE);
            mToPage = args.getInt(TO_PAGE);
            mPageCount = args.getInt(MAX_PAGE);
        }

        View view = activity.getLayoutInflater().inflate(R.layout.dialog_redact_by_page, null);

        mCurrentPageBtn = view.findViewById(R.id.radio_pages_current);
        mPageRangeBtn = view.findViewById(R.id.radio_pages_range);
        mFromPageEditText = view.findViewById(R.id.page_range_from_edittext);
        mToPageEditText = view.findViewById(R.id.page_range_to_edittext);
        TextView maxPageEditText = view.findViewById(R.id.page_range_max);

        if (mFromPage > 0 && mToPage > 0) {
            mPageRangeBtn.setChecked(true);
            setPageRangeEnabled(true);
        } else {
            mCurrentPageBtn.setChecked(true);
            setPageRangeEnabled(false);
            mFromPage = mToPage = mCurrentPage;
        }

        String currentPageStr = String.format(
                view.getContext().getResources().getString(R.string.redact_by_page_current), mCurrentPage);
        mCurrentPageBtn.setText(currentPageStr);
        mFromPageEditText.setText(String.valueOf(mFromPage));
        mToPageEditText.setText(String.valueOf(mToPage));

        String numPages = String.format(
                view.getContext().getResources().getString(R.string.page_label_max_page), mPageCount);
        maxPageEditText.setText(numPages);

        checkPageRange();

        // Setup listeners for radio buttons
        mCurrentPageBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mPageRangeBtn.setChecked(false);
                    setPageRangeEnabled(false);
                }
                checkPageRange();
            }
        });

        mPageRangeBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mCurrentPageBtn.setChecked(false);
                    setPageRangeEnabled(true);
                }
                checkPageRange();
            }
        });

        mFromPageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkPageRange();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mToPageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkPageRange();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view)
                .setTitle(R.string.redact_by_page_title)
                .setPositiveButton(R.string.mark_redaction, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onComplete();
                        dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                });
        return builder.create();
    }

    private void setPositiveButtonEnabled(boolean enabled) {
        AlertDialog dialog = (AlertDialog) getDialog();
        Button posButton = dialog == null ? null : dialog.getButton(Dialog.BUTTON_POSITIVE);
        if (posButton != null) {
            posButton.setEnabled(enabled);
        }
    }

    private void setPageRangeEnabled(boolean enabled) {
        mFromPageEditText.setEnabled(enabled);
        mToPageEditText.setEnabled(enabled);
    }

    private void checkPageRange() {
        if (mCurrentPageBtn.isChecked()) {
            setPositiveButtonEnabled(true);
            return;
        }
        String fromText = mFromPageEditText.getEditableText().toString();
        String toText = mToPageEditText.getEditableText().toString();

        try {
            mFromPage = Integer.parseInt(fromText);
            mToPage = Integer.parseInt(toText);

            // Check if from and to page is correct
            boolean isFromPageCorrect = mFromPage <= mToPage && mFromPage >= 1 && mFromPage <= mPageCount;
            boolean isToPageCorrect = mFromPage <= mToPage && mToPage >= 1 && mToPage <= mPageCount;

            String invalidPageRange = mFromPageEditText.getContext().getString(R.string.page_label_invalid_range);

            mFromPageEditText.setError(isFromPageCorrect ? null : invalidPageRange);
            mToPageEditText.setError(isToPageCorrect ? null : invalidPageRange);

            if (isFromPageCorrect && isToPageCorrect) {
                setPositiveButtonEnabled(true);
            } else {
                setPositiveButtonEnabled(false);
            }
        } catch (NumberFormatException e) {
            setPositiveButtonEnabled(false);
        }
    }

    private void onComplete() {
        boolean currentChecked = mCurrentPageBtn.isChecked();
        boolean rangedChecked = mPageRangeBtn.isChecked();

        FragmentActivity activity = getActivity();
        if (null == activity) {
            throw new RuntimeException("Not attached to a valid activity");
        }
        final RedactionViewModel redactionViewModel = ViewModelProviders.of(activity).get(RedactionViewModel.class);

        ArrayList<Integer> pages = new ArrayList<>();
        if (currentChecked) {
            pages.add(mCurrentPage);
        } else if (rangedChecked) {
            for (int i = mFromPage; i <= mToPage; i++) {
                pages.add(i);
            }
        }
        redactionViewModel.onRedactByPage(pages);
    }
}
