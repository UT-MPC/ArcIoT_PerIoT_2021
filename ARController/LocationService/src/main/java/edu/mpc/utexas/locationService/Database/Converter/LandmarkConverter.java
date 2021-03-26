package edu.mpc.utexas.locationService.Database.Converter;

import java.util.ArrayList;
import java.util.List;

import edu.mpc.utexas.locationService.Database.LandmarkDb;
import edu.mpc.utexas.locationService.service.Landmark.Landmark;

public class LandmarkConverter {
    private static final String TAG = "LandmarkConverter";

    public static Landmark toLandmark(LandmarkDb landmarkDb) {
        if (landmarkDb == null) {
            return null;
        } else {
            return new Landmark(landmarkDb.type,
                    landmarkDb.x, landmarkDb.y,
                    landmarkDb.varX, landmarkDb.varY, landmarkDb.addr);
        }
    }

    public static LandmarkDb toLandmarkDb(Landmark ldmk) {
        if (ldmk == null || ldmk.addr == null)
            return null;
        LandmarkDb newDb = new LandmarkDb();
        newDb.addr = ldmk.addr;
        newDb.type = ldmk.type;
        newDb.x = ldmk.x;
        newDb.y = ldmk.y;
        newDb.varX = ldmk.varX;
        newDb.varY = ldmk.varY;
        return newDb;
    }

    public static List<Landmark> toLandmarks(List<LandmarkDb> landmarkDbs) {
        List<Landmark> landmarks = new ArrayList<>();
        for (LandmarkDb l : landmarkDbs) {
            landmarks.add(toLandmark(l));
        }
        return landmarks;
    }

    public static List<LandmarkDb> toLandmarkDbs(List<Landmark> landmarks) {
        List<LandmarkDb> landmarkDbs = new ArrayList<>();
        if (landmarks == null) return landmarkDbs;
        for (Landmark l : landmarks) {
            landmarkDbs.add(toLandmarkDb(l));
        }
        return landmarkDbs;
    }
}
