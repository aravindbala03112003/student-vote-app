package com.example.voteapp.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                VoteEntity.class,
                BroadcastMessageEntity.class,
                UserEntity.class,
                CandidateEntity.class,
                GeofenceConfigEntity.class
        },
        version = 8,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract VoteDao voteDao();

    public abstract BroadcastDao broadcastDao();

    public abstract UserDao userDao();

    public abstract CandidateDao candidateDao();
    
    public abstract GeofenceConfigDao geofenceConfigDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "vote_db"
                            )
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
