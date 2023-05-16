package com.pdftron.pdf.widget.toolbar.data;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {ToolbarEntity.class, ToolbarItemEntity.class}, version = 2)
public abstract class ToolbarDatabase extends RoomDatabase {

    public static final String DEFAULT_DATABASE_NAME = "annotation-toolbars.db";

    public abstract ToolbarDao getToolbarDao();

    public abstract ToolbarItemDao getToolbarItemDao();

    // We need to keep a static reference to database, otherwise leak warning
    // see https://github.com/android/architecture-components-samples/issues/396#issuecomment-413461882
    // From https://developer.android.com/training/data-storage/room#database it seems it's okay to store
    // as singleton and is even recommended
    @Nullable
    private static volatile ToolbarDatabase INSTANCE = null;

    // Migration from 1 to 2, Room 2.2.5
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Okay to delete items since toolbar still in beta in this case
            database.execSQL("DELETE FROM ToolbarItemEntity");
            // Add new column for buttonId
            database.execSQL("ALTER TABLE ToolbarItemEntity ADD COLUMN buttonId INTEGER NOT NULL DEFAULT 0");
        }
    };

    // Pattern from https://github.com/android/architecture-components-samples/blob/2c19434f89e925b8bea56366faa0a197c5b90b96/BasicRxJavaSample/app/src/main/java/com/example/android/observability/persistence/UsersDatabase.java
    @NonNull
    public static ToolbarDatabase getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized(ToolbarDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context,
                            ToolbarDatabase.class, ToolbarDatabase.DEFAULT_DATABASE_NAME)
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

}
