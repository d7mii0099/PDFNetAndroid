package com.pdftron.pdf.widget.toolbar.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class ToolbarEntity {
    @NonNull
    @PrimaryKey
    private String id;

    private String title;

    public ToolbarEntity(@NonNull String id, @NonNull String title) {
        this.id = id;
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public String getId() {
        return id;
    }
}
