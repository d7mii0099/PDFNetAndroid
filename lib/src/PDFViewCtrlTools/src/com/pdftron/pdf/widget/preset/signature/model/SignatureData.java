package com.pdftron.pdf.widget.preset.signature.model;

import androidx.annotation.NonNull;

public class SignatureData {
    @NonNull
    private String filePath;
    private long lastUsedRawDate;

    public SignatureData(@NonNull String filePath, long lastUsedRawDate) {
        this.filePath = filePath;
        this.lastUsedRawDate = lastUsedRawDate;
    }

    @NonNull
    public String getFilePath() {
        return filePath;
    }

    public long getLastUsedRawDate() {
        return lastUsedRawDate;
    }

}
