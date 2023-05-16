package com.pdftron.pdf.widget.toolbar.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public abstract class ToolbarItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertAll(ToolbarItemEntity... toolbarItemEntity);

    @Transaction
    public void clearAndInsertAll(final String toolbarId, ToolbarItemEntity... toolbarItemEntity) {
        clear(toolbarId);
        insertAll(toolbarItemEntity);
    }

    @Query("DELETE FROM ToolbarItemEntity WHERE toolbarId=:toolbarId")
    public abstract void clear(final String toolbarId);

    @Query("SELECT * FROM ToolbarItemEntity WHERE toolbarId=:toolbarId ORDER BY `order` ASC")
    public abstract List<ToolbarItemEntity> getToolbarItemsFromToolbar(final String toolbarId);
}
