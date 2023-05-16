package com.pdftron.demo.browser.db.file;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// Only used for Android Q in LocalDocumentFileViewFragment
@TargetApi(Build.VERSION_CODES.Q)
@Database(entities = {FileEntity.class}, version = 2)
public abstract class DocumentFileDatabase extends RoomDatabase {

    private static volatile DocumentFileDatabase INSTANCE;

    public abstract FileDao fileDao();

    public static DocumentFileDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (DocumentFileDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            DocumentFileDatabase.class, "alldocumentfiles.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
