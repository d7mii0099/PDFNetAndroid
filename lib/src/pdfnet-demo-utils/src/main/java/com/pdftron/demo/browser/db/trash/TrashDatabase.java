package com.pdftron.demo.browser.db.trash;

import android.content.Context;
import androidx.annotation.VisibleForTesting;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.pdftron.demo.browser.db.converter.DateConverter;

@Database(entities = {TrashEntity.class}, version = 9)
@TypeConverters(DateConverter.class)
public abstract class TrashDatabase extends RoomDatabase {

    private static volatile TrashDatabase mInstance;

    @VisibleForTesting
    public static final String DATABASE_NAME = "pdftron-trash.db";

    public abstract TrashDao mTrashDao();

    public static TrashDatabase getInstance(final Context context) {
        if (mInstance == null) {
            synchronized (TrashDatabase.class) {
                if (mInstance == null) {
                    mInstance = Room.databaseBuilder(context.getApplicationContext(),
                            TrashDatabase.class, DATABASE_NAME)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return mInstance;
    }
}
