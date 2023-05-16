package com.pdftron.demo.browser.db.file;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import io.reactivex.Flowable;

@Dao
public interface FileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFilesInBackground(List<FileEntity> fileEntities);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFiles(FileEntity ... fileEntity);

    @Transaction
    @Query("SELECT * FROM FileEntity")
    List<FileEntity> getFiles();

    @Transaction
    @Query("SELECT * FROM FileEntity WHERE filename LIKE :searchQuery" +
            " AND docType in (:documentTypes)" +
            " ORDER BY fileParent COLLATE NOCASE ASC, filename COLLATE NOCASE ASC")
    Flowable<List<FileEntity>> getGroupedFilesFlowableByName(String searchQuery, List<Integer> documentTypes);

    @Transaction
    @Query("SELECT * FROM FileEntity WHERE filename LIKE :searchQuery" +
            " AND docType in (:documentTypes)" +
            " ORDER BY fileParent COLLATE NOCASE ASC, date COLLATE NOCASE DESC")
    Flowable<List<FileEntity>> getGroupedFlowableByDate(String searchQuery, List<Integer> documentTypes);

    @Transaction
    @Query("SELECT * FROM FileEntity WHERE filename LIKE :searchQuery" +
            " AND docType in (:documentTypes)" +
            " ORDER BY filename COLLATE NOCASE ASC")
    Flowable<List<FileEntity>> getFlatFilesFlowableByName(String searchQuery, List<Integer> documentTypes);

    @Transaction
    @Query("SELECT * FROM FileEntity WHERE filename LIKE :searchQuery" +
            " AND docType in (:documentTypes)" +
            " ORDER BY date COLLATE NOCASE DESC")
    Flowable<List<FileEntity>> getFlatFlowableByDate(String searchQuery, List<Integer> documentTypes);

    @Delete
    void deleteFiles(FileEntity... fileEntity);

    @Query("DELETE FROM FileEntity")
    void clearFiles();
}
