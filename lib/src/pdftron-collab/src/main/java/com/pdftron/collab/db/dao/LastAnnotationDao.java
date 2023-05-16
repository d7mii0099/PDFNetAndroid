package com.pdftron.collab.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.pdftron.collab.db.entity.LastAnnotationEntity;

import java.util.List;

/**
 * Data Access Object for the last annotation table
 */
@Dao
public interface LastAnnotationDao {

    /**
     * Gets the last annotations merged
     * @return the last annotations
     */
    @Query("SELECT * from last_annotation_table ORDER BY date ASC")
    LiveData<List<LastAnnotationEntity>> getLastAnnotations();

    @Query("SELECT * from last_annotation_table ORDER BY id ASC")
    List<LastAnnotationEntity> getLastAnnotationsSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LastAnnotationEntity lastAnnotationEntity);

    @Query("DELETE FROM last_annotation_table WHERE id IN (:ids)")
    void deleteByIds(List<String> ids);

    @Query("DELETE FROM last_annotation_table")
    void deleteAll();
}
