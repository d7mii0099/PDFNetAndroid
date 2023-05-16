package com.pdftron.pdf.dialog.measurecount;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                MeasureCountTool.class
        },
        version = 1,
        exportSchema = false
)
public abstract class MeasureCountToolDb extends RoomDatabase {
    public abstract MeasureCountToolDao mMeasureCountToolDao();

    private static MeasureCountToolDb INSTANCE;
    private static final String DATABASE_NAME = "measure_count_tool_db";

    public static synchronized MeasureCountToolDb getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = buildDatabase(context.getApplicationContext());
        }
        return INSTANCE;
    }

    private static MeasureCountToolDb buildDatabase(Context appContext) {
        return Room.databaseBuilder(appContext, MeasureCountToolDb.class, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build();
    }
}