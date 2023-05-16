package com.pdftron.pdf.dialog.measurecount;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "measure_count_tool")
public class MeasureCountTool {
    @PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull
    public String label = "";
    public String annotStyleJson;
    public int annotCount;
}
