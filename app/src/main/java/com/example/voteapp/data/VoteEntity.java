package com.example.voteapp.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "votes")
public class VoteEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String studentId = "";

    @NonNull
    public String candidateName = "";

    public long timestamp;
    
    // For Module 4.5: Secure Voting
    public double latitude;
    public double longitude;

    // Feedback feature
    public String feedback;

    @Ignore
    public VoteEntity() {
        // Required for Firebase
    }

    public VoteEntity(@NonNull String studentId, @NonNull String candidateName, long timestamp) {
        this.studentId = studentId;
        this.candidateName = candidateName;
        this.timestamp = timestamp;
    }
}
