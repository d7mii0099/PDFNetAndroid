//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;

import java.io.File;

/**
 * A Dialog fragment for entering password
 */
public class PasswordDialogFragment extends DialogFragment {

    /**
     * Callback interface to be invoked when an interaction is needed.
     */
    public interface PasswordDialogFragmentListener {
        /**
         * Called when OK button has been clicked.
         *
         * @param fileType The file type
         * @param file     The file
         * @param path     The file path
         * @param password The entered password
         * @param id       The ID
         */
        void onPasswordDialogPositiveClick(int fileType, @Nullable File file, @Nullable String path, String password, @Nullable String id);

        /**
         * Called when Cancel button has been clicked.
         *
         * @param fileType The file type
         * @param file     The file
         * @param path     The file path
         */
        void onPasswordDialogNegativeClick(int fileType, @Nullable File file, @Nullable String path);

        /**
         * Called when dialog is dismissed
         *
         * @param forcedDismiss True if the dialog is forced to dismiss
         */
        void onPasswordDialogDismiss(boolean forcedDismiss);
    }

    private PasswordDialogFragmentListener mListener;
    protected File mFile;
    private boolean mForcedDismiss;
    protected boolean mDismissOnPositiveClicked = true;
    private String mPath;
    private int mFileType;
    private int mMessageId = -1;
    @Nullable
    private String mMessageString = null;
    private String mId;
    protected String mTitle;
    private boolean mAllowEmptyPassword = true;
    private boolean mRequireConfirmation = false;

    protected String mPassword;
    private String mPasswordConfirm;

    protected View mRoot;
    @Nullable
    protected TextInputEditText mPasswordEditText;
    @Nullable
    private TextInputEditText mConfirmEditText;
    @Nullable
    private MaterialButton mPositiveBtn;
    @Nullable
    private MaterialButton mNegativeBtn;
    @Nullable
    protected TextView mTitleTextView;
    @Nullable
    protected TextView mMessageTextView;
    @Nullable
    protected TextInputLayout mPasswordLayout;
    @Nullable
    private TextInputLayout mConfirmLayout;

    private static final String KEY_FILE = "key_file";
    private static final String KEY_FILETYPE = "key_filetype";
    private static final String KEY_PATH = "key_path";
    private static final String KEY_ID = "key_id";
    private static final String KEY_HINT = "key_hint";
    private static final String KEY_CONFIRMATION_HINT = "key_confirmation_hint";
    protected static final String KEY_POSITIVE_STRING_RES = "key_positive_string_res";
    private static final String KEY_ALLOW_EMPTY = "key_allow_empty";
    private static final String KEY_REQUIRE_CONFIRMATION = "key_require_confirmation";
    private static final String KEY_TITLE = "key_title";
    private static final String KEY_MESSAGE = "key_message";

    private Theme mTheme;

    /**
     * Class constructor
     */
    public PasswordDialogFragment() {

    }

    public static class Builder {
        private final Bundle bundle = new Bundle();

        public Builder setFileType(int fileType) {
            bundle.putInt(KEY_FILETYPE, fileType);
            return this;
        }

        public Builder setFile(File file) {
            bundle.putSerializable(KEY_FILE, file);
            return this;
        }

        public Builder setPath(String path) {
            bundle.putString(KEY_PATH, path);
            return this;
        }

        public Builder setId(String id) {
            bundle.putString(KEY_ID, id);
            return this;
        }

        public Builder setTitle(String title) {
            bundle.putString(KEY_TITLE, title);
            return this;
        }

        public Builder setHint(String hint) {
            bundle.putString(KEY_HINT, hint);
            return this;
        }

        public Builder setConfirmationHint(String hint) {
            bundle.putString(KEY_CONFIRMATION_HINT, hint);
            return this;
        }

        public Builder setPositiveStringRes(@StringRes int res) {
            bundle.putInt(KEY_POSITIVE_STRING_RES, res);
            return this;
        }

        public Builder setAllowEmptyPassword(boolean allowEmptyPassword) {
            bundle.putBoolean(KEY_ALLOW_EMPTY, allowEmptyPassword);
            return this;
        }

        public Builder setRequireConfirmation(boolean requireConfirmation) {
            bundle.putBoolean(KEY_REQUIRE_CONFIRMATION, requireConfirmation);
            return this;
        }

        public PasswordDialogFragment build() {
            PasswordDialogFragment fragment = new PasswordDialogFragment();
            fragment.setArguments(bundle);
            return fragment;
        }

        public Bundle getBundle() {
            return bundle;
        }

        public Builder setMessage(@Nullable  String message) {
            bundle.putString(KEY_MESSAGE, message);
            return this;
        }
    }

    /**
     * Returns a new instance of the class.
     */
    public static PasswordDialogFragment newInstance(int fileType, File file, String path, String id) {
        return new Builder().setFileType(fileType)
                .setFile(file)
                .setPath(path)
                .setId(id)
                .build();
    }

    /**
     * Returns a new instance of the class.
     */
    public static PasswordDialogFragment newInstance(int fileType, File file, String path, String id, String hint) {
        return new Builder().setFileType(fileType)
                .setFile(file)
                .setPath(path)
                .setId(id)
                .setHint(hint)
                .build();
    }

