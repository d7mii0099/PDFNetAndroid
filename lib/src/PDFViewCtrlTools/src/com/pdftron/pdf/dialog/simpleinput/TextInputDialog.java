package com.pdftron.pdf.dialog.simpleinput;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.FixedKeyboardEditText;

public class TextInputDialog extends DialogFragment {
    public static final String TAG = TextInputDialog.class.getName();

    public static final String REQUEST_CODE = "TextInputDialog_requestcode";
    public static final String TITLE_RES = "TextInputDialog_titleres";
    public static final String HINT_RES = "TextInputDialog_hintres";
    public static final String POSITIVE_BUTTON_RES = "TextInputDialog_positivebtnres";
    public static final String NEGATIVE_BUTTON_RES = "TextInputDialog_negativebtnres";
    public static final String INITIAL_INPUT = "TextInputDialog_initial_input";
    public static final String BUNDLE_EXTRA = "TextInputDialog_extra";

    private int mRequestCode;
    private @StringRes
    int mTitleRes;
    private @StringRes
    int mHintRes;
    private @StringRes
    int mPosRes;
    private @StringRes
    int mNegRes;
    @Nullable
    private Bundle mExtra;
    @Nullable
    private String mInitialInput;

    private TextInputViewModel mViewModel;

    public static TextInputDialog newInstance(int requestCode,
            @StringRes int titleRes, @StringRes int hintRes,
            @StringRes int posRes, @StringRes int negRes) {
        return newInstance(requestCode, titleRes,
                hintRes, posRes, negRes, null);
    }

    public static TextInputDialog newInstance(int requestCode,
            @StringRes int titleRes, @StringRes int hintRes,
            @StringRes int posRes, @StringRes int negRes,
            @Nullable Bundle extra) {
        TextInputDialog dialog = new TextInputDialog();
        Bundle args = new Bundle();
        args.putInt(REQUEST_CODE, requestCode);
        args.putInt(TITLE_RES, titleRes);
        args.putInt(HINT_RES, hintRes);
        args.putInt(POSITIVE_BUTTON_RES, posRes);
        args.putInt(NEGATIVE_BUTTON_RES, negRes);
        if (extra != null) {
            args.putBundle(BUNDLE_EXTRA, extra);
        }
        dialog.setArguments(args);
        return dialog;
    }

    public static TextInputDialog newInstance(int requestCode,
            @StringRes int titleRes, @StringRes int hintRes,
            @StringRes int posRes, @StringRes int negRes,
            @Nullable Bundle extra, @Nullable String initialInput) {
        TextInputDialog dialog = new TextInputDialog();
        Bundle args = new Bundle();
        args.putInt(REQUEST_CODE, requestCode);
        args.putInt(TITLE_RES, titleRes);
        args.putInt(HINT_RES, hintRes);
        args.putInt(POSITIVE_BUTTON_RES, posRes);
        args.putInt(NEGATIVE_BUTTON_RES, negRes);
        if (initialInput != null) {
            args.putString(INITIAL_INPUT, initialInput);
        }
        if (extra != null) {
            args.putBundle(BUNDLE_EXTRA, extra);
        }
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mRequestCode = args.getInt(REQUEST_CODE);
            mTitleRes = args.getInt(TITLE_RES);
            mHintRes = args.getInt(HINT_RES);
            mPosRes = args.getInt(POSITIVE_BUTTON_RES);
            mNegRes = args.getInt(NEGATIVE_BUTTON_RES);
            mExtra = args.getBundle(BUNDLE_EXTRA);
            mInitialInput = args.getString(INITIAL_INPUT);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        mViewModel = ViewModelProviders.of(activity).get(TextInputViewModel.class);

        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_text_input, null);
        FixedKeyboardEditText editText = view.findViewById(R.id.text_input);
        editText.setHint(mHintRes);
        editText.setText(mInitialInput);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String input = s.toString();
                TextInputResult textInputResult = new TextInputResult(mRequestCode, input, mExtra);
                mViewModel.set(textInputResult);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        editText.focusAndShowKeyboard();

        mViewModel.observeChanges(this, new Observer<TextInputResult>() {
            @Override
            public void onChanged(@Nullable TextInputResult textInputResult) {
                if (textInputResult == null || Utils.isNullOrEmpty(textInputResult.getResult())) {
                    setPositiveButtonEnabled(false);
                } else {
                    setPositiveButtonEnabled(true);
                }
            }
        });
        setPositiveButtonEnabled(Utils.isNullOrEmpty(editText.getText() == null ?
                null : editText.getText().toString()));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view)
                .setTitle(mTitleRes)
                .setPositiveButton(mPosRes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                })
                .setNegativeButton(mNegRes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mViewModel.set(null);
                        dismiss();
                    }
                });
        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        mViewModel.complete();
    }

    private void setPositiveButtonEnabled(boolean enabled) {
        AlertDialog dialog = (AlertDialog) getDialog();
        Button posButton = dialog == null ? null : dialog.getButton(Dialog.BUTTON_POSITIVE);
        if (posButton != null) {
            posButton.setEnabled(enabled);
        }
    }
}
