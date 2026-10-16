package com.example.voteapp.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface GeofenceConfigDao {
    @Query("SELECT * FROM geofence_config LIMIT 1")
    GeofenceConfigEntity getConfig();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(GeofenceConfigEntity config);

    @Query("DELETE FROM geofence_config")
    void clear();
}
