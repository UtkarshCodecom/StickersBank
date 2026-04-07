package com.stickers.bank;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class PushNotificationService extends FirebaseMessagingService {

    // Should not change the channel ID's
    private final long[] DEFAULT_VIBRATE_PATTERN = {0, 250, 250, 250};
    private Uri SOUND_URI;
    private String channelId;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        if (remoteMessage.getNotification() != null) {
            sendMyNotification(remoteMessage);
        }
    }


    private void sendMyNotification(RemoteMessage message) {

        // The notification sound is based on the channel id. So it is mandatory to sent channel id from backend
        /*if (message.getNotification().getChannelId() == NOTIFICATION_EMERGENCY_CHANNEL_ID) {
            channelId = NOTIFICATION_EMERGENCY_CHANNEL_ID;
            SOUND_URI = Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/" + R.raw.tune);
        } else {
            channelId = NOTIFICATION_DEFAULT_CHANNEL_ID;
            SOUND_URI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }*/
        channelId = getString(R.string.default_notification_channel_id);
        SOUND_URI = Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/" + R.raw.tune);


        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);


        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_tray_icon)
                .setContentTitle(message.getNotification().getTitle() == null ? getResources().getString(R.string.app_name) : message.getNotification().getTitle())
                .setContentText(message.getNotification().getBody() == null ? "" : message.getNotification().getBody())
                .setAutoCancel(true)
                .setSound(SOUND_URI)
                .setVibrate(DEFAULT_VIBRATE_PATTERN)
                .setContentIntent(pendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            if (SOUND_URI != null) {
                // Changing Default mode of notification
                notificationBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
                // Creating an Audio Attribute
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build();

                // Creating Channel
                NotificationChannel notificationChannel = new NotificationChannel(channelId, "StickersBank", NotificationManager.IMPORTANCE_HIGH);
                notificationChannel.setSound(SOUND_URI, audioAttributes);
                notificationChannel.setVibrationPattern(DEFAULT_VIBRATE_PATTERN);
                notificationChannel.enableLights(true);
                notificationChannel.setLightColor(Color.RED);
                mNotificationManager.createNotificationChannel(notificationChannel);
            }
        }
        mNotificationManager.notify(0, notificationBuilder.build());
    }

}