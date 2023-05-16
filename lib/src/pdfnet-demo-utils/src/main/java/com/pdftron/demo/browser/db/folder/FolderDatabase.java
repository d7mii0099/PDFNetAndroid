package com.pdftron.demo.browser.db.folder;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

@Database(entities = {FolderEntity.class}, version = 2)
public abstract class FolderDatabase extends RoomDatabase {

    private static volatile FolderDatabase INSTANCE;

    public abstract FolderDao folderDao();

    public static FolderDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (FolderDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            FolderDatabase.class, "allfolders.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
