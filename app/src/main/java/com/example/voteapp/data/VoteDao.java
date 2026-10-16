package com.example.voteapp.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface VoteDao {

    @Insert
    void insert(VoteEntity vote);

    @Query("SELECT COUNT(*) FROM votes WHERE studentId = :studentId")
    int getVoteCountForStudent(String studentId);

    @Query("SELECT COUNT(*) FROM votes WHERE candidateName = :candidateName")
    int getVoteCountForCandidate(String candidateName);

    @Query("SELECT COUNT(*) FROM votes")
    int getTotalVotes();

    @Query("SELECT COUNT(DISTINCT studentId) FROM votes")
    int getDistinctVoterCount();

    @Query("UPDATE votes SET candidateName = :newName WHERE candidateName = :oldName")
    void updateCandidateName(String oldName, String newName);

    @Query("SELECT * FROM votes ORDER BY timestamp DESC")
    java.util.List<VoteEntity> getAllVotes();

    @Query("DELETE FROM votes")
    void clearAll();
}
