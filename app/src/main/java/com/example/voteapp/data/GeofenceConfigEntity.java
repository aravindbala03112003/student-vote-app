package com.example.voteapp.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "geofence_config")
public class GeofenceConfigEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public double centerLat;
    public double centerLng;
    public float radiusMeters;

    // For Session Management (Module 4.4)
    public long startTime;
    public long endTime;
    public boolean isActive = true;

    public GeofenceConfigEntity() {
        // Required for Firebase
    }

    @Ignore
    public GeofenceConfigEntity(double centerLat, double centerLng, float radiusMeters) {
        this.centerLat = centerLat;
        this.centerLng = centerLng;
        this.radiusMeters = radiusMeters;
    }
}
