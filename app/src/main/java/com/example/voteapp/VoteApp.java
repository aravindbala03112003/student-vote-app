package com.example.voteapp;

import android.app.Application;
import android.util.Log;

import com.google.firebase.database.FirebaseDatabase;

public class VoteApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        enableFirebaseOfflinePersistence();
    }

    private void enableFirebaseOfflinePersistence() {
        try {
            // Enable disk persistence so reads work without internet and writes queue safely.
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception e) {
            // setPersistenceEnabled throws if invoked more than once; swallow to avoid crash.
            Log.w("VoteApp", "Firebase persistence already enabled: " + e.getMessage());
        }
    }
}
