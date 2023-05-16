//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.dialog.pagelabel.PageLabelUtils;
import com.pdftron.pdf.tools.R;

/**
 * A dialog for going to a specific page.
 */
public class DialogGoToPage {

    public interface DialogGoToPageListener {
        void onPageSet(int pageNum);
    }

    private EditText mEditTextBox;
    private PDFViewCtrl mPdfViewCtrl;
    private Context mContext;
    private String mHint;
    private int mPageCount;

    private boolean mUsingLabel;

    private DialogGoToPageListener mDialogGoToPageListener;

    public DialogGoToPage(@NonNull Context context, @NonNull PDFViewCtrl ctrl, @Nullable DialogGoToPageListener listener) {
        mPdfViewCtrl = ctrl;
        mContext = context;
        mPageCount = 0;
        mHint = "";
        mDialogGoToPageListener = listener;
        PDFDoc doc = mPdfViewCtrl.getDoc();
        if (doc != null) {
            boolean shouldUnlockRead = false;
            try {
                doc.lockRead();
                shouldUnlockRead = true;
                mPageCount = doc.getPageCount();
                if (mPageCount > 0) {
                    mHint = String.format(context.getResources().getString(R.string.dialog_gotopage_number), 1, mPageCount);
                    String firstPage = PageLabelUtils.getPageLabelTitle(mPdfViewCtrl, 1);
                    String lastPage = PageLabelUtils.getPageLabelTitle(mPdfViewCtrl, mPageCount);
                    if (!Utils.isNullOrEmpty(firstPage) && !Utils.isNullOrEmpty(lastPage)) {
                        mHint = String.format(context.getResources().getString(R.string.dialog_gotopage_label), firstPage, lastPage);
                        mUsingLabel = true;
                    }
                }
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            } finally {
                if (shouldUnlockRead) {
                    Utils.unlockReadQuietly(doc);
                }
            }
        }
    }

    /**
     * Shows the dialog.
     */
    public void show() {
        show(R.string.dialog_gotopage_title, R.string.ok, null);
    }

    /**
     * Shows the dialog.
     */
    public void show(@StringRes int titleRes, @StringRes int posRes, @Nullable String initialValue) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(titleRes);
        mEditTextBox = new EditText(mContext);
        if (mPageCount > 0) {
            mEditTextBox.setHint(mHint);
        }
        if (!mUsingLabel) {
            mEditTextBox.setInputType(InputType.TYPE_CLASS_NUMBER);
        }
        mEditTextBox.setImeOptions(EditorInfo.IME_ACTION_GO);
        mEditTextBox.setFocusable(true);
        mEditTextBox.setSingleLine();
        if (initialValue != null) {
            mEditTextBox.setText(initialValue);
            mEditTextBox.setSelection(mEditTextBox.getText().length());
        }
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(-1, -1);
        mEditTextBox.setLayoutParams(layoutParams);
        FrameLayout layout = new FrameLayout(mPdfViewCtrl.getContext());
        layout.addView(mEditTextBox);
        int topPadding = mContext.getResources().getDimensionPixelSize(R.dimen.alert_dialog_top_padding);
        int sidePadding = mContext.getResources().getDimensionPixelSize(R.dimen.alert_dialog_side_padding);
        layout.setPadding(sidePadding, topPadding, sidePadding, topPadding);
        builder.setView(layout);

        builder.setPositiveButton(mContext.getResources().getString(posRes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing here because we override this button later
                // to change the close behaviour. However, we still need
                // this because on older versions of Android unless we pass
                // a handler the button doesn't get instantiated.
            }
        });
        builder.setNegativeButton(mContext.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                mEditTextBox.requestFocus();
                Utils.showSoftKeyboard(mEditTextBox.getContext(), mEditTextBox);
            }
        });
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    goToPage(dialog);
                }
            });

        mEditTextBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                // If enter key was pressed, then submit password
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    goToPage(dialog);
                    return true;
                }
                return false;
            }
        });
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private void goToPage(AlertDialog dialog) {
        if (mPdfViewCtrl == null) {
            return;
        }

        String text = mEditTextBox.getText().toString();
        int pageNum;
        try {
            pageNum = Integer.parseInt(text);
        } catch (NumberFormatException nfe) {
            pageNum = 0;
        }
        int pageLabelNum = PageLabelUtils.getPageNumberFromLabel(mPdfViewCtrl, text);
        if (pageLabelNum > 0) {
            pageNum = pageLabelNum;
        } else if (pageNum > 0) {
            // try if it is still a valid page number greater than the page count
            try {
                String lastPage = PageLabelUtils.getPageLabelTitle(mPdfViewCtrl, mPageCount);
                if (!Utils.isNullOrEmpty(lastPage)) {
                    int lastPageNum = Integer.parseInt(lastPage);
                    if (pageNum > lastPageNum) {
                        pageNum = mPageCount;
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }
        if (pageNum > 0 && pageNum <= mPageCount) {
            if (mDialogGoToPageListener != null) {
                mDialogGoToPageListener.onPageSet(pageNum);
            }

            if (null != dialog) {
                dialog.dismiss();
            }
        } else {
            mEditTextBox.setText("");
        }
    }
}
