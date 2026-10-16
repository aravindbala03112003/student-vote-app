package com.example.voteapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.voteapp.util.SessionManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Small delay to show splash + animation
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            // If already logged in, LoginActivity will immediately route to MainActivity
            startActivity(intent);
            finish();
        }, 1500);
    }
}
