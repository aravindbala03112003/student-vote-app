package com.example.voteapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.voteapp.MainActivity;
import com.example.voteapp.R;
import com.example.voteapp.util.SessionManager;
import com.google.android.material.button.MaterialButton;

public class HomeFragment extends Fragment {

    public HomeFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        TextView textWelcome = root.findViewById(R.id.textWelcome);
        TextView textDescription = root.findViewById(R.id.textDescription);

        MaterialButton buttonGoToVote = root.findViewById(R.id.buttonGoToVote);
        MaterialButton buttonGoToScan = root.findViewById(R.id.buttonGoToScan);
        MaterialButton buttonGoToBroadcasts = root.findViewById(R.id.buttonGoToBroadcasts);
        MaterialButton buttonGoToGeofence = root.findViewById(R.id.buttonGoToGeofence);
        MaterialButton buttonGoToResults = root.findViewById(R.id.buttonGoToResults);
        MaterialButton buttonGoToCandidates = root.findViewById(R.id.buttonGoToCandidates);
        MaterialButton buttonGoToProfile = root.findViewById(R.id.buttonGoToProfile);

        String name = SessionManager.getUserName(requireContext());
        if (name != null && !name.isEmpty()) {
            textWelcome.setText("Welcome, " + name);
        }

        String department = SessionManager.getDepartment(requireContext());
        String year = SessionManager.getYear(requireContext());
        StringBuilder details = new StringBuilder();
        if (department != null && !department.isEmpty()) {
            details.append("Dept: ").append(department);
        }
        if (year != null && !year.isEmpty()) {
            if (details.length() > 0) details.append(" | ");
            details.append("Year: ").append(year);
        }
        textDescription.setText(details.toString());

        boolean isAdmin = SessionManager.isAdmin(requireContext());
        boolean isCandidate = SessionManager.isCandidate(requireContext());

        if (isAdmin) {
            if (buttonGoToVote != null) buttonGoToVote.setVisibility(View.VISIBLE);
            if (buttonGoToScan != null) buttonGoToScan.setVisibility(View.GONE);
            if (buttonGoToBroadcasts != null) buttonGoToBroadcasts.setVisibility(View.VISIBLE);
            if (buttonGoToResults != null) buttonGoToResults.setVisibility(View.VISIBLE);
            if (buttonGoToCandidates != null) buttonGoToCandidates.setVisibility(View.VISIBLE);
            if (buttonGoToProfile != null) buttonGoToProfile.setVisibility(View.GONE);
        } else if (isCandidate) {
            if (buttonGoToVote != null) buttonGoToVote.setVisibility(View.GONE); // Hide voting for candidates
            if (buttonGoToScan != null) buttonGoToScan.setVisibility(View.GONE);
            if (buttonGoToBroadcasts != null) buttonGoToBroadcasts.setVisibility(View.GONE);
            if (buttonGoToResults != null) buttonGoToResults.setVisibility(View.GONE);
            if (buttonGoToCandidates != null) buttonGoToCandidates.setVisibility(View.GONE);
            if (buttonGoToProfile != null) buttonGoToProfile.setVisibility(View.VISIBLE);
        } else {
            // Student
            if (buttonGoToVote != null) buttonGoToVote.setVisibility(View.VISIBLE);
            if (buttonGoToScan != null) buttonGoToScan.setVisibility(View.VISIBLE);
            if (buttonGoToBroadcasts != null) buttonGoToBroadcasts.setVisibility(View.GONE);
            if (buttonGoToResults != null) buttonGoToResults.setVisibility(View.GONE);
            if (buttonGoToCandidates != null) buttonGoToCandidates.setVisibility(View.GONE);
            if (buttonGoToProfile != null) buttonGoToProfile.setVisibility(View.GONE);
        }

        View.OnClickListener listener = v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity main = (MainActivity) getActivity();
                if (v == buttonGoToVote) main.navigateTo(R.id.navigation_vote);
                else if (v == buttonGoToScan) main.navigateTo(R.id.navigation_scan);
                else if (v == buttonGoToBroadcasts) main.navigateTo(R.id.navigation_broadcast);
                else if (v == buttonGoToGeofence) main.openGeofence();
                else if (v == buttonGoToResults) main.openResults();
                else if (v == buttonGoToCandidates) main.openCandidateManagement();
                else if (v == buttonGoToProfile) main.navigateTo(R.id.navigation_profile);
            }
        };

        if (buttonGoToVote != null) buttonGoToVote.setOnClickListener(listener);
        if (buttonGoToScan != null) buttonGoToScan.setOnClickListener(listener);
        if (buttonGoToBroadcasts != null) buttonGoToBroadcasts.setOnClickListener(listener);
        if (buttonGoToGeofence != null) buttonGoToGeofence.setOnClickListener(listener);
        if (buttonGoToResults != null) buttonGoToResults.setOnClickListener(listener);
        if (buttonGoToCandidates != null) buttonGoToCandidates.setOnClickListener(listener);
        if (buttonGoToProfile != null) buttonGoToProfile.setOnClickListener(listener);

        return root;
    }
}
