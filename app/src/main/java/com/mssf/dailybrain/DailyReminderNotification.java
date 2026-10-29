package com.mssf.dailybrain;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public final class DailyReminderNotification {

    private static final String CHANNEL_ID = "daily_challenge_reminders";
    private static final int NOTIFICATION_ID = 1001;

    private DailyReminderNotification() {
        // Utility class.
    }

    public static void createNotificationChannel(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Daily Challenge Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            channel.setDescription(
                    "Reminders to play the Daily Brain challenge."
            );

            NotificationManager manager =
                    context.getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void show(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        createNotificationChannel(context);

        Intent intent = new Intent(
                context,
                DailyChallengeActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        android.app.Notification notification =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("Today's puzzle is ready!")
                        .setContentText(
                                "A new Daily Challenge is waiting for you. Can you beat it?"
                        )
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(
                                                "A new Daily Challenge is waiting for you. "
                                                        + "Play today's puzzle and challenge yourself!"
                                        )
                        )
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .build();

        NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID, notification);
    }
}