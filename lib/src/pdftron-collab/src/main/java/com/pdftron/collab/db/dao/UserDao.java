package com.pdftron.collab.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.pdftron.collab.db.entity.UserEntity;

/**
 * Data Access Object for the user table
 */
@Dao
public interface UserDao {

    /**
     * Gets current user
     *
     * @return the user
     */
    @Query("SELECT * from user_table WHERE current_user=1 ORDER BY date DESC LIMIT 1")
    LiveData<UserEntity> getCurrentUser();

    @Query("SELECT * from user_table WHERE current_user=1 ORDER BY date DESC LIMIT 1")
    UserEntity getCurrentUserSync();

    @Query("SELECT * from user_table WHERE id=:id")
    UserEntity getUserSync(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserEntity userEntity);

    @Query("UPDATE user_table SET current_user=:isCurrentUser WHERE id=:id")
    void updateIsCurrentUser(String id, boolean isCurrentUser);

    @Query("UPDATE user_table SET active_annotation=:activeAnnotation WHERE id=:id")
    void update(String id, String activeAnnotation);

    @Query("DELETE FROM user_table")
    void deleteAll();
}
