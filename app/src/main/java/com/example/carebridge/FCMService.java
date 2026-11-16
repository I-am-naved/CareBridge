package com.example.carebridge;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FCMService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        
        // Save token to Firebase if logged in
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseHelper.getUserRef(userId).child("fcmToken").setValue(token);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            
            // Provide a default intent
            Intent intent = new Intent(this, activity_home.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            // If data payload exists, try to extract offerId
            if (remoteMessage.getData().size() > 0) {
                if (remoteMessage.getData().containsKey("offerId")) {
                    intent.putExtra("offerId", remoteMessage.getData().get("offerId"));
                }
                if (remoteMessage.getData().containsKey("taskId")) {
                    intent.putExtra("taskId", remoteMessage.getData().get("taskId"));
                }
            }
            
            NotificationHelper.showNotification(this, title, body, intent);
        } else if (remoteMessage.getData().size() > 0) {
            // Check if message contains only a data payload
            String title = remoteMessage.getData().get("title");
            String body = remoteMessage.getData().get("body");
            if (title == null) title = "CareBridge";
            if (body == null) body = "You have a new update.";
            
            Intent intent = new Intent(this, activity_home.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            if (remoteMessage.getData().containsKey("offerId")) {
                intent.putExtra("offerId", remoteMessage.getData().get("offerId"));
            }
            if (remoteMessage.getData().containsKey("taskId")) {
                intent.putExtra("taskId", remoteMessage.getData().get("taskId"));
            }
            NotificationHelper.showNotification(this, title, body, intent);
        }
    }
}
