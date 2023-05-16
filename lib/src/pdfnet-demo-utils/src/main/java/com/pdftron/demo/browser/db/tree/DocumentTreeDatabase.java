package com.pdftron.demo.browser.db.tree;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// Only used for Android Q
@TargetApi(Build.VERSION_CODES.Q)
@Database(entities = {DocumentTreeEntity.class}, version = 2)
public abstract class DocumentTreeDatabase extends RoomDatabase {

    private static volatile DocumentTreeDatabase INSTANCE;

    public abstract DocumentTreeDao folderDao();

    public static DocumentTreeDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (DocumentTreeDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            DocumentTreeDatabase.class, "roots.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
