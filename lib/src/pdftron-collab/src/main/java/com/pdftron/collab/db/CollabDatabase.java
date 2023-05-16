package com.pdftron.collab.db;

import android.content.Context;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.pdftron.collab.db.converter.DateConverter;
import com.pdftron.collab.db.dao.AnnotationDao;
import com.pdftron.collab.db.dao.DocumentDao;
import com.pdftron.collab.db.dao.LastAnnotationDao;
import com.pdftron.collab.db.dao.ReplyDao;
import com.pdftron.collab.db.dao.UserDao;
import com.pdftron.collab.db.entity.AnnotationEntity;
import com.pdftron.collab.db.entity.DocumentEntity;
import com.pdftron.collab.db.entity.LastAnnotationEntity;
import com.pdftron.collab.db.entity.ReplyEntity;
import com.pdftron.collab.db.entity.UserEntity;

import java.io.File;

/**
 * The Room database that contains the collaboration information
 */
@Database(entities = {UserEntity.class, DocumentEntity.class, AnnotationEntity.class, LastAnnotationEntity.class, ReplyEntity.class}, version = 11)
@TypeConverters(DateConverter.class)
public abstract class CollabDatabase extends RoomDatabase {

    private static volatile CollabDatabase sInstance;

    @VisibleForTesting
    public static final String DATABASE_NAME = "pdftron-collab.db";

    public abstract UserDao userDao();

    public abstract DocumentDao documentDao();

    public abstract AnnotationDao annotationDao();

    public abstract LastAnnotationDao lastAnnotationDao();

    public abstract ReplyDao replyDao();

    private final MutableLiveData<Boolean> mIsDatabaseCreated = new MutableLiveData<>();

    private static File sXfdfCache;

    public static CollabDatabase getInstance(final Context context) {
        if (sInstance == null) {
            synchronized (CollabDatabase.class) {
                if (sInstance == null) {
                    sInstance = Room.databaseBuilder(context.getApplicationContext(),
                            CollabDatabase.class, DATABASE_NAME)
                            .fallbackToDestructiveMigration()
                            .build();
                    sInstance.updateDatabaseCreated(context.getApplicationContext());

                    // create xfdf cache path
                    File xfdfCachePath = new File(context.getCacheDir(), "pdftron-xfdf-cache");
                    xfdfCachePath.mkdir();
                    sXfdfCache = xfdfCachePath;
                }
            }
        }
        return sInstance;
    }

    public static File getXfdfCachePath() {
        return sXfdfCache;
    }

    /**
     * Check whether the database already exists and expose it via {@link #getDatabaseCreated()}
     */
    private void updateDatabaseCreated(final Context context) {
        if (context.getDatabasePath(DATABASE_NAME).exists()) {
            setDatabaseCreated();
        }
    }

    private void setDatabaseCreated() {
        mIsDatabaseCreated.postValue(true);
    }

    public LiveData<Boolean> getDatabaseCreated() {
        return mIsDatabaseCreated;
    }
}
