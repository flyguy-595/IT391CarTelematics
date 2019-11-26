package edu.ilstu.cartelematics;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

@TargetApi(26)
public class MediaProjectionService extends Service {
    public MediaProjectionService() {

    }

    @Override
    public void onCreate(){
        NotificationManager manager = getSystemService(NotificationManager.class);
        NotificationChannel notificationChannel = new NotificationChannel("CarTelematics", "Car Telematics", NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.setDescription("Car Telematics is recording...");
        manager.createNotificationChannel(notificationChannel);
        Notification notification = new Notification.Builder(this, "CarTelematics").build();
        startForeground(1, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
