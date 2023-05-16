package com.pdftron.demo.browser.db.file;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

@Database(entities = {FileEntity.class}, version = 2)
public abstract class FileDatabase extends RoomDatabase {

    private static volatile FileDatabase INSTANCE;

    public abstract FileDao fileDao();

    public static FileDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (FileDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            FileDatabase.class, "allfiles.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
