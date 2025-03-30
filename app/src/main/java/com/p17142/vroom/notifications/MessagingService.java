package com.p17142.vroom.notifications;

import static com.p17142.vroom.utilities.Constants.KEY_NOTIFICATION_SENDER;
import static com.p17142.vroom.utilities.Constants.KEY_NOTIFICATION_TRIP_UID;
import static com.p17142.vroom.utilities.Constants.KEY_NOTIFICATION_TYPE;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.p17142.vroom.R;
import com.p17142.vroom.utilities.Logger;

import java.util.Objects;


public class MessagingService extends FirebaseMessagingService {

    private static final String MESSAGE_CHANNEL_ID = "message-notification";
    private NotificationManager notificationManager;
    private String notificationType = "";
    private String notificationSourceUsername = "";
    private String notificationRelatedTripUid = "";

    @Override
    public void onNewToken(@NonNull String token) { // runs on new fcm token request
        super.onNewToken(token);
        Logger.printLog(MessagingService.class,"New FCM Token: "+token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        if (!message.getData().isEmpty()) {
            notificationSourceUsername = message.getData().get(KEY_NOTIFICATION_SENDER);
            notificationRelatedTripUid = message.getData().get(KEY_NOTIFICATION_TRIP_UID);
            notificationType = message.getData().get(KEY_NOTIFICATION_TYPE);

            Logger.printLog(MessagingService.class, "FCM notificationSourceUsername: " + notificationSourceUsername);
            Logger.printLog(MessagingService.class, "FCM notificationRelatedTripUid: " + notificationRelatedTripUid);
            Logger.printLog(MessagingService.class, "FCM notificationType: " + notificationType);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,MESSAGE_CHANNEL_ID); // channel id

        // not new activity intent ftm
        Intent intent = new Intent(this, MessagingService.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY); // ensure no activity is launched

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );
        builder.setContentIntent(pendingIntent);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        builder.setContentTitle(Objects.requireNonNull(message.getNotification()).getTitle());
        builder.setContentText(message.getNotification().getBody());
        builder.setSound(defaultSoundUri);
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message.getNotification().getBody()));
        builder.setAutoCancel(true); // auto-close on click
        builder.setSmallIcon(R.drawable.baseline_message_24);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);

        notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId = MESSAGE_CHANNEL_ID;
        NotificationChannel channel = new NotificationChannel(channelId,"Vroom Messaging", NotificationManager.IMPORTANCE_HIGH);
        channel.enableLights(true);
        channel.canBypassDnd();
        notificationManager.createNotificationChannel(channel);
        builder.setChannelId(channelId);

        notificationManager.notify(100,builder.build());
    }
}
