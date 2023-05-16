package com.pdftron.pdf.widget.toolbar.data;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface ToolbarDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(ToolbarEntity... toolbarEntities);

    @Update
    void update(ToolbarEntity... repos);

    @Delete
    void delete(ToolbarEntity toolbarEntity);


    @Query("SELECT * FROM ToolbarEntity WHERE id = :id")
    ToolbarEntity getToolbar(@NonNull String id);

}
