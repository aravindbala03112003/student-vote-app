package com.example.voteapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.example.voteapp.data.UserEntity;
import com.example.voteapp.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        if (SessionManager.isLoggedIn(this)) {
            openMain();
            return;
        }

        setContentView(R.layout.activity_login);

        TextInputEditText editUsername = findViewById(R.id.editUsername);
        TextInputEditText editPassword = findViewById(R.id.editPassword);
        MaterialButton buttonLogin = findViewById(R.id.buttonLogin);
        MaterialButton buttonGoToRegister = findViewById(R.id.buttonGoToRegister);

        buttonLogin.setOnClickListener(v -> {
            String username = editUsername.getText() != null ? editUsername.getText().toString().trim() : "";
            String password = editPassword.getText() != null ? editPassword.getText().toString().trim() : "";
            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Enter username and password", Toast.LENGTH_SHORT).show();
                return;
            }
            
            loginWithFirebase(username, password);
        });

        buttonGoToRegister.setOnClickListener(v -> openRegister());
    }

    private void loginWithFirebase(String username, String password) {
        String email = username + "@voteapp.com";

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        fetchUserRoleAndFinish(mAuth.getCurrentUser().getUid());
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchUserRoleAndFinish(String userId) {
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Manual mapping to match UserEntity structure
                    String role = snapshot.child("role").getValue(String.class);
                    String name = snapshot.child("name").getValue(String.class);
                    String dept = snapshot.child("department").getValue(String.class);
                    String year = snapshot.child("year").getValue(String.class);
                    String username = snapshot.child("username").getValue(String.class);

                    UserEntity user = new UserEntity(
                            username != null ? username : "",
                            "", // Password not needed in session
                            role != null ? role : SessionManager.ROLE_STUDENT,
                            name, dept, year);
                    
                    handleLoginSuccess(user);
                } else {
                    Toast.makeText(LoginActivity.this, "User profile not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void openRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    private void handleLoginSuccess(UserEntity user) {
        if (SessionManager.ROLE_STUDENT.equals(user.role)) {
            BiometricManager manager = BiometricManager.from(this);
            int can = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
            if (can != BiometricManager.BIOMETRIC_SUCCESS) {
                completeLogin(user);
                return;
            }

            Executor executor = ContextCompat.getMainExecutor(this);
            BiometricPrompt.AuthenticationCallback callback = new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    completeLogin(user);
                }

                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    Toast.makeText(LoginActivity.this, "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                }
            };

            BiometricPrompt prompt = new BiometricPrompt(this, executor, callback);
            BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Confirm student login")
                    .setSubtitle("Authenticate with fingerprint to continue")
                    .setNegativeButtonText("Cancel")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .build();
            prompt.authenticate(info);
        } else {
            completeLogin(user);
        }
    }

    private void completeLogin(UserEntity user) {
        SessionManager.setLoggedInUser(this,
                0, // ID not used with Firebase Auth
                user.role,
                user.name != null ? user.name : user.username,
                user.department != null ? user.department : "",
                user.year != null ? user.year : "");
        openMain();
    }
}
