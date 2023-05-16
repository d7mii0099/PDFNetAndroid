package com.pdftron.collab.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.collab.model.User;

/**
 * Immutable model class for a user
 */
@Entity(tableName = "user_table")
public class UserEntity implements User {

    @PrimaryKey
    @NonNull
    private String id;

    @NonNull
    private String name;

    @NonNull
    private Long date;

    @ColumnInfo(name = "current_user")
    private boolean isCurrentUser;

    @Nullable
    @ColumnInfo(name = "active_annotation")
    private String activeAnnotation;

    public UserEntity(@NonNull String id, @Nullable String name,
            @NonNull Long date, @Nullable String activeAnnotation) {
        this.id = id;
        this.name = name != null ? name : id;
        this.date = date;
        this.activeAnnotation = activeAnnotation;
        this.isCurrentUser = false;
    }

    public void setIsCurrentUser(boolean isCurrentUser) {
        this.isCurrentUser = isCurrentUser;
    }

    /**
     * Gets the unique identifier of the user
     */
    @Override
    public String getId() {
        return this.id;
    }

    /**
     * Gets the name of the user
     */
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Long getDate() {
        return this.date;
    }

    @Override
    public boolean isCurrentUser() {
        return this.isCurrentUser;
    }

    /**
     * Gets the annotation id the user is currently reading
     */
    @Override
    public String getActiveAnnotation() {
        return this.activeAnnotation;
    }
}
