package com.pdftron.pdf.utils;

import androidx.annotation.StyleRes;

import com.pdftron.pdf.tools.R;

public class ThemeProvider {

    private @StyleRes
    int mTheme = R.style.PDFTronAppTheme;

    public void setTheme(@StyleRes int theme) {
        mTheme = theme;
    }

    @StyleRes
    public int getTheme() {
        return mTheme;
    }
}
