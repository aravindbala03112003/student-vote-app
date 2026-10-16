package com.example.voteapp.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "candidates")
public class CandidateEntity {

    @PrimaryKey
    @NonNull
    public String firebaseId = "";

    public int numericalId; // Simple ID like 1, 2, 3...

    @NonNull
    public String name = "";

    public String department;
    public String partyName;
    public String gender;
    public String phoneNumber;

    public static final String GENDER_MALE = "Male";
    public static final String GENDER_FEMALE = "Female";

    @Ignore
    public CandidateEntity() {
        // Required for Firebase
    }

    public CandidateEntity(@NonNull String name, String department, String partyName, String gender, String phoneNumber) {
        this.name = name;
        this.department = department;
        this.partyName = partyName;
        this.gender = gender;
        this.phoneNumber = phoneNumber;
    }
}
