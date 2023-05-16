package com.pdftron.collab.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.pdftron.collab.db.entity.DocumentEntity;

import java.util.List;

/**
 * Data Access Object for the document table
 */
@Dao
public interface DocumentDao {

    /**
     * Gets current document
     * @return the document
     */
    @Query("SELECT * from document_table ORDER BY date DESC LIMIT 1")
    LiveData<DocumentEntity> getDocument();

    @Query("SELECT * from document_table ORDER BY id ASC")
    List<DocumentEntity> getDocumentsSync();

    @Query("SELECT * from document_table WHERE id=:id")
    DocumentEntity getDocumentSync(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DocumentEntity documentEntity);

    @Query("UPDATE document_table SET unreads=:unreads WHERE id=:id")
    void updateUnreads(String id, String unreads);

    @Query("DELETE FROM document_table")
    void deleteAll();
}
