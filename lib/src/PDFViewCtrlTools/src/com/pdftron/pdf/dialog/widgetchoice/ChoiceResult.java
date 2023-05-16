package com.pdftron.pdf.dialog.widgetchoice;

import androidx.annotation.Nullable;

public class ChoiceResult {
    private final long widget;
    private final int page;
    private final boolean singleChoice;
    @Nullable
    private final String[] options;

    public ChoiceResult(long widget, int page, boolean singleChoice, @Nullable String[] options) {
        this.widget = widget;
        this.page = page;
        this.singleChoice = singleChoice;
        this.options = options;
    }

    public long getWidget() {
        return this.widget;
    }

    public int getPage() {
        return this.page;
    }

    public boolean isSingleChoice() {
        return this.singleChoice;
    }

    @Nullable
    public String[] getOptions() {
        return this.options;
    }
}