    /**
     * Sets {@link PasswordDialogFragmentListener} listener
     *
     * @param listener The listener
     */
    public void setListener(PasswordDialogFragmentListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Initialize member variables from the arguments
        Bundle args = getArguments();
        String hint = null;
        String confirmationHint = null;
        int positiveStringRes = R.string.ok;
        if (args != null) {
            if (args.containsKey(KEY_FILE)) {
                mFile = (File) args.getSerializable(KEY_FILE);
            }
            mFileType = args.getInt(KEY_FILETYPE);
            mPath = args.getString(KEY_PATH);
            mId = args.getString(KEY_ID);
            mTitle = args.getString(KEY_TITLE);
            mMessageString = args.getString(KEY_MESSAGE);
            hint = args.getString(KEY_HINT);
            confirmationHint = args.getString(KEY_CONFIRMATION_HINT);
            mAllowEmptyPassword = args.getBoolean(KEY_ALLOW_EMPTY, true);
            mRequireConfirmation = args.getBoolean(KEY_REQUIRE_CONFIRMATION, false);
            positiveStringRes = args.getInt(KEY_POSITIVE_STRING_RES, R.string.ok);
        }

        mTheme = Theme.fromContext(getActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        mRoot = inflater.inflate(R.layout.fragment_password_dialog, null);

        mPasswordLayout = mRoot.findViewById(R.id.password_layout);
        if (!Utils.isNullOrEmpty(hint)) {
            mPasswordLayout.setHint(hint);
        }
        mPasswordEditText = mRoot.findViewById(R.id.password_edit_text);
        if (mRequireConfirmation) {
            mPasswordEditText.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        } else {
            mPasswordEditText.setImeOptions(EditorInfo.IME_ACTION_GO);
        }
        if (mAllowEmptyPassword) {
            mPasswordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    // If enter key was pressed, then submit password
                    if (actionId == EditorInfo.IME_ACTION_GO) {
                        onPositiveClicked();
                        return true;
                    }
                    return false;
                }
            });

