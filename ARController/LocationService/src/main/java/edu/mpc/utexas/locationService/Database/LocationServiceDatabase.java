package edu.mpc.utexas.locationService.Database;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import edu.mpc.utexas.locationService.Database.Converter.LandmarkConverter;
import edu.mpc.utexas.locationService.service.Landmark.Landmark;
import edu.mpc.utexas.locationService.service.LocationService;

@Database(entities = {LandmarkDb.class},
        version = 1,
        exportSchema = false)
public abstract class LocationServiceDatabase extends RoomDatabase {
    private static final String TAG = "LocationServiceDatabase";

    private static LocationServiceDatabase INSTANCE;
    private static LocationServiceDatabase SNAPSHOT_INSTANCE;

    public abstract LandmarkDao landmarkDbDao();

    public static void initializeDatabase(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context, LocationServiceDatabase.class, "AppDatabase")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
        }
    }

    public static void initializeDatabaseSnapshot(Context context) {
        if (SNAPSHOT_INSTANCE == null) {
            SNAPSHOT_INSTANCE = Room.databaseBuilder(context, LocationServiceDatabase.class, "Snapshot")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
        }
    }

    public static LocationServiceDatabase getDatabase() throws NullPointerException {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        else {
            throw new NullPointerException("AppDatabase has not been initialized");
        }
    }

    public static LocationServiceDatabase getSnapshotDatabase() throws NullPointerException {
        if (SNAPSHOT_INSTANCE != null) {
            return SNAPSHOT_INSTANCE;
        }
        else {
            throw new NullPointerException("AppDatabase has not been initialized");
        }
    }

    public Landmark getLandmarkByAddr(String addr) {
        if (addr == null) return null;

        LandmarkDb landmarkDb = getDatabase().landmarkDbDao().getLandmarkDbByAddr(addr);

        return LandmarkConverter.toLandmark(landmarkDb);
    }

    public List<Landmark> getAllLandmarks() {
        List<LandmarkDb> landmarkDbs = getDatabase().landmarkDbDao().getSavedLandmarks();
        return LandmarkConverter.toLandmarks(landmarkDbs);
    }

    public void saveLandmark(Landmark landmark) {
        LandmarkDb ldmkDb = LandmarkConverter.toLandmarkDb(landmark);
        if (ldmkDb == null) return;

        getDatabase().landmarkDbDao().insert(ldmkDb);
    }

    public void saveLandmarks(Iterable<Landmark> landmarks) {
        for (Landmark l: landmarks) {
            saveLandmark(l);
        }
    }

    public void deleteAllLandmarks() {
        getDatabase().landmarkDbDao().deleteAll();
    }

    public void deleteLandmark(String addr) {
        getDatabase().landmarkDbDao().deleteByAddr(addr);
    }

    public void saveLandmarkSnapshot(Iterable<Landmark> landmarks) {
        for (Landmark l : landmarks) {
            LandmarkDb ldmkDb = LandmarkConverter.toLandmarkDb(l);
            if (ldmkDb == null) return;
            getSnapshotDatabase().landmarkDbDao().insert(ldmkDb);
        }
    }

    public List<Landmark> getSnapshotLandmarks() {
        List<LandmarkDb> landmarkDbs = getSnapshotDatabase().landmarkDbDao().getSavedLandmarks();
        return LandmarkConverter.toLandmarks(landmarkDbs);
    }
}
