package com.pdftron.pdf.dialog.simpleinput;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Utility class for gson
 */
public class TextInputResult {
    private final int requestCode;
    @NonNull
    private final String result;
    @Nullable
    private final Bundle extra;

    public TextInputResult(int requestCode, @NonNull String result) {
        this.requestCode = requestCode;
        this.result = result;
        this.extra = null;
    }

    public TextInputResult(int requestCode, @NonNull String result, @Nullable Bundle extra) {
        this.requestCode = requestCode;
        this.result = result;
        this.extra = extra;
    }

    public int getRequestCode() {
        return requestCode;
    }

    @NonNull
    public String getResult() {
        return result;
    }

    @Nullable
    public Bundle getExtra() {
        return extra;
    }
}