            mPasswordEditText.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        onPositiveClicked();
                        return true;
                    }
                    return false;
                }
            });
        }

        mConfirmLayout = mRoot.findViewById(R.id.password_confirm_layout);
        mConfirmLayout.setVisibility(mRequireConfirmation ? View.VISIBLE : View.GONE);

        mTitleTextView = mRoot.findViewById(R.id.title);
        if (mTitleTextView != null) {
            mTitleTextView.setText(getTitle(mTitleTextView.getContext()));
        }

        mMessageTextView = mRoot.findViewById(R.id.message);
        if (mMessageTextView != null) {
            if (mMessageId != -1) {
                mMessageTextView.setText(mMessageId);
                mMessageTextView.setVisibility(View.VISIBLE);
            } else if (mMessageString != null) {
                mMessageTextView.setText(mMessageString);
                mMessageTextView.setVisibility(View.VISIBLE);
            } else {
                mMessageTextView.setVisibility(View.GONE);
            }
        }

        mPositiveBtn = mRoot.findViewById(R.id.positive_btn);
        mPositiveBtn.setText(positiveStringRes);
        mPositiveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPositiveClicked();
            }
        });

        mNegativeBtn = mRoot.findViewById(R.id.negative_btn);
        mNegativeBtn.setText(R.string.cancel);
        mNegativeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PasswordDialogFragment.this.getDialog().cancel();
            }
        });

        builder.setView(mRoot);

        final AlertDialog alertDialog = builder.create();

        // Show keyboard automatically when the dialog is shown.
        mPasswordEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus && alertDialog.getWindow() != null) {
                    alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });

        if (!mAllowEmptyPassword) {
            mPasswordEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    mPassword = s.toString();
                    if (mPositiveBtn != null) {
                        mPositiveBtn.setEnabled(canSubmit());
                        updateButtonState();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (mRequireConfirmation) {
                        if (!canSubmit()) {
                            mConfirmLayout.setError(mConfirmLayout.getContext().getString(R.string.dialog_password_not_matching_warning));
                        } else {
                            mConfirmLayout.setError(null);
                        }
                    }
                }
            });
        }

        if (mRequireConfirmation) {
            mConfirmEditText = mRoot.findViewById(R.id.password_confirm_edit_text);
            if (!Utils.isNullOrEmpty(confirmationHint)) {
                mConfirmLayout.setHint(confirmationHint);
            }
            mConfirmEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    mPasswordConfirm = s.toString();
                    if (mPositiveBtn != null) {
                        mPositiveBtn.setEnabled(canSubmit());
                        updateButtonState();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (!canSubmit()) {
                        mConfirmLayout.setError(mConfirmLayout.getContext().getString(R.string.dialog_password_not_matching_warning));
                    } else {
                        mConfirmLayout.setError(null);
                    }
                }
            });
        }
        setThemeColors();
        return alertDialog;
    }

    private void onPositiveClicked() {
        if (mDismissOnPositiveClicked) {
            mForcedDismiss = true;
            if (PasswordDialogFragment.this.getDialog().isShowing()) {
                PasswordDialogFragment.this.getDialog().dismiss();
            }
        }
        if (null != mListener) {
            String password = mPasswordEditText.getText().toString().trim();
            mListener.onPasswordDialogPositiveClick(mFileType, mFile, mPath, password, mId);
        }
    }

    private boolean canSubmit() {
        if (mRequireConfirmation) {
            return !Utils.isNullOrEmpty(mPassword) && mPassword.equals(mPasswordConfirm);
        }
        if (!mAllowEmptyPassword) {
            return !Utils.isNullOrEmpty(mPassword);
        }
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            if (!mAllowEmptyPassword) {
                if (mPositiveBtn != null) {
                    mPositiveBtn.setEnabled(false);
                    updateButtonState();
                }
            }
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (null != mListener) {
            mListener.onPasswordDialogDismiss(mForcedDismiss);
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (null != mListener) {
            mListener.onPasswordDialogNegativeClick(mFileType, mFile, mPath);
        }
    }

    /**
     * Sets the message ID
     *
     * @param messageId The message ID
     */
    public void setMessage(int messageId) {
        this.mMessageId = messageId;
    }

    private void setThemeColors() {
        if (mTheme != null && getContext() != null) {
            if (mTitleTextView != null) {
                mTitleTextView.setTextColor(mTheme.titleColor);
            }
            if (mMessageTextView != null) {
                mMessageTextView.setTextColor(mTheme.messageColor);
            }
            if (mPasswordLayout != null) {
                mPasswordLayout.setEndIconTintList(ColorStateList.valueOf(mTheme.endIconColor));
                mPasswordLayout.setHintTextColor(ColorStateList.valueOf(mTheme.hintTextColor));
            }
            if (mConfirmLayout != null) {
                mConfirmLayout.setEndIconTintList(ColorStateList.valueOf(mTheme.endIconColor));
                mPasswordLayout.setHintTextColor(ColorStateList.valueOf(mTheme.hintTextColor));
            }
            if (mPasswordEditText != null) {
                mPasswordEditText.setTextColor(mTheme.inputTextColor);
            }
            if (mConfirmEditText != null) {
                mConfirmEditText.setTextColor(mTheme.inputTextColor);
            }
            updateButtonState();
        }
    }

    @NonNull
    protected String getTitle(@NonNull Context context) {
        if (mTitle == null) {
            return context.getString(R.string.dialog_password_title);
        } else {
            return mTitle;
        }
    }

    private void updateButtonState() {
        if (mTheme != null && getContext() != null) {
            if (mPositiveBtn != null) {
                int color = mPositiveBtn.isEnabled() ? Utils.getAccentColor(getContext()) : mTheme.buttonDisabledColor;
                mPositiveBtn.setBackgroundTintList(ColorStateList.valueOf(color));
            }
            if (mNegativeBtn != null) {
                int color = mNegativeBtn.isEnabled() ? Utils.getAccentColor(getContext()) : mTheme.buttonDisabledColor;
                mNegativeBtn.setStrokeColor(ColorStateList.valueOf(color));
                mNegativeBtn.setTextColor(color);
            }
        }

    }

    public static class Theme {
        @ColorInt
        public final int buttonDisabledColor;
        @ColorInt
        final int inputTextColor;
        @ColorInt
        final int hintTextColor;
        @ColorInt
        final int titleColor;
        @ColorInt
        final int messageColor;
        @ColorInt
        final int endIconColor;

        Theme(int buttonDisabledColor, int inputTextColor, int hintTextColor, int titleColor, int messageColor, int endIconColor) {
            this.buttonDisabledColor = buttonDisabledColor;
            this.inputTextColor = inputTextColor;
            this.hintTextColor = hintTextColor;
            this.titleColor = titleColor;
            this.messageColor = messageColor;
            this.endIconColor = endIconColor;
        }

        public static Theme fromContext(@NonNull Context context) {
            final TypedArray a = context.obtainStyledAttributes(
                    null, R.styleable.PasswordDialogTheme, R.attr.pt_password_dialog_style, R.style.PTPasswordDialogTheme);

            int buttonDisabledColor = a.getColor(R.styleable.PasswordDialogTheme_disabledButtonColor, Utils.getAccentColor(context));
            int inputTextColor = a.getColor(R.styleable.PasswordDialogTheme_inputTextColor, Utils.getPrimaryTextColor(context));
            int titleColor = a.getColor(R.styleable.PasswordDialogTheme_titleColor, Utils.getPrimaryTextColor(context));
            int hintTextColor = a.getColor(R.styleable.PasswordDialogTheme_hintTextColor, Utils.getAccentColor(context));
            int messageColor = a.getColor(R.styleable.PasswordDialogTheme_messageColor, Utils.getPrimaryTextColor(context));
            int endIconColor = a.getColor(R.styleable.PasswordDialogTheme_endIconColor, Utils.getSecondaryTextColor(context));
            a.recycle();

            return new Theme(buttonDisabledColor, inputTextColor, hintTextColor, titleColor, messageColor, endIconColor);
        }
    }
}
