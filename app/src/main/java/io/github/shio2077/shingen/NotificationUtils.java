// NotificationUtils.java
package io.github.shio2077.shingen;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotificationUtils {

    public static final String CHANNEL_ID = "screen_capture";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "screen_capture",
                    "Screen Capture",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    public static Notification createNotification(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "screen_capture",
                    "Screen Capture",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(context, "screen_capture")
                .setContentTitle("Screen capturing")
                .setContentText("Recording your screen")
                .setSmallIcon(android.R.drawable.presence_video_away)
                .setOngoing(true)
                .build();
    }
}