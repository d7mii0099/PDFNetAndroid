package com.pdftron.demo.browser.db.tree;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class DocumentTreeEntity {
    @NonNull
    @PrimaryKey
    private String documentFileUri;

    public DocumentTreeEntity(@NonNull String documentFileUri) {
        this.documentFileUri = documentFileUri;
    }

    @NonNull
    public String getDocumentFileUri() {
        return documentFileUri;
    }
}
