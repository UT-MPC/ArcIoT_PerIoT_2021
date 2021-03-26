package edu.mpc.utexas.locationService.Database;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import static edu.mpc.utexas.locationService.utility.Constant.DB_LDMK_TABLE;

@Dao
public interface LandmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(LandmarkDb landmarkDb);

    @Update
    void update(LandmarkDb... landmarks);

    @Query("SELECT * FROM " + DB_LDMK_TABLE)
    List<LandmarkDb> getSavedLandmarks();

    @Query("SELECT * FROM " + DB_LDMK_TABLE + " WHERE addr=:addr")
    LandmarkDb getLandmarkDbByAddr(String addr);

    @Query("DELETE FROM " + DB_LDMK_TABLE + " WHERE addr = :addr")
    void deleteByAddr(String addr);

    @Query("DELETE FROM " + DB_LDMK_TABLE)
    void deleteAll();
}
