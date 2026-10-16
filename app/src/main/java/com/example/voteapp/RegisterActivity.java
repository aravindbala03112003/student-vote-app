package com.example.voteapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.voteapp.data.UserEntity;
import com.example.voteapp.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        TextInputEditText editUsername = findViewById(R.id.editRegUsername);
        TextInputEditText editPassword = findViewById(R.id.editRegPassword);
        TextInputEditText editName = findViewById(R.id.editRegName);
        TextInputEditText editDept = findViewById(R.id.editRegDepartment);
        TextInputEditText editYear = findViewById(R.id.editRegYear);
        RadioGroup radioRole = findViewById(R.id.radioRoleGroup);
        MaterialButton buttonRegister = findViewById(R.id.buttonRegister);
        MaterialButton buttonTabLogin = findViewById(R.id.buttonTabLogin);

        buttonRegister.setOnClickListener(v -> {
            String username = getText(editUsername);
            String password = getText(editPassword);
            String name = getText(editName);
            String dept = getText(editDept);
            String year = getText(editYear);

            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Username and password are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            String role = SessionManager.ROLE_STUDENT;
            if (radioRole != null) {
                int checkedId = radioRole.getCheckedRadioButtonId();
                if (checkedId != -1) {
                    RadioButton rb = findViewById(checkedId);
                    if (rb != null && rb.getTag() != null) {
                        role = rb.getTag().toString();
                    }
                }
            }

            registerWithFirebase(username, password, role, name, dept, year);
        });

        if (buttonTabLogin != null) {
            buttonTabLogin.setOnClickListener(v -> {
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }

    private void registerWithFirebase(String username, String password, String role, String name, String dept, String year) {
        String email = username.trim() + "@voteapp.com";

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String userId = mAuth.getCurrentUser().getUid();
                        
                        UserEntity user = new UserEntity(username, password, role, name, dept, year);
                        // Default values for candidates
                        if (SessionManager.ROLE_CANDIDATE.equals(role)) {
                            user.status = "pending";
                        } else {
                            user.status = "approved"; // Students and Admins are approved by default in this simple flow
                        }
                        
                        mDatabase.child("users").child(userId).setValue(user)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(RegisterActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                                    SessionManager.setLoggedInUser(RegisterActivity.this, 0, role, name, dept, year);
                                    
                                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> Toast.makeText(RegisterActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Auth Failed";
                        Toast.makeText(RegisterActivity.this, "Registration failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
