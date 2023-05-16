package com.pdftron.pdf.widget.toolbar.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(foreignKeys = @ForeignKey(entity = ToolbarEntity.class,
        parentColumns = "id",
        childColumns = "toolbarId",
        onDelete = ForeignKey.CASCADE))
public class ToolbarItemEntity {
    @NonNull
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int buttonId;
    public String toolbarId; // id of the toolbar that owns this toolbar item

    public int order;
    public int buttonType;

    public ToolbarItemEntity(int buttonId, @NonNull String toolbarId, int order, int buttonType) {
        this.buttonId = buttonId;
        this.toolbarId = toolbarId;
        this.order = order;
        this.buttonType = buttonType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ToolbarItemEntity that = (ToolbarItemEntity) o;

        return id == that.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
