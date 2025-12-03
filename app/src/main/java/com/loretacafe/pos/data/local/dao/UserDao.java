package com.loretacafe.pos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.loretacafe.pos.data.local.entity.UserEntity;

import java.util.List;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserEntity user);

    @Update
    void update(UserEntity user);

    @Query("SELECT * FROM users WHERE user_id = :id LIMIT 1")
    LiveData<UserEntity> observeUser(long id);

    @Query("SELECT * FROM users WHERE user_id = :id LIMIT 1")
    UserEntity getUserById(long id);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    UserEntity getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE email = :email OR name = :username LIMIT 1")
    UserEntity getUserByEmailOrUsername(String email, String username);

    @Query("SELECT * FROM users WHERE (email = :identifier OR name = :identifier) AND password = :password AND is_active = 1 LIMIT 1")
    UserEntity authenticateUser(String identifier, String password);

    @Query("UPDATE users SET password = :newPassword WHERE email = :email")
    void updatePassword(String email, String newPassword);

    @Query("SELECT * FROM users ORDER BY created_at DESC")
    List<UserEntity> getAll();

    @Query("DELETE FROM users WHERE user_id = :userId")
    void deleteUser(long userId);

    @Query("DELETE FROM users")
    void clear();

    @Query("UPDATE users SET is_active = :isActive, updated_at = :updatedAt WHERE user_id = :userId")
    void updateActiveStatus(long userId, boolean isActive, java.time.OffsetDateTime updatedAt);

    @Query("SELECT * FROM users WHERE role = :role AND is_active = 1 ORDER BY created_at DESC")
    List<UserEntity> getActiveUsersByRole(String role);
}

