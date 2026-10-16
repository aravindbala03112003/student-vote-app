package com.example.voteapp.ui;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.voteapp.R;
import com.example.voteapp.data.GeofenceConfigEntity;
import com.example.voteapp.util.SessionManager;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class GeofenceFragment extends Fragment {

    private static final int REQ_LOCATION = 1002;

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference mDatabase;
    private ValueEventListener geofenceListener;

    private TextView textCurrentConfig, textSelectedTimes;
    private TextInputEditText editLat, editLng, editRadius;
    private MaterialButton buttonSetLocation, buttonSaveGeofence, buttonClearGeofence;
    private MaterialButton buttonStart, buttonEnd;

    private long selectedStartTime = 0;
    private long selectedEndTime = 0;

    public GeofenceFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_geofence, container, false);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        textCurrentConfig = root.findViewById(R.id.textCurrentConfig);
        textSelectedTimes = root.findViewById(R.id.textSelectedTimes);
        editLat = root.findViewById(R.id.editLat);
        editLng = root.findViewById(R.id.editLng);
        editRadius = root.findViewById(R.id.editRadius);

        buttonSetLocation = root.findViewById(R.id.buttonSetLocation);
        buttonSaveGeofence = root.findViewById(R.id.buttonSaveGeofence);
        buttonClearGeofence = root.findViewById(R.id.buttonClearGeofence);
        buttonStart = root.findViewById(R.id.buttonPickStartTime);
        buttonEnd = root.findViewById(R.id.buttonPickEndTime);

        fetchGeofenceFromFirebase();

        boolean isAdmin = SessionManager.isAdmin(requireContext());
        if (!isAdmin) {
            buttonSetLocation.setVisibility(View.GONE);
            buttonSaveGeofence.setVisibility(View.GONE);
            buttonClearGeofence.setVisibility(View.GONE);
            buttonStart.setVisibility(View.GONE);
            buttonEnd.setVisibility(View.GONE);
            editLat.setEnabled(false);
            editLng.setEnabled(false);
            editRadius.setEnabled(false);
        } else {
            buttonSetLocation.setOnClickListener(v -> checkPermissionAndSetLocation());
            buttonSaveGeofence.setOnClickListener(v -> saveConfigToFirebase());
            buttonClearGeofence.setOnClickListener(v -> clearGeofenceOnFirebase());
            buttonStart.setOnClickListener(v -> pickDateTime(true));
            buttonEnd.setOnClickListener(v -> pickDateTime(false));
        }

        return root;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (geofenceListener != null) {
            mDatabase.child("geofence").removeEventListener(geofenceListener);
            geofenceListener = null;
        }
    }

    private void pickDateTime(boolean isStart) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            new TimePickerDialog(requireContext(), (view1, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);

                if (isStart) selectedStartTime = calendar.getTimeInMillis();
                else selectedEndTime = calendar.getTimeInMillis();

                updateSelectedTimesUI();
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateSelectedTimesUI() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        String startStr = selectedStartTime == 0 ? "Not set" : sdf.format(new Date(selectedStartTime));
        String endStr = selectedEndTime == 0 ? "Not set" : sdf.format(new Date(selectedEndTime));
        textSelectedTimes.setText(String.format("Start: %s\nEnd: %s", startStr, endStr));
    }

    private void fetchGeofenceFromFirebase() {
        geofenceListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                if (snapshot.exists()) {
                    GeofenceConfigEntity config = snapshot.getValue(GeofenceConfigEntity.class);
                    if (config != null) {
                        updateUIWithConfig(config);
                    }
                } else {
                    textCurrentConfig.setText("Current session: Not Configured");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        mDatabase.child("geofence").addValueEventListener(geofenceListener);
    }

    private void updateUIWithConfig(GeofenceConfigEntity config) {
        if (config == null || !isAdded()) return;
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        String sessionStr = String.format("Session: %s - %s\nRadius: %.1fm",
                sdf.format(new Date(config.startTime)),
                sdf.format(new Date(config.endTime)),
                config.radiusMeters);
        textCurrentConfig.setText(sessionStr);

        if (editLat.getText().toString().isEmpty()) {
            editLat.setText(String.valueOf(config.centerLat));
            editLng.setText(String.valueOf(config.centerLng));
            editRadius.setText(String.valueOf(config.radiusMeters));
            selectedStartTime = config.startTime;
            selectedEndTime = config.endTime;
            updateSelectedTimesUI();
        }
    }

    private void checkPermissionAndSetLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
        } else {
            fetchLocationAndPopulate();
        }
    }

    private void fetchLocationAndPopulate() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        // First check if location providers are enabled
        LocationManager lm = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        if(!gps_enabled && !network_enabled) {
            Toast.makeText(requireContext(), "Location services are disabled. Please enable GPS and Wireless networks in settings.", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(requireContext(), "Fetching fresh location...", Toast.LENGTH_SHORT).show();

        // Request a fresh high-accuracy location
        CurrentLocationRequest request = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();

        fusedLocationClient.getCurrentLocation(request, null).addOnSuccessListener(location -> {
            if (location != null) {
                editLat.setText(String.format(Locale.US, "%.6f", location.getLatitude()));
                editLng.setText(String.format(Locale.US, "%.6f", location.getLongitude()));
                Toast.makeText(requireContext(), "Location updated successfully", Toast.LENGTH_SHORT).show();
            } else {
                // Fallback to last known location if fresh location fails
                fusedLocationClient.getLastLocation().addOnSuccessListener(lastLoc -> {
                    if (lastLoc != null) {
                        editLat.setText(String.format(Locale.US, "%.6f", lastLoc.getLatitude()));
                        editLng.setText(String.format(Locale.US, "%.6f", lastLoc.getLongitude()));
                        Toast.makeText(requireContext(), "Using last known location", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Unable to determine location. Try moving outdoors.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void saveConfigToFirebase() {
        try {
            double lat = Double.parseDouble(editLat.getText().toString());
            double lng = Double.parseDouble(editLng.getText().toString());
            float rad = Float.parseFloat(editRadius.getText().toString());

            if (rad <= 0f) {
                Toast.makeText(requireContext(), "Radius must be greater than zero", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedStartTime == 0 || selectedEndTime == 0) {
                Toast.makeText(requireContext(), "Please set start and end times", Toast.LENGTH_SHORT).show();
                return;
            }

            GeofenceConfigEntity config = new GeofenceConfigEntity(lat, lng, rad);
            config.startTime = selectedStartTime;
            config.endTime = selectedEndTime;
            config.isActive = true;

            mDatabase.child("geofence").setValue(config)
                    .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Session saved to Cloud", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Error saving to Cloud: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Invalid input format. Check numbers.", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearGeofenceOnFirebase() {
        mDatabase.child("geofence").removeValue()
                .addOnSuccessListener(aVoid -> {
                    textCurrentConfig.setText("Current session: Not Configured");
                    Toast.makeText(requireContext(), "Session cleared", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQ_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocationAndPopulate();
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
