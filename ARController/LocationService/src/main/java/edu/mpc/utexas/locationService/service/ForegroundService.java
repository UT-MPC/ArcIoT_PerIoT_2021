package edu.mpc.utexas.locationService.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public abstract class ForegroundService extends Service {
    private String TAG = "ForegroundService";
    private String CHANNEL_ID = "ForegroundServiceChannel";
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        try {
            String input = intent.getStringExtra("contextText");
            String activityClassName = intent.getStringExtra("targetActivity");
            createNotificationChannel();
            Intent notificationIntent = new Intent(this, Class.forName(activityClassName));
            PendingIntent pendingIntent = PendingIntent.getActivity(this,
                    0, notificationIntent, 0);
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Foreground Service")
                    .setContentText(input)
                    .setContentIntent(pendingIntent)
                    .build();
            startForeground(1, notification);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Cannot find target class");
            stopSelf();
        }
        return START_STICKY;
        //do heavy work on a background thread
//        stopSelf();

    }

}
