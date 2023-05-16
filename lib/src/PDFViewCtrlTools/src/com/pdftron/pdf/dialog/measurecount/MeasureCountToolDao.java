package com.pdftron.pdf.dialog.measurecount;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MeasureCountToolDao {
    @Query("SELECT * FROM measure_count_tool")
    LiveData<List<MeasureCountTool>> getCountToolPresets();

    @Query("SELECT * FROM measure_count_tool WHERE label == :label")
    List<MeasureCountTool> getPresetByLabel(String label);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MeasureCountTool measureCountTool);

    @Delete
    void delete(MeasureCountTool measureCountTool);

    @Update
    void update(MeasureCountTool measureCountTool);
}
