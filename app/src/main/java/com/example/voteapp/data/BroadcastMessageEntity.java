package com.example.voteapp.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "broadcasts")
public class BroadcastMessageEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String title = "";

    @NonNull
    public String message = "";

    public long createdAt;

    @Ignore
    public BroadcastMessageEntity() {
        // Required for Firebase
    }

    public BroadcastMessageEntity(@NonNull String title, @NonNull String message, long createdAt) {
        this.title = title;
        this.message = message;
        this.createdAt = createdAt;
    }
}
