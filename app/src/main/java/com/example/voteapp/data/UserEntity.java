package com.example.voteapp.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String username = "";

    @NonNull
    public String password = "";

    @NonNull
    public String role = "student"; // "student", "admin", or "candidate"

    public String name;
    public String department;
    public String year;
    public String phoneNumber;
    public String email;

    // For Candidate Module (4.2 & 4.3)
    public String status = "pending"; // pending, approved, rejected
    public boolean hasBacklog = false;
    public String partyName;
    public String gender = "Male";
    public String idCardUrl;
    public String marksheetUrl;

    @Ignore
    public UserEntity() {
        // Required for Firebase
    }

    public UserEntity(@NonNull String username,
                      @NonNull String password,
                      @NonNull String role,
                      String name,
                      String department,
                      String year) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.name = name;
        this.department = department;
        this.year = year;
    }
}
