package com.pdftron.demo.browser.db.tree;

import android.annotation.TargetApi;
import android.os.Build;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

// Only used for Android Q
@TargetApi(Build.VERSION_CODES.Q)
@Dao
public interface DocumentTreeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRoot(DocumentTreeEntity documentTreeEntity);

    @Delete
    void deleteRoot(DocumentTreeEntity documentTreeEntity);

    @Query("SELECT * FROM DocumentTreeEntity")
    List<DocumentTreeEntity> getRoots();
}
