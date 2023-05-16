package com.pdftron.demo.browser.db.trash;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TrashDao {

    @Query("SELECT * FROM trash_table WHERE IS_EXTERNAL = 0 ORDER BY trash_date DESC")
    List<TrashEntity> getTrashes();

    @Query("SELECT * FROM trash_table WHERE IS_EXTERNAL = 1 ORDER BY trash_date DESC")
    List<TrashEntity> getExternalTrashes();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertTrashes(TrashEntity... trashEntities);

    @Query("DELETE FROM trash_table WHERE id = :id")
    void delete(Long id);
}
