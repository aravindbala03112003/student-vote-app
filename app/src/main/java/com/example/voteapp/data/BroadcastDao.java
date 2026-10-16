package com.example.voteapp.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BroadcastDao {

    @Query("SELECT * FROM broadcasts ORDER BY createdAt DESC")
    List<BroadcastMessageEntity> getAll();

    @Insert
    void insert(BroadcastMessageEntity broadcast);
}
