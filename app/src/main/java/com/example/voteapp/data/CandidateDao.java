package com.example.voteapp.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CandidateDao {

    @Insert
    long insert(CandidateEntity candidate);

    @Update
    void update(CandidateEntity candidate);

    @Delete
    void delete(CandidateEntity candidate);

    @Query("DELETE FROM candidates WHERE firebaseId = :id")
    void deleteById(String id);

    @Query("SELECT * FROM candidates WHERE firebaseId = :id")
    CandidateEntity getById(String id);

    @Query("SELECT * FROM candidates ORDER BY name ASC")
    List<CandidateEntity> getAll();

    @Query("DELETE FROM candidates")
    void clearAll();
}
