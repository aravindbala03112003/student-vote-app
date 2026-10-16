package com.example.voteapp.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface UserDao {

    @Insert
    long insert(UserEntity user);

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    UserEntity getByUsername(String username);

    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    UserEntity login(String username, String password);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    UserEntity getById(long id);

    @Query("SELECT COUNT(*) FROM users WHERE role = :role")
    int getCountByRole(String role);
}
