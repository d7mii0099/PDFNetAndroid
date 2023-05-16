package com.pdftron.demo.browser.db.folder;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FolderDao {

    @Query("SELECT * FROM FolderEntity")
    LiveData<List<FolderEntity>> getFolders();

    @Query("SELECT * FROM FolderEntity WHERE folderPath = :folderPath")
    List<FolderEntity> getFolder(String folderPath);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertFolders(List<FolderEntity> folderEntity);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertFolderInBackground(FolderEntity folderEntity);

    @Update
    void updateCollapseState(FolderEntity folderEntity);


}
