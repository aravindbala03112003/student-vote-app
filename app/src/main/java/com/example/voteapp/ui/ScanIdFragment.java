package com.example.voteapp.ui;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.voteapp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScanIdFragment extends Fragment {

    private static final int REQ_CAMERA = 2001;

    private ImageView imagePreview;
    private TextView textRecognized;
    private String extractedId;

    private TextInputEditText editDetectedId;

    private ActivityResultLauncher<Void> cameraLauncher;

    public ScanIdFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
            if (bitmap != null) {
                if (imagePreview != null) {
                    imagePreview.setImageBitmap(bitmap);
                }
                runTextRecognition(bitmap);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_scan_id, container, false);

        imagePreview = root.findViewById(R.id.imagePreview);
        textRecognized = root.findViewById(R.id.textRecognized);
        editDetectedId = root.findViewById(R.id.editDetectedId);
        MaterialButton buttonCapture = root.findViewById(R.id.buttonCapture);
        MaterialButton buttonSaveId = root.findViewById(R.id.buttonSaveId);

        buttonCapture.setOnClickListener(v -> ensureCameraAndCapture());

        buttonSaveId.setOnClickListener(v -> {
            String finalId;
            if (editDetectedId != null && editDetectedId.getText() != null) {
                finalId = editDetectedId.getText().toString().trim();
            } else {
                finalId = extractedId != null ? extractedId.trim() : "";
            }

            if (TextUtils.isEmpty(finalId)) {
                Toast.makeText(requireContext(), "Enter or scan a valid student ID first", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences prefs = requireContext().getSharedPreferences("vote_prefs", Context.MODE_PRIVATE);
            prefs.edit().putString("student_id", finalId).apply();
            extractedId = finalId;
            Toast.makeText(requireContext(), "Saved student ID: " + finalId, Toast.LENGTH_SHORT).show();
        });

        return root;
    }

    private void ensureCameraAndCapture() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        } else {
            cameraLauncher.launch(null);
        }
    }

    private void runTextRecognition(@NonNull Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        com.google.mlkit.vision.text.TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(this::onTextRecognized)
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Text recognition failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void onTextRecognized(@NonNull Text result) {
        String fullText = result.getText();
        textRecognized.setText(fullText);

        // Improved heuristic: choose the longest numeric sequence (likely the ID).
        String[] lines = fullText.split("\n");
        Pattern digitPattern = Pattern.compile("\\d+");
        String bestDigits = null;
        int bestLen = 0;

        for (String line : lines) {
            Matcher m = digitPattern.matcher(line);
            while (m.find()) {
                String candidate = m.group();
                int len = candidate.length();
                if (len >= 4 && len > bestLen) {
                    bestLen = len;
                    bestDigits = candidate;
                }
            }
        }

        if (bestDigits != null) {
            extractedId = bestDigits;
        } else {
            // Fallback: pick the longest non-empty token from the recognized text
            String fallback = null;
            int bestTokenLen = 0;
            for (String line : lines) {
                String[] tokens = line.trim().split("\\s+");
                for (String token : tokens) {
                    if (token.length() > bestTokenLen) {
                        bestTokenLen = token.length();
                        fallback = token;
                    }
                }
            }
            extractedId = fallback != null ? fallback : "";
        }

        if (editDetectedId != null) {
            editDetectedId.setText(extractedId);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                cameraLauncher.launch(null);
            } else {
                if (getContext() != null) {
                    Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
