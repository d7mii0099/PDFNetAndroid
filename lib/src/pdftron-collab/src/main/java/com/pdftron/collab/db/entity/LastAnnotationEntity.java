package com.pdftron.collab.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.pdftron.collab.model.LastAnnotation;
import com.pdftron.collab.service.CustomServiceUtils;

/**
 * Immutable model class for a last annotation, this table only contains a single entry with key "xfdf"
 */
@Entity(tableName = "last_annotation_table")
public class LastAnnotationEntity implements LastAnnotation {

    @PrimaryKey
    @NonNull
    private String id;
    @NonNull
    private String xfdf;
    @NonNull
    private Long date;

    public LastAnnotationEntity(@NonNull String id, @NonNull String xfdf, @NonNull Long date) {
        this.id = id;
        // XFDF is usually too big to fit in database
        // instead, we will write them to disk
        this.xfdf = CustomServiceUtils.getXfdfFile(xfdf);
        this.date = date;
    }

    /**
     * Gets the key which is "xfdf"
     */
    @Override
    public String getId() {
        return this.id;
    }

    /**
     * Gets the last annotation XFDF
     */
    @Override
    public String getXfdf() {
        return this.xfdf;
    }

    @Override
    public Long getDate() {
        return this.date;
    }
}
