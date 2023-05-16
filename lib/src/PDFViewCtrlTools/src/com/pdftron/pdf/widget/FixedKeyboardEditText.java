package com.pdftron.pdf.widget;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;
import com.pdftron.pdf.utils.Utils;

/**
 * Reference: https://developer.squareup.com/blog/showing-the-android-keyboard-reliably/
 */
public class FixedKeyboardEditText extends TextInputEditText {

    private boolean showKeyboardDelayed = false;

    public FixedKeyboardEditText(@NonNull Context context) {
        super(context);
    }

    public FixedKeyboardEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FixedKeyboardEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void focusAndShowKeyboard() {
        requestFocus();
        showKeyboardDelayed = true;
        maybeShowKeyboard();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        maybeShowKeyboard();
    }

    private void maybeShowKeyboard() {
        if (hasWindowFocus() && showKeyboardDelayed) {
            if (isFocused()) {
                post(new Runnable() {
                    @Override
                    public void run() {

                        Utils.showSoftKeyboard(getContext(), FixedKeyboardEditText.this);
                    }
                });
            }
            showKeyboardDelayed = false;
        }
    }
}
