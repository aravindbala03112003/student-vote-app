package com.example.voteapp.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.voteapp.R;
import com.example.voteapp.data.UserEntity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class CandidateProfileFragment extends Fragment {

    private DatabaseReference mDatabase;
    private String userId;

    private TextView textStatus, textUploadStatus;
    private TextInputEditText editName, editEmail, editPhone, editDept, editYear, editParty;
    private CheckBox checkBacklog;
    private ProgressBar progressBar;
    private RadioGroup radioGenderGroup;
    
    private String idCardBase64, marksheetBase64;

    private final ActivityResultLauncher<String> idCardPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    idCardBase64 = uriToBase64(uri);
                    updateUIStatus();
                }
            }
    );

    private final ActivityResultLauncher<String> marksheetPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    marksheetBase64 = uriToBase64(uri);
                    updateUIStatus();
                }
            }
    );

    public CandidateProfileFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_candidate_profile, container, false);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        userId = FirebaseAuth.getInstance().getUid();

        textStatus = root.findViewById(R.id.textCandidateStatus);
        textUploadStatus = root.findViewById(R.id.textUploadStatus);
        progressBar = root.findViewById(R.id.progressBarProfile);
        
        editName = root.findViewById(R.id.editProfileName);
        editEmail = root.findViewById(R.id.editProfileEmail);
        editPhone = root.findViewById(R.id.editProfilePhone);
        editDept = root.findViewById(R.id.editProfileDept);
        editYear = root.findViewById(R.id.editProfileYear);
        editParty = root.findViewById(R.id.editProfileParty);
        checkBacklog = root.findViewById(R.id.checkBacklog);
        radioGenderGroup = root.findViewById(R.id.radioProfileGenderGroup);
        
        MaterialButton buttonIdCard = root.findViewById(R.id.buttonUploadIdCard);
        MaterialButton buttonMarksheet = root.findViewById(R.id.buttonUploadMarksheet);
        MaterialButton buttonSave = root.findViewById(R.id.buttonSaveProfile);

        loadProfileData();

        buttonIdCard.setOnClickListener(v -> idCardPicker.launch("image/*"));
        buttonMarksheet.setOnClickListener(v -> marksheetPicker.launch("image/*"));
        buttonSave.setOnClickListener(v -> submitProfile());

        return root;
    }

    private String uriToBase64(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            
            // Resize to keep database entry lightweight
            int maxSize = 800;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width > maxSize || height > maxSize) {
                float ratio = (float) width / height;
                if (ratio > 1) { width = maxSize; height = (int) (maxSize / ratio); }
                else { height = maxSize; width = (int) (maxSize * ratio); }
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream);
            return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Image processing failed", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void updateUIStatus() {
        String status = "Docs: " + (idCardBase64 != null ? "ID ✅" : "ID ❌") + 
                        " | " + (marksheetBase64 != null ? "Marks ✅" : "Marks ❌");
        textUploadStatus.setText(status);
    }

    private void loadProfileData() {
        if (userId == null) return;
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserEntity user = snapshot.getValue(UserEntity.class);
                if (user != null) {
                    editName.setText(user.name);
                    editEmail.setText(user.email);
                    editPhone.setText(user.phoneNumber);
                    editDept.setText(user.department);
                    editYear.setText(user.year);
                    editParty.setText(user.partyName);
                    checkBacklog.setChecked(user.hasBacklog);
                    textStatus.setText("Status: " + user.status.toUpperCase());
                    
                    if ("Female".equalsIgnoreCase(user.gender)) {
                        radioGenderGroup.check(R.id.radioProfileFemale);
                    } else {
                        radioGenderGroup.check(R.id.radioProfileMale);
                    }
                    
                    idCardBase64 = user.idCardUrl;
                    marksheetBase64 = user.marksheetUrl;
                    updateUIStatus();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void submitProfile() {
        if (TextUtils.isEmpty(editName.getText()) || TextUtils.isEmpty(editDept.getText())) {
            Toast.makeText(getContext(), "Missing required info", Toast.LENGTH_SHORT).show();
            return;
        }
        if (idCardBase64 == null || marksheetBase64 == null) {
            Toast.makeText(getContext(), "Please select both documents", Toast.LENGTH_SHORT).show();
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        String gender = "Male";
        int checkedId = radioGenderGroup.getCheckedRadioButtonId();
        if (checkedId != -1) {
            RadioButton rb = getView().findViewById(checkedId);
            if (rb != null && rb.getTag() != null) {
                gender = rb.getTag().toString();
            }
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", editName.getText().toString().trim());
        updates.put("email", editEmail.getText().toString().trim());
        updates.put("phoneNumber", editPhone.getText().toString().trim());
        updates.put("department", editDept.getText().toString().trim());
        updates.put("year", editYear.getText().toString().trim());
        updates.put("partyName", editParty.getText().toString().trim());
        updates.put("gender", gender);
        updates.put("hasBacklog", checkBacklog.isChecked());
        updates.put("idCardUrl", idCardBase64); // Base64 stored here
        updates.put("marksheetUrl", marksheetBase64);
        updates.put("status", "pending");

        mDatabase.child("users").child(userId).updateChildren(updates).addOnSuccessListener(aVoid -> {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Application Submitted!", Toast.LENGTH_SHORT).show();
            loadProfileData();
        });
    }
}
