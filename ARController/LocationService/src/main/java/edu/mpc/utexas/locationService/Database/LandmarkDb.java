package edu.mpc.utexas.locationService.Database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import static edu.mpc.utexas.locationService.utility.Constant.DB_LDMK_TABLE;

@Entity(tableName = DB_LDMK_TABLE)
public class LandmarkDb {
    @PrimaryKey
    @NonNull
    public String addr;

    public int type;
    public double x, y;
    public double varX, varY;


}
