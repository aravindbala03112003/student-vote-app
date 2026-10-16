package com.example.voteapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.voteapp.ui.BroadcastFragment;
import com.example.voteapp.ui.GeofenceFragment;
import com.example.voteapp.ui.HomeFragment;
import com.example.voteapp.ui.ResultsFragment;
import com.example.voteapp.ui.CandidateManagementFragment;
import com.example.voteapp.ui.ScanIdFragment;
import com.example.voteapp.ui.VoteFragment;
import com.example.voteapp.ui.CandidateProfileFragment;
import com.example.voteapp.util.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private MaterialToolbar topAppBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        topAppBar = findViewById(R.id.topAppBar);
        topAppBar.inflateMenu(R.menu.top_appbar_menu);
        topAppBar.setOnMenuItemClickListener(this::onTopAppBarMenuItemClicked);
        updateTopAppBarMenuVisibility();

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        updateBottomNavVisibility();

        bottomNavigationView.setOnItemSelectedListener(item -> {
            navigateTo(item.getItemId());
            return true;
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_home);
            navigateTo(R.id.navigation_home);
        }
    }

    private void updateTopAppBarMenuVisibility() {
        if (topAppBar == null || topAppBar.getMenu() == null) return;
        MenuItem clearItem = topAppBar.getMenu().findItem(R.id.action_clear_data);
        if (clearItem != null) {
            clearItem.setVisible(SessionManager.isAdmin(this));
        }
    }

    private boolean onTopAppBarMenuItemClicked(@NonNull MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.action_logout) {
            logout();
            return true;
        } else if (itemId == R.id.action_clear_data) {
            if (!SessionManager.isAdmin(this)) {
                Toast.makeText(this, "Admin permission required", Toast.LENGTH_SHORT).show();
                return true;
            }
            new AlertDialog.Builder(this)
                .setTitle("Clear All Data")
                .setMessage("This will delete all candidates, votes, and geofence data from the Cloud. Are you sure?")
                .setPositiveButton("Clear Data", (dialog, which) -> clearCloudDatabaseBranches())
                .setNegativeButton(android.R.string.cancel, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
            return true;
        }
        return false;
    }

    private void clearCloudDatabaseBranches() {
        if (!SessionManager.isAdmin(this)) {
            Toast.makeText(this, "Admin permission required", Toast.LENGTH_SHORT).show();
            return;
        }
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        Map<String, Object> updates = new HashMap<>();
        updates.put("candidates", null);
        updates.put("votes", null);
        updates.put("broadcasts", null);
        updates.put("geofence", null);
        
        rootRef.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(MainActivity.this, "Election Data Cleared Successfully", Toast.LENGTH_SHORT).show();
                navigateTo(R.id.navigation_home);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(MainActivity.this, "Clear Failed: Check Firebase Rules", Toast.LENGTH_LONG).show();
            });
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        SessionManager.clearSession(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void updateBottomNavVisibility() {
        if (bottomNavigationView == null) return;
        
        boolean isAdmin = SessionManager.isAdmin(this);
        boolean isCandidate = SessionManager.isCandidate(this);

        MenuItem scanItem = bottomNavigationView.getMenu().findItem(R.id.navigation_scan);
        if (scanItem != null) scanItem.setVisible(!isAdmin && !isCandidate); 

        MenuItem voteItem = bottomNavigationView.getMenu().findItem(R.id.navigation_vote);
        if (voteItem != null) voteItem.setVisible(!isCandidate); // Hide vote tab for candidates

        MenuItem profileItem = bottomNavigationView.getMenu().findItem(R.id.navigation_profile);
        if (profileItem != null) profileItem.setVisible(isCandidate);

        MenuItem broadcastItem = bottomNavigationView.getMenu().findItem(R.id.navigation_broadcast);
        if (broadcastItem != null) broadcastItem.setVisible(isAdmin); 
    }

    public void navigateTo(int itemId) {
        Fragment fragment;
        String title;
        if (itemId == R.id.navigation_vote) {
            fragment = new VoteFragment();
            title = "Vote";
        } else if (itemId == R.id.navigation_scan) {
            fragment = new ScanIdFragment();
            title = "Scan ID";
        } else if (itemId == R.id.navigation_profile) {
            fragment = new CandidateProfileFragment();
            title = "Profile";
        } else if (itemId == R.id.navigation_broadcast) {
            fragment = new BroadcastFragment();
            title = "Broadcasts";
        } else if (itemId == R.id.navigation_home) {
            fragment = new HomeFragment();
            title = getString(R.string.app_name);
        } else {
            // Default fallback
            fragment = new HomeFragment();
            title = getString(R.string.app_name);
        }

        switchFragment(fragment, title);
    }

    public void openGeofence() {
        switchFragment(new GeofenceFragment(), "Campus");
    }

    public void openResults() {
        switchFragment(new ResultsFragment(), "Results");
    }

    public void openCandidateManagement() {
        switchFragment(new CandidateManagementFragment(), "Candidates");
    }

    private void switchFragment(Fragment fragment, String title) {
        topAppBar.setTitle(title);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
