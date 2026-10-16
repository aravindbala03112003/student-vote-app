package com.example.voteapp.ui;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.example.voteapp.R;
import com.example.voteapp.data.AppDatabase;
import com.example.voteapp.data.CandidateDao;
import com.example.voteapp.data.CandidateEntity;
import com.example.voteapp.data.GeofenceConfigDao;
import com.example.voteapp.data.GeofenceConfigEntity;
import com.example.voteapp.data.VoteDao;
import com.example.voteapp.data.VoteEntity;
import com.example.voteapp.util.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class VoteFragment extends Fragment {

    private static final int REQ_LOCATION = 1001;

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference mDatabase;

    private AppDatabase appDatabase;
    private CandidateDao candidateDao;
    private VoteDao voteDao;
    private GeofenceConfigDao geofenceDao;

    private ValueEventListener candidateListener;
    private ValueEventListener votesListener;
    private ValueEventListener geofenceListener;

    private TextView textLocationStatus, textStudentIdStatus, textNoCandidates;
    private RecyclerView recyclerCandidates;
    private VoteCandidateAdapter candidateAdapter;

    private boolean insideCampus = false;
    private boolean sessionActive = false;
    private String currentStudentId;
    private Location lastKnownLocation;

    private List<CandidateEntity> latestCandidates = new ArrayList<>();

    public VoteFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_vote, container, false);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        appDatabase = AppDatabase.getInstance(requireContext());
        candidateDao = appDatabase.candidateDao();
        voteDao = appDatabase.voteDao();
        geofenceDao = appDatabase.geofenceConfigDao();

        textLocationStatus = root.findViewById(R.id.textLocationStatus);
        textStudentIdStatus = root.findViewById(R.id.textStudentIdStatus);
        recyclerCandidates = root.findViewById(R.id.recyclerVoteCandidates);
        textNoCandidates = root.findViewById(R.id.textNoCandidates);

        recyclerCandidates.setLayoutManager(new LinearLayoutManager(requireContext()));
        candidateAdapter = new VoteCandidateAdapter(SessionManager.isAdmin(requireContext()), candidate -> tryVote(candidate.name));
        recyclerCandidates.setAdapter(candidateAdapter);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStudentId();
        loadCachedSession();
        checkLocationAndSession();
        loadCandidates();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (candidateListener != null) {
            mDatabase.child("candidates").removeEventListener(candidateListener);
            candidateListener = null;
        }
        if (votesListener != null) {
            mDatabase.child("votes").removeEventListener(votesListener);
            votesListener = null;
        }
        if (geofenceListener != null) {
            mDatabase.child("geofence").removeEventListener(geofenceListener);
            geofenceListener = null;
        }
    }

    private void loadStudentId() {
        SharedPreferences prefs = requireContext().getSharedPreferences("vote_prefs", Context.MODE_PRIVATE);
        currentStudentId = prefs.getString("student_id", null);
        if (textStudentIdStatus != null) {
            textStudentIdStatus.setText(currentStudentId == null ? "Please scan your student ID" : "ID: " + currentStudentId);
        }
    }

    private void loadCachedSession() {
        GeofenceConfigEntity cached = geofenceDao.getConfig();
        evaluateSessionWithConfig(cached);
    }

    private void checkLocationAndSession() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
            return;
        }

        geofenceListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                GeofenceConfigEntity config = snapshot.getValue(GeofenceConfigEntity.class);
                if (config != null) {
                    geofenceDao.clear();
                    geofenceDao.insert(config);
                }
                evaluateSessionWithConfig(config);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                evaluateSessionWithConfig(geofenceDao.getConfig());
            }
        };
        mDatabase.child("geofence").addValueEventListener(geofenceListener);
    }

    private void evaluateSessionWithConfig(GeofenceConfigEntity config) {
        if (!isAdded()) return;
        if (config == null) {
            sessionActive = false;
            insideCampus = false;
            textLocationStatus.setText("No active session configured.");
            return;
        }

        long now = System.currentTimeMillis();
        sessionActive = config.isActive && now >= config.startTime && now <= config.endTime;

        if (!sessionActive) {
            insideCampus = false;
            updateStatusText();
            return;
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            textLocationStatus.setText("Location permission required to validate radius.");
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    lastKnownLocation = location;
                    if (location != null) {
                        float[] results = new float[1];
                        Location.distanceBetween(location.getLatitude(), location.getLongitude(), config.centerLat, config.centerLng, results);
                        insideCampus = results[0] <= config.radiusMeters;
                    } else {
                        insideCampus = false;
                    }
                    updateStatusText();
                })
                .addOnFailureListener(e -> {
                    insideCampus = false;
                    updateStatusText();
                });
    }

    private void updateStatusText() {
        String msg;
        if (!sessionActive) msg = "Voting session is closed.";
        else if (!insideCampus) msg = "You are outside campus boundaries.";
        else msg = "System ready. Cast your vote.";
        textLocationStatus.setText(msg);
    }

    private void loadCandidates() {
        loadCandidatesFromCache();

        candidateListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<CandidateEntity> candidates = new ArrayList<>();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    CandidateEntity c = postSnapshot.getValue(CandidateEntity.class);
                    if (c != null) {
                        if (c.firebaseId == null || c.firebaseId.isEmpty()) {
                            String key = postSnapshot.getKey();
                            c.firebaseId = key != null ? key : (c.name != null ? c.name : String.valueOf(candidates.size()));
                        }
                        candidates.add(c);
                    }
                }
                latestCandidates = candidates;
                candidateDao.clearAll();
                for (CandidateEntity c : candidates) {
                    candidateDao.insert(c);
                }
                attachVotesListener();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Keep showing cached data
            }
        };
        mDatabase.child("candidates").addValueEventListener(candidateListener);
    }

    private void attachVotesListener() {
        if (votesListener != null) {
            mDatabase.child("votes").removeEventListener(votesListener);
        }
        votesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot voteSnapshot) {
                List<VoteEntity> votes = new ArrayList<>();
                for (DataSnapshot vs : voteSnapshot.getChildren()) {
                    VoteEntity v = vs.getValue(VoteEntity.class);
                    if (v != null && v.candidateName != null) {
                        votes.add(v);
                    }
                }

                voteDao.clearAll();
                for (VoteEntity v : votes) {
                    voteDao.insert(v);
                }

                displayCandidates(latestCandidates, votes);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                List<VoteEntity> cachedVotes = voteDao.getAllVotes();
                displayCandidates(latestCandidates, cachedVotes);
            }
        };
        mDatabase.child("votes").addValueEventListener(votesListener);
    }

    private void loadCandidatesFromCache() {
        List<CandidateEntity> cachedCandidates = candidateDao.getAll();
        if (!cachedCandidates.isEmpty()) {
            latestCandidates = cachedCandidates;
            List<VoteEntity> cachedVotes = voteDao.getAllVotes();
            displayCandidates(cachedCandidates, cachedVotes);
        }
    }

    private void displayCandidates(List<CandidateEntity> candidates, List<VoteEntity> votes) {
        if (!isAdded()) return;
        Map<String, Integer> counts = new HashMap<>();
        if (votes != null) {
            for (VoteEntity v : votes) {
                if (v != null && v.candidateName != null) {
                    counts.put(v.candidateName, counts.getOrDefault(v.candidateName, 0) + 1);
                }
            }
        }

        List<VoteCandidateAdapter.Row> rows = new ArrayList<>();
        if (candidates != null) {
            for (CandidateEntity c : candidates) {
                rows.add(new VoteCandidateAdapter.Row(c, counts.getOrDefault(c.name, 0)));
            }
        }
        candidateAdapter.submitList(rows);
        if (textNoCandidates != null) {
            textNoCandidates.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void tryVote(String candidateName) {
        if (SessionManager.isAdmin(requireContext())) {
            Toast.makeText(requireContext(), "Admins cannot vote", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!sessionActive) {
            Toast.makeText(requireContext(), "Voting session not active", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!insideCampus) {
            Toast.makeText(requireContext(), "Must be inside campus to vote", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentStudentId == null) {
            Toast.makeText(requireContext(), "Scan student ID first", Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.child("votes").child(currentStudentId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(requireContext(), "Already voted", Toast.LENGTH_SHORT).show();
                } else {
                    showFeedbackDialog(candidateName);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showFeedbackDialog(String candidateName) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_feedback, null);
        TextView textTitle = dialogView.findViewById(R.id.textFeedbackCandidate);
        TextInputEditText editFeedback = dialogView.findViewById(R.id.editFeedbackText);
        MaterialButton btnCancel = dialogView.findViewById(R.id.buttonCancelFeedback);
        MaterialButton btnSubmit = dialogView.findViewById(R.id.buttonSubmitVote);

        textTitle.setText("Why are you voting for " + candidateName + "?");

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSubmit.setOnClickListener(v -> {
            String feedback = editFeedback.getText() != null ? editFeedback.getText().toString().trim() : "";
            dialog.dismiss();
            startBiometricAuth(candidateName, feedback);
        });

        dialog.show();
    }

    private void startBiometricAuth(String candidateName, String feedback) {
        BiometricManager manager = BiometricManager.from(requireContext());
        if (manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) {
            castVote(candidateName, feedback);
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(requireContext());
        BiometricPrompt prompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                castVote(candidateName, feedback);
            }
        });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Verification")
                .setSubtitle("Confirm your identity to vote")
                .setNegativeButtonText("Cancel")
                .build();
        prompt.authenticate(info);
    }

    private void castVote(String candidateName, String feedback) {
        VoteEntity vote = new VoteEntity(currentStudentId, candidateName, System.currentTimeMillis());
        if (lastKnownLocation != null) {
            vote.latitude = lastKnownLocation.getLatitude();
            vote.longitude = lastKnownLocation.getLongitude();
        }
        vote.feedback = feedback;

        voteDao.insert(vote);

        mDatabase.child("votes").child(currentStudentId).setValue(vote).addOnSuccessListener(aVoid -> {
            Toast.makeText(requireContext(), "Vote cast for " + candidateName, Toast.LENGTH_SHORT).show();
            if (getView() != null) {
                LottieAnimationView success = getView().findViewById(R.id.lottieVoteSuccess);
                if (success != null) {
                    success.setVisibility(View.VISIBLE);
                    success.playAnimation();
                }
            }
        }).addOnFailureListener(e -> Toast.makeText(requireContext(), "Saved locally, sync when online", Toast.LENGTH_SHORT).show());
    }
}
