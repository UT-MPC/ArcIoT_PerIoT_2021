package edu.mpc.utexas.locationService.UI;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

import edu.mpc.utexas.locationService.service.Landmark.Landmark;

import static android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
import static android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_LOW;
import static android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM;
import static edu.mpc.utexas.locationService.utility.MathFunc.polarToCartesian;

public class DrawView extends View {
    // Constants
    private final int userSize = 10;

    private final float dircLen = 50;
    private final float meterToPixel = 80;
    private final int estColor = Color.GRAY;

    private float[] userPos;
    private float[] userDir;

    private Paint userPaint;
    private Paint ldmkPaint;

    private Map<String, Landmark> savedLandmarks;


    private float[] canvasCenter;

    private int magSensorAcc = SENSOR_STATUS_ACCURACY_MEDIUM;
    public static final Map<Integer, Integer> magAccColor = new HashMap<Integer, Integer>(){{
        put(SENSOR_STATUS_ACCURACY_LOW, Color.RED);
        put(SENSOR_STATUS_ACCURACY_MEDIUM, Color.GREEN);
        put(SENSOR_STATUS_ACCURACY_HIGH, Color.BLUE);
    }};

    private Map<String, Integer> markedLandmark;

    public DrawView(Context ctx) {
        super(ctx);
        setUserCircle();
        setLandmarkCircle();
//        beaconColor.put("C2:84:A9:53:29:BB", Color.GREEN);      // 18
//        beaconColor.put("DB:18:83:20:34:23", Color.BLUE);       // 14
        markedLandmark = new HashMap<>();
    }


    private void setUserCircle() {
        userPaint = new Paint();
        userPaint.setAntiAlias(true);
        userPaint.setStyle(Paint.Style.STROKE);
        userPaint.setStrokeJoin(Paint.Join.ROUND);
        userPaint.setStrokeWidth(4f);

        userDir= new float[]{0,1};
    }

    private void setLandmarkCircle() {
        ldmkPaint = new Paint();
        ldmkPaint.setAntiAlias(true);
        ldmkPaint.setStyle(Paint.Style.STROKE);
    }

    private void drawUser(Canvas canvas) {
        userPaint.setColor(magAccColor.getOrDefault(magSensorAcc, Color.BLACK));
        canvas.drawCircle(userPos[0], userPos[1], userSize, userPaint);
        canvas.drawLine(userPos[0], userPos[1], userPos[0] + userDir[0] * dircLen, userPos[1] + userDir[1] * dircLen, userPaint);
    }

    public void markLandmark(String addr, Integer c) {
        markedLandmark.clear();
        markedLandmark.put(addr, c);
    }

    private void drawLandmarks(Canvas canvas) {
        if (savedLandmarks == null) {
            return;
        }
        for (Map.Entry<String, Landmark> landmark : savedLandmarks.entrySet()) {
            Landmark l = landmark.getValue();
            if (l.type == Landmark.EST) {
                float[] pos = coordToPixel(new double[]{l.x, -l.y});
                float rx = (float)Math.sqrt(l.varX) * meterToPixel;
                float ry = (float)Math.sqrt(l.varY) * meterToPixel;
//                Log.d("Draw ", "??" + landmark.getKey());
                ldmkPaint.setColor(markedLandmark.getOrDefault(landmark.getKey(), estColor));
                canvas.drawOval(pos[0] - rx, pos[1] - ry, pos[0] + rx, pos[1] + ry, ldmkPaint);
            }
        }
    }

    public void updateOri(double heading, int acc) {
        userDir[0] = (float)polarToCartesian(1, heading)[0];
        userDir[1] = -(float)polarToCartesian(1, heading)[1];
        magSensorAcc = acc;
        this.invalidate();
    }

    private float[] coordToPixel(double coord[]) {
        return new float[]{(float)coord[0] * meterToPixel + canvasCenter[0], (float)coord[1] * meterToPixel + canvasCenter[1]};
    }
    public void updateLoc(double[] loc) {
        userPos = coordToPixel(new double[]{loc[0], -loc[1]});
        this.invalidate();
    }

    public void updateLandmarks(Map<String, Landmark> ll) {
        savedLandmarks = ll;

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        canvasCenter = new float[]{w / 2.0f, h / 2.0f};
        userPos = coordToPixel(new double[]{0,0});

    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawUser(canvas);
        drawLandmarks(canvas);
    }
}
