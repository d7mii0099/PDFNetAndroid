package com.pdftron.collab.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.pdftron.collab.db.entity.AnnotationEntity;

import java.util.Date;
import java.util.List;

import io.reactivex.Flowable;

/**
 * Data Access Object for the annotation table
 */
@Dao
public interface AnnotationDao {

    /**
     * Gets all annotations associated with a document
     *
     * @param docId the unique identifier of the document
     * @return the annotations
     */
    @Query("SELECT * from annotation_table WHERE document_id=:docId ORDER BY id ASC")
    Flowable<List<AnnotationEntity>> getAnnotations(String docId);

    @Query("SELECT * from annotation_table WHERE document_id=:docId ORDER BY id ASC")
    List<AnnotationEntity> getAnnotationsSync(String docId);

    @Query("SELECT * from annotation_table WHERE id=:id")
    Flowable<AnnotationEntity> getAnnotation(String id);

    @Query("SELECT * from annotation_table WHERE id=:id")
    AnnotationEntity getAnnotationSync(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AnnotationEntity annotationEntity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAnnotations(List<AnnotationEntity> annotationEntities);

    @Query("UPDATE annotation_table SET xfdf=:xfdf, at=:action, date=:date, contents=:contents, y_pos=:yPos, color=:color, opacity=:opacity WHERE id=:id")
    void update(String id, String xfdf, String action, String contents, double yPos, int color, float opacity, Date date);

    @Query("UPDATE annotation_table SET last_reply_author=:replyAuthor, last_reply_contents=:replyContents, last_reply_date=:replyDate WHERE id=:id")
    void updateReply(String id, String replyAuthor, String replyContents, Date replyDate);

    @Query("UPDATE annotation_table SET unreads=:unreads, unread_count=:count WHERE id=:id")
    void updateUnreads(String id, String unreads, int count);

    @Query("UPDATE annotation_table SET review_state=:reviewState WHERE id=:id")
    void updateReviewState(String id, int reviewState);

    @Query("UPDATE annotation_table SET server_id=:serverId WHERE id=:id")
    void updateServerId(String id, String serverId);

    @Query("UPDATE annotation_table SET author_name=:authorName WHERE author_id=:authorId")
    void updateAuthorName(String authorId, String authorName);

    @Query("UPDATE annotation_table SET unread_count=0, unreads=null WHERE id=:id")
    void resetUnreadCount(String id);

    @Query("DELETE FROM annotation_table WHERE id = :id")
    void delete(String id);

    @Query("DELETE FROM annotation_table")
    void deleteAll();
}
